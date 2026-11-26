<!-- learning-docs source archive（机械抓取原文，禁止手改代码内容） -->
<!-- origin: org.springframework:spring-jdbc:7.1.0 | path: org/springframework/jdbc/datasource/DataSourceTransactionManager.java -->
<!-- 正文引用：[[源码:DataSourceTransactionManager.java#L起-L止]] ；代码块头部：(src: DataSourceTransactionManager.java#L起-L止) -->

# DataSourceTransactionManager.java

| 项 | 值 |
| --- | --- |
| 来源 | `org.springframework:spring-jdbc:7.1.0` |
| 路径 | `org/springframework/jdbc/datasource/DataSourceTransactionManager.java` |
| 行数 | 523 |
| 在线地址 | https://repo1.maven.org/maven2/org/springframework/spring-jdbc/7.1.0/spring-jdbc-7.1.0-sources.jar |
| 抓取时间 | 2026-11-25T14:01:03.935Z |

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
   17 | package org.springframework.jdbc.datasource;
   18 | 
   19 | import java.sql.Connection;
   20 | import java.sql.SQLException;
   21 | import java.sql.Statement;
   22 | 
   23 | import javax.sql.DataSource;
   24 | 
   25 | import org.jspecify.annotations.Nullable;
   26 | 
   27 | import org.springframework.beans.factory.InitializingBean;
   28 | import org.springframework.transaction.CannotCreateTransactionException;
   29 | import org.springframework.transaction.TransactionDefinition;
   30 | import org.springframework.transaction.TransactionSystemException;
   31 | import org.springframework.transaction.support.AbstractPlatformTransactionManager;
   32 | import org.springframework.transaction.support.DefaultTransactionStatus;
   33 | import org.springframework.transaction.support.ResourceTransactionManager;
   34 | import org.springframework.transaction.support.TransactionSynchronizationManager;
   35 | import org.springframework.transaction.support.TransactionSynchronizationUtils;
   36 | import org.springframework.util.Assert;
   37 | 
   38 | /**
   39 |  * {@link org.springframework.transaction.PlatformTransactionManager} implementation
   40 |  * for a single JDBC {@link javax.sql.DataSource}. This class is capable of working
   41 |  * in any environment with any JDBC driver, as long as the setup uses a
   42 |  * {@code javax.sql.DataSource} as its {@code Connection} factory mechanism.
   43 |  * Binds a JDBC {@code Connection} from the specified {@code DataSource} to the
   44 |  * current thread, potentially allowing for one thread-bound {@code Connection}
   45 |  * per {@code DataSource}.
   46 |  *
   47 |  * <p><b>Note: The {@code DataSource} that this transaction manager operates on
   48 |  * needs to return independent {@code Connection}s.</b> The {@code Connection}s
   49 |  * typically come from a connection pool but the {@code DataSource} must not return
   50 |  * specifically scoped or constrained {@code Connection}s. This transaction manager
   51 |  * will associate {@code Connection}s with thread-bound transactions, according
   52 |  * to the specified propagation behavior. It assumes that a separate, independent
   53 |  * {@code Connection} can be obtained even during an ongoing transaction.
   54 |  *
   55 |  * <p>Application code is required to retrieve the JDBC {@code Connection} via
   56 |  * {@link DataSourceUtils#getConnection(DataSource)} instead of a standard
   57 |  * EE-style {@link DataSource#getConnection()} call. Spring classes such as
   58 |  * {@link org.springframework.jdbc.core.JdbcTemplate} use this strategy implicitly.
   59 |  * If not used in combination with this transaction manager, the
   60 |  * {@link DataSourceUtils} lookup strategy behaves exactly like the native
   61 |  * {@code DataSource} lookup; it can thus be used in a portable fashion.
   62 |  *
   63 |  * <p>Alternatively, you can allow application code to work with the standard
   64 |  * EE-style lookup pattern {@link DataSource#getConnection()}, for example
   65 |  * for legacy code that is not aware of Spring at all. In that case, define a
   66 |  * {@link TransactionAwareDataSourceProxy} for your target {@code DataSource},
   67 |  * and pass that proxy {@code DataSource} to your DAOs which will automatically
   68 |  * participate in Spring-managed transactions when accessing it.
   69 |  *
   70 |  * <p>Supports custom isolation levels, and timeouts which get applied as
   71 |  * appropriate JDBC statement timeouts. To support the latter, application code
   72 |  * must either use {@link org.springframework.jdbc.core.JdbcTemplate}, call
   73 |  * {@link DataSourceUtils#applyTransactionTimeout} for each created JDBC
   74 |  * {@code Statement}, or go through a {@link TransactionAwareDataSourceProxy}
   75 |  * which will create timeout-aware JDBC {@code Connection}s and {@code Statement}s
   76 |  * automatically.
   77 |  *
   78 |  * <p>Consider defining a {@link LazyConnectionDataSourceProxy} for your target
   79 |  * {@code DataSource}, pointing both this transaction manager and your DAOs to it.
   80 |  * This will lead to optimized handling of "empty" transactions, i.e. of transactions
   81 |  * without any JDBC statements executed. A {@code LazyConnectionDataSourceProxy} will
   82 |  * not fetch an actual JDBC {@code Connection} from the target {@code DataSource}
   83 |  * until a {@code Statement} gets executed, lazily applying the specified transaction
   84 |  * settings to the target {@code Connection}.
   85 |  *
   86 |  * <p>This transaction manager supports nested transactions via the JDBC
   87 |  * {@link java.sql.Savepoint} mechanism. The
   88 |  * {@link #setNestedTransactionAllowed "nestedTransactionAllowed"} flag defaults
   89 |  * to "true", since nested transactions will work without restrictions on JDBC
   90 |  * drivers that support savepoints (such as the Oracle JDBC driver).
   91 |  *
   92 |  * <p>This transaction manager can be used as a replacement for the
   93 |  * {@link org.springframework.transaction.jta.JtaTransactionManager} in the single
   94 |  * resource case, as it does not require a container that supports JTA, typically
   95 |  * in combination with a locally defined JDBC {@code DataSource} (for example, a Hikari
   96 |  * connection pool). Switching between this local strategy and a JTA environment
   97 |  * is just a matter of configuration!
   98 |  *
   99 |  * <p>As of 4.3.4, this transaction manager triggers flush callbacks on registered
  100 |  * transaction synchronizations (if synchronization is generally active), assuming
  101 |  * resources operating on the underlying JDBC {@code Connection}. This allows for
  102 |  * setup analogous to {@code JtaTransactionManager}, in particular with respect to
  103 |  * lazily registered ORM resources (for example, a Hibernate {@code Session}).
  104 |  *
  105 |  * <p><b>NOTE: As of 5.3, {@link org.springframework.jdbc.support.JdbcTransactionManager}
  106 |  * is available as an extended subclass which includes commit/rollback exception
  107 |  * translation, aligned with {@link org.springframework.jdbc.core.JdbcTemplate}.</b>
  108 |  *
  109 |  * @author Juergen Hoeller
  110 |  * @since 02.05.2003
  111 |  * @see #setNestedTransactionAllowed
  112 |  * @see java.sql.Savepoint
  113 |  * @see DataSourceUtils#getConnection(javax.sql.DataSource)
  114 |  * @see DataSourceUtils#applyTransactionTimeout
  115 |  * @see DataSourceUtils#releaseConnection
  116 |  * @see TransactionAwareDataSourceProxy
  117 |  * @see LazyConnectionDataSourceProxy
  118 |  * @see org.springframework.jdbc.core.JdbcTemplate
  119 |  * @see org.springframework.jdbc.support.JdbcTransactionManager
  120 |  */
  121 | @SuppressWarnings("serial")
  122 | public class DataSourceTransactionManager extends AbstractPlatformTransactionManager
  123 | 		implements ResourceTransactionManager, InitializingBean {
  124 | 
  125 | 	private @Nullable DataSource dataSource;
  126 | 
  127 | 	private boolean enforceReadOnly = false;
  128 | 
  129 | 	private volatile @Nullable Boolean defaultReadOnly;
  130 | 
  131 | 
  132 | 	/**
  133 | 	 * Create a new {@code DataSourceTransactionManager} instance.
  134 | 	 * A {@code DataSource} has to be set to be able to use it.
  135 | 	 * @see #setDataSource
  136 | 	 */
  137 | 	public DataSourceTransactionManager() {
  138 | 		setNestedTransactionAllowed(true);
  139 | 	}
  140 | 
  141 | 	/**
  142 | 	 * Create a new {@code DataSourceTransactionManager} instance.
  143 | 	 * @param dataSource the JDBC DataSource to manage transactions for
  144 | 	 */
  145 | 	public DataSourceTransactionManager(DataSource dataSource) {
  146 | 		this();
  147 | 		setDataSource(dataSource);
  148 | 		afterPropertiesSet();
  149 | 	}
  150 | 
  151 | 
  152 | 	/**
  153 | 	 * Set the JDBC {@code DataSource} that this instance should manage transactions for.
  154 | 	 * <p>This will typically be a locally defined {@code DataSource}, for example a
  155 | 	 * Hikari connection pool. Alternatively, you can also manage transactions for a
  156 | 	 * non-XA {@code DataSource} fetched from JNDI. For an XA {@code DataSource},
  157 | 	 * use {@link org.springframework.transaction.jta.JtaTransactionManager} instead.
  158 | 	 * <p>The {@code DataSource} specified here should be the target {@code DataSource}
  159 | 	 * to manage transactions for, not a {@link TransactionAwareDataSourceProxy}.
  160 | 	 * Only data access code may work with {@code TransactionAwareDataSourceProxy} while
  161 | 	 * the transaction manager needs to work on the underlying target {@code DataSource}.
  162 | 	 * If there is nevertheless a {@code TransactionAwareDataSourceProxy} passed in,
  163 | 	 * it will be unwrapped to extract its target {@code DataSource}.
  164 | 	 * <p><b>The {@code DataSource} passed in here needs to return independent
  165 | 	 * {@code Connection}s.</b> The {@code Connection}s typically come from a
  166 | 	 * connection pool but the {@code DataSource} must not return specifically
  167 | 	 * scoped or constrained {@code Connection}s, just possibly lazily fetched.
  168 | 	 * @see LazyConnectionDataSourceProxy
  169 | 	 */
  170 | 	public void setDataSource(@Nullable DataSource dataSource) {
  171 | 		if (dataSource instanceof TransactionAwareDataSourceProxy tadsp) {
  172 | 			// If we got a TransactionAwareDataSourceProxy, we need to perform transactions
  173 | 			// for its underlying target DataSource, else data access code won't see
  174 | 			// properly exposed transactions (i.e. transactions for the target DataSource).
  175 | 			this.dataSource = tadsp.getTargetDataSource();
  176 | 		}
  177 | 		else {
  178 | 			this.dataSource = dataSource;
  179 | 		}
  180 | 	}
  181 | 
  182 | 	/**
  183 | 	 * Return the JDBC {@code DataSource} that this instance manages transactions for.
  184 | 	 */
  185 | 	public @Nullable DataSource getDataSource() {
  186 | 		return this.dataSource;
  187 | 	}
  188 | 
  189 | 	/**
  190 | 	 * Obtain the {@code DataSource} for actual use.
  191 | 	 * @return the DataSource (never {@code null})
  192 | 	 * @throws IllegalStateException in case of no DataSource set
  193 | 	 * @since 5.0
  194 | 	 */
  195 | 	protected DataSource obtainDataSource() {
  196 | 		DataSource dataSource = getDataSource();
  197 | 		Assert.state(dataSource != null, "No DataSource set");
  198 | 		return dataSource;
  199 | 	}
  200 | 
  201 | 	/**
  202 | 	 * Specify whether to enforce the read-only nature of a transaction
  203 | 	 * (as indicated by {@link TransactionDefinition#isReadOnly()})
  204 | 	 * through an explicit statement on the transactional connection:
  205 | 	 * "SET TRANSACTION READ ONLY" as understood by Oracle, MySQL and Postgres.
  206 | 	 * <p>The exact treatment, including any SQL statement executed on the connection,
  207 | 	 * can be customized through {@link #prepareTransactionalConnection}.
  208 | 	 * <p>This mode of read-only handling goes beyond the {@link Connection#setReadOnly}
  209 | 	 * hint that Spring applies by default. In contrast to that standard JDBC hint,
  210 | 	 * "SET TRANSACTION READ ONLY" enforces an isolation-level-like connection mode
  211 | 	 * where data manipulation statements are strictly disallowed. Also, on Oracle,
  212 | 	 * this read-only mode provides read consistency for the entire transaction.
  213 | 	 * <p>Note that older Oracle JDBC drivers (9i, 10g) used to enforce this read-only
  214 | 	 * mode even for {@code Connection.setReadOnly(true}. However, with recent drivers,
  215 | 	 * this strong enforcement needs to be applied explicitly, for example, through this flag.
  216 | 	 * @since 4.3.7
  217 | 	 * @see #prepareTransactionalConnection
  218 | 	 */
  219 | 	public void setEnforceReadOnly(boolean enforceReadOnly) {
  220 | 		this.enforceReadOnly = enforceReadOnly;
  221 | 	}
  222 | 
  223 | 	/**
  224 | 	 * Return whether to enforce the read-only nature of a transaction
  225 | 	 * through an explicit statement on the transactional connection.
  226 | 	 * @since 4.3.7
  227 | 	 * @see #setEnforceReadOnly
  228 | 	 */
  229 | 	public boolean isEnforceReadOnly() {
  230 | 		return this.enforceReadOnly;
  231 | 	}
  232 | 
  233 | 	@Override
  234 | 	public void afterPropertiesSet() {
  235 | 		if (getDataSource() == null) {
  236 | 			throw new IllegalArgumentException("Property 'dataSource' is required");
  237 | 		}
  238 | 	}
  239 | 
  240 | 
  241 | 	@Override
  242 | 	public Object getResourceFactory() {
  243 | 		return obtainDataSource();
  244 | 	}
  245 | 
  246 | 	@Override
  247 | 	protected Object doGetTransaction() {
  248 | 		DataSourceTransactionObject txObject = new DataSourceTransactionObject();
  249 | 		txObject.setSavepointAllowed(isNestedTransactionAllowed());
  250 | 		ConnectionHolder conHolder =
  251 | 				(ConnectionHolder) TransactionSynchronizationManager.getResource(obtainDataSource());
  252 | 		txObject.setConnectionHolder(conHolder, false);
  253 | 		return txObject;
  254 | 	}
  255 | 
  256 | 	@Override
  257 | 	protected boolean isExistingTransaction(Object transaction) {
  258 | 		DataSourceTransactionObject txObject = (DataSourceTransactionObject) transaction;
  259 | 		return (txObject.hasConnectionHolder() && txObject.getConnectionHolder().isTransactionActive());
  260 | 	}
  261 | 
  262 | 	@Override
  263 | 	protected void doBegin(Object transaction, TransactionDefinition definition) {
  264 | 		DataSourceTransactionObject txObject = (DataSourceTransactionObject) transaction;
  265 | 		Connection con = null;
  266 | 
  267 | 		try {
  268 | 			if (!txObject.hasConnectionHolder() ||
  269 | 					txObject.getConnectionHolder().isSynchronizedWithTransaction()) {
  270 | 				Connection newCon = obtainDataSource().getConnection();
  271 | 				if (logger.isDebugEnabled()) {
  272 | 					logger.debug("Acquired Connection [" + newCon + "] for JDBC transaction");
  273 | 				}
  274 | 				if (definition.isReadOnly()) {
  275 | 					checkDefaultReadOnly(newCon);
  276 | 				}
  277 | 				txObject.setConnectionHolder(new ConnectionHolder(newCon), true);
  278 | 			}
  279 | 
  280 | 			txObject.getConnectionHolder().setSynchronizedWithTransaction(true);
  281 | 			con = txObject.getConnectionHolder().getConnection();
  282 | 
  283 | 			Integer previousIsolationLevel = DataSourceUtils.prepareConnectionForTransaction(con,
  284 | 					definition.getIsolationLevel(),
  285 | 					(definition.isReadOnly() && !isDefaultReadOnly()));
  286 | 			txObject.setPreviousIsolationLevel(previousIsolationLevel);
  287 | 			txObject.setReadOnly(definition.isReadOnly());
  288 | 
  289 | 			// Switch to manual commit if necessary. This is very expensive in some JDBC drivers,
  290 | 			// so we don't want to do it unnecessarily (for example if we've explicitly
  291 | 			// configured the connection pool to set it already).
  292 | 			if (con.getAutoCommit()) {
  293 | 				txObject.setMustRestoreAutoCommit(true);
  294 | 				if (logger.isDebugEnabled()) {
  295 | 					logger.debug("Switching JDBC Connection [" + con + "] to manual commit");
  296 | 				}
  297 | 				con.setAutoCommit(false);
  298 | 			}
  299 | 
  300 | 			prepareTransactionalConnection(con, definition);
  301 | 			txObject.getConnectionHolder().setTransactionActive(true);
  302 | 
  303 | 			int timeout = determineTimeout(definition);
  304 | 			if (timeout != TransactionDefinition.TIMEOUT_DEFAULT) {
  305 | 				txObject.getConnectionHolder().setTimeoutInSeconds(timeout);
  306 | 			}
  307 | 
  308 | 			// Bind the connection holder to the thread.
  309 | 			if (txObject.isNewConnectionHolder()) {
  310 | 				TransactionSynchronizationManager.bindResource(obtainDataSource(), txObject.getConnectionHolder());
  311 | 			}
  312 | 		}
  313 | 
  314 | 		catch (Throwable ex) {
  315 | 			if (txObject.isNewConnectionHolder()) {
  316 | 				DataSourceUtils.releaseConnection(con, obtainDataSource());
  317 | 				txObject.setConnectionHolder(null, false);
  318 | 			}
  319 | 			throw new CannotCreateTransactionException("Could not open JDBC Connection for transaction", ex);
  320 | 		}
  321 | 	}
  322 | 
  323 | 	@Override
  324 | 	protected Object doSuspend(Object transaction) {
  325 | 		DataSourceTransactionObject txObject = (DataSourceTransactionObject) transaction;
  326 | 		txObject.setConnectionHolder(null);
  327 | 		return TransactionSynchronizationManager.unbindResource(obtainDataSource());
  328 | 	}
  329 | 
  330 | 	@Override
  331 | 	protected void doResume(@Nullable Object transaction, Object suspendedResources) {
  332 | 		TransactionSynchronizationManager.bindResource(obtainDataSource(), suspendedResources);
  333 | 	}
  334 | 
  335 | 	@Override
  336 | 	protected void doCommit(DefaultTransactionStatus status) {
  337 | 		DataSourceTransactionObject txObject = (DataSourceTransactionObject) status.getTransaction();
  338 | 		Connection con = txObject.getConnectionHolder().getConnection();
  339 | 		if (status.isDebug()) {
  340 | 			logger.debug("Committing JDBC transaction on Connection [" + con + "]");
  341 | 		}
  342 | 		try {
  343 | 			con.commit();
  344 | 		}
  345 | 		catch (SQLException ex) {
  346 | 			throw translateException("JDBC commit", ex);
  347 | 		}
  348 | 	}
  349 | 
  350 | 	@Override
  351 | 	protected void doRollback(DefaultTransactionStatus status) {
  352 | 		DataSourceTransactionObject txObject = (DataSourceTransactionObject) status.getTransaction();
  353 | 		Connection con = txObject.getConnectionHolder().getConnection();
  354 | 		if (status.isDebug()) {
  355 | 			logger.debug("Rolling back JDBC transaction on Connection [" + con + "]");
  356 | 		}
  357 | 		try {
  358 | 			con.rollback();
  359 | 		}
  360 | 		catch (SQLException ex) {
  361 | 			throw translateException("JDBC rollback", ex);
  362 | 		}
  363 | 	}
  364 | 
  365 | 	@Override
  366 | 	protected void doSetRollbackOnly(DefaultTransactionStatus status) {
  367 | 		DataSourceTransactionObject txObject = (DataSourceTransactionObject) status.getTransaction();
  368 | 		if (status.isDebug()) {
  369 | 			logger.debug("Setting JDBC transaction [" + txObject.getConnectionHolder().getConnection() +
  370 | 					"] rollback-only");
  371 | 		}
  372 | 		txObject.setRollbackOnly();
  373 | 	}
  374 | 
  375 | 	@Override
  376 | 	protected void doCleanupAfterCompletion(Object transaction) {
  377 | 		DataSourceTransactionObject txObject = (DataSourceTransactionObject) transaction;
  378 | 
  379 | 		// Remove the connection holder from the thread, if exposed.
  380 | 		if (txObject.isNewConnectionHolder()) {
  381 | 			TransactionSynchronizationManager.unbindResource(obtainDataSource());
  382 | 		}
  383 | 
  384 | 		// Reset connection.
  385 | 		Connection con = txObject.getConnectionHolder().getConnection();
  386 | 		try {
  387 | 			if (txObject.isMustRestoreAutoCommit()) {
  388 | 				con.setAutoCommit(true);
  389 | 			}
  390 | 			DataSourceUtils.resetConnectionAfterTransaction(con,
  391 | 					txObject.getPreviousIsolationLevel(),
  392 | 					(txObject.isReadOnly() && !isDefaultReadOnly()));
  393 | 		}
  394 | 		catch (Throwable ex) {
  395 | 			logger.debug("Could not reset JDBC Connection after transaction", ex);
  396 | 		}
  397 | 
  398 | 		if (txObject.isNewConnectionHolder()) {
  399 | 			if (logger.isDebugEnabled()) {
  400 | 				logger.debug("Releasing JDBC Connection [" + con + "] after transaction");
  401 | 			}
  402 | 			DataSourceUtils.releaseConnection(con, this.dataSource);
  403 | 		}
  404 | 
  405 | 		txObject.getConnectionHolder().clear();
  406 | 	}
  407 | 
  408 | 
  409 | 	/**
  410 | 	 * Check the default {@link Connection#isReadOnly()} flag on a freshly
  411 | 	 * obtained connection from the {@code DataSource}, assuming that the
  412 | 	 * same flag applies to all connections obtained from the given setup.
  413 | 	 * @param newCon the Connection to check
  414 | 	 * @since 6.2.13
  415 | 	 * @see #isDefaultReadOnly()
  416 | 	 */
  417 | 	private void checkDefaultReadOnly(Connection newCon) {
  418 | 		if (this.defaultReadOnly == null) {
  419 | 			try {
  420 | 				this.defaultReadOnly = newCon.isReadOnly();
  421 | 			}
  422 | 			catch (Throwable ex) {
  423 | 				logger.debug("Could not determine default JDBC Connection isReadOnly - assuming false", ex);
  424 | 				this.defaultReadOnly = false;
  425 | 			}
  426 | 		}
  427 | 	}
  428 | 
  429 | 	/**
  430 | 	 * Check whether the default read-only flag has been determined as {@code true},
  431 | 	 * assuming that all encountered connections will be read-only by default and
  432 | 	 * therefore do not need explicit {@link Connection#setReadOnly} (re)setting.
  433 | 	 * @since 6.2.13
  434 | 	 * @see #checkDefaultReadOnly(Connection)
  435 | 	 */
  436 | 	private boolean isDefaultReadOnly() {
  437 | 		return (this.defaultReadOnly == Boolean.TRUE);
  438 | 	}
  439 | 
  440 | 	/**
  441 | 	 * Prepare the transactional {@code Connection} right after transaction begin.
  442 | 	 * <p>The default implementation executes a "SET TRANSACTION READ ONLY" statement
  443 | 	 * if the {@link #setEnforceReadOnly "enforceReadOnly"} flag is set to {@code true}
  444 | 	 * and the transaction definition indicates a read-only transaction.
  445 | 	 * <p>The "SET TRANSACTION READ ONLY" is understood by Oracle, MySQL and Postgres
  446 | 	 * and may work with other databases as well. If you'd like to adapt this treatment,
  447 | 	 * override this method accordingly.
  448 | 	 * @param con the transactional JDBC Connection
  449 | 	 * @param definition the current transaction definition
  450 | 	 * @throws SQLException if thrown by JDBC API
  451 | 	 * @since 4.3.7
  452 | 	 * @see #setEnforceReadOnly
  453 | 	 */
  454 | 	protected void prepareTransactionalConnection(Connection con, TransactionDefinition definition)
  455 | 			throws SQLException {
  456 | 
  457 | 		if (isEnforceReadOnly() && definition.isReadOnly()) {
  458 | 			try (Statement stmt = con.createStatement()) {
  459 | 				stmt.executeUpdate("SET TRANSACTION READ ONLY");
  460 | 			}
  461 | 		}
  462 | 	}
  463 | 
  464 | 	/**
  465 | 	 * Translate the given JDBC commit/rollback exception to a common Spring
  466 | 	 * exception to propagate from the {@link #commit}/{@link #rollback} call.
  467 | 	 * <p>The default implementation throws a {@link TransactionSystemException}.
  468 | 	 * Subclasses may specifically identify concurrency failures etc.
  469 | 	 * @param task the task description (commit or rollback)
  470 | 	 * @param ex the SQLException thrown from commit/rollback
  471 | 	 * @return the translated exception to throw, either a
  472 | 	 * {@link org.springframework.dao.DataAccessException} or a
  473 | 	 * {@link org.springframework.transaction.TransactionException}
  474 | 	 * @since 5.3
  475 | 	 */
  476 | 	protected RuntimeException translateException(String task, SQLException ex) {
  477 | 		return new TransactionSystemException(task + " failed", ex);
  478 | 	}
  479 | 
  480 | 
  481 | 	/**
  482 | 	 * DataSource transaction object, representing a ConnectionHolder.
  483 | 	 * Used as transaction object by DataSourceTransactionManager.
  484 | 	 */
  485 | 	private static class DataSourceTransactionObject extends JdbcTransactionObjectSupport {
  486 | 
  487 | 		private boolean newConnectionHolder;
  488 | 
  489 | 		private boolean mustRestoreAutoCommit;
  490 | 
  491 | 		public void setConnectionHolder(@Nullable ConnectionHolder connectionHolder, boolean newConnectionHolder) {
  492 | 			super.setConnectionHolder(connectionHolder);
  493 | 			this.newConnectionHolder = newConnectionHolder;
  494 | 		}
  495 | 
  496 | 		public boolean isNewConnectionHolder() {
  497 | 			return this.newConnectionHolder;
  498 | 		}
  499 | 
  500 | 		public void setMustRestoreAutoCommit(boolean mustRestoreAutoCommit) {
  501 | 			this.mustRestoreAutoCommit = mustRestoreAutoCommit;
  502 | 		}
  503 | 
  504 | 		public boolean isMustRestoreAutoCommit() {
  505 | 			return this.mustRestoreAutoCommit;
  506 | 		}
  507 | 
  508 | 		public void setRollbackOnly() {
  509 | 			getConnectionHolder().setRollbackOnly();
  510 | 		}
  511 | 
  512 | 		@Override
  513 | 		public boolean isRollbackOnly() {
  514 | 			return getConnectionHolder().isRollbackOnly();
  515 | 		}
  516 | 
  517 | 		@Override
  518 | 		public void flush() {
  519 | 			TransactionSynchronizationUtils.triggerFlush();
  520 | 		}
  521 | 	}
  522 | 
  523 | }
```
