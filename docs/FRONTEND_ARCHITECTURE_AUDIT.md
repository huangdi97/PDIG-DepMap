# FRONTEND_ARCHITECTURE_AUDIT.md — PDIG 前端架构审计（Production RC V1）

> 范围：`app/`（uni-app x）。方法：逐文件实读 + `core/scripts/check-ui.mjs` 机械校验。
> 结论口径：`PASS` / `FAIL` / `BLOCKED` / `NOT_RUN` / `PARTIAL_WITH_REPORT`。

---

## 1. 审计前的实际状态（重要）

审计发现**两个结构性缺陷**，均非样式问题，而是架构与正确性问题：

### 1.1 页面直接操作 SQLite（违反 §61）

审计前实测：**12 个页面**含 `getSecureDb().query/execute/exec`。

### 1.2 页面复算领域逻辑（违反 §62）

| 页面 | 复算内容 | 风险 |
|---|---|---|
| `timeline.uvue` | 在页面内用 4 段 SQL + 客户端排序**重实现 Timeline 投影** | 与 `core/src/services/timeline.ts` 形成两份真相 |
| `plan-detail.uvue` | 客户端解析 `impact_snapshot_json` 派生 PlanReadiness | 与 Core 规则漂移 |
| `drift.uvue` | 客户端解析 drift 行并**直接 `UPDATE meta SET graph_revision = graph_revision + 1`** | 绕过 Core 事务与语义，属高危 |
| `impact-result.uvue` | 页面内判定 must_change / backup_path | 与 Core Impact Kernel 语义漂移 |

> **最严重**：`drift.uvue` 以裸 SQL 手动提升 graphRevision，绕开 Core 的
> 「Reality mutation 与 revision +1 同事务」保证（Core GR-001..012 已冻结）。
> 这属于**正确性风险**，而非风格问题。

### 1.3 接口不匹配（真实缺陷）

`depmap-secure-database` 插件接口只定义 `execute()`，而页面使用了 `db.exec()`：

- `plan-detail.uvue`、`drift.uvue`、`plan-create.uvue` 共 6 处

### 1.4 未声明标识符 / 不可用 API

| 文件 | 问题 |
|---|---|
| `plan-detail.uvue:155` | 使用未声明标识符 `mustChange`（且 `countOccurrences` 未使用） |
| `nodes.uvue:96` | `crypto.randomUUID()` —— uni-app x App 端**不可用**的 Web API |
| `App.uvue:2` | `import { checkUnlocked } from './stores/app-state'` —— 该导出**不存在** |
| `App.uvue:7` | `console.log('DepMap launched')` —— 违反 §88 Release 日志规则 |

**结论：UI 从未编译过，且即便有工具链也无法通过编译。**

---

## 2. 本轮处置

### 2.1 建立 Application Service 层（修复 §61）

新增 `app/services/depmap-service.uts`：**唯一数据边界**，承载全部 SQL。

- 所有页面改为调用服务函数（`getHomeSummary` / `listPlans` / `getPlanDetail` / `listDrifts` /
  `resolveDrift` / `listCandidates` / `acceptProposal` / `getTimeline` / `simulateUnavailable` …）
- 凡涉及 Reality mutation 的写入（`resolveDrift` / `acceptProposal` / `acceptCandidate` /
  `retireDependency`）一律置于 `transaction()` 内，并与 `graphRevision +1` **同事务**
- `dismissDrift` / `dismissCandidate` / `rejectProposal` 明确**不 bump revision、不改 Graph**

### 2.2 抽出纯规则层（收敛 §62）

新增 `app/services/rules.uts`：`computeReadiness` / `isImpactKeyResolved` /
`timelineBucketLabel` / `coverageLabel` / `workflowLabel` / `relationLabel` /
`criticalityLabel` / `SCENARIOS`。

### 2.3 修复真实缺陷

- 删除 `crypto.randomUUID()` → 由服务层生成 ID
- 删除不存在的 `checkUnlocked` 导入与 `console.log`
- 全部 `db.exec` → 服务层 `execute`
- `plan-detail` 未声明标识符随重写消除
- `node-detail.uvue` 模板引用未声明字段 `dep.peerName` → 统一为 `DependencyItem.peerName`（并新增 `check:ui` U9 机械拦截此类缺陷）

### 2.4 依赖方向修正（正确性缺陷）

`core/src/impact/kernel.ts` 的方向约定是：**`from` 为 `to` 提供支付**（`from` 失效 → `to` 受影响）。
App 侧原先有两处把方向写反，导致「换卡计划列出的不是受影响的服务，而是这张卡的资金来源」：

| 位置 | 原实现 | 修正后 |
|---|---|---|
| `createPlanWithAnalysis` | `WHERE d.to_node = 目标卡`，并把目标卡本身当作 impacted key | `WHERE d.from_node = 目标卡`；每个下游 `to_node` 各自成为一个 impacted key |
| `simulateUnavailable` | `WHERE d.to_node = 选中卡`，列出该卡的付款来源 | `WHERE d.from_node = 选中卡`，列出受影响的下游服务 |
| `getPlanCoverage` | `WHERE to_node = ?` | `WHERE from_node = ?`（覆盖率口径 = 目标节点的下游数量） |
| `plan-detail.uvue` | 把 `planId` 当作 nodeId 传给 Coverage（口径恒为空） | 改用新增的 `PlanDetail.targetNodeId` |
| `getNodeDetail` | 只查入边，且模板引用不存在的 `peerName` | 双向查询（`out` / `in`），按方向分别表述 |

