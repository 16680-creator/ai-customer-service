<!-- learning-docs source archive（机械抓取原文，禁止手改代码内容） -->
<!-- origin: org.springframework.boot:spring-boot-actuator:4.2.0 | path: org/springframework/boot/actuate/env/EnvironmentEndpoint.java -->
<!-- 正文引用：[[源码:EnvironmentEndpoint.java#L起-L止]] ；代码块头部：(src: EnvironmentEndpoint.java#L起-L止) -->

# EnvironmentEndpoint.java

| 项 | 值 |
| --- | --- |
| 来源 | `org.springframework.boot:spring-boot-actuator:4.2.0` |
| 路径 | `org/springframework/boot/actuate/env/EnvironmentEndpoint.java` |
| 行数 | 389 |
| 在线地址 | https://repo1.maven.org/maven2/org/springframework/boot/spring-boot-actuator/4.2.0/spring-boot-actuator-4.2.0-sources.jar |
| 抓取时间 | 2026-11-25T14:05:01.442Z |

```java
    1 | /*
    2 |  * Copyright 2012-present the original author or authors.
    3 |  */
    4 | 
    5 | package org.springframework.boot.actuate.env;
    6 | 
    7 | import java.util.ArrayList;
    8 | import java.util.Arrays;
    9 | import java.util.LinkedHashMap;
   10 | import java.util.List;
   11 | import java.util.Map;
   12 | import java.util.function.Predicate;
   13 | import java.util.regex.Pattern;
   14 | import java.util.regex.PatternSyntaxException;
   15 | import java.util.stream.Stream;
   16 | 
   17 | import com.fasterxml.jackson.annotation.JsonInclude;
   18 | import com.fasterxml.jackson.annotation.JsonInclude.Include;
   19 | import org.jspecify.annotations.Nullable;
   20 | 
   21 | import org.springframework.boot.actuate.endpoint.InvalidEndpointRequestException;
   22 | import org.springframework.boot.actuate.endpoint.OperationResponseBody;
   23 | import org.springframework.boot.actuate.endpoint.SanitizableData;
   24 | import org.springframework.boot.actuate.endpoint.Sanitizer;
   25 | import org.springframework.boot.actuate.endpoint.SanitizingFunction;
   26 | import org.springframework.boot.actuate.endpoint.Show;
   27 | import org.springframework.boot.actuate.endpoint.annotation.Endpoint;
   28 | import org.springframework.boot.actuate.endpoint.annotation.ReadOperation;
   29 | import org.springframework.boot.actuate.endpoint.annotation.Selector;
   30 | import org.springframework.boot.context.properties.bind.PlaceholdersResolver;
   31 | import org.springframework.boot.context.properties.bind.PropertySourcesPlaceholdersResolver;
   32 | import org.springframework.boot.context.properties.source.ConfigurationPropertySources;
   33 | import org.springframework.boot.origin.Origin;
   34 | import org.springframework.boot.origin.OriginLookup;
   35 | import org.springframework.core.env.CompositePropertySource;
   36 | import org.springframework.core.env.ConfigurableEnvironment;
   37 | import org.springframework.core.env.EnumerablePropertySource;
   38 | import org.springframework.core.env.Environment;
   39 | import org.springframework.core.env.MutablePropertySources;
   40 | import org.springframework.core.env.PropertySource;
   41 | import org.springframework.core.env.StandardEnvironment;
   42 | import org.springframework.util.ClassUtils;
   43 | import org.springframework.util.StringUtils;
   44 | 
   45 | /**
   46 |  * {@link Endpoint @Endpoint} to expose {@link ConfigurableEnvironment environment}
   47 |  * information.
   48 |  *
   49 |  * @author Dave Syer
   50 |  * @author Phillip Webb
   51 |  * @author Christian Dupuis
   52 |  * @author Madhura Bhave
   53 |  * @author Stephane Nicoll
   54 |  * @author Scott Frederick
   55 |  * @since 2.0.0
   56 |  */
   57 | @Endpoint(id = "env")
   58 | public class EnvironmentEndpoint {
   59 | 
   60 | 	private final Sanitizer sanitizer;
   61 | 
   62 | 	private final Environment environment;
   63 | 
   64 | 	private final Show showValues;
   65 | 
   66 | 	public EnvironmentEndpoint(Environment environment, Iterable<SanitizingFunction> sanitizingFunctions,
   67 | 			Show showValues) {
   68 | 		this.environment = environment;
   69 | 		this.sanitizer = new Sanitizer(sanitizingFunctions);
   70 | 		this.showValues = showValues;
   71 | 	}
   72 | 
   73 | 	@ReadOperation
   74 | 	public EnvironmentDescriptor environment(@Nullable String pattern) {
   75 | 		boolean showUnsanitized = this.showValues.isShown(true);
   76 | 		return getEnvironmentDescriptor(pattern, showUnsanitized);
   77 | 	}
   78 | 
   79 | 	EnvironmentDescriptor getEnvironmentDescriptor(@Nullable String pattern, boolean showUnsanitized) {
   80 | 		if (StringUtils.hasText(pattern)) {
   81 | 			return getEnvironmentDescriptor(parsePattern(pattern).asPredicate(), showUnsanitized);
   82 | 		}
   83 | 		return getEnvironmentDescriptor((name) -> true, showUnsanitized);
   84 | 	}
   85 | 
   86 | 	private Pattern parsePattern(String pattern) {
   87 | 		try {
   88 | 			return Pattern.compile(pattern);
   89 | 		}
   90 | 		catch (PatternSyntaxException ex) {
   91 | 			throw new InvalidEndpointRequestException("Failed to parse regular expression",
   92 | 					"Invalid regular expression");
   93 | 		}
   94 | 	}
   95 | 
   96 | 	private EnvironmentDescriptor getEnvironmentDescriptor(Predicate<String> propertyNamePredicate,
   97 | 			boolean showUnsanitized) {
   98 | 		List<PropertySourceDescriptor> propertySources = new ArrayList<>();
   99 | 		getPropertySourcesAsMap().forEach((sourceName, source) -> {
  100 | 			if (source instanceof EnumerablePropertySource) {
  101 | 				propertySources.add(describeSource(sourceName, (EnumerablePropertySource<?>) source,
  102 | 						propertyNamePredicate, showUnsanitized));
  103 | 			}
  104 | 		});
  105 | 		return new EnvironmentDescriptor(Arrays.asList(this.environment.getActiveProfiles()),
  106 | 				Arrays.asList(this.environment.getDefaultProfiles()), propertySources);
  107 | 	}
  108 | 
  109 | 	@ReadOperation
  110 | 	public EnvironmentEntryDescriptor environmentEntry(@Selector String toMatch) {
  111 | 		boolean showUnsanitized = this.showValues.isShown(true);
  112 | 		return getEnvironmentEntryDescriptor(toMatch, showUnsanitized);
  113 | 	}
  114 | 
  115 | 	EnvironmentEntryDescriptor getEnvironmentEntryDescriptor(String propertyName, boolean showUnsanitized) {
  116 | 		Map<String, @Nullable PropertyValueDescriptor> descriptors = getPropertySourceDescriptors(propertyName,
  117 | 				showUnsanitized);
  118 | 		PropertySummaryDescriptor summary = getPropertySummaryDescriptor(descriptors);
  119 | 		return new EnvironmentEntryDescriptor(summary, Arrays.asList(this.environment.getActiveProfiles()),
  120 | 				Arrays.asList(this.environment.getDefaultProfiles()), toPropertySourceDescriptors(descriptors));
  121 | 	}
  122 | 
  123 | 	private List<PropertySourceEntryDescriptor> toPropertySourceDescriptors(
  124 | 			Map<String, @Nullable PropertyValueDescriptor> descriptors) {
  125 | 		List<PropertySourceEntryDescriptor> result = new ArrayList<>();
  126 | 		descriptors.forEach((name, property) -> result.add(new PropertySourceEntryDescriptor(name, property)));
  127 | 		return result;
  128 | 	}
  129 | 
  130 | 	private @Nullable PropertySummaryDescriptor getPropertySummaryDescriptor(
  131 | 			Map<String, @Nullable PropertyValueDescriptor> descriptors) {
  132 | 		for (Map.Entry<String, @Nullable PropertyValueDescriptor> entry : descriptors.entrySet()) {
  133 | 			if (entry.getValue() != null) {
  134 | 				return new PropertySummaryDescriptor(entry.getKey(), entry.getValue().getValue());
  135 | 			}
  136 | 		}
  137 | 		return null;
  138 | 	}
  139 | 
  140 | 	private Map<String, @Nullable PropertyValueDescriptor> getPropertySourceDescriptors(String propertyName,
  141 | 			boolean showUnsanitized) {
  142 | 		Map<String, @Nullable PropertyValueDescriptor> propertySources = new LinkedHashMap<>();
  143 | 		getPropertySourcesAsMap().forEach((sourceName, source) -> propertySources.put(sourceName,
  144 | 				source.containsProperty(propertyName) ? describeValueOf(propertyName, source, showUnsanitized) : null));
  145 | 		return propertySources;
  146 | 	}
  147 | 
  148 | 	private PropertySourceDescriptor describeSource(String sourceName, EnumerablePropertySource<?> source,
  149 | 			Predicate<String> namePredicate, boolean showUnsanitized) {
  150 | 		Map<String, PropertyValueDescriptor> properties = new LinkedHashMap<>();
  151 | 		Stream.of(source.getPropertyNames())
  152 | 			.filter(namePredicate)
  153 | 			.forEach((name) -> properties.put(name, describeValueOf(name, source, showUnsanitized)));
  154 | 		return new PropertySourceDescriptor(sourceName, properties);
  155 | 	}
  156 | 
  157 | 	@SuppressWarnings("unchecked")
  158 | 	private PropertyValueDescriptor describeValueOf(String name, PropertySource<?> source, boolean showUnsanitized) {
  159 | 		PlaceholdersResolver resolver = new PropertySourcesPlaceholdersResolver(getPropertySources());
  160 | 		Object resolved = resolver.resolvePlaceholders(source.getProperty(name));
  161 | 		Origin origin = ((source instanceof OriginLookup) ? ((OriginLookup<Object>) source).getOrigin(name) : null);
  162 | 		Object sanitizedValue = sanitize(source, name, resolved, showUnsanitized);
  163 | 		return new PropertyValueDescriptor(stringifyIfNecessary(sanitizedValue), origin);
  164 | 	}
  165 | 
  166 | 	private Map<String, PropertySource<?>> getPropertySourcesAsMap() {
  167 | 		Map<String, PropertySource<?>> map = new LinkedHashMap<>();
  168 | 		for (PropertySource<?> source : getPropertySources()) {
  169 | 			if (!ConfigurationPropertySources.isAttachedConfigurationPropertySource(source)) {
  170 | 				extract("", map, source);
  171 | 			}
  172 | 		}
  173 | 		return map;
  174 | 	}
  175 | 
  176 | 	private MutablePropertySources getPropertySources() {
  177 | 		if (this.environment instanceof ConfigurableEnvironment configurableEnvironment) {
  178 | 			return configurableEnvironment.getPropertySources();
  179 | 		}
  180 | 		return new StandardEnvironment().getPropertySources();
  181 | 	}
  182 | 
  183 | 	private void extract(String root, Map<String, PropertySource<?>> map, PropertySource<?> source) {
  184 | 		if (source instanceof CompositePropertySource compositePropertySource) {
  185 | 			for (PropertySource<?> nest : compositePropertySource.getPropertySources()) {
  186 | 				extract(source.getName() + ":", map, nest);
  187 | 			}
  188 | 		}
  189 | 		else {
  190 | 			map.put(root + source.getName(), source);
  191 | 		}
  192 | 	}
  193 | 
  194 | 	private @Nullable Object sanitize(PropertySource<?> source, String name, @Nullable Object value,
  195 | 			boolean showUnsanitized) {
  196 | 		return this.sanitizer.sanitize(new SanitizableData(source, name, value), showUnsanitized);
  197 | 	}
  198 | 
  199 | 	protected @Nullable Object stringifyIfNecessary(@Nullable Object value) {
  200 | 		if (value == null || ClassUtils.isPrimitiveOrWrapper(value.getClass()) || value instanceof Number) {
  201 | 			return value;
  202 | 		}
  203 | 		if (value instanceof CharSequence) {
  204 | 			return value.toString();
  205 | 		}
  206 | 		return "Complex property type " + value.getClass().getName();
  207 | 	}
  208 | 
  209 | 	/**
  210 | 	 * Description of an {@link Environment}.
  211 | 	 */
  212 | 	public static final class EnvironmentDescriptor implements OperationResponseBody {
  213 | 
  214 | 		private final List<String> activeProfiles;
  215 | 
  216 | 		private final List<String> defaultProfiles;
  217 | 
  218 | 		private final List<PropertySourceDescriptor> propertySources;
  219 | 
  220 | 		private EnvironmentDescriptor(List<String> activeProfiles, List<String> defaultProfiles,
  221 | 				List<PropertySourceDescriptor> propertySources) {
  222 | 			this.activeProfiles = activeProfiles;
  223 | 			this.defaultProfiles = defaultProfiles;
  224 | 			this.propertySources = propertySources;
  225 | 		}
  226 | 
  227 | 		public List<String> getActiveProfiles() {
  228 | 			return this.activeProfiles;
  229 | 		}
  230 | 
  231 | 		public List<String> getDefaultProfiles() {
  232 | 			return this.defaultProfiles;
  233 | 		}
  234 | 
  235 | 		public List<PropertySourceDescriptor> getPropertySources() {
  236 | 			return this.propertySources;
  237 | 		}
  238 | 
  239 | 	}
  240 | 
  241 | 	/**
  242 | 	 * Description of an entry of the {@link Environment}.
  243 | 	 */
  244 | 	@JsonInclude(Include.NON_NULL)
  245 | 	public static final class EnvironmentEntryDescriptor {
  246 | 
  247 | 		private final @Nullable PropertySummaryDescriptor property;
  248 | 
  249 | 		private final List<String> activeProfiles;
  250 | 
  251 | 		private final List<String> defaultProfiles;
  252 | 
  253 | 		private final List<PropertySourceEntryDescriptor> propertySources;
  254 | 
  255 | 		EnvironmentEntryDescriptor(@Nullable PropertySummaryDescriptor property, List<String> activeProfiles,
  256 | 				List<String> defaultProfiles, List<PropertySourceEntryDescriptor> propertySources) {
  257 | 			this.property = property;
  258 | 			this.activeProfiles = activeProfiles;
  259 | 			this.defaultProfiles = defaultProfiles;
  260 | 			this.propertySources = propertySources;
  261 | 		}
  262 | 
  263 | 		public @Nullable PropertySummaryDescriptor getProperty() {
  264 | 			return this.property;
  265 | 		}
  266 | 
  267 | 		public List<String> getActiveProfiles() {
  268 | 			return this.activeProfiles;
  269 | 		}
  270 | 
  271 | 		public List<String> getDefaultProfiles() {
  272 | 			return this.defaultProfiles;
  273 | 		}
  274 | 
  275 | 		public List<PropertySourceEntryDescriptor> getPropertySources() {
  276 | 			return this.propertySources;
  277 | 		}
  278 | 
  279 | 	}
  280 | 
  281 | 	/**
  282 | 	 * Description of a particular entry of the {@link Environment}.
  283 | 	 */
  284 | 	@JsonInclude(Include.NON_NULL)
  285 | 	public static final class PropertySummaryDescriptor {
  286 | 
  287 | 		private final String source;
  288 | 
  289 | 		private final @Nullable Object value;
  290 | 
  291 | 		public PropertySummaryDescriptor(String source, @Nullable Object value) {
  292 | 			this.source = source;
  293 | 			this.value = value;
  294 | 		}
  295 | 
  296 | 		public String getSource() {
  297 | 			return this.source;
  298 | 		}
  299 | 
  300 | 		public @Nullable Object getValue() {
  301 | 			return this.value;
  302 | 		}
  303 | 
  304 | 	}
  305 | 
  306 | 	/**
  307 | 	 * Description of a {@link PropertySource}.
  308 | 	 */
  309 | 	public static final class PropertySourceDescriptor {
  310 | 
  311 | 		private final String name;
  312 | 
  313 | 		private final Map<String, PropertyValueDescriptor> properties;
  314 | 
  315 | 		private PropertySourceDescriptor(String name, Map<String, PropertyValueDescriptor> properties) {
  316 | 			this.name = name;
  317 | 			this.properties = properties;
  318 | 		}
  319 | 
  320 | 		public String getName() {
  321 | 			return this.name;
  322 | 		}
  323 | 
  324 | 		public Map<String, PropertyValueDescriptor> getProperties() {
  325 | 			return this.properties;
  326 | 		}
  327 | 
  328 | 	}
  329 | 
  330 | 	/**
  331 | 	 * Description of a particular entry of {@link PropertySource}.
  332 | 	 */
  333 | 	@JsonInclude(Include.NON_NULL)
  334 | 	public static final class PropertySourceEntryDescriptor {
  335 | 
  336 | 		private final String name;
  337 | 
  338 | 		private final @Nullable PropertyValueDescriptor property;
  339 | 
  340 | 		private PropertySourceEntryDescriptor(String name, @Nullable PropertyValueDescriptor property) {
  341 | 			this.name = name;
  342 | 			this.property = property;
  343 | 		}
  344 | 
  345 | 		public String getName() {
  346 | 			return this.name;
  347 | 		}
  348 | 
  349 | 		public @Nullable PropertyValueDescriptor getProperty() {
  350 | 			return this.property;
  351 | 		}
  352 | 
  353 | 	}
  354 | 
  355 | 	/**
  356 | 	 * Description of a property's value, including its origin if available.
  357 | 	 */
  358 | 	@JsonInclude(Include.NON_NULL)
  359 | 	public static final class PropertyValueDescriptor {
  360 | 
  361 | 		private final @Nullable Object value;
  362 | 
  363 | 		private final @Nullable String origin;
  364 | 
  365 | 		private final String @Nullable [] originParents;
  366 | 
  367 | 		private PropertyValueDescriptor(@Nullable Object value, @Nullable Origin origin) {
  368 | 			this.value = value;
  369 | 			this.origin = (origin != null) ? origin.toString() : null;
  370 | 			List<Origin> originParents = Origin.parentsFrom(origin);
  371 | 			this.originParents = originParents.isEmpty() ? null
  372 | 					: originParents.stream().map(Object::toString).toArray(String[]::new);
  373 | 		}
  374 | 
  375 | 		public @Nullable Object getValue() {
  376 | 			return this.value;
  377 | 		}
  378 | 
  379 | 		public @Nullable String getOrigin() {
  380 | 			return this.origin;
  381 | 		}
  382 | 
  383 | 		public String @Nullable [] getOriginParents() {
  384 | 			return this.originParents;
  385 | 		}
  386 | 
  387 | 	}
  388 | 
  389 | }
```
