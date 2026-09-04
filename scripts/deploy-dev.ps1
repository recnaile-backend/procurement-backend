param(
    [switch]$PlanOnly,
    [switch]$AutoApprove
)

$ErrorActionPreference = "Stop"
$Environment = "dev"
$TerraformDir = Join-Path $PSScriptRoot "..\infrastructure\terraform\environments\$Environment"

Write-Host "=== Procurement Platform Foundation / $Environment ==="
Write-Host "This deployment intentionally provisions platform infrastructure only."
Write-Host "No business microservices are deployed by this foundation repository."

Push-Location $TerraformDir
try {
    terraform init
    terraform validate
    terraform plan -out="tfplan-$Environment"

    if ($PlanOnly) {
        Write-Host "Plan complete. No resources were changed."
        exit 0
    }

    if (-not $AutoApprove) {
        $answer = Read-Host "Apply $Environment infrastructure? Type APPLY to continue"
        if ($answer -ne "APPLY") { throw "Deployment cancelled." }
    }

    terraform apply "tfplan-$Environment"
    Write-Host "$Environment infrastructure deployment completed."
} finally {
    Pop-Location
}
