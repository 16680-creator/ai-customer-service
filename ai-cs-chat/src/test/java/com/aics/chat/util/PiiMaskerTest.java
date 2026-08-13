package com.aics.chat.util;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

/**
 * PiiMasker 敏感信息脱敏测试。
 */
class PiiMaskerTest {

    private final PiiMasker masker = new PiiMasker();

    @Test
    @DisplayName("手机号脱敏")
    void maskPhone() {
        assertEquals("手机号 138****5678 请联系", masker.mask("手机号 13812345678 请联系"));
    }

    @Test
    @DisplayName("身份证号脱敏")
    void maskIdCard() {
        assertEquals("身份证 110101********1234", masker.mask("身份证 110101199001011234"));
    }

    @Test
    @DisplayName("null 与空串原样返回")
    void nullAndEmpty() {
        assertNull(masker.mask(null));
        assertEquals("", masker.mask(""));
    }
}
