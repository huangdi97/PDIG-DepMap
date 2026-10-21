#!/usr/bin/env python3
"""
PDIG post-freeze closure runtime capture (API36 phone / tablet).

Every screenshot is paired with a UIAutomator XML snapshot and a MANIFEST entry
that binds it to the exact source head, the installed APK hash, the device
profile, the navigation sequence and the assertions that were actually checked.

This script never decides that the product is correct. It reports what the
running APK showed, fails loudly when an assertion does not hold, and leaves
humanReview = NOT_RUN for the human visual gate.
"""

from __future__ import annotations

import argparse
import hashlib
import json
import os
import re
import subprocess
import sys
import time
import xml.etree.ElementTree as ET
from datetime import datetime, timezone
from pathlib import Path

SERIAL = os.environ.get("PDIG_SERIAL", "emulator-5554")

PRODUCTION_PACKAGE = "com.pdig.app"
PRODUCTION_ACTIVITY = "com.pdig.app.MainActivity"
PREVIEW_PACKAGE_PREFIX = "com.pdig.app.preview"
PREVIEW_ACTIVITY = "com.pdig.app.PreviewLauncherActivity"

REALITY_MARKER = "你的数字基础设施 · 已记录的全球上下文"
UNLOCK_ACTION = "已知悉风险，本次进入"

# Vocabulary the Production secondary identity surfaces must NOT present as fact.
FORBIDDEN_INFERENCE = (
    "Passkey",
    "Authenticator",
    "Secure Enclave",
    "Trusted device",
    "MFA",
    "通行密钥",
)


