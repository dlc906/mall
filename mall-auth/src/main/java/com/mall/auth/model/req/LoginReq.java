package com.mall.auth.model.req;

import lombok.Data;
import javax.validation.constraints.NotBlank;

@Data
public class LoginReq {
    @NotBlank(message = "用户名不能为空")
    private String username;
    @NotBlank(message = "密码不能为空")
    private String password;
    /** 验证码标识（先调用 /api/auth/captcha 获取） */
    private String captchaKey;
    /** 验证码内容 */
    private String captchaCode;
}
