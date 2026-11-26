<!-- learning-docs source archive（机械抓取原文，禁止手改代码内容） -->
<!-- origin: org.springframework.cloud:spring-cloud-commons:5.1.0 | path: org/springframework/cloud/configuration/CompatibilityVerifierProperties.java -->
<!-- 正文引用：[[源码:CompatibilityVerifierProperties.java#L起-L止]] ；代码块头部：(src: CompatibilityVerifierProperties.java#L起-L止) -->

# CompatibilityVerifierProperties.java

| 项 | 值 |
| --- | --- |
| 来源 | `org.springframework.cloud:spring-cloud-commons:5.1.0` |
| 路径 | `org/springframework/cloud/configuration/CompatibilityVerifierProperties.java` |
| 行数 | 58 |
| 在线地址 | https://repo1.maven.org/maven2/org/springframework/cloud/spring-cloud-commons/5.1.0/spring-cloud-commons-5.1.0-sources.jar |
| 抓取时间 | 2026-11-25T16:07:51.426Z |

```java
    1 | /*
    2 |  * Copyright 2012-present the original author or authors.
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
   17 | package org.springframework.cloud.configuration;
   18 | 
   19 | import java.util.List;
   20 | 
   21 | import org.springframework.boot.context.properties.ConfigurationProperties;
   22 | 
   23 | /**
   24 |  * @author Marcin Grzejszczak
   25 |  * @author Olga Maciaszek-Sharma
   26 |  */
   27 | @ConfigurationProperties("spring.cloud.compatibility-verifier")
   28 | public class CompatibilityVerifierProperties {
   29 | 
   30 | 	/**
   31 | 	 * Enables creation of Spring Cloud compatibility verification.
   32 | 	 */
   33 | 	private boolean enabled;
   34 | 
   35 | 	/**
   36 | 	 * Default accepted versions for the Spring Boot dependency. You can set {@code x} for
   37 | 	 * the patch version if you don't want to specify a concrete value. Example:
   38 | 	 * {@code 3.5.x}
   39 | 	 */
   40 | 	private List<String> compatibleBootVersions = List.of("4.2.x");
   41 | 
   42 | 	public boolean isEnabled() {
   43 | 		return this.enabled;
   44 | 	}
   45 | 
   46 | 	public void setEnabled(boolean enabled) {
   47 | 		this.enabled = enabled;
   48 | 	}
   49 | 
   50 | 	public List<String> getCompatibleBootVersions() {
   51 | 		return this.compatibleBootVersions;
   52 | 	}
   53 | 
   54 | 	public void setCompatibleBootVersions(List<String> compatibleBootVersions) {
   55 | 		this.compatibleBootVersions = compatibleBootVersions;
   56 | 	}
   57 | 
   58 | }
```