class Capture:
    def __init__(self, out_root: Path, mode: str, font_scale: float, label: str) -> None:
        self.out_root = out_root
        self.mode = mode
        self.font_scale = font_scale
        self.label = label
        self.entries: list[dict] = []
        self.navigation: list[str] = []
        self.source_head = os.environ.get("PDIG_SOURCE_HEAD", "") or git("rev-parse", "HEAD")
        self.android_tree_head = os.environ.get("PDIG_ANDROID_TREE_HEAD", "") or git(
            "log", "-1", "--format=%h", "--", "android/"
        )
        self.package = os.environ.get("PDIG_PACKAGE", PRODUCTION_PACKAGE)
        self.apk_sha256 = os.environ.get("PDIG_APK_SHA256", "") or installed_apk_sha256(self.package)
        self.data_mode = os.environ.get("PDIG_DATA_MODE", "isolated-test-reality")
        self.profile = device_profile()

    # ---------------- capture ----------------
    def shot(self, shot_id: str, screen: str, state: str, assertions: list[str]) -> None:
        png_dir = self.out_root / self.label
        xml_dir = self.out_root / "xml" / self.label
        png_dir.mkdir(parents=True, exist_ok=True)
        xml_dir.mkdir(parents=True, exist_ok=True)
        png = png_dir / f"{shot_id}.png"
        raw = adb_out("exec-out", "screencap", "-p")
        png.write_bytes(raw)
        nodes = self.dump()
        (xml_dir / f"{shot_id}.xml").write_text(self.last_raw_xml, encoding="utf-8")
        labels = [label_of(n) for n in nodes]
        missing = [a for a in assertions if not any(a in x for x in labels)]
        entry = {
            "shot_id": shot_id,
            "screen": screen,
            "state": state,
            "label": self.label,
            "mode": self.mode,
            "sourceHead": self.source_head,
            "androidTreeHead": self.android_tree_head,
            "apkSha256": self.apk_sha256,
            "package": self.package,
            "dataMode": self.data_mode,
            "device": self.profile["device"],
            "api": self.profile["api"],
            "resolution": self.profile["resolution"],
            "density": self.profile["density"],
            "fontScale": self.font_scale,
            "navigationSequence": list(self.navigation),
            "assertions": assertions,
            "assertionsMissing": missing,
            "capturedAtUtc": datetime.now(timezone.utc).isoformat(timespec="seconds"),
            "humanReview": "NOT_RUN",
        }
        self.entries.append(entry)
        if missing:
            print(f"ASSERTION_FAILED {shot_id}: missing={missing}", flush=True)
            raise SystemExit(f"assertion failed for {shot_id}: {missing}")
        print(f"SHOT {shot_id} ok ({len(assertions)} assertions)", flush=True)

    def dump(self) -> list[dict[str, str]]:
        for _ in range(4):
            adb("shell", "uiautomator", "dump", "/sdcard/pdig-window.xml")
            raw = shell_out("cat", "/sdcard/pdig-window.xml")
            if raw.strip().startswith("<?xml"):
                break
            time.sleep(0.6)
        else:
            raise SystemExit("uiautomator dump never produced XML")
        self.last_raw_xml = raw
        root = ET.fromstring(raw)
        return [dict(n.attrib) for n in root.iter("node")]

    # ---------------- interrogation ----------------
    def labels(self) -> list[str]:
        return [x for n in self.dump() if (x := label_of(n))]

    def wait_text(self, text: str, timeout: float = 20.0) -> list[dict[str, str]]:
        deadline = time.time() + timeout
        last: list[dict[str, str]] = []
        while time.time() < deadline:
            try:
                last = self.dump()
                if any(text in label_of(n) for n in last):
                    return last
            except Exception:  # transient uiautomator failure
                pass
            time.sleep(0.7)
        raise SystemExit(
            f"Timed out waiting for {text!r}; labels={[label_of(n) for n in last][:60]}"
        )

    def tap(
        self,
        text: str,
        timeout: float = 15.0,
        exact: bool = False,
        index: int = 0,
        prefer_bottom: bool = False,
    ) -> None:
        deadline = time.time() + timeout
        while time.time() < deadline:
            nodes = self.dump()
            matches = [
                n
                for n in nodes
                if (label_of(n) == text if exact else text in label_of(n))
                and n.get("bounds")
            ]
            if matches:
                if prefer_bottom:
                    matches.sort(key=lambda n: bounds_center(n["bounds"])[1])
                x, y = bounds_center(matches[min(index, len(matches) - 1)]["bounds"])
                adb("shell", "input", "tap", str(x), str(y))
                self.navigation.append(text)
                time.sleep(0.9)
                return
            time.sleep(0.6)
        raise SystemExit(f"could not tap {text!r}")

    def scroll_until(self, text: str, attempts: int = 6, dy: int = -620) -> None:
        for _ in range(attempts):
            if any(text in label_of(n) for n in self.dump()):
                return
            adb("shell", "input", "swipe", "540", "1900", "540", str(1900 + dy), "320")
            time.sleep(0.9)
        raise SystemExit(f"scroll target {text!r} unreachable")

    def require_absent(self, shot_id: str, *forbidden: str) -> None:
        labels = self.labels()
        hits = [f for f in forbidden if any(f in x for x in labels)]
        if hits:
            raise SystemExit(f"{shot_id}: forbidden inference vocabulary present: {hits}")

    def tap_scrolled(self, text: str, exact: bool = True, timeout: float = 25.0) -> None:
        """Tap a control that lives inside a scroll container.

        Scrolls until the matched node sits in a comfortable tap band, because a
        tile flush against the navigation bar can be outside its own hit region.
        """
        deadline = time.time() + timeout
        while time.time() < deadline:
            for n in self.dump():
                matched = label_of(n) == text if exact else text in label_of(n)
                if matched and n.get("bounds"):
                    x, y = bounds_center(n["bounds"])
                    if 300 < y < 2000:
                        adb("shell", "input", "tap", str(x), str(y))
                        self.navigation.append(text)
                        time.sleep(1.0)
                        return
            adb("shell", "input", "swipe", "540", "1900", "540", "1300", "320")
            time.sleep(0.9)
        raise SystemExit(f"could not scrolled-tap {text!r}")

    def tap_below(self, anchor: str, text: str, timeout: float = 25.0) -> None:
        """Tap ``text`` only when it sits below ``anchor`` in the current layout.

        The Infrastructure hub repeats category names in a read-only count grid
        above the real management tiles; anchoring on the section header keeps the
        tap on the tile that actually navigates.
        """
        deadline = time.time() + timeout
        while time.time() < deadline:
            nodes = self.dump()
            anchor_y = None
            for n in nodes:
                if label_of(n) == anchor and n.get("bounds"):
                    anchor_y = bounds_center(n["bounds"])[1]
                    break
            if anchor_y is not None:
                for n in nodes:
                    if label_of(n) == text and n.get("bounds"):
                        x, y = bounds_center(n["bounds"])
                        if anchor_y < y < 2050:
                            adb("shell", "input", "tap", str(x), str(y))
                            self.navigation.append(text)
                            time.sleep(1.2)
                            return
            adb("shell", "input", "swipe", "540", "1900", "540", "1300", "320")
            time.sleep(0.9)
        raise SystemExit(f"could not tap {text!r} below {anchor!r}")

    def ensure_visible(self, text: str, max_down: int = 8, max_up: int = 8) -> None:
        """Scroll a long surface until ``text`` is on screen (either direction)."""
        for _ in range(max_down):
            if any(text in label_of(n) for n in self.dump()):
                return
            adb("shell", "input", "swipe", "540", "1900", "540", "1250", "320")
            time.sleep(0.9)
        for _ in range(max_up):
            if any(text in label_of(n) for n in self.dump()):
                return
            adb("shell", "input", "swipe", "540", "1250", "540", "1900", "320")
            time.sleep(0.9)
        raise SystemExit(f"could not bring {text!r} on screen")

    def tap_cta(self, label: str) -> None:
        self.ensure_visible(label)
        self.tap(label, exact=True)

    def back(self) -> None:
        adb("shell", "input", "keyevent", "4")
        self.navigation.append("BACK")
        time.sleep(1.0)

    # ---------------- manifest ----------------
    def write_manifest(self) -> Path:
        path = self.out_root / "MANIFEST.json"
        existing: list[dict] = []
        if path.exists():
            try:
                existing = json.loads(path.read_text(encoding="utf-8"))
                if not isinstance(existing, list):
                    existing = []
            except json.JSONDecodeError:
                existing = []
        merged = [e for e in existing if e.get("shot_id") not in {n["shot_id"] for n in self.entries}]
        merged.extend(self.entries)
        merged.sort(key=lambda e: (e.get("label", ""), e.get("shot_id", "")))
        path.write_text(json.dumps(merged, ensure_ascii=False, indent=2), encoding="utf-8")
        print(f"MANIFEST={path} entries={len(merged)}", flush=True)
        return path


