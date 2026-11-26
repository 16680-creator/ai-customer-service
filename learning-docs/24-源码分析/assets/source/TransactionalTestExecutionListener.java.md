<!-- learning-docs source archive（机械抓取原文，禁止手改代码内容） -->
<!-- origin: org.springframework:spring-test:7.1.0 | path: org/springframework/test/context/transaction/TransactionalTestExecutionListener.java -->
<!-- 正文引用：[[源码:TransactionalTestExecutionListener.java#L起-L止]] ；代码块头部：(src: TransactionalTestExecutionListener.java#L起-L止) -->

# TransactionalTestExecutionListener.java

| 项 | 值 |
| --- | --- |
| 来源 | `org.springframework:spring-test:7.1.0` |
| 路径 | `org/springframework/test/context/transaction/TransactionalTestExecutionListener.java` |
| 行数 | 499 |
| 在线地址 | https://repo1.maven.org/maven2/org/springframework/spring-test/7.1.0/spring-test-7.1.0-sources.jar |
| 抓取时间 | 2026-11-25T14:10:52.021Z |

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
   17 | package org.springframework.test.context.transaction;
   18 | 
   19 | import java.lang.annotation.Annotation;
   20 | import java.lang.reflect.InvocationTargetException;
   21 | import java.lang.reflect.Method;
   22 | import java.util.Arrays;
   23 | import java.util.Collections;
   24 | import java.util.List;
   25 | 
   26 | import org.apache.commons.logging.Log;
   27 | import org.apache.commons.logging.LogFactory;
   28 | import org.jspecify.annotations.Nullable;
   29 | 
   30 | import org.springframework.beans.BeansException;
   31 | import org.springframework.beans.factory.BeanFactory;
   32 | import org.springframework.beans.factory.annotation.BeanFactoryAnnotationUtils;
   33 | import org.springframework.core.annotation.AnnotatedElementUtils;
   34 | import org.springframework.test.annotation.Commit;
   35 | import org.springframework.test.annotation.Rollback;
   36 | import org.springframework.test.context.TestContext;
   37 | import org.springframework.test.context.TestContextAnnotationUtils;
   38 | import org.springframework.test.context.support.AbstractTestExecutionListener;
   39 | import org.springframework.transaction.PlatformTransactionManager;
   40 | import org.springframework.transaction.TransactionDefinition;
   41 | import org.springframework.transaction.TransactionStatus;
   42 | import org.springframework.transaction.annotation.AnnotationTransactionAttributeSource;
   43 | import org.springframework.transaction.annotation.Transactional;
   44 | import org.springframework.transaction.interceptor.TransactionAttribute;
   45 | import org.springframework.transaction.interceptor.TransactionAttributeSource;
   46 | import org.springframework.util.Assert;
   47 | import org.springframework.util.ReflectionUtils;
   48 | import org.springframework.util.ReflectionUtils.MethodFilter;
   49 | import org.springframework.util.StringUtils;
   50 | 
   51 | /**
   52 |  * {@code TestExecutionListener} that provides support for executing tests
   53 |  * within <em>test-managed transactions</em> by honoring Spring's
   54 |  * {@link Transactional @Transactional} annotation.
   55 |  *
   56 |  * <h3>Test-managed Transactions</h3>
   57 |  * <p><em>Test-managed transactions</em> are transactions that are managed
   58 |  * declaratively via this listener or programmatically via
   59 |  * {@link TestTransaction}. Such transactions should not be confused with
   60 |  * <em>Spring-managed transactions</em> (i.e., those managed directly
   61 |  * by Spring within the {@code ApplicationContext} loaded for tests) or
   62 |  * <em>application-managed transactions</em> (i.e., those managed
   63 |  * programmatically within application code that is invoked via tests).
   64 |  * Spring-managed and application-managed transactions will typically
   65 |  * participate in test-managed transactions; however, caution should be
   66 |  * taken if Spring-managed or application-managed transactions are
   67 |  * configured with any propagation type other than
   68 |  * {@link org.springframework.transaction.annotation.Propagation#REQUIRED REQUIRED}
   69 |  * or {@link org.springframework.transaction.annotation.Propagation#SUPPORTS SUPPORTS}.
   70 |  *
   71 |  * <h3>Enabling and Disabling Transactions</h3>
   72 |  * <p>Annotating a test method with {@code @Transactional} causes the test
   73 |  * to be run within a transaction that will, by default, be automatically
   74 |  * <em>rolled back</em> after completion of the test. If a test class is
   75 |  * annotated with {@code @Transactional}, each test method within that class
   76 |  * hierarchy or nested class hierarchy will be run within a transaction. Test
   77 |  * methods that are <em>not</em> annotated with {@code @Transactional} (at the
   78 |  * class or method level) will not be run within a transaction. Furthermore,
   79 |  * tests that <em>are</em> annotated with {@code @Transactional} but have the
   80 |  * {@link Transactional#propagation propagation} type set to
   81 |  * {@link org.springframework.transaction.annotation.Propagation#NOT_SUPPORTED NOT_SUPPORTED}
   82 |  * or {@link org.springframework.transaction.annotation.Propagation#NEVER NEVER}
   83 |  * will not be run within a transaction.
   84 |  *
   85 |  * <h3>Declarative Rollback and Commit Behavior</h3>
   86 |  * <p>By default, test transactions will be automatically <em>rolled back</em>
   87 |  * after completion of the test; however, transactional commit and rollback
   88 |  * behavior can be configured declaratively via the {@link Commit @Commit}
   89 |  * and {@link Rollback @Rollback} annotations at the class level and at the
   90 |  * method level.
   91 |  *
   92 |  * <h3>Programmatic Transaction Management</h3>
   93 |  * <p>As of Spring Framework 4.1, it is possible to interact with test-managed
   94 |  * transactions programmatically via the static methods in {@link TestTransaction}.
   95 |  * {@code TestTransaction} may be used within <em>test</em> methods,
   96 |  * <em>before</em> methods, and <em>after</em> methods.
   97 |  *
   98 |  * <h3>Executing Code outside of a Transaction</h3>
   99 |  * <p>When executing transactional tests, it is sometimes useful to be able to
  100 |  * execute certain <em>set up</em> or <em>tear down</em> code outside a
  101 |  * transaction. {@code TransactionalTestExecutionListener} provides such
  102 |  * support for methods annotated with {@link BeforeTransaction @BeforeTransaction}
  103 |  * or {@link AfterTransaction @AfterTransaction}. {@code @BeforeTransaction} and
  104 |  * {@code @AfterTransaction} may also be declared on interface default methods.
  105 |  *
  106 |  * <h3>Configuring a Transaction Manager</h3>
  107 |  * <p>{@code TransactionalTestExecutionListener} expects a
  108 |  * {@link PlatformTransactionManager} bean to be defined in the Spring
  109 |  * {@code ApplicationContext} for the test. In case there are multiple
  110 |  * instances of {@code PlatformTransactionManager} within the test's
  111 |  * {@code ApplicationContext}, a <em>qualifier</em> may be declared via
  112 |  * {@link Transactional @Transactional} (for example, {@code @Transactional("myTxMgr")}
  113 |  * or {@code @Transactional(transactionManager = "myTxMgr")}, or
  114 |  * {@link org.springframework.transaction.annotation.TransactionManagementConfigurer
  115 |  * TransactionManagementConfigurer} can be implemented by an
  116 |  * {@link org.springframework.context.annotation.Configuration @Configuration}
  117 |  * class. See {@link TestContextTransactionUtils#retrieveTransactionManager}
  118 |  * for details on the algorithm used to look up a transaction manager in
  119 |  * the test's {@code ApplicationContext}.
  120 |  *
  121 |  * <h3>{@code @Transactional} Attribute Support</h3>
  122 |  * <table border="1">
  123 |  * <tr><th>Attribute</th><th>Supported for test-managed transactions</th></tr>
  124 |  * <tr><td>{@link Transactional#value value} and {@link Transactional#transactionManager transactionManager}</td><td>yes</td></tr>
  125 |  * <tr><td>{@link Transactional#propagation propagation}</td>
  126 |  * <td>only {@link org.springframework.transaction.annotation.Propagation#NOT_SUPPORTED NOT_SUPPORTED}
  127 |  * and {@link org.springframework.transaction.annotation.Propagation#NEVER NEVER} are supported</td></tr>
  128 |  * <tr><td>{@link Transactional#isolation isolation}</td><td>no</td></tr>
  129 |  * <tr><td>{@link Transactional#timeout timeout}</td><td>no</td></tr>
  130 |  * <tr><td>{@link Transactional#readOnly readOnly}</td><td>no</td></tr>
  131 |  * <tr><td>{@link Transactional#rollbackFor rollbackFor} and {@link Transactional#rollbackForClassName rollbackForClassName}</td>
  132 |  * <td>no: use {@link TestTransaction#flagForRollback()} instead</td></tr>
  133 |  * <tr><td>{@link Transactional#noRollbackFor noRollbackFor} and {@link Transactional#noRollbackForClassName noRollbackForClassName}</td>
  134 |  * <td>no: use {@link TestTransaction#flagForCommit()} instead</td></tr>
  135 |  * </table>
  136 |  *
  137 |  * @author Sam Brannen
  138 |  * @author Juergen Hoeller
  139 |  * @since 2.5
  140 |  * @see org.springframework.transaction.annotation.TransactionManagementConfigurer
  141 |  * @see org.springframework.transaction.annotation.Transactional
  142 |  * @see org.springframework.test.annotation.Commit
  143 |  * @see org.springframework.test.annotation.Rollback
  144 |  * @see BeforeTransaction
  145 |  * @see AfterTransaction
  146 |  * @see TestTransaction
  147 |  */
  148 | public class TransactionalTestExecutionListener extends AbstractTestExecutionListener {
  149 | 
  150 | 	/**
  151 | 	 * The {@link #getOrder() order} value for this listener: {@value}.
  152 | 	 * @since 6.2.3
  153 | 	 */
  154 | 	public static final int ORDER = 4000;
  155 | 
  156 | 	private static final Log logger = LogFactory.getLog(TransactionalTestExecutionListener.class);
  157 | 
  158 | 	// Do not require @Transactional test methods to be public.
  159 | 	@SuppressWarnings("serial")
  160 | 	protected final TransactionAttributeSource attributeSource = new AnnotationTransactionAttributeSource(false) {
  161 | 
  162 | 		@Override
  163 | 		protected @Nullable TransactionAttribute findTransactionAttribute(Class<?> clazz) {
  164 | 			// @Transactional present in inheritance hierarchy?
  165 | 			TransactionAttribute result = super.findTransactionAttribute(clazz);
  166 | 			if (result != null) {
  167 | 				return result;
  168 | 			}
  169 | 			// @Transactional present in enclosing class hierarchy?
  170 | 			return findTransactionAttributeInEnclosingClassHierarchy(clazz);
  171 | 		}
  172 | 
  173 | 		private @Nullable TransactionAttribute findTransactionAttributeInEnclosingClassHierarchy(Class<?> clazz) {
  174 | 			if (TestContextAnnotationUtils.searchEnclosingClass(clazz)) {
  175 | 				return findTransactionAttribute(clazz.getEnclosingClass());
  176 | 			}
  177 | 			return null;
  178 | 		}
  179 | 	};
  180 | 
  181 | 
  182 | 	/**
  183 | 	 * Returns {@value #ORDER}, which ensures that the {@code TransactionalTestExecutionListener}
  184 | 	 * is ordered after the
  185 | 	 * {@link org.springframework.test.context.support.CommonCachesTestExecutionListener
  186 | 	 * CommonCachesTestExecutionListener} and before the
  187 | 	 * {@link org.springframework.test.context.jdbc.SqlScriptsTestExecutionListener
  188 | 	 * SqlScriptsTestExecutionListener}.
  189 | 	 */
  190 | 	@Override
  191 | 	public final int getOrder() {
  192 | 		return ORDER;
  193 | 	}
  194 | 
  195 | 	/**
  196 | 	 * If the test method of the supplied {@linkplain TestContext test context}
  197 | 	 * is configured to run within a transaction, this method will run
  198 | 	 * {@link BeforeTransaction @BeforeTransaction} methods and start a new
  199 | 	 * transaction.
  200 | 	 * <p>Note that if a {@code @BeforeTransaction} method fails, any remaining
  201 | 	 * {@code @BeforeTransaction} methods will not be invoked, and a transaction
  202 | 	 * will not be started.
  203 | 	 * @see Transactional @Transactional
  204 | 	 * @see #getTransactionManager(TestContext, String)
  205 | 	 */
  206 | 	@Override
  207 | 	@SuppressWarnings("NullAway") // Dataflow analysis limitation
  208 | 	public void beforeTestMethod(final TestContext testContext) throws Exception {
  209 | 		Method testMethod = testContext.getTestMethod();
  210 | 		Class<?> testClass = testContext.getTestClass();
  211 | 		Assert.notNull(testMethod, "Test method of supplied TestContext must not be null");
  212 | 
  213 | 		TransactionContext txContext = TransactionContextHolder.removeCurrentTransactionContext();
  214 | 		Assert.state(txContext == null, "Cannot start new transaction without ending existing transaction");
  215 | 
  216 | 		PlatformTransactionManager tm = null;
  217 | 		TransactionAttribute transactionAttribute = this.attributeSource.getTransactionAttribute(testMethod, testClass);
  218 | 
  219 | 		if (transactionAttribute != null) {
  220 | 			transactionAttribute = TestContextTransactionUtils.createDelegatingTransactionAttribute(testContext,
  221 | 				transactionAttribute);
  222 | 
  223 | 			if (logger.isTraceEnabled()) {
  224 | 				logger.trace("Explicit transaction definition [%s] found for test context %s"
  225 | 						.formatted(transactionAttribute, testContext));
  226 | 			}
  227 | 			else if (logger.isDebugEnabled()) {
  228 | 				logger.debug("Explicit transaction definition [%s] found for test class [%s] and test method [%s]"
  229 | 						.formatted(transactionAttribute, testClass.getName(), testMethod.getName()));
  230 | 			}
  231 | 
  232 | 			if (transactionAttribute.getPropagationBehavior() == TransactionDefinition.PROPAGATION_NOT_SUPPORTED ||
  233 | 					transactionAttribute.getPropagationBehavior() == TransactionDefinition.PROPAGATION_NEVER) {
  234 | 				return;
  235 | 			}
  236 | 
  237 | 			tm = getTransactionManager(testContext, transactionAttribute.getQualifier());
  238 | 			Assert.state(tm != null,
  239 | 					() -> "Failed to retrieve PlatformTransactionManager for @Transactional test: " + testContext);
  240 | 		}
  241 | 
  242 | 		if (tm != null) {
  243 | 			txContext = new TransactionContext(testContext, tm, transactionAttribute, isRollback(testContext));
  244 | 			runBeforeTransactionMethods(testContext);
  245 | 			txContext.startTransaction();
  246 | 			TransactionContextHolder.setCurrentTransactionContext(txContext);
  247 | 		}
  248 | 	}
  249 | 
  250 | 	/**
  251 | 	 * If a transaction is currently active for the supplied
  252 | 	 * {@linkplain TestContext test context}, this method will end the transaction
  253 | 	 * and run {@link AfterTransaction @AfterTransaction} methods.
  254 | 	 * <p>{@code @AfterTransaction} methods are guaranteed to be invoked even if
  255 | 	 * an error occurs while ending the transaction.
  256 | 	 */
  257 | 	@Override
  258 | 	public void afterTestMethod(TestContext testContext) throws Exception {
  259 | 		Method testMethod = testContext.getTestMethod();
  260 | 		Assert.notNull(testMethod, "The test method of the supplied TestContext must not be null");
  261 | 
  262 | 		TransactionContext txContext = TransactionContextHolder.removeCurrentTransactionContext();
  263 | 		// If there was (or perhaps still is) a transaction...
  264 | 		if (txContext != null) {
  265 | 			TransactionStatus transactionStatus = txContext.getTransactionStatus();
  266 | 			try {
  267 | 				// If the transaction is still active...
  268 | 				if (transactionStatus != null && !transactionStatus.isCompleted()) {
  269 | 					txContext.endTransaction();
  270 | 				}
  271 | 			}
  272 | 			finally {
  273 | 				runAfterTransactionMethods(testContext);
  274 | 			}
  275 | 		}
  276 | 	}
  277 | 
  278 | 	/**
  279 | 	 * Run all {@link BeforeTransaction @BeforeTransaction} methods for the
  280 | 	 * specified {@linkplain TestContext test context}. If one of the methods
  281 | 	 * fails, however, the caught exception will be rethrown in a wrapped
  282 | 	 * {@link RuntimeException}, and the remaining methods will <strong>not</strong>
  283 | 	 * be given a chance to execute.
  284 | 	 * @param testContext the current test context
  285 | 	 */
  286 | 	protected void runBeforeTransactionMethods(TestContext testContext) throws Exception {
  287 | 		try {
  288 | 			Class<?> testClass = testContext.getTestClass();
  289 | 			List<Method> methods = getAnnotatedMethods(testClass, BeforeTransaction.class);
  290 | 			Collections.reverse(methods);
  291 | 			for (Method method : methods) {
  292 | 				if (logger.isTraceEnabled()) {
  293 | 					logger.trace("Executing @BeforeTransaction method [%s] for test context %s"
  294 | 							.formatted(method, testContext));
  295 | 				}
  296 | 				else if (logger.isDebugEnabled()) {
  297 | 					logger.debug("Executing @BeforeTransaction method [%s] for test class [%s]"
  298 | 							.formatted(method, testClass.getName()));
  299 | 				}
  300 | 				testContext.getMethodInvoker().invoke(method, testContext.getTestInstance());
  301 | 			}
  302 | 		}
  303 | 		catch (InvocationTargetException ex) {
  304 | 			if (logger.isErrorEnabled()) {
  305 | 				logger.error("Exception encountered while executing @BeforeTransaction methods for test context " +
  306 | 						testContext, ex.getTargetException());
  307 | 			}
  308 | 			ReflectionUtils.rethrowException(ex.getTargetException());
  309 | 		}
  310 | 	}
  311 | 
  312 | 	/**
  313 | 	 * Run all {@link AfterTransaction @AfterTransaction} methods for the
  314 | 	 * specified {@linkplain TestContext test context}. If one of the methods
  315 | 	 * fails, the caught exception will be logged as an error, and the remaining
  316 | 	 * methods will be given a chance to execute. After all methods have
  317 | 	 * executed, the first caught exception, if any, will be rethrown.
  318 | 	 * @param testContext the current test context
  319 | 	 */
  320 | 	protected void runAfterTransactionMethods(TestContext testContext) throws Exception {
  321 | 		Throwable afterTransactionException = null;
  322 | 
  323 | 		Class<?> testClass = testContext.getTestClass();
  324 | 		List<Method> methods = getAnnotatedMethods(testClass, AfterTransaction.class);
  325 | 		for (Method method : methods) {
  326 | 			try {
  327 | 				if (logger.isTraceEnabled()) {
  328 | 					logger.trace("Executing @AfterTransaction method [%s] for test context %s"
  329 | 							.formatted(method, testContext));
  330 | 				}
  331 | 				else if (logger.isDebugEnabled()) {
  332 | 					logger.debug("Executing @AfterTransaction method [%s] for test class [%s]"
  333 | 							.formatted(method, testClass.getName()));
  334 | 				}
  335 | 				testContext.getMethodInvoker().invoke(method, testContext.getTestInstance());
  336 | 			}
  337 | 			catch (InvocationTargetException ex) {
  338 | 				Throwable targetException = ex.getTargetException();
  339 | 				if (afterTransactionException == null) {
  340 | 					afterTransactionException = targetException;
  341 | 				}
  342 | 				logger.error("Exception encountered while executing @AfterTransaction method [" + method +
  343 | 						"] for test context " + testContext, targetException);
  344 | 			}
  345 | 			catch (Exception ex) {
  346 | 				if (afterTransactionException == null) {
  347 | 					afterTransactionException = ex;
  348 | 				}
  349 | 				logger.error("Exception encountered while executing @AfterTransaction method [" + method +
  350 | 						"] for test context " + testContext, ex);
  351 | 			}
  352 | 		}
  353 | 
  354 | 		if (afterTransactionException != null) {
  355 | 			ReflectionUtils.rethrowException(afterTransactionException);
  356 | 		}
  357 | 	}
  358 | 
  359 | 	/**
  360 | 	 * Get the {@linkplain PlatformTransactionManager transaction manager} to use
  361 | 	 * for the supplied {@linkplain TestContext test context} and {@code qualifier}.
  362 | 	 * <p>Delegates to {@link #getTransactionManager(TestContext)} if the
  363 | 	 * supplied {@code qualifier} is {@code null} or empty.
  364 | 	 * @param testContext the test context for which the transaction manager
  365 | 	 * should be retrieved
  366 | 	 * @param qualifier the qualifier for selecting between multiple bean matches;
  367 | 	 * may be {@code null} or empty
  368 | 	 * @return the transaction manager to use, or {@code null} if not found
  369 | 	 * @throws BeansException if an error occurs while retrieving the transaction manager
  370 | 	 * @see #getTransactionManager(TestContext)
  371 | 	 */
  372 | 	protected @Nullable PlatformTransactionManager getTransactionManager(TestContext testContext, @Nullable String qualifier) {
  373 | 		// Look up by type and qualifier from @Transactional
  374 | 		if (StringUtils.hasText(qualifier)) {
  375 | 			try {
  376 | 				// Use autowire-capable factory in order to support extended qualifier matching
  377 | 				// (only exposed on the internal BeanFactory, not on the ApplicationContext).
  378 | 				BeanFactory bf = testContext.getApplicationContext().getAutowireCapableBeanFactory();
  379 | 
  380 | 				return BeanFactoryAnnotationUtils.qualifiedBeanOfType(bf, PlatformTransactionManager.class, qualifier);
  381 | 			}
  382 | 			catch (RuntimeException ex) {
  383 | 				if (logger.isWarnEnabled()) {
  384 | 					logger.warn(String.format(
  385 | 							"Caught exception while retrieving transaction manager with qualifier '%s' for test context %s",
  386 | 							qualifier, testContext), ex);
  387 | 				}
  388 | 				throw ex;
  389 | 			}
  390 | 		}
  391 | 
  392 | 		// else
  393 | 		return getTransactionManager(testContext);
  394 | 	}
  395 | 
  396 | 	/**
  397 | 	 * Get the {@linkplain PlatformTransactionManager transaction manager}
  398 | 	 * to use for the supplied {@linkplain TestContext test context}.
  399 | 	 * <p>The default implementation simply delegates to
  400 | 	 * {@link TestContextTransactionUtils#retrieveTransactionManager}.
  401 | 	 * @param testContext the test context for which the transaction manager
  402 | 	 * should be retrieved
  403 | 	 * @return the transaction manager to use, or {@code null} if not found
  404 | 	 * @throws BeansException if an error occurs while retrieving an explicitly
  405 | 	 * named transaction manager
  406 | 	 * @throws IllegalStateException if more than one TransactionManagementConfigurer
  407 | 	 * exists in the ApplicationContext
  408 | 	 * @see #getTransactionManager(TestContext, String)
  409 | 	 */
  410 | 	protected @Nullable PlatformTransactionManager getTransactionManager(TestContext testContext) {
  411 | 		return TestContextTransactionUtils.retrieveTransactionManager(testContext, null);
  412 | 	}
  413 | 
  414 | 	/**
  415 | 	 * Determine whether to rollback transactions by default for the
  416 | 	 * supplied {@linkplain TestContext test context}.
  417 | 	 * <p>Supports {@link Rollback @Rollback} or {@link Commit @Commit} at the
  418 | 	 * class-level.
  419 | 	 * @param testContext the test context for which the default rollback flag
  420 | 	 * should be retrieved
  421 | 	 * @return the <em>default rollback</em> flag for the supplied test context
  422 | 	 * @throws Exception if an error occurs while determining the default rollback flag
  423 | 	 */
  424 | 	@SuppressWarnings("NullAway")
  425 | 	protected final boolean isDefaultRollback(TestContext testContext) throws Exception {
  426 | 		Class<?> testClass = testContext.getTestClass();
  427 | 		Rollback rollback = TestContextAnnotationUtils.findMergedAnnotation(testClass, Rollback.class);
  428 | 		boolean rollbackPresent = (rollback != null);
  429 | 
  430 | 		if (rollbackPresent) {
  431 | 			boolean defaultRollback = rollback.value();
  432 | 			if (logger.isDebugEnabled()) {
  433 | 				logger.debug(String.format("Retrieved default @Rollback(%s) for test class [%s].",
  434 | 						defaultRollback, testClass.getName()));
  435 | 			}
  436 | 			return defaultRollback;
  437 | 		}
  438 | 
  439 | 		// else
  440 | 		return true;
  441 | 	}
  442 | 
  443 | 	/**
  444 | 	 * Determine whether to rollback transactions for the supplied
  445 | 	 * {@linkplain TestContext test context} by taking into consideration the
  446 | 	 * {@linkplain #isDefaultRollback(TestContext) default rollback} flag and a
  447 | 	 * possible method-level override via the {@link Rollback @Rollback}
  448 | 	 * annotation.
  449 | 	 * @param testContext the test context for which the rollback flag
  450 | 	 * should be retrieved
  451 | 	 * @return the <em>rollback</em> flag for the supplied test context
  452 | 	 * @throws Exception if an error occurs while determining the rollback flag
  453 | 	 */
  454 | 	protected final boolean isRollback(TestContext testContext) throws Exception {
  455 | 		boolean rollback = isDefaultRollback(testContext);
  456 | 		Method testMethod = testContext.getTestMethod();
  457 | 		Rollback rollbackAnnotation = AnnotatedElementUtils.findMergedAnnotation(testMethod, Rollback.class);
  458 | 		if (rollbackAnnotation != null) {
  459 | 			boolean rollbackOverride = rollbackAnnotation.value();
  460 | 			if (logger.isTraceEnabled()) {
  461 | 				logger.trace("Method-level @Rollback(%s) overrides default rollback [%s] for test context %s"
  462 | 						.formatted(rollbackOverride, rollback, testContext));
  463 | 			}
  464 | 			else if (logger.isDebugEnabled()) {
  465 | 				logger.debug("Method-level @Rollback(%s) overrides default rollback [%s] for test method [%s]"
  466 | 						.formatted(rollbackOverride, rollback, testMethod));
  467 | 			}
  468 | 			rollback = rollbackOverride;
  469 | 		}
  470 | 		else {
  471 | 			if (logger.isTraceEnabled()) {
  472 | 				logger.trace("No method-level @Rollback override: using default rollback [%s] for test context %s"
  473 | 						.formatted(rollback, testContext));
  474 | 			}
  475 | 			else if (logger.isDebugEnabled()) {
  476 | 				logger.debug("No method-level @Rollback override: using default rollback [%s] for test method [%s]"
  477 | 						.formatted(rollback, testMethod));
  478 | 			}
  479 | 		}
  480 | 		return rollback;
  481 | 	}
  482 | 
  483 | 	/**
  484 | 	 * Get all methods in the supplied {@link Class class} and its superclasses
  485 | 	 * which are annotated with the supplied {@code annotationType} but
  486 | 	 * which are not <em>shadowed</em> by methods overridden in subclasses.
  487 | 	 * <p>Default methods on interfaces are also detected.
  488 | 	 * @param clazz the class for which to retrieve the annotated methods
  489 | 	 * @param annotationType the annotation type for which to search
  490 | 	 * @return all annotated methods in the supplied class and its superclasses
  491 | 	 * as well as annotated interface default methods
  492 | 	 */
  493 | 	private List<Method> getAnnotatedMethods(Class<?> clazz, Class<? extends Annotation> annotationType) {
  494 | 		MethodFilter methodFilter = ReflectionUtils.USER_DECLARED_METHODS
  495 | 				.and(method -> AnnotatedElementUtils.hasAnnotation(method, annotationType));
  496 | 		return Arrays.asList(ReflectionUtils.getUniqueDeclaredMethods(clazz, methodFilter));
  497 | 	}
  498 | 
  499 | }
```
