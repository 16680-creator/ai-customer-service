<!-- learning-docs source archive（机械抓取原文，禁止手改代码内容） -->
<!-- origin: org.springframework.cloud:spring-cloud-gateway-server-webflux:5.1.0 | path: org/springframework/cloud/gateway/filter/factory/AbstractNameValueGatewayFilterFactory.java -->
<!-- 正文引用：[[源码:AbstractNameValueGatewayFilterFactory.java#L起-L止]] ；代码块头部：(src: AbstractNameValueGatewayFilterFactory.java#L起-L止) -->

# AbstractNameValueGatewayFilterFactory.java

| 项 | 值 |
| --- | --- |
| 来源 | `org.springframework.cloud:spring-cloud-gateway-server-webflux:5.1.0` |
| 路径 | `org/springframework/cloud/gateway/filter/factory/AbstractNameValueGatewayFilterFactory.java` |
| 行数 | 75 |
| 在线地址 | https://repo1.maven.org/maven2/org/springframework/cloud/spring-cloud-gateway-server-webflux/5.1.0/spring-cloud-gateway-server-webflux-5.1.0-sources.jar |
| 抓取时间 | 2026-11-26T08:49:14.297Z |

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
   17 | package org.springframework.cloud.gateway.filter.factory;
   18 | 
   19 | import java.util.Arrays;
   20 | import java.util.List;
   21 | 
   22 | import jakarta.validation.constraints.NotEmpty;
   23 | import org.jspecify.annotations.Nullable;
   24 | 
   25 | import org.springframework.cloud.gateway.filter.GatewayFilter;
   26 | import org.springframework.core.style.ToStringCreator;
   27 | import org.springframework.validation.annotation.Validated;
   28 | 
   29 | public abstract class AbstractNameValueGatewayFilterFactory
   30 | 		extends AbstractGatewayFilterFactory<AbstractNameValueGatewayFilterFactory.NameValueConfig> {
   31 | 
   32 | 	public AbstractNameValueGatewayFilterFactory() {
   33 | 		super(NameValueConfig.class);
   34 | 	}
   35 | 
   36 | 	@Override
   37 | 	public List<String> shortcutFieldOrder() {
   38 | 		return Arrays.asList(GatewayFilter.NAME_KEY, GatewayFilter.VALUE_KEY);
   39 | 	}
   40 | 
   41 | 	@Validated
   42 | 	public static class NameValueConfig {
   43 | 
   44 | 		@NotEmpty
   45 | 		protected @Nullable String name;
   46 | 
   47 | 		@NotEmpty
   48 | 		protected @Nullable String value;
   49 | 
   50 | 		public @Nullable String getName() {
   51 | 			return name;
   52 | 		}
   53 | 
   54 | 		public NameValueConfig setName(String name) {
   55 | 			this.name = name;
   56 | 			return this;
   57 | 		}
   58 | 
   59 | 		public @Nullable String getValue() {
   60 | 			return value;
   61 | 		}
   62 | 
   63 | 		public NameValueConfig setValue(String value) {
   64 | 			this.value = value;
   65 | 			return this;
   66 | 		}
   67 | 
   68 | 		@Override
   69 | 		public String toString() {
   70 | 			return new ToStringCreator(this).append("name", name).append("value", value).toString();
   71 | 		}
   72 | 
   73 | 	}
   74 | 
   75 | }
```
