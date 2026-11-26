<!-- learning-docs source archive（机械抓取原文，禁止手改代码内容） -->
<!-- origin: org.springframework:spring-web:7.1.0 | path: org/springframework/web/bind/annotation/ExceptionHandler.java -->
<!-- 正文引用：[[源码:ExceptionHandler.java#L起-L止]] ；代码块头部：(src: ExceptionHandler.java#L起-L止) -->

# ExceptionHandler.java

| 项 | 值 |
| --- | --- |
| 来源 | `org.springframework:spring-web:7.1.0` |
| 路径 | `org/springframework/web/bind/annotation/ExceptionHandler.java` |
| 行数 | 138 |
| 在线地址 | https://repo1.maven.org/maven2/org/springframework/spring-web/7.1.0/spring-web-7.1.0-sources.jar |
| 抓取时间 | 2026-11-25T14:01:23.819Z |

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
   25 | import org.springframework.aot.hint.annotation.Reflective;
   26 | import org.springframework.core.annotation.AliasFor;
   27 | 
   28 | /**
   29 |  * Annotation for handling exceptions in specific handler classes and/or
   30 |  * handler methods.
   31 |  *
   32 |  * <p>Handler methods which are annotated with this annotation are allowed to
   33 |  * have very flexible signatures. They may have parameters of the following
   34 |  * types, in arbitrary order:
   35 |  * <ul>
   36 |  * <li>An exception argument: declared as a general Exception or as a more
   37 |  * specific exception. This also serves as a mapping hint if the annotation
   38 |  * itself does not narrow the exception types through its {@link #value()}.
   39 |  * You may refer to a top-level exception being propagated or to a nested
   40 |  * cause within a wrapper exception. Any cause level is exposed.
   41 |  * <li>Request and/or response objects (typically from the Servlet API).
   42 |  * You may choose any specific request/response type, for example,
   43 |  * {@link jakarta.servlet.ServletRequest} / {@link jakarta.servlet.http.HttpServletRequest}.
   44 |  * <li>Session object: typically {@link jakarta.servlet.http.HttpSession}.
   45 |  * An argument of this type will enforce the presence of a corresponding session.
   46 |  * As a consequence, such an argument will never be {@code null}.
   47 |  * <i>Note that session access may not be thread-safe, in particular in a
   48 |  * Servlet environment: Consider switching the
   49 |  * {@link org.springframework.web.servlet.mvc.method.annotation.RequestMappingHandlerAdapter#setSynchronizeOnSession
   50 |  * "synchronizeOnSession"} flag to "true" if multiple requests are allowed to
   51 |  * access a session concurrently.</i>
   52 |  * <li>{@link org.springframework.web.context.request.WebRequest} or
   53 |  * {@link org.springframework.web.context.request.NativeWebRequest}.
   54 |  * Allows for generic request parameter access as well as request/session
   55 |  * attribute access, without ties to the native Servlet API.
   56 |  * <li>{@link java.util.Locale} for the current request locale
   57 |  * (determined by the most specific locale resolver available,
   58 |  * i.e. the configured {@link org.springframework.web.servlet.LocaleResolver}
   59 |  * in a Servlet environment).
   60 |  * <li>{@link java.io.InputStream} / {@link java.io.Reader} for access
   61 |  * to the request's content. This will be the raw InputStream/Reader as
   62 |  * exposed by the Servlet API.
   63 |  * <li>{@link java.io.OutputStream} / {@link java.io.Writer} for generating
   64 |  * the response's content. This will be the raw OutputStream/Writer as
   65 |  * exposed by the Servlet API.
   66 |  * <li>{@link org.springframework.ui.Model} as an alternative to returning
   67 |  * a model map from the handler method. Note that the provided model is not
   68 |  * pre-populated with regular model attributes and therefore always empty,
   69 |  * as a convenience for preparing the model for an exception-specific view.
   70 |  * </ul>
   71 |  *
   72 |  * <p>The following return types are supported for handler methods:
   73 |  * <ul>
   74 |  * <li>{@code ModelAndView} object (from Servlet MVC).
   75 |  * <li>{@link org.springframework.ui.Model} object, with the view name implicitly
   76 |  * determined through a {@link org.springframework.web.servlet.RequestToViewNameTranslator}.
   77 |  * <li>{@link java.util.Map} object for exposing a model,
   78 |  * with the view name implicitly determined through a
   79 |  * {@link org.springframework.web.servlet.RequestToViewNameTranslator}.
   80 |  * <li>{@link org.springframework.web.servlet.View} object.
   81 |  * <li>{@link String} value which is interpreted as view name.
   82 |  * <li>{@link ResponseBody @ResponseBody} annotated methods (Servlet-only)
   83 |  * to set the response content. The return value will be converted to the
   84 |  * response stream using
   85 |  * {@linkplain org.springframework.http.converter.HttpMessageConverter message converters}.
   86 |  * <li>{@link org.springframework.http.HttpEntity HttpEntity&lt;?&gt;} or
   87 |  * {@link org.springframework.http.ResponseEntity ResponseEntity&lt;?&gt;} object
   88 |  * (Servlet-only) to set response headers and content. The ResponseEntity body
   89 |  * will be converted and written to the response stream using
   90 |  * {@linkplain org.springframework.http.converter.HttpMessageConverter message converters}.
   91 |  * <li>{@link org.springframework.http.ProblemDetail} or {@link org.springframework.web.ErrorResponse}
   92 |  * object to render an RFC 9457 error response with details in the body.
   93 |  * <li>{@code void} if the method handles the response itself (by
   94 |  * writing the response content directly, declaring an argument of type
   95 |  * {@link jakarta.servlet.ServletResponse} / {@link jakarta.servlet.http.HttpServletResponse}
   96 |  * for that purpose) or if the view name is supposed to be implicitly determined
   97 |  * through a {@link org.springframework.web.servlet.RequestToViewNameTranslator}
   98 |  * (not declaring a response argument in the handler method signature).
   99 |  * </ul>
  100 |  *
  101 |  * <p>You may combine the {@code ExceptionHandler} annotation with
  102 |  * {@link ResponseStatus @ResponseStatus} for a specific HTTP error status.
  103 |  *
  104 |  * @author Arjen Poutsma
  105 |  * @author Juergen Hoeller
  106 |  * @author Brian Clozel
  107 |  * @since 3.0
  108 |  * @see ControllerAdvice
  109 |  * @see org.springframework.web.context.request.WebRequest
  110 |  */
  111 | @Target(ElementType.METHOD)
  112 | @Retention(RetentionPolicy.RUNTIME)
  113 | @Documented
  114 | @Reflective(ExceptionHandlerReflectiveProcessor.class)
  115 | public @interface ExceptionHandler {
  116 | 
  117 | 	/**
  118 | 	 * Exceptions handled by the annotated method.
  119 | 	 * <p>This is an alias for {@link #exception}.
  120 | 	 */
  121 | 	@AliasFor("exception")
  122 | 	Class<? extends Throwable>[] value() default {};
  123 | 
  124 | 	/**
  125 | 	 * Exceptions handled by the annotated method. If empty, will default to any
  126 | 	 * exceptions listed in the method argument list.
  127 | 	 * @since 6.2
  128 | 	 */
  129 | 	@AliasFor("value")
  130 | 	Class<? extends Throwable>[] exception() default {};
  131 | 
  132 | 	/**
  133 | 	 * Media Types that can be produced by the annotated method.
  134 | 	 * @since 6.2
  135 | 	 */
  136 | 	String[] produces() default {};
  137 | 
  138 | }
```
