<!-- learning-docs source archive（机械抓取原文，禁止手改代码内容） -->
<!-- origin: org.springframework.security:spring-security-core:7.2.0 | path: org/springframework/security/core/authority/SimpleGrantedAuthority.java -->
<!-- 正文引用：[[源码:SimpleGrantedAuthority.java#L起-L止]] ；代码块头部：(src: SimpleGrantedAuthority.java#L起-L止) -->

# SimpleGrantedAuthority.java

| 项 | 值 |
| --- | --- |
| 来源 | `org.springframework.security:spring-security-core:7.2.0` |
| 路径 | `org/springframework/security/core/authority/SimpleGrantedAuthority.java` |
| 行数 | 76 |
| 在线地址 | https://repo1.maven.org/maven2/org/springframework/security/spring-security-core/7.2.0/spring-security-core-7.2.0-sources.jar |
| 抓取时间 | 2026-11-26T06:37:40.091Z |

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
   17 | package org.springframework.security.core.authority;
   18 | 
   19 | import org.jspecify.annotations.Nullable;
   20 | 
   21 | import org.springframework.security.core.GrantedAuthority;
   22 | import org.springframework.util.Assert;
   23 | 
   24 | /**
   25 |  * Basic concrete implementation of a {@link GrantedAuthority}.
   26 |  *
   27 |  * <p>
   28 |  * Stores a {@code String} representation of an authority granted to the
   29 |  * {@link org.springframework.security.core.Authentication Authentication} object.
   30 |  *
   31 |  * @author Luke Taylor
   32 |  * @author Yanming Zhou
   33 |  */
   34 | public final class SimpleGrantedAuthority implements GrantedAuthority {
   35 | 
   36 | 	private static final long serialVersionUID = 620L;
   37 | 
   38 | 	private final String role;
   39 | 
   40 | 	/**
   41 | 	 * Constructs a {@code SimpleGrantedAuthority} using the provided authority.
   42 | 	 * @param authority The provided authority, including any prefix; for example,
   43 | 	 * {@code ROLE_ADMIN}
   44 | 	 */
   45 | 	public SimpleGrantedAuthority(String authority) {
   46 | 		Assert.hasText(authority, "A granted authority textual representation is required");
   47 | 		this.role = authority;
   48 | 	}
   49 | 
   50 | 	@Override
   51 | 	public String getAuthority() {
   52 | 		return this.role;
   53 | 	}
   54 | 
   55 | 	@Override
   56 | 	public boolean equals(@Nullable Object obj) {
   57 | 		if (this == obj) {
   58 | 			return true;
   59 | 		}
   60 | 		if (obj instanceof SimpleGrantedAuthority sga) {
   61 | 			return this.role.equals(sga.getAuthority());
   62 | 		}
   63 | 		return false;
   64 | 	}
   65 | 
   66 | 	@Override
   67 | 	public int hashCode() {
   68 | 		return this.role.hashCode();
   69 | 	}
   70 | 
   71 | 	@Override
   72 | 	public String toString() {
   73 | 		return this.role;
   74 | 	}
   75 | 
   76 | }
```
