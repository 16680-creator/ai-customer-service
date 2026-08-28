package com.aics.chat.cache;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * 缓存层配置（RAG 三级缓存：向量缓存 / 语义缓存 / 热门问答缓存）。
 *
 * <p>配置前缀 {@code aics.cache.*}，生产环境可放到 Nacos（ai-cs-chat.yml）动态调整。</p>
 */
@Data
@Component
@ConfigurationProperties(prefix = "aics.cache")
public class CacheProperties {

    /** 语义缓存：相似问题直接返回缓存回答 */
    private Semantic semantic = new Semantic();

    /** 热门问答缓存：高频问题精确命中 */
    private HotQa hotQa = new HotQa();

    /** 向量缓存：相同文本的 Embedding 结果去重计算 */
    private Vector vector = new Vector();

    @Data
    public static class Semantic {
        /** 是否启用语义缓存 */
        private boolean enabled = true;
        /** 命中阈值：问题向量与缓存问题的余弦相似度 ≥ 该值才视为"相似问题" */
        private double threshold = 0.92;
        /** 每个知识库最多缓存的条数（超出按最近命中时间淘汰最旧条目） */
        private int maxEntries = 200;
        /** 缓存条目 TTL（小时），每次写入刷新 */
        private int ttlHours = 168;
    }

    @Data
    public static class HotQa {
        /** 是否启用热门问答缓存 */
        private boolean enabled = true;
        /** 同一问题（归一化后）出现次数达到该值，即把回答提升为热门问答缓存 */
        private int promoteThreshold = 3;
        /** 热门问答 TTL（小时），每次写入/命中刷新频次键 */
        private int ttlHours = 24;
    }

    @Data
    public static class Vector {
        /** 是否启用向量缓存（包装 EmbeddingModel，相同文本不再重复调用向量化 API） */
        private boolean enabled = true;
        /** 进程内 L1 缓存最大条目数 */
        private int l1MaxEntries = 4096;
        /** Redis L2 缓存 TTL（小时） */
        private int ttlHours = 168;
    }
}
