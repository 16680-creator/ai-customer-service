package com.aics.chat.service.impl;

import com.aics.chat.config.VisionProperties;
import com.aics.chat.observability.ModelUsageRecorder;
import com.aics.chat.observability.TraceContext;
import com.aics.chat.observability.TraceContextHolder;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import io.github.resilience4j.retry.annotation.Retry;
import io.github.resilience4j.timelimiter.annotation.TimeLimiter;
import io.micrometer.observation.Observation;
import io.micrometer.observation.ObservationRegistry;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.ai.chat.metadata.Usage;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.ai.content.Media;
import org.springframework.ai.openai.OpenAiChatModel;
import org.springframework.ai.openai.OpenAiChatOptions;
import org.springframework.ai.openai.api.OpenAiApi;
import org.springframework.stereotype.Service;
import org.springframework.util.MimeTypeUtils;
import org.springframework.util.StringUtils;

import java.net.URI;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;

/**
 * 视觉模型客户端 —— 封装多模态图片理解调用。
 *
 * <p>为什么独立成组件而非注册 OpenAiChatModel Bean：
 * 容器中已有 DeepSeek 的 {@link OpenAiChatModel}（自动装配），再注册一个同类型的
 * 视觉模型 Bean 会引发类型歧义。故本组件在 {@link PostConstruct} 阶段用
 * {@link VisionProperties} 手动构造视觉模型，与文本模型解耦。</p>
 *
 * <p>弹性治理：复用 Resilience4j（{@code visionService} 实例），
 * 视觉调用失败时降级返回 null，由 {@link VisionChatServiceImpl} 据此退化为纯文本对话。</p>
 *
 * <h3>【AI 技术详解】多模态视觉模型</h3>
 * <ul>
 *   <li><b>模型选型</b>：硅基流动 Qwen2.5-VL-72B-Instruct
 *       <ul>
 *         <li>Qwen2.5：阿里通义千问系列，中英文效果优秀</li>
 *         <li>VL：Vision-Language，视觉-语言多模态模型</li>
 *         <li>72B：720 亿参数，理解能力强</li>
 *         <li>Instruct：指令微调版本，遵循指令能力强</li>
 *       </ul>
 *   </li>
 *   <li><b>为什么不用 GPT-4V</b>：需要海外网络、付费，且中文效果不如 Qwen2.5</li>
 *   <li><b>为什么不用本地部署</b>：72B 模型需要大量 GPU 资源，云端 API 更便捷</li>
 * </ul>
 *
 * <h3>【AI 技术详解】多模态消息构造</h3>
 * <pre>
 *   UserMessage.builder()
 *       .text("请描述这张图片中的关键信息...")  // 文本指令
 *       .media(new Media(mimeType, imageUrl))   // 图片 URL
 *       .build()
 *
 *   // Spring AI 会自动转成 OpenAI 协议格式：
 *   {
 *     "role": "user",
 *     "content": [
 *       {"type": "text", "text": "请描述这张图片中的关键信息..."},
 *       {"type": "image_url", "image_url": {"url": "https://..."}}
 *     ]
 *   }
 * </pre>
 *
 * <h3>【技术关联】视觉模型与文本模型的分离</h3>
 * <ul>
 *   <li><b>视觉模型</b>：Qwen2.5-VL-72B（看图，生成描述）</li>
 *   <li><b>文本模型</b>：DeepSeek-Chat（生成回答）</li>
 *   <li><b>分离原因</b>：DeepSeek 不支持视觉，需要独立的视觉模型</li>
 *   <li><b>Bean 冲突</b>：两个 OpenAiChatModel 类型相同，用 @Primary 会冲突，
 *       故视觉模型手动构造，不注册为 Spring Bean</li>
 * </ul>
 */
@Slf4j
@Service
public class VisionModelClient {

    private final VisionProperties visionProperties;
    private final ObservationRegistry observationRegistry;
    private final ModelUsageRecorder modelUsageRecorder;

    /** 场景：图片理解（vision） */
    private static final String SCENARIO_VISION = "vision";

    /** 视觉模型供应商（硅基流动） */
    private static final String PROVIDER = "siliconflow";

    /** 视觉模型（懒初始化，enabled 且配置 apiKey 时才构造） */
    private volatile OpenAiChatModel visionModel;

    public VisionModelClient(VisionProperties visionProperties,
                             ObservationRegistry observationRegistry,
                             ModelUsageRecorder modelUsageRecorder) {
        this.visionProperties = visionProperties;
        this.observationRegistry = observationRegistry;
        this.modelUsageRecorder = modelUsageRecorder;
    }

    /**
     * 初始化视觉模型：仅在启用且配置了 API Key 时构造，否则图片对话降级。
     */
    @PostConstruct
    void init() {
        if (!visionProperties.isEnabled() || !StringUtils.hasText(visionProperties.getApiKey())) {
            log.warn("视觉模型未启用或未配置 API Key，图片对话将降级为纯文本");
            return;
        }
        OpenAiApi api = OpenAiApi.builder()
                .baseUrl(visionProperties.getBaseUrl())
                .apiKey(visionProperties.getApiKey())
                .build();
        this.visionModel = OpenAiChatModel.builder()
                .openAiApi(api)
                .defaultOptions(OpenAiChatOptions.builder().model(visionProperties.getModel()).build())
                .build();
        log.info("视觉模型已初始化: model={}", visionProperties.getModel());
    }

