# HARMONY_N3_CONFORMANCE_REPORT.md

> Harmony Conformance 报告。生成时间：2026-09-17（Asia/Shanghai）
> **最近更新：2026-09-18（第四轮 —— 主机执行面打通，57 个用例真的跑了）**
> 口径：§H —— same fixtures → ArkTS implementation → normalized output → compare expected。
> **不得把 notImplemented 或 blocked 写成 PASS。**

---

## 0. 总结论

```
HARMONY_CONFORMANCE       = NOT_RUN      （设备执行面）pass = 0 / 91
HARMONY_CONFORMANCE_HOST  = PASS         （主机执行面）pass = 57 / 57 可执行
```

**设备上仍然没有任何用例被执行过**（`hdc list targets` = `[Empty]`）。
但现在有了第二个、独立的执行面：**DevEco 本地单元测试能在无设备主机上真正执行
ArkTS 代码**，而这一轮已经用它把那 57 个运行时无关用例跑通并逐字节比对通过。

三个数字必须同时记住（总 91）：

| 分类 | 数量 | 含义 |
| ---- | ---- | ---- |
| **已执行且通过** | **57** | 在**真实 ArkTS 运行时**下 actual 逐字节等于 expected |
| `BLOCKED_BY_RUNTIME` | **28** | 依赖 Argon2 / relationalStore 等 @ohos 能力，主机无实现 |
| `NOT_IMPLEMENTED` | **6** | timeline ×3 + state-machine ×3，runner 侧尚未实现 |

### 0.0 本轮最重要的修正：账目本身是错的

上一版（第三轮）写的是「**60 可执行 / 3 notImplemented**」。那是**推算**的，不是实测的：
我当时默认 state-machine 的 5 个用例都已实现，实际只有 2 个。
真实执行后账目是 **57 / 28 / 6**。

> **教训一句**：没有执行过的账目，就是没有被验证的账目。
> 而且这个错误是**自家测试自己抓出来的** —— `accountingSplitMatchesSection11`
> 断言了三个数必须精确等于实测值，推算值一放进去就炸。

### 0.1 本轮交付（第四轮：主机执行面）

| # | 产出 | 路径 | 说明 |
| - | ---- | ---- | ---- |
| 1 | fixture 文本源抽象 | `conformance/ConformanceRunner.ets` | runner 改为**零 @ohos 依赖** |
| 2 | 设备侧 fs 文本源 | `conformance/FsTextSource.ets` | 唯一允许 import `@ohos.file.fs` 的模块 |
| 3 | fixture 内嵌生成器 | `tools/conformance/embed-fixtures.mjs` | 91 个 fixture → ArkTS 模块；`--check` 守漂移 |
| 4 | **主机 conformance 测试** | `harmony/entry/src/test/ConformanceHost.test.ets` | 逐用例生成 `it()`，57 条 |
| 5 | 主机 domain 自检 | `harmony/entry/src/test/DomainHost.test.ets` | 15 项自检真跑 |
| 6 | 门禁驱动 | `tools/harmony/run-conformance-host.mjs` | `HARMONY_CONFORMANCE_HOST` 判定 |
| 7 | 镜像修复 | `tools/harmony/build-ascii-mirror.mjs` | oh_modules 解引用复制（见 §7.2） |

### 0.2 四条 gate 必须分开看（本报告最重要的一节）

| Gate | 状态 | 依据 |
| ---- | ---- | ---- |
| `HARMONY_CONFORMANCE_RUNNER_IMPLEMENTED` | **PASS** | 4 模块落地，runner 本体已模块化 |
| `HARMONY_MODULE_COMPILED` | **PASS** | **23/23** required；A+B+C+D 四判据 |
| **`HARMONY_CONFORMANCE_HOST`** | **PASS** | **57/57** 运行时无关用例在真实 ArkTS 下逐字节复现 |
| `HARMONY_DOMAIN_HOST` | **PASS** | 15/15 域自检在主机真跑全绿（修复 3 个缺陷后） |
| `HARMONY_CONFORMANCE_EXECUTED` | **NOT_RUN** | 设备上 0 个用例真正执行 |
| **`HARMONY_CONFORMANCE`** | **NOT_RUN** | 只有设备上 **91/91** 全绿才允许写 PASS |

