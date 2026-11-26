<!-- learning-docs source archive（机械抓取原文，禁止手改代码内容） -->
<!-- origin: org.springframework.cloud:spring-cloud-commons:5.1.0 | path: org/springframework/cloud/client/loadbalancer/LoadBalancerInterceptor.java -->
<!-- 正文引用：[[源码:LoadBalancerInterceptor.java#L起-L止]] ；代码块头部：(src: LoadBalancerInterceptor.java#L起-L止) -->

# LoadBalancerInterceptor.java

| 项 | 值 |
| --- | --- |
| 来源 | `org.springframework.cloud:spring-cloud-commons:5.1.0` |
| 路径 | `org/springframework/cloud/client/loadbalancer/LoadBalancerInterceptor.java` |
| 行数 | 58 |
| 在线地址 | https://repo1.maven.org/maven2/org/springframework/cloud/spring-cloud-commons/5.1.0/spring-cloud-commons-5.1.0-sources.jar |
| 抓取时间 | 2026-11-25T15:32:40.215Z |

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
   19 | import java.io.IOException;
   20 | import java.net.URI;
   21 | 
   22 | import org.springframework.http.HttpRequest;
   23 | import org.springframework.http.client.ClientHttpRequestExecution;
   24 | import org.springframework.http.client.ClientHttpResponse;
   25 | import org.springframework.util.Assert;
   26 | 
   27 | /**
   28 |  * @author Spencer Gibb
   29 |  * @author Dave Syer
   30 |  * @author Ryan Baxter
   31 |  * @author William Tran
   32 |  */
   33 | public class LoadBalancerInterceptor implements BlockingLoadBalancerInterceptor {
   34 | 
   35 | 	private final LoadBalancerClient loadBalancer;
   36 | 
   37 | 	private final LoadBalancerRequestFactory requestFactory;
   38 | 
   39 | 	public LoadBalancerInterceptor(LoadBalancerClient loadBalancer, LoadBalancerRequestFactory requestFactory) {
   40 | 		this.loadBalancer = loadBalancer;
   41 | 		this.requestFactory = requestFactory;
   42 | 	}
   43 | 
   44 | 	public LoadBalancerInterceptor(LoadBalancerClient loadBalancer) {
   45 | 		// for backwards compatibility
   46 | 		this(loadBalancer, new LoadBalancerRequestFactory(loadBalancer));
   47 | 	}
   48 | 
   49 | 	@Override
   50 | 	public ClientHttpResponse intercept(HttpRequest request, byte[] body, ClientHttpRequestExecution execution)
   51 | 			throws IOException {
   52 | 		URI originalUri = request.getURI();
   53 | 		String serviceName = originalUri.getHost();
   54 | 		Assert.state(serviceName != null, "Request URI does not contain a valid hostname: " + originalUri);
   55 | 		return loadBalancer.execute(serviceName, requestFactory.createRequest(request, body, execution));
   56 | 	}
   57 | 
   58 | }
```
