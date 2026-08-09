package com.aics.pay.channel;

import lombok.Builder;
import lombok.Data;

/**
 * 支付下单结果（渠道返回的支付参数）
 */
@Data
@Builder
public class PayResult {

    /** 支付方式类型：QRCODE（扫码）/ REDIRECT（跳转收银台）/ JSAPI（调起支付） */
    private String payType;

    /** 跳转地址（收银台/支付宝收银台等） */
    private String payUrl;

    /** 二维码内容（Native/当面付） */
    private String codeUrl;

    /** 渠道交易号（如 prepay_id / trade_no） */
    private String tradeNo;
}