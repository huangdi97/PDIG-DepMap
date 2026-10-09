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
        # A toolbar Person icon also has content-desc="我". A cold launch can
        # expose it before the bottom navigation subtree is fully composed.
        # Reject top-bar matches instead of dispatching a real tap to the
        # wrong surface and then incorrectly claiming Me has broken routing.
        # The pixel runner pins a 2340px emulator; derive threshold from the
        # physical display rather than fixed y-coordinate clicks.
        match = re.search(r"(\d+)x(\d+)", adb("shell", "wm", "size").stdout)
        display_height = int(match.group(2)) if match else 2340
        matches = [hit for hit in matches if hit[1] >= display_height * 0.70]
        if not matches:
            return False
        matches.sort(key=lambda entry: entry[1], reverse=True)
    x, y, desc = matches[0]
    # Emulator ADB input can fail transiently even when the visible window
    # has a valid hitbox (e.g. guest shell is briefly restarting). Preserve
    # the real failure mode instead of reporting a misleading UI defect.
    last_error = ""
    for attempt in range(1, 5):
        try:
            result = subprocess.run(
                ["adb", "shell", "input", "tap", str(x), str(y)],
                text=True, capture_output=True, timeout=15,
            )
            if result.returncode == 0:
                time.sleep(2)
                print("TAP", repr(label), x, y, repr(desc), flush=True)
                return True
            last_error = (f"exit={result.returncode} "
                          f"stderr={result.stderr[:350]!r} stdout={result.stdout[:200]!r}")
        except subprocess.TimeoutExpired:
            last_error = "shell input tap timeout"
        print("ADB_TAP_RETRY", attempt, last_error, flush=True)
        time.sleep(3)
    diagnose_navigation("adb-tap-transport-failed")
    raise RuntimeError(f"Emulator could not dispatch actual tap to {label!r} "
                       f"at ({x},{y}) after four attempts: {last_error}")

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
    # Pixel Emulator sometimes returns an empty PNG while composition and GPU
    # capture race on its first textured frame. Retry the *same real screenshot*
    # rather than treating one empty adb pipe as a user-interface defect.
    last_error = "none"
    for attempt in range(1, 5):
        try:
            with path.open("wb") as f:
                proc = subprocess.run(["adb", "exec-out", "screencap", "-p"],
                                      stdout=f, stderr=subprocess.PIPE, timeout=50)
            size = path.stat().st_size
            if proc.returncode == 0 and size >= 10000:
                break
            last_error = f"attempt={attempt}, adb_exit={proc.returncode}, bytes={size}, stderr={proc.stderr[:220]!r}"
        except subprocess.TimeoutExpired:
            last_error = f"attempt={attempt}, adb screencap timed out"
        time.sleep(3)
    else:
        diagnose_navigation(f"{name}-empty-screenshot")
        raise RuntimeError(f"Empty/unavailable screenshot after four attempts: {path}; {last_error}")
    nodes = xml_nodes()
    labels = [label_of(n).strip() for n in nodes if label_of(n).strip()]
    (ROOT / (name + ".json")).write_text(
        json.dumps({"name": name, "sha": __import__("os").environ.get("GITHUB_SHA"),
                    "uiText": labels[:300], "bytes": path.stat().st_size,
                    "textureGate": texture_state},
                   ensure_ascii=False, indent=2), encoding="utf-8")
    observed.append({"name": name, "size": path.stat().st_size, "labels": len(labels)})
    print("CAPTURE", name, path.stat().st_size, flush=True)

