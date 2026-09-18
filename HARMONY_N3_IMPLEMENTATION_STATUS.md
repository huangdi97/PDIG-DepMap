# HARMONY_N3_IMPLEMENTATION_STATUS.md

> Harmony N3 实施状态。生成时间：2026-09-17；**最近更新：2026-09-18**
> 上游冻结基准：`ANDROID_NATIVE_CORE_FREEZE.md`
> 工程审计：`HARMONY_N3_BASELINE_AUDIT.md`
> 配套文档：`HARMONY_COMPILE_REACHABILITY_GATE.md`、`HARMONY_ARGON2_INTEGRATION_REPORT.md`、
> `HARMONY_RUNTIME_ENVIRONMENT_AUDIT.md`

> **口径声明**：本报告只写**当前事实**。
> 已过期的叙述见 `WORK_STATUS.md` 的 Historical 区，原样保留不改写。
> 任何未在真实环境执行过的项，一律记 `NOT_RUN`。
> 特别地：`HARMONY_BUILD = PASS` **不等于**「ArkTS 都编译了」——
> 后者由 `HARMONY_MODULE_COMPILED` 独立判定（见 §2.1）。

---

## 0. 一句话结论

> N3 已是**唯一活跃主线**。此前 `cryptoFramework` 无 Argon2 造成的
> `HARMONY_DEPMAP = BLOCKED` **已解除** —— 改走**自建 NAPI + vendored PHC 参考实现**，
> 全链路已打通至「打包进 HAP 且符号表只导出 NAPI 入口」。
>
> 本轮同时固化了**编译可达性 Gate**，并据此抓到并修复了一个真实的孤儿模块
> （`Argon2idNative.ets` 因依赖方向反转而从未被编译，构建却是绿的）。
>
> 剩余唯一硬 blocker 是**外部资源**：缺 Emulator 系统镜像，
> 需人工登录华为账号下载。见 `HARMONY_RUNTIME_ENVIRONMENT_AUDIT.md`。
>
> **Domain 11 组已全部落地**（§9）—— 纯 ArkTS、零平台依赖、
> 经真实编译验证（18/18 required reachable，`modules.abc` 175,732 B），
> 15 项语义自检标识符可从打包产物中反查。详见 §1.1。

---

## 1. 本轮实际落地的内容

| # | 内容 | 位置 | 验证方式 | 状态 |
| --- | --- | --- | --- | --- |
| 1 | **编译可达性 Gate** | `tools/harmony/check-compiled-reachability.mjs` | A/B/C/D 四判据实跑 | **PASS** |
| 2 | import 拓扑诊断工具 | `tools/harmony/dump-import-graph.mjs` | 实跑 | PASS |
| 3 | **KDF 契约中立化** | `ets/crypto/KdfContract.ets` | 消除依赖反转 | **COMPILED** |
| 4 | **Argon2 NAPI 正式绑定** | `ets/crypto/Argon2idNative.ets` | 进 `modules.abc` + 负向 probe | **COMPILED** |
| 5 | NAPI 类型声明 | `cpp/types/libpdiargon2/{index.d.ts,oh-package.json5}` | 构建无警告 | PASS |
| 6 | Argon2 vendoring | `third_party/argon2/`（16 文件） | 哈希 Gate 16/16 | **PASS** |
| 7 | 交叉编译 Gate | `tools/harmony/check-argon2-native-build.mjs` | arm64 + x86_64 | **PASS** |
| 8 | 第三方完整性 Gate | `tools/harmony/check-third-party-hashes.mjs` | 16/16，undeclared=0 | **PASS** |
| 9 | 镜像补齐 `third_party/` | `tools/harmony/build-ascii-mirror.mjs` | CMake 不再 FATAL_ERROR | PASS |
| 10 | Stage Model 工程骨架 | `harmony/**` | hvigor 真实构建 | PASS |
| 11 | 纯 ArkTS Domain：Relations | `ets/domain/Relations.ets` | 进 `modules.abc` | COMPILED |
| 12 | DEPMAP_CONTAINER_V1 | `ets/crypto/{Jcs,DepmapContainerV1,ContainerSelfCheck}.ets` | 进 `modules.abc` + 黄金校验 | **COMPILED** |
| 13 | **Domain 11 组（本轮补全 10 组）** | `ets/domain/*.ets` | 真实编译 + 打包 + 自检反查 | **COMPILED** |
| 14 | **编译 Gate 的 `--build` 缺陷修复** | `tools/harmony/check-compiled-reachability.mjs` | 重建后才读产物 | **PASS** |

