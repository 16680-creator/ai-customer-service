<!-- learning-docs source archive（机械抓取原文，禁止手改代码内容） -->
<!-- origin: org.springframework.boot:spring-boot:4.2.0 | path: org/springframework/boot/ApplicationProperties.java -->
<!-- 正文引用：[[源码:ApplicationProperties.java#L起-L止]] ；代码块头部：(src: ApplicationProperties.java#L起-L止) -->

# ApplicationProperties.java

| 项 | 值 |
| --- | --- |
| 来源 | `org.springframework.boot:spring-boot:4.2.0` |
| 路径 | `org/springframework/boot/ApplicationProperties.java` |
| 行数 | 163 |
| 在线地址 | https://repo1.maven.org/maven2/org/springframework/boot/spring-boot/4.2.0/spring-boot-4.2.0-sources.jar |
| 抓取时间 | 2026-11-26T09:22:05.651Z |

```java
    1 | /*
    2 |  * Copyright 2012-present the original author or authors.
    3 |  */
    4 | 
    5 | package org.springframework.boot;
    6 | 
    7 | import java.util.LinkedHashSet;
    8 | import java.util.Set;
    9 | 
   10 | import org.jspecify.annotations.Nullable;
   11 | 
   12 | import org.springframework.aot.hint.RuntimeHints;
   13 | import org.springframework.aot.hint.RuntimeHintsRegistrar;
   14 | import org.springframework.boot.Banner.Mode;
   15 | import org.springframework.boot.context.properties.bind.BindableRuntimeHintsRegistrar;
   16 | import org.springframework.boot.logging.LoggingSystemProperty;
   17 | import org.springframework.core.env.Environment;
   18 | import org.springframework.util.Assert;
   19 | 
   20 | /**
   21 |  * Spring application properties.
   22 |  *
   23 |  * @author Moritz Halbritter
   24 |  */
   25 | class ApplicationProperties {
   26 | 
   27 | 	/**
   28 | 	 * Whether bean definition overriding, by registering a definition with the same name
   29 | 	 * as an existing definition, is allowed.
   30 | 	 */
   31 | 	private boolean allowBeanDefinitionOverriding;
   32 | 
   33 | 	/**
   34 | 	 * Whether to allow circular references between beans and automatically try to resolve
   35 | 	 * them.
   36 | 	 */
   37 | 	private boolean allowCircularReferences;
   38 | 
   39 | 	/**
   40 | 	 * Mode used to display the banner when the application runs.
   41 | 	 */
   42 | 	private Banner.@Nullable Mode bannerMode;
   43 | 
   44 | 	/**
   45 | 	 * Whether to keep the application alive even if there are no more non-daemon threads.
   46 | 	 */
   47 | 	private boolean keepAlive;
   48 | 
   49 | 	/**
   50 | 	 * Whether initialization should be performed lazily.
   51 | 	 */
   52 | 	private boolean lazyInitialization;
   53 | 
   54 | 	/**
   55 | 	 * Whether to log information about the application when it starts.
   56 | 	 */
   57 | 	private boolean logStartupInfo = true;
   58 | 
   59 | 	/**
   60 | 	 * Whether the application should have a shutdown hook registered.
   61 | 	 */
   62 | 	private boolean registerShutdownHook = true;
   63 | 
   64 | 	/**
   65 | 	 * Sources (class names, package names, or XML resource locations) to include in the
   66 | 	 * ApplicationContext.
   67 | 	 */
   68 | 	private Set<String> sources = new LinkedHashSet<>();
   69 | 
   70 | 	/**
   71 | 	 * Flag to explicitly request a specific type of web application. If not set,
   72 | 	 * auto-detected based on the classpath.
   73 | 	 */
   74 | 	private @Nullable WebApplicationType webApplicationType;
   75 | 
   76 | 	boolean isAllowBeanDefinitionOverriding() {
   77 | 		return this.allowBeanDefinitionOverriding;
   78 | 	}
   79 | 
   80 | 	void setAllowBeanDefinitionOverriding(boolean allowBeanDefinitionOverriding) {
   81 | 		this.allowBeanDefinitionOverriding = allowBeanDefinitionOverriding;
   82 | 	}
   83 | 
   84 | 	boolean isAllowCircularReferences() {
   85 | 		return this.allowCircularReferences;
   86 | 	}
   87 | 
   88 | 	void setAllowCircularReferences(boolean allowCircularReferences) {
   89 | 		this.allowCircularReferences = allowCircularReferences;
   90 | 	}
   91 | 
   92 | 	Mode getBannerMode(Environment environment) {
   93 | 		if (this.bannerMode != null) {
   94 | 			return this.bannerMode;
   95 | 		}
   96 | 		String applicationPropertyName = LoggingSystemProperty.CONSOLE_STRUCTURED_FORMAT.getApplicationPropertyName();
   97 | 		Assert.state(applicationPropertyName != null, "applicationPropertyName must not be null");
   98 | 		boolean structuredLoggingEnabled = environment.containsProperty(applicationPropertyName);
   99 | 		return (structuredLoggingEnabled) ? Mode.OFF : Banner.Mode.CONSOLE;
  100 | 	}
  101 | 
  102 | 	void setBannerMode(@Nullable Mode bannerMode) {
  103 | 		this.bannerMode = bannerMode;
  104 | 	}
  105 | 
  106 | 	boolean isKeepAlive() {
  107 | 		return this.keepAlive;
  108 | 	}
  109 | 
  110 | 	void setKeepAlive(boolean keepAlive) {
  111 | 		this.keepAlive = keepAlive;
  112 | 	}
  113 | 
  114 | 	boolean isLazyInitialization() {
  115 | 		return this.lazyInitialization;
  116 | 	}
  117 | 
  118 | 	void setLazyInitialization(boolean lazyInitialization) {
  119 | 		this.lazyInitialization = lazyInitialization;
  120 | 	}
  121 | 
  122 | 	boolean isLogStartupInfo() {
  123 | 		return this.logStartupInfo;
  124 | 	}
  125 | 
  126 | 	void setLogStartupInfo(boolean logStartupInfo) {
  127 | 		this.logStartupInfo = logStartupInfo;
  128 | 	}
  129 | 
  130 | 	boolean isRegisterShutdownHook() {
  131 | 		return this.registerShutdownHook;
  132 | 	}
  133 | 
  134 | 	void setRegisterShutdownHook(boolean registerShutdownHook) {
  135 | 		this.registerShutdownHook = registerShutdownHook;
  136 | 	}
  137 | 
  138 | 	Set<String> getSources() {
  139 | 		return this.sources;
  140 | 	}
  141 | 
  142 | 	void setSources(Set<String> sources) {
  143 | 		this.sources = new LinkedHashSet<>(sources);
  144 | 	}
  145 | 
  146 | 	@Nullable WebApplicationType getWebApplicationType() {
  147 | 		return this.webApplicationType;
  148 | 	}
  149 | 
  150 | 	void setWebApplicationType(@Nullable WebApplicationType webApplicationType) {
  151 | 		this.webApplicationType = webApplicationType;
  152 | 	}
  153 | 
  154 | 	static class ApplicationPropertiesRuntimeHints implements RuntimeHintsRegistrar {
  155 | 
  156 | 		@Override
  157 | 		public void registerHints(RuntimeHints hints, @Nullable ClassLoader classLoader) {
  158 | 			BindableRuntimeHintsRegistrar.forTypes(ApplicationProperties.class).registerHints(hints, classLoader);
  159 | 		}
  160 | 
  161 | 	}
  162 | 
  163 | }
```
