#!/usr/bin/env python3
"""OHOS 交叉编译驱动器。

为什么需要这个 Python 中间层 —— 本机（Windows）实测的**宿主环境缺陷**：

    OHOS clang 15.0.4（DevEco 5.0.5.310 随附）在子进程环境下会以
    0xE06D7363 崩溃，栈里是第三方注入 DLL：
        D:\\...\\MToolBox_v2.0.2\\x64\\MToolExtend.dll
    即使只编译一个 hello.c 也照样崩 —— 与 vendored 源码无关。

    定位过程（见 tools/harmony/probe-clang-*.py）：

        父进程 = PowerShell 直接调用          -> 编译成功
        父进程 = node.exe                      -> CRASH
        父进程 = python（由 PowerShell 启动）  -> CRASH
        python + env={}                        -> 编译成功
        python + env={PATH}                    -> 编译成功
        python + env={SystemRoot}              -> CRASH   <== 触发因子
        python + env={PATH, SystemRoot}        -> CRASH

    结论：**SystemRoot 环境变量**的存在触发该注入 DLL 的崩溃路径。
    这解释了为什么 PowerShell 直接调用能成功（那里 clang 走的是另一条
    变量继承路径），而任何"把 SystemRoot 传下去"的派生方式都会崩。

规避策略：派生 clang 子进程时**不传 SystemRoot**（只保留 PATH 与 TEMP/TMP）。
    编译器、sysroot、优化级别、可见性 flags 全部不变，被绕开的只是
    一个与源码和工具链都无关的宿主变量。

用法：
    python ohos-clang-build.py <DEVECO_HOME> <WORKDIR> <ARGON2_ROOT> <BRIDGE_CPP>

输出：JSON（stdout），形如
    {"targets":[{"target":"arm64-v8a","build":"OK","soPath":"...","machine":"AArch64","bytes":123}, ...]}
"""

import json
import os
import subprocess
import sys


# 派生 clang 时使用的最小环境：**刻意不含 SystemRoot**（见文件头说明）。
def child_env():
    env = {}
    if os.environ.get("PATH"):
        env["PATH"] = os.environ["PATH"]
    else:
        env["PATH"] = r"C:\Windows\System32"
    for k in ("TEMP", "TMP"):
        if os.environ.get(k):
            env[k] = os.environ[k]
    return env


AR2_SOURCES = [
    "src/argon2.c",
    "src/core.c",
    "src/encoding.c",
    "src/thread.c",
    "src/blake2/blake2b.c",
    "src/ref.c",  # 必须是 ref.c：ref 可移植实现；opt.c 是 x86 SSE2，arm64 会链接失败
]

TARGETS = [
    {"name": "arm64-v8a", "triple": "aarch64-linux-ohos", "machine": "AArch64"},
    # llvm-readelf 把 x86_64 打印成 "Advanced Micro Devices X86-64"，不是 "X86-64"。
    {"name": "x86_64", "triple": "x86_64-linux-ohos", "machine": "Advanced Micro Devices X86-64"},
]


def run(args, timeout=900):
    p = subprocess.run(args, capture_output=True, timeout=timeout, env=child_env())
    if p.returncode != 0:
        raise RuntimeError(
            "command failed (rc=%d)\n%s\n%s"
            % (p.returncode, " ".join(args), p.stderr.decode("utf-8", "replace")[:6000])
        )
    return p


def main():
    deveco_home, workdir, argon2_root, bridge = sys.argv[1:5]

    ohos = os.path.join(deveco_home, "sdk", "default", "openharmony")
    clang = os.path.join(ohos, "native", "llvm", "bin", "clang.exe")
    clangxx = os.path.join(ohos, "native", "llvm", "bin", "clang++.exe")
    sysroot = os.path.join(ohos, "native", "sysroot")

    for p in (clang, clangxx, sysroot, argon2_root, bridge):
        if not os.path.exists(p):
            json.dump({"error": "missing: %s" % p}, sys.stdout)
            return 1

    os.makedirs(workdir, exist_ok=True)
    results = []

    for t in TARGETS:
        so_path = os.path.join(workdir, "libpdiargon2-%s.so" % t["name"])
        obj_dir = os.path.join(workdir, "obj-%s" % t["name"])
        os.makedirs(obj_dir, exist_ok=True)

        common = [
            "--target=%s" % t["triple"],
            "--sysroot=%s" % sysroot,
            "-fPIC",
            "-O2",
            # 只保留 -fvisibility=hidden，**刻意不加 -DA2_VISCTL=1**：
            #   上游 Makefile 用 "-fvisibility=hidden -DA2_VISCTL=1" 是为了让
            #   libargon2.so 把 argon2_* 作为**导出**符号暴露出去（A2_VISCTL 把
            #   ARGON2_PUBLIC 定义成 visibility("default")）。
            #   但本项目是把 libargon2 **静态链接进 NAPI 模块**，不需要、也不应该
            #   把 argon2_* 暴露给 ArkTS —— 那会无谓扩大 native 信任面。
            #   因此这里不定义 A2_VISCTL，让 ARGON2_PUBLIC 为空，
            #   再由 -fvisibility=hidden 统一隐藏，只在桥接层显式导出 NAPI 入口。
            "-fvisibility=hidden",
            "-Wall",
            "-Wextra",
            "-Wno-unused-parameter",
            "-I%s" % os.path.join(argon2_root, "include"),
            "-I%s" % os.path.join(argon2_root, "src"),
        ]

        try:
            objs = []
            # Argon2 是 C89/C99：必须用 clang 编译（clang++ 会拒绝 -std=c99）
            for src in AR2_SOURCES:
                obj = os.path.join(obj_dir, src.replace("/", "_") + ".o")
                run([clang] + common + ["-std=c99", "-c", os.path.join(argon2_root, src), "-o", obj])
                objs.append(obj)
            # 桥接是 C++（napi 宏 / EXTERN_C）。
            # 桥接对象单独用 -DA2_PDI_EXPORT 编译：让 NAPI 模块注册入口
            # 以 default visibility 导出（否则 -fvisibility=hidden 会把它藏起来，
            # ArkTS 侧 import 'libpdiargon2.so' 将找不到模块）。
            bridge_obj = os.path.join(obj_dir, "pdi_argon2.o")
            run([clangxx] + common + ["-std=c++17", "-DA2_PDI_EXPORT=1", "-c", bridge, "-o", bridge_obj])
            objs.append(bridge_obj)

            run(
                [clangxx, "--target=%s" % t["triple"], "--sysroot=%s" % sysroot,
                 "-shared", "-o", so_path] + objs + ["-lace_napi.z", "-lpthread", "-lm"]
            )
        except Exception as exc:  # noqa: BLE001 - 报告原始错误，不吞异常
            results.append({"target": t["name"], "build": "FAIL", "detail": str(exc)[:6000]})
            continue

        results.append({
            "target": t["name"],
            "build": "OK",
            "soPath": so_path,
            "machine": t["machine"],
            "bytes": os.path.getsize(so_path),
        })

    json.dump({"targets": results}, sys.stdout)
    return 0


if __name__ == "__main__":
    sys.exit(main())
