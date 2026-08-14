package com.aics.product.service.impl;

import com.aics.product.service.ImageDescriptionService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.ai.content.Media;
import org.springframework.ai.openai.OpenAiChatModel;
import org.springframework.ai.openai.OpenAiChatOptions;
import org.springframework.ai.openai.api.OpenAiApi;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Service;
import org.springframework.util.MimeTypeUtils;
import org.springframework.util.StringUtils;

import java.net.URI;
import java.util.List;

/**
 * 商品图片描述服务实现（硅基流动视觉模型，OpenAI 兼容协议）。
 *
 * <p>替换 {@link NoopImageDescriptionService} 占位实现：调用多模态视觉模型
 * （{@code aics.vision.model}，默认 Qwen2.5-VL）识别商品图，生成文本描述，
 * 供 {@link ProductVectorService} 的"以图搜文/相似商品"增强向量检索。</p>
 *
 * <p>降级策略：视觉模型未配置、未启用或调用失败时返回 null，商品检索主流程不中断
 * （{@code buildSearchText} 对 null 描述直接跳过）。</p>
 */
@Slf4j
@Service
@Primary
public class SiliconFlowImageDescriptionService implements ImageDescriptionService {

    private final String baseUrl;
    private final String apiKey;
    private final String model;
    private final boolean enabled;

    /** 视觉模型（懒初始化，enabled 且配置 apiKey 时才构造） */
    private volatile OpenAiChatModel visionModel;

    public SiliconFlowImageDescriptionService(
            @Value("${aics.vision.base-url:https://api.siliconflow.cn}") String baseUrl,
            @Value("${aics.vision.api-key:}") String apiKey,
            @Value("${aics.vision.model:Qwen/Qwen2.5-VL-72B-Instruct}") String model,
            @Value("${aics.vision.enabled:true}") boolean enabled) {
        this.baseUrl = baseUrl;
        this.apiKey = apiKey;
        this.model = model;
        this.enabled = enabled;
        init();
    }

    private void init() {
        if (enabled && StringUtils.hasText(apiKey)) {
            OpenAiApi api = OpenAiApi.builder()
                    .baseUrl(baseUrl)
                    .apiKey(apiKey)
                    .build();
            this.visionModel = OpenAiChatModel.builder()
                    .openAiApi(api)
                    .defaultOptions(OpenAiChatOptions.builder().model(model).build())
                    .build();
            log.info("商品图片视觉模型已初始化: model={}", model);
        } else {
            log.warn("商品图片视觉模型未启用或未配置 API Key，图片描述将返回 null");
        }
    }

    @Override
    public String describe(String imageUrl) {
        // 视觉模型未初始化 或 图片地址为空 → 返回 null（调用方跳过描述，不阻断主流程）
        if (visionModel == null || !StringUtils.hasText(imageUrl)) {
            return null;
        }
        try {
            // 构造多模态消息：指令 + 图片 URL（Media 包装，OpenAiChatModel 转 image_url）
            UserMessage userMessage = UserMessage.builder()
                    .text("请描述这张商品图片的外观、品类和关键特征，用简洁的中文概括。")
                    .media(new Media(MimeTypeUtils.IMAGE_PNG, URI.create(imageUrl)))
                    .build();
            // 调用视觉模型：call → 结果 → 输出 → 纯文本描述
            String description = visionModel.call(new Prompt(List.of(userMessage)))
                    .getResult()
                    .getOutput()
                    .getText();
            // 描述为空串视为识别失败，统一返回 null
            return StringUtils.hasText(description) ? description : null;
        } catch (Exception e) {
            // 视觉调用异常（超时/限流/网络）→ 返回 null，商品创建/更新流程不中断
            log.warn("商品图片描述生成失败: {}", e.getMessage());
            return null;
        }
    }
}
