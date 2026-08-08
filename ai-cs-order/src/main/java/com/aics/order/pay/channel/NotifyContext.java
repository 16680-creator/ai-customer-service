package com.aics.order.pay.channel;

import lombok.Builder;
import lombok.Data;

import java.util.Map;

/**
 * 渠道通知上下文：统一承载不同渠道的异步通知原始数据
 *
 * <ul>
 *   <li>支付宝/银联：表单参数在 {@link #params}</li>
 *   <li>微信 v3：验签头在 {@link #headers}，加密报文在 {@link #body}</li>
 * </ul>
 */
@Data
@Builder
public class NotifyContext {

    /** 表单/JSON 参数（支付宝 trade_status、银联 respCode 等） */
    private Map<String, String> params;

    /** HTTP 头（微信 v3 验签需要 Wechatpay-Signature/Nonce/Timestamp/Serial） */
    private Map<String, String> headers;

    /** 原始请求体（微信 v3 回调 JSON 报文） */
    private String body;
}