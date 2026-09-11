# SECRET_SCAN_REPORT.md — Secret 扫描（RC PHASE O）

> 执行：2026-09-12 · `cd core && npm run check:secrets`（regex + filename fallback；
> gitleaks 本机不可用，fallback 脚本见 core/scripts/check-secrets.mjs）
> 扫描范围：git tracked + working tree 未跟踪（排除二进制），共 160+ 文件。

## 结果

```text
secret scan PASS (160 files scanned, 0 production secrets)
```

## 扫描规则（check-secrets.mjs）

文件名：`.env`、`.env.*`、password/secret/token/credential 命名文件、
keystore/jks/p12/p8/mobileprovision、real-bill 数据文件（csv/ofx/pdf/xlsx）。

内容：`BEGIN PRIVATE KEY` 块、AWS AKIA、GitHub ghp_、Slack xox、
api key 赋值字面量、云凭据字段、带口令的 DB 连接串。

Allowlist（可复现的已知非秘密）：
- `depmap-test` —— Golden Test Vector 口令，公开测试向量（docs/CRYPTO_PROTOCOL.md），非生产秘密
- 测试内 fpSecret 字面量（'secret' / 'fuzz-secret' / 'perf-secret' 等测试夹具）

## 人工复核（git 层）

- `git ls-files | grep -iE "keystore|p12|p8|env$|pem|key$"` → 仅
  `KeystoreSecureKeyAdapter.kt`（AndroidKeyStore API 名称，非密钥文件）
- `local_private/` 只含 README.md（.gitignore 排除数据）
- 无 `*.depmap.decrypted`、无解密产物入库

## 结论

**production secret findings = 0**；真实账单/签名材料/密钥材料 0 入库。
