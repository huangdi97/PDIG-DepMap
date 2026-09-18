# HARMONY COMPILE REACHABILITY GATE

> 固化「ArkTS 模块算不算已编译」的判定标准。
> 本文是 `tools/harmony/check-compiled-reachability.mjs` 的设计依据与使用说明。
>
> 一句话：**"源文件存在" + "HAP BUILD SUCCESSFUL" ≠ "已编译"。**

- 日期：2026-09-18
- 工具：`tools/harmony/check-compiled-reachability.mjs`
- 诊断辅助：`tools/harmony/dump-import-graph.mjs`

---

## 1. 要杀死的假结论

hvigor 的 `CompileArkTS` **只编译从 ability / page 可达的模块**。
未被任何 `EntryAbility` / page 通过 `import` 到达的 `.ets` 文件
**根本不进入编译图**。

实测（`HARMONY_CONTAINER_V1_POC.md` §4 首次发现，本轮再次复现）：
在一个不可达文件里放语法错误甚至类型错误，`assembleHap` 依然
`BUILD SUCCESSFUL`。

因此：

```
"源文件存在" + "HAP BUILD SUCCESSFUL"  =  "已编译"      ← 假结论
```

这是本项目见过**最危险**的一类假信号：构建是绿的，产物是真的，
但你想验证的代码从未被编译器看过一眼。

### 1.1 本轮的真实案例

`crypto/Argon2idNative.ets` 写完时，它 `import { Argon2idDeriver } from './DepmapContainerV1'`。
这条 import 让依赖方向**反转**：

```
逻辑上：  DepmapContainerV1  →  Argon2idNative      （容器依赖派生器）
代码上：  Argon2idNative     →  DepmapContainerV1   （为拿一个类型）
```

后果：**没有任何模块 import `Argon2idNative`**，它成为孤儿 ——
源文件存在、从未被编译、构建依然成功、NAPI 绑定完全不可验证。

修法：把类型抽到中立的 `crypto/KdfContract.ets`，两侧都只依赖它，
得到无环前向 DAG；再由 `pages/Index.ets` 直接 import `Argon2idNative` 建立入边。

> **教训：新 ArkTS 模块必须同时确认它有「入边」。只写出边不算接入。**

---

## 2. 判定标准（A / B / C / D）

只有四项**同时**成立，才允许 `HARMONY_MODULE_COMPILED = PASS`。

| 判据 | 含义 | 证据来源 |
| --- | --- | --- |
| **A** 可达 | 模块在 ability/page 可达的 import graph 中 | 静态解析 import 边 + 从入口 BFS |
| **B** clean build | `--clean assembleHap` 成功 | 构建输出 |
| **C** 产物符号 | `modules.abc` 中存在该模块 | 扫描 `modules.abc` |
| **D** 负向 probe | 注入真实类型错误能让构建**真的失败** | 注入 + 重新 clean 构建 |

A 单独是**纯静态**结论，不需要 SDK；B/C/D 需要真实构建。

### 2.1 C 对「纯类型模块」必须豁免（实测校正）

只含 `type` / `interface` 声明的模块，在 emit 后**被整体擦除**，
`modules.abc` 里**不会、也不应该**出现它的符号。

实测：`KdfContract.ets` 在 `modules.abc` 中查无 `KdfContract` / `Argon2idDeriver`，
而同目录的 `DepmapContainerV1` / `Argon2idNative` 都在。

对这类模块，C 判据改为「**被至少一个可达的非 typeOnly 模块 import**」，
通过值记为 `TYPE_ONLY`。

> 若不豁免，会把一个**正确的编译结果**判成 FAIL ——
> **假阴性**与假阳性同样有害。Gate 的价值在于判定准确，不在于判定严格。

### 2.2 D 必须 `--clean`

hvigor 的 `CompileArkTS` 有增量缓存。只改源码而不清目录时，
它会直接复用上一次的编译结果，于是「注入类型错误后构建仍然成功」——
**那是缓存造成的假象，不是可达性证据**。

实测：不带 `--clean` 时，本 probe 对**所有**模块都误报 FAIL。

### 2.3 D 只跑一个代表模块

负向 probe 每次是一个 **clean 全量构建**，本机实测单次约 15–30 秒
（含 native 编译）。7 个 required 模块串行全跑要数分钟，
既拖垮工具链，也让人不愿跑这个 Gate ——
**一个没人跑的 Gate 等于没有 Gate。**

负向可达性本质上是**编译图的整体性质**，不是单文件的独立性质：
只要探针模块确实在从入口可达的那张图里，注入类型错误就必然让构建失败。

因此只用一个代表模块做负向验证（默认首个 required 且非 typeOnly 的），
其余模块复用该结论，并在报告中**显式标注复用来源**：

