package com.aics.chat.feign;

import com.aics.chat.dto.SecurityEventDTO;
import com.aics.common.result.Result;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

/**
 * 消息服务安全事件 Feign 客户端（3.2 F7 审计留痕：调用 ai-cs-message 持久化 security_event）。
 */
@FeignClient(name = "ai-cs-message", contextId = "securityEvent")
public interface SecurityEventFeignClient {

    /**
     * 记录一条安全事件（同 eventId 幂等）
     */
    @PostMapping("/api/security/events")
    Result<Void> record(@RequestBody SecurityEventDTO dto);
}
