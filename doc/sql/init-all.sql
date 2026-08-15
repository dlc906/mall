-- ============================================
-- 商城微服务 - 一键初始化脚本
-- 用法: mysql -u root -p < doc/sql/init-all.sql
-- 或直接在 GUI 工具中运行整个文件
-- ============================================

-- ========== 1. 创建所有数据库 ==========
CREATE DATABASE IF NOT EXISTS mall_user DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci;
CREATE DATABASE IF NOT EXISTS mall_product DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci;
CREATE DATABASE IF NOT EXISTS mall_order DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci;
CREATE DATABASE IF NOT EXISTS mall_payment DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci;
CREATE DATABASE IF NOT EXISTS mall_distribution DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci;

-- ========== 2. mall_user - 用户数据库 ==========
USE mall_user;

CREATE TABLE IF NOT EXISTS mall_user (
    id BIGINT NOT NULL COMMENT '用户ID',
    username VARCHAR(50) NOT NULL COMMENT '用户名',
    password VARCHAR(200) NOT NULL COMMENT '密码(BCrypt加密)',
    phone VARCHAR(20) DEFAULT NULL COMMENT '手机号',
    email VARCHAR(100) DEFAULT NULL COMMENT '邮箱',
    avatar VARCHAR(500) DEFAULT NULL COMMENT '头像URL',
    nickname VARCHAR(50) DEFAULT NULL COMMENT '昵称',
    status INT DEFAULT 1 COMMENT '状态: 0=禁用, 1=启用',
    points INT DEFAULT 0 COMMENT '积分',
    invite_code VARCHAR(20) DEFAULT NULL COMMENT '邀请码',
    parent_inviter_id BIGINT DEFAULT NULL COMMENT '邀请人ID',
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    deleted INT DEFAULT 0 COMMENT '逻辑删除: 0=未删除, 1=已删除',
    PRIMARY KEY (id),
    UNIQUE KEY uk_username (username),
    KEY idx_invite_code (invite_code),
    KEY idx_parent_inviter (parent_inviter_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='用户表';

CREATE TABLE IF NOT EXISTS mall_address (
    id BIGINT NOT NULL COMMENT '地址ID',
    user_id BIGINT NOT NULL COMMENT '用户ID',
    receiver_name VARCHAR(50) NOT NULL COMMENT '收货人姓名',
    receiver_phone VARCHAR(20) NOT NULL COMMENT '收货人手机号',
    province VARCHAR(50) DEFAULT NULL COMMENT '省份',
    city VARCHAR(50) DEFAULT NULL COMMENT '城市',
    district VARCHAR(50) DEFAULT NULL COMMENT '区/县',
    detail_address VARCHAR(200) NOT NULL COMMENT '详细地址',
    is_default INT DEFAULT 0 COMMENT '是否默认: 0=否, 1=是',
    tag VARCHAR(50) DEFAULT NULL COMMENT '地址标签(家/公司)',
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    deleted INT DEFAULT 0 COMMENT '逻辑删除',
    PRIMARY KEY (id),
    KEY idx_user_id (user_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='收货地址表';

SET NAMES utf8mb4;

-- 测试用户 (密码: 123456, BCrypt: $2a$10$dZY8D8r9SpAK7EZkTqcafOOHvD7uhGdDX9Zg/DGN18H7bhA7UDqx6)
INSERT IGNORE INTO mall_user (id, username, password, nickname, status, invite_code) VALUES
(1, 'admin', '$2a$10$dZY8D8r9SpAK7EZkTqcafOOHvD7uhGdDX9Zg/DGN18H7bhA7UDqx6', '管理员', 1, 'ADMIN001'),
(2, 'test', '$2a$10$dZY8D8r9SpAK7EZkTqcafOOHvD7uhGdDX9Zg/DGN18H7bhA7UDqx6', '测试用户', 1, 'TEST001');

-- ========== 3. mall_product - 商品数据库 ==========
USE mall_product;

CREATE TABLE IF NOT EXISTS mall_category (
    id BIGINT NOT NULL COMMENT '分类ID',
    name VARCHAR(50) NOT NULL COMMENT '分类名称',
    parent_id BIGINT DEFAULT 0 COMMENT '父分类ID, 0表示一级分类',
    level INT DEFAULT 1 COMMENT '层级',
    sort INT DEFAULT 0 COMMENT '排序',
    icon VARCHAR(200) DEFAULT NULL COMMENT '图标URL',
    status INT DEFAULT 1 COMMENT '状态: 0=隐藏, 1=显示',
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
    update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    deleted INT DEFAULT 0,
    PRIMARY KEY (id),
    KEY idx_parent_id (parent_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='商品分类表';

CREATE TABLE IF NOT EXISTS mall_product (
    id BIGINT NOT NULL COMMENT '商品ID',
    name VARCHAR(200) NOT NULL COMMENT '商品名称',
    category_id BIGINT DEFAULT NULL COMMENT '分类ID',
    title VARCHAR(500) DEFAULT NULL COMMENT '商品标题/副标题',
    description VARCHAR(1000) DEFAULT NULL COMMENT '商品描述',
    main_image VARCHAR(500) DEFAULT NULL COMMENT '主图URL',
    images TEXT DEFAULT NULL COMMENT '图片列表(JSON)',
    detail TEXT DEFAULT NULL COMMENT '商品详情(HTML)',
    price DECIMAL(10,2) NOT NULL DEFAULT 0.00 COMMENT '销售价格',
    original_price DECIMAL(10,2) DEFAULT NULL COMMENT '原价',
    stock INT NOT NULL DEFAULT 0 COMMENT '库存',
    sales INT DEFAULT 0 COMMENT '销量',
    status INT DEFAULT 1 COMMENT '状态: 0=下架, 1=上架',
    sort INT DEFAULT 0 COMMENT '排序',
    keywords VARCHAR(500) DEFAULT NULL COMMENT '搜索关键词',
    specs TEXT DEFAULT NULL COMMENT '规格(JSON)',
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
    update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    deleted INT DEFAULT 0,
    PRIMARY KEY (id),
    KEY idx_category_id (category_id),
    KEY idx_name (name),
    FULLTEXT KEY ft_keywords (name, keywords, description)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='商品表';

SET NAMES utf8mb4;

-- 分类数据
INSERT IGNORE INTO mall_category (id, name, parent_id, level, sort, status) VALUES
(1, '数码产品', 0, 1, 1, 1),
(2, '服装鞋帽', 0, 1, 2, 1),
(3, '食品饮料', 0, 1, 3, 1),
(4, '手机', 1, 2, 1, 1),
(5, '电脑', 1, 2, 2, 1),
(6, '男装', 2, 2, 1, 1),
(7, '女装', 2, 2, 2, 1);

SET NAMES utf8mb4;

-- 商品数据
INSERT IGNORE INTO mall_product (id, name, category_id, title, description, main_image, price, original_price, stock, sales, status, keywords) VALUES
(1, 'iPhone 15 Pro Max 256GB', 4, '苹果旗舰手机', 'A17 Pro芯片，钛金属设计', 'https://placehold.co/300x300/EEE/999?text=iPhone', 9999.00, 10999.00, 100, 2560, 1, '手机,苹果,iPhone'),
(2, 'MacBook Pro 14英寸 M3', 5, '专业级笔记本电脑', 'Apple M3芯片，Liquid Retina XDR显示屏', 'https://placehold.co/300x300/EEE/999?text=MacBook', 14999.00, 16999.00, 50, 890, 1, '电脑,苹果,MacBook'),
(3, '华为 Mate 60 Pro', 4, '华为旗舰手机', '麒麟9000S芯片，卫星通话', 'https://placehold.co/300x300/EEE/999?text=Mate60', 6999.00, 7999.00, 80, 3200, 1, '手机,华为,Mate'),
(4, '男士商务休闲夹克', 6, '春秋季新款', '优质面料，修身版型', 'https://placehold.co/300x300/EEE/999?text=Jacket', 399.00, 699.00, 500, 1200, 1, '男装,夹克,商务'),
(5, '女士连衣裙 2024新款', 7, '夏季碎花连衣裙', '轻盈雪纺面料，法式复古风格', 'https://placehold.co/300x300/EEE/999?text=Dress', 299.00, 499.00, 300, 4500, 1, '女装,连衣裙,碎花'),
(6, '三只松鼠坚果礼盒', 3, '零食大礼包', '每日坚果混合装，健康美味', 'https://placehold.co/300x300/EEE/999?text=Nuts', 129.00, 199.00, 1000, 8900, 1, '食品,坚果,零食'),
(7, '联想 ThinkPad X1 Carbon', 5, '商务旗舰笔记本', '第13代酷睿i7，14英寸OLED屏', 'https://placehold.co/300x300/EEE/999?text=ThinkPad', 9999.00, 12999.00, 30, 670, 1, '电脑,联想,ThinkPad'),
(8, '小米14 Ultra', 4, '徕卡光学 专业影像旗舰', '骁龙8 Gen 3，徕卡全焦段四摄', 'https://placehold.co/300x300/EEE/999?text=Xiaomi14', 5999.00, 6499.00, 120, 5600, 1, '手机,小米,徕卡');

-- Seata AT模式 undo_log 表 (mall_product数据库也需要)
USE mall_product;
CREATE TABLE IF NOT EXISTS undo_log (
    id BIGINT NOT NULL AUTO_INCREMENT,
    branch_id BIGINT NOT NULL,
    xid VARCHAR(100) NOT NULL,
    context VARCHAR(128) NOT NULL,
    rollback_info LONGBLOB NOT NULL,
    log_status INT NOT NULL,
    log_created DATETIME NOT NULL,
    log_modified DATETIME NOT NULL,
    ext VARCHAR(100) DEFAULT NULL,
    PRIMARY KEY (id),
    UNIQUE KEY ux_undo_log (xid, branch_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='Seata AT模式undo_log表';
USE mall_order;

CREATE TABLE IF NOT EXISTS mall_order (
    id BIGINT NOT NULL COMMENT '订单ID',
    order_no VARCHAR(64) NOT NULL COMMENT '订单号',
    user_id BIGINT NOT NULL COMMENT '用户ID',
    status INT DEFAULT 0 COMMENT '订单状态: 0=待支付, 1=已支付, 2=已发货, 3=已完成, 4=已取消, 5=退款中, 6=已退款',
    total_amount DECIMAL(10,2) NOT NULL DEFAULT 0.00 COMMENT '订单总金额',
    pay_amount DECIMAL(10,2) NOT NULL DEFAULT 0.00 COMMENT '实付金额',
    discount_amount DECIMAL(10,2) DEFAULT 0.00 COMMENT '优惠金额',
    freight_amount DECIMAL(10,2) DEFAULT 0.00 COMMENT '运费',
    receiver_name VARCHAR(50) DEFAULT NULL COMMENT '收货人',
    receiver_phone VARCHAR(20) DEFAULT NULL COMMENT '收货人手机号',
    receiver_address VARCHAR(500) DEFAULT NULL COMMENT '收货地址',
    remark VARCHAR(500) DEFAULT NULL COMMENT '订单备注',
    pay_time DATETIME DEFAULT NULL COMMENT '支付时间',
    ship_time DATETIME DEFAULT NULL COMMENT '发货时间',
    complete_time DATETIME DEFAULT NULL COMMENT '完成时间',
    cancel_time DATETIME DEFAULT NULL COMMENT '取消时间',
    cancel_reason VARCHAR(200) DEFAULT NULL COMMENT '取消原因',
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
    update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    deleted INT DEFAULT 0,
    PRIMARY KEY (id),
    UNIQUE KEY uk_order_no (order_no),
    KEY idx_user_id (user_id),
    KEY idx_status (status),
    KEY idx_create_time (create_time)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='订单表';

CREATE TABLE IF NOT EXISTS mall_order_item (
    id BIGINT NOT NULL COMMENT '订单项ID',
    order_id BIGINT NOT NULL COMMENT '订单ID',
    order_no VARCHAR(64) NOT NULL COMMENT '订单号',
    product_id BIGINT NOT NULL COMMENT '商品ID',
    product_name VARCHAR(200) DEFAULT NULL COMMENT '商品名称',
    product_image VARCHAR(500) DEFAULT NULL COMMENT '商品图片',
    product_price DECIMAL(10,2) NOT NULL COMMENT '商品单价',
    quantity INT NOT NULL DEFAULT 1 COMMENT '购买数量',
    total_price DECIMAL(10,2) NOT NULL COMMENT '小计金额',
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
    update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    deleted INT DEFAULT 0,
    PRIMARY KEY (id),
    KEY idx_order_id (order_id),
    KEY idx_order_no (order_no)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='订单项表';

-- Seata AT模式 undo_log 表 (分布式事务必需)
CREATE TABLE IF NOT EXISTS undo_log (
    id BIGINT NOT NULL AUTO_INCREMENT,
    branch_id BIGINT NOT NULL,
    xid VARCHAR(100) NOT NULL,
    context VARCHAR(128) NOT NULL,
    rollback_info LONGBLOB NOT NULL,
    log_status INT NOT NULL,
    log_created DATETIME NOT NULL,
    log_modified DATETIME NOT NULL,
    ext VARCHAR(100) DEFAULT NULL,
    PRIMARY KEY (id),
    UNIQUE KEY ux_undo_log (xid, branch_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='Seata AT模式undo_log表';

-- ========== 5. mall_payment - 支付数据库 ==========
USE mall_payment;

CREATE TABLE IF NOT EXISTS mall_payment_record (
    id BIGINT NOT NULL COMMENT '支付记录ID',
    payment_no VARCHAR(64) NOT NULL COMMENT '支付单号',
    order_no VARCHAR(64) NOT NULL COMMENT '订单号',
    user_id BIGINT NOT NULL COMMENT '用户ID',
    amount DECIMAL(10,2) NOT NULL COMMENT '金额',
    pay_type INT DEFAULT 0 COMMENT '类型: 0=支付, 1=退款',
    pay_method INT DEFAULT 1 COMMENT '方式: 1=模拟支付, 2=微信, 3=支付宝',
    status INT DEFAULT 0 COMMENT '状态: 0=处理中, 1=成功, 2=失败',
    trade_no VARCHAR(100) DEFAULT NULL COMMENT '第三方交易号',
    pay_time DATETIME DEFAULT NULL COMMENT '支付时间',
    remark VARCHAR(200) DEFAULT NULL COMMENT '备注',
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
    update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    deleted INT DEFAULT 0,
    PRIMARY KEY (id),
    UNIQUE KEY uk_payment_no (payment_no),
    KEY idx_order_no (order_no),
    UNIQUE KEY uk_order_pay_type (order_no, pay_type),
    KEY idx_user_id (user_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='支付记录表';

-- ========== 6. mall_distribution - 分销数据库 ==========
USE mall_distribution;

CREATE TABLE IF NOT EXISTS mall_distribution_relationship (
    id BIGINT NOT NULL COMMENT '关系ID',
    user_id BIGINT NOT NULL COMMENT '下级用户ID',
    parent_id BIGINT DEFAULT NULL COMMENT '一级上级ID',
    grandparent_id BIGINT DEFAULT NULL COMMENT '二级上级ID',
    level INT DEFAULT NULL COMMENT '分销层级',
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
    update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    deleted INT DEFAULT 0,
    PRIMARY KEY (id),
    KEY idx_user_id (user_id),
    KEY idx_parent_id (parent_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='分销关系表';

CREATE TABLE IF NOT EXISTS mall_commission_record (
    id BIGINT NOT NULL COMMENT '佣金记录ID',
    user_id BIGINT NOT NULL COMMENT '获得佣金的用户ID',
    order_no VARCHAR(64) NOT NULL COMMENT '关联订单号',
    order_id BIGINT DEFAULT NULL COMMENT '关联订单ID',
    buyer_user_id BIGINT DEFAULT NULL COMMENT '下单用户ID',
    order_amount DECIMAL(10,2) NOT NULL COMMENT '订单金额',
    commission_ratio DECIMAL(5,2) NOT NULL COMMENT '佣金比例(%)',
    commission_amount DECIMAL(10,2) NOT NULL COMMENT '佣金金额',
    level INT DEFAULT 1 COMMENT '分销层级: 1=一级, 2=二级',
    status INT DEFAULT 0 COMMENT '状态: 0=待结算, 1=已结算, 2=已提现',
    settle_month VARCHAR(7) DEFAULT NULL COMMENT '结算月份',
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
    update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    deleted INT DEFAULT 0,
    PRIMARY KEY (id),
    KEY idx_user_id (user_id),
    KEY idx_order_no (order_no),
    KEY idx_settle_month (settle_month)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='佣金记录表';
