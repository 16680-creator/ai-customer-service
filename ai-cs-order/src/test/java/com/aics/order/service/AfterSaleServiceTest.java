package com.aics.order.service;

import com.aics.common.exception.BusinessException;
import com.aics.common.result.ResultCode;
import com.aics.order.dto.AfterSaleApplyDTO;
import com.aics.order.dto.EligibilityQueryDTO;
import com.aics.order.entity.AfterSaleApplication;
import com.aics.order.enums.AfterSaleActionType;
import com.aics.order.enums.AfterSaleStatus;
import com.aics.order.enums.OrderStatus;
import com.aics.order.mapper.AfterSaleApplicationMapper;
import com.aics.order.service.impl.AfterSaleServiceImpl;
import com.aics.order.vo.AfterSaleApplyVO;
import com.aics.order.vo.EligibilityVO;
import com.aics.order.vo.OrderVO;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * 售后申请服务单元测试
 * TDD: 资格校验、幂等创建、查询（纯 Mockito）
 */
@ExtendWith(MockitoExtension.class)
class AfterSaleServiceTest {

    @Mock
    private AfterSaleApplicationMapper afterSaleApplicationMapper;

    @Mock
    private OrderService orderService;

    @InjectMocks
    private AfterSaleServiceImpl afterSaleService;

    // ==================== 工具方法 ====================

    private OrderVO buildOrderVO(String status) {
        OrderVO vo = new OrderVO();
        vo.setOrderNo("ORD20260809001");
        vo.setStatus(status);
        OrderVO.OrderItemVO item = new OrderVO.OrderItemVO();
        item.setProductId(1001L);
        item.setProductName("无线蓝牙耳机");
        vo.setItems(List.of(item));
        return vo;
    }

    private EligibilityQueryDTO buildEligibilityDTO(String orderNo, String actionType) {
        EligibilityQueryDTO dto = new EligibilityQueryDTO();
        dto.setOrderNo(orderNo);
        dto.setActionType(actionType);
        return dto;
    }

    private AfterSaleApplyDTO buildApplyDTO() {
        AfterSaleApplyDTO dto = new AfterSaleApplyDTO();
        dto.setOrderNo("ORD20260809001");
        dto.setProductId(1001L);
        dto.setQuantity(1);
        dto.setActionType(AfterSaleActionType.EXCHANGE.getCode());
        dto.setReason("耳机无法开机");
        dto.setRunId("run-001");
        dto.setIdempotencyKey("run-001-EXCHANGE");
        return dto;
    }

    private AfterSaleApplication buildEntity(String applicationNo, String idempotencyKey) {
        AfterSaleApplication entity = new AfterSaleApplication();
        entity.setId(1L);
        entity.setApplicationNo(applicationNo);
        entity.setIdempotencyKey(idempotencyKey);
        entity.setUserId(100L);
        entity.setOrderNo("ORD20260809001");
        entity.setProductId(1001L);
        entity.setProductName("无线蓝牙耳机");
        entity.setQuantity(1);
        entity.setActionType(AfterSaleActionType.EXCHANGE.getCode());
        entity.setReason("耳机无法开机");
        entity.setStatus(AfterSaleStatus.PENDING.getCode());
        return entity;
    }

    // ==================== checkEligibility ====================

    @Test
    @DisplayName("资格校验 - 订单不存在（或他人订单）不满足")
    void checkEligibility_orderNotFound_shouldReturnIneligible() {
        when(orderService.getOrderDetail(100L, "ORD_NONE"))
                .thenThrow(new BusinessException(ResultCode.ORDER_NOT_FOUND));

        EligibilityVO vo = afterSaleService.checkEligibility(100L,
                buildEligibilityDTO("ORD_NONE", "EXCHANGE"));

        assertFalse(vo.isEligible());
        assertEquals("订单不存在或不属于当前用户", vo.getReason());
        verify(afterSaleApplicationMapper, never()).selectCount(any());
    }

