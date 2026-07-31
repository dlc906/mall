# ============================================
# 开发环境一键启动脚本
# 启动: Nacos + Redis + RocketMQ
# 用法: 右键 → 使用 PowerShell 运行
# ============================================

Write-Host "========================================" -ForegroundColor Cyan
Write-Host "  开发环境启动中..." -ForegroundColor Cyan
Write-Host "========================================" -ForegroundColor Cyan

# ========== 1. Nacos (新窗口) ==========
Write-Host "[1/3] 启动 Nacos..." -ForegroundColor Yellow
$nacosDir = "E:\javatool\nacos-server-2.2.3"
Start-Process -FilePath "$nacosDir\bin\startup.cmd" -ArgumentList "-m standalone" -WindowStyle Normal
Write-Host "   Nacos (http://127.0.0.1:8848/nacos)" -ForegroundColor Green

# ========== 2. Redis ==========
Write-Host "[2/3] 启动 Redis..." -ForegroundColor Yellow
$redisDir = "E:\javatool\Redis-8.8.1-Windows-x64-msys2\Redis-8.8.1-Windows-x64-msys2"
Start-Process -FilePath "$redisDir\redis-server.exe" -WorkingDirectory $redisDir -WindowStyle Normal
Write-Host "   Redis (127.0.0.1:6379)" -ForegroundColor Green

# ========== 3. RocketMQ (两个窗口) ==========
Write-Host "[3/3] 启动 RocketMQ..." -ForegroundColor Yellow
$rocketDir = "E:\javatool\rocketmq-all-5.5.0-bin-release\rocketmq-all-5.5.0-bin-release"

# 设置临时环境变量
$env:ROCKETMQ_HOME = $rocketDir

# NameServer
Start-Process -FilePath "$rocketDir\bin\mqnamesrv.cmd" -WorkingDirectory "$rocketDir\bin" -WindowStyle Normal
Write-Host "   NameServer (127.0.0.1:9876)" -ForegroundColor Green

Start-Sleep -Seconds 3

# Broker (带配置文件，自动创建 Topic)
Start-Process -FilePath "$rocketDir\bin\mqbroker.cmd" -ArgumentList "-n 127.0.0.1:9876 -c ..\conf\broker.conf" -WorkingDirectory "$rocketDir\bin" -WindowStyle Normal
Write-Host "   Broker (127.0.0.1:10911)" -ForegroundColor Green

Write-Host "========================================" -ForegroundColor Cyan
Write-Host "  所有服务已启动！共打开 4 个窗口" -ForegroundColor Cyan
Write-Host "  请等待 10-15 秒后启动您的应用" -ForegroundColor Cyan
Write-Host "========================================" -ForegroundColor Cyan

# 打开 Nacos 管理页面
Start-Sleep -Seconds 8
Start-Process "http://127.0.0.1:8848/nacos"
