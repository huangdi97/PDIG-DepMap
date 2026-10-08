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
import os
PACKAGE = "com.pdig.app.preview.p" + os.environ["GITHUB_SHA"][:7] if os.environ.get("GITHUB_SHA") else "com.pdig.app.preview"
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
    raise RuntimeError(f"Globe texture did not become TEXTURE_READY: {seen[-3:]}")


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
            raise RuntimeError(f"Screen {name} did not show {required!r}; captured wrong page")


def _bounds_for_exact(label):
    for node in xml_nodes():
        if label_of(node).strip() != label:
            continue
        bounds = re.findall(r"\d+", node.get("bounds") or "")
        if len(bounds) == 4:
            rect = tuple(map(int, bounds))
            if rect[2] > rect[0] and rect[3] > rect[1]:
                return rect
    return None


def assert_home_world_geometry():
    """P0 readable layers: title, real region identities, then 4-asset footer.

    A first-fold consumer reference is invalid when metrics cover region chips or
    region labels overprint the heading. Test the actual UI bounds, not merely Kotlin
    component existence. This does not claim pixel-perfect artistic parity.
    """
    header = _bounds_for_exact("你的全球数字基础设施")
    footer = _bounds_for_exact("银行卡")
    regions = {
        name: _bounds_for_exact(name)
        for name in ("美国", "英国", "中国大陆", "香港", "新加坡")
    }
    if header is None or footer is None or any(x is None for x in regions.values()):
        raise RuntimeError(f"World hero labels missing: title={header}, footer={footer}, regions={regions}")
    if header[3] >= min(rect[1] for rect in regions.values()):
        raise RuntimeError(f"World hero title overlaps region identities: title={header}, regions={regions}")
    if max(rect[3] for rect in regions.values()) >= footer[1]:
        raise RuntimeError(f"World hero region identities overlap metric strip: footer={footer}, regions={regions}")
    for a, b in (("美国", "香港"), ("英国", "中国大陆"), ("中国大陆", "新加坡")):
        x, y = regions[a], regions[b]
        if min(x[2], y[2]) > max(x[0], y[0]) and min(x[3], y[3]) > max(x[1], y[1]):
            raise RuntimeError(f"Overlapping region identity labels: {a}={x}, {b}={y}")
    (ROOT / "01-now-layout-bounds.json").write_text(
        json.dumps({"title": header, "metric": footer, "regions": regions,
                    "result": "DISJOINT_LABEL_BOUNDS_ONLY_NOT_HUMAN_VISUAL_ACCEPTANCE"},
                   ensure_ascii=False, indent=2), encoding="utf-8")
    print("PHONE_WORLD_GEOMETRY_LABEL_DISJOINT=PASS", flush=True)


def verify_preview_world_light():
    """Mechanical early-warning gate, NOT human art approval.

    Fixture/device are pinned to 1080x2340. Sample the central unlabelled part
    of the planet, not the sky background or bright region identity chips.
    A near-black globe passed previous text/texture gates; it must not pass R9.
    """
    from PIL import Image
    image = Image.open(ROOT / "01-now.png").convert("RGB")
    width, height = image.size
    if (width != 1080 or height != 2340):
        raise RuntimeError(f"World light probe needs pinned 1080x2340, got {width}x{height}")
    sample = image.crop((int(width * .40), int(height * .34),
                         int(width * .60), int(height * .42)))
    pixels = list(sample.getdata())
    luma = sum(.2126 * r + .7152 * g + .0722 * b for r, g, b in pixels) / len(pixels)
    low = sum(1 for r, g, b in pixels if (.2126*r + .7152*g + .0722*b) < 55) / len(pixels)
    summary = {"sourceSha": os.environ.get("GITHUB_SHA"), "box": [.40,.34,.60,.42],
               "meanLuma": round(luma, 2), "darkPixelFraction": round(low, 4),
               "evidenceKind": "R9_TEXTURE_GRADE_MECHANICAL_ONLY",
               "humanVisualParity": "NOT_ACCEPTED"}
    (ROOT / "01-now-globe-visual-metrics.json").write_text(
        json.dumps(summary, ensure_ascii=False, indent=2), encoding="utf-8")
    print("R9_GLOBE_LIGHT_PROBE", summary, flush=True)
    if luma < 83.0:
        raise RuntimeError("R9 Globe is still too dark for the light-first Preview; "
                           f"central mean luma={luma:.1f} < 83.0")