    @Test
    @DisplayName("资格校验 - getOrderDetail 返回 null 视为不满足（契约兼容）")
    void checkEligibility_orderNull_shouldReturnIneligible() {
        when(orderService.getOrderDetail(100L, "ORD_NULL")).thenReturn(null);

        EligibilityVO vo = afterSaleService.checkEligibility(100L,
                buildEligibilityDTO("ORD_NULL", "EXCHANGE"));

        assertFalse(vo.isEligible());
        assertEquals("订单不存在或不属于当前用户", vo.getReason());
    }

    @Test
    @DisplayName("资格校验 - 非已支付订单不满足并附当前状态")
    void checkEligibility_orderNotPaid_shouldReturnIneligible() {
        when(orderService.getOrderDetail(100L, "ORD20260809001"))
                .thenReturn(buildOrderVO(OrderStatus.PENDING_PAY.getCode()));

        EligibilityVO vo = afterSaleService.checkEligibility(100L,
                buildEligibilityDTO("ORD20260809001", "EXCHANGE"));

        assertFalse(vo.isEligible());
        assertTrue(vo.getReason().contains("订单状态不允许售后"));
        assertTrue(vo.getReason().contains("PENDING_PAY"));
        assertEquals("PENDING_PAY", vo.getOrderStatus());
    }

    @Test
    @DisplayName("资格校验 - 已存在进行中的售后申请不满足")
    void checkEligibility_hasOngoingApplication_shouldReturnIneligible() {
        when(orderService.getOrderDetail(100L, "ORD20260809001"))
                .thenReturn(buildOrderVO(OrderStatus.PAID.getCode()));
        when(afterSaleApplicationMapper.selectCount(any())).thenReturn(1L);

        EligibilityVO vo = afterSaleService.checkEligibility(100L,
                buildEligibilityDTO("ORD20260809001", "EXCHANGE"));

        assertFalse(vo.isEligible());
        assertEquals("该订单已存在进行中的售后申请", vo.getReason());
    }

    @Test
    @DisplayName("资格校验 - 满足条件返回 eligible=true")
    void checkEligibility_eligible_shouldReturnTrue() {
        when(orderService.getOrderDetail(100L, "ORD20260809001"))
                .thenReturn(buildOrderVO(OrderStatus.PAID.getCode()));
        when(afterSaleApplicationMapper.selectCount(any())).thenReturn(0L);

        EligibilityVO vo = afterSaleService.checkEligibility(100L,
                buildEligibilityDTO("ORD20260809001", "EXCHANGE"));

        assertTrue(vo.isEligible());
        assertNull(vo.getReason());
        assertEquals("ORD20260809001", vo.getOrderNo());
        assertEquals("PAID", vo.getOrderStatus());
    }

    @Test
    @DisplayName("资格校验 - selectCount 返回 null 视为无进行中申请")
    void checkEligibility_selectCountNull_shouldBeEligible() {
        when(orderService.getOrderDetail(100L, "ORD20260809001"))
                .thenReturn(buildOrderVO(OrderStatus.PAID.getCode()));
        when(afterSaleApplicationMapper.selectCount(any())).thenReturn(null);

        EligibilityVO vo = afterSaleService.checkEligibility(100L,
                buildEligibilityDTO("ORD20260809001", "EXCHANGE"));

        assertTrue(vo.isEligible());
    }

    // ==================== createApplication ====================