### 1.1 Domain 11 组的实际落地（§9）

`HARMONY_DOMAIN` 从 `PARTIAL_WITH_REPORT`（仅 Relations）推进到 **COMPILED**。

| # | 组 | 文件 | 状态 |
| --- | --- | --- | --- |
| 1 | Entities | `domain/Entities.ets` | COMPILED |
| 2 | LogicalKey | `domain/LogicalKey.ets` | COMPILED |
| 3 | ImpactKernel | `domain/ImpactKernel.ets` | COMPILED |
| 4 | PlanReadiness | `domain/PlanReadiness.ets` | COMPILED |
| 5 | ScenarioCoverage | `domain/ScenarioCoverage.ets` | COMPILED |
| 6 | StateMachines | `domain/StateMachines.ets` | COMPILED |
| 7 | GraphRevision | `domain/GraphRevision.ets` | COMPILED |
| 8 | ScenarioTemplate | `domain/ScenarioTemplate.ets` | COMPILED |
| 9 | Timeline | `domain/Timeline.ets` | COMPILED |
| — | Relations（既有） | `domain/Relations.ets` | COMPILED |
| — | CanonicalWire（辅助，非 11 组） | `domain/CanonicalWire.ets` | COMPILED |
| — | DomainSelfCheck（入边） | `domain/DomainSelfCheck.ets` | COMPILED |

**RelationRegistry 未实现 —— 这是刻意决定，不是遗漏。**
TS 基准的 `relation-registry.ts` 比 Android 冻结版多了两个字段
（`verificationPolicy` / `impactSemantics`），而 **Android 冻结基准里没有这两个字段**。
只在 Harmony 侧加上它们会**引入跨端分歧**，与 N3 parity 的目标相反。
故对齐 Android 冻结版（即既有 `Relations.ets`），并把差异记录在此。

**分层约束已核实**：`domain/` 下所有模块的导入集合只含
`../generated/CanonicalEnums` 与同目录 `./*`，
**不含** ArkUI / Ability / relationalStore / HUKS / NAPI / cryptoFramework。

### 1.2 本轮修掉的一个 Gate 缺陷（重要）

`check-compiled-reachability.mjs` 的 `--build` **原先只启用负向 probe，从不重建主产物**。
于是判据 C（"符号是否真的进了 `modules.abc`"）读的一直是上一次构建留下的旧 abc。
这个缺陷**双向有害**：

| 方向 | 表现 | 本轮是否实际发生 |
| --- | --- | --- |
| 假阴性 | 新模块已正确落地，却因 abc 陈旧被判 `inAbc=false` → FAIL | **是**（新 Domain 模块全部被误判） |
| 假阳性 | 模块已被删除/改坏，旧 abc 里仍留着老符号 → 照样 PASS | 未发生但同等危险 |

修复后立即暴露出一个旧 abc **一直在掩盖**的真实 ArkTS 错误：

```
ImpactKernel.ets:257:3 Nested functions are not supported (arkts-no-nested-funcs)
```

即原 TS 里的嵌套 `function evaluateTarget` 不能直接搬到 ArkTS。
已提取为模块级函数 + 显式 `EvalContext`。
**共享引用（而非拷贝）是必须的**：wave-BFS 每轮都往
`unavailableSeen` / `uncertainSeen` 里追加，浅拷贝会让传播在第一轮后停住。


### 分层遵守情况（§G）

| 层 | 目录 | 状态 |
| --- | --- | --- |
| presentation | `entry/src/main/ets/pages/` | 骨架（1 页 + 3 条探针） |
| application | — | 未开工 |
| **domain** | `entry/src/main/ets/domain/` | **仅 Relations** |
| data | — | 未开工 |
| platform | — | 未开工 |
| security | — | 未开工 |

Domain 层**不依赖** ArkUI / Ability / ArkData / HUKS / NAPI / cryptoFramework ——
已通过导入集合核实（`Relations.ets` 仅依赖 `../generated/CanonicalEnums`）。

---

## 2. N3 Gate 状态

