package com.aics.gateway.controller;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cloud.client.discovery.DiscoveryClient;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Mono;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 服务健康检查（基于 Nacos 注册中心健康实例，供前端首页实时展示）
 */
@RestController
@CrossOrigin(origins = "*")
@RequestMapping("/api/health")
public class HealthController {

    private static final Logger log = LoggerFactory.getLogger(HealthController.class);

    private final DiscoveryClient discoveryClient;

    /** 服务清单：key=注册中心服务名，port=端口 */
    private static final List<Map<String, Object>> SERVICES = List.of(
            Map.of("name", "API Gateway", "key", "gateway", "port", 8080, "icon", "SetUp"),
            Map.of("name", "User 用户服务", "key", "ai-cs-user", "port", 8081, "icon", "User"),
            Map.of("name", "Knowledge 知识库", "key", "ai-cs-knowledge", "port", 8082, "icon", "Collection"),
            Map.of("name", "Chat 对话服务", "key", "ai-cs-chat", "port", 8083, "icon", "ChatDotRound"),
            Map.of("name", "Search 搜索服务", "key", "ai-cs-search", "port", 8084, "icon", "Search"),
            Map.of("name", "Message 消息服务", "key", "ai-cs-message", "port", 8085, "icon", "Message"),
            Map.of("name", "Notify 通知服务", "key", "ai-cs-notify", "port", 8086, "icon", "Bell"),
            Map.of("name", "Order 订单服务", "key", "ai-cs-order", "port", 8087, "icon", "Tickets"),
            Map.of("name", "Product 商品服务", "key", "ai-cs-product", "port", 8088, "icon", "Goods")
    );

    public HealthController(DiscoveryClient discoveryClient) {
        this.discoveryClient = discoveryClient;
    }

    @GetMapping
    public Mono<List<Map<String, Object>>> health() {
        return Mono.just(buildHealth());
    }

    private List<Map<String, Object>> buildHealth() {
        List<Map<String, Object>> result = new ArrayList<>();
        for (Map<String, Object> svc : SERVICES) {
            Map<String, Object> item = new HashMap<>(svc);
            String key = (String) svc.get("key");
            if ("gateway".equals(key)) {
                item.put("status", "UP");
            } else {
                try {
                    boolean healthy = !discoveryClient.getInstances(key).isEmpty();
                    item.put("status", healthy ? "UP" : "DOWN");
                } catch (Exception e) {
                    log.warn("健康检查异常: {}", key, e);
                    item.put("status", "UNKNOWN");
                }
            }
            result.add(item);
        }
        return result;
    }
}
