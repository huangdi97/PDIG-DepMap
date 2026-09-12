# DATE_TIME_RULES.md — 日期/时间工程规则（Engineering Baseline V1）

## 背景

本项目出现过非法日期问题（RC 轮修复：日历校验）。以下规则永久生效。

## 规则

1. **内部表示**：ISO 8601 字符串，UTC Instant 或带显式 offset（微信账单固定 `+08:00`，
   见 `parseWechatTime` —— 输入语义是中国大陆本地时间，不假装是 UTC）。
2. **禁止依赖 JS Date 自动纠正**：`new Date(2026, 1, 30)` 会静默滚动到 3 月 2 日。
   所有输入日期必须先严格 parse + **分量往返校验**
   （构造后逐分量回读比较，`src/parser/wechat/parser.ts:122` 为参考实现）。
3. **禁止在 domain 内隐式 Date parsing**：domain 只携带 ISO 字符串；解析发生在 parser/adapter 边界。
4. **必测日期边界**：leap year（2028-02-29 合法 / 2026-02-29 非法）、02-30、月界、
   年界、非 leap 世纪（2100-02-28）、时区 offset、DST 切换（微信 `+08:00` 无 DST）。
   现有证据：`tests/parser/wechat.test.ts` 日历校验用例 + `tests/contract/adapter-contract.test.ts` C3
   （occurredAt 必须 `^\d{4}-\d{2}-\d{2}` 且 `Date.parse` 有限）。
5. **Clock / UUID**：`utils/ids.ts` 不做注入抽象；需要 determinism 的调用点显式传 id/时间戳
   （`canonicalGroupKey` 等纯函数不取系统时间）。核心测试禁止依赖 `Date.now`/`Math.random`
   （PRE_AUDIT 扫描 = 0）；crypto randomness 必须真实 CSPRNG（`crypto.randomUUID` / node:crypto），
   测试用固定向量（golden salt/nonce）。
