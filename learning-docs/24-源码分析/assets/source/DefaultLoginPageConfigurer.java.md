<!-- learning-docs source archive（机械抓取原文，禁止手改代码内容） -->
<!-- origin: org.springframework.security:spring-security-config:7.2.0 | path: org/springframework/security/config/annotation/web/configurers/DefaultLoginPageConfigurer.java -->
<!-- 正文引用：[[源码:DefaultLoginPageConfigurer.java#L起-L止]] ；代码块头部：(src: DefaultLoginPageConfigurer.java#L起-L止) -->

# DefaultLoginPageConfigurer.java

| 项 | 值 |
| --- | --- |
| 来源 | `org.springframework.security:spring-security-config:7.2.0` |
| 路径 | `org/springframework/security/config/annotation/web/configurers/DefaultLoginPageConfigurer.java` |
| 行数 | 102 |
| 在线地址 | https://repo1.maven.org/maven2/org/springframework/security/spring-security-config/7.2.0/spring-security-config-7.2.0-sources.jar |
| 抓取时间 | 2026-11-25T16:40:14.898Z |

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
   19 | import java.util.Collections;
   20 | import java.util.Map;
   21 | 
   22 | import jakarta.servlet.http.HttpServletRequest;
   23 | 
   24 | import org.springframework.security.config.annotation.web.HttpSecurityBuilder;
   25 | import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
   26 | import org.springframework.security.web.AuthenticationEntryPoint;
   27 | import org.springframework.security.web.authentication.ui.DefaultLoginPageGeneratingFilter;
   28 | import org.springframework.security.web.authentication.ui.DefaultLogoutPageGeneratingFilter;
   29 | import org.springframework.security.web.authentication.ui.DefaultResourcesFilter;
   30 | import org.springframework.security.web.csrf.CsrfToken;
   31 | 
   32 | /**
   33 |  * Adds a Filter that will generate a login page if one is not specified otherwise when
   34 |  * using {@link EnableWebSecurity}.
   35 |  *
   36 |  * <h2>Security Filters</h2>
   37 |  *
   38 |  * The following Filters are conditionally populated
   39 |  *
   40 |  * <ul>
   41 |  * <li>{@link DefaultLoginPageGeneratingFilter} if the {@link FormLoginConfigurer} did not
   42 |  * have a login page specified</li>
   43 |  * </ul>
   44 |  *
   45 |  * <h2>Shared Objects Created</h2>
   46 |  *
   47 |  * No shared objects are created.
   48 |  *
   49 |  * <h2>Shared Objects Used</h2>
   50 |  *
   51 |  * The following shared objects are used:
   52 |  *
   53 |  * <ul>
   54 |  * <li>{@link FormLoginConfigurer} is used to determine if the
   55 |  * {@link DefaultLoginPageConfigurer} should be added and how to configure it.</li>
   56 |  * </ul>
   57 |  *
   58 |  * @author Rob Winch
   59 |  * @since 3.2
   60 |  * @see EnableWebSecurity
   61 |  */
   62 | public final class DefaultLoginPageConfigurer<H extends HttpSecurityBuilder<H>>
   63 | 		extends AbstractHttpConfigurer<DefaultLoginPageConfigurer<H>, H> {
   64 | 
   65 | 	private DefaultLoginPageGeneratingFilter loginPageGeneratingFilter = new DefaultLoginPageGeneratingFilter();
   66 | 
   67 | 	private DefaultLogoutPageGeneratingFilter logoutPageGeneratingFilter = new DefaultLogoutPageGeneratingFilter();
   68 | 
   69 | 	@Override
   70 | 	public void init(H http) {
   71 | 		this.loginPageGeneratingFilter.setSecurityContextHolderStrategy(getSecurityContextHolderStrategy());
   72 | 		this.loginPageGeneratingFilter.setResolveHiddenInputs(DefaultLoginPageConfigurer.this::hiddenInputs);
   73 | 		this.logoutPageGeneratingFilter.setResolveHiddenInputs(DefaultLoginPageConfigurer.this::hiddenInputs);
   74 | 		http.setSharedObject(DefaultLoginPageGeneratingFilter.class, this.loginPageGeneratingFilter);
   75 | 	}
   76 | 
   77 | 	private Map<String, String> hiddenInputs(HttpServletRequest request) {
   78 | 		CsrfToken token = (CsrfToken) request.getAttribute(CsrfToken.class.getName());
   79 | 		return (token != null) ? Collections.singletonMap(token.getParameterName(), token.getToken())
   80 | 				: Collections.emptyMap();
   81 | 	}
   82 | 
   83 | 	@Override
   84 | 	@SuppressWarnings("unchecked")
   85 | 	public void configure(H http) {
   86 | 		AuthenticationEntryPoint authenticationEntryPoint = null;
   87 | 		ExceptionHandlingConfigurer<?> exceptionConf = http.getConfigurer(ExceptionHandlingConfigurer.class);
   88 | 		if (exceptionConf != null) {
   89 | 			authenticationEntryPoint = exceptionConf.getAuthenticationEntryPoint();
   90 | 		}
   91 | 		if (this.loginPageGeneratingFilter.isEnabled() && authenticationEntryPoint == null) {
   92 | 			this.loginPageGeneratingFilter = postProcess(this.loginPageGeneratingFilter);
   93 | 			http.addFilter(this.loginPageGeneratingFilter);
   94 | 			http.addFilter(DefaultResourcesFilter.css());
   95 | 			LogoutConfigurer<H> logoutConfigurer = http.getConfigurer(LogoutConfigurer.class);
   96 | 			if (logoutConfigurer != null) {
   97 | 				http.addFilter(this.logoutPageGeneratingFilter);
   98 | 			}
   99 | 		}
  100 | 	}
  101 | 
  102 | }
```
