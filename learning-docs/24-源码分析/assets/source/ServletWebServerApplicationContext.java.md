<!-- learning-docs source archive（机械抓取原文，禁止手改代码内容） -->
<!-- origin: org.springframework.boot:spring-boot-web-server:4.2.0 | path: org/springframework/boot/web/server/servlet/context/ServletWebServerApplicationContext.java -->
<!-- 正文引用：[[源码:ServletWebServerApplicationContext.java#L起-L止]] ；代码块头部：(src: ServletWebServerApplicationContext.java#L起-L止) -->

# ServletWebServerApplicationContext.java

| 项 | 值 |
| --- | --- |
| 来源 | `org.springframework.boot:spring-boot-web-server:4.2.0` |
| 路径 | `org/springframework/boot/web/server/servlet/context/ServletWebServerApplicationContext.java` |
| 行数 | 311 |
| 在线地址 | https://repo1.maven.org/maven2/org/springframework/boot/spring-boot-web-server/4.2.0/spring-boot-web-server-4.2.0-sources.jar |
| 抓取时间 | 2026-11-25T14:00:54.295Z |

```java
    1 | /*
    2 |  * Copyright 2012-present the original author or authors.
    3 |  */
    4 | 
    5 | package org.springframework.boot.web.server.servlet.context;
    6 | 
    7 | import java.util.Collections;
    8 | import java.util.HashMap;
    9 | import java.util.LinkedHashSet;
   10 | import java.util.Map;
   11 | import java.util.Set;
   12 | 
   13 | import jakarta.servlet.Filter;
   14 | import jakarta.servlet.Servlet;
   15 | import jakarta.servlet.ServletConfig;
   16 | import jakarta.servlet.ServletContext;
   17 | import jakarta.servlet.ServletException;
   18 | import org.apache.commons.logging.Log;
   19 | import org.apache.commons.logging.LogFactory;
   20 | import org.jspecify.annotations.Nullable;
   21 | 
   22 | import org.springframework.beans.BeansException;
   23 | import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;
   24 | import org.springframework.beans.factory.config.Scope;
   25 | import org.springframework.beans.factory.support.DefaultListableBeanFactory;
   26 | import org.springframework.boot.WebApplicationType;
   27 | import org.springframework.boot.availability.AvailabilityChangeEvent;
   28 | import org.springframework.boot.availability.ReadinessState;
   29 | import org.springframework.boot.web.context.servlet.WebApplicationContextInitializer;
   30 | import org.springframework.boot.web.server.WebServer;
   31 | import org.springframework.boot.web.server.context.ConfigurableWebServerApplicationContext;
   32 | import org.springframework.boot.web.server.context.MissingWebServerFactoryBeanException;
   33 | import org.springframework.boot.web.server.context.WebServerGracefulShutdownLifecycle;
   34 | import org.springframework.boot.web.server.servlet.ServletWebServerFactory;
   35 | import org.springframework.boot.web.servlet.FilterRegistrationBean;
   36 | import org.springframework.boot.web.servlet.ServletContextInitializer;
   37 | import org.springframework.boot.web.servlet.ServletRegistrationBean;
   38 | import org.springframework.context.ApplicationContext;
   39 | import org.springframework.context.ApplicationContextException;
   40 | import org.springframework.core.io.Resource;
   41 | import org.springframework.core.metrics.StartupStep;
   42 | import org.springframework.util.StringUtils;
   43 | import org.springframework.web.context.ServletContextAware;
   44 | import org.springframework.web.context.WebApplicationContext;
   45 | import org.springframework.web.context.support.GenericWebApplicationContext;
   46 | import org.springframework.web.context.support.ServletContextAwareProcessor;
   47 | import org.springframework.web.context.support.ServletContextResource;
   48 | import org.springframework.web.context.support.WebApplicationContextUtils;
   49 | 
   50 | /**
   51 |  * A {@link WebApplicationContext} that can be used to bootstrap itself from a contained
   52 |  * {@link ServletWebServerFactory} bean.
   53 |  * <p>
   54 |  * This context will create, initialize and run an {@link WebServer} by searching for a
   55 |  * single {@link ServletWebServerFactory} bean within the {@link ApplicationContext}
   56 |  * itself. The {@link ServletWebServerFactory} is free to use standard Spring concepts
   57 |  * (such as dependency injection, lifecycle callbacks and property placeholder variables).
   58 |  * <p>
   59 |  * In addition, any {@link Servlet} or {@link Filter} beans defined in the context will be
   60 |  * automatically registered with the web server. In the case of a single Servlet bean, the
   61 |  * '/' mapping will be used. If multiple Servlet beans are found then the lowercase bean
   62 |  * name will be used as a mapping prefix. Any Servlet named 'dispatcherServlet' will
   63 |  * always be mapped to '/'. Filter beans will be mapped to all URLs ('/*').
   64 |  * <p>
   65 |  * For more advanced configuration, the context can instead define beans that implement
   66 |  * the {@link ServletContextInitializer} interface (most often
   67 |  * {@link ServletRegistrationBean}s and/or {@link FilterRegistrationBean}s). To prevent
   68 |  * double registration, the use of {@link ServletContextInitializer} beans will disable
   69 |  * automatic Servlet and Filter bean registration.
   70 |  * <p>
   71 |  * Although this context can be used directly, most developers should consider using the
   72 |  * {@link AnnotationConfigServletWebServerApplicationContext} or
   73 |  * {@link XmlServletWebServerApplicationContext} variants.
   74 |  *
   75 |  * @author Phillip Webb
   76 |  * @author Dave Syer
   77 |  * @author Scott Frederick
   78 |  * @since 4.0.0
   79 |  * @see AnnotationConfigServletWebServerApplicationContext
   80 |  * @see XmlServletWebServerApplicationContext
   81 |  * @see ServletWebServerFactory
   82 |  */
   83 | public class ServletWebServerApplicationContext extends GenericWebApplicationContext
   84 | 		implements ConfigurableWebServerApplicationContext {
   85 | 
   86 | 	private static final Log logger = LogFactory.getLog(ServletWebServerApplicationContext.class);
   87 | 
   88 | 	/**
   89 | 	 * Constant value for the DispatcherServlet bean name. A Servlet bean with this name
   90 | 	 * is deemed to be the "main" servlet and is automatically given a mapping of "/" by
   91 | 	 * default. To change the default behavior you can use a
   92 | 	 * {@link ServletRegistrationBean} or a different bean name.
   93 | 	 */
   94 | 	public static final String DISPATCHER_SERVLET_NAME = "dispatcherServlet";
   95 | 
   96 | 	private volatile @Nullable WebServer webServer;
   97 | 
   98 | 	private @Nullable ServletConfig servletConfig;
   99 | 
  100 | 	private @Nullable String serverNamespace;
  101 | 
  102 | 	/**
  103 | 	 * Create a new {@link ServletWebServerApplicationContext}.
  104 | 	 */
  105 | 	public ServletWebServerApplicationContext() {
  106 | 	}
  107 | 
  108 | 	/**
  109 | 	 * Create a new {@link ServletWebServerApplicationContext} with the given
  110 | 	 * {@code DefaultListableBeanFactory}.
  111 | 	 * @param beanFactory the DefaultListableBeanFactory instance to use for this context
  112 | 	 */
  113 | 	public ServletWebServerApplicationContext(DefaultListableBeanFactory beanFactory) {
  114 | 		super(beanFactory);
  115 | 	}
  116 | 
  117 | 	/**
  118 | 	 * Register ServletContextAwareProcessor.
  119 | 	 * @see ServletContextAwareProcessor
  120 | 	 */
  121 | 	@Override
  122 | 	protected void postProcessBeanFactory(ConfigurableListableBeanFactory beanFactory) {
  123 | 		beanFactory.addBeanPostProcessor(new WebApplicationContextServletContextAwareProcessor(this));
  124 | 		beanFactory.ignoreDependencyInterface(ServletContextAware.class);
  125 | 		registerWebApplicationScopes();
  126 | 	}
  127 | 
  128 | 	@Override
  129 | 	public final void refresh() throws BeansException, IllegalStateException {
  130 | 		try {
  131 | 			super.refresh();
  132 | 		}
  133 | 		catch (RuntimeException ex) {
  134 | 			WebServer webServer = this.webServer;
  135 | 			if (webServer != null) {
  136 | 				try {
  137 | 					webServer.stop();
  138 | 					webServer.destroy();
  139 | 				}
  140 | 				catch (RuntimeException stopOrDestroyEx) {
  141 | 					ex.addSuppressed(stopOrDestroyEx);
  142 | 				}
  143 | 			}
  144 | 			throw ex;
  145 | 		}
  146 | 	}
  147 | 
  148 | 	@Override
  149 | 	protected void onRefresh() {
  150 | 		super.onRefresh();
  151 | 		try {
  152 | 			createWebServer();
  153 | 		}
  154 | 		catch (Throwable ex) {
  155 | 			throw new ApplicationContextException("Unable to start web server", ex);
  156 | 		}
  157 | 	}
  158 | 
  159 | 	@Override
  160 | 	protected void doClose() {
  161 | 		if (isActive()) {
  162 | 			AvailabilityChangeEvent.publish(this, ReadinessState.REFUSING_TRAFFIC);
  163 | 		}
  164 | 		super.doClose();
  165 | 		WebServer webServer = this.webServer;
  166 | 		if (webServer != null) {
  167 | 			webServer.destroy();
  168 | 		}
  169 | 	}
  170 | 
  171 | 	private void createWebServer() {
  172 | 		WebServer webServer = this.webServer;
  173 | 		ServletContext servletContext = getServletContext();
  174 | 		if (webServer == null && servletContext == null) {
  175 | 			StartupStep createWebServer = getApplicationStartup().start("spring.boot.webserver.create");
  176 | 			ServletWebServerFactory factory = getWebServerFactory();
  177 | 			createWebServer.tag("factory", factory.getClass().toString());
  178 | 			webServer = factory.getWebServer(getSelfInitializer());
  179 | 			this.webServer = webServer;
  180 | 			createWebServer.end();
  181 | 			getBeanFactory().registerSingleton("webServerGracefulShutdown",
  182 | 					new WebServerGracefulShutdownLifecycle(webServer));
  183 | 			getBeanFactory().registerSingleton("webServerStartStop", new WebServerStartStopLifecycle(this, webServer));
  184 | 		}
  185 | 		else if (servletContext != null) {
  186 | 			try {
  187 | 				getSelfInitializer().onStartup(servletContext);
  188 | 			}
  189 | 			catch (ServletException ex) {
  190 | 				throw new ApplicationContextException("Cannot initialize servlet context", ex);
  191 | 			}
  192 | 		}
  193 | 		initPropertySources();
  194 | 	}
  195 | 
  196 | 	/**
  197 | 	 * Returns the {@link ServletWebServerFactory} that should be used to create the
  198 | 	 * embedded {@link WebServer}. By default this method searches for a suitable bean in
  199 | 	 * the context itself.
  200 | 	 * @return a {@link ServletWebServerFactory} (never {@code null})
  201 | 	 */
  202 | 	protected ServletWebServerFactory getWebServerFactory() {
  203 | 		// Use bean names so that we don't consider the hierarchy
  204 | 		String[] beanNames = getBeanFactory().getBeanNamesForType(ServletWebServerFactory.class);
  205 | 		if (beanNames.length == 0) {
  206 | 			throw new MissingWebServerFactoryBeanException(getClass(), ServletWebServerFactory.class,
  207 | 					WebApplicationType.SERVLET);
  208 | 		}
  209 | 		if (beanNames.length > 1) {
  210 | 			throw new ApplicationContextException("Unable to start ServletWebServerApplicationContext due to multiple "
  211 | 					+ "ServletWebServerFactory beans : " + StringUtils.arrayToCommaDelimitedString(beanNames));
  212 | 		}
  213 | 		return getBeanFactory().getBean(beanNames[0], ServletWebServerFactory.class);
  214 | 	}
  215 | 
  216 | 	/**
  217 | 	 * Returns the {@link ServletContextInitializer} that will be used to complete the
  218 | 	 * setup of this {@link WebApplicationContext}.
  219 | 	 * @return the self initializer
  220 | 	 * @see #prepareWebApplicationContext(ServletContext)
  221 | 	 */
  222 | 	private org.springframework.boot.web.servlet.ServletContextInitializer getSelfInitializer() {
  223 | 		return new WebApplicationContextInitializer(this)::initialize;
  224 | 	}
  225 | 
  226 | 	private void registerWebApplicationScopes() {
  227 | 		ExistingWebApplicationScopes existingScopes = new ExistingWebApplicationScopes(getBeanFactory());
  228 | 		WebApplicationContextUtils.registerWebApplicationScopes(getBeanFactory());
  229 | 		existingScopes.restore();
  230 | 	}
  231 | 
  232 | 	@Override
  233 | 	protected Resource getResourceByPath(String path) {
  234 | 		if (getServletContext() == null) {
  235 | 			return new ClassPathContextResource(path, getClassLoader());
  236 | 		}
  237 | 		return new ServletContextResource(getServletContext(), path);
  238 | 	}
  239 | 
  240 | 	@Override
  241 | 	public @Nullable String getServerNamespace() {
  242 | 		return this.serverNamespace;
  243 | 	}
  244 | 
  245 | 	@Override
  246 | 	public void setServerNamespace(@Nullable String serverNamespace) {
  247 | 		this.serverNamespace = serverNamespace;
  248 | 	}
  249 | 
  250 | 	@Override
  251 | 	public void setServletConfig(@Nullable ServletConfig servletConfig) {
  252 | 		this.servletConfig = servletConfig;
  253 | 	}
  254 | 
  255 | 	@Override
  256 | 	public @Nullable ServletConfig getServletConfig() {
  257 | 		return this.servletConfig;
  258 | 	}
  259 | 
  260 | 	/**
  261 | 	 * Returns the {@link WebServer} that was created by the context or {@code null} if
  262 | 	 * the server has not yet been created.
  263 | 	 * @return the embedded web server
  264 | 	 */
  265 | 	@Override
  266 | 	public @Nullable WebServer getWebServer() {
  267 | 		return this.webServer;
  268 | 	}
  269 | 
  270 | 	/**
  271 | 	 * Utility class to store and restore any user defined scopes. This allows scopes to
  272 | 	 * be registered in an ApplicationContextInitializer in the same way as they would in
  273 | 	 * a classic non-embedded web application context.
  274 | 	 */
  275 | 	public static class ExistingWebApplicationScopes {
  276 | 
  277 | 		private static final Set<String> SCOPES;
  278 | 
  279 | 		static {
  280 | 			Set<String> scopes = new LinkedHashSet<>();
  281 | 			scopes.add(WebApplicationContext.SCOPE_REQUEST);
  282 | 			scopes.add(WebApplicationContext.SCOPE_SESSION);
  283 | 			SCOPES = Collections.unmodifiableSet(scopes);
  284 | 		}
  285 | 
  286 | 		private final ConfigurableListableBeanFactory beanFactory;
  287 | 
  288 | 		private final Map<String, Scope> scopes = new HashMap<>();
  289 | 
  290 | 		public ExistingWebApplicationScopes(ConfigurableListableBeanFactory beanFactory) {
  291 | 			this.beanFactory = beanFactory;
  292 | 			for (String scopeName : SCOPES) {
  293 | 				Scope scope = beanFactory.getRegisteredScope(scopeName);
  294 | 				if (scope != null) {
  295 | 					this.scopes.put(scopeName, scope);
  296 | 				}
  297 | 			}
  298 | 		}
  299 | 
  300 | 		public void restore() {
  301 | 			this.scopes.forEach((key, value) -> {
  302 | 				if (logger.isInfoEnabled()) {
  303 | 					logger.info("Restoring user defined scope " + key);
  304 | 				}
  305 | 				this.beanFactory.registerScope(key, value);
  306 | 			});
  307 | 		}
  308 | 
  309 | 	}
  310 | 
  311 | }
```
