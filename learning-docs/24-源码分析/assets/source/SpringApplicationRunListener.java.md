<!-- learning-docs source archive（机械抓取原文，禁止手改代码内容） -->
<!-- origin: org.springframework.boot:spring-boot:4.2.0 | path: org/springframework/boot/SpringApplicationRunListener.java -->
<!-- 正文引用：[[源码:SpringApplicationRunListener.java#L起-L止]] ；代码块头部：(src: SpringApplicationRunListener.java#L起-L止) -->

# SpringApplicationRunListener.java

| 项 | 值 |
| --- | --- |
| 来源 | `org.springframework.boot:spring-boot:4.2.0` |
| 路径 | `org/springframework/boot/SpringApplicationRunListener.java` |
| 行数 | 99 |
| 在线地址 | https://repo1.maven.org/maven2/org/springframework/boot/spring-boot/4.2.0/spring-boot-4.2.0-sources.jar |
| 抓取时间 | 2026-11-26T09:21:08.681Z |

```java
    1 | /*
    2 |  * Copyright 2012-present the original author or authors.
    3 |  */
    4 | 
    5 | package org.springframework.boot;
    6 | 
    7 | import java.time.Duration;
    8 | 
    9 | import org.jspecify.annotations.Nullable;
   10 | 
   11 | import org.springframework.boot.bootstrap.ConfigurableBootstrapContext;
   12 | import org.springframework.context.ApplicationContext;
   13 | import org.springframework.context.ConfigurableApplicationContext;
   14 | import org.springframework.core.env.ConfigurableEnvironment;
   15 | import org.springframework.core.io.support.SpringFactoriesLoader;
   16 | 
   17 | /**
   18 |  * Listener for the {@link SpringApplication} {@code run} method.
   19 |  * {@link SpringApplicationRunListener}s are loaded through the
   20 |  * {@link SpringFactoriesLoader} and should declare a public constructor that accepts a
   21 |  * {@link SpringApplication} instance and a {@code String[]} of arguments. A new
   22 |  * {@link SpringApplicationRunListener} instance will be created for each run.
   23 |  *
   24 |  * @author Phillip Webb
   25 |  * @author Dave Syer
   26 |  * @author Andy Wilkinson
   27 |  * @author Chris Bono
   28 |  * @since 1.0.0
   29 |  */
   30 | public interface SpringApplicationRunListener {
   31 | 
   32 | 	/**
   33 | 	 * Called immediately when the run method has first started. Can be used for very
   34 | 	 * early initialization.
   35 | 	 * @param bootstrapContext the bootstrap context
   36 | 	 */
   37 | 	default void starting(ConfigurableBootstrapContext bootstrapContext) {
   38 | 	}
   39 | 
   40 | 	/**
   41 | 	 * Called once the environment has been prepared, but before the
   42 | 	 * {@link ApplicationContext} has been created.
   43 | 	 * @param bootstrapContext the bootstrap context
   44 | 	 * @param environment the environment
   45 | 	 */
   46 | 	default void environmentPrepared(ConfigurableBootstrapContext bootstrapContext,
   47 | 			ConfigurableEnvironment environment) {
   48 | 	}
   49 | 
   50 | 	/**
   51 | 	 * Called once the {@link ApplicationContext} has been created and prepared, but
   52 | 	 * before sources have been loaded.
   53 | 	 * @param context the application context
   54 | 	 */
   55 | 	default void contextPrepared(ConfigurableApplicationContext context) {
   56 | 	}
   57 | 
   58 | 	/**
   59 | 	 * Called once the application context has been loaded but before it has been
   60 | 	 * refreshed.
   61 | 	 * @param context the application context
   62 | 	 */
   63 | 	default void contextLoaded(ConfigurableApplicationContext context) {
   64 | 	}
   65 | 
   66 | 	/**
   67 | 	 * The context has been refreshed and the application has started but
   68 | 	 * {@link CommandLineRunner CommandLineRunners} and {@link ApplicationRunner
   69 | 	 * ApplicationRunners} have not been called.
   70 | 	 * @param context the application context.
   71 | 	 * @param timeTaken the time taken to start the application or {@code null} if unknown
   72 | 	 * @since 2.6.0
   73 | 	 */
   74 | 	default void started(ConfigurableApplicationContext context, @Nullable Duration timeTaken) {
   75 | 	}
   76 | 
   77 | 	/**
   78 | 	 * Called immediately before the run method finishes, when the application context has
   79 | 	 * been refreshed and all {@link CommandLineRunner CommandLineRunners} and
   80 | 	 * {@link ApplicationRunner ApplicationRunners} have been called.
   81 | 	 * @param context the application context.
   82 | 	 * @param timeTaken the time taken for the application to be ready or {@code null} if
   83 | 	 * unknown
   84 | 	 * @since 2.6.0
   85 | 	 */
   86 | 	default void ready(ConfigurableApplicationContext context, @Nullable Duration timeTaken) {
   87 | 	}
   88 | 
   89 | 	/**
   90 | 	 * Called when a failure occurs when running the application.
   91 | 	 * @param context the application context or {@code null} if a failure occurred before
   92 | 	 * the context was created
   93 | 	 * @param exception the failure
   94 | 	 * @since 2.0.0
   95 | 	 */
   96 | 	default void failed(@Nullable ConfigurableApplicationContext context, Throwable exception) {
   97 | 	}
   98 | 
   99 | }
```
