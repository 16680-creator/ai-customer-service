<!-- learning-docs source archive（机械抓取原文，禁止手改代码内容） -->
<!-- origin: org.springframework:spring-webmvc:7.1.0 | path: org/springframework/web/servlet/mvc/support/DefaultHandlerExceptionResolver.java -->
<!-- 正文引用：[[源码:DefaultHandlerExceptionResolver.java#L起-L止]] ；代码块头部：(src: DefaultHandlerExceptionResolver.java#L起-L止) -->

# DefaultHandlerExceptionResolver.java

| 项 | 值 |
| --- | --- |
| 来源 | `org.springframework:spring-webmvc:7.1.0` |
| 路径 | `org/springframework/web/servlet/mvc/support/DefaultHandlerExceptionResolver.java` |
| 行数 | 729 |
| 在线地址 | https://repo1.maven.org/maven2/org/springframework/spring-webmvc/7.1.0/spring-webmvc-7.1.0-sources.jar |
| 抓取时间 | 2026-11-25T14:01:11.486Z |

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
   17 | package org.springframework.web.servlet.mvc.support;
   18 | 
   19 | import java.io.IOException;
   20 | 
   21 | import jakarta.servlet.http.HttpServletRequest;
   22 | import jakarta.servlet.http.HttpServletResponse;
   23 | import org.apache.commons.logging.Log;
   24 | import org.apache.commons.logging.LogFactory;
   25 | import org.jspecify.annotations.Nullable;
   26 | 
   27 | import org.springframework.beans.ConversionNotSupportedException;
   28 | import org.springframework.beans.TypeMismatchException;
   29 | import org.springframework.core.Ordered;
   30 | import org.springframework.http.HttpHeaders;
   31 | import org.springframework.http.ProblemDetail;
   32 | import org.springframework.http.converter.HttpMessageNotReadableException;
   33 | import org.springframework.http.converter.HttpMessageNotWritableException;
   34 | import org.springframework.validation.method.MethodValidationException;
   35 | import org.springframework.web.ErrorResponse;
   36 | import org.springframework.web.HttpMediaTypeNotAcceptableException;
   37 | import org.springframework.web.HttpMediaTypeNotSupportedException;
   38 | import org.springframework.web.HttpRequestMethodNotSupportedException;
   39 | import org.springframework.web.bind.MethodArgumentNotValidException;
   40 | import org.springframework.web.bind.MissingPathVariableException;
   41 | import org.springframework.web.bind.MissingServletRequestParameterException;
   42 | import org.springframework.web.bind.ServletRequestBindingException;
   43 | import org.springframework.web.bind.annotation.RequestBody;
   44 | import org.springframework.web.bind.annotation.RequestPart;
   45 | import org.springframework.web.context.request.async.AsyncRequestNotUsableException;
   46 | import org.springframework.web.context.request.async.AsyncRequestTimeoutException;
   47 | import org.springframework.web.filter.ServerHttpObservationFilter;
   48 | import org.springframework.web.method.annotation.HandlerMethodValidationException;
   49 | import org.springframework.web.multipart.MultipartFile;
   50 | import org.springframework.web.multipart.support.MissingServletRequestPartException;
   51 | import org.springframework.web.servlet.ModelAndView;
   52 | import org.springframework.web.servlet.NoHandlerFoundException;
   53 | import org.springframework.web.servlet.handler.AbstractHandlerExceptionResolver;
   54 | import org.springframework.web.servlet.resource.NoResourceFoundException;
   55 | import org.springframework.web.util.DisconnectedClientHelper;
   56 | import org.springframework.web.util.WebUtils;
   57 | 
   58 | /**
   59 |  * The default implementation of the {@link org.springframework.web.servlet.HandlerExceptionResolver}
   60 |  * interface, resolving standard Spring MVC exceptions and translating them to corresponding
   61 |  * HTTP status codes.
   62 |  *
   63 |  * <p>This exception resolver is enabled by default in the common Spring
   64 |  * {@link org.springframework.web.servlet.DispatcherServlet}.
   65 |  *
   66 |  * <h3>Supported Exceptions</h3>
   67 |  * <table>
   68 |  * <thead>
   69 |  * <tr>
   70 |  * <th class="table-header col-first">Exception</th>
   71 |  * <th class="table-header col-last">HTTP Status Code</th>
   72 |  * </tr>
   73 |  * </thead>
   74 |  * <tbody>
   75 |  * <tr class="even-row-color">
   76 |  * <td><div class="block">HttpRequestMethodNotSupportedException</div></td>
   77 |  * <td><div class="block">405 (SC_METHOD_NOT_ALLOWED)</div></td>
   78 |  * </tr>
   79 |  * <tr class="odd-row-color">
   80 |  * <td><div class="block">HttpMediaTypeNotSupportedException</div></td>
   81 |  * <td><div class="block">415 (SC_UNSUPPORTED_MEDIA_TYPE)</div></td>
   82 |  * </tr>
   83 |  * <tr class="even-row-color">
   84 |  * <td><div class="block">HttpMediaTypeNotAcceptableException</div></td>
   85 |  * <td><div class="block">406 (SC_NOT_ACCEPTABLE)</div></td>
   86 |  * </tr>
   87 |  * <tr class="odd-row-color">
   88 |  * <td><div class="block">MissingPathVariableException</div></td>
   89 |  * <td><div class="block">500 (SC_INTERNAL_SERVER_ERROR)</div></td>
   90 |  * </tr>
   91 |  * <tr class="even-row-color">
   92 |  * <td><div class="block">MissingServletRequestParameterException</div></td>
   93 |  * <td><div class="block">400 (SC_BAD_REQUEST)</div></td>
   94 |  * </tr>
   95 |  * <tr class="odd-row-color">
   96 |  * <td><div class="block">MissingServletRequestPartException</div></td>
   97 |  * <td><div class="block">400 (SC_BAD_REQUEST)</div></td>
   98 |  * </tr>
   99 |  * <tr class="even-row-color">
  100 |  * <td><div class="block">ServletRequestBindingException</div></td>
  101 |  * <td><div class="block">400 (SC_BAD_REQUEST)</div></td>
  102 |  * </tr>
  103 |  * <tr class="odd-row-color">
  104 |  * <td><div class="block">ConversionNotSupportedException</div></td>
  105 |  * <td><div class="block">500 (SC_INTERNAL_SERVER_ERROR)</div></td>
  106 |  * </tr>
  107 |  * <tr class="even-row-color">
  108 |  * <td><div class="block">TypeMismatchException</div></td>
  109 |  * <td><div class="block">400 (SC_BAD_REQUEST)</div></td>
  110 |  * </tr>
  111 |  * <tr class="odd-row-color">
  112 |  * <td><div class="block">HttpMessageNotReadableException</div></td>
  113 |  * <td><div class="block">400 (SC_BAD_REQUEST)</div></td>
  114 |  * </tr>
  115 |  * <tr class="even-row-color">
  116 |  * <td><div class="block">HttpMessageNotWritableException</div></td>
  117 |  * <td><div class="block">500 (SC_INTERNAL_SERVER_ERROR)</div></td>
  118 |  * </tr>
  119 |  * <tr class="odd-row-color">
  120 |  * <td><div class="block">MethodArgumentNotValidException</div></td>
  121 |  * <td><div class="block">400 (SC_BAD_REQUEST)</div></td>
  122 |  * </tr>
  123 |  * <tr class="odd-row-color">
  124 |  * <td><div class="block">{@link MethodValidationException}</div></td>
  125 |  * <td><div class="block">500 (SC_INTERNAL_SERVER_ERROR)</div></td>
  126 |  * </tr>
  127 |  * <tr class="odd-row-color">
  128 |  * <td><div class="block">{@link HandlerMethodValidationException}</div></td>
  129 |  * <td><div class="block">400 (SC_BAD_REQUEST)</div></td>
  130 |  * </tr>
  131 |  * <tr class="odd-row-color">
  132 |  * <td><div class="block">NoHandlerFoundException</div></td>
  133 |  * <td><div class="block">404 (SC_NOT_FOUND)</div></td>
  134 |  * </tr>
  135 |  * <tr class="even-row-color">
  136 |  * <td><div class="block">NoResourceFoundException</div></td>
  137 |  * <td><div class="block">404 (SC_NOT_FOUND)</div></td>
  138 |  * </tr>
  139 |  * <tr class="odd-row-color">
  140 |  * <td><div class="block">AsyncRequestTimeoutException</div></td>
  141 |  * <td><div class="block">503 (SC_SERVICE_UNAVAILABLE)</div></td>
  142 |  * </tr>
  143 |  * <tr class="even-row-color">
  144 |  * <td><div class="block">AsyncRequestNotUsableException</div></td>
  145 |  * <td><div class="block">Not applicable</div></td>
  146 |  * </tr>
  147 |  * </tbody>
  148 |  * </table>
  149 |  *
  150 |  * @author Arjen Poutsma
  151 |  * @author Rossen Stoyanchev
  152 |  * @author Juergen Hoeller
  153 |  * @since 3.0
  154 |  * @see org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler
  155 |  */
  156 | public class DefaultHandlerExceptionResolver extends AbstractHandlerExceptionResolver {
  157 | 
  158 | 	/**
  159 | 	 * Log category to use when no mapped handler is found for a request.
  160 | 	 * @see #pageNotFoundLogger
  161 | 	 */
  162 | 	public static final String PAGE_NOT_FOUND_LOG_CATEGORY = "org.springframework.web.servlet.PageNotFound";
  163 | 
  164 | 	/**
  165 | 	 * Additional logger to use when no mapped handler is found for a request.
  166 | 	 * @see #PAGE_NOT_FOUND_LOG_CATEGORY
  167 | 	 */
  168 | 	protected static final Log pageNotFoundLogger = LogFactory.getLog(PAGE_NOT_FOUND_LOG_CATEGORY);
  169 | 
  170 | 
  171 | 	private static final String DISCONNECTED_CLIENT_LOG_CATEGORY =
  172 | 			"org.springframework.web.servlet.handler.DisconnectedClient";
  173 | 
  174 | 	private static final DisconnectedClientHelper disconnectedClientHelper =
  175 | 			new DisconnectedClientHelper(DISCONNECTED_CLIENT_LOG_CATEGORY);
  176 | 
  177 | 
  178 | 	/**
  179 | 	 * Sets the {@linkplain #setOrder(int) order} to {@link #LOWEST_PRECEDENCE}.
  180 | 	 */
  181 | 	public DefaultHandlerExceptionResolver() {
  182 | 		setOrder(Ordered.LOWEST_PRECEDENCE);
  183 | 		setWarnLogCategory(getClass().getName());
  184 | 	}
  185 | 
  186 | 
  187 | 	@Override
  188 | 	protected @Nullable ModelAndView doResolveException(
  189 | 			HttpServletRequest request, HttpServletResponse response, @Nullable Object handler, Exception ex) {
  190 | 
  191 | 		try {
  192 | 			// ErrorResponse exceptions that expose HTTP response details
  193 | 			if (ex instanceof ErrorResponse errorResponse) {
  194 | 				ModelAndView mav = null;
  195 | 				if (ex instanceof HttpRequestMethodNotSupportedException theEx) {
  196 | 					mav = handleHttpRequestMethodNotSupported(theEx, request, response, handler);
  197 | 				}
  198 | 				else if (ex instanceof HttpMediaTypeNotSupportedException theEx) {
  199 | 					mav = handleHttpMediaTypeNotSupported(theEx, request, response, handler);
  200 | 				}
  201 | 				else if (ex instanceof HttpMediaTypeNotAcceptableException theEx) {
  202 | 					mav = handleHttpMediaTypeNotAcceptable(theEx, request, response, handler);
  203 | 				}
  204 | 				else if (ex instanceof MissingPathVariableException theEx) {
  205 | 					mav = handleMissingPathVariable(theEx, request, response, handler);
  206 | 				}
  207 | 				else if (ex instanceof MissingServletRequestParameterException theEx) {
  208 | 					mav = handleMissingServletRequestParameter(theEx, request, response, handler);
  209 | 				}
  210 | 				else if (ex instanceof MissingServletRequestPartException theEx) {
  211 | 					mav = handleMissingServletRequestPartException(theEx, request, response, handler);
  212 | 				}
  213 | 				else if (ex instanceof ServletRequestBindingException theEx) {
  214 | 					mav = handleServletRequestBindingException(theEx, request, response, handler);
  215 | 				}
  216 | 				else if (ex instanceof MethodArgumentNotValidException theEx) {
  217 | 					mav = handleMethodArgumentNotValidException(theEx, request, response, handler);
  218 | 				}
  219 | 				else if (ex instanceof HandlerMethodValidationException theEx) {
  220 | 					mav = handleHandlerMethodValidationException(theEx, request, response, handler);
  221 | 				}
  222 | 				else if (ex instanceof NoHandlerFoundException theEx) {
  223 | 					mav = handleNoHandlerFoundException(theEx, request, response, handler);
  224 | 				}
  225 | 				else if (ex instanceof NoResourceFoundException theEx) {
  226 | 					mav = handleNoResourceFoundException(theEx, request, response, handler);
  227 | 				}
  228 | 				else if (ex instanceof AsyncRequestTimeoutException theEx) {
  229 | 					mav = handleAsyncRequestTimeoutException(theEx, request, response, handler);
  230 | 				}
  231 | 
  232 | 				return (mav != null ? mav :
  233 | 						handleErrorResponse(errorResponse, request, response, handler));
  234 | 			}
  235 | 
  236 | 			// Other, lower level exceptions
  237 | 
  238 | 			if (ex instanceof ConversionNotSupportedException theEx) {
  239 | 				return handleConversionNotSupported(theEx, request, response, handler);
  240 | 			}
  241 | 			else if (ex instanceof TypeMismatchException theEx) {
  242 | 				return handleTypeMismatch(theEx, request, response, handler);
  243 | 			}
  244 | 			else if (ex instanceof HttpMessageNotReadableException theEx) {
  245 | 				return handleHttpMessageNotReadable(theEx, request, response, handler);
  246 | 			}
  247 | 			else if (ex instanceof HttpMessageNotWritableException theEx) {
  248 | 				return handleHttpMessageNotWritable(theEx, request, response, handler);
  249 | 			}
  250 | 			else if (ex instanceof MethodValidationException theEx) {
  251 | 				return handleMethodValidationException(theEx, request, response, handler);
  252 | 			}
  253 | 			else if (ex instanceof AsyncRequestNotUsableException) {
  254 | 				return handleAsyncRequestNotUsableException(
  255 | 						(AsyncRequestNotUsableException) ex, request, response, handler);
  256 | 			}
  257 | 			else if (disconnectedClientHelper.checkAndLogClientDisconnectedException(ex)) {
  258 | 				return handleDisconnectedClientException(ex, request, response, handler);
  259 | 			}
  260 | 		}
  261 | 		catch (Exception handlerEx) {
  262 | 			if (logger.isWarnEnabled()) {
  263 | 				logger.warn("Failure while trying to resolve exception [" + ex.getClass().getName() + "]", handlerEx);
  264 | 			}
  265 | 		}
  266 | 
  267 | 		return null;
  268 | 	}
  269 | 
  270 | 	/**
  271 | 	 * Handle the case where no handler was found for the HTTP method.
  272 | 	 * <p>The default implementation returns {@code null} in which case the
  273 | 	 * exception is handled in {@link #handleErrorResponse}.
  274 | 	 * @param ex the HttpRequestMethodNotSupportedException to be handled
  275 | 	 * @param request current HTTP request
  276 | 	 * @param response current HTTP response
  277 | 	 * @param handler the executed handler, or {@code null} if none chosen
  278 | 	 * at the time of the exception (for example, if multipart resolution failed)
  279 | 	 * @return an empty {@code ModelAndView} indicating the exception was handled, or
  280 | 	 * {@code null} indicating the exception should be handled in {@link #handleErrorResponse}
  281 | 	 * @throws IOException potentially thrown from {@link HttpServletResponse#sendError}
  282 | 	 */
  283 | 	protected @Nullable ModelAndView handleHttpRequestMethodNotSupported(HttpRequestMethodNotSupportedException ex,
  284 | 			HttpServletRequest request, HttpServletResponse response, @Nullable Object handler) throws IOException {
  285 | 
  286 | 		return null;
  287 | 	}
  288 | 
  289 | 	/**
  290 | 	 * Handle the case where no
  291 | 	 * {@linkplain org.springframework.http.converter.HttpMessageConverter message converters}
  292 | 	 * were found for PUT or POSTed content.
  293 | 	 * <p>The default implementation returns {@code null} in which case the
  294 | 	 * exception is handled in {@link #handleErrorResponse}.
  295 | 	 * @param ex the HttpMediaTypeNotSupportedException to be handled
  296 | 	 * @param request current HTTP request
  297 | 	 * @param response current HTTP response
  298 | 	 * @param handler the executed handler
  299 | 	 * @return an empty {@code ModelAndView} indicating the exception was handled, or
  300 | 	 * {@code null} indicating the exception should be handled in {@link #handleErrorResponse}
  301 | 	 * @throws IOException potentially thrown from {@link HttpServletResponse#sendError}
  302 | 	 */
  303 | 	protected @Nullable ModelAndView handleHttpMediaTypeNotSupported(HttpMediaTypeNotSupportedException ex,
  304 | 			HttpServletRequest request, HttpServletResponse response, @Nullable Object handler) throws IOException {
  305 | 
  306 | 		return null;
  307 | 	}
  308 | 
  309 | 	/**
  310 | 	 * Handle the case where no
  311 | 	 * {@linkplain org.springframework.http.converter.HttpMessageConverter message converters}
  312 | 	 * were found that were acceptable for the client (expressed via the {@code Accept} header).
  313 | 	 * <p>The default implementation returns {@code null} in which case the
  314 | 	 * exception is handled in {@link #handleErrorResponse}.
  315 | 	 * @param ex the HttpMediaTypeNotAcceptableException to be handled
  316 | 	 * @param request current HTTP request
  317 | 	 * @param response current HTTP response
  318 | 	 * @param handler the executed handler
  319 | 	 * @return an empty {@code ModelAndView} indicating the exception was handled, or
  320 | 	 * {@code null} indicating the exception should be handled in {@link #handleErrorResponse}
  321 | 	 * @throws IOException potentially thrown from {@link HttpServletResponse#sendError}
  322 | 	 */
  323 | 	protected @Nullable ModelAndView handleHttpMediaTypeNotAcceptable(HttpMediaTypeNotAcceptableException ex,
  324 | 			HttpServletRequest request, HttpServletResponse response, @Nullable Object handler) throws IOException {
  325 | 
  326 | 		return null;
  327 | 	}
  328 | 
  329 | 	/**
  330 | 	 * Handle the case when a declared path variable does not match any extracted URI variable.
  331 | 	 * <p>The default implementation returns {@code null} in which case the
  332 | 	 * exception is handled in {@link #handleErrorResponse}.
  333 | 	 * @param ex the MissingPathVariableException to be handled
  334 | 	 * @param request current HTTP request
  335 | 	 * @param response current HTTP response
  336 | 	 * @param handler the executed handler
  337 | 	 * @return an empty {@code ModelAndView} indicating the exception was handled, or
  338 | 	 * {@code null} indicating the exception should be handled in {@link #handleErrorResponse}
  339 | 	 * @throws IOException potentially thrown from {@link HttpServletResponse#sendError}
  340 | 	 * @since 4.2
  341 | 	 */
  342 | 	protected @Nullable ModelAndView handleMissingPathVariable(MissingPathVariableException ex,
  343 | 			HttpServletRequest request, HttpServletResponse response, @Nullable Object handler) throws IOException {
  344 | 
  345 | 		return null;
  346 | 	}
  347 | 
  348 | 	/**
  349 | 	 * Handle the case when a required parameter is missing.
  350 | 	 * <p>The default implementation returns {@code null} in which case the
  351 | 	 * exception is handled in {@link #handleErrorResponse}.
  352 | 	 * @param ex the MissingServletRequestParameterException to be handled
  353 | 	 * @param request current HTTP request
  354 | 	 * @param response current HTTP response
  355 | 	 * @param handler the executed handler
  356 | 	 * @return an empty {@code ModelAndView} indicating the exception was handled, or
  357 | 	 * {@code null} indicating the exception should be handled in {@link #handleErrorResponse}
  358 | 	 * @throws IOException potentially thrown from {@link HttpServletResponse#sendError}
  359 | 	 */
  360 | 	protected @Nullable ModelAndView handleMissingServletRequestParameter(MissingServletRequestParameterException ex,
  361 | 			HttpServletRequest request, HttpServletResponse response, @Nullable Object handler) throws IOException {
  362 | 
  363 | 		return null;
  364 | 	}
  365 | 
  366 | 	/**
  367 | 	 * Handle the case where an {@linkplain RequestPart @RequestPart}, a {@link MultipartFile},
  368 | 	 * or a {@code jakarta.servlet.http.Part} argument is required but is missing.
  369 | 	 * <p>By default, an HTTP 400 error is sent back to the client.
  370 | 	 * @param request current HTTP request
  371 | 	 * @param response current HTTP response
  372 | 	 * @param handler the executed handler
  373 | 	 * @return an empty {@code ModelAndView} indicating the exception was handled, or
  374 | 	 * {@code null} indicating the exception should be handled in {@link #handleErrorResponse}
  375 | 	 * @throws IOException potentially thrown from {@link HttpServletResponse#sendError}
  376 | 	 */
  377 | 	protected @Nullable ModelAndView handleMissingServletRequestPartException(MissingServletRequestPartException ex,
  378 | 			HttpServletRequest request, HttpServletResponse response, @Nullable Object handler) throws IOException {
  379 | 
  380 | 		return null;
  381 | 	}
  382 | 
  383 | 	/**
  384 | 	 * Handle the case when an unrecoverable binding exception occurs - for example,
  385 | 	 * required header, required cookie.
  386 | 	 * <p>The default implementation returns {@code null} in which case the
  387 | 	 * exception is handled in {@link #handleErrorResponse}.
  388 | 	 * @param ex the exception to be handled
  389 | 	 * @param request current HTTP request
  390 | 	 * @param response current HTTP response
  391 | 	 * @param handler the executed handler
  392 | 	 * @return an empty {@code ModelAndView} indicating the exception was handled, or
  393 | 	 * {@code null} indicating the exception should be handled in {@link #handleErrorResponse}
  394 | 	 * @throws IOException potentially thrown from {@link HttpServletResponse#sendError}
  395 | 	 */
  396 | 	protected @Nullable ModelAndView handleServletRequestBindingException(ServletRequestBindingException ex,
  397 | 			HttpServletRequest request, HttpServletResponse response, @Nullable Object handler) throws IOException {
  398 | 
  399 | 		return null;
  400 | 	}
  401 | 
  402 | 	/**
  403 | 	 * Handle the case where an argument annotated with {@code @Valid} such as
  404 | 	 * an {@link RequestBody} or {@link RequestPart} argument fails validation.
  405 | 	 * <p>The default implementation returns {@code null} in which case the
  406 | 	 * exception is handled in {@link #handleErrorResponse}.
  407 | 	 * @param request current HTTP request
  408 | 	 * @param response current HTTP response
  409 | 	 * @param handler the executed handler
  410 | 	 * @return an empty {@code ModelAndView} indicating the exception was handled, or
  411 | 	 * {@code null} indicating the exception should be handled in {@link #handleErrorResponse}
  412 | 	 * @throws IOException potentially thrown from {@link HttpServletResponse#sendError}
  413 | 	 */
  414 | 	protected @Nullable ModelAndView handleMethodArgumentNotValidException(MethodArgumentNotValidException ex,
  415 | 			HttpServletRequest request, HttpServletResponse response, @Nullable Object handler) throws IOException {
  416 | 
  417 | 		return null;
  418 | 	}
  419 | 
  420 | 	/**
  421 | 	 * Handle the case where method validation for a controller method failed.
  422 | 	 * <p>The default implementation returns {@code null} in which case the
  423 | 	 * exception is handled in {@link #handleErrorResponse}.
  424 | 	 * @param ex the exception to be handled
  425 | 	 * @param request current HTTP request
  426 | 	 * @param response current HTTP response
  427 | 	 * @param handler the executed handler
  428 | 	 * @return an empty {@code ModelAndView} indicating the exception was handled, or
  429 | 	 * {@code null} indicating the exception should be handled in {@link #handleErrorResponse}
  430 | 	 * @throws IOException potentially thrown from {@link HttpServletResponse#sendError}
  431 | 	 * @since 6.1
  432 | 	 */
  433 | 	protected @Nullable ModelAndView handleHandlerMethodValidationException(HandlerMethodValidationException ex,
  434 | 			HttpServletRequest request, HttpServletResponse response, @Nullable Object handler) throws IOException {
  435 | 
  436 | 		return null;
  437 | 	}
  438 | 
  439 | 	/**
  440 | 	 * Handle the case where no handler was found during the dispatch.
  441 | 	 * <p>The default implementation returns {@code null} in which case the
  442 | 	 * exception is handled in {@link #handleErrorResponse}.
  443 | 	 * @param ex the NoHandlerFoundException to be handled
  444 | 	 * @param request current HTTP request
  445 | 	 * @param response current HTTP response
  446 | 	 * @param handler the executed handler, or {@code null} if none chosen
  447 | 	 * at the time of the exception (for example, if multipart resolution failed)
  448 | 	 * @return an empty {@code ModelAndView} indicating the exception was handled, or
  449 | 	 * {@code null} indicating the exception should be handled in {@link #handleErrorResponse}
  450 | 	 * @throws IOException potentially thrown from {@link HttpServletResponse#sendError}
  451 | 	 * @since 4.0
  452 | 	 */
  453 | 	protected @Nullable ModelAndView handleNoHandlerFoundException(NoHandlerFoundException ex,
  454 | 			HttpServletRequest request, HttpServletResponse response, @Nullable Object handler) throws IOException {
  455 | 
  456 | 		pageNotFoundLogger.warn(ex.getMessage());
  457 | 		return null;
  458 | 	}
  459 | 
  460 | 	/**
  461 | 	 * Handle the case where no static resource was found.
  462 | 	 * <p>The default implementation returns {@code null} in which case the
  463 | 	 * exception is handled in {@link #handleErrorResponse}.
  464 | 	 * @param ex the {@link NoResourceFoundException} to be handled
  465 | 	 * @param request current HTTP request
  466 | 	 * @param response current HTTP response
  467 | 	 * @param handler the resource handler
  468 | 	 * @return an empty {@code ModelAndView} indicating the exception was handled, or
  469 | 	 * {@code null} indicating the exception should be handled in {@link #handleErrorResponse}
  470 | 	 * @throws IOException potentially thrown from {@link HttpServletResponse#sendError}
  471 | 	 * @since 6.1
  472 | 	 */
  473 | 	protected @Nullable ModelAndView handleNoResourceFoundException(NoResourceFoundException ex,
  474 | 			HttpServletRequest request, HttpServletResponse response, @Nullable Object handler) throws IOException {
  475 | 
  476 | 		return null;
  477 | 	}
  478 | 
  479 | 	/**
  480 | 	 * Handle the case where an async request timed out.
  481 | 	 * <p>The default implementation returns {@code null} in which case the
  482 | 	 * exception is handled in {@link #handleErrorResponse}.
  483 | 	 * @param ex the {@link AsyncRequestTimeoutException} to be handled
  484 | 	 * @param request current HTTP request
  485 | 	 * @param response current HTTP response
  486 | 	 * @param handler the executed handler, or {@code null} if none chosen
  487 | 	 * at the time of the exception (for example, if multipart resolution failed)
  488 | 	 * @return an empty {@code ModelAndView} indicating the exception was handled, or
  489 | 	 * {@code null} indicating the exception should be handled in {@link #handleErrorResponse}
  490 | 	 * @throws IOException potentially thrown from {@link HttpServletResponse#sendError}
  491 | 	 * @since 4.2.8
  492 | 	 */
  493 | 	protected @Nullable ModelAndView handleAsyncRequestTimeoutException(AsyncRequestTimeoutException ex,
  494 | 			HttpServletRequest request, HttpServletResponse response, @Nullable Object handler) throws IOException {
  495 | 
  496 | 		return null;
  497 | 	}
  498 | 
  499 | 	/**
  500 | 	 * Handle the case of an I/O failure from the ServletOutputStream.
  501 | 	 * <p>By default, do nothing since the response is not usable.
  502 | 	 * @param ex the {@link AsyncRequestTimeoutException} to be handled
  503 | 	 * @param request current HTTP request
  504 | 	 * @param response current HTTP response
  505 | 	 * @param handler the executed handler, or {@code null} if none chosen
  506 | 	 * at the time of the exception (for example, if multipart resolution failed)
  507 | 	 * @return an empty ModelAndView indicating the exception was handled
  508 | 	 * @since 5.3.33
  509 | 	 */
  510 | 	protected ModelAndView handleAsyncRequestNotUsableException(AsyncRequestNotUsableException ex,
  511 | 			HttpServletRequest request, HttpServletResponse response, @Nullable Object handler) {
  512 | 
  513 | 		return new ModelAndView();
  514 | 	}
  515 | 
  516 | 	/**
  517 | 	 * Handle an Exception that indicates the client has gone away as determined
  518 | 	 * via {@link DisconnectedClientHelper}.
  519 | 	 * <p>By default, as of 7.1 this method attempts to set the response status
  520 | 	 * to 500 in case the exception is due to a connection issue to a remote host,
  521 | 	 * as part of request handling, rather than to the client.
  522 | 	 * @param ex the {@code Exception} to be handled
  523 | 	 * @param request current HTTP request
  524 | 	 * @param response current HTTP response
  525 | 	 * @param handler the executed handler, or {@code null} if none chosen
  526 | 	 * at the time of the exception (for example, if multipart resolution failed)
  527 | 	 * @return an empty ModelAndView indicating the exception was handled
  528 | 	 * @since 6.2
  529 | 	 */
  530 | 	protected ModelAndView handleDisconnectedClientException(
  531 | 			Exception ex, HttpServletRequest request, HttpServletResponse response, @Nullable Object handler) {
  532 | 
  533 | 		// Attempt to send 500 in case of onward (rather than client) connection issue
  534 | 		try {
  535 | 			sendServerError(ex, request, response);
  536 | 		}
  537 | 		catch (Exception ex2) {
  538 | 			// ignore
  539 | 		}
  540 | 		return new ModelAndView();
  541 | 	}
  542 | 
  543 | 	/**
  544 | 	 * Handle an {@link ErrorResponse} exception.
  545 | 	 * <p>The default implementation sets status and the headers of the response
  546 | 	 * to those obtained from the {@code ErrorResponse}. If available, the
  547 | 	 * {@link ProblemDetail#getDetail()} is used as the message for
  548 | 	 * {@link HttpServletResponse#sendError(int, String)}.
  549 | 	 * <p>As of 7.1, if the status is a 5xx server error, the exception is also
  550 | 	 * recorded as the error of the current HTTP server observation, if any.
  551 | 	 * @param errorResponse the exception to be handled
  552 | 	 * @param request current HTTP request
  553 | 	 * @param response current HTTP response
  554 | 	 * @param handler the executed handler
  555 | 	 * @return an empty {@code ModelAndView} indicating the exception was handled
  556 | 	 * @throws IOException potentially thrown from {@link HttpServletResponse#sendError}
  557 | 	 * @since 6.0
  558 | 	 */
  559 | 	protected ModelAndView handleErrorResponse(ErrorResponse errorResponse,
  560 | 			HttpServletRequest request, HttpServletResponse response, @Nullable Object handler) throws IOException {
  561 | 
  562 | 		if (errorResponse.getStatusCode().is5xxServerError() && errorResponse instanceof Exception ex) {
  563 | 			ServerHttpObservationFilter.recordHandledException(request, ex);
  564 | 		}
  565 | 
  566 | 		if (!response.isCommitted()) {
  567 | 			HttpHeaders headers = errorResponse.getHeaders();
  568 | 			headers.forEach((name, values) -> values.forEach(value -> response.addHeader(name, value)));
  569 | 
  570 | 			int status = errorResponse.getStatusCode().value();
  571 | 			String message = errorResponse.getBody().getDetail();
  572 | 			if (message != null) {
  573 | 				response.sendError(status, message);
  574 | 			}
  575 | 			else {
  576 | 				response.sendError(status);
  577 | 			}
  578 | 		}
  579 | 		else if (logger.isWarnEnabled()) {
  580 | 			logger.warn("Ignoring exception, response committed already: " + errorResponse);
  581 | 		}
  582 | 
  583 | 		return new ModelAndView();
  584 | 	}
  585 | 
  586 | 	/**
  587 | 	 * Handle the case when a {@link org.springframework.web.bind.WebDataBinder} conversion cannot occur.
  588 | 	 * <p>The default implementation sends an HTTP 500 error, and returns an empty {@code ModelAndView}.
  589 | 	 * Alternatively, a fallback view could be chosen, or the ConversionNotSupportedException could be
  590 | 	 * rethrown as-is.
  591 | 	 * @param ex the ConversionNotSupportedException to be handled
  592 | 	 * @param request current HTTP request
  593 | 	 * @param response current HTTP response
  594 | 	 * @param handler the executed handler
  595 | 	 * @return an empty {@code ModelAndView} indicating the exception was handled
  596 | 	 * @throws IOException potentially thrown from {@link HttpServletResponse#sendError}
  597 | 	 */
  598 | 	protected ModelAndView handleConversionNotSupported(ConversionNotSupportedException ex,
  599 | 			HttpServletRequest request, HttpServletResponse response, @Nullable Object handler) throws IOException {
  600 | 
  601 | 		sendServerError(ex, request, response);
  602 | 		return new ModelAndView();
  603 | 	}
  604 | 
  605 | 	/**
  606 | 	 * Handle the case when a {@link org.springframework.web.bind.WebDataBinder} conversion error occurs.
  607 | 	 * <p>The default implementation sends an HTTP 400 error, and returns an empty {@code ModelAndView}.
  608 | 	 * Alternatively, a fallback view could be chosen, or the TypeMismatchException could be rethrown as-is.
  609 | 	 * @param ex the TypeMismatchException to be handled
  610 | 	 * @param request current HTTP request
  611 | 	 * @param response current HTTP response
  612 | 	 * @param handler the executed handler
  613 | 	 * @return an empty {@code ModelAndView} indicating the exception was handled
  614 | 	 * @throws IOException potentially thrown from {@link HttpServletResponse#sendError}
  615 | 	 */
  616 | 	protected ModelAndView handleTypeMismatch(TypeMismatchException ex,
  617 | 			HttpServletRequest request, HttpServletResponse response, @Nullable Object handler) throws IOException {
  618 | 
  619 | 		response.sendError(HttpServletResponse.SC_BAD_REQUEST);
  620 | 		return new ModelAndView();
  621 | 	}
  622 | 
  623 | 	/**
  624 | 	 * Handle the case where a {@linkplain org.springframework.http.converter.HttpMessageConverter message converter}
  625 | 	 * cannot read from an HTTP request.
  626 | 	 * <p>The default implementation sends an HTTP 400 error, and returns an empty {@code ModelAndView}.
  627 | 	 * Alternatively, a fallback view could be chosen, or the HttpMessageNotReadableException could be
  628 | 	 * rethrown as-is.
  629 | 	 * <p>If the root cause suggests a "lost connection", handling is delegated instead to
  630 | 	 * {@link #handleDisconnectedClientException(Exception, HttpServletRequest, HttpServletResponse, Object)}.
  631 | 	 * @param ex the HttpMessageNotReadableException to be handled
  632 | 	 * @param request current HTTP request
  633 | 	 * @param response current HTTP response
  634 | 	 * @param handler the executed handler
  635 | 	 * @return an empty {@code ModelAndView} indicating the exception was handled
  636 | 	 * @throws IOException potentially thrown from {@link HttpServletResponse#sendError}
  637 | 	 */
  638 | 	protected ModelAndView handleHttpMessageNotReadable(HttpMessageNotReadableException ex,
  639 | 			HttpServletRequest request, HttpServletResponse response, @Nullable Object handler) throws IOException {
  640 | 
  641 | 		if (DisconnectedClientHelper.isClientDisconnectedException(ex)) {
  642 | 			return handleDisconnectedClientException(ex, request, response, handler);
  643 | 		}
  644 | 
  645 | 		if (!response.isCommitted()) {
  646 | 			response.sendError(HttpServletResponse.SC_BAD_REQUEST);
  647 | 		}
  648 | 		else if (logger.isWarnEnabled()) {
  649 | 			logger.warn("Ignoring exception, response committed already: " + ex);
  650 | 		}
  651 | 		return new ModelAndView();
  652 | 	}
  653 | 
  654 | 	/**
  655 | 	 * Handle the case where a
  656 | 	 * {@linkplain org.springframework.http.converter.HttpMessageConverter message converter}
  657 | 	 * cannot write to an HTTP response.
  658 | 	 * <p>The default implementation sends an HTTP 500 error, and returns an empty {@code ModelAndView}.
  659 | 	 * Alternatively, a fallback view could be chosen, or the HttpMessageNotWritableException could
  660 | 	 * be rethrown as-is.
  661 | 	 * <p>If the root cause suggests a "lost connection", handling is delegated instead to
  662 | 	 * {@link #handleDisconnectedClientException(Exception, HttpServletRequest, HttpServletResponse, Object)}.
  663 | 	 * @param ex the HttpMessageNotWritableException to be handled
  664 | 	 * @param request current HTTP request
  665 | 	 * @param response current HTTP response
  666 | 	 * @param handler the executed handler
  667 | 	 * @return an empty {@code ModelAndView} indicating the exception was handled
  668 | 	 * @throws IOException potentially thrown from {@link HttpServletResponse#sendError}
  669 | 	 */
  670 | 	protected ModelAndView handleHttpMessageNotWritable(HttpMessageNotWritableException ex,
  671 | 			HttpServletRequest request, HttpServletResponse response, @Nullable Object handler) throws IOException {
  672 | 
  673 | 		if (DisconnectedClientHelper.isClientDisconnectedException(ex)) {
  674 | 			return handleDisconnectedClientException(ex, request, response, handler);
  675 | 		}
  676 | 
  677 | 		if (!response.isCommitted()) {
  678 | 			sendServerError(ex, request, response);
  679 | 		}
  680 | 		else if (logger.isWarnEnabled()) {
  681 | 			logger.warn("Ignoring exception, response committed already: " + ex);
  682 | 		}
  683 | 		return new ModelAndView();
  684 | 	}
  685 | 
  686 | 	/**
  687 | 	 * Handle the case where method validation failed on a component that is
  688 | 	 * not a web controller, for example, on some underlying service.
  689 | 	 * <p>The default implementation sends an HTTP 500 error, and returns an empty {@code ModelAndView}.
  690 | 	 * Alternatively, a fallback view could be chosen, or the HttpMessageNotWritableException could
  691 | 	 * be rethrown as-is.
  692 | 	 * @param ex the exception to be handled
  693 | 	 * @param request current HTTP request
  694 | 	 * @param response current HTTP response
  695 | 	 * @param handler the executed handler
  696 | 	 * @return an empty {@code ModelAndView} indicating the exception was handled
  697 | 	 * @throws IOException potentially thrown from {@link HttpServletResponse#sendError}
  698 | 	 * @since 6.1
  699 | 	 */
  700 | 	protected ModelAndView handleMethodValidationException(MethodValidationException ex,
  701 | 			HttpServletRequest request, HttpServletResponse response, @Nullable Object handler) throws IOException {
  702 | 
  703 | 		sendServerError(ex, request, response);
  704 | 		return new ModelAndView();
  705 | 	}
  706 | 
  707 | 	/**
  708 | 	 * Invoked to send a server error. Sets the status to 500 and also sets the
  709 | 	 * request attribute "jakarta.servlet.error.exception" to the Exception.
  710 | 	 */
  711 | 	protected void sendServerError(Exception ex, HttpServletRequest request, HttpServletResponse response)
  712 | 			throws IOException {
  713 | 
  714 | 		request.setAttribute(WebUtils.ERROR_EXCEPTION_ATTRIBUTE, ex);
  715 | 		response.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
  716 | 	}
  717 | 
  718 | 	@Override
  719 | 	protected void logException(Exception ex, HttpServletRequest request) {
  720 | 		if (ex instanceof NoHandlerFoundException || ex instanceof NoResourceFoundException) {
  721 | 			if (logger.isDebugEnabled()) {
  722 | 				logger.debug(buildLogMessage(ex, request));
  723 | 			}
  724 | 			return;
  725 | 		}
  726 | 		super.logException(ex, request);
  727 | 	}
  728 | 
  729 | }
```
