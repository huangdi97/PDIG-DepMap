import type { ImpactGraph, ImpactProposalInput } from '../impact/kernel.ts'
import type { DependencyGroupRepository } from '../repositories/group-repository.ts'
import type { DependencyProposalRepository } from '../repositories/proposal-repository.ts'
import type { DependencyRepository } from '../repositories/dependency-repository.ts'
import type { SqliteDriver } from '../db/driver.ts'

/** buildImpactGraph 所需的仓库集合（plan-analysis / drift / coverage 共用）。 */
export interface ImpactGraphRepos {
  deps: DependencyRepository
  groups: DependencyGroupRepository
  proposals: DependencyProposalRepository
}

/**
 * ImpactGraph 组装视图（MVP03）：ChangePlan / ScenarioCoverage / Drift 共用。
 * pending proposal 才进入图（accepted 的已成为 Reality 边，不重复计入 proposal 通道）。
 */
export function buildImpactGraph(driver: SqliteDriver, repos: ImpactGraphRepos): ImpactGraph {
  const pendingProposals: ImpactProposalInput[] = repos.proposals
    .listAll()
    .filter((p) => p.decision === 'pending')
    .map((p) => ({ key: p.key, from: p.from, to: p.to, capability: p.capability }))
  return {
    dependencies: repos.deps.listAll(),
    groups: repos.groups.listAllActive(),
    proposals: pendingProposals,
  }
}
