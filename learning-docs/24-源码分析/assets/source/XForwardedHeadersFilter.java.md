<!-- learning-docs source archive（机械抓取原文，禁止手改代码内容） -->
<!-- origin: org.springframework.cloud:spring-cloud-gateway-server-webflux:5.1.0 | path: org/springframework/cloud/gateway/filter/headers/XForwardedHeadersFilter.java -->
<!-- 正文引用：[[源码:XForwardedHeadersFilter.java#L起-L止]] ；代码块头部：(src: XForwardedHeadersFilter.java#L起-L止) -->

# XForwardedHeadersFilter.java

| 项 | 值 |
| --- | --- |
| 来源 | `org.springframework.cloud:spring-cloud-gateway-server-webflux:5.1.0` |
| 路径 | `org/springframework/cloud/gateway/filter/headers/XForwardedHeadersFilter.java` |
| 行数 | 377 |
| 在线地址 | https://repo1.maven.org/maven2/org/springframework/cloud/spring-cloud-gateway-server-webflux/5.1.0/spring-cloud-gateway-server-webflux-5.1.0-sources.jar |
| 抓取时间 | 2026-11-25T15:33:49.274Z |

```java
    1 | /*
    2 |  * Copyright 2013-present the original author or authors.
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
   17 | package org.springframework.cloud.gateway.filter.headers;
   18 | 
   19 | import java.net.URI;
   20 | import java.util.LinkedHashSet;
   21 | import java.util.List;
   22 | import java.util.Map;
   23 | import java.util.function.Predicate;
   24 | 
   25 | import org.apache.commons.logging.Log;
   26 | import org.apache.commons.logging.LogFactory;
   27 | import org.jspecify.annotations.Nullable;
   28 | 
   29 | import org.springframework.boot.context.properties.ConfigurationProperties;
   30 | import org.springframework.cloud.gateway.config.GatewayProperties;
   31 | import org.springframework.core.Ordered;
   32 | import org.springframework.core.log.LogMessage;
   33 | import org.springframework.http.HttpHeaders;
   34 | import org.springframework.http.server.reactive.ServerHttpRequest;
   35 | import org.springframework.util.ObjectUtils;
   36 | import org.springframework.util.StringUtils;
   37 | import org.springframework.web.server.ServerWebExchange;
   38 | 
   39 | import static org.springframework.cloud.gateway.filter.headers.RemoveXForwardedHeadersFilter.removeXForwardedHeaders;
   40 | import static org.springframework.cloud.gateway.support.ServerWebExchangeUtils.GATEWAY_ORIGINAL_REQUEST_URL_ATTR;
   41 | import static org.springframework.cloud.gateway.support.ServerWebExchangeUtils.GATEWAY_REQUEST_URL_ATTR;
   42 | 
   43 | @ConfigurationProperties("spring.cloud.gateway.server.webflux.x-forwarded")
   44 | public class XForwardedHeadersFilter implements HttpHeadersFilter, Ordered {
   45 | 
   46 | 	private static final Log log = LogFactory.getLog(XForwardedHeadersFilter.class);
   47 | 
   48 | 	/** Default http port. */
   49 | 	public static final int HTTP_PORT = 80;
   50 | 
   51 | 	/** Default https port. */
   52 | 	public static final int HTTPS_PORT = 443;
   53 | 
   54 | 	/** Http url scheme. */
   55 | 	public static final String HTTP_SCHEME = "http";
   56 | 
   57 | 	/** Https url scheme. */
   58 | 	public static final String HTTPS_SCHEME = "https";
   59 | 
   60 | 	/** X-Forwarded-For Header. */
   61 | 	public static final String X_FORWARDED_FOR_HEADER = "X-Forwarded-For";
   62 | 
   63 | 	/** X-Forwarded-Host Header. */
   64 | 	public static final String X_FORWARDED_HOST_HEADER = "X-Forwarded-Host";
   65 | 
   66 | 	/** X-Forwarded-Port Header. */
   67 | 	public static final String X_FORWARDED_PORT_HEADER = "X-Forwarded-Port";
   68 | 
   69 | 	/** X-Forwarded-Proto Header. */
   70 | 	public static final String X_FORWARDED_PROTO_HEADER = "X-Forwarded-Proto";
   71 | 
   72 | 	/** X-Forwarded-Prefix Header. */
   73 | 	public static final String X_FORWARDED_PREFIX_HEADER = "X-Forwarded-Prefix";
   74 | 
   75 | 	/** The order of the XForwardedHeadersFilter. */
   76 | 	private int order = 0;
   77 | 
   78 | 	/** If the XForwardedHeadersFilter is enabled. */
   79 | 	private boolean enabled = true;
   80 | 
   81 | 	/** If X-Forwarded-For is enabled. */
   82 | 	private boolean forEnabled = true;
   83 | 
   84 | 	/** If X-Forwarded-Host is enabled. */
   85 | 	private boolean hostEnabled = true;
   86 | 
   87 | 	/** If X-Forwarded-Port is enabled. */
   88 | 	private boolean portEnabled = true;
   89 | 
   90 | 	/** If X-Forwarded-Proto is enabled. */
   91 | 	private boolean protoEnabled = true;
   92 | 
   93 | 	/** If X-Forwarded-Prefix is enabled. */
   94 | 	private boolean prefixEnabled = true;
   95 | 
   96 | 	/** If appending X-Forwarded-For as a list is enabled. */
   97 | 	private boolean forAppend = true;
   98 | 
   99 | 	/** If appending X-Forwarded-Host as a list is enabled. */
  100 | 	private boolean hostAppend = true;
  101 | 
  102 | 	/** If appending X-Forwarded-Port as a list is enabled. */
  103 | 	private boolean portAppend = true;
  104 | 
  105 | 	/** If appending X-Forwarded-Proto as a list is enabled. */
  106 | 	private boolean protoAppend = true;
  107 | 
  108 | 	/** If appending X-Forwarded-Prefix as a list is enabled. */
  109 | 	private boolean prefixAppend = true;
  110 | 
  111 | 	private final TrustedProxies trustedProxies;
  112 | 
  113 | 	@Deprecated
  114 | 	public XForwardedHeadersFilter() {
  115 | 		trustedProxies = s -> true;
  116 | 		log.warn(GatewayProperties.PREFIX
  117 | 				+ ".trusted-proxies is not set. Using deprecated Constructor. Untrusted hosts might be added to Forwarded header.");
  118 | 	}
  119 | 
  120 | 	public XForwardedHeadersFilter(String trustedProxiesRegex) {
  121 | 		trustedProxies = TrustedProxies.from(trustedProxiesRegex);
  122 | 	}
  123 | 
  124 | 	@Override
  125 | 	public int getOrder() {
  126 | 		return this.order;
  127 | 	}
  128 | 
  129 | 	public void setOrder(int order) {
  130 | 		this.order = order;
  131 | 	}
  132 | 
  133 | 	public boolean isEnabled() {
  134 | 		return enabled;
  135 | 	}
  136 | 
  137 | 	public void setEnabled(boolean enabled) {
  138 | 		this.enabled = enabled;
  139 | 	}
  140 | 
  141 | 	public boolean isForEnabled() {
  142 | 		return forEnabled;
  143 | 	}
  144 | 
  145 | 	public void setForEnabled(boolean forEnabled) {
  146 | 		this.forEnabled = forEnabled;
  147 | 	}
  148 | 
  149 | 	public boolean isHostEnabled() {
  150 | 		return hostEnabled;
  151 | 	}
  152 | 
  153 | 	public void setHostEnabled(boolean hostEnabled) {
  154 | 		this.hostEnabled = hostEnabled;
  155 | 	}
  156 | 
  157 | 	public boolean isPortEnabled() {
  158 | 		return portEnabled;
  159 | 	}
  160 | 
  161 | 	public void setPortEnabled(boolean portEnabled) {
  162 | 		this.portEnabled = portEnabled;
  163 | 	}
  164 | 
  165 | 	public boolean isProtoEnabled() {
  166 | 		return protoEnabled;
  167 | 	}
  168 | 
  169 | 	public void setProtoEnabled(boolean protoEnabled) {
  170 | 		this.protoEnabled = protoEnabled;
  171 | 	}
  172 | 
  173 | 	public boolean isPrefixEnabled() {
  174 | 		return prefixEnabled;
  175 | 	}
  176 | 
  177 | 	public void setPrefixEnabled(boolean prefixEnabled) {
  178 | 		this.prefixEnabled = prefixEnabled;
  179 | 	}
  180 | 
  181 | 	public boolean isForAppend() {
  182 | 		return forAppend;
  183 | 	}
  184 | 
  185 | 	public void setForAppend(boolean forAppend) {
  186 | 		this.forAppend = forAppend;
  187 | 	}
  188 | 
  189 | 	public boolean isHostAppend() {
  190 | 		return hostAppend;
  191 | 	}
  192 | 
  193 | 	public void setHostAppend(boolean hostAppend) {
  194 | 		this.hostAppend = hostAppend;
  195 | 	}
  196 | 
  197 | 	public boolean isPortAppend() {
  198 | 		return portAppend;
  199 | 	}
  200 | 
  201 | 	public void setPortAppend(boolean portAppend) {
  202 | 		this.portAppend = portAppend;
  203 | 	}
  204 | 
  205 | 	public boolean isProtoAppend() {
  206 | 		return protoAppend;
  207 | 	}
  208 | 
  209 | 	public void setProtoAppend(boolean protoAppend) {
  210 | 		this.protoAppend = protoAppend;
  211 | 	}
  212 | 
  213 | 	public boolean isPrefixAppend() {
  214 | 		return prefixAppend;
  215 | 	}
  216 | 
  217 | 	public void setPrefixAppend(boolean prefixAppend) {
  218 | 		this.prefixAppend = prefixAppend;
  219 | 	}
  220 | 
  221 | 	@Override
  222 | 	public HttpHeaders filter(HttpHeaders input, ServerWebExchange exchange) {
  223 | 		ServerHttpRequest request = exchange.getRequest();
  224 | 
  225 | 		boolean trustedRemoteAddress = request.getRemoteAddress() == null
  226 | 				|| trustedProxies.isTrusted(request.getRemoteAddress().getHostString());
  227 | 
  228 | 		HttpHeaders original = input;
  229 | 
  230 | 		if (!trustedRemoteAddress) {
  231 | 			log.trace(LogMessage.format("Remote address not trusted. pattern %s remote address %s", trustedProxies,
  232 | 					request.getRemoteAddress()));
  233 | 			original = removeXForwardedHeaders(input, exchange);
  234 | 		}
  235 | 
  236 | 		HttpHeaders updated = new HttpHeaders();
  237 | 
  238 | 		for (Map.Entry<String, List<String>> entry : original.headerSet()) {
  239 | 			updated.addAll(entry.getKey(), entry.getValue());
  240 | 		}
  241 | 
  242 | 		if (isForEnabled()) {
  243 | 			String remoteAddr = null;
  244 | 			if (request.getRemoteAddress() != null && request.getRemoteAddress().getAddress() != null) {
  245 | 				remoteAddr = request.getRemoteAddress().getHostString();
  246 | 			}
  247 | 			// match xforwarded for against trusted proxies
  248 | 			if (trustedRemoteAddress) {
  249 | 				write(updated, X_FORWARDED_FOR_HEADER, remoteAddr, isForAppend(), trustedProxies::isTrusted);
  250 | 			}
  251 | 			else {
  252 | 				write(updated, X_FORWARDED_FOR_HEADER, remoteAddr, isForAppend(), value -> true);
  253 | 			}
  254 | 		}
  255 | 
  256 | 		String proto = request.getURI().getScheme();
  257 | 		if (isProtoEnabled()) {
  258 | 			write(updated, X_FORWARDED_PROTO_HEADER, proto, isProtoAppend());
  259 | 		}
  260 | 
  261 | 		if (isPrefixEnabled()) {
  262 | 			// If the path of the url that the gw is routing to is a subset
  263 | 			// (and ending part) of the url that it is routing from then the difference
  264 | 			// is the prefix e.g. if request original.com/prefix/get/ is routed
  265 | 			// to routedservice:8090/get then /prefix is the prefix
  266 | 			// - see XForwardedHeadersFilterTests, so first get uris, then extract paths
  267 | 			// and remove one from another if it's the ending part.
  268 | 
  269 | 			LinkedHashSet<URI> originalUris = exchange.getAttribute(GATEWAY_ORIGINAL_REQUEST_URL_ATTR);
  270 | 			URI requestUri = exchange.getAttribute(GATEWAY_REQUEST_URL_ATTR);
  271 | 
  272 | 			if (originalUris != null && requestUri != null) {
  273 | 
  274 | 				originalUris.forEach(originalUri -> {
  275 | 
  276 | 					if (originalUri != null && originalUri.getPath() != null) {
  277 | 						String prefix = originalUri.getPath();
  278 | 
  279 | 						// strip trailing slashes before checking if request path is end
  280 | 						// of original path
  281 | 						String originalUriPath = stripTrailingSlash(originalUri);
  282 | 						String requestUriPath = stripTrailingSlash(requestUri);
  283 | 
  284 | 						updateRequest(updated, originalUri, originalUriPath, requestUriPath);
  285 | 
  286 | 					}
  287 | 				});
  288 | 			}
  289 | 		}
  290 | 
  291 | 		if (isPortEnabled()) {
  292 | 			String port = String.valueOf(request.getURI().getPort());
  293 | 			if (request.getURI().getPort() < 0) {
  294 | 				port = String.valueOf(getDefaultPort(proto));
  295 | 			}
  296 | 			write(updated, X_FORWARDED_PORT_HEADER, port, isPortAppend());
  297 | 		}
  298 | 
  299 | 		if (isHostEnabled()) {
  300 | 			String host = toHostHeader(request);
  301 | 			write(updated, X_FORWARDED_HOST_HEADER, host, isHostAppend());
  302 | 		}
  303 | 
  304 | 		return updated;
  305 | 	}
  306 | 
  307 | 	private void updateRequest(HttpHeaders updated, URI originalUri, String originalUriPath, String requestUriPath) {
  308 | 		String prefix;
  309 | 		if (requestUriPath != null && (originalUriPath.endsWith(requestUriPath))) {
  310 | 			prefix = substringBeforeLast(originalUriPath, requestUriPath);
  311 | 			if (prefix != null && prefix.length() > 0 && prefix.length() <= originalUri.getPath().length()) {
  312 | 				write(updated, X_FORWARDED_PREFIX_HEADER, prefix, isPrefixAppend());
  313 | 			}
  314 | 		}
  315 | 	}
  316 | 
  317 | 	private static String substringBeforeLast(String str, String separator) {
  318 | 		if (ObjectUtils.isEmpty(str) || ObjectUtils.isEmpty(separator)) {
  319 | 			return str;
  320 | 		}
  321 | 		int pos = str.lastIndexOf(separator);
  322 | 		if (pos == -1) {
  323 | 			return str;
  324 | 		}
  325 | 		return str.substring(0, pos);
  326 | 	}
  327 | 
  328 | 	private void write(HttpHeaders headers, String name, String value, boolean append) {
  329 | 		write(headers, name, value, append, s -> true);
  330 | 	}
  331 | 
  332 | 	private void write(HttpHeaders headers, String name, @Nullable String value, boolean append,
  333 | 			Predicate<String> shouldWrite) {
  334 | 		if (append) {
  335 | 			if (value != null) {
  336 | 				headers.add(name, value);
  337 | 			}
  338 | 			// these headers should be treated as a single comma separated header
  339 | 			if (headers.containsHeader(name)) {
  340 | 				List<String> values = headers.getValuesAsList(name).stream().filter(shouldWrite).toList();
  341 | 				String delimitedValue = StringUtils.collectionToCommaDelimitedString(values);
  342 | 				headers.set(name, delimitedValue);
  343 | 			}
  344 | 		}
  345 | 		else if (value != null && shouldWrite.test(value)) {
  346 | 			headers.set(name, value);
  347 | 		}
  348 | 	}
  349 | 
  350 | 	private int getDefaultPort(String scheme) {
  351 | 		return HTTPS_SCHEME.equals(scheme) ? HTTPS_PORT : HTTP_PORT;
  352 | 	}
  353 | 
  354 | 	private String toHostHeader(ServerHttpRequest request) {
  355 | 		int port = request.getURI().getPort();
  356 | 		String host = request.getURI().getHost();
  357 | 		String scheme = request.getURI().getScheme();
  358 | 		if (port < 0 || (port == HTTP_PORT && HTTP_SCHEME.equals(scheme))
  359 | 				|| (port == HTTPS_PORT && HTTPS_SCHEME.equals(scheme))) {
  360 | 			return host;
  361 | 		}
  362 | 		else {
  363 | 			return host + ":" + port;
  364 | 		}
  365 | 	}
  366 | 
  367 | 	private String stripTrailingSlash(URI uri) {
  368 | 		String path = uri.getPath();
  369 | 		if (path != null && path.endsWith("/")) {
  370 | 			return path.substring(0, path.length() - 1);
  371 | 		}
  372 | 		else {
  373 | 			return path;
  374 | 		}
  375 | 	}
  376 | 
  377 | }
```
