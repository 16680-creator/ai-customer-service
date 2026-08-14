package com.aics.chat.observability;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * LLM 可观测性配置项（前缀 aics.observability）
 *
 * <p>对应 docs/15 第 3.3 节「LLM 可观测性」：
 * <ul>
 *   <li>{@code enabled}：总开关，关闭后不采集任何 trace 数据；</li>
 *   <li>{@code sample-rate}：采样率（0~1），按请求抽取采集，控制存储与开销；</li>
 *   <li>{@code otlp-endpoint}：OTLP 兼容追踪后端地址（Langfuse/Phoenix），
 *       配置后才创建 OtlpHttpSpanExporter，未配置时降级为结构化日志导出；</li>
 *   <li>{@code log-export}：是否同时输出结构化日志（默认 true，OTLP 未配置时的导出通道）。</li>
 * </ul>
 * </p>
 */
@Getter
@Setter
@Component
@ConfigurationProperties(prefix = "aics.observability")
public class ObservabilityProperties {

    /** 观测总开关（默认开启） */
    private boolean enabled = true;

    /** 采样率 0~1（默认 1.0 全量采集；生产可调低） */
    private double sampleRate = 1.0;

    /** OTLP 兼容后端端点（空=不启用 OTLP 导出，走日志导出） */
    private String otlpEndpoint = "";

    /** 是否输出结构化日志（默认 true） */
    private boolean logExport = true;
}
