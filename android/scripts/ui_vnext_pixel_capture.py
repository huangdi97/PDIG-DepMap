#!/usr/bin/env python3
"""Capture real emulator pixels (not Compose previews) from the installable preview APK.

Captures launch, home, infrastructure, card list/details, change and records when reachable.
Never marks visual parity as PASS; that is a human comparison against the 1536x1024 board.
"""
from pathlib import Path
import json
import re
import subprocess
import time
import xml.etree.ElementTree as ET

ROOT = Path("artifacts/runtime-evidence/preview-phone")
ROOT.mkdir(parents=True, exist_ok=True)
PACKAGE = "com.pdig.app.preview"
ACTIVITY = "com.pdig.app.PreviewLauncherActivity"
observed = []

def adb(*args, check=True):
    return subprocess.run(["adb", *map(str, args)], check=check, text=True,
                          capture_output=True, timeout=80)

def xml_nodes():
    adb("shell", "uiautomator", "dump", "/sdcard/pdig-window.xml", check=False)
    adb("pull", "/sdcard/pdig-window.xml", str(ROOT / "last-ui.xml"), check=False)
    file = ROOT / "last-ui.xml"
    if not file.exists():
        return []
    try:
        return list(ET.parse(file).iter("node"))
    except ET.ParseError:
        return []

def label_of(node):
    return (node.get("text") or "") + " " + (node.get("content-desc") or "")

def tap_match(label, exact=False):
    matches = []
    for node in xml_nodes():
        desc = label_of(node)
        if (desc.strip() == label if exact else label in desc):
            bounds = re.findall(r"\d+", node.get("bounds") or "")
            if len(bounds) == 4:
                x1, y1, x2, y2 = map(int, bounds)
                if x2 > x1 and y2 > y1:
                    matches.append(((x1 + x2) // 2, (y1 + y2) // 2, desc))
    if not matches:
        return False
    x, y, desc = matches[0]
    adb("shell", "input", "tap", x, y)
    time.sleep(2)
    print("TAP", repr(label), x, y, repr(desc), flush=True)
    return True

def capture(name):
    time.sleep(3)
    path = ROOT / (name + ".png")
    with path.open("wb") as f:
        proc = subprocess.run(["adb", "exec-out", "screencap", "-p"],
                              stdout=f, stderr=subprocess.PIPE, timeout=40)
    if proc.returncode or path.stat().st_size < 10000:
        raise RuntimeError(f"Empty/unavailable screenshot: {path}")
    nodes = xml_nodes()
    labels = [label_of(n).strip() for n in nodes if label_of(n).strip()]
    (ROOT / (name + ".json")).write_text(
        json.dumps({"name": name, "sha": __import__("os").environ.get("GITHUB_SHA"),
                    "uiText": labels[:300], "bytes": path.stat().st_size},
                   ensure_ascii=False, indent=2), encoding="utf-8")
    observed.append({"name": name, "size": path.stat().st_size, "labels": len(labels)})
    print("CAPTURE", name, path.stat().st_size, flush=True)

def main():
    print(adb("shell", "wm", "size", "1080x2340").stdout)
    print(adb("shell", "wm", "density", "440").stdout)
    adb("shell", "am", "force-stop", PACKAGE, check=False)
    adb("shell", "monkey", "-p", PACKAGE, "1")
    time.sleep(10)
    capture("00-first-launch")
    tap_match("跳过")
    time.sleep(3)
    capture("01-now")
    if not tap_match("基础设施"):
        # Keep failure evidenced; top-level root may be hidden behind onboarding.
        capture("01-navigation-blocked")
        raise RuntimeError("Could not navigate to 基础设施 after onboarding")
    capture("02-infrastructure")
    if tap_match("卡片"):
        capture("03-cards")
        tap_match("招行储蓄卡")
        capture("04-card-detail")
    # Home-screen navigation is still the canonical path for root screens.
    adb("shell", "am", "force-stop", PACKAGE)
    adb("shell", "monkey", "-p", PACKAGE, "1")
    time.sleep(3)
    tap_match("变更")
    capture("05-change")
    tap_match("记录")
    capture("06-records")
    (ROOT / "manifest.json").write_text(
        json.dumps({"sha": __import__("os").environ.get("GITHUB_SHA"),
                    "app": PACKAGE, "activity": ACTIVITY,
                    "device": adb("shell", "getprop", "ro.product.model").stdout.strip(),
                    "capture": observed,
                    "assertion": "RUNTIME_PIXELS_CAPTURED_ONLY__NOT_VISUAL_PARITY"},
                   ensure_ascii=False, indent=2), encoding="utf-8")

if __name__ == "__main__":
    try:
        main()
    except Exception as exc:
        (ROOT / "ERROR.txt").write_text(repr(exc), encoding="utf-8")
        raise
