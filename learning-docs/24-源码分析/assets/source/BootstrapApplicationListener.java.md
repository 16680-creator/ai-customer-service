<!-- learning-docs source archive（机械抓取原文，禁止手改代码内容） -->
<!-- origin: org.springframework.cloud:spring-cloud-context:5.1.0 | path: org/springframework/cloud/bootstrap/BootstrapApplicationListener.java -->
<!-- 正文引用：[[源码:BootstrapApplicationListener.java#L起-L止]] ；代码块头部：(src: BootstrapApplicationListener.java#L起-L止) -->

# BootstrapApplicationListener.java

| 项 | 值 |
| --- | --- |
| 来源 | `org.springframework.cloud:spring-cloud-context:5.1.0` |
| 路径 | `org/springframework/cloud/bootstrap/BootstrapApplicationListener.java` |
| 行数 | 523 |
| 在线地址 | https://repo1.maven.org/maven2/org/springframework/cloud/spring-cloud-context/5.1.0/spring-cloud-context-5.1.0-sources.jar |
| 抓取时间 | 2026-11-25T15:31:37.355Z |

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
   17 | package org.springframework.cloud.bootstrap;
   18 | 
   19 | import java.lang.reflect.Field;
   20 | import java.util.ArrayList;
   21 | import java.util.Arrays;
   22 | import java.util.Collection;
   23 | import java.util.HashMap;
   24 | import java.util.LinkedHashMap;
   25 | import java.util.LinkedHashSet;
   26 | import java.util.List;
   27 | import java.util.Map;
   28 | import java.util.Set;
   29 | 
   30 | import org.jspecify.annotations.Nullable;
   31 | 
   32 | import org.springframework.beans.factory.ListableBeanFactory;
   33 | import org.springframework.boot.Banner.Mode;
   34 | import org.springframework.boot.SpringApplication;
   35 | import org.springframework.boot.WebApplicationType;
   36 | import org.springframework.boot.builder.ParentContextApplicationContextInitializer;
   37 | import org.springframework.boot.builder.SpringApplicationBuilder;
   38 | import org.springframework.boot.context.event.ApplicationEnvironmentPreparedEvent;
   39 | import org.springframework.boot.context.event.ApplicationFailedEvent;
   40 | import org.springframework.boot.context.logging.LoggingApplicationListener;
   41 | import org.springframework.boot.env.OriginTrackedMapPropertySource;
   42 | import org.springframework.boot.origin.Origin;
   43 | import org.springframework.boot.origin.OriginLookup;
   44 | import org.springframework.cloud.bootstrap.encrypt.EnvironmentDecryptApplicationInitializer;
   45 | import org.springframework.cloud.bootstrap.support.OriginTrackedCompositePropertySource;
   46 | import org.springframework.context.ApplicationContextInitializer;
   47 | import org.springframework.context.ApplicationEvent;
   48 | import org.springframework.context.ApplicationListener;
   49 | import org.springframework.context.ConfigurableApplicationContext;
   50 | import org.springframework.context.event.SmartApplicationListener;
   51 | import org.springframework.core.Ordered;
   52 | import org.springframework.core.annotation.AnnotationAwareOrderComparator;
   53 | import org.springframework.core.annotation.Order;
   54 | import org.springframework.core.env.AbstractEnvironment;
   55 | import org.springframework.core.env.CompositePropertySource;
   56 | import org.springframework.core.env.ConfigurableEnvironment;
   57 | import org.springframework.core.env.MapPropertySource;
   58 | import org.springframework.core.env.MutablePropertySources;
   59 | import org.springframework.core.env.PropertySource;
   60 | import org.springframework.core.env.PropertySource.StubPropertySource;
   61 | import org.springframework.core.env.SystemEnvironmentPropertySource;
   62 | import org.springframework.util.ReflectionUtils;
   63 | import org.springframework.util.StringUtils;
   64 | 
   65 | import static org.springframework.cloud.util.PropertyUtils.bootstrapEnabled;
   66 | import static org.springframework.cloud.util.PropertyUtils.useLegacyProcessing;
   67 | 
   68 | /**
   69 |  * A listener that prepares a SpringApplication (e.g. populating its Environment) by
   70 |  * delegating to {@link ApplicationContextInitializer} beans in a separate bootstrap
   71 |  * context. The bootstrap context is a SpringApplication created from sources defined in
   72 |  * spring.factories as {@link BootstrapConfiguration}, and initialized with external
   73 |  * config taken from "bootstrap.properties" (or yml), instead of the normal
   74 |  * "application.properties".
   75 |  *
   76 |  * @author Dave Syer
   77 |  *
   78 |  */
   79 | public class BootstrapApplicationListener implements ApplicationListener<ApplicationEnvironmentPreparedEvent>, Ordered {
   80 | 
   81 | 	/**
   82 | 	 * Property source name for bootstrap.
   83 | 	 */
   84 | 	public static final String BOOTSTRAP_PROPERTY_SOURCE_NAME = "bootstrap";
   85 | 
   86 | 	/**
   87 | 	 * The default order for this listener.
   88 | 	 */
   89 | 	public static final int DEFAULT_ORDER = Ordered.HIGHEST_PRECEDENCE + 5;
   90 | 
   91 | 	/**
   92 | 	 * The name of the default properties.
   93 | 	 */
   94 | 	public static final String DEFAULT_PROPERTIES = "springCloudDefaultProperties";
   95 | 
   96 | 	private int order = DEFAULT_ORDER;
   97 | 
   98 | 	@Override
   99 | 	public void onApplicationEvent(ApplicationEnvironmentPreparedEvent event) {
  100 | 		ConfigurableEnvironment environment = event.getEnvironment();
  101 | 		if (!bootstrapEnabled(environment) && !useLegacyProcessing(environment)) {
  102 | 			return;
  103 | 		}
  104 | 		// don't listen to events in a bootstrap context
  105 | 		if (environment.getPropertySources().contains(BOOTSTRAP_PROPERTY_SOURCE_NAME)) {
  106 | 			return;
  107 | 		}
  108 | 		ConfigurableApplicationContext context = null;
  109 | 		String configName = environment.resolvePlaceholders("${spring.cloud.bootstrap.name:bootstrap}");
  110 | 		for (ApplicationContextInitializer<?> initializer : event.getSpringApplication().getInitializers()) {
  111 | 			if (initializer instanceof ParentContextApplicationContextInitializer contextInitializer) {
  112 | 				context = findBootstrapContext(contextInitializer, configName);
  113 | 			}
  114 | 		}
  115 | 		if (context == null) {
  116 | 			context = bootstrapServiceContext(environment, event.getSpringApplication(), configName);
  117 | 			event.getSpringApplication().addListeners(new CloseContextOnFailureApplicationListener(context));
  118 | 		}
  119 | 
  120 | 		apply(context, event.getSpringApplication(), environment);
  121 | 	}
  122 | 
  123 | 	private @Nullable ConfigurableApplicationContext findBootstrapContext(
  124 | 			ParentContextApplicationContextInitializer initializer, String configName) {
  125 | 		ConfigurableApplicationContext parent = null;
  126 | 		Field field = ReflectionUtils.findField(ParentContextApplicationContextInitializer.class, "parent");
  127 | 		if (field != null) {
  128 | 			ReflectionUtils.makeAccessible(field);
  129 | 			Object value = ReflectionUtils.getField(field, initializer);
  130 | 			if (value != null) {
  131 | 				parent = safeCast(ConfigurableApplicationContext.class, value);
  132 | 			}
  133 | 			if (parent != null && !configName.equals(parent.getId()) && parent.getParent() != null) {
  134 | 				parent = safeCast(ConfigurableApplicationContext.class, parent.getParent());
  135 | 			}
  136 | 		}
  137 | 		return parent;
  138 | 	}
  139 | 
  140 | 	private <T> @Nullable T safeCast(Class<T> type, Object object) {
  141 | 		try {
  142 | 			return type.cast(object);
  143 | 		}
  144 | 		catch (ClassCastException e) {
  145 | 			return null;
  146 | 		}
  147 | 	}
  148 | 
  149 | 	private ConfigurableApplicationContext bootstrapServiceContext(ConfigurableEnvironment environment,
  150 | 			final SpringApplication application, String configName) {
  151 | 		ConfigurableEnvironment bootstrapEnvironment = new AbstractEnvironment() {
  152 | 		};
  153 | 		MutablePropertySources bootstrapProperties = bootstrapEnvironment.getPropertySources();
  154 | 		String configLocation = environment.resolvePlaceholders("${spring.cloud.bootstrap.location:}");
  155 | 		String configAdditionalLocation = environment
  156 | 			.resolvePlaceholders("${spring.cloud.bootstrap.additional-location:}");
  157 | 		Map<String, Object> bootstrapMap = new HashMap<>();
  158 | 		bootstrapMap.put("spring.config.name", configName);
  159 | 		// if an app (or test) uses spring.main.web-application-type=reactive, bootstrap
  160 | 		// will fail
  161 | 		// force the environment to use none, because if though it is set below in the
  162 | 		// builder
  163 | 		// the environment overrides it
  164 | 		bootstrapMap.put("spring.main.web-application-type", "none");
  165 | 		if (StringUtils.hasText(configLocation)) {
  166 | 			bootstrapMap.put("spring.config.location", configLocation);
  167 | 		}
  168 | 		if (StringUtils.hasText(configAdditionalLocation)) {
  169 | 			bootstrapMap.put("spring.config.additional-location", configAdditionalLocation);
  170 | 		}
  171 | 		bootstrapProperties.addFirst(new MapPropertySource(BOOTSTRAP_PROPERTY_SOURCE_NAME, bootstrapMap));
  172 | 		for (PropertySource<?> source : environment.getPropertySources()) {
  173 | 			if (source instanceof StubPropertySource) {
  174 | 				continue;
  175 | 			}
  176 | 			bootstrapProperties.addLast(source);
  177 | 		}
  178 | 		// TODO: is it possible or sensible to share a ResourceLoader?
  179 | 		SpringApplicationBuilder builder = new SpringApplicationBuilder().profiles(environment.getActiveProfiles())
  180 | 			.bannerMode(Mode.OFF)
  181 | 			.environment(bootstrapEnvironment)
  182 | 			// Don't use the default properties in this builder
  183 | 			.registerShutdownHook(false)
  184 | 			.logStartupInfo(false)
  185 | 			.web(WebApplicationType.NONE);
  186 | 		final SpringApplication builderApplication = builder.application();
  187 | 		if (builderApplication.getMainApplicationClass() == null && application.getMainApplicationClass() != null) {
  188 | 			// gh_425:
  189 | 			// SpringApplication cannot deduce the MainApplicationClass here
  190 | 			// if it is booted from SpringBootServletInitializer due to the
  191 | 			// absense of the "main" method in stackTraces.
  192 | 			// But luckily this method's second parameter "application" here
  193 | 			// carries the real MainApplicationClass which has been explicitly
  194 | 			// set by SpringBootServletInitializer itself already.
  195 | 			builder.main(application.getMainApplicationClass());
  196 | 		}
  197 | 		if (environment.getPropertySources().contains("refreshArgs")) {
  198 | 			// If we are doing a context refresh, really we only want to refresh the
  199 | 			// Environment, and there are some toxic listeners (like the
  200 | 			// LoggingApplicationListener) that affect global static state, so we need a
  201 | 			// way to switch those off.
  202 | 			builderApplication.setListeners(filterListeners(builderApplication.getListeners()));
  203 | 		}
  204 | 		builder.sources(BootstrapImportSelectorConfiguration.class);
  205 | 		final ConfigurableApplicationContext context = builder.run();
  206 | 		// gh-214 using spring.application.name=bootstrap to set the context id via
  207 | 		// `ContextIdApplicationContextInitializer` prevents apps from getting the actual
  208 | 		// spring.application.name
  209 | 		// during the bootstrap phase.
  210 | 		context.setId("bootstrap");
  211 | 		// Make the bootstrap context a parent of the app context
  212 | 		addAncestorInitializer(application, context);
  213 | 		// It only has properties in it now that we don't want in the parent so remove
  214 | 		// it (and it will be added back later)
  215 | 		bootstrapProperties.remove(BOOTSTRAP_PROPERTY_SOURCE_NAME);
  216 | 		mergeDefaultProperties(environment.getPropertySources(), bootstrapProperties);
  217 | 		return context;
  218 | 	}
  219 | 
  220 | 	private Collection<? extends ApplicationListener<?>> filterListeners(Set<ApplicationListener<?>> listeners) {
  221 | 		Set<ApplicationListener<?>> result = new LinkedHashSet<>();
  222 | 		for (ApplicationListener<?> listener : listeners) {
  223 | 			if (!(listener instanceof LoggingApplicationListener)
  224 | 					&& !(listener instanceof LoggingSystemShutdownListener)) {
  225 | 				result.add(listener);
  226 | 			}
  227 | 		}
  228 | 		return result;
  229 | 	}
  230 | 
  231 | 	private void mergeDefaultProperties(MutablePropertySources environment, MutablePropertySources bootstrap) {
  232 | 		String name = DEFAULT_PROPERTIES;
  233 | 		if (bootstrap.contains(name)) {
  234 | 			PropertySource<?> source = bootstrap.get(name);
  235 | 			if (!environment.contains(name) && source != null) {
  236 | 				environment.addLast(source);
  237 | 			}
  238 | 			else {
  239 | 				PropertySource<?> target = environment.get(name);
  240 | 				if (target instanceof MapPropertySource targetMapPropertySource && target != source
  241 | 						&& source instanceof MapPropertySource sourceMapPropertySource) {
  242 | 					Map<String, Object> targetMap = targetMapPropertySource.getSource();
  243 | 					Map<String, Object> map = sourceMapPropertySource.getSource();
  244 | 					for (String key : map.keySet()) {
  245 | 						if (!target.containsProperty(key)) {
  246 | 							targetMap.put(key, map.get(key));
  247 | 						}
  248 | 					}
  249 | 				}
  250 | 			}
  251 | 		}
  252 | 		mergeAdditionalPropertySources(environment, bootstrap);
  253 | 	}
  254 | 
  255 | 	private void mergeAdditionalPropertySources(MutablePropertySources environment, MutablePropertySources bootstrap) {
  256 | 		PropertySource<?> defaultProperties = environment.get(DEFAULT_PROPERTIES);
  257 | 		if (defaultProperties == null) {
  258 | 			defaultProperties = new MapPropertySource(DEFAULT_PROPERTIES, new LinkedHashMap<>());
  259 | 		}
  260 | 		ExtendedDefaultPropertySource result = defaultProperties instanceof ExtendedDefaultPropertySource
  261 | 				? (ExtendedDefaultPropertySource) defaultProperties
  262 | 				: new ExtendedDefaultPropertySource(DEFAULT_PROPERTIES, defaultProperties);
  263 | 		for (PropertySource<?> source : bootstrap) {
  264 | 			if (!environment.contains(source.getName())) {
  265 | 				result.add(source);
  266 | 			}
  267 | 		}
  268 | 		for (String name : result.getPropertySourceNames()) {
  269 | 			bootstrap.remove(name);
  270 | 		}
  271 | 		addOrReplace(environment, result);
  272 | 		addOrReplace(bootstrap, result);
  273 | 	}
  274 | 
  275 | 	private void addOrReplace(MutablePropertySources environment, PropertySource<?> result) {
  276 | 		if (environment.contains(result.getName())) {
  277 | 			environment.replace(result.getName(), result);
  278 | 		}
  279 | 		else {
  280 | 			environment.addLast(result);
  281 | 		}
  282 | 	}
  283 | 
  284 | 	private void addAncestorInitializer(SpringApplication application, ConfigurableApplicationContext context) {
  285 | 		boolean installed = false;
  286 | 		for (ApplicationContextInitializer<?> initializer : application.getInitializers()) {
  287 | 			if (initializer instanceof AncestorInitializer ancestorInitializer) {
  288 | 				installed = true;
  289 | 				// New parent
  290 | 				ancestorInitializer.setParent(context);
  291 | 			}
  292 | 		}
  293 | 		if (!installed) {
  294 | 			application.addInitializers(new AncestorInitializer(context));
  295 | 		}
  296 | 
  297 | 	}
  298 | 
  299 | 	@SuppressWarnings("unchecked")
  300 | 	private void apply(ConfigurableApplicationContext context, SpringApplication application,
  301 | 			ConfigurableEnvironment environment) {
  302 | 		if (application.getAllSources().contains(BootstrapMarkerConfiguration.class)) {
  303 | 			return;
  304 | 		}
  305 | 		application.addPrimarySources(List.of(BootstrapMarkerConfiguration.class));
  306 | 		@SuppressWarnings("rawtypes")
  307 | 		Set target = new LinkedHashSet<>(application.getInitializers());
  308 | 		target.addAll(getOrderedBeansOfType(context, ApplicationContextInitializer.class));
  309 | 		application.setInitializers(target);
  310 | 		addBootstrapDecryptInitializer(application);
  311 | 
  312 | 		// Get the active profiles from the bootstrap context and set them in main
  313 | 		// application
  314 | 		// environment. This allows any profiles activated during bootstrap to be
  315 | 		// activated when
  316 | 		// config data runs in the main application context.
  317 | 		environment.setActiveProfiles(context.getEnvironment().getActiveProfiles());
  318 | 	}
  319 | 
  320 | 	@SuppressWarnings("unchecked")
  321 | 	private void addBootstrapDecryptInitializer(SpringApplication application) {
  322 | 		DelegatingEnvironmentDecryptApplicationInitializer decrypter = null;
  323 | 		Set<ApplicationContextInitializer<?>> initializers = new LinkedHashSet<>();
  324 | 		for (ApplicationContextInitializer<?> ini : application.getInitializers()) {
  325 | 			if (ini instanceof EnvironmentDecryptApplicationInitializer) {
  326 | 				@SuppressWarnings("rawtypes")
  327 | 				ApplicationContextInitializer del = ini;
  328 | 				decrypter = new DelegatingEnvironmentDecryptApplicationInitializer(del);
  329 | 				initializers.add(ini);
  330 | 				initializers.add(decrypter);
  331 | 			}
  332 | 			else if (ini instanceof DelegatingEnvironmentDecryptApplicationInitializer) {
  333 | 				// do nothing
  334 | 			}
  335 | 			else {
  336 | 				initializers.add(ini);
  337 | 			}
  338 | 		}
  339 | 		ArrayList<ApplicationContextInitializer<?>> target = new ArrayList<>(initializers);
  340 | 		application.setInitializers(target);
  341 | 	}
  342 | 
  343 | 	private <T> List<T> getOrderedBeansOfType(ListableBeanFactory context, Class<T> type) {
  344 | 		List<T> result = new ArrayList<>();
  345 | 		for (String name : context.getBeanNamesForType(type)) {
  346 | 			result.add(context.getBean(name, type));
  347 | 		}
  348 | 		AnnotationAwareOrderComparator.sort(result);
  349 | 		return result;
  350 | 	}
  351 | 
  352 | 	@Override
  353 | 	public int getOrder() {
  354 | 		return this.order;
  355 | 	}
  356 | 
  357 | 	public void setOrder(int order) {
  358 | 		this.order = order;
  359 | 	}
  360 | 
  361 | 	private static class BootstrapMarkerConfiguration {
  362 | 
  363 | 	}
  364 | 
  365 | 	private static class AncestorInitializer
  366 | 			implements ApplicationContextInitializer<ConfigurableApplicationContext>, Ordered {
  367 | 
  368 | 		private ConfigurableApplicationContext parent;
  369 | 
  370 | 		AncestorInitializer(ConfigurableApplicationContext parent) {
  371 | 			this.parent = parent;
  372 | 		}
  373 | 
  374 | 		public void setParent(ConfigurableApplicationContext parent) {
  375 | 			this.parent = parent;
  376 | 		}
  377 | 
  378 | 		@Override
  379 | 		public int getOrder() {
  380 | 			// Need to run not too late (so not unordered), so that, for instance, the
  381 | 			// ContextIdApplicationContextInitializer runs later and picks up the merged
  382 | 			// Environment. Also needs to be quite early so that other initializers can
  383 | 			// pick up the parent (especially the Environment).
  384 | 			return Ordered.HIGHEST_PRECEDENCE + 5;
  385 | 		}
  386 | 
  387 | 		@Override
  388 | 		public void initialize(ConfigurableApplicationContext context) {
  389 | 			while (context.getParent() != null && context.getParent() != context) {
  390 | 				context = (ConfigurableApplicationContext) context.getParent();
  391 | 			}
  392 | 			reorderSources(context.getEnvironment());
  393 | 			new ParentContextApplicationContextInitializer(this.parent).initialize(context);
  394 | 		}
  395 | 
  396 | 		private void reorderSources(ConfigurableEnvironment environment) {
  397 | 			PropertySource<?> removed = environment.getPropertySources().remove(DEFAULT_PROPERTIES);
  398 | 			if (removed instanceof ExtendedDefaultPropertySource defaultProperties) {
  399 | 				environment.getPropertySources()
  400 | 					.addLast(new MapPropertySource(DEFAULT_PROPERTIES, defaultProperties.getSource()));
  401 | 				for (PropertySource<?> source : defaultProperties.getPropertySources().getPropertySources()) {
  402 | 					if (!environment.getPropertySources().contains(source.getName())) {
  403 | 						environment.getPropertySources().addBefore(DEFAULT_PROPERTIES, source);
  404 | 					}
  405 | 				}
  406 | 			}
  407 | 		}
  408 | 
  409 | 	}
  410 | 
  411 | 	/**
  412 | 	 * A special initializer designed to run before the property source bootstrap and
  413 | 	 * decrypt any properties needed there (e.g. URL of config server).
  414 | 	 */
  415 | 	@Order(Ordered.HIGHEST_PRECEDENCE + 9)
  416 | 	private static class DelegatingEnvironmentDecryptApplicationInitializer
  417 | 			implements ApplicationContextInitializer<ConfigurableApplicationContext> {
  418 | 
  419 | 		private ApplicationContextInitializer<ConfigurableApplicationContext> delegate;
  420 | 
  421 | 		DelegatingEnvironmentDecryptApplicationInitializer(
  422 | 				ApplicationContextInitializer<ConfigurableApplicationContext> delegate) {
  423 | 			this.delegate = delegate;
  424 | 		}
  425 | 
  426 | 		@Override
  427 | 		public void initialize(ConfigurableApplicationContext applicationContext) {
  428 | 			this.delegate.initialize(applicationContext);
  429 | 		}
  430 | 
  431 | 	}
  432 | 
  433 | 	private static class ExtendedDefaultPropertySource extends SystemEnvironmentPropertySource
  434 | 			implements OriginLookup<String> {
  435 | 
  436 | 		private final OriginTrackedCompositePropertySource sources;
  437 | 
  438 | 		private final List<String> names = new ArrayList<>();
  439 | 
  440 | 		ExtendedDefaultPropertySource(String name, PropertySource<?> propertySource) {
  441 | 			super(name, findMap(propertySource));
  442 | 			this.sources = new OriginTrackedCompositePropertySource(name);
  443 | 		}
  444 | 
  445 | 		@SuppressWarnings("unchecked")
  446 | 		private static Map<String, Object> findMap(PropertySource<?> propertySource) {
  447 | 			if (propertySource instanceof MapPropertySource) {
  448 | 				return (Map<String, Object>) propertySource.getSource();
  449 | 			}
  450 | 			return new LinkedHashMap<>();
  451 | 		}
  452 | 
  453 | 		public CompositePropertySource getPropertySources() {
  454 | 			return this.sources;
  455 | 		}
  456 | 
  457 | 		public List<String> getPropertySourceNames() {
  458 | 			return this.names;
  459 | 		}
  460 | 
  461 | 		public void add(PropertySource<?> source) {
  462 | 			// Only add map property sources added by boot, see gh-476
  463 | 			if (source instanceof OriginTrackedMapPropertySource && !this.names.contains(source.getName())) {
  464 | 				this.sources.addPropertySource(source);
  465 | 				this.names.add(source.getName());
  466 | 			}
  467 | 		}
  468 | 
  469 | 		@Override
  470 | 		public @Nullable Object getProperty(String name) {
  471 | 			if (this.sources.containsProperty(name)) {
  472 | 				return this.sources.getProperty(name);
  473 | 			}
  474 | 			return super.getProperty(name);
  475 | 		}
  476 | 
  477 | 		@Override
  478 | 		public boolean containsProperty(String name) {
  479 | 			if (this.sources.containsProperty(name)) {
  480 | 				return true;
  481 | 			}
  482 | 			return super.containsProperty(name);
  483 | 		}
  484 | 
  485 | 		@Override
  486 | 		public String[] getPropertyNames() {
  487 | 			List<String> names = new ArrayList<>();
  488 | 			names.addAll(Arrays.asList(this.sources.getPropertyNames()));
  489 | 			names.addAll(Arrays.asList(super.getPropertyNames()));
  490 | 			return names.toArray(new String[0]);
  491 | 		}
  492 | 
  493 | 		@Override
  494 | 		public @Nullable Origin getOrigin(String name) {
  495 | 			return this.sources.getOrigin(name);
  496 | 		}
  497 | 
  498 | 	}
  499 | 
  500 | 	private static class CloseContextOnFailureApplicationListener implements SmartApplicationListener {
  501 | 
  502 | 		private final ConfigurableApplicationContext context;
  503 | 
  504 | 		CloseContextOnFailureApplicationListener(ConfigurableApplicationContext context) {
  505 | 			this.context = context;
  506 | 		}
  507 | 
  508 | 		@Override
  509 | 		public boolean supportsEventType(Class<? extends ApplicationEvent> eventType) {
  510 | 			return ApplicationFailedEvent.class.isAssignableFrom(eventType);
  511 | 		}
  512 | 
  513 | 		@Override
  514 | 		public void onApplicationEvent(ApplicationEvent event) {
  515 | 			if (event instanceof ApplicationFailedEvent) {
  516 | 				this.context.close();
  517 | 			}
  518 | 
  519 | 		}
  520 | 
  521 | 	}
  522 | 
  523 | }
```
