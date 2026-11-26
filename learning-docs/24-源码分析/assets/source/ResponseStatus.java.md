<!-- learning-docs source archive（机械抓取原文，禁止手改代码内容） -->
<!-- origin: org.springframework:spring-web:7.1.0 | path: org/springframework/web/bind/annotation/ResponseStatus.java -->
<!-- 正文引用：[[源码:ResponseStatus.java#L起-L止]] ；代码块头部：(src: ResponseStatus.java#L起-L止) -->

# ResponseStatus.java

| 项 | 值 |
| --- | --- |
| 来源 | `org.springframework:spring-web:7.1.0` |
| 路径 | `org/springframework/web/bind/annotation/ResponseStatus.java` |
| 行数 | 89 |
| 在线地址 | https://repo1.maven.org/maven2/org/springframework/spring-web/7.1.0/spring-web-7.1.0-sources.jar |
| 抓取时间 | 2026-11-25T14:01:23.560Z |

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
   19 | import java.lang.annotation.Documented;
   20 | import java.lang.annotation.ElementType;
   21 | import java.lang.annotation.Retention;
   22 | import java.lang.annotation.RetentionPolicy;
   23 | import java.lang.annotation.Target;
   24 | 
   25 | import org.springframework.core.annotation.AliasFor;
   26 | import org.springframework.http.HttpStatus;
   27 | 
   28 | /**
   29 |  * Marks a method or exception class with the status {@link #code} and
   30 |  * {@link #reason} that should be returned.
   31 |  *
   32 |  * <p>The status code is applied to the HTTP response when the handler
   33 |  * method is invoked, but does not override status information set by other
   34 |  * means, such as {@code ResponseEntity} or {@code "redirect:"}.
   35 |  *
   36 |  * <p><strong>Warning</strong>: when using this annotation on an exception
   37 |  * class, or when setting the {@code reason} attribute of this annotation,
   38 |  * the {@code HttpServletResponse.sendError} method will be used.
   39 |  *
   40 |  * <p>With {@code HttpServletResponse.sendError}, the response is considered
   41 |  * complete and should not be written to any further. Furthermore, the Servlet
   42 |  * container will typically write an HTML error page therefore making the
   43 |  * use of a {@code reason} unsuitable for REST APIs. For such cases it is
   44 |  * preferable to use a {@link org.springframework.http.ResponseEntity} as
   45 |  * a return type and avoid the use of {@code @ResponseStatus} altogether.
   46 |  *
   47 |  * <p>Note that a controller class may also be annotated with
   48 |  * {@code @ResponseStatus} which is then inherited by all {@code @RequestMapping}
   49 |  * and {@code @ExceptionHandler} methods in that class and its subclasses unless
   50 |  * overridden by a local {@code @ResponseStatus} declaration on the method.
   51 |  *
   52 |  * @author Arjen Poutsma
   53 |  * @author Sam Brannen
   54 |  * @since 3.0
   55 |  * @see org.springframework.web.servlet.mvc.annotation.ResponseStatusExceptionResolver
   56 |  * @see jakarta.servlet.http.HttpServletResponse#sendError(int, String)
   57 |  */
   58 | @Target({ElementType.TYPE, ElementType.METHOD})
   59 | @Retention(RetentionPolicy.RUNTIME)
   60 | @Documented
   61 | public @interface ResponseStatus {
   62 | 
   63 | 	/**
   64 | 	 * Alias for {@link #code}.
   65 | 	 */
   66 | 	@AliasFor("code")
   67 | 	HttpStatus value() default HttpStatus.INTERNAL_SERVER_ERROR;
   68 | 
   69 | 	/**
   70 | 	 * The status <em>code</em> to use for the response.
   71 | 	 * <p>Default is {@link HttpStatus#INTERNAL_SERVER_ERROR}, which should
   72 | 	 * typically be changed to something more appropriate.
   73 | 	 * @since 4.2
   74 | 	 * @see jakarta.servlet.http.HttpServletResponse#setStatus(int)
   75 | 	 * @see jakarta.servlet.http.HttpServletResponse#sendError(int)
   76 | 	 */
   77 | 	@AliasFor("value")
   78 | 	HttpStatus code() default HttpStatus.INTERNAL_SERVER_ERROR;
   79 | 
   80 | 	/**
   81 | 	 * The <em>reason</em> to be used for the response.
   82 | 	 * <p>Defaults to an empty string which will be ignored. Set the reason to a
   83 | 	 * non-empty value to have it used to send a Servlet container error page.
   84 | 	 * In this case, the return value of the handler method will be ignored.
   85 | 	 * @see jakarta.servlet.http.HttpServletResponse#sendError(int, String)
   86 | 	 */
   87 | 	String reason() default "";
   88 | 
   89 | }
```
