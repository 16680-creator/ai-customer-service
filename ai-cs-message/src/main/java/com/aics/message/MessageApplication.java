package com.aics.message;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;

/**
 * 消息服务启动类
 * <p>
 * 所属模块：ai-cs-message（智能客服消息服务）。
 * 职责：作为独立 Spring Cloud 微服务启动，负责会话与聊天消息的持久化、查询以及
 * 基于 RocketMQ 的异步消息消费入库。
 * 关键协作：
 * <ul>
 *     <li>通过 {@link EnableDiscoveryClient} 注册到注册中心，供 chat 等上游服务调用；</li>
 *     <li>通过 {@link MapperScan} 扫描 com.aics.message.mapper 包下的 MyBatis-Plus Mapper；</li>
 *     <li>消费 RocketMQ 中 chat-message-topic 的消息并落库，实现 chat 模块与持久化解耦。</li>
 * </ul>
 * 技术要点：Spring Boot + Spring Cloud + MyBatis-Plus + RocketMQ。
 * </p>
 */
@SpringBootApplication
@EnableDiscoveryClient
@MapperScan("com.aics.message.mapper")
public class MessageApplication {

    public static void main(String[] args) {
        SpringApplication.run(MessageApplication.class, args);
    }
}
