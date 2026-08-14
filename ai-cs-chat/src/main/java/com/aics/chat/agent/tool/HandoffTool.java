package com.aics.chat.agent.tool;

import com.aics.chat.agent.state.AgentStateMachine;
import com.aics.chat.dto.HandoffNoticeDTO;
import com.aics.chat.dto.HandoffTicketDTO;
import com.aics.chat.dto.HandoffTicketVO;
import com.aics.chat.feign.AgentTraceFeignClient;
import com.aics.chat.feign.NotifyFeignClient;
import com.aics.chat.util.ChatUserContext;
import com.aics.common.result.Result;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * 转人工工具（写操作）：创建转人工工单并推送通知。
 *
 * <p>工单携带订单、情绪、问题摘要与已执行步骤（验收：转人工时携带完整上下文）。</p>
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class HandoffTool implements AgentTool {

    private final AgentTraceFeignClient agentTraceFeignClient;
    private final NotifyFeignClient notifyFeignClient;

    @Override
    public String name() {
        return AgentStateMachine.TOOL_HANDOFF;
    }

    @Override
    public RiskLevel riskLevel() {
        return RiskLevel.WRITE;
    }

    @Override
    public boolean requiresConfirmation() {
        return false;
    }

    /**
     * 创建转人工工单
     *
     * @param runId         执行 ID
     * @param sessionId     会话 ID
     * @param reason        触发原因
     * @param priority      优先级
     * @param orderNo       订单号（可为空）
     * @param sentiment     情绪
     * @param problemSummary 问题摘要
     * @param executedSteps 已执行步骤（JSON 数组字符串）
     * @return SUCCESS：工单号；FAIL：失败原因
     */
    public ToolResult createHandoff(String runId, Long sessionId, String reason, String priority,
                                    String orderNo, String sentiment, String problemSummary,
                                    String executedSteps) {
        // 未登录不能创建转人工工单
        Long userId = ChatUserContext.getUserId();
        if (userId == null) {
            return ToolResult.fail("无法识别当前登录用户身份，请先登录");
        }
        HandoffTicketDTO dto = new HandoffTicketDTO();
        dto.setRunId(runId);
        dto.setSessionId(sessionId);
        dto.setUserId(userId);
        dto.setReason(reason);
        dto.setPriority(priority);
        dto.setOrderNo(orderNo);
        dto.setSentiment(sentiment);
        dto.setProblemSummary(problemSummary);
        // 已执行步骤清单随工单移交（坐席可追溯 Agent 做过什么）
        dto.setExecutedSteps(executedSteps);
        try {
            Result<HandoffTicketVO> result = agentTraceFeignClient.createHandoffTicket(dto);
            if (result != null && result.isSuccess() && result.getData() != null) {
                String ticketNo = result.getData().getTicketNo();
                log.info("转人工工单创建成功: ticketNo={}, reason={}", ticketNo, reason);
                // 工单创建成功后再通知坐席侧
                notifyHandoff(ticketNo, userId, priority, orderNo, problemSummary);
                return ToolResult.success("转人工工单创建成功", ticketNo);
            }
            return ToolResult.fail(result != null ? result.getMessage() : "转人工工单创建失败");
        } catch (Exception e) {
            log.warn("转人工工单创建失败: reason={}, err={}", reason, e.getMessage());
            return ToolResult.fail("转人工工单创建失败：" + e.getMessage());
        }
    }

    /**
     * 通知坐席侧（尽力而为，失败不阻断转人工）
     */
    private void notifyHandoff(String ticketNo, Long userId, String priority,
                               String orderNo, String summary) {
        try {
            HandoffNoticeDTO notice = new HandoffNoticeDTO();
            notice.setTicketNo(ticketNo);
            notice.setUserId(userId);
            notice.setPriority(priority);
            notice.setOrderNo(orderNo);
            notice.setSummary(summary);
            notifyFeignClient.handoffNotice(notice);
        } catch (Exception e) {
            // 通知失败仅告警，不阻断转人工主流程
            log.warn("转人工通知推送失败: ticketNo={}, err={}", ticketNo, e.getMessage());
        }
    }
}
