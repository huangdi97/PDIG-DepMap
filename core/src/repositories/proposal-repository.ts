import type {
  Capability,
  Criticality,
  DependencyProposal,
  ProposalDecision,
  Relation,
} from '../domain/types.ts'
import { dependencyLogicalKey } from '../domain/types.ts'
import type { SqliteDriver } from '../db/driver.ts'
import { newId, nowIso } from '../utils/ids.ts'
import { optionalString } from './meta-repository.ts'
import { EvidenceRepository } from './evidence-repository.ts'

export interface UpsertProposalInput {
  from: string
  relation: Relation
  to: string
  capability: Capability
  proposalType: string
  source: string
  parserId: string
  parserVersion: number
  confidenceScore: number
  path?: string[]
  /**
   * Schema v2：本次新证据流（按 SourceInstance 分流累计）。
   * 省略时仅做 proposal 状态运算（repository 级测试用）。
   */
  evidence?: {
    sourceInstanceId: string
    adapterId: string
    adapterVersion: number
    evidenceKind?: string
    sourceType?: string
    newObservations: number
    firstObservedAt?: string
    lastObservedAt?: string
  }
  /** 兼容字段：直接累计总数（无 evidence 流时使用） */
  newObservations?: number
}

export interface UpsertProposalResult {
  proposal: DependencyProposal
  /**
   * 本次 upsert 是否改变了**决策相关**状态。
   *
   * 决策相关 = 会向用户再次提问 = 仅两种情形：
   * - 新建 proposal（pending）
   * - rejected → pending 的软性重提
   *
   * 纯计数/证据累计（pending 继续累计、accepted 后追加 evidence）不算 changed：
   * 用户不会被重复询问。需要判断“是否新写入数据”请读 observationCount / evidenceRefs。
   */
  changed: boolean
  /** rejected 且不满足重提条件 → 忽略 */
  suppressed: boolean
  /** accepted → 不重复问 */
  alreadyAccepted: boolean
}

/** 单流重提阈值：新观测 ≥ 3 且覆盖 ≥ 1 完整 recurrence cycle（单个 stream 内满足）。 */
export const REPROPOSAL_MIN_NEW_OBSERVATIONS = 3

/**
 * 重提判定（v2 保守规则）：必须在**单个 Evidence stream 内**独立满足阈值。
 * 禁止 WeChat 2 + Bank CSV 2 = 4 直接重提（Precision-first，GOAL MVP02 §10/§24）。
 */
export function canRepropose(
  proposal: DependencyProposal,
  streamCounts: Array<{ evidenceId: string; observationCount: number }>,
  cyclesCovered: number,
): boolean {
  if (proposal.decision !== 'rejected') return false
  if (cyclesCovered < 1) return false
  const snapshots = proposal.rejectedAtStreamCounts ?? {}
  for (const stream of streamCounts) {
    const atRejection = snapshots[stream.evidenceId] ?? 0
    if (stream.observationCount - atRejection >= REPROPOSAL_MIN_NEW_OBSERVATIONS) {
      return true
    }
  }
  return false
}

function parseStreamCounts(v: unknown): Record<string, number> | null {
  const raw = typeof v === 'string' ? v : null
  if (!raw) return null
  try {
    const parsed: unknown = JSON.parse(raw)
    if (parsed !== null && typeof parsed === 'object' && !Array.isArray(parsed)) {
      const out: Record<string, number> = {}
      for (const [k, val] of Object.entries(parsed as Record<string, unknown>)) {
        out[k] = Number(val)
      }
      return out
    }
    return null
  } catch {
    return null
  }
}

function parseStringArray(v: unknown): string[] {
  const raw = typeof v === 'string' ? v : '[]'
  try {
    const parsed: unknown = JSON.parse(raw)
    return Array.isArray(parsed) ? parsed.map(String) : []
  } catch {
    return []
  }
}

function rowToProposal(row: Record<string, unknown>, evidenceRefs: string[]): DependencyProposal {
  return {
    id: String(row.id),
    key: String(row.key),
    from: String(row.from_node),
    relation: String(row.relation) as Relation,
    to: String(row.to_node),
    capability: String(row.capability) as Capability,
    proposalType: String(row.proposal_type),
    source: String(row.source),
    parserId: String(row.parser_id),
    parserVersion: Number(row.parser_version),
    confidenceScore: Number(row.confidence_score),
    path: parseStringArray(row.path_json),
    evidenceRefs,
    decision: String(row.decision) as ProposalDecision,
    decidedAt: optionalString(row.decided_at as never),
    criticalityDecision: (optionalString(row.criticality_decision as never) ??
      null) as Criticality | null,
    observationCount: Number(row.observation_count ?? 0),
    rejectedAt: optionalString(row.rejected_at as never),
    rejectedAtStreamCounts: parseStreamCounts(row.rejected_at_stream_counts_json),
    createdAt: String(row.created_at),
    updatedAt: String(row.updated_at),
  }
}

export interface UpsertOptions {
  cyclesCovered?: number
  id?: string
  importSessionId?: string
}

