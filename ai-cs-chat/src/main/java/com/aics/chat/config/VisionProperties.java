package com.aics.chat.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * 多模态视觉模型配置属性（Nacos 前缀 {@code aics.vision.*}）。
 *
 * <p>视觉模型与 DeepSeek 文本模型解耦：DeepSeek 不支持视觉，故"看图"走硅基流动
 * 多模态模型（OpenAI 兼容协议），"回答"走 DeepSeek，两者配置相互独立。</p>
 */
@Data
@ConfigurationProperties(prefix = "aics.vision")
public class VisionProperties {

    /** 视觉模型端点（OpenAI 兼容协议） */
    private String baseUrl = "https://api.siliconflow.cn";

    /** 视觉模型 API Key（Nacos 管理，禁止硬编码） */
    private String apiKey = "";

    /** 视觉模型名（硅基流动多模态模型） */
    private String model = "Qwen/Qwen3-VL-32B-Instruct";

    /** 视觉能力总开关（false 时图片对话全部降级） */
    private boolean enabled = true;

    /** 图片 URL 白名单主机（SSRF 防护，逗号分隔；为空时拒绝所有外网地址） */
    private String allowedImageHost = "";
}
