package com.aics.message.service;

import com.aics.message.dto.OnlineEvalRecordDTO;
import com.aics.message.dto.UserFeedbackDTO;
import com.aics.message.vo.OnlineEvalStatsVO;
import com.aics.message.vo.UserFeedbackVO;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 线上评估与反馈服务接口
 * <p>
 * 所属模块：ai-cs-message。
 * 职责：定义线上采样评估（online_eval_record 表）与用户反馈（user_feedback 表）的
 * 写入与查询能力，供 chat 模块上报评估/反馈并支撑质量看板。
 * 统计约定：stats 使用 LambdaQueryWrapper 过滤 + selectList 全量查出后内存聚合，
 * 不写自定义 SQL，保证可 mock 单元测试。
 * 写入约定：saveFeedback 不校验 requestId 存在性（未知 requestId 照常插入）。
 * 实现类：{@link com.aics.message.service.impl.OnlineEvalServiceImpl}。
 * 调用方：{@link com.aics.message.controller.OnlineEvalController}。
 *
 * <h3>【设计原理】为什么本服务聚合两张表的统计</h3>
 * <ul>
 *   <li>评估（LLM-as-Judge 结果）与反馈（用户点赞/点踩）在业务上是同一个"质量看板"视图，
 *       由本服务合并为 {@code OnlineEvalStatsVO} 一次返回，调用方无需两次调用；</li>
 *   <li>内部仍保持两张表独立存储、独立 selectList，各自语义清晰，
 *       避免为"看起来像一件事"而引入 JOIN 的笛卡尔积计数陷阱。</li>
 * </ul>
 * </p>
 */
public interface OnlineEvalService {

    /**
     * 记录线上采样评估
     *
     * @param dto 评估记录信息
     */
    void recordEval(OnlineEvalRecordDTO dto);

    /**
     * 统计线上评估与用户反馈（时间范围可空过滤，内存聚合）
     *
     * @param startTime 起始时间（可空）
     * @param endTime   结束时间（可空）
     * @return 评估与反馈统计结果
     */
    OnlineEvalStatsVO stats(LocalDateTime startTime, LocalDateTime endTime);

    /**
     * 保存用户反馈（requestId 不存在也照常插入，不校验存在性）
     *
     * @param dto 反馈信息
     */
    void saveFeedback(UserFeedbackDTO dto);

    /**
     * 查询用户反馈列表（requestId/时间范围可空过滤，create_time 倒序）
     *
     * @param requestId 请求ID（可空）
     * @param startTime 起始时间（可空）
     * @param endTime   结束时间（可空）
     * @return 反馈列表（按创建时间倒序）
     */
    List<UserFeedbackVO> listFeedback(String requestId, LocalDateTime startTime, LocalDateTime endTime);
}
