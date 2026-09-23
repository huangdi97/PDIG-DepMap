# REAL_DATA_PILOT_0_REPORT.md

> 生成时间：2026-09-23（ANDROID_2026_PRODUCTION_REALITY_CLOSURE · API36 工程全速收口）
> 依据：粘贴 Goal §35–§44 + 批准契约 G3（如实未执行）。
> 状态：**REAL_DATA_CORRECTNESS / REAL_DATA_VALUE = BLOCKED_BY_REAL_DATA**（本轮无用户授权真实账单）

---

## 1. 本轮为什么未执行

- 用户裁决（Goal 契约确认）：本轮**没有**提供「1 位真实用户 + 1 份本人明确授权的账单」。
- 项目纪律（AGENTS / 粘贴 Goal §84）：synthetic statement **不能冒充** real data；
  没有授权数字签名证明，禁止把模拟数据标记为真实数据验收。

## 2. 已就绪的协议与材料（真实可用）

| 材料 | 位置 | 状态 |
|------|------|------|
| Pilot-0 执行协议（Parser/Node Resolution/Proposal/Impact/产品价值前后对照） | `REAL_DATA_PILOT_0_PROTOCOL.md` | READY |
| 隐私协议（raw statement 会话内存、不持久化交易、delete pilot data/delete all/backup/restore） | `REAL_DATA_PRIVACY_PROTOCOL.md` | READY |
| 指标模板（total/parsed/failed rows、date/amount/currency/description 正确性、stable ID/fingerprint、resolution 统计、false merge 高严重度、proposal 计分、false must_change=P0） | 粘贴 Goal §38–§41 驱动的协议模板 | 就绪 |

## 3. 执行时的判定标准（重申红线）

- `false must_change` = **P0 correctness defect**：发现即 `REAL_DATA_CORRECTNESS = FAIL`，必须修。
- `false merge` = 高严重度问题（不能为 recall 强制 fuzzy merge）。
- Proposal 不得自动升级为 Dependency/Group（Observation ≠ Reality）。
- 结论只能写 `PASS_PILOT_0（scope=Pilot-0, n=1）`；**不得**因一份账单宣称
  `REAL_DATA_VALUE = UNIVERSALLY_VALIDATED`（粘贴 Goal §43/§82）。

## 4. 当前状态

```text
REAL_DATA_CORRECTNESS = BLOCKED_BY_REAL_DATA（未执行；协议就绪）
REAL_DATA_VALUE = BLOCKED_BY_REAL_DATA
REAL_DATA_PILOT_0 = NOT_EXECUTED（如实标注，不以 synthetic 冒充）
```

## 5. 解除方法（用户最小动作）

> HUMAN_REQUIRED：提供 1 份**你本人授权**的真实账单文件（微信/支付宝导出 CSV 或 OFX/QFX 均可；
> 仅用于本机解析验证，原始数据不入库、上传、提交）。完成后按 `REAL_DATA_PILOT_0_PROTOCOL.md` 执行。
> 若暂时无法提供：不阻塞本轮的 Play 工程收口（粘贴 Goal §44）。