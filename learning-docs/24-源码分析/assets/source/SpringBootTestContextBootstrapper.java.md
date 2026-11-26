<!-- learning-docs source archive（机械抓取原文，禁止手改代码内容） -->
<!-- origin: org.springframework.boot:spring-boot-test:4.2.0 | path: org/springframework/boot/test/context/SpringBootTestContextBootstrapper.java -->
<!-- 正文引用：[[源码:SpringBootTestContextBootstrapper.java#L起-L止]] ；代码块头部：(src: SpringBootTestContextBootstrapper.java#L起-L止) -->

# SpringBootTestContextBootstrapper.java

| 项 | 值 |
| --- | --- |
| 来源 | `org.springframework.boot:spring-boot-test:4.2.0` |
| 路径 | `org/springframework/boot/test/context/SpringBootTestContextBootstrapper.java` |
| 行数 | 351 |
| 在线地址 | https://repo1.maven.org/maven2/org/springframework/boot/spring-boot-test/4.2.0/spring-boot-test-4.2.0-sources.jar |
| 抓取时间 | 2026-11-25T14:10:56.819Z |

```java
    1 | /*
    2 |  * Copyright 2012-present the original author or authors.
    3 |  */
    4 | 
    5 | package org.springframework.boot.test.context;
    6 | 
    7 | import java.util.ArrayList;
    8 | import java.util.Arrays;
    9 | import java.util.HashSet;
   10 | import java.util.LinkedHashSet;
   11 | import java.util.List;
   12 | import java.util.Set;
   13 | 
   14 | import org.apache.commons.logging.Log;
   15 | import org.apache.commons.logging.LogFactory;
   16 | import org.jspecify.annotations.Nullable;
   17 | 
   18 | import org.springframework.boot.SpringBootConfiguration;
   19 | import org.springframework.boot.WebApplicationType;
   20 | import org.springframework.boot.context.properties.bind.Bindable;
   21 | import org.springframework.boot.context.properties.bind.Binder;
   22 | import org.springframework.boot.context.properties.source.ConfigurationPropertySource;
   23 | import org.springframework.boot.context.properties.source.MapConfigurationPropertySource;
   24 | import org.springframework.boot.test.context.SpringBootTest.WebEnvironment;
   25 | import org.springframework.core.annotation.AnnotationUtils;
   26 | import org.springframework.core.annotation.MergedAnnotation;
   27 | import org.springframework.core.annotation.MergedAnnotations;
   28 | import org.springframework.core.annotation.MergedAnnotations.SearchStrategy;
   29 | import org.springframework.core.env.Environment;
   30 | import org.springframework.test.context.ContextConfiguration;
   31 | import org.springframework.test.context.ContextConfigurationAttributes;
   32 | import org.springframework.test.context.ContextCustomizer;
   33 | import org.springframework.test.context.ContextHierarchy;
   34 | import org.springframework.test.context.ContextLoader;
   35 | import org.springframework.test.context.MergedContextConfiguration;
   36 | import org.springframework.test.context.TestContext;
   37 | import org.springframework.test.context.TestContextAnnotationUtils;
   38 | import org.springframework.test.context.TestContextBootstrapper;
   39 | import org.springframework.test.context.aot.AotTestAttributes;
   40 | import org.springframework.test.context.support.DefaultTestContextBootstrapper;
   41 | import org.springframework.test.context.support.TestPropertySourceUtils;
   42 | import org.springframework.test.context.web.WebAppConfiguration;
   43 | import org.springframework.test.context.web.WebMergedContextConfiguration;
   44 | import org.springframework.util.Assert;
   45 | import org.springframework.util.ClassUtils;
   46 | import org.springframework.util.ObjectUtils;
   47 | import org.springframework.util.StringUtils;
   48 | 
   49 | /**
   50 |  * {@link TestContextBootstrapper} for Spring Boot. Provides support for
   51 |  * {@link SpringBootTest @SpringBootTest} and may also be used directly or subclassed.
   52 |  * Provides the following features over and above {@link DefaultTestContextBootstrapper}:
   53 |  * <ul>
   54 |  * <li>Uses {@link SpringBootContextLoader} as the
   55 |  * {@link #getDefaultContextLoaderClass(Class) default context loader}.</li>
   56 |  * <li>Automatically searches for a
   57 |  * {@link SpringBootConfiguration @SpringBootConfiguration} when required.</li>
   58 |  * <li>Allows custom {@link Environment} {@link #getProperties(Class)} to be defined.</li>
   59 |  * <li>Provides support for different {@link WebEnvironment webEnvironment} modes.</li>
   60 |  * </ul>
   61 |  *
   62 |  * @author Phillip Webb
   63 |  * @author Andy Wilkinson
   64 |  * @author Brian Clozel
   65 |  * @author Madhura Bhave
   66 |  * @author Lorenzo Dee
   67 |  * @since 1.4.0
   68 |  * @see SpringBootTest
   69 |  * @see TestConfiguration
   70 |  */
   71 | public class SpringBootTestContextBootstrapper extends DefaultTestContextBootstrapper {
   72 | 
   73 | 	private static final String ACTIVATE_SERVLET_LISTENER = "org.springframework.test."
   74 | 			+ "context.web.ServletTestExecutionListener.activateListener";
   75 | 
   76 | 	private static final Log logger = LogFactory.getLog(SpringBootTestContextBootstrapper.class);
   77 | 
   78 | 	private final AotTestAttributes aotTestAttributes;
   79 | 
   80 | 	public SpringBootTestContextBootstrapper() {
   81 | 		this(AotTestAttributes.getInstance());
   82 | 	}
   83 | 
   84 | 	SpringBootTestContextBootstrapper(AotTestAttributes aotTestAttributes) {
   85 | 		this.aotTestAttributes = aotTestAttributes;
   86 | 	}
   87 | 
   88 | 	@Override
   89 | 	public TestContext buildTestContext() {
   90 | 		TestContext context = super.buildTestContext();
   91 | 		verifyConfiguration(context.getTestClass());
   92 | 		WebEnvironment webEnvironment = getWebEnvironment(context.getTestClass());
   93 | 		if (webEnvironment == WebEnvironment.MOCK && WebApplicationType.deduce() == WebApplicationType.SERVLET) {
   94 | 			context.setAttribute(ACTIVATE_SERVLET_LISTENER, true);
   95 | 		}
   96 | 		else if (webEnvironment != null && webEnvironment.isEmbedded()) {
   97 | 			context.setAttribute(ACTIVATE_SERVLET_LISTENER, false);
   98 | 		}
   99 | 		return context;
  100 | 	}
  101 | 
  102 | 	@Override
  103 | 	protected ContextLoader resolveContextLoader(Class<?> testClass,
  104 | 			List<ContextConfigurationAttributes> configAttributesList) {
  105 | 		Class<?>[] classes = getClasses(testClass);
  106 | 		if (!ObjectUtils.isEmpty(classes)) {
  107 | 			for (ContextConfigurationAttributes configAttributes : configAttributesList) {
  108 | 				addConfigAttributesClasses(configAttributes, classes);
  109 | 			}
  110 | 		}
  111 | 		return super.resolveContextLoader(testClass, configAttributesList);
  112 | 	}
  113 | 
  114 | 	private void addConfigAttributesClasses(ContextConfigurationAttributes configAttributes, Class<?>[] classes) {
  115 | 		Set<Class<?>> combined = new LinkedHashSet<>(Arrays.asList(classes));
  116 | 		if (configAttributes.getClasses() != null) {
  117 | 			combined.addAll(Arrays.asList(configAttributes.getClasses()));
  118 | 		}
  119 | 		configAttributes.setClasses(ClassUtils.toClassArray(combined));
  120 | 	}
  121 | 
  122 | 	@Override
  123 | 	protected Class<? extends ContextLoader> getDefaultContextLoaderClass(Class<?> testClass) {
  124 | 		return SpringBootContextLoader.class;
  125 | 	}
  126 | 
  127 | 	@Override
  128 | 	protected MergedContextConfiguration processMergedContextConfiguration(MergedContextConfiguration mergedConfig) {
  129 | 		Class<?>[] classes = getOrFindConfigurationClasses(mergedConfig);
  130 | 		List<String> propertySourceProperties = getAndProcessPropertySourceProperties(mergedConfig);
  131 | 		mergedConfig = createModifiedConfig(mergedConfig, classes, StringUtils.toStringArray(propertySourceProperties));
  132 | 		WebEnvironment webEnvironment = getWebEnvironment(mergedConfig.getTestClass());
  133 | 		if (webEnvironment != null && isWebEnvironmentSupported(mergedConfig)) {
  134 | 			WebApplicationType webApplicationType = getWebApplicationType(mergedConfig);
  135 | 			if (webApplicationType == WebApplicationType.SERVLET
  136 | 					&& (webEnvironment.isEmbedded() || webEnvironment == WebEnvironment.MOCK)) {
  137 | 				mergedConfig = new WebMergedContextConfiguration(mergedConfig, determineResourceBasePath(mergedConfig));
  138 | 			}
  139 | 			else if (webApplicationType == WebApplicationType.REACTIVE
  140 | 					&& (webEnvironment.isEmbedded() || webEnvironment == WebEnvironment.MOCK)) {
  141 | 				return new ReactiveWebMergedContextConfiguration(mergedConfig);
  142 | 			}
  143 | 		}
  144 | 		return mergedConfig;
  145 | 	}
  146 | 
  147 | 	private WebApplicationType getWebApplicationType(MergedContextConfiguration configuration) {
  148 | 		ConfigurationPropertySource source = new MapConfigurationPropertySource(
  149 | 				TestPropertySourceUtils.convertInlinedPropertiesToMap(configuration.getPropertySourceProperties()));
  150 | 		Binder binder = new Binder(source);
  151 | 		return binder.bind("spring.main.web-application-type", Bindable.of(WebApplicationType.class))
  152 | 			.orElseGet(WebApplicationType::deduce);
  153 | 	}
  154 | 
  155 | 	/**
  156 | 	 * Determines the resource base path for web applications using the value of
  157 | 	 * {@link WebAppConfiguration @WebAppConfiguration}, if any, on the test class of the
  158 | 	 * given {@code configuration}. Defaults to {@code src/main/webapp} in its absence.
  159 | 	 * @param configuration the configuration to examine
  160 | 	 * @return the resource base path
  161 | 	 * @since 2.1.6
  162 | 	 */
  163 | 	protected String determineResourceBasePath(MergedContextConfiguration configuration) {
  164 | 		return MergedAnnotations.from(configuration.getTestClass(), SearchStrategy.TYPE_HIERARCHY)
  165 | 			.get(WebAppConfiguration.class)
  166 | 			.getValue(MergedAnnotation.VALUE, String.class)
  167 | 			.orElse("src/main/webapp");
  168 | 	}
  169 | 
  170 | 	private boolean isWebEnvironmentSupported(MergedContextConfiguration mergedConfig) {
  171 | 		Class<?> testClass = mergedConfig.getTestClass();
  172 | 		ContextHierarchy hierarchy = AnnotationUtils.getAnnotation(testClass, ContextHierarchy.class);
  173 | 		if (hierarchy == null || hierarchy.value().length == 0) {
  174 | 			return true;
  175 | 		}
  176 | 		ContextConfiguration[] configurations = hierarchy.value();
  177 | 		return isFromConfiguration(mergedConfig, configurations[configurations.length - 1]);
  178 | 	}
  179 | 
  180 | 	private boolean isFromConfiguration(MergedContextConfiguration candidateConfig,
  181 | 			ContextConfiguration configuration) {
  182 | 		ContextConfigurationAttributes attributes = new ContextConfigurationAttributes(candidateConfig.getTestClass(),
  183 | 				configuration);
  184 | 		Set<Class<?>> configurationClasses = new HashSet<>(Arrays.asList(attributes.getClasses()));
  185 | 		for (Class<?> candidate : candidateConfig.getClasses()) {
  186 | 			if (configurationClasses.contains(candidate)) {
  187 | 				return true;
  188 | 			}
  189 | 		}
  190 | 		return false;
  191 | 	}
  192 | 
  193 | 	protected Class<?>[] getOrFindConfigurationClasses(MergedContextConfiguration mergedConfig) {
  194 | 		Class<?>[] classes = mergedConfig.getClasses();
  195 | 		if (containsNonTestComponent(classes) || mergedConfig.hasLocations()) {
  196 | 			return classes;
  197 | 		}
  198 | 		Class<?> found = findConfigurationClass(mergedConfig.getTestClass());
  199 | 		logger.info("Found @SpringBootConfiguration " + found.getName() + " for test " + mergedConfig.getTestClass());
  200 | 		return merge(found, classes);
  201 | 	}
  202 | 
  203 | 	private Class<?> findConfigurationClass(Class<?> testClass) {
  204 | 		String propertyName = "%s.SpringBootConfiguration.%s"
  205 | 			.formatted(SpringBootTestContextBootstrapper.class.getName(), testClass.getName());
  206 | 		String foundClassName = this.aotTestAttributes.getString(propertyName);
  207 | 		if (foundClassName != null) {
  208 | 			return ClassUtils.resolveClassName(foundClassName, testClass.getClassLoader());
  209 | 		}
  210 | 		Class<?> found = new AnnotatedClassFinder(SpringBootConfiguration.class).findFromClass(testClass);
  211 | 		Assert.state(found != null,
  212 | 				"Unable to find a @SpringBootConfiguration by searching packages upwards from the test. "
  213 | 						+ "You can use @ContextConfiguration, @SpringBootTest(classes=...) or other Spring Test "
  214 | 						+ "supported mechanisms to explicitly declare the configuration classes to load. Classes "
  215 | 						+ "annotated with @TestConfiguration are not considered.");
  216 | 		this.aotTestAttributes.setAttribute(propertyName, found.getName());
  217 | 		return found;
  218 | 	}
  219 | 
  220 | 	private boolean containsNonTestComponent(Class<?>[] classes) {
  221 | 		for (Class<?> candidate : classes) {
  222 | 			if (!MergedAnnotations.from(candidate, SearchStrategy.INHERITED_ANNOTATIONS)
  223 | 				.isPresent(TestConfiguration.class)) {
  224 | 				return true;
  225 | 			}
  226 | 		}
  227 | 		return false;
  228 | 	}
  229 | 
  230 | 	private Class<?>[] merge(Class<?> head, Class<?>[] existing) {
  231 | 		Class<?>[] result = new Class<?>[existing.length + 1];
  232 | 		result[0] = head;
  233 | 		System.arraycopy(existing, 0, result, 1, existing.length);
  234 | 		return result;
  235 | 	}
  236 | 
  237 | 	private List<String> getAndProcessPropertySourceProperties(MergedContextConfiguration mergedConfig) {
  238 | 		List<String> propertySourceProperties = new ArrayList<>(
  239 | 				Arrays.asList(mergedConfig.getPropertySourceProperties()));
  240 | 		String differentiator = getDifferentiatorPropertySourceProperty();
  241 | 		if (differentiator != null) {
  242 | 			propertySourceProperties.add(differentiator);
  243 | 		}
  244 | 		processPropertySourceProperties(mergedConfig, propertySourceProperties);
  245 | 		return propertySourceProperties;
  246 | 	}
  247 | 
  248 | 	/**
  249 | 	 * Return a "differentiator" property to ensure that there is something to
  250 | 	 * differentiate regular tests and bootstrapped tests. Without this property a cached
  251 | 	 * context could be returned that wasn't created by this bootstrapper. By default uses
  252 | 	 * the bootstrapper class as a property.
  253 | 	 * @return the differentiator or {@code null}
  254 | 	 */
  255 | 	protected @Nullable String getDifferentiatorPropertySourceProperty() {
  256 | 		return getClass().getName() + "=true";
  257 | 	}
  258 | 
  259 | 	/**
  260 | 	 * Post process the property source properties, adding or removing elements as
  261 | 	 * required.
  262 | 	 * @param mergedConfig the merged context configuration
  263 | 	 * @param propertySourceProperties the property source properties to process
  264 | 	 */
  265 | 	protected void processPropertySourceProperties(MergedContextConfiguration mergedConfig,
  266 | 			List<String> propertySourceProperties) {
  267 | 		Class<?> testClass = mergedConfig.getTestClass();
  268 | 		String[] properties = getProperties(testClass);
  269 | 		if (!ObjectUtils.isEmpty(properties)) {
  270 | 			// Added first so that inlined properties from @TestPropertySource take
  271 | 			// precedence
  272 | 			propertySourceProperties.addAll(0, Arrays.asList(properties));
  273 | 		}
  274 | 		WebEnvironment webEnvironment = getWebEnvironment(testClass);
  275 | 		if (webEnvironment == WebEnvironment.RANDOM_PORT) {
  276 | 			propertySourceProperties.add("server.port=0");
  277 | 		}
  278 | 		else if (webEnvironment == WebEnvironment.NONE) {
  279 | 			propertySourceProperties.add("spring.main.web-application-type=none");
  280 | 		}
  281 | 	}
  282 | 
  283 | 	/**
  284 | 	 * Return the {@link WebEnvironment} type for this test or null if undefined.
  285 | 	 * @param testClass the source test class
  286 | 	 * @return the {@link WebEnvironment} or {@code null}
  287 | 	 */
  288 | 	protected @Nullable WebEnvironment getWebEnvironment(Class<?> testClass) {
  289 | 		SpringBootTest annotation = getAnnotation(testClass);
  290 | 		return (annotation != null) ? annotation.webEnvironment() : null;
  291 | 	}
  292 | 
  293 | 	protected Class<?> @Nullable [] getClasses(Class<?> testClass) {
  294 | 		SpringBootTest annotation = getAnnotation(testClass);
  295 | 		return (annotation != null) ? annotation.classes() : null;
  296 | 	}
  297 | 
  298 | 	protected String @Nullable [] getProperties(Class<?> testClass) {
  299 | 		SpringBootTest annotation = getAnnotation(testClass);
  300 | 		return (annotation != null) ? annotation.properties() : null;
  301 | 	}
  302 | 
  303 | 	protected @Nullable SpringBootTest getAnnotation(Class<?> testClass) {
  304 | 		return TestContextAnnotationUtils.findMergedAnnotation(testClass, SpringBootTest.class);
  305 | 	}
  306 | 
  307 | 	protected void verifyConfiguration(Class<?> testClass) {
  308 | 		SpringBootTest springBootTest = getAnnotation(testClass);
  309 | 		if (springBootTest != null && isListeningOnPort(springBootTest.webEnvironment())
  310 | 				&& MergedAnnotations.from(testClass, SearchStrategy.INHERITED_ANNOTATIONS)
  311 | 					.isPresent(WebAppConfiguration.class)) {
  312 | 			throw new IllegalStateException("@WebAppConfiguration should only be used "
  313 | 					+ "with @SpringBootTest when @SpringBootTest is configured with a "
  314 | 					+ "mock web environment. Please remove @WebAppConfiguration or reconfigure @SpringBootTest.");
  315 | 		}
  316 | 	}
  317 | 
  318 | 	private boolean isListeningOnPort(WebEnvironment webEnvironment) {
  319 | 		return webEnvironment == WebEnvironment.DEFINED_PORT || webEnvironment == WebEnvironment.RANDOM_PORT;
  320 | 	}
  321 | 
  322 | 	/**
  323 | 	 * Create a new {@link MergedContextConfiguration} with different classes.
  324 | 	 * @param mergedConfig the source config
  325 | 	 * @param classes the replacement classes
  326 | 	 * @return a new {@link MergedContextConfiguration}
  327 | 	 */
  328 | 	protected final MergedContextConfiguration createModifiedConfig(MergedContextConfiguration mergedConfig,
  329 | 			Class<?>[] classes) {
  330 | 		return createModifiedConfig(mergedConfig, classes, mergedConfig.getPropertySourceProperties());
  331 | 	}
  332 | 
  333 | 	/**
  334 | 	 * Create a new {@link MergedContextConfiguration} with different classes and
  335 | 	 * properties.
  336 | 	 * @param mergedConfig the source config
  337 | 	 * @param classes the replacement classes
  338 | 	 * @param propertySourceProperties the replacement properties
  339 | 	 * @return a new {@link MergedContextConfiguration}
  340 | 	 */
  341 | 	protected final MergedContextConfiguration createModifiedConfig(MergedContextConfiguration mergedConfig,
  342 | 			Class<?>[] classes, String[] propertySourceProperties) {
  343 | 		Set<ContextCustomizer> contextCustomizers = new LinkedHashSet<>(mergedConfig.getContextCustomizers());
  344 | 		contextCustomizers.add(new SpringBootTestAnnotation(mergedConfig.getTestClass()));
  345 | 		return new MergedContextConfiguration(mergedConfig.getTestClass(), mergedConfig.getLocations(), classes,
  346 | 				mergedConfig.getContextInitializerClasses(), mergedConfig.getActiveProfiles(),
  347 | 				mergedConfig.getPropertySourceDescriptors(), propertySourceProperties, contextCustomizers,
  348 | 				mergedConfig.getContextLoader(), getCacheAwareContextLoaderDelegate(), mergedConfig.getParent());
  349 | 	}
  350 | 
  351 | }
```
