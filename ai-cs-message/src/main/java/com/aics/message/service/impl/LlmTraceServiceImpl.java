package com.aics.message.service.impl;

import com.aics.message.dto.LlmTraceDTO;
import com.aics.message.dto.PageResult;
import com.aics.message.entity.LlmTrace;
import com.aics.message.mapper.LlmTraceMapper;
import com.aics.message.service.LlmTraceService;
import com.aics.message.vo.LlmTraceVO;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * LLM 调用链追踪服务实现
 * <p>
 * 所属模块：ai-cs-message。
 * 职责：实现 {@link LlmTraceService}，基于 MyBatis-Plus Mapper 完成 llm_trace 表的读写。
 * 设计要点：
 * <ul>
 *     <li>创建前先按 requestId 查询，已存在则幂等返回首次 requestId，不覆盖首次记录；</li>
 *     <li>查询单条不存在时返回 null（不抛异常），与 Agent 轨迹的强校验语义区分；</li>
 *     <li>分页查询使用 MyBatis-Plus {@link Page} + {@link LambdaQueryWrapper}，
 *     userId/scenario 可空过滤（条件布尔值控制），create_time 倒序保证最新在前。</li>
 * </ul>
 * </p>
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class LlmTraceServiceImpl implements LlmTraceService {

    /** LLM 调用链追踪 Mapper */
    private final LlmTraceMapper llmTraceMapper;

    /**
     * 创建调用链追踪：requestId 已存在时幂等返回已有 requestId，否则插入新记录。
     * 默认状态 SUCCESS、耗时 0 由实体字段初始值保证（DTO 未传时生效）。
     */
    @Override
    public String createTrace(LlmTraceDTO dto) {
        // 1. 幂等检查：按 requestId 查询，已存在则直接返回首次创建的 requestId，避免覆盖首次记录。
        //    为什么"先查后写"而不是"insert 后捕获主键冲突"：先查后写是常态路径，日志清晰；
        //    即便并发下有竞态，requestId 是表主键，重复插入会被 DB 主键约束拒绝，不会产生脏数据
        LlmTrace existing = llmTraceMapper.selectById(dto.getRequestId());
        if (existing != null) {
            log.info("LLM 调用链追踪已存在，幂等返回: requestId={}", dto.getRequestId());
            return existing.getRequestId();
        }
        // 2. 组装新追踪记录（可选字段为空时沿用实体默认值：SUCCESS / 0）
        LlmTrace trace = new LlmTrace();
        trace.setRequestId(dto.getRequestId());
        trace.setUserId(dto.getUserId());
        trace.setSessionId(dto.getSessionId());
        trace.setScenario(dto.getScenario());
        if (dto.getStatus() != null) {
            trace.setStatus(dto.getStatus());
        }
        if (dto.getTotalDurationMs() != null) {
            trace.setTotalDurationMs(dto.getTotalDurationMs());
        }
        trace.setSpansJson(dto.getSpansJson());
        trace.setErrorSummary(dto.getErrorSummary());
        // 3. 落库并返回 requestId（createTime 由 MetaObjectHandler 自动填充）
        llmTraceMapper.insert(trace);
        log.info("LLM 调用链追踪创建成功: requestId={}, scenario={}", trace.getRequestId(), trace.getScenario());
        return trace.getRequestId();
    }

    /**
     * 查询调用链追踪：不存在时返回 null（不抛异常），由调用方自行决定展示策略。
     */
    @Override
    public LlmTraceVO getTrace(String requestId) {
        // 单条查询：不存在返回 null，保持查询语义宽松（区别于 Agent 轨迹的强校验）
        LlmTrace trace = llmTraceMapper.selectById(requestId);
        if (trace == null) {
            log.info("LLM 调用链追踪不存在: requestId={}", requestId);
            return null;
        }
        return toVO(trace);
    }

    /**
     * 分页查询调用链追踪：userId/scenario 可空过滤，create_time 倒序（最新在前）。
     */
    @Override
    public PageResult<LlmTraceVO> pageTraces(Long userId, String scenario, int page, int size) {
        // 1. 构造分页参数（MyBatis-Plus Page，配合分页插件生成 LIMIT 语句）
        Page<LlmTrace> pageParam = new Page<>(page, size);
        // 2. 组装查询条件：userId/scenario 为空时不参与过滤；按 create_time 倒序。
        //    eq(boolean, ...) 重载的精髓：把"是否过滤"写进条件本身，为 null 时该条件被跳过，
        //    无需 if/else 分支拼装 wrapper，可读性与可维护性都更好
        LambdaQueryWrapper<LlmTrace> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(userId != null, LlmTrace::getUserId, userId)
                .eq(scenario != null, LlmTrace::getScenario, scenario)
                .orderByDesc(LlmTrace::getCreateTime);
        // 3. 分页查询：selectPage 会把 records/total 回填到传入的 page 对象并返回，一查两得
        Page<LlmTrace> result = llmTraceMapper.selectPage(pageParam, wrapper);
        // 4. 实体转 VO 并组装分页结果（getCurrent/getSize 为 long，与 PageResult 字段类型对齐）
        List<LlmTraceVO> records = result.getRecords().stream().map(LlmTraceServiceImpl::toVO).toList();
        return new PageResult<>(records, result.getTotal(), result.getCurrent(), result.getSize());
    }

    /**
     * 实体转 VO（LLM 调用链追踪）
     * 为什么是 static：转换不依赖任何实例状态，静态方法可被流式调用
     * {@code stream().map(LlmTraceServiceImpl::toVO)} 直接引用为方法句柄，简洁且无闭包开销。
     */
    private static LlmTraceVO toVO(LlmTrace trace) {
        // 实体转 VO：字段逐一拷贝，供查询响应使用
        LlmTraceVO vo = new LlmTraceVO();
        vo.setRequestId(trace.getRequestId());
        vo.setUserId(trace.getUserId());
        vo.setSessionId(trace.getSessionId());
        vo.setScenario(trace.getScenario());
        vo.setStatus(trace.getStatus());
        vo.setTotalDurationMs(trace.getTotalDurationMs());
        vo.setSpansJson(trace.getSpansJson());
        vo.setErrorSummary(trace.getErrorSummary());
        vo.setCreateTime(trace.getCreateTime());
        return vo;
    }
}
