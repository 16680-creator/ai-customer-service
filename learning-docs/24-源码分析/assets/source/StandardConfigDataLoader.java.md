<!-- learning-docs source archive（机械抓取原文，禁止手改代码内容） -->
<!-- origin: org.springframework.boot:spring-boot:4.2.0 | path: org/springframework/boot/context/config/StandardConfigDataLoader.java -->
<!-- 正文引用：[[源码:StandardConfigDataLoader.java#L起-L止]] ；代码块头部：(src: StandardConfigDataLoader.java#L起-L止) -->

# StandardConfigDataLoader.java

| 项 | 值 |
| --- | --- |
| 来源 | `org.springframework.boot:spring-boot:4.2.0` |
| 路径 | `org/springframework/boot/context/config/StandardConfigDataLoader.java` |
| 行数 | 48 |
| 在线地址 | https://repo1.maven.org/maven2/org/springframework/boot/spring-boot/4.2.0/spring-boot-4.2.0-sources.jar |
| 抓取时间 | 2026-11-25T14:04:55.459Z |

```java
    1 | /*
    2 |  * Copyright 2012-present the original author or authors.
    3 |  */
    4 | 
    5 | package org.springframework.boot.context.config;
    6 | 
    7 | import java.io.IOException;
    8 | import java.util.List;
    9 | 
   10 | import org.springframework.boot.context.config.ConfigData.Option;
   11 | import org.springframework.boot.context.config.ConfigData.PropertySourceOptions;
   12 | import org.springframework.boot.origin.Origin;
   13 | import org.springframework.boot.origin.OriginTrackedResource;
   14 | import org.springframework.core.env.PropertySource;
   15 | import org.springframework.core.io.Resource;
   16 | 
   17 | /**
   18 |  * {@link ConfigDataLoader} for {@link Resource} backed locations.
   19 |  *
   20 |  * @author Phillip Webb
   21 |  * @author Madhura Bhave
   22 |  * @since 2.4.0
   23 |  */
   24 | public class StandardConfigDataLoader implements ConfigDataLoader<StandardConfigDataResource> {
   25 | 
   26 | 	private static final PropertySourceOptions PROFILE_SPECIFIC = PropertySourceOptions.always(Option.PROFILE_SPECIFIC);
   27 | 
   28 | 	private static final PropertySourceOptions NON_PROFILE_SPECIFIC = PropertySourceOptions.ALWAYS_NONE;
   29 | 
   30 | 	@Override
   31 | 	public ConfigData load(ConfigDataLoaderContext context, StandardConfigDataResource resource)
   32 | 			throws IOException, ConfigDataNotFoundException {
   33 | 		if (resource.isEmptyDirectory()) {
   34 | 			return ConfigData.EMPTY;
   35 | 		}
   36 | 		ConfigDataResourceNotFoundException.throwIfDoesNotExist(resource, resource.getResource());
   37 | 		StandardConfigDataReference reference = resource.getReference();
   38 | 		Resource originTrackedResource = OriginTrackedResource.of(resource.getResource(),
   39 | 				Origin.from(reference.getConfigDataLocation()));
   40 | 		String name = String.format("Config resource '%s' via location '%s'", resource,
   41 | 				reference.getConfigDataLocation());
   42 | 		List<PropertySource<?>> propertySources = reference.getPropertySourceLoader()
   43 | 			.load(name, originTrackedResource, reference.getEncoding());
   44 | 		PropertySourceOptions options = (resource.getProfile() != null) ? PROFILE_SPECIFIC : NON_PROFILE_SPECIFIC;
   45 | 		return new ConfigData(propertySources, options);
   46 | 	}
   47 | 
   48 | }
```
