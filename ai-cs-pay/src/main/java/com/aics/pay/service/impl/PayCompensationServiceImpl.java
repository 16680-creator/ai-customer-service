package com.aics.pay.service.impl;

import com.aics.pay.channel.PayChannel;
import com.aics.pay.client.OrderPayClient;
import com.aics.pay.dto.OrderPayDetailVO;
import com.aics.pay.entity.PayTransaction;
import com.aics.pay.mapper.PayTransactionMapper;
import com.aics.pay.service.PayCompensationService;
import com.aics.pay.service.PayNotifyService;
import com.aics.pay.service.PayTransactionService;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 支付补偿/对账服务实现
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class PayCompensationServiceImpl implements PayCompensationService {

    private final PayTransactionMapper payTransactionMapper;
    private final PayNotifyService payNotifyService;
    private final OrderPayClient orderPayClient;

    @Override
    public Map<String, Object> compensate() {
        Map<String, Object> report = new LinkedHashMap<>();
        LocalDateTime now = LocalDateTime.now();

        // 1. 待支付流水 → 查单兜底补偿（回调丢失场景）
        List<PayTransaction> pending = payTransactionMapper.selectList(new LambdaQueryWrapper<PayTransaction>()
                .eq(PayTransaction::getStatus, PayTransactionService.STATUS_PENDING));
        int querySynced = 0;
        for (PayTransaction tx : pending) {
            try {
                if (payNotifyService.syncByQuery(tx.getOrderNo(), tx.getPaymentMethod())) {
                    querySynced++;
                }
            } catch (Exception e) {
                log.warn("查单兜底补偿失败: orderNo={}, err={}", tx.getOrderNo(), e.getMessage());
            }
        }

        // 2. 一致性对账：流水已成功但订单未支付（单边账检测）
        List<String> paySuccessButOrderNotPaid = new ArrayList<>();
        List<PayTransaction> successTxs = payTransactionMapper.selectList(new LambdaQueryWrapper<PayTransaction>()
                .eq(PayTransaction::getStatus, PayTransactionService.STATUS_SUCCESS));
        for (PayTransaction tx : successTxs) {
            try {
                OrderPayDetailVO order = orderPayClient.getOrderDetail(tx.getOrderNo());
                if (order == null || !"PAID".equals(order.getStatus())) {
                    paySuccessButOrderNotPaid.add(tx.getOrderNo());
                }
            } catch (Exception e) {
                paySuccessButOrderNotPaid.add(tx.getOrderNo() + "(订单查询失败)");
            }
        }

        report.put("time", now.toString());
        report.put("querySynced", querySynced);
        report.put("paySuccessButOrderNotPaid", paySuccessButOrderNotPaid);
        log.info("支付补偿/对账完成: querySynced={}, diffs={}", querySynced, paySuccessButOrderNotPaid);
        return report;
    }
}