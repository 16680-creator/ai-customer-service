<!-- learning-docs source archive（机械抓取原文，禁止手改代码内容） -->
<!-- origin: org.springframework.security:spring-security-config:7.2.0 | path: org/springframework/security/config/annotation/web/configurers/SessionManagementConfigurer.java -->
<!-- 正文引用：[[源码:SessionManagementConfigurer.java#L起-L止]] ；代码块头部：(src: SessionManagementConfigurer.java#L起-L止) -->

# SessionManagementConfigurer.java

| 项 | 值 |
| --- | --- |
| 来源 | `org.springframework.security:spring-security-config:7.2.0` |
| 路径 | `org/springframework/security/config/annotation/web/configurers/SessionManagementConfigurer.java` |
| 行数 | 780 |
| 在线地址 | https://repo1.maven.org/maven2/org/springframework/security/spring-security-config/7.2.0/spring-security-config-7.2.0-sources.jar |
| 抓取时间 | 2026-11-25T16:55:53.217Z |

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
   20 | import java.util.Arrays;
   21 | import java.util.HashSet;
   22 | import java.util.List;
   23 | import java.util.Set;
   24 | 
   25 | import jakarta.servlet.http.HttpServletResponse;
   26 | import jakarta.servlet.http.HttpSession;
   27 | 
   28 | import org.springframework.context.ApplicationContext;
   29 | import org.springframework.context.ApplicationListener;
   30 | import org.springframework.context.event.GenericApplicationListenerAdapter;
   31 | import org.springframework.context.event.SmartApplicationListener;
   32 | import org.springframework.security.authentication.AuthenticationTrustResolver;
   33 | import org.springframework.security.config.Customizer;
   34 | import org.springframework.security.config.annotation.web.HttpSecurityBuilder;
   35 | import org.springframework.security.config.annotation.web.builders.HttpSecurity;
   36 | import org.springframework.security.config.http.SessionCreationPolicy;
   37 | import org.springframework.security.context.DelegatingApplicationListener;
   38 | import org.springframework.security.core.session.SessionRegistry;
   39 | import org.springframework.security.core.session.SessionRegistryImpl;
   40 | import org.springframework.security.web.authentication.AuthenticationFailureHandler;
   41 | import org.springframework.security.web.authentication.SimpleUrlAuthenticationFailureHandler;
   42 | import org.springframework.security.web.authentication.logout.LogoutHandler;
   43 | import org.springframework.security.web.authentication.session.ChangeSessionIdAuthenticationStrategy;
   44 | import org.springframework.security.web.authentication.session.CompositeSessionAuthenticationStrategy;
   45 | import org.springframework.security.web.authentication.session.ConcurrentSessionControlAuthenticationStrategy;
   46 | import org.springframework.security.web.authentication.session.NullAuthenticatedSessionStrategy;
   47 | import org.springframework.security.web.authentication.session.RegisterSessionAuthenticationStrategy;
   48 | import org.springframework.security.web.authentication.session.SessionAuthenticationStrategy;
   49 | import org.springframework.security.web.authentication.session.SessionFixationProtectionStrategy;
   50 | import org.springframework.security.web.authentication.session.SessionLimit;
   51 | import org.springframework.security.web.context.DelegatingSecurityContextRepository;
   52 | import org.springframework.security.web.context.HttpSessionSecurityContextRepository;
   53 | import org.springframework.security.web.context.NullSecurityContextRepository;
   54 | import org.springframework.security.web.context.RequestAttributeSecurityContextRepository;
   55 | import org.springframework.security.web.context.SecurityContextRepository;
   56 | import org.springframework.security.web.savedrequest.NullRequestCache;
   57 | import org.springframework.security.web.savedrequest.RequestCache;
   58 | import org.springframework.security.web.session.ConcurrentSessionFilter;
   59 | import org.springframework.security.web.session.DisableEncodeUrlFilter;
   60 | import org.springframework.security.web.session.ForceEagerSessionCreationFilter;
   61 | import org.springframework.security.web.session.InvalidSessionStrategy;
   62 | import org.springframework.security.web.session.SessionInformationExpiredStrategy;
   63 | import org.springframework.security.web.session.SessionManagementFilter;
   64 | import org.springframework.security.web.session.SimpleRedirectInvalidSessionStrategy;
   65 | import org.springframework.security.web.session.SimpleRedirectSessionInformationExpiredStrategy;
   66 | import org.springframework.util.Assert;
   67 | import org.springframework.util.CollectionUtils;
   68 | 
   69 | /**
   70 |  * Allows configuring session management.
   71 |  *
   72 |  * <h2>Security Filters</h2>
   73 |  *
   74 |  * The following Filters are populated
   75 |  *
   76 |  * <ul>
   77 |  * <li>{@link SessionManagementFilter}</li>
   78 |  * <li>{@link ConcurrentSessionFilter} if there are restrictions on how many concurrent
   79 |  * sessions a user can have</li>
   80 |  * </ul>
   81 |  *
   82 |  * <h2>Shared Objects Created</h2>
   83 |  *
   84 |  * The following shared objects are created:
   85 |  *
   86 |  * <ul>
   87 |  * <li>{@link RequestCache}</li>
   88 |  * <li>{@link SecurityContextRepository}</li>
   89 |  * <li>{@link SessionManagementConfigurer}</li>
   90 |  * <li>{@link InvalidSessionStrategy}</li>
   91 |  * </ul>
   92 |  *
   93 |  * <h2>Shared Objects Used</h2>
   94 |  *
   95 |  * <ul>
   96 |  * <li>{@link SecurityContextRepository}</li>
   97 |  * <li>{@link AuthenticationTrustResolver} is optionally used to populate the
   98 |  * {@link HttpSessionSecurityContextRepository} and {@link SessionManagementFilter}</li>
   99 |  * </ul>
  100 |  *
  101 |  * @author Rob Winch
  102 |  * @author Onur Kagan Ozcan
  103 |  * @author Ngoc Nhan
  104 |  * @since 3.2
  105 |  * @see SessionManagementFilter
  106 |  * @see ConcurrentSessionFilter
  107 |  */
  108 | public final class SessionManagementConfigurer<H extends HttpSecurityBuilder<H>>
  109 | 		extends AbstractHttpConfigurer<SessionManagementConfigurer<H>, H> {
  110 | 
  111 | 	private final SessionAuthenticationStrategy DEFAULT_SESSION_FIXATION_STRATEGY = createDefaultSessionFixationProtectionStrategy();
  112 | 
  113 | 	private SessionAuthenticationStrategy sessionFixationAuthenticationStrategy = this.DEFAULT_SESSION_FIXATION_STRATEGY;
  114 | 
  115 | 	private SessionAuthenticationStrategy sessionAuthenticationStrategy;
  116 | 
  117 | 	private SessionAuthenticationStrategy providedSessionAuthenticationStrategy;
  118 | 
  119 | 	private InvalidSessionStrategy invalidSessionStrategy;
  120 | 
  121 | 	private SessionInformationExpiredStrategy expiredSessionStrategy;
  122 | 
  123 | 	private List<SessionAuthenticationStrategy> sessionAuthenticationStrategies = new ArrayList<>();
  124 | 
  125 | 	private SessionRegistry sessionRegistry;
  126 | 
  127 | 	private SessionLimit sessionLimit;
  128 | 
  129 | 	private String expiredUrl;
  130 | 
  131 | 	private boolean maxSessionsPreventsLogin;
  132 | 
  133 | 	private SessionCreationPolicy sessionPolicy;
  134 | 
  135 | 	private boolean enableSessionUrlRewriting;
  136 | 
  137 | 	private String invalidSessionUrl;
  138 | 
  139 | 	private String sessionAuthenticationErrorUrl;
  140 | 
  141 | 	private AuthenticationFailureHandler sessionAuthenticationFailureHandler;
  142 | 
  143 | 	private Set<String> propertiesThatRequireImplicitAuthentication = new HashSet<>();
  144 | 
  145 | 	private Boolean requireExplicitAuthenticationStrategy;
  146 | 
  147 | 	/**
  148 | 	 * This should not use RequestAttributeSecurityContextRepository since that is
  149 | 	 * stateless and session management is about state management.
  150 | 	 */
  151 | 	private SecurityContextRepository sessionManagementSecurityContextRepository = new HttpSessionSecurityContextRepository();
  152 | 
  153 | 	/**
  154 | 	 * Creates a new instance.
  155 | 	 * @see HttpSecurity#sessionManagement(Customizer)
  156 | 	 */
  157 | 	public SessionManagementConfigurer() {
  158 | 	}
  159 | 
  160 | 	/**
  161 | 	 * Setting this attribute will inject the {@link SessionManagementFilter} with a
  162 | 	 * {@link SimpleRedirectInvalidSessionStrategy} configured with the attribute value.
  163 | 	 * When an invalid session ID is submitted, the strategy will be invoked, redirecting
  164 | 	 * to the configured URL.
  165 | 	 * @param invalidSessionUrl the URL to redirect to when an invalid session is detected
  166 | 	 * @return the {@link SessionManagementConfigurer} for further customization
  167 | 	 */
  168 | 	public SessionManagementConfigurer<H> invalidSessionUrl(String invalidSessionUrl) {
  169 | 		this.invalidSessionUrl = invalidSessionUrl;
  170 | 		this.propertiesThatRequireImplicitAuthentication.add("invalidSessionUrl = " + invalidSessionUrl);
  171 | 		return this;
  172 | 	}
  173 | 
  174 | 	/**
  175 | 	 * Setting this means that explicit invocation of
  176 | 	 * {@link SessionAuthenticationStrategy} is required.
  177 | 	 * @param requireExplicitAuthenticationStrategy require explicit invocation of
  178 | 	 * {@link SessionAuthenticationStrategy}
  179 | 	 * @return the {@link SessionManagementConfigurer} for further customization
  180 | 	 */
  181 | 	public SessionManagementConfigurer<H> requireExplicitAuthenticationStrategy(
  182 | 			boolean requireExplicitAuthenticationStrategy) {
  183 | 		this.requireExplicitAuthenticationStrategy = requireExplicitAuthenticationStrategy;
  184 | 		return this;
  185 | 	}
  186 | 
  187 | 	/**
  188 | 	 * Setting this attribute will inject the provided invalidSessionStrategy into the
  189 | 	 * {@link SessionManagementFilter}. When an invalid session ID is submitted, the
  190 | 	 * strategy will be invoked, redirecting to the configured URL.
  191 | 	 * @param invalidSessionStrategy the strategy to use when an invalid session ID is
  192 | 	 * submitted.
  193 | 	 * @return the {@link SessionManagementConfigurer} for further customization
  194 | 	 */
  195 | 	public SessionManagementConfigurer<H> invalidSessionStrategy(InvalidSessionStrategy invalidSessionStrategy) {
  196 | 		Assert.notNull(invalidSessionStrategy, "invalidSessionStrategy");
  197 | 		this.invalidSessionStrategy = invalidSessionStrategy;
  198 | 		this.propertiesThatRequireImplicitAuthentication.add("invalidSessionStrategy = " + invalidSessionStrategy);
  199 | 		return this;
  200 | 	}
  201 | 
  202 | 	/**
  203 | 	 * Defines the URL of the error page which should be shown when the
  204 | 	 * SessionAuthenticationStrategy raises an exception. If not set, an unauthorized
  205 | 	 * (402) error code will be returned to the client. Note that this attribute doesn't
  206 | 	 * apply if the error occurs during a form-based login, where the URL for
  207 | 	 * authentication failure will take precedence.
  208 | 	 * @param sessionAuthenticationErrorUrl the URL to redirect to
  209 | 	 * @return the {@link SessionManagementConfigurer} for further customization
  210 | 	 */
  211 | 	public SessionManagementConfigurer<H> sessionAuthenticationErrorUrl(String sessionAuthenticationErrorUrl) {
  212 | 		this.sessionAuthenticationErrorUrl = sessionAuthenticationErrorUrl;
  213 | 		this.propertiesThatRequireImplicitAuthentication
  214 | 			.add("sessionAuthenticationErrorUrl = " + sessionAuthenticationErrorUrl);
  215 | 		return this;
  216 | 	}
  217 | 
  218 | 	/**
  219 | 	 * Defines the {@code AuthenticationFailureHandler} which will be used when the
  220 | 	 * SessionAuthenticationStrategy raises an exception. If not set, an unauthorized
  221 | 	 * (402) error code will be returned to the client. Note that this attribute doesn't
  222 | 	 * apply if the error occurs during a form-based login, where the URL for
  223 | 	 * authentication failure will take precedence.
  224 | 	 * @param sessionAuthenticationFailureHandler the handler to use
  225 | 	 * @return the {@link SessionManagementConfigurer} for further customization
  226 | 	 */
  227 | 	public SessionManagementConfigurer<H> sessionAuthenticationFailureHandler(
  228 | 			AuthenticationFailureHandler sessionAuthenticationFailureHandler) {
  229 | 		this.sessionAuthenticationFailureHandler = sessionAuthenticationFailureHandler;
  230 | 		this.propertiesThatRequireImplicitAuthentication
  231 | 			.add("sessionAuthenticationFailureHandler = " + sessionAuthenticationFailureHandler);
  232 | 		return this;
  233 | 	}
  234 | 
  235 | 	/**
  236 | 	 * If set to true, allows HTTP sessions to be rewritten in the URLs when using
  237 | 	 * {@link HttpServletResponse#encodeRedirectURL(String)} or
  238 | 	 * {@link HttpServletResponse#encodeURL(String)}, otherwise disallows HTTP sessions to
  239 | 	 * be included in the URL. This prevents leaking information to external domains.
  240 | 	 * <p>
  241 | 	 * This is achieved by guarding {@link HttpServletResponse#encodeURL} and
  242 | 	 * {@link HttpServletResponse#encodeRedirectURL} invocations. Any code that also
  243 | 	 * overrides either of these two methods, like
  244 | 	 * {@link org.springframework.web.servlet.resource.ResourceUrlEncodingFilter}, needs
  245 | 	 * to come after the security filter chain or risk being skipped.
  246 | 	 * @param enableSessionUrlRewriting true if should allow the JSESSIONID to be
  247 | 	 * rewritten into the URLs, else false (default)
  248 | 	 * @return the {@link SessionManagementConfigurer} for further customization
  249 | 	 * @see HttpSessionSecurityContextRepository#setDisableUrlRewriting(boolean)
  250 | 	 */
  251 | 	public SessionManagementConfigurer<H> enableSessionUrlRewriting(boolean enableSessionUrlRewriting) {
  252 | 		this.enableSessionUrlRewriting = enableSessionUrlRewriting;
  253 | 		return this;
  254 | 	}
  255 | 
  256 | 	/**
  257 | 	 * Allows specifying the {@link SessionCreationPolicy}.
  258 | 	 * @param sessionCreationPolicy the {@link SessionCreationPolicy} to use. Cannot be
  259 | 	 * null.
  260 | 	 * @return the {@link SessionManagementConfigurer} for further customizations
  261 | 	 * @throws IllegalArgumentException if {@link SessionCreationPolicy} is null.
  262 | 	 * @see SessionCreationPolicy
  263 | 	 */
  264 | 	public SessionManagementConfigurer<H> sessionCreationPolicy(SessionCreationPolicy sessionCreationPolicy) {
  265 | 		Assert.notNull(sessionCreationPolicy, "sessionCreationPolicy cannot be null");
  266 | 		this.sessionPolicy = sessionCreationPolicy;
  267 | 		this.propertiesThatRequireImplicitAuthentication.add("sessionCreationPolicy = " + sessionCreationPolicy);
  268 | 		return this;
  269 | 	}
  270 | 
  271 | 	/**
  272 | 	 * Allows explicitly specifying the {@link SessionAuthenticationStrategy}. The default
  273 | 	 * is to use {@link ChangeSessionIdAuthenticationStrategy}. If restricting the maximum
  274 | 	 * number of sessions is configured, then
  275 | 	 * {@link CompositeSessionAuthenticationStrategy} delegating to
  276 | 	 * {@link ConcurrentSessionControlAuthenticationStrategy}, the default OR supplied
  277 | 	 * {@code SessionAuthenticationStrategy} and
  278 | 	 * {@link RegisterSessionAuthenticationStrategy}.
  279 | 	 *
  280 | 	 * <p>
  281 | 	 * NOTE: Supplying a custom {@link SessionAuthenticationStrategy} will override the
  282 | 	 * default session fixation strategy.
  283 | 	 * @param sessionAuthenticationStrategy
  284 | 	 * @return the {@link SessionManagementConfigurer} for further customizations
  285 | 	 */
  286 | 	public SessionManagementConfigurer<H> sessionAuthenticationStrategy(
  287 | 			SessionAuthenticationStrategy sessionAuthenticationStrategy) {
  288 | 		this.providedSessionAuthenticationStrategy = sessionAuthenticationStrategy;
  289 | 		this.propertiesThatRequireImplicitAuthentication
  290 | 			.add("sessionAuthenticationStrategy = " + sessionAuthenticationStrategy);
  291 | 		return this;
  292 | 	}
  293 | 
  294 | 	/**
  295 | 	 * Adds an additional {@link SessionAuthenticationStrategy} to be used within the
  296 | 	 * {@link CompositeSessionAuthenticationStrategy}.
  297 | 	 * @param sessionAuthenticationStrategy
  298 | 	 * @return the {@link SessionManagementConfigurer} for further customizations
  299 | 	 */
  300 | 	public SessionManagementConfigurer<H> addSessionAuthenticationStrategy(
  301 | 			SessionAuthenticationStrategy sessionAuthenticationStrategy) {
  302 | 		this.sessionAuthenticationStrategies.add(sessionAuthenticationStrategy);
  303 | 		return this;
  304 | 	}
  305 | 
  306 | 	/**
  307 | 	 * Allows changing the default {@link SessionFixationProtectionStrategy}.
  308 | 	 * @return the {@link SessionFixationConfigurer} for further customizations
  309 | 	 */
  310 | 	public SessionFixationConfigurer sessionFixation() {
  311 | 		return new SessionFixationConfigurer();
  312 | 	}
  313 | 
  314 | 	/**
  315 | 	 * Allows configuring session fixation protection.
  316 | 	 * @param sessionFixationCustomizer the {@link Customizer} to provide more options for
  317 | 	 * the {@link SessionFixationConfigurer}
  318 | 	 * @return the {@link SessionManagementConfigurer} for further customizations
  319 | 	 */
  320 | 	public SessionManagementConfigurer<H> sessionFixation(
  321 | 			Customizer<SessionFixationConfigurer> sessionFixationCustomizer) {
  322 | 		sessionFixationCustomizer.customize(new SessionFixationConfigurer());
  323 | 		return this;
  324 | 	}
  325 | 
  326 | 	/**
  327 | 	 * Controls the maximum number of sessions for a user. The default is to allow any
  328 | 	 * number of sessions.
  329 | 	 * @param maximumSessions the maximum number of sessions for a user
  330 | 	 * @return the {@link SessionManagementConfigurer} for further customizations
  331 | 	 */
  332 | 	public ConcurrencyControlConfigurer maximumSessions(int maximumSessions) {
  333 | 		this.sessionLimit = SessionLimit.of(maximumSessions);
  334 | 		this.propertiesThatRequireImplicitAuthentication.add("maximumSessions = " + maximumSessions);
  335 | 		return new ConcurrencyControlConfigurer();
  336 | 	}
  337 | 
  338 | 	/**
  339 | 	 * Controls the maximum number of sessions for a user. The default is to allow any
  340 | 	 * number of users.
  341 | 	 * @param sessionConcurrencyCustomizer the {@link Customizer} to provide more options
  342 | 	 * for the {@link ConcurrencyControlConfigurer}
  343 | 	 * @return the {@link SessionManagementConfigurer} for further customizations
  344 | 	 */
  345 | 	public SessionManagementConfigurer<H> sessionConcurrency(
  346 | 			Customizer<ConcurrencyControlConfigurer> sessionConcurrencyCustomizer) {
  347 | 		sessionConcurrencyCustomizer.customize(new ConcurrencyControlConfigurer());
  348 | 		return this;
  349 | 	}
  350 | 
  351 | 	/**
  352 | 	 * Invokes {@link #postProcess(Object)} and sets the
  353 | 	 * {@link SessionAuthenticationStrategy} for session fixation.
  354 | 	 * @param sessionFixationAuthenticationStrategy
  355 | 	 */
  356 | 	private void setSessionFixationAuthenticationStrategy(
  357 | 			SessionAuthenticationStrategy sessionFixationAuthenticationStrategy) {
  358 | 		this.sessionFixationAuthenticationStrategy = postProcess(sessionFixationAuthenticationStrategy);
  359 | 	}
  360 | 
  361 | 	@Override
  362 | 	public void init(H http) {
  363 | 		SecurityContextRepository securityContextRepository = http.getSharedObject(SecurityContextRepository.class);
  364 | 		boolean stateless = isStateless();
  365 | 		if (securityContextRepository == null) {
  366 | 			if (stateless) {
  367 | 				http.setSharedObject(SecurityContextRepository.class, new RequestAttributeSecurityContextRepository());
  368 | 				this.sessionManagementSecurityContextRepository = new NullSecurityContextRepository();
  369 | 			}
  370 | 			else {
  371 | 				HttpSessionSecurityContextRepository httpSecurityRepository = new HttpSessionSecurityContextRepository();
  372 | 				httpSecurityRepository.setDisableUrlRewriting(!this.enableSessionUrlRewriting);
  373 | 				httpSecurityRepository.setAllowSessionCreation(isAllowSessionCreation());
  374 | 				AuthenticationTrustResolver trustResolver = http.getSharedObject(AuthenticationTrustResolver.class);
  375 | 				if (trustResolver != null) {
  376 | 					httpSecurityRepository.setTrustResolver(trustResolver);
  377 | 				}
  378 | 				this.sessionManagementSecurityContextRepository = httpSecurityRepository;
  379 | 				DelegatingSecurityContextRepository defaultRepository = new DelegatingSecurityContextRepository(
  380 | 						httpSecurityRepository, new RequestAttributeSecurityContextRepository());
  381 | 				http.setSharedObject(SecurityContextRepository.class, defaultRepository);
  382 | 			}
  383 | 		}
  384 | 		else {
  385 | 			this.sessionManagementSecurityContextRepository = securityContextRepository;
  386 | 		}
  387 | 		RequestCache requestCache = http.getSharedObject(RequestCache.class);
  388 | 		if (requestCache == null) {
  389 | 			if (stateless) {
  390 | 				http.setSharedObject(RequestCache.class, new NullRequestCache());
  391 | 			}
  392 | 		}
  393 | 		http.setSharedObject(SessionAuthenticationStrategy.class, getSessionAuthenticationStrategy(http));
  394 | 		http.setSharedObject(InvalidSessionStrategy.class, getInvalidSessionStrategy());
  395 | 	}
  396 | 
  397 | 	@Override
  398 | 	public void configure(H http) {
  399 | 		SessionManagementFilter sessionManagementFilter = createSessionManagementFilter(http);
  400 | 		if (sessionManagementFilter != null) {
  401 | 			http.addFilter(sessionManagementFilter);
  402 | 		}
  403 | 		if (isConcurrentSessionControlEnabled()) {
  404 | 			ConcurrentSessionFilter concurrentSessionFilter = createConcurrencyFilter(http);
  405 | 
  406 | 			concurrentSessionFilter = postProcess(concurrentSessionFilter);
  407 | 			http.addFilter(concurrentSessionFilter);
  408 | 		}
  409 | 		if (!this.enableSessionUrlRewriting) {
  410 | 			http.addFilter(new DisableEncodeUrlFilter());
  411 | 		}
  412 | 		if (this.sessionPolicy == SessionCreationPolicy.ALWAYS) {
  413 | 			http.addFilter(new ForceEagerSessionCreationFilter());
  414 | 		}
  415 | 	}
  416 | 
  417 | 	private boolean shouldRequireExplicitAuthenticationStrategy() {
  418 | 		boolean defaultRequireExplicitAuthenticationStrategy = this.propertiesThatRequireImplicitAuthentication
  419 | 			.isEmpty();
  420 | 		if (this.requireExplicitAuthenticationStrategy == null) {
  421 | 			// explicit is not set, use default
  422 | 			return defaultRequireExplicitAuthenticationStrategy;
  423 | 		}
  424 | 		if (this.requireExplicitAuthenticationStrategy && !defaultRequireExplicitAuthenticationStrategy) {
  425 | 			// explicit disabled and implicit requires it
  426 | 			throw new IllegalStateException(
  427 | 					"Invalid configuration that explicitly sets requireExplicitAuthenticationStrategy to "
  428 | 							+ this.requireExplicitAuthenticationStrategy
  429 | 							+ " but implicitly requires it due to the following properties being set: "
  430 | 							+ this.propertiesThatRequireImplicitAuthentication);
  431 | 		}
  432 | 		return this.requireExplicitAuthenticationStrategy;
  433 | 	}
  434 | 
  435 | 	private SessionManagementFilter createSessionManagementFilter(H http) {
  436 | 		if (shouldRequireExplicitAuthenticationStrategy()) {
  437 | 			return null;
  438 | 		}
  439 | 		SecurityContextRepository securityContextRepository = this.sessionManagementSecurityContextRepository;
  440 | 		SessionManagementFilter sessionManagementFilter = new SessionManagementFilter(securityContextRepository,
  441 | 				getSessionAuthenticationStrategy(http));
  442 | 		if (this.sessionAuthenticationErrorUrl != null) {
  443 | 			sessionManagementFilter.setAuthenticationFailureHandler(
  444 | 					new SimpleUrlAuthenticationFailureHandler(this.sessionAuthenticationErrorUrl));
  445 | 		}
  446 | 		InvalidSessionStrategy strategy = getInvalidSessionStrategy();
  447 | 		if (strategy != null) {
  448 | 			sessionManagementFilter.setInvalidSessionStrategy(strategy);
  449 | 		}
  450 | 		AuthenticationFailureHandler failureHandler = getSessionAuthenticationFailureHandler();
  451 | 		if (failureHandler != null) {
  452 | 			sessionManagementFilter.setAuthenticationFailureHandler(failureHandler);
  453 | 		}
  454 | 		AuthenticationTrustResolver trustResolver = http.getSharedObject(AuthenticationTrustResolver.class);
  455 | 		if (trustResolver != null) {
  456 | 			sessionManagementFilter.setTrustResolver(trustResolver);
  457 | 		}
  458 | 		sessionManagementFilter.setSecurityContextHolderStrategy(getSecurityContextHolderStrategy());
  459 | 		return postProcess(sessionManagementFilter);
  460 | 	}
  461 | 
  462 | 	private ConcurrentSessionFilter createConcurrencyFilter(H http) {
  463 | 		SessionInformationExpiredStrategy expireStrategy = getExpiredSessionStrategy();
  464 | 		SessionRegistry sessionRegistry = getSessionRegistry(http);
  465 | 		ConcurrentSessionFilter concurrentSessionFilter = (expireStrategy != null)
  466 | 				? new ConcurrentSessionFilter(sessionRegistry, expireStrategy)
  467 | 				: new ConcurrentSessionFilter(sessionRegistry);
  468 | 		LogoutConfigurer<H> logoutConfigurer = http.getConfigurer(LogoutConfigurer.class);
  469 | 		if (logoutConfigurer != null) {
  470 | 			List<LogoutHandler> logoutHandlers = logoutConfigurer.getLogoutHandlers();
  471 | 			if (!CollectionUtils.isEmpty(logoutHandlers)) {
  472 | 				concurrentSessionFilter.setLogoutHandlers(logoutHandlers);
  473 | 			}
  474 | 		}
  475 | 		concurrentSessionFilter.setSecurityContextHolderStrategy(getSecurityContextHolderStrategy());
  476 | 		return concurrentSessionFilter;
  477 | 	}
  478 | 
  479 | 	/**
  480 | 	 * Gets the {@link InvalidSessionStrategy} to use. If null and
  481 | 	 * {@link #invalidSessionUrl} is not null defaults to
  482 | 	 * {@link SimpleRedirectInvalidSessionStrategy}.
  483 | 	 * @return the {@link InvalidSessionStrategy} to use
  484 | 	 */
  485 | 	InvalidSessionStrategy getInvalidSessionStrategy() {
  486 | 		if (this.invalidSessionStrategy != null) {
  487 | 			return this.invalidSessionStrategy;
  488 | 		}
  489 | 		if (this.invalidSessionUrl == null) {
  490 | 			return null;
  491 | 		}
  492 | 		this.invalidSessionStrategy = new SimpleRedirectInvalidSessionStrategy(this.invalidSessionUrl);
  493 | 		return this.invalidSessionStrategy;
  494 | 	}
  495 | 
  496 | 	SessionInformationExpiredStrategy getExpiredSessionStrategy() {
  497 | 		if (this.expiredSessionStrategy != null) {
  498 | 			return this.expiredSessionStrategy;
  499 | 		}
  500 | 		if (this.expiredUrl == null) {
  501 | 			return null;
  502 | 		}
  503 | 		this.expiredSessionStrategy = new SimpleRedirectSessionInformationExpiredStrategy(this.expiredUrl);
  504 | 		return this.expiredSessionStrategy;
  505 | 	}
  506 | 
  507 | 	AuthenticationFailureHandler getSessionAuthenticationFailureHandler() {
  508 | 		if (this.sessionAuthenticationFailureHandler != null) {
  509 | 			return this.sessionAuthenticationFailureHandler;
  510 | 		}
  511 | 		if (this.sessionAuthenticationErrorUrl == null) {
  512 | 			return null;
  513 | 		}
  514 | 		this.sessionAuthenticationFailureHandler = new SimpleUrlAuthenticationFailureHandler(
  515 | 				this.sessionAuthenticationErrorUrl);
  516 | 		return this.sessionAuthenticationFailureHandler;
  517 | 	}
  518 | 
  519 | 	/**
  520 | 	 * Gets the {@link SessionCreationPolicy}. Can not be null.
  521 | 	 * @return the {@link SessionCreationPolicy}
  522 | 	 */
  523 | 	SessionCreationPolicy getSessionCreationPolicy() {
  524 | 		if (this.sessionPolicy != null) {
  525 | 			return this.sessionPolicy;
  526 | 		}
  527 | 		SessionCreationPolicy sessionPolicy = getBuilder().getSharedObject(SessionCreationPolicy.class);
  528 | 		return (sessionPolicy != null) ? sessionPolicy : SessionCreationPolicy.IF_REQUIRED;
  529 | 	}
  530 | 
  531 | 	/**
  532 | 	 * Returns true if the {@link SessionCreationPolicy} allows session creation, else
  533 | 	 * false.
  534 | 	 * @return true if the {@link SessionCreationPolicy} allows session creation
  535 | 	 */
  536 | 	private boolean isAllowSessionCreation() {
  537 | 		SessionCreationPolicy sessionPolicy = getSessionCreationPolicy();
  538 | 		return SessionCreationPolicy.ALWAYS == sessionPolicy || SessionCreationPolicy.IF_REQUIRED == sessionPolicy;
  539 | 	}
  540 | 
  541 | 	/**
  542 | 	 * Returns true if the {@link SessionCreationPolicy} is stateless.
  543 | 	 * @return
  544 | 	 */
  545 | 	private boolean isStateless() {
  546 | 		SessionCreationPolicy sessionPolicy = getSessionCreationPolicy();
  547 | 		return SessionCreationPolicy.STATELESS == sessionPolicy;
  548 | 	}
  549 | 
  550 | 	/**
  551 | 	 * Gets the customized {@link SessionAuthenticationStrategy} if
  552 | 	 * {@link #sessionAuthenticationStrategy(SessionAuthenticationStrategy)} was
  553 | 	 * specified. Otherwise creates a default {@link SessionAuthenticationStrategy}.
  554 | 	 * @return the {@link SessionAuthenticationStrategy} to use
  555 | 	 */
  556 | 	private SessionAuthenticationStrategy getSessionAuthenticationStrategy(H http) {
  557 | 		if (this.sessionAuthenticationStrategy != null) {
  558 | 			return this.sessionAuthenticationStrategy;
  559 | 		}
  560 | 		List<SessionAuthenticationStrategy> delegateStrategies = this.sessionAuthenticationStrategies;
  561 | 		SessionAuthenticationStrategy defaultSessionAuthenticationStrategy;
  562 | 		if (this.providedSessionAuthenticationStrategy == null) {
  563 | 			// If the user did not provide a SessionAuthenticationStrategy
  564 | 			// then default to sessionFixationAuthenticationStrategy
  565 | 			defaultSessionAuthenticationStrategy = postProcess(this.sessionFixationAuthenticationStrategy);
  566 | 		}
  567 | 		else {
  568 | 			defaultSessionAuthenticationStrategy = this.providedSessionAuthenticationStrategy;
  569 | 		}
  570 | 		if (isConcurrentSessionControlEnabled()) {
  571 | 			SessionRegistry sessionRegistry = getSessionRegistry(http);
  572 | 			ConcurrentSessionControlAuthenticationStrategy concurrentSessionControlStrategy = new ConcurrentSessionControlAuthenticationStrategy(
  573 | 					sessionRegistry);
  574 | 			concurrentSessionControlStrategy.setMaximumSessions(this.sessionLimit);
  575 | 			concurrentSessionControlStrategy.setExceptionIfMaximumExceeded(this.maxSessionsPreventsLogin);
  576 | 			concurrentSessionControlStrategy = postProcess(concurrentSessionControlStrategy);
  577 | 			RegisterSessionAuthenticationStrategy registerSessionStrategy = new RegisterSessionAuthenticationStrategy(
  578 | 					sessionRegistry);
  579 | 			registerSessionStrategy = postProcess(registerSessionStrategy);
  580 | 
  581 | 			delegateStrategies.addAll(Arrays.asList(concurrentSessionControlStrategy,
  582 | 					defaultSessionAuthenticationStrategy, registerSessionStrategy));
  583 | 		}
  584 | 		else {
  585 | 			delegateStrategies.add(defaultSessionAuthenticationStrategy);
  586 | 		}
  587 | 		this.sessionAuthenticationStrategy = postProcess(
  588 | 				new CompositeSessionAuthenticationStrategy(delegateStrategies));
  589 | 		return this.sessionAuthenticationStrategy;
  590 | 	}
  591 | 
  592 | 	private SessionRegistry getSessionRegistry(H http) {
  593 | 		if (this.sessionRegistry == null) {
  594 | 			this.sessionRegistry = getBeanOrNull(SessionRegistry.class);
  595 | 		}
  596 | 		if (this.sessionRegistry == null) {
  597 | 			SessionRegistryImpl sessionRegistry = new SessionRegistryImpl();
  598 | 			registerDelegateApplicationListener(http, sessionRegistry);
  599 | 			this.sessionRegistry = sessionRegistry;
  600 | 		}
  601 | 		return this.sessionRegistry;
  602 | 	}
  603 | 
  604 | 	private void registerDelegateApplicationListener(H http, ApplicationListener<?> delegate) {
  605 | 		DelegatingApplicationListener delegating = getBeanOrNull(DelegatingApplicationListener.class);
  606 | 		if (delegating == null) {
  607 | 			return;
  608 | 		}
  609 | 		SmartApplicationListener smartListener = new GenericApplicationListenerAdapter(delegate);
  610 | 		delegating.addListener(smartListener);
  611 | 	}
  612 | 
  613 | 	/**
  614 | 	 * Returns true if the number of concurrent sessions per user should be restricted.
  615 | 	 * @return
  616 | 	 */
  617 | 	private boolean isConcurrentSessionControlEnabled() {
  618 | 		return this.sessionLimit != null;
  619 | 	}
  620 | 
  621 | 	/**
  622 | 	 * Creates the default {@link SessionAuthenticationStrategy} for session fixation.
  623 | 	 * @return the default {@link SessionAuthenticationStrategy} for session fixation
  624 | 	 */
  625 | 	private static SessionAuthenticationStrategy createDefaultSessionFixationProtectionStrategy() {
  626 | 		return new ChangeSessionIdAuthenticationStrategy();
  627 | 	}
  628 | 
  629 | 	private <T> T getBeanOrNull(Class<T> type) {
  630 | 		ApplicationContext context = getBuilder().getSharedObject(ApplicationContext.class);
  631 | 		if (context == null) {
  632 | 			return null;
  633 | 		}
  634 | 
  635 | 		return context.getBeanProvider(type).getIfUnique();
  636 | 	}
  637 | 
  638 | 	/**
  639 | 	 * Allows configuring SessionFixation protection.
  640 | 	 *
  641 | 	 * @author Rob Winch
  642 | 	 */
  643 | 	public final class SessionFixationConfigurer {
  644 | 
  645 | 		/**
  646 | 		 * Specifies that a new session should be created, but the session attributes from
  647 | 		 * the original {@link HttpSession} should not be retained.
  648 | 		 * @return the {@link SessionManagementConfigurer} for further customizations
  649 | 		 */
  650 | 		public SessionManagementConfigurer<H> newSession() {
  651 | 			SessionFixationProtectionStrategy sessionFixationProtectionStrategy = new SessionFixationProtectionStrategy();
  652 | 			sessionFixationProtectionStrategy.setMigrateSessionAttributes(false);
  653 | 			setSessionFixationAuthenticationStrategy(sessionFixationProtectionStrategy);
  654 | 			return SessionManagementConfigurer.this;
  655 | 		}
  656 | 
  657 | 		/**
  658 | 		 * Specifies that a new session should be created and the session attributes from
  659 | 		 * the original {@link HttpSession} should be retained.
  660 | 		 * @return the {@link SessionManagementConfigurer} for further customizations
  661 | 		 */
  662 | 		public SessionManagementConfigurer<H> migrateSession() {
  663 | 			setSessionFixationAuthenticationStrategy(new SessionFixationProtectionStrategy());
  664 | 			return SessionManagementConfigurer.this;
  665 | 		}
  666 | 
  667 | 		/**
  668 | 		 * Specifies that the Servlet container-provided session fixation protection
  669 | 		 * should be used. When a session authenticates, the Servlet method
  670 | 		 * {@code HttpServletRequest#changeSessionId()} is called to change the session ID
  671 | 		 * and retain all session attributes.
  672 | 		 * @return the {@link SessionManagementConfigurer} for further customizations
  673 | 		 */
  674 | 		public SessionManagementConfigurer<H> changeSessionId() {
  675 | 			setSessionFixationAuthenticationStrategy(new ChangeSessionIdAuthenticationStrategy());
  676 | 			return SessionManagementConfigurer.this;
  677 | 		}
  678 | 
  679 | 		/**
  680 | 		 * Specifies that no session fixation protection should be enabled. This may be
  681 | 		 * useful when utilizing other mechanisms for protecting against session fixation.
  682 | 		 * For example, if application container session fixation protection is already in
  683 | 		 * use. Otherwise, this option is not recommended.
  684 | 		 * @return the {@link SessionManagementConfigurer} for further customizations
  685 | 		 */
  686 | 		public SessionManagementConfigurer<H> none() {
  687 | 			setSessionFixationAuthenticationStrategy(new NullAuthenticatedSessionStrategy());
  688 | 			return SessionManagementConfigurer.this;
  689 | 		}
  690 | 
  691 | 	}
  692 | 
  693 | 	/**
  694 | 	 * Allows configuring controlling of multiple sessions.
  695 | 	 *
  696 | 	 * @author Rob Winch
  697 | 	 */
  698 | 	public final class ConcurrencyControlConfigurer {
  699 | 
  700 | 		private ConcurrencyControlConfigurer() {
  701 | 		}
  702 | 
  703 | 		/**
  704 | 		 * Controls the maximum number of sessions for a user. The default is to allow any
  705 | 		 * number of users.
  706 | 		 * @param maximumSessions the maximum number of sessions for a user
  707 | 		 * @return the {@link ConcurrencyControlConfigurer} for further customizations
  708 | 		 */
  709 | 		public ConcurrencyControlConfigurer maximumSessions(int maximumSessions) {
  710 | 			SessionManagementConfigurer.this.sessionLimit = SessionLimit.of(maximumSessions);
  711 | 			return this;
  712 | 		}
  713 | 
  714 | 		/**
  715 | 		 * Determines the behaviour when a session limit is detected.
  716 | 		 * @param sessionLimit the {@link SessionLimit} to check the maximum number of
  717 | 		 * sessions for a user
  718 | 		 * @return the {@link ConcurrencyControlConfigurer} for further customizations
  719 | 		 * @since 6.5
  720 | 		 */
  721 | 		public ConcurrencyControlConfigurer maximumSessions(SessionLimit sessionLimit) {
  722 | 			SessionManagementConfigurer.this.sessionLimit = sessionLimit;
  723 | 			return this;
  724 | 		}
  725 | 
  726 | 		/**
  727 | 		 * The URL to redirect to if a user tries to access a resource and their session
  728 | 		 * has been expired due to too many sessions for the current user. The default is
  729 | 		 * to write a simple error message to the response.
  730 | 		 * @param expiredUrl the URL to redirect to
  731 | 		 * @return the {@link ConcurrencyControlConfigurer} for further customizations
  732 | 		 */
  733 | 		public ConcurrencyControlConfigurer expiredUrl(String expiredUrl) {
  734 | 			SessionManagementConfigurer.this.expiredUrl = expiredUrl;
  735 | 			return this;
  736 | 		}
  737 | 
  738 | 		/**
  739 | 		 * Determines the behaviour when an expired session is detected.
  740 | 		 * @param expiredSessionStrategy the {@link SessionInformationExpiredStrategy} to
  741 | 		 * use when an expired session is detected.
  742 | 		 * @return the {@link ConcurrencyControlConfigurer} for further customizations
  743 | 		 */
  744 | 		public ConcurrencyControlConfigurer expiredSessionStrategy(
  745 | 				SessionInformationExpiredStrategy expiredSessionStrategy) {
  746 | 			SessionManagementConfigurer.this.expiredSessionStrategy = expiredSessionStrategy;
  747 | 			return this;
  748 | 		}
  749 | 
  750 | 		/**
  751 | 		 * If true, prevents a user from authenticating when the
  752 | 		 * {@link #maximumSessions(int)} has been reached. Otherwise (default), the user
  753 | 		 * who authenticates is allowed access and an existing user's session is expired.
  754 | 		 * The user's who's session is forcibly expired is sent to
  755 | 		 * {@link #expiredUrl(String)}. The advantage of this approach is if a user
  756 | 		 * accidentally does not log out, there is no need for an administrator to
  757 | 		 * intervene or wait till their session expires.
  758 | 		 * @param maxSessionsPreventsLogin true to have an error at time of
  759 | 		 * authentication, else false (default)
  760 | 		 * @return the {@link ConcurrencyControlConfigurer} for further customizations
  761 | 		 */
  762 | 		public ConcurrencyControlConfigurer maxSessionsPreventsLogin(boolean maxSessionsPreventsLogin) {
  763 | 			SessionManagementConfigurer.this.maxSessionsPreventsLogin = maxSessionsPreventsLogin;
  764 | 			return this;
  765 | 		}
  766 | 
  767 | 		/**
  768 | 		 * Controls the {@link SessionRegistry} implementation used. The default is
  769 | 		 * {@link SessionRegistryImpl} which is an in memory implementation.
  770 | 		 * @param sessionRegistry the {@link SessionRegistry} to use
  771 | 		 * @return the {@link ConcurrencyControlConfigurer} for further customizations
  772 | 		 */
  773 | 		public ConcurrencyControlConfigurer sessionRegistry(SessionRegistry sessionRegistry) {
  774 | 			SessionManagementConfigurer.this.sessionRegistry = sessionRegistry;
  775 | 			return this;
  776 | 		}
  777 | 
  778 | 	}
  779 | 
  780 | }
```
