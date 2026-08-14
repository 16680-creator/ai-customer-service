package com.aics.message.service;

import com.aics.message.dto.OnlineEvalRecordDTO;
import com.aics.message.dto.UserFeedbackDTO;
import com.aics.message.entity.OnlineEvalRecord;
import com.aics.message.entity.UserFeedback;
import com.aics.message.mapper.OnlineEvalRecordMapper;
import com.aics.message.mapper.UserFeedbackMapper;
import com.aics.message.service.impl.OnlineEvalServiceImpl;
import com.aics.message.vo.OnlineEvalStatsVO;
import com.aics.message.vo.UserFeedbackVO;
import com.baomidou.mybatisplus.core.conditions.Wrapper;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * 线上评估与反馈服务单元测试
 * <p>
 * TDD：先写测试（Red），再实现 {@link OnlineEvalServiceImpl} 至通过（Green）。
 * 纯 Mockito 单测（与模块既有约定一致，Mapper 全部 mock），不加载 Spring 上下文。
 * 覆盖：评估写入 / 统计聚合（样本/评分成功/平均分/反馈/点赞点踩）/ 评分成功数为 0 时平均分为 null /
 * 反馈写入 / 反馈列表过滤与倒序。
 * </p>
 */
@ExtendWith(MockitoExtension.class)
class OnlineEvalServiceTest {

    @Mock
    private OnlineEvalRecordMapper onlineEvalRecordMapper;

    @Mock
    private UserFeedbackMapper userFeedbackMapper;

    @InjectMocks
    private OnlineEvalServiceImpl onlineEvalService;

    // ==================== recordEval ====================

    @Test
    @DisplayName("记录评估 - 正常插入")
    void recordEval_shouldInsert() {
        OnlineEvalRecordDTO dto = new OnlineEvalRecordDTO();
        dto.setRequestId("trace-1");
        dto.setSessionId(100L);
        dto.setUserId(1000L);
        dto.setQuestionDigest("退货政策?");
        dto.setAnswerDigest("7 天无理由退货");
        dto.setLlmScore(4);
        dto.setJudgeStatus("SUCCESS");
        when(onlineEvalRecordMapper.insert(any(OnlineEvalRecord.class))).thenReturn(1);

        onlineEvalService.recordEval(dto);

        ArgumentCaptor<OnlineEvalRecord> captor = ArgumentCaptor.forClass(OnlineEvalRecord.class);
        verify(onlineEvalRecordMapper).insert(captor.capture());
        OnlineEvalRecord inserted = captor.getValue();
        assertEquals("trace-1", inserted.getRequestId());
        assertEquals(1000L, inserted.getUserId());
        assertEquals("SUCCESS", inserted.getJudgeStatus());
        assertEquals(4, inserted.getLlmScore());
    }

    // ==================== stats ====================

    @Test
    @DisplayName("统计 - 评估样本/评分成功/平均分/反馈/点赞点踩 全部正确")
    void stats_aggregates() {
        // 评估：2 条评分成功（4/5 分）+ 1 条失败
        OnlineEvalRecord e1 = new OnlineEvalRecord();
        e1.setJudgeStatus("SUCCESS");
        e1.setLlmScore(4);
        OnlineEvalRecord e2 = new OnlineEvalRecord();
        e2.setJudgeStatus("SUCCESS");
        e2.setLlmScore(5);
        OnlineEvalRecord e3 = new OnlineEvalRecord();
        e3.setJudgeStatus("FAILED");
        e3.setLlmScore(null);
        when(onlineEvalRecordMapper.selectList(any())).thenReturn(Arrays.asList(e1, e2, e3));

        // 反馈：1 点赞 + 1 点踩 + 1 其他
        UserFeedback f1 = new UserFeedback();
        f1.setFeedbackType("LIKE");
        UserFeedback f2 = new UserFeedback();
        f2.setFeedbackType("DISLIKE");
        UserFeedback f3 = new UserFeedback();
        f3.setFeedbackType("NEUTRAL");
        when(userFeedbackMapper.selectList(any())).thenReturn(Arrays.asList(f1, f2, f3));

        LocalDateTime start = LocalDateTime.of(2026, 8, 14, 0, 0);
        LocalDateTime end = LocalDateTime.of(2026, 8, 14, 23, 59);
        OnlineEvalStatsVO vo = onlineEvalService.stats(start, end);

        assertEquals(3L, vo.getSampleCount());
        assertEquals(2L, vo.getScoredCount());
        assertEquals(4.5, vo.getAvgLlmScore(), 0.0001, "平均分 = (4+5)/2");
        assertEquals(3L, vo.getFeedbackCount());
        assertEquals(1L, vo.getLikeCount());
        assertEquals(1L, vo.getDislikeCount());
        // 测试数据刻意混入 1 条 NEUTRAL 反馈：验证统计只认 LIKE/DISLIKE，
        // 其他类型计入 feedbackCount 但不计入 like/dislike（口径不被未知类型污染）

        // 两个 Mapper 均以起止时间为条件查询（ge+le 两个条件：
        // 首个占 3 个片段，后续增 4 个片段含隐式 AND，共 7 个 normal 片段）
        ArgumentCaptor<Wrapper<OnlineEvalRecord>> evalWrapperCaptor = ArgumentCaptor.forClass(Wrapper.class);
        verify(onlineEvalRecordMapper).selectList(evalWrapperCaptor.capture());
        LambdaQueryWrapper<OnlineEvalRecord> evalWrapper =
                (LambdaQueryWrapper<OnlineEvalRecord>) evalWrapperCaptor.getValue();
        assertEquals(7, evalWrapper.getExpression().getNormal().size(), "评估查询应包含起止时间两个过滤条件");
        ArgumentCaptor<Wrapper<UserFeedback>> feedbackWrapperCaptor = ArgumentCaptor.forClass(Wrapper.class);
        verify(userFeedbackMapper).selectList(feedbackWrapperCaptor.capture());
        LambdaQueryWrapper<UserFeedback> feedbackWrapper =
                (LambdaQueryWrapper<UserFeedback>) feedbackWrapperCaptor.getValue();
        assertEquals(7, feedbackWrapper.getExpression().getNormal().size(), "反馈查询应包含起止时间两个过滤条件");
    }

