package com.aics.product.service;

/**
 * 图片描述服务：根据商品图片 URL 生成文本描述，用于增强向量检索。
 * 当前为占位实现；待接入 MiniMax/Ollama 视觉模型后替换为真实实现。
 */
public interface ImageDescriptionService {

    /**
     * 生成图片描述
     *
     * @param imageUrl 图片 URL
     * @return 图片文本描述；无法识别时返回 null
     */
    String describe(String imageUrl);
}
