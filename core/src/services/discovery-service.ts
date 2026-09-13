import type { DiscoveryCandidate, DiscoveryCandidateKind } from '../repositories/discovery-candidate-repository.ts'
import { DiscoveryCandidateRepository } from '../repositories/discovery-candidate-repository.ts'
import { NodeRepository, type CreateNodeInput } from '../repositories/node-repository.ts'
import type { NodeKind } from '../domain/types.ts'
import type { SqliteDriver } from '../db/driver.ts'

/**
 * DiscoveryCandidate → Node 确认服务（MVP03 §31–§34）。
 *
 * 铁律：
 * - Candidate 本身不进入 Impact、不 bump graphRevision；
 *   只有 accepted 产生的 **Node + 后续 Reality relation** 才影响图。
 * - accept 幂等：重复 accept 不创建第二个 Node。
 * - dismiss 不建 Node；重提由 repository 的保守阈值控制。
 */

const KIND_TO_NODE_KIND: Record<DiscoveryCandidateKind, NodeKind> = {
  payment_instrument: 'payment_instrument',
  account: 'account',
  service: 'service',
  membership: 'membership',
  device: 'device',
  identity_anchor: 'identity_anchor',
  custom: 'custom',
}

export interface AcceptCandidateResult {
  nodeId: string
  created: boolean
}

export class DiscoveryService {
  private readonly candidates: DiscoveryCandidateRepository
  private readonly nodes: NodeRepository

  constructor(driver: SqliteDriver) {
    this.candidates = new DiscoveryCandidateRepository(driver)
    this.nodes = new NodeRepository(driver)
  }

  upsertCandidate(
    input: Parameters<DiscoveryCandidateRepository['upsert']>[0],
  ): { candidate: DiscoveryCandidate; changed: boolean } {
    return this.candidates.upsert(input)
  }

  dismiss(id: string): DiscoveryCandidate {
    return this.candidates.markDismissed(id)
  }

  listPending(): DiscoveryCandidate[] {
    return this.candidates.listByStatus('pending')
  }

  /**
   * 接受候选 → 创建 Node（kind 由 candidateKind 映射）。
   * 幂等：已 accepted 的候选返回同一个 node，不重复创建（PC：accept replay）。
   */
  accept(id: string, nodeInput?: Partial<CreateNodeInput>): AcceptCandidateResult {
    const candidate = this.candidates.getExisting(id)
    if (candidate.status === 'accepted' && candidate.acceptedNodeId) {
      return { nodeId: candidate.acceptedNodeId, created: false }
    }
    if (candidate.status !== 'pending') {
      throw new Error(`candidate ${id} is ${candidate.status}; only pending candidates can be accepted`)
    }
    const node = this.nodes.create({
      ...nodeInput,
      kind: KIND_TO_NODE_KIND[candidate.candidateKind],
      name: nodeInput?.name ?? candidate.displayLabel,
    })
    this.candidates.markAccepted(id, node.id)
    return { nodeId: node.id, created: true }
  }
}