def main():
    print(adb("shell", "wm", "size", "1080x2340").stdout)
    print(adb("shell", "wm", "density", "440").stdout)
    adb("shell", "am", "force-stop", PACKAGE, check=False)
    # Pixel Launcher may display an emulator-only ANR dialog during first boot.
    # Explicit Activity launch avoids waiting for the home launcher to handle intents.
    adb("shell", "settings", "put", "global", "window_animation_scale", "0", check=False)
    adb("shell", "settings", "put", "global", "transition_animation_scale", "0", check=False)
    adb("shell", "am", "start", "-n", PACKAGE + "/" + ACTIVITY)
    time.sleep(10)
    # An emulator-only launcher ANR may cover the actual PDIG guide even when the
    # Activity is healthy. Dismiss the OS dialog, never treat the dialog as PDIG pixels.
    if any("Pixel Launcher isn't responding" in label_of(n) for n in xml_nodes()):
        if not tap_match("Close app", exact=True):
            raise RuntimeError("Emulator launcher ANR could not be dismissed")
        time.sleep(2)
    capture("00-first-launch")
    if not tap_retry("跳过", exact=True):
        raise RuntimeError("Preview first-run onboarding skip could not be activated")
    time.sleep(3)
    capture("01-now")
    require_screen("01-now", "你的全球数字基础设施", "早上好", "R9 · ")
    assert_home_world_geometry()
    verify_preview_world_light()
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
    require_screen("02-infrastructure", "8 类资产", "地区分布", "管理你的全球数字基础设施")
    if not tap_retry("美国", exact=True):
        raise RuntimeError("Selecting a concrete region from the infrastructure overview failed")
    capture("02a-infrastructure-selected-us")
    require_screen("02a-infrastructure-selected-us", "已选地区：美国", "查看全球")
    if not tap_retry("查看全球 →", exact=True):
        raise RuntimeError("Clearing the selected region failed")
    capture("02b-infrastructure-global-restored")
    if any("已选地区：" in text for text in
           json.loads((ROOT / "02b-infrastructure-global-restored.json").read_text(encoding="utf-8"))["uiText"]):
        raise RuntimeError("Region context was not cleared before continuing to Cards")
    if not tap_match("卡片", exact=True):
        raise RuntimeError("Card icon not clickable from infrastructure hub")
    capture("03-cards")
    require_screen("03-cards", "全球支付卡片", "招行储蓄卡")
    if not tap_match("招行储蓄卡"):
        raise RuntimeError("Card detail tap not found")
    capture("04-card-detail")
    require_screen("04-card-detail", "基本信息")
    for name, shot, required in (
        ("关联服务", "04b-card-services", "关联服务"),
        ("账单", "04c-card-statements", "尚未导入可核验的账单"),
        ("安全与风险", "04d-card-risk", "安全与风险"),
    ):
        if not tap_retry(name, exact=True):
            raise RuntimeError(f"Missing genuine card-detail tab: {name}")
        capture(shot)
        require_screen(shot, required)
    # Studio MUST be an interactive surface: entering, changing a theme and
    # saving PresentationProfile must genuinely update the detail renderer.
    if not tap_retry("定制这张卡的外观 →", exact=True):
        raise RuntimeError("Card Studio CTA did not open")
    capture("04e-card-studio")
    require_screen("04e-card-studio", "卡面定制", "实时预览", "选择主题")
    if not tap_retry("极简", exact=True):
        raise RuntimeError("Card Studio theme control was not reachable")
    capture("04f-card-studio-edited")
    require_screen("04f-card-studio-edited", "保存外观")
    if not tap_retry("保存外观", exact=True):
        raise RuntimeError("Card Studio failed to persist PresentationProfile")
    if not any("已保存" in label_of(n) for n in xml_nodes()):
        raise RuntimeError("Saved card presentation profile did not show confirmed state")
    adb("shell", "input", "keyevent", "4")
    time.sleep(2)
    capture("04g-card-detail-after-studio")
    require_screen("04g-card-detail-after-studio", "基本信息")
    # Home-screen navigation is still the canonical path for root screens.
    adb("shell", "am", "force-stop", PACKAGE)
    adb("shell", "am", "start", "-n", PACKAGE + "/" + ACTIVITY)
    time.sleep(3)
    if not tap_retry("变更", exact=True, prefer_bottom=True):
        raise RuntimeError("Change root navigation unavailable after activity relaunch")
    capture("05-change")
    require_screen("05-change", "影响分析 · 关键服务", "旧手机号")
    if not tap_retry("完成后（计划）", exact=True):
        raise RuntimeError("Change Phone plan projection selector missing")
    capture("05b-change-plan")
    require_screen("05b-change-plan", "完成后预览")
    if not tap_retry("记录", exact=True, prefer_bottom=True):
        raise RuntimeError("Records bottom navigation not found")
    capture("06-records")
    require_screen("06-records", "迁移进度", "追踪变更、风险")
    # R9 parity requires that every 4x2 category opens a real R9 object workspace;
    # source-only composable existence is not accepted evidence.
    for category, slug, expected in (
        ("账户", "accounts", "已记录账户"),
        ("邮箱", "emails", "已记录邮箱"),
        ("设备", "devices", "已记录设备"),
        ("服务", "services", "已记录服务"),
        ("薄弱点", "weaknesses", "已记录关注项"),
    ):
        if not tap_retry("基础设施", exact=True, prefer_bottom=True):
            raise RuntimeError(f"Cannot navigate from Records to Infrastructure for {category}")
        require_screen_if_navigated = [label_of(n) for n in xml_nodes()]
        if not any("基础设施" in label for label in require_screen_if_navigated):
            raise RuntimeError("Infrastructure root context missing")
        if not tap_retry(category, exact=True):
            raise RuntimeError(f"Cannot open R9 infrastructure category {category}")
        name = f"07-{slug}"
        capture(name)
        require_screen(name, expected)
        if not any(f"pdig.r9.screen.{slug}" in (node.get("resource-id") or "") for node in xml_nodes()):
            # Compose test tags are not always surfaced as Android resource IDs,
            # therefore screen-content assertion above remains the hard gate.
            print("CATEGORY_CONTENT_VERIFIED", slug, flush=True)

    if not tap_retry("基础设施", exact=True, prefer_bottom=True):
        raise RuntimeError("Number Studio: return to Infrastructure failed")
    if not tap_retry("号码", exact=True):
        raise RuntimeError("Number Studio: number category missing")
    capture("08-numbers")
    require_screen("08-numbers", "号码 · 通信身份", "主号 中国移动")
    if not tap_retry("主号 中国移动"):
        raise RuntimeError("Number Studio: opening primary-number detail failed")
    capture("08b-number-detail")
    require_screen("08b-number-detail", "安全与恢复", "关联服务")
    if not tap_retry("定制 →", exact=True):
        raise RuntimeError("Number Studio: enter customization missing")
    capture("08c-number-studio")
    require_screen("08c-number-studio", "号码面定制", "实时预览", "选择主题")
    if not tap_retry("城市", exact=True):
        raise RuntimeError("Number Studio: theme selection unavailable")
    capture("08d-number-studio-edited")
    require_screen("08d-number-studio-edited", "保存外观")
    if not tap_retry("保存外观", exact=True):
        raise RuntimeError("Number Studio: saving presentation profile failed")

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
