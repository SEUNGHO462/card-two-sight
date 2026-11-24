@echo off
setlocal
cd /d "%~dp0"
chcp 65001 >nul
powershell.exe -NoProfile -ExecutionPolicy Bypass -File "tools\start_database.ps1"
if errorlevel 1 goto done
call gradlew.bat :server:installDist --console=plain
if errorlevel 1 goto done
java -Dfile.encoding=UTF-8 -cp "server\build\install\server\lib\*" kr.ac.c2s.server.DatabaseRecords
:done
pause
