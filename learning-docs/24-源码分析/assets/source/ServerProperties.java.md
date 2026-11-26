<!-- learning-docs source archive（机械抓取原文，禁止手改代码内容） -->
<!-- origin: org.springframework.boot:spring-boot-web-server:4.2.0 | path: org/springframework/boot/web/server/autoconfigure/ServerProperties.java -->
<!-- 正文引用：[[源码:ServerProperties.java#L起-L止]] ；代码块头部：(src: ServerProperties.java#L起-L止) -->

# ServerProperties.java

| 项 | 值 |
| --- | --- |
| 来源 | `org.springframework.boot:spring-boot-web-server:4.2.0` |
| 路径 | `org/springframework/boot/web/server/autoconfigure/ServerProperties.java` |
| 行数 | 463 |
| 在线地址 | https://repo1.maven.org/maven2/org/springframework/boot/spring-boot-web-server/4.2.0/spring-boot-web-server-4.2.0-sources.jar |
| 抓取时间 | 2026-11-25T14:00:55.197Z |

```java
    1 | /*
    2 |  * Copyright 2012-present the original author or authors.
    3 |  */
    4 | 
    5 | package org.springframework.boot.web.server.autoconfigure;
    6 | 
    7 | import java.net.InetAddress;
    8 | import java.nio.charset.Charset;
    9 | import java.time.Duration;
   10 | import java.time.temporal.ChronoUnit;
   11 | import java.util.LinkedHashMap;
   12 | import java.util.Locale;
   13 | import java.util.Map;
   14 | 
   15 | import org.jspecify.annotations.Nullable;
   16 | 
   17 | import org.springframework.boot.context.properties.ConfigurationProperties;
   18 | import org.springframework.boot.context.properties.DeprecatedConfigurationProperty;
   19 | import org.springframework.boot.context.properties.NestedConfigurationProperty;
   20 | import org.springframework.boot.convert.DurationUnit;
   21 | import org.springframework.boot.web.server.Compression;
   22 | import org.springframework.boot.web.server.Cookie;
   23 | import org.springframework.boot.web.server.ForwardedRequestHeaders;
   24 | import org.springframework.boot.web.server.ForwardedRequestHeaders.Naming;
   25 | import org.springframework.boot.web.server.Http2;
   26 | import org.springframework.boot.web.server.MimeMappings;
   27 | import org.springframework.boot.web.server.Shutdown;
   28 | import org.springframework.boot.web.server.Ssl;
   29 | import org.springframework.boot.web.server.servlet.Jsp;
   30 | import org.springframework.boot.web.server.servlet.Session;
   31 | import org.springframework.util.StringUtils;
   32 | import org.springframework.util.unit.DataSize;
   33 | 
   34 | /**
   35 |  * {@link ConfigurationProperties @ConfigurationProperties} for a web server (e.g. port
   36 |  * and path settings).
   37 |  *
   38 |  * @author Dave Syer
   39 |  * @author Stephane Nicoll
   40 |  * @author Andy Wilkinson
   41 |  * @author Ivan Sopov
   42 |  * @author Marcos Barbero
   43 |  * @author Eddú Meléndez
   44 |  * @author Quinten De Swaef
   45 |  * @author Venil Noronha
   46 |  * @author Aurélien Leboulanger
   47 |  * @author Brian Clozel
   48 |  * @author Olivier Lamy
   49 |  * @author Chentao Qu
   50 |  * @author Artsiom Yudovin
   51 |  * @author Andrew McGhie
   52 |  * @author Rafiullah Hamedy
   53 |  * @author Dirk Deyne
   54 |  * @author HaiTao Zhang
   55 |  * @author Victor Mandujano
   56 |  * @author Chris Bono
   57 |  * @author Parviz Rozikov
   58 |  * @author Florian Storz
   59 |  * @author Michael Weidmann
   60 |  * @author Lasse Wulff
   61 |  * @since 4.0.0
   62 |  */
   63 | @ConfigurationProperties("server")
   64 | public class ServerProperties {
   65 | 
   66 | 	/**
   67 | 	 * Server HTTP port.
   68 | 	 */
   69 | 	private @Nullable Integer port;
   70 | 
   71 | 	/**
   72 | 	 * Network address to which the server should bind.
   73 | 	 */
   74 | 	private @Nullable InetAddress address;
   75 | 
   76 | 	/**
   77 | 	 * Strategy for handling X-Forwarded-* headers.
   78 | 	 * @deprecated since 4.2.0 for removal in 4.4.0
   79 | 	 */
   80 | 	@Deprecated(since = "4.2.0", forRemoval = true)
   81 | 	private @Nullable ForwardHeadersStrategy forwardHeadersStrategy;
   82 | 
   83 | 	/**
   84 | 	 * Value to use for the Server response header (if empty, no header is sent).
   85 | 	 */
   86 | 	private @Nullable String serverHeader;
   87 | 
   88 | 	/**
   89 | 	 * Maximum size of the HTTP request header. Refer to the documentation for your chosen
   90 | 	 * embedded server for details of exactly how this limit is applied. For example,
   91 | 	 * Netty applies the limit separately to each individual header in the request whereas
   92 | 	 * Tomcat applies the limit to the combined size of the request line and all of the
   93 | 	 * header names and values in the request.
   94 | 	 */
   95 | 	private DataSize maxHttpRequestHeaderSize = DataSize.ofKilobytes(8);
   96 | 
   97 | 	/**
   98 | 	 * Type of shutdown that the server will support.
   99 | 	 */
  100 | 	private Shutdown shutdown = Shutdown.GRACEFUL;
  101 | 
  102 | 	@NestedConfigurationProperty
  103 | 	private @Nullable Ssl ssl;
  104 | 
  105 | 	@NestedConfigurationProperty
  106 | 	private final Compression compression = new Compression();
  107 | 
  108 | 	/**
  109 | 	 * Custom MIME mappings in addition to the default MIME mappings.
  110 | 	 */
  111 | 	private final MimeMappings mimeMappings = new MimeMappings();
  112 | 
  113 | 	@NestedConfigurationProperty
  114 | 	private final Http2 http2 = new Http2();
  115 | 
  116 | 	private final Servlet servlet = new Servlet();
  117 | 
  118 | 	private final Reactive reactive = new Reactive();
  119 | 
  120 | 	private final Request request = new Request();
  121 | 
  122 | 	public @Nullable Integer getPort() {
  123 | 		return this.port;
  124 | 	}
  125 | 
  126 | 	public void setPort(@Nullable Integer port) {
  127 | 		this.port = port;
  128 | 	}
  129 | 
  130 | 	public @Nullable InetAddress getAddress() {
  131 | 		return this.address;
  132 | 	}
  133 | 
  134 | 	public void setAddress(@Nullable InetAddress address) {
  135 | 		this.address = address;
  136 | 	}
  137 | 
  138 | 	public @Nullable String getServerHeader() {
  139 | 		return this.serverHeader;
  140 | 	}
  141 | 
  142 | 	public void setServerHeader(@Nullable String serverHeader) {
  143 | 		this.serverHeader = serverHeader;
  144 | 	}
  145 | 
  146 | 	public DataSize getMaxHttpRequestHeaderSize() {
  147 | 		return this.maxHttpRequestHeaderSize;
  148 | 	}
  149 | 
  150 | 	public void setMaxHttpRequestHeaderSize(DataSize maxHttpRequestHeaderSize) {
  151 | 		this.maxHttpRequestHeaderSize = maxHttpRequestHeaderSize;
  152 | 	}
  153 | 
  154 | 	public Shutdown getShutdown() {
  155 | 		return this.shutdown;
  156 | 	}
  157 | 
  158 | 	public void setShutdown(Shutdown shutdown) {
  159 | 		this.shutdown = shutdown;
  160 | 	}
  161 | 
  162 | 	public @Nullable Ssl getSsl() {
  163 | 		return this.ssl;
  164 | 	}
  165 | 
  166 | 	public void setSsl(@Nullable Ssl ssl) {
  167 | 		this.ssl = ssl;
  168 | 	}
  169 | 
  170 | 	public Compression getCompression() {
  171 | 		return this.compression;
  172 | 	}
  173 | 
  174 | 	public MimeMappings getMimeMappings() {
  175 | 		return this.mimeMappings;
  176 | 	}
  177 | 
  178 | 	public void setMimeMappings(Map<String, String> customMappings) {
  179 | 		customMappings.forEach(this.mimeMappings::add);
  180 | 	}
  181 | 
  182 | 	public Http2 getHttp2() {
  183 | 		return this.http2;
  184 | 	}
  185 | 
  186 | 	public Servlet getServlet() {
  187 | 		return this.servlet;
  188 | 	}
  189 | 
  190 | 	public Reactive getReactive() {
  191 | 		return this.reactive;
  192 | 	}
  193 | 
  194 | 	public Request getRequest() {
  195 | 		return this.request;
  196 | 	}
  197 | 
  198 | 	@Deprecated(since = "4.2.0", forRemoval = true)
  199 | 	@DeprecatedConfigurationProperty(since = "4.2.0", replacement = "server.request.header.forwarded.strategy")
  200 | 	public @Nullable ForwardHeadersStrategy getForwardHeadersStrategy() {
  201 | 		return this.forwardHeadersStrategy;
  202 | 	}
  203 | 
  204 | 	@Deprecated(since = "4.2.0", forRemoval = true)
  205 | 	public void setForwardHeadersStrategy(@Nullable ForwardHeadersStrategy forwardHeadersStrategy) {
  206 | 		this.forwardHeadersStrategy = forwardHeadersStrategy;
  207 | 	}
  208 | 
  209 | 	/**
  210 | 	 * Servlet server properties.
  211 | 	 */
  212 | 	public static class Servlet {
  213 | 
  214 | 		/**
  215 | 		 * Servlet context init parameters.
  216 | 		 */
  217 | 		private final Map<String, String> contextParameters = new LinkedHashMap<>();
  218 | 
  219 | 		/**
  220 | 		 * Context path of the application.
  221 | 		 */
  222 | 		private @Nullable String contextPath;
  223 | 
  224 | 		/**
  225 | 		 * Display name of the application.
  226 | 		 */
  227 | 		private String applicationDisplayName = "application";
  228 | 
  229 | 		/**
  230 | 		 * Whether to register the default Servlet with the container.
  231 | 		 */
  232 | 		private boolean registerDefaultServlet;
  233 | 
  234 | 		private final Encoding encoding = new Encoding();
  235 | 
  236 | 		@NestedConfigurationProperty
  237 | 		private final Jsp jsp = new Jsp();
  238 | 
  239 | 		@NestedConfigurationProperty
  240 | 		private final Session session = new Session();
  241 | 
  242 | 		public @Nullable String getContextPath() {
  243 | 			return this.contextPath;
  244 | 		}
  245 | 
  246 | 		public void setContextPath(@Nullable String contextPath) {
  247 | 			this.contextPath = cleanContextPath(contextPath);
  248 | 		}
  249 | 
  250 | 		private @Nullable String cleanContextPath(@Nullable String contextPath) {
  251 | 			String candidate = null;
  252 | 			if (StringUtils.hasLength(contextPath)) {
  253 | 				candidate = contextPath.strip();
  254 | 			}
  255 | 			if (StringUtils.hasText(candidate) && candidate.endsWith("/")) {
  256 | 				return candidate.substring(0, candidate.length() - 1);
  257 | 			}
  258 | 			return candidate;
  259 | 		}
  260 | 
  261 | 		public String getApplicationDisplayName() {
  262 | 			return this.applicationDisplayName;
  263 | 		}
  264 | 
  265 | 		public void setApplicationDisplayName(String displayName) {
  266 | 			this.applicationDisplayName = displayName;
  267 | 		}
  268 | 
  269 | 		public boolean isRegisterDefaultServlet() {
  270 | 			return this.registerDefaultServlet;
  271 | 		}
  272 | 
  273 | 		public void setRegisterDefaultServlet(boolean registerDefaultServlet) {
  274 | 			this.registerDefaultServlet = registerDefaultServlet;
  275 | 		}
  276 | 
  277 | 		public Map<String, String> getContextParameters() {
  278 | 			return this.contextParameters;
  279 | 		}
  280 | 
  281 | 		public Encoding getEncoding() {
  282 | 			return this.encoding;
  283 | 		}
  284 | 
  285 | 		public Jsp getJsp() {
  286 | 			return this.jsp;
  287 | 		}
  288 | 
  289 | 		public Session getSession() {
  290 | 			return this.session;
  291 | 		}
  292 | 
  293 | 	}
  294 | 
  295 | 	/**
  296 | 	 * Reactive server properties.
  297 | 	 */
  298 | 	public static class Reactive {
  299 | 
  300 | 		private final Session session = new Session();
  301 | 
  302 | 		public Session getSession() {
  303 | 			return this.session;
  304 | 		}
  305 | 
  306 | 		public static class Session {
  307 | 
  308 | 			/**
  309 | 			 * Session timeout. If a duration suffix is not specified, seconds will be
  310 | 			 * used.
  311 | 			 */
  312 | 			@DurationUnit(ChronoUnit.SECONDS)
  313 | 			private Duration timeout = Duration.ofMinutes(30);
  314 | 
  315 | 			/**
  316 | 			 * Maximum number of sessions that can be stored.
  317 | 			 */
  318 | 			private int maxSessions = 10000;
  319 | 
  320 | 			@NestedConfigurationProperty
  321 | 			private final Cookie cookie = new Cookie();
  322 | 
  323 | 			public Duration getTimeout() {
  324 | 				return this.timeout;
  325 | 			}
  326 | 
  327 | 			public void setTimeout(Duration timeout) {
  328 | 				this.timeout = timeout;
  329 | 			}
  330 | 
  331 | 			public int getMaxSessions() {
  332 | 				return this.maxSessions;
  333 | 			}
  334 | 
  335 | 			public void setMaxSessions(int maxSessions) {
  336 | 				this.maxSessions = maxSessions;
  337 | 			}
  338 | 
  339 | 			public Cookie getCookie() {
  340 | 				return this.cookie;
  341 | 			}
  342 | 
  343 | 		}
  344 | 
  345 | 	}
  346 | 
  347 | 	/**
  348 | 	 * Request server properties.
  349 | 	 */
  350 | 	public static class Request {
  351 | 
  352 | 		private final Header header = new Header();
  353 | 
  354 | 		public Header getHeader() {
  355 | 			return this.header;
  356 | 		}
  357 | 
  358 | 		/**
  359 | 		 * Request header server properties.
  360 | 		 */
  361 | 		public static class Header {
  362 | 
  363 | 			private final Forwarded forwarded = new Forwarded();
  364 | 
  365 | 			public Forwarded getForwarded() {
  366 | 				return this.forwarded;
  367 | 			}
  368 | 
  369 | 			/**
  370 | 			 * Forwarded request header server properties.
  371 | 			 */
  372 | 			public static class Forwarded {
  373 | 
  374 | 				/**
  375 | 				 * Strategy to use to process the forwarded headers. If not specified, the
  376 | 				 * strategy will be deduced based on the cloud platform.
  377 | 				 */
  378 | 				private @Nullable ForwardedHeaderStrategy strategy;
  379 | 
  380 | 				/**
  381 | 				 * Naming of forwarded headers.
  382 | 				 */
  383 | 				private ForwardedRequestHeaders.Naming naming = Naming.X_FORWARDED;
  384 | 
  385 | 				/**
  386 | 				 * If the 'prefix' forward header should be used.
  387 | 				 */
  388 | 				private boolean usePrefix;
  389 | 
  390 | 				public @Nullable ForwardedHeaderStrategy getStrategy() {
  391 | 					return this.strategy;
  392 | 				}
  393 | 
  394 | 				public void setStrategy(@Nullable ForwardedHeaderStrategy strategy) {
  395 | 					this.strategy = strategy;
  396 | 				}
  397 | 
  398 | 				public ForwardedRequestHeaders.Naming getNaming() {
  399 | 					return this.naming;
  400 | 				}
  401 | 
  402 | 				public void setNaming(ForwardedRequestHeaders.Naming naming) {
  403 | 					this.naming = naming;
  404 | 				}
  405 | 
  406 | 				public boolean isUsePrefix() {
  407 | 					return this.usePrefix;
  408 | 				}
  409 | 
  410 | 				public void setUsePrefix(boolean usePrefix) {
  411 | 					this.usePrefix = usePrefix;
  412 | 				}
  413 | 
  414 | 			}
  415 | 
  416 | 		}
  417 | 
  418 | 	}
  419 | 
  420 | 	/**
  421 | 	 * Strategies for supporting forward headers.
  422 | 	 *
  423 | 	 * @deprecated since 4.2.0 for removal in 4.4.0 in favor of
  424 | 	 * {@link ForwardedHeaderStrategy}
  425 | 	 */
  426 | 	@Deprecated(since = "4.2.0", forRemoval = true)
  427 | 	public enum ForwardHeadersStrategy {
  428 | 
  429 | 		/**
  430 | 		 * Use the underlying container's native support for forwarded headers.
  431 | 		 */
  432 | 		NATIVE,
  433 | 
  434 | 		/**
  435 | 		 * Use Spring's support for handling forwarded headers.
  436 | 		 */
  437 | 		FRAMEWORK,
  438 | 
  439 | 		/**
  440 | 		 * Ignore X-Forwarded-* headers.
  441 | 		 */
  442 | 		NONE
  443 | 
  444 | 	}
  445 | 
  446 | 	public static class Encoding {
  447 | 
  448 | 		/**
  449 | 		 * Mapping of locale to charset for response encoding.
  450 | 		 */
  451 | 		private @Nullable Map<Locale, Charset> mapping;
  452 | 
  453 | 		public @Nullable Map<Locale, Charset> getMapping() {
  454 | 			return this.mapping;
  455 | 		}
  456 | 
  457 | 		public void setMapping(@Nullable Map<Locale, Charset> mapping) {
  458 | 			this.mapping = mapping;
  459 | 		}
  460 | 
  461 | 	}
  462 | 
  463 | }
```
