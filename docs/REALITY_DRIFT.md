# REALITY_DRIFT.md — 现实漂移（MVP03 §25–§30）

## 定义

RealityDrift = 「已确认的 Reality **可能**发生变化」的信号。绝不自动修改 Graph。

与 Proposal 的职责边界（§29）：
- Proposal：「一个 Dependency 是否存在？」
- RealityDrift：「**已经确认的** Reality 是否可能变了？」

能复用 Proposal Evidence 就复用（引用 evidenceRefs），不复制 Evidence。

## 触发（只接受正向证据）

- `possible_replacement`：target 已有其他 confirmed 来源，新正向证据指向另一来源
- `possible_additional_path`：target 无 confirmed 来源，新正向证据出现
- `relation_reappeared`：曾经 confirmed、现已 retired 的关系重新出现正向证据

**absence 永远不能触发**（RD-002）：「本月没看到 CMB」不产生任何 drift ——
检测入口在类型上只接受正向 evidence 信号（结构性保证）。新建阈值默认 ≥2 次观测
（阈值只挡新建；已 open 的 drift 后续正向证据无论大小都累计）。

## 数据结构（Schema v3 `reality_drifts`）

id / kind / targetNodeId / capability / candidateFrom / candidateRelation /
relatedDependencyIds / evidenceRefs / proposalKeys / observationCount / detectedAt /
updatedAt / status（open → confirmed_change | dismissed | superseded）。

## 用户处理（四选项，无一键自动改图）

| 选择 | 映射 |
|---|---|
| 已经换成新来源 | confirm 新边（user_confirmed）+ retire 旧相关边 → graphRevision +N |
| 两个都在用 | 只 confirm 新边，旧边保持 active |
| 没有变化 / 稍后确认 | 仅 drift 状态 → dismissed，Graph 不变 |

同 key 的其他 open drift 在 resolution 后 superseded（防重复打扰）。

## 测试证据（RD-001..010 + 2 补充，tests/services/reality-drift.test.ts）

replacement 触发 / absence 无通道 / 检测不写 Reality / dismiss 不变图 /
replacement 确认（revision+）/ additional 旧边保留 / 重复证据不重复 /
upsert 累计 / 跨源 provenance / rejected proposal 无通道 / 低于阈值忽略 / reappeared。
人工变异 M-R3（阈值失效）= KILLED。
