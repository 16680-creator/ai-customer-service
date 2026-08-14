package com.aics.message.controller;

import com.aics.common.result.Result;
import com.aics.message.dto.LlmTraceDTO;
import com.aics.message.dto.PageResult;
import com.aics.message.service.LlmTraceService;
import com.aics.message.vo.LlmTraceVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

/**
 * LLM 调用链追踪控制器
 * <p>
 * 所属模块：ai-cs-message。
 * 职责：对外暴露 LLM 调用链追踪的 REST 接口（/api/observability/traces*），
 * 供 chat 模块 LLM 编排链路（经 OpenFeign）上报调用链元数据、查询单条追踪、
 * 按用户/场景分页检索，包括：
 * <ul>
 *     <li>创建调用链追踪（幂等，返回 requestId）；</li>
 *     <li>按 requestId 查询单条追踪（不存在返回 success(null)）；</li>
 *     <li>按 userId/scenario 分页查询（create_time 倒序）。</li>
 * </ul>
 * 统一返回 {@link Result} 包装结构。
 * </p>
 */
@Tag(name = "LLM 调用链追踪")
@RestController
@RequestMapping("/api/observability")
@RequiredArgsConstructor
@Validated
public class LlmTraceController {

    /** LLM 调用链追踪服务 */
    private final LlmTraceService llmTraceService;

    /**
     * 创建 LLM 调用链追踪（幂等：requestId 已存在时返回已有 requestId）
     *
     * @param dto 调用链追踪信息
     * @return 请求ID（requestId）
     */
    @Operation(summary = "创建 LLM 调用链追踪")
    @PostMapping("/traces")
    public Result<String> createTrace(@Valid @RequestBody LlmTraceDTO dto) {
        // @Valid 触发 DTO 的 jakarta 校验（requestId/scenario @NotBlank）：校验失败由全局异常处理器
        // 统一转成 Result(400)，Controller 自身保持零校验代码
        // 幂等创建：requestId 已存在时服务层直接返回首次创建的 requestId
        return Result.success(llmTraceService.createTrace(dto));
    }

    /**
     * 按 requestId 查询 LLM 调用链追踪（不存在返回 success(null)）
     *
     * @param requestId 请求ID
     * @return 调用链追踪信息，不存在时为 null
     */
    @Operation(summary = "查询 LLM 调用链追踪详情")
    @GetMapping("/traces/{requestId}")
    public Result<LlmTraceVO> getTrace(@PathVariable("requestId") String requestId) {
        // 不存在时服务层返回 null，这里包装为 success(null)，不视为错误。
        // 为什么用 success(null) 而非 fail：HTTP 语义上"查询成功但无数据"（200 + null）与
        // "查询失败"（4xx/5xx）是两回事，调用方（chat 模块）据此决定展示占位还是告警
        return Result.success(llmTraceService.getTrace(requestId));
    }

    /**
     * 分页查询 LLM 调用链追踪（userId/scenario 可空过滤，create_time 倒序）
     *
     * @param userId   用户ID（可空）
     * @param scenario 场景（可空）
     * @param page     页码（默认 1）
     * @param size     每页大小（默认 20）
     * @return 分页查询结果
     */
    @Operation(summary = "分页查询 LLM 调用链追踪")
    @GetMapping("/traces")
    public Result<PageResult<LlmTraceVO>> pageTraces(
            @RequestParam(required = false) Long userId,
            @RequestParam(required = false) String scenario,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int size) {
        // required=false：查询参数可空，为 null 时服务层跳过对应过滤；
        // defaultValue 保证 page/size 永远有合法值，避免前端漏传导致分页异常
        // 分页查询：userId/scenario 为空时不参与过滤
        return Result.success(llmTraceService.pageTraces(userId, scenario, page, size));
    }
}
