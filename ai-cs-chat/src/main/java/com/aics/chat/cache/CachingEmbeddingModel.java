package com.aics.chat.cache;

import org.springframework.ai.document.Document;
import org.springframework.ai.embedding.Embedding;
import org.springframework.ai.embedding.EmbeddingModel;
import org.springframework.ai.embedding.EmbeddingRequest;
import org.springframework.ai.embedding.EmbeddingResponse;

import java.util.ArrayList;
import java.util.List;

/**
 * 带缓存的 EmbeddingModel 装饰器（装饰器模式包装真实模型）。
 *
 * <p>只覆写 {@link #call(EmbeddingRequest)} 一个方法——EmbeddingModel 接口的
 * {@code embed(String)} / {@code embed(List<String>)} 等默认方法全部经由 call() 实现，
 * VectorStore（Chroma）入库与检索最终也走 call()，因此单点覆写即可覆盖全部向量化入口：
 * 检索请求中命中的文本不再重复调用真实模型，未命中的批量提交给委托模型并回填缓存。</p>
 *
 * <h3>学习点：为什么用装饰器而不是改 VectorStore</h3>
 * <ul>
 *   <li>Embedding 的调用点藏在 VectorStore 内部（add/similaritySearch），
 *       在模型层包缓存 = 所有调用方零改动获得缓存能力（关注点分离）。</li>
 *   <li>批量请求逐条查缓存、只把"未命中子集"发给真实模型——
 *       这是批量场景下缓存的标准做法（per-item cache-aside）。</li>
 *   <li>缓存键带模型名命名空间：换模型后旧向量不能复用（向量空间不同），必须隔离。</li>
 * </ul>
 */
public class CachingEmbeddingModel implements EmbeddingModel {

    private final EmbeddingModel delegate;
    private final VectorCacheStore cache;
    /** 缓存命名空间：真实 Embedding 模型名（如 BAAI/bge-m3） */
    private final String modelKey;

    public CachingEmbeddingModel(EmbeddingModel delegate, VectorCacheStore cache, String modelKey) {
        this.delegate = delegate;
        this.cache = cache;
        this.modelKey = modelKey;
    }

    @Override
    public EmbeddingResponse call(EmbeddingRequest request) {
        List<String> texts = request.getInstructions();
        float[][] vectors = new float[texts.size()][];

        // 1. 逐条查缓存，收集未命中的下标与文本
        List<String> missTexts = new ArrayList<>();
        List<Integer> missIndexes = new ArrayList<>();
        for (int i = 0; i < texts.size(); i++) {
            float[] cached = cache.get(modelKey, texts.get(i));
            if (cached != null) {
                vectors[i] = cached;
            } else {
                missTexts.add(texts.get(i));
                missIndexes.add(i);
            }
        }
        if (missTexts.isEmpty()) {
            return respond(vectors);
        }

        // 2. 未命中子集批量调用真实模型（保持原始 EmbeddingOptions）
        EmbeddingResponse missResponse = delegate.call(new EmbeddingRequest(missTexts, request.getOptions()));
        List<Embedding> missEmbeddings = missResponse.getResults();

        // 3. 结果按原下标回填 + 写缓存
        for (int j = 0; j < missEmbeddings.size() && j < missIndexes.size(); j++) {
            float[] vector = missEmbeddings.get(j).getOutput();
            vectors[missIndexes.get(j)] = vector;
            cache.put(modelKey, missTexts.get(j), vector);
        }
        return respond(vectors);
    }

    @Override
    public float[] embed(Document document) {
        // 文档入库时保留真实模型对 MetadataMode 的处理口径：缓存键用格式化内容，
        // miss 时委托 delegate.embed(document)，不能退化为 embed(String) 丢失元数据语义
        String cacheText = document.getFormattedContent();
        float[] cached = cache.get(modelKey, cacheText);
        if (cached != null) {
            return cached;
        }
        float[] vector = delegate.embed(document);
        cache.put(modelKey, cacheText, vector);
        return vector;
    }

    private EmbeddingResponse respond(float[][] vectors) {
        List<Embedding> embeddings = new ArrayList<>(vectors.length);
        for (int i = 0; i < vectors.length; i++) {
            embeddings.add(new Embedding(vectors[i], i));
        }
        return new EmbeddingResponse(embeddings);
    }
}
