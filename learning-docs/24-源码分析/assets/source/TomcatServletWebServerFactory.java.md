<!-- learning-docs source archive（机械抓取原文，禁止手改代码内容） -->
<!-- origin: org.springframework.boot:spring-boot-tomcat:4.2.0 | path: org/springframework/boot/tomcat/servlet/TomcatServletWebServerFactory.java -->
<!-- 正文引用：[[源码:TomcatServletWebServerFactory.java#L起-L止]] ；代码块头部：(src: TomcatServletWebServerFactory.java#L起-L止) -->

# TomcatServletWebServerFactory.java

| 项 | 值 |
| --- | --- |
| 来源 | `org.springframework.boot:spring-boot-tomcat:4.2.0` |
| 路径 | `org/springframework/boot/tomcat/servlet/TomcatServletWebServerFactory.java` |
| 行数 | 697 |
| 在线地址 | https://repo1.maven.org/maven2/org/springframework/boot/spring-boot-tomcat/4.2.0/spring-boot-tomcat-4.2.0-sources.jar |
| 抓取时间 | 2026-11-25T14:01:01.099Z |

```java
    1 | /*
    2 |  * Copyright 2012-present the original author or authors.
    3 |  */
    4 | 
    5 | package org.springframework.boot.tomcat.servlet;
    6 | 
    7 | import java.io.File;
    8 | import java.io.InputStream;
    9 | import java.lang.reflect.Method;
   10 | import java.net.MalformedURLException;
   11 | import java.net.URL;
   12 | import java.nio.charset.Charset;
   13 | import java.nio.charset.StandardCharsets;
   14 | import java.time.Duration;
   15 | import java.util.ArrayList;
   16 | import java.util.Arrays;
   17 | import java.util.Collection;
   18 | import java.util.Collections;
   19 | import java.util.LinkedHashSet;
   20 | import java.util.List;
   21 | import java.util.Locale;
   22 | import java.util.Set;
   23 | import java.util.stream.Collectors;
   24 | 
   25 | import jakarta.servlet.ServletContainerInitializer;
   26 | import jakarta.servlet.http.Cookie;
   27 | import jakarta.servlet.http.HttpServletRequest;
   28 | import org.apache.catalina.Context;
   29 | import org.apache.catalina.Host;
   30 | import org.apache.catalina.Lifecycle;
   31 | import org.apache.catalina.LifecycleEvent;
   32 | import org.apache.catalina.LifecycleException;
   33 | import org.apache.catalina.LifecycleListener;
   34 | import org.apache.catalina.Manager;
   35 | import org.apache.catalina.Valve;
   36 | import org.apache.catalina.WebResource;
   37 | import org.apache.catalina.WebResourceRoot;
   38 | import org.apache.catalina.WebResourceRoot.ResourceSetType;
   39 | import org.apache.catalina.WebResourceSet;
   40 | import org.apache.catalina.Wrapper;
   41 | import org.apache.catalina.loader.WebappLoader;
   42 | import org.apache.catalina.session.StandardManager;
   43 | import org.apache.catalina.startup.Tomcat;
   44 | import org.apache.catalina.startup.Tomcat.FixContextListener;
   45 | import org.apache.catalina.util.LifecycleBase;
   46 | import org.apache.catalina.util.SessionConfig;
   47 | import org.apache.catalina.webresources.AbstractResourceSet;
   48 | import org.apache.catalina.webresources.EmptyResource;
   49 | import org.apache.catalina.webresources.StandardRoot;
   50 | import org.apache.commons.logging.Log;
   51 | import org.apache.commons.logging.LogFactory;
   52 | import org.apache.tomcat.util.http.Rfc6265CookieProcessor;
   53 | import org.apache.tomcat.util.scan.StandardJarScanFilter;
   54 | import org.jspecify.annotations.Nullable;
   55 | 
   56 | import org.springframework.boot.tomcat.ConfigurableTomcatWebServerFactory;
   57 | import org.springframework.boot.tomcat.DisableReferenceClearingContextCustomizer;
   58 | import org.springframework.boot.tomcat.TomcatContextCustomizer;
   59 | import org.springframework.boot.tomcat.TomcatEmbeddedContext;
   60 | import org.springframework.boot.tomcat.TomcatEmbeddedWebappClassLoader;
   61 | import org.springframework.boot.tomcat.TomcatWebServer;
   62 | import org.springframework.boot.tomcat.TomcatWebServerFactory;
   63 | import org.springframework.boot.web.error.ErrorPage;
   64 | import org.springframework.boot.web.server.Cookie.SameSite;
   65 | import org.springframework.boot.web.server.MimeMappings;
   66 | import org.springframework.boot.web.server.WebServer;
   67 | import org.springframework.boot.web.server.servlet.ConfigurableServletWebServerFactory;
   68 | import org.springframework.boot.web.server.servlet.ContextPath;
   69 | import org.springframework.boot.web.server.servlet.CookieSameSiteSupplier;
   70 | import org.springframework.boot.web.server.servlet.DocumentRoot;
   71 | import org.springframework.boot.web.server.servlet.ServletContextInitializers;
   72 | import org.springframework.boot.web.server.servlet.ServletWebServerSettings;
   73 | import org.springframework.boot.web.servlet.ServletContextInitializer;
   74 | import org.springframework.context.ResourceLoaderAware;
   75 | import org.springframework.core.io.ResourceLoader;
   76 | import org.springframework.lang.Contract;
   77 | import org.springframework.util.Assert;
   78 | import org.springframework.util.ClassUtils;
   79 | import org.springframework.util.CollectionUtils;
   80 | import org.springframework.util.ReflectionUtils;
   81 | import org.springframework.util.StringUtils;
   82 | 
   83 | /**
   84 |  * {@link ConfigurableServletWebServerFactory} that can be used to create
   85 |  * {@link TomcatWebServer}s. Can be initialized using Spring's
   86 |  * {@link ServletContextInitializer}s or Tomcat {@link LifecycleListener}s.
   87 |  * <p>
   88 |  * Unless explicitly configured otherwise this factory will create containers that listen
   89 |  * for HTTP requests on port 8080.
   90 |  *
   91 |  * @author Phillip Webb
   92 |  * @author Dave Syer
   93 |  * @author Brock Mills
   94 |  * @author Stephane Nicoll
   95 |  * @author Andy Wilkinson
   96 |  * @author Eddú Meléndez
   97 |  * @author Christoffer Sawicki
   98 |  * @author Dawid Antecki
   99 |  * @author Moritz Halbritter
  100 |  * @author Scott Frederick
  101 |  * @since 4.0.0
  102 |  * @see #setPort(int)
  103 |  * @see #setContextLifecycleListeners(Collection)
  104 |  * @see TomcatWebServer
  105 |  */
  106 | public class TomcatServletWebServerFactory extends TomcatWebServerFactory
  107 | 		implements ConfigurableTomcatWebServerFactory, ConfigurableServletWebServerFactory, ResourceLoaderAware {
  108 | 
  109 | 	private static final Log logger = LogFactory.getLog(TomcatServletWebServerFactory.class);
  110 | 
  111 | 	private static final Charset DEFAULT_CHARSET = StandardCharsets.UTF_8;
  112 | 
  113 | 	private static final Set<Class<?>> NO_CLASSES = Collections.emptySet();
  114 | 
  115 | 	private final ServletWebServerSettings settings = new ServletWebServerSettings();
  116 | 
  117 | 	@SuppressWarnings("NullAway.Init")
  118 | 	private ResourceLoader resourceLoader;
  119 | 
  120 | 	private Set<String> tldSkipPatterns = new LinkedHashSet<>(TldPatterns.DEFAULT_SKIP);
  121 | 
  122 | 	private final Set<String> tldScanPatterns = new LinkedHashSet<>(TldPatterns.DEFAULT_SCAN);
  123 | 
  124 | 	/**
  125 | 	 * Create a new {@link TomcatServletWebServerFactory} instance.
  126 | 	 */
  127 | 	public TomcatServletWebServerFactory() {
  128 | 	}
  129 | 
  130 | 	/**
  131 | 	 * Create a new {@link TomcatServletWebServerFactory} that listens for requests using
  132 | 	 * the specified port.
  133 | 	 * @param port the port to listen on
  134 | 	 */
  135 | 	public TomcatServletWebServerFactory(int port) {
  136 | 		super(port);
  137 | 	}
  138 | 
  139 | 	/**
  140 | 	 * Create a new {@link TomcatServletWebServerFactory} with the specified context path
  141 | 	 * and port.
  142 | 	 * @param contextPath the root context path
  143 | 	 * @param port the port to listen on
  144 | 	 */
  145 | 	public TomcatServletWebServerFactory(String contextPath, int port) {
  146 | 		super(port);
  147 | 		this.settings.setContextPath(ContextPath.of(contextPath));
  148 | 	}
  149 | 
  150 | 	@Override
  151 | 	public WebServer getWebServer(ServletContextInitializer... initializers) {
  152 | 		TempDirs tempDirs = new TempDirs(getPort());
  153 | 		Tomcat tomcat = createTomcat(tempDirs);
  154 | 		prepareContext(tomcat.getHost(), initializers, tempDirs);
  155 | 		return getTomcatWebServer(tomcat);
  156 | 	}
  157 | 
  158 | 	/**
  159 | 	 * Prepares the context.
  160 | 	 * @param host the host
  161 | 	 * @param initializers the servlet context initializers
  162 | 	 * @deprecated since 4.1.0 for removal in 4.3.0 in favor of
  163 | 	 * {@link #prepareContext(Host, ServletContextInitializer[], TempDirs)}
  164 | 	 */
  165 | 	@Deprecated(forRemoval = true, since = "4.1.0")
  166 | 	protected void prepareContext(Host host, ServletContextInitializer[] initializers) {
  167 | 		prepareContext(host, initializers, new TempDirs(getPort()));
  168 | 	}
  169 | 
  170 | 	/**
  171 | 	 * Prepares the context.
  172 | 	 * @param host the host
  173 | 	 * @param initializers the servlet context initializers
  174 | 	 * @param tempDirs to manage temporary directories
  175 | 	 * @since 4.1.0
  176 | 	 */
  177 | 	protected void prepareContext(Host host, ServletContextInitializer[] initializers, TempDirs tempDirs) {
  178 | 		DocumentRoot documentRoot = new DocumentRoot(logger);
  179 | 		documentRoot.setDirectory(this.settings.getDocumentRoot());
  180 | 		File documentRootFile = documentRoot.getValidDirectory();
  181 | 		TomcatEmbeddedContext context = new TomcatEmbeddedContext();
  182 | 		WebResourceRoot resourceRoot = (documentRootFile != null) ? new LoaderHidingResourceRoot(context)
  183 | 				: new StandardRoot(context);
  184 | 		ignoringNoSuchMethodError(() -> resourceRoot.setReadOnly(true));
  185 | 		context.setResources(resourceRoot);
  186 | 		String contextPath = this.settings.getContextPath().toString();
  187 | 		context.setName(contextPath);
  188 | 		context.setDisplayName(this.settings.getDisplayName());
  189 | 		context.setPath(contextPath);
  190 | 		File docBase = (documentRootFile != null) ? documentRootFile
  191 | 				: tempDirs.createTempDir("tomcat-docbase").toFile();
  192 | 		context.setDocBase(docBase.getAbsolutePath());
  193 | 		context.addLifecycleListener(new FixContextListener());
  194 | 		ClassLoader parentClassLoader = (this.resourceLoader != null) ? this.resourceLoader.getClassLoader()
  195 | 				: ClassUtils.getDefaultClassLoader();
  196 | 		context.setParentClassLoader(parentClassLoader);
  197 | 		resetDefaultLocaleMapping(context);
  198 | 		addLocaleMappings(context);
  199 | 		context.setCreateUploadTargets(true);
  200 | 		configureTldPatterns(context);
  201 | 		WebappLoader loader = new WebappLoader();
  202 | 		loader.setLoaderInstance(new TomcatEmbeddedWebappClassLoader(parentClassLoader));
  203 | 		loader.setDelegate(true);
  204 | 		context.setLoader(loader);
  205 | 		if (this.settings.isRegisterDefaultServlet()) {
  206 | 			addDefaultServlet(context);
  207 | 		}
  208 | 		if (shouldRegisterJspServlet()) {
  209 | 			addJspServlet(context);
  210 | 			addJasperInitializer(context);
  211 | 		}
  212 | 		context.addLifecycleListener(new StaticResourceConfigurer(context));
  213 | 		ServletContextInitializers initializersToUse = ServletContextInitializers.from(this.settings, initializers);
  214 | 		host.addChild(context);
  215 | 		configureContext(context, initializersToUse);
  216 | 		postProcessContext(context);
  217 | 	}
  218 | 
  219 | 	private void ignoringNoSuchMethodError(Runnable method) {
  220 | 		try {
  221 | 			method.run();
  222 | 		}
  223 | 		catch (NoSuchMethodError ex) {
  224 | 			// Intentionally left blank
  225 | 		}
  226 | 	}
  227 | 
  228 | 	private boolean shouldRegisterJspServlet() {
  229 | 		return this.settings.getJsp() != null && this.settings.getJsp().getRegistered()
  230 | 				&& ClassUtils.isPresent(this.settings.getJsp().getClassName(), getClass().getClassLoader());
  231 | 	}
  232 | 
  233 | 	/**
  234 | 	 * Override Tomcat's default locale mappings to align with other servers. See
  235 | 	 * {@code org.apache.catalina.util.CharsetMapperDefault.properties}.
  236 | 	 * @param context the context to reset
  237 | 	 */
  238 | 	private void resetDefaultLocaleMapping(TomcatEmbeddedContext context) {
  239 | 		context.addLocaleEncodingMappingParameter(Locale.ENGLISH.toString(), DEFAULT_CHARSET.displayName());
  240 | 		context.addLocaleEncodingMappingParameter(Locale.FRENCH.toString(), DEFAULT_CHARSET.displayName());
  241 | 		context.addLocaleEncodingMappingParameter(Locale.JAPANESE.toString(), DEFAULT_CHARSET.displayName());
  242 | 	}
  243 | 
  244 | 	private void addLocaleMappings(TomcatEmbeddedContext context) {
  245 | 		this.settings.getLocaleCharsetMappings()
  246 | 			.forEach((locale, charset) -> context.addLocaleEncodingMappingParameter(locale.toString(),
  247 | 					charset.toString()));
  248 | 	}
  249 | 
  250 | 	private void configureTldPatterns(TomcatEmbeddedContext context) {
  251 | 		StandardJarScanFilter filter = new StandardJarScanFilter();
  252 | 		filter.setTldSkip(StringUtils.collectionToCommaDelimitedString(this.tldSkipPatterns));
  253 | 		filter.setTldScan(StringUtils.collectionToCommaDelimitedString(this.tldScanPatterns));
  254 | 		context.getJarScanner().setJarScanFilter(filter);
  255 | 	}
  256 | 
  257 | 	private void addDefaultServlet(Context context) {
  258 | 		Wrapper defaultServlet = context.createWrapper();
  259 | 		defaultServlet.setName("default");
  260 | 		defaultServlet.setServletClass("org.apache.catalina.servlets.DefaultServlet");
  261 | 		defaultServlet.addInitParameter("debug", "0");
  262 | 		defaultServlet.addInitParameter("listings", "false");
  263 | 		defaultServlet.setLoadOnStartup(1);
  264 | 		// Otherwise the default location of a Spring DispatcherServlet cannot be set
  265 | 		defaultServlet.setOverridable(true);
  266 | 		context.addChild(defaultServlet);
  267 | 		context.addServletMapping("/", "default");
  268 | 	}
  269 | 
  270 | 	private void addJspServlet(Context context) {
  271 | 		Wrapper jspServlet = context.createWrapper();
  272 | 		jspServlet.setName("jsp");
  273 | 		jspServlet.setServletClass(this.settings.getJsp().getClassName());
  274 | 		jspServlet.addInitParameter("fork", "false");
  275 | 		this.settings.getJsp().getInitParameters().forEach(jspServlet::addInitParameter);
  276 | 		jspServlet.setLoadOnStartup(3);
  277 | 		context.addChild(jspServlet);
  278 | 		context.addServletMapping("*.jsp", "jsp");
  279 | 		context.addServletMapping("*.jspx", "jsp");
  280 | 	}
  281 | 
  282 | 	private void addJasperInitializer(TomcatEmbeddedContext context) {
  283 | 		try {
  284 | 			ServletContainerInitializer initializer = (ServletContainerInitializer) ClassUtils
  285 | 				.forName("org.apache.jasper.servlet.JasperInitializer", null)
  286 | 				.getDeclaredConstructor()
  287 | 				.newInstance();
  288 | 			context.addServletContainerInitializer(initializer, null);
  289 | 		}
  290 | 		catch (Exception ex) {
  291 | 			// Probably not Tomcat 8
  292 | 		}
  293 | 	}
  294 | 
  295 | 	/**
  296 | 	 * Configure the Tomcat {@link Context}.
  297 | 	 * @param context the Tomcat context
  298 | 	 * @param initializers initializers to apply
  299 | 	 */
  300 | 	protected void configureContext(Context context, Iterable<ServletContextInitializer> initializers) {
  301 | 		DeferredServletContainerInitializers deferredInitializers = new DeferredServletContainerInitializers(
  302 | 				initializers);
  303 | 		if (context instanceof TomcatEmbeddedContext embeddedContext) {
  304 | 			embeddedContext.setDeferredStartupExceptions(deferredInitializers);
  305 | 			embeddedContext.setFailCtxIfServletStartFails(true);
  306 | 		}
  307 | 		context.addServletContainerInitializer(deferredInitializers, NO_CLASSES);
  308 | 		for (LifecycleListener lifecycleListener : this.getContextLifecycleListeners()) {
  309 | 			context.addLifecycleListener(lifecycleListener);
  310 | 		}
  311 | 		for (Valve valve : this.getContextValves()) {
  312 | 			context.getPipeline().addValve(valve);
  313 | 		}
  314 | 		for (ErrorPage errorPage : getErrorPages()) {
  315 | 			org.apache.tomcat.util.descriptor.web.ErrorPage tomcatErrorPage = new org.apache.tomcat.util.descriptor.web.ErrorPage();
  316 | 			tomcatErrorPage.setLocation(errorPage.getPath());
  317 | 			tomcatErrorPage.setErrorCode(errorPage.getStatusCode());
  318 | 			tomcatErrorPage.setExceptionType(errorPage.getExceptionName());
  319 | 			context.addErrorPage(tomcatErrorPage);
  320 | 		}
  321 | 		setMimeMappings(context);
  322 | 		configureSession(context);
  323 | 		configureCookieProcessor(context);
  324 | 		new DisableReferenceClearingContextCustomizer().customize(context);
  325 | 		for (String webListenerClassName : getSettings().getWebListenerClassNames()) {
  326 | 			context.addApplicationListener(webListenerClassName);
  327 | 		}
  328 | 		for (TomcatContextCustomizer customizer : this.getContextCustomizers()) {
  329 | 			customizer.customize(context);
  330 | 		}
  331 | 	}
  332 | 
  333 | 	private void configureSession(Context context) {
  334 | 		long sessionTimeout = getSessionTimeoutInMinutes();
  335 | 		context.setSessionTimeout((int) sessionTimeout);
  336 | 		Boolean httpOnly = this.settings.getSession().getCookie().getHttpOnly();
  337 | 		if (httpOnly != null) {
  338 | 			context.setUseHttpOnly(httpOnly);
  339 | 		}
  340 | 		if (this.settings.getSession().isPersistent()) {
  341 | 			Manager manager = context.getManager();
  342 | 			if (manager == null) {
  343 | 				manager = new StandardManager();
  344 | 				context.setManager(manager);
  345 | 			}
  346 | 			configurePersistSession(manager);
  347 | 		}
  348 | 		else {
  349 | 			context.addLifecycleListener(new DisablePersistSessionListener());
  350 | 		}
  351 | 	}
  352 | 
  353 | 	private void setMimeMappings(Context context) {
  354 | 		MimeMappings mimeMappings = this.settings.getMimeMappings();
  355 | 		if (context instanceof TomcatEmbeddedContext embeddedContext) {
  356 | 			embeddedContext.setMimeMappings(mimeMappings);
  357 | 			return;
  358 | 		}
  359 | 		for (MimeMappings.Mapping mapping : mimeMappings) {
  360 | 			context.addMimeMapping(mapping.getExtension(), mapping.getMimeType());
  361 | 		}
  362 | 	}
  363 | 
  364 | 	private void configureCookieProcessor(Context context) {
  365 | 		SameSite sessionSameSite = this.settings.getSession().getCookie().getSameSite();
  366 | 		List<CookieSameSiteSupplier> suppliers = new ArrayList<>();
  367 | 		if (sessionSameSite != null) {
  368 | 			suppliers.add(CookieSameSiteSupplier.of(sessionSameSite)
  369 | 				.whenHasName(() -> SessionConfig.getSessionCookieName(context)));
  370 | 		}
  371 | 		if (!CollectionUtils.isEmpty(this.settings.getCookieSameSiteSuppliers())) {
  372 | 			suppliers.addAll(this.settings.getCookieSameSiteSuppliers());
  373 | 		}
  374 | 		if (!suppliers.isEmpty()) {
  375 | 			context.setCookieProcessor(new SuppliedSameSiteCookieProcessor(suppliers));
  376 | 		}
  377 | 	}
  378 | 
  379 | 	private void configurePersistSession(Manager manager) {
  380 | 		Assert.state(manager instanceof StandardManager,
  381 | 				() -> "Unable to persist HTTP session state using manager type " + manager.getClass().getName());
  382 | 		File dir = this.settings.getSession().getSessionStoreDirectory().getValidDirectory(true);
  383 | 		File file = new File(dir, "SESSIONS.ser");
  384 | 		((StandardManager) manager).setPathname(file.getAbsolutePath());
  385 | 	}
  386 | 
  387 | 	private long getSessionTimeoutInMinutes() {
  388 | 		Duration sessionTimeout = this.settings.getSession().getTimeout();
  389 | 		if (isZeroOrLess(sessionTimeout)) {
  390 | 			return 0;
  391 | 		}
  392 | 		return Math.max(sessionTimeout.toMinutes(), 1);
  393 | 	}
  394 | 
  395 | 	@Contract("null -> true")
  396 | 	private boolean isZeroOrLess(@Nullable Duration sessionTimeout) {
  397 | 		return sessionTimeout == null || sessionTimeout.isNegative() || sessionTimeout.isZero();
  398 | 	}
  399 | 
  400 | 	/**
  401 | 	 * Post process the Tomcat {@link Context} before it's used with the Tomcat Server.
  402 | 	 * Subclasses can override this method to apply additional processing to the
  403 | 	 * {@link Context}.
  404 | 	 * @param context the Tomcat {@link Context}
  405 | 	 */
  406 | 	protected void postProcessContext(Context context) {
  407 | 	}
  408 | 
  409 | 	/**
  410 | 	 * Factory method called to create the {@link TomcatWebServer}. Subclasses can
  411 | 	 * override this method to return a different {@link TomcatWebServer} or apply
  412 | 	 * additional processing to the Tomcat server.
  413 | 	 * @param tomcat the Tomcat server.
  414 | 	 * @return a new {@link TomcatWebServer} instance
  415 | 	 */
  416 | 	protected TomcatWebServer getTomcatWebServer(Tomcat tomcat) {
  417 | 		return new TomcatWebServer(tomcat, getPort() >= 0, getShutdown());
  418 | 	}
  419 | 
  420 | 	@Override
  421 | 	public void setResourceLoader(ResourceLoader resourceLoader) {
  422 | 		this.resourceLoader = resourceLoader;
  423 | 	}
  424 | 
  425 | 	/**
  426 | 	 * Returns a mutable set of the patterns that match jars to ignore for TLD scanning.
  427 | 	 * @return the set of jars to ignore for TLD scanning
  428 | 	 */
  429 | 	public Set<String> getTldSkipPatterns() {
  430 | 		return this.tldSkipPatterns;
  431 | 	}
  432 | 
  433 | 	/**
  434 | 	 * Set the patterns that match jars to ignore for TLD scanning. See Tomcat's
  435 | 	 * catalina.properties for typical values. Defaults to a list drawn from that source.
  436 | 	 * @param patterns the jar patterns to skip when scanning for TLDs etc
  437 | 	 */
  438 | 	public void setTldSkipPatterns(Collection<String> patterns) {
  439 | 		Assert.notNull(patterns, "'patterns' must not be null");
  440 | 		this.tldSkipPatterns = new LinkedHashSet<>(patterns);
  441 | 	}
  442 | 
  443 | 	/**
  444 | 	 * Add patterns that match jars to ignore for TLD scanning. See Tomcat's
  445 | 	 * catalina.properties for typical values.
  446 | 	 * @param patterns the additional jar patterns to skip when scanning for TLDs etc
  447 | 	 */
  448 | 	public void addTldSkipPatterns(String... patterns) {
  449 | 		Assert.notNull(patterns, "'patterns' must not be null");
  450 | 		this.tldSkipPatterns.addAll(Arrays.asList(patterns));
  451 | 	}
  452 | 
  453 | 	@Override
  454 | 	public ServletWebServerSettings getSettings() {
  455 | 		return this.settings;
  456 | 	}
  457 | 
  458 | 	/**
  459 | 	 * {@link LifecycleListener} to disable persistence in the {@link StandardManager}. A
  460 | 	 * {@link LifecycleListener} is used so not to interfere with Tomcat's default manager
  461 | 	 * creation logic.
  462 | 	 */
  463 | 	private static final class DisablePersistSessionListener implements LifecycleListener {
  464 | 
  465 | 		@Override
  466 | 		public void lifecycleEvent(LifecycleEvent event) {
  467 | 			if (event.getType().equals(Lifecycle.START_EVENT)) {
  468 | 				Context context = (Context) event.getLifecycle();
  469 | 				Manager manager = context.getManager();
  470 | 				if (manager instanceof StandardManager standardManager) {
  471 | 					standardManager.setPathname(null);
  472 | 				}
  473 | 			}
  474 | 		}
  475 | 
  476 | 	}
  477 | 
  478 | 	private final class StaticResourceConfigurer implements LifecycleListener {
  479 | 
  480 | 		private static final String WEB_APP_MOUNT = "/";
  481 | 
  482 | 		private static final String INTERNAL_PATH = "/META-INF/resources";
  483 | 
  484 | 		private final Context context;
  485 | 
  486 | 		private StaticResourceConfigurer(Context context) {
  487 | 			this.context = context;
  488 | 		}
  489 | 
  490 | 		@Override
  491 | 		public void lifecycleEvent(LifecycleEvent event) {
  492 | 			if (event.getType().equals(Lifecycle.BEFORE_INIT_EVENT)) {
  493 | 				addResourceJars(TomcatServletWebServerFactory.this.getSettings().getStaticResourceUrls());
  494 | 			}
  495 | 		}
  496 | 
  497 | 		private void addResourceJars(List<URL> resourceJarUrls) {
  498 | 			for (URL url : resourceJarUrls) {
  499 | 				String path = url.getPath();
  500 | 				if (path.endsWith(".jar") || path.endsWith(".jar!/")) {
  501 | 					String jar = url.toString();
  502 | 					if (!jar.startsWith("jar:")) {
  503 | 						// A jar file in the file system. Convert to Jar URL.
  504 | 						jar = "jar:" + jar + "!/";
  505 | 					}
  506 | 					addResourceSet(jar);
  507 | 				}
  508 | 				else {
  509 | 					addResourceSet(url.toString());
  510 | 				}
  511 | 			}
  512 | 			for (WebResourceSet resources : this.context.getResources().getJarResources()) {
  513 | 				resources.setReadOnly(true);
  514 | 			}
  515 | 		}
  516 | 
  517 | 		private void addResourceSet(String resource) {
  518 | 			try {
  519 | 				if (isInsideClassicNestedJar(resource)) {
  520 | 					addClassicNestedResourceSet(resource);
  521 | 					return;
  522 | 				}
  523 | 				WebResourceRoot root = this.context.getResources();
  524 | 				URL url = new URL(resource);
  525 | 				if (isInsideNestedJar(resource)) {
  526 | 					root.addJarResources(new NestedJarResourceSet(url, root, WEB_APP_MOUNT, INTERNAL_PATH));
  527 | 				}
  528 | 				else {
  529 | 					root.createWebResourceSet(ResourceSetType.RESOURCE_JAR, WEB_APP_MOUNT, url, INTERNAL_PATH);
  530 | 				}
  531 | 			}
  532 | 			catch (Exception ex) {
  533 | 				// Ignore (probably not a directory)
  534 | 			}
  535 | 		}
  536 | 
  537 | 		private void addClassicNestedResourceSet(String resource) throws MalformedURLException {
  538 | 			// It's a nested jar but we now don't want the suffix because Tomcat
  539 | 			// is going to try and locate it as a root URL (not the resource
  540 | 			// inside it)
  541 | 			URL url = new URL(resource.substring(0, resource.length() - 2));
  542 | 			this.context.getResources()
  543 | 				.createWebResourceSet(ResourceSetType.RESOURCE_JAR, WEB_APP_MOUNT, url, INTERNAL_PATH);
  544 | 		}
  545 | 
  546 | 		private boolean isInsideClassicNestedJar(String resource) {
  547 | 			return !isInsideNestedJar(resource) && resource.indexOf("!/") < resource.lastIndexOf("!/");
  548 | 		}
  549 | 
  550 | 		private boolean isInsideNestedJar(String resource) {
  551 | 			return resource.startsWith("jar:nested:");
  552 | 		}
  553 | 
  554 | 	}
  555 | 
  556 | 	private static final class LoaderHidingResourceRoot extends StandardRoot {
  557 | 
  558 | 		private LoaderHidingResourceRoot(TomcatEmbeddedContext context) {
  559 | 			super(context);
  560 | 		}
  561 | 
  562 | 		@Override
  563 | 		protected WebResourceSet createMainResourceSet() {
  564 | 			return new LoaderHidingWebResourceSet(super.createMainResourceSet());
  565 | 		}
  566 | 
  567 | 	}
  568 | 
  569 | 	private static final class LoaderHidingWebResourceSet extends AbstractResourceSet {
  570 | 
  571 | 		private final WebResourceSet delegate;
  572 | 
  573 | 		private final Method initInternal;
  574 | 
  575 | 		private LoaderHidingWebResourceSet(WebResourceSet delegate) {
  576 | 			this.delegate = delegate;
  577 | 			try {
  578 | 				this.initInternal = LifecycleBase.class.getDeclaredMethod("initInternal");
  579 | 				this.initInternal.setAccessible(true);
  580 | 			}
  581 | 			catch (Exception ex) {
  582 | 				throw new IllegalStateException(ex);
  583 | 			}
  584 | 		}
  585 | 
  586 | 		@Override
  587 | 		public WebResource getResource(String path) {
  588 | 			if (path.startsWith("/org/springframework/boot")) {
  589 | 				return new EmptyResource(getRoot(), path);
  590 | 			}
  591 | 			return this.delegate.getResource(path);
  592 | 		}
  593 | 
  594 | 		@Override
  595 | 		public String[] list(String path) {
  596 | 			return this.delegate.list(path);
  597 | 		}
  598 | 
  599 | 		@Override
  600 | 		public Set<String> listWebAppPaths(String path) {
  601 | 			return this.delegate.listWebAppPaths(path)
  602 | 				.stream()
  603 | 				.filter((webAppPath) -> !webAppPath.startsWith("/org/springframework/boot"))
  604 | 				.collect(Collectors.toSet());
  605 | 		}
  606 | 
  607 | 		@Override
  608 | 		public boolean mkdir(String path) {
  609 | 			return this.delegate.mkdir(path);
  610 | 		}
  611 | 
  612 | 		@Override
  613 | 		public boolean write(String path, InputStream is, boolean overwrite) {
  614 | 			return this.delegate.write(path, is, overwrite);
  615 | 		}
  616 | 
  617 | 		@Override
  618 | 		public URL getBaseUrl() {
  619 | 			return this.delegate.getBaseUrl();
  620 | 		}
  621 | 
  622 | 		@Override
  623 | 		public void setReadOnly(boolean readOnly) {
  624 | 			this.delegate.setReadOnly(readOnly);
  625 | 		}
  626 | 
  627 | 		@Override
  628 | 		public boolean isReadOnly() {
  629 | 			return this.delegate.isReadOnly();
  630 | 		}
  631 | 
  632 | 		@Override
  633 | 		public void gc() {
  634 | 			this.delegate.gc();
  635 | 		}
  636 | 
  637 | 		@Override
  638 | 		public void setAllowLinking(boolean allowLinking) {
  639 | 			this.delegate.setAllowLinking(allowLinking);
  640 | 		}
  641 | 
  642 | 		@Override
  643 | 		public boolean getAllowLinking() {
  644 | 			return this.delegate.getAllowLinking();
  645 | 		}
  646 | 
  647 | 		@Override
  648 | 		protected void initInternal() throws LifecycleException {
  649 | 			if (this.delegate instanceof LifecycleBase) {
  650 | 				try {
  651 | 					ReflectionUtils.invokeMethod(this.initInternal, this.delegate);
  652 | 				}
  653 | 				catch (Exception ex) {
  654 | 					throw new LifecycleException(ex);
  655 | 				}
  656 | 			}
  657 | 		}
  658 | 
  659 | 	}
  660 | 
  661 | 	/**
  662 | 	 * {@link Rfc6265CookieProcessor} that supports {@link CookieSameSiteSupplier
  663 | 	 * supplied} {@link SameSite} values.
  664 | 	 */
  665 | 	private static class SuppliedSameSiteCookieProcessor extends Rfc6265CookieProcessor {
  666 | 
  667 | 		private final List<CookieSameSiteSupplier> suppliers;
  668 | 
  669 | 		SuppliedSameSiteCookieProcessor(List<CookieSameSiteSupplier> suppliers) {
  670 | 			this.suppliers = suppliers;
  671 | 		}
  672 | 
  673 | 		@Override
  674 | 		public String generateHeader(Cookie cookie, HttpServletRequest request) {
  675 | 			SameSite sameSite = getSameSite(cookie);
  676 | 			String sameSiteValue = (sameSite != null) ? sameSite.attributeValue() : null;
  677 | 			if (sameSiteValue == null) {
  678 | 				return super.generateHeader(cookie, request);
  679 | 			}
  680 | 			Rfc6265CookieProcessor delegate = new Rfc6265CookieProcessor();
  681 | 			delegate.setSameSiteCookies(sameSiteValue);
  682 | 			return delegate.generateHeader(cookie, request);
  683 | 		}
  684 | 
  685 | 		private @Nullable SameSite getSameSite(Cookie cookie) {
  686 | 			for (CookieSameSiteSupplier supplier : this.suppliers) {
  687 | 				SameSite sameSite = supplier.getSameSite(cookie);
  688 | 				if (sameSite != null) {
  689 | 					return sameSite;
  690 | 				}
  691 | 			}
  692 | 			return null;
  693 | 		}
  694 | 
  695 | 	}
  696 | 
  697 | }
```
