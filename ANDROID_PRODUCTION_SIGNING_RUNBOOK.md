# ANDROID_PRODUCTION_SIGNING_RUNBOOK.md

> 生成时间：2026-09-20（Android Product Finalization）
> 当前事实：`NON_PRODUCTION_TEST_SIGNING = PASS`；`PRODUCTION_SIGNING = BLOCKED_BY_MISSING_PRODUCTION_KEYSTORE`。
> 本文档是**步骤手册**，不是「已签名」声明 —— 在用户提供正式 keystore 之前，release 产物保持未签名（如实记录，不伪造）。

---

## 0. 原则（不可妥协）

1. **不生成测试 key 冒充 production key**。仓库内 `local_private/build-chain/pdig-nonprod.jks` 只用于验证签名流水线本身。
2. **keystore 与口令永不进 Git**。`signing/`、`.jks`、`.p12`、口令全部由 `.gitignore` 覆盖；CI 用 Secrets。
3. keystore **丢失 = 无法更新已发布应用**。必须离线双备份（至少两份，异地）。
4. 首次发布前必须决定 **Play App Signing**：Google 托管上传密钥 vs 自持签名密钥（见 §6）。

---

## 1. 用户需要提供 / 决定

| # | 项 | 说明 |
|---|----|------|
| 1 | **生产 keystore**（`.jks` / `.keystore`） | 可用 `keytool` 生成，但应由发布负责人生成并安全保管 |
| 2 | **storePassword / keyPassword / keyAlias** | 三个独立强口令；至少 20 位随机 |
| 3 | **Play App Signing 决策** | 推荐启用（丢失上传密钥可恢复，签名密钥由 Google 托管） |
| 4 | **正式 applicationId** | 当前 `com.pdig.app` 为占位（见 ANDROID_RELEASE_DECISIONS_REQUIRED.md） |

### 1.1 生成样例（供发布负责人参考，不是仓库执行）

```bash
keytool -genkeypair -v \
  -keystore pdign-prod.jks \
  -alias pdign \
  -keyalg RSA -keysize 4096 \
  -validity 10000 \
  -dname "CN=PDIG, OU=PDIG, O=PDIG, L=City, ST=State, C=CN"
# 记录：storePassword / keyPassword / keyAlias -> 密码管理器 + 离线纸质副本
```

---

## 2. 本地 Gradle 接线（不落库）

在 `android/gradle.properties`（本地、不提交）或环境变量中：

```properties
# 本地 gradle.properties（.gitignore 应覆盖或由用户本地持有）
PDIG_PROD_KEYSTORE_PASSWORD=<storePassword>
PDIG_PROD_KEY_ALIAS=<alias>
PDIG_PROD_KEY_PASSWORD=<keyPassword>
```

`app/build.gradle.kts` 的 `signingConfigs` 已存在 `nonProductionTest` 模板；生产签名按同样模式新增 `production`：

```kotlin
signingConfigs {
    create("production") {
        storeFile = rootProject.file(System.getenv("PDIG_PROD_KEYSTORE_PATH") ?: "../signing/pdig-prod.jks")
        storePassword = System.getenv("PDIG_PROD_KEYSTORE_PASSWORD") ?: ""
        keyAlias = System.getenv("PDIG_PROD_KEY_ALIAS") ?: ""
        keyPassword = System.getenv("PDIG_PROD_KEY_PASSWORD") ?: ""
    }
}
```

> ⚠ 生产签名**只在显式 `-PpdigProdSigning=true` 且 keystore 存在**时套用；缺失任一值直接 `error()`，
> 绝不 fallback 到 debug/nonprod（与现有 nonProdSigning 逻辑同构）。

验证已签名产物：

