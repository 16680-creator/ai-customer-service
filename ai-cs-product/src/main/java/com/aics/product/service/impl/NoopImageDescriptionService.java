package com.aics.product.service.impl;

import com.aics.product.service.ImageDescriptionService;
import org.springframework.stereotype.Service;

/**
 * 图片描述占位实现：暂不识别图片，返回 null。
 * 接入视觉模型（MiniMax/Ollama）后替换或删除本类。
 */
@Service
public class NoopImageDescriptionService implements ImageDescriptionService {

    @Override
    public String describe(String imageUrl) {
        return null;
    }
}
