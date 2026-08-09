package com.aics.pay.client;

import com.aics.common.exception.BusinessException;
import com.aics.common.result.ResultCode;
import com.aics.pay.dto.OrderPayDetailVO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import java.math.BigDecimal;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * 订单服务调用客户端（支付服务 -> 订单服务，走 Nacos 服务名）
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class OrderPayClient {

    private static final String ORDER_URL = "http://ai-cs-order/order/pay";

    private final RestTemplate restTemplate;

    public OrderPayDetailVO getOrderDetail(String orderNo) {
        try {
            Map<?, ?> resp = restTemplate.getForObject(ORDER_URL + "/detail/{orderNo}", Map.class, orderNo);
            return parseDetail(resp, orderNo);
        } catch (BusinessException e) {
            throw e;
        } catch (Exception e) {
            log.error("调用订单服务获取支付信息失败: orderNo={}", orderNo, e);
            throw new BusinessException(ResultCode.ORDER_NOT_FOUND, "订单服务暂时不可用");
        }
    }

    @SuppressWarnings("unchecked")
    private OrderPayDetailVO parseDetail(Map<?, ?> resp, String orderNo) {
        if (!isOk(resp)) {
            throw new BusinessException(ResultCode.ORDER_NOT_FOUND, "订单不存在或不可支付");
        }
        Map<String, Object> data = (Map<String, Object>) resp.get("data");
        if (data == null) {
            throw new BusinessException(ResultCode.ORDER_NOT_FOUND, "订单不存在或不可支付");
        }
        OrderPayDetailVO vo = new OrderPayDetailVO();
        vo.setOrderNo((String) data.get("orderNo"));
        vo.setUserId(((Number) data.get("userId")).longValue());
        vo.setStatus((String) data.get("status"));
        vo.setPayAmount(new BigDecimal(String.valueOf(data.get("payAmount"))));
        vo.setPaymentMethod((String) data.get("paymentMethod"));
        vo.setExpireTime((String) data.get("expireTime"));
        return vo;
    }

    public void confirmPay(String orderNo, String paymentMethod, BigDecimal amount, String tradeNo) {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("orderNo", orderNo);
        body.put("paymentMethod", paymentMethod);
        body.put("amount", amount == null ? null : amount.toPlainString());
        body.put("tradeNo", tradeNo);
        try {
            Map<?, ?> resp = restTemplate.postForObject(ORDER_URL + "/confirm", body, Map.class);
            if (!isOk(resp)) {
                log.warn("订单支付确认失败: orderNo={}, resp={}", orderNo, resp);
                throw new BusinessException(ResultCode.ORDER_PAY_AMOUNT_MISMATCH,
                        resp == null ? "订单确认失败" : String.valueOf(resp.get("message")));
            }
        } catch (BusinessException e) {
            throw e;
        } catch (Exception e) {
            log.error("调用订单服务支付确认失败: orderNo={}", orderNo, e);
            throw new BusinessException(ResultCode.ORDER_CREATE_FAIL, "订单服务暂时不可用");
        }
    }

    public void refundConfirm(String orderNo) {
        Map<String, Object> body = Map.of("orderNo", orderNo);
        try {
            Map<?, ?> resp = restTemplate.postForObject(ORDER_URL + "/refund-confirm", body, Map.class);
            if (!isOk(resp)) {
                log.warn("订单退款确认失败: orderNo={}, resp={}", orderNo, resp);
                throw new BusinessException(ResultCode.ORDER_NOT_FOUND,
                        resp == null ? "退款确认失败" : String.valueOf(resp.get("message")));
            }
        } catch (BusinessException e) {
            throw e;
        } catch (Exception e) {
            log.error("调用订单服务退款确认失败: orderNo={}", orderNo, e);
            throw new BusinessException(ResultCode.ORDER_CREATE_FAIL, "订单服务暂时不可用");
        }
    }

    private boolean isOk(Map<?, ?> resp) {
        if (resp == null || !resp.containsKey("code")) {
            return false;
        }
        Object code = resp.get("code");
        return code instanceof Number num && num.intValue() == 200;
    }
}