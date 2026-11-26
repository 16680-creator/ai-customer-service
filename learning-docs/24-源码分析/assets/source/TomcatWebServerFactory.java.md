<!-- learning-docs source archive（机械抓取原文，禁止手改代码内容） -->
<!-- origin: org.springframework.boot:spring-boot-tomcat:4.2.0 | path: org/springframework/boot/tomcat/TomcatWebServerFactory.java -->
<!-- 正文引用：[[源码:TomcatWebServerFactory.java#L起-L止]] ；代码块头部：(src: TomcatWebServerFactory.java#L起-L止) -->

# TomcatWebServerFactory.java

| 项 | 值 |
| --- | --- |
| 来源 | `org.springframework.boot:spring-boot-tomcat:4.2.0` |
| 路径 | `org/springframework/boot/tomcat/TomcatWebServerFactory.java` |
| 行数 | 568 |
| 在线地址 | https://repo1.maven.org/maven2/org/springframework/boot/spring-boot-tomcat/4.2.0/spring-boot-tomcat-4.2.0-sources.jar |
| 抓取时间 | 2026-11-25T14:23:24.690Z |

```java
    1 | /*
    2 |  * Copyright 2012-present the original author or authors.
    3 |  */
    4 | 
    5 | package org.springframework.boot.tomcat;
    6 | 
    7 | import java.io.File;
    8 | import java.nio.charset.Charset;
    9 | import java.nio.charset.StandardCharsets;
   10 | import java.util.ArrayList;
   11 | import java.util.Arrays;
   12 | import java.util.Collection;
   13 | import java.util.LinkedHashSet;
   14 | import java.util.List;
   15 | import java.util.Set;
   16 | 
   17 | import org.apache.catalina.Context;
   18 | import org.apache.catalina.Engine;
   19 | import org.apache.catalina.Executor;
   20 | import org.apache.catalina.Lifecycle;
   21 | import org.apache.catalina.LifecycleEvent;
   22 | import org.apache.catalina.LifecycleListener;
   23 | import org.apache.catalina.Valve;
   24 | import org.apache.catalina.connector.Connector;
   25 | import org.apache.catalina.core.AprLifecycleListener;
   26 | import org.apache.catalina.startup.Tomcat;
   27 | import org.apache.catalina.valves.RemoteIpValve;
   28 | import org.apache.commons.logging.Log;
   29 | import org.apache.commons.logging.LogFactory;
   30 | import org.apache.coyote.AbstractProtocol;
   31 | import org.apache.coyote.ProtocolHandler;
   32 | import org.apache.coyote.http2.Http2Protocol;
   33 | import org.apache.tomcat.util.modeler.Registry;
   34 | import org.jspecify.annotations.Nullable;
   35 | 
   36 | import org.springframework.boot.context.properties.PropertyMapper;
   37 | import org.springframework.boot.context.properties.PropertyMapper.Source;
   38 | import org.springframework.boot.ssl.SslBundles;
   39 | import org.springframework.boot.tomcat.RemoteIp.Header;
   40 | import org.springframework.boot.tomcat.RemoteIp.Proxies;
   41 | import org.springframework.boot.util.LambdaSafe;
   42 | import org.springframework.boot.web.server.AbstractConfigurableWebServerFactory;
   43 | import org.springframework.boot.web.server.ForwardedRequestHeaders;
   44 | import org.springframework.boot.web.server.Ssl;
   45 | import org.springframework.core.NativeDetector;
   46 | import org.springframework.util.Assert;
   47 | import org.springframework.util.StringUtils;
   48 | 
   49 | /**
   50 |  * Base class for factories that produce a {@link TomcatWebServer}.
   51 |  *
   52 |  * @author Andy Wilkinson
   53 |  * @since 4.0.0
   54 |  */
   55 | public class TomcatWebServerFactory extends AbstractConfigurableWebServerFactory
   56 | 		implements ConfigurableTomcatWebServerFactory {
   57 | 
   58 | 	private static final Charset DEFAULT_CHARSET = StandardCharsets.UTF_8;
   59 | 
   60 | 	/**
   61 | 	 * The class name of default protocol used.
   62 | 	 */
   63 | 	public static final String DEFAULT_PROTOCOL = "org.apache.coyote.http11.Http11NioProtocol";
   64 | 
   65 | 	private final Log logger = LogFactory.getLog(getClass());
   66 | 
   67 | 	private @Nullable File baseDirectory;
   68 | 
   69 | 	private int backgroundProcessorDelay;
   70 | 
   71 | 	private @Nullable RemoteIp remoteIp;
   72 | 
   73 | 	private List<Valve> engineValves = new ArrayList<>();
   74 | 
   75 | 	private List<Valve> contextValves = new ArrayList<>();
   76 | 
   77 | 	private List<LifecycleListener> contextLifecycleListeners = new ArrayList<>();
   78 | 
   79 | 	private Set<TomcatContextCustomizer> contextCustomizers = new LinkedHashSet<>();
   80 | 
   81 | 	private Set<TomcatConnectorCustomizer> connectorCustomizers = new LinkedHashSet<>();
   82 | 
   83 | 	private Set<TomcatProtocolHandlerCustomizer<?>> protocolHandlerCustomizers = new LinkedHashSet<>();
   84 | 
   85 | 	private Set<TomcatServerCustomizer> serverCustomizers = new LinkedHashSet<>();
   86 | 
   87 | 	private List<Connector> additionalConnectors = new ArrayList<>();
   88 | 
   89 | 	private Charset uriEncoding = DEFAULT_CHARSET;
   90 | 
   91 | 	private String protocol = DEFAULT_PROTOCOL;
   92 | 
   93 | 	private boolean disableMBeanRegistry = true;
   94 | 
   95 | 	private boolean useApr;
   96 | 
   97 | 	protected TomcatWebServerFactory() {
   98 | 	}
   99 | 
  100 | 	protected TomcatWebServerFactory(int port) {
  101 | 		super(port);
  102 | 	}
  103 | 
  104 | 	private List<LifecycleListener> getDefaultServerLifecycleListeners() {
  105 | 		ArrayList<LifecycleListener> lifecycleListeners = new ArrayList<>();
  106 | 		if (this.useApr && !NativeDetector.inNativeImage()) {
  107 | 			lifecycleListeners.add(new AprLifecycleListener());
  108 | 		}
  109 | 		return lifecycleListeners;
  110 | 	}
  111 | 
  112 | 	@Override
  113 | 	public void setBaseDirectory(@Nullable File baseDirectory) {
  114 | 		this.baseDirectory = baseDirectory;
  115 | 	}
  116 | 
  117 | 	public @Nullable File getBaseDirectory() {
  118 | 		return this.baseDirectory;
  119 | 	}
  120 | 
  121 | 	/**
  122 | 	 * Returns a mutable collection of the {@link Valve}s that will be applied to the
  123 | 	 * Tomcat {@link Engine}.
  124 | 	 * @return the engine valves that will be applied
  125 | 	 */
  126 | 	public Collection<Valve> getEngineValves() {
  127 | 		return this.engineValves;
  128 | 	}
  129 | 
  130 | 	/**
  131 | 	 * Set {@link Valve}s that should be applied to the Tomcat {@link Engine}. Calling
  132 | 	 * this method will replace any existing valves.
  133 | 	 * @param engineValves the valves to set
  134 | 	 */
  135 | 	public void setEngineValves(Collection<? extends Valve> engineValves) {
  136 | 		Assert.notNull(engineValves, "'engineValves' must not be null");
  137 | 		this.engineValves = new ArrayList<>(engineValves);
  138 | 	}
  139 | 
  140 | 	@Override
  141 | 	public void addEngineValves(Valve... engineValves) {
  142 | 		Assert.notNull(engineValves, "'engineValves' must not be null");
  143 | 		this.engineValves.addAll(Arrays.asList(engineValves));
  144 | 	}
  145 | 
  146 | 	public Charset getUriEncoding() {
  147 | 		return this.uriEncoding;
  148 | 	}
  149 | 
  150 | 	@Override
  151 | 	public void setUriEncoding(Charset uriEncoding) {
  152 | 		this.uriEncoding = uriEncoding;
  153 | 	}
  154 | 
  155 | 	public int getBackgroundProcessorDelay() {
  156 | 		return this.backgroundProcessorDelay;
  157 | 	}
  158 | 
  159 | 	@Override
  160 | 	public void setBackgroundProcessorDelay(int delay) {
  161 | 		this.backgroundProcessorDelay = delay;
  162 | 	}
  163 | 
  164 | 	public @Nullable RemoteIp getRemoteIp() {
  165 | 		return this.remoteIp;
  166 | 	}
  167 | 
  168 | 	@Override
  169 | 	public void setRemoteIp(@Nullable RemoteIp remoteIp) {
  170 | 		this.remoteIp = remoteIp;
  171 | 	}
  172 | 
  173 | 	public String getProtocol() {
  174 | 		return this.protocol;
  175 | 	}
  176 | 
  177 | 	/**
  178 | 	 * The Tomcat protocol to use when create the {@link Connector}.
  179 | 	 * @param protocol the protocol
  180 | 	 * @see Connector#Connector(String)
  181 | 	 */
  182 | 	public void setProtocol(String protocol) {
  183 | 		Assert.hasLength(protocol, "'protocol' must not be empty");
  184 | 		this.protocol = protocol;
  185 | 	}
  186 | 
  187 | 	/**
  188 | 	 * Returns a mutable collection of the {@link Valve}s that will be applied to the
  189 | 	 * Tomcat {@link Context}.
  190 | 	 * @return the context valves that will be applied
  191 | 	 * @see #getEngineValves()
  192 | 	 */
  193 | 	public Collection<Valve> getContextValves() {
  194 | 		return this.contextValves;
  195 | 	}
  196 | 
  197 | 	/**
  198 | 	 * Set {@link Valve}s that should be applied to the Tomcat {@link Context}. Calling
  199 | 	 * this method will replace any existing valves.
  200 | 	 * @param contextValves the valves to set
  201 | 	 */
  202 | 	public void setContextValves(Collection<? extends Valve> contextValves) {
  203 | 		Assert.notNull(contextValves, "'contextValves' must not be null");
  204 | 		this.contextValves = new ArrayList<>(contextValves);
  205 | 	}
  206 | 
  207 | 	/**
  208 | 	 * Add {@link Valve}s that should be applied to the Tomcat {@link Context}.
  209 | 	 * @param contextValves the valves to add
  210 | 	 */
  211 | 	public void addContextValves(Valve... contextValves) {
  212 | 		Assert.notNull(contextValves, "'contextValves' must not be null");
  213 | 		this.contextValves.addAll(Arrays.asList(contextValves));
  214 | 	}
  215 | 
  216 | 	/**
  217 | 	 * Returns a mutable collection of the {@link LifecycleListener}s that will be applied
  218 | 	 * to the Tomcat {@link Context}.
  219 | 	 * @return the context lifecycle listeners that will be applied
  220 | 	 */
  221 | 	public Collection<LifecycleListener> getContextLifecycleListeners() {
  222 | 		return this.contextLifecycleListeners;
  223 | 	}
  224 | 
  225 | 	/**
  226 | 	 * Set {@link LifecycleListener}s that should be applied to the Tomcat
  227 | 	 * {@link Context}. Calling this method will replace any existing listeners.
  228 | 	 * @param contextLifecycleListeners the listeners to set
  229 | 	 */
  230 | 	public void setContextLifecycleListeners(Collection<? extends LifecycleListener> contextLifecycleListeners) {
  231 | 		Assert.notNull(contextLifecycleListeners, "'contextLifecycleListeners' must not be null");
  232 | 		this.contextLifecycleListeners = new ArrayList<>(contextLifecycleListeners);
  233 | 	}
  234 | 
  235 | 	/**
  236 | 	 * Add {@link LifecycleListener}s that should be added to the Tomcat {@link Context}.
  237 | 	 * @param contextLifecycleListeners the listeners to add
  238 | 	 */
  239 | 	public void addContextLifecycleListeners(LifecycleListener... contextLifecycleListeners) {
  240 | 		Assert.notNull(contextLifecycleListeners, "'contextLifecycleListeners' must not be null");
  241 | 		this.contextLifecycleListeners.addAll(Arrays.asList(contextLifecycleListeners));
  242 | 	}
  243 | 
  244 | 	/**
  245 | 	 * Returns a mutable collection of the {@link TomcatContextCustomizer}s that will be
  246 | 	 * applied to the Tomcat {@link Context}.
  247 | 	 * @return the listeners that will be applied
  248 | 	 */
  249 | 	public Collection<TomcatContextCustomizer> getContextCustomizers() {
  250 | 		return this.contextCustomizers;
  251 | 	}
  252 | 
  253 | 	/**
  254 | 	 * Set {@link TomcatContextCustomizer}s that should be applied to the Tomcat
  255 | 	 * {@link Context}. Calling this method will replace any existing customizers.
  256 | 	 * @param contextCustomizers the customizers to set
  257 | 	 */
  258 | 	public void setContextCustomizers(Collection<? extends TomcatContextCustomizer> contextCustomizers) {
  259 | 		Assert.notNull(contextCustomizers, "'contextCustomizers' must not be null");
  260 | 		this.contextCustomizers = new LinkedHashSet<>(contextCustomizers);
  261 | 	}
  262 | 
  263 | 	@Override
  264 | 	public void addContextCustomizers(TomcatContextCustomizer... contextCustomizers) {
  265 | 		Assert.notNull(contextCustomizers, "'contextCustomizers' must not be null");
  266 | 		this.contextCustomizers.addAll(Arrays.asList(contextCustomizers));
  267 | 	}
  268 | 
  269 | 	/**
  270 | 	 * Returns a mutable collection of the {@link TomcatConnectorCustomizer}s that will be
  271 | 	 * applied to the Tomcat {@link Connector}.
  272 | 	 * @return the customizers that will be applied
  273 | 	 */
  274 | 	public Set<TomcatConnectorCustomizer> getConnectorCustomizers() {
  275 | 		return this.connectorCustomizers;
  276 | 	}
  277 | 
  278 | 	/**
  279 | 	 * Set {@link TomcatConnectorCustomizer}s that should be applied to the Tomcat
  280 | 	 * {@link Connector}. Calling this method will replace any existing customizers.
  281 | 	 * @param connectorCustomizers the customizers to set
  282 | 	 */
  283 | 	public void setConnectorCustomizers(Collection<? extends TomcatConnectorCustomizer> connectorCustomizers) {
  284 | 		Assert.notNull(connectorCustomizers, "'connectorCustomizers' must not be null");
  285 | 		this.connectorCustomizers = new LinkedHashSet<>(connectorCustomizers);
  286 | 	}
  287 | 
  288 | 	@Override
  289 | 	public void addConnectorCustomizers(TomcatConnectorCustomizer... connectorCustomizers) {
  290 | 		Assert.notNull(connectorCustomizers, "'connectorCustomizers' must not be null");
  291 | 		this.connectorCustomizers.addAll(Arrays.asList(connectorCustomizers));
  292 | 	}
  293 | 
  294 | 	/**
  295 | 	 * Returns a mutable collection of the {@link TomcatProtocolHandlerCustomizer}s that
  296 | 	 * will be applied to the Tomcat {@link Connector}.
  297 | 	 * @return the customizers that will be applied
  298 | 	 */
  299 | 	public Set<TomcatProtocolHandlerCustomizer<?>> getProtocolHandlerCustomizers() {
  300 | 		return this.protocolHandlerCustomizers;
  301 | 	}
  302 | 
  303 | 	/**
  304 | 	 * Set {@link TomcatProtocolHandlerCustomizer}s that should be applied to the Tomcat
  305 | 	 * {@link Connector}. Calling this method will replace any existing customizers.
  306 | 	 * @param protocolHandlerCustomizers the customizers to set
  307 | 	 */
  308 | 	public void setProtocolHandlerCustomizers(
  309 | 			Collection<? extends TomcatProtocolHandlerCustomizer<?>> protocolHandlerCustomizers) {
  310 | 		Assert.notNull(protocolHandlerCustomizers, "'protocolHandlerCustomizers' must not be null");
  311 | 		this.protocolHandlerCustomizers = new LinkedHashSet<>(protocolHandlerCustomizers);
  312 | 	}
  313 | 
  314 | 	@Override
  315 | 	public void addProtocolHandlerCustomizers(TomcatProtocolHandlerCustomizer<?>... protocolHandlerCustomizers) {
  316 | 		Assert.notNull(protocolHandlerCustomizers, "'protocolHandlerCustomizers' must not be null");
  317 | 		this.protocolHandlerCustomizers.addAll(Arrays.asList(protocolHandlerCustomizers));
  318 | 	}
  319 | 
  320 | 	/**
  321 | 	 * Returns a mutable collection of the {@link TomcatServerCustomizer}s that will be
  322 | 	 * applied to the Tomcat {@link org.apache.catalina.Server}.
  323 | 	 * @return the customizers that will be applied
  324 | 	 * @since 4.2.0
  325 | 	 */
  326 | 	public Set<TomcatServerCustomizer> getServerCustomizers() {
  327 | 		return this.serverCustomizers;
  328 | 	}
  329 | 
  330 | 	/**
  331 | 	 * Set {@link TomcatServerCustomizer}s that will be applied to the Tomcat
  332 | 	 * {@link org.apache.catalina.Server}. Calling this method will replace any existing
  333 | 	 * customizers.
  334 | 	 * @param serverCustomizers the customizers to set
  335 | 	 * @since 4.2.0
  336 | 	 */
  337 | 	public void setServerCustomizers(Collection<? extends TomcatServerCustomizer> serverCustomizers) {
  338 | 		Assert.notNull(serverCustomizers, "'serverCustomizers' must not be null");
  339 | 		this.serverCustomizers = new LinkedHashSet<>(serverCustomizers);
  340 | 	}
  341 | 
  342 | 	@Override
  343 | 	public void addServerCustomizers(TomcatServerCustomizer... serverCustomizers) {
  344 | 		Assert.notNull(serverCustomizers, "'serverCustomizers' must not be null");
  345 | 		this.serverCustomizers.addAll(Arrays.asList(serverCustomizers));
  346 | 	}
  347 | 
  348 | 	/**
  349 | 	 * Returns a mutable collection of the {@link Connector}s that will be added to the
  350 | 	 * Tomcat server.
  351 | 	 * @return the additional connectors
  352 | 	 */
  353 | 	public List<Connector> getAdditionalConnectors() {
  354 | 		return this.additionalConnectors;
  355 | 	}
  356 | 
  357 | 	/**
  358 | 	 * Set additional {@link Connector}s that should be added to the Tomcat server .
  359 | 	 * Calling this method will replace any existing additional connectors.
  360 | 	 * @param additionalConnectors the additionalConnectors to set
  361 | 	 */
  362 | 	public void setAdditionalConnectors(Collection<? extends Connector> additionalConnectors) {
  363 | 		Assert.notNull(additionalConnectors, "'additionalConnectors' must not be null");
  364 | 		this.additionalConnectors = new ArrayList<>(additionalConnectors);
  365 | 	}
  366 | 
  367 | 	/**
  368 | 	 * Add {@link Connector}s in addition to the default connector, e.g. for SSL or AJP.
  369 | 	 * <p>
  370 | 	 * {@link #getConnectorCustomizers Connector customizers} are not applied to
  371 | 	 * connectors added this way.
  372 | 	 * @param connectors the connectors to add
  373 | 	 */
  374 | 	public void addAdditionalConnectors(Connector... connectors) {
  375 | 		Assert.notNull(connectors, "'connectors' must not be null");
  376 | 		this.additionalConnectors.addAll(Arrays.asList(connectors));
  377 | 	}
  378 | 
  379 | 	/**
  380 | 	 * Returns whether the factory should disable Tomcat's MBean registry prior to
  381 | 	 * creating the server.
  382 | 	 * @return whether to disable Tomcat's MBean registry
  383 | 	 */
  384 | 	public boolean isDisableMBeanRegistry() {
  385 | 		return this.disableMBeanRegistry;
  386 | 	}
  387 | 
  388 | 	/**
  389 | 	 * Set whether the factory should disable Tomcat's MBean registry prior to creating
  390 | 	 * the server.
  391 | 	 * @param disableMBeanRegistry whether to disable the MBean registry
  392 | 	 */
  393 | 	@Override
  394 | 	public void setDisableMBeanRegistry(boolean disableMBeanRegistry) {
  395 | 		this.disableMBeanRegistry = disableMBeanRegistry;
  396 | 	}
  397 | 
  398 | 	/**
  399 | 	 * Whether to use APR.
  400 | 	 * @param useApr whether to use APR
  401 | 	 */
  402 | 	@Override
  403 | 	public void setUseApr(boolean useApr) {
  404 | 		this.useApr = useApr;
  405 | 	}
  406 | 
  407 | 	/**
  408 | 	 * Creates the {@link Tomcat} web server.
  409 | 	 * @return the web server.
  410 | 	 * @deprecated since 4.1.0 for removal in 4.3.0 in favor of
  411 | 	 * {@link #createTomcat(TempDirs)}
  412 | 	 */
  413 | 	@Deprecated(forRemoval = true, since = "4.1.0")
  414 | 	protected Tomcat createTomcat() {
  415 | 		return createTomcat(new TempDirs(getPort()));
  416 | 	}
  417 | 
  418 | 	/**
  419 | 	 * Creates the {@link Tomcat} web server.
  420 | 	 * @param tempDirs to manage temporary directories
  421 | 	 * @return the web server
  422 | 	 * @since 4.1.0
  423 | 	 */
  424 | 	protected Tomcat createTomcat(TempDirs tempDirs) {
  425 | 		if (this.isDisableMBeanRegistry()) {
  426 | 			Registry.disableRegistry();
  427 | 		}
  428 | 		Tomcat tomcat = new Tomcat();
  429 | 		File baseDir = (getBaseDirectory() != null) ? getBaseDirectory() : tempDirs.createTempDir("tomcat").toFile();
  430 | 		tomcat.setBaseDir(baseDir.getAbsolutePath());
  431 | 		for (LifecycleListener listener : getDefaultServerLifecycleListeners()) {
  432 | 			tomcat.getServer().addLifecycleListener(listener);
  433 | 		}
  434 | 		tomcat.getServer().addLifecycleListener(new CleanTempDirsListener(tempDirs));
  435 | 		Connector connector = new Connector(getProtocol());
  436 | 		connector.setThrowOnFailure(true);
  437 | 		tomcat.getService().addConnector(connector);
  438 | 		customizeConnector(connector);
  439 | 		tomcat.setConnector(connector);
  440 | 		registerConnectorExecutor(tomcat, connector);
  441 | 		addRemoteIpValveIfNecessary(tomcat.getEngine());
  442 | 		tomcat.getHost().setAutoDeploy(false);
  443 | 		configureEngine(tomcat.getEngine());
  444 | 		for (Connector additionalConnector : this.getAdditionalConnectors()) {
  445 | 			tomcat.getService().addConnector(additionalConnector);
  446 | 			registerConnectorExecutor(tomcat, additionalConnector);
  447 | 		}
  448 | 		for (TomcatServerCustomizer customizer : getServerCustomizers()) {
  449 | 			customizer.customize(tomcat.getServer());
  450 | 		}
  451 | 		return tomcat;
  452 | 	}
  453 | 
  454 | 	protected void customizeConnector(Connector connector) {
  455 | 		int port = Math.max(getPort(), 0);
  456 | 		connector.setPort(port);
  457 | 		if (StringUtils.hasText(getServerHeader())) {
  458 | 			connector.setProperty("server", getServerHeader());
  459 | 		}
  460 | 		if (connector.getProtocolHandler() instanceof AbstractProtocol<?> abstractProtocol) {
  461 | 			customizeProtocol(abstractProtocol);
  462 | 		}
  463 | 		invokeProtocolHandlerCustomizers(connector.getProtocolHandler());
  464 | 		if (getUriEncoding() != null) {
  465 | 			connector.setURIEncoding(getUriEncoding().name());
  466 | 		}
  467 | 		if (getHttp2() != null && getHttp2().isEnabled()) {
  468 | 			connector.addUpgradeProtocol(new Http2Protocol());
  469 | 		}
  470 | 		Ssl ssl = getSsl();
  471 | 		if (Ssl.isEnabled(ssl)) {
  472 | 			customizeSsl(connector, ssl);
  473 | 		}
  474 | 		TomcatConnectorCustomizer compression = new CompressionConnectorCustomizer(getCompression());
  475 | 		compression.customize(connector);
  476 | 		for (TomcatConnectorCustomizer customizer : this.getConnectorCustomizers()) {
  477 | 			customizer.customize(connector);
  478 | 		}
  479 | 	}
  480 | 
  481 | 	private void customizeProtocol(AbstractProtocol<?> protocol) {
  482 | 		if (getAddress() != null) {
  483 | 			protocol.setAddress(getAddress());
  484 | 		}
  485 | 	}
  486 | 
  487 | 	@SuppressWarnings("unchecked")
  488 | 	private void invokeProtocolHandlerCustomizers(ProtocolHandler protocolHandler) {
  489 | 		LambdaSafe
  490 | 			.callbacks(TomcatProtocolHandlerCustomizer.class, this.getProtocolHandlerCustomizers(), protocolHandler)
  491 | 			.invoke((customizer) -> customizer.customize(protocolHandler));
  492 | 	}
  493 | 
  494 | 	private void customizeSsl(Connector connector, Ssl ssl) {
  495 | 		SslConnectorCustomizer customizer = new SslConnectorCustomizer(this.logger, connector, ssl.getClientAuth());
  496 | 		customizer.customize(getSslBundle(), getServerNameSslBundles());
  497 | 		addBundleUpdateHandler(null, ssl.getBundle(), customizer);
  498 | 		ssl.getServerNameBundles()
  499 | 			.forEach((serverNameSslBundle) -> addBundleUpdateHandler(serverNameSslBundle.serverName(),
  500 | 					serverNameSslBundle.bundle(), customizer));
  501 | 	}
  502 | 
  503 | 	private void addBundleUpdateHandler(@Nullable String serverName, @Nullable String sslBundleName,
  504 | 			SslConnectorCustomizer customizer) {
  505 | 		if (StringUtils.hasText(sslBundleName)) {
  506 | 			SslBundles sslBundles = getSslBundles();
  507 | 			Assert.state(sslBundles != null, "'sslBundles' must not be null");
  508 | 			sslBundles.addBundleUpdateHandler(sslBundleName, (sslBundle) -> customizer.update(serverName, sslBundle));
  509 | 		}
  510 | 	}
  511 | 
  512 | 	private void registerConnectorExecutor(Tomcat tomcat, Connector connector) {
  513 | 		if (connector.getProtocolHandler().getExecutor() instanceof Executor executor) {
  514 | 			tomcat.getService().addExecutor(executor);
  515 | 		}
  516 | 	}
  517 | 
  518 | 	private void addRemoteIpValveIfNecessary(Engine engine) {
  519 | 		RemoteIp remoteIp = getRemoteIp();
  520 | 		ForwardedRequestHeaders forwardedHeaders = getForwardedRequestHeaders();
  521 | 		Assert.state(
  522 | 				forwardedHeaders == null || forwardedHeaders.equals(ForwardedRequestHeaders.X_FORWARDED_WITHOUT_PREFIX),
  523 | 				"Tomcat does not support RFC 7239 or 'prefix' forwarded header");
  524 | 		if (forwardedHeaders != null || (remoteIp != null && remoteIp.usage() == RemoteIp.Usage.ALWAYS)) {
  525 | 			RemoteIpValve valve = new RemoteIpValve();
  526 | 			if (forwardedHeaders != null) {
  527 | 				valve.setHostHeader("X-Forwarded-Host");
  528 | 				valve.setPortHeader("X-Forwarded-Port");
  529 | 			}
  530 | 			PropertyMapper map = PropertyMapper.get();
  531 | 			Source<Header> mapFromHeader = map.from(remoteIp).as(RemoteIp::header);
  532 | 			mapFromHeader.as(Header::host).to(valve::setHostHeader);
  533 | 			mapFromHeader.as(Header::port).to(valve::setPortHeader);
  534 | 			mapFromHeader.as(Header::remoteIp).to(valve::setRemoteIpHeader);
  535 | 			mapFromHeader.as(Header::protocol).to(valve::setProtocolHeader);
  536 | 			mapFromHeader.as(Header::protocolHttpsValue).to(valve::setProtocolHeaderHttpsValue);
  537 | 			Source<Proxies> mapFromProxies = map.from(remoteIp).as(RemoteIp::proxies);
  538 | 			mapFromProxies.as(Proxies::internal).to(valve::setInternalProxies);
  539 | 			mapFromProxies.as(Proxies::trusted).to(valve::setTrustedProxies);
  540 | 			engine.getPipeline().addValve(valve);
  541 | 		}
  542 | 	}
  543 | 
  544 | 	private void configureEngine(Engine engine) {
  545 | 		engine.setBackgroundProcessorDelay(getBackgroundProcessorDelay());
  546 | 		for (Valve valve : getEngineValves()) {
  547 | 			engine.getPipeline().addValve(valve);
  548 | 		}
  549 | 	}
  550 | 
  551 | 	private static class CleanTempDirsListener implements LifecycleListener {
  552 | 
  553 | 		private final TempDirs tempDirs;
  554 | 
  555 | 		CleanTempDirsListener(TempDirs tempDirs) {
  556 | 			this.tempDirs = tempDirs;
  557 | 		}
  558 | 
  559 | 		@Override
  560 | 		public void lifecycleEvent(LifecycleEvent event) {
  561 | 			if (event.getType().equals(Lifecycle.AFTER_DESTROY_EVENT)) {
  562 | 				this.tempDirs.cleanup();
  563 | 			}
  564 | 		}
  565 | 
  566 | 	}
  567 | 
  568 | }
```
