package com.aics.chat.util;

/**
 * 当前对话用户上下文（ThreadLocal）
 * 由 ChatController 从网关透传的 X-User-Id 设置，供工具调用（@Tool）读取当前用户
 */
public final class ChatUserContext {

    private static final ThreadLocal<Long> USER_ID = new ThreadLocal<>();

    private ChatUserContext() {
    }

    public static void setUserId(Long userId) {
        USER_ID.set(userId);
    }

    public static Long getUserId() {
        return USER_ID.get();
    }

    public static void clear() {
        USER_ID.remove();
    }
}