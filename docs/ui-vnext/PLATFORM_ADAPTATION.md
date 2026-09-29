# PLATFORM_ADAPTATION.md — 四端平台适配（语义一致，像素不同）

> 2026-09-29 · feat/pdig-ui-vnext · 依据 spec §88 + RESPONSIVE_CONTRACT.json
> **铁律**：12FV — 移动端**不复制**桌面像素布局；语义四端一致，形态各端原生。

## 1. Desktop（Compose，锚点端）

| 区域 | 契约值（LAYOUT_CONTRACT / DESIGN_TOKENS） | Desktop 实现 |
| --- | --- | --- |
| Navigation Rail | collapsed 76–88（80）/ expanded ≤188 | rail collapsed 80 / expanded 188（VNextShell） |
| Top Command Bar | 44–52（48） | 48 |
| Page Padding | 24–32 | 24 |
| Main Gap | 16–24 | 20 |
| Overview Globe Stage | 宽 55–65% × 高 65–78%（min 860×650） | 60% × 70%（probe 实测 1024×688 @1920） |
| Activity Rail | 300–380 | 340 |
| Quick Entry | 88–120 | 104 |
| Cards 网格 | @1920 4 列 / @1280 3 列 / 更小 2 列；ratio 1.586、gap 16–20 | 4/3/2 列断点；probe 取证 |
| Card Detail | 身份列 30–36% / 信息区 64–70% | 34% / 62%（右信息区契约内） |
| Customization | 三栏 0.22 / 0.46 / 0.32 | 同契约 |
| 输入 | 键盘优先（Tab/Enter/Space/Escape/Arrow）+ 鼠标 | focusable rail + top + 内容 + Region List |

网格策略：@1920 4 列（cardsGrid.columns1920）、@1280 3 列、small 2 列，gap 16–20。

## 2. Android（Compose）

| 能力 | 契约（RESPONSIVE_CONTRACT android） | 说明 |
| --- | --- | --- |
| 导航 | bottom nav ≤5（一级 now/infrastructure/change/records + settings 折叠） | 不复制桌面 rail |
| Globe | compact globe hero（手机）；tabletLarge = expanded globe | 共享 spatial spec + 平台 renderer |
| 数据面 | attention list / active change；不 glass | L3–L5 保持 solid |
| 触控 | ≥48dp（touchTarget.android = 48） | token 一致 |
| 明暗 | light/dark 双支持 | 深浅两套 |

状态：实现/证据由并行平台 fixer 产出，汇合后补全（PENDING_CONVERGENCE）。

## 3. iOS（SwiftUI）

| 能力 | 契约（RESPONSIVE_CONTRACT ios） | 说明 |
| --- | --- | --- |
| 导航 | NavigationStack + List + sheets + toolbar | 原生形态 |
| iPad | NavigationSplitView（Infrastructure）+ expanded globe | tablet 自适应 |
| Dynamic Type | 不裁切、不隐藏 CTA | 系统字体缩放 |
| 触控 | ≥44pt（touchTarget.ios = 44） | token 一致 |
| 明暗 | light/dark 双支持 | 深浅两套 |

状态：实现/证据由并行平台 fixer 产出，汇合后补全（PENDING_CONVERGENCE）。

## 4. Harmony（ArkUI）

| 能力 | 契约（RESPONSIVE_CONTRACT harmony） | 说明 |
| --- | --- | --- |
| 导航 | ArkUI 原生（一级 ≤5） | 平台原生组件 |
| 自适应 | window width 适配（phone/tablet） | 布局随宽度变化 |
| Globe | 共享 spatial spec + 平台 renderer | 程序化 2.5D |
| 构建 | hvigor assembleHap | 本机可构建（无 hdc 设备） |

状态：实现/证据由并行平台 fixer 产出，汇合后补全（PENDING_CONVERGENCE）。

## 5. 跨端一致性（必须相同）

- 语义：十屏信息架构（IA.md）、Globe 状态机（INTERACTION_CONTRACT）、PresentationProfile（PRESENTATION_PROFILE_SCHEMA）
- testId：`pdig.nav.* / pdig.globe.* / pdig.region.* / pdig.card.* / pdig.phone.* / pdig.change.* / pdig.customization.*`（四端一致）
- token：单一真源 DESIGN_TOKENS.json → codegen 四端 GeneratedPdigV2Tokens
- 行为：区域过滤、Change Phone 6 阶段闸门、Privacy Mask 语义

## 6. 禁止项（Responsive rules）

- 移动端不得出现桌面 rail/三栏 inspector 的像素复制；
- L3–L5 数据面任何断点都不得被 glass 覆盖（colorRule）；
- 触控目标不得低于 48dp（Android）/ 44pt（iOS）；
- 桌面行高 40–48、键盘可达不可因响应式而丢失。
