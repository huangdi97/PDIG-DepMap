# android_real_device_acceptance.ps1
#
# Automated collection tool for real-device acceptance
# (companion to ANDROID_REAL_DEVICE_ACCEPTANCE_PLAN.md)
#
# Read-only; does NOT collect unrelated private data.
#   1. adb devices -l (serial / model)
#   2. Device props: model / manufacturer / Android version / API / ABI / density / resolution
#   3. APK existence + SHA256 (-ApkPath, default .\app-release.apk)
#   4. adb install -r result
#   5. Optional androidTest run (-Instrumentation <full runner class>)
#   6. logcat error scan: FATAL EXCEPTION / ANR / SQLiteException -> crash/ANR counts
#   7. Markdown report -> scripts/android_real_device_acceptance_report.md
#
# Privacy boundary: does not read contacts / SMS / location / statements / clipboard / account tokens.
#
# Usage (PowerShell):
#   .\scripts\android_real_device_acceptance.ps1 -DeviceSerial emulator-5554 -ApkPath .\app-release.apk
#   .\scripts\android_real_device_acceptance.ps1 -DeviceSerial <serial> -Instrumentation com.pdig.app.test/androidx.test.runner.AndroidJUnitRunner

param(
    [string]$DeviceSerial = "",
    [string]$ApkPath = "",
    [string]$Instrumentation = "",
    [string]$ReportPath = "scripts\android_real_device_acceptance_report.md"
)

$ErrorActionPreference = "Continue"
$global:LASTEXITCODE = 0

function Find-Adb {
    $candidates = @()
    if ($env:ANDROID_HOME) { $candidates += "$env:ANDROID_HOME\platform-tools\adb.exe" }
    if ($env:LOCALAPPDATA) { $candidates += "$env:LOCALAPPDATA\Android\Sdk\platform-tools\adb.exe" }
    foreach ($c in $candidates) { if (Test-Path $c) { return $c } }
    $g = Get-Command adb -ErrorAction SilentlyContinue
    if ($g) { return $g.Source }
    throw "adb not found. Install Android SDK platform-tools or set ANDROID_HOME."
}

function Invoke-AdbShell {
    # Usage: Invoke-AdbShell $serial 'shell getprop ro.product.model'
    param([string]$Serial, [string]$ArgsText)
    if ($Serial) { & $adb -s $Serial $ArgsText.Split(' ') 2>&1 } else { & $adb $ArgsText.Split(' ') 2>&1 }
}

function Adb-Devices {
    param([string]$Serial)
    if ($Serial) { & $adb -s $Serial devices 2>&1 } else { & $adb devices 2>&1 }
}

function Adb-DevicesL {
    if ($DeviceSerial) { & $adb -s $DeviceSerial devices -l 2>&1 } else { & $adb devices -l 2>&1 }
}

$adb = Find-Adb
Write-Host "adb = $adb"

# Pick target device
if (-not $DeviceSerial) {
    $devs = Adb-Devices "" | Select-Object -Skip 1 | Where-Object { $_ -match "device$" }
    if (-not $devs) { throw "No device available (adb devices empty)." }
    $DeviceSerial = ($devs[0] -split "\s+")[0]
    Write-Host "Auto-selected device: $DeviceSerial"
}

$report = [System.Text.StringBuilder]::new()
[void]$report.AppendLine("# ANDROID_REAL_DEVICE_ACCEPTANCE_REPORT")
[void]$report.AppendLine("")
[void]$report.AppendLine("> Auto-generated $(Get-Date -Format 'yyyy-MM-dd HH:mm:ss') by scripts/android_real_device_acceptance.ps1")
[void]$report.AppendLine("> Status: device info / APK / install / test / logcat summary (human verdict in ANDROID_REAL_DEVICE_ACCEPTANCE_PLAN.md section 10)")
[void]$report.AppendLine("")

# 1. adb devices
[void]$report.AppendLine("## 1. adb devices")
$devList = Adb-DevicesL
[void]$report.AppendLine('```')
[void]$report.AppendLine($devList -join "`n")
[void]$report.AppendLine('```')
[void]$report.AppendLine("")

# 2. Device props
[void]$report.AppendLine("## 2. Device properties")
$props = @(
    "ro.product.model", "ro.product.manufacturer", "ro.build.version.release",
    "ro.build.version.sdk", "ro.product.cpu.abi", "ro.sf.lcd_density"
)
[void]$report.AppendLine("| Property | Value |")
[void]$report.AppendLine("|----------|-------|")
foreach ($p in $props) {
    $v = (Invoke-AdbShell $DeviceSerial "shell getprop $p" | Out-String).Trim()
    [void]$report.AppendLine("| $p | $v |")
}
$wmSize = (Invoke-AdbShell $DeviceSerial "shell wm size" | Out-String).Trim()
$wmDensity = (Invoke-AdbShell $DeviceSerial "shell wm density" | Out-String).Trim()
[void]$report.AppendLine("| wm size (resolution) | $wmSize |")
[void]$report.AppendLine("| wm density | $wmDensity |")
[void]$report.AppendLine("")

