<!-- learning-docs source archive（机械抓取原文，禁止手改代码内容） -->
<!-- origin: org.springframework:spring-web:7.1.0 | path: org/springframework/web/bind/annotation/RestControllerAdvice.java -->
<!-- 正文引用：[[源码:RestControllerAdvice.java#L起-L止]] ；代码块头部：(src: RestControllerAdvice.java#L起-L止) -->

# RestControllerAdvice.java

| 项 | 值 |
| --- | --- |
| 来源 | `org.springframework:spring-web:7.1.0` |
| 路径 | `org/springframework/web/bind/annotation/RestControllerAdvice.java` |
| 行数 | 112 |
| 在线地址 | https://repo1.maven.org/maven2/org/springframework/spring-web/7.1.0/spring-web-7.1.0-sources.jar |
| 抓取时间 | 2026-11-25T14:01:24.318Z |

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
   27 | 
   28 | /**
   29 |  * A shortcut annotation that combines {@link ControllerAdvice @ControllerAdvice}
   30 |  * with {@link ResponseBody @ResponseBody}, in effect simply an
   31 |  * {@code @ControllerAdvice} whose exception handler methods render to the
   32 |  * response body.
   33 |  *
   34 |  * <p>By default, {@code @RestControllerAdvice} applies to any controller,
   35 |  * including {@code @Controller} and {@code @RestController}. Use attributes of
   36 |  * the annotation to apply more specific filtering criteria.
   37 |  *
   38 |  * <p>See {@link ControllerAdvice} for more details.
   39 |  *
   40 |  * @author Rossen Stoyanchev
   41 |  * @author Sam Brannen
   42 |  * @since 4.3
   43 |  * @see RestController
   44 |  * @see ControllerAdvice
   45 |  */
   46 | @Target(ElementType.TYPE)
   47 | @Retention(RetentionPolicy.RUNTIME)
   48 | @Documented
   49 | @ControllerAdvice
   50 | @ResponseBody
   51 | public @interface RestControllerAdvice {
   52 | 
   53 | 	/**
   54 | 	 * Alias for {@link ControllerAdvice#name}.
   55 | 	 * @since 6.1
   56 | 	 */
   57 | 	@AliasFor(annotation = ControllerAdvice.class)
   58 | 	String name() default "";
   59 | 
   60 | 	/**
   61 | 	 * Alias for the {@link #basePackages} attribute.
   62 | 	 * <p>Allows for more concise annotation declarations &mdash; for example,
   63 | 	 * {@code @RestControllerAdvice("org.my.pkg")} is equivalent to
   64 | 	 * {@code @RestControllerAdvice(basePackages = "org.my.pkg")}.
   65 | 	 * @see #basePackages
   66 | 	 */
   67 | 	@AliasFor(annotation = ControllerAdvice.class)
   68 | 	String[] value() default {};
   69 | 
   70 | 	/**
   71 | 	 * Array of base packages.
   72 | 	 * <p>Controllers that belong to those base packages or sub-packages thereof
   73 | 	 * will be included &mdash; for example,
   74 | 	 * {@code @RestControllerAdvice(basePackages = "org.my.pkg")} or
   75 | 	 * {@code @RestControllerAdvice(basePackages = {"org.my.pkg", "org.my.other.pkg"})}.
   76 | 	 * <p>{@link #value} is an alias for this attribute, simply allowing for
   77 | 	 * more concise use of the annotation.
   78 | 	 * <p>Also consider using {@link #basePackageClasses} as a type-safe
   79 | 	 * alternative to String-based package names.
   80 | 	 */
   81 | 	@AliasFor(annotation = ControllerAdvice.class)
   82 | 	String[] basePackages() default {};
   83 | 
   84 | 	/**
   85 | 	 * Type-safe alternative to {@link #basePackages} for specifying the packages
   86 | 	 * in which to select controllers to be advised by the {@code @RestControllerAdvice}
   87 | 	 * annotated class.
   88 | 	 * <p>Consider creating a special no-op marker class or interface in each package
   89 | 	 * that serves no purpose other than being referenced by this attribute.
   90 | 	 */
   91 | 	@AliasFor(annotation = ControllerAdvice.class)
   92 | 	Class<?>[] basePackageClasses() default {};
   93 | 
   94 | 	/**
   95 | 	 * Array of classes.
   96 | 	 * <p>Controllers that are assignable to at least one of the given types
   97 | 	 * will be advised by the {@code @RestControllerAdvice} annotated class.
   98 | 	 */
   99 | 	@AliasFor(annotation = ControllerAdvice.class)
  100 | 	Class<?>[] assignableTypes() default {};
  101 | 
  102 | 	/**
  103 | 	 * Array of annotations.
  104 | 	 * <p>Controllers that are annotated with at least one of the supplied annotation
  105 | 	 * types will be advised by the {@code @RestControllerAdvice} annotated class.
  106 | 	 * <p>Consider creating a custom composed annotation or use a predefined one,
  107 | 	 * like {@link RestController @RestController}.
  108 | 	 */
  109 | 	@AliasFor(annotation = ControllerAdvice.class)
  110 | 	Class<? extends Annotation>[] annotations() default {};
  111 | 
  112 | }
```
