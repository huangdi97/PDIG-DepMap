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
import { optionalNumber, optionalString } from './meta-repository.ts'

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
  evidenceId?: string | null
  /** 本次新累计的 observation 数（用于 observationCount 累加） */
  newObservations?: number
}

export interface UpsertProposalResult {
  proposal: DependencyProposal
  /** upsert 实际改变了记录（pending 更新 / rejected 允许的重提） */
  changed: boolean
  /** rejected 且不满足重提条件 → 忽略 */
  suppressed: boolean
  /** accepted → 不重复问 */
  alreadyAccepted: boolean
}

/** 重提条件：新观测 ≥ 3 且覆盖至少一个完整 recurrence cycle。 */
export const REPROPOSAL_MIN_NEW_OBSERVATIONS = 3

export function canRepropose(
  proposal: DependencyProposal,
  currentObservationCount: number,
  cyclesCovered: number,
): boolean {
  if (proposal.decision !== 'rejected') return false
  const rejectedAtCount = proposal.rejectedAtObservationCount ?? 0
  const newOnes = currentObservationCount - rejectedAtCount
  return newOnes >= REPROPOSAL_MIN_NEW_OBSERVATIONS && cyclesCovered >= 1
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

function rowToProposal(row: Record<string, unknown>): DependencyProposal {
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
    evidenceId: optionalString(row.evidence_id as never),
    decision: String(row.decision) as ProposalDecision,
    decidedAt: optionalString(row.decided_at as never),
    criticalityDecision: (optionalString(row.criticality_decision as never) ??
      null) as Criticality | null,
    observationCount: Number(row.observation_count ?? 0),
    rejectedAt: optionalString(row.rejected_at as never),
    rejectedAtObservationCount: optionalNumber(row.rejected_at_observation_count as never),
    createdAt: String(row.created_at),
    updatedAt: String(row.updated_at),
  }
}

/**
 * DependencyProposal —— 机器推断队列，不是图实体 (AGENTS §11)。
 *
 * 同 key UPSERT：
 * - 不存在 → INSERT pending
 * - pending → 更新 evidence / confidence / observationCount
 * - accepted → 不重复问（changed=false）
 * - rejected → 仅当新 evidence 达标（≥3 新观测 + ≥1 完整周期）才软性重提（回到 pending）
 */
export class DependencyProposalRepository {
  constructor(private readonly driver: SqliteDriver) {}

  getByKey(key: string): DependencyProposal | null {
    const row = this.driver.prepare(`SELECT * FROM dependency_proposals WHERE key = ?`).get(key)
    return row ? rowToProposal(row) : null
  }

  getById(id: string): DependencyProposal | null {
    const row = this.driver.prepare(`SELECT * FROM dependency_proposals WHERE id = ?`).get(id)
    return row ? rowToProposal(row) : null
  }

  listByDecision(decision: ProposalDecision): DependencyProposal[] {
    return this.driver
      .prepare(`SELECT * FROM dependency_proposals WHERE decision = ? ORDER BY updated_at, key`)
      .all(decision)
      .map(rowToProposal)
  }

  listAll(): DependencyProposal[] {
    return this.driver
      .prepare(`SELECT * FROM dependency_proposals ORDER BY key`)
      .all()
      .map(rowToProposal)
  }

  upsert(
    input: UpsertProposalInput,
    opts: { cyclesCovered?: number; id?: string } = {},
  ): UpsertProposalResult {
    const key = dependencyLogicalKey(input)
    return this.driver.transaction(() => {
      const now = nowIso()
      const existing = this.getByKey(key)
      const addObs = input.newObservations ?? 0

      if (!existing) {
        const id = opts.id ?? newId()
        this.driver
          .prepare(
            `INSERT INTO dependency_proposals (id, key, from_node, relation, to_node, capability, proposal_type, source, parser_id, parser_version, confidence_score, path_json, evidence_id, decision, decided_at, criticality_decision, observation_count, rejected_at, rejected_at_observation_count, created_at, updated_at)
             VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, 'pending', NULL, NULL, ?, NULL, NULL, ?, ?)`,
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
            input.evidenceId ?? null,
            addObs,
            now,
            now,
          )
        return {
          proposal: this.getById(id) as DependencyProposal,
          changed: true,
          suppressed: false,
          alreadyAccepted: false,
        }
      }

      if (existing.decision === 'accepted') {
        return { proposal: existing, changed: false, suppressed: false, alreadyAccepted: true }
      }

      if (existing.decision === 'rejected') {
        const cycles = opts.cyclesCovered ?? 0
        const currentCount = existing.observationCount + addObs
        if (!canRepropose(existing, currentCount, cycles)) {
          // rejected 但新 evidence 不足 → 保持 rejected，不重提
          if (addObs > 0) {
            // 仅累计 observationCount（evidence 真实增长），但不改 decision
            this.driver
              .prepare(
                `UPDATE dependency_proposals SET observation_count = ?, updated_at = ? WHERE id = ?`,
              )
              .run(currentCount, now, existing.id)
            return {
              proposal: this.getById(existing.id) as DependencyProposal,
              changed: true,
              suppressed: true,
              alreadyAccepted: false,
            }
          }
          return { proposal: existing, changed: false, suppressed: true, alreadyAccepted: false }
        }
        // 软性重提：回到 pending
        this.driver
          .prepare(
            `UPDATE dependency_proposals SET decision = 'pending', decided_at = NULL, confidence_score = ?, path_json = ?, evidence_id = COALESCE(?, evidence_id), observation_count = ?, rejected_at = NULL, rejected_at_observation_count = NULL, updated_at = ?
             WHERE id = ?`,
          )
          .run(
            input.confidenceScore,
            JSON.stringify(input.path ?? existing.path),
            input.evidenceId ?? null,
            currentCount,
            now,
            existing.id,
          )
        return {
          proposal: this.getById(existing.id) as DependencyProposal,
          changed: true,
          suppressed: false,
          alreadyAccepted: false,
        }
      }

      // pending → 继续累计 evidence
      this.driver
        .prepare(
          `UPDATE dependency_proposals SET confidence_score = MAX(confidence_score, ?), path_json = ?, evidence_id = COALESCE(?, evidence_id), observation_count = observation_count + ?, updated_at = ?
           WHERE id = ?`,
        )
        .run(
          input.confidenceScore,
          JSON.stringify(input.path ?? existing.path),
          input.evidenceId ?? null,
          addObs,
          now,
          existing.id,
        )
      return {
        proposal: this.getById(existing.id) as DependencyProposal,
        changed: true,
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
        this.driver
          .prepare(
            `UPDATE dependency_proposals SET decision = 'rejected', decided_at = ?, rejected_at = ?, rejected_at_observation_count = observation_count, criticality_decision = COALESCE(?, criticality_decision), updated_at = ?
             WHERE id = ?`,
          )
          .run(now, now, criticalityDecision ?? null, now, existing.id)
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
