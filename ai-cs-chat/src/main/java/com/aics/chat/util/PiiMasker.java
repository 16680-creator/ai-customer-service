package com.aics.chat.util;

import org.springframework.stereotype.Component;

import java.util.regex.Pattern;

/**
 * 敏感信息脱敏工具（PII Mask）。
 *
 * <p>图片对话的视觉描述可能包含截图中的手机号、身份证号等敏感信息，
 * 注入 Prompt 前先脱敏，避免敏感信息进入大模型与回答。</p>
 */
@Component
public class PiiMasker {

    /**
     * 11 位手机号：保留前 3 位 + 后 4 位
     * 正则拆解：
     *   (1[3-9]\d)  — 第1组：前3位（1 开头 + 3~9 + 任意数字，即手机号号段）
     *   \d{4}       — 中间4位（被脱敏，不保留）
     *   (\d{4})     — 第2组：后4位（保留）
     */
    private static final Pattern PHONE = Pattern.compile("(1[3-9]\\d)\\d{4}(\\d{4})");

    /**
     * 18 位身份证号：保留前 6 位 + 后 4 位
     * 正则拆解：
     *   (\d{6})         — 第1组：前6位（行政区划码）
     *   \d{8}           — 中间8位（出生日期，被脱敏）
     *   (\d{3}[\dXx])   — 第2组：后4位（顺序码3位 + 校验码，校验码可为 X）
     */
    private static final Pattern ID_CARD = Pattern.compile("(\\d{6})\\d{8}(\\d{3}[\\dXx])");

    /**
     * 脱敏文本中的手机号与身份证号。
     *
     * @param text 原始文本（可为 null）
     * @return 脱敏后的文本
     */
    public String mask(String text) {
        if (text == null || text.isEmpty()) {
            return text;
        }
        // 先脱敏身份证（18位，更长更具体），再脱敏手机号，避免手机号正则误匹配身份证片段
        String masked = ID_CARD.matcher(text).replaceAll("$1********$2");
        masked = PHONE.matcher(masked).replaceAll("$1****$2");
        return masked;
    }
}
