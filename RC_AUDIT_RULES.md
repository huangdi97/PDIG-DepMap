# RC_AUDIT_RULES.md

## 本轮性质
MVP01 工程收口，不是功能开发。

优先级：
1. correctness
2. security/privacy
3. deterministic behavior
4. tests
5. type safety
6. architecture boundaries
7. buildability
8. maintainability
9. style
10. documentation

## 禁止
禁止为了让 Gate 变绿而：
- 删除/skip 测试
- 降低断言
- 全局 eslint-disable
- 关闭 Core strict
- silent catch
- 把 error 改成 warning
- 硬编码 fake PASS
- 未编译写 compiled
- synthetic 冒充 real data

## 允许
允许修：
- correctness/security/interoperability bug
- test gap
- type/style/maintainability
- dead code
- docs/build config

禁止新增业务范围。

## Critical path
以下必须最高标准：
impact / crypto / parser / fingerprint / proposal / group proposal / repository / migration / validation

## 平台状态
必须区分：
IMPLEMENTED / STATIC_AUDITED / COMPILED / TESTED / DEVICE_VERIFIED / STORE_READY

## Real Data
本轮固定：
- Correctness Gate = NOT_RUN
- Value Gate = NOT_RUN

## 报告
所有结论写可复现证据：command、exit code、test count、file path、build result。
