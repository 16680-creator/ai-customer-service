package com.aics.order.pay.channel;

import com.aics.common.exception.BusinessException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.*;

/**
 * 银联渠道单元测试
 * 验证：未配置商户参数拒绝下单；金额元/分换算正确。
 * （真实下单/回调验签需要银联签名证书与验签证书，接入联调时验证）
 */
class UnionpayChannelTest {

    @Test
    @DisplayName("下单 - 未配置商户参数应抛出异常")
    void createPayment_notConfigured_shouldThrow() {
        UnionpayChannel channel = new UnionpayChannel();

        BusinessException exception = assertThrows(BusinessException.class,
                () -> channel.createPayment(PayContext.builder().orderNo("ORD001")
                        .payAmount(new BigDecimal("199.00")).build()));
        assertTrue(exception.getMessage().contains("银联渠道未配置"));
    }

    @Test
    @DisplayName("金额换算 - 元转分")
    void yuanToFen_shouldConvert() {
        assertEquals(19900, UnionpayChannel.yuanToFen(new BigDecimal("199.00")));
        assertEquals(1, UnionpayChannel.yuanToFen(new BigDecimal("0.005")));
        assertEquals(0, UnionpayChannel.yuanToFen(null));
    }
}