状态取值：`PASS` / `FAIL` / `BLOCKED` / `NOT_RUN` / `PARTIAL_WITH_REPORT` / `COMPILED`。

| Gate | 状态 | 依据 |
| --- | --- | --- |
| `HARMONY_BUILD` | **PASS** | clean `assembleHap` 成功（含 native 编译） |
| `HARMONY_MODULE_COMPILED` | **PASS** | A/B/C/D 四判据，**22/22** required 模块；`modules.abc` 243,556 B；见 §2.1 |
| `HARMONY_DEPMAP` | **NATIVE_BUILD_PASS / ON_DEVICE_NOT_RUN** | 全链路打通至 HAP；设备执行未验证 |
| `HARMONY_CRYPTO` | **COMPILED** | JCS / AAD / AES-256-GCM 已进 `modules.abc`，主机黄金校验 5/5 |
| `HARMONY_DOMAIN` | **COMPILED** | 11 组全部落地（RelationRegistry 按对齐决定不实现）；15 项自检进产物 |
| `HARMONY_CONFORMANCE_RUNNER` | **PASS** | 真实 ArkTS runner 已落地并通过负向探针；见 §2.3 |
| **`HARMONY_CONFORMANCE_HOST`** | **PASS** | **主机执行面**：**63/63 运行时无关用例**（全部）在真实 ArkTS 下逐字节复现；见 §2.4 |
| **`HARMONY_DOMAIN_HOST`** | **PASS** | 15/15 域自检在主机真跑全绿（修复 3 个缺陷后）；见 §2.4 |
| `HARMONY_CONFORMANCE` | **NOT_RUN** | 设备执行面：**0 执行 / 91**。**不得**写 PASS |
| `HARMONY_ARKDATA` | **NOT_STARTED** | — |
| `HARMONY_MIGRATION` | **NOT_STARTED** | — |
| `HARMONY_REPOSITORY` | **NOT_STARTED** | — |
| `HARMONY_HUKS` | **NOT_STARTED** | — |
| `HARMONY_AUTH` | **NOT_STARTED** | — |
| `HARMONY_ARKUI` | **PARTIAL_WITH_REPORT** | Stage Model + 1 占位页；§L 的 17 页未开工 |
| `HARMONY_IMPORT` | **NOT_STARTED** | — |
| `HARMONY_BACKUP_RESTORE` | **NOT_STARTED** | — |
| `HARMONY_RUNTIME_E2E` | **RUNTIME_NOT_RUN** | 无模拟器系统镜像；`hdc list targets` = `[Empty]` |
| `HARMONY_PRIVACY_SECURITY` | **NOT_STARTED** | — |
| `HARMONY_NATIVE_CORE_HANDOFF` | **FAIL**（未达成） | 见 §2.2 |
| `N3_HARMONY_FULL_PARITY` | **ACTIVE**（parity 仍 0/73） | 见 `NATIVE_PARITY_MATRIX.md` |

### 2.1 `HARMONY_MODULE_COMPILED` 的独立判定（本轮新立，重要）

**`HARMONY_BUILD = PASS` 曾经是一个近乎空心信号**：hvigor 的 `CompileArkTS`
只编译从 ability/page **可达**的模块，未被 import 的 `.ets` 根本不进编译图，
在那种文件里放类型错误，`assembleHap` 依然 `BUILD SUCCESSFUL`。

本轮据此**抓到并修复了一个真实缺陷**：`Argon2idNative.ets` 因
`import { Argon2idDeriver } from './DepmapContainerV1'` 造成依赖方向反转，
成为无人 import 的孤儿 —— 源文件存在、从未被编译、构建却是绿的。

现由 `check-compiled-reachability.mjs` 以 A/B/C/D 四判据守住，实跑（本轮最终值）：

