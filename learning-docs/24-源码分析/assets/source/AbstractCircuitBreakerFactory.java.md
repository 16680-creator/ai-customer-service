<!-- learning-docs source archive（机械抓取原文，禁止手改代码内容） -->
<!-- origin: org.springframework.cloud:spring-cloud-commons:5.1.0 | path: org/springframework/cloud/client/circuitbreaker/AbstractCircuitBreakerFactory.java -->
<!-- 正文引用：[[源码:AbstractCircuitBreakerFactory.java#L起-L止]] ；代码块头部：(src: AbstractCircuitBreakerFactory.java#L起-L止) -->

# AbstractCircuitBreakerFactory.java

| 项 | 值 |
| --- | --- |
| 来源 | `org.springframework.cloud:spring-cloud-commons:5.1.0` |
| 路径 | `org/springframework/cloud/client/circuitbreaker/AbstractCircuitBreakerFactory.java` |
| 行数 | 68 |
| 在线地址 | https://repo1.maven.org/maven2/org/springframework/cloud/spring-cloud-commons/5.1.0/spring-cloud-commons-5.1.0-sources.jar |
| 抓取时间 | 2026-11-25T15:33:21.347Z |

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
   17 | package org.springframework.cloud.client.circuitbreaker;
   18 | 
   19 | import java.util.concurrent.ConcurrentHashMap;
   20 | import java.util.function.Consumer;
   21 | import java.util.function.Function;
   22 | 
   23 | /**
   24 |  * Base class for factories which produce circuit breakers.
   25 |  *
   26 |  * @author Ryan Baxter
   27 |  */
   28 | public abstract class AbstractCircuitBreakerFactory<CONF, CONFB extends ConfigBuilder<CONF>> {
   29 | 
   30 | 	private final ConcurrentHashMap<String, CONF> configurations = new ConcurrentHashMap<>();
   31 | 
   32 | 	/**
   33 | 	 * Adds configurations for circuit breakers.
   34 | 	 * @param ids The id of the circuit breaker
   35 | 	 * @param consumer A configuration builder consumer, allows consumers to customize the
   36 | 	 * builder before the configuration is built
   37 | 	 */
   38 | 	public void configure(Consumer<CONFB> consumer, String... ids) {
   39 | 		for (String id : ids) {
   40 | 			CONFB builder = configBuilder(id);
   41 | 			consumer.accept(builder);
   42 | 			CONF conf = builder.build();
   43 | 			getConfigurations().put(id, conf);
   44 | 		}
   45 | 	}
   46 | 
   47 | 	/**
   48 | 	 * Gets the configurations for the circuit breakers.
   49 | 	 * @return The configurations
   50 | 	 */
   51 | 	protected ConcurrentHashMap<String, CONF> getConfigurations() {
   52 | 		return configurations;
   53 | 	}
   54 | 
   55 | 	/**
   56 | 	 * Creates a configuration builder for the given id.
   57 | 	 * @param id The id of the circuit breaker
   58 | 	 * @return The configuration builder
   59 | 	 */
   60 | 	protected abstract CONFB configBuilder(String id);
   61 | 
   62 | 	/**
   63 | 	 * Sets the default configuration for circuit breakers.
   64 | 	 * @param defaultConfiguration A function that returns the default configuration
   65 | 	 */
   66 | 	public abstract void configureDefault(Function<String, CONF> defaultConfiguration);
   67 | 
   68 | }
```
