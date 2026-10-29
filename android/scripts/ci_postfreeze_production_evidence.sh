#!/usr/bin/env bash
# Exact-head post-freeze Production VNext evidence capture.
#
# Runs only inside the GitHub Actions API36 emulator after the two-key
# productionDebug + androidTest APKs have been built. The isolated Reality is
# seeded through PostFreezeEvidenceSeedTest/AppContainer, never through Preview
# fixtures. This script does not and cannot approve Human Visual Acceptance.
set -euo pipefail

ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/../.." && pwd)"
cd "$ROOT"

SOURCE_HEAD="${PDIG_EVIDENCE_SOURCE_HEAD:?PDIG_EVIDENCE_SOURCE_HEAD is required}"
EVIDENCE_LABEL="${PDIG_EVIDENCE_LABEL:-phone}"
FONT_SCALE="${PDIG_EVIDENCE_FONT_SCALE:-1.0}"
APK="$ROOT/android/app/build/outputs/apk/production/debug/app-production-debug.apk"
TEST_APK="$(find "$ROOT/android/app/build/outputs/apk/androidTest" -type f -name '*production*debug*androidTest*.apk' -print -quit)"
OUT="$ROOT/artifacts/runtime-evidence/postfreeze-production-${SOURCE_HEAD}"
mkdir -p "$OUT/seed-logs"

test -f "$APK"
test -n "$TEST_APK"
test -f "$TEST_APK"

adb uninstall com.pdig.app >/dev/null 2>&1 || true
adb install "$APK"
adb install "$TEST_APK"

# Capture the same authoritative Production surfaces under the requested
# system font scale. A fresh app launch after this setting observes the new
# configuration; the capture harness records the measured device value.
adb shell settings put system font_scale "$FONT_SCALE"
MEASURED_FONT_SCALE="$(adb shell settings get system font_scale | tr -d '\r')"
echo "requested_font_scale=$FONT_SCALE"
echo "measured_font_scale=$MEASURED_FONT_SCALE"

INSTRUMENTATION="$(adb shell pm list instrumentation | tr -d '\r' | awk '/target=com.pdig.app/ {sub(/^instrumentation:/,"",$1); print $1; exit}')"
if [[ -z "$INSTRUMENTATION" ]]; then
  echo "No instrumentation runner targeting com.pdig.app"
  exit 1
fi
echo "instrumentation=$INSTRUMENTATION"

run_seed() {
  local method="$1"
  local log="$OUT/seed-logs/${method}.txt"
  adb shell am force-stop com.pdig.app || true
  set +e
  adb shell am instrument -w \
    -e class "com.pdig.uivnext.evidence.PostFreezeEvidenceSeedTest#${method}" \
    "$INSTRUMENTATION" | tee "$log"
  local instrument_status=${PIPESTATUS[0]}
  set -e
  if [[ "$instrument_status" -ne 0 ]] || ! grep -q 'OK (1 test)' "$log"; then
    echo "Seed instrumentation did not report OK (1 test): $method"
    exit 1
  fi
}

export PDIG_SOURCE_HEAD="$SOURCE_HEAD"
export PDIG_ANDROID_TREE_HEAD="$(git log -1 --format=%h -- android/)"
export PDIG_PACKAGE=com.pdig.app
export PDIG_APK_SHA256="$(sha256sum "$APK" | awk '{print $1}')"
export PDIG_DATA_MODE=isolated-postfreeze-evidence-reality
# This workflow builds productionDebug with BOTH explicit VNext cutover keys.
# Capture through the no-extra default launcher so Human Review sees the same
# route topology as a cutover candidate, not the debug-only rehearsal override.
export PDIG_PRODUCTION_DEFAULT=true

# Preserve the exact two-key Production VNext APK used by this evidence run as
# a downloadable human-review candidate. This is debug/non-production signed,
# never a Store Release artifact, and does not change release routing.
mkdir -p "$OUT/apk"
REVIEW_APK="$OUT/apk/pdig-production-vnext-human-review.apk"
cp "$APK" "$REVIEW_APK"
(
  cd "$OUT/apk"
  sha256sum pdig-production-vnext-human-review.apk > SHA256SUMS.txt
)
{
  echo "source_head=$SOURCE_HEAD"
  echo "package=com.pdig.app"
  echo "variant=productionDebug"
  echo "ui_generation=vnext"
  echo "cutover_approved=true"
  echo "signing=debug/non-production"
  echo "evidence_label=$EVIDENCE_LABEL"
  echo "font_scale=$FONT_SCALE"
  echo "human_review=NOT_RUN"
  echo "store_release=HOLD"
} > "$OUT/apk/REVIEW_CANDIDATE.txt"

run_seed seedPhoneMakeBeforeBreakJourney
python3 android/scripts/postfreeze_closure_capture.py \
  --mode prod-journey \
  --out "$OUT" \
  --label "$EVIDENCE_LABEL" \
  --font-scale "$FONT_SCALE"

run_seed seedStalePhoneChange
python3 android/scripts/postfreeze_closure_capture.py \
  --mode prod-stale \
  --out "$OUT" \
  --label "${EVIDENCE_LABEL}-stale-reanalysis" \
  --font-scale "$FONT_SCALE"

python3 - <<'PY'
import json
import os
from pathlib import Path

