import type { Capability } from '../domain/types.ts'
import type { SqliteDriver } from '../db/driver.ts'
import { newId, nowIso } from '../utils/ids.ts'

/**
 * RealityDrift（MVP03 §25–§29）：「已确认的 Reality 可能发生变化」的信号。
 *
 * 铁律：
 * - Drift 只引用 Evidence refs / Node ids / Dependency ids，绝不自动修改 Graph；
 * - absence 永远不能触发 Drift（检测入口只接受正向 evidence 信号）；
 * - Proposal 问「一个 Dependency 是否存在？」，Drift 问「已确认 Reality 是否可能变了？」。
 */

export type RealityDriftKind =
  | 'possible_replacement'
  | 'possible_additional_path'
  | 'relation_reappeared'

export type RealityDriftStatus = 'open' | 'confirmed_change' | 'dismissed' | 'superseded'

export interface RealityDrift {
  id: string
  kind: RealityDriftKind
  targetNodeId: string
  capability: Capability
  /** 新证据指向的来源对象（possible_replacement/additional 时为疑似新来源节点）。 */
  candidateFrom: string | null
  candidateRelation: string
  relatedDependencyIds: string[]
  evidenceRefs: string[]
  proposalKeys: string[]
  observationCount: number
  detectedAt: string
  updatedAt: string
  status: RealityDriftStatus
}

export interface UpsertDriftSignalInput {
  kind: RealityDriftKind
  targetNodeId: string
  capability: Capability
  candidateFrom: string | null
  candidateRelation?: string
  relatedDependencyIds?: string[]
  evidenceRef?: string
  proposalKey?: string
  /** 本信号包含的观测数（默认 1）。 */
  observations?: number
}

function rowToDrift(row: Record<string, unknown>): RealityDrift {
  return {
    id: String(row.id),
    kind: String(row.kind) as RealityDriftKind,
    targetNodeId: String(row.target_node_id),
    capability: String(row.capability) as Capability,
    candidateFrom: (row.candidate_from as string | null) ?? null,
    candidateRelation: String(row.candidate_relation),
    relatedDependencyIds: parseArray(row.related_dependency_ids_json),
    evidenceRefs: parseArray(row.evidence_refs_json),
    proposalKeys: parseArray(row.proposal_keys_json),
    observationCount: Number(row.observation_count),
    detectedAt: String(row.detected_at),
    updatedAt: String(row.updated_at),
    status: String(row.status) as RealityDriftStatus,
  }
}

function parseArray(value: unknown): string[] {
  try {
    const parsed: unknown = JSON.parse(typeof value === 'string' ? value : '[]')
    return Array.isArray(parsed) ? parsed.map(String) : []
  } catch {
    return []
  }
}

function driftKeyPrefix(input: UpsertDriftSignalInput): string {
  return `${input.kind}|${input.targetNodeId}|${input.capability}|${input.candidateFrom ?? '-'}|${input.candidateRelation ?? 'funding_source'}`
}

export class RealityDriftRepository {
  private readonly driver: SqliteDriver

  constructor(driver: SqliteDriver) {
    this.driver = driver
  }

