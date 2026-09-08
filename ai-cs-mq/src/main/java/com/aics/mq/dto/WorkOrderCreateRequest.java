package com.aics.mq.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

import java.io.Serializable;

/**
 * 创建工单请求体
 */
@Data
public class WorkOrderCreateRequest implements Serializable {

    private static final long serialVersionUID = 1L;

    /** 工单标题（必填） */
    @NotBlank(message = "工单标题不能为空")
    private String title;

    /** 工单内容 */
    private String content;

    /** 是否模拟消费失败（演示失败重试 → 死信 → 重推闭环） */
    private Boolean simulateFail = false;
}
