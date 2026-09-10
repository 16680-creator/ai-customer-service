package com.aics.mq.controller;

import com.aics.common.mq.MessagePublisher;
import com.aics.common.result.Result;
import com.aics.mq.dto.WorkOrderCreateRequest;
import com.aics.mq.entity.MqFailRecord;
import com.aics.mq.entity.WorkOrder;
import com.aics.mq.producer.WorkOrderProducer;
import com.aics.mq.service.WorkOrderService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 工单消息闭环演示接口
 */
@Tag(name = "工单消息演示", description = "创建工单 / 工单列表 / 消费失败记录 / 重推 / 当前 Broker")
@RestController
@RequestMapping("/mq")
@RequiredArgsConstructor
public class WorkOrderController {

    private final WorkOrderService workOrderService;

    /** 抽象层注入：具体是 RocketMQ 还是 Kafka 由 aics.mq.type 决定 */
    private final MessagePublisher messagePublisher;

    @Operation(summary = "当前生效的消息中间件",
            description = "查看 aics.mq.type 开关的实际装配结果，确认切换是否生效")
    @GetMapping("/broker-type")
    public Result<Map<String, Object>> brokerType() {
        Map<String, Object> info = new LinkedHashMap<>();
        info.put("brokerType", messagePublisher.brokerType().name());
        info.put("publisher", messagePublisher.getClass().getSimpleName());
        info.put("topic", WorkOrderProducer.TOPIC);
        info.put("switch", "aics.mq.type=rocketmq|kafka（Kafka 模式需同时激活 kafka profile）");
        return Result.success(info);
    }

    @Operation(summary = "创建工单并发送消息",
            description = "先落库(NEW)再发送；simulateFail=true 时消费端将失败并触发 MQ 重试直至死信")
    @PostMapping("/work-order/create")
    public Result<WorkOrder> create(@Valid @RequestBody WorkOrderCreateRequest request) {
        return Result.success(workOrderService.create(request));
    }

    @Operation(summary = "工单列表", description = "最新 100 条在前")
    @GetMapping("/work-order/list")
    public Result<List<WorkOrder>> listWorkOrders() {
        return Result.success(workOrderService.listWorkOrders());
    }

    @Operation(summary = "消费失败记录列表", description = "死信落库记录，最新 100 条在前")
    @GetMapping("/fail-records/list")
    public Result<List<MqFailRecord>> listFailRecords() {
        return Result.success(workOrderService.listFailRecords());
    }

    @Operation(summary = "重推失败记录",
            description = "按原消息体重新投递（去掉模拟失败标志），记录标记 REPUSHED")
    @PostMapping("/fail-records/{id}/repush")
    public Result<Void> repush(@PathVariable("id") Long id) {
        workOrderService.repush(id);
        return Result.success();
    }
}
