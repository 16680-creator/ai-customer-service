<!-- learning-docs source archive（机械抓取原文，禁止手改代码内容） -->
<!-- origin: org.springframework.boot:spring-boot-actuator:4.2.0 | path: org/springframework/boot/actuate/endpoint/annotation/EndpointDiscoverer.java -->
<!-- 正文引用：[[源码:EndpointDiscoverer.java#L起-L止]] ；代码块头部：(src: EndpointDiscoverer.java#L起-L止) -->

# EndpointDiscoverer.java

| 项 | 值 |
| --- | --- |
| 来源 | `org.springframework.boot:spring-boot-actuator:4.2.0` |
| 路径 | `org/springframework/boot/actuate/endpoint/annotation/EndpointDiscoverer.java` |
| 行数 | 576 |
| 在线地址 | https://repo1.maven.org/maven2/org/springframework/boot/spring-boot-actuator/4.2.0/spring-boot-actuator-4.2.0-sources.jar |
| 抓取时间 | 2026-11-25T14:05:01.098Z |

```java
    1 | /*
    2 |  * Copyright 2012-present the original author or authors.
    3 |  */
    4 | 
    5 | package org.springframework.boot.actuate.endpoint.annotation;
    6 | 
    7 | import java.util.Collection;
    8 | import java.util.Collections;
    9 | import java.util.HashSet;
   10 | import java.util.LinkedHashMap;
   11 | import java.util.LinkedHashSet;
   12 | import java.util.List;
   13 | import java.util.Map;
   14 | import java.util.Objects;
   15 | import java.util.Set;
   16 | import java.util.concurrent.ConcurrentHashMap;
   17 | import java.util.function.Function;
   18 | import java.util.function.Supplier;
   19 | import java.util.stream.Collectors;
   20 | 
   21 | import org.jspecify.annotations.Nullable;
   22 | 
   23 | import org.springframework.aop.scope.ScopedProxyUtils;
   24 | import org.springframework.beans.BeanUtils;
   25 | import org.springframework.beans.factory.BeanFactoryUtils;
   26 | import org.springframework.boot.actuate.endpoint.Access;
   27 | import org.springframework.boot.actuate.endpoint.EndpointFilter;
   28 | import org.springframework.boot.actuate.endpoint.EndpointId;
   29 | import org.springframework.boot.actuate.endpoint.EndpointsSupplier;
   30 | import org.springframework.boot.actuate.endpoint.ExposableEndpoint;
   31 | import org.springframework.boot.actuate.endpoint.Operation;
   32 | import org.springframework.boot.actuate.endpoint.OperationFilter;
   33 | import org.springframework.boot.actuate.endpoint.invoke.OperationInvoker;
   34 | import org.springframework.boot.actuate.endpoint.invoke.OperationInvokerAdvisor;
   35 | import org.springframework.boot.actuate.endpoint.invoke.ParameterValueMapper;
   36 | import org.springframework.boot.util.LambdaSafe;
   37 | import org.springframework.context.ApplicationContext;
   38 | import org.springframework.core.ResolvableType;
   39 | import org.springframework.core.annotation.MergedAnnotation;
   40 | import org.springframework.core.annotation.MergedAnnotations;
   41 | import org.springframework.core.annotation.MergedAnnotations.SearchStrategy;
   42 | import org.springframework.core.env.Environment;
   43 | import org.springframework.util.Assert;
   44 | import org.springframework.util.ClassUtils;
   45 | import org.springframework.util.CollectionUtils;
   46 | import org.springframework.util.LinkedMultiValueMap;
   47 | import org.springframework.util.MultiValueMap;
   48 | import org.springframework.util.StringUtils;
   49 | 
   50 | /**
   51 |  * A Base for {@link EndpointsSupplier} implementations that discover
   52 |  * {@link Endpoint @Endpoint} beans and {@link EndpointExtension @EndpointExtension} beans
   53 |  * in an application context.
   54 |  *
   55 |  * @param <E> the endpoint type
   56 |  * @param <O> the operation type
   57 |  * @author Andy Wilkinson
   58 |  * @author Stephane Nicoll
   59 |  * @author Phillip Webb
   60 |  * @since 2.0.0
   61 |  */
   62 | public abstract class EndpointDiscoverer<E extends ExposableEndpoint<O>, O extends Operation>
   63 | 		implements EndpointsSupplier<E> {
   64 | 
   65 | 	private final ApplicationContext applicationContext;
   66 | 
   67 | 	private final Collection<EndpointFilter<E>> endpointFilters;
   68 | 
   69 | 	private final Collection<OperationFilter<O>> operationFilters;
   70 | 
   71 | 	private final DiscoveredOperationsFactory<O> operationsFactory;
   72 | 
   73 | 	private final Map<EndpointBean, E> filterEndpoints = new ConcurrentHashMap<>();
   74 | 
   75 | 	private volatile @Nullable Collection<E> endpoints;
   76 | 
   77 | 	/**
   78 | 	 * Create a new {@link EndpointDiscoverer} instance.
   79 | 	 * @param applicationContext the source application context
   80 | 	 * @param parameterValueMapper the parameter value mapper
   81 | 	 * @param invokerAdvisors invoker advisors to apply
   82 | 	 * @param endpointFilters endpoint filters to apply
   83 | 	 * @param operationFilters operation filters to apply
   84 | 	 * @since 3.4.0
   85 | 	 */
   86 | 	public EndpointDiscoverer(ApplicationContext applicationContext, ParameterValueMapper parameterValueMapper,
   87 | 			Collection<OperationInvokerAdvisor> invokerAdvisors, Collection<EndpointFilter<E>> endpointFilters,
   88 | 			Collection<OperationFilter<O>> operationFilters) {
   89 | 		Assert.notNull(applicationContext, "'applicationContext' must not be null");
   90 | 		Assert.notNull(parameterValueMapper, "'parameterValueMapper' must not be null");
   91 | 		Assert.notNull(invokerAdvisors, "'invokerAdvisors' must not be null");
   92 | 		Assert.notNull(endpointFilters, "'endpointFilters' must not be null");
   93 | 		Assert.notNull(operationFilters, "'operationFilters' must not be null");
   94 | 		this.applicationContext = applicationContext;
   95 | 		this.endpointFilters = Collections.unmodifiableCollection(endpointFilters);
   96 | 		this.operationFilters = Collections.unmodifiableCollection(operationFilters);
   97 | 		this.operationsFactory = getOperationsFactory(parameterValueMapper, invokerAdvisors);
   98 | 	}
   99 | 
  100 | 	private DiscoveredOperationsFactory<O> getOperationsFactory(ParameterValueMapper parameterValueMapper,
  101 | 			Collection<OperationInvokerAdvisor> invokerAdvisors) {
  102 | 		return new DiscoveredOperationsFactory<>(parameterValueMapper, invokerAdvisors) {
  103 | 
  104 | 			@Override
  105 | 			Collection<O> createOperations(EndpointId id, Object target) {
  106 | 				return super.createOperations(id, target);
  107 | 			}
  108 | 
  109 | 			@Override
  110 | 			protected O createOperation(EndpointId endpointId, DiscoveredOperationMethod operationMethod,
  111 | 					OperationInvoker invoker) {
  112 | 				return EndpointDiscoverer.this.createOperation(endpointId, operationMethod, invoker);
  113 | 			}
  114 | 
  115 | 		};
  116 | 	}
  117 | 
  118 | 	@Override
  119 | 	public final Collection<E> getEndpoints() {
  120 | 		Collection<E> endpoints = this.endpoints;
  121 | 		if (endpoints == null) {
  122 | 			endpoints = discoverEndpoints();
  123 | 			this.endpoints = endpoints;
  124 | 		}
  125 | 		return endpoints;
  126 | 	}
  127 | 
  128 | 	private Collection<E> discoverEndpoints() {
  129 | 		Collection<EndpointBean> endpointBeans = createEndpointBeans();
  130 | 		addExtensionBeans(endpointBeans);
  131 | 		return convertToEndpoints(endpointBeans);
  132 | 	}
  133 | 
  134 | 	private Collection<EndpointBean> createEndpointBeans() {
  135 | 		Map<EndpointId, EndpointBean> byId = new LinkedHashMap<>();
  136 | 		String[] beanNames = BeanFactoryUtils.beanNamesForAnnotationIncludingAncestors(this.applicationContext,
  137 | 				Endpoint.class);
  138 | 		for (String beanName : beanNames) {
  139 | 			if (!ScopedProxyUtils.isScopedTarget(beanName)) {
  140 | 				EndpointBean endpointBean = createEndpointBean(beanName);
  141 | 				EndpointBean previous = byId.putIfAbsent(endpointBean.getId(), endpointBean);
  142 | 				if (previous != null) {
  143 | 					throw new IllegalStateException("Found two endpoints with the id '" + endpointBean.getId() + "': '"
  144 | 							+ endpointBean.getBeanName() + "' and '" + previous.getBeanName() + "'");
  145 | 				}
  146 | 			}
  147 | 		}
  148 | 		return byId.values();
  149 | 	}
  150 | 
  151 | 	private EndpointBean createEndpointBean(String beanName) {
  152 | 		Class<?> type = this.applicationContext.getType(beanName, false);
  153 | 		Assert.state(type != null, "'type' must not be null");
  154 | 		Class<?> beanType = ClassUtils.getUserClass(type);
  155 | 		Supplier<Object> beanSupplier = () -> this.applicationContext.getBean(beanName);
  156 | 		return new EndpointBean(this.applicationContext.getEnvironment(), beanName, beanType, beanSupplier);
  157 | 	}
  158 | 
  159 | 	private void addExtensionBeans(Collection<EndpointBean> endpointBeans) {
  160 | 		Map<EndpointId, EndpointBean> byId = endpointBeans.stream()
  161 | 			.collect(Collectors.toMap(EndpointBean::getId, Function.identity()));
  162 | 		String[] beanNames = BeanFactoryUtils.beanNamesForAnnotationIncludingAncestors(this.applicationContext,
  163 | 				EndpointExtension.class);
  164 | 		for (String beanName : beanNames) {
  165 | 			ExtensionBean extensionBean = createExtensionBean(beanName);
  166 | 			EndpointBean endpointBean = byId.get(extensionBean.getEndpointId());
  167 | 			Assert.state(endpointBean != null, () -> ("Invalid extension '" + extensionBean.getBeanName()
  168 | 					+ "': no endpoint found with id '" + extensionBean.getEndpointId() + "'"));
  169 | 			addExtensionBean(endpointBean, extensionBean);
  170 | 		}
  171 | 	}
  172 | 
  173 | 	private ExtensionBean createExtensionBean(String beanName) {
  174 | 		Class<?> type = this.applicationContext.getType(beanName);
  175 | 		Assert.state(type != null, "'type' must not be null");
  176 | 		Class<?> beanType = ClassUtils.getUserClass(type);
  177 | 		Supplier<Object> beanSupplier = () -> this.applicationContext.getBean(beanName);
  178 | 		return new ExtensionBean(this.applicationContext.getEnvironment(), beanName, beanType, beanSupplier);
  179 | 	}
  180 | 
  181 | 	private void addExtensionBean(EndpointBean endpointBean, ExtensionBean extensionBean) {
  182 | 		if (isExtensionExposed(endpointBean, extensionBean)) {
  183 | 			Assert.state(isEndpointExposed(endpointBean) || isEndpointFiltered(endpointBean),
  184 | 					() -> "Endpoint bean '" + endpointBean.getBeanName() + "' cannot support the extension bean '"
  185 | 							+ extensionBean.getBeanName() + "'");
  186 | 			endpointBean.addExtension(extensionBean);
  187 | 		}
  188 | 	}
  189 | 
  190 | 	private Collection<E> convertToEndpoints(Collection<EndpointBean> endpointBeans) {
  191 | 		Set<E> endpoints = new LinkedHashSet<>();
  192 | 		for (EndpointBean endpointBean : endpointBeans) {
  193 | 			if (isEndpointExposed(endpointBean)) {
  194 | 				E endpoint = convertToEndpoint(endpointBean);
  195 | 				if (isInvocable(endpoint)) {
  196 | 					endpoints.add(endpoint);
  197 | 				}
  198 | 			}
  199 | 		}
  200 | 		return Collections.unmodifiableSet(endpoints);
  201 | 	}
  202 | 
  203 | 	/**
  204 | 	 * Returns whether the endpoint is invocable and should be included in the discovered
  205 | 	 * endpoints. The default implementation returns {@code true} if the endpoint has any
  206 | 	 * operations, otherwise {@code false}.
  207 | 	 * @param endpoint the endpoint to assess
  208 | 	 * @return {@code true} if the endpoint is invocable, otherwise {@code false}.
  209 | 	 * @since 3.4.0
  210 | 	 */
  211 | 	protected boolean isInvocable(E endpoint) {
  212 | 		return !endpoint.getOperations().isEmpty();
  213 | 	}
  214 | 
  215 | 	private E convertToEndpoint(EndpointBean endpointBean) {
  216 | 		MultiValueMap<OperationKey, O> indexed = new LinkedMultiValueMap<>();
  217 | 		EndpointId id = endpointBean.getId();
  218 | 		addOperations(indexed, id, endpointBean.getDefaultAccess(), endpointBean.getBean(), false);
  219 | 		if (endpointBean.getExtensions().size() > 1) {
  220 | 			String extensionBeans = endpointBean.getExtensions()
  221 | 				.stream()
  222 | 				.map(ExtensionBean::getBeanName)
  223 | 				.collect(Collectors.joining(", "));
  224 | 			throw new IllegalStateException("Found multiple extensions for the endpoint bean "
  225 | 					+ endpointBean.getBeanName() + " (" + extensionBeans + ")");
  226 | 		}
  227 | 		for (ExtensionBean extensionBean : endpointBean.getExtensions()) {
  228 | 			addOperations(indexed, id, endpointBean.getDefaultAccess(), extensionBean.getBean(), true);
  229 | 		}
  230 | 		assertNoDuplicateOperations(endpointBean, indexed);
  231 | 		List<O> operations = indexed.values().stream().map(this::getLast).filter(Objects::nonNull).toList();
  232 | 		return createEndpoint(endpointBean.getBean(), id, endpointBean.getDefaultAccess(), operations);
  233 | 	}
  234 | 
  235 | 	private void addOperations(MultiValueMap<OperationKey, O> indexed, EndpointId id, Access defaultAccess,
  236 | 			Object target, boolean replaceLast) {
  237 | 		Set<OperationKey> replacedLast = new HashSet<>();
  238 | 		Collection<O> operations = this.operationsFactory.createOperations(id, target);
  239 | 		for (O operation : operations) {
  240 | 			if (!isOperationFiltered(operation, id, defaultAccess)) {
  241 | 				OperationKey key = createOperationKey(operation);
  242 | 				O last = getLast(indexed.get(key));
  243 | 				if (replaceLast && replacedLast.add(key) && last != null) {
  244 | 					List<O> os = indexed.get(key);
  245 | 					Assert.state(os != null, "'os' must not be null");
  246 | 					os.remove(last);
  247 | 				}
  248 | 				indexed.add(key, operation);
  249 | 			}
  250 | 		}
  251 | 	}
  252 | 
  253 | 	private <T> @Nullable T getLast(@Nullable List<T> list) {
  254 | 		return CollectionUtils.isEmpty(list) ? null : list.get(list.size() - 1);
  255 | 	}
  256 | 
  257 | 	private void assertNoDuplicateOperations(EndpointBean endpointBean, MultiValueMap<OperationKey, O> indexed) {
  258 | 		List<OperationKey> duplicates = indexed.entrySet()
  259 | 			.stream()
  260 | 			.filter((entry) -> entry.getValue().size() > 1)
  261 | 			.map(Map.Entry::getKey)
  262 | 			.toList();
  263 | 		if (!duplicates.isEmpty()) {
  264 | 			Set<ExtensionBean> extensions = endpointBean.getExtensions();
  265 | 			String extensionBeanNames = extensions.stream()
  266 | 				.map(ExtensionBean::getBeanName)
  267 | 				.collect(Collectors.joining(", "));
  268 | 			throw new IllegalStateException("Unable to map duplicate endpoint operations: " + duplicates + " to "
  269 | 					+ endpointBean.getBeanName() + (extensions.isEmpty() ? "" : " (" + extensionBeanNames + ")"));
  270 | 		}
  271 | 	}
  272 | 
  273 | 	private boolean isExtensionExposed(EndpointBean endpointBean, ExtensionBean extensionBean) {
  274 | 		return isFilterMatch(extensionBean.getFilter(), endpointBean)
  275 | 				&& isExtensionTypeExposed(extensionBean.getBeanType());
  276 | 	}
  277 | 
  278 | 	/**
  279 | 	 * Determine if an extension bean should be exposed. Subclasses can override this
  280 | 	 * method to provide additional logic.
  281 | 	 * @param extensionBeanType the extension bean type
  282 | 	 * @return {@code true} if the extension is exposed
  283 | 	 */
  284 | 	protected boolean isExtensionTypeExposed(Class<?> extensionBeanType) {
  285 | 		return true;
  286 | 	}
  287 | 
  288 | 	private boolean isEndpointExposed(EndpointBean endpointBean) {
  289 | 		return isFilterMatch(endpointBean.getFilter(), endpointBean) && !isEndpointFiltered(endpointBean)
  290 | 				&& isEndpointTypeExposed(endpointBean.getBeanType());
  291 | 	}
  292 | 
  293 | 	/**
  294 | 	 * Determine if an endpoint bean should be exposed. Subclasses can override this
  295 | 	 * method to provide additional logic.
  296 | 	 * @param beanType the endpoint bean type
  297 | 	 * @return {@code true} if the endpoint is exposed
  298 | 	 */
  299 | 	protected boolean isEndpointTypeExposed(Class<?> beanType) {
  300 | 		return true;
  301 | 	}
  302 | 
  303 | 	private boolean isEndpointFiltered(EndpointBean endpointBean) {
  304 | 		for (EndpointFilter<E> filter : this.endpointFilters) {
  305 | 			if (!isFilterMatch(filter, endpointBean)) {
  306 | 				return true;
  307 | 			}
  308 | 		}
  309 | 		return false;
  310 | 	}
  311 | 
  312 | 	@SuppressWarnings("unchecked")
  313 | 	private boolean isFilterMatch(@Nullable Class<?> filter, EndpointBean endpointBean) {
  314 | 		if (!isEndpointTypeExposed(endpointBean.getBeanType())) {
  315 | 			return false;
  316 | 		}
  317 | 		if (filter == null) {
  318 | 			return true;
  319 | 		}
  320 | 		E endpoint = getFilterEndpoint(endpointBean);
  321 | 		Class<?> generic = ResolvableType.forClass(EndpointFilter.class, filter).resolveGeneric(0);
  322 | 		if (generic == null || generic.isInstance(endpoint)) {
  323 | 			EndpointFilter<E> instance = (EndpointFilter<E>) BeanUtils.instantiateClass(filter);
  324 | 			return isFilterMatch(instance, endpoint);
  325 | 		}
  326 | 		return false;
  327 | 	}
  328 | 
  329 | 	private boolean isFilterMatch(EndpointFilter<E> filter, EndpointBean endpointBean) {
  330 | 		return isFilterMatch(filter, getFilterEndpoint(endpointBean));
  331 | 	}
  332 | 
  333 | 	@SuppressWarnings("unchecked")
  334 | 	private boolean isFilterMatch(EndpointFilter<E> filter, E endpoint) {
  335 | 		Boolean result = LambdaSafe.callback(EndpointFilter.class, filter, endpoint)
  336 | 			.withLogger(EndpointDiscoverer.class)
  337 | 			.invokeAnd((f) -> f.match(endpoint))
  338 | 			.get();
  339 | 		Assert.state(result != null, "'result' must not be null");
  340 | 		return result;
  341 | 	}
  342 | 
  343 | 	private boolean isOperationFiltered(Operation operation, EndpointId endpointId, Access defaultAccess) {
  344 | 		for (OperationFilter<O> filter : this.operationFilters) {
  345 | 			if (!isFilterMatch(filter, operation, endpointId, defaultAccess)) {
  346 | 				return true;
  347 | 			}
  348 | 		}
  349 | 		return false;
  350 | 	}
  351 | 
  352 | 	@SuppressWarnings("unchecked")
  353 | 	private boolean isFilterMatch(OperationFilter<O> filter, Operation operation, EndpointId endpointId,
  354 | 			Access defaultAccess) {
  355 | 		Boolean result = LambdaSafe.callback(OperationFilter.class, filter, operation)
  356 | 			.withLogger(EndpointDiscoverer.class)
  357 | 			.invokeAnd((f) -> f.match(operation, endpointId, defaultAccess))
  358 | 			.get();
  359 | 		Assert.state(result != null, "'result' must not be null");
  360 | 		return result;
  361 | 	}
  362 | 
  363 | 	private E getFilterEndpoint(EndpointBean endpointBean) {
  364 | 		return this.filterEndpoints.computeIfAbsent(endpointBean, (key) -> createEndpoint(endpointBean.getBean(),
  365 | 				endpointBean.getId(), endpointBean.getDefaultAccess(), Collections.emptySet()));
  366 | 	}
  367 | 
  368 | 	@SuppressWarnings("unchecked")
  369 | 	protected Class<? extends E> getEndpointType() {
  370 | 		Class<?> generic = ResolvableType.forClass(EndpointDiscoverer.class, getClass()).resolveGeneric(0);
  371 | 		Assert.state(generic != null, "'generic' must not be null");
  372 | 		return (Class<? extends E>) generic;
  373 | 	}
  374 | 
  375 | 	/**
  376 | 	 * Factory method called to create the {@link ExposableEndpoint endpoint}.
  377 | 	 * @param endpointBean the source endpoint bean
  378 | 	 * @param id the ID of the endpoint
  379 | 	 * @param defaultAccess access to the endpoint that is permitted by default
  380 | 	 * @param operations the endpoint operations
  381 | 	 * @return a created endpoint (a {@link DiscoveredEndpoint} is recommended)
  382 | 	 */
  383 | 	protected abstract E createEndpoint(Object endpointBean, EndpointId id, Access defaultAccess,
  384 | 			Collection<O> operations);
  385 | 
  386 | 	/**
  387 | 	 * Factory method to create an {@link Operation endpoint operation}.
  388 | 	 * @param endpointId the endpoint id
  389 | 	 * @param operationMethod the operation method
  390 | 	 * @param invoker the invoker to use
  391 | 	 * @return a created operation
  392 | 	 */
  393 | 	protected abstract O createOperation(EndpointId endpointId, DiscoveredOperationMethod operationMethod,
  394 | 			OperationInvoker invoker);
  395 | 
  396 | 	/**
  397 | 	 * Create an {@link OperationKey} for the given operation.
  398 | 	 * @param operation the source operation
  399 | 	 * @return the operation key
  400 | 	 */
  401 | 	protected abstract OperationKey createOperationKey(O operation);
  402 | 
  403 | 	/**
  404 | 	 * A key generated for an {@link Operation} based on specific criteria from the actual
  405 | 	 * operation implementation.
  406 | 	 */
  407 | 	protected static final class OperationKey {
  408 | 
  409 | 		private final Object key;
  410 | 
  411 | 		private final Supplier<String> description;
  412 | 
  413 | 		/**
  414 | 		 * Create a new {@link OperationKey} instance.
  415 | 		 * @param key the underlying key for the operation
  416 | 		 * @param description a human-readable description of the key
  417 | 		 */
  418 | 		public OperationKey(Object key, Supplier<String> description) {
  419 | 			Assert.notNull(key, "'key' must not be null");
  420 | 			Assert.notNull(description, "'description' must not be null");
  421 | 			this.key = key;
  422 | 			this.description = description;
  423 | 		}
  424 | 
  425 | 		@Override
  426 | 		public boolean equals(Object obj) {
  427 | 			if (obj == this) {
  428 | 				return true;
  429 | 			}
  430 | 			if (obj == null || getClass() != obj.getClass()) {
  431 | 				return false;
  432 | 			}
  433 | 			return this.key.equals(((OperationKey) obj).key);
  434 | 		}
  435 | 
  436 | 		@Override
  437 | 		public int hashCode() {
  438 | 			return this.key.hashCode();
  439 | 		}
  440 | 
  441 | 		@Override
  442 | 		public String toString() {
  443 | 			return this.description.get();
  444 | 		}
  445 | 
  446 | 	}
  447 | 
  448 | 	/**
  449 | 	 * Information about an {@link Endpoint @Endpoint} bean.
  450 | 	 */
  451 | 	private static class EndpointBean {
  452 | 
  453 | 		private final String beanName;
  454 | 
  455 | 		private final Class<?> beanType;
  456 | 
  457 | 		private final Supplier<Object> beanSupplier;
  458 | 
  459 | 		private final EndpointId id;
  460 | 
  461 | 		private final Access defaultAccess;
  462 | 
  463 | 		private final @Nullable Class<?> filter;
  464 | 
  465 | 		private final Set<ExtensionBean> extensions = new LinkedHashSet<>();
  466 | 
  467 | 		EndpointBean(Environment environment, String beanName, Class<?> beanType, Supplier<Object> beanSupplier) {
  468 | 			MergedAnnotation<Endpoint> annotation = MergedAnnotations.from(beanType, SearchStrategy.TYPE_HIERARCHY)
  469 | 				.get(Endpoint.class);
  470 | 			String id = annotation.getString("id");
  471 | 			Assert.state(StringUtils.hasText(id),
  472 | 					() -> "No @Endpoint id attribute specified for " + beanType.getName());
  473 | 			this.beanName = beanName;
  474 | 			this.beanType = beanType;
  475 | 			this.beanSupplier = beanSupplier;
  476 | 			this.id = EndpointId.of(environment, id);
  477 | 			this.defaultAccess = annotation.getEnum("defaultAccess", Access.class);
  478 | 			this.filter = getFilter(beanType);
  479 | 		}
  480 | 
  481 | 		void addExtension(ExtensionBean extensionBean) {
  482 | 			this.extensions.add(extensionBean);
  483 | 		}
  484 | 
  485 | 		Set<ExtensionBean> getExtensions() {
  486 | 			return this.extensions;
  487 | 		}
  488 | 
  489 | 		private @Nullable Class<?> getFilter(Class<?> type) {
  490 | 			return MergedAnnotations.from(type, SearchStrategy.TYPE_HIERARCHY)
  491 | 				.get(FilteredEndpoint.class)
  492 | 				.getValue(MergedAnnotation.VALUE, Class.class)
  493 | 				.orElse(null);
  494 | 		}
  495 | 
  496 | 		String getBeanName() {
  497 | 			return this.beanName;
  498 | 		}
  499 | 
  500 | 		Class<?> getBeanType() {
  501 | 			return this.beanType;
  502 | 		}
  503 | 
  504 | 		Object getBean() {
  505 | 			return this.beanSupplier.get();
  506 | 		}
  507 | 
  508 | 		EndpointId getId() {
  509 | 			return this.id;
  510 | 		}
  511 | 
  512 | 		Access getDefaultAccess() {
  513 | 			return this.defaultAccess;
  514 | 		}
  515 | 
  516 | 		@Nullable Class<?> getFilter() {
  517 | 			return this.filter;
  518 | 		}
  519 | 
  520 | 	}
  521 | 
  522 | 	/**
  523 | 	 * Information about an {@link EndpointExtension @EndpointExtension} bean.
  524 | 	 */
  525 | 	private static class ExtensionBean {
  526 | 
  527 | 		private final String beanName;
  528 | 
  529 | 		private final Class<?> beanType;
  530 | 
  531 | 		private final Supplier<Object> beanSupplier;
  532 | 
  533 | 		private final EndpointId endpointId;
  534 | 
  535 | 		private final Class<?> filter;
  536 | 
  537 | 		ExtensionBean(Environment environment, String beanName, Class<?> beanType, Supplier<Object> beanSupplier) {
  538 | 			this.beanName = beanName;
  539 | 			this.beanType = beanType;
  540 | 			this.beanSupplier = beanSupplier;
  541 | 			MergedAnnotation<EndpointExtension> extensionAnnotation = MergedAnnotations
  542 | 				.from(beanType, SearchStrategy.TYPE_HIERARCHY)
  543 | 				.get(EndpointExtension.class);
  544 | 			Class<?> endpointType = extensionAnnotation.getClass("endpoint");
  545 | 			MergedAnnotation<Endpoint> endpointAnnotation = MergedAnnotations
  546 | 				.from(endpointType, SearchStrategy.TYPE_HIERARCHY)
  547 | 				.get(Endpoint.class);
  548 | 			Assert.state(endpointAnnotation.isPresent(),
  549 | 					() -> "Extension " + endpointType.getName() + " does not specify an endpoint");
  550 | 			this.endpointId = EndpointId.of(environment, endpointAnnotation.getString("id"));
  551 | 			this.filter = extensionAnnotation.getClass("filter");
  552 | 		}
  553 | 
  554 | 		String getBeanName() {
  555 | 			return this.beanName;
  556 | 		}
  557 | 
  558 | 		Class<?> getBeanType() {
  559 | 			return this.beanType;
  560 | 		}
  561 | 
  562 | 		Object getBean() {
  563 | 			return this.beanSupplier.get();
  564 | 		}
  565 | 
  566 | 		EndpointId getEndpointId() {
  567 | 			return this.endpointId;
  568 | 		}
  569 | 
  570 | 		Class<?> getFilter() {
  571 | 			return this.filter;
  572 | 		}
  573 | 
  574 | 	}
  575 | 
  576 | }
```
