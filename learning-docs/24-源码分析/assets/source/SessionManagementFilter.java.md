<!-- learning-docs source archive（机械抓取原文，禁止手改代码内容） -->
<!-- origin: org.springframework.security:spring-security-web:7.2.0 | path: org/springframework/security/web/session/SessionManagementFilter.java -->
<!-- 正文引用：[[源码:SessionManagementFilter.java#L起-L止]] ；代码块头部：(src: SessionManagementFilter.java#L起-L止) -->

# SessionManagementFilter.java

| 项 | 值 |
| --- | --- |
| 来源 | `org.springframework.security:spring-security-web:7.2.0` |
| 路径 | `org/springframework/security/web/session/SessionManagementFilter.java` |
| 行数 | 180 |
| 在线地址 | https://repo1.maven.org/maven2/org/springframework/security/spring-security-web/7.2.0/spring-security-web-7.2.0-sources.jar |
| 抓取时间 | 2026-11-25T16:40:15.514Z |

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
   17 | package org.springframework.security.web.session;
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
   29 | import org.springframework.core.log.LogMessage;
   30 | import org.springframework.security.authentication.AuthenticationTrustResolver;
   31 | import org.springframework.security.authentication.AuthenticationTrustResolverImpl;
   32 | import org.springframework.security.core.Authentication;
   33 | import org.springframework.security.core.context.SecurityContextHolder;
   34 | import org.springframework.security.core.context.SecurityContextHolderStrategy;
   35 | import org.springframework.security.web.authentication.AuthenticationFailureHandler;
   36 | import org.springframework.security.web.authentication.SimpleUrlAuthenticationFailureHandler;
   37 | import org.springframework.security.web.authentication.session.SessionAuthenticationException;
   38 | import org.springframework.security.web.authentication.session.SessionAuthenticationStrategy;
   39 | import org.springframework.security.web.authentication.session.SessionFixationProtectionStrategy;
   40 | import org.springframework.security.web.context.SecurityContextRepository;
   41 | import org.springframework.util.Assert;
   42 | import org.springframework.web.filter.GenericFilterBean;
   43 | 
   44 | /**
   45 |  * Detects that a user has been authenticated since the start of the request and, if they
   46 |  * have, calls the configured {@link SessionAuthenticationStrategy} to perform any
   47 |  * session-related activity such as activating session-fixation protection mechanisms or
   48 |  * checking for multiple concurrent logins.
   49 |  *
   50 |  * @author Martin Algesten
   51 |  * @author Luke Taylor
   52 |  * @since 2.0
   53 |  */
   54 | public class SessionManagementFilter extends GenericFilterBean {
   55 | 
   56 | 	static final String FILTER_APPLIED = "__spring_security_session_mgmt_filter_applied";
   57 | 
   58 | 	private SecurityContextHolderStrategy securityContextHolderStrategy = SecurityContextHolder
   59 | 		.getContextHolderStrategy();
   60 | 
   61 | 	private final SecurityContextRepository securityContextRepository;
   62 | 
   63 | 	private SessionAuthenticationStrategy sessionAuthenticationStrategy;
   64 | 
   65 | 	private AuthenticationTrustResolver trustResolver = new AuthenticationTrustResolverImpl();
   66 | 
   67 | 	private @Nullable InvalidSessionStrategy invalidSessionStrategy = null;
   68 | 
   69 | 	private AuthenticationFailureHandler failureHandler = new SimpleUrlAuthenticationFailureHandler();
   70 | 
   71 | 	public SessionManagementFilter(SecurityContextRepository securityContextRepository) {
   72 | 		this(securityContextRepository, new SessionFixationProtectionStrategy());
   73 | 	}
   74 | 
   75 | 	public SessionManagementFilter(SecurityContextRepository securityContextRepository,
   76 | 			SessionAuthenticationStrategy sessionStrategy) {
   77 | 		Assert.notNull(securityContextRepository, "SecurityContextRepository cannot be null");
   78 | 		Assert.notNull(sessionStrategy, "SessionAuthenticationStrategy cannot be null");
   79 | 		this.securityContextRepository = securityContextRepository;
   80 | 		this.sessionAuthenticationStrategy = sessionStrategy;
   81 | 	}
   82 | 
   83 | 	@Override
   84 | 	public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
   85 | 			throws IOException, ServletException {
   86 | 		doFilter((HttpServletRequest) request, (HttpServletResponse) response, chain);
   87 | 	}
   88 | 
   89 | 	private void doFilter(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
   90 | 			throws IOException, ServletException {
   91 | 		if (request.getAttribute(FILTER_APPLIED) != null) {
   92 | 			chain.doFilter(request, response);
   93 | 			return;
   94 | 		}
   95 | 		request.setAttribute(FILTER_APPLIED, Boolean.TRUE);
   96 | 		if (!this.securityContextRepository.containsContext(request)) {
   97 | 			Authentication authentication = this.securityContextHolderStrategy.getContext().getAuthentication();
   98 | 			if (this.trustResolver.isAuthenticated(authentication)) {
   99 | 				// The user has been authenticated during the current request, so call the
  100 | 				// session strategy
  101 | 				try {
  102 | 					this.sessionAuthenticationStrategy.onAuthentication(authentication, request, response);
  103 | 				}
  104 | 				catch (SessionAuthenticationException ex) {
  105 | 					// The session strategy can reject the authentication
  106 | 					this.logger.debug("SessionAuthenticationStrategy rejected the authentication object", ex);
  107 | 					this.securityContextHolderStrategy.clearContext();
  108 | 					this.failureHandler.onAuthenticationFailure(request, response, ex);
  109 | 					return;
  110 | 				}
  111 | 				// Eagerly save the security context to make it available for any possible
  112 | 				// re-entrant requests which may occur before the current request
  113 | 				// completes. SEC-1396.
  114 | 				this.securityContextRepository.saveContext(this.securityContextHolderStrategy.getContext(), request,
  115 | 						response);
  116 | 			}
  117 | 			else {
  118 | 				// No security context or authentication present. Check for a session
  119 | 				// timeout
  120 | 				if (request.getRequestedSessionId() != null && !request.isRequestedSessionIdValid()) {
  121 | 					if (this.logger.isDebugEnabled()) {
  122 | 						this.logger.debug(LogMessage.format("Request requested invalid session id %s",
  123 | 								request.getRequestedSessionId()));
  124 | 					}
  125 | 					if (this.invalidSessionStrategy != null) {
  126 | 						this.invalidSessionStrategy.onInvalidSessionDetected(request, response);
  127 | 						return;
  128 | 					}
  129 | 				}
  130 | 			}
  131 | 		}
  132 | 		chain.doFilter(request, response);
  133 | 	}
  134 | 
  135 | 	/**
  136 | 	 * Sets the strategy which will be invoked instead of allowing the filter chain to
  137 | 	 * proceed, if the user agent requests an invalid session ID. If the property is not
  138 | 	 * set, no action will be taken.
  139 | 	 * @param invalidSessionStrategy the strategy to invoke. Typically a
  140 | 	 * {@link SimpleRedirectInvalidSessionStrategy}.
  141 | 	 */
  142 | 	public void setInvalidSessionStrategy(InvalidSessionStrategy invalidSessionStrategy) {
  143 | 		this.invalidSessionStrategy = invalidSessionStrategy;
  144 | 	}
  145 | 
  146 | 	/**
  147 | 	 * The handler which will be invoked if the <tt>AuthenticatedSessionStrategy</tt>
  148 | 	 * raises a <tt>SessionAuthenticationException</tt>, indicating that the user is not
  149 | 	 * allowed to be authenticated for this session (typically because they already have
  150 | 	 * too many sessions open).
  151 | 	 *
  152 | 	 */
  153 | 	public void setAuthenticationFailureHandler(AuthenticationFailureHandler failureHandler) {
  154 | 		Assert.notNull(failureHandler, "failureHandler cannot be null");
  155 | 		this.failureHandler = failureHandler;
  156 | 	}
  157 | 
  158 | 	/**
  159 | 	 * Sets the {@link AuthenticationTrustResolver} to be used. The default is
  160 | 	 * {@link AuthenticationTrustResolverImpl}.
  161 | 	 * @param trustResolver the {@link AuthenticationTrustResolver} to use. Cannot be
  162 | 	 * null.
  163 | 	 */
  164 | 	public void setTrustResolver(AuthenticationTrustResolver trustResolver) {
  165 | 		Assert.notNull(trustResolver, "trustResolver cannot be null");
  166 | 		this.trustResolver = trustResolver;
  167 | 	}
  168 | 
  169 | 	/**
  170 | 	 * Sets the {@link SecurityContextHolderStrategy} to use. The default action is to use
  171 | 	 * the {@link SecurityContextHolderStrategy} stored in {@link SecurityContextHolder}.
  172 | 	 *
  173 | 	 * @since 5.8
  174 | 	 */
  175 | 	public void setSecurityContextHolderStrategy(SecurityContextHolderStrategy securityContextHolderStrategy) {
  176 | 		Assert.notNull(securityContextHolderStrategy, "securityContextHolderStrategy cannot be null");
  177 | 		this.securityContextHolderStrategy = securityContextHolderStrategy;
  178 | 	}
  179 | 
  180 | }
```
