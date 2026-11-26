<!-- learning-docs source archive（机械抓取原文，禁止手改代码内容） -->
<!-- origin: org.springframework.security:spring-security-core:7.2.0 | path: org/springframework/security/authorization/AuthorizationDeniedException.java -->
<!-- 正文引用：[[源码:AuthorizationDeniedException.java#L起-L止]] ；代码块头部：(src: AuthorizationDeniedException.java#L起-L止) -->

# AuthorizationDeniedException.java

| 项 | 值 |
| --- | --- |
| 来源 | `org.springframework.security:spring-security-core:7.2.0` |
| 路径 | `org/springframework/security/authorization/AuthorizationDeniedException.java` |
| 行数 | 57 |
| 在线地址 | https://repo1.maven.org/maven2/org/springframework/security/spring-security-core/7.2.0/spring-security-core-7.2.0-sources.jar |
| 抓取时间 | 2026-11-26T06:37:40.268Z |

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
   17 | package org.springframework.security.authorization;
   18 | 
   19 | import java.io.Serial;
   20 | 
   21 | import org.springframework.security.access.AccessDeniedException;
   22 | import org.springframework.util.Assert;
   23 | 
   24 | /**
   25 |  * An {@link AccessDeniedException} that contains the {@link AuthorizationResult}.
   26 |  *
   27 |  * @author Marcus da Coregio
   28 |  * @since 6.3
   29 |  */
   30 | public class AuthorizationDeniedException extends AccessDeniedException implements AuthorizationResult {
   31 | 
   32 | 	@Serial
   33 | 	private static final long serialVersionUID = 3227305845919610459L;
   34 | 
   35 | 	private final AuthorizationResult result;
   36 | 
   37 | 	public AuthorizationDeniedException(String msg, AuthorizationResult authorizationResult) {
   38 | 		super(msg);
   39 | 		Assert.notNull(authorizationResult, "authorizationResult cannot be null");
   40 | 		Assert.isTrue(!authorizationResult.isGranted(), "Granted authorization results are not supported");
   41 | 		this.result = authorizationResult;
   42 | 	}
   43 | 
   44 | 	public AuthorizationDeniedException(String msg) {
   45 | 		this(msg, new AuthorizationDecision(false));
   46 | 	}
   47 | 
   48 | 	public AuthorizationResult getAuthorizationResult() {
   49 | 		return this.result;
   50 | 	}
   51 | 
   52 | 	@Override
   53 | 	public boolean isGranted() {
   54 | 		return false;
   55 | 	}
   56 | 
   57 | }
```
