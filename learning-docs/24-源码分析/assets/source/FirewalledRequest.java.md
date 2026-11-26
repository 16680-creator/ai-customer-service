<!-- learning-docs source archive（机械抓取原文，禁止手改代码内容） -->
<!-- origin: org.springframework.security:spring-security-web:7.2.0 | path: org/springframework/security/web/firewall/FirewalledRequest.java -->
<!-- 正文引用：[[源码:FirewalledRequest.java#L起-L止]] ；代码块头部：(src: FirewalledRequest.java#L起-L止) -->

# FirewalledRequest.java

| 项 | 值 |
| --- | --- |
| 来源 | `org.springframework.security:spring-security-web:7.2.0` |
| 路径 | `org/springframework/security/web/firewall/FirewalledRequest.java` |
| 行数 | 56 |
| 在线地址 | https://repo1.maven.org/maven2/org/springframework/security/spring-security-web/7.2.0/spring-security-web-7.2.0-sources.jar |
| 抓取时间 | 2026-11-25T17:02:22.683Z |

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
   17 | package org.springframework.security.web.firewall;
   18 | 
   19 | import jakarta.servlet.http.HttpServletRequest;
   20 | import jakarta.servlet.http.HttpServletRequestWrapper;
   21 | 
   22 | /**
   23 |  * Request wrapper which is returned by the {@code HttpFirewall} interface.
   24 |  * <p>
   25 |  * The only difference is the {@code reset} method which allows some or all of the state
   26 |  * to be reset by the {@code FilterChainProxy} when the request leaves the security filter
   27 |  * chain.
   28 |  *
   29 |  * @author Luke Taylor
   30 |  */
   31 | public abstract class FirewalledRequest extends HttpServletRequestWrapper {
   32 | 
   33 | 	/**
   34 | 	 * Constructs a request object wrapping the given request.
   35 | 	 * @throws IllegalArgumentException if the request is null
   36 | 	 */
   37 | 	public FirewalledRequest(HttpServletRequest request) {
   38 | 		super(request);
   39 | 	}
   40 | 
   41 | 	/**
   42 | 	 * This method will be called once the request has passed through the security filter
   43 | 	 * chain, when it is about to proceed to the application proper.
   44 | 	 * <p>
   45 | 	 * An implementation can thus choose to modify the state of the request for the
   46 | 	 * security infrastructure, while still maintaining the original
   47 | 	 * {@link HttpServletRequest}.
   48 | 	 */
   49 | 	public abstract void reset();
   50 | 
   51 | 	@Override
   52 | 	public String toString() {
   53 | 		return "FirewalledRequest[ " + getRequest() + "]";
   54 | 	}
   55 | 
   56 | }
```
