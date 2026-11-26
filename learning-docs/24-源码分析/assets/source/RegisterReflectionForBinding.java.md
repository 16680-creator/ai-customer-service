<!-- learning-docs source archive（机械抓取原文，禁止手改代码内容） -->
<!-- origin: org.springframework:spring-core:7.1.0 | path: org/springframework/aot/hint/annotation/RegisterReflectionForBinding.java -->
<!-- 正文引用：[[源码:RegisterReflectionForBinding.java#L起-L止]] ；代码块头部：(src: RegisterReflectionForBinding.java#L起-L止) -->

# RegisterReflectionForBinding.java

| 项 | 值 |
| --- | --- |
| 来源 | `org.springframework:spring-core:7.1.0` |
| 路径 | `org/springframework/aot/hint/annotation/RegisterReflectionForBinding.java` |
| 行数 | 103 |
| 在线地址 | https://repo1.maven.org/maven2/org/springframework/spring-core/7.1.0/spring-core-7.1.0-sources.jar |
| 抓取时间 | 2026-11-25T14:05:28.509Z |

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
   28 |  * Register reflection hints for data binding or reflection-based serialization
   29 |  * against an arbitrary number of target classes.
   30 |  *
   31 |  * <p>For each class hints are registered for constructors, fields, properties,
   32 |  * and record components. Hints are also registered for types transitively used
   33 |  * on properties and record components.
   34 |  *
   35 |  * <p>You can use this annotation on any bean that is contributed to the context:
   36 |  * <pre><code class="java">
   37 |  * &#064;Configuration
   38 |  * &#064;RegisterReflectionForBinding({Foo.class, Bar.class})
   39 |  * class MyConfig {
   40 |  *     // ...
   41 |  * }</code></pre>
   42 |  *
   43 |  * <p>If scanning of {@link Reflective} is enabled, any type in the configured
   44 |  * packages can use this annotation as well.
   45 |  *
   46 |  * <p>When the annotated element is a type, the type itself is registered if no
   47 |  * candidates are provided:<pre><code class="java">
   48 |  * &#064;Component
   49 |  * &#064;RegisterReflectionForBinding
   50 |  * class MyBean {
   51 |  *     // ...
   52 |  * }</code></pre>
   53 |  *
   54 |  * The annotation can also be specified on a method. In that case, at least one
   55 |  * target class must be specified:<pre><code class="java">
   56 |  * &#064;Component
   57 |  * class MyService {
   58 |  *
   59 |  *     &#064;RegisterReflectionForBinding(Baz.class)
   60 |  *     public Baz process() {
   61 |  *         // ...
   62 |  *     }
   63 |  *
   64 |  * }</code></pre>
   65 |  *
   66 |  * <p>The annotated element can also be any test class that uses the <em>Spring
   67 |  * TestContext Framework</em> to load an {@code ApplicationContext}.
   68 |  *
   69 |  * @author Sebastien Deleuze
   70 |  * @author Stephane Nicoll
   71 |  * @since 6.0
   72 |  * @see org.springframework.aot.hint.BindingReflectionHintsRegistrar
   73 |  * @see RegisterReflection @RegisterReflection
   74 |  */
   75 | @Target({ElementType.TYPE, ElementType.METHOD})
   76 | @Retention(RetentionPolicy.RUNTIME)
   77 | @Documented
   78 | @RegisterReflection
   79 | @Reflective(RegisterReflectionForBindingProcessor.class)
   80 | public @interface RegisterReflectionForBinding {
   81 | 
   82 | 	/**
   83 | 	 * Alias for {@link #classes()}.
   84 | 	 */
   85 | 	@AliasFor(annotation = RegisterReflection.class, attribute = "classes")
   86 | 	Class<?>[] value() default {};
   87 | 
   88 | 	/**
   89 | 	 * Classes for which reflection hints should be registered.
   90 | 	 * <p>At least one class must be specified either via {@link #value} or {@code classes}.
   91 | 	 * @see #value()
   92 | 	 */
   93 | 	@AliasFor(annotation = RegisterReflection.class, attribute = "classes")
   94 | 	Class<?>[] classes() default {};
   95 | 
   96 | 	/**
   97 | 	 * Alternative to {@link #classes()} to specify the classes as class names.
   98 | 	 * @see #classes()
   99 | 	 */
  100 | 	@AliasFor(annotation = RegisterReflection.class, attribute = "classNames")
  101 | 	String[] classNames() default {};
  102 | 
  103 | }
```
