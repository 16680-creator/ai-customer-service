package com.aics.chat.service.impl;

import com.aics.chat.config.VisionProperties;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import io.github.resilience4j.retry.annotation.Retry;
import io.github.resilience4j.timelimiter.annotation.TimeLimiter;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.messages.UserMessage;
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
 */
@Slf4j
@Service
public class VisionModelClient {

    private final VisionProperties visionProperties;

    /** 视觉模型（懒初始化，enabled 且配置 apiKey 时才构造） */
    private volatile OpenAiChatModel visionModel;

    public VisionModelClient(VisionProperties visionProperties) {
        this.visionProperties = visionProperties;
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
     * 弹性图片理解调用：把图片 URL 作为多模态消息交给视觉模型，返回图片文本描述。
     *
     * @param imageUrl 图片 URL（已通过 {@link com.aics.chat.util.ImageUrlValidator} 校验）
     * @return 图片描述文本；识别失败或降级时返回 null
     */
    @TimeLimiter(name = "visionService", fallbackMethod = "fallbackDescribe")
    @Retry(name = "visionService", fallbackMethod = "fallbackDescribe")
    @CircuitBreaker(name = "visionService", fallbackMethod = "fallbackDescribe")
    public CompletableFuture<String> describeAsync(String imageUrl) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                if (visionModel == null) {
                    throw new IllegalStateException("视觉模型未初始化");
                }
                // 多模态消息：文本指令 + 图片（URL 作为 Media）
                UserMessage userMessage = UserMessage.builder()
                        .text("请描述这张图片中的关键信息（文字、型号、错误码、页面状态等），用简洁的中文概括。")
                        .media(new Media(MimeTypeUtils.IMAGE_PNG, URI.create(imageUrl)))
                        .build();
                String description = visionModel.call(new Prompt(List.of(userMessage)))
                        .getResult()
                        .getOutput()
                        .getText();
                log.info("视觉理解完成: 描述长度={}", description == null ? 0 : description.length());
                return description;
            } catch (Exception e) {
                // 包装为 CompletionException，供外层 Future.get 解包与重试判定
                throw new CompletionException(e);
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
