package com.aics.order.pay.channel;

import java.math.BigDecimal;

/**
 * 支付渠道抽象（解耦核心）
 *
 * <p>新增支付方式 = 新增一个实现类并在 Spring 中注册（bean 的 method() 即渠道标识），
 * 无需改动下单/回调/退款业务流程。已内置实现：
 * <ul>
 *   <li>{@link MockPayChannel}：模拟渠道（学习/演示）</li>
 *   <li>AlipayChannel：支付宝（当面付扫码）</li>
 *   <li>WechatNativeChannel：微信支付 Native（PC 扫码）</li>
 *   <li>UnionpayChannel：银联云闪付二维码</li>
 * </ul>
 */
public interface PayChannel {

    /** 渠道查单/通知：待支付 */
    String STATUS_PENDING = "PENDING";

    /** 渠道查单/通知：支付成功 */
    String STATUS_SUCCESS = "SUCCESS";

    /** 渠道查单/通知：已关闭/支付失败 */
    String STATUS_CLOSED = "CLOSED";

    /**
     * 渠道标识：MOCK / ALIPAY / WECHAT / UNIONPAY ...
     */
    String getMethod();

    /**
     * 渠道下单：返回支付参数（二维码内容 / 收银台跳转地址等）
     */
    PayResult createPayment(PayContext context);

    /**
     * 主动查单：返回渠道侧支付状态（PENDING / SUCCESS / CLOSED）
     */
    String queryPayment(String orderNo);

    /**
     * 解析渠道异步通知：验签（+微信解密）后返回标准化支付结果。
     * 验签失败必须抛出异常，防止伪造通知。
     */
    NotifyResult parseNotify(NotifyContext context);

    /**
     * 退款：返回商户退款单号与状态
     */
    RefundResult refund(String orderNo, BigDecimal refundAmount);
}