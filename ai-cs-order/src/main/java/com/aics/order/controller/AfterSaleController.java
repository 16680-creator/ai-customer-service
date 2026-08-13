package com.aics.order.controller;

import com.aics.common.result.Result;
import com.aics.order.dto.AfterSaleApplyDTO;
import com.aics.order.dto.EligibilityQueryDTO;
import com.aics.order.service.AfterSaleService;
import com.aics.order.vo.AfterSaleApplyVO;
import com.aics.order.vo.EligibilityVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 售后申请控制器
 */
@Tag(name = "售后申请", description = "售后资格校验、申请创建、申请查询")
@RestController
@RequestMapping("/order/after-sale")
@RequiredArgsConstructor
public class AfterSaleController {

    private final AfterSaleService afterSaleService;

    @Operation(summary = "校验售后资格")
    @PostMapping("/eligibility")
    public Result<EligibilityVO> checkEligibility(@RequestHeader("X-User-Id") Long userId,
                                                  @Valid @RequestBody EligibilityQueryDTO dto) {
        return Result.success(afterSaleService.checkEligibility(userId, dto));
    }

    @Operation(summary = "创建售后申请（幂等）")
    @PostMapping("/apply")
    public Result<AfterSaleApplyVO> createApplication(@RequestHeader("X-User-Id") Long userId,
                                                      @Valid @RequestBody AfterSaleApplyDTO dto) {
        return Result.success("售后申请已提交", afterSaleService.createApplication(userId, dto));
    }

    @Operation(summary = "查询我的售后申请列表")
    @GetMapping("/list")
    public Result<List<AfterSaleApplyVO>> listByUser(@RequestHeader("X-User-Id") Long userId) {
        return Result.success(afterSaleService.listByUser(userId));
    }

    @Operation(summary = "按申请单号查询售后申请")
    @GetMapping("/{applicationNo}")
    public Result<AfterSaleApplyVO> getByApplicationNo(@RequestHeader("X-User-Id") Long userId,
                                                       @PathVariable("applicationNo") String applicationNo) {
        return Result.success(afterSaleService.getByApplicationNo(userId, applicationNo));
    }
}
