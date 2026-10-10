#!/usr/bin/env bash
# Exact-head API36 tablet runtime proof. Called as ONE bash process because
# reactivecircus/android-emulator-runner executes individual inline script lines
# in separate /usr/bin/sh -c contexts.
set -euo pipefail
ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/../.." && pwd)"
cd "$ROOT"

adb shell settings put system accelerometer_rotation 0
adb shell settings put system user_rotation 1
adb shell wm size 1920x1200
adb shell wm density 240
sleep 3

mkdir -p artifacts/runtime-evidence/preview-tablet
{
  echo "source_sha=${GITHUB_SHA:-unknown}"
  adb shell wm size
  adb shell wm density
  adb shell dumpsys display | grep -E 'mCurrentDisplayRect|DisplayInfo|logicalWidth|logicalHeight' | head -40 || true
} > artifacts/runtime-evidence/preview-tablet/device-profile.txt

(
  cd android
  ./gradlew :app:connectedPreviewDebugAndroidTest --no-daemon --stacktrace \
    -Pandroid.testInstrumentationRunnerArguments.class=com.pdig.uivnext.evidence.TabletAdaptiveContractTest
)

adb shell rm -rf /sdcard/Download/ui-shots || true
(
  cd android
  ./gradlew :app:connectedPreviewDebugAndroidTest --no-daemon --stacktrace \
    -Pandroid.testInstrumentationRunnerArguments.class=com.pdig.uivnext.evidence.SourceCompleteScreenshotEvidenceTest
)

adb pull /sdcard/Download/ui-shots artifacts/runtime-evidence/preview-tablet/ui-shots
find artifacts/runtime-evidence/preview-tablet -maxdepth 3 -type f -print | sort
