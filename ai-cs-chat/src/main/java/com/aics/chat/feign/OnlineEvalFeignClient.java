package com.aics.chat.feign;

import com.aics.chat.dto.OnlineEvalRecordDTO;
import com.aics.chat.dto.OnlineEvalStatsVO;
import com.aics.chat.dto.UserFeedbackDTO;
import com.aics.chat.dto.UserFeedbackVO;
import com.aics.common.result.Result;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 消息服务线上评估与反馈 Feign 客户端（调用 ai-cs-message 持久化 online_eval_record / user_feedback）
 *
 * <h3>【AI 技术详解】为什么"评估"与"反馈"共用一个客户端？</h3>
 * <p>两者都是"线上质量信号"的采集通道，落库表相邻（online_eval_record / user_feedback）、
 * 统计口径同源（OnlineEvalStatsVO 同时汇总两者），共用一个 contextId 客户端让
 * chat 侧只需注入一个依赖、维护一份超时/重试配置；若后续职责膨胀再拆分为独立客户端。</p>
 *
 * <p><b>只读接口的时间窗口</b>：getStats / listFeedback 都以 startTime~endTime 过滤，
 * 服务端有默认窗口兜底（如近 7 天），客户端不传时不会全表扫描。</p>
 */
@FeignClient(name = "ai-cs-message", contextId = "onlineEval")
public interface OnlineEvalFeignClient {

    /**
     * 上报线上采样评估记录
     *
     * <p>由 OnlineEvalService 在 evalExecutor 线程池异步调用；评估是"尽力而为"的
     * 增强数据，失败不重试、不阻断主链路。</p>
     */
    @PostMapping("/api/eval/online-records")
    Result<Void> recordEval(@RequestBody OnlineEvalRecordDTO dto);

    /**
     * 查询线上评估与反馈统计（时间窗口内）
     */
    @GetMapping("/api/eval/online-records/stats")
    Result<OnlineEvalStatsVO> getStats(@RequestParam(value = "startTime", required = false)
                                       @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss") LocalDateTime startTime,
                                       @RequestParam(value = "endTime", required = false)
                                       @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss") LocalDateTime endTime);

    /**
     * 上报用户反馈（点赞/点踩/评分）
     *
     * <p>反馈是用户主动行为，同步调用失败应让用户感知（前端提示重试），
     * 因此不放入异步线程池，与 recordEval 的"尽力而为"策略不同。</p>
     */
    @PostMapping("/api/eval/feedback")
    Result<Void> saveFeedback(@RequestBody UserFeedbackDTO dto);

    /**
     * 查询用户反馈
     */
    @GetMapping("/api/eval/feedback")
    Result<List<UserFeedbackVO>> listFeedback(@RequestParam(value = "requestId", required = false) String requestId,
                                              @RequestParam(value = "startTime", required = false)
                                              @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss") LocalDateTime startTime,
                                              @RequestParam(value = "endTime", required = false)
                                              @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss") LocalDateTime endTime);
}
