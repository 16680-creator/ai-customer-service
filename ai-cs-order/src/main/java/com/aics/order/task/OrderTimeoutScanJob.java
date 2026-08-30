package com.aics.order.task;

import com.xxl.job.core.context.XxlJobHelper;
import com.xxl.job.core.handler.annotation.XxlJob;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * 订单超时扫描 JobHandler（XXL-Job 触发）。
 *
 * <p>学习要点：JobHandler 只做「调度入口」，业务逻辑复用 {@link OrderTimeoutScheduler}
 * 的扫描实现——@Scheduled（本地兜底）与 XXL-Job（集中调度）双通道执行同一份幂等逻辑，
 * 关单操作按订单状态机天然幂等，重复触发无副作用。</p>
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class OrderTimeoutScanJob {

    private final OrderTimeoutScheduler orderTimeoutScheduler;

    /**
     * 在 XXL-Job admin 中新建任务时填写 JobHandler = orderTimeoutScanJob，
     * 路由策略建议 FIRST（默认第一个执行器），阻塞处理策略建议丢弃后续调度。
     */
    @XxlJob("orderTimeoutScanJob")
    public void orderTimeoutScanJob() {
        log.info("XXL-Job: 执行订单超时扫描");
        orderTimeoutScheduler.scanExpiredOrders();
        XxlJobHelper.handleSuccess("订单超时扫描完成");
    }
}
