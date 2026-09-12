import type { Capability, Criticality, Dependency, DependencyGroup } from '../domain/types.ts'
import { dependencyLogicalKey } from '../domain/types.ts'
import type { SqliteDriver } from '../db/driver.ts'
import { NodeRepository } from '../repositories/node-repository.ts'
import { DependencyRepository } from '../repositories/dependency-repository.ts'
import { DependencyGroupRepository } from '../repositories/group-repository.ts'
import { DependencyProposalRepository } from '../repositories/proposal-repository.ts'
import { DependencyGroupProposalRepository } from '../repositories/group-proposal-repository.ts'
import { groupProposalKey } from '../domain/types.ts'
import { validateRelationUse, validateRelationGroupUse } from '../domain/relation-registry.ts'
import { userConfirmedBasis } from '../domain/source.ts'
import { nowIso } from '../utils/ids.ts'

/**
 * 确认服务 — “机器提出、用户确认现实”的唯一入口 (AGENTS §9-§11)
 *
 * - acceptProposal：用户确认关系存在 → Dependency（origin=proposal，criticality 默认
 *   unknown；只有用户明确选择 required 才是 required）
 * - rejectProposal：记录拒绝 + rejectedAtObservationCount（非永久为假）
 * - detectGroupProposals：同目标 ≥2 条 active funding 边且无 confirmed group →
 *   生成 GroupProposal（仅提议，不是备用路径事实）
 * - acceptGroupProposal：用户确认 → DependencyGroup（canonical groupKey）
 */

export class ConfirmationService {
  private nodes: NodeRepository
  private deps: DependencyRepository
  private groups: DependencyGroupRepository
  private proposals: DependencyProposalRepository
  private groupProposals: DependencyGroupProposalRepository

  constructor(driver: SqliteDriver) {
    this.nodes = new NodeRepository(driver)
    this.deps = new DependencyRepository(driver)
    this.groups = new DependencyGroupRepository(driver)
    this.proposals = new DependencyProposalRepository(driver)
    this.groupProposals = new DependencyGroupProposalRepository(driver)
  }

  /** 用户确认一条 Dependency（不存在/active/retired 由 repository UPSERT 处理）。 */
  acceptProposal(
    proposalKey: string,
    criticalityDecision?: Criticality,
  ): { dependency: Dependency; created: boolean } {
    const proposal = this.proposals.getByKey(proposalKey)
    if (!proposal) throw new Error(`proposal not found: ${proposalKey}`)
    if (proposal.decision === 'rejected') {
      throw new Error('proposal is rejected; reproposal flow required before acceptance')
    }
    // 节点必须已存在（Node Resolution 是 Proposal 前置条件）
    const fromNode = this.nodes.getExisting(proposal.from)
    const toNode = this.nodes.getExisting(proposal.to)
    // RelationDefinitionRegistry 校验（GOAL MVP02 §12）
    const relationCheck = validateRelationUse(
      fromNode.kind,
      proposal.relation,
      toNode.kind,
      proposal.capability,
    )
    if (!relationCheck.ok) throw new Error(`relation registry rejected: ${relationCheck.reason}`)

    this.proposals.decide(proposalKey, 'accepted', criticalityDecision ?? null)
    const result = this.deps.confirm({
      from: proposal.from,
      relation: proposal.relation,
      to: proposal.to,
      capability: proposal.capability,
      criticality: criticalityDecision ?? 'unknown',
      origin: 'proposal',
      evidenceRefs: proposal.evidenceRefs,
      // MVP02：三个文件 Adapter 全为 event_stream / authoritativeFor=[]，
      // 因此正式 Dependency 只能以 user_confirmed 为依据（禁止 CSV/OFX 自动确认 Reality）。
      verificationBasis: userConfirmedBasis(nowIso()),
    })
    return { dependency: result.dependency, created: !result.verified && !result.reactivated }
  }

  rejectProposal(proposalKey: string): void {
    this.proposals.decide(proposalKey, 'rejected')
  }

