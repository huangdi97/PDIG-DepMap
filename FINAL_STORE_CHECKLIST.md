# FINAL_STORE_CHECKLIST.md

> PDIG / DepMap — FINAL PRODUCTION CLOSURE V1，PHASE O。
> 规则：**不虚构 URL、不虚构开发者账号、不伪造截图、不伪造签名、不擅自上传。**
> `STORE_SUBMITTED = NO`（用户未授权提审）。

---

## 1. 汇总

| 项                     | 状态                                                  |
| ---------------------- | ----------------------------------------------------- |
| STORE_METADATA_READY   | **PARTIAL_WITH_REPORT**（文案就绪；URL / 品牌名缺失） |
| STORE_ASSETS_READY     | **BLOCKED**（图标 / 启动图 / 截图缺失）               |
| STORE_SUBMISSION_READY | **REQUIRES_USER_RELEASE_DECISION**                    |
| STORE_SUBMITTED        | **NO**                                                |

---

## 2. 逐项清单

### 2.1 名称

| 项               | 状态        | 值 / 说明                                            |
| ---------------- | ----------- | ---------------------------------------------------- |
| 产品名（工作名） | **PARTIAL** | 「个人数字依赖图」—— 工作名，非最终品牌名（**B14**） |
| App Name（商店） | **待定**    | `store/STORE_LISTING_ZH.md` 记为「个人数字依赖图」   |
| 副标题（≤30 字） | **就绪**    | 「换卡前，先看清哪些服务会受影响」                   |

### 2.2 包标识

| 平台                  | 当前值                                              | 状态                       |
| --------------------- | --------------------------------------------------- | -------------------------- |
| Android applicationId | `com.example.depmap`                                | **占位 —— BLOCKER（B11）** |
| iOS bundle id         | 未设定                                              | **BLOCKER（B11）**         |
| HarmonyOS bundleName  | `com.depmap.app`（`platforms/harmonyos/app.json5`） | **待确认是否最终值**       |
| uni-app x appid       | `__UNI__DEPMAP01`                                   | 内部标识，非商店标识       |

> **未擅自修改任何包标识。**

### 2.3 版本

| 项                         | 值          |
| -------------------------- | ----------- |
| App versionName            | `0.1.0`     |
| App versionCode            | `1`         |
| schemaVersion              | `3`         |
| DEPMAP formatVersion       | `1`         |
| Android minSdk / targetSdk | `26` / `34` |
| HarmonyOS min/target API   | `12` / `12` |
| iOS deployment target      | `14+`       |

> **未宣布 1.0。** 当前版本号保持 `0.1.0`。

### 2.4 图标（BLOCKED）

| 平台      | 需求                                             | 现状     |
| --------- | ------------------------------------------------ | -------- |
| Android   | Adaptive icon 前景/背景、legacy 各密度、Play 512 | **缺失** |
| iOS       | `AppIcon-1024.png`（无 alpha）                   | **缺失** |
| HarmonyOS | `icon_foreground` / `icon_background`            | **缺失** |

- `assets/` 目录**仅含 `README.md`**，**无任何 PNG**。
- **发现 U-1**：`app/manifest.json` 已引用 `static/icons/{48,72,96,192}x*.png` 与 `static/splash/*.png`，但 **`app/static/` 目录不存在** → 路径与资产不一致。
- 规格：`docs/APP_ICON_ASSET_SPEC.md`。
- **未生成占位/测试图标充当上线资产**（违反第 93 节）。

### 2.5 启动图（BLOCKED）

`app/manifest.json` 引用 `static/splash/{480x762, 720x1242, 960x1656, 1242x2688}.png` —— **全部缺失**。**B16**。

### 2.6 商店截图（BLOCKED）

- 方案：`store/SCREENSHOT_PLAN.md`（计划 Home / Scenario / Impact-Plan / Timeline / Privacy-Local-first）。
- 依赖 UI 可运行（**B10**）→ 当前 **NOT_RUN**。
- **未伪造截图。**

