package com.aics.chat.agent.workflow;

import com.aics.chat.agent.AgentProperties;
import com.aics.chat.agent.confirm.ConfirmationService;
import com.aics.chat.agent.context.AfterSaleContext;
import com.aics.chat.agent.intent.IntentClassifierService;
import com.aics.chat.agent.model.AfterSaleActionType;
import com.aics.chat.agent.model.AgentActionPlan;
import com.aics.chat.agent.model.AgentIntent;
import com.aics.chat.agent.model.AgentIntentType;
import com.aics.chat.agent.model.AgentTurnResult;
import com.aics.chat.agent.model.HandoffInfo;
import com.aics.chat.agent.model.IntentResult;
import com.aics.chat.agent.model.PolicyCheckResult;
import com.aics.chat.agent.model.SafetyCheckResult;
import com.aics.chat.agent.safety.SafetyGuardService;
import com.aics.chat.agent.state.AfterSaleState;
import com.aics.chat.agent.state.AgentStateMachine;
import com.aics.chat.agent.store.AgentRunStore;
import com.aics.chat.agent.tool.AgentToolRegistry;
import com.aics.chat.agent.tool.CreateAfterSaleTool;
import com.aics.chat.agent.tool.HandoffTool;
import com.aics.chat.agent.tool.OrderLocatorTool;
import com.aics.chat.agent.tool.PolicyCheckTool;
import com.aics.chat.agent.tool.ProductRecommendTool;
import com.aics.chat.agent.tool.ToolResult;
import com.aics.chat.agent.trace.AgentTraceRecorder;
import com.aics.chat.observability.TraceSpans;
import com.aics.chat.dto.AfterSaleApplyVO;
import com.aics.chat.dto.OrderVO;
import com.aics.chat.dto.ProductRecommendVO;
import com.aics.common.exception.BusinessException;
import com.aics.common.result.ResultCode;
import io.micrometer.observation.ObservationRegistry;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Supplier;

