package com.aics.chat.cache;

import com.aics.chat.dto.ChatRagResponseDTO;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.embedding.EmbeddingModel;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

/**
 * 语义缓存：相似问题直接返回缓存回答，跳过"检索 + LLM 生成"整条昂贵链路。
 *
 * <h3>工作原理</h3>
 * <ol>
 *   <li><b>写入</b>：RAG 回答完成后，把问题向量 + 回答 + 引用打包成条目存入
 *       Redis Hash（aics:semcache:entry:{kb}），并在 ZSET（aics:semcache:index:{kb}）
 *       记录条目的最近命中时间（用作 LRU 淘汰依据）；</li>
 *   <li><b>查询</b>：把新问题向量化，与缓存中所有条目的问题向量算余弦相似度，
 *       最高分 ≥ 阈值（默认 0.92）即视为"相似问题"，直接返回缓存回答；</li>
 *   <li><b>淘汰</b>：条目数超过上限（默认 200/知识库）时，按最近命中时间淘汰最旧条目；
 *       整组键带 TTL 兜底。</li>
 * </ol>
 *
 * <h3>学习点：语义缓存与普通缓存的核心差异</h3>
 * <ul>
 *   <li>普通缓存按<b>精确 key</b> 命中（问题文本一字不差）；语义缓存按<b>语义相似</b>命中
 *       （"怎么申请退款"与"退款怎么办理"视为同一问题）——本质是把"相似度判定"前置为缓存查询。</li>
 *   <li>阈值是"省钱 vs 答非所问"的权衡旋钮：0.92 起步偏保守（宁可 miss 不可错答）；
 *       答案高度模板化的场景可放宽到 0.90，开放域建议 ≥0.95。</li>
 *   <li>线性扫描 O(n) 的取舍：n 上限 200 时每次查询 ≈ 200 次点积，微秒级可接受；
 *       规模上万应换成向量库 HNSW 索引（Redis Stack / Chroma 均可），本实现刻意保持零额外依赖。</li>
 *   <li>问题向量化经由 {@link CachingEmbeddingModel}（向量缓存），
 *       相同问题反复查询时连 Embedding 调用都省掉。</li>
 * </ul>
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class SemanticCacheService {

    /** 缓存条目 Hash：field=条目ID，value=条目 JSON（问题+向量+回答） */
    private static final String ENTRY_KEY = "aics:semcache:entry:";
    /** 条目索引 ZSET：member=条目ID，score=最近命中时间戳（LRU 淘汰依据） */
    private static final String INDEX_KEY = "aics:semcache:index:";

    private final StringRedisTemplate redisTemplate;
    private final ObjectMapper objectMapper;
    private final CacheProperties properties;
    private final EmbeddingModel embeddingModel;

    /**
     * 语义缓存查询：问题向量化 → 与缓存条目逐一算余弦相似度 → 最高分达阈值即命中。
     *
     * @return 命中时返回带 cacheHit/cacheSource 标记的回答；未命中或故障返回 empty
     */
    public Optional<ChatRagResponseDTO> lookup(String knowledgeBase, String question) {
        if (!properties.getSemantic().isEnabled()) {
            return Optional.empty();
        }
        try {
            float[] queryVector = embeddingModel.embed(question);
            Map<Object, Object> entries = redisTemplate.opsForHash().entries(ENTRY_KEY + knowledgeBase);
            if (entries.isEmpty()) {
                return Optional.empty();
            }
            double bestScore = -1;
            CachedEntry best = null;
            String bestId = null;
            for (Map.Entry<Object, Object> e : entries.entrySet()) {
                CachedEntry entry = objectMapper.readValue(String.valueOf(e.getValue()), CachedEntry.class);
                if (entry == null || entry.getVector() == null || entry.getAnswer() == null) {
                    continue;
                }
                double score = EmbeddingMath.cosine(queryVector, entry.getVector());
                if (score > bestScore) {
                    bestScore = score;
                    best = entry;
                    bestId = String.valueOf(e.getKey());
                }
            }
            if (best == null || bestScore < properties.getSemantic().getThreshold()) {
                return Optional.empty();
            }
            // 刷新最近命中时间（LRU 依据），失败不影响命中结果
            try {
                redisTemplate.opsForZSet().add(INDEX_KEY + knowledgeBase, bestId, System.currentTimeMillis());
            } catch (Exception ignore) {
                // ignore
            }
            ChatRagResponseDTO answer = best.getAnswer();
            answer.setCacheHit(true).setCacheSource("semantic");
            log.info("语义缓存命中: kb={}, score={}, q={}", knowledgeBase,
                    String.format("%.4f", bestScore), question);
            return Optional.of(answer);
        } catch (Exception e) {
            log.warn("语义缓存读取失败（不影响业务）: kb={}, err={}", knowledgeBase, e.getMessage());
            return Optional.empty();
        }
    }

    /**
     * 写入语义缓存：问题向量 + 回答打包入库，超上限时按最近命中时间淘汰。
     */
    public void put(String knowledgeBase, String question, ChatRagResponseDTO answer) {
        if (!properties.getSemantic().isEnabled()
                || answer == null || answer.getContent() == null || answer.getContent().isBlank()) {
            return;
        }
        try {
            String entryId = UUID.randomUUID().toString();
            float[] vector = embeddingModel.embed(question);
            // 序列化独立副本，不能修改调用方 DTO 的缓存命中标记
            ChatRagResponseDTO stored = new ChatRagResponseDTO()
                    .setContent(answer.getContent())
                    .setCitations(answer.getCitations());
            CachedEntry entry = new CachedEntry();
            entry.setQuestion(question);
            entry.setVector(vector);
            entry.setAnswer(stored);
            String json = objectMapper.writeValueAsString(entry);

            redisTemplate.opsForHash().put(ENTRY_KEY + knowledgeBase, entryId, json);
            redisTemplate.opsForZSet().add(INDEX_KEY + knowledgeBase, entryId, System.currentTimeMillis());
            Duration ttl = Duration.ofHours(Math.max(1, properties.getSemantic().getTtlHours()));
            redisTemplate.expire(ENTRY_KEY + knowledgeBase, ttl);
            redisTemplate.expire(INDEX_KEY + knowledgeBase, ttl);
            evictOverflow(knowledgeBase);
        } catch (Exception e) {
            log.warn("语义缓存写入失败（不影响业务）: kb={}, err={}", knowledgeBase, e.getMessage());
        }
    }

    /**
     * 条目数超过 maxEntries 时，按最近命中时间淘汰最旧条目（ZSET 分数最小者）。
     */
    private void evictOverflow(String knowledgeBase) {
        int maxEntries = Math.max(1, properties.getSemantic().getMaxEntries());
        Long size = redisTemplate.opsForZSet().zCard(INDEX_KEY + knowledgeBase);
        if (size == null || size <= maxEntries) {
            return;
        }
        Set<String> stale = redisTemplate.opsForZSet()
                .range(INDEX_KEY + knowledgeBase, 0, size - maxEntries - 1);
        if (stale == null || stale.isEmpty()) {
            return;
        }
        redisTemplate.opsForZSet().remove(INDEX_KEY + knowledgeBase, stale.toArray());
        redisTemplate.opsForHash().delete(ENTRY_KEY + knowledgeBase, stale.toArray());
        log.info("语义缓存淘汰最旧{}条: kb={}", stale.size(), knowledgeBase);
    }

    /**
     * 当前缓存条目数（运营可观测）。
     */
    public long entryCount(String knowledgeBase) {
        try {
            Long size = redisTemplate.opsForZSet().zCard(INDEX_KEY + knowledgeBase);
            return size == null ? 0 : size;
        } catch (Exception e) {
            return 0;
        }
    }

    /** 缓存条目：问题原文 + 问题向量 + 回答（含引用），随 Hash 一起序列化进 Redis */
    @lombok.Data
    public static class CachedEntry {
        private String question;
        private float[] vector;
        private ChatRagResponseDTO answer;
    }
}
