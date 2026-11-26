<!-- learning-docs source archive（机械抓取原文，禁止手改代码内容） -->
<!-- origin: org.springframework.security:spring-security-core:7.2.0 | path: org/springframework/security/core/GrantedAuthority.java -->
<!-- 正文引用：[[源码:GrantedAuthority.java#L起-L止]] ；代码块头部：(src: GrantedAuthority.java#L起-L止) -->

# GrantedAuthority.java

| 项 | 值 |
| --- | --- |
| 来源 | `org.springframework.security:spring-security-core:7.2.0` |
| 路径 | `org/springframework/security/core/GrantedAuthority.java` |
| 行数 | 53 |
| 在线地址 | https://repo1.maven.org/maven2/org/springframework/security/spring-security-core/7.2.0/spring-security-core-7.2.0-sources.jar |
| 抓取时间 | 2026-11-26T06:37:40.015Z |

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
   17 | package org.springframework.security.core;
   18 | 
   19 | import java.io.Serializable;
   20 | 
   21 | import org.jspecify.annotations.Nullable;
   22 | 
   23 | import org.springframework.security.authorization.AuthorizationManager;
   24 | 
   25 | /**
   26 |  * Represents an authority granted to an {@link Authentication} object.
   27 |  *
   28 |  * <p>
   29 |  * A <code>GrantedAuthority</code> must either represent itself as a <code>String</code>
   30 |  * or be specifically supported by an {@link AuthorizationManager}.
   31 |  *
   32 |  * @author Ben Alex
   33 |  */
   34 | public interface GrantedAuthority extends Serializable {
   35 | 
   36 | 	/**
   37 | 	 * If the <code>GrantedAuthority</code> can be represented as a <code>String</code>
   38 | 	 * and that <code>String</code> is sufficient in precision to be relied upon for an
   39 | 	 * access control decision by an {@link AuthorizationManager} (or delegate), this
   40 | 	 * method should return such a <code>String</code>.
   41 | 	 * <p>
   42 | 	 * If the <code>GrantedAuthority</code> cannot be expressed with sufficient precision
   43 | 	 * as a <code>String</code>, <code>null</code> should be returned. Returning
   44 | 	 * <code>null</code> will require an <code>AccessDecisionManager</code> (or delegate)
   45 | 	 * to specifically support the <code>GrantedAuthority</code> implementation, so
   46 | 	 * returning <code>null</code> should be avoided unless actually required.
   47 | 	 * @return a representation of the granted authority (or <code>null</code> if the
   48 | 	 * granted authority cannot be expressed as a <code>String</code> with sufficient
   49 | 	 * precision).
   50 | 	 */
   51 | 	@Nullable String getAuthority();
   52 | 
   53 | }
```
