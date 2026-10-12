[CmdletBinding()]
param(
    [string]$BaseUrl = 'http://localhost:8092',
    [switch]$SkipDocker
)

$ErrorActionPreference = 'Stop'
$checks = @(
    @{ Name = 'Liveness'; Url = "$BaseUrl/livez" },
    @{ Name = 'Readiness (MongoDB)'; Url = "$BaseUrl/readyz" },
    @{ Name = 'Release metadata'; Url = "$BaseUrl/actuator/info" },
    @{ Name = 'Prometheus scrape'; Url = "$BaseUrl/actuator/prometheus" }
)
$failed = [System.Collections.Generic.List[string]]::new()
foreach ($check in $checks) {
    try {
        $response = Invoke-WebRequest -Uri $check.Url -Method Get -TimeoutSec 10
        if ($response.StatusCode -lt 200 -or $response.StatusCode -ge 300) {
            throw "Unexpected HTTP status $($response.StatusCode)"
        }
        Write-Host "PASS $($check.Name): HTTP $($response.StatusCode)" -ForegroundColor Green
    } catch {
        $failed.Add("$($check.Name): $($_.Exception.Message)")
        Write-Host "FAIL $($check.Name): $($_.Exception.Message)" -ForegroundColor Red
    }
}
if (-not $SkipDocker) {
    try {
        $container = docker compose ps --status running --services reporting-service
        if (-not ($container -match 'reporting-service')) {
            throw 'reporting-service is not listed as running by docker compose.'
        }
        Write-Host 'PASS Docker Compose: reporting-service is running' -ForegroundColor Green
    } catch {
        $failed.Add("Docker Compose: $($_.Exception.Message)")
        Write-Host "FAIL Docker Compose: $($_.Exception.Message)" -ForegroundColor Red
    }
}
if ($failed.Count -gt 0) {
    Write-Error ("Release verification failed:`n - " + ($failed -join "`n - "))
    exit 1
}
Write-Host 'HTTP/container checks passed. Complete the authenticated API and Kafka event checks in PHASE-15-PLATFORM-INTEGRATION-RELEASE.md.' -ForegroundColor Cyan
