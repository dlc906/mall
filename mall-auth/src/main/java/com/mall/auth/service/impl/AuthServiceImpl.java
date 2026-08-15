package com.mall.auth.service.impl;

import cn.hutool.captcha.CaptchaUtil;
import cn.hutool.captcha.LineCaptcha;
import cn.hutool.core.util.IdUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.crypto.SecureUtil;
import cn.hutool.crypto.digest.BCrypt;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.mall.auth.entity.User;
import com.mall.auth.mapper.UserMapper;
import com.mall.auth.model.req.LoginReq;
import com.mall.auth.model.req.RegisterReq;
import com.mall.auth.model.resp.CaptchaResp;
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

    /** 验证码有效期（分钟） */
    private static final long CAPTCHA_EXPIRE_MINUTES = 5;
    /** 最大登录失败次数 */
    private static final int MAX_LOGIN_FAIL = 5;
    /** 失败锁定时间（分钟） */
    private static final long LOCK_MINUTES = 15;

    @Resource
    private UserMapper userMapper;
    @Resource
    private RedisUtils redisUtils;

    @Override
    public LoginResp login(LoginReq req) {
        // 1. 校验验证码（防机器人/暴力破解）
        verifyCaptcha(req);

        // 2. 检查账号是否已被锁定（连续失败超限）
        String failKey = RedisKey.RATE_LIMIT + "login:fail:" + req.getUsername();
        Long failCount = redisUtils.get(failKey, Long.class);
        if (failCount != null && failCount >= MAX_LOGIN_FAIL) {
            long remain = redisUtils.getExpire(failKey, TimeUnit.MINUTES);
            throw new BizException("登录失败次数过多，请 " + Math.max(remain, 1) + " 分钟后再试");
        }

        // 3. 校验用户与密码
        User user = userMapper.selectOne(new LambdaQueryWrapper<User>()
                .eq(User::getUsername, req.getUsername()));
        boolean authOk = false;
        if (user != null && (user.getStatus() == null || user.getStatus() != 0)) {
            authOk = checkPassword(req.getPassword(), user);
        }

        if (!authOk) {
            // 4. 失败计数 + 锁定
            long count = redisUtils.increment(failKey, 1);
            if (count == 1) {
                redisUtils.expire(failKey, LOCK_MINUTES, TimeUnit.MINUTES);
            }
            if (count >= MAX_LOGIN_FAIL) {
                log.warn("Login account locked: username={}, failCount={}", req.getUsername(), count);
                throw new BizException("登录失败次数过多，账号已锁定 " + LOCK_MINUTES + " 分钟");
            }
            throw new BizException("用户名或密码错误（还可尝试 " + (MAX_LOGIN_FAIL - count) + " 次）");
        }

        if (user.getStatus() != null && user.getStatus() == 0) {
            throw new BizException("账号已被禁用");
        }

        // 5. 登录成功：清除失败计数，返回令牌
        redisUtils.delete(failKey);
        log.info("User login success: {}", req.getUsername());
        return buildLoginResp(user);
    }

    @Override
    public CaptchaResp generateCaptcha() {
        LineCaptcha captcha = CaptchaUtil.createLineCaptcha(120, 40, 4, 20);
        String code = captcha.getCode();
        String captchaKey = IdUtil.fastSimpleUUID();
        redisUtils.set(RedisKey.VERIFY_CODE + captchaKey, code, CAPTCHA_EXPIRE_MINUTES, TimeUnit.MINUTES);
        return CaptchaResp.builder()
                .captchaKey(captchaKey)
                .captchaImg(captcha.getImageBase64Data())
                .build();
    }

    /**
     * 校验密码：优先 BCrypt；兼容历史 MD5 数据（登录成功后自动升级为 BCrypt）
     */
    private boolean checkPassword(String rawPassword, User user) {
        String stored = user.getPassword();
        // BCrypt 哈希以 $2a$/$2b$/$2y$ 开头
        if (StrUtil.startWith(stored, "$2")) {
            return BCrypt.checkpw(rawPassword, stored);
        }
        // 兼容历史 MD5 数据（32位hex）
        boolean ok = SecureUtil.md5(rawPassword).equalsIgnoreCase(stored);
        if (ok) {
            // 懒迁移：登录成功后自动升级为 BCrypt
            user.setPassword(BCrypt.hashpw(rawPassword, BCrypt.gensalt()));
            userMapper.updateById(user);
            log.info("Password upgraded to BCrypt for user: {}", user.getUsername());
        }
        return ok;
    }

    /**
     * 校验验证码（一次性使用：无论对错都删除，防止暴力试）
     */
    private void verifyCaptcha(LoginReq req) {
        if (StrUtil.isBlank(req.getCaptchaKey()) || StrUtil.isBlank(req.getCaptchaCode())) {
            throw new BizException("请输入验证码");
        }
        String key = RedisKey.VERIFY_CODE + req.getCaptchaKey();
        String savedCode = redisUtils.get(key, String.class);
        if (savedCode == null) {
            throw new BizException("验证码已过期，请刷新后重试");
        }
        redisUtils.delete(key);  // 一次性使用
        if (!savedCode.equalsIgnoreCase(req.getCaptchaCode().trim())) {
            throw new BizException("验证码错误");
        }
    }

    @Override
    public LoginResp refresh(String refreshToken) {
        // 严格解析 + 校验必须是 refresh 类型（纵深防御，防止 access token 被当作 refresh 使用）
        io.jsonwebtoken.Claims claims = JwtUtils.parseToken(refreshToken);
        if (claims == null || !"refresh".equals(claims.get("type", String.class))) {
            throw new BizException(CommonConstants.UNAUTHORIZED, "RefreshToken无效");
        }
        Long userId = Long.valueOf(claims.getSubject());
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
        // 宽容解析：即使 access token 已过期，也要清理 Redis 中的用户状态
        Long userId = JwtUtils.getUserIdAllowExpired(accessToken);
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
        user.setPassword(BCrypt.hashpw(req.getPassword(), BCrypt.gensalt()));
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
