package com.aics.message.controller;

import com.aics.common.result.Result;
import com.aics.message.dto.ModelUsageQuotaDTO;
import com.aics.message.service.ModelUsageQuotaService;
import com.aics.message.vo.ModelUsageQuotaVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

/**
 * 模型用量配额控制器
 * <p>
 * 所属模块：ai-cs-message。
 * 职责：对外暴露模型用量配额的 REST 接口（/api/model-usage/quota*），
 * 供成本治理配置与预检查使用，包括：
 * <ul>
 *     <li>按 (userId, scenario) 查询配额（不存在返回 success(null)）；</li>
 *     <li>按 (userId, scenario) 设置/更新配额（幂等 upsert）。</li>
 * </ul>
 * 统一返回 {@link Result} 包装结构。
 * </p>
 */
@Tag(name = "模型用量配额")
@RestController
@RequestMapping("/api/model-usage/quota")
@RequiredArgsConstructor
@Validated
public class ModelUsageQuotaController {

    /** 模型用量配额服务 */
    private final ModelUsageQuotaService modelUsageQuotaService;

    /**
     * 按 (userId, scenario) 查询模型用量配额（不存在返回 success(null)）
     *
     * @param userId   用户ID
     * @param scenario 场景
     * @return 配额信息，不存在时为 null
     */
    @Operation(summary = "查询模型用量配额")
    @GetMapping
    public Result<ModelUsageQuotaVO> getQuota(@RequestParam("userId") Long userId,
                                              @RequestParam("scenario") String scenario) {
        // 不带 required=false：userId/scenario 是配额定位键（GET 无默认语义），缺失直接 400，
        // 由类级 @Validated + 全局异常处理器统一处理，Controller 不写判空分支
        // 不存在时服务层返回 null，这里包装为 success(null)，不视为错误
        return Result.success(modelUsageQuotaService.getQuota(userId, scenario));
    }

    /**
     * 按 (userId, scenario) 设置/更新模型用量配额（幂等 upsert）
     *
     * @param dto 配额信息
     * @return 空结果包装
     */
    @Operation(summary = "设置模型用量配额")
    @PostMapping
    public Result<Void> upsertQuota(@Valid @RequestBody ModelUsageQuotaDTO dto) {
        // 幂等 upsert：已存在则更新，不存在则插入
        modelUsageQuotaService.upsertQuota(dto);
        return Result.success();
    }
}
