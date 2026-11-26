<!-- learning-docs source archive（机械抓取原文，禁止手改代码内容） -->
<!-- origin: org.springframework.cloud:spring-cloud-gateway-server-webflux:5.1.0 | path: org/springframework/cloud/gateway/filter/ReactiveLoadBalancerClientFilter.java -->
<!-- 正文引用：[[源码:ReactiveLoadBalancerClientFilter.java#L起-L止]] ；代码块头部：(src: ReactiveLoadBalancerClientFilter.java#L起-L止) -->

# ReactiveLoadBalancerClientFilter.java

| 项 | 值 |
| --- | --- |
| 来源 | `org.springframework.cloud:spring-cloud-gateway-server-webflux:5.1.0` |
| 路径 | `org/springframework/cloud/gateway/filter/ReactiveLoadBalancerClientFilter.java` |
| 行数 | 177 |
| 在线地址 | https://repo1.maven.org/maven2/org/springframework/cloud/spring-cloud-gateway-server-webflux/5.1.0/spring-cloud-gateway-server-webflux-5.1.0-sources.jar |
| 抓取时间 | 2026-11-25T15:33:47.966Z |

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
   17 | package org.springframework.cloud.gateway.filter;
   18 | 
   19 | import java.net.URI;
   20 | import java.util.Map;
   21 | import java.util.Objects;
   22 | import java.util.Set;
   23 | 
   24 | import org.apache.commons.logging.Log;
   25 | import org.apache.commons.logging.LogFactory;
   26 | import reactor.core.publisher.Mono;
   27 | 
   28 | import org.springframework.cloud.client.ServiceInstance;
   29 | import org.springframework.cloud.client.loadbalancer.CompletionContext;
   30 | import org.springframework.cloud.client.loadbalancer.DefaultRequest;
   31 | import org.springframework.cloud.client.loadbalancer.LoadBalancerLifecycle;
   32 | import org.springframework.cloud.client.loadbalancer.LoadBalancerLifecycleValidator;
   33 | import org.springframework.cloud.client.loadbalancer.LoadBalancerProperties;
   34 | import org.springframework.cloud.client.loadbalancer.LoadBalancerUriTools;
   35 | import org.springframework.cloud.client.loadbalancer.Request;
   36 | import org.springframework.cloud.client.loadbalancer.RequestData;
   37 | import org.springframework.cloud.client.loadbalancer.RequestDataContext;
   38 | import org.springframework.cloud.client.loadbalancer.Response;
   39 | import org.springframework.cloud.client.loadbalancer.ResponseData;
   40 | import org.springframework.cloud.gateway.config.GatewayLoadBalancerProperties;
   41 | import org.springframework.cloud.gateway.support.DelegatingServiceInstance;
   42 | import org.springframework.cloud.gateway.support.NotFoundException;
   43 | import org.springframework.cloud.loadbalancer.core.ReactorLoadBalancer;
   44 | import org.springframework.cloud.loadbalancer.core.ReactorServiceInstanceLoadBalancer;
   45 | import org.springframework.cloud.loadbalancer.support.LoadBalancerClientFactory;
   46 | import org.springframework.core.Ordered;
   47 | import org.springframework.web.server.ServerWebExchange;
   48 | 
   49 | import static org.springframework.cloud.gateway.support.ServerWebExchangeUtils.GATEWAY_LOADBALANCER_RESPONSE_ATTR;
   50 | import static org.springframework.cloud.gateway.support.ServerWebExchangeUtils.GATEWAY_REQUEST_URL_ATTR;
   51 | import static org.springframework.cloud.gateway.support.ServerWebExchangeUtils.GATEWAY_SCHEME_PREFIX_ATTR;
   52 | import static org.springframework.cloud.gateway.support.ServerWebExchangeUtils.addOriginalRequestUrl;
   53 | 
   54 | /**
   55 |  * A {@link GlobalFilter} implementation that routes requests using reactive Spring Cloud
   56 |  * LoadBalancer.
   57 |  *
   58 |  * @author Spencer Gibb
   59 |  * @author Tim Ysewyn
   60 |  * @author Olga Maciaszek-Sharma
   61 |  */
   62 | @SuppressWarnings({ "rawtypes", "unchecked" })
   63 | public class ReactiveLoadBalancerClientFilter implements GlobalFilter, Ordered {
   64 | 
   65 | 	private static final Log log = LogFactory.getLog(ReactiveLoadBalancerClientFilter.class);
   66 | 
   67 | 	/**
   68 | 	 * Order of filter.
   69 | 	 */
   70 | 	public static final int LOAD_BALANCER_CLIENT_FILTER_ORDER = 10150;
   71 | 
   72 | 	private final LoadBalancerClientFactory clientFactory;
   73 | 
   74 | 	private final GatewayLoadBalancerProperties properties;
   75 | 
   76 | 	public ReactiveLoadBalancerClientFilter(LoadBalancerClientFactory clientFactory,
   77 | 			GatewayLoadBalancerProperties properties) {
   78 | 		this.clientFactory = clientFactory;
   79 | 		this.properties = properties;
   80 | 	}
   81 | 
   82 | 	@Override
   83 | 	public int getOrder() {
   84 | 		return LOAD_BALANCER_CLIENT_FILTER_ORDER;
   85 | 	}
   86 | 
   87 | 	@Override
   88 | 	public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
   89 | 		URI url = exchange.getAttribute(GATEWAY_REQUEST_URL_ATTR);
   90 | 		String schemePrefix = exchange.getAttribute(GATEWAY_SCHEME_PREFIX_ATTR);
   91 | 		if (url == null || (!"lb".equals(url.getScheme()) && !"lb".equals(schemePrefix))) {
   92 | 			return chain.filter(exchange);
   93 | 		}
   94 | 		// preserve the original url
   95 | 		addOriginalRequestUrl(exchange, url);
   96 | 
   97 | 		if (log.isTraceEnabled()) {
   98 | 			log.trace(ReactiveLoadBalancerClientFilter.class.getSimpleName() + " url before: " + url);
   99 | 		}
  100 | 
  101 | 		URI requestUri = exchange.getAttribute(GATEWAY_REQUEST_URL_ATTR);
  102 | 		Objects.requireNonNull(requestUri, "requestUri can not be null");
  103 | 		String serviceId = requestUri.getHost();
  104 | 		Set<LoadBalancerLifecycle> supportedLifecycleProcessors = LoadBalancerLifecycleValidator
  105 | 			.getSupportedLifecycleProcessors(clientFactory.getInstances(serviceId, LoadBalancerLifecycle.class),
  106 | 					RequestDataContext.class, ResponseData.class, ServiceInstance.class);
  107 | 		DefaultRequest<RequestDataContext> lbRequest = new DefaultRequest<>(new RequestDataContext(
  108 | 				new RequestData(exchange.getRequest(), exchange.getAttributes()), getHint(serviceId)));
  109 | 		return choose(lbRequest, serviceId, supportedLifecycleProcessors).doOnNext(response -> {
  110 | 
  111 | 			if (!response.hasServer()) {
  112 | 				supportedLifecycleProcessors.forEach(lifecycle -> lifecycle
  113 | 					.onComplete(new CompletionContext<>(CompletionContext.Status.DISCARD, lbRequest, response)));
  114 | 				throw NotFoundException.create(properties.isUse404(), "Unable to find instance for " + url.getHost());
  115 | 			}
  116 | 
  117 | 			ServiceInstance retrievedInstance = response.getServer();
  118 | 
  119 | 			URI uri = exchange.getRequest().getURI();
  120 | 
  121 | 			// if the `lb:<scheme>` mechanism was used, use `<scheme>` as the default,
  122 | 			// if the loadbalancer doesn't provide one.
  123 | 			Objects.requireNonNull(retrievedInstance, "retrievedInstance can not be null");
  124 | 			String overrideScheme = retrievedInstance.isSecure() ? "https" : "http";
  125 | 			if (schemePrefix != null) {
  126 | 				overrideScheme = url.getScheme();
  127 | 			}
  128 | 
  129 | 			DelegatingServiceInstance serviceInstance = new DelegatingServiceInstance(retrievedInstance,
  130 | 					overrideScheme);
  131 | 
  132 | 			URI requestUrl = reconstructURI(serviceInstance, uri);
  133 | 
  134 | 			if (log.isTraceEnabled()) {
  135 | 				log.trace("LoadBalancerClientFilter url chosen: " + requestUrl);
  136 | 			}
  137 | 			exchange.getAttributes().put(GATEWAY_REQUEST_URL_ATTR, requestUrl);
  138 | 			exchange.getAttributes().put(GATEWAY_LOADBALANCER_RESPONSE_ATTR, response);
  139 | 			supportedLifecycleProcessors.forEach(lifecycle -> lifecycle.onStartRequest(lbRequest, response));
  140 | 		})
  141 | 			.then(chain.filter(exchange))
  142 | 			.doOnError(throwable -> supportedLifecycleProcessors.forEach(lifecycle -> lifecycle
  143 | 				.onComplete(new CompletionContext<ResponseData, ServiceInstance, RequestDataContext>(
  144 | 						CompletionContext.Status.FAILED, throwable, lbRequest,
  145 | 						exchange.getAttribute(GATEWAY_LOADBALANCER_RESPONSE_ATTR)))))
  146 | 			.doOnSuccess(aVoid -> supportedLifecycleProcessors.forEach(lifecycle -> lifecycle
  147 | 				.onComplete(new CompletionContext<ResponseData, ServiceInstance, RequestDataContext>(
  148 | 						CompletionContext.Status.SUCCESS, lbRequest,
  149 | 						exchange.getAttribute(GATEWAY_LOADBALANCER_RESPONSE_ATTR),
  150 | 						new ResponseData(exchange.getResponse(),
  151 | 								new RequestData(exchange.getRequest(), exchange.getAttributes()))))));
  152 | 	}
  153 | 
  154 | 	protected URI reconstructURI(ServiceInstance serviceInstance, URI original) {
  155 | 		return LoadBalancerUriTools.reconstructURI(serviceInstance, original);
  156 | 	}
  157 | 
  158 | 	private Mono<Response<ServiceInstance>> choose(Request<RequestDataContext> lbRequest, String serviceId,
  159 | 			Set<LoadBalancerLifecycle> supportedLifecycleProcessors) {
  160 | 		ReactorLoadBalancer<ServiceInstance> loadBalancer = this.clientFactory.getInstance(serviceId,
  161 | 				ReactorServiceInstanceLoadBalancer.class);
  162 | 		if (loadBalancer == null) {
  163 | 			throw new NotFoundException("No loadbalancer available for " + serviceId);
  164 | 		}
  165 | 		supportedLifecycleProcessors.forEach(lifecycle -> lifecycle.onStart(lbRequest));
  166 | 		return loadBalancer.choose(lbRequest);
  167 | 	}
  168 | 
  169 | 	private String getHint(String serviceId) {
  170 | 		LoadBalancerProperties loadBalancerProperties = clientFactory.getProperties(serviceId);
  171 | 		Map<String, String> hints = loadBalancerProperties.getHint();
  172 | 		String defaultHint = hints.getOrDefault("default", "default");
  173 | 		String hintPropertyValue = hints.get(serviceId);
  174 | 		return hintPropertyValue != null ? hintPropertyValue : defaultHint;
  175 | 	}
  176 | 
  177 | }
```
