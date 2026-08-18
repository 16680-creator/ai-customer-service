package com.aics.chat.prompt;

/**
 * Prompt 渲染/加载异常。
 *
 * <p>触发场景：
 * <ul>
 *   <li>请求了未配置的 scenario；</li>
 *   <li>渲染时模板引用了未提供的 {@code {{var}}} 占位符；</li>
 *   <li>启动校验失败（activeVersion 不存在、灰度权重和不为 1）。</li>
 * </ul>
 * 该异常为 {@link RuntimeException}，由调用方决定降级还是抛出。
 */
public class PromptRenderException extends RuntimeException {

    public PromptRenderException(String message) {
        super(message);
    }

    public PromptRenderException(String message, Throwable cause) {
        super(message, cause);
    }
}
