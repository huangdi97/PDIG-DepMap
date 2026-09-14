# FINAL_SECURITY_REPORT.md

> PDIG / DepMap — FINAL PRODUCTION CLOSURE V1，PHASE E。
> 覆盖 SECRETS / NETWORK / LOGGING / PERMISSIONS / CRYPTO / DB / BACKUP / DEPENDENCIES / LICENSES / SUPPLY_CHAIN。
> 全部结论对应可复现命令；未执行项明确标注。

---

## 1. 结论

| 维度               | 结果                                                                   |
| ------------------ | ---------------------------------------------------------------------- |
| SECRETS            | **PASS**（404 files 扫描，0 production secrets）                       |
| NETWORK            | **PASS**（118 business source files，0 网络原语；业务网络调用 = 0）    |
| LOGGING            | **PASS**（`core/src` 内 `console.*` = 0；无敏感数据落日志）            |
| PERMISSIONS        | **PASS**（Android 仅 `USE_BIOMETRIC`；app manifest `permissions: []`） |
| CRYPTO             | **PASS**（fail-closed 结构化变异测试全绿）                             |
| DB                 | **PASS**（`check:db-integrity` 6 passed；加密落盘由平台适配器承担）    |
| BACKUP             | **PASS（源码级）**；设备端 **BLOCKED**（B21）                          |
| DEPENDENCIES       | **PASS**（runtime 0 high / 0 critical；dev-only 3 moderate 已登记）    |
| LICENSES           | **PASS**（runtime 依赖 license 明确）                                  |
| SUPPLY_CHAIN       | **PASS**（lockfile 提交、无本地 file 依赖、无 CDN runtime script）     |
| **SECURITY_READY** | **PASS（代码/工程侧）**                                                |
| **PRIVACY_READY**  | **PASS（代码/工程侧）**；正式隐私 URL **BLOCKED**（B12）               |

---

## 2. SECRETS

命令：`npm run check:secrets` → `secret scan PASS (404 files scanned, 0 production secrets)`。

- 扫描集 = tracked + untracked − ignored。
- **文件数量 392 仅为本次工作区上下文的计数**（随工作区状态浮动）；**真正的 Gate 是 production secrets = 0**，本轮为 0。
- 仓库内匹配 `secret` / `credential` 的文件仅两个，且均为合法内容：`core/scripts/check-secrets.mjs`（扫描器自身）、`docs/SECRET_SCAN_REPORT.md`（报告）。

### 敏感文件不得提交（核对）

`git ls-files` 匹配 `.env` / `.keystore` / `.jks` / `.p12` / `.p8` / `mobileprovision` / `.pem` / `.key` / `secret` / `credential` → **无敏感文件被跟踪**。

`.gitignore` 已覆盖：`.env*`、`*.secret(s)`、`secrets/`、`credentials/`、`*.keystore`、`*.jks`、`*.p12`、`*.p8`、`*.cer`、`*.mobileprovision`、`*.provisionprofile`、`*.der`、`*.pem`、`*.key`、`signing/`、`certificates/`、`local_private/*`、`*.sqlite*`、`*.db*`、`*.depmap.json`、`decrypted/`、`tmp_private/`。**本轮新增**：`.gradle/`、`local.properties`、`.kotlin/`、`*.hprof`、`captures/`。

---

## 3. NETWORK（业务网络调用 = 0）

命令：`npm run check:network` → `network gate PASS (118 business source files, 0 network primitives)`。

补充独立扫描（`core/src` + `app/`，`.ts` / `.uts` / `.uvue`）针对 `fetch(` / `axios` / `uni.request` / `XMLHttpRequest` / `OkHttp` / `URLSession` / `http.` → **0 命中**。

平台侧：Android manifest `usesCleartextTraffic="false"`；iOS `UIBackgroundModes: []`；HarmonyOS `requestPermissions: []`。`assets/README.md` 明确「不得使用远程 CDN 加载图标」。

**业务网络调用 = 0 成立。**

---

## 4. LOGGING

- `core/src` 内 `console.log/info/debug/warn/error` 计数 = **0**。
- 敏感字段落日志扫描（`console.*(password|secret|key|ciphertext|plaintext|token)`）= **0**。
- 禁止项核对（raw transaction / raw bill / merchant history dump / transaction ID / password / fpSecret / DB key / fileEncryptionKey / depmap plaintext / full Graph dump）：`core/src` 无任何日志输出面，**不存在上述泄漏路径**。
- 策略文件：`docs/LOGGING_POLICY.md`、`docs/LOGGING_AUDIT.md`。

