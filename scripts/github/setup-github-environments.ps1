[CmdletBinding()]
param([Parameter(Mandatory=$true)][string]$Organization,[Parameter(Mandatory=$true)][string]$Repository)
$ErrorActionPreference='Stop';Set-StrictMode -Version Latest
if(-not(Get-Command gh -ErrorAction SilentlyContinue)){throw 'GitHub CLI (gh) is required.'};& gh auth status;if($LASTEXITCODE -ne 0){throw 'Run gh auth login first.'}
foreach($environment in @('dev','staging','production')){Write-Host "Ensuring environment: $environment" -ForegroundColor Cyan;& gh api -X PUT "repos/$Organization/$Repository/environments/$environment" -H 'Accept: application/vnd.github+json' *> $null;if($LASTEXITCODE -ne 0){throw "Failed to create environment $environment."}}
Write-Host 'Environments created: dev, staging, production.' -ForegroundColor Green
Write-Host 'Configure production environment reviewers using the @production-approvers team (info@recnaile.com).'
Write-Host 'Configure staging environment reviewers using the @development-approvers team (software@recnaile.com).'
