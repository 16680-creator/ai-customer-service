package com.aics.pay.channel;

import com.aics.common.exception.BusinessException;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.*;

class WechatNativeChannelTest {

    @Test
    void createPayment_notConfigured_shouldThrow() {
        WechatNativeChannel channel = new WechatNativeChannel();
        BusinessException ex = assertThrows(BusinessException.class,
                () -> channel.createPayment(PayContext.builder().orderNo("ORD001")
                        .payAmount(new BigDecimal("199.00")).build()));
        assertTrue(ex.getMessage().contains("微信支付渠道未配置"));
    }

    @Test
    void closeOrder_notConfigured_shouldThrow() {
        assertThrows(BusinessException.class, () -> new WechatNativeChannel().closeOrder("ORD001"));
    }

    @Test
    void yuanToFen_shouldConvert() {
        assertEquals(19900, WechatNativeChannel.yuanToFen(new BigDecimal("199.00")));
        assertEquals(0, WechatNativeChannel.yuanToFen(null));
    }

    @Test
    void fenToYuan_shouldConvert() {
        assertEquals(new BigDecimal("199.00"), WechatNativeChannel.fenToYuan(19900));
        assertEquals(BigDecimal.ZERO, WechatNativeChannel.fenToYuan(null));
    }
}