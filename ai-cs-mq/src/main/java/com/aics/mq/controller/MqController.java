package com.aics.mq.controller;

import com.aics.common.result.Result;
import com.aics.mq.service.RocketMqAdminService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * RocketMQ 调度/管理接口
 */
@Tag(name = "RocketMQ 调度", description = "集群 / Topic / 消费组 / 堆积查询")
@RestController
@RequestMapping("/mq")
@RequiredArgsConstructor
public class MqController {

    private final RocketMqAdminService adminService;

    @Operation(summary = "概览")
    @GetMapping("/overview")
    public Result<Map<String, Object>> overview() {
        return Result.success(adminService.overview());
    }

    @Operation(summary = "集群/Broker 列表")
    @GetMapping("/cluster")
    public Result<List<Map<String, Object>>> cluster() {
        return Result.success(adminService.cluster());
    }

    @Operation(summary = "Topic 列表")
    @GetMapping("/topics")
    public Result<List<Map<String, Object>>> topics() {
        return Result.success(adminService.topics());
    }

    @Operation(summary = "Topic 详情")
    @GetMapping("/topic/{topic}")
    public Result<Map<String, Object>> topicDetail(@PathVariable("topic") String topic) {
        return Result.success(adminService.topicDetail(topic));
    }

    @Operation(summary = "消费组列表")
    @GetMapping("/groups")
    public Result<List<Map<String, Object>>> groups() {
        return Result.success(adminService.groups());
    }

    @Operation(summary = "消费组详情")
    @GetMapping("/group/{group}")
    public Result<Map<String, Object>> groupDetail(@PathVariable("group") String group) {
        return Result.success(adminService.groupDetail(group));
    }
}