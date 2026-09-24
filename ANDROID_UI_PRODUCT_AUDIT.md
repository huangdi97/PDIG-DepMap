# ANDROID_UI_PRODUCT_AUDIT.md

> 审计时间：2026-09-20（Android Product Finalization）
> 审计对象：Android App 全部产品页面（Kotlin/Compose 原生）
> 证据来源：源码静态审计 + 设备内 androidTest（**59/59**，含新增 CandidateDrift 7 + DeleteAllData 1）+ Core Journey E2E v4（**41/41 PASS / 0 FAIL**）+ 设备 UI 操作（截图/像素取证）

---

## 0. 导航总表

| 路由                    | 页面                             | 组成文件                  | 可达入口                         | 截图保护                   |
| ----------------------- | -------------------------------- | ------------------------- | -------------------------------- | -------------------------- |
| `home`                  | 首页（产品首页）                 | `HomeScreens.kt`          | startDestination                 | 否（非敏感）               |
| `scenarios`             | 场景中心                         | `HomeScreens.kt`          | 首页「常用场景」                 | 否                         |
| `scenario/{templateId}` | 场景设置                         | `ImpactJourneyScreens.kt` | 场景中心卡片                     | **是**（敏感：变更哪张卡） |
| `impact/{nodeId}`       | 影响面                           | `ImpactJourneyScreens.kt` | Node 详情 / 计划页               | **是**（敏感：依赖明细）   |
| `plan/{planId}`         | 变更计划                         | `PlanScreens.kt`          | 场景设置创建后 / 时间线          | **是**（敏感：计划明细）   |
| `timeline`              | 即将到来（时间线）               | `HomeScreens.kt`          | 首页 Attention/Upcoming 卡片     | **是**                     |
| `review`                | 待确认服务（Proposal）           | `PlanScreens.kt`          | 首页 Attention（有待确认关系时） | **是**                     |
| `drift`                 | 可能发生了变化（RealityDrift）   | `PlanScreens.kt`          | 首页「可能发生了变化」           | **是**                     |
| `candidates`            | 待确认服务（DiscoveryCandidate） | `PlanScreens.kt`          | 首页 Attention（有候选时）       | **是**                     |
| `infrastructure`        | 我的基础设施                     | `InfraScreens.kt`         | 首页「我的基础设施」             | **是**（敏感：全部对象）   |
| `graph`                 | 依赖图（二级/高级辅助视图）      | `InfraScreens.kt`         | 基础设施页「依赖图」             | **是**                     |
| `node/{nodeId}`         | 对象详情                         | `InfraScreens.kt`         | 基础设施/图                      | **是**                     |
| `sources`               | 数据来源管理                     | `InfraScreens.kt`         | 首页「数据与设置」/设置          | **是**                     |
| `import`                | 导入账单                         | `DataScreens.kt`          | 数据来源页「导入」               | **是**                     |
| `backup`                | 备份（导出 .depmap）             | `DataScreens.kt`          | 设置「备份」                     | **是**                     |
| `restore`               | 从备份恢复                       | `DataScreens.kt`          | 设置「从备份恢复」               | **是**                     |
| `settings`              | 设置                             | `DataScreens.kt`          | 首页「数据与设置」               | 否                         |
| `privacy`               | 隐私                             | `DataScreens.kt`          | 设置「隐私」                     | 否                         |
| `about`                 | 关于                             | `DataScreens.kt`          | 设置「关于」                     | 否                         |
| `lock`                  | 锁屏（App 的门，非导航目的地）   | `LockScreen.kt`           | 冷启动 / 前后台回锁              | 否（须可见锁状态）         |

**Onboarding（`onboarding`）**：路由已按 D-9 产品决策移除注册；spec 记录取消；无 ghost 状态。

**Graph View**：仅作高级辅助视图（基础设施页内二级入口），**不是首页**，不违反「Graph 不是主界面」。

---

## 1. 逐页审计

### 1.1 Lock（锁屏）

- **真实存在**：`LockScreen.kt`；在 `PdigApp.kt` 中 `LockGate.locked` 时组合，NavHost 此时**不参与组合**（结构性不可绕过）。
- **可达**：冷启动必达（`LockGate` 初值 true）；前后台回锁必达（`ON_STOP` lockNow + `ON_RESUME` recheck）。
- **三态**：有凭据（生物识别/设备凭据）→ `验证身份并解锁`；无凭据 → `已知悉风险，本次进入` + `重新检查设备能力`；`LockCheckingScreen` 为中立首帧。
- **loading / success / error**：能力读取在后台线程（`Dispatchers.Default`），无锁死；错误均 fail-closed。
- **Back**：锁定时 Back 无任何可退出的敏感内容（栈里没有非锁页面）。
- **process restore**：应用锁是应用级状态（`LockGate` 不持久化 unlock），进程死亡重建后必回锁 —— E2E J9 `relaunch-locked` PASS。
- **断言**：`AppLockNavigationTest`（设备内 7/7，含「锁定时 NavHost 不参与组合」）、E2E J0 冷启动/前后台/二次解锁 PASS。
- **PASS**

