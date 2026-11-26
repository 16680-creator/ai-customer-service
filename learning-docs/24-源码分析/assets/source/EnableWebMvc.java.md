<!-- learning-docs source archive（机械抓取原文，禁止手改代码内容） -->
<!-- origin: org.springframework:spring-webmvc:7.1.0 | path: org/springframework/web/servlet/config/annotation/EnableWebMvc.java -->
<!-- 正文引用：[[源码:EnableWebMvc.java#L起-L止]] ；代码块头部：(src: EnableWebMvc.java#L起-L止) -->

# EnableWebMvc.java

| 项 | 值 |
| --- | --- |
| 来源 | `org.springframework:spring-webmvc:7.1.0` |
| 路径 | `org/springframework/web/servlet/config/annotation/EnableWebMvc.java` |
| 行数 | 102 |
| 在线地址 | https://repo1.maven.org/maven2/org/springframework/spring-webmvc/7.1.0/spring-webmvc-7.1.0-sources.jar |
| 抓取时间 | 2026-11-25T14:01:14.419Z |

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
   17 | package org.springframework.web.servlet.config.annotation;
   18 | 
   19 | import java.lang.annotation.Documented;
   20 | import java.lang.annotation.ElementType;
   21 | import java.lang.annotation.Retention;
   22 | import java.lang.annotation.RetentionPolicy;
   23 | import java.lang.annotation.Target;
   24 | 
   25 | import org.springframework.context.annotation.Import;
   26 | 
   27 | /**
   28 |  * Adding this annotation to an {@code @Configuration} class imports the Spring MVC
   29 |  * configuration from {@link WebMvcConfigurationSupport}, for example:
   30 |  *
   31 |  * <pre class="code">
   32 |  * &#064;Configuration
   33 |  * &#064;EnableWebMvc
   34 |  * &#064;ComponentScan(basePackageClasses = MyConfiguration.class)
   35 |  * public class MyConfiguration {
   36 |  * }
   37 |  * </pre>
   38 |  *
   39 |  * <p>To customize the imported configuration, implement the interface
   40 |  * {@link WebMvcConfigurer} and override individual methods, for example:
   41 |  *
   42 |  * <pre class="code">
   43 |  * &#064;Configuration
   44 |  * &#064;EnableWebMvc
   45 |  * &#064;ComponentScan(basePackageClasses = MyConfiguration.class)
   46 |  * public class MyConfiguration implements WebMvcConfigurer {
   47 |  *
   48 |  *     &#064;Override
   49 |  *     public void addFormatters(FormatterRegistry formatterRegistry) {
   50 |  *         formatterRegistry.addConverter(new MyConverter());
   51 |  *     }
   52 |  *
   53 |  *     &#064;Override
   54 |  *     public void configureMessageConverters(List&lt;HttpMessageConverter&lt;?&gt;&gt; converters) {
   55 |  *         converters.add(new MyHttpMessageConverter());
   56 |  *     }
   57 |  *
   58 |  * }
   59 |  * </pre>
   60 |  *
   61 |  * <p><strong>Note:</strong> only one {@code @Configuration} class may have the
   62 |  * {@code @EnableWebMvc} annotation to import the Spring Web MVC
   63 |  * configuration. There can however be multiple {@code @Configuration} classes
   64 |  * implementing {@code WebMvcConfigurer} in order to customize the provided
   65 |  * configuration.
   66 |  *
   67 |  * <p>If {@link WebMvcConfigurer} does not expose some more advanced setting that
   68 |  * needs to be configured, consider removing the {@code @EnableWebMvc}
   69 |  * annotation and extending directly from {@link WebMvcConfigurationSupport}
   70 |  * or {@link DelegatingWebMvcConfiguration}, for example:
   71 |  *
   72 |  * <pre class="code">
   73 |  * &#064;Configuration
   74 |  * &#064;ComponentScan(basePackageClasses = { MyConfiguration.class })
   75 |  * public class MyConfiguration extends WebMvcConfigurationSupport {
   76 |  *
   77 |  *     &#064;Override
   78 |  *     public void addFormatters(FormatterRegistry formatterRegistry) {
   79 |  *         formatterRegistry.addConverter(new MyConverter());
   80 |  *     }
   81 |  *
   82 |  *     &#064;Bean
   83 |  *     public RequestMappingHandlerAdapter requestMappingHandlerAdapter() {
   84 |  *         // Create or delegate to "super" to create and
   85 |  *         // customize properties of RequestMappingHandlerAdapter
   86 |  *     }
   87 |  * }
   88 |  * </pre>
   89 |  *
   90 |  * @author Dave Syer
   91 |  * @author Rossen Stoyanchev
   92 |  * @since 3.1
   93 |  * @see org.springframework.web.servlet.config.annotation.WebMvcConfigurer
   94 |  * @see org.springframework.web.servlet.config.annotation.WebMvcConfigurationSupport
   95 |  * @see org.springframework.web.servlet.config.annotation.DelegatingWebMvcConfiguration
   96 |  */
   97 | @Retention(RetentionPolicy.RUNTIME)
   98 | @Target(ElementType.TYPE)
   99 | @Documented
  100 | @Import(DelegatingWebMvcConfiguration.class)
  101 | public @interface EnableWebMvc {
  102 | }
```
