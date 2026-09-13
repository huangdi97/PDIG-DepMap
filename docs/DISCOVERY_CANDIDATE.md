# DISCOVERY_CANDIDATE.md — 发现候选（MVP03 §31–§34）

## 定义

DiscoveryCandidate 解决「发现一个可能存在的数字基础设施对象，但还不能直接创建 Node」：

```
Observation → DiscoveryCandidate → 用户确认 → Node
Observation → DependencyProposal → 用户确认 → Dependency   （严格分开的第二条链）
```

## 数据结构（Schema v3 `discovery_candidates`，normalizedKey UNIQUE）

id / candidateKind / displayLabel / normalizedKey / sourceInstanceId / evidenceRefs /
observationCount / firstSeenAt / lastSeenAt / dismissedAtObservationCount /
acceptedNodeId / status（pending | accepted | dismissed | superseded）。

## 生命周期

- **pending**：同 key upsert 累计 observationCount / evidenceRefs（跨 SourceInstance provenance 保留）
- **accepted**：创建 Node（kind 由 candidateKind 映射）；replay 幂等（同 id，不重复建）
- **dismissed**：不建 Node；重提需 dismiss 后新观测 ≥ `CANDIDATE_REOPEN_MIN_NEW_OBSERVATIONS`（=2，保守）
- **superseded**：被正式对象覆盖后不再打扰

## 铁律

- Candidate 本身**不进入 Impact**（无 Reality 边）、**不 bump graphRevision**；
  只有 accepted 产生的 Node + 后续 Reality 关系才影响图。
- 隐私（§33）：只存必要 label / normalized metadata；禁止完整浏览历史 / URL path /
  query string / 页面内容 / 原始账单行。当前服务 Node Resolver 未确认对象；
  未来 Browser/OAuth Discovery 复用此结构（NEXT_BACKLOG）。

## 测试证据（PC 七条，tests/services/discovery-candidate.test.ts）

同 key upsert / 跨源 provenance / accept 一个 Node / replay 不重复 /
dismiss 不建 Node + 保守重提 / 不 bump revision / 不进 Impact / accepted 后不打扰。
人工变异 M-R5（dismissed 无门槛重提）= KILLED。
