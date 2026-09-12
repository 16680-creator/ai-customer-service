@echo off
rem 启动 OrcaTerm -> Trae CN 桥接服务（OpenAI 兼容, 127.0.0.1:8317）
chcp 65001 >nul
cd /d "%~dp0"
python bridge.py
pause
