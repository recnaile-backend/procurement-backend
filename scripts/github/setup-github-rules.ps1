[CmdletBinding()]
param([Parameter(Mandatory=$true)][string]$Organization,[Parameter(Mandatory=$true)][string]$Repository,[string]$DefaultBranch='main',[string]$StagingBranch='staging')
$ErrorActionPreference='Stop';Set-StrictMode -Version Latest
if(-not(Get-Command gh -ErrorAction SilentlyContinue)){throw 'GitHub CLI (gh) is required.'};& gh auth status;if($LASTEXITCODE -ne 0){throw 'Run gh auth login first.'}
function Apply-Ruleset([string]$Name,[string]$Branch,[int]$Approvals){
 $json=@{name=$Name;target='branch';enforcement='active';conditions=@{ref_name=@{include=@("refs/heads/$Branch");exclude=@()}};rules=@(@{type='deletion'},@{type='non_fast_forward'},@{type='pull_request';parameters=@{required_approving_review_count=$Approvals;dismiss_stale_reviews_on_push=$true;require_code_owner_review=$true;require_last_push_approval=$true;required_review_thread_resolution=$true}})}|ConvertTo-Json -Depth 10 -Compress
 $existing=& gh api "repos/$Organization/$Repository/rulesets" --jq ".[] | select(.name == \"$Name\") | .id" 2>$null
 $tmp=Join-Path $env:TEMP "github-ruleset-$Branch.json";Set-Content $tmp $json -Encoding utf8
 if($existing){& gh api -X PUT "repos/$Organization/$Repository/rulesets/$existing" -H 'Accept: application/vnd.github+json' --input $tmp}else{& gh api -X POST "repos/$Organization/$Repository/rulesets" -H 'Accept: application/vnd.github+json' --input $tmp};Remove-Item $tmp -Force -ErrorAction SilentlyContinue;if($LASTEXITCODE -ne 0){throw "Failed to apply ruleset $Name."}
}
Apply-Ruleset 'PROD-MAIN-PROTECTION' $DefaultBranch 2
Apply-Ruleset 'STAGING-PROTECTION' $StagingBranch 1
Write-Host 'Branch rulesets configured.' -ForegroundColor Green
Write-Host 'main: 2 approvals + CODEOWNERS; production approval group: @production-approvers (info@recnaile.com)'
Write-Host 'staging: 1 approval + CODEOWNERS; development approval group: @development-approvers (software@recnaile.com)'
