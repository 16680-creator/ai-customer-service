package com.aics.message.service;

import com.aics.message.dto.LlmTraceDTO;
import com.aics.message.dto.PageResult;
import com.aics.message.entity.LlmTrace;
import com.aics.message.mapper.LlmTraceMapper;
import com.aics.message.service.impl.LlmTraceServiceImpl;
import com.aics.message.vo.LlmTraceVO;
import com.baomidou.mybatisplus.core.conditions.Wrapper;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.Arrays;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * LLM 调用链追踪服务单元测试
 * <p>
 * TDD：先写测试（Red），再实现 {@link LlmTraceServiceImpl} 至通过（Green）。
 * 纯 Mockito 单测（与模块既有约定一致，Mapper 全部 mock），不加载 Spring 上下文，
 * 避免引入 RocketMQ / Redis / Nacos 等外部依赖。
 * 覆盖：创建成功 / 幂等 / 查询存在 / 查询缺失返回 null / 分页过滤与倒序。
 *
 * <h3>【测试设计】为什么用 ArgumentCaptor 断言"传给 mock 的参数"</h3>
 * <p>Mapper 是 mock 时，它的 insert/selectPage 返回值由 when() 决定，
 * 无法证明"服务层把 DTO 的值正确组装进了实体/条件"；用 {@code verify(mapper).xxx(captor.capture())}
 * 捕获真实入参再逐字段断言，是验证"服务层组装逻辑"的唯一手段。</p>
 * </p>
 */
@ExtendWith(MockitoExtension.class)
class LlmTraceServiceTest {

    @Mock
    private LlmTraceMapper llmTraceMapper;

    @InjectMocks
    private LlmTraceServiceImpl llmTraceService;

    // ==================== createTrace ====================

    @Test
    @DisplayName("创建调用链追踪 - 成功返回 requestId，默认状态 SUCCESS/耗时 0")
    void createTrace_success() {
        LlmTraceDTO dto = buildTraceDTO("trace-1");
        when(llmTraceMapper.selectById("trace-1")).thenReturn(null);
        when(llmTraceMapper.insert(any(LlmTrace.class))).thenReturn(1);

        String requestId = llmTraceService.createTrace(dto);

        assertEquals("trace-1", requestId);
        // ArgumentCaptor 捕获 insert 的真实入参：验证"服务层把 DTO 字段逐一装进实体"
        // （含默认值兜底 status=SUCCESS / totalDurationMs=0），而非只验证 insert 被调用
        ArgumentCaptor<LlmTrace> captor = ArgumentCaptor.forClass(LlmTrace.class);
        verify(llmTraceMapper).insert(captor.capture());
        LlmTrace inserted = captor.getValue();
        assertEquals("trace-1", inserted.getRequestId());
        assertEquals(1000L, inserted.getUserId());
        assertEquals("1001", inserted.getSessionId());
        assertEquals("chat", inserted.getScenario());
        assertEquals("SUCCESS", inserted.getStatus());
        assertEquals(0L, inserted.getTotalDurationMs());
        assertNull(inserted.getErrorSummary());
    }

    @Test
    @DisplayName("创建调用链追踪 - requestId 已存在时幂等返回，不重复插入")
    void createTrace_idempotent() {
        LlmTrace existing = new LlmTrace();
        existing.setRequestId("trace-1");
        when(llmTraceMapper.selectById("trace-1")).thenReturn(existing);

        String requestId = llmTraceService.createTrace(buildTraceDTO("trace-1"));

        assertEquals("trace-1", requestId);
        // 幂等的关键断言：verify(..., never()) 证明"已存在时一次都不插入"，
        // 这正是"不覆盖首次记录"契约的测试表达
        verify(llmTraceMapper, never()).insert(any());
    }

    // ==================== getTrace ====================

    @Test
    @DisplayName("查询调用链追踪 - 存在时返回完整 VO")
    void getTrace_found() {
        LlmTrace trace = new LlmTrace();
        trace.setRequestId("trace-1");
        trace.setUserId(1000L);
        trace.setSessionId("1001");
        trace.setScenario("chat");
        trace.setStatus("FAILED");
        trace.setTotalDurationMs(1500L);
        trace.setSpansJson("[{\"name\":\"llm-call\"}]");
        trace.setErrorSummary("timeout");
        trace.setCreateTime(LocalDateTime.of(2026, 8, 14, 10, 30));
        when(llmTraceMapper.selectById("trace-1")).thenReturn(trace);

        LlmTraceVO vo = llmTraceService.getTrace("trace-1");

        assertNotNull(vo);
        assertEquals("trace-1", vo.getRequestId());
        assertEquals(1000L, vo.getUserId());
        assertEquals("1001", vo.getSessionId());
        assertEquals("chat", vo.getScenario());
        assertEquals("FAILED", vo.getStatus());
        assertEquals(1500L, vo.getTotalDurationMs());
        assertEquals("timeout", vo.getErrorSummary());
        assertEquals(LocalDateTime.of(2026, 8, 14, 10, 30), vo.getCreateTime());
    }