前四行的 PASS **一个都不能**被写成 `HARMONY_CONFORMANCE = PASS`。
`HARMONY_CONFORMANCE_HOST` 覆盖的是 57/91 这个子集，
把它当成 91/91 会让分母失去意义。

### 0.3 阻塞原因（已更新）

1. **无 Harmony 运行时（设备侧）**：`hdc list targets` = `[Empty]`；本机无模拟器系统镜像。
   → 主机执行面**缓解但不消除**这一项：28 个 @ohos 依赖用例仍然只能等设备。
2. **fixture 尚未投放到设备沙箱**：#1 解决后仍需 `hdc file send`
   （命令已固化进 `HarnessFs.harnessHint()`）。
3. ~~**未接本地测试框架**~~ → **已解决**：机机执行面已打通（§7）。
4. **6 个用例尚未实现**：timeline ×3 + state-machine 的
   action-verification / discovery-candidate / reality-drift。
   它们是 runner 侧的实现缺口，**不是**环境问题。

---

## 1. 为什么必须是真的 ArkTS runner（§10 原文要求）

§10 的原话是 "Real Harmony Conformance Runner (not a Node mirror masquerading as ArkTS)"。
本轮把这句话拆成三条**可验证**的要求：

### 1.1 它必须在 ArkTS 里真算，而不是转发给 Node

ArkTS 有一条硬约束决定了实现形态：
**禁止 `any` / 动态索引 / 索引签名，`JSON.parse` 的返回值无法映射到 class 实例。**
用 `const doc: Record<string, Object> = JSON.parse(t)` 收下再取字段会被编译期直接拒绝。

因此 runner 采用**受控文本扫描**：`JsonText` 在原始文本上定位对象/数组，
数组元素被切成各自独立的 `[start, end)` 窗口后再逐字段装载。
关键在于**窗口化**：若在一个大窗口里反复 `findField('done')`，
第一个元素的 `done` 会被后续每个元素读到 ——
于是"两个动作只完成其一"这个用例会**假通过**。
窗口化之后每个元素只能看到自己的字段。

装载也不是"通用 JSON 解析"：每个字段都经过 wire→enum 转换，
未知取值直接抛错（fail-closed），不猜、不兜底。

### 1.2 它必须真的执行 Domain 内核，而不是只检查形状

最容易滑向假 runner 的一步，是让它做形状检查（"expected 里有 targets 字段吗"）
而从不调用 Impact 内核 —— 那样跑出的 13/13 毫无证据力。

硬性要求：`impact` 分类由 fixture 的 `input` 重建 `ImpactGraph`，
**真正调用 `impact/ImpactKernel.simulateScenario`**，再序列化比对。

### 1.3 它必须在可达编译图内

这是本项目已经踩过一次的坑（见 `HARMONY_CONTAINER_V1_POC.md`）：
源文件存在 + `BUILD SUCCESSFUL` ≠ 已编译。可达链：

```
pages/Index.ets
  → conformance/ConformanceSelfCheck.ets
      → conformance/ConformanceRunner.ets
          → { JsonText, HarnessFs,
              ImpactKernel, PlanReadiness, ScenarioCoverage, Relations,
              ScenarioTemplate, StateMachines, GraphRevision, Timeline,
              CanonicalWire, Entities, CanonicalEnums }
```

已登记进 `check-compiled-reachability.mjs` 的 `REQUIRED_MODULES`。

---

## 2. 产物级取证（不是推断）

### 2.1 编译

```
node tools/harmony/build-ascii-mirror.mjs --clean assembleHap
→ BUILD SUCCESSFUL in 15 s 350 ms；CompileArkTS 完成；EXIT=0
```

### 2.2 `modules.abc` 符号（243,556 bytes）

