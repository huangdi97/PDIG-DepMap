# CUSTOMIZATION_MODEL.md — PresentationProfile 定制模型

> 2026-09-29 · feat/pdig-ui-vnext · 契约：PRESENTATION_PROFILE_SCHEMA.json + INTERACTION_CONTRACT §8
> **Presentation Layer 铁律**：定制只影响表现，**绝不**改变 node identity / dependencies / evidence / confirmation；
> **绝不写入 .depmap frozen payload**（DESIGN_TOKENS.json policy.privacyRule）。

## 1. 模型字段（schema 1.0.0）

| 字段 | 类型/取值 | 语义 |
| --- | --- | --- |
| targetType | node \| card \| phoneNumber | 定制对象类型 |
| targetId | string（UI-facing，可 masked） | 本地对象 id |
| themeId | string（deep-space / minimal / region / city / glass / metal / abstract / country / banking / travel / recovery / work / private） | 视觉主题 |
| material | glass \| metal \| matte \| paper \| none | 材质 |
| accentColor | hex | 强调色（默认 #4D74FF） |
| background | { kind: preset\|gradient\|procedural\|bundled-image\|user-image, value } | 背景来源 |
| layout | standard \| minimal \| dense \| editorial | 布局密度 |
| privacy | { maskSensitive, maskLast4, maskNumber } | 遮蔽偏好 |
| visibleFields | string[]（空 = 默认集） | 可见字段 |
| flags | showLogo / showNetwork / showRegion / showCurrency / showStatus / showCarrier / showSimBadge / showRole / showUsageTags / showCountryFlagForNumber | 身份面显示开关 |

Desktop 实现：`desktop/app/src/main/kotlin/com/pdig/uivnext/model/UiVNextModels.kt` `PresentationProfile`
（targetType/targetId/themeId/material/accentColor/backgroundKind/backgroundValue/layout/maskSensitive；
`defaultFor()` 默认 material=glass、accent=#4D74FF、backgroundKind=preset、layout=standard、maskSensitive=true）。

## 2. 存储语义（本地偏好）

- 定制结果 = **本地 app preference**：存储走既有本地偏好机制；**不得**写入 .depmap frozen payload；
- Schema v4 Canonical 零改动；node 领域数据零改动；
- Privacy Mask（全局 mask 所有 last4/号码/账户名）为全局偏好，与 per-profile privacy 并存；
- 预设（preset）只表达视觉身份，**不等于**语义角色（如 travel preset 不改变号码的 recovery 语义）。

## 3. 素材政策

- 素材默认 **bundled local / procedural**（procedural seed、bundled asset key）；
- **禁止远程 URL**（noRemoteImages / noRemoteIconCdn / noRemoteFonts = true）；
- `user-image` 为未来能力占位，本轮不引入导入流程。

## 4. preset visual ≠ semantic role（防混淆规则）

| 视觉 preset | 可能映射语义 | 铁律 |
| --- | --- | --- |
| banking | 银行用途 | 视觉 preset 不改变 role/status/confirmation |
| travel | 旅行用途 | 同上 |
| recovery | 恢复用途 | recoveryOnly 是领域事实，preset 只是外观 |
| private | 隐私强调 | maskSensitive 是本地偏好，不影响领域遮蔽 |

## 5. 定制入口（Desktop 已实现）

- CardCustomization / NumberCustomization：Asset Library → Live Preview → Property Inspector 三栏（0.22/0.46/0.32）；
- Personalization Center：workspace theme / globe theme / navigation density / card+number defaults /
  privacy masking / home modules 显隐重排 / region grouping / motion / reduced effects；
  **P0 critical action 不可隐藏**（交互契约 §9）。
- 保存 = 本地 preference；保存/失败有明确反馈（短 loading + 完成确认 / 错误软底 + 人话 + 重试）。

## 6. 测试与证据

- 定制页纳入 90 帧取证（card-customization / number-customization / personalization-center 各 5 档案）；
- 契约一致性由 PRESENTATION_PROFILE_SCHEMA.json + UI 实现双份校验（并行 fixer 侧按同一 schema）；
- 领域回归：core `npm run check` 487 tests 全绿（定制模型不进入 core/domain）。
