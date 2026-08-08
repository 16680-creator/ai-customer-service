package com.aics.order.pay.channel;

import com.aics.common.exception.BusinessException;
import com.aics.common.result.ResultCode;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * 支付渠道工厂：按渠道标识（method）路由到对应实现
 *
 * <p>Spring 会自动注入所有 {@link PayChannel} 实现（按 getMethod() 建索引），
 * 因此"新增一种支付方式 = 新增一个实现类"，对业务代码零侵入。
 */
@Slf4j
@Component
public class PayChannelFactory {

    private final Map<String, PayChannel> channelMap;

    public PayChannelFactory(List<PayChannel> channels) {
        this.channelMap = channels.stream()
                .collect(Collectors.toMap(PayChannel::getMethod, Function.identity()));
        log.info("已注册支付渠道: {}", channelMap.keySet());
    }

    public PayChannel getChannel(String method) {
        PayChannel channel = channelMap.get(method);
        if (channel == null) {
            throw new BusinessException(ResultCode.ORDER_PAYMENT_METHOD_INVALID,
                    "不支持的支付方式: " + method);
        }
        return channel;
    }

    public Map<String, PayChannel> allChannels() {
        return channelMap;
    }
}