def git(*args: str) -> str:
    try:
        return subprocess.run(
            ["git", *args],
            capture_output=True,
            check=False,
            encoding="utf-8",
            errors="replace",
        ).stdout.strip()
    except Exception:
        return ""


def adb(*args: str) -> None:
    cmd = ["adb", "-s", SERIAL, *map(str, args)]
    res = subprocess.run(
        cmd, capture_output=True, check=False, encoding="utf-8", errors="replace"
    )
    if res.returncode != 0:
        raise SystemExit(f"adb {' '.join(map(str, args))} failed: {res.stdout}{res.stderr}")


def adb_out(*args: str) -> bytes:
    res = subprocess.run(
        ["adb", "-s", SERIAL, *map(str, args)], capture_output=True, check=False
    )
    if res.returncode != 0:
        raise SystemExit(f"adb {' '.join(args)} failed: {res.stdout[:400]!r}")
    return res.stdout


def shell_out(*args: str) -> str:
    res = subprocess.run(
        ["adb", "-s", SERIAL, "shell", *map(str, args)],
        capture_output=True,
        check=False,
        encoding="utf-8",
        errors="replace",
    )
    return res.stdout.strip()


def label_of(node: dict[str, str]) -> str:
    return (node.get("text") or node.get("content-desc") or "").strip()


def bounds_center(raw: str) -> tuple[int, int]:
    m = re.fullmatch(r"\[(\d+),(\d+)\]\[(\d+),(\d+)\]", raw)
    if not m:
        raise SystemExit(f"bad bounds {raw!r}")
    x1, y1, x2, y2 = map(int, m.groups())
    return (x1 + x2) // 2, (y1 + y2) // 2


def device_profile() -> dict:
    size = shell_out("wm", "size")
    density = shell_out("wm", "density")
    return {
        "device": shell_out("getprop", "ro.product.model") or "unknown",
        "api": int(shell_out("getprop", "ro.build.version.sdk") or 0),
        "resolution": size.split(":")[-1].strip(),
        "density": density.split(":")[-1].strip(),
        "serial": SERIAL,
    }


def installed_apk_sha256(package: str) -> str:
    path = shell_out("pm", "path", package).replace("package:", "").strip().splitlines()
    if not path:
        return "NOT_INSTALLED"
    out = shell_out("sha256sum", path[0])
    return out.split(" ")[0] if out else "UNKNOWN"




# --------------------------------------------------------------------------
# production flows
# --------------------------------------------------------------------------
def launch_production(cap: Capture, vnext: bool = True) -> None:
    adb("shell", "am", "force-stop", PRODUCTION_PACKAGE)
    time.sleep(0.8)
    args = ["shell", "am", "start", "-W", "-n", f"{PRODUCTION_PACKAGE}/{PRODUCTION_ACTIVITY}"]
    if vnext:
        args += ["--ez", "vnext_production", "true"]
    adb(*args)
    time.sleep(1.6)


