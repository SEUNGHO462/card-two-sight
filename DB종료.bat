@echo off
setlocal
cd /d "%~dp0"
".tools\mysql\bin\mysqladmin.exe" --defaults-extra-file=".runtime\mysql\admin.cnf" shutdown
if errorlevel 1 pause
