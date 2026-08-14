package com.aics.chat.agent.context;

import com.aics.chat.agent.model.AfterSaleActionType;
import com.aics.chat.agent.model.AgentActionPlan;
import com.aics.chat.agent.model.AgentIntentType;
import com.aics.chat.agent.model.HandoffInfo;
import com.aics.chat.agent.model.IntentResult;
import com.aics.chat.agent.model.PolicyCheckResult;
import com.aics.chat.agent.state.AfterSaleState;
import com.aics.chat.dto.OrderVO;
import com.aics.chat.dto.ProductRecommendVO;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * 单次 Agent run 的执行上下文（多轮共享，经 AgentRunStore 持久化）
 */
@Data
public class AfterSaleContext {

    /** 执行 ID */
    private String runId;

    /** 会话 ID */
    private Long sessionId;

    /** 用户 ID */
    private Long userId;

    /** 当前用户输入 */
    private String input;

    /** 当前状态 */
    private AfterSaleState state = AfterSaleState.START;

    /** 意图识别结果 */
    private IntentResult intentResult;

    /** 已确认的意图类型 */
    private List<AgentIntentType> intents = new ArrayList<>();

    /** 定位到的订单 */
    private OrderVO order;

    /** 候选订单（多候选时） */
    private List<OrderVO> candidates = new ArrayList<>();

    /** 用户选择的订单号（LOCATE_ORDER 等待选择时） */
    private String pendingOrderNo;

    /** 规则校验结果 */
    private PolicyCheckResult policyResult;

    /** 售后动作类型（从意图参数解析） */
    private AfterSaleActionType actionType;

    /** 用户补充提供的售后原因（COLLECT_EVIDENCE 等待输入） */
    private String userProvidedReason;

    /** 商品推荐结果 */
    private List<ProductRecommendVO> recommendations = new ArrayList<>();

    /** 待执行操作计划 */
    private AgentActionPlan actionPlan;

    /** 确认凭证 */
    private String confirmationToken;

    /** 操作摘要摘要值（SHA-256） */
    private String payloadDigest;

    /** 确认超时时间 */
    private LocalDateTime confirmationExpiresAt;

    /** 是否已确认 */
    private boolean confirmed;

    /** 转人工信息 */
    private HandoffInfo handoff;

    /** 售后申请单号 */
    private String applicationNo;

    /** 已执行步骤摘要（转人工移交用） */
    private List<String> stepSummaries = new ArrayList<>();

    /** 失败摘要 */
    private String errorSummary;

    /** 已执行步骤数 */
    private int steps;

    /** 创建时间 */
    private LocalDateTime createdAt = LocalDateTime.now();

    /** 是否需要用户输入（等待用户响应） */
    private boolean needsUserInput;

    public boolean isTerminal() {
        return state != null && state.isTerminal();
    }

    public void transit(AfterSaleState next) {
        // 迁移到下一状态：清除等待标记并累计步骤数
        this.state = next;
        this.needsUserInput = false;
        this.steps++;
    }

    public void markWaitingUser() {
        // 标记等待用户输入（状态机循环在此暂停）
        this.needsUserInput = true;
    }

    public void recordStep() {
        // 仅累计步骤数（不迁移状态）
        this.steps++;
    }
}
