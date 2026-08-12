$env:DB_PASSWORD = "Yxw172707"
$root  = "D:\Projects\Persion\ai-customer-service"
$log   = "$root\tools\logs"

Start-Process -FilePath "mvn.cmd" `
    -ArgumentList "-pl", "ai-cs-user", "spring-boot:run", "-DskipTests" `
    -WorkingDirectory $root `
    -RedirectStandardOutput "$log\ai-cs-user.springboot.out.log" `
    -RedirectStandardError "$log\ai-cs-user.springboot.err.log" `
    -WindowStyle Hidden -PassThru | ForEach-Object { Write-Output "ai-cs-user PID=$($_.Id)" }