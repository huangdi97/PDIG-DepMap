# android_preflight.ps1
#
# PDIG v0.2.0 Android environment preflight (goal §7-§9).
# Deterministic, fail-fast environment check before any Android build/test.
#
# Checks:
#   SDK root (ANDROID_HOME / ANDROID_SDK_ROOT / local.properties / LOCALAPPDATA)
#   adb actual path + version (platform-tools), old-adb PATH conflict -> ADB_PATH_CONFLICT
#   emulator path, sdkmanager path
#   system-images;android-36;google_apis;x86_64 (system.img + kernel-ranchu)
#   AVD presence (pdig36), boot_completed, adb device status
#
# Usage:
#   powershell -ExecutionPolicy Bypass -File scripts\android\android_preflight.ps1
#   powershell -ExecutionPolicy Bypass -File scripts\android\android_preflight.ps1 -SdkRoot D:\Code\Android\SDK
#
# Exit codes: 0 = PASS, 2 = ADB_PATH_CONFLICT, 3 = ANDROID36_SYSTEM_IMAGE_CORRUPTED,
#             4 = environment missing (sdk/adb/emulator/avd), 5 = device boot incomplete.

param(
    [string]$SdkRoot = "",
    [string]$AvdName = "pdig36",
    [string]$SystemImage = "system-images;android-36;google_apis;x86_64"
)

$ErrorActionPreference = "Continue"
$script:failures = @()

function Report {
    param([string]$Status, [string]$Name, [string]$Value)
    "{0}`t{1}`t{2}" -f $Status, $Name, $Value
}

function Fail {
    param([string]$Code, [string]$Message)
    $script:failures += $Code
    Write-Error $Message
}

# ---------------------------------------------------------------------------
# 1. SDK root resolution
# ---------------------------------------------------------------------------
if (-not $SdkRoot) {
    foreach ($envName in @("ANDROID_HOME", "ANDROID_SDK_ROOT")) {
        if ($(Get-Item Env:$envName -ErrorAction SilentlyContinue) -and (Test-Path (Get-Item Env:$envName -ErrorAction SilentlyContinue).Value)) { $SdkRoot = (Get-Item Env:$envName -ErrorAction SilentlyContinue).Value; break }
    }
    $repoRoot = Split-Path (Split-Path $PSScriptRoot -Parent) -Parent
    $localProps = Join-Path $repoRoot "android\local.properties"
    if (Test-Path $localProps) {
        $line = Get-Content $localProps | Where-Object { $_ -match '^sdk\.dir=' } | Select-Object -First 1
        if ($line) {
            $candidate = ($line -replace '^sdk\.dir=', '').Trim()
            # local.properties escapes \ and : ; unescape for real path check
            $candidate = $candidate -replace '\\\\', '\' -replace '\\:', ':'
            if ($candidate -match '^[A-Za-z]:\\.*') { $SdkRoot = $candidate }
        }
    }
}
if (-not $SdkRoot -and $env:LOCALAPPDATA) {
    $candidate = Join-Path $env:LOCALAPPDATA "Android\Sdk"
    if (Test-Path $candidate) { $SdkRoot = $candidate }
}
if (-not $SdkRoot -or -not (Test-Path $SdkRoot)) {
    Fail "SDK_ROOT_MISSING" "Android SDK root not found. Set ANDROID_HOME or pass -SdkRoot."
} else {
    Report "OK" "sdk_root" $SdkRoot
}

# ---------------------------------------------------------------------------
# 2. adb: actual path + version + PATH conflict detection (goal §8)
# ---------------------------------------------------------------------------
$platformToolsAdb = ""
if ($SdkRoot -and (Test-Path (Join-Path $SdkRoot "platform-tools\adb.exe"))) {
    $platformToolsAdb = Join-Path $SdkRoot "platform-tools\adb.exe"
}

$pathAdb = ""
$g = Get-Command adb -ErrorAction SilentlyContinue
if ($g) { $pathAdb = $g.Source }

if (-not $platformToolsAdb) {
    Fail "PLATFORM_TOOLS_MISSING" "platform-tools\adb.exe missing under SDK root '$SdkRoot'."
} else {
    $ptVer = (& $platformToolsAdb version 2>&1 | Select-Object -First 1) -join ' '
    Report "OK" "platform_tools_adb" $platformToolsAdb
    Report "INFO" "platform_tools_adb_version" $ptVer
}

if ($pathAdb) {
    $pathAdbFull = (Resolve-Path $pathAdb -ErrorAction SilentlyContinue).Path
    $ptFull = if ($platformToolsAdb) { (Resolve-Path $platformToolsAdb -ErrorAction SilentlyContinue).Path } else { "" }
    $conflict = $false
    if ($pathAdbFull -and $ptFull -and ($pathAdbFull -ine $ptFull)) {
        $conflict = $true
    } elseif ($pathAdbFull -and -not $ptFull) {
        $conflict = $true
    }
    if ($conflict) {
        # Old adb (e.g. C:\Android\adb.exe 1.0.32) occupies PATH -> fail fast.
        $pathVer = (& $pathAdb version 2>&1 | Select-Object -First 1) -join ' '
        Write-Host ""
        Write-Host "ADB_PATH_CONFLICT"
        Write-Host "actual adb path:      $pathAdbFull"
        Write-Host "actual adb version:   $pathVer"
        Write-Host "required platform-tools adb path: $platformToolsAdb"
        Write-Host ""
        Write-Host "Fix: prepend '<SDK>\platform-tools' to PATH, or run scripts with explicit platform-tools adb."
        Fail "ADB_PATH_CONFLICT" "Old adb on PATH ($pathAdbFull) differs from platform-tools adb."
    } else {
        $pathVer = (& $pathAdb version 2>&1 | Select-Object -First 1) -join ' '
        Report "OK" "adb_on_path" $pathAdbFull
        Report "INFO" "adb_on_path_version" $pathVer
    }
} else {
    Report "WARN" "adb_on_path" "not on PATH (script uses platform-tools adb explicitly)"
}

