package com.aics.order.pay.channel;

import com.aics.common.exception.BusinessException;
import com.aics.common.result.ResultCode;
import com.aics.order.pay.channel.UnionpaySignature.SignCert;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.util.StringUtils;
import org.springframework.web.client.RestTemplate;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.security.PublicKey;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * 银联云闪付二维码渠道（银联全渠道网关）
 *
 * <p>使用银联全渠道网关后台交易接口（backTransReq），纯 JCE 实现签名，无需官方 SDK：
 * <ul>
 *   <li>下单：txnType=01, txnSubType=07（消费-二维码主扫），返回 qrCode，用户用云闪付 App 扫码</li>
 *   <li>查单：txnType=00（查询）</li>
 *   <li>回调：银联证书验签（SHA256withRSA）</li>
 *   <li>退款：txnType=04（退款）</li>
 * </ul>
 * 接入前需在银联开放平台/收单机构申请商户号与证书（签名证书 .p12 + 验签证书 .cer）。
 */
@Slf4j
@Component
public class UnionpayChannel implements PayChannel {

    private static final String VERSION = "5.1.0";
    private static final String ENCODING = "UTF-8";
    private static final String SIGN_METHOD = "01";   // RSA
    private static final String CHANNEL_TYPE = "07";  // 互联网
    private static final String ACCESS_TYPE = "0";    // 商户直连接入

    @Value("${pay.unionpay.gateway:https://gateway.95516.com}")
    private String gateway;

    @Value("${pay.unionpay.merchant-id:}")
    private String merchantId;

    /** 签名证书（PKCS12，.p12）路径 */
    @Value("${pay.unionpay.sign-cert-path:}")
    private String signCertPath;

    @Value("${pay.unionpay.sign-cert-pwd:}")
    private String signCertPwd;

    /** 银联验签公钥证书（.cer）路径 */
    @Value("${pay.unionpay.verify-cert-path:}")
    private String verifyCertPath;

    @Value("${pay.unionpay.notify-url:}")
    private String notifyUrl;

    /** 独立的 RestTemplate（银联网关为外网地址，不走 @LoadBalanced） */
    private final RestTemplate restTemplate = new RestTemplate();

    @Override
    public String getMethod() {
        return "UNIONPAY";
    }

    @Override
    public PayResult createPayment(PayContext context) {
        ensureConfigured();
        SignCert signCert = loadSignCert();
        Map<String, String> params = baseParams();
        params.put("txnType", "01");      // 消费
        params.put("txnSubType", "07");   // 二维码主扫
        params.put("bizType", "000000");
        params.put("orderId", context.getOrderNo());
        params.put("txnTime", now());
        params.put("txnAmt", String.valueOf(yuanToFen(context.getPayAmount())));
        params.put("currencyCode", "156");
        params.put("backUrl", notifyUrl);
        params.put("certId", signCert.certId());
        params.put("sign", UnionpaySignature.sign(params, signCert.privateKey(), ENCODING));

        Map<String, String> resp = postBackTrans(params);
        if (!"00".equals(resp.get("respCode"))) {
            log.error("银联下单失败: orderNo={}, respMsg={}", context.getOrderNo(), resp.get("respMsg"));
            throw new BusinessException(ResultCode.ORDER_PAYMENT_METHOD_INVALID,
                    "银联下单失败: " + resp.get("respMsg"));
        }
        String qrCode = resp.get("qrCode");
        if (!StringUtils.hasText(qrCode)) {
            throw new BusinessException(ResultCode.ORDER_PAYMENT_METHOD_INVALID,
                    "银联下单失败：未获取到二维码");
        }
        return PayResult.builder()
                .payType("QRCODE")
                .codeUrl(qrCode)
                .tradeNo(resp.get("queryId"))
                .build();
    }

    @Override
    public String queryPayment(String orderNo) {
        ensureConfigured();
        Map<String, String> params = baseParams();
        params.put("txnType", "00");      // 查询
        params.put("txnSubType", "00");
        params.put("bizType", "000000");
        params.put("orderId", orderNo);
        params.put("txnTime", now());

        Map<String, String> resp = postBackTrans(params);
        if (!"00".equals(resp.get("respCode"))) {
            log.warn("银联查单失败: orderNo={}, respMsg={}", orderNo, resp.get("respMsg"));
            return STATUS_PENDING;
        }
        String origRespCode = resp.get("origRespCode");
        if ("00".equals(origRespCode)) {
            return STATUS_SUCCESS;
        }
        if ("04".equals(origRespCode) || "05".equals(origRespCode)) {
            return STATUS_CLOSED; // 04=已撤销 / 05=交易失败
        }
        return STATUS_PENDING;
    }

