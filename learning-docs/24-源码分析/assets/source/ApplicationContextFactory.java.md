<!-- learning-docs source archive（机械抓取原文，禁止手改代码内容） -->
<!-- origin: org.springframework.boot:spring-boot:4.2.0 | path: org/springframework/boot/ApplicationContextFactory.java -->
<!-- 正文引用：[[源码:ApplicationContextFactory.java#L起-L止]] ；代码块头部：(src: ApplicationContextFactory.java#L起-L止) -->

# ApplicationContextFactory.java

| 项 | 值 |
| --- | --- |
| 来源 | `org.springframework.boot:spring-boot:4.2.0` |
| 路径 | `org/springframework/boot/ApplicationContextFactory.java` |
| 行数 | 91 |
| 在线地址 | https://repo1.maven.org/maven2/org/springframework/boot/spring-boot/4.2.0/spring-boot-4.2.0-sources.jar |
| 抓取时间 | 2026-11-26T09:21:47.218Z |

```java
    1 | /*
    2 |  * Copyright 2012-present the original author or authors.
    3 |  */
    4 | 
    5 | package org.springframework.boot;
    6 | 
    7 | import java.util.function.Supplier;
    8 | 
    9 | import org.jspecify.annotations.Nullable;
   10 | 
   11 | import org.springframework.beans.BeanUtils;
   12 | import org.springframework.context.ConfigurableApplicationContext;
   13 | import org.springframework.core.env.ConfigurableEnvironment;
   14 | import org.springframework.core.env.Environment;
   15 | 
   16 | /**
   17 |  * Strategy interface for creating the {@link ConfigurableApplicationContext} used by a
   18 |  * {@link SpringApplication}. Created contexts should be returned in their default form,
   19 |  * with the {@code SpringApplication} responsible for configuring and refreshing the
   20 |  * context.
   21 |  *
   22 |  * @author Andy Wilkinson
   23 |  * @author Phillip Webb
   24 |  * @since 2.4.0
   25 |  */
   26 | @FunctionalInterface
   27 | public interface ApplicationContextFactory {
   28 | 
   29 | 	/**
   30 | 	 * A default {@link ApplicationContextFactory} implementation that will create an
   31 | 	 * appropriate context for the {@link WebApplicationType}.
   32 | 	 */
   33 | 	ApplicationContextFactory DEFAULT = new DefaultApplicationContextFactory();
   34 | 
   35 | 	/**
   36 | 	 * Return the {@link Environment} type expected to be set on the
   37 | 	 * {@link #create(WebApplicationType) created} application context. The result of this
   38 | 	 * method can be used to convert an existing environment instance to the correct type.
   39 | 	 * @param webApplicationType the web application type or {@code null}
   40 | 	 * @return the expected application context type or {@code null} to use the default
   41 | 	 * @since 2.6.14
   42 | 	 */
   43 | 	default @Nullable Class<? extends ConfigurableEnvironment> getEnvironmentType(
   44 | 			@Nullable WebApplicationType webApplicationType) {
   45 | 		return null;
   46 | 	}
   47 | 
   48 | 	/**
   49 | 	 * Create a new {@link Environment} to be set on the
   50 | 	 * {@link #create(WebApplicationType) created} application context. The result of this
   51 | 	 * method must match the type returned by
   52 | 	 * {@link #getEnvironmentType(WebApplicationType)}.
   53 | 	 * @param webApplicationType the web application type or {@code null}
   54 | 	 * @return an environment instance or {@code null} to use the default
   55 | 	 * @since 2.6.14
   56 | 	 */
   57 | 	default @Nullable ConfigurableEnvironment createEnvironment(@Nullable WebApplicationType webApplicationType) {
   58 | 		return null;
   59 | 	}
   60 | 
   61 | 	/**
   62 | 	 * Creates the {@link ConfigurableApplicationContext application context} for a
   63 | 	 * {@link SpringApplication}, respecting the given {@code webApplicationType}.
   64 | 	 * @param webApplicationType the web application type
   65 | 	 * @return the newly created application context
   66 | 	 */
   67 | 	@Nullable ConfigurableApplicationContext create(@Nullable WebApplicationType webApplicationType);
   68 | 
   69 | 	/**
   70 | 	 * Creates an {@code ApplicationContextFactory} that will create contexts by
   71 | 	 * instantiating the given {@code contextClass} through its primary constructor.
   72 | 	 * @param contextClass the context class
   73 | 	 * @return the factory that will instantiate the context class
   74 | 	 * @see BeanUtils#instantiateClass(Class)
   75 | 	 */
   76 | 	static ApplicationContextFactory ofContextClass(Class<? extends ConfigurableApplicationContext> contextClass) {
   77 | 		return of(() -> BeanUtils.instantiateClass(contextClass));
   78 | 	}
   79 | 
   80 | 	/**
   81 | 	 * Creates an {@code ApplicationContextFactory} that will create contexts by calling
   82 | 	 * the given {@link Supplier}.
   83 | 	 * @param supplier the context supplier, for example
   84 | 	 * {@code AnnotationConfigApplicationContext::new}
   85 | 	 * @return the factory that will instantiate the context class
   86 | 	 */
   87 | 	static ApplicationContextFactory of(Supplier<ConfigurableApplicationContext> supplier) {
   88 | 		return (webApplicationType) -> supplier.get();
   89 | 	}
   90 | 
   91 | }
```
