package com.aics.order.controller;

import com.aics.common.exception.BusinessException;
import com.aics.common.result.Result;
import com.aics.common.result.ResultCode;
import com.aics.order.dto.AfterSaleApplyDTO;
import com.aics.order.dto.EligibilityQueryDTO;
import com.aics.order.service.AfterSaleService;
import com.aics.order.vo.AfterSaleApplyVO;
import com.aics.order.vo.EligibilityVO;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * 售后申请控制器单元测试
 * TDD: 验证四个端点正确委托 Service、Result 结构、X-User-Id 透传
 */
@ExtendWith(MockitoExtension.class)
class AfterSaleControllerTest {

    @Mock
    private AfterSaleService afterSaleService;

    @InjectMocks
    private AfterSaleController afterSaleController;

    private EligibilityVO buildEligibilityVO() {
        EligibilityVO vo = new EligibilityVO();
        vo.setEligible(true);
        vo.setOrderNo("ORD20260809001");
        vo.setOrderStatus("PAID");
        return vo;
    }

    private AfterSaleApplyVO buildApplyVO() {
        AfterSaleApplyVO vo = new AfterSaleApplyVO();
        vo.setApplicationNo("AS202608091200000001");
        vo.setStatus("PENDING");
        vo.setActionType("EXCHANGE");
        vo.setOrderNo("ORD20260809001");
        vo.setProductName("无线蓝牙耳机");
        vo.setQuantity(1);
        vo.setReason("耳机无法开机");
        vo.setCreateTime(LocalDateTime.now());
        return vo;
    }

    @Test
    @DisplayName("校验售后资格 - 委托 Service 并返回 Result 结构")
    void checkEligibility_success() {
        EligibilityQueryDTO dto = new EligibilityQueryDTO();
        dto.setOrderNo("ORD20260809001");
        dto.setActionType("EXCHANGE");
        when(afterSaleService.checkEligibility(100L, dto)).thenReturn(buildEligibilityVO());

        Result<EligibilityVO> result = afterSaleController.checkEligibility(100L, dto);

        assertEquals(200, result.getCode());
        assertTrue(result.isSuccess());
        assertTrue(result.getData().isEligible());
        verify(afterSaleService).checkEligibility(eq(100L), same(dto));
    }

    @Test
    @DisplayName("创建售后申请 - 委托 Service 并返回申请单号")
    void createApplication_success() {
        AfterSaleApplyDTO dto = new AfterSaleApplyDTO();
        dto.setOrderNo("ORD20260809001");
        dto.setActionType("EXCHANGE");
        dto.setReason("耳机无法开机");
        dto.setIdempotencyKey("k1");
        when(afterSaleService.createApplication(100L, dto)).thenReturn(buildApplyVO());

        Result<AfterSaleApplyVO> result = afterSaleController.createApplication(100L, dto);

        assertEquals(200, result.getCode());
        assertEquals("AS202608091200000001", result.getData().getApplicationNo());
        assertEquals("PENDING", result.getData().getStatus());
        verify(afterSaleService).createApplication(eq(100L), same(dto));
    }

    @Test
    @DisplayName("查询售后申请列表 - 委托 Service 返回列表")
    void listByUser_success() {
        when(afterSaleService.listByUser(100L)).thenReturn(List.of(buildApplyVO()));

        Result<List<AfterSaleApplyVO>> result = afterSaleController.listByUser(100L);

        assertEquals(200, result.getCode());
        assertEquals(1, result.getData().size());
        assertEquals("AS202608091200000001", result.getData().get(0).getApplicationNo());
        verify(afterSaleService).listByUser(100L);
    }

    @Test
    @DisplayName("按申请单号查询 - 委托 Service 返回详情")
    void getByApplicationNo_success() {
        when(afterSaleService.getByApplicationNo(100L, "AS202608091200000001"))
                .thenReturn(buildApplyVO());

        Result<AfterSaleApplyVO> result =
                afterSaleController.getByApplicationNo(100L, "AS202608091200000001");

        assertEquals(200, result.getCode());
        assertEquals("ORD20260809001", result.getData().getOrderNo());
        verify(afterSaleService).getByApplicationNo(100L, "AS202608091200000001");
    }

    @Test
    @DisplayName("按申请单号查询 - Service 抛业务异常原样传播")
    void getByApplicationNo_notFound_shouldPropagate() {
        when(afterSaleService.getByApplicationNo(100L, "AS_NONE"))
                .thenThrow(new BusinessException(ResultCode.AFTER_SALE_APPLICATION_NOT_FOUND));

        BusinessException ex = assertThrows(BusinessException.class,
                () -> afterSaleController.getByApplicationNo(100L, "AS_NONE"));
        assertEquals(7104, ex.getCode());
    }
}
