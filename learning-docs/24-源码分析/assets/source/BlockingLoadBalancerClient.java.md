<!-- learning-docs source archive（机械抓取原文，禁止手改代码内容） -->
<!-- origin: org.springframework.cloud:spring-cloud-loadbalancer:5.1.0 | path: org/springframework/cloud/loadbalancer/blocking/client/BlockingLoadBalancerClient.java -->
<!-- 正文引用：[[源码:BlockingLoadBalancerClient.java#L起-L止]] ；代码块头部：(src: BlockingLoadBalancerClient.java#L起-L止) -->

# BlockingLoadBalancerClient.java

| 项 | 值 |
| --- | --- |
| 来源 | `org.springframework.cloud:spring-cloud-loadbalancer:5.1.0` |
| 路径 | `org/springframework/cloud/loadbalancer/blocking/client/BlockingLoadBalancerClient.java` |
| 行数 | 177 |
| 在线地址 | https://repo1.maven.org/maven2/org/springframework/cloud/spring-cloud-loadbalancer/5.1.0/spring-cloud-loadbalancer-5.1.0-sources.jar |
| 抓取时间 | 2026-11-25T15:33:02.953Z |

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
   17 | package org.springframework.cloud.loadbalancer.blocking.client;
   18 | 
   19 | import java.io.IOException;
   20 | import java.net.URI;
   21 | import java.util.Set;
   22 | 
   23 | import reactor.core.publisher.Mono;
   24 | 
   25 | import org.springframework.cloud.client.ServiceInstance;
   26 | import org.springframework.cloud.client.loadbalancer.CompletionContext;
   27 | import org.springframework.cloud.client.loadbalancer.DefaultRequest;
   28 | import org.springframework.cloud.client.loadbalancer.DefaultRequestContext;
   29 | import org.springframework.cloud.client.loadbalancer.DefaultResponse;
   30 | import org.springframework.cloud.client.loadbalancer.EmptyResponse;
   31 | import org.springframework.cloud.client.loadbalancer.HttpRequestLoadBalancerRequest;
   32 | import org.springframework.cloud.client.loadbalancer.LoadBalancerClient;
   33 | import org.springframework.cloud.client.loadbalancer.LoadBalancerLifecycle;
   34 | import org.springframework.cloud.client.loadbalancer.LoadBalancerLifecycleValidator;
   35 | import org.springframework.cloud.client.loadbalancer.LoadBalancerProperties;
   36 | import org.springframework.cloud.client.loadbalancer.LoadBalancerRequest;
   37 | import org.springframework.cloud.client.loadbalancer.LoadBalancerRequestAdapter;
   38 | import org.springframework.cloud.client.loadbalancer.LoadBalancerUriTools;
   39 | import org.springframework.cloud.client.loadbalancer.Request;
   40 | import org.springframework.cloud.client.loadbalancer.RequestData;
   41 | import org.springframework.cloud.client.loadbalancer.RequestDataContext;
   42 | import org.springframework.cloud.client.loadbalancer.Response;
   43 | import org.springframework.cloud.client.loadbalancer.ResponseData;
   44 | import org.springframework.cloud.client.loadbalancer.TimedRequestContext;
   45 | import org.springframework.cloud.client.loadbalancer.reactive.ReactiveLoadBalancer;
   46 | import org.springframework.http.HttpRequest;
   47 | import org.springframework.http.client.ClientHttpResponse;
   48 | import org.springframework.util.ReflectionUtils;
   49 | 
   50 | import static org.springframework.cloud.client.loadbalancer.reactive.ReactiveLoadBalancer.REQUEST;
   51 | 
   52 | /**
   53 |  * The default {@link LoadBalancerClient} implementation.
   54 |  *
   55 |  * @author Olga Maciaszek-Sharma
   56 |  * @since 2.2.0
   57 |  */
   58 | @SuppressWarnings({ "unchecked", "rawtypes" })
   59 | public class BlockingLoadBalancerClient implements LoadBalancerClient {
   60 | 
   61 | 	private final ReactiveLoadBalancer.Factory<ServiceInstance> loadBalancerClientFactory;
   62 | 
   63 | 	public BlockingLoadBalancerClient(ReactiveLoadBalancer.Factory<ServiceInstance> loadBalancerClientFactory) {
   64 | 		this.loadBalancerClientFactory = loadBalancerClientFactory;
   65 | 	}
   66 | 
   67 | 	@Override
   68 | 	public <T> T execute(String serviceId, LoadBalancerRequest<T> request) throws IOException {
   69 | 		String hint = getHint(serviceId);
   70 | 		LoadBalancerRequestAdapter<T, TimedRequestContext> lbRequest = new LoadBalancerRequestAdapter<>(request,
   71 | 				buildRequestContext(request, hint));
   72 | 		Set<LoadBalancerLifecycle> supportedLifecycleProcessors = getSupportedLifecycleProcessors(serviceId);
   73 | 		supportedLifecycleProcessors.forEach(lifecycle -> lifecycle.onStart(lbRequest));
   74 | 		ServiceInstance serviceInstance = choose(serviceId, lbRequest);
   75 | 		if (serviceInstance == null) {
   76 | 			supportedLifecycleProcessors.forEach(lifecycle -> lifecycle
   77 | 				.onComplete(new CompletionContext<>(CompletionContext.Status.DISCARD, lbRequest, new EmptyResponse())));
   78 | 			throw new IllegalStateException("No instances available for " + serviceId);
   79 | 		}
   80 | 		return execute(serviceId, serviceInstance, lbRequest);
   81 | 	}
   82 | 
   83 | 	private <T> TimedRequestContext buildRequestContext(LoadBalancerRequest<T> delegate, String hint) {
   84 | 		if (delegate instanceof HttpRequestLoadBalancerRequest httpRequestLoadBalancerRequest) {
   85 | 			HttpRequest request = httpRequestLoadBalancerRequest.getHttpRequest();
   86 | 			if (request != null) {
   87 | 				RequestData requestData = new RequestData(request);
   88 | 				return new RequestDataContext(requestData, hint);
   89 | 			}
   90 | 		}
   91 | 		return new DefaultRequestContext(delegate, hint);
   92 | 	}
   93 | 
   94 | 	@Override
   95 | 	public <T> T execute(String serviceId, ServiceInstance serviceInstance, LoadBalancerRequest<T> request)
   96 | 			throws IOException {
   97 | 		if (serviceInstance == null) {
   98 | 			throw new IllegalArgumentException("Service Instance cannot be null, serviceId: " + serviceId);
   99 | 		}
  100 | 		DefaultResponse defaultResponse = new DefaultResponse(serviceInstance);
  101 | 		Set<LoadBalancerLifecycle> supportedLifecycleProcessors = getSupportedLifecycleProcessors(serviceId);
  102 | 		Request lbRequest = request instanceof Request ? (Request) request : new DefaultRequest<>();
  103 | 		supportedLifecycleProcessors
  104 | 			.forEach(lifecycle -> lifecycle.onStartRequest(lbRequest, new DefaultResponse(serviceInstance)));
  105 | 		try {
  106 | 			T response = request.apply(serviceInstance);
  107 | 			Object clientResponse = getClientResponse(response);
  108 | 			supportedLifecycleProcessors
  109 | 				.forEach(lifecycle -> lifecycle.onComplete(new CompletionContext<>(CompletionContext.Status.SUCCESS,
  110 | 						lbRequest, defaultResponse, clientResponse)));
  111 | 			return response;
  112 | 		}
  113 | 		catch (IOException iOException) {
  114 | 			supportedLifecycleProcessors.forEach(lifecycle -> lifecycle.onComplete(
  115 | 					new CompletionContext<>(CompletionContext.Status.FAILED, iOException, lbRequest, defaultResponse)));
  116 | 			throw iOException;
  117 | 		}
  118 | 		catch (Exception exception) {
  119 | 			supportedLifecycleProcessors.forEach(lifecycle -> lifecycle.onComplete(
  120 | 					new CompletionContext<>(CompletionContext.Status.FAILED, exception, lbRequest, defaultResponse)));
  121 | 			ReflectionUtils.rethrowRuntimeException(exception);
  122 | 		}
  123 | 		return null;
  124 | 	}
  125 | 
  126 | 	private <T> Object getClientResponse(T response) {
  127 | 		ClientHttpResponse clientHttpResponse = null;
  128 | 		if (response instanceof ClientHttpResponse httpResponse) {
  129 | 			clientHttpResponse = httpResponse;
  130 | 		}
  131 | 		if (clientHttpResponse != null) {
  132 | 			try {
  133 | 				return new ResponseData(clientHttpResponse, null);
  134 | 			}
  135 | 			catch (IOException ignored) {
  136 | 			}
  137 | 		}
  138 | 		return response;
  139 | 	}
  140 | 
  141 | 	private Set<LoadBalancerLifecycle> getSupportedLifecycleProcessors(String serviceId) {
  142 | 		return LoadBalancerLifecycleValidator.getSupportedLifecycleProcessors(
  143 | 				loadBalancerClientFactory.getInstances(serviceId, LoadBalancerLifecycle.class),
  144 | 				DefaultRequestContext.class, Object.class, ServiceInstance.class);
  145 | 	}
  146 | 
  147 | 	@Override
  148 | 	public URI reconstructURI(ServiceInstance serviceInstance, URI original) {
  149 | 		return LoadBalancerUriTools.reconstructURI(serviceInstance, original);
  150 | 	}
  151 | 
  152 | 	@Override
  153 | 	public ServiceInstance choose(String serviceId) {
  154 | 		return choose(serviceId, REQUEST);
  155 | 	}
  156 | 
  157 | 	@Override
  158 | 	public <T> ServiceInstance choose(String serviceId, Request<T> request) {
  159 | 		ReactiveLoadBalancer<ServiceInstance> loadBalancer = loadBalancerClientFactory.getInstance(serviceId);
  160 | 		if (loadBalancer == null) {
  161 | 			return null;
  162 | 		}
  163 | 		Response<ServiceInstance> loadBalancerResponse = Mono.from(loadBalancer.choose(request)).block();
  164 | 		if (loadBalancerResponse == null) {
  165 | 			return null;
  166 | 		}
  167 | 		return loadBalancerResponse.getServer();
  168 | 	}
  169 | 
  170 | 	private String getHint(String serviceId) {
  171 | 		LoadBalancerProperties properties = loadBalancerClientFactory.getProperties(serviceId);
  172 | 		String defaultHint = properties.getHint().getOrDefault("default", "default");
  173 | 		String hintPropertyValue = properties.getHint().get(serviceId);
  174 | 		return hintPropertyValue != null ? hintPropertyValue : defaultHint;
  175 | 	}
  176 | 
  177 | }
```
