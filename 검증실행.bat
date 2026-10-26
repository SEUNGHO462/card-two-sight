@echo off
setlocal
cd /d "%~dp0"
call gradlew.bat test :lwjgl3:installDist --console=plain
if errorlevel 1 goto failed
java -Dfile.encoding=UTF-8 -Dc2s.noAudio=true "-Dc2s.dataDir=%~dp0.runtime\smoke-data" "-Dc2s.smokeDir=%~dp0.runtime\preview" -cp "lwjgl3\build\install\CARD_TWO_SIGHT\lib\*" kr.ac.c2s.lwjgl3.DesktopLauncher --smoke
if errorlevel 1 goto failed
if exist ".runtime\preview\FAILED.txt" goto failed
if not exist ".runtime\preview\PASS.txt" goto failed
type ".runtime\preview\PASS.txt"
pause
exit /b 0
:failed
echo Validation failed. See the message above.
pause
exit /b 1
