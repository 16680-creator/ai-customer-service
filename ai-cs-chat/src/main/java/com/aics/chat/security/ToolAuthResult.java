package com.aics.chat.security;

/**
 * 工具授权结果（3.2 F2）。
 *
 * @param allowed 是否允许调用
 * @param role    当前用户角色
 * @param reason  拒绝原因（允许时为 null）
 */
public record ToolAuthResult(boolean allowed, String role, String reason) {

    public static ToolAuthResult allowed(String role) {
        return new ToolAuthResult(true, role, null);
    }

    public static ToolAuthResult denied(String role, String reason) {
        return new ToolAuthResult(false, role, reason);
    }
}
