<!-- learning-docs source archive（机械抓取原文，禁止手改代码内容） -->
<!-- origin: org.springframework.security:spring-security-web:7.2.0 | path: org/springframework/security/web/authentication/AbstractAuthenticationProcessingFilter.java -->
<!-- 正文引用：[[源码:AbstractAuthenticationProcessingFilter.java#L起-L止]] ；代码块头部：(src: AbstractAuthenticationProcessingFilter.java#L起-L止) -->

# AbstractAuthenticationProcessingFilter.java

| 项 | 值 |
| --- | --- |
| 来源 | `org.springframework.security:spring-security-web:7.2.0` |
| 路径 | `org/springframework/security/web/authentication/AbstractAuthenticationProcessingFilter.java` |
| 行数 | 566 |
| 在线地址 | https://repo1.maven.org/maven2/org/springframework/security/spring-security-web/7.2.0/spring-security-web-7.2.0-sources.jar |
| 抓取时间 | 2026-11-25T16:27:27.897Z |

```java
    1 | /*
    2 |  * Copyright 2004, 2005, 2006 Acegi Technology Pty Limited
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
   17 | package org.springframework.security.web.authentication;
   18 | 
   19 | import java.io.IOException;
   20 | import java.lang.reflect.Method;
   21 | import java.util.Set;
   22 | import java.util.stream.Collectors;
   23 | 
   24 | import jakarta.servlet.FilterChain;
   25 | import jakarta.servlet.ServletException;
   26 | import jakarta.servlet.ServletRequest;
   27 | import jakarta.servlet.ServletResponse;
   28 | import jakarta.servlet.http.HttpServletRequest;
   29 | import jakarta.servlet.http.HttpServletResponse;
   30 | import org.jspecify.annotations.Nullable;
   31 | 
   32 | import org.springframework.context.ApplicationEventPublisher;
   33 | import org.springframework.context.ApplicationEventPublisherAware;
   34 | import org.springframework.context.MessageSource;
   35 | import org.springframework.context.MessageSourceAware;
   36 | import org.springframework.context.support.MessageSourceAccessor;
   37 | import org.springframework.core.log.LogMessage;
   38 | import org.springframework.lang.Contract;
   39 | import org.springframework.security.authentication.AuthenticationCredentialsNotFoundException;
   40 | import org.springframework.security.authentication.AuthenticationDetailsSource;
   41 | import org.springframework.security.authentication.AuthenticationManager;
   42 | import org.springframework.security.authentication.InternalAuthenticationServiceException;
   43 | import org.springframework.security.authentication.event.InteractiveAuthenticationSuccessEvent;
   44 | import org.springframework.security.core.Authentication;
   45 | import org.springframework.security.core.AuthenticationException;
   46 | import org.springframework.security.core.GrantedAuthority;
   47 | import org.springframework.security.core.SpringSecurityMessageSource;
   48 | import org.springframework.security.core.context.SecurityContext;
   49 | import org.springframework.security.core.context.SecurityContextHolder;
   50 | import org.springframework.security.core.context.SecurityContextHolderStrategy;
   51 | import org.springframework.security.web.authentication.session.NullAuthenticatedSessionStrategy;
   52 | import org.springframework.security.web.authentication.session.SessionAuthenticationStrategy;
   53 | import org.springframework.security.web.context.RequestAttributeSecurityContextRepository;
   54 | import org.springframework.security.web.context.SecurityContextRepository;
   55 | import org.springframework.security.web.util.matcher.RequestMatcher;
   56 | import org.springframework.util.Assert;
   57 | import org.springframework.web.filter.GenericFilterBean;
   58 | 
   59 | import static org.springframework.security.web.servlet.util.matcher.PathPatternRequestMatcher.pathPattern;
   60 | 
   61 | /**
   62 |  * Abstract processor of browser-based HTTP-based authentication requests.
   63 |  *
   64 |  * <h3>Authentication Process</h3>
   65 |  *
   66 |  * The filter requires that you set the <tt>authenticationManager</tt> property. An
   67 |  * <tt>AuthenticationManager</tt> is required to process the authentication request tokens
   68 |  * created by implementing classes.
   69 |  * <p>
   70 |  * This filter will intercept a request and attempt to perform authentication from that
   71 |  * request if the request matches the
   72 |  * {@link #setRequiresAuthenticationRequestMatcher(RequestMatcher)}.
   73 |  * <p>
   74 |  * Authentication is performed by the
   75 |  * {@link #attemptAuthentication(HttpServletRequest, HttpServletResponse)
   76 |  * attemptAuthentication} method, which must be implemented by subclasses.
   77 |  *
   78 |  * <h4>Authentication Success</h4>
   79 |  *
   80 |  * If authentication is successful, the resulting {@link Authentication} object will be
   81 |  * placed into the <code>SecurityContext</code> for the current thread, which is
   82 |  * guaranteed to have already been created by an earlier filter.
   83 |  * <p>
   84 |  * The configured {@link #setAuthenticationSuccessHandler(AuthenticationSuccessHandler)
   85 |  * AuthenticationSuccessHandler} will then be called to take the redirect to the
   86 |  * appropriate destination after a successful login. The default behaviour is implemented
   87 |  * in a {@link SavedRequestAwareAuthenticationSuccessHandler} which will make use of any
   88 |  * <tt>DefaultSavedRequest</tt> set by the <tt>ExceptionTranslationFilter</tt> and
   89 |  * redirect the user to the URL contained therein. Otherwise it will redirect to the
   90 |  * webapp root "/". You can customize this behaviour by injecting a differently configured
   91 |  * instance of this class, or by using a different implementation.
   92 |  * <p>
   93 |  * See the
   94 |  * {@link #successfulAuthentication(HttpServletRequest, HttpServletResponse, FilterChain, Authentication)}
   95 |  * method for more information.
   96 |  *
   97 |  * <h4>Authentication Failure</h4>
   98 |  *
   99 |  * If authentication fails, it will delegate to the configured
  100 |  * {@link AuthenticationFailureHandler} to allow the failure information to be conveyed to
  101 |  * the client. The default implementation is {@link SimpleUrlAuthenticationFailureHandler}
  102 |  * , which sends a 401 error code to the client. It may also be configured with a failure
  103 |  * URL as an alternative. Again you can inject whatever behaviour you require here.
  104 |  *
  105 |  * <h4>Event Publication</h4>
  106 |  *
  107 |  * If authentication is successful, an {@link InteractiveAuthenticationSuccessEvent} will
  108 |  * be published via the application context. No events will be published if authentication
  109 |  * was unsuccessful, because this would generally be recorded via an
  110 |  * {@code AuthenticationManager}-specific application event.
  111 |  *
  112 |  * <h4>Session Authentication</h4>
  113 |  *
  114 |  * The class has an optional {@link SessionAuthenticationStrategy} which will be invoked
  115 |  * immediately after a successful call to {@code attemptAuthentication()}. Different
  116 |  * implementations {@link #setSessionAuthenticationStrategy(SessionAuthenticationStrategy)
  117 |  * can be injected} to enable things like session-fixation attack prevention or to control
  118 |  * the number of simultaneous sessions a principal may have.
  119 |  *
  120 |  * @author Ben Alex
  121 |  * @author Luke Taylor
  122 |  */
  123 | public abstract class AbstractAuthenticationProcessingFilter extends GenericFilterBean
  124 | 		implements ApplicationEventPublisherAware, MessageSourceAware {
  125 | 
  126 | 	private SecurityContextHolderStrategy securityContextHolderStrategy = SecurityContextHolder
  127 | 		.getContextHolderStrategy();
  128 | 
  129 | 	protected @Nullable ApplicationEventPublisher eventPublisher;
  130 | 
  131 | 	protected AuthenticationDetailsSource<HttpServletRequest, ?> authenticationDetailsSource = new WebAuthenticationDetailsSource();
  132 | 
  133 | 	private AuthenticationConverter authenticationConverter = (request) -> {
  134 | 		throw new AuthenticationCredentialsNotFoundException(
  135 | 				"Please either configure an AuthenticationConverter or override attemptAuthentication when extending AbstractAuthenticationProcessingFilter");
  136 | 	};
  137 | 
  138 | 	@SuppressWarnings("NullAway.Init")
  139 | 	private AuthenticationManager authenticationManager;
  140 | 
  141 | 	protected MessageSourceAccessor messages = SpringSecurityMessageSource.getAccessor();
  142 | 
  143 | 	private RememberMeServices rememberMeServices = new NullRememberMeServices();
  144 | 
  145 | 	private RequestMatcher requiresAuthenticationRequestMatcher;
  146 | 
  147 | 	private boolean continueChainBeforeSuccessfulAuthentication = false;
  148 | 
  149 | 	private boolean continueChainWhenNoAuthenticationResult;
  150 | 
  151 | 	private SessionAuthenticationStrategy sessionStrategy = new NullAuthenticatedSessionStrategy();
  152 | 
  153 | 	private boolean allowSessionCreation = true;
  154 | 
  155 | 	private AuthenticationSuccessHandler successHandler = new SavedRequestAwareAuthenticationSuccessHandler();
  156 | 
  157 | 	private AuthenticationFailureHandler failureHandler = new SimpleUrlAuthenticationFailureHandler();
  158 | 
  159 | 	private SecurityContextRepository securityContextRepository = new RequestAttributeSecurityContextRepository();
  160 | 
  161 | 	private boolean mfaEnabled;
  162 | 
  163 | 	/**
  164 | 	 * Creates a new instance.
  165 | 	 * @param defaultFilterProcessesUrl the default value for <tt>filterProcessesUrl</tt>.
  166 | 	 */
  167 | 	protected AbstractAuthenticationProcessingFilter(String defaultFilterProcessesUrl) {
  168 | 		this(pathPattern(defaultFilterProcessesUrl));
  169 | 	}
  170 | 
  171 | 	/**
  172 | 	 * Creates a new instance.
  173 | 	 * @param requiresAuthenticationRequestMatcher the {@link RequestMatcher} used to
  174 | 	 * determine if authentication is required. Cannot be null.
  175 | 	 */
  176 | 	protected AbstractAuthenticationProcessingFilter(RequestMatcher requiresAuthenticationRequestMatcher) {
  177 | 		Assert.notNull(requiresAuthenticationRequestMatcher, "requiresAuthenticationRequestMatcher cannot be null");
  178 | 		this.requiresAuthenticationRequestMatcher = requiresAuthenticationRequestMatcher;
  179 | 	}
  180 | 
  181 | 	/**
  182 | 	 * Creates a new instance with a default filterProcessesUrl and an
  183 | 	 * {@link AuthenticationManager}.
  184 | 	 * @param defaultFilterProcessesUrl the default value for <tt>filterProcessesUrl</tt>.
  185 | 	 * @param authenticationManager the {@link AuthenticationManager} used to authenticate
  186 | 	 * an {@link Authentication} object. Cannot be null.
  187 | 	 */
  188 | 	protected AbstractAuthenticationProcessingFilter(String defaultFilterProcessesUrl,
  189 | 			AuthenticationManager authenticationManager) {
  190 | 		this(pathPattern(defaultFilterProcessesUrl));
  191 | 		setAuthenticationManager(authenticationManager);
  192 | 	}
  193 | 
  194 | 	/**
  195 | 	 * Creates a new instance with a {@link RequestMatcher} and an
  196 | 	 * {@link AuthenticationManager}.
  197 | 	 * @param requiresAuthenticationRequestMatcher the {@link RequestMatcher} used to
  198 | 	 * determine if authentication is required. Cannot be null.
  199 | 	 * @param authenticationManager the {@link AuthenticationManager} used to authenticate
  200 | 	 * an {@link Authentication} object. Cannot be null.
  201 | 	 */
  202 | 	protected AbstractAuthenticationProcessingFilter(RequestMatcher requiresAuthenticationRequestMatcher,
  203 | 			AuthenticationManager authenticationManager) {
  204 | 		setRequiresAuthenticationRequestMatcher(requiresAuthenticationRequestMatcher);
  205 | 		setAuthenticationManager(authenticationManager);
  206 | 	}
  207 | 
  208 | 	@Override
  209 | 	public void afterPropertiesSet() {
  210 | 		Assert.notNull(this.authenticationManager, "authenticationManager must be specified");
  211 | 	}
  212 | 
  213 | 	/**
  214 | 	 * Invokes the {@link #requiresAuthentication(HttpServletRequest, HttpServletResponse)
  215 | 	 * requiresAuthentication} method to determine whether the request is for
  216 | 	 * authentication and should be handled by this filter. If it is an authentication
  217 | 	 * request, the {@link #attemptAuthentication(HttpServletRequest, HttpServletResponse)
  218 | 	 * attemptAuthentication} will be invoked to perform the authentication. There are
  219 | 	 * then three possible outcomes:
  220 | 	 * <ol>
  221 | 	 * <li>An <tt>Authentication</tt> object is returned. The configured
  222 | 	 * {@link SessionAuthenticationStrategy} will be invoked (to handle any
  223 | 	 * session-related behaviour such as creating a new session to protect against
  224 | 	 * session-fixation attacks) followed by the invocation of
  225 | 	 * {@link #successfulAuthentication(HttpServletRequest, HttpServletResponse, FilterChain, Authentication)}
  226 | 	 * method</li>
  227 | 	 * <li>An <tt>AuthenticationException</tt> occurs during authentication. The
  228 | 	 * {@link #unsuccessfulAuthentication(HttpServletRequest, HttpServletResponse, AuthenticationException)
  229 | 	 * unsuccessfulAuthentication} method will be invoked</li>
  230 | 	 * <li>Null is returned, indicating that the authentication process is incomplete. The
  231 | 	 * method will then return immediately, assuming that the subclass has done any
  232 | 	 * necessary work (such as redirects) to continue the authentication process. The
  233 | 	 * assumption is that a later request will be received by this method where the
  234 | 	 * returned <tt>Authentication</tt> object is not null.
  235 | 	 * </ol>
  236 | 	 */
  237 | 	@Override
  238 | 	public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
  239 | 			throws IOException, ServletException {
  240 | 		doFilter((HttpServletRequest) request, (HttpServletResponse) response, chain);
  241 | 	}
  242 | 
  243 | 	private void doFilter(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
  244 | 			throws IOException, ServletException {
  245 | 		if (!requiresAuthentication(request, response)) {
  246 | 			chain.doFilter(request, response);
  247 | 			return;
  248 | 		}
  249 | 		try {
  250 | 			Authentication authenticationResult = attemptAuthentication(request, response);
  251 | 			if (authenticationResult == null) {
  252 | 				if (this.continueChainWhenNoAuthenticationResult) {
  253 | 					chain.doFilter(request, response);
  254 | 					return;
  255 | 				}
  256 | 				// return immediately as subclass has indicated that it hasn't completed
  257 | 				return;
  258 | 			}
  259 | 			Authentication current = this.securityContextHolderStrategy.getContext().getAuthentication();
  260 | 			if (shouldPerformMfa(current, authenticationResult)) {
  261 | 				authenticationResult = authenticationResult.toBuilder()
  262 | 				// @formatter:off
  263 | 					.authorities((a) -> {
  264 | 						Set<String> newAuthorities = a.stream()
  265 | 							.map(GrantedAuthority::getAuthority)
  266 | 							.collect(Collectors.toUnmodifiableSet());
  267 | 						for (GrantedAuthority currentAuthority : current.getAuthorities()) {
  268 | 							if (!newAuthorities.contains(currentAuthority.getAuthority())) {
  269 | 								a.add(currentAuthority);
  270 | 							}
  271 | 						}
  272 | 					})
  273 | 					.build();
  274 | 					// @formatter:on
  275 | 			}
  276 | 			this.sessionStrategy.onAuthentication(authenticationResult, request, response);
  277 | 			// Authentication success
  278 | 			if (this.continueChainBeforeSuccessfulAuthentication) {
  279 | 				chain.doFilter(request, response);
  280 | 			}
  281 | 			successfulAuthentication(request, response, chain, authenticationResult);
  282 | 		}
  283 | 		catch (InternalAuthenticationServiceException failed) {
  284 | 			this.logger.error("An internal error occurred while trying to authenticate the user.", failed);
  285 | 			unsuccessfulAuthentication(request, response, failed);
  286 | 		}
  287 | 		catch (AuthenticationException ex) {
  288 | 			// Authentication failed
  289 | 			unsuccessfulAuthentication(request, response, ex);
  290 | 		}
  291 | 	}
  292 | 
  293 | 	@Contract("null, _ -> false")
  294 | 	private boolean shouldPerformMfa(@Nullable Authentication current, Authentication authenticationResult) {
  295 | 		if (!this.mfaEnabled) {
  296 | 			return false;
  297 | 		}
  298 | 		if (current == null || !current.isAuthenticated()) {
  299 | 			return false;
  300 | 		}
  301 | 		if (!declaresToBuilder(authenticationResult)) {
  302 | 			return false;
  303 | 		}
  304 | 		return current.getName().equals(authenticationResult.getName());
  305 | 	}
  306 | 
  307 | 	private static boolean declaresToBuilder(Authentication authentication) {
  308 | 		for (Method method : authentication.getClass().getDeclaredMethods()) {
  309 | 			if (method.getName().equals("toBuilder") && method.getParameterTypes().length == 0) {
  310 | 				return true;
  311 | 			}
  312 | 		}
  313 | 		return false;
  314 | 	}
  315 | 
  316 | 	/**
  317 | 	 * Indicates whether this filter should attempt to process a login request for the
  318 | 	 * current invocation.
  319 | 	 * <p>
  320 | 	 * It strips any parameters from the "path" section of the request URL (such as the
  321 | 	 * jsessionid parameter in <em>https://host/myapp/index.html;jsessionid=blah</em>)
  322 | 	 * before matching against the <code>filterProcessesUrl</code> property.
  323 | 	 * <p>
  324 | 	 * Subclasses may override for special requirements, such as Tapestry integration.
  325 | 	 * @return <code>true</code> if the filter should attempt authentication,
  326 | 	 * <code>false</code> otherwise.
  327 | 	 */
  328 | 	protected boolean requiresAuthentication(HttpServletRequest request, HttpServletResponse response) {
  329 | 		if (this.requiresAuthenticationRequestMatcher.matches(request)) {
  330 | 			return true;
  331 | 		}
  332 | 		if (this.logger.isTraceEnabled()) {
  333 | 			this.logger
  334 | 				.trace(LogMessage.format("Did not match request to %s", this.requiresAuthenticationRequestMatcher));
  335 | 		}
  336 | 		return false;
  337 | 	}
  338 | 
  339 | 	/**
  340 | 	 * Performs actual authentication.
  341 | 	 * <p>
  342 | 	 * The implementation should do one of the following:
  343 | 	 * <ol>
  344 | 	 * <li>Return a populated authentication token for the authenticated user, indicating
  345 | 	 * successful authentication</li>
  346 | 	 * <li>Return null, indicating that the authentication process is still in progress.
  347 | 	 * Before returning, the implementation should perform any additional work required to
  348 | 	 * complete the process.</li>
  349 | 	 * <li>Throw an <tt>AuthenticationException</tt> if the authentication process
  350 | 	 * fails</li>
  351 | 	 * </ol>
  352 | 	 * @param request from which to extract parameters and perform the authentication
  353 | 	 * @param response the response, which may be needed if the implementation has to do a
  354 | 	 * redirect as part of a multi-stage authentication process (such as OIDC).
  355 | 	 * @return the authenticated user token, or null if authentication is incomplete.
  356 | 	 * @throws AuthenticationException if authentication fails.
  357 | 	 */
  358 | 	public @Nullable Authentication attemptAuthentication(HttpServletRequest request, HttpServletResponse response)
  359 | 			throws AuthenticationException, IOException, ServletException {
  360 | 		Authentication authentication = this.authenticationConverter.convert(request);
  361 | 		if (authentication == null) {
  362 | 			return null;
  363 | 		}
  364 | 		Authentication result = this.authenticationManager.authenticate(authentication);
  365 | 		if (result == null) {
  366 | 			throw new ServletException("AuthenticationManager should not return null Authentication object.");
  367 | 		}
  368 | 		return result;
  369 | 	}
  370 | 
  371 | 	/**
  372 | 	 * Default behaviour for successful authentication.
  373 | 	 * <ol>
  374 | 	 * <li>Sets the successful <tt>Authentication</tt> object on the
  375 | 	 * {@link SecurityContextHolder}</li>
  376 | 	 * <li>Informs the configured <tt>RememberMeServices</tt> of the successful login</li>
  377 | 	 * <li>Fires an {@link InteractiveAuthenticationSuccessEvent} via the configured
  378 | 	 * <tt>ApplicationEventPublisher</tt></li>
  379 | 	 * <li>Delegates additional behaviour to the
  380 | 	 * {@link AuthenticationSuccessHandler}.</li>
  381 | 	 * </ol>
  382 | 	 *
  383 | 	 * Subclasses can override this method to continue the {@link FilterChain} after
  384 | 	 * successful authentication.
  385 | 	 * @param request
  386 | 	 * @param response
  387 | 	 * @param chain
  388 | 	 * @param authResult the object returned from the <tt>attemptAuthentication</tt>
  389 | 	 * method.
  390 | 	 * @throws IOException
  391 | 	 * @throws ServletException
  392 | 	 */
  393 | 	protected void successfulAuthentication(HttpServletRequest request, HttpServletResponse response, FilterChain chain,
  394 | 			Authentication authResult) throws IOException, ServletException {
  395 | 		SecurityContext context = this.securityContextHolderStrategy.createEmptyContext();
  396 | 		context.setAuthentication(authResult);
  397 | 		this.securityContextHolderStrategy.setContext(context);
  398 | 		this.securityContextRepository.saveContext(context, request, response);
  399 | 		if (this.logger.isDebugEnabled()) {
  400 | 			this.logger.debug(LogMessage.format("Set SecurityContextHolder to %s", authResult));
  401 | 		}
  402 | 		this.rememberMeServices.loginSuccess(request, response, authResult);
  403 | 		if (this.eventPublisher != null) {
  404 | 			this.eventPublisher.publishEvent(new InteractiveAuthenticationSuccessEvent(authResult, this.getClass()));
  405 | 		}
  406 | 		this.successHandler.onAuthenticationSuccess(request, response, authResult);
  407 | 	}
  408 | 
  409 | 	/**
  410 | 	 * Default behaviour for unsuccessful authentication.
  411 | 	 * <ol>
  412 | 	 * <li>Clears the {@link SecurityContextHolder}</li>
  413 | 	 * <li>Stores the exception in the session (if it exists or
  414 | 	 * <tt>allowSessionCreation</tt> is set to <tt>true</tt>)</li>
  415 | 	 * <li>Informs the configured <tt>RememberMeServices</tt> of the failed login</li>
  416 | 	 * <li>Delegates additional behaviour to the
  417 | 	 * {@link AuthenticationFailureHandler}.</li>
  418 | 	 * </ol>
  419 | 	 */
  420 | 	protected void unsuccessfulAuthentication(HttpServletRequest request, HttpServletResponse response,
  421 | 			AuthenticationException failed) throws IOException, ServletException {
  422 | 		this.securityContextHolderStrategy.clearContext();
  423 | 		this.logger.trace("Failed to process authentication request", failed);
  424 | 		this.logger.trace("Cleared SecurityContextHolder");
  425 | 		this.logger.trace("Handling authentication failure");
  426 | 		this.rememberMeServices.loginFail(request, response);
  427 | 		this.failureHandler.onAuthenticationFailure(request, response, failed);
  428 | 	}
  429 | 
  430 | 	public void setAuthenticationConverter(AuthenticationConverter authenticationConverter) {
  431 | 		Assert.notNull(authenticationConverter, "authenticationConverter cannot be null");
  432 | 		this.authenticationConverter = authenticationConverter;
  433 | 		this.continueChainWhenNoAuthenticationResult = true;
  434 | 	}
  435 | 
  436 | 	protected AuthenticationManager getAuthenticationManager() {
  437 | 		return this.authenticationManager;
  438 | 	}
  439 | 
  440 | 	public void setAuthenticationManager(AuthenticationManager authenticationManager) {
  441 | 		this.authenticationManager = authenticationManager;
  442 | 	}
  443 | 
  444 | 	/**
  445 | 	 * Sets the URL that determines if authentication is required.
  446 | 	 * @param filterProcessesUrl
  447 | 	 */
  448 | 	public void setFilterProcessesUrl(String filterProcessesUrl) {
  449 | 		setRequiresAuthenticationRequestMatcher(pathPattern(filterProcessesUrl));
  450 | 	}
  451 | 
  452 | 	public final void setRequiresAuthenticationRequestMatcher(RequestMatcher requestMatcher) {
  453 | 		Assert.notNull(requestMatcher, "requestMatcher cannot be null or empty");
  454 | 		this.requiresAuthenticationRequestMatcher = requestMatcher;
  455 | 	}
  456 | 
  457 | 	public RememberMeServices getRememberMeServices() {
  458 | 		return this.rememberMeServices;
  459 | 	}
  460 | 
  461 | 	public void setRememberMeServices(RememberMeServices rememberMeServices) {
  462 | 		Assert.notNull(rememberMeServices, "rememberMeServices cannot be null");
  463 | 		this.rememberMeServices = rememberMeServices;
  464 | 	}
  465 | 
  466 | 	/**
  467 | 	 * Indicates if the filter chain should be continued prior to delegation to
  468 | 	 * {@link #successfulAuthentication(HttpServletRequest, HttpServletResponse, FilterChain, Authentication)},
  469 | 	 * which may be useful in certain environment (such as Tapestry applications).
  470 | 	 * Defaults to <code>false</code>.
  471 | 	 */
  472 | 	public void setContinueChainBeforeSuccessfulAuthentication(boolean continueChainBeforeSuccessfulAuthentication) {
  473 | 		this.continueChainBeforeSuccessfulAuthentication = continueChainBeforeSuccessfulAuthentication;
  474 | 	}
  475 | 
  476 | 	@Override
  477 | 	public void setApplicationEventPublisher(ApplicationEventPublisher eventPublisher) {
  478 | 		this.eventPublisher = eventPublisher;
  479 | 	}
  480 | 
  481 | 	public void setAuthenticationDetailsSource(
  482 | 			AuthenticationDetailsSource<HttpServletRequest, ?> authenticationDetailsSource) {
  483 | 		Assert.notNull(authenticationDetailsSource, "AuthenticationDetailsSource required");
  484 | 		this.authenticationDetailsSource = authenticationDetailsSource;
  485 | 	}
  486 | 
  487 | 	@Override
  488 | 	public void setMessageSource(MessageSource messageSource) {
  489 | 		this.messages = new MessageSourceAccessor(messageSource);
  490 | 	}
  491 | 
  492 | 	protected boolean getAllowSessionCreation() {
  493 | 		return this.allowSessionCreation;
  494 | 	}
  495 | 
  496 | 	public void setAllowSessionCreation(boolean allowSessionCreation) {
  497 | 		this.allowSessionCreation = allowSessionCreation;
  498 | 	}
  499 | 
  500 | 	/**
  501 | 	 * Enables Multi-Factor Authentication (MFA) support.
  502 | 	 * @param mfaEnabled true to enable MFA support, false to disable it. Default is
  503 | 	 * false.
  504 | 	 */
  505 | 	public void setMfaEnabled(boolean mfaEnabled) {
  506 | 		this.mfaEnabled = mfaEnabled;
  507 | 	}
  508 | 
  509 | 	/**
  510 | 	 * The session handling strategy which will be invoked immediately after an
  511 | 	 * authentication request is successfully processed by the
  512 | 	 * <tt>AuthenticationManager</tt>. Used, for example, to handle changing of the
  513 | 	 * session identifier to prevent session fixation attacks.
  514 | 	 * @param sessionStrategy the implementation to use. If not set a null implementation
  515 | 	 * is used.
  516 | 	 */
  517 | 	public void setSessionAuthenticationStrategy(SessionAuthenticationStrategy sessionStrategy) {
  518 | 		this.sessionStrategy = sessionStrategy;
  519 | 	}
  520 | 
  521 | 	/**
  522 | 	 * Sets the strategy used to handle a successful authentication. By default a
  523 | 	 * {@link SavedRequestAwareAuthenticationSuccessHandler} is used.
  524 | 	 */
  525 | 	public void setAuthenticationSuccessHandler(AuthenticationSuccessHandler successHandler) {
  526 | 		Assert.notNull(successHandler, "successHandler cannot be null");
  527 | 		this.successHandler = successHandler;
  528 | 	}
  529 | 
  530 | 	public void setAuthenticationFailureHandler(AuthenticationFailureHandler failureHandler) {
  531 | 		Assert.notNull(failureHandler, "failureHandler cannot be null");
  532 | 		this.failureHandler = failureHandler;
  533 | 	}
  534 | 
  535 | 	/**
  536 | 	 * Sets the {@link SecurityContextRepository} to save the {@link SecurityContext} on
  537 | 	 * authentication success. The default action is not to save the
  538 | 	 * {@link SecurityContext}.
  539 | 	 * @param securityContextRepository the {@link SecurityContextRepository} to use.
  540 | 	 * Cannot be null.
  541 | 	 */
  542 | 	public void setSecurityContextRepository(SecurityContextRepository securityContextRepository) {
  543 | 		Assert.notNull(securityContextRepository, "securityContextRepository cannot be null");
  544 | 		this.securityContextRepository = securityContextRepository;
  545 | 	}
  546 | 
  547 | 	/**
  548 | 	 * Sets the {@link SecurityContextHolderStrategy} to use. The default action is to use
  549 | 	 * the {@link SecurityContextHolderStrategy} stored in {@link SecurityContextHolder}.
  550 | 	 *
  551 | 	 * @since 5.8
  552 | 	 */
  553 | 	public void setSecurityContextHolderStrategy(SecurityContextHolderStrategy securityContextHolderStrategy) {
  554 | 		Assert.notNull(securityContextHolderStrategy, "securityContextHolderStrategy cannot be null");
  555 | 		this.securityContextHolderStrategy = securityContextHolderStrategy;
  556 | 	}
  557 | 
  558 | 	protected AuthenticationSuccessHandler getSuccessHandler() {
  559 | 		return this.successHandler;
  560 | 	}
  561 | 
  562 | 	protected AuthenticationFailureHandler getFailureHandler() {
  563 | 		return this.failureHandler;
  564 | 	}
  565 | 
  566 | }
```
