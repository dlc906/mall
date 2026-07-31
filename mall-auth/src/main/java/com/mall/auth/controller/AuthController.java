package com.mall.auth.controller;

import com.mall.auth.model.req.LoginReq;
import com.mall.auth.model.req.RegisterReq;
import com.mall.auth.model.resp.LoginResp;
import com.mall.auth.service.AuthService;
import com.mall.common.entity.Result;
import com.mall.common.utils.JwtUtils;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;
import javax.validation.Valid;

@Tag(name = "认证中心", description = "登录、注册、Token刷新")
@RestController
@RequestMapping("/api/auth")
public class AuthController {

    @Resource
    private AuthService authService;

    @Operation(summary = "用户登录")
    @PostMapping("/login")
    public Result<LoginResp> login(@Valid @RequestBody LoginReq req) {
        return Result.success(authService.login(req));
    }

    @Operation(summary = "用户注册")
    @PostMapping("/register")
    public Result<LoginResp> register(@Valid @RequestBody RegisterReq req) {
        return Result.success(authService.register(req));
    }

    @Operation(summary = "刷新Token")
    @PostMapping("/refresh")
    public Result<LoginResp> refresh(@RequestHeader("X-Refresh-Token") String refreshToken) {
        return Result.success(authService.refresh(refreshToken));
    }

    @Operation(summary = "登出")
    @PostMapping("/logout")
    public Result<Void> logout(@RequestHeader("Authorization") String authHeader) {
        String token = authHeader.replace("Bearer ", "");
        authService.logout(token);
        return Result.success();
    }

    @Operation(summary = "获取当前用户信息")
    @GetMapping("/current-user")
    public Result<?> currentUser(@RequestHeader("X-User-Id") Long userId) {
        // User info is already passed through gateway
        return Result.success(userId);
    }
}
