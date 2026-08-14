package com.aics.user.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

import java.io.Serializable;

/**
 * 登录请求
 */
@Data
@Schema(description = "账号密码登录请求")
public class LoginRequest implements Serializable {

    private static final long serialVersionUID = 1L;

    /** 用户名 */
    @Schema(description = "用户名", example = "admin")
    @NotBlank(message = "用户名不能为空")
    private String username;

    /** 密码 */
    @Schema(description = "密码", example = "admin123")
    @NotBlank(message = "密码不能为空")
    private String password;
}
