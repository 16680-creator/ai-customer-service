<!-- learning-docs source archive（机械抓取原文，禁止手改代码内容） -->
<!-- origin: org.springframework:spring-core:7.1.0 | path: org/springframework/aot/hint/annotation/Reflective.java -->
<!-- 正文引用：[[源码:Reflective.java#L起-L止]] ；代码块头部：(src: Reflective.java#L起-L止) -->

# Reflective.java

| 项 | 值 |
| --- | --- |
| 来源 | `org.springframework:spring-core:7.1.0` |
| 路径 | `org/springframework/aot/hint/annotation/Reflective.java` |
| 行数 | 60 |
| 在线地址 | https://repo1.maven.org/maven2/org/springframework/spring-core/7.1.0/spring-core-7.1.0-sources.jar |
| 抓取时间 | 2026-11-25T14:05:28.373Z |

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
   17 | package org.springframework.aot.hint.annotation;
   18 | 
   19 | import java.lang.annotation.Documented;
   20 | import java.lang.annotation.ElementType;
   21 | import java.lang.annotation.Retention;
   22 | import java.lang.annotation.RetentionPolicy;
   23 | import java.lang.annotation.Target;
   24 | 
   25 | import org.springframework.core.annotation.AliasFor;
   26 | 
   27 | /**
   28 |  * Indicate that the annotated element requires reflection.
   29 |  *
   30 |  * <p>When present, either directly or as a meta-annotation, this annotation
   31 |  * triggers the configured {@linkplain ReflectiveProcessor processors} against
   32 |  * the annotated element. By default, a reflection hint is registered for the
   33 |  * annotated element so that it can be discovered and invoked if necessary.
   34 |  *
   35 |  * @author Stephane Nicoll
   36 |  * @author Sam Brannen
   37 |  * @since 6.0
   38 |  * @see ReflectiveRuntimeHintsRegistrar
   39 |  * @see RegisterReflection @RegisterReflection
   40 |  * @see RegisterReflectionForBinding @RegisterReflectionForBinding
   41 |  */
   42 | @Target({ElementType.ANNOTATION_TYPE, ElementType.TYPE, ElementType.CONSTRUCTOR, ElementType.FIELD, ElementType.METHOD})
   43 | @Retention(RetentionPolicy.RUNTIME)
   44 | @Documented
   45 | public @interface Reflective {
   46 | 
   47 | 	/**
   48 | 	 * Alias for {@link #processors()}.
   49 | 	 */
   50 | 	@AliasFor("processors")
   51 | 	Class<? extends ReflectiveProcessor>[] value() default SimpleReflectiveProcessor.class;
   52 | 
   53 | 	/**
   54 | 	 * {@link ReflectiveProcessor} implementations to invoke against the
   55 | 	 * annotated element.
   56 | 	 */
   57 | 	@AliasFor("value")
   58 | 	Class<? extends ReflectiveProcessor>[] processors() default SimpleReflectiveProcessor.class;
   59 | 
   60 | }
```
