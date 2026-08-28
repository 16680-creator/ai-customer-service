package com.aics.chat.cache;

import java.util.Locale;

/**
 * 缓存层通用数学/文本工具（纯静态，便于单元测试）。
 */
public final class EmbeddingMath {

    private EmbeddingMath() {
    }

    /**
     * 余弦相似度：衡量两个向量的方向一致性，取值 [-1, 1]，越接近 1 越相似。
     *
     * <p>维度不一致或任一向量零模长时返回 0（无相似性可用）。</p>
     */
    public static double cosine(float[] a, float[] b) {
        if (a == null || b == null || a.length != b.length || a.length == 0) {
            return 0;
        }
        double dot = 0;
        double normA = 0;
        double normB = 0;
        for (int i = 0; i < a.length; i++) {
            dot += (double) a[i] * b[i];
            normA += (double) a[i] * a[i];
            normB += (double) b[i] * b[i];
        }
        if (normA == 0 || normB == 0) {
            return 0;
        }
        return dot / (Math.sqrt(normA) * Math.sqrt(normB));
    }

    /**
     * 问题归一化：小写 + 去掉空白与标点，只保留字母/数字/中日韩文字。
     *
     * <p>用于热门问答缓存的精确匹配键——"如何退款？"与"如何退款"应视为同一问题。</p>
     */
    public static String normalizeQuestion(String question) {
        if (question == null) {
            return "";
        }
        return question.toLowerCase(Locale.ROOT)
                .replaceAll("[^\\p{IsAlphabetic}\\p{IsDigit}]", "");
    }
}