    @Test
    @DisplayName("统计 - 评分成功数为 0 时平均分为 null")
    void stats_scoredZero_avgNull() {
        OnlineEvalRecord e1 = new OnlineEvalRecord();
        e1.setJudgeStatus("FAILED");
        e1.setLlmScore(null);
        OnlineEvalRecord e2 = new OnlineEvalRecord();
        e2.setJudgeStatus("SKIPPED");
        e2.setLlmScore(null);
        when(onlineEvalRecordMapper.selectList(any())).thenReturn(Arrays.asList(e1, e2));
        when(userFeedbackMapper.selectList(any())).thenReturn(Collections.emptyList());

        OnlineEvalStatsVO vo = onlineEvalService.stats(null, null);

        assertEquals(2L, vo.getSampleCount());
        assertEquals(0L, vo.getScoredCount());
        // 关键断言：评分成功数为 0 时平均分必须是 null 而非 0.0——
        // null 表达"无样本可评"（看板展示占位），0.0 会被误读为"平均 0 分"，
        // 这是 OnlineEvalStatsVO 语义契约的测试表达
        assertNull(vo.getAvgLlmScore(), "评分成功数为 0 时平均分应为 null");
        assertEquals(0L, vo.getFeedbackCount());
        assertEquals(0L, vo.getLikeCount());
        assertEquals(0L, vo.getDislikeCount());
    }

    // ==================== saveFeedback ====================

    @Test
    @DisplayName("保存反馈 - requestId 为空也照常插入，不校验存在性")
    void saveFeedback_unknownRequestId_shouldInsert() {
        UserFeedbackDTO dto = new UserFeedbackDTO();
        dto.setRequestId(null); // 未知 requestId
        dto.setSessionId(100L);
        dto.setUserId(1000L);
        dto.setFeedbackType("LIKE");
        dto.setScore(4);
        dto.setComment("回答清晰");
        when(userFeedbackMapper.insert(any(UserFeedback.class))).thenReturn(1);

        onlineEvalService.saveFeedback(dto);

        ArgumentCaptor<UserFeedback> captor = ArgumentCaptor.forClass(UserFeedback.class);
        verify(userFeedbackMapper).insert(captor.capture());
        UserFeedback inserted = captor.getValue();
        assertNull(inserted.getRequestId(), "requestId 未知时应为 null");
        assertEquals("LIKE", inserted.getFeedbackType());
        assertEquals(4, inserted.getScore());
        assertEquals("回答清晰", inserted.getComment());
    }

    // ==================== listFeedback ====================

    @Test
    @DisplayName("反馈列表 - requestId 过滤且 create_time 倒序，实体转 VO")
    void listFeedback_filterAndOrder() {
        UserFeedback f1 = new UserFeedback();
        f1.setId(1L);
        f1.setRequestId("trace-1");
        f1.setFeedbackType("LIKE");
        UserFeedback f2 = new UserFeedback();
        f2.setId(2L);
        f2.setRequestId("trace-1");
        f2.setFeedbackType("DISLIKE");
        when(userFeedbackMapper.selectList(any())).thenReturn(Arrays.asList(f1, f2));

        List<UserFeedbackVO> result = onlineEvalService.listFeedback("trace-1", null, null);

        assertEquals(2, result.size());
        assertEquals(1L, result.get(0).getId());
        assertEquals("LIKE", result.get(0).getFeedbackType());
        assertEquals("DISLIKE", result.get(1).getFeedbackType());

        // 过滤条件与倒序正确：1 个 eq 条件占 3 个 normal 片段；orderByDesc 在 orderBy 列表占 1 个片段
        ArgumentCaptor<Wrapper<UserFeedback>> wrapperCaptor = ArgumentCaptor.forClass(Wrapper.class);
        verify(userFeedbackMapper).selectList(wrapperCaptor.capture());
        LambdaQueryWrapper<UserFeedback> wrapper = (LambdaQueryWrapper<UserFeedback>) wrapperCaptor.getValue();
        assertEquals(3, wrapper.getExpression().getNormal().size(), "应包含 requestId 过滤条件");
        assertEquals(1, wrapper.getExpression().getOrderBy().size(), "应按 create_time 倒序");
    }

    @Test
    @DisplayName("反馈列表 - 过滤条件全空时返回全部")
    void listFeedback_noFilters() {
        when(userFeedbackMapper.selectList(any())).thenReturn(Collections.emptyList());

        List<UserFeedbackVO> result = onlineEvalService.listFeedback(null, null, null);

        assertTrue(result.isEmpty());
        ArgumentCaptor<Wrapper<UserFeedback>> wrapperCaptor = ArgumentCaptor.forClass(Wrapper.class);
        verify(userFeedbackMapper).selectList(wrapperCaptor.capture());
        LambdaQueryWrapper<UserFeedback> wrapper = (LambdaQueryWrapper<UserFeedback>) wrapperCaptor.getValue();
        assertEquals(0, wrapper.getExpression().getNormal().size(), "无过滤条件时 normal 片段应为空");
    }
}
