# 启动全部 AI 客服微服务（mvn spring-boot:run），日志写入 tools/logs
# 用法: powershell -ExecutionPolicy Bypass -File .\start-services.ps1

$root    = "D:\Projects\Persion\ai-customer-service"
$logDir  = "$root\tools\logs"
New-Item -ItemType Directory -Force -Path $logDir | Out-Null

$services = @(
    "ai-cs-user",
    "ai-cs-knowledge",
    "ai-cs-chat",
    "ai-cs-search",
    "ai-cs-message",
    "ai-cs-notify",
    "ai-cs-order",
    "ai-cs-product",
    "ai-cs-pay",
    "ai-cs-mq",
    "ai-cs-gateway"
)

foreach ($s in $services) {
    $out = "$logDir\$s.springboot.out.log"
    $err = "$logDir\$s.springboot.err.log"
    $p = Start-Process -FilePath "mvn.cmd" `
        -ArgumentList "-pl", $s, "spring-boot:run", "-DskipTests" `
        -WorkingDirectory $root `
        -RedirectStandardOutput $out -RedirectStandardError $err `
        -WindowStyle Hidden -PassThru
    Write-Output ("{0} PID={1}" -f $s, $p.Id)
}
