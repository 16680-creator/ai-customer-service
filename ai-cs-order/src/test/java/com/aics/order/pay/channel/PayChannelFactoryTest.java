package com.aics.order.pay.channel;

import com.aics.common.exception.BusinessException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * 支付渠道工厂单元测试
 * 验证：按渠道标识路由、未注册渠道拒绝（新增渠道只需在容器中注册实现类）。
 */
class PayChannelFactoryTest {

    private final PayChannelFactory factory = new PayChannelFactory(List.of(new MockPayChannel()));

    @Test
    @DisplayName("获取渠道 - MOCK 返回模拟渠道")
    void getChannel_mock_shouldReturnMockPayChannel() {
        PayChannel channel = factory.getChannel("MOCK");
        assertNotNull(channel);
        assertTrue(channel instanceof MockPayChannel);
    }

    @Test
    @DisplayName("获取渠道 - 未注册渠道应抛出异常")
    void getChannel_unknown_shouldThrow() {
        BusinessException exception = assertThrows(BusinessException.class,
                () -> factory.getChannel("ALIPAY"));
        assertTrue(exception.getMessage().contains("不支持的支付方式"));
    }
}