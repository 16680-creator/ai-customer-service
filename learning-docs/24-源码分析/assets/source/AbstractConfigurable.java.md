<!-- learning-docs source archive（机械抓取原文，禁止手改代码内容） -->
<!-- origin: org.springframework.cloud:spring-cloud-gateway-server-webflux:5.1.0 | path: org/springframework/cloud/gateway/support/AbstractConfigurable.java -->
<!-- 正文引用：[[源码:AbstractConfigurable.java#L起-L止]] ；代码块头部：(src: AbstractConfigurable.java#L起-L止) -->

# AbstractConfigurable.java

| 项 | 值 |
| --- | --- |
| 来源 | `org.springframework.cloud:spring-cloud-gateway-server-webflux:5.1.0` |
| 路径 | `org/springframework/cloud/gateway/support/AbstractConfigurable.java` |
| 行数 | 45 |
| 在线地址 | https://repo1.maven.org/maven2/org/springframework/cloud/spring-cloud-gateway-server-webflux/5.1.0/spring-cloud-gateway-server-webflux-5.1.0-sources.jar |
| 抓取时间 | 2026-11-26T07:50:19.044Z |

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
   19 | import org.springframework.beans.BeanUtils;
   20 | import org.springframework.core.style.ToStringCreator;
   21 | 
   22 | public abstract class AbstractConfigurable<C> implements Configurable<C> {
   23 | 
   24 | 	private Class<C> configClass;
   25 | 
   26 | 	protected AbstractConfigurable(Class<C> configClass) {
   27 | 		this.configClass = configClass;
   28 | 	}
   29 | 
   30 | 	@Override
   31 | 	public Class<C> getConfigClass() {
   32 | 		return configClass;
   33 | 	}
   34 | 
   35 | 	@Override
   36 | 	public C newConfig() {
   37 | 		return BeanUtils.instantiateClass(this.configClass);
   38 | 	}
   39 | 
   40 | 	@Override
   41 | 	public String toString() {
   42 | 		return new ToStringCreator(this).append("configClass", configClass).toString();
   43 | 	}
   44 | 
   45 | }
```
