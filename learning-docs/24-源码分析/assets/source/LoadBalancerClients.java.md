<!-- learning-docs source archive（机械抓取原文，禁止手改代码内容） -->
<!-- origin: org.springframework.cloud:spring-cloud-loadbalancer:5.1.0 | path: org/springframework/cloud/loadbalancer/annotation/LoadBalancerClients.java -->
<!-- 正文引用：[[源码:LoadBalancerClients.java#L起-L止]] ；代码块头部：(src: LoadBalancerClients.java#L起-L止) -->

# LoadBalancerClients.java

| 项 | 值 |
| --- | --- |
| 来源 | `org.springframework.cloud:spring-cloud-loadbalancer:5.1.0` |
| 路径 | `org/springframework/cloud/loadbalancer/annotation/LoadBalancerClients.java` |
| 行数 | 54 |
| 在线地址 | https://repo1.maven.org/maven2/org/springframework/cloud/spring-cloud-loadbalancer/5.1.0/spring-cloud-loadbalancer-5.1.0-sources.jar |
| 抓取时间 | 2026-11-25T15:33:02.885Z |

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
   28 | 
   29 | /**
   30 |  * Convenience annotation that allows user to combine multiple
   31 |  * <code>@LoadBalancerClient</code> annotations on a single class (including in Java 7).
   32 |  *
   33 |  * @author Dave Syer
   34 |  */
   35 | @Configuration(proxyBeanMethods = false)
   36 | @Retention(RetentionPolicy.RUNTIME)
   37 | @Target({ ElementType.TYPE })
   38 | @Documented
   39 | @Import(LoadBalancerClientConfigurationRegistrar.class)
   40 | public @interface LoadBalancerClients {
   41 | 
   42 | 	LoadBalancerClient[] value() default {};
   43 | 
   44 | 	/**
   45 | 	 * {@link LoadBalancerClientConfigurationRegistrar} creates a
   46 | 	 * {@link LoadBalancerClientSpecification} with this as an argument. These in turn are
   47 | 	 * added as default contexts in {@link LoadBalancerClientFactory}. Configuration
   48 | 	 * defined in these classes are used as defaults if values aren't defined via
   49 | 	 * {@link LoadBalancerClient#configuration()}
   50 | 	 * @return classes for default configurations
   51 | 	 */
   52 | 	Class<?>[] defaultConfiguration() default {};
   53 | 
   54 | }
```
