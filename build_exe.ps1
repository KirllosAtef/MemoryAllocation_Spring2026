# ==============================================================================
# Memory Allocator Project - EXE Packaging Script
# ==============================================================================
# This script compiles the project and packages it into a Windows EXE installer.
# Prerequisites:
# 1. JDK 17 or higher installed and in PATH.
# 2. Maven installed and in PATH.
# 3. WiX Toolset installed (required by jpackage for EXE/MSI on Windows).
# ==============================================================================

$ErrorActionPreference = "Stop"

# 1. Define variables
$VERSION = "1.0"
$APP_NAME = "MemoryAllocatorSimulator"
$MAIN_JAR = "MemoryAllocatorProject-$VERSION-fat.jar"
$DEST_DIR = "dist"

Write-Host "`n[1/3] Cleaning and building Fat JAR..." -ForegroundColor Cyan
mvn clean package -DskipTests

if (!(Test-Path "target/$MAIN_JAR")) {
    Write-Error "Could not find the fat JAR: target/$MAIN_JAR. Check maven output above."
}

# 2. Prepare output directory
if (Test-Path $DEST_DIR) {
    Remove-Item -Recurse -Force $DEST_DIR
}
New-Item -ItemType Directory -Path $DEST_DIR | Out-Null

Write-Host "[2/3] Creating Windows Installer (EXE)..." -ForegroundColor Cyan
Write-Host "      This may take a minute as it bundles a private Java runtime." -ForegroundColor Gray

# 3. Run jpackage
# Note: --win-dir-chooser allows the user to select the install location
#       --win-shortcut adds a desktop icon
jpackage --input target/ `
         --dest $DEST_DIR `
         --name $APP_NAME `
         --main-jar $MAIN_JAR `
         --main-class Launcher `
         --type exe `
         --vendor "KirllosAtef" `
         --description "Memory Allocation Simulator - OS Assignment" `
         --app-version $VERSION `
         --win-dir-chooser `
         --win-menu `
         --win-shortcut `
         --copyright "Copyright (c) 2026"

if ($LASTEXITCODE -eq 0) {
    Write-Host "`n[3/3] Success!" -ForegroundColor Green
    Write-Host "=============================================================================="
    Write-Host " Installer created: $DEST_DIR\$APP_NAME-$VERSION.exe"
    Write-Host " You can share this EXE with anyone; they don't need Java installed to run it."
    Write-Host "==============================================================================`n"
} else {
    Write-Host "`n[!] jpackage failed." -ForegroundColor Red
    Write-Host "Tip: If you get an error about 'WiX Toolset', download it from wixtoolset.org"
    Write-Host "Alternatively, you can run 'mvn javafx:jlink' for a portable folder version."
}
