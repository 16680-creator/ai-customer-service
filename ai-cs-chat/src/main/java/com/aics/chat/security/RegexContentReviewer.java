package com.aics.chat.security;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

/**
 * 正则内容审核器（3.2 F4 默认实现）：按配置的分类正则逐条匹配，命中任一即违规。
 *
 * <p>确定性实现，便于单元测试与对抗样本验证；分类与规则全部来自
 * {@link SecurityProperties#getContentCategories()}。</p>
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class RegexContentReviewer implements ContentReviewer {

    private final SecurityProperties properties;

    @Override
    public ContentReviewResult review(String text, String stage) {
        if (text == null || text.isBlank()) {
            return ContentReviewResult.pass();
        }
        Map<String, List<String>> categories = properties.getContentCategories();
        if (categories == null || categories.isEmpty()) {
            return ContentReviewResult.pass();
        }
        for (Map.Entry<String, List<String>> entry : categories.entrySet()) {
            for (String regex : entry.getValue()) {
                try {
                    if (Pattern.compile(regex, Pattern.CASE_INSENSITIVE).matcher(text).find()) {
                        log.warn("内容审核命中: category={}, stage={}", entry.getKey(), stage);
                        return ContentReviewResult.block(entry.getKey(),
                                "内容涉及" + entry.getKey() + "分类，已拦截");
                    }
                } catch (Exception e) {
                    // 单条规则配置错误不影响其他规则
                    log.warn("内容审核规则解析失败: category={}, regex={}, err={}",
                            entry.getKey(), regex, e.getMessage());
                }
            }
        }
        return ContentReviewResult.pass();
    }
}