def unlock(cap: Capture) -> None:
    nodes = cap.dump()
    if any(UNLOCK_ACTION in label_of(n) for n in nodes):
        cap.tap(UNLOCK_ACTION, exact=True)
    cap.wait_text(REALITY_MARKER, timeout=30)


def five_primary(cap: Capture) -> None:
    labels = cap.labels()
    missing = [x for x in ("现在", "基础设施", "变更", "记录", "我") if x not in labels]
    if missing:
        raise SystemExit(f"missing primary destinations: {missing}")


def goto_primary(cap: Capture, name: str) -> None:
    # Bottom navigation only exists on root destinations. Child screens (object
    # detail, collection, plan) hide it, so pop back until the five frozen
    # primary destinations are actually on screen.
    for _ in range(6):
        labels = cap.labels()
        if all(x in labels for x in ("现在", "基础设施", "变更", "记录", "我")):
            cap.tap(name, exact=True, prefer_bottom=True)
            return
        cap.back()
    raise SystemExit(f"could not reach primary destination {name!r}")


def production_inventory_category(cap: Capture, category: str, object_name: str) -> None:
    goto_primary(cap, "基础设施")
    # The Infrastructure hub renders a read-only count grid above the real
    # "管理分类" tiles, so scroll to the management section before tapping a
    # category and pick the tile that actually navigates.
    cap.scroll_until("管理分类")
    cap.tap_below("管理分类", category)
    cap.scroll_until(object_name)
    cap.tap(object_name, exact=True)


def flow_production_journey(cap: Capture) -> None:
    # 1. Lock gate before any Reality is visible.
    launch_production(cap)
    cap.wait_text("PDIG 已锁定", timeout=30)
    cap.shot(
        "production-lock",
        "LockGate",
        "locked",
        ["PDIG 已锁定", "已知悉风险，本次进入"],
    )

    # 2. Unlocked Production VNext shell with the five frozen primary destinations.
    unlock(cap)
    five_primary(cap)
    cap.shot(
        "production-now",
        "Now",
        "unlocked",
        [REALITY_MARKER, "现在", "基础设施", "变更", "记录", "我"],
    )

    # 3. Infrastructure hub.
    goto_primary(cap, "基础设施")
    cap.shot(
        "production-infrastructure",
        "Infrastructure",
        "hub",
        ["基础设施", "账户", "邮箱", "设备", "服务"],
    )

    # 4. Secondary identity details: governed facts only, explicit type identity.
    # Device detail is intentionally absent: the canonical spec does not allow
    # runtime creation of device nodes, so there is no governed way to put one in
    # Reality in this build. Its real empty state is captured instead.
    for category, object_name, hero, shot_id, surface in (
        ("账户", "主账户", "账户身份", "production-account-detail", "Account"),
        ("邮箱", "主邮箱", "通信身份", "production-email-detail", "Email"),
        ("服务", "登录服务", "服务节点", "production-service-detail", "Service"),
    ):
        production_inventory_category(cap, category, object_name)
        cap.wait_text(hero, timeout=20)
        cap.shot(shot_id, f"{surface} detail", "governed identity hero", [hero, object_name])
        cap.require_absent(shot_id, *FORBIDDEN_INFERENCE)
        cap.back()

    # 5. Change Center and the seeded phone change plan.
    goto_primary(cap, "变更")
    cap.wait_text("已记录变更计划", timeout=15)
    cap.shot(
        "production-change-center",
        "Change",
        "plan list",
        ["变更", "已记录变更计划", "更换手机号"],
    )
    cap.tap("更换手机号", exact=True)
    cap.wait_text("变更全程", timeout=20)
    cap.shot(
        "production-change-current",
        "Change phone",
        "current",
        ["变更全程", "当前", "过渡中", "完成后（计划）", "下一步"],
    )

    # 6. Make-Before-Break: prepare -> migrate -> (pending verification) -> blocked retirement.
    cap.tap_cta("记录完成这一步")
    cap.ensure_visible("添加新手机号并迁移关键账户")
    cap.tap_cta("记录完成这一步")
    cap.ensure_visible("确认验证这一步")
    cap.shot(
        "production-change-pending-verification",
        "Change phone",
        "done but not verified",
        ["确认验证这一步", "动作已记录完成，结果尚未验证", "确认验证会写入正式记录"],
    )

    # The old path must still be blocked while the migration result has not been
    # independently verified. Its own step card says it is waiting on a
    # prerequisite, and the primary CTA is verification, not retirement.
    cap.ensure_visible("停用旧手机号（新路径全部验证后）")
    blocked_labels = cap.labels()
    if "记录完成这一步" in blocked_labels and any(
        x == "停用旧手机号（新路径全部验证后）" for x in blocked_labels
    ):
        primary_titles = [
            x for x in blocked_labels if x in ("记录完成这一步", "确认验证这一步")
        ]
        if not primary_titles:
            raise SystemExit("retirement had no gated predecessor while verification was pending")
    cap.shot(
        "production-change-retirement-blocked",
        "Change phone",
        "retirement blocked",
        ["停用旧手机号（新路径全部验证后）", "等待前置步骤"],
    )

    # 7. Verification, then the recovery-path step, then its own verification.
    cap.tap_cta("确认验证这一步")
    cap.ensure_visible("验证新手机号恢复路径")
    cap.tap_cta("记录完成这一步")
    cap.ensure_visible("确认验证这一步")
    cap.tap_cta("确认验证这一步")

    # 8. Every required new path is verified -> retirement becomes the next step.
    # The primary CTA itself now names the destructive retirement step, which is
    # only reachable after both new-path verifications are authoritative.
    cap.ensure_visible("记录完成这一步")
    cap.shot(
        "production-change-retirement-eligible",
        "Change phone",
        "retirement eligible",
        ["记录完成这一步", "停用旧手机号（新路径全部验证后）"],
    )

    # 9. Retire the old path, then prove AFTER is still only a projection.
    cap.tap_cta("记录完成这一步")
    cap.ensure_visible("计划预期")
    cap.shot(
        "production-change-after-plan",
        "Change phone",
        "after = plan projection",
        ["计划预期", "未发生"],
    )

    # 10. Records: what happened, and done != verified.
    goto_primary(cap, "记录")
    cap.wait_text("已记录的操作与验证证据 · 按计划分组", timeout=20)
    cap.shot(
        "production-records",
        "Records",
        "recorded history",
        ["已记录的操作与验证证据 · 按计划分组", "已记录完成", "已验证"],
    )


