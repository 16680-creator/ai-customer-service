<!-- learning-docs source archive（机械抓取原文，禁止手改代码内容） -->
<!-- origin: org.springframework:spring-jdbc:7.1.0 | path: org/springframework/jdbc/core/JdbcOperations.java -->
<!-- 正文引用：[[源码:JdbcOperations.java#L起-L止]] ；代码块头部：(src: JdbcOperations.java#L起-L止) -->

# JdbcOperations.java

| 项 | 值 |
| --- | --- |
| 来源 | `org.springframework:spring-jdbc:7.1.0` |
| 路径 | `org/springframework/jdbc/core/JdbcOperations.java` |
| 行数 | 1098 |
| 在线地址 | https://repo1.maven.org/maven2/org/springframework/spring-jdbc/7.1.0/spring-jdbc-7.1.0-sources.jar |
| 抓取时间 | 2026-11-25T14:00:09.248Z |

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
   19 | import java.util.Collection;
   20 | import java.util.List;
   21 | import java.util.Map;
   22 | import java.util.stream.Stream;
   23 | 
   24 | import org.jspecify.annotations.Nullable;
   25 | 
   26 | import org.springframework.dao.DataAccessException;
   27 | import org.springframework.jdbc.support.KeyHolder;
   28 | import org.springframework.jdbc.support.rowset.SqlRowSet;
   29 | 
   30 | /**
   31 |  * Interface specifying a basic set of JDBC operations.
   32 |  *
   33 |  * <p>Implemented by {@link JdbcTemplate}. Not often used directly, but a useful
   34 |  * option to enhance testability, as it can easily be mocked or stubbed.
   35 |  *
   36 |  * <p>Alternatively, the standard JDBC infrastructure can be mocked.
   37 |  * However, mocking this interface constitutes significantly less work.
   38 |  * As an alternative to a mock objects approach to testing data access code,
   39 |  * consider the powerful integration testing support provided via the
   40 |  * <em>Spring TestContext Framework</em>, in the {@code spring-test} artifact.
   41 |  *
   42 |  * <p><b>NOTE: As of 6.1, there is a unified JDBC access facade available in
   43 |  * the form of {@link org.springframework.jdbc.core.simple.JdbcClient}.</b>
   44 |  * {@code JdbcClient} provides a fluent API style for common JDBC queries/updates
   45 |  * with flexible use of indexed or named parameters. It delegates to
   46 |  * {@code JdbcOperations}/{@code NamedParameterJdbcOperations} for actual execution.
   47 |  *
   48 |  * @author Rod Johnson
   49 |  * @author Juergen Hoeller
   50 |  * @see JdbcTemplate
   51 |  * @see org.springframework.jdbc.core.namedparam.NamedParameterJdbcOperations
   52 |  */
   53 | public interface JdbcOperations {
   54 | 
   55 | 	//-------------------------------------------------------------------------
   56 | 	// Methods dealing with a plain java.sql.Connection
   57 | 	//-------------------------------------------------------------------------
   58 | 
   59 | 	/**
   60 | 	 * Execute a JDBC data access operation, implemented as callback action
   61 | 	 * working on a JDBC Connection. This allows for implementing arbitrary
   62 | 	 * data access operations, within Spring's managed JDBC environment:
   63 | 	 * that is, participating in Spring-managed transactions and converting
   64 | 	 * JDBC SQLExceptions into Spring's DataAccessException hierarchy.
   65 | 	 * <p>The callback action can return a result object, for example a domain
   66 | 	 * object or a collection of domain objects.
   67 | 	 * @param action a callback object that specifies the action
   68 | 	 * @return a result object returned by the action, or {@code null} if none
   69 | 	 * @throws DataAccessException if there is any problem
   70 | 	 */
   71 | 	<T extends @Nullable Object> T execute(ConnectionCallback<T> action) throws DataAccessException;
   72 | 
   73 | 
   74 | 	//-------------------------------------------------------------------------
   75 | 	// Methods dealing with static SQL (java.sql.Statement)
   76 | 	//-------------------------------------------------------------------------
   77 | 
   78 | 	/**
   79 | 	 * Execute a JDBC data access operation, implemented as callback action
   80 | 	 * working on a JDBC Statement. This allows for implementing arbitrary data
   81 | 	 * access operations on a single Statement, within Spring's managed JDBC
   82 | 	 * environment: that is, participating in Spring-managed transactions and
   83 | 	 * converting JDBC SQLExceptions into Spring's DataAccessException hierarchy.
   84 | 	 * <p>The callback action can return a result object, for example a domain
   85 | 	 * object or a collection of domain objects.
   86 | 	 * @param action a callback that specifies the action
   87 | 	 * @return a result object returned by the action, or {@code null} if none
   88 | 	 * @throws DataAccessException if there is any problem
   89 | 	 */
   90 | 	<T extends @Nullable Object> T execute(StatementCallback<T> action) throws DataAccessException;
   91 | 
   92 | 	/**
   93 | 	 * Issue a single SQL execute, typically a DDL statement.
   94 | 	 * @param sql static SQL to execute
   95 | 	 * @throws DataAccessException if there is any problem
   96 | 	 */
   97 | 	void execute(String sql) throws DataAccessException;
   98 | 
   99 | 	/**
  100 | 	 * Execute a query given static SQL, reading the ResultSet with a
  101 | 	 * ResultSetExtractor.
  102 | 	 * <p>Uses a JDBC Statement, not a PreparedStatement. If you want to
  103 | 	 * execute a static query with a PreparedStatement, use the overloaded
  104 | 	 * {@code query} method with {@code null} as argument array.
  105 | 	 * @param sql the SQL query to execute
  106 | 	 * @param rse a callback that will extract all rows of results
  107 | 	 * @return an arbitrary result object, as returned by the ResultSetExtractor
  108 | 	 * @throws DataAccessException if there is any problem executing the query
  109 | 	 * @see #query(String, ResultSetExtractor, Object...)
  110 | 	 */
  111 | 	<T extends @Nullable Object> T query(String sql, ResultSetExtractor<T> rse) throws DataAccessException;
  112 | 
  113 | 	/**
  114 | 	 * Execute a query given static SQL, reading the ResultSet on a per-row
  115 | 	 * basis with a RowCallbackHandler.
  116 | 	 * <p>Uses a JDBC Statement, not a PreparedStatement. If you want to
  117 | 	 * execute a static query with a PreparedStatement, use the overloaded
  118 | 	 * {@code query} method with {@code null} as argument array.
  119 | 	 * @param sql the SQL query to execute
  120 | 	 * @param rch a callback that will extract results, one row at a time
  121 | 	 * @throws DataAccessException if there is any problem executing the query
  122 | 	 * @see #query(String, RowCallbackHandler, Object...)
  123 | 	 */
  124 | 	void query(String sql, RowCallbackHandler rch) throws DataAccessException;
  125 | 
  126 | 	/**
  127 | 	 * Execute a query given static SQL, mapping each row to a result object
  128 | 	 * via a RowMapper.
  129 | 	 * <p>Uses a JDBC Statement, not a PreparedStatement. If you want to
  130 | 	 * execute a static query with a PreparedStatement, use the overloaded
  131 | 	 * {@code query} method with {@code null} as argument array.
  132 | 	 * @param sql the SQL query to execute
  133 | 	 * @param rowMapper a callback that will map one object per row
  134 | 	 * @return the result List, containing mapped objects
  135 | 	 * @throws DataAccessException if there is any problem executing the query
  136 | 	 * @see #query(String, RowMapper, Object...)
  137 | 	 */
  138 | 	<T extends @Nullable Object> List<T> query(String sql, RowMapper<T> rowMapper) throws DataAccessException;
  139 | 
  140 | 	/**
  141 | 	 * Execute a query given static SQL, mapping each row to a result object
  142 | 	 * via a RowMapper, and turning it into an iterable and closeable Stream.
  143 | 	 * <p>Uses a JDBC Statement, not a PreparedStatement. If you want to
  144 | 	 * execute a static query with a PreparedStatement, use the overloaded
  145 | 	 * {@code query} method with {@code null} as argument array.
  146 | 	 * @param sql the SQL query to execute
  147 | 	 * @param rowMapper a callback that will map one object per row
  148 | 	 * @return the result Stream, containing mapped objects, needing to be
  149 | 	 * closed once fully processed (for example, through a try-with-resources clause)
  150 | 	 * @throws DataAccessException if there is any problem executing the query
  151 | 	 * @since 5.3
  152 | 	 * @see #queryForStream(String, RowMapper, Object...)
  153 | 	 */
  154 | 	<T extends @Nullable Object> Stream<T> queryForStream(String sql, RowMapper<T> rowMapper) throws DataAccessException;
  155 | 
  156 | 	/**
  157 | 	 * Execute a query given static SQL, mapping a single result row to a
  158 | 	 * result object via a RowMapper.
  159 | 	 * <p>Uses a JDBC Statement, not a PreparedStatement. If you want to
  160 | 	 * execute a static query with a PreparedStatement, use the overloaded
  161 | 	 * {@link #queryForObject(String, RowMapper, Object...)} method with
  162 | 	 * {@code null} as argument array.
  163 | 	 * @param sql the SQL query to execute
  164 | 	 * @param rowMapper a callback that will map one object per row
  165 | 	 * @return the single mapped object (may be {@code null} if the given
  166 | 	 * {@link RowMapper} returned {@code null})
  167 | 	 * @throws org.springframework.dao.IncorrectResultSizeDataAccessException
  168 | 	 * if the query does not return exactly one row
  169 | 	 * @throws DataAccessException if there is any problem executing the query
  170 | 	 * @see #queryForObject(String, RowMapper, Object...)
  171 | 	 */
  172 | 	<T extends @Nullable Object> T queryForObject(String sql, RowMapper<T> rowMapper) throws DataAccessException;
  173 | 
  174 | 	/**
  175 | 	 * Execute a query for a result object, given static SQL.
  176 | 	 * <p>Uses a JDBC Statement, not a PreparedStatement. If you want to
  177 | 	 * execute a static query with a PreparedStatement, use the overloaded
  178 | 	 * {@link #queryForObject(String, Class, Object...)} method with
  179 | 	 * {@code null} as argument array.
  180 | 	 * <p>This method is useful for running static SQL with a known outcome.
  181 | 	 * The query is expected to be a single row/single column query; the returned
  182 | 	 * result will be directly mapped to the corresponding object type.
  183 | 	 * @param sql the SQL query to execute
  184 | 	 * @param requiredType the type that the result object is expected to match
  185 | 	 * @return the result object of the required type, or {@code null} in case of SQL NULL
  186 | 	 * @throws org.springframework.dao.IncorrectResultSizeDataAccessException
  187 | 	 * if the query does not return exactly one row
  188 | 	 * @throws org.springframework.jdbc.IncorrectResultSetColumnCountException
  189 | 	 * if the query does not return a row containing a single column
  190 | 	 * @throws DataAccessException if there is any problem executing the query
  191 | 	 * @see #queryForObject(String, Class, Object...)
  192 | 	 */
  193 | 	<T> @Nullable T queryForObject(String sql, Class<T> requiredType) throws DataAccessException;
  194 | 
  195 | 	/**
  196 | 	 * Execute a query for a result map, given static SQL.
  197 | 	 * <p>Uses a JDBC Statement, not a PreparedStatement. If you want to
  198 | 	 * execute a static query with a PreparedStatement, use the overloaded
  199 | 	 * {@link #queryForMap(String, Object...)} method with {@code null}
  200 | 	 * as argument array.
  201 | 	 * <p>The query is expected to be a single row query; the result row will be
  202 | 	 * mapped to a Map (one entry for each column, using the column name as the key).
  203 | 	 * @param sql the SQL query to execute
  204 | 	 * @return the result Map (one entry per column, with column name as key)
  205 | 	 * @throws org.springframework.dao.IncorrectResultSizeDataAccessException
  206 | 	 * if the query does not return exactly one row
  207 | 	 * @throws DataAccessException if there is any problem executing the query
  208 | 	 * @see #queryForMap(String, Object...)
  209 | 	 * @see ColumnMapRowMapper
  210 | 	 */
  211 | 	Map<String, @Nullable Object> queryForMap(String sql) throws DataAccessException;
  212 | 
  213 | 	/**
  214 | 	 * Execute a query for a result list, given static SQL.
  215 | 	 * <p>Uses a JDBC Statement, not a PreparedStatement. If you want to
  216 | 	 * execute a static query with a PreparedStatement, use the overloaded
  217 | 	 * {@code queryForList} method with {@code null} as argument array.
  218 | 	 * <p>The results will be mapped to a List (one entry for each row) of
  219 | 	 * result objects, each of them matching the specified element type.
  220 | 	 * @param sql the SQL query to execute
  221 | 	 * @param elementType the required type of element in the result list
  222 | 	 * (for example, {@code Integer.class})
  223 | 	 * @return a List of objects that match the specified element type
  224 | 	 * @throws DataAccessException if there is any problem executing the query
  225 | 	 * @see #queryForList(String, Class, Object...)
  226 | 	 * @see SingleColumnRowMapper
  227 | 	 */
  228 | 	<T> List<@Nullable T> queryForList(String sql, Class<T> elementType) throws DataAccessException;
  229 | 
  230 | 	/**
  231 | 	 * Execute a query for a result list, given static SQL.
  232 | 	 * <p>Uses a JDBC Statement, not a PreparedStatement. If you want to
  233 | 	 * execute a static query with a PreparedStatement, use the overloaded
  234 | 	 * {@code queryForList} method with {@code null} as argument array.
  235 | 	 * <p>The results will be mapped to a List (one entry for each row) of
  236 | 	 * Maps (one entry for each column using the column name as the key).
  237 | 	 * Each element in the list will be of the form returned by this interface's
  238 | 	 * {@code queryForMap} methods.
  239 | 	 * @param sql the SQL query to execute
  240 | 	 * @return a List that contains a Map per row
  241 | 	 * @throws DataAccessException if there is any problem executing the query
  242 | 	 * @see #queryForList(String, Object...)
  243 | 	 */
  244 | 	List<Map<String, @Nullable Object>> queryForList(String sql) throws DataAccessException;
  245 | 
  246 | 	/**
  247 | 	 * Execute a query for an SqlRowSet, given static SQL.
  248 | 	 * <p>Uses a JDBC Statement, not a PreparedStatement. If you want to
  249 | 	 * execute a static query with a PreparedStatement, use the overloaded
  250 | 	 * {@code queryForRowSet} method with {@code null} as argument array.
  251 | 	 * <p>The results will be mapped to an SqlRowSet which holds the data in a
  252 | 	 * disconnected fashion. This wrapper will translate any SQLExceptions thrown.
  253 | 	 * <p>Note that, for the default implementation, JDBC RowSet support needs to
  254 | 	 * be available at runtime: by default, a standard JDBC {@code CachedRowSet}
  255 | 	 * is used.
  256 | 	 * @param sql the SQL query to execute
  257 | 	 * @return an SqlRowSet representation (possibly a wrapper around a
  258 | 	 * {@code javax.sql.rowset.CachedRowSet})
  259 | 	 * @throws DataAccessException if there is any problem executing the query
  260 | 	 * @see #queryForRowSet(String, Object...)
  261 | 	 * @see SqlRowSetResultSetExtractor
  262 | 	 * @see javax.sql.rowset.CachedRowSet
  263 | 	 */
  264 | 	SqlRowSet queryForRowSet(String sql) throws DataAccessException;
  265 | 
  266 | 	/**
  267 | 	 * Issue a single SQL update operation (such as an insert, update or delete statement).
  268 | 	 * @param sql static SQL to execute
  269 | 	 * @return the number of rows affected
  270 | 	 * @throws DataAccessException if there is any problem.
  271 | 	 */
  272 | 	int update(String sql) throws DataAccessException;
  273 | 
  274 | 	/**
  275 | 	 * Issue multiple SQL updates on a single JDBC Statement using batching.
  276 | 	 * <p>Will fall back to separate updates on a single Statement if the JDBC
  277 | 	 * driver does not support batch updates.
  278 | 	 * @param sql defining an array of SQL statements that will be executed.
  279 | 	 * @return an array of the number of rows affected by each statement
  280 | 	 * @throws DataAccessException if there is any problem executing the batch
  281 | 	 */
  282 | 	int[] batchUpdate(String... sql) throws DataAccessException;
  283 | 
  284 | 
  285 | 	//-------------------------------------------------------------------------
  286 | 	// Methods dealing with prepared statements
  287 | 	//-------------------------------------------------------------------------
  288 | 
  289 | 	/**
  290 | 	 * Execute a JDBC data access operation, implemented as callback action
  291 | 	 * working on a JDBC PreparedStatement. This allows for implementing arbitrary
  292 | 	 * data access operations on a single Statement, within Spring's managed JDBC
  293 | 	 * environment: that is, participating in Spring-managed transactions and
  294 | 	 * converting JDBC SQLExceptions into Spring's DataAccessException hierarchy.
  295 | 	 * <p>The callback action can return a result object, for example a domain
  296 | 	 * object or a collection of domain objects.
  297 | 	 * @param psc a callback that creates a PreparedStatement given a Connection
  298 | 	 * @param action a callback that specifies the action
  299 | 	 * @return a result object returned by the action, or {@code null} if none
  300 | 	 * @throws DataAccessException if there is any problem
  301 | 	 */
  302 | 	<T extends @Nullable Object> T execute(PreparedStatementCreator psc, PreparedStatementCallback<T> action) throws DataAccessException;
  303 | 
  304 | 	/**
  305 | 	 * Execute a JDBC data access operation, implemented as callback action
  306 | 	 * working on a JDBC PreparedStatement. This allows for implementing arbitrary
  307 | 	 * data access operations on a single Statement, within Spring's managed JDBC
  308 | 	 * environment: that is, participating in Spring-managed transactions and
  309 | 	 * converting JDBC SQLExceptions into Spring's DataAccessException hierarchy.
  310 | 	 * <p>The callback action can return a result object, for example a domain
  311 | 	 * object or a collection of domain objects.
  312 | 	 * @param sql the SQL to execute
  313 | 	 * @param action a callback that specifies the action
  314 | 	 * @return a result object returned by the action, or {@code null} if none
  315 | 	 * @throws DataAccessException if there is any problem
  316 | 	 */
  317 | 	<T extends @Nullable Object> T execute(String sql, PreparedStatementCallback<T> action) throws DataAccessException;
  318 | 
  319 | 	/**
  320 | 	 * Query using a prepared statement, reading the ResultSet with a ResultSetExtractor.
  321 | 	 * <p>A PreparedStatementCreator can either be implemented directly or
  322 | 	 * configured through a PreparedStatementCreatorFactory.
  323 | 	 * @param psc a callback that creates a PreparedStatement given a Connection
  324 | 	 * @param rse a callback that will extract results
  325 | 	 * @return an arbitrary result object, as returned by the ResultSetExtractor
  326 | 	 * @throws DataAccessException if there is any problem
  327 | 	 * @see PreparedStatementCreatorFactory
  328 | 	 */
  329 | 	<T extends @Nullable Object> T query(PreparedStatementCreator psc, ResultSetExtractor<T> rse) throws DataAccessException;
  330 | 
  331 | 	/**
  332 | 	 * Query using a prepared statement, reading the ResultSet with a ResultSetExtractor.
  333 | 	 * @param sql the SQL query to execute
  334 | 	 * @param pss a callback that knows how to set values on the prepared statement.
  335 | 	 * If this is {@code null}, the SQL will be assumed to contain no bind parameters.
  336 | 	 * Even if there are no bind parameters, this callback may be used to set the
  337 | 	 * fetch size and other performance options.
  338 | 	 * @param rse a callback that will extract results
  339 | 	 * @return an arbitrary result object, as returned by the ResultSetExtractor
  340 | 	 * @throws DataAccessException if there is any problem
  341 | 	 */
  342 | 	<T extends @Nullable Object> T query(String sql, @Nullable PreparedStatementSetter pss, ResultSetExtractor<T> rse)
  343 | 			throws DataAccessException;
  344 | 
  345 | 	/**
  346 | 	 * Query given SQL to create a prepared statement from SQL and a list of arguments
  347 | 	 * to bind to the query, reading the ResultSet with a ResultSetExtractor.
  348 | 	 * @param sql the SQL query to execute
  349 | 	 * @param args arguments to bind to the query
  350 | 	 * @param argTypes the SQL types of the arguments
  351 | 	 * (constants from {@code java.sql.Types})
  352 | 	 * @param rse a callback that will extract results
  353 | 	 * @return an arbitrary result object, as returned by the ResultSetExtractor
  354 | 	 * @throws DataAccessException if the query fails
  355 | 	 * @see java.sql.Types
  356 | 	 */
  357 | 	<T extends @Nullable Object> T query(String sql, @Nullable Object @Nullable [] args, int[] argTypes, ResultSetExtractor<T> rse) throws DataAccessException;
  358 | 
  359 | 	/**
  360 | 	 * Query given SQL to create a prepared statement from SQL and a list of arguments
  361 | 	 * to bind to the query, reading the ResultSet with a ResultSetExtractor.
  362 | 	 * @param sql the SQL query to execute
  363 | 	 * @param args arguments to bind to the query
  364 | 	 * (leaving it to the PreparedStatement to guess the corresponding SQL type);
  365 | 	 * may also contain {@link SqlParameterValue} objects which indicate not
  366 | 	 * only the argument value but also the SQL type and optionally the scale
  367 | 	 * @param rse a callback that will extract results
  368 | 	 * @return an arbitrary result object, as returned by the ResultSetExtractor
  369 | 	 * @throws DataAccessException if the query fails
  370 | 	 * @deprecated in favor of {@link #query(String, ResultSetExtractor, Object...)}
  371 | 	 */
  372 | 	@Deprecated(since = "5.3")
  373 | 	<T extends @Nullable Object> T query(String sql, @Nullable Object @Nullable [] args, ResultSetExtractor<T> rse) throws DataAccessException;
  374 | 
  375 | 	/**
  376 | 	 * Query given SQL to create a prepared statement from SQL and a list of arguments
  377 | 	 * to bind to the query, reading the ResultSet with a ResultSetExtractor.
  378 | 	 * @param sql the SQL query to execute
  379 | 	 * @param rse a callback that will extract results
  380 | 	 * @param args arguments to bind to the query
  381 | 	 * (leaving it to the PreparedStatement to guess the corresponding SQL type);
  382 | 	 * may also contain {@link SqlParameterValue} objects which indicate not
  383 | 	 * only the argument value but also the SQL type and optionally the scale
  384 | 	 * @return an arbitrary result object, as returned by the ResultSetExtractor
  385 | 	 * @throws DataAccessException if the query fails
  386 | 	 * @since 3.0.1
  387 | 	 */
  388 | 	<T extends @Nullable Object> T query(String sql, ResultSetExtractor<T> rse, @Nullable Object @Nullable ... args) throws DataAccessException;
  389 | 
  390 | 	/**
  391 | 	 * Query using a prepared statement, reading the ResultSet on a per-row basis
  392 | 	 * with a RowCallbackHandler.
  393 | 	 * <p>A PreparedStatementCreator can either be implemented directly or
  394 | 	 * configured through a PreparedStatementCreatorFactory.
  395 | 	 * @param psc a callback that creates a PreparedStatement given a Connection
  396 | 	 * @param rch a callback that will extract results, one row at a time
  397 | 	 * @throws DataAccessException if there is any problem
  398 | 	 * @see PreparedStatementCreatorFactory
  399 | 	 */
  400 | 	void query(PreparedStatementCreator psc, RowCallbackHandler rch) throws DataAccessException;
  401 | 
  402 | 	/**
  403 | 	 * Query given SQL to create a prepared statement from SQL and a
  404 | 	 * PreparedStatementSetter implementation that knows how to bind values to the
  405 | 	 * query, reading the ResultSet on a per-row basis with a RowCallbackHandler.
  406 | 	 * @param sql the SQL query to execute
  407 | 	 * @param pss a callback that knows how to set values on the prepared statement.
  408 | 	 * If this is {@code null}, the SQL will be assumed to contain no bind parameters.
  409 | 	 * Even if there are no bind parameters, this callback may be used to set the
  410 | 	 * fetch size and other performance options.
  411 | 	 * @param rch a callback that will extract results, one row at a time
  412 | 	 * @throws DataAccessException if the query fails
  413 | 	 */
  414 | 	void query(String sql, @Nullable PreparedStatementSetter pss, RowCallbackHandler rch) throws DataAccessException;
  415 | 
  416 | 	/**
  417 | 	 * Query given SQL to create a prepared statement from SQL and a list of
  418 | 	 * arguments to bind to the query, reading the ResultSet on a per-row basis
  419 | 	 * with a RowCallbackHandler.
  420 | 	 * @param sql the SQL query to execute
  421 | 	 * @param args arguments to bind to the query
  422 | 	 * @param argTypes the SQL types of the arguments
  423 | 	 * (constants from {@code java.sql.Types})
  424 | 	 * @param rch a callback that will extract results, one row at a time
  425 | 	 * @throws DataAccessException if the query fails
  426 | 	 * @see java.sql.Types
  427 | 	 */
  428 | 	void query(String sql, @Nullable Object @Nullable [] args, int[] argTypes, RowCallbackHandler rch) throws DataAccessException;
  429 | 
  430 | 	/**
  431 | 	 * Query given SQL to create a prepared statement from SQL and a list of
  432 | 	 * arguments to bind to the query, reading the ResultSet on a per-row basis
  433 | 	 * with a RowCallbackHandler.
  434 | 	 * @param sql the SQL query to execute
  435 | 	 * @param args arguments to bind to the query
  436 | 	 * (leaving it to the PreparedStatement to guess the corresponding SQL type);
  437 | 	 * may also contain {@link SqlParameterValue} objects which indicate not
  438 | 	 * only the argument value but also the SQL type and optionally the scale
  439 | 	 * @param rch a callback that will extract results, one row at a time
  440 | 	 * @throws DataAccessException if the query fails
  441 | 	 * @deprecated in favor of {@link #query(String, RowCallbackHandler, Object...)}
  442 | 	 */
  443 | 	@Deprecated(since = "5.3")
  444 | 	void query(String sql, @Nullable Object @Nullable [] args, RowCallbackHandler rch) throws DataAccessException;
  445 | 
  446 | 	/**
  447 | 	 * Query given SQL to create a prepared statement from SQL and a list of
  448 | 	 * arguments to bind to the query, reading the ResultSet on a per-row basis
  449 | 	 * with a RowCallbackHandler.
  450 | 	 * @param sql the SQL query to execute
  451 | 	 * @param rch a callback that will extract results, one row at a time
  452 | 	 * @param args arguments to bind to the query
  453 | 	 * (leaving it to the PreparedStatement to guess the corresponding SQL type);
  454 | 	 * may also contain {@link SqlParameterValue} objects which indicate not
  455 | 	 * only the argument value but also the SQL type and optionally the scale
  456 | 	 * @throws DataAccessException if the query fails
  457 | 	 * @since 3.0.1
  458 | 	 */
  459 | 	void query(String sql, RowCallbackHandler rch, @Nullable Object @Nullable ... args) throws DataAccessException;
  460 | 
  461 | 	/**
  462 | 	 * Query using a prepared statement, mapping each row to a result object
  463 | 	 * via a RowMapper.
  464 | 	 * <p>A PreparedStatementCreator can either be implemented directly or
  465 | 	 * configured through a PreparedStatementCreatorFactory.
  466 | 	 * @param psc a callback that creates a PreparedStatement given a Connection
  467 | 	 * @param rowMapper a callback that will map one object per row
  468 | 	 * @return the result List, containing mapped objects
  469 | 	 * @throws DataAccessException if there is any problem
  470 | 	 * @see PreparedStatementCreatorFactory
  471 | 	 */
  472 | 	<T extends @Nullable Object> List<T> query(PreparedStatementCreator psc, RowMapper<T> rowMapper) throws DataAccessException;
  473 | 
  474 | 	/**
  475 | 	 * Query given SQL to create a prepared statement from SQL and a
  476 | 	 * PreparedStatementSetter implementation that knows how to bind values
  477 | 	 * to the query, mapping each row to a result object via a RowMapper.
  478 | 	 * @param sql the SQL query to execute
  479 | 	 * @param pss a callback that knows how to set values on the prepared statement.
  480 | 	 * If this is {@code null}, the SQL will be assumed to contain no bind parameters.
  481 | 	 * Even if there are no bind parameters, this callback may be used to set the
  482 | 	 * fetch size and other performance options.
  483 | 	 * @param rowMapper a callback that will map one object per row
  484 | 	 * @return the result List, containing mapped objects
  485 | 	 * @throws DataAccessException if the query fails
  486 | 	 */
  487 | 	<T extends @Nullable Object> List<T> query(String sql, @Nullable PreparedStatementSetter pss, RowMapper<T> rowMapper)
  488 | 			throws DataAccessException;
  489 | 
  490 | 	/**
  491 | 	 * Query given SQL to create a prepared statement from SQL and a list of
  492 | 	 * arguments to bind to the query, mapping each row to a result object
  493 | 	 * via a RowMapper.
  494 | 	 * @param sql the SQL query to execute
  495 | 	 * @param args arguments to bind to the query
  496 | 	 * @param argTypes the SQL types of the arguments
  497 | 	 * (constants from {@code java.sql.Types})
  498 | 	 * @param rowMapper a callback that will map one object per row
  499 | 	 * @return the result List, containing mapped objects
  500 | 	 * @throws DataAccessException if the query fails
  501 | 	 * @see java.sql.Types
  502 | 	 */
  503 | 	<T extends @Nullable Object> List<T> query(String sql, @Nullable Object @Nullable [] args, int[] argTypes, RowMapper<T> rowMapper) throws DataAccessException;
  504 | 
  505 | 	/**
  506 | 	 * Query given SQL to create a prepared statement from SQL and a list of
  507 | 	 * arguments to bind to the query, mapping each row to a result object
  508 | 	 * via a RowMapper.
  509 | 	 * @param sql the SQL query to execute
  510 | 	 * @param args arguments to bind to the query
  511 | 	 * (leaving it to the PreparedStatement to guess the corresponding SQL type);
  512 | 	 * may also contain {@link SqlParameterValue} objects which indicate not
  513 | 	 * only the argument value but also the SQL type and optionally the scale
  514 | 	 * @param rowMapper a callback that will map one object per row
  515 | 	 * @return the result List, containing mapped objects
  516 | 	 * @throws DataAccessException if the query fails
  517 | 	 * @deprecated in favor of {@link #query(String, RowMapper, Object...)}
  518 | 	 */
  519 | 	@Deprecated(since = "5.3")
  520 | 	<T extends @Nullable Object> List<T> query(String sql, @Nullable Object @Nullable [] args, RowMapper<T> rowMapper) throws DataAccessException;
  521 | 
  522 | 	/**
  523 | 	 * Query given SQL to create a prepared statement from SQL and a list of
  524 | 	 * arguments to bind to the query, mapping each row to a result object
  525 | 	 * via a RowMapper.
  526 | 	 * @param sql the SQL query to execute
  527 | 	 * @param rowMapper a callback that will map one object per row
  528 | 	 * @param args arguments to bind to the query
  529 | 	 * (leaving it to the PreparedStatement to guess the corresponding SQL type);
  530 | 	 * may also contain {@link SqlParameterValue} objects which indicate not
  531 | 	 * only the argument value but also the SQL type and optionally the scale
  532 | 	 * @return the result List, containing mapped objects
  533 | 	 * @throws DataAccessException if the query fails
  534 | 	 * @since 3.0.1
  535 | 	 */
  536 | 	<T extends @Nullable Object> List<T> query(String sql, RowMapper<T> rowMapper, @Nullable Object @Nullable ... args) throws DataAccessException;
  537 | 
  538 | 	/**
  539 | 	 * Query using a prepared statement, mapping each row to a result object
  540 | 	 * via a RowMapper, and turning it into an iterable and closeable Stream.
  541 | 	 * <p>A PreparedStatementCreator can either be implemented directly or
  542 | 	 * configured through a PreparedStatementCreatorFactory.
  543 | 	 * @param psc a callback that creates a PreparedStatement given a Connection
  544 | 	 * @param rowMapper a callback that will map one object per row
  545 | 	 * @return the result Stream, containing mapped objects, needing to be
  546 | 	 * closed once fully processed (for example, through a try-with-resources clause)
  547 | 	 * @throws DataAccessException if there is any problem
  548 | 	 * @see PreparedStatementCreatorFactory
  549 | 	 * @since 5.3
  550 | 	 */
  551 | 	<T extends @Nullable Object> Stream<T> queryForStream(PreparedStatementCreator psc, RowMapper<T> rowMapper) throws DataAccessException;
  552 | 
  553 | 	/**
  554 | 	 * Query given SQL to create a prepared statement from SQL and a
  555 | 	 * PreparedStatementSetter implementation that knows how to bind values
  556 | 	 * to the query, mapping each row to a result object via a RowMapper,
  557 | 	 * and turning it into an iterable and closeable Stream.
  558 | 	 * @param sql the SQL query to execute
  559 | 	 * @param pss a callback that knows how to set values on the prepared statement.
  560 | 	 * If this is {@code null}, the SQL will be assumed to contain no bind parameters.
  561 | 	 * Even if there are no bind parameters, this callback may be used to set the
  562 | 	 * fetch size and other performance options.
  563 | 	 * @param rowMapper a callback that will map one object per row
  564 | 	 * @return the result Stream, containing mapped objects, needing to be
  565 | 	 * closed once fully processed (for example, through a try-with-resources clause)
  566 | 	 * @throws DataAccessException if the query fails
  567 | 	 * @since 5.3
  568 | 	 */
  569 | 	<T extends @Nullable Object> Stream<T> queryForStream(String sql, @Nullable PreparedStatementSetter pss, RowMapper<T> rowMapper)
  570 | 			throws DataAccessException;
  571 | 
  572 | 	/**
  573 | 	 * Query given SQL to create a prepared statement from SQL and a list of
  574 | 	 * arguments to bind to the query, mapping each row to a result object
  575 | 	 * via a RowMapper, and turning it into an iterable and closeable Stream.
  576 | 	 * @param sql the SQL query to execute
  577 | 	 * @param rowMapper a callback that will map one object per row
  578 | 	 * @param args arguments to bind to the query
  579 | 	 * (leaving it to the PreparedStatement to guess the corresponding SQL type);
  580 | 	 * may also contain {@link SqlParameterValue} objects which indicate not
  581 | 	 * only the argument value but also the SQL type and optionally the scale
  582 | 	 * @return the result Stream, containing mapped objects, needing to be
  583 | 	 * closed once fully processed (for example, through a try-with-resources clause)
  584 | 	 * @throws DataAccessException if the query fails
  585 | 	 * @since 5.3
  586 | 	 */
  587 | 	<T extends @Nullable Object> Stream<T> queryForStream(String sql, RowMapper<T> rowMapper, @Nullable Object @Nullable ... args)
  588 | 			throws DataAccessException;
  589 | 
  590 | 	/**
  591 | 	 * Query given SQL to create a prepared statement from SQL and a list
  592 | 	 * of arguments to bind to the query, mapping a single result row to a
  593 | 	 * result object via a RowMapper.
  594 | 	 * @param sql the SQL query to execute
  595 | 	 * @param args arguments to bind to the query
  596 | 	 * (leaving it to the PreparedStatement to guess the corresponding SQL type)
  597 | 	 * @param argTypes the SQL types of the arguments
  598 | 	 * (constants from {@code java.sql.Types})
  599 | 	 * @param rowMapper a callback that will map one object per row
  600 | 	 * @return the single mapped object (may be {@code null} if the given
  601 | 	 * {@link RowMapper} returned {@code null})
  602 | 	 * @throws org.springframework.dao.IncorrectResultSizeDataAccessException
  603 | 	 * if the query does not return exactly one row
  604 | 	 * @throws DataAccessException if the query fails
  605 | 	 */
  606 | 	<T extends @Nullable Object> T queryForObject(String sql, @Nullable Object @Nullable [] args, int[] argTypes, RowMapper<T> rowMapper)
  607 | 			throws DataAccessException;
  608 | 
  609 | 	/**
  610 | 	 * Query given SQL to create a prepared statement from SQL and a list
  611 | 	 * of arguments to bind to the query, mapping a single result row to a
  612 | 	 * result object via a RowMapper.
  613 | 	 * @param sql the SQL query to execute
  614 | 	 * @param args arguments to bind to the query
  615 | 	 * (leaving it to the PreparedStatement to guess the corresponding SQL type);
  616 | 	 * may also contain {@link SqlParameterValue} objects which indicate not
  617 | 	 * only the argument value but also the SQL type and optionally the scale
  618 | 	 * @param rowMapper a callback that will map one object per row
  619 | 	 * @return the single mapped object (may be {@code null} if the given
  620 | 	 * {@link RowMapper} returned {@code null})
  621 | 	 * @throws org.springframework.dao.IncorrectResultSizeDataAccessException
  622 | 	 * if the query does not return exactly one row
  623 | 	 * @throws DataAccessException if the query fails
  624 | 	 * @deprecated in favor of {@link #queryForObject(String, RowMapper, Object...)}
  625 | 	 */
  626 | 	@Deprecated(since = "5.3")
  627 | 	<T extends @Nullable Object> T queryForObject(String sql, @Nullable Object @Nullable [] args, RowMapper<T> rowMapper) throws DataAccessException;
  628 | 
  629 | 	/**
  630 | 	 * Query given SQL to create a prepared statement from SQL and a list
  631 | 	 * of arguments to bind to the query, mapping a single result row to a
  632 | 	 * result object via a RowMapper.
  633 | 	 * @param sql the SQL query to execute
  634 | 	 * @param rowMapper a callback that will map one object per row
  635 | 	 * @param args arguments to bind to the query
  636 | 	 * (leaving it to the PreparedStatement to guess the corresponding SQL type);
  637 | 	 * may also contain {@link SqlParameterValue} objects which indicate not
  638 | 	 * only the argument value but also the SQL type and optionally the scale
  639 | 	 * @return the single mapped object (may be {@code null} if the given
  640 | 	 * {@link RowMapper} returned {@code null})
  641 | 	 * @throws org.springframework.dao.IncorrectResultSizeDataAccessException
  642 | 	 * if the query does not return exactly one row
  643 | 	 * @throws DataAccessException if the query fails
  644 | 	 * @since 3.0.1
  645 | 	 */
  646 | 	<T extends @Nullable Object> T queryForObject(String sql, RowMapper<T> rowMapper, @Nullable Object @Nullable ... args) throws DataAccessException;
  647 | 
  648 | 	/**
  649 | 	 * Query given SQL to create a prepared statement from SQL and a list of
  650 | 	 * arguments to bind to the query, expecting a result object.
  651 | 	 * <p>The query is expected to be a single row/single column query; the returned
  652 | 	 * result will be directly mapped to the corresponding object type.
  653 | 	 * @param sql the SQL query to execute
  654 | 	 * @param args arguments to bind to the query
  655 | 	 * @param argTypes the SQL types of the arguments
  656 | 	 * (constants from {@code java.sql.Types})
  657 | 	 * @param requiredType the type that the result object is expected to match
  658 | 	 * @return the result object of the required type, or {@code null} in case of SQL NULL
  659 | 	 * @throws org.springframework.dao.IncorrectResultSizeDataAccessException
  660 | 	 * if the query does not return exactly one row
  661 | 	 * @throws org.springframework.jdbc.IncorrectResultSetColumnCountException
  662 | 	 * if the query does not return a row containing a single column
  663 | 	 * @throws DataAccessException if the query fails
  664 | 	 * @see #queryForObject(String, Class)
  665 | 	 * @see java.sql.Types
  666 | 	 */
  667 | 	<T> @Nullable T queryForObject(String sql, @Nullable Object @Nullable [] args, int[] argTypes, Class<T> requiredType)
  668 | 			throws DataAccessException;
  669 | 
  670 | 	/**
  671 | 	 * Query given SQL to create a prepared statement from SQL and a list of
  672 | 	 * arguments to bind to the query, expecting a result object.
  673 | 	 * <p>The query is expected to be a single row/single column query; the returned
  674 | 	 * result will be directly mapped to the corresponding object type.
  675 | 	 * @param sql the SQL query to execute
  676 | 	 * @param args arguments to bind to the query
  677 | 	 * (leaving it to the PreparedStatement to guess the corresponding SQL type);
  678 | 	 * may also contain {@link SqlParameterValue} objects which indicate not
  679 | 	 * only the argument value but also the SQL type and optionally the scale
  680 | 	 * @param requiredType the type that the result object is expected to match
  681 | 	 * @return the result object of the required type, or {@code null} in case of SQL NULL
  682 | 	 * @throws org.springframework.dao.IncorrectResultSizeDataAccessException
  683 | 	 * if the query does not return exactly one row
  684 | 	 * @throws org.springframework.jdbc.IncorrectResultSetColumnCountException
  685 | 	 * if the query does not return a row containing a single column
  686 | 	 * @throws DataAccessException if the query fails
  687 | 	 * @see #queryForObject(String, Class)
  688 | 	 * @deprecated in favor of {@link #queryForObject(String, Class, Object...)}
  689 | 	 */
  690 | 	@Deprecated(since = "5.3")
  691 | 	<T> @Nullable T queryForObject(String sql, @Nullable Object @Nullable [] args, Class<T> requiredType) throws DataAccessException;
  692 | 
  693 | 	/**
  694 | 	 * Query given SQL to create a prepared statement from SQL and a list of
  695 | 	 * arguments to bind to the query, expecting a result object.
  696 | 	 * <p>The query is expected to be a single row/single column query; the returned
  697 | 	 * result will be directly mapped to the corresponding object type.
  698 | 	 * @param sql the SQL query to execute
  699 | 	 * @param requiredType the type that the result object is expected to match
  700 | 	 * @param args arguments to bind to the query
  701 | 	 * (leaving it to the PreparedStatement to guess the corresponding SQL type);
  702 | 	 * may also contain {@link SqlParameterValue} objects which indicate not
  703 | 	 * only the argument value but also the SQL type and optionally the scale
  704 | 	 * @return the result object of the required type, or {@code null} in case of SQL NULL
  705 | 	 * @throws org.springframework.dao.IncorrectResultSizeDataAccessException
  706 | 	 * if the query does not return exactly one row
  707 | 	 * @throws org.springframework.jdbc.IncorrectResultSetColumnCountException
  708 | 	 * if the query does not return a row containing a single column
  709 | 	 * @throws DataAccessException if the query fails
  710 | 	 * @since 3.0.1
  711 | 	 * @see #queryForObject(String, Class)
  712 | 	 */
  713 | 	<T> @Nullable T queryForObject(String sql, Class<T> requiredType, @Nullable Object @Nullable ... args) throws DataAccessException;
  714 | 
  715 | 	/**
  716 | 	 * Query given SQL to create a prepared statement from SQL and a list of
  717 | 	 * arguments to bind to the query, expecting a result map.
  718 | 	 * <p>The query is expected to be a single row query; the result row will be
  719 | 	 * mapped to a Map (one entry for each column, using the column name as the key).
  720 | 	 * @param sql the SQL query to execute
  721 | 	 * @param args arguments to bind to the query
  722 | 	 * @param argTypes the SQL types of the arguments
  723 | 	 * (constants from {@code java.sql.Types})
  724 | 	 * @return the result Map (one entry per column, with column name as key)
  725 | 	 * @throws org.springframework.dao.IncorrectResultSizeDataAccessException
  726 | 	 * if the query does not return exactly one row
  727 | 	 * @throws DataAccessException if the query fails
  728 | 	 * @see #queryForMap(String)
  729 | 	 * @see ColumnMapRowMapper
  730 | 	 * @see java.sql.Types
  731 | 	 */
  732 | 	Map<String, @Nullable Object> queryForMap(String sql, @Nullable Object @Nullable [] args, int[] argTypes) throws DataAccessException;
  733 | 
  734 | 	/**
  735 | 	 * Query given SQL to create a prepared statement from SQL and a list of
  736 | 	 * arguments to bind to the query, expecting a result map.
  737 | 	 * <p>The {@code queryForMap} methods defined by this interface are appropriate
  738 | 	 * when you don't have a domain model. Otherwise, consider using one of the
  739 | 	 * {@code queryForObject} methods.
  740 | 	 * <p>The query is expected to be a single row query; the result row will be
  741 | 	 * mapped to a Map (one entry for each column, using the column name as the key).
  742 | 	 * @param sql the SQL query to execute
  743 | 	 * @param args arguments to bind to the query
  744 | 	 * (leaving it to the PreparedStatement to guess the corresponding SQL type);
  745 | 	 * may also contain {@link SqlParameterValue} objects which indicate not
  746 | 	 * only the argument value but also the SQL type and optionally the scale
  747 | 	 * @return the result Map (one entry for each column, using the
  748 | 	 * column name as the key)
  749 | 	 * @throws org.springframework.dao.IncorrectResultSizeDataAccessException
  750 | 	 * if the query does not return exactly one row
  751 | 	 * @throws DataAccessException if the query fails
  752 | 	 * @see #queryForMap(String)
  753 | 	 * @see ColumnMapRowMapper
  754 | 	 */
  755 | 	Map<String, @Nullable Object> queryForMap(String sql, @Nullable Object @Nullable ... args) throws DataAccessException;
  756 | 
  757 | 	/**
  758 | 	 * Query given SQL to create a prepared statement from SQL and a list of
  759 | 	 * arguments to bind to the query, expecting a result list.
  760 | 	 * <p>The results will be mapped to a List (one entry for each row) of
  761 | 	 * result objects, each of them matching the specified element type.
  762 | 	 * @param sql the SQL query to execute
  763 | 	 * @param args arguments to bind to the query
  764 | 	 * @param argTypes the SQL types of the arguments
  765 | 	 * (constants from {@code java.sql.Types})
  766 | 	 * @param elementType the required type of element in the result list
  767 | 	 * (for example, {@code Integer.class})
  768 | 	 * @return a List of objects that match the specified element type
  769 | 	 * @throws DataAccessException if the query fails
  770 | 	 * @see #queryForList(String, Class)
  771 | 	 * @see SingleColumnRowMapper
  772 | 	 */
  773 | 	<T> List<@Nullable T> queryForList(String sql, @Nullable Object @Nullable [] args, int[] argTypes, Class<T> elementType)
  774 | 			throws DataAccessException;
  775 | 
  776 | 	/**
  777 | 	 * Query given SQL to create a prepared statement from SQL and a list of
  778 | 	 * arguments to bind to the query, expecting a result list.
  779 | 	 * <p>The results will be mapped to a List (one entry for each row) of
  780 | 	 * result objects, each of them matching the specified element type.
  781 | 	 * @param sql the SQL query to execute
  782 | 	 * @param args arguments to bind to the query
  783 | 	 * (leaving it to the PreparedStatement to guess the corresponding SQL type);
  784 | 	 * may also contain {@link SqlParameterValue} objects which indicate not
  785 | 	 * only the argument value but also the SQL type and optionally the scale
  786 | 	 * @param elementType the required type of element in the result list
  787 | 	 * (for example, {@code Integer.class})
  788 | 	 * @return a List of objects that match the specified element type
  789 | 	 * @throws DataAccessException if the query fails
  790 | 	 * @see #queryForList(String, Class)
  791 | 	 * @see SingleColumnRowMapper
  792 | 	 * @deprecated in favor of {@link #queryForList(String, Class, Object...)}
  793 | 	 */
  794 | 	@Deprecated(since = "5.3")
  795 | 	<T> List<@Nullable T> queryForList(String sql, @Nullable Object @Nullable [] args, Class<T> elementType) throws DataAccessException;
  796 | 
  797 | 	/**
  798 | 	 * Query given SQL to create a prepared statement from SQL and a list of
  799 | 	 * arguments to bind to the query, expecting a result list.
  800 | 	 * <p>The results will be mapped to a List (one entry for each row) of
  801 | 	 * result objects, each of them matching the specified element type.
  802 | 	 * @param sql the SQL query to execute
  803 | 	 * @param elementType the required type of element in the result list
  804 | 	 * (for example, {@code Integer.class})
  805 | 	 * @param args arguments to bind to the query
  806 | 	 * (leaving it to the PreparedStatement to guess the corresponding SQL type);
  807 | 	 * may also contain {@link SqlParameterValue} objects which indicate not
  808 | 	 * only the argument value but also the SQL type and optionally the scale
  809 | 	 * @return a List of objects that match the specified element type
  810 | 	 * @throws DataAccessException if the query fails
  811 | 	 * @since 3.0.1
  812 | 	 * @see #queryForList(String, Class)
  813 | 	 * @see SingleColumnRowMapper
  814 | 	 */
  815 | 	<T> List<@Nullable T> queryForList(String sql, Class<T> elementType, @Nullable Object @Nullable ... args) throws DataAccessException;
  816 | 
  817 | 	/**
  818 | 	 * Query given SQL to create a prepared statement from SQL and a list of
  819 | 	 * arguments to bind to the query, expecting a result list.
  820 | 	 * <p>The results will be mapped to a List (one entry for each row) of
  821 | 	 * Maps (one entry for each column, using the column name as the key).
  822 | 	 * Each element in the list will be of the form returned by this interface's
  823 | 	 * {@code queryForMap} methods.
  824 | 	 * @param sql the SQL query to execute
  825 | 	 * @param args arguments to bind to the query
  826 | 	 * @param argTypes the SQL types of the arguments
  827 | 	 * (constants from {@code java.sql.Types})
  828 | 	 * @return a List that contains a Map per row
  829 | 	 * @throws DataAccessException if the query fails
  830 | 	 * @see #queryForList(String)
  831 | 	 * @see java.sql.Types
  832 | 	 */
  833 | 	List<Map<String, @Nullable Object>> queryForList(String sql, @Nullable Object @Nullable [] args, int[] argTypes) throws DataAccessException;
  834 | 
  835 | 	/**
  836 | 	 * Query given SQL to create a prepared statement from SQL and a list of
  837 | 	 * arguments to bind to the query, expecting a result list.
  838 | 	 * <p>The results will be mapped to a List (one entry for each row) of
  839 | 	 * Maps (one entry for each column, using the column name as the key).
  840 | 	 * Each element in the list will be of the form returned by this interface's
  841 | 	 * {@code queryForMap} methods.
  842 | 	 * @param sql the SQL query to execute
  843 | 	 * @param args arguments to bind to the query
  844 | 	 * (leaving it to the PreparedStatement to guess the corresponding SQL type);
  845 | 	 * may also contain {@link SqlParameterValue} objects which indicate not
  846 | 	 * only the argument value but also the SQL type and optionally the scale
  847 | 	 * @return a List that contains a Map per row
  848 | 	 * @throws DataAccessException if the query fails
  849 | 	 * @see #queryForList(String)
  850 | 	 */
  851 | 	List<Map<String, @Nullable Object>> queryForList(String sql, @Nullable Object @Nullable ... args) throws DataAccessException;
  852 | 
  853 | 	/**
  854 | 	 * Query given SQL to create a prepared statement from SQL and a list of
  855 | 	 * arguments to bind to the query, expecting an SqlRowSet.
  856 | 	 * <p>The results will be mapped to an SqlRowSet which holds the data in a
  857 | 	 * disconnected fashion. This wrapper will translate any SQLExceptions thrown.
  858 | 	 * <p>Note that, for the default implementation, JDBC RowSet support needs to
  859 | 	 * be available at runtime: by default, a standard JDBC {@code CachedRowSet}
  860 | 	 * is used.
  861 | 	 * @param sql the SQL query to execute
  862 | 	 * @param args arguments to bind to the query
  863 | 	 * @param argTypes the SQL types of the arguments
  864 | 	 * (constants from {@code java.sql.Types})
  865 | 	 * @return an SqlRowSet representation (possibly a wrapper around a
  866 | 	 * {@code javax.sql.rowset.CachedRowSet})
  867 | 	 * @throws DataAccessException if there is any problem executing the query
  868 | 	 * @see #queryForRowSet(String)
  869 | 	 * @see SqlRowSetResultSetExtractor
  870 | 	 * @see javax.sql.rowset.CachedRowSet
  871 | 	 * @see java.sql.Types
  872 | 	 */
  873 | 	SqlRowSet queryForRowSet(String sql, @Nullable Object @Nullable [] args, int[] argTypes) throws DataAccessException;
  874 | 
  875 | 	/**
  876 | 	 * Query given SQL to create a prepared statement from SQL and a list of
  877 | 	 * arguments to bind to the query, expecting an SqlRowSet.
  878 | 	 * <p>The results will be mapped to an SqlRowSet which holds the data in a
  879 | 	 * disconnected fashion. This wrapper will translate any SQLExceptions thrown.
  880 | 	 * <p>Note that, for the default implementation, JDBC RowSet support needs to
  881 | 	 * be available at runtime: by default, a standard JDBC {@code CachedRowSet}
  882 | 	 * is used.
  883 | 	 * @param sql the SQL query to execute
  884 | 	 * @param args arguments to bind to the query
  885 | 	 * (leaving it to the PreparedStatement to guess the corresponding SQL type);
  886 | 	 * may also contain {@link SqlParameterValue} objects which indicate not
  887 | 	 * only the argument value but also the SQL type and optionally the scale
  888 | 	 * @return an SqlRowSet representation (possibly a wrapper around a
  889 | 	 * {@code javax.sql.rowset.CachedRowSet})
  890 | 	 * @throws DataAccessException if there is any problem executing the query
  891 | 	 * @see #queryForRowSet(String)
  892 | 	 * @see SqlRowSetResultSetExtractor
  893 | 	 * @see javax.sql.rowset.CachedRowSet
  894 | 	 */
  895 | 	SqlRowSet queryForRowSet(String sql, @Nullable Object @Nullable ... args) throws DataAccessException;
  896 | 
  897 | 	/**
  898 | 	 * Issue a single SQL update operation (such as an insert, update or delete
  899 | 	 * statement) using a PreparedStatementCreator to provide SQL and any
  900 | 	 * required parameters.
  901 | 	 * <p>A PreparedStatementCreator can either be implemented directly or
  902 | 	 * configured through a PreparedStatementCreatorFactory.
  903 | 	 * @param psc a callback that provides SQL and any necessary parameters
  904 | 	 * @return the number of rows affected
  905 | 	 * @throws DataAccessException if there is any problem issuing the update
  906 | 	 * @see PreparedStatementCreatorFactory
  907 | 	 */
  908 | 	int update(PreparedStatementCreator psc) throws DataAccessException;
  909 | 
  910 | 	/**
  911 | 	 * Issue an update statement using a PreparedStatementCreator to provide SQL and
  912 | 	 * any required parameters. Generated keys will be put into the given KeyHolder.
  913 | 	 * <p>Note that the given PreparedStatementCreator has to create a statement
  914 | 	 * with activated extraction of generated keys (a JDBC 3.0 feature). This can
  915 | 	 * either be done directly or through using a PreparedStatementCreatorFactory.
  916 | 	 * <p>This method requires support for generated keys in the JDBC driver.
  917 | 	 * @param psc a callback that provides SQL and any necessary parameters
  918 | 	 * @param generatedKeyHolder a KeyHolder that will hold the generated keys
  919 | 	 * @return the number of rows affected
  920 | 	 * @throws DataAccessException if there is any problem issuing the update
  921 | 	 * @see PreparedStatementCreatorFactory
  922 | 	 * @see org.springframework.jdbc.support.GeneratedKeyHolder
  923 | 	 * @see java.sql.DatabaseMetaData#supportsGetGeneratedKeys()
  924 | 	 */
  925 | 	int update(PreparedStatementCreator psc, KeyHolder generatedKeyHolder) throws DataAccessException;
  926 | 
  927 | 	/**
  928 | 	 * Issue an update statement using a PreparedStatementSetter to set bind parameters,
  929 | 	 * with given SQL. Simpler than using a PreparedStatementCreator as this method
  930 | 	 * will create the PreparedStatement: The PreparedStatementSetter just needs to
  931 | 	 * set parameters.
  932 | 	 * @param sql the SQL containing bind parameters
  933 | 	 * @param pss helper that sets bind parameters. If this is {@code null}
  934 | 	 * we run an update with static SQL.
  935 | 	 * @return the number of rows affected
  936 | 	 * @throws DataAccessException if there is any problem issuing the update
  937 | 	 */
  938 | 	int update(String sql, @Nullable PreparedStatementSetter pss) throws DataAccessException;
  939 | 
  940 | 	/**
  941 | 	 * Issue a single SQL update operation (such as an insert, update or delete statement)
  942 | 	 * via a prepared statement, binding the given arguments.
  943 | 	 * @param sql the SQL containing bind parameters
  944 | 	 * @param args arguments to bind to the query
  945 | 	 * @param argTypes the SQL types of the arguments
  946 | 	 * (constants from {@code java.sql.Types})
  947 | 	 * @return the number of rows affected
  948 | 	 * @throws DataAccessException if there is any problem issuing the update
  949 | 	 * @see java.sql.Types
  950 | 	 */
  951 | 	int update(String sql, @Nullable Object @Nullable [] args, int[] argTypes) throws DataAccessException;
  952 | 
  953 | 	/**
  954 | 	 * Issue a single SQL update operation (such as an insert, update or delete statement)
  955 | 	 * via a prepared statement, binding the given arguments.
  956 | 	 * @param sql the SQL containing bind parameters
  957 | 	 * @param args arguments to bind to the query
  958 | 	 * (leaving it to the PreparedStatement to guess the corresponding SQL type);
  959 | 	 * may also contain {@link SqlParameterValue} objects which indicate not
  960 | 	 * only the argument value but also the SQL type and optionally the scale
  961 | 	 * @return the number of rows affected
  962 | 	 * @throws DataAccessException if there is any problem issuing the update
  963 | 	 */
  964 | 	int update(String sql, @Nullable Object @Nullable ... args) throws DataAccessException;
  965 | 
  966 | 	/**
  967 | 	 * Issue multiple update statements on a single PreparedStatement,
  968 | 	 * using batch updates and a BatchPreparedStatementSetter to set values.
  969 | 	 * <p>Will fall back to separate updates on a single PreparedStatement
  970 | 	 * if the JDBC driver does not support batch updates.
  971 | 	 * @param sql defining PreparedStatement that will be reused.
  972 | 	 * All statements in the batch will use the same SQL.
  973 | 	 * @param pss object to set parameters on the PreparedStatement
  974 | 	 * created by this method
  975 | 	 * @return an array of the number of rows affected by each statement
  976 | 	 * (may also contain special JDBC-defined negative values for affected rows such as
  977 | 	 * {@link java.sql.Statement#SUCCESS_NO_INFO}/{@link java.sql.Statement#EXECUTE_FAILED})
  978 | 	 * @throws DataAccessException if there is any problem issuing the update
  979 | 	 */
  980 | 	int[] batchUpdate(String sql, BatchPreparedStatementSetter pss) throws DataAccessException;
  981 | 
  982 | 	/**
  983 | 	 * Issue multiple update statements on a single PreparedStatement,
  984 | 	 * using batch updates and a BatchPreparedStatementSetter to set values.
  985 | 	 * Generated keys will be put into the given KeyHolder.
  986 | 	 * <p>Note that the given PreparedStatementCreator has to create a statement
  987 | 	 * with activated extraction of generated keys (a JDBC 3.0 feature). This can
  988 | 	 * either be done directly or through using a PreparedStatementCreatorFactory.
  989 | 	 * <p>This method requires support for generated keys in the JDBC driver.
  990 | 	 * It will fall back to separate updates on a single PreparedStatement
  991 | 	 * if the JDBC driver does not support batch updates.
  992 | 	 * @param psc a callback that creates a PreparedStatement given a Connection
  993 | 	 * @param pss object to set parameters on the PreparedStatement
  994 | 	 * created by this method
  995 | 	 * @param generatedKeyHolder a KeyHolder that will hold the generated keys
  996 | 	 * @return an array of the number of rows affected by each statement
  997 | 	 * (may also contain special JDBC-defined negative values for affected rows such as
  998 | 	 * {@link java.sql.Statement#SUCCESS_NO_INFO}/{@link java.sql.Statement#EXECUTE_FAILED})
  999 | 	 * @throws DataAccessException if there is any problem issuing the update
 1000 | 	 * @since 6.1
 1001 | 	 * @see org.springframework.jdbc.support.GeneratedKeyHolder
 1002 | 	 * @see java.sql.DatabaseMetaData#supportsGetGeneratedKeys()
 1003 | 	 */
 1004 | 	int[] batchUpdate(PreparedStatementCreator psc, BatchPreparedStatementSetter pss,
 1005 | 			KeyHolder generatedKeyHolder) throws DataAccessException;
 1006 | 
 1007 | 	/**
 1008 | 	 * Execute a batch using the supplied SQL statement with the batch of supplied arguments.
 1009 | 	 * @param sql the SQL statement to execute
 1010 | 	 * @param batchArgs the List of Object arrays containing the batch of arguments for the query
 1011 | 	 * @return an array containing the numbers of rows affected by each update in the batch
 1012 | 	 * (may also contain special JDBC-defined negative values for affected rows such as
 1013 | 	 * {@link java.sql.Statement#SUCCESS_NO_INFO}/{@link java.sql.Statement#EXECUTE_FAILED})
 1014 | 	 * @throws DataAccessException if there is any problem issuing the update
 1015 | 	 */
 1016 | 	int[] batchUpdate(String sql, List<? extends @Nullable Object[]> batchArgs) throws DataAccessException;
 1017 | 
 1018 | 	/**
 1019 | 	 * Execute a batch using the supplied SQL statement with the batch of supplied arguments.
 1020 | 	 * @param sql the SQL statement to execute.
 1021 | 	 * @param batchArgs the List of Object arrays containing the batch of arguments for the query
 1022 | 	 * @param argTypes the SQL types of the arguments
 1023 | 	 * (constants from {@code java.sql.Types})
 1024 | 	 * @return an array containing the numbers of rows affected by each update in the batch
 1025 | 	 * (may also contain special JDBC-defined negative values for affected rows such as
 1026 | 	 * {@link java.sql.Statement#SUCCESS_NO_INFO}/{@link java.sql.Statement#EXECUTE_FAILED})
 1027 | 	 * @throws DataAccessException if there is any problem issuing the update
 1028 | 	 */
 1029 | 	int[] batchUpdate(String sql, List<? extends @Nullable Object[]> batchArgs, int[] argTypes) throws DataAccessException;
 1030 | 
 1031 | 	/**
 1032 | 	 * Execute multiple batches using the supplied SQL statement with the collect of supplied
 1033 | 	 * arguments. The arguments' values will be set using the ParameterizedPreparedStatementSetter.
 1034 | 	 * Each batch should be of size indicated in 'batchSize'.
 1035 | 	 * @param sql the SQL statement to execute.
 1036 | 	 * @param batchArgs the List of Object arrays containing the batch of arguments for the query
 1037 | 	 * @param batchSize batch size
 1038 | 	 * @param pss the ParameterizedPreparedStatementSetter to use
 1039 | 	 * @return an array containing for each batch another array containing the numbers of
 1040 | 	 * rows affected by each update in the batch
 1041 | 	 * (may also contain special JDBC-defined negative values for affected rows such as
 1042 | 	 * {@link java.sql.Statement#SUCCESS_NO_INFO}/{@link java.sql.Statement#EXECUTE_FAILED})
 1043 | 	 * @throws DataAccessException if there is any problem issuing the update
 1044 | 	 * @since 3.1
 1045 | 	 * @deprecated as of 7.1, in favor of single batchUpdate calls which execute the
 1046 | 	 * entire batch (since modern databases do not restrict the batch size anymore)
 1047 | 	 */
 1048 | 	@Deprecated(since = "7.1")
 1049 | 	<T> int[][] batchUpdate(String sql, Collection<T> batchArgs, int batchSize,
 1050 | 			ParameterizedPreparedStatementSetter<T> pss) throws DataAccessException;
 1051 | 
 1052 | 
 1053 | 	//-------------------------------------------------------------------------
 1054 | 	// Methods dealing with callable statements
 1055 | 	//-------------------------------------------------------------------------
 1056 | 
 1057 | 	/**
 1058 | 	 * Execute a JDBC data access operation, implemented as callback action
 1059 | 	 * working on a JDBC CallableStatement. This allows for implementing arbitrary
 1060 | 	 * data access operations on a single Statement, within Spring's managed JDBC
 1061 | 	 * environment: that is, participating in Spring-managed transactions and
 1062 | 	 * converting JDBC SQLExceptions into Spring's DataAccessException hierarchy.
 1063 | 	 * <p>The callback action can return a result object, for example a domain
 1064 | 	 * object or a collection of domain objects.
 1065 | 	 * @param csc a callback that creates a CallableStatement given a Connection
 1066 | 	 * @param action a callback that specifies the action
 1067 | 	 * @return a result object returned by the action, or {@code null} if none
 1068 | 	 * @throws DataAccessException if there is any problem
 1069 | 	 */
 1070 | 	<T extends @Nullable Object> T execute(CallableStatementCreator csc, CallableStatementCallback<T> action) throws DataAccessException;
 1071 | 
 1072 | 	/**
 1073 | 	 * Execute a JDBC data access operation, implemented as callback action
 1074 | 	 * working on a JDBC CallableStatement. This allows for implementing arbitrary
 1075 | 	 * data access operations on a single Statement, within Spring's managed JDBC
 1076 | 	 * environment: that is, participating in Spring-managed transactions and
 1077 | 	 * converting JDBC SQLExceptions into Spring's DataAccessException hierarchy.
 1078 | 	 * <p>The callback action can return a result object, for example a domain
 1079 | 	 * object or a collection of domain objects.
 1080 | 	 * @param callString the SQL call string to execute
 1081 | 	 * @param action a callback that specifies the action
 1082 | 	 * @return a result object returned by the action, or {@code null} if none
 1083 | 	 * @throws DataAccessException if there is any problem
 1084 | 	 */
 1085 | 	<T extends @Nullable Object> T execute(String callString, CallableStatementCallback<T> action) throws DataAccessException;
 1086 | 
 1087 | 	/**
 1088 | 	 * Execute an SQL call using a CallableStatementCreator to provide SQL and
 1089 | 	 * any required parameters.
 1090 | 	 * @param csc a callback that provides SQL and any necessary parameters
 1091 | 	 * @param declaredParameters list of declared SqlParameter objects
 1092 | 	 * @return a Map of extracted out parameters
 1093 | 	 * @throws DataAccessException if there is any problem issuing the update
 1094 | 	 */
 1095 | 	Map<String, @Nullable Object> call(CallableStatementCreator csc, List<SqlParameter> declaredParameters)
 1096 | 			throws DataAccessException;
 1097 | 
 1098 | }
```