> 注：`core/src/services/import-coordinator.ts:22` 的 `console` 命中为**注释中的单词**（`SourceInstance → adapter...` 行内说明），非日志调用。

---

## 5. PERMISSIONS（最小权限）

| 平台                                                                  | 实测权限                                               | 判定                                                                                                                                                     |
| --------------------------------------------------------------------- | ------------------------------------------------------ | -------------------------------------------------------------------------------------------------------------------------------------------------------- |
| Android（`platforms/android/core/src/main/AndroidManifest.xml`）      | `android.permission.USE_BIOMETRIC`（唯一）             | PASS —— 无 INTERNET / 无存储 / 无短信；`allowBackup="false"`、`fullBackupContent="false"`、`dataExtractionRules="@null"`、`usesCleartextTraffic="false"` |
| Android（`app/manifest.json` → `app.distribute.android.permissions`） | `[]`（空数组）                                         | PASS                                                                                                                                                     |
| HarmonyOS（`platforms/harmonyos/entry/src/main/module.json5`）        | `requestPermissions: []`（空）                         | PASS —— 注释说明仅需本地生物识别/设备凭据，system 无需额外声明                                                                                           |
| iOS（`app/manifest.json` → `privacyDescription`）                     | `NSFaceIDUsageDescription`、`NSCameraUsageDescription` | **需复核**：见 §5.1                                                                                                                                      |

### 5.1 发现：iOS 相机用途声明与功能一致性

`app/manifest.json` 声明 `NSCameraUsageDescription: "仅在你选择扫描二维码导入时使用..."`。但当前 App 侧导入功能**不可用**（B20），代码中**未实现二维码扫描**。

- **性质**：隐私声明与实际能力不一致（声明了未使用的权限用途）。
- **风险**：商店审核可能因「声明权限但无对应功能」被质询；亦与「least privilege」原则不符。
- **处置**：**本轮不擅自删除**（可能属于规划中的导入路径），登记为 **STORE_METADATA 待决项**，需在提交前与实现对齐。见 `FINAL_STORE_CHECKLIST.md` §隐私。

---

## 6. CRYPTO

协议：Argon2id（KDF）+ AES-256-GCM（AEAD），AAD = `UTF8(RFC8785-JCS({format, formatVersion, kdf, cipher}))`，ciphertext/tag 不进 AAD。`DEPMAP_FORMAT_VERSION = 1`。

结构化变异 fail-closed 测试（`crypto/container-mutation.test.ts` + `unit/crypto-negative.test.ts`）：

| 变异                                 | 期望                                                                     | 结果 |
| ------------------------------------ | ------------------------------------------------------------------------ | ---- |
| wrong password                       | `auth_failed`                                                            | PASS |
| bit-flipped tag                      | `auth_failed`                                                            | PASS |
| nonce mutation                       | `auth_failed`                                                            | PASS |
| salt mutation                        | `auth_failed`                                                            | PASS |
| header mutation（界内 kdf 改动）     | AAD mismatch → 失败                                                      | PASS |
| ciphertext truncation（截断 base64） | `auth_failed`，**不返回 partial**                                        | PASS |
| KDF 极端值（恶意 container）         | 拒绝，不得触发超大内存 KDF（×100 边界拒绝）                              | PASS |
| unsupported version                  | 拒绝                                                                     | PASS |
| wrong password 与密文篡改            | **不可区分**（同为 `auth_failed`）                                       | PASS |
| password 字节语义                    | 使用 password 的**精确 UTF-8 字节**（无 Unicode 归一化）                 | PASS |
| Golden Test Vector（冻结）           | golden container 用 golden password 打开、plaintext 匹配；错一字节即失败 | PASS |

**Crypto fail-closed 成立：任何失败路径均不返回明文、不返回 partial。**

---

## 7. DB

- `npm run check:db-integrity` → **6 passed**。
- 完整性约束（`CHECK`）覆盖关键枚举：`criticality CHECK(required,unknown)`、`state CHECK(active,retired)`、`origin CHECK(manual,proposal)`、`mode CHECK(ANY,ALL)`、`decision CHECK`。
- 唯一键约束：`UNIQUE(from_node, relation, to_node, capability)`、`UNIQUE(group_key)`、`UNIQUE(key)`、`UNIQUE(proposal_key)`、`UNIQUE(source, fingerprint)`。
- **Reality mutation 与 `graphRevision` bump 同事务**（GR-001..012），由 property 测试守护（`revision ≡ 成功 Reality mutation 计数`）。
- 落盘加密（SQLCipher / ArkData relationalStore encrypt）由平台适配器承担，**设备端未验证（BLOCKED）**。