```
[reachability] ets modules found : 24
[reachability] reachable         : 24
[reachability] modules.abc       : 243556 bytes

CanonicalEnums      reachable=true  inAbc=true       negative=PASS (via ConformanceRunner)  PASS
Relations           reachable=true  inAbc=true       negative=PASS (via ConformanceRunner)  PASS
Jcs                 reachable=true  inAbc=true       negative=PASS (via ConformanceRunner)  PASS
KdfContract         reachable=true  inAbc=TYPE_ONLY  negative=NOT_RUN                    PASS
DepmapContainerV1   reachable=true  inAbc=true       negative=PASS (via ConformanceRunner)  PASS
ContainerSelfCheck  reachable=true  inAbc=true       negative=PASS (via ConformanceRunner)  PASS
Argon2idNative      reachable=true  inAbc=true       negative=PASS (via ConformanceRunner)  PASS
CanonicalWire       reachable=true  inAbc=true       negative=PASS (via ConformanceRunner)  PASS
Entities            reachable=true  inAbc=true       negative=PASS (via ConformanceRunner)  PASS
LogicalKey          reachable=true  inAbc=true       negative=PASS (via ConformanceRunner)  PASS
ImpactKernel        reachable=true  inAbc=true       negative=PASS (via ConformanceRunner)  PASS
PlanReadiness       reachable=true  inAbc=true       negative=PASS (via ConformanceRunner)  PASS
ScenarioCoverage    reachable=true  inAbc=true       negative=PASS (via ConformanceRunner)  PASS
StateMachines       reachable=true  inAbc=true       negative=PASS (via ConformanceRunner)  PASS
GraphRevision       reachable=true  inAbc=true       negative=PASS (via ConformanceRunner)  PASS
ScenarioTemplate    reachable=true  inAbc=true       negative=PASS (via ConformanceRunner)  PASS
Timeline            reachable=true  inAbc=true       negative=PASS (via ConformanceRunner)  PASS
DomainSelfCheck     reachable=true  inAbc=true       negative=PASS (via ConformanceRunner)  PASS
JsonText            reachable=true  inAbc=true       negative=PASS (via ConformanceRunner)  PASS
HarnessFs           reachable=true  inAbc=true       negative=PASS (via ConformanceRunner)  PASS
ConformanceRunner   reachable=true  inAbc=true       negative=PASS (via ConformanceRunner)  PASS
ConformanceSelfCheck reachable=true inAbc=true       negative=PASS (via ConformanceRunner)  PASS

  required modules              : 22
  all required reachable (A)    : true
  all required in modules.abc(C): true

HARMONY_COMPILE_REACHABILITY=PASS
```

> `negative=PASS (via …)` 是**本轮新增的第 D 条判据**：工具会往每个模块尾部注入
> `__pdiTypeProbe` 类型错误并重跑构建，要求构建**真的失败**。
> 只有"注错 → 构建 FAIL"这一对事实同时成立，"该模块参与编译"才算被证明。
> 探针残留已核实清理（`grep -rn "__pdiTypeProbe" harmony/entry/src/main/ets/` 无匹配，exit 1）。

§1.2 的 `--build` 缺陷修复后，上表由 7 项扩到 18 项 —— 之前那 11 项
不是"没做"，而是"做了但 Gate 读的是旧产物、看不出来"。

设计细节（含为何纯类型模块豁免 C、为何 D 必须 `--clean`、
为何只跑一个代表模块、以及残留自愈）见 `HARMONY_COMPILE_REACHABILITY_GATE.md`。

### 2.2 `HARMONY_NATIVE_CORE_HANDOFF` 仍未达成

18 个 gate 中：**7 PASS / 5 COMPILED / 3 NOT_STARTED / 1 PARTIAL / 1 BLOCKED / 1 NOT_RUN**。
Domain 已从 PARTIAL 推进到 COMPILED；Conformance **runner 侧**已落地（§2.3），
**剩余主要缺口是用例实际执行（§10 的 91/91）与 ArkUI（§14）**。

### 2.3 `HARMONY_CONFORMANCE_RUNNER` 的独立判定（本轮新立，重要）

§10 的要求是「**真实 Harmony Conformance Runner，不得用 Node 镜像冒充 ArkTS**，
且 `91/91` 才算 PASS」。本轮交付的是这条要求的**前半段**，且刻意只声称前半段。

**「真实 ArkTS runner」可验证的三项**（缺一不可，全部已独立核实）：

