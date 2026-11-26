<!-- learning-docs source archive（机械抓取原文，禁止手改代码内容） -->
<!-- origin: org.springframework.boot:spring-boot:4.2.0 | path: org/springframework/boot/context/config/ConfigDataEnvironmentPostProcessor.java -->
<!-- 正文引用：[[源码:ConfigDataEnvironmentPostProcessor.java#L起-L止]] ；代码块头部：(src: ConfigDataEnvironmentPostProcessor.java#L起-L止) -->

# ConfigDataEnvironmentPostProcessor.java

| 项 | 值 |
| --- | --- |
| 来源 | `org.springframework.boot:spring-boot:4.2.0` |
| 路径 | `org/springframework/boot/context/config/ConfigDataEnvironmentPostProcessor.java` |
| 行数 | 161 |
| 在线地址 | https://repo1.maven.org/maven2/org/springframework/boot/spring-boot/4.2.0/spring-boot-4.2.0-sources.jar |
| 抓取时间 | 2026-11-25T14:04:54.495Z |

```java
    1 | /*
    2 |  * Copyright 2012-present the original author or authors.
    3 |  */
    4 | 
    5 | package org.springframework.boot.context.config;
    6 | 
    7 | import java.util.Arrays;
    8 | import java.util.Collection;
    9 | import java.util.Collections;
   10 | import java.util.function.Supplier;
   11 | 
   12 | import org.apache.commons.logging.Log;
   13 | import org.jspecify.annotations.Nullable;
   14 | 
   15 | import org.springframework.boot.EnvironmentPostProcessor;
   16 | import org.springframework.boot.SpringApplication;
   17 | import org.springframework.boot.bootstrap.ConfigurableBootstrapContext;
   18 | import org.springframework.boot.bootstrap.DefaultBootstrapContext;
   19 | import org.springframework.boot.logging.DeferredLogFactory;
   20 | import org.springframework.core.Ordered;
   21 | import org.springframework.core.env.ConfigurableEnvironment;
   22 | import org.springframework.core.env.Environment;
   23 | import org.springframework.core.io.DefaultResourceLoader;
   24 | import org.springframework.core.io.ResourceLoader;
   25 | 
   26 | /**
   27 |  * {@link EnvironmentPostProcessor} that loads and applies {@link ConfigData} to Spring's
   28 |  * {@link Environment}.
   29 |  *
   30 |  * @author Phillip Webb
   31 |  * @author Madhura Bhave
   32 |  * @author Nguyen Bao Sach
   33 |  * @since 2.4.0
   34 |  */
   35 | public class ConfigDataEnvironmentPostProcessor implements EnvironmentPostProcessor, Ordered {
   36 | 
   37 | 	/**
   38 | 	 * The default order for the processor.
   39 | 	 */
   40 | 	public static final int ORDER = Ordered.HIGHEST_PRECEDENCE + 10;
   41 | 
   42 | 	/**
   43 | 	 * Property used to determine what action to take when a
   44 | 	 * {@code ConfigDataLocationNotFoundException} is thrown.
   45 | 	 * @see ConfigDataNotFoundAction
   46 | 	 */
   47 | 	public static final String ON_LOCATION_NOT_FOUND_PROPERTY = ConfigDataEnvironment.ON_NOT_FOUND_PROPERTY;
   48 | 
   49 | 	private final DeferredLogFactory logFactory;
   50 | 
   51 | 	private final Log logger;
   52 | 
   53 | 	private final ConfigurableBootstrapContext bootstrapContext;
   54 | 
   55 | 	private final @Nullable ConfigDataEnvironmentUpdateListener environmentUpdateListener;
   56 | 
   57 | 	public ConfigDataEnvironmentPostProcessor(DeferredLogFactory logFactory,
   58 | 			ConfigurableBootstrapContext bootstrapContext) {
   59 | 		this(logFactory, bootstrapContext, null);
   60 | 	}
   61 | 
   62 | 	private ConfigDataEnvironmentPostProcessor(DeferredLogFactory logFactory,
   63 | 			ConfigurableBootstrapContext bootstrapContext,
   64 | 			@Nullable ConfigDataEnvironmentUpdateListener environmentUpdateListener) {
   65 | 		this.logFactory = logFactory;
   66 | 		this.logger = logFactory.getLog(getClass());
   67 | 		this.bootstrapContext = bootstrapContext;
   68 | 		this.environmentUpdateListener = environmentUpdateListener;
   69 | 	}
   70 | 
   71 | 	@Override
   72 | 	public int getOrder() {
   73 | 		return ORDER;
   74 | 	}
   75 | 
   76 | 	@Override
   77 | 	public void postProcessEnvironment(ConfigurableEnvironment environment, SpringApplication application) {
   78 | 		postProcessEnvironment(environment, application.getResourceLoader(), application.getAdditionalProfiles());
   79 | 	}
   80 | 
   81 | 	void postProcessEnvironment(ConfigurableEnvironment environment, @Nullable ResourceLoader resourceLoader,
   82 | 			Collection<String> additionalProfiles) {
   83 | 		this.logger.trace("Post-processing environment to add config data");
   84 | 		resourceLoader = (resourceLoader != null) ? resourceLoader : new DefaultResourceLoader();
   85 | 		getConfigDataEnvironment(environment, resourceLoader, additionalProfiles).processAndApply();
   86 | 	}
   87 | 
   88 | 	ConfigDataEnvironment getConfigDataEnvironment(ConfigurableEnvironment environment, ResourceLoader resourceLoader,
   89 | 			Collection<String> additionalProfiles) {
   90 | 		return new ConfigDataEnvironment(this.logFactory, this.bootstrapContext, environment, resourceLoader,
   91 | 				additionalProfiles, this.environmentUpdateListener);
   92 | 	}
   93 | 
   94 | 	/**
   95 | 	 * Apply {@link ConfigData} post-processing to an existing {@link Environment}. This
   96 | 	 * method can be useful when working with an {@link Environment} that has been created
   97 | 	 * directly and not necessarily as part of a {@link SpringApplication}.
   98 | 	 * @param environment the environment to apply {@link ConfigData} to
   99 | 	 */
  100 | 	public static void applyTo(ConfigurableEnvironment environment) {
  101 | 		applyTo(environment, null, null, Collections.emptyList());
  102 | 	}
  103 | 
  104 | 	/**
  105 | 	 * Apply {@link ConfigData} post-processing to an existing {@link Environment}. This
  106 | 	 * method can be useful when working with an {@link Environment} that has been created
  107 | 	 * directly and not necessarily as part of a {@link SpringApplication}.
  108 | 	 * @param environment the environment to apply {@link ConfigData} to
  109 | 	 * @param resourceLoader the resource loader to use
  110 | 	 * @param bootstrapContext the bootstrap context to use or {@code null} to use a
  111 | 	 * throw-away context
  112 | 	 * @param additionalProfiles any additional profiles that should be applied
  113 | 	 */
  114 | 	public static void applyTo(ConfigurableEnvironment environment, ResourceLoader resourceLoader,
  115 | 			@Nullable ConfigurableBootstrapContext bootstrapContext, String... additionalProfiles) {
  116 | 		applyTo(environment, resourceLoader, bootstrapContext, Arrays.asList(additionalProfiles));
  117 | 	}
  118 | 
  119 | 	/**
  120 | 	 * Apply {@link ConfigData} post-processing to an existing {@link Environment}. This
  121 | 	 * method can be useful when working with an {@link Environment} that has been created
  122 | 	 * directly and not necessarily as part of a {@link SpringApplication}.
  123 | 	 * @param environment the environment to apply {@link ConfigData} to
  124 | 	 * @param resourceLoader the resource loader to use
  125 | 	 * @param bootstrapContext the bootstrap context to use or {@code null} to use a
  126 | 	 * throw-away context
  127 | 	 * @param additionalProfiles any additional profiles that should be applied
  128 | 	 */
  129 | 	public static void applyTo(ConfigurableEnvironment environment, @Nullable ResourceLoader resourceLoader,
  130 | 			@Nullable ConfigurableBootstrapContext bootstrapContext, Collection<String> additionalProfiles) {
  131 | 		DeferredLogFactory logFactory = Supplier::get;
  132 | 		bootstrapContext = (bootstrapContext != null) ? bootstrapContext : new DefaultBootstrapContext();
  133 | 		ConfigDataEnvironmentPostProcessor postProcessor = new ConfigDataEnvironmentPostProcessor(logFactory,
  134 | 				bootstrapContext);
  135 | 		postProcessor.postProcessEnvironment(environment, resourceLoader, additionalProfiles);
  136 | 	}
  137 | 
  138 | 	/**
  139 | 	 * Apply {@link ConfigData} post-processing to an existing {@link Environment}. This
  140 | 	 * method can be useful when working with an {@link Environment} that has been created
  141 | 	 * directly and not necessarily as part of a {@link SpringApplication}.
  142 | 	 * @param environment the environment to apply {@link ConfigData} to
  143 | 	 * @param resourceLoader the resource loader to use
  144 | 	 * @param bootstrapContext the bootstrap context to use or {@code null} to use a
  145 | 	 * throw-away context
  146 | 	 * @param additionalProfiles any additional profiles that should be applied
  147 | 	 * @param environmentUpdateListener optional
  148 | 	 * {@link ConfigDataEnvironmentUpdateListener} that can be used to track
  149 | 	 * {@link Environment} updates.
  150 | 	 */
  151 | 	public static void applyTo(ConfigurableEnvironment environment, @Nullable ResourceLoader resourceLoader,
  152 | 			@Nullable ConfigurableBootstrapContext bootstrapContext, Collection<String> additionalProfiles,
  153 | 			ConfigDataEnvironmentUpdateListener environmentUpdateListener) {
  154 | 		DeferredLogFactory logFactory = Supplier::get;
  155 | 		bootstrapContext = (bootstrapContext != null) ? bootstrapContext : new DefaultBootstrapContext();
  156 | 		ConfigDataEnvironmentPostProcessor postProcessor = new ConfigDataEnvironmentPostProcessor(logFactory,
  157 | 				bootstrapContext, environmentUpdateListener);
  158 | 		postProcessor.postProcessEnvironment(environment, resourceLoader, additionalProfiles);
  159 | 	}
  160 | 
  161 | }
```
