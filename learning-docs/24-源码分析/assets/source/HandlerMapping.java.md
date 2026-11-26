<!-- learning-docs source archive（机械抓取原文，禁止手改代码内容） -->
<!-- origin: org.springframework:spring-webmvc:7.1.0 | path: org/springframework/web/servlet/HandlerMapping.java -->
<!-- 正文引用：[[源码:HandlerMapping.java#L起-L止]] ；代码块头部：(src: HandlerMapping.java#L起-L止) -->

# HandlerMapping.java

| 项 | 值 |
| --- | --- |
| 来源 | `org.springframework:spring-webmvc:7.1.0` |
| 路径 | `org/springframework/web/servlet/HandlerMapping.java` |
| 行数 | 189 |
| 在线地址 | https://repo1.maven.org/maven2/org/springframework/spring-webmvc/7.1.0/spring-webmvc-7.1.0-sources.jar |
| 抓取时间 | 2026-11-25T14:01:09.093Z |

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
   17 | package org.springframework.web.servlet;
   18 | 
   19 | import jakarta.servlet.ServletRequest;
   20 | import jakarta.servlet.http.HttpServletRequest;
   21 | import org.jspecify.annotations.Nullable;
   22 | 
   23 | import org.springframework.web.accept.ApiVersionHolder;
   24 | 
   25 | /**
   26 |  * Interface to be implemented by objects that define a mapping between
   27 |  * requests and handler objects.
   28 |  *
   29 |  * <p>This class can be implemented by application developers, although this is not
   30 |  * necessary, as {@link org.springframework.web.servlet.handler.BeanNameUrlHandlerMapping}
   31 |  * and {@link org.springframework.web.servlet.mvc.method.annotation.RequestMappingHandlerMapping}
   32 |  * are included in the framework. The former is the default if no
   33 |  * HandlerMapping bean is registered in the application context.
   34 |  *
   35 |  * <p>HandlerMapping implementations can support mapped interceptors but do not
   36 |  * have to. A handler will always be wrapped in a {@link HandlerExecutionChain}
   37 |  * instance, optionally accompanied by some {@link HandlerInterceptor} instances.
   38 |  * The DispatcherServlet will first call each HandlerInterceptor's
   39 |  * {@code preHandle} method in the given order, finally invoking the handler
   40 |  * itself if all {@code preHandle} methods have returned {@code true}.
   41 |  *
   42 |  * <p>The ability to parameterize this mapping is a powerful and unusual
   43 |  * capability of this MVC framework. For example, it is possible to write
   44 |  * a custom mapping based on session state, cookie state or many other
   45 |  * variables. No other MVC framework seems to be equally flexible.
   46 |  *
   47 |  * <p>Note: Implementations can implement the {@link org.springframework.core.Ordered}
   48 |  * interface to be able to specify a sorting order and thus a priority for getting
   49 |  * applied by DispatcherServlet. Non-Ordered instances get treated as the lowest priority.
   50 |  *
   51 |  * @author Rod Johnson
   52 |  * @author Juergen Hoeller
   53 |  * @see org.springframework.core.Ordered
   54 |  * @see org.springframework.web.servlet.handler.AbstractHandlerMapping
   55 |  * @see org.springframework.web.servlet.handler.BeanNameUrlHandlerMapping
   56 |  * @see org.springframework.web.servlet.mvc.method.annotation.RequestMappingHandlerMapping
   57 |  */
   58 | public interface HandlerMapping {
   59 | 
   60 | 	/**
   61 | 	 * Name of the {@link HttpServletRequest} attribute that contains the mapped
   62 | 	 * handler for the best matching pattern.
   63 | 	 * @since 4.3.21
   64 | 	 */
   65 | 	String BEST_MATCHING_HANDLER_ATTRIBUTE = HandlerMapping.class.getName() + ".bestMatchingHandler";
   66 | 
   67 | 	/**
   68 | 	 * Name of the {@link HttpServletRequest} attribute that contains the path
   69 | 	 * used to look up the matching handler, which depending on the configured
   70 | 	 * {@link org.springframework.web.util.UrlPathHelper} could be the full path
   71 | 	 * or without the context path, decoded or not, etc.
   72 | 	 * @since 5.2
   73 | 	 * @deprecated in favor of
   74 | 	 * {@link org.springframework.web.util.UrlPathHelper#PATH_ATTRIBUTE} and
   75 | 	 * {@link org.springframework.web.util.ServletRequestPathUtils#PATH_ATTRIBUTE}.
   76 | 	 * To access the cached path used for request mapping, use
   77 | 	 * {@link org.springframework.web.util.ServletRequestPathUtils#getCachedPathValue(ServletRequest)}.
   78 | 	 */
   79 | 	@Deprecated(since = "5.3")
   80 | 	String LOOKUP_PATH = HandlerMapping.class.getName() + ".lookupPath";
   81 | 
   82 | 	/**
   83 | 	 * Name of the {@link HttpServletRequest} attribute that contains the path
   84 | 	 * within the handler mapping, in case of a pattern match, or the full
   85 | 	 * relevant URI (typically within the DispatcherServlet's mapping) else.
   86 | 	 * <p>Note: This attribute is not required to be supported by all
   87 | 	 * HandlerMapping implementations. URL-based HandlerMappings will
   88 | 	 * typically support it, but handlers should not necessarily expect
   89 | 	 * this request attribute to be present in all scenarios.
   90 | 	 */
   91 | 	String PATH_WITHIN_HANDLER_MAPPING_ATTRIBUTE = HandlerMapping.class.getName() + ".pathWithinHandlerMapping";
   92 | 
   93 | 	/**
   94 | 	 * Name of the {@link HttpServletRequest} attribute that contains the
   95 | 	 * best matching pattern within the handler mapping.
   96 | 	 * <p>Note: This attribute is not required to be supported by all
   97 | 	 * HandlerMapping implementations. URL-based HandlerMappings will
   98 | 	 * typically support it, but handlers should not necessarily expect
   99 | 	 * this request attribute to be present in all scenarios.
  100 | 	 */
  101 | 	String BEST_MATCHING_PATTERN_ATTRIBUTE = HandlerMapping.class.getName() + ".bestMatchingPattern";
  102 | 
  103 | 	/**
  104 | 	 * Name of the {@link HttpServletRequest} attribute that contains the best
  105 | 	 * matching {@link org.springframework.web.util.pattern.PathPattern}, when
  106 | 	 * parsed patterns are in use.
  107 | 	 * @since 7.1
  108 | 	 */
  109 | 	String BEST_MATCHING_PATH_PATTERN_ATTRIBUTE = HandlerMapping.class.getName() + ".bestMatchingPathPattern";
  110 | 
  111 | 	/**
  112 | 	 * Name of the boolean {@link HttpServletRequest} attribute that indicates
  113 | 	 * whether type-level mappings should be inspected.
  114 | 	 * <p>Note: This attribute is not required to be supported by all
  115 | 	 * HandlerMapping implementations.
  116 | 	 */
  117 | 	String INTROSPECT_TYPE_LEVEL_MAPPING = HandlerMapping.class.getName() + ".introspectTypeLevelMapping";
  118 | 
  119 | 	/**
  120 | 	 * Name of the {@link HttpServletRequest} attribute that contains the URI
  121 | 	 * templates map, mapping variable names to values.
  122 | 	 * <p>Note: This attribute is not required to be supported by all
  123 | 	 * HandlerMapping implementations. URL-based HandlerMappings will
  124 | 	 * typically support it, but handlers should not necessarily expect
  125 | 	 * this request attribute to be present in all scenarios.
  126 | 	 */
  127 | 	String URI_TEMPLATE_VARIABLES_ATTRIBUTE = HandlerMapping.class.getName() + ".uriTemplateVariables";
  128 | 
  129 | 	/**
  130 | 	 * Name of the {@link HttpServletRequest} attribute that contains a map with
  131 | 	 * URI variable names and a corresponding MultiValueMap of URI matrix
  132 | 	 * variables for each.
  133 | 	 * <p>Note: This attribute is not required to be supported by all
  134 | 	 * HandlerMapping implementations and may also not be present depending on
  135 | 	 * whether the HandlerMapping is configured to keep matrix variable content
  136 | 	 */
  137 | 	String MATRIX_VARIABLES_ATTRIBUTE = HandlerMapping.class.getName() + ".matrixVariables";
  138 | 
  139 | 	/**
  140 | 	 * Name of the {@link HttpServletRequest} attribute that contains the set of
  141 | 	 * producible MediaTypes applicable to the mapped handler.
  142 | 	 * <p>Note: This attribute is not required to be supported by all
  143 | 	 * HandlerMapping implementations. Handlers should not necessarily expect
  144 | 	 * this request attribute to be present in all scenarios.
  145 | 	 */
  146 | 	String PRODUCIBLE_MEDIA_TYPES_ATTRIBUTE = HandlerMapping.class.getName() + ".producibleMediaTypes";
  147 | 
  148 | 	/**
  149 | 	 * Name of the {@link HttpServletRequest} attribute that contains an
  150 | 	 * {@link ApiVersionHolder} with the
  151 | 	 * result of obtaining and parsing the API version of the request.
  152 | 	 * @since 7.0
  153 | 	 */
  154 | 	String API_VERSION_ATTRIBUTE = HandlerMapping.class.getName() + ".apiVersion";
  155 | 
  156 | 
  157 | 	/**
  158 | 	 * Whether this {@code HandlerMapping} instance has been enabled to use parsed
  159 | 	 * {@link org.springframework.web.util.pattern.PathPattern}s in which case
  160 | 	 * the {@link DispatcherServlet} automatically
  161 | 	 * {@link org.springframework.web.util.ServletRequestPathUtils#parseAndCache parses}
  162 | 	 * the {@code RequestPath} to make it available for
  163 | 	 * {@link org.springframework.web.util.ServletRequestPathUtils#getParsedRequestPath
  164 | 	 * access} in {@code HandlerMapping}s, {@code HandlerInterceptor}s, and
  165 | 	 * other components.
  166 | 	 * @since 5.3
  167 | 	 */
  168 | 	default boolean usesPathPatterns() {
  169 | 		return false;
  170 | 	}
  171 | 
  172 | 	/**
  173 | 	 * Return a handler and any interceptors for this request. The choice may be made
  174 | 	 * on request URL, session state, or any factor the implementing class chooses.
  175 | 	 * <p>The returned HandlerExecutionChain contains a handler Object, rather than
  176 | 	 * even a tag interface, so that handlers are not constrained in any way.
  177 | 	 * For example, a HandlerAdapter could be written to allow another framework's
  178 | 	 * handler objects to be used.
  179 | 	 * <p>Returns {@code null} if no match was found. This is not an error.
  180 | 	 * The DispatcherServlet will query all registered HandlerMapping beans to find
  181 | 	 * a match, and only decide there is an error if none can find a handler.
  182 | 	 * @param request current HTTP request
  183 | 	 * @return a HandlerExecutionChain instance containing handler object and
  184 | 	 * any interceptors, or {@code null} if no mapping found
  185 | 	 * @throws Exception if there is an internal error
  186 | 	 */
  187 | 	@Nullable HandlerExecutionChain getHandler(HttpServletRequest request) throws Exception;
  188 | 
  189 | }
```
