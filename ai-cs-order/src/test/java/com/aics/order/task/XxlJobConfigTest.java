package com.aics.order.task;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * XXL-Job 执行器条件装配测试：默认关闭（本地无 admin 可启动），开关打开才装配
 */
class XxlJobConfigTest {

    private final ApplicationContextRunner runner = new ApplicationContextRunner()
            .withUserConfiguration(XxlJobConfig.class)
            .withPropertyValues("aics.xxl-job.admin-addresses=http://127.0.0.1:8099/xxl-job-admin");

    @Test
    @DisplayName("默认（未开启开关）不装配执行器")
    void disabledByDefault() {
        runner.run(context -> assertFalse(context.containsBean("xxlJobExecutor")));
    }

    @Test
    @DisplayName("开关打开时装配 XxlJobSpringExecutor")
    void enabledWhenPropertySet() {
        runner.withPropertyValues("aics.xxl-job.enabled=true")
                .run(context -> assertTrue(context.containsBean("xxlJobExecutor")));
    }
}
