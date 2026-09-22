# REAL_DATA_PRIVACY_PROTOCOL.md

> 生成时间：2026-09-22（ANDROID_CANONICAL_FREEZE → Production/Reality Closure 轮）
> 状态：**PROTOCOL READY / NOT EXECUTED（无授权数据）**
> 配套：`REAL_DATA_PILOT_0_PROTOCOL.md` / `ANDROID_REAL_DATA_ACCEPTANCE.md`
> 原则（AGENTS §12 / §17 / §20）：**Raw statement session-only；禁止持久化完整交易历史**。

---

## 1. 数据边界（什么能碰、什么不能碰）

| 数据 | 可处理 | 说明 |
|------|--------|------|
| 用户授权的账单文件（一份） | ✅ 本机解析 | 仅用于本次 pilot；session-only |
| 服务 / 商户名（如「腾讯视频」） | ✅ 可保留 | 用于 Node Resolution 判断 |
| 金额 / 日期 / 币种 | ✅ 统计用 | 不进入任何报告正文 |
| 卡号 / 账号 / 手机号 / 邮箱 | ❌ | 出现即脱敏为 `***` |
| 任何可识别单个人的组合 | ❌ | 报告发布前二次检查 |

---

## 2. 生命周期（session-only）

| 阶段 | 约束 |
|------|------|
| 文件到达 | 用户放入 `local_private/`（gitignored）或设备本机；**立即从版本管理排除** |
| 解析 | 仅导入会话内存；**不落库** |
| 会话结束 | 原始账单与 parsed transaction arrays **销毁**（session-only） |
| 持久化白名单 | 只允许：必要 **fingerprint** / **EvidenceSummary** / **provenance** / **用户确认后的 Reality**（AGENTS §12） |
| 禁止持久化 | 完整交易历史、raw statement、parsed transaction arrays、完整交易时间线 |

> 产品本身即如此（Observation 仅存会话内存）；Pilot-0 协议只是重申并审计这一点。

---

## 3. 处理链（每步约束）

1. **文件到达**：`local_private/`（全忽略）或设备本机；不入库。
2. **解析**：app 内内存解析；日志不输出原始行（AGENTS §17）。
3. **证据采集**：UI 截图若含金额/卡号 → 截图前遮盖或直接不截。
4. **报告**：只输出计数与**脱敏后的服务名**。

---

## 4. 操作流程（backup before / restore after / delete）

| 流程 | 步骤 |
|------|------|
| **Backup before pilot** | 先导出 `.depmap` 加密备份（app 内 Backup 功能，MediaStore 落盘） |
| **Pilot 执行** | 按 REAL_DATA_PILOT_0_PROTOCOL.md §9 |
| **Delete pilot data** | 删除本 pilot 产生的解析产物（导入会话数据）；app 内可逐项删除，或用 L-37「删除所有数据」兜底 |
| **Restore after pilot** | 从步骤 1 的备份恢复（选 `.depmap` → 口令 → 显式确认） |
| **Delete all data** | 设置 → 「删除所有数据」（L-37，已实现并有设备测试 `DeleteAllDataEvidenceTest` 1/1） |
| **原始账单销毁** | pilot 结束后用户确认删除原始文件；删除后 `Get-ChildItem` 复核为空（AGENTS §21 诚实原则） |

---

## 5. 日志红线（AGENTS §17）

禁止任何日志输出：raw CSV row / full user object dump / source transaction id / password /
SQLCipher secret / HUKS·Keychain material / fileEncryptionKey / fpSecret / decrypted depmap / 真实账单内容。
**开发模式也遵守。**

---

## 6. 违规处理

发现任何一处真实数据进入报告 / log / 截图 → 该轮 pilot 作废重跑，
并在 `ANDROID_REAL_DATA_ACCEPTANCE.md` 记录违规类型与修复。

---

## 结论

```text
ANDROID_REAL_DATA_PRIVACY = READY（协议就绪，未执行 —— 无授权数据）
REAL_DATA_CORRECTNESS = BLOCKED_BY_REAL_DATA
REAL_DATA_VALUE       = BLOCKED_BY_REAL_DATA
```

## 配套文档

- `REAL_DATA_PILOT_0_PROTOCOL.md`（执行协议）
- `ANDROID_REAL_DATA_ACCEPTANCE.md`（验收门禁）
- `ANDROID_SECURITY_PRIVACY_FINAL_AUDIT.md`（整体安全审计）
