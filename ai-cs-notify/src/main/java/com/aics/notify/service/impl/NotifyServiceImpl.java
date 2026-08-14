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
    /** 定向推送：按 userId 找到 WebSocket 会话并发送；离线则忽略（可扩展为离线缓存） */
    public void sendToUser(String userId, String message) {
        log.info("发送通知给用户: userId={}, message={}", userId, message);
        NotifyWebSocketHandler.sendMessageToUser(userId, message);
    }

    @Override
    /** 广播：推送给所有在线用户 */
    public void broadcast(String message) {
        log.info("广播通知: message={}", message);
        NotifyWebSocketHandler.broadcastMessage(message);
    }

    @Override
    /** 当前在线连接数（监控/大屏用） */
    public int getOnlineCount() {
        int count = NotifyWebSocketHandler.getOnlineCount();
        log.info("查询在线用户数: count={}", count);
        return count;
    }
}
