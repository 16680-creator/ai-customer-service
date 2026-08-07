package com.aics.chat.service;

import com.aics.chat.rag.DocumentLoader;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.document.Document;
import org.springframework.ai.transformer.splitter.TokenTextSplitter;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

/**
 * 知识库服务 —— 真正的 RAG（检索增强生成）核心逻辑。
 *
 * <p>把某个知识库的文档切块、向量化后写入 {@link VectorStore}，
 * 提问时通过语义相似度检索出最相关的文档片段，供大模型生成有依据的回答。</p>
 *
 * <h3>RAG 完整链路（两步）</h3>
 * <pre>
 *   【入库】原始文档 ──分词裁剪 TokenTextSplitter──▶ 多个文档片段 ──EmbeddingModel 向量化──▶ 写入 VectorStore
 *   【检索】用户问题 ──EmbeddingModel 向量化──▶ 在 VectorStore 做相似度检索 ──▶ 返回 Top-K 相关片段
 * </pre>
 *
 * <h3>为什么需要对文档做"分块"（Chunking）？</h3>
 * <pre>
 * 1. 大模型上下文有限，不能把整本手册塞进去；
 * 2. 相似度检索是按"片段"匹配的，块越小越精准；
 * 3. 分块后每个片段可独立检索、独立溯源。
 * </pre>
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class KnowledgeBaseService {

    private final VectorStore vectorStore;
    private final DocumentLoader documentLoader;

    /**
     * 将文本内容写入知识库（入库）。
     *
     * @param knowledgeBase 知识库标识（存入 metadata 的 {@code knowledgeBase} 字段，便于按库过滤检索）
     * @param text          原始文本内容
     * @return 写入的分块文档数量
     */
    public int addText(String knowledgeBase, String text) {
        // 1. 分块：把长文本按 token 切分成多个小片段，每个片段独立检索
        List<Document> chunks = new TokenTextSplitter().split(new Document(text));
        return addChunks(knowledgeBase, chunks);
    }

    /**
     * 将上传的文件（PDF/TXT）写入知识库（入库）。
     *
     * @param knowledgeBase 知识库标识
     * @param file          上传的文档文件
     * @return 写入的分块文档数量
     */
    public int addFile(String knowledgeBase, MultipartFile file) {
        // 把 MultipartFile 转成 Spring Resource，交给 DocumentLoader 读取
        Resource resource = file.getResource();
        List<Document> documents = isPdf(file)
                ? documentLoader.loadPdf(resource)
                : documentLoader.loadText(resource);
        return addChunks(knowledgeBase, documents);
    }

    /**
     * 分块并写入向量库（入库的通用实现）。
     *
     * @param knowledgeBase 知识库标识
     * @param documents     读取到的原始文档列表
     * @return 写入的分块数量
     */
    private int addChunks(String knowledgeBase, List<Document> documents) {
        // 1. 分块：每个 Document 再切分为更小的片段，便于精准检索
        List<Document> chunks = new TokenTextSplitter().apply(documents);

        // 2. 给每个块打上知识库归属的 metadata，检索时按 knowledgeBase 过滤，避免跨库串扰
        chunks.forEach(chunk -> chunk.getMetadata().put("knowledgeBase", knowledgeBase));

        // 3. 写入向量库（内部会调用 EmbeddingModel 把文本转成向量后存储）
        vectorStore.add(chunks);
        log.info("知识库[{}]入库完成, 共{}个分块", knowledgeBase, chunks.size());
        return chunks.size();
    }

    /**
     * 语义检索：根据用户问题，在指定知识库中检索最相关的文档片段。
     *
     * @param knowledgeBase 知识库标识（用于过滤）
     * @param query         用户问题
     * @param topK          返回最相似的几条
     * @param threshold     相似度阈值（低于该值视为"无相关内容"，建议 0.5 ~ 0.8）
     * @return 相关文档片段列表（按相似度降序）
     */
    public List<Document> search(String knowledgeBase, String query, int topK, double threshold) {
        // 构造检索请求：query 会被 EmbeddingModel 向量化后做余弦相似度检索
        // bge-m3 检索建议：查询文本加指令前缀，提升语义检索质量
        String retrievalQuery = "为这个句子生成表示以用于检索相关文章：" + query;
        SearchRequest searchRequest = SearchRequest.builder()
                .query(retrievalQuery)
                .topK(topK)
                .similarityThreshold(threshold)
                .filterExpression("knowledgeBase == '" + knowledgeBase + "'") // 只检索当前知识库
                .build();

        List<Document> results = vectorStore.similaritySearch(searchRequest);
        log.info("知识库[{}]检索完成: 命中{}条", knowledgeBase, results.size());
        return results;
    }

    /**
     * 把检索到的文档片段拼装成 AI 可用的上下文文本。
     *
     * @param documents 检索命中的文档片段
     * @return 拼接后的上下文，供注入 Prompt
     */
    public String buildContext(List<Document> documents) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < documents.size(); i++) {
            sb.append("【资料").append(i + 1).append("】").append(documents.get(i).getText()).append("\n\n");
        }
        return sb.toString().trim();
    }

    /**
     * 判断是否 PDF 文件。
     */
    private boolean isPdf(MultipartFile file) {
        String name = file.getOriginalFilename();
        return name != null && name.toLowerCase().endsWith(".pdf");
    }
}