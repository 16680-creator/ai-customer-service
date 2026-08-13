package com.aics.chat.util;

import com.aics.chat.config.VisionProperties;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.net.URI;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

/**
 * 图片 URL 安全校验器（SSRF 防护）。
 *
 * <p>图片对话的 imageUrl 是外部输入，若直接请求可被利用发起 SSRF 攻击
 * （探测内网、读取云元数据等）。本类对 URL 做白名单校验：
 * 仅允许 http/https 协议，且主机名必须命中配置的 {@code aics.vision.allowed-image-host} 白名单。</p>
 */
@Slf4j
@Component
public class ImageUrlValidator {

    /** 允许的图片主机白名单（子域可匹配） */
    private final Set<String> allowedHosts = new HashSet<>();

    public ImageUrlValidator(VisionProperties visionProperties) {
        String hosts = visionProperties.getAllowedImageHost();
        if (StringUtils.hasText(hosts)) {
            Arrays.stream(hosts.split(","))
                    .map(String::trim)
                    .filter(StringUtils::hasText)
                    .forEach(allowedHosts::add);
        }
    }

    /**
     * 校验图片 URL 是否合法且命中白名单。
     *
     * @param imageUrl 待校验的图片 URL
     * @return true 表示可安全访问
     */
    public boolean isValid(String imageUrl) {
        if (!StringUtils.hasText(imageUrl)) {
            return false;
        }
        try {
            URI uri = URI.create(imageUrl);
            String scheme = uri.getScheme();
            // 仅允许 http/https，拒绝 file/ftp 等其他协议
            if (scheme == null || (!"http".equalsIgnoreCase(scheme) && !"https".equalsIgnoreCase(scheme))) {
                return false;
            }
            String host = uri.getHost();
            if (!StringUtils.hasText(host)) {
                return false;
            }
            // 白名单未配置时拒绝所有地址（安全默认，防 SSRF）
            if (allowedHosts.isEmpty()) {
                log.warn("图片 URL 白名单未配置，拒绝访问: host={}", host);
                return false;
            }
            // 主机名精确匹配或子域匹配
            for (String allowed : allowedHosts) {
                if (host.equalsIgnoreCase(allowed) || host.endsWith("." + allowed)) {
                    return true;
                }
            }
            return false;
        } catch (Exception e) {
            log.warn("图片 URL 解析失败: imageUrl={}", imageUrl);
            return false;
        }
    }
}
