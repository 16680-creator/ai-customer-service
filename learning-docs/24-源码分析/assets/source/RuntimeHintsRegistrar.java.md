<!-- learning-docs source archive（机械抓取原文，禁止手改代码内容） -->
<!-- origin: org.springframework:spring-core:7.1.0 | path: org/springframework/aot/hint/RuntimeHintsRegistrar.java -->
<!-- 正文引用：[[源码:RuntimeHintsRegistrar.java#L起-L止]] ；代码块头部：(src: RuntimeHintsRegistrar.java#L起-L止) -->

# RuntimeHintsRegistrar.java

| 项 | 值 |
| --- | --- |
| 来源 | `org.springframework:spring-core:7.1.0` |
| 路径 | `org/springframework/aot/hint/RuntimeHintsRegistrar.java` |
| 行数 | 46 |
| 在线地址 | https://repo1.maven.org/maven2/org/springframework/spring-core/7.1.0/spring-core-7.1.0-sources.jar |
| 抓取时间 | 2026-11-25T14:05:28.127Z |

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
   17 | package org.springframework.aot.hint;
   18 | 
   19 | import org.jspecify.annotations.Nullable;
   20 | 
   21 | /**
   22 |  * Contract for registering {@link RuntimeHints} based on the {@link ClassLoader}
   23 |  * of the deployment unit. Implementations should, if possible, use the specified
   24 |  * {@link ClassLoader} to determine if hints have to be contributed.
   25 |  *
   26 |  * <p>Implementations of this interface can be registered dynamically by using
   27 |  * {@link org.springframework.context.annotation.ImportRuntimeHints @ImportRuntimeHints}
   28 |  * or statically in {@code META-INF/spring/aot.factories} by using the fully-qualified
   29 |  * class name of this interface as the key. A standard no-arg constructor is required
   30 |  * for implementations.
   31 |  *
   32 |  * @author Brian Clozel
   33 |  * @author Stephane Nicoll
   34 |  * @since 6.0
   35 |  */
   36 | @FunctionalInterface
   37 | public interface RuntimeHintsRegistrar {
   38 | 
   39 | 	/**
   40 | 	 * Contribute hints to the given {@link RuntimeHints} instance.
   41 | 	 * @param hints the hints contributed so far for the deployment unit
   42 | 	 * @param classLoader the ClassLoader to use, or {@code null} for the default
   43 | 	 */
   44 | 	void registerHints(RuntimeHints hints, @Nullable ClassLoader classLoader);
   45 | 
   46 | }
```
