package com.aics.chat.cache;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.ai.embedding.EmbeddingModel;
import org.springframework.ai.embedding.EmbeddingRequest;
import org.springframework.ai.embedding.EmbeddingResponse;

import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * 向量缓存装饰器测试：命中缓存的文本不再调用真实 Embedding 模型。
 */
class CachingEmbeddingModelTest {

    private static final String MODEL = "BAAI/bge-m3";

    /** 真实模型的替身：每次调用 +1，返回固定可区分的向量 */
    private final AtomicInteger delegateCalls = new AtomicInteger();
    private EmbeddingModel delegate;

    /** VectorCacheStore 替身：内存 Map 模拟 L1 */
    private final java.util.Map<String, float[]> fakeCache = new java.util.HashMap<>();
    private VectorCacheStore cacheStore;

    private CachingEmbeddingModel cachingModel;

    @BeforeEach
    void setUp() {
        delegateCalls.set(0);
        delegate = new EmbeddingModel() {
            @Override
            public EmbeddingResponse call(EmbeddingRequest request) {
                delegateCalls.incrementAndGet();
                List<org.springframework.ai.embedding.Embedding> embeddings = request.getInstructions().stream()
                        .map(text -> new org.springframework.ai.embedding.Embedding(
                                new float[]{text.length(), 1f}, 0))
                        .toList();
                return new EmbeddingResponse(embeddings);
            }

            @Override
            public float[] embed(org.springframework.ai.document.Document document) {
                return embed(document.getText());
            }
        };
        cacheStore = new VectorCacheStore(null, null, new CacheProperties()) {
            @Override
            public float[] get(String modelKey, String text) {
                return fakeCache.get(modelKey + ":" + text);
            }

            @Override
            public void put(String modelKey, String text, float[] vector) {
                fakeCache.put(modelKey + ":" + text, vector);
            }
        };
        cachingModel = new CachingEmbeddingModel(delegate, cacheStore, MODEL);
    }

    @Test
    void 相同文本第二次调用不再访问真实模型() {
        float[] first = cachingModel.embed("如何退款");
        assertEquals(1, delegateCalls.get(), "首次调用应访问真实模型");

        float[] second = cachingModel.embed("如何退款");
        assertEquals(1, delegateCalls.get(), "相同文本应命中缓存，不再调用真实模型");
        assertArrayEquals(first, second);
    }

    @Test
    void 批量请求只补算未命中的子集() {
        // 预热一条
        cachingModel.embed("A");

        List<float[]> result = cachingModel.embed(List.of("A", "B", "A"));
        assertEquals(2, delegateCalls.get(), "批量中仅未命中的 B 需要真实模型（A 复用缓存）");
        // 未命中子集向量来自真实模型：B.length=1
        assertEquals(1f, result.get(1)[0], 1e-6);
        // 命中子集与首次结果一致
        assertArrayEquals(cachingModel.embed("A"), result.get(0));
    }

    @Test
    void 不同文本不共享缓存() {
        cachingModel.embed("A");
        cachingModel.embed("B");
        assertEquals(2, delegateCalls.get(), "不同文本各自调用真实模型");
    }

    @Test
    void 缓存键带模型命名空间() {
        CachingEmbeddingModel otherModel = new CachingEmbeddingModel(delegate, cacheStore, "other-model");
        cachingModel.embed("A");
        otherModel.embed("A");
        assertEquals(2, delegateCalls.get(), "不同模型的缓存必须隔离（向量空间不同）");
    }
}