### 2.5 补齐「手动声明支付关系」路径（使应用真正可用）

原先 Dependency 只能由 Proposal 接受产生，而 Proposal 只能由导入产生 —— 导入不可用（B20）
意味着**设备上无法建立任何依赖关系**，应用不可用。

本轮新增（语义完全在 MVP01 范围内：`Dependency 存在即用户确认`，AGENTS §9）：

- 服务层 `declareDependency(from, to, relation, capability, criticality)`
  —— 同一 logical key 的 INSERT / 不重复建边 / retired 后复用同 id 重新激活；
  Reality mutation 与 `graphRevision +1` **同事务**；criticality 只能由用户显式选择。
- 服务层 `dependencyExistsFor(...)`（重复声明提示）
- 规则层 `DECLARABLE_RELATIONS` / `CRITICALITY_OPTIONS` / `declarationRoleOf` / `peerKindsFor`
  —— 方向由「本节点是什么」推导，不让用户自己猜。
- 页面 `app/pages/nodes/declare-relation.uvue`（三步式：关系类型 → 另一端 → 是否必须处理）
- `node-detail.uvue` 入口按钮；`nodes.uvue` 空态引导改为手动添加

---

## 3. 现状判定

| 项 | 状态 | 证据 |
|---|---|---|
| UI 不直接操作 SQLite | **PASS** | `check:ui` U4（24 页 0 命中） |
| UI 不复算领域逻辑 | **PARTIAL_WITH_REPORT** | 纯规则已收敛到 `rules.uts`；但 **Core→App 桥接不存在**，服务层仍以 SQL 镜像部分 Core 语义（drift 解决、timeline 投影、coverage 分级） |
| 无未声明标识符 | **PASS** | `check:ui` U6 |
| 模板不引用不存在的字段 | **PASS** | `check:ui` U9（新增；已用注入探针验证可捕获） |
| 组件化 | **PASS** | 5 个 `dp-*` 组件；`check:ui` U7 |
| 设计 token | **PASS** | `app/theme/tokens.uts`；`check:ui` U5（0 非 token 颜色） |
| 页面状态完备 | **PASS（静态）** | `check:ui` U8 |
| 页面集合 | **PASS** | 24 页（含 onboarding / candidates / sources / backup / about / privacy / declare-relation） |
| 一级导航 | **PASS** | `pages.json` tabBar 4 项：首页 / 场景 / 计划 / 我的 |
| 依赖方向与 Core 一致 | **PASS（源码级）** | 4 处方向错误已修正，见 §2.4 |
| 无「假功能」 | **PASS** | 设置页无实际效果的开关已移除；导入/备份如实标注不可用 |

---

## 4. 已知限制（诚实登记，不声称已解决）

### 4.1 Core 与 App 之间**没有代码桥接**（最高优先级技术债）

Core 是纯 TypeScript（Node 22，453 tests）；App 是 UTS。**两者之间不存在共享运行时的机制**。
后果：

1. 页面/服务必须以 SQL + 本地规则**镜像** Core 语义 → 两份真相，存在漂移风险。
2. 影响分析（Impact Kernel）、PlanRebase、Drift 检测、Proposal 生成、CSV/OFX 解析
   **无法在设备上真实运行**。

**直接影响的产品能力**：

| 能力 | 设备上是否可用 |
|---|---|
| 导入账单（解析 + 指纹 + 生成 Proposal） | **不可用**（已登记 B20） |
| 手动建立对象 + 手动声明支付关系 | **可用**（本轮补齐，见 §2.5） |
| 影响模拟（已确认 `required` / `unknown` 依赖） | **可用**（方向已修正，见 §2.4） |
| 创建变更计划 + 计划内影响清单 | **可用**（保守口径：只有已确认且 `required` 才进 must_change） |
| 完整波次传播 / 多跳 Impact Kernel | **部分**：当前为单跳保守判定，非 Core 的多跳 kernel |
| 备份 / 恢复（Argon2id + AES-GCM） | **不可用**（已登记 B21） |
| 计划 Rebase 的确定性 diff | **部分**：仅推进版本号 |

**终局方案**（不在本轮范围）：Core → UTS 代码生成，或把纯规则编译为三端共享产物，
使 Core 成为唯一真相源。

### 4.2 UI 从未编译

`UI_BUILD_READY = BLOCKED（B10，无 HBuilderX）`。
本轮的 UI 质量保证仅限于 `check:ui` 的 **9 类**静态校验，**不能替代编译与真机验证**。

---

## 5. 结论

```
FRONTEND_ARCHITECTURE_AUDIT = PARTIAL_WITH_REPORT
  §61（UI 不直连 DB）        = PASS（本轮修复）
  §62（UI 不复算领域逻辑）    = PARTIAL（纯规则已收敛；Core 桥接缺失导致语义镜像）
  真实缺陷（exec/未声明/crypto/假导入/假开关/方向错误/模板字段）= 已修复或已显式标注
  产品可用性（不依赖导入）     = PASS（新增手动声明支付关系路径）
  Core→App 桥接               = NOT_IMPLEMENTED（B20/B21，阻断导入与备份）
```
