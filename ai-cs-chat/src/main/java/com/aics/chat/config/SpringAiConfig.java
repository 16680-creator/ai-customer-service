package com.aics.chat.config;

import com.aics.chat.service.OrderQueryService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.advisor.vectorstore.QuestionAnswerAdvisor;
import org.springframework.ai.document.MetadataMode;
import org.springframework.ai.embedding.EmbeddingModel;
import org.springframework.ai.openai.OpenAiChatModel;
import org.springframework.ai.openai.OpenAiEmbeddingModel;
import org.springframework.ai.openai.OpenAiEmbeddingOptions;
import org.springframework.ai.openai.api.OpenAiApi;
import org.springframework.ai.tool.ToolCallbackProvider;
import org.springframework.ai.tool.method.MethodToolCallbackProvider;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.beans.factory.annotation.Value;

/**
 * Spring AI 配置
 */
@Configuration
public class SpringAiConfig {

    private static final Logger log = LoggerFactory.getLogger(SpringAiConfig.class);

    // Embedding 配置从 Nacos（aics.embedding.*）读取，DeepSeek 不支持 /v1/embeddings
    @Value("${aics.embedding.base-url:https://api.siliconflow.cn}")
    private String embeddingBaseUrl;

    @Value("${aics.embedding.api-key:}")
    private String embeddingApiKey;

    @Value("${aics.embedding.model:BAAI/bge-m3}")
    private String embeddingModel;

    /**
     * 自定义 EmbeddingModel：使用硅基流动 API（DeepSeek 不支持 /v1/embeddings）。
     * @Primary 确保 ChromaVectorStore 等注入点优先使用此 Bean。
     */
    @Bean
    @Primary
    public EmbeddingModel siliconFlowEmbeddingModel() {
        log.info("========================================");
        log.info("创建硅基流动 EmbeddingModel");
        log.info("  base-url : {}", embeddingBaseUrl);
        log.info("  api-key  : {}...{}", embeddingApiKey.substring(0, 6), embeddingApiKey.substring(embeddingApiKey.length() - 4));
        log.info("  model    : {}", embeddingModel);
        log.info("========================================");

        OpenAiApi embeddingApi = OpenAiApi.builder()
                .baseUrl(embeddingBaseUrl)
                .apiKey(embeddingApiKey)
                .build();
        return new OpenAiEmbeddingModel(embeddingApi, MetadataMode.EMBED,
                OpenAiEmbeddingOptions.builder().model(embeddingModel).build());
    }

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
     */
    @Bean
    public QuestionAnswerAdvisor ragAdvisor(VectorStore vectorStore) {
        return QuestionAnswerAdvisor.builder(vectorStore)
                .searchRequest(SearchRequest.builder()
                        .similarityThreshold(0.3d)
                        .topK(5)
                        .build())
                .build();
    }

    /**
     * 注册 ChatClient Bean
     */
    @Bean
    public ChatClient chatClient(OpenAiChatModel chatModel, ToolCallbackProvider orderToolCallbackProvider,
                                 QuestionAnswerAdvisor ragAdvisor) {
        return ChatClient.builder(chatModel)
                .defaultSystem("""
                        你是AI客服平台的智能助手，代表平台为用户提供专业、友好的服务。
                        
                        重要规则：
                        1. 绝对不要透露你使用的底层模型名称、版本号或技术提供商信息
                        2. 如果用户询问你是什么模型、用什么技术构建，请回答：“我是AI客服平台的智能助手，专注于为您提供优质的服务体验”
                        3. 不要提及MiniMax、GPT、Claude、LLM等任何具体模型或技术名称
                        4. 保持专业形象，始终以帮助用户解决问题为首要目标
                        
                        能力范围：
                        - 订单查询：当用户想查询订单信息时，使用 queryOrderByOrderId 或 queryOrdersByUserId 工具查询订单数据，并用清晰、结构化的方式呈现给用户
                        - 当前登录用户已由系统自动识别，无需向用户索要用户ID；用户询问“我的订单/订单列表”时直接调用 queryOrdersByUserId 查询
                          - 查询时可以通过订单号精确查询，也可以通过当前用户ID查询名下所有订单
                        - 查询结果要包含订单状态、商品信息、金额、物流等关键信息，用简洁易懂的格式展示
                        
                        回答风格：简洁、准确、有亲和力，适当使用emoji增加友好感。
                        """)
                .defaultToolCallbacks(orderToolCallbackProvider)
                .defaultAdvisors(ragAdvisor)
                .build();
    }
}