  /**
   * 信号 upsert：
   * - 已有 open 同 key drift → 累计 evidence/observations（RD-008/007 幂等：同 evidenceRef 不重复计数）
   * - 无 open drift 且该 evidenceRef 未出现在历史同 key drift → 新建 open drift
   * - confirmed/dismissed/superseded 的历史 drift 不复活（新证据必须是未见过的 ref）
   */
  upsertSignal(input: UpsertDriftSignalInput): { drift: RealityDrift; created: boolean; changed: boolean } {
    return this.driver.transaction(() => {
      const prefix = driftKeyPrefix(input)
      const all = this.driver
        .prepare(`SELECT * FROM reality_drifts WHERE status = 'open'`)
        .all()
        .map(rowToDrift)
      const open = all.find((d) => driftKeyPrefixOf(d) === prefix)
      const now = nowIso()
      const observations = Math.max(1, input.observations ?? 1)

      if (open) {
        const refs =
          input.evidenceRef && !open.evidenceRefs.includes(input.evidenceRef)
            ? [...open.evidenceRefs, input.evidenceRef]
            : open.evidenceRefs
        const alreadyCounted = input.evidenceRef !== undefined && open.evidenceRefs.includes(input.evidenceRef)
        const keys =
          input.proposalKey && !open.proposalKeys.includes(input.proposalKey)
            ? [...open.proposalKeys, input.proposalKey]
            : open.proposalKeys
        const relatedChanged =
          JSON.stringify(mergeUnique(open.relatedDependencyIds, input.relatedDependencyIds ?? [])) !==
          JSON.stringify([...open.relatedDependencyIds].sort())
        const changed = !alreadyCounted || refs.length !== open.evidenceRefs.length || keys.length !== open.proposalKeys.length || relatedChanged
        this.driver
          .prepare(
            `UPDATE reality_drifts SET evidence_refs_json = ?, proposal_keys_json = ?, related_dependency_ids_json = ?, observation_count = ?, updated_at = ? WHERE id = ?`,
          )
          .run(
            JSON.stringify(refs),
            JSON.stringify(keys),
            JSON.stringify(
              mergeUnique(open.relatedDependencyIds, input.relatedDependencyIds ?? []),
            ),
            alreadyCounted ? open.observationCount : open.observationCount + observations,
            now,
            open.id,
          )
        return { drift: this.getById(open.id) as RealityDrift, created: false, changed }
      }

      // 无 open drift：同一 evidenceRef 已出现在历史 drift（同 key）→ 幂等不重复（RD-007）
      const signalRef = input.evidenceRef
      const history = this.listAll().filter((d) => driftKeyPrefixOf(d) === prefix)
      if (signalRef && history.some((d) => d.evidenceRefs.includes(signalRef))) {
        const latest = [...history].sort((a, b) => (a.updatedAt < b.updatedAt ? 1 : -1))[0]
        if (latest) return { drift: latest, created: false, changed: false }
      }

      const id = newId()
      this.driver
        .prepare(
          `INSERT INTO reality_drifts (id, kind, target_node_id, capability, candidate_from, candidate_relation, related_dependency_ids_json, evidence_refs_json, proposal_keys_json, observation_count, detected_at, updated_at, status)
           VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, 'open')`,
        )
        .run(
          id,
          input.kind,
          input.targetNodeId,
          input.capability,
          input.candidateFrom,
          input.candidateRelation ?? 'funding_source',
          JSON.stringify(input.relatedDependencyIds ?? []),
          JSON.stringify(input.evidenceRef ? [input.evidenceRef] : []),
          JSON.stringify(input.proposalKey ? [input.proposalKey] : []),
          observations,
          now,
          now,
        )
      return { drift: this.getById(id) as RealityDrift, created: true, changed: true }
    })
  }

  getById(id: string): RealityDrift | null {
    const row = this.driver.prepare(`SELECT * FROM reality_drifts WHERE id = ?`).get(id)
    return row ? rowToDrift(row) : null
  }

  getExisting(id: string): RealityDrift {
    const d = this.getById(id)
    if (!d) throw new Error(`reality drift not found: ${id}`)
    return d
  }

  listByStatus(status: RealityDriftStatus): RealityDrift[] {
    return this.driver
      .prepare(`SELECT * FROM reality_drifts WHERE status = ? ORDER BY detected_at, id`)
      .all(status)
      .map(rowToDrift)
  }

  listAll(): RealityDrift[] {
    return this.driver
      .prepare(`SELECT * FROM reality_drifts ORDER BY detected_at, id`)
      .all()
      .map(rowToDrift)
  }

  setStatus(id: string, status: RealityDriftStatus): RealityDrift {
    this.driver
      .prepare(`UPDATE reality_drifts SET status = ?, updated_at = ? WHERE id = ?`)
      .run(status, nowIso(), id)
    return this.getExisting(id)
  }

  countAll(): number {
    const row = this.driver.prepare(`SELECT COUNT(*) AS c FROM reality_drifts`).get()
    return Number(row?.c ?? 0)
  }
}

function driftKeyPrefixOf(d: RealityDrift): string {
  return `${d.kind}|${d.targetNodeId}|${d.capability}|${d.candidateFrom ?? '-'}|${d.candidateRelation}`
}

function mergeUnique(a: string[], b: string[]): string[] {
  return [...new Set([...a, ...b])].sort()
}
