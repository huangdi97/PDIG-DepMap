# HARMONY_N3_IMPLEMENTATION_STATUS.md

> Harmony N3 实施状态。生成时间：2026-09-17（Asia/Shanghai）
> 上游冻结基准：`ANDROID_NATIVE_CORE_FREEZE.md` @ HEAD `648aa36`
> 工程审计：`HARMONY_N3_BASELINE_AUDIT.md`

---

## 0. 一句话结论

> N3 **已正式开工**，并已取得**第一个真实施工证据**：
> `harmony/` 从「只有 1 个 codegen 文件」变成**可被 hvigor 真实构建、产出 HAP 的 Stage Model 工程**，
> 且**首个纯 ArkTS Domain 模块（Relations）已编译并被打进 HAP**。
>
> 但 `.depmap`（K 节）存在**硬 blocker**：`cryptoFramework` 不含 Argon2。
> 端到端运行无法验证：**无模拟器镜像、`hdc list targets` 为空**。
>
> 因此 `HARMONY_NATIVE_CORE_HANDOFF` **尚未达成**，本轮按"遇到真实 blocker 后停止"处理。

---

## 1. 本轮实际落地的内容

| # | 内容 | 位置 | 验证方式 |
| --- | --- | --- | --- |
| 1 | Stage Model 工程骨架 | `harmony/{AppScope,build-profile.json5,oh-package.json5,hvigorfile.ts,hvigor/hvigor-config.json5}` | hvigor 真实构建 |
| 2 | entry HAP 模块 | `harmony/entry/{build-profile.json5,hvigorfile.ts,oh-package.json5,src/main/module.json5}` | 同上 |
| 3 | EntryAbility（Stage Model） | `harmony/entry/src/main/ets/entryability/EntryAbility.ets` | 同上 |
| 4 | 资源（string / color / profile / 占位图标） | `harmony/**/resources/**` | 同上 |
| 5 | **纯 ArkTS Domain：Relations** | `harmony/entry/src/main/ets/domain/Relations.ets` | 同上 + HAP 内含代码标记 |
| 6 | ASCII 镜像构建器 | `tools/harmony/build-ascii-mirror.mjs` | 实跑 |
| 7 | Relations 语义镜像自检（**非门禁**） | `tools/harmony/check-relations-semantics.mjs` | 实跑 18/18 |

### 分层遵守情况（§G）

| 层 | 目录 | 状态 |
| --- | --- | --- |
| presentation | `entry/src/main/ets/pages/` | 骨架（1 页占位） |
| application | — | 未开工 |
| **domain** | `entry/src/main/ets/domain/` | **Relations 已实现** |
| data | — | 未开工 |
| platform | — | 未开工 |
| security | — | 未开工 |

Domain 层**不依赖** ArkUI / Ability / ArkData / HUKS 实现 —— 已通过 `Relations.ets` 的导入集合核实
（仅 `../generated/CanonicalEnums`）。

---

## 2. N3 Gate 状态（§O 口径：PASS / FAIL / BLOCKED / NOT_RUN / PARTIAL_WITH_REPORT）

| Gate | 状态 | 依据 |
| --- | --- | --- |
| `HARMONY_DOMAIN` | **PARTIAL_WITH_REPORT** | 仅 Relations（registry + validateUse + validateGroupUse）落地；Impact / PlanReadiness / Coverage / StateMachine / Timeline / Scenario / Migration / GraphRevision 未开工 |
| `HARMONY_CONFORMANCE` | **NOT_RUN** | 无设备/模拟器；未接本地测试框架。**不得**写 PASS（详见下条与 conformance 报告） |
| `HARMONY_ARKDATA` | **NOT_STARTED** | — |
| `HARMONY_MIGRATION` | **NOT_STARTED** | — |
| `HARMONY_REPOSITORY` | **NOT_STARTED** | — |
| `HARMONY_HUKS` | **NOT_STARTED** | — |
| `HARMONY_AUTH` | **NOT_STARTED** | — |
| `HARMONY_DEPMAP` | **BLOCKED** | `cryptoFramework` KDF 仅 PBKDF2 / HKDF，**无 Argon2**（见 §3） |
| `HARMONY_ARKUI` | **PARTIAL_WITH_REPORT** | Stage Model + 1 个占位页；§L 的 17 个页面全部未开工 |
| `HARMONY_IMPORT` | **NOT_STARTED** | — |
| `HARMONY_BACKUP_RESTORE` | **NOT_STARTED** | 且受 `HARMONY_DEPMAP` 牵连 |
| `HARMONY_BUILD` | **PASS** | 全清重建：`CompileArkTS` + `PackageHap` 成功，产出 HAP（见 §4） |
| `HARMONY_RUNTIME_E2E` | **RUNTIME_NOT_RUN** | 无模拟器系统镜像；`hdc list targets` = `[Empty]` |
| `HARMONY_PRIVACY_SECURITY` | **NOT_STARTED** | — |
| `HARMONY_NATIVE_CORE_HANDOFF` | **FAIL**（未达成） | 14 个 Gate 中 1 PASS / 11 NOT_STARTED / 1 BLOCKED / 1 NOT_RUN |
| `N3_HARMONY_FULL_PARITY` | **NOT_STARTED** | parity 矩阵 Harmony 列仍 0 / 73 |

