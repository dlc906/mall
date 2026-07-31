package com.mall.user.controller;

import com.mall.common.entity.Result;
import com.mall.user.model.req.UserRegisterReq;
import com.mall.user.model.resp.UserInfoResp;
import com.mall.user.entity.User;
import com.mall.user.service.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;
import javax.validation.Valid;

@Tag(name = "用户管理", description = "用户注册、信息查询、修改")
@RestController
@RequestMapping("/api/user")
public class UserController {

    @Resource
    private UserService userService;

    @Operation(summary = "用户注册")
    @PostMapping("/register")
    public Result<UserInfoResp> register(@Valid @RequestBody UserRegisterReq req) {
        return Result.success(userService.register(req));
    }

    @Operation(summary = "获取用户信息")
    @GetMapping("/info")
    public Result<UserInfoResp> getUserInfo(@RequestHeader("X-User-Id") Long userId) {
        return Result.success(userService.getUserInfo(userId));
    }

    @Operation(summary = "修改用户信息")
    @PutMapping("/info")
    public Result<UserInfoResp> updateUserInfo(@RequestHeader("X-User-Id") Long userId,
                                                @RequestBody User user) {
        return Result.success(userService.updateUserInfo(userId, user));
    }

    @Operation(summary = "根据用户名查询用户(Feign调用)")
    @GetMapping("/by-username/{username}")
    public Result<UserInfoResp> getUserByUsername(@PathVariable String username) {
        return Result.success(userService.getUserByUsername(username));
    }

    @Operation(summary = "健康检查")
    @GetMapping("/health")
    public Result<String> health() {
        return Result.success("OK");
    }
}
