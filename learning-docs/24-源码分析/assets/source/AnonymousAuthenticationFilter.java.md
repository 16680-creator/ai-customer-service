<!-- learning-docs source archive（机械抓取原文，禁止手改代码内容） -->
<!-- origin: org.springframework.security:spring-security-web:7.2.0 | path: org/springframework/security/web/authentication/AnonymousAuthenticationFilter.java -->
<!-- 正文引用：[[源码:AnonymousAuthenticationFilter.java#L起-L止]] ；代码块头部：(src: AnonymousAuthenticationFilter.java#L起-L止) -->

# AnonymousAuthenticationFilter.java

| 项 | 值 |
| --- | --- |
| 来源 | `org.springframework.security:spring-security-web:7.2.0` |
| 路径 | `org/springframework/security/web/authentication/AnonymousAuthenticationFilter.java` |
| 行数 | 167 |
| 在线地址 | https://repo1.maven.org/maven2/org/springframework/security/spring-security-web/7.2.0/spring-security-web-7.2.0-sources.jar |
| 抓取时间 | 2026-11-25T16:27:27.757Z |

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
   20 | import java.util.List;
   21 | import java.util.function.Supplier;
   22 | 
   23 | import jakarta.servlet.FilterChain;
   24 | import jakarta.servlet.ServletException;
   25 | import jakarta.servlet.ServletRequest;
   26 | import jakarta.servlet.ServletResponse;
   27 | import jakarta.servlet.http.HttpServletRequest;
   28 | 
   29 | import org.springframework.beans.factory.InitializingBean;
   30 | import org.springframework.core.log.LogMessage;
   31 | import org.springframework.security.authentication.AnonymousAuthenticationToken;
   32 | import org.springframework.security.authentication.AuthenticationDetailsSource;
   33 | import org.springframework.security.core.Authentication;
   34 | import org.springframework.security.core.GrantedAuthority;
   35 | import org.springframework.security.core.authority.AuthorityUtils;
   36 | import org.springframework.security.core.context.SecurityContext;
   37 | import org.springframework.security.core.context.SecurityContextHolder;
   38 | import org.springframework.security.core.context.SecurityContextHolderStrategy;
   39 | import org.springframework.util.Assert;
   40 | import org.springframework.util.function.SingletonSupplier;
   41 | import org.springframework.web.filter.GenericFilterBean;
   42 | 
   43 | /**
   44 |  * Detects if there is no {@code Authentication} object in the
   45 |  * {@code SecurityContextHolder}, and populates it with one if needed.
   46 |  *
   47 |  * @author Ben Alex
   48 |  * @author Luke Taylor
   49 |  * @author Evgeniy Cheban
   50 |  */
   51 | public class AnonymousAuthenticationFilter extends GenericFilterBean implements InitializingBean {
   52 | 
   53 | 	private SecurityContextHolderStrategy securityContextHolderStrategy = SecurityContextHolder
   54 | 		.getContextHolderStrategy();
   55 | 
   56 | 	private AuthenticationDetailsSource<HttpServletRequest, ?> authenticationDetailsSource = new WebAuthenticationDetailsSource();
   57 | 
   58 | 	private String key;
   59 | 
   60 | 	private Object principal;
   61 | 
   62 | 	private List<GrantedAuthority> authorities;
   63 | 
   64 | 	/**
   65 | 	 * Creates a filter with a principal named "anonymousUser" and the single authority
   66 | 	 * "ROLE_ANONYMOUS".
   67 | 	 * @param key the key to identify tokens created by this filter
   68 | 	 */
   69 | 	public AnonymousAuthenticationFilter(String key) {
   70 | 		this(key, "anonymousUser", AuthorityUtils.createAuthorityList("ROLE_ANONYMOUS"));
   71 | 	}
   72 | 
   73 | 	/**
   74 | 	 * Creates a filter with the given principal and authorities.
   75 | 	 * @param key key the key to identify tokens created by this filter
   76 | 	 * @param principal the principal which will be used to represent anonymous users
   77 | 	 * @param authorities the authority list for anonymous users
   78 | 	 */
   79 | 	public AnonymousAuthenticationFilter(String key, Object principal, List<GrantedAuthority> authorities) {
   80 | 		Assert.hasLength(key, "key cannot be null or empty");
   81 | 		Assert.notNull(principal, "Anonymous authentication principal must be set");
   82 | 		Assert.notNull(authorities, "Anonymous authorities must be set");
   83 | 		this.key = key;
   84 | 		this.principal = principal;
   85 | 		this.authorities = authorities;
   86 | 	}
   87 | 
   88 | 	@Override
   89 | 	public void afterPropertiesSet() {
   90 | 		Assert.hasLength(this.key, "key must have length");
   91 | 		Assert.notNull(this.principal, "Anonymous authentication principal must be set");
   92 | 		Assert.notNull(this.authorities, "Anonymous authorities must be set");
   93 | 	}
   94 | 
   95 | 	@Override
   96 | 	public void doFilter(ServletRequest req, ServletResponse res, FilterChain chain)
   97 | 			throws IOException, ServletException {
   98 | 		Supplier<SecurityContext> deferredContext = this.securityContextHolderStrategy.getDeferredContext();
   99 | 		this.securityContextHolderStrategy
  100 | 			.setDeferredContext(defaultWithAnonymous((HttpServletRequest) req, deferredContext));
  101 | 		chain.doFilter(req, res);
  102 | 	}
  103 | 
  104 | 	private Supplier<SecurityContext> defaultWithAnonymous(HttpServletRequest request,
  105 | 			Supplier<SecurityContext> currentDeferredContext) {
  106 | 		return SingletonSupplier.of(() -> {
  107 | 			SecurityContext currentContext = currentDeferredContext.get();
  108 | 			return defaultWithAnonymous(request, currentContext);
  109 | 		});
  110 | 	}
  111 | 
  112 | 	private SecurityContext defaultWithAnonymous(HttpServletRequest request, SecurityContext currentContext) {
  113 | 		Authentication currentAuthentication = currentContext.getAuthentication();
  114 | 		if (currentAuthentication == null) {
  115 | 			Authentication anonymous = createAuthentication(request);
  116 | 			if (this.logger.isTraceEnabled()) {
  117 | 				this.logger.trace(LogMessage.of(() -> "Set SecurityContextHolder to " + anonymous));
  118 | 			}
  119 | 			else {
  120 | 				this.logger.debug("Set SecurityContextHolder to anonymous SecurityContext");
  121 | 			}
  122 | 			SecurityContext anonymousContext = this.securityContextHolderStrategy.createEmptyContext();
  123 | 			anonymousContext.setAuthentication(anonymous);
  124 | 			return anonymousContext;
  125 | 		}
  126 | 		else {
  127 | 			if (this.logger.isTraceEnabled()) {
  128 | 				this.logger.trace(LogMessage.of(() -> "Did not set SecurityContextHolder since already authenticated "
  129 | 						+ currentAuthentication));
  130 | 			}
  131 | 		}
  132 | 		return currentContext;
  133 | 	}
  134 | 
  135 | 	protected Authentication createAuthentication(HttpServletRequest request) {
  136 | 		AnonymousAuthenticationToken token = new AnonymousAuthenticationToken(this.key, this.principal,
  137 | 				this.authorities);
  138 | 		token.setDetails(this.authenticationDetailsSource.buildDetails(request));
  139 | 		return token;
  140 | 	}
  141 | 
  142 | 	public void setAuthenticationDetailsSource(
  143 | 			AuthenticationDetailsSource<HttpServletRequest, ?> authenticationDetailsSource) {
  144 | 		Assert.notNull(authenticationDetailsSource, "AuthenticationDetailsSource required");
  145 | 		this.authenticationDetailsSource = authenticationDetailsSource;
  146 | 	}
  147 | 
  148 | 	/**
  149 | 	 * Sets the {@link SecurityContextHolderStrategy} to use. The default action is to use
  150 | 	 * the {@link SecurityContextHolderStrategy} stored in {@link SecurityContextHolder}.
  151 | 	 *
  152 | 	 * @since 5.8
  153 | 	 */
  154 | 	public void setSecurityContextHolderStrategy(SecurityContextHolderStrategy securityContextHolderStrategy) {
  155 | 		Assert.notNull(securityContextHolderStrategy, "securityContextHolderStrategy cannot be null");
  156 | 		this.securityContextHolderStrategy = securityContextHolderStrategy;
  157 | 	}
  158 | 
  159 | 	public Object getPrincipal() {
  160 | 		return this.principal;
  161 | 	}
  162 | 
  163 | 	public List<GrantedAuthority> getAuthorities() {
  164 | 		return this.authorities;
  165 | 	}
  166 | 
  167 | }
```
