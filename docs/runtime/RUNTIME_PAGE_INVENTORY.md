# RUNTIME_PAGE_INVENTORY.md

> 生成轮：2026-09-26 multiclient runtime / visual evidence sweep（spec §13-§15）
> 方法：**真实扫描客户端导航树**（enum Screen / NavHost routes / ArkUI pages / SwiftPM targets），
> 非凭记忆。平台实现列给出文件与行号，便于复核。
> 本轮口径："所有可导航 production 页面" = 下表逐行必须有 ≥1 张 primary-state 截图 + ≥1 个 runtime reachability 断言（§15）。

---

## 0. Canonical 页面最低集合（spec §14）→ 各端映射

| Canonical 页面（§14） | Desktop | Android | Harmony | iOS |
|---|---|---|---|---|
| Onboarding | 无（产品未实现） | **已删除**（D-9 决策，PdigApp.kt:51 注释） | 无 | 无 |
| Lock / Unlock | GateScreen（新建/打开即解锁存储） | Route.LOCK（故意不注册，作门；PdigApp.kt:56-62） | 无 | 无 |
| Home | Screen.HOME | Route.HOME | pages/Index（占位） | 无 |
| Attention Center | Screen.ATTENTION | Route.HOME（首页聚合"需要处理"） | 无 | 无 |
| Sources | Screen.SOURCES | Route.SOURCES | 无 | 无 |
| Import Source | Screen.IMPORT（向导 stage0） | Route.IMPORT（向导） | 无 | 无 |
| Import Privacy | 同上（隐私承诺文案） | 同上 | 无 | 无 |
| File Selection | AwtDesktopFileOps 原生对话框 | SAF picker（FileWorkflowD16） | 无 | 无 |
| Parse Preview | Screen.IMPORT stage1/2 | Route.IMPORT 阶段预览 | 无 | 无 |
| CSV Mapping | Screen.MAPPING | Route.IMPORT（mapping 阶段） | 无 | 无 |
| Node Resolution | Screen.REVIEW（导入解析） | Route.IMPORT → Route.REVIEW | 无 | 无 |
| Import Review | Screen.REVIEW / Screen.PROPOSALS | Route.REVIEW | 无 | 无 |
| Proposal Review | Screen.PROPOSALS | Route.REVIEW（PendingReview） | 无 | 无 |
| Candidate Review | Screen.CANDIDATES | Route.CANDIDATES（**已有导航器**，HomeScreens.kt:109-113） | 无 | 无 |
| Drift Review | Screen.DRIFTS | Route.DRIFT | 无 | 无 |
| Infrastructure | Screen.INFRA | Route.INFRASTRUCTURE | 无 | 无 |
| Node Detail | Screen.NODE（selectedNodeId） | Route.NODE/{nodeId} | 无 | 无 |
| Scenario Center | Screen.SCENARIOS | Route.SCENARIOS | 无 | 无 |
| Scenario Setup | Screen.SCENARIO_SETUP（selectedScenarioId） | Route.SCENARIO_SETUP/{templateId} | 无 | 无 |
| Impact | Screen.IMPACT（selectedNodeId） | Route.IMPACT/{nodeId} | 无 | 无 |
| ChangePlan | Screen.PLAN（selectedPlanId） | Route.PLAN/{planId} | 无 | 无 |
| Action | Screen.ACTIONS（selectedPlanId） | Route.PLAN（变更计划=动作+验证） | 无 | 无 |
| Verification | Screen.VERIFICATION | Route.PLAN（验证区） | 无 | 无 |
| Timeline | Screen.TIMELINE | Route.TIMELINE | 无 | 无 |
| Backup | Screen.BACKUP | Route.BACKUP | 无 | 无 |
| Restore | Screen.RESTORE | Route.RESTORE | 无 | 无 |
| Settings | Screen.SETTINGS | Route.SETTINGS | 无 | 无 |
| Security | Screen.SECURITY | Route.PRIVACY（隐私/安全） | 无 | 无 |
| About | Screen.ABOUT | Route.ABOUT | 无 | 无 |
| Delete All Data | （BackupScreen 内入口） | Route.SETTINGS（删除数据项） | 无 | 无 |
| Graph View | 无（产品为"图非首页"；Infra 列表） | Route.GRAPH | 无 | 无 |

> iOS 结论（N4 扫描）：`ios/Sources` 只有 PDIGCore/PDIGConformance/PDIGArgon2/PDIGArgon2C/CSQLite 五个库 target，
> **无 @main、无 SwiftUI 屏、无 .xcodeproj/.xcworkspace** → 全部页面 NOT_IMPLEMENTED（见 IOS_RUNTIME_BASELINE_AUDIT.md）。

---