    /**
     * 【AI 核心】弹性图片理解调用：把图片 URL 作为多模态消息交给视觉模型，返回图片文本描述。
     *
     * <p><b>【AI 技术详解】多模态消息构造</b>：
     * <ul>
     *   <li><b>UserMessage.builder()</b>：Spring AI 的多模态消息构建器</li>
     *   <li><b>.text(...)</b>：给视觉模型的指令，让它聚焦"提取关键信息"</li>
     *   <li><b>.media(new Media(mimeType, URI))</b>：把图片 URL 包装成 Media，
     *       OpenAiChatModel 会自动转成 OpenAI 协议的 image_url 字段</li>
     * </ul>
     *
     * <p><b>【技术关联】与 VisionChatServiceImpl 的关系</b>：
     * <ul>
     *   <li>本方法：封装视觉模型调用，带弹性容错（超时/重试/熔断）</li>
     *   <li>VisionChatServiceImpl：调用本方法并处理结果（脱敏、降级）</li>
     *   <li>分离关注点：模型调用 vs 业务逻辑</li>
     * </ul>
     *
     * @param imageUrl 图片 URL（已通过 {@link com.aics.chat.util.ImageUrlValidator} 校验）
     * @return 图片描述文本；识别失败或降级时返回 null
     */
    @TimeLimiter(name = "visionService", fallbackMethod = "fallbackDescribe")
    @Retry(name = "visionService", fallbackMethod = "fallbackDescribe")
    @CircuitBreaker(name = "visionService", fallbackMethod = "fallbackDescribe")
    public CompletableFuture<String> describeAsync(String imageUrl) {
        // 跨异步边界捕获 TraceContext（视觉调用在独立线程执行）
        // 学习点：与 ResilientAiService 相同的异步传播模式——capture 在调用线程、
        // restore 在 supplyAsync 内，保证视觉 span 归属正确的请求上下文；
        // 视觉调用与文本 LLM 调用分属不同模型通道（Qwen-VL vs DeepSeek），
        // 但都统一走 LLM span 类型 + scenario=vision 区分，trace 图上能看清"两段式"编排
        TraceContext captured = TraceContextHolder.capture();
        // supplyAsync：把阻塞的视觉模型调用丢到独立线程执行，
        // 返回 Future 供 @TimeLimiter 通过 Future.get(timeout) 实现 5s 超时控制
        return CompletableFuture.supplyAsync(() -> {
            TraceContextHolder.restore(captured);
            try {
                if (visionModel == null) {
                    // 视觉模型未初始化（未启用/未配 API Key）→ 抛异常触发降级
                    throw new IllegalStateException("视觉模型未初始化");
                }
                // 构造多模态消息：文本指令 + 图片
                // - UserMessage.builder()：Spring AI 的多模态消息构建器
                // - .text(...)：给视觉模型的指令，让它聚焦"提取关键信息"
                // - .media(new Media(mimeType, URI))：把图片 URL 包装成 Media，
                //   OpenAiChatModel 会自动转成 OpenAI 协议的 image_url 字段
                UserMessage userMessage = UserMessage.builder()
                        .text("请描述这张图片中的关键信息（文字、型号、错误码、页面状态等），用简洁的中文概括。")
                        .media(new Media(MimeTypeUtils.IMAGE_PNG, URI.create(imageUrl)))
                        .build();
                // LLM 环节观测（scenario=vision）+ Token/费用计量
                Observation observation = Observation.createNotStarted("chat.vision", observationRegistry)
                        .lowCardinalityKeyValue("span.type", "LLM")
                        .lowCardinalityKeyValue("provider", PROVIDER)
                        .lowCardinalityKeyValue("model", visionProperties.getModel())
                        .highCardinalityKeyValue("scenario", SCENARIO_VISION)
                        .start();
                try {
                    // call(...)：发起视觉模型调用
                    //   .getResult()：拿到完整结果
                    //   .getOutput()：拿到消息输出
                    //   .getText()：提取纯文本描述
                    ChatResponse response = visionModel.call(new Prompt(List.of(userMessage)));
                    String description = response == null || response.getResult() == null
                            ? null : response.getResult().getOutput().getText();
                    Usage usage = response == null || response.getMetadata() == null
                            ? null : response.getMetadata().getUsage();
                    if (usage != null) {
                        observation.highCardinalityKeyValue("promptTokens", String.valueOf(usage.getPromptTokens()))
                                .highCardinalityKeyValue("completionTokens", String.valueOf(usage.getCompletionTokens()));
                    }
                    modelUsageRecorder.record(SCENARIO_VISION, PROVIDER, visionProperties.getModel(),
                            usage == null ? null : usage.getPromptTokens(),
                            usage == null ? null : usage.getCompletionTokens(),
                            "SUCCESS", null);
                    log.info("视觉理解完成: 描述长度={}", description == null ? 0 : description.length());
                    return description;
                } catch (Exception e) {
                    observation.error(e);
                    modelUsageRecorder.record(SCENARIO_VISION, PROVIDER, visionProperties.getModel(),
                            null, null, "FAILED", e.getMessage());
                    throw e;
                } finally {
                    observation.stop();
                }
            } catch (Exception e) {
                // 包装为 CompletionException，供外层 Future.get 解包与重试判定
                throw new CompletionException(e);
            } finally {
                TraceContextHolder.clear();
            }
        });
    }

    /**
     * 视觉调用降级：返回 null 表示"未识别到图片内容"。
     */
    @SuppressWarnings("unused")
    private CompletableFuture<String> fallbackDescribe(Throwable e) {
        log.warn("视觉调用降级: {}", e.getMessage());
        return CompletableFuture.completedFuture(null);
    }
}
