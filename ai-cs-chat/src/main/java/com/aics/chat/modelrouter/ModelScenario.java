package com.aics.chat.modelrouter;

// 设计要点：路由场景用编译期枚举而非字符串——配置键和调用方都可被编译/启动校验，拼写错误无法悄悄进入生产
public enum ModelScenario {
    /** 普通对话：多轮闲聊、非 RAG 场景的流式/同步回复 */
    CHAT,
    /** RAG 检索增强生成：携带知识库上下文的用户问答 */
    RAG,
    /** 会话摘要：对历史消息做压缩总结，用于上下文窗口管理 */
    SUMMARY,
    /** 意图识别：对用户输入做分类，判断后续走 RAG、NL2SQL 还是其他分支 */
    INTENT,
    /** Agent 工具调用：预留给 ReAct / Function-Calling 等智能体场景 */
    AGENT,
    /** NL2SQL：将自然语言问题转换为 SQL 查询语句 */
    NL2SQL,
    /** 查询改写：对用户原始 query 做纠错、补全、去口语化等预处理 */
    REWRITE,
    /** 图表生成：基于数据生成可视化图表（ECharts 配置等） */
    CHART,
    /** RAG 质量评判：用 LLM 对检索结果做相关性/忠实度打分 */
    JUDGE
}