```
Argon2idNative   true  true  true  PASS (via CanonicalEnums)  PASS
```

复用是刻意的设计，但**必须可见** —— 不能把「没测」伪装成「测过了」。
可用 `--probe-module <id>` 指定代表模块做针对性复验。

### 2.4 D 判定要三层从严

只看「构建失败」是不够的 —— native 编译失败、超时、环境缺失都会让构建失败。
判定必须依次确认：

1. 失败落在 **`CompileArkTS`** 阶段（可达性的直接证据）；
2. 失败信息里出现**目标文件**；
3. 出现 **ArkTS 类型错误**字样。

三层任一不满足即判 FAIL，并输出实际观察到的内容供定位。

---

## 3. 使用

```bash
# 纯静态（A + C）。无 SDK 也能跑，缺产物时记 PARTIAL_WITH_REPORT
node tools/harmony/check-compiled-reachability.mjs

# 完整（A + B + C + D）。需要 PDIG_DEVECO_HOME
PDIG_DEVECO_HOME="<DEVECO_HOME>" node tools/harmony/check-compiled-reachability.mjs --build

# 指定代表模块做针对性负向复验
PDIG_DEVECO_HOME="<DEVECO_HOME>" node tools/harmony/check-compiled-reachability.mjs --build --probe-module Argon2idNative

# 定位断边 / 孤儿模块
node tools/harmony/dump-import-graph.mjs
```

状态取值受限（与 §19 的 gate 词表一致）：
`PASS` / `FAIL` / `PARTIAL_WITH_REPORT` / `NOT_IMPLEMENTED` / `NOT_RUN`。

---

## 4. 残留自愈（血的教训）

负向 probe 的实现是「**改真实源码 → 构建 → `finally` 还原**」。

**若进程被强杀，`finally` 不会执行**，注入的类型错误会**留在源码树里污染仓库**。

实测发生过：进程被本机沙箱的批量删除守卫静默杀死
（表现为 exit 127、无 stderr），`CanonicalEnums.ets` 被留下 3 行
`const __pdiTypeProbe: number = 'not a number';`。

旧实现更糟：它还要 `rmSync` 一个递归镜像目录，更早触发守卫。

**现设计：**

1. 不做镜像 `cpSync` / `rmSync`（那是最初的触发点）；
2. 每次 Gate 启动先扫 `__pdiTypeProbe` 残留，**明确报告**后自愈；
3. 注入后**读回校验**，确认改动真的落盘（否则 probe 在验证一个没被改过的文件）。

> 第 2 点是刻意的：**静默清理会让「上次是脏死」这个事实消失**。
> 一个会自己恢复但不说实话的工具，比一个会报错的工具更危险。

---

## 5. 当前结果

```
$ PDIG_DEVECO_HOME="<DEVECO_HOME>" node tools/harmony/check-compiled-reachability.mjs --build

[reachability] ets modules found : 9
[reachability] entry roots       : entryability/EntryAbility.ets, pages/Index.ets
[reachability] reachable         : 9
[reachability] modules.abc       : 75076 bytes

module                     present  reachable  inAbc       negative               status
CanonicalEnums             true     true       true        PASS (via CanonicalEnums) PASS
Relations                  true     true       true        PASS (via CanonicalEnums) PASS
Jcs                        true     true       true        PASS (via CanonicalEnums) PASS
KdfContract                true     true       TYPE_ONLY   NOT_RUN                PASS
DepmapContainerV1          true     true       true        PASS (via CanonicalEnums) PASS
ContainerSelfCheck         true     true       true        PASS (via CanonicalEnums) PASS
Argon2idNative             true     true       true        PASS (via CanonicalEnums) PASS

  required modules              : 7
  all required reachable (A)    : true
  all required in modules.abc(C): true

HARMONY_COMPILE_REACHABILITY=PASS
```

`--probe-module CanonicalEnums` 的负向证据原文：

```
> hvigor ERROR: Failed :entry:default@CompileArkTS...
1 ERROR: ArkTS:ERROR File: .../generated/CanonicalEnums.ets:350:7
COMPILE RESULT:FAIL {ERROR:2 WARN:1}
> hvigor ERROR: BUILD FAILED in 15 s 56 ms
```

---

## 6. 维护约定

新增 Domain / Repository / Security 模块时：

1. 在 `REQUIRED_MODULES` 中登记（`required: true` 表示缺失即 Gate 失败）；
2. 纯类型模块标记 `typeOnly: true`；
3. **建立入边** —— 从某个可达模块 import 它，
   否则它不会进编译图，Gate 会（正确地）报 FAIL；
4. 跑一遍完整 Gate 确认 A/B/C/D 全绿。
