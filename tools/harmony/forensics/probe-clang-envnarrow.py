#!/usr/bin/env python3
"""验证"净化环境 + 保留必要变量"能否稳定编译。

已确认：
    env={}                -> 编译成功
    env=inherit(full)     -> 崩溃

因此策略：只传一组**白名单**变量（PATH / SystemRoot / TEMP / TMP），
并逐一验证 vendored argon2 的每个源文件在两个 ABI 下都能编译。

⚠ 但实测 PATH+SystemRoot 也会崩 —— 说明触发因子在 PATH 或 SystemRoot 里。
   本脚本继续做窄化：只用 PATH、只用 SystemRoot、两者都不给。
"""

import os
import subprocess

DEVECO = r"D:\Code\Harmony\DevEco Studio"
OHOS = os.path.join(DEVECO, "sdk", "default", "openharmony")
CLANG = os.path.join(OHOS, "native", "llvm", "bin", "clang.exe")
HELLO = r"C:\Users\Kaiser\pdig-harmony-build\clang-minrepro\hello.c"
OBJ = r"C:\Users\Kaiser\pdig-harmony-build\clang-pathfind\narrow.o"
OUT = r"E:\AI\号卡管理\bisect-out.txt"

ARGS = [CLANG, "-O2", "-c", HELLO, "-o", OBJ]


def crash(env):
    try:
        p = subprocess.run(ARGS, capture_output=True, timeout=120, env=env)
    except Exception:  # noqa: BLE001
        return True
    return p.returncode != 0


cases = [
    ("env={} (nothing)", {}),
    ("PATH only", {"PATH": os.environ.get("PATH", "")}),
    ("SystemRoot only", {"SystemRoot": os.environ.get("SystemRoot", r"C:\Windows")}),
    ("TEMP only", {"TEMP": os.environ.get("TEMP", "")}),
    ("PATH(system32 only)", {"PATH": r"C:\Windows\System32"}),
    ("PATH+SystemRoot+TEMP", {
        "PATH": os.environ.get("PATH", ""),
        "SystemRoot": os.environ.get("SystemRoot", r"C:\Windows"),
        "TEMP": os.environ.get("TEMP", ""),
    }),
]

lines = []
for label, env in cases:
    lines.append("%-24s -> crash=%s" % (label, crash(env)))

with open(OUT, "w", encoding="utf-8") as fh:
    fh.write("\n".join(lines) + "\n")
print("written", OUT)
