# ANDROID_VISUAL_CONTRACT.md

> Android Human Main Set 视觉契约（任务书 §42）：无眼 Agent 按契约实现，不自评美感。
> 每屏：Primary object / Secondary object / Information hierarchy / Required visible states /
> Forbidden regressions / Reference desktop screen / Android translation rationale。
> 证据：`artifacts/runtime-evidence/2026-10-02-android-ui-vnext-translation/`（phone + tablet）。

## 00-shell（全局导航）

- **Primary object**：五个一级目的地 `现在 / 基础设施 / 变更 / 记录 / 我`。
- **Hierarchy**：五项均为 root；`我` 不是设置快捷方式，而是「我的数字生活」一级工作区。
- **Phone**：bottom navigation 五项全部直接可见；头像可以作为快捷入口，但不得替代「我」Tab。
- **Medium / Expanded**：primary rail 同样保留五项；数据源/设置是「我」的低频子工具。
- **Back / Up**：System Back = 真实访问历史；Header Up = 产品层级；五个 root 不显示 Header Up。
- **Required states**：`我` 选中态；Settings/Sources 打开时保留「我」父级上下文；详情聚焦流返回 root 后五项恢复。
- **Forbidden**：把「我」降级为头像-only、overflow、二级工具；手机四项/平板五项的不一致 IA。


## 01-now（现在）

- **Primary object**：Global Infrastructure Navigator（纹理地球 Globe hero，强主角，非装饰）。
- **Secondary object**：需要处理 / 进行中的变更 / 即将到来。
- **Hierarchy**：Globe（视觉舞台）→ 需要处理（P0 语义最重）→ 进行中变更 → 即将到来。
- **Required states**：global 视角；地区聚焦态（REGION_SELECTED）由 Globe 交互进入；空态（无关注/无变更/无即将到来）。
- **Forbidden**：8 个统计卡；Globe 退化为静态图片；「一切安全」文案。
- **Reference**：Desktop Now（1920×1080@1.0）。
- **Rationale**：手机 hero 280dp、展开 360dp；三区纵向 feed；COMPACT 不压缩桌面三列。

## 02-overview（基础设施总览）

- **Primary object**：Globe Stage（L1 视觉主导）。
- **Secondary object**：活动轨（Region List 非视觉替代 + 需要处理）、底部快速入口。
- **Hierarchy**：Globe → 活动轨 → 快速入口。
- **Required states**：global；region-selected（filter 生效 + REGION_SELECTED）；无地区数据空态；地球上的地区 callout 必须来自实时相机投影并显示已记录资产 footprint。
- **Forbidden**：纯二维地图替代 Globe；固定角落假标签；地区列表消失（无障碍替代必须保留）。
- **Reference**：Desktop Infrastructure Overview。
- **Rationale**：手机 Globe 上 / 活动轨下 / 快速入口底部；EXPANDED 左右分栏。

## 03-cards（卡片）

- **Primary object**：CardIdentitySystem（AssetCard 卡面：issuer identity / preset 材质 / 遮蔽）。
- **Secondary object**：地区过滤 chips、Grid/List 切换、状态徽标。
- **Hierarchy**：标题与计数 → 过滤 → 网格（2/3/4 列按 breakpoint）。
- **Required states**：global；region 过滤态；Cards Empty（未记录 ≠ 无风险）。
- **Forbidden**：ListItem + 银行名 + ****1234 退化；issuer 不可区分；伪安全空态。
- **Reference**：Desktop Cards。
- **Rationale**：手机 2 列 vertical gallery；MEDIUM 3 列；EXPANDED 4 列；列表视图保留高密度。

## 04-card-detail（卡片详情）

- **Primary object**：卡片视觉身份（Hero AssetCard）。
- **Secondary object**：Identity/状态 → 用卡周期（年费 / 账单日 / 还款日 / 分期）→ 绑定服务 → Impact Lens（如果它发生变化？）→ 变更入口。
- **Hierarchy**：身份最上；生命周期是资产事实层；依赖/Impact 语义中段；操作末段。
- **Required states**：active 卡；expiring_soon 卡（风险条）；年费/账单/分期有值与“未记录”两种 truth state；Impact Lens 必须区分已确认依赖与未知；Unknown != safe 文案。
- **Truth boundary**：生命周期字段是用户已记录资料；缺值必须显示“未记录”，不得从交易、Provider 通用规则或 UI preset 推断。
- **Forbidden**：Desktop 左右两栏在手机压缩成不可读双栏；把「未发现风险」写成「安全」。
- **Reference**：Desktop Card Detail。
- **Rationale**：COMPACT 纵向堆叠；EXPANDED/MEDIUM 两栏（Identity 33% / Info 67%）。

## 05-card-image-ocean / 06-card-image-night（更换卡面图片）

- **Primary object**：Live Preview（实时卡面）。
- **Secondary object**：系统相册入口 + 少量内置图片（原卡面 / 海洋 / 云蓝 / 霞光 / 星夜）。
- **Hierarchy**：标题 / 保存 → Preview → 选择图片；这是小功能，不是工程 Studio。
- **Required states**：ocean != night（视觉 + SHA 互异）；保存后 List / Inspector / Detail / Preview 同一图片；保存仅写本机 PresentationProfile。
- **Privacy**：初始不遮蔽；是否遮蔽由用户控制。相册图片转存为 app-private 安全 JPEG filename，不保存任意 URI/path。
- **Forbidden**：material / layout / hex / internal preset id；宽屏恢复三栏 Inspector；Preview 被图片选择区遮挡；图片写入 .depmap。
- **Reference**：Android R10/R19 consumer card-image decision（Desktop old Studio 仅为历史视觉参考，不再控制 Android Card UX）。
- **Rationale**：COMPACT 填充内容宽；MEDIUM 居中 ≤680dp；EXPANDED 居中 ≤760dp，只增加留白，不增加工程控制。

