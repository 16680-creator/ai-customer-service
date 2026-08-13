package com.aics.order.service;

import com.aics.order.dto.AfterSaleApplyDTO;
import com.aics.order.dto.EligibilityQueryDTO;
import com.aics.order.vo.AfterSaleApplyVO;
import com.aics.order.vo.EligibilityVO;

import java.util.List;

/**
 * 售后申请服务
 */
public interface AfterSaleService {

    /**
     * 校验售后资格（订单归属、状态、进行中申请去重）
     */
    EligibilityVO checkEligibility(Long userId, EligibilityQueryDTO dto);

    /**
     * 创建售后申请（幂等：同一 idempotencyKey 重复提交返回首次结果）
     */
    AfterSaleApplyVO createApplication(Long userId, AfterSaleApplyDTO dto);

    /**
     * 查询当前用户全部售后申请
     */
    List<AfterSaleApplyVO> listByUser(Long userId);

    /**
     * 按申请单号查询（含归属校验，防止越权）
     */
    AfterSaleApplyVO getByApplicationNo(Long userId, String applicationNo);
}
