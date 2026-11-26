package com.aics.user.entity;

import com.aics.user.dto.UserValidationGroups;
import com.baomidou.mybatisplus.annotation.*;
import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.io.Serializable;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;

/**
 * 用户实体
 */
@Data
@TableName("sys_user")
public class User implements Serializable {

    private static final long serialVersionUID = 1L;

    /** 主键ID */
    @NotNull(message = "用户ID不能为空", groups = UserValidationGroups.Update.class)
    @TableId(type = IdType.ASSIGN_ID)
    private Long id;

    /** 用户名 */
    @NotBlank(message = "用户名不能为空", groups = UserValidationGroups.Create.class)
    @Size(max = 64, message = "用户名不能超过64个字符", groups = {
            UserValidationGroups.Create.class, UserValidationGroups.Update.class})
    private String username;

    /** 密码（加密存储） */
    @NotBlank(message = "密码不能为空", groups = UserValidationGroups.Create.class)
    @Size(min = 6, max = 72, message = "密码长度必须为6到72个字符",
            groups = UserValidationGroups.Create.class)
    private String password;

    /** BCrypt 的输入上限为 72 个 UTF-8 字节，不能简单等同于 72 个字符。 */
    @JsonIgnore
    @AssertTrue(message = "密码UTF-8编码不能超过72字节", groups = UserValidationGroups.Create.class)
    public boolean isPasswordWithinBcryptLimit() {
        return password == null || password.getBytes(StandardCharsets.UTF_8).length <= 72;
    }

    /** 昵称 */
    private String nickname;

    /** 手机号 */
    private String phone;

    /** 邮箱 */
    private String email;

    /** 头像URL */
    private String avatar;

    /** 状态：0-禁用 1-启用 */
    private Integer status;

    /** 角色标识 */
    private String role;

    /** 创建时间 */
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    /** 更新时间 */
    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;

    /** 逻辑删除：0-未删除 1-已删除 */
    @TableLogic
    private Integer deleted;
}
