package com.aics.mq.controller;

import com.aics.common.result.Result;
import com.aics.mq.service.RocketMqAdminService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class MqControllerTest {

    @Mock
    private RocketMqAdminService adminService;

    @InjectMocks
    private MqController mqController;

    @Test
    @DisplayName("概览 - 返回统计")
    void overview_shouldReturn() {
        when(adminService.overview()).thenReturn(Map.of("brokerCount", 1, "topicCount", 2, "groupCount", 1, "totalDiff", 0L));

        Result<Map<String, Object>> result = mqController.overview();

        assertEquals(200, result.getCode());
        assertEquals(2, result.getData().get("topicCount"));
    }

    @Test
    @DisplayName("Topic 列表 - 正常返回")
    void topics_shouldReturn() {
        when(adminService.topics()).thenReturn(List.of(Map.of("topic", "order-timeout-topic")));

        Result<List<Map<String, Object>>> result = mqController.topics();

        assertEquals(200, result.getCode());
        assertEquals(1, result.getData().size());
        assertEquals("order-timeout-topic", result.getData().get(0).get("topic"));
    }

    @Test
    @DisplayName("消费组列表 - 正常返回")
    void groups_shouldReturn() {
        when(adminService.groups()).thenReturn(List.of(Map.of("group", "chat-producer-group")));

        Result<List<Map<String, Object>>> result = mqController.groups();

        assertEquals(200, result.getCode());
        assertEquals("chat-producer-group", result.getData().get(0).get("group"));
    }
}