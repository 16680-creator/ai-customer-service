<!-- learning-docs source archive（机械抓取原文，禁止手改代码内容） -->
<!-- origin: org.springframework.cloud:spring-cloud-commons:5.1.0 | path: org/springframework/cloud/client/circuitbreaker/httpservice/CircuitBreakerAdapterDecorator.java -->
<!-- 正文引用：[[源码:CircuitBreakerAdapterDecorator.java#L起-L止]] ；代码块头部：(src: CircuitBreakerAdapterDecorator.java#L起-L止) -->

# CircuitBreakerAdapterDecorator.java

| 项 | 值 |
| --- | --- |
| 来源 | `org.springframework.cloud:spring-cloud-commons:5.1.0` |
| 路径 | `org/springframework/cloud/client/circuitbreaker/httpservice/CircuitBreakerAdapterDecorator.java` |
| 行数 | 148 |
| 在线地址 | https://repo1.maven.org/maven2/org/springframework/cloud/spring-cloud-commons/5.1.0/spring-cloud-commons-5.1.0-sources.jar |
| 抓取时间 | 2026-11-25T15:33:21.716Z |

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
   17 | package org.springframework.cloud.client.circuitbreaker.httpservice;
   18 | 
   19 | import java.util.Map;
   20 | import java.util.function.Function;
   21 | 
   22 | import org.apache.commons.logging.Log;
   23 | import org.apache.commons.logging.LogFactory;
   24 | import org.jspecify.annotations.Nullable;
   25 | 
   26 | import org.springframework.cloud.client.circuitbreaker.CircuitBreaker;
   27 | import org.springframework.core.ParameterizedTypeReference;
   28 | import org.springframework.http.HttpHeaders;
   29 | import org.springframework.http.ResponseEntity;
   30 | import org.springframework.web.service.invoker.HttpExchangeAdapter;
   31 | import org.springframework.web.service.invoker.HttpExchangeAdapterDecorator;
   32 | import org.springframework.web.service.invoker.HttpRequestValues;
   33 | 
   34 | import static org.springframework.cloud.client.circuitbreaker.httpservice.CircuitBreakerConfigurerUtils.createProxies;
   35 | import static org.springframework.cloud.client.circuitbreaker.httpservice.CircuitBreakerConfigurerUtils.getFallback;
   36 | 
   37 | /**
   38 |  * Blocking implementation of {@link HttpExchangeAdapterDecorator} that wraps
   39 |  * {@code @HttpExchange}
   40 |  * <p>
   41 |  * In the event of a CircuitBreaker fallback, this class uses the user-provided fallback
   42 |  * class to create a proxy. The fallback method is selected by matching either:
   43 |  * <ul>
   44 |  * <li>A method with the same name and argument types as the original method, or</li>
   45 |  * <li>A method with the same name and the original arguments preceded by a
   46 |  * {@link Throwable}, allowing the user to access the cause of failure within the
   47 |  * fallback.</li>
   48 |  * </ul>
   49 |  * Once a matching method is found, it is invoked to provide the fallback behavior. Both
   50 |  * the fallback class and the fallback methods must be public.
   51 |  * </p>
   52 |  *
   53 |  * @author Olga Maciaszek-Sharma
   54 |  * @since 5.0.0
   55 |  * @see HttpServiceFallback
   56 |  */
   57 | public class CircuitBreakerAdapterDecorator extends HttpExchangeAdapterDecorator {
   58 | 
   59 | 	private static final Log LOG = LogFactory.getLog(CircuitBreakerAdapterDecorator.class);
   60 | 
   61 | 	private final CircuitBreaker circuitBreaker;
   62 | 
   63 | 	private final Map<String, Class<?>> fallbackClasses;
   64 | 
   65 | 	private volatile Map<String, Object> fallbackProxies;
   66 | 
   67 | 	public CircuitBreakerAdapterDecorator(HttpExchangeAdapter delegate, CircuitBreaker circuitBreaker,
   68 | 			Map<String, Class<?>> fallbackClasses) {
   69 | 		super(delegate);
   70 | 		this.circuitBreaker = circuitBreaker;
   71 | 		this.fallbackClasses = fallbackClasses;
   72 | 	}
   73 | 
   74 | 	@Override
   75 | 	public void exchange(HttpRequestValues requestValues) {
   76 | 		circuitBreaker.run(() -> {
   77 | 			super.exchange(requestValues);
   78 | 			return null;
   79 | 		}, createFallbackHandler(requestValues));
   80 | 	}
   81 | 
   82 | 	@Override
   83 | 	public HttpHeaders exchangeForHeaders(HttpRequestValues values) {
   84 | 		Object result = circuitBreaker.run(() -> super.exchangeForHeaders(values), createFallbackHandler(values));
   85 | 		return castIfPossible(result);
   86 | 	}
   87 | 
   88 | 	@Override
   89 | 	public <T> @Nullable T exchangeForBody(HttpRequestValues values, ParameterizedTypeReference<T> bodyType) {
   90 | 		Object result = circuitBreaker.run(() -> super.exchangeForBody(values, bodyType),
   91 | 				createFallbackHandler(values));
   92 | 		return castIfPossible(result);
   93 | 	}
   94 | 
   95 | 	@Override
   96 | 	public ResponseEntity<Void> exchangeForBodilessEntity(HttpRequestValues values) {
   97 | 		Object result = circuitBreaker.run(() -> super.exchangeForBodilessEntity(values),
   98 | 				createFallbackHandler(values));
   99 | 		return castIfPossible(result);
  100 | 	}
  101 | 
  102 | 	@Override
  103 | 	public <T> ResponseEntity<T> exchangeForEntity(HttpRequestValues values, ParameterizedTypeReference<T> bodyType) {
  104 | 		Object result = circuitBreaker.run(() -> super.exchangeForEntity(values, bodyType),
  105 | 				createFallbackHandler(values));
  106 | 		return castIfPossible(result);
  107 | 	}
  108 | 
  109 | 	// Visible for tests
  110 | 	CircuitBreaker getCircuitBreaker() {
  111 | 		return circuitBreaker;
  112 | 	}
  113 | 
  114 | 	// Visible for tests
  115 | 	Map<String, Class<?>> getFallbackClasses() {
  116 | 		return fallbackClasses;
  117 | 	}
  118 | 
  119 | 	@SuppressWarnings("unchecked")
  120 | 	private <T> T castIfPossible(Object result) {
  121 | 		try {
  122 | 			return (T) result;
  123 | 		}
  124 | 		catch (ClassCastException exception) {
  125 | 			if (LOG.isErrorEnabled()) {
  126 | 				LOG.error("Failed to cast object of type " + result.getClass() + " to expected type.");
  127 | 			}
  128 | 			throw exception;
  129 | 		}
  130 | 	}
  131 | 
  132 | 	// Visible for tests
  133 | 	Function<Throwable, Object> createFallbackHandler(HttpRequestValues requestValues) {
  134 | 		return throwable -> getFallback(requestValues, throwable, getFallbackProxies(), fallbackClasses);
  135 | 	}
  136 | 
  137 | 	private Map<String, Object> getFallbackProxies() {
  138 | 		if (fallbackProxies == null) {
  139 | 			synchronized (this) {
  140 | 				if (fallbackProxies == null) {
  141 | 					fallbackProxies = createProxies(fallbackClasses);
  142 | 				}
  143 | 			}
  144 | 		}
  145 | 		return fallbackProxies;
  146 | 	}
  147 | 
  148 | }
```
