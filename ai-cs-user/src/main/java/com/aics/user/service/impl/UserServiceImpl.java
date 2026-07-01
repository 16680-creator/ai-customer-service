package com.aics.user.service.impl;

import cn.hutool.crypto.digest.BCrypt;
import com.aics.common.exception.BusinessException;
import com.aics.common.result.Result;
import com.aics.common.result.ResultCode;
import com.aics.common.util.JwtUtil;
import com.aics.user.entity.User;
import com.aics.user.mapper.UserMapper;
import com.aics.user.service.UserService;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Map;

/**
 * 用户服务实现
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class UserServiceImpl implements UserService {

    private final UserMapper userMapper;

    @Override
    public Result<Void> register(User user) {
        log.info("用户注册: username={}", user.getUsername());

        // 检查用户名是否已存在
        Long count = userMapper.selectCount(
                new LambdaQueryWrapper<User>().eq(User::getUsername, user.getUsername()));
        if (count > 0) {
            throw new BusinessException(ResultCode.USER_ALREADY_EXISTS);
        }

        // 密码加密
        user.setPassword(BCrypt.hashpw(user.getPassword()));
        user.setStatus(1);
        user.setRole("user");

        userMapper.insert(user);
        log.info("用户注册成功: username={}, id={}", user.getUsername(), user.getId());
        return Result.success();
    }

    @Override
    public Result<String> login(String username, String password) {
        log.info("用户登录: username={}", username);

        // 查询用户
        User user = userMapper.selectOne(
                new LambdaQueryWrapper<User>().eq(User::getUsername, username));
        if (user == null) {
            throw new BusinessException(ResultCode.USER_NOT_FOUND);
        }

        // 校验密码
        if (!BCrypt.checkpw(password, user.getPassword())) {
            throw new BusinessException(ResultCode.USER_PASSWORD_ERROR);
        }

        // 校验状态
        if (user.getStatus() != 1) {
            throw new BusinessException(ResultCode.USER_ACCOUNT_DISABLED);
        }

        // 生成 Token
        Map<String, Object> claims = new HashMap<>(2);
        claims.put("username", user.getUsername());
        claims.put("role", user.getRole());
        String token = JwtUtil.generateToken(String.valueOf(user.getId()), claims);

        log.info("用户登录成功: username={}", username);
        return Result.success(token);
    }

    @Override
    public Result<User> getUserById(Long id) {
        log.info("查询用户: id={}", id);
        User user = userMapper.selectById(id);
        if (user == null) {
            throw new BusinessException(ResultCode.USER_NOT_FOUND);
        }
        // 脱敏：清除密码
        user.setPassword(null);
        return Result.success(user);
    }

    @Override
    public Result<Void> updateUser(User user) {
        log.info("更新用户: id={}", user.getId());
        // 不允许通过此接口修改密码
        user.setPassword(null);
        userMapper.updateById(user);
        log.info("用户更新成功: id={}", user.getId());
        return Result.success();
    }
}
