import type { ImportSession, Observation } from '../domain/types.ts'
import { dependencyLogicalKey } from '../domain/types.ts'
import type { SourceInstance, NormalizedPaymentObservation } from '../domain/source.ts'
import type { SqliteDriver } from '../db/driver.ts'
import { NodeRepository } from '../repositories/node-repository.ts'
import { DependencyProposalRepository } from '../repositories/proposal-repository.ts'
import {
  FingerprintRepository,
  type ScopedFingerprintRecord,
} from '../repositories/fingerprint-repository.ts'
import { ImportSessionRepository } from '../repositories/import-session-repository.ts'
import { SourceInstanceRepository } from '../repositories/source-instance-repository.ts'
import { MetaRepository } from '../repositories/meta-repository.ts'
import { assignFingerprintsV2, FINGERPRINT_VERSION } from '../fingerprint/fingerprint.ts'
import { detectRecurrence } from '../parser/wechat/recurring.ts'
import { resolveMerchant, type ResolvableEntity } from '../resolver/resolver.ts'
import type { EvidenceSourceAdapter, SourceInput } from '../sources/types.ts'

/**
 * ImportCoordinator —— source-neutral 导入引擎（GOAL MVP02 §19）。
 *
 * SourceInstance → adapter(parse/normalize) → fingerprint(scope) → resolver
 * → evidence(按流) → proposal。Domain 层不出现任何 `if source == wechat`；
 * source-specific 逻辑（如微信 funding 推断）只存在于 Adapter.suggestRoutes。
 *
 * 三段式（Resolution 完成才允许 Proposal）：begin → resolveMerchant → finalize。
 * finalize 在 normalize 完成后以同步事务落库（失败整体回滚，不留半成品）。
 */

import type { ParseError } from '../parser/wechat/parser.ts'

export interface MerchantCandidate {
  /** 商户描述符（merchantRaw / counterparty / NAME） */
  merchantRaw: string
  resolvedNodeId: string | null
  resolution: 'auto' | 'pending'
  observationCount: number
  paymentMethods: string[]
  firstObservedAt: string
  lastObservedAt: string
}

export interface CoordinatorBeginResult {
  session: ImportSession
  rawCount: number
  candidates: MerchantCandidate[]
  errors: ParseError[]
}

export interface CoordinatorOutcome {
  session: ImportSession
  newUniqueCount: number
  duplicateCount: number
  errorCount: number
  proposalKeys: string[]
  unresolvedMerchants: string[]
  recurrences: Array<{
    merchantRaw: string
    period: string
    confidence: number
    occurrences: number
    typicalAmount: number
  }>
}

interface PendingImport {
  session: ImportSession
  sourceInstance: SourceInstance
  adapter: EvidenceSourceAdapter
  input: SourceInput
  observations: Observation[]
  normalized: NormalizedPaymentObservation[]
  errors: ParseError[]
  candidates: Map<string, MerchantCandidate>
  manualResolutions: Map<string, string>
}

export class ImportCoordinator {
  private readonly driver: SqliteDriver
  private nodes: NodeRepository
  private proposals: DependencyProposalRepository
  private fingerprintsRepo: FingerprintRepository
  private sessions: ImportSessionRepository
  private sourceInstances: SourceInstanceRepository
  private meta: MetaRepository

  constructor(driver: SqliteDriver) {
    this.driver = driver
    this.nodes = new NodeRepository(driver)
    this.proposals = new DependencyProposalRepository(driver)
    this.fingerprintsRepo = new FingerprintRepository(driver)
    this.sessions = new ImportSessionRepository(driver)
    this.sourceInstances = new SourceInstanceRepository(driver)
    this.meta = new MetaRepository(driver)
  }

  /** 阶段 1：parse + normalize + 商户分组 + 首次 resolution。 */
  async begin(
    sourceInstanceId: string,
    adapter: EvidenceSourceAdapter,
    input: SourceInput,
  ): Promise<CoordinatorBeginResult> {
    const sourceInstance = this.sourceInstances.getExisting(sourceInstanceId)
    const context = { sourceInstance }
    const parsed = await adapter.parse(input, context)
    const normalized = await adapter.normalize(parsed, context)
    if (normalized.length !== parsed.length) {
      throw new Error('adapter contract violation: normalize length mismatch')
    }
    const errors = adapter.lastParseErrors?.() ?? []

    const session = this.sessions.start({
      sourceType: adapter.id,
      parserId: adapter.id,
      parserVersion: adapter.version,
      sourceInstanceId: sourceInstance.id,
      adapterId: adapter.id,
      adapterVersion: adapter.version,
    })

    const entities = this.resolvableEntities()
    const grouped = new Map<string, number[]>()
    normalized.forEach((n, idx) => {
      const key = (n.merchantRaw ?? n.counterparty ?? '').trim()
      if (key === '') return
      const list = grouped.get(key) ?? []
      list.push(idx)
      grouped.set(key, list)
    })

    const candidates = new Map<string, MerchantCandidate>()
    for (const [merchantRaw, idxList] of grouped) {
      const result = resolveMerchant(merchantRaw, entities)
      const methods = [
        ...new Set(
          idxList.map((i) => normalized[i]?.paymentMethodHint ?? '').filter((m) => m !== ''),
        ),
      ].sort()
      const times = idxList.map((i) => normalized[i]?.occurredAt ?? '').sort()
      const firstAt = times[0]
      const lastAt = times[times.length - 1]
      if (firstAt === undefined || lastAt === undefined || firstAt === '' || lastAt === '') {
        throw new Error(`empty observation group for merchant: ${merchantRaw}`)
      }
      candidates.set(merchantRaw, {
        merchantRaw,
        resolvedNodeId: result.status === 'resolved' ? result.nodeId : null,
        resolution: result.status === 'resolved' ? 'auto' : 'pending',
        observationCount: idxList.length,
        paymentMethods: methods,
        firstObservedAt: firstAt,
        lastObservedAt: lastAt,
      })
    }

    const pending: PendingImport = {
      session,
      sourceInstance,
      adapter,
      input,
      observations: parsed,
      normalized,
      errors,
      candidates,
      manualResolutions: new Map(),
    }
    BEGIN_STATE.set(this, pending)
    return {
      session,
      rawCount: parsed.length,
      candidates: [...candidates.values()].sort(
        (a, b) =>
          b.observationCount - a.observationCount || (a.merchantRaw < b.merchantRaw ? -1 : 1),
      ),
      errors,
    }
  }

