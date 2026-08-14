package com.aics.chat.controller;

import com.aics.chat.dto.LlmTraceVO;
import com.aics.chat.feign.TraceFeignClient;
import com.aics.common.result.Result;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * LLM 可观测性查询控制器（chat 侧）
 *
 * <p>对外暴露 trace 查询接口，经 {@link TraceFeignClient} 回读 ai-cs-message 的
 * {@code llm_trace} 数据，用于还原一次请求的完整调用链（模型、工具、文档、费用）。</p>
 *
 * <h3>【AI 技术详解】为什么控制器如此"薄"？</h3>
 * <p>可观测性数据的<b>产生</b>在 chat 侧（TraceRecorder 组装、ModelUsageRecorder 计量），
 * 但<b>存储与查询</b>在 message 侧。控制器只做两件事：接收 HTTP 请求、把参数转交
 * Feign 客户端。业务逻辑全部下沉到数据归属方，避免"查询逻辑跨服务复制"——
 * 若 chat 侧自己做统计，message 侧表结构一变，两处都得改。</p>
 */
@Tag(name = "LLM 可观测性")
@RestController
@RequestMapping("/api/observability")
@RequiredArgsConstructor
public class ObservabilityController {

    private final TraceFeignClient traceFeignClient;

    /**
     * 按 requestId 查询一次请求的完整调用链。
     *
     * @param requestId 请求 ID
     * @return 调用链详情（不存在返回 null 数据）
     */
    @Operation(summary = "查询 LLM 调用链详情")
    // 路径参数（GET 语义）：requestId 是幂等键/UUID，短小安全，适合放路径而非查询串
    @GetMapping("/traces/{requestId}")
    public Result<LlmTraceVO> getTrace(@PathVariable("requestId") String requestId) {
        // 薄委托：Feign 返回的 Result 原样透传（含 code/message），
        // 不存在时 message 侧返回 success(null)，前端据此展示"未找到"而非报错
        return traceFeignClient.getTrace(requestId);
    }
}