    @Override
    public NotifyResult parseNotify(NotifyContext context) {
        ensureConfigured();
        Map<String, String> params = context.getParams();
        boolean ok = UnionpaySignature.verify(params, loadVerifyPublicKey(), ENCODING);
        if (!ok) {
            log.warn("银联回调验签失败: orderId={}", params.get("orderId"));
            throw new BusinessException(ResultCode.UNAUTHORIZED, "银联回调验签失败");
        }
        boolean success = "00".equals(params.get("respCode"));
        BigDecimal amount = params.get("txnAmt") == null ? BigDecimal.ZERO
                : new BigDecimal(params.get("txnAmt")).movePointLeft(2);
        return NotifyResult.builder()
                .orderNo(params.get("orderId"))
                .success(success)
                .amount(amount)
                .transactionId(params.get("queryId"))
                .build();
    }

    @Override
    public RefundResult refund(String orderNo, BigDecimal refundAmount) {
        ensureConfigured();
        SignCert signCert = loadSignCert();
        String refundOrderNo = "REFUND-" + orderNo + "-" + System.currentTimeMillis();
        Map<String, String> params = baseParams();
        params.put("txnType", "04");      // 退款
        params.put("txnSubType", "00");
        params.put("bizType", "000000");
        params.put("orderId", refundOrderNo);
        params.put("txnTime", now());
        params.put("txnAmt", String.valueOf(yuanToFen(refundAmount)));
        params.put("currencyCode", "156");
        params.put("backUrl", notifyUrl);
        params.put("certId", signCert.certId());
        // TODO: 银联退款必须携带原消费交易 queryId（origQryId）。
        // 建议在订单表增加 pay_transaction_id 字段，支付回调时落库，接入时从订单读取。
        params.put("origQryId", resolveOrigQryId(orderNo));
        params.put("sign", UnionpaySignature.sign(params, signCert.privateKey(), ENCODING));

        Map<String, String> resp = postBackTrans(params);
        if (!"00".equals(resp.get("respCode"))) {
            log.error("银联退款失败: orderNo={}, respMsg={}", orderNo, resp.get("respMsg"));
            throw new BusinessException(ResultCode.ORDER_PAYMENT_METHOD_INVALID,
                    "银联退款失败: " + resp.get("respMsg"));
        }
        return RefundResult.builder()
                .refundNo(refundOrderNo)
                .status("SUCCESS")
                .build();
    }

    /** 原交易 queryId（TODO：接入时从订单表 pay_transaction_id 读取，当前以订单号占位） */
    protected String resolveOrigQryId(String orderNo) {
        return orderNo;
    }

    private Map<String, String> baseParams() {
        Map<String, String> params = new LinkedHashMap<>();
        params.put("version", VERSION);
        params.put("encoding", ENCODING);
        params.put("signMethod", SIGN_METHOD);
        params.put("channelType", CHANNEL_TYPE);
        params.put("accessType", ACCESS_TYPE);
        params.put("merId", merchantId);
        return params;
    }

    private Map<String, String> postBackTrans(Map<String, String> params) {
        String url = gateway + "/gateway/api/backTransReq.do";
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);
        MultiValueMap<String, String> form = new LinkedMultiValueMap<>();
        params.forEach(form::add);
        try {
            ResponseEntity<String> entity = restTemplate.postForEntity(url, new HttpEntity<>(form, headers), String.class);
            return UnionpaySignature.parseForm(entity.getBody());
        } catch (Exception e) {
            log.error("银联后台交易请求异常: url={}", url, e);
            throw new BusinessException(ResultCode.ORDER_PAYMENT_METHOD_INVALID,
                    "银联通道请求异常: " + e.getMessage());
        }
    }

    private SignCert loadSignCert() {
        return UnionpaySignature.loadSignCert(signCertPath, signCertPwd);
    }

    private PublicKey loadVerifyPublicKey() {
        return UnionpaySignature.loadVerifyPublicKey(verifyCertPath);
    }

    private void ensureConfigured() {
        if (!StringUtils.hasText(merchantId) || !StringUtils.hasText(signCertPath)
                || !StringUtils.hasText(verifyCertPath)) {
            throw new BusinessException(ResultCode.ORDER_PAYMENT_METHOD_INVALID,
                    "银联渠道未配置商户参数，请在 Nacos 配置 pay.unionpay.*（merchant-id/证书路径）");
        }
    }

    private String now() {
        return new SimpleDateFormat("yyyyMMddHHmmss").format(new Date());
    }

    /** 元 → 分（银联金额单位为分） */
    public static int yuanToFen(BigDecimal yuan) {
        if (yuan == null) {
            return 0;
        }
        return yuan.movePointRight(2).setScale(0, RoundingMode.HALF_UP).intValueExact();
    }
}