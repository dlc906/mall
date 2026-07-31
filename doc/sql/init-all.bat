@echo off
chcp 65001 >nul
setlocal enabledelayedexpansion

:: ============================================
::  Mall 数据库初始化脚本 (Windows)
::  按顺序执行所有 SQL 文件
:: ============================================

echo.
echo  ========================================
echo    Mall 数据库一键初始化
echo  ========================================
echo.

:: 获取 MySQL 连接信息
set /p DB_HOST="Please input MySQL Host [default: localhost]: "
if "!DB_HOST!"=="" set DB_HOST=localhost

set /p DB_PORT="Please input MySQL Port [default: 3306]: "
if "!DB_PORT!"=="" set DB_PORT=3306

set /p DB_USER="Please input MySQL User [default: root]: "
if "!DB_USER!"=="" set DB_USER=root

:: 隐藏密码输入 (使用 PowerShell)
echo Please input MySQL Password:
for /f "delims=" %%i in ('powershell -Command "$pwd = Read-Host -AsSecureString; [Runtime.InteropServices.Marshal]::PtrToStringAuto([Runtime.InteropServices.Marshal]::SecureStringToBSTR($pwd))"') do set DB_PASS=%%i

if "!DB_PASS!"=="" (
    set MYSQL_CMD=mysql -h!DB_HOST! -P!DB_PORT! -u!DB_USER!
) else (
    set MYSQL_CMD=mysql -h!DB_HOST! -P!DB_PORT! -u!DB_USER! -p!DB_PASS!
)

:: 获取脚本所在目录
set SCRIPT_DIR=%~dp0

echo.
echo  ========================================
echo   开始执行 SQL 文件...
echo  ========================================
echo.

:: 定义 SQL 文件列表
set SQL_FILES=01-init-databases.sql 02-mall-user.sql 03-mall-product.sql 04-mall-order.sql 05-mall-payment.sql 06-mall-distribution.sql

set SUCCESS_COUNT=0
set FAIL_COUNT=0

for %%f in (%SQL_FILES%) do (
    set FILE_PATH=!SCRIPT_DIR!%%f

    if exist "!FILE_PATH!" (
        echo [执行] %%f ...
        !MYSQL_CMD! < "!FILE_PATH!" 2>nul

        if !ERRORLEVEL! EQU 0 (
            echo   [成功] %%f 执行完成.
            set /a SUCCESS_COUNT+=1
        ) else (
            echo   [失败] %%f 执行失败!
            set /a FAIL_COUNT+=1
        )
    ) else (
        echo   [跳过] %%f 文件不存在: !FILE_PATH!
        set /a FAIL_COUNT+=1
    )
    echo.
)

echo  ========================================
echo   执行完毕: 成功 !SUCCESS_COUNT! 个, 失败 !FAIL_COUNT! 个
echo  ========================================
echo.

pause
