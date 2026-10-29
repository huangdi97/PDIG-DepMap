# REFERENCE_VISUAL_CONTRACT_91de8ead-e858-4e69-bb98-6887ce6c9b1e.md

> HUMAN-APPROVED VISUAL TARGET（spec/ui-vnext/references/91de8ead-e858-4e69-bb98-6887ce6c9b1e.png，1672×941）。
> 本契约内容**全部来自 Human Visual Review 2026-09-30 / 2026-10-01 + 冻结 spec/ui-vnext**；
> No-Vision Agent **不声称理解图片内容**；逐图像素级注释 = `PENDING_VISION_ANNOTATION`，留待 Vision Reviewer 填写。

## 契约字段（Review §19）

| 字段 | 内容 | 来源 |
| --- | --- | --- |
| dominant object | 空间迁移图：OLD 号码节点 → 迁移关系通道 → NEW 号码节点 | Human Review N/§16/§17 + ChangePhoneScreen |
| background type | deep-space 环境 + 迁移场景（非普通步骤后台） | Human Review E/N + spec colors.canvas* |
| main composition | 顶部 6-stage 进度；中央 OLD/NEW 身份节点 + 状态关系线；下方详情 inspector 降级 | Human Review N/§16/§17 + ChangePhoneScreen |
| primary-secondary ratio | 迁移图为绝对主角；下方两组列表降级为 detail inspector（非主视觉） | Human Review §17 + ChangePhoneScreen |
| material hierarchy | 环境(L0) → 身份节点(L4) → 关系线/状态(L5) → 详情 inspector(L3) | Human Review §18 + ChangePhoneScreen |
| lighting hierarchy | 状态色 = 第三通道（migrated=green/waiting=amber/blocked=red/not-started=muted/manual=neutral），始终配 icon+label | Human Review N/§16 + ChangePhoneScreen.migrationStatus* |
| asset identity | OLD/NEW = 号码身份节点（masked number / carrier / 承担用途） | Human Review §14/§16 + ChangePhoneScreen nodes |
| navigation prominence | 无独立导航；进度 = 顶部 ContinuityRail | Human Review N/§16 + ContinuityRail |
| information density | 低—中；一眼看出旧号承担什么、新号接管什么、什么不能停 | Human Review N + ChangePhoneScreen |
| interaction implication | 迁移项状态可见（icon+label+color）；验证通过前停用保持禁用 | Human Review N + changeStages.blockReason |
| PENDING_VISION_ANNOTATION | 待 Vision Reviewer 对原图逐项补充（composition/scale/material/lighting 的实际像素证据） | — |
