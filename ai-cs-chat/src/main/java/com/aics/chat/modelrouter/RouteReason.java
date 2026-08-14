package com.aics.chat.modelrouter;

public enum RouteReason {
    SCENARIO_DEFAULT,
    PRIMARY_UNAVAILABLE,
    QUOTA_DOWNGRADE,
    QUOTA_NO_CHEAPER_MODEL,
    NO_ELIGIBLE_MODEL
}
