import type { Capability, DependencyGroup, GroupMode } from '../domain/types.ts'
import { canonicalGroupKey } from '../domain/types.ts'
import type { SqliteDriver } from '../db/driver.ts'
import { newId, nowIso } from '../utils/ids.ts'

function rowToGroup(row: Record<string, unknown>): DependencyGroup {
  return {
    id: String(row.id),
    groupKey: String(row.group_key),
    targetNodeId: String(row.target_node_id),
    capability: String(row.capability) as Capability,
    mode: String(row.mode) as GroupMode,
    memberEdgeIds: JSON.parse(String(row.member_edge_ids_json)) as string[],
    state: String(row.state) as DependencyGroup['state'],
    confirmedAt: String(row.confirmed_at),
    lastVerifiedAt: String(row.last_verified_at),
    createdAt: String(row.created_at),
    updatedAt: String(row.updated_at),
  }
}

export interface CreateGroupInput {
  targetNodeId: string
  capability: Capability
  mode: GroupMode
  memberEdgeIds: string[]
  id?: string
}

/**
 * DependencyGroup 只能由用户确认产生（AGENTS §10）；
 * groupKey deterministic + UNIQUE，[A,B] 与 [B,A] 同组。
 */
export class DependencyGroupRepository {
  private readonly driver: SqliteDriver

  constructor(driver: SqliteDriver) {
    this.driver = driver
  }

  /** 按 groupKey 查找；同一成员组合不重复建组。 */
  findByKey(groupKey: string): DependencyGroup | null {
    const row = this.driver
      .prepare(`SELECT * FROM dependency_groups WHERE group_key = ?`)
      .get(groupKey)
    return row ? rowToGroup(row) : null
  }

  getById(id: string): DependencyGroup | null {
    const row = this.driver.prepare(`SELECT * FROM dependency_groups WHERE id = ?`).get(id)
    return row ? rowToGroup(row) : null
  }

  listActiveByTarget(targetNodeId: string, capability: Capability): DependencyGroup[] {
    return this.driver
      .prepare(
        `SELECT * FROM dependency_groups WHERE target_node_id = ? AND capability = ? AND state = 'active' ORDER BY id`,
      )
      .all(targetNodeId, capability)
      .map(rowToGroup)
  }

  listAllActive(): DependencyGroup[] {
    return this.driver
      .prepare(`SELECT * FROM dependency_groups WHERE state = 'active' ORDER BY id`)
      .all()
      .map(rowToGroup)
  }

  /**
   * 确认创建（或 re-activate 同一 groupKey）。
   * memberLogicalKeys 必须传入以构成 canonical groupKey。
   */
  confirm(
    input: CreateGroupInput,
    memberLogicalKeys: string[],
  ): { group: DependencyGroup; reactivated: boolean } {
    return this.driver.transaction(() => {
      const key = canonicalGroupKey(
        input.targetNodeId,
        input.capability,
        input.mode,
        memberLogicalKeys,
      )
      const existing = this.findByKey(key)
      const now = nowIso()
      if (existing) {
        if (existing.state === 'active') {
          this.driver
            .prepare(
              `UPDATE dependency_groups SET last_verified_at = ?, updated_at = ? WHERE id = ?`,
            )
            .run(now, now, existing.id)
          return { group: this.getById(existing.id) as DependencyGroup, reactivated: false }
        }
        this.driver
          .prepare(
            `UPDATE dependency_groups SET state = 'active', confirmed_at = ?, last_verified_at = ?, updated_at = ? WHERE id = ?`,
          )
          .run(now, now, now, existing.id)
        return { group: this.getById(existing.id) as DependencyGroup, reactivated: true }
      }
      const id = input.id ?? newId()
      this.driver
        .prepare(
          `INSERT INTO dependency_groups (id, group_key, target_node_id, capability, mode, member_edge_ids_json, state, confirmed_at, last_verified_at, created_at, updated_at)
           VALUES (?, ?, ?, ?, ?, ?, 'active', ?, ?, ?, ?)`,
        )
        .run(
          id,
          key,
          input.targetNodeId,
          input.capability,
          input.mode,
          JSON.stringify([...new Set(input.memberEdgeIds)].sort()),
          now,
          now,
          now,
          now,
        )
      return { group: this.getById(id) as DependencyGroup, reactivated: false }
    })
  }

  retire(id: string): DependencyGroup {
    const existing = this.getById(id)
    if (!existing) throw new Error(`group not found: ${id}`)
    if (existing.state === 'retired') return existing
    const now = nowIso()
    this.driver
      .prepare(`UPDATE dependency_groups SET state = 'retired', updated_at = ? WHERE id = ?`)
      .run(now, id)
    return this.getById(id) as DependencyGroup
  }
}
