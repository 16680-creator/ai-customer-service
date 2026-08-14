package com.aics.message.controller;

import com.aics.common.result.Result;
import com.aics.message.dto.ModelUsageDTO;
import com.aics.message.service.ModelUsageService;
import com.aics.message.vo.ModelUsageStatsVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;

/**
 * 模型用量计量控制器
 * <p>
 * 所属模块：ai-cs-message。
 * 职责：对外暴露模型用量计量的 REST 接口（/api/model-usage/*），供 chat 模块
 * LLM 编排链路（经 OpenFeign）上报 Token 用量与费用、按条件聚合统计，包括：
 * <ul>
 *     <li>上报单次模型用量（totalTokens 未传时服务层兜底计算）；</li>
 *     <li>按 userId/scenario/model/时间范围统计（内存聚合）。</li>
 * </ul>
 * 统一返回 {@link Result} 包装结构。
 * </p>
 */
@Tag(name = "模型用量计量")
@RestController
@RequestMapping("/api/model-usage")
@RequiredArgsConstructor
@Validated
public class ModelUsageController {

    /** 模型用量计量服务 */
    private final ModelUsageService modelUsageService;

    /**
     * 上报单次模型用量
     *
     * @param dto 模型用量信息
     * @return 空结果包装
     */
    @Operation(summary = "上报模型用量")
    @PostMapping("/records")
    public Result<Void> recordUsage(@Valid @RequestBody ModelUsageDTO dto) {
        // 委托服务层落库（totalTokens 未传时按 input+output 兜底计算）
        modelUsageService.recordUsage(dto);
        return Result.success();
    }

    /**
     * 按条件统计模型用量（userId/scenario/model/时间范围均可空过滤）
     *
     * @param userId    用户ID（可空）
     * @param scenario  场景（可空）
     * @param model     模型名（可空）
     * @param startTime 起始时间（可空，格式 yyyy-MM-dd HH:mm:ss）
     * @param endTime   结束时间（可空，格式 yyyy-MM-dd HH:mm:ss）
     * @return 用量统计结果
     */
    @Operation(summary = "统计模型用量")
    @GetMapping("/stats")
    public Result<ModelUsageStatsVO> stats(
            @RequestParam(required = false) Long userId,
            @RequestParam(required = false) String scenario,
            @RequestParam(required = false) String model,
            // @DateTimeFormat 为什么必须显式声明：Spring 默认只支持 ISO 格式解析 LocalDateTime，
            // 而前端/运维习惯传 "yyyy-MM-dd HH:mm:ss"，不注解会直接 400；
            // 与查询串"&"分隔的 QueryParam 配合，格式可读、可缓存
            @RequestParam(required = false) @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss") LocalDateTime startTime,
            @RequestParam(required = false) @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss") LocalDateTime endTime) {
        // 统计：过滤条件均为空值时可跳过对应过滤
        return Result.success(modelUsageService.stats(userId, scenario, model, startTime, endTime));
    }
}
