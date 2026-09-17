# HARMONY_N3_BASELINE_AUDIT.md

> Harmony N3 第一阶段：harmony/ 工程真实性审计。
>
> 生成时间：2026-09-17（Asia/Shanghai）
> 原则：**先审计，不写代码**。不得把 Legacy Harmony HAP / UTS 证据冒充新的 Native `harmony/` 证据。

---

## 0. 一句话结论

> **`harmony/` 目前不是可运行工程骨架，它是纯 generated artifact。**
> 全目录**只有 1 个文件**，没有任何 Stage Model 工程所需的配置文件。
>
> 但**本机 Harmony 原生工具链是完整可用的**（DevEco 5.0.5.310 + OpenHarmony API 13 + hvigor 5.13.2 + ohpm 5.0.10），
> 因此 N3 的阻碍**不是"没有工具链"**，而是：**(a) 工程尚未创建**，**(b) `cryptoFramework` 无 Argon2**，**(c) 无模拟器/真机镜像**。

---

## 1. harmony/ 现状（实测）

```
harmony/
└── entry/
    └── src/
        └── main/
            └── ets/
                └── generated/
                    └── CanonicalEnums.ets      ← 唯一文件（347 行）
```

| Stage Model 必需文件 | 是否存在 |
| --- | --- |
| `AppScope/app.json5` | ❌ |
| `build-profile.json5` | ❌（根目录） |
| `oh-package.json5` | ❌ |
| `hvigorfile.ts` | ❌ |
| `hvigor/hvigor-config.json5` | ❌ |
| `entry/build-profile.json5` | ❌ |
| `entry/hvigorfile.ts` | ❌ |
| `entry/oh-package.json5` | ❌ |
| `entry/src/main/module.json5` | ❌ |
| `entry/src/main/ets/entryability/EntryAbility.ets` | ❌ |
| `hvigorw` / `hvigorw.bat` | ❌ |
| `local.properties` | ❌ |

**判定：`harmony/` = 纯 codegen 产物，不可被 hvigor 解析，不可构建。**

该产品已由 `node tools/codegen/generate.mjs --check` 校验为**正式产物**
（`CODEGEN GATE: PASS`），并已于本轮入库（`648aa36`），**不是**临时探测文件。

---

## 2. 工具链清单（本机实测）

### 2.1 DevEco Studio

| 项 | 值 |
| --- | --- |
| 安装路径 | `<DEVECO_HOME>` |
| build | `DS-233.14475.28.36.505310` |
| version | `5.0.5.310` |
| productCode | `DS` |
| 自带 JBR | 存在（`jbr/bin`） |

### 2.2 Harmony SDK

| 项 | 值 |
| --- | --- |
| 路径 | `<DEVECO_HOME>\sdk\default` |
| 组件 | `openharmony`、`hms` |
| **apiVersion** | **13** |
| version | `5.0.1.115` |
| releaseType | `Release` |
| openharmony/ets 子件 | `api`、`arkts`、`build-tools`、`component`、`kits` |
| toolchains | `hdc.exe`、`restool.exe`、`syscap_tool.exe`、`modulecheck`、`configcheck` 等 |

`hms/ets` 同版本（apiVersion 13 / 5.0.1.115），含 `@hms.core.*` 系列能力。

### 2.3 打包与依赖管理

| 工具 | 版本 | 位置 |
| --- | --- | --- |
| `@ohos/hvigor` | **5.13.2** | `tools/hvigor/hvigor` |
| `@ohos/hvigor-ohos-plugin` | **5.13.2** | `tools/hvigor/hvigor-ohos-plugin` |
| `@ohos/ohpm` | **5.0.10** | `tools/ohpm` |
| Node（DevEco 内置） | **v18.20.1** | `tools/node/node.exe` |

### 2.4 关键 API 存在性核验（读 `openharmony/ets/api` 目录）

| N3 需要的能力 | 声明文件 | 存在 |
| --- | --- | --- |
| ArkData 关系型数据库 | `@ohos.data.relationalStore.d.ts` | ✅ |
| HUKS 密钥管理 | `@ohos.security.huks.d.ts` | ✅ |
| 通用密码框架 | `@ohos.security.cryptoFramework.d.ts` | ✅ |
| 用户认证 | `@ohos.userIAM.userAuth.d.ts` | ✅ |
| 文件选择器 | `@ohos.file.picker.d.ts` | ✅ |
| Stage Model UIAbility | `@ohos.app.ability.UIAbility.d.ts` | ✅ |
| ArkUI 组件框架 | `@ohos.arkui.*` | ✅ |
| 偏好存储 | `@ohos.data.preferences.d.ts` | ✅ |

---

## 3. ⚠ P0 Blocker —— **Argon2 不在 cryptoFramework 中**

这是本次审计最重的发现，直接决定 `.depmap`（K 节）能否实现。

### 3.1 事实（直接读 SDK 类型声明，非推测）

`@ohos.security.cryptoFramework.d.ts` 的 KDF 派生树只有两条：

```
KdfSpec (interface)
├── PBKDF2Spec extends KdfSpec
└── HKDFSpec   extends KdfSpec
```

对全文做 `Argon2|ARGON|argon2` 正则检索 → **0 命中**。
（`symKeyGenerator` 支持 GCM 的 iv / aad 参数，AES-256-GCM 侧无同类问题。）

### 3.2 影响

`DEPMAP_CONTAINER_V1` 固定要求 `argon2id / version 19`。K 节明确要求：

