<!-- learning-docs source archive（机械抓取原文，禁止手改代码内容） -->
<!-- origin: org.springframework:spring-orm:7.1.0 | path: org/springframework/orm/jpa/JpaTransactionManager.java -->
<!-- 正文引用：[[源码:JpaTransactionManager.java#L起-L止]] ；代码块头部：(src: JpaTransactionManager.java#L起-L止) -->

# JpaTransactionManager.java

| 项 | 值 |
| --- | --- |
| 来源 | `org.springframework:spring-orm:7.1.0` |
| 路径 | `org/springframework/orm/jpa/JpaTransactionManager.java` |
| 行数 | 811 |
| 在线地址 | https://repo1.maven.org/maven2/org/springframework/spring-orm/7.1.0/spring-orm-7.1.0-sources.jar |
| 抓取时间 | 2026-11-25T14:00:52.692Z |

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
   19 | import java.util.HashMap;
   20 | import java.util.Map;
   21 | import java.util.Properties;
   22 | import java.util.function.Consumer;
   23 | 
   24 | import javax.sql.DataSource;
   25 | 
   26 | import jakarta.persistence.EntityManager;
   27 | import jakarta.persistence.EntityManagerFactory;
   28 | import jakarta.persistence.EntityTransaction;
   29 | import jakarta.persistence.PersistenceException;
   30 | import jakarta.persistence.RollbackException;
   31 | import org.jspecify.annotations.Nullable;
   32 | 
   33 | import org.springframework.beans.BeansException;
   34 | import org.springframework.beans.factory.BeanFactory;
   35 | import org.springframework.beans.factory.BeanFactoryAware;
   36 | import org.springframework.beans.factory.InitializingBean;
   37 | import org.springframework.beans.factory.ListableBeanFactory;
   38 | import org.springframework.dao.DataAccessException;
   39 | import org.springframework.dao.support.DataAccessUtils;
   40 | import org.springframework.jdbc.datasource.ConnectionHandle;
   41 | import org.springframework.jdbc.datasource.ConnectionHolder;
   42 | import org.springframework.jdbc.datasource.JdbcTransactionObjectSupport;
   43 | import org.springframework.jdbc.datasource.TransactionAwareDataSourceProxy;
   44 | import org.springframework.transaction.CannotCreateTransactionException;
   45 | import org.springframework.transaction.IllegalTransactionStateException;
   46 | import org.springframework.transaction.NestedTransactionNotSupportedException;
   47 | import org.springframework.transaction.SavepointManager;
   48 | import org.springframework.transaction.TransactionDefinition;
   49 | import org.springframework.transaction.TransactionException;
   50 | import org.springframework.transaction.TransactionSystemException;
   51 | import org.springframework.transaction.support.AbstractPlatformTransactionManager;
   52 | import org.springframework.transaction.support.DefaultTransactionStatus;
   53 | import org.springframework.transaction.support.DelegatingTransactionDefinition;
   54 | import org.springframework.transaction.support.ResourceTransactionDefinition;
   55 | import org.springframework.transaction.support.ResourceTransactionManager;
   56 | import org.springframework.transaction.support.TransactionSynchronizationManager;
   57 | import org.springframework.util.Assert;
   58 | import org.springframework.util.CollectionUtils;
   59 | 
   60 | /**
   61 |  * {@link org.springframework.transaction.PlatformTransactionManager} implementation
   62 |  * for a single JPA {@link jakarta.persistence.EntityManagerFactory}. Binds a JPA
   63 |  * EntityManager from the specified factory to the thread, potentially allowing for
   64 |  * one thread-bound EntityManager per factory. {@link SharedEntityManagerCreator} and
   65 |  * {@code @PersistenceContext} are aware of thread-bound entity managers and participate
   66 |  * in such transactions automatically. Using either is required for JPA access code
   67 |  * supporting this transaction management mechanism.
   68 |  *
   69 |  * <p>This transaction manager is appropriate for applications that use a single
   70 |  * JPA EntityManagerFactory for transactional data access. JTA (usually through
   71 |  * {@link org.springframework.transaction.jta.JtaTransactionManager}) is necessary
   72 |  * for accessing multiple transactional resources within the same transaction.
   73 |  * Note that you need to configure your JPA provider accordingly in order to make
   74 |  * it participate in JTA transactions.
   75 |  *
   76 |  * <p>This transaction manager also supports direct DataSource access within a
   77 |  * transaction (i.e. plain JDBC code working with the same DataSource).
   78 |  * This allows for mixing services which access JPA and services which use plain
   79 |  * JDBC (without being aware of JPA)! Application code needs to stick to the
   80 |  * same simple Connection lookup pattern as with
   81 |  * {@link org.springframework.jdbc.datasource.DataSourceTransactionManager}
   82 |  * (i.e. {@link org.springframework.jdbc.datasource.DataSourceUtils#getConnection}
   83 |  * or going through a
   84 |  * {@link org.springframework.jdbc.datasource.TransactionAwareDataSourceProxy}).
   85 |  * Note that this requires a vendor-specific {@link JpaDialect} to be configured.
   86 |  *
   87 |  * <p>Note: To be able to register a DataSource's Connection for plain JDBC code,
   88 |  * this instance needs to be aware of the DataSource ({@link #setDataSource}).
   89 |  * The given DataSource should obviously match the one used by the given
   90 |  * EntityManagerFactory. This transaction manager will autodetect the DataSource
   91 |  * used as the connection factory of the EntityManagerFactory, so you usually
   92 |  * don't need to explicitly specify the "dataSource" property.
   93 |  *
   94 |  * <p>This transaction manager supports nested transactions via JDBC Savepoints.
   95 |  * The {@link #setNestedTransactionAllowed "nestedTransactionAllowed"} flag defaults
   96 |  * to "false", though, as nested transactions will just apply to the JDBC Connection,
   97 |  * not to the JPA EntityManager and its cached entity objects and related context.
   98 |  * You can manually set the flag to "true" if you want to use nested transactions
   99 |  * for JDBC access code which participates in JPA transactions (provided that your
  100 |  * JDBC driver supports savepoints). <i>Note that JPA itself does not support
  101 |  * nested transactions! Hence, do not expect JPA access code to semantically
  102 |  * participate in a nested transaction.</i>
  103 |  *
  104 |  * @author Juergen Hoeller
  105 |  * @since 2.0
  106 |  * @see #setEntityManagerFactory
  107 |  * @see #setDataSource
  108 |  * @see LocalEntityManagerFactoryBean
  109 |  * @see org.springframework.orm.jpa.support.SharedEntityManagerBean
  110 |  * @see org.springframework.jdbc.datasource.DataSourceUtils#getConnection
  111 |  * @see org.springframework.jdbc.datasource.DataSourceUtils#releaseConnection
  112 |  * @see org.springframework.jdbc.core.JdbcTemplate
  113 |  * @see org.springframework.jdbc.support.JdbcTransactionManager
  114 |  * @see org.springframework.transaction.jta.JtaTransactionManager
  115 |  */
  116 | @SuppressWarnings("serial")
  117 | public class JpaTransactionManager extends AbstractPlatformTransactionManager
  118 | 		implements ResourceTransactionManager, BeanFactoryAware, InitializingBean {
  119 | 
  120 | 	private @Nullable EntityManagerFactory entityManagerFactory;
  121 | 
  122 | 	private @Nullable String persistenceUnitName;
  123 | 
  124 | 	private final Map<String, Object> jpaPropertyMap = new HashMap<>();
  125 | 
  126 | 	private @Nullable DataSource dataSource;
  127 | 
  128 | 	private JpaDialect jpaDialect = new DefaultJpaDialect();
  129 | 
  130 | 	private @Nullable Consumer<EntityManager> entityManagerInitializer;
  131 | 
  132 | 
  133 | 	/**
  134 | 	 * Create a new JpaTransactionManager instance.
  135 | 	 * <p>An EntityManagerFactory has to be set to be able to use it.
  136 | 	 * @see #setEntityManagerFactory
  137 | 	 */
  138 | 	public JpaTransactionManager() {
  139 | 	}
  140 | 
  141 | 	/**
  142 | 	 * Create a new JpaTransactionManager instance.
  143 | 	 * @param emf the EntityManagerFactory to manage transactions for
  144 | 	 */
  145 | 	public JpaTransactionManager(EntityManagerFactory emf) {
  146 | 		this.entityManagerFactory = emf;
  147 | 		afterPropertiesSet();
  148 | 	}
  149 | 
  150 | 
  151 | 	/**
  152 | 	 * Set the EntityManagerFactory that this instance should manage transactions for.
  153 | 	 * <p>Alternatively, specify the persistence unit name of the target EntityManagerFactory.
  154 | 	 * By default, a default EntityManagerFactory will be retrieved by finding a
  155 | 	 * single unique bean of type EntityManagerFactory in the containing BeanFactory.
  156 | 	 * @see #setPersistenceUnitName
  157 | 	 */
  158 | 	public void setEntityManagerFactory(@Nullable EntityManagerFactory emf) {
  159 | 		this.entityManagerFactory = emf;
  160 | 	}
  161 | 
  162 | 	/**
  163 | 	 * Return the EntityManagerFactory that this instance should manage transactions for.
  164 | 	 */
  165 | 	public @Nullable EntityManagerFactory getEntityManagerFactory() {
  166 | 		return this.entityManagerFactory;
  167 | 	}
  168 | 
  169 | 	/**
  170 | 	 * Obtain the EntityManagerFactory for actual use.
  171 | 	 * @return the EntityManagerFactory (never {@code null})
  172 | 	 * @throws IllegalStateException in case of no EntityManagerFactory set
  173 | 	 * @since 5.0
  174 | 	 */
  175 | 	protected final EntityManagerFactory obtainEntityManagerFactory() {
  176 | 		EntityManagerFactory emf = getEntityManagerFactory();
  177 | 		Assert.state(emf != null, "No EntityManagerFactory set");
  178 | 		return emf;
  179 | 	}
  180 | 
  181 | 	/**
  182 | 	 * Set the name of the persistence unit to manage transactions for.
  183 | 	 * <p>This is an alternative to specifying the EntityManagerFactory by direct reference,
  184 | 	 * resolving it by its persistence unit name instead. If no EntityManagerFactory and
  185 | 	 * no persistence unit name have been specified, a default EntityManagerFactory will
  186 | 	 * be retrieved by finding a single unique bean of type EntityManagerFactory.
  187 | 	 * @see #setEntityManagerFactory
  188 | 	 */
  189 | 	public void setPersistenceUnitName(@Nullable String persistenceUnitName) {
  190 | 		this.persistenceUnitName = persistenceUnitName;
  191 | 	}
  192 | 
  193 | 	/**
  194 | 	 * Return the name of the persistence unit to manage transactions for, if any.
  195 | 	 */
  196 | 	public @Nullable String getPersistenceUnitName() {
  197 | 		return this.persistenceUnitName;
  198 | 	}
  199 | 
  200 | 	/**
  201 | 	 * Specify JPA properties, to be passed into
  202 | 	 * {@code EntityManagerFactory.createEntityManager(Map)} (if any).
  203 | 	 * <p>Can be populated with a String "value" (parsed via PropertiesEditor)
  204 | 	 * or a "props" element in XML bean definitions.
  205 | 	 * @see jakarta.persistence.EntityManagerFactory#createEntityManager(java.util.Map)
  206 | 	 */
  207 | 	public void setJpaProperties(@Nullable Properties jpaProperties) {
  208 | 		CollectionUtils.mergePropertiesIntoMap(jpaProperties, this.jpaPropertyMap);
  209 | 	}
  210 | 
  211 | 	/**
  212 | 	 * Specify JPA properties as a Map, to be passed into
  213 | 	 * {@code EntityManagerFactory.createEntityManager(Map)} (if any).
  214 | 	 * <p>Can be populated with a "map" or "props" element in XML bean definitions.
  215 | 	 * @see jakarta.persistence.EntityManagerFactory#createEntityManager(java.util.Map)
  216 | 	 */
  217 | 	public void setJpaPropertyMap(@Nullable Map<String, ?> jpaProperties) {
  218 | 		if (jpaProperties != null) {
  219 | 			this.jpaPropertyMap.putAll(jpaProperties);
  220 | 		}
  221 | 	}
  222 | 
  223 | 	/**
  224 | 	 * Allow {@code Map} access to the JPA properties to be passed to the persistence
  225 | 	 * provider, with the option to add or override specific entries.
  226 | 	 * <p>Useful for specifying entries directly, for example via {@code jpaPropertyMap[myKey]}.
  227 | 	 */
  228 | 	public Map<String, Object> getJpaPropertyMap() {
  229 | 		return this.jpaPropertyMap;
  230 | 	}
  231 | 
  232 | 	/**
  233 | 	 * Set the JDBC DataSource that this instance should manage transactions for.
  234 | 	 * The DataSource should match the one used by the JPA EntityManagerFactory:
  235 | 	 * for example, you could specify the same JNDI DataSource for both.
  236 | 	 * <p>If the EntityManagerFactory uses a known DataSource as its connection factory,
  237 | 	 * the DataSource will be autodetected: You can still explicitly specify the
  238 | 	 * DataSource, but you don't need to in this case.
  239 | 	 * <p>A transactional JDBC Connection for this DataSource will be provided to
  240 | 	 * application code accessing this DataSource directly via DataSourceUtils
  241 | 	 * or JdbcTemplate. The Connection will be taken from the JPA EntityManager.
  242 | 	 * <p>Note that you need to use a JPA dialect for a specific JPA implementation
  243 | 	 * to allow for exposing JPA transactions as JDBC transactions.
  244 | 	 * <p>The DataSource specified here should be the target DataSource to manage
  245 | 	 * transactions for, not a TransactionAwareDataSourceProxy. Only data access
  246 | 	 * code may work with TransactionAwareDataSourceProxy, while the transaction
  247 | 	 * manager needs to work on the underlying target DataSource. If there's
  248 | 	 * nevertheless a TransactionAwareDataSourceProxy passed in, it will be
  249 | 	 * unwrapped to extract its target DataSource.
  250 | 	 * @see EntityManagerFactoryInfo#getDataSource()
  251 | 	 * @see #setJpaDialect
  252 | 	 * @see org.springframework.jdbc.datasource.TransactionAwareDataSourceProxy
  253 | 	 * @see org.springframework.jdbc.datasource.DataSourceUtils
  254 | 	 * @see org.springframework.jdbc.core.JdbcTemplate
  255 | 	 */
  256 | 	public void setDataSource(@Nullable DataSource dataSource) {
  257 | 		if (dataSource instanceof TransactionAwareDataSourceProxy proxy) {
  258 | 			// If we got a TransactionAwareDataSourceProxy, we need to perform transactions
  259 | 			// for its underlying target DataSource, else data access code won't see
  260 | 			// properly exposed transactions (i.e. transactions for the target DataSource).
  261 | 			this.dataSource = proxy.getTargetDataSource();
  262 | 		}
  263 | 		else {
  264 | 			this.dataSource = dataSource;
  265 | 		}
  266 | 	}
  267 | 
  268 | 	/**
  269 | 	 * Return the JDBC DataSource that this instance manages transactions for.
  270 | 	 */
  271 | 	public @Nullable DataSource getDataSource() {
  272 | 		return this.dataSource;
  273 | 	}
  274 | 
  275 | 	/**
  276 | 	 * Set the JPA dialect to use for this transaction manager.
  277 | 	 * Used for vendor-specific transaction management and JDBC connection exposure.
  278 | 	 * <p>If the EntityManagerFactory uses a known JpaDialect, it will be autodetected:
  279 | 	 * You can still explicitly specify the DataSource, but you don't need to in this case.
  280 | 	 * <p>The dialect object can be used to retrieve the underlying JDBC connection
  281 | 	 * and thus allows for exposing JPA transactions as JDBC transactions.
  282 | 	 * @see EntityManagerFactoryInfo#getJpaDialect()
  283 | 	 * @see JpaDialect#beginTransaction
  284 | 	 * @see JpaDialect#getJdbcConnection
  285 | 	 */
  286 | 	public void setJpaDialect(@Nullable JpaDialect jpaDialect) {
  287 | 		this.jpaDialect = (jpaDialect != null ? jpaDialect : new DefaultJpaDialect());
  288 | 	}
  289 | 
  290 | 	/**
  291 | 	 * Return the JPA dialect to use for this transaction manager.
  292 | 	 */
  293 | 	public JpaDialect getJpaDialect() {
  294 | 		return this.jpaDialect;
  295 | 	}
  296 | 
  297 | 	/**
  298 | 	 * Specify a callback for customizing every {@code EntityManager} resource
  299 | 	 * created for a new transaction managed by this {@code JpaTransactionManager}.
  300 | 	 * <p>This is an alternative to a factory-level {@code EntityManager} customizer
  301 | 	 * and to a {@code JpaVendorAdapter}-level {@code postProcessEntityManager}
  302 | 	 * callback, enabling specific customizations of transactional resources.
  303 | 	 * @since 5.3
  304 | 	 * @see #createEntityManagerForTransaction()
  305 | 	 * @see AbstractEntityManagerFactoryBean#setEntityManagerInitializer
  306 | 	 * @see JpaVendorAdapter#postProcessEntityManager
  307 | 	 */
  308 | 	public void setEntityManagerInitializer(Consumer<EntityManager> entityManagerInitializer) {
  309 | 		this.entityManagerInitializer = entityManagerInitializer;
  310 | 	}
  311 | 
  312 | 	/**
  313 | 	 * Retrieves an EntityManagerFactory by persistence unit name, if none set explicitly.
  314 | 	 * Falls back to a default EntityManagerFactory bean if no persistence unit specified.
  315 | 	 * @see #setPersistenceUnitName
  316 | 	 */
  317 | 	@Override
  318 | 	public void setBeanFactory(BeanFactory beanFactory) throws BeansException {
  319 | 		if (getEntityManagerFactory() == null) {
  320 | 			if (!(beanFactory instanceof ListableBeanFactory lbf)) {
  321 | 				throw new IllegalStateException("Cannot retrieve EntityManagerFactory by persistence unit name " +
  322 | 						"in a non-listable BeanFactory: " + beanFactory);
  323 | 			}
  324 | 			setEntityManagerFactory(EntityManagerFactoryUtils.findEntityManagerFactory(lbf, getPersistenceUnitName()));
  325 | 		}
  326 | 	}
  327 | 
  328 | 	/**
  329 | 	 * Eagerly initialize the JPA dialect, creating a default one
  330 | 	 * for the specified EntityManagerFactory if none set.
  331 | 	 * Auto-detect the EntityManagerFactory's DataSource, if any.
  332 | 	 */
  333 | 	@Override
  334 | 	public void afterPropertiesSet() {
  335 | 		if (getEntityManagerFactory() == null) {
  336 | 			throw new IllegalArgumentException("'entityManagerFactory' or 'persistenceUnitName' is required");
  337 | 		}
  338 | 		if (getEntityManagerFactory() instanceof EntityManagerFactoryInfo emfInfo) {
  339 | 			DataSource dataSource = emfInfo.getDataSource();
  340 | 			if (dataSource != null) {
  341 | 				setDataSource(dataSource);
  342 | 			}
  343 | 			JpaDialect jpaDialect = emfInfo.getJpaDialect();
  344 | 			if (jpaDialect != null) {
  345 | 				setJpaDialect(jpaDialect);
  346 | 			}
  347 | 		}
  348 | 	}
  349 | 
  350 | 
  351 | 	@Override
  352 | 	public Object getResourceFactory() {
  353 | 		return obtainEntityManagerFactory();
  354 | 	}
  355 | 
  356 | 	@Override
  357 | 	protected Object doGetTransaction() {
  358 | 		JpaTransactionObject txObject = new JpaTransactionObject();
  359 | 		txObject.setSavepointAllowed(isNestedTransactionAllowed());
  360 | 
  361 | 		EntityManagerHolder emHolder = (EntityManagerHolder)
  362 | 				TransactionSynchronizationManager.getResource(obtainEntityManagerFactory());
  363 | 		if (emHolder != null) {
  364 | 			if (logger.isDebugEnabled()) {
  365 | 				logger.debug("Found thread-bound EntityManager [" + emHolder.getEntityManager() +
  366 | 						"] for JPA transaction");
  367 | 			}
  368 | 			txObject.setEntityManagerHolder(emHolder, false);
  369 | 		}
  370 | 
  371 | 		if (getDataSource() != null) {
  372 | 			ConnectionHolder conHolder = (ConnectionHolder)
  373 | 					TransactionSynchronizationManager.getResource(getDataSource());
  374 | 			txObject.setConnectionHolder(conHolder);
  375 | 		}
  376 | 
  377 | 		return txObject;
  378 | 	}
  379 | 
  380 | 	@Override
  381 | 	protected boolean isExistingTransaction(Object transaction) {
  382 | 		return ((JpaTransactionObject) transaction).hasTransaction();
  383 | 	}
  384 | 
  385 | 	@Override
  386 | 	protected void doBegin(Object transaction, TransactionDefinition definition) {
  387 | 		JpaTransactionObject txObject = (JpaTransactionObject) transaction;
  388 | 
  389 | 		if (txObject.hasConnectionHolder() && !txObject.getConnectionHolder().isSynchronizedWithTransaction()) {
  390 | 			throw new IllegalTransactionStateException("""
  391 | 					Pre-bound JDBC Connection found! JpaTransactionManager does not support running within \
  392 | 					another transaction manager that exposes a JDBC Connection for the same DataSource \
  393 | 					(or after a leaked Connection binding) if told to manage the DataSource itself. \
  394 | 					Use a single JpaTransactionManager for all transactions on a single DataSource.""");
  395 | 		}
  396 | 
  397 | 		try {
  398 | 			if (!txObject.hasEntityManagerHolder() ||
  399 | 					txObject.getEntityManagerHolder().isSynchronizedWithTransaction()) {
  400 | 				EntityManager newEm = createEntityManagerForTransaction();
  401 | 				if (logger.isDebugEnabled()) {
  402 | 					logger.debug("Opened new EntityManager [" + newEm + "] for JPA transaction");
  403 | 				}
  404 | 				txObject.setEntityManagerHolder(new EntityManagerHolder(newEm), true);
  405 | 			}
  406 | 
  407 | 			EntityManager em = txObject.getEntityManagerHolder().getEntityManager();
  408 | 
  409 | 			// Delegate to JpaDialect for actual transaction begin.
  410 | 			int timeoutToUse = determineTimeout(definition);
  411 | 			Object transactionData = getJpaDialect().beginTransaction(em,
  412 | 					new JpaTransactionDefinition(definition, timeoutToUse, txObject.isNewEntityManagerHolder()));
  413 | 			txObject.setTransactionData(transactionData);
  414 | 			txObject.setReadOnly(definition.isReadOnly());
  415 | 
  416 | 			// Register transaction timeout.
  417 | 			if (timeoutToUse != TransactionDefinition.TIMEOUT_DEFAULT) {
  418 | 				txObject.getEntityManagerHolder().setTimeoutInSeconds(timeoutToUse);
  419 | 			}
  420 | 
  421 | 			// Register the JPA EntityManager's JDBC Connection for the DataSource, if set.
  422 | 			if (getDataSource() != null) {
  423 | 				ConnectionHandle conHandle = getJpaDialect().getJdbcConnection(em, definition.isReadOnly());
  424 | 				if (conHandle != null) {
  425 | 					ConnectionHolder conHolder = new ConnectionHolder(conHandle);
  426 | 					if (timeoutToUse != TransactionDefinition.TIMEOUT_DEFAULT) {
  427 | 						conHolder.setTimeoutInSeconds(timeoutToUse);
  428 | 					}
  429 | 					if (logger.isDebugEnabled()) {
  430 | 						logger.debug("Exposing JPA transaction as JDBC [" + conHandle + "]");
  431 | 					}
  432 | 					TransactionSynchronizationManager.bindResource(getDataSource(), conHolder);
  433 | 					txObject.setConnectionHolder(conHolder);
  434 | 				}
  435 | 				else {
  436 | 					if (logger.isDebugEnabled()) {
  437 | 						logger.debug("Not exposing JPA transaction [" + em + "] as JDBC transaction because " +
  438 | 								"JpaDialect [" + getJpaDialect() + "] does not support JDBC Connection retrieval");
  439 | 					}
  440 | 				}
  441 | 			}
  442 | 
  443 | 			// Bind the entity manager holder to the thread.
  444 | 			if (txObject.isNewEntityManagerHolder()) {
  445 | 				TransactionSynchronizationManager.bindResource(
  446 | 						obtainEntityManagerFactory(), txObject.getEntityManagerHolder());
  447 | 			}
  448 | 			txObject.getEntityManagerHolder().setSynchronizedWithTransaction(true);
  449 | 		}
  450 | 
  451 | 		catch (TransactionException ex) {
  452 | 			closeEntityManagerAfterFailedBegin(txObject);
  453 | 			throw ex;
  454 | 		}
  455 | 		catch (Throwable ex) {
  456 | 			closeEntityManagerAfterFailedBegin(txObject);
  457 | 			throw new CannotCreateTransactionException("Could not open JPA EntityManager for transaction", ex);
  458 | 		}
  459 | 	}
  460 | 
  461 | 	/**
  462 | 	 * Create a JPA EntityManager to be used for a transaction.
  463 | 	 * <p>The default implementation checks whether the EntityManagerFactory
  464 | 	 * is a Spring proxy and delegates to
  465 | 	 * {@link EntityManagerFactoryInfo#createNativeEntityManager}
  466 | 	 * if possible which in turns applies
  467 | 	 * {@link JpaVendorAdapter#postProcessEntityManager(EntityManager)}.
  468 | 	 * @see jakarta.persistence.EntityManagerFactory#createEntityManager()
  469 | 	 */
  470 | 	protected EntityManager createEntityManagerForTransaction() {
  471 | 		EntityManagerFactory emf = obtainEntityManagerFactory();
  472 | 		Map<String, Object> properties = getJpaPropertyMap();
  473 | 		EntityManager em;
  474 | 		if (emf instanceof EntityManagerFactoryInfo emfInfo) {
  475 | 			em = emfInfo.createNativeEntityManager(properties);
  476 | 		}
  477 | 		else {
  478 | 			em = EntityManagerFactoryUtils.createEntityManager(emf, properties);
  479 | 		}
  480 | 		if (this.entityManagerInitializer != null) {
  481 | 			this.entityManagerInitializer.accept(em);
  482 | 		}
  483 | 		return em;
  484 | 	}
  485 | 
  486 | 	/**
  487 | 	 * Close the current transaction's EntityManager.
  488 | 	 * Called after a transaction begin attempt failed.
  489 | 	 * @param txObject the current transaction
  490 | 	 */
  491 | 	protected void closeEntityManagerAfterFailedBegin(JpaTransactionObject txObject) {
  492 | 		if (txObject.isNewEntityManagerHolder() && txObject.getEntityManagerHolder().hasEntityManager()) {
  493 | 			EntityManager em = txObject.getEntityManagerHolder().getEntityManager();
  494 | 			try {
  495 | 				if (em.getTransaction().isActive()) {
  496 | 					em.getTransaction().rollback();
  497 | 				}
  498 | 			}
  499 | 			catch (Throwable ex) {
  500 | 				logger.debug("Could not rollback EntityManager after failed transaction begin", ex);
  501 | 			}
  502 | 			finally {
  503 | 				EntityManagerFactoryUtils.closeEntityManager(em);
  504 | 			}
  505 | 			txObject.setEntityManagerHolder(null, false);
  506 | 		}
  507 | 	}
  508 | 
  509 | 	@Override
  510 | 	protected Object doSuspend(Object transaction) {
  511 | 		JpaTransactionObject txObject = (JpaTransactionObject) transaction;
  512 | 		txObject.setEntityManagerHolder(null, false);
  513 | 		EntityManagerHolder entityManagerHolder = (EntityManagerHolder)
  514 | 				TransactionSynchronizationManager.unbindResource(obtainEntityManagerFactory());
  515 | 		txObject.setConnectionHolder(null);
  516 | 		ConnectionHolder connectionHolder = null;
  517 | 		if (getDataSource() != null && TransactionSynchronizationManager.hasResource(getDataSource())) {
  518 | 			connectionHolder = (ConnectionHolder) TransactionSynchronizationManager.unbindResource(getDataSource());
  519 | 		}
  520 | 		return new SuspendedResourcesHolder(entityManagerHolder, connectionHolder);
  521 | 	}
  522 | 
  523 | 	@Override
  524 | 	protected void doResume(@Nullable Object transaction, Object suspendedResources) {
  525 | 		SuspendedResourcesHolder resourcesHolder = (SuspendedResourcesHolder) suspendedResources;
  526 | 		TransactionSynchronizationManager.bindResource(
  527 | 				obtainEntityManagerFactory(), resourcesHolder.getEntityManagerHolder());
  528 | 		ConnectionHolder connectionHolder = resourcesHolder.getConnectionHolder();
  529 | 		if (connectionHolder != null && getDataSource() != null) {
  530 | 			TransactionSynchronizationManager.bindResource(getDataSource(), connectionHolder);
  531 | 		}
  532 | 	}
  533 | 
  534 | 	/**
  535 | 	 * This implementation returns "true": a JPA commit will properly handle
  536 | 	 * transactions that have been marked rollback-only at a global level.
  537 | 	 */
  538 | 	@Override
  539 | 	protected boolean shouldCommitOnGlobalRollbackOnly() {
  540 | 		return true;
  541 | 	}
  542 | 
  543 | 	@Override
  544 | 	protected void doCommit(DefaultTransactionStatus status) {
  545 | 		JpaTransactionObject txObject = (JpaTransactionObject) status.getTransaction();
  546 | 		if (status.isDebug()) {
  547 | 			logger.debug("Committing JPA transaction on EntityManager [" +
  548 | 					txObject.getEntityManagerHolder().getEntityManager() + "]");
  549 | 		}
  550 | 		try {
  551 | 			EntityTransaction tx = txObject.getEntityManagerHolder().getEntityManager().getTransaction();
  552 | 			tx.commit();
  553 | 		}
  554 | 		catch (RollbackException ex) {
  555 | 			if (ex.getCause() instanceof RuntimeException runtimeException) {
  556 | 				DataAccessException dae = getJpaDialect().translateExceptionIfPossible(runtimeException);
  557 | 				if (dae != null) {
  558 | 					throw dae;
  559 | 				}
  560 | 			}
  561 | 			throw new TransactionSystemException("Could not commit JPA transaction", ex);
  562 | 		}
  563 | 		catch (RuntimeException ex) {
  564 | 			// Assumably failed to flush changes to database.
  565 | 			throw DataAccessUtils.translateIfNecessary(ex, getJpaDialect());
  566 | 		}
  567 | 	}
  568 | 
  569 | 	@Override
  570 | 	protected void doRollback(DefaultTransactionStatus status) {
  571 | 		JpaTransactionObject txObject = (JpaTransactionObject) status.getTransaction();
  572 | 		if (status.isDebug()) {
  573 | 			logger.debug("Rolling back JPA transaction on EntityManager [" +
  574 | 					txObject.getEntityManagerHolder().getEntityManager() + "]");
  575 | 		}
  576 | 		try {
  577 | 			EntityTransaction tx = txObject.getEntityManagerHolder().getEntityManager().getTransaction();
  578 | 			if (tx.isActive()) {
  579 | 				tx.rollback();
  580 | 			}
  581 | 		}
  582 | 		catch (PersistenceException ex) {
  583 | 			DataAccessException dae = getJpaDialect().translateExceptionIfPossible(ex);
  584 | 			if (dae != null) {
  585 | 				throw dae;
  586 | 			}
  587 | 			throw new TransactionSystemException("Could not roll back JPA transaction", ex);
  588 | 		}
  589 | 		finally {
  590 | 			if (!txObject.isNewEntityManagerHolder()) {
  591 | 				// Clear all pending inserts/updates/deletes in the EntityManager.
  592 | 				// Necessary for pre-bound EntityManagers, to avoid inconsistent state.
  593 | 				txObject.getEntityManagerHolder().getEntityManager().clear();
  594 | 			}
  595 | 		}
  596 | 	}
  597 | 
  598 | 	@Override
  599 | 	protected void doSetRollbackOnly(DefaultTransactionStatus status) {
  600 | 		JpaTransactionObject txObject = (JpaTransactionObject) status.getTransaction();
  601 | 		if (status.isDebug()) {
  602 | 			logger.debug("Setting JPA transaction on EntityManager [" +
  603 | 					txObject.getEntityManagerHolder().getEntityManager() + "] rollback-only");
  604 | 		}
  605 | 		txObject.setRollbackOnly();
  606 | 	}
  607 | 
  608 | 	@Override
  609 | 	protected void doCleanupAfterCompletion(Object transaction) {
  610 | 		JpaTransactionObject txObject = (JpaTransactionObject) transaction;
  611 | 
  612 | 		// Remove the entity manager holder from the thread, if still there.
  613 | 		// (Could have been removed by EntityManagerFactoryUtils in order
  614 | 		// to replace it with an unsynchronized EntityManager).
  615 | 		if (txObject.isNewEntityManagerHolder()) {
  616 | 			TransactionSynchronizationManager.unbindResourceIfPossible(obtainEntityManagerFactory());
  617 | 		}
  618 | 		txObject.getEntityManagerHolder().clear();
  619 | 
  620 | 		// Remove the JDBC connection holder from the thread, if exposed.
  621 | 		if (getDataSource() != null && txObject.hasConnectionHolder()) {
  622 | 			TransactionSynchronizationManager.unbindResource(getDataSource());
  623 | 			ConnectionHandle conHandle = txObject.getConnectionHolder().getConnectionHandle();
  624 | 			if (conHandle != null) {
  625 | 				try {
  626 | 					getJpaDialect().releaseJdbcConnection(conHandle,
  627 | 							txObject.getEntityManagerHolder().getEntityManager());
  628 | 				}
  629 | 				catch (Throwable ex) {
  630 | 					// Just log it, to keep a transaction-related exception.
  631 | 					logger.error("Failed to release JDBC connection after transaction", ex);
  632 | 				}
  633 | 			}
  634 | 		}
  635 | 
  636 | 		getJpaDialect().cleanupTransaction(txObject.getTransactionData());
  637 | 
  638 | 		// Remove the entity manager holder from the thread.
  639 | 		if (txObject.isNewEntityManagerHolder()) {
  640 | 			logger.debug("Closing JPA EntityManager after transaction");
  641 | 			txObject.getEntityManagerHolder().closeAll();
  642 | 		}
  643 | 		else {
  644 | 			logger.debug("Not closing pre-bound JPA EntityManager after transaction");
  645 | 		}
  646 | 	}
  647 | 
  648 | 
  649 | 	/**
  650 | 	 * JPA transaction object, representing a EntityManagerHolder.
  651 | 	 * Used as transaction object by JpaTransactionManager.
  652 | 	 */
  653 | 	private class JpaTransactionObject extends JdbcTransactionObjectSupport {
  654 | 
  655 | 		private @Nullable EntityManagerHolder entityManagerHolder;
  656 | 
  657 | 		private boolean newEntityManagerHolder;
  658 | 
  659 | 		private @Nullable Object transactionData;
  660 | 
  661 | 		public void setEntityManagerHolder(
  662 | 				@Nullable EntityManagerHolder entityManagerHolder, boolean newEntityManagerHolder) {
  663 | 
  664 | 			this.entityManagerHolder = entityManagerHolder;
  665 | 			this.newEntityManagerHolder = newEntityManagerHolder;
  666 | 		}
  667 | 
  668 | 		public EntityManagerHolder getEntityManagerHolder() {
  669 | 			Assert.state(this.entityManagerHolder != null, "No EntityManagerHolder available");
  670 | 			return this.entityManagerHolder;
  671 | 		}
  672 | 
  673 | 		public boolean hasEntityManagerHolder() {
  674 | 			return (this.entityManagerHolder != null);
  675 | 		}
  676 | 
  677 | 		public boolean isNewEntityManagerHolder() {
  678 | 			return this.newEntityManagerHolder;
  679 | 		}
  680 | 
  681 | 		public boolean hasTransaction() {
  682 | 			return (this.entityManagerHolder != null && this.entityManagerHolder.isTransactionActive());
  683 | 		}
  684 | 
  685 | 		public void setTransactionData(@Nullable Object transactionData) {
  686 | 			this.transactionData = transactionData;
  687 | 			getEntityManagerHolder().setTransactionActive(true);
  688 | 			if (transactionData instanceof SavepointManager savepointManager) {
  689 | 				getEntityManagerHolder().setSavepointManager(savepointManager);
  690 | 			}
  691 | 		}
  692 | 
  693 | 		public @Nullable Object getTransactionData() {
  694 | 			return this.transactionData;
  695 | 		}
  696 | 
  697 | 		public void setRollbackOnly() {
  698 | 			EntityTransaction tx = getEntityManagerHolder().getEntityManager().getTransaction();
  699 | 			if (tx.isActive()) {
  700 | 				tx.setRollbackOnly();
  701 | 			}
  702 | 			if (hasConnectionHolder()) {
  703 | 				getConnectionHolder().setRollbackOnly();
  704 | 			}
  705 | 		}
  706 | 
  707 | 		@Override
  708 | 		public boolean isRollbackOnly() {
  709 | 			EntityTransaction tx = getEntityManagerHolder().getEntityManager().getTransaction();
  710 | 			return tx.getRollbackOnly();
  711 | 		}
  712 | 
  713 | 		@Override
  714 | 		public void flush() {
  715 | 			try {
  716 | 				getEntityManagerHolder().getEntityManager().flush();
  717 | 			}
  718 | 			catch (RuntimeException ex) {
  719 | 				throw DataAccessUtils.translateIfNecessary(ex, getJpaDialect());
  720 | 			}
  721 | 		}
  722 | 
  723 | 		@Override
  724 | 		public Object createSavepoint() throws TransactionException {
  725 | 			if (getEntityManagerHolder().isRollbackOnly()) {
  726 | 				throw new CannotCreateTransactionException(
  727 | 						"Cannot create savepoint for transaction which is already marked as rollback-only");
  728 | 			}
  729 | 			return getSavepointManager().createSavepoint();
  730 | 		}
  731 | 
  732 | 		@Override
  733 | 		public void rollbackToSavepoint(Object savepoint) throws TransactionException {
  734 | 			getSavepointManager().rollbackToSavepoint(savepoint);
  735 | 			getEntityManagerHolder().resetRollbackOnly();
  736 | 		}
  737 | 
  738 | 		@Override
  739 | 		public void releaseSavepoint(Object savepoint) throws TransactionException {
  740 | 			getSavepointManager().releaseSavepoint(savepoint);
  741 | 		}
  742 | 
  743 | 		private SavepointManager getSavepointManager() {
  744 | 			if (!isSavepointAllowed()) {
  745 | 				throw new NestedTransactionNotSupportedException(
  746 | 						"Transaction manager does not allow nested transactions");
  747 | 			}
  748 | 			SavepointManager savepointManager = getEntityManagerHolder().getSavepointManager();
  749 | 			if (savepointManager == null) {
  750 | 				throw new NestedTransactionNotSupportedException(
  751 | 						"JpaDialect does not support savepoints - check your JPA provider's capabilities");
  752 | 			}
  753 | 			return savepointManager;
  754 | 		}
  755 | 	}
  756 | 
  757 | 
  758 | 	/**
  759 | 	 * JPA-specific transaction definition to be passed to {@link JpaDialect#beginTransaction}.
  760 | 	 * @since 5.1
  761 | 	 */
  762 | 	private static class JpaTransactionDefinition extends DelegatingTransactionDefinition
  763 | 			implements ResourceTransactionDefinition {
  764 | 
  765 | 		private final int timeout;
  766 | 
  767 | 		private final boolean localResource;
  768 | 
  769 | 		public JpaTransactionDefinition(TransactionDefinition targetDefinition, int timeout, boolean localResource) {
  770 | 			super(targetDefinition);
  771 | 			this.timeout = timeout;
  772 | 			this.localResource = localResource;
  773 | 		}
  774 | 
  775 | 		@Override
  776 | 		public int getTimeout() {
  777 | 			return this.timeout;
  778 | 		}
  779 | 
  780 | 		@Override
  781 | 		public boolean isLocalResource() {
  782 | 			return this.localResource;
  783 | 		}
  784 | 	}
  785 | 
  786 | 
  787 | 	/**
  788 | 	 * Holder for suspended resources.
  789 | 	 * Used internally by {@code doSuspend} and {@code doResume}.
  790 | 	 */
  791 | 	private static final class SuspendedResourcesHolder {
  792 | 
  793 | 		private final EntityManagerHolder entityManagerHolder;
  794 | 
  795 | 		private final @Nullable ConnectionHolder connectionHolder;
  796 | 
  797 | 		private SuspendedResourcesHolder(EntityManagerHolder emHolder, @Nullable ConnectionHolder conHolder) {
  798 | 			this.entityManagerHolder = emHolder;
  799 | 			this.connectionHolder = conHolder;
  800 | 		}
  801 | 
  802 | 		private EntityManagerHolder getEntityManagerHolder() {
  803 | 			return this.entityManagerHolder;
  804 | 		}
  805 | 
  806 | 		private @Nullable ConnectionHolder getConnectionHolder() {
  807 | 			return this.connectionHolder;
  808 | 		}
  809 | 	}
  810 | 
  811 | }
```
