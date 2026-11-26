<!-- learning-docs source archive（机械抓取原文，禁止手改代码内容） -->
<!-- origin: org.springframework:spring-webmvc:7.1.0 | path: org/springframework/web/servlet/handler/MappedInterceptor.java -->
<!-- 正文引用：[[源码:MappedInterceptor.java#L起-L止]] ；代码块头部：(src: MappedInterceptor.java#L起-L止) -->

# MappedInterceptor.java

| 项 | 值 |
| --- | --- |
| 来源 | `org.springframework:spring-webmvc:7.1.0` |
| 路径 | `org/springframework/web/servlet/handler/MappedInterceptor.java` |
| 行数 | 353 |
| 在线地址 | https://repo1.maven.org/maven2/org/springframework/spring-webmvc/7.1.0/spring-webmvc-7.1.0-sources.jar |
| 抓取时间 | 2026-11-25T14:01:10.826Z |

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
   19 | import java.util.Arrays;
   20 | 
   21 | import jakarta.servlet.http.HttpServletRequest;
   22 | import jakarta.servlet.http.HttpServletResponse;
   23 | import org.jspecify.annotations.Nullable;
   24 | 
   25 | import org.springframework.http.HttpMethod;
   26 | import org.springframework.http.server.PathContainer;
   27 | import org.springframework.util.AntPathMatcher;
   28 | import org.springframework.util.ObjectUtils;
   29 | import org.springframework.util.PathMatcher;
   30 | import org.springframework.web.context.request.WebRequestInterceptor;
   31 | import org.springframework.web.servlet.HandlerInterceptor;
   32 | import org.springframework.web.servlet.HandlerMapping;
   33 | import org.springframework.web.servlet.ModelAndView;
   34 | import org.springframework.web.util.ServletRequestPathUtils;
   35 | import org.springframework.web.util.UrlPathHelper;
   36 | import org.springframework.web.util.pattern.PathPattern;
   37 | import org.springframework.web.util.pattern.PathPatternParser;
   38 | import org.springframework.web.util.pattern.PatternParseException;
   39 | 
   40 | /**
   41 |  * Wraps a {@link HandlerInterceptor} and uses URL patterns to determine whether
   42 |  * it applies to a given request.
   43 |  *
   44 |  * <p>Pattern matching can be done with a {@link PathMatcher} or with a parsed
   45 |  * {@link PathPattern}. The syntax is largely the same with the latter being more
   46 |  * tailored for web usage and more efficient. The choice is driven by the
   47 |  * presence of a {@linkplain UrlPathHelper#resolveAndCacheLookupPath resolved}
   48 |  * {@code String} lookupPath or a {@linkplain ServletRequestPathUtils#parseAndCache
   49 |  * parsed} {@code RequestPath} which in turn depends on the {@link HandlerMapping}
   50 |  * that matched the current request.
   51 |  *
   52 |  * <p>{@code MappedInterceptor} is supported by subclasses of
   53 |  * {@link org.springframework.web.servlet.handler.AbstractHandlerMethodMapping
   54 |  * AbstractHandlerMethodMapping} which detect beans of type {@code MappedInterceptor}
   55 |  * and also check if interceptors directly registered with it are of this type.
   56 |  *
   57 |  * @author Keith Donald
   58 |  * @author Rossen Stoyanchev
   59 |  * @author Brian Clozel
   60 |  * @since 3.0
   61 |  */
   62 | public final class MappedInterceptor implements HandlerInterceptor {
   63 | 
   64 | 	private static final PathMatcher defaultPathMatcher = new AntPathMatcher();
   65 | 
   66 | 
   67 | 	private final PatternAdapter @Nullable [] includePatterns;
   68 | 
   69 | 	private final PatternAdapter @Nullable [] excludePatterns;
   70 | 
   71 | 	private final HttpMethod @Nullable [] includeHttpMethods;
   72 | 
   73 | 	private final HttpMethod @Nullable [] excludeHttpMethods;
   74 | 
   75 | 	private PathMatcher pathMatcher = defaultPathMatcher;
   76 | 
   77 | 	private final HandlerInterceptor interceptor;
   78 | 
   79 | 
   80 | 	/**
   81 | 	 * Create an instance with the given include and exclude patterns and HTTP methods.
   82 | 	 * @param includePatterns patterns to match, or null to match all paths
   83 | 	 * @param excludePatterns patterns for which requests must not match
   84 | 	 * @param includeHttpMethods the HTTP methods to match, or null for all methods
   85 | 	 * @param excludeHttpMethods the ßHTTP methods to which requests must not match
   86 | 	 * @param interceptor the target interceptor
   87 | 	 * @param parser a parser to use to pre-parse patterns into {@link PathPattern};
   88 | 	 * when not provided, {@link PathPatternParser#defaultInstance} is used.
   89 | 	 * @since 7.0
   90 | 	 */
   91 | 	public MappedInterceptor(String @Nullable [] includePatterns, String @Nullable [] excludePatterns,
   92 | 			HttpMethod @Nullable [] includeHttpMethods, HttpMethod @Nullable [] excludeHttpMethods,
   93 | 			HandlerInterceptor interceptor, @Nullable PathPatternParser parser) {
   94 | 
   95 | 		this.includePatterns = PatternAdapter.initPatterns(includePatterns, parser);
   96 | 		this.excludePatterns = PatternAdapter.initPatterns(excludePatterns, parser);
   97 | 		this.includeHttpMethods = includeHttpMethods;
   98 | 		this.excludeHttpMethods = excludeHttpMethods;
   99 | 		this.interceptor = interceptor;
  100 | 	}
  101 | 
  102 | 	/**
  103 | 	 * Variation of
  104 | 	 * {@link #MappedInterceptor(String[], String[], HttpMethod[], HttpMethod[], HandlerInterceptor, PathPatternParser)}
  105 | 	 * without HTTP methods.
  106 | 	 * @since 5.3
  107 | 	 * @deprecated in favor of the constructor variant with HTTP methods
  108 | 	 */
  109 | 	@Deprecated(since = "7.0", forRemoval = true)
  110 | 	public MappedInterceptor(String @Nullable [] includePatterns, String @Nullable [] excludePatterns,
  111 | 			HandlerInterceptor interceptor, @Nullable PathPatternParser parser) {
  112 | 
  113 | 		this(includePatterns, excludePatterns, null, null, interceptor, parser);
  114 | 	}
  115 | 
  116 | 	/**
  117 | 	 * Variant of
  118 | 	 * {@link #MappedInterceptor(String[], String[], HttpMethod[], HttpMethod[], HandlerInterceptor, PathPatternParser)}
  119 | 	 * with include patterns only.
  120 | 	 */
  121 | 	public MappedInterceptor(String @Nullable [] includePatterns, HandlerInterceptor interceptor) {
  122 | 		this(includePatterns, null, null, null, interceptor, null);
  123 | 	}
  124 | 
  125 | 	/**
  126 | 	 * Variant of
  127 | 	 * {@link #MappedInterceptor(String[], String[], HttpMethod[], HttpMethod[], HandlerInterceptor, PathPatternParser)}
  128 | 	 * without a provided parser.
  129 | 	 */
  130 | 	public MappedInterceptor(String @Nullable [] includePatterns, String @Nullable [] excludePatterns,
  131 | 			HandlerInterceptor interceptor) {
  132 | 
  133 | 		this(includePatterns, excludePatterns, null, null, interceptor, null);
  134 | 	}
  135 | 
  136 | 	/**
  137 | 	 * Variant of
  138 | 	 * {@link #MappedInterceptor(String[], String[], HttpMethod[], HttpMethod[], HandlerInterceptor, PathPatternParser)}
  139 | 	 * with a {@link WebRequestInterceptor} as the target.
  140 | 	 */
  141 | 	public MappedInterceptor(String @Nullable [] includePatterns, WebRequestInterceptor interceptor) {
  142 | 		this(includePatterns, null, interceptor);
  143 | 	}
  144 | 
  145 | 	/**
  146 | 	 * Variant of
  147 | 	 * {@link #MappedInterceptor(String[], String[], HttpMethod[], HttpMethod[], HandlerInterceptor, PathPatternParser)}
  148 | 	 * with a {@link WebRequestInterceptor} as the target.
  149 | 	 */
  150 | 	public MappedInterceptor(String @Nullable [] includePatterns, String @Nullable [] excludePatterns,
  151 | 			WebRequestInterceptor interceptor) {
  152 | 
  153 | 		this(includePatterns, excludePatterns, null, null, new WebRequestHandlerInterceptorAdapter(interceptor), null);
  154 | 	}
  155 | 
  156 | 
  157 | 	/**
  158 | 	 * Get the include path patterns this interceptor is mapped to.
  159 | 	 * @since 6.1
  160 | 	 * @see #getExcludePathPatterns()
  161 | 	 */
  162 | 	public String @Nullable [] getIncludePathPatterns() {
  163 | 		return (!ObjectUtils.isEmpty(this.includePatterns) ?
  164 | 				Arrays.stream(this.includePatterns).map(PatternAdapter::getPatternString).toArray(String[]::new) :
  165 | 				null);
  166 | 	}
  167 | 
  168 | 	/**
  169 | 	 * Get the exclude path patterns this interceptor is mapped to.
  170 | 	 * @since 6.1
  171 | 	 * @see #getIncludePathPatterns()
  172 | 	 */
  173 | 	public String @Nullable [] getExcludePathPatterns() {
  174 | 		return (!ObjectUtils.isEmpty(this.excludePatterns) ?
  175 | 				Arrays.stream(this.excludePatterns).map(PatternAdapter::getPatternString).toArray(String[]::new) :
  176 | 				null);
  177 | 	}
  178 | 
  179 | 	/**
  180 | 	 * The target {@link HandlerInterceptor} to invoke in case of a match.
  181 | 	 */
  182 | 	public HandlerInterceptor getInterceptor() {
  183 | 		return this.interceptor;
  184 | 	}
  185 | 
  186 | 	/**
  187 | 	 * Configure the PathMatcher to use to match URL paths with against include
  188 | 	 * and exclude patterns.
  189 | 	 * <p>This is an advanced property that should be used only when a
  190 | 	 * customized {@link AntPathMatcher} or a custom PathMatcher is required.
  191 | 	 * <p>By default this is {@link AntPathMatcher}.
  192 | 	 * <p><strong>Note:</strong> Setting {@code PathMatcher} enforces use of
  193 | 	 * String pattern matching even when a
  194 | 	 * {@linkplain ServletRequestPathUtils#parseAndCache parsed} {@code RequestPath}
  195 | 	 * is available.
  196 | 	 * @deprecated use of {@link PathMatcher} and {@link UrlPathHelper} is deprecated
  197 | 	 * for use at runtime in web modules in favor of parsed patterns with
  198 | 	 * {@link PathPatternParser}.
  199 | 	 */
  200 | 	@Deprecated(since = "7.0", forRemoval = true)
  201 | 	public void setPathMatcher(PathMatcher pathMatcher) {
  202 | 		this.pathMatcher = pathMatcher;
  203 | 	}
  204 | 
  205 | 	/**
  206 | 	 * Get the {@linkplain #setPathMatcher(PathMatcher) configured} PathMatcher.
  207 | 	 * @deprecated use of {@link PathMatcher} and {@link UrlPathHelper} is deprecated
  208 | 	 * for use at runtime in web modules in favor of parsed patterns with
  209 | 	 * {@link PathPatternParser}.
  210 | 	 */
  211 | 	@Deprecated(since = "7.0", forRemoval = true)
  212 | 	public PathMatcher getPathMatcher() {
  213 | 		return this.pathMatcher;
  214 | 	}
  215 | 
  216 | 
  217 | 	/**
  218 | 	 * Check whether this interceptor is mapped to the request.
  219 | 	 * <p>The request mapping path is expected to have been resolved externally.
  220 | 	 * See also class-level Javadoc.
  221 | 	 * @param request the request to match to
  222 | 	 * @return {@code true} if the interceptor should be applied to the request
  223 | 	 */
  224 | 	public boolean matches(HttpServletRequest request) {
  225 | 		Object path = ServletRequestPathUtils.getCachedPath(request);
  226 | 		HttpMethod httpMethod = HttpMethod.valueOf(request.getMethod());
  227 | 		if (this.pathMatcher != defaultPathMatcher) {
  228 | 			path = path.toString();
  229 | 		}
  230 | 		boolean isPathContainer = (path instanceof PathContainer);
  231 | 		if (!ObjectUtils.isEmpty(this.excludePatterns)) {
  232 | 			for (PatternAdapter adapter : this.excludePatterns) {
  233 | 				if (adapter.match(path, isPathContainer, this.pathMatcher)) {
  234 | 					return false;
  235 | 				}
  236 | 			}
  237 | 		}
  238 | 		if (!ObjectUtils.isEmpty(this.excludeHttpMethods)) {
  239 | 			for (HttpMethod excluded : this.excludeHttpMethods) {
  240 | 				if (excluded == httpMethod) {
  241 | 					return false;
  242 | 				}
  243 | 			}
  244 | 		}
  245 | 		if (!ObjectUtils.isEmpty(this.includePatterns)) {
  246 | 			boolean match = false;
  247 | 			for (PatternAdapter adapter : this.includePatterns) {
  248 | 				if (adapter.match(path, isPathContainer, this.pathMatcher)) {
  249 | 					match = true;
  250 | 					break;
  251 | 				}
  252 | 			}
  253 | 			if (!match) {
  254 | 				return false;
  255 | 			}
  256 | 		}
  257 | 		if (!ObjectUtils.isEmpty(this.includeHttpMethods)) {
  258 | 			boolean match = false;
  259 | 			for (HttpMethod included : this.includeHttpMethods) {
  260 | 				if (included == httpMethod) {
  261 | 					match = true;
  262 | 					break;
  263 | 				}
  264 | 			}
  265 | 			if (!match) {
  266 | 				return false;
  267 | 			}
  268 | 		}
  269 | 		return true;
  270 | 	}
  271 | 
  272 | 
  273 | 	// HandlerInterceptor delegation
  274 | 
  275 | 	@Override
  276 | 	public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler)
  277 | 			throws Exception {
  278 | 
  279 | 		return this.interceptor.preHandle(request, response, handler);
  280 | 	}
  281 | 
  282 | 	@Override
  283 | 	public void postHandle(HttpServletRequest request, HttpServletResponse response, Object handler,
  284 | 			@Nullable ModelAndView modelAndView) throws Exception {
  285 | 
  286 | 		this.interceptor.postHandle(request, response, handler, modelAndView);
  287 | 	}
  288 | 
  289 | 	@Override
  290 | 	public void afterCompletion(HttpServletRequest request, HttpServletResponse response, Object handler,
  291 | 			@Nullable Exception ex) throws Exception {
  292 | 
  293 | 		this.interceptor.afterCompletion(request, response, handler, ex);
  294 | 	}
  295 | 
  296 | 
  297 | 	/**
  298 | 	 * Contains both the parsed {@link PathPattern} and the raw String pattern,
  299 | 	 * and uses the former when the cached path is {@link PathContainer} or the
  300 | 	 * latter otherwise. If the pattern cannot be parsed due to unsupported
  301 | 	 * syntax, then {@link PathMatcher} is used for all requests.
  302 | 	 * @since 5.3.6
  303 | 	 */
  304 | 	private static class PatternAdapter {
  305 | 
  306 | 		private final String patternString;
  307 | 
  308 | 		private final @Nullable PathPattern pathPattern;
  309 | 
  310 | 
  311 | 		public PatternAdapter(String pattern, @Nullable PathPatternParser parser) {
  312 | 			this.patternString = pattern;
  313 | 			this.pathPattern = initPathPattern(pattern, parser);
  314 | 		}
  315 | 
  316 | 		private static @Nullable PathPattern initPathPattern(String pattern, @Nullable PathPatternParser parser) {
  317 | 			try {
  318 | 				return (parser != null ? parser : PathPatternParser.defaultInstance).parse(pattern);
  319 | 			}
  320 | 			catch (PatternParseException ex) {
  321 | 				return null;
  322 | 			}
  323 | 		}
  324 | 
  325 | 		public String getPatternString() {
  326 | 			return this.patternString;
  327 | 		}
  328 | 
  329 | 		public boolean match(Object path, boolean isPathContainer, PathMatcher pathMatcher) {
  330 | 			if (isPathContainer) {
  331 | 				PathContainer pathContainer = (PathContainer) path;
  332 | 				if (this.pathPattern != null) {
  333 | 					return this.pathPattern.matches(pathContainer);
  334 | 				}
  335 | 				String lookupPath = pathContainer.value();
  336 | 				path = UrlPathHelper.defaultInstance.removeSemicolonContent(lookupPath);
  337 | 			}
  338 | 			return pathMatcher.match(this.patternString, (String) path);
  339 | 		}
  340 | 
  341 | 		public static PatternAdapter @Nullable [] initPatterns(
  342 | 				String @Nullable [] patterns, @Nullable PathPatternParser parser) {
  343 | 
  344 | 			if (ObjectUtils.isEmpty(patterns)) {
  345 | 				return null;
  346 | 			}
  347 | 			return Arrays.stream(patterns)
  348 | 					.map(pattern -> new PatternAdapter(pattern, parser))
  349 | 					.toArray(PatternAdapter[]::new);
  350 | 		}
  351 | 	}
  352 | 
  353 | }
```