> 如果 Harmony crypto library 无法支持：明确 blocker，**不要自己实现 Argon2/AES primitive**。

因此 **`HARMONY_DEPMAP = BLOCKED`**，理由是平台 API 缺能力，且禁止自研原语。

### 3.3 还核查过的两条"绕路"

| 路径 | 结论 |
| --- | --- |
| NDK native OpenSSL | ❌ `openharmony/native` 下仅存在 CMake 的 `Modules/FindOpenSSL.cmake`，**无 openssl 头/库** |
| NDK 内 libsodium / argon2 产物 | ❌ 递归检索 `openharmony/native` 无任何命中 |

### 3.4 尚未排除的可行路径（下轮评估，**本轮不实施**）

1. **NAPI + 外部 Argon2 参考实现**（如 PHC argon2 reference / libsodium），经 CMake 编进 HAP，
   由 ArkTS 侧通过 NAPI 调用。这属于**链接经过审计的外部库**，不是自研原语。
   代价：引入 native 构建链 + 第三方库安全评审 + 必须产出与 Golden Vector 逐字节一致的结果。
2. 由用户在 DevEco 侧引入经审计的 ohpm 三方 Argon2 包 —— 同样需要合规与安全评审。

> 两条路都不改变本轮结论：**当前不可用，标记为 BLOCKED。**

---

## 4. 运行时环境（§N 的拆分口径）

| 项 | 实测 | 结论 |
| --- | --- | --- |
| `Emulator.exe` | 存在（`tools/emulator/Emulator.exe`，含 Qt 依赖） | 二进制就绪 |
| **模拟器系统镜像** | ❌ `%USERPROFILE%/AppData/Local/Huawei`、`%USERPROFILE%/Huawei`、`<HUAWEI_HOME>` 等常见位置均无；AppData/Local 下检索 `*.img`/`*.qcow2` 无命中 | **缺镜像** |
| `hdc list targets` | **`[Empty]`** | 无设备、无在线模拟器 |

**真实拆分结论：**

```
HARMONY_SOURCE      = 待建（harmony/ 目前只有 codegen）
HARMONY_BUILD       = 待验证（工具链齐，但工程不存在）
HARMONY_RUNTIME_E2E = RUNTIME_NOT_RUN        ← 真实外部 blocker
```

blocker 性质：模拟器系统镜像需经 **DevEco Studio GUI 下载**（通常需要华为账号登录），
与 Android 侧"需在 SDK Manager 装 system-image"B18 同类，**属于用户侧外部闸门**，
不属于工程缺口，因此**不用大量代码去填**，按 §N 如实拆分报告。

---

## 5. Legacy Harmony 的定位（边界声明）

`platforms/harmonyos/` 是一个**完整的 UTS 时代 hvigor 工程**：

```
platforms/harmonyos/
├── AppScope/app.json5
├── build-profile.json5、oh-package.json5、hvigorfile.ts
├── hvigor/hvigor-config.json5        ← 含本机工具链 file: 依赖写法
├── hvigorw / hvigorw.bat / hvigorw.js
├── entry/{build-profile.json5,hvigorfile.ts,oh-package.json5,src}
├── artifacts/entry-default-unsigned.hap   ← UTS 桥接时代产物
└── .hvigor/{cache,outputs,report}
```

按 §D / §F：

- **仅 Legacy Reference**，不得成为 Production dependency。
- `artifacts/entry-default-unsigned.hap` **不得**计入新的 Native `harmony/` 任何 Gate 证据。

其中 `hvigor-config.json5` 的一条实测经验值得保留（作为**参考**而非复制）：

> `@ohos/hvigor` 5.13.2 需用 `file:` 协议直接引用本机 DevEco 内置包；
> 且两个包必须都写在 `dependencies`（写在 `hvigorVersion` 会触发 enum schema 校验失败）。
> —— **该结论为继承表述，本轮未重新验证。**

---

## 6. 审计通过后 N3 的第一批动作（建议顺序）

1. 用 **hvigor/ArkTS/Stage Model** 建 `harmony/` 工程骨架（不复用 `platforms/harmonyos/`）。
2. 挂 codegen 产物（`CanonicalEnums.ets` 已入库）作为 spec 一致性的锚。
3. 实现**分层纯 ArkTS Domain**（presentation / application / domain / data / platform / security），
   Domain 层禁止依赖 ArkUI / Ability / ArkData / HUKS 实现。
4. 建 Harmony conformance runner，**读同一批 fixture**，不用 device 也能跑通的部分先跑。
5. `.depmap` 标记为 BLOCKED，等 Argon2 路径确立后再解。

---

## 7. 附：本轮使用的审计命令

```bash
# codegen 归属判定
node tools/codegen/generate.mjs --check          # → CODEGEN GATE: PASS（三端）

# 工具链版本（读 SDK/DevEco 自带 package.json 与 oh-uni-package.json，而非执行 GUI）
DevEco Studio/product-info.json                  # 5.0.5.310
sdk/default/openharmony/ets/oh-uni-package.json  # apiVersion 13 / 5.0.1.115
tools/hvigor/hvigor/package.json                 # 5.13.2
tools/ohpm/package.json                          # 5.0.10

# Argon2 缺失取证
grep -n "Argon2|ARGON|argon2" sdk/default/openharmony/ets/api/@ohos.security.cryptoFramework.d.ts
# → 0 命中；KdfSpec 仅有 PBKDF2Spec / HKDFSpec 两个子接口

# 运行时
hdc list targets                                 # → [Empty]
```
