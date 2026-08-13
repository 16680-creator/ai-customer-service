package com.aics.chat.util;

import com.aics.chat.config.VisionProperties;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * ImageUrlValidator SSRF 白名单校验测试。
 */
class ImageUrlValidatorTest {

    private ImageUrlValidator validatorWith(String hosts) {
        VisionProperties p = new VisionProperties();
        p.setAllowedImageHost(hosts);
        return new ImageUrlValidator(p);
    }

    @Test
    @DisplayName("白名单命中（精确 + 子域）返回 true")
    void validUrlInWhitelist() {
        ImageUrlValidator validator = validatorWith("minio.internal, cdn.example.com");
        assertTrue(validator.isValid("http://minio.internal/aics/chat/images/a.png"));
        assertTrue(validator.isValid("https://cdn.example.com/x.png"));
        assertTrue(validator.isValid("https://img.minio.internal/x.jpg"));
    }

    @Test
    @DisplayName("白名单未命中返回 false")
    void urlNotInWhitelist() {
        ImageUrlValidator validator = validatorWith("minio.internal");
        assertFalse(validator.isValid("http://evil.com/x.png"));
        assertFalse(validator.isValid("http://minio.internal.evil.com/x.png"));
    }

    @Test
    @DisplayName("非 http/https 协议拒绝")
    void nonHttpSchemeRejected() {
        ImageUrlValidator validator = validatorWith("minio.internal");
        assertFalse(validator.isValid("file:///etc/passwd"));
        assertFalse(validator.isValid("ftp://minio.internal/x.png"));
        assertFalse(validator.isValid("javascript:alert(1)"));
    }

    @Test
    @DisplayName("空 URL / 空白名单拒绝（安全默认）")
    void emptyUrlOrWhitelistRejected() {
        ImageUrlValidator validator = validatorWith("minio.internal");
        assertFalse(validator.isValid(""));
        assertFalse(validator.isValid(null));

        ImageUrlValidator emptyWhitelist = validatorWith("");
        assertFalse(emptyWhitelist.isValid("http://minio.internal/x.png"));
    }
}
