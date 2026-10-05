@echo off
rem Luma staging: wrapper to run scripts\start-staging.ps1 without changing the PowerShell execution policy
powershell -NoProfile -ExecutionPolicy Bypass -File "%~dp0scripts\start-staging.ps1" %*
exit /b %ERRORLEVEL%
