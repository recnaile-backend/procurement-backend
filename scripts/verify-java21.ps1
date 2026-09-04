$ErrorActionPreference = 'Stop'

Write-Host "Checking Java..."
java -version

$javaMajor = (& java -version 2>&1 | Select-String -Pattern 'version "([0-9]+)' | ForEach-Object { $_.Matches[0].Groups[1].Value })
if ($javaMajor -ne '21') {
    throw "Java 21 is required. Detected Java $javaMajor."
}

Write-Host "Checking Gradle..."
if (Get-Command gradle -ErrorAction SilentlyContinue) {
    gradle -version
    gradle :gateway:api-gateway:tasks --all | Out-Null
} elseif (Test-Path .\gradlew.bat) {
    & .\gradlew.bat -version
    & .\gradlew.bat :gateway:api-gateway:tasks --all | Out-Null
} else {
    throw "Gradle is not installed. Run scripts\bootstrap-windows.ps1 first."
}

Write-Host "Java 21 verification passed."
