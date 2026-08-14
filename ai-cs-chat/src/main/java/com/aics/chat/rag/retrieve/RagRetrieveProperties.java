package com.aics.chat.rag.retrieve;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * 对话侧检索配置。
 */
@Data
@ConfigurationProperties(prefix = "aics.rag")
public class RagRetrieveProperties {

    /** 对话侧 Hybrid 全局开关（false 时即使请求 hybrid=true 也强制纯向量） */
    private boolean hybridEnabled = false;

    /** 查询改写/HyDE 开关 */
    private boolean rewriteEnabled = false;

    /** 图谱开关 */
    private boolean graphEnabled = false;

    /** RRF 平滑常数 */
    private int rrfK = 60;
}