---

## 3. P0 Blocker：Argon2（K 节）

**事实**（`BASELINE_AUDIT.md` §3，直接读 SDK 类型声明）：

```
@ohos.security.cryptoFramework.d.ts
KdfSpec
├── PBKDF2Spec
└── HKDFSpec
全文检索 Argon2|ARGON|argon2 → 0 命中
```

`DEPMAP_CONTAINER_V1` 要求 `argon2id / version 19`。K 节明确禁止自研原语，故 **BLOCKED**。

已排除的绕路：NDK 无 openssl 头/库、无 libsodium/argon2 产物。

**尚未评估的可行路径**（下轮，需评审）：NAPI + 经审计的外部 Argon2 参考实现（CMake 编入 HAP）；
或引入经审计的 ohpm 三方包。二者都需产出与 Golden Vector 逐字节一致的结果。

---

## 4. HARMONY_BUILD 证据（本轮实跑）

```
> hvigor Finished :entry:default@CompileArkTS... after 5 s 936 ms
> hvigor Finished :entry:default@PackageHap... after 503 ms
> hvigor BUILD SUCCESSFUL in 12 s 247 ms
```

| 项 | 值 |
| --- | --- |
| 产物 | `entry-default-unsigned.hap` |
| bytes | **60,133** |
| sha256 | `ac86a5af1a7f15d2ddba70b139b4cbe862d3d1af2898efa496ac05801f9c00fa` |
| mtime | 2026-09-17T12:51:35Z |
| 域代码确已打包 | HAP 内含 `"is not in the runtime registry"` 与探针串 `relations: funding_source=` → **true / true** |

> **两点必须记住的坑**（本轮踩到）：
>
> 1. **hvigor 拒绝非 ASCII 工程路径**，且校验的是 `process.cwd()`，正则只允许 ASCII + 空格，
>    没有环境变量绕过 → 必须做 ASCII 镜像（`tools/harmony/build-ascii-mirror.mjs`）。
>    `mklink /J` **无效**：Node 会把 cwd 解析回真实路径（与 Gradle 侧结论一致）。
> 2. **hvigor 增量 `CompileArkTS` 会漏掉新增文件**：本轮曾出现新加 `Relations.ets` 后仍报
>    `UP-TO-DATE` 且 BUILD SUCCESSFUL，而该文件其实**未被编译**（其中含一个错误导入）。
>    只有在把该文件接入页面引用后才暴露真实错误。
>    → **验证性构建必须全清**（`rm -rf <mirror>` 后重建），不能信增量结果。

---

## 5. 关于 Conformance 的诚实口径

`HARMONY_CONFORMANCE = NOT_RUN`。

- 无设备、无模拟器镜像 → ArkTS conformance runner 无法执行。
- 未接 DevEco 本地测试框架 → 无替代执行面。
- 本轮**唯一**与 conformance 相关的正数，是
  `tools/harmony/check-relations-semantics.mjs` 的 **18 / 18**，
  但它是**源码语义镜像**（Node 侧等价实现 vs 同一批 fixtures），
  **不是 ArkTS 运行时执行结果**，因此**不写入** `HARMONY_CONFORMANCE`。
- 目标仍是 **91 / 91**（same fixtures → ArkTS → normalized → compare expected）；
  实现顺序建议：relations(18) → impact(13) → readiness(16) → coverage(6) → parser(22) → 其余。

---

## 6. 停止点与下一步

按 §R，本轮在**真实 blocker** 处停止：

1. `HARMONY_DEPMAP` = **BLOCKED**（平台 API 缺 Argon2）
2. `HARMONY_RUNTIME_E2E` = **RUNTIME_NOT_RUN**（缺模拟器镜像，属用户侧外部闸门）

**未进入 iOS N4。**

下一步（下轮，按优先级）：

| 优先级 | 事项 | 前置 |
| --- | --- | --- |
| P0 | 确立 Argon2 路径或正式升级为长期 blocker | 安全评审 |
| P0 | 在 DevEco 下载模拟器系统镜像，打通 `HARMONY_RUNTIME_E2E` | 用户操作（账号/网络） |
| P1 | 接本地测试框架，让 conformance 可脱离设备执行 | — |
| P1 | 继续 Domain：Impact → PlanReadiness → Coverage → StateMachine | Android 冻结基准 |
| P2 | ArkData persistence + migration（I 节） | — |
| P2 | HUKS + 用户认证（J 节） | — |

---

## 7. 复现

```bash
export PATH="/c/Users/Kaiser/.workbuddy/binaries/PortableGit/versions/1.2.0/usr/bin:$PATH"

# 全清 + 构建 + 产出 HAP
rm -rf "$HOME/pdig-harmony-build"
node tools/harmony/build-ascii-mirror.mjs

# （非门禁的）Relations 语义镜像自检
node tools/harmony/check-relations-semantics.mjs
```
