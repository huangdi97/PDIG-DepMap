# PDIG / DepMap — Android R48–R51 本地 Agent 全量实施与验收总 GOAL

> 执行模式：本地持续执行，不是分析报告。直接读取、修改、构建、运行、截图、修复、测试、提交与推送；直到所有可由本机完成的门禁闭环，剩余事项被证据证明必须人工/外部授权。  
> 仓库：`huangdi97/PDIG-DepMap`  
> 主要工作分支：`feat/android-ui-vnext-translation`；PR #1（历史状态为 Draft，须重新核验）。  
> **历史参考**：此前观察到 HEAD `5a0918546461273efcf99684cf0c1302bc0b89b8`，主 CI/iOS/Preview PASS，而 Phone Pixel、Tablet Pixel、Production Rehearsal 未通过。**历史 SHA 和状态只用于定位，不是当前事实。**

## 0. 你的职责与最终目标

你是本机 PDIG Android 产品、UI/UX、Compose、Canonical/Production 集成、自动化测试和运行取证的主执行 Agent。不要停在设计建议、计划、伪代码、截图示例或“下一步请用户操作”。能够在本机完成的事项，应直接实施、验证、修复并推送。

**产品北极星**：PDIG 是 _Personal Digital Infrastructure Graph & Continuity System_，不是普通卡包、手机号管理器、财务记账器或工程 Dashboard。

`Inventory → Typed Dependency → Continuity Analysis → Change Orchestration`

产品要回答：我拥有什么？它们彼此如何依赖？哪里存在经证据支持的薄弱点？某个对象发生变化时有什么影响？我如何安全地完成改变？

本轮设计与工程主线：

`R48 资产体验全量提升 → R49 连续性与变更体验 → R50 个人中心/全局体验 → R51 exact-HEAD 运行/截图/视觉/Production 收口`

地球 UI 已基本接近目标。**不要重做地球，不要退回 generic sphere，不要牺牲已完成 GPU Globe。** 将主要资源投入地球以外的产品界面、交互、信息密度与真实性；只修复地球的真实回归或页面融合问题。

## 1. 第一件事：重新核验仓库事实，保护已有工作

在本机找到用户现有 PDIG 工作目录，不猜一个绝对路径；**只在本机目录工作，优先复用本机既有 SDK、Gradle 缓存、API36 模拟器与证据目录**。不要新建无关工程，不要无故卸载系统镜像、清除模拟器数据或重装 Android Studio。

依次执行并记录：

1. `git status --short`、`git branch -vv`、`git remote -v`、`git fetch --all --prune`；记录工作目录、当前 HEAD、远端工作分支 HEAD、main HEAD、PR #1 状态、最新提交。
2. 检查已有本机改动与其他 Agent 的并行提交：未提交内容先备份、stash 或使用独立 worktree，**绝不 reset --hard、强制 push、盲目覆盖或改写他人提交**。优先在现有工作分支安全同步；有冲突先做三方检查并解决。
3. 核查 GitHub Actions 当前 exact HEAD 的 CI、iOS、Preview APK、Phone Pixel、Tablet Pixel、Production Rehearsal；记录每条 run ID、结论、最后失败步骤和日志位置。不能把历史 PASS 充当新 HEAD PASS。
4. 核对当前源代码究竟推进到 R47、R48 或更远；本 GOAL 的版本号只表示本轮工作包，不是覆盖最新仓库版本的依据。若已完成某项，先验收，不重复实现。
5. 读取最新母版、规范和验收证据（实际存在者）：
   - `PDIG_v2.3-R1_个人数字基础设施图谱与连续性系统_ProductArchitectureVNext_统一全量母版_2026-10-07.md`
   - `ANDROID_UI_VNEXT_CURRENT_REALITY.md`、`ANDROID_REFERENCE_MAPPING.md`、`ANDROID_VISUAL_CONTRACT.md`
   - `ANDROID_UI_VNEXT_R45_SOURCE_REPORT.md`、`ANDROID_UI_VNEXT_R46_REFERENCE_REALITY_AUDIT.md`、更新的 R47 报告/PR 注释
   - `spec/ui-vnext/FIVE_PRIMARY_NAVIGATION_DECISION.md`、`spec/ui-vnext/IA.md`、`spec/ui-vnext/INTERACTION_CONTRACT.md`、`spec/ui-vnext/RESPONSIVE_CONTRACT.json`
   - `spec/ui-vnext/references/` 与 GitHub 现存用户参考图、`spec/ui-vnext/ASSET_CONTINUITY_UX_CONTRACT.md`
   - `spec/proposals/asset-lifecycle-maintenance-v1.md`、`docs/ADR_UI_VNEXT_PRODUCTION_BINDING.md`、Production cutover runbook
   - 当前 Android `uivnext/ui/`、`uivnext/production/`、`uivnext/globe/`、`androidTest`、JVM test 和 `.github/workflows/`。
