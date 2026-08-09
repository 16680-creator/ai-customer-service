package com.aics.pay.channel;

import com.aics.common.exception.BusinessException;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.*;

class UnionpayChannelTest {

    @Test
    void createPayment_notConfigured_shouldThrow() {
        UnionpayChannel channel = new UnionpayChannel();
        BusinessException ex = assertThrows(BusinessException.class,
                () -> channel.createPayment(PayContext.builder().orderNo("ORD001")
                        .payAmount(new BigDecimal("199.00")).build()));
        assertTrue(ex.getMessage().contains("银联渠道未配置"));
    }

    @Test
    void closeOrder_notConfigured_shouldThrow() {
        assertThrows(BusinessException.class, () -> new UnionpayChannel().closeOrder("ORD001"));
    }

    @Test
    void yuanToFen_shouldConvert() {
        assertEquals(19900, UnionpayChannel.yuanToFen(new BigDecimal("199.00")));
        assertEquals(0, UnionpayChannel.yuanToFen(null));
    }
}