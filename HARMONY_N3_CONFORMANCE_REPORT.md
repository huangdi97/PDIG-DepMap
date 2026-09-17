# HARMONY_N3_CONFORMANCE_REPORT.md

> Harmony Conformance 报告。生成时间：2026-09-17（Asia/Shanghai）
> 口径：§H —— same fixtures → ArkTS implementation → normalized output → compare expected。
> **不得把 notImplemented 或 blocked 写成 PASS。**

---

## 0. 总结论

```
HARMONY_CONFORMANCE = NOT_RUN
pass = 0   fail = 0   notImplemented = 87   blocked = 4   total = 91
```

**没有任何一个用例在 Harmony 侧被执行过。**

原因是真实的执行面缺失，不是"跑了但没全绿"：

1. **无 Harmony 运行时**：`hdc list targets` = `[Empty]`；本机无任何模拟器系统镜像。
2. **未接本地测试框架**：DevEco 的本地单元测试（Hypium）尚未在工程里布好并执行。

因此本报告**不提供任何 Harmony 侧的 pass 计数**。

---

## 1. 目标与当前位置

| 项 | 值 |
| --- | --- |
| 目标 | **91 / 91 PASS** |
| 输入 | `fixtures/`（119 files，tree sha256 `4c83ccc3d487abb559682ab7a576a43621552961ac4b1ee3f69bbd98bfabf41a`） |
| manifest | `conformance/CONFORMANCE_MANIFEST.json` sha256 `26d9bdd7bd09fb74…`，cases = 91 |
| Android 侧基准 | **91 / 91 PASS**（本轮实跑，见 `ANDROID_NATIVE_CORE_FREEZE.md` §3.2） |
| Harmony 侧 | **0 执行** |

---

## 2. 逐分类状态

| 分类 | 用例数 | Harmony 状态 | 说明 |
| --- | --- | --- | --- |
| relations | 18 | **notImplemented**（Domain 已移植，未执行） | `Relations.ets` 已编译进 HAP；无运行时 → 未跑 |
| impact | 13 | notImplemented | 未开工 |
| readiness | 16 | notImplemented | 未开工 |
| coverage | 6 | notImplemented | 未开工 |
| parser | 22 | notImplemented | 未开工 |
| state-machine | 5 | notImplemented | 未开工 |
| timeline | 3 | notImplemented | 未开工 |
| migration | 2 | notImplemented | 未开工 |
| scenario | 1 | notImplemented | 未开工 |
| jcs | 1 | notImplemented | 未开工 |
| **depmap** | **3** | **BLOCKED** | `cryptoFramework` 无 Argon2（§3） |
| **backup** | **1** | **BLOCKED** | 依赖 `.depmap` 加解密，被 depmap 牵连 |
| **合计** | **91** | 87 notImplemented / 4 blocked | |

---

## 3. BLOCKED 的依据（不是保守估计）

```
@ohos.security.cryptoFramework.d.ts
  interface KdfSpec
  ├── interface PBKDF2Spec extends KdfSpec
  └── interface HKDFSpec   extends KdfSpec
  grep -n "Argon2|ARGON|argon2" → 0 命中
```

`DEPMAP_CONTAINER_V1` 硬性要求 `argon2id / version 19 / m=65536 / t=3 / p=1 / key=32`。
K 节明确：**不允许自己实现 Argon2 / AES 原语**。故 depmap 3 例 + backup 1 例记为 BLOCKED。

已排除的绕路：NDK 无 openssl 头/库、无 libsodium / argon2 产物（仅 CMake 的 `FindOpenSSL.cmake`）。

---

## 4. ⚠ 一个容易误用的正数（必须连带说明一起引用）

```
$ node tools/harmony/check-relations-semantics.mjs
[relations-semantics-mirror] fixtures: 18
[relations-semantics-mirror] pass = 18 fail = 0
```

**它是什么**：把 `fixtures/relations/` 的 18 个用例，喂给一个与
`Relations.ets` **源码等价的 Node 实现**，逐字节比对 `expected`（含 reason 文案）。

**它不是什么**：

- 不是 ArkTS 运行时执行结果；
- 不是 Harmony 设备端 conformance；
- **不得**计入 `HARMONY_CONFORMANCE`，**不得**写成 "Harmony 18/91 PASS"。

它的唯一用途：在移植过程中尽早发现"实现写错了"。真值仍以设备端执行为准。

---

## 5. 达成 91 / 91 的路径

1. **打通执行面**（前置，最高优先）
   - 在 DevEco 下载模拟器系统镜像 → 可跑 `ohosTest`；或
   - 接入 DevEco 本地单元测试框架 → 脱离设备执行。
   *在此之前，任何 conformance 数字都只能是 NOT_RUN。*
2. **接 runner**：读 `fixtures/`（与 Android 同一批）→ ArkTS 实现 → 归一化 → 比对 `expected`。
   **禁止手抄 expected**。
3. **按分类推进**（建议顺序，按"用例数 × 领域独立性"）：
   `relations(18)` → `impact(13)` → `readiness(16)` → `coverage(6)` →
   `parser(22)` → `state-machine(5)` → `timeline(3)` → `migration(2)` → `scenario(1)` → `jcs(1)`
4. **解 depmap blocker** 后再补 `depmap(3)` + `backup(1)`，并用**同一 Golden Vector**
   与 Android / Legacy 冻结基准对齐（derivedKey / ciphertext / tag / AAD-JCS）。
