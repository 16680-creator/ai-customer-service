package com.aics.notify.service;

import com.aics.notify.dto.HandoffNoticeDTO;

/**
 * 转人工通知服务接口
 */
public interface NotifyHandoffService {

    /**
     * 发送转人工通知给指定用户
     *
     * @param dto 转人工通知信息
     */
    void sendHandoffNotice(HandoffNoticeDTO dto);
}