  /** 手工添加 Dependency（origin=manual；同样只进 confirmed 图）。 */
  addManualDependency(input: {
    from: string
    relation: Dependency['relation']
    to: string
    capability: Capability
    criticality?: Criticality
  }): Dependency {
    const fromNode = this.nodes.getExisting(input.from)
    const toNode = this.nodes.getExisting(input.to)
    const check = validateRelationUse(fromNode.kind, input.relation, toNode.kind, input.capability)
    if (!check.ok) throw new Error(`relation registry rejected: ${check.reason}`)
    return this.deps.confirm({ ...input, origin: 'manual' }).dependency
  }

  retireDependency(id: string): Dependency {
    return this.deps.retire(id)
  }

  /**
   * 检测 GroupProposal 候选：对每个目标节点，若其 active funding/merchant 入边
   * ≥2 且无 active confirmed group 覆盖 → upsert 一条 ANY GroupProposal。
   * 只提议，不确认。
   */
  detectGroupProposals(): string[] {
    const keys: string[] = []
    const activeDeps = this.deps.listActive().filter((d) => d.capability === 'payment')
    const byTarget = new Map<string, typeof activeDeps>()
    for (const d of activeDeps) {
      const list = byTarget.get(d.to) ?? []
      list.push(d)
      byTarget.set(d.to, list)
    }
    const activeGroups = this.groups.listAllActive()
    for (const [target, edges] of byTarget) {
      if (edges.length < 2) continue
      const memberKeys = edges.map((e) => dependencyLogicalKey(e))
      const covered = activeGroups.some(
        (g) =>
          g.targetNodeId === target &&
          g.capability === 'payment' &&
          g.memberEdgeIds.some((id) => edges.some((e) => e.id === id)),
      )
      if (covered) continue
      const key = groupProposalKey(target, 'payment', 'ANY', memberKeys)
      const r = this.groupProposals.upsert({
        targetNodeId: target,
        capability: 'payment',
        mode: 'ANY',
        memberDependencyKeys: memberKeys,
      })
      if (!r.alreadyAccepted) keys.push(key)
    }
    return keys
  }

  /** 用户确认 Group → 创建/复用 DependencyGroup，回填成员边 groupId。 */
  acceptGroupProposal(groupProposalKeyStr: string): DependencyGroup {
    const gp = this.groupProposals.getByKey(groupProposalKeyStr)
    if (!gp) throw new Error(`group proposal not found: ${groupProposalKeyStr}`)
    if (gp.decision === 'rejected') {
      throw new Error('group proposal is rejected; reproposal flow required')
    }

    // RelationDefinitionRegistry 校验（GOAL MVP02 §12）：
    // Group 必须 capability-scoped，且其成员关系必须在 runtime registry 中声明允许成组。
    // 缺失此校验会让“不支持成组的 relation”被静默提升为 ANY/ALL 语义，属正确性缺陷。
    for (const memberKey of gp.memberDependencyKeys) {
      const parts = memberKey.split('|')
      const relation = parts[1]
      if (!relation) throw new Error(`malformed member dependency key: ${memberKey}`)
      const relationCheck = validateRelationGroupUse(relation, gp.mode)
      if (!relationCheck.ok) {
        throw new Error(`relation registry rejected group: ${relationCheck.reason}`)
      }
    }

    this.groupProposals.decide(groupProposalKeyStr, 'accepted')

    const memberEdges = gp.memberDependencyKeys
      .map((k) => {
        const parts = k.split('|')
        if (parts.length !== 4) {
          throw new Error(`malformed member dependency key: ${k}`)
        }
        const from = parts[0]
        const relation = parts[1]
        const to = parts[2]
        const capability = parts[3]
        if (!from || !relation || !to || !capability) {
          throw new Error(`malformed member dependency key: ${k}`)
        }
        return this.deps.findByLogicalKey(from, relation, to, capability)
      })
      .filter((d): d is Dependency => d !== null && d.state === 'active')

    const { group } = this.groups.confirm(
      {
        targetNodeId: gp.targetNodeId,
        capability: gp.capability,
        mode: gp.mode,
        memberEdgeIds: memberEdges.map((e) => e.id),
      },
      gp.memberDependencyKeys,
    )
    for (const edge of memberEdges) {
      this.deps.setGroupId(edge.id, group.id)
    }
    return group
  }

  rejectGroupProposal(key: string): void {
    this.groupProposals.decide(key, 'rejected')
  }
}
