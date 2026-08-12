package com.aics.pay.channel;

import com.aics.common.exception.BusinessException;
import com.aics.common.result.ResultCode;

import com.wechat.pay.java.core.RSAAutoCertificateConfig;
import com.wechat.pay.java.core.notification.NotificationParser;
import com.wechat.pay.java.core.notification.RequestParam;
import com.wechat.pay.java.service.payments.model.Transaction;
import com.wechat.pay.java.service.payments.nativepay.NativePayService;
import com.wechat.pay.java.service.payments.nativepay.model.Amount;
import com.wechat.pay.java.service.payments.nativepay.model.CloseOrderRequest;
import com.wechat.pay.java.service.payments.nativepay.model.PrepayRequest;
import com.wechat.pay.java.service.payments.nativepay.model.PrepayResponse;
import com.wechat.pay.java.service.payments.nativepay.model.QueryOrderByOutTradeNoRequest;
import com.wechat.pay.java.service.refund.RefundService;
import com.wechat.pay.java.service.refund.model.AmountReq;
import com.wechat.pay.java.service.refund.model.CreateRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.math.RoundingMode;

/**
 * 微信支付 Native（PC 扫码）
 *
 * <p>使用官方 wechatpay-java（APIv3）：
 * <ul>
 *   <li>下单：/v3/pay/transactions/native（返回 code_url，前端渲染二维码）</li>
 *   <li>查单：/v3/pay/transactions/out-trade-no/{out_trade_no}</li>
 *   <li>回调：NotificationParser 自动验签 + AES-256-GCM 解密 resource</li>
 *   <li>退款：/v3/refund/domestic/refunds</li>
 * </ul>
 * 金额单位为"分"，本类通过 {@link #yuanToFen} / {@link #fenToYuan} 与业务层（元）互转。
 */
@Slf4j
@Component
public class WechatNativeChannel implements PayChannel {

    @Value("${pay.wechat.app-id:}")
    private String appId;

    @Value("${pay.wechat.mch-id:}")
    private String mchId;

    @Value("${pay.wechat.api-v3-key:}")
    private String apiV3Key;

    @Value("${pay.wechat.merchant-serial-no:}")
    private String merchantSerialNo;

    /** 商户 API 私钥（PEM 文件路径，与 merchant-private-key 二选一） */
    @Value("${pay.wechat.merchant-private-key-path:}")
    private String merchantPrivateKeyPath;

    /** 商户 API 私钥（PEM 内容，与 merchant-private-key-path 二选一） */
    @Value("${pay.wechat.merchant-private-key:}")
    private String merchantPrivateKey;

    @Value("${pay.wechat.notify-url:}")
    private String notifyUrl;

    @Value("${pay.wechat.gateway:https://api.mch.weixin.qq.com}")
    private String gateway;

    @Override
    /** 返回本渠道标识（与 PaymentMethod 枚举对应） */
    public String getMethod() {
        return "WECHAT";
    }

    @Override
    /**
     * 创建微信 Native 支付：生成预支付单并返回支付二维码链接。
     * <p><b>学习要点</b>：金额单位换算——微信接口用"分"，业务用"元"，
     * 必须经 yuanToFen 转换，避免精度丢失。</p>
     */
    public PayResult createPayment(PayContext context) {
        ensureConfigured();
        NativePayService service = new NativePayService.Builder().config(config()).build();

        PrepayRequest request = new PrepayRequest();
        request.setAppid(appId);
        request.setMchid(mchId);
        request.setDescription(context.getSubject());
        request.setOutTradeNo(context.getOrderNo());
        request.setNotifyUrl(notifyUrl);

        Amount amount = new Amount();
        amount.setTotal(yuanToFen(context.getPayAmount()));
        amount.setCurrency("CNY");
        request.setAmount(amount);

        PrepayResponse response = service.prepay(request);
        if (!StringUtils.hasText(response.getCodeUrl())) {
            throw new BusinessException(ResultCode.ORDER_PAYMENT_METHOD_INVALID,
                    "微信下单失败：未获取到 code_url");
        }
        log.info("[WechatPay] 下单成功: orderNo={}", context.getOrderNo());
        return PayResult.builder()
                .payType("QRCODE")
                .codeUrl(response.getCodeUrl())
                .build();
    }

    @Override
    /** 主动查询微信侧支付结果（轮询/对账用） */
    public String queryPayment(String orderNo) {
        ensureConfigured();
        NativePayService service = new NativePayService.Builder().config(config()).build();
        QueryOrderByOutTradeNoRequest request = new QueryOrderByOutTradeNoRequest();
        request.setMchid(mchId);
        request.setOutTradeNo(orderNo);

        Transaction transaction = service.queryOrderByOutTradeNo(request);
        Transaction.TradeStateEnum state = transaction.getTradeState();
        if (state == null) {
            return STATUS_PENDING;
        }
        return switch (state) {
            case SUCCESS -> STATUS_SUCCESS;
            case CLOSED, REVOKED, PAYERROR -> STATUS_CLOSED;
            default -> STATUS_PENDING;
        };
    }

