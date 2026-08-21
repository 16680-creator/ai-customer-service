package com.aics.chat.observability;

import io.micrometer.observation.ObservationRegistry;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.FilterType;
import org.springframework.context.annotation.Import;
import org.springframework.core.type.classreading.MetadataReader;
import org.springframework.core.type.classreading.MetadataReaderFactory;
import org.springframework.core.type.filter.TypeFilter;
import org.springframework.stereotype.Component;

import java.io.IOException;

import static org.assertj.core.api.Assertions.assertThat;

class ObservabilityPropertiesContextTest {

    private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
            .withUserConfiguration(ObservabilityScanConfig.class);

    @Test
    @DisplayName("可观测性配置属性只注册一个 Bean")
    void observabilityProperties_registersSingleBean() {
        contextRunner.run(context -> {
            assertThat(context).hasNotFailed();
            assertThat(context).hasSingleBean(ObservabilityProperties.class);
            assertThat(context).hasSingleBean(ObservationRegistry.class);
        });
    }

    @Configuration(proxyBeanMethods = false)
    @Import(ObservationConfig.class)
    @ComponentScan(
            basePackageClasses = ObservabilityProperties.class,
            useDefaultFilters = false,
            includeFilters = @ComponentScan.Filter(
                    type = FilterType.CUSTOM,
                    classes = ObservabilityPropertiesComponentFilter.class
            )
    )
    static class ObservabilityScanConfig {
    }

    static class ObservabilityPropertiesComponentFilter implements TypeFilter {

        @Override
        public boolean match(MetadataReader metadataReader,
                             MetadataReaderFactory metadataReaderFactory) throws IOException {
            return ObservabilityProperties.class.getName()
                    .equals(metadataReader.getClassMetadata().getClassName())
                    && metadataReader.getAnnotationMetadata()
                    .hasAnnotation(Component.class.getName());
        }
    }
}
