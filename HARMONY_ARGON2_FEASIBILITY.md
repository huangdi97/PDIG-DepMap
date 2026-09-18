# HARMONY_ARGON2_FEASIBILITY.md

> Harmony N3 · Argon2 路径可行性深挖（**不改动 `.depmap` 协议**）
>
> 生成时间：2026-09-18（Asia/Shanghai）
> 关联：`HARMONY_N3_BASELINE_AUDIT.md`、`spec/security/depmap-container-v1.json`、
> `docs/DEPENDENCY_POLICY.md`、`docs/DEPMAP_FORMAT.md`、`docs/NATIVE_MIGRATION.md`
>
> 原则：**先取证、先 PoC，再谈实现**。不得把"编译产物存在"说成"设备上可用"；
> 不得把"链接外部参考实现"说成"自研原语"。

---

## 0. 结论

| 路径 | 结论 | 证据强度 |
| --- | --- | --- |
| `cryptoFramework`（托管 KDF） | **排除** | 直接读 SDK 类型声明，Argon2 命中 **0** |
| `HUKS`（托管密钥/派生） | **排除** | 直接读 SDK 类型声明，Argon2 命中 **0** |
| SDK 自带 OpenSSL / libsodium | **不存在** | 递归检索 `openharmony/native`，仅命中 CMake 的 `FindOpenSSL` 文档 |
| `hash-wasm` 的 `argon2.c` 当原生源 vendored | **排除** | 该文件是 WASM 目标实现，依赖 `__builtin_wasm_memory_grow` |
| **NDK + PHC 参考实现 + NAPI 桥接** | **可行（编译期已验证）** | 已产出 arm64 `.so`；主机侧 PoC 逐字节复现 Golden Vector |

**`HARMONY_DEPMAP` 的状态迁移**：`BLOCKED` → **`BLOCKED_BY_NATIVE_VERIFICATION`**。

- 从"平台没有能力、且禁止自研原语"变成"**能力链路已打通，只差设备上复验**"；
- 但**不能**改为 PASS —— 设备上一次都没跑过（`hdc list targets = [Empty]`，无模拟器镜像）。

**这不违反"禁止自研 Argon2/AES primitive"**：KDF 本体是 PHC 参考实现（Argon2 官方参考实现，
双许可 CC0-1.0 OR Apache-2.0），桥接层只做参数编组，不实现任何密码学运算。

---

## 1. 不可协商的约束

1. `DEPMAP_CONTAINER_V1` 已 FROZEN，**本轮不改协议**（要改须新增 `formatVersion`）。
2. `argon2id / version 19` 是**不可降级的兼容性闸门**（不是性能选项）。
3. spec K 节：**禁止自研 Argon2 / AES primitive**（允许链接经审计的外部实现）。
4. `docs/DEPENDENCY_POLICY.md`：生产面 runtime crypto 目前**只有 hash-wasm**；
   引入任何新 runtime 依赖必须先更新该表 + `THIRD_PARTY_NOTICES` + 评审 bundle impact。

---

## 2. 被排除的路径（逐条带证据）

### 2.1 `cryptoFramework`（API 13 / 5.0.1.115）

读 `openharmony/ets/api/@ohos.security.cryptoFramework.d.ts`：

- 正则 `argon2` 命中：**0**
- KDF 派生树只有两条：`KdfSpec` → `PBKDF2Spec`、`HKDFSpec`
- 对称侧有 `GcmParamsSpec`，**AES-256-GCM 无同类问题**（后续 AES 步骤不受此 blocker 影响）

### 2.2 `HUKS`（API 13）

读 `@ohos.security.huks.d.ts`：

- 正则 `argon2` 命中：**0**
- 派生相关算法只有 `HUKS_ALG_HKDF`、`HUKS_ALG_PBKDF2`

### 2.3 `hash-wasm/src/argon2.c` —— 不能当原生源

该文件（6,351 B）头部写明 "Based on Golang's Argon2 implementation ... Written for hash-wasm"，
并使用 `__builtin_wasm_memory_grow` / `__builtin_wasm_memory_size` —— 它**只能**编译到 WebAssembly，
**无法**编译为 native `.so`。这是本次深挖中新排除的一条候选路径（基线审计未覆盖）。

### 2.4 SDK 不自带 Argon2 / OpenSSL / libsodium

递归检索 `openharmony/native`，匹配 `argon2|libsodium|libcrypto|openssl`：

- 仅命中 **CMake 自带的 `FindOpenSSL.cmake` 及其文档/HTML**，**无实际头文件与库**
- 无任何 argon2 / libsodium 产物

---

## 3. 可行路径：NDK + PHC 参考实现 + NAPI

### 3.1 工具链实测（本机）

