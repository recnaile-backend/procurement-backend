$ErrorActionPreference = "Stop"

Write-Host "=== Procurement Platform Foundation Verification ==="

Write-Host "`n[1/5] Java"
java -version

Write-Host "`n[2/5] Docker"
docker --version
docker compose version

Write-Host "`n[3/5] Infrastructure"
docker compose up -d
docker compose ps

Write-Host "`n[4/5] Redis"
$redis = docker exec redis redis-cli ping
if ($redis -ne "PONG") { throw "Redis verification failed: $redis" }
Write-Host "PASS: Redis"

Write-Host "`n[5/5] Build"
gradle clean test build --no-daemon
Write-Host "PASS: Gradle build"

Write-Host "`nFoundation verification completed."
