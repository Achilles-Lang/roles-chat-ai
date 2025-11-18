package com.blueberry.quiz_user.controller;

import com.blueberry.model.common.Result;
import com.blueberry.model.user.UserDTO;
import com.blueberry.model.user.UserVO;
import com.blueberry.quiz_user.auth.TokenManager;
import com.blueberry.quiz_user.service.impl.UserServiceImpl;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("user")
public class UserController {
    @Autowired
    private UserServiceImpl userService;
    @Autowired
    private TokenManager tokenManager;

    @PostMapping("register")
    public Result<String> registerUser(@RequestBody UserDTO user) {
        var result = userService.addUser(user);
        return result ? login(user) : Result.fail("注册失败");
    }

    @PostMapping("login")
    public Result<String> login(@RequestBody UserDTO user) {
        var userId = userService.login(user);
        var token = tokenManager.generateToken(userId);
        return Result.success(token);
    }

    @PostMapping("logout")
    public Result<String> logout(Long userId) {
        tokenManager.clearToken(userId);
        return Result.success();
    }

    /**
     * 用于已登录过的用户的鉴权
     * 由于网关是统一鉴权的，所以能访问到该接口说明权限正常
     */
    @PostMapping("verify")
    public Result<String> verify(HttpServletRequest request) {
        var userId = tokenManager.verifyToken(request);
        if (userId == null) {
            return Result.fail("用户信息不存在"); //不太可能走到
        }
        return Result.success(userId);
    }

    @GetMapping("get")
    public Result<UserVO> getUser(@RequestParam("userId") Long userId) {
        return Result.success(userService.getUser(userId));
    }
}