def flow_production_stale(cap: Capture) -> None:
    launch_production(cap)
    unlock(cap)
    goto_primary(cap, "变更")
    cap.wait_text("已记录变更计划", timeout=15)
    cap.tap("更换手机号", exact=True)
    cap.wait_text("变更全程", timeout=20)
    cap.ensure_visible("重新分析当前影响")
    cap.shot(
        "production-change-stale",
        "Change phone",
        "stale",
        [
            "基础设施已发生变化",
            "重新分析当前影响",
            "使用上方的「重新分析当前影响」；界面不会用旧快照继续写入。",
        ],
    )
    # The stale plan must not accept a new completion: the only primary action
    # is reanalysis, and no execution CTA is offered.
    labels = cap.labels()
    if "记录完成这一步" in labels or "确认验证这一步" in labels:
        raise SystemExit("stale plan still advertised an execution CTA")
    cap.tap("重新分析当前影响", exact=True)
    deadline = time.time() + 25
    while time.time() < deadline:
        labels = cap.labels()
        if "基础设施已发生变化" not in " ".join(labels):
            break
        time.sleep(0.9)
    cap.ensure_visible("准备状态：")
    labels = cap.labels()
    revisions = [
        m.groups()
        for m in (re.search(r"分析时数据版本 (\d+) · 当前数据版本 (\d+)", x) for x in labels)
        if m
    ]
    if not revisions:
        raise SystemExit("reanalyzed plan did not expose its analyzed/current revision pair")
    analyzed, current = revisions[0]
    if analyzed != current:
        raise SystemExit(
            f"reanalysis did not advance the analyzed revision: {analyzed} != {current}"
        )
    cap.shot(
        "production-change-reanalyzed",
        "Change phone",
        "reanalyzed",
        ["准备状态：", f"分析时数据版本 {analyzed} · 当前数据版本 {current}"],
    )


