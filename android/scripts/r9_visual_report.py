#!/usr/bin/env python3
"""Human-review evidence, not an automatic artistic-parity certification.

Makes full-screen, exact-SHA real-emulator contact sheets. The bundled concept
board is a 320x213 visual-direction thumbnail, NOT a pixel golden.
"""
import json
import os
from pathlib import Path
from PIL import Image, ImageDraw

ROOT = Path("artifacts/runtime-evidence/preview-phone")
REF = Path("spec/ui-vnext/references/android/PDIG_ANDROID_LIGHT_VISUAL_REFERENCE_2026-10-05.jpg")
ROUTES = [
    ("01-now", "现在 / Globe"),
    ("02-infrastructure", "基础设施 / Hub"),
    ("02a-infrastructure-selected-us", "地区选中 / Context"),
    ("03-cards", "银行卡 / Issuer cards"),
    ("04-card-detail", "卡片详情 / Object"),
    ("04e-card-studio", "卡面定制 / Studio"),
    ("05-change", "变更 / Transition"),
    ("05b-change-plan", "变更 / Plan Only"),
    ("06-records", "记录 / Timeline"),
    ("07-accounts", "账户 / Recovery"),
    ("07-weaknesses", "薄弱点 / Unknown"),
    ("08-numbers", "号码 / Identity"),
    ("08b-number-detail", "号码详情 / Recovery"),
    ("08c-number-studio", "号码定制 / Studio"),
]
W, H, TITLE = 280, 612, 23

def contact_sheet():
    found = [(name, label, ROOT / (name + ".png")) for name, label in ROUTES
             if (ROOT / (name + ".png")).is_file()]
    if not found:
        return []
    columns = 4
    rows = (len(found) + columns - 1) // columns
    canvas = Image.new("RGB", (W * columns, H * rows), "#E7EFF9")
    draw = ImageDraw.Draw(canvas)
    for i, (name, label, path) in enumerate(found):
        with Image.open(path) as src:
            im = src.convert("RGB")
            im.thumbnail((W - 10, H - TITLE - 9), Image.Resampling.LANCZOS)
            x = (i % columns) * W + (W - im.width) // 2
            y = (i // columns) * H + TITLE + 1
            canvas.paste(im, (x, y))
            draw.text(((i % columns) * W + 7, (i // columns) * H + 5),
                      name + " " + label, fill="#112646")
    canvas.save(ROOT / "R9_ACTUAL_RUNTIME_CONTACT.jpg", quality=91, optimize=True)
    return [(name, label, str(path.name)) for name, label, path in found]

def main():
    ROOT.mkdir(parents=True, exist_ok=True)
    sha = os.environ.get("GITHUB_SHA", "UNKNOWN")
    found = contact_sheet()
    summary = {
        "sourceHead": sha,
        "sourceType": "installed previewDebug emulator screenshots",
        "screensCaptured": len(found),
        "referenceAsset": str(REF),
        "referenceIsPixelGolden": False,
        "humanVisualParity": "HOLD",
        "automaticVisualAcceptance": False,
    }
    lum_file = ROOT / "01-now-globe-visual-metrics.json"
    if lum_file.exists():
        summary["mechanicalGlobeProbe"] = json.loads(lum_file.read_text(encoding="utf-8"))
    (ROOT / "R9_VISUAL_REVIEW_DATA.json").write_text(
        json.dumps(summary, ensure_ascii=False, indent=2), encoding="utf-8")
    message = [
        "# R9 exact-SHA actual pixel review",
        "",
        "Source: " + sha,
        "",
        "The contact sheet is reconstructed from actual installed APK screenshots.",
        "Only screenshot geometry and mechanical globe luma are automatically checked.",
        "The compressed 320×213 visual-direction JPG is **not** a pixel-matching golden.",
        "**Human Visual Acceptance = HOLD** until the actual screenshot is reviewed against",
        "the user's full-resolution reference board. No unreviewed rebaseline is allowed.",
        "",
        "## Screenshot index",
        "",
    ]
    message.extend(["- " + n + " — " + label for n, label, _ in found])
    message.extend(["", "Uncaptured routes are not certified by this contact sheet.", ""])
    (ROOT / "R9_VISUAL_REVIEW.md").write_text("\n".join(message), encoding="utf-8")
    print("R9_VISUAL_REPORT_CREATED", summary, flush=True)

if __name__ == "__main__":
    main()
