# run_avd_gate.ps1
#
# PDIG v0.2.0 Android emulator lifecycle harness (goal §10).
# Wraps: launch emulator -> wait boot -> run operation -> collect evidence -> shutdown,
# all inside one script so the emulator process is not reaped when the launching
# shell exits. Tests no longer depend on ad-hoc agent shell assembly.
#
# Usage:
#   powershell -ExecutionPolicy Bypass -File scripts\android\run_avd_gate.ps1 `
#     -AvdName pdig36 -Operation "adb shell getprop ro.build.version.release" `
#     -EvidenceFile .tmp_audit\avd-gate-evidence.txt
#
#   -Operation is run after boot; its stdout is appended to -EvidenceFile.
#   Use -NoShutdown to keep the AVD alive (debug only).

param(
    [string]$SdkRoot = "",
    [string]$AvdName = "pdig36",
    [string]$Operation = "",
    [string]$EvidenceFile = "",
    [int]$BootTimeoutSeconds = 300,
    [switch]$NoShutdown,
    [switch]$NoBootCheck
)

$ErrorActionPreference = "Continue"

if (-not $SdkRoot) {
    $repoRoot = Split-Path (Split-Path $PSScriptRoot -Parent) -Parent
    $localProps = Join-Path $repoRoot "android\local.properties"
    if (Test-Path $localProps) {
        $line = Get-Content $localProps | Where-Object { $_ -match '^sdk\.dir=' } | Select-Object -First 1
        if ($line) {
            $candidate = (($line -replace '^sdk\.dir=', '').Trim()) -replace '\\\\', '\' -replace '\\:', ':'
            if ($candidate -match '^[A-Za-z]:\\') { $SdkRoot = $candidate }
        }
    }
}
if (-not $SdkRoot -and $env:LOCALAPPDATA) {
    $candidate = Join-Path $env:LOCALAPPDATA "Android\Sdk"
    if (Test-Path $candidate) { $SdkRoot = $candidate }
}
if (-not $SdkRoot) { throw "SDK root not found. Pass -SdkRoot or set ANDROID_HOME." }

$emulator = Join-Path $SdkRoot "emulator\emulator.exe"
$adb = Join-Path $SdkRoot "platform-tools\adb.exe"
if (-not (Test-Path $emulator)) { throw "emulator.exe missing: $emulator" }
if (-not (Test-Path $adb)) { throw "adb.exe missing: $adb" }

function Log { param([string]$M) Write-Host "[avd-gate] $M" }

# ---------------------------------------------------------------------------
# Launch emulator detached (no-window, no-audio; survives this shell)
# ---------------------------------------------------------------------------
$serial = "emulator-5554"
Log "launching AVD $AvdName ..."
$emuProc = Start-Process -FilePath $emulator `
    -ArgumentList @("-avd", $AvdName, "-no-window", "-no-audio", "-no-boot-anim", "-no-snapshot", "-wipe-data") `
    -PassThru -WindowStyle Hidden
Log "emulator pid=$($emuProc.Id)"

# ---------------------------------------------------------------------------
# Wait for boot
# ---------------------------------------------------------------------------
$booted = $false
if ($NoBootCheck) {
    Log "skipping boot check (-NoBootCheck)"
    $booted = $true
} else {
    $deadline = (Get-Date).AddSeconds($BootTimeoutSeconds)
    while ((Get-Date) -lt $deadline) {
        if ($emuProc.HasExited) {
            Log "FATAL: emulator process exited early (exit=$($emuProc.ExitCode))"
            exit 1
        }
        $state = (& $adb -s $serial shell getprop sys.boot_completed 2>&1 | Select-Object -First 1)
        if ("$state".Trim() -eq "1") { $booted = $true; break }
        Start-Sleep -Seconds 5
    }
}
if (-not $booted) {
    Log "FATAL: AVD $AvdName did not reach boot_completed within ${BootTimeoutSeconds}s"
    & $adb -s $serial shell getprop init.svc.bootanim 2>&1 | ForEach-Object { Log $_ }
    exit 2
}
Log "boot_completed = 1 (took up to ${BootTimeoutSeconds}s)"

# ---------------------------------------------------------------------------
# Run operation, collect evidence
# ---------------------------------------------------------------------------
if ($Operation) {
    Log "running operation: $Operation"
    if ($EvidenceFile) {
        $opOut = Invoke-Expression $Operation 2>&1 | Out-String
        $opOut | Out-File -FilePath $EvidenceFile -Encoding utf8
        Log "evidence appended -> $EvidenceFile"
    } else {
        Invoke-Expression $Operation 2>&1 | ForEach-Object { Log $_ }
    }
}

# ---------------------------------------------------------------------------
# Shutdown (unless -NoShutdown)
# ---------------------------------------------------------------------------
if ($NoShutdown) {
    Log "keeping AVD alive (-NoShutdown)"
} else {
    Log "shutting down AVD ..."
    & $adb -s $serial emu kill 2>&1 | Out-Null
    Start-Sleep -Seconds 8
    if (-not $emuProc.HasExited) { Stop-Process -Id $emuProc.Id -Force -ErrorAction SilentlyContinue }
    Log "AVD stopped"
}
Log "run_avd_gate complete"
exit 0
