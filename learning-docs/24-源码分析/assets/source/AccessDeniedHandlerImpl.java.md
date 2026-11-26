<!-- learning-docs source archive（机械抓取原文，禁止手改代码内容） -->
<!-- origin: org.springframework.security:spring-security-web:7.2.0 | path: org/springframework/security/web/access/AccessDeniedHandlerImpl.java -->
<!-- 正文引用：[[源码:AccessDeniedHandlerImpl.java#L起-L止]] ；代码块头部：(src: AccessDeniedHandlerImpl.java#L起-L止) -->

# AccessDeniedHandlerImpl.java

| 项 | 值 |
| --- | --- |
| 来源 | `org.springframework.security:spring-security-web:7.2.0` |
| 路径 | `org/springframework/security/web/access/AccessDeniedHandlerImpl.java` |
| 行数 | 88 |
| 在线地址 | https://repo1.maven.org/maven2/org/springframework/security/spring-security-web/7.2.0/spring-security-web-7.2.0-sources.jar |
| 抓取时间 | 2026-11-26T06:37:40.981Z |

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
   17 | package org.springframework.security.web.access;
   18 | 
   19 | import java.io.IOException;
   20 | 
   21 | import jakarta.servlet.ServletException;
   22 | import jakarta.servlet.http.HttpServletRequest;
   23 | import jakarta.servlet.http.HttpServletResponse;
   24 | import org.apache.commons.logging.Log;
   25 | import org.apache.commons.logging.LogFactory;
   26 | import org.jspecify.annotations.Nullable;
   27 | 
   28 | import org.springframework.core.log.LogMessage;
   29 | import org.springframework.http.HttpStatus;
   30 | import org.springframework.security.access.AccessDeniedException;
   31 | import org.springframework.security.web.WebAttributes;
   32 | import org.springframework.util.Assert;
   33 | 
   34 | /**
   35 |  * Base implementation of {@link AccessDeniedHandler}.
   36 |  * <p>
   37 |  * This implementation sends a 403 (SC_FORBIDDEN) HTTP error code. In addition, if an
   38 |  * {@link #errorPage} is defined, the implementation will perform a request dispatcher
   39 |  * "forward" to the specified error page view. Being a "forward", the
   40 |  * <code>SecurityContextHolder</code> will remain populated. This is of benefit if the
   41 |  * view (or a tag library or macro) wishes to access the
   42 |  * <code>SecurityContextHolder</code>. The request scope will also be populated with the
   43 |  * exception itself, available from the key {@link WebAttributes#ACCESS_DENIED_403}.
   44 |  *
   45 |  * @author Ben Alex
   46 |  */
   47 | public class AccessDeniedHandlerImpl implements AccessDeniedHandler {
   48 | 
   49 | 	protected static final Log logger = LogFactory.getLog(AccessDeniedHandlerImpl.class);
   50 | 
   51 | 	private @Nullable String errorPage;
   52 | 
   53 | 	@Override
   54 | 	public void handle(HttpServletRequest request, HttpServletResponse response,
   55 | 			AccessDeniedException accessDeniedException) throws IOException, ServletException {
   56 | 		if (response.isCommitted()) {
   57 | 			logger.trace("Did not write to response since already committed");
   58 | 			return;
   59 | 		}
   60 | 		if (this.errorPage == null) {
   61 | 			logger.debug("Responding with 403 status code");
   62 | 			response.sendError(HttpStatus.FORBIDDEN.value(), HttpStatus.FORBIDDEN.getReasonPhrase());
   63 | 			return;
   64 | 		}
   65 | 		// Put exception into request scope (perhaps of use to a view)
   66 | 		request.setAttribute(WebAttributes.ACCESS_DENIED_403, accessDeniedException);
   67 | 		// Set the 403 status code.
   68 | 		response.setStatus(HttpStatus.FORBIDDEN.value());
   69 | 		// forward to error page.
   70 | 		if (logger.isDebugEnabled()) {
   71 | 			logger.debug(LogMessage.format("Forwarding to %s with status code 403", this.errorPage));
   72 | 		}
   73 | 		request.getRequestDispatcher(this.errorPage).forward(request, response);
   74 | 	}
   75 | 
   76 | 	/**
   77 | 	 * The error page to use. Must begin with a "/" and is interpreted relative to the
   78 | 	 * current context root.
   79 | 	 * @param errorPage the dispatcher path to display
   80 | 	 * @throws IllegalArgumentException if the argument doesn't comply with the above
   81 | 	 * limitations
   82 | 	 */
   83 | 	public void setErrorPage(String errorPage) {
   84 | 		Assert.isTrue(errorPage == null || errorPage.startsWith("/"), "errorPage must begin with '/'");
   85 | 		this.errorPage = errorPage;
   86 | 	}
   87 | 
   88 | }
```
