@echo off
rem Luma staging: wrapper to run scripts\setup-staging.ps1 without changing the PowerShell execution policy
powershell -NoProfile -ExecutionPolicy Bypass -File "%~dp0scripts\setup-staging.ps1" %*
exit /b %ERRORLEVEL%
