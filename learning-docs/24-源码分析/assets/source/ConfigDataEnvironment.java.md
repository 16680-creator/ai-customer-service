<!-- learning-docs source archive（机械抓取原文，禁止手改代码内容） -->
<!-- origin: org.springframework.boot:spring-boot:4.2.0 | path: org/springframework/boot/context/config/ConfigDataEnvironment.java -->
<!-- 正文引用：[[源码:ConfigDataEnvironment.java#L起-L止]] ；代码块头部：(src: ConfigDataEnvironment.java#L起-L止) -->

# ConfigDataEnvironment.java

| 项 | 值 |
| --- | --- |
| 来源 | `org.springframework.boot:spring-boot:4.2.0` |
| 路径 | `org/springframework/boot/context/config/ConfigDataEnvironment.java` |
| 行数 | 404 |
| 在线地址 | https://repo1.maven.org/maven2/org/springframework/boot/spring-boot/4.2.0/spring-boot-4.2.0-sources.jar |
| 抓取时间 | 2026-11-25T14:04:54.622Z |

```java
    1 | /*
    2 |  * Copyright 2012-present the original author or authors.
    3 |  */
    4 | 
    5 | package org.springframework.boot.context.config;
    6 | 
    7 | import java.util.ArrayList;
    8 | import java.util.Collection;
    9 | import java.util.Collections;
   10 | import java.util.LinkedHashSet;
   11 | import java.util.List;
   12 | import java.util.Set;
   13 | 
   14 | import org.apache.commons.logging.Log;
   15 | import org.jspecify.annotations.Nullable;
   16 | 
   17 | import org.springframework.boot.bootstrap.BootstrapRegistry.InstanceSupplier;
   18 | import org.springframework.boot.bootstrap.BootstrapRegistry.Scope;
   19 | import org.springframework.boot.bootstrap.ConfigurableBootstrapContext;
   20 | import org.springframework.boot.context.config.ConfigDataEnvironmentContributors.BinderOption;
   21 | import org.springframework.boot.context.properties.bind.BindException;
   22 | import org.springframework.boot.context.properties.bind.Bindable;
   23 | import org.springframework.boot.context.properties.bind.Binder;
   24 | import org.springframework.boot.context.properties.bind.PlaceholdersResolver;
   25 | import org.springframework.boot.context.properties.source.ConfigurationPropertySource;
   26 | import org.springframework.boot.env.DefaultPropertiesPropertySource;
   27 | import org.springframework.boot.logging.DeferredLogFactory;
   28 | import org.springframework.core.env.ConfigurableEnvironment;
   29 | import org.springframework.core.env.Environment;
   30 | import org.springframework.core.env.MutablePropertySources;
   31 | import org.springframework.core.env.PropertySource;
   32 | import org.springframework.core.io.ResourceLoader;
   33 | import org.springframework.core.io.support.SpringFactoriesLoader;
   34 | import org.springframework.core.log.LogMessage;
   35 | import org.springframework.util.Assert;
   36 | import org.springframework.util.StringUtils;
   37 | 
   38 | /**
   39 |  * Wrapper around a {@link ConfigurableEnvironment} that can be used to import and apply
   40 |  * {@link ConfigData}. Configures the initial set of
   41 |  * {@link ConfigDataEnvironmentContributors} by wrapping property sources from the Spring
   42 |  * {@link Environment} and adding the initial set of locations.
   43 |  * <p>
   44 |  * The initial locations can be influenced through the {@link #LOCATION_PROPERTY},
   45 |  * {@value #ADDITIONAL_LOCATION_PROPERTY} and {@value #IMPORT_PROPERTY} properties. If no
   46 |  * explicit properties are set, the {@link #DEFAULT_SEARCH_LOCATIONS} will be used.
   47 |  *
   48 |  * @author Phillip Webb
   49 |  * @author Madhura Bhave
   50 |  * @author Nan Chiu
   51 |  */
   52 | class ConfigDataEnvironment {
   53 | 
   54 | 	/**
   55 | 	 * Property used override the imported locations.
   56 | 	 */
   57 | 	static final String LOCATION_PROPERTY = "spring.config.location";
   58 | 
   59 | 	/**
   60 | 	 * Property used to provide additional locations to import.
   61 | 	 */
   62 | 	static final String ADDITIONAL_LOCATION_PROPERTY = "spring.config.additional-location";
   63 | 
   64 | 	/**
   65 | 	 * Property used to provide additional locations to import.
   66 | 	 */
   67 | 	static final String IMPORT_PROPERTY = "spring.config.import";
   68 | 
   69 | 	/**
   70 | 	 * Property used to determine what action to take when a
   71 | 	 * {@code ConfigDataNotFoundAction} is thrown.
   72 | 	 * @see ConfigDataNotFoundAction
   73 | 	 */
   74 | 	static final String ON_NOT_FOUND_PROPERTY = "spring.config.on-not-found";
   75 | 
   76 | 	/**
   77 | 	 * Default search locations used if not {@link #LOCATION_PROPERTY} is found.
   78 | 	 */
   79 | 	static final ConfigDataLocation[] DEFAULT_SEARCH_LOCATIONS;
   80 | 	static {
   81 | 		List<ConfigDataLocation> locations = new ArrayList<>();
   82 | 		locations.add(ConfigDataLocation.of("optional:classpath:/;optional:classpath:/config/"));
   83 | 		locations.add(ConfigDataLocation.of("optional:file:./;optional:file:./config/;optional:file:./config/*/"));
   84 | 		DEFAULT_SEARCH_LOCATIONS = locations.toArray(new ConfigDataLocation[0]);
   85 | 	}
   86 | 
   87 | 	private static final ConfigDataLocation[] EMPTY_LOCATIONS = new ConfigDataLocation[0];
   88 | 
   89 | 	private static final Bindable<ConfigDataLocation[]> CONFIG_DATA_LOCATION_ARRAY = Bindable
   90 | 		.of(ConfigDataLocation[].class);
   91 | 
   92 | 	private static final Bindable<List<String>> STRING_LIST = Bindable.listOf(String.class);
   93 | 
   94 | 	private static final BinderOption[] ALLOW_INACTIVE_BINDING = {};
   95 | 
   96 | 	private static final BinderOption[] DENY_INACTIVE_BINDING = { BinderOption.FAIL_ON_BIND_TO_INACTIVE_SOURCE };
   97 | 
   98 | 	private final DeferredLogFactory logFactory;
   99 | 
  100 | 	private final Log logger;
  101 | 
  102 | 	private final ConfigDataNotFoundAction notFoundAction;
  103 | 
  104 | 	private final ConfigurableBootstrapContext bootstrapContext;
  105 | 
  106 | 	private final ConfigurableEnvironment environment;
  107 | 
  108 | 	private final ConfigDataLocationResolvers resolvers;
  109 | 
  110 | 	private final Collection<String> additionalProfiles;
  111 | 
  112 | 	private final ConfigDataEnvironmentUpdateListener environmentUpdateListener;
  113 | 
  114 | 	private final ConfigDataLoaders loaders;
  115 | 
  116 | 	private final ConfigDataEnvironmentContributors contributors;
  117 | 
  118 | 	/**
  119 | 	 * Create a new {@link ConfigDataEnvironment} instance.
  120 | 	 * @param logFactory the deferred log factory
  121 | 	 * @param bootstrapContext the bootstrap context
  122 | 	 * @param environment the Spring {@link Environment}.
  123 | 	 * @param resourceLoader {@link ResourceLoader} to load resource locations
  124 | 	 * @param additionalProfiles any additional profiles to activate
  125 | 	 * @param environmentUpdateListener optional
  126 | 	 * {@link ConfigDataEnvironmentUpdateListener} that can be used to track
  127 | 	 * {@link Environment} updates.
  128 | 	 */
  129 | 	ConfigDataEnvironment(DeferredLogFactory logFactory, ConfigurableBootstrapContext bootstrapContext,
  130 | 			ConfigurableEnvironment environment, ResourceLoader resourceLoader, Collection<String> additionalProfiles,
  131 | 			@Nullable ConfigDataEnvironmentUpdateListener environmentUpdateListener) {
  132 | 		Binder binder = Binder.get(environment);
  133 | 		this.logFactory = logFactory;
  134 | 		this.logger = logFactory.getLog(getClass());
  135 | 		this.notFoundAction = binder.bind(ON_NOT_FOUND_PROPERTY, ConfigDataNotFoundAction.class)
  136 | 			.orElse(ConfigDataNotFoundAction.FAIL);
  137 | 		this.bootstrapContext = bootstrapContext;
  138 | 		this.environment = environment;
  139 | 		this.resolvers = createConfigDataLocationResolvers(logFactory, bootstrapContext, binder, resourceLoader);
  140 | 		this.additionalProfiles = additionalProfiles;
  141 | 		this.environmentUpdateListener = (environmentUpdateListener != null) ? environmentUpdateListener
  142 | 				: ConfigDataEnvironmentUpdateListener.NONE;
  143 | 		this.loaders = new ConfigDataLoaders(logFactory, bootstrapContext,
  144 | 				SpringFactoriesLoader.forDefaultResourceLocation(resourceLoader.getClassLoader()));
  145 | 		this.contributors = createContributors(binder);
  146 | 	}
  147 | 
  148 | 	protected ConfigDataLocationResolvers createConfigDataLocationResolvers(DeferredLogFactory logFactory,
  149 | 			ConfigurableBootstrapContext bootstrapContext, Binder binder, ResourceLoader resourceLoader) {
  150 | 		return new ConfigDataLocationResolvers(logFactory, bootstrapContext, binder, resourceLoader,
  151 | 				SpringFactoriesLoader.forDefaultResourceLocation(resourceLoader.getClassLoader()));
  152 | 	}
  153 | 
  154 | 	private ConfigDataEnvironmentContributors createContributors(Binder binder) {
  155 | 		this.logger.trace("Building config data environment contributors");
  156 | 		MutablePropertySources propertySources = this.environment.getPropertySources();
  157 | 		List<ConfigDataEnvironmentContributor> contributors = new ArrayList<>(propertySources.size() + 10);
  158 | 		PropertySource<?> defaultPropertySource = null;
  159 | 		for (PropertySource<?> propertySource : propertySources) {
  160 | 			if (DefaultPropertiesPropertySource.hasMatchingName(propertySource)) {
  161 | 				defaultPropertySource = propertySource;
  162 | 			}
  163 | 			else {
  164 | 				this.logger.trace(LogMessage.format("Creating wrapped config data contributor for '%s'",
  165 | 						propertySource.getName()));
  166 | 				contributors.add(ConfigDataEnvironmentContributor.ofExisting(propertySource,
  167 | 						this.environment.getConversionService()));
  168 | 			}
  169 | 		}
  170 | 		contributors.addAll(getInitialImportContributors(binder));
  171 | 		if (defaultPropertySource != null) {
  172 | 			this.logger.trace("Creating wrapped config data contributor for default property source");
  173 | 			contributors.add(ConfigDataEnvironmentContributor.ofExisting(defaultPropertySource,
  174 | 					this.environment.getConversionService()));
  175 | 		}
  176 | 		return createContributors(contributors);
  177 | 	}
  178 | 
  179 | 	protected ConfigDataEnvironmentContributors createContributors(
  180 | 			List<ConfigDataEnvironmentContributor> contributors) {
  181 | 		return new ConfigDataEnvironmentContributors(this.logFactory, this.bootstrapContext, contributors,
  182 | 				this.environment.getConversionService(), this.environmentUpdateListener);
  183 | 	}
  184 | 
  185 | 	ConfigDataEnvironmentContributors getContributors() {
  186 | 		return this.contributors;
  187 | 	}
  188 | 
  189 | 	private List<ConfigDataEnvironmentContributor> getInitialImportContributors(Binder binder) {
  190 | 		List<ConfigDataEnvironmentContributor> initialContributors = new ArrayList<>();
  191 | 		addInitialImportContributors(initialContributors, binder, IMPORT_PROPERTY, EMPTY_LOCATIONS, false);
  192 | 		addInitialImportContributors(initialContributors, binder, ADDITIONAL_LOCATION_PROPERTY, EMPTY_LOCATIONS, true);
  193 | 		addInitialImportContributors(initialContributors, binder, LOCATION_PROPERTY, DEFAULT_SEARCH_LOCATIONS, true);
  194 | 		return initialContributors;
  195 | 	}
  196 | 
  197 | 	private void addInitialImportContributors(List<ConfigDataEnvironmentContributor> initialContributors, Binder binder,
  198 | 			String propertyName, ConfigDataLocation[] defaultValue, boolean registerIndividually) {
  199 | 		ConfigDataLocation[] locations = binder.bind(propertyName, CONFIG_DATA_LOCATION_ARRAY).orElse(defaultValue);
  200 | 		if (registerIndividually) {
  201 | 			for (int i = locations.length - 1; i >= 0; i--) {
  202 | 				addInitialImportContributors(initialContributors, List.of(locations[i]));
  203 | 			}
  204 | 		}
  205 | 		else {
  206 | 			addInitialImportContributors(initialContributors, List.of(locations));
  207 | 		}
  208 | 	}
  209 | 
  210 | 	private void addInitialImportContributors(List<ConfigDataEnvironmentContributor> initialContributors,
  211 | 			List<ConfigDataLocation> locations) {
  212 | 		if (!locations.isEmpty()) {
  213 | 			this.logger.trace(LogMessage.format("Adding initial config data import from locations %s", locations));
  214 | 			ConfigDataEnvironmentContributor contributor = ConfigDataEnvironmentContributor.ofInitialImports(locations,
  215 | 					this.environment.getConversionService());
  216 | 			initialContributors.add(contributor);
  217 | 		}
  218 | 	}
  219 | 
  220 | 	/**
  221 | 	 * Process all contributions and apply any newly imported property sources to the
  222 | 	 * {@link Environment}.
  223 | 	 */
  224 | 	void processAndApply() {
  225 | 		ConfigDataImporter importer = new ConfigDataImporter(this.logFactory, this.notFoundAction, this.resolvers,
  226 | 				this.loaders);
  227 | 		registerBootstrapBinder(this.contributors, null, DENY_INACTIVE_BINDING);
  228 | 		ConfigDataEnvironmentContributors contributors = processInitial(this.contributors, importer);
  229 | 		ConfigDataActivationContext activationContext = createActivationContext(
  230 | 				contributors.getBinder(null, BinderOption.FAIL_ON_BIND_TO_INACTIVE_SOURCE));
  231 | 		contributors = processWithoutProfiles(contributors, importer, activationContext);
  232 | 		activationContext = withProfiles(contributors, activationContext);
  233 | 		contributors = processWithProfiles(contributors, importer, activationContext);
  234 | 		applyToEnvironment(contributors, activationContext, importer.getLoadedLocations(),
  235 | 				importer.getOptionalLocations());
  236 | 	}
  237 | 
  238 | 	private ConfigDataEnvironmentContributors processInitial(ConfigDataEnvironmentContributors contributors,
  239 | 			ConfigDataImporter importer) {
  240 | 		this.logger.trace("Processing initial config data environment contributors without activation context");
  241 | 		contributors = contributors.withProcessedImports(importer, null);
  242 | 		registerBootstrapBinder(contributors, null, DENY_INACTIVE_BINDING);
  243 | 		return contributors;
  244 | 	}
  245 | 
  246 | 	private ConfigDataActivationContext createActivationContext(Binder initialBinder) {
  247 | 		this.logger.trace("Creating config data activation context from initial contributions");
  248 | 		try {
  249 | 			return new ConfigDataActivationContext(this.environment, initialBinder);
  250 | 		}
  251 | 		catch (BindException ex) {
  252 | 			if (ex.getCause() instanceof InactiveConfigDataAccessException inactiveException) {
  253 | 				throw inactiveException;
  254 | 			}
  255 | 			throw ex;
  256 | 		}
  257 | 	}
  258 | 
  259 | 	private ConfigDataEnvironmentContributors processWithoutProfiles(ConfigDataEnvironmentContributors contributors,
  260 | 			ConfigDataImporter importer, ConfigDataActivationContext activationContext) {
  261 | 		this.logger.trace("Processing config data environment contributors with initial activation context");
  262 | 		contributors = contributors.withProcessedImports(importer, activationContext);
  263 | 		registerBootstrapBinder(contributors, activationContext, DENY_INACTIVE_BINDING);
  264 | 		return contributors;
  265 | 	}
  266 | 
  267 | 	private ConfigDataActivationContext withProfiles(ConfigDataEnvironmentContributors contributors,
  268 | 			ConfigDataActivationContext activationContext) {
  269 | 		this.logger.trace("Deducing profiles from current config data environment contributors");
  270 | 		Binder binder = contributors.getBinder(activationContext,
  271 | 				(contributor) -> !contributor.hasConfigDataOption(ConfigData.Option.IGNORE_PROFILES),
  272 | 				BinderOption.FAIL_ON_BIND_TO_INACTIVE_SOURCE);
  273 | 		try {
  274 | 			Set<String> additionalProfiles = new LinkedHashSet<>(this.additionalProfiles);
  275 | 			additionalProfiles.addAll(getIncludedProfiles(contributors, activationContext));
  276 | 			Profiles profiles = new Profiles(this.environment, binder, additionalProfiles);
  277 | 			return activationContext.withProfiles(profiles);
  278 | 		}
  279 | 		catch (BindException ex) {
  280 | 			if (ex.getCause() instanceof InactiveConfigDataAccessException inactiveException) {
  281 | 				throw inactiveException;
  282 | 			}
  283 | 			throw ex;
  284 | 		}
  285 | 	}
  286 | 
  287 | 	private Collection<? extends String> getIncludedProfiles(ConfigDataEnvironmentContributors contributors,
  288 | 			ConfigDataActivationContext activationContext) {
  289 | 		PlaceholdersResolver placeholdersResolver = new ConfigDataEnvironmentContributorPlaceholdersResolver(
  290 | 				contributors, activationContext, null, true, this.environment.getConversionService());
  291 | 		Set<String> result = new LinkedHashSet<>();
  292 | 		for (ConfigDataEnvironmentContributor contributor : contributors) {
  293 | 			ConfigurationPropertySource source = contributor.getConfigurationPropertySource();
  294 | 			if (source != null && !contributor.hasConfigDataOption(ConfigData.Option.IGNORE_PROFILES)) {
  295 | 				Binder binder = new Binder(Collections.singleton(source), placeholdersResolver);
  296 | 				binder.bind(Profiles.INCLUDE_PROFILES, STRING_LIST, ProfilesValidator.get(binder))
  297 | 					.ifBound((includes) -> {
  298 | 						if (!contributor.isActive(activationContext)) {
  299 | 							InactiveConfigDataAccessException.throwIfPropertyFound(contributor,
  300 | 									Profiles.INCLUDE_PROFILES);
  301 | 							InactiveConfigDataAccessException.throwIfPropertyFound(contributor,
  302 | 									Profiles.INCLUDE_PROFILES.append("[0]"));
  303 | 						}
  304 | 						result.addAll(includes);
  305 | 					});
  306 | 			}
  307 | 		}
  308 | 		return result;
  309 | 	}
  310 | 
  311 | 	private ConfigDataEnvironmentContributors processWithProfiles(ConfigDataEnvironmentContributors contributors,
  312 | 			ConfigDataImporter importer, ConfigDataActivationContext activationContext) {
  313 | 		this.logger.trace("Processing config data environment contributors with profile activation context");
  314 | 		contributors = contributors.withProcessedImports(importer, activationContext);
  315 | 		registerBootstrapBinder(contributors, activationContext, ALLOW_INACTIVE_BINDING);
  316 | 		return contributors;
  317 | 	}
  318 | 
  319 | 	private void registerBootstrapBinder(ConfigDataEnvironmentContributors contributors,
  320 | 			@Nullable ConfigDataActivationContext activationContext, BinderOption... binderOptions) {
  321 | 		this.bootstrapContext.register(Binder.class,
  322 | 				InstanceSupplier.from(() -> contributors.getBinder(activationContext, binderOptions))
  323 | 					.withScope(Scope.PROTOTYPE));
  324 | 	}
  325 | 
  326 | 	private void applyToEnvironment(ConfigDataEnvironmentContributors contributors,
  327 | 			ConfigDataActivationContext activationContext, Set<ConfigDataLocation> loadedLocations,
  328 | 			Set<ConfigDataLocation> optionalLocations) {
  329 | 		checkForInvalidProperties(contributors);
  330 | 		checkMandatoryLocations(contributors, activationContext, loadedLocations, optionalLocations);
  331 | 		MutablePropertySources propertySources = this.environment.getPropertySources();
  332 | 		applyContributor(contributors, activationContext, propertySources);
  333 | 		DefaultPropertiesPropertySource.moveToEnd(propertySources);
  334 | 		Profiles profiles = activationContext.getProfiles();
  335 | 		Assert.state(profiles != null, "'profiles' must not be null");
  336 | 		this.logger.trace(LogMessage.format("Setting default profiles: %s", profiles.getDefault()));
  337 | 		this.environment.setDefaultProfiles(StringUtils.toStringArray(profiles.getDefault()));
  338 | 		this.logger.trace(LogMessage.format("Setting active profiles: %s", profiles.getActive()));
  339 | 		this.environment.setActiveProfiles(StringUtils.toStringArray(profiles.getActive()));
  340 | 		this.environmentUpdateListener.onSetProfiles(profiles);
  341 | 	}
  342 | 
  343 | 	private void applyContributor(ConfigDataEnvironmentContributors contributors,
  344 | 			ConfigDataActivationContext activationContext, MutablePropertySources propertySources) {
  345 | 		this.logger.trace("Applying config data environment contributions");
  346 | 		for (ConfigDataEnvironmentContributor contributor : contributors) {
  347 | 			PropertySource<?> propertySource = contributor.getPropertySource();
  348 | 			if (contributor.getKind() == ConfigDataEnvironmentContributor.Kind.BOUND_IMPORT && propertySource != null) {
  349 | 				if (!contributor.isActive(activationContext)) {
  350 | 					this.logger
  351 | 						.trace(LogMessage.format("Skipping inactive property source '%s'", propertySource.getName()));
  352 | 				}
  353 | 				else {
  354 | 					this.logger
  355 | 						.trace(LogMessage.format("Adding imported property source '%s'", propertySource.getName()));
  356 | 					propertySources.addLast(propertySource);
  357 | 					this.environmentUpdateListener.onPropertySourceAdded(propertySource, contributor.getLocation(),
  358 | 							contributor.getResource());
  359 | 				}
  360 | 			}
  361 | 		}
  362 | 	}
  363 | 
  364 | 	private void checkForInvalidProperties(ConfigDataEnvironmentContributors contributors) {
  365 | 		for (ConfigDataEnvironmentContributor contributor : contributors) {
  366 | 			InvalidConfigDataPropertyException.throwIfPropertyFound(contributor);
  367 | 		}
  368 | 	}
  369 | 
  370 | 	private void checkMandatoryLocations(ConfigDataEnvironmentContributors contributors,
  371 | 			ConfigDataActivationContext activationContext, Set<ConfigDataLocation> loadedLocations,
  372 | 			Set<ConfigDataLocation> optionalLocations) {
  373 | 		Set<ConfigDataLocation> mandatoryLocations = new LinkedHashSet<>();
  374 | 		for (ConfigDataEnvironmentContributor contributor : contributors) {
  375 | 			if (contributor.isActive(activationContext)) {
  376 | 				mandatoryLocations.addAll(getMandatoryImports(contributor));
  377 | 			}
  378 | 		}
  379 | 		for (ConfigDataEnvironmentContributor contributor : contributors) {
  380 | 			if (contributor.getLocation() != null) {
  381 | 				mandatoryLocations.remove(contributor.getLocation());
  382 | 			}
  383 | 		}
  384 | 		mandatoryLocations.removeAll(loadedLocations);
  385 | 		mandatoryLocations.removeAll(optionalLocations);
  386 | 		if (!mandatoryLocations.isEmpty()) {
  387 | 			for (ConfigDataLocation mandatoryLocation : mandatoryLocations) {
  388 | 				this.notFoundAction.handle(this.logger, new ConfigDataLocationNotFoundException(mandatoryLocation));
  389 | 			}
  390 | 		}
  391 | 	}
  392 | 
  393 | 	private Set<ConfigDataLocation> getMandatoryImports(ConfigDataEnvironmentContributor contributor) {
  394 | 		List<ConfigDataLocation> imports = contributor.getImports();
  395 | 		Set<ConfigDataLocation> mandatoryLocations = new LinkedHashSet<>(imports.size());
  396 | 		for (ConfigDataLocation location : imports) {
  397 | 			if (!location.isOptional()) {
  398 | 				mandatoryLocations.add(location);
  399 | 			}
  400 | 		}
  401 | 		return mandatoryLocations;
  402 | 	}
  403 | 
  404 | }
```