## 1. Desktop（Compose Desktop，Kotlin/JVM）

- 导航模型：无栈，`UiState.screen: Screen`（enum state）+ NavRail 赋值（App.kt:46）；动态参数 = selectedNodeId/selectedPlanId/selectedScenarioId/selectedDriftId（UiState.kt:33-36）。
- 入口：`desktop/app/src/main/kotlin/com/pdig/desktop/Main.kt:18`（mainClass com.pdig.desktop.MainKt）。
- 分派：`App.kt:24 PDIGAppShell`（dataFile==null → GateScreen；否则 AppFrame → KeyedContent when(ui.screen) 23 屏，App.kt:66-90）。
- 状态变体：无 loading；每屏 empty / populated ＋ 全局 notice/error（Kit.kt EmptyState/NoticeStrip/ErrorStrip）。
- 定义文件：`ui/Screens.kt:8-31`（23 个 enum 值 + 7 个 TOP_LEVEL_SCREENS 入口）。

| Page ID | 用户可见名 | 实现（Route enum） | Entry / 定义 | States | 截图 | Runtime 断言 |
|---|---|---|---|---|---|---|
| desktop-gate | 新建/打开 .depmap | —（dataFile==null 分支） | App.kt:25-27；GateScreen.kt:26 | empty | 必 | 是 |
| desktop-home | 首页 | HOME | Screens.kt:8；App.kt:67；HomeScreen.kt | empty/populated | 必 | 是 |
| desktop-attention | 需要处理 | ATTENTION | Screens.kt:9；App.kt:68；AttentionScreen.kt | empty/populated | 必 | 是 |
| desktop-sources | 数据来源 | SOURCES | Screens.kt:10；App.kt:69；SourcesScreen.kt | empty/populated | 必 | 是 |
| desktop-import | 导入 | IMPORT | Screens.kt:11；App.kt:70；ImportScreen.kt+ImportSteps.kt | stage0-3 | 必 | 是 |
| desktop-mapping | CSV 映射 | MAPPING | Screens.kt:12；App.kt:71；MappingScreen.kt | empty/populated | 必 | 是 |
| desktop-review | 待确认 | REVIEW | Screens.kt:13；App.kt:72；ReviewScreen.kt | empty/populated | 必 | 是 |
| desktop-proposals | 待确认关系 | PROPOSALS | Screens.kt:14；App.kt:73；ProposalsScreen.kt | empty/populated | 必 | 是 |
| desktop-candidates | 待确认服务 | CANDIDATES | Screens.kt:15；App.kt:74；CandidatesScreen.kt | empty/populated | 必 | 是 |
| desktop-drifts | 可能发生了变化 | DRIFTS | Screens.kt:16；App.kt:75；DriftsScreen.kt | empty/populated | 必 | 是 |
| desktop-infra | 基础设施 | INFRA | Screens.kt:17；App.kt:76；InfraScreen.kt | empty/populated | 必 | 是 |
| desktop-node-detail | 对象详情 | NODE | Screens.kt:18；App.kt:77；NodeDetailScreen.kt | empty/populated | 必 | 是 |
| desktop-scenarios | 场景中心 | SCENARIOS | Screens.kt:19；App.kt:78；ScenariosScreen.kt | populated | 必 | 是 |
| desktop-scenario-setup | 场景设置 | SCENARIO_SETUP | Screens.kt:20；App.kt:79；ScenarioSetupScreen.kt | empty/populated | 必 | 是 |
| desktop-impact | 影响分析 | IMPACT | Screens.kt:21；App.kt:80；ImpactScreen.kt | unselected/populated | 必 | 是 |
| desktop-plan | 变更计划 | PLAN | Screens.kt:22；App.kt:81；PlanScreen.kt | empty/populated | 必 | 是 |
| desktop-actions | 行动计划 | ACTIONS | Screens.kt:23；App.kt:82；ActionsScreen.kt | empty/populated | 必 | 是 |
| desktop-verification | 验证 | VERIFICATION | Screens.kt:24；App.kt:83；VerificationScreen.kt | empty/populated | 必 | 是 |
| desktop-timeline | 时间线 | TIMELINE | Screens.kt:25；App.kt:84；TimelineScreen.kt | empty/populated | 必 | 是 |
| desktop-backup | 备份 | BACKUP | Screens.kt:26；App.kt:85；BackupScreen.kt | empty/populated | 必 | 是 |
| desktop-restore | 恢复 | RESTORE | Screens.kt:27；App.kt:86；RestoreScreen.kt | empty/populated | 必 | 是 |
| desktop-settings | 设置 | SETTINGS | Screens.kt:28；App.kt:87；SettingsScreen.kt | populated | 必 | 是 |
| desktop-security | 安全 | SECURITY | Screens.kt:29；App.kt:88；SecurityScreen.kt | populated | 必 | 是 |
| desktop-about | 关于 | ABOUT | Screens.kt:30；App.kt:89；AboutScreen.kt | populated | 必 | 是 |

