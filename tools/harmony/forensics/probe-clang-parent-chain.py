#!/usr/bin/env python3
"""找出能在本机成功调用 OHOS clang 的父进程链。

已知：
    PowerShell -> clang            成功
    Python     -> clang            成功（但仅当 python 由 PowerShell 启动时）
    Node       -> clang            崩溃
    Node       -> Python -> clang  崩溃（注入 DLL 沿进程链继承）

本脚本由 PowerShell 直接启动，逐一尝试若干"洗掉注入 DLL"的手段，
把结论写入文件，供 build gate 选择正确策略。
"""

import json
import os
import subprocess
import sys

DEVECO = r"D:\Code\Harmony\DevEco Studio"
OHOS = os.path.join(DEVECO, "sdk", "default", "openharmony")
CLANG = os.path.join(OHOS, "native", "llvm", "bin", "clang.exe")
SYSROOT = os.path.join(OHOS, "native", "sysroot")
ARGON2 = r"E:\AI\号卡管理\third_party\argon2"
WORK = r"C:\Users\Kaiser\pdig-harmony-build\clang-probe3"
OUT = r"E:\AI\号卡管理\.probe-clang-out.txt"

os.makedirs(WORK, exist_ok=True)

ARGS = [
    CLANG,
    "--target=aarch64-linux-ohos",
    "--sysroot=" + SYSROOT,
    "-fPIC", "-O2",
    "-fvisibility=hidden", "-DA2_VISCTL=1",
    "-I" + os.path.join(ARGON2, "include"),
    "-I" + os.path.join(ARGON2, "src"),
    "-std=c99",
    "-c", os.path.join(ARGON2, "src", "argon2.c"),
    "-o", os.path.join(WORK, "a.o"),
]

lines = []


def attempt(label, env=None, exe=None, args=None):
    try:
        p = subprocess.run(args or ARGS, capture_output=True, timeout=300, env=env, executable=exe)
        if p.returncode == 0:
            lines.append("OK    " + label)
            return True
        err = p.stderr.decode("utf-8", "replace")
        kind = "CRASH" if "crash backtrace" in err else "rc=%d" % p.returncode
        lines.append("%-6s %s :: %s" % (kind, label, err.split("\n")[0][:120]))
    except Exception as exc:  # noqa: BLE001
        lines.append("EXC    %s :: %s" % (label, str(exc)[:160]))
    return False


# 1) 基线：直接调用
attempt("baseline direct subprocess")

# 2) 极简环境（去掉一切可能触发注入的变量）
minimal = {
    "PATH": os.path.dirname(CLANG) + os.pathsep + r"C:\Windows\System32",
    "SystemRoot": r"C:\Windows",
    "TEMP": os.environ.get("TEMP", r"C:\Windows\Temp"),
    "TMP": os.environ.get("TMP", r"C:\Windows\Temp"),
}
attempt("minimal env", env=minimal)

# 3) 经 cmd /c 二次派生
cmdline = " ".join('"%s"' % a if " " in a else a for a in ARGS)
attempt("via cmd /c", args=["cmd.exe", "/c", cmdline])

# 4) 经 cmd /c + 极简环境
attempt("via cmd /c + minimal env", env=minimal, args=["cmd.exe", "/c", cmdline])

# 5) 显式关掉 AppInit_DLLs 注入（需要注册表，通常没权限，仅试探）
attempt("with empty AppInit-ish env", env=dict(minimal, APPDATA="", LOCALAPPDATA=""))

with open(OUT, "w", encoding="utf-8") as fh:
    fh.write("\n".join(lines) + "\n")
print("written:", OUT)
