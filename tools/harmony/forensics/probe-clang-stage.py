#!/usr/bin/env python3
"""判定 OHOS clang 崩溃发生在哪个阶段，以及本机是否有可用的替代 C 编译器。

要回答的问题：
  1. clang 的预处理阶段（-E）是否正常？（正常 => 崩溃在 codegen/优化阶段）
  2. 换一个 driver（clang-cl / 直接调 cc1）是否可用？
  3. 本机是否还有别的可交叉编译 OHOS 的 clang？
"""

import glob
import os
import subprocess

DEVECO = r"D:\Code\Harmony\DevEco Studio"
LLVM_BIN = os.path.join(DEVECO, "sdk", "default", "openharmony", "native", "llvm", "bin")
CLANG = os.path.join(LLVM_BIN, "clang.exe")
HELLO = r"C:\Users\Kaiser\pdig-harmony-build\clang-minrepro\hello.c"
OUT = r"E:\AI\号卡管理\.probe-clang-out.txt"

lines = []


def run(label, args):
    try:
        p = subprocess.run(args, capture_output=True, timeout=180)
        err = p.stderr.decode("utf-8", "replace")
        crash = "crash backtrace" in err
        lines.append("%-34s rc=%-10d crash=%s out=%dB" % (label, p.returncode, crash, len(p.stdout)))
    except Exception as exc:  # noqa: BLE001
        lines.append("%-34s EXC %s" % (label, str(exc)[:120]))


# 1) 预处理阶段
run("-E only (preprocess)", [CLANG, "-E", "-x", "c", HELLO])
# 2) 直接调前端 cc1（跳过 driver）
cc1 = os.path.join(LLVM_BIN, "clang.exe")
run("-cc1 direct", [cc1, "-cc1", "-emit-obj", "-o", os.path.join(os.environ["TEMP"], "cc1.o"), HELLO])
# 3) -S 只生成汇编
run("-S (emit asm)", [CLANG, "-S", "-o", os.path.join(os.environ["TEMP"], "h.s"), HELLO])
# 4) clang-cl 前端
clangcl = os.path.join(LLVM_BIN, "clang-cl.exe")
if os.path.exists(clangcl):
    run("clang-cl", [clangcl, "/c", HELLO, "/Fo" + os.path.join(os.environ["TEMP"], "h.obj")])
else:
    lines.append("%-34s NOT_PRESENT" % "clang-cl")

# 5) 本机其它 OHOS 工具链
lines.append("--- other llvm/clang installs on this machine ---")
patterns = [
    r"D:\Code\Harmony\**\clang.exe",
    r"C:\Program Files\**\clang.exe",
    r"D:\**\ndk\**\clang.exe",
]
found = []
for pat in patterns:
    try:
        found.extend(glob.glob(pat, recursive=True)[:5])
    except Exception:  # noqa: BLE001
        pass
for f in sorted(set(found))[:20]:
    lines.append("  " + f)
if not found:
    lines.append("  (none found via glob)")

with open(OUT, "w", encoding="utf-8") as fh:
    fh.write("\n".join(lines) + "\n")
print("\n".join(lines))
