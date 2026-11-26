<!-- learning-docs source archive（机械抓取原文，禁止手改代码内容） -->
<!-- origin: org.springframework.security:spring-security-web:7.2.0 | path: org/springframework/security/web/authentication/session/ChangeSessionIdAuthenticationStrategy.java -->
<!-- 正文引用：[[源码:ChangeSessionIdAuthenticationStrategy.java#L起-L止]] ；代码块头部：(src: ChangeSessionIdAuthenticationStrategy.java#L起-L止) -->

# ChangeSessionIdAuthenticationStrategy.java

| 项 | 值 |
| --- | --- |
| 来源 | `org.springframework.security:spring-security-web:7.2.0` |
| 路径 | `org/springframework/security/web/authentication/session/ChangeSessionIdAuthenticationStrategy.java` |
| 行数 | 37 |
| 在线地址 | https://repo1.maven.org/maven2/org/springframework/security/spring-security-web/7.2.0/spring-security-web-7.2.0-sources.jar |
| 抓取时间 | 2026-11-25T16:38:37.485Z |

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
   17 | package org.springframework.security.web.authentication.session;
   18 | 
   19 | import jakarta.servlet.http.HttpServletRequest;
   20 | import jakarta.servlet.http.HttpSession;
   21 | 
   22 | /**
   23 |  * Uses {@code HttpServletRequest.changeSessionId()} to protect against session fixation
   24 |  * attacks. This is the default implementation.
   25 |  *
   26 |  * @author Rob Winch
   27 |  * @since 3.2
   28 |  */
   29 | public final class ChangeSessionIdAuthenticationStrategy extends AbstractSessionFixationProtectionStrategy {
   30 | 
   31 | 	@Override
   32 | 	HttpSession applySessionFixation(HttpServletRequest request) {
   33 | 		request.changeSessionId();
   34 | 		return request.getSession();
   35 | 	}
   36 | 
   37 | }
```
