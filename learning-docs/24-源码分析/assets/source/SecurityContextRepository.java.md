<!-- learning-docs source archive（机械抓取原文，禁止手改代码内容） -->
<!-- origin: org.springframework.security:spring-security-web:7.2.0 | path: org/springframework/security/web/context/SecurityContextRepository.java -->
<!-- 正文引用：[[源码:SecurityContextRepository.java#L起-L止]] ；代码块头部：(src: SecurityContextRepository.java#L起-L止) -->

# SecurityContextRepository.java

| 项 | 值 |
| --- | --- |
| 来源 | `org.springframework.security:spring-security-web:7.2.0` |
| 路径 | `org/springframework/security/web/context/SecurityContextRepository.java` |
| 行数 | 106 |
| 在线地址 | https://repo1.maven.org/maven2/org/springframework/security/spring-security-web/7.2.0/spring-security-web-7.2.0-sources.jar |
| 抓取时间 | 2026-11-25T16:27:28.316Z |

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
   21 | import jakarta.servlet.http.HttpServletRequest;
   22 | import jakarta.servlet.http.HttpServletResponse;
   23 | 
   24 | import org.springframework.security.core.context.DeferredSecurityContext;
   25 | import org.springframework.security.core.context.SecurityContext;
   26 | import org.springframework.security.core.context.SecurityContextHolder;
   27 | import org.springframework.util.function.SingletonSupplier;
   28 | 
   29 | /**
   30 |  * Strategy used for persisting a {@link SecurityContext} between requests.
   31 |  * <p>
   32 |  * Used by {@link SecurityContextPersistenceFilter} to obtain the context which should be
   33 |  * used for the current thread of execution and to store the context once it has been
   34 |  * removed from thread-local storage and the request has completed.
   35 |  * <p>
   36 |  * The persistence mechanism used will depend on the implementation, but most commonly the
   37 |  * <tt>HttpSession</tt> will be used to store the context.
   38 |  *
   39 |  * @author Luke Taylor
   40 |  * @since 3.0
   41 |  * @see SecurityContextPersistenceFilter
   42 |  * @see HttpSessionSecurityContextRepository
   43 |  * @see SaveContextOnUpdateOrErrorResponseWrapper
   44 |  */
   45 | public interface SecurityContextRepository {
   46 | 
   47 | 	/**
   48 | 	 * Obtains the security context for the supplied request. For an unauthenticated user,
   49 | 	 * an empty context implementation should be returned. This method should not return
   50 | 	 * null.
   51 | 	 * <p>
   52 | 	 * The use of the <tt>HttpRequestResponseHolder</tt> parameter allows implementations
   53 | 	 * to return wrapped versions of the request or response (or both), allowing them to
   54 | 	 * access implementation-specific state for the request. The values obtained from the
   55 | 	 * holder will be passed on to the filter chain and also to the <tt>saveContext</tt>
   56 | 	 * method when it is finally called to allow implicit saves of the
   57 | 	 * <tt>SecurityContext</tt>. Implementations may wish to return a subclass of
   58 | 	 * {@link SaveContextOnUpdateOrErrorResponseWrapper} as the response object, which
   59 | 	 * guarantees that the context is persisted when an error or redirect occurs.
   60 | 	 * Implementations may allow passing in the original request response to allow
   61 | 	 * explicit saves.
   62 | 	 * @param requestResponseHolder holder for the current request and response for which
   63 | 	 * the context should be loaded.
   64 | 	 * @return The security context which should be used for the current request, never
   65 | 	 * null.
   66 | 	 * @deprecated Use {@link #loadDeferredContext(HttpServletRequest)} instead.
   67 | 	 */
   68 | 	@Deprecated
   69 | 	SecurityContext loadContext(HttpRequestResponseHolder requestResponseHolder);
   70 | 
   71 | 	/**
   72 | 	 * Defers loading the {@link SecurityContext} using the {@link HttpServletRequest}
   73 | 	 * until it is needed by the application.
   74 | 	 * @param request the {@link HttpServletRequest} to load the {@link SecurityContext}
   75 | 	 * from
   76 | 	 * @return a {@link DeferredSecurityContext} that returns the {@link SecurityContext}
   77 | 	 * which cannot be null
   78 | 	 * @since 5.8
   79 | 	 */
   80 | 	default DeferredSecurityContext loadDeferredContext(HttpServletRequest request) {
   81 | 		Supplier<SecurityContext> supplier = () -> {
   82 | 			@SuppressWarnings("NullAway") // fixed when remove deprecated method
   83 | 			HttpRequestResponseHolder holder = new HttpRequestResponseHolder(request, null);
   84 | 			return loadContext(holder);
   85 | 		};
   86 | 		return new SupplierDeferredSecurityContext(SingletonSupplier.of(supplier),
   87 | 				SecurityContextHolder.getContextHolderStrategy());
   88 | 	}
   89 | 
   90 | 	/**
   91 | 	 * Stores the security context on completion of a request.
   92 | 	 * @param context the non-null context which was obtained from the holder.
   93 | 	 * @param request
   94 | 	 * @param response
   95 | 	 */
   96 | 	void saveContext(SecurityContext context, HttpServletRequest request, HttpServletResponse response);
   97 | 
   98 | 	/**
   99 | 	 * Allows the repository to be queried as to whether it contains a security context
  100 | 	 * for the current request.
  101 | 	 * @param request the current request
  102 | 	 * @return true if a context is found for the request, false otherwise
  103 | 	 */
  104 | 	boolean containsContext(HttpServletRequest request);
  105 | 
  106 | }
```
