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

def tap_match(label, exact=False, prefer_bottom=False):
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
    if prefer_bottom:
        matches.sort(key=lambda entry: entry[1], reverse=True)
    x, y, desc = matches[0]
    adb("shell", "input", "tap", x, y)
    time.sleep(2)
    print("TAP", repr(label), x, y, repr(desc), flush=True)
    return True

def tap_retry(label, exact=False, prefer_bottom=False, retries=6):
    for attempt in range(retries):
        if tap_match(label, exact=exact, prefer_bottom=prefer_bottom):
            return True
        time.sleep(2)
    return False


def wait_globe_texture(max_wait=65):
    # A near-black placeholder is NOT accepted as proof of the designed Earth.
    end = time.monotonic() + max_wait
    seen = []
    while time.monotonic() < end:
        found = [label_of(node) for node in xml_nodes()
                 if "全球基础设施导航器" in label_of(node)]
        if not found:
            return "NOT_A_GLOBE_SCREEN"
        seen.extend(found[:1])
        if any("纹理状态=TEXTURE_READY" in line for line in found):
            return "TEXTURE_READY"
        time.sleep(2)
    raise RuntimeError(f"Globe texture did not become TEXTURE_READY: ${seen[-3:]}")


def capture(name):
    texture_state = wait_globe_texture() if name in ("01-now", "02-infrastructure") else "NOT_REQUIRED"
    time.sleep(2)
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
                    "uiText": labels[:300], "bytes": path.stat().st_size,
                    "textureGate": texture_state},
                   ensure_ascii=False, indent=2), encoding="utf-8")
    observed.append({"name": name, "size": path.stat().st_size, "labels": len(labels)})
    print("CAPTURE", name, path.stat().st_size, flush=True)

def require_screen(name, *texts):
    data = json.loads((ROOT / (name + ".json")).read_text(encoding="utf-8"))
    labels = data.get("uiText", [])
    for required in texts:
        if not any(required in label for label in labels):
            raise RuntimeError(f"Screen ${name} did not show ${required!r}; captured wrong page")


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
    require_screen("01-now", "你的全球数字基础设施")
    from os import environ
    short_sha = environ.get("GITHUB_SHA", "")[:7]
    if short_sha:
        ui = json.loads((ROOT / "01-now.json").read_text(encoding="utf-8"))["uiText"]
        if not any(short_sha in line for line in ui):
            raise RuntimeError(f"Displayed preview source SHA {short_sha} missing from 01-now UI XML")
    if not tap_match("基础设施", exact=True, prefer_bottom=True):
        # Keep failure evidenced; top-level root may be hidden behind onboarding.
        capture("01-navigation-blocked")
        raise RuntimeError("Could not navigate to 基础设施 after onboarding")
    capture("02-infrastructure")
    require_screen("02-infrastructure", "8 类资产", "地区分布")
    if not tap_match("卡片", exact=True):
        raise RuntimeError("Card icon not clickable from infrastructure hub")
    capture("03-cards")
    require_screen("03-cards", "招行储蓄卡")
    if not tap_match("招行储蓄卡"):
        raise RuntimeError("Card detail tap not found")
    capture("04-card-detail")
    require_screen("04-card-detail", "基本信息")
    # Home-screen navigation is still the canonical path for root screens.
    adb("shell", "am", "force-stop", PACKAGE)
    adb("shell", "monkey", "-p", PACKAGE, "1")
    time.sleep(3)
    if not tap_retry("变更", exact=True, prefer_bottom=True):
        raise RuntimeError("Change root navigation unavailable after activity relaunch")
    capture("05-change")
    require_screen("05-change", "影响分析 · 关键服务")
    if not tap_match("记录", exact=True, prefer_bottom=True):
        raise RuntimeError("Records bottom navigation not found")
    capture("06-records")
    require_screen("06-records", "迁移进度")
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
