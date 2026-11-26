<!-- learning-docs source archive（机械抓取原文，禁止手改代码内容） -->
<!-- origin: org.springframework:spring-aop:7.1.0 | path: org/springframework/aop/framework/AopProxy.java -->
<!-- 正文引用：[[源码:AopProxy.java#L起-L止]] ；代码块头部：(src: AopProxy.java#L起-L止) -->

# AopProxy.java

| 项 | 值 |
| --- | --- |
| 来源 | `org.springframework:spring-aop:7.1.0` |
| 路径 | `org/springframework/aop/framework/AopProxy.java` |
| 行数 | 64 |
| 在线地址 | https://repo1.maven.org/maven2/org/springframework/spring-aop/7.1.0/spring-aop-7.1.0-sources.jar |
| 抓取时间 | 2026-11-25T13:59:56.322Z |

```java
    1 | /*
    2 |  * Copyright 2002-present the original author or authors.
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
   17 | package org.springframework.aop.framework;
   18 | 
   19 | import org.jspecify.annotations.Nullable;
   20 | 
   21 | /**
   22 |  * Delegate interface for a configured AOP proxy, allowing for the creation
   23 |  * of actual proxy objects.
   24 |  *
   25 |  * <p>Out-of-the-box implementations are available for JDK dynamic proxies
   26 |  * and for CGLIB proxies, as applied by {@link DefaultAopProxyFactory}.
   27 |  *
   28 |  * @author Rod Johnson
   29 |  * @author Juergen Hoeller
   30 |  * @see DefaultAopProxyFactory
   31 |  */
   32 | public interface AopProxy {
   33 | 
   34 | 	/**
   35 | 	 * Create a new proxy object.
   36 | 	 * <p>Uses the AopProxy's default class loader (if necessary for proxy creation):
   37 | 	 * usually, the thread context class loader.
   38 | 	 * @return the new proxy object (never {@code null})
   39 | 	 * @see Thread#getContextClassLoader()
   40 | 	 */
   41 | 	Object getProxy();
   42 | 
   43 | 	/**
   44 | 	 * Create a new proxy object.
   45 | 	 * <p>Uses the given class loader (if necessary for proxy creation).
   46 | 	 * {@code null} will simply be passed down and thus lead to the low-level
   47 | 	 * proxy facility's default, which is usually different from the default chosen
   48 | 	 * by the AopProxy implementation's {@link #getProxy()} method.
   49 | 	 * @param classLoader the class loader to create the proxy with
   50 | 	 * (or {@code null} for the low-level proxy facility's default)
   51 | 	 * @return the new proxy object (never {@code null})
   52 | 	 */
   53 | 	Object getProxy(@Nullable ClassLoader classLoader);
   54 | 
   55 | 	/**
   56 | 	 * Determine the proxy class.
   57 | 	 * @param classLoader the class loader to create the proxy class with
   58 | 	 * (or {@code null} for the low-level proxy facility's default)
   59 | 	 * @return the proxy class
   60 | 	 * @since 6.0
   61 | 	 */
   62 | 	Class<?> getProxyClass(@Nullable ClassLoader classLoader);
   63 | 
   64 | }
```