6. 用户提供的三张视觉参考板为体验准则：亮色资产世界、手机/平板/桌面同一设计体系、卡片身份、号码身份、变更旅程、五个一级入口。**若参考图不在仓库/本机，不可伪造“已对照”；检查已有 reference 映射，缺图记为独立 Human Review 输入缺口，但其余任务继续。**
7. 建立 `artifacts/runtime-evidence/<UTC-or-local-date>-r48-r51/BASELINE.md`：记录事实、日志索引、截图清单、差距矩阵、已有失败、工作树保护策略。之后所有工作以最新实际事实为准。

## 2. 永久不可破坏的产品与真实性边界

### 2.1 五个一级导航不可降级

手机 Bottom Navigation 与平板/Expanded Rail **必须**是：`现在 / 基础设施 / 变更 / 记录 / 我`。

“我”不是 avatar-only、overflow 或设置二级入口；头像只能是其快捷方式。次级页顶部返回是**返回上一级**；Android 系统返回按正确 back stack 返回之前位置，不得无故回桌面/首页。不能因旧母版中的四入口历史快照撤销用户最新五入口决策。

### 2.2 真实来源与权限

- Preview → synthetic fixture → Reference UI；Production → lock-gated `AppContainer` → encrypted Reality → authoritative projection → consumer UI。
- 不允许 Synthetic → Production 漏出；UI/Compose 不可重新计算 Canonical 领域真相。
- `null != 0`；未知 != 安全；未记录不能变成免费/无风险；不从卡名猜 issuer/network，不从号码前缀猜地区/运营商。
- `recoveryOnly != uniqueRecoveryPath`；edge count != FailureDomain-aware independent recovery path；不以关系条数制造 SPOF/安全分/健康分。
- `Current` 是权威读取；`Transition` 是行动进程；`After` 是 **Plan Projection**，并非现实。
- `completeAction != verifyAction`；Make-Before-Break；保留旧活跃号、保号号码、新目标号三者独立身份。
- 变更、建对象、建关系、保号维护、验证、导入复核均通过已有 Authority/Gateway；没有权限的按钮隐藏或禁用，并说明原因。
- 卡片外观、本地图片、自定义别名和隐私遮蔽是 Presentation；不可把敏感资料塞进非治理 `fields_json`、UI KV、PresentationProfile 或未经 schema 迁移的 `.depmap`。
- 生命周期（年费、账单日、还款日、分期、续费、保号等）只有在正式已治理 Persistence 中存在并读取到时才呈现为事实；否则显示“未记录”，不假冒实时银行或运营商状态。
- 默认隐私设置：**允许用户自行开关遮蔽，初始不遮蔽**。开启全局/单对象遮蔽后，卡面、标题、发行方、尾号、图片、关系节点、搜索、副标题、快捷入口都不得侧漏敏感身份；“未知”字段仍保持未知。

### 2.3 视觉方向

Android **light-first、consumer-facing、asset-first、quiet、precise、high information density、spatial where meaningful**。不要把其他页面做成管理后台、工程控制台、全局深空黑、KPI 墙或通用设置行。地球/银行卡材质允许局部深色。除名称及用户自己填写的描述外，产品文案以中文为主。

## 3. P0：先修阻断真实验收的测试/运行缺陷

依据**最新 HEAD** 日志复现，不能硬套下面的历史定位。此前在 `5a091854` 观察到：主 CI、iOS、Preview SUCCESS，但 Phone Pixel、Tablet Pixel、Production Rehearsal FAILURE：

- Phone Pixel：`04i-card-change-transition-blocked` 未出现预期的“尚未选择替代卡片，迁移阶段保持阻断”文案。分清是文案改变、页面导航失败、滚动/异步等待、还是状态语义不正确；修正确切问题。
- Tablet Pixel：`SourceCompleteScreenshotEvidenceTest` 在 `02-overview-global` 捕获失败。检查 Surface/Compose、宽屏层级、Quick Entries semantics、GPU texture ready、测试捕获生命周期与异常 cause；不得只无限延长 sleep 或把断言删掉。
- Production Rehearsal：`ProductionManualRelationshipContractTest.productionFormCommitsCurrentRuntimeRelationThroughAuthority` 触摸输入注入失败。检查滑动/可见性/触摸目标/键盘遮挡/正确节点，不绕过正式关系 Authority。

