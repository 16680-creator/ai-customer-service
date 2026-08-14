package com.aics.pay.config;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

/**
 * 启动时幂等初始化支付流水表（生产可改用 Flyway/Liquibase）
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class PaySchemaInitializer implements ApplicationRunner {

    private final JdbcTemplate jdbcTemplate;

    @Override
    public void run(ApplicationArguments args) {
        String ddl = """
                CREATE TABLE IF NOT EXISTS `pay_transaction` (
                    `id` BIGINT NOT NULL AUTO_INCREMENT,
                    `order_no` VARCHAR(32) NOT NULL,
                    `user_id` BIGINT NOT NULL,
                    `payment_method` VARCHAR(20) NOT NULL,
                    `trade_no` VARCHAR(64) DEFAULT NULL,
                    `pay_amount` DECIMAL(10,2) NOT NULL,
                    `status` VARCHAR(20) NOT NULL DEFAULT 'PENDING',
                    `notify_count` INT NOT NULL DEFAULT 0,
                    `pay_time` DATETIME DEFAULT NULL,
                    `refund_time` DATETIME DEFAULT NULL,
                    `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
                    `update_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
                    PRIMARY KEY (`id`),
                    UNIQUE KEY `uk_order_no` (`order_no`),
                    KEY `idx_user_id` (`user_id`)
                ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='支付流水表'
                """;
        try {
            jdbcTemplate.execute(ddl);
            log.info("支付流水表就绪: pay_transaction");
        } catch (Exception e) {
            log.warn("支付流水表初始化失败（不影响启动）: {}", e.getMessage());
        }
    }
}