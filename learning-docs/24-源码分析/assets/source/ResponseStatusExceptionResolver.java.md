<!-- learning-docs source archive（机械抓取原文，禁止手改代码内容） -->
<!-- origin: org.springframework:spring-webmvc:7.1.0 | path: org/springframework/web/servlet/mvc/annotation/ResponseStatusExceptionResolver.java -->
<!-- 正文引用：[[源码:ResponseStatusExceptionResolver.java#L起-L止]] ；代码块头部：(src: ResponseStatusExceptionResolver.java#L起-L止) -->

# ResponseStatusExceptionResolver.java

| 项 | 值 |
| --- | --- |
| 来源 | `org.springframework:spring-webmvc:7.1.0` |
| 路径 | `org/springframework/web/servlet/mvc/annotation/ResponseStatusExceptionResolver.java` |
| 行数 | 176 |
| 在线地址 | https://repo1.maven.org/maven2/org/springframework/spring-webmvc/7.1.0/spring-webmvc-7.1.0-sources.jar |
| 抓取时间 | 2026-11-25T14:01:11.637Z |

```java
    1 | /*
    2 |  * Copyright 2002-present the original author or authors.
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
   17 | package org.springframework.web.servlet.mvc.annotation;
   18 | 
   19 | import java.io.IOException;
   20 | 
   21 | import jakarta.servlet.http.HttpServletRequest;
   22 | import jakarta.servlet.http.HttpServletResponse;
   23 | import org.jspecify.annotations.Nullable;
   24 | 
   25 | import org.springframework.context.MessageSource;
   26 | import org.springframework.context.MessageSourceAware;
   27 | import org.springframework.context.i18n.LocaleContextHolder;
   28 | import org.springframework.core.annotation.AnnotatedElementUtils;
   29 | import org.springframework.http.HttpStatusCode;
   30 | import org.springframework.util.StringUtils;
   31 | import org.springframework.web.bind.annotation.ResponseStatus;
   32 | import org.springframework.web.filter.ServerHttpObservationFilter;
   33 | import org.springframework.web.server.ResponseStatusException;
   34 | import org.springframework.web.servlet.ModelAndView;
   35 | import org.springframework.web.servlet.handler.AbstractHandlerExceptionResolver;
   36 | 
   37 | /**
   38 |  * A {@link org.springframework.web.servlet.HandlerExceptionResolver
   39 |  * HandlerExceptionResolver} that uses the {@link ResponseStatus @ResponseStatus}
   40 |  * annotation to map exceptions to HTTP status codes.
   41 |  *
   42 |  * <p>This exception resolver is enabled by default in the
   43 |  * {@link org.springframework.web.servlet.DispatcherServlet DispatcherServlet}
   44 |  * and the MVC Java config and the MVC namespace.
   45 |  *
   46 |  * <p>As of 4.2 this resolver also looks recursively for {@code @ResponseStatus}
   47 |  * present on cause exceptions, and as of 4.2.2 this resolver supports
   48 |  * attribute overrides for {@code @ResponseStatus} in custom composed annotations.
   49 |  *
   50 |  * <p>As of 5.0 this resolver also supports {@link ResponseStatusException}.
   51 |  *
   52 |  * <p>As of 7.1, exceptions resolved to a 5xx server error status are recorded
   53 |  * as the error of the current HTTP server observation, if any.
   54 |  *
   55 |  * @author Arjen Poutsma
   56 |  * @author Rossen Stoyanchev
   57 |  * @author Sam Brannen
   58 |  * @since 3.0
   59 |  * @see ResponseStatus
   60 |  * @see ResponseStatusException
   61 |  */
   62 | public class ResponseStatusExceptionResolver extends AbstractHandlerExceptionResolver implements MessageSourceAware {
   63 | 
   64 | 	private @Nullable MessageSource messageSource;
   65 | 
   66 | 
   67 | 	@Override
   68 | 	public void setMessageSource(@Nullable MessageSource messageSource) {
   69 | 		this.messageSource = messageSource;
   70 | 	}
   71 | 
   72 | 
   73 | 	@Override
   74 | 	protected @Nullable ModelAndView doResolveException(
   75 | 			HttpServletRequest request, HttpServletResponse response, @Nullable Object handler, Exception ex) {
   76 | 
   77 | 		try {
   78 | 			if (ex instanceof ResponseStatusException rse) {
   79 | 				ModelAndView mav = resolveResponseStatusException(rse, request, response, handler);
   80 | 				recordServerError(rse.getStatusCode(), ex, request);
   81 | 				return mav;
   82 | 			}
   83 | 
   84 | 			ResponseStatus status = AnnotatedElementUtils.findMergedAnnotation(ex.getClass(), ResponseStatus.class);
   85 | 			if (status != null) {
   86 | 				ModelAndView mav = resolveResponseStatus(status, request, response, handler, ex);
   87 | 				recordServerError(status.code(), ex, request);
   88 | 				return mav;
   89 | 			}
   90 | 
   91 | 			if (ex.getCause() instanceof Exception cause) {
   92 | 				return doResolveException(request, response, handler, cause);
   93 | 			}
   94 | 		}
   95 | 		catch (Exception resolveEx) {
   96 | 			if (logger.isWarnEnabled()) {
   97 | 				logger.warn("Failure while trying to resolve exception [" + ex.getClass().getName() + "]", resolveEx);
   98 | 			}
   99 | 		}
  100 | 		return null;
  101 | 	}
  102 | 
  103 | 	private void recordServerError(HttpStatusCode statusCode, Exception ex, HttpServletRequest request) {
  104 | 		if (statusCode.is5xxServerError()) {
  105 | 			ServerHttpObservationFilter.recordHandledException(request, ex);
  106 | 		}
  107 | 	}
  108 | 
  109 | 	/**
  110 | 	 * Template method that handles the {@link ResponseStatus @ResponseStatus} annotation.
  111 | 	 * <p>The default implementation delegates to {@link #applyStatusAndReason}
  112 | 	 * with the status code and reason from the annotation.
  113 | 	 * @param responseStatus the {@code @ResponseStatus} annotation
  114 | 	 * @param request current HTTP request
  115 | 	 * @param response current HTTP response
  116 | 	 * @param handler the executed handler, or {@code null} if none chosen at the
  117 | 	 * time of the exception, for example, if multipart resolution failed
  118 | 	 * @param ex the exception
  119 | 	 * @return an empty ModelAndView, i.e. exception resolved
  120 | 	 */
  121 | 	protected ModelAndView resolveResponseStatus(ResponseStatus responseStatus, HttpServletRequest request,
  122 | 			HttpServletResponse response, @Nullable Object handler, Exception ex) throws Exception {
  123 | 
  124 | 		int statusCode = responseStatus.code().value();
  125 | 		String reason = responseStatus.reason();
  126 | 		return applyStatusAndReason(statusCode, reason, response);
  127 | 	}
  128 | 
  129 | 	/**
  130 | 	 * Template method that handles an {@link ResponseStatusException}.
  131 | 	 * <p>The default implementation applies the headers from
  132 | 	 * {@link ResponseStatusException#getHeaders()} and delegates to
  133 | 	 * {@link #applyStatusAndReason} with the status code and reason from the
  134 | 	 * exception.
  135 | 	 * @param ex the exception
  136 | 	 * @param request current HTTP request
  137 | 	 * @param response current HTTP response
  138 | 	 * @param handler the executed handler, or {@code null} if none chosen at the
  139 | 	 * time of the exception, for example, if multipart resolution failed
  140 | 	 * @return an empty ModelAndView, i.e. exception resolved
  141 | 	 * @since 5.0
  142 | 	 */
  143 | 	protected ModelAndView resolveResponseStatusException(ResponseStatusException ex,
  144 | 			HttpServletRequest request, HttpServletResponse response, @Nullable Object handler) throws Exception {
  145 | 
  146 | 		ex.getHeaders().forEach((name, values) -> values.forEach(value -> response.addHeader(name, value)));
  147 | 		return applyStatusAndReason(ex.getStatusCode().value(), ex.getReason(), response);
  148 | 	}
  149 | 
  150 | 	/**
  151 | 	 * Apply the resolved status code and reason to the response.
  152 | 	 * <p>The default implementation sends a response error using
  153 | 	 * {@link HttpServletResponse#sendError(int)} or
  154 | 	 * {@link HttpServletResponse#sendError(int, String)} if there is a reason
  155 | 	 * and then returns an empty ModelAndView.
  156 | 	 * @param statusCode the HTTP status code
  157 | 	 * @param reason the associated reason (may be {@code null} or empty)
  158 | 	 * @param response current HTTP response
  159 | 	 * @since 5.0
  160 | 	 */
  161 | 	protected ModelAndView applyStatusAndReason(int statusCode, @Nullable String reason, HttpServletResponse response)
  162 | 			throws IOException {
  163 | 
  164 | 		if (!StringUtils.hasLength(reason)) {
  165 | 			response.sendError(statusCode);
  166 | 		}
  167 | 		else {
  168 | 			String resolvedReason = (this.messageSource != null ?
  169 | 					this.messageSource.getMessage(reason, null, reason, LocaleContextHolder.getLocale()) :
  170 | 					reason);
  171 | 			response.sendError(statusCode, resolvedReason);
  172 | 		}
  173 | 		return new ModelAndView();
  174 | 	}
  175 | 
  176 | }
```
