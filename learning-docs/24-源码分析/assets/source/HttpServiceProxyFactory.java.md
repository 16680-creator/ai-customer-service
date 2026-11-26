<!-- learning-docs source archive（机械抓取原文，禁止手改代码内容） -->
<!-- origin: org.springframework:spring-web:7.1.0 | path: org/springframework/web/service/invoker/HttpServiceProxyFactory.java -->
<!-- 正文引用：[[源码:HttpServiceProxyFactory.java#L起-L止]] ；代码块头部：(src: HttpServiceProxyFactory.java#L起-L止) -->

# HttpServiceProxyFactory.java

| 项 | 值 |
| --- | --- |
| 来源 | `org.springframework:spring-web:7.1.0` |
| 路径 | `org/springframework/web/service/invoker/HttpServiceProxyFactory.java` |
| 行数 | 355 |
| 在线地址 | https://repo1.maven.org/maven2/org/springframework/spring-web/7.1.0/spring-web-7.1.0-sources.jar |
| 抓取时间 | 2026-11-25T13:56:37.908Z |

```java
    1 | /*
    2 |  * Copyright 2002-present the original author or authors.
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
   17 | package org.springframework.web.service.invoker;
   18 | 
   19 | import java.lang.reflect.InvocationHandler;
   20 | import java.lang.reflect.Method;
   21 | import java.util.ArrayList;
   22 | import java.util.List;
   23 | import java.util.Map;
   24 | import java.util.function.Function;
   25 | import java.util.stream.Collectors;
   26 | 
   27 | import org.aopalliance.intercept.MethodInterceptor;
   28 | import org.aopalliance.intercept.MethodInvocation;
   29 | import org.jspecify.annotations.Nullable;
   30 | 
   31 | import org.springframework.aop.framework.ProxyFactory;
   32 | import org.springframework.aop.framework.ReflectiveMethodInvocation;
   33 | import org.springframework.core.KotlinDetector;
   34 | import org.springframework.core.MethodIntrospector;
   35 | import org.springframework.core.MethodParameter;
   36 | import org.springframework.core.annotation.AnnotatedElementUtils;
   37 | import org.springframework.core.convert.ConversionService;
   38 | import org.springframework.format.support.DefaultFormattingConversionService;
   39 | import org.springframework.util.Assert;
   40 | import org.springframework.util.StringValueResolver;
   41 | import org.springframework.web.service.annotation.HttpExchange;
   42 | 
   43 | /**
   44 |  * Factory to create a client proxy from an HTTP service interface with
   45 |  * {@link HttpExchange @HttpExchange} methods.
   46 |  *
   47 |  * <p>To create an instance, use static methods to obtain a
   48 |  * {@link Builder Builder}.
   49 |  *
   50 |  * @author Rossen Stoyanchev
   51 |  * @since 6.0
   52 |  * @see org.springframework.web.client.support.RestClientAdapter
   53 |  * @see org.springframework.web.reactive.function.client.support.WebClientAdapter
   54 |  * @see org.springframework.web.client.support.RestTemplateAdapter
   55 |  */
   56 | public final class HttpServiceProxyFactory {
   57 | 
   58 | 	private final HttpExchangeAdapter exchangeAdapter;
   59 | 
   60 | 	private final List<HttpServiceArgumentResolver> argumentResolvers;
   61 | 
   62 | 	private final HttpRequestValues.Processor requestValuesProcessor;
   63 | 
   64 | 	private final @Nullable StringValueResolver embeddedValueResolver;
   65 | 
   66 | 	private final List<ProxyFactoryCustomizer> proxyFactoryCustomizers;
   67 | 
   68 | 
   69 | 	private HttpServiceProxyFactory(
   70 | 			HttpExchangeAdapter exchangeAdapter, List<HttpServiceArgumentResolver> argumentResolvers,
   71 | 			List<HttpRequestValues.Processor> requestValuesProcessors,
   72 | 			@Nullable StringValueResolver embeddedValueResolver,
   73 | 			List<ProxyFactoryCustomizer> proxyFactoryCustomizers) {
   74 | 
   75 | 		this.exchangeAdapter = exchangeAdapter;
   76 | 		this.argumentResolvers = argumentResolvers;
   77 | 		// Defensive copies, since the Builder's lists may be modified after build().
   78 | 		this.requestValuesProcessor =
   79 | 				new CompositeHttpRequestValuesProcessor(List.copyOf(requestValuesProcessors));
   80 | 		this.embeddedValueResolver = embeddedValueResolver;
   81 | 		this.proxyFactoryCustomizers = List.copyOf(proxyFactoryCustomizers);
   82 | 	}
   83 | 
   84 | 
   85 | 	/**
   86 | 	 * Return a proxy that implements the given HTTP service interface to perform
   87 | 	 * HTTP requests and retrieve responses through an HTTP client.
   88 | 	 * @param serviceType the HTTP service to create a proxy for
   89 | 	 * @param <S> the HTTP service type
   90 | 	 * @return the created proxy
   91 | 	 */
   92 | 	public <S> S createClient(Class<S> serviceType) {
   93 | 		List<HttpServiceMethod> httpServiceMethods =
   94 | 				MethodIntrospector.selectMethods(serviceType, this::isExchangeMethod).stream()
   95 | 						.map(method -> createHttpServiceMethod(serviceType, method))
   96 | 						.toList();
   97 | 
   98 | 		return getProxy(serviceType, httpServiceMethods);
   99 | 	}
  100 | 
  101 | 	@SuppressWarnings("unchecked")
  102 | 	private <S> S getProxy(Class<S> serviceType, List<HttpServiceMethod> httpServiceMethods) {
  103 | 		MethodInterceptor interceptor = new HttpServiceMethodInterceptor(httpServiceMethods);
  104 | 		ProxyFactory factory = new ProxyFactory(serviceType, interceptor);
  105 | 		for (ProxyFactoryCustomizer customizer : this.proxyFactoryCustomizers) {
  106 | 			customizer.customize(factory, serviceType);
  107 | 		}
  108 | 		return (S) factory.getProxy(serviceType.getClassLoader());
  109 | 	}
  110 | 
  111 | 	private boolean isExchangeMethod(Method method) {
  112 | 		return AnnotatedElementUtils.hasAnnotation(method, HttpExchange.class);
  113 | 	}
  114 | 
  115 | 	private <S> HttpServiceMethod createHttpServiceMethod(Class<S> serviceType, Method method) {
  116 | 		Assert.notNull(this.argumentResolvers,
  117 | 				"No argument resolvers: afterPropertiesSet was not called");
  118 | 
  119 | 		return new HttpServiceMethod(
  120 | 				method, serviceType, this.argumentResolvers, this.requestValuesProcessor,
  121 | 				this.exchangeAdapter, this.embeddedValueResolver);
  122 | 	}
  123 | 
  124 | 
  125 | 	/**
  126 | 	 * Return a builder that's initialized with the given client.
  127 | 	 * @since 6.1
  128 | 	 */
  129 | 	public static Builder builderFor(HttpExchangeAdapter exchangeAdapter) {
  130 | 		return new Builder().exchangeAdapter(exchangeAdapter);
  131 | 	}
  132 | 
  133 | 	/**
  134 | 	 * Return an empty builder, with the client to be provided to builder.
  135 | 	 */
  136 | 	public static Builder builder() {
  137 | 		return new Builder();
  138 | 	}
  139 | 
  140 | 
  141 | 	/**
  142 | 	 * Builder to create an {@link HttpServiceProxyFactory}.
  143 | 	 */
  144 | 	public static final class Builder {
  145 | 
  146 | 		private @Nullable HttpExchangeAdapter exchangeAdapter;
  147 | 
  148 | 		private Function<HttpExchangeAdapter, HttpExchangeAdapter> exchangeAdapterDecorator = Function.identity();
  149 | 
  150 | 		private final List<HttpServiceArgumentResolver> customArgumentResolvers = new ArrayList<>();
  151 | 
  152 | 		private @Nullable ConversionService conversionService;
  153 | 
  154 | 		private final List<HttpRequestValues.Processor> requestValuesProcessors = new ArrayList<>();
  155 | 
  156 | 		private @Nullable StringValueResolver embeddedValueResolver;
  157 | 
  158 | 		private final List<ProxyFactoryCustomizer> proxyFactoryCustomizers = new ArrayList<>();
  159 | 
  160 | 		private Builder() {
  161 | 		}
  162 | 
  163 | 		/**
  164 | 		 * Provide the HTTP client to perform requests through.
  165 | 		 * @param adapter a client adapted to {@link HttpExchangeAdapter}
  166 | 		 * @return this same builder instance
  167 | 		 * @since 6.1
  168 | 		 */
  169 | 		public Builder exchangeAdapter(HttpExchangeAdapter adapter) {
  170 | 			this.exchangeAdapter = adapter;
  171 | 			return this;
  172 | 		}
  173 | 
  174 | 		/**
  175 | 		 * Provide a function to wrap the configured {@code HttpExchangeAdapter}.
  176 | 		 * @param decorator a client adapted to {@link HttpExchangeAdapter}
  177 | 		 * @return this same builder instance
  178 | 		 * @since 7.0
  179 | 		 */
  180 | 		public Builder exchangeAdapterDecorator(Function<HttpExchangeAdapter, HttpExchangeAdapter> decorator) {
  181 | 			this.exchangeAdapterDecorator = this.exchangeAdapterDecorator.andThen(decorator);
  182 | 			return this;
  183 | 		}
  184 | 
  185 | 		/**
  186 | 		 * Register a custom argument resolver, invoked ahead of default resolvers.
  187 | 		 * @param resolver the resolver to add
  188 | 		 * @return this same builder instance
  189 | 		 */
  190 | 		public Builder customArgumentResolver(HttpServiceArgumentResolver resolver) {
  191 | 			this.customArgumentResolvers.add(resolver);
  192 | 			return this;
  193 | 		}
  194 | 
  195 | 		/**
  196 | 		 * Set the {@link ConversionService} to use where input values need to
  197 | 		 * be formatted as Strings.
  198 | 		 * <p>By default, this is {@link DefaultFormattingConversionService}.
  199 | 		 * @return this same builder instance
  200 | 		 */
  201 | 		public Builder conversionService(ConversionService conversionService) {
  202 | 			this.conversionService = conversionService;
  203 | 			return this;
  204 | 		}
  205 | 
  206 | 		/**
  207 | 		 * Register an {@link HttpRequestValues} processor that can further
  208 | 		 * customize request values based on the method and all arguments.
  209 | 		 * @param processor the processor to add
  210 | 		 * @return this same builder instance
  211 | 		 * @since 7.0
  212 | 		 */
  213 | 		public Builder httpRequestValuesProcessor(HttpRequestValues.Processor processor) {
  214 | 			this.requestValuesProcessors.add(processor);
  215 | 			return this;
  216 | 		}
  217 | 
  218 | 		/**
  219 | 		 * Set the {@link StringValueResolver} to use for resolving placeholders
  220 | 		 * and expressions embedded in {@link HttpExchange#url()}.
  221 | 		 * @param embeddedValueResolver the resolver to use
  222 | 		 * @return this same builder instance
  223 | 		 */
  224 | 		public Builder embeddedValueResolver(StringValueResolver embeddedValueResolver) {
  225 | 			this.embeddedValueResolver = embeddedValueResolver;
  226 | 			return this;
  227 | 		}
  228 | 
  229 | 		/**
  230 | 		 * Register a callback to customize the {@link ProxyFactory} used to
  231 | 		 * create the proxy for each HTTP service interface.
  232 | 		 * @param customizer the customizer to add
  233 | 		 * @return this same builder instance
  234 | 		 * @since 7.0.4
  235 | 		 */
  236 | 		public Builder proxyFactoryCustomizer(ProxyFactoryCustomizer customizer) {
  237 | 			this.proxyFactoryCustomizers.add(customizer);
  238 | 			return this;
  239 | 		}
  240 | 
  241 | 		/**
  242 | 		 * Build the {@link HttpServiceProxyFactory} instance.
  243 | 		 */
  244 | 		public HttpServiceProxyFactory build() {
  245 | 			Assert.notNull(this.exchangeAdapter, "HttpClientAdapter is required");
  246 | 			HttpExchangeAdapter adapterToUse = this.exchangeAdapterDecorator.apply(this.exchangeAdapter);
  247 | 
  248 | 			return new HttpServiceProxyFactory(
  249 | 					adapterToUse, initArgumentResolvers(), this.requestValuesProcessors,
  250 | 					this.embeddedValueResolver, this.proxyFactoryCustomizers);
  251 | 		}
  252 | 
  253 | 		@SuppressWarnings({"DataFlowIssue", "NullAway"})
  254 | 		private List<HttpServiceArgumentResolver> initArgumentResolvers() {
  255 | 
  256 | 			// Custom
  257 | 			List<HttpServiceArgumentResolver> resolvers = new ArrayList<>(this.customArgumentResolvers);
  258 | 
  259 | 			ConversionService service = (this.conversionService != null ?
  260 | 					this.conversionService : new DefaultFormattingConversionService());
  261 | 
  262 | 			// Annotation-based
  263 | 			resolvers.add(new RequestHeaderArgumentResolver(service));
  264 | 			resolvers.add(new RequestBodyArgumentResolver(this.exchangeAdapter));
  265 | 			resolvers.add(new PathVariableArgumentResolver(service));
  266 | 			resolvers.add(new RequestParamArgumentResolver(service));
  267 | 			resolvers.add(new RequestPartArgumentResolver(this.exchangeAdapter));
  268 | 			resolvers.add(new CookieValueArgumentResolver(service));
  269 | 			if (this.exchangeAdapter.supportsRequestAttributes()) {
  270 | 				resolvers.add(new RequestAttributeArgumentResolver());
  271 | 			}
  272 | 
  273 | 			// Specific type
  274 | 			resolvers.add(new UrlArgumentResolver());
  275 | 			resolvers.add(new UriBuilderFactoryArgumentResolver());
  276 | 			resolvers.add(new HttpMethodArgumentResolver());
  277 | 
  278 | 			return resolvers;
  279 | 		}
  280 | 	}
  281 | 
  282 | 
  283 | 	/**
  284 | 	 * {@link MethodInterceptor} that invokes an {@link HttpServiceMethod}.
  285 | 	 */
  286 | 	private static final class HttpServiceMethodInterceptor implements MethodInterceptor {
  287 | 
  288 | 		private final Map<Method, HttpServiceMethod> httpServiceMethods;
  289 | 
  290 | 		private HttpServiceMethodInterceptor(List<HttpServiceMethod> methods) {
  291 | 			this.httpServiceMethods = methods.stream()
  292 | 					.collect(Collectors.toMap(HttpServiceMethod::getMethod, Function.identity()));
  293 | 		}
  294 | 
  295 | 		@Override
  296 | 		public @Nullable Object invoke(MethodInvocation invocation) throws Throwable {
  297 | 			Method method = invocation.getMethod();
  298 | 			HttpServiceMethod httpServiceMethod = this.httpServiceMethods.get(method);
  299 | 			if (httpServiceMethod != null) {
  300 | 				@Nullable Object[] arguments = KotlinDetector.isSuspendingFunction(method) ?
  301 | 						resolveCoroutinesArguments(invocation.getArguments()) : invocation.getArguments();
  302 | 				return httpServiceMethod.invoke(arguments);
  303 | 			}
  304 | 			if (method.isDefault()) {
  305 | 				if (invocation instanceof ReflectiveMethodInvocation reflectiveMethodInvocation) {
  306 | 					Object proxy = reflectiveMethodInvocation.getProxy();
  307 | 					return InvocationHandler.invokeDefault(proxy, method, invocation.getArguments());
  308 | 				}
  309 | 			}
  310 | 			throw new IllegalStateException("Unexpected method invocation: " + method);
  311 | 		}
  312 | 
  313 | 		private static Object[] resolveCoroutinesArguments(@Nullable Object[] args) {
  314 | 			Object[] functionArgs = new Object[args.length - 1];
  315 | 			System.arraycopy(args, 0, functionArgs, 0, args.length - 1);
  316 | 			return functionArgs;
  317 | 		}
  318 | 	}
  319 | 
  320 | 
  321 | 	/**
  322 | 	 * Processor that delegates to a list of other processors.
  323 | 	 */
  324 | 	private record CompositeHttpRequestValuesProcessor(List<HttpRequestValues.Processor> processors)
  325 | 			implements HttpRequestValues.Processor {
  326 | 
  327 | 		@Override
  328 | 		public void process(
  329 | 				Method method, MethodParameter[] parameters, @Nullable Object[] arguments,
  330 | 				HttpRequestValues.Builder builder) {
  331 | 
  332 | 			for (HttpRequestValues.Processor processor : this.processors) {
  333 | 				processor.process(method, parameters, arguments, builder);
  334 | 			}
  335 | 		}
  336 | 	}
  337 | 
  338 | 	/**
  339 | 	 * Callback mechanism to customize the {@link ProxyFactory} used to create the
  340 | 	 * proxy for each HTTP service interface &mdash; for example, to add AOP interceptors.
  341 | 	 * @author Dung Dang Minh
  342 | 	 * @since 7.0.4
  343 | 	 */
  344 | 	public interface ProxyFactoryCustomizer {
  345 | 
  346 | 		/**
  347 | 		 * Customize the {@link ProxyFactory} for the given HTTP service type.
  348 | 		 * @param proxyFactory the proxy factory to customize
  349 | 		 * @param serviceType the HTTP service interface type
  350 | 		 */
  351 | 		void customize(ProxyFactory proxyFactory, Class<?> serviceType);
  352 | 
  353 | 	}
  354 | 
  355 | }
```
