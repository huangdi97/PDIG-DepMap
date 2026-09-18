#!/usr/bin/env python3
"""在 Python 子进程链下逐一测试优化级别，确认崩溃是否与 -O 相关。

由 PowerShell（非 node）启动，因此父进程链干净。
"""

import os
import subprocess

DEVECO = r"D:\Code\Harmony\DevEco Studio"
OHOS = os.path.join(DEVECO, "sdk", "default", "openharmony")
CLANG = os.path.join(OHOS, "native", "llvm", "bin", "clang.exe")
SYSROOT = os.path.join(OHOS, "native", "sysroot")
ARGON2 = r"E:\AI\号卡管理\third_party\argon2"
WORK = r"C:\Users\Kaiser\pdig-harmony-build\clang-probe4"
OUT = r"E:\AI\号卡管理\.probe-clang-out.txt"
os.makedirs(WORK, exist_ok=True)

BASE = [
    CLANG, "--target=aarch64-linux-ohos", "--sysroot=" + SYSROOT,
    "-fPIC", "-fvisibility=hidden", "-DA2_VISCTL=1",
    "-I" + os.path.join(ARGON2, "include"), "-I" + os.path.join(ARGON2, "src"),
    "-std=c99", "-c", os.path.join(ARGON2, "src", "argon2.c"),
]

lines = []
for label, opt in [("no -O", []), ("-O0", ["-O0"]), ("-O1", ["-O1"]),
                   ("-O2", ["-O2"]), ("-O3", ["-O3"]), ("-Os", ["-Os"]), ("-Oz", ["-Oz"])]:
    out = os.path.join(WORK, "a-%s.o" % label.replace(" ", "").replace("-", ""))
    p = subprocess.run(BASE + opt + ["-o", out], capture_output=True, timeout=300)
    if p.returncode == 0:
        lines.append("OK     %-8s (%d bytes)" % (label, os.path.getsize(out)))
    else:
        err = p.stderr.decode("utf-8", "replace")
        lines.append("FAIL   %-8s rc=%d crash=%s" % (label, p.returncode, "crash backtrace" in err))

with open(OUT, "w", encoding="utf-8") as fh:
    fh.write("\n".join(lines) + "\n")
print("\n".join(lines))
