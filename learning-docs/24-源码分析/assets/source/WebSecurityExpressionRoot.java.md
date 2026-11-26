<!-- learning-docs source archive（机械抓取原文，禁止手改代码内容） -->
<!-- origin: org.springframework.security:spring-security-web:7.2.0 | path: org/springframework/security/web/access/expression/WebSecurityExpressionRoot.java -->
<!-- 正文引用：[[源码:WebSecurityExpressionRoot.java#L起-L止]] ；代码块头部：(src: WebSecurityExpressionRoot.java#L起-L止) -->

# WebSecurityExpressionRoot.java

| 项 | 值 |
| --- | --- |
| 来源 | `org.springframework.security:spring-security-web:7.2.0` |
| 路径 | `org/springframework/security/web/access/expression/WebSecurityExpressionRoot.java` |
| 行数 | 98 |
| 在线地址 | https://repo1.maven.org/maven2/org/springframework/security/spring-security-web/7.2.0/spring-security-web-7.2.0-sources.jar |
| 抓取时间 | 2026-11-26T06:37:41.041Z |

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
   17 | package org.springframework.security.web.access.expression;
   18 | 
   19 | import java.util.function.Supplier;
   20 | 
   21 | import jakarta.servlet.http.HttpServletRequest;
   22 | import org.jspecify.annotations.Nullable;
   23 | 
   24 | import org.springframework.security.access.expression.SecurityExpressionRoot;
   25 | import org.springframework.security.core.Authentication;
   26 | import org.springframework.security.web.FilterInvocation;
   27 | import org.springframework.security.web.access.intercept.RequestAuthorizationContext;
   28 | import org.springframework.security.web.util.matcher.IpAddressMatcher;
   29 | 
   30 | /**
   31 |  * The {@link SecurityExpressionRoot} used for web security expression evaluation.
   32 |  *
   33 |  * @author Luke Taylor
   34 |  * @author Evgeniy Cheban
   35 |  * @author Steve Riesenberg
   36 |  * @since 3.0
   37 |  */
   38 | public class WebSecurityExpressionRoot extends SecurityExpressionRoot<RequestAuthorizationContext> {
   39 | 
   40 | 	/**
   41 | 	 * Allows direct access to the request object.
   42 | 	 */
   43 | 	public final HttpServletRequest request;
   44 | 
   45 | 	/**
   46 | 	 * Creates an instance for the given {@link Authentication} and
   47 | 	 * {@link FilterInvocation}.
   48 | 	 * @deprecated Use
   49 | 	 * {@link #WebSecurityExpressionRoot(Supplier, RequestAuthorizationContext)} instead
   50 | 	 */
   51 | 	@Deprecated(since = "7.0")
   52 | 	public WebSecurityExpressionRoot(@Nullable Authentication a, FilterInvocation fi) {
   53 | 		this(() -> a, new RequestAuthorizationContext(fi.getRequest()));
   54 | 	}
   55 | 
   56 | 	/**
   57 | 	 * Creates an instance for the given {@link Supplier} of the {@link Authentication}
   58 | 	 * and {@link HttpServletRequest}.
   59 | 	 * @param authentication the {@link Supplier} of the {@link Authentication} to use
   60 | 	 * @param request the {@link HttpServletRequest} to use
   61 | 	 * @since 5.8
   62 | 	 * @deprecated Use
   63 | 	 * {@link #WebSecurityExpressionRoot(Supplier, RequestAuthorizationContext)} instead
   64 | 	 */
   65 | 	@Deprecated(since = "7.0")
   66 | 	@SuppressWarnings("NullAway") // https://github.com/uber/NullAway/issues/1246
   67 | 	public WebSecurityExpressionRoot(Supplier<? extends @Nullable Authentication> authentication,
   68 | 			HttpServletRequest request) {
   69 | 		super(authentication, new RequestAuthorizationContext(request));
   70 | 		this.request = request;
   71 | 	}
   72 | 
   73 | 	/**
   74 | 	 * Creates an instance for the given {@link Supplier} of the {@link Authentication}
   75 | 	 * and {@link HttpServletRequest}.
   76 | 	 * @param authentication the {@link Supplier} of the {@link Authentication} to use
   77 | 	 * @param context the {@link RequestAuthorizationContext} to use
   78 | 	 * @since 7.0
   79 | 	 */
   80 | 	public WebSecurityExpressionRoot(Supplier<? extends @Nullable Authentication> authentication,
   81 | 			RequestAuthorizationContext context) {
   82 | 		super(authentication, context);
   83 | 		this.request = context.getRequest();
   84 | 	}
   85 | 
   86 | 	/**
   87 | 	 * Takes a specific IP address or a range using the IP/Netmask (e.g. 192.168.1.0/24 or
   88 | 	 * 202.24.0.0/14).
   89 | 	 * @param ipAddress the address or range of addresses from which the request must
   90 | 	 * come.
   91 | 	 * @return true if the IP address of the current request is in the required range.
   92 | 	 */
   93 | 	public boolean hasIpAddress(String ipAddress) {
   94 | 		IpAddressMatcher matcher = new IpAddressMatcher(ipAddress);
   95 | 		return matcher.matches(this.request);
   96 | 	}
   97 | 
   98 | }
```