对四个新模块**全部**在产物中：

```
sLcom.pdig.depmap/entry/ets/conformance/ConformanceRunner;
yLcom.pdig.depmap/entry/ets/conformance/ConformanceSelfCheck;
cLcom.pdig.depmap/entry/ets/conformance/HarnessFs;
aLcom.pdig.depmap/entry/ets/conformance/JsonText;
```

runner 的关键函数逐个可查：

```
com.pdig.depmap/entry/ets/conformance/ConformanceRunner.runConformance
com.pdig.depmap/entry/ets/conformance/ConformanceRunner.computeCase
com.pdig.depmap/entry/ets/conformance/ConformanceRunner.runImpactSingle
com.pdig.depmap/entry/ets/conformance/ConformanceRunner.runReadiness
com.pdig.depmap/entry/ets/conformance/ConformanceRunner.runCoverage
com.pdig.depmap/entry/ets/conformance/ConformanceRunner.runRelations
com.pdig.depmap/entry/ets/conformance/ConformanceRunner.runStateMachine
com.pdig.depmap/entry/ets/conformance/ConformanceRunner.runScenario
com.pdig.depmap/entry/ets/conformance/ConformanceRunner.runMigrationContract
com.pdig.depmap/entry/ets/conformance/ConformanceRunner.extractExpected
com.pdig.depmap/entry/ets/conformance/ConformanceRunner.serializeImpact
com.pdig.depmap/entry/ets/conformance/HarnessFs.locateConformanceRoot
com.pdig.depmap/entry/ets/conformance/ConformanceSelfCheck.conformanceSummaryLine
com.pdig.depmap/entry/ets/conformance/ConformanceSelfCheck.conformanceRun
```

**（本轮补强）取证脚本本身已修正**：`probe-abc-symbols.mjs` 原先的 `TARGETS`
只覆盖 3 个 crypto 模块，**完全不看 conformance** —— 也就是说上一轮"符号已取证"
这句话对 runner 而言是**用错工具的结论**。本轮把它扩为 7 个模块目标
（+4 个 conformance）并新增**函数级**判据（13 个入口），实测：

```
--- 目标模块命中 ---
  PRESENT  entry/ets/crypto/Jcs
  PRESENT  entry/ets/crypto/DepmapContainerV1
  PRESENT  entry/ets/crypto/ContainerSelfCheck
  PRESENT  entry/ets/conformance/JsonText
  PRESENT  entry/ets/conformance/HarnessFs
  PRESENT  entry/ets/conformance/ConformanceRunner
  PRESENT  entry/ets/conformance/ConformanceSelfCheck

--- runner 函数符号 ---
  PRESENT  runConformance / computeCase / extractExpected
  PRESENT  runImpactSingle / runReadiness / runCoverage / runRelations
  PRESENT  runStateMachine / runScenario / runMigrationContract
  PRESENT  serializeImpact / locateConformanceRoot / conformanceSummaryLine

ABC_VERDICT=PRESENT   (EXIT=0)
```

> 函数级判据的意义：**模块路径出现在 `modules.abc` 里**并不自动意味着
> **函数体被保留**（理论上仍可能被裁剪）。逐入口确认符号驻留，
> 才把"该模块整体未被 tree-shake"收紧到"这些入口确实在产物中"。

`JsonText` 的方法嵌套在类符号内（无点式条目），其存在由类条目
`JsonText` / `ocom.pdig.depmap/entry/ets/conformance/JsonText.JsonText`
以及六条自带错误串证明：
`JsonText: required string field not found in window:` /
`JsonText: required number array not found:` /
`JsonText: unterminated array for field:` 等。

### 2.3 可达性门禁

```
required modules              : 22
all required reachable (A)    : true
all required in modules.abc(C): true
HARMONY_COMPILE_REACHABILITY=PASS      (EXIT=0)
```

24 个 ets 模块，24 个可达，**0 个孤儿**。

### 2.4 编译期真实拦截 —— 本轮最有价值的证据

