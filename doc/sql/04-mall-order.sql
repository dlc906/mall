-- mall_order 数据库
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
