import type { ImportSession, Observation } from '../domain/types.ts'
import { dependencyLogicalKey } from '../domain/types.ts'
import type { SqliteDriver } from '../db/driver.ts'
import { NodeRepository } from '../repositories/node-repository.ts'
import { DependencyProposalRepository } from '../repositories/proposal-repository.ts'
import { EvidenceRepository } from '../repositories/evidence-repository.ts'
import { FingerprintRepository } from '../repositories/fingerprint-repository.ts'
import { ImportSessionRepository } from '../repositories/import-session-repository.ts'
import { MetaRepository } from '../repositories/meta-repository.ts'
import { assignFingerprints, FINGERPRINT_VERSION } from '../fingerprint/fingerprint.ts'
import { WECHAT_PARSER_ID, WECHAT_PARSER_VERSION, parseWechatBill } from '../parser/wechat/parser.ts'
import { detectRecurrence } from '../parser/wechat/recurring.ts'
import {
  findBankCardNode,
  parsePaymentMethod,
  resolveMerchant,
  type ResolvableEntity
} from '../resolver/resolver.ts'

/**
 * 导入流程 — 三段式（匹配 CANONICAL §5.6：Resolution 完成才允许 Proposal）
 *
 *   begin(raw)
 *     → parse + fingerprint 计算 + 按商户分组 + 首次 Resolution 尝试
 *     → 返回 pendingResolutions（未解析商户等待用户创建/选择节点）
 *   resolveMerchant(merchantRaw, nodeId)   （用户确认，可多次）
 *   finalize()
 *     → 指纹批量入库（UNIQUE 去重）→ recurrence → Proposal upsert → evidence → session 完成
 *
 * 铁律：
 * - 原始账单只在内存；Observation 会话结束销毁，不持久化
 * - 指纹在 finalize 时落库：用户中途放弃不会丢失未来重提机会
 * - 未完成 Node Resolution 的商户不产生 Proposal
 * - 只持久化 Fingerprint / Evidence Summary / Proposal 状态 / ImportSession
 */

export interface MerchantCandidate {
  merchantRaw: string
  /** begin 时已唯一解析到的节点（null = 需要用户确认） */
  resolvedNodeId: string | null
  /** begin 时已解析方式：'auto' | 'pending' */
  resolution: 'auto' | 'pending'
  observationCount: number
  paymentMethods: string[]
  firstObservedAt: string
  lastObservedAt: string
}

export interface BeginImportResult {
  session: ImportSession
  rawCount: number
  candidates: MerchantCandidate[]
  errors: Array<{ line: number; reason: string }>
}

export interface ImportOutcome {
  session: ImportSession
  newUniqueCount: number
  duplicateCount: number
  errorCount: number
  /** 本次新创建/更新的 proposal keys */
  proposalKeys: string[]
  /** 仍未 resolution 的商户（finalize 时跳过） */
  unresolvedMerchants: string[]
  recurrences: Array<{
    merchantRaw: string
    period: string
    confidence: number
    occurrences: number
    typicalAmount: number
  }>
}

export class ImportFlow {
  private session: ImportSession | null = null
  private observations: Observation[] = []
  private fingerprints: Array<{ fingerprint: string; stable: boolean }> = []
  private candidates = new Map<string, MerchantCandidate>()
  private manualResolutions = new Map<string, string>()
  private parseErrors: Array<{ line: number; reason: string }> = []
  private fpSecret: string

  private nodes: NodeRepository
  private proposals: DependencyProposalRepository
  private evidence: EvidenceRepository
  private fingerprintsRepo: FingerprintRepository
  private sessions: ImportSessionRepository
  private meta: MetaRepository

  constructor(private readonly driver: SqliteDriver) {
    this.nodes = new NodeRepository(driver)
    this.proposals = new DependencyProposalRepository(driver)
    this.evidence = new EvidenceRepository(driver)
    this.fingerprintsRepo = new FingerprintRepository(driver)
    this.sessions = new ImportSessionRepository(driver)
    this.meta = new MetaRepository(driver)
    this.fpSecret = this.meta.getOrCreateFpSecret(() => randomHex32()).secret
  }

