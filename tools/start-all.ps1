# =============================================================================
# One-click start: infrastructure + application (backend + frontend)
#
# Simply runs start-infra.ps1 then start-app.ps1. Prefer running the two
# scripts separately to skip parts that are already up:
#   powershell -ExecutionPolicy Bypass -File .\tools\start-infra.ps1
#   powershell -ExecutionPolicy Bypass -File .\tools\start-app.ps1 -SkipBuild
# =============================================================================

param(
    [switch]$SkipBuild,   # pass through to start-app.ps1
    [switch]$NoFrontend,  # pass through to start-app.ps1
    [string]$Service = "" # pass through to start-app.ps1
)

Write-Host "########## [1/2] Infrastructure (Nacos + RocketMQ + configs) ##########"
& "$PSScriptRoot\start-infra.ps1"

Write-Host ""
Write-Host "########## [2/2] Application (backend + frontend) ##########"
& "$PSScriptRoot\start-app.ps1" -SkipBuild:$SkipBuild -NoFrontend:$NoFrontend -Service $Service