<!-- learning-docs source archive（机械抓取原文，禁止手改代码内容） -->
<!-- origin: org.springframework.cloud:spring-cloud-commons:5.1.0 | path: org/springframework/cloud/configuration/CompatibilityVerifierAutoConfiguration.java -->
<!-- 正文引用：[[源码:CompatibilityVerifierAutoConfiguration.java#L起-L止]] ；代码块头部：(src: CompatibilityVerifierAutoConfiguration.java#L起-L止) -->

# CompatibilityVerifierAutoConfiguration.java

| 项 | 值 |
| --- | --- |
| 来源 | `org.springframework.cloud:spring-cloud-commons:5.1.0` |
| 路径 | `org/springframework/cloud/configuration/CompatibilityVerifierAutoConfiguration.java` |
| 行数 | 53 |
| 在线地址 | https://repo1.maven.org/maven2/org/springframework/cloud/spring-cloud-commons/5.1.0/spring-cloud-commons-5.1.0-sources.jar |
| 抓取时间 | 2026-11-25T16:08:06.634Z |

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
   21 | import org.springframework.boot.autoconfigure.AutoConfigureOrder;
   22 | import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
   23 | import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
   24 | import org.springframework.boot.context.properties.EnableConfigurationProperties;
   25 | import org.springframework.context.annotation.Bean;
   26 | import org.springframework.context.annotation.Configuration;
   27 | 
   28 | /**
   29 |  * {@link EnableAutoConfiguration Auto-configuration} that fails the build fast for
   30 |  * incompatible versions of dependencies (e.g. invalid version of Boot).
   31 |  *
   32 |  * @author Marcin Grzejszczak
   33 |  * @since 1.3.6
   34 |  */
   35 | @Configuration(proxyBeanMethods = false)
   36 | @ConditionalOnProperty(value = "spring.cloud.compatibility-verifier.enabled", matchIfMissing = true)
   37 | @AutoConfigureOrder(0)
   38 | @EnableConfigurationProperties(CompatibilityVerifierProperties.class)
   39 | public class CompatibilityVerifierAutoConfiguration {
   40 | 
   41 | 	@Bean
   42 | 	CompositeCompatibilityVerifier compositeCompatibilityVerifier(List<CompatibilityVerifier> verifiers) {
   43 | 		CompositeCompatibilityVerifier verifier = new CompositeCompatibilityVerifier(verifiers);
   44 | 		verifier.verifyDependencies();
   45 | 		return verifier;
   46 | 	}
   47 | 
   48 | 	@Bean
   49 | 	SpringBootVersionVerifier springBootVersionVerifier(CompatibilityVerifierProperties properties) {
   50 | 		return new SpringBootVersionVerifier(properties.getCompatibleBootVersions());
   51 | 	}
   52 | 
   53 | }
```
