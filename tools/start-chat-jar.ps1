$root  = "D:\Projects\Persion\ai-customer-service"
$log   = "$root\tools\logs"
$jar   = "$root\ai-cs-chat\target\ai-cs-chat-1.0.0-SNAPSHOT.jar"

Start-Process -FilePath "java.exe" `
    -ArgumentList "-jar", $jar `
    -WorkingDirectory $root `
    -RedirectStandardOutput "$log\ai-cs-chat.jar.out.log" `
    -RedirectStandardError "$log\ai-cs-chat.jar.err.log" `
    -WindowStyle Hidden -PassThru | ForEach-Object { Write-Output "ai-cs-chat PID=$($_.Id)" }