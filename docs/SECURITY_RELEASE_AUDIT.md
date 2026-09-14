# SECURITY_RELEASE_AUDIT.md — PDIG 发布安全审计（Production RC V1）

> 方法：复用并实跑既有 Gate（`check:secrets` / `check:network` / `check:architecture`）+
> 逐项人工复核 + 本轮新增对象的隐私面审查。
> 状态口径：`PASS` / `FAIL` / `BLOCKED` / `NOT_RUN` / `PARTIAL_WITH_REPORT`

---

## 1. Secret 扫描

```
npm run check:secrets
→ secret scan PASS (355 files scanned, 0 production secrets)
```

**PASS。** 扫描范围含本轮新增文件（`app/services/*.uts`、`app/components/*`、`core/scripts/check-ui.mjs`）。

## 2. 网络审计

```
npm run check:network
→ network gate PASS (103 business source files, 0 network primitives)
```

**PASS。** 业务源码零网络原语（无 `fetch` / `axios` / `uni.request` / `XMLHttpRequest`）。

补充说明：

- 本轮 UI 新增页面（backup / import / sources / candidates / about / privacy）**未引入任何网络调用**。
- `uni.chooseFile` 为系统文件选择器，不涉及网络。
- 无 analytics / telemetry / ads SDK。

## 3. 架构边界

```
npm run check:architecture
→ architecture check PASS (48 files scanned, circular dependencies = 0)
```

**PASS。** Core 48 文件，零循环依赖，4 条边界规则满足。

## 4. 加密

| 项                  | 状态                                         | 证据                                                    |
| ------------------- | -------------------------------------------- | ------------------------------------------------------- |
| Golden Test Vector  | PASS                                         | `core/tests/crypto/depmap.test.ts`                      |
| 容器结构化变异      | PASS                                         | `core/tests/crypto/container-mutation.test.ts`（F1–F6） |
| 错误口令不可区分    | PASS                                         | fail-closed 矩阵（`auth_failed` 不可区分性）            |
| 参数边界预校验      | PASS                                         | bounds 先于 Argon2 执行                                 |
| 算法                | Argon2id v19 + AES-256-GCM + RFC8785 JCS AAD | `docs/CRYPTO_PROTOCOL.md`                               |
| 32-byte derived key | PASS                                         | —                                                       |

**PASS（Core 层）。** 设备端实现（Kotlin / Swift / ArkTS）**未编译**（B1/B2/B3）。

## 5. 数据访问边界（本轮新增）

| 项                                  | 状态           | 说明                                                                                                                                   |
| ----------------------------------- | -------------- | -------------------------------------------------------------------------------------------------------------------------------------- |
| UI 不直接操作 SQLite                | PASS           | `check:ui` U4：24 页 0 命中                                                                                                            |
| Reality mutation 与 revision 同事务 | PASS（源码级） | `resolveDrift` / `acceptProposal` / `acceptCandidate` / `retireDependency` 均在 `transaction()` 内                                     |
| 非 Reality 写入不 bump revision     | PASS（源码级） | `dismissDrift` / `dismissCandidate` / `rejectProposal` 不触碰 revision                                                                 |
| 机器不得产生 required               | PASS（源码级） | `acceptProposal` 只接受 `required` / `unknown` 且由用户选择；`createPlanWithAnalysis` 只把**已确认且 required** 的依赖标为 must_change |

> **重要**：以上为**源码级**判定。UI 未编译（B10），设备端行为未验证。

## 6. 日志

| 项                                                  | 状态                                         |
| --------------------------------------------------- | -------------------------------------------- |
| 生产路径无 `console.log`                            | PASS（本轮删除 `App.uvue` 的 `console.log`） |
| 无 raw transaction / merchant / password / key 输出 | PASS                                         |
| `core/scripts/check-secrets.mjs` 覆盖               | PASS                                         |

## 7. 权限最小化

| 平台                          | 状态                                                                     |
| ----------------------------- | ------------------------------------------------------------------------ |
| Android `AndroidManifest.xml` | STATIC_AUDITED（`docs/PERMISSION_AUDIT.md`）；真机验证 **BLOCKED（B1）** |
| iOS `Info.plist`              | STATIC_AUDITED；**BLOCKED（B3）**                                        |
| HarmonyOS `module.json5`      | STATIC_AUDITED；**BLOCKED（B2）**                                        |

## 8. 依赖与供应链

```
npm run check:deps
→ dependency tree: OK / lockfile in sync: OK / license snapshot: 全 MIT 或 Apache-2.0
→ deps gate PASS（audit: 3 moderate, dev-only）
```

**PASS。** 无运行时 critical / high 漏洞。无未审计的 CDN 运行时依赖。

## 9. 发布配置

| 项                                                    | 状态           |
| ----------------------------------------------------- | -------------- |
| Release build 无 debug menu / demo fixture / test key | PASS（源码级） |
| 无 auth bypass                                        | PASS           |
| 无 verbose 敏感日志                                   | PASS           |
| 无开发端点                                            | PASS           |

## 10. 本轮发现并修复的安全/正确性问题

| #   | 问题                                                                             | 风险                         | 处置                                                           |
| --- | -------------------------------------------------------------------------------- | ---------------------------- | -------------------------------------------------------------- |
| S-1 | `drift.uvue` 以裸 SQL 直接 `UPDATE meta SET graph_revision = graph_revision + 1` | **高**：绕过 Core 事务与语义 | 改为服务层 `resolveDrift`，Reality mutation 与 revision 同事务 |
| S-2 | `App.uvue` 导入不存在的 `checkUnlocked`                                          | 中：编译/运行失败            | 已删除                                                         |
| S-3 | `App.uvue` `console.log`                                                         | 低：Release 日志             | 已删除                                                         |
| S-4 | `db.exec` 与插件接口 `execute` 不匹配（6 处）                                    | 中：运行失败                 | 全部经服务层 `execute`                                         |
| S-5 | `crypto.randomUUID()` 在 App 端不可用                                            | 中：运行失败                 | 由服务层生成 ID                                                |
| S-6 | 导入/备份按钮为**假功能**（点击无效果）                                          | 中：误导用户                 | 明确标注"尚未接入"，不写数据（§76）                            |

## 11. 未验证项（诚实登记）

| 项                                    | 状态        | 原因                         |
| ------------------------------------- | ----------- | ---------------------------- |
| SQLCipher 真实加密路径                | **NOT_RUN** | 无 JDK17 / Android SDK（B1） |
| Keystore / Keychain / HUKS 路径       | **NOT_RUN** | B1 / B2 / B3                 |
| BiometricPrompt / LocalAuthentication | **NOT_RUN** | B1 / B3                      |
| FLAG_SECURE / 隐私屏真机行为          | **NOT_RUN** | B1 / B2 / B3                 |
| 设备端 DB 解密失败 / 篡改行为         | **NOT_RUN** | B1 / B2 / B3                 |

---

## 结论

```
SECRET_SCAN          = PASS
NETWORK_ZERO         = PASS（业务网络调用 = 0）
ANALYTICS/TELEMETRY  = 0
ARCHITECTURE         = PASS
CRYPTO（Core）        = PASS
DATA_ACCESS_BOUNDARY = PASS（源码级）
RELEASE_CONFIG       = PASS（源码级）
DEVICE_SECURITY      = NOT_RUN（B1/B2/B3）

SECURITY_RELEASE_AUDIT = PARTIAL_WITH_REPORT
```
