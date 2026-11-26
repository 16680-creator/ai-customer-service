<!-- learning-docs source archive（机械抓取原文，禁止手改代码内容） -->
<!-- origin: org.springframework.security:spring-security-web:7.2.0 | path: org/springframework/security/web/context/SecurityContextHolderFilter.java -->
<!-- 正文引用：[[源码:SecurityContextHolderFilter.java#L起-L止]] ；代码块头部：(src: SecurityContextHolderFilter.java#L起-L止) -->

# SecurityContextHolderFilter.java

| 项 | 值 |
| --- | --- |
| 来源 | `org.springframework.security:spring-security-web:7.2.0` |
| 路径 | `org/springframework/security/web/context/SecurityContextHolderFilter.java` |
| 行数 | 101 |
| 在线地址 | https://repo1.maven.org/maven2/org/springframework/security/spring-security-web/7.2.0/spring-security-web-7.2.0-sources.jar |
| 抓取时间 | 2026-11-25T16:27:28.113Z |

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
   20 | import java.util.function.Supplier;
   21 | 
   22 | import jakarta.servlet.FilterChain;
   23 | import jakarta.servlet.ServletException;
   24 | import jakarta.servlet.ServletRequest;
   25 | import jakarta.servlet.ServletResponse;
   26 | import jakarta.servlet.http.HttpServletRequest;
   27 | import jakarta.servlet.http.HttpServletResponse;
   28 | 
   29 | import org.springframework.security.core.context.SecurityContext;
   30 | import org.springframework.security.core.context.SecurityContextHolder;
   31 | import org.springframework.security.core.context.SecurityContextHolderStrategy;
   32 | import org.springframework.util.Assert;
   33 | import org.springframework.web.filter.GenericFilterBean;
   34 | 
   35 | /**
   36 |  * A {@link jakarta.servlet.Filter} that uses the {@link SecurityContextRepository} to
   37 |  * obtain the {@link SecurityContext} and set it on the {@link SecurityContextHolder}.
   38 |  * This is similar to {@link SecurityContextPersistenceFilter} except that the
   39 |  * {@link SecurityContextRepository#saveContext(SecurityContext, HttpServletRequest, HttpServletResponse)}
   40 |  * must be explicitly invoked to save the {@link SecurityContext}. This improves the
   41 |  * efficiency and provides better flexibility by allowing different authentication
   42 |  * mechanisms to choose individually if authentication should be persisted.
   43 |  *
   44 |  * @author Rob Winch
   45 |  * @author Marcus da Coregio
   46 |  * @since 5.7
   47 |  */
   48 | public class SecurityContextHolderFilter extends GenericFilterBean {
   49 | 
   50 | 	private static final String FILTER_APPLIED = SecurityContextHolderFilter.class.getName() + ".APPLIED";
   51 | 
   52 | 	private final SecurityContextRepository securityContextRepository;
   53 | 
   54 | 	private SecurityContextHolderStrategy securityContextHolderStrategy = SecurityContextHolder
   55 | 		.getContextHolderStrategy();
   56 | 
   57 | 	/**
   58 | 	 * Creates a new instance.
   59 | 	 * @param securityContextRepository the repository to use. Cannot be null.
   60 | 	 */
   61 | 	public SecurityContextHolderFilter(SecurityContextRepository securityContextRepository) {
   62 | 		Assert.notNull(securityContextRepository, "securityContextRepository cannot be null");
   63 | 		this.securityContextRepository = securityContextRepository;
   64 | 	}
   65 | 
   66 | 	@Override
   67 | 	public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
   68 | 			throws IOException, ServletException {
   69 | 		doFilter((HttpServletRequest) request, (HttpServletResponse) response, chain);
   70 | 	}
   71 | 
   72 | 	private void doFilter(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
   73 | 			throws ServletException, IOException {
   74 | 		if (request.getAttribute(FILTER_APPLIED) != null) {
   75 | 			chain.doFilter(request, response);
   76 | 			return;
   77 | 		}
   78 | 		request.setAttribute(FILTER_APPLIED, Boolean.TRUE);
   79 | 		Supplier<SecurityContext> deferredContext = this.securityContextRepository.loadDeferredContext(request);
   80 | 		try {
   81 | 			this.securityContextHolderStrategy.setDeferredContext(deferredContext);
   82 | 			chain.doFilter(request, response);
   83 | 		}
   84 | 		finally {
   85 | 			this.securityContextHolderStrategy.clearContext();
   86 | 			request.removeAttribute(FILTER_APPLIED);
   87 | 		}
   88 | 	}
   89 | 
   90 | 	/**
   91 | 	 * Sets the {@link SecurityContextHolderStrategy} to use. The default action is to use
   92 | 	 * the {@link SecurityContextHolderStrategy} stored in {@link SecurityContextHolder}.
   93 | 	 *
   94 | 	 * @since 5.8
   95 | 	 */
   96 | 	public void setSecurityContextHolderStrategy(SecurityContextHolderStrategy securityContextHolderStrategy) {
   97 | 		Assert.notNull(securityContextHolderStrategy, "securityContextHolderStrategy cannot be null");
   98 | 		this.securityContextHolderStrategy = securityContextHolderStrategy;
   99 | 	}
  100 | 
  101 | }
```
