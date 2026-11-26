<!-- learning-docs source archive（机械抓取原文，禁止手改代码内容） -->
<!-- origin: org.springframework.boot:spring-boot:4.2.0 | path: org/springframework/boot/web/servlet/support/SpringBootServletInitializer.java -->
<!-- 正文引用：[[源码:SpringBootServletInitializer.java#L起-L止]] ；代码块头部：(src: SpringBootServletInitializer.java#L起-L止) -->

# SpringBootServletInitializer.java

| 项 | 值 |
| --- | --- |
| 来源 | `org.springframework.boot:spring-boot:4.2.0` |
| 路径 | `org/springframework/boot/web/servlet/support/SpringBootServletInitializer.java` |
| 行数 | 319 |
| 在线地址 | https://repo1.maven.org/maven2/org/springframework/boot/spring-boot/4.2.0/spring-boot-4.2.0-sources.jar |
| 抓取时间 | 2026-11-25T14:02:49.292Z |

```java
    1 | /*
    2 |  * Copyright 2012-present the original author or authors.
    3 |  */
    4 | 
    5 | package org.springframework.boot.web.servlet.support;
    6 | 
    7 | import java.sql.Driver;
    8 | import java.sql.DriverManager;
    9 | import java.sql.SQLException;
   10 | import java.util.Collections;
   11 | 
   12 | import jakarta.servlet.Filter;
   13 | import jakarta.servlet.Servlet;
   14 | import jakarta.servlet.ServletContext;
   15 | import jakarta.servlet.ServletContextEvent;
   16 | import jakarta.servlet.ServletException;
   17 | import org.apache.commons.logging.Log;
   18 | import org.apache.commons.logging.LogFactory;
   19 | import org.jspecify.annotations.Nullable;
   20 | import reactor.core.scheduler.Schedulers;
   21 | 
   22 | import org.springframework.boot.ApplicationContextFactory;
   23 | import org.springframework.boot.SpringApplication;
   24 | import org.springframework.boot.WebApplicationType;
   25 | import org.springframework.boot.builder.ParentContextApplicationContextInitializer;
   26 | import org.springframework.boot.builder.SpringApplicationBuilder;
   27 | import org.springframework.boot.context.event.ApplicationEnvironmentPreparedEvent;
   28 | import org.springframework.boot.context.logging.LoggingApplicationListener;
   29 | import org.springframework.boot.web.context.servlet.AnnotationConfigServletWebApplicationContext;
   30 | import org.springframework.boot.web.context.servlet.ApplicationServletEnvironment;
   31 | import org.springframework.boot.web.context.servlet.WebApplicationContextInitializer;
   32 | import org.springframework.boot.web.servlet.ServletContextInitializer;
   33 | import org.springframework.context.ApplicationContext;
   34 | import org.springframework.context.ApplicationContextException;
   35 | import org.springframework.context.ApplicationListener;
   36 | import org.springframework.context.ConfigurableApplicationContext;
   37 | import org.springframework.context.annotation.Configuration;
   38 | import org.springframework.core.Ordered;
   39 | import org.springframework.core.annotation.MergedAnnotations;
   40 | import org.springframework.core.annotation.MergedAnnotations.SearchStrategy;
   41 | import org.springframework.core.env.ConfigurableEnvironment;
   42 | import org.springframework.util.Assert;
   43 | import org.springframework.util.ClassUtils;
   44 | import org.springframework.web.WebApplicationInitializer;
   45 | import org.springframework.web.context.ConfigurableWebEnvironment;
   46 | import org.springframework.web.context.ContextLoaderListener;
   47 | import org.springframework.web.context.WebApplicationContext;
   48 | 
   49 | /**
   50 |  * An opinionated {@link WebApplicationInitializer} to run a {@link SpringApplication}
   51 |  * from a traditional WAR deployment. Binds {@link Servlet}, {@link Filter} and
   52 |  * {@link ServletContextInitializer} beans from the application context to the server.
   53 |  * <p>
   54 |  * To configure the application either override the
   55 |  * {@link #configure(SpringApplicationBuilder)} method (calling
   56 |  * {@link SpringApplicationBuilder#sources(Class...)}) or make the initializer itself a
   57 |  * {@code @Configuration}. If you are using {@link SpringBootServletInitializer} in
   58 |  * combination with other {@link WebApplicationInitializer WebApplicationInitializers} you
   59 |  * might also want to add an {@code @Ordered} annotation to configure a specific startup
   60 |  * order.
   61 |  * <p>
   62 |  * Note that a WebApplicationInitializer is only needed if you are building a war file and
   63 |  * deploying it. If you prefer to run an embedded web server then you won't need this at
   64 |  * all.
   65 |  *
   66 |  * @author Dave Syer
   67 |  * @author Phillip Webb
   68 |  * @author Andy Wilkinson
   69 |  * @author Brian Clozel
   70 |  * @since 2.0.0
   71 |  * @see #configure(SpringApplicationBuilder)
   72 |  */
   73 | public abstract class SpringBootServletInitializer implements WebApplicationInitializer {
   74 | 
   75 | 	private static final boolean REACTOR_PRESENT = ClassUtils.isPresent("reactor.core.scheduler.Schedulers",
   76 | 			SpringBootServletInitializer.class.getClassLoader());
   77 | 
   78 | 	protected @Nullable Log logger; // Don't initialize early
   79 | 
   80 | 	private boolean registerErrorPageFilter = true;
   81 | 
   82 | 	/**
   83 | 	 * Set if the {@link ErrorPageFilter} should be registered. Set to {@code false} if
   84 | 	 * error page mappings should be handled through the server and not Spring Boot.
   85 | 	 * @param registerErrorPageFilter if the {@link ErrorPageFilter} should be registered.
   86 | 	 */
   87 | 	protected final void setRegisterErrorPageFilter(boolean registerErrorPageFilter) {
   88 | 		this.registerErrorPageFilter = registerErrorPageFilter;
   89 | 	}
   90 | 
   91 | 	@Override
   92 | 	public void onStartup(ServletContext servletContext) throws ServletException {
   93 | 		servletContext.setAttribute(LoggingApplicationListener.REGISTER_SHUTDOWN_HOOK_PROPERTY, false);
   94 | 		// Logger initialization is deferred in case an ordered
   95 | 		// LogServletContextInitializer is being used
   96 | 		this.logger = LogFactory.getLog(getClass());
   97 | 		WebApplicationContext rootApplicationContext = createRootApplicationContext(servletContext);
   98 | 		if (rootApplicationContext != null) {
   99 | 			servletContext.addListener(new SpringBootContextLoaderListener(rootApplicationContext, servletContext));
  100 | 		}
  101 | 		else {
  102 | 			this.logger.debug("No ContextLoaderListener registered, as createRootApplicationContext() did not "
  103 | 					+ "return an application context");
  104 | 		}
  105 | 	}
  106 | 
  107 | 	/**
  108 | 	 * Deregisters the JDBC drivers that were registered by the application represented by
  109 | 	 * the given {@code servletContext}. The default implementation
  110 | 	 * {@link DriverManager#deregisterDriver(Driver) deregisters} every {@link Driver}
  111 | 	 * that was loaded by the {@link ServletContext#getClassLoader web application's class
  112 | 	 * loader}.
  113 | 	 * @param servletContext the web application's servlet context
  114 | 	 * @since 2.3.0
  115 | 	 */
  116 | 	protected void deregisterJdbcDrivers(ServletContext servletContext) {
  117 | 		for (Driver driver : Collections.list(DriverManager.getDrivers())) {
  118 | 			if (driver.getClass().getClassLoader() == servletContext.getClassLoader()) {
  119 | 				try {
  120 | 					DriverManager.deregisterDriver(driver);
  121 | 				}
  122 | 				catch (SQLException ex) {
  123 | 					// Continue
  124 | 				}
  125 | 			}
  126 | 		}
  127 | 	}
  128 | 
  129 | 	/**
  130 | 	 * Shuts down the reactor {@link Schedulers} that were initialized by
  131 | 	 * {@code Schedulers.boundedElastic()} (or similar). The default implementation
  132 | 	 * {@link Schedulers#shutdownNow()} schedulers if they were initialized on this web
  133 | 	 * application's class loader.
  134 | 	 * @param servletContext the web application's servlet context
  135 | 	 * @since 3.4.0
  136 | 	 */
  137 | 	protected void shutDownSharedReactorSchedulers(ServletContext servletContext) {
  138 | 		if (Schedulers.class.getClassLoader() == servletContext.getClassLoader()) {
  139 | 			Schedulers.shutdownNow();
  140 | 		}
  141 | 	}
  142 | 
  143 | 	protected @Nullable WebApplicationContext createRootApplicationContext(ServletContext servletContext) {
  144 | 		SpringApplicationBuilder builder = createSpringApplicationBuilder();
  145 | 		builder.main(getClass());
  146 | 		ApplicationContext parent = getExistingRootWebApplicationContext(servletContext);
  147 | 		if (parent != null) {
  148 | 			getLogger().info("Root context already created (using as parent).");
  149 | 			servletContext.setAttribute(WebApplicationContext.ROOT_WEB_APPLICATION_CONTEXT_ATTRIBUTE, null);
  150 | 			builder.initializers(new ParentContextApplicationContextInitializer(parent));
  151 | 		}
  152 | 		builder.initializers(new ServletContextApplicationContextInitializer(servletContext));
  153 | 		builder.contextFactory(new WarDeploymentApplicationContextFactory(servletContext));
  154 | 		builder = configure(builder);
  155 | 		builder.listeners(new WebEnvironmentPropertySourceInitializer(servletContext));
  156 | 		SpringApplication application = builder.build();
  157 | 		if (application.getAllSources().isEmpty()
  158 | 				&& MergedAnnotations.from(getClass(), SearchStrategy.TYPE_HIERARCHY).isPresent(Configuration.class)) {
  159 | 			application.addPrimarySources(Collections.singleton(getClass()));
  160 | 		}
  161 | 		Assert.state(!application.getAllSources().isEmpty(),
  162 | 				"No SpringApplication sources have been defined. Either override the "
  163 | 						+ "configure method or add an @Configuration annotation");
  164 | 		// Ensure error pages are registered
  165 | 		if (this.registerErrorPageFilter) {
  166 | 			application.addPrimarySources(Collections.singleton(ErrorPageFilterConfiguration.class));
  167 | 		}
  168 | 		application.setRegisterShutdownHook(false);
  169 | 		return run(application);
  170 | 	}
  171 | 
  172 | 	private Log getLogger() {
  173 | 		Assert.state(this.logger != null, "Logger not set");
  174 | 		return this.logger;
  175 | 	}
  176 | 
  177 | 	/**
  178 | 	 * Returns the {@code SpringApplicationBuilder} that is used to configure and create
  179 | 	 * the {@link SpringApplication}. The default implementation returns a new
  180 | 	 * {@code SpringApplicationBuilder} in its default state.
  181 | 	 * @return the {@code SpringApplicationBuilder}.
  182 | 	 */
  183 | 	protected SpringApplicationBuilder createSpringApplicationBuilder() {
  184 | 		return new SpringApplicationBuilder();
  185 | 	}
  186 | 
  187 | 	/**
  188 | 	 * Called to run a fully configured {@link SpringApplication}.
  189 | 	 * @param application the application to run
  190 | 	 * @return the {@link WebApplicationContext}
  191 | 	 */
  192 | 	protected @Nullable WebApplicationContext run(SpringApplication application) {
  193 | 		return (WebApplicationContext) application.run();
  194 | 	}
  195 | 
  196 | 	private @Nullable ApplicationContext getExistingRootWebApplicationContext(ServletContext servletContext) {
  197 | 		Object context = servletContext.getAttribute(WebApplicationContext.ROOT_WEB_APPLICATION_CONTEXT_ATTRIBUTE);
  198 | 		if (context instanceof ApplicationContext applicationContext) {
  199 | 			return applicationContext;
  200 | 		}
  201 | 		return null;
  202 | 	}
  203 | 
  204 | 	/**
  205 | 	 * Configure the application. Normally all you would need to do is to add sources
  206 | 	 * (e.g. config classes) because other settings have sensible defaults. You might
  207 | 	 * choose (for instance) to add default command line arguments, or set an active
  208 | 	 * Spring profile.
  209 | 	 * @param builder a builder for the application context
  210 | 	 * @return the application builder
  211 | 	 * @see SpringApplicationBuilder
  212 | 	 */
  213 | 	protected SpringApplicationBuilder configure(SpringApplicationBuilder builder) {
  214 | 		return builder;
  215 | 	}
  216 | 
  217 | 	/**
  218 | 	 * {@link ApplicationListener} to trigger
  219 | 	 * {@link ConfigurableWebEnvironment#initPropertySources(ServletContext, jakarta.servlet.ServletConfig)}.
  220 | 	 */
  221 | 	private static final class WebEnvironmentPropertySourceInitializer
  222 | 			implements ApplicationListener<ApplicationEnvironmentPreparedEvent>, Ordered {
  223 | 
  224 | 		private final ServletContext servletContext;
  225 | 
  226 | 		private WebEnvironmentPropertySourceInitializer(ServletContext servletContext) {
  227 | 			this.servletContext = servletContext;
  228 | 		}
  229 | 
  230 | 		@Override
  231 | 		public void onApplicationEvent(ApplicationEnvironmentPreparedEvent event) {
  232 | 			ConfigurableEnvironment environment = event.getEnvironment();
  233 | 			if (environment instanceof ConfigurableWebEnvironment configurableWebEnvironment) {
  234 | 				configurableWebEnvironment.initPropertySources(this.servletContext, null);
  235 | 			}
  236 | 		}
  237 | 
  238 | 		@Override
  239 | 		public int getOrder() {
  240 | 			return Ordered.HIGHEST_PRECEDENCE;
  241 | 		}
  242 | 
  243 | 	}
  244 | 
  245 | 	/**
  246 | 	 * {@link ContextLoaderListener} for the initialized context.
  247 | 	 */
  248 | 	private class SpringBootContextLoaderListener extends ContextLoaderListener {
  249 | 
  250 | 		private final ServletContext servletContext;
  251 | 
  252 | 		SpringBootContextLoaderListener(WebApplicationContext applicationContext, ServletContext servletContext) {
  253 | 			super(applicationContext);
  254 | 			this.servletContext = servletContext;
  255 | 		}
  256 | 
  257 | 		@Override
  258 | 		public void contextInitialized(ServletContextEvent event) {
  259 | 			// no-op because the application context is already initialized
  260 | 		}
  261 | 
  262 | 		@Override
  263 | 		public void contextDestroyed(ServletContextEvent event) {
  264 | 			try {
  265 | 				super.contextDestroyed(event);
  266 | 			}
  267 | 			finally {
  268 | 				// Use original context so that the classloader can be accessed
  269 | 				deregisterJdbcDrivers(this.servletContext);
  270 | 				// Shut down shared reactor schedulers tied to this classloader
  271 | 				if (REACTOR_PRESENT) {
  272 | 					shutDownSharedReactorSchedulers(this.servletContext);
  273 | 				}
  274 | 			}
  275 | 		}
  276 | 
  277 | 	}
  278 | 
  279 | 	private static final class WarDeploymentApplicationContextFactory implements ApplicationContextFactory {
  280 | 
  281 | 		private final ServletContext servletContext;
  282 | 
  283 | 		private WarDeploymentApplicationContextFactory(ServletContext servletContext) {
  284 | 			this.servletContext = servletContext;
  285 | 		}
  286 | 
  287 | 		@Override
  288 | 		public @Nullable Class<? extends ConfigurableEnvironment> getEnvironmentType(
  289 | 				@Nullable WebApplicationType webApplicationType) {
  290 | 			return (webApplicationType != WebApplicationType.SERVLET) ? null : ApplicationServletEnvironment.class;
  291 | 		}
  292 | 
  293 | 		@Override
  294 | 		public ConfigurableEnvironment createEnvironment(@Nullable WebApplicationType webApplicationType) {
  295 | 			return new ApplicationServletEnvironment();
  296 | 		}
  297 | 
  298 | 		@Override
  299 | 		public ConfigurableApplicationContext create(@Nullable WebApplicationType webApplicationType) {
  300 | 			return new AnnotationConfigServletWebApplicationContext() {
  301 | 
  302 | 				@Override
  303 | 				protected void onRefresh() {
  304 | 					super.onRefresh();
  305 | 					try {
  306 | 						new WebApplicationContextInitializer(this)
  307 | 							.initialize(WarDeploymentApplicationContextFactory.this.servletContext);
  308 | 					}
  309 | 					catch (ServletException ex) {
  310 | 						throw new ApplicationContextException("Cannot initialize servlet context", ex);
  311 | 					}
  312 | 				}
  313 | 
  314 | 			};
  315 | 		}
  316 | 
  317 | 	}
  318 | 
  319 | }
```
