[CmdletBinding()]
param([Parameter(Mandatory=$true)][string]$Organization,[Parameter(Mandatory=$true)][string]$Repository)
$ErrorActionPreference='Stop'; Set-StrictMode -Version Latest
if (-not (Get-Command gh -ErrorAction SilentlyContinue)) { throw 'GitHub CLI (gh) is required.' }
& gh auth status; if ($LASTEXITCODE -ne 0) { throw 'Run gh auth login first.' }
$teams=@(
 @{slug='engineering';name='Engineering';description='Engineering team for Recnaile Procurement Platform'},
 @{slug='backend';name='Backend';description='Backend engineering; mailbox software.backend@recnaile.com'},
 @{slug='frontend';name='Frontend';description='Frontend engineering; mailbox software.frontend@recnaile.com'},
 @{slug='devops';name='DevOps';description='Cloud CI/CD and infrastructure engineering'},
 @{slug='qa';name='QA';description='Quality engineering and test automation'},
 @{slug='security';name='Security';description='Application and platform security'},
 @{slug='architecture';name='Architecture';description='Architecture and technical governance'},
 @{slug='development-approvers';name='Development Approvers';description='Development and staging PR approvals; mailbox software@recnaile.com'},
 @{slug='production-approvers';name='Production Approvers';description='Production main PR approvals; mailbox info@recnaile.com'}
)
foreach($team in $teams){
 Write-Host "Ensuring team @$($team.slug)" -ForegroundColor Cyan
 $payload=@{name=$team.name;description=$team.description;privacy='closed';notification_setting='notifications_enabled'}|ConvertTo-Json -Compress
 & gh api "orgs/$Organization/teams/$($team.slug)" *> $null; $exists=($LASTEXITCODE -eq 0)
 if(-not $exists){$tmp=Join-Path $env:TEMP "github-team-$($team.slug).json";Set-Content $tmp $payload -Encoding utf8;& gh api -X POST "orgs/$Organization/teams" -H 'Accept: application/vnd.github+json' --input $tmp;Remove-Item $tmp -Force -ErrorAction SilentlyContinue;if($LASTEXITCODE -ne 0){throw "Failed creating team $($team.slug)."}}
 & gh api -X PUT "orgs/$Organization/teams/$($team.slug)/repos/$Organization/$Repository" -f permission=push *> $null
 if($LASTEXITCODE -ne 0){Write-Warning "Could not grant repository access to @$($team.slug). Check organization permissions."}
}
Write-Host "`nTeam structure configured." -ForegroundColor Green
Write-Host 'Production/main PR approvals -> @production-approvers -> info@recnaile.com'
Write-Host 'Development/staging PR approvals -> @development-approvers -> software@recnaile.com'
Write-Host 'Backend ownership -> @backend -> software.backend@recnaile.com'
Write-Host 'Frontend ownership -> @frontend -> software.frontend@recnaile.com'
Write-Host '`nIMPORTANT: GitHub uses GitHub accounts/teams, not email addresses, for CODEOWNERS. Add the corresponding GitHub accounts to these teams.' -ForegroundColor Yellow
