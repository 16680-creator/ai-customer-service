$env:DB_PASSWORD = "Yxw172707"
$root    = "D:\Projects\Persion\ai-customer-service"
$logDir  = "$root\tools\logs"

$services = @("ai-cs-knowledge", "ai-cs-search", "ai-cs-order", "ai-cs-product")
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