# Publish all tools\nacos-config\*.yml to the Nacos "aics" namespace.
# Usage: powershell -ExecutionPolicy Bypass -File .\publish-nacos-config.ps1
#
# Uses curl.exe --data-urlencode "content@<file>" so file bytes are sent
# raw (UTF-8 safe). Do NOT use Invoke-RestMethod with a hashtable body:
# PowerShell 5.1 encodes it as GBK and corrupts Chinese characters.

$nacos  = "http://127.0.0.1:8848"
$tenant = "aics"
$group  = "DEFAULT_GROUP"
$dir    = "$PSScriptRoot\nacos-config"

$ok = 0; $fail = 0
Get-ChildItem -Path $dir -Filter *.yml | ForEach-Object {
    $r = & curl.exe -s -X POST "$nacos/nacos/v1/cs/configs" `
        --data-urlencode "dataId=$($_.Name)" `
        --data-urlencode "group=$group" `
        --data-urlencode "tenant=$tenant" `
        --data-urlencode "type=yaml" `
        --data-urlencode "content@$($_.FullName)"
    if ("$r".Trim() -eq "true") { $ok++; Write-Output ("  [OK]   " + $_.Name) }
    else { $fail++; Write-Output ("  [FAIL] " + $_.Name + " => " + $r) }
}
Write-Output "Published $ok configs, $fail failed"