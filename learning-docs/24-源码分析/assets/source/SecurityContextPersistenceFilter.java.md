<!-- learning-docs source archive（机械抓取原文，禁止手改代码内容） -->
<!-- origin: org.springframework.security:spring-security-web:7.2.0 | path: org/springframework/security/web/context/SecurityContextPersistenceFilter.java -->
<!-- 正文引用：[[源码:SecurityContextPersistenceFilter.java#L起-L止]] ；代码块头部：(src: SecurityContextPersistenceFilter.java#L起-L止) -->

# SecurityContextPersistenceFilter.java

| 项 | 值 |
| --- | --- |
| 来源 | `org.springframework.security:spring-security-web:7.2.0` |
| 路径 | `org/springframework/security/web/context/SecurityContextPersistenceFilter.java` |
| 行数 | 144 |
| 在线地址 | https://repo1.maven.org/maven2/org/springframework/security/spring-security-web/7.2.0/spring-security-web-7.2.0-sources.jar |
| 抓取时间 | 2026-11-25T16:27:28.195Z |

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
   19 | import java.io.IOException;
   20 | 
   21 | import jakarta.servlet.FilterChain;
   22 | import jakarta.servlet.ServletException;
   23 | import jakarta.servlet.ServletRequest;
   24 | import jakarta.servlet.ServletResponse;
   25 | import jakarta.servlet.http.HttpServletRequest;
   26 | import jakarta.servlet.http.HttpServletResponse;
   27 | import jakarta.servlet.http.HttpSession;
   28 | 
   29 | import org.springframework.core.log.LogMessage;
   30 | import org.springframework.security.core.context.SecurityContext;
   31 | import org.springframework.security.core.context.SecurityContextHolder;
   32 | import org.springframework.security.core.context.SecurityContextHolderStrategy;
   33 | import org.springframework.util.Assert;
   34 | import org.springframework.web.filter.GenericFilterBean;
   35 | 
   36 | /**
   37 |  * Populates the {@link SecurityContextHolder} with information obtained from the
   38 |  * configured {@link SecurityContextRepository} prior to the request and stores it back in
   39 |  * the repository once the request has completed and clearing the context holder. By
   40 |  * default it uses an {@link HttpSessionSecurityContextRepository}. See this class for
   41 |  * information <tt>HttpSession</tt> related configuration options.
   42 |  * <p>
   43 |  * This filter will only execute once per request, to resolve servlet container
   44 |  * (specifically Weblogic) incompatibilities.
   45 |  * <p>
   46 |  * This filter MUST be executed BEFORE any authentication processing mechanisms.
   47 |  * Authentication processing mechanisms (e.g. BASIC, CAS processing filters etc) expect
   48 |  * the <code>SecurityContextHolder</code> to contain a valid <code>SecurityContext</code>
   49 |  * by the time they execute.
   50 |  * <p>
   51 |  * This is essentially a refactoring of the old
   52 |  * <tt>HttpSessionContextIntegrationFilter</tt> to delegate the storage issues to a
   53 |  * separate strategy, allowing for more customization in the way the security context is
   54 |  * maintained between requests.
   55 |  * <p>
   56 |  * The <tt>forceEagerSessionCreation</tt> property can be used to ensure that a session is
   57 |  * always available before the filter chain executes (the default is <code>false</code>,
   58 |  * as this is resource intensive and not recommended).
   59 |  *
   60 |  * @author Luke Taylor
   61 |  * @since 3.0
   62 |  * @deprecated Use {@link SecurityContextHolderFilter}
   63 |  */
   64 | @Deprecated
   65 | public class SecurityContextPersistenceFilter extends GenericFilterBean {
   66 | 
   67 | 	static final String FILTER_APPLIED = "__spring_security_scpf_applied";
   68 | 
   69 | 	private SecurityContextRepository repo;
   70 | 
   71 | 	private SecurityContextHolderStrategy securityContextHolderStrategy = SecurityContextHolder
   72 | 		.getContextHolderStrategy();
   73 | 
   74 | 	private boolean forceEagerSessionCreation = false;
   75 | 
   76 | 	public SecurityContextPersistenceFilter() {
   77 | 		this(new HttpSessionSecurityContextRepository());
   78 | 	}
   79 | 
   80 | 	public SecurityContextPersistenceFilter(SecurityContextRepository repo) {
   81 | 		this.repo = repo;
   82 | 	}
   83 | 
   84 | 	@Override
   85 | 	public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
   86 | 			throws IOException, ServletException {
   87 | 		doFilter((HttpServletRequest) request, (HttpServletResponse) response, chain);
   88 | 	}
   89 | 
   90 | 	private void doFilter(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
   91 | 			throws IOException, ServletException {
   92 | 		// ensure that filter is only applied once per request
   93 | 		if (request.getAttribute(FILTER_APPLIED) != null) {
   94 | 			chain.doFilter(request, response);
   95 | 			return;
   96 | 		}
   97 | 		request.setAttribute(FILTER_APPLIED, Boolean.TRUE);
   98 | 		if (this.forceEagerSessionCreation) {
   99 | 			HttpSession session = request.getSession();
  100 | 			if (this.logger.isDebugEnabled() && session.isNew()) {
  101 | 				this.logger.debug(LogMessage.format("Created session %s eagerly", session.getId()));
  102 | 			}
  103 | 		}
  104 | 		HttpRequestResponseHolder holder = new HttpRequestResponseHolder(request, response);
  105 | 		SecurityContext contextBeforeChainExecution = this.repo.loadContext(holder);
  106 | 		try {
  107 | 			this.securityContextHolderStrategy.setContext(contextBeforeChainExecution);
  108 | 			if (contextBeforeChainExecution.getAuthentication() == null) {
  109 | 				logger.debug("Set SecurityContextHolder to empty SecurityContext");
  110 | 			}
  111 | 			else {
  112 | 				if (this.logger.isDebugEnabled()) {
  113 | 					this.logger
  114 | 						.debug(LogMessage.format("Set SecurityContextHolder to %s", contextBeforeChainExecution));
  115 | 				}
  116 | 			}
  117 | 			chain.doFilter(holder.getRequest(), holder.getResponse());
  118 | 		}
  119 | 		finally {
  120 | 			SecurityContext contextAfterChainExecution = this.securityContextHolderStrategy.getContext();
  121 | 			// Crucial removal of SecurityContextHolder contents before anything else.
  122 | 			this.securityContextHolderStrategy.clearContext();
  123 | 			this.repo.saveContext(contextAfterChainExecution, holder.getRequest(), holder.getResponse());
  124 | 			request.removeAttribute(FILTER_APPLIED);
  125 | 			this.logger.debug("Cleared SecurityContextHolder to complete request");
  126 | 		}
  127 | 	}
  128 | 
  129 | 	public void setForceEagerSessionCreation(boolean forceEagerSessionCreation) {
  130 | 		this.forceEagerSessionCreation = forceEagerSessionCreation;
  131 | 	}
  132 | 
  133 | 	/**
  134 | 	 * Sets the {@link SecurityContextHolderStrategy} to use. The default action is to use
  135 | 	 * the {@link SecurityContextHolderStrategy} stored in {@link SecurityContextHolder}.
  136 | 	 *
  137 | 	 * @since 5.8
  138 | 	 */
  139 | 	public void setSecurityContextHolderStrategy(SecurityContextHolderStrategy securityContextHolderStrategy) {
  140 | 		Assert.notNull(securityContextHolderStrategy, "securityContextHolderStrategy cannot be null");
  141 | 		this.securityContextHolderStrategy = securityContextHolderStrategy;
  142 | 	}
  143 | 
  144 | }
```
