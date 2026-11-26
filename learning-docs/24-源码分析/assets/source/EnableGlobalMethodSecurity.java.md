<!-- learning-docs source archive（机械抓取原文，禁止手改代码内容） -->
<!-- origin: org.springframework.security:spring-security-config:7.2.0 | path: org/springframework/security/config/annotation/method/configuration/EnableGlobalMethodSecurity.java -->
<!-- 正文引用：[[源码:EnableGlobalMethodSecurity.java#L起-L止]] ；代码块头部：(src: EnableGlobalMethodSecurity.java#L起-L止) -->

# EnableGlobalMethodSecurity.java

| 项 | 值 |
| --- | --- |
| 来源 | `org.springframework.security:spring-security-config:7.2.0` |
| 路径 | `org/springframework/security/config/annotation/method/configuration/EnableGlobalMethodSecurity.java` |
| 行数 | 109 |
| 在线地址 | https://repo1.maven.org/maven2/org/springframework/security/spring-security-config/7.2.0/spring-security-config-7.2.0-sources.jar |
| 抓取时间 | 2026-11-26T06:49:26.422Z |

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
   27 | import org.springframework.core.Ordered;
   28 | import org.springframework.security.access.annotation.Secured;
   29 | import org.springframework.security.config.annotation.authentication.configuration.EnableGlobalAuthentication;
   30 | 
   31 | /**
   32 |  * <p>
   33 |  * Enables Spring Security global method security similar to the
   34 |  * &lt;global-method-security&gt; xml support.
   35 |  *
   36 |  * <p>
   37 |  * More advanced configurations may wish to extend
   38 |  * {@link GlobalMethodSecurityConfiguration} and override the protected methods to provide
   39 |  * custom implementations. Note that {@link EnableGlobalMethodSecurity} still must be
   40 |  * included on the class extending {@link GlobalMethodSecurityConfiguration} to determine
   41 |  * the settings.
   42 |  *
   43 |  * @author Rob Winch
   44 |  * @since 3.2
   45 |  * @deprecated Use {@link EnableMethodSecurity} instead
   46 |  */
   47 | @Deprecated
   48 | @Retention(RetentionPolicy.RUNTIME)
   49 | @Target(ElementType.TYPE)
   50 | @Documented
   51 | @Import({ GlobalMethodSecuritySelector.class })
   52 | @EnableGlobalAuthentication
   53 | public @interface EnableGlobalMethodSecurity {
   54 | 
   55 | 	/**
   56 | 	 * Determines if Spring Security's pre post annotations should be enabled. Default is
   57 | 	 * false.
   58 | 	 * @return true if pre post annotations should be enabled false otherwise.
   59 | 	 */
   60 | 	boolean prePostEnabled() default false;
   61 | 
   62 | 	/**
   63 | 	 * Determines if Spring Security's {@link Secured} annotations should be enabled.
   64 | 	 * @return true if {@link Secured} annotations should be enabled false otherwise.
   65 | 	 * Default is false.
   66 | 	 */
   67 | 	boolean securedEnabled() default false;
   68 | 
   69 | 	/**
   70 | 	 * Determines if JSR-250 annotations should be enabled. Default is false.
   71 | 	 * @return true if JSR-250 should be enabled false otherwise.
   72 | 	 */
   73 | 	boolean jsr250Enabled() default false;
   74 | 
   75 | 	/**
   76 | 	 * Indicate whether subclass-based (CGLIB) proxies are to be created ({@code true}) as
   77 | 	 * opposed to standard Java interface-based proxies ({@code false}). The default is
   78 | 	 * {@code false}. <strong>Applicable only if {@link #mode()} is set to
   79 | 	 * {@link AdviceMode#PROXY}</strong>.
   80 | 	 *
   81 | 	 * <p>
   82 | 	 * Note that setting this attribute to {@code true} will affect <em>all</em>
   83 | 	 * Spring-managed beans requiring proxying, not just those marked with the Security
   84 | 	 * annotations. For example, other beans marked with Spring's {@code @Transactional}
   85 | 	 * annotation will be upgraded to subclass proxying at the same time. This approach
   86 | 	 * has no negative impact in practice unless one is explicitly expecting one type of
   87 | 	 * proxy vs another, e.g. in tests.
   88 | 	 * @return true if CGILIB proxies should be created instead of interface based
   89 | 	 * proxies, else false
   90 | 	 */
   91 | 	boolean proxyTargetClass() default false;
   92 | 
   93 | 	/**
   94 | 	 * Indicate how security advice should be applied. The default is
   95 | 	 * {@link AdviceMode#PROXY}.
   96 | 	 * @see AdviceMode
   97 | 	 * @return the {@link AdviceMode} to use
   98 | 	 */
   99 | 	AdviceMode mode() default AdviceMode.PROXY;
  100 | 
  101 | 	/**
  102 | 	 * Indicate the ordering of the execution of the security advisor when multiple
  103 | 	 * advices are applied at a specific joinpoint. The default is
  104 | 	 * {@link Ordered#LOWEST_PRECEDENCE}.
  105 | 	 * @return the order the security advisor should be applied
  106 | 	 */
  107 | 	int order() default Ordered.LOWEST_PRECEDENCE;
  108 | 
  109 | }
```
