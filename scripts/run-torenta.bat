@echo off
setlocal

set "SCRIPT_DIR=%~dp0"
set "APP_DIR=%SCRIPT_DIR%app"

if not exist "%APP_DIR%" (
  echo Portable layout not found. Expected app\ next to this script.
  exit /b 1
)

for %%f in ("%APP_DIR%\*.jar") do (
  set "APP_JAR=%%f"
  goto :jar_found
)

echo No backend jar found in %APP_DIR%.
exit /b 1

:jar_found
"%SCRIPT_DIR%runtime\bin\java.exe" -jar "%APP_JAR%"
