import type {
  Capability,
  Criticality,
  Dependency,
  DependencyOrigin,
  Relation
} from '../domain/types.ts'
import { dependencyLogicalKey } from '../domain/types.ts'
import type { SqliteDriver } from '../db/driver.ts'
import { newId, nowIso } from '../utils/ids.ts'
import { optionalString } from './meta-repository.ts'

export interface ConfirmDependencyInput {
  from: string
  relation: Relation
  to: string
  capability: Capability
  /** 默认 unknown；机器不得自动产生 required (AGENTS §9)。 */
  criticality?: Criticality
  origin?: DependencyOrigin
  groupId?: string | null
  evidenceRefs?: string[]
  id?: string
}

export interface ConfirmDependencyResult {
  dependency: Dependency
  /** 是否为 retired → re-activate（同一 id）。 */
  reactivated: boolean
  /** 是否为已 active 的 verify 更新。 */
  verified: boolean
}

function rowToDependency(row: Record<string, unknown>): Dependency {
  return {
    id: String(row.id),
    from: String(row.from_node),
    relation: String(row.relation) as Relation,
    to: String(row.to_node),
    capability: String(row.capability) as Capability,
    criticality: String(row.criticality) as Criticality,
    groupId: optionalString(row.group_id as never),
    state: String(row.state) as Dependency['state'],
    origin: String(row.origin) as DependencyOrigin,
    confirmedAt: String(row.confirmed_at),
    lastVerifiedAt: String(row.last_verified_at),
    retiredAt: optionalString(row.retired_at as never),
    evidenceRefs: JSON.parse(String(row.evidence_refs_json ?? '[]')) as string[],
    createdAt: String(row.created_at),
    updatedAt: String(row.updated_at)
  }
}

/**
 * Dependency 存在即用户确认（AGENTS §9）。
 *
 * 同一 logical key (`from|relation|to|capability`)：
 * - 不存在   → INSERT
 * - active   → UPDATE lastVerifiedAt / evidence merge（不重复建边）
 * - retired  → re-activate 同一 id（清 retiredAt，更新 confirmedAt/lastVerifiedAt）
 */
export class DependencyRepository {
  constructor(private readonly driver: SqliteDriver) {}

  findByLogicalKey(from: string, relation: string, to: string, capability: string): Dependency | null {
    const row = this.driver
      .prepare(
        `SELECT * FROM dependencies WHERE from_node = ? AND relation = ? AND to_node = ? AND capability = ?`
      )
      .get(from, relation, to, capability)
    return row ? rowToDependency(row) : null
  }

  getById(id: string): Dependency | null {
    const row = this.driver.prepare(`SELECT * FROM dependencies WHERE id = ?`).get(id)
    return row ? rowToDependency(row) : null
  }

  listActive(): Dependency[] {
    return this.driver
      .prepare(`SELECT * FROM dependencies WHERE state = 'active' ORDER BY id`)
      .all()
      .map(rowToDependency)
  }

  listAll(): Dependency[] {
    return this.driver.prepare(`SELECT * FROM dependencies ORDER BY id`).all().map(rowToDependency)
  }

  /** 目标节点为 to、capability 匹配的 active 入边（Impact 用）。 */
  listActiveIncomingTo(to: string, capability: Capability): Dependency[] {
    return this.driver
      .prepare(
        `SELECT * FROM dependencies WHERE to_node = ? AND capability = ? AND state = 'active' ORDER BY id`
      )
      .all(to, capability)
      .map(rowToDependency)
  }

  /** from 节点的 active 出边（Impact 传播用）。 */
  listActiveOutgoingFrom(from: string, capability: Capability): Dependency[] {
    return this.driver
      .prepare(
        `SELECT * FROM dependencies WHERE from_node = ? AND capability = ? AND state = 'active' ORDER BY id`
      )
      .all(from, capability)
      .map(rowToDependency)
  }

