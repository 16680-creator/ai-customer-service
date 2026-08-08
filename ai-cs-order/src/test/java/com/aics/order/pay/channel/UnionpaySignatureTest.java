package com.aics.order.pay.channel;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.util.LinkedHashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * 银联签名工具单元测试
 * 验证：RSA/SHA256 签名-验签往返、篡改失败、表单解析。
 */
class UnionpaySignatureTest {

    private static final String CHARSET = "UTF-8";

    private static KeyPair keyPair() throws Exception {
        KeyPairGenerator generator = KeyPairGenerator.getInstance("RSA");
        generator.initialize(2048);
        return generator.generateKeyPair();
    }

    @Test
    @DisplayName("签名验签 - 正常往返应通过")
    void signAndVerify_shouldPass() throws Exception {
        KeyPair keyPair = keyPair();
        Map<String, String> params = new LinkedHashMap<>();
        params.put("version", "5.1.0");
        params.put("respCode", "00");
        params.put("orderId", "ORD001");
        params.put("txnAmt", "100");

        String sign = UnionpaySignature.sign(params, keyPair.getPrivate(), CHARSET);
        params.put("sign", sign);

        assertTrue(UnionpaySignature.verify(params, keyPair.getPublic(), CHARSET));
    }

    @Test
    @DisplayName("验签 - 参数被篡改应失败")
    void verify_tampered_shouldFail() throws Exception {
        KeyPair keyPair = keyPair();
        Map<String, String> params = new LinkedHashMap<>();
        params.put("version", "5.1.0");
        params.put("respCode", "00");
        params.put("orderId", "ORD001");
        params.put("txnAmt", "100");

        String sign = UnionpaySignature.sign(params, keyPair.getPrivate(), CHARSET);
        params.put("sign", sign);
        params.put("txnAmt", "200"); // 篡改

        assertFalse(UnionpaySignature.verify(params, keyPair.getPublic(), CHARSET));
    }

    @Test
    @DisplayName("验签 - 缺少 sign 应失败")
    void verify_noSign_shouldFail() throws Exception {
        KeyPair keyPair = keyPair();
        Map<String, String> params = Map.of("orderId", "ORD001");

        assertFalse(UnionpaySignature.verify(params, keyPair.getPublic(), CHARSET));
    }

    @Test
    @DisplayName("解析表单 - urlencoded 响应")
    void parseForm_shouldDecode() {
        Map<String, String> map = UnionpaySignature.parseForm("respCode=00&respMsg=%E6%88%90%E5%8A%9F&orderId=ORD001");

        assertEquals("00", map.get("respCode"));
        assertEquals("成功", map.get("respMsg"));
        assertEquals("ORD001", map.get("orderId"));
    }
}