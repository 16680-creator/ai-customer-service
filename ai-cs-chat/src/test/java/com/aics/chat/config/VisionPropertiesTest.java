package com.aics.chat.config;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * VisionProperties 配置属性默认值测试。
 */
class VisionPropertiesTest {

    @Test
    @DisplayName("视觉配置默认值正确")
    void defaultValues() {
        VisionProperties p = new VisionProperties();
        assertEquals("https://api.siliconflow.cn", p.getBaseUrl());
        assertEquals("Qwen/Qwen3-VL-32B-Instruct", p.getModel());
        assertTrue(p.isEnabled());
        assertEquals("", p.getApiKey());
        assertEquals("", p.getAllowedImageHost());
    }
}