/**
 * DependencyProposal —— 机器推断队列，不是图实体 (AGENTS §11)。
 *
 * 同 key UPSERT（v2 多源语义）：
 * - 不存在 → INSERT pending + 绑定 evidence stream
 * - pending → 更新 confidence / observationCount / evidenceRefs
 * - accepted → 不重复问（changed=false）
 * - rejected → 仅当**单个 Evidence stream**内新观测 ≥3 且跨完整周期才软性重提
 *
 * 多源聚合（GOAL MVP02 §22）：同一 logical key 永远只有一条 Proposal，
 * 新 source 只增加 evidenceRefs（provenance），不产生第二条建议、不改 decision。
 */
export class DependencyProposalRepository {
  private readonly driver: SqliteDriver
  private readonly evidence: EvidenceRepository

  constructor(driver: SqliteDriver) {
    this.driver = driver
    this.evidence = new EvidenceRepository(driver)
  }

  private attachEvidenceRef(key: string, evidenceId: string): void {
    this.driver
      .prepare(
        `INSERT OR IGNORE INTO proposal_evidence_refs (proposal_key, evidence_id, position)
         VALUES (?, ?, (SELECT COALESCE(MAX(position), -1) + 1 FROM proposal_evidence_refs WHERE proposal_key = ?))`,
      )
      .run(key, evidenceId, key)
  }

  private evidenceRefsOf(key: string): string[] {
    return this.driver
      .prepare(
        `SELECT evidence_id FROM proposal_evidence_refs WHERE proposal_key = ? ORDER BY position`,
      )
      .all(key)
      .map((r) => String((r as Record<string, unknown>).evidence_id))
  }

  private streamCountsOf(key: string): Array<{ evidenceId: string; observationCount: number }> {
    return this.driver
      .prepare(`SELECT id, observation_count FROM evidence WHERE proposal_key = ?`)
      .all(key)
      .map((r) => {
        const rec = r as Record<string, unknown>
        return { evidenceId: String(rec.id), observationCount: Number(rec.observation_count) }
      })
  }

  private hydrate(row: Record<string, unknown> | undefined): DependencyProposal | null {
    if (!row) return null
    return rowToProposal(row, this.evidenceRefsOf(String(row.key)))
  }

  getByKey(key: string): DependencyProposal | null {
    return this.hydrate(
      this.driver.prepare(`SELECT * FROM dependency_proposals WHERE key = ?`).get(key),
    )
  }

  getById(id: string): DependencyProposal | null {
    return this.hydrate(
      this.driver.prepare(`SELECT * FROM dependency_proposals WHERE id = ?`).get(id),
    )
  }

  listByDecision(decision: ProposalDecision): DependencyProposal[] {
    return this.driver
      .prepare(`SELECT * FROM dependency_proposals WHERE decision = ? ORDER BY updated_at, key`)
      .all(decision)
      .map((r) => this.hydrate(r) as DependencyProposal)
  }

  listAll(): DependencyProposal[] {
    return this.driver
      .prepare(`SELECT * FROM dependency_proposals ORDER BY key`)
      .all()
      .map((r) => this.hydrate(r) as DependencyProposal)
  }

