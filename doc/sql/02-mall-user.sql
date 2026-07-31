-- mall_user 数据库
USE mall_user;

CREATE TABLE IF NOT EXISTS mall_user (
    id BIGINT NOT NULL COMMENT '用户ID',
    username VARCHAR(50) NOT NULL COMMENT '用户名',
    password VARCHAR(200) NOT NULL COMMENT '密码(MD5加密)',
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

-- 测试用户 (密码: 123456, MD5: e10adc3949ba59abbe56e057f20f883e)
INSERT INTO mall_user (id, username, password, nickname, status, invite_code) VALUES
(1, 'admin', 'e10adc3949ba59abbe56e057f20f883e', '管理员', 1, 'ADMIN001'),
(2, 'test', 'e10adc3949ba59abbe56e057f20f883e', '测试用户', 1, 'TEST001');
