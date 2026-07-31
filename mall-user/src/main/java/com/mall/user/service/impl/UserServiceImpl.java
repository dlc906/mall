package com.mall.user.service.impl;

import cn.hutool.core.util.IdUtil;
import cn.hutool.crypto.SecureUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.mall.common.exception.BizException;
import com.mall.user.entity.User;
import com.mall.user.mapper.UserMapper;
import com.mall.user.model.req.UserRegisterReq;
import com.mall.user.model.resp.UserInfoResp;
import com.mall.user.service.UserService;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.Resource;

@Service
public class UserServiceImpl implements UserService {

    @Resource
    private UserMapper userMapper;

    @Override
    @Transactional
    public UserInfoResp register(UserRegisterReq req) {
        Long count = userMapper.selectCount(new LambdaQueryWrapper<User>()
                .eq(User::getUsername, req.getUsername()));
        if (count > 0) {
            throw new BizException("用户名已存在");
        }

        User user = new User();
        user.setUsername(req.getUsername());
        user.setPassword(SecureUtil.md5(req.getPassword()));
        user.setPhone(req.getPhone());
        user.setEmail(req.getEmail());
        user.setNickname(req.getUsername());
        user.setStatus(1);
        user.setPoints(0);
        user.setInviteCode(IdUtil.fastSimpleUUID().substring(0, 8));
        user.setAvatar("https://api.dicebear.com/7.x/initials/svg?seed=" + req.getUsername());

        // Bind inviter if invite code provided
        if (req.getInviteCode() != null && !req.getInviteCode().isEmpty()) {
            User inviter = userMapper.selectOne(new LambdaQueryWrapper<User>()
                    .eq(User::getInviteCode, req.getInviteCode()));
            if (inviter != null && !inviter.getId().equals(user.getId())) {
                user.setParentInviterId(inviter.getId());
            }
        }

        userMapper.insert(user);

        UserInfoResp resp = UserInfoResp.builder().build();
        BeanUtils.copyProperties(user, resp);
        return resp;
    }

    @Override
    public UserInfoResp getUserInfo(Long userId) {
        User user = userMapper.selectById(userId);
        if (user == null) {
            throw new BizException("用户不存在");
        }
        UserInfoResp resp = UserInfoResp.builder().build();
        BeanUtils.copyProperties(user, resp);
        return resp;
    }

    @Override
    public UserInfoResp updateUserInfo(Long userId, User updateUser) {
        User user = userMapper.selectById(userId);
        if (user == null) {
            throw new BizException("用户不存在");
        }
        if (updateUser.getNickname() != null) user.setNickname(updateUser.getNickname());
        if (updateUser.getPhone() != null) user.setPhone(updateUser.getPhone());
        if (updateUser.getEmail() != null) user.setEmail(updateUser.getEmail());
        if (updateUser.getAvatar() != null) user.setAvatar(updateUser.getAvatar());
        userMapper.updateById(user);

        UserInfoResp resp = UserInfoResp.builder().build();
        BeanUtils.copyProperties(user, resp);
        return resp;
    }

    @Override
    public UserInfoResp getUserByUsername(String username) {
        User user = userMapper.selectOne(new LambdaQueryWrapper<User>()
                .eq(User::getUsername, username));
        if (user == null) return null;
        UserInfoResp resp = UserInfoResp.builder().build();
        BeanUtils.copyProperties(user, resp);
        return resp;
    }
}
