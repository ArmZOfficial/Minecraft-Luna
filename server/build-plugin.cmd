@echo off
rem Luma staging: wrapper to run scripts\build-plugin.ps1 without changing the PowerShell execution policy
powershell -NoProfile -ExecutionPolicy Bypass -File "%~dp0scripts\build-plugin.ps1" %*
exit /b %ERRORLEVEL%
