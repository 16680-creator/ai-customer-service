<!-- learning-docs source archive（机械抓取原文，禁止手改代码内容） -->
<!-- origin: org.springframework.cloud:spring-cloud-context:5.1.0 | path: org/springframework/cloud/context/environment/EnvironmentChangeEvent.java -->
<!-- 正文引用：[[源码:EnvironmentChangeEvent.java#L起-L止]] ；代码块头部：(src: EnvironmentChangeEvent.java#L起-L止) -->

# EnvironmentChangeEvent.java

| 项 | 值 |
| --- | --- |
| 来源 | `org.springframework.cloud:spring-cloud-context:5.1.0` |
| 路径 | `org/springframework/cloud/context/environment/EnvironmentChangeEvent.java` |
| 行数 | 57 |
| 在线地址 | https://repo1.maven.org/maven2/org/springframework/cloud/spring-cloud-context/5.1.0/spring-cloud-context-5.1.0-sources.jar |
| 抓取时间 | 2026-11-25T15:31:37.858Z |

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
   17 | package org.springframework.cloud.context.environment;
   18 | 
   19 | import java.util.Set;
   20 | 
   21 | import org.springframework.context.ApplicationEvent;
   22 | import org.springframework.core.env.Environment;
   23 | 
   24 | /**
   25 |  * Event published to signal a change in the {@link Environment}.
   26 |  *
   27 |  * @author Dave Syer
   28 |  * @author Mikhail Polivakha
   29 |  */
   30 | @SuppressWarnings("serial")
   31 | public class EnvironmentChangeEvent extends ApplicationEvent {
   32 | 
   33 | 	private final Set<String> keys;
   34 | 
   35 | 	/**
   36 | 	 * @param keys Key set that represents the difference between old and new versions of
   37 | 	 * the {@link Environment}.
   38 | 	 */
   39 | 	public EnvironmentChangeEvent(Set<String> keys) {
   40 | 		// Backwards compatible constructor with less utility (practically no use at all)
   41 | 		this(keys, keys);
   42 | 	}
   43 | 
   44 | 	public EnvironmentChangeEvent(Object context, Set<String> keys) {
   45 | 		super(context);
   46 | 		this.keys = keys;
   47 | 	}
   48 | 
   49 | 	/**
   50 | 	 * @return Key set that represents the difference between old and new versions of the
   51 | 	 * {@link Environment}.
   52 | 	 */
   53 | 	public Set<String> getKeys() {
   54 | 		return this.keys;
   55 | 	}
   56 | 
   57 | }
```
