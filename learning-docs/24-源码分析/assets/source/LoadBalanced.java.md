<!-- learning-docs source archive（机械抓取原文，禁止手改代码内容） -->
<!-- origin: org.springframework.cloud:spring-cloud-commons:5.1.0 | path: org/springframework/cloud/client/loadbalancer/LoadBalanced.java -->
<!-- 正文引用：[[源码:LoadBalanced.java#L起-L止]] ；代码块头部：(src: LoadBalanced.java#L起-L止) -->

# LoadBalanced.java

| 项 | 值 |
| --- | --- |
| 来源 | `org.springframework.cloud:spring-cloud-commons:5.1.0` |
| 路径 | `org/springframework/cloud/client/loadbalancer/LoadBalanced.java` |
| 行数 | 41 |
| 在线地址 | https://repo1.maven.org/maven2/org/springframework/cloud/spring-cloud-commons/5.1.0/spring-cloud-commons-5.1.0-sources.jar |
| 抓取时间 | 2026-11-25T15:32:40.413Z |

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
   17 | package org.springframework.cloud.client.loadbalancer;
   18 | 
   19 | import java.lang.annotation.Documented;
   20 | import java.lang.annotation.ElementType;
   21 | import java.lang.annotation.Inherited;
   22 | import java.lang.annotation.Retention;
   23 | import java.lang.annotation.RetentionPolicy;
   24 | import java.lang.annotation.Target;
   25 | 
   26 | import org.springframework.beans.factory.annotation.Qualifier;
   27 | 
   28 | /**
   29 |  * Annotation to mark a RestTemplate, RestClient.Builder or WebClient.Builder bean to be
   30 |  * configured to use a LoadBalancerClient.
   31 |  *
   32 |  * @author Spencer Gibb
   33 |  */
   34 | @Target({ ElementType.FIELD, ElementType.PARAMETER, ElementType.METHOD })
   35 | @Retention(RetentionPolicy.RUNTIME)
   36 | @Documented
   37 | @Inherited
   38 | @Qualifier
   39 | public @interface LoadBalanced {
   40 | 
   41 | }
```
