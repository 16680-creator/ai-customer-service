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
            // 白名单配置形如 "minio.internal,cdn.example.com"，逗号分隔
            Arrays.stream(hosts.split(","))       // 按逗号拆成多个主机
                    .map(String::trim)            // 去掉首尾空格
                    .filter(StringUtils::hasText) // 过滤空项
                    .forEach(allowedHosts::add);  // 加入白名单集合
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
            URI uri = URI.create(imageUrl);   // 解析 URL（失败会抛异常，走 catch 返回 false）
            String scheme = uri.getScheme();  // 协议：http / https / file / ftp ...
            // 仅允许 http/https，拒绝 file/ftp 等其他协议（防本地文件读取）
            if (scheme == null || (!"http".equalsIgnoreCase(scheme) && !"https".equalsIgnoreCase(scheme))) {
                return false;
            }
            String host = uri.getHost();      // 主机名，如 "minio.internal"
            if (!StringUtils.hasText(host)) {
                return false;                  // 无主机名（如相对路径）拒绝
            }
            // 白名单未配置时拒绝所有地址（安全默认，防 SSRF 探测内网）
            if (allowedHosts.isEmpty()) {
                log.warn("图片 URL 白名单未配置，拒绝访问: host={}", host);
                return false;
            }
            // 主机名精确匹配（minio.internal）或子域匹配（img.minio.internal）
            for (String allowed : allowedHosts) {
                if (host.equalsIgnoreCase(allowed) || host.endsWith("." + allowed)) {
                    return true;
                }
            }
            return false;   // 未命中白名单，拒绝
        } catch (Exception e) {
            log.warn("图片 URL 解析失败: imageUrl={}", imageUrl);
            return false;
        }
    }
}
