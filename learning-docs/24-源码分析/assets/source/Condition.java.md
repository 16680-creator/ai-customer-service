<!-- learning-docs source archive（机械抓取原文，禁止手改代码内容） -->
<!-- origin: org.springframework:spring-context:7.1.0 | path: org/springframework/context/annotation/Condition.java -->
<!-- 正文引用：[[源码:Condition.java#L起-L止]] ；代码块头部：(src: Condition.java#L起-L止) -->

# Condition.java

| 项 | 值 |
| --- | --- |
| 来源 | `org.springframework:spring-context:7.1.0` |
| 路径 | `org/springframework/context/annotation/Condition.java` |
| 行数 | 61 |
| 在线地址 | https://repo1.maven.org/maven2/org/springframework/spring-context/7.1.0/spring-context-7.1.0-sources.jar |
| 抓取时间 | 2026-11-25T16:34:21.941Z |

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
   17 | package org.springframework.context.annotation;
   18 | 
   19 | import org.springframework.beans.factory.config.BeanFactoryPostProcessor;
   20 | import org.springframework.core.type.AnnotatedTypeMetadata;
   21 | 
   22 | /**
   23 |  * A single condition that must be {@linkplain #matches matched} in order for a
   24 |  * component to be registered.
   25 |  *
   26 |  * <p>Conditions are checked immediately before the bean definition is due to be
   27 |  * registered and are free to veto registration based on any criteria that can
   28 |  * be determined at that point.
   29 |  *
   30 |  * <p>Conditions must follow the same restrictions as a {@link BeanFactoryPostProcessor}
   31 |  * and take care to never interact with bean instances. For more fine-grained control
   32 |  * over conditions that interact with {@code @Configuration} beans, consider implementing
   33 |  * the {@link ConfigurationCondition} interface.
   34 |  *
   35 |  * <p>Multiple conditions on a given class or on a given method will be ordered
   36 |  * according to the semantics of Spring's {@link org.springframework.core.Ordered}
   37 |  * interface and {@link org.springframework.core.annotation.Order @Order} annotation.
   38 |  * See {@link org.springframework.core.annotation.AnnotationAwareOrderComparator}
   39 |  * for details.
   40 |  *
   41 |  * @author Phillip Webb
   42 |  * @author Sam Brannen
   43 |  * @since 4.0
   44 |  * @see ConfigurationCondition
   45 |  * @see Conditional
   46 |  * @see ConditionContext
   47 |  */
   48 | @FunctionalInterface
   49 | public interface Condition {
   50 | 
   51 | 	/**
   52 | 	 * Determine if the condition matches.
   53 | 	 * @param context the condition context
   54 | 	 * @param metadata the metadata of the {@link org.springframework.core.type.AnnotationMetadata class}
   55 | 	 * or {@link org.springframework.core.type.MethodMetadata method} being checked
   56 | 	 * @return {@code true} if the condition matches and the component can be registered,
   57 | 	 * or {@code false} to veto the annotated component's registration
   58 | 	 */
   59 | 	boolean matches(ConditionContext context, AnnotatedTypeMetadata metadata);
   60 | 
   61 | }
```
