-- ============================================
-- 商城微服务 - 数据库初始化脚本
-- 用法: 逐条执行 (Navicat: 选中一条 → 运行已选择的)
-- 或: mysql -u root -p < init-databases.sql
-- ============================================

-- 1. 创建所有数据库 (逐条执行!)
CREATE DATABASE IF NOT EXISTS mall_user DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci;
CREATE DATABASE IF NOT EXISTS mall_product DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci;
CREATE DATABASE IF NOT EXISTS mall_order DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci;
CREATE DATABASE IF NOT EXISTS mall_payment DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci;
CREATE DATABASE IF NOT EXISTS mall_distribution DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci;