也检查此前已出现过的测试选择器重复文案、`onFirst()` 与 Compose 版本不匹配、`/usr/bin/sh pipefail`、安卓模拟器分行 shell、Card Detail 长滚动触达 CTA 等历史缺陷是否确实不再回归。

必须达到：日志可复现 → 定位根因 → 生产源码/测试/CI 脚本一起修复 → 本机 exact HEAD 真机/模拟器执行 → 截证 → 提交。**禁止靠删测、跳测、降低断言、伪造截图、强制设 PASS 收口。** 如果本机运行门禁依赖外部凭证，则将安全可测部分执行完，记录唯一的凭证阻断，不终止其他任务。

## 4. R48：资产体验全量重做（本轮主要 UI 工作包）

### 4.1 全局 Design System 与基础设施总览

- 统一全局 header、页面 title/subtitle、卡片背景、容器、圆角、阴影、边框、图标、筛选 chips、搜索、空态、错误、加载、提示、底部导航、平板 Rail；避免每页有不同的组件美学。
- 建议设计 token：8/12/16/24dp 间距节奏；触达区域原则上 ≥48dp；主信息 14–16sp、说明一般 12–13sp，大标题约 22–26sp。不要用大量 9sp 当正文。检查实际字体缩放、长名称和中文排版。
- “基础设施”呈现资产类别、全局地区（仅确认 RegionFact）、关键资产入口与必要的资产数量；不可退化为“8 个工程按钮 + 一屏 KPI”。地区选择应将地球/列表/筛选共享一个状态，并可返回全球。
- 对 `Card / Number / Account / Email / Device / Service` 使用统一视觉体系但不同身份语言：卡是 Financial Asset Identity；号码/邮箱是 Communication Identity；账户是 Access Identity；设备是 Physical Endpoint；服务是 Dependency Endpoint。
- 小屏紧凑但丰富；Medium/Expanded 重排布局而非简单放大手机。

### 4.2 卡片列表与卡片详情

以用户亮色参考板为目标：**手机单列高密度卡片行**，包含高质量卡面缩略图 + 显著名称 + 发行方/尾号/状态/重要生命周期信息；Medium 双列，Expanded 四列或真正合适的断点网格。列表可以按地区、类型、状态筛选；操作不占用大片卡面空间。用户自定义卡面只是详情中的一个小功能。

卡片详情视觉层级：

`Card Identity → Lifecycle → Confirmed Dependencies → Impact Lens → Change / Presentation`。

完成核对并修复：发卡机构、尾号、有效期、年费及到期、账单日、还款日、分期摘要、自动还款状态；全部依权威字段读取，缺失标“未记录”，不推断交易金额/余额/最低还款。已确认关系要能点入，风险/影响有证据，有实际“分析更换此卡”操作。Privacy Mask 要贯穿列表、详情、搜索、关联对象、图谱及卡面图像。对象内添加卡片只在有真实 manual-establish Authority 时展示。

### 4.3 手机号码列表和号码详情（R48 最高优先之一）

不要银行卡化号码：做出**通信身份**主卡/身份面，体现用户自定义 alias（空则显示号码）、地区、运营商（仅已记录）、SIM/eSIM（仅已记录）、primary / secondary / keep 角色。保号是独立正式角色，号码可按地区/角色/当前状态管理。

详情信息：

`Communication Identity → Lifecycle/Keep-alive → Recovery semantics → Confirmed Dependencies → Impact Lens → Change Phone`。

展示账单方式、套餐金额、续期/保号日期、周期、上次保号、续期方法，以及记录来源和更新时间。倒计时仅基于用户已记录 schedule；**到期/逾期不能自动记为“已续费/已保号”**。区分“恢复用途、唯一性未知”和证据确认的“唯一恢复路径”。名单别名必须被搜索索引读取；mask 状态端到端一致。

### 4.4 账户、邮箱、设备、服务

分别整理对象身份 Hero、核心字段、可信资料来源、已确认关系、可解释 Impact、历史与变更入口；不要“一切都套同一块大白卡”。不得制造尚无治理 subtype 的号码/账户语义。通用 `identity_anchor` 保持 generic identity surface，不能默认变手机号。

### 4.5 每屏实际交互

