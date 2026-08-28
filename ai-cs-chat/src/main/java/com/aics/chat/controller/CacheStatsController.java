package com.aics.chat.controller;

import com.aics.chat.cache.CacheProperties;
import com.aics.chat.cache.HotQaCacheService;
import com.aics.chat.cache.SemanticCacheService;
import com.aics.common.result.Result;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 缓存层可观测端点：查看语义缓存条目数与热门问答 Top-N。
 *
 * <p>网关路由 /api/chat/** → stripPrefix(1) → /chat/cache/stats。</p>
 */
@Slf4j
@RestController
@RequestMapping("/chat/cache")
@RequiredArgsConstructor
public class CacheStatsController {

    private final CacheProperties cacheProperties;
    private final SemanticCacheService semanticCacheService;
    private final HotQaCacheService hotQaCacheService;

    /**
     * 缓存层状态：{@code GET /chat/cache/stats?knowledgeBase=knowledge&topN=10}
     */
    @GetMapping("/stats")
    public Result<Map<String, Object>> stats(
            @RequestParam(defaultValue = "knowledge") String knowledgeBase,
            @RequestParam(defaultValue = "10") int topN) {
        String namespace = knowledgeBase + ":user:" +
                (com.aics.chat.util.ChatUserContext.getUserId() == null
                        ? "anonymous" : com.aics.chat.util.ChatUserContext.getUserId());
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("semantic", Map.of(
                "enabled", cacheProperties.getSemantic().isEnabled(),
                "threshold", cacheProperties.getSemantic().getThreshold(),
                "maxEntries", cacheProperties.getSemantic().getMaxEntries(),
                "entries", semanticCacheService.entryCount(namespace)));
        data.put("hotQa", Map.of(
                "enabled", cacheProperties.getHotQa().isEnabled(),
                "promoteThreshold", cacheProperties.getHotQa().getPromoteThreshold(),
                "top", topQuestionsSafe(namespace, topN)));
        return Result.success(data);
    }

    private List<Map<String, Object>> topQuestionsSafe(String knowledgeBase, int topN) {
        try {
            return hotQaCacheService.topQuestions(knowledgeBase, topN);
        } catch (Exception e) {
            log.warn("热门问答 Top-N 查询失败: kb={}, err={}", knowledgeBase, e.getMessage());
            return List.of();
        }
    }
}
