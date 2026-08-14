package com.aics.message.service.impl;

import com.aics.message.dto.SecurityEventDTO;
import com.aics.message.entity.SecurityEvent;
import com.aics.message.mapper.SecurityEventMapper;
import com.aics.message.service.SecurityEventService;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

/**
 * 安全事件服务实现（3.2 F7 审计留痕）。
 * <p>
 * 设计要点：
 * <ul>
 *     <li>按 eventId 幂等：重复上报（Feign 重试/重放）直接跳过，避免审计数据重复；</li>
 *     <li>inputDigest 由 chat 侧脱敏后上报，本服务不再接触明文敏感信息；</li>
 *     <li>审计写入失败由调用方（chat 侧）告警兜底，本服务仅尽力持久化。</li>
 * </ul>
 * </p>
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class SecurityEventServiceImpl implements SecurityEventService {

    private final SecurityEventMapper securityEventMapper;

    @Override
    public void record(SecurityEventDTO dto) {
        if (dto == null || dto.getEventId() == null || dto.getEventId().isBlank()) {
            log.warn("安全事件缺少 eventId，跳过落库");
            return;
        }
        // 1. 幂等检查：按 eventId 查询，已存在则跳过（Feign 重试/重复上报不产生重复审计）
        // 学习点：审计接口必须幂等——Feign 客户端默认有超时重试，若上报接口不幂等，
        // 一次拦截事件可能落库多条，审计计数与告警全被污染；
        // 用调用方生成的 UUID（eventId）做幂等键，比“查重放”语义更简单可靠。
        LambdaQueryWrapper<SecurityEvent> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(SecurityEvent::getEventId, dto.getEventId());
        if (securityEventMapper.selectCount(wrapper) > 0) {
            log.info("安全事件已存在，幂等跳过: eventId={}", dto.getEventId());
            return;
        }
        // 2. 组装实体落库（createTime 由 MetaObjectHandler 自动填充）
        SecurityEvent event = new SecurityEvent();
        event.setEventId(dto.getEventId());
        event.setType(dto.getType());
        event.setStage(dto.getStage());
        event.setUserId(dto.getUserId());
        event.setSessionId(dto.getSessionId());
        event.setRunId(dto.getRunId());
        event.setRule(dto.getRule());
        event.setInputDigest(dto.getInputDigest());
        event.setAction(dto.getAction());
        event.setDetail(dto.getDetail());
        securityEventMapper.insert(event);
        log.info("安全事件已落库: eventId={}, type={}, rule={}, action={}",
                dto.getEventId(), dto.getType(), dto.getRule(), dto.getAction());
    }
}
