$ErrorActionPreference = "Stop"

Write-Host "=== Procurement Platform Foundation - Windows Bootstrap ==="

# ------------------------------------------------------------
# Java check
# ------------------------------------------------------------

if (-not (Get-Command java -ErrorAction SilentlyContinue)) {
    throw "Java is not installed. Install JDK 21 and rerun."
}

Write-Host "Java detected."

# ------------------------------------------------------------
# Gradle check
# ------------------------------------------------------------

if (-not (Get-Command gradle -ErrorAction SilentlyContinue)) {

    Write-Host ""
    Write-Host "Gradle not found. Installing Gradle with winget..."

    if (-not (Get-Command winget -ErrorAction SilentlyContinue)) {
        throw "winget is not available. Install Gradle manually, then rerun."
    }

    winget install --id Gradle.Gradle --accept-source-agreements --accept-package-agreements

    Write-Host ""
    Write-Host "Gradle installation completed."
    Write-Host ""
    Write-Host "IMPORTANT: Close this PowerShell window and open a NEW PowerShell window."
    Write-Host "Then run this script again."

    exit 0
}

# ------------------------------------------------------------
# Gradle version
# ------------------------------------------------------------

Write-Host ""
Write-Host "Gradle detected."
gradle --version

# ------------------------------------------------------------
# Generate / refresh Gradle wrapper
# ------------------------------------------------------------

Write-Host ""
Write-Host "Generating/refreshing Gradle wrapper..."

gradle wrapper --gradle-version 8.14.5

Write-Host ""
Write-Host "=== Bootstrap complete ==="
Write-Host ""
Write-Host "Gradle wrapper is now available."
Write-Host ""
Write-Host "Next command:"
Write-Host ".\gradlew.bat --version"