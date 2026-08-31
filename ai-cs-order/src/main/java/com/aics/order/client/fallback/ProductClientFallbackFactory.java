package com.aics.order.client.fallback;

import com.aics.common.exception.BusinessException;
import com.aics.common.result.Result;
import com.aics.common.result.ResultCode;
import com.aics.order.client.ProductClient;
import com.aics.order.dto.ProductRemoteDTO;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cloud.openfeign.FallbackFactory;
import org.springframework.stereotype.Component;

/**
 * 商品服务降级工厂（productClient 熔断开启 / 调用异常时触发）。
 *
 * <h3>降级语义红线（03-P2）：按方法性质区分，绝不假成功</h3>
 * <ul>
 *   <li><b>deductStock（关键写）</b>：抛业务异常快速失败——库存没扣成功下单必须失败，
 *       静默吞掉会造成超卖；Seata 全局事务随之回滚。</li>
 *   <li><b>getProduct（读）</b>：抛业务异常——购物车/库存校验路径已有
 *       "商品服务暂不可用"的用户提示兜底，库存校验退化为 Redis 镜像值。</li>
 *   <li><b>restoreStock（尽力而为写）</b>：记告警日志返回 fail，不抛——
 *       关单主流程（退券、关渠道订单）不应因回补失败中断；
 *       回补缺失靠对账任务兜底（见 03 计划 CDC/对账章节）。</li>
 * </ul>
 */
@Slf4j
@Component
public class ProductClientFallbackFactory implements FallbackFactory<ProductClient> {

    @Override
    public ProductClient create(Throwable cause) {
        log.warn("商品服务调用降级: cause={}", cause.getMessage());
        return new ProductClient() {

            @Override
            public Result<Void> deductStock(Long id, int quantity) {
                log.error("扣库存降级快速失败: productId={}, quantity={}, cause={}", id, quantity, cause.getMessage());
                throw new BusinessException(ResultCode.GATEWAY_SERVICE_UNAVAILABLE, "商品服务暂不可用，下单失败，请稍后再试");
            }

            @Override
            public Result<Void> restoreStock(Long id, int quantity) {
                log.error("回补库存降级（告警，不阻断关单）: productId={}, quantity={}, cause={}", id, quantity, cause.getMessage());
                return Result.fail(ResultCode.GATEWAY_SERVICE_UNAVAILABLE);
            }

            @Override
            public ProductRemoteDTO getProduct(Long id) {
                log.warn("商品详情降级快速失败: productId={}, cause={}", id, cause.getMessage());
                throw new BusinessException(ResultCode.GATEWAY_SERVICE_UNAVAILABLE);
            }
        };
    }
}
