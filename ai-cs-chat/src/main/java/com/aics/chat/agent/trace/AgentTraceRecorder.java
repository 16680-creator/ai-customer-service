package com.aics.chat.agent.trace;

import com.aics.chat.agent.context.AfterSaleContext;
import com.aics.chat.dto.AgentConfirmationDTO;
import com.aics.chat.dto.AgentRunDTO;
import com.aics.chat.dto.AgentRunStatusDTO;
import com.aics.chat.dto.AgentStepDTO;
import com.aics.chat.feign.AgentTraceFeignClient;
import com.aics.chat.util.PiiMasker;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.List;

/**
 * Agent 轨迹记录器：把 run/step/confirmation 摘要化后经 Feign 持久化到 ai-cs-message。
 *
 * <p>审计要求：敏感参数不进普通日志，一律以 SHA-256 截断摘要落库；
 * 3.2 F3 起，步骤输入/输出摘要落库前再经 {@link PiiMasker} 脱敏（双保险，
 * 防止工具结果/参数中出现手机号、银行卡等明文敏感信息进入审计存储）；
 * 轨迹持久化失败只告警不阻断业务流程（审计尽力而为，业务强一致）。</p>
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class AgentTraceRecorder {

    private final AgentTraceFeignClient agentTraceFeignClient;
    private final ObjectMapper objectMapper;
    private final PiiMasker piiMasker;

    /**
     * 创建执行记录（幂等）
     */
    public void createRun(AfterSaleContext ctx) {
        try {
            AgentRunDTO dto = new AgentRunDTO();
            dto.setRunId(ctx.getRunId());
            dto.setSessionId(ctx.getSessionId());
            dto.setUserId(ctx.getUserId());
            dto.setStatus("RUNNING");
            dto.setCurrentStep(0);
            // 记录提示词/规则版本，审计可追溯
            dto.setPromptVersion("after-sale-v1");
            agentTraceFeignClient.createRun(dto);
        } catch (Exception e) {
            // 轨迹写入失败仅告警：审计尽力而为，不阻断业务
            log.warn("Agent run 轨迹写入失败: runId={}, err={}", ctx.getRunId(), e.getMessage());
        }
    }

    /**
     * 更新执行状态
     */
    public void updateRunStatus(AfterSaleContext ctx, String status, String errorSummary) {
        try {
            AgentRunStatusDTO dto = new AgentRunStatusDTO();
            dto.setStatus(status);
            dto.setCurrentStep(ctx.getSteps());
            dto.setErrorSummary(errorSummary);
            agentTraceFeignClient.updateRunStatus(ctx.getRunId(), dto);
        } catch (Exception e) {
            log.warn("Agent run 状态更新失败: runId={}, err={}", ctx.getRunId(), e.getMessage());
        }
    }

    /**
     * 追加步骤轨迹（摘要化，幂等）
     */
    public void step(AfterSaleContext ctx, String stepType, String toolName,
                     String inputDigest, String outputDigest, long durationMs,
                     String status, String errorSummary) {
        try {
            AgentStepDTO dto = new AgentStepDTO();
            dto.setRunId(ctx.getRunId());
            dto.setStepNo(ctx.getSteps() + 1);
            dto.setStepType(stepType);
            dto.setToolName(toolName);
            // 输入/输出摘要化后落库：敏感参数不落明文；
            // 3.2 F3 增加 PII 脱敏，防止工具参数/结果携带手机号、银行卡等明文
            dto.setInputDigest(piiMasker.mask(truncate(inputDigest, 256)));
            dto.setOutputDigest(piiMasker.mask(truncate(outputDigest, 1024)));
            dto.setDurationMs(durationMs);
            dto.setStatus(status);
            dto.setErrorSummary(errorSummary);
            agentTraceFeignClient.appendStep(ctx.getRunId(), dto);
        } catch (Exception e) {
            log.warn("Agent step 轨迹写入失败: runId={}, err={}", ctx.getRunId(), e.getMessage());
        }
    }

    /**
     * 记录写操作确认
     */
    public void confirmation(AfterSaleContext ctx, String status, Long confirmedBy) {
        try {
            AgentConfirmationDTO dto = new AgentConfirmationDTO();
            dto.setRunId(ctx.getRunId());
            dto.setAction("CREATE_" + ctx.getActionType().name());
            dto.setPayloadDigest(ctx.getPayloadDigest());
            dto.setStatus(status);
            dto.setConfirmedBy(confirmedBy);
            dto.setTimeoutAt(ctx.getConfirmationExpiresAt());
            agentTraceFeignClient.recordConfirmation(ctx.getRunId(), dto);
        } catch (Exception e) {
            log.warn("Agent 确认记录写入失败: runId={}, err={}", ctx.getRunId(), e.getMessage());
        }
    }

    /**
     * SHA-256 摘要（截断）
     */
    public String digest(String text) {
        if (text == null) {
            return null;
        }
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            byte[] bytes = md.digest(text.getBytes(StandardCharsets.UTF_8));
            StringBuilder sb = new StringBuilder();
            for (byte b : bytes) {
                sb.append(String.format("%02x", b));
            }
            // SHA-256 十六进制摘要，截断为 64 位
            return truncate(sb.toString(), 64);
        } catch (Exception e) {
            // 摘要计算异常：退化为原文截断
            return truncate(text, 64);
        }
    }

    /**
     * 已执行步骤清单 JSON（转人工移交用）
     */
    public String executedStepsJson(List<String> stepSummaries) {
        try {
            // 已执行步骤序列化：转人工移交时携带完整上下文
            return objectMapper.writeValueAsString(stepSummaries == null ? List.of() : stepSummaries);
        } catch (Exception e) {
            return "[]";
        }
    }

    private static String truncate(String text, int max) {
        if (text == null) {
            return null;
        }
        // 超长截断：防止大字段撑爆轨迹存储
        return text.length() <= max ? text : text.substring(0, max);
    }
}
