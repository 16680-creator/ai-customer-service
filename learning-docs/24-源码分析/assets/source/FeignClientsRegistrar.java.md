<!-- learning-docs source archive（机械抓取原文，禁止手改代码内容） -->
<!-- origin: org.springframework.cloud:spring-cloud-openfeign-core:5.1.0 | path: org/springframework/cloud/openfeign/FeignClientsRegistrar.java -->
<!-- 正文引用：[[源码:FeignClientsRegistrar.java#L起-L止]] ；代码块头部：(src: FeignClientsRegistrar.java#L起-L止) -->

# FeignClientsRegistrar.java

| 项 | 值 |
| --- | --- |
| 来源 | `org.springframework.cloud:spring-cloud-openfeign-core:5.1.0` |
| 路径 | `org/springframework/cloud/openfeign/FeignClientsRegistrar.java` |
| 行数 | 502 |
| 在线地址 | https://repo1.maven.org/maven2/org/springframework/cloud/spring-cloud-openfeign-core/5.1.0/spring-cloud-openfeign-core-5.1.0-sources.jar |
| 抓取时间 | 2026-11-25T15:33:24.306Z |

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
   17 | package org.springframework.cloud.openfeign;
   18 | 
   19 | import java.net.URI;
   20 | import java.net.URISyntaxException;
   21 | import java.util.ArrayList;
   22 | import java.util.Arrays;
   23 | import java.util.Collections;
   24 | import java.util.HashSet;
   25 | import java.util.LinkedHashSet;
   26 | import java.util.List;
   27 | import java.util.Map;
   28 | import java.util.Set;
   29 | 
   30 | import feign.Request;
   31 | 
   32 | import org.springframework.aop.scope.ScopedProxyUtils;
   33 | import org.springframework.beans.factory.FactoryBean;
   34 | import org.springframework.beans.factory.annotation.AnnotatedBeanDefinition;
   35 | import org.springframework.beans.factory.annotation.AnnotatedGenericBeanDefinition;
   36 | import org.springframework.beans.factory.config.BeanDefinition;
   37 | import org.springframework.beans.factory.config.BeanDefinitionHolder;
   38 | import org.springframework.beans.factory.config.BeanExpressionContext;
   39 | import org.springframework.beans.factory.config.BeanExpressionResolver;
   40 | import org.springframework.beans.factory.config.ConfigurableBeanFactory;
   41 | import org.springframework.beans.factory.support.AbstractBeanDefinition;
   42 | import org.springframework.beans.factory.support.BeanDefinitionBuilder;
   43 | import org.springframework.beans.factory.support.BeanDefinitionReaderUtils;
   44 | import org.springframework.beans.factory.support.BeanDefinitionRegistry;
   45 | import org.springframework.context.EnvironmentAware;
   46 | import org.springframework.context.ResourceLoaderAware;
   47 | import org.springframework.context.annotation.ClassPathScanningCandidateComponentProvider;
   48 | import org.springframework.context.annotation.ImportBeanDefinitionRegistrar;
   49 | import org.springframework.core.annotation.AnnotationAttributes;
   50 | import org.springframework.core.env.Environment;
   51 | import org.springframework.core.io.ResourceLoader;
   52 | import org.springframework.core.type.AnnotationMetadata;
   53 | import org.springframework.core.type.filter.AnnotationTypeFilter;
   54 | import org.springframework.util.Assert;
   55 | import org.springframework.util.ClassUtils;
   56 | import org.springframework.util.ObjectUtils;
   57 | import org.springframework.util.StringUtils;
   58 | 
   59 | /**
   60 |  * @author Spencer Gibb
   61 |  * @author Jakub Narloch
   62 |  * @author Venil Noronha
   63 |  * @author Gang Li
   64 |  * @author Michal Domagala
   65 |  * @author Marcin Grzejszczak
   66 |  * @author Olga Maciaszek-Sharma
   67 |  * @author Jasbir Singh
   68 |  * @author Jinho Lee
   69 |  * @author HyeongDo Myeong
   70 |  */
   71 | class FeignClientsRegistrar implements ImportBeanDefinitionRegistrar, ResourceLoaderAware, EnvironmentAware {
   72 | 
   73 | 	// patterned after Spring Integration IntegrationComponentScanRegistrar
   74 | 	// and RibbonClientsConfigurationRegistrar
   75 | 
   76 | 	private ResourceLoader resourceLoader;
   77 | 
   78 | 	private Environment environment;
   79 | 
   80 | 	FeignClientsRegistrar() {
   81 | 	}
   82 | 
   83 | 	static void validateFallback(final Class clazz) {
   84 | 		Assert.isTrue(!clazz.isInterface(), "Fallback class must implement the interface annotated by @FeignClient");
   85 | 	}
   86 | 
   87 | 	static void validateFallbackFactory(final Class clazz) {
   88 | 		Assert.isTrue(!clazz.isInterface(), "Fallback factory must produce instances "
   89 | 				+ "of fallback classes that implement the interface annotated by @FeignClient");
   90 | 	}
   91 | 
   92 | 	static String getName(String name) {
   93 | 		if (!StringUtils.hasText(name)) {
   94 | 			return "";
   95 | 		}
   96 | 
   97 | 		String host = null;
   98 | 		try {
   99 | 			String url;
  100 | 			if (!name.startsWith("http://") && !name.startsWith("https://")) {
  101 | 				url = "http://" + name;
  102 | 			}
  103 | 			else {
  104 | 				url = name;
  105 | 			}
  106 | 			host = new URI(url).getHost();
  107 | 
  108 | 		}
  109 | 		catch (URISyntaxException ignored) {
  110 | 		}
  111 | 		Assert.state(host != null, "Service id not legal hostname (" + name + ")");
  112 | 		return name;
  113 | 	}
  114 | 
  115 | 	static String getUrl(String url) {
  116 | 		if (StringUtils.hasText(url) && !(url.startsWith("#{") && url.contains("}"))) {
  117 | 			if (!url.contains("://")) {
  118 | 				url = "http://" + url;
  119 | 			}
  120 | 			if (url.endsWith("/")) {
  121 | 				url = url.substring(0, url.length() - 1);
  122 | 			}
  123 | 			try {
  124 | 				new URI(url);
  125 | 			}
  126 | 			catch (URISyntaxException e) {
  127 | 				throw new IllegalArgumentException(url + " is malformed", e);
  128 | 			}
  129 | 		}
  130 | 		return url;
  131 | 	}
  132 | 
  133 | 	static String getPath(String path) {
  134 | 		if (StringUtils.hasText(path)) {
  135 | 			path = path.trim();
  136 | 			if (!path.startsWith("/")) {
  137 | 				path = "/" + path;
  138 | 			}
  139 | 			if (path.endsWith("/")) {
  140 | 				path = path.substring(0, path.length() - 1);
  141 | 			}
  142 | 		}
  143 | 		return path;
  144 | 	}
  145 | 
  146 | 	@Override
  147 | 	public void setResourceLoader(ResourceLoader resourceLoader) {
  148 | 		this.resourceLoader = resourceLoader;
  149 | 	}
  150 | 
  151 | 	@Override
  152 | 	public void registerBeanDefinitions(AnnotationMetadata metadata, BeanDefinitionRegistry registry) {
  153 | 		registerDefaultConfiguration(metadata, registry);
  154 | 		registerFeignClients(metadata, registry);
  155 | 	}
  156 | 
  157 | 	private void registerDefaultConfiguration(AnnotationMetadata metadata, BeanDefinitionRegistry registry) {
  158 | 		Map<String, Object> defaultAttrs = metadata.getAnnotationAttributes(EnableFeignClients.class.getName(), true);
  159 | 
  160 | 		if (defaultAttrs != null && defaultAttrs.containsKey("defaultConfiguration")) {
  161 | 			String name;
  162 | 			if (metadata.hasEnclosingClass()) {
  163 | 				name = "default." + metadata.getEnclosingClassName();
  164 | 			}
  165 | 			else {
  166 | 				name = "default." + metadata.getClassName();
  167 | 			}
  168 | 			registerClientConfiguration(registry, name, "default", defaultAttrs.get("defaultConfiguration"));
  169 | 		}
  170 | 	}
  171 | 
  172 | 	public void registerFeignClients(AnnotationMetadata metadata, BeanDefinitionRegistry registry) {
  173 | 		LinkedHashSet<BeanDefinition> candidateComponents = new LinkedHashSet<>();
  174 | 		Map<String, Object> attrs = metadata.getAnnotationAttributes(EnableFeignClients.class.getName());
  175 | 		final Class<?>[] clients = attrs == null ? null : (Class<?>[]) attrs.get("clients");
  176 | 		if (clients == null || clients.length == 0) {
  177 | 			ClassPathScanningCandidateComponentProvider scanner = getScanner();
  178 | 			scanner.setResourceLoader(this.resourceLoader);
  179 | 			scanner.addIncludeFilter(new AnnotationTypeFilter(FeignClient.class));
  180 | 			Set<String> basePackages = getBasePackages(metadata);
  181 | 			for (String basePackage : basePackages) {
  182 | 				candidateComponents.addAll(scanner.findCandidateComponents(basePackage));
  183 | 			}
  184 | 		}
  185 | 		else {
  186 | 			for (Class<?> clazz : clients) {
  187 | 				candidateComponents.add(new AnnotatedGenericBeanDefinition(clazz));
  188 | 			}
  189 | 		}
  190 | 
  191 | 		for (BeanDefinition candidateComponent : candidateComponents) {
  192 | 			if (candidateComponent instanceof AnnotatedBeanDefinition beanDefinition) {
  193 | 				// verify annotated class is an interface
  194 | 				AnnotationMetadata annotationMetadata = beanDefinition.getMetadata();
  195 | 				Assert.isTrue(annotationMetadata.isInterface(), "@FeignClient can only be specified on an interface");
  196 | 
  197 | 				Map<String, Object> attributes = annotationMetadata
  198 | 					.getAnnotationAttributes(FeignClient.class.getCanonicalName());
  199 | 
  200 | 				String name = getClientName(attributes);
  201 | 				String className = annotationMetadata.getClassName();
  202 | 				registerClientConfiguration(registry, name, className, attributes.get("configuration"));
  203 | 
  204 | 				registerFeignClient(registry, annotationMetadata, attributes);
  205 | 			}
  206 | 		}
  207 | 	}
  208 | 
  209 | 	private void registerFeignClient(BeanDefinitionRegistry registry, AnnotationMetadata annotationMetadata,
  210 | 			Map<String, Object> attributes) {
  211 | 		String className = annotationMetadata.getClassName();
  212 | 		if (String.valueOf(false)
  213 | 			.equals(environment.getProperty("spring.cloud.openfeign.lazy-attributes-resolution",
  214 | 					String.valueOf(false)))) {
  215 | 			eagerlyRegisterFeignClientBeanDefinition(className, attributes, registry);
  216 | 		}
  217 | 		else {
  218 | 			lazilyRegisterFeignClientBeanDefinition(className, attributes, registry);
  219 | 		}
  220 | 	}
  221 | 
  222 | 	private void eagerlyRegisterFeignClientBeanDefinition(String className, Map<String, Object> attributes,
  223 | 			BeanDefinitionRegistry registry) {
  224 | 		validate(attributes);
  225 | 		BeanDefinitionBuilder definition = BeanDefinitionBuilder.genericBeanDefinition(FeignClientFactoryBean.class);
  226 | 		definition.addPropertyValue("url", getUrl(null, attributes));
  227 | 		definition.addPropertyValue("path", getPath(null, attributes));
  228 | 		String name = getName(attributes);
  229 | 		definition.addPropertyValue("name", name);
  230 | 		String contextId = getContextId(null, attributes);
  231 | 		definition.addPropertyValue("contextId", contextId);
  232 | 		definition.addPropertyValue("type", className);
  233 | 		definition.addPropertyValue("dismiss404", Boolean.parseBoolean(String.valueOf(attributes.get("dismiss404"))));
  234 | 		Object fallback = attributes.get("fallback");
  235 | 		if (fallback != null) {
  236 | 			definition.addPropertyValue("fallback",
  237 | 					(fallback instanceof Class ? fallback : ClassUtils.resolveClassName(fallback.toString(), null)));
  238 | 		}
  239 | 		Object fallbackFactory = attributes.get("fallbackFactory");
  240 | 		if (fallbackFactory != null) {
  241 | 			definition.addPropertyValue("fallbackFactory", fallbackFactory instanceof Class ? fallbackFactory
  242 | 					: ClassUtils.resolveClassName(fallbackFactory.toString(), null));
  243 | 		}
  244 | 		definition.setAutowireMode(AbstractBeanDefinition.AUTOWIRE_BY_TYPE);
  245 | 		definition.addPropertyValue("refreshableClient", isClientRefreshEnabled());
  246 | 		String[] qualifiers = getQualifiers(attributes);
  247 | 		if (ObjectUtils.isEmpty(qualifiers)) {
  248 | 			qualifiers = new String[] { contextId + "FeignClient" };
  249 | 		}
  250 | 		// This is done so that there's a way to retrieve qualifiers while generating AOT
  251 | 		// code
  252 | 		definition.addPropertyValue("qualifiers", qualifiers);
  253 | 		AbstractBeanDefinition beanDefinition = definition.getBeanDefinition();
  254 | 		Class<?> type = ClassUtils.resolveClassName(className, null);
  255 | 		beanDefinition.setAttribute(FactoryBean.OBJECT_TYPE_ATTRIBUTE, type);
  256 | 		// has a default, won't be null
  257 | 		boolean primary = (Boolean) attributes.get("primary");
  258 | 		beanDefinition.setPrimary(primary);
  259 | 		BeanDefinitionHolder holder = new BeanDefinitionHolder(beanDefinition, className, qualifiers);
  260 | 		BeanDefinitionReaderUtils.registerBeanDefinition(holder, registry);
  261 | 		registerRefreshableBeanDefinition(registry, contextId, Request.Options.class, OptionsFactoryBean.class);
  262 | 		registerRefreshableBeanDefinition(registry, contextId, RefreshableUrl.class, RefreshableUrlFactoryBean.class);
  263 | 	}
  264 | 
  265 | 	private void lazilyRegisterFeignClientBeanDefinition(String className, Map<String, Object> attributes,
  266 | 			BeanDefinitionRegistry registry) {
  267 | 		ConfigurableBeanFactory beanFactory = registry instanceof ConfigurableBeanFactory
  268 | 				? (ConfigurableBeanFactory) registry : null;
  269 | 		Class clazz = ClassUtils.resolveClassName(className, null);
  270 | 		String contextId = getContextId(beanFactory, attributes);
  271 | 		String name = getName(attributes);
  272 | 		FeignClientFactoryBean factoryBean = new FeignClientFactoryBean();
  273 | 		factoryBean.setBeanFactory(beanFactory);
  274 | 		factoryBean.setName(name);
  275 | 		factoryBean.setContextId(contextId);
  276 | 		factoryBean.setType(clazz);
  277 | 		factoryBean.setRefreshableClient(isClientRefreshEnabled());
  278 | 		BeanDefinitionBuilder definition = BeanDefinitionBuilder.genericBeanDefinition(clazz, () -> {
  279 | 			factoryBean.setUrl(getUrl(beanFactory, attributes));
  280 | 			factoryBean.setPath(getPath(beanFactory, attributes));
  281 | 			factoryBean.setDismiss404(Boolean.parseBoolean(String.valueOf(attributes.get("dismiss404"))));
  282 | 			Object fallback = attributes.get("fallback");
  283 | 			if (fallback != null) {
  284 | 				factoryBean.setFallback(fallback instanceof Class ? (Class<?>) fallback
  285 | 						: ClassUtils.resolveClassName(fallback.toString(), null));
  286 | 			}
  287 | 			Object fallbackFactory = attributes.get("fallbackFactory");
  288 | 			if (fallbackFactory != null) {
  289 | 				factoryBean.setFallbackFactory(fallbackFactory instanceof Class ? (Class<?>) fallbackFactory
  290 | 						: ClassUtils.resolveClassName(fallbackFactory.toString(), null));
  291 | 			}
  292 | 			return factoryBean.getObject();
  293 | 		});
  294 | 		definition.setAutowireMode(AbstractBeanDefinition.AUTOWIRE_BY_TYPE);
  295 | 		definition.setLazyInit(true);
  296 | 		validate(attributes);
  297 | 
  298 | 		AbstractBeanDefinition beanDefinition = definition.getBeanDefinition();
  299 | 		beanDefinition.setAttribute("feignClientsRegistrarFactoryBean", factoryBean);
  300 | 
  301 | 		// has a default, won't be null
  302 | 		boolean primary = (Boolean) attributes.get("primary");
  303 | 
  304 | 		beanDefinition.setPrimary(primary);
  305 | 
  306 | 		String[] qualifiers = getQualifiers(attributes);
  307 | 		if (ObjectUtils.isEmpty(qualifiers)) {
  308 | 			qualifiers = new String[] { contextId + "FeignClient" };
  309 | 		}
  310 | 
  311 | 		BeanDefinitionHolder holder = new BeanDefinitionHolder(beanDefinition, className, qualifiers);
  312 | 		BeanDefinitionReaderUtils.registerBeanDefinition(holder, registry);
  313 | 
  314 | 		registerRefreshableBeanDefinition(registry, contextId, Request.Options.class, OptionsFactoryBean.class);
  315 | 		registerRefreshableBeanDefinition(registry, contextId, RefreshableUrl.class, RefreshableUrlFactoryBean.class);
  316 | 	}
  317 | 
  318 | 	private void validate(Map<String, Object> attributes) {
  319 | 		AnnotationAttributes annotation = AnnotationAttributes.fromMap(attributes);
  320 | 		// This blows up if an aliased property is overspecified
  321 | 		// FIXME annotation.getAliasedString("name", FeignClient.class, null);
  322 | 		validateFallback(annotation.getClass("fallback"));
  323 | 		validateFallbackFactory(annotation.getClass("fallbackFactory"));
  324 | 	}
  325 | 
  326 | 	String getName(Map<String, Object> attributes) {
  327 | 		String name = (String) attributes.get("serviceId");
  328 | 		if (!StringUtils.hasText(name)) {
  329 | 			name = (String) attributes.get("name");
  330 | 		}
  331 | 		if (!StringUtils.hasText(name)) {
  332 | 			name = (String) attributes.get("value");
  333 | 		}
  334 | 		name = resolve(null, name);
  335 | 		return getName(name);
  336 | 	}
  337 | 
  338 | 	private String getContextId(ConfigurableBeanFactory beanFactory, Map<String, Object> attributes) {
  339 | 		String contextId = (String) attributes.get("contextId");
  340 | 		if (!StringUtils.hasText(contextId)) {
  341 | 			return getName(attributes);
  342 | 		}
  343 | 
  344 | 		contextId = resolve(beanFactory, contextId);
  345 | 		return getName(contextId);
  346 | 	}
  347 | 
  348 | 	private String resolve(ConfigurableBeanFactory beanFactory, String value) {
  349 | 		if (StringUtils.hasText(value)) {
  350 | 			if (beanFactory == null) {
  351 | 				return this.environment.resolvePlaceholders(value);
  352 | 			}
  353 | 			BeanExpressionResolver resolver = beanFactory.getBeanExpressionResolver();
  354 | 			String resolved = beanFactory.resolveEmbeddedValue(value);
  355 | 			if (resolver == null) {
  356 | 				return resolved;
  357 | 			}
  358 | 			Object evaluateValue = resolver.evaluate(resolved, new BeanExpressionContext(beanFactory, null));
  359 | 			if (evaluateValue != null) {
  360 | 				return String.valueOf(evaluateValue);
  361 | 			}
  362 | 			return null;
  363 | 		}
  364 | 		return value;
  365 | 	}
  366 | 
  367 | 	private String getUrl(ConfigurableBeanFactory beanFactory, Map<String, Object> attributes) {
  368 | 		String url = resolve(beanFactory, (String) attributes.get("url"));
  369 | 		return getUrl(url);
  370 | 	}
  371 | 
  372 | 	private String getPath(ConfigurableBeanFactory beanFactory, Map<String, Object> attributes) {
  373 | 		String path = resolve(beanFactory, (String) attributes.get("path"));
  374 | 		return getPath(path);
  375 | 	}
  376 | 
  377 | 	protected ClassPathScanningCandidateComponentProvider getScanner() {
  378 | 		return new ClassPathScanningCandidateComponentProvider(false, this.environment) {
  379 | 			@Override
  380 | 			protected boolean isCandidateComponent(AnnotatedBeanDefinition beanDefinition) {
  381 | 				boolean isCandidate = false;
  382 | 				if (beanDefinition.getMetadata().isIndependent()) {
  383 | 					if (!beanDefinition.getMetadata().isAnnotation()) {
  384 | 						isCandidate = true;
  385 | 					}
  386 | 				}
  387 | 				return isCandidate;
  388 | 			}
  389 | 		};
  390 | 	}
  391 | 
  392 | 	protected Set<String> getBasePackages(AnnotationMetadata importingClassMetadata) {
  393 | 		Map<String, Object> attributes = importingClassMetadata
  394 | 			.getAnnotationAttributes(EnableFeignClients.class.getCanonicalName());
  395 | 
  396 | 		Set<String> basePackages = new HashSet<>();
  397 | 		for (String pkg : (String[]) attributes.get("value")) {
  398 | 			if (StringUtils.hasText(pkg)) {
  399 | 				basePackages.add(pkg);
  400 | 			}
  401 | 		}
  402 | 		for (String pkg : (String[]) attributes.get("basePackages")) {
  403 | 			if (StringUtils.hasText(pkg)) {
  404 | 				basePackages.add(pkg);
  405 | 			}
  406 | 		}
  407 | 		for (Class<?> clazz : (Class[]) attributes.get("basePackageClasses")) {
  408 | 			basePackages.add(ClassUtils.getPackageName(clazz));
  409 | 		}
  410 | 
  411 | 		if (basePackages.isEmpty()) {
  412 | 			basePackages.add(ClassUtils.getPackageName(importingClassMetadata.getClassName()));
  413 | 		}
  414 | 		return basePackages;
  415 | 	}
  416 | 
  417 | 	private String getQualifier(Map<String, Object> client) {
  418 | 		if (client == null) {
  419 | 			return null;
  420 | 		}
  421 | 		String qualifier = (String) client.get("qualifier");
  422 | 		if (StringUtils.hasText(qualifier)) {
  423 | 			return qualifier;
  424 | 		}
  425 | 		return null;
  426 | 	}
  427 | 
  428 | 	private String[] getQualifiers(Map<String, Object> client) {
  429 | 		if (client == null) {
  430 | 			return null;
  431 | 		}
  432 | 		List<String> qualifierList = new ArrayList<>(Arrays.asList((String[]) client.get("qualifiers")));
  433 | 		qualifierList.removeIf(qualifier -> !StringUtils.hasText(qualifier));
  434 | 		if (qualifierList.isEmpty() && getQualifier(client) != null) {
  435 | 			qualifierList = Collections.singletonList(getQualifier(client));
  436 | 		}
  437 | 		return !qualifierList.isEmpty() ? qualifierList.toArray(new String[0]) : null;
  438 | 	}
  439 | 
  440 | 	private String getClientName(Map<String, Object> client) {
  441 | 		if (client == null) {
  442 | 			return null;
  443 | 		}
  444 | 		String value = (String) client.get("contextId");
  445 | 		if (!StringUtils.hasText(value)) {
  446 | 			value = (String) client.get("value");
  447 | 		}
  448 | 		if (!StringUtils.hasText(value)) {
  449 | 			value = (String) client.get("name");
  450 | 		}
  451 | 		if (!StringUtils.hasText(value)) {
  452 | 			value = (String) client.get("serviceId");
  453 | 		}
  454 | 		if (StringUtils.hasText(value)) {
  455 | 			return value;
  456 | 		}
  457 | 
  458 | 		throw new IllegalStateException(
  459 | 				"Either 'name' or 'value' must be provided in @" + FeignClient.class.getSimpleName());
  460 | 	}
  461 | 
  462 | 	private void registerClientConfiguration(BeanDefinitionRegistry registry, Object name, Object className,
  463 | 			Object configuration) {
  464 | 		BeanDefinitionBuilder builder = BeanDefinitionBuilder.genericBeanDefinition(FeignClientSpecification.class);
  465 | 		builder.addConstructorArgValue(name);
  466 | 		builder.addConstructorArgValue(className);
  467 | 		builder.addConstructorArgValue(configuration);
  468 | 		registry.registerBeanDefinition(name + "." + FeignClientSpecification.class.getSimpleName(),
  469 | 				builder.getBeanDefinition());
  470 | 	}
  471 | 
  472 | 	@Override
  473 | 	public void setEnvironment(Environment environment) {
  474 | 		this.environment = environment;
  475 | 	}
  476 | 
  477 | 	/**
  478 | 	 * This method registers beans definition with refreshScope.
  479 | 	 * @param registry spring bean definition registry
  480 | 	 * @param contextId name of feign client
  481 | 	 * @param beanType type of bean
  482 | 	 * @param factoryBeanType points to a relevant bean factory
  483 | 	 */
  484 | 	private void registerRefreshableBeanDefinition(BeanDefinitionRegistry registry, String contextId, Class<?> beanType,
  485 | 			Class<?> factoryBeanType) {
  486 | 		if (isClientRefreshEnabled()) {
  487 | 			String beanName = beanType.getCanonicalName() + "-" + contextId;
  488 | 			BeanDefinitionBuilder definitionBuilder = BeanDefinitionBuilder.genericBeanDefinition(factoryBeanType);
  489 | 			definitionBuilder.setScope("refresh");
  490 | 			definitionBuilder.addPropertyValue("contextId", contextId);
  491 | 			BeanDefinitionHolder definitionHolder = new BeanDefinitionHolder(definitionBuilder.getBeanDefinition(),
  492 | 					beanName);
  493 | 			definitionHolder = ScopedProxyUtils.createScopedProxy(definitionHolder, registry, true);
  494 | 			BeanDefinitionReaderUtils.registerBeanDefinition(definitionHolder, registry);
  495 | 		}
  496 | 	}
  497 | 
  498 | 	private boolean isClientRefreshEnabled() {
  499 | 		return environment.getProperty("spring.cloud.openfeign.client.refresh-enabled", Boolean.class, false);
  500 | 	}
  501 | 
  502 | }
```
