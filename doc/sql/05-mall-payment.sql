-- mall_payment 数据库
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
