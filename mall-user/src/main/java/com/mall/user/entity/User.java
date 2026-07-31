package com.mall.user.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.mall.common.entity.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("mall_user")
public class User extends BaseEntity {
    private String username;
    private String password;
    private String phone;
    private String email;
    private String avatar;
    private String nickname;
    private Integer status;
    private Integer points;
    private String inviteCode;
    private Long parentInviterId;
}