head = os.environ["PDIG_SOURCE_HEAD"]
root = Path("artifacts/runtime-evidence") / f"postfreeze-production-{head}"
manifest = root / "MANIFEST.json"
entries = json.loads(manifest.read_text(encoding="utf-8"))
by_id = {entry["shot_id"]: entry for entry in entries}

expected = {
    "production-device-empty",
    "production-account-detail-change-boundary",
    "production-email-detail-change-boundary",
    "production-service-detail-change-boundary",
    "production-records",
    "production-records-trace",
    "production-change-stale",
    "production-change-reanalyzed",
}
missing = sorted(expected - set(by_id))
if missing:
    raise SystemExit(f"missing expected post-freeze evidence entries: {missing}")

apk_sha = os.environ["PDIG_APK_SHA256"]
expected_font_scale = float(os.environ.get("PDIG_EVIDENCE_FONT_SCALE", "1.0"))
for shot_id in sorted(expected):
    entry = by_id[shot_id]
    if entry.get("sourceHead") != head:
        raise SystemExit(
            f"{shot_id}: sourceHead mismatch {entry.get('sourceHead')} != {head}"
        )
    if entry.get("apkSha256") != apk_sha:
        raise SystemExit(f"{shot_id}: APK hash mismatch")
    if abs(float(entry.get("fontScale", -1.0)) - expected_font_scale) > 0.01:
        raise SystemExit(
            f"{shot_id}: font scale mismatch "
            f"{entry.get('fontScale')} != {expected_font_scale}"
        )
    if entry.get("humanReview") != "NOT_RUN":
        raise SystemExit(
            f"{shot_id}: evidence harness must not self-approve human review"
        )
    if entry.get("assertionsMissing"):
        raise SystemExit(
            f"{shot_id}: missing assertions {entry['assertionsMissing']}"
        )

secure_expected = {
    "production-device-empty",
    "production-account-detail-change-boundary",
    "production-email-detail-change-boundary",
    "production-service-detail-change-boundary",
    "production-records",
    "production-records-trace",
    "production-change-stale",
    "production-change-reanalyzed",
}
for shot_id in sorted(secure_expected):
    if by_id[shot_id].get("secureWindowExpected") is not True:
        raise SystemExit(f"{shot_id}: FLAG_SECURE expectation missing from manifest")

for shot_id in ("production-lock", "production-now", "production-change-center"):
    if by_id[shot_id].get("secureWindowExpected") is not False:
        raise SystemExit(f"{shot_id}: non-sensitive root unexpectedly marked secure")

# XML is the semantic/layout proof for FLAG_SECURE surfaces. Require the newly
# changed controls to sit fully inside the physical screen rather than merely
# touching the bottom edge.
import re
import xml.etree.ElementTree as ET

layout_targets = {
    "production-account-detail-change-boundary": (
        "此对象暂不能直接创建变更计划",
        "账户变更尚未开放正式可执行模板；不会用通用步骤替代真实变更语义。",
        "查看变更中心 →",
    ),
    "production-email-detail-change-boundary": (
        "此对象暂不能直接创建变更计划",
        "邮箱更换尚未开放正式可执行模板；恢复与认证影响不会由界面自行猜测。",
        "查看变更中心 →",
    ),
    "production-service-detail-change-boundary": (
        "此对象暂不能直接创建变更计划",
        "服务迁移尚未开放正式可执行模板；不会把关系数量当作迁移计划。",
        "查看变更中心 →",
    ),
    "production-change-reanalyzed": (
        "重新分析完成",
        "这里只比较重新分析前后的正式影响投影",
    ),
}

def node_label(node):
    return (node.attrib.get("text") or node.attrib.get("content-desc") or "").strip()

for shot_id, targets in layout_targets.items():
    entry = by_id[shot_id]
    width, height = map(int, entry["resolution"].lower().split("x"))
    xml_path = root / "xml" / entry["label"] / f"{shot_id}.xml"
    tree = ET.parse(xml_path)
    nodes = list(tree.getroot().iter("node"))
    for target in targets:
        matches = [n for n in nodes if target in node_label(n)]
        if not matches:
            raise SystemExit(f"{shot_id}: XML missing layout target {target!r}")
        raw = matches[0].attrib.get("bounds", "")
        match = re.fullmatch(r"\[(\d+),(\d+)\]\[(\d+),(\d+)\]", raw)
        if not match:
            raise SystemExit(f"{shot_id}: invalid bounds for {target!r}: {raw!r}")
        x1, y1, x2, y2 = map(int, match.groups())
        safe_top = 120
        safe_bottom = height - 80
        if not (
            0 <= x1 < x2 <= width and
            safe_top <= y1 < y2 <= safe_bottom
        ):
            raise SystemExit(
                f"{shot_id}: {target!r} is not safely framed: "
                f"{raw} within {width}x{height} "
                f"(safe vertical band {safe_top}..{safe_bottom})"
            )

print(f"POSTFREEZE_EVIDENCE_MANIFEST=PASS entries={len(entries)}")
print("POSTFREEZE_LAYOUT_BOUNDS=PASS")
print(f"POSTFREEZE_EVIDENCE_HEAD={head}")
print(f"POSTFREEZE_EVIDENCE_APK_SHA256={apk_sha}")
print(f"POSTFREEZE_EVIDENCE_FONT_SCALE={expected_font_scale}")
PY
