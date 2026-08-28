# 一键启动 ai-cs-py-chat:创建 venv(首次) -> 安装依赖 -> 启动服务
# 用法:在 ai-cs-py-chat 目录下执行  .\scripts\run.ps1
$ErrorActionPreference = "Stop"

$root = Split-Path -Parent $PSScriptRoot
Set-Location $root

# 1. 检查 Python(WindowsApps 下的 python.exe 是商店占位符,不算数)
$py = Get-Command python -ErrorAction SilentlyContinue
if (-not $py -or $py.Source -like "*WindowsApps*") {
    Write-Host "[错误] 未检测到可用的 Python,请先安装 Python 3.10+(推荐 3.12):" -ForegroundColor Red
    Write-Host "       winget install -e --id Python.Python.3.12" -ForegroundColor Yellow
    Write-Host "       安装后重新打开终端再运行本脚本" -ForegroundColor Yellow
    exit 1
}

# 2. 创建 venv(仅首次)
if (-not (Test-Path ".venv")) {
    Write-Host "[1/3] 创建虚拟环境 .venv ..."
    python -m venv .venv
}

$python = Join-Path $root ".venv\Scripts\python.exe"

# 3. 安装依赖
Write-Host "[2/3] 安装依赖 ..."
& $python -m pip install -r requirements.txt -q

# 4. 生成 .env(仅首次,需自行填入 LLM_API_KEY)
if (-not (Test-Path ".env")) {
    Copy-Item ".env.example" ".env"
    Write-Host "[提示] 已从模板生成 .env,请填入 LLM_API_KEY 后再发起对话" -ForegroundColor Yellow
}

# 5. 启动服务(开发模式,代码改动自动重载)
Write-Host "[3/3] 启动服务 http://localhost:8000 (接口文档: http://localhost:8000/docs)" -ForegroundColor Green
& $python -m uvicorn app.main:app --host 0.0.0.0 --port 8000 --reload
