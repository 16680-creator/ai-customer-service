<!-- learning-docs source archive（机械抓取原文，禁止手改代码内容） -->
<!-- origin: org.springframework.security:spring-security-config:7.2.0 | path: org/springframework/security/config/annotation/method/configuration/EnableMethodSecurity.java -->
<!-- 正文引用：[[源码:EnableMethodSecurity.java#L起-L止]] ；代码块头部：(src: EnableMethodSecurity.java#L起-L止) -->

# EnableMethodSecurity.java

| 项 | 值 |
| --- | --- |
| 来源 | `org.springframework.security:spring-security-config:7.2.0` |
| 路径 | `org/springframework/security/config/annotation/method/configuration/EnableMethodSecurity.java` |
| 行数 | 100 |
| 在线地址 | https://repo1.maven.org/maven2/org/springframework/security/spring-security-config/7.2.0/spring-security-config-7.2.0-sources.jar |
| 抓取时间 | 2026-11-25T16:27:52.389Z |

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
   17 | package org.springframework.security.config.annotation.method.configuration;
   18 | 
   19 | import java.lang.annotation.Documented;
   20 | import java.lang.annotation.ElementType;
   21 | import java.lang.annotation.Retention;
   22 | import java.lang.annotation.RetentionPolicy;
   23 | import java.lang.annotation.Target;
   24 | 
   25 | import org.springframework.context.annotation.AdviceMode;
   26 | import org.springframework.context.annotation.Import;
   27 | import org.springframework.security.access.annotation.Secured;
   28 | import org.springframework.security.access.prepost.PostAuthorize;
   29 | import org.springframework.security.access.prepost.PostFilter;
   30 | import org.springframework.security.access.prepost.PreAuthorize;
   31 | import org.springframework.security.access.prepost.PreFilter;
   32 | 
   33 | /**
   34 |  * Enables Spring Security Method Security.
   35 |  *
   36 |  * @author Evgeniy Cheban
   37 |  * @author Josh Cummings
   38 |  * @since 5.6
   39 |  */
   40 | @Retention(RetentionPolicy.RUNTIME)
   41 | @Target(ElementType.TYPE)
   42 | @Documented
   43 | @Import(MethodSecuritySelector.class)
   44 | public @interface EnableMethodSecurity {
   45 | 
   46 | 	/**
   47 | 	 * Determines if Spring Security's {@link PreAuthorize}, {@link PostAuthorize},
   48 | 	 * {@link PreFilter}, and {@link PostFilter} annotations should be enabled. Default is
   49 | 	 * true.
   50 | 	 * @return true if pre/post annotations should be enabled false otherwise
   51 | 	 */
   52 | 	boolean prePostEnabled() default true;
   53 | 
   54 | 	/**
   55 | 	 * Determines if Spring Security's {@link Secured} annotation should be enabled.
   56 | 	 * Default is false.
   57 | 	 * @return true if {@link Secured} annotation should be enabled false otherwise
   58 | 	 */
   59 | 	boolean securedEnabled() default false;
   60 | 
   61 | 	/**
   62 | 	 * Determines if JSR-250 annotations should be enabled. Default is false.
   63 | 	 * @return true if JSR-250 should be enabled false otherwise
   64 | 	 */
   65 | 	boolean jsr250Enabled() default false;
   66 | 
   67 | 	/**
   68 | 	 * Indicate whether subclass-based (CGLIB) proxies are to be created as opposed to
   69 | 	 * standard Java interface-based proxies. The default is {@code false}. <strong>
   70 | 	 * Applicable only if {@link #mode()} is set to {@link AdviceMode#PROXY}</strong>.
   71 | 	 * <p>
   72 | 	 * Note that setting this attribute to {@code true} will affect <em>all</em>
   73 | 	 * Spring-managed beans requiring proxying, not just those marked with
   74 | 	 * {@code @Cacheable}. For example, other beans marked with Spring's
   75 | 	 * {@code @Transactional} annotation will be upgraded to subclass proxying at the same
   76 | 	 * time. This approach has no negative impact in practice unless one is explicitly
   77 | 	 * expecting one type of proxy vs another, e.g. in tests.
   78 | 	 * @return true if subclass-based (CGLIB) proxies are to be created
   79 | 	 */
   80 | 	boolean proxyTargetClass() default false;
   81 | 
   82 | 	/**
   83 | 	 * Indicate how security advice should be applied. The default is
   84 | 	 * {@link AdviceMode#PROXY}.
   85 | 	 * @see AdviceMode
   86 | 	 * @return the {@link AdviceMode} to use
   87 | 	 */
   88 | 	AdviceMode mode() default AdviceMode.PROXY;
   89 | 
   90 | 	/**
   91 | 	 * Indicate additional offset in the ordering of the execution of the security
   92 | 	 * interceptors when multiple advices are applied at a specific joinpoint. I.e.,
   93 | 	 * precedence of each security interceptor enabled by this annotation will be
   94 | 	 * calculated as sum of its default precedence and offset. The default is 0.
   95 | 	 * @return the offset in the order the security advisor should be applied
   96 | 	 * @since 6.3
   97 | 	 */
   98 | 	int offset() default 0;
   99 | 
  100 | }
```
