package com.aics.common.ai.embedding;

import org.springframework.ai.document.Document;
import org.springframework.ai.embedding.Embedding;
import org.springframework.ai.embedding.EmbeddingModel;
import org.springframework.ai.embedding.EmbeddingRequest;
import org.springframework.ai.embedding.EmbeddingResponse;
import org.springframework.ai.embedding.EmbeddingOptions;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * 本地哈希向量 Embedding（零依赖，用于开发/测试环境跑通检索链路）。
 * 提取英文单词与中文二元字符组，经双哈希映射到固定维度向量，L2 归一化。
 * 生产环境可替换为 Ollama / MiniMax / OpenAI 的 EmbeddingModel Bean。
 */
public class HashEmbeddingModel implements EmbeddingModel {

    public static final int DIMENSIONS = 768;

    @Override
    public float[] embed(Document document) {
        return embed(document.getText());
    }

    @Override
    public float[] embed(String text) {
        float[] vector = new float[DIMENSIONS];
        for (String token : extractTokens(text)) {
            int h1 = Math.floorMod(token.hashCode(), DIMENSIONS);
            int h2 = Math.floorMod(fnv1a(token), DIMENSIONS);
            vector[h1] += 1.0f;
            vector[h2] += 1.0f;
        }
        normalize(vector);
        return vector;
    }

    @Override
    public List<float[]> embed(List<String> texts) {
        List<float[]> result = new ArrayList<>(texts.size());
        for (String text : texts) {
            result.add(embed(text));
        }
        return result;
    }

    @Override
    public EmbeddingResponse call(EmbeddingRequest request) {
        List<String> inputs = request.getInstructions();
        List<Embedding> embeddings = new ArrayList<>(inputs.size());
        List<float[]> vectors = embed(inputs);
        for (int i = 0; i < vectors.size(); i++) {
            embeddings.add(new Embedding(vectors.get(i), i));
        }
        return new EmbeddingResponse(embeddings);
    }

    @Override
    public int dimensions() {
        return DIMENSIONS;
    }

    private List<String> extractTokens(String text) {
        List<String> tokens = new ArrayList<>();
        if (text == null || text.isBlank()) {
            return tokens;
        }
        String normalized = text.toLowerCase(Locale.ROOT).trim();
        // 英文单词
        String[] words = normalized.split("[^a-z0-9]+");
        for (String word : words) {
            if (word.length() >= 2) {
                tokens.add("w:" + word);
            }
        }
        // 中文二元字符组
        char[] chars = normalized.replaceAll("[^\\u4e00-\\u9fa5]", "").toCharArray();
        for (int i = 0; i < chars.length - 1; i++) {
            tokens.add("c:" + chars[i] + chars[i + 1]);
        }
        return tokens;
    }

    private long fnv1a(String s) {
        long hash = 0xcbf29ce484222325L;
        for (int i = 0; i < s.length(); i++) {
            hash ^= s.charAt(i);
            hash *= 0x100000001b3L;
        }
        return hash;
    }

    private void normalize(float[] vector) {
        double sum = 0;
        for (float v : vector) {
            sum += v * v;
        }
        if (sum == 0) {
            return;
        }
        float norm = (float) Math.sqrt(sum);
        for (int i = 0; i < vector.length; i++) {
            vector[i] /= norm;
        }
    }
}
