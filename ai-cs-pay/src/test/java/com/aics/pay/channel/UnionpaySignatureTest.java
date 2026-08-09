package com.aics.pay.channel;

import org.junit.jupiter.api.Test;

import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.util.LinkedHashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class UnionpaySignatureTest {

    private static final String CHARSET = "UTF-8";

    private static KeyPair keyPair() throws Exception {
        KeyPairGenerator g = KeyPairGenerator.getInstance("RSA");
        g.initialize(2048);
        return g.generateKeyPair();
    }

    @Test
    void signAndVerify_shouldPass() throws Exception {
        KeyPair kp = keyPair();
        Map<String, String> params = new LinkedHashMap<>();
        params.put("version", "5.1.0");
        params.put("respCode", "00");
        params.put("orderId", "ORD001");
        params.put("txnAmt", "100");
        params.put("sign", UnionpaySignature.sign(params, kp.getPrivate(), CHARSET));
        assertTrue(UnionpaySignature.verify(params, kp.getPublic(), CHARSET));
    }

    @Test
    void verify_tampered_shouldFail() throws Exception {
        KeyPair kp = keyPair();
        Map<String, String> params = new LinkedHashMap<>();
        params.put("version", "5.1.0");
        params.put("respCode", "00");
        params.put("orderId", "ORD001");
        params.put("sign", UnionpaySignature.sign(params, kp.getPrivate(), CHARSET));
        params.put("txnAmt", "200");
        assertFalse(UnionpaySignature.verify(params, kp.getPublic(), CHARSET));
    }

    @Test
    void parseForm_shouldDecode() {
        Map<String, String> map = UnionpaySignature.parseForm("respCode=00&respMsg=%E6%88%90%E5%8A%9F&orderId=ORD001");
        assertEquals("00", map.get("respCode"));
        assertEquals("成功", map.get("respMsg"));
    }
}