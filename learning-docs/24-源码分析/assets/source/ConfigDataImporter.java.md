<!-- learning-docs source archive（机械抓取原文，禁止手改代码内容） -->
<!-- origin: org.springframework.boot:spring-boot:4.2.0 | path: org/springframework/boot/context/config/ConfigDataImporter.java -->
<!-- 正文引用：[[源码:ConfigDataImporter.java#L起-L止]] ；代码块头部：(src: ConfigDataImporter.java#L起-L止) -->

# ConfigDataImporter.java

| 项 | 值 |
| --- | --- |
| 来源 | `org.springframework.boot:spring-boot:4.2.0` |
| 路径 | `org/springframework/boot/context/config/ConfigDataImporter.java` |
| 行数 | 161 |
| 在线地址 | https://repo1.maven.org/maven2/org/springframework/boot/spring-boot/4.2.0/spring-boot-4.2.0-sources.jar |
| 抓取时间 | 2026-11-25T14:04:54.751Z |

```java
    1 | /*
    2 |  * Copyright 2012-present the original author or authors.
    3 |  */
    4 | 
    5 | package org.springframework.boot.context.config;
    6 | 
    7 | import java.io.IOException;
    8 | import java.util.ArrayList;
    9 | import java.util.Collections;
   10 | import java.util.HashSet;
   11 | import java.util.LinkedHashMap;
   12 | import java.util.List;
   13 | import java.util.Map;
   14 | import java.util.Set;
   15 | 
   16 | import org.apache.commons.logging.Log;
   17 | import org.jspecify.annotations.Nullable;
   18 | 
   19 | import org.springframework.boot.logging.DeferredLogFactory;
   20 | import org.springframework.core.log.LogMessage;
   21 | 
   22 | /**
   23 |  * Imports {@link ConfigData} by {@link ConfigDataLocationResolver resolving} and
   24 |  * {@link ConfigDataLoader loading} locations. {@link ConfigDataResource resources} are
   25 |  * tracked to ensure that they are not imported multiple times.
   26 |  *
   27 |  * @author Phillip Webb
   28 |  * @author Madhura Bhave
   29 |  */
   30 | class ConfigDataImporter {
   31 | 
   32 | 	private final Log logger;
   33 | 
   34 | 	private final ConfigDataLocationResolvers resolvers;
   35 | 
   36 | 	private final ConfigDataLoaders loaders;
   37 | 
   38 | 	private final ConfigDataNotFoundAction notFoundAction;
   39 | 
   40 | 	private final Set<ConfigDataResource> loaded = new HashSet<>();
   41 | 
   42 | 	private final Set<ConfigDataLocation> loadedLocations = new HashSet<>();
   43 | 
   44 | 	private final Set<ConfigDataLocation> optionalLocations = new HashSet<>();
   45 | 
   46 | 	/**
   47 | 	 * Create a new {@link ConfigDataImporter} instance.
   48 | 	 * @param logFactory the log factory
   49 | 	 * @param notFoundAction the action to take when a location cannot be found
   50 | 	 * @param resolvers the config data location resolvers
   51 | 	 * @param loaders the config data loaders
   52 | 	 */
   53 | 	ConfigDataImporter(DeferredLogFactory logFactory, ConfigDataNotFoundAction notFoundAction,
   54 | 			ConfigDataLocationResolvers resolvers, ConfigDataLoaders loaders) {
   55 | 		this.logger = logFactory.getLog(getClass());
   56 | 		this.resolvers = resolvers;
   57 | 		this.loaders = loaders;
   58 | 		this.notFoundAction = notFoundAction;
   59 | 	}
   60 | 
   61 | 	/**
   62 | 	 * Resolve and load the given list of locations, filtering any that have been
   63 | 	 * previously loaded.
   64 | 	 * @param activationContext the activation context
   65 | 	 * @param locationResolverContext the location resolver context
   66 | 	 * @param loaderContext the loader context
   67 | 	 * @param locations the locations to resolve
   68 | 	 * @return a map of the loaded locations and data
   69 | 	 */
   70 | 	Map<ConfigDataResolutionResult, ConfigData> resolveAndLoad(@Nullable ConfigDataActivationContext activationContext,
   71 | 			ConfigDataLocationResolverContext locationResolverContext, ConfigDataLoaderContext loaderContext,
   72 | 			List<ConfigDataLocation> locations) {
   73 | 		try {
   74 | 			Profiles profiles = (activationContext != null) ? activationContext.getProfiles() : null;
   75 | 			List<ConfigDataResolutionResult> resolved = resolve(locationResolverContext, profiles, locations);
   76 | 			return load(loaderContext, resolved);
   77 | 		}
   78 | 		catch (IOException ex) {
   79 | 			throw new IllegalStateException("IO error on loading imports from " + locations, ex);
   80 | 		}
   81 | 	}
   82 | 
   83 | 	private List<ConfigDataResolutionResult> resolve(ConfigDataLocationResolverContext locationResolverContext,
   84 | 			@Nullable Profiles profiles, List<ConfigDataLocation> locations) {
   85 | 		List<ConfigDataResolutionResult> resolved = new ArrayList<>(locations.size());
   86 | 		for (ConfigDataLocation location : locations) {
   87 | 			resolved.addAll(resolve(locationResolverContext, profiles, location));
   88 | 		}
   89 | 		return Collections.unmodifiableList(resolved);
   90 | 	}
   91 | 
   92 | 	private List<ConfigDataResolutionResult> resolve(ConfigDataLocationResolverContext locationResolverContext,
   93 | 			@Nullable Profiles profiles, ConfigDataLocation location) {
   94 | 		try {
   95 | 			return this.resolvers.resolve(locationResolverContext, location, profiles);
   96 | 		}
   97 | 		catch (ConfigDataNotFoundException ex) {
   98 | 			handle(ex, location, null);
   99 | 			return Collections.emptyList();
  100 | 		}
  101 | 	}
  102 | 
  103 | 	private Map<ConfigDataResolutionResult, ConfigData> load(ConfigDataLoaderContext loaderContext,
  104 | 			List<ConfigDataResolutionResult> candidates) throws IOException {
  105 | 		Map<ConfigDataResolutionResult, ConfigData> result = new LinkedHashMap<>();
  106 | 		for (int i = candidates.size() - 1; i >= 0; i--) {
  107 | 			ConfigDataResolutionResult candidate = candidates.get(i);
  108 | 			ConfigDataLocation location = candidate.getLocation();
  109 | 			ConfigDataResource resource = candidate.getResource();
  110 | 			this.logger.trace(LogMessage.format("Considering resource %s from location %s", resource, location));
  111 | 			if (resource.isOptional()) {
  112 | 				this.optionalLocations.add(location);
  113 | 			}
  114 | 			if (this.loaded.contains(resource)) {
  115 | 				this.logger
  116 | 					.trace(LogMessage.format("Already loaded resource %s ignoring location %s", resource, location));
  117 | 				this.loadedLocations.add(location);
  118 | 			}
  119 | 			else {
  120 | 				try {
  121 | 					ConfigData loaded = this.loaders.load(loaderContext, resource);
  122 | 					if (loaded != null) {
  123 | 						this.logger.trace(LogMessage.format("Loaded resource %s from location %s", resource, location));
  124 | 						this.loaded.add(resource);
  125 | 						this.loadedLocations.add(location);
  126 | 						result.put(candidate, loaded);
  127 | 					}
  128 | 				}
  129 | 				catch (ConfigDataNotFoundException ex) {
  130 | 					handle(ex, location, resource);
  131 | 				}
  132 | 			}
  133 | 		}
  134 | 		return Collections.unmodifiableMap(result);
  135 | 	}
  136 | 
  137 | 	private void handle(ConfigDataNotFoundException ex, ConfigDataLocation location,
  138 | 			@Nullable ConfigDataResource resource) {
  139 | 		if (ex instanceof ConfigDataResourceNotFoundException notFoundException) {
  140 | 			ex = notFoundException.withLocation(location);
  141 | 		}
  142 | 		getNotFoundAction(location, resource).handle(this.logger, ex);
  143 | 	}
  144 | 
  145 | 	private ConfigDataNotFoundAction getNotFoundAction(ConfigDataLocation location,
  146 | 			@Nullable ConfigDataResource resource) {
  147 | 		if (location.isOptional() || (resource != null && resource.isOptional())) {
  148 | 			return ConfigDataNotFoundAction.IGNORE;
  149 | 		}
  150 | 		return this.notFoundAction;
  151 | 	}
  152 | 
  153 | 	Set<ConfigDataLocation> getLoadedLocations() {
  154 | 		return this.loadedLocations;
  155 | 	}
  156 | 
  157 | 	Set<ConfigDataLocation> getOptionalLocations() {
  158 | 		return this.optionalLocations;
  159 | 	}
  160 | 
  161 | }
```
