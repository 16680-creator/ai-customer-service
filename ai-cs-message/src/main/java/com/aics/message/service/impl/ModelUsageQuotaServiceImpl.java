package com.aics.message.service.impl;

import com.aics.message.dto.ModelUsageQuotaDTO;
import com.aics.message.entity.ModelUsageQuota;
import com.aics.message.mapper.ModelUsageQuotaMapper;
import com.aics.message.service.ModelUsageQuotaService;
import com.aics.message.vo.ModelUsageQuotaVO;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

/**
 * 模型用量配额服务实现
 * <p>
 * 所属模块：ai-cs-message。
 * 职责：实现 {@link ModelUsageQuotaService}，基于 MyBatis-Plus Mapper 完成 model_usage_quota 表的读写。
 * 设计要点：
 * <ul>
 *     <li>upsert 先按 (userId, scenario) 查询：已存在则在原记录上更新（可空字段不覆盖原值），
 *     不存在则插入新记录（默认窗口 DAILY 由实体初始值保证）；</li>
 *     <li>查询不存在时返回 null（不抛异常），由调用方决定默认策略。</li>
 * </ul>
 * </p>
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ModelUsageQuotaServiceImpl implements ModelUsageQuotaService {

    /** 模型用量配额 Mapper */
    private final ModelUsageQuotaMapper modelUsageQuotaMapper;

    /**
     * 设置/更新配额：按 (userId, scenario) 先查后写，实现 upsert 语义。
     * 更新时仅覆盖非空字段（windowType/quotaTokens/quotaCost/periodStart），
     * 传入 NULL 表示保持原值（quota 为 NULL 表示不限，如需清空配额请显式传 0）。
     */
    @Override
    public void upsertQuota(ModelUsageQuotaDTO dto) {
        // 1. 按 (userId, scenario) 查询既有配额。
        //    为什么用 selectOne(wrapper) 而非 selectById：定位键是复合的 (userId, scenario)，
        //    selectById 只能按单主键查，这里必须走条件构造器；表级唯一键 uk_user_scenario 保证最多一条
        LambdaQueryWrapper<ModelUsageQuota> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(ModelUsageQuota::getUserId, dto.getUserId())
                .eq(ModelUsageQuota::getScenario, dto.getScenario());
        ModelUsageQuota existing = modelUsageQuotaMapper.selectOne(wrapper);
        if (existing != null) {
            // 2. 已存在则更新（可空字段不覆盖原值；windowType 缺省沿用原值）。
            //    为什么"只 set 非空字段 + updateById"：MyBatis-Plus 的 updateById 默认 FieldStrategy
            //    为 NOT_NULL（null 字段不进 SET 子句），配合"只 set 传入的非空值"，未传字段自然保留原值，
            //    天然实现"部分更新"语义，无需手工拼 SET 语句
            if (dto.getWindowType() != null) {
                existing.setWindowType(dto.getWindowType());
            }
            if (dto.getQuotaTokens() != null) {
                existing.setQuotaTokens(dto.getQuotaTokens());
            }
            if (dto.getQuotaCost() != null) {
                existing.setQuotaCost(dto.getQuotaCost());
            }
            if (dto.getPeriodStart() != null) {
                existing.setPeriodStart(dto.getPeriodStart());
            }
            modelUsageQuotaMapper.updateById(existing);
            log.info("模型用量配额已更新: userId={}, scenario={}, windowType={}",
                    dto.getUserId(), dto.getScenario(), existing.getWindowType());
            return;
        }
        // 3. 不存在则插入新配额（默认窗口 DAILY 由实体初始值保证）
        ModelUsageQuota quota = new ModelUsageQuota();
        quota.setUserId(dto.getUserId());
        quota.setScenario(dto.getScenario());
        if (dto.getWindowType() != null) {
            quota.setWindowType(dto.getWindowType());
        }
        if (dto.getQuotaTokens() != null) {
            quota.setQuotaTokens(dto.getQuotaTokens());
        }
        if (dto.getQuotaCost() != null) {
            quota.setQuotaCost(dto.getQuotaCost());
        }
        if (dto.getPeriodStart() != null) {
            quota.setPeriodStart(dto.getPeriodStart());
        }
        modelUsageQuotaMapper.insert(quota);
        log.info("模型用量配额已创建: userId={}, scenario={}, windowType={}",
                dto.getUserId(), dto.getScenario(), quota.getWindowType());
    }

    /**
     * 查询配额：不存在时返回 null（不抛异常），由调用方决定默认策略。
     */
    @Override
    public ModelUsageQuotaVO getQuota(Long userId, String scenario) {
        // 按 (userId, scenario) 查询：不存在返回 null，保持查询语义宽松
        LambdaQueryWrapper<ModelUsageQuota> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(ModelUsageQuota::getUserId, userId)
                .eq(ModelUsageQuota::getScenario, scenario);
        ModelUsageQuota quota = modelUsageQuotaMapper.selectOne(wrapper);
        if (quota == null) {
            log.info("模型用量配额不存在: userId={}, scenario={}", userId, scenario);
            return null;
        }
        return toVO(quota);
    }

    /**
     * 实体转 VO（模型用量配额）
     */
    private static ModelUsageQuotaVO toVO(ModelUsageQuota quota) {
        // 实体转 VO：字段逐一拷贝，供查询响应使用
        ModelUsageQuotaVO vo = new ModelUsageQuotaVO();
        vo.setUserId(quota.getUserId());
        vo.setScenario(quota.getScenario());
        vo.setWindowType(quota.getWindowType());
        vo.setQuotaTokens(quota.getQuotaTokens());
        vo.setQuotaCost(quota.getQuotaCost());
        vo.setPeriodStart(quota.getPeriodStart());
        vo.setCreateTime(quota.getCreateTime());
        vo.setUpdateTime(quota.getUpdateTime());
        return vo;
    }
}
