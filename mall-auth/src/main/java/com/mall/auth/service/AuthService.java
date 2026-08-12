package com.mall.auth.service;

import com.mall.auth.model.req.LoginReq;
import com.mall.auth.model.req.RegisterReq;
import com.mall.auth.model.resp.CaptchaResp;
import com.mall.auth.model.resp.LoginResp;

public interface AuthService {
    LoginResp login(LoginReq req);
    CaptchaResp generateCaptcha();
    LoginResp refresh(String refreshToken);
    void logout(String accessToken);
    LoginResp register(RegisterReq req);
}
