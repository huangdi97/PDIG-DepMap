# PDIG Desktop v0.2.0 packaging - Windows x64
#   installer (NSIS)  +  portable zip  from a deterministic app-image.
#
# Why not jpackage (2026-09-25, v0.1.2 finding):
#   The v0.1.2 release artifacts could not launch: the jpackage app-image
#   was built WITHOUT runtime\bin\java.exe/javaw.exe (only java.dll + jvm.dll),
#   so PDIG.exe sat alive with no window and no JVM child. jpackage on this
#   JDK (21.0.12.101) is also deterministically broken here ("Cannot access
#   file with path exceeding 32000 characters" even on a minimal hello image).
#   -> v0.2.0 assembles the image deterministically:
#       1. jlink runtime  (verified: java.exe + javaw.exe are produced)
#       2. manual PDIG/ layout: PDIG.cmd launcher + app/ jars + runtime/
#       3. post-build verification: JVM launchers MUST exist, else fail.
#
# Usage:
#   powershell -ExecutionPolicy Bypass -File scripts/release/build-desktop-package.ps1 -OutDir <ascii-dir>
# Options: -Version 0.2.0  -BuildRoot <ascii-dir>  -SkipGradle
param(
    [Parameter(Mandatory = $true)][string]$OutDir,
    [string]$Version = "0.2.0",
    [string]$BuildRoot = "",
    [switch]$SkipGradle
)

$ErrorActionPreference = "Stop"
$RepoRoot = (Resolve-Path (Join-Path $PSScriptRoot "..\..")).Path
$JavaHome = $env:JAVA_HOME
if (-not $JavaHome) { $JavaHome = (Get-Command java).Source | Split-Path | Split-Path }
if (-not $BuildRoot) { $BuildRoot = $env:USERPROFILE + "\pdig-desktop-build" }

$OutDir = (Resolve-Path $OutDir).Path
$AsciiOut = $OutDir + "\desktop"
New-Item -ItemType Directory -Force -Path $AsciiOut | Out-Null

# ASCII build root workaround for the non-ASCII repo path (desktop/settings.gradle.kts).
$env:PDIG_DESKTOP_ASCII_BUILD_ROOT = $BuildRoot

if (-not $SkipGradle) {
    Write-Host "[1/6] clean desktop build (jar + runtime classpath)..."
    & (Join-Path $RepoRoot "android\gradlew.bat") --no-daemon -p (Join-Path $RepoRoot "desktop") :app:clean :app:jar :app:collectRuntimeForJpackage 2>&1 | Tee-Object -FilePath (Join-Path $AsciiOut "build.log")
    if ($LASTEXITCODE -ne 0) { throw "gradle desktop build failed (exit $LASTEXITCODE)" }
}

$Libs = Join-Path $BuildRoot "app\jpackage-libs"
$MainJar = Get-ChildItem -Path $Libs -Filter "app-*.jar" | Where-Object { $_.Name -notmatch "sources" } | Select-Object -First 1
if (-not $MainJar) { throw "main jar not found under $Libs" }
Write-Host "[2/6] main jar: $($MainJar.Name)"

# ---------------------------------------------------------------------------
# Runtime: compute module set from the classpath, then jlink it.
# jlink is the piece proven to work on this machine (produces java.exe/javaw.exe).
# ---------------------------------------------------------------------------
Write-Host "[3/6] computing jlink modules via jdeps ..."
$ClassPath = (Get-ChildItem -Path $Libs -Filter "*.jar" | ForEach-Object { $_.FullName }) -join ";"
if ($LASTEXITCODE -ne 0) { throw "libs enumeration failed" }
$Modules = (& (Join-Path $JavaHome "bin\jdeps.exe") --print-module-deps --ignore-missing-deps -cp $ClassPath $MainJar.FullName 2>$null | Select-Object -First 1)
if (-not $Modules -or $Modules -match "error|Error|Exception") {
    Write-Host "  jdeps could not determine modules ($Modules); falling back to ALL-MODULE-PATH"
    $Modules = "ALL-MODULE-PATH"
}
Write-Host "  modules: $Modules"

$RuntimeDir = Join-Path $AsciiOut "pdig-runtime"
if (Test-Path $RuntimeDir) { Remove-Item -Recurse -Force $RuntimeDir }
Write-Host "[4/6] jlink runtime ..."
& (Join-Path $JavaHome "bin\jlink.exe") --add-modules $Modules --no-header-files --no-man-pages --strip-debug --output $RuntimeDir
if ($LASTEXITCODE -ne 0) { throw "jlink failed (exit $LASTEXITCODE)" }