接线之前先跑了一次构建，当时 `Index.ets` 还没 import conformance，
构建**成功**（15 s 102 ms）。接线之后再构建，**立即失败**：

```
COMPILE RESULT:FAIL {ERROR:8 WARN:2}
  ConformanceRunner.ets:676  Use explicit types instead of "any", "unknown" (arkts-no-any-unknown)
  ConformanceRunner.ets:36   Module '"../domain/CanonicalWire"' has no exported member 'CanonicalWire'
  ConformanceRunner.ets:57   '"../domain/Entities"' has no exported member named 'CoverageSourceInput'.
  ConformanceRunner.ets:59   Module '"../domain/Entities"' has no exported member 'RelationValidationResult'
  ConformanceRunner.ets:210  Property 'root' does not exist on type 'ConformanceReport'
  ConformanceRunner.ets:613  Type 'string' is not assignable to type 'ChangePlanWorkflowState'
  ConformanceRunner.ets:627  Type 'string' is not assignable to type 'PlanActionPhase'
```

这 7 类错误是**驳不倒的正面证据**：既证明接线前那次"成功"是空证据
（放进去的文件根本没被编译），也证明接线后该文件真的进入了 `CompileArkTS`。
全部修复后构建恢复绿。

> 自我纠错记录：`CanonicalWire`（我写的未使用导入）、
> `CoverageSourceInput`（实为 `CoverageSourceInfo`）、
> `RelationValidationResult`（应从 `../domain/Relations` 导入）
> 这三处都是凭印象写错的**我自己的**名字，被编译器逐条纠正。
> 这正是保留真实 build 的价值 —— 光看代码是看不出来的。

---

## 3. 逐分类状态（本轮重算）

| 分类 | 用例数 | Harmony 实现 | 本轮归属 | 说明 |
| --- | --- | --- | --- | --- |
| relations | 18 | COMPILED | **可执行** | 直接调 `validateRelationUse` / `validateRelationGroupUse` |
| readiness | 16 | COMPILED | **可执行** | 直接调 `computePlanReadiness` |
| impact | 13 | COMPILED | **可执行** | 重建图后**真调** `simulateScenario` |
| coverage | 6 | COMPILED | **可执行** | 直接调 `computeScenarioCoverage` |
| state-machine | 5 | COMPILED | **可执行** | `allowedNextStates` / `isTerminalState` / `doesMutationBumpRevision` |
| scenario | 1 | COMPILED | **可执行** | 读 `SCENARIO_TEMPLATES` / `PLANNED_TEMPLATES` 注册表 |
| migration | 2 | 部分 | 1 **可执行** + 1 `BLOCKED_BY_RUNTIME` | `version-contract` 纯静态可跑；`db-v1-to-v3` 需 relationalStore |
| timeline | 3 | 内核已 COMPILED | **NOT_IMPLEMENTED** | 见 §3.1 |
| parser | 22 | 未开工 | `BLOCKED_BY_RUNTIME` | 需 `TextDecoder('gb18030')` + 字节读取；ArkTS adapter 未移植 |
| depmap | 3 | COMPILED | `BLOCKED_BY_RUNTIME` | expected 全含 `derivedKeyHex`/`ciphertextBase64`/`tagBase64`，必须真跑 Argon2id+AES-GCM |
| jcs | 1 | COMPILED | `BLOCKED_BY_RUNTIME` | 需真实异常类型名 + JCS 内核 |
| backup | 1 | 未开工 | `BLOCKED_BY_RUNTIME` | 需 DB 导出/恢复往返 + 加密容器 |
| **合计** | **91** | — | **63 可执行 / 28 blockedByRuntime / 3 timeline NOT_IMPLEMENTED** | **0 执行** |

> 计数说明：63 = 18+16+13+6+5+1+1+3(timeline 计入分母但不在可执行集)。
> 精确口径：**可执行 60（relations 18 + readiness 16 + impact 13 + coverage 6 +
> state-machine 5 + scenario 1 + migration-version-contract 1）+ timeline 3 NOT_IMPLEMENTED
> + 28 blockedByRuntime = 91**。

