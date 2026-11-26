<!-- learning-docs source archive（机械抓取原文，禁止手改代码内容） -->
<!-- origin: org.springframework.security:spring-security-core:7.2.0 | path: org/springframework/security/access/AccessDeniedException.java -->
<!-- 正文引用：[[源码:AccessDeniedException.java#L起-L止]] ；代码块头部：(src: AccessDeniedException.java#L起-L止) -->

# AccessDeniedException.java

| 项 | 值 |
| --- | --- |
| 来源 | `org.springframework.security:spring-security-core:7.2.0` |
| 路径 | `org/springframework/security/access/AccessDeniedException.java` |
| 行数 | 50 |
| 在线地址 | https://repo1.maven.org/maven2/org/springframework/security/spring-security-core/7.2.0/spring-security-core-7.2.0-sources.jar |
| 抓取时间 | 2026-11-26T06:37:40.324Z |

```java
    1 | /*
    2 |  * Copyright 2004, 2005, 2006 Acegi Technology Pty Limited
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
   17 | package org.springframework.security.access;
   18 | 
   19 | import java.io.Serial;
   20 | 
   21 | /**
   22 |  * Thrown if an {@link org.springframework.security.core.Authentication Authentication}
   23 |  * object does not hold a required authority.
   24 |  *
   25 |  * @author Ben Alex
   26 |  */
   27 | public class AccessDeniedException extends RuntimeException {
   28 | 
   29 | 	@Serial
   30 | 	private static final long serialVersionUID = 6395817500121599533L;
   31 | 
   32 | 	/**
   33 | 	 * Constructs an <code>AccessDeniedException</code> with the specified message.
   34 | 	 * @param msg the detail message
   35 | 	 */
   36 | 	public AccessDeniedException(String msg) {
   37 | 		super(msg);
   38 | 	}
   39 | 
   40 | 	/**
   41 | 	 * Constructs an <code>AccessDeniedException</code> with the specified message and
   42 | 	 * root cause.
   43 | 	 * @param msg the detail message
   44 | 	 * @param cause root cause
   45 | 	 */
   46 | 	public AccessDeniedException(String msg, Throwable cause) {
   47 | 		super(msg, cause);
   48 | 	}
   49 | 
   50 | }
```