  upsert(input: UpsertProposalInput, opts: UpsertOptions = {}): UpsertProposalResult {
    const key = dependencyLogicalKey(input)
    return this.driver.transaction(() => {
      const now = nowIso()
      const existing = this.getByKey(key)

      // ---- evidence 流累计（v2 provenance；跨源只增加 stream，不相加语义） ----
      let evidenceId: string | null = null
      if (input.evidence) {
        const acc = this.evidence.accumulate({
          proposalKey: key,
          sourceInstanceId: input.evidence.sourceInstanceId,
          adapterId: input.evidence.adapterId,
          adapterVersion: input.evidence.adapterVersion,
          evidenceKind: input.evidence.evidenceKind,
          sourceType: input.evidence.sourceType ?? input.source,
          parserId: input.parserId,
          parserVersion: input.parserVersion,
          importSessionId: opts.importSessionId ?? input.evidence.sourceInstanceId,
          firstObservedAt: input.evidence.firstObservedAt ?? nowIso(),
          lastObservedAt: input.evidence.lastObservedAt ?? nowIso(),
          newObservations: input.evidence.newObservations,
        })
        evidenceId = acc.evidence.id
      }
      const addObs = input.evidence ? input.evidence.newObservations : (input.newObservations ?? 0)

      if (!existing) {
        const id = opts.id ?? newId()
        this.driver
          .prepare(
            `INSERT INTO dependency_proposals (id, key, from_node, relation, to_node, capability, proposal_type, source, parser_id, parser_version, confidence_score, path_json, decision, decided_at, criticality_decision, observation_count, rejected_at, rejected_at_stream_counts_json, created_at, updated_at)
             VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, 'pending', NULL, NULL, ?, NULL, NULL, ?, ?)`,
          )
          .run(
            id,
            key,
            input.from,
            input.relation,
            input.to,
            input.capability,
            input.proposalType,
            input.source,
            input.parserId,
            input.parserVersion,
            input.confidenceScore,
            JSON.stringify(input.path ?? []),
            addObs,
            now,
            now,
          )
        if (evidenceId) this.attachEvidenceRef(key, evidenceId)
        return {
          proposal: this.getById(id) as DependencyProposal,
          changed: true,
          suppressed: false,
          alreadyAccepted: false,
        }
      }

      if (existing.decision === 'accepted') {
        // accepted 不重复问；新 source evidence 只增加 provenance（GOAL MVP02 §22）
        if (evidenceId) this.attachEvidenceRef(key, evidenceId)
        if (addObs > 0) {
          this.driver
            .prepare(
              `UPDATE dependency_proposals SET observation_count = observation_count + ?, updated_at = ? WHERE id = ?`,
            )
            .run(addObs, now, existing.id)
        }
        return {
          proposal: this.getById(existing.id) as DependencyProposal,
          // 决策已是 accepted → 不会再次提问 → changed 恒为 false
          changed: false,
          suppressed: false,
          alreadyAccepted: true,
        }
      }

      if (existing.decision === 'rejected') {
        const cycles = opts.cyclesCovered ?? 0
        const currentCount = existing.observationCount + addObs
        const eligible = canRepropose(
          { ...existing, decision: 'rejected' },
          this.streamCountsOf(key),
          cycles,
        )
        if (!eligible) {
          if (addObs > 0) {
            this.driver
              .prepare(
                `UPDATE dependency_proposals SET observation_count = ?, updated_at = ? WHERE id = ?`,
              )
              .run(currentCount, now, existing.id)
          }
          if (evidenceId) this.attachEvidenceRef(key, evidenceId)
          return {
            proposal: this.getById(existing.id) as DependencyProposal,
            // 仍为 rejected → 不会提问 → changed=false（数据已累计，见 proposal）
            changed: false,
            suppressed: true,
            alreadyAccepted: false,
          }
        }
        // 软性重提：回到 pending（单流阈值已满足）
        this.driver
          .prepare(
            `UPDATE dependency_proposals SET decision = 'pending', decided_at = NULL, confidence_score = ?, path_json = ?, observation_count = ?, rejected_at = NULL, rejected_at_stream_counts_json = NULL, updated_at = ?
             WHERE id = ?`,
          )
          .run(
            input.confidenceScore,
            JSON.stringify(input.path ?? existing.path),
            currentCount,
            now,
            existing.id,
          )
        if (evidenceId) this.attachEvidenceRef(key, evidenceId)
        return {
          proposal: this.getById(existing.id) as DependencyProposal,
          changed: true,
          suppressed: false,
          alreadyAccepted: false,
        }
      }

      // pending → 继续累计 evidence（决策未变，不会重复提问 → changed=false）
      this.driver
        .prepare(
          `UPDATE dependency_proposals SET confidence_score = MAX(confidence_score, ?), path_json = ?, observation_count = observation_count + ?, updated_at = ?
           WHERE id = ?`,
        )
        .run(
          input.confidenceScore,
          JSON.stringify(input.path ?? existing.path),
          addObs,
          now,
          existing.id,
        )
      if (evidenceId) this.attachEvidenceRef(key, evidenceId)
      return {
        proposal: this.getById(existing.id) as DependencyProposal,
        changed: false,
        suppressed: false,
        alreadyAccepted: false,
      }
    })
  }

  decide(
    key: string,
    decision: Exclude<ProposalDecision, 'pending'>,
    criticalityDecision?: Criticality | null,
  ): DependencyProposal {
    return this.driver.transaction(() => {
      const existing = this.getByKey(key)
      if (!existing) throw new Error(`proposal not found: ${key}`)
      const now = nowIso()
      if (decision === 'rejected') {
        // 快照每个 Evidence stream 的当前计数（单流重提阈值基准）
        const snapshots: Record<string, number> = {}
        for (const s of this.streamCountsOf(key)) snapshots[s.evidenceId] = s.observationCount
        this.driver
          .prepare(
            `UPDATE dependency_proposals SET decision = 'rejected', decided_at = ?, rejected_at = ?, rejected_at_stream_counts_json = ?, criticality_decision = COALESCE(?, criticality_decision), updated_at = ?
             WHERE id = ?`,
          )
          .run(now, now, JSON.stringify(snapshots), criticalityDecision ?? null, now, existing.id)
      } else {
        this.driver
          .prepare(
            `UPDATE dependency_proposals SET decision = 'accepted', decided_at = ?, criticality_decision = COALESCE(?, criticality_decision), updated_at = ? WHERE id = ?`,
          )
          .run(now, criticalityDecision ?? null, now, existing.id)
      }
      return this.getById(existing.id) as DependencyProposal
    })
  }

  countAll(): number {
    const row = this.driver.prepare(`SELECT COUNT(*) AS c FROM dependency_proposals`).get()
    return Number(row?.c ?? 0)
  }
}
