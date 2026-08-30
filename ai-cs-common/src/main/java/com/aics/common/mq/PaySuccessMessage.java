package com.aics.common.mq;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.math.BigDecimal;

/**
 * 支付成功消息（pay → order 的 RocketMQ 事务消息载荷）。
 *
 * <p>pay 服务以「半消息 + 本地事务 + 回查」发送；order 服务消费后执行
 * {@code confirmPay} 确认订单（内部幂等，重复消费安全）。两端共用此契约，
 * 字段变更需两端同步升级（学习项目简化为同仓共享）。</p>
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PaySuccessMessage implements Serializable {

    private static final long serialVersionUID = 1L;

    /** 订单号 */
    private String orderNo;

    /** 支付方式（WECHAT/ALIPAY/MOCK） */
    private String paymentMethod;

    /** 实付金额（order 侧用于与订单应付金额比对，防篡改） */
    private BigDecimal amount;

    /** 渠道交易流水号 */
    private String tradeNo;
}
