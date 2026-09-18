# HARMONY ARGON2 INTEGRATION REPORT

> 范围：Argon2id 从「vendoring 决策」到「打包进 HAP」的完整记录。
> 本文只描述**已验证**的事实；未验证的部分明确标注为未验证。
>
> 结论先行：**vendoring PASS · 交叉编译 PASS · 打包 PASS · 信任边界 PASS；
> 设备执行 NOT_RUN（无运行时，见 `HARMONY_RUNTIME_ENVIRONMENT_AUDIT.md`）。**

- 日期：2026-09-18
- 相关：`third_party/argon2/VENDOR.json`、`THIRD_PARTY_NOTICES.md`、
  `docs/DEPENDENCY_POLICY.md`、`HARMONY_N3_IMPLEMENTATION_STATUS.md`

---

## 1. Vendoring 决策（§3）

**决策：vendored 源码，不经过 npm / ohpm。**

理由：密码学实现不应经由包管理器间接引入 —— 供应链可审计性要求
「上游 commit + 逐文件哈希 + 许可证」三者在仓库内可独立复核。

| 项 | 值 |
| --- | --- |
| 上游 | PHC reference implementation（libargon2） |
| 固定版本 | tag `20190702` |
| 固定 commit | `62358ba2123abd17fccf2a108a301d4b52c01a7c` |
| 下载件 SHA-256 | `daf972a89577f8772602bf2eb38b6a3dd3d922bf5724d45e7f9589b5e830442c`（1,505,307 字节） |
| 许可证 | 双许可 **CC0-1.0 OR Apache-2.0** |
| vendored 文件数 | **16** |
| **本地修改数** | **0**（逐文件哈希与上游一致） |
| argon2 版本常量 | `ARGON2_VERSION_13 = 19`（0x13） |

清单文件 `third_party/argon2/VENDOR.json` 记录每个文件的 `sha256` 与字节数，
由 `tools/harmony/check-third-party-hashes.mjs` 在每次运行中复核。

### 1.1 刻意排除的文件

**`src/opt.c` 未 vendored**，且是**故意**的。

`opt.c` 是 x86 SSE2 优化实现；本项目同时目标 arm64-v8a 与 x86_64。
保留它会在 arm64 上产生 `undefined reference to fill_segment`（实测）。
更稳妥的做法是**物理上不提供**该文件，使其无法被误选。
arm64 与 x86_64 均使用可移植的 `src/ref.c`。

### 1.2 完整性 Gate

```
$ node tools/harmony/check-third-party-hashes.mjs
  declared=16 onDisk=16 undeclared=0
  local_modifications declared as: 0
THIRD_PARTY_INTEGRITY=PASS
```

该 Gate 同时报告 **UNDECLARED（在盘但未登记）** 文件 ——
防止有人往 `third_party/` 里塞东西而不更新清单。

---

## 2. 原生桥接（§4）

**链路：`ArkTS → NAPI(libpdiargon2.so) → libargon2(PHC) → derivedKey`**

### 2.1 可信边界（刻意最小化）

原生层**只做 Argon2id KDF**。它：

- ❌ 不接收 JSON 容器
- ❌ 不解析 `.depmap`
- ❌ 不做 AES
- ❌ 不做 JCS

上述全部留在 ArkTS（`DepmapContainerV1.ets`）。
理由：ArkTS 侧有编译 Gate 与 conformance runner 覆盖；
原生层每多一项职责就多一份**不可验证的信任面**。

### 2.2 接口（最小面）

```
deriveArgon2id(password, salt, memoryKiB, iterations, parallelism, outputLength, version) -> Uint8Array
argon2Version() -> number
```

对应的 ArkTS 类型声明位于
`harmony/entry/src/main/cpp/types/libpdiargon2/index.d.ts`，
其 `oh-package.json5` 指向该声明文件。

### 2.3 参数不做隐式替换（§5）

规范锁定：`type=Argon2id` · `version=19` · `memoryKiB=65536` ·
`iterations=3` · `parallelism=1` · `outputLength=32`。

但桥接层**必须支持 `parallelism = 1..4` 透传**，以证明它没有偷偷 hardcode `p=1`。
`Argon2idNative.verifyNativeParameterPassthrough()` 在设备上以固定的
password/salt、仅改变 p 派生四次，要求**四者两两不同**：

- 若原生层把 p hardcode 成 1 → 四次结果相同 → **FAIL**（正是要抓的缺陷）；
- 同时校验链接进来的 libargon2 报告版本 = 19。

**参数越界一律 fail closed**，在进入 KDF 之前即拒绝。
边界常量与 spec 一致（`MEMORY_KIB_MIN 16384` … `PARALLELISM_MAX 4`）。

---

## 3. 符号可见性（实测校正，重要）

### 3.1 `A2_VISCTL` 是一个陷阱

上游 Makefile 用 `-fvisibility=hidden -DA2_VISCTL=1` 构建 `libargon2.so`。
但读 `argon2.h` 可知，`A2_VISCTL` 使

```c
#define ARGON2_PUBLIC __attribute__((visibility("default")))
```

