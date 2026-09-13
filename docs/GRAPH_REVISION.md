# GRAPH_REVISION.md — 图修订版本（MVP03 §9–§11）

## 定义

`graphRevision` 是 **Confirmed Reality Graph 的语义版本**，存于 `meta.graph_revision`（初始 0）。
不建新表、无 event sourcing、无 CRDT、无 snapshot。

## 提升规则（唯一合法来源）

只有以下 Reality mutation 提升 revision，**且与 Reality 写入在同一次 DB 事务内**：

| 操作 | 位置 |
|---|---|
| Dependency created（confirm INSERT） | `dependency-repository.confirm` |
| Dependency retired（幂等重放不加） | `dependency-repository.retire` |
| Dependency reactivated | `dependency-repository.confirm`（retired 分支） |
| Dependency criticality 被用户修改（值变化才加） | `dependency-repository.updateCriticality` |
| DependencyGroup confirmed（INSERT） | `group-repository.confirm` |
| DependencyGroup retired / reactivated | `group-repository` |
| （未来）其它 confirmed Reality mutation | 必须走同事务 bump |

## 明确不提升

Observation / ImportSession / Fingerprint / Evidence 更新 / Proposal 创建与 confidence 更新 /
RealityDrift 创建与处理 / DiscoveryCandidate 全生命周期 / Timeline projection / UI 状态。

## 事务保证

`bumpGraphRevision(driver)` 在 mutation 的 `driver.transaction` 内调用；
失败回滚时 revision 一并回滚（NodeSqliteDriver 嵌套事务经 SAVEPOINT）。
不存在「Reality 已变 revision 未变」或「revision +1 但 Reality 写失败」。

## 测试证据（GR-001..012，tests/repository/graph-revision.test.ts）

fresh=0 / confirm+1 / evidence 不变 / proposal 不变 / retire+1（幂等）/ reactivate+1 /
group confirm+retire / migration-restart 保持 / 事务失败不增 / 顺序事务不丢 /
replay 不重复增加 / deterministic。人工变异 M-R1（bump no-op）= KILLED。

## Payload

graph_revision 随 `meta` 行进入 payload v3，导出/导入往返保持（J5d）。
