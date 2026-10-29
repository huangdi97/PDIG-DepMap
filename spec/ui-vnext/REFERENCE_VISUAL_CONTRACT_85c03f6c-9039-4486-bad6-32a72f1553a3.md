# REFERENCE_VISUAL_CONTRACT_85c03f6c-9039-4486-bad6-32a72f1553a3.md

> HUMAN-APPROVED VISUAL TARGET（spec/ui-vnext/references/85c03f6c-9039-4486-bad6-32a72f1553a3.png，1672×941）。
> 本契约内容**全部来自 Human Visual Review 2026-09-30 / 2026-10-01 + 冻结 spec/ui-vnext**；
> No-Vision Agent **不声称理解图片内容**；逐图像素级注释 = `PENDING_VISION_ANNOTATION`，留待 Vision Reviewer 填写。

## 契约字段（Review §19）

| 字段 | 内容 | 来源 |
| --- | --- | --- |
| dominant object | 定制工作室中央大尺寸实时预览（被展示的对象：卡片/号码） | Human Review I/§11 + spec customization.previewColumnRatio=0.46 |
| background type | 舞台环境（spotlight 聚光 + soft floor 反射 + 环境辉光；非巨大空黑矩形） | Human Review I/§11 + StudioFrame.drawPreviewStage |
| main composition | 三栏：LEFT 对象库+主题缩略图(22%) / CENTER 实时预览(46%，卡占 65–80%) / RIGHT 用户语言编辑器(32%) | Human Review I/K/§9 + spec customization.*ColumnRatio |
| primary-secondary ratio | 中央预览为绝对主角；右侧编辑器为次级；左侧为导航性列表 | Human Review I/K/§9–§12 + spec customization |
| material hierarchy | 舞台环境(L0–L2) → 资产材质(L4) → 编辑控件(L3/L5) | Human Review §18 + spec policy.glassRule |
| lighting hierarchy | spotlight + 卡片下方 soft floor + 环境辉光；tilt 仅在 reduce-motion 关闭时开启 | Human Review I/§11 + StudioFrame.graphicsLayer |
| asset identity | 预览中的资产 = 真实身份面（材质/主题/布局/显示内容实时反映编辑） | Human Review §7/§8 + CardFace/NumberFace |
| navigation prominence | 无独立导航；保存动作右上；编辑分组清晰（卡面设计/显示内容/隐私） | Human Review §10 + StudioFrame |
| information density | 中；编辑器用用户语言（材质/背景/布局/强调色），删除开发者 token 行 | Human Review §10 + StudioFrame.materialLabel/layoutLabel |
| interaction implication | 每次修改 live preview；保存只写 PresentationProfile | Human Review I/§11 + CustomizationScreen |
| PENDING_VISION_ANNOTATION | 待 Vision Reviewer 对原图逐项补充（composition/scale/material/lighting 的实际像素证据） | — |
