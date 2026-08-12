# 将 tools/nacos-config 下的所有 yml 配置发布到 Nacos aics 命名空间
# 用法: powershell -ExecutionPolicy Bypass -File .\publish-nacos-config.ps1

$nacos   = "http://127.0.0.1:8848"
$tenant  = "aics"
$group   = "DEFAULT_GROUP"
$dir     = "D:\Projects\Persion\ai-customer-service\tools\nacos-config"

Get-ChildItem -Path $dir -Filter *.yml | ForEach-Object {
    $dataId  = $_.Name
    $content = Get-Content -Raw -Path $_.FullName
    $body = @{
        dataId  = $dataId
        group   = $group
        tenant  = $tenant
        type    = "yaml"
        content = $content
    }
    try {
        $r = Invoke-RestMethod -Uri "$nacos/nacos/v1/cs/configs" -Method Post -Body $body -TimeoutSec 15
        Write-Output ("{0} => {1}" -f $dataId, $r)
    } catch {
        Write-Output ("{0} => ERROR: {1}" -f $dataId, $_.Exception.Message)
    }
}