```bash
cd android
# 开启生产签名（keystore 路径 + 三个口令通过环境变量传入）
PDIG_PROD_KEYSTORE_PATH=/secure/pdig-prod.jks \
PDIG_PROD_KEYSTORE_PASSWORD=*** PDIG_PROD_KEY_ALIAS=*** PDIG_PROD_KEY_PASSWORD=*** \
  ./gradlew :app:assembleRelease -PpdigProdSigning=true

# 校验签名
"$ANDROID_HOME/build-tools/34.0.0/apksigner" verify --print-certs app/build/outputs/apk/release/app-release.apk
# 预期：Signer #1 certificate DN 为生产主体，非 CN=Android Debug / androiddebugkey
```

---

## 3. CI secret 注入（GitHub Actions）

`.github/workflows/ci.yml` 增加生产签名 job（**仅在 main 上、且 secrets 齐全时**跑，避免 PR 误签）：

```yaml
  android-release-sign:
    name: Android release (signed, main only)
    if: github.ref == 'refs/heads/main' && secrets.PDIG_PROD_KEYSTORE != ''
    runs-on: ubuntu-latest
    steps:
      - uses: actions/checkout@v4
      - uses: actions/setup-java@v4
        with: { distribution: temurin, java-version: "21" }
      - name: Decode keystore
        env:
          PDIG_PROD_KEYSTORE: ${{ secrets.PDIG_PROD_KEYSTORE_B64 }}
        run: echo "$PDIG_PROD_KEYSTORE" | base64 -d > pdig-prod.jks
      - name: Build signed release
        env:
          PDIG_PROD_KEYSTORE_PATH: ${{ github.workspace }}/pdig-prod.jks
          PDIG_PROD_KEYSTORE_PASSWORD: ${{ secrets.PDIG_PROD_KEYSTORE_PASSWORD }}
          PDIG_PROD_KEY_ALIAS: ${{ secrets.PDIG_PROD_KEY_ALIAS }}
          PDIG_PROD_KEY_PASSWORD: ${{ secrets.PDIG_PROD_KEY_PASSWORD }}
        working-directory: android
        run: ./gradlew :app:assembleRelease -PpdigProdSigning=true
      - uses: actions/upload-artifact@v4
        with:
          name: app-release-signed
          path: android/app/build/outputs/apk/release/app-release.apk
```

> GitHub Secrets：`PDIG_PROD_KEYSTORE_B64`（base64 编码的 keystore）、三个口令。

---

## 4. 备份与轮转

| 项 | 做法 |
|----|------|
| 备份 | keystore 文件 + 三个口令 → 密码管理器 + 离线加密介质×2；**发布后任何丢失都是灾难** |
| 轮转 | Android keystore **不可轮转**（签名密钥固定）。密钥泄露只能换 applicationId 全新发布。因此保管优先级最高 |
| Play App Signing | 启用后上传密钥可被替换；签名密钥由 Google 托管，丢失风险降低 |

---

## 5. Play Console 设置

1. 创建应用（需正式 applicationId）。
2. **App signing**：选「Google 管理签名密钥并保护您的应用」→ 上传密钥 = 生产 keystore 的证书。
3. 上传 AAB（`bundleRelease` 产物）→ Play 自动签名生成 APK。
4. 关闭「应用未经测试标记」前完成内部/封闭测试发布。

---

## 6. Play App Signing 决策

| 选项 | 说明 | 建议 |
|------|------|------|
| **启用 Play App Signing（推荐）** | Play 托管签名密钥（上传密钥=本地生成）；keystore 丢失时可申请替换上传密钥 | ✅ 对单人/小团队开发者损失最小 |
| 自持签名密钥（不启用） | Play 使用你上传的密钥直接签名；丢失 = 无法更新 | 仅当必须完全自持时 |

---

## 7. 当前状态（诚实）

```
NON_PRODUCTION_TEST_SIGNING   = PASS（pdig-nonprod.jks 本地签名后 apksigner verify 通过）
PRODUCTION_SIGNING            = BLOCKED_BY_MISSING_PRODUCTION_KEYSTORE
PRODUCTION_READY_SIGNED_APK   = 无（release 产物当前未签名：app-release-unsigned.apk）
```