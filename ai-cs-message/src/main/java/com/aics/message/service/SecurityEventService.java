package com.aics.message.service;

import com.aics.message.dto.SecurityEventDTO;

/**
 * 安全事件服务接口（3.2 F7 审计留痕）。
 * <p>
 * 所属模块：ai-cs-message。
 * 职责：定义安全事件（security_event 表）的持久化能力，供 chat 模块 Guardrail 链路
 * 经 Feign 上报。幂等约定：按 {@code eventId} 幂等，重复上报跳过。
 * 实现类：{@link com.aics.message.service.impl.SecurityEventServiceImpl}。
 * 调用方：{@link com.aics.message.controller.SecurityEventController}。
 * </p>
 */
public interface SecurityEventService {

    /**
     * 记录安全事件（按 eventId 幂等，重复上报跳过）
     *
     * @param dto 安全事件信息
     */
    void record(SecurityEventDTO dto);
}
