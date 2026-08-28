package com.aics.chat.cache;

import com.aics.chat.dto.ChatRagResponseDTO;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

/**
 * 热门问答缓存：Redis 记录问题频次，高频问题的回答直接精确命中返回。
 *
 * <h3>工作原理</h3>
 * <ol>
 *   <li><b>频次统计</b>：每次 RAG 回答完成后，把问题归一化（去标点/空白、小写）
 *       作为成员 ZINCRBY 进 ZSET（aics:hotqa:freq:{kb}）；</li>
 *   <li><b>热度提升</b>：频次达到阈值（默认 3 次）时，把当前回答写入 Hash
 *       （aics:hotqa:answer:{kb}，field=归一化问题，value=回答+引用 JSON）；</li>
 *   <li><b>精确命中</b>：后续相同问题先查 Hash，O(1) 直接返回缓存回答，
 *       完全跳过"Embedding → 检索 → LLM 生成"整条链路。</li>
 * </ol>
 *
 * <h3>学习点：热门问答缓存 vs 语义缓存的分工</h3>
 * <ul>
 *   <li>热门问答是<b>精确匹配</b>：一次 HGET 即可判定，成本极低，适合"高频标准问题"；
 *       但改写一个字就 miss。</li>
 *   <li>语义缓存是<b>相似匹配</b>：需要向量相似度计算，成本高一些，但能兜住
 *       "换种问法的同一问题"。</li>
 *   <li>两者按成本升序串联：先查热门问答（O(1)）→ 再查语义缓存（向量比对）→ 都 miss 才走完整 RAG。
 *       与 CPU 缓存的 L1→L2→内存 分级思想一致：越便宜、越精确的判定越先做。</li>
 * </ul>
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class HotQaCacheService {

    /** 问题频次 ZSET：member=归一化问题，score=出现次数 */
    private static final String FREQ_KEY = "aics:hotqa:freq:";
    /** 热门问答 Hash：field=归一化问题，value=回答 JSON */
    private static final String ANSWER_KEY = "aics:hotqa:answer:";

    private final StringRedisTemplate redisTemplate;
    private final ObjectMapper objectMapper;
    private final CacheProperties properties;

    /**
     * 查询热门问答缓存（精确匹配）。
     *
     * @return 命中时返回带 cacheHit/cacheSource 标记的回答；未命中或故障返回 empty
     */
    public Optional<ChatRagResponseDTO> lookup(String knowledgeBase, String question) {
        if (!properties.getHotQa().isEnabled()) {
            return Optional.empty();
        }
        try {
            Object json = redisTemplate.opsForHash().get(ANSWER_KEY + knowledgeBase,
                    EmbeddingMath.normalizeQuestion(question));
            if (json == null) {
                return Optional.empty();
            }
            ChatRagResponseDTO answer = objectMapper.readValue(String.valueOf(json), ChatRagResponseDTO.class);
            if (answer == null || answer.getContent() == null || answer.getContent().isBlank()) {
                return Optional.empty();
            }
            answer.setCacheHit(true).setCacheSource("hot-qa");
            log.info("热门问答缓存命中: kb={}, q={}", knowledgeBase, question);
            return Optional.of(answer);
        } catch (Exception e) {
            // 缓存层任何故障都不阻断业务，退化为正常 RAG 流程
            log.warn("热门问答缓存读取失败（不影响业务）: kb={}, err={}", knowledgeBase, e.getMessage());
            return Optional.empty();
        }
    }

    /**
     * 记录一次问题出现：频次 +1，达到提升阈值时缓存本次回答。
     *
     * @param answer 本次 RAG 生成的回答（仅当作"标准答案"来源，频次不足时不写入）
     */
    public void record(String knowledgeBase, String question, ChatRagResponseDTO answer) {
        if (!properties.getHotQa().isEnabled()
                || answer == null || answer.getContent() == null || answer.getContent().isBlank()) {
            return;
        }
        try {
            String normalized = EmbeddingMath.normalizeQuestion(question);
            if (normalized.isEmpty()) {
                return;
            }
            Double count = redisTemplate.opsForZSet().incrementScore(FREQ_KEY + knowledgeBase, normalized, 1);
            redisTemplate.expire(FREQ_KEY + knowledgeBase,
                    Duration.ofHours(Math.max(1, properties.getHotQa().getTtlHours())));
            if (count != null && count >= properties.getHotQa().getPromoteThreshold()) {
                save(knowledgeBase, normalized, answer);
            }
        } catch (Exception e) {
            log.warn("热门问答频次记录失败（不影响业务）: kb={}, err={}", knowledgeBase, e.getMessage());
        }
    }

    /**
     * 把回答写入热门问答缓存（调用方已保证 content 非空）。
     */
    private void save(String knowledgeBase, String normalizedQuestion, ChatRagResponseDTO answer) throws Exception {
        // 序列化独立副本，不能直接清空调用方 answer 的 cacheHit/cacheSource：
        // 语义缓存命中后触发热门问答提升时，原响应仍应保留 semantic 命中标记
        ChatRagResponseDTO stored = new ChatRagResponseDTO()
                .setContent(answer.getContent())
                .setCitations(answer.getCitations());
        String json = objectMapper.writeValueAsString(stored);
        redisTemplate.opsForHash().put(ANSWER_KEY + knowledgeBase, normalizedQuestion, json);
        redisTemplate.expire(ANSWER_KEY + knowledgeBase,
                Duration.ofHours(Math.max(1, properties.getHotQa().getTtlHours())));
        log.info("热门问答缓存已提升: kb={}, 频次达标 q={}", knowledgeBase, normalizedQuestion);
    }

    /**
     * 查询热门问题 Top-N（运营可观测：当前哪些问题最热、是否已缓存）。
     *
     * @return [{question, count, cached}]，按次数降序
     */
    public java.util.List<Map<String, Object>> topQuestions(String knowledgeBase, int topN) {
        Set<String> members = redisTemplate.opsForZSet().reverseRange(FREQ_KEY + knowledgeBase, 0, Math.max(0, topN - 1));
        java.util.List<Map<String, Object>> result = new java.util.ArrayList<>();
        if (members == null) {
            return result;
        }
        for (String member : members) {
            Double count = redisTemplate.opsForZSet().score(FREQ_KEY + knowledgeBase, member);
            Map<String, Object> item = new LinkedHashMap<>();
            item.put("question", member);
            item.put("count", count == null ? 0 : count.intValue());
            item.put("cached", Boolean.TRUE.equals(
                    redisTemplate.opsForHash().hasKey(ANSWER_KEY + knowledgeBase, member)));
            result.add(item);
        }
        return result;
    }
}
