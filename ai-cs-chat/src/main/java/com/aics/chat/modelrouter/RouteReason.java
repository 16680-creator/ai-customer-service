package com.aics.chat.modelrouter;

// 设计要点：路由原因枚举化——观测和日志可按原因聚合，比散落的字符串文案更容易统计降级分布
public enum RouteReason {
    SCENARIO_DEFAULT,
    PRIMARY_UNAVAILABLE,
    QUOTA_DOWNGRADE,
    QUOTA_NO_CHEAPER_MODEL,
    NO_ELIGIBLE_MODEL
}
