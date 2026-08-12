package com.aics.user.service.impl;

import cn.hutool.crypto.digest.BCrypt;
import com.aics.common.exception.BusinessException;
import com.aics.common.result.Result;
import com.aics.common.result.ResultCode;
import com.aics.common.util.JwtUtil;
import com.aics.user.dto.LoginRequest;
import com.aics.user.dto.LoginVO;
import com.aics.user.entity.User;
import com.aics.user.mapper.UserMapper;
import com.aics.user.service.UserService;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
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

    /** JWT 签名密钥（来自 Nacos 配置中心 aics-shared.yml） */
    @Value("${aics.jwt.secret:aics-platform-jwt-secret-key-must-be-at-least-256-bits-long-for-hs256}")
    private String jwtSecret;

    /** Token 有效期（小时） */
    @Value("${aics.jwt.expire-hours:24}")
    private long jwtExpireHours;

    @Override
    /**
     * 用户注册。
     *
     * <p><b>学习要点</b>：密码绝不存明文——用 BCrypt 哈希后存储；
     * 注册成功后即可用账号密码登录换取 JWT。</p>
     */
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
    /**
     * 用户登录。
     *
     * <p><b>学习要点（技术：JWT 登录流程）</b>：校验账号密码（比对 BCrypt 哈希）→
     * 用 JwtUtil 签发 Token（载荷含 userId、过期时间）→ 返回给前端；
     * 前端后续请求携带 Token，由网关校验并透传 X-User-Id（见 ai-cs-gateway AuthFilter）。</p>
     */
    public Result<LoginVO> login(LoginRequest request) {
        String username = request.getUsername();
        log.info("用户登录: username={}", username);

        // 查询用户
        User user = userMapper.selectOne(
                new LambdaQueryWrapper<User>().eq(User::getUsername, username));
        if (user == null) {
            throw new BusinessException(ResultCode.USER_NOT_FOUND);
        }

        // 校验密码
        if (!BCrypt.checkpw(request.getPassword(), user.getPassword())) {
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
        String token = JwtUtil.generateToken(String.valueOf(user.getId()), claims,
                jwtSecret, jwtExpireHours * 60 * 60 * 1000L);

        // 组装登录返回信息
        LoginVO vo = new LoginVO();
        vo.setToken(token);
        vo.setUserId(user.getId());
        vo.setUsername(user.getUsername());
        vo.setNickname(user.getNickname() != null ? user.getNickname() : user.getUsername());
        vo.setRole(user.getRole());

        log.info("用户登录成功: username={}", username);
        return Result.success(vo);
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
