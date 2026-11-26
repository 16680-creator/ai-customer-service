<!-- learning-docs source archive（机械抓取原文，禁止手改代码内容） -->
<!-- origin: org.springframework.cloud:spring-cloud-openfeign-core:5.1.0 | path: org/springframework/cloud/openfeign/loadbalancer/FeignBlockingLoadBalancerClient.java -->
<!-- 正文引用：[[源码:FeignBlockingLoadBalancerClient.java#L起-L止]] ；代码块头部：(src: FeignBlockingLoadBalancerClient.java#L起-L止) -->

# FeignBlockingLoadBalancerClient.java

| 项 | 值 |
| --- | --- |
| 来源 | `org.springframework.cloud:spring-cloud-openfeign-core:5.1.0` |
| 路径 | `org/springframework/cloud/openfeign/loadbalancer/FeignBlockingLoadBalancerClient.java` |
| 行数 | 141 |
| 在线地址 | https://repo1.maven.org/maven2/org/springframework/cloud/spring-cloud-openfeign-core/5.1.0/spring-cloud-openfeign-core-5.1.0-sources.jar |
| 抓取时间 | 2026-11-25T15:33:25.629Z |

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
   17 | package org.springframework.cloud.openfeign.loadbalancer;
   18 | 
   19 | import java.io.IOException;
   20 | import java.net.URI;
   21 | import java.nio.charset.StandardCharsets;
   22 | import java.util.List;
   23 | import java.util.Set;
   24 | 
   25 | import feign.Client;
   26 | import feign.Request;
   27 | import feign.Response;
   28 | import org.apache.commons.logging.Log;
   29 | import org.apache.commons.logging.LogFactory;
   30 | 
   31 | import org.springframework.cloud.client.ServiceInstance;
   32 | import org.springframework.cloud.client.loadbalancer.CompletionContext;
   33 | import org.springframework.cloud.client.loadbalancer.DefaultRequest;
   34 | import org.springframework.cloud.client.loadbalancer.DefaultResponse;
   35 | import org.springframework.cloud.client.loadbalancer.LoadBalancerClient;
   36 | import org.springframework.cloud.client.loadbalancer.LoadBalancerLifecycle;
   37 | import org.springframework.cloud.client.loadbalancer.LoadBalancerLifecycleValidator;
   38 | import org.springframework.cloud.client.loadbalancer.LoadBalancerProperties;
   39 | import org.springframework.cloud.client.loadbalancer.RequestDataContext;
   40 | import org.springframework.cloud.client.loadbalancer.ResponseData;
   41 | import org.springframework.cloud.loadbalancer.support.LoadBalancerClientFactory;
   42 | import org.springframework.http.HttpStatus;
   43 | import org.springframework.util.Assert;
   44 | 
   45 | import static org.springframework.cloud.openfeign.loadbalancer.LoadBalancerUtils.buildRequestData;
   46 | import static org.springframework.cloud.openfeign.loadbalancer.LoadBalancerUtils.executeWithLoadBalancerLifecycleProcessing;
   47 | 
   48 | /**
   49 |  * A {@link Client} implementation that uses {@link LoadBalancerClient} to select a
   50 |  * {@link ServiceInstance} to use while resolving the request host.
   51 |  *
   52 |  * @author Olga Maciaszek-Sharma
   53 |  * @author changjin wei(魏昌进)
   54 |  * @since 2.2.0
   55 |  */
   56 | @SuppressWarnings({ "unchecked", "rawtypes" })
   57 | public class FeignBlockingLoadBalancerClient implements Client {
   58 | 
   59 | 	private static final Log LOG = LogFactory.getLog(FeignBlockingLoadBalancerClient.class);
   60 | 
   61 | 	private final Client delegate;
   62 | 
   63 | 	private final LoadBalancerClient loadBalancerClient;
   64 | 
   65 | 	private final LoadBalancerClientFactory loadBalancerClientFactory;
   66 | 
   67 | 	private final List<LoadBalancerFeignRequestTransformer> transformers;
   68 | 
   69 | 	public FeignBlockingLoadBalancerClient(Client delegate, LoadBalancerClient loadBalancerClient,
   70 | 			LoadBalancerClientFactory loadBalancerClientFactory,
   71 | 			List<LoadBalancerFeignRequestTransformer> transformers) {
   72 | 		this.delegate = delegate;
   73 | 		this.loadBalancerClient = loadBalancerClient;
   74 | 		this.loadBalancerClientFactory = loadBalancerClientFactory;
   75 | 		this.transformers = transformers;
   76 | 	}
   77 | 
   78 | 	@Override
   79 | 	public Response execute(Request request, Request.Options options) throws IOException {
   80 | 		final URI originalUri = URI.create(request.url());
   81 | 		String serviceId = originalUri.getHost();
   82 | 		Assert.state(serviceId != null, "Request URI does not contain a valid hostname: " + originalUri);
   83 | 		String hint = getHint(serviceId);
   84 | 		DefaultRequest<RequestDataContext> lbRequest = new DefaultRequest<>(
   85 | 				new RequestDataContext(buildRequestData(request), hint));
   86 | 		Set<LoadBalancerLifecycle> supportedLifecycleProcessors = LoadBalancerLifecycleValidator
   87 | 			.getSupportedLifecycleProcessors(
   88 | 					loadBalancerClientFactory.getInstances(serviceId, LoadBalancerLifecycle.class),
   89 | 					RequestDataContext.class, ResponseData.class, ServiceInstance.class);
   90 | 		supportedLifecycleProcessors.forEach(lifecycle -> lifecycle.onStart(lbRequest));
   91 | 		ServiceInstance instance = loadBalancerClient.choose(serviceId, lbRequest);
   92 | 		org.springframework.cloud.client.loadbalancer.Response<ServiceInstance> lbResponse = new DefaultResponse(
   93 | 				instance);
   94 | 		if (instance == null) {
   95 | 			String message = "Load balancer does not contain an instance for the service " + serviceId;
   96 | 			if (LOG.isWarnEnabled()) {
   97 | 				LOG.warn(message);
   98 | 			}
   99 | 			supportedLifecycleProcessors.forEach(lifecycle -> lifecycle
  100 | 				.onComplete(new CompletionContext<ResponseData, ServiceInstance, RequestDataContext>(
  101 | 						CompletionContext.Status.DISCARD, lbRequest, lbResponse)));
  102 | 			return Response.builder()
  103 | 				.request(request)
  104 | 				.status(HttpStatus.SERVICE_UNAVAILABLE.value())
  105 | 				.body(message, StandardCharsets.UTF_8)
  106 | 				.build();
  107 | 		}
  108 | 		String reconstructedUrl = loadBalancerClient.reconstructURI(instance, originalUri).toString();
  109 | 		Request newRequest = buildRequest(request, reconstructedUrl, instance);
  110 | 		return executeWithLoadBalancerLifecycleProcessing(delegate, options, newRequest, lbRequest, lbResponse,
  111 | 				supportedLifecycleProcessors);
  112 | 	}
  113 | 
  114 | 	protected Request buildRequest(Request request, String reconstructedUrl) {
  115 | 		return Request.create(request.httpMethod(), reconstructedUrl, request.headers(), request.body(),
  116 | 				request.charset(), request.requestTemplate());
  117 | 	}
  118 | 
  119 | 	protected Request buildRequest(Request request, String reconstructedUrl, ServiceInstance instance) {
  120 | 		Request newRequest = buildRequest(request, reconstructedUrl);
  121 | 		if (transformers != null) {
  122 | 			for (LoadBalancerFeignRequestTransformer transformer : transformers) {
  123 | 				newRequest = transformer.transformRequest(newRequest, instance);
  124 | 			}
  125 | 		}
  126 | 		return newRequest;
  127 | 	}
  128 | 
  129 | 	// Visible for Sleuth instrumentation
  130 | 	public Client getDelegate() {
  131 | 		return delegate;
  132 | 	}
  133 | 
  134 | 	private String getHint(String serviceId) {
  135 | 		LoadBalancerProperties properties = loadBalancerClientFactory.getProperties(serviceId);
  136 | 		String defaultHint = properties.getHint().getOrDefault("default", "default");
  137 | 		String hintPropertyValue = properties.getHint().get(serviceId);
  138 | 		return hintPropertyValue != null ? hintPropertyValue : defaultHint;
  139 | 	}
  140 | 
  141 | }
```
