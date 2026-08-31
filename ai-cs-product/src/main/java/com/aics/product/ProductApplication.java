package com.aics.product;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;

/**
 * 商品服务启动类
 */
@SpringBootApplication(scanBasePackages = {"com.aics.product"}, exclude = {org.springframework.ai.model.openai.autoconfigure.OpenAiEmbeddingAutoConfiguration.class})
@EnableDiscoveryClient
@MapperScan("com.aics.product.mapper")
public class ProductApplication {

    public static void main(String[] args) {
        SpringApplication.run(ProductApplication.class, args);
    }
}
