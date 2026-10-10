#!/usr/bin/env python3
"""
PDIG Production VNext secure-rehearsal proof.

Runs against a productionDebug APK on an API36 emulator and proves:
1. vnext_production enters the SAME fail-closed lock before any Reality UI appears;
2. explicit no-credential acknowledgement is required on the clean CI emulator;
3. after unlock, the real-Reality Production VNext shell appears with all five
   primary destinations;
4. background -> foreground relocks before Reality is visible again;
5. process restart relocks;
6. default productionDebug (without extra) remains the legacy production shell.

This script is evidence, not self-approval. It writes XML snapshots and a manifest
for later human/audit review.
"""

from __future__ import annotations

import json
import os
import subprocess
import time
import xml.etree.ElementTree as ET
from pathlib import Path

PACKAGE = "com.pdig.app"
ACTIVITY = "com.pdig.app.MainActivity"
OUT = Path("artifacts/runtime-evidence/production-vnext-rehearsal")
OUT.mkdir(parents=True, exist_ok=True)


def adb(*args: str, check: bool = True) -> subprocess.CompletedProcess[str]:
    return subprocess.run(
        ["adb", *args],
        check=check,
        text=True,
        stdout=subprocess.PIPE,
        stderr=subprocess.STDOUT,
    )


def dump(name: str) -> list[dict[str, str]]:
    remote = "/sdcard/window.xml"
    adb("shell", "uiautomator", "dump", remote)
    raw = adb("shell", "cat", remote).stdout
    (OUT / f"{name}.xml").write_text(raw, encoding="utf-8")
    root = ET.fromstring(raw)
    return [dict(node.attrib) for node in root.iter("node")]


def label(node: dict[str, str]) -> str:
    return (node.get("text") or node.get("content-desc") or "").strip()


def labels(nodes: list[dict[str, str]]) -> list[str]:
    return [x for n in nodes if (x := label(n))]


def has(nodes: list[dict[str, str]], text: str) -> bool:
    return any(text in label(n) for n in nodes)


def wait_for(text: str, timeout: float = 20.0, snapshot: str = "wait") -> list[dict[str, str]]:
    deadline = time.time() + timeout
    last: list[dict[str, str]] = []
    while time.time() < deadline:
        try:
            last = dump(snapshot)
            if has(last, text):
                return last
        except Exception:
            pass
        time.sleep(0.8)
    raise RuntimeError(f"Timed out waiting for {text!r}; last labels={labels(last)[:80]}")


