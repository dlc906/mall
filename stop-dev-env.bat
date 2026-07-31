@echo off
title Shutdown Middleware

echo ========================================
echo   Shutting Down All Middleware...
echo ========================================

:: 1. Nacos (port 8848)
echo [1/6] Stopping Nacos...
for /f "tokens=5" %%a in ('netstat -ano ^| findstr ":8848"') do (
    taskkill /f /pid %%a >nul 2>&1
)
echo    Nacos stopped.

:: 2. Redis
echo [2/6] Stopping Redis...
taskkill /f /im redis-server.exe >nul 2>&1
echo    Redis stopped.

:: 3. RocketMQ NameServer (port 9876)
echo [3/6] Stopping NameServer...
for /f "tokens=5" %%a in ('netstat -ano ^| findstr ":9876"') do (
    taskkill /f /pid %%a >nul 2>&1
)
echo    NameServer stopped.

:: 4. RocketMQ Broker (port 10911)
echo [4/6] Stopping Broker...
for /f "tokens=5" %%a in ('netstat -ano ^| findstr ":10911"') do (
    taskkill /f /pid %%a >nul 2>&1
)
echo    Broker stopped.

:: 5. Seata Server (port 8091)
echo [5/6] Stopping Seata...
for /f "tokens=5" %%a in ('netstat -ano ^| findstr ":8091"') do (
    taskkill /f /pid %%a >nul 2>&1
)
echo    Seata stopped.

:: 6. XXL-Job Admin (port 8100)
echo [6/6] Stopping XXL-Job...
for /f "tokens=5" %%a in ('netstat -ano ^| findstr ":8100"') do (
    taskkill /f /pid %%a >nul 2>&1
)
echo    XXL-Job stopped.

:: 7. Sentinel Dashboard (port 18080)
echo [7/7] Stopping Sentinel Dashboard...
for /f "tokens=5" %%a in ('netstat -ano ^| findstr ":18080"') do (
    taskkill /f /pid %%a >nul 2>&1
)
echo    Sentinel Dashboard stopped.

echo ========================================
echo   All middleware stopped.
echo ========================================
pause
