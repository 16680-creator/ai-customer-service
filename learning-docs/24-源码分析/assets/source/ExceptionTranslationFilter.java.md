<!-- learning-docs source archive（机械抓取原文，禁止手改代码内容） -->
<!-- origin: org.springframework.security:spring-security-web:7.2.0 | path: org/springframework/security/web/access/ExceptionTranslationFilter.java -->
<!-- 正文引用：[[源码:ExceptionTranslationFilter.java#L起-L止]] ；代码块头部：(src: ExceptionTranslationFilter.java#L起-L止) -->

# ExceptionTranslationFilter.java

| 项 | 值 |
| --- | --- |
| 来源 | `org.springframework.security:spring-security-web:7.2.0` |
| 路径 | `org/springframework/security/web/access/ExceptionTranslationFilter.java` |
| 行数 | 281 |
| 在线地址 | https://repo1.maven.org/maven2/org/springframework/security/spring-security-web/7.2.0/spring-security-web-7.2.0-sources.jar |
| 抓取时间 | 2026-11-25T16:27:18.469Z |

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
   17 | package org.springframework.security.web.access;
   18 | 
   19 | import java.io.IOException;
   20 | 
   21 | import jakarta.servlet.FilterChain;
   22 | import jakarta.servlet.ServletException;
   23 | import jakarta.servlet.ServletRequest;
   24 | import jakarta.servlet.ServletResponse;
   25 | import jakarta.servlet.http.HttpServletRequest;
   26 | import jakarta.servlet.http.HttpServletResponse;
   27 | import org.jspecify.annotations.Nullable;
   28 | 
   29 | import org.springframework.context.MessageSource;
   30 | import org.springframework.context.MessageSourceAware;
   31 | import org.springframework.context.support.MessageSourceAccessor;
   32 | import org.springframework.core.log.LogMessage;
   33 | import org.springframework.security.access.AccessDeniedException;
   34 | import org.springframework.security.authentication.AuthenticationTrustResolver;
   35 | import org.springframework.security.authentication.AuthenticationTrustResolverImpl;
   36 | import org.springframework.security.authentication.InsufficientAuthenticationException;
   37 | import org.springframework.security.core.Authentication;
   38 | import org.springframework.security.core.AuthenticationException;
   39 | import org.springframework.security.core.SpringSecurityMessageSource;
   40 | import org.springframework.security.core.context.SecurityContext;
   41 | import org.springframework.security.core.context.SecurityContextHolder;
   42 | import org.springframework.security.core.context.SecurityContextHolderStrategy;
   43 | import org.springframework.security.web.AuthenticationEntryPoint;
   44 | import org.springframework.security.web.savedrequest.HttpSessionRequestCache;
   45 | import org.springframework.security.web.savedrequest.RequestCache;
   46 | import org.springframework.security.web.util.ThrowableAnalyzer;
   47 | import org.springframework.util.Assert;
   48 | import org.springframework.web.filter.GenericFilterBean;
   49 | 
   50 | /**
   51 |  * Handles any <code>AccessDeniedException</code> and <code>AuthenticationException</code>
   52 |  * thrown within the filter chain.
   53 |  * <p>
   54 |  * This filter is necessary because it provides the bridge between Java exceptions and
   55 |  * HTTP responses. It is solely concerned with maintaining the user interface. This filter
   56 |  * does not do any actual security enforcement.
   57 |  * <p>
   58 |  * If an {@link AuthenticationException} is detected, the filter will launch the
   59 |  * <code>authenticationEntryPoint</code>. This allows common handling of authentication
   60 |  * failures originating from Web or Method Security.
   61 |  * <p>
   62 |  * If an {@link AccessDeniedException} is detected, the filter will determine whether or
   63 |  * not the user is an anonymous user. If they are an anonymous user, the
   64 |  * <code>authenticationEntryPoint</code> will be launched. If they are not an anonymous
   65 |  * user, the filter will delegate to the
   66 |  * {@link org.springframework.security.web.access.AccessDeniedHandler}. By default the
   67 |  * filter will use
   68 |  * {@link org.springframework.security.web.access.AccessDeniedHandlerImpl}.
   69 |  * <p>
   70 |  * To use this filter, it is necessary to specify the following properties:
   71 |  * <ul>
   72 |  * <li><code>authenticationEntryPoint</code> indicates the handler that should commence
   73 |  * the authentication process if an <code>AuthenticationException</code> is detected. Note
   74 |  * that this may also switch the current protocol from http to https for an SSL
   75 |  * login.</li>
   76 |  * <li><tt>requestCache</tt> determines the strategy used to save a request during the
   77 |  * authentication process in order that it may be retrieved and reused once the user has
   78 |  * authenticated. The default implementation is {@link HttpSessionRequestCache}.</li>
   79 |  * </ul>
   80 |  *
   81 |  * @author Ben Alex
   82 |  * @author colin sampaleanu
   83 |  */
   84 | public class ExceptionTranslationFilter extends GenericFilterBean implements MessageSourceAware {
   85 | 
   86 | 	private SecurityContextHolderStrategy securityContextHolderStrategy = SecurityContextHolder
   87 | 		.getContextHolderStrategy();
   88 | 
   89 | 	private AccessDeniedHandler accessDeniedHandler = new AccessDeniedHandlerImpl();
   90 | 
   91 | 	private AuthenticationEntryPoint authenticationEntryPoint;
   92 | 
   93 | 	private AuthenticationTrustResolver authenticationTrustResolver = new AuthenticationTrustResolverImpl();
   94 | 
   95 | 	private ThrowableAnalyzer throwableAnalyzer = new DefaultThrowableAnalyzer();
   96 | 
   97 | 	private final RequestCache requestCache;
   98 | 
   99 | 	protected MessageSourceAccessor messages = SpringSecurityMessageSource.getAccessor();
  100 | 
  101 | 	public ExceptionTranslationFilter(AuthenticationEntryPoint authenticationEntryPoint) {
  102 | 		this(authenticationEntryPoint, new HttpSessionRequestCache());
  103 | 	}
  104 | 
  105 | 	public ExceptionTranslationFilter(AuthenticationEntryPoint authenticationEntryPoint, RequestCache requestCache) {
  106 | 		Assert.notNull(authenticationEntryPoint, "authenticationEntryPoint cannot be null");
  107 | 		Assert.notNull(requestCache, "requestCache cannot be null");
  108 | 		this.authenticationEntryPoint = authenticationEntryPoint;
  109 | 		this.requestCache = requestCache;
  110 | 	}
  111 | 
  112 | 	@Override
  113 | 	public void afterPropertiesSet() {
  114 | 		Assert.notNull(this.authenticationEntryPoint, "authenticationEntryPoint must be specified");
  115 | 	}
  116 | 
  117 | 	@Override
  118 | 	public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
  119 | 			throws IOException, ServletException {
  120 | 		doFilter((HttpServletRequest) request, (HttpServletResponse) response, chain);
  121 | 	}
  122 | 
  123 | 	private void doFilter(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
  124 | 			throws IOException, ServletException {
  125 | 		try {
  126 | 			chain.doFilter(request, response);
  127 | 		}
  128 | 		catch (IOException ex) {
  129 | 			throw ex;
  130 | 		}
  131 | 		catch (Exception ex) {
  132 | 			// Try to extract a SpringSecurityException from the stacktrace
  133 | 			Throwable[] causeChain = this.throwableAnalyzer.determineCauseChain(ex);
  134 | 			RuntimeException securityException = (AuthenticationException) this.throwableAnalyzer
  135 | 				.getFirstThrowableOfType(AuthenticationException.class, causeChain);
  136 | 			if (securityException == null) {
  137 | 				securityException = (AccessDeniedException) this.throwableAnalyzer
  138 | 					.getFirstThrowableOfType(AccessDeniedException.class, causeChain);
  139 | 			}
  140 | 			if (securityException == null) {
  141 | 				rethrow(ex);
  142 | 			}
  143 | 			if (response.isCommitted()) {
  144 | 				throw new ServletException("Unable to handle the Spring Security Exception "
  145 | 						+ "because the response is already committed.", ex);
  146 | 			}
  147 | 			handleSpringSecurityException(request, response, chain, securityException);
  148 | 		}
  149 | 	}
  150 | 
  151 | 	private void rethrow(Exception ex) throws ServletException {
  152 | 		// Rethrow ServletExceptions and RuntimeExceptions as-is
  153 | 		if (ex instanceof ServletException) {
  154 | 			throw (ServletException) ex;
  155 | 		}
  156 | 		if (ex instanceof RuntimeException) {
  157 | 			throw (RuntimeException) ex;
  158 | 		}
  159 | 		// Wrap other Exceptions. This shouldn't actually happen
  160 | 		// as we've already covered all the possibilities for doFilter
  161 | 		throw new RuntimeException(ex);
  162 | 	}
  163 | 
  164 | 	public AuthenticationEntryPoint getAuthenticationEntryPoint() {
  165 | 		return this.authenticationEntryPoint;
  166 | 	}
  167 | 
  168 | 	protected AuthenticationTrustResolver getAuthenticationTrustResolver() {
  169 | 		return this.authenticationTrustResolver;
  170 | 	}
  171 | 
  172 | 	private void handleSpringSecurityException(HttpServletRequest request, HttpServletResponse response,
  173 | 			FilterChain chain, @Nullable RuntimeException exception) throws IOException, ServletException {
  174 | 		if (exception instanceof AuthenticationException) {
  175 | 			handleAuthenticationException(request, response, chain, (AuthenticationException) exception);
  176 | 		}
  177 | 		else if (exception instanceof AccessDeniedException) {
  178 | 			handleAccessDeniedException(request, response, chain, (AccessDeniedException) exception);
  179 | 		}
  180 | 	}
  181 | 
  182 | 	private void handleAuthenticationException(HttpServletRequest request, HttpServletResponse response,
  183 | 			FilterChain chain, AuthenticationException exception) throws ServletException, IOException {
  184 | 		this.logger.trace("Sending to authentication entry point since authentication failed", exception);
  185 | 		sendStartAuthentication(request, response, chain, exception);
  186 | 	}
  187 | 
  188 | 	private void handleAccessDeniedException(HttpServletRequest request, HttpServletResponse response,
  189 | 			FilterChain chain, AccessDeniedException exception) throws ServletException, IOException {
  190 | 		Authentication authentication = this.securityContextHolderStrategy.getContext().getAuthentication();
  191 | 		boolean isAnonymous = this.authenticationTrustResolver.isAnonymous(authentication);
  192 | 		if (isAnonymous || this.authenticationTrustResolver.isRememberMe(authentication)) {
  193 | 			if (logger.isTraceEnabled()) {
  194 | 				logger.trace(LogMessage.format("Sending %s to authentication entry point since access is denied",
  195 | 						authentication), exception);
  196 | 			}
  197 | 			AuthenticationException ex = new InsufficientAuthenticationException(
  198 | 					this.messages.getMessage("ExceptionTranslationFilter.insufficientAuthentication",
  199 | 							"Full authentication is required to access this resource"),
  200 | 					exception);
  201 | 			ex.setAuthenticationRequest(authentication);
  202 | 			sendStartAuthentication(request, response, chain, ex);
  203 | 		}
  204 | 		else {
  205 | 			if (logger.isTraceEnabled()) {
  206 | 				logger.trace(
  207 | 						LogMessage.format("Sending %s to access denied handler since access is denied", authentication),
  208 | 						exception);
  209 | 			}
  210 | 			this.accessDeniedHandler.handle(request, response, exception);
  211 | 		}
  212 | 	}
  213 | 
  214 | 	protected void sendStartAuthentication(HttpServletRequest request, HttpServletResponse response, FilterChain chain,
  215 | 			AuthenticationException reason) throws ServletException, IOException {
  216 | 		// SEC-112: Clear the SecurityContextHolder's Authentication, as the
  217 | 		// existing Authentication is no longer considered valid
  218 | 		SecurityContext context = this.securityContextHolderStrategy.createEmptyContext();
  219 | 		this.securityContextHolderStrategy.setContext(context);
  220 | 		this.requestCache.saveRequest(request, response);
  221 | 		this.authenticationEntryPoint.commence(request, response, reason);
  222 | 	}
  223 | 
  224 | 	public void setAccessDeniedHandler(AccessDeniedHandler accessDeniedHandler) {
  225 | 		Assert.notNull(accessDeniedHandler, "AccessDeniedHandler required");
  226 | 		this.accessDeniedHandler = accessDeniedHandler;
  227 | 	}
  228 | 
  229 | 	public void setAuthenticationTrustResolver(AuthenticationTrustResolver authenticationTrustResolver) {
  230 | 		Assert.notNull(authenticationTrustResolver, "authenticationTrustResolver must not be null");
  231 | 		this.authenticationTrustResolver = authenticationTrustResolver;
  232 | 	}
  233 | 
  234 | 	public void setThrowableAnalyzer(ThrowableAnalyzer throwableAnalyzer) {
  235 | 		Assert.notNull(throwableAnalyzer, "throwableAnalyzer must not be null");
  236 | 		this.throwableAnalyzer = throwableAnalyzer;
  237 | 	}
  238 | 
  239 | 	/**
  240 | 	 * Sets the {@link MessageSource} used to resolve exception messages.
  241 | 	 * @since 5.5
  242 | 	 */
  243 | 	@Override
  244 | 	public void setMessageSource(MessageSource messageSource) {
  245 | 		Assert.notNull(messageSource, "messageSource cannot be null");
  246 | 		this.messages = new MessageSourceAccessor(messageSource);
  247 | 	}
  248 | 
  249 | 	/**
  250 | 	 * Sets the {@link SecurityContextHolderStrategy} to use. The default action is to use
  251 | 	 * the {@link SecurityContextHolderStrategy} stored in {@link SecurityContextHolder}.
  252 | 	 *
  253 | 	 * @since 5.8
  254 | 	 */
  255 | 	public void setSecurityContextHolderStrategy(SecurityContextHolderStrategy securityContextHolderStrategy) {
  256 | 		Assert.notNull(securityContextHolderStrategy, "securityContextHolderStrategy cannot be null");
  257 | 		this.securityContextHolderStrategy = securityContextHolderStrategy;
  258 | 	}
  259 | 
  260 | 	/**
  261 | 	 * Default implementation of <code>ThrowableAnalyzer</code> which is capable of also
  262 | 	 * unwrapping <code>ServletException</code>s.
  263 | 	 */
  264 | 	private static final class DefaultThrowableAnalyzer extends ThrowableAnalyzer {
  265 | 
  266 | 		/**
  267 | 		 * Registers extractors for unwrapping {@code ServletException}s.
  268 | 		 * @see org.springframework.security.web.util.ThrowableAnalyzer#initExtractorMap()
  269 | 		 */
  270 | 		@Override
  271 | 		protected void initExtractorMap() {
  272 | 			super.initExtractorMap();
  273 | 			registerExtractor(ServletException.class, (throwable) -> {
  274 | 				ThrowableAnalyzer.verifyThrowableHierarchy(throwable, ServletException.class);
  275 | 				return ((ServletException) throwable).getRootCause();
  276 | 			});
  277 | 		}
  278 | 
  279 | 	}
  280 | 
  281 | }
```
