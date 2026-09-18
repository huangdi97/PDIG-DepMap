#!/usr/bin/env python3
"""定位致崩环境变量（修正输出路径）。

关键已确认事实：
    env={}                -> 编译成功 (rc=0)
    全量环境 / PATH 单独  -> 崩溃 (0xE06D7363, LLVM crash backtrace)

本脚本贪心求一个极小的致崩变量集合，并逐变量验证。
"""

import os
import subprocess
import sys

DEVECO = r"D:\Code\Harmony\DevEco Studio"
OHOS = os.path.join(DEVECO, "sdk", "default", "openharmony")
CLANG = os.path.join(OHOS, "native", "llvm", "bin", "clang.exe")
HELLO = r"C:\Users\Kaiser\pdig-harmony-build\clang-minrepro\hello.c"
OBJ = r"C:\Users\Kaiser\pdig-harmony-build\clang-pathfind\env2.o"
OUT = sys.argv[1] if len(sys.argv) > 1 else r"E:\AI\号卡管理\bisect-result.txt"

ARGS = [CLANG, "-O2", "-c", HELLO, "-o", OBJ]


def crashes(env):
    try:
        p = subprocess.run(ARGS, capture_output=True, timeout=120, env=env)
    except Exception:  # noqa: BLE001
        return True
    return p.returncode != 0


lines = []
full = dict(os.environ)
lines.append("A. inherit full env            -> crash=%s" % crashes(full))

# 贪心最小化
current = dict(full)
for k in sorted(full.keys()):
    if k not in current:
        continue
    trial = dict(current)
    del trial[k]
    if crashes(trial):
        current = trial
    # 否则 k 是必需因子，保留
lines.append("B. minimal crashing set size=%d" % len(current))
for k, v in sorted(current.items()):
    lines.append("   KEEP %s=%s" % (k, v[:160]))

# 逐变量：全量环境中删掉每个 KEEP，看是否恢复
lines.append("C. full env minus each minimal member (restored => that var is required)")
for k in sorted(current.keys()):
    trial = dict(full)
    trial.pop(k, None)
    lines.append("   full-%s -> crash=%s" % (k, crashes(trial)))

# 单独：只放最小集合
lines.append("D. only the minimal set -> crash=%s" % crashes(dict(current)))

with open(OUT, "w", encoding="utf-8") as fh:
    fh.write("\n".join(lines) + "\n")
print("written", OUT)
