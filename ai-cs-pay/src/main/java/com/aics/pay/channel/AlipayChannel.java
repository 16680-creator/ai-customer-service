package com.aics.pay.channel;

import com.aics.common.exception.BusinessException;
import com.aics.common.result.ResultCode;
import com.alipay.api.AlipayApiException;
import com.alipay.api.AlipayClient;
import com.alipay.api.DefaultAlipayClient;
import com.alipay.api.domain.AlipayTradeCloseModel;
import com.alipay.api.domain.AlipayTradePrecreateModel;
import com.alipay.api.domain.AlipayTradeQueryModel;
import com.alipay.api.domain.AlipayTradeRefundModel;
import com.alipay.api.internal.util.AlipaySignature;
import com.alipay.api.request.AlipayTradeCloseRequest;
import com.alipay.api.request.AlipayTradePrecreateRequest;
import com.alipay.api.request.AlipayTradeQueryRequest;
import com.alipay.api.request.AlipayTradeRefundRequest;
import com.alipay.api.response.AlipayTradeCloseResponse;
import com.alipay.api.response.AlipayTradePrecreateResponse;
import com.alipay.api.response.AlipayTradeQueryResponse;
import com.alipay.api.response.AlipayTradeRefundResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.util.Map;

/**
 * 支付宝渠道（当面付 / 扫码）
 *
 * <p>使用官方 alipay-sdk-java：
 * <ul>
 *   <li>下单：alipay.trade.precreate（当面付预下单，返回二维码内容 qr_code）</li>
 *   <li>查单：alipay.trade.query</li>
 *   <li>回调：AlipaySignature.rsaCheckV1（RSA2 验签）</li>
 *   <li>退款：alipay.trade.refund</li>
 * </ul>
 * 如需"电脑网站支付"（跳转支付宝收银台），可把 precreate 换成 alipay.trade.page.pay，
 * payType 改为 REDIRECT 即可。
 */
@Slf4j
@Component
public class AlipayChannel implements PayChannel {

    @Value("${pay.alipay.gateway:https://openapi.alipay.com/gateway.do}")
    private String gateway;

    @Value("${pay.alipay.app-id:}")
    private String appId;

    @Value("${pay.alipay.private-key:}")
    private String privateKey;

    @Value("${pay.alipay.alipay-public-key:}")
    private String alipayPublicKey;

    @Value("${pay.alipay.notify-url:}")
    private String notifyUrl;

    @Value("${pay.alipay.return-url:}")
    private String returnUrl;

    @Override
    public String getMethod() {
        return "ALIPAY";
    }

    @Override
    public PayResult createPayment(PayContext context) {
        ensureConfigured();
        try {
            AlipayClient client = buildClient();
            AlipayTradePrecreateRequest request = new AlipayTradePrecreateRequest();
            request.setNotifyUrl(notifyUrl);
            AlipayTradePrecreateModel model = new AlipayTradePrecreateModel();
            model.setOutTradeNo(context.getOrderNo());
            model.setTotalAmount(context.getPayAmount().toPlainString());
            model.setSubject(context.getSubject());
            request.setBizModel(model);
            AlipayTradePrecreateResponse response = client.execute(request);
            if (!response.isSuccess()) {
                log.error("支付宝下单失败: orderNo={}, subMsg={}", context.getOrderNo(), response.getSubMsg());
                throw new BusinessException(ResultCode.ORDER_PAYMENT_METHOD_INVALID,
                        "支付宝下单失败: " + response.getSubMsg());
            }
            return PayResult.builder()
                    .payType("QRCODE")
                    .codeUrl(response.getQrCode())
                    .tradeNo(response.getOutTradeNo())
                    .build();
        } catch (AlipayApiException e) {
            log.error("支付宝下单异常: orderNo={}", context.getOrderNo(), e);
            throw new BusinessException(ResultCode.ORDER_PAYMENT_METHOD_INVALID,
                    "支付宝下单异常: " + e.getErrMsg());
        }
    }

