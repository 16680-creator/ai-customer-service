<!-- learning-docs source archive（机械抓取原文，禁止手改代码内容） -->
<!-- origin: org.springframework:spring-webmvc:7.1.0 | path: org/springframework/web/servlet/FrameworkServlet.java -->
<!-- 正文引用：[[源码:FrameworkServlet.java#L起-L止]] ；代码块头部：(src: FrameworkServlet.java#L起-L止) -->

# FrameworkServlet.java

| 项 | 值 |
| --- | --- |
| 来源 | `org.springframework:spring-webmvc:7.1.0` |
| 路径 | `org/springframework/web/servlet/FrameworkServlet.java` |
| 行数 | 1212 |
| 在线地址 | https://repo1.maven.org/maven2/org/springframework/spring-webmvc/7.1.0/spring-webmvc-7.1.0-sources.jar |
| 抓取时间 | 2026-11-25T14:01:08.556Z |

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
   20 | import java.security.Principal;
   21 | import java.util.ArrayList;
   22 | import java.util.Collection;
   23 | import java.util.List;
   24 | import java.util.Set;
   25 | import java.util.concurrent.Callable;
   26 | import java.util.stream.Collectors;
   27 | 
   28 | import jakarta.servlet.DispatcherType;
   29 | import jakarta.servlet.ServletContext;
   30 | import jakarta.servlet.ServletException;
   31 | import jakarta.servlet.http.HttpServletRequest;
   32 | import jakarta.servlet.http.HttpServletResponse;
   33 | import org.jspecify.annotations.Nullable;
   34 | 
   35 | import org.springframework.beans.BeanUtils;
   36 | import org.springframework.context.ApplicationContext;
   37 | import org.springframework.context.ApplicationContextAware;
   38 | import org.springframework.context.ApplicationContextException;
   39 | import org.springframework.context.ApplicationContextInitializer;
   40 | import org.springframework.context.ApplicationListener;
   41 | import org.springframework.context.ConfigurableApplicationContext;
   42 | import org.springframework.context.event.ContextRefreshedEvent;
   43 | import org.springframework.context.event.SourceFilteringListener;
   44 | import org.springframework.context.i18n.LocaleContext;
   45 | import org.springframework.context.i18n.LocaleContextHolder;
   46 | import org.springframework.context.i18n.SimpleLocaleContext;
   47 | import org.springframework.core.GenericTypeResolver;
   48 | import org.springframework.core.annotation.AnnotationAwareOrderComparator;
   49 | import org.springframework.core.env.ConfigurableEnvironment;
   50 | import org.springframework.http.HttpHeaders;
   51 | import org.springframework.http.HttpStatusCode;
   52 | import org.springframework.util.ClassUtils;
   53 | import org.springframework.util.ObjectUtils;
   54 | import org.springframework.util.StringUtils;
   55 | import org.springframework.web.context.ConfigurableWebApplicationContext;
   56 | import org.springframework.web.context.ConfigurableWebEnvironment;
   57 | import org.springframework.web.context.ContextLoader;
   58 | import org.springframework.web.context.WebApplicationContext;
   59 | import org.springframework.web.context.request.NativeWebRequest;
   60 | import org.springframework.web.context.request.RequestAttributes;
   61 | import org.springframework.web.context.request.RequestContextHolder;
   62 | import org.springframework.web.context.request.ServletRequestAttributes;
   63 | import org.springframework.web.context.request.async.CallableProcessingInterceptor;
   64 | import org.springframework.web.context.request.async.WebAsyncManager;
   65 | import org.springframework.web.context.request.async.WebAsyncUtils;
   66 | import org.springframework.web.context.support.ServletRequestHandledEvent;
   67 | import org.springframework.web.context.support.WebApplicationContextUtils;
   68 | import org.springframework.web.context.support.XmlWebApplicationContext;
   69 | import org.springframework.web.cors.CorsUtils;
   70 | import org.springframework.web.util.WebUtils;
   71 | 
   72 | /**
   73 |  * Base servlet for Spring's web framework. Provides integration with
   74 |  * a Spring application context, in a JavaBean-based overall solution.
   75 |  *
   76 |  * <p>This class offers the following functionality:
   77 |  * <ul>
   78 |  * <li>Manages a {@link org.springframework.web.context.WebApplicationContext
   79 |  * WebApplicationContext} instance per servlet. The servlet's configuration is determined
   80 |  * by beans in the servlet's namespace.
   81 |  * <li>Publishes events on request processing, whether or not a request is
   82 |  * successfully handled.
   83 |  * </ul>
   84 |  *
   85 |  * <p>Subclasses must implement {@link #doService} to handle requests. Because this extends
   86 |  * {@link HttpServletBean} rather than HttpServlet directly, bean properties are
   87 |  * automatically mapped onto it. Subclasses can override {@link #initFrameworkServlet()}
   88 |  * for custom initialization.
   89 |  *
   90 |  * <p>Detects a "contextClass" parameter at the servlet init-param level,
   91 |  * falling back to the default context class,
   92 |  * {@link org.springframework.web.context.support.XmlWebApplicationContext
   93 |  * XmlWebApplicationContext}, if not found. Note that, with the default
   94 |  * {@code FrameworkServlet}, a custom context class needs to implement the
   95 |  * {@link org.springframework.web.context.ConfigurableWebApplicationContext
   96 |  * ConfigurableWebApplicationContext} SPI.
   97 |  *
   98 |  * <p>Accepts an optional "contextInitializerClasses" servlet init-param that
   99 |  * specifies one or more {@link org.springframework.context.ApplicationContextInitializer
  100 |  * ApplicationContextInitializer} classes. The managed web application context will be
  101 |  * delegated to these initializers, allowing for additional programmatic configuration,
  102 |  * for example, adding property sources or activating profiles against the {@linkplain
  103 |  * org.springframework.context.ConfigurableApplicationContext#getEnvironment() context's
  104 |  * environment}. See also {@link org.springframework.web.context.ContextLoader} which
  105 |  * supports a "contextInitializerClasses" context-param with identical semantics for
  106 |  * the "root" web application context.
  107 |  *
  108 |  * <p>Passes a "contextConfigLocation" servlet init-param to the context instance,
  109 |  * parsing it into potentially multiple file paths which can be separated by any
  110 |  * number of commas and spaces, like "test-servlet.xml, myServlet.xml".
  111 |  * If not explicitly specified, the context implementation is supposed to build a
  112 |  * default location from the namespace of the servlet.
  113 |  *
  114 |  * <p>Note: In case of multiple config locations, later bean definitions will
  115 |  * override ones defined in earlier loaded files, at least when using Spring's
  116 |  * default ApplicationContext implementation. This can be leveraged to
  117 |  * deliberately override certain bean definitions via an extra XML file.
  118 |  *
  119 |  * <p>The default namespace is "'servlet-name'-servlet", for example, "test-servlet" for a
  120 |  * servlet-name "test" (leading to a "/WEB-INF/test-servlet.xml" default location
  121 |  * with XmlWebApplicationContext). The namespace can also be set explicitly via
  122 |  * the "namespace" servlet init-param.
  123 |  *
  124 |  * <p>{@code FrameworkServlet} may be injected with a web application context,
  125 |  * rather than creating its own internally. This is useful in Servlet 3.0+
  126 |  * environments, which support programmatic registration of servlet instances. See
  127 |  * {@link #FrameworkServlet(WebApplicationContext)} Javadoc for details.
  128 |  *
  129 |  * @author Rod Johnson
  130 |  * @author Juergen Hoeller
  131 |  * @author Sam Brannen
  132 |  * @author Chris Beams
  133 |  * @author Rossen Stoyanchev
  134 |  * @author Phillip Webb
  135 |  * @see #doService
  136 |  * @see #setContextClass
  137 |  * @see #setContextConfigLocation
  138 |  * @see #setContextInitializerClasses
  139 |  * @see #setNamespace
  140 |  */
  141 | @SuppressWarnings("serial")
  142 | public abstract class FrameworkServlet extends HttpServletBean implements ApplicationContextAware {
  143 | 
  144 | 	/**
  145 | 	 * Suffix for WebApplicationContext namespaces. If a servlet of this class is
  146 | 	 * given the name "test" in a context, the namespace used by the servlet will
  147 | 	 * resolve to "test-servlet".
  148 | 	 */
  149 | 	public static final String DEFAULT_NAMESPACE_SUFFIX = "-servlet";
  150 | 
  151 | 	/**
  152 | 	 * Default context class for FrameworkServlet.
  153 | 	 * @see org.springframework.web.context.support.XmlWebApplicationContext
  154 | 	 */
  155 | 	public static final Class<?> DEFAULT_CONTEXT_CLASS = XmlWebApplicationContext.class;
  156 | 
  157 | 	/**
  158 | 	 * Prefix for the ServletContext attribute for the WebApplicationContext.
  159 | 	 * The completion is the servlet name.
  160 | 	 */
  161 | 	public static final String SERVLET_CONTEXT_PREFIX = FrameworkServlet.class.getName() + ".CONTEXT.";
  162 | 
  163 | 	/**
  164 | 	 * Any number of these characters are considered delimiters between
  165 | 	 * multiple values in a single init-param String value.
  166 | 	 */
  167 | 	private static final String INIT_PARAM_DELIMITERS = ",; \t\n";
  168 | 
  169 | 	/**
  170 | 	 * HTTP methods supported by {@link jakarta.servlet.http.HttpServlet}.
  171 | 	 */
  172 | 	private static final Set<String> HTTP_SERVLET_METHODS =
  173 | 			Set.of("DELETE", "HEAD", "GET", "OPTIONS", "PATCH", "POST", "PUT", "TRACE");
  174 | 
  175 | 
  176 | 	/** ServletContext attribute to find the WebApplicationContext in. */
  177 | 	private @Nullable String contextAttribute;
  178 | 
  179 | 	/** WebApplicationContext implementation class to create. */
  180 | 	private Class<?> contextClass = DEFAULT_CONTEXT_CLASS;
  181 | 
  182 | 	/** WebApplicationContext ID to assign. */
  183 | 	private @Nullable String contextId;
  184 | 
  185 | 	/** Namespace for this servlet. */
  186 | 	private @Nullable String namespace;
  187 | 
  188 | 	/** Explicit context config location. */
  189 | 	private @Nullable String contextConfigLocation;
  190 | 
  191 | 	/** Actual ApplicationContextInitializer instances to apply to the context. */
  192 | 	private final List<ApplicationContextInitializer<ConfigurableApplicationContext>> contextInitializers =
  193 | 			new ArrayList<>();
  194 | 
  195 | 	/** Comma-delimited ApplicationContextInitializer class names set through init param. */
  196 | 	private @Nullable String contextInitializerClasses;
  197 | 
  198 | 	/** Whether to publish the context as a ServletContext attribute. */
  199 | 	private boolean publishContext = true;
  200 | 
  201 | 	/** Whether to publish a ServletRequestHandledEvent at the end of each request. */
  202 | 	private boolean publishEvents = true;
  203 | 
  204 | 	/** Whether to expose LocaleContext and RequestAttributes as inheritable for child threads. */
  205 | 	private boolean threadContextInheritable = false;
  206 | 
  207 | 	/** Whether to dispatch an HTTP OPTIONS request to {@link #doService}. */
  208 | 	private boolean dispatchOptionsRequest = false;
  209 | 
  210 | 	/** Whether to dispatch an HTTP TRACE request to {@link #doService}. */
  211 | 	private boolean dispatchTraceRequest = false;
  212 | 
  213 | 	/** Whether to log potentially sensitive info (request params at DEBUG and headers at TRACE). */
  214 | 	private boolean enableLoggingRequestDetails = false;
  215 | 
  216 | 	/** WebApplicationContext for this servlet. */
  217 | 	private @Nullable WebApplicationContext webApplicationContext;
  218 | 
  219 | 	/** Whether the WebApplicationContext was injected via {@link #setApplicationContext}. */
  220 | 	private boolean webApplicationContextInjected = false;
  221 | 
  222 | 	/** Flag used to detect whether onRefresh has already been called. */
  223 | 	private volatile boolean refreshEventReceived;
  224 | 
  225 | 	/** Monitor for synchronized onRefresh execution. */
  226 | 	private final Object onRefreshMonitor = new Object();
  227 | 
  228 | 
  229 | 	/**
  230 | 	 * Create a new {@code FrameworkServlet} that will create its own internal web
  231 | 	 * application context based on defaults and values provided through servlet
  232 | 	 * init-params. Typically used in Servlet 2.5 or earlier environments, where the only
  233 | 	 * option for servlet registration is through {@code web.xml} which requires the use
  234 | 	 * of a no-arg constructor.
  235 | 	 * <p>Calling {@link #setContextConfigLocation} (init-param 'contextConfigLocation')
  236 | 	 * will dictate which XML files will be loaded by the
  237 | 	 * {@linkplain #DEFAULT_CONTEXT_CLASS default XmlWebApplicationContext}
  238 | 	 * <p>Calling {@link #setContextClass} (init-param 'contextClass') overrides the
  239 | 	 * default {@code XmlWebApplicationContext} and allows for specifying an alternative class,
  240 | 	 * such as {@code AnnotationConfigWebApplicationContext}.
  241 | 	 * <p>Calling {@link #setContextInitializerClasses} (init-param 'contextInitializerClasses')
  242 | 	 * indicates which {@link ApplicationContextInitializer} classes should be used to
  243 | 	 * further configure the internal application context prior to refresh.
  244 | 	 * @see #FrameworkServlet(WebApplicationContext)
  245 | 	 */
  246 | 	public FrameworkServlet() {
  247 | 	}
  248 | 
  249 | 	/**
  250 | 	 * Create a new {@code FrameworkServlet} with the given web application context. This
  251 | 	 * constructor is useful in Servlet environments where instance-based registration
  252 | 	 * of servlets is possible through the {@link ServletContext#addServlet} API.
  253 | 	 * <p>Using this constructor indicates that the following properties / init-params
  254 | 	 * will be ignored:
  255 | 	 * <ul>
  256 | 	 * <li>{@link #setContextClass(Class)} / 'contextClass'</li>
  257 | 	 * <li>{@link #setContextConfigLocation(String)} / 'contextConfigLocation'</li>
  258 | 	 * <li>{@link #setContextAttribute(String)} / 'contextAttribute'</li>
  259 | 	 * <li>{@link #setNamespace(String)} / 'namespace'</li>
  260 | 	 * </ul>
  261 | 	 * <p>The given web application context may or may not yet be {@linkplain
  262 | 	 * ConfigurableApplicationContext#refresh() refreshed}. If it (a) is an implementation
  263 | 	 * of {@link ConfigurableWebApplicationContext} and (b) has <strong>not</strong>
  264 | 	 * already been refreshed (the recommended approach), then the following will occur:
  265 | 	 * <ul>
  266 | 	 * <li>If the given context does not already have a {@linkplain
  267 | 	 * ConfigurableApplicationContext#setParent parent}, the root application context
  268 | 	 * will be set as the parent.</li>
  269 | 	 * <li>If the given context has not already been assigned an {@linkplain
  270 | 	 * ConfigurableApplicationContext#setId ID}, one will be assigned to it</li>
  271 | 	 * <li>{@code ServletContext} and {@code ServletConfig} objects will be delegated to
  272 | 	 * the application context</li>
  273 | 	 * <li>{@link #postProcessWebApplicationContext} will be called</li>
  274 | 	 * <li>Any {@link ApplicationContextInitializer ApplicationContextInitializers} specified through the
  275 | 	 * "contextInitializerClasses" init-param or through the {@link
  276 | 	 * #setContextInitializers} property will be applied.</li>
  277 | 	 * <li>{@link ConfigurableApplicationContext#refresh refresh()} will be called</li>
  278 | 	 * </ul>
  279 | 	 * If the context has already been refreshed or does not implement
  280 | 	 * {@code ConfigurableWebApplicationContext}, none of the above will occur under the
  281 | 	 * assumption that the user has performed these actions (or not) per his or her
  282 | 	 * specific needs.
  283 | 	 * <p>See {@link org.springframework.web.WebApplicationInitializer} for usage examples.
  284 | 	 * @param webApplicationContext the context to use
  285 | 	 * @see #initWebApplicationContext
  286 | 	 * @see #configureAndRefreshWebApplicationContext
  287 | 	 * @see org.springframework.web.WebApplicationInitializer
  288 | 	 */
  289 | 	public FrameworkServlet(WebApplicationContext webApplicationContext) {
  290 | 		this.webApplicationContext = webApplicationContext;
  291 | 	}
  292 | 
  293 | 
  294 | 	/**
  295 | 	 * Set the name of the ServletContext attribute which should be used to retrieve the
  296 | 	 * {@link WebApplicationContext} that this servlet is supposed to use.
  297 | 	 */
  298 | 	public void setContextAttribute(@Nullable String contextAttribute) {
  299 | 		this.contextAttribute = contextAttribute;
  300 | 	}
  301 | 
  302 | 	/**
  303 | 	 * Return the name of the ServletContext attribute which should be used to retrieve the
  304 | 	 * {@link WebApplicationContext} that this servlet is supposed to use.
  305 | 	 */
  306 | 	public @Nullable String getContextAttribute() {
  307 | 		return this.contextAttribute;
  308 | 	}
  309 | 
  310 | 	/**
  311 | 	 * Set a custom context class. This class must be of type
  312 | 	 * {@link org.springframework.web.context.WebApplicationContext}.
  313 | 	 * <p>When using the default FrameworkServlet implementation,
  314 | 	 * the context class must also implement the
  315 | 	 * {@link org.springframework.web.context.ConfigurableWebApplicationContext}
  316 | 	 * interface.
  317 | 	 * @see #createWebApplicationContext
  318 | 	 */
  319 | 	public void setContextClass(Class<?> contextClass) {
  320 | 		this.contextClass = contextClass;
  321 | 	}
  322 | 
  323 | 	/**
  324 | 	 * Return the custom context class.
  325 | 	 */
  326 | 	public Class<?> getContextClass() {
  327 | 		return this.contextClass;
  328 | 	}
  329 | 
  330 | 	/**
  331 | 	 * Specify a custom WebApplicationContext ID,
  332 | 	 * to be used as serialization ID for the underlying BeanFactory.
  333 | 	 */
  334 | 	public void setContextId(@Nullable String contextId) {
  335 | 		this.contextId = contextId;
  336 | 	}
  337 | 
  338 | 	/**
  339 | 	 * Return the custom WebApplicationContext ID, if any.
  340 | 	 */
  341 | 	public @Nullable String getContextId() {
  342 | 		return this.contextId;
  343 | 	}
  344 | 
  345 | 	/**
  346 | 	 * Set a custom namespace for this servlet,
  347 | 	 * to be used for building a default context config location.
  348 | 	 */
  349 | 	public void setNamespace(String namespace) {
  350 | 		this.namespace = namespace;
  351 | 	}
  352 | 
  353 | 	/**
  354 | 	 * Return the namespace for this servlet, falling back to default scheme if
  355 | 	 * no custom namespace was set: for example, "test-servlet" for a servlet named "test".
  356 | 	 */
  357 | 	public String getNamespace() {
  358 | 		return (this.namespace != null ? this.namespace : getServletName() + DEFAULT_NAMESPACE_SUFFIX);
  359 | 	}
  360 | 
  361 | 	/**
  362 | 	 * Set the context config location explicitly, instead of relying on the default
  363 | 	 * location built from the namespace. This location string can consist of
  364 | 	 * multiple locations separated by any number of commas and spaces.
  365 | 	 */
  366 | 	public void setContextConfigLocation(@Nullable String contextConfigLocation) {
  367 | 		this.contextConfigLocation = contextConfigLocation;
  368 | 	}
  369 | 
  370 | 	/**
  371 | 	 * Return the explicit context config location, if any.
  372 | 	 */
  373 | 	public @Nullable String getContextConfigLocation() {
  374 | 		return this.contextConfigLocation;
  375 | 	}
  376 | 
  377 | 	/**
  378 | 	 * Specify which {@link ApplicationContextInitializer} instances should be used
  379 | 	 * to initialize the application context used by this {@code FrameworkServlet}.
  380 | 	 * @see #configureAndRefreshWebApplicationContext
  381 | 	 * @see #applyInitializers
  382 | 	 */
  383 | 	@SuppressWarnings("unchecked")
  384 | 	public void setContextInitializers(ApplicationContextInitializer<?> @Nullable ... initializers) {
  385 | 		if (initializers != null) {
  386 | 			for (ApplicationContextInitializer<?> initializer : initializers) {
  387 | 				this.contextInitializers.add((ApplicationContextInitializer<ConfigurableApplicationContext>) initializer);
  388 | 			}
  389 | 		}
  390 | 	}
  391 | 
  392 | 	/**
  393 | 	 * Specify the set of fully-qualified {@link ApplicationContextInitializer} class
  394 | 	 * names, per the optional "contextInitializerClasses" servlet init-param.
  395 | 	 * @see #configureAndRefreshWebApplicationContext(ConfigurableWebApplicationContext)
  396 | 	 * @see #applyInitializers(ConfigurableApplicationContext)
  397 | 	 */
  398 | 	public void setContextInitializerClasses(String contextInitializerClasses) {
  399 | 		this.contextInitializerClasses = contextInitializerClasses;
  400 | 	}
  401 | 
  402 | 	/**
  403 | 	 * Set whether to publish this servlet's context as a ServletContext attribute,
  404 | 	 * available to all objects in the web container. Default is "true".
  405 | 	 * <p>This is especially handy during testing, although it is debatable whether
  406 | 	 * it's good practice to let other application objects access the context this way.
  407 | 	 */
  408 | 	public void setPublishContext(boolean publishContext) {
  409 | 		this.publishContext = publishContext;
  410 | 	}
  411 | 
  412 | 	/**
  413 | 	 * Set whether this servlet should publish a ServletRequestHandledEvent at the end
  414 | 	 * of each request. Default is "true"; can be turned off for a slight performance
  415 | 	 * improvement, provided that no ApplicationListeners rely on such events.
  416 | 	 * @see org.springframework.web.context.support.ServletRequestHandledEvent
  417 | 	 */
  418 | 	public void setPublishEvents(boolean publishEvents) {
  419 | 		this.publishEvents = publishEvents;
  420 | 	}
  421 | 
  422 | 	/**
  423 | 	 * Set whether to expose the LocaleContext and RequestAttributes as inheritable
  424 | 	 * for child threads (using an {@link java.lang.InheritableThreadLocal}).
  425 | 	 * <p>Default is "false", to avoid side effects on spawned background threads.
  426 | 	 * Switch this to "true" to enable inheritance for custom child threads which
  427 | 	 * are spawned during request processing and only used for this request
  428 | 	 * (that is, ending after their initial task, without reuse of the thread).
  429 | 	 * <p><b>WARNING:</b> Do not use inheritance for child threads if you are
  430 | 	 * accessing a thread pool which is configured to potentially add new threads
  431 | 	 * on demand (for example, a JDK {@link java.util.concurrent.ThreadPoolExecutor}),
  432 | 	 * since this will expose the inherited context to such a pooled thread.
  433 | 	 */
  434 | 	public void setThreadContextInheritable(boolean threadContextInheritable) {
  435 | 		this.threadContextInheritable = threadContextInheritable;
  436 | 	}
  437 | 
  438 | 	/**
  439 | 	 * Set whether this servlet should dispatch an HTTP OPTIONS request to
  440 | 	 * the {@link #doService} method.
  441 | 	 * <p>Default in the {@code FrameworkServlet} is "false", applying
  442 | 	 * {@link jakarta.servlet.http.HttpServlet}'s default behavior (i.e.enumerating
  443 | 	 * all standard HTTP request methods as a response to the OPTIONS request).
  444 | 	 * Note however that as of 4.3 the {@code DispatcherServlet} sets this
  445 | 	 * property to "true" by default due to its built-in support for OPTIONS.
  446 | 	 * <p>Turn this flag on if you prefer OPTIONS requests to go through the
  447 | 	 * regular dispatching chain, just like other HTTP requests. This usually
  448 | 	 * means that your controllers will receive those requests; make sure
  449 | 	 * that those endpoints are actually able to handle an OPTIONS request.
  450 | 	 * <p>Note that HttpServlet's default OPTIONS processing will be applied
  451 | 	 * in any case if your controllers happen to not set the 'Allow' header
  452 | 	 * (as required for an OPTIONS response).
  453 | 	 */
  454 | 	public void setDispatchOptionsRequest(boolean dispatchOptionsRequest) {
  455 | 		this.dispatchOptionsRequest = dispatchOptionsRequest;
  456 | 	}
  457 | 
  458 | 	/**
  459 | 	 * Set whether this servlet should dispatch an HTTP TRACE request to
  460 | 	 * the {@link #doService} method.
  461 | 	 * <p>Default is "false", applying {@link jakarta.servlet.http.HttpServlet}'s
  462 | 	 * default behavior (i.e. reflecting the message received back to the client).
  463 | 	 * <p>Turn this flag on if you prefer TRACE requests to go through the
  464 | 	 * regular dispatching chain, just like other HTTP requests. This usually means
  465 | 	 * that your controllers will receive those requests, in which case you must
  466 | 	 * make sure that those endpoints are actually able to handle a TRACE request.
  467 | 	 * <p>Note that HttpServlet's default TRACE processing will be applied
  468 | 	 * in any case if your controllers happen to not generate a response
  469 | 	 * of content type 'message/http' (as required for a TRACE response).
  470 | 	 */
  471 | 	public void setDispatchTraceRequest(boolean dispatchTraceRequest) {
  472 | 		this.dispatchTraceRequest = dispatchTraceRequest;
  473 | 	}
  474 | 
  475 | 	/**
  476 | 	 * Set whether to log request parameters at DEBUG level and headers at TRACE level.
  477 | 	 * Both may contain sensitive information.
  478 | 	 * <p>Defaults to {@code false} so that request details are not shown.
  479 | 	 * @param enable whether to enable or not
  480 | 	 * @since 5.1
  481 | 	 */
  482 | 	public void setEnableLoggingRequestDetails(boolean enable) {
  483 | 		this.enableLoggingRequestDetails = enable;
  484 | 	}
  485 | 
  486 | 	/**
  487 | 	 * Whether logging of potentially sensitive request details at DEBUG and
  488 | 	 * TRACE level is allowed.
  489 | 	 * @since 5.1
  490 | 	 */
  491 | 	public boolean isEnableLoggingRequestDetails() {
  492 | 		return this.enableLoggingRequestDetails;
  493 | 	}
  494 | 
  495 | 	/**
  496 | 	 * Called by Spring via {@link ApplicationContextAware} to inject the current
  497 | 	 * application context. This method allows FrameworkServlets to be registered as
  498 | 	 * Spring beans inside an existing {@link WebApplicationContext} rather than
  499 | 	 * {@link #findWebApplicationContext() finding} a
  500 | 	 * {@link org.springframework.web.context.ContextLoaderListener bootstrapped} context.
  501 | 	 * <p>Primarily added to support use in embedded servlet containers.
  502 | 	 * @since 4.0
  503 | 	 */
  504 | 	@Override
  505 | 	public void setApplicationContext(ApplicationContext applicationContext) {
  506 | 		if (this.webApplicationContext == null && applicationContext instanceof WebApplicationContext wac) {
  507 | 			this.webApplicationContext = wac;
  508 | 			this.webApplicationContextInjected = true;
  509 | 		}
  510 | 	}
  511 | 
  512 | 
  513 | 	/**
  514 | 	 * Overridden method of {@link HttpServletBean}, invoked after any bean properties
  515 | 	 * have been set. Creates this servlet's WebApplicationContext.
  516 | 	 */
  517 | 	@Override
  518 | 	protected final void initServletBean() throws ServletException {
  519 | 		getServletContext().log("Initializing Spring " + getClass().getSimpleName() + " '" + getServletName() + "'");
  520 | 		if (logger.isInfoEnabled()) {
  521 | 			logger.info("Initializing Servlet '" + getServletName() + "'");
  522 | 		}
  523 | 		long startTime = System.currentTimeMillis();
  524 | 
  525 | 		try {
  526 | 			this.webApplicationContext = initWebApplicationContext();
  527 | 			initFrameworkServlet();
  528 | 		}
  529 | 		catch (ServletException | RuntimeException ex) {
  530 | 			logger.error("Context initialization failed", ex);
  531 | 			throw ex;
  532 | 		}
  533 | 
  534 | 		if (logger.isDebugEnabled()) {
  535 | 			String value = this.enableLoggingRequestDetails ?
  536 | 					"shown which may lead to unsafe logging of potentially sensitive data" :
  537 | 					"masked to prevent unsafe logging of potentially sensitive data";
  538 | 			logger.debug("enableLoggingRequestDetails='" + this.enableLoggingRequestDetails +
  539 | 					"': request parameters and headers will be " + value);
  540 | 		}
  541 | 
  542 | 		if (logger.isInfoEnabled()) {
  543 | 			logger.info("Completed initialization in " + (System.currentTimeMillis() - startTime) + " ms");
  544 | 		}
  545 | 	}
  546 | 
  547 | 	/**
  548 | 	 * Initialize and publish the WebApplicationContext for this servlet.
  549 | 	 * <p>Delegates to {@link #createWebApplicationContext} for actual creation
  550 | 	 * of the context. Can be overridden in subclasses.
  551 | 	 * @return the WebApplicationContext instance
  552 | 	 * @see #FrameworkServlet(WebApplicationContext)
  553 | 	 * @see #setContextClass
  554 | 	 * @see #setContextConfigLocation
  555 | 	 */
  556 | 	protected WebApplicationContext initWebApplicationContext() {
  557 | 		WebApplicationContext rootContext =
  558 | 				WebApplicationContextUtils.getWebApplicationContext(getServletContext());
  559 | 		WebApplicationContext wac = null;
  560 | 
  561 | 		if (this.webApplicationContext != null) {
  562 | 			// A context instance was injected at construction time -> use it
  563 | 			wac = this.webApplicationContext;
  564 | 			if (wac instanceof ConfigurableWebApplicationContext cwac && !cwac.isActive()) {
  565 | 				// The context has not yet been refreshed -> provide services such as
  566 | 				// setting the parent context, setting the application context ID, etc
  567 | 				if (cwac.getParent() == null) {
  568 | 					// The context instance was injected without an explicit parent -> set
  569 | 					// the root application context (if any; may be null) as the parent
  570 | 					cwac.setParent(rootContext);
  571 | 				}
  572 | 				configureAndRefreshWebApplicationContext(cwac);
  573 | 			}
  574 | 		}
  575 | 		if (wac == null) {
  576 | 			// No context instance was injected at construction time -> see if one
  577 | 			// has been registered in the servlet context. If one exists, it is assumed
  578 | 			// that the parent context (if any) has already been set and that the
  579 | 			// user has performed any initialization such as setting the context ID
  580 | 			wac = findWebApplicationContext();
  581 | 		}
  582 | 		if (wac == null) {
  583 | 			// No context instance is defined for this servlet -> create a local one
  584 | 			wac = createWebApplicationContext(rootContext);
  585 | 		}
  586 | 
  587 | 		if (!this.refreshEventReceived) {
  588 | 			// Either the context is not a ConfigurableApplicationContext with refresh
  589 | 			// support or the context injected at construction time had already been
  590 | 			// refreshed -> trigger initial onRefresh manually here.
  591 | 			synchronized (this.onRefreshMonitor) {
  592 | 				onRefresh(wac);
  593 | 			}
  594 | 		}
  595 | 
  596 | 		if (this.publishContext) {
  597 | 			// Publish the context as a servlet context attribute.
  598 | 			String attrName = getServletContextAttributeName();
  599 | 			getServletContext().setAttribute(attrName, wac);
  600 | 		}
  601 | 
  602 | 		return wac;
  603 | 	}
  604 | 
  605 | 	/**
  606 | 	 * Retrieve a {@code WebApplicationContext} from the {@code ServletContext}
  607 | 	 * attribute with the {@link #setContextAttribute configured name}. The
  608 | 	 * {@code WebApplicationContext} must have already been loaded and stored in the
  609 | 	 * {@code ServletContext} before this servlet gets initialized (or invoked).
  610 | 	 * <p>Subclasses may override this method to provide a different
  611 | 	 * {@code WebApplicationContext} retrieval strategy.
  612 | 	 * @return the WebApplicationContext for this servlet, or {@code null} if not found
  613 | 	 * @see #getContextAttribute()
  614 | 	 */
  615 | 	protected @Nullable WebApplicationContext findWebApplicationContext() {
  616 | 		String attrName = getContextAttribute();
  617 | 		if (attrName == null) {
  618 | 			return null;
  619 | 		}
  620 | 		WebApplicationContext wac =
  621 | 				WebApplicationContextUtils.getWebApplicationContext(getServletContext(), attrName);
  622 | 		if (wac == null) {
  623 | 			throw new IllegalStateException("No WebApplicationContext found: initializer not registered?");
  624 | 		}
  625 | 		return wac;
  626 | 	}
  627 | 
  628 | 	/**
  629 | 	 * Instantiate the WebApplicationContext for this servlet, either a default
  630 | 	 * {@link org.springframework.web.context.support.XmlWebApplicationContext}
  631 | 	 * or a {@link #setContextClass custom context class}, if set.
  632 | 	 * <p>This implementation expects custom contexts to implement the
  633 | 	 * {@link org.springframework.web.context.ConfigurableWebApplicationContext}
  634 | 	 * interface. Can be overridden in subclasses.
  635 | 	 * <p>Do not forget to register this servlet instance as application listener on the
  636 | 	 * created context (for triggering its {@link #onRefresh callback}), and to call
  637 | 	 * {@link org.springframework.context.ConfigurableApplicationContext#refresh()}
  638 | 	 * before returning the context instance.
  639 | 	 * @param parent the parent ApplicationContext to use, or {@code null} if none
  640 | 	 * @return the WebApplicationContext for this servlet
  641 | 	 * @see org.springframework.web.context.support.XmlWebApplicationContext
  642 | 	 */
  643 | 	protected WebApplicationContext createWebApplicationContext(@Nullable ApplicationContext parent) {
  644 | 		Class<?> contextClass = getContextClass();
  645 | 		if (!ConfigurableWebApplicationContext.class.isAssignableFrom(contextClass)) {
  646 | 			throw new ApplicationContextException(
  647 | 					"Fatal initialization error in servlet with name '" + getServletName() +
  648 | 					"': custom WebApplicationContext class [" + contextClass.getName() +
  649 | 					"] is not of type ConfigurableWebApplicationContext");
  650 | 		}
  651 | 		ConfigurableWebApplicationContext wac =
  652 | 				(ConfigurableWebApplicationContext) BeanUtils.instantiateClass(contextClass);
  653 | 
  654 | 		wac.setEnvironment(getEnvironment());
  655 | 		wac.setParent(parent);
  656 | 		String configLocation = getContextConfigLocation();
  657 | 		if (configLocation != null) {
  658 | 			wac.setConfigLocation(configLocation);
  659 | 		}
  660 | 		configureAndRefreshWebApplicationContext(wac);
  661 | 
  662 | 		return wac;
  663 | 	}
  664 | 
  665 | 	protected void configureAndRefreshWebApplicationContext(ConfigurableWebApplicationContext wac) {
  666 | 		if (ObjectUtils.identityToString(wac).equals(wac.getId())) {
  667 | 			// The application context ID is still set to its original default value
  668 | 			// -> assign a more useful ID based on available information
  669 | 			if (this.contextId != null) {
  670 | 				wac.setId(this.contextId);
  671 | 			}
  672 | 			else {
  673 | 				// Generate default ID...
  674 | 				wac.setId(ConfigurableWebApplicationContext.APPLICATION_CONTEXT_ID_PREFIX +
  675 | 						ObjectUtils.getDisplayString(getServletContext().getContextPath()) + '/' + getServletName());
  676 | 			}
  677 | 		}
  678 | 
  679 | 		wac.setServletContext(getServletContext());
  680 | 		wac.setServletConfig(getServletConfig());
  681 | 		wac.setNamespace(getNamespace());
  682 | 		wac.addApplicationListener(new SourceFilteringListener(wac, new ContextRefreshListener()));
  683 | 
  684 | 		// The wac environment's #initPropertySources will be called in any case when the context
  685 | 		// is refreshed; do it eagerly here to ensure servlet property sources are in place for
  686 | 		// use in any post-processing or initialization that occurs below prior to #refresh
  687 | 		ConfigurableEnvironment env = wac.getEnvironment();
  688 | 		if (env instanceof ConfigurableWebEnvironment cwe) {
  689 | 			cwe.initPropertySources(getServletContext(), getServletConfig());
  690 | 		}
  691 | 
  692 | 		postProcessWebApplicationContext(wac);
  693 | 		applyInitializers(wac);
  694 | 		wac.refresh();
  695 | 	}
  696 | 
  697 | 	/**
  698 | 	 * Instantiate the WebApplicationContext for this servlet, either a default
  699 | 	 * {@link org.springframework.web.context.support.XmlWebApplicationContext}
  700 | 	 * or a {@link #setContextClass custom context class}, if set.
  701 | 	 * Delegates to #createWebApplicationContext(ApplicationContext).
  702 | 	 * @param parent the parent WebApplicationContext to use, or {@code null} if none
  703 | 	 * @return the WebApplicationContext for this servlet
  704 | 	 * @see org.springframework.web.context.support.XmlWebApplicationContext
  705 | 	 * @see #createWebApplicationContext(ApplicationContext)
  706 | 	 */
  707 | 	protected WebApplicationContext createWebApplicationContext(@Nullable WebApplicationContext parent) {
  708 | 		return createWebApplicationContext((ApplicationContext) parent);
  709 | 	}
  710 | 
  711 | 	/**
  712 | 	 * Post-process the given WebApplicationContext before it is refreshed
  713 | 	 * and activated as context for this servlet.
  714 | 	 * <p>The default implementation is empty. {@code refresh()} will
  715 | 	 * be called automatically after this method returns.
  716 | 	 * <p>Note that this method is designed to allow subclasses to modify the application
  717 | 	 * context, while {@link #initWebApplicationContext} is designed to allow
  718 | 	 * end-users to modify the context through the use of
  719 | 	 * {@link ApplicationContextInitializer ApplicationContextInitializers}.
  720 | 	 * @param wac the configured WebApplicationContext (not refreshed yet)
  721 | 	 * @see #createWebApplicationContext
  722 | 	 * @see #initWebApplicationContext
  723 | 	 * @see ConfigurableWebApplicationContext#refresh()
  724 | 	 */
  725 | 	protected void postProcessWebApplicationContext(ConfigurableWebApplicationContext wac) {
  726 | 	}
  727 | 
  728 | 	/**
  729 | 	 * Delegate the WebApplicationContext before it is refreshed to any
  730 | 	 * {@link ApplicationContextInitializer} instances specified by the
  731 | 	 * "contextInitializerClasses" servlet init-param.
  732 | 	 * <p>See also {@link #postProcessWebApplicationContext}, which is designed to allow
  733 | 	 * subclasses (as opposed to end-users) to modify the application context, and is
  734 | 	 * called immediately before this method.
  735 | 	 * @param wac the configured WebApplicationContext (not refreshed yet)
  736 | 	 * @see #createWebApplicationContext
  737 | 	 * @see #postProcessWebApplicationContext
  738 | 	 * @see ConfigurableApplicationContext#refresh()
  739 | 	 */
  740 | 	protected void applyInitializers(ConfigurableApplicationContext wac) {
  741 | 		String globalClassNames = getServletContext().getInitParameter(ContextLoader.GLOBAL_INITIALIZER_CLASSES_PARAM);
  742 | 		if (globalClassNames != null) {
  743 | 			for (String className : StringUtils.tokenizeToStringArray(globalClassNames, INIT_PARAM_DELIMITERS)) {
  744 | 				this.contextInitializers.add(loadInitializer(className, wac));
  745 | 			}
  746 | 		}
  747 | 
  748 | 		if (this.contextInitializerClasses != null) {
  749 | 			for (String className : StringUtils.tokenizeToStringArray(this.contextInitializerClasses, INIT_PARAM_DELIMITERS)) {
  750 | 				this.contextInitializers.add(loadInitializer(className, wac));
  751 | 			}
  752 | 		}
  753 | 
  754 | 		AnnotationAwareOrderComparator.sort(this.contextInitializers);
  755 | 		for (ApplicationContextInitializer<ConfigurableApplicationContext> initializer : this.contextInitializers) {
  756 | 			initializer.initialize(wac);
  757 | 		}
  758 | 	}
  759 | 
  760 | 	@SuppressWarnings("unchecked")
  761 | 	private ApplicationContextInitializer<ConfigurableApplicationContext> loadInitializer(
  762 | 			String className, ConfigurableApplicationContext wac) {
  763 | 		try {
  764 | 			Class<?> initializerClass = ClassUtils.forName(className, wac.getClassLoader());
  765 | 			Class<?> initializerContextClass =
  766 | 					GenericTypeResolver.resolveTypeArgument(initializerClass, ApplicationContextInitializer.class);
  767 | 			if (initializerContextClass != null && !initializerContextClass.isInstance(wac)) {
  768 | 				throw new ApplicationContextException(String.format(
  769 | 						"Could not apply context initializer [%s] since its generic parameter [%s] " +
  770 | 						"is not assignable from the type of application context used by this " +
  771 | 						"framework servlet: [%s]", initializerClass.getName(), initializerContextClass.getName(),
  772 | 						wac.getClass().getName()));
  773 | 			}
  774 | 			return BeanUtils.instantiateClass(initializerClass, ApplicationContextInitializer.class);
  775 | 		}
  776 | 		catch (ClassNotFoundException ex) {
  777 | 			throw new ApplicationContextException(String.format("Could not load class [%s] specified " +
  778 | 					"via 'contextInitializerClasses' init-param", className), ex);
  779 | 		}
  780 | 	}
  781 | 
  782 | 	/**
  783 | 	 * Return the ServletContext attribute name for this servlet's WebApplicationContext.
  784 | 	 * <p>The default implementation returns
  785 | 	 * {@code SERVLET_CONTEXT_PREFIX + servlet name}.
  786 | 	 * @see #SERVLET_CONTEXT_PREFIX
  787 | 	 * @see #getServletName
  788 | 	 */
  789 | 	public String getServletContextAttributeName() {
  790 | 		return SERVLET_CONTEXT_PREFIX + getServletName();
  791 | 	}
  792 | 
  793 | 	/**
  794 | 	 * Return this servlet's WebApplicationContext.
  795 | 	 */
  796 | 	public final @Nullable WebApplicationContext getWebApplicationContext() {
  797 | 		return this.webApplicationContext;
  798 | 	}
  799 | 
  800 | 
  801 | 	/**
  802 | 	 * This method will be invoked after any bean properties have been set and
  803 | 	 * the WebApplicationContext has been loaded. The default implementation is empty;
  804 | 	 * subclasses may override this method to perform any initialization they require.
  805 | 	 * @throws ServletException in case of an initialization exception
  806 | 	 */
  807 | 	protected void initFrameworkServlet() throws ServletException {
  808 | 	}
  809 | 
  810 | 	/**
  811 | 	 * Refresh this servlet's application context, as well as the
  812 | 	 * dependent state of the servlet.
  813 | 	 * @see #getWebApplicationContext()
  814 | 	 * @see org.springframework.context.ConfigurableApplicationContext#refresh()
  815 | 	 */
  816 | 	public void refresh() {
  817 | 		WebApplicationContext wac = getWebApplicationContext();
  818 | 		if (!(wac instanceof ConfigurableApplicationContext cac)) {
  819 | 			throw new IllegalStateException("WebApplicationContext does not support refresh: " + wac);
  820 | 		}
  821 | 		cac.refresh();
  822 | 	}
  823 | 
  824 | 	/**
  825 | 	 * Callback that receives refresh events from this servlet's WebApplicationContext.
  826 | 	 * <p>The default implementation calls {@link #onRefresh},
  827 | 	 * triggering a refresh of this servlet's context-dependent state.
  828 | 	 * @param event the incoming ApplicationContext event
  829 | 	 */
  830 | 	public void onApplicationEvent(ContextRefreshedEvent event) {
  831 | 		this.refreshEventReceived = true;
  832 | 		synchronized (this.onRefreshMonitor) {
  833 | 			onRefresh(event.getApplicationContext());
  834 | 		}
  835 | 	}
  836 | 
  837 | 	/**
  838 | 	 * Template method which can be overridden to add servlet-specific refresh work.
  839 | 	 * Called after successful context refresh.
  840 | 	 * <p>This implementation is empty.
  841 | 	 * @param context the current WebApplicationContext
  842 | 	 * @see #refresh()
  843 | 	 */
  844 | 	protected void onRefresh(ApplicationContext context) {
  845 | 		// For subclasses: do nothing by default.
  846 | 	}
  847 | 
  848 | 	/**
  849 | 	 * Close the WebApplicationContext of this servlet.
  850 | 	 * @see org.springframework.context.ConfigurableApplicationContext#close()
  851 | 	 */
  852 | 	@Override
  853 | 	public void destroy() {
  854 | 		getServletContext().log("Destroying Spring FrameworkServlet '" + getServletName() + "'");
  855 | 		// Only call close() on WebApplicationContext if locally managed...
  856 | 		if (!this.webApplicationContextInjected &&
  857 | 				this.webApplicationContext instanceof ConfigurableApplicationContext cac) {
  858 | 			cac.close();
  859 | 		}
  860 | 	}
  861 | 
  862 | 
  863 | 	/**
  864 | 	 * Override the parent class implementation in order to intercept requests
  865 | 	 * using non-standard HTTP methods (such as WebDAV).
  866 | 	 */
  867 | 	@Override
  868 | 	protected void service(HttpServletRequest request, HttpServletResponse response)
  869 | 			throws ServletException, IOException {
  870 | 
  871 | 		if (HTTP_SERVLET_METHODS.contains(request.getMethod())) {
  872 | 			super.service(request, response);
  873 | 		}
  874 | 		else {
  875 | 			processRequest(request, response);
  876 | 		}
  877 | 	}
  878 | 
  879 | 	/**
  880 | 	 * Delegate {@code GET} requests to processRequest/doService.
  881 | 	 * <p>Will also be invoked by HttpServlet's default implementation of {@code doHead},
  882 | 	 * with a {@code NoBodyResponse} that just captures the content length.
  883 | 	 * @see #doService
  884 | 	 * @see #doHead
  885 | 	 */
  886 | 	@Override
  887 | 	protected final void doGet(HttpServletRequest request, HttpServletResponse response)
  888 | 			throws ServletException, IOException {
  889 | 
  890 | 		processRequest(request, response);
  891 | 	}
  892 | 
  893 | 	/**
  894 | 	 * Delegate {@code PATCH} requests to {@link #processRequest}.
  895 | 	 * @since 7.1
  896 | 	 * @see #doService
  897 | 	 */
  898 | 	@Override
  899 | 	protected final void doPatch(HttpServletRequest request, HttpServletResponse response)
  900 | 			throws ServletException, IOException {
  901 | 
  902 | 		processRequest(request, response);
  903 | 	}
  904 | 
  905 | 	/**
  906 | 	 * Delegate {@code POST} requests to {@link #processRequest}.
  907 | 	 * @see #doService
  908 | 	 */
  909 | 	@Override
  910 | 	protected final void doPost(HttpServletRequest request, HttpServletResponse response)
  911 | 			throws ServletException, IOException {
  912 | 
  913 | 		processRequest(request, response);
  914 | 	}
  915 | 
  916 | 	/**
  917 | 	 * Delegate {@code PUT} requests to {@link #processRequest}.
  918 | 	 * @see #doService
  919 | 	 */
  920 | 	@Override
  921 | 	protected final void doPut(HttpServletRequest request, HttpServletResponse response)
  922 | 			throws ServletException, IOException {
  923 | 
  924 | 		processRequest(request, response);
  925 | 	}
  926 | 
  927 | 	/**
  928 | 	 * Delegate {@code DELETE} requests to {@link #processRequest}.
  929 | 	 * @see #doService
  930 | 	 */
  931 | 	@Override
  932 | 	protected final void doDelete(HttpServletRequest request, HttpServletResponse response)
  933 | 			throws ServletException, IOException {
  934 | 
  935 | 		processRequest(request, response);
  936 | 	}
  937 | 
  938 | 	/**
  939 | 	 * Delegate {@code OPTIONS} requests to {@link #processRequest}, if desired.
  940 | 	 * <p>Applies HttpServlet's standard OPTIONS processing otherwise,
  941 | 	 * and also if there is still no 'Allow' header set after dispatching.
  942 | 	 * @see #doService
  943 | 	 */
  944 | 	@Override
  945 | 	protected void doOptions(HttpServletRequest request, HttpServletResponse response)
  946 | 			throws ServletException, IOException {
  947 | 
  948 | 		if (this.dispatchOptionsRequest || CorsUtils.isPreFlightRequest(request)) {
  949 | 			processRequest(request, response);
  950 | 			if (response.containsHeader(HttpHeaders.ALLOW)) {
  951 | 				// Proper OPTIONS response coming from a handler - we're done.
  952 | 				return;
  953 | 			}
  954 | 		}
  955 | 
  956 | 		super.doOptions(request, response);
  957 | 	}
  958 | 
  959 | 	/**
  960 | 	 * Delegate {@code TRACE} requests to {@link #processRequest}, if desired.
  961 | 	 * <p>Applies HttpServlet's standard TRACE processing otherwise.
  962 | 	 * @see #doService
  963 | 	 */
  964 | 	@Override
  965 | 	protected void doTrace(HttpServletRequest request, HttpServletResponse response)
  966 | 			throws ServletException, IOException {
  967 | 
  968 | 		if (this.dispatchTraceRequest) {
  969 | 			processRequest(request, response);
  970 | 			if ("message/http".equals(response.getContentType())) {
  971 | 				// Proper TRACE response coming from a handler - we're done.
  972 | 				return;
  973 | 			}
  974 | 		}
  975 | 		super.doTrace(request, response);
  976 | 	}
  977 | 
  978 | 	/**
  979 | 	 * Process this request, publishing an event regardless of the outcome.
  980 | 	 * <p>The actual event handling is performed by the abstract
  981 | 	 * {@link #doService} template method.
  982 | 	 */
  983 | 	protected final void processRequest(HttpServletRequest request, HttpServletResponse response)
  984 | 			throws ServletException, IOException {
  985 | 
  986 | 		long startTime = System.currentTimeMillis();
  987 | 		Throwable failureCause = null;
  988 | 
  989 | 		LocaleContext previousLocaleContext = LocaleContextHolder.getLocaleContext();
  990 | 		LocaleContext localeContext = buildLocaleContext(request);
  991 | 
  992 | 		RequestAttributes previousAttributes = RequestContextHolder.getRequestAttributes();
  993 | 		ServletRequestAttributes requestAttributes = buildRequestAttributes(request, response, previousAttributes);
  994 | 
  995 | 		WebAsyncManager asyncManager = WebAsyncUtils.getAsyncManager(request);
  996 | 		asyncManager.registerCallableInterceptor(FrameworkServlet.class.getName(), new RequestBindingInterceptor());
  997 | 
  998 | 		initContextHolders(request, localeContext, requestAttributes);
  999 | 
 1000 | 		try {
 1001 | 			doService(request, response);
 1002 | 		}
 1003 | 		catch (ServletException | IOException ex) {
 1004 | 			failureCause = ex;
 1005 | 			throw ex;
 1006 | 		}
 1007 | 		catch (Throwable ex) {
 1008 | 			failureCause = ex;
 1009 | 			throw new ServletException("Request processing failed: " + ex, ex);
 1010 | 		}
 1011 | 
 1012 | 		finally {
 1013 | 			resetContextHolders(request, previousLocaleContext, previousAttributes);
 1014 | 			if (requestAttributes != null) {
 1015 | 				requestAttributes.requestCompleted();
 1016 | 			}
 1017 | 			logResult(request, response, failureCause, asyncManager);
 1018 | 			publishRequestHandledEvent(request, response, startTime, failureCause);
 1019 | 		}
 1020 | 	}
 1021 | 
 1022 | 	/**
 1023 | 	 * Build a LocaleContext for the given request, exposing the request's
 1024 | 	 * primary locale as current locale.
 1025 | 	 * @param request current HTTP request
 1026 | 	 * @return the corresponding LocaleContext, or {@code null} if none to bind
 1027 | 	 * @see LocaleContextHolder#setLocaleContext
 1028 | 	 */
 1029 | 	protected @Nullable LocaleContext buildLocaleContext(HttpServletRequest request) {
 1030 | 		return new SimpleLocaleContext(request.getLocale());
 1031 | 	}
 1032 | 
 1033 | 	/**
 1034 | 	 * Build ServletRequestAttributes for the given request (potentially also
 1035 | 	 * holding a reference to the response), taking pre-bound attributes
 1036 | 	 * (and their type) into consideration.
 1037 | 	 * @param request current HTTP request
 1038 | 	 * @param response current HTTP response
 1039 | 	 * @param previousAttributes pre-bound RequestAttributes instance, if any
 1040 | 	 * @return the ServletRequestAttributes to bind, or {@code null} to preserve
 1041 | 	 * the previously bound instance (or not binding any, if none bound before)
 1042 | 	 * @see RequestContextHolder#setRequestAttributes
 1043 | 	 */
 1044 | 	protected @Nullable ServletRequestAttributes buildRequestAttributes(HttpServletRequest request,
 1045 | 			@Nullable HttpServletResponse response, @Nullable RequestAttributes previousAttributes) {
 1046 | 
 1047 | 		if (previousAttributes == null || previousAttributes instanceof ServletRequestAttributes) {
 1048 | 			return new ServletRequestAttributes(request, response);
 1049 | 		}
 1050 | 		else {
 1051 | 			return null;  // preserve the pre-bound RequestAttributes instance
 1052 | 		}
 1053 | 	}
 1054 | 
 1055 | 	private void initContextHolders(HttpServletRequest request,
 1056 | 			@Nullable LocaleContext localeContext, @Nullable RequestAttributes requestAttributes) {
 1057 | 
 1058 | 		if (localeContext != null) {
 1059 | 			LocaleContextHolder.setLocaleContext(localeContext, this.threadContextInheritable);
 1060 | 		}
 1061 | 		if (requestAttributes != null) {
 1062 | 			RequestContextHolder.setRequestAttributes(requestAttributes, this.threadContextInheritable);
 1063 | 		}
 1064 | 	}
 1065 | 
 1066 | 	private void resetContextHolders(HttpServletRequest request,
 1067 | 			@Nullable LocaleContext prevLocaleContext, @Nullable RequestAttributes previousAttributes) {
 1068 | 
 1069 | 		LocaleContextHolder.setLocaleContext(prevLocaleContext, this.threadContextInheritable);
 1070 | 		RequestContextHolder.setRequestAttributes(previousAttributes, this.threadContextInheritable);
 1071 | 	}
 1072 | 
 1073 | 	private void logResult(HttpServletRequest request, HttpServletResponse response,
 1074 | 			@Nullable Throwable failureCause, WebAsyncManager asyncManager) {
 1075 | 
 1076 | 		if (!logger.isDebugEnabled()) {
 1077 | 			return;
 1078 | 		}
 1079 | 
 1080 | 		DispatcherType dispatchType = request.getDispatcherType();
 1081 | 		boolean initialDispatch = (dispatchType == DispatcherType.REQUEST);
 1082 | 
 1083 | 		if (failureCause != null) {
 1084 | 			if (!initialDispatch) {
 1085 | 				// FORWARD/ERROR/ASYNC: minimal message (there should be enough context already)
 1086 | 				if (logger.isDebugEnabled()) {
 1087 | 					logger.debug("Unresolved failure from \"" + dispatchType + "\" dispatch: " + failureCause);
 1088 | 				}
 1089 | 			}
 1090 | 			else if (logger.isTraceEnabled()) {
 1091 | 				logger.trace("Failed to complete request", failureCause);
 1092 | 			}
 1093 | 			else {
 1094 | 				logger.debug("Failed to complete request: " + failureCause);
 1095 | 			}
 1096 | 			return;
 1097 | 		}
 1098 | 
 1099 | 		if (asyncManager.isConcurrentHandlingStarted()) {
 1100 | 			logger.debug("Exiting but response remains open for further handling");
 1101 | 			return;
 1102 | 		}
 1103 | 
 1104 | 		int status = response.getStatus();
 1105 | 		String headers = "";  // nothing below trace
 1106 | 
 1107 | 		if (logger.isTraceEnabled()) {
 1108 | 			Collection<String> names = response.getHeaderNames();
 1109 | 			if (this.enableLoggingRequestDetails) {
 1110 | 				headers = names.stream().map(name -> name + ":" + response.getHeaders(name))
 1111 | 						.collect(Collectors.joining(", "));
 1112 | 			}
 1113 | 			else {
 1114 | 				headers = names.isEmpty() ? "" : "masked";
 1115 | 			}
 1116 | 			headers = ", headers={" + headers + "}";
 1117 | 		}
 1118 | 
 1119 | 		if (!initialDispatch) {
 1120 | 			logger.debug("Exiting from \"" + dispatchType + "\" dispatch, status " + status + headers);
 1121 | 		}
 1122 | 		else {
 1123 | 			logger.debug("Completed " + HttpStatusCode.valueOf(status) + headers);
 1124 | 		}
 1125 | 	}
 1126 | 
 1127 | 	private void publishRequestHandledEvent(HttpServletRequest request, HttpServletResponse response,
 1128 | 			long startTime, @Nullable Throwable failureCause) {
 1129 | 
 1130 | 		if (this.publishEvents && this.webApplicationContext != null) {
 1131 | 			// Whether or not we succeeded, publish an event.
 1132 | 			long processingTime = System.currentTimeMillis() - startTime;
 1133 | 			this.webApplicationContext.publishEvent(
 1134 | 					new ServletRequestHandledEvent(this,
 1135 | 							request.getRequestURI(), request.getRemoteAddr(),
 1136 | 							request.getMethod(), getServletConfig().getServletName(),
 1137 | 							WebUtils.getSessionId(request), getUsernameForRequest(request),
 1138 | 							processingTime, failureCause, response.getStatus()));
 1139 | 		}
 1140 | 	}
 1141 | 
 1142 | 	/**
 1143 | 	 * Determine the username for the given request.
 1144 | 	 * <p>The default implementation takes the name of the UserPrincipal, if any.
 1145 | 	 * Can be overridden in subclasses.
 1146 | 	 * @param request current HTTP request
 1147 | 	 * @return the username, or {@code null} if none found
 1148 | 	 * @see jakarta.servlet.http.HttpServletRequest#getUserPrincipal()
 1149 | 	 */
 1150 | 	protected @Nullable String getUsernameForRequest(HttpServletRequest request) {
 1151 | 		Principal userPrincipal = request.getUserPrincipal();
 1152 | 		return (userPrincipal != null ? userPrincipal.getName() : null);
 1153 | 	}
 1154 | 
 1155 | 
 1156 | 	/**
 1157 | 	 * Subclasses must implement this method to do the work of request handling,
 1158 | 	 * receiving a centralized callback for {@code GET}, {@code PATCH}, {@code POST},
 1159 | 	 * {@code PUT}, {@code DELETE}, {@code OPTIONS}, and {@code TRACE} requests
 1160 | 	 * as well as for requests using non-standard HTTP methods (such as WebDAV).
 1161 | 	 * <p>The contract is essentially the same as that for the commonly overridden
 1162 | 	 * {@code doGet} or {@code doPost} methods of HttpServlet.
 1163 | 	 * <p>This class intercepts calls to ensure that exception handling and
 1164 | 	 * event publication takes place.
 1165 | 	 * @param request current HTTP request
 1166 | 	 * @param response current HTTP response
 1167 | 	 * @throws Exception in case of any kind of processing failure
 1168 | 	 * @see jakarta.servlet.http.HttpServlet#doGet
 1169 | 	 * @see jakarta.servlet.http.HttpServlet#doPost
 1170 | 	 */
 1171 | 	protected abstract void doService(HttpServletRequest request, HttpServletResponse response)
 1172 | 			throws Exception;
 1173 | 
 1174 | 
 1175 | 	/**
 1176 | 	 * ApplicationListener endpoint that receives events from this servlet's WebApplicationContext
 1177 | 	 * only, delegating to {@code onApplicationEvent} on the FrameworkServlet instance.
 1178 | 	 */
 1179 | 	private class ContextRefreshListener implements ApplicationListener<ContextRefreshedEvent> {
 1180 | 
 1181 | 		@Override
 1182 | 		public void onApplicationEvent(ContextRefreshedEvent event) {
 1183 | 			FrameworkServlet.this.onApplicationEvent(event);
 1184 | 		}
 1185 | 	}
 1186 | 
 1187 | 
 1188 | 	/**
 1189 | 	 * CallableProcessingInterceptor implementation that initializes and resets
 1190 | 	 * FrameworkServlet's context holders, i.e. LocaleContextHolder and RequestContextHolder.
 1191 | 	 */
 1192 | 	private class RequestBindingInterceptor implements CallableProcessingInterceptor {
 1193 | 
 1194 | 		@Override
 1195 | 		public <T> void preProcess(NativeWebRequest webRequest, Callable<T> task) {
 1196 | 			HttpServletRequest request = webRequest.getNativeRequest(HttpServletRequest.class);
 1197 | 			if (request != null) {
 1198 | 				HttpServletResponse response = webRequest.getNativeResponse(HttpServletResponse.class);
 1199 | 				initContextHolders(request, buildLocaleContext(request),
 1200 | 						buildRequestAttributes(request, response, null));
 1201 | 			}
 1202 | 		}
 1203 | 		@Override
 1204 | 		public <T> void postProcess(NativeWebRequest webRequest, Callable<T> task, @Nullable Object concurrentResult) {
 1205 | 			HttpServletRequest request = webRequest.getNativeRequest(HttpServletRequest.class);
 1206 | 			if (request != null) {
 1207 | 				resetContextHolders(request, null, null);
 1208 | 			}
 1209 | 		}
 1210 | 	}
 1211 | 
 1212 | }
```
