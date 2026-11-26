<!-- learning-docs source archive（机械抓取原文，禁止手改代码内容） -->
<!-- origin: org.springframework:spring-core:7.1.0 | path: org/springframework/aot/hint/RuntimeHints.java -->
<!-- 正文引用：[[源码:RuntimeHints.java#L起-L止]] ；代码块头部：(src: RuntimeHints.java#L起-L止) -->

# RuntimeHints.java

| 项 | 值 |
| --- | --- |
| 来源 | `org.springframework:spring-core:7.1.0` |
| 路径 | `org/springframework/aot/hint/RuntimeHints.java` |
| 行数 | 91 |
| 在线地址 | https://repo1.maven.org/maven2/org/springframework/spring-core/7.1.0/spring-core-7.1.0-sources.jar |
| 抓取时间 | 2026-11-25T14:05:28.022Z |

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
   19 | /**
   20 |  * Hints that can be used to optimize the application runtime.
   21 |  *
   22 |  * <p>The use of reflection can be recorded for individual members of a type,
   23 |  * lambdas, or broader {@linkplain MemberCategory member categories}.
   24 |  *
   25 |  * <p>Access to resources can be specified using patterns or the base name of a
   26 |  * resource bundle.
   27 |  *
   28 |  * <p>The need for Java serialization or proxies can be recorded as well.
   29 |  *
   30 |  * @author Stephane Nicoll
   31 |  * @author Janne Valkealahti
   32 |  * @since 6.0
   33 |  */
   34 | public class RuntimeHints {
   35 | 
   36 | 	private final ReflectionHints reflection = new ReflectionHints();
   37 | 
   38 | 	private final ResourceHints resources = new ResourceHints();
   39 | 
   40 | 	@SuppressWarnings("removal")
   41 | 	private final SerializationHints serialization = new SerializationHints();
   42 | 
   43 | 	private final ProxyHints proxies = new ProxyHints();
   44 | 
   45 | 	private final ReflectionHints jni = new ReflectionHints();
   46 | 
   47 | 
   48 | 	/**
   49 | 	 * Provide access to reflection-based hints.
   50 | 	 * @return reflection hints
   51 | 	 */
   52 | 	public ReflectionHints reflection() {
   53 | 		return this.reflection;
   54 | 	}
   55 | 
   56 | 	/**
   57 | 	 * Provide access to resource-based hints.
   58 | 	 * @return resource hints
   59 | 	 */
   60 | 	public ResourceHints resources() {
   61 | 		return this.resources;
   62 | 	}
   63 | 
   64 | 	/**
   65 | 	 * Provide access to serialization-based hints.
   66 | 	 * @return serialization hints
   67 | 	 * @deprecated in favor of {@link #reflection()}
   68 | 	 */
   69 | 	@SuppressWarnings("removal")
   70 | 	@Deprecated(since = "7.0.6", forRemoval = true)
   71 | 	public SerializationHints serialization() {
   72 | 		return this.serialization;
   73 | 	}
   74 | 
   75 | 	/**
   76 | 	 * Provide access to proxy-based hints.
   77 | 	 * @return proxy hints
   78 | 	 */
   79 | 	public ProxyHints proxies() {
   80 | 		return this.proxies;
   81 | 	}
   82 | 
   83 | 	/**
   84 | 	 * Provide access to JNI-based hints.
   85 | 	 * @return JNI hints
   86 | 	 */
   87 | 	public ReflectionHints jni() {
   88 | 		return this.jni;
   89 | 	}
   90 | 
   91 | }
```
