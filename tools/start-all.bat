@echo off
rem ===========================================================================
rem AI Customer Service Platform - one-click launcher (double-click friendly)
rem Delegates to start-all.ps1; edit tools\env.ps1 first if JDK/Maven paths differ.
rem ===========================================================================
powershell -NoProfile -ExecutionPolicy Bypass -File "%~dp0start-all.ps1" %*
echo.
pause