每一个按钮都必须产生真正作用：点开详情、应用筛选、返回上一级、确认改变、展示证据、保存别名或调用 Authority。尤其检查焦点、滚动保持、手机系统返回、软键盘、分页/多选、空态动作、遮蔽及长内容截断。

## 5. R49：连续性、变更、薄弱点与记录（产品差异化最高优先）

### 5.1 变更中心和迁移旅程

把当前“普通步骤列表/切换 tabs”推进成可理解的 Consumer Change Workspace，手机主要使用直观纵向旅程，平板可使用并列当前/过渡/预期布局。

`Current → Transition → After (Plan Projection)` 必须明显区分。顶部放当前对象与目标/备用路径，中部是受影响服务和已确认关系、迁移进度、阻断事项，下部是唯一醒目的“下一步操作”；已完成、待核对、待外部验证、已验证采用不同状态，不得混用。

支持 Change Phone / Change Card，保留 `old active != keep number != new target`，已确认/未知/阻断服务分别标识。切换阶段时不中断上下文，不在未来投影里编造真实完成。必须实际执行并验证 authority 的
`createPlanForScenario → completeAction → verifyAction → readback`；操作失败给清晰解释和恢复路径。缺少 provider automation 则作为指导/核对，不显示虚假一键完成。

### 5.2 Impact Lens / Weaknesses

每个对象能回答“如果它变化？”并展示：已确认依赖、需要关注、关键账户、独立备用路径的证据和未知状态。只消费正式 Impact / Finding engine；FailureDomain 未证明独立性则明确未知。Finding 不是单纯数字列表：要能点入“为何发现 → 基于哪些 Confirmed Reality → 还不知道什么 → 可以做什么”。拒绝健康分、危险的“100%”或自造结论。

### 5.3 记录与证据时间线

按时间、对象、变更计划、动作及验证组织真实历史；明确 `done != verified`。事件展开可展示依据、关联对象、来源、状态、时间。计划上下文不可冒充已经发生的事件；无记录明确说“未记录”，不生成 fake history。对“上一页/详情回退”保留筛选与滚动位置。

## 6. R50：我、全局搜索、隐私、空态与首次体验

- **“我”是第五个一级 Tab**，做成“我的数字生活”：关键身份、跨地区数字存在、连续性概览、个人偏好、Privacy Mask、数据源与未来 Continuity Profile；设置只是其中一部分。Phone/Medium/Expanded 均有真正自适应界面。
- 全局搜索支持已记录：对象名、用户保存的号码别名、issuer、carrier、region、年费、账单日、保号到期、`keep/保号` 角色和合规的关联上下文。支持类型过滤和键盘操作；无结果 ≠ 不存在。结果名称、副标题、issuer、尾号和关系也都必须遵守 global/per-object mask。
- 简洁、可用的 Onboarding ≤3 屏；首次空库应教用户如何建立第一张卡、第一条号码及第一条确认关系。空数据、未知值、加载失败、锁定状态和无权限状态不允许显示 fake 示例计数。
- 用户端不需要底层 Canonical/Schema 专业词汇；文案语义忠实但表达应自然，避免开发者控制台文案。

## 7. R51：本机全量构建、运行、视觉与证据验收

### 7.1 复用本机工具和模拟器

- Windows 环境优先复用现有 `ANDROID_HOME`、JDK、SDK、Gradle 缓存、adb、API36 Phone/Tablet AVD；记录 `adb devices -l`、`emulator -list-avds`、分辨率、密度、API、系统版本。
- 优先使用已有稳定平板配置（历史参考为 1920×1200@240dpi / API36），手机使用现有 API36 Pixel。不要因取证脚本方便而无故清除原有数据。必要时创建**独立隔离的测试 AVD**，写明原因，保留原有 AVD。
- 可先运行已声明的 repo 标准任务（需核对当前 Gradle task 名）：
  - `:core:test`、`:conformance:run`
  - `:app:testDebugUnitTest`、`:app:assembleDebug`
  - `:app:assemblePreviewDebug`、`:app:connectedPreviewDebugAndroidTest`
  - `:app:assembleProductionDebug`、`:app:connectedProductionDebugAndroidTest`
  - 官方 codegen、fixtures、Oracle parity、secret scan、Release/CI 对应检查。
- 每次源码提交后以 exact HEAD 跑本机相关 Gradle、Instrumentation，再对照 GitHub 远端 HEAD 对应的 CI 与 Workflow，不接受不同 SHA 的拼接 PASS。

### 7.2 必须真实取图的页面/状态矩阵