    @Test
    @DisplayName("创建售后申请 - 成功：AS 单号、PENDING、幂等键落库、商品快照")
    void createApplication_success() {
        when(orderService.getOrderDetail(100L, "ORD20260809001"))
                .thenReturn(buildOrderVO(OrderStatus.PAID.getCode()));
        when(afterSaleApplicationMapper.selectCount(any())).thenReturn(0L);
        when(afterSaleApplicationMapper.insert(any())).thenReturn(1);

        AfterSaleApplyVO vo = afterSaleService.createApplication(100L, buildApplyDTO());

        assertNotNull(vo);
        assertTrue(vo.getApplicationNo().startsWith("AS"));
        assertEquals(AfterSaleStatus.PENDING.getCode(), vo.getStatus());
        assertEquals("无线蓝牙耳机", vo.getProductName());
        assertEquals(1, vo.getQuantity());

        ArgumentCaptor<AfterSaleApplication> captor = ArgumentCaptor.forClass(AfterSaleApplication.class);
        verify(afterSaleApplicationMapper).insert(captor.capture());
        AfterSaleApplication saved = captor.getValue();
        assertEquals("run-001-EXCHANGE", saved.getIdempotencyKey());
        assertEquals(100L, saved.getUserId());
        assertEquals("ORD20260809001", saved.getOrderNo());
        assertEquals(AfterSaleActionType.EXCHANGE.getCode(), saved.getActionType());
        assertEquals(AfterSaleStatus.PENDING.getCode(), saved.getStatus());
        assertEquals("耳机无法开机", saved.getReason());
        assertEquals("run-001", saved.getRunId());
        assertEquals(1001L, saved.getProductId());
        assertEquals("无线蓝牙耳机", saved.getProductName());
    }

    @Test
    @DisplayName("创建售后申请 - 同一幂等键重复提交返回首次结果且只 insert 一次")
    void createApplication_idempotent_shouldReturnExisting() {
        AfterSaleApplication existing = buildEntity("AS202608091200000001", "run-001-EXCHANGE");
        when(afterSaleApplicationMapper.selectOne(any())).thenReturn(existing);

        AfterSaleApplyVO first = afterSaleService.createApplication(100L, buildApplyDTO());
        AfterSaleApplyVO second = afterSaleService.createApplication(100L, buildApplyDTO());

        assertEquals("AS202608091200000001", first.getApplicationNo());
        assertEquals("AS202608091200000001", second.getApplicationNo());
        verify(afterSaleApplicationMapper, never()).insert(any());
        verify(orderService, never()).getOrderDetail(anyLong(), anyString());
    }

    @Test
    @DisplayName("创建售后申请 - 非法 actionType 抛 AFTER_SALE_ACTION_INVALID")
    void createApplication_invalidActionType_shouldThrow() {
        AfterSaleApplyDTO dto = buildApplyDTO();
        dto.setActionType("XXX");

        BusinessException ex = assertThrows(BusinessException.class,
                () -> afterSaleService.createApplication(100L, dto));
        assertEquals(7105, ex.getCode());
        verify(afterSaleApplicationMapper, never()).insert(any());
    }

    @Test
    @DisplayName("创建售后申请 - 资格不通过抛 AFTER_SALE_NOT_ELIGIBLE 且带原因")
    void createApplication_notEligible_shouldThrow() {
        when(orderService.getOrderDetail(100L, "ORD20260809001"))
                .thenReturn(buildOrderVO(OrderStatus.PENDING_PAY.getCode()));

        BusinessException ex = assertThrows(BusinessException.class,
                () -> afterSaleService.createApplication(100L, buildApplyDTO()));
        assertEquals(7101, ex.getCode());
        assertTrue(ex.getMessage().contains("订单状态不允许售后"));
        verify(afterSaleApplicationMapper, never()).insert(any());
    }

    @Test
    @DisplayName("创建售后申请 - 整单售后（无商品ID）商品名称为空")
    void createApplication_withoutProductId_productNameNull() {
        when(orderService.getOrderDetail(100L, "ORD20260809001"))
                .thenReturn(buildOrderVO(OrderStatus.PAID.getCode()));
        when(afterSaleApplicationMapper.selectCount(any())).thenReturn(0L);
        when(afterSaleApplicationMapper.insert(any())).thenReturn(1);

        AfterSaleApplyDTO dto = buildApplyDTO();
        dto.setProductId(null);

        AfterSaleApplyVO vo = afterSaleService.createApplication(100L, dto);
        assertNull(vo.getProductName());

        ArgumentCaptor<AfterSaleApplication> captor = ArgumentCaptor.forClass(AfterSaleApplication.class);
        verify(afterSaleApplicationMapper).insert(captor.capture());
        assertNull(captor.getValue().getProductName());
    }

