# GENERIC_CSV_ADAPTER.md（MVP02）

> 状态：IMPLEMENTED + TESTED。实现：`core/src/sources/generic-csv/adapter.ts`
> （adapterId=`generic_csv`，version=1，sourceKind=`statement_file`，
> coverageMode=`event_stream`，authoritativeFor=`[]`）。

## 1. 定位

解析任意银行/发卡行导出的 CSV 账单。**只做显式映射**：用户/导入方提供
`MappingProfile`，Adapter 禁止猜测列含义，禁止 LLM 自动 mapping。

## 2. MappingProfile（src/sources/types.ts）

columns（显式列名）：`transactionId? / dateTime / amount? / debit? / credit? /
description? / counterparty? / currency? / balance? / transactionType? /
paymentMethod?`

options：

- `delimiter`：`,` `;` `\t` `|`（支持 quoted delimiter、RFC4180 引号）
- `encoding`：utf-8（BOM 自动剥离）/ gb18030
- `dateFormats`：如 `MM/DD/YYYY`、`DD.MM.YYYY`（错误日期 → bad row，不猜）
- `decimalSeparator`：`.` `,`（EU 逗号小数）
- `amountSignMode`：`signed`（金额列自带符号）/ `debit_credit`（借/贷两列）/
  `outward_positive`（支出为正）
- `positiveDirection`（signed 模式）：`in` | `out` —— **必须显式声明**，
  零金额记为 `neutral`，绝不猜方向

## 3. 语义不变量

- 坏行（bad date / bad amount / 缺映射列）进 `lastParseErrors()`（行号+原因），
  不产出伪造观测、不中断整单。
- `transactionId` 缺失时用 canonical row 指纹（deterministic fallback）。
- multi-currency：currency 列值进入观测元数据，不换汇。
- 相同输入 ×50 输出逐字段一致（determinism 用例）。
- Adapter 只产观测；不写 Graph / 不设 required / 不建 Group / 不调网络。

## 4. 本轮修复的生产缺陷（均有回归测试）

1. `matchFormat()` 正则转义破坏捕获组 → 所有映射 CSV 静默变成 0 观测。
   修复：先切分字面量/token，仅转义字面量再拼装。
2. `positiveDirection` 声明未生效 → "消费记正"的发卡行方向全反。
   修复：按显式声明判定；零金额 = neutral。

## 5. 测试证据

- tests/sources/generic-csv.test.ts：20 用例 / 10 fixtures
  （US signed / EU semicolon / debit-credit / BOM / quoted delimiter /
  CRLF / CR-only / bad date / bad amount / missing mapping /
  同 txn 跨 SourceInstance / multi-currency / ×50 确定性）
- 性能 smoke：10k rows parse 64ms（<10s 阈值，performance-smoke.test.ts）
