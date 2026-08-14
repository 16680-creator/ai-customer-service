package com.aics.pay.service;

import com.aics.pay.entity.PayTransaction;
import com.aics.pay.mapper.PayTransactionMapper;
import com.aics.pay.service.impl.PayTransactionServiceImpl;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PayTransactionServiceTest {

    @Mock
    private PayTransactionMapper payTransactionMapper;

    @InjectMocks
    private PayTransactionServiceImpl payTransactionService;

    @Test
    void createOrUpdatePending_shouldInsert() {
        when(payTransactionMapper.selectOne(any())).thenReturn(null);
        PayTransaction tx = payTransactionService.createOrUpdatePending("ORD001", 100L, "ALIPAY", new BigDecimal("199.00"));
        assertNotNull(tx);
        verify(payTransactionMapper).insert(any());
    }

    @Test
    void markSuccess_idempotent() {
        PayTransaction tx = new PayTransaction();
        tx.setStatus(PayTransactionService.STATUS_SUCCESS);
        when(payTransactionMapper.selectOne(any())).thenReturn(tx);
        assertFalse(payTransactionService.markSuccess("ORD001", "trade1", new BigDecimal("199.00")));
        verify(payTransactionMapper, never()).updateById(any());
    }

    @Test
    void markSuccess_pendingToSuccess() {
        PayTransaction tx = new PayTransaction();
        tx.setStatus(PayTransactionService.STATUS_PENDING);
        when(payTransactionMapper.selectOne(any())).thenReturn(tx);
        assertTrue(payTransactionService.markSuccess("ORD001", "trade1", new BigDecimal("199.00")));
        verify(payTransactionMapper).updateById(argThat(t ->
                PayTransactionService.STATUS_SUCCESS.equals(t.getStatus()) && "trade1".equals(t.getTradeNo())));
    }
}