### 2.7 描述文案

| 项            | 状态                | 位置                        |
| ------------- | ------------------- | --------------------------- |
| 短描述        | 就绪                | `store/STORE_LISTING_ZH.md` |
| 长描述        | 就绪                | 同上                        |
| 关键词        | 就绪                | 同上                        |
| Release notes | 就绪                | `store/RELEASE_NOTES.md`    |
| 分类          | 就绪（工具 / 效率） | `store/STORE_LISTING_ZH.md` |

### 2.8 隐私

| 项                                       | 状态                       | 说明                                                                                                            |
| ---------------------------------------- | -------------------------- | --------------------------------------------------------------------------------------------------------------- |
| 隐私披露事实矩阵                         | **就绪**                   | `store/PRIVACY_DISCLOSURE_MATRIX.md` + `docs/PRIVACY_DATAFLOW_AUDIT.md`                                         |
| 本地数据                                 | 是（无云端）               | 与代码一致（`check:network` 0 网络原语）                                                                        |
| financial info                           | **否**（不采集）           | —                                                                                                               |
| diagnostics / tracking / analytics / ads | **否**                     | 无相关 SDK                                                                                                      |
| account                                  | **否**（无账号系统）       | —                                                                                                               |
| network                                  | **0 调用**                 | 与代码一致                                                                                                      |
| 隐私政策 URL                             | **缺失 —— BLOCKER（B12）** | 草稿：`docs/PRIVACY_POLICY_DRAFT.md`。**未虚构 URL。**                                                          |
| 用户须知                                 | 就绪                       | `docs/USER_NOTICE_DRAFT.md`：非银行 / 非财务顾问 / 非自动支付工具 / 结果基于已知与确认数据                      |
| iOS 隐私字符串                           | **需复核（U-2）**          | `NSFaceIDUsageDescription` 合理；`NSCameraUsageDescription` 声明了尚未实现的二维码扫描用途 → 提交前需与实现对齐 |

### 2.9 支持

| 项       | 状态                        |
| -------- | --------------------------- |
| 支持 URL | **缺失 —— BLOCKER（B12b）** |
| 支持邮箱 | 未提供                      |
| 支持页面 | 无                          |

**未虚构 URL。**

### 2.10 权限

| 平台                            | 权限                      | 判定             |
| ------------------------------- | ------------------------- | ---------------- |
| Android（app manifest）         | `[]`（空）                | PASS（最小权限） |
| Android（native core manifest） | `USE_BIOMETRIC`           | PASS             |
| HarmonyOS                       | `[]`（空）                | PASS             |
| iOS                             | FaceID + Camera（见 U-2） | **待复核**       |

### 2.11 签名

| 平台                       | 状态                                             |
| -------------------------- | ------------------------------------------------ |
| Android release keystore   | **缺失（B4）**                                   |
| iOS signing / provisioning | **缺失（B9）**；需 Apple Developer Account（B8） |
| HarmonyOS release signing  | **缺失（B7）**；需 Huawei Developer / AGC（B6）  |

`.gitignore` 已覆盖全部签名材料路径；**未将任何私钥纳入版本控制**（已核验 `git ls-files` 无命中）。

### 2.12 构建产物

| 平台      | 产物          | 状态              |
| --------- | ------------- | ----------------- |
| Android   | APK / AAB     | **无**（BLOCKED） |
| iOS       | IPA / archive | **无**（BLOCKED） |
| HarmonyOS | HAP           | **无**（BLOCKED） |

**未伪造任何产物或 hash。**

### 2.13 设备测试

**0 台设备完成测试**（`adb devices` 空；无 HarmonyOS 设备；无 iOS 设备）。
真机 E2E（安装 → 引导 → 解锁 → 导入 → 计划 → 时间轴 → 备份/恢复 → 重启）= **BLOCKED**。

### 2.14 Real Data 决策

