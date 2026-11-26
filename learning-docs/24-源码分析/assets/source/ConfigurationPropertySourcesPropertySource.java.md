<!-- learning-docs source archive（机械抓取原文，禁止手改代码内容） -->
<!-- origin: org.springframework.boot:spring-boot:4.2.0 | path: org/springframework/boot/context/properties/source/ConfigurationPropertySourcesPropertySource.java -->
<!-- 正文引用：[[源码:ConfigurationPropertySourcesPropertySource.java#L起-L止]] ；代码块头部：(src: ConfigurationPropertySourcesPropertySource.java#L起-L止) -->

# ConfigurationPropertySourcesPropertySource.java

| 项 | 值 |
| --- | --- |
| 来源 | `org.springframework.boot:spring-boot:4.2.0` |
| 路径 | `org/springframework/boot/context/properties/source/ConfigurationPropertySourcesPropertySource.java` |
| 行数 | 68 |
| 在线地址 | https://repo1.maven.org/maven2/org/springframework/boot/spring-boot/4.2.0/spring-boot-4.2.0-sources.jar |
| 抓取时间 | 2026-11-25T14:12:01.599Z |

```java
    1 | /*
    2 |  * Copyright 2012-present the original author or authors.
    3 |  */
    4 | 
    5 | package org.springframework.boot.context.properties.source;
    6 | 
    7 | import org.jspecify.annotations.Nullable;
    8 | 
    9 | import org.springframework.boot.origin.Origin;
   10 | import org.springframework.boot.origin.OriginLookup;
   11 | import org.springframework.core.env.Environment;
   12 | import org.springframework.core.env.PropertyResolver;
   13 | import org.springframework.core.env.PropertySource;
   14 | 
   15 | /**
   16 |  * {@link PropertySource} that exposes {@link ConfigurationPropertySource} instances so
   17 |  * that they can be used with a {@link PropertyResolver} or added to the
   18 |  * {@link Environment}.
   19 |  *
   20 |  * @author Phillip Webb
   21 |  * @author Madhura Bhave
   22 |  */
   23 | class ConfigurationPropertySourcesPropertySource extends PropertySource<Iterable<ConfigurationPropertySource>>
   24 | 		implements OriginLookup<String> {
   25 | 
   26 | 	ConfigurationPropertySourcesPropertySource(String name, Iterable<ConfigurationPropertySource> source) {
   27 | 		super(name, source);
   28 | 	}
   29 | 
   30 | 	@Override
   31 | 	public boolean containsProperty(String name) {
   32 | 		return findConfigurationProperty(name) != null;
   33 | 	}
   34 | 
   35 | 	@Override
   36 | 	public @Nullable Object getProperty(String name) {
   37 | 		ConfigurationProperty configurationProperty = findConfigurationProperty(name);
   38 | 		return (configurationProperty != null) ? configurationProperty.getValue() : null;
   39 | 	}
   40 | 
   41 | 	@Override
   42 | 	public @Nullable Origin getOrigin(String name) {
   43 | 		return Origin.from(findConfigurationProperty(name));
   44 | 	}
   45 | 
   46 | 	private @Nullable ConfigurationProperty findConfigurationProperty(String name) {
   47 | 		try {
   48 | 			return findConfigurationProperty(ConfigurationPropertyName.of(name, true));
   49 | 		}
   50 | 		catch (Exception ex) {
   51 | 			return null;
   52 | 		}
   53 | 	}
   54 | 
   55 | 	@Nullable ConfigurationProperty findConfigurationProperty(@Nullable ConfigurationPropertyName name) {
   56 | 		if (name == null) {
   57 | 			return null;
   58 | 		}
   59 | 		for (ConfigurationPropertySource configurationPropertySource : getSource()) {
   60 | 			ConfigurationProperty configurationProperty = configurationPropertySource.getConfigurationProperty(name);
   61 | 			if (configurationProperty != null) {
   62 | 				return configurationProperty;
   63 | 			}
   64 | 		}
   65 | 		return null;
   66 | 	}
   67 | 
   68 | }
```
