<!-- learning-docs source archive（机械抓取原文，禁止手改代码内容） -->
<!-- origin: org.springframework.security:spring-security-core:7.2.0 | path: org/springframework/security/authorization/AuthorizationDecision.java -->
<!-- 正文引用：[[源码:AuthorizationDecision.java#L起-L止]] ；代码块头部：(src: AuthorizationDecision.java#L起-L止) -->

# AuthorizationDecision.java

| 项 | 值 |
| --- | --- |
| 来源 | `org.springframework.security:spring-security-core:7.2.0` |
| 路径 | `org/springframework/security/authorization/AuthorizationDecision.java` |
| 行数 | 48 |
| 在线地址 | https://repo1.maven.org/maven2/org/springframework/security/spring-security-core/7.2.0/spring-security-core-7.2.0-sources.jar |
| 抓取时间 | 2026-11-25T16:27:45.179Z |

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
   21 | /**
   22 |  * The result of an authorization decision indicating whether access is granted.
   23 |  *
   24 |  * @author Rob Winch
   25 |  * @since 5.0
   26 |  */
   27 | public class AuthorizationDecision implements AuthorizationResult {
   28 | 
   29 | 	@Serial
   30 | 	private static final long serialVersionUID = -3226018324649244416L;
   31 | 
   32 | 	private final boolean granted;
   33 | 
   34 | 	public AuthorizationDecision(boolean granted) {
   35 | 		this.granted = granted;
   36 | 	}
   37 | 
   38 | 	@Override
   39 | 	public boolean isGranted() {
   40 | 		return this.granted;
   41 | 	}
   42 | 
   43 | 	@Override
   44 | 	public String toString() {
   45 | 		return getClass().getSimpleName() + " [granted=" + this.granted + "]";
   46 | 	}
   47 | 
   48 | }
```
