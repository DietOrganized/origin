package com.example.calorie.controller;

import com.example.calorie.common.Result;
import com.example.calorie.dto.LoginRequest;
import com.example.calorie.dto.RegisterRequest;
import com.example.calorie.service.AuthService;
import com.example.calorie.vo.LoginVO;
import com.example.calorie.vo.UserVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "01. 认证")
@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;

    @Operation(summary = "注册")
    @PostMapping("/register")
    public Result<UserVO> register(@Valid @RequestBody RegisterRequest request) {
        return Result.ok(authService.register(request));
    }

    @Operation(summary = "登录（返回 30 天有效 token）")
    @PostMapping("/login")
    public Result<LoginVO> login(@Valid @RequestBody LoginRequest request) {
        return Result.ok(authService.login(request));
    }

    @Operation(summary = "退出登录（token 立即失效）")
    @PostMapping("/logout")
    public Result<Void> logout() {
        authService.logout();
        return Result.ok();
    }

    @Operation(summary = "当前登录用户")
    @GetMapping("/me")
    public Result<UserVO> me() {
        return Result.ok(authService.currentUser());
    }
}
