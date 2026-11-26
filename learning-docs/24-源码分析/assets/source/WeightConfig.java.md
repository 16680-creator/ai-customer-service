<!-- learning-docs source archive（机械抓取原文，禁止手改代码内容） -->
<!-- origin: org.springframework.cloud:spring-cloud-gateway-server-webflux:5.1.0 | path: org/springframework/cloud/gateway/support/WeightConfig.java -->
<!-- 正文引用：[[源码:WeightConfig.java#L起-L止]] ；代码块头部：(src: WeightConfig.java#L起-L止) -->

# WeightConfig.java

| 项 | 值 |
| --- | --- |
| 来源 | `org.springframework.cloud:spring-cloud-gateway-server-webflux:5.1.0` |
| 路径 | `org/springframework/cloud/gateway/support/WeightConfig.java` |
| 行数 | 89 |
| 在线地址 | https://repo1.maven.org/maven2/org/springframework/cloud/spring-cloud-gateway-server-webflux/5.1.0/spring-cloud-gateway-server-webflux-5.1.0-sources.jar |
| 抓取时间 | 2026-11-26T08:49:14.126Z |

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
   17 | package org.springframework.cloud.gateway.support;
   18 | 
   19 | import jakarta.validation.constraints.Min;
   20 | import jakarta.validation.constraints.NotEmpty;
   21 | 
   22 | import org.springframework.core.style.ToStringCreator;
   23 | import org.springframework.validation.annotation.Validated;
   24 | 
   25 | @Validated
   26 | public class WeightConfig {
   27 | 
   28 | 	/**
   29 | 	 * Configuration prefix for {@link WeightConfig}.
   30 | 	 */
   31 | 	public static final String CONFIG_PREFIX = "weight";
   32 | 
   33 | 	@NotEmpty
   34 | 	private String group;
   35 | 
   36 | 	private String routeId;
   37 | 
   38 | 	@Min(0)
   39 | 	private int weight;
   40 | 
   41 | 	private WeightConfig() {
   42 | 	}
   43 | 
   44 | 	public WeightConfig(String group, String routeId, int weight) {
   45 | 		this.routeId = routeId;
   46 | 		this.group = group;
   47 | 		this.weight = weight;
   48 | 	}
   49 | 
   50 | 	public WeightConfig(String routeId) {
   51 | 		this.routeId = routeId;
   52 | 	}
   53 | 
   54 | 	public String getGroup() {
   55 | 		return group;
   56 | 	}
   57 | 
   58 | 	public WeightConfig setGroup(String group) {
   59 | 		this.group = group;
   60 | 		return this;
   61 | 	}
   62 | 
   63 | 	public String getRouteId() {
   64 | 		return routeId;
   65 | 	}
   66 | 
   67 | 	public WeightConfig setRouteId(String routeId) {
   68 | 		this.routeId = routeId;
   69 | 		return this;
   70 | 	}
   71 | 
   72 | 	public int getWeight() {
   73 | 		return weight;
   74 | 	}
   75 | 
   76 | 	public WeightConfig setWeight(int weight) {
   77 | 		this.weight = weight;
   78 | 		return this;
   79 | 	}
   80 | 
   81 | 	@Override
   82 | 	public String toString() {
   83 | 		return new ToStringCreator(this).append("group", group)
   84 | 			.append("routeId", routeId)
   85 | 			.append("weight", weight)
   86 | 			.toString();
   87 | 	}
   88 | 
   89 | }
```