# ---------------------------------------------------------------------------
# Assemble manual app-image. Do NOT use jpackage (see header).
# ---------------------------------------------------------------------------
$AppDir = Join-Path $AsciiOut "PDIG"
if (Test-Path $AppDir) { Remove-Item -Recurse -Force $AppDir }
New-Item -ItemType Directory -Force -Path (Join-Path $AppDir "app") | Out-Null
Copy-Item -Path (Join-Path $Libs "*.jar") -Destination (Join-Path $AppDir "app") -Force
Move-Item -Path $RuntimeDir -Destination (Join-Path $AppDir "runtime")

$Launcher = Join-Path $AppDir "PDIG.cmd"
$LauncherBody = @"
@echo off
setlocal
set "APP_DIR=%~dp0"
start "" "%APP_DIR%runtime\bin\javaw.exe" -cp "%APP_DIR%app\*" com.pdig.desktop.MainKt
"@
Set-Content -Path $Launcher -Value $LauncherBody -Encoding ASCII

# ---------------------------------------------------------------------------
# Post-build verification (v0.1.2 regression guard):
# the packaged runtime MUST be able to launch -> JVM launchers must exist.
# ---------------------------------------------------------------------------
foreach ($req in @("runtime\bin\java.exe", "runtime\bin\javaw.exe", "PDIG.cmd")) {
    $p = Join-Path $AppDir $req
    if (-not (Test-Path $p)) { throw "post-build verification failed: missing $req in app-image" }
    Write-Host "[verify] OK $req"
}

Write-Host "[5/6] portable zip..."
$PortableZip = Join-Path $OutDir "PDIG-$Version-windows-x64-portable.zip"
if (Test-Path $PortableZip) { Remove-Item -Force $PortableZip }
Compress-Archive -Path $AppDir -DestinationPath $PortableZip -CompressionLevel Optimal

Write-Host "[6/6] NSIS installer..."
$Nsi = Join-Path $AsciiOut "PDIG-installer.nsi"
$NsiBody = @"
; PDIG $Version Developer Preview installer (unsigned; SmartScreen warning expected).
!define APPNAME "PDIG"
!define APPVERSION "$Version"
!define SRCDIR "$AppDir"
Name "PDIG $APPVERSION"
OutFile "$(Join-Path $OutDir ('PDIG-' + $Version + '-windows-x64-setup.exe'))"
InstallDir "`$LOCALAPPDATA\Programs\PDIG"
RequestExecutionLevel user
Unicode true
!include "MUI2.nsh"
!insertmacro MUI_PAGE_WELCOME
!insertmacro MUI_PAGE_INSTFILES
!insertmacro MUI_PAGE_FINISH
!insertmacro MUI_LANGUAGE "SimpChinese"
Section "Install"
  SetOutPath "`$INSTDIR"
  File /r "`${SRCDIR}\*.*"
  CreateDirectory "`$SMPROGRAMS\PDIG"
  CreateShortCut "`$SMPROGRAMS\PDIG\PDIG.lnk" "`$INSTDIR\PDIG.cmd"
  CreateShortCut "`$DESKTOP\PDIG.lnk" "`$INSTDIR\PDIG.cmd"
  WriteUninstaller "`$INSTDIR\Uninstall.exe"
SectionEnd
Section "Uninstall"
  Delete "`$INSTDIR\Uninstall.exe"
  RMDir /r "`$INSTDIR"
  Delete "`$SMPROGRAMS\PDIG\PDIG.lnk"
  RMDir "`$SMPROGRAMS\PDIG"
  Delete "`$DESKTOP\PDIG.lnk"
SectionEnd
"@
Set-Content -Path $Nsi -Value $NsiBody -Encoding ASCII
$MakeNsis = "C:\Program Files (x86)\NSIS\makensis.exe"
if (-not (Test-Path $MakeNsis)) { throw "makensis not found: $MakeNsis" }
& $MakeNsis $Nsi
if ($LASTEXITCODE -ne 0) { throw "makensis failed (exit $LASTEXITCODE)" }

$Setup = Join-Path $OutDir "PDIG-$Version-windows-x64-setup.exe"
if (-not (Test-Path $Setup)) { throw "installer missing: $Setup" }

Write-Host ""
Write-Host "DESKTOP PACKAGE OK"
Write-Host "  installer : $Setup  ($((Get-Item $Setup).Length) bytes)"
Write-Host "  portable  : $PortableZip  ($((Get-Item $PortableZip).Length) bytes)"
Write-Host "  sha256    :"
Get-FileHash $Setup -Algorithm SHA256 | ForEach-Object { Write-Host "    setup   $($_.Hash.ToLower())" }
Get-FileHash $PortableZip -Algorithm SHA256 | ForEach-Object { Write-Host "    portable $($_.Hash.ToLower())" }