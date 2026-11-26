<!-- learning-docs source archive（机械抓取原文，禁止手改代码内容） -->
<!-- origin: org.springframework.cloud:spring-cloud-openfeign-core:5.1.0 | path: org/springframework/cloud/openfeign/SpringQueryMap.java -->
<!-- 正文引用：[[源码:SpringQueryMap.java#L起-L止]] ；代码块头部：(src: SpringQueryMap.java#L起-L止) -->

# SpringQueryMap.java

| 项 | 值 |
| --- | --- |
| 来源 | `org.springframework.cloud:spring-cloud-openfeign-core:5.1.0` |
| 路径 | `org/springframework/cloud/openfeign/SpringQueryMap.java` |
| 行数 | 37 |
| 在线地址 | https://repo1.maven.org/maven2/org/springframework/cloud/spring-cloud-openfeign-core/5.1.0/spring-cloud-openfeign-core-5.1.0-sources.jar |
| 抓取时间 | 2026-11-25T15:33:26.556Z |

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
   19 | import java.lang.annotation.ElementType;
   20 | import java.lang.annotation.Retention;
   21 | import java.lang.annotation.RetentionPolicy;
   22 | import java.lang.annotation.Target;
   23 | 
   24 | /**
   25 |  * Spring MVC equivalent of OpenFeign's {@link feign.QueryMap} parameter annotation.
   26 |  *
   27 |  * @author Aram Peres
   28 |  * @see feign.QueryMap
   29 |  * @see feign.QueryMapEncoder
   30 |  * @see org.springframework.cloud.openfeign.FeignClientsConfiguration
   31 |  * @see org.springframework.cloud.openfeign.annotation.QueryMapParameterProcessor
   32 |  */
   33 | @Retention(RetentionPolicy.RUNTIME)
   34 | @Target({ ElementType.PARAMETER })
   35 | public @interface SpringQueryMap {
   36 | 
   37 | }
```
