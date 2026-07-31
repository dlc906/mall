-- mall_product 数据库
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

-- 分类数据
INSERT INTO mall_category (id, name, parent_id, level, sort, status) VALUES
(1, '数码产品', 0, 1, 1, 1),
(2, '服装鞋帽', 0, 1, 2, 1),
(3, '食品饮料', 0, 1, 3, 1),
(4, '手机', 1, 2, 1, 1),
(5, '电脑', 1, 2, 2, 1),
(6, '男装', 2, 2, 1, 1),
(7, '女装', 2, 2, 2, 1);

-- 商品数据
INSERT INTO mall_product (id, name, category_id, title, description, main_image, price, original_price, stock, sales, status, keywords) VALUES
(1, 'iPhone 15 Pro Max 256GB', 4, '苹果旗舰手机', 'A17 Pro芯片，钛金属设计', 'https://placehold.co/300x300/EEE/999?text=iPhone', 9999.00, 10999.00, 100, 2560, 1, '手机,苹果,iPhone'),
(2, 'MacBook Pro 14英寸 M3', 5, '专业级笔记本电脑', 'Apple M3芯片，Liquid Retina XDR显示屏', 'https://placehold.co/300x300/EEE/999?text=MacBook', 14999.00, 16999.00, 50, 890, 1, '电脑,苹果,MacBook'),
(3, '华为 Mate 60 Pro', 4, '华为旗舰手机', '麒麟9000S芯片，卫星通话', 'https://placehold.co/300x300/EEE/999?text=Mate60', 6999.00, 7999.00, 80, 3200, 1, '手机,华为,Mate'),
(4, '男士商务休闲夹克', 6, '春秋季新款', '优质面料，修身版型', 'https://placehold.co/300x300/EEE/999?text=Jacket', 399.00, 699.00, 500, 1200, 1, '男装,夹克,商务'),
(5, '女士连衣裙 2024新款', 7, '夏季碎花连衣裙', '轻盈雪纺面料，法式复古风格', 'https://placehold.co/300x300/EEE/999?text=Dress', 299.00, 499.00, 300, 4500, 1, '女装,连衣裙,碎花'),
(6, '三只松鼠坚果礼盒', 3, '零食大礼包', '每日坚果混合装，健康美味', 'https://placehold.co/300x300/EEE/999?text=Nuts', 129.00, 199.00, 1000, 8900, 1, '食品,坚果,零食'),
(7, '联想 ThinkPad X1 Carbon', 5, '商务旗舰笔记本', '第13代酷睿i7，14英寸OLED屏', 'https://placehold.co/300x300/EEE/999?text=ThinkPad', 9999.00, 12999.00, 30, 670, 1, '电脑,联想,ThinkPad'),
(8, '小米14 Ultra', 4, '徕卡光学 专业影像旗舰', '骁龙8 Gen 3，徕卡全焦段四摄', 'https://placehold.co/300x300/EEE/999?text=Xiaomi14', 5999.00, 6499.00, 120, 5600, 1, '手机,小米,徕卡');

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
