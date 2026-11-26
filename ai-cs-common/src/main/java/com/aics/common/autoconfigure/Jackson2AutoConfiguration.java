package com.aics.common.autoconfigure;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;

/**
 * 为仍使用 Jackson 2 的业务代码保留独立 ObjectMapper。
 * Spring Boot 4 默认提供 Jackson 3 的 tools.jackson JsonMapper，二者不是同一类型。
 */
@AutoConfiguration
@ConditionalOnClass(ObjectMapper.class)
public class Jackson2AutoConfiguration {

    @Bean
    @ConditionalOnMissingBean(ObjectMapper.class)
    public ObjectMapper jackson2ObjectMapper() {
        return new ObjectMapper();
    }
}
