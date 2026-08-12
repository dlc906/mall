package com.mall.auth.model.resp;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CaptchaResp {
    /** 验证码唯一标识（登录时需回传） */
    private String captchaKey;
    /** 验证码图片（base64，前端直接用于 <img src>） */
    private String captchaImg;
}
