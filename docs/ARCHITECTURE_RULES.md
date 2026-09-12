# ARCHITECTURE_RULES.md — 架构边界规则（Engineering Baseline V1，冻结）

> 自动化：`npm run check:architecture`（`core/scripts/check-architecture.mjs`），违规 exit 1。
> 历史设计文档：`docs/ARCHITECTURE.md`、`docs/SOURCE_ARCHITECTURE.md`。

## R1 Core ↔ 外部

`core/src/**` 不得 import：
- UI（app/、uni-app、Vue）
- 平台 SDK（Android / iOS / HarmonyOS 任何符号）
- `core/` 之外的相对路径

## R2 Domain 纯净性

`src/domain/**` 不得 import：`repositories/` `services/` `impact/` `crypto/` `db/`。
（domain 只定义类型、常量与纯函数：types.ts / source.ts / relation-registry.ts）

## R3 Crypto 独立性

`src/crypto/**` 不得 import：`repositories/` `services/` `impact/`。
（协议实现不得反向依赖业务层；UI 在 core 中不存在）

## R4 node:sqlite 隔离

`node:sqlite` 只允许出现在 `src/db/**`（业务代码永远通过 `SqliteDriver` 接口）。

## R5 循环依赖 = 0

`check:architecture` 内置 src 内部 import 图 DFS 三色标记环检测；
发现任何环即 FAIL。当前实测：**circular dependencies = 0**（35 files）。

## R6 语义职责边界（测试与评审强制）

| 层 | 禁止 |
|---|---|
| parser / source adapter | 创建 Dependency / DependencyGroup；设置 `required`；执行 Impact；写任何 Reality 表（contract C5 强制） |
| Impact kernel | import UI / parser / 平台 API / repositories；把 Proposal、unknown criticality 升级为 must_change |
| crypto 协议层 | 依赖 Repository / 业务层实现 |
| UI（app/） | 自行实现 Impact 语义；自行判定 required；自行确认 Proposal（只能调用确认服务） |
| repositories | 跨层处理业务判定（UPSERT/复活等存储语义除外） |

## R7 平台适配（AGENTS.md §6 重申）

业务核心不得直接依赖平台 API；Android/iOS/HarmonyOS 差异必须收敛在
SecureDatabaseAdapter / SecureKeyAdapter / BiometricAdapter / FileCryptoAdapter /
PrivacyScreenAdapter / FilePicker-ShareAdapter 之后。

## 新规则落位流程

新增边界规则时：先改 `check-architecture.mjs`（可自动化部分），再更新本文件并记录到
CHANGE_RISK_POLICY。仅靠评审强制的规则（R6 类）必须在 TEST_STRATEGY 的对应层注明证据测试。
