<!-- learning-docs source archive（机械抓取原文，禁止手改代码内容） -->
<!-- origin: org.springframework.security:spring-security-config:7.2.0 | path: org/springframework/security/config/annotation/web/configuration/WebSecurityConfiguration.java -->
<!-- 正文引用：[[源码:WebSecurityConfiguration.java#L起-L止]] ；代码块头部：(src: WebSecurityConfiguration.java#L起-L止) -->

# WebSecurityConfiguration.java

| 项 | 值 |
| --- | --- |
| 来源 | `org.springframework.security:spring-security-config:7.2.0` |
| 路径 | `org/springframework/security/config/annotation/web/configuration/WebSecurityConfiguration.java` |
| 行数 | 402 |
| 在线地址 | https://repo1.maven.org/maven2/org/springframework/security/spring-security-config/7.2.0/spring-security-config-7.2.0-sources.jar |
| 抓取时间 | 2026-11-25T16:27:52.544Z |

```java
    1 | /*
    2 |  * Copyright 2004-present the original author or authors.
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
   17 | package org.springframework.security.config.annotation.web.configuration;
   18 | 
   19 | import java.io.IOException;
   20 | import java.util.Collections;
   21 | import java.util.List;
   22 | import java.util.Map;
   23 | 
   24 | import jakarta.servlet.Filter;
   25 | import jakarta.servlet.FilterChain;
   26 | import jakarta.servlet.ServletException;
   27 | import jakarta.servlet.ServletRequest;
   28 | import jakarta.servlet.ServletResponse;
   29 | import jakarta.servlet.http.HttpServletRequest;
   30 | 
   31 | import org.springframework.beans.BeanMetadataElement;
   32 | import org.springframework.beans.BeansException;
   33 | import org.springframework.beans.factory.ObjectProvider;
   34 | import org.springframework.beans.factory.annotation.Autowired;
   35 | import org.springframework.beans.factory.config.BeanDefinition;
   36 | import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;
   37 | import org.springframework.beans.factory.support.BeanDefinitionBuilder;
   38 | import org.springframework.beans.factory.support.BeanDefinitionRegistry;
   39 | import org.springframework.beans.factory.support.BeanDefinitionRegistryPostProcessor;
   40 | import org.springframework.beans.factory.support.ManagedList;
   41 | import org.springframework.context.annotation.Bean;
   42 | import org.springframework.context.annotation.Configuration;
   43 | import org.springframework.context.annotation.DependsOn;
   44 | import org.springframework.context.annotation.ImportAware;
   45 | import org.springframework.core.OrderComparator;
   46 | import org.springframework.core.Ordered;
   47 | import org.springframework.core.annotation.AnnotationAttributes;
   48 | import org.springframework.core.annotation.AnnotationUtils;
   49 | import org.springframework.core.annotation.Order;
   50 | import org.springframework.core.type.AnnotationMetadata;
   51 | import org.springframework.security.access.expression.SecurityExpressionHandler;
   52 | import org.springframework.security.config.Customizer;
   53 | import org.springframework.security.config.ObjectPostProcessor;
   54 | import org.springframework.security.config.annotation.SecurityConfigurer;
   55 | import org.springframework.security.config.annotation.web.WebSecurityConfigurer;
   56 | import org.springframework.security.config.annotation.web.builders.HttpSecurity;
   57 | import org.springframework.security.config.annotation.web.builders.WebSecurity;
   58 | import org.springframework.security.config.crypto.RsaKeyConversionServicePostProcessor;
   59 | import org.springframework.security.context.DelegatingApplicationListener;
   60 | import org.springframework.security.core.context.SecurityContextHolderStrategy;
   61 | import org.springframework.security.web.FilterChainProxy;
   62 | import org.springframework.security.web.FilterInvocation;
   63 | import org.springframework.security.web.SecurityFilterChain;
   64 | import org.springframework.security.web.access.WebInvocationPrivilegeEvaluator;
   65 | import org.springframework.security.web.context.AbstractSecurityWebApplicationInitializer;
   66 | import org.springframework.security.web.debug.DebugFilter;
   67 | import org.springframework.security.web.firewall.HttpFirewall;
   68 | import org.springframework.security.web.firewall.RequestRejectedHandler;
   69 | import org.springframework.web.filter.CompositeFilter;
   70 | import org.springframework.web.filter.ServletRequestPathFilter;
   71 | 
   72 | /**
   73 |  * Uses a {@link WebSecurity} to create the {@link FilterChainProxy} that performs the web
   74 |  * based security for Spring Security. It then exports the necessary beans. Customizations
   75 |  * can be made to {@link WebSecurity} by implementing {@link WebSecurityConfigurer} and
   76 |  * exposing it as a {@link Configuration} or exposing a {@link WebSecurityCustomizer}
   77 |  * bean. This configuration is imported when using {@link EnableWebSecurity}.
   78 |  *
   79 |  * @author Rob Winch
   80 |  * @author Keesun Baik
   81 |  * @author Yanming Zhou
   82 |  * @since 3.2
   83 |  * @see EnableWebSecurity
   84 |  * @see WebSecurity
   85 |  */
   86 | @Configuration(proxyBeanMethods = false)
   87 | public class WebSecurityConfiguration implements ImportAware {
   88 | 
   89 | 	private WebSecurity webSecurity;
   90 | 
   91 | 	private Boolean debugEnabled;
   92 | 
   93 | 	private List<SecurityFilterChain> securityFilterChains = Collections.emptyList();
   94 | 
   95 | 	private List<WebSecurityCustomizer> webSecurityCustomizers = Collections.emptyList();
   96 | 
   97 | 	@Bean
   98 | 	public static DelegatingApplicationListener delegatingApplicationListener() {
   99 | 		return new DelegatingApplicationListener();
  100 | 	}
  101 | 
  102 | 	@Bean
  103 | 	@DependsOn(AbstractSecurityWebApplicationInitializer.DEFAULT_FILTER_NAME)
  104 | 	public SecurityExpressionHandler<FilterInvocation> webSecurityExpressionHandler() {
  105 | 		return this.webSecurity.getExpressionHandler();
  106 | 	}
  107 | 
  108 | 	/**
  109 | 	 * Creates the Spring Security Filter Chain.
  110 | 	 * @return the {@link Filter} that represents the security filter chain
  111 | 	 * @throws Exception
  112 | 	 */
  113 | 	@Bean(name = AbstractSecurityWebApplicationInitializer.DEFAULT_FILTER_NAME)
  114 | 	public Filter springSecurityFilterChain(ObjectProvider<HttpSecurity> provider) throws Exception {
  115 | 		boolean hasFilterChain = !this.securityFilterChains.isEmpty();
  116 | 		if (!hasFilterChain) {
  117 | 			this.webSecurity.addSecurityFilterChainBuilder(() -> {
  118 | 				HttpSecurity httpSecurity = provider.getObject();
  119 | 				httpSecurity.authorizeHttpRequests((authorize) -> authorize.anyRequest().authenticated());
  120 | 				httpSecurity.formLogin(Customizer.withDefaults());
  121 | 				httpSecurity.httpBasic(Customizer.withDefaults());
  122 | 				return httpSecurity.build();
  123 | 			});
  124 | 		}
  125 | 		for (SecurityFilterChain securityFilterChain : this.securityFilterChains) {
  126 | 			this.webSecurity.addSecurityFilterChainBuilder(() -> securityFilterChain);
  127 | 		}
  128 | 		for (WebSecurityCustomizer customizer : this.webSecurityCustomizers) {
  129 | 			customizer.customize(this.webSecurity);
  130 | 		}
  131 | 		return this.webSecurity.build();
  132 | 	}
  133 | 
  134 | 	/**
  135 | 	 * Creates the {@link WebInvocationPrivilegeEvaluator} that is necessary to evaluate
  136 | 	 * privileges for a given web URI.
  137 | 	 * @return the {@link WebInvocationPrivilegeEvaluator}
  138 | 	 */
  139 | 	@Bean
  140 | 	@DependsOn(AbstractSecurityWebApplicationInitializer.DEFAULT_FILTER_NAME)
  141 | 	public WebInvocationPrivilegeEvaluator privilegeEvaluator() {
  142 | 		return this.webSecurity.getPrivilegeEvaluator();
  143 | 	}
  144 | 
  145 | 	/**
  146 | 	 * Sets the {@code <SecurityConfigurer<FilterChainProxy, WebSecurityBuilder>}
  147 | 	 * instances used to create the web configuration.
  148 | 	 * @param objectPostProcessor the {@link ObjectPostProcessor} used to create a
  149 | 	 * {@link WebSecurity} instance
  150 | 	 * @param beanFactory the bean factory to use to retrieve the relevant
  151 | 	 * {@code <SecurityConfigurer<FilterChainProxy, WebSecurityBuilder>} instances used to
  152 | 	 * create the web configuration
  153 | 	 * @throws Exception
  154 | 	 */
  155 | 	@Autowired(required = false)
  156 | 	public void setFilterChainProxySecurityConfigurer(ObjectPostProcessor<Object> objectPostProcessor,
  157 | 			ConfigurableListableBeanFactory beanFactory) throws Exception {
  158 | 		this.webSecurity = objectPostProcessor.postProcess(new WebSecurity(objectPostProcessor));
  159 | 		if (this.debugEnabled != null) {
  160 | 			this.webSecurity.debug(this.debugEnabled);
  161 | 		}
  162 | 		List<SecurityConfigurer<Filter, WebSecurity>> webSecurityConfigurers = new AutowiredWebSecurityConfigurersIgnoreParents(
  163 | 				beanFactory)
  164 | 			.getWebSecurityConfigurers();
  165 | 		webSecurityConfigurers.sort(AnnotationAwareOrderComparator.INSTANCE);
  166 | 		Integer previousOrder = null;
  167 | 		Object previousConfig = null;
  168 | 		for (SecurityConfigurer<Filter, WebSecurity> config : webSecurityConfigurers) {
  169 | 			Integer order = AnnotationAwareOrderComparator.lookupOrder(config);
  170 | 			if (previousOrder != null && previousOrder.equals(order)) {
  171 | 				throw new IllegalStateException("@Order on WebSecurityConfigurers must be unique. Order of " + order
  172 | 						+ " was already used on " + previousConfig + ", so it cannot be used on " + config + " too.");
  173 | 			}
  174 | 			previousOrder = order;
  175 | 			previousConfig = config;
  176 | 		}
  177 | 		for (SecurityConfigurer<Filter, WebSecurity> webSecurityConfigurer : webSecurityConfigurers) {
  178 | 			this.webSecurity.apply(webSecurityConfigurer);
  179 | 		}
  180 | 	}
  181 | 
  182 | 	@Autowired(required = false)
  183 | 	void setFilterChains(List<SecurityFilterChain> securityFilterChains) {
  184 | 		this.securityFilterChains = securityFilterChains;
  185 | 	}
  186 | 
  187 | 	@Autowired(required = false)
  188 | 	void setWebSecurityCustomizers(List<WebSecurityCustomizer> webSecurityCustomizers) {
  189 | 		this.webSecurityCustomizers = webSecurityCustomizers;
  190 | 	}
  191 | 
  192 | 	@Bean
  193 | 	public static RsaKeyConversionServicePostProcessor conversionServicePostProcessor() {
  194 | 		return new RsaKeyConversionServicePostProcessor();
  195 | 	}
  196 | 
  197 | 	@Override
  198 | 	public void setImportMetadata(AnnotationMetadata importMetadata) {
  199 | 		Map<String, Object> enableWebSecurityAttrMap = importMetadata
  200 | 			.getAnnotationAttributes(EnableWebSecurity.class.getName());
  201 | 		AnnotationAttributes enableWebSecurityAttrs = AnnotationAttributes.fromMap(enableWebSecurityAttrMap);
  202 | 		this.debugEnabled = enableWebSecurityAttrs.getBoolean("debug");
  203 | 		if (this.webSecurity != null) {
  204 | 			this.webSecurity.debug(this.debugEnabled);
  205 | 		}
  206 | 	}
  207 | 
  208 | 	/**
  209 | 	 * Used to ensure Spring MVC request matching is cached.
  210 | 	 *
  211 | 	 * Creates a {@link BeanDefinitionRegistryPostProcessor} that moves the
  212 | 	 * AbstractSecurityWebApplicationInitializer.DEFAULT_FILTER_NAME to another bean name
  213 | 	 * and then adds a {@link CompositeFilter} that contains
  214 | 	 * {@link ServletRequestPathFilter} and the original FilterChainProxy under the
  215 | 	 * original Bean name.
  216 | 	 * @return
  217 | 	 */
  218 | 	@Bean
  219 | 	static BeanDefinitionRegistryPostProcessor springSecurityPathPatternParserBeanDefinitionRegistryPostProcessor() {
  220 | 		return new BeanDefinitionRegistryPostProcessor() {
  221 | 			@Override
  222 | 			public void postProcessBeanFactory(ConfigurableListableBeanFactory beanFactory) throws BeansException {
  223 | 			}
  224 | 
  225 | 			@Override
  226 | 			public void postProcessBeanDefinitionRegistry(BeanDefinitionRegistry registry) throws BeansException {
  227 | 				BeanDefinition filterChainProxy = registry
  228 | 					.getBeanDefinition(AbstractSecurityWebApplicationInitializer.DEFAULT_FILTER_NAME);
  229 | 
  230 | 				if (filterChainProxy.getResolvableType().isInstance(CompositeFilterChainProxy.class)) {
  231 | 					return;
  232 | 				}
  233 | 
  234 | 				BeanDefinitionBuilder pppCacheFilterBldr = BeanDefinitionBuilder
  235 | 					.rootBeanDefinition(ServletRequestPathFilter.class)
  236 | 					.setRole(BeanDefinition.ROLE_INFRASTRUCTURE);
  237 | 
  238 | 				ManagedList<BeanMetadataElement> filters = new ManagedList<>();
  239 | 				filters.add(pppCacheFilterBldr.getBeanDefinition());
  240 | 				filters.add(filterChainProxy);
  241 | 				BeanDefinitionBuilder compositeSpringSecurityFilterChainBldr = BeanDefinitionBuilder
  242 | 					.rootBeanDefinition(CompositeFilterChainProxy.class)
  243 | 					.addConstructorArgValue(filters);
  244 | 
  245 | 				registry.removeBeanDefinition(AbstractSecurityWebApplicationInitializer.DEFAULT_FILTER_NAME);
  246 | 				registry.registerBeanDefinition(AbstractSecurityWebApplicationInitializer.DEFAULT_FILTER_NAME,
  247 | 						compositeSpringSecurityFilterChainBldr.getBeanDefinition());
  248 | 			}
  249 | 		};
  250 | 	}
  251 | 
  252 | 	/**
  253 | 	 * A custom version of the Spring provided AnnotationAwareOrderComparator that uses
  254 | 	 * {@link AnnotationUtils#findAnnotation(Class, Class)} to look on super class
  255 | 	 * instances for the {@link Order} annotation.
  256 | 	 *
  257 | 	 * @author Rob Winch
  258 | 	 * @since 3.2
  259 | 	 */
  260 | 	private static class AnnotationAwareOrderComparator extends OrderComparator {
  261 | 
  262 | 		private static final AnnotationAwareOrderComparator INSTANCE = new AnnotationAwareOrderComparator();
  263 | 
  264 | 		@Override
  265 | 		protected int getOrder(Object obj) {
  266 | 			return lookupOrder(obj);
  267 | 		}
  268 | 
  269 | 		private static int lookupOrder(Object obj) {
  270 | 			if (obj instanceof Ordered) {
  271 | 				return ((Ordered) obj).getOrder();
  272 | 			}
  273 | 			if (obj != null) {
  274 | 				Class<?> clazz = ((obj instanceof Class) ? (Class<?>) obj : obj.getClass());
  275 | 				Order order = AnnotationUtils.findAnnotation(clazz, Order.class);
  276 | 				if (order != null) {
  277 | 					return order.value();
  278 | 				}
  279 | 			}
  280 | 			return Ordered.LOWEST_PRECEDENCE;
  281 | 		}
  282 | 
  283 | 	}
  284 | 
  285 | 	/**
  286 | 	 * Extends {@link FilterChainProxy} to provide as much passivity as possible but
  287 | 	 * delegates to {@link CompositeFilter} for
  288 | 	 * {@link #doFilter(ServletRequest, ServletResponse, FilterChain)}.
  289 | 	 */
  290 | 	static class CompositeFilterChainProxy extends FilterChainProxy {
  291 | 
  292 | 		/**
  293 | 		 * Used for {@link #doFilter(ServletRequest, ServletResponse, FilterChain)}.
  294 | 		 */
  295 | 		private final Filter doFilterDelegate;
  296 | 
  297 | 		private final FilterChainProxy springSecurityFilterChain;
  298 | 
  299 | 		/**
  300 | 		 * Creates a new instance.
  301 | 		 * @param filters the Filters to delegate to. One of which must be
  302 | 		 * FilterChainProxy.
  303 | 		 */
  304 | 		CompositeFilterChainProxy(List<? extends Filter> filters) {
  305 | 			this.doFilterDelegate = createDoFilterDelegate(filters);
  306 | 			this.springSecurityFilterChain = findFilterChainProxy(filters);
  307 | 		}
  308 | 
  309 | 		@Override
  310 | 		public void afterPropertiesSet() {
  311 | 			this.springSecurityFilterChain.afterPropertiesSet();
  312 | 		}
  313 | 
  314 | 		@Override
  315 | 		public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
  316 | 				throws IOException, ServletException {
  317 | 			this.doFilterDelegate.doFilter(request, response, chain);
  318 | 		}
  319 | 
  320 | 		@Override
  321 | 		public List<Filter> getFilters(String url) {
  322 | 			return this.springSecurityFilterChain.getFilters(url);
  323 | 		}
  324 | 
  325 | 		@Override
  326 | 		public List<SecurityFilterChain> getFilterChains() {
  327 | 			return this.springSecurityFilterChain.getFilterChains();
  328 | 		}
  329 | 
  330 | 		@Override
  331 | 		public void setSecurityContextHolderStrategy(SecurityContextHolderStrategy securityContextHolderStrategy) {
  332 | 			this.springSecurityFilterChain.setSecurityContextHolderStrategy(securityContextHolderStrategy);
  333 | 		}
  334 | 
  335 | 		@Override
  336 | 		public void setFilterChainValidator(FilterChainValidator filterChainValidator) {
  337 | 			this.springSecurityFilterChain.setFilterChainValidator(filterChainValidator);
  338 | 		}
  339 | 
  340 | 		@Override
  341 | 		public void setFilterChainDecorator(FilterChainDecorator filterChainDecorator) {
  342 | 			this.springSecurityFilterChain.setFilterChainDecorator(filterChainDecorator);
  343 | 		}
  344 | 
  345 | 		@Override
  346 | 		public void setFirewall(HttpFirewall firewall) {
  347 | 			this.springSecurityFilterChain.setFirewall(firewall);
  348 | 		}
  349 | 
  350 | 		@Override
  351 | 		public void setRequestRejectedHandler(RequestRejectedHandler requestRejectedHandler) {
  352 | 			this.springSecurityFilterChain.setRequestRejectedHandler(requestRejectedHandler);
  353 | 		}
  354 | 
  355 | 		/**
  356 | 		 * Used through reflection by Spring Security's Test support to lookup the
  357 | 		 * FilterChainProxy Filters for a specific HttpServletRequest.
  358 | 		 * @param request
  359 | 		 * @return
  360 | 		 */
  361 | 		private List<? extends Filter> getFilters(HttpServletRequest request) {
  362 | 			List<SecurityFilterChain> filterChains = this.springSecurityFilterChain.getFilterChains();
  363 | 			for (SecurityFilterChain chain : filterChains) {
  364 | 				if (chain.matches(request)) {
  365 | 					return chain.getFilters();
  366 | 				}
  367 | 			}
  368 | 			return null;
  369 | 		}
  370 | 
  371 | 		/**
  372 | 		 * Creates the Filter to delegate to for doFilter.
  373 | 		 * @param filters the Filters to delegate to.
  374 | 		 * @return the Filter for doFilter
  375 | 		 */
  376 | 		private static Filter createDoFilterDelegate(List<? extends Filter> filters) {
  377 | 			CompositeFilter delegate = new CompositeFilter();
  378 | 			delegate.setFilters(filters);
  379 | 			return delegate;
  380 | 		}
  381 | 
  382 | 		/**
  383 | 		 * Find the FilterChainProxy in a List of Filter.
  384 | 		 * @param filters
  385 | 		 * @return non-null FilterChainProxy
  386 | 		 * @throws IllegalStateException if the FilterChainProxy cannot be found
  387 | 		 */
  388 | 		private static FilterChainProxy findFilterChainProxy(List<? extends Filter> filters) {
  389 | 			for (Filter filter : filters) {
  390 | 				if (filter instanceof FilterChainProxy fcp) {
  391 | 					return fcp;
  392 | 				}
  393 | 				if (filter instanceof DebugFilter debugFilter) {
  394 | 					return debugFilter.getFilterChainProxy();
  395 | 				}
  396 | 			}
  397 | 			throw new IllegalStateException("Couldn't find FilterChainProxy in " + filters);
  398 | 		}
  399 | 
  400 | 	}
  401 | 
  402 | }
```
