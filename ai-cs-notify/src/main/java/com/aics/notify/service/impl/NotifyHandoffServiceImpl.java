package com.aics.notify.service.impl;

import com.aics.common.exception.BusinessException;
import com.aics.common.result.ResultCode;
import com.aics.notify.dto.HandoffNoticeDTO;
import com.aics.notify.service.NotifyHandoffService;
import com.aics.notify.websocket.NotifyWebSocketHandler;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

/**
 * 转人工通知服务实现 —— 将 {@link HandoffNoticeDTO} 序列化为 JSON（含 event=HANDOFF），
 * 并通过 {@link NotifyWebSocketHandler} 定向推送给目标用户。
 * 用户不在线时推送静默忽略（不抛异常）。
 */
@Slf4j
@Service
public class NotifyHandoffServiceImpl implements NotifyHandoffService {

    private final ObjectMapper objectMapper;

    // 注入 Jackson ObjectMapper，用于 DTO → JSON 的序列化
    public NotifyHandoffServiceImpl(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    @Override
    public void sendHandoffNotice(HandoffNoticeDTO dto) {
        try {
            // 1. 将 DTO 转为 JSON 树，并注入事件类型 event=HANDOFF（前端据此路由到转人工处理）
            ObjectNode payload = objectMapper.valueToTree(dto);
            payload.put("event", "HANDOFF");
            // 2. 序列化为 JSON 字符串（event 字段一并携带）
            String json = objectMapper.writeValueAsString(payload);
            log.info("发送转人工通知: userId={}, ticketNo={}, priority={}, summary={}",
                    dto.getUserId(), dto.getTicketNo(), dto.getPriority(), dto.getSummary());
            // 3. 经静态方法 WebSocket 定向推送：用户不在线时 handler 内部仅告警不抛异常（静默忽略）
            NotifyWebSocketHandler.sendMessageToUser(String.valueOf(dto.getUserId()), json);
        } catch (Exception e) {
            // 4. 兜底：序列化/推送任何异常统一记录日志并抛 NOTIFY_SEND_FAIL，由上层决定是否重试
            log.error("转人工通知发送失败: ticketNo={}", dto.getTicketNo(), e);
            throw new BusinessException(ResultCode.NOTIFY_SEND_FAIL);
        }
    }
}
