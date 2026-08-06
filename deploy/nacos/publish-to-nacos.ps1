# ============================================================
# 将 deploy/nacos/configs/*.yml 批量发布到 Nacos 配置中心
# 用法：
#   powershell -ExecutionPolicy Bypass -File publish-to-nacos.ps1
#   可选环境变量：NACOS_ADDR（默认 127.0.0.1:8848）、NACOS_NAMESPACE（默认 aics）
# ============================================================
$ErrorActionPreference = 'Stop'

$NacosAddr = if ($env:NACOS_ADDR) { $env:NACOS_ADDR } else { '127.0.0.1:8848' }
$Namespace = if ($env:NACOS_NAMESPACE) { $env:NACOS_NAMESPACE } else { 'aics' }
$Group     = 'DEFAULT_GROUP'
$ConfigDir = Join-Path $PSScriptRoot 'configs'

Write-Host "Nacos: $NacosAddr | namespace: $Namespace | group: $Group" -ForegroundColor Cyan

$files = Get-ChildItem -Path $ConfigDir -Filter *.yml
if (-not $files) {
    Write-Host "configs 目录下没有 yml 文件" -ForegroundColor Red
    exit 1
}

$ok = 0; $fail = 0
foreach ($file in $files) {
    $dataId = $file.Name
    $result = & curl.exe -s -X POST "http://$NacosAddr/nacos/v1/cs/configs" `
        --data-urlencode "dataId=$dataId" `
        --data-urlencode "group=$Group" `
        --data-urlencode "tenant=$Namespace" `
        --data-urlencode "type=yaml" `
        --data-urlencode "content@$($file.FullName)"
    if ($result -eq 'true') {
        Write-Host "[OK]   $dataId" -ForegroundColor Green
        $ok++
    } else {
        Write-Host "[FAIL] $dataId -> $result" -ForegroundColor Red
        $fail++
    }
}

Write-Host ""
Write-Host "发布完成：成功 $ok，失败 $fail" -ForegroundColor Cyan
if ($fail -gt 0) { exit 1 }