即 **`A2_VISCTL` 的语义是「导出」而不是「隐藏」**。
上游之所以那样写，是因为它要产出供他人链接的 `libargon2.so`。

本项目是把 libargon2 **静态链接进 NAPI 模块**，不应把 `argon2_*` 暴露给 ArkTS。

### 3.2 结论

| 配置 | 动态符号表中的 `argon2_*` | 判定 |
| --- | --- | --- |
| 带 `-DA2_VISCTL=1` | **22 个** | **FAIL** |
| 不带（当前） | **0 个** | **PASS** |

只对 NAPI 入口单独开 default 可见性：

```cpp
extern "C" PDI_EXPORT __attribute__((constructor)) void RegisterPdiArgon2Module(void)
```

---

## 4. 交叉编译 Gate（§6）

```
$ PDIG_DEVECO_HOME="<DEVECO_HOME>" node tools/harmony/check-argon2-native-build.mjs
  {"target":"arm64-v8a","build":"OK","elfMachine":"AArch64","exportedArgon2Symbols":0,"napiEntryExported":true,"neededLibs":["libace_napi.z.so","libc++_shared.so","libc.so"],"bytes":40768,"verdict":"PASS"}
  {"target":"x86_64","build":"OK","elfMachine":"Advanced Micro Devices X86-64","exportedArgon2Symbols":0,"napiEntryExported":true,"neededLibs":["libace_napi.z.so","libc++_shared.so","libc.so"],"bytes":42296,"verdict":"PASS"}
ARGON2_NATIVE_BUILD=PASS
```

**状态区分（§6 明确要求）：**

| 状态 | 值 | 依据 |
| --- | --- | --- |
| `NATIVE_LIBRARY_BUILD` | **PASS** | 两个 ABI 均编译成功，ELF machine 正确 |
| `NAPI_LINK` | **PASS** | `NEEDED` 含 `libace_napi.z.so`；NAPI 入口符号已导出 |
| `ON_DEVICE_EXECUTION` | **NOT_RUN** | 无设备 / 无模拟器镜像 |

三者**必须分开报告**，不可合并成一个「Argon2 通了」。

### 4.1 打包进 HAP（端到端取证）

```
HAP: entry-default-unsigned.hap  (2,774,411 B, 11 entries)
  libs/arm64-v8a/libc++_shared.so    1261960
  libs/arm64-v8a/libpdiargon2.so       49776
  libs/x86_64/libc++_shared.so       1294440
  libs/x86_64/libpdiargon2.so          50576
  ets/modules.abc                      75076
```

**对打包并 strip 后的 `.so` 复验动态符号表：**

```
DEFINED GLOBAL SYMBOLS (3):
    31: _init
    32: _fini
    33: RegisterPdiArgon2Module   (28 bytes)

argon2_* symbols: (none)
```

这是信任边界在**最终产物**上（而非中间产物上）的验证：
strip 之后仍然恰好只有 NAPI 入口可见，`argon2_*` 为 0。

---

## 5. 未验证的部分（明确声明）

| 项 | 状态 | 原因 |
| --- | --- | --- |
| 设备上真实派生 | **NOT_RUN** | 无运行时 |
| `p=1..4` 透传的设备侧取证 | **NOT_RUN** | 同上；代码已就绪 |
| 与 Core Node / Android 的三端 Golden Vector 互操作 | **主机侧 PASS，设备侧 NOT_RUN** | 容器格式与 KDF 参数已一致，但 Harmony 侧未在真实运行时执行 |

**不得**把上述任何一项记为 PASS。

---

## 6. 构建环境坑（可复用）

1. **`SystemRoot` 会让 OHOS clang 崩溃**（`0xE06D7363`）。
   注入宿主进程的 `MToolExtend.dll` 在 `SystemRoot` 存在时走入崩溃路径。
   实测：`env={}` / `{PATH}` / `{TEMP}` 均正常，`{SystemRoot}` 必崩；
   且对空 `hello.c` 同样崩溃（与 argon2 源码无关）。
   修复：`tools/harmony/ohos-clang-build.py` 的 `child_env()` **刻意省略 `SystemRoot`**。
2. **C 源必须用 `clang` 而非 `clang++`**：`-std=c99` 不能传给 C++ 前端。
   仅桥接文件用 `clang++`（`-std=c++17`）。
3. **`third_party/` 必须一起进 ASCII 镜像**：
   `CMakeLists.txt` 用相对路径 `../../../../../third_party/argon2` 定位 vendored 源码，
   从 `harmony/entry/src/main/cpp` 上溯五级正好落到镜像根。
   镜像若只 copy `harmony/`，CMake 会 `FATAL_ERROR`。
4. **`llvm-readelf` 的 machine 名**：x86_64 显示为
   `Advanced Micro Devices X86-64`，不是 `X86-64`。
5. **符号泄漏误报**：`RegisterPdiArgon2Module` 名字里含 "Argon2"，
   用 `/argon2/i` 粗暴匹配会把它算成泄漏 —— 它恰恰是**唯一必须导出**的符号。
