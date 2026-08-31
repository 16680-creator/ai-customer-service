package com.aics.order.client.fallback;

import com.aics.common.result.Result;
import com.aics.common.result.ResultCode;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.LinkedHashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;

/**
 * 支付服务降级工厂测试：关单通知属尽力而为语义，降级不抛、绝不假成功。
 */
class PayClientFallbackFactoryTest {

    private final PayClientFallbackFactory factory = new PayClientFallbackFactory();

    @Test
    @DisplayName("closeOrder 降级 - 返回 fail 不抛异常（关单主流程不中断）")
    void closeOrder_shouldReturnFailWithoutThrowing() {
        assertDoesNotThrow(() -> {
            Result<Void> result = factory.create(new RuntimeException("pay svc down"))
                    .closeOrder(new LinkedHashMap<>(Map.of("orderNo", "NO1", "paymentMethod", "WECHAT")));
            assertFalse(ResultCode.SUCCESS.getCode() == result.getCode());
        });
    }
}
