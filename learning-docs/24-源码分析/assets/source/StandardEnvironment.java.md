<!-- learning-docs source archive（机械抓取原文，禁止手改代码内容） -->
<!-- origin: org.springframework:spring-core:7.1.0 | path: org/springframework/core/env/StandardEnvironment.java -->
<!-- 正文引用：[[源码:StandardEnvironment.java#L起-L止]] ；代码块头部：(src: StandardEnvironment.java#L起-L止) -->

# StandardEnvironment.java

| 项 | 值 |
| --- | --- |
| 来源 | `org.springframework:spring-core:7.1.0` |
| 路径 | `org/springframework/core/env/StandardEnvironment.java` |
| 行数 | 103 |
| 在线地址 | https://repo1.maven.org/maven2/org/springframework/spring-core/7.1.0/spring-core-7.1.0-sources.jar |
| 抓取时间 | 2026-11-25T14:14:36.605Z |

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
   17 | package org.springframework.core.env;
   18 | 
   19 | /**
   20 |  * {@link Environment} implementation suitable for use in 'standard' (i.e. non-web)
   21 |  * applications.
   22 |  *
   23 |  * <p>In addition to the usual functions of a {@link ConfigurableEnvironment} such as
   24 |  * property resolution and profile-related operations, this implementation configures two
   25 |  * default property sources, to be searched in the following order:
   26 |  * <ul>
   27 |  * <li>{@linkplain AbstractEnvironment#getSystemProperties() system properties}
   28 |  * <li>{@linkplain AbstractEnvironment#getSystemEnvironment() system environment variables}
   29 |  * </ul>
   30 |  *
   31 |  * That is, if the key "xyz" is present both in the JVM system properties as well as in
   32 |  * the set of environment variables for the current process, the value of key "xyz" from
   33 |  * system properties will return from a call to {@code environment.getProperty("xyz")}.
   34 |  * This ordering is chosen by default because system properties are per-JVM, while
   35 |  * environment variables may be the same across many JVMs on a given system.  Giving
   36 |  * system properties precedence allows for overriding of environment variables on a
   37 |  * per-JVM basis.
   38 |  *
   39 |  * <p>These default property sources may be removed, reordered, or replaced; and
   40 |  * additional property sources may be added using the {@link MutablePropertySources}
   41 |  * instance available from {@link #getPropertySources()}. See
   42 |  * {@link ConfigurableEnvironment} Javadoc for usage examples.
   43 |  *
   44 |  * <p>See {@link SystemEnvironmentPropertySource} javadoc for details on special handling
   45 |  * of property names in shell environments (for example, Bash) that disallow period characters in
   46 |  * variable names.
   47 |  *
   48 |  * @author Chris Beams
   49 |  * @author Phillip Webb
   50 |  * @since 3.1
   51 |  * @see ConfigurableEnvironment
   52 |  * @see SystemEnvironmentPropertySource
   53 |  * @see org.springframework.web.context.support.StandardServletEnvironment
   54 |  */
   55 | public class StandardEnvironment extends AbstractEnvironment {
   56 | 
   57 | 	/** System environment property source name: {@value}. */
   58 | 	public static final String SYSTEM_ENVIRONMENT_PROPERTY_SOURCE_NAME = "systemEnvironment";
   59 | 
   60 | 	/** JVM system properties property source name: {@value}. */
   61 | 	public static final String SYSTEM_PROPERTIES_PROPERTY_SOURCE_NAME = "systemProperties";
   62 | 
   63 | 
   64 | 	/**
   65 | 	 * Create a new {@code StandardEnvironment} instance with a default
   66 | 	 * {@link MutablePropertySources} instance.
   67 | 	 */
   68 | 	public StandardEnvironment() {
   69 | 	}
   70 | 
   71 | 	/**
   72 | 	 * Create a new {@code StandardEnvironment} instance with a specific
   73 | 	 * {@link MutablePropertySources} instance.
   74 | 	 * @param propertySources property sources to use
   75 | 	 * @since 5.3.4
   76 | 	 */
   77 | 	protected StandardEnvironment(MutablePropertySources propertySources) {
   78 | 		super(propertySources);
   79 | 	}
   80 | 
   81 | 
   82 | 	/**
   83 | 	 * Customize the set of property sources with those appropriate for any standard
   84 | 	 * Java environment:
   85 | 	 * <ul>
   86 | 	 * <li>{@value #SYSTEM_PROPERTIES_PROPERTY_SOURCE_NAME}
   87 | 	 * <li>{@value #SYSTEM_ENVIRONMENT_PROPERTY_SOURCE_NAME}
   88 | 	 * </ul>
   89 | 	 * <p>Properties present in {@value #SYSTEM_PROPERTIES_PROPERTY_SOURCE_NAME} will
   90 | 	 * take precedence over those in {@value #SYSTEM_ENVIRONMENT_PROPERTY_SOURCE_NAME}.
   91 | 	 * @see AbstractEnvironment#customizePropertySources(MutablePropertySources)
   92 | 	 * @see #getSystemProperties()
   93 | 	 * @see #getSystemEnvironment()
   94 | 	 */
   95 | 	@Override
   96 | 	protected void customizePropertySources(MutablePropertySources propertySources) {
   97 | 		propertySources.addLast(
   98 | 				new PropertiesPropertySource(SYSTEM_PROPERTIES_PROPERTY_SOURCE_NAME, getSystemProperties()));
   99 | 		propertySources.addLast(
  100 | 				new SystemEnvironmentPropertySource(SYSTEM_ENVIRONMENT_PROPERTY_SOURCE_NAME, getSystemEnvironment()));
  101 | 	}
  102 | 
  103 | }
```
