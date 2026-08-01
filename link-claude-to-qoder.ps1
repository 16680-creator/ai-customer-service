# link-claude-to-qoder.ps1
# 将 .claude 下的常用目录链接到 .qoder 下
# 使用 mklink /J 创建 Junction，通常不需要管理员权限
# 不链接 worktrees，避免 Qoder 索引 Claude Code 的临时工作区
# 适用于 Windows PowerShell 5.1 / PowerShell 7+
# powershell -ExecutionPolicy Bypass -File .\link-claude-to-qoder.ps1

$ErrorActionPreference = "Stop"

$ProjectRoot = Get-Location
$ClaudeDir = Join-Path $ProjectRoot ".claude"
$QoderDir  = Join-Path $ProjectRoot ".qoder"

# 不包含 worktrees
$Dirs = @(
    "agents",
    "commands",
    "context",
    "memory",
    "rules",
    "skills"
)

Write-Host "========================================"
Write-Host "Link .claude directories to .qoder"
Write-Host "========================================"
Write-Host "Project root: $ProjectRoot"
Write-Host "Claude dir : $ClaudeDir"
Write-Host "Qoder dir  : $QoderDir"
Write-Host ""

if (!(Test-Path $ClaudeDir)) {
    throw "Directory not found: $ClaudeDir"
}

if (!(Test-Path $QoderDir)) {
    New-Item -ItemType Directory -Path $QoderDir -Force | Out-Null
    Write-Host "[OK] Created .qoder directory"
}

foreach ($Dir in $Dirs) {
    $Source = Join-Path $ClaudeDir $Dir
    $Target = Join-Path $QoderDir $Dir

    Write-Host ""
    Write-Host "Processing: $Dir"

    if (!(Test-Path $Source)) {
        Write-Host "  [SKIP] Source does not exist: $Source" -ForegroundColor Yellow
        continue
    }

    if (Test-Path $Target) {
        $Item = Get-Item $Target -Force

        if ($Item.Attributes -band [IO.FileAttributes]::ReparsePoint) {
            Write-Host "  [OK] Link/Junction already exists: $Target" -ForegroundColor Green
            continue
        }

        Write-Host "  [WARN] Target already exists and is not a link/junction: $Target" -ForegroundColor Yellow
        Write-Host "         Please backup/remove it manually if you want to replace it."
        continue
    }

    # 使用 Junction，避免 mklink /D 的管理员权限限制
    cmd /c mklink /J "`"$Target`"" "`"$Source`"" | Out-Null

    if ($LASTEXITCODE -eq 0) {
        Write-Host "  [OK] Junction created: .qoder\$Dir -> .claude\$Dir" -ForegroundColor Green
    } else {
        throw "Failed to create junction: $Target -> $Source"
    }
}

Write-Host ""
Write-Host "========================================"
Write-Host "Done."
Write-Host "Please restart qodercli to reload commands/agents/skills/rules."
Write-Host "========================================" -ForegroundColor Green