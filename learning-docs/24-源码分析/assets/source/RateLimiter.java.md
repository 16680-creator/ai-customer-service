<!-- learning-docs source archive（机械抓取原文，禁止手改代码内容） -->
<!-- origin: org.springframework.cloud:spring-cloud-gateway-server-webflux:5.1.0 | path: org/springframework/cloud/gateway/filter/ratelimit/RateLimiter.java -->
<!-- 正文引用：[[源码:RateLimiter.java#L起-L止]] ；代码块头部：(src: RateLimiter.java#L起-L止) -->

# RateLimiter.java

| 项 | 值 |
| --- | --- |
| 来源 | `org.springframework.cloud:spring-cloud-gateway-server-webflux:5.1.0` |
| 路径 | `org/springframework/cloud/gateway/filter/ratelimit/RateLimiter.java` |
| 行数 | 69 |
| 在线地址 | https://repo1.maven.org/maven2/org/springframework/cloud/spring-cloud-gateway-server-webflux/5.1.0/spring-cloud-gateway-server-webflux-5.1.0-sources.jar |
| 抓取时间 | 2026-11-26T07:50:18.037Z |

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
   17 | package org.springframework.cloud.gateway.filter.ratelimit;
   18 | 
   19 | import java.util.Collections;
   20 | import java.util.Map;
   21 | import java.util.Objects;
   22 | 
   23 | import reactor.core.publisher.Mono;
   24 | 
   25 | import org.springframework.cloud.gateway.support.StatefulConfigurable;
   26 | 
   27 | /**
   28 |  * @author Spencer Gibb
   29 |  */
   30 | public interface RateLimiter<C> extends StatefulConfigurable<C> {
   31 | 
   32 | 	Mono<Response> isAllowed(String routeId, String id);
   33 | 
   34 | 	class Response {
   35 | 
   36 | 		private final boolean allowed;
   37 | 
   38 | 		private final long tokensRemaining;
   39 | 
   40 | 		private final Map<String, String> headers;
   41 | 
   42 | 		public Response(boolean allowed, Map<String, String> headers) {
   43 | 			this.allowed = allowed;
   44 | 			this.tokensRemaining = -1;
   45 | 			Objects.requireNonNull(headers, "headers may not be null");
   46 | 			this.headers = headers;
   47 | 		}
   48 | 
   49 | 		public boolean isAllowed() {
   50 | 			return allowed;
   51 | 		}
   52 | 
   53 | 		public Map<String, String> getHeaders() {
   54 | 			return Collections.unmodifiableMap(headers);
   55 | 		}
   56 | 
   57 | 		@Override
   58 | 		public String toString() {
   59 | 			final StringBuffer sb = new StringBuffer("Response{");
   60 | 			sb.append("allowed=").append(allowed);
   61 | 			sb.append(", headers=").append(headers);
   62 | 			sb.append(", tokensRemaining=").append(tokensRemaining);
   63 | 			sb.append('}');
   64 | 			return sb.toString();
   65 | 		}
   66 | 
   67 | 	}
   68 | 
   69 | }
```
