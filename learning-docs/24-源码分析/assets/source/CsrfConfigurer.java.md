<!-- learning-docs source archive（机械抓取原文，禁止手改代码内容） -->
<!-- origin: org.springframework.security:spring-security-config:7.2.0 | path: org/springframework/security/config/annotation/web/configurers/CsrfConfigurer.java -->
<!-- 正文引用：[[源码:CsrfConfigurer.java#L起-L止]] ；代码块头部：(src: CsrfConfigurer.java#L起-L止) -->

# CsrfConfigurer.java

| 项 | 值 |
| --- | --- |
| 来源 | `org.springframework.security:spring-security-config:7.2.0` |
| 路径 | `org/springframework/security/config/annotation/web/configurers/CsrfConfigurer.java` |
| 行数 | 420 |
| 在线地址 | https://repo1.maven.org/maven2/org/springframework/security/spring-security-config/7.2.0/spring-security-config-7.2.0-sources.jar |
| 抓取时间 | 2026-11-25T16:40:14.244Z |

```java
    1 | /*
    2 |  * Copyright 2004-present the original author or authors.
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
   17 | package org.springframework.security.config.annotation.web.configurers;
   18 | 
   19 | import java.util.ArrayList;
   20 | import java.util.LinkedHashMap;
   21 | import java.util.List;
   22 | import java.util.function.Supplier;
   23 | 
   24 | import io.micrometer.observation.ObservationRegistry;
   25 | import jakarta.servlet.http.HttpServletRequest;
   26 | import jakarta.servlet.http.HttpServletResponse;
   27 | 
   28 | import org.springframework.context.ApplicationContext;
   29 | import org.springframework.security.access.AccessDeniedException;
   30 | import org.springframework.security.config.Customizer;
   31 | import org.springframework.security.config.annotation.web.AbstractRequestMatcherRegistry;
   32 | import org.springframework.security.config.annotation.web.HttpSecurityBuilder;
   33 | import org.springframework.security.config.annotation.web.builders.HttpSecurity;
   34 | import org.springframework.security.web.access.AccessDeniedHandler;
   35 | import org.springframework.security.web.access.AccessDeniedHandlerImpl;
   36 | import org.springframework.security.web.access.CompositeAccessDeniedHandler;
   37 | import org.springframework.security.web.access.DelegatingAccessDeniedHandler;
   38 | import org.springframework.security.web.access.ObservationMarkingAccessDeniedHandler;
   39 | import org.springframework.security.web.authentication.session.SessionAuthenticationStrategy;
   40 | import org.springframework.security.web.csrf.CookieCsrfTokenRepository;
   41 | import org.springframework.security.web.csrf.CsrfAuthenticationStrategy;
   42 | import org.springframework.security.web.csrf.CsrfFilter;
   43 | import org.springframework.security.web.csrf.CsrfLogoutHandler;
   44 | import org.springframework.security.web.csrf.CsrfToken;
   45 | import org.springframework.security.web.csrf.CsrfTokenRepository;
   46 | import org.springframework.security.web.csrf.CsrfTokenRequestAttributeHandler;
   47 | import org.springframework.security.web.csrf.CsrfTokenRequestHandler;
   48 | import org.springframework.security.web.csrf.HttpSessionCsrfTokenRepository;
   49 | import org.springframework.security.web.csrf.MissingCsrfTokenException;
   50 | import org.springframework.security.web.csrf.XorCsrfTokenRequestAttributeHandler;
   51 | import org.springframework.security.web.session.InvalidSessionAccessDeniedHandler;
   52 | import org.springframework.security.web.session.InvalidSessionStrategy;
   53 | import org.springframework.security.web.util.matcher.AndRequestMatcher;
   54 | import org.springframework.security.web.util.matcher.NegatedRequestMatcher;
   55 | import org.springframework.security.web.util.matcher.OrRequestMatcher;
   56 | import org.springframework.security.web.util.matcher.RequestMatcher;
   57 | import org.springframework.util.Assert;
   58 | import org.springframework.util.StringUtils;
   59 | 
   60 | /**
   61 |  * Adds
   62 |  * <a href="https://www.owasp.org/index.php/Cross-Site_Request_Forgery_(CSRF)" >CSRF</a>
   63 |  * protection for the methods as specified by
   64 |  * {@link #requireCsrfProtectionMatcher(RequestMatcher)}.
   65 |  *
   66 |  * <h2>Security Filters</h2>
   67 |  *
   68 |  * The following Filters are populated
   69 |  *
   70 |  * <ul>
   71 |  * <li>{@link CsrfFilter}</li>
   72 |  * </ul>
   73 |  *
   74 |  * <h2>Shared Objects Created</h2>
   75 |  *
   76 |  * No shared objects are created.
   77 |  *
   78 |  * <h2>Shared Objects Used</h2>
   79 |  *
   80 |  * <ul>
   81 |  * <li>{@link ExceptionHandlingConfigurer#accessDeniedHandler(AccessDeniedHandler)} is
   82 |  * used to determine how to handle CSRF attempts</li>
   83 |  * <li>{@link InvalidSessionStrategy}</li>
   84 |  * </ul>
   85 |  *
   86 |  * @author Rob Winch
   87 |  * @author Michael Vitz
   88 |  * @since 3.2
   89 |  */
   90 | public final class CsrfConfigurer<H extends HttpSecurityBuilder<H>>
   91 | 		extends AbstractHttpConfigurer<CsrfConfigurer<H>, H> {
   92 | 
   93 | 	private CsrfTokenRepository csrfTokenRepository = new HttpSessionCsrfTokenRepository();
   94 | 
   95 | 	private RequestMatcher requireCsrfProtectionMatcher = CsrfFilter.DEFAULT_CSRF_MATCHER;
   96 | 
   97 | 	private List<RequestMatcher> ignoredCsrfProtectionMatchers = new ArrayList<>();
   98 | 
   99 | 	private SessionAuthenticationStrategy sessionAuthenticationStrategy;
  100 | 
  101 | 	private CsrfTokenRequestHandler requestHandler;
  102 | 
  103 | 	private final ApplicationContext context;
  104 | 
  105 | 	/**
  106 | 	 * Creates a new instance.
  107 | 	 * @see HttpSecurity#csrf(Customizer)
  108 | 	 */
  109 | 	public CsrfConfigurer(ApplicationContext context) {
  110 | 		this.context = context;
  111 | 	}
  112 | 
  113 | 	/**
  114 | 	 * Specify the {@link CsrfTokenRepository} to use. The default is an
  115 | 	 * {@link HttpSessionCsrfTokenRepository}.
  116 | 	 * @param csrfTokenRepository the {@link CsrfTokenRepository} to use
  117 | 	 * @return the {@link CsrfConfigurer} for further customizations
  118 | 	 */
  119 | 	public CsrfConfigurer<H> csrfTokenRepository(CsrfTokenRepository csrfTokenRepository) {
  120 | 		Assert.notNull(csrfTokenRepository, "csrfTokenRepository cannot be null");
  121 | 		this.csrfTokenRepository = csrfTokenRepository;
  122 | 		return this;
  123 | 	}
  124 | 
  125 | 	/**
  126 | 	 * Specify the {@link RequestMatcher} to use for determining when CSRF should be
  127 | 	 * applied. The default is to ignore GET, HEAD, TRACE, OPTIONS and process all other
  128 | 	 * requests.
  129 | 	 * @param requireCsrfProtectionMatcher the {@link RequestMatcher} to use
  130 | 	 * @return the {@link CsrfConfigurer} for further customizations
  131 | 	 */
  132 | 	public CsrfConfigurer<H> requireCsrfProtectionMatcher(RequestMatcher requireCsrfProtectionMatcher) {
  133 | 		Assert.notNull(requireCsrfProtectionMatcher, "requireCsrfProtectionMatcher cannot be null");
  134 | 		this.requireCsrfProtectionMatcher = requireCsrfProtectionMatcher;
  135 | 		return this;
  136 | 	}
  137 | 
  138 | 	/**
  139 | 	 * Specify a {@link CsrfTokenRequestHandler} to use for making the {@code CsrfToken}
  140 | 	 * available as a request attribute.
  141 | 	 * @param requestHandler the {@link CsrfTokenRequestHandler} to use
  142 | 	 * @return the {@link CsrfConfigurer} for further customizations
  143 | 	 * @since 5.8
  144 | 	 */
  145 | 	public CsrfConfigurer<H> csrfTokenRequestHandler(CsrfTokenRequestHandler requestHandler) {
  146 | 		this.requestHandler = requestHandler;
  147 | 		return this;
  148 | 	}
  149 | 
  150 | 	/**
  151 | 	 * <p>
  152 | 	 * Allows specifying {@link HttpServletRequest}s that should not use CSRF Protection
  153 | 	 * even if they match the {@link #requireCsrfProtectionMatcher(RequestMatcher)}.
  154 | 	 * </p>
  155 | 	 *
  156 | 	 * <p>
  157 | 	 * For example, the following configuration will ensure CSRF protection ignores:
  158 | 	 * </p>
  159 | 	 * <ul>
  160 | 	 * <li>Any GET, HEAD, TRACE, OPTIONS (this is the default)</li>
  161 | 	 * <li>We also explicitly state to ignore any request that has a "X-Requested-With:
  162 | 	 * XMLHttpRequest" header</li>
  163 | 	 * </ul>
  164 | 	 *
  165 | 	 * <pre>
  166 | 	 * http
  167 | 	 *     .csrf((csrf) -&gt; csrf
  168 | 	 *         .ignoringRequestMatchers((request) -&gt; "XMLHttpRequest".equals(request.getHeader("X-Requested-With"))))
  169 | 	 *     ...
  170 | 	 * </pre>
  171 | 	 *
  172 | 	 * @since 5.1
  173 | 	 */
  174 | 	public CsrfConfigurer<H> ignoringRequestMatchers(RequestMatcher... requestMatchers) {
  175 | 		new IgnoreCsrfProtectionRegistry(this.context).requestMatchers(requestMatchers);
  176 | 		return this;
  177 | 	}
  178 | 
  179 | 	/**
  180 | 	 * <p>
  181 | 	 * Allows specifying {@link HttpServletRequest} that should not use CSRF Protection
  182 | 	 * even if they match the {@link #requireCsrfProtectionMatcher(RequestMatcher)}.
  183 | 	 * </p>
  184 | 	 *
  185 | 	 * <p>
  186 | 	 * For example, the following configuration will ensure CSRF protection ignores:
  187 | 	 * </p>
  188 | 	 * <ul>
  189 | 	 * <li>Any GET, HEAD, TRACE, OPTIONS (this is the default)</li>
  190 | 	 * <li>We also explicitly state to ignore any request that starts with "/sockjs/"</li>
  191 | 	 * </ul>
  192 | 	 *
  193 | 	 * <pre>
  194 | 	 * http
  195 | 	 *     .csrf((csrf) -&gt; csrf
  196 | 	 *         .ignoringRequestMatchers("/sockjs/**"))
  197 | 	 *     ...
  198 | 	 * </pre>
  199 | 	 *
  200 | 	 * @since 5.8
  201 | 	 * @see AbstractRequestMatcherRegistry#requestMatchers(String...)
  202 | 	 */
  203 | 	public CsrfConfigurer<H> ignoringRequestMatchers(String... patterns) {
  204 | 		new IgnoreCsrfProtectionRegistry(this.context).requestMatchers(patterns);
  205 | 		return this;
  206 | 	}
  207 | 
  208 | 	/**
  209 | 	 * <p>
  210 | 	 * Specify the {@link SessionAuthenticationStrategy} to use. The default is a
  211 | 	 * {@link CsrfAuthenticationStrategy}.
  212 | 	 * </p>
  213 | 	 * @param sessionAuthenticationStrategy the {@link SessionAuthenticationStrategy} to
  214 | 	 * use
  215 | 	 * @return the {@link CsrfConfigurer} for further customizations
  216 | 	 * @since 5.2
  217 | 	 */
  218 | 	public CsrfConfigurer<H> sessionAuthenticationStrategy(
  219 | 			SessionAuthenticationStrategy sessionAuthenticationStrategy) {
  220 | 		Assert.notNull(sessionAuthenticationStrategy, "sessionAuthenticationStrategy cannot be null");
  221 | 		this.sessionAuthenticationStrategy = sessionAuthenticationStrategy;
  222 | 		return this;
  223 | 	}
  224 | 
  225 | 	/**
  226 | 	 * <p>
  227 | 	 * Sensible CSRF defaults when used in combination with a single page application.
  228 | 	 * Creates a cookie-based token repository and a custom request handler to resolve the
  229 | 	 * actual token value instead of the encoded token.
  230 | 	 * </p>
  231 | 	 * @return the {@link CsrfConfigurer} for further customizations
  232 | 	 * @since 7.0
  233 | 	 */
  234 | 	public CsrfConfigurer<H> spa() {
  235 | 		this.csrfTokenRepository = CookieCsrfTokenRepository.withHttpOnlyFalse();
  236 | 		this.requestHandler = new SpaCsrfTokenRequestHandler();
  237 | 		return this;
  238 | 	}
  239 | 
  240 | 	@SuppressWarnings("unchecked")
  241 | 	@Override
  242 | 	public void configure(H http) {
  243 | 		CsrfFilter filter = new CsrfFilter(this.csrfTokenRepository);
  244 | 		RequestMatcher requireCsrfProtectionMatcher = getRequireCsrfProtectionMatcher();
  245 | 		if (requireCsrfProtectionMatcher != null) {
  246 | 			filter.setRequireCsrfProtectionMatcher(requireCsrfProtectionMatcher);
  247 | 		}
  248 | 		AccessDeniedHandler accessDeniedHandler = createAccessDeniedHandler(http);
  249 | 		ObservationRegistry registry = getObservationRegistry();
  250 | 		if (!registry.isNoop()) {
  251 | 			ObservationMarkingAccessDeniedHandler observable = new ObservationMarkingAccessDeniedHandler(registry);
  252 | 			accessDeniedHandler = new CompositeAccessDeniedHandler(observable, accessDeniedHandler);
  253 | 		}
  254 | 		if (accessDeniedHandler != null) {
  255 | 			filter.setAccessDeniedHandler(accessDeniedHandler);
  256 | 		}
  257 | 		LogoutConfigurer<H> logoutConfigurer = http.getConfigurer(LogoutConfigurer.class);
  258 | 		if (logoutConfigurer != null) {
  259 | 			logoutConfigurer.addLogoutHandler(new CsrfLogoutHandler(this.csrfTokenRepository));
  260 | 		}
  261 | 		SessionManagementConfigurer<H> sessionConfigurer = http.getConfigurer(SessionManagementConfigurer.class);
  262 | 		if (sessionConfigurer != null) {
  263 | 			sessionConfigurer.addSessionAuthenticationStrategy(getSessionAuthenticationStrategy());
  264 | 		}
  265 | 		if (this.requestHandler != null) {
  266 | 			filter.setRequestHandler(this.requestHandler);
  267 | 		}
  268 | 		filter = postProcess(filter);
  269 | 		http.addFilter(filter);
  270 | 	}
  271 | 
  272 | 	/**
  273 | 	 * Gets the final {@link RequestMatcher} to use by combining the
  274 | 	 * {@link #requireCsrfProtectionMatcher(RequestMatcher)} and any {@link #ignore()}.
  275 | 	 * @return the {@link RequestMatcher} to use
  276 | 	 */
  277 | 	private RequestMatcher getRequireCsrfProtectionMatcher() {
  278 | 		if (this.ignoredCsrfProtectionMatchers.isEmpty()) {
  279 | 			return this.requireCsrfProtectionMatcher;
  280 | 		}
  281 | 		return new AndRequestMatcher(this.requireCsrfProtectionMatcher,
  282 | 				new NegatedRequestMatcher(new OrRequestMatcher(this.ignoredCsrfProtectionMatchers)));
  283 | 	}
  284 | 
  285 | 	/**
  286 | 	 * Gets the default {@link AccessDeniedHandler} from the
  287 | 	 * {@link ExceptionHandlingConfigurer#getAccessDeniedHandler(HttpSecurityBuilder)} or
  288 | 	 * create a {@link AccessDeniedHandlerImpl} if not available.
  289 | 	 * @param http the {@link HttpSecurityBuilder}
  290 | 	 * @return the {@link AccessDeniedHandler}
  291 | 	 */
  292 | 	@SuppressWarnings("unchecked")
  293 | 	private AccessDeniedHandler getDefaultAccessDeniedHandler(H http) {
  294 | 		ExceptionHandlingConfigurer<H> exceptionConfig = http.getConfigurer(ExceptionHandlingConfigurer.class);
  295 | 		AccessDeniedHandler handler = null;
  296 | 		if (exceptionConfig != null) {
  297 | 			handler = exceptionConfig.getAccessDeniedHandler(http);
  298 | 		}
  299 | 		if (handler == null) {
  300 | 			handler = new AccessDeniedHandlerImpl();
  301 | 		}
  302 | 		return handler;
  303 | 	}
  304 | 
  305 | 	/**
  306 | 	 * Gets the default {@link InvalidSessionStrategy} from the
  307 | 	 * {@link SessionManagementConfigurer#getInvalidSessionStrategy()} or null if not
  308 | 	 * available.
  309 | 	 * @param http the {@link HttpSecurityBuilder}
  310 | 	 * @return the {@link InvalidSessionStrategy}
  311 | 	 */
  312 | 	@SuppressWarnings("unchecked")
  313 | 	private InvalidSessionStrategy getInvalidSessionStrategy(H http) {
  314 | 		SessionManagementConfigurer<H> sessionManagement = http.getConfigurer(SessionManagementConfigurer.class);
  315 | 		if (sessionManagement == null) {
  316 | 			return null;
  317 | 		}
  318 | 		return sessionManagement.getInvalidSessionStrategy();
  319 | 	}
  320 | 
  321 | 	/**
  322 | 	 * Creates the {@link AccessDeniedHandler} from the result of
  323 | 	 * {@link #getDefaultAccessDeniedHandler(HttpSecurityBuilder)} and
  324 | 	 * {@link #getInvalidSessionStrategy(HttpSecurityBuilder)}. If
  325 | 	 * {@link #getInvalidSessionStrategy(HttpSecurityBuilder)} is non-null, then a
  326 | 	 * {@link DelegatingAccessDeniedHandler} is used in combination with
  327 | 	 * {@link InvalidSessionAccessDeniedHandler} and the
  328 | 	 * {@link #getDefaultAccessDeniedHandler(HttpSecurityBuilder)}. Otherwise, only
  329 | 	 * {@link #getDefaultAccessDeniedHandler(HttpSecurityBuilder)} is used.
  330 | 	 * @param http the {@link HttpSecurityBuilder}
  331 | 	 * @return the {@link AccessDeniedHandler}
  332 | 	 */
  333 | 	private AccessDeniedHandler createAccessDeniedHandler(H http) {
  334 | 		InvalidSessionStrategy invalidSessionStrategy = getInvalidSessionStrategy(http);
  335 | 		AccessDeniedHandler defaultAccessDeniedHandler = getDefaultAccessDeniedHandler(http);
  336 | 		if (invalidSessionStrategy == null) {
  337 | 			return defaultAccessDeniedHandler;
  338 | 		}
  339 | 		InvalidSessionAccessDeniedHandler invalidSessionDeniedHandler = new InvalidSessionAccessDeniedHandler(
  340 | 				invalidSessionStrategy);
  341 | 		LinkedHashMap<Class<? extends AccessDeniedException>, AccessDeniedHandler> handlers = new LinkedHashMap<>();
  342 | 		handlers.put(MissingCsrfTokenException.class, invalidSessionDeniedHandler);
  343 | 		return new DelegatingAccessDeniedHandler(handlers, defaultAccessDeniedHandler);
  344 | 	}
  345 | 
  346 | 	/**
  347 | 	 * Gets the {@link SessionAuthenticationStrategy} to use. If none was set by the user
  348 | 	 * a {@link CsrfAuthenticationStrategy} is created.
  349 | 	 * @return the {@link SessionAuthenticationStrategy}
  350 | 	 * @since 5.2
  351 | 	 */
  352 | 	private SessionAuthenticationStrategy getSessionAuthenticationStrategy() {
  353 | 		if (this.sessionAuthenticationStrategy != null) {
  354 | 			return this.sessionAuthenticationStrategy;
  355 | 		}
  356 | 		CsrfAuthenticationStrategy csrfAuthenticationStrategy = new CsrfAuthenticationStrategy(
  357 | 				this.csrfTokenRepository);
  358 | 		if (this.requestHandler != null) {
  359 | 			csrfAuthenticationStrategy.setRequestHandler(this.requestHandler);
  360 | 		}
  361 | 		return csrfAuthenticationStrategy;
  362 | 	}
  363 | 
  364 | 	private ObservationRegistry getObservationRegistry() {
  365 | 		ApplicationContext context = getBuilder().getSharedObject(ApplicationContext.class);
  366 | 		String[] names = context.getBeanNamesForType(ObservationRegistry.class);
  367 | 		if (names.length == 1) {
  368 | 			return context.getBean(ObservationRegistry.class);
  369 | 		}
  370 | 		else {
  371 | 			return ObservationRegistry.NOOP;
  372 | 		}
  373 | 	}
  374 | 
  375 | 	/**
  376 | 	 * Allows registering {@link RequestMatcher} instances that should be ignored (even if
  377 | 	 * the {@link HttpServletRequest} matches the
  378 | 	 * {@link CsrfConfigurer#requireCsrfProtectionMatcher(RequestMatcher)}.
  379 | 	 *
  380 | 	 * @author Rob Winch
  381 | 	 * @since 4.0
  382 | 	 */
  383 | 	private class IgnoreCsrfProtectionRegistry extends AbstractRequestMatcherRegistry<IgnoreCsrfProtectionRegistry> {
  384 | 
  385 | 		IgnoreCsrfProtectionRegistry(ApplicationContext context) {
  386 | 			setApplicationContext(context);
  387 | 		}
  388 | 
  389 | 		@Override
  390 | 		protected IgnoreCsrfProtectionRegistry chainRequestMatchers(List<RequestMatcher> requestMatchers) {
  391 | 			CsrfConfigurer.this.ignoredCsrfProtectionMatchers.addAll(requestMatchers);
  392 | 			return this;
  393 | 		}
  394 | 
  395 | 	}
  396 | 
  397 | 	private static final class SpaCsrfTokenRequestHandler implements CsrfTokenRequestHandler {
  398 | 
  399 | 		private final CsrfTokenRequestAttributeHandler plain = new CsrfTokenRequestAttributeHandler();
  400 | 
  401 | 		private final CsrfTokenRequestAttributeHandler xor = new XorCsrfTokenRequestAttributeHandler();
  402 | 
  403 | 		SpaCsrfTokenRequestHandler() {
  404 | 			this.xor.setCsrfRequestAttributeName(null);
  405 | 		}
  406 | 
  407 | 		@Override
  408 | 		public void handle(HttpServletRequest request, HttpServletResponse response, Supplier<CsrfToken> csrfToken) {
  409 | 			this.xor.handle(request, response, csrfToken);
  410 | 		}
  411 | 
  412 | 		@Override
  413 | 		public String resolveCsrfTokenValue(HttpServletRequest request, CsrfToken csrfToken) {
  414 | 			String headerValue = request.getHeader(csrfToken.getHeaderName());
  415 | 			return (StringUtils.hasText(headerValue) ? this.plain : this.xor).resolveCsrfTokenValue(request, csrfToken);
  416 | 		}
  417 | 
  418 | 	}
  419 | 
  420 | }
```
