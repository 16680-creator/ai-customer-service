<!-- learning-docs source archive（机械抓取原文，禁止手改代码内容） -->
<!-- origin: org.springframework.cloud:spring-cloud-gateway-server-webflux:5.1.0 | path: org/springframework/cloud/gateway/config/HttpClientProperties.java -->
<!-- 正文引用：[[源码:HttpClientProperties.java#L起-L止]] ；代码块头部：(src: HttpClientProperties.java#L起-L止) -->

# HttpClientProperties.java

| 项 | 值 |
| --- | --- |
| 来源 | `org.springframework.cloud:spring-cloud-gateway-server-webflux:5.1.0` |
| 路径 | `org/springframework/cloud/gateway/config/HttpClientProperties.java` |
| 行数 | 586 |
| 在线地址 | https://repo1.maven.org/maven2/org/springframework/cloud/spring-cloud-gateway-server-webflux/5.1.0/spring-cloud-gateway-server-webflux-5.1.0-sources.jar |
| 抓取时间 | 2026-11-25T15:33:49.718Z |

```java
    1 | /*
    2 |  * Copyright 2013-present the original author or authors.
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
   17 | package org.springframework.cloud.gateway.config;
   18 | 
   19 | import java.time.Duration;
   20 | import java.util.ArrayList;
   21 | import java.util.List;
   22 | 
   23 | import jakarta.validation.constraints.Max;
   24 | import reactor.netty.resources.ConnectionProvider;
   25 | import reactor.netty.transport.ProxyProvider;
   26 | 
   27 | import org.springframework.boot.context.properties.ConfigurationProperties;
   28 | import org.springframework.core.style.ToStringCreator;
   29 | import org.springframework.util.unit.DataSize;
   30 | import org.springframework.validation.annotation.Validated;
   31 | 
   32 | /**
   33 |  * Configuration properties for the Netty {@link reactor.netty.http.client.HttpClient}.
   34 |  */
   35 | @ConfigurationProperties(GatewayProperties.PREFIX + ".httpclient")
   36 | @Validated
   37 | public class HttpClientProperties {
   38 | 
   39 | 	/** The connect timeout in millis, the default is 30s. */
   40 | 	private Integer connectTimeout;
   41 | 
   42 | 	/** The response timeout. */
   43 | 	private Duration responseTimeout;
   44 | 
   45 | 	/** The max response header size. */
   46 | 	private DataSize maxHeaderSize;
   47 | 
   48 | 	/** The max initial line length. */
   49 | 	private DataSize maxInitialLineLength;
   50 | 
   51 | 	/** Pool configuration for Netty HttpClient. */
   52 | 	private Pool pool = new Pool();
   53 | 
   54 | 	/** Proxy configuration for Netty HttpClient. */
   55 | 	private Proxy proxy = new Proxy();
   56 | 
   57 | 	/** SSL configuration for Netty HttpClient. */
   58 | 	private Ssl ssl = new Ssl();
   59 | 
   60 | 	/** Websocket configuration for Netty HttpClient. */
   61 | 	private Websocket websocket = new Websocket();
   62 | 
   63 | 	/** Enables wiretap debugging for Netty HttpClient. */
   64 | 	private boolean wiretap;
   65 | 
   66 | 	/** Enables compression for Netty HttpClient. */
   67 | 	private boolean compression;
   68 | 
   69 | 	public Integer getConnectTimeout() {
   70 | 		return connectTimeout;
   71 | 	}
   72 | 
   73 | 	public void setConnectTimeout(Integer connectTimeout) {
   74 | 		this.connectTimeout = connectTimeout;
   75 | 	}
   76 | 
   77 | 	public Duration getResponseTimeout() {
   78 | 		return responseTimeout;
   79 | 	}
   80 | 
   81 | 	public void setResponseTimeout(Duration responseTimeout) {
   82 | 		this.responseTimeout = responseTimeout;
   83 | 	}
   84 | 
   85 | 	@Max(Integer.MAX_VALUE)
   86 | 	public DataSize getMaxHeaderSize() {
   87 | 		return maxHeaderSize;
   88 | 	}
   89 | 
   90 | 	public void setMaxHeaderSize(DataSize maxHeaderSize) {
   91 | 		this.maxHeaderSize = maxHeaderSize;
   92 | 	}
   93 | 
   94 | 	@Max(Integer.MAX_VALUE)
   95 | 	public DataSize getMaxInitialLineLength() {
   96 | 		return maxInitialLineLength;
   97 | 	}
   98 | 
   99 | 	public void setMaxInitialLineLength(DataSize maxInitialLineLength) {
  100 | 		this.maxInitialLineLength = maxInitialLineLength;
  101 | 	}
  102 | 
  103 | 	public Pool getPool() {
  104 | 		return pool;
  105 | 	}
  106 | 
  107 | 	public void setPool(Pool pool) {
  108 | 		this.pool = pool;
  109 | 	}
  110 | 
  111 | 	public Proxy getProxy() {
  112 | 		return proxy;
  113 | 	}
  114 | 
  115 | 	public void setProxy(Proxy proxy) {
  116 | 		this.proxy = proxy;
  117 | 	}
  118 | 
  119 | 	public Ssl getSsl() {
  120 | 		return ssl;
  121 | 	}
  122 | 
  123 | 	public void setSsl(Ssl ssl) {
  124 | 		this.ssl = ssl;
  125 | 	}
  126 | 
  127 | 	public Websocket getWebsocket() {
  128 | 		return this.websocket;
  129 | 	}
  130 | 
  131 | 	public void setWebsocket(Websocket websocket) {
  132 | 		this.websocket = websocket;
  133 | 	}
  134 | 
  135 | 	public boolean isWiretap() {
  136 | 		return this.wiretap;
  137 | 	}
  138 | 
  139 | 	public void setWiretap(boolean wiretap) {
  140 | 		this.wiretap = wiretap;
  141 | 	}
  142 | 
  143 | 	public boolean isCompression() {
  144 | 		return compression;
  145 | 	}
  146 | 
  147 | 	public void setCompression(boolean compression) {
  148 | 		this.compression = compression;
  149 | 	}
  150 | 
  151 | 	@Override
  152 | 	public String toString() {
  153 | 		// @formatter:off
  154 | 		return new ToStringCreator(this)
  155 | 				.append("connectTimeout", connectTimeout)
  156 | 				.append("responseTimeout", responseTimeout)
  157 | 				.append("maxHeaderSize", maxHeaderSize)
  158 | 				.append("maxInitialLineLength", maxInitialLineLength)
  159 | 				.append("pool", pool)
  160 | 				.append("proxy", proxy)
  161 | 				.append("ssl", ssl)
  162 | 				.append("websocket", websocket)
  163 | 				.append("wiretap", wiretap)
  164 | 				.append("compression", compression)
  165 | 				.toString();
  166 | 		// @formatter:on
  167 | 
  168 | 	}
  169 | 
  170 | 	public static class Pool {
  171 | 
  172 | 		/** Type of pool for HttpClient to use (elastic, fixed or disabled). */
  173 | 		private PoolType type = PoolType.ELASTIC;
  174 | 
  175 | 		/** The channel pool map name, defaults to proxy. */
  176 | 		private String name = "proxy";
  177 | 
  178 | 		/**
  179 | 		 * Only for type FIXED, the maximum number of connections before starting pending
  180 | 		 * acquisition on existing ones.
  181 | 		 */
  182 | 		private Integer maxConnections = ConnectionProvider.DEFAULT_POOL_MAX_CONNECTIONS;
  183 | 
  184 | 		/** Only for type FIXED, the maximum time in millis to wait for acquiring. */
  185 | 		private Long acquireTimeout = ConnectionProvider.DEFAULT_POOL_ACQUIRE_TIMEOUT;
  186 | 
  187 | 		/**
  188 | 		 * Time in millis after which the channel will be closed. If NULL, there is no max
  189 | 		 * idle time.
  190 | 		 */
  191 | 		private Duration maxIdleTime = null;
  192 | 
  193 | 		/**
  194 | 		 * Duration after which the channel will be closed. If NULL, there is no max life
  195 | 		 * time.
  196 | 		 */
  197 | 		private Duration maxLifeTime = null;
  198 | 
  199 | 		/**
  200 | 		 * Perform regular eviction checks in the background at a specified interval.
  201 | 		 * Disabled by default ({@link Duration#ZERO})
  202 | 		 */
  203 | 		private Duration evictionInterval = Duration.ZERO;
  204 | 
  205 | 		/**
  206 | 		 * Enables channel pools metrics to be collected and registered in Micrometer.
  207 | 		 * Disabled by default.
  208 | 		 */
  209 | 		private boolean metrics = false;
  210 | 
  211 | 		/**
  212 | 		 * Configures the leasing strategy for the pool (fifo or lifo), defaults to FIFO
  213 | 		 * which is Netty's default.
  214 | 		 */
  215 | 		private LeasingStrategy leasingStrategy = LeasingStrategy.FIFO;
  216 | 
  217 | 		public PoolType getType() {
  218 | 			return type;
  219 | 		}
  220 | 
  221 | 		public void setType(PoolType type) {
  222 | 			this.type = type;
  223 | 		}
  224 | 
  225 | 		public String getName() {
  226 | 			return name;
  227 | 		}
  228 | 
  229 | 		public void setName(String name) {
  230 | 			this.name = name;
  231 | 		}
  232 | 
  233 | 		public Integer getMaxConnections() {
  234 | 			return maxConnections;
  235 | 		}
  236 | 
  237 | 		public void setMaxConnections(Integer maxConnections) {
  238 | 			this.maxConnections = maxConnections;
  239 | 		}
  240 | 
  241 | 		public Long getAcquireTimeout() {
  242 | 			return acquireTimeout;
  243 | 		}
  244 | 
  245 | 		public void setAcquireTimeout(Long acquireTimeout) {
  246 | 			this.acquireTimeout = acquireTimeout;
  247 | 		}
  248 | 
  249 | 		public Duration getMaxIdleTime() {
  250 | 			return maxIdleTime;
  251 | 		}
  252 | 
  253 | 		public void setMaxIdleTime(Duration maxIdleTime) {
  254 | 			this.maxIdleTime = maxIdleTime;
  255 | 		}
  256 | 
  257 | 		public Duration getMaxLifeTime() {
  258 | 			return maxLifeTime;
  259 | 		}
  260 | 
  261 | 		public void setMaxLifeTime(Duration maxLifeTime) {
  262 | 			this.maxLifeTime = maxLifeTime;
  263 | 		}
  264 | 
  265 | 		public Duration getEvictionInterval() {
  266 | 			return evictionInterval;
  267 | 		}
  268 | 
  269 | 		public void setEvictionInterval(Duration evictionInterval) {
  270 | 			this.evictionInterval = evictionInterval;
  271 | 		}
  272 | 
  273 | 		public boolean isMetrics() {
  274 | 			return metrics;
  275 | 		}
  276 | 
  277 | 		public void setMetrics(boolean metrics) {
  278 | 			this.metrics = metrics;
  279 | 		}
  280 | 
  281 | 		public LeasingStrategy getLeasingStrategy() {
  282 | 			return leasingStrategy;
  283 | 		}
  284 | 
  285 | 		public void setLeasingStrategy(LeasingStrategy leasingStrategy) {
  286 | 			this.leasingStrategy = leasingStrategy;
  287 | 		}
  288 | 
  289 | 		@Override
  290 | 		public String toString() {
  291 | 			return "Pool{" + "type=" + type + ", name='" + name + '\'' + ", maxConnections=" + maxConnections
  292 | 					+ ", acquireTimeout=" + acquireTimeout + ", maxIdleTime=" + maxIdleTime + ", maxLifeTime="
  293 | 					+ maxLifeTime + ", evictionInterval=" + evictionInterval + ", metrics=" + metrics
  294 | 					+ ", leasingStrategy=" + leasingStrategy + '}';
  295 | 		}
  296 | 
  297 | 		public enum PoolType {
  298 | 
  299 | 			/**
  300 | 			 * Elastic pool type.
  301 | 			 */
  302 | 			ELASTIC,
  303 | 
  304 | 			/**
  305 | 			 * Fixed pool type.
  306 | 			 */
  307 | 			FIXED,
  308 | 
  309 | 			/**
  310 | 			 * Disabled pool type.
  311 | 			 */
  312 | 			DISABLED
  313 | 
  314 | 		}
  315 | 
  316 | 		public enum LeasingStrategy {
  317 | 
  318 | 			/**
  319 | 			 * FIFO leasing strategy.
  320 | 			 */
  321 | 			FIFO,
  322 | 
  323 | 			/**
  324 | 			 * LIFO leasing strategy.
  325 | 			 */
  326 | 			LIFO
  327 | 
  328 | 		}
  329 | 
  330 | 	}
  331 | 
  332 | 	public static class Proxy {
  333 | 
  334 | 		/**
  335 | 		 * proxyType for proxy configuration of Netty HttpClient (http, socks4 or socks5).
  336 | 		 */
  337 | 		private ProxyProvider.Proxy type = ProxyProvider.Proxy.HTTP;
  338 | 
  339 | 		/** Hostname for proxy configuration of Netty HttpClient. */
  340 | 		private String host;
  341 | 
  342 | 		/** Port for proxy configuration of Netty HttpClient. */
  343 | 		private Integer port;
  344 | 
  345 | 		/** Username for proxy configuration of Netty HttpClient. */
  346 | 		private String username;
  347 | 
  348 | 		/** Password for proxy configuration of Netty HttpClient. */
  349 | 		private String password;
  350 | 
  351 | 		/**
  352 | 		 * Regular expression (Java) for a configured list of hosts. that should be
  353 | 		 * reached directly, bypassing the proxy
  354 | 		 */
  355 | 		private String nonProxyHostsPattern;
  356 | 
  357 | 		public ProxyProvider.Proxy getType() {
  358 | 			return type;
  359 | 		}
  360 | 
  361 | 		public void setType(ProxyProvider.Proxy type) {
  362 | 			this.type = type;
  363 | 		}
  364 | 
  365 | 		public String getHost() {
  366 | 			return host;
  367 | 		}
  368 | 
  369 | 		public void setHost(String host) {
  370 | 			this.host = host;
  371 | 		}
  372 | 
  373 | 		public Integer getPort() {
  374 | 			return port;
  375 | 		}
  376 | 
  377 | 		public void setPort(Integer port) {
  378 | 			this.port = port;
  379 | 		}
  380 | 
  381 | 		public String getUsername() {
  382 | 			return username;
  383 | 		}
  384 | 
  385 | 		public void setUsername(String username) {
  386 | 			this.username = username;
  387 | 		}
  388 | 
  389 | 		public String getPassword() {
  390 | 			return password;
  391 | 		}
  392 | 
  393 | 		public void setPassword(String password) {
  394 | 			this.password = password;
  395 | 		}
  396 | 
  397 | 		public String getNonProxyHostsPattern() {
  398 | 			return nonProxyHostsPattern;
  399 | 		}
  400 | 
  401 | 		public void setNonProxyHostsPattern(String nonProxyHostsPattern) {
  402 | 			this.nonProxyHostsPattern = nonProxyHostsPattern;
  403 | 		}
  404 | 
  405 | 		@Override
  406 | 		public String toString() {
  407 | 			return "Proxy{" + "type='" + type + '\'' + "host='" + host + '\'' + ", port=" + port + ", username='"
  408 | 					+ username + '\'' + ", password='" + password + '\'' + ", nonProxyHostsPattern='"
  409 | 					+ nonProxyHostsPattern + '\'' + '}';
  410 | 		}
  411 | 
  412 | 	}
  413 | 
  414 | 	public static class Ssl {
  415 | 
  416 | 		/**
  417 | 		 * Installs the netty InsecureTrustManagerFactory. This is insecure and not
  418 | 		 * suitable for production.
  419 | 		 */
  420 | 		private boolean useInsecureTrustManager = false;
  421 | 
  422 | 		/** Trusted certificates for verifying the remote endpoint's certificate. */
  423 | 		private List<String> trustedX509Certificates = new ArrayList<>();
  424 | 
  425 | 		/** The name of the SSL bundle to use. */
  426 | 		private String sslBundle;
  427 | 
  428 | 		// use netty default SSL timeouts
  429 | 		/** SSL handshake timeout. Default to 10000 ms */
  430 | 		private Duration handshakeTimeout = Duration.ofMillis(10000);
  431 | 
  432 | 		/** SSL close_notify flush timeout. Default to 3000 ms. */
  433 | 		private Duration closeNotifyFlushTimeout = Duration.ofMillis(3000);
  434 | 
  435 | 		/** SSL close_notify read timeout. Default to 0 ms. */
  436 | 		private Duration closeNotifyReadTimeout = Duration.ZERO;
  437 | 
  438 | 		/** Keystore path for Netty HttpClient. */
  439 | 		private String keyStore;
  440 | 
  441 | 		/** Keystore type for Netty HttpClient, default is JKS. */
  442 | 		private String keyStoreType = "JKS";
  443 | 
  444 | 		/** Keystore provider for Netty HttpClient, optional field. */
  445 | 		private String keyStoreProvider;
  446 | 
  447 | 		/** Keystore password. */
  448 | 		private String keyStorePassword;
  449 | 
  450 | 		/** Key password, default is same as keyStorePassword. */
  451 | 		private String keyPassword;
  452 | 
  453 | 		public String getKeyStorePassword() {
  454 | 			return keyStorePassword;
  455 | 		}
  456 | 
  457 | 		public void setKeyStorePassword(String keyStorePassword) {
  458 | 			this.keyStorePassword = keyStorePassword;
  459 | 		}
  460 | 
  461 | 		public String getKeyStoreType() {
  462 | 			return keyStoreType;
  463 | 		}
  464 | 
  465 | 		public void setKeyStoreType(String keyStoreType) {
  466 | 			this.keyStoreType = keyStoreType;
  467 | 		}
  468 | 
  469 | 		public String getKeyStoreProvider() {
  470 | 			return keyStoreProvider;
  471 | 		}
  472 | 
  473 | 		public void setKeyStoreProvider(String keyStoreProvider) {
  474 | 			this.keyStoreProvider = keyStoreProvider;
  475 | 		}
  476 | 
  477 | 		public String getKeyStore() {
  478 | 			return keyStore;
  479 | 		}
  480 | 
  481 | 		public void setKeyStore(String keyStore) {
  482 | 			this.keyStore = keyStore;
  483 | 		}
  484 | 
  485 | 		public String getKeyPassword() {
  486 | 			return keyPassword;
  487 | 		}
  488 | 
  489 | 		public void setKeyPassword(String keyPassword) {
  490 | 			this.keyPassword = keyPassword;
  491 | 		}
  492 | 
  493 | 		public List<String> getTrustedX509Certificates() {
  494 | 			return trustedX509Certificates;
  495 | 		}
  496 | 
  497 | 		public void setTrustedX509Certificates(List<String> trustedX509) {
  498 | 			this.trustedX509Certificates = trustedX509;
  499 | 		}
  500 | 
  501 | 		public boolean isUseInsecureTrustManager() {
  502 | 			return useInsecureTrustManager;
  503 | 		}
  504 | 
  505 | 		public void setUseInsecureTrustManager(boolean useInsecureTrustManager) {
  506 | 			this.useInsecureTrustManager = useInsecureTrustManager;
  507 | 		}
  508 | 
  509 | 		public Duration getHandshakeTimeout() {
  510 | 			return handshakeTimeout;
  511 | 		}
  512 | 
  513 | 		public void setHandshakeTimeout(Duration handshakeTimeout) {
  514 | 			this.handshakeTimeout = handshakeTimeout;
  515 | 		}
  516 | 
  517 | 		public Duration getCloseNotifyFlushTimeout() {
  518 | 			return closeNotifyFlushTimeout;
  519 | 		}
  520 | 
  521 | 		public void setCloseNotifyFlushTimeout(Duration closeNotifyFlushTimeout) {
  522 | 			this.closeNotifyFlushTimeout = closeNotifyFlushTimeout;
  523 | 		}
  524 | 
  525 | 		public Duration getCloseNotifyReadTimeout() {
  526 | 			return closeNotifyReadTimeout;
  527 | 		}
  528 | 
  529 | 		public void setCloseNotifyReadTimeout(Duration closeNotifyReadTimeout) {
  530 | 			this.closeNotifyReadTimeout = closeNotifyReadTimeout;
  531 | 		}
  532 | 
  533 | 		public String getSslBundle() {
  534 | 			return sslBundle;
  535 | 		}
  536 | 
  537 | 		public void setSslBundle(String sslBundle) {
  538 | 			this.sslBundle = sslBundle;
  539 | 		}
  540 | 
  541 | 		@Override
  542 | 		public String toString() {
  543 | 			return new ToStringCreator(this).append("useInsecureTrustManager", useInsecureTrustManager)
  544 | 				.append("trustedX509Certificates", trustedX509Certificates)
  545 | 				.append("handshakeTimeout", handshakeTimeout)
  546 | 				.append("closeNotifyFlushTimeout", closeNotifyFlushTimeout)
  547 | 				.append("closeNotifyReadTimeout", closeNotifyReadTimeout)
  548 | 				.toString();
  549 | 		}
  550 | 
  551 | 	}
  552 | 
  553 | 	public static class Websocket {
  554 | 
  555 | 		/** Max frame payload length. */
  556 | 		private Integer maxFramePayloadLength;
  557 | 
  558 | 		/** Proxy ping frames to downstream services, defaults to true. */
  559 | 		private boolean proxyPing = true;
  560 | 
  561 | 		public Integer getMaxFramePayloadLength() {
  562 | 			return this.maxFramePayloadLength;
  563 | 		}
  564 | 
  565 | 		public void setMaxFramePayloadLength(Integer maxFramePayloadLength) {
  566 | 			this.maxFramePayloadLength = maxFramePayloadLength;
  567 | 		}
  568 | 
  569 | 		public boolean isProxyPing() {
  570 | 			return proxyPing;
  571 | 		}
  572 | 
  573 | 		public void setProxyPing(boolean proxyPing) {
  574 | 			this.proxyPing = proxyPing;
  575 | 		}
  576 | 
  577 | 		@Override
  578 | 		public String toString() {
  579 | 			return new ToStringCreator(this).append("maxFramePayloadLength", maxFramePayloadLength)
  580 | 				.append("proxyPing", proxyPing)
  581 | 				.toString();
  582 | 		}
  583 | 
  584 | 	}
  585 | 
  586 | }
```
