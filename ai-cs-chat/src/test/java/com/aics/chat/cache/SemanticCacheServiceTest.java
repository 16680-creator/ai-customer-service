package com.aics.chat.cache;

import com.aics.chat.dto.ChatRagResponseDTO;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.data.redis.core.HashOperations;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ZSetOperations;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyDouble;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;

/**
 * 语义缓存测试：相似问题命中、不相似问题未命中、低于阈值不返回。
 *
 * <p>Embedding 用 common 模块的 HashEmbeddingModel（确定性向量，相同/相似文本相似度高），
 * Redis 用 Mockito 模拟 Hash/ZSet 结构。</p>
 */
class SemanticCacheServiceTest {

    private static final String KB = "knowledge";
    private static final String ENTRY_KEY = "aics:semcache:entry:" + KB;
    private static final String INDEX_KEY = "aics:semcache:index:" + KB;

    private final Map<Object, Object> hashStore = new HashMap<>();
    private final Map<String, Double> zsetStore = new HashMap<>();
    private final ObjectMapper objectMapper = new ObjectMapper();

    private SemanticCacheService service;

    @BeforeEach
    @SuppressWarnings("unchecked")
    void setUp() {
        StringRedisTemplate redisTemplate = mock(StringRedisTemplate.class);
        HashOperations<String, Object, Object> hashOps = mock(HashOperations.class);
        ZSetOperations<String, String> zsetOps = mock(ZSetOperations.class);
        lenient().when(redisTemplate.opsForHash()).thenReturn(hashOps);
        lenient().when(redisTemplate.opsForZSet()).thenReturn(zsetOps);

        lenient().when(hashOps.entries(anyString())).thenAnswer(inv -> new HashMap<>(hashStore));
        lenient().doAnswer(inv -> {
            hashStore.put(inv.getArgument(1), inv.getArgument(2));
            return null;
        }).when(hashOps).put(anyString(), any(), any());

        lenient().when(zsetOps.add(anyString(), any(), anyDouble()))
                .thenAnswer(inv -> {
                    zsetStore.put(inv.getArgument(1), (Double) inv.getArgument(2));
                    return true;
                });
        lenient().when(zsetOps.zCard(anyString())).thenAnswer(inv -> (long) zsetStore.size());
        lenient().when(zsetOps.range(anyString(), anyLong(), anyLong())).thenAnswer(
                inv -> java.util.Collections.emptySet());
        lenient().when(redisTemplate.expire(anyString(), any())).thenReturn(true);

        CacheProperties properties = new CacheProperties();
        properties.getSemantic().setThreshold(0.92);
        service = new SemanticCacheService(redisTemplate, objectMapper, properties,
                new com.aics.common.ai.embedding.HashEmbeddingModel());
    }

    @Test
    void 相同问题写入后命中语义缓存() {
        ChatRagResponseDTO answer = new ChatRagResponseDTO()
                .setContent("退款将在3个工作日内原路返回").setCitations(java.util.List.of());
        service.put(KB, "如何申请退款？", answer);

        Optional<ChatRagResponseDTO> hit = service.lookup(KB, "如何申请退款？");
        assertTrue(hit.isPresent(), "相同问题应命中语义缓存");
        assertEquals("退款将在3个工作日内原路返回", hit.get().getContent());
        assertTrue(hit.get().getCacheHit(), "命中应带 cacheHit 标记");
        assertEquals("semantic", hit.get().getCacheSource());
    }

    @Test
    void 不相关问题不会误命中() {
        ChatRagResponseDTO answer = new ChatRagResponseDTO().setContent("关于退货政策的回答");
        service.put(KB, "退货政策是什么", answer);

        // 完全不相关的问题：相似度低于阈值 → 未命中
        Optional<ChatRagResponseDTO> miss = service.lookup(KB, "今天天气怎么样适合出游吗");
        assertFalse(miss.isPresent(), "不相关问题不得返回缓存回答");
    }

    @Test
    void 空缓存未命中() {
        assertFalse(service.lookup(KB, "任何问题").isPresent());
    }

    @Test
    void 缓存命中后条目数可见() {
        service.put(KB, "问题一", new ChatRagResponseDTO().setContent("回答一"));
        assertEquals(1, service.entryCount(KB));
    }
}