    @Override
    public String queryPayment(String orderNo) {
        ensureConfigured();
        try {
            AlipayTradeQueryRequest request = new AlipayTradeQueryRequest();
            AlipayTradeQueryModel model = new AlipayTradeQueryModel();
            model.setOutTradeNo(orderNo);
            request.setBizModel(model);
            AlipayTradeQueryResponse response = buildClient().execute(request);
            if (!response.isSuccess()) {
                log.warn("支付宝查单失败: orderNo={}, subMsg={}", orderNo, response.getSubMsg());
                return STATUS_PENDING;
            }
            String tradeStatus = response.getTradeStatus();
            if ("TRADE_SUCCESS".equals(tradeStatus) || "TRADE_FINISHED".equals(tradeStatus)) {
                return STATUS_SUCCESS;
            }
            if ("TRADE_CLOSED".equals(tradeStatus)) {
                return STATUS_CLOSED;
            }
            return STATUS_PENDING;
        } catch (AlipayApiException e) {
            log.error("支付宝查单异常: orderNo={}", orderNo, e);
            return STATUS_PENDING;
        }
    }

    @Override
    public NotifyResult parseNotify(NotifyContext context) {
        ensureConfigured();
        Map<String, String> params = context.getParams();
        try {
            boolean ok = AlipaySignature.rsaCheckV1(params, alipayPublicKey, "UTF-8", "RSA2");
            if (!ok) {
                log.warn("支付宝回调验签失败");
                throw new BusinessException(ResultCode.UNAUTHORIZED, "支付宝回调验签失败");
            }
        } catch (AlipayApiException e) {
            log.error("支付宝回调验签异常", e);
            throw new BusinessException(ResultCode.UNAUTHORIZED, "支付宝回调验签异常");
        }
        String tradeStatus = params.get("trade_status");
        boolean success = "TRADE_SUCCESS".equals(tradeStatus) || "TRADE_FINISHED".equals(tradeStatus);
        return NotifyResult.builder()
                .orderNo(params.get("out_trade_no"))
                .success(success)
                .amount(new BigDecimal(params.getOrDefault("total_amount", "0")))
                .transactionId(params.get("trade_no"))
                .build();
    }

    @Override
    public void closeOrder(String orderNo) {
        ensureConfigured();
        try {
            AlipayTradeCloseRequest request = new AlipayTradeCloseRequest();
            AlipayTradeCloseModel model = new AlipayTradeCloseModel();
            model.setOutTradeNo(orderNo);
            request.setBizModel(model);
            AlipayTradeCloseResponse response = buildClient().execute(request);
            if (!response.isSuccess()) {
                log.warn("支付宝关单失败: orderNo={}, subMsg={}", orderNo, response.getSubMsg());
            }
        } catch (AlipayApiException e) {
            log.warn("支付宝关单异常: orderNo={}", orderNo, e);
        }
    }

    @Override
    public RefundResult refund(String orderNo, BigDecimal refundAmount) {
        ensureConfigured();
        try {
            AlipayTradeRefundRequest request = new AlipayTradeRefundRequest();
            AlipayTradeRefundModel model = new AlipayTradeRefundModel();
            model.setOutTradeNo(orderNo);
            model.setRefundAmount(refundAmount.toPlainString());
            request.setBizModel(model);
            AlipayTradeRefundResponse response = buildClient().execute(request);
            if (!response.isSuccess()) {
                log.error("支付宝退款失败: orderNo={}, subMsg={}", orderNo, response.getSubMsg());
                throw new BusinessException(ResultCode.ORDER_PAYMENT_METHOD_INVALID,
                        "支付宝退款失败: " + response.getSubMsg());
            }
            return RefundResult.builder()
                    .refundNo(response.getTradeNo())
                    .status("SUCCESS")
                    .build();
        } catch (AlipayApiException e) {
            log.error("支付宝退款异常: orderNo={}", orderNo, e);
            throw new BusinessException(ResultCode.ORDER_PAYMENT_METHOD_INVALID,
                    "支付宝退款异常: " + e.getErrMsg());
        }
    }

    /** 构建支付宝客户端（protected 便于测试替换；沙箱响应较慢，读超时放宽到 60s） */
    protected AlipayClient buildClient() {
        return DefaultAlipayClient.builder(gateway, appId, privateKey)
                .format("json")
                .charset("UTF-8")
                .alipayPublicKey(alipayPublicKey)
                .signType("RSA2")
                .connectTimeout(10000)
                .readTimeout(60000)
                .build();
    }

    private void ensureConfigured() {
        if (!StringUtils.hasText(appId) || !StringUtils.hasText(privateKey)
                || !StringUtils.hasText(alipayPublicKey)) {
            throw new BusinessException(ResultCode.ORDER_PAYMENT_METHOD_INVALID,
                    "支付宝渠道未配置商户参数，请在 Nacos 配置 pay.alipay.app-id/private-key/alipay-public-key");
        }
    }
}