    @Test
    @DisplayName("查询调用链追踪 - 不存在返回 null，不抛异常")
    void getTrace_notFound_returnsNull() {
        when(llmTraceMapper.selectById("missing")).thenReturn(null);

        LlmTraceVO vo = llmTraceService.getTrace("missing");

        assertNull(vo);
    }

    // ==================== pageTraces ====================

    @Test
    @DisplayName("分页查询 - userId/scenario 过滤且 create_time 倒序，实体转 VO")
    void pageTraces_filterAndOrder() {
        LlmTrace t1 = new LlmTrace();
        t1.setRequestId("trace-1");
        t1.setUserId(1000L);
        t1.setScenario("chat");
        LlmTrace t2 = new LlmTrace();
        t2.setRequestId("trace-2");
        t2.setUserId(1000L);
        t2.setScenario("chat");
        Page<LlmTrace> page = new Page<>(2, 10, 25);
        page.setRecords(Arrays.asList(t1, t2));
        when(llmTraceMapper.selectPage(any(), any())).thenReturn(page);

        PageResult<LlmTraceVO> result = llmTraceService.pageTraces(1000L, "chat", 2, 10);

        // 分页元数据与记录转换正确
        assertEquals(25, result.getTotal());
        assertEquals(2, result.getPage());
        assertEquals(10, result.getSize());
        assertEquals(2, result.getRecords().size());
        assertEquals("trace-1", result.getRecords().get(0).getRequestId());
        assertEquals("chat", result.getRecords().get(0).getScenario());

        // 分页参数传递正确
        ArgumentCaptor<Page<LlmTrace>> pageCaptor = ArgumentCaptor.forClass(Page.class);
        ArgumentCaptor<Wrapper<LlmTrace>> wrapperCaptor = ArgumentCaptor.forClass(Wrapper.class);
        verify(llmTraceMapper).selectPage(pageCaptor.capture(), wrapperCaptor.capture());
        assertEquals(2L, pageCaptor.getValue().getCurrent());
        assertEquals(10L, pageCaptor.getValue().getSize());

        // 过滤条件（userId/scenario）与倒序（ORDER BY create_time DESC）正确：
        // 为什么断言 getExpression() 的片段数而非 getSqlSegment()：纯 Mockito 下没有 MyBatis-Plus
        // 的实体表信息缓存（TableInfo），getSqlSegment() 会抛 "can not find lambda cache"；
        // 片段数则是 MP 条件 DSL 的可观测面——首个条件 3 片段（列名/运算符/参数），
        // 后续条件各增 4 片段（含隐式 AND），2 个条件 = 7；orderByDesc 在 orderBy 列表占 1 片段
        LambdaQueryWrapper<LlmTrace> wrapper = (LambdaQueryWrapper<LlmTrace>) wrapperCaptor.getValue();
        assertEquals(7, wrapper.getExpression().getNormal().size(), "应生成 userId/scenario 两个过滤条件");
        assertEquals(1, wrapper.getExpression().getOrderBy().size(), "应按 create_time 倒序");
    }

    @Test
    @DisplayName("分页查询 - userId/scenario 为空时不参与过滤")
    void pageTraces_nullFilters_noCondition() {
        Page<LlmTrace> page = new Page<>(1, 20, 0);
        page.setRecords(java.util.Collections.emptyList());
        when(llmTraceMapper.selectPage(any(), any())).thenReturn(page);

        PageResult<LlmTraceVO> result = llmTraceService.pageTraces(null, null, 1, 20);

        assertEquals(0, result.getTotal());
        assertTrue(result.getRecords().isEmpty());

        ArgumentCaptor<Wrapper<LlmTrace>> wrapperCaptor = ArgumentCaptor.forClass(Wrapper.class);
        verify(llmTraceMapper).selectPage(any(Page.class), wrapperCaptor.capture());
        LambdaQueryWrapper<LlmTrace> wrapper = (LambdaQueryWrapper<LlmTrace>) wrapperCaptor.getValue();
        assertEquals(0, wrapper.getExpression().getNormal().size(), "无过滤条件时 normal 片段应为空");
        assertEquals(1, wrapper.getExpression().getOrderBy().size(), "无过滤条件仍应保持倒序条件");
    }

    // ==================== 测试数据构造 ====================

    private LlmTraceDTO buildTraceDTO(String requestId) {
        LlmTraceDTO dto = new LlmTraceDTO();
        dto.setRequestId(requestId);
        dto.setUserId(1000L);
        dto.setSessionId("1001");
        dto.setScenario("chat");
        return dto;
    }
}
