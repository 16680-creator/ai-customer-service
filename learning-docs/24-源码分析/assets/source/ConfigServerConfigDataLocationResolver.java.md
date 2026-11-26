<!-- learning-docs source archive（机械抓取原文，禁止手改代码内容） -->
<!-- origin: org.springframework.cloud:spring-cloud-config-client:5.1.0 | path: org/springframework/cloud/config/client/ConfigServerConfigDataLocationResolver.java -->
<!-- 正文引用：[[源码:ConfigServerConfigDataLocationResolver.java#L起-L止]] ；代码块头部：(src: ConfigServerConfigDataLocationResolver.java#L起-L止) -->

# ConfigServerConfigDataLocationResolver.java

| 项 | 值 |
| --- | --- |
| 来源 | `org.springframework.cloud:spring-cloud-config-client:5.1.0` |
| 路径 | `org/springframework/cloud/config/client/ConfigServerConfigDataLocationResolver.java` |
| 行数 | 396 |
| 在线地址 | https://repo1.maven.org/maven2/org/springframework/cloud/spring-cloud-config-client/5.1.0/spring-cloud-config-client-5.1.0-sources.jar |
| 抓取时间 | 2026-11-25T15:31:49.610Z |

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
   17 | package org.springframework.cloud.config.client;
   18 | 
   19 | import java.util.ArrayList;
   20 | import java.util.List;
   21 | import java.util.Properties;
   22 | import java.util.function.Supplier;
   23 | 
   24 | import org.apache.commons.logging.Log;
   25 | 
   26 | import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;
   27 | import org.springframework.boot.bootstrap.BootstrapContextClosedEvent;
   28 | import org.springframework.boot.bootstrap.BootstrapRegistry;
   29 | import org.springframework.boot.bootstrap.BootstrapRegistry.InstanceSupplier;
   30 | import org.springframework.boot.bootstrap.ConfigurableBootstrapContext;
   31 | import org.springframework.boot.context.config.ConfigDataLocation;
   32 | import org.springframework.boot.context.config.ConfigDataLocationNotFoundException;
   33 | import org.springframework.boot.context.config.ConfigDataLocationResolver;
   34 | import org.springframework.boot.context.config.ConfigDataLocationResolverContext;
   35 | import org.springframework.boot.context.config.ConfigDataResourceNotFoundException;
   36 | import org.springframework.boot.context.config.Profiles;
   37 | import org.springframework.boot.context.properties.PropertyMapper;
   38 | import org.springframework.boot.context.properties.bind.BindHandler;
   39 | import org.springframework.boot.context.properties.bind.Bindable;
   40 | import org.springframework.boot.context.properties.bind.Binder;
   41 | import org.springframework.boot.logging.DeferredLogFactory;
   42 | import org.springframework.cloud.bootstrap.encrypt.KeyProperties;
   43 | import org.springframework.cloud.bootstrap.encrypt.RsaProperties;
   44 | import org.springframework.cloud.bootstrap.encrypt.TextEncryptorUtils;
   45 | import org.springframework.cloud.client.ServiceInstance;
   46 | import org.springframework.cloud.config.client.oauth2.ConfigClientOAuth2Support;
   47 | import org.springframework.cloud.config.client.validation.ApplicationNameValidator;
   48 | import org.springframework.cloud.context.encrypt.EncryptorFactory;
   49 | import org.springframework.core.Ordered;
   50 | import org.springframework.core.log.LogMessage;
   51 | import org.springframework.core.retry.RetryTemplate;
   52 | import org.springframework.security.crypto.encrypt.TextEncryptor;
   53 | import org.springframework.util.ClassUtils;
   54 | import org.springframework.util.ObjectUtils;
   55 | import org.springframework.util.StringUtils;
   56 | import org.springframework.web.client.RestClient;
   57 | import org.springframework.web.client.RestTemplate;
   58 | 
   59 | import static org.springframework.cloud.config.client.ConfigClientProperties.CONFIG_DISCOVERY_ENABLED;
   60 | 
   61 | public class ConfigServerConfigDataLocationResolver
   62 | 		implements ConfigDataLocationResolver<ConfigServerConfigDataResource>, Ordered {
   63 | 
   64 | 	/**
   65 | 	 * Prefix for Config Server imports.
   66 | 	 */
   67 | 	public static final String PREFIX = "configserver:";
   68 | 
   69 | 	/**
   70 | 	 * Name of the {@link ConfigClientProperties} bean resolved by the config server
   71 | 	 * import.
   72 | 	 */
   73 | 	public static final String PROPERTIES_BEAN_NAME = "configDataConfigClientProperties";
   74 | 
   75 | 	static final boolean RSA_IS_PRESENT = ClassUtils
   76 | 		.isPresent("org.springframework.security.crypto.encrypt.RsaSecretEncryptor", null);
   77 | 
   78 | 	private final Log log;
   79 | 
   80 | 	public ConfigServerConfigDataLocationResolver(DeferredLogFactory factory) {
   81 | 		this.log = factory.getLog(ConfigServerConfigDataLocationResolver.class);
   82 | 	}
   83 | 
   84 | 	@Override
   85 | 	public int getOrder() {
   86 | 		return -1;
   87 | 	}
   88 | 
   89 | 	/*
   90 | 	 * Depending on whether encrypt.key is set as an environment or system property we may
   91 | 	 * have created a TextEncryptor implementation or just created a
   92 | 	 * FailsafeTextEncryptor. This is because when TextEncryptorConfigBootstrapper runs we
   93 | 	 * have not yet loaded any configuration files (application.yaml | properties etc).
   94 | 	 * However, at this point when the ConfigServerConfigDataLocationResolver is resolving
   95 | 	 * configuration we would have resolved the configuration files on the classpath at
   96 | 	 * least so we can potentially create a properly configured TextEncryptor. So if the
   97 | 	 * FailsafeTextEncryptor is in the context and we can create a TextEncryptor then we
   98 | 	 * set the delegate in the FailsafeTextEncryptor so that we can decrypt any encrypted
   99 | 	 * properties at this point.
  100 | 	 */
  101 | 	protected void setTextEncryptorDelegate(ConfigDataLocationResolverContext context) {
  102 | 		if (context.getBootstrapContext().isRegistered(TextEncryptor.class)) {
  103 | 			Binder binder = context.getBinder();
  104 | 			KeyProperties keyProperties = binder.bindOrCreate(KeyProperties.PREFIX, Bindable.of(KeyProperties.class));
  105 | 			boolean textEncryptorRegistered = context.getBootstrapContext().isRegistered(TextEncryptor.class);
  106 | 			if (TextEncryptorUtils.keysConfigured(keyProperties) && textEncryptorRegistered) {
  107 | 				TextEncryptor textEncryptor = context.getBootstrapContext().get(TextEncryptor.class);
  108 | 				if (textEncryptor instanceof TextEncryptorUtils.FailsafeTextEncryptor failsafeTextEncryptor) {
  109 | 					TextEncryptor delegate;
  110 | 					if (RSA_IS_PRESENT) {
  111 | 						RsaProperties rsaProperties = binder.bindOrCreate(RsaProperties.PREFIX,
  112 | 								Bindable.of(RsaProperties.class));
  113 | 						delegate = TextEncryptorUtils.createTextEncryptor(keyProperties, rsaProperties);
  114 | 					}
  115 | 					else {
  116 | 						delegate = new EncryptorFactory(keyProperties.getSalt()).create(keyProperties.getKey());
  117 | 					}
  118 | 					failsafeTextEncryptor.setDelegate(delegate);
  119 | 				}
  120 | 			}
  121 | 		}
  122 | 	}
  123 | 
  124 | 	protected PropertyHolder loadProperties(ConfigDataLocationResolverContext context, String uris) {
  125 | 		Binder binder = context.getBinder();
  126 | 		BindHandler bindHandler = getBindHandler(context);
  127 | 
  128 | 		ConfigClientProperties configClientProperties;
  129 | 		if (context.getBootstrapContext().isRegistered(ConfigClientProperties.class)) {
  130 | 			configClientProperties = binder
  131 | 				.bind(ConfigClientProperties.PREFIX, Bindable.of(ConfigClientProperties.class), bindHandler)
  132 | 				.orElseGet(ConfigClientProperties::new);
  133 | 			boolean discoveryEnabled = context.getBinder()
  134 | 				.bind(CONFIG_DISCOVERY_ENABLED, Bindable.of(Boolean.class), getBindHandler(context))
  135 | 				.orElse(false);
  136 | 			// In the case where discovery is enabled we need to extract the config server
  137 | 			// uris, username, and password
  138 | 			// from the ConfigServiceMonitor. These are set in
  139 | 			// ConfigServerInstanceMonitor.refresh which will only
  140 | 			// be called the first time we fetch configuration.
  141 | 			// They will continue to be updated as HeartbeatEvents are received.
  142 | 			if (discoveryEnabled) {
  143 | 				ConfigServerInstanceMonitor instanceMonitor = context.getBootstrapContext()
  144 | 					.get(ConfigServerInstanceMonitor.class);
  145 | 
  146 | 				configClientProperties.setUri(instanceMonitor.getUri());
  147 | 				configClientProperties.setPassword(instanceMonitor.getPassword());
  148 | 				configClientProperties.setUsername(instanceMonitor.getUsername());
  149 | 			}
  150 | 		}
  151 | 		else {
  152 | 			configClientProperties = binder
  153 | 				.bind(ConfigClientProperties.PREFIX, Bindable.of(ConfigClientProperties.class), bindHandler)
  154 | 				.orElseGet(ConfigClientProperties::new);
  155 | 		}
  156 | 		if (!StringUtils.hasText(configClientProperties.getName())
  157 | 				|| "application".equals(configClientProperties.getName())) {
  158 | 			// default to spring.application.name if name isn't set
  159 | 			String applicationName = binder.bind("spring.application.name", Bindable.of(String.class), bindHandler)
  160 | 				.orElse("application");
  161 | 			configClientProperties.setName(applicationName);
  162 | 		}
  163 | 
  164 | 		PropertyHolder holder = new PropertyHolder();
  165 | 		holder.properties = configClientProperties;
  166 | 		// bind retry, override later
  167 | 		holder.retryProperties = binder.bind(RetryProperties.PREFIX, RetryProperties.class)
  168 | 			.orElseGet(RetryProperties::new);
  169 | 
  170 | 		if (StringUtils.hasText(uris)) {
  171 | 			String[] uri = StringUtils.commaDelimitedListToStringArray(uris);
  172 | 			String paramStr = null;
  173 | 			for (int i = 0; i < uri.length; i++) {
  174 | 				int paramIdx = uri[i].indexOf('?');
  175 | 				if (paramIdx > 0) {
  176 | 					if (i == 0) {
  177 | 						// only gather params from first uri
  178 | 						paramStr = uri[i].substring(paramIdx + 1);
  179 | 					}
  180 | 					uri[i] = uri[i].substring(0, paramIdx);
  181 | 				}
  182 | 			}
  183 | 			if (StringUtils.hasText(paramStr)) {
  184 | 				Properties properties = StringUtils
  185 | 					.splitArrayElementsIntoProperties(StringUtils.delimitedListToStringArray(paramStr, "&"), "=");
  186 | 				if (properties != null) {
  187 | 					PropertyMapper map = PropertyMapper.get();
  188 | 					map.from(() -> properties.getProperty("fail-fast"))
  189 | 						.as(Boolean::valueOf)
  190 | 						.to(configClientProperties::setFailFast);
  191 | 					map.from(() -> properties.getProperty("max-attempts"))
  192 | 						.as(Integer::valueOf)
  193 | 						.to(holder.retryProperties::setMaxAttempts);
  194 | 					map.from(() -> properties.getProperty("max-interval"))
  195 | 						.as(Long::valueOf)
  196 | 						.to(holder.retryProperties::setMaxInterval);
  197 | 					map.from(() -> properties.getProperty("multiplier"))
  198 | 						.as(Double::valueOf)
  199 | 						.to(holder.retryProperties::setMultiplier);
  200 | 					map.from(() -> properties.getProperty("initial-interval"))
  201 | 						.as(Long::valueOf)
  202 | 						.to(holder.retryProperties::setInitialInterval);
  203 | 				}
  204 | 			}
  205 | 			configClientProperties.setUri(uri);
  206 | 		}
  207 | 		return holder;
  208 | 	}
  209 | 
  210 | 	private BindHandler getBindHandler(ConfigDataLocationResolverContext context) {
  211 | 		return context.getBootstrapContext().getOrElse(BindHandler.class, null);
  212 | 	}
  213 | 
  214 | 	protected Log getLog() {
  215 | 		return this.log;
  216 | 	}
  217 | 
  218 | 	@Override
  219 | 	public boolean isResolvable(ConfigDataLocationResolverContext context, ConfigDataLocation location) {
  220 | 		if (!location.hasPrefix(getPrefix())) {
  221 | 			return false;
  222 | 		}
  223 | 		return context.getBinder().bind(ConfigClientProperties.PREFIX + ".enabled", Boolean.class).orElse(true);
  224 | 	}
  225 | 
  226 | 	protected String getPrefix() {
  227 | 		return PREFIX;
  228 | 	}
  229 | 
  230 | 	@Override
  231 | 	public List<ConfigServerConfigDataResource> resolve(ConfigDataLocationResolverContext context,
  232 | 			ConfigDataLocation location)
  233 | 			throws ConfigDataLocationNotFoundException, ConfigDataResourceNotFoundException {
  234 | 		return resolveProfileSpecific(context, location, null);
  235 | 	}
  236 | 
  237 | 	@Override
  238 | 	public List<ConfigServerConfigDataResource> resolveProfileSpecific(
  239 | 			ConfigDataLocationResolverContext resolverContext, ConfigDataLocation location, Profiles profiles)
  240 | 			throws ConfigDataLocationNotFoundException {
  241 | 		setTextEncryptorDelegate(resolverContext);
  242 | 		String uris = location.getNonPrefixedValue(getPrefix());
  243 | 		PropertyHolder propertyHolder = loadProperties(resolverContext, uris);
  244 | 		ConfigClientProperties properties = propertyHolder.properties;
  245 | 
  246 | 		if (!ApplicationNameValidator.validate(properties, log)) {
  247 | 			return new ArrayList<>();
  248 | 		}
  249 | 		ConfigurableBootstrapContext bootstrapContext = resolverContext.getBootstrapContext();
  250 | 		bootstrapContext.register(ConfigClientProperties.class,
  251 | 				InstanceSupplier.of(properties).withScope(BootstrapRegistry.Scope.PROTOTYPE));
  252 | 		bootstrapContext.addCloseListener(event -> event.getApplicationContext()
  253 | 			.getBeanFactory()
  254 | 			.registerSingleton(PROPERTIES_BEAN_NAME, event.getBootstrapContext().get(ConfigClientProperties.class)));
  255 | 
  256 | 		bootstrapContext.registerIfAbsent(ConfigClientRequestTemplateFactory.class, context -> {
  257 | 			ConfigClientRequestTemplateFactory factory = new ConfigClientRequestTemplateFactory(log,
  258 | 					context.get(ConfigClientProperties.class));
  259 | 			ConfigClientOAuth2Support.registerInterceptor(bootstrapContext, resolverContext.getBinder(),
  260 | 					getBindHandler(resolverContext), factory);
  261 | 			return factory;
  262 | 		});
  263 | 
  264 | 		bootstrapContext.registerIfAbsent(RestTemplate.class, context -> {
  265 | 			ConfigClientRequestTemplateFactory factory = context.get(ConfigClientRequestTemplateFactory.class);
  266 | 			return factory.create();
  267 | 		});
  268 | 		bootstrapContext.addCloseListener(this::registerRestClientHolder);
  269 | 
  270 | 		bootstrapContext.registerIfAbsent(PropertyResolver.class,
  271 | 				context -> new PropertyResolver(resolverContext.getBinder(), getBindHandler(resolverContext)));
  272 | 
  273 | 		ConfigServerConfigDataResource resource = new ConfigServerConfigDataResource(properties, location.isOptional(),
  274 | 				profiles);
  275 | 		resource.setProfileSpecific(!ObjectUtils.isEmpty(profiles));
  276 | 		resource.setLog(log);
  277 | 		resource.setRetryProperties(propertyHolder.retryProperties);
  278 | 
  279 | 		boolean discoveryEnabled = resolverContext.getBinder()
  280 | 			.bind(CONFIG_DISCOVERY_ENABLED, Bindable.of(Boolean.class), getBindHandler(resolverContext))
  281 | 			.orElse(false);
  282 | 
  283 | 		boolean retryEnabled = resolverContext.getBinder()
  284 | 			.bind(ConfigClientProperties.PREFIX + ".fail-fast", Bindable.of(Boolean.class),
  285 | 					getBindHandler(resolverContext))
  286 | 			.orElse(false) && propertyHolder.retryProperties.isEnabled();
  287 | 
  288 | 		if (discoveryEnabled) {
  289 | 			log.debug(LogMessage.format("discovery enabled"));
  290 | 			// register ConfigServerInstanceMonitor
  291 | 			bootstrapContext.registerIfAbsent(ConfigServerInstanceMonitor.class, context -> {
  292 | 				ConfigServerInstanceProvider.Function function = context
  293 | 					.get(ConfigServerInstanceProvider.Function.class);
  294 | 
  295 | 				ConfigServerInstanceProvider instanceProvider;
  296 | 				if (retryEnabled) {
  297 | 					log.debug(LogMessage.format("discovery plus retry enabled"));
  298 | 					RetryTemplate retryTemplate = RetryTemplateFactory.create(propertyHolder.retryProperties, log);
  299 | 					instanceProvider = new ConfigServerInstanceProvider(function, resolverContext.getBinder(),
  300 | 							getBindHandler(resolverContext)) {
  301 | 						@Override
  302 | 						public List<ServiceInstance> getConfigServerInstances(String serviceId) {
  303 | 							return retryTemplate.invoke(() -> super.getConfigServerInstances(serviceId));
  304 | 						}
  305 | 					};
  306 | 				}
  307 | 				else {
  308 | 					instanceProvider = new ConfigServerInstanceProvider(function, resolverContext.getBinder(),
  309 | 							getBindHandler(resolverContext));
  310 | 				}
  311 | 				instanceProvider.setLog(log);
  312 | 
  313 | 				ConfigClientProperties clientProperties = context.get(ConfigClientProperties.class);
  314 | 				ConfigServerInstanceMonitor instanceMonitor = new ConfigServerInstanceMonitor(log, clientProperties,
  315 | 						instanceProvider);
  316 | 				instanceMonitor.setRefreshOnStartup(false);
  317 | 				instanceMonitor.refresh();
  318 | 				return instanceMonitor;
  319 | 			});
  320 | 			// promote ConfigServerInstanceMonitor to bean so updates can be made to
  321 | 			// config client uri
  322 | 			bootstrapContext.addCloseListener(event -> {
  323 | 				ConfigServerInstanceMonitor configServerInstanceMonitor = event.getBootstrapContext()
  324 | 					.get(ConfigServerInstanceMonitor.class);
  325 | 				event.getApplicationContext()
  326 | 					.getBeanFactory()
  327 | 					.registerSingleton("configServerInstanceMonitor", configServerInstanceMonitor);
  328 | 			});
  329 | 		}
  330 | 
  331 | 		List<ConfigServerConfigDataResource> locations = new ArrayList<>();
  332 | 		locations.add(resource);
  333 | 		return locations;
  334 | 	}
  335 | 
  336 | 	/**
  337 | 	 * Keep the {@link RestTemplate} that loaded the configuration, which carries the URI,
  338 | 	 * credentials, headers, TLS settings, timeouts and interceptors of the client,
  339 | 	 * available as a {@link RestClient} to the {@link ConfigResourceClient}, without
  340 | 	 * exposing it as a {@link RestClient} bean.
  341 | 	 */
  342 | 	private void registerRestClientHolder(BootstrapContextClosedEvent event) {
  343 | 		ConfigurableListableBeanFactory beanFactory = event.getApplicationContext().getBeanFactory();
  344 | 		// The listener is added on every resolution (one per import location and
  345 | 		// profile pass) and runs that many times when the bootstrap context closes,
  346 | 		// while a singleton can only be registered once.
  347 | 		if (beanFactory.containsBean(ConfigServerRestClientHolder.BEAN_NAME)
  348 | 				|| !event.getBootstrapContext().isRegistered(RestTemplate.class)) {
  349 | 			return;
  350 | 		}
  351 | 		try {
  352 | 			RestClient restClient = RestClient.create(event.getBootstrapContext().get(RestTemplate.class));
  353 | 			beanFactory.registerSingleton(ConfigServerRestClientHolder.BEAN_NAME,
  354 | 					new ConfigServerRestClientHolder(restClient));
  355 | 		}
  356 | 		catch (RuntimeException ex) {
  357 | 			// An optional component must not prevent the application from starting
  358 | 			this.log.warn("Could not build a RestClient from the config client RestTemplate; "
  359 | 					+ "the ConfigResourceClient will not be available", ex);
  360 | 		}
  361 | 	}
  362 | 
  363 | 	public static class PropertyResolver {
  364 | 
  365 | 		private final Binder binder;
  366 | 
  367 | 		private final BindHandler bindHandler;
  368 | 
  369 | 		public PropertyResolver(Binder binder, BindHandler bindHandler) {
  370 | 			this.binder = binder;
  371 | 			this.bindHandler = bindHandler;
  372 | 		}
  373 | 
  374 | 		public <T> T get(String key, Class<T> type, T defaultValue) {
  375 | 			return binder.bind(key, Bindable.of(type)).orElse(defaultValue);
  376 | 		}
  377 | 
  378 | 		public <T> T resolveConfigurationProperties(String prefix, Class<T> type, Supplier<T> defaultValue) {
  379 | 			return binder.bind(prefix, Bindable.of(type), bindHandler).orElseGet(defaultValue);
  380 | 		}
  381 | 
  382 | 		public <T> T resolveOrCreateConfigurationProperties(String prefix, Class<T> type) {
  383 | 			return binder.bindOrCreate(prefix, Bindable.of(type), bindHandler);
  384 | 		}
  385 | 
  386 | 	}
  387 | 
  388 | 	private static final class PropertyHolder {
  389 | 
  390 | 		ConfigClientProperties properties;
  391 | 
  392 | 		RetryProperties retryProperties;
  393 | 
  394 | 	}
  395 | 
  396 | }
```
