# OFX_QFX_ADAPTER.md（MVP02）

> 状态：IMPLEMENTED + TESTED。实现：`core/src/sources/ofx/adapter.ts`
> （adapterId=`ofx_qfx`，version=1，sourceKind=`statement_file`，
> coverageMode=`event_stream`，authoritativeFor=`[]`）。

## 1. 定位

解析 OFX（SGML v1/v2）与 QFX（Intuit 变体）银行对账单。支持字段：
`FITID / DTPOSTED / TRNAMT / TRNTYPE / NAME / MEMO`（以及 CURDEF、
BANKTRANLIST 结构）。

## 2. 指纹规则

- 有 `FITID`：稳定交易号指纹 `HMAC(fpSecret, adapterId:sourceInstanceId:FITID)`。
- 缺 `FITID`：canonical row fallback（DTPOSTED+TRNAMT+NAME 等规范化拼接），
  同样 deterministic。
- 同 FITID 出现在**不同 SourceInstance** → 两条独立指纹记录（命名空间隔离）；
  同实例内重复 FITID → duplicate。

## 3. 语义不变量

- `DTPOSTED` 非法（bad date）→ bad row（`lastParseErrors()`），不猜日期。
- `TRNAMT` 缺失/空 → bad row，**不伪造 0 元交易**
  （本轮修复：`parseOfxAmount('')` 曾返回 0）。
- malformed XML/SGML 结构 → 坏块计数，能解析的部分照常产出，错误可见。
- 方向取 TRNAMT 符号 + TRNTYPE（DEBIT/CREDIT）校验；不一致记 neutral。
- 不自动创建 Reality：只产观测/证据；确认只能来自用户
  （verificationBasis=user_confirmed 唯一来源是 confirmation-service）。

## 4. 测试证据

- tests/sources/ofx-qfx.test.ts：15 用例 / 8 fixtures
  （basic / multiple transactions / FITID fingerprint / missing FITID fallback /
  invalid date / malformed / negative+positive / QFX / 同 FITID 不同 SourceInstance /
  ×50 确定性）
- 性能 smoke：10k STMTTRN parse 67ms（<10s 阈值）
- 批内重复 FITID 假冲突修复（import-coordinator preview 对齐 insertBatch
  语义）→ 含重复 FITID 的真实账单不再整单失败
