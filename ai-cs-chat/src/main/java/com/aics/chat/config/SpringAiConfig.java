package com.aics.chat.config;

import com.aics.chat.cache.CacheProperties;
import com.aics.chat.cache.CachingEmbeddingModel;
import com.aics.chat.cache.VectorCacheStore;
import com.aics.chat.modelrouter.ChatClientCustomizer;
import com.aics.chat.rag.rerank.RerankProperties;
import com.aics.chat.service.OrderQueryService;
import com.aics.chat.nl2sql.Nl2SqlQueryService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.chat.client.advisor.vectorstore.QuestionAnswerAdvisor;
import org.springframework.ai.document.MetadataMode;
import org.springframework.ai.embedding.EmbeddingModel;
import org.springframework.ai.openai.OpenAiEmbeddingModel;
import org.springframework.ai.openai.OpenAiEmbeddingOptions;
import org.springframework.ai.openai.api.OpenAiApi;
import org.springframework.ai.tool.ToolCallbackProvider;
import org.springframework.ai.tool.method.MethodToolCallbackProvider;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.beans.factory.annotation.Value;

/**
 * Spring AI 核心配置类 —— 装配对话、工具调用与 RAG 检索所需的 Bean。
 *
 * <h3>【AI 技术深度解析】Spring AI 框架架构</h3>
 * <p>Spring AI 是 Spring 官方推出的 AI 应用开发框架，核心设计理念是"抽象统一"：
 * <ul>
 *   <li><b>ChatClient</b>：统一的 LLM 调用入口，屏蔽底层模型差异（OpenAI/DeepSeek/ollama 等）</li>
 *   <li><b>EmbeddingModel</b>：文本向量化抽象，将文本转为高维向量（如 1024 维），用于语义相似度计算</li>
 *   <li><b>VectorStore</b>：向量存储抽象，支持 Chroma/Milvus/PGVector 等多种实现</li>
 *   <li><b>Advisor</b>：拦截器模式，在 LLM 调用前后注入逻辑（如 RAG 检索、历史管理）</li>
 *   <li><b>Tool Calling</b>：函数调用能力，让 LLM 能调用外部工具（如查询数据库、调用 API）</li>
 * </ul>
 *
 * <h3>Bean 装配总览</h3>
 * <ul>
 *   <li>{@link #siliconFlowEmbeddingModel()}：向量模型（EmbeddingModel），{@code @Primary} 标注，
 *       指向硅基流动 {@code https://api.siliconflow.cn}，模型 {@code BAAI/bge-m3}。</li>
 *   <li>{@link #toolCallbackProvider(OrderQueryService, Nl2SqlQueryService)}：把 {@link OrderQueryService} 上
 *       {@code @Tool} 标注的方法注册成 LLM 可调用的 Function Tool。</li>
 *   <li>{@link #ragAdvisor(VectorStore)}：{@link QuestionAnswerAdvisor}，Spring AI 内置的 RAG 顾问，
 *       在每次 ChatClient 调用时自动注入向量检索结果作为上下文。</li>
 *   <li>{@link #chatClientCustomizer(ToolCallbackProvider, QuestionAnswerAdvisor, com.aics.chat.prompt.PromptRegistry)}：
 *       ChatClientCustomizer，为模型路由注册的每个 ChatClient 绑定默认系统提示与 RAG Advisor。</li>
 * </ul>
 *
 * <h3>【AI 技术】ChatModel 来源（OpenAI 兼容协议对接 DeepSeek）</h3>
 * <p>OpenAiChatModel 由 Spring AI 的 {@code spring-ai-openai-spring-boot-starter} 自动装配，
 * 配置项见 {@code application.yml} 的 {@code spring.ai.openai.*}：
 * 通过把 {@code base-url} 指向 DeepSeek 的 OpenAI 兼容端点（{@code https://api.deepseek.com}）、
 * 使用 DeepSeek 颁发的 API Key、模型名设为 {@code deepseek-chat}，即可让 Spring AI 用调用 OpenAI 的方式
 * 调用 DeepSeek —— 因为 DeepSeek 实现了 OpenAI Chat Completions 协议，请求/响应格式完全兼容。</p>
 *
 * <p><b>【技术关联】OpenAI 兼容协议的意义</b>：
 * OpenAI 的 Chat Completions API（{@code POST /v1/chat/completions}）已成为事实标准，
 * DeepSeek/硅基流动/ollama 等都实现了该协议。Spring AI 的 OpenAI starter 可以无缝对接所有兼容实现，
 * 只需修改 {@code base-url} 和 {@code api-key} 即可切换模型供应商，实现"一套代码，多模型运行"。</p>
 *
 * <h3>【AI 技术】为什么 Embedding 单独走硅基流动</h3>
 * <p>DeepSeek 官方不提供 {@code /v1/embeddings} 接口，无法用于文档向量化。
 * 故本配置类手动装配一个指向硅基流动的 {@link OpenAiEmbeddingModel}：
 * 硅基流动聚合了 BAAI 开源的 {@code bge-m3} 多语言向量模型（1024 维，中英文效果好，免费额度充足），
 * 同样使用 OpenAI 兼容协议调用。{@code @Primary} 保证 {@link org.springframework.ai.vectorstore.VectorStore}
 * 以及其他注入 {@link EmbeddingModel} 的位置优先使用此 Bean，而非 Spring Boot 自动装配的默认实例。
 * 启动类 {@link com.aics.chat.ChatApplication} 已通过 {@code exclude = OpenAiEmbeddingAutoConfiguration.class}
 * 关闭默认的 OpenAI Embedding 自动装配，避免冲突。</p>
 *
 * <p><b>【技术关联】Embedding 模型选型</b>：
 * <ul>
 *   <li><b>BAAI/bge-m3</b>：BAAI（北京智源）开源的多语言向量模型，支持 100+ 语言，
 *       1024 维向量，在中文检索场景效果优秀，且免费额度充足（硅基流动提供）</li>
 *   <li><b>为什么不用 OpenAI text-embedding-3</b>：需要海外网络、付费，且中文效果不如 bge-m3</li>
 *   <li><b>为什么不用本地模型</b>：本地部署需要 GPU 资源，硅基流动提供云端 API 更便捷</li>
 * </ul>
 *
 * <h3>配置来源</h3>
 * <p>Embedding 的 base-url / api-key / model 从 Nacos 读取（{@code aics.embedding.*}），
 * 见类下 {@link Value} 字段；ChatModel 配置由 {@code spring.ai.openai.*} 装配，不在本类显式声明。</p>
 *
 * <h3>【技术关联】与其他模块的关系</h3>
 * <ul>
 *   <li><b>ai-cs-knowledge</b>：知识库模块使用相同的 bge-m3 模型进行文档向量化，保证向量空间一致</li>
 *   <li><b>ai-cs-search</b>：搜索模块使用 Elasticsearch 做关键词检索，与向量检索形成 Hybrid 互补</li>
 *   <li><b>ai-cs-chat</b>：对话模块通过 ChatClient 调用 LLM，通过 VectorStore 做 RAG 检索</li>
 * </ul>
 */
