package com.aics.chat.util;

/**
 * 当前对话用户上下文（基于 {@link ThreadLocal}）。
 *
 * <p>用户身份传递链路：</p>
 * <pre>
 *   网关鉴权 → 注入 X-User-Id 请求头 → ChatController.chat() 读取请求头
 *        → {@link #setUserId(Long)} 写入 ThreadLocal
 *        → {@link com.aics.chat.service.OrderQueryService} 的 @Tool 方法通过 {@link #getUserId()} 读取
 *        → finally 块中 {@link #clear()} 清理
 * </pre>
 *
 * <p>为什么用 ThreadLocal：Spring MVC 是基于线程池的请求模型，同一请求的 Controller → Service → @Tool
 * 都在同一个 Servlet 工作线程内执行，ThreadLocal 可以零参数透传用户身份，避免方法签名污染。</p>
 *
 * <p><b>线程复用警告</b>：Tomcat 线程会被复用处理后续请求，必须在 Controller 的 finally 块中调用
 * {@link #clear()}，否则下一个请求会读到上个用户的 ID，造成越权查询。</p>
 *
 * <p><b>异步失效警告</b>：ThreadLocal 不能跨线程传递，若 @Tool 方法内启用了新线程或异步执行，
 * 子线程无法读到当前用户 ID。本项目的 Tool Calling 走 Spring AI 同步回调，无此问题。</p>
 */
public final class ChatUserContext {

    /** 当前线程持有的用户 ID，null 表示未登录/匿名 */
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