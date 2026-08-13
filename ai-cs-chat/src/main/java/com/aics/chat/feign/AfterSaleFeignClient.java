package com.aics.chat.feign;

import com.aics.chat.dto.AfterSaleApplyDTO;
import com.aics.chat.dto.AfterSaleApplyVO;
import com.aics.common.result.Result;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;

/**
 * 订单服务售后 Feign 客户端（调用 ai-cs-order 的售后命令）
 */
@FeignClient(name = "ai-cs-order")
public interface AfterSaleFeignClient {

    /**
     * 创建售后申请（写操作，幂等键去重）
     *
     * @param userId 当前登录用户 ID（透传做权限校验）
     * @param dto    申请参数（含 idempotencyKey）
     * @return 申请单号（包装在统一 Result 中）
     */
    @PostMapping("/order/after-sale/apply")
    Result<AfterSaleApplyVO> apply(@RequestHeader("X-User-Id") Long userId,
                                   @RequestBody AfterSaleApplyDTO dto);
}
