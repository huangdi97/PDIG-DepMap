# GLOBAL_SECURITY_PRIVACY_AUDIT.md

> PDIG / DepMap —— 全仓工程治理审计 v0.1.0 round（2026-09-23）
> 结论先行：**GLOBAL_SECURITY_PRIVACY = PASS（active scope）**；
> 无新增 INTERNET / analytics / telemetry；日志与持久化不落明文；密钥不进 Git。

## 1. 审计面（与已有单一主题审计的衔接）

- Android 单一主题审计已有：`ANDROID_SECURITY_PRIVACY_FINAL_AUDIT.md`、`ANDROID_PERMISSION_FINAL_AUDIT.md`、
  `ANDROID_SUPPLY_CHAIN_FINAL_AUDIT.md`、`GITHUB_SECRET_PRIVACY_AUDIT.md`、`ANDROID_REAL_DATA_PRIVACY.md`（本轮引用，不复抄）。
- 本文档集中回答 C7 合同项 + Desktop 新面（E6）。

## 2. 网络 / 遥测 / 权限（保持）

- Android manifest：**无 INTERNET 权限**（实测：README 与 perms 审计一致）；无 analytics/telemetry SDK；
- 本轮新增 Desktop：**零网络依赖**（无 http client 依赖；Compose Desktop 不申网络）；
- 权限面不变（storage 类权限仍未引入；文件走系统 picker/SAF）。

## 3. 持久化（C7 / E6 专项）

| 面                                   | 结论                                                                                                                                                                            | 证据                                                     |
| ------------------------------------ | ------------------------------------------------------------------------------------------------------------------------------------------------------------------------------- | -------------------------------------------------------- |
| RAW_FINANCIAL_DATA_LONG_TERM_STORAGE | **= 0**：Observation 仅导入会话内存（:core sources + :repos SourceRepository 语义），持久化只有 Fingerprint / Evidence summary / Proposal 状态 / 图实体 / ImportSession summary | Repository 全部写入路径核对                              |
| Android 本地库                       | SQLCipher 加密（db_passphrase 由 Keystore 包裹，never 明文）                                                                                                                    | AndroidSqliteDriver + DatabaseKeyStore                   |
| Desktop 落盘                         | 仅 `.depmap` = DEPMAP_CONTAINER_V1（Argon2id v19 / AES-256-GCM / RFC 8785 JCS AAD）加密 payload；内存库永不明文落盘                                                             | DepmapFileStore（唯一出口）；桌面无明文 SQLite/JSON 缓存 |
| 密码/密钥                            | 不写日志、不写源码、不写配置文件；Desktop 的“记住本机”为 opt-in 且只存 DPAPI-protected blob（Windows OS 持钥）                                                                  | DeviceUnlockStore + WindowsDpapiSecurityPort             |
| 未来 schema                          | payloadVersion/schemaVersion 超前 → 显式拒绝（spec §42），不猜测、不降级明文                                                                                                    | DepmapFileStore.isFutureSchema + core SchemaVersion      |

## 4. 日志纪律（实测扫描）

- Gate `SENSITIVE_LOGGING = 0`（active scope）；全仓 source 扫描无 `Log/password/secret/token/decrypted/payload` 组合；
- AGENTS §17 禁止项（raw CSV row / user object dump / sourceTxnId / SQLCipher secret / fpSecret / decrypted depmap）在
  `android/**/src/main` 与 `desktop/**/src/main` 无一命中；
- conformance runner 输出 fixture id 与计数，不含数据正文（report GUID 化，见 conformance/README）。

## 5. Secret 入库检查（C2 / G2）

- Gate `HARDCODED_SECRET = 0`（源码无 BEGIN PRIVATE KEY / 口令字面量 / api key / 长 token 字面量）；
- `local_private/`（含 non-prod keystore 构建链）在 .gitignore 中；非填充物不入 Git；
- 唯一口令字面量是 conformance 的**测试向量口令**（fixtures/depmap golden，`depmap-test`），属 goldset 纯数据，非真实凭据；
- 本轮仓库级 `git check-ignore` 复查 + `git ls-files` 复查：无 .env / keystore / p12 / p8 / provisioning 进入 Git（GITHUB_SECRET_PRIVACY_AUDIT 延续）。

## 6. 密码学（不自行发明）