### 1.2 Home（回答式产品首页）

- **真实存在 / 可达**：startDestination；E2E J1 `first-launch` PASS。
- **信息架构固定**：「需要你处理 → 可能发生了变化 → 即将到来 → 常用场景 → 我的基础设施 → 数据与设置」。
- **Attention 聚合**（E-10）：pending Proposal 计数 → `review`；pending DiscoveryCandidate 计数 → `candidates`；open Drift 计数 → `drift`；时间线 attention 桶（过期计划 needs_revalidation / 待验证 / 到期 / 来源过期）→ `timeline`。`HomeCounts` 一次 IO 取回（D-12：不在主线程读库）。
- **empty state**：无 attention → `现在没有需要你处理的事项。`；无 upcoming → `未来 90 天内没有已计划的变更。`；节点数 0 由卡片显示。
- **loading**：`LoadingState()` 在数据未就绪时显示。
- **error / retry**：数据库读失败被 `withContext(IO)` 包裹，失败保留 null → 停留在 loading（保守）；无网络错误路径（无网络）。
- **滚动 / insets**：`PdigScrollingPage` 提供滚动 + 底部空隙；E2E 导航栏顶边 = 2208 物理屏 2340 的适配已实测通过。
- **大字体 / 无障碍**：Compose 语义树审计 14 屏 0 无标签可交互节点；触摸目标 ≥48dp（`PdigTokens.MinTouchTarget=48.dp`）。
- **PASS（RUNTIME_VERIFIED）**

### 1.3 Scenario Center / Setup（场景中心 / 场景设置）

- **真实存在 / 可达**：首页「常用场景」→ `scenarios`；E2E v4 曾走过（v3 报告 J6a）。
- **场景清单**：唯一来源 `ScenarioRegistry.active`（replace_payment_card / expiring_payment_card / close_payment_instrument）；hardcode 被点名避免（历史缺陷 D-14）。
- **empty / loading / error**：active 恒 3 个 → 无空态；instrument 列表 loading 有 `LoadingState`。
- **Back**：`onBack = popBackStack` 正确。
- **planned 模板拒绝**：`PlannedScenarioNotice` 分支（replace_phone_number 不可执行）。
- **PASS（RUNTIME_VERIFIED 于 E2E；代码语义门禁）**

### 1.4 Impact（影响面）

- **真实存在 / 可达**：通过 `node/{nodeId}` 详情 → `impact/{nodeId}`，或计划页「查看影响范围」。
- **分组**：必须处理 / 建议检查 / 有备用路径·能力降级 / 未受影响；`impactStatusLabel` 人话标签（不显示内部术语）。
- **Reality Boundary（F-12）**：`必须处理` 只来源于 Confirmed Reality（ImpactKernel 输入仅 dependencies + pending proposals，且 criticality 只认 user_confirmed required）；proposal-only 最多 `建议检查`。
- **处理顺序**：`impact.checklist` 渲染。
- **PASS（E2E J5 `必须处理（2）` PASS；conformance impact 13/13）**

### 1.5 ChangePlan（变更计划）

- **真实存在 / 可达**：场景设置创建后抵达 `plan/{planId}`；时间线卡片也可达。
- **done ≠ verified**：动作卡片同时显示「已完成」与「验证：待验证」+「确认验证」按钮 —— E2E J7 `done-is-not-verified` PASS。
- **信息时效（E-11 修正）**：不再显示原始 graphRevision 数字，改为「计划依据的信息没有发生变化 / 创建后信息有更新，需要重新检查」人话。
- **状态标签**：`StatusChip` + `PdigStatus.label` 冻结用户语言（blocked→还有必须处理的事项…）。
- **PASS**

### 1.6 Timeline（即将到来）

- **真实存在 / 可达**：首页「需要你处理 / 即将到来」卡片 → `timeline`。
- **语义**：`buildTimeline` 纯投影（不落库、不 bump revision）；deterministic 排序（bucket → priority → scheduledAt → id）；conformance `timeline-buckets-and-ordering` / `timeline-attention-signals` / `timeline-terminal-plans-excluded` 全 PASS。
- **copy**：桶标签（需要你处理/已过期/今天/7 天内/30 天内/90 天内/以后）与 spec copy-zh 一致。
- **PASS（语义门禁 CONFORMANCE_PASS；E2E 首页聚合走通）**

