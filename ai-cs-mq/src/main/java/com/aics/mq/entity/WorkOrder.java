package com.aics.mq.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 工单实体（RocketMQ 消息闭环演示）。
 * <p>
 * 状态流转：NEW（创建后待消费）→ DONE（消费成功）；
 * 消费重试耗尽进死信后由死信消费者标记为 FAILED，前端重推成功后回到 DONE。
 * 时间字段由数据库默认值维护（本模块组件扫描不含 common 的 MetaObjectHandler）。</p>
 */
@Data
@TableName("work_order")
public class WorkOrder implements Serializable {

    private static final long serialVersionUID = 1L;

    /** 主键ID */
    @TableId(type = IdType.AUTO)
    private Long id;

    /** 工单号（业务唯一键，WO+时间戳+序号） */
    private String ticketNo;

    /** 工单标题 */
    private String title;

    /** 工单内容 */
    private String content;

    /** 状态：NEW-待消费 DONE-消费完成 FAILED-消费失败(死信) */
    private String status;

    /** 创建时间 */
    private LocalDateTime createTime;

    /** 更新时间 */
    private LocalDateTime updateTime;
}
