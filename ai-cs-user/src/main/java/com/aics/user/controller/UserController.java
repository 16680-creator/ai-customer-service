package com.aics.user.controller;

import com.aics.common.result.Result;
import com.aics.user.dto.LoginRequest;
import com.aics.user.dto.LoginVO;
import com.aics.user.entity.User;
import com.aics.user.service.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
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
    public Result<LoginVO> login(@Valid @RequestBody LoginRequest request) {
        return userService.login(request);
    }

    @Operation(summary = "查询用户信息")
    @GetMapping("/{id}")
    @PreAuthorize("authentication.name == #p0.toString() or hasRole('ADMIN')")
    public Result<User> getUserById(@PathVariable("id") Long id) {
        return userService.getUserById(id);
    }

    @Operation(summary = "更新用户信息")
    @PutMapping
    @PreAuthorize("authentication.name == #p0.id.toString() or hasRole('ADMIN')")
    public Result<Void> updateUser(@RequestBody User user) {
        return userService.updateUser(user);
    }
}
