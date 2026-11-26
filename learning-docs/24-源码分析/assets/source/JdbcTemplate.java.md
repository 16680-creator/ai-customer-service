<!-- learning-docs source archive（机械抓取原文，禁止手改代码内容） -->
<!-- origin: org.springframework:spring-jdbc:7.1.0 | path: org/springframework/jdbc/core/JdbcTemplate.java -->
<!-- 正文引用：[[源码:JdbcTemplate.java#L起-L止]] ；代码块头部：(src: JdbcTemplate.java#L起-L止) -->

# JdbcTemplate.java

| 项 | 值 |
| --- | --- |
| 来源 | `org.springframework:spring-jdbc:7.1.0` |
| 路径 | `org/springframework/jdbc/core/JdbcTemplate.java` |
| 行数 | 1826 |
| 在线地址 | https://repo1.maven.org/maven2/org/springframework/spring-jdbc/7.1.0/spring-jdbc-7.1.0-sources.jar |
| 抓取时间 | 2026-11-25T14:01:04.725Z |

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
   17 | package org.springframework.jdbc.core;
   18 | 
   19 | import java.lang.reflect.InvocationHandler;
   20 | import java.lang.reflect.InvocationTargetException;
   21 | import java.lang.reflect.Method;
   22 | import java.lang.reflect.Proxy;
   23 | import java.sql.BatchUpdateException;
   24 | import java.sql.CallableStatement;
   25 | import java.sql.Connection;
   26 | import java.sql.PreparedStatement;
   27 | import java.sql.ResultSet;
   28 | import java.sql.SQLException;
   29 | import java.sql.SQLWarning;
   30 | import java.sql.Statement;
   31 | import java.util.ArrayList;
   32 | import java.util.Collection;
   33 | import java.util.Collections;
   34 | import java.util.LinkedHashMap;
   35 | import java.util.List;
   36 | import java.util.Map;
   37 | import java.util.Spliterator;
   38 | import java.util.function.Consumer;
   39 | import java.util.stream.Stream;
   40 | import java.util.stream.StreamSupport;
   41 | 
   42 | import javax.sql.DataSource;
   43 | 
   44 | import org.jspecify.annotations.Nullable;
   45 | 
   46 | import org.springframework.dao.DataAccessException;
   47 | import org.springframework.dao.InvalidDataAccessApiUsageException;
   48 | import org.springframework.dao.support.DataAccessUtils;
   49 | import org.springframework.jdbc.InvalidResultSetAccessException;
   50 | import org.springframework.jdbc.SQLWarningException;
   51 | import org.springframework.jdbc.UncategorizedSQLException;
   52 | import org.springframework.jdbc.datasource.ConnectionProxy;
   53 | import org.springframework.jdbc.datasource.DataSourceUtils;
   54 | import org.springframework.jdbc.support.JdbcAccessor;
   55 | import org.springframework.jdbc.support.JdbcUtils;
   56 | import org.springframework.jdbc.support.KeyHolder;
   57 | import org.springframework.jdbc.support.rowset.SqlRowSet;
   58 | import org.springframework.util.Assert;
   59 | import org.springframework.util.CollectionUtils;
   60 | import org.springframework.util.LinkedCaseInsensitiveMap;
   61 | 
   62 | /**
   63 |  * <b>This is the central delegate in the JDBC core package.</b>
   64 |  * It can be used directly for many data access purposes, supporting any kind
   65 |  * of JDBC operation. For a more focused and convenient facade on top of this,
   66 |  * consider {@link org.springframework.jdbc.core.simple.JdbcClient} as of 6.1.
   67 |  *
   68 |  * <p>This class simplifies the use of JDBC and helps to avoid common errors.
   69 |  * It executes core JDBC workflow, leaving application code to provide SQL
   70 |  * and extract results. This class executes SQL queries or updates, initiating
   71 |  * iteration over ResultSets and catching JDBC exceptions and translating
   72 |  * them to the common {@code org.springframework.dao} exception hierarchy.
   73 |  *
   74 |  * <p>Code using this class need only implement callback interfaces, giving
   75 |  * them a clearly defined contract. The {@link PreparedStatementCreator} callback
   76 |  * interface creates a prepared statement given a Connection, providing SQL and
   77 |  * any necessary parameters. The {@link ResultSetExtractor} interface extracts
   78 |  * values from a ResultSet. See also {@link PreparedStatementSetter} and
   79 |  * {@link RowMapper} for two popular alternative callback interfaces.
   80 |  *
   81 |  * <p>An instance of this template class is thread-safe once configured.
   82 |  * Can be used within a service implementation via direct instantiation
   83 |  * with a DataSource reference, or get prepared in an application context
   84 |  * and given to services as bean reference. Note: The DataSource should
   85 |  * always be configured as a bean in the application context, in the first case
   86 |  * given to the service directly, in the second case to the prepared template.
   87 |  *
   88 |  * <p>Because this class is parameterizable by the callback interfaces and
   89 |  * the {@link org.springframework.jdbc.support.SQLExceptionTranslator}
   90 |  * interface, there should be no need to subclass it.
   91 |  *
   92 |  * <p>All SQL operations performed by this class are logged at debug level,
   93 |  * using "org.springframework.jdbc.core.JdbcTemplate" as log category.
   94 |  *
   95 |  * <p><b>NOTE: As of 6.1, there is a unified JDBC access facade available in
   96 |  * the form of {@link org.springframework.jdbc.core.simple.JdbcClient}.</b>
   97 |  * {@code JdbcClient} provides a fluent API style for common JDBC queries/updates
   98 |  * with flexible use of indexed or named parameters. It delegates to a
   99 |  * {@code JdbcTemplate}/{@code NamedParameterJdbcTemplate} for actual execution.
  100 |  *
  101 |  * @author Rod Johnson
  102 |  * @author Juergen Hoeller
  103 |  * @author Thomas Risberg
  104 |  * @author Yanming Zhou
  105 |  * @since May 3, 2001
  106 |  * @see JdbcOperations
  107 |  * @see PreparedStatementCreator
  108 |  * @see PreparedStatementSetter
  109 |  * @see CallableStatementCreator
  110 |  * @see PreparedStatementCallback
  111 |  * @see CallableStatementCallback
  112 |  * @see ResultSetExtractor
  113 |  * @see RowCallbackHandler
  114 |  * @see RowMapper
  115 |  * @see org.springframework.jdbc.support.SQLExceptionTranslator
  116 |  * @see org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate
  117 |  */
  118 | public class JdbcTemplate extends JdbcAccessor implements JdbcOperations {
  119 | 
  120 | 	private static final String RETURN_RESULT_SET_PREFIX = "#result-set-";
  121 | 
  122 | 	private static final String RETURN_UPDATE_COUNT_PREFIX = "#update-count-";
  123 | 
  124 | 
  125 | 	/** If this variable is {@code false}, we will throw exceptions on SQL warnings. */
  126 | 	private boolean ignoreWarnings = true;
  127 | 
  128 | 	/**
  129 | 	 * If this variable is set to a non-negative value, it will be used for setting the
  130 | 	 * fetchSize property on statements used for query processing.
  131 | 	 */
  132 | 	private int fetchSize = -1;
  133 | 
  134 | 	/**
  135 | 	 * If this variable is set to a non-negative value, it will be used for setting the
  136 | 	 * maxRows property on statements used for query processing.
  137 | 	 */
  138 | 	private int maxRows = -1;
  139 | 
  140 | 	/**
  141 | 	 * If this variable is set to a non-negative value, it will be used for setting the
  142 | 	 * queryTimeout property on statements used for query processing.
  143 | 	 */
  144 | 	private int queryTimeout = -1;
  145 | 
  146 | 	/**
  147 | 	 * If this variable is set to true, then all results checking will be bypassed for any
  148 | 	 * callable statement processing. This can be used to avoid a bug in some older Oracle
  149 | 	 * JDBC drivers like 10.1.0.2.
  150 | 	 */
  151 | 	private boolean skipResultsProcessing = false;
  152 | 
  153 | 	/**
  154 | 	 * If this variable is set to true then all results from a stored procedure call
  155 | 	 * that don't have a corresponding SqlOutParameter declaration will be bypassed.
  156 | 	 * All other results processing will be take place unless the variable
  157 | 	 * {@code skipResultsProcessing} is set to {@code true}.
  158 | 	 */
  159 | 	private boolean skipUndeclaredResults = false;
  160 | 
  161 | 	/**
  162 | 	 * If this variable is set to true then execution of a CallableStatement will return
  163 | 	 * the results in a Map that uses case-insensitive names for the parameters.
  164 | 	 */
  165 | 	private boolean resultsMapCaseInsensitive = false;
  166 | 
  167 | 
  168 | 	/**
  169 | 	 * Construct a new JdbcTemplate for bean usage.
  170 | 	 * <p>Note: The DataSource has to be set before using the instance.
  171 | 	 * @see #setDataSource
  172 | 	 */
  173 | 	public JdbcTemplate() {
  174 | 	}
  175 | 
  176 | 	/**
  177 | 	 * Construct a new JdbcTemplate, given a DataSource to obtain connections from.
  178 | 	 * <p>Note: This will not trigger initialization of the exception translator.
  179 | 	 * @param dataSource the JDBC DataSource to obtain connections from
  180 | 	 */
  181 | 	public JdbcTemplate(DataSource dataSource) {
  182 | 		setDataSource(dataSource);
  183 | 		afterPropertiesSet();
  184 | 	}
  185 | 
  186 | 	/**
  187 | 	 * Construct a new JdbcTemplate, given a DataSource to obtain connections from.
  188 | 	 * <p>Note: Depending on the "lazyInit" flag, initialization of the exception translator
  189 | 	 * will be triggered.
  190 | 	 * @param dataSource the JDBC DataSource to obtain connections from
  191 | 	 * @param lazyInit whether to lazily initialize the SQLExceptionTranslator
  192 | 	 */
  193 | 	public JdbcTemplate(DataSource dataSource, boolean lazyInit) {
  194 | 		setDataSource(dataSource);
  195 | 		setLazyInit(lazyInit);
  196 | 		afterPropertiesSet();
  197 | 	}
  198 | 
  199 | 	/**
  200 | 	 * Copy constructor for a derived JdbcTemplate.
  201 | 	 * @param original the original template to copy from
  202 | 	 * @since 7.0
  203 | 	 */
  204 | 	public JdbcTemplate(JdbcAccessor original) {
  205 | 		setDataSource(original.getDataSource());
  206 | 		setExceptionTranslator(original.getExceptionTranslator());
  207 | 		setLazyInit(original.isLazyInit());
  208 | 		if (original instanceof JdbcTemplate originalTemplate) {
  209 | 			setIgnoreWarnings(originalTemplate.isIgnoreWarnings());
  210 | 			setFetchSize(originalTemplate.getFetchSize());
  211 | 			setMaxRows(originalTemplate.getMaxRows());
  212 | 			setQueryTimeout(originalTemplate.getQueryTimeout());
  213 | 			setSkipResultsProcessing(originalTemplate.isSkipResultsProcessing());
  214 | 			setSkipUndeclaredResults(originalTemplate.isSkipUndeclaredResults());
  215 | 			setResultsMapCaseInsensitive(originalTemplate.isResultsMapCaseInsensitive());
  216 | 		}
  217 | 	}
  218 | 
  219 | 
  220 | 	/**
  221 | 	 * Set whether we want to ignore JDBC statement warnings ({@link SQLWarning}).
  222 | 	 * <p>Default is {@code true}, swallowing and logging all warnings. Switch this flag to
  223 | 	 * {@code false} to make this JdbcTemplate throw a {@link SQLWarningException} instead
  224 | 	 * (or chain the {@link SQLWarning} into the primary {@link SQLException}, if any).
  225 | 	 * @see Statement#getWarnings()
  226 | 	 * @see java.sql.SQLWarning
  227 | 	 * @see org.springframework.jdbc.SQLWarningException
  228 | 	 * @see #handleWarnings(Statement)
  229 | 	 */
  230 | 	public void setIgnoreWarnings(boolean ignoreWarnings) {
  231 | 		this.ignoreWarnings = ignoreWarnings;
  232 | 	}
  233 | 
  234 | 	/**
  235 | 	 * Return whether we ignore SQLWarnings.
  236 | 	 */
  237 | 	public boolean isIgnoreWarnings() {
  238 | 		return this.ignoreWarnings;
  239 | 	}
  240 | 
  241 | 	/**
  242 | 	 * Set the fetch size for this JdbcTemplate. This is important for processing large
  243 | 	 * result sets: Setting this higher than the default value will increase processing
  244 | 	 * speed at the cost of memory consumption; setting this lower can avoid transferring
  245 | 	 * row data that will never be read by the application.
  246 | 	 * <p>Default is -1, indicating to use the JDBC driver's default configuration
  247 | 	 * (i.e. to not pass a specific fetch size setting on to the driver).
  248 | 	 * <p>Note: As of 4.3, negative values other than -1 will get passed on to the driver,
  249 | 	 * since, for example, MySQL supports special behavior for {@code Integer.MIN_VALUE}.
  250 | 	 * @see java.sql.Statement#setFetchSize
  251 | 	 */
  252 | 	public void setFetchSize(int fetchSize) {
  253 | 		this.fetchSize = fetchSize;
  254 | 	}
  255 | 
  256 | 	/**
  257 | 	 * Return the fetch size specified for this JdbcTemplate.
  258 | 	 */
  259 | 	public int getFetchSize() {
  260 | 		return this.fetchSize;
  261 | 	}
  262 | 
  263 | 	/**
  264 | 	 * Set the maximum number of rows for this JdbcTemplate. This is important for
  265 | 	 * processing subsets of large result sets, avoiding to read and hold the entire
  266 | 	 * result set in the database or in the JDBC driver if we're never interested in
  267 | 	 * the entire result in the first place (for example, when performing searches
  268 | 	 * that might return a large number of matches).
  269 | 	 * <p>Default is -1, indicating to use the JDBC driver's default configuration
  270 | 	 * (i.e. to not pass a specific max rows setting on to the driver).
  271 | 	 * <p>Note: As of 4.3, negative values other than -1 will get passed on to the
  272 | 	 * driver, in sync with {@link #setFetchSize}'s support for special MySQL values.
  273 | 	 * @see java.sql.Statement#setMaxRows
  274 | 	 */
  275 | 	public void setMaxRows(int maxRows) {
  276 | 		this.maxRows = maxRows;
  277 | 	}
  278 | 
  279 | 	/**
  280 | 	 * Return the maximum number of rows specified for this JdbcTemplate.
  281 | 	 */
  282 | 	public int getMaxRows() {
  283 | 		return this.maxRows;
  284 | 	}
  285 | 
  286 | 	/**
  287 | 	 * Set the query timeout (seconds) for statements that this JdbcTemplate executes.
  288 | 	 * <p>Default is -1, indicating to use the JDBC driver's default
  289 | 	 * (i.e. to not pass a specific query timeout setting on the driver).
  290 | 	 * <p>Note: Any timeout specified here will be overridden by the remaining
  291 | 	 * transaction timeout when executing within a transaction that has a
  292 | 	 * timeout specified at the transaction level.
  293 | 	 * @see java.sql.Statement#setQueryTimeout
  294 | 	 */
  295 | 	public void setQueryTimeout(int queryTimeout) {
  296 | 		this.queryTimeout = queryTimeout;
  297 | 	}
  298 | 
  299 | 	/**
  300 | 	 * Return the query timeout (seconds) for statements that this JdbcTemplate executes.
  301 | 	 */
  302 | 	public int getQueryTimeout() {
  303 | 		return this.queryTimeout;
  304 | 	}
  305 | 
  306 | 	/**
  307 | 	 * Set whether results processing should be skipped. Can be used to optimize callable
  308 | 	 * statement processing when we know that no results are being passed back - the processing
  309 | 	 * of out parameter will still take place. This can be used to avoid a bug in some older
  310 | 	 * Oracle JDBC drivers like 10.1.0.2.
  311 | 	 */
  312 | 	public void setSkipResultsProcessing(boolean skipResultsProcessing) {
  313 | 		this.skipResultsProcessing = skipResultsProcessing;
  314 | 	}
  315 | 
  316 | 	/**
  317 | 	 * Return whether results processing should be skipped.
  318 | 	 */
  319 | 	public boolean isSkipResultsProcessing() {
  320 | 		return this.skipResultsProcessing;
  321 | 	}
  322 | 
  323 | 	/**
  324 | 	 * Set whether undeclared results should be skipped.
  325 | 	 */
  326 | 	public void setSkipUndeclaredResults(boolean skipUndeclaredResults) {
  327 | 		this.skipUndeclaredResults = skipUndeclaredResults;
  328 | 	}
  329 | 
  330 | 	/**
  331 | 	 * Return whether undeclared results should be skipped.
  332 | 	 */
  333 | 	public boolean isSkipUndeclaredResults() {
  334 | 		return this.skipUndeclaredResults;
  335 | 	}
  336 | 
  337 | 	/**
  338 | 	 * Set whether execution of a CallableStatement will return the results in a Map
  339 | 	 * that uses case-insensitive names for the parameters.
  340 | 	 */
  341 | 	public void setResultsMapCaseInsensitive(boolean resultsMapCaseInsensitive) {
  342 | 		this.resultsMapCaseInsensitive = resultsMapCaseInsensitive;
  343 | 	}
  344 | 
  345 | 	/**
  346 | 	 * Return whether execution of a CallableStatement will return the results in a Map
  347 | 	 * that uses case-insensitive names for the parameters.
  348 | 	 */
  349 | 	public boolean isResultsMapCaseInsensitive() {
  350 | 		return this.resultsMapCaseInsensitive;
  351 | 	}
  352 | 
  353 | 
  354 | 	//-------------------------------------------------------------------------
  355 | 	// Methods dealing with a plain java.sql.Connection
  356 | 	//-------------------------------------------------------------------------
  357 | 
  358 | 	@Override
  359 | 	public <T extends @Nullable Object> T execute(ConnectionCallback<T> action) throws DataAccessException {
  360 | 		Assert.notNull(action, "Callback object must not be null");
  361 | 
  362 | 		Connection con = DataSourceUtils.getConnection(obtainDataSource());
  363 | 		try {
  364 | 			// Create close-suppressing Connection proxy, also preparing returned Statements.
  365 | 			Connection conToUse = createConnectionProxy(con);
  366 | 			return action.doInConnection(conToUse);
  367 | 		}
  368 | 		catch (SQLException ex) {
  369 | 			// Release Connection early, to avoid potential connection pool deadlock
  370 | 			// in the case when the exception translator hasn't been initialized yet.
  371 | 			String sql = getSql(action);
  372 | 			DataSourceUtils.releaseConnection(con, getDataSource());
  373 | 			con = null;
  374 | 			throw translateException("ConnectionCallback", sql, ex);
  375 | 		}
  376 | 		finally {
  377 | 			DataSourceUtils.releaseConnection(con, getDataSource());
  378 | 		}
  379 | 	}
  380 | 
  381 | 	/**
  382 | 	 * Create a close-suppressing proxy for the given JDBC Connection.
  383 | 	 * Called by the {@code execute} method.
  384 | 	 * <p>The proxy also prepares returned JDBC Statements, applying
  385 | 	 * statement settings such as fetch size, max rows, and query timeout.
  386 | 	 * @param con the JDBC Connection to create a proxy for
  387 | 	 * @return the Connection proxy
  388 | 	 * @see java.sql.Connection#close()
  389 | 	 * @see #execute(ConnectionCallback)
  390 | 	 * @see #applyStatementSettings
  391 | 	 */
  392 | 	protected Connection createConnectionProxy(Connection con) {
  393 | 		return (Connection) Proxy.newProxyInstance(
  394 | 				ConnectionProxy.class.getClassLoader(),
  395 | 				new Class<?>[] {ConnectionProxy.class},
  396 | 				new CloseSuppressingInvocationHandler(con));
  397 | 	}
  398 | 
  399 | 
  400 | 	//-------------------------------------------------------------------------
  401 | 	// Methods dealing with static SQL (java.sql.Statement)
  402 | 	//-------------------------------------------------------------------------
  403 | 
  404 | 	private <T extends @Nullable Object> T execute(StatementCallback<T> action, boolean closeResources) throws DataAccessException {
  405 | 		Assert.notNull(action, "Callback object must not be null");
  406 | 
  407 | 		Connection con = DataSourceUtils.getConnection(obtainDataSource());
  408 | 		Statement stmt = null;
  409 | 		try {
  410 | 			stmt = con.createStatement();
  411 | 			applyStatementSettings(stmt);
  412 | 			T result = action.doInStatement(stmt);
  413 | 			handleWarnings(stmt);
  414 | 			return result;
  415 | 		}
  416 | 		catch (SQLException ex) {
  417 | 			// Release Connection early, to avoid potential connection pool deadlock
  418 | 			// in the case when the exception translator hasn't been initialized yet.
  419 | 			if (stmt != null) {
  420 | 				handleWarnings(stmt, ex);
  421 | 			}
  422 | 			String sql = getSql(action);
  423 | 			JdbcUtils.closeStatement(stmt);
  424 | 			stmt = null;
  425 | 			DataSourceUtils.releaseConnection(con, getDataSource());
  426 | 			con = null;
  427 | 			throw translateException("StatementCallback", sql, ex);
  428 | 		}
  429 | 		finally {
  430 | 			if (closeResources) {
  431 | 				JdbcUtils.closeStatement(stmt);
  432 | 				DataSourceUtils.releaseConnection(con, getDataSource());
  433 | 			}
  434 | 		}
  435 | 	}
  436 | 
  437 | 	@Override
  438 | 	public <T extends @Nullable Object> T execute(StatementCallback<T> action) throws DataAccessException {
  439 | 		return execute(action, true);
  440 | 	}
  441 | 
  442 | 	@Override
  443 | 	public void execute(String sql) throws DataAccessException {
  444 | 		if (logger.isDebugEnabled()) {
  445 | 			logger.debug("Executing SQL statement [" + sql + "]");
  446 | 		}
  447 | 
  448 | 		// Callback to execute the statement.
  449 | 		class ExecuteStatementCallback implements StatementCallback<@Nullable Object>, SqlProvider {
  450 | 			@Override
  451 | 			public @Nullable Object doInStatement(Statement stmt) throws SQLException {
  452 | 				stmt.execute(sql);
  453 | 				return null;
  454 | 			}
  455 | 			@Override
  456 | 			public String getSql() {
  457 | 				return sql;
  458 | 			}
  459 | 		}
  460 | 
  461 | 		execute(new ExecuteStatementCallback(), true);
  462 | 	}
  463 | 
  464 | 	@Override
  465 | 	public <T extends @Nullable Object> T query(String sql, ResultSetExtractor<T> rse) throws DataAccessException {
  466 | 		Assert.notNull(sql, "SQL must not be null");
  467 | 		Assert.notNull(rse, "ResultSetExtractor must not be null");
  468 | 		if (logger.isDebugEnabled()) {
  469 | 			logger.debug("Executing SQL query [" + sql + "]");
  470 | 		}
  471 | 
  472 | 		// Callback to execute the query.
  473 | 		class QueryStatementCallback implements StatementCallback<T>, SqlProvider {
  474 | 			@Override
  475 | 			public T doInStatement(Statement stmt) throws SQLException {
  476 | 				ResultSet rs = null;
  477 | 				try {
  478 | 					rs = stmt.executeQuery(sql);
  479 | 					return rse.extractData(rs);
  480 | 				}
  481 | 				finally {
  482 | 					JdbcUtils.closeResultSet(rs);
  483 | 				}
  484 | 			}
  485 | 			@Override
  486 | 			public String getSql() {
  487 | 				return sql;
  488 | 			}
  489 | 		}
  490 | 
  491 | 		return execute(new QueryStatementCallback(), true);
  492 | 	}
  493 | 
  494 | 	@Override
  495 | 	public void query(String sql, RowCallbackHandler rch) throws DataAccessException {
  496 | 		query(sql, new RowCallbackHandlerResultSetExtractor(rch, this.maxRows));
  497 | 	}
  498 | 
  499 | 	@Override
  500 | 	public <T extends @Nullable Object> List<T> query(String sql, RowMapper<T> rowMapper) throws DataAccessException {
  501 | 		return result(query(sql, new RowMapperResultSetExtractor<>(rowMapper, 0, this.maxRows)));
  502 | 	}
  503 | 
  504 | 	@Override
  505 | 	public <T extends @Nullable Object> Stream<T> queryForStream(String sql, RowMapper<T> rowMapper) throws DataAccessException {
  506 | 		class StreamStatementCallback implements StatementCallback<Stream<T>>, SqlProvider {
  507 | 			@Override
  508 | 			public Stream<T> doInStatement(Statement stmt) throws SQLException {
  509 | 				ResultSet rs = stmt.executeQuery(sql);
  510 | 				Connection con = stmt.getConnection();
  511 | 				return new ResultSetSpliterator<>(rs, rowMapper, JdbcTemplate.this.maxRows).stream().onClose(() -> {
  512 | 					JdbcUtils.closeResultSet(rs);
  513 | 					JdbcUtils.closeStatement(stmt);
  514 | 					DataSourceUtils.releaseConnection(con, getDataSource());
  515 | 				});
  516 | 			}
  517 | 			@Override
  518 | 			public String getSql() {
  519 | 				return sql;
  520 | 			}
  521 | 		}
  522 | 
  523 | 		return result(execute(new StreamStatementCallback(), false));
  524 | 	}
  525 | 
  526 | 	@Override
  527 | 	public Map<String, @Nullable Object> queryForMap(String sql) throws DataAccessException {
  528 | 		return result(queryForObject(sql, getColumnMapRowMapper()));
  529 | 	}
  530 | 
  531 | 	@Override
  532 | 	public <T extends @Nullable Object> T queryForObject(String sql, RowMapper<T> rowMapper) throws DataAccessException {
  533 | 		List<T> results = query(sql, rowMapper);
  534 | 		return DataAccessUtils.nullableSingleResult(results);
  535 | 	}
  536 | 
  537 | 	@Override
  538 | 	public <T> @Nullable T queryForObject(String sql, Class<T> requiredType) throws DataAccessException {
  539 | 		return queryForObject(sql, getSingleColumnRowMapper(requiredType));
  540 | 	}
  541 | 
  542 | 	@Override
  543 | 	public <T> List<@Nullable T> queryForList(String sql, Class<T> elementType) throws DataAccessException {
  544 | 		return query(sql, getSingleColumnRowMapper(elementType));
  545 | 	}
  546 | 
  547 | 	@Override
  548 | 	public List<Map<String, @Nullable Object>> queryForList(String sql) throws DataAccessException {
  549 | 		return query(sql, getColumnMapRowMapper());
  550 | 	}
  551 | 
  552 | 	@Override
  553 | 	public SqlRowSet queryForRowSet(String sql) throws DataAccessException {
  554 | 		return result(query(sql, new SqlRowSetResultSetExtractor()));
  555 | 	}
  556 | 
  557 | 	@Override
  558 | 	public int update(String sql) throws DataAccessException {
  559 | 		Assert.notNull(sql, "SQL must not be null");
  560 | 		if (logger.isDebugEnabled()) {
  561 | 			logger.debug("Executing SQL update [" + sql + "]");
  562 | 		}
  563 | 
  564 | 		// Callback to execute the update statement.
  565 | 		class UpdateStatementCallback implements StatementCallback<Integer>, SqlProvider {
  566 | 			@Override
  567 | 			public Integer doInStatement(Statement stmt) throws SQLException {
  568 | 				int rows = stmt.executeUpdate(sql);
  569 | 				if (logger.isTraceEnabled()) {
  570 | 					logger.trace("SQL update affected " + rows + " rows");
  571 | 				}
  572 | 				return rows;
  573 | 			}
  574 | 			@Override
  575 | 			public String getSql() {
  576 | 				return sql;
  577 | 			}
  578 | 		}
  579 | 
  580 | 		return updateCount(execute(new UpdateStatementCallback(), true));
  581 | 	}
  582 | 
  583 | 	@Override
  584 | 	public int[] batchUpdate(String... sql) throws DataAccessException {
  585 | 		Assert.notEmpty(sql, "SQL array must not be empty");
  586 | 		if (logger.isDebugEnabled()) {
  587 | 			logger.debug("Executing SQL batch update of " + sql.length + " statements");
  588 | 		}
  589 | 
  590 | 		// Callback to execute the batch update.
  591 | 		class BatchUpdateStatementCallback implements StatementCallback<int[]>, SqlProvider {
  592 | 
  593 | 			private final StringBuilder currSql = new StringBuilder();
  594 | 
  595 | 			@Override
  596 | 			public int[] doInStatement(Statement stmt) throws SQLException, DataAccessException {
  597 | 				int[] rowsAffected = new int[sql.length];
  598 | 				if (JdbcUtils.supportsBatchUpdates(stmt.getConnection())) {
  599 | 					for (String sqlStmt : sql) {
  600 | 						appendSql(sqlStmt);
  601 | 						stmt.addBatch(sqlStmt);
  602 | 					}
  603 | 					try {
  604 | 						rowsAffected = stmt.executeBatch();
  605 | 					}
  606 | 					catch (BatchUpdateException ex) {
  607 | 						this.currSql.setLength(0);
  608 | 						int[] updateCounts = ex.getUpdateCounts();
  609 | 						for (int i = 0; i < ex.getUpdateCounts().length; i++) {
  610 | 							if (updateCounts[i] == Statement.EXECUTE_FAILED) {
  611 | 								appendSql(sql[i]);
  612 | 							}
  613 | 						}
  614 | 						throw ex;
  615 | 					}
  616 | 				}
  617 | 				else {
  618 | 					for (int i = 0; i < sql.length; i++) {
  619 | 						this.currSql.setLength(0);
  620 | 						this.currSql.append(sql[i]);
  621 | 						if (!stmt.execute(sql[i])) {
  622 | 							rowsAffected[i] = stmt.getUpdateCount();
  623 | 						}
  624 | 						else {
  625 | 							throw new InvalidDataAccessApiUsageException("Invalid batch SQL statement: " + sql[i]);
  626 | 						}
  627 | 					}
  628 | 				}
  629 | 				return rowsAffected;
  630 | 			}
  631 | 
  632 | 			private void appendSql(String statement) {
  633 | 				if (!this.currSql.isEmpty()) {
  634 | 					this.currSql.append("; ");
  635 | 				}
  636 | 				this.currSql.append(statement);
  637 | 			}
  638 | 
  639 | 			@Override
  640 | 			public @Nullable String getSql() {
  641 | 				return this.currSql.toString();
  642 | 			}
  643 | 		}
  644 | 
  645 | 		int[] result = execute(new BatchUpdateStatementCallback(), true);
  646 | 		Assert.state(result != null, "No update counts");
  647 | 		return result;
  648 | 	}
  649 | 
  650 | 
  651 | 	//-------------------------------------------------------------------------
  652 | 	// Methods dealing with prepared statements
  653 | 	//-------------------------------------------------------------------------
  654 | 
  655 | 	private <T extends @Nullable Object> T execute(PreparedStatementCreator psc, PreparedStatementCallback<T> action, boolean closeResources)
  656 | 			throws DataAccessException {
  657 | 
  658 | 		Assert.notNull(psc, "PreparedStatementCreator must not be null");
  659 | 		Assert.notNull(action, "Callback object must not be null");
  660 | 		if (logger.isDebugEnabled()) {
  661 | 			String sql = getSql(psc);
  662 | 			logger.debug("Executing prepared SQL statement" + (sql != null ? " [" + sql + "]" : ""));
  663 | 		}
  664 | 
  665 | 		Connection con = DataSourceUtils.getConnection(obtainDataSource());
  666 | 		PreparedStatement ps = null;
  667 | 		try {
  668 | 			ps = psc.createPreparedStatement(con);
  669 | 			applyStatementSettings(ps);
  670 | 			T result = action.doInPreparedStatement(ps);
  671 | 			handleWarnings(ps);
  672 | 			return result;
  673 | 		}
  674 | 		catch (SQLException ex) {
  675 | 			// Release Connection early, to avoid potential connection pool deadlock
  676 | 			// in the case when the exception translator hasn't been initialized yet.
  677 | 			if (psc instanceof ParameterDisposer parameterDisposer) {
  678 | 				parameterDisposer.cleanupParameters();
  679 | 			}
  680 | 			if (ps != null) {
  681 | 				handleWarnings(ps, ex);
  682 | 			}
  683 | 			String sql = getSql(psc);
  684 | 			psc = null;
  685 | 			JdbcUtils.closeStatement(ps);
  686 | 			ps = null;
  687 | 			DataSourceUtils.releaseConnection(con, getDataSource());
  688 | 			con = null;
  689 | 			throw translateException("PreparedStatementCallback", sql, ex);
  690 | 		}
  691 | 		finally {
  692 | 			if (closeResources) {
  693 | 				if (psc instanceof ParameterDisposer parameterDisposer) {
  694 | 					parameterDisposer.cleanupParameters();
  695 | 				}
  696 | 				JdbcUtils.closeStatement(ps);
  697 | 				DataSourceUtils.releaseConnection(con, getDataSource());
  698 | 			}
  699 | 		}
  700 | 	}
  701 | 
  702 | 	@Override
  703 | 	public <T extends @Nullable Object> T execute(PreparedStatementCreator psc, PreparedStatementCallback<T> action)
  704 | 			throws DataAccessException {
  705 | 
  706 | 		return execute(psc, action, true);
  707 | 	}
  708 | 
  709 | 	@Override
  710 | 	public <T extends @Nullable Object> T execute(String sql, PreparedStatementCallback<T> action) throws DataAccessException {
  711 | 		return execute(new SimplePreparedStatementCreator(sql), action, true);
  712 | 	}
  713 | 
  714 | 	/**
  715 | 	 * Query using a prepared statement, allowing for a PreparedStatementCreator
  716 | 	 * and a PreparedStatementSetter. Most other query methods use this method,
  717 | 	 * but application code will always work with either a creator or a setter.
  718 | 	 * @param psc a callback that creates a PreparedStatement given a Connection
  719 | 	 * @param pss a callback that knows how to set values on the prepared statement.
  720 | 	 * If this is {@code null}, the SQL will be assumed to contain no bind parameters.
  721 | 	 * @param rse a callback that will extract results
  722 | 	 * @return an arbitrary result object, as returned by the ResultSetExtractor
  723 | 	 * @throws DataAccessException if there is any problem
  724 | 	 */
  725 | 	public <T extends @Nullable Object> T query(
  726 | 			PreparedStatementCreator psc, @Nullable PreparedStatementSetter pss, ResultSetExtractor<T> rse)
  727 | 			throws DataAccessException {
  728 | 
  729 | 		Assert.notNull(rse, "ResultSetExtractor must not be null");
  730 | 		logger.debug("Executing prepared SQL query");
  731 | 
  732 | 		return execute(psc, (PreparedStatementCallback<T>) ps -> {
  733 | 			ResultSet rs = null;
  734 | 			try {
  735 | 				if (pss != null) {
  736 | 					pss.setValues(ps);
  737 | 				}
  738 | 				rs = ps.executeQuery();
  739 | 				return rse.extractData(rs);
  740 | 			}
  741 | 			finally {
  742 | 				JdbcUtils.closeResultSet(rs);
  743 | 				if (pss instanceof ParameterDisposer parameterDisposer) {
  744 | 					parameterDisposer.cleanupParameters();
  745 | 				}
  746 | 			}
  747 | 		}, true);
  748 | 	}
  749 | 
  750 | 	@Override
  751 | 	public <T extends @Nullable Object> T query(PreparedStatementCreator psc, ResultSetExtractor<T> rse) throws DataAccessException {
  752 | 		return query(psc, null, rse);
  753 | 	}
  754 | 
  755 | 	@Override
  756 | 	public <T extends @Nullable Object> T query(String sql, @Nullable PreparedStatementSetter pss, ResultSetExtractor<T> rse) throws DataAccessException {
  757 | 		return query(new SimplePreparedStatementCreator(sql), pss, rse);
  758 | 	}
  759 | 
  760 | 	@Override
  761 | 	public <T extends @Nullable Object> T query(String sql, @Nullable Object @Nullable [] args, int[] argTypes, ResultSetExtractor<T> rse) throws DataAccessException {
  762 | 		return query(sql, newArgTypePreparedStatementSetter(args, argTypes), rse);
  763 | 	}
  764 | 
  765 | 	@Deprecated(since = "5.3")
  766 | 	@Override
  767 | 	public <T extends @Nullable Object> T query(String sql, @Nullable Object @Nullable [] args, ResultSetExtractor<T> rse) throws DataAccessException {
  768 | 		return query(sql, newArgPreparedStatementSetter(args), rse);
  769 | 	}
  770 | 
  771 | 	@Override
  772 | 	public <T extends @Nullable Object> T query(String sql, ResultSetExtractor<T> rse, @Nullable Object @Nullable ... args) throws DataAccessException {
  773 | 		return query(sql, newArgPreparedStatementSetter(args), rse);
  774 | 	}
  775 | 
  776 | 	@Override
  777 | 	public void query(PreparedStatementCreator psc, RowCallbackHandler rch) throws DataAccessException {
  778 | 		query(psc, new RowCallbackHandlerResultSetExtractor(rch, this.maxRows));
  779 | 	}
  780 | 
  781 | 	@Override
  782 | 	public void query(String sql, @Nullable PreparedStatementSetter pss, RowCallbackHandler rch) throws DataAccessException {
  783 | 		query(sql, pss, new RowCallbackHandlerResultSetExtractor(rch, this.maxRows));
  784 | 	}
  785 | 
  786 | 	@Override
  787 | 	public void query(String sql, @Nullable Object @Nullable [] args, int[] argTypes, RowCallbackHandler rch) throws DataAccessException {
  788 | 		query(sql, newArgTypePreparedStatementSetter(args, argTypes), rch);
  789 | 	}
  790 | 
  791 | 	@Deprecated(since = "5.3")
  792 | 	@Override
  793 | 	public void query(String sql, @Nullable Object @Nullable [] args, RowCallbackHandler rch) throws DataAccessException {
  794 | 		query(sql, newArgPreparedStatementSetter(args), rch);
  795 | 	}
  796 | 
  797 | 	@Override
  798 | 	public void query(String sql, RowCallbackHandler rch, @Nullable Object @Nullable ... args) throws DataAccessException {
  799 | 		query(sql, newArgPreparedStatementSetter(args), rch);
  800 | 	}
  801 | 
  802 | 	@Override
  803 | 	public <T extends @Nullable Object> List<T> query(PreparedStatementCreator psc, RowMapper<T> rowMapper) throws DataAccessException {
  804 | 		return result(query(psc, new RowMapperResultSetExtractor<>(rowMapper, 0, this.maxRows)));
  805 | 	}
  806 | 
  807 | 	@Override
  808 | 	public <T extends @Nullable Object> List<T> query(String sql, @Nullable PreparedStatementSetter pss, RowMapper<T> rowMapper) throws DataAccessException {
  809 | 		return result(query(sql, pss, new RowMapperResultSetExtractor<>(rowMapper, 0, this.maxRows)));
  810 | 	}
  811 | 
  812 | 	@Override
  813 | 	public <T extends @Nullable Object> List<T> query(String sql, @Nullable Object @Nullable [] args, int[] argTypes, RowMapper<T> rowMapper) throws DataAccessException {
  814 | 		return result(query(sql, args, argTypes, new RowMapperResultSetExtractor<>(rowMapper, 0, this.maxRows)));
  815 | 	}
  816 | 
  817 | 	@Deprecated(since = "5.3")
  818 | 	@Override
  819 | 	public <T extends @Nullable Object> List<T> query(String sql, @Nullable Object @Nullable [] args, RowMapper<T> rowMapper) throws DataAccessException {
  820 | 		return result(query(sql, newArgPreparedStatementSetter(args), new RowMapperResultSetExtractor<>(rowMapper, 0, this.maxRows)));
  821 | 	}
  822 | 
  823 | 	@Override
  824 | 	public <T extends @Nullable Object> List<T> query(String sql, RowMapper<T> rowMapper, @Nullable Object @Nullable ... args) throws DataAccessException {
  825 | 		return result(query(sql, newArgPreparedStatementSetter(args), new RowMapperResultSetExtractor<>(rowMapper, 0, this.maxRows)));
  826 | 	}
  827 | 
  828 | 	/**
  829 | 	 * Query using a prepared statement, allowing for a PreparedStatementCreator
  830 | 	 * and a PreparedStatementSetter. Most other query methods use this method,
  831 | 	 * but application code will always work with either a creator or a setter.
  832 | 	 * @param psc a callback that creates a PreparedStatement given a Connection
  833 | 	 * @param pss a callback that knows how to set values on the prepared statement.
  834 | 	 * If this is {@code null}, the SQL will be assumed to contain no bind parameters.
  835 | 	 * @param rowMapper a callback that will map one object per row
  836 | 	 * @return the result Stream, containing mapped objects, needing to be
  837 | 	 * closed once fully processed (for example, through a try-with-resources clause)
  838 | 	 * @throws DataAccessException if the query fails
  839 | 	 * @since 5.3
  840 | 	 */
  841 | 	public <T extends @Nullable Object> Stream<T> queryForStream(PreparedStatementCreator psc, @Nullable PreparedStatementSetter pss,
  842 | 			RowMapper<T> rowMapper) throws DataAccessException {
  843 | 
  844 | 		return result(execute(psc, ps -> {
  845 | 			if (pss != null) {
  846 | 				pss.setValues(ps);
  847 | 			}
  848 | 			ResultSet rs = ps.executeQuery();
  849 | 			Connection con = ps.getConnection();
  850 | 			return new ResultSetSpliterator<>(rs, rowMapper, this.maxRows).stream().onClose(() -> {
  851 | 				JdbcUtils.closeResultSet(rs);
  852 | 				if (pss instanceof ParameterDisposer parameterDisposer) {
  853 | 					parameterDisposer.cleanupParameters();
  854 | 				}
  855 | 				JdbcUtils.closeStatement(ps);
  856 | 				DataSourceUtils.releaseConnection(con, getDataSource());
  857 | 			});
  858 | 		}, false));
  859 | 	}
  860 | 
  861 | 	@Override
  862 | 	public <T extends @Nullable Object> Stream<T> queryForStream(PreparedStatementCreator psc, RowMapper<T> rowMapper) throws DataAccessException {
  863 | 		return queryForStream(psc, null, rowMapper);
  864 | 	}
  865 | 
  866 | 	@Override
  867 | 	public <T extends @Nullable Object> Stream<T> queryForStream(String sql, @Nullable PreparedStatementSetter pss, RowMapper<T> rowMapper) throws DataAccessException {
  868 | 		return queryForStream(new SimplePreparedStatementCreator(sql), pss, rowMapper);
  869 | 	}
  870 | 
  871 | 	@Override
  872 | 	public <T extends @Nullable Object> Stream<T> queryForStream(String sql, RowMapper<T> rowMapper, @Nullable Object @Nullable ... args) throws DataAccessException {
  873 | 		return queryForStream(new SimplePreparedStatementCreator(sql), newArgPreparedStatementSetter(args), rowMapper);
  874 | 	}
  875 | 
  876 | 	@Override
  877 | 	public <T extends @Nullable Object> T queryForObject(String sql, @Nullable Object @Nullable [] args, int[] argTypes, RowMapper<T> rowMapper)
  878 | 			throws DataAccessException {
  879 | 
  880 | 		List<T> results = query(sql, args, argTypes, new RowMapperResultSetExtractor<>(rowMapper, 1));
  881 | 		return DataAccessUtils.nullableSingleResult(results);
  882 | 	}
  883 | 
  884 | 	@Deprecated(since = "5.3")
  885 | 	@Override
  886 | 	public <T extends @Nullable Object> T queryForObject(String sql, @Nullable Object @Nullable [] args, RowMapper<T> rowMapper) throws DataAccessException {
  887 | 		List<T> results = query(sql, newArgPreparedStatementSetter(args), new RowMapperResultSetExtractor<>(rowMapper, 1));
  888 | 		return DataAccessUtils.nullableSingleResult(results);
  889 | 	}
  890 | 
  891 | 	@Override
  892 | 	public <T extends @Nullable Object> T queryForObject(String sql, RowMapper<T> rowMapper, @Nullable Object @Nullable ... args) throws DataAccessException {
  893 | 		List<T> results = query(sql, newArgPreparedStatementSetter(args), new RowMapperResultSetExtractor<>(rowMapper, 1));
  894 | 		return DataAccessUtils.nullableSingleResult(results);
  895 | 	}
  896 | 
  897 | 	@Override
  898 | 	public <T> @Nullable T queryForObject(String sql, @Nullable Object @Nullable [] args, int[] argTypes, Class<T> requiredType)
  899 | 			throws DataAccessException {
  900 | 
  901 | 		return queryForObject(sql, args, argTypes, getSingleColumnRowMapper(requiredType));
  902 | 	}
  903 | 
  904 | 	@Deprecated(since = "5.3")
  905 | 	@Override
  906 | 	public <T> @Nullable T queryForObject(String sql, @Nullable Object @Nullable [] args, Class<T> requiredType) throws DataAccessException {
  907 | 		return queryForObject(sql, getSingleColumnRowMapper(requiredType), args);
  908 | 	}
  909 | 
  910 | 	@Override
  911 | 	public <T> @Nullable T queryForObject(String sql, Class<T> requiredType, @Nullable Object @Nullable ... args) throws DataAccessException {
  912 | 		return queryForObject(sql, getSingleColumnRowMapper(requiredType), args);
  913 | 	}
  914 | 
  915 | 	@Override
  916 | 	public Map<String, @Nullable Object> queryForMap(String sql, @Nullable Object @Nullable [] args, int[] argTypes) throws DataAccessException {
  917 | 		return result(queryForObject(sql, args, argTypes, getColumnMapRowMapper()));
  918 | 	}
  919 | 
  920 | 	@Override
  921 | 	public Map<String, @Nullable Object> queryForMap(String sql, @Nullable Object @Nullable ... args) throws DataAccessException {
  922 | 		return result(queryForObject(sql, getColumnMapRowMapper(), args));
  923 | 	}
  924 | 
  925 | 	@Override
  926 | 	public <T> List<@Nullable T> queryForList(String sql, @Nullable Object @Nullable [] args, int[] argTypes, Class<T> elementType) throws DataAccessException {
  927 | 		return query(sql, args, argTypes, getSingleColumnRowMapper(elementType));
  928 | 	}
  929 | 
  930 | 	@Deprecated(since = "5.3")
  931 | 	@Override
  932 | 	public <T> List<@Nullable T> queryForList(String sql, @Nullable Object @Nullable [] args, Class<T> elementType) throws DataAccessException {
  933 | 		return query(sql, newArgPreparedStatementSetter(args), getSingleColumnRowMapper(elementType));
  934 | 	}
  935 | 
  936 | 	@Override
  937 | 	public <T> List<@Nullable T> queryForList(String sql, Class<T> elementType, @Nullable Object @Nullable ... args) throws DataAccessException {
  938 | 		return query(sql, newArgPreparedStatementSetter(args), getSingleColumnRowMapper(elementType));
  939 | 	}
  940 | 
  941 | 	@Override
  942 | 	public List<Map<String, @Nullable Object>> queryForList(String sql, @Nullable Object @Nullable [] args, int[] argTypes) throws DataAccessException {
  943 | 		return query(sql, args, argTypes, getColumnMapRowMapper());
  944 | 	}
  945 | 
  946 | 	@Override
  947 | 	public List<Map<String, @Nullable Object>> queryForList(String sql, @Nullable Object @Nullable ... args) throws DataAccessException {
  948 | 		return query(sql, newArgPreparedStatementSetter(args), getColumnMapRowMapper());
  949 | 	}
  950 | 
  951 | 	@Override
  952 | 	public SqlRowSet queryForRowSet(String sql, @Nullable Object @Nullable [] args, int[] argTypes) throws DataAccessException {
  953 | 		return result(query(sql, args, argTypes, new SqlRowSetResultSetExtractor()));
  954 | 	}
  955 | 
  956 | 	@Override
  957 | 	public SqlRowSet queryForRowSet(String sql, @Nullable Object @Nullable ... args) throws DataAccessException {
  958 | 		return result(query(sql, newArgPreparedStatementSetter(args), new SqlRowSetResultSetExtractor()));
  959 | 	}
  960 | 
  961 | 	protected int update(PreparedStatementCreator psc, @Nullable PreparedStatementSetter pss)
  962 | 			throws DataAccessException {
  963 | 
  964 | 		logger.debug("Executing prepared SQL update");
  965 | 
  966 | 		return updateCount(execute(psc, ps -> {
  967 | 			try {
  968 | 				if (pss != null) {
  969 | 					pss.setValues(ps);
  970 | 				}
  971 | 				int rows = ps.executeUpdate();
  972 | 				if (logger.isTraceEnabled()) {
  973 | 					logger.trace("SQL update affected " + rows + " rows");
  974 | 				}
  975 | 				return rows;
  976 | 			}
  977 | 			finally {
  978 | 				if (pss instanceof ParameterDisposer parameterDisposer) {
  979 | 					parameterDisposer.cleanupParameters();
  980 | 				}
  981 | 			}
  982 | 		}, true));
  983 | 	}
  984 | 
  985 | 	@Override
  986 | 	public int update(PreparedStatementCreator psc) throws DataAccessException {
  987 | 		return update(psc, (PreparedStatementSetter) null);
  988 | 	}
  989 | 
  990 | 	@Override
  991 | 	public int update(PreparedStatementCreator psc, KeyHolder generatedKeyHolder)
  992 | 			throws DataAccessException {
  993 | 
  994 | 		Assert.notNull(generatedKeyHolder, "KeyHolder must not be null");
  995 | 		logger.debug("Executing SQL update and returning generated keys");
  996 | 
  997 | 		return updateCount(execute(psc, ps -> {
  998 | 			int rows = ps.executeUpdate();
  999 | 			generatedKeyHolder.getKeyList().clear();
 1000 | 			storeGeneratedKeys(generatedKeyHolder, ps, 1);
 1001 | 			if (logger.isTraceEnabled()) {
 1002 | 				logger.trace("SQL update affected " + rows + " rows and returned " + generatedKeyHolder.getKeyList().size() + " keys");
 1003 | 			}
 1004 | 			return rows;
 1005 | 		}, true));
 1006 | 	}
 1007 | 
 1008 | 	@Override
 1009 | 	public int update(String sql, @Nullable PreparedStatementSetter pss) throws DataAccessException {
 1010 | 		return update(new SimplePreparedStatementCreator(sql), pss);
 1011 | 	}
 1012 | 
 1013 | 	@Override
 1014 | 	public int update(String sql, @Nullable Object @Nullable [] args, int[] argTypes) throws DataAccessException {
 1015 | 		return update(sql, newArgTypePreparedStatementSetter(args, argTypes));
 1016 | 	}
 1017 | 
 1018 | 	@Override
 1019 | 	public int update(String sql, @Nullable Object @Nullable ... args) throws DataAccessException {
 1020 | 		return update(sql, newArgPreparedStatementSetter(args));
 1021 | 	}
 1022 | 
 1023 | 	@Override
 1024 | 	public int[] batchUpdate(PreparedStatementCreator psc, BatchPreparedStatementSetter pss,
 1025 | 			KeyHolder generatedKeyHolder) throws DataAccessException {
 1026 | 
 1027 | 		int[] result = execute(psc, getPreparedStatementCallback(pss, generatedKeyHolder));
 1028 | 
 1029 | 		Assert.state(result != null, "No result array");
 1030 | 		return result;
 1031 | 	}
 1032 | 
 1033 | 	@Override
 1034 | 	public int[] batchUpdate(String sql, BatchPreparedStatementSetter pss) throws DataAccessException {
 1035 | 		if (logger.isDebugEnabled()) {
 1036 | 			logger.debug("Executing SQL batch update [" + sql + "]");
 1037 | 		}
 1038 | 		int batchSize = pss.getBatchSize();
 1039 | 		if (batchSize == 0) {
 1040 | 			return new int[0];
 1041 | 		}
 1042 | 
 1043 | 		int[] result = execute(sql, getPreparedStatementCallback(pss, null));
 1044 | 		Assert.state(result != null, "No result array");
 1045 | 		return result;
 1046 | 	}
 1047 | 
 1048 | 	@Override
 1049 | 	public int[] batchUpdate(String sql, List<? extends @Nullable Object[]> batchArgs) throws DataAccessException {
 1050 | 		return batchUpdate(sql, batchArgs, new int[0]);
 1051 | 	}
 1052 | 
 1053 | 	@Override
 1054 | 	public int[] batchUpdate(String sql, List<? extends @Nullable Object[]> batchArgs, int[] argTypes) throws DataAccessException {
 1055 | 		if (batchArgs.isEmpty()) {
 1056 | 			return new int[0];
 1057 | 		}
 1058 | 
 1059 | 		return batchUpdate(
 1060 | 				sql,
 1061 | 				new BatchPreparedStatementSetter() {
 1062 | 					@Override
 1063 | 					public void setValues(PreparedStatement ps, int i) throws SQLException {
 1064 | 						@Nullable Object[] values = batchArgs.get(i);
 1065 | 						int colIndex = 0;
 1066 | 						for (@Nullable Object value : values) {
 1067 | 							colIndex++;
 1068 | 							if (value instanceof SqlParameterValue paramValue) {
 1069 | 								StatementCreatorUtils.setParameterValue(ps, colIndex, paramValue, paramValue.getValue());
 1070 | 							}
 1071 | 							else {
 1072 | 								int colType;
 1073 | 								if (argTypes.length < colIndex) {
 1074 | 									colType = SqlTypeValue.TYPE_UNKNOWN;
 1075 | 								}
 1076 | 								else {
 1077 | 									colType = argTypes[colIndex - 1];
 1078 | 								}
 1079 | 								StatementCreatorUtils.setParameterValue(ps, colIndex, colType, value);
 1080 | 							}
 1081 | 						}
 1082 | 					}
 1083 | 					@Override
 1084 | 					public int getBatchSize() {
 1085 | 						return batchArgs.size();
 1086 | 					}
 1087 | 				});
 1088 | 	}
 1089 | 
 1090 | 	@Deprecated(since = "7.1")
 1091 | 	@Override
 1092 | 	public <T> int[][] batchUpdate(String sql, Collection<T> batchArgs, int batchSize,
 1093 | 			ParameterizedPreparedStatementSetter<T> pss) throws DataAccessException {
 1094 | 
 1095 | 		if (logger.isDebugEnabled()) {
 1096 | 			logger.debug("Executing SQL batch update [" + sql + "] with a batch size of " + batchSize);
 1097 | 		}
 1098 | 		int[][] result = execute(sql, (PreparedStatementCallback<int[][]>) ps -> {
 1099 | 			List<int[]> rowsAffected = new ArrayList<>();
 1100 | 			try {
 1101 | 				boolean batchSupported = JdbcUtils.supportsBatchUpdates(ps.getConnection());
 1102 | 				int n = 0;
 1103 | 				for (T obj : batchArgs) {
 1104 | 					pss.setValues(ps, obj);
 1105 | 					n++;
 1106 | 					if (batchSupported) {
 1107 | 						ps.addBatch();
 1108 | 						if (n % batchSize == 0 || n == batchArgs.size()) {
 1109 | 							if (logger.isTraceEnabled()) {
 1110 | 								int batchIdx = (n % batchSize == 0) ? n / batchSize : (n / batchSize) + 1;
 1111 | 								int items = n - ((n % batchSize == 0) ? n / batchSize - 1 : (n / batchSize)) * batchSize;
 1112 | 								logger.trace("Sending SQL batch update #" + batchIdx + " with " + items + " items");
 1113 | 							}
 1114 | 							try {
 1115 | 								int[] updateCounts = ps.executeBatch();
 1116 | 								rowsAffected.add(updateCounts);
 1117 | 							}
 1118 | 							catch (BatchUpdateException ex) {
 1119 | 								throw new AggregatedBatchUpdateException(rowsAffected.toArray(int[][]::new), ex);
 1120 | 							}
 1121 | 						}
 1122 | 					}
 1123 | 					else {
 1124 | 						int i = ps.executeUpdate();
 1125 | 						rowsAffected.add(new int[] {i});
 1126 | 					}
 1127 | 				}
 1128 | 				int[][] result1 = new int[rowsAffected.size()][];
 1129 | 				for (int i = 0; i < result1.length; i++) {
 1130 | 					result1[i] = rowsAffected.get(i);
 1131 | 				}
 1132 | 				return result1;
 1133 | 			}
 1134 | 			finally {
 1135 | 				if (pss instanceof ParameterDisposer parameterDisposer) {
 1136 | 					parameterDisposer.cleanupParameters();
 1137 | 				}
 1138 | 			}
 1139 | 		});
 1140 | 
 1141 | 		Assert.state(result != null, "No result array");
 1142 | 		return result;
 1143 | 	}
 1144 | 
 1145 | 
 1146 | 	//-------------------------------------------------------------------------
 1147 | 	// Methods dealing with callable statements
 1148 | 	//-------------------------------------------------------------------------
 1149 | 
 1150 | 	@Override
 1151 | 	public <T extends @Nullable Object> T execute(CallableStatementCreator csc, CallableStatementCallback<T> action)
 1152 | 			throws DataAccessException {
 1153 | 
 1154 | 		Assert.notNull(csc, "CallableStatementCreator must not be null");
 1155 | 		Assert.notNull(action, "Callback object must not be null");
 1156 | 		if (logger.isDebugEnabled()) {
 1157 | 			String sql = getSql(csc);
 1158 | 			logger.debug("Calling stored procedure" + (sql != null ? " [" + sql + "]" : ""));
 1159 | 		}
 1160 | 
 1161 | 		Connection con = DataSourceUtils.getConnection(obtainDataSource());
 1162 | 		CallableStatement cs = null;
 1163 | 		try {
 1164 | 			cs = csc.createCallableStatement(con);
 1165 | 			applyStatementSettings(cs);
 1166 | 			T result = action.doInCallableStatement(cs);
 1167 | 			handleWarnings(cs);
 1168 | 			return result;
 1169 | 		}
 1170 | 		catch (SQLException ex) {
 1171 | 			// Release Connection early, to avoid potential connection pool deadlock
 1172 | 			// in the case when the exception translator hasn't been initialized yet.
 1173 | 			if (csc instanceof ParameterDisposer parameterDisposer) {
 1174 | 				parameterDisposer.cleanupParameters();
 1175 | 			}
 1176 | 			if (cs != null) {
 1177 | 				handleWarnings(cs, ex);
 1178 | 			}
 1179 | 			String sql = getSql(csc);
 1180 | 			csc = null;
 1181 | 			JdbcUtils.closeStatement(cs);
 1182 | 			cs = null;
 1183 | 			DataSourceUtils.releaseConnection(con, getDataSource());
 1184 | 			con = null;
 1185 | 			throw translateException("CallableStatementCallback", sql, ex);
 1186 | 		}
 1187 | 		finally {
 1188 | 			if (csc instanceof ParameterDisposer parameterDisposer) {
 1189 | 				parameterDisposer.cleanupParameters();
 1190 | 			}
 1191 | 			JdbcUtils.closeStatement(cs);
 1192 | 			DataSourceUtils.releaseConnection(con, getDataSource());
 1193 | 		}
 1194 | 	}
 1195 | 
 1196 | 	@Override
 1197 | 	public <T extends @Nullable Object> T execute(String callString, CallableStatementCallback<T> action) throws DataAccessException {
 1198 | 		return execute(new SimpleCallableStatementCreator(callString), action);
 1199 | 	}
 1200 | 
 1201 | 	@Override
 1202 | 	public Map<String, @Nullable Object> call(CallableStatementCreator csc, List<SqlParameter> declaredParameters)
 1203 | 			throws DataAccessException {
 1204 | 
 1205 | 		List<SqlParameter> updateCountParameters = new ArrayList<>();
 1206 | 		List<SqlParameter> resultSetParameters = new ArrayList<>();
 1207 | 		List<SqlParameter> callParameters = new ArrayList<>();
 1208 | 
 1209 | 		for (SqlParameter parameter : declaredParameters) {
 1210 | 			if (parameter.isResultsParameter()) {
 1211 | 				if (parameter instanceof SqlReturnResultSet) {
 1212 | 					resultSetParameters.add(parameter);
 1213 | 				}
 1214 | 				else {
 1215 | 					updateCountParameters.add(parameter);
 1216 | 				}
 1217 | 			}
 1218 | 			else {
 1219 | 				callParameters.add(parameter);
 1220 | 			}
 1221 | 		}
 1222 | 
 1223 | 		Map<String, @Nullable Object> result = execute(csc, cs -> {
 1224 | 			boolean retVal = cs.execute();
 1225 | 			int updateCount = cs.getUpdateCount();
 1226 | 			if (logger.isTraceEnabled()) {
 1227 | 				logger.trace("CallableStatement.execute() returned '" + retVal + "'");
 1228 | 				logger.trace("CallableStatement.getUpdateCount() returned " + updateCount);
 1229 | 			}
 1230 | 			Map<String, @Nullable Object> resultsMap = createResultsMap();
 1231 | 			if (retVal || updateCount != -1) {
 1232 | 				resultsMap.putAll(extractReturnedResults(cs, updateCountParameters, resultSetParameters, updateCount));
 1233 | 			}
 1234 | 			resultsMap.putAll(extractOutputParameters(cs, callParameters));
 1235 | 			return resultsMap;
 1236 | 		});
 1237 | 
 1238 | 		Assert.state(result != null, "No result map");
 1239 | 		return result;
 1240 | 	}
 1241 | 
 1242 | 	/**
 1243 | 	 * Extract returned ResultSets from the completed stored procedure.
 1244 | 	 * @param cs a JDBC wrapper for the stored procedure
 1245 | 	 * @param updateCountParameters the parameter list of declared update count parameters for the stored procedure
 1246 | 	 * @param resultSetParameters the parameter list of declared resultSet parameters for the stored procedure
 1247 | 	 * @return a Map that contains returned results
 1248 | 	 */
 1249 | 	protected Map<String, @Nullable Object> extractReturnedResults(CallableStatement cs,
 1250 | 			@Nullable List<SqlParameter> updateCountParameters, @Nullable List<SqlParameter> resultSetParameters,
 1251 | 			int updateCount) throws SQLException {
 1252 | 
 1253 | 		Map<String, @Nullable Object> results = new LinkedHashMap<>(4);
 1254 | 		int rsIndex = 0;
 1255 | 		int updateIndex = 0;
 1256 | 		boolean moreResults;
 1257 | 		if (!isSkipResultsProcessing()) {
 1258 | 			do {
 1259 | 				if (updateCount == -1) {
 1260 | 					if (resultSetParameters != null && resultSetParameters.size() > rsIndex) {
 1261 | 						SqlReturnResultSet declaredRsParam = (SqlReturnResultSet) resultSetParameters.get(rsIndex);
 1262 | 						results.putAll(processResultSet(cs.getResultSet(), declaredRsParam));
 1263 | 						rsIndex++;
 1264 | 					}
 1265 | 					else {
 1266 | 						if (!isSkipUndeclaredResults()) {
 1267 | 							String rsName = RETURN_RESULT_SET_PREFIX + (rsIndex + 1);
 1268 | 							SqlReturnResultSet undeclaredRsParam = new SqlReturnResultSet(rsName, getColumnMapRowMapper());
 1269 | 							if (logger.isTraceEnabled()) {
 1270 | 								logger.trace("Added default SqlReturnResultSet parameter named '" + rsName + "'");
 1271 | 							}
 1272 | 							results.putAll(processResultSet(cs.getResultSet(), undeclaredRsParam));
 1273 | 							rsIndex++;
 1274 | 						}
 1275 | 					}
 1276 | 				}
 1277 | 				else {
 1278 | 					if (updateCountParameters != null && updateCountParameters.size() > updateIndex) {
 1279 | 						SqlReturnUpdateCount ucParam = (SqlReturnUpdateCount) updateCountParameters.get(updateIndex);
 1280 | 						String declaredUcName = ucParam.getName();
 1281 | 						results.put(declaredUcName, updateCount);
 1282 | 						updateIndex++;
 1283 | 					}
 1284 | 					else {
 1285 | 						if (!isSkipUndeclaredResults()) {
 1286 | 							String undeclaredName = RETURN_UPDATE_COUNT_PREFIX + (updateIndex + 1);
 1287 | 							if (logger.isTraceEnabled()) {
 1288 | 								logger.trace("Added default SqlReturnUpdateCount parameter named '" + undeclaredName + "'");
 1289 | 							}
 1290 | 							results.put(undeclaredName, updateCount);
 1291 | 							updateIndex++;
 1292 | 						}
 1293 | 					}
 1294 | 				}
 1295 | 				moreResults = cs.getMoreResults();
 1296 | 				updateCount = cs.getUpdateCount();
 1297 | 				if (logger.isTraceEnabled()) {
 1298 | 					logger.trace("CallableStatement.getUpdateCount() returned " + updateCount);
 1299 | 				}
 1300 | 			}
 1301 | 			while (moreResults || updateCount != -1);
 1302 | 		}
 1303 | 		return results;
 1304 | 	}
 1305 | 
 1306 | 	/**
 1307 | 	 * Extract output parameters from the completed stored procedure.
 1308 | 	 * @param cs the JDBC wrapper for the stored procedure
 1309 | 	 * @param parameters parameter list for the stored procedure
 1310 | 	 * @return a Map that contains returned results
 1311 | 	 */
 1312 | 	protected Map<String, @Nullable Object> extractOutputParameters(CallableStatement cs, List<SqlParameter> parameters)
 1313 | 			throws SQLException {
 1314 | 
 1315 | 		Map<String, @Nullable Object> results = CollectionUtils.newLinkedHashMap(parameters.size());
 1316 | 		int sqlColIndex = 1;
 1317 | 		for (SqlParameter param : parameters) {
 1318 | 			if (param instanceof SqlOutParameter outParam) {
 1319 | 				Assert.state(outParam.getName() != null, "Anonymous parameters not allowed");
 1320 | 				SqlReturnType returnType = outParam.getSqlReturnType();
 1321 | 				if (returnType != null) {
 1322 | 					Object out = returnType.getTypeValue(cs, sqlColIndex, outParam.getSqlType(), outParam.getTypeName());
 1323 | 					results.put(outParam.getName(), out);
 1324 | 				}
 1325 | 				else {
 1326 | 					Object out = cs.getObject(sqlColIndex);
 1327 | 					if (out instanceof ResultSet resultSet) {
 1328 | 						if (outParam.isResultSetSupported()) {
 1329 | 							results.putAll(processResultSet(resultSet, outParam));
 1330 | 						}
 1331 | 						else {
 1332 | 							String rsName = outParam.getName();
 1333 | 							SqlReturnResultSet rsParam = new SqlReturnResultSet(rsName, getColumnMapRowMapper());
 1334 | 							results.putAll(processResultSet(resultSet, rsParam));
 1335 | 							if (logger.isTraceEnabled()) {
 1336 | 								logger.trace("Added default SqlReturnResultSet parameter named '" + rsName + "'");
 1337 | 							}
 1338 | 						}
 1339 | 					}
 1340 | 					else {
 1341 | 						results.put(outParam.getName(), out);
 1342 | 					}
 1343 | 				}
 1344 | 			}
 1345 | 			if (!param.isResultsParameter()) {
 1346 | 				sqlColIndex++;
 1347 | 			}
 1348 | 		}
 1349 | 		return results;
 1350 | 	}
 1351 | 
 1352 | 	/**
 1353 | 	 * Process the given ResultSet from a stored procedure.
 1354 | 	 * @param rs the ResultSet to process
 1355 | 	 * @param param the corresponding stored procedure parameter
 1356 | 	 * @return a Map that contains returned results
 1357 | 	 */
 1358 | 	@SuppressWarnings("NullAway") // See https://github.com/uber/NullAway/issues/950
 1359 | 	protected Map<@Nullable String, @Nullable Object> processResultSet(
 1360 | 			@Nullable ResultSet rs, ResultSetSupportingSqlParameter param) throws SQLException {
 1361 | 
 1362 | 		if (rs != null) {
 1363 | 			try {
 1364 | 				if (param.getRowMapper() != null) {
 1365 | 					RowMapper<? extends @Nullable Object> rowMapper = param.getRowMapper();
 1366 | 					Object data = (new RowMapperResultSetExtractor<>(rowMapper)).extractData(rs);
 1367 | 					return Collections.singletonMap(param.getName(), data);
 1368 | 				}
 1369 | 				else if (param.getRowCallbackHandler() != null) {
 1370 | 					RowCallbackHandler rch = param.getRowCallbackHandler();
 1371 | 					(new RowCallbackHandlerResultSetExtractor(rch, -1)).extractData(rs);
 1372 | 					return Collections.singletonMap(param.getName(),
 1373 | 							"ResultSet returned from stored procedure was processed");
 1374 | 				}
 1375 | 				else if (param.getResultSetExtractor() != null) {
 1376 | 					Object data = param.getResultSetExtractor().extractData(rs);
 1377 | 					return Collections.singletonMap(param.getName(), data);
 1378 | 				}
 1379 | 			}
 1380 | 			finally {
 1381 | 				JdbcUtils.closeResultSet(rs);
 1382 | 			}
 1383 | 		}
 1384 | 		return Collections.emptyMap();
 1385 | 	}
 1386 | 
 1387 | 
 1388 | 	//-------------------------------------------------------------------------
 1389 | 	// Implementation hooks and helper methods
 1390 | 	//-------------------------------------------------------------------------
 1391 | 
 1392 | 	/**
 1393 | 	 * Create a new RowMapper for reading columns as key-value pairs.
 1394 | 	 * @return the RowMapper to use
 1395 | 	 * @see ColumnMapRowMapper
 1396 | 	 */
 1397 | 	protected RowMapper<Map<String, @Nullable Object>> getColumnMapRowMapper() {
 1398 | 		return new ColumnMapRowMapper();
 1399 | 	}
 1400 | 
 1401 | 	/**
 1402 | 	 * Create a new RowMapper for reading result objects from a single column.
 1403 | 	 * @param requiredType the type that each result object is expected to match
 1404 | 	 * @return the RowMapper to use
 1405 | 	 * @see SingleColumnRowMapper
 1406 | 	 */
 1407 | 	protected <T> RowMapper<@Nullable T> getSingleColumnRowMapper(Class<T> requiredType) {
 1408 | 		return new SingleColumnRowMapper<>(requiredType);
 1409 | 	}
 1410 | 
 1411 | 	/**
 1412 | 	 * Create a Map instance to be used as the results map.
 1413 | 	 * <p>If {@link #resultsMapCaseInsensitive} has been set to true,
 1414 | 	 * a {@link LinkedCaseInsensitiveMap} will be created; otherwise, a
 1415 | 	 * {@link LinkedHashMap} will be created.
 1416 | 	 * @return the results Map instance
 1417 | 	 * @see #setResultsMapCaseInsensitive
 1418 | 	 * @see #isResultsMapCaseInsensitive
 1419 | 	 */
 1420 | 	protected Map<String, @Nullable Object> createResultsMap() {
 1421 | 		if (isResultsMapCaseInsensitive()) {
 1422 | 			return new LinkedCaseInsensitiveMap<>();
 1423 | 		}
 1424 | 		else {
 1425 | 			return new LinkedHashMap<>();
 1426 | 		}
 1427 | 	}
 1428 | 
 1429 | 	/**
 1430 | 	 * Prepare the given JDBC Statement (or PreparedStatement or CallableStatement),
 1431 | 	 * applying statement settings such as fetch size, max rows, and query timeout.
 1432 | 	 * @param stmt the JDBC Statement to prepare
 1433 | 	 * @throws SQLException if thrown by JDBC API
 1434 | 	 * @see #setFetchSize
 1435 | 	 * @see #setMaxRows
 1436 | 	 * @see #setQueryTimeout
 1437 | 	 * @see org.springframework.jdbc.datasource.DataSourceUtils#applyTransactionTimeout
 1438 | 	 */
 1439 | 	protected void applyStatementSettings(Statement stmt) throws SQLException {
 1440 | 		int fetchSize = getFetchSize();
 1441 | 		if (fetchSize != -1) {
 1442 | 			stmt.setFetchSize(fetchSize);
 1443 | 		}
 1444 | 		int maxRows = getMaxRows();
 1445 | 		if (maxRows != -1) {
 1446 | 			stmt.setMaxRows(maxRows);
 1447 | 		}
 1448 | 		DataSourceUtils.applyTimeout(stmt, getDataSource(), getQueryTimeout());
 1449 | 	}
 1450 | 
 1451 | 	/**
 1452 | 	 * Create a new arg-based PreparedStatementSetter using the args passed in.
 1453 | 	 * <p>By default, we'll create an {@link ArgumentPreparedStatementSetter}.
 1454 | 	 * This method allows for the creation to be overridden by subclasses.
 1455 | 	 * @param args object array with arguments
 1456 | 	 * @return the new PreparedStatementSetter to use
 1457 | 	 */
 1458 | 	protected PreparedStatementSetter newArgPreparedStatementSetter(@Nullable Object @Nullable [] args) {
 1459 | 		return new ArgumentPreparedStatementSetter(args);
 1460 | 	}
 1461 | 
 1462 | 	/**
 1463 | 	 * Create a new arg-type-based PreparedStatementSetter using the args and types passed in.
 1464 | 	 * <p>By default, we'll create an {@link ArgumentTypePreparedStatementSetter}.
 1465 | 	 * This method allows for the creation to be overridden by subclasses.
 1466 | 	 * @param args object array with arguments
 1467 | 	 * @param argTypes int array of SQLTypes for the associated arguments
 1468 | 	 * @return the new PreparedStatementSetter to use
 1469 | 	 */
 1470 | 	protected PreparedStatementSetter newArgTypePreparedStatementSetter(@Nullable Object @Nullable [] args, int[] argTypes) {
 1471 | 		return new ArgumentTypePreparedStatementSetter(args, argTypes);
 1472 | 	}
 1473 | 
 1474 | 	/**
 1475 | 	 * Handle warnings before propagating a primary {@code SQLException}
 1476 | 	 * from executing the given statement.
 1477 | 	 * <p>Calls regular {@link #handleWarnings(Statement)} but catches
 1478 | 	 * {@link SQLWarningException} in order to chain the {@link SQLWarning}
 1479 | 	 * into the primary exception instead.
 1480 | 	 * @param stmt the current JDBC statement
 1481 | 	 * @param ex the primary exception after failed statement execution
 1482 | 	 * @since 5.3.29
 1483 | 	 * @see #handleWarnings(Statement)
 1484 | 	 * @see SQLException#setNextException
 1485 | 	 */
 1486 | 	protected void handleWarnings(Statement stmt, SQLException ex) {
 1487 | 		try {
 1488 | 			handleWarnings(stmt);
 1489 | 		}
 1490 | 		catch (SQLWarningException nonIgnoredWarning) {
 1491 | 			ex.setNextException(nonIgnoredWarning.getSQLWarning());
 1492 | 		}
 1493 | 		catch (SQLException warningsEx) {
 1494 | 			logger.debug("Failed to retrieve warnings", warningsEx);
 1495 | 		}
 1496 | 		catch (Throwable warningsEx) {
 1497 | 			logger.debug("Failed to process warnings", warningsEx);
 1498 | 		}
 1499 | 	}
 1500 | 
 1501 | 	/**
 1502 | 	 * Handle the warnings for the given JDBC statement, if any.
 1503 | 	 * <p>Throws a {@link SQLWarningException} if we're not ignoring warnings,
 1504 | 	 * otherwise logs the warnings at debug level.
 1505 | 	 * @param stmt the current JDBC statement
 1506 | 	 * @throws SQLException in case of warnings retrieval failure
 1507 | 	 * @throws SQLWarningException for a concrete warning to raise
 1508 | 	 * (when not ignoring warnings)
 1509 | 	 * @see #setIgnoreWarnings
 1510 | 	 * @see #handleWarnings(SQLWarning)
 1511 | 	 */
 1512 | 	protected void handleWarnings(Statement stmt) throws SQLException, SQLWarningException {
 1513 | 		if (isIgnoreWarnings()) {
 1514 | 			if (logger.isDebugEnabled()) {
 1515 | 				SQLWarning warningToLog = stmt.getWarnings();
 1516 | 				while (warningToLog != null) {
 1517 | 					logger.debug("SQLWarning ignored: SQL state '" + warningToLog.getSQLState() + "', error code '" +
 1518 | 							warningToLog.getErrorCode() + "', message [" + warningToLog.getMessage() + "]");
 1519 | 					warningToLog = warningToLog.getNextWarning();
 1520 | 				}
 1521 | 			}
 1522 | 		}
 1523 | 		else {
 1524 | 			handleWarnings(stmt.getWarnings());
 1525 | 		}
 1526 | 	}
 1527 | 
 1528 | 	/**
 1529 | 	 * Throw a {@link SQLWarningException} if encountering an actual warning.
 1530 | 	 * @param warning the warnings object from the current statement.
 1531 | 	 * May be {@code null}, in which case this method does nothing.
 1532 | 	 * @throws SQLWarningException in case of an actual warning to be raised
 1533 | 	 */
 1534 | 	protected void handleWarnings(@Nullable SQLWarning warning) throws SQLWarningException {
 1535 | 		if (warning != null) {
 1536 | 			throw new SQLWarningException("Warning not ignored", warning);
 1537 | 		}
 1538 | 	}
 1539 | 
 1540 | 	/**
 1541 | 	 * Translate the given {@link SQLException} into a generic {@link DataAccessException}.
 1542 | 	 * @param task readable text describing the task being attempted
 1543 | 	 * @param sql the SQL query or update that caused the problem (may be {@code null})
 1544 | 	 * @param ex the offending {@code SQLException}
 1545 | 	 * @return a DataAccessException wrapping the {@code SQLException} (never {@code null})
 1546 | 	 * @since 5.0
 1547 | 	 * @see #getExceptionTranslator()
 1548 | 	 */
 1549 | 	protected DataAccessException translateException(String task, @Nullable String sql, SQLException ex) {
 1550 | 		DataAccessException dae = getExceptionTranslator().translate(task, sql, ex);
 1551 | 		return (dae != null ? dae : new UncategorizedSQLException(task, sql, ex));
 1552 | 	}
 1553 | 
 1554 | 
 1555 | 	/**
 1556 | 	 * Determine SQL from potential provider object.
 1557 | 	 * @param obj object which is potentially an SqlProvider
 1558 | 	 * @return the SQL string, or {@code null} if not known
 1559 | 	 * @see SqlProvider
 1560 | 	 */
 1561 | 	private static @Nullable String getSql(Object obj) {
 1562 | 		return (obj instanceof SqlProvider sqlProvider ? sqlProvider.getSql() : null);
 1563 | 	}
 1564 | 
 1565 | 	private static <T> T result(@Nullable T result) {
 1566 | 		Assert.state(result != null, "No result");
 1567 | 		return result;
 1568 | 	}
 1569 | 
 1570 | 	private static int updateCount(@Nullable Integer result) {
 1571 | 		Assert.state(result != null, "No update count");
 1572 | 		return result;
 1573 | 	}
 1574 | 
 1575 | 	private void storeGeneratedKeys(KeyHolder generatedKeyHolder, PreparedStatement ps, int rowsExpected)
 1576 | 			throws SQLException {
 1577 | 
 1578 | 		List<Map<String, Object>> generatedKeys = generatedKeyHolder.getKeyList();
 1579 | 		ResultSet keys = ps.getGeneratedKeys();
 1580 | 		if (keys != null) {
 1581 | 			try {
 1582 | 				RowMapperResultSetExtractor<Map<String, @Nullable Object>> rse =
 1583 | 						new RowMapperResultSetExtractor<>(getColumnMapRowMapper(), rowsExpected);
 1584 | 				generatedKeys.addAll(result(rse.extractData(keys)));
 1585 | 			}
 1586 | 			finally {
 1587 | 				JdbcUtils.closeResultSet(keys);
 1588 | 			}
 1589 | 		}
 1590 | 	}
 1591 | 
 1592 | 	private PreparedStatementCallback<int[]> getPreparedStatementCallback(BatchPreparedStatementSetter pss,
 1593 | 			@Nullable KeyHolder generatedKeyHolder) {
 1594 | 		return ps -> {
 1595 | 			try {
 1596 | 				int batchSize = pss.getBatchSize();
 1597 | 				InterruptibleBatchPreparedStatementSetter ipss =
 1598 | 						(pss instanceof InterruptibleBatchPreparedStatementSetter ibpss ? ibpss : null);
 1599 | 				if (generatedKeyHolder != null) {
 1600 | 					generatedKeyHolder.getKeyList().clear();
 1601 | 				}
 1602 | 				if (JdbcUtils.supportsBatchUpdates(ps.getConnection())) {
 1603 | 					for (int i = 0; i < batchSize; i++) {
 1604 | 						pss.setValues(ps, i);
 1605 | 						if (ipss != null && ipss.isBatchExhausted(i)) {
 1606 | 							break;
 1607 | 						}
 1608 | 						ps.addBatch();
 1609 | 					}
 1610 | 					int[] results = ps.executeBatch();
 1611 | 					if (generatedKeyHolder != null) {
 1612 | 						storeGeneratedKeys(generatedKeyHolder, ps, batchSize);
 1613 | 					}
 1614 | 					return results;
 1615 | 				}
 1616 | 				else {
 1617 | 					List<Integer> rowsAffected = new ArrayList<>();
 1618 | 					for (int i = 0; i < batchSize; i++) {
 1619 | 						pss.setValues(ps, i);
 1620 | 						if (ipss != null && ipss.isBatchExhausted(i)) {
 1621 | 							break;
 1622 | 						}
 1623 | 						rowsAffected.add(ps.executeUpdate());
 1624 | 						if (generatedKeyHolder != null) {
 1625 | 							storeGeneratedKeys(generatedKeyHolder, ps, 1);
 1626 | 						}
 1627 | 					}
 1628 | 					int[] rowsAffectedArray = new int[rowsAffected.size()];
 1629 | 					for (int i = 0; i < rowsAffectedArray.length; i++) {
 1630 | 						rowsAffectedArray[i] = rowsAffected.get(i);
 1631 | 					}
 1632 | 					return rowsAffectedArray;
 1633 | 				}
 1634 | 			}
 1635 | 			finally {
 1636 | 				if (pss instanceof ParameterDisposer parameterDisposer) {
 1637 | 					parameterDisposer.cleanupParameters();
 1638 | 				}
 1639 | 			}
 1640 | 		};
 1641 | 	}
 1642 | 
 1643 | 
 1644 | 	/**
 1645 | 	 * Invocation handler that suppresses close calls on JDBC Connections.
 1646 | 	 * Also prepares returned Statement (Prepared/CallbackStatement) objects.
 1647 | 	 * @see java.sql.Connection#close()
 1648 | 	 */
 1649 | 	private class CloseSuppressingInvocationHandler implements InvocationHandler {
 1650 | 
 1651 | 		private final Connection target;
 1652 | 
 1653 | 		public CloseSuppressingInvocationHandler(Connection target) {
 1654 | 			this.target = target;
 1655 | 		}
 1656 | 
 1657 | 		@Override
 1658 | 		public @Nullable Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
 1659 | 			// Invocation on ConnectionProxy interface coming in...
 1660 | 
 1661 | 			return switch (method.getName()) {
 1662 | 				// Only consider equal when proxies are identical.
 1663 | 				case "equals" -> (proxy == args[0]);
 1664 | 				// Use hashCode of Connection proxy.
 1665 | 				case "hashCode" -> System.identityHashCode(proxy);
 1666 | 				// Handle close method: suppress, not valid.
 1667 | 				case "close" -> null;
 1668 | 				case "isClosed" -> false;
 1669 | 				// Handle getTargetConnection method: return underlying Connection.
 1670 | 				case "getTargetConnection" -> this.target;
 1671 | 				case "unwrap" ->
 1672 | 						(((Class<?>) args[0]).isInstance(proxy) ? proxy : this.target.unwrap((Class<?>) args[0]));
 1673 | 				case "isWrapperFor" ->
 1674 | 						(((Class<?>) args[0]).isInstance(proxy) || this.target.isWrapperFor((Class<?>) args[0]));
 1675 | 				default -> {
 1676 | 					try {
 1677 | 						// Invoke method on target Connection.
 1678 | 						Object retVal = method.invoke(this.target, args);
 1679 | 
 1680 | 						// If return value is a JDBC Statement, apply statement settings
 1681 | 						// (fetch size, max rows, transaction timeout).
 1682 | 						if (retVal instanceof Statement statement) {
 1683 | 							applyStatementSettings(statement);
 1684 | 						}
 1685 | 
 1686 | 						yield retVal;
 1687 | 					}
 1688 | 					catch (InvocationTargetException ex) {
 1689 | 						throw ex.getTargetException();
 1690 | 					}
 1691 | 				}
 1692 | 			};
 1693 | 		}
 1694 | 	}
 1695 | 
 1696 | 
 1697 | 	/**
 1698 | 	 * Simple adapter for PreparedStatementCreator, allowing to use a plain SQL statement.
 1699 | 	 */
 1700 | 	private static class SimplePreparedStatementCreator implements PreparedStatementCreator, SqlProvider {
 1701 | 
 1702 | 		private final String sql;
 1703 | 
 1704 | 		public SimplePreparedStatementCreator(String sql) {
 1705 | 			Assert.notNull(sql, "SQL must not be null");
 1706 | 			this.sql = sql;
 1707 | 		}
 1708 | 
 1709 | 		@Override
 1710 | 		public PreparedStatement createPreparedStatement(Connection con) throws SQLException {
 1711 | 			return con.prepareStatement(this.sql);
 1712 | 		}
 1713 | 
 1714 | 		@Override
 1715 | 		public String getSql() {
 1716 | 			return this.sql;
 1717 | 		}
 1718 | 	}
 1719 | 
 1720 | 
 1721 | 	/**
 1722 | 	 * Simple adapter for CallableStatementCreator, allowing to use a plain SQL statement.
 1723 | 	 */
 1724 | 	private static class SimpleCallableStatementCreator implements CallableStatementCreator, SqlProvider {
 1725 | 
 1726 | 		private final String callString;
 1727 | 
 1728 | 		public SimpleCallableStatementCreator(String callString) {
 1729 | 			Assert.notNull(callString, "Call string must not be null");
 1730 | 			this.callString = callString;
 1731 | 		}
 1732 | 
 1733 | 		@Override
 1734 | 		public CallableStatement createCallableStatement(Connection con) throws SQLException {
 1735 | 			return con.prepareCall(this.callString);
 1736 | 		}
 1737 | 
 1738 | 		@Override
 1739 | 		public String getSql() {
 1740 | 			return this.callString;
 1741 | 		}
 1742 | 	}
 1743 | 
 1744 | 
 1745 | 	/**
 1746 | 	 * Adapter to enable use of a RowCallbackHandler inside a ResultSetExtractor.
 1747 | 	 * <p>Uses a regular ResultSet, so we have to be careful when using it:
 1748 | 	 * We don't use it for navigating since this could lead to unpredictable consequences.
 1749 | 	 */
 1750 | 	private static class RowCallbackHandlerResultSetExtractor implements ResultSetExtractor<@Nullable Object> {
 1751 | 
 1752 | 		private final RowCallbackHandler rch;
 1753 | 
 1754 | 		private final int maxRows;
 1755 | 
 1756 | 		public RowCallbackHandlerResultSetExtractor(RowCallbackHandler rch, int maxRows) {
 1757 | 			this.rch = rch;
 1758 | 			this.maxRows = maxRows;
 1759 | 		}
 1760 | 
 1761 | 		@Override
 1762 | 		public @Nullable Object extractData(ResultSet rs) throws SQLException {
 1763 | 			int processed = 0;
 1764 | 			while (rs.next() && (this.maxRows == -1 || (processed++) < this.maxRows)) {
 1765 | 				this.rch.processRow(rs);
 1766 | 			}
 1767 | 			return null;
 1768 | 		}
 1769 | 	}
 1770 | 
 1771 | 
 1772 | 	/**
 1773 | 	 * Spliterator for queryForStream adaptation of a ResultSet to a Stream.
 1774 | 	 * @since 5.3
 1775 | 	 */
 1776 | 	private static class ResultSetSpliterator<T> implements Spliterator<T> {
 1777 | 
 1778 | 		private final ResultSet rs;
 1779 | 
 1780 | 		private final RowMapper<T> rowMapper;
 1781 | 
 1782 | 		private final int maxRows;
 1783 | 
 1784 | 		private int rowNum = 0;
 1785 | 
 1786 | 		public ResultSetSpliterator(ResultSet rs, RowMapper<T> rowMapper, int maxRows) {
 1787 | 			this.rs = rs;
 1788 | 			this.rowMapper = rowMapper;
 1789 | 			this.maxRows = maxRows;
 1790 | 		}
 1791 | 
 1792 | 		@Override
 1793 | 		public boolean tryAdvance(Consumer<? super T> action) {
 1794 | 			try {
 1795 | 				if (this.rs.next() && (this.maxRows == -1 || this.rowNum < this.maxRows)) {
 1796 | 					action.accept(this.rowMapper.mapRow(this.rs, this.rowNum++));
 1797 | 					return true;
 1798 | 				}
 1799 | 				return false;
 1800 | 			}
 1801 | 			catch (SQLException ex) {
 1802 | 				throw new InvalidResultSetAccessException(ex);
 1803 | 			}
 1804 | 		}
 1805 | 
 1806 | 		@Override
 1807 | 		public @Nullable Spliterator<T> trySplit() {
 1808 | 			return null;
 1809 | 		}
 1810 | 
 1811 | 		@Override
 1812 | 		public long estimateSize() {
 1813 | 			return Long.MAX_VALUE;
 1814 | 		}
 1815 | 
 1816 | 		@Override
 1817 | 		public int characteristics() {
 1818 | 			return Spliterator.ORDERED;
 1819 | 		}
 1820 | 
 1821 | 		public Stream<T> stream() {
 1822 | 			return StreamSupport.stream(this, false);
 1823 | 		}
 1824 | 	}
 1825 | 
 1826 | }
```
