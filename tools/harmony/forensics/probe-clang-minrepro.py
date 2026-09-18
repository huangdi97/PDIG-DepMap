#!/usr/bin/env python3
"""最小复现：OHOS clang 编译 vendored argon2 是否真的可行？

把所有外部变量剥掉：
  - 不用 --target / --sysroot（纯本机目标，排除 sysroot 因素）
  - 只编译 argon2.c，不做链接
  - 逐个加法：先空文件，再 argon2.c，再加 -DA2_VISCTL=1，再加 -fvisibility=hidden
这样可以把"崩溃"归因到具体是哪个输入引起的。
"""

import os
import subprocess

DEVECO = r"D:\Code\Harmony\DevEco Studio"
CLANG = os.path.join(DEVECO, "sdk", "default", "openharmony", "native", "llvm", "bin", "clang.exe")
ARGON2 = r"E:\AI\号卡管理\third_party\argon2"
WORK = r"C:\Users\Kaiser\pdig-harmony-build\clang-minrepro"
OUT = r"E:\AI\号卡管理\.probe-clang-out.txt"
os.makedirs(WORK, exist_ok=True)

hello = os.path.join(WORK, "hello.c")
with open(hello, "w", encoding="utf-8") as fh:
    fh.write("int f(int x){return x+1;}\n")

cases = [
    ("hello.c, no flags",            [hello]),
    ("hello.c, -O2",                 ["-O2", hello]),
    ("hello.c, --target ohos",       ["--target=aarch64-linux-ohos", hello]),
    ("argon2.c, no flags",           [os.path.join(ARGON2, "src", "argon2.c")]),
    ("argon2.c, -DA2_VISCTL=1",      ["-DA2_VISCTL=1", os.path.join(ARGON2, "src", "argon2.c")]),
    ("argon2.c, -fvisibility=hidden",["-fvisibility=hidden", os.path.join(ARGON2, "src", "argon2.c")]),
    ("argon2.c, -I include",         ["-I" + os.path.join(ARGON2, "include"), os.path.join(ARGON2, "src", "argon2.c")]),
]

lines = []
for i, (label, extra) in enumerate(cases):
    out = os.path.join(WORK, "out%d.o" % i)
    p = subprocess.run([CLANG] + extra + ["-c", "-o", out], capture_output=True, timeout=300)
    if p.returncode == 0:
        lines.append("OK     %-32s" % label)
    else:
        err = p.stderr.decode("utf-8", "replace")
        head = [l for l in err.split("\n") if l.strip()][:2]
        lines.append("FAIL   %-32s rc=%-9d %s" % (label, p.returncode, " | ".join(head)[:150]))

with open(OUT, "w", encoding="utf-8") as fh:
    fh.write("\n".join(lines) + "\n")
print("\n".join(lines))
