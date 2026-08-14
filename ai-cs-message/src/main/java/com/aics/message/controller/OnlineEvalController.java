package com.aics.message.controller;

import com.aics.common.result.Result;
import com.aics.message.dto.OnlineEvalRecordDTO;
import com.aics.message.dto.UserFeedbackDTO;
import com.aics.message.service.OnlineEvalService;
import com.aics.message.vo.OnlineEvalStatsVO;
import com.aics.message.vo.UserFeedbackVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 线上评估与反馈控制器
 * <p>
 * 所属模块：ai-cs-message。
 * 职责：对外暴露线上评估与用户反馈的 REST 接口（/api/eval/*），供 chat 模块
 * （经 OpenFeign）上报评估/反馈并支撑质量看板，包括：
 * <ul>
 *     <li>上报线上采样评估记录；</li>
 *     <li>按时间范围统计评估与反馈指标；</li>
 *     <li>上报用户反馈（不校验 requestId 存在性）；</li>
 *     <li>按 requestId/时间范围查询反馈列表（create_time 倒序）。</li>
 * </ul>
 * 统一返回 {@link Result} 包装结构。
 * </p>
 */
@Tag(name = "线上评估与反馈")
@RestController
@RequestMapping("/api/eval")
@RequiredArgsConstructor
@Validated
public class OnlineEvalController {

    /** 线上评估与反馈服务 */
    private final OnlineEvalService onlineEvalService;

    /**
     * 上报线上采样评估记录
     *
     * @param dto 评估记录信息
     * @return 空结果包装
     */
    @Operation(summary = "上报线上采样评估记录")
    @PostMapping("/online-records")
    public Result<Void> recordEval(@Valid @RequestBody OnlineEvalRecordDTO dto) {
        // 追加型写入：评估记录直接落库
        onlineEvalService.recordEval(dto);
        return Result.success();
    }

    /**
     * 统计线上评估与用户反馈（时间范围可空过滤）
     *
     * @param startTime 起始时间（可空，格式 yyyy-MM-dd HH:mm:ss）
     * @param endTime   结束时间（可空，格式 yyyy-MM-dd HH:mm:ss）
     * @return 评估与反馈统计结果
     */
    @Operation(summary = "统计线上评估与反馈")
    @GetMapping("/online-records/stats")
    public Result<OnlineEvalStatsVO> stats(
            @RequestParam(required = false) @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss") LocalDateTime startTime,
            @RequestParam(required = false) @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss") LocalDateTime endTime) {
        // 统计：时间范围为空时不参与过滤
        return Result.success(onlineEvalService.stats(startTime, endTime));
    }

    /**
     * 上报用户反馈（requestId 不存在也照常插入）
     *
     * @param dto 反馈信息
     * @return 空结果包装
     */
    @Operation(summary = "上报用户反馈")
    @PostMapping("/feedback")
    public Result<Void> saveFeedback(@Valid @RequestBody UserFeedbackDTO dto) {
        // 不校验 requestId 存在性：反馈是独立信号，未知 requestId 照常入库（服务层与 DTO 均不校验），
        // 保证用户侧的点赞/点踩在任何场景都不会因溯源缺失而丢失
        onlineEvalService.saveFeedback(dto);
        return Result.success();
    }

    /**
     * 查询用户反馈列表（requestId/时间范围可空过滤，create_time 倒序）
     *
     * @param requestId 请求ID（可空）
     * @param startTime 起始时间（可空，格式 yyyy-MM-dd HH:mm:ss）
     * @param endTime   结束时间（可空，格式 yyyy-MM-dd HH:mm:ss）
     * @return 反馈列表
     */
    @Operation(summary = "查询用户反馈列表")
    @GetMapping("/feedback")
    public Result<List<UserFeedbackVO>> listFeedback(
            @RequestParam(required = false) String requestId,
            @RequestParam(required = false) @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss") LocalDateTime startTime,
            @RequestParam(required = false) @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss") LocalDateTime endTime) {
        // 条件查询：requestId/时间范围为空时不参与过滤
        return Result.success(onlineEvalService.listFeedback(requestId, startTime, endTime));
    }
}