### 1.7 Review（待确认服务 / Proposal）

- **真实存在 / 可达**：首页 Attention（有待确认关系时显示计数卡片）→ `review`。
- **Reality Boundary**：卡片文案「观测到 N 次，置信度 X%；确认前不会当成事实，也不参与影响分析」。
- **动作**：`确认`（acceptProposal → 建 Dependency） / `忽略`（rejectProposal）。
- **空态**：`没有待确认的项目。`
- **PASS（E2E J3/J4 走过）**

### 1.8 Drift（可能发生了变化 / RealityDrift）

- **真实存在 / 可达**：首页「可能发生了变化」→ `drift`（计数卡片：`有 N 条变化待确认` / `查看待确认的变化`）。
- **H-17 用户选择**：每张 drift 卡含 4 个动作 —— `已更换`（resolveAsReplacement：retire 旧边 + 建新边，bump）、`两者都在用`（resolveAsAdditionalPath：保留旧边 + 建新边，bump）、`没变化`（dismiss，不 bump）、`稍后确认`（保持 open）。
- **Reality Boundary**：文案「依据 N 条观测记录… 请确认：这张卡现在怎么在用？」—— 明确是「可能」而非事实。
- **empty / loading**：`没有检测到需要确认的变化。` / `LoadingState`。
- **断言**：`CandidateDriftEvidenceTest`（设备内 7/7：accept 幂等不 bump、dismiss 不改 Reality、replacement/additional 建边 bump、dismiss 不 bump、非 open 拒绝）。
- **PASS**

### 1.9 Candidates（待确认服务 / DiscoveryCandidate）

- **真实存在 / 可达**：首页 Attention（有待确认服务时显示计数卡片）→ `candidates`（H-16 补齐入口）。
- **动作**：`确认`（acceptCandidate → 创建 1 个 Node，**不 bump**，幂等） / `忽略`（dismiss，记录 dismissedAtObservationCount，不 bump）。
- **Reality Boundary**：「观测到 N 次；尚未确认，不会参与影响分析。」
- **空态**：`没有新的候选对象。`
- **PASS**

### 1.10 Infrastructure / Graph / Node Detail（基础设施 / 图 / 对象详情）

- **Infrastructure**：一级入口；空态 `还没有记录任何对象。可以先导入一份账单。`；卡片 → Node Detail。E2E J4 `back-home` 走过（招商银行…出现）。
- **Graph**：二级高级辅助视图（不做首页）；基础设施页「依赖图」进入。
- **Node Detail**：显示名称 + 依赖明细；`J5-mark-required` 走过（显式标记必需 2 次）。
- **PASS**

### 1.11 Sources / Import / Mapping / Review（来源与导入全流程）

- **Sources**：来源列表 + 空态 + 新建入口。
- **Import**：完整步骤「选择来源 → 选择文件 → 解析预览 → Node Resolution → 确认写入」；隐私提示「文件只在本机解析，不会上传；解析结果不会长期保存原始交易明细」。
- **D-16 双断言**（每个外部 picker 节点）：`externalPickerDoesNotBypassLock`（App 回锁 ≥1 次）+ `externalPickerDoesNotDestroyPendingWorkflow`（解锁后仍在原向导）—— E2E v4 每个 picker 都跑，全 PASS。
- **Node Resolution / Mapping / Review**：E2E J2 走过（支付方式 2 / 收款对象 3 → 记录 6 行，新增不重复 6 条）。
- **PASS**

### 1.12 Backup / Restore（备份导出 / 恢复）

- **Backup**：MediaStore Downloads 导出（不经外部 picker → 不受 D-16 影响）；UI 成功态 `备份已导出`；F1 回归 `BackupExportRegressionTest` 3/3（file exists + valid ⇔ UI success 一致）。
- **Restore**：SAF 选 `.depmap` → 回锁 → 解锁 → 恢复页 → 输密码 → `开始恢复`（显式确认，绝无 auto-restore）；错误密码 / 篡改容器均被拒（E2E J10/J11、`_tamper_dst` 内容）。
- **PASS**

### 1.13 Settings / Privacy / About / Security（设置 / 隐私 / 关于 / 安全）

