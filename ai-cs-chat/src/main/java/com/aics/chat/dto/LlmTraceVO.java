package com.aics.chat.dto;

import lombok.Data;

import java.time.LocalDateTime;

/**
 * LLM 调用链追踪查询 VO（chat 侧，与 ai-cs-message 的 LlmTraceVO 对齐，用于 Feign 回读）
 *
 * <h3>【AI 技术详解】VO 与 DTO 分离的意义</h3>
 * <p>VO 是"读"契约：除与 DTO 对称的业务字段外，多出 {@code createTime} 等由服务端生成、
 * 客户端无法提供的字段。若用同一个类承载入参与出参，序列化时这些只读字段既无法被写入方
 * 填充，又会被读取方误以为是可提交字段，容易造成契约混乱。读写分离后：</p>
 * <ul>
 *   <li>chat 侧组装上报时只看 DTO（知道自己必须提供什么）；</li>
 *   <li>chat 侧展示查询结果时只看 VO（知道服务端还回传了什么）。</li>
 * </ul>
 */
@Data
public class LlmTraceVO {

    /** 请求 ID */
    private String requestId;

    /** 用户 ID */
    private Long userId;

    /** 会话 ID */
    private String sessionId;

    /** 场景 */
    private String scenario;

    /** 状态：SUCCESS/FAILED */
    private String status;

    /** 总耗时（毫秒） */
    private Long totalDurationMs;

    /** 调用链 span 列表 JSON */
    // 与上报 DTO 同构：message 侧原样存储、原样回读，chat 侧反序列化后用于前端链路展示
    private String spansJson;

    /** 失败摘要 */
    private String errorSummary;

    /** 创建时间 */
    // 服务端生成字段（VO 独有）：trace 落库时间由 message 侧写入，回读用于时间线排序与审计
    private LocalDateTime createTime;
}
