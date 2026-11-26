<!-- learning-docs source archive（机械抓取原文，禁止手改代码内容） -->
<!-- origin: org.springframework:spring-webmvc:7.1.0 | path: org/springframework/web/servlet/handler/AbstractHandlerExceptionResolver.java -->
<!-- 正文引用：[[源码:AbstractHandlerExceptionResolver.java#L起-L止]] ；代码块头部：(src: AbstractHandlerExceptionResolver.java#L起-L止) -->

# AbstractHandlerExceptionResolver.java

| 项 | 值 |
| --- | --- |
| 来源 | `org.springframework:spring-webmvc:7.1.0` |
| 路径 | `org/springframework/web/servlet/handler/AbstractHandlerExceptionResolver.java` |
| 行数 | 305 |
| 在线地址 | https://repo1.maven.org/maven2/org/springframework/spring-webmvc/7.1.0/spring-webmvc-7.1.0-sources.jar |
| 抓取时间 | 2026-11-25T14:16:06.248Z |

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
   17 | package org.springframework.web.servlet.handler;
   18 | 
   19 | import java.util.Set;
   20 | import java.util.function.Predicate;
   21 | 
   22 | import jakarta.servlet.http.HttpServletRequest;
   23 | import jakarta.servlet.http.HttpServletResponse;
   24 | import org.apache.commons.logging.Log;
   25 | import org.apache.commons.logging.LogFactory;
   26 | import org.jspecify.annotations.Nullable;
   27 | 
   28 | import org.springframework.core.Ordered;
   29 | import org.springframework.core.log.LogFormatUtils;
   30 | import org.springframework.util.ObjectUtils;
   31 | import org.springframework.util.StringUtils;
   32 | import org.springframework.web.servlet.HandlerExceptionResolver;
   33 | import org.springframework.web.servlet.ModelAndView;
   34 | import org.springframework.web.util.DisconnectedClientHelper;
   35 | 
   36 | /**
   37 |  * Abstract base class for {@link HandlerExceptionResolver} implementations.
   38 |  *
   39 |  * <p>Supports mapped {@linkplain #setMappedHandlers handlers} and
   40 |  * {@linkplain #setMappedHandlerClasses handler classes} that the resolver
   41 |  * should be applied to and implements the {@link Ordered} interface.
   42 |  *
   43 |  * @author Arjen Poutsma
   44 |  * @author Juergen Hoeller
   45 |  * @author Sam Brannen
   46 |  * @since 3.0
   47 |  */
   48 | public abstract class AbstractHandlerExceptionResolver implements HandlerExceptionResolver, Ordered {
   49 | 
   50 | 	private static final String HEADER_CACHE_CONTROL = "Cache-Control";
   51 | 
   52 | 	private static final String DISCONNECTED_CLIENT_LOG_CATEGORY =
   53 | 			"org.springframework.web.servlet.handler.DisconnectedClient";
   54 | 
   55 | 	private static final DisconnectedClientHelper disconnectedClientHelper =
   56 | 			new DisconnectedClientHelper(DISCONNECTED_CLIENT_LOG_CATEGORY);
   57 | 
   58 | 
   59 | 	/** Logger available to subclasses. */
   60 | 	protected final Log logger = LogFactory.getLog(getClass());
   61 | 
   62 | 	private int order = Ordered.LOWEST_PRECEDENCE;
   63 | 
   64 | 	private @Nullable Predicate<Object> mappedHandlerPredicate;
   65 | 
   66 | 	private @Nullable Set<?> mappedHandlers;
   67 | 
   68 | 	private Class<?> @Nullable [] mappedHandlerClasses;
   69 | 
   70 | 	private @Nullable Log warnLogger;
   71 | 
   72 | 	private boolean preventResponseCaching = false;
   73 | 
   74 | 
   75 | 	public void setOrder(int order) {
   76 | 		this.order = order;
   77 | 	}
   78 | 
   79 | 	@Override
   80 | 	public int getOrder() {
   81 | 		return this.order;
   82 | 	}
   83 | 
   84 | 	/**
   85 | 	 * Use a {@code Predicate} to determine which handlers this exception
   86 | 	 * resolver applies to, including when the request was not mapped in which
   87 | 	 * case the handler is {@code null}.
   88 | 	 * <p>If no handler predicate, nor handlers, nor handler classes are set,
   89 | 	 * the exception resolver applies to all handlers.
   90 | 	 * @since 6.1.2
   91 | 	 */
   92 | 	public void setMappedHandlerPredicate(Predicate<Object> predicate) {
   93 | 		this.mappedHandlerPredicate =
   94 | 				(this.mappedHandlerPredicate != null ? this.mappedHandlerPredicate.and(predicate) : predicate);
   95 | 	}
   96 | 
   97 | 	/**
   98 | 	 * Specify the set of handlers that this exception resolver should apply to.
   99 | 	 * <p>If no handler predicate, nor handlers, nor handler classes are set,
  100 | 	 * the exception resolver applies to all handlers.
  101 | 	 * @see #setMappedHandlerPredicate(Predicate)
  102 | 	 */
  103 | 	public void setMappedHandlers(Set<?> mappedHandlers) {
  104 | 		this.mappedHandlers = mappedHandlers;
  105 | 	}
  106 | 
  107 | 	/**
  108 | 	 * Specify the set of classes that this exception resolver should apply to.
  109 | 	 * The resolver will only apply to handlers of the specified types; the
  110 | 	 * specified types may be interfaces or superclasses of handlers as well.
  111 | 	 * <p>If no handler predicate, nor handlers, nor handler classes are set,
  112 | 	 * the exception resolver applies to all handlers.
  113 | 	 * @see #setMappedHandlerPredicate(Predicate)
  114 | 	 */
  115 | 	public void setMappedHandlerClasses(Class<?>... mappedHandlerClasses) {
  116 | 		this.mappedHandlerClasses = mappedHandlerClasses;
  117 | 	}
  118 | 
  119 | 	/**
  120 | 	 * Alternative to {@link #setMappedHandlerClasses(Class[])}.
  121 | 	 * @since 6.1
  122 | 	 */
  123 | 	public void addMappedHandlerClass(Class<?> mappedHandlerClass) {
  124 | 		this.mappedHandlerClasses = (this.mappedHandlerClasses != null ?
  125 | 				ObjectUtils.addObjectToArray(this.mappedHandlerClasses, mappedHandlerClass) :
  126 | 				new Class<?>[] {mappedHandlerClass});
  127 | 	}
  128 | 
  129 | 	/**
  130 | 	 * Return the {@link #setMappedHandlerClasses(Class[]) configured} mapped
  131 | 	 * handler classes.
  132 | 	 */
  133 | 	protected Class<?> @Nullable [] getMappedHandlerClasses() {
  134 | 		return this.mappedHandlerClasses;
  135 | 	}
  136 | 
  137 | 	/**
  138 | 	 * Set the log category for warn logging. The name will be passed to the underlying logger
  139 | 	 * implementation through Commons Logging, getting interpreted as a log category according
  140 | 	 * to the logger's configuration. If {@code null} or empty String is passed, warn logging
  141 | 	 * is turned off.
  142 | 	 * <p>By default there is no warn logging although subclasses like
  143 | 	 * {@link org.springframework.web.servlet.mvc.support.DefaultHandlerExceptionResolver}
  144 | 	 * can change that default. Specify this setting to activate warn logging into a specific
  145 | 	 * category. Alternatively, override the {@link #logException} method for custom logging.
  146 | 	 * @see org.apache.commons.logging.LogFactory#getLog(String)
  147 | 	 * @see java.util.logging.Logger#getLogger(String)
  148 | 	 */
  149 | 	public void setWarnLogCategory(String loggerName) {
  150 | 		this.warnLogger = (StringUtils.hasLength(loggerName) ? LogFactory.getLog(loggerName) : null);
  151 | 	}
  152 | 
  153 | 	/**
  154 | 	 * Specify whether to prevent HTTP response caching for any view resolved
  155 | 	 * by this exception resolver.
  156 | 	 * <p>Default is {@code false}. Switch this to {@code true} in order to
  157 | 	 * automatically generate HTTP response headers that suppress response caching.
  158 | 	 */
  159 | 	public void setPreventResponseCaching(boolean preventResponseCaching) {
  160 | 		this.preventResponseCaching = preventResponseCaching;
  161 | 	}
  162 | 
  163 | 
  164 | 	/**
  165 | 	 * Check whether this resolver is supposed to apply (i.e. if the supplied handler
  166 | 	 * matches any of the configured {@linkplain #setMappedHandlers handlers} or
  167 | 	 * {@linkplain #setMappedHandlerClasses handler classes}), and then delegate
  168 | 	 * to the {@link #doResolveException} template method.
  169 | 	 */
  170 | 	@Override
  171 | 	public @Nullable ModelAndView resolveException(
  172 | 			HttpServletRequest request, HttpServletResponse response, @Nullable Object handler, Exception ex) {
  173 | 
  174 | 		if (shouldApplyTo(request, handler)) {
  175 | 			prepareResponse(ex, response);
  176 | 			ModelAndView result = doResolveException(request, response, handler, ex);
  177 | 			if (result != null && !disconnectedClientHelper.checkAndLogClientDisconnectedException(ex)) {
  178 | 				// Print debug message when warn logger is not enabled.
  179 | 				if (logger.isDebugEnabled() && (this.warnLogger == null || !this.warnLogger.isWarnEnabled())) {
  180 | 					logger.debug(buildLogMessage(ex, request) + (result.isEmpty() ? "" : " to " + result));
  181 | 				}
  182 | 				// Explicitly configured warn logger in logException method.
  183 | 				logException(ex, request);
  184 | 			}
  185 | 			return result;
  186 | 		}
  187 | 		else {
  188 | 			return null;
  189 | 		}
  190 | 	}
  191 | 
  192 | 	/**
  193 | 	 * Check whether this resolver is supposed to apply to the given handler.
  194 | 	 * <p>The default implementation checks against the configured
  195 | 	 * {@linkplain #setMappedHandlerPredicate(Predicate) handlerPredicate}
  196 | 	 * {@linkplain #setMappedHandlers handlers} and
  197 | 	 * {@linkplain #setMappedHandlerClasses handler classes}, if any.
  198 | 	 * @param request current HTTP request
  199 | 	 * @param handler the executed handler, or {@code null} if none chosen
  200 | 	 * at the time of the exception (for example, if multipart resolution failed)
  201 | 	 * @return whether this resolved should proceed with resolving the exception
  202 | 	 * for the given request and handler
  203 | 	 * @see #setMappedHandlers
  204 | 	 * @see #setMappedHandlerClasses
  205 | 	 */
  206 | 	protected boolean shouldApplyTo(HttpServletRequest request, @Nullable Object handler) {
  207 | 		if (this.mappedHandlerPredicate != null) {
  208 | 			return this.mappedHandlerPredicate.test(handler);
  209 | 		}
  210 | 		if (handler != null) {
  211 | 			if (this.mappedHandlers != null && this.mappedHandlers.contains(handler)) {
  212 | 				return true;
  213 | 			}
  214 | 			if (this.mappedHandlerClasses != null) {
  215 | 				for (Class<?> handlerClass : this.mappedHandlerClasses) {
  216 | 					if (handlerClass.isInstance(handler)) {
  217 | 						return true;
  218 | 					}
  219 | 				}
  220 | 			}
  221 | 		}
  222 | 		return !hasHandlerMappings();
  223 | 	}
  224 | 
  225 | 	/**
  226 | 	 * Whether there are any handler mappings registered via
  227 | 	 * {@link #setMappedHandlers(Set)}, {@link #setMappedHandlerClasses(Class[])}, or
  228 | 	 * {@link #setMappedHandlerPredicate(Predicate)}.
  229 | 	 * @since 5.3
  230 | 	 */
  231 | 	protected boolean hasHandlerMappings() {
  232 | 		return (this.mappedHandlers != null || this.mappedHandlerClasses != null ||
  233 | 				this.mappedHandlerPredicate != null);
  234 | 	}
  235 | 
  236 | 	/**
  237 | 	 * Log the given exception at warn level, provided that warn logging has been
  238 | 	 * activated through the {@link #setWarnLogCategory "warnLogCategory"} property.
  239 | 	 * <p>Calls {@link #buildLogMessage} in order to determine the concrete message to log.
  240 | 	 * @param ex the exception that got thrown during handler execution
  241 | 	 * @param request current HTTP request (useful for obtaining metadata)
  242 | 	 * @see #setWarnLogCategory
  243 | 	 * @see #buildLogMessage
  244 | 	 * @see org.apache.commons.logging.Log#warn(Object, Throwable)
  245 | 	 */
  246 | 	protected void logException(Exception ex, HttpServletRequest request) {
  247 | 		if (this.warnLogger != null && this.warnLogger.isWarnEnabled()) {
  248 | 			this.warnLogger.warn(buildLogMessage(ex, request));
  249 | 		}
  250 | 	}
  251 | 
  252 | 	/**
  253 | 	 * Build a log message for the given exception, occurred during processing the given request.
  254 | 	 * @param ex the exception that got thrown during handler execution
  255 | 	 * @param request current HTTP request (useful for obtaining metadata)
  256 | 	 * @return the log message to use
  257 | 	 */
  258 | 	protected String buildLogMessage(Exception ex, HttpServletRequest request) {
  259 | 		return "Resolved [" + LogFormatUtils.formatValue(ex, -1, true) + "]";
  260 | 	}
  261 | 
  262 | 	/**
  263 | 	 * Prepare the response for the exceptional case.
  264 | 	 * <p>The default implementation prevents the response from being cached,
  265 | 	 * if the {@link #setPreventResponseCaching "preventResponseCaching"} property
  266 | 	 * has been set to "true".
  267 | 	 * @param ex the exception that got thrown during handler execution
  268 | 	 * @param response current HTTP response
  269 | 	 * @see #preventCaching
  270 | 	 */
  271 | 	protected void prepareResponse(Exception ex, HttpServletResponse response) {
  272 | 		if (this.preventResponseCaching) {
  273 | 			preventCaching(response);
  274 | 		}
  275 | 	}
  276 | 
  277 | 	/**
  278 | 	 * Prevents the response from being cached, through setting corresponding
  279 | 	 * HTTP {@code Cache-Control: no-store} header.
  280 | 	 * @param response current HTTP response
  281 | 	 */
  282 | 	protected void preventCaching(HttpServletResponse response) {
  283 | 		response.addHeader(HEADER_CACHE_CONTROL, "no-store");
  284 | 	}
  285 | 
  286 | 
  287 | 	/**
  288 | 	 * Actually resolve the given exception that got thrown during handler execution,
  289 | 	 * returning a {@link ModelAndView} that represents a specific error page if appropriate.
  290 | 	 * <p>May be overridden in subclasses, in order to apply specific exception checks.
  291 | 	 * Note that this template method will be invoked <i>after</i> checking whether this
  292 | 	 * resolver applies ("mappedHandlers" etc), so an implementation may simply proceed
  293 | 	 * with its actual exception handling.
  294 | 	 * @param request current HTTP request
  295 | 	 * @param response current HTTP response
  296 | 	 * @param handler the executed handler, or {@code null} if none chosen at the time
  297 | 	 * of the exception (for example, if multipart resolution failed)
  298 | 	 * @param ex the exception that got thrown during handler execution
  299 | 	 * @return a corresponding {@code ModelAndView} to forward to,
  300 | 	 * or {@code null} for default processing in the resolution chain
  301 | 	 */
  302 | 	protected abstract @Nullable ModelAndView doResolveException(
  303 | 			HttpServletRequest request, HttpServletResponse response, @Nullable Object handler, Exception ex);
  304 | 
  305 | }
```
