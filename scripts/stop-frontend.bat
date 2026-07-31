@echo off
title Stop Frontend

echo ========================================
echo   Stopping Frontend...
echo ========================================

:: Frontend (Vite dev server, port 5173)
echo [1/1] Stopping frontend dev server...
for /f "tokens=5" %%a in ('netstat -ano ^| findstr ":5173"') do (
    taskkill /f /pid %%a >nul 2>&1
)
echo    Frontend (port 5173) stopped.

echo ========================================
echo   Done.
echo ========================================
pause
