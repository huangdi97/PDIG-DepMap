# PDIG Desktop v0.1.0 packaging - Windows x64
#   installer (NSIS)  +  portable zip  from a clean jpackage app-image.
#
# Usage:
#   powershell -ExecutionPolicy Bypass -File scripts/release/build-desktop-package.ps1 -OutDir <ascii-dir>
#
# Reproducible inputs: repo source at current HEAD; outputs only to ASCII paths.
param(
    [Parameter(Mandatory = $true)][string]$OutDir,
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
    Write-Host "[1/5] clean desktop build (jar + runtime classpath)..."
    & (Join-Path $RepoRoot "android\gradlew.bat") --no-daemon -p (Join-Path $RepoRoot "desktop") :app:clean :app:jar :app:collectRuntimeForJpackage 2>&1 | Tee-Object -FilePath (Join-Path $AsciiOut "build.log")
    if ($LASTEXITCODE -ne 0) { throw "gradle desktop build failed (exit $LASTEXITCODE)" }
}

$Libs = Join-Path $BuildRoot "app\jpackage-libs"
$MainJar = Get-ChildItem -Path $Libs -Filter "app-*.jar" | Where-Object { $_.Name -notmatch "sources" } | Select-Object -First 1
if (-not $MainJar) { throw "main jar not found under $Libs" }
Write-Host "[2/5] main jar: $($MainJar.Name)"

# jpackage --name PDIG --dest $AsciiOut creates the image at <dest>/PDIG.
$AppDir = Join-Path $AsciiOut "PDIG"
if (Test-Path $AppDir) { Remove-Item -Recurse -Force $AppDir }

Write-Host "[3/5] jpackage app-image..."
& (Join-Path $JavaHome "bin\jpackage.exe") `
    --type app-image `
    --input $Libs `
    --main-jar $MainJar.Name `
    --main-class com.pdig.desktop.MainKt `
    --name PDIG `
    --app-version 0.1.0 `
    --vendor PDIG `
    --dest $AsciiOut
if ($LASTEXITCODE -ne 0) { throw "jpackage failed (exit $LASTEXITCODE)" }

$Launcher = Join-Path $AppDir "PDIG.exe"
if (-not (Test-Path $Launcher)) { throw "app-image launcher missing: $Launcher" }

Write-Host "[4/5] portable zip..."
$PortableZip = Join-Path $OutDir "PDIG-0.1.0-windows-x64-portable.zip"
if (Test-Path $PortableZip) { Remove-Item -Force $PortableZip }
# Zip the PDIG folder itself so extraction yields PDIG/PDIG.exe at the zip root.
Compress-Archive -Path $AppDir -DestinationPath $PortableZip -CompressionLevel Optimal

Write-Host "[5/5] NSIS installer..."
$Nsi = Join-Path $AsciiOut "PDIG-installer.nsi"
$NsiBody = @"
; PDIG 0.1.0 Developer Preview installer (unsigned; SmartScreen warning expected).
!define APPNAME "PDIG"
!define APPVERSION "0.1.0"
!define SRCDIR "$AppDir"
Name "PDIG $APPVERSION"
OutFile "$(Join-Path $OutDir 'PDIG-0.1.0-windows-x64-setup.exe')"
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
  CreateShortCut "`$SMPROGRAMS\PDIG\PDIG.lnk" "`$INSTDIR\PDIG.exe"
  CreateShortCut "`$DESKTOP\PDIG.lnk" "`$INSTDIR\PDIG.exe"
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

$Setup = Join-Path $OutDir "PDIG-0.1.0-windows-x64-setup.exe"
if (-not (Test-Path $Setup)) { throw "installer missing: $Setup" }

Write-Host ""
Write-Host "DESKTOP PACKAGE OK"
Write-Host "  installer : $Setup  ($((Get-Item $Setup).Length) bytes)"
Write-Host "  portable  : $PortableZip  ($((Get-Item $PortableZip).Length) bytes)"
Write-Host "  sha256    :"
Get-FileHash $Setup -Algorithm SHA256 | ForEach-Object { Write-Host "    setup   $($_.Hash.ToLower())" }
Get-FileHash $PortableZip -Algorithm SHA256 | ForEach-Object { Write-Host "    portable $($_.Hash.ToLower())" }
