<!-- learning-docs source archive（机械抓取原文，禁止手改代码内容） -->
<!-- origin: org.springframework.cloud:spring-cloud-loadbalancer:5.1.0 | path: org/springframework/cloud/loadbalancer/annotation/LoadBalancerClient.java -->
<!-- 正文引用：[[源码:LoadBalancerClient.java#L起-L止]] ；代码块头部：(src: LoadBalancerClient.java#L起-L止) -->

# LoadBalancerClient.java

| 项 | 值 |
| --- | --- |
| 来源 | `org.springframework.cloud:spring-cloud-loadbalancer:5.1.0` |
| 路径 | `org/springframework/cloud/loadbalancer/annotation/LoadBalancerClient.java` |
| 行数 | 70 |
| 在线地址 | https://repo1.maven.org/maven2/org/springframework/cloud/spring-cloud-loadbalancer/5.1.0/spring-cloud-loadbalancer-5.1.0-sources.jar |
| 抓取时间 | 2026-11-26T06:35:53.074Z |

```java
    1 | /*
    2 |  * Copyright 2012-present the original author or authors.
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
   17 | package org.springframework.cloud.loadbalancer.annotation;
   18 | 
   19 | import java.lang.annotation.Documented;
   20 | import java.lang.annotation.ElementType;
   21 | import java.lang.annotation.Retention;
   22 | import java.lang.annotation.RetentionPolicy;
   23 | import java.lang.annotation.Target;
   24 | 
   25 | import org.springframework.cloud.loadbalancer.support.LoadBalancerClientFactory;
   26 | import org.springframework.context.annotation.Configuration;
   27 | import org.springframework.context.annotation.Import;
   28 | import org.springframework.core.annotation.AliasFor;
   29 | 
   30 | /**
   31 |  * Declarative configuration for a load balancer client. Add this annotation to any
   32 |  * <code>@Configuration</code> and then inject a {@link LoadBalancerClientFactory} to
   33 |  * access the client that is created.
   34 |  *
   35 |  * @author Dave Syer
   36 |  */
   37 | @Configuration(proxyBeanMethods = false)
   38 | @Import(LoadBalancerClientConfigurationRegistrar.class)
   39 | @Target(ElementType.TYPE)
   40 | @Retention(RetentionPolicy.RUNTIME)
   41 | @Documented
   42 | public @interface LoadBalancerClient {
   43 | 
   44 | 	/**
   45 | 	 * Synonym for name (the name of the client).
   46 | 	 *
   47 | 	 * @see #name()
   48 | 	 * @return the name of the load balancer client
   49 | 	 */
   50 | 	@AliasFor("name")
   51 | 	String value() default "";
   52 | 
   53 | 	/**
   54 | 	 * The name of the load balancer client, uniquely identifying a set of client
   55 | 	 * resources, including a load balancer.
   56 | 	 * @return the name of the load balancer client
   57 | 	 */
   58 | 	@AliasFor("value")
   59 | 	String name() default "";
   60 | 
   61 | 	/**
   62 | 	 * A custom <code>@Configuration</code> for the load balancer client. Can contain
   63 | 	 * override <code>@Bean</code> definition for the pieces that make up the client.
   64 | 	 *
   65 | 	 * @see LoadBalancerClientConfiguration for the defaults
   66 | 	 * @return configuration classes for the load balancer client.
   67 | 	 */
   68 | 	Class<?>[] configuration() default {};
   69 | 
   70 | }
```
