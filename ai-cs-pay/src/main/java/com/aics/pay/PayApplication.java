package com.aics.pay;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;

/**
 * 支付服务启动类
 */
@SpringBootApplication(scanBasePackages = {"com.aics.pay"})
@EnableDiscoveryClient
@MapperScan("com.aics.pay.mapper")
public class PayApplication {

    public static void main(String[] args) {
        SpringApplication.run(PayApplication.class, args);
    }
}