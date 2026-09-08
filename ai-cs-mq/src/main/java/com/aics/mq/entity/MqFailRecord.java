package com.aics.mq.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * MQ 消费失败记录实体。
 * <p>
 * 由死信消费者（{@link com.aics.mq.consumer.WorkOrderDlqListener}）在消费重试耗尽后落库，
 * 前端可查看并按条重推；msg_id 唯一约束保证同一死信消息只落一条。</p>
 */
@Data
@TableName("mq_fail_record")
public class MqFailRecord implements Serializable {

    private static final long serialVersionUID = 1L;

    /** 主键ID */
    @TableId(type = IdType.AUTO)
    private Long id;

    /** RocketMQ 消息ID（唯一，防重复落库） */
    private String msgId;

    /** 来源业务主题 */
    private String topic;

    /** 消费组 */
    private String consumerGroup;

    /** 业务键（工单号） */
    private String bizKey;

    /** 原始消息体（JSON，重推时原样回放） */
    private String payload;

    /** 失败原因 */
    private String failReason;

    /** MQ 已重试次数 */
    private Integer reconsumeTimes;

    /** 状态：PENDING-待重推 REPUSHED-已重推 */
    private String status;

    /** 落库时间 */
    private LocalDateTime createTime;

    /** 重推时间 */
    private LocalDateTime repushTime;
}
