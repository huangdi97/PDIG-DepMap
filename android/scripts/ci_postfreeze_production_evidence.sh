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

run_seed seedPhoneMakeBeforeBreakJourney
python3 android/scripts/postfreeze_closure_capture.py \
  --mode prod-journey \
  --out "$OUT" \
  --label phone

run_seed seedStalePhoneChange
python3 android/scripts/postfreeze_closure_capture.py \
  --mode prod-stale \
  --out "$OUT" \
  --label stale-reanalysis

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
    "production-account-detail-change-boundary",
    "production-email-detail-change-boundary",
    "production-service-detail-change-boundary",
    "production-records",
    "production-change-stale",
    "production-change-reanalyzed",
}
missing = sorted(expected - set(by_id))
if missing:
    raise SystemExit(f"missing expected post-freeze evidence entries: {missing}")

apk_sha = os.environ["PDIG_APK_SHA256"]
for shot_id in sorted(expected):
    entry = by_id[shot_id]
    if entry.get("sourceHead") != head:
        raise SystemExit(
            f"{shot_id}: sourceHead mismatch {entry.get('sourceHead')} != {head}"
        )
    if entry.get("apkSha256") != apk_sha:
        raise SystemExit(f"{shot_id}: APK hash mismatch")
    if entry.get("humanReview") != "NOT_RUN":
        raise SystemExit(
            f"{shot_id}: evidence harness must not self-approve human review"
        )
    if entry.get("assertionsMissing"):
        raise SystemExit(
            f"{shot_id}: missing assertions {entry['assertionsMissing']}"
        )

records = by_id["production-records"]
if records.get("secureWindowExpected") is not True:
    raise SystemExit("Production Records evidence must retain FLAG_SECURE expectation")

print(f"POSTFREEZE_EVIDENCE_MANIFEST=PASS entries={len(entries)}")
print(f"POSTFREEZE_EVIDENCE_HEAD={head}")
print(f"POSTFREEZE_EVIDENCE_APK_SHA256={apk_sha}")
PY
