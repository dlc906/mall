package com.mall.user.model.resp;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class UserInfoResp {
    private Long id;
    private String username;
    private String phone;
    private String email;
    private String avatar;
    private String nickname;
    private Integer points;
    private String inviteCode;
}
