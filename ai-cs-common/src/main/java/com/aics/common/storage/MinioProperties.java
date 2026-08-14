package com.aics.common.storage;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * MinIO 对象存储配置
 */
@Data
@Component
@ConfigurationProperties(prefix = "aics.minio")
public class MinioProperties {

    /** 服务地址，如 http://123.60.31.79:9000 */
    private String endpoint = "http://123.60.31.79:9000";

    /** 访问密钥 */
    private String accessKey = "minioadmin";

    /** 密钥 */
    private String secretKey = "minioadmin";

    /** 默认桶名 */
    private String bucket = "ai-cs";

    /** 访问地址（公网/网关地址），为空时使用 endpoint */
    private String publicEndpoint;
}
