<!-- learning-docs source archive（机械抓取原文，禁止手改代码内容） -->
<!-- origin: org.springframework.boot:spring-boot-security:4.2.0 | path: org/springframework/boot/security/autoconfigure/web/servlet/SecurityFilterAutoConfiguration.java -->
<!-- 正文引用：[[源码:SecurityFilterAutoConfiguration.java#L起-L止]] ；代码块头部：(src: SecurityFilterAutoConfiguration.java#L起-L止) -->

# SecurityFilterAutoConfiguration.java

| 项 | 值 |
| --- | --- |
| 来源 | `org.springframework.boot:spring-boot-security:4.2.0` |
| 路径 | `org/springframework/boot/security/autoconfigure/web/servlet/SecurityFilterAutoConfiguration.java` |
| 行数 | 66 |
| 在线地址 | https://repo1.maven.org/maven2/org/springframework/boot/spring-boot-security/4.2.0/spring-boot-security-4.2.0-sources.jar |
| 抓取时间 | 2026-11-25T16:27:52.795Z |

```java
    1 | /*
    2 |  * Copyright 2012-present the original author or authors.
    3 |  */
    4 | 
    5 | package org.springframework.boot.security.autoconfigure.web.servlet;
    6 | 
    7 | import java.util.EnumSet;
    8 | import java.util.stream.Collectors;
    9 | 
   10 | import jakarta.servlet.DispatcherType;
   11 | import org.jspecify.annotations.Nullable;
   12 | 
   13 | import org.springframework.boot.autoconfigure.AutoConfiguration;
   14 | import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
   15 | import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
   16 | import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
   17 | import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication;
   18 | import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication.Type;
   19 | import org.springframework.boot.context.properties.EnableConfigurationProperties;
   20 | import org.springframework.boot.web.servlet.DelegatingFilterProxyRegistrationBean;
   21 | import org.springframework.context.annotation.Bean;
   22 | import org.springframework.security.config.annotation.web.configuration.WebSecurityConfiguration;
   23 | import org.springframework.security.config.http.SessionCreationPolicy;
   24 | import org.springframework.security.web.context.AbstractSecurityWebApplicationInitializer;
   25 | 
   26 | /**
   27 |  * {@link EnableAutoConfiguration Auto-configuration} for Spring Security's Filter.
   28 |  * Configured separately from {@link ServletWebSecurityAutoConfiguration} to ensure that
   29 |  * the filter's order is still configured when a user-provided
   30 |  * {@link WebSecurityConfiguration} exists.
   31 |  *
   32 |  * @author Rob Winch
   33 |  * @author Phillip Webb
   34 |  * @author Andy Wilkinson
   35 |  * @since 4.0.0
   36 |  */
   37 | @AutoConfiguration(after = ServletWebSecurityAutoConfiguration.class)
   38 | @ConditionalOnWebApplication(type = Type.SERVLET)
   39 | @EnableConfigurationProperties(SecurityFilterProperties.class)
   40 | @ConditionalOnClass({ AbstractSecurityWebApplicationInitializer.class, SessionCreationPolicy.class })
   41 | public final class SecurityFilterAutoConfiguration {
   42 | 
   43 | 	private static final String DEFAULT_FILTER_NAME = AbstractSecurityWebApplicationInitializer.DEFAULT_FILTER_NAME;
   44 | 
   45 | 	@Bean
   46 | 	@ConditionalOnBean(name = DEFAULT_FILTER_NAME)
   47 | 	DelegatingFilterProxyRegistrationBean securityFilterChainRegistration(
   48 | 			SecurityFilterProperties securityFilterProperties) {
   49 | 		DelegatingFilterProxyRegistrationBean registration = new DelegatingFilterProxyRegistrationBean(
   50 | 				DEFAULT_FILTER_NAME);
   51 | 		registration.setOrder(securityFilterProperties.getOrder());
   52 | 		registration.setDispatcherTypes(getDispatcherTypes(securityFilterProperties));
   53 | 		return registration;
   54 | 	}
   55 | 
   56 | 	private @Nullable EnumSet<DispatcherType> getDispatcherTypes(SecurityFilterProperties securityFilterProperties) {
   57 | 		if (securityFilterProperties.getDispatcherTypes() == null) {
   58 | 			return null;
   59 | 		}
   60 | 		return securityFilterProperties.getDispatcherTypes()
   61 | 			.stream()
   62 | 			.map((type) -> DispatcherType.valueOf(type.name()))
   63 | 			.collect(Collectors.toCollection(() -> EnumSet.noneOf(DispatcherType.class)));
   64 | 	}
   65 | 
   66 | }
```
