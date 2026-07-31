@echo off
title Start Frontend
cd /d %~dp0..
echo Starting frontend...

if not exist "mall-web\node_modules" (
    cd mall-web
    echo Installing dependencies...
    call npm install
    cd %~dp0..
)

start "Mall-Frontend" cmd /c "cd /d %~dp0..\mall-web && npm run dev"
echo Frontend started at http://localhost:5173

timeout /t 3 /nobreak >nul
start http://localhost:5173
exit
