package com.aics.order.client;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * 支付服务调用客户端（订单服务 -> 支付服务，走 Nacos 服务名）
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class OrderPayClient {

    private static final String PAY_URL = "http://ai-cs-pay/pay/close";

    private final RestTemplate restTemplate;

    /** 通知支付服务关闭渠道订单（取消/超时关单，尽力而为） */
    public void closeChannel(String paymentMethod, String orderNo) {
        try {
            Map<String, String> body = new LinkedHashMap<>();
            body.put("orderNo", orderNo);
            body.put("paymentMethod", paymentMethod);
            restTemplate.postForObject(PAY_URL, body, Map.class);
            log.info("已通知支付服务关单: orderNo={}, method={}", orderNo, paymentMethod);
        } catch (Exception e) {
            log.warn("调用支付服务关单失败: orderNo={}, err={}", orderNo, e.getMessage());
        }
    }
}