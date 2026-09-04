$ErrorActionPreference = "Stop"
./gradlew clean build --no-daemon
Write-Host "Built gateway and all business microservices."
