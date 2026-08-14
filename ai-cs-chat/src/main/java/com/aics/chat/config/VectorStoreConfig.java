package com.aics.chat.config;

import org.springframework.context.annotation.Configuration;

/**
 * 向量存储（VectorStore）配置 —— RAG 检索增强生成的核心存储层。
 *
 * <p>作用：把知识库文档转成向量后存进向量库，提问时在向量库里做"语义相似度检索"，
 * 找出与问题最相关的文档片段，作为上下文喂给大模型，从而让模型回答"它本来不知道"的私有知识。</p>
 *
 * <h3>当前实现（Chroma，持久化向量库）</h3>
 * <pre>
 *   EmbeddingModel → 由 ai-cs-common 提供：
 *                     - aics.ai.embedding.provider=local（默认） → HashEmbeddingModel（零依赖哈希向量）
 *                     - aics.ai.embedding.provider=openai       → OpenAI 兼容向量（需配 spring.ai.openai.*）
 *   VectorStore    → ChromaVectorStore（由 spring-ai-starter-vector-store-chroma 自动装配）
 *                    连接配置见 application.yml 的 spring.ai.vectorstore.chroma.*
 * </pre>
 *
 * <p>本类不声明任何 @Bean：Chroma 的 VectorStore 由 starter 依据
 * {@code spring.ai.vectorstore.chroma.*} 自动装配，业务层只需注入 {@code VectorStore} 接口即可。</p>
 *
 * <h3>切换为其他向量库</h3>
 * <p>当前用 Chroma 实现 {@code VectorStore} 接口。若要换回内存版（本地零依赖快速验证）或
 * 切换 Elasticsearch / Milvus / Qdrant / Redis / PGVector 等，只需：</p>
 * <pre>
 * 1. pom.xml 替换/增加对应 starter 依赖（如 spring-ai-elasticsearch-store-spring-boot-starter）
 * 2. application.yml 配置对应连接参数
 * 3. 业务代码（KnowledgeBaseService / SpringAiConfig）一行不改，因为它们只依赖 VectorStore 接口
 * </pre>
 *
 * <h3>启动 Chroma（本地开发）</h3>
 * <pre>
 * 方式一：Docker
 *   docker run -d --name chroma -p 8000:8000 chromadb/chroma
 * 方式二：Python
 *   pip install chromadb && chroma run --host 0.0.0.0 --port 8000
 * </pre>
 */
@Configuration
public class VectorStoreConfig {
    // 无需手动创建 Bean：spring-ai-starter-vector-store-chroma 已自动装配 ChromaVectorStore
}
