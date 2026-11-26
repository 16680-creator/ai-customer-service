<!-- learning-docs source archive（机械抓取原文，禁止手改代码内容） -->
<!-- origin: org.springframework:spring-webmvc:7.1.0 | path: org/springframework/web/servlet/mvc/method/RequestMappingInfo.java -->
<!-- 正文引用：[[源码:RequestMappingInfo.java#L起-L止]] ；代码块头部：(src: RequestMappingInfo.java#L起-L止) -->

# RequestMappingInfo.java

| 项 | 值 |
| --- | --- |
| 来源 | `org.springframework:spring-webmvc:7.1.0` |
| 路径 | `org/springframework/web/servlet/mvc/method/RequestMappingInfo.java` |
| 行数 | 1084 |
| 在线地址 | https://repo1.maven.org/maven2/org/springframework/spring-webmvc/7.1.0/spring-webmvc-7.1.0-sources.jar |
| 抓取时间 | 2026-11-25T14:01:10.965Z |

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
   17 | package org.springframework.web.servlet.mvc.method;
   18 | 
   19 | import java.util.Set;
   20 | 
   21 | import jakarta.servlet.ServletRequest;
   22 | import jakarta.servlet.http.HttpServletRequest;
   23 | import org.jspecify.annotations.Nullable;
   24 | 
   25 | import org.springframework.http.HttpMethod;
   26 | import org.springframework.util.Assert;
   27 | import org.springframework.util.ObjectUtils;
   28 | import org.springframework.util.PathMatcher;
   29 | import org.springframework.util.StringUtils;
   30 | import org.springframework.web.accept.ApiVersionStrategy;
   31 | import org.springframework.web.accept.ContentNegotiationManager;
   32 | import org.springframework.web.bind.annotation.RequestMethod;
   33 | import org.springframework.web.servlet.handler.AbstractHandlerMapping;
   34 | import org.springframework.web.servlet.mvc.condition.ConsumesRequestCondition;
   35 | import org.springframework.web.servlet.mvc.condition.HeadersRequestCondition;
   36 | import org.springframework.web.servlet.mvc.condition.ParamsRequestCondition;
   37 | import org.springframework.web.servlet.mvc.condition.PathPatternsRequestCondition;
   38 | import org.springframework.web.servlet.mvc.condition.PatternsRequestCondition;
   39 | import org.springframework.web.servlet.mvc.condition.ProducesRequestCondition;
   40 | import org.springframework.web.servlet.mvc.condition.RequestCondition;
   41 | import org.springframework.web.servlet.mvc.condition.RequestConditionHolder;
   42 | import org.springframework.web.servlet.mvc.condition.RequestMethodsRequestCondition;
   43 | import org.springframework.web.servlet.mvc.condition.VersionRequestCondition;
   44 | import org.springframework.web.util.ServletRequestPathUtils;
   45 | import org.springframework.web.util.UrlPathHelper;
   46 | import org.springframework.web.util.pattern.PathPattern;
   47 | import org.springframework.web.util.pattern.PathPatternParser;
   48 | 
   49 | /**
   50 |  * Request mapping information. A composite for the following conditions:
   51 |  * <ol>
   52 |  * <li>{@link PathPatternsRequestCondition} with parsed {@code PathPatterns} or
   53 |  * {@link PatternsRequestCondition} with String patterns via {@code PathMatcher}
   54 |  * <li>{@link RequestMethodsRequestCondition}
   55 |  * <li>{@link ParamsRequestCondition}
   56 |  * <li>{@link HeadersRequestCondition}
   57 |  * <li>{@link ConsumesRequestCondition}
   58 |  * <li>{@link ProducesRequestCondition}
   59 |  * <li>{@code RequestCondition} (optional, custom request condition)
   60 |  * </ol>
   61 |  *
   62 |  * @author Arjen Poutsma
   63 |  * @author Rossen Stoyanchev
   64 |  * @since 3.1
   65 |  */
   66 | public final class RequestMappingInfo implements RequestCondition<RequestMappingInfo> {
   67 | 
   68 | 	private static final PathPatternsRequestCondition EMPTY_PATH_PATTERNS = new PathPatternsRequestCondition();
   69 | 
   70 | 	@SuppressWarnings("removal")
   71 | 	private static final PatternsRequestCondition EMPTY_PATTERNS = new PatternsRequestCondition();
   72 | 
   73 | 	private static final RequestMethodsRequestCondition EMPTY_REQUEST_METHODS = new RequestMethodsRequestCondition();
   74 | 
   75 | 	private static final ParamsRequestCondition EMPTY_PARAMS = new ParamsRequestCondition();
   76 | 
   77 | 	private static final HeadersRequestCondition EMPTY_HEADERS = new HeadersRequestCondition();
   78 | 
   79 | 	private static final ConsumesRequestCondition EMPTY_CONSUMES = new ConsumesRequestCondition();
   80 | 
   81 | 	private static final ProducesRequestCondition EMPTY_PRODUCES = new ProducesRequestCondition();
   82 | 
   83 | 	private static final RequestConditionHolder EMPTY_CUSTOM = new RequestConditionHolder(null);
   84 | 
   85 | 
   86 | 	private final @Nullable String name;
   87 | 
   88 | 	private final @Nullable PathPatternsRequestCondition pathPatternsCondition;
   89 | 
   90 | 	@SuppressWarnings("removal")
   91 | 	private final @Nullable PatternsRequestCondition patternsCondition;
   92 | 
   93 | 	private final RequestMethodsRequestCondition methodsCondition;
   94 | 
   95 | 	private final ParamsRequestCondition paramsCondition;
   96 | 
   97 | 	private final HeadersRequestCondition headersCondition;
   98 | 
   99 | 	private final ConsumesRequestCondition consumesCondition;
  100 | 
  101 | 	private final ProducesRequestCondition producesCondition;
  102 | 
  103 | 	private final VersionRequestCondition versionCondition;
  104 | 
  105 | 	private final RequestConditionHolder customConditionHolder;
  106 | 
  107 | 	private @Nullable Integer hashCode;
  108 | 
  109 | 	private final BuilderConfiguration options;
  110 | 
  111 | 
  112 | 	/**
  113 | 	 * Full constructor with a mapping name.
  114 | 	 * @deprecated in favor using {@link RequestMappingInfo.Builder} via {@link #paths(String...)}.
  115 | 	 */
  116 | 	@SuppressWarnings("removal")
  117 | 	@Deprecated(since = "5.3")
  118 | 	public RequestMappingInfo(@Nullable String name, @Nullable PatternsRequestCondition patterns,
  119 | 			@Nullable RequestMethodsRequestCondition methods, @Nullable ParamsRequestCondition params,
  120 | 			@Nullable HeadersRequestCondition headers, @Nullable ConsumesRequestCondition consumes,
  121 | 			@Nullable ProducesRequestCondition produces, @Nullable VersionRequestCondition version,
  122 | 			@Nullable RequestCondition<?> custom) {
  123 | 
  124 | 		this(name, null,
  125 | 				(patterns != null ? patterns : EMPTY_PATTERNS),
  126 | 				(methods != null ? methods : EMPTY_REQUEST_METHODS),
  127 | 				(params != null ? params : EMPTY_PARAMS),
  128 | 				(headers != null ? headers : EMPTY_HEADERS),
  129 | 				(consumes != null ? consumes : EMPTY_CONSUMES),
  130 | 				(produces != null ? produces : EMPTY_PRODUCES),
  131 | 				(version != null ? version : new VersionRequestCondition(null, null)),
  132 | 				(custom != null ? new RequestConditionHolder(custom) : EMPTY_CUSTOM),
  133 | 				new BuilderConfiguration());
  134 | 	}
  135 | 
  136 | 	/**
  137 | 	 * Create an instance with the given conditions.
  138 | 	 * @deprecated in favor using {@link RequestMappingInfo.Builder} via
  139 | 	 * {@link #paths(String...)}.
  140 | 	 */
  141 | 	@SuppressWarnings("removal")
  142 | 	@Deprecated(since = "5.3")
  143 | 	public RequestMappingInfo(@Nullable PatternsRequestCondition patterns,
  144 | 			@Nullable RequestMethodsRequestCondition methods, @Nullable ParamsRequestCondition params,
  145 | 			@Nullable HeadersRequestCondition headers, @Nullable ConsumesRequestCondition consumes,
  146 | 			@Nullable ProducesRequestCondition produces, @Nullable VersionRequestCondition version,
  147 | 			@Nullable RequestCondition<?> custom) {
  148 | 
  149 | 		this(null, patterns, methods, params, headers, consumes, produces, version, custom);
  150 | 	}
  151 | 
  152 | 	/**
  153 | 	 * Re-create a RequestMappingInfo with the given custom request condition.
  154 | 	 * @deprecated since 5.3 in favor of using {@link #addCustomCondition(RequestCondition)}.
  155 | 	 */
  156 | 	@Deprecated(since = "5.3")
  157 | 	public RequestMappingInfo(RequestMappingInfo info, @Nullable RequestCondition<?> customRequestCondition) {
  158 | 		this(info.name, info.patternsCondition, info.methodsCondition, info.paramsCondition,
  159 | 				info.headersCondition, info.consumesCondition, info.producesCondition,
  160 | 				info.versionCondition, customRequestCondition);
  161 | 	}
  162 | 
  163 | 	@SuppressWarnings("removal")
  164 | 	private RequestMappingInfo(@Nullable String name,
  165 | 			@Nullable PathPatternsRequestCondition pathPatternsCondition,
  166 | 			@Nullable PatternsRequestCondition patternsCondition,
  167 | 			RequestMethodsRequestCondition methodsCondition, ParamsRequestCondition paramsCondition,
  168 | 			HeadersRequestCondition headersCondition, ConsumesRequestCondition consumesCondition,
  169 | 			ProducesRequestCondition producesCondition, VersionRequestCondition versionCondition,
  170 | 			RequestConditionHolder customCondition, BuilderConfiguration options) {
  171 | 
  172 | 		this(name, pathPatternsCondition, patternsCondition,
  173 | 				methodsCondition, paramsCondition, headersCondition,
  174 | 				consumesCondition, producesCondition, versionCondition,
  175 | 				customCondition, options, true);
  176 | 	}
  177 | 
  178 | 	@SuppressWarnings("removal")
  179 | 	private RequestMappingInfo(@Nullable String name,
  180 | 			@Nullable PathPatternsRequestCondition pathPatternsCondition,
  181 | 			@Nullable PatternsRequestCondition patternsCondition,
  182 | 			RequestMethodsRequestCondition methodsCondition, ParamsRequestCondition paramsCondition,
  183 | 			HeadersRequestCondition headersCondition, ConsumesRequestCondition consumesCondition,
  184 | 			ProducesRequestCondition producesCondition, VersionRequestCondition versionCondition,
  185 | 			RequestConditionHolder customCondition, BuilderConfiguration options,
  186 | 			boolean calculateHashCode) {
  187 | 
  188 | 		Assert.isTrue(pathPatternsCondition != null || patternsCondition != null,
  189 | 				"Neither PathPatterns nor String patterns condition");
  190 | 
  191 | 		this.name = (StringUtils.hasText(name) ? name : null);
  192 | 		this.pathPatternsCondition = pathPatternsCondition;
  193 | 		this.patternsCondition = patternsCondition;
  194 | 		this.methodsCondition = methodsCondition;
  195 | 		this.paramsCondition = paramsCondition;
  196 | 		this.headersCondition = headersCondition;
  197 | 		this.consumesCondition = consumesCondition;
  198 | 		this.producesCondition = producesCondition;
  199 | 		this.versionCondition = versionCondition;
  200 | 		this.customConditionHolder = customCondition;
  201 | 		this.options = options;
  202 | 
  203 | 		if (calculateHashCode) {
  204 | 			this.hashCode = calculateHashCode();
  205 | 		}
  206 | 	}
  207 | 
  208 | 
  209 | 	/**
  210 | 	 * Return the name for this mapping, or {@code null}.
  211 | 	 */
  212 | 	public @Nullable String getName() {
  213 | 		return this.name;
  214 | 	}
  215 | 
  216 | 	/**
  217 | 	 * Return the patterns condition in use when parsed patterns are
  218 | 	 * {@link AbstractHandlerMapping#usesPathPatterns() enabled}.
  219 | 	 * <p>This is mutually exclusive with {@link #getPatternsCondition()} such
  220 | 	 * that when one returns {@code null} the other one returns an instance.
  221 | 	 * @since 5.3
  222 | 	 * @see #getActivePatternsCondition()
  223 | 	 */
  224 | 	public @Nullable PathPatternsRequestCondition getPathPatternsCondition() {
  225 | 		return this.pathPatternsCondition;
  226 | 	}
  227 | 
  228 | 	/**
  229 | 	 * Return the patterns condition when String pattern matching via
  230 | 	 * {@link PathMatcher} is in use.
  231 | 	 * <p>This is mutually exclusive with {@link #getPathPatternsCondition()}
  232 | 	 * such that when one returns {@code null} the other one returns an instance.
  233 | 	 * @deprecated use of {@link PathMatcher} and {@link UrlPathHelper} is deprecated
  234 | 	 * for use at runtime in web modules in favor of parsed patterns with
  235 | 	 * {@link PathPatternParser}.
  236 | 	 */
  237 | 	@SuppressWarnings("removal")
  238 | 	@Deprecated(since = "7.0", forRemoval = true)
  239 | 	public @Nullable PatternsRequestCondition getPatternsCondition() {
  240 | 		return this.patternsCondition;
  241 | 	}
  242 | 
  243 | 	/**
  244 | 	 * Returns either {@link #getPathPatternsCondition()} or
  245 | 	 * {@link #getPatternsCondition()} depending on which is not null.
  246 | 	 * @since 5.3
  247 | 	 */
  248 | 	@SuppressWarnings("unchecked")
  249 | 	public <T> RequestCondition<T> getActivePatternsCondition() {
  250 | 		if (this.pathPatternsCondition != null) {
  251 | 			return (RequestCondition<T>) this.pathPatternsCondition;
  252 | 		}
  253 | 		else if (this.patternsCondition != null) {
  254 | 			return (RequestCondition<T>) this.patternsCondition;
  255 | 		}
  256 | 		else {
  257 | 			// Already checked in the constructor...
  258 | 			throw new IllegalStateException();
  259 | 		}
  260 | 	}
  261 | 
  262 | 	/**
  263 | 	 * Return the mapping paths that are not patterns.
  264 | 	 * @since 5.3
  265 | 	 */
  266 | 	@SuppressWarnings("removal")
  267 | 	public Set<String> getDirectPaths() {
  268 | 		RequestCondition<?> condition = getActivePatternsCondition();
  269 | 		return (condition instanceof PathPatternsRequestCondition pprc ?
  270 | 				pprc.getDirectPaths() : ((PatternsRequestCondition) condition).getDirectPaths());
  271 | 	}
  272 | 
  273 | 	/**
  274 | 	 * Return the patterns for the {@link #getActivePatternsCondition() active}
  275 | 	 * patterns condition as Strings.
  276 | 	 * @since 5.3
  277 | 	 */
  278 | 	@SuppressWarnings("removal")
  279 | 	public Set<String> getPatternValues() {
  280 | 		RequestCondition<?> condition = getActivePatternsCondition();
  281 | 		return (condition instanceof PathPatternsRequestCondition pprc ?
  282 | 				pprc.getPatternValues() : ((PatternsRequestCondition) condition).getPatterns());
  283 | 	}
  284 | 
  285 | 	/**
  286 | 	 * Whether the request mapping has an empty URL path mapping.
  287 | 	 * @since 6.0.10
  288 | 	 */
  289 | 	@SuppressWarnings("removal")
  290 | 	public boolean isEmptyMapping() {
  291 | 		RequestCondition<?> condition = getActivePatternsCondition();
  292 | 		return (condition instanceof PathPatternsRequestCondition pprc ?
  293 | 				pprc.isEmptyPathMapping() : ((PatternsRequestCondition) condition).isEmptyPathMapping());
  294 | 	}
  295 | 
  296 | 	/**
  297 | 	 * Return the HTTP request methods of this {@link RequestMappingInfo};
  298 | 	 * or instance with 0 request methods (never {@code null}).
  299 | 	 */
  300 | 	public RequestMethodsRequestCondition getMethodsCondition() {
  301 | 		return this.methodsCondition;
  302 | 	}
  303 | 
  304 | 	/**
  305 | 	 * Return the "parameters" condition of this {@link RequestMappingInfo};
  306 | 	 * or instance with 0 parameter expressions (never {@code null}).
  307 | 	 */
  308 | 	public ParamsRequestCondition getParamsCondition() {
  309 | 		return this.paramsCondition;
  310 | 	}
  311 | 
  312 | 	/**
  313 | 	 * Return the "headers" condition of this {@link RequestMappingInfo};
  314 | 	 * or instance with 0 header expressions (never {@code null}).
  315 | 	 */
  316 | 	public HeadersRequestCondition getHeadersCondition() {
  317 | 		return this.headersCondition;
  318 | 	}
  319 | 
  320 | 	/**
  321 | 	 * Return the "consumes" condition of this {@link RequestMappingInfo};
  322 | 	 * or instance with 0 consumes expressions (never {@code null}).
  323 | 	 */
  324 | 	public ConsumesRequestCondition getConsumesCondition() {
  325 | 		return this.consumesCondition;
  326 | 	}
  327 | 
  328 | 	/**
  329 | 	 * Return the "produces" condition of this {@link RequestMappingInfo};
  330 | 	 * or instance with 0 produces expressions (never {@code null}).
  331 | 	 */
  332 | 	public ProducesRequestCondition getProducesCondition() {
  333 | 		return this.producesCondition;
  334 | 	}
  335 | 
  336 | 	/**
  337 | 	 * Return the version condition of this {@link RequestMappingInfo},
  338 | 	 * or an instance without a version.
  339 | 	 * @since 7.0
  340 | 	 */
  341 | 	public VersionRequestCondition getVersionCondition() {
  342 | 		return this.versionCondition;
  343 | 	}
  344 | 
  345 | 	/**
  346 | 	 * Return the "custom" condition of this {@link RequestMappingInfo}, or {@code null}.
  347 | 	 */
  348 | 	public @Nullable RequestCondition<?> getCustomCondition() {
  349 | 		return this.customConditionHolder.getCondition();
  350 | 	}
  351 | 
  352 | 	/**
  353 | 	 * Create a new instance based on the current one, also adding the given
  354 | 	 * custom condition.
  355 | 	 * @param customCondition the custom condition to add
  356 | 	 * @since 5.3
  357 | 	 */
  358 | 	public RequestMappingInfo addCustomCondition(RequestCondition<?> customCondition) {
  359 | 		return new RequestMappingInfo(this.name,
  360 | 				this.pathPatternsCondition, this.patternsCondition,
  361 | 				this.methodsCondition, this.paramsCondition, this.headersCondition,
  362 | 				this.consumesCondition, this.producesCondition, this.versionCondition,
  363 | 				new RequestConditionHolder(customCondition), this.options);
  364 | 	}
  365 | 
  366 | 	/**
  367 | 	 * Combine "this" request mapping info (i.e. the current instance) with
  368 | 	 * another request mapping info instance.
  369 | 	 * <p>Example: combine type- and method-level request mappings.
  370 | 	 * @return a new request mapping info instance; never {@code null}
  371 | 	 */
  372 | 	@SuppressWarnings("removal")
  373 | 	@Override
  374 | 	public RequestMappingInfo combine(RequestMappingInfo other) {
  375 | 		String name = combineNames(other);
  376 | 
  377 | 		PathPatternsRequestCondition pathPatterns =
  378 | 				(this.pathPatternsCondition != null && other.pathPatternsCondition != null ?
  379 | 						this.pathPatternsCondition.combine(other.pathPatternsCondition) : null);
  380 | 
  381 | 		PatternsRequestCondition patterns =
  382 | 				(this.patternsCondition != null && other.patternsCondition != null ?
  383 | 						this.patternsCondition.combine(other.patternsCondition) : null);
  384 | 
  385 | 		RequestMethodsRequestCondition methods = this.methodsCondition.combine(other.methodsCondition);
  386 | 		ParamsRequestCondition params = this.paramsCondition.combine(other.paramsCondition);
  387 | 		HeadersRequestCondition headers = this.headersCondition.combine(other.headersCondition);
  388 | 		ConsumesRequestCondition consumes = this.consumesCondition.combine(other.consumesCondition);
  389 | 		ProducesRequestCondition produces = this.producesCondition.combine(other.producesCondition);
  390 | 		VersionRequestCondition version = this.versionCondition.combine(other.versionCondition);
  391 | 		RequestConditionHolder custom = this.customConditionHolder.combine(other.customConditionHolder);
  392 | 
  393 | 		return new RequestMappingInfo(name, pathPatterns, patterns, methods,
  394 | 				params, headers, consumes, produces, version, custom, this.options);
  395 | 	}
  396 | 
  397 | 	private @Nullable String combineNames(RequestMappingInfo other) {
  398 | 		if (this.name != null && other.name != null) {
  399 | 			String separator = RequestMappingInfoHandlerMethodMappingNamingStrategy.SEPARATOR;
  400 | 			return this.name + separator + other.name;
  401 | 		}
  402 | 		else if (this.name != null) {
  403 | 			return this.name;
  404 | 		}
  405 | 		else {
  406 | 			return other.name;
  407 | 		}
  408 | 	}
  409 | 
  410 | 	/**
  411 | 	 * Checks if all conditions in this request mapping info match the provided
  412 | 	 * request and returns a potentially new request mapping info with conditions
  413 | 	 * tailored to the current request.
  414 | 	 * <p>For example the returned instance may contain the subset of URL
  415 | 	 * patterns that match to the current request, sorted with best matching
  416 | 	 * patterns on top.
  417 | 	 * @return a new instance in case of a match; or {@code null} otherwise
  418 | 	 */
  419 | 	@SuppressWarnings("removal")
  420 | 	@Override
  421 | 	public @Nullable RequestMappingInfo getMatchingCondition(HttpServletRequest request) {
  422 | 		RequestMethodsRequestCondition methods = this.methodsCondition.getMatchingCondition(request);
  423 | 		if (methods == null) {
  424 | 			return null;
  425 | 		}
  426 | 		ParamsRequestCondition params = this.paramsCondition.getMatchingCondition(request);
  427 | 		if (params == null) {
  428 | 			return null;
  429 | 		}
  430 | 		HeadersRequestCondition headers = this.headersCondition.getMatchingCondition(request);
  431 | 		if (headers == null) {
  432 | 			return null;
  433 | 		}
  434 | 		ConsumesRequestCondition consumes = this.consumesCondition.getMatchingCondition(request);
  435 | 		if (consumes == null) {
  436 | 			return null;
  437 | 		}
  438 | 		ProducesRequestCondition produces = this.producesCondition.getMatchingCondition(request);
  439 | 		if (produces == null) {
  440 | 			return null;
  441 | 		}
  442 | 		VersionRequestCondition version = this.versionCondition.getMatchingCondition(request);
  443 | 		if (version == null) {
  444 | 			return null;
  445 | 		}
  446 | 		PathPatternsRequestCondition pathPatterns = null;
  447 | 		if (this.pathPatternsCondition != null) {
  448 | 			pathPatterns = this.pathPatternsCondition.getMatchingCondition(request);
  449 | 			if (pathPatterns == null) {
  450 | 				return null;
  451 | 			}
  452 | 		}
  453 | 		PatternsRequestCondition patterns = null;
  454 | 		if (this.patternsCondition != null) {
  455 | 			patterns = this.patternsCondition.getMatchingCondition(request);
  456 | 			if (patterns == null) {
  457 | 				return null;
  458 | 			}
  459 | 		}
  460 | 		RequestConditionHolder custom = this.customConditionHolder.getMatchingCondition(request);
  461 | 		if (custom == null) {
  462 | 			return null;
  463 | 		}
  464 | 		return new RequestMappingInfo(this.name, pathPatterns, patterns,
  465 | 				methods, params, headers, consumes, produces, version, custom, this.options, false);
  466 | 	}
  467 | 
  468 | 	/**
  469 | 	 * Compares "this" info (i.e. the current instance) with another info in the
  470 | 	 * context of a request.
  471 | 	 * <p>Note: It is assumed both instances have been obtained via
  472 | 	 * {@link #getMatchingCondition(HttpServletRequest)} to ensure they have
  473 | 	 * conditions with content relevant to current request.
  474 | 	 */
  475 | 	@Override
  476 | 	public int compareTo(RequestMappingInfo other, HttpServletRequest request) {
  477 | 		int result;
  478 | 		// Automatic vs explicit HTTP HEAD mapping
  479 | 		if (HttpMethod.HEAD.matches(request.getMethod())) {
  480 | 			result = this.methodsCondition.compareTo(other.getMethodsCondition(), request);
  481 | 			if (result != 0) {
  482 | 				return result;
  483 | 			}
  484 | 		}
  485 | 		result = getActivePatternsCondition().compareTo(other.getActivePatternsCondition(), request);
  486 | 		if (result != 0) {
  487 | 			return result;
  488 | 		}
  489 | 		result = this.paramsCondition.compareTo(other.getParamsCondition(), request);
  490 | 		if (result != 0) {
  491 | 			return result;
  492 | 		}
  493 | 		result = this.headersCondition.compareTo(other.getHeadersCondition(), request);
  494 | 		if (result != 0) {
  495 | 			return result;
  496 | 		}
  497 | 		result = this.consumesCondition.compareTo(other.getConsumesCondition(), request);
  498 | 		if (result != 0) {
  499 | 			return result;
  500 | 		}
  501 | 		result = this.producesCondition.compareTo(other.getProducesCondition(), request);
  502 | 		if (result != 0) {
  503 | 			return result;
  504 | 		}
  505 | 		result = this.versionCondition.compareTo(other.getVersionCondition(), request);
  506 | 		if (result != 0) {
  507 | 			return result;
  508 | 		}
  509 | 		// Implicit (no method) vs explicit HTTP method mappings
  510 | 		result = this.methodsCondition.compareTo(other.getMethodsCondition(), request);
  511 | 		if (result != 0) {
  512 | 			return result;
  513 | 		}
  514 | 		result = this.customConditionHolder.compareTo(other.customConditionHolder, request);
  515 | 		if (result != 0) {
  516 | 			return result;
  517 | 		}
  518 | 		return 0;
  519 | 	}
  520 | 
  521 | 	@Override
  522 | 	public boolean equals(@Nullable Object other) {
  523 | 		return (this == other || (other instanceof RequestMappingInfo that &&
  524 | 				getActivePatternsCondition().equals(that.getActivePatternsCondition()) &&
  525 | 				this.methodsCondition.equals(that.methodsCondition) &&
  526 | 				this.paramsCondition.equals(that.paramsCondition) &&
  527 | 				this.headersCondition.equals(that.headersCondition) &&
  528 | 				this.consumesCondition.equals(that.consumesCondition) &&
  529 | 				this.producesCondition.equals(that.producesCondition) &&
  530 | 				this.versionCondition.equals(that.versionCondition) &&
  531 | 				this.customConditionHolder.equals(that.customConditionHolder)));
  532 | 	}
  533 | 
  534 | 	@Override
  535 | 	public int hashCode() {
  536 | 		if (this.hashCode == null) {
  537 | 			this.hashCode = calculateHashCode();
  538 | 		}
  539 | 		return this.hashCode;
  540 | 	}
  541 | 
  542 | 	@SuppressWarnings({"ConstantConditions", "NullAway", "removal"})
  543 | 	private int calculateHashCode() {
  544 | 
  545 | 		int result = (this.pathPatternsCondition != null ?
  546 | 				this.pathPatternsCondition : this.patternsCondition).hashCode();
  547 | 
  548 | 		result = 31 * result + this.methodsCondition.hashCode();
  549 | 		result = 31 * result + this.paramsCondition.hashCode();
  550 | 		result = 31 * result + this.headersCondition.hashCode();
  551 | 		result = 31 * result + this.consumesCondition.hashCode();
  552 | 		result = 31 * result + this.producesCondition.hashCode();
  553 | 		result = 31 * result + this.versionCondition.hashCode();
  554 | 		result = 31 * result + this.customConditionHolder.hashCode();
  555 | 		return result;
  556 | 	}
  557 | 
  558 | 	@Override
  559 | 	public String toString() {
  560 | 		StringBuilder builder = new StringBuilder("{");
  561 | 		if (!this.methodsCondition.isEmpty()) {
  562 | 			Set<RequestMethod> httpMethods = this.methodsCondition.getMethods();
  563 | 			builder.append(httpMethods.size() == 1 ? httpMethods.iterator().next() : httpMethods);
  564 | 		}
  565 | 
  566 | 		// Patterns conditions are never empty and have "" (empty path) at least.
  567 | 		builder.append(' ').append(getActivePatternsCondition());
  568 | 
  569 | 		if (!this.paramsCondition.isEmpty()) {
  570 | 			builder.append(", params ").append(this.paramsCondition);
  571 | 		}
  572 | 		if (!this.headersCondition.isEmpty()) {
  573 | 			builder.append(", headers ").append(this.headersCondition);
  574 | 		}
  575 | 		if (!this.consumesCondition.isEmpty()) {
  576 | 			builder.append(", consumes ").append(this.consumesCondition);
  577 | 		}
  578 | 		if (!this.producesCondition.isEmpty()) {
  579 | 			builder.append(", produces ").append(this.producesCondition);
  580 | 		}
  581 | 		if (!this.versionCondition.isEmpty()) {
  582 | 			builder.append(", version ").append(this.versionCondition);
  583 | 		}
  584 | 		if (!this.customConditionHolder.isEmpty()) {
  585 | 			builder.append(", and ").append(this.customConditionHolder);
  586 | 		}
  587 | 		builder.append('}');
  588 | 		return builder.toString();
  589 | 	}
  590 | 
  591 | 	/**
  592 | 	 * Return a builder to create a new RequestMappingInfo by modifying this one.
  593 | 	 * @return a builder to create a new, modified instance
  594 | 	 * @since 5.3.4
  595 | 	 */
  596 | 	public Builder mutate() {
  597 | 		return new MutateBuilder(this);
  598 | 	}
  599 | 
  600 | 
  601 | 	/**
  602 | 	 * Create a new {@code RequestMappingInfo.Builder} with the given paths.
  603 | 	 * @param paths the paths to use
  604 | 	 * @since 4.2
  605 | 	 */
  606 | 	public static Builder paths(String... paths) {
  607 | 		return new DefaultBuilder(paths);
  608 | 	}
  609 | 
  610 | 
  611 | 	/**
  612 | 	 * Defines a builder for creating a RequestMappingInfo.
  613 | 	 * @since 4.2
  614 | 	 */
  615 | 	public interface Builder {
  616 | 
  617 | 		/**
  618 | 		 * Set the URL path patterns.
  619 | 		 */
  620 | 		Builder paths(String... paths);
  621 | 
  622 | 		/**
  623 | 		 * Set the request method conditions.
  624 | 		 */
  625 | 		Builder methods(RequestMethod... methods);
  626 | 
  627 | 		/**
  628 | 		 * Set the request param conditions.
  629 | 		 */
  630 | 		Builder params(String... params);
  631 | 
  632 | 		/**
  633 | 		 * Set the header conditions.
  634 | 		 * <p>By default this is not set.
  635 | 		 */
  636 | 		Builder headers(String... headers);
  637 | 
  638 | 		/**
  639 | 		 * Set the consumes conditions.
  640 | 		 */
  641 | 		Builder consumes(String... consumes);
  642 | 
  643 | 		/**
  644 | 		 * Set the produces conditions.
  645 | 		 */
  646 | 		Builder produces(String... produces);
  647 | 
  648 | 		/**
  649 | 		 * Set the API version condition.
  650 | 		 * @since 7.0
  651 | 		 */
  652 | 		Builder version(String version);
  653 | 
  654 | 		/**
  655 | 		 * Set the mapping name.
  656 | 		 */
  657 | 		Builder mappingName(String name);
  658 | 
  659 | 		/**
  660 | 		 * Set a custom condition to use.
  661 | 		 */
  662 | 		Builder customCondition(RequestCondition<?> condition);
  663 | 
  664 | 		/**
  665 | 		 * Provide additional configuration needed for request mapping purposes.
  666 | 		 */
  667 | 		Builder options(BuilderConfiguration options);
  668 | 
  669 | 		/**
  670 | 		 * Build the RequestMappingInfo.
  671 | 		 */
  672 | 		RequestMappingInfo build();
  673 | 	}
  674 | 
  675 | 
  676 | 	private static class DefaultBuilder implements Builder {
  677 | 
  678 | 		private String[] paths;
  679 | 
  680 | 		private RequestMethod[] methods = new RequestMethod[0];
  681 | 
  682 | 		private String[] params = new String[0];
  683 | 
  684 | 		private String[] headers = new String[0];
  685 | 
  686 | 		private String[] consumes = new String[0];
  687 | 
  688 | 		private String[] produces = new String[0];
  689 | 
  690 | 		private @Nullable String version;
  691 | 
  692 | 		private boolean hasContentType;
  693 | 
  694 | 		private boolean hasAccept;
  695 | 
  696 | 		private @Nullable String mappingName;
  697 | 
  698 | 		private @Nullable RequestCondition<?> customCondition;
  699 | 
  700 | 		private BuilderConfiguration options = new BuilderConfiguration();
  701 | 
  702 | 		public DefaultBuilder(String... paths) {
  703 | 			this.paths = paths;
  704 | 		}
  705 | 
  706 | 		@Override
  707 | 		public Builder paths(String... paths) {
  708 | 			this.paths = paths;
  709 | 			return this;
  710 | 		}
  711 | 
  712 | 		@Override
  713 | 		public DefaultBuilder methods(RequestMethod... methods) {
  714 | 			this.methods = methods;
  715 | 			return this;
  716 | 		}
  717 | 
  718 | 		@Override
  719 | 		public DefaultBuilder params(String... params) {
  720 | 			this.params = params;
  721 | 			return this;
  722 | 		}
  723 | 
  724 | 		@Override
  725 | 		public DefaultBuilder headers(String... headers) {
  726 | 			for (String header : headers) {
  727 | 				this.hasContentType = this.hasContentType ||
  728 | 						header.contains("Content-Type") || header.contains("content-type");
  729 | 				this.hasAccept = this.hasAccept ||
  730 | 						header.contains("Accept") || header.contains("accept");
  731 | 			}
  732 | 			this.headers = headers;
  733 | 			return this;
  734 | 		}
  735 | 
  736 | 		@Override
  737 | 		public DefaultBuilder consumes(String... consumes) {
  738 | 			this.consumes = consumes;
  739 | 			return this;
  740 | 		}
  741 | 
  742 | 		@Override
  743 | 		public DefaultBuilder produces(String... produces) {
  744 | 			this.produces = produces;
  745 | 			return this;
  746 | 		}
  747 | 
  748 | 		@Override
  749 | 		public Builder version(String version) {
  750 | 			this.version = version;
  751 | 			return this;
  752 | 		}
  753 | 
  754 | 		@Override
  755 | 		public DefaultBuilder mappingName(String name) {
  756 | 			this.mappingName = name;
  757 | 			return this;
  758 | 		}
  759 | 
  760 | 		@Override
  761 | 		public DefaultBuilder customCondition(RequestCondition<?> condition) {
  762 | 			this.customCondition = condition;
  763 | 			return this;
  764 | 		}
  765 | 
  766 | 		@Override
  767 | 		public Builder options(BuilderConfiguration options) {
  768 | 			this.options = options;
  769 | 			return this;
  770 | 		}
  771 | 
  772 | 		@SuppressWarnings("removal")
  773 | 		@Override
  774 | 		public RequestMappingInfo build() {
  775 | 
  776 | 			PathPatternsRequestCondition pathPatternsCondition = null;
  777 | 			PatternsRequestCondition patternsCondition = null;
  778 | 			PathPatternParser parser = this.options.getPatternParserToUse();
  779 | 			if (parser != null) {
  780 | 				pathPatternsCondition = (ObjectUtils.isEmpty(this.paths) ?
  781 | 						EMPTY_PATH_PATTERNS :
  782 | 						new PathPatternsRequestCondition(parser, this.paths));
  783 | 			}
  784 | 			else {
  785 | 				patternsCondition = (ObjectUtils.isEmpty(this.paths) ?
  786 | 						EMPTY_PATTERNS :
  787 | 						new PatternsRequestCondition(this.paths, null, this.options.pathMatcher));
  788 | 			}
  789 | 
  790 | 			ContentNegotiationManager manager = this.options.getContentNegotiationManager();
  791 | 
  792 | 			ApiVersionStrategy strategy = this.options.getApiVersionStrategy();
  793 | 			Assert.state(strategy != null || !StringUtils.hasText(this.version),
  794 | 					"API version specified, but no ApiVersionStrategy configured");
  795 | 			VersionRequestCondition versionCondition = new VersionRequestCondition(this.version, strategy);
  796 | 
  797 | 			return new RequestMappingInfo(
  798 | 					this.mappingName, pathPatternsCondition, patternsCondition,
  799 | 					ObjectUtils.isEmpty(this.methods) ?
  800 | 							EMPTY_REQUEST_METHODS : new RequestMethodsRequestCondition(this.methods),
  801 | 					ObjectUtils.isEmpty(this.params) ?
  802 | 							EMPTY_PARAMS : new ParamsRequestCondition(this.params),
  803 | 					ObjectUtils.isEmpty(this.headers) ?
  804 | 							EMPTY_HEADERS : new HeadersRequestCondition(this.headers),
  805 | 					ObjectUtils.isEmpty(this.consumes) && !this.hasContentType ?
  806 | 							EMPTY_CONSUMES : new ConsumesRequestCondition(this.consumes, this.headers),
  807 | 					ObjectUtils.isEmpty(this.produces) && !this.hasAccept ?
  808 | 							EMPTY_PRODUCES : new ProducesRequestCondition(this.produces, this.headers, manager),
  809 | 					versionCondition,
  810 | 					this.customCondition != null ?
  811 | 							new RequestConditionHolder(this.customCondition) : EMPTY_CUSTOM, this.options);
  812 | 		}
  813 | 	}
  814 | 
  815 | 
  816 | 	private static class MutateBuilder implements Builder {
  817 | 
  818 | 		private @Nullable String name;
  819 | 
  820 | 		private @Nullable PathPatternsRequestCondition pathPatternsCondition;
  821 | 
  822 | 		@SuppressWarnings("removal")
  823 | 		private @Nullable PatternsRequestCondition patternsCondition;
  824 | 
  825 | 		private RequestMethodsRequestCondition methodsCondition;
  826 | 
  827 | 		private ParamsRequestCondition paramsCondition;
  828 | 
  829 | 		private HeadersRequestCondition headersCondition;
  830 | 
  831 | 		private ConsumesRequestCondition consumesCondition;
  832 | 
  833 | 		private ProducesRequestCondition producesCondition;
  834 | 
  835 | 		private VersionRequestCondition versionCondition;
  836 | 
  837 | 		private RequestConditionHolder customConditionHolder;
  838 | 
  839 | 		private BuilderConfiguration options;
  840 | 
  841 | 		public MutateBuilder(RequestMappingInfo other) {
  842 | 			this.name = other.name;
  843 | 			this.pathPatternsCondition = other.pathPatternsCondition;
  844 | 			this.patternsCondition = other.patternsCondition;
  845 | 			this.methodsCondition = other.methodsCondition;
  846 | 			this.paramsCondition = other.paramsCondition;
  847 | 			this.headersCondition = other.headersCondition;
  848 | 			this.consumesCondition = other.consumesCondition;
  849 | 			this.producesCondition = other.producesCondition;
  850 | 			this.versionCondition = other.versionCondition;
  851 | 			this.customConditionHolder = other.customConditionHolder;
  852 | 			this.options = other.options;
  853 | 		}
  854 | 
  855 | 		@SuppressWarnings("removal")
  856 | 		@Override
  857 | 		public Builder paths(String... paths) {
  858 | 			PathPatternParser parser = this.options.getPatternParserToUse();
  859 | 
  860 | 			if (parser != null) {
  861 | 				this.pathPatternsCondition = (ObjectUtils.isEmpty(paths) ?
  862 | 						EMPTY_PATH_PATTERNS : new PathPatternsRequestCondition(parser, paths));
  863 | 			}
  864 | 			else {
  865 | 				this.patternsCondition = (ObjectUtils.isEmpty(paths) ?
  866 | 						EMPTY_PATTERNS :
  867 | 						new PatternsRequestCondition(paths, null, this.options.getPathMatcher()));
  868 | 			}
  869 | 			return this;
  870 | 		}
  871 | 
  872 | 		@Override
  873 | 		public Builder methods(RequestMethod... methods) {
  874 | 			this.methodsCondition = (ObjectUtils.isEmpty(methods) ?
  875 | 					EMPTY_REQUEST_METHODS : new RequestMethodsRequestCondition(methods));
  876 | 			return this;
  877 | 		}
  878 | 
  879 | 		@Override
  880 | 		public Builder params(String... params) {
  881 | 			this.paramsCondition = (ObjectUtils.isEmpty(params) ?
  882 | 					EMPTY_PARAMS : new ParamsRequestCondition(params));
  883 | 			return this;
  884 | 		}
  885 | 
  886 | 		@Override
  887 | 		public Builder headers(String... headers) {
  888 | 			this.headersCondition = (ObjectUtils.isEmpty(headers) ?
  889 | 					EMPTY_HEADERS : new HeadersRequestCondition(headers));
  890 | 			return this;
  891 | 		}
  892 | 
  893 | 		@Override
  894 | 		public Builder consumes(String... consumes) {
  895 | 			this.consumesCondition = (ObjectUtils.isEmpty(consumes) ?
  896 | 					EMPTY_CONSUMES : new ConsumesRequestCondition(consumes));
  897 | 			return this;
  898 | 		}
  899 | 
  900 | 		@Override
  901 | 		public Builder produces(String... produces) {
  902 | 			this.producesCondition = (ObjectUtils.isEmpty(produces) ?
  903 | 					EMPTY_PRODUCES :
  904 | 					new ProducesRequestCondition(produces, null, this.options.getContentNegotiationManager()));
  905 | 			return this;
  906 | 		}
  907 | 
  908 | 		@Override
  909 | 		public Builder version(@Nullable String version) {
  910 | 			ApiVersionStrategy strategy = this.options.getApiVersionStrategy();
  911 | 			Assert.state(strategy != null || !StringUtils.hasText(version),
  912 | 					"API version specified, but no ApiVersionStrategy configured");
  913 | 			this.versionCondition = new VersionRequestCondition(version, strategy);
  914 | 			return this;
  915 | 		}
  916 | 
  917 | 		@Override
  918 | 		public Builder mappingName(String name) {
  919 | 			this.name = name;
  920 | 			return this;
  921 | 		}
  922 | 
  923 | 		@Override
  924 | 		public Builder customCondition(RequestCondition<?> condition) {
  925 | 			this.customConditionHolder = new RequestConditionHolder(condition);
  926 | 			return this;
  927 | 		}
  928 | 
  929 | 		@Override
  930 | 		public Builder options(BuilderConfiguration options) {
  931 | 			this.options = options;
  932 | 			return this;
  933 | 		}
  934 | 
  935 | 		@Override
  936 | 		public RequestMappingInfo build() {
  937 | 			return new RequestMappingInfo(this.name,
  938 | 					this.pathPatternsCondition, this.patternsCondition,
  939 | 					this.methodsCondition, this.paramsCondition, this.headersCondition,
  940 | 					this.consumesCondition, this.producesCondition, this.versionCondition,
  941 | 					this.customConditionHolder, this.options);
  942 | 		}
  943 | 	}
  944 | 
  945 | 
  946 | 	/**
  947 | 	 * Container for configuration options used for request mapping purposes.
  948 | 	 * Such configuration is required to create RequestMappingInfo instances but
  949 | 	 * is typically used across all RequestMappingInfo instances.
  950 | 	 * @since 4.2
  951 | 	 * @see Builder#options
  952 | 	 */
  953 | 	public static class BuilderConfiguration {
  954 | 
  955 | 		private static final PathPatternParser defaultPatternParser = new PathPatternParser();
  956 | 
  957 | 
  958 | 		private @Nullable PathPatternParser patternParser;
  959 | 
  960 | 		private @Nullable PathMatcher pathMatcher;
  961 | 
  962 | 		private @Nullable ContentNegotiationManager contentNegotiationManager;
  963 | 
  964 | 		private @Nullable ApiVersionStrategy apiVersionStrategy;
  965 | 
  966 | 
  967 | 		/**
  968 | 		 * Enable use of parsed {@link PathPattern}s as described in
  969 | 		 * {@link AbstractHandlerMapping#setPatternParser(PathPatternParser)}.
  970 | 		 * <p><strong>Note:</strong> This property is mutually exclusive with
  971 | 		 * {@link #setPathMatcher(PathMatcher)}.
  972 | 		 * <p>By default, this is not set, but {@link RequestMappingInfo.Builder}
  973 | 		 * defaults to using {@link PathPatternParser} unless
  974 | 		 * {@link #setPathMatcher(PathMatcher)} is explicitly set.
  975 | 		 * @since 5.3
  976 | 		 */
  977 | 		public void setPatternParser(@Nullable PathPatternParser patternParser) {
  978 | 			this.patternParser = patternParser;
  979 | 		}
  980 | 
  981 | 		/**
  982 | 		 * Return the {@link #setPatternParser(PathPatternParser) configured}
  983 | 		 * {@code PathPatternParser}, or {@code null}.
  984 | 		 * @since 5.3
  985 | 		 */
  986 | 		public @Nullable PathPatternParser getPatternParser() {
  987 | 			return this.patternParser;
  988 | 		}
  989 | 
  990 | 		/**
  991 | 		 * Set a custom UrlPathHelper to use for the PatternsRequestCondition.
  992 | 		 * <p>By default this is not set.
  993 | 		 * @since 4.2.8
  994 | 		 * @deprecated the path is resolved externally and obtained with
  995 | 		 * {@link ServletRequestPathUtils#getCachedPathValue(ServletRequest)}
  996 | 		 */
  997 | 		@Deprecated(since = "5.3")
  998 | 		public void setUrlPathHelper(@Nullable UrlPathHelper urlPathHelper) {
  999 | 		}
 1000 | 
 1001 | 		/**
 1002 | 		 * Return the configured UrlPathHelper.
 1003 | 		 * @deprecated the path is resolved externally and obtained with
 1004 | 		 * {@link ServletRequestPathUtils#getCachedPathValue(ServletRequest)};
 1005 | 		 * this method always returns {@link UrlPathHelper#defaultInstance}.
 1006 | 		 */
 1007 | 		@Deprecated(since = "5.3")
 1008 | 		public @Nullable UrlPathHelper getUrlPathHelper() {
 1009 | 			return UrlPathHelper.defaultInstance;
 1010 | 		}
 1011 | 
 1012 | 		/**
 1013 | 		 * Set a custom PathMatcher to use for the PatternsRequestCondition.
 1014 | 		 * <p>By default, this is not set. You must set it explicitly if you want
 1015 | 		 * {@link PathMatcher} to be used, or otherwise {@link RequestMappingInfo}
 1016 | 		 * defaults to using {@link PathPatternParser}.
 1017 | 		 * @deprecated use of {@link PathMatcher} and {@link UrlPathHelper} is deprecated
 1018 | 		 * for use at runtime in web modules in favor of parsed patterns with
 1019 | 		 * {@link PathPatternParser}.
 1020 | 		 */
 1021 | 		@Deprecated(since = "7.0", forRemoval = true)
 1022 | 		public void setPathMatcher(@Nullable PathMatcher pathMatcher) {
 1023 | 			this.pathMatcher = pathMatcher;
 1024 | 		}
 1025 | 
 1026 | 		/**
 1027 | 		 * Return a custom PathMatcher to use for the PatternsRequestCondition, if any.
 1028 | 		 * @deprecated use of {@link PathMatcher} and {@link UrlPathHelper} is deprecated
 1029 | 		 * for use at runtime in web modules in favor of parsed patterns with
 1030 | 		 * {@link PathPatternParser}.
 1031 | 		 */
 1032 | 		@Deprecated(since = "7.0", forRemoval = true)
 1033 | 		public @Nullable PathMatcher getPathMatcher() {
 1034 | 			return this.pathMatcher;
 1035 | 		}
 1036 | 
 1037 | 		/**
 1038 | 		 * Return the {@code PathPatternParser} to use, the one set explicitly
 1039 | 		 * or falling back on a default instance if both {@code PathPatternParser}
 1040 | 		 * and {@code PathMatcher} are not set.
 1041 | 		 * @since 6.1.2
 1042 | 		 */
 1043 | 		public @Nullable PathPatternParser getPatternParserToUse() {
 1044 | 			if (this.patternParser == null && this.pathMatcher == null) {
 1045 | 				return defaultPatternParser;
 1046 | 			}
 1047 | 			return this.patternParser;
 1048 | 		}
 1049 | 
 1050 | 		/**
 1051 | 		 * Set the ContentNegotiationManager to use for the ProducesRequestCondition.
 1052 | 		 * <p>By default, this is not set.
 1053 | 		 */
 1054 | 		public void setContentNegotiationManager(ContentNegotiationManager contentNegotiationManager) {
 1055 | 			this.contentNegotiationManager = contentNegotiationManager;
 1056 | 		}
 1057 | 
 1058 | 		/**
 1059 | 		 * Return the ContentNegotiationManager to use for the ProducesRequestCondition,
 1060 | 		 * if any.
 1061 | 		 */
 1062 | 		public @Nullable ContentNegotiationManager getContentNegotiationManager() {
 1063 | 			return this.contentNegotiationManager;
 1064 | 		}
 1065 | 
 1066 | 		/**
 1067 | 		 * Set the strategy for API versioning.
 1068 | 		 * @param apiVersionStrategy the strategy to use
 1069 | 		 * @since 7.0
 1070 | 		 */
 1071 | 		public void setApiVersionStrategy(@Nullable ApiVersionStrategy apiVersionStrategy) {
 1072 | 			this.apiVersionStrategy = apiVersionStrategy;
 1073 | 		}
 1074 | 
 1075 | 		/**
 1076 | 		 * Return the configured strategy for API versioning.
 1077 | 		 * @since 7.0
 1078 | 		 */
 1079 | 		public @Nullable ApiVersionStrategy getApiVersionStrategy() {
 1080 | 			return this.apiVersionStrategy;
 1081 | 		}
 1082 | 	}
 1083 | 
 1084 | }
```
