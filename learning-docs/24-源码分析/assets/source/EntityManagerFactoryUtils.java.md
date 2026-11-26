<!-- learning-docs source archive（机械抓取原文，禁止手改代码内容） -->
<!-- origin: org.springframework:spring-orm:7.1.0 | path: org/springframework/orm/jpa/EntityManagerFactoryUtils.java -->
<!-- 正文引用：[[源码:EntityManagerFactoryUtils.java#L起-L止]] ；代码块头部：(src: EntityManagerFactoryUtils.java#L起-L止) -->

# EntityManagerFactoryUtils.java

| 项 | 值 |
| --- | --- |
| 来源 | `org.springframework:spring-orm:7.1.0` |
| 路径 | `org/springframework/orm/jpa/EntityManagerFactoryUtils.java` |
| 行数 | 666 |
| 在线地址 | https://repo1.maven.org/maven2/org/springframework/spring-orm/7.1.0/spring-orm-7.1.0-sources.jar |
| 抓取时间 | 2026-11-25T14:00:52.818Z |

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
   17 | package org.springframework.orm.jpa;
   18 | 
   19 | import java.lang.reflect.Method;
   20 | import java.util.Map;
   21 | 
   22 | import jakarta.persistence.EntityExistsException;
   23 | import jakarta.persistence.EntityManager;
   24 | import jakarta.persistence.EntityManagerFactory;
   25 | import jakarta.persistence.EntityNotFoundException;
   26 | import jakarta.persistence.LockTimeoutException;
   27 | import jakarta.persistence.NoResultException;
   28 | import jakarta.persistence.NonUniqueResultException;
   29 | import jakarta.persistence.OptimisticLockException;
   30 | import jakarta.persistence.PersistenceException;
   31 | import jakarta.persistence.PessimisticLockException;
   32 | import jakarta.persistence.Query;
   33 | import jakarta.persistence.QueryTimeoutException;
   34 | import jakarta.persistence.SynchronizationType;
   35 | import jakarta.persistence.TransactionRequiredException;
   36 | import org.apache.commons.logging.Log;
   37 | import org.apache.commons.logging.LogFactory;
   38 | import org.jspecify.annotations.Nullable;
   39 | 
   40 | import org.springframework.beans.factory.BeanFactoryUtils;
   41 | import org.springframework.beans.factory.ListableBeanFactory;
   42 | import org.springframework.beans.factory.NoSuchBeanDefinitionException;
   43 | import org.springframework.core.Ordered;
   44 | import org.springframework.dao.CannotAcquireLockException;
   45 | import org.springframework.dao.DataAccessException;
   46 | import org.springframework.dao.DataAccessResourceFailureException;
   47 | import org.springframework.dao.DataIntegrityViolationException;
   48 | import org.springframework.dao.EmptyResultDataAccessException;
   49 | import org.springframework.dao.IncorrectResultSizeDataAccessException;
   50 | import org.springframework.dao.InvalidDataAccessApiUsageException;
   51 | import org.springframework.dao.PessimisticLockingFailureException;
   52 | import org.springframework.jdbc.datasource.DataSourceUtils;
   53 | import org.springframework.transaction.support.ResourceHolderSynchronization;
   54 | import org.springframework.transaction.support.TransactionSynchronizationManager;
   55 | import org.springframework.util.Assert;
   56 | import org.springframework.util.ClassUtils;
   57 | import org.springframework.util.CollectionUtils;
   58 | import org.springframework.util.ReflectionUtils;
   59 | import org.springframework.util.StringUtils;
   60 | 
   61 | /**
   62 |  * Helper class featuring methods for JPA EntityManager handling,
   63 |  * allowing for reuse of EntityManager instances within transactions.
   64 |  * Also provides support for exception translation.
   65 |  *
   66 |  * <p>Mainly intended for internal use within the framework.
   67 |  *
   68 |  * @author Juergen Hoeller
   69 |  * @since 2.0
   70 |  */
   71 | public abstract class EntityManagerFactoryUtils {
   72 | 
   73 | 	/**
   74 | 	 * Order value for TransactionSynchronization objects that clean up JPA
   75 | 	 * EntityManagers. Return DataSourceUtils.CONNECTION_SYNCHRONIZATION_ORDER - 100
   76 | 	 * to execute EntityManager cleanup before JDBC Connection cleanup, if any.
   77 | 	 * @see org.springframework.jdbc.datasource.DataSourceUtils#CONNECTION_SYNCHRONIZATION_ORDER
   78 | 	 */
   79 | 	public static final int ENTITY_MANAGER_SYNCHRONIZATION_ORDER =
   80 | 			DataSourceUtils.CONNECTION_SYNCHRONIZATION_ORDER - 100;
   81 | 
   82 | 	private static final boolean CREATE_ENTITY_MANAGER_WITHOUT_ARGUMENTS_AVAILABLE =
   83 | 			ClassUtils.hasMethod(EntityManagerFactory.class, "createEntityManager");
   84 | 
   85 | 	private static final @Nullable Method CREATE_ENTITY_AGENT_METHOD =
   86 | 			ClassUtils.getMethodIfAvailable(EntityManagerFactory.class, "createEntityAgent", Map.class);
   87 | 
   88 | 	private static final Log logger = LogFactory.getLog(EntityManagerFactoryUtils.class);
   89 | 
   90 | 
   91 | 	/**
   92 | 	 * Find an EntityManagerFactory with the given name in the given
   93 | 	 * Spring application context (represented as ListableBeanFactory).
   94 | 	 * <p>The specified unit name will be matched against the configured
   95 | 	 * persistence unit, provided that a discovered EntityManagerFactory
   96 | 	 * implements the {@link EntityManagerFactoryInfo} interface. If not,
   97 | 	 * the persistence unit name will be matched against the Spring bean name,
   98 | 	 * assuming that the EntityManagerFactory bean names follow that convention.
   99 | 	 * <p>If no unit name has been given, this method will search for a default
  100 | 	 * EntityManagerFactory through {@link ListableBeanFactory#getBean(Class)}.
  101 | 	 * @param beanFactory the ListableBeanFactory to search
  102 | 	 * @param unitName the name of the persistence unit (may be {@code null} or empty,
  103 | 	 * in which case a single bean of type EntityManagerFactory will be searched for)
  104 | 	 * @return the EntityManagerFactory
  105 | 	 * @throws NoSuchBeanDefinitionException if there is no such EntityManagerFactory in the context
  106 | 	 * @see EntityManagerFactoryInfo#getPersistenceUnitName()
  107 | 	 */
  108 | 	public static EntityManagerFactory findEntityManagerFactory(
  109 | 			ListableBeanFactory beanFactory, @Nullable String unitName) throws NoSuchBeanDefinitionException {
  110 | 
  111 | 		Assert.notNull(beanFactory, "ListableBeanFactory must not be null");
  112 | 		if (StringUtils.hasLength(unitName)) {
  113 | 			// See whether we can find an EntityManagerFactory with matching persistence unit name.
  114 | 			String[] candidateNames =
  115 | 					BeanFactoryUtils.beanNamesForTypeIncludingAncestors(beanFactory, EntityManagerFactory.class);
  116 | 			for (String candidateName : candidateNames) {
  117 | 				EntityManagerFactory emf = (EntityManagerFactory) beanFactory.getBean(candidateName);
  118 | 				if (emf instanceof EntityManagerFactoryInfo emfInfo &&
  119 | 						unitName.equals(emfInfo.getPersistenceUnitName())) {
  120 | 					return emf;
  121 | 				}
  122 | 			}
  123 | 			// No matching persistence unit found - simply take the EntityManagerFactory
  124 | 			// with the persistence unit name as bean name (by convention).
  125 | 			return beanFactory.getBean(unitName, EntityManagerFactory.class);
  126 | 		}
  127 | 		else {
  128 | 			// Find unique EntityManagerFactory bean in the context, falling back to parent contexts.
  129 | 			return beanFactory.getBean(EntityManagerFactory.class);
  130 | 		}
  131 | 	}
  132 | 
  133 | 	/**
  134 | 	 * Obtain a JPA EntityManager from the given factory. Is aware of a corresponding
  135 | 	 * EntityManager bound to the current thread, for example, when using JpaTransactionManager.
  136 | 	 * <p>Note: Will return {@code null} if no thread-bound EntityManager found!
  137 | 	 * @param emf the EntityManagerFactory to create the EntityManager with
  138 | 	 * @return the EntityManager, or {@code null} if none found
  139 | 	 * @throws DataAccessResourceFailureException if the EntityManager couldn't be obtained
  140 | 	 * @see JpaTransactionManager
  141 | 	 */
  142 | 	public static @Nullable EntityManager getTransactionalEntityManager(EntityManagerFactory emf)
  143 | 			throws DataAccessResourceFailureException {
  144 | 
  145 | 		return getTransactionalEntityManager(emf, null);
  146 | 	}
  147 | 
  148 | 	/**
  149 | 	 * Obtain a JPA EntityManager from the given factory. Is aware of a corresponding
  150 | 	 * EntityManager bound to the current thread, for example, when using JpaTransactionManager.
  151 | 	 * <p>Note: Will return {@code null} if no thread-bound EntityManager found!
  152 | 	 * @param emf the EntityManagerFactory to create the EntityManager with
  153 | 	 * @param properties the properties to be passed into the {@code createEntityManager}
  154 | 	 * call (may be {@code null})
  155 | 	 * @return the EntityManager, or {@code null} if none found
  156 | 	 * @throws DataAccessResourceFailureException if the EntityManager couldn't be obtained
  157 | 	 * @see JpaTransactionManager
  158 | 	 */
  159 | 	public static @Nullable EntityManager getTransactionalEntityManager(EntityManagerFactory emf, @Nullable Map<?, ?> properties)
  160 | 			throws DataAccessResourceFailureException {
  161 | 		try {
  162 | 			return doGetTransactionalEntityManager(emf, properties, true);
  163 | 		}
  164 | 		catch (PersistenceException ex) {
  165 | 			throw new DataAccessResourceFailureException("Could not obtain JPA EntityManager", ex);
  166 | 		}
  167 | 	}
  168 | 
  169 | 	/**
  170 | 	 * Obtain a JPA EntityManager from the given factory. Is aware of a corresponding
  171 | 	 * EntityManager bound to the current thread, for example, when using JpaTransactionManager.
  172 | 	 * <p>Same as {@code getEntityManager}, but throwing the original PersistenceException.
  173 | 	 * @param emf the EntityManagerFactory to create the EntityManager with
  174 | 	 * @param properties the properties to be passed into the {@code createEntityManager}
  175 | 	 * call (may be {@code null})
  176 | 	 * @return the EntityManager, or {@code null} if none found
  177 | 	 * @throws jakarta.persistence.PersistenceException if the EntityManager couldn't be created
  178 | 	 * @see #getTransactionalEntityManager(jakarta.persistence.EntityManagerFactory)
  179 | 	 * @see JpaTransactionManager
  180 | 	 */
  181 | 	public static @Nullable EntityManager doGetTransactionalEntityManager(EntityManagerFactory emf, Map<?, ?> properties)
  182 | 			throws PersistenceException {
  183 | 
  184 | 		return doGetTransactionalEntityManager(emf, properties, true);
  185 | 	}
  186 | 
  187 | 	/**
  188 | 	 * Obtain a JPA EntityManager from the given factory. Is aware of a corresponding
  189 | 	 * EntityManager bound to the current thread, for example, when using JpaTransactionManager.
  190 | 	 * <p>Same as {@code getEntityManager}, but throwing the original PersistenceException.
  191 | 	 * @param emf the EntityManagerFactory to create the EntityManager with
  192 | 	 * @param properties the properties to be passed into the {@code createEntityManager}
  193 | 	 * call (may be {@code null})
  194 | 	 * @param synchronizedWithTransaction whether to automatically join ongoing
  195 | 	 * transactions (according to the JPA 2.1 SynchronizationType rules)
  196 | 	 * @return the EntityManager, or {@code null} if none found
  197 | 	 * @throws jakarta.persistence.PersistenceException if the EntityManager couldn't be created
  198 | 	 * @see #getTransactionalEntityManager(jakarta.persistence.EntityManagerFactory)
  199 | 	 * @see JpaTransactionManager
  200 | 	 */
  201 | 	public static @Nullable EntityManager doGetTransactionalEntityManager(
  202 | 			EntityManagerFactory emf, @Nullable Map<?, ?> properties, boolean synchronizedWithTransaction)
  203 | 			throws PersistenceException {
  204 | 
  205 | 		Assert.notNull(emf, "No EntityManagerFactory specified");
  206 | 
  207 | 		EntityManagerHolder emHolder = (EntityManagerHolder) TransactionSynchronizationManager.getResource(emf);
  208 | 		if (emHolder != null && emHolder.hasEntityManager()) {
  209 | 			if (synchronizedWithTransaction) {
  210 | 				if (!emHolder.isSynchronizedWithTransaction()) {
  211 | 					if (TransactionSynchronizationManager.isActualTransactionActive()) {
  212 | 						// Try to explicitly synchronize the EntityManager itself
  213 | 						// with an ongoing JTA transaction, if any.
  214 | 						try {
  215 | 							emHolder.getEntityManager().joinTransaction();
  216 | 						}
  217 | 						catch (TransactionRequiredException ex) {
  218 | 							logger.debug("Could not join transaction because none was actually active", ex);
  219 | 						}
  220 | 					}
  221 | 					if (TransactionSynchronizationManager.isSynchronizationActive()) {
  222 | 						Object transactionData = prepareTransaction(emHolder.getEntityManager(), emf);
  223 | 						TransactionSynchronizationManager.registerSynchronization(
  224 | 								new TransactionalEntityManagerSynchronization(emHolder, emf, transactionData, false));
  225 | 						emHolder.setSynchronizedWithTransaction(true);
  226 | 					}
  227 | 				}
  228 | 				// Use holder's reference count to track synchronizedWithTransaction access.
  229 | 				// isOpen() check used below to find out about it.
  230 | 				emHolder.requested();
  231 | 				return emHolder.getEntityManager();
  232 | 			}
  233 | 			else {
  234 | 				// unsynchronized EntityManager demanded
  235 | 				if (emHolder.isTransactionActive() && !emHolder.isOpen()) {
  236 | 					if (!TransactionSynchronizationManager.isSynchronizationActive()) {
  237 | 						return null;
  238 | 					}
  239 | 					// EntityManagerHolder with an active transaction coming from JpaTransactionManager,
  240 | 					// with no synchronized EntityManager having been requested by application code before.
  241 | 					// Unbind in order to register a new unsynchronized EntityManager instead.
  242 | 					TransactionSynchronizationManager.unbindResource(emf);
  243 | 					emHolder = null;
  244 | 				}
  245 | 				else {
  246 | 					// Either a previously bound unsynchronized EntityManager, or the application
  247 | 					// has requested a synchronized EntityManager before and therefore upgraded
  248 | 					// this transaction's EntityManager to synchronized before.
  249 | 					return emHolder.getEntityManager();
  250 | 				}
  251 | 			}
  252 | 		}
  253 | 		else if (!TransactionSynchronizationManager.isSynchronizationActive()) {
  254 | 			// Indicate that we can't obtain a transactional EntityManager.
  255 | 			return null;
  256 | 		}
  257 | 
  258 | 		// Create a new EntityManager for use within the current transaction.
  259 | 		logger.debug("Opening JPA EntityManager");
  260 | 		EntityManager em = null;
  261 | 		if (!synchronizedWithTransaction) {
  262 | 			try {
  263 | 				em = emf.createEntityManager(SynchronizationType.UNSYNCHRONIZED, properties);
  264 | 			}
  265 | 			catch (AbstractMethodError err) {
  266 | 				// JPA 2.1 API available but method not actually implemented in persistence provider:
  267 | 				// falling back to regular createEntityManager method.
  268 | 			}
  269 | 		}
  270 | 		if (em == null) {
  271 | 			em = createEntityManager(emf, properties);
  272 | 		}
  273 | 
  274 | 		try {
  275 | 			if (emHolder != null) {
  276 | 				emHolder.setEntityManager(em);
  277 | 				Object transactionData = prepareTransaction(em, emf);
  278 | 				TransactionSynchronizationManager.registerSynchronization(
  279 | 						new TransactionalEntityManagerSynchronization(emHolder, emf, transactionData, false));
  280 | 				emHolder.setSynchronizedWithTransaction(true);
  281 | 			}
  282 | 			else {
  283 | 				// Use same EntityManager for further JPA operations within the transaction.
  284 | 				// Thread-bound object will get removed by synchronization at transaction completion.
  285 | 				emHolder = new EntityManagerHolder(em);
  286 | 				if (synchronizedWithTransaction) {
  287 | 					Object transactionData = prepareTransaction(em, emf);
  288 | 					TransactionSynchronizationManager.registerSynchronization(
  289 | 							new TransactionalEntityManagerSynchronization(emHolder, emf, transactionData, true));
  290 | 					emHolder.setSynchronizedWithTransaction(true);
  291 | 				}
  292 | 				else {
  293 | 					// Unsynchronized - just scope it for the transaction, as demanded by the JPA 2.1 spec...
  294 | 					TransactionSynchronizationManager.registerSynchronization(
  295 | 							new TransactionScopedEntityManagerSynchronization(emHolder, emf));
  296 | 				}
  297 | 				TransactionSynchronizationManager.bindResource(emf, emHolder);
  298 | 			}
  299 | 		}
  300 | 		catch (RuntimeException ex) {
  301 | 			// Unexpected exception from external delegation call -> close EntityManager and rethrow.
  302 | 			closeEntityManager(em);
  303 | 			throw ex;
  304 | 		}
  305 | 
  306 | 		return em;
  307 | 	}
  308 | 
  309 | 	/**
  310 | 	 * Obtain a JPA EntityAgent from the given factory. Binds a corresponding
  311 | 	 * EntityAgent to the thread when running in a Spring-managed transaction.
  312 | 	 * @param emf the EntityManagerFactory to create the EntityAgent with
  313 | 	 * @param properties the properties to be passed into the {@code createEntityAgent}
  314 | 	 * call (may be {@code null})
  315 | 	 * @return the EntityAgent, or {@code null} if none found
  316 | 	 * @throws PersistenceException if the EntityManager couldn't be created
  317 | 	 * @since 7.0.4
  318 | 	 */
  319 | 	static @Nullable Object doGetTransactionalEntityAgent(EntityManagerFactory emf, @Nullable Map<?, ?> properties)
  320 | 			throws PersistenceException {
  321 | 
  322 | 		Assert.notNull(emf, "No EntityManagerFactory specified");
  323 | 
  324 | 		EntityManagerHolder emHolder = (EntityManagerHolder) TransactionSynchronizationManager.getResource(emf);
  325 | 		if (emHolder != null) {
  326 | 			if (emHolder.hasEntityAgent()) {
  327 | 				return emHolder.getEntityAgent();
  328 | 			}
  329 | 		}
  330 | 		else if (!TransactionSynchronizationManager.isSynchronizationActive()) {
  331 | 			// Indicate that we can't obtain a transactional EntityAgent.
  332 | 			return null;
  333 | 		}
  334 | 
  335 | 		// Create a new EntityAgent for use within the current transaction.
  336 | 		logger.debug("Opening JPA EntityAgent");
  337 | 		Object entityAgent = null;
  338 | 		if (emHolder != null && emf instanceof EntityManagerFactoryInfo info &&
  339 | 				info.getJpaDialect() instanceof DefaultJpaDialect defaultJpaDialect) {
  340 | 			// For JpaTransactionManager: share transaction context with primary EntityManager.
  341 | 			entityAgent = defaultJpaDialect.deriveEntityAgent(emHolder.getEntityManager(), properties);
  342 | 		}
  343 | 		if (entityAgent == null) {
  344 | 			entityAgent = createEntityAgent(emf, properties);
  345 | 		}
  346 | 
  347 | 		if (emHolder != null) {
  348 | 			emHolder.setEntityAgent(entityAgent);
  349 | 		}
  350 | 		else {
  351 | 			emHolder = new EntityManagerHolder(entityAgent);
  352 | 			TransactionSynchronizationManager.registerSynchronization(
  353 | 					new TransactionScopedEntityManagerSynchronization(emHolder, emf));
  354 | 			TransactionSynchronizationManager.bindResource(emf, emHolder);
  355 | 		}
  356 | 		emHolder.setSynchronizedWithTransaction(true);
  357 | 
  358 | 		return entityAgent;
  359 | 	}
  360 | 
  361 | 	/**
  362 | 	 * Create a new JPA EntityManager via reflectively detected JPA 3.2/4.0 API.
  363 | 	 * @param emf the EntityManagerFactory to create the EntityManager with
  364 | 	 * @param properties the properties to be passed into the {@code createEntityManager}
  365 | 	 * call (may be {@code null})
  366 | 	 * @since 7.0.8
  367 | 	 */
  368 | 	public static EntityManager createEntityManager(EntityManagerFactory emf, @Nullable Map<?, ?> properties) {
  369 | 		if (CollectionUtils.isEmpty(properties)) {
  370 | 			properties = null;
  371 | 		}
  372 | 		if (properties == null && CREATE_ENTITY_MANAGER_WITHOUT_ARGUMENTS_AVAILABLE) {
  373 | 			return emf.createEntityManager();  // on JPA 3.2
  374 | 		}
  375 | 		return emf.createEntityManager(properties);  // on JPA 4.0 even for empty properties
  376 | 	}
  377 | 
  378 | 	/**
  379 | 	 * Create a new JPA EntityAgent via reflectively detected JPA 4.0 API.
  380 | 	 * @param emf the EntityManagerFactory to create the EntityAgent with
  381 | 	 * @param properties the properties to be passed into the {@code createEntityAgent}
  382 | 	 * call (may be {@code null})
  383 | 	 * @since 7.0.4
  384 | 	 */
  385 | 	static Object createEntityAgent(EntityManagerFactory emf, @Nullable Map<?, ?> properties) {
  386 | 		if (CREATE_ENTITY_AGENT_METHOD == null) {
  387 | 			throw new IllegalStateException("JPA 4.0 createEntityAgent API not available");
  388 | 		}
  389 | 		if (CollectionUtils.isEmpty(properties)) {
  390 | 			properties = null;
  391 | 		}
  392 | 		Object entityAgent = ReflectionUtils.invokeMethod(CREATE_ENTITY_AGENT_METHOD, emf, properties);
  393 | 		if (entityAgent == null) {
  394 | 			throw new IllegalStateException("JPA 4.0 createEntityAgent API returned null");
  395 | 		}
  396 | 		return entityAgent;
  397 | 	}
  398 | 
  399 | 	/**
  400 | 	 * Determine the {@code jakarta.persistence.EntityAgent} class.
  401 | 	 * <p>Intended for internal adaptation to JPA 3.2 vs 4.0.
  402 | 	 * @return the {@code EntityAgent} class (on JPA 4.0),
  403 | 	 * or {@code null} if not available (on JPA 3.2)
  404 | 	 * @since 7.1
  405 | 	 */
  406 | 	public static @Nullable Class<?> getEntityAgentClass() {
  407 | 		return (CREATE_ENTITY_AGENT_METHOD != null ? CREATE_ENTITY_AGENT_METHOD.getReturnType() : null);
  408 | 	}
  409 | 
  410 | 	/**
  411 | 	 * Prepare a transaction on the given EntityManager, if possible.
  412 | 	 * @param em the EntityManager to prepare
  413 | 	 * @param emf the EntityManagerFactory that the EntityManager has been created with
  414 | 	 * @return an arbitrary object that holds transaction data, if any
  415 | 	 * (to be passed into cleanupTransaction)
  416 | 	 * @see JpaDialect#prepareTransaction
  417 | 	 */
  418 | 	private static @Nullable Object prepareTransaction(EntityManager em, EntityManagerFactory emf) {
  419 | 		if (emf instanceof EntityManagerFactoryInfo emfInfo) {
  420 | 			JpaDialect jpaDialect = emfInfo.getJpaDialect();
  421 | 			if (jpaDialect != null) {
  422 | 				return jpaDialect.prepareTransaction(em,
  423 | 						TransactionSynchronizationManager.isCurrentTransactionReadOnly(),
  424 | 						TransactionSynchronizationManager.getCurrentTransactionName());
  425 | 			}
  426 | 		}
  427 | 		return null;
  428 | 	}
  429 | 
  430 | 	/**
  431 | 	 * Prepare a transaction on the given EntityManager, if possible.
  432 | 	 * @param transactionData arbitrary object that holds transaction data, if any
  433 | 	 * (as returned by prepareTransaction)
  434 | 	 * @param emf the EntityManagerFactory that the EntityManager has been created with
  435 | 	 * @see JpaDialect#cleanupTransaction
  436 | 	 */
  437 | 	private static void cleanupTransaction(@Nullable Object transactionData, EntityManagerFactory emf) {
  438 | 		if (emf instanceof EntityManagerFactoryInfo emfInfo) {
  439 | 			JpaDialect jpaDialect = emfInfo.getJpaDialect();
  440 | 			if (jpaDialect != null) {
  441 | 				jpaDialect.cleanupTransaction(transactionData);
  442 | 			}
  443 | 		}
  444 | 	}
  445 | 
  446 | 	/**
  447 | 	 * Apply the current transaction timeout, if any, to the given JPA Query object.
  448 | 	 * <p>This method sets the JPA query hint "jakarta.persistence.query.timeout" accordingly.
  449 | 	 * @param query the JPA Query object
  450 | 	 * @param emf the JPA EntityManagerFactory that the Query was created for
  451 | 	 */
  452 | 	public static void applyTransactionTimeout(Query query, EntityManagerFactory emf) {
  453 | 		EntityManagerHolder emHolder = (EntityManagerHolder) TransactionSynchronizationManager.getResource(emf);
  454 | 		if (emHolder != null && emHolder.hasTimeout()) {
  455 | 			int timeoutValue = (int) emHolder.getTimeToLiveInMillis();
  456 | 			try {
  457 | 				query.setHint("jakarta.persistence.query.timeout", timeoutValue);
  458 | 			}
  459 | 			catch (IllegalArgumentException ex) {
  460 | 				// oh well, at least we tried...
  461 | 			}
  462 | 		}
  463 | 	}
  464 | 
  465 | 	/**
  466 | 	 * Convert the given runtime exception to an appropriate exception from the
  467 | 	 * {@code org.springframework.dao} hierarchy.
  468 | 	 * Return null if no translation is appropriate: any other exception may
  469 | 	 * have resulted from user code, and should not be translated.
  470 | 	 * <p>The most important cases like object not found or optimistic locking failure
  471 | 	 * are covered here. For more fine-granular conversion, JpaTransactionManager etc
  472 | 	 * support sophisticated translation of exceptions via a JpaDialect.
  473 | 	 * @param ex runtime exception that occurred
  474 | 	 * @return the corresponding DataAccessException instance,
  475 | 	 * or {@code null} if the exception should not be translated
  476 | 	 */
  477 | 	public static @Nullable DataAccessException convertJpaAccessExceptionIfPossible(RuntimeException ex) {
  478 | 		// Following the JPA specification, a persistence provider can also
  479 | 		// throw these two exceptions, besides PersistenceException.
  480 | 		if (ex instanceof IllegalStateException) {
  481 | 			return new InvalidDataAccessApiUsageException(ex.getMessage(), ex);
  482 | 		}
  483 | 		if (ex instanceof IllegalArgumentException) {
  484 | 			return new InvalidDataAccessApiUsageException(ex.getMessage(), ex);
  485 | 		}
  486 | 
  487 | 		// Check for well-known PersistenceException subclasses.
  488 | 		if (ex instanceof EntityNotFoundException entityNotFoundException) {
  489 | 			return new JpaObjectRetrievalFailureException(entityNotFoundException);
  490 | 		}
  491 | 		if (ex instanceof NoResultException) {
  492 | 			return new EmptyResultDataAccessException(ex.getMessage(), 1, ex);
  493 | 		}
  494 | 		if (ex instanceof NonUniqueResultException) {
  495 | 			return new IncorrectResultSizeDataAccessException(ex.getMessage(), 1, ex);
  496 | 		}
  497 | 		if (ex instanceof QueryTimeoutException) {
  498 | 			return new org.springframework.dao.QueryTimeoutException(ex.getMessage(), ex);
  499 | 		}
  500 | 		if (ex instanceof LockTimeoutException) {
  501 | 			return new CannotAcquireLockException(ex.getMessage(), ex);
  502 | 		}
  503 | 		if (ex instanceof PessimisticLockException) {
  504 | 			return new PessimisticLockingFailureException(ex.getMessage(), ex);
  505 | 		}
  506 | 		if (ex instanceof OptimisticLockException optimisticLockException) {
  507 | 			return new JpaOptimisticLockingFailureException(optimisticLockException);
  508 | 		}
  509 | 		if (ex instanceof EntityExistsException) {
  510 | 			return new DataIntegrityViolationException(ex.getMessage(), ex);
  511 | 		}
  512 | 		if (ex instanceof TransactionRequiredException) {
  513 | 			return new InvalidDataAccessApiUsageException(ex.getMessage(), ex);
  514 | 		}
  515 | 
  516 | 		// If we have another kind of PersistenceException, throw it.
  517 | 		if (ex instanceof PersistenceException) {
  518 | 			return new JpaSystemException(ex);
  519 | 		}
  520 | 
  521 | 		// If we get here, we have an exception that resulted from user code,
  522 | 		// rather than the persistence provider, so we return null to indicate
  523 | 		// that translation should not occur.
  524 | 		return null;
  525 | 	}
  526 | 
  527 | 	/**
  528 | 	 * Close the given JPA EntityManager,
  529 | 	 * catching and logging any cleanup exceptions thrown.
  530 | 	 * @param em the JPA EntityManager to close (may be {@code null})
  531 | 	 * @see jakarta.persistence.EntityManager#close()
  532 | 	 */
  533 | 	public static void closeEntityManager(@Nullable EntityManager em) {
  534 | 		if (em != null) {
  535 | 			try {
  536 | 				if (em.isOpen()) {
  537 | 					em.close();
  538 | 				}
  539 | 			}
  540 | 			catch (Throwable ex) {
  541 | 				logger.error("Failed to release JPA EntityManager", ex);
  542 | 			}
  543 | 		}
  544 | 	}
  545 | 
  546 | 	/**
  547 | 	 * Close the given JPA EntityHandler (EntityManager or EntityAgent),
  548 | 	 * catching and logging any cleanup exceptions thrown.
  549 | 	 * @param entityHandler the JPA EntityManager/EntityAgent to close
  550 | 	 * @since 7.0.4
  551 | 	 */
  552 | 	static void closeEntityHandler(@Nullable Object entityHandler) {
  553 | 		if (entityHandler instanceof AutoCloseable closeable) {
  554 | 			try {
  555 | 				closeable.close();
  556 | 			}
  557 | 			catch (Throwable ex) {
  558 | 				logger.error("Failed to close JPA EntityHandler", ex);
  559 | 			}
  560 | 		}
  561 | 	}
  562 | 
  563 | 
  564 | 	/**
  565 | 	 * Callback for resource cleanup at the end of a non-JPA transaction
  566 | 	 * (for example, when participating in a JtaTransactionManager transaction),
  567 | 	 * fully synchronized with the ongoing transaction.
  568 | 	 * @see org.springframework.transaction.jta.JtaTransactionManager
  569 | 	 */
  570 | 	private static class TransactionalEntityManagerSynchronization
  571 | 			extends ResourceHolderSynchronization<EntityManagerHolder, EntityManagerFactory>
  572 | 			implements Ordered {
  573 | 
  574 | 		private final @Nullable Object transactionData;
  575 | 
  576 | 		private final @Nullable JpaDialect jpaDialect;
  577 | 
  578 | 		private final boolean newEntityManager;
  579 | 
  580 | 		public TransactionalEntityManagerSynchronization(
  581 | 				EntityManagerHolder emHolder, EntityManagerFactory emf, @Nullable Object txData, boolean newEm) {
  582 | 
  583 | 			super(emHolder, emf);
  584 | 			this.transactionData = txData;
  585 | 			this.jpaDialect = (emf instanceof EntityManagerFactoryInfo emfInfo ? emfInfo.getJpaDialect() : null);
  586 | 			this.newEntityManager = newEm;
  587 | 		}
  588 | 
  589 | 		@Override
  590 | 		public int getOrder() {
  591 | 			return ENTITY_MANAGER_SYNCHRONIZATION_ORDER;
  592 | 		}
  593 | 
  594 | 		@Override
  595 | 		protected void flushResource(EntityManagerHolder resourceHolder) {
  596 | 			EntityManager em = resourceHolder.getEntityManager();
  597 | 			if (em instanceof EntityManagerProxy emProxy) {
  598 | 				EntityManager target = emProxy.getTargetEntityManager();
  599 | 				if (TransactionSynchronizationManager.hasResource(target)) {
  600 | 					// ExtendedEntityManagerSynchronization active after joinTransaction() call:
  601 | 					// flush synchronization already registered.
  602 | 					return;
  603 | 				}
  604 | 			}
  605 | 			try {
  606 | 				em.flush();
  607 | 			}
  608 | 			catch (RuntimeException ex) {
  609 | 				DataAccessException dae;
  610 | 				if (this.jpaDialect != null) {
  611 | 					dae = this.jpaDialect.translateExceptionIfPossible(ex);
  612 | 				}
  613 | 				else {
  614 | 					dae = convertJpaAccessExceptionIfPossible(ex);
  615 | 				}
  616 | 				throw (dae != null ? dae : ex);
  617 | 			}
  618 | 		}
  619 | 
  620 | 		@Override
  621 | 		protected boolean shouldUnbindAtCompletion() {
  622 | 			return this.newEntityManager;
  623 | 		}
  624 | 
  625 | 		@Override
  626 | 		protected void releaseResource(EntityManagerHolder resourceHolder, EntityManagerFactory resourceKey) {
  627 | 			resourceHolder.closeAll();
  628 | 		}
  629 | 
  630 | 		@Override
  631 | 		protected void cleanupResource(
  632 | 				EntityManagerHolder resourceHolder, EntityManagerFactory resourceKey, boolean committed) {
  633 | 
  634 | 			if (!committed) {
  635 | 				// Clear all pending inserts/updates/deletes in the EntityManager.
  636 | 				// Necessary for pre-bound EntityManagers, to avoid inconsistent state.
  637 | 				resourceHolder.getEntityManager().clear();
  638 | 			}
  639 | 			cleanupTransaction(this.transactionData, resourceKey);
  640 | 		}
  641 | 	}
  642 | 
  643 | 
  644 | 	/**
  645 | 	 * Minimal callback that just closes the EntityManager at the end of the transaction.
  646 | 	 */
  647 | 	private static class TransactionScopedEntityManagerSynchronization
  648 | 			extends ResourceHolderSynchronization<EntityManagerHolder, EntityManagerFactory>
  649 | 			implements Ordered {
  650 | 
  651 | 		public TransactionScopedEntityManagerSynchronization(EntityManagerHolder emHolder, EntityManagerFactory emf) {
  652 | 			super(emHolder, emf);
  653 | 		}
  654 | 
  655 | 		@Override
  656 | 		public int getOrder() {
  657 | 			return ENTITY_MANAGER_SYNCHRONIZATION_ORDER + 1;
  658 | 		}
  659 | 
  660 | 		@Override
  661 | 		protected void releaseResource(EntityManagerHolder resourceHolder, EntityManagerFactory resourceKey) {
  662 | 			resourceHolder.closeAll();
  663 | 		}
  664 | 	}
  665 | 
  666 | }
```