---

## 8. BACKUP

- 容器格式 `DEPMAP_CONTAINER_V1`（`DEPMAP_FORMAT_VERSION = 1`）保持向后兼容；golden vector 冻结不变。
- 备份 = 加密容器；恢复 = 校验 → 解密 → schema 校验 → 迁移 → 事务导入，**失败不产生 partial DB**（由迁移回滚测试覆盖）。
- **设备端导出/恢复不可用（B21：App 端加解密桥接缺失）**；`pages/backup/*` 已如实标注「暂未接入」。

---

## 9. DEPENDENCIES

命令：`npm run check:deps` → `dependency tree: OK` / `lockfile in sync: OK` / `deps gate PASS`。

**runtime 依赖（`dependencies`）= 1 个**：`hash-wasm ^4.12.0`。

`npm audit` 实测：

| 严重度     | 数量  | 说明                                                                                                                                         |
| ---------- | ----- | -------------------------------------------------------------------------------------------------------------------------------------------- |
| critical   | **0** | —                                                                                                                                            |
| high       | **0** | —                                                                                                                                            |
| moderate   | **3** | 全部 **dev-only**：`vitest`、`@vitest/coverage-v8`、`@vitest/mocker` —— GHSA-82fw-gwwq-j7x9（Vitest mocker 路径穿越 / 任意文件读，CVSS 5.9） |
| low / info | 0     | —                                                                                                                                            |

**判定**：**runtime critical/high = 0**，无 Release 阻断。3 个 moderate 均为测试工具链、不进产品运行时；已登记（`docs/DEPENDENCY_AUDIT.md`），处置方式为升级 vitest 至 `>=4.1.11`（非本轮范围，且升级会影响 453 个用例的运行器版本，需独立变更窗口）。

---

## 10. LICENSES

`check:deps` license 快照：

| 包                     | License    |
| ---------------------- | ---------- |
| `hash-wasm`（runtime） | MIT        |
| `@types/node`          | MIT        |
| `@vitest/coverage-v8`  | MIT        |
| `eslint`               | MIT        |
| `fast-check`           | MIT        |
| `globals`              | MIT        |
| `iconv-lite`           | MIT        |
| `prettier`             | MIT        |
| `typescript`           | Apache-2.0 |
| `typescript-eslint`    | MIT        |
| `vitest`               | MIT        |

**runtime 直接依赖 license 全部明确（MIT）**，无未知 / custom license，无 `REVIEW_REQUIRED` 项。`THIRD_PARTY_NOTICES.md` 已存在。

---

## 11. SUPPLY_CHAIN

| 项                     | 实测                                                                          |
| ---------------------- | ----------------------------------------------------------------------------- |
| lockfile committed     | **YES**（`core/package-lock.json`，`lockfileVersion 3`，与 manifest 同步）    |
| packageManager 固定    | `npm@11.3.0`（声明）—— 与实际 `npm 10.9.7` **不一致（发现 F-3）**，未擅自修改 |
| 本地 file 依赖         | **无**                                                                        |
| 未知 tarball 依赖      | **无**                                                                        |
| CDN runtime script     | **无**（`check:network` + `assets/README.md` 双重约束）                       |
| 远程字体               | **无**                                                                        |
| 未经审计的下载执行脚本 | **无**（`core/scripts/*.mjs` 全部为本地分析脚本，无网络调用）                 |

---

## 12. 未执行 / 受阻（明确登记）

| 项                                                          | 状态        | 原因                                         |
| ----------------------------------------------------------- | ----------- | -------------------------------------------- |
| Android SQLCipher / Keystore / BiometricPrompt 真机安全验证 | **BLOCKED** | 无 Gradle 发行版 + 构建依赖不可获取 + 无真机 |
| HarmonyOS HUKS / UserAuth / ArkData 加密运行验证            | **BLOCKED** | SDK 组件需 DevEco SDK Manager 同步           |
| iOS Keychain / LocalAuthentication 运行验证                 | **BLOCKED** | 无 macOS / Xcode                             |
| 正式隐私政策 URL                                            | **BLOCKED** | 需用户提供（B12）                            |
| 正式支持 URL                                                | **BLOCKED** | 需用户提供（B12b）                           |

---

## 13. 复现

```
cd core
npm run check:secrets
npm run check:network
npm run check:db-integrity
npm run check:deps
npx vitest run tests/crypto tests/unit/crypto-negative.test.ts
npm audit
```
