# ANDROID_RELEASE_DECISIONS_REQUIRED.md

> 生成时间：2026-09-20（Android Product Finalization）
> 目的：把「能自动确定的确定」与「需要用户决定的」在发布前一次性列清，
> 不擅自做不可逆决定（applicationId / 品牌 / 版本号策略）。

---

## 0. 当前实际值（来自 `android/app/build.gradle.kts`）

| 项            | 当前值            | 性质                                                                                                                        |
| ------------- | ----------------- | --------------------------------------------------------------------------------------------------------------------------- |
| applicationId | `com.pdig.app`    | **占位** —— 需要用户正式决定（不可逆，上架后无法改）                                                                        |
| versionName   | `0.1.0-milestone` | 内部里程碑版本名；对外发布需改为正式版本名（如 `1.0.0`）                                                                    |
| versionCode   | `1`               | 数字版本号；发布后每次更新必须增大（Play 要求单调递增）                                                                     |
| MIN_SDK       | `26`              | **已确定**：Android 8.0+（与 `.depmap` 容器、SQLCipher、BiometricPrompt 能力匹配），无需用户决定                            |
| TARGET_SDK    | `34`              | **已确定**：与 compileSdk 34 一致；上架合规层面 targetSdk 34 为安全默认。若 Play 后续强制更高 targetSdk，需提版本重验并更新 |
| compileSdk    | `34`              | **已确定**                                                                                                                  |

---

## 1. 能自动确定 / 已确定（无需用户决策）

| 项               | 值                          | 理由                                                             |
| ---------------- | --------------------------- | ---------------------------------------------------------------- |
| minSdk           | 26                          | 产品能力上限所需（SQLCipher + AES-GCM + BiometricPrompt 均支持） |
| targetSdk        | 34                          | 当前稳定合规线；与底层依赖（Compose 1.7 / AGP 8.x）匹配          |
| versionCode 起点 | 1                           | 首次发布即 1；后续 +1（Play 要求）                               |
| 签名算法         | RSA 4096（keystore 生成时） | 推荐关键位                                                       |

---

## 2. 需要用户决定（PRODUCT_DECISION_REQUIRED）

| #   | 项                                           | 当前占位                                                        | 为什么不自动定                                       | 决定后落在哪             |
| --- | -------------------------------------------- | --------------------------------------------------------------- | ---------------------------------------------------- | ------------------------ |
| R-1 | **APP_DISPLAY_NAME**（桌面图标名）           | `PDIG`（`app_name`）/ `个人数字基础设施图谱`（`app_name_full`） | 涉及最终品牌；且 `PDIG` 为内部代号，可能非最终对外名 | `res/values/strings.xml` |
| R-2 | **APPLICATION_ID**                           | `com.pdig.app`                                                  | **不可逆**：上架后改 applicationId = 变成另一个应用  | `app/build.gradle.kts`   |
| R-3 | **VERSION_NAME（对外）**                     | `0.1.0-milestone`                                               | 需品牌/市场策略                                      | `app/build.gradle.kts`   |
| R-4 | 是否启用 **Play App Signing**                | 未定                                                            | 安全与托管偏好                                       | Play Console             |
| R-5 | 发布渠道（Google Play 首发 / 侧载 / 华为？） | 未定                                                            | 商店策略（华为需 AGC 另配）                          | 商店账号（B5）           |

> R-2/R-3 若保持 `com.pdig.app` / `0.1.0-milestone` 直接发布，会造成不可逆的对外身份问题（应用 id 无法改、版本 0.x 会劝退用户）。**发布前必须确认**。

---

## 3. 建议值（供用户参考，非默认执行）

- `APP_DISPLAY_NAME`：`数字依赖图` 或 `依赖图`（简短、避开「基础设施 / 图谱」这种高概念词）—— 最终以用户品牌决定为准。
- `APPLICATION_ID`：若无自有域名，建议 `io.github.<owner>.<product>` 或与 GitHub 组织一致的命名空间（仅建议，绝不擅自改）。
- `VERSION_NAME`：对外首发建议 `1.0.0`。
- `VERSION_CODE`：`1`。

---

## 4. 改动影响面（若确定修改）

| 改动               | 需要同步验证                                                                                   |
| ------------------ | ---------------------------------------------------------------------------------------------- |
| applicationId 变化 | 全量回归（manifest 引用、SAF/MediaStore、备份文件不包含包名所以无损）；设备内 androidTest 重跑 |
| versionName 变化   | About 页文本（当前硬编码 `0.1.0-milestone（Native Migration）` → 改为从 BuildConfig 读）       |
| display name 变化  | 桌面图标名 + 截图素材                                                                          |

---

## 5. 结论

```
可自动确定：MIN_SDK / TARGET_SDK / versionCode 起点 / 签名算法 —— 已确定
需用户决定：APP_DISPLAY_NAME / APPLICATION_ID / 对外 VERSION_NAME / Play App Signing / 发布渠道
状态标记：ANDROID_RELEASE_DECISIONS_REQUIRED = OPEN（等待用户 R-1..R-5）
```
