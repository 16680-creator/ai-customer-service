<!-- learning-docs source archive（机械抓取原文，禁止手改代码内容） -->
<!-- origin: org.springframework.security:spring-security-core:7.2.0 | path: org/springframework/security/authentication/AnonymousAuthenticationToken.java -->
<!-- 正文引用：[[源码:AnonymousAuthenticationToken.java#L起-L止]] ；代码块头部：(src: AnonymousAuthenticationToken.java#L起-L止) -->

# AnonymousAuthenticationToken.java

| 项 | 值 |
| --- | --- |
| 来源 | `org.springframework.security:spring-security-core:7.2.0` |
| 路径 | `org/springframework/security/authentication/AnonymousAuthenticationToken.java` |
| 行数 | 110 |
| 在线地址 | https://repo1.maven.org/maven2/org/springframework/security/spring-security-core/7.2.0/spring-security-core-7.2.0-sources.jar |
| 抓取时间 | 2026-11-25T16:47:23.292Z |

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
   17 | package org.springframework.security.authentication;
   18 | 
   19 | import java.io.Serializable;
   20 | import java.util.Collection;
   21 | 
   22 | import org.jspecify.annotations.Nullable;
   23 | 
   24 | import org.springframework.security.core.GrantedAuthority;
   25 | import org.springframework.util.Assert;
   26 | 
   27 | /**
   28 |  * Represents an anonymous <code>Authentication</code>.
   29 |  *
   30 |  * @author Ben Alex
   31 |  */
   32 | public class AnonymousAuthenticationToken extends AbstractAuthenticationToken implements Serializable {
   33 | 
   34 | 	private static final long serialVersionUID = 1L;
   35 | 
   36 | 	private final Object principal;
   37 | 
   38 | 	private final int keyHash;
   39 | 
   40 | 	/**
   41 | 	 * Constructor.
   42 | 	 * @param key to identify if this object made by an authorised client
   43 | 	 * @param principal the principal (typically a <code>UserDetails</code>)
   44 | 	 * @param authorities the authorities granted to the principal
   45 | 	 * @throws IllegalArgumentException if a <code>null</code> was passed
   46 | 	 */
   47 | 	public AnonymousAuthenticationToken(String key, Object principal,
   48 | 			Collection<? extends GrantedAuthority> authorities) {
   49 | 		this(extractKeyHash(key), principal, authorities);
   50 | 	}
   51 | 
   52 | 	/**
   53 | 	 * Constructor helps in Jackson Deserialization.
   54 | 	 * @param keyHash hashCode of provided Key, constructed by above constructor
   55 | 	 * @param principal the principal (typically a <code>UserDetails</code>)
   56 | 	 * @param authorities the authorities granted to the principal
   57 | 	 * @since 4.2
   58 | 	 */
   59 | 	private AnonymousAuthenticationToken(Integer keyHash, Object principal,
   60 | 			Collection<? extends GrantedAuthority> authorities) {
   61 | 		super(authorities);
   62 | 		Assert.isTrue(principal != null && !"".equals(principal), "principal cannot be null or empty");
   63 | 		Assert.notEmpty(authorities, "authorities cannot be null or empty");
   64 | 		this.keyHash = keyHash;
   65 | 		this.principal = principal;
   66 | 		setAuthenticated(true);
   67 | 	}
   68 | 
   69 | 	private static Integer extractKeyHash(String key) {
   70 | 		Assert.hasLength(key, "key cannot be empty or null");
   71 | 		return key.hashCode();
   72 | 	}
   73 | 
   74 | 	@Override
   75 | 	public boolean equals(@Nullable Object obj) {
   76 | 		if (!super.equals(obj)) {
   77 | 			return false;
   78 | 		}
   79 | 		if (obj instanceof AnonymousAuthenticationToken test) {
   80 | 			return (this.getKeyHash() == test.getKeyHash());
   81 | 		}
   82 | 		return false;
   83 | 	}
   84 | 
   85 | 	@Override
   86 | 	public int hashCode() {
   87 | 		int result = super.hashCode();
   88 | 		result = 31 * result + this.keyHash;
   89 | 		return result;
   90 | 	}
   91 | 
   92 | 	/**
   93 | 	 * Always returns an empty <code>String</code>.
   94 | 	 * @return an empty String
   95 | 	 */
   96 | 	@Override
   97 | 	public Object getCredentials() {
   98 | 		return "";
   99 | 	}
  100 | 
  101 | 	public int getKeyHash() {
  102 | 		return this.keyHash;
  103 | 	}
  104 | 
  105 | 	@Override
  106 | 	public Object getPrincipal() {
  107 | 		return this.principal;
  108 | 	}
  109 | 
  110 | }
```
