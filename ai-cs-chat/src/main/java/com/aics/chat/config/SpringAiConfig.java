package com.aics.chat.config;

import com.aics.chat.rag.rerank.RerankProperties;
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
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.beans.factory.annotation.Value;

/**
 * Spring AI 核心配置类 —— 装配对话、工具调用与 RAG 检索所需的 Bean。
 *
 * <h3>Bean 装配总览</h3>
 * <ul>
 *   <li>{@link #siliconFlowEmbeddingModel()}：向量模型（EmbeddingModel），{@code @Primary} 标注，
 *       指向硅基流动 {@code https://api.siliconflow.cn}，模型 {@code BAAI/bge-m3}。</li>
 *   <li>{@link #orderToolCallbackProvider(OrderQueryService)}：把 {@link OrderQueryService} 上
 *       {@code @Tool} 标注的方法注册成 LLM 可调用的 Function Tool。</li>
 *   <li>{@link #ragAdvisor(VectorStore)}：{@link QuestionAnswerAdvisor}，Spring AI 内置的 RAG 顾问，
 *       在每次 ChatClient 调用时自动注入向量检索结果作为上下文。</li>
 *   <li>{@link #chatClient(OpenAiChatModel, ToolCallbackProvider, QuestionAnswerAdvisor)}：
 *       {@link ChatClient}，绑定默认系统提示、工具与 RAG Advisor，是业务层 LLM 调用的统一入口。</li>
 * </ul>
 *
 * <h3>ChatModel 来源（OpenAI 兼容协议对接 DeepSeek）</h3>
 * <p>{@link OpenAiChatModel} 由 Spring AI 的 {@code spring-ai-openai-spring-boot-starter} 自动装配，
 * 配置项见 {@code application.yml} 的 {@code spring.ai.openai.*}：
 * 通过把 {@code base-url} 指向 DeepSeek 的 OpenAI 兼容端点（{@code https://api.deepseek.com}）、
 * 使用 DeepSeek 颁发的 API Key、模型名设为 {@code deepseek-chat}，即可让 Spring AI 用调用 OpenAI 的方式
 * 调用 DeepSeek —— 因为 DeepSeek 实现了 OpenAI Chat Completions 协议，请求/响应格式完全兼容。</p>
 *
 * <h3>为什么 Embedding 单独走硅基流动</h3>
 * <p>DeepSeek 官方不提供 {@code /v1/embeddings} 接口，无法用于文档向量化。
 * 故本配置类手动装配一个指向硅基流动的 {@link OpenAiEmbeddingModel}：
 * 硅基流动聚合了 BAAI 开源的 {@code bge-m3} 多语言向量模型（1024 维，中英文效果好，免费额度充足），
 * 同样使用 OpenAI 兼容协议调用。{@code @Primary} 保证 {@link org.springframework.ai.vectorstore.VectorStore}
 * 以及其他注入 {@link EmbeddingModel} 的位置优先使用此 Bean，而非 Spring Boot 自动装配的默认实例。
 * 启动类 {@link com.aics.chat.ChatApplication} 已通过 {@code exclude = OpenAiEmbeddingAutoConfiguration.class}
 * 关闭默认的 OpenAI Embedding 自动装配，避免冲突。</p>
 *
 * <h3>配置来源</h3>
 * <p>Embedding 的 base-url / api-key / model 从 Nacos 读取（{@code aics.embedding.*}），
 * 见类下 {@link Value} 字段；ChatModel 配置由 {@code spring.ai.openai.*} 装配，不在本类显式声明。</p>
 */
@Configuration
@EnableConfigurationProperties(RerankProperties.class)
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
     *
     * <p>装配逻辑：用 {@link OpenAiApi} 指向硅基流动 base-url + apiKey，
     * 然后构造 {@link OpenAiEmbeddingModel}（OpenAI 兼容协议），模型名 {@code BAAI/bge-m3}。
     * {@link MetadataMode#EMBED} 表示按"用于向量化"的模式读取文档元数据（去掉无关字段，保留正文）。</p>
     *
     * @return 硅基流动 BAAI/bge-m3 EmbeddingModel Bean
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
     * 注册 ToolCallbackProvider，用于注册 @Tool 注解的方法。
     *
     * <p>把 {@link OrderQueryService} 作为工具对象注册，Spring AI 会扫描其上
     * {@link org.springframework.ai.tool.annotation.Tool} 注解的方法（如订单查询），
     * 包装成 {@code ToolCallback} 暴露给 LLM；当 LLM 决定调用工具时，会回调到这些方法。</p>
     *
     * @param orderQueryService 订单查询服务（含 {@code @Tool} 方法）
     * @return 工具回调提供者
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
     * <p>{@link QuestionAnswerAdvisor} 会在每次 ChatClient 调用前自动：
     * 把用户问题送入 {@link VectorStore} 做相似度检索，命中结果作为上下文拼到 Prompt 中。
     * 这里配置 {@code similarityThreshold=0.3}（较低阈值，宽召回）+ {@code topK=5}。</p>
     *
     * @param vectorStore 向量库（Chroma）
     * @return RAG 检索顾问
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
     * 注册 ChatClient Bean。
     *
     * <p>ChatClient 是业务层调用 LLM 的统一入口，此处配置三项默认值：</p>
     * <ul>
     *   <li>{@code defaultSystem}：系统提示词，约束 AI 的身份、能力范围与回答风格
     *       （禁止透露底层模型名、识别当前用户、订单工具调用规则等）。</li>
     *   <li>{@code defaultToolCallbacks}：默认携带订单查询工具回调。</li>
     *   <li>{@code defaultAdvisors}：默认携带 {@link QuestionAnswerAdvisor}，
     *       让所有 ChatClient 调用都自动走 RAG 检索增强。</li>
     * </ul>
     *
     * @param chatModel                 OpenAiChatModel（DeepSeek 兼容），由 starter 自动装配
     * @param orderToolCallbackProvider 工具回调
     * @param ragAdvisor                RAG 顾问
     * @return 配置好默认值的 ChatClient
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