  /** 阶段 2：用户确认某商户描述符 → 节点。 */
  resolveMerchant(merchantRaw: string, nodeId: string): void {
    const pending = this.requirePending()
    this.nodes.getExisting(nodeId)
    const candidate = pending.candidates.get(merchantRaw)
    if (!candidate) throw new Error(`merchant not in this import: ${merchantRaw}`)
    candidate.resolvedNodeId = nodeId
    candidate.resolution = 'pending'
    pending.manualResolutions.set(merchantRaw, nodeId)
  }

  /** 阶段 3：normalize 已完成 → 指纹(scope) → recurrence → 路由 → proposals → session 完成。 */
  async finalize(): Promise<CoordinatorOutcome> {
    const pending = this.requirePending()
    const result = await this.finalizeInner(pending)
    BEGIN_STATE.delete(this)
    return result
  }

  private async finalizeInner(pending: PendingImport): Promise<CoordinatorOutcome> {
    // 契约要求 Promise（finalize 链为 async）；内部落库为同步事务。
    await Promise.resolve()
    const session = pending.session
    const fpSecret = this.meta.getOrCreateFpSecret(() => randomHex32()).secret
    const normalized = pending.normalized

    // 1. fingerprint 计算（scope：sourceInstanceId + fingerprintVersion）
    //    只计算不落库：落库放入下方事务，保证与 proposal/evidence/session 原子提交。
    const fps = assignFingerprintsV2(fpSecret, normalized)
    const batch: ScopedFingerprintRecord[] = normalized.map((n, i) => {
      const fp = fps[i]
      if (!fp) throw new Error('fingerprint/observation length mismatch')
      return {
        fingerprint: fp.fingerprint,
        sourceInstanceId: pending.sourceInstance.id,
        source: pending.adapter.id,
        fingerprintVersion: FINGERPRINT_VERSION,
        importSessionId: session.id,
        firstSeenAt: n.occurredAt,
      }
    })

    // fresh 判定：先只读探测（不写入），供事务内分组使用
    const fresh: string[] = []
    let duplicates = 0
    for (const rec of batch) {
      if (
        this.fingerprintsRepo.exists(rec.sourceInstanceId, rec.fingerprint, rec.fingerprintVersion)
      ) {
        duplicates += 1
      } else {
        fresh.push(rec.fingerprint)
      }
    }
    const freshSet = new Set(fresh)
    const freshIdx = normalized
      .map((_, i) => i)
      .filter((i) => {
        const fp = fps[i]
        return fp !== undefined && freshSet.has(fp.fingerprint)
      })

    // 2. 商户分组（fresh only）+ recurrence + 路由建议 → proposals（单事务）
    //
    // 事务完整性（RC PHASE AE）：指纹写入必须先于 proposals 但**不能独立提交**。
    // 在此事务内先写入指纹，再写 proposal/evidence/session：任何一步失败则整体回滚，
    // 不留下“指纹已落库但 proposal 未生成”的半成品状态。
    return this.driver.transaction(() => {
      const fpResult = this.fingerprintsRepo.insertBatch(batch)
      if (fpResult.duplicates !== duplicates || fpResult.fresh.length !== fresh.length) {
        throw new Error('fingerprint state changed between preview and transactional write')
      }
      const proposalKeys: string[] = []
      const recurrences: CoordinatorOutcome['recurrences'] = []
      const unresolved: string[] = []
      const resolvedByMerchant = new Map<string, string>()

      const merchantToNode = (merchantRaw: string): string | null => {
        const candidate = pending.candidates.get(merchantRaw)
        return candidate?.resolvedNodeId ?? pending.manualResolutions.get(merchantRaw) ?? null
      }

      const merchantGroups = new Map<string, number[]>()
      for (const i of freshIdx) {
        const n = normalized[i]
        if (!n) continue
        const key = (n.merchantRaw ?? n.counterparty ?? '').trim()
        if (key === '') continue
        const list = merchantGroups.get(key) ?? []
        merchantGroups.set(key, list)
        list.push(i)
      }

      for (const [merchantRaw, idxList] of merchantGroups) {
        const serviceNodeId = merchantToNode(merchantRaw)
        if (!serviceNodeId) {
          unresolved.push(merchantRaw)
          continue // 未 resolution → 不产生 Proposal（GOAL §14）
        }
        resolvedByMerchant.set(merchantRaw, serviceNodeId)

        // 周期识别：把 normalized 观测适配为 recurrence 输入形状
        const rec = detectRecurrence(
          merchantRaw,
          idxList
            .map((i) => normalized[i])
            .filter((n): n is NormalizedPaymentObservation => n !== undefined)
            .map((n) => viewForRecurrence(n)),
        )
        if (!rec) continue
        recurrences.push({
          merchantRaw,
          period: rec.period,
          confidence: rec.confidence,
          occurrences: rec.occurrences,
          typicalAmount: rec.typicalAmount,
        })

        // 路由建议：adapter-specific 优先；默认仅 merchant_agreement(accountNodeId → service)
        const routes =
          pending.adapter.suggestRoutes?.(
            normalized,
            { sourceInstance: pending.sourceInstance },
            resolvedByMerchant,
          ) ?? defaultRoutes(pending.sourceInstance, serviceNodeId)

        for (const route of routes) {
          if (route.relation === 'merchant_agreement' && route.to !== serviceNodeId) continue
          const proposalKey = dependencyLogicalKey({
            from: route.from,
            relation: route.relation,
            to: route.to,
            capability: 'payment',
          })
          this.proposals.upsert(
            {
              from: route.from,
              relation: route.relation,
              to: route.to,
              capability: 'payment',
              proposalType: 'recurring_payment_route',
              source: 'statement',
              parserId: pending.adapter.id,
              parserVersion: pending.adapter.version,
              confidenceScore: rec.confidence,
              path: [merchantRaw],
              evidence: {
                sourceInstanceId: pending.sourceInstance.id,
                adapterId: pending.adapter.id,
                adapterVersion: pending.adapter.version,
                newObservations: idxList.length,
                firstObservedAt: rec.firstObservedAt,
                lastObservedAt: rec.lastObservedAt,
              },
            },
            { importSessionId: session.id },
          )
          proposalKeys.push(proposalKey)
        }
      }

      const completed = this.sessions.update(session.id, {
        completedAt: new Date().toISOString(),
        rawCount: pending.observations.length,
        newUniqueCount: fresh.length,
        duplicateCount: duplicates,
        proposalCount: new Set(proposalKeys).size,
        errorCount: pending.errors.length,
      })
      this.sourceInstances.touchIngested(
        pending.sourceInstance.id,
        completed.completedAt ?? new Date().toISOString(),
      )

      return {
        session: completed,
        newUniqueCount: fresh.length,
        duplicateCount: duplicates,
        errorCount: pending.errors.length,
        proposalKeys: [...new Set(proposalKeys)],
        unresolvedMerchants: unresolved,
        recurrences,
      }
    })
  }

