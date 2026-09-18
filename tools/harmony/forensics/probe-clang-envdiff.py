#!/usr/bin/env python3
"""对比"PowerShell 直接调用"与"Python 子进程调用"的环境差异，定位崩溃触发因子。

已知：
    PowerShell -> clang             成功（实测，多次）
    PowerShell -> python -> clang   崩溃

差异候选：
    - 环境变量集合（尤其 PATH 顺序、未知注入变量）
    - stdin/stdout 是否连接到控制台
    - 进程组 / 作业对象
    - 当前工作目录

本脚本把 python 进程的完整环境与 PowerShell 的对照，并尝试在子进程里
清空额外变量后重试。
"""

import os
import subprocess

DEVECO = r"D:\Code\Harmony\DevEco Studio"
OHOS = os.path.join(DEVECO, "sdk", "default", "openharmony")
CLANG = os.path.join(OHOS, "native", "llvm", "bin", "clang.exe")
SYSROOT = os.path.join(OHOS, "native", "sysroot")
HELLO = r"C:\Users\Kaiser\pdig-harmony-build\clang-minrepro\hello.c"
OUT = r"E:\AI\号卡管理\.probe-clang-out.txt"

lines = []
ARGS = [CLANG, "-c", HELLO, "-o", r"C:\Users\Kaiser\pdig-harmony-build\clang-pathfind\h.o"]

# 1) 继承环境
p = subprocess.run(ARGS, capture_output=True, timeout=120)
lines.append("inherit env                       rc=%-9d crash=%s" % (p.returncode, "crash backtrace" in p.stderr.decode("utf-8", "replace")))

# 2) 只留 PATH
minimal = {"PATH": os.environ.get("PATH", ""), "SystemRoot": r"C:\Windows"}
p = subprocess.run(ARGS, capture_output=True, timeout=120, env=minimal)
lines.append("PATH+SystemRoot only              rc=%-9d crash=%s" % (p.returncode, "crash backtrace" in p.stderr.decode("utf-8", "replace")))

# 3) 完全空环境
p = subprocess.run(ARGS, capture_output=True, timeout=120, env={})
lines.append("empty env                         rc=%-9d crash=%s" % (p.returncode, "crash backtrace" in p.stderr.decode("utf-8", "replace")))

# 4) 用子进程链接控制台
p = subprocess.run(ARGS, capture_output=True, timeout=120, creationflags=0x00000010)  # CREATE_NEW_CONSOLE
lines.append("CREATE_NEW_CONSOLE                rc=%-9d crash=%s" % (p.returncode, "crash backtrace" in p.stderr.decode("utf-8", "replace")))

# 5) 分离进程组
p = subprocess.run(ARGS, capture_output=True, timeout=120, creationflags=0x00000200)  # CREATE_NEW_PROCESS_GROUP
lines.append("CREATE_NEW_PROCESS_GROUP          rc=%-9d crash=%s" % (p.returncode, "crash backtrace" in p.stderr.decode("utf-8", "replace")))

# 6) 列出本 python 进程环境里所有可能被注入的变量
lines.append("--- env var names in this python process ---")
names = sorted(os.environ.keys())
lines.append("count=%d" % len(names))
interesting = [n for n in names if not n.startswith(("ProgramFiles", "CommonProgram", "PROCESSOR", "NUMBER_OF", "OS", "USER", "SystemDrive", "windir", "COMPUTERNAME", "PATHEXT", "ComSpec", "TEMP", "TMP", "HOMEDRIVE", "HOMEPATH", "USERPROFILE", "ALLUSERSPROFILE", "PUBLIC", "APPDATA", "LOCALAPPDATA", "OneDrive", "SESSIONNAME"))]
for n in interesting:
    v = os.environ[n]
    lines.append("  %s=%s" % (n, v[:150]))

with open(OUT, "w", encoding="utf-8") as fh:
    fh.write("\n".join(lines) + "\n")
print("\n".join(lines[:6]))
