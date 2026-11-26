<!-- learning-docs source archive（机械抓取原文，禁止手改代码内容） -->
<!-- origin: org.springframework.security:spring-security-crypto:7.2.0 | path: org/springframework/security/crypto/factory/PasswordEncoderFactories.java -->
<!-- 正文引用：[[源码:PasswordEncoderFactories.java#L起-L止]] ；代码块头部：(src: PasswordEncoderFactories.java#L起-L止) -->

# PasswordEncoderFactories.java

| 项 | 值 |
| --- | --- |
| 来源 | `org.springframework.security:spring-security-crypto:7.2.0` |
| 路径 | `org/springframework/security/crypto/factory/PasswordEncoderFactories.java` |
| 行数 | 93 |
| 在线地址 | https://repo1.maven.org/maven2/org/springframework/security/spring-security-crypto/7.2.0/spring-security-crypto-7.2.0-sources.jar |
| 抓取时间 | 2026-11-25T16:27:44.999Z |

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
   17 | package org.springframework.security.crypto.factory;
   18 | 
   19 | import java.util.HashMap;
   20 | import java.util.Map;
   21 | 
   22 | import org.springframework.security.crypto.argon2.Argon2PasswordEncoder;
   23 | import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
   24 | import org.springframework.security.crypto.password.DelegatingPasswordEncoder;
   25 | import org.springframework.security.crypto.password.PasswordEncoder;
   26 | import org.springframework.security.crypto.password.Pbkdf2PasswordEncoder;
   27 | import org.springframework.security.crypto.scrypt.SCryptPasswordEncoder;
   28 | 
   29 | /**
   30 |  * Used for creating {@link PasswordEncoder} instances.
   31 |  *
   32 |  * @author Rob Winch
   33 |  * @since 5.0
   34 |  */
   35 | public final class PasswordEncoderFactories {
   36 | 
   37 | 	private PasswordEncoderFactories() {
   38 | 	}
   39 | 
   40 | 	/**
   41 | 	 * Creates a {@link DelegatingPasswordEncoder} with default mappings. Additional
   42 | 	 * mappings may be added and the encoding will be updated to conform with best
   43 | 	 * practices. However, due to the nature of {@link DelegatingPasswordEncoder} the
   44 | 	 * updates should not impact users. The mappings current are:
   45 | 	 *
   46 | 	 * <ul>
   47 | 	 * <li>bcrypt - {@link BCryptPasswordEncoder} (Also used for encoding)</li>
   48 | 	 * <li>ldap -
   49 | 	 * {@link org.springframework.security.crypto.password.LdapShaPasswordEncoder}</li>
   50 | 	 * <li>MD4 -
   51 | 	 * {@link org.springframework.security.crypto.password.Md4PasswordEncoder}</li>
   52 | 	 * <li>MD5 - {@code new MessageDigestPasswordEncoder("MD5")}</li>
   53 | 	 * <li>noop -
   54 | 	 * {@link org.springframework.security.crypto.password.NoOpPasswordEncoder}</li>
   55 | 	 * <li>pbkdf2 - {@link Pbkdf2PasswordEncoder#defaultsForSpringSecurity_v5_5()}</li>
   56 | 	 * <li>pbkdf2@SpringSecurity_v5_8 -
   57 | 	 * {@link Pbkdf2PasswordEncoder#defaultsForSpringSecurity_v5_8()}</li>
   58 | 	 * <li>scrypt - {@link SCryptPasswordEncoder#defaultsForSpringSecurity_v4_1()}</li>
   59 | 	 * <li>scrypt@SpringSecurity_v5_8 -
   60 | 	 * {@link SCryptPasswordEncoder#defaultsForSpringSecurity_v5_8()}</li>
   61 | 	 * <li>SHA-1 - {@code new MessageDigestPasswordEncoder("SHA-1")}</li>
   62 | 	 * <li>SHA-256 - {@code new MessageDigestPasswordEncoder("SHA-256")}</li>
   63 | 	 * <li>sha256 -
   64 | 	 * {@link org.springframework.security.crypto.password.StandardPasswordEncoder}</li>
   65 | 	 * <li>argon2 - {@link Argon2PasswordEncoder#defaultsForSpringSecurity_v5_2()}</li>
   66 | 	 * <li>argon2@SpringSecurity_v5_8 -
   67 | 	 * {@link Argon2PasswordEncoder#defaultsForSpringSecurity_v5_8()}</li>
   68 | 	 * </ul>
   69 | 	 * @return the {@link PasswordEncoder} to use
   70 | 	 */
   71 | 	@SuppressWarnings("deprecation")
   72 | 	public static PasswordEncoder createDelegatingPasswordEncoder() {
   73 | 		String encodingId = "bcrypt";
   74 | 		Map<String, PasswordEncoder> encoders = new HashMap<>();
   75 | 		encoders.put(encodingId, new BCryptPasswordEncoder());
   76 | 		encoders.put("ldap", new org.springframework.security.crypto.password.LdapShaPasswordEncoder());
   77 | 		encoders.put("MD4", new org.springframework.security.crypto.password.Md4PasswordEncoder());
   78 | 		encoders.put("MD5", new org.springframework.security.crypto.password.MessageDigestPasswordEncoder("MD5"));
   79 | 		encoders.put("noop", org.springframework.security.crypto.password.NoOpPasswordEncoder.getInstance());
   80 | 		encoders.put("pbkdf2", Pbkdf2PasswordEncoder.defaultsForSpringSecurity_v5_5());
   81 | 		encoders.put("pbkdf2@SpringSecurity_v5_8", Pbkdf2PasswordEncoder.defaultsForSpringSecurity_v5_8());
   82 | 		encoders.put("scrypt", SCryptPasswordEncoder.defaultsForSpringSecurity_v4_1());
   83 | 		encoders.put("scrypt@SpringSecurity_v5_8", SCryptPasswordEncoder.defaultsForSpringSecurity_v5_8());
   84 | 		encoders.put("SHA-1", new org.springframework.security.crypto.password.MessageDigestPasswordEncoder("SHA-1"));
   85 | 		encoders.put("SHA-256",
   86 | 				new org.springframework.security.crypto.password.MessageDigestPasswordEncoder("SHA-256"));
   87 | 		encoders.put("sha256", new org.springframework.security.crypto.password.StandardPasswordEncoder());
   88 | 		encoders.put("argon2", Argon2PasswordEncoder.defaultsForSpringSecurity_v5_2());
   89 | 		encoders.put("argon2@SpringSecurity_v5_8", Argon2PasswordEncoder.defaultsForSpringSecurity_v5_8());
   90 | 		return new DelegatingPasswordEncoder(encodingId, encoders);
   91 | 	}
   92 | 
   93 | }
```
