package com.aics.order.controller;

import com.aics.order.pay.channel.NotifyContext;
import com.aics.order.service.PayNotifyService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;

/**
 * 支付回调控制器（内部接口，由支付渠道异步通知）
 *
 * <p>统一走 {@link PayNotifyService#processNotify}：
 * 渠道验签（+微信 v3 解密）→ 幂等更新订单状态（PENDING_PAY → PAID）→ 投递通知。
 * <ul>
 *   <li>支付宝/银联：表单参数</li>
 *   <li>微信 v3：JSON 报文 + 验签头</li>
 * </ul>
 */
@Slf4j
@Tag(name = "支付回调", description = "支付渠道异步通知处理（验签 + 幂等更新订单状态）")
@RestController
@RequestMapping("/pay")
@RequiredArgsConstructor
public class PayCallbackController {

    private static final String[] WECHAT_HEADERS = {
            "Wechatpay-Timestamp", "Wechatpay-Nonce", "Wechatpay-Signature",
            "Wechatpay-Serial", "Wechatpay-Signature-Type"
    };

    private final PayNotifyService payNotifyService;

    @Operation(summary = "支付结果回调")
    @PostMapping("/callback/{paymentMethod}")
    public Map<String, String> payCallback(@PathVariable("paymentMethod") String paymentMethod,
                                           HttpServletRequest request) {
        try {
            payNotifyService.processNotify(paymentMethod, buildNotifyContext(request));
            return Map.of("code", "SUCCESS", "message", "OK");
        } catch (Exception e) {
            log.warn("支付回调处理失败: method={}, err={}", paymentMethod, e.getMessage());
            return Map.of("code", "FAIL", "message", e.getMessage() == null ? "处理失败" : e.getMessage());
        }
    }

    private NotifyContext buildNotifyContext(HttpServletRequest request) throws IOException {
        Map<String, String> params = new HashMap<>();
        request.getParameterMap().forEach((k, v) -> params.put(k, (v != null && v.length > 0) ? v[0] : ""));

        Map<String, String> headers = new HashMap<>();
        for (String h : WECHAT_HEADERS) {
            String val = request.getHeader(h);
            if (val != null) {
                headers.put(h, val);
            }
        }

        String body = "";
        String contentType = request.getContentType();
        if (contentType != null && contentType.toLowerCase().contains("json")) {
            body = new String(request.getInputStream().readAllBytes(), StandardCharsets.UTF_8);
        }
        return NotifyContext.builder().params(params).headers(headers).body(body).build();
    }
}