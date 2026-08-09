package com.aics.pay.service.impl;

import com.aics.pay.entity.PayTransaction;
import com.aics.pay.mapper.PayTransactionMapper;
import com.aics.pay.service.PayTransactionService;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 支付流水服务实现（幂等）
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class PayTransactionServiceImpl implements PayTransactionService {

    private final PayTransactionMapper payTransactionMapper;

    @Override
    public PayTransaction createOrUpdatePending(String orderNo, Long userId, String paymentMethod, BigDecimal payAmount) {
        PayTransaction tx = getByOrderNo(orderNo);
        if (tx == null) {
            tx = new PayTransaction();
            tx.setOrderNo(orderNo);
            tx.setUserId(userId);
            tx.setPaymentMethod(paymentMethod);
            tx.setPayAmount(payAmount);
            tx.setStatus(STATUS_PENDING);
            tx.setNotifyCount(0);
            payTransactionMapper.insert(tx);
            log.info("创建支付流水: orderNo={}, method={}, amount={}", orderNo, paymentMethod, payAmount);
        } else if (STATUS_PENDING.equals(tx.getStatus())) {
            tx.setPaymentMethod(paymentMethod);
            tx.setPayAmount(payAmount);
            payTransactionMapper.updateById(tx);
        }
        return tx;
    }

    @Override
    public boolean markSuccess(String orderNo, String tradeNo, BigDecimal amount) {
        PayTransaction tx = getByOrderNo(orderNo);
        if (tx == null || STATUS_SUCCESS.equals(tx.getStatus())) {
            return false; // 幂等：已成功或不存在
        }
        tx.setStatus(STATUS_SUCCESS);
        if (StringUtils.hasText(tradeNo)) {
            tx.setTradeNo(tradeNo);
        }
        if (amount != null) {
            tx.setPayAmount(amount);
        }
        tx.setNotifyCount(tx.getNotifyCount() == null ? 1 : tx.getNotifyCount() + 1);
        tx.setPayTime(LocalDateTime.now());
        payTransactionMapper.updateById(tx);
        log.info("支付流水标记成功: orderNo={}, tradeNo={}", orderNo, tradeNo);
        return true;
    }

    @Override
    public boolean markClosed(String orderNo) {
        PayTransaction tx = getByOrderNo(orderNo);
        if (tx == null || !STATUS_PENDING.equals(tx.getStatus())) {
            return false;
        }
        tx.setStatus(STATUS_CLOSED);
        payTransactionMapper.updateById(tx);
        return true;
    }

    @Override
    public boolean markRefunding(String orderNo) {
        PayTransaction tx = getByOrderNo(orderNo);
        if (tx == null || (!STATUS_SUCCESS.equals(tx.getStatus()) && !STATUS_PENDING.equals(tx.getStatus()))) {
            return false;
        }
        tx.setStatus(STATUS_REFUNDING);
        payTransactionMapper.updateById(tx);
        return true;
    }

    @Override
    public boolean markRefunded(String orderNo) {
        PayTransaction tx = getByOrderNo(orderNo);
        if (tx == null) {
            return false;
        }
        tx.setStatus(STATUS_REFUNDED);
        tx.setRefundTime(LocalDateTime.now());
        payTransactionMapper.updateById(tx);
        return true;
    }

    @Override
    public PayTransaction getByOrderNo(String orderNo) {
        return payTransactionMapper.selectOne(
                new LambdaQueryWrapper<PayTransaction>().eq(PayTransaction::getOrderNo, orderNo));
    }
}