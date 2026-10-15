
@echo off
setlocal
cd /d "%~dp0"

echo.
echo ========================================
echo          Starting LifeAdmin
echo ========================================
echo.

echo Starting the Spring Boot server...
start "LifeAdmin Server" cmd /k "cd /d ""%~dp0"" && call mvnw.cmd spring-boot:run"

echo Waiting for LifeAdmin to become ready...
echo Maximum wait time: 60 seconds.
echo.

powershell -NoProfile -ExecutionPolicy Bypass -Command ^
  "$deadline = (Get-Date).AddSeconds(60); " ^
  "while ((Get-Date) -lt $deadline) { " ^
  "  try { " ^
  "    $r = Invoke-WebRequest -Uri 'http://localhost:8080/' -UseBasicParsing -TimeoutSec 2; " ^
  "    if ($r.StatusCode -eq 200) { exit 0 } " ^
  "  } catch { } " ^
  "  Start-Sleep -Seconds 2 " ^
  "}; exit 1"

if errorlevel 1 goto startupFailed

echo.
echo ========================================
echo          LifeAdmin is ready!
echo ========================================
echo Opening LifeAdmin in your browser...
start "" "http://localhost:8080/"
echo.
echo You can close this launcher window.
echo Keep the LifeAdmin Server window open.
echo.
pause
exit /b 0

:startupFailed
echo.
echo ========================================
echo          STARTUP FAILED
echo ========================================
echo.
echo LifeAdmin did not become ready within 60 seconds.
echo.
echo Please check the LifeAdmin Server window for errors.
echo.
echo Things to check:
echo   1. Is Java 21 installed and available?
echo   2. Is your internet connection working for Maven?
echo   3. Is port 8080 already being used?
echo   4. Did Spring Boot report an error?
echo.
echo Fix the problem, then run this file again.
echo.
pause
exit /b 1
