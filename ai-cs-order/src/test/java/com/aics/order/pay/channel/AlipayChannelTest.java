package com.aics.order.pay.channel;

import com.aics.common.exception.BusinessException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.Signature;
import java.util.Base64;
import java.util.Map;
import java.util.TreeMap;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.*;

/**
 * 支付宝渠道单元测试
 * 验证：未配置商户参数时拒绝下单；回调 RSA2 验签通过/篡改失败。
 */
class AlipayChannelTest {

    private static final KeyPair KEY_PAIR = generateKeyPair();

    @Test
    @DisplayName("下单 - 未配置商户参数应抛出异常")
    void createPayment_notConfigured_shouldThrow() {
        AlipayChannel channel = new AlipayChannel();

        BusinessException exception = assertThrows(BusinessException.class,
                () -> channel.createPayment(PayContext.builder().orderNo("ORD001")
                        .payAmount(new BigDecimal("199.00")).build()));
        assertTrue(exception.getMessage().contains("支付宝渠道未配置"));
    }

    @Test
    @DisplayName("解析回调 - RSA2 验签通过返回订单号与金额")
    void parseNotify_verifyOk_shouldReturnResult() throws Exception {
        AlipayChannel channel = configuredChannel();

        Map<String, String> params = new TreeMap<>();
        params.put("out_trade_no", "ORD001");
        params.put("total_amount", "199.00");
        params.put("trade_status", "TRADE_SUCCESS");
        params.put("trade_no", "2026080922000000001");
        params.put("sign", rsa2Sign(params));

        NotifyResult result = channel.parseNotify(NotifyContext.builder().params(params).build());

        assertTrue(result.isSuccess());
        assertEquals("ORD001", result.getOrderNo());
        assertEquals(new BigDecimal("199.00"), result.getAmount());
    }

    @Test
    @DisplayName("解析回调 - 参数被篡改验签失败应抛出异常")
    void parseNotify_tampered_shouldThrow() throws Exception {
        AlipayChannel channel = configuredChannel();

        Map<String, String> params = new TreeMap<>();
        params.put("out_trade_no", "ORD001");
        params.put("total_amount", "199.00");
        params.put("trade_status", "TRADE_SUCCESS");
        params.put("sign", rsa2Sign(params));
        params.put("total_amount", "0.01"); // 篡改金额

        assertThrows(BusinessException.class,
                () -> channel.parseNotify(NotifyContext.builder().params(params).build()));
    }

    /** 按支付宝 RSA2 规则签名（排除 sign/sign_type，按键升序拼接） */
    private static String rsa2Sign(Map<String, String> params) throws Exception {
        String content = params.entrySet().stream()
                .filter(e -> e.getValue() != null && !e.getValue().isEmpty())
                .filter(e -> !"sign".equals(e.getKey()) && !"sign_type".equals(e.getKey()))
                .sorted(Map.Entry.comparingByKey())
                .map(e -> e.getKey() + "=" + e.getValue())
                .collect(Collectors.joining("&"));
        Signature signature = Signature.getInstance("SHA256withRSA");
        signature.initSign(KEY_PAIR.getPrivate());
        signature.update(content.getBytes(StandardCharsets.UTF_8));
        return Base64.getEncoder().encodeToString(signature.sign());
    }

    private AlipayChannel configuredChannel() {
        AlipayChannel channel = new AlipayChannel();
        ReflectionTestUtils.setField(channel, "appId", "test-app");
        ReflectionTestUtils.setField(channel, "privateKey", privateKeyStr());
        ReflectionTestUtils.setField(channel, "alipayPublicKey", publicKeyStr());
        return channel;
    }

    private static KeyPair generateKeyPair() {
        try {
            KeyPairGenerator generator = KeyPairGenerator.getInstance("RSA");
            generator.initialize(2048);
            return generator.generateKeyPair();
        } catch (Exception e) {
            throw new IllegalStateException("生成测试 RSA 密钥失败", e);
        }
    }

    private static String privateKeyStr() {
        return Base64.getEncoder().encodeToString(KEY_PAIR.getPrivate().getEncoded());
    }

    private static String publicKeyStr() {
        return Base64.getEncoder().encodeToString(KEY_PAIR.getPublic().getEncoded());
    }
}