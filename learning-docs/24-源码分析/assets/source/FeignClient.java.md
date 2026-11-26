<!-- learning-docs source archive（机械抓取原文，禁止手改代码内容） -->
<!-- origin: org.springframework.cloud:spring-cloud-openfeign-core:5.1.0 | path: org/springframework/cloud/openfeign/FeignClient.java -->
<!-- 正文引用：[[源码:FeignClient.java#L起-L止]] ；代码块头部：(src: FeignClient.java#L起-L止) -->

# FeignClient.java

| 项 | 值 |
| --- | --- |
| 来源 | `org.springframework.cloud:spring-cloud-openfeign-core:5.1.0` |
| 路径 | `org/springframework/cloud/openfeign/FeignClient.java` |
| 行数 | 119 |
| 在线地址 | https://repo1.maven.org/maven2/org/springframework/cloud/spring-cloud-openfeign-core/5.1.0/spring-cloud-openfeign-core-5.1.0-sources.jar |
| 抓取时间 | 2026-11-25T15:33:24.715Z |

```java
    1 | /*
    2 |  * Copyright 2013-present the original author or authors.
    3 |  *
    4 |  * Licensed under the Apache License, Version 2.0 (the "License");
    5 |  * you may not use this file except in compliance with the License.
    6 |  * You may obtain a copy of the License at
    7 |  *
    8 |  *      https://www.apache.org/licenses/LICENSE-2.0
    9 |  *
   10 |  * Unless required by applicable law or agreed to in writing, software
   11 |  * distributed under the License is distributed on an "AS IS" BASIS,
   12 |  * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
   13 |  * See the License for the specific language governing permissions and
   14 |  * limitations under the License.
   15 |  */
   16 | 
   17 | package org.springframework.cloud.openfeign;
   18 | 
   19 | import java.lang.annotation.Documented;
   20 | import java.lang.annotation.ElementType;
   21 | import java.lang.annotation.Inherited;
   22 | import java.lang.annotation.Retention;
   23 | import java.lang.annotation.RetentionPolicy;
   24 | import java.lang.annotation.Target;
   25 | 
   26 | import org.springframework.core.annotation.AliasFor;
   27 | 
   28 | /**
   29 |  * Annotation for interfaces declaring that a REST client with that interface should be
   30 |  * created (e.g. for autowiring into another component). If SC LoadBalancer is available
   31 |  * it will be used to load balance the backend requests, and the load balancer can be
   32 |  * configured using the same name (i.e. value) as the feign client.
   33 |  *
   34 |  * @author Spencer Gibb
   35 |  * @author Venil Noronha
   36 |  * @author Olga Maciaszek-Sharma
   37 |  */
   38 | @Target(ElementType.TYPE)
   39 | @Retention(RetentionPolicy.RUNTIME)
   40 | @Documented
   41 | @Inherited
   42 | public @interface FeignClient {
   43 | 
   44 | 	/**
   45 | 	 * The name of the service with optional protocol prefix. Synonym for {@link #name()
   46 | 	 * name}. A name must be specified for all clients, whether or not a url is provided.
   47 | 	 * Can be specified as property key, eg: ${propertyKey}.
   48 | 	 * @return the name of the service with optional protocol prefix
   49 | 	 */
   50 | 	@AliasFor("name")
   51 | 	String value() default "";
   52 | 
   53 | 	/**
   54 | 	 * This will be used as the bean name instead of name if present, but will not be used
   55 | 	 * as a service id.
   56 | 	 * @return bean name instead of name if present
   57 | 	 */
   58 | 	String contextId() default "";
   59 | 
   60 | 	/**
   61 | 	 * @return The service id with optional protocol prefix. Synonym for {@link #value()
   62 | 	 * value}.
   63 | 	 */
   64 | 	@AliasFor("value")
   65 | 	String name() default "";
   66 | 
   67 | 	/**
   68 | 	 * @return the <code>@Qualifiers</code> value for the feign client.
   69 | 	 */
   70 | 	String[] qualifiers() default {};
   71 | 
   72 | 	/**
   73 | 	 * @return an absolute URL or resolvable hostname (the protocol is optional).
   74 | 	 */
   75 | 	String url() default "";
   76 | 
   77 | 	/**
   78 | 	 * @return whether 404s should be decoded instead of throwing FeignExceptions
   79 | 	 */
   80 | 	boolean dismiss404() default false;
   81 | 
   82 | 	/**
   83 | 	 * A custom configuration class for the feign client. Can contain override
   84 | 	 * <code>@Bean</code> definition for the pieces that make up the client, for instance
   85 | 	 * {@link feign.codec.Decoder}, {@link feign.codec.Encoder}, {@link feign.Contract}.
   86 | 	 *
   87 | 	 * @see FeignClientsConfiguration for the defaults
   88 | 	 * @return list of configurations for feign client
   89 | 	 */
   90 | 	Class<?>[] configuration() default {};
   91 | 
   92 | 	/**
   93 | 	 * Fallback class for the specified Feign client interface. The fallback class must
   94 | 	 * implement the interface annotated by this annotation and be a valid spring bean.
   95 | 	 * @return fallback class for the specified Feign client interface
   96 | 	 */
   97 | 	Class<?> fallback() default void.class;
   98 | 
   99 | 	/**
  100 | 	 * Define a fallback factory for the specified Feign client interface. The fallback
  101 | 	 * factory must produce instances of fallback classes that implement the interface
  102 | 	 * annotated by {@link FeignClient}. The fallback factory must be a valid spring bean.
  103 | 	 *
  104 | 	 * @see FallbackFactory for details.
  105 | 	 * @return fallback factory for the specified Feign client interface
  106 | 	 */
  107 | 	Class<?> fallbackFactory() default void.class;
  108 | 
  109 | 	/**
  110 | 	 * @return path prefix to be used by all method-level mappings.
  111 | 	 */
  112 | 	String path() default "";
  113 | 
  114 | 	/**
  115 | 	 * @return whether to mark the feign proxy as a primary bean. Defaults to true.
  116 | 	 */
  117 | 	boolean primary() default true;
  118 | 
  119 | }
```
