@echo off
setlocal
cd /d "%~dp0"
powershell.exe -NoProfile -ExecutionPolicy Bypass -File "tools\start_database.ps1"
if errorlevel 1 goto failed
call gradlew.bat :server:installDist --console=plain
if errorlevel 1 goto failed
call "server\build\install\server\bin\server.bat" %*
if errorlevel 1 goto failed
exit /b 0
:failed
echo Server could not start. See the message above. Java 17+ is required.
pause
exit /b 1
