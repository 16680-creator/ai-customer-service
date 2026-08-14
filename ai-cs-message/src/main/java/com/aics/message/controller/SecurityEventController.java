package com.aics.message.controller;

import com.aics.common.result.Result;
import com.aics.message.dto.SecurityEventDTO;
import com.aics.message.service.SecurityEventService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 安全事件控制器（3.2 F7 审计留痕）。
 * <p>
 * 所属模块：ai-cs-message。
 * 职责：对外暴露安全事件上报 REST 接口（POST /api/security/events），供 chat 模块
 * Guardrail 链路（经 OpenFeign）持久化注入拦截、内容审核、工具越权、RAG ACL 过滤、
 * SQL 拦截等安全事件，构成可追溯的安全审计底座。
 * </p>
 */
@Tag(name = "安全事件审计")
@RestController
@RequestMapping("/api/security")
@RequiredArgsConstructor
@Validated
public class SecurityEventController {

    /** 安全事件服务 */
    private final SecurityEventService securityEventService;

    /**
     * 记录安全事件（按 eventId 幂等，重复上报跳过）
     *
     * @param dto 安全事件信息
     * @return 空结果包装
     */
    @Operation(summary = "记录安全事件")
    @PostMapping("/events")
    public Result<Void> record(@Valid @RequestBody SecurityEventDTO dto) {
        // 委托服务层按 eventId 幂等落库
        securityEventService.record(dto);
        return Result.success();
    }
}
