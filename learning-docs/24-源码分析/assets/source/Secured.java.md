<!-- learning-docs source archive（机械抓取原文，禁止手改代码内容） -->
<!-- origin: org.springframework.security:spring-security-core:7.2.0 | path: org/springframework/security/access/annotation/Secured.java -->
<!-- 正文引用：[[源码:Secured.java#L起-L止]] ；代码块头部：(src: Secured.java#L起-L止) -->

# Secured.java

| 项 | 值 |
| --- | --- |
| 来源 | `org.springframework.security:spring-security-core:7.2.0` |
| 路径 | `org/springframework/security/access/annotation/Secured.java` |
| 行数 | 62 |
| 在线地址 | https://repo1.maven.org/maven2/org/springframework/security/spring-security-core/7.2.0/spring-security-core-7.2.0-sources.jar |
| 抓取时间 | 2026-11-26T06:38:07.234Z |

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
   17 | package org.springframework.security.access.annotation;
   18 | 
   19 | import java.lang.annotation.Documented;
   20 | import java.lang.annotation.ElementType;
   21 | import java.lang.annotation.Inherited;
   22 | import java.lang.annotation.Retention;
   23 | import java.lang.annotation.RetentionPolicy;
   24 | import java.lang.annotation.Target;
   25 | 
   26 | /**
   27 |  * Java 5 annotation for describing service layer security attributes.
   28 |  *
   29 |  * <p>
   30 |  * The <code>Secured</code> annotation is used to define a list of security configuration
   31 |  * attributes for business methods. This annotation can be used as a Java 5 alternative to
   32 |  * XML configuration.
   33 |  * <p>
   34 |  * For example:
   35 |  *
   36 |  * <pre>
   37 |  * &#064;Secured({ &quot;ROLE_USER&quot; })
   38 |  * public void create(Contact contact);
   39 |  *
   40 |  * &#064;Secured({ &quot;ROLE_USER&quot;, &quot;ROLE_ADMIN&quot; })
   41 |  * public void update(Contact contact);
   42 |  *
   43 |  * &#064;Secured({ &quot;ROLE_ADMIN&quot; })
   44 |  * public void delete(Contact contact);
   45 |  * </pre>
   46 |  *
   47 |  * @author Mark St.Godard
   48 |  */
   49 | @Target({ ElementType.METHOD, ElementType.TYPE })
   50 | @Retention(RetentionPolicy.RUNTIME)
   51 | @Inherited
   52 | @Documented
   53 | public @interface Secured {
   54 | 
   55 | 	/**
   56 | 	 * Returns the list of security configuration attributes (e.g.&nbsp;ROLE_USER,
   57 | 	 * ROLE_ADMIN).
   58 | 	 * @return String[] The secure method attributes
   59 | 	 */
   60 | 	String[] value();
   61 | 
   62 | }
```
