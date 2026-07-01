package com.aics.notify.service;

/**
 * 通知服务接口
 */
public interface NotifyService {

    /**
     * 发送消息给指定用户
     *
     * @param userId  用户ID
     * @param message 消息内容
     */
    void sendToUser(String userId, String message);

    /**
     * 广播消息
     *
     * @param message 消息内容
     */
    void broadcast(String message);

    /**
     * 获取在线用户数
     *
     * @return 在线用户数
     */
    int getOnlineCount();
}