| 项 | 判据 | 实测 |
| --- | --- | --- |
| A 可达 | 有从 ability/page 出发的 import 入边，能被 `CompileArkTS` 看见 | `pages/Index.ets` → `ConformanceSelfCheck` → `ConformanceRunner` → `{JsonText, HarnessFs}` |
| B 进产物 | 符号真的在 `modules.abc` 里 | 4 个模块全在；`runConformance`/`computeCase`/`runImpactSingle`/`runReadiness`/`runCoverage`/`runRelations`/`runStateMachine`/`runScenario`/`runMigrationContract`/`extractExpected`/`serializeImpact` 逐个 grep 到 |
| C 真编译 | 该模块参与编译，而非被 tree-shake | Gate 第 D 判据逐模块注入类型错误 → 构建**真的失败**；`negative=PASS (via ConformanceRunner)` |

**接线的证据链（负向探针在这里发挥了作用）**：

```
未接线时：BUILD SUCCESSFUL in 12 s 41 ms        ← 空证据
接线后首次构建：COMPILE RESULT:FAIL {ERROR:8 WARN:2}
  ConformanceRunner.ets:676:13  arkts-no-any-unknown
  ConformanceRunner.ets:36:3    'CanonicalWire' has no exported member
  ConformanceRunner.ets:57:3    'CoverageSourceInput' → did you mean 'CoverageSourceInfo'
  ConformanceRunner.ets:59:3    'RelationValidationResult' not in '../domain/Entities'
  ConformanceRunner.ets:210:10  Property 'root' does not exist on 'ConformanceReport'
  ConformanceRunner.ets:613:3   'string' not assignable to 'ChangePlanWorkflowState'
  ConformanceRunner.ets:627:7   'string' not assignable to 'PlanActionPhase'
修复后：BUILD SUCCESSFUL in 15 s 350 ms（EXIT=0）
```

这批错误本身**就是**「runner 真的被编译了」的最好证据 ——
一个从未进编译图的文件不可能产生编译错误。

**架构要点（为何不能复用 Node 实现）**：ArkTS 禁止 `any`/`unknown` 与动态索引，
`JSON.parse` 的返回值**无法映射到类**。因此 runner 不解析 JSON，
而是"文本定位 + 受控扫描"：`JsonText` 提供**带窗口的**字符串扫描原语，
数组元素**各占自己的 `[start,end)` 窗口**，字段查找不能跨元素泄漏。
这是"两个 action、只完成一个"这类用例不会假通过的正确性来源。

**必须继续记住的界限**：`HARMONY_CONFORMANCE_RUNNER = PASS`
**只说明 runner 存在且被真的编译**。它**没有**说明任何一条用例的行为正确性 ——
`HARMONY_CONFORMANCE` 仍是 **NOT_RUN，0 执行 / 91**。

### 2.4 `HARMONY_CONFORMANCE_HOST` —— 主机执行面（本轮核心发现）

第三轮结束时，`HARMONY_CONFORMANCE` 的唯一出路被描述为"等设备"。
本轮把这句话拆开验证，发现**存在第二个执行面**：

| 候选 | 实测 |
| --- | --- |
| `ark_js_vm` | **不存在**（SDK 只有 `es2abc.exe` 编译器，无运行时） |
| `Previewer.exe` | 存在但需 IDE socket；且 `@ohos` 是预览桩 → **弃用**（作为证据通道不成立） |
| **hvigor `test`（本地单元测试）** | ✅ **可用**：编译 ArkTS → 执行 → `test_result.txt` |

**实测结果**：

```
node tools/harmony/run-conformance-host.mjs
  [conformance-host] 执行面 : hvigor 本地单元测试（ArkTS，无设备）
  [conformance-host] 用例   : 67 条（含 3 条元测试）
  [conformance-host] 汇总   : run=67 pass=67 fail=0 error=0
  HARMONY_CONFORMANCE_HOST=PASS
```

67 = **63 conformance 用例（全部运行时无关项）+ 3 conformance 元测试 + 1 domain 自检**。

**为什么这不违反 §10**：被执行的代码是 `ConformanceRunner` **本身**
（真 ArkTS 编译器产出），不是另写的 Node 等价实现。
对照 `check-relations-semantics.mjs`：那是另写实现，**不得**计入 conformance；
本门禁可以。内嵌 fixture 由 `embed-fixtures.mjs --check` 先于执行守漂移。

**边界**：主机对 `@ohos.*` 只提供不可调用的桩（实测 `fs.readTextSync` →
`is not callable`），因此 28 个 @ohos 依赖用例仍是 `BLOCKED_BY_RUNTIME`。
**主机执行面不替代设备执行面。**

