<!-- learning-docs source archive（机械抓取原文，禁止手改代码内容） -->
<!-- origin: org.springframework:spring-webmvc:7.1.0 | path: org/springframework/web/servlet/handler/AbstractHandlerMapping.java -->
<!-- 正文引用：[[源码:AbstractHandlerMapping.java#L起-L止]] ；代码块头部：(src: AbstractHandlerMapping.java#L起-L止) -->

# AbstractHandlerMapping.java

| 项 | 值 |
| --- | --- |
| 来源 | `org.springframework:spring-webmvc:7.1.0` |
| 路径 | `org/springframework/web/servlet/handler/AbstractHandlerMapping.java` |
| 行数 | 824 |
| 在线地址 | https://repo1.maven.org/maven2/org/springframework/spring-webmvc/7.1.0/spring-webmvc-7.1.0-sources.jar |
| 抓取时间 | 2026-11-25T14:01:09.968Z |

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
   17 | package org.springframework.web.servlet.handler;
   18 | 
   19 | import java.io.IOException;
   20 | import java.util.ArrayList;
   21 | import java.util.Arrays;
   22 | import java.util.List;
   23 | import java.util.Map;
   24 | 
   25 | import jakarta.servlet.DispatcherType;
   26 | import jakarta.servlet.http.HttpServletRequest;
   27 | import jakarta.servlet.http.HttpServletResponse;
   28 | import org.apache.commons.logging.Log;
   29 | import org.jspecify.annotations.Nullable;
   30 | 
   31 | import org.springframework.beans.BeansException;
   32 | import org.springframework.beans.factory.BeanFactoryUtils;
   33 | import org.springframework.beans.factory.BeanNameAware;
   34 | import org.springframework.core.Ordered;
   35 | import org.springframework.core.log.LogDelegateFactory;
   36 | import org.springframework.http.server.RequestPath;
   37 | import org.springframework.util.AntPathMatcher;
   38 | import org.springframework.util.Assert;
   39 | import org.springframework.util.CollectionUtils;
   40 | import org.springframework.util.PathMatcher;
   41 | import org.springframework.web.HttpRequestHandler;
   42 | import org.springframework.web.accept.ApiVersionHolder;
   43 | import org.springframework.web.accept.ApiVersionStrategy;
   44 | import org.springframework.web.context.request.WebRequestInterceptor;
   45 | import org.springframework.web.context.request.async.WebAsyncManager;
   46 | import org.springframework.web.context.request.async.WebAsyncUtils;
   47 | import org.springframework.web.context.support.WebApplicationObjectSupport;
   48 | import org.springframework.web.cors.CorsConfiguration;
   49 | import org.springframework.web.cors.CorsConfigurationSource;
   50 | import org.springframework.web.cors.CorsProcessor;
   51 | import org.springframework.web.cors.CorsUtils;
   52 | import org.springframework.web.cors.DefaultCorsProcessor;
   53 | import org.springframework.web.cors.PreFlightRequestHandler;
   54 | import org.springframework.web.cors.UrlBasedCorsConfigurationSource;
   55 | import org.springframework.web.servlet.DispatcherServlet;
   56 | import org.springframework.web.servlet.HandlerExecutionChain;
   57 | import org.springframework.web.servlet.HandlerInterceptor;
   58 | import org.springframework.web.servlet.HandlerMapping;
   59 | import org.springframework.web.util.ServletRequestPathUtils;
   60 | import org.springframework.web.util.UrlPathHelper;
   61 | import org.springframework.web.util.pattern.PathPattern;
   62 | import org.springframework.web.util.pattern.PathPatternParser;
   63 | 
   64 | /**
   65 |  * Abstract base class for {@link org.springframework.web.servlet.HandlerMapping}
   66 |  * implementations. Supports ordering, a default handler, and handler interceptors,
   67 |  * including handler interceptors mapped by path patterns.
   68 |  *
   69 |  * <p>Note: This base class does <i>not</i> support exposure of the
   70 |  * {@link #PATH_WITHIN_HANDLER_MAPPING_ATTRIBUTE}. Support for this attribute
   71 |  * is up to concrete subclasses, typically based on request URL mappings.
   72 |  *
   73 |  * @author Juergen Hoeller
   74 |  * @author Rossen Stoyanchev
   75 |  * @author Sam Brannen
   76 |  * @since 07.04.2003
   77 |  * @see #getHandlerInternal
   78 |  * @see #setDefaultHandler
   79 |  * @see #setInterceptors
   80 |  * @see org.springframework.web.servlet.HandlerInterceptor
   81 |  */
   82 | public abstract class AbstractHandlerMapping extends WebApplicationObjectSupport
   83 | 		implements HandlerMapping, Ordered, BeanNameAware {
   84 | 
   85 | 	static final String SUPPRESS_LOGGING_ATTRIBUTE = AbstractHandlerMapping.class.getName() + ".SUPPRESS_LOGGING";
   86 | 
   87 | 
   88 | 	/** Dedicated "hidden" logger for request mappings. */
   89 | 	protected final Log mappingsLogger =
   90 | 			LogDelegateFactory.getHiddenLog(HandlerMapping.class.getName() + ".Mappings");
   91 | 
   92 | 
   93 | 	private @Nullable Object defaultHandler;
   94 | 
   95 | 	private @Nullable PathPatternParser patternParser = new PathPatternParser();
   96 | 
   97 | 	private UrlPathHelper urlPathHelper = new UrlPathHelper();
   98 | 
   99 | 	private PathMatcher pathMatcher = new AntPathMatcher();
  100 | 
  101 | 	private final List<Object> interceptors = new ArrayList<>();
  102 | 
  103 | 	private final List<HandlerInterceptor> adaptedInterceptors = new ArrayList<>();
  104 | 
  105 | 	private @Nullable CorsConfigurationSource corsConfigurationSource;
  106 | 
  107 | 	private CorsProcessor corsProcessor = new DefaultCorsProcessor();
  108 | 
  109 | 	private @Nullable ApiVersionStrategy versionStrategy;
  110 | 
  111 | 	private int order = Ordered.LOWEST_PRECEDENCE;  // default: same as non-Ordered
  112 | 
  113 | 	private @Nullable String beanName;
  114 | 
  115 | 
  116 | 	/**
  117 | 	 * Set the default handler for this handler mapping.
  118 | 	 * This handler will be returned if no specific mapping was found.
  119 | 	 * <p>Default is {@code null}, indicating no default handler.
  120 | 	 */
  121 | 	public void setDefaultHandler(@Nullable Object defaultHandler) {
  122 | 		this.defaultHandler = defaultHandler;
  123 | 	}
  124 | 
  125 | 	/**
  126 | 	 * Return the default handler for this handler mapping,
  127 | 	 * or {@code null} if none.
  128 | 	 */
  129 | 	public @Nullable Object getDefaultHandler() {
  130 | 		return this.defaultHandler;
  131 | 	}
  132 | 
  133 | 	/**
  134 | 	 * Set the {@link PathPatternParser} to parse {@link PathPattern patterns}
  135 | 	 * with for URL path matching. Parsed patterns provide a more modern and
  136 | 	 * efficient alternative to String path matching via {@link AntPathMatcher}.
  137 | 	 * <p><strong>Note:</strong> This property is mutually exclusive with the
  138 | 	 * below properties, all of which are not necessary for parsed patterns and
  139 | 	 * are ignored when a {@code PathPatternParser} is available:
  140 | 	 * <ul>
  141 | 	 * <li>{@link #setAlwaysUseFullPath} -- parsed patterns always use the
  142 | 	 * full path and consider the servletPath only when a Servlet is mapped by
  143 | 	 * path prefix.
  144 | 	 * <li>{@link #setRemoveSemicolonContent} -- parsed patterns always
  145 | 	 * ignore semicolon content for path matching purposes, but path parameters
  146 | 	 * remain available for use in controllers via {@code @MatrixVariable}.
  147 | 	 * <li>{@link #setUrlDecode} -- parsed patterns match one decoded path
  148 | 	 * segment at a time and therefore don't need to decode the full path.
  149 | 	 * <li>{@link #setUrlPathHelper} -- for parsed patterns, the request path
  150 | 	 * is parsed once in {@link org.springframework.web.servlet.DispatcherServlet
  151 | 	 * DispatcherServlet} or in
  152 | 	 * {@link org.springframework.web.filter.ServletRequestPathFilter
  153 | 	 * ServletRequestPathFilter} using {@link ServletRequestPathUtils} and cached
  154 | 	 * in a request attribute.
  155 | 	 * <li>{@link #setPathMatcher} -- a parsed patterns encapsulates the logic
  156 | 	 * for path matching and does need a {@code PathMatcher}.
  157 | 	 * </ul>
  158 | 	 * <p>By default, as of 6.0, this is set to a {@link PathPatternParser}
  159 | 	 * instance with default settings and therefore use of parsed patterns is
  160 | 	 * enabled. Set this to {@code null} to switch to String path matching
  161 | 	 * via {@link AntPathMatcher} instead.
  162 | 	 * @param patternParser the parser to use
  163 | 	 * @since 5.3
  164 | 	 */
  165 | 	public void setPatternParser(@Nullable PathPatternParser patternParser) {
  166 | 		this.patternParser = patternParser;
  167 | 	}
  168 | 
  169 | 	/**
  170 | 	 * Return the {@link #setPatternParser(PathPatternParser) configured}
  171 | 	 * {@code PathPatternParser}, or {@code null} otherwise which indicates that
  172 | 	 * String pattern matching with {@link AntPathMatcher} is enabled instead.
  173 | 	 * @since 5.3
  174 | 	 */
  175 | 	public @Nullable PathPatternParser getPatternParser() {
  176 | 		return this.patternParser;
  177 | 	}
  178 | 
  179 | 	/**
  180 | 	 * Shortcut to same property on the configured {@code UrlPathHelper}.
  181 | 	 * <p><strong>Note:</strong> This property is mutually exclusive with and
  182 | 	 * ignored when {@link #setPatternParser(PathPatternParser)} is set.
  183 | 	 * @see org.springframework.web.util.UrlPathHelper#setAlwaysUseFullPath(boolean)
  184 | 	 * @deprecated as of 6.0, in favor of using {@link #setUrlPathHelper(UrlPathHelper)}
  185 | 	 */
  186 | 	@SuppressWarnings("removal")
  187 | 	@Deprecated(since = "6.0")
  188 | 	public void setAlwaysUseFullPath(boolean alwaysUseFullPath) {
  189 | 		this.urlPathHelper.setAlwaysUseFullPath(alwaysUseFullPath);
  190 | 		if (this.corsConfigurationSource instanceof UrlBasedCorsConfigurationSource urlConfigSource) {
  191 | 			urlConfigSource.setAlwaysUseFullPath(alwaysUseFullPath);
  192 | 		}
  193 | 	}
  194 | 
  195 | 	/**
  196 | 	 * Shortcut to same property on the underlying {@code UrlPathHelper}.
  197 | 	 * <p><strong>Note:</strong> This property is mutually exclusive with and
  198 | 	 * ignored when {@link #setPatternParser(PathPatternParser)} is set.
  199 | 	 * @see org.springframework.web.util.UrlPathHelper#setUrlDecode(boolean)
  200 | 	 * @deprecated as of 6.0, in favor of using {@link #setUrlPathHelper(UrlPathHelper)}
  201 | 	 */
  202 | 	@SuppressWarnings("removal")
  203 | 	@Deprecated(since = "6.0")
  204 | 	public void setUrlDecode(boolean urlDecode) {
  205 | 		this.urlPathHelper.setUrlDecode(urlDecode);
  206 | 		if (this.corsConfigurationSource instanceof UrlBasedCorsConfigurationSource urlConfigSource) {
  207 | 			urlConfigSource.setUrlDecode(urlDecode);
  208 | 		}
  209 | 	}
  210 | 
  211 | 	/**
  212 | 	 * Shortcut to same property on the underlying {@code UrlPathHelper}.
  213 | 	 * <p><strong>Note:</strong> This property is mutually exclusive with and
  214 | 	 * ignored when {@link #setPatternParser(PathPatternParser)} is set.
  215 | 	 * @see org.springframework.web.util.UrlPathHelper#setRemoveSemicolonContent(boolean)
  216 | 	 * @deprecated as of 6.0, in favor of using {@link #setUrlPathHelper(UrlPathHelper)}
  217 | 	 */
  218 | 	@SuppressWarnings("removal")
  219 | 	@Deprecated(since = "6.0")
  220 | 	public void setRemoveSemicolonContent(boolean removeSemicolonContent) {
  221 | 		this.urlPathHelper.setRemoveSemicolonContent(removeSemicolonContent);
  222 | 		if (this.corsConfigurationSource instanceof UrlBasedCorsConfigurationSource urlConfigSource) {
  223 | 			urlConfigSource.setRemoveSemicolonContent(removeSemicolonContent);
  224 | 		}
  225 | 	}
  226 | 
  227 | 	/**
  228 | 	 * Configure the UrlPathHelper to use for resolution of lookup paths.
  229 | 	 * <p><strong>Note:</strong> This property is mutually exclusive with and
  230 | 	 * ignored when {@link #setPatternParser(PathPatternParser)} is set.
  231 | 	 * @deprecated use of {@link PathMatcher} and {@link UrlPathHelper} is deprecated
  232 | 	 * for use at runtime in web modules in favor of parsed patterns with
  233 | 	 * {@link PathPatternParser}.
  234 | 	 */
  235 | 	@SuppressWarnings("removal")
  236 | 	@Deprecated(since = "7.0", forRemoval = true)
  237 | 	public void setUrlPathHelper(UrlPathHelper urlPathHelper) {
  238 | 		Assert.notNull(urlPathHelper, "UrlPathHelper must not be null");
  239 | 		this.urlPathHelper = urlPathHelper;
  240 | 		if (this.corsConfigurationSource instanceof UrlBasedCorsConfigurationSource urlConfigSource) {
  241 | 			urlConfigSource.setUrlPathHelper(urlPathHelper);
  242 | 		}
  243 | 	}
  244 | 
  245 | 	/**
  246 | 	 * Return the {@link #setUrlPathHelper configured} {@code UrlPathHelper}.
  247 | 	 * @deprecated use of {@link PathMatcher} and {@link UrlPathHelper} is deprecated
  248 | 	 * for use at runtime in web modules in favor of parsed patterns with
  249 | 	 * {@link PathPatternParser}.
  250 | 	 */
  251 | 	@Deprecated(since = "7.0", forRemoval = true)
  252 | 	public UrlPathHelper getUrlPathHelper() {
  253 | 		return this.urlPathHelper;
  254 | 	}
  255 | 
  256 | 	/**
  257 | 	 * Configure the PathMatcher to use.
  258 | 	 * <p><strong>Note:</strong> This property is mutually exclusive with and
  259 | 	 * ignored when {@link #setPatternParser(PathPatternParser)} is set.
  260 | 	 * <p>By default, this is {@link AntPathMatcher}.
  261 | 	 * @see org.springframework.util.AntPathMatcher
  262 | 	 * @deprecated use of {@link PathMatcher} and {@link UrlPathHelper} is deprecated
  263 | 	 * for use at runtime in web modules in favor of parsed patterns with
  264 | 	 * {@link PathPatternParser}.
  265 | 	 */
  266 | 	@SuppressWarnings("removal")
  267 | 	@Deprecated(since = "7.0", forRemoval = true)
  268 | 	public void setPathMatcher(PathMatcher pathMatcher) {
  269 | 		Assert.notNull(pathMatcher, "PathMatcher must not be null");
  270 | 		this.pathMatcher = pathMatcher;
  271 | 		if (this.corsConfigurationSource instanceof UrlBasedCorsConfigurationSource urlConfigSource) {
  272 | 			urlConfigSource.setPathMatcher(pathMatcher);
  273 | 		}
  274 | 	}
  275 | 
  276 | 	/**
  277 | 	 * Return the {@link #setPathMatcher configured} {@code PathMatcher}.
  278 | 	 * @deprecated use of {@link PathMatcher} and {@link UrlPathHelper} is deprecated
  279 | 	 * for use at runtime in web modules in favor of parsed patterns with
  280 | 	 * {@link PathPatternParser}.
  281 | 	 */
  282 | 	@Deprecated(since = "7.0", forRemoval = true)
  283 | 	public PathMatcher getPathMatcher() {
  284 | 		return this.pathMatcher;
  285 | 	}
  286 | 
  287 | 	/**
  288 | 	 * Set the interceptors to apply for all handlers mapped by this handler mapping.
  289 | 	 * <p>Supported interceptor types are {@link HandlerInterceptor},
  290 | 	 * {@link WebRequestInterceptor}, and {@link MappedInterceptor}.
  291 | 	 * Mapped interceptors apply only to request URLs that match its path patterns.
  292 | 	 * Mapped interceptor beans are also detected by type during initialization.
  293 | 	 * @param interceptors array of handler interceptors
  294 | 	 * @see #adaptInterceptor
  295 | 	 * @see org.springframework.web.servlet.HandlerInterceptor
  296 | 	 * @see org.springframework.web.context.request.WebRequestInterceptor
  297 | 	 * @see MappedInterceptor
  298 | 	 */
  299 | 	public void setInterceptors(Object... interceptors) {
  300 | 		this.interceptors.addAll(Arrays.asList(interceptors));
  301 | 	}
  302 | 
  303 | 	/**
  304 | 	 * Return all configured interceptors adapted to {@link HandlerInterceptor}.
  305 | 	 * @return the array of configured interceptors, or {@code null} if none
  306 | 	 * are configured; this method also returns {@code null} if called too early,
  307 | 	 * or more specifically before
  308 | 	 * {@link org.springframework.context.ApplicationContextAware#setApplicationContext}.
  309 | 	 */
  310 | 	public final HandlerInterceptor @Nullable [] getAdaptedInterceptors() {
  311 | 		return (!this.adaptedInterceptors.isEmpty() ?
  312 | 				this.adaptedInterceptors.toArray(new HandlerInterceptor[0]) : null);
  313 | 	}
  314 | 
  315 | 	/**
  316 | 	 * Return all configured {@link MappedInterceptor}s as an array.
  317 | 	 * @return the array of {@link MappedInterceptor}s, or {@code null} if none
  318 | 	 */
  319 | 	protected final MappedInterceptor @Nullable [] getMappedInterceptors() {
  320 | 		List<MappedInterceptor> mappedInterceptors = new ArrayList<>(this.adaptedInterceptors.size());
  321 | 		for (HandlerInterceptor interceptor : this.adaptedInterceptors) {
  322 | 			if (interceptor instanceof MappedInterceptor mappedInterceptor) {
  323 | 				mappedInterceptors.add(mappedInterceptor);
  324 | 			}
  325 | 		}
  326 | 		return (!mappedInterceptors.isEmpty() ? mappedInterceptors.toArray(new MappedInterceptor[0]) : null);
  327 | 	}
  328 | 
  329 | 	/**
  330 | 	 * Set "global" CORS configuration mappings. The first matching URL pattern
  331 | 	 * determines the {@code CorsConfiguration} to use which is then further
  332 | 	 * {@link CorsConfiguration#combine(CorsConfiguration) combined} with the
  333 | 	 * {@code CorsConfiguration} for the selected handler.
  334 | 	 * <p>This is mutually exclusive with
  335 | 	 * {@link #setCorsConfigurationSource(CorsConfigurationSource)}.
  336 | 	 * @since 4.2
  337 | 	 * @see #setCorsProcessor(CorsProcessor)
  338 | 	 */
  339 | 	@SuppressWarnings("removal")
  340 | 	public void setCorsConfigurations(Map<String, CorsConfiguration> corsConfigurations) {
  341 | 		if (CollectionUtils.isEmpty(corsConfigurations)) {
  342 | 			this.corsConfigurationSource = null;
  343 | 			return;
  344 | 		}
  345 | 		UrlBasedCorsConfigurationSource source;
  346 | 		if (getPatternParser() != null) {
  347 | 			source = new UrlBasedCorsConfigurationSource(getPatternParser());
  348 | 			source.setCorsConfigurations(corsConfigurations);
  349 | 		}
  350 | 		else {
  351 | 			source = new UrlBasedCorsConfigurationSource();
  352 | 			source.setCorsConfigurations(corsConfigurations);
  353 | 			source.setPathMatcher(this.pathMatcher);
  354 | 			source.setUrlPathHelper(this.urlPathHelper);
  355 | 		}
  356 | 		setCorsConfigurationSource(source);
  357 | 	}
  358 | 
  359 | 	/**
  360 | 	 * Set a {@code CorsConfigurationSource} for "global" CORS config. The
  361 | 	 * {@code CorsConfiguration} determined by the source is
  362 | 	 * {@link CorsConfiguration#combine(CorsConfiguration) combined} with the
  363 | 	 * {@code CorsConfiguration} for the selected handler.
  364 | 	 * <p>This is mutually exclusive with {@link #setCorsConfigurations(Map)}.
  365 | 	 * @since 5.1
  366 | 	 * @see #setCorsProcessor(CorsProcessor)
  367 | 	 */
  368 | 	@SuppressWarnings("removal")
  369 | 	public void setCorsConfigurationSource(CorsConfigurationSource source) {
  370 | 		Assert.notNull(source, "CorsConfigurationSource must not be null");
  371 | 		this.corsConfigurationSource = source;
  372 | 		if (source instanceof UrlBasedCorsConfigurationSource urlConfigSource) {
  373 | 			urlConfigSource.setAllowInitLookupPath(false);
  374 | 		}
  375 | 	}
  376 | 
  377 | 	/**
  378 | 	 * Return the {@link #setCorsConfigurationSource(CorsConfigurationSource)
  379 | 	 * configured} {@code CorsConfigurationSource}, if any.
  380 | 	 * @since 5.3
  381 | 	 */
  382 | 	public @Nullable CorsConfigurationSource getCorsConfigurationSource() {
  383 | 		return this.corsConfigurationSource;
  384 | 	}
  385 | 
  386 | 	/**
  387 | 	 * Configure a custom {@link CorsProcessor} to use to apply the matched
  388 | 	 * {@link CorsConfiguration} for a request.
  389 | 	 * <p>By default {@link DefaultCorsProcessor} is used.
  390 | 	 * @since 4.2
  391 | 	 */
  392 | 	public void setCorsProcessor(CorsProcessor corsProcessor) {
  393 | 		Assert.notNull(corsProcessor, "CorsProcessor must not be null");
  394 | 		this.corsProcessor = corsProcessor;
  395 | 	}
  396 | 
  397 | 	/**
  398 | 	 * Return the configured {@link CorsProcessor}.
  399 | 	 */
  400 | 	public CorsProcessor getCorsProcessor() {
  401 | 		return this.corsProcessor;
  402 | 	}
  403 | 
  404 | 	/**
  405 | 	 * Configure a strategy to manage API versioning.
  406 | 	 * @param strategy the strategy to use
  407 | 	 * @since 7.0
  408 | 	 */
  409 | 	public void setApiVersionStrategy(@Nullable ApiVersionStrategy strategy) {
  410 | 		this.versionStrategy = strategy;
  411 | 	}
  412 | 
  413 | 	/**
  414 | 	 * Return the configured {@link ApiVersionStrategy} strategy.
  415 | 	 * @since 7.0
  416 | 	 */
  417 | 	public @Nullable ApiVersionStrategy getApiVersionStrategy() {
  418 | 		return this.versionStrategy;
  419 | 	}
  420 | 
  421 | 	/**
  422 | 	 * Specify the order value for this HandlerMapping bean.
  423 | 	 * <p>The default value is {@code Ordered.LOWEST_PRECEDENCE}, meaning non-ordered.
  424 | 	 * @see org.springframework.core.Ordered#getOrder()
  425 | 	 */
  426 | 	public void setOrder(int order) {
  427 | 		this.order = order;
  428 | 	}
  429 | 
  430 | 	@Override
  431 | 	public int getOrder() {
  432 | 		return this.order;
  433 | 	}
  434 | 
  435 | 	@Override
  436 | 	public void setBeanName(String name) {
  437 | 		this.beanName = name;
  438 | 	}
  439 | 
  440 | 	protected String formatMappingName() {
  441 | 		return (this.beanName != null ? "'" + this.beanName + "'" : getClass().getName());
  442 | 	}
  443 | 
  444 | 
  445 | 	/**
  446 | 	 * Initializes the interceptors.
  447 | 	 * @see #extendInterceptors(java.util.List)
  448 | 	 * @see #initInterceptors()
  449 | 	 */
  450 | 	@Override
  451 | 	protected void initApplicationContext() throws BeansException {
  452 | 		extendInterceptors(this.interceptors);
  453 | 		detectMappedInterceptors(this.adaptedInterceptors);
  454 | 		initInterceptors();
  455 | 	}
  456 | 
  457 | 	/**
  458 | 	 * Extension hook that subclasses can override to register additional interceptors,
  459 | 	 * given the configured interceptors (see {@link #setInterceptors}).
  460 | 	 * <p>Will be invoked before {@link #initInterceptors()} adapts the specified
  461 | 	 * interceptors into {@link HandlerInterceptor} instances.
  462 | 	 * <p>The default implementation is empty.
  463 | 	 * @param interceptors the configured interceptor List (never {@code null}), allowing
  464 | 	 * to add further interceptors before as well as after the existing interceptors
  465 | 	 */
  466 | 	protected void extendInterceptors(List<Object> interceptors) {
  467 | 	}
  468 | 
  469 | 	/**
  470 | 	 * Detect beans of type {@link MappedInterceptor} and add them to the list
  471 | 	 * of mapped interceptors.
  472 | 	 * <p>This is called in addition to any {@link MappedInterceptor}s that may
  473 | 	 * have been provided via {@link #setInterceptors}, by default adding all
  474 | 	 * beans of type {@link MappedInterceptor} from the current context and its
  475 | 	 * ancestors. Subclasses can override and refine this policy.
  476 | 	 * @param mappedInterceptors an empty list to add to
  477 | 	 */
  478 | 	protected void detectMappedInterceptors(List<HandlerInterceptor> mappedInterceptors) {
  479 | 		mappedInterceptors.addAll(BeanFactoryUtils.beansOfTypeIncludingAncestors(
  480 | 				obtainApplicationContext(), MappedInterceptor.class, true, false).values());
  481 | 	}
  482 | 
  483 | 	/**
  484 | 	 * Initialize the specified interceptors adapting
  485 | 	 * {@link WebRequestInterceptor}s to {@link HandlerInterceptor}.
  486 | 	 * @see #setInterceptors
  487 | 	 * @see #adaptInterceptor
  488 | 	 */
  489 | 	protected void initInterceptors() {
  490 | 		if (!this.interceptors.isEmpty()) {
  491 | 			for (int i = 0; i < this.interceptors.size(); i++) {
  492 | 				Object interceptor = this.interceptors.get(i);
  493 | 				if (interceptor == null) {
  494 | 					throw new IllegalArgumentException("Entry number " + i + " in interceptors array is null");
  495 | 				}
  496 | 				this.adaptedInterceptors.add(adaptInterceptor(interceptor));
  497 | 			}
  498 | 		}
  499 | 	}
  500 | 
  501 | 	/**
  502 | 	 * Adapt the given interceptor object to {@link HandlerInterceptor}.
  503 | 	 * <p>By default, the supported interceptor types are
  504 | 	 * {@link HandlerInterceptor} and {@link WebRequestInterceptor}. Each given
  505 | 	 * {@link WebRequestInterceptor} is wrapped with
  506 | 	 * {@link WebRequestHandlerInterceptorAdapter}.
  507 | 	 * @param interceptor the interceptor
  508 | 	 * @return the interceptor downcast or adapted to HandlerInterceptor
  509 | 	 * @see org.springframework.web.servlet.HandlerInterceptor
  510 | 	 * @see org.springframework.web.context.request.WebRequestInterceptor
  511 | 	 * @see WebRequestHandlerInterceptorAdapter
  512 | 	 */
  513 | 	protected HandlerInterceptor adaptInterceptor(Object interceptor) {
  514 | 		if (interceptor instanceof HandlerInterceptor handlerInterceptor) {
  515 | 			return handlerInterceptor;
  516 | 		}
  517 | 		else if (interceptor instanceof WebRequestInterceptor webRequestInterceptor) {
  518 | 			return new WebRequestHandlerInterceptorAdapter(webRequestInterceptor);
  519 | 		}
  520 | 		else {
  521 | 			throw new IllegalArgumentException("Interceptor type not supported: " + interceptor.getClass().getName());
  522 | 		}
  523 | 	}
  524 | 
  525 | 	/**
  526 | 	 * Return "true" if this {@code HandlerMapping} has been
  527 | 	 * {@link #setPatternParser enabled} to use parsed {@code PathPattern}s.
  528 | 	 */
  529 | 	@Override
  530 | 	public boolean usesPathPatterns() {
  531 | 		return getPatternParser() != null;
  532 | 	}
  533 | 
  534 | 	/**
  535 | 	 * Look up a handler for the given request, falling back to the default
  536 | 	 * handler if no specific one is found.
  537 | 	 * @param request current HTTP request
  538 | 	 * @return the corresponding handler instance, or the default handler
  539 | 	 * @see #getHandlerInternal
  540 | 	 */
  541 | 	@Override
  542 | 	public final @Nullable HandlerExecutionChain getHandler(HttpServletRequest request) throws Exception {
  543 | 		ApiVersionHolder versionHolder = initApiVersion(request);
  544 | 		Object handler = getHandlerInternal(request);
  545 | 		if (handler == null) {
  546 | 			handler = getDefaultHandler();
  547 | 		}
  548 | 		if (handler == null) {
  549 | 			return null;
  550 | 		}
  551 | 
  552 | 		if (versionHolder.hasError() && !request.getDispatcherType().equals(DispatcherType.ERROR)) {
  553 | 			throw versionHolder.getError();
  554 | 		}
  555 | 
  556 | 		// Bean name or resolved handler?
  557 | 		if (handler instanceof String handlerName) {
  558 | 			handler = obtainApplicationContext().getBean(handlerName);
  559 | 		}
  560 | 
  561 | 		// Ensure presence of cached lookupPath for interceptors and others
  562 | 		if (!ServletRequestPathUtils.hasCachedPath(request)) {
  563 | 			initLookupPath(request);
  564 | 		}
  565 | 
  566 | 		HandlerExecutionChain executionChain = getHandlerExecutionChain(handler, request);
  567 | 
  568 | 		if (request.getAttribute(SUPPRESS_LOGGING_ATTRIBUTE) == null) {
  569 | 			if (logger.isTraceEnabled()) {
  570 | 				logger.trace("Mapped to " + handler);
  571 | 			}
  572 | 			else if (logger.isDebugEnabled() && !DispatcherType.ASYNC.equals(request.getDispatcherType())) {
  573 | 				logger.debug("Mapped to " + executionChain.getHandler());
  574 | 			}
  575 | 		}
  576 | 
  577 | 		if (hasCorsConfigurationSource(handler) || CorsUtils.isPreFlightRequest(request)) {
  578 | 			CorsConfiguration config = getCorsConfiguration(handler, request);
  579 | 			if (getCorsConfigurationSource() != null) {
  580 | 				CorsConfiguration globalConfig = getCorsConfigurationSource().getCorsConfiguration(request);
  581 | 				config = (globalConfig != null ? globalConfig.combine(config) : config);
  582 | 			}
  583 | 			if (config != null) {
  584 | 				config.validateAllowCredentials();
  585 | 				config.validateAllowPrivateNetwork();
  586 | 			}
  587 | 			executionChain = getCorsHandlerExecutionChain(request, executionChain, config);
  588 | 		}
  589 | 
  590 | 		return executionChain;
  591 | 	}
  592 | 
  593 | 	private ApiVersionHolder initApiVersion(HttpServletRequest request) {
  594 | 		ApiVersionHolder versionHolder;
  595 | 		if (this.versionStrategy == null) {
  596 | 			versionHolder = ApiVersionHolder.EMPTY;
  597 | 		}
  598 | 		else {
  599 | 			try {
  600 | 				Comparable<?> version = this.versionStrategy.resolveParseAndValidateVersion(request);
  601 | 				versionHolder = ApiVersionHolder.fromVersion(version);
  602 | 			}
  603 | 			catch (RuntimeException ex) {
  604 | 				versionHolder = ApiVersionHolder.fromError(ex);
  605 | 			}
  606 | 		}
  607 | 		request.setAttribute(API_VERSION_ATTRIBUTE, versionHolder);
  608 | 		return versionHolder;
  609 | 	}
  610 | 
  611 | 	/**
  612 | 	 * Look up a handler for the given request, returning {@code null} if no
  613 | 	 * specific one is found. This method is called by {@link #getHandler};
  614 | 	 * a {@code null} return value will lead to the default handler, if one is set.
  615 | 	 * <p>On CORS pre-flight requests this method should return a match not for
  616 | 	 * the pre-flight request but for the expected actual request based on the URL
  617 | 	 * path, the HTTP methods from the "Access-Control-Request-Method" header, and
  618 | 	 * the headers from the "Access-Control-Request-Headers" header thus allowing
  619 | 	 * the CORS configuration to be obtained via {@link #getCorsConfiguration(Object, HttpServletRequest)},
  620 | 	 * <p>Note: This method may also return a pre-built {@link HandlerExecutionChain},
  621 | 	 * combining a handler object with dynamically determined interceptors.
  622 | 	 * Statically specified interceptors will get merged into such an existing chain.
  623 | 	 * @param request current HTTP request
  624 | 	 * @return the corresponding handler instance, or {@code null} if none found
  625 | 	 * @throws Exception if there is an internal error
  626 | 	 */
  627 | 	protected abstract @Nullable Object getHandlerInternal(HttpServletRequest request) throws Exception;
  628 | 
  629 | 	/**
  630 | 	 * Initialize the path to use for request mapping.
  631 | 	 * <p>When parsed patterns are {@link #usesPathPatterns() enabled} a parsed
  632 | 	 * {@code RequestPath} is expected to have been
  633 | 	 * {@link ServletRequestPathUtils#parseAndCache(HttpServletRequest) parsed}
  634 | 	 * externally by the {@link org.springframework.web.servlet.DispatcherServlet}
  635 | 	 * or {@link org.springframework.web.filter.ServletRequestPathFilter}.
  636 | 	 * <p>Otherwise for String pattern matching via {@code PathMatcher} the
  637 | 	 * path is {@link UrlPathHelper#resolveAndCacheLookupPath resolved} by this
  638 | 	 * method.
  639 | 	 * @since 5.3
  640 | 	 */
  641 | 	protected String initLookupPath(HttpServletRequest request) {
  642 | 		if (usesPathPatterns()) {
  643 | 			request.removeAttribute(UrlPathHelper.PATH_ATTRIBUTE);
  644 | 			RequestPath requestPath = getRequestPath(request);
  645 | 			String lookupPath = requestPath.pathWithinApplication().value();
  646 | 			return UrlPathHelper.defaultInstance.removeSemicolonContent(lookupPath);
  647 | 		}
  648 | 		else {
  649 | 			return getUrlPathHelper().resolveAndCacheLookupPath(request);
  650 | 		}
  651 | 	}
  652 | 
  653 | 	private RequestPath getRequestPath(HttpServletRequest request) {
  654 | 		// Expect pre-parsed path with DispatcherServlet,
  655 | 		// but otherwise parse per handler lookup + cache for handling
  656 | 		return (request.getAttribute(DispatcherServlet.WEB_APPLICATION_CONTEXT_ATTRIBUTE) != null ?
  657 | 				ServletRequestPathUtils.getParsedRequestPath(request) :
  658 | 				ServletRequestPathUtils.parseAndCache(request));
  659 | 	}
  660 | 
  661 | 	/**
  662 | 	 * Build a {@link HandlerExecutionChain} for the given handler, including
  663 | 	 * applicable interceptors.
  664 | 	 * <p>The default implementation builds a standard {@link HandlerExecutionChain}
  665 | 	 * with the given handler, the common interceptors of the handler mapping, and any
  666 | 	 * {@link MappedInterceptor MappedInterceptors} matching to the current request URL. Interceptors
  667 | 	 * are added in the order they were registered. Subclasses may override this
  668 | 	 * in order to extend/rearrange the list of interceptors.
  669 | 	 * <p><b>NOTE:</b> The passed-in handler object may be a raw handler or a
  670 | 	 * pre-built {@link HandlerExecutionChain}. This method should handle those
  671 | 	 * two cases explicitly, either building a new {@link HandlerExecutionChain}
  672 | 	 * or extending the existing chain.
  673 | 	 * <p>For simply adding an interceptor in a custom subclass, consider calling
  674 | 	 * {@code super.getHandlerExecutionChain(handler, request)} and invoking
  675 | 	 * {@link HandlerExecutionChain#addInterceptor} on the returned chain object.
  676 | 	 * @param handler the resolved handler instance (never {@code null})
  677 | 	 * @param request current HTTP request
  678 | 	 * @return the HandlerExecutionChain (never {@code null})
  679 | 	 * @see #getAdaptedInterceptors()
  680 | 	 */
  681 | 	protected HandlerExecutionChain getHandlerExecutionChain(Object handler, HttpServletRequest request) {
  682 | 
  683 | 		HandlerExecutionChain chain = (handler instanceof HandlerExecutionChain handlerExecutionChain ?
  684 | 				handlerExecutionChain : new HandlerExecutionChain(handler));
  685 | 
  686 | 		for (HandlerInterceptor interceptor : this.adaptedInterceptors) {
  687 | 			if (interceptor instanceof MappedInterceptor mappedInterceptor) {
  688 | 				if (mappedInterceptor.matches(request)) {
  689 | 					chain.addInterceptor(mappedInterceptor.getInterceptor());
  690 | 				}
  691 | 			}
  692 | 			else {
  693 | 				chain.addInterceptor(interceptor);
  694 | 			}
  695 | 		}
  696 | 
  697 | 		if (this.versionStrategy != null) {
  698 | 			ApiVersionHolder versionHolder = (ApiVersionHolder) request.getAttribute(API_VERSION_ATTRIBUTE);
  699 | 			if (versionHolder.hasVersion()) {
  700 | 				Comparable<?> version = versionHolder.getVersion();
  701 | 				chain.addInterceptor(new ApiVersionDeprecationHandlerInterceptor(this.versionStrategy, version));
  702 | 			}
  703 | 		}
  704 | 
  705 | 		return chain;
  706 | 	}
  707 | 
  708 | 	/**
  709 | 	 * Return {@code true} if there is a {@link CorsConfigurationSource} for this handler.
  710 | 	 * @since 5.2
  711 | 	 */
  712 | 	protected boolean hasCorsConfigurationSource(Object handler) {
  713 | 		if (handler instanceof HandlerExecutionChain handlerExecutionChain) {
  714 | 			handler = handlerExecutionChain.getHandler();
  715 | 		}
  716 | 		return (handler instanceof CorsConfigurationSource || this.corsConfigurationSource != null);
  717 | 	}
  718 | 
  719 | 	/**
  720 | 	 * Retrieve the CORS configuration for the given handler.
  721 | 	 * @param handler the handler to check (never {@code null}).
  722 | 	 * @param request the current request.
  723 | 	 * @return the CORS configuration for the handler, or {@code null} if none
  724 | 	 * @since 4.2
  725 | 	 */
  726 | 	protected @Nullable CorsConfiguration getCorsConfiguration(Object handler, HttpServletRequest request) {
  727 | 		Object resolvedHandler = handler;
  728 | 		if (handler instanceof HandlerExecutionChain handlerExecutionChain) {
  729 | 			resolvedHandler = handlerExecutionChain.getHandler();
  730 | 		}
  731 | 		if (resolvedHandler instanceof CorsConfigurationSource configSource) {
  732 | 			return configSource.getCorsConfiguration(request);
  733 | 		}
  734 | 		return null;
  735 | 	}
  736 | 
  737 | 	/**
  738 | 	 * Update {@link HandlerExecutionChain} for CORS requests, inserting an
  739 | 	 * interceptor at the start of the chain to perform CORS checks, and
  740 | 	 * also using a no-op handler for preflight requests.
  741 | 	 * @param request the current request
  742 | 	 * @param chain the chain to update
  743 | 	 * @param config the CORS configuration applicable to the handler
  744 | 	 * @since 4.2
  745 | 	 */
  746 | 	protected HandlerExecutionChain getCorsHandlerExecutionChain(
  747 | 			HttpServletRequest request, HandlerExecutionChain chain, @Nullable CorsConfiguration config) {
  748 | 
  749 | 		if (CorsUtils.isPreFlightRequest(request)) {
  750 | 			PreFlightHttpRequestHandler handler = new PreFlightHttpRequestHandler(config);
  751 | 			chain.addInterceptor(0, handler);
  752 | 			return new HandlerExecutionChain(handler, chain.getInterceptors());
  753 | 		}
  754 | 		else {
  755 | 			chain.addInterceptor(0, new CorsInterceptor(config));
  756 | 			return chain;
  757 | 		}
  758 | 	}
  759 | 
  760 | 
  761 | 	private class CorsInterceptor implements HandlerInterceptor, CorsConfigurationSource {
  762 | 
  763 | 		private final @Nullable CorsConfiguration config;
  764 | 
  765 | 		public CorsInterceptor(@Nullable CorsConfiguration config) {
  766 | 			this.config = config;
  767 | 		}
  768 | 
  769 | 		@Override
  770 | 		public @Nullable CorsConfiguration getCorsConfiguration(HttpServletRequest request) {
  771 | 			return this.config;
  772 | 		}
  773 | 
  774 | 		@Override
  775 | 		public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler)
  776 | 				throws Exception {
  777 | 
  778 | 			// Consistent with CorsFilter, ignore ASYNC dispatches
  779 | 			WebAsyncManager asyncManager = WebAsyncUtils.getAsyncManager(request);
  780 | 			if (asyncManager.hasConcurrentResult()) {
  781 | 				return true;
  782 | 			}
  783 | 
  784 | 			return invokeCorsProcessor(request, response);
  785 | 		}
  786 | 
  787 | 		protected boolean invokeCorsProcessor(
  788 | 				HttpServletRequest request, HttpServletResponse response) throws IOException {
  789 | 
  790 | 			return corsProcessor.processRequest(this.config, request, response);
  791 | 		}
  792 | 	}
  793 | 
  794 | 
  795 | 	private final class PreFlightHttpRequestHandler
  796 | 			extends CorsInterceptor implements HttpRequestHandler, PreFlightRequestHandler {
  797 | 
  798 | 		public PreFlightHttpRequestHandler(@Nullable CorsConfiguration config) {
  799 | 			super(config);
  800 | 		}
  801 | 
  802 | 		@Override
  803 | 		public void handleRequest(HttpServletRequest request, HttpServletResponse response) {
  804 | 			// no-op
  805 | 		}
  806 | 
  807 | 		@Override
  808 | 		public void handlePreFlight(HttpServletRequest request, HttpServletResponse response) throws IOException {
  809 | 			invokeCorsProcessor(request, response);
  810 | 		}
  811 | 	}
  812 | 
  813 | 
  814 | 	private record ApiVersionDeprecationHandlerInterceptor(
  815 | 			ApiVersionStrategy versionStrategy, Comparable<?> version) implements HandlerInterceptor {
  816 | 
  817 | 		@Override
  818 | 		public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
  819 | 			this.versionStrategy.handleDeprecations(this.version, handler, request, response);
  820 | 			return true;
  821 | 		}
  822 | 	}
  823 | 
  824 | }
```
