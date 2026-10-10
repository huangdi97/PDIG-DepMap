# PRODUCT.md — PDIG（UI/UX 上下文）

> **These files are UI/UX context. They DO NOT override the PDIG Canonical Master (`spec/`).**
> 业务语义、Schema、Impact 语义以 `spec/`（Canonical Master）与 `spec/README.md` 为准。
> 本文件只描述「当前产品的用户视角事实」，供 UI/UX 设计使用（impeccable/frontend-design skill 上下文）。

## 1. 一句话

**个人数字基础设施（PDIG）**：在换卡、换号、换邮箱、注销账户之前，告诉用户哪些账户、支付路径和依赖会受到影响，以及应该先处理什么。

## 2. 当前 MVP 唯一核心 Job

> 模拟更换 / 注销一张银行卡（`replace_payment_card` 等支付场景）与更换手机号（`replace_phone_number`，v0.3.0 起 active）。

## 3. 用户（当前事实）

- 单人本地用户（local-first，无账号、无后端、无遥测）。
- 场景：准备更换银行卡/手机号/注销账户前，需要先看清影响与顺序。
- 信任敏感：用户必须能区分「机器推断」与「我确认过的现实」。

## 4. 产品必须回答的问题（信息架构轴）

1. 现在有什么要处理？(attention / findings)
2. 为什么？(what/why/confirmed basis)
3. 如果改变 X 会怎样？(impact)
4. 下一步是什么？(change plan / actions)
5. 完成了吗？验证了吗？(verification — done ≠ verified)

## 5. 语义铁律（UI 必须遵守）

- Observation ≠ Dependency；Proposal ≠ Reality；unknown ≠ required；done ≠ verified。
- 机器永不自动产生 `required` / `must_change`；只有用户确认的现实才能驱动「必须处理」。
- 没有安全分数；没有「100% 安全 / all clear」；unknown 不被展示为 healthy。
- Graph 是内部状态模型，不是主 UI。

## 6. 当前用户可见行为（v0.3.1）

- 打开/新建加密 `.depmap` 本地文件（口令解锁；可选 Windows DPAPI 本机解锁）。
- 导入微信账单 CSV / 通用 CSV（列映射）/ OFX-QFX → 本会话观察 → 候选/依赖建议（Proposal）。
- 确认依赖 / 现实变化（Drift）→ 形成已确认 Reality。
- 场景中心：更换银行卡 / 银行卡即将到期 / 注销银行卡 / 更换手机号。
- 影响分析 → 变更计划（Make-Before-Break：先建立并验证新路径，再移除旧路径）→ 动作 → 验证。
- 备份/恢复（加密 .depmap）；设置；安全；关于。
- 平台：Windows Desktop（Compose Desktop）、Android（Compose/M3）、iOS（SwiftUI）、HarmonyOS（ArkTS/ArkUI）。

## 7. 产品 register

**PRODUCT REGISTER**（高信任基础设施工具）。不是品牌营销页，不是 graph demo，不是记账软件。
