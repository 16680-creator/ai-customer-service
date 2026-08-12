@echo off
echo Starting Nacos and RocketMQ...

echo [1/3] Starting Nacos (standalone, remote MySQL)...
start "Nacos" cmd /k "cd /d D:\Code\aiCode\Daily\ai-customer-service\tools\nacos\bin && startup.cmd -m standalone"

echo [2/3] Starting RocketMQ NameServer...
start "RocketMQ-NameSrv" cmd /k "cd /d D:\Code\aiCode\Daily\ai-customer-service\tools\rocketmq\rocketmq-all-5.1.4-bin-release\bin && mqnamesrv.cmd"

timeout /t 10 /nobreak >nul

echo [3/3] Starting RocketMQ Broker...
start "RocketMQ-Broker" cmd /k "cd /d D:\Code\aiCode\Daily\ai-customer-service\tools\rocketmq\rocketmq-all-5.1.4-bin-release\bin && mqbroker.cmd -n 127.0.0.1:9876 autoCreateTopicEnable=true"

echo.
echo All services starting in separate windows...
echo   - Nacos:      http://localhost:8848/nacos  (nacos/nacos)
echo   - RocketMQ NameServer: 127.0.0.1:9876
echo   - RocketMQ Broker:     127.0.0.1:10911
echo.
pause