def diagnose_navigation(name, previous_pid=None):
    """Keep device facts for every failed page transition; never reclassify as PASS."""
    pid = adb("shell", "pidof", PACKAGE, check=False).stdout.strip()
    activity = adb("shell", "dumpsys", "activity", "activities", check=False).stdout
    logs = adb("logcat", "-d", "-t", "750", check=False).stdout
    (ROOT / (name + "-diagnosis.json")).write_text(
        json.dumps({"previousPid": previous_pid, "currentPid": pid,
                    "package": PACKAGE, "sha": os.environ.get("GITHUB_SHA"),
                    "resumedActivities": [line.strip() for line in activity.splitlines()
                                          if "mResumed" in line or "topResumed" in line or PACKAGE in line][:80],
                    "androidRuntimeErrors": [line for line in logs.splitlines()
                                            if "FATAL EXCEPTION" in line or "AndroidRuntime" in line
                                            or "Process: " + PACKAGE in line][-120:],
                    "routeTrace": [line for line in logs.splitlines()
                                   if "PdigPreviewNav" in line or "am_proc_died" in line
                                   or "ActivityManager" in line and PACKAGE in line][-80:]},
                   ensure_ascii=False, indent=2), encoding="utf-8")
    print("NAVIGATION_DIAGNOSIS", name, "pid", previous_pid, "->", pid, flush=True)


def require_screen(name, *texts):
    data = json.loads((ROOT / (name + ".json")).read_text(encoding="utf-8"))
    labels = data.get("uiText", [])
    for required in texts:
        if not any(required in label for label in labels):
            diagnose_navigation("screen-" + name + "-missing")
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


def projected_region_bounds():
    """Real Compose geographic annotations; NOT fixed R15 corner cards."""
    found = {}
    for node in xml_nodes():
        label = label_of(node).strip()
        if not label.startswith(("地球地区：", "地球地区组：")):
            continue
        bounds = re.findall(r"\d+", node.get("bounds") or "")
        if len(bounds) != 4:
            continue
        rect = tuple(map(int, bounds))
        if rect[2] > rect[0] and rect[3] > rect[1]:
            found[label] = rect
    return found


