<!-- learning-docs source archive（机械抓取原文，禁止手改代码内容） -->
<!-- origin: org.springframework:spring-web:7.1.0 | path: org/springframework/web/context/request/ServletWebRequest.java -->
<!-- 正文引用：[[源码:ServletWebRequest.java#L起-L止]] ；代码块头部：(src: ServletWebRequest.java#L起-L止) -->

# ServletWebRequest.java

| 项 | 值 |
| --- | --- |
| 来源 | `org.springframework:spring-web:7.1.0` |
| 路径 | `org/springframework/web/context/request/ServletWebRequest.java` |
| 行数 | 393 |
| 在线地址 | https://repo1.maven.org/maven2/org/springframework/spring-web/7.1.0/spring-web-7.1.0-sources.jar |
| 抓取时间 | 2026-11-25T14:05:28.778Z |

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
   17 | package org.springframework.web.context.request;
   18 | 
   19 | import java.security.Principal;
   20 | import java.text.ParseException;
   21 | import java.text.SimpleDateFormat;
   22 | import java.util.Enumeration;
   23 | import java.util.Iterator;
   24 | import java.util.Locale;
   25 | import java.util.Map;
   26 | import java.util.Set;
   27 | import java.util.TimeZone;
   28 | 
   29 | import jakarta.servlet.http.HttpServletRequest;
   30 | import jakarta.servlet.http.HttpServletResponse;
   31 | import jakarta.servlet.http.HttpSession;
   32 | import org.jspecify.annotations.Nullable;
   33 | 
   34 | import org.springframework.http.ETag;
   35 | import org.springframework.http.HttpHeaders;
   36 | import org.springframework.http.HttpMethod;
   37 | import org.springframework.http.HttpStatus;
   38 | import org.springframework.util.CollectionUtils;
   39 | import org.springframework.util.ObjectUtils;
   40 | import org.springframework.util.StringUtils;
   41 | import org.springframework.web.util.WebUtils;
   42 | 
   43 | /**
   44 |  * {@link WebRequest} adapter for an {@link jakarta.servlet.http.HttpServletRequest}.
   45 |  *
   46 |  * @author Juergen Hoeller
   47 |  * @author Brian Clozel
   48 |  * @author Markus Malkusch
   49 |  * @since 2.0
   50 |  */
   51 | public class ServletWebRequest extends ServletRequestAttributes implements NativeWebRequest {
   52 | 
   53 | 	private static final Set<String> SAFE_METHODS = Set.of("GET", "HEAD");
   54 | 
   55 | 	/**
   56 | 	 * Date formats as specified in the HTTP RFC.
   57 | 	 * @see <a href="https://tools.ietf.org/html/rfc7231#section-7.1.1.1">Section 7.1.1.1 of RFC 7231</a>
   58 | 	 */
   59 | 	private static final String[] DATE_FORMATS = new String[] {
   60 | 			"EEE, dd MMM yyyy HH:mm:ss zzz",
   61 | 			"EEE, dd-MMM-yy HH:mm:ss zzz",
   62 | 			"EEE MMM dd HH:mm:ss yyyy"
   63 | 	};
   64 | 
   65 | 	private static final TimeZone GMT = TimeZone.getTimeZone("GMT");
   66 | 
   67 | 	private boolean notModified = false;
   68 | 
   69 | 
   70 | 	/**
   71 | 	 * Create a new ServletWebRequest instance for the given request.
   72 | 	 * @param request current HTTP request
   73 | 	 */
   74 | 	public ServletWebRequest(HttpServletRequest request) {
   75 | 		super(request);
   76 | 	}
   77 | 
   78 | 	/**
   79 | 	 * Create a new ServletWebRequest instance for the given request/response pair.
   80 | 	 * @param request current HTTP request
   81 | 	 * @param response current HTTP response (for automatic last-modified handling)
   82 | 	 */
   83 | 	public ServletWebRequest(HttpServletRequest request, @Nullable HttpServletResponse response) {
   84 | 		super(request, response);
   85 | 	}
   86 | 
   87 | 
   88 | 	@Override
   89 | 	public Object getNativeRequest() {
   90 | 		return getRequest();
   91 | 	}
   92 | 
   93 | 	@Override
   94 | 	public @Nullable Object getNativeResponse() {
   95 | 		return getResponse();
   96 | 	}
   97 | 
   98 | 	@Override
   99 | 	public <T> @Nullable T getNativeRequest(@Nullable Class<T> requiredType) {
  100 | 		return WebUtils.getNativeRequest(getRequest(), requiredType);
  101 | 	}
  102 | 
  103 | 	@Override
  104 | 	public <T> @Nullable T getNativeResponse(@Nullable Class<T> requiredType) {
  105 | 		HttpServletResponse response = getResponse();
  106 | 		return (response != null ? WebUtils.getNativeResponse(response, requiredType) : null);
  107 | 	}
  108 | 
  109 | 	/**
  110 | 	 * Return the HTTP method of the request.
  111 | 	 * @since 4.0.2
  112 | 	 */
  113 | 	public HttpMethod getHttpMethod() {
  114 | 		return HttpMethod.valueOf(getRequest().getMethod());
  115 | 	}
  116 | 
  117 | 	@Override
  118 | 	public @Nullable String getHeader(String headerName) {
  119 | 		return getRequest().getHeader(headerName);
  120 | 	}
  121 | 
  122 | 	@Override
  123 | 	public String @Nullable [] getHeaderValues(String headerName) {
  124 | 		String[] headerValues = StringUtils.toStringArray(getRequest().getHeaders(headerName));
  125 | 		return (!ObjectUtils.isEmpty(headerValues) ? headerValues : null);
  126 | 	}
  127 | 
  128 | 	@Override
  129 | 	public Iterator<String> getHeaderNames() {
  130 | 		return CollectionUtils.toIterator(getRequest().getHeaderNames());
  131 | 	}
  132 | 
  133 | 	@Override
  134 | 	public @Nullable String getParameter(String paramName) {
  135 | 		return getRequest().getParameter(paramName);
  136 | 	}
  137 | 
  138 | 	@Override
  139 | 	public String @Nullable [] getParameterValues(String paramName) {
  140 | 		return getRequest().getParameterValues(paramName);
  141 | 	}
  142 | 
  143 | 	@Override
  144 | 	public Iterator<String> getParameterNames() {
  145 | 		return CollectionUtils.toIterator(getRequest().getParameterNames());
  146 | 	}
  147 | 
  148 | 	@Override
  149 | 	public Map<String, String[]> getParameterMap() {
  150 | 		return getRequest().getParameterMap();
  151 | 	}
  152 | 
  153 | 	@Override
  154 | 	public Locale getLocale() {
  155 | 		return getRequest().getLocale();
  156 | 	}
  157 | 
  158 | 	@Override
  159 | 	public String getContextPath() {
  160 | 		return getRequest().getContextPath();
  161 | 	}
  162 | 
  163 | 	@Override
  164 | 	public @Nullable String getRemoteUser() {
  165 | 		return getRequest().getRemoteUser();
  166 | 	}
  167 | 
  168 | 	@Override
  169 | 	public @Nullable Principal getUserPrincipal() {
  170 | 		return getRequest().getUserPrincipal();
  171 | 	}
  172 | 
  173 | 	@Override
  174 | 	public boolean isUserInRole(String role) {
  175 | 		return getRequest().isUserInRole(role);
  176 | 	}
  177 | 
  178 | 	@Override
  179 | 	public boolean isSecure() {
  180 | 		return getRequest().isSecure();
  181 | 	}
  182 | 
  183 | 
  184 | 	@Override
  185 | 	public boolean checkNotModified(long lastModifiedTimestamp) {
  186 | 		return checkNotModified(null, lastModifiedTimestamp);
  187 | 	}
  188 | 
  189 | 	@Override
  190 | 	public boolean checkNotModified(String etag) {
  191 | 		return checkNotModified(etag, -1);
  192 | 	}
  193 | 
  194 | 	@Override
  195 | 	public boolean checkNotModified(@Nullable String etag, long lastModifiedTimestamp) {
  196 | 		if (this.notModified) {
  197 | 			return true;
  198 | 		}
  199 | 
  200 | 		HttpServletResponse response = getResponse();
  201 | 		if (response != null && HttpStatus.OK.value() != response.getStatus()) {
  202 | 			return false;
  203 | 		}
  204 | 
  205 | 		// Evaluate conditions in order of precedence.
  206 | 		// See https://datatracker.ietf.org/doc/html/rfc9110#section-13.2.2
  207 | 		if (validateIfMatch(etag)) {
  208 | 			updateResponseStateChanging(etag, lastModifiedTimestamp);
  209 | 			return this.notModified;
  210 | 		}
  211 | 		// 2) If-Unmodified-Since
  212 | 		if (validateIfUnmodifiedSince(lastModifiedTimestamp)) {
  213 | 			updateResponseStateChanging(etag, lastModifiedTimestamp);
  214 | 			return this.notModified;
  215 | 		}
  216 | 		// 3) If-None-Match
  217 | 		if (!validateIfNoneMatch(etag)) {
  218 | 			// 4) If-Modified-Since
  219 | 			validateIfModifiedSince(lastModifiedTimestamp);
  220 | 		}
  221 | 		updateResponseIdempotent(etag, lastModifiedTimestamp);
  222 | 		return this.notModified;
  223 | 	}
  224 | 
  225 | 	private boolean validateIfMatch(@Nullable String etag) {
  226 | 		if (SAFE_METHODS.contains(getRequest().getMethod())) {
  227 | 			return false;
  228 | 		}
  229 | 		Enumeration<String> ifMatchHeaders = getRequest().getHeaders(HttpHeaders.IF_MATCH);
  230 | 		if (!ifMatchHeaders.hasMoreElements()) {
  231 | 			return false;
  232 | 		}
  233 | 		this.notModified = matchRequestedETags(ifMatchHeaders, etag, false);
  234 | 		return true;
  235 | 	}
  236 | 
  237 | 	private boolean validateIfNoneMatch(@Nullable String etag) {
  238 | 		Enumeration<String> ifNoneMatchHeaders = getRequest().getHeaders(HttpHeaders.IF_NONE_MATCH);
  239 | 		if (!ifNoneMatchHeaders.hasMoreElements()) {
  240 | 			return false;
  241 | 		}
  242 | 		this.notModified = !matchRequestedETags(ifNoneMatchHeaders, etag, true);
  243 | 		return true;
  244 | 	}
  245 | 
  246 | 	private boolean matchRequestedETags(Enumeration<String> requestedETags, @Nullable String tag, boolean weakCompare) {
  247 | 		if (StringUtils.hasLength(tag)) {
  248 | 			ETag eTag = ETag.create(tag);
  249 | 			boolean isNotSafeMethod = !SAFE_METHODS.contains(getRequest().getMethod());
  250 | 			while (requestedETags.hasMoreElements()) {
  251 | 				// Compare weak/strong ETags as per https://datatracker.ietf.org/doc/html/rfc9110#section-8.8.3
  252 | 				for (ETag requestedETag : ETag.parse(requestedETags.nextElement())) {
  253 | 					// only consider "lost updates" checks for unsafe HTTP methods
  254 | 					if (requestedETag.isWildcard() && isNotSafeMethod) {
  255 | 						return false;
  256 | 					}
  257 | 					if (requestedETag.compare(eTag, !weakCompare)) {
  258 | 						return false;
  259 | 					}
  260 | 				}
  261 | 			}
  262 | 		}
  263 | 		return true;
  264 | 	}
  265 | 
  266 | 	private void updateResponseStateChanging(@Nullable String etag, long lastModifiedTimestamp) {
  267 | 		if (this.notModified && getResponse() != null) {
  268 | 			getResponse().setStatus(HttpStatus.PRECONDITION_FAILED.value());
  269 | 		}
  270 | 		else {
  271 | 			addCachingResponseHeaders(etag, lastModifiedTimestamp);
  272 | 		}
  273 | 	}
  274 | 
  275 | 	private boolean validateIfUnmodifiedSince(long lastModifiedTimestamp) {
  276 | 		if (lastModifiedTimestamp < 0) {
  277 | 			return false;
  278 | 		}
  279 | 		long ifUnmodifiedSince = parseDateHeader(HttpHeaders.IF_UNMODIFIED_SINCE);
  280 | 		if (ifUnmodifiedSince == -1) {
  281 | 			return false;
  282 | 		}
  283 | 		this.notModified = (ifUnmodifiedSince < (lastModifiedTimestamp / 1000 * 1000));
  284 | 		return true;
  285 | 	}
  286 | 
  287 | 	private void validateIfModifiedSince(long lastModifiedTimestamp) {
  288 | 		if (lastModifiedTimestamp < 0) {
  289 | 			return;
  290 | 		}
  291 | 		long ifModifiedSince = parseDateHeader(HttpHeaders.IF_MODIFIED_SINCE);
  292 | 		if (ifModifiedSince != -1) {
  293 | 			// We will perform this validation...
  294 | 			this.notModified = ifModifiedSince >= (lastModifiedTimestamp / 1000 * 1000);
  295 | 		}
  296 | 	}
  297 | 
  298 | 	private void updateResponseIdempotent(@Nullable String etag, long lastModifiedTimestamp) {
  299 | 		if (getResponse() != null) {
  300 | 			boolean isHttpGetOrHead = SAFE_METHODS.contains(getRequest().getMethod());
  301 | 			if (this.notModified) {
  302 | 				getResponse().setStatus(isHttpGetOrHead ?
  303 | 						HttpStatus.NOT_MODIFIED.value() : HttpStatus.PRECONDITION_FAILED.value());
  304 | 			}
  305 | 			addCachingResponseHeaders(etag, lastModifiedTimestamp);
  306 | 		}
  307 | 	}
  308 | 
  309 | 	private void addCachingResponseHeaders(@Nullable String eTag, long lastModifiedTimestamp) {
  310 | 		if (getResponse() != null && SAFE_METHODS.contains(getRequest().getMethod())) {
  311 | 			if (lastModifiedTimestamp > 0 && parseDateValue(getResponse().getHeader(HttpHeaders.LAST_MODIFIED)) == -1) {
  312 | 				getResponse().setDateHeader(HttpHeaders.LAST_MODIFIED, lastModifiedTimestamp);
  313 | 			}
  314 | 			if (StringUtils.hasLength(eTag) && getResponse().getHeader(HttpHeaders.ETAG) == null) {
  315 | 				getResponse().setHeader(HttpHeaders.ETAG, ETag.quoteETagIfNecessary(eTag));
  316 | 			}
  317 | 		}
  318 | 	}
  319 | 
  320 | 	public boolean isNotModified() {
  321 | 		return this.notModified;
  322 | 	}
  323 | 
  324 | 	private long parseDateHeader(String headerName) {
  325 | 		long dateValue = -1;
  326 | 		try {
  327 | 			dateValue = getRequest().getDateHeader(headerName);
  328 | 		}
  329 | 		catch (IllegalArgumentException ex) {
  330 | 			String headerValue = getHeader(headerName);
  331 | 			// Possibly an IE 10 style value: "Wed, 09 Apr 2014 09:57:42 GMT; length=13774"
  332 | 			if (headerValue != null) {
  333 | 				int separatorIndex = headerValue.indexOf(';');
  334 | 				if (separatorIndex != -1) {
  335 | 					String datePart = headerValue.substring(0, separatorIndex);
  336 | 					dateValue = parseDateValue(datePart);
  337 | 				}
  338 | 			}
  339 | 		}
  340 | 		return dateValue;
  341 | 	}
  342 | 
  343 | 	private long parseDateValue(@Nullable String headerValue) {
  344 | 		if (headerValue == null) {
  345 | 			// No header value sent at all
  346 | 			return -1;
  347 | 		}
  348 | 		if (headerValue.length() >= 3) {
  349 | 			// Short "0" or "-1" like values are never valid HTTP date headers...
  350 | 			// Let's only bother with SimpleDateFormat parsing for long enough values.
  351 | 			for (String dateFormat : DATE_FORMATS) {
  352 | 				SimpleDateFormat simpleDateFormat = new SimpleDateFormat(dateFormat, Locale.US);
  353 | 				simpleDateFormat.setTimeZone(GMT);
  354 | 				try {
  355 | 					return simpleDateFormat.parse(headerValue).getTime();
  356 | 				}
  357 | 				catch (ParseException ex) {
  358 | 					// ignore
  359 | 				}
  360 | 			}
  361 | 		}
  362 | 		return -1;
  363 | 	}
  364 | 
  365 | 	@Override
  366 | 	public String getDescription(boolean includeClientInfo) {
  367 | 		HttpServletRequest request = getRequest();
  368 | 		StringBuilder sb = new StringBuilder();
  369 | 		sb.append("uri=").append(request.getRequestURI());
  370 | 		if (includeClientInfo) {
  371 | 			String client = request.getRemoteAddr();
  372 | 			if (StringUtils.hasLength(client)) {
  373 | 				sb.append(";client=").append(client);
  374 | 			}
  375 | 			HttpSession session = request.getSession(false);
  376 | 			if (session != null) {
  377 | 				sb.append(";session=").append(session.getId());
  378 | 			}
  379 | 			String user = request.getRemoteUser();
  380 | 			if (StringUtils.hasLength(user)) {
  381 | 				sb.append(";user=").append(user);
  382 | 			}
  383 | 		}
  384 | 		return sb.toString();
  385 | 	}
  386 | 
  387 | 
  388 | 	@Override
  389 | 	public String toString() {
  390 | 		return "ServletWebRequest: " + getDescription(true);
  391 | 	}
  392 | 
  393 | }
```
