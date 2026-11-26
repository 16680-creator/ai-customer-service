<!-- learning-docs source archive（机械抓取原文，禁止手改代码内容） -->
<!-- origin: org.springframework.boot:spring-boot:4.2.0 | path: org/springframework/boot/context/properties/source/ConfigurationPropertySources.java -->
<!-- 正文引用：[[源码:ConfigurationPropertySources.java#L起-L止]] ；代码块头部：(src: ConfigurationPropertySources.java#L起-L止) -->

# ConfigurationPropertySources.java

| 项 | 值 |
| --- | --- |
| 来源 | `org.springframework.boot:spring-boot:4.2.0` |
| 路径 | `org/springframework/boot/context/properties/source/ConfigurationPropertySources.java` |
| 行数 | 166 |
| 在线地址 | https://repo1.maven.org/maven2/org/springframework/boot/spring-boot/4.2.0/spring-boot-4.2.0-sources.jar |
| 抓取时间 | 2026-11-25T14:04:54.376Z |

```java
    1 | /*
    2 |  * Copyright 2012-present the original author or authors.
    3 |  */
    4 | 
    5 | package org.springframework.boot.context.properties.source;
    6 | 
    7 | import java.util.Collections;
    8 | import java.util.stream.Stream;
    9 | 
   10 | import org.jspecify.annotations.Nullable;
   11 | 
   12 | import org.springframework.core.env.ConfigurableEnvironment;
   13 | import org.springframework.core.env.ConfigurablePropertyResolver;
   14 | import org.springframework.core.env.Environment;
   15 | import org.springframework.core.env.MutablePropertySources;
   16 | import org.springframework.core.env.PropertyResolver;
   17 | import org.springframework.core.env.PropertySource;
   18 | import org.springframework.core.env.PropertySource.StubPropertySource;
   19 | import org.springframework.core.env.PropertySources;
   20 | import org.springframework.core.env.PropertySourcesPropertyResolver;
   21 | import org.springframework.lang.Contract;
   22 | import org.springframework.util.Assert;
   23 | 
   24 | /**
   25 |  * Provides access to {@link ConfigurationPropertySource ConfigurationPropertySources}.
   26 |  *
   27 |  * @author Phillip Webb
   28 |  * @since 2.0.0
   29 |  */
   30 | public final class ConfigurationPropertySources {
   31 | 
   32 | 	/**
   33 | 	 * The name of the {@link PropertySource} {@link #attach(Environment) adapter}.
   34 | 	 */
   35 | 	private static final String ATTACHED_PROPERTY_SOURCE_NAME = "configurationProperties";
   36 | 
   37 | 	private ConfigurationPropertySources() {
   38 | 	}
   39 | 
   40 | 	/**
   41 | 	 * Create a new {@link PropertyResolver} that resolves property values against an
   42 | 	 * underlying set of {@link PropertySources}. Provides an
   43 | 	 * {@link ConfigurationPropertySource} aware and optimized alternative to
   44 | 	 * {@link PropertySourcesPropertyResolver}.
   45 | 	 * @param propertySources the set of {@link PropertySource} objects to use
   46 | 	 * @return a {@link ConfigurablePropertyResolver} implementation
   47 | 	 * @since 2.5.0
   48 | 	 */
   49 | 	public static ConfigurablePropertyResolver createPropertyResolver(MutablePropertySources propertySources) {
   50 | 		return new ConfigurationPropertySourcesPropertyResolver(propertySources);
   51 | 	}
   52 | 
   53 | 	/**
   54 | 	 * Determines if the specific {@link PropertySource} is the
   55 | 	 * {@link ConfigurationPropertySource} that was {@link #attach(Environment) attached}
   56 | 	 * to the {@link Environment}.
   57 | 	 * @param propertySource the property source to test
   58 | 	 * @return {@code true} if this is the attached {@link ConfigurationPropertySource}
   59 | 	 */
   60 | 	public static boolean isAttachedConfigurationPropertySource(PropertySource<?> propertySource) {
   61 | 		return ATTACHED_PROPERTY_SOURCE_NAME.equals(propertySource.getName());
   62 | 	}
   63 | 
   64 | 	/**
   65 | 	 * Attach a {@link ConfigurationPropertySource} support to the specified
   66 | 	 * {@link Environment}. Adapts each {@link PropertySource} managed by the environment
   67 | 	 * to a {@link ConfigurationPropertySource} and allows classic
   68 | 	 * {@link PropertySourcesPropertyResolver} calls to resolve using
   69 | 	 * {@link ConfigurationPropertyName configuration property names}.
   70 | 	 * <p>
   71 | 	 * The attached resolver will dynamically track any additions or removals from the
   72 | 	 * underlying {@link Environment} property sources.
   73 | 	 * @param environment the source environment (must be an instance of
   74 | 	 * {@link ConfigurableEnvironment})
   75 | 	 * @see #get(Environment)
   76 | 	 */
   77 | 	public static void attach(Environment environment) {
   78 | 		Assert.isInstanceOf(ConfigurableEnvironment.class, environment);
   79 | 		MutablePropertySources sources = ((ConfigurableEnvironment) environment).getPropertySources();
   80 | 		PropertySource<?> attached = getAttached(sources);
   81 | 		if (!isUsingSources(attached, sources)) {
   82 | 			attached = new ConfigurationPropertySourcesPropertySource(ATTACHED_PROPERTY_SOURCE_NAME,
   83 | 					new SpringConfigurationPropertySources(sources));
   84 | 		}
   85 | 		sources.remove(ATTACHED_PROPERTY_SOURCE_NAME);
   86 | 		sources.addFirst(attached);
   87 | 	}
   88 | 
   89 | 	@Contract("null, _ -> false")
   90 | 	private static boolean isUsingSources(@Nullable PropertySource<?> attached, MutablePropertySources sources) {
   91 | 		return attached instanceof ConfigurationPropertySourcesPropertySource
   92 | 				&& ((SpringConfigurationPropertySources) attached.getSource()).isUsingSources(sources);
   93 | 	}
   94 | 
   95 | 	static @Nullable PropertySource<?> getAttached(@Nullable MutablePropertySources sources) {
   96 | 		return (sources != null) ? sources.get(ATTACHED_PROPERTY_SOURCE_NAME) : null;
   97 | 	}
   98 | 
   99 | 	/**
  100 | 	 * Return a set of {@link ConfigurationPropertySource} instances that have previously
  101 | 	 * been {@link #attach(Environment) attached} to the {@link Environment}.
  102 | 	 * @param environment the source environment (must be an instance of
  103 | 	 * {@link ConfigurableEnvironment})
  104 | 	 * @return an iterable set of configuration property sources
  105 | 	 * @throws IllegalStateException if not configuration property sources have been
  106 | 	 * attached
  107 | 	 */
  108 | 	public static Iterable<ConfigurationPropertySource> get(Environment environment) {
  109 | 		Assert.isInstanceOf(ConfigurableEnvironment.class, environment);
  110 | 		MutablePropertySources sources = ((ConfigurableEnvironment) environment).getPropertySources();
  111 | 		ConfigurationPropertySourcesPropertySource attached = (ConfigurationPropertySourcesPropertySource) sources
  112 | 			.get(ATTACHED_PROPERTY_SOURCE_NAME);
  113 | 		if (attached == null) {
  114 | 			return from(sources);
  115 | 		}
  116 | 		return attached.getSource();
  117 | 	}
  118 | 
  119 | 	/**
  120 | 	 * Return {@link Iterable} containing a single new {@link ConfigurationPropertySource}
  121 | 	 * adapted from the given Spring {@link PropertySource}. The single element can be
  122 | 	 * {@code null} if the source cannot be adapted.
  123 | 	 * @param source the Spring property source to adapt
  124 | 	 * @return an {@link Iterable} containing a single newly adapted
  125 | 	 * {@link SpringConfigurationPropertySource}
  126 | 	 */
  127 | 	public static Iterable<@Nullable ConfigurationPropertySource> from(PropertySource<?> source) {
  128 | 		return Collections.singleton(ConfigurationPropertySource.from(source));
  129 | 	}
  130 | 
  131 | 	/**
  132 | 	 * Return {@link Iterable} containing new {@link ConfigurationPropertySource}
  133 | 	 * instances adapted from the given Spring {@link PropertySource PropertySources}.
  134 | 	 * <p>
  135 | 	 * This method will flatten any nested property sources and will filter all
  136 | 	 * {@link StubPropertySource stub property sources}. Updates to the underlying source,
  137 | 	 * identified by changes in the sources returned by its iterator, will be
  138 | 	 * automatically tracked. The underlying source should be thread safe, for example a
  139 | 	 * {@link MutablePropertySources}
  140 | 	 * @param sources the Spring property sources to adapt
  141 | 	 * @return an {@link Iterable} containing newly adapted
  142 | 	 * {@link SpringConfigurationPropertySource} instances
  143 | 	 */
  144 | 	public static Iterable<ConfigurationPropertySource> from(Iterable<PropertySource<?>> sources) {
  145 | 		return new SpringConfigurationPropertySources(sources);
  146 | 	}
  147 | 
  148 | 	private static Stream<PropertySource<?>> streamPropertySources(PropertySources sources) {
  149 | 		return sources.stream()
  150 | 			.flatMap(ConfigurationPropertySources::flatten)
  151 | 			.filter(ConfigurationPropertySources::isIncluded);
  152 | 	}
  153 | 
  154 | 	private static Stream<PropertySource<?>> flatten(PropertySource<?> source) {
  155 | 		if (source.getSource() instanceof ConfigurableEnvironment configurableEnvironment) {
  156 | 			return streamPropertySources(configurableEnvironment.getPropertySources());
  157 | 		}
  158 | 		return Stream.of(source);
  159 | 	}
  160 | 
  161 | 	private static boolean isIncluded(PropertySource<?> source) {
  162 | 		return !(source instanceof StubPropertySource)
  163 | 				&& !(source instanceof ConfigurationPropertySourcesPropertySource);
  164 | 	}
  165 | 
  166 | }
```