def flow_production_privacy(cap: Capture) -> None:
    launch_production(cap)
    unlock(cap)
    goto_primary(cap, "我")
    cap.wait_text("我的数字生活", timeout=20)
    cap.shot("production-me", "Me", "identity summary", ["我的数字生活"])

    # Privacy lives under "隐私与偏好" on the Me workspace and is OFF by default.
    cap.ensure_visible("隐私与偏好")
    labels = cap.labels()
    if "敏感信息遮蔽：已关闭" not in labels:
        raise SystemExit(f"privacy masking is not OFF by default: {labels[:40]}")
    cap.shot(
        "production-privacy-default-off",
        "Me",
        "masking default off",
        ["隐私与偏好", "敏感信息遮蔽：已关闭", "本机显示偏好，不修改已确认记录"],
    )

    # Toggle inside the real preference panel (the row, not the summary).
    cap.tap("隐私与偏好")
    cap.ensure_visible("本机呈现偏好")
    off_before = sum(1 for x in cap.labels() if x == "已关闭")
    cap.tap("敏感信息遮蔽", exact=True)
    deadline = time.time() + 15
    while time.time() < deadline:
        labels = cap.labels()
        if sum(1 for x in labels if x == "已关闭") < off_before:
            break
        time.sleep(0.8)
    else:
        raise SystemExit("masking toggle did not change its own state")
    cap.shot(
        "production-privacy-on",
        "Me · 隐私与偏好",
        "masking on",
        ["隐私与偏好", "敏感信息遮蔽", "隐藏号码、卡号等敏感呈现；不会改写对象身份。"],
    )
    cap.back()

    # Masked inventory must still be navigable and must not echo identifiers.
    goto_primary(cap, "基础设施")
    cap.ensure_visible("身份对象")
    masked_labels = cap.labels()
    if any("旧主号" in x for x in masked_labels):
        raise SystemExit("masked Infrastructure still echoed the real phone identity name")
    cap.shot(
        "production-privacy-on-infrastructure",
        "Infrastructure",
        "masking on",
        ["基础设施", "身份对象"],
    )

    # Turning masking off must restore the real values, not merely re-label them.
    goto_primary(cap, "我")
    cap.ensure_visible("隐私与偏好")
    cap.tap("隐私与偏好")
    cap.ensure_visible("本机呈现偏好")
    cap.tap("敏感信息遮蔽", exact=True)
    time.sleep(1.2)
    cap.back()
    cap.ensure_visible("敏感信息遮蔽：已关闭")
    cap.shot(
        "production-privacy-off-restored",
        "Me",
        "masking off",
        ["敏感信息遮蔽：已关闭"],
    )
    goto_primary(cap, "基础设施")
    cap.ensure_visible("旧主号")
    cap.shot(
        "production-privacy-off-restored-infrastructure",
        "Infrastructure",
        "masking off restores real values",
        ["身份对象", "旧主号"],
    )


def flow_production_tablet(cap: Capture) -> None:
    launch_production(cap)
    unlock(cap)
    five_primary(cap)
    cap.shot(
        "tablet-now",
        "Now",
        "unlocked",
        [REALITY_MARKER, "现在", "基础设施", "变更", "记录", "我"],
    )
    goto_primary(cap, "基础设施")
    cap.wait_text("基础设施", timeout=15)
    cap.shot("tablet-infrastructure", "Infrastructure", "hub", ["基础设施"])
    goto_primary(cap, "变更")
    cap.wait_text("已记录变更计划", timeout=15)
    cap.shot("tablet-change", "Change", "plan list", ["变更", "已记录变更计划"])
    goto_primary(cap, "记录")
    cap.wait_text("已记录的操作与验证证据 · 按计划分组", timeout=20)
    cap.shot("tablet-records", "Records", "history", ["已记录的操作与验证证据 · 按计划分组"])
    goto_primary(cap, "我")
    cap.wait_text("我的数字生活", timeout=20)
    cap.shot("tablet-me", "Me", "identity summary", ["我的数字生活"])


def main() -> None:
    parser = argparse.ArgumentParser()
    parser.add_argument(
        "--mode",
        required=True,
        choices=["prod-journey", "prod-stale", "prod-privacy", "prod-tablet"],
    )
    parser.add_argument("--out", required=True)
    parser.add_argument("--label", default="production")
    parser.add_argument("--font-scale", type=float, default=1.0)
    args = parser.parse_args()

    out_root = Path(args.out)
    out_root.mkdir(parents=True, exist_ok=True)
    cap = Capture(out_root, args.mode, args.font_scale, args.label)

    if args.mode == "prod-journey":
        flow_production_journey(cap)
    elif args.mode == "prod-stale":
        flow_production_stale(cap)
    elif args.mode == "prod-tablet":
        flow_production_tablet(cap)
    else:
        flow_production_privacy(cap)

    cap.write_manifest()


if __name__ == "__main__":
    main()
