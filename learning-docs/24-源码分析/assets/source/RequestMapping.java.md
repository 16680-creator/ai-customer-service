<!-- learning-docs source archive（机械抓取原文，禁止手改代码内容） -->
<!-- origin: org.springframework:spring-web:7.1.0 | path: org/springframework/web/bind/annotation/RequestMapping.java -->
<!-- 正文引用：[[源码:RequestMapping.java#L起-L止]] ；代码块头部：(src: RequestMapping.java#L起-L止) -->

# RequestMapping.java

| 项 | 值 |
| --- | --- |
| 来源 | `org.springframework:spring-web:7.1.0` |
| 路径 | `org/springframework/web/bind/annotation/RequestMapping.java` |
| 行数 | 238 |
| 在线地址 | https://repo1.maven.org/maven2/org/springframework/spring-web/7.1.0/spring-web-7.1.0-sources.jar |
| 抓取时间 | 2026-11-25T14:01:24.031Z |

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
   29 |  * Annotation for mapping web requests onto methods in request-handling classes
   30 |  * with flexible method signatures.
   31 |  *
   32 |  * <p>Both Spring MVC and Spring WebFlux support this annotation through a
   33 |  * {@code RequestMappingHandlerMapping} and {@code RequestMappingHandlerAdapter}
   34 |  * in their respective modules and package structures. For the exact list of
   35 |  * supported handler method arguments and return types in each, please use the
   36 |  * reference documentation links below:
   37 |  * <ul>
   38 |  * <li>Spring MVC
   39 |  * <a href="https://docs.spring.io/spring/docs/current/spring-framework-reference/web.html#mvc-ann-arguments">Method Arguments</a>
   40 |  * and
   41 |  * <a href="https://docs.spring.io/spring/docs/current/spring-framework-reference/web.html#mvc-ann-return-types">Return Values</a>
   42 |  * </li>
   43 |  * <li>Spring WebFlux
   44 |  * <a href="https://docs.spring.io/spring/docs/current/spring-framework-reference/web-reactive.html#webflux-ann-arguments">Method Arguments</a>
   45 |  * and
   46 |  * <a href="https://docs.spring.io/spring/docs/current/spring-framework-reference/web-reactive.html#webflux-ann-return-types">Return Values</a>
   47 |  * </li>
   48 |  * </ul>
   49 |  *
   50 |  * <p><strong>NOTE:</strong> This annotation can be used both at the class and
   51 |  * at the method level. In most cases, at the method level applications will
   52 |  * prefer to use one of the HTTP method specific variants
   53 |  * {@link GetMapping @GetMapping}, {@link PostMapping @PostMapping},
   54 |  * {@link PutMapping @PutMapping}, {@link DeleteMapping @DeleteMapping}, or
   55 |  * {@link PatchMapping @PatchMapping}.
   56 |  *
   57 |  * <p><strong>NOTE:</strong> This annotation cannot be used in conjunction with
   58 |  * other {@code @RequestMapping} annotations that are declared on the same element
   59 |  * (class, interface, or method). If multiple {@code @RequestMapping} annotations
   60 |  * are detected on the same element, a warning will be logged, and only the first
   61 |  * mapping will be used. This also applies to composed {@code @RequestMapping}
   62 |  * annotations such as {@code @GetMapping}, {@code @PostMapping}, etc.
   63 |  *
   64 |  * <p><b>NOTE:</b> When using controller interfaces (for example, for AOP proxying),
   65 |  * make sure to consistently put <i>all</i> your mapping annotations &mdash; such
   66 |  * as {@code @RequestMapping} and {@code @SessionAttributes} &mdash; on
   67 |  * the controller <i>interface</i> rather than on the implementation class.
   68 |  *
   69 |  * @author Juergen Hoeller
   70 |  * @author Arjen Poutsma
   71 |  * @author Sam Brannen
   72 |  * @since 2.5
   73 |  * @see GetMapping
   74 |  * @see PostMapping
   75 |  * @see PutMapping
   76 |  * @see DeleteMapping
   77 |  * @see PatchMapping
   78 |  */
   79 | @Target({ElementType.TYPE, ElementType.METHOD})
   80 | @Retention(RetentionPolicy.RUNTIME)
   81 | @Documented
   82 | @Mapping
   83 | @Reflective(ControllerMappingReflectiveProcessor.class)
   84 | public @interface RequestMapping {
   85 | 
   86 | 	/**
   87 | 	 * Assign a name to this mapping.
   88 | 	 * <p><b>Supported at the type level as well as at the method level!</b>
   89 | 	 * When used on both levels, a combined name is derived by concatenation
   90 | 	 * with "#" as separator.
   91 | 	 * @see org.springframework.web.servlet.mvc.method.annotation.MvcUriComponentsBuilder
   92 | 	 * @see org.springframework.web.servlet.handler.HandlerMethodMappingNamingStrategy
   93 | 	 */
   94 | 	String name() default "";
   95 | 
   96 | 	/**
   97 | 	 * The path mapping URIs &mdash; for example, {@code "/profile"}.
   98 | 	 * <p>This is an alias for {@link #path}. For example,
   99 | 	 * {@code @RequestMapping("/profile")} is equivalent to
  100 | 	 * {@code @RequestMapping(path="/profile")}.
  101 | 	 * <p>See {@link #path} for further details.
  102 | 	 */
  103 | 	@AliasFor("path")
  104 | 	String[] value() default {};
  105 | 
  106 | 	/**
  107 | 	 * The path mapping URIs &mdash; for example, {@code "/profile"}.
  108 | 	 * <p>Ant-style path patterns are also supported, e.g. {@code "/profile/**"}.
  109 | 	 * At the method level, relative paths, e.g., {@code "edit"} are supported
  110 | 	 * within the primary mapping expressed at the type level.
  111 | 	 * Path mapping URIs may contain property placeholders, e.g. <code>"/${profile_path}"</code>,
  112 | 	 * and SpEL expressions, e.g. {@code "/profile/#{@bean.property}"}.
  113 | 	 * <p><b>Supported at the type level as well as at the method level!</b>
  114 | 	 * When used at the type level, all method-level mappings inherit
  115 | 	 * this primary mapping, narrowing it for a specific handler method.
  116 | 	 * <p><strong>NOTE</strong>: A handler method that is not mapped to any path
  117 | 	 * explicitly is effectively mapped to an empty path.
  118 | 	 * @since 4.2
  119 | 	 * @see org.springframework.beans.factory.config.EmbeddedValueResolver
  120 | 	 * @see org.springframework.context.expression.StandardBeanExpressionResolver
  121 | 	 * @see org.springframework.context.support.AbstractApplicationContext
  122 | 	 */
  123 | 	@AliasFor("value")
  124 | 	String[] path() default {};
  125 | 
  126 | 	/**
  127 | 	 * The HTTP request methods to map to, narrowing the primary mapping:
  128 | 	 * GET, POST, HEAD, OPTIONS, PUT, PATCH, DELETE, TRACE.
  129 | 	 * <p><b>Supported at the type level as well as at the method level!</b>
  130 | 	 * When used at the type level, all method-level mappings inherit this
  131 | 	 * HTTP method restriction.
  132 | 	 */
  133 | 	RequestMethod[] method() default {};
  134 | 
  135 | 	/**
  136 | 	 * The parameters of the mapped request, narrowing the primary mapping.
  137 | 	 * <p>Same format for any environment: a sequence of "myParam=myValue" style
  138 | 	 * expressions, with a request only mapped if each such parameter is found
  139 | 	 * to have the given value. Expressions can be negated by using the "!=" operator,
  140 | 	 * as in "myParam!=myValue". "myParam" style expressions are also supported,
  141 | 	 * with such parameters having to be present in the request (allowed to have
  142 | 	 * any value). Finally, "!myParam" style expressions indicate that the
  143 | 	 * specified parameter is <i>not</i> supposed to be present in the request.
  144 | 	 * <p><b>Supported at the type level as well as at the method level!</b>
  145 | 	 * When used at the type level, all method-level mappings inherit this
  146 | 	 * parameter restriction.
  147 | 	 */
  148 | 	String[] params() default {};
  149 | 
  150 | 	/**
  151 | 	 * The headers of the mapped request, narrowing the primary mapping.
  152 | 	 * <p>Same format for any environment: a sequence of "My-Header=myValue" style
  153 | 	 * expressions, with a request only mapped if each such header is found
  154 | 	 * to have the given value. Expressions can be negated by using the "!=" operator,
  155 | 	 * as in "My-Header!=myValue". "My-Header" style expressions are also supported,
  156 | 	 * with such headers having to be present in the request (allowed to have
  157 | 	 * any value). Finally, "!My-Header" style expressions indicate that the
  158 | 	 * specified header is <i>not</i> supposed to be present in the request.
  159 | 	 * <p>Also supports media type wildcards (*), for headers such as Accept
  160 | 	 * and Content-Type. For instance,
  161 | 	 * <pre class="code">
  162 | 	 * &#064;RequestMapping(value = "/something", headers = "content-type=text/*")
  163 | 	 * </pre>
  164 | 	 * will match requests with a Content-Type of "text/html", "text/plain", etc.
  165 | 	 * <p><b>Supported at the type level as well as at the method level!</b>
  166 | 	 * When used at the type level, all method-level mappings inherit this
  167 | 	 * header restriction.
  168 | 	 * @see org.springframework.http.MediaType
  169 | 	 */
  170 | 	String[] headers() default {};
  171 | 
  172 | 	/**
  173 | 	 * Narrows the primary mapping by media types that can be consumed by the
  174 | 	 * mapped handler. Consists of one or more media types one of which must
  175 | 	 * match to the request {@code Content-Type} header. Examples:
  176 | 	 * <pre class="code">
  177 | 	 * consumes = "text/plain"
  178 | 	 * consumes = {"text/plain", "application/*"}
  179 | 	 * consumes = MediaType.TEXT_PLAIN_VALUE
  180 | 	 * </pre>
  181 | 	 * <p>If a declared media type contains a parameter, and if the
  182 | 	 * {@code "content-type"} from the request also has that parameter, then
  183 | 	 * the parameter values must match. Otherwise, if the media type from the
  184 | 	 * request {@code "content-type"} does not contain the parameter, then the
  185 | 	 * parameter is ignored for matching purposes.
  186 | 	 * <p>Expressions can be negated by using the "!" operator, as in
  187 | 	 * "!text/plain", which matches all requests with a {@code Content-Type}
  188 | 	 * other than "text/plain".
  189 | 	 * <p><b>Supported at the type level as well as at the method level!</b>
  190 | 	 * If specified at both levels, the method level consumes condition overrides
  191 | 	 * the type level condition.
  192 | 	 * @see org.springframework.http.MediaType
  193 | 	 * @see jakarta.servlet.http.HttpServletRequest#getContentType()
  194 | 	 */
  195 | 	String[] consumes() default {};
  196 | 
  197 | 	/**
  198 | 	 * Narrows the primary mapping by media types that can be produced by the
  199 | 	 * mapped handler. Consists of one or more media types one of which must
  200 | 	 * be chosen via content negotiation against the "acceptable" media types
  201 | 	 * of the request. Typically those are extracted from the {@code "Accept"}
  202 | 	 * header but may be derived from query parameters, or other. Examples:
  203 | 	 * <pre class="code">
  204 | 	 * produces = "text/plain"
  205 | 	 * produces = {"text/plain", "application/*"}
  206 | 	 * produces = MediaType.TEXT_PLAIN_VALUE
  207 | 	 * produces = "text/plain;charset=UTF-8"
  208 | 	 * </pre>
  209 | 	 * <p>If a declared media type contains a parameter (for example, "charset=UTF-8",
  210 | 	 * "type=feed", "type=entry") and if a compatible media type from the request
  211 | 	 * has that parameter too, then the parameter values must match. Otherwise,
  212 | 	 * if the media type from the request does not contain the parameter, it is
  213 | 	 * assumed the client accepts any value.
  214 | 	 * <p>Expressions can be negated by using the "!" operator, as in "!text/plain",
  215 | 	 * which matches all requests with a {@code Accept} other than "text/plain".
  216 | 	 * <p><b>Supported at the type level as well as at the method level!</b>
  217 | 	 * If specified at both levels, the method level produces condition overrides
  218 | 	 * the type level condition.
  219 | 	 * @see org.springframework.http.MediaType
  220 | 	 */
  221 | 	String[] produces() default {};
  222 | 
  223 | 	/**
  224 | 	 * Narrows the primary mapping by an API version. The version may be one
  225 | 	 * of the following:
  226 | 	 * <ul>
  227 | 	 * <li>Fixed version ("1.2") -- match this version only.
  228 | 	 * <li>Baseline version ("1.2+") -- match this and subsequent versions.
  229 | 	 * </ul>
  230 | 	 * <p>A baseline version allows an endpoint to continue to work in
  231 | 	 * subsequent versions if it remains compatible. When an incompatible change
  232 | 	 * is made eventually, a new controller method for the same endpoint but
  233 | 	 * with a higher version takes precedence.
  234 | 	 * @since 7.0
  235 | 	 */
  236 | 	String version() default "";
  237 | 
  238 | }
```