def bounds_center(raw: str) -> tuple[int, int]:
    # Android bounds form: [x1,y1][x2,y2]
    import re
    m = re.fullmatch(r"\[(\d+),(\d+)\]\[(\d+),(\d+)\]", raw)
    if not m:
        raise ValueError(f"bad bounds: {raw}")
    x1, y1, x2, y2 = map(int, m.groups())
    return ((x1 + x2) // 2, (y1 + y2) // 2)


def tap_text(text: str, timeout: float = 15.0) -> None:
    deadline = time.time() + timeout
    while time.time() < deadline:
        nodes = dump("tap-probe")
        for n in nodes:
            if text in label(n):
                x, y = bounds_center(n["bounds"])
                adb("shell", "input", "tap", str(x), str(y))
                time.sleep(0.8)
                return
        time.sleep(0.6)
    raise RuntimeError(f"Could not tap text {text!r}")


def assert_locked(name: str) -> list[dict[str, str]]:
    nodes = wait_for("PDIG 已锁定", snapshot=name)
    forbidden = ["生产 Reality", "个人控制面", "建立基础设施"]
    leaked = [x for x in forbidden if has(nodes, x)]
    if leaked:
        raise RuntimeError(f"Reality/VNext UI leaked while locked: {leaked}")
    return nodes


def unlock_clean_emulator() -> list[dict[str, str]]:
    nodes = dump("unlock-capability")
    if has(nodes, "已知悉风险，本次进入"):
        tap_text("已知悉风险，本次进入")
    elif has(nodes, "验证身份并解锁"):
        raise RuntimeError(
            "CI emulator unexpectedly requires system credentials; "
            "this rehearsal expects the clean no-credential profile."
        )
    else:
        raise RuntimeError(f"No recognized unlock action; labels={labels(nodes)[:80]}")
    return wait_for("生产 Reality", timeout=25, snapshot="production-vnext-unlocked")


def assert_five_primary(nodes: list[dict[str, str]]) -> None:
    missing = []
    for item in ("现在", "基础设施", "变更", "记录", "我"):
        if not any(label(n) == item for n in nodes):
            missing.append(item)
    if missing:
        raise RuntimeError(f"Production VNext missing primary destinations: {missing}")


def start(extra: bool) -> None:
    adb("shell", "am", "force-stop", PACKAGE)
    args = [
        "shell", "am", "start", "-W",
        "-n", f"{PACKAGE}/{ACTIVITY}",
    ]
    if extra:
        args += ["--ez", "vnext_production", "true"]
    adb(*args)
    time.sleep(1.5)


def main() -> None:
    source_sha = os.environ.get("GITHUB_SHA", "")
    evidence: dict[str, object] = {
        "sourceSha": source_sha,
        "package": PACKAGE,
        "activity": ACTIVITY,
        "result": "PENDING",
        "checks": [],
    }

    # 1. Explicit real-Reality rehearsal must fail closed.
    start(extra=True)
    assert_locked("01-reality-debug-locked")
    evidence["checks"].append("REALITY_DEBUG_FAIL_CLOSED")

    # 2. Clean emulator explicit acknowledgement -> real Production VNext.
    unlocked = unlock_clean_emulator()
    assert_five_primary(unlocked)
    if not has(unlocked, "生产 Reality"):
        raise RuntimeError("Production truth badge missing after unlock")
    dump("02-reality-debug-unlocked")
    evidence["checks"].append("REALITY_DEBUG_FIVE_PRIMARY")

    # 3. Backgrounding must relock before content can return.
    adb("shell", "input", "keyevent", "KEYCODE_HOME")
    time.sleep(1.5)
    adb("shell", "am", "start", "-W", "-n", f"{PACKAGE}/{ACTIVITY}")
    assert_locked("03-background-relocked")
    evidence["checks"].append("BACKGROUND_RELOCK")

    # Unlock again and prove the same Production VNext target survives resume.
    resumed = unlock_clean_emulator()
    assert_five_primary(resumed)
    dump("04-resume-production-vnext")
    evidence["checks"].append("RESUME_RETURNS_PRODUCTION_VNEXT")

    # 4. Process restart with rehearsal extra also starts locked.
    start(extra=True)
    assert_locked("05-process-restart-locked")
    evidence["checks"].append("PROCESS_RESTART_RELOCK")

    # 5. Default productionDebug remains legacy, proving rehearsal is opt-in.
    start(extra=False)
    assert_locked("06-default-production-locked")
    nodes = dump("06-default-production-locked")
    if has(nodes, "生产 Reality"):
        raise RuntimeError("Default production route exposed Production VNext before unlock")
    # Unlock and prove legacy content, not Production VNext.
    current = dump("06-default-unlock-capability")
    if has(current, "已知悉风险，本次进入"):
        tap_text("已知悉风险，本次进入")
    else:
        raise RuntimeError("Default production clean emulator did not expose explicit acknowledgement")
    legacy = wait_for("我的基础设施", timeout=25, snapshot="07-default-production-legacy")
    if has(legacy, "生产 Reality"):
        raise RuntimeError("Default productionDebug silently cut over to Production VNext")
    evidence["checks"].append("DEFAULT_PRODUCTION_REMAINS_LEGACY")

    evidence["result"] = "PASS"
    (OUT / "manifest.json").write_text(
        json.dumps(evidence, ensure_ascii=False, indent=2),
        encoding="utf-8",
    )
    print("PRODUCTION_VNEXT_SECURE_REHEARSAL=PASS", flush=True)


if __name__ == "__main__":
    main()
