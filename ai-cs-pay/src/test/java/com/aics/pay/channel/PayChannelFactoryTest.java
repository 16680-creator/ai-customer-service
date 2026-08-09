package com.aics.pay.channel;

import com.aics.common.exception.BusinessException;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class PayChannelFactoryTest {

    private final PayChannelFactory factory = new PayChannelFactory(List.of(new MockPayChannel()));

    @Test
    void getChannel_mock_shouldReturnMockPayChannel() {
        assertTrue(factory.getChannel("MOCK") instanceof MockPayChannel);
    }

    @Test
    void getChannel_unknown_shouldThrow() {
        BusinessException exception = assertThrows(BusinessException.class, () -> factory.getChannel("ALIPAY"));
        assertTrue(exception.getMessage().contains("不支持的支付方式"));
    }
}