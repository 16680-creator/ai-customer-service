<!-- learning-docs source archive（机械抓取原文，禁止手改代码内容） -->
<!-- origin: org.springframework:spring-context:7.1.0 | path: org/springframework/context/annotation/EnableAspectJAutoProxy.java -->
<!-- 正文引用：[[源码:EnableAspectJAutoProxy.java#L起-L止]] ；代码块头部：(src: EnableAspectJAutoProxy.java#L起-L止) -->

# EnableAspectJAutoProxy.java

| 项 | 值 |
| --- | --- |
| 来源 | `org.springframework:spring-context:7.1.0` |
| 路径 | `org/springframework/context/annotation/EnableAspectJAutoProxy.java` |
| 行数 | 139 |
| 在线地址 | https://repo1.maven.org/maven2/org/springframework/spring-context/7.1.0/spring-context-7.1.0-sources.jar |
| 抓取时间 | 2026-11-25T14:00:02.051Z |

```java
    1 | /*
    2 |  * Copyright 2002-present the original author or authors.
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
   17 | package org.springframework.context.annotation;
   18 | 
   19 | import java.lang.annotation.Documented;
   20 | import java.lang.annotation.ElementType;
   21 | import java.lang.annotation.Retention;
   22 | import java.lang.annotation.RetentionPolicy;
   23 | import java.lang.annotation.Target;
   24 | 
   25 | /**
   26 |  * Enables support for handling components marked with AspectJ's {@code @Aspect} annotation,
   27 |  * similar to functionality found in Spring's {@code <aop:aspectj-autoproxy>} XML element.
   28 |  * To be used on @{@link Configuration} classes as follows:
   29 |  *
   30 |  * <pre class="code">
   31 |  * &#064;Configuration
   32 |  * &#064;EnableAspectJAutoProxy
   33 |  * public class AppConfig {
   34 |  *
   35 |  *     &#064;Bean
   36 |  *     public FooService fooService() {
   37 |  *         return new FooService();
   38 |  *     }
   39 |  *
   40 |  *     &#064;Bean
   41 |  *     public MyAspect myAspect() {
   42 |  *         return new MyAspect();
   43 |  *     }
   44 |  * }</pre>
   45 |  *
   46 |  * Where {@code FooService} is a typical POJO component and {@code MyAspect} is an
   47 |  * {@code @Aspect}-style aspect:
   48 |  *
   49 |  * <pre class="code">
   50 |  * public class FooService {
   51 |  *
   52 |  *     // various methods
   53 |  * }</pre>
   54 |  *
   55 |  * <pre class="code">
   56 |  * &#064;Aspect
   57 |  * public class MyAspect {
   58 |  *
   59 |  *     &#064;Before("execution(* FooService+.*(..))")
   60 |  *     public void advice() {
   61 |  *         // advise FooService methods as appropriate
   62 |  *     }
   63 |  * }</pre>
   64 |  *
   65 |  * In the scenario above, {@code @EnableAspectJAutoProxy} ensures that {@code MyAspect}
   66 |  * will be properly processed and that {@code FooService} will be proxied mixing in the
   67 |  * advice that it contributes.
   68 |  *
   69 |  * <p>Users can control the type of proxy that gets created for {@code FooService} using
   70 |  * the {@link #proxyTargetClass()} attribute. The following enables CGLIB-style 'subclass'
   71 |  * proxies as opposed to the default interface-based JDK proxy approach.
   72 |  *
   73 |  * <pre class="code">
   74 |  * &#064;Configuration
   75 |  * &#064;EnableAspectJAutoProxy(proxyTargetClass=true)
   76 |  * public class AppConfig {
   77 |  *     // ...
   78 |  * }</pre>
   79 |  *
   80 |  * <p>Note that {@code @Aspect} beans may be component-scanned like any other.
   81 |  * Simply mark the aspect with both {@code @Aspect} and {@code @Component}:
   82 |  *
   83 |  * <pre class="code">
   84 |  * package com.foo;
   85 |  *
   86 |  * &#064;Component
   87 |  * public class FooService { ... }
   88 |  *
   89 |  * &#064;Aspect
   90 |  * &#064;Component
   91 |  * public class MyAspect { ... }</pre>
   92 |  *
   93 |  * Then use the @{@link ComponentScan} annotation to pick both up:
   94 |  *
   95 |  * <pre class="code">
   96 |  * &#064;Configuration
   97 |  * &#064;ComponentScan("com.foo")
   98 |  * &#064;EnableAspectJAutoProxy
   99 |  * public class AppConfig {
  100 |  *
  101 |  *     // no explicit &#064;Bean definitions required
  102 |  * }</pre>
  103 |  *
  104 |  * <b>Note: {@code @EnableAspectJAutoProxy} applies to its local application context only,
  105 |  * allowing for selective proxying of beans at different levels.</b> Please redeclare
  106 |  * {@code @EnableAspectJAutoProxy} in each individual context, for example, the common root web
  107 |  * application context and any separate {@code DispatcherServlet} application contexts,
  108 |  * if you need to apply its behavior at multiple levels.
  109 |  *
  110 |  * <p>This feature requires the presence of {@code aspectjweaver} on the classpath.
  111 |  * While that dependency is optional for {@code spring-aop} in general, it is required
  112 |  * for {@code @EnableAspectJAutoProxy} and its underlying facilities.
  113 |  *
  114 |  * @author Chris Beams
  115 |  * @author Juergen Hoeller
  116 |  * @since 3.1
  117 |  * @see org.aspectj.lang.annotation.Aspect
  118 |  */
  119 | @Target(ElementType.TYPE)
  120 | @Retention(RetentionPolicy.RUNTIME)
  121 | @Documented
  122 | @Import(AspectJAutoProxyRegistrar.class)
  123 | public @interface EnableAspectJAutoProxy {
  124 | 
  125 | 	/**
  126 | 	 * Indicate whether subclass-based (CGLIB) proxies are to be created as opposed
  127 | 	 * to standard Java interface-based proxies. The default is {@code false}.
  128 | 	 */
  129 | 	boolean proxyTargetClass() default false;
  130 | 
  131 | 	/**
  132 | 	 * Indicate that the proxy should be exposed by the AOP framework as a {@code ThreadLocal}
  133 | 	 * for retrieval via the {@link org.springframework.aop.framework.AopContext} class.
  134 | 	 * Off by default, i.e. no guarantees that {@code AopContext} access will work.
  135 | 	 * @since 4.3.1
  136 | 	 */
  137 | 	boolean exposeProxy() default false;
  138 | 
  139 | }
```
