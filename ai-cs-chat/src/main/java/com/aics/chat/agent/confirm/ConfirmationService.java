package com.aics.chat.agent.confirm;

import com.aics.chat.agent.AgentProperties;
import com.aics.chat.agent.context.AfterSaleContext;
import com.aics.chat.agent.model.AgentActionPlan;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.LocalDateTime;
import java.util.UUID;

/**
 * 写操作确认服务：确认凭证（Token）生成与校验。
 *
 * <p>凭证与「操作摘要摘要值（SHA-256）」绑定：确认时校验 Token 有效、未超时、
 * 摘要与当前操作一致，防止确认内容被篡改。</p>
 */
@Service
@RequiredArgsConstructor
public class ConfirmationService {

    private final AgentProperties properties;
    private final ObjectMapper objectMapper;

    /**
     * 为当前操作签发确认凭证（写入 context）
     *
     * @return 凭证 Token
     */
    public String issue(AfterSaleContext context, AgentActionPlan plan) {
        // 随机凭证：不可预测，防止伪造
        String token = UUID.randomUUID().toString();
        context.setConfirmationToken(token);
        // 绑定操作摘要摘要值：确认时比对，防确认内容被篡改
        context.setPayloadDigest(digest(plan));
        // 确认超时 = 签发时间 + 配置确认时长（分钟）
        context.setConfirmationExpiresAt(LocalDateTime.now()
                .plusMinutes(properties.getConfirmTimeoutMinutes()));
        return token;
    }

    /**
     * 校验确认凭证：存在、未超时、摘要与当前操作一致
     */
    public boolean validate(AfterSaleContext context, AgentActionPlan plan) {
        // 无凭证或无操作计划：直接校验失败
        if (context.getConfirmationToken() == null || plan == null) {
            return false;
        }
        // 已超时：校验失败（需重新发起确认）
        if (context.getConfirmationExpiresAt() == null
                || LocalDateTime.now().isAfter(context.getConfirmationExpiresAt())) {
            return false;
        }
        // 摘要一致才算有效确认：当前操作与签发时完全一致
        return digest(plan).equals(context.getPayloadDigest());
    }

    /**
     * 是否已超时
     */
    public boolean isExpired(AfterSaleContext context) {
        return context.getConfirmationExpiresAt() != null
                && LocalDateTime.now().isAfter(context.getConfirmationExpiresAt());
    }

    /**
     * 操作摘要摘要值（SHA-256）
     */
    String digest(AgentActionPlan plan) {
        try {
            // 操作计划序列化后做 SHA-256，输出十六进制摘要
            String json = objectMapper.writeValueAsString(plan);
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            byte[] bytes = md.digest(json.getBytes(StandardCharsets.UTF_8));
            StringBuilder sb = new StringBuilder();
            for (byte b : bytes) {
                sb.append(String.format("%02x", b));
            }
            return sb.toString();
        } catch (Exception e) {
            throw new IllegalStateException("操作摘要计算失败", e);
        }
    }
}
