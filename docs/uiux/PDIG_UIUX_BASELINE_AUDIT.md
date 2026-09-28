# PDIG_UIUX_BASELINE_AUDIT.md

> PDIG v0.3.1 UI/UX Refinement · 2026-09-28 · 重构前基线审计（spec §15–§18、§80）
> 依据：2026-09-28 重新生成的真实 Baseline 截图（Desktop 16 页 × 5 档；Android 21 屏 × light/dark）+ 源码审查 + 像素量化分析。**无伪精确总分**；每条 finding 按 P0–P3 分级。

## 0. 证据集

- `artifacts/runtime-evidence/2026-09-28-uiux-baseline/desktop-profiles/*.png`（80 帧，全部非空渲染）
- `artifacts/runtime-evidence/2026-09-28-uiux-baseline/android/*.png`（42 帧，light+dark）
- 像素量化：全过程代表性帧的 lightBgRatio / indigoRatio / darkTextRatio（桌面 1280×720@1.0 全页，Android 1080×2400 light 全页）

### 像素量化摘要（客观数据）

| 平台 | lightBgRatio 区间 | indigoRatio | 文本像素占比 |
| --- | --- | --- | --- |
| Desktop actions/plan/verification/backup | 0.78–0.86 | 0.000–0.009 | 0.001–0.003 |
| Desktop home/scenario-setup | 0.37–0.42 | ≈0 | 0.001–0.003 |
| Android 全页 | 0.89–0.99 | 0.000–0.047 | 0.001–0.022 |

→ 双平台都呈现「大面积近白/浅灰背景 + 仅一、两个 #E0E0E8/#F8F8F8 色板」：信息密度低、品牌色几乎缺席、页面几乎全由「圆角浅紫长条」构成（与 spec §17 已知问题完全吻合，且被独立量化证实）。

## 1. P0 —— correctness / unusable / misleading

| # | 页面/平台 | Finding | 证据 |
| --- | --- | --- | --- |
| A0-1 | Desktop 全局 | **数据文件名/研发性文案直接暴露**：首页副标题「当前数据文件：xxx.depmap」、ShotDriver 预设「Legacy WeChat Statement Source」式来源名与 `plan-…`/`shot-*` 残留进入普通页面文案 | HomeScreen.kt:20；ShotDriver 注入数据；截图 sources/plan |
| A0-2 | Desktop/Android/iOS | **状态几乎纯靠颜色**：StatusChip 只有 label+色块，多数行没有 icon/shape 补充；screen reader 无法分辨 | Kit.kt StatusChip；Findings 各平台截图 |
| A0-3 | Desktop Plan/Verification | **make-before-break 无闸门可视表达**：旧路径停用按钮可点且无「必须新路径验证后才能停用」说明（或仅 disabled 无原因） | PlanScreen/VerificationScreen 截图与源码（后续详核） |
| A0-4 | 全局 | **EmptyState = 一行灰字**，没有「这是哪里 / 为何为空 / 下一步」 | Kit.kt EmptyState；restore/candidates 截图 |

## 2. P1 —— serious UX / accessibility

| # | 页面/平台 | Finding |
| --- | --- | --- |
| B0-1 | Desktop | 固定 180dp 左侧文本按钮导航，无图标、无分组、无选中态强化；24 个入口塞在一列，键盘 tab 顺序即导航线性顺序（App.kt） |
| B0-2 | Desktop | 内容区全宽单列：1280px 下已偏空，2560px 下被拉成横向长条、信息密度进一步稀释（截图 2560×1440 可见大留白） |
| B0-3 | Desktop | Infrastructure 只是卡片列表，无 master-detail、无 By Item/By Capability 分段切换，桌面能力完全未用 |
| B0-4 | Desktop | 首页 = 一堆同形 PdigCard（about 8 个圆角紫条，section header 也是紫色块）→「Dashboard of boxes」 |
| B0-5 | Android | 每屏 89–99% 同底色 + 长列 Card；与桌面同病（「generic grouped-card feel」） |
| B0-6 | 全局 | 无 Continuity Rail；Replace Phone / ChangePlan / Verification 用折返的卡片行表达步骤，前后关系不明 |
| B0-7 | 全局 | Findings 没有 what/why/next 结构化：默认即全量信息（dev 类型标签 + 长描述混排），层级弱 |
| B0-8 | Desktop | `--keys` 键盘取证存在环境性失败记录（本轮以键盘 Gate 专项复测兜底） |
| B0-9 | 全局 | 触控/触达规范离散：Android 行高不足 48dp 之处需统一（spec §71：≥44/48），iOS 需 44pt |

