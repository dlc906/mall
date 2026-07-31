-- mall_distribution 数据库
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
