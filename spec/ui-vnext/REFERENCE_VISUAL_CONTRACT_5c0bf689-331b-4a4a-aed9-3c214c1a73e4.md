# REFERENCE_VISUAL_CONTRACT_5c0bf689-331b-4a4a-aed9-3c214c1a73e4.md

> HUMAN-APPROVED VISUAL TARGET（spec/ui-vnext/references/5c0bf689-331b-4a4a-aed9-3c214c1a73e4.png，1672×941）。
> 本契约内容**全部来自 Human Visual Review 2026-09-30 / 2026-10-01 + 冻结 spec/ui-vnext**；
> No-Vision Agent **不声称理解图片内容**；逐图像素级注释 = `PENDING_VISION_ANNOTATION`，留待 Vision Reviewer 填写。

## 契约字段（Review §19）

| 字段 | 内容 | 来源 |
| --- | --- | --- |
| dominant object | 资产身份对象（支付卡资产 或 号码身份面；1.586 卡比例 / 大号 masked number） | Human Review H/J/§7/§14 + spec PRESENTATION_PROFILE_SCHEMA |
| background type | 资产自身程序化材质背景（minimal/matte/glass/metal/region/city/abstract/deep-space 各自不同） | Human Review H/§7 + spec colors.surface*/ocean/land/cityLight |
| main composition | 资产面分层：材质背景 → 发卡行/昵称 → 卡号/号码 → 金融元数据 → 状态叠层；layout 可改位置 | Human Review §8 + CardFace 实现 |
| primary-secondary ratio | 资产面为主体（65–80%）；metadata/状态为次级 | Human Review I/§11 + spec customization.previewColumnRatio |
| material hierarchy | 资产材质（metal 拉丝/glass 高光/matte 颗粒…）> 内容层 > 状态层 | Human Review §7/§8 + CardFace.drawCardFaceBackdrop |
| lighting hierarchy | 顶部 rim 高光 + 局部 accent 洗色；克制 | Human Review §18 + spec colors.textPrimary alpha 覆盖 |
| asset identity | 每张卡/号码拥有可辨识身份：issuer/nickname/masked/network/category/region/currency/status | Human Review H/J + spec PRESENTATION_PROFILE_SCHEMA |
| navigation prominence | 页面级：rail 68px + top segmented context rail；资产面内无多余导航 | Human Review G/§5 + spec components.nav |
| information density | 中；卡更大、一屏更少列（1920 默认 3 列），宁可放大不要缩小 | Human Review O/§9 + spec cardsGrid.columns1920=3 |
| interaction implication | 点击卡/号码 → 详情；进入定制工作室 → 三栏实时编辑 | spec INTERACTION_CONTRACT + CustomizationScreen |
| PENDING_VISION_ANNOTATION | 待 Vision Reviewer 对原图逐项补充（composition/scale/material/lighting 的实际像素证据） | — |
