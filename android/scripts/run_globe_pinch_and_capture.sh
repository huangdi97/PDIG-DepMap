#!/usr/bin/env bash
# Single Bash process; emulator-runner otherwise executes every script line
# under POSIX /bin/sh where Bash SHA slicing and shell variables fail.
set -euo pipefail
APK="android/app/build/outputs/apk/preview/debug/app-preview-debug.apk"
TEST_APK="android/app/build/outputs/apk/androidTest/preview/debug/app-preview-debug-androidTest.apk"
APP_ID="com.pdig.app.preview.p${GITHUB_SHA:0:7}"
EVIDENCE="artifacts/runtime-evidence/preview-phone"
mkdir -p "$EVIDENCE"
adb install -r "$APK"
adb install -r "$TEST_APK"
echo "GLOBE_REAL_TWO_FINGER_PINCH_SOURCE=$GITHUB_SHA"
adb shell am instrument -w \
  -e class com.pdig.uivnext.evidence.AndroidGlobeGestureContractTest \
  "${APP_ID}.test/androidx.test.runner.AndroidJUnitRunner" \
  | tee "$EVIDENCE/pinch-gesture-instrumentation.txt"
grep -Eq 'OK \(1 test\)' "$EVIDENCE/pinch-gesture-instrumentation.txt"
echo "GLOBE_REAL_TWO_FINGER_PINCH=PASS"
python3 android/scripts/ui_vnext_pixel_capture.py
