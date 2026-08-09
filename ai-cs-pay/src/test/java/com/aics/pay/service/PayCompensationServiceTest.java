package com.aics.pay.service;

import com.aics.pay.client.OrderPayClient;
import com.aics.pay.dto.OrderPayDetailVO;
import com.aics.pay.entity.PayTransaction;
import com.aics.pay.mapper.PayTransactionMapper;
import com.aics.pay.service.impl.PayCompensationServiceImpl;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.Collections;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PayCompensationServiceTest {

    @Mock
    private PayTransactionMapper payTransactionMapper;
    @Mock
    private PayNotifyService payNotifyService;
    @Mock
    private OrderPayClient orderPayClient;

    @InjectMocks
    private PayCompensationServiceImpl compensationService;

    @Test
    void compensate_shouldSyncPendingAndReportDiff() {
        PayTransaction pending = new PayTransaction();
        pending.setOrderNo("ORD-PEND");
        pending.setPaymentMethod("ALIPAY");
        pending.setStatus(PayTransactionService.STATUS_PENDING);

        PayTransaction success = new PayTransaction();
        success.setOrderNo("ORD-SUCCESS");
        success.setStatus(PayTransactionService.STATUS_SUCCESS);

        when(payTransactionMapper.selectList(any()))
                .thenReturn(Collections.singletonList(pending), Collections.singletonList(success));
        when(payNotifyService.syncByQuery("ORD-PEND", "ALIPAY")).thenReturn(true);

        OrderPayDetailVO detail = new OrderPayDetailVO();
        detail.setOrderNo("ORD-SUCCESS");
        detail.setStatus("PENDING_PAY"); // 单边账：流水成功但订单未支付
        when(orderPayClient.getOrderDetail("ORD-SUCCESS")).thenReturn(detail);

        Map<String, Object> report = compensationService.compensate();

        assertEquals(1, report.get("querySynced"));
        assertTrue(((java.util.List<?>) report.get("paySuccessButOrderNotPaid")).contains("ORD-SUCCESS"));
    }
}