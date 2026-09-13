import type {
  Capability,
  Criticality,
  Dependency,
  DependencyOrigin,
  Relation,
} from '../domain/types.ts'
import type { SqliteDriver } from '../db/driver.ts'
import { newId, nowIso } from '../utils/ids.ts'
import { optionalString, unknownToString } from './meta-repository.ts'
import { bumpGraphRevision } from './graph-revision.ts'
import type { VerificationBasis } from '../domain/source.ts'

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
  /**
   * MVP02 §11：Reality 是如何被验证的。
   * 省略时默认 `user_confirmed`（手工添加/用户确认路径）；
   * `authoritative_source` 仅在适配器声明 authoritativeFor 后才允许使用。
   */
  verificationBasis?: VerificationBasis
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
    evidenceRefs: parseJsonArray(row.evidence_refs_json),
    verificationBasis: parseVerificationBasis(
      row.verification_basis_type,
      row.verification_basis_json,
    ),
    createdAt: String(row.created_at),
    updatedAt: String(row.updated_at),
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
  private readonly driver: SqliteDriver

  constructor(driver: SqliteDriver) {
    this.driver = driver
  }

  findByLogicalKey(
    from: string,
    relation: string,
    to: string,
    capability: string,
  ): Dependency | null {
    const row = this.driver
      .prepare(
        `SELECT * FROM dependencies WHERE from_node = ? AND relation = ? AND to_node = ? AND capability = ?`,
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
        `SELECT * FROM dependencies WHERE to_node = ? AND capability = ? AND state = 'active' ORDER BY id`,
      )
      .all(to, capability)
      .map(rowToDependency)
  }

  /** 目标节点为 to、capability 匹配的全部入边（含 retired；Drift 检测用）。 */
  listAllIncomingTo(to: string, capability: Capability): Dependency[] {
    return this.driver
      .prepare(`SELECT * FROM dependencies WHERE to_node = ? AND capability = ? ORDER BY id`)
      .all(to, capability)
      .map(rowToDependency)
  }

  /** from 节点的 active 出边（Impact 传播用）。 */
  listActiveOutgoingFrom(from: string, capability: Capability): Dependency[] {
    return this.driver
      .prepare(
        `SELECT * FROM dependencies WHERE from_node = ? AND capability = ? AND state = 'active' ORDER BY id`,
      )
      .all(from, capability)
      .map(rowToDependency)
  }

  confirm(input: ConfirmDependencyInput): ConfirmDependencyResult {
    return this.driver.transaction(() => {
      const now = nowIso()
      // MVP02 §11：Dependency 存在即已确认。省略 basis 时按 user_confirmed 处理；
      // authoritative_source 只有在适配器显式声明 authoritativeFor 后才允许出现。
      const basis = input.verificationBasis ?? { type: 'user_confirmed', verifiedAt: now }
      const basisType = basis.type
      const basisJson = JSON.stringify(basis)
      const existing = this.findByLogicalKey(input.from, input.relation, input.to, input.capability)
      if (!existing) {
        const id = input.id ?? newId()
        this.driver
          .prepare(
            `INSERT INTO dependencies (id, from_node, relation, to_node, capability, criticality, group_id, state, origin, confirmed_at, last_verified_at, retired_at, evidence_refs_json, verification_basis_type, verification_basis_json, created_at, updated_at)
             VALUES (?, ?, ?, ?, ?, ?, ?, 'active', ?, ?, ?, NULL, ?, ?, ?, ?, ?)`,
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
            basisType,
            basisJson,
            now,
            now,
          )
        bumpGraphRevision(this.driver) // GR-002：Dependency created → revision +1（同事务）
        return { dependency: this.getById(id) as Dependency, reactivated: false, verified: false }
      }

      if (existing.state === 'active') {
        const mergedRefs = mergeUnique(existing.evidenceRefs, input.evidenceRefs ?? [])
        // criticality 只允许用户显式升级；不允许从 required 降级回 unknown 静默覆盖
        const criticality = input.criticality ?? existing.criticality
        this.driver
          .prepare(
            `UPDATE dependencies SET last_verified_at = ?, evidence_refs_json = ?, criticality = ?, group_id = COALESCE(?, group_id), verification_basis_type = ?, verification_basis_json = ?, updated_at = ?
             WHERE id = ?`,
          )
          .run(
            now,
            JSON.stringify(mergedRefs),
            criticality,
            input.groupId ?? null,
            basisType,
            basisJson,
            now,
            existing.id,
          )
        return {
          dependency: this.getById(existing.id) as Dependency,
          reactivated: false,
          verified: true,
        }
      }

      // retired → re-activate 同一 id
      const mergedRefs = mergeUnique(existing.evidenceRefs, input.evidenceRefs ?? [])
      this.driver
        .prepare(
          `UPDATE dependencies SET state = 'active', retired_at = NULL, confirmed_at = ?, last_verified_at = ?, evidence_refs_json = ?, criticality = ?, origin = ?, verification_basis_type = ?, verification_basis_json = ?, updated_at = ?
           WHERE id = ?`,
        )
        .run(
          now,
          now,
          JSON.stringify(mergedRefs),
          input.criticality ?? existing.criticality,
          input.origin ?? existing.origin,
          basisType,
          basisJson,
          now,
          existing.id,
        )
      bumpGraphRevision(this.driver) // GR-006：retired → re-activate → revision +1（同事务）
      return {
        dependency: this.getById(existing.id) as Dependency,
        reactivated: true,
        verified: false,
      }
    })
  }

  retire(id: string): Dependency {
    return this.driver.transaction(() => {
      const existing = this.getById(id)
      if (!existing) throw new Error(`dependency not found: ${id}`)
      if (existing.state === 'retired') return existing
      const now = nowIso()
      this.driver
        .prepare(
          `UPDATE dependencies SET state = 'retired', retired_at = ?, updated_at = ? WHERE id = ?`,
        )
        .run(now, now, id)
      bumpGraphRevision(this.driver) // GR-005：retired → revision +1（同事务；幂等重放不重复加）
      return this.getById(id) as Dependency
    })
  }

  updateCriticality(id: string, criticality: Criticality): Dependency {
    // 只能由用户确认流程调用（service 层控制）；repository 不做 required 自动生成
    return this.driver.transaction(() => {
      const existing = this.getById(id)
      if (!existing) throw new Error(`dependency not found: ${id}`)
      const now = nowIso()
      this.driver
        .prepare(`UPDATE dependencies SET criticality = ?, updated_at = ? WHERE id = ?`)
        .run(criticality, now, id)
      if (existing.criticality !== criticality) {
        bumpGraphRevision(this.driver) // §9：criticality 被用户修改 → revision +1
      }
      return this.getById(id) as Dependency
    })
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

function parseVerificationBasis(type: unknown, json: unknown): VerificationBasis | null {
  if (type !== 'user_confirmed' && type !== 'authoritative_source') return null
  if (type === 'user_confirmed') {
    return { type: 'user_confirmed', verifiedAt: typeof json === 'string' ? json : '' }
  }
  try {
    const parsed: unknown = JSON.parse(typeof json === 'string' ? json : '{}')
    if (parsed !== null && typeof parsed === 'object') {
      const obj = parsed as Record<string, unknown>
      return {
        type: 'authoritative_source',
        sourceInstanceId: unknownToString(obj['sourceInstanceId']),
        factType: unknownToString(obj['factType']),
        verifiedAt: unknownToString(obj['verifiedAt']),
      }
    }
    return null
  } catch {
    return null
  }
}

function parseJsonArray(v: unknown): string[] {
  const raw = typeof v === 'string' ? v : '[]'
  try {
    const parsed: unknown = JSON.parse(raw)
    return Array.isArray(parsed) ? parsed.map(String) : []
  } catch {
    return []
  }
}

function mergeUnique(existing: string[], incoming: string[]): string[] {
  return [...new Set([...existing, ...incoming])]
}
