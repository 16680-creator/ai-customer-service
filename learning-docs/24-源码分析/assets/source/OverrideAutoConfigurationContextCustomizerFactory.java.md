<!-- learning-docs source archive（机械抓取原文，禁止手改代码内容） -->
<!-- origin: org.springframework.boot:spring-boot-test-autoconfigure:4.2.0 | path: org/springframework/boot/test/autoconfigure/OverrideAutoConfigurationContextCustomizerFactory.java -->
<!-- 正文引用：[[源码:OverrideAutoConfigurationContextCustomizerFactory.java#L起-L止]] ；代码块头部：(src: OverrideAutoConfigurationContextCustomizerFactory.java#L起-L止) -->

# OverrideAutoConfigurationContextCustomizerFactory.java

| 项 | 值 |
| --- | --- |
| 来源 | `org.springframework.boot:spring-boot-test-autoconfigure:4.2.0` |
| 路径 | `org/springframework/boot/test/autoconfigure/OverrideAutoConfigurationContextCustomizerFactory.java` |
| 行数 | 63 |
| 在线地址 | https://repo1.maven.org/maven2/org/springframework/boot/spring-boot-test-autoconfigure/4.2.0/spring-boot-test-autoconfigure-4.2.0-sources.jar |
| 抓取时间 | 2026-11-25T14:05:16.355Z |

```java
    1 | /*
    2 |  * Copyright 2012-present the original author or authors.
    3 |  */
    4 | 
    5 | package org.springframework.boot.test.autoconfigure;
    6 | 
    7 | import java.util.List;
    8 | 
    9 | import org.jspecify.annotations.Nullable;
   10 | 
   11 | import org.springframework.aot.AotDetector;
   12 | import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
   13 | import org.springframework.boot.test.util.TestPropertyValues;
   14 | import org.springframework.context.ConfigurableApplicationContext;
   15 | import org.springframework.test.context.ContextConfigurationAttributes;
   16 | import org.springframework.test.context.ContextCustomizer;
   17 | import org.springframework.test.context.ContextCustomizerFactory;
   18 | import org.springframework.test.context.MergedContextConfiguration;
   19 | import org.springframework.test.context.TestContextAnnotationUtils;
   20 | 
   21 | /**
   22 |  * {@link ContextCustomizerFactory} to support
   23 |  * {@link OverrideAutoConfiguration @OverrideAutoConfiguration}.
   24 |  *
   25 |  * @author Phillip Webb
   26 |  */
   27 | class OverrideAutoConfigurationContextCustomizerFactory implements ContextCustomizerFactory {
   28 | 
   29 | 	@Override
   30 | 	public @Nullable ContextCustomizer createContextCustomizer(Class<?> testClass,
   31 | 			List<ContextConfigurationAttributes> configurationAttributes) {
   32 | 		if (AotDetector.useGeneratedArtifacts()) {
   33 | 			return null;
   34 | 		}
   35 | 		OverrideAutoConfiguration overrideAutoConfiguration = TestContextAnnotationUtils.findMergedAnnotation(testClass,
   36 | 				OverrideAutoConfiguration.class);
   37 | 		boolean enabled = (overrideAutoConfiguration == null) || overrideAutoConfiguration.enabled();
   38 | 		return !enabled ? new DisableAutoConfigurationContextCustomizer() : null;
   39 | 	}
   40 | 
   41 | 	/**
   42 | 	 * {@link ContextCustomizer} to disable full auto-configuration.
   43 | 	 */
   44 | 	private static final class DisableAutoConfigurationContextCustomizer implements ContextCustomizer {
   45 | 
   46 | 		@Override
   47 | 		public void customizeContext(ConfigurableApplicationContext context, MergedContextConfiguration mergedConfig) {
   48 | 			TestPropertyValues.of(EnableAutoConfiguration.ENABLED_OVERRIDE_PROPERTY + "=false").applyTo(context);
   49 | 		}
   50 | 
   51 | 		@Override
   52 | 		public boolean equals(@Nullable Object obj) {
   53 | 			return (obj != null) && (obj.getClass() == getClass());
   54 | 		}
   55 | 
   56 | 		@Override
   57 | 		public int hashCode() {
   58 | 			return getClass().hashCode();
   59 | 		}
   60 | 
   61 | 	}
   62 | 
   63 | }
```
