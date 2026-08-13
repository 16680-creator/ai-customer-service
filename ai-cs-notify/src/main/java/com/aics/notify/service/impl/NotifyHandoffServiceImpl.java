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

    public NotifyHandoffServiceImpl(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    @Override
    public void sendHandoffNotice(HandoffNoticeDTO dto) {
        try {
            ObjectNode payload = objectMapper.valueToTree(dto);
            payload.put("event", "HANDOFF");
            String json = objectMapper.writeValueAsString(payload);
            log.info("发送转人工通知: userId={}, ticketNo={}, priority={}, summary={}",
                    dto.getUserId(), dto.getTicketNo(), dto.getPriority(), dto.getSummary());
            NotifyWebSocketHandler.sendMessageToUser(String.valueOf(dto.getUserId()), json);
        } catch (Exception e) {
            log.error("转人工通知发送失败: ticketNo={}", dto.getTicketNo(), e);
            throw new BusinessException(ResultCode.NOTIFY_SEND_FAIL);
        }
    }
}
