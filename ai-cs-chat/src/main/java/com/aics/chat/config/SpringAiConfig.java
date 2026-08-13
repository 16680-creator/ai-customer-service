package com.aics.chat.config;

import com.aics.chat.rag.rerank.RerankProperties;
import com.aics.chat.service.OrderQueryService;
import com.aics.chat.nl2sql.Nl2SqlQueryService;
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
 *   <li>{@link #orderToolCallbackProvider(OrderQueryService)}：把 {@link OrderQueryService} 上
 *       {@code @Tool} 标注的方法注册成 LLM 可调用的 Function Tool。</li>
 *   <li>{@link #ragAdvisor(VectorStore)}：{@link QuestionAnswerAdvisor}，Spring AI 内置的 RAG 顾问，
 *       在每次 ChatClient 调用时自动注入向量检索结果作为上下文。</li>
 *   <li>{@link #chatClient(OpenAiChatModel, ToolCallbackProvider, QuestionAnswerAdvisor)}：
 *       {@link ChatClient}，绑定默认系统提示、工具与 RAG Advisor，是业务层 LLM 调用的统一入口。</li>
 * </ul>
 *
 * <h3>【AI 技术】ChatModel 来源（OpenAI 兼容协议对接 DeepSeek）</h3>
 * <p>{@link OpenAiChatModel} 由 Spring AI 的 {@code spring-ai-openai-spring-boot-starter} 自动装配，
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
     * <p>把 {@link OrderQueryService}（订单查询）与 {@link Nl2SqlQueryService}
     * （AI 智能问数）作为工具对象注册，Spring AI 会扫描其上
     * {@link org.springframework.ai.tool.annotation.Tool} 注解的方法，
     * 包装成 {@code ToolCallback} 暴露给 LLM；当 LLM 决定调用工具时，会回调到这些方法。</p>
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

    /**
     * 【AI 核心】注册 ChatClient Bean —— 业务层 LLM 调用的统一入口。
     *
     * <p>ChatClient 是业务层调用 LLM 的统一入口，此处配置三项默认值：</p>
     * <ul>
     *   <li>{@code defaultSystem}：系统提示词，约束 AI 的身份、能力范围与回答风格
     *       （禁止透露底层模型名、识别当前用户、订单查询/智能问数工具调用规则、数据库 schema 等）。</li>
     *   <li>{@code defaultToolCallbacks}：默认携带订单查询 + 智能问数工具回调。</li>
     *   <li>{@code defaultAdvisors}：默认携带 {@link QuestionAnswerAdvisor}，
     *       让所有 ChatClient 调用都自动走 RAG 检索增强。</li>
     * </ul>
     *
     * <p><b>【AI 技术详解】System Prompt（系统提示词）设计</b>：
     * <ul>
     *   <li><b>身份定义</b>：告诉 AI 它是"AI客服平台的智能助手"，而非通用助手</li>
     *   <li><b>能力边界</b>：明确告知 AI 可以做什么（订单查询、智能问数）以及怎么做</li>
     *   <li><b>安全约束</b>：禁止透露底层模型名称（防止用户探测技术栈）</li>
     *   <li><b>回答风格</b>：简洁、准确、有亲和力，适当使用 emoji</li>
     *   <li><b>数据库 Schema</b>：提供完整的表结构，让 AI 知道如何组装 SQL（NL2SQL 场景）</li>
     * </ul>
     *
     * <p><b>【AI 技术详解】Tool Calling（函数调用）机制</b>：
     * <ul>
     *   <li><b>原理</b>：LLM 不直接执行代码，而是输出"我要调用某个工具"的 JSON 指令，
     *       Spring AI 框架拦截该指令，调用对应的 Java 方法，再把结果返回给 LLM</li>
     *   <li><b>流程</b>：
     *       <ol>
     *         <li>用户问"我的订单 ORD001 什么状态？"</li>
     *         <li>LLM 分析后决定调用 {@code queryOrderByOrderId(orderId="ORD001")}</li>
     *         <li>Spring AI 调用 {@link OrderQueryService#queryOrderByOrderId} 方法</li>
     *         <li>方法返回订单详情 JSON</li>
     *         <li>LLM 基于 JSON 生成自然语言回答</li>
     *       </ol>
     *   </li>
     *   <li><b>优势</b>：LLM 负责理解意图和生成回答，代码负责执行逻辑，各司其职</li>
     * </ul>
     *
     * <p><b>【技术关联】ChatClient 与 OpenAiChatModel 的关系</b>：
     * <ul>
     *   <li><b>OpenAiChatModel</b>：底层模型客户端，负责发送 HTTP 请求到 DeepSeek API</li>
     *   <li><b>ChatClient</b>：高层封装，提供流式 API、Advisor 链、Tool 注册等能力</li>
     *   <li>业务代码只接触 ChatClient，不直接使用 OpenAiChatModel（除了摘要等特殊场景）</li>
     * </ul>
     *
     * @param chatModel          OpenAiChatModel（DeepSeek 兼容），由 starter 自动装配
     * @param toolCallbackProvider 工具回调（订单查询 + 智能问数）
     * @param ragAdvisor         RAG 顾问
     * @return 配置好默认值的 ChatClient
     */
    @Bean
    public ChatClient chatClient(OpenAiChatModel chatModel, ToolCallbackProvider toolCallbackProvider,
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
                        - 数据查询（智能问数）：当用户想了解平台数据（如订单统计、商品销量、用户数量、优惠券使用情况等），
                          使用 executeReadOnlyQuery 工具查询数据库。规则：
                          * 必须根据问题涉及的业务先选对 database（user=用户库、product=商品库、order=订单支付库、chat=对话消息库、knowledge=知识库）
                          * 组装合法 SELECT 语句，可带 WHERE / ORDER BY / GROUP BY / 聚合函数（COUNT/SUM/AVG）
                          * 日期字段用 create_time / pay_time 等，订单金额字段用 pay_amount，订单状态用 status 过滤
                          * 查询出数据后用自然语言向用户汇报结论，可附带表格或关键数字
                          * 若多次尝试仍失败（SQL 语法错误/表列名不存在），如实告知用户"暂时无法查询该数据"，不要编造数字
                         
                        回答风格：简洁、准确、有亲和力，适当使用emoji增加友好感。
                        
                        """ + DB_SCHEMA)
                .defaultToolCallbacks(toolCallbackProvider)
                .defaultAdvisors(ragAdvisor)
                .build();
    }
}
