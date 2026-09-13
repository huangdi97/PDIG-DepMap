import type { SqliteDriver } from '../db/driver.ts'
import { newId, nowIso } from '../utils/ids.ts'

/**
 * DiscoveryCandidate（MVP03 §31–§34）。
 *
 * 链路：Observation → DiscoveryCandidate →（用户确认）→ Node。
 * 与 Proposal 链路（→ Dependency）严格分开；Candidate 本身不进入 Impact、不 bump revision。
 * 同 normalizedKey 唯一（UNIQUE index）；dismissed 后需足够新证据才保守重提（post-dismiss ≥2）。
 */

export type DiscoveryCandidateKind =
  | 'payment_instrument'
  | 'account'
  | 'service'
  | 'membership'
  | 'device'
  | 'identity_anchor'
  | 'custom'

export type DiscoveryCandidateStatus = 'pending' | 'accepted' | 'dismissed' | 'superseded'

export interface DiscoveryCandidate {
  id: string
  candidateKind: DiscoveryCandidateKind
  displayLabel: string
  normalizedKey: string
  sourceInstanceId: string
  /** 引用 evidence（`<sourceInstanceId>#<seq>` 形式的引用串），不复制内容。 */
  evidenceRefs: string[]
  observationCount: number
  firstSeenAt: string
  lastSeenAt: string
  /** dismiss 时的 observationCount 快照（重提阈值 = 当前 count - 快照 ≥ 2）。 */
  dismissedAtObservationCount: number | null
  acceptedNodeId: string | null
  status: DiscoveryCandidateStatus
  createdAt: string
  updatedAt: string
}

export interface UpsertDiscoveryCandidateInput {
  candidateKind: DiscoveryCandidateKind
  displayLabel: string
  normalizedKey: string
  sourceInstanceId: string
  evidenceRef?: string
  occurredAt?: string
}

/** dismissed → pending 的保守重提阈值（新观测数；与 Proposal 重提同口径的保守精神）。 */
export const CANDIDATE_REOPEN_MIN_NEW_OBSERVATIONS = 2

function rowToCandidate(row: Record<string, unknown>): DiscoveryCandidate {
  return {
    id: String(row.id),
    candidateKind: String(row.candidate_kind) as DiscoveryCandidateKind,
    displayLabel: String(row.display_label),
    normalizedKey: String(row.normalized_key),
    sourceInstanceId: String(row.source_instance_id),
    evidenceRefs: safeParseArray(row.evidence_refs_json),
    observationCount: Number(row.observation_count),
    firstSeenAt: String(row.first_seen_at),
    lastSeenAt: String(row.last_seen_at),
    dismissedAtObservationCount:
      row.dismissed_at_observation_count === null || row.dismissed_at_observation_count === undefined
        ? null
        : Number(row.dismissed_at_observation_count),
    acceptedNodeId: (row.accepted_node_id as string | null) ?? null,
    status: String(row.status) as DiscoveryCandidateStatus,
    createdAt: String(row.created_at),
    updatedAt: String(row.updated_at),
  }
}

function safeParseArray(value: unknown): string[] {
  try {
    const parsed: unknown = JSON.parse(typeof value === 'string' ? value : '[]')
    return Array.isArray(parsed) ? parsed.map(String) : []
  } catch {
    return []
  }
}

export class DiscoveryCandidateRepository {
  private readonly driver: SqliteDriver

  constructor(driver: SqliteDriver) {
    this.driver = driver
  }

