package com.aics.order.service.impl;

import cn.hutool.core.util.RandomUtil;
import com.aics.common.exception.BusinessException;
import com.aics.common.result.ResultCode;
import com.aics.order.dto.AfterSaleApplyDTO;
import com.aics.order.dto.EligibilityQueryDTO;
import com.aics.order.entity.AfterSaleApplication;
import com.aics.order.enums.AfterSaleActionType;
import com.aics.order.enums.AfterSaleStatus;
import com.aics.order.enums.OrderStatus;
import com.aics.order.mapper.AfterSaleApplicationMapper;
import com.aics.order.service.AfterSaleService;
import com.aics.order.service.OrderService;
import com.aics.order.vo.AfterSaleApplyVO;
import com.aics.order.vo.EligibilityVO;
import com.aics.order.vo.OrderVO;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 售后申请服务实现
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AfterSaleServiceImpl implements AfterSaleService {

    private static final DateTimeFormatter NO_FORMATTER = DateTimeFormatter.ofPattern("yyyyMMddHHmmss");

    private final AfterSaleApplicationMapper afterSaleApplicationMapper;
    private final OrderService orderService;

    @Override
    public EligibilityVO checkEligibility(Long userId, EligibilityQueryDTO dto) {
        OrderVO order = fetchOrder(userId, dto.getOrderNo());
        return buildEligibility(order, dto.getOrderNo(), dto.getActionType());
    }

    @Override
    public AfterSaleApplyVO createApplication(Long userId, AfterSaleApplyDTO dto) {
        // 1. 动作类型校验
        AfterSaleActionType actionType = AfterSaleActionType.fromCode(dto.getActionType());
        if (actionType == null) {
            log.warn("售后动作类型无效: actionType={}", dto.getActionType());
            throw new BusinessException(ResultCode.AFTER_SALE_ACTION_INVALID);
        }

        // 2. 幂等：按 idempotencyKey 查已存在直接返回首次结果
        AfterSaleApplication existing = afterSaleApplicationMapper.selectOne(
                new LambdaQueryWrapper<AfterSaleApplication>()
                        .eq(AfterSaleApplication::getIdempotencyKey, dto.getIdempotencyKey()));
        if (existing != null) {
            log.info("幂等命中售后申请，直接返回: applicationNo={}", existing.getApplicationNo());
            return toVO(existing);
        }

        // 3. 资格校验（复用 checkEligibility 逻辑）
        OrderVO order = fetchOrder(userId, dto.getOrderNo());
        EligibilityVO eligibility = buildEligibility(order, dto.getOrderNo(), actionType.getCode());
        if (!eligibility.isEligible()) {
            throw new BusinessException(ResultCode.AFTER_SALE_NOT_ELIGIBLE, eligibility.getReason());
        }

        // 4. 组装并落库
        AfterSaleApplication application = new AfterSaleApplication();
        application.setApplicationNo(generateApplicationNo());
        application.setRunId(dto.getRunId());
        application.setIdempotencyKey(dto.getIdempotencyKey());
        application.setUserId(userId);
        application.setOrderNo(dto.getOrderNo());
        application.setProductId(dto.getProductId());
        application.setProductName(resolveProductName(order, dto.getProductId()));
        application.setQuantity(dto.getQuantity());
        application.setActionType(actionType.getCode());
        application.setReason(dto.getReason());
        application.setStatus(AfterSaleStatus.PENDING.getCode());

        int rows = afterSaleApplicationMapper.insert(application);
        if (rows <= 0) {
            log.error("售后申请落库失败: userId={}, orderNo={}", userId, dto.getOrderNo());
            throw new BusinessException(ResultCode.AFTER_SALE_CREATE_FAIL);
        }
        log.info("售后申请创建成功: applicationNo={}, userId={}, orderNo={}",
                application.getApplicationNo(), userId, dto.getOrderNo());
        return toVO(application);
    }

    @Override
    public List<AfterSaleApplyVO> listByUser(Long userId) {
        List<AfterSaleApplication> list = afterSaleApplicationMapper.selectList(
                new LambdaQueryWrapper<AfterSaleApplication>()
                        .eq(AfterSaleApplication::getUserId, userId)
                        .orderByDesc(AfterSaleApplication::getCreateTime));
        return list.stream().map(this::toVO).collect(Collectors.toList());
    }

    @Override
    public AfterSaleApplyVO getByApplicationNo(Long userId, String applicationNo) {
        AfterSaleApplication application = afterSaleApplicationMapper.selectOne(
                new LambdaQueryWrapper<AfterSaleApplication>()
                        .eq(AfterSaleApplication::getApplicationNo, applicationNo)
                        .eq(AfterSaleApplication::getUserId, userId));
        if (application == null) {
            throw new BusinessException(ResultCode.AFTER_SALE_APPLICATION_NOT_FOUND);
        }
        return toVO(application);
    }

    // ==================== 私有方法 ====================

    /**
     * 查询订单（OrderService 已做归属校验；查不到/不属于当前用户视为 null）
     */
    private OrderVO fetchOrder(Long userId, String orderNo) {
        try {
            return orderService.getOrderDetail(userId, orderNo);
        } catch (BusinessException e) {
            log.info("售后资格校验-订单不存在或不属于当前用户: userId={}, orderNo={}", userId, orderNo);
            return null;
        }
    }

    /**
     * 资格校验核心：订单存在/归属 -> 已支付 -> 无进行中申请
     */
    private EligibilityVO buildEligibility(OrderVO order, String orderNo, String actionType) {
        EligibilityVO vo = new EligibilityVO();
        vo.setOrderNo(orderNo);
        if (order == null) {
            vo.setEligible(false);
            vo.setReason("订单不存在或不属于当前用户");
            return vo;
        }
        if (!OrderStatus.PAID.getCode().equals(order.getStatus())) {
            vo.setOrderStatus(order.getStatus());
            vo.setEligible(false);
            vo.setReason("订单状态不允许售后（当前状态：" + order.getStatus() + "）");
            return vo;
        }
        vo.setOrderStatus(order.getStatus());

        Long ongoing = afterSaleApplicationMapper.selectCount(
                new LambdaQueryWrapper<AfterSaleApplication>()
                        .eq(AfterSaleApplication::getOrderNo, orderNo)
                        .eq(AfterSaleApplication::getActionType, actionType)
                        .in(AfterSaleApplication::getStatus,
                                AfterSaleStatus.PENDING.getCode(),
                                AfterSaleStatus.APPROVED.getCode()));
        if (ongoing != null && ongoing > 0) {
            vo.setEligible(false);
            vo.setReason("该订单已存在进行中的售后申请");
            return vo;
        }
        vo.setEligible(true);
        return vo;
    }

    /**
     * 从订单商品快照匹配商品名称（整单售后/未匹配返回 null）
     */
    private String resolveProductName(OrderVO order, Long productId) {
        if (productId == null || order == null
                || order.getItems() == null || order.getItems().isEmpty()) {
            return null;
        }
        return order.getItems().stream()
                .filter(item -> productId.equals(item.getProductId()))
                .map(OrderVO.OrderItemVO::getProductName)
                .findFirst()
                .orElse(null);
    }

    private String generateApplicationNo() {
        return "AS" + LocalDateTime.now().format(NO_FORMATTER) + RandomUtil.randomNumbers(4);
    }

    private AfterSaleApplyVO toVO(AfterSaleApplication entity) {
        AfterSaleApplyVO vo = new AfterSaleApplyVO();
        vo.setApplicationNo(entity.getApplicationNo());
        vo.setStatus(entity.getStatus());
        vo.setActionType(entity.getActionType());
        vo.setOrderNo(entity.getOrderNo());
        vo.setProductName(entity.getProductName());
        vo.setQuantity(entity.getQuantity());
        vo.setReason(entity.getReason());
        vo.setCreateTime(entity.getCreateTime());
        return vo;
    }
}
