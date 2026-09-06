import type { Capability, DependencyGroupProposal, GroupMode, ProposalDecision } from '../domain/types.ts'
import { groupProposalKey } from '../domain/types.ts'
import type { SqliteDriver } from '../db/driver.ts'
import { newId, nowIso } from '../utils/ids.ts'
import { optionalNumber, optionalString } from './meta-repository.ts'

export interface UpsertGroupProposalInput {
  targetNodeId: string
  capability: Capability
  mode: GroupMode
  memberDependencyKeys: string[]
  newObservations?: number
}

function rowToGroupProposal(row: Record<string, unknown>): DependencyGroupProposal {
  return {
    id: String(row.id),
    key: String(row.key),
    targetNodeId: String(row.target_node_id),
    capability: String(row.capability) as Capability,
    mode: String(row.mode) as GroupMode,
    memberDependencyKeys: JSON.parse(String(row.member_dependency_keys_json)) as string[],
    decision: String(row.decision) as ProposalDecision,
    decidedAt: optionalString(row.decided_at as never),
    rejectedAt: optionalString(row.rejected_at as never),
    rejectedAtObservationCount: optionalNumber(row.rejected_at_observation_count as never),
    createdAt: String(row.created_at),
    updatedAt: String(row.updated_at)
  }
}

/**
 * DependencyGroupProposal —— 备用路径也必须有确认生命周期 (AGENTS §10/§11)。
 * 同 key（canonical，[A,B]=[B,A]）UPSERT；拒绝后有显著新 evidence 才重提。
 */
export class DependencyGroupProposalRepository {
  constructor(
    private readonly driver: SqliteDriver,
    private readonly minNewObservationsForReproposal = 3
  ) {}

  getByKey(key: string): DependencyGroupProposal | null {
    const row = this.driver.prepare(`SELECT * FROM dependency_group_proposals WHERE key = ?`).get(key)
    return row ? rowToGroupProposal(row) : null
  }

  getById(id: string): DependencyGroupProposal | null {
    const row = this.driver.prepare(`SELECT * FROM dependency_group_proposals WHERE id = ?`).get(id)
    return row ? rowToGroupProposal(row) : null
  }

  listByDecision(decision: ProposalDecision): DependencyGroupProposal[] {
    return this.driver
      .prepare(`SELECT * FROM dependency_group_proposals WHERE decision = ? ORDER BY updated_at, key`)
      .all(decision)
      .map(rowToGroupProposal)
  }

  listAll(): DependencyGroupProposal[] {
    return this.driver
      .prepare(`SELECT * FROM dependency_group_proposals ORDER BY key`)
      .all()
      .map(rowToGroupProposal)
  }

  upsert(input: UpsertGroupProposalInput, opts: { cyclesCovered?: number; id?: string } = {}): {
    proposal: DependencyGroupProposal
    changed: boolean
    suppressed: boolean
    alreadyAccepted: boolean
  } {
    const key = groupProposalKey(input.targetNodeId, input.capability, input.mode, input.memberDependencyKeys)
    return this.driver.transaction(() => {
      const now = nowIso()
      const existing = this.getByKey(key)
      const addObs = input.newObservations ?? 0

      if (!existing) {
        const id = opts.id ?? newId()
        this.driver
          .prepare(
            `INSERT INTO dependency_group_proposals (id, key, target_node_id, capability, mode, member_dependency_keys_json, decision, decided_at, rejected_at, rejected_at_observation_count, created_at, updated_at)
             VALUES (?, ?, ?, ?, ?, ?, 'pending', NULL, NULL, NULL, ?, ?)`
          )
          .run(
            id,
            key,
            input.targetNodeId,
            input.capability,
            input.mode,
            JSON.stringify([...new Set(input.memberDependencyKeys)].sort()),
            now,
            now
          )
        return { proposal: this.getById(id) as DependencyGroupProposal, changed: true, suppressed: false, alreadyAccepted: false }
      }

      if (existing.decision === 'accepted') {
        return { proposal: existing, changed: false, suppressed: false, alreadyAccepted: true }
      }

      if (existing.decision === 'rejected') {
        // GroupProposal 自身有 observation 计数（每次新观测累计）
        const currentCount = Number(existing.rejectedAtObservationCount ?? 0) + addObs
        const newOnes = addObs
        const cycles = opts.cyclesCovered ?? 0
        if (newOnes < this.minNewObservationsForReproposal || cycles < 1) {
          if (addObs > 0) {
            this.driver
              .prepare(
                `UPDATE dependency_group_proposals SET rejected_at_observation_count = rejected_at_observation_count + ?, updated_at = ? WHERE id = ?`
              )
              .run(addObs, now, existing.id)
            return { proposal: this.getById(existing.id) as DependencyGroupProposal, changed: true, suppressed: true, alreadyAccepted: false }
          }
          return { proposal: existing, changed: false, suppressed: true, alreadyAccepted: false }
        }
        void currentCount
        this.driver
          .prepare(
            `UPDATE dependency_group_proposals SET decision = 'pending', decided_at = NULL, rejected_at = NULL, rejected_at_observation_count = NULL, member_dependency_keys_json = ?, updated_at = ? WHERE id = ?`
          )
          .run(JSON.stringify([...new Set(input.memberDependencyKeys)].sort()), now, existing.id)
        return { proposal: this.getById(existing.id) as DependencyGroupProposal, changed: true, suppressed: false, alreadyAccepted: false }
      }

      // pending → 更新成员集合（扩展）即可，无计数语义
      if (addObs > 0 || JSON.stringify([...new Set(input.memberDependencyKeys)].sort()) !== JSON.stringify([...new Set(existing.memberDependencyKeys)].sort())) {
        this.driver
          .prepare(`UPDATE dependency_group_proposals SET member_dependency_keys_json = ?, updated_at = ? WHERE id = ?`)
          .run(JSON.stringify([...new Set(input.memberDependencyKeys)].sort()), now, existing.id)
        return { proposal: this.getById(existing.id) as DependencyGroupProposal, changed: true, suppressed: false, alreadyAccepted: false }
      }
      return { proposal: existing, changed: false, suppressed: false, alreadyAccepted: false }
    })
  }

  decide(key: string, decision: Exclude<ProposalDecision, 'pending'>): DependencyGroupProposal {
    return this.driver.transaction(() => {
      const existing = this.getByKey(key)
      if (!existing) throw new Error(`group proposal not found: ${key}`)
      const now = nowIso()
      if (decision === 'rejected') {
        this.driver
          .prepare(
            `UPDATE dependency_group_proposals SET decision = 'rejected', decided_at = ?, rejected_at = ?, rejected_at_observation_count = 0, updated_at = ? WHERE id = ?`
          )
          .run(now, now, now, existing.id)
      } else {
        this.driver
          .prepare(
            `UPDATE dependency_group_proposals SET decision = 'accepted', decided_at = ?, updated_at = ? WHERE id = ?`
          )
          .run(now, now, existing.id)
      }
      return this.getById(existing.id) as DependencyGroupProposal
    })
  }
}
