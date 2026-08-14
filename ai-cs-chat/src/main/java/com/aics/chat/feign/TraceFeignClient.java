package com.aics.chat.feign;

import com.aics.chat.dto.LlmTraceDTO;
import com.aics.chat.dto.LlmTraceVO;
import com.aics.common.result.Result;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

/**
 * 消息服务 LLM 调用链追踪 Feign 客户端（调用 ai-cs-message 持久化/查询 llm_trace）
 *
 * <h3>【AI 技术详解】Feign 的 name 与 contextId 分别是什么？</h3>
 * <ul>
 *   <li><b>{@code name}（服务名）</b>：注册中心里的服务标识，决定请求路由到哪个实例
 *       （ai-cs-message）；</li>
 *   <li><b>{@code contextId}（Bean 名）</b>：同一个服务名下可以有多个 FeignClient 接口，
 *       每个接口需要一个<b>唯一</b>的 contextId 作为 Spring Bean 名。若缺省，Feign 会
 *       用 name 作为 Bean 名，同服务多个接口直接冲突；显式指定 {@code llmTrace} 与
 *       {@code AgentTraceFeignClient} 的 {@code agentTrace} 等区分，互不覆盖。</li>
 * </ul>
 *
 * <p><b>为什么返回 {@code Result<T>} 而不是裸类型</b>：跨服务统一响应协议，code 非
 * SUCCESS 时由公共异常处理转为业务异常，调用方无需逐字段判空；Feign 解码器负责
 * 把 JSON 反序列化为 Result，错误码在客户端同样可见。</p>
 */
@FeignClient(name = "ai-cs-message", contextId = "llmTrace")
public interface TraceFeignClient {

    /**
     * 上报一次请求的完整调用链（幂等：requestId 已存在返回首次创建的 requestId）
     *
     * <p><b>【技术详解】为什么要幂等</b>：TraceRecorder 异步上报失败后可能重试，
     * HTTP 重试没有天然去重；以 requestId 为幂等键，message 侧对已存在的键直接
     * 返回首次结果，保证调用链只落库一次。</p>
     */
    @PostMapping("/api/observability/traces")
    Result<String> createTrace(@RequestBody LlmTraceDTO dto);

    /**
     * 按 requestId 查询调用链详情（不存在返回 null）
     */
    // @PathVariable 显式指定路径变量名：不依赖编译期 -parameters 参数名保留，
    // 方法签名重命名时路径模板不受影响（接口方法名与 URL 契约解耦）
    @GetMapping("/api/observability/traces/{requestId}")
    Result<LlmTraceVO> getTrace(@PathVariable("requestId") String requestId);
}
