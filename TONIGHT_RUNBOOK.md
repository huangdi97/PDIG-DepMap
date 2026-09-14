# TONIGHT_RUNBOOK.md

## 复制到现有项目根目录

- GOAL_MVP01_RC_AUDIT.md
- RC_AUDIT_RULES.md
- QUALITY_GATES.md
- RC_ACCEPTANCE.md
- ZCODE_RC_FIRST_PROMPT.txt

不要覆盖已有：
AGENTS.md / CANONICAL_DESIGN.md / MVP_ACCEPTANCE.md / WORK_STATUS.md / BLOCKERS.md

## ZCode 推荐

如果使用 Goal Mode，输入 `/goal`，Goal：

> 完整执行 GOAL_MVP01_RC_AUDIT.md，严格遵守 AGENTS.md、RC_AUDIT_RULES.md、QUALITY_GATES.md 和 RC_ACCEPTANCE.md。本轮不运行真实数据 Gate；除真正外部工具链/设备 Blocker 外，持续执行 format、lint、typecheck、全部测试、负向测试、安全/隐私/secret/dependency/license/architecture 审计、clean install、clean clone 和可用平台编译，直到所有当前环境可执行 Gate 完成并生成 MVP01_RC_AUDIT_REPORT.md。

然后粘贴 `ZCODE_RC_FIRST_PROMPT.txt`。

## 今晚不要做

- MVP02
- SourceInstance/RealityDrift
- 支付宝/OFX/Open Banking
- Browser Discovery
- AI/Agent
- 真实微信账单

## 判断“今晚代码做完”

根目录必须出现 `MVP01_RC_AUDIT_REPORT.md`，且真实写明：

`MVP01_DEV_CLOSEOUT = PASS`

同时：

`REAL_DATA = NOT_RUN`

每个平台分别写：
IMPLEMENTED / STATIC_AUDITED / COMPILED / TESTED / DEVICE_VERIFIED / STORE_READY

无法编译可以 NO，但必须有 exact blocker。

## 如果中途停

新任务第一句：

> 继续执行 GOAL_MVP01_RC_AUDIT.md。先读取 WORK_STATUS.md、RC_ACCEPTANCE.md、BLOCKERS.md，从第一个未完成 Gate 继续。不要重复已验证步骤，不要进入新功能开发。
