# PDIG Harmony emulator preflight (multiclient runtime sweep, spec §52)
#
# Checks, in order:
#   1. DevEco Studio root (PDIG_DEVECO_HOME or harmony/local.properties sdk.dir)
#   2. Harmony SDK (hms + openharmony)
#   3. hdc executable + version
#   4. Emulator executable + image/deploy dir (system image)
#   5. Emulator instance metadata (device templates)
#   6. Device online (hdc list targets)
#   7. API / runtime version of any online target
#   8. Free disk on the image drive
#
# Exit code: 0 = runtime prerequisites satisfiable (or a device is online);
#            1 = a blocking prerequisite is missing (printed as BLOCKER).
#
# Usage:
#   powershell -ExecutionPolicy Bypass -File scripts/harmony/harmony_emulator_preflight.ps1
# Env override: $env:PDIG_DEVECO_HOME

param(
    [string]$DevecoHome = ""
)
$ErrorActionPreference = "Stop"
$out = @()

function Report([string]$item, [string]$detail, [string]$verdict) {
    $script:out += [pscustomobject]@{ Item = $item; Detail = $detail; Verdict = $verdict }
    Write-Host ("{0,-14} {1,-60} {2}" -f $verdict, $item, $detail)
}

if (-not $DevecoHome) { $DevecoHome = $env:PDIG_DEVECO_HOME }
if (-not $DevecoHome) {
    # fall back to harmony/local.properties sdk.dir (parent of sdk)
    $lp = Join-Path (Split-Path $PSScriptRoot) "..\harmony\local.properties"
    $sdkDirLine = Select-String -Path $lp -Pattern "^sdk\.dir\s*=\s*(.+)$" -ErrorAction SilentlyContinue
    if ($sdkDirLine -and $sdkDirLine.Matches.Count -gt 0) {
        $sdkDir = $sdkDirLine.Matches[0].Groups[1].Value -replace "\\\\", "\"
        $DevecoHome = Split-Path $sdkDir -Parent
    }
}

if (-not $DevecoHome -or -not (Test-Path $DevecoHome)) {
    Report "DevEco Studio root" $(if ($DevecoHome) { $DevecoHome } else { "<unset>" }) "BLOCKER"
    Report "Resolution" "set PDIG_DEVECO_HOME or restore DevEco Studio install" "ACTION"
    Write-Host "PREFLIGHT_RESULT=BLOCKED"
    exit 1
}
Report "DevEco Studio root" $DevecoHome "OK"

$sdk = Join-Path $DevecoHome "sdk\default"
foreach ($sub in @("hms", "openharmony")) {
    if (Test-Path (Join-Path $sdk $sub)) { Report ("SDK: " + $sub) (Join-Path $sdk $sub) "OK" }
    else { Report ("SDK: " + $sub) "missing" "BLOCKER" }
}

$hdc = Join-Path $DevecoHome "sdk\default\openharmony\toolchains\hdc.exe"
if (Test-Path $hdc) {
    Report "hdc executable" $hdc "OK"
    $ver = & $hdc version 2>&1 | Select-Object -First 1
    Report "hdc version" ($ver -join " ") "INFO"
} else {
    Report "hdc executable" "missing at $hdc" "BLOCKER"
}

$emulatorExe = Join-Path $DevecoHome "tools\emulator\Emulator.exe"
if (Test-Path $emulatorExe) {
    Report "Emulator executable" $emulatorExe "OK"
} else {
    Report "Emulator executable" "missing at $emulatorExe" "BLOCKER"
}

# System image presence: DevEco emulator keeps images under deploy\all or sdcard-\* templates
$emulatorRoot = Join-Path $DevecoHome "tools\emulator"
$imageDirs = @(Get-ChildItem $emulatorRoot -Recurse -Directory -ErrorAction SilentlyContinue |
    Where-Object { $_.Name -match "deploy|images" -and $_.FullName -match "deploy" })
$bootImg = Get-ChildItem $emulatorRoot -Recurse -Filter "*.img" -ErrorAction SilentlyContinue | Select-Object -First 1
if ($bootImg) {
    Report "Emulator system image" $bootImg.FullName "OK"
} else {
    $hasDeploy = Test-Path (Join-Path $emulatorRoot "deploy")
    Report "Emulator system image" ("deploy dir present=" + $hasDeploy + "; no .img found") "BLOCKER"
}

$deviceTemplates = Get-ChildItem (Join-Path $emulatorRoot "device_config") -Directory -ErrorAction SilentlyContinue
if ($deviceTemplates) {
    Report "Device templates" (($deviceTemplates | Select-Object -ExpandProperty Name) -join ", ") "OK"
} else {
    Report "Device templates" "none found" "INFO"
}

if (Test-Path $hdc) {
    $targets = & $hdc list targets 2>&1 | Out-String
    Report "hdc list targets" ($targets.Trim()) ($(if ($targets -match "Empty|no target") { "NO_DEVICE" } else { "ONLINE" }))
    if ($targets -match "Empty|no target") {
        Report "Runtime target" "no device/emulator online (system image missing -> emulator cannot boot)" "BLOCKER"
    } else {
        $info = & $hdc shell "param get const.ohos.apiversion" 2>&1 | Select-Object -First 1
        Report "Device API version" ($info -join " ") "INFO"
    }
}

$disk = Get-PSDrive -PSProvider FileSystem | Where-Object { $_.Used -ne $null } |
    ForEach-Object { [pscustomobject]@{ Drive = $_.Name; FreeGB = [math]::Round($_.Free / 1GB, 1) } } |
    Sort-Object FreeGB -Descending
$primary = $disk | Select-Object -First 1
Report "Free disk (largest)" ("{0}:\ {1} GB free" -f $primary.Drive, $primary.FreeGB) "INFO"

$blockers = @($out | Where-Object { $_.Verdict -eq "BLOCKER" })
if ($blockers.Count -gt 0) {
    Write-Host "PREFLIGHT_RESULT=BLOCKED"
    Write-Host "BLOCKER_COUNT=$($blockers.Count)"
    foreach ($b in $blockers) { Write-Host ("BLOCKER: " + $b.Item + " -> " + $b.Detail) }
    exit 1
}
Write-Host "PREFLIGHT_RESULT=READY"
exit 0