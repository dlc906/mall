@echo off
title Dev Environment Starter

echo ========================================
echo   Starting Dev Environment...
echo ========================================

:: Helper: check if port is listening, start if not
set "NACOS_PORT=8848"
set "REDIS_PORT=6379"
set "NAMESRV_PORT=9876"
set "BROKER_PORT=10911"
set "SEATA_PORT=8091"
set "XXL_PORT=8100"
set "SENTINEL_PORT=18080"

:: 1. Nacos
echo [1/7] Checking Nacos...
netstat -ano | findstr ":%NACOS_PORT% " | findstr "LISTENING" >nul 2>&1
if %errorlevel% equ 0 (
    echo    Nacos already running.
) else (
    start "Nacos" cmd /c "E:\javatool\nacos-server-2.2.3\nacos\bin\startup.cmd -m standalone"
    echo    Nacos starting...
)

:: 2. Redis
echo [2/7] Checking Redis...
netstat -ano | findstr ":%REDIS_PORT% " | findstr "LISTENING" >nul 2>&1
if %errorlevel% equ 0 (
    echo    Redis already running.
) else (
    start "Redis" "E:\javatool\Redis-8.8.1-Windows-x64-msys2\Redis-8.8.1-Windows-x64-msys2\redis-server.exe"
    echo    Redis starting...
)

:: 3. RocketMQ NameServer
echo [3/7] Checking NameServer...
netstat -ano | findstr ":%NAMESRV_PORT% " | findstr "LISTENING" >nul 2>&1
if %errorlevel% equ 0 (
    echo    NameServer already running.
) else (
    set ROCKETMQ_HOME=E:\javatool\rocketmq-all-5.5.0-bin-release\rocketmq-all-5.5.0-bin-release
    start "RocketMQ-NameServer" cmd /c "cd /d %ROCKETMQ_HOME%\bin && mqnamesrv.cmd"
    echo    NameServer starting...
)

:: 4. RocketMQ Broker (wait for NameServer first)
echo [4/7] Checking Broker...
netstat -ano | findstr ":%BROKER_PORT% " | findstr "LISTENING" >nul 2>&1
if %errorlevel% equ 0 (
    echo    Broker already running.
) else (
    timeout /t 3 /nobreak >nul
    start "RocketMQ-Broker" cmd /c "cd /d %ROCKETMQ_HOME%\bin && mqbroker.cmd -n 127.0.0.1:9876 -c ..\conf\broker.conf"
    echo    Broker starting...
)

:: 5. Seata Server
echo [5/7] Checking Seata...
netstat -ano | findstr ":%SEATA_PORT% " | findstr "LISTENING" >nul 2>&1
if %errorlevel% equ 0 (
    echo    Seata already running.
) else (
    start "Seata" cmd /c "E:\javatool\seata-server-2.0.0\seata\bin\seata-server.bat -p 8091"
    echo    Seata starting...
)

:: 6. XXL-Job Admin
echo [6/7] Checking XXL-Job...
netstat -ano | findstr ":%XXL_PORT% " | findstr "LISTENING" >nul 2>&1
if %errorlevel% equ 0 (
    echo    XXL-Job already running.
) else (
    start "XXL-Job" cmd /c "java -jar E:\javatool\xxl-job-3.4.2\xxl-job-3.4.2\xxl-job-admin\target\xxl-job-admin-3.4.2.jar --server.port=8100"
    echo    XXL-Job starting...
)

:: 7. Sentinel Dashboard
echo [7/7] Checking Sentinel Dashboard...
netstat -ano | findstr ":%SENTINEL_PORT% " | findstr "LISTENING" >nul 2>&1
if %errorlevel% equ 0 (
    echo    Sentinel Dashboard already running.
) else (
    start "Sentinel" cmd /c "java -jar E:\javatool\sentinel\sentinel-dashboard-1.8.10.jar --server.port=18080"
    echo    Sentinel Dashboard starting...
)

echo ========================================
echo   Done.
echo ========================================

timeout /t 5 /nobreak >nul
start http://127.0.0.1:8848/nacos
exit