@Configuration
@EnableConfigurationProperties({RerankProperties.class, VisionProperties.class})
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
     * 数据库表结构参考（智能问数时供 LLM 组装 SQL）。
     * 精简格式：表名(列名 类型)。与远程 MySQL 实际结构一致。
     */
    private static final String DB_SCHEMA = """
            数据库表结构参考（编写 SQL 时必须使用真实表名/列名）：
            【user 用户库】
            sys_user(id, username, password, nickname, phone, email, avatar, status, role, create_time, update_time, deleted) 系统用户
            sys_role(id, role_code, role_name, description, status) 角色
            sys_user_role(id, user_id, role_id) 用户角色关联
            【product 商品库】
            product(id, name, description, price, stock, category_id, image, status, sales, create_time, update_time, deleted) 商品
            product_category(id, name, parent_id, sort, description, create_time, update_time) 商品分类
            【order 订单支付库】
            orders(id, order_no, user_id, total_amount, discount_amount, pay_amount, full_reduction_amount, coupon_amount, coupon_id, payment_method, status, pay_time, cancel_time, expire_time, create_time, update_time) 订单，status枚举如 PENDING_PAY/PAID/CANCELLED
            order_item(id, order_id, order_no, product_id, product_name, product_price, quantity, subtotal) 订单项
            cart_item(id, user_id, product_id, product_name, product_price, quantity, selected, create_time, update_time) 购物车
            coupon(id, user_id, coupon_name, amount, min_order_amount, status, expire_time, use_time, order_no, create_time) 优惠券
            full_reduction_rule(id, rule_name, threshold_amount, reduction_amount, start_time, end_time, enabled) 满减规则
            pay_transaction(id, order_no, user_id, payment_method, trade_no, pay_amount, status, notify_count, pay_time, refund_time, create_time, update_time) 支付流水
            【chat 对话消息库】
            chat_session(id, user_id, agent_id, channel, status, title, create_time, update_time, deleted) 会话
            chat_message(id, session_id, sender_type, sender_id, content, content_type, metadata, create_time, session_key, role) 消息
            【knowledge 知识库】
            kb_document(id, title, content, doc_type, source_url, summary, tags, category_id, status, create_by, create_time, update_time, deleted) 知识文档
            kb_category(id, name, parent_id, sort_order, description) 知识分类
            """;

    /**
     * 【AI 核心】自定义 EmbeddingModel：使用硅基流动 API（DeepSeek 不支持 /v1/embeddings）。
     * @Primary 确保 ChromaVectorStore 等注入点优先使用此 Bean。
     *
     * <p>装配逻辑：用 {@link OpenAiApi} 指向硅基流动 base-url + apiKey，
     * 然后构造 {@link OpenAiEmbeddingModel}（OpenAI 兼容协议），模型名 {@code BAAI/bge-m3}。
     * {@link MetadataMode#EMBED} 表示按"用于向量化"的模式读取文档元数据（去掉无关字段，保留正文）。</p>
     *
     * <p><b>【AI 技术详解】Embedding（向量化）原理</b>：
     * <ul>
     *   <li><b>什么是 Embedding</b>：将文本（如"如何退款"）转换为高维浮点数组（如 1024 维向量），
     *       使得语义相似的文本在向量空间中距离相近（余弦相似度高）</li>
     *   <li><b>为什么需要向量化</b>：传统关键词检索无法理解语义（如"退款"和"退货"是不同词但相关），
     *       向量检索能捕捉语义相似性，实现"以意搜"</li>
     *   <li><b>bge-m3 模型特点</b>：
     *       <ul>
     *         <li>1024 维向量：维度越高表达能力越强，但计算成本也越高</li>
     *         <li>多语言支持：中英文混合场景效果好</li>
     *         <li>指令前缀：检索时加"为这个句子生成表示以用于检索相关文章："前缀可提升效果</li>
     *       </ul>
     *   </li>
     *   <li><b>MetadataMode.EMBED</b>：只对文档正文做向量化，元数据（如 documentId、title）不参与，
     *       避免元数据噪声影响向量质量</li>
     * </ul>
     *
     * <p><b>【技术关联】向量空间一致性</b>：
     * 知识库写入（ai-cs-knowledge）和对话检索（ai-cs-chat）必须使用同一个 EmbeddingModel，
     * 否则同一文本在两个模块生成的向量不同，余弦相似度计算无意义。
     * 本项目通过 @Primary + Nacos 统一配置保证一致性。</p>
     *
     * @return 硅基流动 BAAI/bge-m3 EmbeddingModel Bean
     */
    @Bean
    @Primary   // 优先注入本 Bean（向量库/检索统一用 bge-m3，保证向量空间一致）
    public EmbeddingModel siliconFlowEmbeddingModel(CacheProperties cacheProperties,
                                                    ObjectProvider<VectorCacheStore> vectorCacheStoreProvider) {
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
        EmbeddingModel raw = new OpenAiEmbeddingModel(embeddingApi, MetadataMode.EMBED,
                OpenAiEmbeddingOptions.builder().model(embeddingModel).build());

        // ===== 缓存层：向量缓存（装饰器包装，相同文本的向量不再重复调用向量化 API） =====
        // 学习点：在 EmbeddingModel 层做缓存而非改 VectorStore——ChromaVectorStore 的
        // add/similaritySearch 内部都会经由 EmbeddingModel.call() 取向量，装饰器让
        // 入库、检索、语义缓存取向量三个调用点零改动共享同一份缓存
        if (!cacheProperties.getVector().isEnabled()) {
            return raw;
        }
        VectorCacheStore cacheStore = vectorCacheStoreProvider.getIfAvailable();
        if (cacheStore == null) {
            return raw;
        }
        log.info("向量缓存已启用: L1容量={}, L2 TTL={}h", cacheProperties.getVector().getL1MaxEntries(),
                cacheProperties.getVector().getTtlHours());
        // 缓存键以模型名做命名空间：换模型后向量空间不同，旧缓存必须失效
        return new CachingEmbeddingModel(raw, cacheStore, embeddingModel);
    }

    /**
     * 注册 ToolCallbackProvider，用于注册 @Tool 注解的方法。
     *
     * <p>把 {@link OrderQueryService}（订单查询）与 {@link Nl2SqlQueryService}
     * （AI 智能问数）作为工具对象注册，Spring AI 会扫描其上
     * {@link org.springframework.ai.tool.annotation.Tool} 注解的方法，
     * 包装成 {@code ToolCallback} 暴露给 LLM；当 LLM 决定调用工具时，会回调到这些方法。</p>
     *
     * <p>【工具调用标注】这里是 toolcall 的注册点；挂载点在 ChatModelRegistry.rebuild()
     * （defaultToolCallbacks）；流式场景下工具的实际执行由 Spring AI 在 .stream() 内部自动完成，
     * 详见 ResilientAiService.callSseStream 注释。</p>
     *
     * @param orderQueryService 订单查询服务（含 {@code @Tool} 方法）
     * @param nl2SqlQueryService AI 智能问数服务（含 {@code @Tool} 方法）
     * @return 工具回调提供者
     */
    @Bean
    public ToolCallbackProvider toolCallbackProvider(OrderQueryService orderQueryService,
                                                     Nl2SqlQueryService nl2SqlQueryService) {
        return MethodToolCallbackProvider.builder()
                .toolObjects(orderQueryService, nl2SqlQueryService)
                .build();
    }

    /**
     * 【AI 核心】注册 RAG 检索增强 Advisor。
     *
     * <p>{@link QuestionAnswerAdvisor} 会在每次 ChatClient 调用前自动：
     * 把用户问题送入 {@link VectorStore} 做相似度检索，命中结果作为上下文拼到 Prompt 中。
     * 这里配置 {@code similarityThreshold=0.3}（较低阈值，宽召回）+ {@code topK=5}。</p>
     *
     * <p><b>【AI 技术详解】RAG（Retrieval-Augmented Generation）检索增强生成</b>：
     * <ul>
     *   <li><b>核心思想</b>：大模型（如 DeepSeek）只知道训练数据中的知识，不知道企业私有数据。
     *       RAG 通过"先检索再生成"的方式，让模型基于检索到的参考资料回答问题</li>
     *   <li><b>完整流程</b>：
     *       <ol>
     *         <li>用户提问 → EmbeddingModel 向量化</li>
     *         <li>在 VectorStore 中做余弦相似度检索，找到 Top-K 相关文档</li>
     *         <li>将检索结果作为【参考资料】注入 Prompt</li>
     *         <li>LLM 基于参考资料生成回答（而非凭空编造）</li>
     *       </ol>
     *   </li>
     *   <li><b>similarityThreshold=0.3</b>：相似度阈值，低于此值的文档不返回。
     *       设置较低（0.3）是为了"宽召回"，宁可多返回一些不太相关的，也不要漏掉相关的</li>
     *   <li><b>topK=5</b>：返回最相关的 5 个文档片段。太多会占用过多 Token，太少可能信息不足</li>
     * </ul>
     *
     * <p><b>【技术关联】Advisor 拦截器模式</b>：
     * Spring AI 的 Advisor 类似 Spring MVC 的 Interceptor，可以在 LLM 调用前后注入逻辑：
     * <ul>
     *   <li><b>QuestionAnswerAdvisor</b>：RAG 检索（调用前注入检索结果）</li>
     *   <li><b>MessageChatMemoryAdvisor</b>：历史记忆管理（自动维护多轮对话上下文）</li>
     *   <li><b>SafeGuardAdvisor</b>：安全过滤（拦截敏感问题）</li>
     * </ul>
     * 多个 Advisor 可以链式组合，形成完整的 AI 处理管线。</p>
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

    // 设计要点：ChatClient 从单一 Bean 改为 Customizer——模型注册表为每个模型构建 ChatClient 时统一注入公共配置，避免多模型配置漂移
    @Bean
    public ChatClientCustomizer chatClientCustomizer(ToolCallbackProvider toolCallbackProvider,
                                                     QuestionAnswerAdvisor ragAdvisor,
                                                     com.aics.chat.prompt.PromptRegistry promptRegistry) {
        // 默认系统提示词外置到 application-prompt.yml（scenario=default-system），DB_SCHEMA 作为 {{dbSchema}} 注入
        String defaultSystem = promptRegistry
                .render("default-system", java.util.Map.of("dbSchema", DB_SCHEMA))
                .getSystem();
        return builder -> builder
                .defaultSystem(defaultSystem)
                // 设计要点：工具回调不在此统一挂载——是否具备 TOOL_CALLING 能力由模型定义决定，注册表按能力条件装配
                .defaultAdvisors(ragAdvisor);
    }
}