- Desktop 不引入、不实现任何新密码算法：容器协议 100% 来自冻结 :core `DepmapContainer`（BouncyCastle Argon2id + JDK JCE AES-GCM）；
- DesktopSecurityPort 用 Windows DPAPI（Crypt32Util）—— 平台 OS 受保护密钥，非自研 KDF；
- 禁止出现的不安全实践清单：无自制 6 位 PIN 作为 DB 密钥根（Android 用 Keystore；Desktop 用口令 + Argon2id）；
- 若 Desktop 安全无法实现 → `DESKTOP_SECURITY = FAIL`（本轮已实现，见 DESKTOP 报告）。

## 7. 隐私 / 数据最小化

- 接口/UI 只展示当前任务所需字段（row projections 显式列，不整表拉取全字段到 UI）；
- 日志不含请求正文/正文敏感字段；崩溃报告无 SDK（无）→ 无崩溃正文外泄面；
- REAL_DATA 协议（Pilot-0）仍待真人授权账单，本轮**无真实数据**进入仓库/产物。

## 8. 已知/剩余风险（如实）

1. Desktop 未安装商业代码签名证书 → `WINDOWS_CODE_SIGNING = BLOCKED_BY_MISSING_CODE_SIGNING_CERTIFICATE`
   （不创建 self-signed 冒充 publisher；Release Notes 明确 SmartScreen 提示）。
2. Android Preview 使用 NON-PROD 签名链（apksigner v2 verify PASS），**不冒充** Play Production signing；
   `ANDROID_GOOGLE_PLAY_RELEASED` 仍非 PASS。
3. DPAPI 绑定当前 Windows 用户：换用户/重装后“记住本机”失效 → 回退口令输入（设计使然，非缺陷）。
4. 内存中明文口令生命周期：口令只在 Gate/解锁路径的内存字符串中存在，不缓存不持久化；未实现内存清零（Java String 不可变），
   已在 `FUTURE.md` 记录（如改用 char[] 或密码器封装）。

## 9. 结论

- C7 六项全绿：Reality mutation 同事务、revision 权威清单、无偷偷 commit、无长存原始金融数据、
  无 INTERNET/analytics/telemetry、日志无语义泄漏；
- Gate secret/senslog = 0；DESKTOP_SECURITY = **PASS**（容器 + DPAPI 实现，无明文持久化，未为发布降级）。

## 10. v0.1.2 质量迭代收口轮复核（2026-09-24）

- **Gate 复跑**：`node scripts/quality/check-quality.mjs` → **VERDICT PASS exit 0**（2026-09-24T06:56Z），
  `HARDCODED_SECRET=0`、`SENSITIVE_LOGGING=0` 保持；core `npm run check` secret scan **988 files PASS**、
  network gate 0 primitives（130 文件）。
- **secretPattern 例外复核（JUSTIFIED）**：EXCEPTIONS.json 为合法 UTF-8 JSON、reason 无乱码；
  secretPattern 2 条（SmokeRunner.kt:39、DepmapFileStoreTest.kt:21）均为 **fixture 合成口令**，非真实凭据。
- **发布物核验**：Windows 打包产物（NSIS installer 149,537,743 B / portable zip 149,769,364 B，sha256 见
  `PRODUCT_V0_1_2_RELEASE_MANIFEST.md`）+ SBOM `PDIG-0.1.2-SBOM.cyclonedx.json`（CycloneDX 1.5，
  151 components）+ THIRD-PARTY-NOTICES.md + SHA256SUMS.txt 生成；APK sha256
  `5406d9e7edd6f274b5de23752d98451ded94716c88ff049589e4afe6b4716c8e`（NON-PROD 测试签名，非生产签名）。
- **无新增暴露面**：本轮无新增 INTERNET / analytics / telemetry；无真实数据（全合成 fixture）；
  Windows 未签名（SmartScreen 提示如实披露）。
- **已知限制（如实）**：Android 运行时 smoke 本轮 **RUNTIME_ENVIRONMENT_BLOCKED**（2026-09-24 下午起
  本机所有 AVD full startup 静默退出——环境阻塞，不是回归）；Harmony/iOS 不在本轮（harmony 87/91、
  ios 91/91 为 2026-09-19 旧记录）；无 Play 生产发布。
- **结论**：GLOBAL_SECURITY_PRIVACY = **PASS（保持）**；未把 NOT_RUN 写成 PASS，未夹带秘密。
