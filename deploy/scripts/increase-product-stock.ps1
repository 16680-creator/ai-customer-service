# ============================================================
# 批量增加所有在售商品库存（DB + Redis 同步，走商品服务更新接口）
# 用法：
#   powershell -ExecutionPolicy Bypass -File increase-product-stock.ps1 -Delta 100
#   powershell -ExecutionPolicy Bypass -File increase-product-stock.ps1 -Delta 100 -Gateway http://localhost:8080
# ============================================================
param(
    [int]$Delta = 100,
    [string]$Gateway = "http://localhost:8080",
    [string]$Username = "admin",
    [string]$Password = "admin123"
)

$ErrorActionPreference = "Stop"

# 1. 登录获取 token
$loginBody = "{`"username`":`"$Username`",`"password`":`"$Password`"}"
$login = Invoke-RestMethod -Method Post -Uri "$Gateway/api/user/login" -ContentType "application/json" -Body $loginBody
if ($login.code -ne 200) { throw "登录失败: $($login.message)" }
$headers = @{ Authorization = "Bearer $($login.data.token)" }
Write-Host "登录成功: $Username"

# 2. 拉取全部在售商品
$resp = Invoke-RestMethod -Method Get -Uri "$Gateway/api/product/list?page=1&size=100&status=1" -Headers $headers
$records = @($resp.data.records)
Write-Host "在售商品数: $($records.Count)"

# 3. 逐个增加库存（商品服务更新接口会同时更新 DB 和 Redis stock:{id}）
$ok = 0; $fail = 0
foreach ($p in $records) {
    $newStock = [int]$p.stock + $Delta
    $body = "{`"stock`":$newStock}"
    try {
        $r = Invoke-RestMethod -Method Put -Uri "$Gateway/api/product/$($p.id)" -Headers $headers -ContentType "application/json" -Body $body
        if ($r.code -eq 200) {
            Write-Host ("[OK]   id={0}  {1}: {2} -> {3}" -f $p.id, $p.name, $p.stock, $newStock)
            $ok++
        } else {
            Write-Host ("[FAIL] id={0}  {1}: {2}" -f $p.id, $p.name, $r.message)
            $fail++
        }
    } catch {
        Write-Host ("[FAIL] id={0}  {1}: {2}" -f $p.id, $p.name, $_.Exception.Message)
        $fail++
    }
}

Write-Host "完成: 成功 $ok，失败 $fail"
if ($fail -gt 0) { exit 1 }