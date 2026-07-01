package com.aics.user.controller;

import com.aics.common.result.Result;
import com.aics.user.entity.User;
import com.aics.user.service.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.constraints.NotBlank;
import lombok.RequiredArgsConstructor;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

/**
 * 用户控制器
 */
@Tag(name = "用户管理")
@RestController
@RequestMapping("/user")
@RequiredArgsConstructor
@Validated
public class UserController {

    private final UserService userService;

    @Operation(summary = "用户注册")
    @PostMapping("/register")
    public Result<Void> register(@RequestBody User user) {
        return userService.register(user);
    }

    @Operation(summary = "用户登录")
    @PostMapping("/login")
    public Result<String> login(@RequestParam @NotBlank(message = "用户名不能为空") String username,
                                @RequestParam @NotBlank(message = "密码不能为空") String password) {
        return userService.login(username, password);
    }

    @Operation(summary = "查询用户信息")
    @GetMapping("/{id}")
    public Result<User> getUserById(@PathVariable Long id) {
        return userService.getUserById(id);
    }

    @Operation(summary = "更新用户信息")
    @PutMapping
    public Result<Void> updateUser(@RequestBody User user) {
        return userService.updateUser(user);
    }
}
