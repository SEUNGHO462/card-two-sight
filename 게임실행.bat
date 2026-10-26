@echo off
setlocal
cd /d "%~dp0"
call gradlew.bat :lwjgl3:installDist --console=plain
if errorlevel 1 goto failed
call "lwjgl3\build\install\CARD_TWO_SIGHT\bin\CARD_TWO_SIGHT.bat" %*
if errorlevel 1 goto failed
exit /b 0
:failed
echo Game could not start. See the message above. Java 17+ is required.
pause
exit /b 1