  private requirePending(): PendingImport {
    const pending = BEGIN_STATE.get(this)
    if (!pending) throw new Error('begin() must be called first')
    return pending
  }

  private resolvableEntities(): ResolvableEntity[] {
    return this.nodes.list({ archived: false }).map((n) => ({
      nodeId: n.id,
      name: n.name,
      aliases: (n.fields['aliases'] as string[] | undefined) ?? [],
    }))
  }
}

const BEGIN_STATE = new WeakMap<ImportCoordinator, PendingImport>()

/** detectRecurrence 输入视图（direction/amount/occurredAt/paymentMethodRaw/status）。 */
function viewForRecurrence(n: NormalizedPaymentObservation): Observation {
  return {
    source: n.adapterId,
    sourceTxnId: n.sourceTxnId ?? null,
    merchantTxnId: null,
    occurredAt: n.occurredAt,
    merchantRaw: n.merchantRaw ?? n.counterparty ?? '',
    description: n.description ?? '',
    amount: n.amount,
    currency: n.currency ?? 'XXX',
    direction: n.direction,
    paymentMethodRaw: n.paymentMethodHint ?? '',
    status: n.status ?? '',
    note: n.note ?? '',
  }
}

function defaultRoutes(
  sourceInstance: SourceInstance,
  serviceNodeId: string,
): Array<{ from: string; relation: 'merchant_agreement'; to: string }> {
  if (!sourceInstance.accountNodeId) return []
  return [{ from: sourceInstance.accountNodeId, relation: 'merchant_agreement', to: serviceNodeId }]
}

function randomHex32(): string {
  const bytes = new Uint8Array(32)
  crypto.getRandomValues(bytes)
  return Array.from(bytes)
    .map((b) => b.toString(16).padStart(2, '0'))
    .join('')
}
