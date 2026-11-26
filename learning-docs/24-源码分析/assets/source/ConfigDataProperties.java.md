<!-- learning-docs source archive（机械抓取原文，禁止手改代码内容） -->
<!-- origin: org.springframework.boot:spring-boot:4.2.0 | path: org/springframework/boot/context/config/ConfigDataProperties.java -->
<!-- 正文引用：[[源码:ConfigDataProperties.java#L起-L止]] ；代码块头部：(src: ConfigDataProperties.java#L起-L止) -->

# ConfigDataProperties.java

| 项 | 值 |
| --- | --- |
| 来源 | `org.springframework.boot:spring-boot:4.2.0` |
| 路径 | `org/springframework/boot/context/config/ConfigDataProperties.java` |
| 行数 | 136 |
| 在线地址 | https://repo1.maven.org/maven2/org/springframework/boot/spring-boot/4.2.0/spring-boot-4.2.0-sources.jar |
| 抓取时间 | 2026-11-25T14:04:55.589Z |

```java
    1 | /*
    2 |  * Copyright 2012-present the original author or authors.
    3 |  */
    4 | 
    5 | package org.springframework.boot.context.config;
    6 | 
    7 | import java.util.Collections;
    8 | import java.util.List;
    9 | import java.util.function.Predicate;
   10 | 
   11 | import org.jspecify.annotations.Nullable;
   12 | 
   13 | import org.springframework.boot.cloud.CloudPlatform;
   14 | import org.springframework.boot.context.properties.bind.Bindable;
   15 | import org.springframework.boot.context.properties.bind.Binder;
   16 | import org.springframework.boot.context.properties.bind.Name;
   17 | import org.springframework.boot.context.properties.source.ConfigurationPropertyName;
   18 | import org.springframework.util.Assert;
   19 | import org.springframework.util.ObjectUtils;
   20 | 
   21 | /**
   22 |  * Bound properties used when working with {@link ConfigData}.
   23 |  *
   24 |  * @author Phillip Webb
   25 |  * @author Madhura Bhave
   26 |  * @author Yanming Zhou
   27 |  */
   28 | class ConfigDataProperties {
   29 | 
   30 | 	private static final ConfigurationPropertyName NAME = ConfigurationPropertyName.of("spring.config");
   31 | 
   32 | 	private static final Bindable<ConfigDataProperties> BINDABLE_PROPERTIES = Bindable.of(ConfigDataProperties.class);
   33 | 
   34 | 	private final List<ConfigDataLocation> imports;
   35 | 
   36 | 	private final @Nullable Activate activate;
   37 | 
   38 | 	/**
   39 | 	 * Create a new {@link ConfigDataProperties} instance.
   40 | 	 * @param imports the imports requested
   41 | 	 * @param activate the activate properties
   42 | 	 */
   43 | 	ConfigDataProperties(@Name("import") @Nullable List<ConfigDataLocation> imports, @Nullable Activate activate) {
   44 | 		this.imports = (imports != null) ? imports.stream().filter(ConfigDataLocation::isNotEmpty).toList()
   45 | 				: Collections.emptyList();
   46 | 		this.activate = activate;
   47 | 	}
   48 | 
   49 | 	/**
   50 | 	 * Return any additional imports requested.
   51 | 	 * @return the requested imports
   52 | 	 */
   53 | 	List<ConfigDataLocation> getImports() {
   54 | 		return this.imports;
   55 | 	}
   56 | 
   57 | 	/**
   58 | 	 * Return {@code true} if the properties indicate that the config data property source
   59 | 	 * is active for the given activation context.
   60 | 	 * @param activationContext the activation context
   61 | 	 * @return {@code true} if the config data property source is active
   62 | 	 */
   63 | 	boolean isActive(@Nullable ConfigDataActivationContext activationContext) {
   64 | 		return this.activate == null || this.activate.isActive(activationContext);
   65 | 	}
   66 | 
   67 | 	/**
   68 | 	 * Return a new variant of these properties without any imports.
   69 | 	 * @return a new {@link ConfigDataProperties} instance
   70 | 	 */
   71 | 	ConfigDataProperties withoutImports() {
   72 | 		return new ConfigDataProperties(null, this.activate);
   73 | 	}
   74 | 
   75 | 	/**
   76 | 	 * Factory method used to create {@link ConfigDataProperties} from the given
   77 | 	 * {@link Binder}.
   78 | 	 * @param binder the binder used to bind the properties
   79 | 	 * @return a {@link ConfigDataProperties} instance or {@code null}
   80 | 	 */
   81 | 	static @Nullable ConfigDataProperties get(Binder binder) {
   82 | 		return binder.bind(NAME, BINDABLE_PROPERTIES, new ConfigDataLocationBindHandler()).orElse(null);
   83 | 	}
   84 | 
   85 | 	/**
   86 | 	 * Activate properties used to determine when a config data property source is active.
   87 | 	 */
   88 | 	static class Activate {
   89 | 
   90 | 		private final @Nullable CloudPlatform onCloudPlatform;
   91 | 
   92 | 		private final String @Nullable [] onProfile;
   93 | 
   94 | 		/**
   95 | 		 * Create a new {@link Activate} instance.
   96 | 		 * @param onCloudPlatform the cloud platform required for activation
   97 | 		 * @param onProfile the profile expression required for activation
   98 | 		 */
   99 | 		Activate(@Nullable CloudPlatform onCloudPlatform, String @Nullable [] onProfile) {
  100 | 			this.onProfile = onProfile;
  101 | 			this.onCloudPlatform = onCloudPlatform;
  102 | 		}
  103 | 
  104 | 		/**
  105 | 		 * Return {@code true} if the properties indicate that the config data property
  106 | 		 * source is active for the given activation context.
  107 | 		 * @param activationContext the activation context
  108 | 		 * @return {@code true} if the config data property source is active
  109 | 		 */
  110 | 		boolean isActive(@Nullable ConfigDataActivationContext activationContext) {
  111 | 			if (activationContext == null) {
  112 | 				return false;
  113 | 			}
  114 | 			CloudPlatform cloudPlatform = activationContext.getCloudPlatform();
  115 | 			boolean activate = isActive((cloudPlatform != null) ? cloudPlatform : CloudPlatform.NONE);
  116 | 			activate = activate && isActive(activationContext.getProfiles());
  117 | 			return activate;
  118 | 		}
  119 | 
  120 | 		private boolean isActive(CloudPlatform cloudPlatform) {
  121 | 			return this.onCloudPlatform == null || this.onCloudPlatform == cloudPlatform;
  122 | 		}
  123 | 
  124 | 		private boolean isActive(@Nullable Profiles profiles) {
  125 | 			return ObjectUtils.isEmpty(this.onProfile)
  126 | 					|| (profiles != null && matchesActiveProfiles(profiles::isAccepted));
  127 | 		}
  128 | 
  129 | 		private boolean matchesActiveProfiles(Predicate<String> activeProfiles) {
  130 | 			Assert.state(this.onProfile != null, "'this.onProfile' must not be null");
  131 | 			return org.springframework.core.env.Profiles.of(this.onProfile).matches(activeProfiles);
  132 | 		}
  133 | 
  134 | 	}
  135 | 
  136 | }
```
