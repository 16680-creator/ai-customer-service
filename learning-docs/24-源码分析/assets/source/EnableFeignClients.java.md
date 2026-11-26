<!-- learning-docs source archive（机械抓取原文，禁止手改代码内容） -->
<!-- origin: org.springframework.cloud:spring-cloud-openfeign-core:5.1.0 | path: org/springframework/cloud/openfeign/EnableFeignClients.java -->
<!-- 正文引用：[[源码:EnableFeignClients.java#L起-L止]] ；代码块头部：(src: EnableFeignClients.java#L起-L止) -->

# EnableFeignClients.java

| 项 | 值 |
| --- | --- |
| 来源 | `org.springframework.cloud:spring-cloud-openfeign-core:5.1.0` |
| 路径 | `org/springframework/cloud/openfeign/EnableFeignClients.java` |
| 行数 | 90 |
| 在线地址 | https://repo1.maven.org/maven2/org/springframework/cloud/spring-cloud-openfeign-core/5.1.0/spring-cloud-openfeign-core-5.1.0-sources.jar |
| 抓取时间 | 2026-11-25T15:33:24.858Z |

```java
    1 | /*
    2 |  * Copyright 2013-present the original author or authors.
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
   17 | package org.springframework.cloud.openfeign;
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
   28 |  * Scans for interfaces that declare they are feign clients (via
   29 |  * {@link org.springframework.cloud.openfeign.FeignClient} <code>@FeignClient</code>).
   30 |  * Configures component scanning directives for use with
   31 |  * {@link org.springframework.context.annotation.Configuration}
   32 |  * <code>@Configuration</code> classes.
   33 |  *
   34 |  * @author Spencer Gibb
   35 |  * @author Dave Syer
   36 |  * @since 1.0
   37 |  */
   38 | @Retention(RetentionPolicy.RUNTIME)
   39 | @Target(ElementType.TYPE)
   40 | @Documented
   41 | @Import(FeignClientsRegistrar.class)
   42 | public @interface EnableFeignClients {
   43 | 
   44 | 	/**
   45 | 	 * Alias for the {@link #basePackages()} attribute. Allows for more concise annotation
   46 | 	 * declarations e.g.: {@code @ComponentScan("org.my.pkg")} instead of
   47 | 	 * {@code @ComponentScan(basePackages="org.my.pkg")}.
   48 | 	 * @return the array of 'basePackages'.
   49 | 	 */
   50 | 	String[] value() default {};
   51 | 
   52 | 	/**
   53 | 	 * Base packages to scan for annotated components.
   54 | 	 * <p>
   55 | 	 * {@link #value()} is an alias for (and mutually exclusive with) this attribute.
   56 | 	 * <p>
   57 | 	 * Use {@link #basePackageClasses()} for a type-safe alternative to String-based
   58 | 	 * package names.
   59 | 	 * @return the array of 'basePackages'.
   60 | 	 */
   61 | 	String[] basePackages() default {};
   62 | 
   63 | 	/**
   64 | 	 * Type-safe alternative to {@link #basePackages()} for specifying the packages to
   65 | 	 * scan for annotated components. The package of each class specified will be scanned.
   66 | 	 * <p>
   67 | 	 * Consider creating a special no-op marker class or interface in each package that
   68 | 	 * serves no purpose other than being referenced by this attribute.
   69 | 	 * @return the array of 'basePackageClasses'.
   70 | 	 */
   71 | 	Class<?>[] basePackageClasses() default {};
   72 | 
   73 | 	/**
   74 | 	 * A custom <code>@Configuration</code> for all feign clients. Can contain override
   75 | 	 * <code>@Bean</code> definition for the pieces that make up the client, for instance
   76 | 	 * {@link feign.codec.Decoder}, {@link feign.codec.Encoder}, {@link feign.Contract}.
   77 | 	 *
   78 | 	 * @see FeignClientsConfiguration for the defaults
   79 | 	 * @return list of default configurations
   80 | 	 */
   81 | 	Class<?>[] defaultConfiguration() default {};
   82 | 
   83 | 	/**
   84 | 	 * List of classes annotated with @FeignClient. If not empty, disables classpath
   85 | 	 * scanning.
   86 | 	 * @return list of FeignClient classes
   87 | 	 */
   88 | 	Class<?>[] clients() default {};
   89 | 
   90 | }
```