  /** 阶段 1：解析 + 分组 + 首次 resolution 尝试。 */
  begin(raw: Uint8Array): BeginImportResult {
    if (this.session) throw new Error('flow already begun')
    const parsed = parseWechatBill(raw)
    const session = this.sessions.start({
      sourceType: 'wechat_bill',
      parserId: WECHAT_PARSER_ID,
      parserVersion: WECHAT_PARSER_VERSION
    })
    this.session = session
    this.observations = parsed.observations
    this.parseErrors = parsed.errors
    this.fingerprints = assignFingerprints(this.fpSecret, parsed.observations)

    // 按 merchantRaw 分组（只统计 non-empty）
    const groups = new Map<string, Observation[]>()
    for (const obs of this.observations) {
      if (obs.merchantRaw === '') continue
      const list = groups.get(obs.merchantRaw) ?? []
      list.push(obs)
      groups.set(obs.merchantRaw, list)
    }

    const entities = this.resolvableEntities()
    for (const [merchantRaw, obsList] of groups) {
      const result = resolveMerchant(merchantRaw, entities)
      const methods = [...new Set(obsList.map(o => o.paymentMethodRaw).filter(m => m !== ''))].sort()
      const times = obsList.map(o => o.occurredAt).sort()
      this.candidates.set(merchantRaw, {
        merchantRaw,
        resolvedNodeId: result.status === 'resolved' ? result.nodeId : null,
        resolution: result.status === 'resolved' ? 'auto' : 'pending',
        observationCount: obsList.length,
        paymentMethods: methods,
        firstObservedAt: times[0]!,
        lastObservedAt: times[times.length - 1]!
      })
    }

    return {
      session,
      rawCount: this.observations.length,
      candidates: [...this.candidates.values()].sort((a, b) => b.observationCount - a.observationCount || (a.merchantRaw < b.merchantRaw ? -1 : 1)),
      errors: this.parseErrors
    }
  }

  /** 阶段 2：用户确认某商户 → 节点。 */
  resolveMerchant(merchantRaw: string, nodeId: string): void {
    if (!this.session) throw new Error('begin() must be called first')
    this.nodes.getExisting(nodeId) // 节点必须存在
    const candidate = this.candidates.get(merchantRaw)
    if (!candidate) throw new Error(`merchant not in this import: ${merchantRaw}`)
    candidate.resolvedNodeId = nodeId
    candidate.resolution = 'pending' // 保持标记为“曾待确认”，由 UI 显示来源
    this.manualResolutions.set(merchantRaw, nodeId)
  }