| 能力 | 位置（相对 `<DEVECO_HOME>/sdk/default/openharmony`） | 结果 |
| --- | --- | --- |
| C 编译器 | `native/llvm/bin/clang.exe` | **存在**（clang 15.0.4） |
| C++ / 链接器 / 归档 | `clang++.exe`、`ld.lld.exe`、`llvm-ar.exe` | **存在** |
| sysroot | `native/sysroot/usr/{include,lib}` | **存在** |
| **Node-API 头** | `sysroot/usr/include/napi/native_api.h`、`sysroot/usr/include/node_api.h` | **存在** |
| CMake 工具链文件 | `native/build/cmake/ohos.toolchain.cmake` | **存在** |
| CMake 构建工具 | `native/build-tools/cmake`（3.28） | **存在** |
| 可用 ABI | `sysroot/usr/lib/{aarch64-linux-ohos, arm-linux-ohos, x86_64-linux-ohos}` | 三种；`OHOS_ARCH` 默认 **arm64-v8a** |

结论：**原生构建链路完整**，此前"NDK 下仅有 FindOpenSSL、无 openssl"的判断成立，
但"因此无法编译原生库"是**过度推论** —— 不需要 OpenSSL，需要的只是一个能编译 C 的交叉工具链，它就在 SDK 里。

### 3.2 源码选型与许可

| 项 | 取值 |
| --- | --- |
| 实现 | PHC `phc-winner-argon2` @ `20190702`（Argon2 官方参考实现） |
| 许可 | **CC0-1.0 OR Apache-2.0**（双许可，二选一） |
| 组成 | `argon2.c` `core.c` `encoding.c` `thread.c` `blake2/blake2b.c` + **`ref.c`** |
| 关键可移植性约束 | **arm64 必须用 `ref.c`，不能用 `opt.c`** —— `opt.c` 是 x86 SSE2 优化实现；漏掉 `fill_segment` 会直接链接失败（实测遇到并已定位） |

### 3.3 与"禁止自研原语"的关系

spec 禁止的是**自己实现密码学原语**。本路径是：

- KDF 运算：全部由 PHC 参考实现完成（未经任何修改）；
- 桥接层：只把 ArkTS 的 `pwd / salt / t / m / p` 编组成 C 参数，并把 32 字节结果转成 hex，
  **不含任何密码学运算**。

因此不违反该约束；但它**确实**是新的生产 runtime 依赖，必须走 §6 的登记与评审。

---

## 4. PoC（已执行，结果可复现）

> 第三方源码只下载到**仓库之外的临时工作目录 `<poc-workdir>`**，**未入库**；
> 两个 `.so` 也只存在于 `<poc-workdir>`，**均未提交**。入库属于 §6 的待办，不在本轮。

### 4.1 PoC-1：主机侧 Golden Vector —— **PASS**

输入（取自 `spec/security/depmap-container-v1.json` 冻结向量）：

```
password : "depmap-test"（exact UTF-8 bytes，无归一化）
salt     : 00112233445566778899aabbccddeeff（16 B）
params   : t=3, m=65536 KiB, p=1, dkLen=32 B, argon2id
```

输出：

```
ARGON2_VERSION=19 (spec requires 19 = 0x13)
VERSION_MATCH=YES
EXPECT=66c4bec7f5e98856747d7b41d0a021bdc092d5e12d492852bd80647bd0ff0c86
GOT   =66c4bec7f5e98856747d7b41d0a021bdc092d5e12d492852bd80647bd0ff0c86
MATCH=YES
```

意义：参考实现在**冻结参数**下**逐字节**复现规范值，`ARGON2_VERSION_13 = 19` 与规范的 `version: 19` 一致。
编译器：gcc 14.2.0（`-std=c89 -O3 -pthread`）。

### 4.2 PoC-2：OHOS arm64 交叉编译 —— **PASS（编译期）**

```
产物  : libargon2_ohos.so        37,400 B
ELF   : Class=ELF64  Data=little endian  Type=DYN  Machine=AArch64
NEEDED: 仅 libc.so
导出  : argon2_ctx / argon2_hash / argon2id_hash_raw 等 argon2_* 符号齐全
```

### 4.3 PoC-3：NAPI 桥接模块 —— **PASS（编译期）**

```
产物  : libpdiargon2.so          40,904 B
ELF   : Class=ELF64  Type=DYN  Machine=AArch64
NEEDED: 仅 libc.so
NAPI  : 8 个 napi_* 未定义符号（napi_create_function / napi_get_cb_info /
        napi_get_value_string_utf8 / napi_module_register ...）
        —— NAPI 模块常态：这些符号由 Ark 运行时在加载时解析
入口  : 模块注册宏已生效（napi_module_register 被引用），导出 argon2idDeriveRaw
```

### 4.4 PoC-4：设备上运行 —— **NOT_RUN**

`hdc list targets = [Empty]`，且无模拟器系统镜像（继承 `HARMONY_N3_BASELINE_AUDIT.md` §4）。
因此 `HARMONY_RUNTIME_E2E = RUNTIME_NOT_RUN` 保持不变 —— **编译成功 ≠ 设备上通过**。

---

## 5. 状态迁移（精确口径）