# 3. APK info + SHA256
[void]$report.AppendLine("## 3. APK info")
if (-not $ApkPath) { $ApkPath = Join-Path (Get-Location) "app-release.apk" }
if (Test-Path $ApkPath) {
    $item = Get-Item $ApkPath
    $hash = (Get-FileHash $ApkPath -Algorithm SHA256).Hash
    $ver = Invoke-AdbShell $DeviceSerial "shell dumpsys package com.pdig.app" | Select-String "versionName" | Select-Object -First 1
    [void]$report.AppendLine("| Item | Value |")
    [void]$report.AppendLine("|------|-------|")
    [void]$report.AppendLine("| APK path | $($item.FullName) |")
    [void]$report.AppendLine("| Size (bytes) | $($item.Length) |")
    [void]$report.AppendLine("| SHA256 | $hash |")
    if ($ver) { [void]$report.AppendLine("| Installed version | $($ver.Line.Trim()) |") }
} else {
    [void]$report.AppendLine("> APK not found: $ApkPath (skipping install and hash)")
}
[void]$report.AppendLine("")

# 4. install result
[void]$report.AppendLine("## 4. install result")
if (Test-Path $ApkPath) {
    $installOut = (Invoke-AdbShell $DeviceSerial "install -r $ApkPath" | Out-String)
    [void]$report.AppendLine('```')
    [void]$report.AppendLine($installOut.Trim())
    [void]$report.AppendLine('```')
    [void]$report.AppendLine("")
    if ($installOut -notmatch "Success") {
        Write-Warning "install failed: $installOut"
    }
}

# 5. androidTest (optional)
if ($Instrumentation) {
    [void]$report.AppendLine("## 5. androidTest result")
    [void]$report.AppendLine("> Run: am instrument -w $Instrumentation")
    $testOut = (Invoke-AdbShell $DeviceSerial "shell am instrument -w $Instrumentation" | Out-String)
    [void]$report.AppendLine('```')
    [void]$report.AppendLine($testOut.Trim())
    [void]$report.AppendLine('```')
    [void]$report.AppendLine("")
}

# 6. logcat error summary
[void]$report.AppendLine("## 6. logcat relevant error summary")
Invoke-AdbShell $DeviceSerial "logcat -c" 2>$null | Out-Null
$pkg = "com.pdig.app"
$pidOut = (Invoke-AdbShell $DeviceSerial "shell pidof $pkg" | Out-String).Trim()
if ($pidOut -match '^\d+$') {
    $sample = Invoke-AdbShell $DeviceSerial "shell logcat -d --pid=$pidOut" 2>$null
} else {
    $sample = Invoke-AdbShell $DeviceSerial "shell logcat -d" 2>$null
}
if (-not $sample) { $sample = @() }
$fatal = $sample | Select-String "FATAL EXCEPTION"
$anr = $sample | Select-String "ANR in $pkg|am_anr"
$sqlite = $sample | Select-String "SQLiteException|database is locked|file is not a database"
[void]$report.AppendLine("| Category | Count |")
[void]$report.AppendLine("|----------|-------|")
[void]$report.AppendLine("| FATAL EXCEPTION (crash) | $($fatal.Count) |")
[void]$report.AppendLine("| ANR | $($anr.Count) |")
[void]$report.AppendLine("| SQLiteException | $($sqlite.Count) |")
if ($fatal.Count -gt 0 -or $anr.Count -gt 0) {
    [void]$report.AppendLine("")
    [void]$report.AppendLine("### Samples (truncated to 200 chars)")
    [void]$report.AppendLine('```')
    ($fatal | Select-Object -First 5) | ForEach-Object {
        $t = $_.Line; if ($t.Length -gt 200) { $t = $t.Substring(0, 200) }
        [void]$report.AppendLine($t)
    }
    ($anr | Select-Object -First 5) | ForEach-Object {
        $t = $_.Line; if ($t.Length -gt 200) { $t = $t.Substring(0, 200) }
        [void]$report.AppendLine($t)
    }
    [void]$report.AppendLine('```')
}
[void]$report.AppendLine("")

# 7. Human verdict placeholder
[void]$report.AppendLine("## 7. Human verdict (to be filled by executor)")
[void]$report.AppendLine("```text")
[void]$report.AppendLine("Installation=<PASS/FAIL> | Security=<...> | Lifecycle=<...> | Import=<...> | D-16=<...>")
[void]$report.AppendLine("Scenario=<...> | Backup/Restore=<...> | Accessibility=<...> | Performance=<...>")
[void]$report.AppendLine("ANDROID_REAL_DEVICE_VERIFIED = PASS | BLOCKED_BY_MISSING_REAL_DEVICE | FAIL (with repro)")
[void]$report.AppendLine('`')

$reportDir = Split-Path $ReportPath
if ($reportDir -and -not (Test-Path $reportDir)) { New-Item -ItemType Directory -Path $reportDir -Force | Out-Null }
$report | Set-Content -Path $ReportPath -Encoding UTF8
Write-Host "Report generated: $ReportPath"
Write-Host "Done. Crash/ANR counts in section 6."