**真实执行抓到的 5 个缺陷（前两个代码审阅三轮未发现）**：

| # | 缺陷 | 位置 | 后果 |
| - | ---- | ---- | ---- |
| 1 | 拿美化 JSON 与紧凑 JSON 逐字符比 | runner `extractExpected` | 41/60 假失败 |
| 2 | 白名单用**自造**契约词汇（`dependency_created`…） | `domain/GraphRevision.ets` | 契约里每个 mutation 都被判"不提升" |
| 3 | 注释要求按 registry 判定，代码按枚举判定 | `domain/LogicalKey.ets` | `bound_to` 被错误放行 |
| 4 | **priority 排序方向相反**（升序 vs 基准降序） | `domain/Timeline.ets` | attention 桶内次序错 |
| 5 | **`scheduledAt=null` 排前/排后相反**（基准按空串比较） | `domain/Timeline.ets` | 同时段次序错 |

外加 1 个**自检自身的缺陷**：`PlanReadiness` 的"全清"输入其实不清
（默认 `lastAnalyzedGraphRevision=0` < `currentGraphRevision=1` → 判 stale）。

**缺陷 4/5 的教训**：`checkTimeline` 当时只断言"同 priority 时 id 兜底"，
即只测了**确定性**，没测**方向**。只测确定性不测方向，等于没测排序 ——
现已补上两个方向性断言（`/pd`、`/nf` 两段）。

**缺陷 2 的教训**：`DomainSelfCheck` 用同一套自造词断言同一套自造词，
自洽地全绿 —— **自比自的检查不构成证据**，需要外部词汇表（冻结 fixture）才暴露。

**一条差点混过去的假绿**：runner 抛异常时 `actual`/`expected` **都是空串**，
只比 payload 会让"崩了"与"通过"完全一样。现已先钉 `detail` 为空再比 payload。


---

## 3. P0 Blocker 变更记录：Argon2（原 BLOCKED → 已解除）

**原事实**（`BASELINE_AUDIT.md` §3，直接读 SDK 类型声明）：

```
@ohos.security.cryptoFramework.d.ts
KdfSpec ├── PBKDF2Spec └── HKDFSpec
全文检索 Argon2|ARGON|argon2 → 0 命中
```

K 节禁止自研原语，故当时记 `BLOCKED`。

**解除路径（已落地）**：自建 **NAPI 桥接 + vendored PHC 参考实现**，
而非等待平台 API 或引入未审计的 ohpm 包。

排除的绕路（保持排除）：NDK 无 openssl 头/库、无 libsodium/argon2 产物、
**不改用 PBKDF2**（那是降级协议，被明确禁止）。

关键结果：两个 ABI 均编译通过并打进 HAP；打包并 strip 后动态符号表
**恰好 3 个**（`_init` / `_fini` / `RegisterPdiArgon2Module`），`argon2_*` = 0。

详见 `HARMONY_ARGON2_INTEGRATION_REPORT.md`。

---

## 4. HARMONY_BUILD 证据（本轮实跑）

```
> hvigor Finished :entry:default@BuildNativeWithCmake... after 4 s 97 ms
> hvigor Finished :entry:default@BuildNativeWithNinja... after 1 s 172 ms
> hvigor WARN: ArkTS:WARN File: .../crypto/Argon2idNative.ets:24:26
> hvigor BUILD SUCCESSFUL in 19 s 625 ms
```

唯一的 WARN 是 SDK 对 `.so` import 的提示（要求提供 `.d.ts`），
**非错误**；补 `cpp/types/libpdiargon2/index.d.ts` 后语义已显式化。

| 项 | 值 |
| --- | --- |
| 产物 | `entry-default-unsigned.hap` |
| bytes | **2,774,411** |
| 条目数 | 11 |
| native | `libs/arm64-v8a/libpdiargon2.so` (49,776) / `libs/x86_64/libpdiargon2.so` (50,576) |
| ArkTS | `ets/modules.abc` (75,076) |
| 打包后导出符号 | **3**（`_init`/`_fini`/`RegisterPdiArgon2Module`），`argon2_*` = 0 |

