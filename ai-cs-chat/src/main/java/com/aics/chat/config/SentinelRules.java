package com.aics.chat.config;

/**
 * Sentinel 资源名与规则参数常量。
 *
 * <p>资源名与 {@code @SentinelResource} 注解处的值保持单一来源；
 * 阈值集中在此，避免规则散落在注解与代码里互相打架。</p>
 */
public final class SentinelRules {

    private SentinelRules() {
    }

    /** 同步对话接口资源（/chat/send） */
    public static final String RESOURCE_CHAT_SEND = "chat-send";

    /** RAG 对话接口资源（/chat/rag） */
    public static final String RESOURCE_CHAT_RAG = "chat-rag";

    /** 同步对话 QPS 阈值：LLM 供应商并发有限，超出直接走限流降级 */
    public static final int CHAT_SEND_QPS_THRESHOLD = 10;

    /** RAG 对话 QPS 阈值：检索 + 生成链路更重，阈值更低 */
    public static final int CHAT_RAG_QPS_THRESHOLD = 5;

    /** 流控效果：0-直接拒绝，1-预热(WarmUp)，2-匀速排队 */
    public static final int CONTROL_BEHAVIOR_WARM_UP = 1;

    /** 预热时长（秒）：冷启动 slowly lift QPS，保护刚启动的 LLM 连接池 */
    public static final int WARM_UP_PERIOD_SEC = 10;
}
