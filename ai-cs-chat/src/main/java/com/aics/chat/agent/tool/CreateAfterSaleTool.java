package com.aics.chat.agent.tool;

import com.aics.chat.agent.model.AfterSaleActionType;
import com.aics.chat.agent.model.AgentActionPlan;
import com.aics.chat.agent.state.AgentStateMachine;
import com.aics.chat.dto.AfterSaleApplyDTO;
import com.aics.chat.dto.AfterSaleApplyVO;
import com.aics.chat.feign.AfterSaleFeignClient;
import com.aics.chat.util.ChatUserContext;
import com.aics.common.result.Result;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * 创建售后申请工具（写操作，必须用户确认后执行）
 *
 * <p>幂等键 = runId + 动作类型，订单服务按唯一键去重，重试不产生重复申请。</p>
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class CreateAfterSaleTool implements AgentTool {

    private final AfterSaleFeignClient afterSaleFeignClient;

    @Override
    public String name() {
        return AgentStateMachine.TOOL_CREATE_AFTER_SALE;
    }

    @Override
    public RiskLevel riskLevel() {
        return RiskLevel.WRITE;
    }

    @Override
    public boolean requiresConfirmation() {
        return true;
    }

    /**
     * 创建售后申请
     *
     * @param plan   操作计划（确认过的内容）
     * @param runId  执行 ID（幂等键组成部分）
     * @return SUCCESS：申请单号；FAIL：失败原因
     */
    public ToolResult create(AgentActionPlan plan, String runId) {
        // 未登录不能执行写操作
        Long userId = ChatUserContext.getUserId();
        if (userId == null) {
            return ToolResult.fail("无法识别当前登录用户身份，请先登录");
        }
        AfterSaleApplyDTO dto = new AfterSaleApplyDTO();
        dto.setOrderNo(plan.orderNo());
        dto.setProductId(plan.productId());
        dto.setProductName(plan.productName());
        dto.setQuantity(plan.quantity());
        dto.setActionType(plan.actionType().getCode());
        dto.setReason(plan.reason());
        dto.setRunId(runId);
        // 幂等键 = runId + 动作类型，订单服务按此去重，重试不产生重复申请
        dto.setIdempotencyKey(runId + ":" + plan.actionType().getCode());
        dto.setEvidenceSummary(plan.evidenceSummary());
        try {
            // 调用订单服务创建售后申请（X-User-Id 透传做归属校验）
            Result<AfterSaleApplyVO> result = afterSaleFeignClient.apply(userId, dto);
            if (result != null && result.isSuccess() && result.getData() != null) {
                log.info("售后申请创建成功: applicationNo={}, idempotencyKey={}",
                        result.getData().getApplicationNo(), dto.getIdempotencyKey());
                return ToolResult.success("售后申请创建成功", result.getData());
            }
            return ToolResult.fail(result != null ? result.getMessage() : "售后申请创建失败");
        } catch (Exception e) {
            log.warn("售后申请调用失败: orderNo={}, err={}", plan.orderNo(), e.getMessage());
            // 调用异常：返回可解释失败，由上层重试或转人工
            return ToolResult.fail("售后申请服务暂时不可用：" + e.getMessage());
        }
    }
}
