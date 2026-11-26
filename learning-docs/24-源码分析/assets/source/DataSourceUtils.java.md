<!-- learning-docs source archive（机械抓取原文，禁止手改代码内容） -->
<!-- origin: org.springframework:spring-jdbc:7.1.0 | path: org/springframework/jdbc/datasource/DataSourceUtils.java -->
<!-- 正文引用：[[源码:DataSourceUtils.java#L起-L止]] ；代码块头部：(src: DataSourceUtils.java#L起-L止) -->

# DataSourceUtils.java

| 项 | 值 |
| --- | --- |
| 来源 | `org.springframework:spring-jdbc:7.1.0` |
| 路径 | `org/springframework/jdbc/datasource/DataSourceUtils.java` |
| 行数 | 580 |
| 在线地址 | https://repo1.maven.org/maven2/org/springframework/spring-jdbc/7.1.0/spring-jdbc-7.1.0-sources.jar |
| 抓取时间 | 2026-11-25T14:03:07.343Z |

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
   25 | import org.apache.commons.logging.Log;
   26 | import org.apache.commons.logging.LogFactory;
   27 | import org.jspecify.annotations.Nullable;
   28 | 
   29 | import org.springframework.jdbc.CannotGetJdbcConnectionException;
   30 | import org.springframework.transaction.TransactionDefinition;
   31 | import org.springframework.transaction.support.TransactionSynchronization;
   32 | import org.springframework.transaction.support.TransactionSynchronizationManager;
   33 | import org.springframework.util.Assert;
   34 | 
   35 | /**
   36 |  * Helper class that provides static methods for obtaining JDBC {@code Connection}s
   37 |  * from a {@link javax.sql.DataSource}. Includes special support for Spring-managed
   38 |  * transactional {@code Connection}s, for example, managed by {@link DataSourceTransactionManager}
   39 |  * or {@link org.springframework.transaction.jta.JtaTransactionManager}.
   40 |  *
   41 |  * <p>Used internally by Spring's {@link org.springframework.jdbc.core.JdbcTemplate},
   42 |  * Spring's JDBC operation objects and the JDBC {@link DataSourceTransactionManager}.
   43 |  * Can also be used directly in application code.
   44 |  *
   45 |  * @author Rod Johnson
   46 |  * @author Juergen Hoeller
   47 |  * @see #getConnection
   48 |  * @see #releaseConnection
   49 |  * @see org.springframework.jdbc.core.JdbcTemplate
   50 |  * @see org.springframework.jdbc.support.JdbcTransactionManager
   51 |  * @see org.springframework.transaction.jta.JtaTransactionManager
   52 |  * @see org.springframework.transaction.support.TransactionSynchronizationManager
   53 |  */
   54 | public abstract class DataSourceUtils {
   55 | 
   56 | 	/**
   57 | 	 * Order value for TransactionSynchronization objects that clean up JDBC Connections.
   58 | 	 */
   59 | 	public static final int CONNECTION_SYNCHRONIZATION_ORDER = 1000;
   60 | 
   61 | 	private static final Log logger = LogFactory.getLog(DataSourceUtils.class);
   62 | 
   63 | 
   64 | 	/**
   65 | 	 * Obtain a Connection from the given DataSource. Translates SQLExceptions into
   66 | 	 * the Spring hierarchy of unchecked generic data access exceptions, simplifying
   67 | 	 * calling code and making any exception that is thrown more meaningful.
   68 | 	 * <p>Is aware of a corresponding Connection bound to the current thread, for example
   69 | 	 * when using {@link DataSourceTransactionManager}. Will bind a Connection to the
   70 | 	 * thread if transaction synchronization is active, for example, when running within a
   71 | 	 * {@link org.springframework.transaction.jta.JtaTransactionManager JTA} transaction).
   72 | 	 * @param dataSource the DataSource to obtain Connections from
   73 | 	 * @return a JDBC Connection from the given DataSource
   74 | 	 * @throws org.springframework.jdbc.CannotGetJdbcConnectionException
   75 | 	 * if the attempt to get a Connection failed
   76 | 	 * @see #releaseConnection(Connection, DataSource)
   77 | 	 * @see #isConnectionTransactional(Connection, DataSource)
   78 | 	 */
   79 | 	public static Connection getConnection(DataSource dataSource) throws CannotGetJdbcConnectionException {
   80 | 		try {
   81 | 			return doGetConnection(dataSource);
   82 | 		}
   83 | 		catch (SQLException ex) {
   84 | 			throw new CannotGetJdbcConnectionException("Failed to obtain JDBC Connection", ex);
   85 | 		}
   86 | 		catch (IllegalStateException ex) {
   87 | 			throw new CannotGetJdbcConnectionException("Failed to obtain JDBC Connection", ex);
   88 | 		}
   89 | 	}
   90 | 
   91 | 	/**
   92 | 	 * Actually obtain a JDBC Connection from the given DataSource.
   93 | 	 * Same as {@link #getConnection}, but throwing the original SQLException.
   94 | 	 * <p>Is aware of a corresponding Connection bound to the current thread, for example
   95 | 	 * when using {@link DataSourceTransactionManager}. Will bind a Connection to the thread
   96 | 	 * if transaction synchronization is active (for example, if in a JTA transaction).
   97 | 	 * <p>Directly accessed by {@link TransactionAwareDataSourceProxy}.
   98 | 	 * @param dataSource the DataSource to obtain Connections from
   99 | 	 * @return a JDBC Connection from the given DataSource
  100 | 	 * @throws SQLException if thrown by JDBC methods
  101 | 	 * @see #doReleaseConnection
  102 | 	 */
  103 | 	public static Connection doGetConnection(DataSource dataSource) throws SQLException {
  104 | 		Assert.notNull(dataSource, "No DataSource specified");
  105 | 
  106 | 		ConnectionHolder conHolder = (ConnectionHolder) TransactionSynchronizationManager.getResource(dataSource);
  107 | 		if (conHolder != null && (conHolder.hasConnection() || conHolder.isSynchronizedWithTransaction())) {
  108 | 			conHolder.requested();
  109 | 			if (!conHolder.hasConnection()) {
  110 | 				logger.debug("Fetching resumed JDBC Connection from DataSource");
  111 | 				conHolder.setConnection(fetchConnection(dataSource));
  112 | 			}
  113 | 			return conHolder.getConnection();
  114 | 		}
  115 | 		// Else we either got no holder or an empty thread-bound holder here.
  116 | 
  117 | 		logger.debug("Fetching JDBC Connection from DataSource");
  118 | 		Connection con = fetchConnection(dataSource);
  119 | 
  120 | 		if (TransactionSynchronizationManager.isSynchronizationActive()) {
  121 | 			try {
  122 | 				// Use same Connection for further JDBC actions within the transaction.
  123 | 				// Thread-bound object will get removed by synchronization at transaction completion.
  124 | 				ConnectionHolder holderToUse = conHolder;
  125 | 				if (holderToUse == null) {
  126 | 					holderToUse = new ConnectionHolder(con);
  127 | 				}
  128 | 				else {
  129 | 					holderToUse.setConnection(con);
  130 | 				}
  131 | 				holderToUse.requested();
  132 | 				TransactionSynchronizationManager.registerSynchronization(
  133 | 						new ConnectionSynchronization(holderToUse, dataSource));
  134 | 				holderToUse.setSynchronizedWithTransaction(true);
  135 | 				if (holderToUse != conHolder) {
  136 | 					TransactionSynchronizationManager.bindResource(dataSource, holderToUse);
  137 | 				}
  138 | 			}
  139 | 			catch (RuntimeException ex) {
  140 | 				// Unexpected exception from external delegation call -> close Connection and rethrow.
  141 | 				releaseConnection(con, dataSource);
  142 | 				throw ex;
  143 | 			}
  144 | 		}
  145 | 
  146 | 		return con;
  147 | 	}
  148 | 
  149 | 	/**
  150 | 	 * Actually fetch a {@link Connection} from the given {@link DataSource},
  151 | 	 * defensively turning an unexpected {@code null} return value from
  152 | 	 * {@link DataSource#getConnection()} into an {@link IllegalStateException}.
  153 | 	 * @param dataSource the DataSource to obtain Connections from
  154 | 	 * @return a JDBC Connection from the given DataSource (never {@code null})
  155 | 	 * @throws SQLException if thrown by JDBC methods
  156 | 	 * @throws IllegalStateException if the DataSource returned a null value
  157 | 	 * @see DataSource#getConnection()
  158 | 	 */
  159 | 	private static Connection fetchConnection(DataSource dataSource) throws SQLException {
  160 | 		Connection con = dataSource.getConnection();
  161 | 		if (con == null) {
  162 | 			throw new IllegalStateException("DataSource returned null from getConnection(): " + dataSource);
  163 | 		}
  164 | 		return con;
  165 | 	}
  166 | 
  167 | 	/**
  168 | 	 * Prepare the given Connection with the given transaction semantics.
  169 | 	 * @param con the Connection to prepare
  170 | 	 * @param definition the transaction definition to apply
  171 | 	 * @return the previous isolation level, if any
  172 | 	 * @throws SQLException if thrown by JDBC methods
  173 | 	 * @see #prepareConnectionForTransaction(Connection, int, boolean)
  174 | 	 */
  175 | 	public static @Nullable Integer prepareConnectionForTransaction(Connection con, @Nullable TransactionDefinition definition)
  176 | 			throws SQLException {
  177 | 
  178 | 		return prepareConnectionForTransaction(con,
  179 | 				(definition != null ? definition.getIsolationLevel() : TransactionDefinition.ISOLATION_DEFAULT),
  180 | 				(definition != null && definition.isReadOnly()));
  181 | 	}
  182 | 
  183 | 	/**
  184 | 	 * Prepare the given Connection with the given transaction semantics.
  185 | 	 * @param con the Connection to prepare
  186 | 	 * @param isolationLevel the isolation level to apply
  187 | 	 * @param setReadOnly whether to set the read-only flag
  188 | 	 * @return the previous isolation level, if any
  189 | 	 * @throws SQLException if thrown by JDBC methods
  190 | 	 * @since 6.2.13
  191 | 	 * @see #resetConnectionAfterTransaction(Connection, Integer, boolean)
  192 | 	 * @see Connection#setTransactionIsolation
  193 | 	 * @see Connection#setReadOnly
  194 | 	 */
  195 | 	static @Nullable Integer prepareConnectionForTransaction(Connection con, int isolationLevel, boolean setReadOnly)
  196 | 			throws SQLException {
  197 | 
  198 | 		Assert.notNull(con, "No Connection specified");
  199 | 
  200 | 		boolean debugEnabled = logger.isDebugEnabled();
  201 | 		// Set read-only flag.
  202 | 		if (setReadOnly) {
  203 | 			if (debugEnabled) {
  204 | 				logger.debug("Setting JDBC Connection [" + con + "] read-only");
  205 | 			}
  206 | 			setReadOnlyIfPossible(con);
  207 | 		}
  208 | 
  209 | 		// Apply specific isolation level, if any.
  210 | 		Integer previousIsolationLevel = null;
  211 | 		if (isolationLevel != TransactionDefinition.ISOLATION_DEFAULT) {
  212 | 			if (debugEnabled) {
  213 | 				logger.debug("Changing isolation level of JDBC Connection [" + con + "] to " + isolationLevel);
  214 | 			}
  215 | 			int currentIsolation = con.getTransactionIsolation();
  216 | 			if (currentIsolation != isolationLevel) {
  217 | 				previousIsolationLevel = currentIsolation;
  218 | 				con.setTransactionIsolation(isolationLevel);
  219 | 			}
  220 | 		}
  221 | 
  222 | 		return previousIsolationLevel;
  223 | 	}
  224 | 
  225 | 	/**
  226 | 	 * Apply the read-only hint to the given Connection,
  227 | 	 * suppressing exceptions other than timeout-related ones.
  228 | 	 * @param con the Connection to prepare
  229 | 	 * @throws SQLException in case of a timeout exception
  230 | 	 * @since 6.2.15
  231 | 	 */
  232 | 	static void setReadOnlyIfPossible(Connection con) throws SQLException {
  233 | 		try {
  234 | 			con.setReadOnly(true);
  235 | 		}
  236 | 		catch (SQLException | RuntimeException ex) {
  237 | 			Throwable exToCheck = ex;
  238 | 			while (exToCheck != null) {
  239 | 				if (exToCheck.getClass().getSimpleName().contains("Timeout")) {
  240 | 					// Assume it's a connection timeout that would otherwise get lost: for example, from JDBC 4.0
  241 | 					throw ex;
  242 | 				}
  243 | 				exToCheck = exToCheck.getCause();
  244 | 			}
  245 | 			// "read-only not supported" SQLException -> ignore, it's just a hint anyway
  246 | 			logger.debug("Could not set JDBC Connection read-only", ex);
  247 | 		}
  248 | 	}
  249 | 
  250 | 	/**
  251 | 	 * Reset the given Connection after a transaction,
  252 | 	 * regarding read-only flag and isolation level.
  253 | 	 * @param con the Connection to reset
  254 | 	 * @param previousIsolationLevel the isolation level to restore, if any
  255 | 	 * @param resetReadOnly whether to reset the connection's read-only flag
  256 | 	 * @since 5.2.1
  257 | 	 * @see #prepareConnectionForTransaction
  258 | 	 * @see Connection#setTransactionIsolation
  259 | 	 * @see Connection#setReadOnly
  260 | 	 */
  261 | 	public static void resetConnectionAfterTransaction(
  262 | 			Connection con, @Nullable Integer previousIsolationLevel, boolean resetReadOnly) {
  263 | 
  264 | 		Assert.notNull(con, "No Connection specified");
  265 | 		boolean debugEnabled = logger.isDebugEnabled();
  266 | 		try {
  267 | 			// Reset transaction isolation to previous value, if changed for the transaction.
  268 | 			if (previousIsolationLevel != null) {
  269 | 				if (debugEnabled) {
  270 | 					logger.debug("Resetting isolation level of JDBC Connection [" +
  271 | 							con + "] to " + previousIsolationLevel);
  272 | 				}
  273 | 				con.setTransactionIsolation(previousIsolationLevel);
  274 | 			}
  275 | 
  276 | 			// Reset read-only flag if we originally switched it to true on transaction begin.
  277 | 			if (resetReadOnly) {
  278 | 				if (debugEnabled) {
  279 | 					logger.debug("Resetting read-only flag of JDBC Connection [" + con + "]");
  280 | 				}
  281 | 				con.setReadOnly(false);
  282 | 			}
  283 | 		}
  284 | 		catch (Throwable ex) {
  285 | 			logger.debug("Could not reset JDBC Connection after transaction", ex);
  286 | 		}
  287 | 	}
  288 | 
  289 | 	/**
  290 | 	 * Reset the given Connection after a transaction,
  291 | 	 * regarding read-only flag and isolation level.
  292 | 	 * @param con the Connection to reset
  293 | 	 * @param previousIsolationLevel the isolation level to restore, if any
  294 | 	 * @deprecated in favor of {@link #resetConnectionAfterTransaction(Connection, Integer, boolean)}
  295 | 	 */
  296 | 	@Deprecated(since = "5.1.11")
  297 | 	public static void resetConnectionAfterTransaction(Connection con, @Nullable Integer previousIsolationLevel) {
  298 | 		Assert.notNull(con, "No Connection specified");
  299 | 		try {
  300 | 			// Reset transaction isolation to previous value, if changed for the transaction.
  301 | 			if (previousIsolationLevel != null) {
  302 | 				if (logger.isDebugEnabled()) {
  303 | 					logger.debug("Resetting isolation level of JDBC Connection [" +
  304 | 							con + "] to " + previousIsolationLevel);
  305 | 				}
  306 | 				con.setTransactionIsolation(previousIsolationLevel);
  307 | 			}
  308 | 
  309 | 			// Reset read-only flag.
  310 | 			if (con.isReadOnly()) {
  311 | 				if (logger.isDebugEnabled()) {
  312 | 					logger.debug("Resetting read-only flag of JDBC Connection [" + con + "]");
  313 | 				}
  314 | 				con.setReadOnly(false);
  315 | 			}
  316 | 		}
  317 | 		catch (Throwable ex) {
  318 | 			logger.debug("Could not reset JDBC Connection after transaction", ex);
  319 | 		}
  320 | 	}
  321 | 
  322 | 	/**
  323 | 	 * Determine whether the given JDBC Connection is transactional, that is,
  324 | 	 * bound to the current thread by Spring's transaction facilities.
  325 | 	 * @param con the Connection to check
  326 | 	 * @param dataSource the DataSource that the Connection was obtained from
  327 | 	 * (may be {@code null})
  328 | 	 * @return whether the Connection is transactional
  329 | 	 * @see #getConnection(DataSource)
  330 | 	 */
  331 | 	public static boolean isConnectionTransactional(Connection con, @Nullable DataSource dataSource) {
  332 | 		if (dataSource == null) {
  333 | 			return false;
  334 | 		}
  335 | 		ConnectionHolder conHolder = (ConnectionHolder) TransactionSynchronizationManager.getResource(dataSource);
  336 | 		return (conHolder != null && connectionEquals(conHolder, con));
  337 | 	}
  338 | 
  339 | 	/**
  340 | 	 * Apply the current transaction timeout, if any, to the given JDBC Statement object.
  341 | 	 * @param stmt the JDBC Statement object
  342 | 	 * @param dataSource the DataSource that the Connection was obtained from
  343 | 	 * @throws SQLException if thrown by JDBC methods
  344 | 	 * @see java.sql.Statement#setQueryTimeout
  345 | 	 */
  346 | 	public static void applyTransactionTimeout(Statement stmt, @Nullable DataSource dataSource) throws SQLException {
  347 | 		applyTimeout(stmt, dataSource, -1);
  348 | 	}
  349 | 
  350 | 	/**
  351 | 	 * Apply the specified timeout - overridden by the current transaction timeout,
  352 | 	 * if any - to the given JDBC Statement object.
  353 | 	 * @param stmt the JDBC Statement object
  354 | 	 * @param dataSource the DataSource that the Connection was obtained from
  355 | 	 * @param timeout the timeout to apply (or 0 for no timeout outside of a transaction)
  356 | 	 * @throws SQLException if thrown by JDBC methods
  357 | 	 * @see java.sql.Statement#setQueryTimeout
  358 | 	 */
  359 | 	public static void applyTimeout(Statement stmt, @Nullable DataSource dataSource, int timeout) throws SQLException {
  360 | 		Assert.notNull(stmt, "No Statement specified");
  361 | 		ConnectionHolder holder = null;
  362 | 		if (dataSource != null) {
  363 | 			holder = (ConnectionHolder) TransactionSynchronizationManager.getResource(dataSource);
  364 | 		}
  365 | 		if (holder != null && holder.hasTimeout()) {
  366 | 			// Remaining transaction timeout overrides specified value.
  367 | 			stmt.setQueryTimeout(holder.getTimeToLiveInSeconds());
  368 | 		}
  369 | 		else if (timeout >= 0) {
  370 | 			// No current transaction timeout -> apply specified value.
  371 | 			stmt.setQueryTimeout(timeout);
  372 | 		}
  373 | 	}
  374 | 
  375 | 	/**
  376 | 	 * Close the given Connection, obtained from the given DataSource,
  377 | 	 * if it is not managed externally (that is, not bound to the thread).
  378 | 	 * @param con the Connection to close if necessary
  379 | 	 * (if this is {@code null}, the call will be ignored)
  380 | 	 * @param dataSource the DataSource that the Connection was obtained from
  381 | 	 * (may be {@code null})
  382 | 	 * @see #getConnection
  383 | 	 */
  384 | 	public static void releaseConnection(@Nullable Connection con, @Nullable DataSource dataSource) {
  385 | 		try {
  386 | 			doReleaseConnection(con, dataSource);
  387 | 		}
  388 | 		catch (SQLException ex) {
  389 | 			logger.debug("Could not close JDBC Connection", ex);
  390 | 		}
  391 | 		catch (Throwable ex) {
  392 | 			logger.debug("Unexpected exception on closing JDBC Connection", ex);
  393 | 		}
  394 | 	}
  395 | 
  396 | 	/**
  397 | 	 * Actually close the given Connection, obtained from the given DataSource.
  398 | 	 * Same as {@link #releaseConnection}, but throwing the original SQLException.
  399 | 	 * <p>Directly accessed by {@link TransactionAwareDataSourceProxy}.
  400 | 	 * @param con the Connection to close if necessary
  401 | 	 * (if this is {@code null}, the call will be ignored)
  402 | 	 * @param dataSource the DataSource that the Connection was obtained from
  403 | 	 * (may be {@code null})
  404 | 	 * @throws SQLException if thrown by JDBC methods
  405 | 	 * @see #doGetConnection
  406 | 	 */
  407 | 	public static void doReleaseConnection(@Nullable Connection con, @Nullable DataSource dataSource) throws SQLException {
  408 | 		if (con == null) {
  409 | 			return;
  410 | 		}
  411 | 		if (dataSource != null) {
  412 | 			ConnectionHolder conHolder = (ConnectionHolder) TransactionSynchronizationManager.getResource(dataSource);
  413 | 			if (conHolder != null && connectionEquals(conHolder, con)) {
  414 | 				// It's the transactional Connection: Don't close it.
  415 | 				conHolder.released();
  416 | 				return;
  417 | 			}
  418 | 		}
  419 | 		doCloseConnection(con, dataSource);
  420 | 	}
  421 | 
  422 | 	/**
  423 | 	 * Close the Connection, unless a {@link SmartDataSource} doesn't want us to.
  424 | 	 * @param con the Connection to close if necessary
  425 | 	 * @param dataSource the DataSource that the Connection was obtained from
  426 | 	 * @throws SQLException if thrown by JDBC methods
  427 | 	 * @see Connection#close()
  428 | 	 * @see SmartDataSource#shouldClose(Connection)
  429 | 	 */
  430 | 	public static void doCloseConnection(Connection con, @Nullable DataSource dataSource) throws SQLException {
  431 | 		if (!(dataSource instanceof SmartDataSource smartDataSource) || smartDataSource.shouldClose(con)) {
  432 | 			con.close();
  433 | 		}
  434 | 	}
  435 | 
  436 | 	/**
  437 | 	 * Determine whether the given two Connections are equal, asking the target
  438 | 	 * Connection in case of a proxy. Used to detect equality even if the
  439 | 	 * user passed in a raw target Connection while the held one is a proxy.
  440 | 	 * @param conHolder the ConnectionHolder for the held Connection (potentially a proxy)
  441 | 	 * @param passedInCon the Connection passed-in by the user
  442 | 	 * (potentially a target Connection without proxy)
  443 | 	 * @return whether the given Connections are equal
  444 | 	 * @see #getTargetConnection
  445 | 	 */
  446 | 	private static boolean connectionEquals(ConnectionHolder conHolder, Connection passedInCon) {
  447 | 		if (!conHolder.hasConnection()) {
  448 | 			return false;
  449 | 		}
  450 | 		Connection heldCon = conHolder.getConnection();
  451 | 		// Explicitly check for identity too: for Connection handles that do not implement
  452 | 		// "equals" properly, such as the ones Commons DBCP exposes).
  453 | 		return (heldCon == passedInCon || heldCon.equals(passedInCon) ||
  454 | 				getTargetConnection(heldCon).equals(passedInCon));
  455 | 	}
  456 | 
  457 | 	/**
  458 | 	 * Return the innermost target Connection of the given Connection. If the given
  459 | 	 * Connection is a proxy, it will be unwrapped until a non-proxy Connection is
  460 | 	 * found. Otherwise, the passed-in Connection will be returned as-is.
  461 | 	 * @param con the Connection proxy to unwrap
  462 | 	 * @return the innermost target Connection, or the passed-in one if no proxy
  463 | 	 * @see ConnectionProxy#getTargetConnection()
  464 | 	 */
  465 | 	public static Connection getTargetConnection(Connection con) {
  466 | 		Connection conToUse = con;
  467 | 		while (conToUse instanceof ConnectionProxy connectionProxy) {
  468 | 			Connection targetCon = connectionProxy.getTargetConnection();
  469 | 			if (targetCon == conToUse) {
  470 | 				break;
  471 | 			}
  472 | 			conToUse = targetCon;
  473 | 		}
  474 | 		return conToUse;
  475 | 	}
  476 | 
  477 | 	/**
  478 | 	 * Determine the connection synchronization order to use for the given
  479 | 	 * DataSource. Decreased for every level of nesting that a DataSource
  480 | 	 * has, checked through the level of DelegatingDataSource nesting.
  481 | 	 * @param dataSource the DataSource to check
  482 | 	 * @return the connection synchronization order to use
  483 | 	 * @see #CONNECTION_SYNCHRONIZATION_ORDER
  484 | 	 */
  485 | 	private static int getConnectionSynchronizationOrder(DataSource dataSource) {
  486 | 		int order = CONNECTION_SYNCHRONIZATION_ORDER;
  487 | 		DataSource currDs = dataSource;
  488 | 		while (currDs instanceof DelegatingDataSource delegatingDataSource) {
  489 | 			order--;
  490 | 			currDs = delegatingDataSource.getTargetDataSource();
  491 | 		}
  492 | 		return order;
  493 | 	}
  494 | 
  495 | 
  496 | 	/**
  497 | 	 * Callback for resource cleanup at the end of a non-native JDBC transaction
  498 | 	 * (for example, when participating in a JtaTransactionManager transaction).
  499 | 	 * @see org.springframework.transaction.jta.JtaTransactionManager
  500 | 	 */
  501 | 	private static class ConnectionSynchronization implements TransactionSynchronization {
  502 | 
  503 | 		private final ConnectionHolder connectionHolder;
  504 | 
  505 | 		private final DataSource dataSource;
  506 | 
  507 | 		private final int order;
  508 | 
  509 | 		private boolean holderActive = true;
  510 | 
  511 | 		public ConnectionSynchronization(ConnectionHolder connectionHolder, DataSource dataSource) {
  512 | 			this.connectionHolder = connectionHolder;
  513 | 			this.dataSource = dataSource;
  514 | 			this.order = getConnectionSynchronizationOrder(dataSource);
  515 | 		}
  516 | 
  517 | 		@Override
  518 | 		public int getOrder() {
  519 | 			return this.order;
  520 | 		}
  521 | 
  522 | 		@Override
  523 | 		public void suspend() {
  524 | 			if (this.holderActive) {
  525 | 				TransactionSynchronizationManager.unbindResource(this.dataSource);
  526 | 				if (this.connectionHolder.hasConnection() && !this.connectionHolder.isOpen()) {
  527 | 					// Release Connection on suspend if the application doesn't keep
  528 | 					// a handle to it anymore. We will fetch a fresh Connection if the
  529 | 					// application accesses the ConnectionHolder again after resume,
  530 | 					// assuming that it will participate in the same transaction.
  531 | 					releaseConnection(this.connectionHolder.getConnection(), this.dataSource);
  532 | 					this.connectionHolder.setConnection(null);
  533 | 				}
  534 | 			}
  535 | 		}
  536 | 
  537 | 		@Override
  538 | 		public void resume() {
  539 | 			if (this.holderActive) {
  540 | 				TransactionSynchronizationManager.bindResource(this.dataSource, this.connectionHolder);
  541 | 			}
  542 | 		}
  543 | 
  544 | 		@Override
  545 | 		public void beforeCompletion() {
  546 | 			// Release Connection early if the holder is not open anymore
  547 | 			// (that is, not used by another resource like a Hibernate Session
  548 | 			// that has its own cleanup via transaction synchronization),
  549 | 			// to avoid issues with strict JTA implementations that expect
  550 | 			// the close call before transaction completion.
  551 | 			if (!this.connectionHolder.isOpen()) {
  552 | 				TransactionSynchronizationManager.unbindResource(this.dataSource);
  553 | 				this.holderActive = false;
  554 | 				if (this.connectionHolder.hasConnection()) {
  555 | 					releaseConnection(this.connectionHolder.getConnection(), this.dataSource);
  556 | 				}
  557 | 			}
  558 | 		}
  559 | 
  560 | 		@Override
  561 | 		public void afterCompletion(int status) {
  562 | 			// If we haven't closed the Connection in beforeCompletion,
  563 | 			// close it now. The holder might have been used for other
  564 | 			// cleanup in the meantime, for example by a Hibernate Session.
  565 | 			if (this.holderActive) {
  566 | 				// The thread-bound ConnectionHolder might not be available anymore,
  567 | 				// since afterCompletion might get called from a different thread.
  568 | 				TransactionSynchronizationManager.unbindResourceIfPossible(this.dataSource);
  569 | 				this.holderActive = false;
  570 | 				if (this.connectionHolder.hasConnection()) {
  571 | 					releaseConnection(this.connectionHolder.getConnection(), this.dataSource);
  572 | 					// Reset the ConnectionHolder: It might remain bound to the thread.
  573 | 					this.connectionHolder.setConnection(null);
  574 | 				}
  575 | 			}
  576 | 			this.connectionHolder.reset();
  577 | 		}
  578 | 	}
  579 | 
  580 | }
```
