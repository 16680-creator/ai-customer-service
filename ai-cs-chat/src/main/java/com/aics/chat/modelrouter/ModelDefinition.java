package com.aics.chat.modelrouter;

import lombok.Getter;
import lombok.Setter;

import java.util.HashSet;
import java.util.Set;

@Getter
@Setter
public class ModelDefinition {
    private String id;
    private String provider;
    private String baseUrl;
    private String apiKey;
    private String model;
    // 设计要点：默认启用 + 能力默认为空——新模型配置只写差异项，避免每个模型重复声明完整字段
    private boolean enabled = true;
    private int priority = 0;
    private String tier;
    private Set<ModelCapability> capabilities = new HashSet<>();
    private int contextWindow = 32768;
    // 学习点：超时配置下沉到模型维度——不同供应商/规格响应特性差异大，全局固定超时会误杀慢模型或放大快模型的故障影响
    private long timeoutMs = 30000;
}
