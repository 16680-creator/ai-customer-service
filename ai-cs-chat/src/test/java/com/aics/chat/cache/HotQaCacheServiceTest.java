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
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

/**
 * 热门问答缓存测试：归一化精确命中、频次达标提升、未达标不写回答。
 */
class HotQaCacheServiceTest {

    private static final String KB = "knowledge";
    private static final String ANSWER_KEY = "aics:hotqa:answer:" + KB;

    private final Map<Object, Object> hashStore = new HashMap<>();
    private final Map<String, Double> zsetStore = new HashMap<>();
    private final ObjectMapper objectMapper = new ObjectMapper();
    private HotQaCacheService service;

    @BeforeEach
    @SuppressWarnings("unchecked")
    void setUp() {
        StringRedisTemplate redisTemplate = mock(StringRedisTemplate.class);
        HashOperations<String, Object, Object> hashOps = mock(HashOperations.class);
        ZSetOperations<String, String> zsetOps = mock(ZSetOperations.class);
        lenient().when(redisTemplate.opsForHash()).thenReturn(hashOps);
        lenient().when(redisTemplate.opsForZSet()).thenReturn(zsetOps);

        // Hash：模拟 aics:hotqa:answer:{kb}
        lenient().when(hashOps.get(anyString(), any())).thenAnswer(
                inv -> ANSWER_KEY.equals(inv.getArgument(0)) ? hashStore.get(inv.getArgument(1)) : null);
        lenient().doAnswer(inv -> {
            if (ANSWER_KEY.equals(inv.getArgument(0))) {
                hashStore.put(inv.getArgument(1), inv.getArgument(2));
            }
            return null;
        }).when(hashOps).put(anyString(), any(), any());

        // ZSet：模拟 aics:hotqa:freq:{kb} 的 incrementScore
        lenient().when(zsetOps.incrementScore(anyString(), any(), anyDouble())).thenAnswer(inv -> {
            zsetStore.merge((String) inv.getArgument(1), inv.getArgument(2), Double::sum);
            return zsetStore.get(inv.getArgument(1));
        });
        lenient().when(redisTemplate.expire(anyString(), any())).thenReturn(true);

        CacheProperties properties = new CacheProperties();
        properties.getHotQa().setPromoteThreshold(3);
        service = new HotQaCacheService(redisTemplate, objectMapper, properties);
    }

    @Test
    void 归一化后标点差异仍精确命中() throws Exception {
        // 直接写入一条已提升的热门问答（field 为归一化问题）
        ChatRagResponseDTO answer = new ChatRagResponseDTO().setContent("发货后7天内可无理由退货");
        hashStore.put("如何退货", objectMapper.writeValueAsString(answer));

        // 带 ？ 与空格的问法归一化后命中同一条
        Optional<ChatRagResponseDTO> hit = service.lookup(KB, " 如何退货？");
        assertTrue(hit.isPresent(), "归一化后标点差异应精确命中");
        assertEquals("发货后7天内可无理由退货", hit.get().getContent());
        assertTrue(hit.get().getCacheHit());
        assertEquals("hot-qa", hit.get().getCacheSource());
    }

    @Test
    void 频次未达阈值不写回答缓存() {
        ChatRagResponseDTO answer = new ChatRagResponseDTO().setContent("回答A");
        service.record(KB, "发货时间", answer);
        service.record(KB, "发货时间", answer);   // 共 2 次 < 阈值 3

        assertFalse(service.lookup(KB, "发货时间").isPresent(), "频次不足不得提升为热门问答");
    }

    @Test
    void 频次达阈值自动提升为热门问答() {
        ChatRagResponseDTO answer = new ChatRagResponseDTO().setContent("48小时内发货");
        for (int i = 0; i < 3; i++) {
            service.record(KB, "多久发货", answer);
        }
        Optional<ChatRagResponseDTO> hit = service.lookup(KB, "多久发货？");
        assertTrue(hit.isPresent(), "频次达阈值应提升为热门问答缓存");
        assertEquals("48小时内发货", hit.get().getContent());
    }

    @Test
    void 空回答不记录() {
        service.record(KB, "问题", new ChatRagResponseDTO().setContent(""));
        assertTrue(zsetStore.isEmpty(), "空回答不应计入频次");
    }
}
