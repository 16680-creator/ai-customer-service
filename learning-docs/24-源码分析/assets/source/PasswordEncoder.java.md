<!-- learning-docs source archive（机械抓取原文，禁止手改代码内容） -->
<!-- origin: org.springframework.security:spring-security-crypto:7.2.0 | path: org/springframework/security/crypto/password/PasswordEncoder.java -->
<!-- 正文引用：[[源码:PasswordEncoder.java#L起-L止]] ；代码块头部：(src: PasswordEncoder.java#L起-L止) -->

# PasswordEncoder.java

| 项 | 值 |
| --- | --- |
| 来源 | `org.springframework.security:spring-security-crypto:7.2.0` |
| 路径 | `org/springframework/security/crypto/password/PasswordEncoder.java` |
| 行数 | 69 |
| 在线地址 | https://repo1.maven.org/maven2/org/springframework/security/spring-security-crypto/7.2.0/spring-security-crypto-7.2.0-sources.jar |
| 抓取时间 | 2026-11-25T16:27:44.876Z |

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
   17 | package org.springframework.security.crypto.password;
   18 | 
   19 | import org.jspecify.annotations.Nullable;
   20 | 
   21 | import org.springframework.lang.Contract;
   22 | 
   23 | /**
   24 |  * Service interface for encoding passwords.
   25 |  *
   26 |  * The preferred implementation is {@code BCryptPasswordEncoder}.
   27 |  *
   28 |  * @author Keith Donald
   29 |  * @author Rob Winch
   30 |  */
   31 | public interface PasswordEncoder {
   32 | 
   33 | 	/**
   34 | 	 * Encode the raw password. Generally, a good encoding algorithm uses an adaptive one
   35 | 	 * way function.
   36 | 	 * @param rawPassword a password that has not been encoded. The value can be null in
   37 | 	 * the event that the user has no password; in which case the result must be null.
   38 | 	 * @return A non-null encoded password, unless the rawPassword was null in which case
   39 | 	 * the result must be null.
   40 | 	 */
   41 | 	@Contract("!null -> !null; null -> null")
   42 | 	@Nullable String encode(@Nullable CharSequence rawPassword);
   43 | 
   44 | 	/**
   45 | 	 * Verify the encoded password obtained from storage matches the submitted raw
   46 | 	 * password after it too is encoded. Returns true if the passwords match, false if
   47 | 	 * they do not. The stored password itself is never decoded. Never true if either
   48 | 	 * rawPassword or encodedPassword is null or an empty String.
   49 | 	 * @param rawPassword the raw password to encode and match.
   50 | 	 * @param encodedPassword the encoded password from storage to compare with.
   51 | 	 * @return true if the raw password, after encoding, matches the encoded password from
   52 | 	 * storage.
   53 | 	 */
   54 | 	boolean matches(@Nullable CharSequence rawPassword, @Nullable String encodedPassword);
   55 | 
   56 | 	/**
   57 | 	 * Returns true if the encoded password should be encoded again for better security,
   58 | 	 * else false. The default implementation always returns false.
   59 | 	 * @param encodedPassword the encoded password to check. Possibly null if the user did
   60 | 	 * not have a password.
   61 | 	 * @return true if the encoded password should be encoded again for better security,
   62 | 	 * else false. If encodedPassword is null (the user didn't have a password), then
   63 | 	 * always false.
   64 | 	 */
   65 | 	default boolean upgradeEncoding(@Nullable String encodedPassword) {
   66 | 		return false;
   67 | 	}
   68 | 
   69 | }
```
