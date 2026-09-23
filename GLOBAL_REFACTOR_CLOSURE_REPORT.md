# GLOBAL_REFACTOR_CLOSURE_REPORT.md

> PDIG / DepMap —— 全仓工程治理审计 v0.1.0 round（2026-09-23）
> 目的：把本轮**实际做过的**重构逐项登记（做什么、为什么、行为如何保持、怎么验证、是否遗留风险）。
> 原则（AGENTS §51/§55）：Minimum Safe Refactor；结构变化与行为变化分离；不删空行/不压缩格式/不删有价值注释。

## 0. 总览

| # | 重构 | 类型 | 行为变化 | 证据 |
|---|---|---|---|---|
| R1 | AppContainer.kt 1425 → 205 行，拆出 8 个 Repository/Models/DataUtils | 结构 | 无 | :app 编译 + androidTest 基线 + UI 引用不变 |
| R2 | UI 屏面文件拆分（DataScreens/PlanScreens/ImpactJourneyScreens → 13 单屏文件；FileWorkflowCoordinator + FileWorkflowEngine） | 结构 | 无 | :app:testDebugUnitTest 9/9 + androidTest 基线 |
| R3 | :core Parsers/ImpactKernel/Timeline 机械拆分（9 文件） | 结构 | 无（byte-exact） | :conformance:run 91/91（逐字段比对） |
| R4 | :conformance Main.kt 1095 → Main + 5 Runner | 结构 | 仅错误路径异常类型（ClassCast→Runtime，PASS 路径不变） | 91/91 |
| R5 | Repository 提取为共享纯 JVM 模块 `:repos`（Android + Desktop 共用） | 结构+依赖 | 无（AppContainer 公开 API 不变） | :app 编译 + 9/9 + androidTest |
| R6 | 生产代码 `!!` 23 处 → 0（safe-call/guard 等价改写） | 卫生 | 无（各改写经等价论证） | Gate kotlin-escape PASS + 编译 + 测试 |
| R7 | conformance unchecked cast `as Json.X` → 安全 cast 助手 | 卫生 | 仅错误路径消息 | 91/91 |
| R8 | Android product flavor（production/preview）+ 任务名兼容别名 | 构建 | 无（production 语义不变） | assembleDebug 双 flavor 成功 |

## R1 — AppContainer 拆 Repository

- 拆前：一个 1425 行 class 承载 nodes/deps/groups/proposals/candidates/drifts/plans/sources/backup 全部 SQL。
- 拆后：`AppContainer`（门面，构造函数 + 7 个 repo 字段 + 31 个 1:1 委托）+ `GraphRepository`、
  `ProposalRepository`、`CandidateRepository`、`DriftRepository`、`PlanRepository`、`SourceRepository`、
  `BackupRepository`（留 :app，MediaStore 绑定）+ `Models`(row 投影 + Import/Plan 模型) + `PlanSupport` + `DataUtils`。
- 关键判断：
  1. `GraphRepository` 构造器接收 `() -> List<ProposalRow>` lazy provider，避免 Graph↔Proposal 构造环；
  2. 仅 3 个 helper 由 private 改 internal（bumpRevision/upsertNode/upsertProposal），逐处登记；
  3. UI/测试通过 `com.pdig.app.data.X` 与 `AppContainer.X` 引用类型 —— R5 中把嵌套模型提升到包级时
     同步改写了 6 个 import + 1 处直接引用（变更票据见 commit 903ac92）。

## R2 — UI 屏面拆分

- 拆前：DataScreens.kt 809 / PlanScreens.kt 362 / ImpactJourneyScreens.kt 301 行，多屏共文件。
- 拆后：13 个单屏文件（34–265 行），公共 composable（PdigCard/PdigTopBar/EmptyState/…）留在 `ui/components`；
  `FileWorkflowCoordinator` 公有 API 不变，私有状态/副作用移到 `FileWorkflowEngine`。
- 验证：`:app:testDebugUnitTest` 9/9（FileWorkflowStateTest）、androidTest 基线引用（BACKUP_FAIL_PREFIX/backupUiState 保持 public）。

## R3 / R4 — core 与 conformance 拆分

- 同包顶层函数搬移（FQN 不变）+ 私有→internal（R3 仅 evaluateTarget 1 处；R4 为跨文件 runner 辅助逐项登记）；
- **逐字节验证**：split 前后切片 SHA-256 一致（fixer 报告，快照在 scratch/orig_*.kt）；
- 运行时验证：`:conformance:run` 91/91 PASS（split 后 fresh run，report conformance/reports/android.json）。

## R5 — 共享 `:repos` 模块

