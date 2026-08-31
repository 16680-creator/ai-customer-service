package com.aics.order.event;

/**
 * 订单支付成功领域事件。
 *
 * <p>由 confirmPay 的本地事务内发布；监听器使用
 * {@code @TransactionalEventListener(phase = AFTER_COMMIT)}，只有订单状态真正提交后
 * 才向 notify-topic 投递通知，避免「数据库回滚但用户收到支付成功通知」。</p>
 */
public record OrderPaidEvent(String orderNo, Long userId) {
}
