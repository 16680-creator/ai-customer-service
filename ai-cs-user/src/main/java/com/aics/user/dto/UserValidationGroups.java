package com.aics.user.dto;

/** 用户请求的分组校验：创建与更新复用既有 User 请求结构。 */
public final class UserValidationGroups {

    private UserValidationGroups() {
    }

    /** 注册：必须提供用户名和密码。 */
    public interface Create {
    }

    /** 更新：必须提供主键，其他字段保持可选。 */
    public interface Update {
    }
}
