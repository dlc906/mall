package com.mall.auth.model.resp;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LoginResp {
    private Long userId;
    private String username;
    private String accessToken;
    private String refreshToken;
    private Long expiresIn;
}