至少覆盖 Phone Compact、Tablet Medium、Expanded/横屏（存在时）；重要屏优先 Android light，dark 可作为辅助状态而不是改变产品方向：

1. 现在：全球地球、地区选择、返回全球、现在要处理、Timeline、资产入口、空态；检查与地球融合。
2. 基础设施：总览、八类入口、全局搜索、地区分布、地区筛选/取消、空态。
3. 卡片：列表/筛选/空态/长名称/多地区/局部遮蔽；详情、生命周期、关联服务、Impact、背景图更换。
4. 号码：列表/alias/keep/保号/到期/空态/遮蔽；详情、恢复用途、唯一性未知、关系、Impact、换号入口。
5. 账户、邮箱、设备、服务：详情、关系视图、完整关系列表及空数据。
6. 变更：Change Center、Phone/Card 的 Current / Transition / After，待开始/阻断/待验证/已验证、失败恢复和完成后返回。
7. 记录：时间线、done/verified、证据详情、空态、过滤与回退。
8. 我：五入口导航可达、关键身份、连续性、数据源、隐私设置、返回交互。
9. Search：关键词/alias/日期/卡尾号/零结果/结果点击、隐私泄漏检查。
10. Production：锁定→解锁→加密 Reality→创建对象→创建确认关系→读取→维护 schedule/occurrence→动作完成→验证→再次读取→重新锁定；无权限情况下正确禁用。

每个测试留：**真实设备/模拟器 PNG、截图 source SHA、设备参数、截图 time、必要的 UI hierarchy、日志、测试报告、错误现场**。不能用 Figma/图片生成器/Preview 渲染冒充真实运行像素；参考板与真实图必须分开存档。

### 7.3 视觉审核要求

对照用户已提供的亮色设计展板，逐屏做 **Reference → Actual → Diff → 原因 → 修改 → Recapture**：

- 消费者视觉层级、布局密度和可读性；不得有文字截断、图层遮挡、奇怪空白和跳动。
- 卡面缩略图、卡片状态、发行方与尾号对齐；号码身份与卡身份明显不同。
- 变更过程的阶段、依赖和下一步操作明确；手机与平板不同布局却同语义。
- 五导航和“我”的优先级不退化。
- Globe GPU、云层/大气/昼夜/地区浮层保持原有表现，仅修真实回归。地区 tether 不是真实依赖边。
- TalkBack/semantics、系统字号调大、无障碍触达、48dp 交互目标、对比度、横屏/分屏与旋转恢复。

机器可核对布局、截断、语义树、截图差异、性能与稳定性；**Human Visual Acceptance 必须留给用户确认，Agent 不得自称人眼验收通过。**

## 8. 工作方式：持续执行直到本机范围真正收口

每轮严格按以下闭环：

`重新读取远端 HEAD → 明确一个缺口 → 修改源码 → 加/改回归测试 → 本机 Gradle/Instrumentation → 运行截图 → 比参考 → 修复 → 提交 → Push → 检查相同 SHA 的 CI → 更新事实报告 → 继续下一个缺口`。

要求：

1. 按 P0 运行阻断 → R48 Asset → R49 Change → R50 Me/Search → R51 全量验收推进。可以按真实依赖关系调整顺序，但不能用“已经做了很多”代替收口。
2. 首次审计需建立 traceability matrix：每个主屏的 Reference requirement、源码实现、测试覆盖、当前截图、真实状态、剩余问题。之后每次提交更新。
3. 对代码使用既有架构与组件，遵循真实跨端 contracts；必要的新 Canonical 字段必须走 `Spec → ADR/Schema → Migration → Golden/Negative fixtures → Conformance → Android/iOS/Harmony/Desktop → UI`。如果需超出当前治理变更权限，产出完整 RFC 和明确 blocker，不许偷偷落私有字段。
4. 不以“改测试期望值”掩盖产品语义错误；只在确认测试因为可见重复节点、滚动位置或旧文案失效时更新测试，并保持同等或更强的业务断言。
5. Windows 脚本注意 PowerShell 与 bash 语法差异、Gradle 进程退出码、adb 多设备精确 `-s`、记录的路径不含敏感个人数据。
6. 每完成一个可审计子阶段生成 commit；保证 commit message 说明范围。同期有其他 Agent 推送时先 fetch、比较/重放，不 force push。PR #1 持续 Draft，未经明确批准不 merge、不改变 ProductionRelease 默认路由、不发布正式商店版、不动真实用户数据。
7. 网络/工具临时失败时重试并定位；没有 GitHub 凭证时完成本机可做任务，保留提交与 push 命令；不能为了“继续”假装已经推送。
8. 只在确实需要密码、不可获得的签名、外部账号授权、真实设备或人工视觉判定时停止相应子任务，其他并行可做项继续。

