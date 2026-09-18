#!/usr/bin/env python3
"""寻找可用的 OHOS 交叉编译路径。

已知事实：
    clang -E                 OK
    clang -S (emit asm)      OK
    clang -cc1 -emit-obj     OK
    clang -c                 CRASH  <- 只有走 driver 的默认 codegen 路径才崩
    clang-cl /c              OK

因此候选路径：
    A. clang -S  ->  llvm-mc / clang -cc1 -emit-obj  ->  llvm-ar / ld.lld（完全绕开 -c）
    B. clang-cl（MSVC 风格 driver）
    C. BiSheng clang（另一套工具链）

本脚本验证 A/B/C 三条路径能否为 arm64-v8a 与 x86_64 产出目标文件。
"""

import os
import subprocess

DEVECO = r"D:\Code\Harmony\DevEco Studio"
OHOS = os.path.join(DEVECO, "sdk", "default", "openharmony")
LLVM_BIN = os.path.join(OHOS, "native", "llvm", "bin")
CLANG = os.path.join(LLVM_BIN, "clang.exe")
CLANGCL = os.path.join(LLVM_BIN, "clang-cl.exe")
BISHENG = os.path.join(DEVECO, "sdk", "default", "hms", "native", "BiSheng", "bin", "clang.exe")
SYSROOT = os.path.join(OHOS, "native", "sysroot")
ARGON2 = r"E:\AI\号卡管理\third_party\argon2"
WORK = r"C:\Users\Kaiser\pdig-harmony-build\clang-pathfind"
OUT = r"E:\AI\号卡管理\.probe-clang-out.txt"
os.makedirs(WORK, exist_ok=True)

lines = []


def try_run(label, args):
    try:
        p = subprocess.run(args, capture_output=True, timeout=300)
        err = p.stderr.decode("utf-8", "replace")
        crash = "crash backtrace" in err
        ok = p.returncode == 0
        lines.append("%-52s %s %s" % (label, "OK  " if ok else "FAIL", ("crash" if crash else err.split("\n")[0][:90])))
        return ok
    except Exception as exc:  # noqa: BLE001
        lines.append("%-52s EXC %s" % (label, str(exc)[:100]))
        return False


triples = [("arm64", "aarch64-linux-ohos"), ("x86_64", "x86_64-linux-ohos")]

for name, triple in triples:
    inc = ["-I" + os.path.join(ARGON2, "include"), "-I" + os.path.join(ARGON2, "src")]
    sysr = ["--sysroot=" + SYSROOT]

    # A) -S -> -cc1 -emit-obj
    asm = os.path.join(WORK, "argon2-%s.s" % name)
    obj = os.path.join(WORK, "argon2-%s.o" % name)
    a_ok = try_run("A1 %s: clang -S (addon from driver)" % name,
                   [CLANG, "--target=" + triple] + sysr + ["-fPIC", "-O2", "-fvisibility=hidden",
                    "-DA2_VISCTL=1"] + inc + ["-std=c99", "-S", os.path.join(ARGON2, "src", "argon2.c"),
                    "-o", asm])
    if a_ok:
        try_run("A2 %s: clang -cc1 -emit-obj" % name,
                [CLANG, "-cc1", "-triple", triple, "-isysroot", SYSROOT,
                 "-fPIC", "-O2", "-fvisibility=hidden", "-DA2_VISCTL=1",
                 "-I" + os.path.join(ARGON2, "include"), "-I" + os.path.join(ARGON2, "src"),
                 "-std=c99", "-emit-obj", asm, "-o", obj])

    # B) clang-cl
    try_run("B  %s: clang-cl /c" % name,
            [CLANGCL, "/c", "/clang:--target=" + triple, "/clang:--sysroot=" + SYSROOT,
             "/clang:-fPIC", "/clang:-fvisibility=hidden", "/clang:-DA2_VISCTL=1",
             "/clang:-I" + os.path.join(ARGON2, "include"), "/clang:-I" + os.path.join(ARGON2, "src"),
             os.path.join(ARGON2, "src", "argon2.c"), "/Fo" + os.path.join(WORK, "cl-%s.obj" % name)])

    # C) BiSheng clang
    if os.path.exists(BISHENG):
        try_run("C  %s: BiSheng clang -c" % name,
                [BISHENG, "--target=" + triple] + sysr + ["-fPIC", "-O2", "-fvisibility=hidden",
                 "-DA2_VISCTL=1"] + inc + ["-std=c99", "-c",
                 os.path.join(ARGON2, "src", "argon2.c"), "-o", os.path.join(WORK, "bs-%s.o" % name)])
    else:
        lines.append("%-52s NOT_PRESENT" % ("C  %s: BiSheng clang" % name))

# 也测一下 BiSheng 的普通 hello
hello = os.path.join(WORK, "hello.c")
with open(hello, "w", encoding="utf-8") as fh:
    fh.write("int f(int x){return x+1;}\n")
if os.path.exists(BISHENG):
    try_run("C  hello: BiSheng clang -c", [BISHENG, "-c", hello, "-o", os.path.join(WORK, "h-bs.o")])

with open(OUT, "w", encoding="utf-8") as fh:
    fh.write("\n".join(lines) + "\n")
print("\n".join(lines))
