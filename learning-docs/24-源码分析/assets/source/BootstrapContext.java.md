<!-- learning-docs source archive（机械抓取原文，禁止手改代码内容） -->
<!-- origin: org.springframework.boot:spring-boot:4.2.0 | path: org/springframework/boot/bootstrap/BootstrapContext.java -->
<!-- 正文引用：[[源码:BootstrapContext.java#L起-L止]] ；代码块头部：(src: BootstrapContext.java#L起-L止) -->

# BootstrapContext.java

| 项 | 值 |
| --- | --- |
| 来源 | `org.springframework.boot:spring-boot:4.2.0` |
| 路径 | `org/springframework/boot/bootstrap/BootstrapContext.java` |
| 行数 | 82 |
| 在线地址 | https://repo1.maven.org/maven2/org/springframework/boot/spring-boot/4.2.0/spring-boot-4.2.0-sources.jar |
| 抓取时间 | 2026-11-26T09:20:38.507Z |

```java
    1 | /*
    2 |  * Copyright 2012-present the original author or authors.
    3 |  */
    4 | 
    5 | package org.springframework.boot.bootstrap;
    6 | 
    7 | import java.util.function.Supplier;
    8 | 
    9 | import org.jspecify.annotations.Nullable;
   10 | 
   11 | import org.springframework.context.ApplicationContext;
   12 | import org.springframework.core.env.Environment;
   13 | 
   14 | /**
   15 |  * A simple bootstrap context that is available during startup and {@link Environment}
   16 |  * post-processing up to the point that the {@link ApplicationContext} is prepared.
   17 |  * <p>
   18 |  * Provides lazy access to singletons that may be expensive to create, or need to be
   19 |  * shared before the {@link ApplicationContext} is available.
   20 |  * <p>
   21 |  * Instances are registered by type. The contact may return {@code null} values when a
   22 |  * type has been registered but no value is actually supplied.
   23 |  *
   24 |  * @author Phillip Webb
   25 |  * @since 4.0.0
   26 |  * @since 2.4.0
   27 |  * @see BootstrapRegistry
   28 |  */
   29 | public interface BootstrapContext {
   30 | 
   31 | 	/**
   32 | 	 * Return an instance from the context if the type has been registered. The instance
   33 | 	 * will be created if it hasn't been accessed previously.
   34 | 	 * @param <T> the instance type
   35 | 	 * @param type the instance type
   36 | 	 * @return the instance managed by the context, which may be {@code null}
   37 | 	 * @throws IllegalStateException if the type has not been registered
   38 | 	 */
   39 | 	<T> @Nullable T get(Class<T> type) throws IllegalStateException;
   40 | 
   41 | 	/**
   42 | 	 * Return an instance from the context if the type has been registered. The instance
   43 | 	 * will be created if it hasn't been accessed previously.
   44 | 	 * @param <T> the instance type
   45 | 	 * @param type the instance type
   46 | 	 * @param other the instance to use if the type has not been registered
   47 | 	 * @return the instance, which may be {@code null}
   48 | 	 */
   49 | 	<T> @Nullable T getOrElse(Class<T> type, @Nullable T other);
   50 | 
   51 | 	/**
   52 | 	 * Return an instance from the context if the type has been registered. The instance
   53 | 	 * will be created if it hasn't been accessed previously.
   54 | 	 * @param <T> the instance type
   55 | 	 * @param type the instance type
   56 | 	 * @param other a supplier for the instance to use if the type has not been registered
   57 | 	 * @return the instance, which may be {@code null}
   58 | 	 */
   59 | 	<T> @Nullable T getOrElseSupply(Class<T> type, Supplier<@Nullable T> other);
   60 | 
   61 | 	/**
   62 | 	 * Return an instance from the context if the type has been registered. The instance
   63 | 	 * will be created if it hasn't been accessed previously.
   64 | 	 * @param <T> the instance type
   65 | 	 * @param <X> the exception to throw if the type is not registered
   66 | 	 * @param type the instance type
   67 | 	 * @param exceptionSupplier the supplier which will return the exception to be thrown
   68 | 	 * @return the instance managed by the context, which may be {@code null}
   69 | 	 * @throws X if the type has not been registered
   70 | 	 */
   71 | 	<T, X extends Throwable> @Nullable T getOrElseThrow(Class<T> type, Supplier<? extends X> exceptionSupplier)
   72 | 			throws X;
   73 | 
   74 | 	/**
   75 | 	 * Return if a registration exists for the given type.
   76 | 	 * @param <T> the instance type
   77 | 	 * @param type the instance type
   78 | 	 * @return {@code true} if the type has already been registered
   79 | 	 */
   80 | 	<T> boolean isRegistered(Class<T> type);
   81 | 
   82 | }
```