## 9. 最终交付物（必须推送到仓库，真实证据落盘）

建议按仓库现有目录规范组织，不与已有文件冲突：

- `docs/ui-vnext/R48_ASSET_EXPERIENCE_IMPLEMENTATION_REPORT.md`
- `docs/ui-vnext/R49_CONTINUITY_CHANGE_IMPLEMENTATION_REPORT.md`
- `docs/ui-vnext/R50_ME_SEARCH_IMPLEMENTATION_REPORT.md`
- `docs/ui-vnext/R51_ANDROID_EXACT_HEAD_RUNTIME_REPORT.md`
- `docs/ui-vnext/R48_R51_VISUAL_ACCEPTANCE_MATRIX.md`
- `artifacts/runtime-evidence/<date>-r48-r51/` 内的 Phone、Tablet、Expanded、Production 截图/日志/manifest/diff/哈希。
- 变更的代码、测试、Spec/ADR（如需要）、workflow/脚本、UI tokens、组件契约、PR #1 描述及准确测试状态。
- 一份 `LOCAL_AGENT_HANDOFF.md`：最新 local HEAD、remote HEAD、PR、已做功能、测试精确命令和结果、CI Run URL、剩余差距、阻断原因、用户人工审核需要看的具体截图，以及下一步建议。

## 10. 验收 Gate / 停止条件

最终**逐项报告**，不得只写“全部做完”：

| Gate                     | 满足标准                                                                                |
| ------------------------ | --------------------------------------------------------------------------------------- |
| SOURCE_DESIGN            | R48/R49/R50 规范与实际 Compose 一致，5 导航且所有主屏无幽灵按钮                         |
| REALITY_AUTHORITY        | Preview/Production 隔离、Canonical 字段真实性、维护/变更 Authority 读写与隐私语义不回归 |
| TEST_COVERAGE            | 单测、契约/负向测试、Conformance、Instrumentation 完整覆盖主要交互                      |
| BUILD                    | 当前本地与远端 exact HEAD Android Debug/Preview/Production 编译通过                     |
| RUNTIME_PHONE            | API36 Phone 实际运行旅程与截图证据完整                                                  |
| RUNTIME_TABLET           | API36 Tablet/Expanded 交互、截图、语义测试通过                                          |
| PRODUCTION_REHEARSAL     | lock-gated real-Reality 流程与操作 readback 在隔离测试数据上通过                        |
| CI                       | 当前远端 HEAD 的 CI/Preview/Phone/Tablet/Production（含适用 iOS/Harmony）真实通过       |
| VISUAL_CANDIDATE         | 逐屏 Reference/Actual 差异闭环、无 P0/P1 视觉/交互缺陷，已提交 Human Review 套件        |
| HUMAN_ACCEPTED           | **仅由用户审核确认**；无人为批准时一律 Pending                                          |
| ANDROID_REFERENCE_FREEZE | Human 接受 + 所有技术门禁通过后才能申请 Freeze，不自动宣布                              |
| PRODUCTION_CUTOVER       | 双钥批准/签名/回滚/数据治理门禁全部明确完成后才允许进入，不自动切换                     |

**允许的最终输出**：

- `LOCAL_EXECUTION_COMPLETE = TRUE/FALSE`（本机可做部分是否真正结束）
- `SOURCE_IMPLEMENTED = ...`；`EXACT_HEAD_TESTED = ...`；`RUNTIME_VERIFIED = ...`；`VISUAL_CANDIDATE = ...`
- `HUMAN_ACCEPTED = PENDING`（用户未确认时）
- `ANDROID_REFERENCE_FREEZE = HOLD`（未齐 Gate 时）
- `PRODUCTION_CUTOVER = HOLD`（未获批准时）
- 具体列出无法由本机完成的每个外部阻断，包含 owner、所需动作、相关证据、完成后恢复命令。

**执行命令**：现在开始执行第一轮 GitHub/local 仓库核验。随后直接进入 P0 运行缺陷修复、R48–R50 UI 实施与 R51 全量验收。不要停在方案阶段，不要不断向用户询问“是否继续”；在技术上可完成的范围内循环到所有 Gate 关闭，最后交付真实 APK、截图、源码提交、CI 链接与逐项状态报告。
