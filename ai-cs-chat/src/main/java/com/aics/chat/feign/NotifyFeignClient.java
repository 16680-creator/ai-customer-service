package com.aics.chat.feign;

import com.aics.chat.dto.HandoffNoticeDTO;
import com.aics.common.result.Result;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

/**
 * 通知服务 Feign 客户端（调用 ai-cs-notify 的转人工通知）
 */
@FeignClient(name = "ai-cs-notify")
public interface NotifyFeignClient {

    /**
     * 转人工通知（向坐席端推送转人工事件）
     */
    @PostMapping("/api/notify/handoff")
    Result<Void> handoffNotice(@RequestBody HandoffNoticeDTO dto);
}