## 2. Android（Kotlin + Compose，NavHost；单 Activity MainActivity）

- Route 定义：`android/app/src/main/kotlin/com/pdig/app/ui/PdigApp.kt:49-82`（object Route）；NavHost 注册 :219-251；startDestination 默认 HOME。
- 门：Lock 不注册（PdigApp.kt:56-62），LockChecking 为冷启动中转（:154）。
- 截图保护：SecureWindow.kt:26-42 敏感路由集合。

| Page ID | 用户可见名 | Route | 注册位置 | States | 截图 | Runtime 断言 |
|---|---|---|---|---|---|---|
| android-lock | 锁定页（门） | LOCK（刻意不注册） | PdigApp.kt:56-62 注释；AppLockNavigationTest | locked | 必 | 是 |
| android-home | PDIG（首页聚合） | home | PdigApp.kt:220；HomeScreens.kt | empty/populated | 必 | 是 |
| android-scenarios | 场景 | scenarios | PdigApp.kt:221；ScenarioCenterScreen | populated | 必 | 是 |
| android-timeline | 即将到来 | timeline | PdigApp.kt:222；TimelineScreen | empty/populated | 必 | 是 |
| android-review | 待确认关系/服务 | review | PdigApp.kt:223；PendingReviewScreen | empty/populated | 必 | 是 |
| android-drift | 可能发生了变化 | drift | PdigApp.kt:224；RealityDriftScreen | empty/populated | 必 | 是 |
| android-candidates | 待确认服务 | candidates | PdigApp.kt:225；CandidateReviewScreen（HomeScreens.kt:109-113 有导航器） | empty/populated | 必 | 是 |
| android-infrastructure | 基础设施 | infrastructure | PdigApp.kt:226；InfrastructureScreen | empty/populated | 必 | 是 |
| android-graph | 图（二级） | graph | PdigApp.kt:227；GraphScreen | empty/populated | 必 | 是 |
| android-sources | 数据来源 | sources | PdigApp.kt:228；SourceManagementScreen | empty/populated | 必 | 是 |
| android-import | 导入 | import | PdigApp.kt:229；ImportScreen（SAF picker，D-16） | stage0-3 | 必 | 是 |
| android-backup | 备份 | backup | PdigApp.kt:230；BackupScreen | empty/populated | 必 | 是 |
| android-restore | 恢复 | restore | PdigApp.kt:231；RestoreScreen | empty/populated | 必 | 是 |
| android-settings | 设置 | settings | PdigApp.kt:232；SettingsScreen | populated | 必 | 是 |
| android-privacy | 隐私/安全 | privacy | PdigApp.kt:233；PrivacyScreen | populated | 必 | 是 |
| android-about | 关于 | about | PdigApp.kt:234；AboutScreen | populated | 必 | 是 |
| android-impact | 影响分析 | impact/{nodeId} | PdigApp.kt:235-238；ImpactScreen | unselected/populated | 必 | 是 |
| android-scenario-setup | 场景设置 | scenario/{templateId} | PdigApp.kt:239-242；ScenarioSetupScreen | populated | 必 | 是 |
| android-plan | 变更计划/动作/验证 | plan/{planId} | PdigApp.kt:243-246；ChangePlanScreen | 多状态 | 必 | 是 |
| android-node | 对象详情 | node/{nodeId} | PdigApp.kt:247-250；NodeDetailScreen | empty/populated | 必 | 是 |

## 3. Harmony（ArkTS Stage Model）

| Page ID | 用户可见名 | 实现 | Entry | States | 截图 | Runtime 断言 |
|---|---|---|---|---|---|---|
| harmony-index | 首页（占位） | pages/Index | harmony/entry/src/main/ets/pages/Index.ets；abilities EntryAbility | placeholder | 必 | 是 |

> 其余页面（场景/影响/计划/导入等）未实现：NOT_IMPLEMENTED（matrix §5 Harmony 列全 NOT_STARTED）。
> 运行时一旦可用（E-9 解除），按 §63/§68 全页面截图补证。

## 4. iOS

| Page ID | 用户可见名 | 实现 | Entry | 截图 | Runtime 断言 |
|---|---|---|---|---|---|
| — | 无 UI（N4 APP GAP） | 无 @main / SwiftUI / xcodeproj | 无 | 无 | 无 |

> 结论见 `IOS_RUNTIME_BASELINE_AUDIT.md`：IOS_N4_APP_NOT_IMPLEMENTED。