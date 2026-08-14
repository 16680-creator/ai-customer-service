package com.aics.pay.service;

import java.util.Map;

/**
 * 支付补偿/对账服务：待支付流水查单兜底 + 一致性对账（单边账检测）
 */
public interface PayCompensationService {

    Map<String, Object> compensate();
}