/**
 * 售后 Agent 编排器：显式状态机驱动的多轮执行。
 *
 * <p>核心不变量：</p>
 * <ul>
 *   <li><b>写操作门禁</b>：创建售后申请前必须处于 CONFIRM_ACTION 且用户确认，
 *       未确认状态调用写工具直接抛 {@link ResultCode#AGENT_WRITE_OP_NOT_CONFIRMED}</li>
 *   <li><b>幂等</b>：写操作幂等键 = runId + 动作类型</li>
 *   <li><b>降级</b>：步骤超限/总超时/执行失败 → 可解释中止或转人工</li>
 *   <li><b>轨迹</b>：每个步骤摘要化落库，转人工携带订单、情绪、摘要与已执行步骤</li>
 * </ul>
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AfterSaleAgentService {

    private final AgentProperties properties;
    private final SafetyGuardService safetyGuardService;
    private final IntentClassifierService intentClassifierService;
    private final AgentStateMachine stateMachine;
    private final AgentToolRegistry toolRegistry;
    private final AgentRunStore runStore;
    private final ConfirmationService confirmationService;
    private final AgentTraceRecorder traceRecorder;
    private final OrderLocatorTool orderLocatorTool;
    private final PolicyCheckTool policyCheckTool;
    private final ProductRecommendTool productRecommendTool;
    private final CreateAfterSaleTool createAfterSaleTool;
    private final HandoffTool handoffTool;
    private final ObservationRegistry observationRegistry;

    // ==================== 对外入口 ====================

    /**
     * 处理一轮对话（新 run 或续跑）
     *
     * @param userId    用户 ID
     * @param sessionId 会话 ID
     * @param runId     续跑的 runId（可为空）
     * @param input     用户输入
     */
    public AgentTurnResult handleTurn(Long userId, Long sessionId, String runId, String input) {
        // 携带 runId 视为续跑既有 run
        if (StringUtils.hasText(runId)) {
            AfterSaleContext ctx = runStore.load(runId.trim())
                    .orElseThrow(() -> new BusinessException(ResultCode.AGENT_RUN_NOT_FOUND));
            return resumeTurn(ctx, input);
        }
        // 无 runId：开启新 run
        return startTurn(userId, sessionId, input);
    }

    // ==================== 新 run ====================

    private AgentTurnResult startTurn(Long userId, Long sessionId, String input) {
        // 生成新 run 的唯一 ID
        String newRunId = UUID.randomUUID().toString();
        AfterSaleContext ctx = new AfterSaleContext();
        ctx.setRunId(newRunId);
        ctx.setSessionId(sessionId);
        ctx.setUserId(userId);
        ctx.setInput(input);
        // 初始状态：进入意图识别
        ctx.transit(AfterSaleState.CLASSIFY_INTENT);
        // 落库执行记录（审计）
        traceRecorder.createRun(ctx);

        // 1. 输入安全检查（拦截后零工具调用）
        long t0 = System.currentTimeMillis();
        SafetyCheckResult safety = safetyGuardService.check(input);
        traceRecorder.step(ctx, "SAFETY", null, traceRecorder.digest(input),
                safety.passed() ? "PASS" : safety.reason(),
                System.currentTimeMillis() - t0,
                safety.passed() ? "SUCCESS" : "FAILED", safety.reason());
        ctx.getStepSummaries().add("安全检查: " + (safety.passed() ? "通过" : "拦截-" + safety.reason()));
        // 拦截：直接失败终止，本轮不触发任何工具调用
        if (!safety.passed()) {
            ctx.setState(AfterSaleState.FAILED);
            ctx.setErrorSummary(safety.reason());
            traceRecorder.updateRunStatus(ctx, "FAILED", safety.reason());
            runStore.save(ctx);
            return AgentTurnResult.of(newRunId, "FAILED", List.of(), safety.reason(),
                    false, false, null, null, List.of(), null, null, "AGENT_SAFETY_BLOCKED");
        }

        // 2. 意图识别
        t0 = System.currentTimeMillis();
        IntentResult intent = intentClassifierService.classify(input);
        ctx.setIntentResult(intent);
        // 记录识别出的意图类型（供结果回传）
        ctx.setIntents(new ArrayList<>(intent.intents().stream().map(AgentIntent::type).toList()));
        traceRecorder.step(ctx, "INTENT", null, traceRecorder.digest(input),
                summarizeIntent(intent), System.currentTimeMillis() - t0, "SUCCESS", null);
        ctx.getStepSummaries().add("意图识别: " + summarizeIntent(intent));

        // 2.1 情绪触发转人工（愤怒/强烈负面）
        if (intent.needsHandoff()) {
            return doHandoff(ctx, "NEGATIVE_SENTIMENT", "HIGH", null,
                    "检测到用户情绪" + intent.sentiment() + "，触发转人工");
        }
        // 2.2 用户主动要求转人工
        if (intent.has(AgentIntentType.HUMAN_HANDOFF)) {
            return doHandoff(ctx, "USER_REQUEST", "NORMAL", null, "用户主动要求转人工");
        }
        // 2.3 非售后意图：纯推荐走一次性推荐；其余路由回普通对话
        if (!intent.has(AgentIntentType.AFTER_SALE)) {
            ctx.setState(AfterSaleState.COMPLETED);
            traceRecorder.updateRunStatus(ctx, "COMPLETED", null);
            runStore.save(ctx);
            // 纯推荐场景：按预算召回后直接返回结果
            if (intent.has(AgentIntentType.PRODUCT_RECOMMEND)) {
                recommendByBudget(ctx);
                return buildResult(ctx);
            }
            return AgentTurnResult.of(newRunId, "NORMAL_CHAT", ctx.getIntents(), null,
                    false, true, null, null, List.of(), null, null, null);
        }

        // 3. 进入售后状态机
        ctx.setState(AfterSaleState.LOCATE_ORDER);
        runStore.save(ctx);
        // 驱动状态机执行
        return runStateMachine(ctx);
    }

    // ==================== 续跑（多轮） ====================

    private AgentTurnResult resumeTurn(AfterSaleContext ctx, String input) {
        ctx.setInput(input);
        ctx.setNeedsUserInput(false);
        // 终态（完成/取消/转人工/失败）：直接返回，重复请求幂等
        if (ctx.isTerminal()) {
            return buildResult(ctx);
        }
        // 总超时检查：超时则失败中止
        if (isTotalTimeout(ctx)) {
            return failWith(ctx, "AGENT_TIMEOUT", "执行超时（" + properties.getTotalTimeoutMs() + "ms），已中止");
        }
        switch (ctx.getState()) {
            case CONFIRM_ACTION -> {
                // 解析用户输入为 确认/拒绝/未知
                Decision decision = parseDecision(input);
                // 确认分支：校验凭证后放行写操作
                if (decision == Decision.CONFIRM) {
                    // 凭证有效（未超时且摘要一致）→ 标记已确认
                    if (confirmationService.validate(ctx, ctx.getActionPlan())) {
                        ctx.setConfirmed(true);
                        traceRecorder.confirmation(ctx, "CONFIRMED", ctx.getUserId());
                        ctx.getStepSummaries().add("用户确认: 同意执行 " + ctx.getActionPlan().actionType().getDesc());
                        // 迁移到执行态继续状态机
                        ctx.transit(AfterSaleState.EXECUTE_AFTER_SALE);
                        return runStateMachine(ctx);
                    }
                    String reason = confirmationService.isExpired(ctx)
                            ? "确认已超时，请重新发起售后操作" : "确认凭证无效，请重新发起售后操作";
                    return failWith(ctx, "AGENT_CONFIRMATION_EXPIRED", reason);
                }
                // 拒绝分支：记录后迁移到取消终态
                if (decision == Decision.REJECT) {
                    traceRecorder.confirmation(ctx, "REJECTED", ctx.getUserId());
                    ctx.getStepSummaries().add("用户拒绝: 不执行 " + ctx.getActionPlan().actionType().getDesc());
                    ctx.transit(AfterSaleState.CANCELLED);
                    traceRecorder.updateRunStatus(ctx, "CANCELLED", null);
                    runStore.save(ctx);
                    return buildResult(ctx);
                }
                // 无法识别确认/拒绝：保持确认态继续等待用户输入
                ctx.markWaitingUser();
                runStore.save(ctx);
                return buildResult(ctx);
            }
            case LOCATE_ORDER -> {
                // 记录用户选择的订单号后继续状态机
                ctx.setPendingOrderNo(input.trim());
                return runStateMachine(ctx);
            }
            case COLLECT_EVIDENCE -> {
                // 记录用户补充的售后原因后继续状态机
                ctx.setUserProvidedReason(input.trim());
                return runStateMachine(ctx);
            }
            default -> {
                return buildResult(ctx);
            }
        }
    }

    // ==================== 状态机驱动 ====================

    private AgentTurnResult runStateMachine(AfterSaleContext ctx) {
        // 循环保护计数：防止状态机异常死循环
        int guard = 0;
        while (!ctx.isTerminal() && !ctx.isNeedsUserInput()) {
            if (++guard > 50) {
                return failWith(ctx, "AGENT_TIMEOUT", "状态机执行异常，已中止");
            }
            // 步骤数超限：可解释中止
            if (ctx.getSteps() >= properties.getMaxSteps()) {
                return failWith(ctx, "AGENT_MAX_STEPS_EXCEEDED",
                        "超出最大步骤数（" + properties.getMaxSteps() + "），已中止");
            }
            if (isTotalTimeout(ctx)) {
                return failWith(ctx, "AGENT_TIMEOUT", "执行超时，已中止");
            }
            // 按当前状态分发到对应步骤处理器
            switch (ctx.getState()) {
                case LOCATE_ORDER -> locateOrder(ctx);
                case CHECK_POLICY -> checkPolicy(ctx);
                case COLLECT_EVIDENCE -> collectEvidence(ctx);
                case CONFIRM_ACTION -> issueConfirmation(ctx);
                case EXECUTE_AFTER_SALE -> executeAfterSale(ctx);
                default -> {
                    // 其他状态（START/CLASSIFY_INTENT/终态）不应进入循环体
                    return buildResult(ctx);
                }
            }
        }
        // 循环退出（终态或等待用户输入）后持久化上下文
        runStore.save(ctx);
        return buildResult(ctx);
    }

    /** 订单定位 */
    private void locateOrder(AfterSaleContext ctx) {
        long t0 = System.currentTimeMillis();
        // 定位订单：唯一命中/多候选/无订单三态
        ToolResult result = runTool(AgentStateMachine.TOOL_ORDER_LOCATOR,
                traceRecorder.digest(ctx.getPendingOrderNo()),
                () -> orderLocatorTool.locate(ctx.getPendingOrderNo()));
        traceRecorder.step(ctx, "LOCATE_ORDER", AgentStateMachine.TOOL_ORDER_LOCATOR,
                traceRecorder.digest(ctx.getPendingOrderNo()), result.message(),
                System.currentTimeMillis() - t0, result.isFail() ? "FAILED" : "SUCCESS", null);
        ctx.getStepSummaries().add("订单定位: " + result.message());
        // 成功：绑定唯一订单并进入规则校验
        if (result.isSuccess()) {
            ctx.setOrder((OrderVO) result.data());
            ctx.setCandidates(List.of());
            // 命中后若仍需推荐且尚未推荐：按订单商品价格召回
            if (ctx.getIntentResult().has(AgentIntentType.PRODUCT_RECOMMEND)
                    && ctx.getRecommendations().isEmpty()) {
                recommendForOrder(ctx);
            }
            ctx.transit(AfterSaleState.CHECK_POLICY);
        } else if (result.isCandidates()) {
            // 多候选：回传候选列表并等待用户选择
            ctx.setCandidates((List<OrderVO>) result.data());
            ctx.markWaitingUser();
            runStore.save(ctx);
        } else {
            // 无可用订单：按需推荐后结束本轮
            if (ctx.getIntentResult().has(AgentIntentType.PRODUCT_RECOMMEND)) {
                recommendByBudget(ctx);
            }
            ctx.transit(AfterSaleState.COMPLETED);
            traceRecorder.updateRunStatus(ctx, "COMPLETED", null);
        }
    }

    /** 售后规则校验 */
    private void checkPolicy(AfterSaleContext ctx) {
        // 从意图参数解析售后动作（缺省换货）
        AfterSaleActionType actionType = resolveAction(ctx);
        ctx.setActionType(actionType);
        long t0 = System.currentTimeMillis();
        // 按动作类型与订单时间做规则资格校验
        PolicyCheckResult policy = runTool(AgentStateMachine.TOOL_POLICY_CHECK,
                traceRecorder.digest(actionType.getCode()),
                () -> policyCheckTool.check(actionType, ctx.getOrder().getCreateTime()));
        traceRecorder.step(ctx, "CHECK_POLICY", AgentStateMachine.TOOL_POLICY_CHECK,
                traceRecorder.digest(actionType.getCode()), summarizePolicy(policy),
                System.currentTimeMillis() - t0, "SUCCESS", null);
        ctx.getStepSummaries().add("规则校验: " + summarizePolicy(policy));
        ctx.setPolicyResult(policy);
        // 资格满足：进入证据收集
        if (policy.eligible()) {
            ctx.transit(AfterSaleState.COLLECT_EVIDENCE);
        } else {
            // 资格不满足：转人工并携带订单与原因
            doHandoff(ctx, "POLICY_NOT_MET", "NORMAL", ctx.getOrder().getOrderNo(),
                    "售后资格校验不通过：" + policy.reason());
        }
    }

    /** 收集证据（原因等必要参数） */
    private void collectEvidence(AfterSaleContext ctx) {
        // 原因来源优先级：用户补充 > 动作计划 > 意图参数
        String reason = firstNonBlank(
                ctx.getUserProvidedReason(),
                ctx.getActionPlan() == null ? null : ctx.getActionPlan().reason(),
                param(ctx, "reason"));
        // 缺少原因：询问用户后暂停等待输入
        if (!StringUtils.hasText(reason)) {
            ctx.markWaitingUser();
            runStore.save(ctx);
            return;
        }
        OrderVO order = ctx.getOrder();
        OrderVO.OrderItemVO item = firstItem(order);
        // 组装写操作计划（含规则引用摘要，供确认展示）
        AgentActionPlan plan = new AgentActionPlan(
                ctx.getActionType(),
                order.getOrderNo(),
                item == null ? null : item.getProductId(),
                item == null ? null : item.getProductName(),
                item != null && item.getQuantity() != null ? item.getQuantity() : 1,
                reason,
                summarizePolicy(ctx.getPolicyResult()),
                item == null ? null : item.getProductPrice());
        ctx.setActionPlan(plan);
        ctx.getStepSummaries().add("参数收集: 动作=" + ctx.getActionType().getDesc() + ", 原因=" + reason);
        // 参数齐备：进入确认态（写操作前必须确认）
        ctx.transit(AfterSaleState.CONFIRM_ACTION);
    }

    /** 签发确认（写操作前必须确认） */
    private void issueConfirmation(AfterSaleContext ctx) {
        AgentActionPlan plan = ctx.getActionPlan();
        // 签发确认凭证并绑定操作摘要
        confirmationService.issue(ctx, plan);
        traceRecorder.confirmation(ctx, "PENDING", null);
        ctx.getStepSummaries().add("确认请求: " + plan.actionType().getDesc() + " " + plan.orderNo()
                + " " + plan.productName());
        // 等待用户在「确认/拒绝」间选择
        ctx.markWaitingUser();
        runStore.save(ctx);
    }

    /** 执行售后申请（幂等 + 重试） */
    private void executeAfterSale(AfterSaleContext ctx) {
        // 写操作门禁：未确认直接抛错，拒绝执行
        if (!ctx.isConfirmed()) {
            throw new BusinessException(ResultCode.AGENT_WRITE_OP_NOT_CONFIRMED,
                    "写操作未经确认，拒绝执行");
        }
        // 失败重试次数（配置，最少 0 次）；重试循环：成功即退出
        int retries = Math.max(0, properties.getWriteRetryTimes());
        ToolResult result = null;
        for (int i = 0; i <= retries; i++) {
            long t0 = System.currentTimeMillis();
            result = runTool(AgentStateMachine.TOOL_CREATE_AFTER_SALE,
                    traceRecorder.digest(ctx.getRunId() + ":" + ctx.getActionPlan().actionType().getCode()),
                    () -> createAfterSaleTool.create(ctx.getActionPlan(), ctx.getRunId()));
            traceRecorder.step(ctx, "EXECUTE", AgentStateMachine.TOOL_CREATE_AFTER_SALE,
                    traceRecorder.digest(ctx.getRunId() + ":" + ctx.getActionPlan().actionType().getCode()),
                    result.message(), System.currentTimeMillis() - t0,
                    result.isSuccess() ? "SUCCESS" : "FAILED", null);
            if (result.isSuccess()) {
                break;
            }
            log.warn("售后申请执行失败(第{}次): {}", i + 1, result.message());
        }
        // 成功：记录申请单号并迁移到完成态
        if (result != null && result.isSuccess()) {
            AfterSaleApplyVO vo = (AfterSaleApplyVO) result.data();
            ctx.setApplicationNo(vo.getApplicationNo());
            ctx.getStepSummaries().add("执行成功: 申请单号=" + vo.getApplicationNo());
            ctx.transit(AfterSaleState.COMPLETED);
            traceRecorder.updateRunStatus(ctx, "COMPLETED", null);
        } else {
            // 重试后仍失败：转人工兜底
            String msg = result == null ? "未知错误" : result.message();
            doHandoff(ctx, "EXECUTION_FAILED", "NORMAL", ctx.getOrder().getOrderNo(),
                    "售后申请执行失败：" + msg);
        }
    }

    // ==================== 工具执行观测 ====================

    /**
     * 执行工具并记录 TOOL 环节 span（工具名、参数摘要、结果状态、耗时）。
     * 与 AgentTraceRecorder 的审计轨迹互补：trace span 服务线上可观测，轨迹表服务审计回放。
     *
     * <p>学习点：为什么工具执行要"双写"？
     * <ul>
     *   <li><b>AgentTraceRecorder</b>（agent_step 表）：面向审计——业务字段齐全、
     *       摘要化落库、可回放完整执行过程，是"合规留痕"；</li>
     *   <li><b>TOOL span</b>（llm_trace.spans_json）：面向可观测——与 LLM/intent 等
     *       其他环节在同一调用链上，回答"这一次请求各环节花了多久、谁慢了"。</li>
     * </ul>
     * 两者数据同源（工具调用），但消费场景不同，故分开存储。</p>
     */
    private <T> T runTool(String toolName, String inputDigest, Supplier<T> action) {
        return TraceSpans.observeReturn(observationRegistry, "TOOL", "agent.tool." + toolName,
                Map.of("tool", toolName),
                Map.of("detail", inputDigest == null ? "" : inputDigest),
                action);
    }

    // ==================== 转人工 ====================

    /**
     * 转人工：生成工单（携带订单、情绪、问题摘要、已执行步骤）+ 通知坐席
     */
    private AgentTurnResult doHandoff(AfterSaleContext ctx, String reason, String priority,
                                      String orderNo, String summary) {
        long t0 = System.currentTimeMillis();
        // 创建转人工工单（携带订单、情绪、摘要与已执行步骤）
        ToolResult result = runTool(AgentStateMachine.TOOL_HANDOFF,
                traceRecorder.digest(reason),
                () -> handoffTool.createHandoff(ctx.getRunId(), ctx.getSessionId(),
                        reason, priority, orderNo,
                        ctx.getIntentResult() == null ? null : ctx.getIntentResult().sentiment().name(),
                        summary, traceRecorder.executedStepsJson(ctx.getStepSummaries())));
        traceRecorder.step(ctx, "HANDOFF", AgentStateMachine.TOOL_HANDOFF,
                traceRecorder.digest(reason), result.message(), System.currentTimeMillis() - t0,
                result.isSuccess() ? "SUCCESS" : "FAILED", null);
        // 工单号（创建失败为空，不阻断转人工流程）
        String ticketNo = result.isSuccess() ? String.valueOf(result.data()) : null;
        ctx.setHandoff(new HandoffInfo(ticketNo, reason, priority, summary));
        ctx.getStepSummaries().add("转人工: " + reason + (ticketNo == null ? "" : ", 工单=" + ticketNo));
        // 无论迁移表是否允许，最终置为 HANDOFF 终态
        if (stateMachine.canTransit(ctx.getState(), AfterSaleState.HANDOFF)) {
            ctx.transit(AfterSaleState.HANDOFF);
        } else {
            ctx.setState(AfterSaleState.HANDOFF);
        }
        traceRecorder.updateRunStatus(ctx, "HANDOFF", null);
        runStore.save(ctx);
        return buildResult(ctx);
    }

    // ==================== 商品推荐 ====================

    /** 按订单商品单价召回同价位商品 */
    private void recommendForOrder(AfterSaleContext ctx) {
        // 基准价：优先首个商品单价，其次订单实付金额
        OrderVO order = ctx.getOrder();
        BigDecimal base = null;
        if (order.getItems() != null && !order.getItems().isEmpty()) {
            base = order.getItems().get(0).getProductPrice();
        }
        if (base == null) {
            base = order.getPayAmount();
        }
        doRecommend(ctx, base, param(ctx, "keywords"));
    }

    /** 按用户预算召回（无订单场景） */
    private void recommendByBudget(AfterSaleContext ctx) {
        String budget = param(ctx, "budget");
        // 无预算参数：跳过推荐
        if (!StringUtils.hasText(budget)) {
            return;
        }
        try {
            doRecommend(ctx, new BigDecimal(budget.trim()), param(ctx, "keywords"));
        } catch (NumberFormatException e) {
            log.warn("预算参数解析失败: {}", budget);
        }
    }

    private void doRecommend(AfterSaleContext ctx, BigDecimal base, String keywords) {
        long t0 = System.currentTimeMillis();
        // 调用商品服务做同价位召回
        ToolResult result = runTool(AgentStateMachine.TOOL_PRODUCT_RECOMMEND,
                traceRecorder.digest(base.toPlainString() + (keywords == null ? "" : keywords)),
                () -> productRecommendTool.recommend(base, keywords, null));
        traceRecorder.step(ctx, "RECOMMEND", AgentStateMachine.TOOL_PRODUCT_RECOMMEND,
                traceRecorder.digest(base.toPlainString() + (keywords == null ? "" : keywords)),
                result.message(), System.currentTimeMillis() - t0,
                result.isSuccess() ? "SUCCESS" : "FAILED", null);
        ctx.getStepSummaries().add("商品推荐: " + result.message());
        // 成功：记录推荐列表供回复展示
        if (result.isSuccess()) {
            ctx.setRecommendations((List<ProductRecommendVO>) result.data());
        }
    }

    // ==================== 结果构建 ====================

    private AgentTurnResult buildResult(AfterSaleContext ctx) {
        String state = ctx.getState().name();
        List<String> candidates = ctx.getCandidates().stream()
                .map(OrderVO::getOrderNo).toList();
        // 仅确认态回传凭证与操作计划（供前端展示确认卡片）
        boolean waitingConfirm = ctx.getState() == AfterSaleState.CONFIRM_ACTION;
        return AgentTurnResult.of(ctx.getRunId(), state, ctx.getIntents(),
                buildReply(ctx), ctx.isNeedsUserInput(), false,
                waitingConfirm ? ctx.getConfirmationToken() : null,
                waitingConfirm ? ctx.getActionPlan() : null,
                candidates, ctx.getHandoff(), ctx.getApplicationNo(),
                ctx.getErrorSummary() == null ? null : errorCodeOf(ctx));
    }

    private String buildReply(AfterSaleContext ctx) {
        switch (ctx.getState()) {
            case FAILED -> {
                return ctx.getErrorSummary() == null
                        ? "很抱歉，本次处理未能完成，请稍后重试或联系人工客服。"
                        : "很抱歉，" + ctx.getErrorSummary() + "。您可以重新发起或联系人工客服。";
            }
            case CANCELLED -> {
                return "好的，已为您取消本次售后申请，未执行任何操作。如有其他问题欢迎随时咨询。";
            }
            case HANDOFF -> {
                HandoffInfo h = ctx.getHandoff();
                String ticket = h != null && h.ticketNo() != null ? "，工单号 " + h.ticketNo() : "";
                return "已为您转接人工客服" + ticket + "。坐席将携带您的订单与问题上下文尽快接入，请稍候。";
            }
            case COMPLETED -> {
                return buildCompletedReply(ctx);
            }
            case LOCATE_ORDER -> {
                return "您有多个可售后订单，请回复订单号选择其中一个：\n"
                        + String.join("\n", ctx.getCandidates().stream()
                        .map(o -> "• " + o.getOrderNo() + "（" + firstItemName(o) + " ¥" + o.getPayAmount() + "）")
                        .toList());
            }
            case COLLECT_EVIDENCE -> {
                return "好的，为您办理" + ctx.getActionType().getDesc()
                        + "。请简单描述一下商品的问题（例如：无法开机、声音异常、外观破损等），我为您记录。";
            }
            case CONFIRM_ACTION -> {
                return buildConfirmReply(ctx);
            }
            default -> {
                return "正在为您处理，请稍候。";
            }
        }
    }

    private String buildCompletedReply(AfterSaleContext ctx) {
        StringBuilder sb = new StringBuilder();
        if (ctx.getApplicationNo() != null) {
            AgentActionPlan plan = ctx.getActionPlan();
            sb.append("✅ 您的").append(plan.actionType().getDesc()).append("申请已提交成功！\n");
            sb.append("• 申请单号：").append(ctx.getApplicationNo()).append("\n");
            sb.append("• 订单：").append(plan.orderNo())
                    .append("（").append(plan.productName() == null ? "" : plan.productName()).append("）\n");
            sb.append("• 原因：").append(plan.reason()).append("\n");
            if (plan.evidenceSummary() != null && !plan.evidenceSummary().isBlank()) {
                sb.append("• 依据：").append(plan.evidenceSummary()).append("\n");
            }
            sb.append("售后进度将同步推送，请留意通知。");
        } else if (ctx.getOrder() == null) {
            boolean wantRecommend = ctx.getIntentResult() != null
                    && ctx.getIntentResult().has(AgentIntentType.PRODUCT_RECOMMEND);
            if (wantRecommend && ctx.getRecommendations().isEmpty()) {
                sb.append("请提供预算金额（例如「300 元以内」），我可以为您推荐同价位商品。");
            } else {
                sb.append("您目前没有可售后的订单。");
            }
        } else {
            sb.append("本次未产生售后申请。");
        }
        appendRecommendation(sb, ctx);
        return sb.toString();
    }

    private String buildConfirmReply(AfterSaleContext ctx) {
        AgentActionPlan plan = ctx.getActionPlan();
        StringBuilder sb = new StringBuilder();
        sb.append("⚠️ 请确认以下操作：\n");
        sb.append("• 动作：").append(plan.actionType().getDesc()).append("\n");
        sb.append("• 订单：").append(plan.orderNo())
                .append("（").append(plan.productName() == null ? "" : plan.productName()).append("）\n");
        sb.append("• 数量：").append(plan.quantity()).append("\n");
        sb.append("• 原因：").append(plan.reason()).append("\n");
        if (plan.evidenceSummary() != null && !plan.evidenceSummary().isBlank()) {
            sb.append("• 依据：").append(plan.evidenceSummary()).append("\n");
        }
        sb.append("回复「确认」执行，回复「拒绝」取消。");
        return sb.toString();
    }

    private void appendRecommendation(StringBuilder sb, AfterSaleContext ctx) {
        List<ProductRecommendVO> recs = ctx.getRecommendations();
        if (recs == null || recs.isEmpty()) {
            return;
        }
        sb.append("\n\n🎧 同价位商品推荐：\n");
        int i = 1;
        for (ProductRecommendVO vo : recs) {
            sb.append(i++).append(". ").append(vo.getName()).append("（¥").append(vo.getPrice()).append("）\n");
            if (vo.getMatchReason() != null && !vo.getMatchReason().isBlank()) {
                sb.append("   ").append(vo.getMatchReason()).append("\n");
            }
        }
    }

    // ==================== 辅助 ====================

    /** 失败中止（记录轨迹后返回结果） */
    private AgentTurnResult failWith(AfterSaleContext ctx, String code, String reason) {
        ctx.setState(AfterSaleState.FAILED);
        ctx.setErrorSummary(reason);
        traceRecorder.updateRunStatus(ctx, "FAILED", reason);
        runStore.save(ctx);
        return buildResult(ctx);
    }

    private boolean isTotalTimeout(AfterSaleContext ctx) {
        // 总超时判定：创建时间 + 配置总超时毫秒
        return ctx.getCreatedAt() != null
                && LocalDateTime.now().isAfter(ctx.getCreatedAt()
                .plusNanos(properties.getTotalTimeoutMs() * 1_000_000L));
    }

    private String errorCodeOf(AfterSaleContext ctx) {
        String error = ctx.getErrorSummary() == null ? "" : ctx.getErrorSummary();
        // 错误码归类：按失败摘要关键词映射
        if (error.contains("超时")) {
            return "AGENT_TIMEOUT";
        }
        if (error.contains("最大步骤")) {
            return "AGENT_MAX_STEPS_EXCEEDED";
        }
        return "AGENT_EXECUTION_FAILED";
    }

    /** 从意图参数解析售后动作（默认换货） */
    private AfterSaleActionType resolveAction(AfterSaleContext ctx) {
        String action = param(ctx, "action");
        AfterSaleActionType type = AfterSaleActionType.fromCode(action);
        return type == null ? AfterSaleActionType.EXCHANGE : type;
    }

    private String param(AfterSaleContext ctx, String key) {
        if (ctx.getIntentResult() == null) {
            return null;
        }
        return ctx.getIntentResult().intents().stream()
                .map(AgentIntent::params)
                .filter(p -> p != null && p.get(key) != null)
                .map(p -> p.get(key))
                .findFirst().orElse(null);
    }

    private static String firstNonBlank(String... values) {
        for (String v : values) {
            if (StringUtils.hasText(v)) {
                return v.trim();
            }
        }
        return null;
    }

    private static OrderVO.OrderItemVO firstItem(OrderVO order) {
        if (order.getItems() == null || order.getItems().isEmpty()) {
            return null;
        }
        return order.getItems().get(0);
    }

    private static String firstItemName(OrderVO order) {
        OrderVO.OrderItemVO item = firstItem(order);
        return item == null ? "" : item.getProductName();
    }

    private static String summarizeIntent(IntentResult intent) {
        StringBuilder sb = new StringBuilder();
        intent.intents().forEach(i -> sb.append(i.type().name()).append('(')
                .append(String.format("%.2f", i.confidence())).append(") "));
        sb.append("情绪=").append(intent.sentiment());
        return sb.toString().trim();
    }

    private static String summarizePolicy(PolicyCheckResult policy) {
        if (policy == null) {
            return "未校验";
        }
        return (policy.eligible() ? "满足" : "不满足") + " 规则=" + policy.ruleId()
                + " 原因=" + policy.reason();
    }

    /** 确认/拒绝意图解析 */
    private enum Decision {
        CONFIRM, REJECT, UNKNOWN
    }

    private static Decision parseDecision(String input) {
        String text = input == null ? "" : input.trim();
        if (text.isEmpty()) {
            return Decision.UNKNOWN;
        }
        if (containsAny(text, List.of("确认", "同意", "可以", "好的", "好，", "执行", "是的"))) {
            return Decision.CONFIRM;
        }
        if (containsAny(text, List.of("拒绝", "取消", "不要", "算了", "不用了", "不需要"))) {
            return Decision.REJECT;
        }
        return Decision.UNKNOWN;
    }

    private static boolean containsAny(String text, List<String> keywords) {
        return keywords.stream().anyMatch(text::contains);
    }
}
