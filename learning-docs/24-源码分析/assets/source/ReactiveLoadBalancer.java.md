<!-- learning-docs source archive（机械抓取原文，禁止手改代码内容） -->
<!-- origin: org.springframework.cloud:spring-cloud-commons:5.1.0 | path: org/springframework/cloud/client/loadbalancer/reactive/ReactiveLoadBalancer.java -->
<!-- 正文引用：[[源码:ReactiveLoadBalancer.java#L起-L止]] ；代码块头部：(src: ReactiveLoadBalancer.java#L起-L止) -->

# ReactiveLoadBalancer.java

| 项 | 值 |
| --- | --- |
| 来源 | `org.springframework.cloud:spring-cloud-commons:5.1.0` |
| 路径 | `org/springframework/cloud/client/loadbalancer/reactive/ReactiveLoadBalancer.java` |
| 行数 | 88 |
| 在线地址 | https://repo1.maven.org/maven2/org/springframework/cloud/spring-cloud-commons/5.1.0/spring-cloud-commons-5.1.0-sources.jar |
| 抓取时间 | 2026-11-25T15:32:51.340Z |

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
   17 | package org.springframework.cloud.client.loadbalancer.reactive;
   18 | 
   19 | import java.util.Map;
   20 | 
   21 | import org.jspecify.annotations.Nullable;
   22 | import org.reactivestreams.Publisher;
   23 | 
   24 | import org.springframework.cloud.client.loadbalancer.DefaultRequest;
   25 | import org.springframework.cloud.client.loadbalancer.DefaultRequestContext;
   26 | import org.springframework.cloud.client.loadbalancer.LoadBalancerProperties;
   27 | import org.springframework.cloud.client.loadbalancer.Request;
   28 | import org.springframework.cloud.client.loadbalancer.Response;
   29 | 
   30 | /**
   31 |  * Reactive load balancer.
   32 |  *
   33 |  * @param <T> type of the response
   34 |  * @author Spencer Gibb
   35 |  * @author Olga Maciaszek-Sharma
   36 |  */
   37 | public interface ReactiveLoadBalancer<T> {
   38 | 
   39 | 	/**
   40 | 	 * Default implementation of a request.
   41 | 	 */
   42 | 	Request<DefaultRequestContext> REQUEST = new DefaultRequest<>();
   43 | 
   44 | 	/**
   45 | 	 * Choose the next server based on the load balancing algorithm.
   46 | 	 * @param request - incoming request
   47 | 	 * @return publisher for the response
   48 | 	 */
   49 | 	@SuppressWarnings("rawtypes")
   50 | 	Publisher<Response<T>> choose(Request request);
   51 | 
   52 | 	default Publisher<Response<T>> choose() { // conflicting name
   53 | 		return choose(REQUEST);
   54 | 	}
   55 | 
   56 | 	interface Factory<T> {
   57 | 
   58 | 		default @Nullable LoadBalancerProperties getProperties(String serviceId) {
   59 | 			return null;
   60 | 		}
   61 | 
   62 | 		ReactiveLoadBalancer<T> getInstance(String serviceId);
   63 | 
   64 | 		/**
   65 | 		 * Allows accessing beans registered within client-specific LoadBalancer contexts.
   66 | 		 * @param name Name of the beans to be returned
   67 | 		 * @param type The class of the beans to be returned
   68 | 		 * @param <X> The type of the beans to be returned
   69 | 		 * @return a {@link Map} of beans
   70 | 		 * @see <code>@LoadBalancerClient</code>
   71 | 		 */
   72 | 		<X> Map<String, X> getInstances(String name, Class<X> type);
   73 | 
   74 | 		/**
   75 | 		 * Allows accessing a bean registered within client-specific LoadBalancer
   76 | 		 * contexts.
   77 | 		 * @param name Name of the bean to be returned
   78 | 		 * @param clazz The class of the bean to be returned
   79 | 		 * @param generics The classes of generic types of the bean to be returned
   80 | 		 * @param <X> The type of the bean to be returned
   81 | 		 * @return a {@link Map} of beans
   82 | 		 * @see <code>@LoadBalancerClient</code>
   83 | 		 */
   84 | 		<X> X getInstance(String name, Class<?> clazz, Class<?>... generics);
   85 | 
   86 | 	}
   87 | 
   88 | }
```
