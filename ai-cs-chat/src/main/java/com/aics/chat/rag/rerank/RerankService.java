package com.aics.chat.rag.rerank;

import org.springframework.ai.document.Document;
import reactor.core.publisher.Mono;

import java.util.List;

/**
 * Rerank（重排序）服务接口
 *
 * <p>对第一阶段向量检索的粗召回结果做精排，按相关度分数降序返回。
 * 失败/降级时返回空 Mono（调用方 {@code block()} 得到 {@code null}，回退为向量相似度排序）。</p>
 */
public interface RerankService {

    /**
     * 对粗召回文档执行重排序。
     *
     * @param query     用户问题
     * @param documents 粗召回的文档列表
     * @param topN      重排序后返回的条数（实际以配置的 topN/minScore 为准）
     * @return 按相关度分数降序的重排序结果；异常/无 API Key 时为 empty（block 得到 null）
     */
    Mono<List<RerankResultItem>> rerank(String query, List<Document> documents, int topN);
}
