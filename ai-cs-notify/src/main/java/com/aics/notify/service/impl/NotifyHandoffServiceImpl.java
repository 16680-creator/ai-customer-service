package com.aics.notify.service.impl;

import com.aics.common.exception.BusinessException;
import com.aics.common.result.ResultCode;
import com.aics.notify.dto.HandoffNoticeDTO;
import com.aics.notify.service.NotifyHandoffService;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;

/**
 * 转人工通知：STOMP user destination 定向推送；用户身份来自 CONNECT JWT Principal。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class NotifyHandoffServiceImpl implements NotifyHandoffService {

    private final ObjectMapper objectMapper;
    private final SimpMessagingTemplate messagingTemplate;

    @Override
    public void sendHandoffNotice(HandoffNoticeDTO dto) {
        try {
            ObjectNode payload = objectMapper.valueToTree(dto);
            payload.put("event", "HANDOFF");
            log.info("STOMP 转人工通知: userId={}, ticketNo={}", dto.getUserId(), dto.getTicketNo());
            messagingTemplate.convertAndSendToUser(String.valueOf(dto.getUserId()),
                    NotifyServiceImpl.USER_NOTIFY_DESTINATION, objectMapper.writeValueAsString(payload));
        } catch (Exception e) {
            log.error("转人工通知发送失败: ticketNo={}", dto.getTicketNo(), e);
            throw new BusinessException(ResultCode.NOTIFY_SEND_FAIL);
        }
    }
}
