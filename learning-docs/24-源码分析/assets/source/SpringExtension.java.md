<!-- learning-docs source archive（机械抓取原文，禁止手改代码内容） -->
<!-- origin: org.springframework:spring-test:7.1.0 | path: org/springframework/test/context/junit/jupiter/SpringExtension.java -->
<!-- 正文引用：[[源码:SpringExtension.java#L起-L止]] ；代码块头部：(src: SpringExtension.java#L起-L止) -->

# SpringExtension.java

| 项 | 值 |
| --- | --- |
| 来源 | `org.springframework:spring-test:7.1.0` |
| 路径 | `org/springframework/test/context/junit/jupiter/SpringExtension.java` |
| 行数 | 654 |
| 在线地址 | https://repo1.maven.org/maven2/org/springframework/spring-test/7.1.0/spring-test-7.1.0-sources.jar |
| 抓取时间 | 2026-11-25T14:10:51.751Z |

```java
    1 | /*
    2 |  * Copyright 2002-present the original author or authors.
    3 |  *
    4 |  * Licensed under the Apache License, Version 2.0 (the "License");
    5 |  * you may not use this file except in compliance with the License.
    6 |  * You may obtain a copy of the License at
    7 |  *
    8 |  *      https://www.apache.org/licenses/LICENSE-2.0
    9 |  *
   10 |  * Unless required by applicable law or agreed to in writing, software
   11 |  * distributed under the License is distributed on an "AS IS" BASIS,
   12 |  * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
   13 |  * See the License for the specific language governing permissions and
   14 |  * limitations under the License.
   15 |  */
   16 | 
   17 | package org.springframework.test.context.junit.jupiter;
   18 | 
   19 | import java.lang.annotation.Annotation;
   20 | import java.lang.reflect.Constructor;
   21 | import java.lang.reflect.Executable;
   22 | import java.lang.reflect.Method;
   23 | import java.lang.reflect.Parameter;
   24 | import java.util.Arrays;
   25 | import java.util.List;
   26 | import java.util.Locale;
   27 | 
   28 | import org.jspecify.annotations.Nullable;
   29 | import org.junit.jupiter.api.AfterAll;
   30 | import org.junit.jupiter.api.AfterEach;
   31 | import org.junit.jupiter.api.BeforeAll;
   32 | import org.junit.jupiter.api.BeforeEach;
   33 | import org.junit.jupiter.api.TestInstance.Lifecycle;
   34 | import org.junit.jupiter.api.extension.AfterAllCallback;
   35 | import org.junit.jupiter.api.extension.AfterEachCallback;
   36 | import org.junit.jupiter.api.extension.AfterTestExecutionCallback;
   37 | import org.junit.jupiter.api.extension.BeforeAllCallback;
   38 | import org.junit.jupiter.api.extension.BeforeEachCallback;
   39 | import org.junit.jupiter.api.extension.BeforeTestExecutionCallback;
   40 | import org.junit.jupiter.api.extension.ExtensionContext;
   41 | import org.junit.jupiter.api.extension.ExtensionContext.Namespace;
   42 | import org.junit.jupiter.api.extension.ExtensionContext.Store;
   43 | import org.junit.jupiter.api.extension.ParameterContext;
   44 | import org.junit.jupiter.api.extension.ParameterResolver;
   45 | import org.junit.jupiter.api.extension.TestInstancePostProcessor;
   46 | import org.junit.jupiter.api.extension.TestInstantiationAwareExtension;
   47 | import org.junit.jupiter.api.parallel.ExecutionMode;
   48 | import org.junit.platform.commons.annotation.Testable;
   49 | 
   50 | import org.springframework.beans.factory.annotation.Autowired;
   51 | import org.springframework.beans.factory.annotation.ParameterResolutionDelegate;
   52 | import org.springframework.context.ApplicationContext;
   53 | import org.springframework.core.SpringProperties;
   54 | import org.springframework.core.annotation.MergedAnnotation;
   55 | import org.springframework.core.annotation.MergedAnnotations;
   56 | import org.springframework.core.annotation.MergedAnnotations.SearchStrategy;
   57 | import org.springframework.core.annotation.RepeatableContainers;
   58 | import org.springframework.test.context.MethodInvoker;
   59 | import org.springframework.test.context.TestContextAnnotationUtils;
   60 | import org.springframework.test.context.TestContextManager;
   61 | import org.springframework.test.context.bean.override.BeanOverride;
   62 | import org.springframework.test.context.bean.override.BeanOverrideHandler;
   63 | import org.springframework.test.context.bean.override.BeanOverrideUtils;
   64 | import org.springframework.test.context.event.ApplicationEvents;
   65 | import org.springframework.test.context.event.RecordApplicationEvents;
   66 | import org.springframework.test.context.support.PropertyProvider;
   67 | import org.springframework.test.context.support.TestConstructorUtils;
   68 | import org.springframework.util.Assert;
   69 | import org.springframework.util.ClassUtils;
   70 | import org.springframework.util.ReflectionUtils;
   71 | import org.springframework.util.ReflectionUtils.MethodFilter;
   72 | import org.springframework.util.StringUtils;
   73 | 
   74 | /**
   75 |  * {@code SpringExtension} integrates the <em>Spring TestContext Framework</em>
   76 |  * into the JUnit Jupiter testing framework.
   77 |  *
   78 |  * <p>To use this extension, annotate a JUnit Jupiter based test class with
   79 |  * {@code @ExtendWith(SpringExtension.class)}, {@code @SpringJUnitConfig},
   80 |  * {@code @SpringJUnitWebConfig}, or any other annotation that is meta-annotated
   81 |  * with {@code @ExtendWith(SpringExtension.class)} such as {@code @SpringBootTest},
   82 |  * etc.
   83 |  *
   84 |  * <p>As of Spring Framework 7.0, the {@code SpringExtension} is
   85 |  * {@linkplain #getTestInstantiationExtensionContextScope(ExtensionContext)
   86 |  * configured} to use a test-method scoped {@link ExtensionContext}, which
   87 |  * enables consistent dependency injection into fields and constructors from the
   88 |  * {@link ApplicationContext} for the current test method in a
   89 |  * {@link org.junit.jupiter.api.Nested @Nested} test class hierarchy. However,
   90 |  * if a third-party {@link org.junit.platform.launcher.TestExecutionListener
   91 |  * TestExecutionListener} is not compatible with the semantics associated with
   92 |  * a test-method scoped extension context &mdash; or if a developer wishes to
   93 |  * switch to test-class scoped semantics &mdash; the {@code SpringExtension} can
   94 |  * be configured by annotating a top-level test class with
   95 |  * {@link SpringExtensionConfig#useTestClassScopedExtensionContext()
   96 |  * &#64;SpringExtensionConfig(useTestClassScopedExtensionContext = true)} or by
   97 |  * setting the {@value #EXTENSION_CONTEXT_SCOPE_PROPERTY_NAME} property to
   98 |  * {@link ExtensionContextScope#TEST_CLASS test_class}. Note that an explicit
   99 |  * {@code @SpringExtensionConfig} declaration overrides the globally configured
  100 |  * property. Furthermore, the {@code SpringExtension} will always use a test-class
  101 |  * scoped {@code ExtensionContext} for a test class that is configured to use JUnit
  102 |  * Jupiter’s {@code @TestInstance(Lifecycle.PER_CLASS)} semantics.
  103 |  *
  104 |  * <p><strong>NOTE:</strong> This class requires JUnit Jupiter 6.0 or higher.
  105 |  *
  106 |  * @author Sam Brannen
  107 |  * @author Simon Baslé
  108 |  * @since 5.0
  109 |  * @see org.junit.jupiter.api.extension.ExtendWith @ExtendWith
  110 |  * @see org.springframework.test.context.junit.jupiter.SpringExtensionConfig @SpringExtensionConfig
  111 |  * @see org.springframework.test.context.junit.jupiter.SpringJUnitConfig @SpringJUnitConfig
  112 |  * @see org.springframework.test.context.junit.jupiter.web.SpringJUnitWebConfig @SpringJUnitWebConfig
  113 |  * @see org.springframework.test.context.junit.jupiter.EnabledIf @EnabledIf
  114 |  * @see org.springframework.test.context.junit.jupiter.DisabledIf @DisabledIf
  115 |  * @see org.springframework.test.context.TestContextManager TestContextManager
  116 |  */
  117 | public class SpringExtension implements BeforeAllCallback, AfterAllCallback, TestInstancePostProcessor,
  118 | 		BeforeEachCallback, AfterEachCallback, BeforeTestExecutionCallback, AfterTestExecutionCallback,
  119 | 		ParameterResolver {
  120 | 
  121 | 	/**
  122 | 	 * JVM system property used to configure the default {@link ExtensionContextScope}
  123 | 	 * for the {@code SpringExtension}: {@value}.
  124 | 	 * <p>Acceptable values include enum constants defined in {@code ExtensionContextScope},
  125 | 	 * ignoring case. For example, the default may be changed to
  126 | 	 * {@link ExtensionContextScope#TEST_CLASS} by supplying the following JVM
  127 | 	 * system property via the command line.
  128 | 	 * <pre style="code">-Dspring.test.extension.context.scope=test_class</pre>
  129 | 	 * <p>If the property is not set, {@link ExtensionContextScope#TEST_METHOD}
  130 | 	 * semantics will apply. Note, however, that {@code @SpringExtensionConfig}
  131 | 	 * takes precedence over this property.
  132 | 	 * <p>May alternatively be configured via the
  133 | 	 * {@link org.springframework.core.SpringProperties SpringProperties}
  134 | 	 * mechanism or as a
  135 | 	 * <a href="https://docs.junit.org/current/running-tests/configuration-parameters.html">JUnit
  136 | 	 * Platform configuration parameter</a>.
  137 | 	 * @since 7.0.7
  138 | 	 * @see ExtensionContextScope
  139 | 	 * @see SpringExtensionConfig
  140 | 	 */
  141 | 	public static final String EXTENSION_CONTEXT_SCOPE_PROPERTY_NAME = "spring.test.extension.context.scope";
  142 | 
  143 | 	/**
  144 | 	 * {@link Namespace} in which {@code TestContextManagers} are stored, keyed
  145 | 	 * by test class.
  146 | 	 */
  147 | 	private static final Namespace TEST_CONTEXT_MANAGER_NAMESPACE = Namespace.create(SpringExtension.class);
  148 | 
  149 | 	/**
  150 | 	 * {@link Namespace} in which the resolved default {@link ExtensionContextScope}
  151 | 	 * is stored.
  152 | 	 * @since 7.0.7
  153 | 	 */
  154 | 	private static final Namespace DEFAULT_EXTENSION_CONTEXT_SCOPE_NAMESPACE =
  155 | 			Namespace.create(SpringExtension.class.getName() + "#default.extension.context.scope");
  156 | 
  157 | 	/**
  158 | 	 * {@link Namespace} in which {@code @Autowired} validation error messages
  159 | 	 * are stored, keyed by test class.
  160 | 	 */
  161 | 	private static final Namespace AUTOWIRED_VALIDATION_NAMESPACE =
  162 | 			Namespace.create(SpringExtension.class.getName() + "#autowired.validation");
  163 | 
  164 | 	/**
  165 | 	 * <em>Marker</em> string constant to represent that no violations were detected.
  166 | 	 * <p>The value is an empty string which allows this class to perform quick
  167 | 	 * {@code isEmpty()} checks instead of performing unnecessary string comparisons.
  168 | 	 */
  169 | 	private static final String NO_VIOLATIONS_DETECTED = "";
  170 | 
  171 | 	/**
  172 | 	 * {@link Namespace} in which {@code @RecordApplicationEvents} validation error messages
  173 | 	 * are stored, keyed by test class.
  174 | 	 */
  175 | 	private static final Namespace RECORD_APPLICATION_EVENTS_VALIDATION_NAMESPACE =
  176 | 			Namespace.create(SpringExtension.class.getName() + "#recordApplicationEvents.validation");
  177 | 
  178 | 	// Note that @Test, @TestFactory, @TestTemplate, @RepeatedTest, and @ParameterizedTest
  179 | 	// are all meta-annotated with @Testable.
  180 | 	private static final List<Class<? extends Annotation>> JUPITER_ANNOTATION_TYPES =
  181 | 			List.of(BeforeAll.class, AfterAll.class, BeforeEach.class, AfterEach.class, Testable.class);
  182 | 
  183 | 	private static final MethodFilter autowiredTestOrLifecycleMethodFilter =
  184 | 			ReflectionUtils.USER_DECLARED_METHODS.and(SpringExtension::isAutowiredTestOrLifecycleMethod);
  185 | 
  186 | 
  187 | 	/**
  188 | 	 * Returns {@link TestInstantiationAwareExtension.ExtensionContextScope#TEST_METHOD
  189 | 	 * ExtensionContextScope.TEST_METHOD}.
  190 | 	 * <p>This can be overridden <em>locally</em> via the
  191 | 	 * {@link SpringExtensionConfig @SpringExtensionConfig} annotation or
  192 | 	 * <em>globally</em> via the {@value #EXTENSION_CONTEXT_SCOPE_PROPERTY_NAME}
  193 | 	 * property. See the {@linkplain SpringExtension class-level Javadoc} for further
  194 | 	 * details.
  195 | 	 * @since 7.0
  196 | 	 * @see SpringExtensionConfig#useTestClassScopedExtensionContext()
  197 | 	 * @see #EXTENSION_CONTEXT_SCOPE_PROPERTY_NAME
  198 | 	 * @see ExtensionContextScope
  199 | 	 */
  200 | 	@Override
  201 | 	public TestInstantiationAwareExtension.ExtensionContextScope getTestInstantiationExtensionContextScope(
  202 | 			ExtensionContext rootContext) {
  203 | 
  204 | 		return TestInstantiationAwareExtension.ExtensionContextScope.TEST_METHOD;
  205 | 	}
  206 | 
  207 | 	/**
  208 | 	 * Delegates to {@link TestContextManager#beforeTestClass}.
  209 | 	 */
  210 | 	@Override
  211 | 	public void beforeAll(ExtensionContext context) throws Exception {
  212 | 		TestContextManager testContextManager = getTestContextManager(context);
  213 | 		registerMethodInvoker(testContextManager, context);
  214 | 		testContextManager.beforeTestClass();
  215 | 	}
  216 | 
  217 | 	/**
  218 | 	 * Delegates to {@link TestContextManager#afterTestClass}.
  219 | 	 */
  220 | 	@Override
  221 | 	public void afterAll(ExtensionContext context) throws Exception {
  222 | 		try {
  223 | 			TestContextManager testContextManager = getTestContextManager(context);
  224 | 			registerMethodInvoker(testContextManager, context);
  225 | 			testContextManager.afterTestClass();
  226 | 		}
  227 | 		finally {
  228 | 			getStore(context).remove(context.getRequiredTestClass());
  229 | 		}
  230 | 	}
  231 | 
  232 | 	/**
  233 | 	 * Delegates to {@link TestContextManager#prepareTestInstance}.
  234 | 	 * <p>This method also validates that test methods and test lifecycle methods
  235 | 	 * are not annotated with {@link Autowired @Autowired}.
  236 | 	 */
  237 | 	@Override
  238 | 	public void postProcessTestInstance(Object testInstance, ExtensionContext context) throws Exception {
  239 | 		context = findProperlyScopedExtensionContext(testInstance.getClass(), context);
  240 | 
  241 | 		validateAutowiredConfig(context);
  242 | 		validateRecordApplicationEventsConfig(context);
  243 | 		TestContextManager testContextManager = getTestContextManager(context);
  244 | 		registerMethodInvoker(testContextManager, context);
  245 | 		testContextManager.prepareTestInstance(testInstance);
  246 | 	}
  247 | 
  248 | 	/**
  249 | 	 * Validate that test methods and test lifecycle methods in the supplied
  250 | 	 * test class are not annotated with {@link Autowired @Autowired}.
  251 | 	 * @since 5.3.2
  252 | 	 */
  253 | 	private void validateAutowiredConfig(ExtensionContext context) {
  254 | 		// We save the result in the ExtensionContext.Store so that we don't
  255 | 		// re-validate all methods for the same test class multiple times.
  256 | 		Store store = context.getStore(AUTOWIRED_VALIDATION_NAMESPACE);
  257 | 
  258 | 		String errorMessage = store.computeIfAbsent(context.getRequiredTestClass(), testClass -> {
  259 | 				Method[] methodsWithErrors =
  260 | 						ReflectionUtils.getUniqueDeclaredMethods(testClass, autowiredTestOrLifecycleMethodFilter);
  261 | 				return (methodsWithErrors.length == 0 ? NO_VIOLATIONS_DETECTED :
  262 | 						String.format(
  263 | 								"Test methods and test lifecycle methods must not be annotated with @Autowired. " +
  264 | 								"You should instead annotate individual method parameters with @Autowired, " +
  265 | 								"@Qualifier, or @Value. Offending methods in test class %s: %s",
  266 | 								testClass.getName(), Arrays.toString(methodsWithErrors)));
  267 | 			}, String.class);
  268 | 
  269 | 		if (!errorMessage.isEmpty()) {
  270 | 			throw new IllegalStateException(errorMessage);
  271 | 		}
  272 | 	}
  273 | 
  274 | 	/**
  275 | 	 * Validate that the test class or its enclosing class doesn't attempt to record
  276 | 	 * application events in a parallel mode that makes it non-deterministic
  277 | 	 * ({@code @TestInstance(PER_CLASS)} and {@code @Execution(CONCURRENT)}
  278 | 	 * combination).
  279 | 	 * @since 6.1
  280 | 	 */
  281 | 	private void validateRecordApplicationEventsConfig(ExtensionContext context) {
  282 | 		// We save the result in the ExtensionContext.Store so that we don't
  283 | 		// re-validate the configuration for the same test class multiple times.
  284 | 		Store store = context.getStore(RECORD_APPLICATION_EVENTS_VALIDATION_NAMESPACE);
  285 | 
  286 | 		String errorMessage = store.computeIfAbsent(context.getRequiredTestClass(), testClass -> {
  287 | 			boolean recording = TestContextAnnotationUtils.hasAnnotation(testClass, RecordApplicationEvents.class);
  288 | 			if (!recording) {
  289 | 				return NO_VIOLATIONS_DETECTED;
  290 | 			}
  291 | 
  292 | 			if (context.getTestInstanceLifecycle().orElse(Lifecycle.PER_METHOD) == Lifecycle.PER_METHOD) {
  293 | 				return NO_VIOLATIONS_DETECTED;
  294 | 			}
  295 | 
  296 | 			if (context.getExecutionMode() == ExecutionMode.SAME_THREAD) {
  297 | 				return NO_VIOLATIONS_DETECTED;
  298 | 			}
  299 | 
  300 | 			return """
  301 | 					Test classes or @Nested test classes that @RecordApplicationEvents must not be run \
  302 | 					in parallel with the @TestInstance(PER_CLASS) lifecycle mode. Configure either \
  303 | 					@Execution(SAME_THREAD) or @TestInstance(PER_METHOD) semantics, or disable parallel \
  304 | 					execution altogether. Note that when recording events in parallel, one might see events \
  305 | 					published by other tests since the application context may be shared.""";
  306 | 		}, String.class);
  307 | 
  308 | 		if (!errorMessage.isEmpty()) {
  309 | 			throw new IllegalStateException(errorMessage);
  310 | 		}
  311 | 	}
  312 | 
  313 | 	/**
  314 | 	 * Delegates to {@link TestContextManager#beforeTestMethod}.
  315 | 	 */
  316 | 	@Override
  317 | 	public void beforeEach(ExtensionContext context) throws Exception {
  318 | 		Object testInstance = context.getRequiredTestInstance();
  319 | 		Method testMethod = context.getRequiredTestMethod();
  320 | 		TestContextManager testContextManager = getTestContextManager(context);
  321 | 		registerMethodInvoker(testContextManager, context);
  322 | 		testContextManager.beforeTestMethod(testInstance, testMethod);
  323 | 	}
  324 | 
  325 | 	/**
  326 | 	 * Delegates to {@link TestContextManager#beforeTestExecution}.
  327 | 	 */
  328 | 	@Override
  329 | 	public void beforeTestExecution(ExtensionContext context) throws Exception {
  330 | 		Object testInstance = context.getRequiredTestInstance();
  331 | 		Method testMethod = context.getRequiredTestMethod();
  332 | 		TestContextManager testContextManager = getTestContextManager(context);
  333 | 		registerMethodInvoker(testContextManager, context);
  334 | 		testContextManager.beforeTestExecution(testInstance, testMethod);
  335 | 	}
  336 | 
  337 | 	/**
  338 | 	 * Delegates to {@link TestContextManager#afterTestExecution}.
  339 | 	 */
  340 | 	@Override
  341 | 	public void afterTestExecution(ExtensionContext context) throws Exception {
  342 | 		Object testInstance = context.getRequiredTestInstance();
  343 | 		Method testMethod = context.getRequiredTestMethod();
  344 | 		Throwable testException = context.getExecutionException().orElse(null);
  345 | 		TestContextManager testContextManager = getTestContextManager(context);
  346 | 		registerMethodInvoker(testContextManager, context);
  347 | 		testContextManager.afterTestExecution(testInstance, testMethod, testException);
  348 | 	}
  349 | 
  350 | 	/**
  351 | 	 * Delegates to {@link TestContextManager#afterTestMethod}.
  352 | 	 */
  353 | 	@Override
  354 | 	public void afterEach(ExtensionContext context) throws Exception {
  355 | 		Object testInstance = context.getRequiredTestInstance();
  356 | 		Method testMethod = context.getRequiredTestMethod();
  357 | 		Throwable testException = context.getExecutionException().orElse(null);
  358 | 		TestContextManager testContextManager = getTestContextManager(context);
  359 | 		registerMethodInvoker(testContextManager, context);
  360 | 		testContextManager.afterTestMethod(testInstance, testMethod, testException);
  361 | 	}
  362 | 
  363 | 	/**
  364 | 	 * Determine if the value for the {@link Parameter} in the supplied {@link ParameterContext}
  365 | 	 * should be autowired from the test's {@link ApplicationContext}.
  366 | 	 * <p>A parameter is considered to be autowirable if one of the following
  367 | 	 * conditions is {@code true}.
  368 | 	 * <ol>
  369 | 	 * <li>The {@linkplain ParameterContext#getDeclaringExecutable() declaring
  370 | 	 * executable} is a {@link Constructor} and
  371 | 	 * {@link TestConstructorUtils#isAutowirableConstructor(Executable, PropertyProvider)}
  372 | 	 * returns {@code true}. Note that {@code isAutowirableConstructor()} will be
  373 | 	 * invoked with a fallback {@link PropertyProvider} that delegates its lookup
  374 | 	 * to {@link ExtensionContext#getConfigurationParameter(String)}.</li>
  375 | 	 * <li>The parameter is of type {@link ApplicationContext} or a sub-type thereof.</li>
  376 | 	 * <li>The parameter is annotated or meta-annotated with a
  377 | 	 * {@link BeanOverride @BeanOverride} composed annotation &mdash; for example,
  378 | 	 * {@code @MockitoBean} or {@code @MockitoSpyBean}.</li>
  379 | 	 * <li>The parameter is of type {@link ApplicationEvents} or a sub-type thereof.</li>
  380 | 	 * <li>{@link ParameterResolutionDelegate#isAutowirable} returns {@code true}.</li>
  381 | 	 * </ol>
  382 | 	 * <p>This method does not {@linkplain #getApplicationContext(ExtensionContext)
  383 | 	 * load} the {@code ApplicationContext} or verify that the application context
  384 | 	 * actually contains a matching candidate bean, since doing so would potentially
  385 | 	 * load an application context too early or unnecessarily.
  386 | 	 * <p><strong>WARNING</strong>: If a test class {@code Constructor} is annotated
  387 | 	 * with {@code @Autowired} or automatically autowirable (see
  388 | 	 * {@link org.springframework.test.context.TestConstructor @TestConstructor}),
  389 | 	 * Spring will assume the responsibility for resolving all parameters in the
  390 | 	 * constructor. Consequently, no other registered {@link ParameterResolver}
  391 | 	 * will be able to resolve parameters.
  392 | 	 * @see #resolveParameter
  393 | 	 * @see TestConstructorUtils#isAutowirableConstructor(Executable, PropertyProvider)
  394 | 	 * @see ParameterResolutionDelegate#isAutowirable
  395 | 	 */
  396 | 	@Override
  397 | 	public boolean supportsParameter(ParameterContext parameterContext, ExtensionContext extensionContext) {
  398 | 		Parameter parameter = parameterContext.getParameter();
  399 | 		Class<?> parameterType = parameter.getType();
  400 | 		Executable executable = parameter.getDeclaringExecutable();
  401 | 		PropertyProvider junitPropertyProvider = propertyName ->
  402 | 				extensionContext.getConfigurationParameter(propertyName).orElse(null);
  403 | 		return (TestConstructorUtils.isAutowirableConstructor(executable, junitPropertyProvider) ||
  404 | 				ApplicationContext.class.isAssignableFrom(parameterType) ||
  405 | 				isBeanOverride(parameter) ||
  406 | 				supportsApplicationEvents(parameterType, executable) ||
  407 | 				ParameterResolutionDelegate.isAutowirable(parameter, parameterContext.getIndex()));
  408 | 	}
  409 | 
  410 | 	private static boolean supportsApplicationEvents(Class<?> parameterType, Executable executable) {
  411 | 		if (ApplicationEvents.class.isAssignableFrom(parameterType)) {
  412 | 			Assert.isTrue(executable instanceof Method,
  413 | 					"ApplicationEvents can only be injected into test and lifecycle methods");
  414 | 			return true;
  415 | 		}
  416 | 		return false;
  417 | 	}
  418 | 
  419 | 	/**
  420 | 	 * Resolve a value for the {@link Parameter} in the supplied {@link ParameterContext} by
  421 | 	 * retrieving the corresponding dependency from the test's {@link ApplicationContext}.
  422 | 	 * <p>Delegates to {@link ParameterResolutionDelegate}.
  423 | 	 * @see #supportsParameter
  424 | 	 * @see ParameterResolutionDelegate#resolveDependency(Parameter, int, String, Class, org.springframework.beans.factory.config.AutowireCapableBeanFactory)
  425 | 	 */
  426 | 	@Override
  427 | 	public @Nullable Object resolveParameter(ParameterContext parameterContext, ExtensionContext extensionContext) {
  428 | 		Parameter parameter = parameterContext.getParameter();
  429 | 		int index = parameterContext.getIndex();
  430 | 		Executable executable = parameterContext.getDeclaringExecutable();
  431 | 		Class<?> testClass = extensionContext.getRequiredTestClass();
  432 | 		if (executable instanceof Constructor<?> constructor) {
  433 | 			testClass = constructor.getDeclaringClass();
  434 | 			extensionContext = findProperlyScopedExtensionContext(testClass, extensionContext);
  435 | 		}
  436 | 
  437 | 		ApplicationContext applicationContext = getApplicationContext(extensionContext);
  438 | 
  439 | 		// If the parameter is a @BeanOverride with an explicit name, we simply look
  440 | 		// up the bean by name instead of performing full dependency resolution.
  441 | 		if (isBeanOverride(parameter)) {
  442 | 			BeanOverrideHandler handler = BeanOverrideUtils.resolveHandlerForParameter(parameter, testClass);
  443 | 			if (handler != null && handler.getBeanName() != null) {
  444 | 				return applicationContext.getBean(handler.getBeanName());
  445 | 			}
  446 | 		}
  447 | 		return ParameterResolutionDelegate.resolveDependency(parameter, index, testClass,
  448 | 				applicationContext.getAutowireCapableBeanFactory());
  449 | 	}
  450 | 
  451 | 
  452 | 	/**
  453 | 	 * Get the {@link ApplicationContext} associated with the supplied {@link ExtensionContext}.
  454 | 	 * <p><strong>NOTE</strong>: As of Spring Framework 7.0, the supplied
  455 | 	 * {@code ExtensionContext} may not be properly <em>scoped</em>. See the
  456 | 	 * {@linkplain SpringExtension class-level Javadoc} for further details.
  457 | 	 * <p><strong>WARNING</strong>: Invoking this method ensures that the
  458 | 	 * corresponding {@code ApplicationContext} is
  459 | 	 * {@linkplain org.springframework.test.context.TestContext#getApplicationContext()
  460 | 	 * loaded}. Consequently, this method should not be used if eager loading of
  461 | 	 * the application context is undesired. For example,
  462 | 	 * {@link #supportsParameter(ParameterContext, ExtensionContext)} intentionally
  463 | 	 * does not invoke this method, since doing so would potentially load an
  464 | 	 * application context too early or unnecessarily.
  465 | 	 * @param context the current {@code ExtensionContext} (never {@code null})
  466 | 	 * @return the application context
  467 | 	 * @throws IllegalStateException if an error occurs while retrieving the application context
  468 | 	 * @see org.springframework.test.context.TestContext#getApplicationContext()
  469 | 	 */
  470 | 	public static ApplicationContext getApplicationContext(ExtensionContext context) {
  471 | 		return getTestContextManager(context).getTestContext().getApplicationContext();
  472 | 	}
  473 | 
  474 | 	/**
  475 | 	 * Get the {@link TestContextManager} associated with the supplied {@link ExtensionContext}.
  476 | 	 * @return the {@code TestContextManager} (never {@code null})
  477 | 	 */
  478 | 	static TestContextManager getTestContextManager(ExtensionContext context) {
  479 | 		Assert.notNull(context, "ExtensionContext must not be null");
  480 | 		Class<?> testClass = context.getRequiredTestClass();
  481 | 		Store store = getStore(context);
  482 | 		return store.computeIfAbsent(testClass, TestContextManager::new, TestContextManager.class);
  483 | 	}
  484 | 
  485 | 	private static Store getStore(ExtensionContext context) {
  486 | 		return context.getRoot().getStore(TEST_CONTEXT_MANAGER_NAMESPACE);
  487 | 	}
  488 | 
  489 | 	/**
  490 | 	 * Register a {@link MethodInvoker} adaptor for Jupiter's
  491 | 	 * {@link org.junit.jupiter.api.extension.ExecutableInvoker ExecutableInvoker}
  492 | 	 * in the {@link org.springframework.test.context.TestContext TestContext} for
  493 | 	 * the supplied {@link TestContextManager}.
  494 | 	 * @since 6.1
  495 | 	 */
  496 | 	private static void registerMethodInvoker(TestContextManager testContextManager, ExtensionContext context) {
  497 | 		testContextManager.getTestContext().setMethodInvoker(context.getExecutableInvoker()::invoke);
  498 | 	}
  499 | 
  500 | 	private static boolean isAutowiredTestOrLifecycleMethod(Method method) {
  501 | 		MergedAnnotations mergedAnnotations =
  502 | 				MergedAnnotations.from(method, SearchStrategy.DIRECT, RepeatableContainers.none());
  503 | 		if (!mergedAnnotations.isPresent(Autowired.class)) {
  504 | 			return false;
  505 | 		}
  506 | 		for (Class<? extends Annotation> annotationType : JUPITER_ANNOTATION_TYPES) {
  507 | 			if (mergedAnnotations.isPresent(annotationType)) {
  508 | 				return true;
  509 | 			}
  510 | 		}
  511 | 		return false;
  512 | 	}
  513 | 
  514 | 	private static boolean isBeanOverride(Parameter parameter) {
  515 | 		return (parameter.getDeclaringExecutable() instanceof Constructor &&
  516 | 				MergedAnnotations.from(parameter).isPresent(BeanOverride.class));
  517 | 	}
  518 | 
  519 | 	/**
  520 | 	 * Find the properly scoped {@link ExtensionContext} for the supplied test class.
  521 | 	 * <p>If the supplied {@code ExtensionContext} is already properly scoped, it
  522 | 	 * will be returned. Otherwise, if test-class scoped semantics apply (see
  523 | 	 * {@linkplain SpringExtension class-level Javadoc}), this method searches the
  524 | 	 * {@code ExtensionContext} hierarchy for an {@code ExtensionContext} whose test
  525 | 	 * class is the same as the supplied test class.
  526 | 	 * @since 7.0
  527 | 	 * @see SpringExtensionConfig#useTestClassScopedExtensionContext()
  528 | 	 * @see #EXTENSION_CONTEXT_SCOPE_PROPERTY_NAME
  529 | 	 * @see ExtensionContextScope
  530 | 	 */
  531 | 	private static ExtensionContext findProperlyScopedExtensionContext(Class<?> testClass, ExtensionContext context) {
  532 | 		if (shouldUseTestClassScopedExtensionContext(testClass, context)) {
  533 | 			while (context.getRequiredTestClass() != testClass) {
  534 | 				context = context.getParent().get();
  535 | 			}
  536 | 		}
  537 | 		return context;
  538 | 	}
  539 | 
  540 | 	/**
  541 | 	 * Determine whether test-class scoped {@code ExtensionContext} semantics apply
  542 | 	 * for the supplied test class.
  543 | 	 * @since 7.0
  544 | 	 */
  545 | 	private static boolean shouldUseTestClassScopedExtensionContext(Class<?> testClass, ExtensionContext context) {
  546 | 		MergedAnnotation<SpringExtensionConfig> mergedAnnotation =
  547 | 				MergedAnnotations.search(SearchStrategy.TYPE_HIERARCHY)
  548 | 						.withEnclosingClasses(ClassUtils::isInnerClass)
  549 | 						.from(testClass)
  550 | 						.get(SpringExtensionConfig.class);
  551 | 
  552 | 		if (mergedAnnotation.isPresent()) {
  553 | 			if (mergedAnnotation.getSource() instanceof Class<?> source && ClassUtils.isInnerClass(source)) {
  554 | 				throw new IllegalStateException("""
  555 | 						Test class [%s] must not be annotated with @SpringExtensionConfig. \
  556 | 						@SpringExtensionConfig is only supported on top-level classes.\
  557 | 						""".formatted(source.getName()));
  558 | 			}
  559 | 			return mergedAnnotation.getBoolean("useTestClassScopedExtensionContext");
  560 | 		}
  561 | 
  562 | 		return (resolveDefaultExtensionContextScope(context) == ExtensionContextScope.TEST_CLASS);
  563 | 	}
  564 | 
  565 | 	/**
  566 | 	 * Resolve the default {@link ExtensionContextScope} from the
  567 | 	 * {@value #EXTENSION_CONTEXT_SCOPE_PROPERTY_NAME} property, first via
  568 | 	 * {@link SpringProperties} and then via
  569 | 	 * {@link ExtensionContext#getConfigurationParameter(String)} as a fallback
  570 | 	 * strategy if the Spring property is not set.
  571 | 	 * @param context the current {@code ExtensionContext}
  572 | 	 * @return the resolved scope, or {@link ExtensionContextScope#TEST_METHOD}
  573 | 	 * if the property is not set
  574 | 	 * @since 7.0.7
  575 | 	 */
  576 | 	private static ExtensionContextScope resolveDefaultExtensionContextScope(ExtensionContext context) {
  577 | 		return context.getRoot().getStore(DEFAULT_EXTENSION_CONTEXT_SCOPE_NAMESPACE)
  578 | 				.computeIfAbsent(ExtensionContextScope.class, key -> {
  579 | 					String springValue = SpringProperties.getProperty(EXTENSION_CONTEXT_SCOPE_PROPERTY_NAME);
  580 | 					ExtensionContextScope scope = ExtensionContextScope.from(springValue);
  581 | 					if (scope != null) {
  582 | 						return scope;
  583 | 					}
  584 | 					rejectUnsupportedScope(springValue);
  585 | 
  586 | 					String junitValue = context.getConfigurationParameter(EXTENSION_CONTEXT_SCOPE_PROPERTY_NAME)
  587 | 							.orElse(null);
  588 | 					scope = ExtensionContextScope.from(junitValue);
  589 | 					if (scope != null) {
  590 | 						return scope;
  591 | 					}
  592 | 					rejectUnsupportedScope(junitValue);
  593 | 
  594 | 					// Default to test-method scope.
  595 | 					return ExtensionContextScope.TEST_METHOD;
  596 | 				}, ExtensionContextScope.class);
  597 | 	}
  598 | 
  599 | 	private static void rejectUnsupportedScope(@Nullable String scope) {
  600 | 		if (StringUtils.hasText(scope)) {
  601 | 			throw new IllegalArgumentException("Unsupported value '%s' for property '%s'"
  602 | 					.formatted(scope.strip(), EXTENSION_CONTEXT_SCOPE_PROPERTY_NAME));
  603 | 		}
  604 | 	}
  605 | 
  606 | 
  607 | 	/**
  608 | 	 * Enumeration of <em>extension context scopes</em> for configuring how the
  609 | 	 * {@link SpringExtension} resolves an {@link ExtensionContext} within
  610 | 	 * {@code @Nested} test class hierarchies.
  611 | 	 *
  612 | 	 * @since 7.0.7
  613 | 	 * @see SpringExtension#EXTENSION_CONTEXT_SCOPE_PROPERTY_NAME
  614 | 	 * @see SpringExtensionConfig
  615 | 	 * @see org.junit.jupiter.api.extension.TestInstantiationAwareExtension.ExtensionContextScope
  616 | 	 */
  617 | 	public enum ExtensionContextScope {
  618 | 
  619 | 		/**
  620 | 		 * Use a test-method scoped {@link ExtensionContext} within {@code @Nested}
  621 | 		 * test class hierarchies.
  622 | 		 * @see org.junit.jupiter.api.extension.TestInstantiationAwareExtension.ExtensionContextScope#TEST_METHOD
  623 | 		 */
  624 | 		TEST_METHOD,
  625 | 
  626 | 		/**
  627 | 		 * Use a test-class scoped {@link ExtensionContext} within {@code @Nested}
  628 | 		 * test class hierarchies.
  629 | 		 * @see org.junit.jupiter.api.extension.TestInstantiationAwareExtension.ExtensionContextScope#DEFAULT
  630 | 		 */
  631 | 		TEST_CLASS;
  632 | 
  633 | 
  634 | 		/**
  635 | 		 * Get the {@link ExtensionContextScope} enum constant with the supplied name,
  636 | 		 * {@linkplain String#strip() stripped} and ignoring case.
  637 | 		 * @param name the name of the enum constant to retrieve
  638 | 		 * @return the corresponding enum constant, or {@code null} if not found
  639 | 		 * @see ExtensionContextScope#valueOf(String)
  640 | 		 */
  641 | 		static @Nullable ExtensionContextScope from(@Nullable String name) {
  642 | 			if (!StringUtils.hasText(name)) {
  643 | 				return null;
  644 | 			}
  645 | 			try {
  646 | 				return ExtensionContextScope.valueOf(name.strip().toUpperCase(Locale.ROOT));
  647 | 			}
  648 | 			catch (IllegalArgumentException ex) {
  649 | 				return null;
  650 | 			}
  651 | 		}
  652 | 	}
  653 | 
  654 | }
```
