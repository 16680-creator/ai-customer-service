package com.aics.mq;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;

/**
 * RocketMQ 管理服务启动类
 */
@SpringBootApplication(scanBasePackages = {"com.aics.mq", "com.aics.common"})
@EnableDiscoveryClient
public class MqApplication {

    public static void main(String[] args) {
        SpringApplication.run(MqApplication.class, args);
    }
}