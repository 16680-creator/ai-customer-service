package com.aics.mq.dto;

import lombok.Data;

import java.io.Serializable;

/**
 * 工单消息载荷（RocketMQ body 的 JSON 结构）。
 * <p>
 * 生产者与消费者共用：创建工单时序列化发送，
 * 消费/死信落库/重推时按此结构反序列化。</p>
 */
@Data
public class WorkOrderMessage implements Serializable {

    private static final long serialVersionUID = 1L;

    /** 工单号（业务唯一键） */
    private String ticketNo;

    /** 工单标题 */
    private String title;

    /** 工单内容 */
    private String content;

    /** 是否模拟消费失败（true：消费端抛异常触发 MQ 重试直至死信，用于演示失败闭环） */
    private Boolean simulateFail;
}
