<!-- learning-docs source archive（机械抓取原文，禁止手改代码内容） -->
<!-- origin: org.springframework:spring-web:7.1.0 | path: org/springframework/web/bind/annotation/ControllerAdvice.java -->
<!-- 正文引用：[[源码:ControllerAdvice.java#L起-L止]] ；代码块头部：(src: ControllerAdvice.java#L起-L止) -->

# ControllerAdvice.java

| 项 | 值 |
| --- | --- |
| 来源 | `org.springframework:spring-web:7.1.0` |
| 路径 | `org/springframework/web/bind/annotation/ControllerAdvice.java` |
| 行数 | 142 |
| 在线地址 | https://repo1.maven.org/maven2/org/springframework/spring-web/7.1.0/spring-web-7.1.0-sources.jar |
| 抓取时间 | 2026-11-25T14:01:23.278Z |

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
   17 | package org.springframework.web.bind.annotation;
   18 | 
   19 | import java.lang.annotation.Annotation;
   20 | import java.lang.annotation.Documented;
   21 | import java.lang.annotation.ElementType;
   22 | import java.lang.annotation.Retention;
   23 | import java.lang.annotation.RetentionPolicy;
   24 | import java.lang.annotation.Target;
   25 | 
   26 | import org.springframework.core.annotation.AliasFor;
   27 | import org.springframework.stereotype.Component;
   28 | 
   29 | /**
   30 |  * Specialization of {@link Component @Component} for classes that declare
   31 |  * {@link ExceptionHandler @ExceptionHandler}, {@link InitBinder @InitBinder}, or
   32 |  * {@link ModelAttribute @ModelAttribute} methods to be shared across
   33 |  * multiple {@code @Controller} classes.
   34 |  *
   35 |  * <p>Classes annotated with {@code @ControllerAdvice} can be declared explicitly
   36 |  * as Spring beans or auto-detected via classpath scanning. All such beans are
   37 |  * sorted based on {@link org.springframework.core.Ordered Ordered} semantics or
   38 |  * {@link org.springframework.core.annotation.Order @Order} /
   39 |  * {@link jakarta.annotation.Priority @Priority} declarations, with {@code Ordered}
   40 |  * semantics taking precedence over {@code @Order} / {@code @Priority} declarations.
   41 |  * {@code @ControllerAdvice} beans are then applied in that order at runtime.
   42 |  * Note, however, that {@code @ControllerAdvice} beans that implement
   43 |  * {@link org.springframework.core.PriorityOrdered PriorityOrdered} are <em>not</em>
   44 |  * given priority over {@code @ControllerAdvice} beans that implement {@code Ordered}.
   45 |  * In addition, {@code Ordered} is not honored for scoped {@code @ControllerAdvice}
   46 |  * beans &mdash; for example if such a bean has been configured as a request-scoped
   47 |  * or session-scoped bean.  For handling exceptions, an {@code @ExceptionHandler}
   48 |  * will be picked on the first advice with a matching exception handler method. For
   49 |  * model attributes and data binding initialization, {@code @ModelAttribute} and
   50 |  * {@code @InitBinder} methods will follow {@code @ControllerAdvice} order.
   51 |  *
   52 |  * <p>Note: For {@code @ExceptionHandler} methods, a root exception match will be
   53 |  * preferred to just matching a cause of the current exception, among the handler
   54 |  * methods of a particular advice bean. However, a cause match on a higher-priority
   55 |  * advice will still be preferred over any match (whether root or cause level)
   56 |  * on a lower-priority advice bean. As a consequence, please declare your primary
   57 |  * root exception mappings on a prioritized advice bean with a corresponding order.
   58 |  *
   59 |  * <p>By default, the methods in an {@code @ControllerAdvice} apply globally to
   60 |  * all controllers. Use selectors such as {@link #annotations},
   61 |  * {@link #basePackageClasses}, and {@link #basePackages} (or its alias
   62 |  * {@link #value}) to define a more narrow subset of targeted controllers.
   63 |  * If multiple selectors are declared, boolean {@code OR} logic is applied, meaning
   64 |  * selected controllers should match at least one selector. Note that selector checks
   65 |  * are performed at runtime, so adding many selectors may negatively impact
   66 |  * performance and add complexity.
   67 |  *
   68 |  * @author Rossen Stoyanchev
   69 |  * @author Brian Clozel
   70 |  * @author Sam Brannen
   71 |  * @since 3.2
   72 |  * @see org.springframework.stereotype.Controller
   73 |  * @see RestControllerAdvice
   74 |  */
   75 | @Target(ElementType.TYPE)
   76 | @Retention(RetentionPolicy.RUNTIME)
   77 | @Documented
   78 | @Component
   79 | public @interface ControllerAdvice {
   80 | 
   81 | 	/**
   82 | 	 * Alias for {@link Component#value}.
   83 | 	 * @since 6.1
   84 | 	 */
   85 | 	@AliasFor(annotation = Component.class, attribute = "value")
   86 | 	String name() default "";
   87 | 
   88 | 	/**
   89 | 	 * Alias for the {@link #basePackages} attribute.
   90 | 	 * <p>Allows for more concise annotation declarations &mdash; for example,
   91 | 	 * {@code @ControllerAdvice("org.my.pkg")} is equivalent to
   92 | 	 * {@code @ControllerAdvice(basePackages = "org.my.pkg")}.
   93 | 	 * @since 4.0
   94 | 	 * @see #basePackages
   95 | 	 */
   96 | 	@AliasFor("basePackages")
   97 | 	String[] value() default {};
   98 | 
   99 | 	/**
  100 | 	 * Array of base packages.
  101 | 	 * <p>Controllers that belong to those base packages or sub-packages thereof
  102 | 	 * will be included &mdash; for example,
  103 | 	 * {@code @ControllerAdvice(basePackages = "org.my.pkg")} or
  104 | 	 * {@code @ControllerAdvice(basePackages = {"org.my.pkg", "org.my.other.pkg"})}.
  105 | 	 * <p>{@link #value} is an alias for this attribute, simply allowing for
  106 | 	 * more concise use of the annotation.
  107 | 	 * <p>Also consider using {@link #basePackageClasses} as a type-safe
  108 | 	 * alternative to String-based package names.
  109 | 	 * @since 4.0
  110 | 	 */
  111 | 	@AliasFor("value")
  112 | 	String[] basePackages() default {};
  113 | 
  114 | 	/**
  115 | 	 * Type-safe alternative to {@link #basePackages} for specifying the packages
  116 | 	 * in which to select controllers to be advised by the {@code @ControllerAdvice}
  117 | 	 * annotated class.
  118 | 	 * <p>Consider creating a special no-op marker class or interface in each package
  119 | 	 * that serves no purpose other than being referenced by this attribute.
  120 | 	 * @since 4.0
  121 | 	 */
  122 | 	Class<?>[] basePackageClasses() default {};
  123 | 
  124 | 	/**
  125 | 	 * Array of classes.
  126 | 	 * <p>Controllers that are assignable to at least one of the given types
  127 | 	 * will be advised by the {@code @ControllerAdvice} annotated class.
  128 | 	 * @since 4.0
  129 | 	 */
  130 | 	Class<?>[] assignableTypes() default {};
  131 | 
  132 | 	/**
  133 | 	 * Array of annotation types.
  134 | 	 * <p>Controllers that are annotated with at least one of the supplied annotation
  135 | 	 * types will be advised by the {@code @ControllerAdvice} annotated class.
  136 | 	 * <p>Consider creating a custom composed annotation or use a predefined one,
  137 | 	 * like {@link RestController @RestController}.
  138 | 	 * @since 4.0
  139 | 	 */
  140 | 	Class<? extends Annotation>[] annotations() default {};
  141 | 
  142 | }
```
