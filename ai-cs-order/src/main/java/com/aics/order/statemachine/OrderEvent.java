package com.aics.order.statemachine;

/** 订单状态机事件（合法迁移的唯一入口）。 */
public enum OrderEvent {
    PAY, CANCEL, TIMEOUT, REFUND_REQUEST, REFUND_SUCCESS
}