> **两点必须记住的坑**：
>
> 1. **hvigor 拒绝非 ASCII 工程路径**，且校验 `process.cwd()`，
>    无环境变量绕过 → 必须做 ASCII 镜像（`tools/harmony/build-ascii-mirror.mjs`）。
>    `mklink /J` **无效**：Node 会把 cwd 解析回真实路径。
> 2. **镜像必须包含 `third_party/`**：`CMakeLists.txt` 用相对路径
>    `../../../../../third_party/argon2` 定位 vendored 源码，
>    只 copy `harmony/` 会导致 CMake `FATAL_ERROR`（本轮已修）。

---

## 5. 关于 Conformance 的诚实口径

`HARMONY_CONFORMANCE = NOT_RUN`（**0 执行 / 91**）。
`HARMONY_CONFORMANCE_RUNNER = PASS`（runner 已存在且真的被编译，见 §2.3）。
**两者不可互相替代**，这是本轮最重要的口径分离。

- 无设备、无模拟器镜像 → runner **无法执行**，只能"编译通过"。
- **仍未**接 DevEco 本地测试框架（Hypium）→ 无替代执行面。
  这是**当前性价比最高的下一步**（见 §6 P0）。
- `tools/harmony/check-relations-semantics.mjs` 的 18/18 是**源码语义镜像**
  （Node 侧等价实现 vs 同一批 fixtures），**不是 ArkTS 运行时执行结果**，
  因此**不写入** `HARMONY_CONFORMANCE`。这个界限必须继续保持。

**§11 要求的 91 例拆分（实测，取代此前两次推算值）**：

| 分类 | 数量 | 含义 |
| --- | --- | --- |
| **已执行且通过** | **63** | 主机执行面真跑，actual 逐字节等于 expected |
| `BLOCKED_BY_RUNTIME` | **28** | 依赖 Argon2 / relationalStore 等 @ohos 能力 |
| `NOT_IMPLEMENTED` | **0** | — |
| **合计** | **91** | |

按类别：parser 22 / depmap 3 / jcs 1 / backup 1 / `migration-db-v1-to-v3` 1 = **28 blocked**；
其余 **63 全部执行且通过**（impact 13 + readiness 16 + coverage 6 + relations 18 +
scenario 1 + migration 1 + state-machine 5 + timeline 3）。

> **⚠ 两次推算值均已作废**：曾写「87 notImplemented / 4 blocked」（漏了 timeline 与
> migration 的存在），又写「60 可执行 / 3 notImplemented」（默认 state-machine 5 个
> 用例都已实现，实际只有 2 个）。两次都是**推算**，最终由自家测试
> `accountingSplitMatchesSection11` 抓出。**没有执行过的账目，就是没有被验证的账目。**
>
> 现在的意义是：**所有运行时无关用例都真的跑通并通过了**，
> 剩下的 28 个只受"没有设备运行时"这一个原因阻塞。

---

## 6. 停止点与下一步

本轮**未触发新的 stop condition**（§21 的 A–E 均未新增达成）。

**外部硬 blocker 仍是**：缺 Emulator 系统镜像（需人工下载）。
但 §10 的执行面**已不再依赖它** —— 主机执行面已打通，63/63 运行时无关用例已真跑通过。

**下一轮推荐动作（按性价比排序）**：

1. **裁决 LogicalKey 的跨端收紧**（2026-09-18 已记录为两个选项，见
   `HARMONY_N3_CONFORMANCE_REPORT.md` §7.8）—— **唯一未决的口径问题**，需人工拍板；
   在裁决前该差异既不计 PASS 也不计 FAIL。
2. **决策 timeline fixture 的欠定问题**（见下）—— 属契约质量问题，需人工裁定。
3. ArkData / Repository / migration 落地（`RUNTIME_VERIFIED` 需设备）——
   这也是把 28 个 `BLOCKED_BY_RUNTIME` 变成可执行的前提。
4. ArkUI 纵向链路（§14，**不做 Graph View 优先**）。

| 优先级 | 事项 | 前置 |
| --- | --- | --- |
| **P0** | 复核 `LogicalKey` 的 LC-003 收紧是否造成跨端行为差异 | 无 |
| **P0** | 裁定 timeline fixture 的欠定问题（见下） | 无 |
| P1 | ArkData / Repository / migration 落地 | 设备（`RUNTIME_VERIFIED`） |
| P2 | ArkUI 纵向链路（§14） | Domain |
| P2 | HUKS + 用户认证 | 设备 |
| — | 打通 `HARMONY_RUNTIME_E2E` | **用户操作**（登录 + 下载镜像） |

