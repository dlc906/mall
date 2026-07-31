package com.mall.auth.service.impl;

import cn.hutool.core.util.IdUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.crypto.SecureUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.mall.auth.entity.User;
import com.mall.auth.mapper.UserMapper;
import com.mall.auth.model.req.LoginReq;
import com.mall.auth.model.req.RegisterReq;
import com.mall.auth.model.resp.LoginResp;
import com.mall.auth.service.AuthService;
import com.mall.common.constant.CommonConstants;
import com.mall.common.constant.RedisKey;
import com.mall.common.exception.BizException;
import com.mall.common.utils.JwtUtils;
import com.mall.common.utils.RedisUtils;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.Resource;
import java.util.concurrent.TimeUnit;

@Slf4j
@Service
public class AuthServiceImpl implements AuthService {

    @Resource
    private UserMapper userMapper;
    @Resource
    private RedisUtils redisUtils;

    @Override
    public LoginResp login(LoginReq req) {
        User user = userMapper.selectOne(new LambdaQueryWrapper<User>()
                .eq(User::getUsername, req.getUsername()));
        if (user == null) {
            throw new BizException("用户名或密码错误");
        }
        if (user.getStatus() != null && user.getStatus() == 0) {
            throw new BizException("账号已被禁用");
        }
        // BCrypt-alike: MD5 for simplicity (production use BCrypt)
        String encryptedPassword = SecureUtil.md5(req.getPassword());
        if (!encryptedPassword.equals(user.getPassword())) {
            throw new BizException("用户名或密码错误");
        }
        return buildLoginResp(user);
    }

    @Override
    public LoginResp refresh(String refreshToken) {
        Long userId = JwtUtils.getUserId(refreshToken);
        if (userId == null) {
            throw new BizException(CommonConstants.UNAUTHORIZED, "RefreshToken无效");
        }
        // Check if refresh token exists in Redis
        String redisKey = RedisKey.USER_REFRESH_TOKEN + userId;
        String savedToken = redisUtils.get(redisKey, String.class);
        if (!refreshToken.equals(savedToken)) {
            throw new BizException(CommonConstants.UNAUTHORIZED, "RefreshToken已过期，请重新登录");
        }
        User user = userMapper.selectById(userId);
        if (user == null) {
            throw new BizException("用户不存在");
        }
        return buildLoginResp(user);
    }

    @Override
    public void logout(String accessToken) {
        Long userId = JwtUtils.getUserId(accessToken);
        if (userId != null) {
            // Clear refresh token from Redis
            redisUtils.delete(RedisKey.USER_REFRESH_TOKEN + userId);
        }
        // Add accessToken to blacklist
        long expireSeconds = JwtUtils.ACCESS_TOKEN_EXPIRE / 1000;
        redisUtils.set(RedisKey.TOKEN_BLACKLIST + accessToken, "1", expireSeconds, TimeUnit.SECONDS);
    }

    @Override
    @Transactional
    public LoginResp register(RegisterReq req) {
        // Check username uniqueness
        Long count = userMapper.selectCount(new LambdaQueryWrapper<User>()
                .eq(User::getUsername, req.getUsername()));
        if (count > 0) {
            throw new BizException("用户名已存在");
        }

        User user = new User();
        user.setUsername(req.getUsername());
        user.setPassword(SecureUtil.md5(req.getPassword()));
        user.setPhone(req.getPhone());
        user.setNickname(req.getUsername());
        user.setStatus(1);
        user.setInviteCode(IdUtil.fastSimpleUUID().substring(0, 8));
        user.setAvatar("https://api.dicebear.com/7.x/initials/svg?seed=" + req.getUsername());

        // Handle invite code - bind distributor relationship
        if (StrUtil.isNotBlank(req.getInviteCode())) {
            User inviter = userMapper.selectOne(new LambdaQueryWrapper<User>()
                    .eq(User::getInviteCode, req.getInviteCode()));
            if (inviter != null) {
                user.setParentInviterId(inviter.getId());
            }
        }

        userMapper.insert(user);
        return buildLoginResp(user);
    }

    private LoginResp buildLoginResp(User user) {
        String accessToken = JwtUtils.createAccessToken(user.getId(), user.getUsername());
        String refreshToken = JwtUtils.createRefreshToken(user.getId(), user.getUsername());

        // Save refresh token to Redis
        String redisKey = RedisKey.USER_REFRESH_TOKEN + user.getId();
        redisUtils.set(redisKey, refreshToken, JwtUtils.REFRESH_TOKEN_EXPIRE, TimeUnit.MILLISECONDS);

        // Save access token to Redis for SSO
        String tokenKey = RedisKey.USER_TOKEN + user.getId();
        redisUtils.set(tokenKey, accessToken, JwtUtils.ACCESS_TOKEN_EXPIRE, TimeUnit.MILLISECONDS);

        return LoginResp.builder()
                .userId(user.getId())
                .username(user.getUsername())
                .accessToken(accessToken)
                .refreshToken(refreshToken)
                .expiresIn(JwtUtils.ACCESS_TOKEN_EXPIRE / 1000)
                .build();
    }
}
