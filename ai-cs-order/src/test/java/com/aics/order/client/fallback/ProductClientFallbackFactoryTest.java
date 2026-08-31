package com.aics.order.client.fallback;

import com.aics.common.exception.BusinessException;
import com.aics.common.result.Result;
import com.aics.common.result.ResultCode;
import com.aics.order.client.ProductClient;
import com.aics.order.dto.ProductRemoteDTO;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * 商品服务降级工厂测试：验证三类方法的降级语义红线。
 */
class ProductClientFallbackFactoryTest {

    private final ProductClient fallback =
            new ProductClientFallbackFactory().create(new RuntimeException("connect timeout"));

    @Test
    @DisplayName("deductStock 关键写 - 降级快速失败抛业务异常（绝不假成功）")
    void deductStock_shouldFailFast() {
        BusinessException ex = assertThrows(BusinessException.class,
                () -> fallback.deductStock(1001L, 2));
        assertEquals(ResultCode.GATEWAY_SERVICE_UNAVAILABLE.getCode(), ex.getCode());
    }

    @Test
    @DisplayName("getProduct 读 - 降级抛业务异常，由调用方转用户提示/Redis 兜底")
    void getProduct_shouldFailFast() {
        BusinessException ex = assertThrows(BusinessException.class,
                () -> fallback.getProduct(1001L));
        assertEquals(ResultCode.GATEWAY_SERVICE_UNAVAILABLE.getCode(), ex.getCode());
    }

    @Test
    @DisplayName("restoreStock 尽力而为 - 降级返回 fail 不抛（不阻断关单主流程）")
    void restoreStock_shouldReturnFailWithoutThrowing() {
        assertDoesNotThrow(() -> {
            Result<Void> result = fallback.restoreStock(1001L, 2);
            assertFalse(ResultCode.SUCCESS.getCode() == result.getCode(), "降级结果必须是 fail，绝不能假成功");
        });
    }
}
