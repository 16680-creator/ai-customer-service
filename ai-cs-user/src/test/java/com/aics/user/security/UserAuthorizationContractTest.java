package com.aics.user.security;

import com.aics.user.controller.UserController;
import com.aics.user.entity.User;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.security.access.prepost.PreAuthorize;

import java.lang.reflect.Method;

import static org.junit.jupiter.api.Assertions.*;

/**
 * 用户接口方法级授权契约：本人可访问，ADMIN 可越权管理，其他用户 403。
 */
class UserAuthorizationContractTest {

    @Test
    @DisplayName("查询用户 - 必须声明本人或 ADMIN")
    void getUserMustBeSelfOrAdmin() throws Exception {
        Method method = UserController.class.getMethod("getUserById", Long.class);
        PreAuthorize annotation = method.getAnnotation(PreAuthorize.class);
        assertNotNull(annotation);
        assertTrue(annotation.value().contains("authentication.name == #p0.toString()"));
        assertTrue(annotation.value().contains("hasRole('ADMIN')"));
    }

    @Test
    @DisplayName("更新用户 - 必须声明本人或 ADMIN")
    void updateUserMustBeSelfOrAdmin() throws Exception {
        Method method = UserController.class.getMethod("updateUser", User.class);
        PreAuthorize annotation = method.getAnnotation(PreAuthorize.class);
        assertNotNull(annotation);
        assertTrue(annotation.value().contains("authentication.name == #p0.id.toString()"));
        assertTrue(annotation.value().contains("hasRole('ADMIN')"));
    }
}