    @Test
    @DisplayName("创建售后申请 - 商品不在订单中时商品名称为空")
    void createApplication_productNotInOrder_productNameNull() {
        when(orderService.getOrderDetail(100L, "ORD20260809001"))
                .thenReturn(buildOrderVO(OrderStatus.PAID.getCode()));
        when(afterSaleApplicationMapper.selectCount(any())).thenReturn(0L);
        when(afterSaleApplicationMapper.insert(any())).thenReturn(1);

        AfterSaleApplyDTO dto = buildApplyDTO();
        dto.setProductId(9999L);

        AfterSaleApplyVO vo = afterSaleService.createApplication(100L, dto);
        assertNull(vo.getProductName());
    }

    @Test
    @DisplayName("创建售后申请 - 落库失败抛 AFTER_SALE_CREATE_FAIL")
    void createApplication_insertFail_shouldThrow() {
        when(orderService.getOrderDetail(100L, "ORD20260809001"))
                .thenReturn(buildOrderVO(OrderStatus.PAID.getCode()));
        when(afterSaleApplicationMapper.selectCount(any())).thenReturn(0L);
        when(afterSaleApplicationMapper.insert(any())).thenReturn(0);

        BusinessException ex = assertThrows(BusinessException.class,
                () -> afterSaleService.createApplication(100L, buildApplyDTO()));
        assertEquals(7103, ex.getCode());
    }

    // ==================== listByUser / getByApplicationNo ====================

    @Test
    @DisplayName("查询我的售后申请列表 - 返回映射后的 VO 列表")
    void listByUser_shouldReturnVOList() {
        AfterSaleApplication e1 = buildEntity("AS202608091200000001", "k1");
        AfterSaleApplication e2 = buildEntity("AS202608091200000002", "k2");
        e2.setStatus(AfterSaleStatus.APPROVED.getCode());
        when(afterSaleApplicationMapper.selectList(any())).thenReturn(List.of(e1, e2));

        List<AfterSaleApplyVO> list = afterSaleService.listByUser(100L);

        assertEquals(2, list.size());
        assertEquals("AS202608091200000001", list.get(0).getApplicationNo());
        assertEquals(AfterSaleStatus.PENDING.getCode(), list.get(0).getStatus());
        assertEquals("AS202608091200000002", list.get(1).getApplicationNo());
        assertEquals(AfterSaleStatus.APPROVED.getCode(), list.get(1).getStatus());
        assertEquals("无线蓝牙耳机", list.get(0).getProductName());
    }

    @Test
    @DisplayName("按申请单号查询 - 成功返回")
    void getByApplicationNo_success() {
        when(afterSaleApplicationMapper.selectOne(any()))
                .thenReturn(buildEntity("AS202608091200000001", "k1"));

        AfterSaleApplyVO vo = afterSaleService.getByApplicationNo(100L, "AS202608091200000001");

        assertNotNull(vo);
        assertEquals("AS202608091200000001", vo.getApplicationNo());
        assertEquals("EXCHANGE", vo.getActionType());
        assertEquals("ORD20260809001", vo.getOrderNo());
        assertEquals("耳机无法开机", vo.getReason());
    }

    @Test
    @DisplayName("按申请单号查询 - 不存在或不属于该用户抛 AFTER_SALE_APPLICATION_NOT_FOUND")
    void getByApplicationNo_notFound_shouldThrow() {
        when(afterSaleApplicationMapper.selectOne(any())).thenReturn(null);

        BusinessException ex = assertThrows(BusinessException.class,
                () -> afterSaleService.getByApplicationNo(100L, "AS_NONE"));
        assertEquals(7104, ex.getCode());
    }
}
