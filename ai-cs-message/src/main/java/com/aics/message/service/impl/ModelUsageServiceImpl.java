package com.aics.message.service.impl;

import com.aics.message.dto.ModelUsageDTO;
import com.aics.message.entity.ModelUsage;
import com.aics.message.mapper.ModelUsageMapper;
import com.aics.message.service.ModelUsageService;
import com.aics.message.vo.ModelUsageStatsVO;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Objects;

/**
 * 模型用量计量服务实现
 * <p>
 * 所属模块：ai-cs-message。
 * 职责：实现 {@link ModelUsageService}，基于 MyBatis-Plus Mapper 完成 model_usage 表的读写。
 * 设计要点：
 * <ul>
 *     <li>写入时 totalTokens 未上报则按 inputTokens + outputTokens 兜底计算；</li>
 *     <li>统计使用 {@link LambdaQueryWrapper} 过滤 + {@code selectList} 全量查出后内存聚合
 *     （调用次数/各 Token 求和/费用求和），不写自定义 SQL，保证 Mapper 可 mock 单测；</li>
 *     <li>过滤条件均支持空值（userId/scenario/model/startTime/endTime 为空时不参与过滤）。</li>
 * </ul>
 * </p>
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ModelUsageServiceImpl implements ModelUsageService {

    /** 模型用量计量 Mapper */
    private final ModelUsageMapper modelUsageMapper;

    /**
     * 记录模型用量：totalTokens 未传时按 input+output 兜底计算；
     * 默认状态 SUCCESS、非估算由实体字段初始值保证（DTO 未传时生效）。
     */
    @Override
    public void recordUsage(ModelUsageDTO dto) {
        // 1. 组装用量实体（totalTokens 未上报时按 input+output 兜底计算）
        ModelUsage usage = new ModelUsage();
        usage.setRequestId(dto.getRequestId());
        usage.setUserId(dto.getUserId());
        usage.setSessionId(dto.getSessionId());
        usage.setScenario(dto.getScenario());
        usage.setProvider(dto.getProvider());
        usage.setModel(dto.getModel());
        if (dto.getInputTokens() != null) {
            usage.setInputTokens(dto.getInputTokens());
        }
        if (dto.getOutputTokens() != null) {
            usage.setOutputTokens(dto.getOutputTokens());
        }
        if (dto.getTotalTokens() != null) {
            usage.setTotalTokens(dto.getTotalTokens());
        } else {
            // 兜底：总 Token 数 = 输入 + 输出（null 按 0 处理）。
            // 为什么放在服务层：兜底是"业务计算"（依赖两个字段求和），DTO 保持纯数据不掺逻辑；
            // 单独成 else 分支而非"实体默认 0"：因为实体默认值无法表达"input+output"这种派生关系
            int input = dto.getInputTokens() == null ? 0 : dto.getInputTokens();
            int output = dto.getOutputTokens() == null ? 0 : dto.getOutputTokens();
            usage.setTotalTokens(input + output);
        }
        if (dto.getEstimatedCost() != null) {
            usage.setEstimatedCost(dto.getEstimatedCost());
        }
        if (dto.getEstimated() != null) {
            usage.setEstimated(dto.getEstimated());
        }
        if (dto.getStatus() != null) {
            usage.setStatus(dto.getStatus());
        }
        usage.setErrorSummary(dto.getErrorSummary());
        // 2. 落库（createTime 由 MetaObjectHandler 自动填充）
        modelUsageMapper.insert(usage);
        log.info("模型用量已记录: scenario={}, model={}, totalTokens={}", usage.getScenario(), usage.getModel(), usage.getTotalTokens());
    }

    /**
     * 按条件统计模型用量：过滤后全量查出，内存聚合调用次数/Token 求和/费用求和。
     */
    @Override
    public ModelUsageStatsVO stats(Long userId, String scenario, String model, LocalDateTime startTime, LocalDateTime endTime) {
        // 1. 组装过滤条件：userId/scenario/model/时间范围均可空，为空时不参与过滤
        LambdaQueryWrapper<ModelUsage> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(userId != null, ModelUsage::getUserId, userId)
                .eq(scenario != null, ModelUsage::getScenario, scenario)
                .eq(model != null, ModelUsage::getModel, model)
                .ge(startTime != null, ModelUsage::getCreateTime, startTime)
                .le(endTime != null, ModelUsage::getCreateTime, endTime);
        // 2. 全量查出后在内存聚合（数据量级可控；不写自定义 SQL，保证 Mapper 可 mock）
        List<ModelUsage> records = modelUsageMapper.selectList(wrapper);
        ModelUsageStatsVO vo = new ModelUsageStatsVO();
        vo.setCallCount((long) records.size());
        // 各 Token 维度用 mapToLong(...).sum()：null 显式按 0 计入，避免 NPE 打断整条聚合流
        vo.setInputTokens(records.stream()
                .mapToLong(u -> u.getInputTokens() == null ? 0L : u.getInputTokens()).sum());
        vo.setOutputTokens(records.stream()
                .mapToLong(u -> u.getOutputTokens() == null ? 0L : u.getOutputTokens()).sum());
        vo.setTotalTokens(records.stream()
                .mapToLong(u -> u.getTotalTokens() == null ? 0L : u.getTotalTokens()).sum());
        // 费用求和先 filter 掉 null：历史脏数据/未设费用的记录不应拉低或污染合计；
        // reduce(BigDecimal.ZERO, BigDecimal::add) 保证空列表也返回 0 而非 null
        vo.setEstimatedCost(records.stream()
                .map(ModelUsage::getEstimatedCost)
                .filter(Objects::nonNull)
                .reduce(BigDecimal.ZERO, BigDecimal::add));
        log.info("模型用量统计完成: 记录数={}, 总Token={}", vo.getCallCount(), vo.getTotalTokens());
        return vo;
    }
}