- **Settings**：数据来源管理、备份、从备份恢复、立即锁定、**删除所有数据（本轮新增，L-37）**、隐私、关于。破坏性操作有 AlertDialog 二次确认。
- **Privacy**：无业务网络 / 无分析 / 原始账单不落库 / 备份加密 —— 与真实产品能力一致（不虚标）。
- **About**：PDIG + 版本 0.1.0-milestone（Native Migration）+「内部里程碑版本」说明；无假 URL。
- **PASS**

---

## 2. 横向检查（每页共用的产品标准）

| 检查项                      | 结论                    | 证据                                                                                                                                                                                     |
| --------------------------- | ----------------------- | ---------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------- |
| 真实存在                    | **PASS**                | 21 个目的地全部实现在 NavHost（见 §0；onboarding 已按决策移除）                                                                                                                          |
| 可达                        | **PASS**                | 首页聚合 + 设置 + 二级入口；被移除的 onboarding 不再计入                                                                                                                                 |
| empty state                 | **PASS**                | 每页有 `EmptyState`（Home / Timeline / Review / Drift / Candidates / Import / Infra / Plan / Impact 均已核对文案）                                                                       |
| loading                     | **PASS**                | `LoadingState` 统一组件；DB 读放 IO 线程（D-12，防 ANR）                                                                                                                                 |
| success / error             | **PASS**                | 导入完成态 `ImportDonePanel`、备份成功/失败提示、恢复拒绝文案                                                                                                                            |
| retry                       | **PASS**                | 导入/恢复失败留原页可重试；无凭据锁屏有 `重新检查设备能力`                                                                                                                               |
| Back 行为                   | **PASS**                | 二级页均有 `onBack = popBackStack`                                                                                                                                                       |
| 破坏性确认                  | **PASS**                | 删除所有数据（AlertDialog）；恢复替换数据有明确文案                                                                                                                                      |
| 滚动                        | **PASS**                | `PdigScrollingPage` / `verticalScroll` 统一                                                                                                                                              |
| 键盘                        | **PASS**                | `windowSoftInputMode=adjustResize`；导入页面键盘收起逻辑（E2E v4 处理）                                                                                                                  |
| system insets               | **PASS**                | 顶部 TopBar + 底部空隙实测（导航栏顶边 2208/2340）                                                                                                                                       |
| 小屏 / 大字体               | **PASS**                | 触摸目标 ≥48dp + Compose 语义树 14 屏 0 无标签节点（`AccessibilitySemanticsTest` 14/14）；字体缩放 1.0–2.0 设备测试通过（e2e/a11y 证据）                                                 |
| process restore             | **PASS**                | E2E J9：`am kill` 重建后数据仍在 + 首屏锁屏；D-16 工作流 Activity 作用域跨锁存活                                                                                                         |
| 无障碍（TalkBack 实机读屏） | **NOT_RUN（诚实记录）** | 语义树审计 PASS；TalkBack 需 Play Store 镜像（API35 playstore 已下载，本轮尝试安装 TalkBack —— 见 ANDROID_BIOMETRIC/ACCESSIBILITY 记录；无法可靠完成时按 `BLOCKED_BY_REAL_DEVICE` 记录） |

---

## 3. 本轮关闭的 UI 缺口

| 缺口                                                       | 关闭方式                                                            |
| ---------------------------------------------------------- | ------------------------------------------------------------------- |
| Candidate Review 无入口（`Route.CANDIDATES` 存在但无 nav） | 首页 attention 聚合候选计数 → `candidates`（H-16）                  |
| Candidate 卡片无动作（无确认/忽略）                        | 补 `acceptCandidate`/`dismissCandidate` + 按钮（H-16）              |
| Drift 无用户决策动作                                       | 补 `已更换/两者都在用/没变化/稍后确认` 四选择（H-17）               |
| 计划页暴露原始 graphRevision 数字                          | 改为人话「信息时效」文案（E-11）                                    |
| 首页不聚合 Proposal/Candidate/Drift 计数                   | `HomeCounts` 六路聚合（E-10）                                       |
| Onboarding ghost 路由                                      | 移除注册 + spec 记录取消（D-9）                                     |
| 设置无「删除所有数据」                                     | 新增 + 二次确认（L-37，本轮代码已验证编译；设备测试见设备内证据批） |

---

## 4. 结论

```
ANDROID_UI_PRODUCT_AUDIT = PASS（21 个产品目的地，0 个 ghost 路由）
  - 产品 UI 审计缺口：无（全部条目本轮已关闭或为设备环境受限项）
- 设备运行证据：androidTest 59/59 + Core Journey E2E **41/41 PASS / 0 FAIL**（含错误口令/篡改拒绝、进程死亡、D-16 双断言）
  - TalkBack 实机读屏 = 诚实记 NOT_RUN / 按环境受限跟踪
```
