package com.aics.notify.service.impl;

import com.aics.notify.service.NotifyService;
import com.aics.notify.websocket.NotifyWebSocketHandler;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

/**
 * 通知服务实现
 */
@Slf4j
@Service
public class NotifyServiceImpl implements NotifyService {

    @Override
    public void sendToUser(String userId, String message) {
        log.info("发送通知给用户: userId={}, message={}", userId, message);
        NotifyWebSocketHandler.sendMessageToUser(userId, message);
    }

    @Override
    public void broadcast(String message) {
        log.info("广播通知: message={}", message);
        NotifyWebSocketHandler.broadcastMessage(message);
    }

    @Override
    public int getOnlineCount() {
        int count = NotifyWebSocketHandler.getOnlineCount();
        log.info("查询在线用户数: count={}", count);
        return count;
    }
}