## 07-numbers（号码）

- **Primary object**：号码行（dial code / carrier / role / recovery / dependency / status 一眼可见）。
- **Secondary object**：List + Inspector 摘要。
- **Hierarchy**：dial code 最强；角色 / 恢复用途 / 明确的唯一恢复证据分层展示。
- **Required states**：global；region 过滤；保号 role；Numbers Empty（未记录 ≠ 无风险）。
- **Forbidden**：号码设计成银行卡；把 recoveryOnly（恢复用途）直接解释成唯一恢复路径。
- **Reference**：Desktop Numbers。
- **Rationale**：communication identity（拨号弧/信号条在详情与 Studio）；高密度列表非卡面。

## 08-number-detail（号码详情）

- **Primary object**：NumberFace（Dial Code 最强）。
- **Secondary object**：状态/角色/用途 → 号码生命周期（资费 / 保号日期 / 保号周期 / 最近操作）→ 恢复用途 → 关联服务 → Impact Lens → 变更入口。
- **Hierarchy**：身份 hero → 生命周期 → 已记录恢复语义 → 服务依赖 → Impact Lens → 变更。
- **Required states**：recoveryOnly 只表示已记录恢复用途；uniqueRecoveryPath 只有显式证据时才显示“唯一恢复”；否则显示“未知”；保号资料有值与“未记录”两种 truth state。
- **Truth boundary**：保号日期/资费属于已记录资料，不代表运营商实时状态；recoveryOnly != uniqueRecoveryPath；缺失事实不得反向渲染成安全结论。
- **Forbidden**：重设计 Number Identity；恢复语义被弱化。
- **Reference**：Desktop Number Detail。
- **Rationale**：保持冻结 identity hierarchy，不压缩为桌面双栏。

## 09-number-studio-travel（号码面定制）

- **Primary object**：Live Preview（communication identity 号码面）。
- **Secondary object**：预设（travel/banking/recovery/…）、属性。
- **Hierarchy**：标题 → Preview → 预设 → 属性。
- **Required states**：country != travel != recovery（SHA 互异）；「preset visual ≠ 语义角色」提示。
- **Forbidden**：chip/银行卡比例/支付网络 motif。
- **Reference**：Desktop Number Studio Travel。
- **Rationale**：手机 Preview-first；预设仅呈现，不改变 PersonalReality role truth。

## 10-change-current / 11-change-transition / 12-change-after（更换手机号）

- **Primary object**：ContinuityRail 6 阶段 + 旧号 → 服务节点 → 新号。
- **Secondary object**：投影选择器（当前/迁移中/计划完成）、阶段明细、风险提示。
- **Hierarchy**：投影横幅（语义）→ 连续性流 → 阶段明细 → 风险。
- **Required states**：
  - current：旧号主号，迁移未开始；
  - transition：验证新号码 + 阶段 6 阻塞（make-before-break）；
  - after：旧号 ghost（已停用·计划），新号主号，未完成服务仍「待处理」，横幅「计划投影 ≠ 现实」。
- **Forbidden**：after 写成「已全部完成 / 迁移成功」；服务节点被连线淹没。
- **Reference**：Desktop Change Phone Transition / After。
- **Rationale**：手机纵向连续性流；服务节点 > 路径；三投影语义经变体 SHA 验证互异。

## 13-cards-empty（卡片空态）

- **Primary object**：EmptyState（semantic illustration + title + description + CTA）。
- **Secondary object**：主 CTA（查看全部卡片）/ 次 CTA（查看号码）。
- **Hierarchy**：illustration → title → description → CTA。
- **Required states**：空态文案不得含「一切安全 / 100% safe」；必须含「未记录 ≠ 无风险」语义。
- **Forbidden**：空白占位；伪安全结论。
- **Reference**：Desktop Cards Empty。
- **Rationale**：按手机空间重新构图（图标 96dp、文案 ≤85% 宽）。

## 14-search-command（搜索 / 命令）

- **Primary object**：搜索字段（真实搜索 card/number/region/service）。
- **Secondary object**：导航命令（Cards/Numbers/Overview/Change Phone/Records/Settings）。
- **Hierarchy**：字段 → 命令 → 结果。
- **Required states**：空查询（命令常驻）；无匹配（「没有匹配…未记录 ≠ 无风险」，不做假结果）。
- **Forbidden**：假搜索 UI；Ctrl+K 作为唯一入口。
- **Reference**：Desktop Command Palette（interaction primitive）。
- **Rationale**：触控入口（TopCommandBar）→ 全屏 SearchScreen；返回语义（back → 上一层）。

## 全局契约

- **触控目标**：bottom nav / chips / studio 预设 / projection 选择器 ≥48dp（a11y 测试断言）。
- **无障碍**：nav contentDescription；Globe 语义描述 + Region List 非视觉替代（a11y 测试断言）。
- **状态三通道**：icon + label + color（StatusBadge / AttentionRow）。
- **Presentation 边界**：PresentationProfile 只落呈现层；测试 fixture 与真实持久化隔离。