> **§7 备注 1（需人工决策）**：`LogicalKey.parseDependencyLogicalKey` 现在按
> runtime registry 拒绝 relation（LC-003 的意图），而 TS 基准只在**成组使用**时校验。
> 这是**本端刻意的额外收紧**，已按注释意图实现并写进报告，但它构成跨端行为差异 ——
> 建议复核后决定"保留并记录为差异"还是"对齐基准"。
>
> **§7 备注 2（fixture 质量问题）**：timeline 三个 fixture 的 `input`
> **不足以决定 `expected`** —— generator 先建真实 SQLite 场景再投影，
> 而 input 只留下 `{now, plans, graphRevision}`，场景本身不在 fixture 里。
> 三端都只能**重建同一场景**再跑投影（Android `Main.kt` 的 `runTimeline`
> 正是按 caseId 硬编码同样的 insert）。
> 因此这 3 个用例验证的是"**同一场景下投影内核是否一致**"，
> 而**不是**"input → output"。要改成真正由 input 驱动，需改动冻结的 fixture
> 生成器 —— 超出 N3 范围，故只记录不动手。

### 6.1 Hypium 前置：已在第四轮落地（本节保留为施工记录）

第三轮探明的前置，第四轮**已全部落实**：

| 项 | 状态 |
| --- | --- |
| `@kit.TestKit.d.ts` / ohpm registry / SDK API 13 | 均可用 |
| `@ohos/hypium` | 已在**工程根与 `entry/` 两处**声明 `1.0.24` |
| `@ohos/hamock` | pin 已从 `1.0.1` 改为 **`1.0.0`** ← 见下方修正 |
| `src/test` 本地单元测试 | **已建成并跑通**（57 用例 + 4 元测试） |
| `src/ohosTest` 设备测试 | 仍未建（设备侧路线，暂不需要） |

**修正记录**：`harmony/oh-package.json5` 原先声明 `"@ohos/hamock": "1.0.1"`，
但 registry 中该包**最高只有 `1.0.0`** → `ohpm install` **必然失败**：

```
ohpm ERROR: NOTFOUND package '@ohos/hamock@1.0.1' not found from all the registries
```

第三轮只做探针验证并还原；第四轮**已正式改为 `1.0.0`**（实测 install 立即成功）。
这是一个与 conformance 无关、但会卡住任何测试工作的既有缺陷。

**⚠ 上一轮记录过的既有问题（已修）**：`harmony/oh-package.json5` 曾声明
`"@ohos/hamock": "1.0.1"`，但 registry 中 `@ohos/hamock` **最高只有 `1.0.0`**
（`versions: 2`）→ `ohpm install` **必然失败**：

```
ohpm ERROR: NOTFOUND package '@ohos/hamock@1.0.1' not found from all the registries
ohpm ERROR: Install failed
```

现已改为 `1.0.0`，`ohpm install` 立即成功
（`fetch package done 1 @ohos/hamock … hamock-1.0.0.har`、`done 2 @ohos/hypium … 1.0.24.har`）。

**约束提醒（不可违反）**：不得实现 RealityDrift / IncidentPlan /
Browser Discovery / Open Banking / AI-LLM / 云同步；
**未进入 iOS N4。**

---

## 7. 复现

```bash
PDIG_DEVECO_HOME="<DEVECO_HOME>"
PDIG_HARMONY_BUILD_ROOT="C:/Users/Kaiser/pdig-harmony-build"

# 1. 第三方完整性
node tools/harmony/check-third-party-hashes.mjs

# 2. 原生交叉编译
node tools/harmony/check-argon2-native-build.mjs

# 3. 编译可达性（A+B+C+D，含负向探针）
node tools/harmony/check-compiled-reachability.mjs --build

# 4. 产物符号反查（跑后可 grep conformance 模块与函数）
node tools/harmony/probe-abc-symbols.mjs

# 5. 全清构建
node tools/harmony/build-ascii-mirror.mjs --clean assembleHap

# 6. 诊断：import 拓扑 / 孤儿模块
node tools/harmony/dump-import-graph.mjs
```
