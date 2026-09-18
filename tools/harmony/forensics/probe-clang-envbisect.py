#!/usr/bin/env python3
"""二分定位：哪个环境变量导致 OHOS clang 崩溃。

已知事实（实测）：
    env={}                       -> rc=0    编译成功
    env=inherit                  -> crash
    env=PATH+SystemRoot          -> crash
    env 为空但只加 SystemRoot?   -> 待测

因此崩溃由一个（或几个）**环境变量**触发。用二分法找出它。
这是纯粹的宿主环境缺陷，与 vendored argon2 源码无关。
"""

import os
import subprocess

DEVECO = r"D:\Code\Harmony\DevEco Studio"
OHOS = os.path.join(DEVECO, "sdk", "default", "openharmony")
CLANG = os.path.join(OHOS, "native", "llvm", "bin", "clang.exe")
HELLO = r"C:\Users\Kaiser\pdig-harmony-build\clang-minrepro\hello.c"
OBJ = r"C:\Users\Kaiser\pdig-harmony-build\clang-pathfind\env.o"
OUT = r"E:\AI\号卡管理\.probe-clang-out.txt"

ARGS = [CLANG, "-O2", "-c", HELLO, "-o", OBJ]


def crashes(env):
    p = subprocess.run(ARGS, capture_output=True, timeout=120, env=env)
    return p.returncode != 0


lines = []
full = dict(os.environ)
lines.append("baseline inherit: crash=%s" % crashes(full))

# 二分：找出一个"最小致崩集合"
items = sorted(full.items())


def minimal_crashing_set(candidates):
    """返回极小的致崩子集（贪心剔除）。"""
    current = dict(candidates)
    changed = True
    while changed:
        changed = False
        for k in list(current.keys()):
            trial = dict(current)
            del trial[k]
            if len(trial) == 0:
                continue
            if not crashes(trial):
                # 删掉 k 后不再崩 -> k 必是必需因子之一
                continue
            current = trial
            changed = True
    return current


# 先确认全量必崩
if not crashes(full):
    lines.append("full env does not crash; aborting bisect")
else:
    minimal = minimal_crashing_set(items)
    lines.append("--- minimal crashing env (%d vars) ---" % len(minimal))
    for k, v in sorted(minimal.items()):
        lines.append("  %s=%s" % (k, v[:200]))

    # 对每个候选再单独验证
    lines.append("--- per-variable check (each var alone with nothing else) ---")
    for k in sorted(minimal.keys()):
        single = {k: minimal[k]}
        lines.append("  %-28s alone -> crash=%s" % (k, crashes(single)))

    # 反过来：只保留全量 env 去掉 minimal 里的某个，看是否恢复
    lines.append("--- full env minus each candidate ---")
    for k in sorted(minimal.keys()):
        trial = dict(full)
        trial.pop(k, None)
        lines.append("  full - %-24s -> crash=%s" % (k, crashes(trial)))

with open(OUT, "w", encoding="utf-8") as fh:
    fh.write("\n".join(lines) + "\n")
print("\n".join(lines))