### 3.1 timeline 为什么是 NOT_IMPLEMENTED 而不是 PASS

`timeline` 3 个用例的 expected 里都含**来自仓库读取的条目**
（`tl-fresh-legacy-wechat-statement` 需要 `SourceInstanceRepository`；
`timeline-attention-signals` 需要 Drift 行；
`timeline-terminal-plans-excluded` 需要 DB 里的计划）。

Harmony domain 层的 `Timeline.ets` **只保留了分类与排序内核**
（`bucketOf` + `sortTimelineKeys`），条目收集属 data 层且尚未落地 ——
这是该文件头部已显式记录的有意差异。因此 runner 对这 3 个用例返回空串
→ `NOT_IMPLEMENTED`。

**如实记账的意义**：把"只有内核"报成"通过"，等于声称已经实现了尚未实现的仓库读取。
`NOT_IMPLEMENTED` 既不是 FAIL 也不是 PASS，这是 harness 的既有契约。

### 3.2 为什么 `migration` 要按**用例**而非**分类**切

`migration` 类里两个用例的依赖完全不同：

- `migration-version-contract` —— 纯静态版本契约，**能跑**；
- `migration-db-v1-to-v3` —— 需真实 DB 迁移，**跑不了**。

若按分类一刀切，一个可执行用例会被从分母里抹掉 —— 那是隐性降级门禁。
因此 runner 里 `RUNTIME_DEPENDENT_CATEGORIES` 与 `RUNTIME_DEPENDENT_CASE_IDS`
是两张表，后者精确到用例 id。

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
> `check-compiled-reachability.mjs` 的 PASS、`check-argon2-native-build.mjs` 的 PASS、
> **以及本轮新增的 runner 编译取证** —— 全部是**主机侧 / 编译期证据**，
> 没有一条是 conformance 执行证据。
> 把它们混入 91/91 会稀释掉 `0/91` 这个真实状态。

---

## 5. 达成 91 / 91 的路径（本轮已推进到第 2 步）

1. **打通执行面**（前置，最高优先）
   - 在 DevEco 下载模拟器系统镜像 → 可跑 `ohosTest`；或
   - **接入 DevEco 本地单元测试框架（Hypium）→ 脱离设备执行**。
   *在此之前，任何 conformance 数字都只能是 NOT_RUN。*
2. ~~接 runner~~ —— **本轮完成**：读 `fixtures/`（与 Android 同一批）→
   ArkTS 实现 → 归一化 → 比对 `expected`；**未手抄任何 expected**。
3. **投放 + 执行**：`hdc file send` 推 `conformance/` + `fixtures/` 进沙箱，
   读 `ConformanceSelfCheck.conformanceSummaryLine()` 的真实计数。
4. **补 `depmap(3)` + `backup(1)`**：代码侧已就绪，等运行时到位后执行，
   并用**同一 Golden Vector** 与 Android / Legacy 冻结基准对齐
   （derivedKey / ciphertext / tag / AAD-JCS）。

### 5.1 分阶段可达的近期目标

| 阶段 | 内容 | 需要运行时？ | 状态 |
| --- | --- | --- | --- |
| 1 | Domain 11 组落地 | 否 | **完成**（COMPILED） |
| 2 | ArkTS conformance runner 落地并进编译图 | 否 | **完成（本轮）** |
| 3 | 接 Hypium 或投放沙箱后执行 60 个可执行用例 | 否（若能脱离设备） | 待做 |
| 4 | 执行 28 个 `BLOCKED_BY_RUNTIME` 用例 → `91/91` | **是** | 待做 |

**阶段 3 仍不需要设备**（若 Hypium 可脱离设备运行），是当前性价比最高的推进方向。
把 91/91 拆成「60 可自行达成 + 3 待补实现 + 28 等外部资源」，
比笼统地说「等运行时」更有行动力。

---

## 6. 本轮明确**不**主张的事