  confirm(input: ConfirmDependencyInput): ConfirmDependencyResult {
    return this.driver.transaction(() => {
      const now = nowIso()
      const existing = this.findByLogicalKey(
        input.from,
        input.relation,
        input.to,
        input.capability
      )
      if (!existing) {
        const id = input.id ?? newId()
        this.driver
          .prepare(
            `INSERT INTO dependencies (id, from_node, relation, to_node, capability, criticality, group_id, state, origin, confirmed_at, last_verified_at, retired_at, evidence_refs_json, created_at, updated_at)
             VALUES (?, ?, ?, ?, ?, ?, ?, 'active', ?, ?, ?, NULL, ?, ?, ?)`
          )
          .run(
            id,
            input.from,
            input.relation,
            input.to,
            input.capability,
            input.criticality ?? 'unknown',
            input.groupId ?? null,
            input.origin ?? 'manual',
            now,
            now,
            JSON.stringify(input.evidenceRefs ?? []),
            now,
            now
          )
        return { dependency: this.getById(id) as Dependency, reactivated: false, verified: false }
      }

      if (existing.state === 'active') {
        const mergedRefs = mergeUnique(existing.evidenceRefs, input.evidenceRefs ?? [])
        // criticality 只允许用户显式升级；不允许从 required 降级回 unknown 静默覆盖
        const criticality = input.criticality ?? existing.criticality
        this.driver
          .prepare(
            `UPDATE dependencies SET last_verified_at = ?, evidence_refs_json = ?, criticality = ?, group_id = COALESCE(?, group_id), updated_at = ?
             WHERE id = ?`
          )
          .run(now, JSON.stringify(mergedRefs), criticality, input.groupId ?? null, now, existing.id)
        return { dependency: this.getById(existing.id) as Dependency, reactivated: false, verified: true }
      }

      // retired → re-activate 同一 id
      const mergedRefs = mergeUnique(existing.evidenceRefs, input.evidenceRefs ?? [])
      this.driver
        .prepare(
          `UPDATE dependencies SET state = 'active', retired_at = NULL, confirmed_at = ?, last_verified_at = ?, evidence_refs_json = ?, criticality = ?, origin = ?, updated_at = ?
           WHERE id = ?`
        )
        .run(
          now,
          now,
          JSON.stringify(mergedRefs),
          input.criticality ?? existing.criticality,
          input.origin ?? existing.origin,
          now,
          existing.id
        )
      return { dependency: this.getById(existing.id) as Dependency, reactivated: true, verified: false }
    })
  }

  retire(id: string): Dependency {
    const existing = this.getById(id)
    if (!existing) throw new Error(`dependency not found: ${id}`)
    if (existing.state === 'retired') return existing
    const now = nowIso()
    this.driver
      .prepare(`UPDATE dependencies SET state = 'retired', retired_at = ?, updated_at = ? WHERE id = ?`)
      .run(now, now, id)
    return this.getById(id) as Dependency
  }

  updateCriticality(id: string, criticality: Criticality): Dependency {
    // 只能由用户确认流程调用（service 层控制）；repository 不做 required 自动生成
    const now = nowIso()
    this.driver
      .prepare(`UPDATE dependencies SET criticality = ?, updated_at = ? WHERE id = ?`)
      .run(criticality, now, id)
    return this.getById(id) as Dependency
  }

  /** 设置 groupId（Group 确认后回填成员边）。 */
  setGroupId(id: string, groupId: string | null): void {
    this.driver
      .prepare(`UPDATE dependencies SET group_id = ?, updated_at = ? WHERE id = ?`)
      .run(groupId, nowIso(), id)
  }

  countAll(): number {
    const row = this.driver.prepare(`SELECT COUNT(*) AS c FROM dependencies`).get()
    return Number(row?.c ?? 0)
  }
}

function mergeUnique(existing: string[], incoming: string[]): string[] {
  return [...new Set([...existing, ...incoming])]
}
