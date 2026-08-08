package com.aics.order.pay.channel;

import com.aics.common.exception.BusinessException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.*;

/**
 * 微信 Native 渠道单元测试
 * 验证：未配置商户参数拒绝下单；金额元/分换算正确。
 * （真实下单/回调解密需要商户证书与 APIv3 密钥，接入联调时验证）
 */
class WechatNativeChannelTest {

    @Test
    @DisplayName("下单 - 未配置商户参数应抛出异常")
    void createPayment_notConfigured_shouldThrow() {
        WechatNativeChannel channel = new WechatNativeChannel();

        BusinessException exception = assertThrows(BusinessException.class,
                () -> channel.createPayment(PayContext.builder().orderNo("ORD001")
                        .payAmount(new BigDecimal("199.00")).build()));
        assertTrue(exception.getMessage().contains("微信支付渠道未配置"));
    }

    @Test
    @DisplayName("金额换算 - 元转分")
    void yuanToFen_shouldConvert() {
        assertEquals(19900, WechatNativeChannel.yuanToFen(new BigDecimal("199.00")));
        assertEquals(100, WechatNativeChannel.yuanToFen(new BigDecimal("1")));
        assertEquals(1, WechatNativeChannel.yuanToFen(new BigDecimal("0.005")));
        assertEquals(0, WechatNativeChannel.yuanToFen(null));
    }

    @Test
    @DisplayName("金额换算 - 分转元")
    void fenToYuan_shouldConvert() {
        assertEquals(new BigDecimal("199.00"), WechatNativeChannel.fenToYuan(19900));
        assertEquals(BigDecimal.ZERO, WechatNativeChannel.fenToYuan(null));
    }
}