## 3. P2 —— visual hierarchy / interaction

| # | Finding |
| --- | --- |
| C0-1 | Radius 滥用：卡片、条、chip 几乎统一大圆角（lg=14 / pill），无半径层级体现「交互边界 vs 分组」 |
| C0-2 | 色彩使用过挤：surfaceVariant 紫灰长条 = 一切容器色，primary 仅在链接/按钮，页面缺层次（只有「底-条」两档） |
| C0-3 | 状态强度不齐：`verified` 与 `done` 视觉同权，`needs_review` 与 warning 无差别（spec §33/§36 要求区分） |
| C0-4 | 无 motion 语义：所有切换硬切，无「步骤前进/验证完成」的短暂反馈窗口（spec §48 允许的 state/step motion 未用） |
| C0-5 | 没有 Empty/Loading/Error/Retry 的成套状态设计；error 仅一条 errorContainer 条带 |

## 4. P3 —— polish / personality

| # | Finding |
| --- | --- |
| D0-1 | 无品牌可识别点：除 indigo 按钮外，把 logo 换掉可成为任一 dashboard（frontend-design §83 判据） |
| D0-2 | 标题/副标题/小节标题全为同字号近似；页级标题（headlineSmall 22）与内容标题（titleSmall 14）之间缺中间层级 |
| D0-3 | Desktop 根目录无 PRODUCT.md / DESIGN.md（impeccable context.mjs 已确认） |
| D0-4 | `PDIG 0.3.0` 硬编码旧版本号文本（App.kt:39），与 0.3.1 不一致 |
| D0-5 | iOS/Harmony 需按本轮设计系统落实（本轮审计时尚未运行，iOS 有 macOS CI 截图；Harmony 无模拟器 → EXTERNAL_GATE） |

## 5. 与 spec §17 已知问题的对照确认

| spec §17 项 | 基线证据 | 状态 |
| --- | --- | --- |
| oversized unused canvas / low density | lightBg 0.78–0.99、2560 横向长条 | CONFIRMED（P1/B0-2） |
| weak hierarchy | 统一卡片列 + 统一字号 | CONFIRMED（P2/C0-1..C0-5） |
| large monochrome/lavender slabs | 主色板 #F8F8F8 + #E0E0E8 | CONFIRMED（P2/C0-2） |
| repeated rounded rectangles / card-list monotony | 8 张同形卡首页 | CONFIRMED（P1/B0-4） |
| weak visual identity | indigoRatio≈0 | CONFIRMED（P3/D0-1） |
| poor desktop space utilization | 2560 全宽单列 | CONFIRMED（P1/B0-2） |
| raw/debug-ish context surfaced | depmap 文件名/plan id/来源英文名 | CONFIRMED（P0/A0-1） |
| master-detail underutilized / mobile-like stacking | Infra 卡片列、无分段 | CONFIRMED（P1/B0-3） |
| iOS/mobile generic grouped-card + gray-on-gray | Android lightBg 0.93–0.99 | CONFIRMED（P1/B0-5） |

## 6. 结论

- 结构性问题集中在 **Desktop 信息架构未桌面化** 与 **全局同一套「圆角紫条卡片」组件语言** 两点；语义正确性（canonical/domain）本轮审计未发现需动域的信号，全部 findings 均为表现层。
- 重构方向：spec §20 Quiet Infrastructure —— 以 Continuity Rail 为唯一视觉 signature，neutral-dominant + indigo accent + 语义状态，Desktop 用 sidebar+master-detail+密度，移动端原生化，全部落地见 `PDIG_UIUX_DIRECTION_FREEZE.md` 与 `PDIG_DESIGN_SYSTEM.md`。

---
剩余待关：本轮结束前清点剩余 P0/P1/P2/P3 见 `UIUX_FINAL_ACCEPTANCE.md`（最终报告）。