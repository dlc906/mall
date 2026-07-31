package com.mall.user.service;

import com.mall.user.model.req.UserRegisterReq;
import com.mall.user.model.resp.UserInfoResp;
import com.mall.user.entity.User;

public interface UserService {
    UserInfoResp register(UserRegisterReq req);
    UserInfoResp getUserInfo(Long userId);
    UserInfoResp updateUserInfo(Long userId, User user);
    UserInfoResp getUserByUsername(String username);
}
