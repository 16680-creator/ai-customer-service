<!-- learning-docs source archive（机械抓取原文，禁止手改代码内容） -->
<!-- origin: org.springframework.cloud:spring-cloud-gateway-server-webflux:5.1.0 | path: org/springframework/cloud/gateway/filter/headers/RemoveHopByHopHeadersFilter.java -->
<!-- 正文引用：[[源码:RemoveHopByHopHeadersFilter.java#L起-L止]] ；代码块头部：(src: RemoveHopByHopHeadersFilter.java#L起-L止) -->

# RemoveHopByHopHeadersFilter.java

| 项 | 值 |
| --- | --- |
| 来源 | `org.springframework.cloud:spring-cloud-gateway-server-webflux:5.1.0` |
| 路径 | `org/springframework/cloud/gateway/filter/headers/RemoveHopByHopHeadersFilter.java` |
| 行数 | 91 |
| 在线地址 | https://repo1.maven.org/maven2/org/springframework/cloud/spring-cloud-gateway-server-webflux/5.1.0/spring-cloud-gateway-server-webflux-5.1.0-sources.jar |
| 抓取时间 | 2026-11-26T06:49:47.247Z |

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
   19 | import java.util.Arrays;
   20 | import java.util.HashSet;
   21 | import java.util.List;
   22 | import java.util.Locale;
   23 | import java.util.Map;
   24 | import java.util.Objects;
   25 | import java.util.Set;
   26 | import java.util.stream.Collectors;
   27 | 
   28 | import org.springframework.boot.context.properties.ConfigurationProperties;
   29 | import org.springframework.core.Ordered;
   30 | import org.springframework.http.HttpHeaders;
   31 | import org.springframework.web.server.ServerWebExchange;
   32 | 
   33 | @ConfigurationProperties("spring.cloud.gateway.server.webflux.filter.remove-hop-by-hop")
   34 | public class RemoveHopByHopHeadersFilter implements HttpHeadersFilter, Ordered {
   35 | 
   36 | 	/**
   37 | 	 * Headers to remove as the result of applying the filter.
   38 | 	 */
   39 | 	public static final Set<String> HEADERS_REMOVED_ON_REQUEST = new HashSet<>(
   40 | 			Arrays.asList("connection", "keep-alive", "transfer-encoding", "te", "trailer", "proxy-authorization",
   41 | 					"proxy-authenticate", "x-application-context", "upgrade"
   42 | 			// these two are not listed in
   43 | 			// https://tools.ietf.org/html/draft-ietf-httpbis-p1-messaging-14#section-7.1.3
   44 | 			// "proxy-connection",
   45 | 			// "content-length",
   46 | 			));
   47 | 
   48 | 	private int order = Ordered.LOWEST_PRECEDENCE - 1;
   49 | 
   50 | 	private Set<String> headers = HEADERS_REMOVED_ON_REQUEST;
   51 | 
   52 | 	public Set<String> getHeaders() {
   53 | 		return headers;
   54 | 	}
   55 | 
   56 | 	public void setHeaders(Set<String> headers) {
   57 | 		Objects.requireNonNull(headers, "headers may not be null");
   58 | 		this.headers = headers.stream().map(String::toLowerCase).collect(Collectors.toSet());
   59 | 	}
   60 | 
   61 | 	@Override
   62 | 	public int getOrder() {
   63 | 		return order;
   64 | 	}
   65 | 
   66 | 	public void setOrder(int order) {
   67 | 		this.order = order;
   68 | 	}
   69 | 
   70 | 	@Override
   71 | 	public HttpHeaders filter(HttpHeaders originalHeaders, ServerWebExchange exchange) {
   72 | 		HttpHeaders filtered = new HttpHeaders();
   73 | 		List<String> connectionOptions = originalHeaders.getConnection().stream().map(String::toLowerCase).toList();
   74 | 		Set<String> headersToRemove = new HashSet<>(headers);
   75 | 		headersToRemove.addAll(connectionOptions);
   76 | 
   77 | 		for (Map.Entry<String, List<String>> entry : originalHeaders.headerSet()) {
   78 | 			if (!headersToRemove.contains(entry.getKey().toLowerCase(Locale.ROOT))) {
   79 | 				filtered.addAll(entry.getKey(), entry.getValue());
   80 | 			}
   81 | 		}
   82 | 
   83 | 		return filtered;
   84 | 	}
   85 | 
   86 | 	@Override
   87 | 	public boolean supports(Type type) {
   88 | 		return type.equals(Type.REQUEST) || type.equals(Type.RESPONSE);
   89 | 	}
   90 | 
   91 | }
```
