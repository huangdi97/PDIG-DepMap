#!/usr/bin/env bash
# Real encrypted-Reality Production VNext emulator rehearsal.
# This script has no authority to enable the shipping release by itself.
set -euo pipefail
ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/../.." && pwd)"
cd "$ROOT"
mkdir -p artifacts/runtime-evidence/production-vnext-rehearsal
{
  echo "source_sha=${GITHUB_SHA:-unknown}"
  adb shell getprop ro.build.version.sdk
  adb shell wm size
  adb shell wm density
} > artifacts/runtime-evidence/production-vnext-rehearsal/device-profile.txt

adb install -r android/app/build/outputs/apk/production/debug/app-production-debug.apk

# Production-source shell and authoritative manual relationship contracts.
# Preview synthetic fixtures cannot be used as Personal Reality here.
(
  cd android
  ./gradlew :app:connectedProductionDebugAndroidTest --no-daemon --stacktrace \
    -Pandroid.testInstrumentationRunnerArguments.class=com.pdig.uivnext.evidence.ProductionVNextShellContractTest
  ./gradlew :app:connectedProductionDebugAndroidTest --no-daemon --stacktrace \
    -Pandroid.testInstrumentationRunnerArguments.class=com.pdig.uivnext.evidence.ProductionManualRelationshipContractTest
)

# AGP uninstalls both APKs after every connectedAndroidTest run, so the app is NOT on the device
# at this point. Without an explicit reinstall the rehearsal dies immediately with
#   Error: Activity class {com.pdig.app/com.pdig.app.MainActivity} does not exist.
# (`am start -W` exits 1), so the lock-gated real-Reality path is never actually proven.
adb install -r android/app/build/outputs/apk/production/debug/app-production-debug.apk

# Repository-default ProductionDebug still opens lock-gated LEGACY.
python3 android/scripts/production_vnext_secure_rehearsal.py

# Exercise only the debug rehearsal variant using BOTH explicit build-time keys.
# This MUST NOT mutate ProductionRelease's default or merge the Draft PR.
(
  cd android
  ./gradlew :app:assembleProductionDebug --no-daemon --stacktrace \
    -PpdigProductionUiGeneration=vnext \
    -PpdigProductionVNextCutoverApproved=true
)
adb install -r android/app/build/outputs/apk/production/debug/app-production-debug.apk
python3 android/scripts/production_vnext_release_default_rehearsal.py
