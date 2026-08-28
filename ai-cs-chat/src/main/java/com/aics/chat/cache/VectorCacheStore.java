package com.aics.chat.cache;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Duration;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * 向量缓存存储（向量缓存层的物理实现）：L1 进程内 LRU + L2 Redis。
 *
 * <p>相同文本的 Embedding 结果不再重复调用向量化 API——
 * 典型收益：同一问题反复追问、语义缓存与 RAG 检索对同一问题各取一次向量、
 * 多实例间共享热点问题的向量。</p>
 *
 * <h3>学习点：两级缓存（L1 + L2）</h3>
 * <ul>
 *   <li><b>L1（进程内 LRU LinkedHashMap）</b>：纳秒级命中、无网络开销，但实例间不共享、
 *       容量受堆内存约束——吃掉的是"最热"那一部分流量。</li>
 *   <li><b>L2（Redis）</b>：毫秒级命中、多实例共享、可持久化——吃掉 L1 未命中的部分，
 *       兜住跨实例与跨请求的重复计算。</li>
 *   <li><b>读路径</b>：L1 → L2 → 回填 L1；<b>写路径</b>：双写（L1 常驻、L2 带 TTL）。</li>
 *   <li><b>降级</b>：Redis 不可用时仅告警并退化为 L1-only，不阻断检索链路。</li>
 * </ul>
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class VectorCacheStore {

    private static final String KEY_PREFIX = "aics:veccache:";

    private final StringRedisTemplate redisTemplate;
    private final ObjectMapper objectMapper;
    private final CacheProperties properties;

    /**
     * L1 进程内缓存：accessOrder=true 的 LinkedHashMap 即 LRU（get/put 都算访问），
     * 超过容量淘汰最久未访问条目；Collections.synchronizedMap 保证线程安全。
     */
    private final Map<String, float[]> l1Cache = Collections.synchronizedMap(
            new LinkedHashMap<>(128, 0.75f, true) {
                @Override
                protected boolean removeEldestEntry(Map.Entry<String, float[]> eldest) {
                    return size() > Math.max(1, properties.getVector().getL1MaxEntries());
                }
            });

    /**
     * 查询文本向量：L1 命中直接返回；L2（Redis）命中回填 L1；均未命中返回 null。
     */
    public float[] get(String modelKey, String text) {
        String key = cacheKey(modelKey, text);
        float[] cached = l1Cache.get(key);
        if (cached != null) {
            return cached;
        }
        try {
            String json = redisTemplate.opsForValue().get(KEY_PREFIX + key);
            if (json == null) {
                return null;
            }
            float[] vector = objectMapper.readValue(json, float[].class);
            l1Cache.put(key, vector);
            return vector;
        } catch (Exception e) {
            // Redis 不可用/数据损坏：降级为 L1-only，不阻断检索
            log.warn("向量缓存 L2 读取失败，降级 L1-only: err={}", e.getMessage());
            return null;
        }
    }

    /**
     * 写入文本向量：L1 常驻 + L2 带 TTL；Redis 失败静默降级。
     */
    public void put(String modelKey, String text, float[] vector) {
        if (vector == null || vector.length == 0) {
            return;
        }
        String key = cacheKey(modelKey, text);
        l1Cache.put(key, vector);
        try {
            String json = objectMapper.writeValueAsString(vector);
            redisTemplate.opsForValue().set(KEY_PREFIX + key, json,
                    Duration.ofHours(Math.max(1, properties.getVector().getTtlHours())));
        } catch (Exception e) {
            log.warn("向量缓存 L2 写入失败（不影响业务）: err={}", e.getMessage());
        }
    }

    /**
     * 缓存键：模型名做命名空间（不同模型的向量不可互用）+ 文本 SHA-256（定长、防超长 key）。
     */
    private String cacheKey(String modelKey, String text) {
        return modelKey + ":" + sha256Hex(text);
    }

    private String sha256Hex(String text) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] bytes = digest.digest((text == null ? "" : text).getBytes(StandardCharsets.UTF_8));
            StringBuilder hex = new StringBuilder(bytes.length * 2);
            for (byte b : bytes) {
                hex.append(Character.forDigit((b >> 4) & 0xF, 16)).append(Character.forDigit(b & 0xF, 16));
            }
            return hex.toString();
        } catch (Exception e) {
            // SHA-256 属 JDK 必带算法，理论不可达；兜底用 hashCode 避免向上抛
            return "fallback" + String.valueOf((text == null ? "" : text).hashCode());
        }
    }
}
