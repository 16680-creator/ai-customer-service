<!-- learning-docs source archive（机械抓取原文，禁止手改代码内容） -->
<!-- origin: org.springframework.cloud:spring-cloud-commons:5.1.0 | path: org/springframework/cloud/client/loadbalancer/reactive/ReactorLoadBalancerExchangeFilterFunction.java -->
<!-- 正文引用：[[源码:ReactorLoadBalancerExchangeFilterFunction.java#L起-L止]] ；代码块头部：(src: ReactorLoadBalancerExchangeFilterFunction.java#L起-L止) -->

# ReactorLoadBalancerExchangeFilterFunction.java

| 项 | 值 |
| --- | --- |
| 来源 | `org.springframework.cloud:spring-cloud-commons:5.1.0` |
| 路径 | `org/springframework/cloud/client/loadbalancer/reactive/ReactorLoadBalancerExchangeFilterFunction.java` |
| 行数 | 131 |
| 在线地址 | https://repo1.maven.org/maven2/org/springframework/cloud/spring-cloud-commons/5.1.0/spring-cloud-commons-5.1.0-sources.jar |
| 抓取时间 | 2026-11-25T15:32:51.282Z |

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
   19 | import java.net.URI;
   20 | import java.util.List;
   21 | import java.util.Set;
   22 | 
   23 | import org.apache.commons.logging.Log;
   24 | import org.apache.commons.logging.LogFactory;
   25 | import reactor.core.publisher.Mono;
   26 | 
   27 | import org.springframework.cloud.client.ServiceInstance;
   28 | import org.springframework.cloud.client.loadbalancer.CompletionContext;
   29 | import org.springframework.cloud.client.loadbalancer.DefaultRequest;
   30 | import org.springframework.cloud.client.loadbalancer.EmptyResponse;
   31 | import org.springframework.cloud.client.loadbalancer.LoadBalancerLifecycle;
   32 | import org.springframework.cloud.client.loadbalancer.LoadBalancerLifecycleValidator;
   33 | import org.springframework.cloud.client.loadbalancer.LoadBalancerProperties;
   34 | import org.springframework.cloud.client.loadbalancer.Request;
   35 | import org.springframework.cloud.client.loadbalancer.RequestData;
   36 | import org.springframework.cloud.client.loadbalancer.RequestDataContext;
   37 | import org.springframework.cloud.client.loadbalancer.Response;
   38 | import org.springframework.cloud.client.loadbalancer.ResponseData;
   39 | import org.springframework.http.HttpStatus;
   40 | import org.springframework.web.reactive.function.client.ClientRequest;
   41 | import org.springframework.web.reactive.function.client.ClientResponse;
   42 | import org.springframework.web.reactive.function.client.ExchangeFilterFunction;
   43 | import org.springframework.web.reactive.function.client.ExchangeFunction;
   44 | 
   45 | import static org.springframework.cloud.client.loadbalancer.reactive.ExchangeFilterFunctionUtils.buildClientRequest;
   46 | import static org.springframework.cloud.client.loadbalancer.reactive.ExchangeFilterFunctionUtils.getHint;
   47 | import static org.springframework.cloud.client.loadbalancer.reactive.ExchangeFilterFunctionUtils.serviceInstanceUnavailableMessage;
   48 | 
   49 | /**
   50 |  * An {@link ExchangeFilterFunction} that uses {@link ReactiveLoadBalancer} to execute
   51 |  * requests against a correct {@link ServiceInstance}.
   52 |  *
   53 |  * @author Olga Maciaszek-Sharma
   54 |  * @since 2.2.0
   55 |  */
   56 | @SuppressWarnings({ "rawtypes", "unchecked" })
   57 | public class ReactorLoadBalancerExchangeFilterFunction implements LoadBalancedExchangeFilterFunction {
   58 | 
   59 | 	private static final Log LOG = LogFactory.getLog(ReactorLoadBalancerExchangeFilterFunction.class);
   60 | 
   61 | 	private final ReactiveLoadBalancer.Factory<ServiceInstance> loadBalancerFactory;
   62 | 
   63 | 	private final List<LoadBalancerClientRequestTransformer> transformers;
   64 | 
   65 | 	public ReactorLoadBalancerExchangeFilterFunction(ReactiveLoadBalancer.Factory<ServiceInstance> loadBalancerFactory,
   66 | 			List<LoadBalancerClientRequestTransformer> transformers) {
   67 | 		this.loadBalancerFactory = loadBalancerFactory;
   68 | 		this.transformers = transformers;
   69 | 	}
   70 | 
   71 | 	@Override
   72 | 	public Mono<ClientResponse> filter(ClientRequest clientRequest, ExchangeFunction next) {
   73 | 		URI originalUrl = clientRequest.url();
   74 | 		String serviceId = originalUrl.getHost();
   75 | 		if (serviceId == null) {
   76 | 			String message = String.format("Request URI does not contain a valid hostname: %s", originalUrl);
   77 | 			if (LOG.isWarnEnabled()) {
   78 | 				LOG.warn(message);
   79 | 			}
   80 | 			return Mono.just(ClientResponse.create(HttpStatus.BAD_REQUEST).body(message).build());
   81 | 		}
   82 | 		Set<LoadBalancerLifecycle> supportedLifecycleProcessors = LoadBalancerLifecycleValidator
   83 | 			.getSupportedLifecycleProcessors(loadBalancerFactory.getInstances(serviceId, LoadBalancerLifecycle.class),
   84 | 					RequestDataContext.class, ResponseData.class, ServiceInstance.class);
   85 | 		String hint = getHint(serviceId, loadBalancerFactory.getProperties(serviceId).getHint());
   86 | 		RequestData requestData = new RequestData(clientRequest);
   87 | 		DefaultRequest<RequestDataContext> lbRequest = new DefaultRequest<>(new RequestDataContext(requestData, hint));
   88 | 		supportedLifecycleProcessors.forEach(lifecycle -> lifecycle.onStart(lbRequest));
   89 | 		return choose(serviceId, lbRequest).flatMap(lbResponse -> {
   90 | 			ServiceInstance instance = lbResponse.getServer();
   91 | 			if (instance == null) {
   92 | 				String message = serviceInstanceUnavailableMessage(serviceId);
   93 | 				if (LOG.isWarnEnabled()) {
   94 | 					LOG.warn(message);
   95 | 				}
   96 | 				supportedLifecycleProcessors.forEach(lifecycle -> lifecycle
   97 | 					.onComplete(new CompletionContext<>(CompletionContext.Status.DISCARD, lbRequest, lbResponse)));
   98 | 				return Mono.just(ClientResponse.create(HttpStatus.SERVICE_UNAVAILABLE)
   99 | 					.body(serviceInstanceUnavailableMessage(serviceId))
  100 | 					.build());
  101 | 			}
  102 | 
  103 | 			if (LOG.isDebugEnabled()) {
  104 | 				LOG.debug(String.format("LoadBalancer has retrieved the instance for service %s: %s", serviceId,
  105 | 						instance.getUri()));
  106 | 			}
  107 | 			LoadBalancerProperties.StickySession stickySessionProperties = loadBalancerFactory.getProperties(serviceId)
  108 | 				.getStickySession();
  109 | 			ClientRequest newRequest = buildClientRequest(clientRequest, instance,
  110 | 					stickySessionProperties.getInstanceIdCookieName(),
  111 | 					stickySessionProperties.isAddServiceInstanceCookie(), transformers);
  112 | 			supportedLifecycleProcessors.forEach(lifecycle -> lifecycle.onStartRequest(lbRequest, lbResponse));
  113 | 			return next.exchange(newRequest)
  114 | 				.doOnError(throwable -> supportedLifecycleProcessors.forEach(lifecycle -> lifecycle
  115 | 					.onComplete(new CompletionContext<ResponseData, ServiceInstance, RequestDataContext>(
  116 | 							CompletionContext.Status.FAILED, throwable, lbRequest, lbResponse))))
  117 | 				.doOnSuccess(clientResponse -> supportedLifecycleProcessors
  118 | 					.forEach(lifecycle -> lifecycle.onComplete(new CompletionContext<>(CompletionContext.Status.SUCCESS,
  119 | 							lbRequest, lbResponse, new ResponseData(clientResponse, requestData)))));
  120 | 		});
  121 | 	}
  122 | 
  123 | 	protected Mono<Response<ServiceInstance>> choose(String serviceId, Request<RequestDataContext> request) {
  124 | 		ReactiveLoadBalancer<ServiceInstance> loadBalancer = loadBalancerFactory.getInstance(serviceId);
  125 | 		if (loadBalancer == null) {
  126 | 			return Mono.just(new EmptyResponse());
  127 | 		}
  128 | 		return Mono.from(loadBalancer.choose(request));
  129 | 	}
  130 | 
  131 | }
```
