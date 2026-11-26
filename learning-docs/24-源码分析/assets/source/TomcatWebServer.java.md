<!-- learning-docs source archive（机械抓取原文，禁止手改代码内容） -->
<!-- origin: org.springframework.boot:spring-boot-tomcat:4.2.0 | path: org/springframework/boot/tomcat/TomcatWebServer.java -->
<!-- 正文引用：[[源码:TomcatWebServer.java#L起-L止]] ；代码块头部：(src: TomcatWebServer.java#L起-L止) -->

# TomcatWebServer.java

| 项 | 值 |
| --- | --- |
| 来源 | `org.springframework.boot:spring-boot-tomcat:4.2.0` |
| 路径 | `org/springframework/boot/tomcat/TomcatWebServer.java` |
| 行数 | 449 |
| 在线地址 | https://repo1.maven.org/maven2/org/springframework/boot/spring-boot-tomcat/4.2.0/spring-boot-tomcat-4.2.0-sources.jar |
| 抓取时间 | 2026-11-25T14:01:01.266Z |

```java
    1 | /*
    2 |  * Copyright 2012-present the original author or authors.
    3 |  */
    4 | 
    5 | package org.springframework.boot.tomcat;
    6 | 
    7 | import java.util.Arrays;
    8 | import java.util.HashMap;
    9 | import java.util.Map;
   10 | import java.util.concurrent.atomic.AtomicInteger;
   11 | import java.util.function.BiConsumer;
   12 | import java.util.stream.Collectors;
   13 | 
   14 | import javax.naming.NamingException;
   15 | 
   16 | import org.apache.catalina.Container;
   17 | import org.apache.catalina.Context;
   18 | import org.apache.catalina.Engine;
   19 | import org.apache.catalina.Lifecycle;
   20 | import org.apache.catalina.LifecycleException;
   21 | import org.apache.catalina.LifecycleState;
   22 | import org.apache.catalina.Service;
   23 | import org.apache.catalina.Wrapper;
   24 | import org.apache.catalina.connector.Connector;
   25 | import org.apache.catalina.startup.Tomcat;
   26 | import org.apache.commons.logging.Log;
   27 | import org.apache.commons.logging.LogFactory;
   28 | import org.apache.naming.ContextBindings;
   29 | import org.jspecify.annotations.Nullable;
   30 | 
   31 | import org.springframework.boot.tomcat.reactive.TomcatReactiveWebServerFactory;
   32 | import org.springframework.boot.tomcat.servlet.TomcatServletWebServerFactory;
   33 | import org.springframework.boot.web.server.GracefulShutdownCallback;
   34 | import org.springframework.boot.web.server.GracefulShutdownResult;
   35 | import org.springframework.boot.web.server.PortInUseException;
   36 | import org.springframework.boot.web.server.Shutdown;
   37 | import org.springframework.boot.web.server.WebServer;
   38 | import org.springframework.boot.web.server.WebServerException;
   39 | import org.springframework.util.Assert;
   40 | import org.springframework.util.StringUtils;
   41 | 
   42 | /**
   43 |  * {@link WebServer} that can be used to control a Tomcat web server. Usually this class
   44 |  * should be created using the {@link TomcatReactiveWebServerFactory} or
   45 |  * {@link TomcatServletWebServerFactory}, but not directly.
   46 |  *
   47 |  * @author Brian Clozel
   48 |  * @author Kristine Jetzke
   49 |  * @since 4.0.0
   50 |  */
   51 | public class TomcatWebServer implements WebServer {
   52 | 
   53 | 	private static final Log logger = LogFactory.getLog(TomcatWebServer.class);
   54 | 
   55 | 	private static final AtomicInteger containerCounter = new AtomicInteger(-1);
   56 | 
   57 | 	private final Object monitor = new Object();
   58 | 
   59 | 	private final Map<Service, Connector[]> serviceConnectors = new HashMap<>();
   60 | 
   61 | 	private final Tomcat tomcat;
   62 | 
   63 | 	private final boolean autoStart;
   64 | 
   65 | 	private final @Nullable GracefulShutdown gracefulShutdown;
   66 | 
   67 | 	private volatile boolean started;
   68 | 
   69 | 	/**
   70 | 	 * Create a new {@link TomcatWebServer} instance.
   71 | 	 * @param tomcat the underlying Tomcat server
   72 | 	 */
   73 | 	public TomcatWebServer(Tomcat tomcat) {
   74 | 		this(tomcat, true);
   75 | 	}
   76 | 
   77 | 	/**
   78 | 	 * Create a new {@link TomcatWebServer} instance.
   79 | 	 * @param tomcat the underlying Tomcat server
   80 | 	 * @param autoStart if the server should be started
   81 | 	 */
   82 | 	public TomcatWebServer(Tomcat tomcat, boolean autoStart) {
   83 | 		this(tomcat, autoStart, Shutdown.IMMEDIATE);
   84 | 	}
   85 | 
   86 | 	/**
   87 | 	 * Create a new {@link TomcatWebServer} instance.
   88 | 	 * @param tomcat the underlying Tomcat server
   89 | 	 * @param autoStart if the server should be started
   90 | 	 * @param shutdown type of shutdown supported by the server
   91 | 	 * @since 4.0.0
   92 | 	 */
   93 | 	public TomcatWebServer(Tomcat tomcat, boolean autoStart, Shutdown shutdown) {
   94 | 		Assert.notNull(tomcat, "'tomcat' must not be null");
   95 | 		this.tomcat = tomcat;
   96 | 		this.autoStart = autoStart;
   97 | 		this.gracefulShutdown = (shutdown == Shutdown.GRACEFUL) ? new GracefulShutdown(tomcat) : null;
   98 | 		initialize();
   99 | 	}
  100 | 
  101 | 	private void initialize() throws WebServerException {
  102 | 		logger.info("Tomcat initialized with " + getPortsDescription(false));
  103 | 		synchronized (this.monitor) {
  104 | 			try {
  105 | 				addInstanceIdToEngineName();
  106 | 
  107 | 				Context context = findContext();
  108 | 				context.addLifecycleListener((event) -> {
  109 | 					if (context.equals(event.getSource()) && Lifecycle.START_EVENT.equals(event.getType())) {
  110 | 						// Remove service connectors so that protocol binding doesn't
  111 | 						// happen when the service is started.
  112 | 						removeServiceConnectors();
  113 | 					}
  114 | 				});
  115 | 
  116 | 				disableBindOnInit();
  117 | 
  118 | 				// Start the server to trigger initialization listeners
  119 | 				this.tomcat.start();
  120 | 
  121 | 				// We can re-throw failure exception directly in the main thread
  122 | 				rethrowDeferredStartupExceptions();
  123 | 
  124 | 				try {
  125 | 					ContextBindings.bindClassLoader(context, context.getNamingToken(), getClass().getClassLoader());
  126 | 				}
  127 | 				catch (NamingException ex) {
  128 | 					// Naming is not enabled. Continue
  129 | 				}
  130 | 
  131 | 				// Unlike Jetty, all Tomcat threads are daemon threads. We create a
  132 | 				// blocking non-daemon to stop immediate shutdown
  133 | 				startNonDaemonAwaitThread();
  134 | 			}
  135 | 			catch (Exception ex) {
  136 | 				stopSilently();
  137 | 				destroySilently();
  138 | 				throw new WebServerException("Unable to start embedded Tomcat", ex);
  139 | 			}
  140 | 		}
  141 | 	}
  142 | 
  143 | 	private Context findContext() {
  144 | 		for (Container child : this.tomcat.getHost().findChildren()) {
  145 | 			if (child instanceof Context context) {
  146 | 				return context;
  147 | 			}
  148 | 		}
  149 | 		throw new IllegalStateException("The host does not contain a Context");
  150 | 	}
  151 | 
  152 | 	private void addInstanceIdToEngineName() {
  153 | 		int instanceId = containerCounter.incrementAndGet();
  154 | 		if (instanceId > 0) {
  155 | 			Engine engine = this.tomcat.getEngine();
  156 | 			engine.setName(engine.getName() + "-" + instanceId);
  157 | 		}
  158 | 	}
  159 | 
  160 | 	private void removeServiceConnectors() {
  161 | 		doWithConnectors((service, connectors) -> {
  162 | 			this.serviceConnectors.put(service, connectors);
  163 | 			for (Connector connector : connectors) {
  164 | 				service.removeConnector(connector);
  165 | 			}
  166 | 		});
  167 | 	}
  168 | 
  169 | 	private void disableBindOnInit() {
  170 | 		doWithConnectors((service, connectors) -> {
  171 | 			for (Connector connector : connectors) {
  172 | 				Object bindOnInit = connector.getProperty("bindOnInit");
  173 | 				if (bindOnInit == null) {
  174 | 					connector.setProperty("bindOnInit", "false");
  175 | 				}
  176 | 			}
  177 | 		});
  178 | 	}
  179 | 
  180 | 	private void doWithConnectors(BiConsumer<Service, Connector[]> consumer) {
  181 | 		for (Service service : this.tomcat.getServer().findServices()) {
  182 | 			Connector[] connectors = service.findConnectors().clone();
  183 | 			consumer.accept(service, connectors);
  184 | 		}
  185 | 	}
  186 | 
  187 | 	private void rethrowDeferredStartupExceptions() throws Exception {
  188 | 		Container[] children = this.tomcat.getHost().findChildren();
  189 | 		for (Container container : children) {
  190 | 			if (container instanceof TomcatEmbeddedContext embeddedContext) {
  191 | 				embeddedContext.getDeferredStartupExceptions().rethrow();
  192 | 			}
  193 | 			if (!LifecycleState.STARTED.equals(container.getState())) {
  194 | 				throw new IllegalStateException(container + " failed to start");
  195 | 			}
  196 | 		}
  197 | 	}
  198 | 
  199 | 	private void startNonDaemonAwaitThread() {
  200 | 		Thread awaitThread = new Thread("container-" + (containerCounter.get())) {
  201 | 
  202 | 			@Override
  203 | 			public void run() {
  204 | 				TomcatWebServer.this.tomcat.getServer().await();
  205 | 			}
  206 | 
  207 | 		};
  208 | 		awaitThread.setContextClassLoader(getClass().getClassLoader());
  209 | 		awaitThread.setDaemon(false);
  210 | 		awaitThread.start();
  211 | 	}
  212 | 
  213 | 	@Override
  214 | 	public void start() throws WebServerException {
  215 | 		synchronized (this.monitor) {
  216 | 			if (this.started) {
  217 | 				return;
  218 | 			}
  219 | 			if (this.gracefulShutdown != null) {
  220 | 				this.gracefulShutdown.reset();
  221 | 			}
  222 | 
  223 | 			try {
  224 | 				addPreviouslyRemovedConnectors();
  225 | 				Connector connector = this.tomcat.getConnector();
  226 | 				if (connector != null && this.autoStart) {
  227 | 					performDeferredLoadOnStartup();
  228 | 				}
  229 | 				checkThatConnectorsHaveStarted();
  230 | 				this.started = true;
  231 | 				logger.info(getStartedLogMessage());
  232 | 			}
  233 | 			catch (ConnectorStartFailedException ex) {
  234 | 				stopSilently();
  235 | 				throw ex;
  236 | 			}
  237 | 			catch (Exception ex) {
  238 | 				PortInUseException.throwIfPortBindingException(ex, () -> this.tomcat.getConnector().getPort());
  239 | 				throw new WebServerException("Unable to start embedded Tomcat server", ex);
  240 | 			}
  241 | 			finally {
  242 | 				Context context = findContext();
  243 | 				ContextBindings.unbindClassLoader(context, context.getNamingToken(), getClass().getClassLoader());
  244 | 			}
  245 | 		}
  246 | 	}
  247 | 
  248 | 	String getStartedLogMessage() {
  249 | 		String contextPath = getContextPath();
  250 | 		return "Tomcat started on " + getPortsDescription(true)
  251 | 				+ ((contextPath != null) ? " with context path '" + contextPath + "'" : "");
  252 | 	}
  253 | 
  254 | 	private void checkThatConnectorsHaveStarted() {
  255 | 		checkConnectorHasStarted(this.tomcat.getConnector());
  256 | 		for (Connector connector : this.tomcat.getService().findConnectors()) {
  257 | 			checkConnectorHasStarted(connector);
  258 | 		}
  259 | 	}
  260 | 
  261 | 	private void checkConnectorHasStarted(Connector connector) {
  262 | 		if (LifecycleState.FAILED.equals(connector.getState())) {
  263 | 			throw new ConnectorStartFailedException(connector.getPort());
  264 | 		}
  265 | 	}
  266 | 
  267 | 	private void stopSilently() {
  268 | 		try {
  269 | 			stopTomcat();
  270 | 		}
  271 | 		catch (LifecycleException ex) {
  272 | 			// Ignore
  273 | 		}
  274 | 	}
  275 | 
  276 | 	private void destroySilently() {
  277 | 		try {
  278 | 			this.tomcat.destroy();
  279 | 		}
  280 | 		catch (LifecycleException ex) {
  281 | 			// Ignore
  282 | 		}
  283 | 	}
  284 | 
  285 | 	private void stopTomcat() throws LifecycleException {
  286 | 		if (Thread.currentThread().getContextClassLoader() instanceof TomcatEmbeddedWebappClassLoader) {
  287 | 			Thread.currentThread().setContextClassLoader(getClass().getClassLoader());
  288 | 		}
  289 | 		this.tomcat.stop();
  290 | 	}
  291 | 
  292 | 	private void addPreviouslyRemovedConnectors() {
  293 | 		Service[] services = this.tomcat.getServer().findServices();
  294 | 		for (Service service : services) {
  295 | 			Connector[] connectors = this.serviceConnectors.get(service);
  296 | 			if (connectors != null) {
  297 | 				for (Connector connector : connectors) {
  298 | 					service.addConnector(connector);
  299 | 					if (!this.autoStart) {
  300 | 						stopProtocolHandler(connector);
  301 | 					}
  302 | 				}
  303 | 				this.serviceConnectors.remove(service);
  304 | 			}
  305 | 		}
  306 | 	}
  307 | 
  308 | 	private void stopProtocolHandler(Connector connector) {
  309 | 		try {
  310 | 			connector.getProtocolHandler().stop();
  311 | 		}
  312 | 		catch (Exception ex) {
  313 | 			logger.error("Cannot pause connector: ", ex);
  314 | 		}
  315 | 	}
  316 | 
  317 | 	private void performDeferredLoadOnStartup() {
  318 | 		try {
  319 | 			for (Container child : this.tomcat.getHost().findChildren()) {
  320 | 				if (child instanceof TomcatEmbeddedContext embeddedContext) {
  321 | 					embeddedContext.deferredLoadOnStartup();
  322 | 				}
  323 | 			}
  324 | 		}
  325 | 		catch (Exception ex) {
  326 | 			if (ex instanceof WebServerException webServerException) {
  327 | 				throw webServerException;
  328 | 			}
  329 | 			throw new WebServerException("Unable to start embedded Tomcat connectors", ex);
  330 | 		}
  331 | 	}
  332 | 
  333 | 	Map<Service, Connector[]> getServiceConnectors() {
  334 | 		return this.serviceConnectors;
  335 | 	}
  336 | 
  337 | 	@Override
  338 | 	public void stop() throws WebServerException {
  339 | 		synchronized (this.monitor) {
  340 | 			boolean wasStarted = this.started;
  341 | 			try {
  342 | 				this.started = false;
  343 | 				if (this.gracefulShutdown != null) {
  344 | 					this.gracefulShutdown.abort();
  345 | 				}
  346 | 				removeServiceConnectors();
  347 | 			}
  348 | 			catch (Exception ex) {
  349 | 				throw new WebServerException("Unable to stop embedded Tomcat", ex);
  350 | 			}
  351 | 			finally {
  352 | 				if (wasStarted) {
  353 | 					containerCounter.decrementAndGet();
  354 | 				}
  355 | 			}
  356 | 		}
  357 | 	}
  358 | 
  359 | 	@Override
  360 | 	public void destroy() throws WebServerException {
  361 | 		try {
  362 | 			stopTomcat();
  363 | 			this.tomcat.destroy();
  364 | 		}
  365 | 		catch (LifecycleException ex) {
  366 | 			// Swallow and continue
  367 | 		}
  368 | 		catch (Exception ex) {
  369 | 			throw new WebServerException("Unable to destroy embedded Tomcat", ex);
  370 | 		}
  371 | 	}
  372 | 
  373 | 	private String getPortsDescription(boolean localPort) {
  374 | 		StringBuilder description = new StringBuilder();
  375 | 		Connector[] connectors = this.tomcat.getService().findConnectors();
  376 | 		description.append("port");
  377 | 		if (connectors.length != 1) {
  378 | 			description.append("s");
  379 | 		}
  380 | 		description.append(" ");
  381 | 		for (int i = 0; i < connectors.length; i++) {
  382 | 			if (i != 0) {
  383 | 				description.append(", ");
  384 | 			}
  385 | 			Connector connector = connectors[i];
  386 | 			int port = localPort ? connector.getLocalPort() : connector.getPort();
  387 | 			description.append(port).append(" (").append(connector.getScheme()).append(')');
  388 | 		}
  389 | 		return description.toString();
  390 | 	}
  391 | 
  392 | 	@Override
  393 | 	public int getPort() {
  394 | 		Connector connector = this.tomcat.getConnector();
  395 | 		if (connector != null) {
  396 | 			return connector.getLocalPort();
  397 | 		}
  398 | 		return -1;
  399 | 	}
  400 | 
  401 | 	private @Nullable String getContextPath() {
  402 | 		String contextPath = Arrays.stream(this.tomcat.getHost().findChildren())
  403 | 			.filter(TomcatEmbeddedContext.class::isInstance)
  404 | 			.map(TomcatEmbeddedContext.class::cast)
  405 | 			.filter(this::imperative)
  406 | 			.map(TomcatEmbeddedContext::getPath)
  407 | 			.map((path) -> path.isEmpty() ? "/" : path)
  408 | 			.collect(Collectors.joining(" "));
  409 | 		return StringUtils.hasText(contextPath) ? contextPath : null;
  410 | 	}
  411 | 
  412 | 	private boolean imperative(TomcatEmbeddedContext context) {
  413 | 		for (Container container : context.findChildren()) {
  414 | 			if (container instanceof Wrapper wrapper) {
  415 | 				if (wrapper.getServletClass()
  416 | 					.equals("org.springframework.http.server.reactive.TomcatHttpHandlerAdapter")) {
  417 | 					return false;
  418 | 				}
  419 | 			}
  420 | 		}
  421 | 		return true;
  422 | 	}
  423 | 
  424 | 	/**
  425 | 	 * Returns access to the underlying Tomcat server.
  426 | 	 * @return the Tomcat server
  427 | 	 */
  428 | 	public Tomcat getTomcat() {
  429 | 		return this.tomcat;
  430 | 	}
  431 | 
  432 | 	/**
  433 | 	 * Initiates a graceful shutdown of the Tomcat web server. Handling of new requests is
  434 | 	 * prevented and the given {@code callback} is invoked at the end of the attempt. The
  435 | 	 * attempt can be explicitly ended by invoking {@link #stop}.
  436 | 	 * <p>
  437 | 	 * Once shutdown has been initiated Tomcat will reject any new connections. Requests
  438 | 	 * on existing idle connections will also be rejected.
  439 | 	 */
  440 | 	@Override
  441 | 	public void shutDownGracefully(GracefulShutdownCallback callback) {
  442 | 		if (this.gracefulShutdown == null) {
  443 | 			callback.shutdownComplete(GracefulShutdownResult.IMMEDIATE);
  444 | 			return;
  445 | 		}
  446 | 		this.gracefulShutdown.shutDownGracefully(callback);
  447 | 	}
  448 | 
  449 | }
```