  /**
   * 同 normalizedKey upsert：
   * - pending → 累计观测数/证据引用/lastSeenAt（跨 SourceInstance 累计 provenance）
   * - accepted / superseded → 不变（幂等；不重复打扰）
   * - dismissed → 仅当 dismiss 后新增观测 ≥ CANDIDATE_REOPEN_MIN_NEW_OBSERVATIONS 才回到 pending
   */
  upsert(input: UpsertDiscoveryCandidateInput): { candidate: DiscoveryCandidate; changed: boolean } {
    return this.driver.transaction(() => {
      const now = nowIso()
      const existing = this.findByNormalizedKey(input.normalizedKey)
      if (!existing) {
        const id = newId()
        this.driver
          .prepare(
            `INSERT INTO discovery_candidates (id, candidate_kind, display_label, normalized_key, source_instance_id, evidence_refs_json, observation_count, first_seen_at, last_seen_at, dismissed_at_observation_count, accepted_node_id, status, created_at, updated_at)
             VALUES (?, ?, ?, ?, ?, ?, 1, ?, ?, NULL, NULL, 'pending', ?, ?)`,
          )
          .run(
            id,
            input.candidateKind,
            input.displayLabel,
            input.normalizedKey,
            input.sourceInstanceId,
            JSON.stringify(input.evidenceRef ? [input.evidenceRef] : []),
            input.occurredAt ?? now,
            input.occurredAt ?? now,
            now,
            now,
          )
        return { candidate: this.getById(id) as DiscoveryCandidate, changed: true }
      }

      if (existing.status === 'accepted' || existing.status === 'superseded') {
        return { candidate: existing, changed: false }
      }

      if (existing.status === 'dismissed') {
        const dismissedAt = existing.dismissedAtObservationCount ?? existing.observationCount
        const newCount = existing.observationCount + 1
        if (newCount - dismissedAt < CANDIDATE_REOPEN_MIN_NEW_OBSERVATIONS) {
          this.driver
            .prepare(
              `UPDATE discovery_candidates SET observation_count = ?, last_seen_at = ?, dismissed_at_observation_count = ?, evidence_refs_json = ?, updated_at = ? WHERE id = ?`,
            )
            .run(
              newCount,
              input.occurredAt ?? now,
              dismissedAt,
              JSON.stringify(
                input.evidenceRef && !existing.evidenceRefs.includes(input.evidenceRef)
                  ? [...existing.evidenceRefs, input.evidenceRef]
                  : existing.evidenceRefs,
              ),
              now,
              existing.id,
            )
          return { candidate: this.getById(existing.id) as DiscoveryCandidate, changed: false }
        }
        // 足够的新证据 → 保守重提回 pending
        this.driver
          .prepare(
            `UPDATE discovery_candidates SET status = 'pending', observation_count = ?, last_seen_at = ?, dismissed_at_observation_count = NULL, evidence_refs_json = ?, updated_at = ? WHERE id = ?`,
          )
          .run(
            newCount,
            input.occurredAt ?? now,
            JSON.stringify(
              input.evidenceRef && !existing.evidenceRefs.includes(input.evidenceRef)
                ? [...existing.evidenceRefs, input.evidenceRef]
                : existing.evidenceRefs,
            ),
            now,
            existing.id,
          )
        return { candidate: this.getById(existing.id) as DiscoveryCandidate, changed: true }
      }

      // pending：累计（跨实例 provenance 保留在 evidenceRefs）
      const refs =
        input.evidenceRef && !existing.evidenceRefs.includes(input.evidenceRef)
          ? [...existing.evidenceRefs, input.evidenceRef]
          : existing.evidenceRefs
      this.driver
        .prepare(
          `UPDATE discovery_candidates SET observation_count = ?, last_seen_at = ?, evidence_refs_json = ?, updated_at = ? WHERE id = ?`,
        )
        .run(existing.observationCount + 1, input.occurredAt ?? now, JSON.stringify(refs), now, existing.id)
      return { candidate: this.getById(existing.id) as DiscoveryCandidate, changed: true }
    })
  }

  getById(id: string): DiscoveryCandidate | null {
    const row = this.driver.prepare(`SELECT * FROM discovery_candidates WHERE id = ?`).get(id)
    return row ? rowToCandidate(row) : null
  }

  getExisting(id: string): DiscoveryCandidate {
    const c = this.getById(id)
    if (!c) throw new Error(`discovery candidate not found: ${id}`)
    return c
  }

  findByNormalizedKey(key: string): DiscoveryCandidate | null {
    const row = this.driver
      .prepare(`SELECT * FROM discovery_candidates WHERE normalized_key = ?`)
      .get(key)
    return row ? rowToCandidate(row) : null
  }

  listByStatus(status: DiscoveryCandidateStatus): DiscoveryCandidate[] {
    return this.driver
      .prepare(`SELECT * FROM discovery_candidates WHERE status = ? ORDER BY normalized_key`)
      .all(status)
      .map(rowToCandidate)
  }

  listAll(): DiscoveryCandidate[] {
    return this.driver
      .prepare(`SELECT * FROM discovery_candidates ORDER BY normalized_key`)
      .all()
      .map(rowToCandidate)
  }

  /** 用户接受：置 accepted + 记录创建的 Node id。**不在此创建 Node**（service 层负责）。 */
  markAccepted(id: string, nodeId: string): DiscoveryCandidate {
    this.driver
      .prepare(
        `UPDATE discovery_candidates SET status = 'accepted', accepted_node_id = ?, updated_at = ? WHERE id = ?`,
      )
      .run(nodeId, nowIso(), id)
    return this.getExisting(id)
  }

  /** 用户忽略：记录 dismiss 时的观测数快照（重提阈值用）。 */
  markDismissed(id: string): DiscoveryCandidate {
    const existing = this.getExisting(id)
    this.driver
      .prepare(
        `UPDATE discovery_candidates SET status = 'dismissed', dismissed_at_observation_count = ?, updated_at = ? WHERE id = ?`,
      )
      .run(existing.observationCount, nowIso(), id)
    return this.getExisting(id)
  }

  /** 被更高优先级对象取代（例如同名候选已被正式 Node 覆盖）。 */
  markSuperseded(id: string): DiscoveryCandidate {
    this.driver
      .prepare(`UPDATE discovery_candidates SET status = 'superseded', updated_at = ? WHERE id = ?`)
      .run(nowIso(), id)
    return this.getExisting(id)
  }

  countAll(): number {
    const row = this.driver.prepare(`SELECT COUNT(*) AS c FROM discovery_candidates`).get()
    return Number(row?.c ?? 0)
  }
}
