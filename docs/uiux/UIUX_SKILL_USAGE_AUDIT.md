# UIUX_SKILL_USAGE_AUDIT.md

> PDIG v0.3.1 UI/UX Refinement · 2026-09-28 · 本轮三个 Skill 的真实使用审计（spec §7–§12、§81–§83）

## 1. Skill Registry（本机已装，未重装/未更新）

| Skill | Resolved path | Version/commit | Local/Global | 访问方式 |
| --- | --- | --- | --- | --- |
| `ui-ux-pro-max` | `C:\Users\Kaiser\.agents\skills\ui-ux-pro-max\SKILL.md` | 内置 dbs（79 styles / 192 palettes / 74 fonts / 119 UX guidelines / 22 stacks） | Global（本机） | Skill 加载 + `python .../scripts/search.py` |
| `impeccable` | `C:\Users\Kaiser\.agents\skills\impeccable\SKILL.md` | scripts/context.mjs + reference/（new-work/shape/critique/audit/…） | Global（本机） | Skill 加载 + `node .../scripts/context.mjs` |
| `frontend-design` | `C:\Users\Kaiser\.agents\skills\frontend-design\SKILL.md` | 单文件说明 | Global（本机） | Skill 加载 |

下载/安装：无（spec §9、§10：已装绝不重装；`npm i -g`/`winget`/`choco` 等均未执行——见 LOCAL_DOWNLOAD_AUDIT.md）。

## 2. 使用记录与决策

### 2.1 ui-ux-pro-max —— 独立检索（spec §11 要求的 5 类全覆盖）

| # | 检索 | domain | 结果 | 决策 |
| --- | --- | --- | --- | --- |
| 1 | personal infrastructure continuity recovery trustworthy tool — `--design-system -p PDIG --motion 2 --density 5`（product/style 方向） | design-system | 命中「Hero+CTA 营销页」pattern + Minimalism/Swiss、navy/paid-green palette、Outfit/Work Sans（远程字体） | **REJECTED**（product register 不适用营销页；CJK system-font 政策禁远程字体；navy 与既有 indigo 身份冲突）。**采纳**：Minimalism & Swiss 的「high contrast、grid、functional color」精神、density 5、motion 2（subtle）。→ 见 Direction Freeze |
| 2 | calm precise control plane dashboard（product 方向） | product | 0 命中 | **REJECTED**（无数据库匹配；按规则明确说明 fallback 到内置默认：生产力工具/密码管理器类推荐 Minimalism + Accessible，见 #3） |
| 3 | productivity infrastructure security operations tool（product 方向，重试） | product | Productivity Tool → Flat+micro-interactions；Password Manager → **Minimalism & Swiss + Accessible & Ethical** + Trust blue/neutral；Developer tool → Dark+Swiss | **采纳**：Password Manager 路径最接近 PDIG（本地优先/信任/恢复语境）；primary flat+Swiss，蓝色系信任色 + 中性底。→ 验证既有 indigo（#4C4FD8）符合「Trust blue」family，保留品牌色 |
| 4 | trusted indigo neutral semantic status（color 方向） | color | Patent/IP DB：neutral #475569 + chips/badges；Incident mgmt：status green/red 语义色 | **采纳**：neutral-dominant + 语义 status tone 架构（对应既有 tokens.statusRoles）；**拒绝** 其具体 palette（品牌色不动，见 §21 spec） |
| 5 | status not color only screen reader（accessibility 方向） | ux | Color Only：icon/text + color；Screen Reader 语义；可访问认证（AG 认证不限于认知测试） | **采纳并升级为 Gate**：状态=icon+label+color；错误聚合并聚焦到 summary；本产品不涉及登录认证（local-first），无 AG 冲突 |
| 6 | mobile app navigation touch target hierarchy（mobile/native 方向） | ux | iOS 44pt vs Android 48dp；触控间隔 ≥8dp；heading hierarchy | **采纳**：触控规范 = Android 48dp / iOS 44pt（同步既有 tokens 44min，按平台调 +4）；导航 ≤5 深度的语义保持桌面 8 顶层 + 内容区细分 |
| 7 | desktop app density keyboard navigation master detail（desktop/productivity 方向） | ux | keyboard 全可达 + visible focus；skip link；按视觉顺序 tab | **采纳**：Desktop 键盘=Tab/Shift+Tab/Enter/Space/Escape 全流程 + focus 可见（→ Keyboard Gate）；master-detail = Infrastructure 结构 |
| 8 | navigation rail selection focus（Jetpack Compose stack） | jetpack-compose | 0 命中 | fallback：按 Material 3 NavigationRail 语义实现（图标+文字+选中指示），不视为数据库匹配 |
| 9 | list row navigation accessibility label（SwiftUI stack） | swiftui | .accessibilityLabel；List 优于 ScrollView+VStack；navigationDestination 类型安全；@Environment dismiss | **采纳**：iOS 全 List+NavigationStack+类型安全 destination+accessibilityLabel；避免 ScrollView 包装 ForEach |
| 10 | typography hierarchy sans serif CJK system font（typography 方向） | typography | System-first（Inter 系 fallback system）层级：800/-0.5 标题、600/18 副标、400 body、label tracking | **采纳**：system CJK + weight/size 层级（spec §42；禁下载字体）；用 16/14/12 body 系 + 20/16 section + 22-28 页标题 |
| 11 | empty state guidance first use（empty 方向） | ux | Empty：说明+动作；不空白 | **采纳**：EmptyState 必须「这里是什么 / 为什么为空 / 下一步」，配合 Page State Matrix |

