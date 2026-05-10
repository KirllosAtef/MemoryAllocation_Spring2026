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

# Smart Maven detection
$MAVEN_CMD = "mvn"
if (!(Get-Command $MAVEN_CMD -ErrorAction SilentlyContinue)) {
    Write-Host "Maven not in PATH, searching in IntelliJ folders..." -ForegroundColor Yellow
    # Search common JetBrains installation path
    $JETBRAINS_MVN = Get-ChildItem -Path "C:\Program Files\JetBrains" -Filter mvn.cmd -Recurse -Depth 8 -ErrorAction SilentlyContinue | Select-Object -First 1
    if ($JETBRAINS_MVN) {
        $MAVEN_CMD = $JETBRAINS_MVN.FullName
        Write-Host "Found Maven: $MAVEN_CMD" -ForegroundColor Gray
    }
}

# Smart jpackage detection
$JPACKAGE_CMD = "jpackage"
if (!(Get-Command $JPACKAGE_CMD -ErrorAction SilentlyContinue)) {
    Write-Host "jpackage not in PATH, searching in Java folders..." -ForegroundColor Yellow
    # Search common Java installation path
    $JAVA_JPACKAGE = Get-ChildItem -Path "C:\Program Files\Java" -Filter jpackage.exe -Recurse -Depth 4 -ErrorAction SilentlyContinue | Select-Object -First 1
    if ($JAVA_JPACKAGE) {
        $JPACKAGE_CMD = $JAVA_JPACKAGE.FullName
        Write-Host "Found jpackage: $JPACKAGE_CMD" -ForegroundColor Gray
    }
}

Write-Host "`n[1/3] Cleaning and building Fat JAR..." -ForegroundColor Cyan
& $MAVEN_CMD clean package -DskipTests

if (!(Test-Path "target/$MAIN_JAR")) {
    Write-Error "Could not find the fat JAR: target/$MAIN_JAR. Check maven output above."
}

# 2. Prepare output directory
if (Test-Path $DEST_DIR) {
    Remove-Item -Recurse -Force $DEST_DIR
}
New-Item -ItemType Directory -Path $DEST_DIR | Out-Null

Write-Host "[2/3] Creating Portable App Image..." -ForegroundColor Cyan
Write-Host "      This bundles a private Java runtime. No extra software (like WiX) is needed." -ForegroundColor Gray

# 3. Run jpackage
# Note: --type app-image creates a portable folder with an EXE inside
& $JPACKAGE_CMD --input target/ `
         --dest $DEST_DIR `
         --name $APP_NAME `
         --main-jar $MAIN_JAR `
         --main-class Launcher `
         --type app-image `
         --vendor "KirllosAtef" `
         --description "Memory Allocation Simulator - OS Assignment" `
         --app-version $VERSION `
         --copyright "Copyright (c) 2026"

if ($LASTEXITCODE -eq 0) {
    Write-Host "`n[3/3] Success!" -ForegroundColor Green
    Write-Host "=============================================================================="
    Write-Host " Portable folder created in: $DEST_DIR\$APP_NAME"
    Write-Host " TO RUN: Open that folder and double-click '$APP_NAME.exe'"
    Write-Host " You can zip this folder and share it; it works without Java installed."
    Write-Host "==============================================================================`n"
} else {
    Write-Host "`n[!] jpackage failed." -ForegroundColor Red
}
