package com.aics.product.service.impl;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertNull;

/**
 * SiliconFlowImageDescriptionService 降级测试。
 */
class SiliconFlowImageDescriptionServiceTest {

    @Test
    @DisplayName("视觉模型禁用时 describe 返回 null")
    void disabledReturnsNull() {
        SiliconFlowImageDescriptionService service = new SiliconFlowImageDescriptionService(
                "https://api.siliconflow.cn", "sk-test", "Qwen/Qwen2.5-VL-72B-Instruct", false);
        assertNull(service.describe("http://minio.internal/x.png"));
    }

    @Test
    @DisplayName("apiKey 为空时 describe 返回 null（视觉模型未初始化）")
    void emptyApiKeyReturnsNull() {
        SiliconFlowImageDescriptionService service = new SiliconFlowImageDescriptionService(
                "https://api.siliconflow.cn", "", "Qwen/Qwen2.5-VL-72B-Instruct", true);
        assertNull(service.describe("http://minio.internal/x.png"));
    }

    @Test
    @DisplayName("imageUrl 为空时 describe 返回 null")
    void emptyUrlReturnsNull() {
        SiliconFlowImageDescriptionService service = new SiliconFlowImageDescriptionService(
                "https://api.siliconflow.cn", "", "Qwen/Qwen2.5-VL-72B-Instruct", true);
        assertNull(service.describe(null));
        assertNull(service.describe(""));
    }
}
