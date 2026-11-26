<!-- learning-docs source archive（机械抓取原文，禁止手改代码内容） -->
<!-- origin: org.springframework.security:spring-security-web:7.2.0 | path: org/springframework/security/web/context/HttpSessionSecurityContextRepository.java -->
<!-- 正文引用：[[源码:HttpSessionSecurityContextRepository.java#L起-L止]] ；代码块头部：(src: HttpSessionSecurityContextRepository.java#L起-L止) -->

# HttpSessionSecurityContextRepository.java

| 项 | 值 |
| --- | --- |
| 来源 | `org.springframework.security:spring-security-web:7.2.0` |
| 路径 | `org/springframework/security/web/context/HttpSessionSecurityContextRepository.java` |
| 行数 | 481 |
| 在线地址 | https://repo1.maven.org/maven2/org/springframework/security/spring-security-web/7.2.0/spring-security-web-7.2.0-sources.jar |
| 抓取时间 | 2026-11-25T16:27:28.448Z |

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
   17 | package org.springframework.security.web.context;
   18 | 
   19 | import java.util.function.Supplier;
   20 | 
   21 | import jakarta.servlet.AsyncContext;
   22 | import jakarta.servlet.ServletRequest;
   23 | import jakarta.servlet.ServletResponse;
   24 | import jakarta.servlet.http.HttpServletRequest;
   25 | import jakarta.servlet.http.HttpServletRequestWrapper;
   26 | import jakarta.servlet.http.HttpServletResponse;
   27 | import jakarta.servlet.http.HttpSession;
   28 | import org.apache.commons.logging.Log;
   29 | import org.apache.commons.logging.LogFactory;
   30 | import org.jspecify.annotations.Nullable;
   31 | 
   32 | import org.springframework.core.annotation.AnnotationUtils;
   33 | import org.springframework.core.log.LogMessage;
   34 | import org.springframework.security.authentication.AuthenticationTrustResolver;
   35 | import org.springframework.security.authentication.AuthenticationTrustResolverImpl;
   36 | import org.springframework.security.core.Authentication;
   37 | import org.springframework.security.core.Transient;
   38 | import org.springframework.security.core.context.DeferredSecurityContext;
   39 | import org.springframework.security.core.context.SecurityContext;
   40 | import org.springframework.security.core.context.SecurityContextHolder;
   41 | import org.springframework.security.core.context.SecurityContextHolderStrategy;
   42 | import org.springframework.util.Assert;
   43 | import org.springframework.web.util.WebUtils;
   44 | 
   45 | /**
   46 |  * A {@code SecurityContextRepository} implementation which stores the security context in
   47 |  * the {@code HttpSession} between requests.
   48 |  * <p>
   49 |  * The {@code HttpSession} will be queried to retrieve the {@code SecurityContext} in the
   50 |  * <tt>loadContext</tt> method (using the key {@link #SPRING_SECURITY_CONTEXT_KEY} by
   51 |  * default). If a valid {@code SecurityContext} cannot be obtained from the
   52 |  * {@code HttpSession} for whatever reason, a fresh {@code SecurityContext} will be
   53 |  * created by calling by {@link SecurityContextHolder#createEmptyContext()} and this
   54 |  * instance will be returned instead.
   55 |  * <p>
   56 |  * When <tt>saveContext</tt> is called, the context will be stored under the same key,
   57 |  * provided
   58 |  * <ol>
   59 |  * <li>The value has changed</li>
   60 |  * <li>The configured <tt>AuthenticationTrustResolver</tt> does not report that the
   61 |  * contents represent an anonymous user</li>
   62 |  * </ol>
   63 |  * <p>
   64 |  * With the standard configuration, no {@code HttpSession} will be created during
   65 |  * <tt>loadContext</tt> if one does not already exist. When <tt>saveContext</tt> is called
   66 |  * at the end of the web request, and no session exists, a new {@code HttpSession} will
   67 |  * <b>only</b> be created if the supplied {@code SecurityContext} is not equal to an empty
   68 |  * {@code SecurityContext} instance. This avoids needless <code>HttpSession</code>
   69 |  * creation, but automates the storage of changes made to the context during the request.
   70 |  * Note that if {@link SecurityContextPersistenceFilter} is configured to eagerly create
   71 |  * sessions, then the session-minimisation logic applied here will not make any
   72 |  * difference. If you are using eager session creation, then you should ensure that the
   73 |  * <tt>allowSessionCreation</tt> property of this class is set to <tt>true</tt> (the
   74 |  * default).
   75 |  * <p>
   76 |  * If for whatever reason no {@code HttpSession} should <b>ever</b> be created (for
   77 |  * example, if Basic authentication is being used or similar clients that will never
   78 |  * present the same {@code jsessionid}), then {@link #setAllowSessionCreation(boolean)
   79 |  * allowSessionCreation} should be set to <code>false</code>. Only do this if you really
   80 |  * need to conserve server memory and ensure all classes using the
   81 |  * {@code SecurityContextHolder} are designed to have no persistence of the
   82 |  * {@code SecurityContext} between web requests.
   83 |  *
   84 |  * @author Luke Taylor
   85 |  * @since 3.0
   86 |  */
   87 | public class HttpSessionSecurityContextRepository implements SecurityContextRepository {
   88 | 
   89 | 	/**
   90 | 	 * The default key under which the security context will be stored in the session.
   91 | 	 */
   92 | 	public static final String SPRING_SECURITY_CONTEXT_KEY = "SPRING_SECURITY_CONTEXT";
   93 | 
   94 | 	protected final Log logger = LogFactory.getLog(this.getClass());
   95 | 
   96 | 	private SecurityContextHolderStrategy securityContextHolderStrategy = SecurityContextHolder
   97 | 		.getContextHolderStrategy();
   98 | 
   99 | 	/**
  100 | 	 * SecurityContext instance used to check for equality with default (unauthenticated)
  101 | 	 * content.
  102 | 	 */
  103 | 	private Object contextObject = this.securityContextHolderStrategy.createEmptyContext();
  104 | 
  105 | 	private boolean allowSessionCreation = true;
  106 | 
  107 | 	private boolean disableUrlRewriting = false;
  108 | 
  109 | 	private String springSecurityContextKey = SPRING_SECURITY_CONTEXT_KEY;
  110 | 
  111 | 	private AuthenticationTrustResolver trustResolver = new AuthenticationTrustResolverImpl();
  112 | 
  113 | 	/**
  114 | 	 * Gets the security context for the current request (if available) and returns it.
  115 | 	 * <p>
  116 | 	 * If the session is null, the context object is null or the context object stored in
  117 | 	 * the session is not an instance of {@code SecurityContext}, a new context object
  118 | 	 * will be generated and returned.
  119 | 	 * @deprecated please see {@link SecurityContextRepository#loadContext}
  120 | 	 */
  121 | 	@Deprecated
  122 | 	@Override
  123 | 	public SecurityContext loadContext(HttpRequestResponseHolder requestResponseHolder) {
  124 | 		HttpServletRequest request = requestResponseHolder.getRequest();
  125 | 		HttpServletResponse response = requestResponseHolder.getResponse();
  126 | 		HttpSession httpSession = request.getSession(false);
  127 | 		SecurityContext context = readSecurityContextFromSession(httpSession);
  128 | 		if (context == null) {
  129 | 			context = generateNewContext();
  130 | 			if (this.logger.isTraceEnabled()) {
  131 | 				this.logger.trace(LogMessage.format("Created %s", context));
  132 | 			}
  133 | 		}
  134 | 		if (response != null) {
  135 | 			SaveToSessionResponseWrapper wrappedResponse = new SaveToSessionResponseWrapper(response, request,
  136 | 					httpSession != null, context);
  137 | 			wrappedResponse.setSecurityContextHolderStrategy(this.securityContextHolderStrategy);
  138 | 			requestResponseHolder.setResponse(wrappedResponse);
  139 | 			requestResponseHolder.setRequest(new SaveToSessionRequestWrapper(request, wrappedResponse));
  140 | 		}
  141 | 		return context;
  142 | 	}
  143 | 
  144 | 	@Override
  145 | 	public DeferredSecurityContext loadDeferredContext(HttpServletRequest request) {
  146 | 		Supplier<SecurityContext> supplier = () -> readSecurityContextFromSession(request.getSession(false));
  147 | 		return new SupplierDeferredSecurityContext(supplier, this.securityContextHolderStrategy);
  148 | 	}
  149 | 
  150 | 	@Override
  151 | 	public void saveContext(SecurityContext context, HttpServletRequest request, HttpServletResponse response) {
  152 | 		SaveContextOnUpdateOrErrorResponseWrapper responseWrapper = WebUtils.getNativeResponse(response,
  153 | 				SaveContextOnUpdateOrErrorResponseWrapper.class);
  154 | 		if (responseWrapper == null) {
  155 | 			saveContextInHttpSession(context, request);
  156 | 			return;
  157 | 		}
  158 | 		responseWrapper.saveContext(context);
  159 | 	}
  160 | 
  161 | 	private void saveContextInHttpSession(SecurityContext context, HttpServletRequest request) {
  162 | 		if (isTransient(context) || isTransient(context.getAuthentication())) {
  163 | 			return;
  164 | 		}
  165 | 		SecurityContext emptyContext = generateNewContext();
  166 | 		if (emptyContext.equals(context)) {
  167 | 			HttpSession session = request.getSession(false);
  168 | 			removeContextFromSession(context, session);
  169 | 		}
  170 | 		else {
  171 | 			boolean createSession = this.allowSessionCreation;
  172 | 			HttpSession session = request.getSession(createSession);
  173 | 			setContextInSession(context, session);
  174 | 		}
  175 | 	}
  176 | 
  177 | 	private void setContextInSession(SecurityContext context, HttpSession session) {
  178 | 		if (session != null) {
  179 | 			session.setAttribute(this.springSecurityContextKey, context);
  180 | 			if (this.logger.isDebugEnabled()) {
  181 | 				this.logger.debug(LogMessage.format("Stored %s to HttpSession [%s]", context, session));
  182 | 			}
  183 | 		}
  184 | 	}
  185 | 
  186 | 	private void removeContextFromSession(SecurityContext context, HttpSession session) {
  187 | 		if (session != null) {
  188 | 			session.removeAttribute(this.springSecurityContextKey);
  189 | 			if (this.logger.isDebugEnabled()) {
  190 | 				this.logger.debug(LogMessage.format("Removed %s from HttpSession [%s]", context, session));
  191 | 			}
  192 | 		}
  193 | 	}
  194 | 
  195 | 	@Override
  196 | 	public boolean containsContext(HttpServletRequest request) {
  197 | 		HttpSession session = request.getSession(false);
  198 | 		if (session == null) {
  199 | 			return false;
  200 | 		}
  201 | 		return session.getAttribute(this.springSecurityContextKey) != null;
  202 | 	}
  203 | 
  204 | 	/**
  205 | 	 * Reads the {@link SecurityContext} from the given session, if present.
  206 | 	 * @param httpSession the session obtained from the request.
  207 | 	 */
  208 | 	private @Nullable SecurityContext readSecurityContextFromSession(HttpSession httpSession) {
  209 | 		if (httpSession == null) {
  210 | 			this.logger.trace("No HttpSession currently exists");
  211 | 			return null;
  212 | 		}
  213 | 		// Session exists, so try to obtain a context from it.
  214 | 		Object contextFromSession = httpSession.getAttribute(this.springSecurityContextKey);
  215 | 		if (contextFromSession == null) {
  216 | 			if (this.logger.isTraceEnabled()) {
  217 | 				this.logger.trace(LogMessage.format("Did not find SecurityContext in HttpSession %s "
  218 | 						+ "using the SPRING_SECURITY_CONTEXT session attribute", httpSession.getId()));
  219 | 			}
  220 | 			return null;
  221 | 		}
  222 | 
  223 | 		// We now have the security context object from the session.
  224 | 		if (!(contextFromSession instanceof SecurityContext)) {
  225 | 			this.logger.warn(LogMessage.format(
  226 | 					"%s did not contain a SecurityContext but contained: '%s'; are you improperly "
  227 | 							+ "modifying the HttpSession directly (you should always use SecurityContextHolder) "
  228 | 							+ "or using the HttpSession attribute reserved for this class?",
  229 | 					this.springSecurityContextKey, contextFromSession));
  230 | 			return null;
  231 | 		}
  232 | 
  233 | 		if (this.logger.isTraceEnabled()) {
  234 | 			this.logger
  235 | 				.trace(LogMessage.format("Retrieved %s from %s", contextFromSession, this.springSecurityContextKey));
  236 | 		}
  237 | 		else if (this.logger.isDebugEnabled()) {
  238 | 			this.logger.debug(LogMessage.format("Retrieved %s", contextFromSession));
  239 | 		}
  240 | 		// Everything OK. The only non-null return from this method.
  241 | 		return (SecurityContext) contextFromSession;
  242 | 	}
  243 | 
  244 | 	/**
  245 | 	 * By default, calls {@link SecurityContextHolder#createEmptyContext()} to obtain a
  246 | 	 * new context (there should be no context present in the holder when this method is
  247 | 	 * called). Using this approach the context creation strategy is decided by the
  248 | 	 * {@link SecurityContextHolderStrategy} in use. The default implementations will
  249 | 	 * return a new <tt>SecurityContextImpl</tt>.
  250 | 	 * @return a new SecurityContext instance. Never null.
  251 | 	 */
  252 | 	protected SecurityContext generateNewContext() {
  253 | 		return this.securityContextHolderStrategy.createEmptyContext();
  254 | 	}
  255 | 
  256 | 	/**
  257 | 	 * If set to true (the default), a session will be created (if required) to store the
  258 | 	 * security context if it is determined that its contents are different from the
  259 | 	 * default empty context value.
  260 | 	 * <p>
  261 | 	 * Note that setting this flag to false does not prevent this class from storing the
  262 | 	 * security context. If your application (or another filter) creates a session, then
  263 | 	 * the security context will still be stored for an authenticated user.
  264 | 	 * @param allowSessionCreation
  265 | 	 */
  266 | 	public void setAllowSessionCreation(boolean allowSessionCreation) {
  267 | 		this.allowSessionCreation = allowSessionCreation;
  268 | 	}
  269 | 
  270 | 	/**
  271 | 	 * Allows the use of session identifiers in URLs to be disabled. Off by default.
  272 | 	 * @param disableUrlRewriting set to <tt>true</tt> to disable URL encoding methods in
  273 | 	 * the response wrapper and prevent the use of <tt>jsessionid</tt> parameters.
  274 | 	 */
  275 | 	public void setDisableUrlRewriting(boolean disableUrlRewriting) {
  276 | 		this.disableUrlRewriting = disableUrlRewriting;
  277 | 	}
  278 | 
  279 | 	/**
  280 | 	 * Allows the session attribute name to be customized for this repository instance.
  281 | 	 * @param springSecurityContextKey the key under which the security context will be
  282 | 	 * stored. Defaults to {@link #SPRING_SECURITY_CONTEXT_KEY}.
  283 | 	 */
  284 | 	public void setSpringSecurityContextKey(String springSecurityContextKey) {
  285 | 		Assert.hasText(springSecurityContextKey, "springSecurityContextKey cannot be empty");
  286 | 		this.springSecurityContextKey = springSecurityContextKey;
  287 | 	}
  288 | 
  289 | 	/**
  290 | 	 * Sets the {@link SecurityContextHolderStrategy} to use. The default action is to use
  291 | 	 * the {@link SecurityContextHolderStrategy} stored in {@link SecurityContextHolder}.
  292 | 	 *
  293 | 	 * @since 5.8
  294 | 	 */
  295 | 	public void setSecurityContextHolderStrategy(SecurityContextHolderStrategy strategy) {
  296 | 		this.securityContextHolderStrategy = strategy;
  297 | 		this.contextObject = this.securityContextHolderStrategy.createEmptyContext();
  298 | 	}
  299 | 
  300 | 	private boolean isTransient(@Nullable Object object) {
  301 | 		if (object == null) {
  302 | 			return false;
  303 | 		}
  304 | 		return AnnotationUtils.getAnnotation(object.getClass(), Transient.class) != null;
  305 | 	}
  306 | 
  307 | 	/**
  308 | 	 * Sets the {@link AuthenticationTrustResolver} to be used. The default is
  309 | 	 * {@link AuthenticationTrustResolverImpl}.
  310 | 	 * @param trustResolver the {@link AuthenticationTrustResolver} to use. Cannot be
  311 | 	 * null.
  312 | 	 */
  313 | 	public void setTrustResolver(AuthenticationTrustResolver trustResolver) {
  314 | 		Assert.notNull(trustResolver, "trustResolver cannot be null");
  315 | 		this.trustResolver = trustResolver;
  316 | 	}
  317 | 
  318 | 	private static class SaveToSessionRequestWrapper extends HttpServletRequestWrapper {
  319 | 
  320 | 		private final SaveContextOnUpdateOrErrorResponseWrapper response;
  321 | 
  322 | 		SaveToSessionRequestWrapper(HttpServletRequest request, SaveContextOnUpdateOrErrorResponseWrapper response) {
  323 | 			super(request);
  324 | 			this.response = response;
  325 | 		}
  326 | 
  327 | 		@Override
  328 | 		public AsyncContext startAsync() {
  329 | 			this.response.disableSaveOnResponseCommitted();
  330 | 			return super.startAsync();
  331 | 		}
  332 | 
  333 | 		@Override
  334 | 		public AsyncContext startAsync(ServletRequest servletRequest, ServletResponse servletResponse)
  335 | 				throws IllegalStateException {
  336 | 			this.response.disableSaveOnResponseCommitted();
  337 | 			return super.startAsync(servletRequest, servletResponse);
  338 | 		}
  339 | 
  340 | 	}
  341 | 
  342 | 	/**
  343 | 	 * Wrapper that is applied to every request/response to update the
  344 | 	 * <code>HttpSession</code> with the <code>SecurityContext</code> when a
  345 | 	 * <code>sendError()</code> or <code>sendRedirect</code> happens. See SEC-398.
  346 | 	 * <p>
  347 | 	 * Stores the necessary state from the start of the request in order to make a
  348 | 	 * decision about whether the security context has changed before saving it.
  349 | 	 */
  350 | 	final class SaveToSessionResponseWrapper extends SaveContextOnUpdateOrErrorResponseWrapper {
  351 | 
  352 | 		private final Log logger = HttpSessionSecurityContextRepository.this.logger;
  353 | 
  354 | 		private final HttpServletRequest request;
  355 | 
  356 | 		private final boolean httpSessionExistedAtStartOfRequest;
  357 | 
  358 | 		private final SecurityContext contextBeforeExecution;
  359 | 
  360 | 		private final @Nullable Authentication authBeforeExecution;
  361 | 
  362 | 		private boolean isSaveContextInvoked;
  363 | 
  364 | 		/**
  365 | 		 * Takes the parameters required to call <code>saveContext()</code> successfully
  366 | 		 * in addition to the request and the response object we are wrapping.
  367 | 		 * @param request the request object (used to obtain the session, if one exists).
  368 | 		 * @param httpSessionExistedAtStartOfRequest indicates whether there was a session
  369 | 		 * in place before the filter chain executed. If this is true, and the session is
  370 | 		 * found to be null, this indicates that it was invalidated during the request and
  371 | 		 * a new session will now be created.
  372 | 		 * @param context the context before the filter chain executed. The context will
  373 | 		 * only be stored if it or its contents changed during the request.
  374 | 		 */
  375 | 		SaveToSessionResponseWrapper(HttpServletResponse response, HttpServletRequest request,
  376 | 				boolean httpSessionExistedAtStartOfRequest, SecurityContext context) {
  377 | 			super(response, HttpSessionSecurityContextRepository.this.disableUrlRewriting);
  378 | 			this.request = request;
  379 | 			this.httpSessionExistedAtStartOfRequest = httpSessionExistedAtStartOfRequest;
  380 | 			this.contextBeforeExecution = context;
  381 | 			this.authBeforeExecution = context.getAuthentication();
  382 | 		}
  383 | 
  384 | 		/**
  385 | 		 * Stores the supplied security context in the session (if available) and if it
  386 | 		 * has changed since it was set at the start of the request. If the
  387 | 		 * AuthenticationTrustResolver identifies the current user as anonymous, then the
  388 | 		 * context will not be stored.
  389 | 		 * @param context the context object obtained from the SecurityContextHolder after
  390 | 		 * the request has been processed by the filter chain.
  391 | 		 * SecurityContextHolder.getContext() cannot be used to obtain the context as it
  392 | 		 * has already been cleared by the time this method is called.
  393 | 		 *
  394 | 		 */
  395 | 		@Override
  396 | 		protected void saveContext(SecurityContext context) {
  397 | 			if (isTransient(context)) {
  398 | 				return;
  399 | 			}
  400 | 			final Authentication authentication = context.getAuthentication();
  401 | 			if (isTransient(authentication)) {
  402 | 				return;
  403 | 			}
  404 | 			HttpSession httpSession = this.request.getSession(false);
  405 | 			String springSecurityContextKey = HttpSessionSecurityContextRepository.this.springSecurityContextKey;
  406 | 			// See SEC-776
  407 | 			if (authentication == null
  408 | 					|| HttpSessionSecurityContextRepository.this.trustResolver.isAnonymous(authentication)) {
  409 | 				if (httpSession != null && this.authBeforeExecution != null) {
  410 | 					// SEC-1587 A non-anonymous context may still be in the session
  411 | 					// SEC-1735 remove if the contextBeforeExecution was not anonymous
  412 | 					httpSession.removeAttribute(springSecurityContextKey);
  413 | 					this.isSaveContextInvoked = true;
  414 | 				}
  415 | 				if (this.logger.isDebugEnabled()) {
  416 | 					if (authentication == null) {
  417 | 						this.logger.debug("Did not store empty SecurityContext");
  418 | 					}
  419 | 					else {
  420 | 						this.logger.debug("Did not store anonymous SecurityContext");
  421 | 					}
  422 | 				}
  423 | 				return;
  424 | 			}
  425 | 			httpSession = (httpSession != null) ? httpSession : createNewSessionIfAllowed(context);
  426 | 			// If HttpSession exists, store current SecurityContext but only if it has
  427 | 			// actually changed in this thread (see SEC-37, SEC-1307, SEC-1528)
  428 | 			if (httpSession != null) {
  429 | 				// We may have a new session, so check also whether the context attribute
  430 | 				// is set SEC-1561
  431 | 				if (contextChanged(context) || httpSession.getAttribute(springSecurityContextKey) == null) {
  432 | 					HttpSessionSecurityContextRepository.this.saveContextInHttpSession(context, this.request);
  433 | 					this.isSaveContextInvoked = true;
  434 | 				}
  435 | 			}
  436 | 		}
  437 | 
  438 | 		private boolean contextChanged(SecurityContext context) {
  439 | 			return this.isSaveContextInvoked || context != this.contextBeforeExecution
  440 | 					|| context.getAuthentication() != this.authBeforeExecution;
  441 | 		}
  442 | 
  443 | 		private @Nullable HttpSession createNewSessionIfAllowed(SecurityContext context) {
  444 | 			if (this.httpSessionExistedAtStartOfRequest) {
  445 | 				this.logger.debug("HttpSession is now null, but was not null at start of request; "
  446 | 						+ "session was invalidated, so do not create a new session");
  447 | 				return null;
  448 | 			}
  449 | 			if (!HttpSessionSecurityContextRepository.this.allowSessionCreation) {
  450 | 				this.logger.debug("The HttpSession is currently null, and the "
  451 | 						+ HttpSessionSecurityContextRepository.class.getSimpleName()
  452 | 						+ " is prohibited from creating an HttpSession "
  453 | 						+ "(because the allowSessionCreation property is false) - SecurityContext thus not "
  454 | 						+ "stored for next request");
  455 | 				return null;
  456 | 			}
  457 | 			// Generate a HttpSession only if we need to
  458 | 			if (HttpSessionSecurityContextRepository.this.contextObject.equals(context)) {
  459 | 				this.logger.debug(LogMessage.format(
  460 | 						"HttpSession is null, but SecurityContext has not changed from "
  461 | 								+ "default empty context %s so not creating HttpSession or storing SecurityContext",
  462 | 						context));
  463 | 				return null;
  464 | 			}
  465 | 			try {
  466 | 				HttpSession session = this.request.getSession(true);
  467 | 				this.logger.debug("Created HttpSession as SecurityContext is non-default");
  468 | 				return session;
  469 | 			}
  470 | 			catch (IllegalStateException ex) {
  471 | 				// Response must already be committed, therefore can't create a new
  472 | 				// session
  473 | 				this.logger.warn("Failed to create a session, as response has been committed. "
  474 | 						+ "Unable to store SecurityContext.");
  475 | 			}
  476 | 			return null;
  477 | 		}
  478 | 
  479 | 	}
  480 | 
  481 | }
```
