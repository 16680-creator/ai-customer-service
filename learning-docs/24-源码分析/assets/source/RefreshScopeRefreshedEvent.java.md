<!-- learning-docs source archive（机械抓取原文，禁止手改代码内容） -->
<!-- origin: org.springframework.cloud:spring-cloud-context:5.1.0 | path: org/springframework/cloud/context/scope/refresh/RefreshScopeRefreshedEvent.java -->
<!-- 正文引用：[[源码:RefreshScopeRefreshedEvent.java#L起-L止]] ；代码块头部：(src: RefreshScopeRefreshedEvent.java#L起-L止) -->

# RefreshScopeRefreshedEvent.java

| 项 | 值 |
| --- | --- |
| 来源 | `org.springframework.cloud:spring-cloud-context:5.1.0` |
| 路径 | `org/springframework/cloud/context/scope/refresh/RefreshScopeRefreshedEvent.java` |
| 行数 | 47 |
| 在线地址 | https://repo1.maven.org/maven2/org/springframework/cloud/spring-cloud-context/5.1.0/spring-cloud-context-5.1.0-sources.jar |
| 抓取时间 | 2026-11-25T15:45:38.282Z |

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
   17 | package org.springframework.cloud.context.scope.refresh;
   18 | 
   19 | import org.springframework.context.ApplicationEvent;
   20 | 
   21 | /**
   22 |  * @author Spencer Gibb
   23 |  */
   24 | @SuppressWarnings("serial")
   25 | public class RefreshScopeRefreshedEvent extends ApplicationEvent {
   26 | 
   27 | 	/**
   28 | 	 * Default name for the refresh scope refreshed event.
   29 | 	 */
   30 | 	public static final String DEFAULT_NAME = "__refreshAll__";
   31 | 
   32 | 	private String name;
   33 | 
   34 | 	public RefreshScopeRefreshedEvent() {
   35 | 		this(DEFAULT_NAME);
   36 | 	}
   37 | 
   38 | 	public RefreshScopeRefreshedEvent(String name) {
   39 | 		super(name);
   40 | 		this.name = name;
   41 | 	}
   42 | 
   43 | 	public String getName() {
   44 | 		return this.name;
   45 | 	}
   46 | 
   47 | }
```
