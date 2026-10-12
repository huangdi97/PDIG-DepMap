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
# Two-key activation is a compiled fact, not an assumption inferred from
# Gradle command arguments or an APK hash. Verify generated BuildConfig before
# interpreting any launch or lock UI on the CI emulator.
CONFIG=android/app/build/generated/source/buildConfig/production/debug/com/pdig/app/BuildConfig.java
if [ ! -f "$CONFIG" ]; then
  echo "CUTOVER_BUILD_CONFIG_MISSING: $CONFIG"; exit 1
fi
grep -F 'PRODUCTION_UI_GENERATION = "vnext";' "$CONFIG" >/dev/null ||
  { echo "CUTOVER_GENERATION_NOT_COMPILED"; exit 1; }
grep -F 'PRODUCTION_VNEXT_CUTOVER_APPROVED = true;' "$CONFIG" >/dev/null ||
  { echo "CUTOVER_APPROVAL_NOT_COMPILED"; exit 1; }
echo "CUTOVER_BUILD_CONFIG_VERIFIED=PASS (productionDebug only)"

# The cutover proof must run against a genuinely fresh install of THIS build. A residual
# installation (or package state left by the earlier legacy rehearsal) can otherwise leave the
# default launcher path on the previous variant even though `install -r` printed Success, which
# shows up as a confusing "the Reality shell never appeared" timeout instead of a build problem.
adb uninstall com.pdig.app >/dev/null 2>&1 || true

APK=android/app/build/outputs/apk/production/debug/app-production-debug.apk
adb install "$APK"

# Prove the device really runs the APK we just built, so a cutover failure can never be confused
# with "the wrong APK was installed".
LOCAL_SHA=$(sha256sum "$APK" | cut -d' ' -f1)
INSTALLED_PATH=$(adb shell pm path com.pdig.app | tr -d '\r' | sed 's/^package://' | head -n1)
if [ -z "$INSTALLED_PATH" ]; then
  echo "com.pdig.app is not installed after adb install"; exit 1
fi
DEVICE_SHA=$(adb shell "sha256sum '$INSTALLED_PATH'" 2>/dev/null | awk '{print $1}' | tr -d '\r')
echo "local_apk_sha256=$LOCAL_SHA"
echo "device_apk_sha256=$DEVICE_SHA"
echo "device_apk_path=$INSTALLED_PATH"
if [ -n "$DEVICE_SHA" ] && [ "$DEVICE_SHA" != "$LOCAL_SHA" ]; then
  echo "INSTALLED APK DOES NOT MATCH THE BUILT APK; refusing to interpret the cutover rehearsal"; exit 1
fi

python3 android/scripts/production_vnext_release_default_rehearsal.py
