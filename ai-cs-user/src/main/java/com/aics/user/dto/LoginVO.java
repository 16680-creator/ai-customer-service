package com.aics.user.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serializable;

/**
 * 登录成功返回信息
 */
@Data
@Schema(description = "登录成功返回信息")
public class LoginVO implements Serializable {

    private static final long serialVersionUID = 1L;

    /** JWT Token */
    @Schema(description = "JWT Token")
    private String token;

    /** 用户ID */
    @Schema(description = "用户ID")
    private Long userId;

    /** 用户名 */
    @Schema(description = "用户名")
    private String username;

    /** 昵称 */
    @Schema(description = "昵称")
    private String nickname;

    /** 角色标识 */
    @Schema(description = "角色标识")
    private String role;
}
