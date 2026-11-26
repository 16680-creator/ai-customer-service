<!-- learning-docs source archive（机械抓取原文，禁止手改代码内容） -->
<!-- origin: org.springframework.boot:spring-boot:4.2.0 | path: org/springframework/boot/context/properties/source/SystemEnvironmentPropertyMapper.java -->
<!-- 正文引用：[[源码:SystemEnvironmentPropertyMapper.java#L起-L止]] ；代码块头部：(src: SystemEnvironmentPropertyMapper.java#L起-L止) -->

# SystemEnvironmentPropertyMapper.java

| 项 | 值 |
| --- | --- |
| 来源 | `org.springframework.boot:spring-boot:4.2.0` |
| 路径 | `org/springframework/boot/context/properties/source/SystemEnvironmentPropertyMapper.java` |
| 行数 | 95 |
| 在线地址 | https://repo1.maven.org/maven2/org/springframework/boot/spring-boot/4.2.0/spring-boot-4.2.0-sources.jar |
| 抓取时间 | 2026-11-25T14:04:54.112Z |

```java
    1 | /*
    2 |  * Copyright 2012-present the original author or authors.
    3 |  */
    4 | 
    5 | package org.springframework.boot.context.properties.source;
    6 | 
    7 | import java.util.ArrayList;
    8 | import java.util.List;
    9 | import java.util.Locale;
   10 | import java.util.Map;
   11 | import java.util.function.BiPredicate;
   12 | 
   13 | import org.springframework.boot.context.properties.source.ConfigurationPropertyName.ToStringFormat;
   14 | import org.springframework.util.ConcurrentReferenceHashMap;
   15 | 
   16 | /**
   17 |  * {@link PropertyMapper} for system environment variables. Names are mapped by removing
   18 |  * invalid characters, converting to lower case and replacing "{@code _}" with
   19 |  * "{@code .}". For example, "{@code SERVER_PORT}" is mapped to "{@code server.port}". In
   20 |  * addition, numeric elements are mapped to indexes (e.g. "{@code HOST_0}" is mapped to
   21 |  * "{@code host[0]}").
   22 |  *
   23 |  * @author Phillip Webb
   24 |  * @author Madhura Bhave
   25 |  * @see PropertyMapper
   26 |  * @see SpringConfigurationPropertySource
   27 |  */
   28 | final class SystemEnvironmentPropertyMapper implements PropertyMapper {
   29 | 
   30 | 	public static final PropertyMapper INSTANCE = new SystemEnvironmentPropertyMapper();
   31 | 
   32 | 	private final Map<String, ConfigurationPropertyName> propertySourceNameCache = new ConcurrentReferenceHashMap<>();
   33 | 
   34 | 	@Override
   35 | 	public List<String> map(ConfigurationPropertyName configurationPropertyName) {
   36 | 		List<String> mapped = new ArrayList<>(4);
   37 | 		addIfMissing(mapped, configurationPropertyName.toString(ToStringFormat.SYSTEM_ENVIRONMENT, true));
   38 | 		addIfMissing(mapped, configurationPropertyName.toString(ToStringFormat.LEGACY_SYSTEM_ENVIRONMENT, true));
   39 | 		addIfMissing(mapped, configurationPropertyName.toString(ToStringFormat.SYSTEM_ENVIRONMENT, false));
   40 | 		addIfMissing(mapped, configurationPropertyName.toString(ToStringFormat.LEGACY_SYSTEM_ENVIRONMENT, false));
   41 | 		return mapped;
   42 | 	}
   43 | 
   44 | 	private void addIfMissing(List<String> list, String value) {
   45 | 		if (!list.contains(value)) {
   46 | 			list.add(value);
   47 | 		}
   48 | 	}
   49 | 
   50 | 	@Override
   51 | 	public ConfigurationPropertyName map(String propertySourceName) {
   52 | 		ConfigurationPropertyName configurationPropertyName = this.propertySourceNameCache.get(propertySourceName);
   53 | 		if (configurationPropertyName == null) {
   54 | 			configurationPropertyName = convertName(propertySourceName);
   55 | 			this.propertySourceNameCache.put(propertySourceName, configurationPropertyName);
   56 | 		}
   57 | 		return configurationPropertyName;
   58 | 	}
   59 | 
   60 | 	private ConfigurationPropertyName convertName(String propertySourceName) {
   61 | 		try {
   62 | 			return ConfigurationPropertyName.adapt(propertySourceName, '_', this::processElementValue);
   63 | 		}
   64 | 		catch (Exception ex) {
   65 | 			return ConfigurationPropertyName.EMPTY;
   66 | 		}
   67 | 	}
   68 | 
   69 | 	private CharSequence processElementValue(CharSequence value) {
   70 | 		String result = value.toString().toLowerCase(Locale.ENGLISH);
   71 | 		return isNumber(result) ? "[" + result + "]" : result;
   72 | 	}
   73 | 
   74 | 	private static boolean isNumber(String string) {
   75 | 		return string.chars().allMatch(Character::isDigit);
   76 | 	}
   77 | 
   78 | 	@Override
   79 | 	public BiPredicate<ConfigurationPropertyName, ConfigurationPropertyName> getAncestorOfCheck() {
   80 | 		return this::isAncestorOf;
   81 | 	}
   82 | 
   83 | 	private boolean isAncestorOf(ConfigurationPropertyName name, ConfigurationPropertyName candidate) {
   84 | 		return name.isAncestorOf(candidate) || isLegacyAncestorOf(name, candidate);
   85 | 	}
   86 | 
   87 | 	private boolean isLegacyAncestorOf(ConfigurationPropertyName name, ConfigurationPropertyName candidate) {
   88 | 		if (!name.hasDashedElement()) {
   89 | 			return false;
   90 | 		}
   91 | 		ConfigurationPropertyName legacyCompatibleName = name.asSystemEnvironmentLegacyName();
   92 | 		return legacyCompatibleName != null && legacyCompatibleName.isAncestorOf(candidate);
   93 | 	}
   94 | 
   95 | }
```
