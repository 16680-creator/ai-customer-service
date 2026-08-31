package com.aics.notify.service.impl;

import com.aics.notify.service.NotifyService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;

/**
 * STOMP 通知服务：用户目的地由 Principal 绑定，不再手工维护 userId -> Session 映射。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class NotifyServiceImpl implements NotifyService {

    public static final String USER_NOTIFY_DESTINATION = "/queue/notify";

    private final SimpMessagingTemplate messagingTemplate;

    @Override
    public void sendToUser(String userId, String message) {
        log.info("STOMP 定向通知: userId={}", userId);
        messagingTemplate.convertAndSendToUser(userId, USER_NOTIFY_DESTINATION, message);
    }

    @Override
    public void broadcast(String message) {
        log.info("STOMP 广播通知");
        messagingTemplate.convertAndSend("/topic/notify", message);
    }

    @Override
    public int getOnlineCount() {
        // STOMP simple broker 不公开跨 session 在线计数；监控以 broker/session 指标替代。
        return 0;
    }
}
