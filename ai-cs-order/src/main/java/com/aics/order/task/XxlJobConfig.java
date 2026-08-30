package com.aics.order.task;

import com.xxl.job.core.executor.impl.XxlJobSpringExecutor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * XXL-Job 执行器装配。
 *
 * <p>学习要点：执行器（executor）内嵌在业务服务里，通过 {@code appname} 注册到调度中心
 * （admin），admin 按 cron 触发时回调本服务的 JobHandler。与 {@code @Scheduled} 的区别：
 * admin 统一管理触发时机/失败重试/分片广播/执行日志看板，多实例部署时任务不会被重复执行
 * （路由策略控制），而 @Scheduled 每个实例都会跑。</p>
 *
 * <p>开关策略：{@code aics.xxl-job.enabled=false}（默认）时不创建执行器，本服务退回
 * {@code @Scheduled} 兜底扫描，保证本地无 admin 也能正常启动。</p>
 */
@Slf4j
@Configuration
@ConditionalOnProperty(name = "aics.xxl-job.enabled", havingValue = "true")
public class XxlJobConfig {

    @Value("${aics.xxl-job.admin-addresses}")
    private String adminAddresses;

    @Value("${aics.xxl-job.access-token:default_token}")
    private String accessToken;

    @Value("${aics.xxl-job.executor.port:9999}")
    private int executorPort;

    @Bean
    public XxlJobSpringExecutor xxlJobExecutor() {
        XxlJobSpringExecutor executor = new XxlJobSpringExecutor();
        executor.setAdminAddresses(adminAddresses);
        executor.setAppname("ai-cs-order");
        executor.setPort(executorPort);
        executor.setAccessToken(accessToken);
        executor.setLogPath(System.getProperty("user.home") + "/logs/xxl-job/ai-cs-order");
        executor.setLogRetentionDays(7);
        log.info("XXL-Job 执行器已启用: admin={}, port={}", adminAddresses, executorPort);
        return executor;
    }
}
