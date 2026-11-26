<!-- learning-docs source archive（机械抓取原文，禁止手改代码内容） -->
<!-- origin: org.springframework.boot:spring-boot:4.2.0 | path: org/springframework/boot/context/config/StandardConfigDataLocationResolver.java -->
<!-- 正文引用：[[源码:StandardConfigDataLocationResolver.java#L起-L止]] ；代码块头部：(src: StandardConfigDataLocationResolver.java#L起-L止) -->

# StandardConfigDataLocationResolver.java

| 项 | 值 |
| --- | --- |
| 来源 | `org.springframework.boot:spring-boot:4.2.0` |
| 路径 | `org/springframework/boot/context/config/StandardConfigDataLocationResolver.java` |
| 行数 | 340 |
| 在线地址 | https://repo1.maven.org/maven2/org/springframework/boot/spring-boot/4.2.0/spring-boot-4.2.0-sources.jar |
| 抓取时间 | 2026-11-25T14:04:55.177Z |

```java
    1 | /*
    2 |  * Copyright 2012-present the original author or authors.
    3 |  */
    4 | 
    5 | package org.springframework.boot.context.config;
    6 | 
    7 | import java.io.File;
    8 | import java.util.ArrayDeque;
    9 | import java.util.ArrayList;
   10 | import java.util.Arrays;
   11 | import java.util.Collection;
   12 | import java.util.Collections;
   13 | import java.util.Deque;
   14 | import java.util.LinkedHashSet;
   15 | import java.util.List;
   16 | import java.util.Set;
   17 | import java.util.regex.Pattern;
   18 | import java.util.stream.Collectors;
   19 | 
   20 | import org.apache.commons.logging.Log;
   21 | import org.jspecify.annotations.Nullable;
   22 | 
   23 | import org.springframework.boot.context.config.LocationResourceLoader.ResourceType;
   24 | import org.springframework.boot.context.properties.bind.Binder;
   25 | import org.springframework.boot.env.PropertySourceLoader;
   26 | import org.springframework.boot.logging.DeferredLogFactory;
   27 | import org.springframework.core.Ordered;
   28 | import org.springframework.core.env.Environment;
   29 | import org.springframework.core.io.ClassPathResource;
   30 | import org.springframework.core.io.Resource;
   31 | import org.springframework.core.io.ResourceLoader;
   32 | import org.springframework.core.io.support.SpringFactoriesLoader;
   33 | import org.springframework.core.log.LogMessage;
   34 | import org.springframework.util.Assert;
   35 | import org.springframework.util.ObjectUtils;
   36 | import org.springframework.util.ResourceUtils;
   37 | import org.springframework.util.StringUtils;
   38 | 
   39 | /**
   40 |  * {@link ConfigDataLocationResolver} for standard locations.
   41 |  *
   42 |  * @author Madhura Bhave
   43 |  * @author Phillip Webb
   44 |  * @author Scott Frederick
   45 |  * @author Sijun Yang
   46 |  * @since 2.4.0
   47 |  */
   48 | public class StandardConfigDataLocationResolver
   49 | 		implements ConfigDataLocationResolver<StandardConfigDataResource>, Ordered {
   50 | 
   51 | 	private static final String PREFIX = "resource:";
   52 | 
   53 | 	static final String CONFIG_NAME_PROPERTY = "spring.config.name";
   54 | 
   55 | 	static final String[] DEFAULT_CONFIG_NAMES = { "application" };
   56 | 
   57 | 	private static final Pattern URL_PREFIX = Pattern.compile("^([a-zA-Z][a-zA-Z0-9*]*?:)(.*$)");
   58 | 
   59 | 	private static final @Nullable String NO_PROFILE = null;
   60 | 
   61 | 	private final Log logger;
   62 | 
   63 | 	private final List<PropertySourceLoader> propertySourceLoaders;
   64 | 
   65 | 	private final String[] configNames;
   66 | 
   67 | 	private final LocationResourceLoader resourceLoader;
   68 | 
   69 | 	/**
   70 | 	 * Create a new {@link StandardConfigDataLocationResolver} instance.
   71 | 	 * @param logFactory the factory for loggers to use
   72 | 	 * @param binder a binder backed by the initial {@link Environment}
   73 | 	 * @param resourceLoader a {@link ResourceLoader} used to load resources
   74 | 	 */
   75 | 	public StandardConfigDataLocationResolver(DeferredLogFactory logFactory, Binder binder,
   76 | 			ResourceLoader resourceLoader) {
   77 | 		this.logger = logFactory.getLog(StandardConfigDataLocationResolver.class);
   78 | 		this.propertySourceLoaders = SpringFactoriesLoader.loadFactories(PropertySourceLoader.class,
   79 | 				resourceLoader.getClassLoader());
   80 | 		this.configNames = getConfigNames(binder);
   81 | 		this.resourceLoader = new LocationResourceLoader(resourceLoader);
   82 | 	}
   83 | 
   84 | 	private String[] getConfigNames(Binder binder) {
   85 | 		String[] configNames = binder.bind(CONFIG_NAME_PROPERTY, String[].class).orElse(DEFAULT_CONFIG_NAMES);
   86 | 		for (String configName : configNames) {
   87 | 			validateConfigName(configName);
   88 | 		}
   89 | 		return configNames;
   90 | 	}
   91 | 
   92 | 	private void validateConfigName(String name) {
   93 | 		Assert.state(!name.contains("*"), () -> "Config name '" + name + "' cannot contain '*'");
   94 | 	}
   95 | 
   96 | 	@Override
   97 | 	public int getOrder() {
   98 | 		return Ordered.LOWEST_PRECEDENCE;
   99 | 	}
  100 | 
  101 | 	@Override
  102 | 	public boolean isResolvable(ConfigDataLocationResolverContext context, ConfigDataLocation location) {
  103 | 		return true;
  104 | 	}
  105 | 
  106 | 	@Override
  107 | 	public List<StandardConfigDataResource> resolve(ConfigDataLocationResolverContext context,
  108 | 			ConfigDataLocation location) throws ConfigDataNotFoundException {
  109 | 		return resolve(getReferences(context, location.split()));
  110 | 	}
  111 | 
  112 | 	private Set<StandardConfigDataReference> getReferences(ConfigDataLocationResolverContext context,
  113 | 			ConfigDataLocation[] configDataLocations) {
  114 | 		Set<StandardConfigDataReference> references = new LinkedHashSet<>();
  115 | 		for (ConfigDataLocation configDataLocation : configDataLocations) {
  116 | 			references.addAll(getReferences(context, configDataLocation));
  117 | 		}
  118 | 		return references;
  119 | 	}
  120 | 
  121 | 	private Set<StandardConfigDataReference> getReferences(ConfigDataLocationResolverContext context,
  122 | 			ConfigDataLocation configDataLocation) {
  123 | 		String resourceLocation = getResourceLocation(context, configDataLocation);
  124 | 		try {
  125 | 			if (isDirectory(resourceLocation)) {
  126 | 				return getReferencesForDirectory(configDataLocation, resourceLocation, NO_PROFILE);
  127 | 			}
  128 | 			return getReferencesForFile(configDataLocation, resourceLocation, NO_PROFILE);
  129 | 		}
  130 | 		catch (RuntimeException ex) {
  131 | 			throw new IllegalStateException("Unable to load config data from '" + configDataLocation + "'", ex);
  132 | 		}
  133 | 	}
  134 | 
  135 | 	@Override
  136 | 	public List<StandardConfigDataResource> resolveProfileSpecific(ConfigDataLocationResolverContext context,
  137 | 			ConfigDataLocation location, Profiles profiles) {
  138 | 		return resolve(getProfileSpecificReferences(context, location.split(), profiles));
  139 | 	}
  140 | 
  141 | 	private Set<StandardConfigDataReference> getProfileSpecificReferences(ConfigDataLocationResolverContext context,
  142 | 			ConfigDataLocation[] configDataLocations, Profiles profiles) {
  143 | 		Set<StandardConfigDataReference> references = new LinkedHashSet<>();
  144 | 		for (String profile : profiles) {
  145 | 			for (ConfigDataLocation configDataLocation : configDataLocations) {
  146 | 				String resourceLocation = getResourceLocation(context, configDataLocation);
  147 | 				references.addAll(getReferences(configDataLocation, resourceLocation, profile));
  148 | 			}
  149 | 		}
  150 | 		return references;
  151 | 	}
  152 | 
  153 | 	private String getResourceLocation(ConfigDataLocationResolverContext context,
  154 | 			ConfigDataLocation configDataLocation) {
  155 | 		String resourceLocation = configDataLocation.getNonPrefixedValue(PREFIX);
  156 | 		boolean isFixedPath = resourceLocation.startsWith("/") || URL_PREFIX.matcher(resourceLocation).matches();
  157 | 		if (isFixedPath) {
  158 | 			return resourceLocation;
  159 | 		}
  160 | 		ConfigDataResource parent = context.getParent();
  161 | 		if (parent instanceof StandardConfigDataResource resource) {
  162 | 			String parentResourceLocation = resource.getReference().getResourceLocation();
  163 | 			String parentDirectory = parentResourceLocation.substring(0, parentResourceLocation.lastIndexOf("/") + 1);
  164 | 			return parentDirectory + resourceLocation;
  165 | 		}
  166 | 		return resourceLocation;
  167 | 	}
  168 | 
  169 | 	private Set<StandardConfigDataReference> getReferences(ConfigDataLocation configDataLocation,
  170 | 			String resourceLocation, String profile) {
  171 | 		if (isDirectory(resourceLocation)) {
  172 | 			return getReferencesForDirectory(configDataLocation, resourceLocation, profile);
  173 | 		}
  174 | 		return getReferencesForFile(configDataLocation, resourceLocation, profile);
  175 | 	}
  176 | 
  177 | 	private Set<StandardConfigDataReference> getReferencesForDirectory(ConfigDataLocation configDataLocation,
  178 | 			String directory, @Nullable String profile) {
  179 | 		Set<StandardConfigDataReference> references = new LinkedHashSet<>();
  180 | 		for (String name : this.configNames) {
  181 | 			Deque<StandardConfigDataReference> referencesForName = getReferencesForConfigName(name, configDataLocation,
  182 | 					directory, profile);
  183 | 			references.addAll(referencesForName);
  184 | 		}
  185 | 		return references;
  186 | 	}
  187 | 
  188 | 	private Deque<StandardConfigDataReference> getReferencesForConfigName(String name,
  189 | 			ConfigDataLocation configDataLocation, String directory, @Nullable String profile) {
  190 | 		Deque<StandardConfigDataReference> references = new ArrayDeque<>();
  191 | 		for (PropertySourceLoader propertySourceLoader : this.propertySourceLoaders) {
  192 | 			for (String extension : propertySourceLoader.getFileExtensions()) {
  193 | 				StandardConfigDataReference reference = new StandardConfigDataReference(configDataLocation, directory,
  194 | 						directory + name, profile, extension, propertySourceLoader, null);
  195 | 				if (!references.contains(reference)) {
  196 | 					references.addFirst(reference);
  197 | 				}
  198 | 			}
  199 | 		}
  200 | 		return references;
  201 | 	}
  202 | 
  203 | 	private Set<StandardConfigDataReference> getReferencesForFile(ConfigDataLocation configDataLocation, String file,
  204 | 			@Nullable String profile) {
  205 | 		FileHint fileHint = FileHint.from(file);
  206 | 		file = FileHint.removeFrom(file);
  207 | 		boolean hasFileHintExtension = fileHint.getExtension() != null;
  208 | 		if (hasFileHintExtension) {
  209 | 			file = file + fileHint.getExtension();
  210 | 		}
  211 | 		for (PropertySourceLoader propertySourceLoader : this.propertySourceLoaders) {
  212 | 			String fileExtension = getLoadableFileExtension(propertySourceLoader, file);
  213 | 			if (fileExtension != null) {
  214 | 				String root = file.substring(0, file.length() - fileExtension.length() - 1);
  215 | 				StandardConfigDataReference reference = new StandardConfigDataReference(configDataLocation, null, root,
  216 | 						profile, (!hasFileHintExtension) ? fileExtension : null, propertySourceLoader,
  217 | 						fileHint.getEncodingAsCharset());
  218 | 				return Collections.singleton(reference);
  219 | 			}
  220 | 		}
  221 | 		if (configDataLocation.isOptional()) {
  222 | 			return Collections.emptySet();
  223 | 		}
  224 | 		if (configDataLocation.hasPrefix(PREFIX) || configDataLocation.hasPrefix(ResourceUtils.FILE_URL_PREFIX)
  225 | 				|| configDataLocation.hasPrefix(ResourceUtils.CLASSPATH_URL_PREFIX)
  226 | 				|| configDataLocation.toString().indexOf(':') == -1) {
  227 | 			throw new IllegalStateException("File extension is not known to any PropertySourceLoader. "
  228 | 					+ "If the location is meant to reference a directory, it must end in '/' or File.separator");
  229 | 		}
  230 | 		throw new IllegalStateException(
  231 | 				"Incorrect ConfigDataLocationResolver chosen or file extension is not known to any PropertySourceLoader. "
  232 | 						+ "If the location is meant to reference a directory, it must end in '/' or File.separator. "
  233 | 						+ "The location is being resolved using the StandardConfigDataLocationResolver, "
  234 | 						+ "check the location prefix if a different resolver is expected");
  235 | 	}
  236 | 
  237 | 	private @Nullable String getLoadableFileExtension(PropertySourceLoader loader, String file) {
  238 | 		for (String fileExtension : loader.getFileExtensions()) {
  239 | 			if (StringUtils.endsWithIgnoreCase(file, fileExtension)) {
  240 | 				return fileExtension;
  241 | 			}
  242 | 		}
  243 | 		return null;
  244 | 	}
  245 | 
  246 | 	private boolean isDirectory(String resourceLocation) {
  247 | 		return resourceLocation.endsWith("/") || resourceLocation.endsWith(File.separator);
  248 | 	}
  249 | 
  250 | 	private List<StandardConfigDataResource> resolve(Set<StandardConfigDataReference> references) {
  251 | 		List<StandardConfigDataResource> resolved = new ArrayList<>();
  252 | 		for (StandardConfigDataReference reference : references) {
  253 | 			resolved.addAll(resolve(reference));
  254 | 		}
  255 | 		if (resolved.isEmpty()) {
  256 | 			resolved.addAll(resolveEmptyDirectories(references));
  257 | 		}
  258 | 		return resolved;
  259 | 	}
  260 | 
  261 | 	private Collection<StandardConfigDataResource> resolveEmptyDirectories(
  262 | 			Set<StandardConfigDataReference> references) {
  263 | 		Set<StandardConfigDataResource> empty = new LinkedHashSet<>();
  264 | 		for (StandardConfigDataReference reference : references) {
  265 | 			if (reference.getDirectory() != null) {
  266 | 				empty.addAll(resolveEmptyDirectories(reference));
  267 | 			}
  268 | 		}
  269 | 		return empty;
  270 | 	}
  271 | 
  272 | 	private Set<StandardConfigDataResource> resolveEmptyDirectories(StandardConfigDataReference reference) {
  273 | 		if (!this.resourceLoader.isPattern(reference.getResourceLocation())) {
  274 | 			return resolveNonPatternEmptyDirectories(reference);
  275 | 		}
  276 | 		return resolvePatternEmptyDirectories(reference);
  277 | 	}
  278 | 
  279 | 	private Set<StandardConfigDataResource> resolveNonPatternEmptyDirectories(StandardConfigDataReference reference) {
  280 | 		String directory = reference.getDirectory();
  281 | 		Assert.state(directory != null, "'directory' must not be null");
  282 | 		Resource resource = this.resourceLoader.getResource(directory);
  283 | 		return (resource instanceof ClassPathResource || !resource.exists()) ? Collections.emptySet()
  284 | 				: Collections.singleton(new StandardConfigDataResource(reference, resource, true));
  285 | 	}
  286 | 
  287 | 	private Set<StandardConfigDataResource> resolvePatternEmptyDirectories(StandardConfigDataReference reference) {
  288 | 		String directory = reference.getDirectory();
  289 | 		Assert.state(directory != null, "'directory' must not be null");
  290 | 		Resource[] subdirectories = this.resourceLoader.getResources(directory, ResourceType.DIRECTORY);
  291 | 		ConfigDataLocation location = reference.getConfigDataLocation();
  292 | 		if (!location.isOptional() && ObjectUtils.isEmpty(subdirectories)) {
  293 | 			String message = String.format("Config data location '%s' contains no subdirectories", location);
  294 | 			throw new ConfigDataLocationNotFoundException(location, message, null);
  295 | 		}
  296 | 		return Arrays.stream(subdirectories)
  297 | 			.filter(Resource::exists)
  298 | 			.map((resource) -> new StandardConfigDataResource(reference, resource, true))
  299 | 			.collect(Collectors.toCollection(LinkedHashSet::new));
  300 | 	}
  301 | 
  302 | 	private List<StandardConfigDataResource> resolve(StandardConfigDataReference reference) {
  303 | 		if (!this.resourceLoader.isPattern(reference.getResourceLocation())) {
  304 | 			return resolveNonPattern(reference);
  305 | 		}
  306 | 		return resolvePattern(reference);
  307 | 	}
  308 | 
  309 | 	private List<StandardConfigDataResource> resolveNonPattern(StandardConfigDataReference reference) {
  310 | 		Resource resource = this.resourceLoader.getResource(reference.getResourceLocation());
  311 | 		if (!resource.exists() && reference.isSkippable()) {
  312 | 			logSkippingResource(reference);
  313 | 			return Collections.emptyList();
  314 | 		}
  315 | 		return Collections.singletonList(createConfigResourceLocation(reference, resource));
  316 | 	}
  317 | 
  318 | 	private List<StandardConfigDataResource> resolvePattern(StandardConfigDataReference reference) {
  319 | 		List<StandardConfigDataResource> resolved = new ArrayList<>();
  320 | 		for (Resource resource : this.resourceLoader.getResources(reference.getResourceLocation(), ResourceType.FILE)) {
  321 | 			if (!resource.exists() && reference.isSkippable()) {
  322 | 				logSkippingResource(reference);
  323 | 			}
  324 | 			else {
  325 | 				resolved.add(createConfigResourceLocation(reference, resource));
  326 | 			}
  327 | 		}
  328 | 		return resolved;
  329 | 	}
  330 | 
  331 | 	private void logSkippingResource(StandardConfigDataReference reference) {
  332 | 		this.logger.trace(LogMessage.format("Skipping missing resource %s", reference));
  333 | 	}
  334 | 
  335 | 	private StandardConfigDataResource createConfigResourceLocation(StandardConfigDataReference reference,
  336 | 			Resource resource) {
  337 | 		return new StandardConfigDataResource(reference, resource);
  338 | 	}
  339 | 
  340 | }
```
