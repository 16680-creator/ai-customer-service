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
    private boolean enabled = true;
    private int priority = 0;
    private String tier;
    private Set<ModelCapability> capabilities = new HashSet<>();
    private int contextWindow = 32768;
    private long timeoutMs = 30000;
}
