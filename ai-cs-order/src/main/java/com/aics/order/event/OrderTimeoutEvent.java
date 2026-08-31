package com.aics.order.event;

/**
 * 订单超时领域事件（进程内事件）。
 *
 * <p>RocketMQ 延迟消息负责跨时间/跨进程唤醒；进入 order 服务后发布本事件，
 * 把消息协议适配（OrderTimeoutListener）与关单业务（OrderTimeoutEventListener）解耦。
 * 消息只携带最小事实 orderNo，监听器自行回查订单状态保证幂等。</p>
 */
public record OrderTimeoutEvent(String orderNo) {
}
