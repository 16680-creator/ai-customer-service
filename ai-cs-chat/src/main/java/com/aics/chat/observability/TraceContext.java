package com.aics.chat.observability;

import lombok.Getter;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * 一次请求的调用链上下文：requestId 贯穿的 span 集合。
 *
 * <p>对应 docs/15 第 3.3 节：<pre>
 * requestId / userId / sessionId
 *   ├─ intent: 类型、置信度、路由结果
 *   ├─ retrieval: query、召回数、文档 ID、分数、耗时
 *   ├─ rerank: 模型、前后排序、耗时
 *   ├─ llm: provider、model、tokens、首 Token、总耗时、重试
 *   ├─ tools: 工具名、参数摘要、结果状态、耗时
 *   └─ answer: 引用数、安全结果、用户反馈
 * </pre>
 * 线程安全：span 列表使用 {@link CopyOnWriteArrayList}，允许异步线程（LLM 调用、
 * SSE 订阅回调）并发追加。</p>
 */
@Getter
public class TraceContext {

    /** 全局唯一请求 ID（UUID），贯穿一次请求全链路 */
    private final String requestId;

    /** 用户 ID（可空：未登录请求） */
    private final Long userId;

    /** 会话 ID（字符串 sessionKey，与 ai-cs-chat 会话体系对齐） */
    private final String sessionId;

    /** 场景：chat / rag / sse / agent / vision / eval 等 */
    private final String scenario;

    /** 请求开始时间（毫秒时间戳） */
    private final long startTimeMs;

    /** 环节 span 列表（线程安全） */
    // 学习点：CopyOnWriteArrayList —— 写时复制，读操作无锁。
    // LLM 调用/SSE 回调在异步线程并发追加 span，而请求主线程在结束时读取，
    // 读多写少的场景下 CopyOnWriteArrayList 比 synchronizedList 并发度更高。
    private final List<TraceSpan> spans = new CopyOnWriteArrayList<>();

    /** 请求整体状态：SUCCESS / FAILED */
    private volatile String status = "SUCCESS";

    /** 请求整体失败摘要 */
    // volatile 保证跨线程可见性：异步线程 markFailed 后，主线程 afterCompletion 能立即读到
    private volatile String errorSummary;

    // ===== Prompt 效果关联（OpenSpec change 2026-08-18-prompt-config）=====
    /** 本次请求当前生效的 Prompt 场景（由调用点在 render 后写入，供 LLM span 关联） */
    private volatile String promptScenario;

    /** 本次请求当前生效的 Prompt 版本 */
    private volatile String promptVersion;

    /** 写入本次 LLM 调用使用的 Prompt 场景与版本（效果关联：promptVersion → 评估质量分） */
    public void setPrompt(String scenario, String version) {
        this.promptScenario = scenario;
        this.promptVersion = version;
    }

    public String getPromptScenario() {
        return promptScenario;
    }

    public String getPromptVersion() {
        return promptVersion;
    }

    public TraceContext(String requestId, Long userId, String sessionId, String scenario) {
        this.requestId = requestId;
        this.userId = userId;
        this.sessionId = sessionId;
        this.scenario = scenario;
        this.startTimeMs = System.currentTimeMillis();
    }

    /** 追加一个环节 span */
    public void addSpan(TraceSpan span) {
        spans.add(span);
    }

    /** 请求总耗时（毫秒） */
    public long totalDurationMs() {
        return System.currentTimeMillis() - startTimeMs;
    }

    /** 标记整体失败 */
    public void markFailed(String errorSummary) {
        this.status = "FAILED";
        this.errorSummary = errorSummary;
    }
}