**采用/否决汇总**：ui-ux-pro-max 建议逐条记录如上；总体方向（Swiss/minimal + neutral-dominant + 语义色 + icon/label/color 状态 + 触控/键盘/a11y 规范）采纳并固化为 PDIG 专属 adaptation；营销页 pattern、远程字体、风格 palette 替换拒绝。

### 2.2 impeccable —— 按 spec §12（PRODUCT REGISTER）执行

- `context.mjs --target desktop`（完成一次；符合技能指引「run once per session」）：输出 `NO_PRODUCT_MD → PRODUCT_INIT_REQUIRED`、`PRODUCT.md/DESIGN.md absent`、`MANUAL_DETECTOR_REQUIRED`（无自动 hook，改用手动 detector）、`autonomy directive check`（技能要求先探测用户再推断，但本 Goal 已由用户批准完整 contract，contract 即 brief；首个回复声明该替代）。
- ROLE/REGISTER：PDIG = **Operate / PRODUCT REGISTER**（技能规定「the mode names what the visitor's success looks like」：用户在完成「换卡/换号前看清楚影响并行动」= Operate）。不用 Persuade。→ 不执行 `bolder/delight`，绝不 `overdrive`（spec §12）。
- 命令负荷：`shape`（先做 UX Shape 再写码）→ `critique`（baseline critique）→ `document`（DESIGN.md，从现有代码生成事实）→ `distill`（反对 card-pill 泛滥）→ `clarify`（CTA/UX writing 检查）→ `adapt`（URL/缩放/响应式；native variant）→ `polish` → `harden`（错误/空态/边界）。参考文件加载：new-work（替换视觉世界的路径判断）、shape、critique、distill、clarify、adapt、polish、harden、craft-floor（编辑 UI 前最后一次加载）。
- 结论约束：craft-floor 的质量底线（Icon 纪律、focus、reduced-motion、dark contrast、safe area）全部写入 Design System 章节。
- PRODUCT.md/DESIGN.md 按 spec §13 创建于 repo root，且必须写明「不覆盖 PDIG Canonical Master」。

### 2.3 frontend-design —— 艺术方向（spec §14、§83）

- Purpose / Users / Tone / Constraints / Differentiation / **Signature visual concept** 前置定义 → 唯一基准 = **Continuity Rail**（连续性轨道）。
- 采纳：由 subject matter 出发（数字基础设施/连续性/验证关卡），每屏坚持一个方向不混搭；排版靠层级（system CJK）不靠换字体；「boldness 用在一处」→ Continuity Rail 是那一处，其余 quiet。
- 反 generic 检查（§83）将作为最终 critique 的必答问题。

## 3. Commands/workflow 使用对应（部分后置）

| Skill 阶段 | 交付物位置 | 状态 |
| --- | --- | --- |
| shape / critique（baseline） | `PDIG_UIUX_BASELINE_AUDIT.md`；`PDIG_UIUX_DIRECTION_FREEZE.md` | ✅（下文产出） |
| design-system/token | `spec/ui/design-tokens.json`（vNext）；`PDIG_DESIGN_SYSTEM.md` | ✅（下文产出） |
| document | `DESIGN.md`（repo root） | ✅（下文产出） |
| clarify（CTA/UX writing） | `UIUX_IMPLEMENTATION_REPORT.md`；页面 copy 遵循 `copy-zh.json` | ✅（下文产出） |
| polish / harden / audit | `PDIG_UIUX_FINAL_ACCEPTANCE.md`；`UIUX_VISUAL_REVIEW.md`（每平台） | ✅（下文产出） |
| final critique（§83） | `UIUX_FINAL_ACCEPTANCE.md` 附 frontend-design 五问 | ✅（下文产出） |