- 动机：Desktop 需要与 Android **完全一致**的 Repository 语义；复制会造成 drift，违反“单一真相源”；
- 结果：`android/repos`（纯 JVM，依赖 :core + kotlinx-coroutines）被 :app 与 desktop/app 同时 include；
  Android 与 Desktop 共用同一批 SQL/事务/revision 代码 —— Desktop 不成为第二个实现；
- 包名保持 `com.pdig.app.data`（避免 UI/测试 import 全改；跨模块同包在 Gradle classpath 模型下合法，已在模块头注释说明）；
- 验证：:app 编译 + 9/9 + androidTest 基线；桌面 smoke 复用同一调用（见 DESKTOP 报告）。

## R6 — `!!` 消除（23 → 0）

- 分布：HomeScreens/InfraScreens/ImportScreen/ScenarioSetupScreen/CandidateReviewScreen/PendingReviewScreen/
  RealityDriftScreen/PlanRepository 各 1-4 处；
- 改写模式：`x!!.isEmpty()` → `x?.isEmpty() == true`（delegate 属性无法 smart-cast，`== null ||` 形式编译不过）；
  `x!!.forEach` → `x?.forEach`；`node!!.name` → `node?.name ?: ""`；`expr == null || expr!!.m == v` → `expr == null || expr?.m == v`；
- 全部为等价改写（branch 顺序/字符串不变）；测试与 Gate 双重验证。

## R7 — conformance cast 消除

- 8+ 处 `as Json.Obj/Arr/Str/Num` → `requireObj/requireArr/requireStr/int` 助手（内部 `as?` + throw）；
- 行为差异只在“类型不匹配的失败路径”（ClassCastException → RuntimeException），runner 两种都报 FAIL，PASS 路径 byte-identical。

## R8 — flavor + 兼容别名

- `preview` flavor：applicationIdSuffix `.preview`、versionCode 200001（internal 轨）、versionName 0.1.0、
  app label “PDIG Preview”（src/preview/res/values/strings.xml）；
- `production` flavor：保持原值（versionCode 1 / 0.1.0-milestone，未定案不冒充）；
- 旧命令兼容：`testDebugUnitTest` / `connectedDebugAndroidTest` / `compileDebugKotlin` 别名指向 production 变体；
- `assembleDebug`：聚合双 flavor，已验证成功。

## 差分清单（本轮 commit）

- `75e33b6`：Gate + R1/R2/R3 + :repos 初版 + desktop 骨架
- `903ac92`：Gate 全 PASS + R6/R7/R8 + Desktop plumbing
- `2bf1820`：desktop 23 screens + secure persistence + smoke E2E + JVM tests 全绿
- `3759f1f` / `c4509de` / `d02a794`：v0.1.0 收尾三件（screen-protection 路由修正、jpackage 打包 task、EXCEPTIONS 登记桌面测试合成口令）
- `f5cc53e`：DESKTOP_V0_1_0_SCOPE_ADDENDUM + 打包/SBOM 工具（release(v0.1.0)）

## v0.1.0 轮复核结论（2026-09-24）

- 行为回归锚点复跑全绿：`:conformance:run` 91/91（fail=0）、`:core:test` 71/71、
  `:app:testDebugUnitTest` 9/9、desktop `:app:test` 9/9、`:app:run --args="--smoke"` 14/14 步骤 PASS；
- API36 AVD 仪器化证据 59/59（12 类全绿，0 crash）；
- `android/repos/src/main` 已纳入 quality gate scope（观察项关闭）；
- 产物：Windows x64 installer + portable zip、Android preview APK（versionCode 200001 / 0.1.0 /
  `com.pdig.app.preview`），Release 相关文档见 `PRODUCT_V0_1_0_RELEASE_MANIFEST.md`。

## 遗留清单（不做 ≠ 掩盖）

1. `BackupRepository` 的 MediaStore 部分未提取共享层（见 GLOBAL_ARCHITECTURE_AUDIT §7.1）；
2. `Route.GRAPH` 维持“非首页”（产品口径，D-条款）；
3. Desktop 未实现 candidate/drift 生成 pipeline（与 Android 对齐，消费既有数据；生成留给统一发现管线，FUTURE.md）；
4. conformance Runner 的失败路径异常类型变更（R4/R7）属可接受的 harness 级行为。

## 结论

- 本轮无 Big-Bang Rewrite、无并发混入业务改动、无历史重写/force push；
- 行为回归锚点：`:core:test` 71/71、`:app:testDebugUnitTest` 9/9、`:conformance:run` 91/91、androidTest 基线、
  Gate 10 计数 PASS —— 重构是可追溯、可复验、最小安全的。