package com.aics.order;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;

/**
 * 订单服务启动类
 */
@SpringBootApplication(scanBasePackages = {"com.aics.order", "com.aics.common"})
@EnableDiscoveryClient
@MapperScan("com.aics.order.mapper")
public class OrderApplication {

    public static void main(String[] args) {
        SpringApplication.run(OrderApplication.class, args);
    }
}
