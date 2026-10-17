# REFERENCE_VISUAL_CONTRACT_032912cc-aacd-44bf-a9ad-dd90018301f4.md

> HUMAN-APPROVED VISUAL TARGET（spec/ui-vnext/references/032912cc-aacd-44bf-a9ad-dd90018301f4.png，1672×941）。
> 本契约内容**全部来自 Human Visual Review 2026-09-30 / 2026-10-01 + 冻结 spec/ui-vnext**；
> No-Vision Agent **不声称理解图片内容**；逐图像素级注释 = `PENDING_VISION_ANNOTATION`，留待 Vision Reviewer 填写。

## 契约字段（Review §19）

| 字段 | 内容 | 来源 |
| --- | --- | --- |
| dominant object | 空间化地球/Globe 或同类空间主角（spatial stage，非表格面板） | Human Review B/C/§4 + spec DESIGN_TOKENS globe |
| background type | deep-space 环境（near-black/deep navy 基底，蓝仅局部辉光） | Human Review E/§2 + spec colors.canvas/canvasDeep |
| main composition | 左导航 rail → 中央大 Globe 空间舞台 → 右浮动空间检查器 → 底部紧凑动作坞 | Human Review C/G/§4 + spec LAYOUT_CONTRACT components.overview |
| primary-secondary ratio | Globe ≈ 60%+ 内容视觉面积；次级检查器 300–340px 悬浮 | Human Review C/§4 + spec components.overview.spatialInspector* |
| material hierarchy | 环境(L0) → 地球(L1) → 玻璃 chrome(L2) → 实体数据(L3) → 资产身份(L4) → 状态动作(L5) | Human Review D/§18 + spec policy.glassRule |
| lighting hierarchy | 方向光（sunlight/terminator）+ 大气 rim + 局部辉光；克制，非霓虹 | Human Review B/E/§18 + spec colors.atmosphere*/terminator* |
| asset identity | 资产 = 可识别视觉对象（卡 1.586 / 号码身份面 / 地区节点） | Human Review H/J/§7/§14 + spec PRESENTATION_PROFILE_SCHEMA |
| navigation prominence | Primary Rail 68px 收起、选中 subtle glow；基础设施二级 = top segmented context rail | Human Review G/§5 + spec components.nav |
| information density | 低—中；宁可更大留白，不塞满一屏（1920×1080 大画布 + 上下文检查器） | Human Review O + spec typography |
| interaction implication | 点击地区 → 聚焦/过滤/抽屉；Escape 回退；滚动缩放；悬停锚点 | spec INTERACTION_CONTRACT + VNextGlobe 状态机 |
| PENDING_VISION_ANNOTATION | 待 Vision Reviewer 对原图逐项补充（composition/scale/material/lighting 的实际像素证据） | — |
