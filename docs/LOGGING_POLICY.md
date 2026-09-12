# LOGGING_POLICY.md — 日志策略（Engineering Baseline V1）

> 配套审计：`docs/LOGGING_AUDIT.md`（RC 轮实测证据）。本文件定义长期政策。

## 硬规则

1. **业务核心（`core/src/**`）禁止一切日志调用**：`console.*` / `Log.*` / `NSLog` / `HiLog`。
   eslint `no-console: error` 强制（唯一豁免：`core/scripts/**/*.ts` 的 CLI 工具向 stderr/stdout 输出结果）。
2. **任何环境（含开发模式）禁止输出**：
   - raw CSV / raw OFX 行内容
   - 单笔交易内容（merchant 历史明细）
   - source transaction id（明文）
   - password / fpSecret / fileEncryptionKey / SQLCipher key
   - 解密后的 `.depmap` / 数据库明文 / 全图 dump
   - 用户账户凭据
3. **错误对象**：使用 typed error（`DepmapError` 带 `code`；错误 message 不含敏感载荷）。
   保留 `cause` 时必须确保不会把密钥/明文带进上层展示。
4. **ImportSession / Evidence 持久化字段**只允许计数、时间边界、哈希指纹 ——
   持久化审计由 `docs/SECURITY_PRIVACY_REGRESSION_MATRIX.md` 与 db-integrity 测试兜底。

## release 行为

- 业务核心为纯库：无 logger、无 debug 开关。release 构建天然无敏感 debug 输出面。
- 平台壳（Android/iOS/HarmonyOS 安全插件）如需日志，仅允许「操作结果码」级别信息，
  具体约束见 `docs/CROSS_PLATFORM_CONTRACT_AUDIT.md`。

## 审计命令

`npm run check:secrets`（敏感模式扫描）+ 人工 grep（PRE_AUDIT 每轮执行：
`grep -rnE "console\.(log|error|warn)" core/src` 必须为 0）。
