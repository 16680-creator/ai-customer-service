package com.aics.notify.service;

import com.aics.notify.dto.HandoffNoticeDTO;

/**
 * 转人工通知服务接口
 */
// 转人工事件通知服务：由 chat 模块经 NotifyFeignClient（POST /api/notify/handoff）触发
public interface NotifyHandoffService {

    /**
     * 发送转人工通知给指定用户
     *
     * @param dto 转人工通知信息
     */
    // 序列化为 JSON（含 event=HANDOFF）后经 WebSocket 定向推送；用户不在线时静默忽略
    void sendHandoffNotice(HandoffNoticeDTO dto);
}
