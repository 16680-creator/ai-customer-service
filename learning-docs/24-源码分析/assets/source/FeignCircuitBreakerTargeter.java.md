<!-- learning-docs source archive（机械抓取原文，禁止手改代码内容） -->
<!-- origin: org.springframework.cloud:spring-cloud-openfeign-core:5.1.0 | path: org/springframework/cloud/openfeign/FeignCircuitBreakerTargeter.java -->
<!-- 正文引用：[[源码:FeignCircuitBreakerTargeter.java#L起-L止]] ；代码块头部：(src: FeignCircuitBreakerTargeter.java#L起-L止) -->

# FeignCircuitBreakerTargeter.java

| 项 | 值 |
| --- | --- |
| 来源 | `org.springframework.cloud:spring-cloud-openfeign-core:5.1.0` |
| 路径 | `org/springframework/cloud/openfeign/FeignCircuitBreakerTargeter.java` |
| 行数 | 117 |
| 在线地址 | https://repo1.maven.org/maven2/org/springframework/cloud/spring-cloud-openfeign-core/5.1.0/spring-cloud-openfeign-core-5.1.0-sources.jar |
| 抓取时间 | 2026-11-25T15:33:26.201Z |

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
   19 | import feign.Feign;
   20 | import feign.Target;
   21 | 
   22 | import org.springframework.beans.factory.FactoryBean;
   23 | import org.springframework.cloud.client.circuitbreaker.CircuitBreakerFactory;
   24 | import org.springframework.util.StringUtils;
   25 | 
   26 | /**
   27 |  * @author 黄学敏（huangxuemin)
   28 |  */
   29 | @SuppressWarnings("unchecked")
   30 | class FeignCircuitBreakerTargeter implements Targeter {
   31 | 
   32 | 	private final CircuitBreakerFactory circuitBreakerFactory;
   33 | 
   34 | 	private final boolean circuitBreakerGroupEnabled;
   35 | 
   36 | 	private final CircuitBreakerNameResolver circuitBreakerNameResolver;
   37 | 
   38 | 	FeignCircuitBreakerTargeter(CircuitBreakerFactory circuitBreakerFactory, boolean circuitBreakerGroupEnabled,
   39 | 			CircuitBreakerNameResolver circuitBreakerNameResolver) {
   40 | 		this.circuitBreakerFactory = circuitBreakerFactory;
   41 | 		this.circuitBreakerGroupEnabled = circuitBreakerGroupEnabled;
   42 | 		this.circuitBreakerNameResolver = circuitBreakerNameResolver;
   43 | 	}
   44 | 
   45 | 	@Override
   46 | 	public <T> T target(FeignClientFactoryBean factory, Feign.Builder feign, FeignClientFactory context,
   47 | 			Target.HardCodedTarget<T> target) {
   48 | 		if (!(feign instanceof FeignCircuitBreaker.Builder builder)) {
   49 | 			return feign.target(target);
   50 | 		}
   51 | 		String name = !StringUtils.hasText(factory.getContextId()) ? factory.getName() : factory.getContextId();
   52 | 		Class<?> fallback = factory.getFallback();
   53 | 		if (fallback != void.class) {
   54 | 			return targetWithFallback(name, context, target, builder, fallback);
   55 | 		}
   56 | 		Class<?> fallbackFactory = factory.getFallbackFactory();
   57 | 		if (fallbackFactory != void.class) {
   58 | 			return targetWithFallbackFactory(name, context, target, builder, fallbackFactory);
   59 | 		}
   60 | 		return builder(name, builder).target(target);
   61 | 	}
   62 | 
   63 | 	private <T> T targetWithFallbackFactory(String feignClientName, FeignClientFactory context,
   64 | 			Target.HardCodedTarget<T> target, FeignCircuitBreaker.Builder builder, Class<?> fallbackFactoryClass) {
   65 | 		FallbackFactory<? extends T> fallbackFactory = (FallbackFactory<? extends T>) getFromContext("fallbackFactory",
   66 | 				feignClientName, context, fallbackFactoryClass, FallbackFactory.class);
   67 | 		return builder(feignClientName, builder).target(target, fallbackFactory);
   68 | 	}
   69 | 
   70 | 	private <T> T targetWithFallback(String feignClientName, FeignClientFactory context,
   71 | 			Target.HardCodedTarget<T> target, FeignCircuitBreaker.Builder builder, Class<?> fallback) {
   72 | 		T fallbackInstance = getFromContext("fallback", feignClientName, context, fallback, target.type());
   73 | 		return builder(feignClientName, builder).target(target, fallbackInstance);
   74 | 	}
   75 | 
   76 | 	private <T> T getFromContext(String fallbackMechanism, String feignClientName, FeignClientFactory context,
   77 | 			Class<?> beanType, Class<T> targetType) {
   78 | 		Object fallbackInstance = context.getInstance(feignClientName, beanType);
   79 | 		if (fallbackInstance == null) {
   80 | 			throw new IllegalStateException(
   81 | 					String.format("No " + fallbackMechanism + " instance of type %s found for feign client %s",
   82 | 							beanType, feignClientName));
   83 | 		}
   84 | 
   85 | 		if (fallbackInstance instanceof FactoryBean<?> factoryBean) {
   86 | 			try {
   87 | 				fallbackInstance = factoryBean.getObject();
   88 | 			}
   89 | 			catch (Exception e) {
   90 | 				throw new IllegalStateException(fallbackMechanism + " create fail", e);
   91 | 			}
   92 | 
   93 | 			if (!targetType.isAssignableFrom(fallbackInstance.getClass())) {
   94 | 				throw new IllegalStateException(String.format("Incompatible " + fallbackMechanism
   95 | 						+ " instance. Fallback/fallbackFactory of type %s is not assignable to %s for feign client %s",
   96 | 						fallbackInstance.getClass(), targetType, feignClientName));
   97 | 			}
   98 | 
   99 | 		}
  100 | 		else {
  101 | 			if (!targetType.isAssignableFrom(beanType)) {
  102 | 				throw new IllegalStateException(String.format("Incompatible " + fallbackMechanism
  103 | 						+ " instance. Fallback/fallbackFactory of type %s is not assignable to %s for feign client %s",
  104 | 						beanType, targetType, feignClientName));
  105 | 			}
  106 | 		}
  107 | 		return (T) fallbackInstance;
  108 | 	}
  109 | 
  110 | 	private FeignCircuitBreaker.Builder builder(String feignClientName, FeignCircuitBreaker.Builder builder) {
  111 | 		return builder.circuitBreakerFactory(circuitBreakerFactory)
  112 | 			.feignClientName(feignClientName)
  113 | 			.circuitBreakerGroupEnabled(circuitBreakerGroupEnabled)
  114 | 			.circuitBreakerNameResolver(circuitBreakerNameResolver);
  115 | 	}
  116 | 
  117 | }
```
