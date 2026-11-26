<!-- learning-docs source archive（机械抓取原文，禁止手改代码内容） -->
<!-- origin: org.springframework.security:spring-security-web:7.2.0 | path: org/springframework/security/web/FilterInvocation.java -->
<!-- 正文引用：[[源码:FilterInvocation.java#L起-L止]] ；代码块头部：(src: FilterInvocation.java#L起-L止) -->

# FilterInvocation.java

| 项 | 值 |
| --- | --- |
| 来源 | `org.springframework.security:spring-security-web:7.2.0` |
| 路径 | `org/springframework/security/web/FilterInvocation.java` |
| 行数 | 363 |
| 在线地址 | https://repo1.maven.org/maven2/org/springframework/security/spring-security-web/7.2.0/spring-security-web-7.2.0-sources.jar |
| 抓取时间 | 2026-11-25T16:27:18.237Z |

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
   17 | package org.springframework.security.web;
   18 | 
   19 | import java.lang.invoke.MethodHandles;
   20 | import java.lang.invoke.MethodHandles.Lookup;
   21 | import java.lang.invoke.MethodType;
   22 | import java.lang.reflect.Constructor;
   23 | import java.lang.reflect.InvocationHandler;
   24 | import java.lang.reflect.Method;
   25 | import java.lang.reflect.Proxy;
   26 | import java.util.Collections;
   27 | import java.util.Enumeration;
   28 | import java.util.LinkedHashMap;
   29 | import java.util.List;
   30 | import java.util.Map;
   31 | 
   32 | import jakarta.servlet.FilterChain;
   33 | import jakarta.servlet.ServletContext;
   34 | import jakarta.servlet.ServletRequest;
   35 | import jakarta.servlet.ServletResponse;
   36 | import jakarta.servlet.http.HttpServletRequest;
   37 | import jakarta.servlet.http.HttpServletRequestWrapper;
   38 | import jakarta.servlet.http.HttpServletResponse;
   39 | import org.jspecify.annotations.Nullable;
   40 | 
   41 | import org.springframework.http.HttpHeaders;
   42 | import org.springframework.security.web.util.UrlUtils;
   43 | import org.springframework.util.Assert;
   44 | import org.springframework.util.StringUtils;
   45 | 
   46 | /**
   47 |  * Holds objects associated with a HTTP filter.
   48 |  * <P>
   49 |  * Guarantees the request and response are instances of <code>HttpServletRequest</code>
   50 |  * and <code>HttpServletResponse</code>, and that there are no <code>null</code> objects.
   51 |  * <p>
   52 |  * Required so that security system classes can obtain access to the filter environment,
   53 |  * as well as the request and response.
   54 |  *
   55 |  * @author Ben Alex
   56 |  * @author colin sampaleanu
   57 |  * @author Luke Taylor
   58 |  * @author Rob Winch
   59 |  */
   60 | public class FilterInvocation {
   61 | 
   62 | 	static final FilterChain DUMMY_CHAIN = (req, res) -> {
   63 | 		throw new UnsupportedOperationException("Dummy filter chain");
   64 | 	};
   65 | 
   66 | 	private final FilterChain chain;
   67 | 
   68 | 	private HttpServletRequest request;
   69 | 
   70 | 	private @Nullable HttpServletResponse response;
   71 | 
   72 | 	public FilterInvocation(ServletRequest request, ServletResponse response, FilterChain chain) {
   73 | 		Assert.isTrue(request != null && response != null && chain != null, "Cannot pass null values to constructor");
   74 | 		this.request = (HttpServletRequest) request;
   75 | 		this.response = (HttpServletResponse) response;
   76 | 		this.chain = chain;
   77 | 	}
   78 | 
   79 | 	public FilterInvocation(String servletPath, String method) {
   80 | 		this(null, servletPath, method);
   81 | 	}
   82 | 
   83 | 	public FilterInvocation(@Nullable String contextPath, String servletPath, String method) {
   84 | 		this(contextPath, servletPath, method, null);
   85 | 	}
   86 | 
   87 | 	public FilterInvocation(@Nullable String contextPath, String servletPath, @Nullable String method,
   88 | 			@Nullable ServletContext servletContext) {
   89 | 		this(contextPath, servletPath, null, null, method, servletContext);
   90 | 	}
   91 | 
   92 | 	public FilterInvocation(String contextPath, String servletPath, String pathInfo, String query, String method) {
   93 | 		this(contextPath, servletPath, pathInfo, query, method, null);
   94 | 	}
   95 | 
   96 | 	public FilterInvocation(@Nullable String contextPath, String servletPath, @Nullable String pathInfo,
   97 | 			@Nullable String query, @Nullable String method, @Nullable ServletContext servletContext) {
   98 | 		DummyRequest request = new DummyRequest();
   99 | 		contextPath = (contextPath != null) ? contextPath : "/cp";
  100 | 		request.setContextPath(contextPath);
  101 | 		request.setServletPath(servletPath);
  102 | 		request.setRequestURI(contextPath + servletPath + ((pathInfo != null) ? pathInfo : ""));
  103 | 		request.setPathInfo(pathInfo);
  104 | 		request.setQueryString(query);
  105 | 		request.setMethod(method);
  106 | 		request.setServletContext(servletContext);
  107 | 		this.request = request;
  108 | 		this.chain = DUMMY_CHAIN;
  109 | 	}
  110 | 
  111 | 	public FilterChain getChain() {
  112 | 		return this.chain;
  113 | 	}
  114 | 
  115 | 	/**
  116 | 	 * Indicates the URL that the user agent used for this request.
  117 | 	 * <p>
  118 | 	 * @return the full URL of this request
  119 | 	 */
  120 | 	public String getFullRequestUrl() {
  121 | 		return UrlUtils.buildFullRequestUrl(this.request);
  122 | 	}
  123 | 
  124 | 	public HttpServletRequest getHttpRequest() {
  125 | 		return this.request;
  126 | 	}
  127 | 
  128 | 	public @Nullable HttpServletResponse getHttpResponse() {
  129 | 		return this.response;
  130 | 	}
  131 | 
  132 | 	/**
  133 | 	 * Obtains the web application-specific fragment of the URL.
  134 | 	 * @return the URL, excluding any server name, context path or servlet path
  135 | 	 */
  136 | 	public String getRequestUrl() {
  137 | 		return UrlUtils.buildRequestUrl(this.request);
  138 | 	}
  139 | 
  140 | 	public HttpServletRequest getRequest() {
  141 | 		return getHttpRequest();
  142 | 	}
  143 | 
  144 | 	public @Nullable HttpServletResponse getResponse() {
  145 | 		return getHttpResponse();
  146 | 	}
  147 | 
  148 | 	@Override
  149 | 	public String toString() {
  150 | 		if (!StringUtils.hasLength(this.request.getMethod())) {
  151 | 			return "filter invocation [" + getRequestUrl() + "]";
  152 | 		}
  153 | 		else {
  154 | 			return "filter invocation [" + this.request.getMethod() + " " + getRequestUrl() + "]";
  155 | 		}
  156 | 	}
  157 | 
  158 | 	static class DummyRequest extends HttpServletRequestWrapper {
  159 | 
  160 | 		private static final HttpServletRequest UNSUPPORTED_REQUEST = (HttpServletRequest) Proxy.newProxyInstance(
  161 | 				DummyRequest.class.getClassLoader(), new Class[] { HttpServletRequest.class },
  162 | 				new UnsupportedOperationExceptionInvocationHandler());
  163 | 
  164 | 		private @Nullable String requestURI;
  165 | 
  166 | 		private String contextPath = "";
  167 | 
  168 | 		private @Nullable String servletPath;
  169 | 
  170 | 		private @Nullable String pathInfo;
  171 | 
  172 | 		private @Nullable String queryString;
  173 | 
  174 | 		private @Nullable String method;
  175 | 
  176 | 		private @Nullable ServletContext servletContext;
  177 | 
  178 | 		private final HttpHeaders headers = new HttpHeaders();
  179 | 
  180 | 		private final Map<String, String[]> parameters = new LinkedHashMap<>();
  181 | 
  182 | 		DummyRequest() {
  183 | 			super(UNSUPPORTED_REQUEST);
  184 | 		}
  185 | 
  186 | 		@Override
  187 | 		public String getCharacterEncoding() {
  188 | 			return "UTF-8";
  189 | 		}
  190 | 
  191 | 		@Override
  192 | 		public @Nullable Object getAttribute(String attributeName) {
  193 | 			return null;
  194 | 		}
  195 | 
  196 | 		void setRequestURI(String requestURI) {
  197 | 			this.requestURI = requestURI;
  198 | 		}
  199 | 
  200 | 		void setPathInfo(@Nullable String pathInfo) {
  201 | 			this.pathInfo = pathInfo;
  202 | 		}
  203 | 
  204 | 		@Override
  205 | 		public @Nullable String getRequestURI() {
  206 | 			return this.requestURI;
  207 | 		}
  208 | 
  209 | 		void setContextPath(String contextPath) {
  210 | 			this.contextPath = contextPath;
  211 | 		}
  212 | 
  213 | 		@Override
  214 | 		public String getContextPath() {
  215 | 			return this.contextPath;
  216 | 		}
  217 | 
  218 | 		void setServletPath(String servletPath) {
  219 | 			this.servletPath = servletPath;
  220 | 		}
  221 | 
  222 | 		@Override
  223 | 		public @Nullable String getServletPath() {
  224 | 			return this.servletPath;
  225 | 		}
  226 | 
  227 | 		void setMethod(@Nullable String method) {
  228 | 			this.method = method;
  229 | 		}
  230 | 
  231 | 		@Override
  232 | 		public @Nullable String getMethod() {
  233 | 			return this.method;
  234 | 		}
  235 | 
  236 | 		@Override
  237 | 		public @Nullable String getPathInfo() {
  238 | 			return this.pathInfo;
  239 | 		}
  240 | 
  241 | 		@Override
  242 | 		public @Nullable String getQueryString() {
  243 | 			return this.queryString;
  244 | 		}
  245 | 
  246 | 		void setQueryString(@Nullable String queryString) {
  247 | 			this.queryString = queryString;
  248 | 		}
  249 | 
  250 | 		@Override
  251 | 		public @Nullable String getServerName() {
  252 | 			return null;
  253 | 		}
  254 | 
  255 | 		@Override
  256 | 		public @Nullable String getHeader(String name) {
  257 | 			return this.headers.getFirst(name);
  258 | 		}
  259 | 
  260 | 		@Override
  261 | 		public Enumeration<String> getHeaders(String name) {
  262 | 			List<String> headerList = this.headers.get(name);
  263 | 			if (headerList == null) {
  264 | 				return Collections.emptyEnumeration();
  265 | 			}
  266 | 			return Collections.enumeration(headerList);
  267 | 		}
  268 | 
  269 | 		@Override
  270 | 		public Enumeration<String> getHeaderNames() {
  271 | 			return Collections.enumeration(this.headers.headerNames());
  272 | 		}
  273 | 
  274 | 		@Override
  275 | 		public int getIntHeader(String name) {
  276 | 			String value = this.headers.getFirst(name);
  277 | 			if (value == null) {
  278 | 				return -1;
  279 | 			}
  280 | 			return Integer.parseInt(value);
  281 | 		}
  282 | 
  283 | 		void addHeader(String name, String value) {
  284 | 			this.headers.add(name, value);
  285 | 		}
  286 | 
  287 | 		@Override
  288 | 		public @Nullable String getParameter(String name) {
  289 | 			String[] array = this.parameters.get(name);
  290 | 			return (array != null && array.length > 0) ? array[0] : null;
  291 | 		}
  292 | 
  293 | 		@Override
  294 | 		public Map<String, String[]> getParameterMap() {
  295 | 			return Collections.unmodifiableMap(this.parameters);
  296 | 		}
  297 | 
  298 | 		@Override
  299 | 		public Enumeration<String> getParameterNames() {
  300 | 			return Collections.enumeration(this.parameters.keySet());
  301 | 		}
  302 | 
  303 | 		@Override
  304 | 		public String @Nullable [] getParameterValues(String name) {
  305 | 			return this.parameters.get(name);
  306 | 		}
  307 | 
  308 | 		void setParameter(String name, String... values) {
  309 | 			this.parameters.put(name, values);
  310 | 		}
  311 | 
  312 | 		@Override
  313 | 		public @Nullable ServletContext getServletContext() {
  314 | 			return this.servletContext;
  315 | 		}
  316 | 
  317 | 		void setServletContext(@Nullable ServletContext servletContext) {
  318 | 			this.servletContext = servletContext;
  319 | 		}
  320 | 
  321 | 	}
  322 | 
  323 | 	static final class UnsupportedOperationExceptionInvocationHandler implements InvocationHandler {
  324 | 
  325 | 		private static final float JAVA_VERSION = Float.parseFloat(System.getProperty("java.class.version", "52"));
  326 | 
  327 | 		@Override
  328 | 		public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
  329 | 			if (method.isDefault()) {
  330 | 				return invokeDefaultMethod(proxy, method, args);
  331 | 			}
  332 | 			throw new UnsupportedOperationException(method + " is not supported");
  333 | 		}
  334 | 
  335 | 		private Object invokeDefaultMethod(Object proxy, Method method, Object[] args) throws Throwable {
  336 | 			if (isJdk8OrEarlier()) {
  337 | 				return invokeDefaultMethodForJdk8(proxy, method, args);
  338 | 			}
  339 | 			return MethodHandles.lookup()
  340 | 				.findSpecial(method.getDeclaringClass(), method.getName(),
  341 | 						MethodType.methodType(method.getReturnType(), new Class[0]), method.getDeclaringClass())
  342 | 				.bindTo(proxy)
  343 | 				.invokeWithArguments(args);
  344 | 		}
  345 | 
  346 | 		private Object invokeDefaultMethodForJdk8(Object proxy, Method method, Object[] args) throws Throwable {
  347 | 			Constructor<Lookup> constructor = Lookup.class.getDeclaredConstructor(Class.class);
  348 | 			constructor.setAccessible(true);
  349 | 			Class<?> clazz = method.getDeclaringClass();
  350 | 			return constructor.newInstance(clazz)
  351 | 				.in(clazz)
  352 | 				.unreflectSpecial(method, clazz)
  353 | 				.bindTo(proxy)
  354 | 				.invokeWithArguments(args);
  355 | 		}
  356 | 
  357 | 		private boolean isJdk8OrEarlier() {
  358 | 			return JAVA_VERSION <= 52;
  359 | 		}
  360 | 
  361 | 	}
  362 | 
  363 | }
```