  /** 阶段 3：指纹落库 + recurrence + proposal + session 完成。 */
  finalize(): ImportOutcome {
    if (!this.session) throw new Error('begin() must be called first')

    // 指纹批量入库（finalize 时机，用户中途放弃不烧指纹）
    const batch = this.fingerprints.map((f, i) => ({
      fingerprint: f.fingerprint,
      source: this.observations[i]!.source,
      fingerprintVersion: FINGERPRINT_VERSION,
      importSessionId: this.session!.id,
      firstSeenAt: this.observations[i]!.occurredAt
    }))
    const { fresh, duplicates } = this.fingerprintsRepo.insertBatch(batch)
    const freshSet = new Set(fresh)
    const freshObservations = this.observations.filter((_, i) => freshSet.has(this.fingerprints[i]!.fingerprint))

    // 用 fresh 观测重建商户分组（与 begin 的分组一致，只是过滤重复）
    const groups = new Map<string, Observation[]>()
    for (const obs of freshObservations) {
      if (obs.merchantRaw === '') continue
      const list = groups.get(obs.merchantRaw) ?? []
      list.push(obs)
      groups.set(obs.merchantRaw, list)
    }

    const proposalKeys: string[] = []
    const recurrences: ImportOutcome['recurrences'] = []
    const unresolved: string[] = []
    const wechatNode = this.ensureWechatAccount()

    for (const [merchantRaw, obsList] of groups) {
      const candidate = this.candidates.get(merchantRaw)!
      const serviceNodeId = candidate.resolvedNodeId ?? this.manualResolutions.get(merchantRaw)
      if (!serviceNodeId) {
        unresolved.push(merchantRaw)
        continue // 未 resolution → 不产生 Proposal（GOAL §14）
      }
      const rec = detectRecurrence(merchantRaw, obsList)
      if (!rec) continue // 周期不成立 → 无 recurring_payment_route proposal
      recurrences.push({
        merchantRaw,
        period: rec.period,
        confidence: rec.confidence,
        occurrences: rec.occurrences,
        typicalAmount: rec.typicalAmount
      })

      // merchant_agreement: wechat → service
      const merchantKey = dependencyLogicalKey({ from: wechatNode.id, relation: 'merchant_agreement', to: serviceNodeId, capability: 'payment' })
      this.proposals.upsert({
        from: wechatNode.id,
        relation: 'merchant_agreement',
        to: serviceNodeId,
        capability: 'payment',
        proposalType: 'recurring_payment_route',
        source: 'statement',
        parserId: WECHAT_PARSER_ID,
        parserVersion: WECHAT_PARSER_VERSION,
        confidenceScore: rec.confidence,
        path: [merchantRaw],
        newObservations: obsList.length
      })
      this.evidence.accumulate({
        proposalKey: merchantKey,
        sourceType: 'wechat_bill',
        parserId: WECHAT_PARSER_ID,
        parserVersion: WECHAT_PARSER_VERSION,
        importSessionId: this.session!.id,
        firstObservedAt: rec.firstObservedAt,
        lastObservedAt: rec.lastObservedAt,
        newObservations: obsList.length
      })
      proposalKeys.push(merchantKey)

      // funding_source: card → wechat（银行卡按 bank+last4 精确匹配；零钱 → wechat 自身）
      const cardIds = new Set<string>()
      for (const method of rec.paymentMethods) {
        const info = parsePaymentMethod(method)
        if (info.kind === 'bank_card') {
          const cardNode = findBankCardNode(info, this.nodes.list({ archived: false }))
          if (cardNode) cardIds.add(cardNode.id)
        } else if (info.kind === 'wechat_balance' || info.kind === 'wechat_change_pocket') {
          cardIds.add(wechatNode.id)
        }
      }
      for (const cardId of cardIds) {
        if (cardId === wechatNode.id) continue
        const cardObs = obsList.filter(o => parsePaymentMethod(o.paymentMethodRaw).kind === 'bank_card')
        const fundingKey = dependencyLogicalKey({ from: cardId, relation: 'funding_source', to: wechatNode.id, capability: 'payment' })
        this.proposals.upsert({
          from: cardId,
          relation: 'funding_source',
          to: wechatNode.id,
          capability: 'payment',
          proposalType: 'recurring_payment_route',
          source: 'statement',
          parserId: WECHAT_PARSER_ID,
          parserVersion: WECHAT_PARSER_VERSION,
          confidenceScore: rec.confidence,
          path: [cardId, wechatNode.id, merchantRaw],
          newObservations: Math.max(cardObs.length, 1)
        })
        this.evidence.accumulate({
          proposalKey: fundingKey,
          sourceType: 'wechat_bill',
          parserId: WECHAT_PARSER_ID,
          parserVersion: WECHAT_PARSER_VERSION,
          importSessionId: this.session!.id,
          firstObservedAt: rec.firstObservedAt,
          lastObservedAt: rec.lastObservedAt,
          newObservations: Math.max(cardObs.length, 1)
        })
        proposalKeys.push(fundingKey)
      }
    }

    const completed = this.sessions.update(this.session!.id, {
      completedAt: new Date().toISOString(),
      rawCount: this.observations.length,
      newUniqueCount: fresh.length,
      duplicateCount: duplicates,
      proposalCount: new Set(proposalKeys).size,
      errorCount: this.parseErrors.length
    })

    return {
      session: completed,
      newUniqueCount: fresh.length,
      duplicateCount: duplicates,
      errorCount: this.parseErrors.length,
      proposalKeys: [...new Set(proposalKeys)],
      unresolvedMerchants: unresolved,
      recurrences
    }
  }

  private resolvableEntities(): ResolvableEntity[] {
    return this.nodes.list({ archived: false }).map(n => ({
      nodeId: n.id,
      name: n.name,
      aliases: (n.fields['aliases'] as string[] | undefined) ?? []
    }))
  }

  private ensureWechatAccount() {
    const existing = this.nodes.list({ archived: false }).filter(n => n.kind === 'account' && n.templateId === 'builtin.account.wechat')
    if (existing.length === 1) return existing[0]!
    return this.nodes.create({
      kind: 'account',
      templateId: 'builtin.account.wechat',
      name: '微信支付'
    })
  }
}

function randomHex32(): string {
  const bytes = new Uint8Array(32)
  crypto.getRandomValues(bytes)
  return Array.from(bytes)
    .map(b => b.toString(16).padStart(2, '0'))
    .join('')
}