| # | 不主张 | 理由 |
| - | ------ | ---- |
| 1 | 任何一个用例"通过" | 一次都没执行过 |
| 2 | 比对逻辑（键序、序列化顺序）已正确 | 本机无 ArkTS 运行时，无法自证 |
| 3 | 60 这个数字会变成 60/60 | 未执行即未知 |
| 4 | `HARMONY_CONFORMANCE` 有任何进展 | 取值仍为 `NOT_RUN` |

### 6.1 一条已被本轮推翻的方法论限制（保留原文以便对照）

第三轮曾写下：

> ArkTS 没有可在 Node 上跑的等价物（这恰恰是 §10 禁止"Node 镜像"的原因），
> 因此**本轮无法在本机证明 runner 的比对逻辑是对的**。

**这条限制已经在第四轮被推翻。** 推翻它的不是"找了个 Node 替身"，
而是找到了一个**真的 ArkTS 执行面**：DevEco 的本地单元测试
（详见 §7）。它执行的是**同一个 ConformanceRunner**，不是另写一份实现。

保留这段原文，是因为它记录了一个真实的推理跳跃：
"没有设备"被当成了"没有执行面"，而实际上还有第二个执行面存在 ——
**一个看起来不可绕过的阻塞，值得再问一次"真的吗"。**

---

## 7. 主机执行面（第四轮，本轮的核心发现）

### 7.1 它是怎么被找到的

原判断是「无设备 → 无法执行 → conformance 只能等设备」。本轮把这句话拆开逐项验证，
结果发现**存在第二个执行面**：

| 候选路径 | 实测结果 |
| -------- | -------- |
| `ark_js_vm`（ArkTS 独立虚拟机） | **不存在**。SDK 只带 `es2abc.exe`（编译器），无运行时 |
| `Previewer.exe` | 存在，但需 `-j <app path>` + 与 IDE 的 socket 管道；且 `@ohos` 模块是预览桩 |
| **hvigor `test` 任务（本地单元测试）** | ✅ **可用**。编译 ArkTS → 执行 → 产出 `test_result.txt` |

`Previewer.exe` 被**弃用**（不是"试不通就放弃"，而是它作为证据通道不成立）：
它的 `@ohos.*` 行为与真实实现不同，用它算出来的 PASS 无法归因到 ArkTS 实现本身。

### 7.2 打通执行面必须解决的 4 个具体问题

| # | 问题 | 现象 | 解法 |
| - | ---- | ---- | ---- |
| 1 | `oh_modules` 未进镜像 | `Cannot find module '@ohos/hypium'` | 镜像时一并复制（**两处**：工程根与 `entry/`） |
| 2 | **`ohpm` 用符号链接组织包** | 复制出来是**空壳**，报 `Failed to resolve OhmUrl` | `copyTree` 对链接取 `statSync` 并**解引用复制** |
| 3 | 不能做 junction | 顺链解析回仓库（非 ASCII）→ `OhmUrl` 失败 | 必须真实复制 |
| 4 | `@ohos/hypium` 须在**模块级**声明 | `has dependency which is not installed at its oh-package.json5` | 写进 `entry/oh-package.json5` |
| 5 | `@ohos/hamock@1.0.1` **在 registry 不存在** | `ohpm install` 必然失败 | 降至 `1.0.0`（registry 最高仅 1.0.0） |

> 第 2 条最隐蔽：`Dirent.isDirectory()` 与 `isFile()` 对符号链接**都返回 false**，
> 于是循环直接 `continue` 跳过，**不报错**。表现为"依赖装好了但解析不到"。
> 这类"静默跳过"比报错更难查。

### 7.3 主机执行面的边界（不可越过的三条）

1. **主机对 `@ohos.*` 只提供不可调用的桩**：实测 `fs.readTextSync(...)` →
   `is not callable`。因此 fixture 文本必须以**数据**形式编译进去（`FixtureBundle.ets`）。
2. **因此 28 个 @ohos 依赖用例在主机上仍记 `BLOCKED_BY_RUNTIME`** ——
   主机执行面**不**覆盖它们，也**不**产出 91/91。
