<!-- learning-docs source archive（机械抓取原文，禁止手改代码内容） -->
<!-- origin: org.springframework.boot:spring-boot-webmvc:4.2.0 | path: org/springframework/boot/webmvc/autoconfigure/WebMvcProperties.java -->
<!-- 正文引用：[[源码:WebMvcProperties.java#L起-L止]] ；代码块头部：(src: WebMvcProperties.java#L起-L止) -->

# WebMvcProperties.java

| 项 | 值 |
| --- | --- |
| 来源 | `org.springframework.boot:spring-boot-webmvc:4.2.0` |
| 路径 | `org/springframework/boot/webmvc/autoconfigure/WebMvcProperties.java` |
| 行数 | 586 |
| 在线地址 | https://repo1.maven.org/maven2/org/springframework/boot/spring-boot-webmvc/4.2.0/spring-boot-webmvc-4.2.0-sources.jar |
| 抓取时间 | 2026-11-25T14:01:01.870Z |

```java
    1 | /*
    2 |  * Copyright 2012-present the original author or authors.
    3 |  */
    4 | 
    5 | package org.springframework.boot.webmvc.autoconfigure;
    6 | 
    7 | import java.time.Duration;
    8 | import java.util.ArrayList;
    9 | import java.util.LinkedHashMap;
   10 | import java.util.List;
   11 | import java.util.Map;
   12 | 
   13 | import org.jspecify.annotations.Nullable;
   14 | 
   15 | import org.springframework.boot.context.properties.ConfigurationProperties;
   16 | import org.springframework.boot.context.properties.DeprecatedConfigurationProperty;
   17 | import org.springframework.boot.context.properties.bind.Name;
   18 | import org.springframework.http.MediaType;
   19 | import org.springframework.util.Assert;
   20 | import org.springframework.validation.DefaultMessageCodesResolver;
   21 | 
   22 | /**
   23 |  * {@link ConfigurationProperties Properties} for Spring MVC.
   24 |  *
   25 |  * @author Phillip Webb
   26 |  * @author Sébastien Deleuze
   27 |  * @author Stephane Nicoll
   28 |  * @author Eddú Meléndez
   29 |  * @author Brian Clozel
   30 |  * @author Vedran Pavic
   31 |  * @since 4.0.0
   32 |  */
   33 | @ConfigurationProperties("spring.mvc")
   34 | public class WebMvcProperties {
   35 | 
   36 | 	/**
   37 | 	 * Formatting strategy for message codes. For instance, 'PREFIX_ERROR_CODE'.
   38 | 	 */
   39 | 	private DefaultMessageCodesResolver.@Nullable Format messageCodesResolverFormat;
   40 | 
   41 | 	private final Format format = new Format();
   42 | 
   43 | 	/**
   44 | 	 * Whether to dispatch TRACE requests to the FrameworkServlet doService method.
   45 | 	 */
   46 | 	private boolean dispatchTraceRequest;
   47 | 
   48 | 	/**
   49 | 	 * Whether to dispatch OPTIONS requests to the FrameworkServlet doService method.
   50 | 	 */
   51 | 	private boolean dispatchOptionsRequest = true;
   52 | 
   53 | 	/**
   54 | 	 * Whether to publish a ServletRequestHandledEvent at the end of each request.
   55 | 	 */
   56 | 	private boolean publishRequestHandledEvents = true;
   57 | 
   58 | 	/**
   59 | 	 * Whether logging of (potentially sensitive) request details at DEBUG and TRACE level
   60 | 	 * is allowed.
   61 | 	 */
   62 | 	private boolean logRequestDetails;
   63 | 
   64 | 	/**
   65 | 	 * Whether to enable warn logging of exceptions resolved by a
   66 | 	 * "HandlerExceptionResolver", except for "DefaultHandlerExceptionResolver".
   67 | 	 */
   68 | 	private boolean logResolvedException;
   69 | 
   70 | 	/**
   71 | 	 * Path pattern used for static resources.
   72 | 	 */
   73 | 	private String staticPathPattern = "/**";
   74 | 
   75 | 	/**
   76 | 	 * Path pattern used for WebJar assets.
   77 | 	 */
   78 | 	private String webjarsPathPattern = "/webjars/**";
   79 | 
   80 | 	private final Async async = new Async();
   81 | 
   82 | 	private final Servlet servlet = new Servlet();
   83 | 
   84 | 	private final View view = new View();
   85 | 
   86 | 	private final Contentnegotiation contentnegotiation = new Contentnegotiation();
   87 | 
   88 | 	@Deprecated(since = "4.2.0", forRemoval = true)
   89 | 	private final Pathmatch pathmatch = new Pathmatch();
   90 | 
   91 | 	private final Problemdetails problemdetails = new Problemdetails();
   92 | 
   93 | 	private final Apiversion apiversion = new Apiversion();
   94 | 
   95 | 	public DefaultMessageCodesResolver.@Nullable Format getMessageCodesResolverFormat() {
   96 | 		return this.messageCodesResolverFormat;
   97 | 	}
   98 | 
   99 | 	public void setMessageCodesResolverFormat(DefaultMessageCodesResolver.@Nullable Format messageCodesResolverFormat) {
  100 | 		this.messageCodesResolverFormat = messageCodesResolverFormat;
  101 | 	}
  102 | 
  103 | 	public Format getFormat() {
  104 | 		return this.format;
  105 | 	}
  106 | 
  107 | 	public boolean isPublishRequestHandledEvents() {
  108 | 		return this.publishRequestHandledEvents;
  109 | 	}
  110 | 
  111 | 	public void setPublishRequestHandledEvents(boolean publishRequestHandledEvents) {
  112 | 		this.publishRequestHandledEvents = publishRequestHandledEvents;
  113 | 	}
  114 | 
  115 | 	public boolean isLogRequestDetails() {
  116 | 		return this.logRequestDetails;
  117 | 	}
  118 | 
  119 | 	public void setLogRequestDetails(boolean logRequestDetails) {
  120 | 		this.logRequestDetails = logRequestDetails;
  121 | 	}
  122 | 
  123 | 	public boolean isLogResolvedException() {
  124 | 		return this.logResolvedException;
  125 | 	}
  126 | 
  127 | 	public void setLogResolvedException(boolean logResolvedException) {
  128 | 		this.logResolvedException = logResolvedException;
  129 | 	}
  130 | 
  131 | 	public boolean isDispatchOptionsRequest() {
  132 | 		return this.dispatchOptionsRequest;
  133 | 	}
  134 | 
  135 | 	public void setDispatchOptionsRequest(boolean dispatchOptionsRequest) {
  136 | 		this.dispatchOptionsRequest = dispatchOptionsRequest;
  137 | 	}
  138 | 
  139 | 	public boolean isDispatchTraceRequest() {
  140 | 		return this.dispatchTraceRequest;
  141 | 	}
  142 | 
  143 | 	public void setDispatchTraceRequest(boolean dispatchTraceRequest) {
  144 | 		this.dispatchTraceRequest = dispatchTraceRequest;
  145 | 	}
  146 | 
  147 | 	public String getStaticPathPattern() {
  148 | 		return this.staticPathPattern;
  149 | 	}
  150 | 
  151 | 	public void setStaticPathPattern(String staticPathPattern) {
  152 | 		this.staticPathPattern = staticPathPattern;
  153 | 	}
  154 | 
  155 | 	public String getWebjarsPathPattern() {
  156 | 		return this.webjarsPathPattern;
  157 | 	}
  158 | 
  159 | 	public void setWebjarsPathPattern(String webjarsPathPattern) {
  160 | 		this.webjarsPathPattern = webjarsPathPattern;
  161 | 	}
  162 | 
  163 | 	public Async getAsync() {
  164 | 		return this.async;
  165 | 	}
  166 | 
  167 | 	public Servlet getServlet() {
  168 | 		return this.servlet;
  169 | 	}
  170 | 
  171 | 	public View getView() {
  172 | 		return this.view;
  173 | 	}
  174 | 
  175 | 	public Contentnegotiation getContentnegotiation() {
  176 | 		return this.contentnegotiation;
  177 | 	}
  178 | 
  179 | 	@Deprecated(since = "4.2.0", forRemoval = true)
  180 | 	public Pathmatch getPathmatch() {
  181 | 		return this.pathmatch;
  182 | 	}
  183 | 
  184 | 	public Problemdetails getProblemdetails() {
  185 | 		return this.problemdetails;
  186 | 	}
  187 | 
  188 | 	public Apiversion getApiversion() {
  189 | 		return this.apiversion;
  190 | 	}
  191 | 
  192 | 	public static class Async {
  193 | 
  194 | 		/**
  195 | 		 * Amount of time before asynchronous request handling times out. If this value is
  196 | 		 * not set, the default timeout of the underlying implementation is used.
  197 | 		 */
  198 | 		private @Nullable Duration requestTimeout;
  199 | 
  200 | 		public @Nullable Duration getRequestTimeout() {
  201 | 			return this.requestTimeout;
  202 | 		}
  203 | 
  204 | 		public void setRequestTimeout(@Nullable Duration requestTimeout) {
  205 | 			this.requestTimeout = requestTimeout;
  206 | 		}
  207 | 
  208 | 	}
  209 | 
  210 | 	public static class Servlet {
  211 | 
  212 | 		/**
  213 | 		 * Path of the dispatcher servlet. Setting a custom value for this property is not
  214 | 		 * compatible with the PathPatternParser matching strategy.
  215 | 		 */
  216 | 		private String path = "/";
  217 | 
  218 | 		/**
  219 | 		 * Load on startup priority of the dispatcher servlet.
  220 | 		 */
  221 | 		private int loadOnStartup = -1;
  222 | 
  223 | 		public String getPath() {
  224 | 			return this.path;
  225 | 		}
  226 | 
  227 | 		public void setPath(String path) {
  228 | 			Assert.notNull(path, "'path' must not be null");
  229 | 			Assert.isTrue(!path.contains("*"), "'path' must not contain wildcards");
  230 | 			this.path = path;
  231 | 		}
  232 | 
  233 | 		public int getLoadOnStartup() {
  234 | 			return this.loadOnStartup;
  235 | 		}
  236 | 
  237 | 		public void setLoadOnStartup(int loadOnStartup) {
  238 | 			this.loadOnStartup = loadOnStartup;
  239 | 		}
  240 | 
  241 | 		public String getServletMapping() {
  242 | 			if (this.path.isEmpty() || this.path.equals("/")) {
  243 | 				return "/";
  244 | 			}
  245 | 			if (this.path.endsWith("/")) {
  246 | 				return this.path + "*";
  247 | 			}
  248 | 			return this.path + "/*";
  249 | 		}
  250 | 
  251 | 		public String getPath(String path) {
  252 | 			String prefix = getServletPrefix();
  253 | 			if (!path.startsWith("/")) {
  254 | 				path = "/" + path;
  255 | 			}
  256 | 			return prefix + path;
  257 | 		}
  258 | 
  259 | 		public String getServletPrefix() {
  260 | 			String result = this.path;
  261 | 			int index = result.indexOf('*');
  262 | 			if (index != -1) {
  263 | 				result = result.substring(0, index);
  264 | 			}
  265 | 			if (result.endsWith("/")) {
  266 | 				result = result.substring(0, result.length() - 1);
  267 | 			}
  268 | 			return result;
  269 | 		}
  270 | 
  271 | 	}
  272 | 
  273 | 	public static class View {
  274 | 
  275 | 		/**
  276 | 		 * Spring MVC view prefix.
  277 | 		 */
  278 | 		private @Nullable String prefix;
  279 | 
  280 | 		/**
  281 | 		 * Spring MVC view suffix.
  282 | 		 */
  283 | 		private @Nullable String suffix;
  284 | 
  285 | 		public @Nullable String getPrefix() {
  286 | 			return this.prefix;
  287 | 		}
  288 | 
  289 | 		public void setPrefix(@Nullable String prefix) {
  290 | 			this.prefix = prefix;
  291 | 		}
  292 | 
  293 | 		public @Nullable String getSuffix() {
  294 | 			return this.suffix;
  295 | 		}
  296 | 
  297 | 		public void setSuffix(@Nullable String suffix) {
  298 | 			this.suffix = suffix;
  299 | 		}
  300 | 
  301 | 	}
  302 | 
  303 | 	public static class Contentnegotiation {
  304 | 
  305 | 		/**
  306 | 		 * Whether a request parameter ("format" by default) should be used to determine
  307 | 		 * the requested media type.
  308 | 		 */
  309 | 		private boolean favorParameter;
  310 | 
  311 | 		/**
  312 | 		 * Query parameter name to use when "favor-parameter" is enabled.
  313 | 		 */
  314 | 		private @Nullable String parameterName;
  315 | 
  316 | 		/**
  317 | 		 * Map file extensions to media types for content negotiation. For instance, yml
  318 | 		 * to text/yaml.
  319 | 		 */
  320 | 		private Map<String, MediaType> mediaTypes = new LinkedHashMap<>();
  321 | 
  322 | 		/**
  323 | 		 * List of default content types to be used when no specific content type is
  324 | 		 * requested.
  325 | 		 */
  326 | 		private List<MediaType> defaultContentTypes = new ArrayList<>();
  327 | 
  328 | 		public boolean isFavorParameter() {
  329 | 			return this.favorParameter;
  330 | 		}
  331 | 
  332 | 		public void setFavorParameter(boolean favorParameter) {
  333 | 			this.favorParameter = favorParameter;
  334 | 		}
  335 | 
  336 | 		public @Nullable String getParameterName() {
  337 | 			return this.parameterName;
  338 | 		}
  339 | 
  340 | 		public void setParameterName(@Nullable String parameterName) {
  341 | 			this.parameterName = parameterName;
  342 | 		}
  343 | 
  344 | 		public Map<String, MediaType> getMediaTypes() {
  345 | 			return this.mediaTypes;
  346 | 		}
  347 | 
  348 | 		public void setMediaTypes(Map<String, MediaType> mediaTypes) {
  349 | 			this.mediaTypes = mediaTypes;
  350 | 		}
  351 | 
  352 | 		public List<MediaType> getDefaultContentTypes() {
  353 | 			return this.defaultContentTypes;
  354 | 		}
  355 | 
  356 | 		public void setDefaultContentTypes(List<MediaType> defaultContentTypes) {
  357 | 			this.defaultContentTypes = defaultContentTypes;
  358 | 		}
  359 | 
  360 | 	}
  361 | 
  362 | 	@Deprecated(since = "4.2.0", forRemoval = true)
  363 | 	public static class Pathmatch {
  364 | 
  365 | 		/**
  366 | 		 * Choice of strategy for matching request paths against registered mappings.
  367 | 		 */
  368 | 		private MatchingStrategy matchingStrategy = MatchingStrategy.PATH_PATTERN_PARSER;
  369 | 
  370 | 		@Deprecated(since = "4.2.0", forRemoval = true)
  371 | 		@DeprecatedConfigurationProperty(reason = "Path matching is no longer configurable", since = "4.2.0")
  372 | 		public MatchingStrategy getMatchingStrategy() {
  373 | 			return this.matchingStrategy;
  374 | 		}
  375 | 
  376 | 		@Deprecated(since = "4.2.0", forRemoval = true)
  377 | 		public void setMatchingStrategy(MatchingStrategy matchingStrategy) {
  378 | 			this.matchingStrategy = matchingStrategy;
  379 | 		}
  380 | 
  381 | 	}
  382 | 
  383 | 	public static class Format {
  384 | 
  385 | 		/**
  386 | 		 * Date format to use, for example 'dd/MM/yyyy'. Used for formatting of
  387 | 		 * java.util.Date and java.time.LocalDate.
  388 | 		 */
  389 | 		private @Nullable String date;
  390 | 
  391 | 		/**
  392 | 		 * Time format to use, for example 'HH:mm:ss'. Used for formatting of java.time's
  393 | 		 * LocalTime and OffsetTime.
  394 | 		 */
  395 | 		private @Nullable String time;
  396 | 
  397 | 		/**
  398 | 		 * Date-time format to use, for example 'yyyy-MM-dd HH:mm:ss'. Used for formatting
  399 | 		 * of java.time's LocalDateTime, OffsetDateTime, and ZonedDateTime.
  400 | 		 */
  401 | 		private @Nullable String dateTime;
  402 | 
  403 | 		public @Nullable String getDate() {
  404 | 			return this.date;
  405 | 		}
  406 | 
  407 | 		public void setDate(@Nullable String date) {
  408 | 			this.date = date;
  409 | 		}
  410 | 
  411 | 		public @Nullable String getTime() {
  412 | 			return this.time;
  413 | 		}
  414 | 
  415 | 		public void setTime(@Nullable String time) {
  416 | 			this.time = time;
  417 | 		}
  418 | 
  419 | 		public @Nullable String getDateTime() {
  420 | 			return this.dateTime;
  421 | 		}
  422 | 
  423 | 		public void setDateTime(@Nullable String dateTime) {
  424 | 			this.dateTime = dateTime;
  425 | 		}
  426 | 
  427 | 	}
  428 | 
  429 | 	/**
  430 | 	 * Matching strategy options.
  431 | 	 */
  432 | 	public enum MatchingStrategy {
  433 | 
  434 | 		/**
  435 | 		 * Use the {@code PathPatternParser} implementation.
  436 | 		 */
  437 | 		PATH_PATTERN_PARSER
  438 | 
  439 | 	}
  440 | 
  441 | 	/**
  442 | 	 * Problem Details.
  443 | 	 */
  444 | 	public static class Problemdetails {
  445 | 
  446 | 		/**
  447 | 		 * Whether RFC 9457 Problem Details support should be enabled.
  448 | 		 */
  449 | 		private boolean enabled;
  450 | 
  451 | 		public boolean isEnabled() {
  452 | 			return this.enabled;
  453 | 		}
  454 | 
  455 | 		public void setEnabled(boolean enabled) {
  456 | 			this.enabled = enabled;
  457 | 		}
  458 | 
  459 | 	}
  460 | 
  461 | 	/**
  462 | 	 * API Version.
  463 | 	 */
  464 | 	public static class Apiversion {
  465 | 
  466 | 		/**
  467 | 		 * Whether the API version is required with each request.
  468 | 		 */
  469 | 		private @Nullable Boolean required;
  470 | 
  471 | 		/**
  472 | 		 * Default version that should be used for each request.
  473 | 		 */
  474 | 		@Name("default")
  475 | 		private @Nullable String defaultVersion;
  476 | 
  477 | 		/**
  478 | 		 * Supported versions.
  479 | 		 */
  480 | 		private @Nullable List<String> supported;
  481 | 
  482 | 		/**
  483 | 		 * Whether supported versions should be detected from controllers.
  484 | 		 */
  485 | 		private @Nullable Boolean detectSupported;
  486 | 
  487 | 		/**
  488 | 		 * How version details should be inserted into requests.
  489 | 		 */
  490 | 		private final Use use = new Use();
  491 | 
  492 | 		public @Nullable Boolean getRequired() {
  493 | 			return this.required;
  494 | 		}
  495 | 
  496 | 		public void setRequired(@Nullable Boolean required) {
  497 | 			this.required = required;
  498 | 		}
  499 | 
  500 | 		public @Nullable String getDefaultVersion() {
  501 | 			return this.defaultVersion;
  502 | 		}
  503 | 
  504 | 		public void setDefaultVersion(@Nullable String defaultVersion) {
  505 | 			this.defaultVersion = defaultVersion;
  506 | 		}
  507 | 
  508 | 		public @Nullable List<String> getSupported() {
  509 | 			return this.supported;
  510 | 		}
  511 | 
  512 | 		public void setSupported(@Nullable List<String> supported) {
  513 | 			this.supported = supported;
  514 | 		}
  515 | 
  516 | 		public @Nullable Boolean getDetectSupported() {
  517 | 			return this.detectSupported;
  518 | 		}
  519 | 
  520 | 		public void setDetectSupported(@Nullable Boolean detectSupported) {
  521 | 			this.detectSupported = detectSupported;
  522 | 		}
  523 | 
  524 | 		public Use getUse() {
  525 | 			return this.use;
  526 | 		}
  527 | 
  528 | 		public static class Use {
  529 | 
  530 | 			/**
  531 | 			 * Use the HTTP header with the given name to obtain the version.
  532 | 			 */
  533 | 			private @Nullable String header;
  534 | 
  535 | 			/**
  536 | 			 * Use the query parameter with the given name to obtain the version.
  537 | 			 */
  538 | 			private @Nullable String queryParameter;
  539 | 
  540 | 			/**
  541 | 			 * Use the path segment at the given index to obtain the version.
  542 | 			 */
  543 | 			private @Nullable Integer pathSegment;
  544 | 
  545 | 			/**
  546 | 			 * Use the media type parameter with the given name to obtain the version.
  547 | 			 */
  548 | 			private Map<MediaType, String> mediaTypeParameter = new LinkedHashMap<>();
  549 | 
  550 | 			public @Nullable String getHeader() {
  551 | 				return this.header;
  552 | 			}
  553 | 
  554 | 			public void setHeader(@Nullable String header) {
  555 | 				this.header = header;
  556 | 			}
  557 | 
  558 | 			public @Nullable String getQueryParameter() {
  559 | 				return this.queryParameter;
  560 | 			}
  561 | 
  562 | 			public void setQueryParameter(@Nullable String queryParameter) {
  563 | 				this.queryParameter = queryParameter;
  564 | 			}
  565 | 
  566 | 			public @Nullable Integer getPathSegment() {
  567 | 				return this.pathSegment;
  568 | 			}
  569 | 
  570 | 			public void setPathSegment(@Nullable Integer pathSegment) {
  571 | 				this.pathSegment = pathSegment;
  572 | 			}
  573 | 
  574 | 			public Map<MediaType, String> getMediaTypeParameter() {
  575 | 				return this.mediaTypeParameter;
  576 | 			}
  577 | 
  578 | 			public void setMediaTypeParameter(Map<MediaType, String> mediaTypeParameter) {
  579 | 				this.mediaTypeParameter = mediaTypeParameter;
  580 | 			}
  581 | 
  582 | 		}
  583 | 
  584 | 	}
  585 | 
  586 | }
```
