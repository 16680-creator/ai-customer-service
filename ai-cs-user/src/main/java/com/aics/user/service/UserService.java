package com.aics.user.service;

import com.aics.common.result.Result;
import com.aics.user.dto.LoginRequest;
import com.aics.user.dto.LoginVO;
import com.aics.user.entity.User;

/**
 * 用户服务接口
 */
public interface UserService {

    /**
     * 用户注册
     *
     * @param user 用户信息
     * @return 注册结果
     */
    Result<Void> register(User user);

    /**
     * 用户登录
     *
     * @param request 登录请求（用户名+密码）
     * @return 登录信息（含 JWT Token）
     */
    Result<LoginVO> login(LoginRequest request);

    /**
     * 根据ID查询用户
     *
     * @param id 用户ID
     * @return 用户信息
     */
    Result<User> getUserById(Long id);

    /**
     * 更新用户信息
     *
     * @param user 用户信息
     * @return 更新结果
     */
    Result<Void> updateUser(User user);
}
