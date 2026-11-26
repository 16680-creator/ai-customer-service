<!-- learning-docs source archive（机械抓取原文，禁止手改代码内容） -->
<!-- origin: org.springframework.security:spring-security-web:7.2.0 | path: org/springframework/security/web/access/intercept/AuthorizationFilter.java -->
<!-- 正文引用：[[源码:AuthorizationFilter.java#L起-L止]] ；代码块头部：(src: AuthorizationFilter.java#L起-L止) -->

# AuthorizationFilter.java

| 项 | 值 |
| --- | --- |
| 来源 | `org.springframework.security:spring-security-web:7.2.0` |
| 路径 | `org/springframework/security/web/access/intercept/AuthorizationFilter.java` |
| 行数 | 211 |
| 在线地址 | https://repo1.maven.org/maven2/org/springframework/security/spring-security-web/7.2.0/spring-security-web-7.2.0-sources.jar |
| 抓取时间 | 2026-11-25T16:27:18.338Z |

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
   17 | package org.springframework.security.web.access.intercept;
   18 | 
   19 | import java.io.IOException;
   20 | import java.util.function.Supplier;
   21 | 
   22 | import jakarta.servlet.DispatcherType;
   23 | import jakarta.servlet.FilterChain;
   24 | import jakarta.servlet.ServletException;
   25 | import jakarta.servlet.ServletRequest;
   26 | import jakarta.servlet.ServletResponse;
   27 | import jakarta.servlet.http.HttpServletRequest;
   28 | import jakarta.servlet.http.HttpServletResponse;
   29 | import org.jspecify.annotations.Nullable;
   30 | 
   31 | import org.springframework.context.ApplicationEventPublisher;
   32 | import org.springframework.security.authentication.AuthenticationCredentialsNotFoundException;
   33 | import org.springframework.security.authorization.AuthorizationDeniedException;
   34 | import org.springframework.security.authorization.AuthorizationEventPublisher;
   35 | import org.springframework.security.authorization.AuthorizationManager;
   36 | import org.springframework.security.authorization.AuthorizationResult;
   37 | import org.springframework.security.authorization.event.AuthorizationDeniedEvent;
   38 | import org.springframework.security.authorization.event.AuthorizationGrantedEvent;
   39 | import org.springframework.security.core.Authentication;
   40 | import org.springframework.security.core.context.SecurityContextHolder;
   41 | import org.springframework.security.core.context.SecurityContextHolderStrategy;
   42 | import org.springframework.util.Assert;
   43 | import org.springframework.web.filter.GenericFilterBean;
   44 | 
   45 | /**
   46 |  * An authorization filter that restricts access to the URL using
   47 |  * {@link AuthorizationManager}.
   48 |  *
   49 |  * @author Evgeniy Cheban
   50 |  * @since 5.5
   51 |  */
   52 | public class AuthorizationFilter extends GenericFilterBean {
   53 | 
   54 | 	private SecurityContextHolderStrategy securityContextHolderStrategy = SecurityContextHolder
   55 | 		.getContextHolderStrategy();
   56 | 
   57 | 	private final AuthorizationManager<HttpServletRequest> authorizationManager;
   58 | 
   59 | 	private AuthorizationEventPublisher eventPublisher = new NoopAuthorizationEventPublisher();
   60 | 
   61 | 	private boolean observeOncePerRequest = false;
   62 | 
   63 | 	private boolean filterErrorDispatch = true;
   64 | 
   65 | 	private boolean filterAsyncDispatch = true;
   66 | 
   67 | 	/**
   68 | 	 * Creates an instance.
   69 | 	 * @param authorizationManager the {@link AuthorizationManager} to use
   70 | 	 */
   71 | 	public AuthorizationFilter(AuthorizationManager<HttpServletRequest> authorizationManager) {
   72 | 		Assert.notNull(authorizationManager, "authorizationManager cannot be null");
   73 | 		this.authorizationManager = authorizationManager;
   74 | 	}
   75 | 
   76 | 	@Override
   77 | 	public void doFilter(ServletRequest servletRequest, ServletResponse servletResponse, FilterChain chain)
   78 | 			throws ServletException, IOException {
   79 | 
   80 | 		HttpServletRequest request = (HttpServletRequest) servletRequest;
   81 | 		HttpServletResponse response = (HttpServletResponse) servletResponse;
   82 | 
   83 | 		if (this.observeOncePerRequest && isApplied(request)) {
   84 | 			chain.doFilter(request, response);
   85 | 			return;
   86 | 		}
   87 | 
   88 | 		if (skipDispatch(request)) {
   89 | 			chain.doFilter(request, response);
   90 | 			return;
   91 | 		}
   92 | 
   93 | 		String alreadyFilteredAttributeName = getAlreadyFilteredAttributeName();
   94 | 		request.setAttribute(alreadyFilteredAttributeName, Boolean.TRUE);
   95 | 		try {
   96 | 			AuthorizationResult result = this.authorizationManager.authorize(this::getAuthentication, request);
   97 | 			this.eventPublisher.publishAuthorizationEvent(this::getAuthentication, request, result);
   98 | 			if (result != null && !result.isGranted()) {
   99 | 				throw new AuthorizationDeniedException("Access Denied", result);
  100 | 			}
  101 | 			chain.doFilter(request, response);
  102 | 		}
  103 | 		finally {
  104 | 			request.removeAttribute(alreadyFilteredAttributeName);
  105 | 		}
  106 | 	}
  107 | 
  108 | 	private boolean skipDispatch(HttpServletRequest request) {
  109 | 		if (DispatcherType.ERROR.equals(request.getDispatcherType()) && !this.filterErrorDispatch) {
  110 | 			return true;
  111 | 		}
  112 | 
  113 | 		return DispatcherType.ASYNC.equals(request.getDispatcherType()) && !this.filterAsyncDispatch;
  114 | 	}
  115 | 
  116 | 	private boolean isApplied(HttpServletRequest request) {
  117 | 		return request.getAttribute(getAlreadyFilteredAttributeName()) != null;
  118 | 	}
  119 | 
  120 | 	private String getAlreadyFilteredAttributeName() {
  121 | 		String name = getFilterName();
  122 | 		if (name == null) {
  123 | 			name = getClass().getName();
  124 | 		}
  125 | 		return name + ".APPLIED";
  126 | 	}
  127 | 
  128 | 	/**
  129 | 	 * Sets the {@link SecurityContextHolderStrategy} to use. The default action is to use
  130 | 	 * the {@link SecurityContextHolderStrategy} stored in {@link SecurityContextHolder}.
  131 | 	 *
  132 | 	 * @since 5.8
  133 | 	 */
  134 | 	public void setSecurityContextHolderStrategy(SecurityContextHolderStrategy securityContextHolderStrategy) {
  135 | 		Assert.notNull(securityContextHolderStrategy, "securityContextHolderStrategy cannot be null");
  136 | 		this.securityContextHolderStrategy = securityContextHolderStrategy;
  137 | 	}
  138 | 
  139 | 	private Authentication getAuthentication() {
  140 | 		Authentication authentication = this.securityContextHolderStrategy.getContext().getAuthentication();
  141 | 		if (authentication == null) {
  142 | 			throw new AuthenticationCredentialsNotFoundException(
  143 | 					"An Authentication object was not found in the SecurityContext");
  144 | 		}
  145 | 		return authentication;
  146 | 	}
  147 | 
  148 | 	/**
  149 | 	 * Use this {@link AuthorizationEventPublisher} to publish
  150 | 	 * {@link AuthorizationDeniedEvent}s and {@link AuthorizationGrantedEvent}s.
  151 | 	 * @param eventPublisher the {@link ApplicationEventPublisher} to use
  152 | 	 * @since 5.7
  153 | 	 */
  154 | 	public void setAuthorizationEventPublisher(AuthorizationEventPublisher eventPublisher) {
  155 | 		Assert.notNull(eventPublisher, "eventPublisher cannot be null");
  156 | 		this.eventPublisher = eventPublisher;
  157 | 	}
  158 | 
  159 | 	/**
  160 | 	 * Gets the {@link AuthorizationManager} used by this filter.
  161 | 	 * @return the {@link AuthorizationManager}
  162 | 	 */
  163 | 	public AuthorizationManager<HttpServletRequest> getAuthorizationManager() {
  164 | 		return this.authorizationManager;
  165 | 	}
  166 | 
  167 | 	public boolean isObserveOncePerRequest() {
  168 | 		return this.observeOncePerRequest;
  169 | 	}
  170 | 
  171 | 	/**
  172 | 	 * Sets whether this filter apply only once per request. By default, this is
  173 | 	 * <code>false</code>, meaning the filter will execute on every request. Sometimes
  174 | 	 * users may wish it to execute more than once per request, such as when JSP forwards
  175 | 	 * are being used and filter security is desired on each included fragment of the HTTP
  176 | 	 * request.
  177 | 	 * @param observeOncePerRequest whether the filter should only be applied once per
  178 | 	 * request
  179 | 	 */
  180 | 	public void setObserveOncePerRequest(boolean observeOncePerRequest) {
  181 | 		this.observeOncePerRequest = observeOncePerRequest;
  182 | 	}
  183 | 
  184 | 	/**
  185 | 	 * If set to true, the filter will be applied to error dispatcher. Defaults to
  186 | 	 * {@code true}.
  187 | 	 * @param filterErrorDispatch whether the filter should be applied to error dispatcher
  188 | 	 */
  189 | 	public void setFilterErrorDispatch(boolean filterErrorDispatch) {
  190 | 		this.filterErrorDispatch = filterErrorDispatch;
  191 | 	}
  192 | 
  193 | 	/**
  194 | 	 * If set to true, the filter will be applied to the async dispatcher. Defaults to
  195 | 	 * {@code true}.
  196 | 	 * @param filterAsyncDispatch whether the filter should be applied to async dispatch
  197 | 	 */
  198 | 	public void setFilterAsyncDispatch(boolean filterAsyncDispatch) {
  199 | 		this.filterAsyncDispatch = filterAsyncDispatch;
  200 | 	}
  201 | 
  202 | 	private static class NoopAuthorizationEventPublisher implements AuthorizationEventPublisher {
  203 | 
  204 | 		@Override
  205 | 		public <T> void publishAuthorizationEvent(Supplier<Authentication> authentication, T object,
  206 | 				@Nullable AuthorizationResult result) {
  207 | 		}
  208 | 
  209 | 	}
  210 | 
  211 | }
```