def assert_home_world_geometry():
    """R16: small geography-locked regions cannot invade title or asset rail."""
    header = _bounds_for_exact("你的全球数字基础设施")
    footer = _bounds_for_exact("银行卡")
    if header is None or footer is None:
        raise RuntimeError(f"World title/asset rail not found: {header}, {footer}")
    tags = projected_region_bounds()
    if not tags:
        raise RuntimeError("R16 projected front-side geographic annotations missing")
    if len(tags) > 4:
        raise RuntimeError(f"R16 phone label budget exceeded: {len(tags)}")
    if header[3] >= footer[1]:
        raise RuntimeError(f"World title and asset rail overlap: {header}, {footer}")
    for name, rect in tags.items():
        if rect[1] < header[3] or rect[3] > footer[1]:
            raise RuntimeError(f"R16 geography chip covers reserved header/asset rail: {name}: {rect}")
    for i, (name_a, a) in enumerate(tags.items()):
        for name_b, b in list(tags.items())[i+1:]:
            if min(a[2], b[2]) > max(a[0], b[0]) and min(a[3], b[3]) > max(a[1], b[1]):
                raise RuntimeError(f"Overlapping R16 geo hit targets: {name_a}={a}, {name_b}={b}")
    (ROOT / "01-now-layout-bounds.json").write_text(
        json.dumps({"title": header, "metric": footer, "geoLabels": tags,
                    "labelBudget": 4, "layout": "R16_CAMERA_PROJECTED",
                    "humanVisualParity": "PENDING"},
                   ensure_ascii=False, indent=2), encoding="utf-8")
    print("R16_GEO_HERO_GEOMETRY=PASS", flush=True)



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
    # A real 3D/textured stage can take longer than a fixed 3s after Skip.
    # Never call screencap while the onboarding UI is still being dismissed.
    entered_now = False
    for attempt in range(24):
        visible = [label_of(node) for node in xml_nodes()]
        if any("你的全球数字基础设施" in text for text in visible):
            entered_now = True
            break
        if attempt in (5, 11) and any("跳过" in text for text in visible):
            tap_match("跳过", exact=True)
        time.sleep(2)
    if not entered_now:
        diagnose_navigation("onboarding-to-now-timeout")
        raise RuntimeError("Onboarding did not reach the R13 Now screen within 48s")
    capture("01-now")
    require_screen("01-now", "你的全球数字基础设施", "轻触地球探索", "· R19")
    # An existing CPU fallback showing a photograph is not proof of R15.
    # Exact-head Preview MUST initialize the independent GPU shader.
    globe_labels = [label_of(n) for n in xml_nodes() if "全球基础设施导航器" in label_of(n)]
    if not any("R15_GPU" in text and "纹理状态=TEXTURE_READY" in text
               for text in globe_labels):
        raise RuntimeError("R15 GPU shader never became active; cannot certify a CPU fallback")
    gl_log = adb("logcat", "-d", "-s", "PDIG_R15:I", "*:S", check=False).stdout
    (ROOT / "01-r15-gl-driver.log").write_text(gl_log, encoding="utf-8")
    if "R15_GPU_TEXTURES_READY" not in gl_log:
        raise RuntimeError("R15 GPU textures missing from actual Android GL initialization logs")
    now_labels = json.loads((ROOT / "01-now.json").read_text(encoding="utf-8"))["uiText"]
    if not any(any(greeting in label for greeting in ("早上好", "中午好", "下午好", "晚上好", "你好"))
               for label in now_labels):
        raise RuntimeError("No localized greeting is visible in R9 Now")
    assert_home_world_geometry()
    verify_preview_world_light()

    # R14 genuine globe interaction: CAMERA and actual textured frames must
    # respond together. A drag which only turns graph arcs is not acceptable.
    def camera_state():
        labels = [label_of(node) for node in xml_nodes()]
        globe = next((line for line in labels if "全球基础设施导航器" in line), "")
        yaw = re.search(r"视角=(-?\d+)度", globe)
        zoom = re.search(r"缩放=(\d+)%", globe)
        if not yaw or not zoom:
            raise RuntimeError("Globe does not expose real camera transform semantics: " + globe)
        return (int(yaw.group(1)), int(zoom.group(1)))

    before_geo = projected_region_bounds()
    if not before_geo:
        raise RuntimeError("No projected region tags before rotation")
    yaw0, zoom0 = camera_state()
    # Derive the drag position from the *actual* globe surface bounds.
    # A hard-coded screen y=855 could land on a floating region chip or below
    # the stage after redesign, giving a false "gesture broken" diagnosis.
    globe_nodes = [n for n in xml_nodes() if "全球基础设施导航器" in label_of(n)]
    bounds = re.findall(r"\d+", globe_nodes[0].get("bounds", "")) if globe_nodes else []
    if len(bounds) != 4:
        raise RuntimeError("Cannot locate real globe bounds for drag evidence")
    x0, y0, x1, y1 = map(int, bounds)
    swipe_y = int(y0 + (y1-y0)*0.56)
    swipe_from = int(x0 + (x1-x0)*0.34)
    swipe_to = int(x0 + (x1-x0)*0.68)
    adb("shell", "input", "swipe", str(swipe_from), str(swipe_y),
        str(swipe_to), str(swipe_y), "650")
    time.sleep(4)
    yaw1, zoom1 = camera_state()
    if abs(yaw1 - yaw0) < 10:
        raise RuntimeError(f"Dragging did not orbit the actual globe: {yaw0} -> {yaw1}")
    capture("01c-world-orbit")
    require_screen("01c-world-orbit", "全球基础设施导航器")
    after_geo = projected_region_bounds()
    if not after_geo:
        raise RuntimeError("R16 geo labels all disappeared after globe rotation")
    if before_geo == after_geo:
        raise RuntimeError("R16 region chips stayed at fixed screen coordinates after true camera orbit")
    shared = set(before_geo) & set(after_geo)
    if shared:
        shifted = any(
            abs(before_geo[name][0] - after_geo[name][0]) > 12 or
            abs(before_geo[name][1] - after_geo[name][1]) > 12
            for name in shared
        )
        if not shifted and set(before_geo) == set(after_geo):
            raise RuntimeError("Camera moved, but matching geographic pills did not")
    (ROOT / "01c-world-orbit-label-motion.json").write_text(
        json.dumps({"sourceSha": os.environ.get("GITHUB_SHA"),
                    "beforeYaw": yaw0, "afterYaw": yaw1,
                    "before": before_geo, "after": after_geo,
                    "result": "CAMERA_LINKED_LABEL_MOVEMENT",
                    "humanVisualParity": "PENDING"},
                   ensure_ascii=False, indent=2), encoding="utf-8")
    print("R16_GEO_LABEL_ORBIT=PASS", flush=True)
    if not tap_retry("放大地球"):
        raise RuntimeError("R14 explicit globe zoom-in control not accessible")
    time.sleep(3)
    yaw2, zoom2 = camera_state()
    if zoom2 <= zoom1:
        raise RuntimeError(f"Globe zoom control did not change camera zoom: {zoom1}% -> {zoom2}%")
    capture("01d-world-zoom-in")
    if not tap_retry("缩小地球"):
        raise RuntimeError("Globe zoom-out control not available")
    if not tap_retry("复位地球"):
        raise RuntimeError("Globe reset control not available")
    time.sleep(3)
    _, zoomReset = camera_state()
    if abs(zoomReset - 100) > 1:
        raise RuntimeError(f"Globe reset did not restore 100%: {zoomReset}%")
    capture("01e-world-reset")

    from os import environ
    short_sha = environ.get("GITHUB_SHA", "")[:7]
    if short_sha:
        ui = json.loads((ROOT / "01-now.json").read_text(encoding="utf-8"))["uiText"]
        if not any(short_sha in line for line in ui):
            raise RuntimeError(f"Displayed preview source SHA {short_sha} missing from 01-now UI XML")
    # The user's fifth root tab is a real workspace, not a decorative icon.
    # Check it *before* later Studio interaction to distinguish ME route bugs
    # from bugs caused by leaving a child customization screen.
    if not tap_retry("我", exact=True, prefer_bottom=True):
        raise RuntimeError("The fifth primary navigation item (我) is not clickable")
    capture("01a-me-from-primary-nav")
    try:
        require_screen("01a-me-from-primary-nav", "我的数字生活", "隐私与个人偏好")
    except RuntimeError:
        diagnose_navigation("me-from-primary-nav")
        raise
    if not tap_retry("现在", exact=True, prefer_bottom=True):
        raise RuntimeError("Could not return from 我 to 现在 through root navigation")
    capture("01b-now-returned-from-me")
    require_screen("01b-now-returned-from-me", "你的全球数字基础设施")

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
    require_screen("03-cards", "全球支付卡片", "招行储蓄卡",
                   "全部地区", "全部状态", "全部卡组织")
    if not tap_retry("全部状态", exact=True):
        raise RuntimeError("R9 card status filter not interactive")
    if not tap_retry("即将到期", exact=True):
        raise RuntimeError("R9 card status options cannot select expiring cards")
    capture("03a-card-expiry-filter")
    require_screen("03a-card-expiry-filter", "工行信用卡", "即将到期")
    if not tap_retry("即将到期", exact=True):
        raise RuntimeError("R9 status filter cannot reopen to clear")
    if not tap_retry("全部状态", exact=True):
        raise RuntimeError("R9 status filter cannot reset to all records")
    capture("03b-card-filter-cleared")
    require_screen("03b-card-filter-cleared", "招行储蓄卡", "全部状态")
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
    # R11: card art is an IN-DETAIL micro action, not a giant Studio.
    if not tap_retry("内置卡面", exact=True):
        raise RuntimeError("The compact inline card-art utility is missing")
    capture("04e-card-presets-inline")
    require_screen("04e-card-presets-inline", "选择内置卡面", "原卡面", "海洋", "相册换图")
    if not tap_retry("海洋", exact=True):
        raise RuntimeError("Compact card artwork option could not be selected")
    # Artwork changes do not reset the actively selected detail tab.
    # Switch to Overview before asserting "基本信息" rather than mistaking a
    # preserved Safety tab for a regression.
    if not tap_retry("概览", exact=True):
        raise RuntimeError("Card detail Overview tab unavailable after artwork change")
    capture("04f-card-art-applied")
    require_screen("04f-card-art-applied", "基本信息")
    # Top-left UP must be hierarchical, not a chronological jump to Now.
    if not tap_retry("返回上一级", exact=True):
        raise RuntimeError("Hierarchical top arrow unavailable on card detail")
    capture("04g-up-to-cards")
    require_screen("04g-up-to-cards", "全球支付卡片")
    # Navigate directly from the real Cards list to Change. The previous
    # script force-stopped/relaunched the app before tapping the root tab,
    # changing the system/back-stack scenario and producing a false Now image.
    # Cold-start behavior is already separately covered by 00-first-launch.
    before_pid = adb("shell", "pidof", PACKAGE, check=False).stdout.strip()
    if not tap_retry("变更", exact=True, prefer_bottom=True):
        diagnose_navigation("05-change-tap-missing", before_pid)
        raise RuntimeError("Change root navigation unavailable directly after Cards")
    capture("05-change")
    after_pid = adb("shell", "pidof", PACKAGE, check=False).stdout.strip()
    if not before_pid or before_pid != after_pid:
        diagnose_navigation("05-change-process-restarted", before_pid)
        raise RuntimeError(f"PDIG process restarted when navigating to Change: {before_pid} -> {after_pid}")
    require_screen("05-change", "影响分析 · 关键服务", "旧手机号")
    if not tap_retry("查看本阶段核验清单 →", exact=True):
        adb("shell", "input", "swipe", "530", "1650", "530", "800", "400")
        if not tap_retry("查看本阶段核验清单 →", exact=True):
            raise RuntimeError("R9 Change action to review the active verification checklist unavailable")
    adb("shell", "input", "swipe", "530", "1660", "530", "1080", "400")
    capture("05a-change-checklist")
    require_screen("05a-change-checklist", "本阶段待办")
    # The disclaimer sits AFTER the four service rows. A real phone viewport
    # cannot show the expanded heading and its footer simultaneously; asserting
    # both from the first screenshot was a false visual/e2e failure.
    for attempt in range(6):
        if any("不会通过点击自动标记已完成" in label_of(n) for n in xml_nodes()):
            break
        adb("shell", "input", "swipe", "530", "1660", "530", "810", "400")
        time.sleep(2)
    else:
        capture("05a-checklist-footer-missing")
        raise RuntimeError("Expanded checklist footer never became visible after scrolling")
    capture("05aa-change-checklist-footer")
    require_screen("05aa-change-checklist-footer", "不会通过点击自动标记已完成")
    # Return to the top from the actual lower scroll position before switching
    # the projection; never pass because a static stale button happens to exist.
    for attempt in range(6):
        if tap_match("完成后（计划）", exact=True):
            break
        adb("shell", "input", "swipe", "530", "480", "530", "1840", "420")
        time.sleep(2)
    else:
        raise RuntimeError("Change Phone plan projection selector missing after return-to-top")
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
    require_screen("08-numbers", "号码 · 通信身份", "+86 138****8823")
    if not tap_retry("+86 138****8823"):
        raise RuntimeError("Recorded number fallback was not selectable")
    capture("08b-number-detail")
    require_screen("08b-number-detail", "安全与恢复", "关联服务")
    if not tap_retry("修改名称 →", exact=True):
        raise RuntimeError("Number alias edit control unavailable")
    if not any("给号码命名" in label_of(n) for n in xml_nodes()):
        raise RuntimeError("Number alias dialog did not open")
    if not tap_retry("自定义名称（可留空）", exact=True):
        raise RuntimeError("Number alias text input was not focusable")
    adb("shell", "input", "text", "HK-Main")
    if not tap_retry("保存名称", exact=True):
        raise RuntimeError("Number alias could not be saved")
    capture("08bb-number-renamed")
    require_screen("08bb-number-renamed", "HK-Main")
    if not tap_retry("号码外观 →", exact=True):
        adb("shell", "input", "swipe", 520, 1650, 520, 780, 350)
        if not tap_retry("号码外观 →", exact=True):
            raise RuntimeError("Number appearance controls unreachable")
    capture("08c-number-studio")
    require_screen("08c-number-studio", "号码面定制", "实时预览", "选择主题")
    if not tap_retry("城市", exact=True):
        raise RuntimeError("Number Studio: theme selection unavailable")
    capture("08d-number-studio-edited")
    require_screen("08d-number-studio-edited", "保存外观")
    if not tap_retry("保存外观", exact=True):
        raise RuntimeError("Number Studio: saving presentation profile failed")
    # Supporting screens must also be the new R9 renderer and expose actual
    # persisted settings/source truth. Avoid static source-only acceptance.
    adb("shell", "input", "keyevent", "4")
    time.sleep(2)
    adb("shell", "input", "keyevent", "4")
    time.sleep(2)
    # Fifth tab replaces the four-icon engineering toolbar. Verify user
    # privacy control and previous-screen back navigation from the actual app.
    adb("shell", "am", "force-stop", PACKAGE)
    adb("shell", "am", "start", "-n", PACKAGE + "/" + ACTIVITY)
    time.sleep(3)
    if not tap_retry("我", exact=True, prefer_bottom=True):
        raise RuntimeError("Fifth Me bottom-navigation label was not yet composed")
    # Dispatch success is not route success. The live content must change to
    # the Me workspace; retry the actual *bottom* tab, not the header shortcut.
    for me_attempt in range(4):
        labels = [label_of(node) for node in xml_nodes()]
        if any("我的数字生活" in label for label in labels):
            break
        print("ME_NAV_RETRY", me_attempt + 1, "Me content not yet visible", flush=True)
        time.sleep(2)
        if not tap_retry("我", exact=True, prefer_bottom=True, retries=2):
            break
    else:
        diagnose_navigation("me-route-not-rendered")
        raise RuntimeError("Bottom Me tab dispatched but R10MeScreen did not render")
    capture("09-me")
    require_screen("09-me", "我的数字生活", "敏感信息遮蔽", "已关闭")
    if not tap_retry("敏感信息遮蔽", exact=True):
        raise RuntimeError("Me tab privacy control not tappable")
    capture("09a-me-mask-enabled")
    require_screen("09a-me-mask-enabled", "已开启")
    if not tap_retry("敏感信息遮蔽", exact=True):
        raise RuntimeError("Me tab privacy control could not be disabled again")
    capture("09b-me-mask-disabled")
    require_screen("09b-me-mask-disabled", "已关闭")
    # The rich Me page intentionally extends past the first fold. Scroll to the
    # real Settings action; absence from a screenshot is NOT a missing route.
    for attempt in range(8):
        if tap_match("偏好设置", exact=True):
            break
        adb("shell", "input", "swipe", 520, 1880, 520, 770, 330)
        time.sleep(1)
    else:
        capture("09-me-settings-unreachable")
        raise RuntimeError("Personal preference panel unreachable after scrolling Me")
    capture("09-personalization")
    # Data Sources is intentionally below the first fold on compact phones.
    # The old test falsely failed a valid scrollable Settings page by demanding
    # that its bottom action appear in the *initial* 2340px screenshot.
    require_screen("09-personalization", "显示与个性化", "隐藏敏感信息")
    for attempt in range(5):
        if tap_match("数据源", exact=True):
            break
        adb("shell", "input", "swipe", 520, 1850, 520, 700, 400)
        time.sleep(2)
    else:
        capture("09-settings-source-unreachable")
        raise RuntimeError("R9 Data Sources entry unreachable after actual scrolling")
    capture("10-data-sources")
    require_screen("10-data-sources", "当前预览工作区", "SYNTHETIC", "未知")

    # Include real runtime frame-time evidence even when device image comparisons
    # have already passed. A static circle with rotating lines is not accepted.
    perf = adb("logcat", "-d", "-s", "PdigGlobePerf:I", "*:S", check=False)
    (ROOT / "globe-runtime-performance.log").write_text(perf.stdout, encoding="utf-8")
    timings = []
    for row in perf.stdout.splitlines():
        match = re.search(r"phase=(DRAG|SETTLED) rect=(\d+) durationMs=(\d+)", row)
        if match:
            timings.append({"phase":match.group(1),"rect":int(match.group(2)),
                            "durationMs":int(match.group(3))})
    (ROOT / "globe-render-timings.json").write_text(
        json.dumps({"sourceSha": os.environ.get("GITHUB_SHA"),
                    "frames":timings, "visualAcceptance":"PENDING"},
                   indent=2), encoding="utf-8")

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