| 标识 | 迁移前 | 迁移后 |
| --- | --- | --- |
| `HARMONY_ARGON2_MANAGED_API` | UNKNOWN | **NOT_AVAILABLE**（证据级排除） |
| `HARMONY_ARGON2_NATIVE_TOOLCHAIN` | UNKNOWN | **AVAILABLE**（clang/sysroot/NAPI/CMake 齐备） |
| `HARMONY_ARGON2_HOST_GOLDEN` | UNKNOWN | **PASS**（PoC-1 逐字节复现） |
| `HARMONY_ARGON2_OHOS_BUILD` | UNKNOWN | **PASS（编译期）**（PoC-2/3，arm64 .so） |
| `HARMONY_ARGON2_ON_DEVICE` | NOT_RUN | **NOT_RUN**（无设备/镜像，外部闸门） |
| `HARMONY_DEPMAP` | BLOCKED | **BLOCKED_BY_NATIVE_VERIFICATION** |
| `HARMONY_RUNTIME_E2E` | RUNTIME_NOT_RUN | **RUNTIME_NOT_RUN**（不变） |

---

## 6. 落地前必须先完成的 Gate（**本轮未做**）

1. **依赖登记**：更新 `docs/DEPENDENCY_POLICY.md` 与 `THIRD_PARTY_NOTICES.md`，
   并明确采用 CC0-1.0 还是 Apache-2.0（两者都需履行相应 NOTICE 义务）。
2. **入库方式决策**：vendored（固化 commit/tag + 完整性校验）还是 ohpm 三方包；
   当前 PoC 源码在仓库外，**尚未入库**。
3. **构建集成**：`CMakeLists.txt` + `ohos.toolchain.cmake` 接入 hvigor；ABI 选择；
   符号可见性收敛（`-fvisibility=hidden`，只导出 NAPI 入口）。
4. **并行度**：PoC 用 `-DARGON2_NO_THREADS`；正式构建应启用 `thread.c`（OHOS 提供 pthread），
   并对 `p=1..4` 逐档复验与 Golden Vector 一致。
5. **内存与时延**：`m=65536 KiB`（64 MiB）在真机上的耗时/内存峰值必须实测，
   并与 `ANDROID_PERFORMANCE_SMOKE_REPORT.md` 的基线对照；
   **ArkTS 主线程不得执行 KDF**，必须放 worker / taskpool，否则会阻塞 UI。
6. **边界校验先于 KDF**（spec `bounds`）：`memoryKiB` 16–256 MiB、`iterations` 1–10、
   `parallelism` 1–4、`salt` 16 B、`nonce` 12 B、`tag` 16 B ——
   桥接层**不得**把越界参数直接交给原生层（恶意容器不得触发超大内存 KDF）。
7. **UTF-8 直通**：密码按 exact UTF-8 bytes 传入，**禁止 NFC/NFD 归一化**；
   已用 `napi_get_value_string_utf8`（按 UTF-8 长度取值，而非 UTF-16），
   仍需在设备上用规范里 5 个 UTF-8 向量（ascii / chinese / emoji / combining / nfc）复验。
8. 以上全部完成且**在设备上**用 Golden Vector + UTF-8 向量复验通过后，
   `HARMONY_DEPMAP` 才可从 `BLOCKED_BY_NATIVE_VERIFICATION` 改为 PASS。

---

## 7. 复现命令（路径已占位化）

```bash
# PoC-1 主机侧：编译 PHC 参考实现并跑 Golden Vector
gcc -std=c89 -O3 -Wall -Iinclude -Isrc -pthread \
    src/argon2.c src/core.c src/encoding.c src/thread.c src/blake2/blake2b.c src/opt.c \
    poc.c -o poc.exe
./poc.exe            # → MATCH=YES

# PoC-2 OHOS arm64 交叉编译（注意：arm64 用 ref.c，不要用 x86 SSE2 的 opt.c）
<DEVECO_HOME>/sdk/default/openharmony/native/llvm/bin/clang \
    --target=aarch64-linux-ohos \
    --sysroot=<DEVECO_HOME>/sdk/default/openharmony/native/sysroot \
    -fPIC -shared -O2 -std=c89 -Iinclude -Isrc -DARGON2_NO_THREADS \
    -o libargon2_ohos.so \
    src/argon2.c src/core.c src/encoding.c src/blake2/blake2b.c src/ref.c

# PoC-3 NAPI 桥接（同一批源 + bridge.c，napi 头来自 sysroot）
<...>/llvm/bin/clang --target=aarch64-linux-ohos --sysroot=<...>/sysroot \
    -fPIC -shared -O2 -std=c99 -Iinclude -Isrc -I<...>/sysroot/usr/include \
    -DARGON2_NO_THREADS -o libpdiargon2.so \
    src/argon2.c src/core.c src/encoding.c src/blake2/blake2b.c src/ref.c bridge.c

# 产物核查
<...>/llvm/bin/llvm-readelf -h libpdiargon2.so     # Machine: AArch64
<...>/llvm/bin/llvm-readelf -d libpdiargon2.so     # NEEDED 仅 libc.so
<...>/llvm/bin/llvm-nm -D -u libpdiargon2.so       # napi_* 由 Ark 运行时解析
```

> `<DEVECO_HOME>` / `<poc-workdir>` 为占位符：本机绝对路径不写入交付文档（隐私门禁）。
