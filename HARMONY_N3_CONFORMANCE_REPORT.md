# HARMONY_N3_CONFORMANCE_REPORT.md

> Harmony Conformance 报告。生成时间：2026-09-17（Asia/Shanghai）
> **最近更新：2026-09-18**
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

1. **无 Harmony 运行时**：`hdc list targets` = `[Empty]`；本机无任何模拟器系统镜像
   （根因已定位，见 `HARMONY_RUNTIME_ENVIRONMENT_AUDIT.md`）。
2. **未接本地测试框架**：DevEco 的本地单元测试（Hypium）尚未在工程里布好并执行。

因此本报告**不提供任何 Harmony 侧的 pass 计数**。

### 0.1 2026-09-18 变更：4 个 blocked 的原因已改变

原 `depmap` / `backup` 的阻塞原因是 **"平台 API 无 Argon2"**。
该原因**已解除** —— 已改走自建 NAPI + vendored PHC 参考实现，
两个 ABI 均编译通过并打进 HAP。

因此这 4 个用例的阻塞分类应从 `BLOCKED_BY_PLATFORM` 改为
**`BLOCKED_BY_RUNTIME`**：代码已就绪且已 `COMPILED`，
纯粹因为**无运行时**而无法执行。

> 这个区分有实际意义：前者意味着「需要设计变更」，
> 后者意味着「等运行时到位即可跑」。两者对下一步动作的指引完全不同。

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

> **更新说明（2026-09-18 第二轮）**：Domain 11 组已全部落地并 **COMPILED**
> （`HARMONY_DOMAIN = COMPILED`，见 `HARMONY_N3_IMPLEMENTATION_STATUS.md` §1.1）。
> 但**本报告的执行计数不变，仍是 pass=0** —— 理由必须讲清楚，
> 否则会把"实现就绪"误读成"已通过":

| 分类 | 用例数 | Harmony 实现 | Harmony 执行 | 说明 |
| --- | --- | --- | --- | --- |
| relations | 18 | **COMPILED** | notImplemented | 18/18 主机镜像已过（`check-relations-semantics.mjs`），但**那不算 conformance 执行** |
| impact | 13 | **COMPILED** | notImplemented | ArkTS runner 未接 |
| readiness | 16 | **COMPILED** | notImplemented | 同上 |
| coverage | 6 | **COMPILED** | notImplemented | 同上 |
| parser | 22 | 未开工 | notImplemented | 属 import/source 层 |
| state-machine | 5 | **COMPILED** | notImplemented | ArkTS runner 未接 |
| timeline | 3 | **COMPILED** | notImplemented | 同上 |
| migration | 2 | 未开工 | notImplemented | 需 ArkData（§12） |
| scenario | 1 | **COMPILED** | notImplemented | 同上 |
| jcs | 1 | **COMPILED** | notImplemented | 同上 |
| **depmap** | **3** | **COMPILED** | **BLOCKED_BY_RUNTIME** | 平台无 Argon2 的阻塞已解除；现因无运行时未执行（§3） |
| **backup** | **1** | 未开工 | **BLOCKED_BY_RUNTIME** | 依赖 `.depmap` 加解密 + ArkData，同上 |
| **合计** | **91** | — | **87 notImplemented / 4 blocked** | **0 执行** |

**为什么实现了却仍记 notImplemented**：本 gate 的判定标准是
「在 **ArkTS 侧**真实执行 fixtures 并比对」。
`check-relations-semantics.mjs` 跑的是**主机侧镜像**，
它证明的是"移植语义正确"，**不是**"Harmony 运行时通过"。
把主机镜像的 18/18 记成 conformance 的 18/18，正是本项目定义的空心信号。
因此本轮**没有**把任何一条从 notImplemented 挪成 pass。

真正能改动上表数字的，是 §10 要求的 **ArkTS conformance runner**（尚未落地）。


---

## 3. BLOCKED 的依据（已更新）

### 3.1 原依据（已失效，保留供追溯）

```
@ohos.security.cryptoFramework.d.ts
  interface KdfSpec
  ├── interface PBKDF2Spec extends KdfSpec
  └── interface HKDFSpec   extends KdfSpec
  grep -n "Argon2|ARGON|argon2" → 0 命中
```

该事实**仍然成立**（平台 API 确实不含 Argon2），但它**不再构成 blocker**：
本项目已改走自建 NAPI 桥接 + vendored PHC 参考实现，
`DEPMAP_CONTAINER_V1` 要求的
`argon2id / version 19 / m=65536 / t=3 / p=1 / key=32` 已由原生层提供。

K 节「不允许自己实现 Argon2 / AES 原语」**始终被遵守**：
vendored 的是**参考实现本身**（逐文件哈希与上游一致，`local_modifications = 0`），
不是自研实现。

### 3.2 现行依据

4 个用例现为 `BLOCKED_BY_RUNTIME`：
代码已 `COMPILED`、已打进 HAP，但**无设备/模拟器可执行**。

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

> 同理适用：`verify-container-golden.mjs` 的 5/5、
> `check-compiled-reachability.mjs` 的 PASS、`check-argon2-native-build.mjs` 的 PASS
> —— 全部是**主机侧回归工具**，没有一条是 conformance 执行证据。
> 把它们混入 91/91 会稀释掉 `0/91` 这个真实状态。

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
4. **补 `depmap(3)` + `backup(1)`**：代码侧已就绪，等运行时到位后执行，
   并用**同一 Golden Vector** 与 Android / Legacy 冻结基准对齐
   （derivedKey / ciphertext / tag / AAD-JCS）。

### 5.1 分阶段可达的近期目标

| 阶段 | 内容 | 需要运行时？ |
| --- | --- | --- |
| 1 | Domain 11 组落地（解除 87 个 notImplemented 的前置） | 否 |
| 2 | ArkTS conformance runner 接入本地测试框架 | 否（若能脱离设备） |
| 3 | 执行 87 个运行时无关用例 → 目标 `87/91` | 否 |
| 4 | 执行 4 个 `BLOCKED_BY_RUNTIME` 用例 → 目标 `91/91` | **是** |

**阶段 1–3 完全不需要设备**，是当前性价比最高的推进方向。
把 91/91 拆成「87 可自行达成 + 4 等外部资源」，比笼统地说「等运行时」更有行动力。
