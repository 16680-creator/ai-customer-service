$env:DB_PASSWORD = "Yxw172707"
$root  = "D:\Projects\Persion\ai-customer-service"
$log   = "$root\tools\logs"

foreach ($s in @("ai-cs-knowledge","ai-cs-order")) {
    $p = Start-Process -FilePath "mvn.cmd" `
        -ArgumentList "-pl", $s, "spring-boot:run", "-DskipTests" `
        -WorkingDirectory $root `
        -RedirectStandardOutput "$log\$s.springboot.out.log" `
        -RedirectStandardError "$log\$s.springboot.err.log" `
        -WindowStyle Hidden -PassThru
    Write-Output ("{0} PID={1}" -f $s, $p.Id)
}