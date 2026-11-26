<!-- learning-docs source archive（机械抓取原文，禁止手改代码内容） -->
<!-- origin: org.springframework:spring-test:7.1.0 | path: org/springframework/test/context/TestContextManager.java -->
<!-- 正文引用：[[源码:TestContextManager.java#L起-L止]] ；代码块头部：(src: TestContextManager.java#L起-L止) -->

# TestContextManager.java

| 项 | 值 |
| --- | --- |
| 来源 | `org.springframework:spring-test:7.1.0` |
| 路径 | `org/springframework/test/context/TestContextManager.java` |
| 行数 | 704 |
| 在线地址 | https://repo1.maven.org/maven2/org/springframework/spring-test/7.1.0/spring-test-7.1.0-sources.jar |
| 抓取时间 | 2026-11-25T14:10:51.559Z |

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
   17 | package org.springframework.test.context;
   18 | 
   19 | import java.lang.reflect.Constructor;
   20 | import java.lang.reflect.Method;
   21 | import java.util.ArrayList;
   22 | import java.util.Collections;
   23 | import java.util.List;
   24 | import java.util.Set;
   25 | 
   26 | import org.apache.commons.logging.Log;
   27 | import org.apache.commons.logging.LogFactory;
   28 | import org.jspecify.annotations.Nullable;
   29 | 
   30 | import org.springframework.util.ClassUtils;
   31 | import org.springframework.util.CollectionUtils;
   32 | import org.springframework.util.ReflectionUtils;
   33 | 
   34 | /**
   35 |  * {@code TestContextManager} is the main entry point into the <em>Spring
   36 |  * TestContext Framework</em>.
   37 |  *
   38 |  * <p>Specifically, a {@code TestContextManager} is responsible for managing a
   39 |  * single {@link TestContext} and signaling events to each registered
   40 |  * {@link TestExecutionListener} at the following test execution points.
   41 |  *
   42 |  * <ul>
   43 |  * <li>{@link #beforeTestClass() before test class execution}: prior to any
   44 |  * <em>before class callbacks</em> of a particular testing framework &mdash; for
   45 |  * example, JUnit Jupiter's {@link org.junit.jupiter.api.BeforeAll @BeforeAll}</li>
   46 |  * <li>{@link #prepareTestInstance test instance preparation}:
   47 |  * immediately following instantiation of the test class</li>
   48 |  * <li>{@link #beforeTestMethod before test setup}:
   49 |  * prior to any <em>before method callbacks</em> of a particular testing framework &mdash;
   50 |  * for example, JUnit Jupiter's {@link org.junit.jupiter.api.BeforeEach @BeforeEach}</li>
   51 |  * <li>{@link #beforeTestExecution before test execution}:
   52 |  * immediately before execution of the {@linkplain java.lang.reflect.Method
   53 |  * test method} but after test setup</li>
   54 |  * <li>{@link #afterTestExecution after test execution}:
   55 |  * immediately after execution of the {@linkplain java.lang.reflect.Method
   56 |  * test method} but before test tear down</li>
   57 |  * <li>{@link #afterTestMethod(Object, Method, Throwable) after test tear down}:
   58 |  * after any <em>after method callbacks</em> of a particular testing framework &mdash;
   59 |  * for example, JUnit Jupiter's {@link org.junit.jupiter.api.AfterEach @AfterEach}</li>
   60 |  * <li>{@link #afterTestClass() after test class execution}: after any
   61 |  * <em>after class callbacks</em> of a particular testing framework &mdash; for example,
   62 |  * JUnit Jupiter's {@link org.junit.jupiter.api.AfterAll @AfterAll}</li>
   63 |  * </ul>
   64 |  *
   65 |  * <p>Support for loading and accessing
   66 |  * {@linkplain org.springframework.context.ApplicationContext application contexts},
   67 |  * dependency injection of test instances,
   68 |  * {@linkplain org.springframework.transaction.annotation.Transactional transactional}
   69 |  * execution of test methods, etc. is provided by
   70 |  * {@link SmartContextLoader ContextLoaders} and {@code TestExecutionListeners},
   71 |  * which are configured via {@link ContextConfiguration @ContextConfiguration} and
   72 |  * {@link TestExecutionListeners @TestExecutionListeners}, respectively.
   73 |  *
   74 |  * <p>Bootstrapping of the {@code TestContext}, the default {@code ContextLoader},
   75 |  * default {@code TestExecutionListeners}, and their collaborators is performed
   76 |  * by a {@link TestContextBootstrapper}, which is configured via
   77 |  * {@link BootstrapWith @BootstrapWith}.
   78 |  *
   79 |  * @author Sam Brannen
   80 |  * @author Juergen Hoeller
   81 |  * @since 2.5
   82 |  * @see BootstrapWith
   83 |  * @see BootstrapContext
   84 |  * @see TestContextBootstrapper
   85 |  * @see TestContext
   86 |  * @see TestExecutionListener
   87 |  * @see TestExecutionListeners
   88 |  * @see ContextConfiguration
   89 |  * @see ContextHierarchy
   90 |  */
   91 | public class TestContextManager {
   92 | 
   93 | 	private static final Log logger = LogFactory.getLog(TestContextManager.class);
   94 | 
   95 | 	private static final Set<Class<? extends Throwable>> skippedExceptionTypes = CollectionUtils.newLinkedHashSet(3);
   96 | 
   97 | 	static {
   98 | 		// JUnit Jupiter
   99 | 		registerSkippedExceptionType("org.opentest4j.TestAbortedException");
  100 | 		// JUnit 4
  101 | 		registerSkippedExceptionType("org.junit.AssumptionViolatedException");
  102 | 		// TestNG
  103 | 		registerSkippedExceptionType("org.testng.SkipException");
  104 | 	}
  105 | 
  106 | 	private final TestContext testContext;
  107 | 
  108 | 	private final ThreadLocal<TestContext> testContextHolder;
  109 | 
  110 | 	private final List<TestExecutionListener> testExecutionListeners = new ArrayList<>(8);
  111 | 
  112 | 
  113 | 	/**
  114 | 	 * Construct a new {@code TestContextManager} for the supplied {@linkplain Class test class}.
  115 | 	 * <p>Delegates to {@link #TestContextManager(TestContextBootstrapper)} with
  116 | 	 * the {@link TestContextBootstrapper} configured for the test class. If the
  117 | 	 * {@link BootstrapWith @BootstrapWith} annotation is present on the test
  118 | 	 * class, either directly or as a meta-annotation, then its
  119 | 	 * {@link BootstrapWith#value value} will be used as the bootstrapper type;
  120 | 	 * otherwise, the {@link org.springframework.test.context.support.DefaultTestContextBootstrapper
  121 | 	 * DefaultTestContextBootstrapper} will be used.
  122 | 	 * @param testClass the test class to be managed
  123 | 	 * @see #TestContextManager(TestContextBootstrapper)
  124 | 	 */
  125 | 	public TestContextManager(Class<?> testClass) {
  126 | 		this(BootstrapUtils.resolveTestContextBootstrapper(testClass));
  127 | 	}
  128 | 
  129 | 	/**
  130 | 	 * Construct a new {@code TestContextManager} using the supplied {@link TestContextBootstrapper}
  131 | 	 * and {@linkplain #registerTestExecutionListeners register} the necessary
  132 | 	 * {@link TestExecutionListener TestExecutionListeners}.
  133 | 	 * <p>Delegates to the supplied {@code TestContextBootstrapper} for building
  134 | 	 * the {@code TestContext} and retrieving the {@code TestExecutionListeners}.
  135 | 	 * @param testContextBootstrapper the bootstrapper to use
  136 | 	 * @since 4.2
  137 | 	 * @see TestContextBootstrapper#buildTestContext
  138 | 	 * @see TestContextBootstrapper#getTestExecutionListeners
  139 | 	 * @see #registerTestExecutionListeners
  140 | 	 */
  141 | 	public TestContextManager(TestContextBootstrapper testContextBootstrapper) {
  142 | 		this.testContext = testContextBootstrapper.buildTestContext();
  143 | 		this.testContextHolder = ThreadLocal.withInitial(() -> copyTestContext(this.testContext));
  144 | 		registerTestExecutionListeners(testContextBootstrapper.getTestExecutionListeners());
  145 | 	}
  146 | 
  147 | 	/**
  148 | 	 * Get the {@link TestContext} managed by this {@code TestContextManager}.
  149 | 	 */
  150 | 	public final TestContext getTestContext() {
  151 | 		return this.testContextHolder.get();
  152 | 	}
  153 | 
  154 | 	/**
  155 | 	 * Register the supplied list of {@link TestExecutionListener TestExecutionListeners}
  156 | 	 * by appending them to the list of listeners used by this {@code TestContextManager}.
  157 | 	 * @see #registerTestExecutionListeners(TestExecutionListener...)
  158 | 	 */
  159 | 	public void registerTestExecutionListeners(List<TestExecutionListener> testExecutionListeners) {
  160 | 		registerTestExecutionListeners(testExecutionListeners.toArray(new TestExecutionListener[0]));
  161 | 	}
  162 | 
  163 | 	/**
  164 | 	 * Register the supplied array of {@link TestExecutionListener TestExecutionListeners}
  165 | 	 * by appending them to the list of listeners used by this {@code TestContextManager}.
  166 | 	 */
  167 | 	public void registerTestExecutionListeners(TestExecutionListener... testExecutionListeners) {
  168 | 		for (TestExecutionListener listener : testExecutionListeners) {
  169 | 			if (logger.isTraceEnabled()) {
  170 | 				logger.trace("Registering TestExecutionListener: " + typeName(listener));
  171 | 			}
  172 | 			this.testExecutionListeners.add(listener);
  173 | 		}
  174 | 	}
  175 | 
  176 | 	/**
  177 | 	 * Get the current {@link TestExecutionListener TestExecutionListeners}
  178 | 	 * registered for this {@code TestContextManager}.
  179 | 	 * <p>Allows for modifications, for example, adding a listener to the beginning of the list.
  180 | 	 * However, make sure to keep the list stable while actually executing tests.
  181 | 	 */
  182 | 	public final List<TestExecutionListener> getTestExecutionListeners() {
  183 | 		return this.testExecutionListeners;
  184 | 	}
  185 | 
  186 | 	/**
  187 | 	 * Get a copy of the {@link TestExecutionListener TestExecutionListeners}
  188 | 	 * registered for this {@code TestContextManager} in reverse order.
  189 | 	 */
  190 | 	private List<TestExecutionListener> getReversedTestExecutionListeners() {
  191 | 		List<TestExecutionListener> listenersReversed = new ArrayList<>(getTestExecutionListeners());
  192 | 		Collections.reverse(listenersReversed);
  193 | 		return listenersReversed;
  194 | 	}
  195 | 
  196 | 	/**
  197 | 	 * Hook for pre-processing a test class <em>before</em> execution of any
  198 | 	 * tests within the class. Should be called prior to any framework-specific
  199 | 	 * <em>before class methods</em> &mdash; for example, methods annotated with
  200 | 	 * JUnit Jupiter's {@link org.junit.jupiter.api.BeforeAll @BeforeAll}.
  201 | 	 * <p>An attempt will be made to give each registered
  202 | 	 * {@link TestExecutionListener} a chance to pre-process the test class
  203 | 	 * execution. If a listener throws an exception, however, the remaining
  204 | 	 * registered listeners will <strong>not</strong> be called.
  205 | 	 * @throws Exception if a registered TestExecutionListener throws an
  206 | 	 * exception
  207 | 	 * @since 3.0
  208 | 	 * @see #getTestExecutionListeners()
  209 | 	 */
  210 | 	public void beforeTestClass() throws Exception {
  211 | 		try {
  212 | 			Class<?> testClass = getTestContext().getTestClass();
  213 | 			if (logger.isTraceEnabled()) {
  214 | 				logger.trace("beforeTestClass(): class [" + typeName(testClass) + "]");
  215 | 			}
  216 | 			getTestContext().updateState(null, null, null);
  217 | 
  218 | 			for (TestExecutionListener testExecutionListener : getTestExecutionListeners()) {
  219 | 				try {
  220 | 					testExecutionListener.beforeTestClass(getTestContext());
  221 | 				}
  222 | 				catch (Throwable ex) {
  223 | 					logException(ex, "beforeTestClass", testExecutionListener, testClass);
  224 | 					ReflectionUtils.rethrowException(ex);
  225 | 				}
  226 | 			}
  227 | 		}
  228 | 		finally {
  229 | 			resetMethodInvoker();
  230 | 		}
  231 | 	}
  232 | 
  233 | 	/**
  234 | 	 * Hook for preparing a test instance prior to execution of any individual
  235 | 	 * test methods &mdash; for example, to inject dependencies.
  236 | 	 * <p>This method should be called immediately after instantiation of the test
  237 | 	 * class or as soon after instantiation as possible (as is the case with the
  238 | 	 * {@link org.springframework.test.context.junit4.rules.SpringMethodRule
  239 | 	 * SpringMethodRule}). In any case, this method must be called prior to any
  240 | 	 * framework-specific lifecycle callbacks.
  241 | 	 * <p>The managed {@link TestContext} will be updated with the supplied
  242 | 	 * {@code testInstance}.
  243 | 	 * <p>An attempt will be made to give each registered
  244 | 	 * {@link TestExecutionListener} a chance to prepare the test instance. If a
  245 | 	 * listener throws an exception, however, the remaining registered listeners
  246 | 	 * will <strong>not</strong> be called.
  247 | 	 * @param testInstance the test instance to prepare
  248 | 	 * @throws Exception if a registered TestExecutionListener throws an exception
  249 | 	 * @see #getTestExecutionListeners()
  250 | 	 */
  251 | 	public void prepareTestInstance(Object testInstance) throws Exception {
  252 | 		try {
  253 | 			if (logger.isTraceEnabled()) {
  254 | 				logger.trace("prepareTestInstance(): instance [" + testInstance + "]");
  255 | 			}
  256 | 			getTestContext().updateState(testInstance, null, null);
  257 | 
  258 | 			for (TestExecutionListener testExecutionListener : getTestExecutionListeners()) {
  259 | 				try {
  260 | 					testExecutionListener.prepareTestInstance(getTestContext());
  261 | 				}
  262 | 				catch (Throwable ex) {
  263 | 					if (isSkippedException(ex)) {
  264 | 						if (logger.isInfoEnabled()) {
  265 | 							logger.info("""
  266 | 									Caught exception while allowing TestExecutionListener [%s] to \
  267 | 									prepare test instance [%s]"""
  268 | 										.formatted(typeName(testExecutionListener), testInstance), ex);
  269 | 						}
  270 | 					}
  271 | 					else if (logger.isWarnEnabled()) {
  272 | 						logger.warn("""
  273 | 							Caught exception while allowing TestExecutionListener [%s] to \
  274 | 							prepare test instance [%s]"""
  275 | 								.formatted(typeName(testExecutionListener), testInstance), ex);
  276 | 					}
  277 | 					ReflectionUtils.rethrowException(ex);
  278 | 				}
  279 | 			}
  280 | 		}
  281 | 		finally {
  282 | 			resetMethodInvoker();
  283 | 		}
  284 | 	}
  285 | 
  286 | 	/**
  287 | 	 * Hook for pre-processing a test <em>before</em> execution of <em>before</em>
  288 | 	 * lifecycle callbacks of the underlying test framework &mdash; for example,
  289 | 	 * setting up test fixtures, starting a transaction, etc.
  290 | 	 * <p>This method <strong>must</strong> be called immediately prior to
  291 | 	 * framework-specific <em>before</em> lifecycle callbacks &mdash; for example, methods
  292 | 	 * annotated with JUnit Jupiter's {@link org.junit.jupiter.api.BeforeEach @BeforeEach}.
  293 | 	 * For historical reasons, this method is named {@code beforeTestMethod}. Since
  294 | 	 * the introduction of {@link #beforeTestExecution}, a more suitable name for
  295 | 	 * this method might be something like {@code beforeTestSetUp} or
  296 | 	 * {@code beforeEach}; however, it is unfortunately impossible to rename
  297 | 	 * this method due to backward compatibility concerns.
  298 | 	 * <p>The managed {@link TestContext} will be updated with the supplied
  299 | 	 * {@code testInstance} and {@code testMethod}.
  300 | 	 * <p>An attempt will be made to give each registered
  301 | 	 * {@link TestExecutionListener} a chance to perform its pre-processing.
  302 | 	 * If a listener throws an exception, however, the remaining registered
  303 | 	 * listeners will <strong>not</strong> be called.
  304 | 	 * @param testInstance the current test instance
  305 | 	 * @param testMethod the test method which is about to be executed on the
  306 | 	 * test instance
  307 | 	 * @throws Exception if a registered TestExecutionListener throws an exception
  308 | 	 * @see #afterTestMethod
  309 | 	 * @see #beforeTestExecution
  310 | 	 * @see #afterTestExecution
  311 | 	 * @see #getTestExecutionListeners()
  312 | 	 */
  313 | 	public void beforeTestMethod(Object testInstance, Method testMethod) throws Exception {
  314 | 		try {
  315 | 			String callbackName = "beforeTestMethod";
  316 | 			prepareForBeforeCallback(callbackName, testInstance, testMethod);
  317 | 
  318 | 			for (TestExecutionListener testExecutionListener : getTestExecutionListeners()) {
  319 | 				try {
  320 | 					testExecutionListener.beforeTestMethod(getTestContext());
  321 | 				}
  322 | 				catch (Throwable ex) {
  323 | 					handleBeforeException(ex, callbackName, testExecutionListener, testInstance, testMethod);
  324 | 				}
  325 | 			}
  326 | 		}
  327 | 		finally {
  328 | 			resetMethodInvoker();
  329 | 		}
  330 | 	}
  331 | 
  332 | 	/**
  333 | 	 * Hook for pre-processing a test <em>immediately before</em> execution of
  334 | 	 * the {@linkplain java.lang.reflect.Method test method} in the supplied
  335 | 	 * {@linkplain TestContext test context} &mdash; for example, for timing
  336 | 	 * or logging purposes.
  337 | 	 * <p>This method <strong>must</strong> be called after framework-specific
  338 | 	 * <em>before</em> lifecycle callbacks &mdash; for example, methods annotated
  339 | 	 * with JUnit Jupiter's {@link org.junit.jupiter.api.BeforeEach @BeforeEach}.
  340 | 	 * <p>The managed {@link TestContext} will be updated with the supplied
  341 | 	 * {@code testInstance} and {@code testMethod}.
  342 | 	 * <p>An attempt will be made to give each registered
  343 | 	 * {@link TestExecutionListener} a chance to perform its pre-processing.
  344 | 	 * If a listener throws an exception, however, the remaining registered
  345 | 	 * listeners will <strong>not</strong> be called.
  346 | 	 * @param testInstance the current test instance
  347 | 	 * @param testMethod the test method which is about to be executed on the
  348 | 	 * test instance
  349 | 	 * @throws Exception if a registered TestExecutionListener throws an exception
  350 | 	 * @since 5.0
  351 | 	 * @see #beforeTestMethod
  352 | 	 * @see #afterTestMethod
  353 | 	 * @see #beforeTestExecution
  354 | 	 * @see #afterTestExecution
  355 | 	 * @see #getTestExecutionListeners()
  356 | 	 */
  357 | 	public void beforeTestExecution(Object testInstance, Method testMethod) throws Exception {
  358 | 		try {
  359 | 			String callbackName = "beforeTestExecution";
  360 | 			prepareForBeforeCallback(callbackName, testInstance, testMethod);
  361 | 
  362 | 			for (TestExecutionListener testExecutionListener : getTestExecutionListeners()) {
  363 | 				try {
  364 | 					testExecutionListener.beforeTestExecution(getTestContext());
  365 | 				}
  366 | 				catch (Throwable ex) {
  367 | 					handleBeforeException(ex, callbackName, testExecutionListener, testInstance, testMethod);
  368 | 				}
  369 | 			}
  370 | 		}
  371 | 		finally {
  372 | 			resetMethodInvoker();
  373 | 		}
  374 | 	}
  375 | 
  376 | 	/**
  377 | 	 * Hook for post-processing a test <em>immediately after</em> execution of
  378 | 	 * the {@linkplain java.lang.reflect.Method test method} in the supplied
  379 | 	 * {@linkplain TestContext test context} &mdash; for example, for timing
  380 | 	 * or logging purposes.
  381 | 	 * <p>This method <strong>must</strong> be called before framework-specific
  382 | 	 * <em>after</em> lifecycle callbacks &mdash; for example, methods annotated
  383 | 	 * with JUnit Jupiter's {@link org.junit.jupiter.api.AfterEach @AfterEach}.
  384 | 	 * <p>The managed {@link TestContext} will be updated with the supplied
  385 | 	 * {@code testInstance}, {@code testMethod}, and {@code exception}.
  386 | 	 * <p>Each registered {@link TestExecutionListener} will be given a chance
  387 | 	 * to perform its post-processing. If a listener throws an exception, the
  388 | 	 * remaining registered listeners will still be called. After all listeners
  389 | 	 * have executed, the first caught exception will be rethrown with any
  390 | 	 * subsequent exceptions {@linkplain Throwable#addSuppressed suppressed} in
  391 | 	 * the first exception.
  392 | 	 * <p>Note that listeners will be executed in the opposite order in which they
  393 | 	 * were registered.
  394 | 	 * @param testInstance the current test instance
  395 | 	 * @param testMethod the test method which has just been executed on the
  396 | 	 * test instance
  397 | 	 * @param exception the exception that was thrown during execution of the
  398 | 	 * test method or by a TestExecutionListener, or {@code null} if none
  399 | 	 * was thrown
  400 | 	 * @throws Exception if a registered TestExecutionListener throws an exception
  401 | 	 * @since 5.0
  402 | 	 * @see #beforeTestMethod
  403 | 	 * @see #afterTestMethod
  404 | 	 * @see #beforeTestExecution
  405 | 	 * @see #getTestExecutionListeners()
  406 | 	 * @see Throwable#addSuppressed(Throwable)
  407 | 	 */
  408 | 	public void afterTestExecution(Object testInstance, Method testMethod, @Nullable Throwable exception)
  409 | 			throws Exception {
  410 | 
  411 | 		try {
  412 | 			String callbackName = "afterTestExecution";
  413 | 			prepareForAfterCallback(callbackName, testInstance, testMethod, exception);
  414 | 			Throwable afterTestExecutionException = null;
  415 | 
  416 | 			// Traverse the TestExecutionListeners in reverse order to ensure proper
  417 | 			// "wrapper"-style execution of listeners.
  418 | 			for (TestExecutionListener testExecutionListener : getReversedTestExecutionListeners()) {
  419 | 				try {
  420 | 					testExecutionListener.afterTestExecution(getTestContext());
  421 | 				}
  422 | 				catch (Throwable ex) {
  423 | 					logException(ex, callbackName, testExecutionListener, testInstance, testMethod);
  424 | 					if (afterTestExecutionException == null) {
  425 | 						afterTestExecutionException = ex;
  426 | 					}
  427 | 					else {
  428 | 						afterTestExecutionException.addSuppressed(ex);
  429 | 					}
  430 | 				}
  431 | 			}
  432 | 
  433 | 			if (afterTestExecutionException != null) {
  434 | 				ReflectionUtils.rethrowException(afterTestExecutionException);
  435 | 			}
  436 | 		}
  437 | 		finally {
  438 | 			resetMethodInvoker();
  439 | 		}
  440 | 	}
  441 | 
  442 | 	/**
  443 | 	 * Hook for post-processing a test <em>after</em> execution of <em>after</em>
  444 | 	 * lifecycle callbacks of the underlying test framework &mdash; for example,
  445 | 	 * tearing down test fixtures, ending a transaction, etc.
  446 | 	 * <p>This method <strong>must</strong> be called immediately after
  447 | 	 * framework-specific <em>after</em> lifecycle callbacks &mdash; for example, methods
  448 | 	 * annotated with JUnit Jupiter's {@link org.junit.jupiter.api.AfterEach @AfterEach}.
  449 | 	 * For historical reasons, this method is named {@code afterTestMethod}. Since
  450 | 	 * the introduction of {@link #afterTestExecution}, a more suitable name for
  451 | 	 * this method might be something like {@code afterTestTearDown} or
  452 | 	 * {@code afterEach}; however, it is unfortunately impossible to rename
  453 | 	 * this method due to backward compatibility concerns.
  454 | 	 * <p>The managed {@link TestContext} will be updated with the supplied
  455 | 	 * {@code testInstance}, {@code testMethod}, and {@code exception}.
  456 | 	 * <p>Each registered {@link TestExecutionListener} will be given a chance
  457 | 	 * to perform its post-processing. If a listener throws an exception, the
  458 | 	 * remaining registered listeners will still be called. After all listeners
  459 | 	 * have executed, the first caught exception will be rethrown with any
  460 | 	 * subsequent exceptions {@linkplain Throwable#addSuppressed suppressed} in
  461 | 	 * the first exception.
  462 | 	 * <p>Note that listeners will be executed in the opposite order in which they
  463 | 	 * were registered.
  464 | 	 * @param testInstance the current test instance
  465 | 	 * @param testMethod the test method which has just been executed on the
  466 | 	 * test instance
  467 | 	 * @param exception the exception that was thrown during execution of the test
  468 | 	 * method or by a TestExecutionListener, or {@code null} if none was thrown
  469 | 	 * @throws Exception if a registered TestExecutionListener throws an exception
  470 | 	 * @see #beforeTestMethod
  471 | 	 * @see #beforeTestExecution
  472 | 	 * @see #afterTestExecution
  473 | 	 * @see #getTestExecutionListeners()
  474 | 	 * @see Throwable#addSuppressed(Throwable)
  475 | 	 */
  476 | 	public void afterTestMethod(Object testInstance, Method testMethod, @Nullable Throwable exception)
  477 | 			throws Exception {
  478 | 
  479 | 		try {
  480 | 			String callbackName = "afterTestMethod";
  481 | 			prepareForAfterCallback(callbackName, testInstance, testMethod, exception);
  482 | 			Throwable afterTestMethodException = null;
  483 | 
  484 | 			// Traverse the TestExecutionListeners in reverse order to ensure proper
  485 | 			// "wrapper"-style execution of listeners.
  486 | 			for (TestExecutionListener testExecutionListener : getReversedTestExecutionListeners()) {
  487 | 				try {
  488 | 					testExecutionListener.afterTestMethod(getTestContext());
  489 | 				}
  490 | 				catch (Throwable ex) {
  491 | 					logException(ex, callbackName, testExecutionListener, testInstance, testMethod);
  492 | 					if (afterTestMethodException == null) {
  493 | 						afterTestMethodException = ex;
  494 | 					}
  495 | 					else {
  496 | 						afterTestMethodException.addSuppressed(ex);
  497 | 					}
  498 | 				}
  499 | 			}
  500 | 
  501 | 			if (afterTestMethodException != null) {
  502 | 				ReflectionUtils.rethrowException(afterTestMethodException);
  503 | 			}
  504 | 		}
  505 | 		finally {
  506 | 			resetMethodInvoker();
  507 | 		}
  508 | 	}
  509 | 
  510 | 	/**
  511 | 	 * Hook for post-processing a test class <em>after</em> execution of all
  512 | 	 * tests within the class. Should be called after any framework-specific
  513 | 	 * <em>after class methods</em> &mdash; for example, methods annotated with
  514 | 	 * JUnit Jupiter's {@link org.junit.jupiter.api.AfterAll @AfterAll}.
  515 | 	 * <p>Each registered {@link TestExecutionListener} will be given a chance
  516 | 	 * to perform its post-processing. If a listener throws an exception, the
  517 | 	 * remaining registered listeners will still be called. After all listeners
  518 | 	 * have executed, the first caught exception will be rethrown with any
  519 | 	 * subsequent exceptions {@linkplain Throwable#addSuppressed suppressed} in
  520 | 	 * the first exception.
  521 | 	 * <p>Note that listeners will be executed in the opposite order in which they
  522 | 	 * were registered.
  523 | 	 * <p>As of Spring Framework 7.0, this method also ensures that the application
  524 | 	 * context for the current {@link #getTestContext() TestContext} is marked as
  525 | 	 * {@linkplain TestContext#markApplicationContextUnused() unused}.
  526 | 	 * @throws Exception if a registered TestExecutionListener throws an exception
  527 | 	 * @since 3.0
  528 | 	 * @see #getTestExecutionListeners()
  529 | 	 * @see Throwable#addSuppressed(Throwable)
  530 | 	 * @see TestContext#markApplicationContextUnused()
  531 | 	 */
  532 | 	public void afterTestClass() throws Exception {
  533 | 		Class<?> testClass = getTestContext().getTestClass();
  534 | 		if (logger.isTraceEnabled()) {
  535 | 			logger.trace("afterTestClass(): class [" + typeName(testClass) + "]");
  536 | 		}
  537 | 		getTestContext().updateState(null, null, null);
  538 | 
  539 | 		Throwable afterTestClassException = null;
  540 | 		// Traverse the TestExecutionListeners in reverse order to ensure proper
  541 | 		// "wrapper"-style execution of listeners.
  542 | 		for (TestExecutionListener testExecutionListener : getReversedTestExecutionListeners()) {
  543 | 			try {
  544 | 				testExecutionListener.afterTestClass(getTestContext());
  545 | 			}
  546 | 			catch (Throwable ex) {
  547 | 				logException(ex, "afterTestClass", testExecutionListener, testClass);
  548 | 				if (afterTestClassException == null) {
  549 | 					afterTestClassException = ex;
  550 | 				}
  551 | 				else {
  552 | 					afterTestClassException.addSuppressed(ex);
  553 | 				}
  554 | 			}
  555 | 		}
  556 | 
  557 | 		try {
  558 | 			if (getTestContext().hasApplicationContext()) {
  559 | 				getTestContext().markApplicationContextUnused();
  560 | 			}
  561 | 		}
  562 | 		catch (Throwable ex) {
  563 | 			if (afterTestClassException == null) {
  564 | 				afterTestClassException = ex;
  565 | 			}
  566 | 			else {
  567 | 				afterTestClassException.addSuppressed(ex);
  568 | 			}
  569 | 		}
  570 | 
  571 | 		this.testContextHolder.remove();
  572 | 
  573 | 		if (afterTestClassException != null) {
  574 | 			ReflectionUtils.rethrowException(afterTestClassException);
  575 | 		}
  576 | 	}
  577 | 
  578 | 	/**
  579 | 	 * Reset the {@link MethodInvoker} to the default to ensure that a custom
  580 | 	 * {@code MethodInvoker} for the current test execution is not retained for
  581 | 	 * subsequent test executions.
  582 | 	 */
  583 | 	private void resetMethodInvoker() {
  584 | 		getTestContext().setMethodInvoker(MethodInvoker.DEFAULT_INVOKER);
  585 | 	}
  586 | 
  587 | 	private void prepareForBeforeCallback(String callbackName, Object testInstance, Method testMethod) {
  588 | 		if (logger.isTraceEnabled()) {
  589 | 			logger.trace("%s(): instance [%s], method [%s]".formatted(callbackName, testInstance, testMethod));
  590 | 		}
  591 | 		getTestContext().updateState(testInstance, testMethod, null);
  592 | 	}
  593 | 
  594 | 	private void prepareForAfterCallback(String callbackName, Object testInstance, Method testMethod,
  595 | 			@Nullable Throwable exception) {
  596 | 
  597 | 		if (logger.isTraceEnabled()) {
  598 | 			logger.trace("%s(): instance [%s], method [%s], exception [%s]"
  599 | 					.formatted(callbackName, testInstance, testMethod, exception));
  600 | 		}
  601 | 		getTestContext().updateState(testInstance, testMethod, exception);
  602 | 	}
  603 | 
  604 | 	private void handleBeforeException(Throwable ex, String callbackName, TestExecutionListener testExecutionListener,
  605 | 			Object testInstance, Method testMethod) throws Exception {
  606 | 
  607 | 		logException(ex, callbackName, testExecutionListener, testInstance, testMethod);
  608 | 		ReflectionUtils.rethrowException(ex);
  609 | 	}
  610 | 
  611 | 	private void logException(
  612 | 			Throwable ex, String callbackName, TestExecutionListener testExecutionListener, Class<?> testClass) {
  613 | 
  614 | 		if (isSkippedException(ex)) {
  615 | 			if (logger.isInfoEnabled()) {
  616 | 				logger.info("""
  617 | 						Caught exception while invoking '%s' callback on TestExecutionListener [%s] \
  618 | 						for test class [%s]"""
  619 | 							.formatted(callbackName, typeName(testExecutionListener), typeName(testClass)), ex);
  620 | 			}
  621 | 		}
  622 | 		else if (logger.isWarnEnabled()) {
  623 | 			logger.warn("""
  624 | 					Caught exception while invoking '%s' callback on TestExecutionListener [%s] \
  625 | 					for test class [%s]"""
  626 | 						.formatted(callbackName, typeName(testExecutionListener), typeName(testClass)), ex);
  627 | 		}
  628 | 	}
  629 | 
  630 | 	private void logException(Throwable ex, String callbackName, TestExecutionListener testExecutionListener,
  631 | 			Object testInstance, Method testMethod) {
  632 | 
  633 | 		if (isSkippedException(ex)) {
  634 | 			if (logger.isInfoEnabled()) {
  635 | 				logger.info("""
  636 | 						Caught exception while invoking '%s' callback on TestExecutionListener [%s] for \
  637 | 						test method [%s] and test instance [%s]"""
  638 | 							.formatted(callbackName, typeName(testExecutionListener), testMethod, testInstance), ex);
  639 | 			}
  640 | 		}
  641 | 		else if (logger.isWarnEnabled()) {
  642 | 			logger.warn("""
  643 | 					Caught exception while invoking '%s' callback on TestExecutionListener [%s] for \
  644 | 					test method [%s] and test instance [%s]"""
  645 | 						.formatted(callbackName, typeName(testExecutionListener), testMethod, testInstance), ex);
  646 | 		}
  647 | 	}
  648 | 
  649 | 
  650 | 	/**
  651 | 	 * Attempt to create a copy of the supplied {@code TestContext} using its
  652 | 	 * <em>copy constructor</em>.
  653 | 	 */
  654 | 	private static TestContext copyTestContext(TestContext testContext) {
  655 | 		Constructor<? extends TestContext> constructor =
  656 | 				ClassUtils.getConstructorIfAvailable(testContext.getClass(), testContext.getClass());
  657 | 
  658 | 		if (constructor != null) {
  659 | 			try {
  660 | 				ReflectionUtils.makeAccessible(constructor);
  661 | 				return constructor.newInstance(testContext);
  662 | 			}
  663 | 			catch (Exception ex) {
  664 | 				if (logger.isInfoEnabled()) {
  665 | 					logger.info("""
  666 | 							Failed to invoke copy constructor for [%s]; concurrent test execution \
  667 | 							is therefore likely not supported.""".formatted(testContext), ex);
  668 | 				}
  669 | 			}
  670 | 		}
  671 | 
  672 | 		// Fallback to original instance
  673 | 		return testContext;
  674 | 	}
  675 | 
  676 | 	private static String typeName(Object obj) {
  677 | 		if (obj instanceof Class<?> type) {
  678 | 			return type.getName();
  679 | 		}
  680 | 		return obj.getClass().getName();
  681 | 	}
  682 | 
  683 | 	@SuppressWarnings("unchecked")
  684 | 	private static void registerSkippedExceptionType(String name) {
  685 | 		try {
  686 | 			Class<? extends Throwable> exceptionType = (Class<? extends Throwable>)
  687 | 					ClassUtils.forName(name, TestContextManager.class.getClassLoader());
  688 | 			skippedExceptionTypes.add(exceptionType);
  689 | 		}
  690 | 		catch (ClassNotFoundException | LinkageError ex) {
  691 | 			// ignore
  692 | 		}
  693 | 	}
  694 | 
  695 | 	private static boolean isSkippedException(Throwable ex) {
  696 | 		for (Class<? extends Throwable> skippedExceptionType : skippedExceptionTypes) {
  697 | 			if (skippedExceptionType.isInstance(ex)) {
  698 | 				return true;
  699 | 			}
  700 | 		}
  701 | 		return false;
  702 | 	}
  703 | 
  704 | }
```