| 项                      | 状态                                    |
| ----------------------- | --------------------------------------- |
| REAL_DATA_CORRECTNESS   | **NOT_RUN**                             |
| REAL_DATA_VALUE         | **NOT_RUN**                             |
| 真实账单                | 未提供（`local_private/` 仅日志与脚本） |
| 是否作为提审前必需 Gate | **需用户决策（见 §3）**                 |

### 2.15 提审

| 项              | 状态                     |
| --------------- | ------------------------ |
| 自动上传        | **未执行**（用户未授权） |
| STORE_SUBMITTED | **NO**                   |

---

## 3. 提审前必须由用户决策/提供的事项

| #   | 事项                                                                     | 类型 |
| --- | ------------------------------------------------------------------------ | ---- |
| 1   | 正式产品名 / 品牌名（B14）                                               | 提供 |
| 2   | 正式 Android applicationId / iOS bundle id / HarmonyOS bundleName（B11） | 提供 |
| 3   | 正式图标资产（B15）                                                      | 提供 |
| 4   | 正式启动图资产（B16）                                                    | 提供 |
| 5   | 商店截图（B17，依赖 B10）                                                | 依赖 |
| 6   | 隐私政策 URL（B12）                                                      | 提供 |
| 7   | 支持 URL（B12b）                                                         | 提供 |
| 8   | Android release keystore（B4）                                           | 提供 |
| 9   | Google Play Developer Account（B5）                                      | 提供 |
| 10  | Huawei Developer / AGC 身份（B6）                                        | 提供 |
| 11  | HarmonyOS release signing（B7）                                          | 提供 |
| 12  | Apple Developer Account（B8）                                            | 提供 |
| 13  | iOS signing / provisioning（B9）                                         | 提供 |
| 14  | 真机（Android / HarmonyOS）（B18）                                       | 提供 |
| 15  | **是否将 Real Data 设为提审前必需 Gate（B19）**                          | 决策 |
| 16  | HBuilderX 安装与 uni-app x 编译（B10）                                   | 环境 |
| 17  | iOS 相机用途声明是否保留（U-2）                                          | 决策 |

---

## 4. Real Data 决策建议（第 101 节）

**结论：`STORE_SUBMISSION_READY = REQUIRES_USER_RELEASE_DECISION`，不得无条件写 PASS。**

理由：

1. 产品核心价值主张是「**从真实账单/导出中发现真实依赖**」（账单导入 → 依赖发现 → 影响分析）。
2. 当前 `REAL_DATA_CORRECTNESS = NOT_RUN`、`REAL_DATA_VALUE = NOT_RUN`；且**账单导入在设备上不可用**（B20）。
3. 因此当前构建虽可**手动**完成主流程（手动建立对象 → 手动声明支付关系 → 影响模拟 → 变更计划 → 验证 → 时间轴），但**无法验证产品最核心的差异化价值**。
4. 若在 Real Data 未验证的情况下提审，存在「商店通过但产品价值未证实」的风险。

**建议**：将 Real Data 双 Gate 设为提审前必需项，或明确接受「先以手动路径上架、账单导入作为后续版本能力」的取舍。**该决策权属于用户。**

### Real Data Pilot 推荐规格（第 102 节，仅准备流程，不自动索取）

1. 1 份真实微信账单导出
2. 2–3 份真实 CSV 导出
3. 1–2 份真实 OFX/QFX

全部置于 `local_private/`（已 gitignore，**永不入 Git**）。验证项：编码（UTF-8 / GB18030 / GBK）、字段变体、重复行、商户描述符、周期性、Resolver 命中、Proposal 正确性、最终价值。

工具已就绪：`core/scripts/validate-real-bill.ts`。

---

## 5. 复现

```
git ls-files | grep -iE "\.env|\.keystore|\.jks|\.p12|\.p8|mobileprovision|\.pem|\.key$"
ls -la assets/            # 仅 README.md
ls -d app/static          # 不存在
cd core && npm run check:secrets && npm run check:network
```
