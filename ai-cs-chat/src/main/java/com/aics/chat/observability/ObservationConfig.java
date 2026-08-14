package com.aics.chat.observability;

import io.micrometer.observation.ObservationHandler;
import io.micrometer.observation.ObservationRegistry;
import io.opentelemetry.api.trace.Tracer;
import io.opentelemetry.exporter.otlp.http.trace.OtlpHttpSpanExporter;
import io.opentelemetry.sdk.OpenTelemetrySdk;
import io.opentelemetry.sdk.trace.SdkTracerProvider;
import io.opentelemetry.sdk.trace.export.BatchSpanProcessor;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.util.StringUtils;

/**
 * 可观测性配置：注册 ObservationRegistry 与 ObservationHandler，条件装配 OTLP 导出器。
 *
 * <p>设计（见 design.md D1）：
 * <ul>
 *   <li><b>ObservationRegistry</b>：Micrometer Observation 统一埋点入口，注册两个 handler：</li>
 *   <li><b>TraceSpanObservationHandler</b>：组装 span 到当前 TraceContext（始终注册）；</li>
 *   <li><b>OtlpObservationHandler + OtlpHttpSpanExporter</b>：仅在配置
 *       {@code aics.observability.otlp-endpoint} 时创建（{@code @ConditionalOnProperty}），
 *       未配置端点时不初始化 exporter，trace 降级为结构化日志导出。</li>
 * </ul>
 * 依赖说明：micrometer-tracing-bridge-otel + opentelemetry-exporter-otlp 版本由
 * spring-boot-dependencies BOM 统一管理（Boot 3.2.5：micrometer-tracing 1.2.5 / opentelemetry 1.31.0）。</p>
 */
@Configuration
@EnableConfigurationProperties(ObservabilityProperties.class)
public class ObservationConfig {

    /**
     * 注册 ObservationRegistry 并挂载 handler。
     * 若 Spring Boot 已自动装配（micrometer-observation 在 classpath 时自动装配），
     * 此处不重复创建（@ConditionalOnMissingBean 语义由 Boot 自动装配保证，见依赖说明）。
     */
    @Bean
    public ObservationRegistry observationRegistry(ObservabilityProperties properties,
                                                   ObjectProvider<Tracer> tracerProvider) {
        ObservationRegistry registry = ObservationRegistry.create();
        // 1. 组装 span 到 TraceContext（始终生效）
        // 学习点：ObservationHandler 通过 observationConfig().observationHandler() 注册，
        // 埋点方只依赖 ObservationRegistry 接口，具体消费方（TraceContext 组装 / OTLP 导出）
        // 由装配期决定——这是"依赖倒置"在可观测性上的典型应用
        registry.observationConfig().observationHandler(new TraceSpanObservationHandler(properties));
        // 2. OTLP 导出（仅在配置了 otlp-endpoint 且 Tracer Bean 存在时生效）
        // ObjectProvider.getIfAvailable()：找不到 Bean 返回 null 而非抛异常，
        // 实现"可选依赖"——未配置 OTLP 端点时 classpath 里有 OTel 依赖也不初始化任何导出链路
        Tracer tracer = tracerProvider.getIfAvailable();
        if (tracer != null) {
            registry.observationConfig().observationHandler(new OtlpObservationHandler(tracer));
        }
        return registry;
    }

    /**
     * OTLP 追踪后端导出器：仅在配置 {@code aics.observability.otlp-endpoint} 时创建。
     * 未配置端点时不初始化 exporter（classpath 依赖存在但无 Bean，零资源占用）。
     *
     * <p>学习点：@ConditionalOnProperty 是 Spring Boot 的"配置驱动装配"——同一份代码
     * 在不同部署形态下自动选择 Bean 图：本地开发零配置走日志导出，生产配置端点后
     * 自动切换 OTLP 导出，无需改业务代码。这是 12-factor 中"配置与代码分离"的落地。</p>
     */
    @Bean
    @ConditionalOnProperty(prefix = "aics.observability", name = "otlp-endpoint")
    public Tracer otelTracer(ObservabilityProperties properties) {
        String endpoint = properties.getOtlpEndpoint();
        if (!StringUtils.hasText(endpoint)) {
            // 防御：配置为空时也不创建（条件注解已保证非空，此处兜底）
            return null;
        }
        // 学习点：OpenTelemetry SDK 的标准装配——OtlpHttpSpanExporter 负责把 SpanData
        // 序列化为 OTLP/gRPC 或 HTTP/protobuf 发往后端；BatchSpanProcessor 是"批量导出器"：
        // 攒够一批或到时间窗才导出，避免每次 span 都发一次 HTTP 请求的高开销
        OtlpHttpSpanExporter exporter = OtlpHttpSpanExporter.builder()
                .setEndpoint(endpoint)
                .build();
        SdkTracerProvider tracerProvider = SdkTracerProvider.builder()
                .addSpanProcessor(BatchSpanProcessor.builder(exporter).build())
                .build();
        OpenTelemetrySdk sdk = OpenTelemetrySdk.builder()
                .setTracerProvider(tracerProvider)
                .build();
        // Tracer 是"生成 span 的工厂"：ai-cs-chat 作为 service name 标识来源服务，
        // Langfuse/Phoenix 等后端据此区分不同服务的 trace
        return sdk.getTracer("ai-cs-chat");
    }
}