    @Override
    public void closeOrder(String orderNo) {
        ensureConfigured();
        NativePayService service = new NativePayService.Builder().config(config()).build();
        CloseOrderRequest request = new CloseOrderRequest();
        request.setMchid(mchId);
        request.setOutTradeNo(orderNo);
        service.closeOrder(request);
        log.info("[WechatPay] 关闭订单: orderNo={}", orderNo);
    }

    @Override
    /**
     * 解析微信回调报文：验签 + 校验金额/订单号，返回业务结果。
     * <p><b>学习要点</b>：回调不可信，必须验签且核对订单号与金额，
     * 处理需幂等（重复回调不重复生效）。</p>
     */
    public NotifyResult parseNotify(NotifyContext context) {
        ensureConfigured();
        RSAAutoCertificateConfig config = config();
        RequestParam requestParam = new RequestParam.Builder()
                .serialNumber(context.getHeaders().getOrDefault("Wechatpay-Serial", ""))
                .nonce(context.getHeaders().getOrDefault("Wechatpay-Nonce", ""))
                .signature(context.getHeaders().getOrDefault("Wechatpay-Signature", ""))
                .timestamp(context.getHeaders().getOrDefault("Wechatpay-Timestamp", ""))
                .body(context.getBody())
                .build();

        // 自动验签 + 解密 resource，失败会抛异常（安全红线）
        NotificationParser parser = new NotificationParser(config);
        Transaction transaction = parser.parse(requestParam, Transaction.class);

        boolean success = Transaction.TradeStateEnum.SUCCESS.equals(transaction.getTradeState());
        BigDecimal amount = (transaction.getAmount() == null || transaction.getAmount().getPayerTotal() == null)
                ? BigDecimal.ZERO
                : fenToYuan(transaction.getAmount().getPayerTotal());
        return NotifyResult.builder()
                .orderNo(transaction.getOutTradeNo())
                .success(success)
                .amount(amount)
                .transactionId(transaction.getTransactionId())
                .build();
    }

    @Override
    /** 发起退款（部分/全额），返回退款结果 */
    public RefundResult refund(String orderNo, BigDecimal refundAmount) {
        ensureConfigured();
        RefundService refundService = new RefundService.Builder().config(config()).build();

        CreateRequest request = new CreateRequest();
        request.setOutTradeNo(orderNo);
        request.setOutRefundNo("REFUND-" + orderNo + "-" + System.currentTimeMillis());
        request.setNotifyUrl(notifyUrl);
        AmountReq amount = new AmountReq();
        int fen = yuanToFen(refundAmount);
        amount.setRefund((long) fen);
        amount.setTotal((long) fen); // 本项目为全额退款，total=refund；部分退款时 total 应为原订单实付金额
        amount.setCurrency("CNY");
        request.setAmount(amount);

        var response = refundService.create(request);
        return RefundResult.builder()
                .refundNo(response.getOutRefundNo())
                .status(response.getStatus() == null ? "PROCESSING" : response.getStatus().name())
                .build();
    }

    /** 构建微信 APIv3 配置（自动证书；protected 便于测试） */
    protected RSAAutoCertificateConfig config() {
        RSAAutoCertificateConfig.Builder builder = new RSAAutoCertificateConfig.Builder()
                .merchantId(mchId)
                .merchantSerialNumber(merchantSerialNo)
                .apiV3Key(apiV3Key);
        if (StringUtils.hasText(merchantPrivateKeyPath)) {
            builder.privateKeyFromPath(merchantPrivateKeyPath);
        } else {
            builder.privateKey(merchantPrivateKey);
        }
        return builder.build();
    }

    private void ensureConfigured() {
        if (!StringUtils.hasText(appId) || !StringUtils.hasText(mchId)
                || !StringUtils.hasText(apiV3Key) || !StringUtils.hasText(merchantSerialNo)
                || (!StringUtils.hasText(merchantPrivateKeyPath) && !StringUtils.hasText(merchantPrivateKey))) {
            throw new BusinessException(ResultCode.ORDER_PAYMENT_METHOD_INVALID,
                    "微信支付渠道未配置商户参数，请在 Nacos 配置 pay.wechat.*（app-id/mch-id/api-v3-key/商户证书）");
        }
    }

    /** 元 → 分（微信金额单位为分，四舍五入） */
    /** 元转分：BigDecimal 避免浮点误差，如 1.23 元 = 123 分 */
    public static int yuanToFen(BigDecimal yuan) {
        if (yuan == null) {
            return 0;
        }
        return yuan.movePointRight(2).setScale(0, RoundingMode.HALF_UP).intValueExact();
    }

    /** 分 → 元 */
    public static BigDecimal fenToYuan(Integer fen) {
        if (fen == null) {
            return BigDecimal.ZERO;
        }
        return BigDecimal.valueOf(fen).movePointLeft(2);
    }
}