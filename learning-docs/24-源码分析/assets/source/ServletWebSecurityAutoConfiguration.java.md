<!-- learning-docs source archive（机械抓取原文，禁止手改代码内容） -->
<!-- origin: org.springframework.boot:spring-boot-security:4.2.0 | path: org/springframework/boot/security/autoconfigure/web/servlet/ServletWebSecurityAutoConfiguration.java -->
<!-- 正文引用：[[源码:ServletWebSecurityAutoConfiguration.java#L起-L止]] ；代码块头部：(src: ServletWebSecurityAutoConfiguration.java#L起-L止) -->

# ServletWebSecurityAutoConfiguration.java

| 项 | 值 |
| --- | --- |
| 来源 | `org.springframework.boot:spring-boot-security:4.2.0` |
| 路径 | `org/springframework/boot/security/autoconfigure/web/servlet/ServletWebSecurityAutoConfiguration.java` |
| 行数 | 97 |
| 在线地址 | https://repo1.maven.org/maven2/org/springframework/boot/spring-boot-security/4.2.0/spring-boot-security-4.2.0-sources.jar |
| 抓取时间 | 2026-11-25T16:28:37.147Z |

```java
    1 | /*
    2 |  * Copyright 2012-present the original author or authors.
    3 |  */
    4 | 
    5 | package org.springframework.boot.security.autoconfigure.web.servlet;
    6 | 
    7 | import org.springframework.boot.autoconfigure.AutoConfiguration;
    8 | import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
    9 | import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
   10 | import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
   11 | import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
   12 | import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication;
   13 | import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication.Type;
   14 | import org.springframework.boot.security.autoconfigure.UserDetailsServiceAutoConfiguration;
   15 | import org.springframework.boot.webmvc.autoconfigure.DispatcherServletPath;
   16 | import org.springframework.context.annotation.Bean;
   17 | import org.springframework.context.annotation.Configuration;
   18 | import org.springframework.core.annotation.Order;
   19 | import org.springframework.security.config.BeanIds;
   20 | import org.springframework.security.config.annotation.web.builders.HttpSecurity;
   21 | import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
   22 | import org.springframework.security.web.SecurityFilterChain;
   23 | import org.springframework.security.web.servlet.util.matcher.PathPatternRequestMatcher;
   24 | 
   25 | import static org.springframework.security.config.Customizer.withDefaults;
   26 | 
   27 | /**
   28 |  * {@link EnableAutoConfiguration Auto-configuration} for Spring Security.
   29 |  *
   30 |  * @author Dave Syer
   31 |  * @author Andy Wilkinson
   32 |  * @author Madhura Bhave
   33 |  * @since 4.0.0
   34 |  */
   35 | @AutoConfiguration(after = UserDetailsServiceAutoConfiguration.class,
   36 | 		afterName = { "org.springframework.boot.webmvc.autoconfigure.WebMvcAutoConfiguration",
   37 | 				"org.springframework.boot.webmvc.test.autoconfigure.MockMvcAutoConfiguration" })
   38 | @ConditionalOnClass(EnableWebSecurity.class)
   39 | @ConditionalOnWebApplication(type = Type.SERVLET)
   40 | public final class ServletWebSecurityAutoConfiguration {
   41 | 
   42 | 	@Configuration(proxyBeanMethods = false)
   43 | 	@ConditionalOnBean(DispatcherServletPath.class)
   44 | 	@ConditionalOnClass(DispatcherServletPath.class)
   45 | 	static class PathPatternRequestMatcherBuilderConfiguration {
   46 | 
   47 | 		@Bean
   48 | 		@ConditionalOnMissingBean
   49 | 		PathPatternRequestMatcher.Builder pathPatternRequestMatcherBuilder(
   50 | 				DispatcherServletPath dispatcherServletPath) {
   51 | 			PathPatternRequestMatcher.Builder builder = PathPatternRequestMatcher.withDefaults();
   52 | 			String path = dispatcherServletPath.getPath();
   53 | 			return (!path.equals("/")) ? builder.basePath(path) : builder;
   54 | 		}
   55 | 
   56 | 	}
   57 | 
   58 | 	/**
   59 | 	 * The default configuration for web security. It relies on Spring Security's
   60 | 	 * content-negotiation strategy to determine what sort of authentication to use. If
   61 | 	 * the user specifies their own {@link SecurityFilterChain} bean, this will back-off
   62 | 	 * completely and the users should specify all the bits that they want to configure as
   63 | 	 * part of the custom security configuration.
   64 | 	 */
   65 | 	@Configuration(proxyBeanMethods = false)
   66 | 	@ConditionalOnDefaultServletWebSecurity
   67 | 	static class SecurityFilterChainConfiguration {
   68 | 
   69 | 		@Bean
   70 | 		@Order(SecurityFilterProperties.BASIC_AUTH_ORDER)
   71 | 		SecurityFilterChain defaultSecurityFilterChain(HttpSecurity http) {
   72 | 			http.authorizeHttpRequests((requests) -> requests.anyRequest().authenticated());
   73 | 			http.formLogin(withDefaults());
   74 | 			http.httpBasic(withDefaults());
   75 | 			return http.build();
   76 | 		}
   77 | 
   78 | 	}
   79 | 
   80 | 	/**
   81 | 	 * Adds the {@link EnableWebSecurity @EnableWebSecurity} annotation if Spring Security
   82 | 	 * is on the classpath. This will make sure that the annotation is present with
   83 | 	 * default security auto-configuration and also if the user adds custom security and
   84 | 	 * forgets to add the annotation. If {@link EnableWebSecurity @EnableWebSecurity} has
   85 | 	 * already been added or if a bean with name
   86 | 	 * {@value BeanIds#SPRING_SECURITY_FILTER_CHAIN} has been configured by the user, this
   87 | 	 * will back-off.
   88 | 	 */
   89 | 	@Configuration(proxyBeanMethods = false)
   90 | 	@ConditionalOnMissingBean(name = BeanIds.SPRING_SECURITY_FILTER_CHAIN)
   91 | 	@ConditionalOnClass(EnableWebSecurity.class)
   92 | 	@EnableWebSecurity
   93 | 	static class EnableWebSecurityConfiguration {
   94 | 
   95 | 	}
   96 | 
   97 | }
```
