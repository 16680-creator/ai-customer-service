<!-- learning-docs source archive（机械抓取原文，禁止手改代码内容） -->
<!-- origin: org.springframework.security:spring-security-web:7.2.0 | path: org/springframework/security/web/access/intercept/RequestAuthorizationContext.java -->
<!-- 正文引用：[[源码:RequestAuthorizationContext.java#L起-L止]] ；代码块头部：(src: RequestAuthorizationContext.java#L起-L止) -->

# RequestAuthorizationContext.java

| 项 | 值 |
| --- | --- |
| 来源 | `org.springframework.security:spring-security-web:7.2.0` |
| 路径 | `org/springframework/security/web/access/intercept/RequestAuthorizationContext.java` |
| 行数 | 73 |
| 在线地址 | https://repo1.maven.org/maven2/org/springframework/security/spring-security-web/7.2.0/spring-security-web-7.2.0-sources.jar |
| 抓取时间 | 2026-11-26T06:37:40.924Z |

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
   17 | package org.springframework.security.web.access.intercept;
   18 | 
   19 | import java.util.Collections;
   20 | import java.util.Map;
   21 | 
   22 | import jakarta.servlet.http.HttpServletRequest;
   23 | 
   24 | /**
   25 |  * An {@link HttpServletRequest} authorization context.
   26 |  *
   27 |  * @author Evgeniy Cheban
   28 |  * @since 5.5
   29 |  */
   30 | public final class RequestAuthorizationContext {
   31 | 
   32 | 	private final HttpServletRequest request;
   33 | 
   34 | 	private final Map<String, String> variables;
   35 | 
   36 | 	/**
   37 | 	 * Creates an instance.
   38 | 	 * @param request the {@link HttpServletRequest} to use
   39 | 	 */
   40 | 	public RequestAuthorizationContext(HttpServletRequest request) {
   41 | 		this(request, Collections.emptyMap());
   42 | 	}
   43 | 
   44 | 	/**
   45 | 	 * Creates an instance.
   46 | 	 * @param request the {@link HttpServletRequest} to use
   47 | 	 * @param variables a map containing key-value pairs representing extracted variable
   48 | 	 * names and variable values
   49 | 	 */
   50 | 	public RequestAuthorizationContext(HttpServletRequest request, Map<String, String> variables) {
   51 | 		this.request = request;
   52 | 		this.variables = variables;
   53 | 	}
   54 | 
   55 | 	/**
   56 | 	 * Returns the {@link HttpServletRequest}.
   57 | 	 * @return the {@link HttpServletRequest} to use
   58 | 	 */
   59 | 	public HttpServletRequest getRequest() {
   60 | 		return this.request;
   61 | 	}
   62 | 
   63 | 	/**
   64 | 	 * Returns the extracted variable values where the key is the variable name and the
   65 | 	 * value is the variable value.
   66 | 	 * @return a map containing key-value pairs representing extracted variable names and
   67 | 	 * variable values
   68 | 	 */
   69 | 	public Map<String, String> getVariables() {
   70 | 		return this.variables;
   71 | 	}
   72 | 
   73 | }
```
