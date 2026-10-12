#!/usr/bin/env python3
"""
PDIG Production VNext release-default rehearsal.

This is NOT a release approval. It runs a productionDebug APK compiled with:
  -PpdigProductionUiGeneration=vnext
  -PpdigProductionVNextCutoverApproved=true

and proves that the *default launcher path*, with no Intent extras:
1. fails closed behind the canonical lock;
2. unlocks into real-Reality Production VNext;
3. exposes all five primary destinations;
4. relocks on background/foreground;
5. relocks on process restart.

The exact same MainActivity / ProductionVNextSecureHost path is used by the future
release target. Synthetic Preview data must never appear in this proof.
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
OUT = Path("artifacts/runtime-evidence/production-vnext-release-default")
OUT.mkdir(parents=True, exist_ok=True)

# The unlocked Production VNext Reality shell's own consumer-facing marker. "生产 Reality" moved to
# the Me workspace page and no longer appears on the Reality Now screen, so it cannot prove that the
# Reality shell is on screen. This marker is the Production Now surface's own title and is also
# forbidden while locked, which keeps the fail-closed boundary strictly enforced.
REALITY_MARKER = "你的数字基础设施 · 已记录的全球上下文"


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


def has(nodes: list[dict[str, str]], text: str) -> bool:
    return any(text in label(n) for n in nodes)


def labels(nodes: list[dict[str, str]]) -> list[str]:
    return [x for n in nodes if (x := label(n))]


def wait_for(text: str, timeout: float = 25.0, snapshot: str = "wait") -> list[dict[str, str]]:
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
    import re
    m = re.fullmatch(r"\[(\d+),(\d+)\]\[(\d+),(\d+)\]", raw)
    if not m:
        raise ValueError(f"bad bounds: {raw}")
    x1, y1, x2, y2 = map(int, m.groups())
    return ((x1 + x2) // 2, (y1 + y2) // 2)


def tap_text(text: str) -> None:
    deadline = time.time() + 15
    while time.time() < deadline:
        nodes = dump("tap-probe")
        for node in nodes:
            if text in label(node):
                x, y = bounds_center(node["bounds"])
                adb("shell", "input", "tap", str(x), str(y))
                time.sleep(0.8)
                return
        time.sleep(0.5)
    raise RuntimeError(f"Could not tap {text!r}")


def start_default() -> None:
    adb("shell", "am", "force-stop", PACKAGE)
    # Each cold start gets its own launch proof; never conflate a prior app
    # process with the newly installed APK or a different Intent extra.
    adb("logcat", "-c")
    adb(
        "shell", "am", "start", "-W",
        "-n", f"{PACKAGE}/{ACTIVITY}",
    )
    time.sleep(1.5)


def assert_launch_target(expected: str) -> None:
    """Fail on compiled route mismatch before blaming the UI or LockGate."""
    raw = adb("logcat", "-d", "-s", "PDIGLaunchProof:I", "*:S").stdout
    (OUT / "debug-launch-proof.log").write_text(raw, encoding="utf-8")
    matching = [line for line in raw.splitlines() if "PDIGLaunchProof:" in line]
    if not matching:
        raise RuntimeError(
            "No debug Production launch provenance emitted. "
            "Cannot establish which BuildConfig flags/target the running APK used."
        )
    actual = matching[-1]
    if (
        f"target={expected}" not in actual or
        "flavor=production" not in actual or
        "generation=vnext" not in actual or
        "approved=true" not in actual or
        "debug=true" not in actual
    ):
        raise RuntimeError(
            f"PRODUCTION_LAUNCH_TARGET_MISMATCH expected={expected} "
            f"actual={actual.strip()}"
        )
    print("PRODUCTION_LAUNCH_PROOF=PASS", flush=True)


def assert_locked(name: str) -> None:
    nodes = wait_for("PDIG 已锁定", snapshot=name)
    for forbidden in ("生产 Reality", "个人控制面", "建立基础设施", REALITY_MARKER, "已确认数据"):
        if has(nodes, forbidden):
            raise RuntimeError(f"Reality leaked while locked: {forbidden}")


def unlock() -> list[dict[str, str]]:
    nodes = dump("unlock-capability")
    if has(nodes, "已知悉风险，本次进入"):
        tap_text("已知悉风险，本次进入")
    elif has(nodes, "验证身份并解锁"):
        raise RuntimeError(
            "Clean CI emulator unexpectedly requires system credentials; "
            "release-default rehearsal expects the no-credential test profile."
        )
    else:
        raise RuntimeError(f"No recognized unlock action; labels={labels(nodes)[:80]}")
    return wait_for(REALITY_MARKER, snapshot="production-vnext-default-unlocked")


def assert_five_primary(nodes: list[dict[str, str]]) -> None:
    missing = [
        item for item in ("现在", "基础设施", "变更", "记录", "我")
        if not any(label(n) == item for n in nodes)
    ]
    if missing:
        raise RuntimeError(f"Release-default Production VNext missing primary destinations: {missing}")


def main() -> None:
    evidence: dict[str, object] = {
        "sourceSha": os.environ.get("GITHUB_SHA", ""),
        "buildKeys": {
            "pdigProductionUiGeneration": "vnext",
            "pdigProductionVNextCutoverApproved": True,
        },
        "launchExtras": "NONE",
        "result": "PENDING",
        "checks": [],
    }

    start_default()
    assert_launch_target("PRODUCTION_REALITY_RELEASE")
    assert_locked("01-default-cutover-locked")
    evidence["checks"].append("DEFAULT_CUTOVER_FAIL_CLOSED")

    unlocked = unlock()
    assert_five_primary(unlocked)
    if has(unlocked, "参考数据") or has(unlocked, "SYNTHETIC"):
        raise RuntimeError("Synthetic Reference marker leaked into release-default Reality route")
    dump("02-default-cutover-unlocked")
    evidence["checks"].append("DEFAULT_CUTOVER_REALITY_FIVE_PRIMARY")

    adb("shell", "input", "keyevent", "KEYCODE_HOME")
    time.sleep(1.5)
    adb("shell", "am", "start", "-W", "-n", f"{PACKAGE}/{ACTIVITY}")
    assert_locked("03-default-cutover-background-relocked")
    evidence["checks"].append("DEFAULT_CUTOVER_BACKGROUND_RELOCK")

    resumed = unlock()
    assert_five_primary(resumed)
    dump("04-default-cutover-resumed")
    evidence["checks"].append("DEFAULT_CUTOVER_RESUME_REALITY")

    start_default()
    assert_locked("05-default-cutover-process-restart-locked")
    evidence["checks"].append("DEFAULT_CUTOVER_PROCESS_RELOCK")

    evidence["result"] = "PASS"
    (OUT / "manifest.json").write_text(
        json.dumps(evidence, ensure_ascii=False, indent=2),
        encoding="utf-8",
    )
    print("PRODUCTION_VNEXT_RELEASE_DEFAULT_REHEARSAL=PASS", flush=True)


if __name__ == "__main__":
    main()
