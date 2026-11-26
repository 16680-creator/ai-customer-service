<!-- learning-docs source archive（机械抓取原文，禁止手改代码内容） -->
<!-- origin: org.springframework:spring-webmvc:7.1.0 | path: org/springframework/web/servlet/DispatcherServlet.java -->
<!-- 正文引用：[[源码:DispatcherServlet.java#L起-L止]] ；代码块头部：(src: DispatcherServlet.java#L起-L止) -->

# DispatcherServlet.java

| 项 | 值 |
| --- | --- |
| 来源 | `org.springframework:spring-webmvc:7.1.0` |
| 路径 | `org/springframework/web/servlet/DispatcherServlet.java` |
| 行数 | 1407 |
| 在线地址 | https://repo1.maven.org/maven2/org/springframework/spring-webmvc/7.1.0/spring-webmvc-7.1.0-sources.jar |
| 抓取时间 | 2026-11-25T14:01:08.211Z |

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
   19 | import java.io.IOException;
   20 | import java.util.ArrayList;
   21 | import java.util.Arrays;
   22 | import java.util.Collections;
   23 | import java.util.Enumeration;
   24 | import java.util.HashMap;
   25 | import java.util.HashSet;
   26 | import java.util.List;
   27 | import java.util.Locale;
   28 | import java.util.Map;
   29 | import java.util.Properties;
   30 | import java.util.Set;
   31 | import java.util.stream.Collectors;
   32 | 
   33 | import jakarta.servlet.DispatcherType;
   34 | import jakarta.servlet.ServletContext;
   35 | import jakarta.servlet.ServletException;
   36 | import jakarta.servlet.http.HttpServletRequest;
   37 | import jakarta.servlet.http.HttpServletResponse;
   38 | import org.apache.commons.logging.Log;
   39 | import org.apache.commons.logging.LogFactory;
   40 | import org.jspecify.annotations.Nullable;
   41 | 
   42 | import org.springframework.beans.factory.BeanFactoryUtils;
   43 | import org.springframework.beans.factory.BeanInitializationException;
   44 | import org.springframework.beans.factory.NoSuchBeanDefinitionException;
   45 | import org.springframework.context.ApplicationContext;
   46 | import org.springframework.context.ConfigurableApplicationContext;
   47 | import org.springframework.context.i18n.LocaleContext;
   48 | import org.springframework.core.annotation.AnnotationAwareOrderComparator;
   49 | import org.springframework.core.io.ClassPathResource;
   50 | import org.springframework.core.io.support.PropertiesLoaderUtils;
   51 | import org.springframework.core.log.LogFormatUtils;
   52 | import org.springframework.http.HttpHeaders;
   53 | import org.springframework.http.MediaType;
   54 | import org.springframework.http.server.RequestPath;
   55 | import org.springframework.http.server.ServletServerHttpRequest;
   56 | import org.springframework.util.ClassUtils;
   57 | import org.springframework.util.StringUtils;
   58 | import org.springframework.web.context.WebApplicationContext;
   59 | import org.springframework.web.context.request.async.WebAsyncManager;
   60 | import org.springframework.web.context.request.async.WebAsyncUtils;
   61 | import org.springframework.web.multipart.MultipartException;
   62 | import org.springframework.web.multipart.MultipartHttpServletRequest;
   63 | import org.springframework.web.multipart.MultipartResolver;
   64 | import org.springframework.web.util.ServletRequestPathUtils;
   65 | import org.springframework.web.util.WebUtils;
   66 | 
   67 | /**
   68 |  * Central dispatcher for HTTP request handlers/controllers, for example, for web UI controllers
   69 |  * or HTTP-based remote service exporters. Dispatches to registered handlers for processing
   70 |  * a web request, providing convenient mapping and exception handling facilities.
   71 |  *
   72 |  * <p>This servlet is very flexible: It can be used with just about any workflow, with the
   73 |  * installation of the appropriate adapter classes. It offers the following functionality
   74 |  * that distinguishes it from other request-driven web MVC frameworks:
   75 |  *
   76 |  * <ul>
   77 |  * <li>It is based around a JavaBeans configuration mechanism.
   78 |  *
   79 |  * <li>It can use any {@link HandlerMapping} implementation - pre-built or provided as part
   80 |  * of an application - to control the routing of requests to handler objects. Default is
   81 |  * {@link org.springframework.web.servlet.handler.BeanNameUrlHandlerMapping} and
   82 |  * {@link org.springframework.web.servlet.mvc.method.annotation.RequestMappingHandlerMapping}.
   83 |  * HandlerMapping objects can be defined as beans in the servlet's application context,
   84 |  * implementing the HandlerMapping interface, overriding the default HandlerMapping if
   85 |  * present. HandlerMappings can be given any bean name (they are tested by type).
   86 |  *
   87 |  * <li>It can use any {@link HandlerAdapter}; this allows for using any handler interface.
   88 |  * Default adapters are {@link org.springframework.web.servlet.mvc.HttpRequestHandlerAdapter},
   89 |  * {@link org.springframework.web.servlet.mvc.SimpleControllerHandlerAdapter}, for Spring's
   90 |  * {@link org.springframework.web.HttpRequestHandler} and
   91 |  * {@link org.springframework.web.servlet.mvc.Controller} interfaces, respectively. A default
   92 |  * {@link org.springframework.web.servlet.mvc.method.annotation.RequestMappingHandlerAdapter}
   93 |  * will be registered as well. HandlerAdapter objects can be added as beans in the
   94 |  * application context, overriding the default HandlerAdapters. Like HandlerMappings,
   95 |  * HandlerAdapters can be given any bean name (they are tested by type).
   96 |  *
   97 |  * <li>The dispatcher's exception resolution strategy can be specified via a
   98 |  * {@link HandlerExceptionResolver}, for example mapping certain exceptions to error pages.
   99 |  * Default are
  100 |  * {@link org.springframework.web.servlet.mvc.method.annotation.ExceptionHandlerExceptionResolver},
  101 |  * {@link org.springframework.web.servlet.mvc.annotation.ResponseStatusExceptionResolver}, and
  102 |  * {@link org.springframework.web.servlet.mvc.support.DefaultHandlerExceptionResolver}.
  103 |  * These HandlerExceptionResolvers can be overridden through the application context.
  104 |  * HandlerExceptionResolver can be given any bean name (they are tested by type).
  105 |  *
  106 |  * <li>Its view resolution strategy can be specified via a {@link ViewResolver}
  107 |  * implementation, resolving symbolic view names into View objects. Default is
  108 |  * {@link org.springframework.web.servlet.view.InternalResourceViewResolver}.
  109 |  * ViewResolver objects can be added as beans in the application context, overriding the
  110 |  * default ViewResolver. ViewResolvers can be given any bean name (they are tested by type).
  111 |  *
  112 |  * <li>If a {@link View} or view name is not supplied by the user, then the configured
  113 |  * {@link RequestToViewNameTranslator} will translate the current request into a view name.
  114 |  * The corresponding bean name is "viewNameTranslator"; the default is
  115 |  * {@link org.springframework.web.servlet.view.DefaultRequestToViewNameTranslator}.
  116 |  *
  117 |  * <li>The dispatcher's strategy for resolving multipart requests is determined by a
  118 |  * {@link org.springframework.web.multipart.MultipartResolver} implementation.
  119 |  * An implementation for standard Servlet multipart processing is included.
  120 |  * The MultipartResolver bean name is "multipartResolver"; default is none.
  121 |  *
  122 |  * <li>Its locale resolution strategy is determined by a {@link LocaleResolver}.
  123 |  * Out-of-the-box implementations work via HTTP accept header, cookie, or session.
  124 |  * The LocaleResolver bean name is "localeResolver"; default is
  125 |  * {@link org.springframework.web.servlet.i18n.AcceptHeaderLocaleResolver}.
  126 |  * </ul>
  127 |  *
  128 |  * <p><b>NOTE: The {@code @RequestMapping} annotation will only be processed if a
  129 |  * corresponding {@code HandlerMapping} (for type-level annotations) and/or
  130 |  * {@code HandlerAdapter} (for method-level annotations) is present in the dispatcher.</b>
  131 |  * This is the case by default. However, if you are defining custom {@code HandlerMappings}
  132 |  * or {@code HandlerAdapters}, then you need to make sure that a corresponding custom
  133 |  * {@code RequestMappingHandlerMapping} and/or {@code RequestMappingHandlerAdapter}
  134 |  * is defined as well - provided that you intend to use {@code @RequestMapping}.
  135 |  *
  136 |  * <p><b>A web application can define any number of DispatcherServlets.</b>
  137 |  * Each servlet will operate in its own namespace, loading its own application context
  138 |  * with mappings, handlers, etc. Only the root application context as loaded by
  139 |  * {@link org.springframework.web.context.ContextLoaderListener}, if any, will be shared.
  140 |  *
  141 |  * <p>{@code DispatcherServlet} may be injected with a web application context,
  142 |  * rather than creating its own internally. This is useful in Servlet 3.0+
  143 |  * environments, which support programmatic registration of servlet instances.
  144 |  * See the {@link #DispatcherServlet(WebApplicationContext)} javadoc for details.
  145 |  *
  146 |  * @author Rod Johnson
  147 |  * @author Juergen Hoeller
  148 |  * @author Rob Harrop
  149 |  * @author Chris Beams
  150 |  * @author Rossen Stoyanchev
  151 |  * @author Sebastien Deleuze
  152 |  * @see org.springframework.web.HttpRequestHandler
  153 |  * @see org.springframework.web.servlet.mvc.Controller
  154 |  * @see org.springframework.web.context.ContextLoaderListener
  155 |  */
  156 | @SuppressWarnings("serial")
  157 | public class DispatcherServlet extends FrameworkServlet {
  158 | 
  159 | 	/** Well-known name for the MultipartResolver object in the bean factory for this namespace. */
  160 | 	public static final String MULTIPART_RESOLVER_BEAN_NAME = "multipartResolver";
  161 | 
  162 | 	/** Well-known name for the LocaleResolver object in the bean factory for this namespace. */
  163 | 	public static final String LOCALE_RESOLVER_BEAN_NAME = "localeResolver";
  164 | 
  165 | 	/**
  166 | 	 * Well-known name for the HandlerMapping object in the bean factory for this namespace.
  167 | 	 * Only used when "detectAllHandlerMappings" is turned off.
  168 | 	 * @see #setDetectAllHandlerMappings
  169 | 	 */
  170 | 	public static final String HANDLER_MAPPING_BEAN_NAME = "handlerMapping";
  171 | 
  172 | 	/**
  173 | 	 * Well-known name for the HandlerAdapter object in the bean factory for this namespace.
  174 | 	 * Only used when "detectAllHandlerAdapters" is turned off.
  175 | 	 * @see #setDetectAllHandlerAdapters
  176 | 	 */
  177 | 	public static final String HANDLER_ADAPTER_BEAN_NAME = "handlerAdapter";
  178 | 
  179 | 	/**
  180 | 	 * Well-known name for the HandlerExceptionResolver object in the bean factory for this namespace.
  181 | 	 * Only used when "detectAllHandlerExceptionResolvers" is turned off.
  182 | 	 * @see #setDetectAllHandlerExceptionResolvers
  183 | 	 */
  184 | 	public static final String HANDLER_EXCEPTION_RESOLVER_BEAN_NAME = "handlerExceptionResolver";
  185 | 
  186 | 	/**
  187 | 	 * Well-known name for the RequestToViewNameTranslator object in the bean factory for this namespace.
  188 | 	 */
  189 | 	public static final String REQUEST_TO_VIEW_NAME_TRANSLATOR_BEAN_NAME = "viewNameTranslator";
  190 | 
  191 | 	/**
  192 | 	 * Well-known name for the ViewResolver object in the bean factory for this namespace.
  193 | 	 * Only used when "detectAllViewResolvers" is turned off.
  194 | 	 * @see #setDetectAllViewResolvers
  195 | 	 */
  196 | 	public static final String VIEW_RESOLVER_BEAN_NAME = "viewResolver";
  197 | 
  198 | 	/**
  199 | 	 * Well-known name for the FlashMapManager object in the bean factory for this namespace.
  200 | 	 */
  201 | 	public static final String FLASH_MAP_MANAGER_BEAN_NAME = "flashMapManager";
  202 | 
  203 | 	/**
  204 | 	 * Request attribute to hold the current web application context.
  205 | 	 * Otherwise only the global web app context is obtainable by tags etc.
  206 | 	 * @see org.springframework.web.servlet.support.RequestContextUtils#findWebApplicationContext
  207 | 	 */
  208 | 	public static final String WEB_APPLICATION_CONTEXT_ATTRIBUTE = DispatcherServlet.class.getName() + ".CONTEXT";
  209 | 
  210 | 	/**
  211 | 	 * Request attribute to hold the current LocaleResolver, retrievable by views.
  212 | 	 * @see org.springframework.web.servlet.support.RequestContextUtils#getLocaleResolver
  213 | 	 */
  214 | 	public static final String LOCALE_RESOLVER_ATTRIBUTE = DispatcherServlet.class.getName() + ".LOCALE_RESOLVER";
  215 | 
  216 | 	/**
  217 | 	 * Name of request attribute that holds a read-only {@code Map<String,?>}
  218 | 	 * with "input" flash attributes saved by a previous request, if any.
  219 | 	 * @see org.springframework.web.servlet.support.RequestContextUtils#getInputFlashMap(HttpServletRequest)
  220 | 	 */
  221 | 	public static final String INPUT_FLASH_MAP_ATTRIBUTE = DispatcherServlet.class.getName() + ".INPUT_FLASH_MAP";
  222 | 
  223 | 	/**
  224 | 	 * Name of request attribute that holds the "output" {@link FlashMap} with
  225 | 	 * attributes to save for a subsequent request.
  226 | 	 * @see org.springframework.web.servlet.support.RequestContextUtils#getOutputFlashMap(HttpServletRequest)
  227 | 	 */
  228 | 	public static final String OUTPUT_FLASH_MAP_ATTRIBUTE = DispatcherServlet.class.getName() + ".OUTPUT_FLASH_MAP";
  229 | 
  230 | 	/**
  231 | 	 * Name of request attribute that holds the {@link FlashMapManager}.
  232 | 	 * @see org.springframework.web.servlet.support.RequestContextUtils#getFlashMapManager(HttpServletRequest)
  233 | 	 */
  234 | 	public static final String FLASH_MAP_MANAGER_ATTRIBUTE = DispatcherServlet.class.getName() + ".FLASH_MAP_MANAGER";
  235 | 
  236 | 	/**
  237 | 	 * Name of request attribute that exposes an Exception resolved with a
  238 | 	 * {@link HandlerExceptionResolver} but where no view was rendered
  239 | 	 * (for example, setting the status code).
  240 | 	 */
  241 | 	public static final String EXCEPTION_ATTRIBUTE = DispatcherServlet.class.getName() + ".EXCEPTION";
  242 | 
  243 | 	/** Log category to use when no mapped handler is found for a request. */
  244 | 	public static final String PAGE_NOT_FOUND_LOG_CATEGORY = "org.springframework.web.servlet.PageNotFound";
  245 | 
  246 | 	/**
  247 | 	 * Name of the class path resource (relative to the DispatcherServlet class)
  248 | 	 * that defines DispatcherServlet's default strategy names.
  249 | 	 */
  250 | 	private static final String DEFAULT_STRATEGIES_PATH = "DispatcherServlet.properties";
  251 | 
  252 | 	/**
  253 | 	 * Common prefix that DispatcherServlet's default strategy attributes start with.
  254 | 	 */
  255 | 	private static final String DEFAULT_STRATEGIES_PREFIX = "org.springframework.web.servlet";
  256 | 
  257 | 
  258 | 	/** Additional logger to use when no mapped handler is found for a request. */
  259 | 	protected static final Log pageNotFoundLogger = LogFactory.getLog(PAGE_NOT_FOUND_LOG_CATEGORY);
  260 | 
  261 | 	/** Store default strategy implementations. */
  262 | 	private static @Nullable Properties defaultStrategies;
  263 | 
  264 | 	/** Detect all HandlerMappings or just expect "handlerMapping" bean?. */
  265 | 	private boolean detectAllHandlerMappings = true;
  266 | 
  267 | 	/** Detect all HandlerAdapters or just expect "handlerAdapter" bean?. */
  268 | 	private boolean detectAllHandlerAdapters = true;
  269 | 
  270 | 	/** Detect all HandlerExceptionResolvers or just expect "handlerExceptionResolver" bean?. */
  271 | 	private boolean detectAllHandlerExceptionResolvers = true;
  272 | 
  273 | 	/** Detect all ViewResolvers or just expect "viewResolver" bean?. */
  274 | 	private boolean detectAllViewResolvers = true;
  275 | 
  276 | 	/** Perform cleanup of request attributes after include request?. */
  277 | 	private boolean cleanupAfterInclude = true;
  278 | 
  279 | 	/** MultipartResolver used by this servlet. */
  280 | 	private @Nullable MultipartResolver multipartResolver;
  281 | 
  282 | 	/** LocaleResolver used by this servlet. */
  283 | 	private @Nullable LocaleResolver localeResolver;
  284 | 
  285 | 	/** List of HandlerMappings used by this servlet. */
  286 | 	private @Nullable List<HandlerMapping> handlerMappings;
  287 | 
  288 | 	/** List of HandlerAdapters used by this servlet. */
  289 | 	private @Nullable List<HandlerAdapter> handlerAdapters;
  290 | 
  291 | 	/** List of HandlerExceptionResolvers used by this servlet. */
  292 | 	private @Nullable List<HandlerExceptionResolver> handlerExceptionResolvers;
  293 | 
  294 | 	/** RequestToViewNameTranslator used by this servlet. */
  295 | 	private @Nullable RequestToViewNameTranslator viewNameTranslator;
  296 | 
  297 | 	/** FlashMapManager used by this servlet. */
  298 | 	private @Nullable FlashMapManager flashMapManager;
  299 | 
  300 | 	/** List of ViewResolvers used by this servlet. */
  301 | 	private @Nullable List<ViewResolver> viewResolvers;
  302 | 
  303 | 	private boolean parseRequestPath;
  304 | 
  305 | 
  306 | 	/**
  307 | 	 * Create a new {@code DispatcherServlet} that will create its own internal web
  308 | 	 * application context based on defaults and values provided through servlet
  309 | 	 * init-params. Typically used in Servlet 2.5 or earlier environments, where the only
  310 | 	 * option for servlet registration is through {@code web.xml} which requires the use
  311 | 	 * of a no-arg constructor.
  312 | 	 * <p>Calling {@link #setContextConfigLocation} (init-param 'contextConfigLocation')
  313 | 	 * will dictate which XML files will be loaded by the
  314 | 	 * {@linkplain #DEFAULT_CONTEXT_CLASS default XmlWebApplicationContext}
  315 | 	 * <p>Calling {@link #setContextClass} (init-param 'contextClass') overrides the
  316 | 	 * default {@code XmlWebApplicationContext} and allows for specifying an alternative class,
  317 | 	 * such as {@code AnnotationConfigWebApplicationContext}.
  318 | 	 * <p>Calling {@link #setContextInitializerClasses} (init-param 'contextInitializerClasses')
  319 | 	 * indicates which {@code ApplicationContextInitializer} classes should be used to
  320 | 	 * further configure the internal application context prior to refresh().
  321 | 	 * @see #DispatcherServlet(WebApplicationContext)
  322 | 	 */
  323 | 	public DispatcherServlet() {
  324 | 		setDispatchOptionsRequest(true);
  325 | 	}
  326 | 
  327 | 	/**
  328 | 	 * Create a new {@code DispatcherServlet} with the given web application context. This
  329 | 	 * constructor is useful in Servlet environments where instance-based registration
  330 | 	 * of servlets is possible through the {@link ServletContext#addServlet} API.
  331 | 	 * <p>Using this constructor indicates that the following properties / init-params
  332 | 	 * will be ignored:
  333 | 	 * <ul>
  334 | 	 * <li>{@link #setContextClass(Class)} / 'contextClass'</li>
  335 | 	 * <li>{@link #setContextConfigLocation(String)} / 'contextConfigLocation'</li>
  336 | 	 * <li>{@link #setContextAttribute(String)} / 'contextAttribute'</li>
  337 | 	 * <li>{@link #setNamespace(String)} / 'namespace'</li>
  338 | 	 * </ul>
  339 | 	 * <p>The given web application context may or may not yet be {@linkplain
  340 | 	 * ConfigurableApplicationContext#refresh() refreshed}. If it has <strong>not</strong>
  341 | 	 * already been refreshed (the recommended approach), then the following will occur:
  342 | 	 * <ul>
  343 | 	 * <li>If the given context does not already have a {@linkplain
  344 | 	 * ConfigurableApplicationContext#setParent parent}, the root application context
  345 | 	 * will be set as the parent.</li>
  346 | 	 * <li>If the given context has not already been assigned an {@linkplain
  347 | 	 * ConfigurableApplicationContext#setId id}, one will be assigned to it</li>
  348 | 	 * <li>{@code ServletContext} and {@code ServletConfig} objects will be delegated to
  349 | 	 * the application context</li>
  350 | 	 * <li>{@link #postProcessWebApplicationContext} will be called</li>
  351 | 	 * <li>Any {@code ApplicationContextInitializer}s specified through the
  352 | 	 * "contextInitializerClasses" init-param or through the {@link
  353 | 	 * #setContextInitializers} property will be applied.</li>
  354 | 	 * <li>{@link ConfigurableApplicationContext#refresh refresh()} will be called if the
  355 | 	 * context implements {@link ConfigurableApplicationContext}</li>
  356 | 	 * </ul>
  357 | 	 * If the context has already been refreshed, none of the above will occur, under the
  358 | 	 * assumption that the user has performed these actions (or not) per their specific
  359 | 	 * needs.
  360 | 	 * <p>See {@link org.springframework.web.WebApplicationInitializer} for usage examples.
  361 | 	 * @param webApplicationContext the context to use
  362 | 	 * @see #initWebApplicationContext
  363 | 	 * @see #configureAndRefreshWebApplicationContext
  364 | 	 * @see org.springframework.web.WebApplicationInitializer
  365 | 	 */
  366 | 	public DispatcherServlet(WebApplicationContext webApplicationContext) {
  367 | 		super(webApplicationContext);
  368 | 		setDispatchOptionsRequest(true);
  369 | 	}
  370 | 
  371 | 
  372 | 	/**
  373 | 	 * Set whether to detect all HandlerMapping beans in this servlet's context. Otherwise,
  374 | 	 * just a single bean with name "handlerMapping" will be expected.
  375 | 	 * <p>Default is "true". Turn this off if you want this servlet to use a single
  376 | 	 * HandlerMapping, despite multiple HandlerMapping beans being defined in the context.
  377 | 	 */
  378 | 	public void setDetectAllHandlerMappings(boolean detectAllHandlerMappings) {
  379 | 		this.detectAllHandlerMappings = detectAllHandlerMappings;
  380 | 	}
  381 | 
  382 | 	/**
  383 | 	 * Set whether to detect all HandlerAdapter beans in this servlet's context. Otherwise,
  384 | 	 * just a single bean with name "handlerAdapter" will be expected.
  385 | 	 * <p>Default is "true". Turn this off if you want this servlet to use a single
  386 | 	 * HandlerAdapter, despite multiple HandlerAdapter beans being defined in the context.
  387 | 	 */
  388 | 	public void setDetectAllHandlerAdapters(boolean detectAllHandlerAdapters) {
  389 | 		this.detectAllHandlerAdapters = detectAllHandlerAdapters;
  390 | 	}
  391 | 
  392 | 	/**
  393 | 	 * Set whether to detect all HandlerExceptionResolver beans in this servlet's context. Otherwise,
  394 | 	 * just a single bean with name "handlerExceptionResolver" will be expected.
  395 | 	 * <p>Default is "true". Turn this off if you want this servlet to use a single
  396 | 	 * HandlerExceptionResolver, despite multiple HandlerExceptionResolver beans being defined in the context.
  397 | 	 */
  398 | 	public void setDetectAllHandlerExceptionResolvers(boolean detectAllHandlerExceptionResolvers) {
  399 | 		this.detectAllHandlerExceptionResolvers = detectAllHandlerExceptionResolvers;
  400 | 	}
  401 | 
  402 | 	/**
  403 | 	 * Set whether to detect all ViewResolver beans in this servlet's context. Otherwise,
  404 | 	 * just a single bean with name "viewResolver" will be expected.
  405 | 	 * <p>Default is "true". Turn this off if you want this servlet to use a single
  406 | 	 * ViewResolver, despite multiple ViewResolver beans being defined in the context.
  407 | 	 */
  408 | 	public void setDetectAllViewResolvers(boolean detectAllViewResolvers) {
  409 | 		this.detectAllViewResolvers = detectAllViewResolvers;
  410 | 	}
  411 | 
  412 | 	/**
  413 | 	 * Set whether to perform cleanup of request attributes after an include request, that is,
  414 | 	 * whether to reset the original state of all request attributes after the DispatcherServlet
  415 | 	 * has processed within an include request. Otherwise, just the DispatcherServlet's own
  416 | 	 * request attributes will be reset, but not model attributes for JSPs or special attributes
  417 | 	 * set by views (for example, JSTL's).
  418 | 	 * <p>Default is "true", which is strongly recommended. Views should not rely on request attributes
  419 | 	 * having been set by (dynamic) includes. This allows JSP views rendered by an included controller
  420 | 	 * to use any model attributes, even with the same names as in the main JSP, without causing side
  421 | 	 * effects. Only turn this off for special needs, for example to deliberately allow main JSPs to
  422 | 	 * access attributes from JSP views rendered by an included controller.
  423 | 	 */
  424 | 	public void setCleanupAfterInclude(boolean cleanupAfterInclude) {
  425 | 		this.cleanupAfterInclude = cleanupAfterInclude;
  426 | 	}
  427 | 
  428 | 
  429 | 	/**
  430 | 	 * This implementation calls {@link #initStrategies}.
  431 | 	 */
  432 | 	@Override
  433 | 	protected void onRefresh(ApplicationContext context) {
  434 | 		initStrategies(context);
  435 | 	}
  436 | 
  437 | 	/**
  438 | 	 * Initialize the strategy objects that this servlet uses.
  439 | 	 * <p>May be overridden in subclasses in order to initialize further strategy objects.
  440 | 	 */
  441 | 	protected void initStrategies(ApplicationContext context) {
  442 | 		initMultipartResolver(context);
  443 | 		initLocaleResolver(context);
  444 | 		initHandlerMappings(context);
  445 | 		initHandlerAdapters(context);
  446 | 		initHandlerExceptionResolvers(context);
  447 | 		initRequestToViewNameTranslator(context);
  448 | 		initViewResolvers(context);
  449 | 		initFlashMapManager(context);
  450 | 	}
  451 | 
  452 | 	/**
  453 | 	 * Initialize the MultipartResolver used by this class.
  454 | 	 * <p>If no bean is defined with the given name in the BeanFactory for this namespace,
  455 | 	 * no multipart handling is provided.
  456 | 	 */
  457 | 	private void initMultipartResolver(ApplicationContext context) {
  458 | 		try {
  459 | 			this.multipartResolver = context.getBean(MULTIPART_RESOLVER_BEAN_NAME, MultipartResolver.class);
  460 | 			if (logger.isTraceEnabled()) {
  461 | 				logger.trace("Detected " + this.multipartResolver);
  462 | 			}
  463 | 			else if (logger.isDebugEnabled()) {
  464 | 				logger.debug("Detected " + this.multipartResolver.getClass().getSimpleName());
  465 | 			}
  466 | 		}
  467 | 		catch (NoSuchBeanDefinitionException ex) {
  468 | 			// Default is no multipart resolver.
  469 | 			this.multipartResolver = null;
  470 | 			if (logger.isTraceEnabled()) {
  471 | 				logger.trace("No MultipartResolver '" + MULTIPART_RESOLVER_BEAN_NAME + "' declared");
  472 | 			}
  473 | 		}
  474 | 	}
  475 | 
  476 | 	/**
  477 | 	 * Initialize the LocaleResolver used by this class.
  478 | 	 * <p>If no bean is defined with the given name in the BeanFactory for this namespace,
  479 | 	 * we default to AcceptHeaderLocaleResolver.
  480 | 	 */
  481 | 	private void initLocaleResolver(ApplicationContext context) {
  482 | 		try {
  483 | 			this.localeResolver = context.getBean(LOCALE_RESOLVER_BEAN_NAME, LocaleResolver.class);
  484 | 			if (logger.isTraceEnabled()) {
  485 | 				logger.trace("Detected " + this.localeResolver);
  486 | 			}
  487 | 			else if (logger.isDebugEnabled()) {
  488 | 				logger.debug("Detected " + this.localeResolver.getClass().getSimpleName());
  489 | 			}
  490 | 		}
  491 | 		catch (NoSuchBeanDefinitionException ex) {
  492 | 			// We need to use the default.
  493 | 			this.localeResolver = getDefaultStrategy(context, LocaleResolver.class);
  494 | 			if (logger.isTraceEnabled()) {
  495 | 				logger.trace("No LocaleResolver '" + LOCALE_RESOLVER_BEAN_NAME +
  496 | 						"': using default [" + this.localeResolver.getClass().getSimpleName() + "]");
  497 | 			}
  498 | 		}
  499 | 	}
  500 | 
  501 | 	/**
  502 | 	 * Initialize the HandlerMappings used by this class.
  503 | 	 * <p>If no HandlerMapping beans are defined in the BeanFactory for this namespace,
  504 | 	 * we default to BeanNameUrlHandlerMapping.
  505 | 	 */
  506 | 	private void initHandlerMappings(ApplicationContext context) {
  507 | 		this.handlerMappings = null;
  508 | 
  509 | 		if (this.detectAllHandlerMappings) {
  510 | 			// Find all HandlerMappings in the ApplicationContext, including ancestor contexts.
  511 | 			Map<String, HandlerMapping> matchingBeans =
  512 | 					BeanFactoryUtils.beansOfTypeIncludingAncestors(context, HandlerMapping.class, true, false);
  513 | 			if (!matchingBeans.isEmpty()) {
  514 | 				this.handlerMappings = new ArrayList<>(matchingBeans.values());
  515 | 				// We keep HandlerMappings in sorted order.
  516 | 				AnnotationAwareOrderComparator.sort(this.handlerMappings);
  517 | 			}
  518 | 		}
  519 | 		else {
  520 | 			try {
  521 | 				HandlerMapping hm = context.getBean(HANDLER_MAPPING_BEAN_NAME, HandlerMapping.class);
  522 | 				this.handlerMappings = Collections.singletonList(hm);
  523 | 			}
  524 | 			catch (NoSuchBeanDefinitionException ex) {
  525 | 				// Ignore, we'll add a default HandlerMapping later.
  526 | 			}
  527 | 		}
  528 | 
  529 | 		// Ensure we have at least one HandlerMapping, by registering
  530 | 		// a default HandlerMapping if no other mappings are found.
  531 | 		if (this.handlerMappings == null) {
  532 | 			this.handlerMappings = getDefaultStrategies(context, HandlerMapping.class);
  533 | 			if (logger.isTraceEnabled()) {
  534 | 				logger.trace("No HandlerMappings declared for servlet '" + getServletName() +
  535 | 						"': using default strategies from DispatcherServlet.properties");
  536 | 			}
  537 | 		}
  538 | 
  539 | 		for (HandlerMapping mapping : this.handlerMappings) {
  540 | 			if (mapping.usesPathPatterns()) {
  541 | 				this.parseRequestPath = true;
  542 | 				break;
  543 | 			}
  544 | 		}
  545 | 	}
  546 | 
  547 | 	/**
  548 | 	 * Initialize the HandlerAdapters used by this class.
  549 | 	 * <p>If no HandlerAdapter beans are defined in the BeanFactory for this namespace,
  550 | 	 * we default to SimpleControllerHandlerAdapter.
  551 | 	 */
  552 | 	private void initHandlerAdapters(ApplicationContext context) {
  553 | 		this.handlerAdapters = null;
  554 | 
  555 | 		if (this.detectAllHandlerAdapters) {
  556 | 			// Find all HandlerAdapters in the ApplicationContext, including ancestor contexts.
  557 | 			Map<String, HandlerAdapter> matchingBeans =
  558 | 					BeanFactoryUtils.beansOfTypeIncludingAncestors(context, HandlerAdapter.class, true, false);
  559 | 			if (!matchingBeans.isEmpty()) {
  560 | 				this.handlerAdapters = new ArrayList<>(matchingBeans.values());
  561 | 				// We keep HandlerAdapters in sorted order.
  562 | 				AnnotationAwareOrderComparator.sort(this.handlerAdapters);
  563 | 			}
  564 | 		}
  565 | 		else {
  566 | 			try {
  567 | 				HandlerAdapter ha = context.getBean(HANDLER_ADAPTER_BEAN_NAME, HandlerAdapter.class);
  568 | 				this.handlerAdapters = Collections.singletonList(ha);
  569 | 			}
  570 | 			catch (NoSuchBeanDefinitionException ex) {
  571 | 				// Ignore, we'll add a default HandlerAdapter later.
  572 | 			}
  573 | 		}
  574 | 
  575 | 		// Ensure we have at least some HandlerAdapters, by registering
  576 | 		// default HandlerAdapters if no other adapters are found.
  577 | 		if (this.handlerAdapters == null) {
  578 | 			this.handlerAdapters = getDefaultStrategies(context, HandlerAdapter.class);
  579 | 			if (logger.isTraceEnabled()) {
  580 | 				logger.trace("No HandlerAdapters declared for servlet '" + getServletName() +
  581 | 						"': using default strategies from DispatcherServlet.properties");
  582 | 			}
  583 | 		}
  584 | 	}
  585 | 
  586 | 	/**
  587 | 	 * Initialize the HandlerExceptionResolver used by this class.
  588 | 	 * <p>If no bean is defined with the given name in the BeanFactory for this namespace,
  589 | 	 * we default to no exception resolver.
  590 | 	 */
  591 | 	private void initHandlerExceptionResolvers(ApplicationContext context) {
  592 | 		this.handlerExceptionResolvers = null;
  593 | 
  594 | 		if (this.detectAllHandlerExceptionResolvers) {
  595 | 			// Find all HandlerExceptionResolvers in the ApplicationContext, including ancestor contexts.
  596 | 			Map<String, HandlerExceptionResolver> matchingBeans = BeanFactoryUtils
  597 | 					.beansOfTypeIncludingAncestors(context, HandlerExceptionResolver.class, true, false);
  598 | 			if (!matchingBeans.isEmpty()) {
  599 | 				this.handlerExceptionResolvers = new ArrayList<>(matchingBeans.values());
  600 | 				// We keep HandlerExceptionResolvers in sorted order.
  601 | 				AnnotationAwareOrderComparator.sort(this.handlerExceptionResolvers);
  602 | 			}
  603 | 		}
  604 | 		else {
  605 | 			try {
  606 | 				HandlerExceptionResolver her =
  607 | 						context.getBean(HANDLER_EXCEPTION_RESOLVER_BEAN_NAME, HandlerExceptionResolver.class);
  608 | 				this.handlerExceptionResolvers = Collections.singletonList(her);
  609 | 			}
  610 | 			catch (NoSuchBeanDefinitionException ex) {
  611 | 				// Ignore, no HandlerExceptionResolver is fine too.
  612 | 			}
  613 | 		}
  614 | 
  615 | 		// Ensure we have at least some HandlerExceptionResolvers, by registering
  616 | 		// default HandlerExceptionResolvers if no other resolvers are found.
  617 | 		if (this.handlerExceptionResolvers == null) {
  618 | 			this.handlerExceptionResolvers = getDefaultStrategies(context, HandlerExceptionResolver.class);
  619 | 			if (logger.isTraceEnabled()) {
  620 | 				logger.trace("No HandlerExceptionResolvers declared in servlet '" + getServletName() +
  621 | 						"': using default strategies from DispatcherServlet.properties");
  622 | 			}
  623 | 		}
  624 | 	}
  625 | 
  626 | 	/**
  627 | 	 * Initialize the RequestToViewNameTranslator used by this servlet instance.
  628 | 	 * <p>If no implementation is configured then we default to DefaultRequestToViewNameTranslator.
  629 | 	 */
  630 | 	private void initRequestToViewNameTranslator(ApplicationContext context) {
  631 | 		try {
  632 | 			this.viewNameTranslator =
  633 | 					context.getBean(REQUEST_TO_VIEW_NAME_TRANSLATOR_BEAN_NAME, RequestToViewNameTranslator.class);
  634 | 			if (logger.isTraceEnabled()) {
  635 | 				logger.trace("Detected " + this.viewNameTranslator);
  636 | 			}
  637 | 			else if (logger.isDebugEnabled()) {
  638 | 				logger.debug("Detected " + this.viewNameTranslator.getClass().getSimpleName());
  639 | 			}
  640 | 		}
  641 | 		catch (NoSuchBeanDefinitionException ex) {
  642 | 			// We need to use the default.
  643 | 			this.viewNameTranslator = getDefaultStrategy(context, RequestToViewNameTranslator.class);
  644 | 			if (logger.isTraceEnabled()) {
  645 | 				logger.trace("No RequestToViewNameTranslator '" + REQUEST_TO_VIEW_NAME_TRANSLATOR_BEAN_NAME +
  646 | 						"': using default [" + this.viewNameTranslator.getClass().getSimpleName() + "]");
  647 | 			}
  648 | 		}
  649 | 	}
  650 | 
  651 | 	/**
  652 | 	 * Initialize the ViewResolvers used by this class.
  653 | 	 * <p>If no ViewResolver beans are defined in the BeanFactory for this
  654 | 	 * namespace, we default to InternalResourceViewResolver.
  655 | 	 */
  656 | 	private void initViewResolvers(ApplicationContext context) {
  657 | 		this.viewResolvers = null;
  658 | 
  659 | 		if (this.detectAllViewResolvers) {
  660 | 			// Find all ViewResolvers in the ApplicationContext, including ancestor contexts.
  661 | 			Map<String, ViewResolver> matchingBeans =
  662 | 					BeanFactoryUtils.beansOfTypeIncludingAncestors(context, ViewResolver.class, true, false);
  663 | 			if (!matchingBeans.isEmpty()) {
  664 | 				this.viewResolvers = new ArrayList<>(matchingBeans.values());
  665 | 				// We keep ViewResolvers in sorted order.
  666 | 				AnnotationAwareOrderComparator.sort(this.viewResolvers);
  667 | 			}
  668 | 		}
  669 | 		else {
  670 | 			try {
  671 | 				ViewResolver vr = context.getBean(VIEW_RESOLVER_BEAN_NAME, ViewResolver.class);
  672 | 				this.viewResolvers = Collections.singletonList(vr);
  673 | 			}
  674 | 			catch (NoSuchBeanDefinitionException ex) {
  675 | 				// Ignore, we'll add a default ViewResolver later.
  676 | 			}
  677 | 		}
  678 | 
  679 | 		// Ensure we have at least one ViewResolver, by registering
  680 | 		// a default ViewResolver if no other resolvers are found.
  681 | 		if (this.viewResolvers == null) {
  682 | 			this.viewResolvers = getDefaultStrategies(context, ViewResolver.class);
  683 | 			if (logger.isTraceEnabled()) {
  684 | 				logger.trace("No ViewResolvers declared for servlet '" + getServletName() +
  685 | 						"': using default strategies from DispatcherServlet.properties");
  686 | 			}
  687 | 		}
  688 | 	}
  689 | 
  690 | 	/**
  691 | 	 * Initialize the {@link FlashMapManager} used by this servlet instance.
  692 | 	 * <p>If no implementation is configured then we default to
  693 | 	 * {@code org.springframework.web.servlet.support.DefaultFlashMapManager}.
  694 | 	 */
  695 | 	private void initFlashMapManager(ApplicationContext context) {
  696 | 		try {
  697 | 			this.flashMapManager = context.getBean(FLASH_MAP_MANAGER_BEAN_NAME, FlashMapManager.class);
  698 | 			if (logger.isTraceEnabled()) {
  699 | 				logger.trace("Detected " + this.flashMapManager);
  700 | 			}
  701 | 			else if (logger.isDebugEnabled()) {
  702 | 				logger.debug("Detected " + this.flashMapManager.getClass().getSimpleName());
  703 | 			}
  704 | 		}
  705 | 		catch (NoSuchBeanDefinitionException ex) {
  706 | 			// We need to use the default.
  707 | 			this.flashMapManager = getDefaultStrategy(context, FlashMapManager.class);
  708 | 			if (logger.isTraceEnabled()) {
  709 | 				logger.trace("No FlashMapManager '" + FLASH_MAP_MANAGER_BEAN_NAME +
  710 | 						"': using default [" + this.flashMapManager.getClass().getSimpleName() + "]");
  711 | 			}
  712 | 		}
  713 | 	}
  714 | 
  715 | 	/**
  716 | 	 * Obtain this servlet's MultipartResolver, if any.
  717 | 	 * @return the MultipartResolver used by this servlet, or {@code null} if none
  718 | 	 * (indicating that no multipart support is available)
  719 | 	 */
  720 | 	public final @Nullable MultipartResolver getMultipartResolver() {
  721 | 		return this.multipartResolver;
  722 | 	}
  723 | 
  724 | 	/**
  725 | 	 * Return the configured {@link HandlerMapping} beans that were detected by
  726 | 	 * type in the {@link WebApplicationContext} or initialized based on the
  727 | 	 * default set of strategies from {@literal DispatcherServlet.properties}.
  728 | 	 * <p><strong>Note:</strong> This method may return {@code null} if invoked
  729 | 	 * prior to {@link #onRefresh(ApplicationContext)}.
  730 | 	 * @return an immutable list with the configured mappings, or {@code null}
  731 | 	 * if not initialized yet
  732 | 	 * @since 5.0
  733 | 	 */
  734 | 	public final @Nullable List<HandlerMapping> getHandlerMappings() {
  735 | 		return (this.handlerMappings != null ? Collections.unmodifiableList(this.handlerMappings) : null);
  736 | 	}
  737 | 
  738 | 	/**
  739 | 	 * Return the default strategy object for the given strategy interface.
  740 | 	 * <p>The default implementation delegates to {@link #getDefaultStrategies},
  741 | 	 * expecting a single object in the list.
  742 | 	 * @param context the current WebApplicationContext
  743 | 	 * @param strategyInterface the strategy interface
  744 | 	 * @return the corresponding strategy object
  745 | 	 * @see #getDefaultStrategies
  746 | 	 */
  747 | 	protected <T> T getDefaultStrategy(ApplicationContext context, Class<T> strategyInterface) {
  748 | 		List<T> strategies = getDefaultStrategies(context, strategyInterface);
  749 | 		if (strategies.size() != 1) {
  750 | 			throw new BeanInitializationException(
  751 | 					"DispatcherServlet needs exactly 1 strategy for interface [" + strategyInterface.getName() + "]");
  752 | 		}
  753 | 		return strategies.get(0);
  754 | 	}
  755 | 
  756 | 	/**
  757 | 	 * Create a List of default strategy objects for the given strategy interface.
  758 | 	 * <p>The default implementation uses the "DispatcherServlet.properties" file (in the same
  759 | 	 * package as the DispatcherServlet class) to determine the class names. It instantiates
  760 | 	 * the strategy objects through the context's BeanFactory.
  761 | 	 * @param context the current WebApplicationContext
  762 | 	 * @param strategyInterface the strategy interface
  763 | 	 * @return the List of corresponding strategy objects
  764 | 	 */
  765 | 	@SuppressWarnings("unchecked")
  766 | 	protected <T> List<T> getDefaultStrategies(ApplicationContext context, Class<T> strategyInterface) {
  767 | 		if (defaultStrategies == null) {
  768 | 			try {
  769 | 				// Load default strategy implementations from properties file.
  770 | 				// This is currently strictly internal and not meant to be customized
  771 | 				// by application developers.
  772 | 				ClassPathResource resource = new ClassPathResource(DEFAULT_STRATEGIES_PATH, DispatcherServlet.class);
  773 | 				defaultStrategies = PropertiesLoaderUtils.loadProperties(resource);
  774 | 			}
  775 | 			catch (IOException ex) {
  776 | 				throw new IllegalStateException("Could not load '" + DEFAULT_STRATEGIES_PATH + "': " + ex.getMessage());
  777 | 			}
  778 | 		}
  779 | 
  780 | 		String key = strategyInterface.getName();
  781 | 		String value = defaultStrategies.getProperty(key);
  782 | 		if (value != null) {
  783 | 			String[] classNames = StringUtils.commaDelimitedListToStringArray(value);
  784 | 			List<T> strategies = new ArrayList<>(classNames.length);
  785 | 			for (String className : classNames) {
  786 | 				try {
  787 | 					Class<?> clazz = ClassUtils.forName(className, DispatcherServlet.class.getClassLoader());
  788 | 					Object strategy = createDefaultStrategy(context, clazz);
  789 | 					strategies.add((T) strategy);
  790 | 				}
  791 | 				catch (ClassNotFoundException ex) {
  792 | 					throw new BeanInitializationException(
  793 | 							"Could not find DispatcherServlet's default strategy class [" + className +
  794 | 							"] for interface [" + key + "]", ex);
  795 | 				}
  796 | 				catch (LinkageError err) {
  797 | 					throw new BeanInitializationException(
  798 | 							"Unresolvable class definition for DispatcherServlet's default strategy class [" +
  799 | 							className + "] for interface [" + key + "]", err);
  800 | 				}
  801 | 			}
  802 | 			return strategies;
  803 | 		}
  804 | 		else {
  805 | 			return Collections.emptyList();
  806 | 		}
  807 | 	}
  808 | 
  809 | 	/**
  810 | 	 * Create a default strategy.
  811 | 	 * <p>The default implementation uses
  812 | 	 * {@link org.springframework.beans.factory.config.AutowireCapableBeanFactory#createBean(Class)}.
  813 | 	 * @param context the current WebApplicationContext
  814 | 	 * @param clazz the strategy implementation class to instantiate
  815 | 	 * @return the fully configured strategy instance
  816 | 	 * @see org.springframework.context.ApplicationContext#getAutowireCapableBeanFactory()
  817 | 	 * @see org.springframework.beans.factory.config.AutowireCapableBeanFactory#createBean(Class)
  818 | 	 */
  819 | 	protected Object createDefaultStrategy(ApplicationContext context, Class<?> clazz) {
  820 | 		return context.getAutowireCapableBeanFactory().createBean(clazz);
  821 | 	}
  822 | 
  823 | 
  824 | 	/**
  825 | 	 * Exposes the DispatcherServlet-specific request attributes and delegates to {@link #doDispatch}
  826 | 	 * for the actual dispatching.
  827 | 	 */
  828 | 	@Override
  829 | 	protected void doService(HttpServletRequest request, HttpServletResponse response) throws Exception {
  830 | 		logRequest(request);
  831 | 
  832 | 		// Keep a snapshot of the request attributes in case of an include,
  833 | 		// to be able to restore the original attributes after the include.
  834 | 		Map<String, Object> attributesSnapshot = null;
  835 | 		if (WebUtils.isIncludeRequest(request)) {
  836 | 			attributesSnapshot = new HashMap<>();
  837 | 			Enumeration<?> attrNames = request.getAttributeNames();
  838 | 			while (attrNames.hasMoreElements()) {
  839 | 				String attrName = (String) attrNames.nextElement();
  840 | 				if (this.cleanupAfterInclude || attrName.startsWith(DEFAULT_STRATEGIES_PREFIX)) {
  841 | 					attributesSnapshot.put(attrName, request.getAttribute(attrName));
  842 | 				}
  843 | 			}
  844 | 		}
  845 | 
  846 | 		// Make framework objects available to handlers and view objects.
  847 | 		request.setAttribute(WEB_APPLICATION_CONTEXT_ATTRIBUTE, getWebApplicationContext());
  848 | 		request.setAttribute(LOCALE_RESOLVER_ATTRIBUTE, this.localeResolver);
  849 | 
  850 | 		if (this.flashMapManager != null) {
  851 | 			FlashMap inputFlashMap = this.flashMapManager.retrieveAndUpdate(request, response);
  852 | 			if (inputFlashMap != null) {
  853 | 				request.setAttribute(INPUT_FLASH_MAP_ATTRIBUTE, Collections.unmodifiableMap(inputFlashMap));
  854 | 			}
  855 | 			request.setAttribute(OUTPUT_FLASH_MAP_ATTRIBUTE, new FlashMap());
  856 | 			request.setAttribute(FLASH_MAP_MANAGER_ATTRIBUTE, this.flashMapManager);
  857 | 		}
  858 | 
  859 | 		RequestPath previousRequestPath = null;
  860 | 		if (this.parseRequestPath) {
  861 | 			previousRequestPath = (RequestPath) request.getAttribute(ServletRequestPathUtils.PATH_ATTRIBUTE);
  862 | 			ServletRequestPathUtils.parseAndCache(request);
  863 | 		}
  864 | 
  865 | 		try {
  866 | 			doDispatch(request, response);
  867 | 		}
  868 | 		finally {
  869 | 			if (!WebAsyncUtils.getAsyncManager(request).isConcurrentHandlingStarted()) {
  870 | 				// Restore the original attribute snapshot, in case of an include.
  871 | 				if (attributesSnapshot != null) {
  872 | 					restoreAttributesAfterInclude(request, attributesSnapshot);
  873 | 				}
  874 | 			}
  875 | 			if (this.parseRequestPath) {
  876 | 				ServletRequestPathUtils.setParsedRequestPath(previousRequestPath, request);
  877 | 			}
  878 | 		}
  879 | 	}
  880 | 
  881 | 	private void logRequest(HttpServletRequest request) {
  882 | 		LogFormatUtils.traceDebug(logger, traceOn -> {
  883 | 			String params;
  884 | 			String contentType = request.getContentType();
  885 | 			if (StringUtils.startsWithIgnoreCase(contentType, "multipart/")) {
  886 | 				params = "multipart";
  887 | 			}
  888 | 			else if (isEnableLoggingRequestDetails()) {
  889 | 				params = request.getParameterMap().entrySet().stream()
  890 | 						.map(entry -> entry.getKey() + ":" + Arrays.toString(entry.getValue()))
  891 | 						.collect(Collectors.joining(", "));
  892 | 			}
  893 | 			else {
  894 | 				// Avoid request body parsing for form data
  895 | 				params = (StringUtils.startsWithIgnoreCase(contentType, MediaType.APPLICATION_FORM_URLENCODED_VALUE) ||
  896 | 						!request.getParameterMap().isEmpty() ? "masked" : "");
  897 | 			}
  898 | 
  899 | 			String queryString = request.getQueryString();
  900 | 			String queryClause = (StringUtils.hasLength(queryString) ? "?" + queryString : "");
  901 | 			String dispatchType = (!DispatcherType.REQUEST.equals(request.getDispatcherType()) ?
  902 | 					"\"" + request.getDispatcherType() + "\" dispatch for " : "");
  903 | 			String message = (dispatchType + request.getMethod() + " \"" + getRequestUri(request) +
  904 | 					queryClause + "\", parameters={" + params + "}");
  905 | 
  906 | 			if (traceOn) {
  907 | 				List<String> values = Collections.list(request.getHeaderNames());
  908 | 				String headers;
  909 | 				if (isEnableLoggingRequestDetails()) {
  910 | 					headers = values.stream().map(name -> name + ":" + Collections.list(request.getHeaders(name)))
  911 | 							.collect(Collectors.joining(", "));
  912 | 				}
  913 | 				else {
  914 | 					headers = (!values.isEmpty() ? "masked" : "");
  915 | 				}
  916 | 				return message + ", headers={" + headers + "} in DispatcherServlet '" + getServletName() + "'";
  917 | 			}
  918 | 			else {
  919 | 				return message;
  920 | 			}
  921 | 		});
  922 | 	}
  923 | 
  924 | 	/**
  925 | 	 * Process the actual dispatching to the handler.
  926 | 	 * <p>The handler will be obtained by applying the servlet's HandlerMappings in order.
  927 | 	 * The HandlerAdapter will be obtained by querying the servlet's installed HandlerAdapters
  928 | 	 * to find the first that supports the handler class.
  929 | 	 * <p>All HTTP methods are handled by this method. It's up to HandlerAdapters or handlers
  930 | 	 * themselves to decide which methods are acceptable.
  931 | 	 * @param request current HTTP request
  932 | 	 * @param response current HTTP response
  933 | 	 * @throws Exception in case of any kind of processing failure
  934 | 	 */
  935 | 	protected void doDispatch(HttpServletRequest request, HttpServletResponse response) throws Exception {
  936 | 		HttpServletRequest processedRequest = request;
  937 | 		HandlerExecutionChain mappedHandler = null;
  938 | 		boolean multipartRequestParsed = false;
  939 | 
  940 | 		WebAsyncManager asyncManager = WebAsyncUtils.getAsyncManager(request);
  941 | 
  942 | 		try {
  943 | 			ModelAndView mv = null;
  944 | 			Exception dispatchException = null;
  945 | 
  946 | 			try {
  947 | 				processedRequest = checkMultipart(request);
  948 | 				multipartRequestParsed = (processedRequest != request);
  949 | 
  950 | 				// Determine handler for the current request.
  951 | 				mappedHandler = getHandler(processedRequest);
  952 | 				if (mappedHandler == null) {
  953 | 					noHandlerFound(processedRequest, response);
  954 | 					return;
  955 | 				}
  956 | 
  957 | 				if (!mappedHandler.applyPreHandle(processedRequest, response)) {
  958 | 					return;
  959 | 				}
  960 | 
  961 | 				// Determine handler adapter and invoke the handler.
  962 | 				HandlerAdapter ha = getHandlerAdapter(mappedHandler.getHandler());
  963 | 				mv = ha.handle(processedRequest, response, mappedHandler.getHandler());
  964 | 
  965 | 				if (asyncManager.isConcurrentHandlingStarted()) {
  966 | 					return;
  967 | 				}
  968 | 
  969 | 				applyDefaultViewName(processedRequest, mv);
  970 | 				mappedHandler.applyPostHandle(processedRequest, response, mv);
  971 | 			}
  972 | 			catch (Exception ex) {
  973 | 				dispatchException = ex;
  974 | 			}
  975 | 			catch (Throwable err) {
  976 | 				// As of 4.3, we're processing Errors thrown from handler methods as well,
  977 | 				// making them available for @ExceptionHandler methods and other scenarios.
  978 | 				dispatchException = new ServletException("Handler dispatch failed: " + err, err);
  979 | 			}
  980 | 			processDispatchResult(processedRequest, response, mappedHandler, mv, dispatchException);
  981 | 		}
  982 | 		catch (Exception ex) {
  983 | 			triggerAfterCompletion(processedRequest, response, mappedHandler, ex);
  984 | 		}
  985 | 		catch (Throwable err) {
  986 | 			triggerAfterCompletion(processedRequest, response, mappedHandler,
  987 | 					new ServletException("Handler processing failed: " + err, err));
  988 | 		}
  989 | 		finally {
  990 | 			if (asyncManager.isConcurrentHandlingStarted()) {
  991 | 				// Instead of postHandle and afterCompletion
  992 | 				if (mappedHandler != null) {
  993 | 					mappedHandler.applyAfterConcurrentHandlingStarted(processedRequest, response);
  994 | 				}
  995 | 				asyncManager.setMultipartRequestParsed(multipartRequestParsed);
  996 | 			}
  997 | 			else {
  998 | 				// Clean up any resources used by a multipart request.
  999 | 				if (multipartRequestParsed || asyncManager.isMultipartRequestParsed()) {
 1000 | 					cleanupMultipart(processedRequest);
 1001 | 				}
 1002 | 			}
 1003 | 		}
 1004 | 	}
 1005 | 
 1006 | 	/**
 1007 | 	 * Do we need view name translation?
 1008 | 	 */
 1009 | 	private void applyDefaultViewName(HttpServletRequest request, @Nullable ModelAndView mv) throws Exception {
 1010 | 		if (mv != null && !mv.hasView()) {
 1011 | 			String defaultViewName = getDefaultViewName(request);
 1012 | 			if (defaultViewName != null) {
 1013 | 				mv.setViewName(defaultViewName);
 1014 | 			}
 1015 | 		}
 1016 | 	}
 1017 | 
 1018 | 	/**
 1019 | 	 * Handle the result of handler selection and handler invocation, which is
 1020 | 	 * either a ModelAndView or an Exception to be resolved to a ModelAndView.
 1021 | 	 */
 1022 | 	private void processDispatchResult(HttpServletRequest request, HttpServletResponse response,
 1023 | 			@Nullable HandlerExecutionChain mappedHandler, @Nullable ModelAndView mv,
 1024 | 			@Nullable Exception exception) throws Exception {
 1025 | 
 1026 | 		boolean errorView = false;
 1027 | 
 1028 | 		if (exception != null) {
 1029 | 			if (exception instanceof ModelAndViewDefiningException mavDefiningException) {
 1030 | 				logger.debug("ModelAndViewDefiningException encountered", exception);
 1031 | 				mv = mavDefiningException.getModelAndView();
 1032 | 			}
 1033 | 			else {
 1034 | 				Object handler = (mappedHandler != null ? mappedHandler.getHandler() : null);
 1035 | 				mv = processHandlerException(request, response, handler, exception);
 1036 | 				errorView = (mv != null);
 1037 | 			}
 1038 | 		}
 1039 | 
 1040 | 		// Did the handler return a view to render?
 1041 | 		if (mv != null && !mv.wasCleared()) {
 1042 | 			render(mv, request, response);
 1043 | 			if (errorView) {
 1044 | 				WebUtils.clearErrorRequestAttributes(request);
 1045 | 			}
 1046 | 		}
 1047 | 		else {
 1048 | 			if (logger.isTraceEnabled()) {
 1049 | 				logger.trace("No view rendering, null ModelAndView returned.");
 1050 | 			}
 1051 | 		}
 1052 | 
 1053 | 		if (WebAsyncUtils.getAsyncManager(request).isConcurrentHandlingStarted()) {
 1054 | 			// Concurrent handling started during a forward
 1055 | 			return;
 1056 | 		}
 1057 | 
 1058 | 		if (mappedHandler != null) {
 1059 | 			// Exception (if any) is already handled..
 1060 | 			mappedHandler.triggerAfterCompletion(request, response, null);
 1061 | 		}
 1062 | 	}
 1063 | 
 1064 | 	/**
 1065 | 	 * Build a LocaleContext for the given request, exposing the request's primary locale as current locale.
 1066 | 	 * <p>The default implementation uses the dispatcher's LocaleResolver to obtain the current locale,
 1067 | 	 * which might change during a request.
 1068 | 	 * @param request current HTTP request
 1069 | 	 * @return the corresponding LocaleContext
 1070 | 	 */
 1071 | 	@Override
 1072 | 	protected LocaleContext buildLocaleContext(final HttpServletRequest request) {
 1073 | 		LocaleResolver lr = this.localeResolver;
 1074 | 		if (lr instanceof LocaleContextResolver localeContextResolver) {
 1075 | 			return localeContextResolver.resolveLocaleContext(request);
 1076 | 		}
 1077 | 		else {
 1078 | 			return () -> (lr != null ? lr.resolveLocale(request) : request.getLocale());
 1079 | 		}
 1080 | 	}
 1081 | 
 1082 | 	/**
 1083 | 	 * Convert the request into a multipart request, and make multipart resolver available.
 1084 | 	 * <p>If no multipart resolver is set, simply use the existing request.
 1085 | 	 * @param request current HTTP request
 1086 | 	 * @return the processed request (multipart wrapper if necessary)
 1087 | 	 * @see MultipartResolver#resolveMultipart
 1088 | 	 */
 1089 | 	protected HttpServletRequest checkMultipart(HttpServletRequest request) throws MultipartException {
 1090 | 		if (this.multipartResolver != null && this.multipartResolver.isMultipart(request)) {
 1091 | 			if (WebUtils.getNativeRequest(request, MultipartHttpServletRequest.class) != null) {
 1092 | 				if (DispatcherType.REQUEST.equals(request.getDispatcherType())) {
 1093 | 					logger.trace("Request already resolved to MultipartHttpServletRequest, for example, by MultipartFilter");
 1094 | 				}
 1095 | 			}
 1096 | 			else if (hasMultipartException(request)) {
 1097 | 				logger.debug("Multipart resolution previously failed for current request - " +
 1098 | 						"skipping re-resolution for undisturbed error rendering");
 1099 | 			}
 1100 | 			else {
 1101 | 				try {
 1102 | 					return this.multipartResolver.resolveMultipart(request);
 1103 | 				}
 1104 | 				catch (MultipartException ex) {
 1105 | 					if (request.getAttribute(WebUtils.ERROR_EXCEPTION_ATTRIBUTE) != null) {
 1106 | 						logger.debug("Multipart resolution failed for error dispatch", ex);
 1107 | 						// Keep processing error dispatch with regular request handle below
 1108 | 					}
 1109 | 					else {
 1110 | 						throw ex;
 1111 | 					}
 1112 | 				}
 1113 | 			}
 1114 | 		}
 1115 | 		// If not returned before: return original request.
 1116 | 		return request;
 1117 | 	}
 1118 | 
 1119 | 	/**
 1120 | 	 * Check "jakarta.servlet.error.exception" attribute for a multipart exception.
 1121 | 	 */
 1122 | 	private static boolean hasMultipartException(HttpServletRequest request) {
 1123 | 		Throwable error = (Throwable) request.getAttribute(WebUtils.ERROR_EXCEPTION_ATTRIBUTE);
 1124 | 		while (error != null) {
 1125 | 			if (error instanceof MultipartException) {
 1126 | 				return true;
 1127 | 			}
 1128 | 			error = error.getCause();
 1129 | 		}
 1130 | 		return false;
 1131 | 	}
 1132 | 
 1133 | 	/**
 1134 | 	 * Clean up any resources used by the given multipart request (if any).
 1135 | 	 * @param request current HTTP request
 1136 | 	 * @see MultipartResolver#cleanupMultipart
 1137 | 	 */
 1138 | 	protected void cleanupMultipart(HttpServletRequest request) {
 1139 | 		if (this.multipartResolver != null) {
 1140 | 			MultipartHttpServletRequest multipartRequest =
 1141 | 					WebUtils.getNativeRequest(request, MultipartHttpServletRequest.class);
 1142 | 			if (multipartRequest != null) {
 1143 | 				this.multipartResolver.cleanupMultipart(multipartRequest);
 1144 | 			}
 1145 | 		}
 1146 | 	}
 1147 | 
 1148 | 	/**
 1149 | 	 * Return the HandlerExecutionChain for this request.
 1150 | 	 * <p>Tries all handler mappings in order.
 1151 | 	 * @param request current HTTP request
 1152 | 	 * @return the HandlerExecutionChain, or {@code null} if no handler could be found
 1153 | 	 */
 1154 | 	protected @Nullable HandlerExecutionChain getHandler(HttpServletRequest request) throws Exception {
 1155 | 		if (this.handlerMappings != null) {
 1156 | 			for (HandlerMapping mapping : this.handlerMappings) {
 1157 | 				HandlerExecutionChain handler = mapping.getHandler(request);
 1158 | 				if (handler != null) {
 1159 | 					return handler;
 1160 | 				}
 1161 | 			}
 1162 | 		}
 1163 | 		return null;
 1164 | 	}
 1165 | 
 1166 | 	/**
 1167 | 	 * No handler found &rarr; set appropriate HTTP response status.
 1168 | 	 * @param request current HTTP request
 1169 | 	 * @param response current HTTP response
 1170 | 	 * @throws Exception if preparing the response failed
 1171 | 	 */
 1172 | 	protected void noHandlerFound(HttpServletRequest request, HttpServletResponse response) throws Exception {
 1173 | 		if (pageNotFoundLogger.isWarnEnabled()) {
 1174 | 			pageNotFoundLogger.warn("No mapping for " + request.getMethod() + " " + getRequestUri(request));
 1175 | 		}
 1176 | 		throw new NoHandlerFoundException(request.getMethod(), getRequestUri(request),
 1177 | 				new ServletServerHttpRequest(request).getHeaders());
 1178 | 	}
 1179 | 
 1180 | 	/**
 1181 | 	 * Return the HandlerAdapter for this handler object.
 1182 | 	 * @param handler the handler object to find an adapter for
 1183 | 	 * @throws ServletException if no HandlerAdapter can be found for the handler. This is a fatal error.
 1184 | 	 */
 1185 | 	protected HandlerAdapter getHandlerAdapter(Object handler) throws ServletException {
 1186 | 		if (this.handlerAdapters != null) {
 1187 | 			for (HandlerAdapter adapter : this.handlerAdapters) {
 1188 | 				if (adapter.supports(handler)) {
 1189 | 					return adapter;
 1190 | 				}
 1191 | 			}
 1192 | 		}
 1193 | 		throw new ServletException("No adapter for handler [" + handler +
 1194 | 				"]: The DispatcherServlet configuration needs to include a HandlerAdapter that supports this handler");
 1195 | 	}
 1196 | 
 1197 | 	/**
 1198 | 	 * Determine an error ModelAndView via the registered HandlerExceptionResolvers.
 1199 | 	 * @param request current HTTP request
 1200 | 	 * @param response current HTTP response
 1201 | 	 * @param handler the executed handler, or {@code null} if none chosen at the time of the exception
 1202 | 	 * (for example, if multipart resolution failed)
 1203 | 	 * @param ex the exception that got thrown during handler execution
 1204 | 	 * @return a corresponding ModelAndView to forward to
 1205 | 	 * @throws Exception if no error ModelAndView found
 1206 | 	 */
 1207 | 	protected @Nullable ModelAndView processHandlerException(HttpServletRequest request, HttpServletResponse response,
 1208 | 			@Nullable Object handler, Exception ex) throws Exception {
 1209 | 
 1210 | 		// Success and error responses may use different content types
 1211 | 		request.removeAttribute(HandlerMapping.PRODUCIBLE_MEDIA_TYPES_ATTRIBUTE);
 1212 | 		// Reset the response content-type header and body buffer if the response is not committed already,
 1213 | 		// leaving the other response headers in place.
 1214 | 		try {
 1215 | 			response.setHeader(HttpHeaders.CONTENT_TYPE, null);
 1216 | 			response.setHeader(HttpHeaders.CONTENT_DISPOSITION, null);
 1217 | 			response.resetBuffer();
 1218 | 		}
 1219 | 		catch (IllegalStateException illegalStateException) {
 1220 | 			// the response is already committed, leave it to exception handlers anyway
 1221 | 		}
 1222 | 
 1223 | 		// Check registered HandlerExceptionResolvers...
 1224 | 		ModelAndView exMv = null;
 1225 | 		if (this.handlerExceptionResolvers != null) {
 1226 | 			for (HandlerExceptionResolver resolver : this.handlerExceptionResolvers) {
 1227 | 				exMv = resolver.resolveException(request, response, handler, ex);
 1228 | 				if (exMv != null) {
 1229 | 					break;
 1230 | 				}
 1231 | 			}
 1232 | 		}
 1233 | 		if (exMv != null) {
 1234 | 			if (exMv.isEmpty()) {
 1235 | 				request.setAttribute(EXCEPTION_ATTRIBUTE, ex);
 1236 | 				return null;
 1237 | 			}
 1238 | 			// We might still need view name translation for a plain error model...
 1239 | 			if (!exMv.hasView()) {
 1240 | 				String defaultViewName = getDefaultViewName(request);
 1241 | 				if (defaultViewName != null) {
 1242 | 					exMv.setViewName(defaultViewName);
 1243 | 				}
 1244 | 			}
 1245 | 			if (logger.isTraceEnabled()) {
 1246 | 				logger.trace("Using resolved error view: " + exMv, ex);
 1247 | 			}
 1248 | 			else if (logger.isDebugEnabled()) {
 1249 | 				logger.debug("Using resolved error view: " + exMv);
 1250 | 			}
 1251 | 			WebUtils.exposeErrorRequestAttributes(request, ex, getServletName());
 1252 | 			return exMv;
 1253 | 		}
 1254 | 
 1255 | 		throw ex;
 1256 | 	}
 1257 | 
 1258 | 	/**
 1259 | 	 * Render the given ModelAndView.
 1260 | 	 * <p>This is the last stage in handling a request. It may involve resolving the view by name.
 1261 | 	 * @param mv the ModelAndView to render
 1262 | 	 * @param request current HTTP servlet request
 1263 | 	 * @param response current HTTP servlet response
 1264 | 	 * @throws ServletException if view is missing or cannot be resolved
 1265 | 	 * @throws Exception if there's a problem rendering the view
 1266 | 	 */
 1267 | 	protected void render(ModelAndView mv, HttpServletRequest request, HttpServletResponse response) throws Exception {
 1268 | 		// Determine locale for request and apply it to the response.
 1269 | 		Locale locale =
 1270 | 				(this.localeResolver != null ? this.localeResolver.resolveLocale(request) : request.getLocale());
 1271 | 		response.setLocale(locale);
 1272 | 
 1273 | 		View view;
 1274 | 		String viewName = mv.getViewName();
 1275 | 		if (viewName != null) {
 1276 | 			// We need to resolve the view name.
 1277 | 			view = resolveViewName(viewName, mv.getModelInternal(), locale, request);
 1278 | 			if (view == null) {
 1279 | 				throw new ServletException("Could not resolve view with name '" + mv.getViewName() +
 1280 | 						"' in servlet with name '" + getServletName() + "'");
 1281 | 			}
 1282 | 		}
 1283 | 		else {
 1284 | 			// No need to lookup: the ModelAndView object contains the actual View object.
 1285 | 			view = mv.getView();
 1286 | 			if (view == null) {
 1287 | 				throw new ServletException("ModelAndView [" + mv + "] neither contains a view name nor a " +
 1288 | 						"View object in servlet with name '" + getServletName() + "'");
 1289 | 			}
 1290 | 		}
 1291 | 
 1292 | 		if (view instanceof SmartView smartView) {
 1293 | 			smartView.resolveNestedViews(this::resolveViewNameInternal, locale);
 1294 | 		}
 1295 | 
 1296 | 		// Delegate to the View object for rendering.
 1297 | 		if (logger.isTraceEnabled()) {
 1298 | 			logger.trace("Rendering view [" + view + "] ");
 1299 | 		}
 1300 | 		try {
 1301 | 			if (mv.getStatus() != null) {
 1302 | 				request.setAttribute(View.RESPONSE_STATUS_ATTRIBUTE, mv.getStatus());
 1303 | 				response.setStatus(mv.getStatus().value());
 1304 | 			}
 1305 | 			view.render(mv.getModelInternal(), request, response);
 1306 | 		}
 1307 | 		catch (Exception ex) {
 1308 | 			if (logger.isDebugEnabled()) {
 1309 | 				logger.debug("Error rendering view [" + view + "]", ex);
 1310 | 			}
 1311 | 			throw ex;
 1312 | 		}
 1313 | 	}
 1314 | 
 1315 | 	/**
 1316 | 	 * Translate the supplied request into a default view name.
 1317 | 	 * @param request current HTTP servlet request
 1318 | 	 * @return the view name (or {@code null} if no default found)
 1319 | 	 * @throws Exception if view name translation failed
 1320 | 	 */
 1321 | 	protected @Nullable String getDefaultViewName(HttpServletRequest request) throws Exception {
 1322 | 		return (this.viewNameTranslator != null ? this.viewNameTranslator.getViewName(request) : null);
 1323 | 	}
 1324 | 
 1325 | 	/**
 1326 | 	 * Resolve the given view name into a View object (to be rendered).
 1327 | 	 * <p>The default implementations asks all ViewResolvers of this dispatcher.
 1328 | 	 * Can be overridden for custom resolution strategies, potentially based on
 1329 | 	 * specific model attributes or request parameters.
 1330 | 	 * @param viewName the name of the view to resolve
 1331 | 	 * @param model the model to be passed to the view
 1332 | 	 * @param locale the current locale
 1333 | 	 * @param request current HTTP servlet request
 1334 | 	 * @return the View object, or {@code null} if none found
 1335 | 	 * @throws Exception if the view cannot be resolved
 1336 | 	 * (typically in case of problems creating an actual View object)
 1337 | 	 * @see ViewResolver#resolveViewName
 1338 | 	 */
 1339 | 	protected @Nullable View resolveViewName(String viewName, @Nullable Map<String, Object> model,
 1340 | 			Locale locale, HttpServletRequest request) throws Exception {
 1341 | 
 1342 | 		return resolveViewNameInternal(viewName, locale);
 1343 | 	}
 1344 | 
 1345 | 	private @Nullable View resolveViewNameInternal(String viewName, Locale locale) throws Exception {
 1346 | 		if (this.viewResolvers != null) {
 1347 | 			for (ViewResolver viewResolver : this.viewResolvers) {
 1348 | 				View view = viewResolver.resolveViewName(viewName, locale);
 1349 | 				if (view != null) {
 1350 | 					return view;
 1351 | 				}
 1352 | 			}
 1353 | 		}
 1354 | 		return null;
 1355 | 	}
 1356 | 
 1357 | 	private static void triggerAfterCompletion(HttpServletRequest request, HttpServletResponse response,
 1358 | 			@Nullable HandlerExecutionChain mappedHandler, Exception ex) throws Exception {
 1359 | 
 1360 | 		if (mappedHandler != null) {
 1361 | 			mappedHandler.triggerAfterCompletion(request, response, ex);
 1362 | 		}
 1363 | 		throw ex;
 1364 | 	}
 1365 | 
 1366 | 	/**
 1367 | 	 * Restore the request attributes after an include.
 1368 | 	 * @param request current HTTP request
 1369 | 	 * @param attributesSnapshot the snapshot of the request attributes before the include
 1370 | 	 */
 1371 | 	private void restoreAttributesAfterInclude(HttpServletRequest request, Map<String, Object> attributesSnapshot) {
 1372 | 		// Need to copy into separate Collection here, to avoid side effects
 1373 | 		// on the Enumeration when removing attributes.
 1374 | 		Set<String> attrsToCheck = new HashSet<>();
 1375 | 		Enumeration<?> attrNames = request.getAttributeNames();
 1376 | 		while (attrNames.hasMoreElements()) {
 1377 | 			String attrName = (String) attrNames.nextElement();
 1378 | 			if (this.cleanupAfterInclude || attrName.startsWith(DEFAULT_STRATEGIES_PREFIX)) {
 1379 | 				attrsToCheck.add(attrName);
 1380 | 			}
 1381 | 		}
 1382 | 
 1383 | 		// Add attributes that may have been removed
 1384 | 		attrsToCheck.addAll(attributesSnapshot.keySet());
 1385 | 
 1386 | 		// Iterate over the attributes to check, restoring the original value
 1387 | 		// or removing the attribute, respectively, if appropriate.
 1388 | 		for (String attrName : attrsToCheck) {
 1389 | 			Object attrValue = attributesSnapshot.get(attrName);
 1390 | 			if (attrValue == null) {
 1391 | 				request.removeAttribute(attrName);
 1392 | 			}
 1393 | 			else if (attrValue != request.getAttribute(attrName)) {
 1394 | 				request.setAttribute(attrName, attrValue);
 1395 | 			}
 1396 | 		}
 1397 | 	}
 1398 | 
 1399 | 	private static String getRequestUri(HttpServletRequest request) {
 1400 | 		String uri = (String) request.getAttribute(WebUtils.INCLUDE_REQUEST_URI_ATTRIBUTE);
 1401 | 		if (uri == null) {
 1402 | 			uri = request.getRequestURI();
 1403 | 		}
 1404 | 		return uri;
 1405 | 	}
 1406 | 
 1407 | }
```