3. **它仍然不是设备运行时**。`HARMONY_CONFORMANCE` 保持 `NOT_RUN`。

### 7.4 为什么这不违反 §10

§10 禁止的是 "**Node mirror masquerading as ArkTS**"，即**另写一份 Node 实现**去跑 fixtures。
本方案的不同之处：

| | 被执行的代码 | 数据 | 结论来源 |
| - | - | - | - |
| `check-relations-semantics.mjs`（**禁止计入**） | 另写的 Node 等价实现 | 同批 fixtures | Node 实现自己 |
| **`HARMONY_CONFORMANCE_HOST`（本轮）** | **ConformanceRunner 本身**（真 ArkTS 编译器产出） | 冻结 fixture 原文，逐字节内嵌 | **ArkTS runner 自己** |

内嵌数据由 `embed-fixtures.mjs --check` 守门：内嵌副本一旦与冻结原件不一致，
门禁立即失败（它是 `run-conformance-host.mjs` 的**第一道**检查，先于执行）。

### 7.5 真实执行一上来就抓到了 3 个缺陷

这是本节最值得记住的部分：**前两个缺陷，代码审阅三轮都没发现。**

| # | 缺陷 | 位置 | 性质 |
| - | ---- | ---- | ---- |
| 1 | **比对基准口径错**：拿美化 JSON 与紧凑 JSON 逐字符比 | runner `extractExpected` | 41/60 用例假失败 |
| 2 | **自造契约词汇表**：白名单用 `dependency_created` 等自造名 | `GraphRevision.ets` | 契约里**每个** mutation 都被判"不提升" |
| 3 | **实现与自身注释不符**：注释要求按 registry 判定，代码按枚举判定 | `LogicalKey.ets` | `bound_to` 被错误放行 |

另有 1 个**测试自身的缺陷**（见 §7.6）：`PlanReadiness` 的"全清"输入其实不清。

**缺陷 2 尤其值得记住**：`DomainSelfCheck` 用**同一套自造词**去断言同一套自造词，
自洽地全绿 —— 自比自的检查不构成证据。它需要一份**外部**词汇表（冻结 fixture）
才暴露得出来。

缺陷 1 的修正方式也值得记：不是"把 expected 改成紧凑"（那是改冻结契约），
而是在 runner 侧新增 `compactJson`，把**两侧**都规范化到紧凑形式再比 ——
与 harness 的权威口径 `JSON.stringify(actual) === JSON.stringify(expected)`
逐字对齐（空白无关、**键序相关**）。

### 7.6 一条差点混过去的假绿

主机测试最初这样断言每个用例：

```ts
expect(o.actual).assertEqual(o.expected);
```

而 runner 在**抛异常**时把 `actual` 与 `expected` 都留成**空串** ——
`expect('') === expect('')` 恒成立，于是**"实现崩了"与"通过"在报表上完全一样**。
`state-machine-graph-revision` 就是这样"通过"了一轮。

修法是先钉 `detail` 为空，再比 payload：

```ts
expect(o.detail).assertEqual('');       // 崩了就有 detail
expect(o.actual).assertEqual(o.expected);
```

**教训**：断言必须能把"没有结果"和"结果正确"区分开。
只比一个可能为空的值，等于没比。

### 7.7 复现

```bash
export PDIG_DEVECO_HOME="<DEVECO_HOME>"
node tools/conformance/embed-fixtures.mjs          # 生成内嵌 fixtures（改了 fixture 才需重跑）
node tools/harmony/run-conformance-host.mjs        # 镜像 → hvigor test → 解析 → 判定
# 期望：
#   HARMONY_CONFORMANCE_HOST=PASS
#   汇总 : run=61 pass=61 fail=0 error=0
```

`--no-build` 可只解析上次结果；无 `PDIG_DEVECO_HOME` 时报 `NOT_RUN` 并 exit 0
（不让缺 SDK 的机器变成假失败）。

