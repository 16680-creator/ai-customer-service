package com.aics.chat.modelrouter;

// 设计要点：路由场景用编译期枚举而非字符串——配置键和调用方都可被编译/启动校验，拼写错误无法悄悄进入生产
public enum ModelScenario {
    CHAT, RAG, SUMMARY, INTENT, AGENT, NL2SQL, REWRITE, CHART, JUDGE
}
