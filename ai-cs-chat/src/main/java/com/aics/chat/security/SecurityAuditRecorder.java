package com.aics.chat.security;

import com.aics.chat.dto.SecurityEventDTO;
import com.aics.chat.feign.SecurityEventFeignClient;
import com.aics.chat.util.PiiMasker;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;
import java.util.UUID;

/**
 * 安全审计记录器（3.2 F7 审计留痕）。
 *
 * <p>所有 Guardrail 命中/拦截/越权事件统一经此记录：</p>
 * <ul>
 *   <li><b>内存缓存</b>：最近 200 条事件（{@link #recentEvents()}），供测试断言与本地排查；</li>
 *   <li><b>落库</b>：经 {@link SecurityEventFeignClient} 异步持久化到 ai-cs-message 的
 *       security_event 表（同 eventId 幂等）；落库失败只告警不阻断主链路（审计尽力而为）；</li>
 *   <li><b>脱敏</b>：原始输入经 {@link PiiMasker} 脱敏 + 截断后才写入事件，禁止明文敏感信息。</li>
 * </ul>
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class SecurityAuditRecorder {

    /** 内存缓存上限 */
    private static final int MAX_RECENT = 200;

    /** 输入摘要最大长度 */
    private static final int MAX_DIGEST = 512;

    /** 详情最大长度 */
    private static final int MAX_DETAIL = 1024;

    private final SecurityEventFeignClient securityEventFeignClient;
    private final SecurityProperties properties;
    private final PiiMasker piiMasker;

    /** 最近事件缓存（并发安全） */
    private final Deque<SecurityEventDTO> recent = new ArrayDeque<>();

    /**
     * 记录一条安全事件：脱敏摘要后入内存缓存，并按配置落库。
     *
     * @param type      事件类型
     * @param stage     发生环节（INPUT/OUTPUT/TOOL/RETRIEVAL/GATEWAY/DEGRADE）
     * @param userId    用户ID（可为空）
     * @param rule      命中规则/工具名/知识库标识
     * @param rawInput  原始输入（会被脱敏，可为空）
     * @param action    处理动作（BLOCK/ALLOW/FILTER 等）
     * @param detail    详情/原因
     */
    public void record(SecurityEventType type, String stage, Long userId, String rule,
                       String rawInput, String action, String detail) {
        // 学习点：审计的“尽力而为”哲学——审计落库是合规要求，但绝不能成为主链路故障点：
        // Feign 失败只告警不重试不阻断（与 3.3 llm_trace 同款策略）；
        // 同时保留内存事件缓存（最近 200 条），测试断言与本地排查零依赖即可用。
        SecurityEventDTO dto = new SecurityEventDTO();
        dto.setEventId(UUID.randomUUID().toString());
        dto.setType(type.name());
        dto.setStage(stage);
        dto.setUserId(userId);
        dto.setRule(truncate(rule, 128));
        // 关键点：原始输入脱敏 + 截断后才允许进入审计（防 PII 泄漏到审计存储）
        dto.setInputDigest(piiMasker.mask(truncate(rawInput, MAX_DIGEST)));
        dto.setAction(action);
        dto.setDetail(truncate(detail, MAX_DETAIL));

        synchronized (recent) {
            recent.addLast(dto);
            while (recent.size() > MAX_RECENT) {
                recent.removeFirst();
            }
        }

        if (properties.isAuditEnabled()) {
            try {
                securityEventFeignClient.record(dto);
            } catch (Exception e) {
                // 审计落库失败仅告警：审计尽力而为，不阻断业务
                log.warn("安全事件落库失败: type={}, rule={}, err={}", type, rule, e.getMessage());
            }
        } else {
            log.info("安全事件(审计关闭，仅缓存): type={}, rule={}, action={}", type, rule, action);
        }
    }

    /**
     * 最近事件快照（测试断言/本地排查用）。
     */
    public List<SecurityEventDTO> recentEvents() {
        synchronized (recent) {
            return new ArrayList<>(recent);
        }
    }

    /**
     * 清空缓存（测试用）。
     */
    public void clear() {
        synchronized (recent) {
            recent.clear();
        }
    }

    private static String truncate(String text, int max) {
        if (text == null) {
            return null;
        }
        return text.length() <= max ? text : text.substring(0, max);
    }
}
