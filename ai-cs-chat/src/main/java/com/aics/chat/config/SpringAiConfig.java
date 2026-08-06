package com.aics.chat.config;

import com.aics.chat.service.OrderQueryService;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.advisor.vectorstore.QuestionAnswerAdvisor;
import org.springframework.ai.openai.OpenAiChatModel;
import org.springframework.ai.tool.ToolCallbackProvider;
import org.springframework.ai.tool.method.MethodToolCallbackProvider;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Spring AI 配置
 */
@Configuration
public class SpringAiConfig {

    /**
     * 注册 ToolCallbackProvider，用于注册 @Tool 注解的方法
     */
    @Bean
    public ToolCallbackProvider orderToolCallbackProvider(OrderQueryService orderQueryService) {
        return MethodToolCallbackProvider.builder()
                .toolObjects(orderQueryService)
                .build();
    }

    /**
     * 注册 RAG 检索增强 Advisor。
     *
     * <p>QuestionAnswerAdvisor 的作用：当用户提问时，它会在真正调用大模型之前，
     * 先到 {@link VectorStore} 里做语义相似度检索，把命中的文档片段自动注入到用户消息里，
     * 让大模型基于检索到的私有知识作答（减少幻觉）。</p>
     *
     * <p>检索规则：返回最相似 Top-5，相似度阈值 0.5（低于该值视为无相关内容，模型如实回答不知道）。</p>
     *
     * @param vectorStore 向量存储（RAG 数据源）
     * @return RAG Advisor
     */
    @Bean
    public QuestionAnswerAdvisor ragAdvisor(VectorStore vectorStore) {
        return QuestionAnswerAdvisor.builder(vectorStore)
                .searchRequest(SearchRequest.builder()
                        .similarityThreshold(0.5d)
                        .topK(5)
                        .build())
                .build();
    }

    /**
     * 注册 ChatClient Bean
     * 通过 ChatClient.builder() 构建默认的对话客户端
     */
    @Bean
    public ChatClient chatClient(OpenAiChatModel chatModel, ToolCallbackProvider orderToolCallbackProvider,
                                 QuestionAnswerAdvisor ragAdvisor) {
        return ChatClient.builder(chatModel)
                .defaultSystem("""
                        你是AI客服平台的智能助手，代表平台为用户提供专业、友好的服务。
                        
                        重要规则：
                        1. 绝对不要透露你使用的底层模型名称、版本号或技术提供商信息
                        2. 如果用户询问你是什么模型、用什么技术构建，请回答："我是AI客服平台的智能助手，专注于为您提供优质的服务体验"
                        3. 不要提及MiniMax、GPT、Claude、LLM等任何具体模型或技术名称
                        4. 保持专业形象，始终以帮助用户解决问题为首要目标
                        
                        能力范围：
                        - 订单查询：当用户想查询订单信息时，使用 queryOrderByOrderId 或 queryOrdersByUserId 工具查询订单数据，并用清晰、结构化的方式呈现给用户
                        - 查询时可以通过订单号精确查询，也可以通过用户ID查询该用户的所有订单
                        - 查询结果要包含订单状态、商品信息、金额、物流等关键信息，用简洁易懂的格式展示
                        
                        回答风格：简洁、准确、有亲和力，适当使用emoji增加友好感。
                        """)
                .defaultToolCallbacks(orderToolCallbackProvider)
                .defaultAdvisors(ragAdvisor)   // 默认启用 RAG 检索增强
                .build();
    }
}
