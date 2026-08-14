package com.aics.message.service.impl;

import com.aics.message.dto.OnlineEvalRecordDTO;
import com.aics.message.dto.UserFeedbackDTO;
import com.aics.message.entity.OnlineEvalRecord;
import com.aics.message.entity.UserFeedback;
import com.aics.message.mapper.OnlineEvalRecordMapper;
import com.aics.message.mapper.UserFeedbackMapper;
import com.aics.message.service.OnlineEvalService;
import com.aics.message.vo.OnlineEvalStatsVO;
import com.aics.message.vo.UserFeedbackVO;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 线上评估与反馈服务实现
 * <p>
 * 所属模块：ai-cs-message。
 * 职责：实现 {@link OnlineEvalService}，基于 MyBatis-Plus Mapper 完成
 * online_eval_record 与 user_feedback 两张表的读写。
 * 设计要点：
 * <ul>
 *     <li>写入不做存在性校验：评估/反馈均为追加型日志数据，requestId 未知时照常落库；</li>
 *     <li>统计使用 {@link LambdaQueryWrapper} 过滤 + {@code selectList} 全量查出后内存聚合
 *     （样本数/评分成功数/平均分/反馈总数/点赞数/点踩数），不写自定义 SQL，保证可 mock 单测；</li>
 *     <li>平均分仅统计评分成功（judgeStatus=SUCCESS）的样本，成功数为 0 时为 null。</li>
 * </ul>
 * </p>
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class OnlineEvalServiceImpl implements OnlineEvalService {

    /** 评分成功状态：SUCCESS */
    private static final String JUDGE_STATUS_SUCCESS = "SUCCESS";
    /** 反馈类型：点赞 LIKE / 点踩 DISLIKE */
    private static final String FEEDBACK_LIKE = "LIKE";
    private static final String FEEDBACK_DISLIKE = "DISLIKE";

    /** 线上采样评估记录 Mapper */
    private final OnlineEvalRecordMapper onlineEvalRecordMapper;
    /** 用户反馈 Mapper */
    private final UserFeedbackMapper userFeedbackMapper;

    /**
     * 记录线上采样评估：追加型写入，不做存在性校验，直接落库。
     */
    @Override
    public void recordEval(OnlineEvalRecordDTO dto) {
        // 组装评估实体（judgeStatus 必填由 DTO 校验保证；createTime 由 MetaObjectHandler 自动填充）
        OnlineEvalRecord record = new OnlineEvalRecord();
        record.setRequestId(dto.getRequestId());
        record.setSessionId(dto.getSessionId());
        record.setUserId(dto.getUserId());
        record.setQuestionDigest(dto.getQuestionDigest());
        record.setAnswerDigest(dto.getAnswerDigest());
        record.setLlmScore(dto.getLlmScore());
        record.setJudgeStatus(dto.getJudgeStatus());
        record.setErrorSummary(dto.getErrorSummary());
        onlineEvalRecordMapper.insert(record);
        log.info("线上采样评估已记录: requestId={}, judgeStatus={}", record.getRequestId(), record.getJudgeStatus());
    }

    /**
     * 统计线上评估与用户反馈：时间范围过滤后全量查出，内存聚合各项指标。
     * 为什么用两次 selectList 而不是一条 JOIN：两张表是"各自独立的聚合计数"，
     * JOIN 会让行数相乘（N 条评估 × M 条反馈 = N×M 行），count 语义被放大失真；
     * 分别查出、Java 侧各自统计再合并，语义精确且两个 Mapper 都可独立 mock。
     */
    @Override
    public OnlineEvalStatsVO stats(LocalDateTime startTime, LocalDateTime endTime) {
        // 1. 评估样本（时间范围过滤），内存统计
        LambdaQueryWrapper<OnlineEvalRecord> evalWrapper = new LambdaQueryWrapper<>();
        evalWrapper.ge(startTime != null, OnlineEvalRecord::getCreateTime, startTime)
                .le(endTime != null, OnlineEvalRecord::getCreateTime, endTime);
        List<OnlineEvalRecord> evals = onlineEvalRecordMapper.selectList(evalWrapper);
        // 评分成功数：judgeStatus = SUCCESS 的样本
        long scoredCount = evals.stream()
                .filter(e -> JUDGE_STATUS_SUCCESS.equals(e.getJudgeStatus())).count();
        // 平均分：仅统计评分成功且带分数的样本；成功数为 0 时为 null。
        // 为什么 scoredCount==0 返回 null：除零在"统计"语义下表示"无样本可评"，
        // 返回 null 而非 0.0 可避免看板误读为"平均 0 分（质量极差）"；
        // filter 里额外要求 llmScore != null 是防御：SUCCESS 但分数缺失的异常记录不计入平均分母
        Double avgLlmScore = scoredCount == 0 ? null :
                evals.stream()
                        .filter(e -> JUDGE_STATUS_SUCCESS.equals(e.getJudgeStatus()) && e.getLlmScore() != null)
                        .mapToInt(OnlineEvalRecord::getLlmScore)
                        .average()
                        .orElse(0.0);

        // 2. 用户反馈（时间范围过滤），内存统计
        LambdaQueryWrapper<UserFeedback> feedbackWrapper = new LambdaQueryWrapper<>();
        feedbackWrapper.ge(startTime != null, UserFeedback::getCreateTime, startTime)
                .le(endTime != null, UserFeedback::getCreateTime, endTime);
        List<UserFeedback> feedbacks = userFeedbackMapper.selectList(feedbackWrapper);
        long likeCount = feedbacks.stream()
                .filter(f -> FEEDBACK_LIKE.equals(f.getFeedbackType())).count();
        long dislikeCount = feedbacks.stream()
                .filter(f -> FEEDBACK_DISLIKE.equals(f.getFeedbackType())).count();

        // 3. 组装统计 VO
        OnlineEvalStatsVO vo = new OnlineEvalStatsVO();
        vo.setSampleCount((long) evals.size());
        vo.setScoredCount(scoredCount);
        vo.setAvgLlmScore(avgLlmScore);
        vo.setFeedbackCount((long) feedbacks.size());
        vo.setLikeCount(likeCount);
        vo.setDislikeCount(dislikeCount);
        log.info("线上评估与反馈统计完成: 样本数={}, 反馈数={}", vo.getSampleCount(), vo.getFeedbackCount());
        return vo;
    }

    /**
     * 保存用户反馈：追加型写入，requestId 不存在（未知）也照常插入，不校验存在性。
     */
    @Override
    public void saveFeedback(UserFeedbackDTO dto) {
        // 组装反馈实体（feedbackType 必填由 DTO 校验保证；createTime 由 MetaObjectHandler 自动填充）。
        // 为什么不做 requestId 存在性校验：反馈是独立信号，未知来源（requestId=null）也要入库，
        // 跨表查询 trace 只为了"校验存在"是浪费，还可能在 trace 尚未落库的竞态窗口丢反馈
        UserFeedback feedback = new UserFeedback();
        feedback.setRequestId(dto.getRequestId());
        feedback.setSessionId(dto.getSessionId());
        feedback.setUserId(dto.getUserId());
        feedback.setFeedbackType(dto.getFeedbackType());
        feedback.setScore(dto.getScore());
        feedback.setComment(dto.getComment());
        userFeedbackMapper.insert(feedback);
        log.info("用户反馈已保存: requestId={}, feedbackType={}", feedback.getRequestId(), feedback.getFeedbackType());
    }

    /**
     * 查询用户反馈列表：requestId/时间范围可空过滤，create_time 倒序（最新在前）。
     */
    @Override
    public List<UserFeedbackVO> listFeedback(String requestId, LocalDateTime startTime, LocalDateTime endTime) {
        // 组装查询条件：requestId/时间范围为空时不参与过滤；按 create_time 倒序（最新反馈排前，便于运营先看新信号）
        LambdaQueryWrapper<UserFeedback> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(requestId != null, UserFeedback::getRequestId, requestId)
                .ge(startTime != null, UserFeedback::getCreateTime, startTime)
                .le(endTime != null, UserFeedback::getCreateTime, endTime)
                .orderByDesc(UserFeedback::getCreateTime);
        // 查询并转 VO
        return userFeedbackMapper.selectList(wrapper).stream()
                .map(OnlineEvalServiceImpl::toFeedbackVO)
                .toList();
    }

    /**
     * 实体转 VO（用户反馈）
     */
    private static UserFeedbackVO toFeedbackVO(UserFeedback feedback) {
        // 实体转 VO：字段逐一拷贝，供查询响应使用
        UserFeedbackVO vo = new UserFeedbackVO();
        vo.setId(feedback.getId());
        vo.setRequestId(feedback.getRequestId());
        vo.setSessionId(feedback.getSessionId());
        vo.setUserId(feedback.getUserId());
        vo.setFeedbackType(feedback.getFeedbackType());
        vo.setScore(feedback.getScore());
        vo.setComment(feedback.getComment());
        vo.setCreateTime(feedback.getCreateTime());
        return vo;
    }
}
