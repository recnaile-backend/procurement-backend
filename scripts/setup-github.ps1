[CmdletBinding()]
param(
  [Parameter(Mandatory=$true)][string]$Organization,
  [string]$Repository='procurement-backend'
)
$script = Join-Path $PSScriptRoot 'github\setup-github.ps1'
& $script -Organization $Organization -Repository $Repository
