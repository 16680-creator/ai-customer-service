<!-- learning-docs source archive（机械抓取原文，禁止手改代码内容） -->
<!-- origin: org.springframework.boot:spring-boot:4.2.0 | path: org/springframework/boot/context/properties/bind/BindHandler.java -->
<!-- 正文引用：[[源码:BindHandler.java#L起-L止]] ；代码块头部：(src: BindHandler.java#L起-L止) -->

# BindHandler.java

| 项 | 值 |
| --- | --- |
| 来源 | `org.springframework.boot:spring-boot:4.2.0` |
| 路径 | `org/springframework/boot/context/properties/bind/BindHandler.java` |
| 行数 | 99 |
| 在线地址 | https://repo1.maven.org/maven2/org/springframework/boot/spring-boot/4.2.0/spring-boot-4.2.0-sources.jar |
| 抓取时间 | 2026-11-25T14:04:53.119Z |

```java
    1 | /*
    2 |  * Copyright 2012-present the original author or authors.
    3 |  */
    4 | 
    5 | package org.springframework.boot.context.properties.bind;
    6 | 
    7 | import org.jspecify.annotations.Nullable;
    8 | 
    9 | import org.springframework.boot.context.properties.source.ConfigurationPropertyName;
   10 | 
   11 | /**
   12 |  * Callback interface that can be used to handle additional logic during element
   13 |  * {@link Binder binding}.
   14 |  *
   15 |  * @author Phillip Webb
   16 |  * @author Madhura Bhave
   17 |  * @since 2.0.0
   18 |  */
   19 | public interface BindHandler {
   20 | 
   21 | 	/**
   22 | 	 * Default no-op bind handler.
   23 | 	 */
   24 | 	BindHandler DEFAULT = new BindHandler() {
   25 | 
   26 | 	};
   27 | 
   28 | 	/**
   29 | 	 * Called when binding of an element starts but before any result has been determined.
   30 | 	 * @param <T> the bindable source type
   31 | 	 * @param name the name of the element being bound
   32 | 	 * @param target the item being bound
   33 | 	 * @param context the bind context
   34 | 	 * @return the actual item that should be used for binding (may be {@code null})
   35 | 	 */
   36 | 	default <T> @Nullable Bindable<T> onStart(ConfigurationPropertyName name, Bindable<T> target, BindContext context) {
   37 | 		return target;
   38 | 	}
   39 | 
   40 | 	/**
   41 | 	 * Called when binding of an element ends with a successful result. Implementations
   42 | 	 * may change the ultimately returned result or perform addition validation.
   43 | 	 * @param name the name of the element being bound
   44 | 	 * @param target the item being bound
   45 | 	 * @param context the bind context
   46 | 	 * @param result the bound result (never {@code null})
   47 | 	 * @return the actual result that should be used (may be {@code null})
   48 | 	 */
   49 | 	default @Nullable Object onSuccess(ConfigurationPropertyName name, Bindable<?> target, BindContext context,
   50 | 			Object result) {
   51 | 		return result;
   52 | 	}
   53 | 
   54 | 	/**
   55 | 	 * Called when binding of an element ends with an unbound result and a newly created
   56 | 	 * instance is about to be returned. Implementations may change the ultimately
   57 | 	 * returned result or perform addition validation.
   58 | 	 * @param name the name of the element being bound
   59 | 	 * @param target the item being bound
   60 | 	 * @param context the bind context
   61 | 	 * @param result the newly created instance (never {@code null})
   62 | 	 * @return the actual result that should be used (must not be {@code null})
   63 | 	 * @since 2.2.2
   64 | 	 */
   65 | 	default Object onCreate(ConfigurationPropertyName name, Bindable<?> target, BindContext context, Object result) {
   66 | 		return result;
   67 | 	}
   68 | 
   69 | 	/**
   70 | 	 * Called when binding fails for any reason (including failures from
   71 | 	 * {@link #onSuccess} or {@link #onCreate} calls). Implementations may choose to
   72 | 	 * swallow exceptions and return an alternative result.
   73 | 	 * @param name the name of the element being bound
   74 | 	 * @param target the item being bound
   75 | 	 * @param context the bind context
   76 | 	 * @param error the cause of the error (if the exception stands it may be re-thrown)
   77 | 	 * @return the actual result that should be used (may be {@code null}).
   78 | 	 * @throws Exception if the binding isn't valid
   79 | 	 */
   80 | 	default @Nullable Object onFailure(ConfigurationPropertyName name, Bindable<?> target, BindContext context,
   81 | 			Exception error) throws Exception {
   82 | 		throw error;
   83 | 	}
   84 | 
   85 | 	/**
   86 | 	 * Called when binding finishes with either bound or unbound result. This method will
   87 | 	 * not be called when binding failed, even if a handler returns a result from
   88 | 	 * {@link #onFailure}.
   89 | 	 * @param name the name of the element being bound
   90 | 	 * @param target the item being bound
   91 | 	 * @param context the bind context
   92 | 	 * @param result the bound result (may be {@code null})
   93 | 	 * @throws Exception if the binding isn't valid
   94 | 	 */
   95 | 	default void onFinish(ConfigurationPropertyName name, Bindable<?> target, BindContext context,
   96 | 			@Nullable Object result) throws Exception {
   97 | 	}
   98 | 
   99 | }
```
