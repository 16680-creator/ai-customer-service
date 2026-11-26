<!-- learning-docs source archive（机械抓取原文，禁止手改代码内容） -->
<!-- origin: org.springframework.boot:spring-boot:4.2.0 | path: org/springframework/boot/context/properties/source/SpringConfigurationPropertySource.java -->
<!-- 正文引用：[[源码:SpringConfigurationPropertySource.java#L起-L止]] ；代码块头部：(src: SpringConfigurationPropertySource.java#L起-L止) -->

# SpringConfigurationPropertySource.java

| 项 | 值 |
| --- | --- |
| 来源 | `org.springframework.boot:spring-boot:4.2.0` |
| 路径 | `org/springframework/boot/context/properties/source/SpringConfigurationPropertySource.java` |
| 行数 | 197 |
| 在线地址 | https://repo1.maven.org/maven2/org/springframework/boot/spring-boot/4.2.0/spring-boot-4.2.0-sources.jar |
| 抓取时间 | 2026-11-25T14:04:53.960Z |

```java
    1 | /*
    2 |  * Copyright 2012-present the original author or authors.
    3 |  */
    4 | 
    5 | package org.springframework.boot.context.properties.source;
    6 | 
    7 | import java.util.Locale;
    8 | import java.util.Map;
    9 | import java.util.Random;
   10 | 
   11 | import org.jspecify.annotations.Nullable;
   12 | 
   13 | import org.springframework.boot.context.properties.source.ConfigurationPropertyName.Form;
   14 | import org.springframework.boot.origin.Origin;
   15 | import org.springframework.boot.origin.PropertySourceOrigin;
   16 | import org.springframework.core.env.EnumerablePropertySource;
   17 | import org.springframework.core.env.PropertySource;
   18 | import org.springframework.core.env.StandardEnvironment;
   19 | import org.springframework.core.env.SystemEnvironmentPropertySource;
   20 | import org.springframework.util.Assert;
   21 | 
   22 | /**
   23 |  * {@link ConfigurationPropertySource} backed by a non-enumerable Spring
   24 |  * {@link PropertySource} or a restricted {@link EnumerablePropertySource} implementation
   25 |  * (such as a security restricted {@code systemEnvironment} source). A
   26 |  * {@link PropertySource} is adapted with the help of a {@link PropertyMapper} which
   27 |  * provides the mapping rules for individual properties.
   28 |  * <p>
   29 |  * Each {@link ConfigurationPropertySource#getConfigurationProperty
   30 |  * getConfigurationProperty} call attempts to
   31 |  * {@link PropertyMapper#map(ConfigurationPropertyName) map} the
   32 |  * {@link ConfigurationPropertyName} to one or more {@code String} based names. This
   33 |  * allows fast property resolution for well-formed property sources.
   34 |  * <p>
   35 |  * When possible the {@link SpringIterableConfigurationPropertySource} will be used in
   36 |  * preference to this implementation since it supports full "relaxed" style resolution.
   37 |  *
   38 |  * @author Phillip Webb
   39 |  * @author Madhura Bhave
   40 |  * @see #from(PropertySource)
   41 |  * @see PropertyMapper
   42 |  * @see SpringIterableConfigurationPropertySource
   43 |  */
   44 | class SpringConfigurationPropertySource implements ConfigurationPropertySource {
   45 | 
   46 | 	private static final PropertyMapper[] DEFAULT_MAPPERS = { DefaultPropertyMapper.INSTANCE };
   47 | 
   48 | 	private static final PropertyMapper[] SYSTEM_ENVIRONMENT_MAPPERS = { SystemEnvironmentPropertyMapper.INSTANCE,
   49 | 			DefaultPropertyMapper.INSTANCE };
   50 | 
   51 | 	private final PropertySource<?> propertySource;
   52 | 
   53 | 	private final boolean systemEnvironmentSource;
   54 | 
   55 | 	private final PropertyMapper[] mappers;
   56 | 
   57 | 	/**
   58 | 	 * Create a new {@link SpringConfigurationPropertySource} implementation.
   59 | 	 * @param propertySource the source property source
   60 | 	 * @param systemEnvironmentSource if the source is from the system environment
   61 | 	 * @param mappers the property mappers
   62 | 	 */
   63 | 	SpringConfigurationPropertySource(PropertySource<?> propertySource, boolean systemEnvironmentSource,
   64 | 			PropertyMapper... mappers) {
   65 | 		Assert.notNull(propertySource, "'propertySource' must not be null");
   66 | 		Assert.isTrue(mappers.length > 0, "'mappers' must contain at least one item");
   67 | 		this.propertySource = propertySource;
   68 | 		this.systemEnvironmentSource = systemEnvironmentSource;
   69 | 		this.mappers = mappers;
   70 | 	}
   71 | 
   72 | 	@Override
   73 | 	public @Nullable ConfigurationProperty getConfigurationProperty(@Nullable ConfigurationPropertyName name) {
   74 | 		if (name == null) {
   75 | 			return null;
   76 | 		}
   77 | 		for (PropertyMapper mapper : this.mappers) {
   78 | 			try {
   79 | 				for (String candidate : mapper.map(name)) {
   80 | 					Object value = getPropertySourceProperty(candidate);
   81 | 					if (value != null) {
   82 | 						Origin origin = PropertySourceOrigin.get(this.propertySource, candidate);
   83 | 						return ConfigurationProperty.of(this, name, value, origin);
   84 | 					}
   85 | 				}
   86 | 			}
   87 | 			catch (Exception ex) {
   88 | 				// Ignore
   89 | 			}
   90 | 		}
   91 | 		return null;
   92 | 	}
   93 | 
   94 | 	protected final @Nullable Object getPropertySourceProperty(String name) {
   95 | 		// Save calls to SystemEnvironmentPropertySource.resolvePropertyName(...)
   96 | 		// since we've already done the mapping
   97 | 		PropertySource<?> propertySource = getPropertySource();
   98 | 		return (!this.systemEnvironmentSource) ? propertySource.getProperty(name)
   99 | 				: getSystemEnvironmentProperty(((SystemEnvironmentPropertySource) propertySource).getSource(), name);
  100 | 	}
  101 | 
  102 | 	@Nullable Object getSystemEnvironmentProperty(Map<String, Object> systemEnvironment, String name) {
  103 | 		Object value = systemEnvironment.get(name);
  104 | 		return (value != null) ? value : systemEnvironment.get(name.toLowerCase(Locale.ROOT));
  105 | 	}
  106 | 
  107 | 	@Override
  108 | 	public ConfigurationPropertyState containsDescendantOf(ConfigurationPropertyName name) {
  109 | 		PropertySource<?> source = getPropertySource();
  110 | 		Object underlyingSource = source.getSource();
  111 | 		if (underlyingSource instanceof Random) {
  112 | 			return containsDescendantOfForRandom("random", name);
  113 | 		}
  114 | 		if (underlyingSource instanceof PropertySource<?> underlyingPropertySource
  115 | 				&& underlyingPropertySource.getSource() instanceof Random) {
  116 | 			// Assume wrapped random sources use the source name as the prefix
  117 | 			return containsDescendantOfForRandom(source.getName(), name);
  118 | 		}
  119 | 		return ConfigurationPropertyState.UNKNOWN;
  120 | 	}
  121 | 
  122 | 	private static ConfigurationPropertyState containsDescendantOfForRandom(String prefix,
  123 | 			ConfigurationPropertyName name) {
  124 | 		if (name.getNumberOfElements() > 1 && name.getElement(0, Form.DASHED).equals(prefix)) {
  125 | 			return ConfigurationPropertyState.PRESENT;
  126 | 		}
  127 | 		return ConfigurationPropertyState.ABSENT;
  128 | 	}
  129 | 
  130 | 	@Override
  131 | 	public Object getUnderlyingSource() {
  132 | 		return this.propertySource;
  133 | 	}
  134 | 
  135 | 	protected PropertySource<?> getPropertySource() {
  136 | 		return this.propertySource;
  137 | 	}
  138 | 
  139 | 	protected final boolean isSystemEnvironmentSource() {
  140 | 		return this.systemEnvironmentSource;
  141 | 	}
  142 | 
  143 | 	protected final PropertyMapper[] getMappers() {
  144 | 		return this.mappers;
  145 | 	}
  146 | 
  147 | 	@Override
  148 | 	public String toString() {
  149 | 		return this.propertySource.toString();
  150 | 	}
  151 | 
  152 | 	/**
  153 | 	 * Create a new {@link SpringConfigurationPropertySource} for the specified
  154 | 	 * {@link PropertySource}.
  155 | 	 * @param source the source Spring {@link PropertySource}
  156 | 	 * @return a {@link SpringConfigurationPropertySource} or
  157 | 	 * {@link SpringIterableConfigurationPropertySource} instance
  158 | 	 */
  159 | 	static SpringConfigurationPropertySource from(PropertySource<?> source) {
  160 | 		Assert.notNull(source, "'source' must not be null");
  161 | 		boolean systemEnvironmentSource = isSystemEnvironmentPropertySource(source);
  162 | 		PropertyMapper[] mappers = (!systemEnvironmentSource) ? DEFAULT_MAPPERS : SYSTEM_ENVIRONMENT_MAPPERS;
  163 | 		return (!isFullEnumerable(source))
  164 | 				? new SpringConfigurationPropertySource(source, systemEnvironmentSource, mappers)
  165 | 				: new SpringIterableConfigurationPropertySource((EnumerablePropertySource<?>) source,
  166 | 						systemEnvironmentSource, mappers);
  167 | 	}
  168 | 
  169 | 	private static boolean isSystemEnvironmentPropertySource(PropertySource<?> source) {
  170 | 		String name = source.getName();
  171 | 		return (source instanceof SystemEnvironmentPropertySource)
  172 | 				&& (StandardEnvironment.SYSTEM_ENVIRONMENT_PROPERTY_SOURCE_NAME.equals(name)
  173 | 						|| name.endsWith("-" + StandardEnvironment.SYSTEM_ENVIRONMENT_PROPERTY_SOURCE_NAME));
  174 | 	}
  175 | 
  176 | 	private static boolean isFullEnumerable(PropertySource<?> source) {
  177 | 		PropertySource<?> rootSource = getRootSource(source);
  178 | 		if (rootSource.getSource() instanceof Map<?, ?> map) {
  179 | 			// Check we're not security restricted
  180 | 			try {
  181 | 				map.size();
  182 | 			}
  183 | 			catch (UnsupportedOperationException ex) {
  184 | 				return false;
  185 | 			}
  186 | 		}
  187 | 		return (source instanceof EnumerablePropertySource);
  188 | 	}
  189 | 
  190 | 	private static PropertySource<?> getRootSource(PropertySource<?> source) {
  191 | 		while (source.getSource() instanceof PropertySource<?> propertySource) {
  192 | 			source = propertySource;
  193 | 		}
  194 | 		return source;
  195 | 	}
  196 | 
  197 | }
```