# ---------------------------------------------------------------------------
# 3. emulator / sdkmanager paths
# ---------------------------------------------------------------------------
$emulator = Join-Path $SdkRoot "emulator\emulator.exe"
if (Test-Path $emulator) {
    Report "OK" "emulator" $emulator
} else {
    Fail "EMULATOR_MISSING" "emulator.exe missing under SDK root '$SdkRoot'."
}

$sdkmanager = ""
foreach ($p in @("cmdline-tools\latest\bin\sdkmanager.bat", "cmdline-tools\bin\sdkmanager.bat")) {
    $candidate = Join-Path $SdkRoot $p
    if (Test-Path $candidate) { $sdkmanager = $candidate; break }
}
if ($sdkmanager) {
    Report "OK" "sdkmanager" $sdkmanager
} else {
    Fail "SDKMANAGER_MISSING" "sdkmanager.bat not found under SDK root '$SdkRoot'."
}

# ---------------------------------------------------------------------------
# 4. android-36 system image integrity (goal §9)
# ---------------------------------------------------------------------------
$imgRel = $SystemImage -replace '^system-images;', 'system-images\' -replace ';', '\'
$imgDir = Join-Path $SdkRoot $imgRel
$systemImg = Join-Path $imgDir "system.img"
$kernelRanchU = Join-Path $imgDir "kernel-ranchu"
$corrupted = $false
if (-not (Test-Path $imgDir)) {
    Fail "ANDROID36_SYSTEM_IMAGE_CORRUPTED" "system image directory missing: $imgDir"
    $corrupted = $true
} else {
    foreach ($f in @($systemImg, $kernelRanchU)) {
        if (-not (Test-Path $f)) {
            Fail "ANDROID36_SYSTEM_IMAGE_CORRUPTED" "missing file in system image: $f"
            $corrupted = $true
        }
    }
}
if (-not $corrupted) {
    $size = (Get-Item $systemImg).Length
    Report "OK" "android36_system_img" ("{0} ({1:N0} bytes)" -f $systemImg, $size)
    Report "OK" "android36_kernel_ranchu" $kernelRanchU
}
if ($corrupted) {
    Write-Host ""
    Write-Host "ANDROID36_SYSTEM_IMAGE_CORRUPTED"
    Write-Host "Deterministic repair (do NOT auto-run; requires user opt-in, ~4.4GB download):"
    Write-Host "  & '$sdkmanager' '$SystemImage'"
}

# ---------------------------------------------------------------------------
# 5. AVD presence + boot state + device status
# ---------------------------------------------------------------------------
$avdDir = Join-Path $env:USERPROFILE ".android\avd\$AvdName.avd"
if (Test-Path $avdDir) {
    Report "OK" "avd" "$AvdName ($avdDir)"
} else {
    Fail "AVD_MISSING" "AVD '$AvdName' not found under $env:USERPROFILE\.android\avd"
}
$adb = $platformToolsAdb
if ($adb) {
    $devices = (& $adb devices 2>&1 | Select-String -Pattern "device$|emulator-" )
    $online = ($devices | Where-Object { $_ -match "\s+device\s*$" }).Count
    Report "INFO" "adb_devices" ($devices -join ' | ')
    if ($online -gt 0) {
        $boot = (& $adb -s $AvdName shell getprop sys.boot_completed 2>&1 | Select-Object -First 1)
        $bootState = (& $adb -s $AvdName shell getprop init.svc.bootanim 2>&1 | Select-Object -First 1)
        if ("$boot".Trim() -eq "1") {
            Report "OK" "boot_completed" $AvdName
        } else {
            Fail "DEVICE_NOT_BOOTED" "AVD $AvdName online but sys.boot_completed != 1 (bootanim=$bootState)"
        }
    } else {
        Report "WARN" "boot_completed" "no online device; boot_completed only checked when AVD running"
    }
}
if ($adb) { & $adb kill-server 2>&1 | Out-Null }


# ---------------------------------------------------------------------------
# Summary
# ---------------------------------------------------------------------------
Write-Host ""
if ($script:failures.Count -eq 0) {
    Write-Host "ANDROID_PREFLIGHT = PASS"
    exit 0
} else {
    Write-Host "ANDROID_PREFLIGHT = FAIL ($($script:failures -join ', '))"
    if ($script:failures -contains "ADB_PATH_CONFLICT") { exit 2 }
    if ($script:failures -contains "ANDROID36_SYSTEM_IMAGE_CORRUPTED") { exit 3 }
    exit 4
}
