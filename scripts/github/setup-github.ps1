[CmdletBinding()]
param(
  [string]$Organization,
  [string]$Repository = "procurement-backend",
  [string]$DefaultBranch = "main",
  [string]$StagingBranch = "staging",
  [switch]$Private = $true,
  [switch]$SkipPush
)

$ErrorActionPreference = 'Stop'
Set-StrictMode -Version Latest

function Step([string]$Message) { Write-Host "`n==> $Message" -ForegroundColor Cyan }
function Require-Command([string]$Name) {
  if (-not (Get-Command $Name -ErrorAction SilentlyContinue)) {
    throw "$Name is required. Install it and run this script again."
  }
}

Require-Command git
Require-Command gh

Step "Checking GitHub CLI authentication"
& gh auth status
if ($LASTEXITCODE -ne 0) {
  Write-Host "Run: gh auth login" -ForegroundColor Yellow
  exit 1
}

if (-not $Organization) {
  $Organization = "Recnaile"
  Write-Host "Using GitHub organization: $Organization" -ForegroundColor Green
}
if ([string]::IsNullOrWhiteSpace($Organization)) { throw "Organization is required." }

Step "Checking repository"
$remote = (& git remote get-url origin 2>$null)
if ($LASTEXITCODE -ne 0 -or [string]::IsNullOrWhiteSpace($remote)) {
  $remote = $null
}

Step "Preparing local Git repository"
if (-not (Test-Path .git)) {
  & git init
  if ($LASTEXITCODE -ne 0) { throw "git init failed." }
}
& git branch -M $DefaultBranch

Step "Ensuring .env is ignored"
$gitignore = Join-Path (Get-Location) '.gitignore'
if (Test-Path $gitignore) {
  $content = Get-Content $gitignore -Raw
  if ($content -notmatch '(?m)^\.env$') {
    Add-Content $gitignore "`n# Local secrets`n.env`n"
  }
}

Step "Creating or verifying GitHub repository"
$repoSlug = "$Organization/$Repository"
$repoExists = $true
& gh repo view $repoSlug *> $null
if ($LASTEXITCODE -ne 0) { $repoExists = $false }

if (-not $repoExists) {
  & gh repo create $repoSlug --private --description "Procurement platform backend foundation - Java 21 / Spring Boot / MongoDB Atlas / production engineering standards"
  if ($LASTEXITCODE -ne 0) { throw "Failed to create GitHub repository." }
} else {
  Write-Host "Repository already exists: $repoSlug" -ForegroundColor Green
}

$originUrl = "https://github.com/$repoSlug.git"
if (-not $remote) {
  & git remote add origin $originUrl
} elseif ($remote -ne $originUrl) {
  Write-Host "Existing origin: $remote" -ForegroundColor Yellow
  $answer = Read-Host "Replace origin with $originUrl? (y/N)"
  if ($answer -match '^[Yy]$') { & git remote set-url origin $originUrl }
}

Step "Creating staging branch locally"
& git show-ref --verify --quiet "refs/heads/$StagingBranch"
if ($LASTEXITCODE -ne 0) {
  & git branch $StagingBranch $DefaultBranch
}

Step "Checking for secrets before first commit"
$blocked = @('.env', '*.pem', '*.key', '*credentials*.json', '*service-account*.json')
$found = @()
foreach ($pattern in $blocked) {
  $found += Get-ChildItem -Path . -Recurse -File -Filter $pattern -ErrorAction SilentlyContinue |
    Where-Object { $_.FullName -notmatch '\\.git\\' }
}
if ($found.Count -gt 0) {
  $found | ForEach-Object { Write-Host "Potential secret file: $($_.FullName)" -ForegroundColor Red }
  throw "Potential secret files found. Remove/ignore them before committing."
}

Step "Creating initial commit if needed"
& git status --porcelain
$status = & git status --porcelain
if ($status) {
  & git add .
  & git diff --cached --name-only
  $confirm = Read-Host "Commit these files as the foundation? (Y/n)"
  if ($confirm -notmatch '^[Nn]$') {
    & git commit -m "chore: initialize procurement backend foundation"
  } else {
    Write-Host "Commit skipped." -ForegroundColor Yellow
  }
}

if (-not $SkipPush) {
  Step "Pushing main and staging"
  & git push -u origin $DefaultBranch
  if ($LASTEXITCODE -ne 0) { throw "Failed to push $DefaultBranch." }
  & git push -u origin $StagingBranch
  if ($LASTEXITCODE -ne 0) { throw "Failed to push $StagingBranch." }
}

Step "Running team configuration scripts"
$scriptRoot = Split-Path -Parent $MyInvocation.MyCommand.Path
& (Join-Path $scriptRoot 'setup-github-teams.ps1') -Organization $Organization -Repository $Repository
& (Join-Path $scriptRoot 'setup-github-rules.ps1') -Organization $Organization -Repository $Repository -DefaultBranch $DefaultBranch -StagingBranch $StagingBranch
& (Join-Path $scriptRoot 'setup-github-environments.ps1') -Organization $Organization -Repository $Repository

Write-Host "`nGitHub bootstrap completed." -ForegroundColor Green
Write-Host "Repository: https://github.com/$repoSlug"
Write-Host "Branches: $DefaultBranch, $StagingBranch"
Write-Host "Production/main PR approvals: info@recnaile.com (via @production-approvers)"
Write-Host "Development/staging PR approvals: software@recnaile.com (via @development-approvers)"
Write-Host "Backend developers: software.backend@recnaile.com (via @backend)"
Write-Host "Frontend developers: software.frontend@recnaile.com (via @frontend)"
Write-Host "Next: add real team members to teams and configure AWS OIDC roles/secrets outside this bootstrap."
