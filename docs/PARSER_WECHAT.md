# PARSER_WECHAT.md — 微信账单 Parser

> 实现源：`core/src/parser/wechat/parser.ts` + `recurring.ts` + `core/src/fingerprint/fingerprint.ts`
> 测试：`core/tests/parser/wechat.test.ts`（18 项 PASS）；fixtures：`core/tests/fixtures/*.csv`（合成数据）

## 输入处理

| 项 | 规则 |
|---|---|
| 编码 | UTF-8 BOM 剥离 → 严格 UTF-8 → 失败回退 GB18030 |
| 表头说明行 | 定位首列 `交易时间` 的列头行；之前全部跳过 |
| 列 | 交易时间/交易类型/交易对方/商品/收/支/金额(元)/支付方式/当前状态/交易单号/商户单号/备注 |
| 金额 | `¥` 前缀、千分位容错；两位小数 |
| 收/支 | 收入/支出/中性交易 |
| 时间 | `YYYY-MM-DD HH:mm:ss`（+08:00）；月/日/时分秒范围校验（`2026-13-40 99:99:99` 判坏行） |
| 退款/撤销 | 保留为观测事实，status 原样；**不参与**周期识别 |
| malformed | 计入 `errors[]`（行号+原因），不抛出、不影响好行 |
| CSV | 引号转义、引号内逗号 |

## Observation（仅内存）

`{source, sourceTxnId, merchantTxnId, occurredAt, merchantRaw, description, amount, currency, direction, paymentMethodRaw, status, note}`
—— 不持久化任何字段；会话结束销毁。

## 指纹（precision 优先）

- 有稳定交易号：`HMAC-SHA256(fpSecret, "wechat:" + sourceTxnId)`
- 无交易号：canonicalRow = 时间|带符号金额|归一商品|归一商户|方向 → sha256；同文件完全重复行追加 ordinal `#1/#2`
- 跨会话去重：`UNIQUE(source, fingerprint)`；1–6 月导入后再导 1–8 月 → 仅 7–8 月为新（已测）
- fpSecret 首次生成后存 meta（平台加密库保护），不绑定设备

## 周期识别（MVP 简单规则）

按商户分组，对“成功支出”观测的相邻间隔：
monthly 28–31 天；quarterly 88–92 天；yearly 360–370 天。
命中率 ≥60% 才成立；confidence = 0.5×间隔命中率 + 0.3×次数因子 + 0.2×金额稳定度。
输出仅为 confidence，**不产生任何 Dependency/required**。

## Node Resolution 前置

未 resolution 的商户（无候选/多候选）不进入 Proposal；
用户确认后创建 service 节点或映射已有节点（三段式 ImportFlow 的阶段 2）。
