<!-- learning-docs source archive（机械抓取原文，禁止手改代码内容） -->
<!-- origin: org.springframework:spring-jdbc:7.1.0 | path: org/springframework/jdbc/support/JdbcUtils.java -->
<!-- 正文引用：[[源码:JdbcUtils.java#L起-L止]] ；代码块头部：(src: JdbcUtils.java#L起-L止) -->

# JdbcUtils.java

| 项 | 值 |
| --- | --- |
| 来源 | `org.springframework:spring-jdbc:7.1.0` |
| 路径 | `org/springframework/jdbc/support/JdbcUtils.java` |
| 行数 | 568 |
| 在线地址 | https://repo1.maven.org/maven2/org/springframework/spring-jdbc/7.1.0/spring-jdbc-7.1.0-sources.jar |
| 抓取时间 | 2026-11-25T14:02:23.324Z |

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
   17 | package org.springframework.jdbc.support;
   18 | 
   19 | import java.lang.reflect.Field;
   20 | import java.lang.reflect.InvocationTargetException;
   21 | import java.math.BigDecimal;
   22 | import java.sql.Blob;
   23 | import java.sql.Clob;
   24 | import java.sql.Connection;
   25 | import java.sql.DatabaseMetaData;
   26 | import java.sql.ResultSet;
   27 | import java.sql.ResultSetMetaData;
   28 | import java.sql.SQLException;
   29 | import java.sql.SQLFeatureNotSupportedException;
   30 | import java.sql.Statement;
   31 | import java.sql.Types;
   32 | import java.util.HashMap;
   33 | import java.util.Map;
   34 | 
   35 | import javax.sql.DataSource;
   36 | 
   37 | import org.apache.commons.logging.Log;
   38 | import org.apache.commons.logging.LogFactory;
   39 | import org.jspecify.annotations.Nullable;
   40 | 
   41 | import org.springframework.jdbc.CannotGetJdbcConnectionException;
   42 | import org.springframework.jdbc.datasource.DataSourceUtils;
   43 | import org.springframework.util.NumberUtils;
   44 | import org.springframework.util.StringUtils;
   45 | 
   46 | /**
   47 |  * Generic utility methods for working with JDBC. Mainly for internal use
   48 |  * within the framework, but also useful for custom JDBC access code.
   49 |  *
   50 |  * @author Thomas Risberg
   51 |  * @author Juergen Hoeller
   52 |  * @author Ben Blinebury
   53 |  */
   54 | public abstract class JdbcUtils {
   55 | 
   56 | 	/**
   57 | 	 * Constant that indicates an unknown (or unspecified) SQL type.
   58 | 	 * @see java.sql.Types
   59 | 	 */
   60 | 	public static final int TYPE_UNKNOWN = Integer.MIN_VALUE;
   61 | 
   62 | 	private static final Log logger = LogFactory.getLog(JdbcUtils.class);
   63 | 
   64 | 	private static final Map<Integer, String> typeNames = new HashMap<>();
   65 | 
   66 | 	static {
   67 | 		try {
   68 | 			for (Field field : Types.class.getFields()) {
   69 | 				typeNames.put((Integer) field.get(null), field.getName());
   70 | 			}
   71 | 		}
   72 | 		catch (Exception ex) {
   73 | 			throw new IllegalStateException("Failed to resolve JDBC Types constants", ex);
   74 | 		}
   75 | 	}
   76 | 
   77 | 
   78 | 	/**
   79 | 	 * Close the given JDBC Connection and ignore any thrown exception.
   80 | 	 * This is useful for typical finally blocks in manual JDBC code.
   81 | 	 * @param con the JDBC Connection to close (may be {@code null})
   82 | 	 */
   83 | 	public static void closeConnection(@Nullable Connection con) {
   84 | 		if (con != null) {
   85 | 			try {
   86 | 				con.close();
   87 | 			}
   88 | 			catch (SQLException ex) {
   89 | 				logger.debug("Could not close JDBC Connection", ex);
   90 | 			}
   91 | 			catch (Throwable ex) {
   92 | 				// We don't trust the JDBC driver: It might throw RuntimeException or Error.
   93 | 				logger.debug("Unexpected exception on closing JDBC Connection", ex);
   94 | 			}
   95 | 		}
   96 | 	}
   97 | 
   98 | 	/**
   99 | 	 * Close the given JDBC Statement and ignore any thrown exception.
  100 | 	 * This is useful for typical finally blocks in manual JDBC code.
  101 | 	 * @param stmt the JDBC Statement to close (may be {@code null})
  102 | 	 */
  103 | 	public static void closeStatement(@Nullable Statement stmt) {
  104 | 		if (stmt != null) {
  105 | 			try {
  106 | 				stmt.close();
  107 | 			}
  108 | 			catch (SQLException ex) {
  109 | 				logger.trace("Could not close JDBC Statement", ex);
  110 | 			}
  111 | 			catch (Throwable ex) {
  112 | 				// We don't trust the JDBC driver: It might throw RuntimeException or Error.
  113 | 				logger.trace("Unexpected exception on closing JDBC Statement", ex);
  114 | 			}
  115 | 		}
  116 | 	}
  117 | 
  118 | 	/**
  119 | 	 * Close the given JDBC ResultSet and ignore any thrown exception.
  120 | 	 * This is useful for typical finally blocks in manual JDBC code.
  121 | 	 * @param rs the JDBC ResultSet to close (may be {@code null})
  122 | 	 */
  123 | 	public static void closeResultSet(@Nullable ResultSet rs) {
  124 | 		if (rs != null) {
  125 | 			try {
  126 | 				rs.close();
  127 | 			}
  128 | 			catch (SQLException ex) {
  129 | 				logger.trace("Could not close JDBC ResultSet", ex);
  130 | 			}
  131 | 			catch (Throwable ex) {
  132 | 				// We don't trust the JDBC driver: It might throw RuntimeException or Error.
  133 | 				logger.trace("Unexpected exception on closing JDBC ResultSet", ex);
  134 | 			}
  135 | 		}
  136 | 	}
  137 | 
  138 | 	/**
  139 | 	 * Retrieve a JDBC column value from a ResultSet, using the specified value type.
  140 | 	 * <p>Uses the specifically typed ResultSet accessor methods, falling back to
  141 | 	 * {@link #getResultSetValue(java.sql.ResultSet, int)} for unknown types.
  142 | 	 * <p>Note that the returned value may not be assignable to the specified
  143 | 	 * required type, in case of an unknown type. Calling code needs to deal
  144 | 	 * with this case appropriately, for example, throwing a corresponding exception.
  145 | 	 * @param rs is the ResultSet holding the data
  146 | 	 * @param index is the column index
  147 | 	 * @param requiredType the required value type (may be {@code null})
  148 | 	 * @return the value object (possibly not of the specified required type,
  149 | 	 * with further conversion steps necessary)
  150 | 	 * @throws SQLException if thrown by the JDBC API
  151 | 	 * @see #getResultSetValue(ResultSet, int)
  152 | 	 */
  153 | 	public static @Nullable Object getResultSetValue(ResultSet rs, int index, @Nullable Class<?> requiredType) throws SQLException {
  154 | 		if (requiredType == null) {
  155 | 			return getResultSetValue(rs, index);
  156 | 		}
  157 | 
  158 | 		Object value;
  159 | 
  160 | 		// Explicitly extract typed value, as far as possible.
  161 | 		if (String.class == requiredType) {
  162 | 			return rs.getString(index);
  163 | 		}
  164 | 		else if (boolean.class == requiredType || Boolean.class == requiredType) {
  165 | 			value = rs.getBoolean(index);
  166 | 		}
  167 | 		else if (byte.class == requiredType || Byte.class == requiredType) {
  168 | 			value = rs.getByte(index);
  169 | 		}
  170 | 		else if (short.class == requiredType || Short.class == requiredType) {
  171 | 			value = rs.getShort(index);
  172 | 		}
  173 | 		else if (int.class == requiredType || Integer.class == requiredType) {
  174 | 			value = rs.getInt(index);
  175 | 		}
  176 | 		else if (long.class == requiredType || Long.class == requiredType) {
  177 | 			value = rs.getLong(index);
  178 | 		}
  179 | 		else if (float.class == requiredType || Float.class == requiredType) {
  180 | 			value = rs.getFloat(index);
  181 | 		}
  182 | 		else if (double.class == requiredType || Double.class == requiredType ||
  183 | 				Number.class == requiredType) {
  184 | 			value = rs.getDouble(index);
  185 | 		}
  186 | 		else if (BigDecimal.class == requiredType) {
  187 | 			return rs.getBigDecimal(index);
  188 | 		}
  189 | 		else if (java.sql.Date.class == requiredType) {
  190 | 			return rs.getDate(index);
  191 | 		}
  192 | 		else if (java.sql.Time.class == requiredType) {
  193 | 			return rs.getTime(index);
  194 | 		}
  195 | 		else if (java.sql.Timestamp.class == requiredType || java.util.Date.class == requiredType) {
  196 | 			return rs.getTimestamp(index);
  197 | 		}
  198 | 		else if (byte[].class == requiredType) {
  199 | 			return rs.getBytes(index);
  200 | 		}
  201 | 		else if (Blob.class == requiredType) {
  202 | 			return rs.getBlob(index);
  203 | 		}
  204 | 		else if (Clob.class == requiredType) {
  205 | 			return rs.getClob(index);
  206 | 		}
  207 | 		else if (requiredType.isEnum()) {
  208 | 			// Enums can either be represented through a String or an enum index value:
  209 | 			// leave enum type conversion up to the caller (for example, a ConversionService)
  210 | 			// but make sure that we return nothing other than a String or an Integer.
  211 | 			Object obj = rs.getObject(index);
  212 | 			if (obj instanceof String) {
  213 | 				return obj;
  214 | 			}
  215 | 			else if (obj instanceof Number number) {
  216 | 				// Defensively convert any Number to an Integer (as needed by our
  217 | 				// ConversionService's IntegerToEnumConverterFactory) for use as index
  218 | 				return NumberUtils.convertNumberToTargetClass(number, Integer.class);
  219 | 			}
  220 | 			else {
  221 | 				// for example, on Postgres: getObject returns a PGObject, but we need a String
  222 | 				return rs.getString(index);
  223 | 			}
  224 | 		}
  225 | 
  226 | 		else {
  227 | 			// Some unknown type desired -> rely on getObject.
  228 | 			try {
  229 | 				return rs.getObject(index, requiredType);
  230 | 			}
  231 | 			catch (SQLFeatureNotSupportedException | AbstractMethodError ex) {
  232 | 				logger.debug("JDBC driver does not support JDBC 4.1 'getObject(int, Class)' method", ex);
  233 | 			}
  234 | 			catch (SQLException ex) {
  235 | 				if (logger.isDebugEnabled()) {
  236 | 					logger.debug("JDBC driver has limited support for 'getObject(int, Class)' with column type: " +
  237 | 							requiredType.getName(), ex);
  238 | 				}
  239 | 			}
  240 | 
  241 | 			// Corresponding SQL types for JSR-310, left up to the caller to convert
  242 | 			// them (for example, through a ConversionService).
  243 | 			String typeName = requiredType.getSimpleName();
  244 | 			return switch (typeName) {
  245 | 				case "LocalDate" -> rs.getDate(index);
  246 | 				case "LocalTime" -> rs.getTime(index);
  247 | 				case "LocalDateTime" -> rs.getTimestamp(index);
  248 | 				// Fall back to getObject without type specification, again
  249 | 				// left up to the caller to convert the value if necessary.
  250 | 				default -> getResultSetValue(rs, index);
  251 | 			};
  252 | 		}
  253 | 
  254 | 		// Perform was-null check if necessary (for results that the JDBC driver returns as primitives).
  255 | 		return (rs.wasNull() ? null : value);
  256 | 	}
  257 | 
  258 | 	/**
  259 | 	 * Retrieve a JDBC column value from a ResultSet, using the most appropriate
  260 | 	 * value type. The returned value should be a detached value object, not having
  261 | 	 * any ties to the active ResultSet: in particular, it should not be a Blob or
  262 | 	 * Clob object but rather a byte array or String representation, respectively.
  263 | 	 * <p>Uses the {@code getObject(index)} method, but includes additional "hacks"
  264 | 	 * to get around Oracle 10g returning a non-standard object for its TIMESTAMP
  265 | 	 * datatype and a {@code java.sql.Date} for DATE columns leaving out the
  266 | 	 * time portion: These columns will explicitly be extracted as standard
  267 | 	 * {@code java.sql.Timestamp} object.
  268 | 	 * @param rs is the ResultSet holding the data
  269 | 	 * @param index is the column index
  270 | 	 * @return the value object
  271 | 	 * @throws SQLException if thrown by the JDBC API
  272 | 	 * @see java.sql.Blob
  273 | 	 * @see java.sql.Clob
  274 | 	 * @see java.sql.Timestamp
  275 | 	 */
  276 | 	public static @Nullable Object getResultSetValue(ResultSet rs, int index) throws SQLException {
  277 | 		Object obj = rs.getObject(index);
  278 | 		String className = null;
  279 | 		if (obj != null) {
  280 | 			className = obj.getClass().getName();
  281 | 		}
  282 | 		if (obj instanceof Blob blob) {
  283 | 			obj = blob.getBytes(1, (int) blob.length());
  284 | 		}
  285 | 		else if (obj instanceof Clob clob) {
  286 | 			obj = clob.getSubString(1, (int) clob.length());
  287 | 		}
  288 | 		else if ("oracle.sql.TIMESTAMP".equals(className) || "oracle.sql.TIMESTAMPTZ".equals(className)) {
  289 | 			obj = rs.getTimestamp(index);
  290 | 		}
  291 | 		else if (className != null && className.startsWith("oracle.sql.DATE")) {
  292 | 			String metaDataClassName = rs.getMetaData().getColumnClassName(index);
  293 | 			if ("java.sql.Timestamp".equals(metaDataClassName) || "oracle.sql.TIMESTAMP".equals(metaDataClassName)) {
  294 | 				obj = rs.getTimestamp(index);
  295 | 			}
  296 | 			else {
  297 | 				obj = rs.getDate(index);
  298 | 			}
  299 | 		}
  300 | 		else if (obj instanceof java.sql.Date) {
  301 | 			if ("java.sql.Timestamp".equals(rs.getMetaData().getColumnClassName(index))) {
  302 | 				obj = rs.getTimestamp(index);
  303 | 			}
  304 | 		}
  305 | 		return obj;
  306 | 	}
  307 | 
  308 | 	/**
  309 | 	 * Extract database meta-data via the given DatabaseMetaDataCallback.
  310 | 	 * <p>This method will open a connection to the database and retrieve its meta-data.
  311 | 	 * Since this method is called before the exception translation feature is configured
  312 | 	 * for a DataSource, this method can not rely on SQLException translation itself.
  313 | 	 * <p>Any exceptions will be wrapped in a MetaDataAccessException. This is a checked
  314 | 	 * exception and any calling code should catch and handle this exception. You can just
  315 | 	 * log the error and hope for the best, but there is probably a more serious error that
  316 | 	 * will reappear when you try to access the database again.
  317 | 	 * @param dataSource the DataSource to extract meta-data for
  318 | 	 * @param action callback that will do the actual work
  319 | 	 * @return object containing the extracted information, as returned by
  320 | 	 * the DatabaseMetaDataCallback's {@code processMetaData} method
  321 | 	 * @throws MetaDataAccessException if meta-data access failed
  322 | 	 * @see java.sql.DatabaseMetaData
  323 | 	 */
  324 | 	public static <T extends @Nullable Object> T extractDatabaseMetaData(DataSource dataSource, DatabaseMetaDataCallback<T> action)
  325 | 			throws MetaDataAccessException {
  326 | 
  327 | 		Connection con = null;
  328 | 		try {
  329 | 			con = DataSourceUtils.getConnection(dataSource);
  330 | 			DatabaseMetaData metaData;
  331 | 			try {
  332 | 				metaData = con.getMetaData();
  333 | 			}
  334 | 			catch (SQLException ex) {
  335 | 				if (DataSourceUtils.isConnectionTransactional(con, dataSource)) {
  336 | 					// Probably a closed thread-bound Connection - retry against fresh Connection
  337 | 					DataSourceUtils.releaseConnection(con, dataSource);
  338 | 					con = null;
  339 | 					logger.debug("Failed to obtain DatabaseMetaData from transactional Connection - " +
  340 | 							"retrying against fresh Connection", ex);
  341 | 					con = dataSource.getConnection();
  342 | 					metaData = con.getMetaData();
  343 | 				}
  344 | 				else {
  345 | 					throw ex;
  346 | 				}
  347 | 			}
  348 | 			if (metaData == null) {
  349 | 				// should only happen in test environments
  350 | 				throw new MetaDataAccessException("DatabaseMetaData returned by Connection [" + con + "] was null");
  351 | 			}
  352 | 			return action.processMetaData(metaData);
  353 | 		}
  354 | 		catch (CannotGetJdbcConnectionException ex) {
  355 | 			throw new MetaDataAccessException("Could not get Connection for extracting meta-data", ex);
  356 | 		}
  357 | 		catch (SQLException ex) {
  358 | 			throw new MetaDataAccessException("Error while extracting DatabaseMetaData", ex);
  359 | 		}
  360 | 		catch (AbstractMethodError err) {
  361 | 			throw new MetaDataAccessException(
  362 | 					"JDBC DatabaseMetaData method not implemented by JDBC driver - upgrade your driver", err);
  363 | 		}
  364 | 		finally {
  365 | 			DataSourceUtils.releaseConnection(con, dataSource);
  366 | 		}
  367 | 	}
  368 | 
  369 | 	/**
  370 | 	 * Call the specified method on DatabaseMetaData for the given DataSource,
  371 | 	 * and extract the invocation result.
  372 | 	 * @param dataSource the DataSource to extract meta-data for
  373 | 	 * @param metaDataMethodName the name of the DatabaseMetaData method to call
  374 | 	 * @return the object returned by the specified DatabaseMetaData method
  375 | 	 * @throws MetaDataAccessException if we couldn't access the DatabaseMetaData
  376 | 	 * or failed to invoke the specified method
  377 | 	 * @see java.sql.DatabaseMetaData
  378 | 	 * @deprecated in favor of
  379 | 	 * {@link #extractDatabaseMetaData(DataSource, DatabaseMetaDataCallback)}
  380 | 	 * with a lambda expression or method reference and a generically typed result
  381 | 	 */
  382 | 	@Deprecated(since = "5.2.9")
  383 | 	@SuppressWarnings("unchecked")
  384 | 	public static <T> T extractDatabaseMetaData(DataSource dataSource, final String metaDataMethodName)
  385 | 			throws MetaDataAccessException {
  386 | 
  387 | 		return (T) extractDatabaseMetaData(dataSource,
  388 | 				dbmd -> {
  389 | 					try {
  390 | 						return DatabaseMetaData.class.getMethod(metaDataMethodName).invoke(dbmd);
  391 | 					}
  392 | 					catch (NoSuchMethodException ex) {
  393 | 						throw new MetaDataAccessException("No method named '" + metaDataMethodName +
  394 | 								"' found on DatabaseMetaData instance [" + dbmd + "]", ex);
  395 | 					}
  396 | 					catch (IllegalAccessException ex) {
  397 | 						throw new MetaDataAccessException(
  398 | 								"Could not access DatabaseMetaData method '" + metaDataMethodName + "'", ex);
  399 | 					}
  400 | 					catch (InvocationTargetException ex) {
  401 | 						if (ex.getTargetException() instanceof SQLException sqlException) {
  402 | 							throw sqlException;
  403 | 						}
  404 | 						throw new MetaDataAccessException(
  405 | 								"Invocation of DatabaseMetaData method '" + metaDataMethodName + "' failed", ex);
  406 | 					}
  407 | 				});
  408 | 	}
  409 | 
  410 | 	/**
  411 | 	 * Return whether the given JDBC driver supports JDBC batch updates.
  412 | 	 * <p>Typically invoked right before execution of a given set of statements:
  413 | 	 * to decide whether the set of SQL statements should be executed through
  414 | 	 * the JDBC batch mechanism or simply in a traditional one-by-one fashion.
  415 | 	 * <p>Logs a warning if the "supportsBatchUpdates" methods throws an exception
  416 | 	 * and simply returns {@code false} in that case.
  417 | 	 * @param con the Connection to check
  418 | 	 * @return whether JDBC batch updates are supported
  419 | 	 * @see java.sql.DatabaseMetaData#supportsBatchUpdates()
  420 | 	 */
  421 | 	public static boolean supportsBatchUpdates(Connection con) {
  422 | 		try {
  423 | 			DatabaseMetaData dbmd = con.getMetaData();
  424 | 			if (dbmd != null) {
  425 | 				if (dbmd.supportsBatchUpdates()) {
  426 | 					logger.debug("JDBC driver supports batch updates");
  427 | 					return true;
  428 | 				}
  429 | 				else {
  430 | 					logger.debug("JDBC driver does not support batch updates");
  431 | 				}
  432 | 			}
  433 | 		}
  434 | 		catch (SQLException ex) {
  435 | 			logger.debug("JDBC driver 'supportsBatchUpdates' method threw exception", ex);
  436 | 		}
  437 | 		return false;
  438 | 	}
  439 | 
  440 | 	/**
  441 | 	 * Extract a common name for the target database in use even if
  442 | 	 * various drivers/platforms provide varying names at runtime.
  443 | 	 * @param source the name as provided in database meta-data
  444 | 	 * @return the common name to be used (for example, "DB2" or "Sybase")
  445 | 	 */
  446 | 	public static @Nullable String commonDatabaseName(@Nullable String source) {
  447 | 		String name = source;
  448 | 		if (source != null && source.startsWith("DB2")) {
  449 | 			name = "DB2";
  450 | 		}
  451 | 		else if ("Sybase SQL Server".equals(source) ||
  452 | 				"Adaptive Server Enterprise".equals(source) ||
  453 | 				"ASE".equals(source) ||
  454 | 				"sql server".equalsIgnoreCase(source) ) {
  455 | 			name = "Sybase";
  456 | 		}
  457 | 		return name;
  458 | 	}
  459 | 
  460 | 	/**
  461 | 	 * Check whether the given SQL type is numeric.
  462 | 	 * @param sqlType the SQL type to be checked
  463 | 	 * @return whether the type is numeric
  464 | 	 */
  465 | 	public static boolean isNumeric(int sqlType) {
  466 | 		return (Types.BIT == sqlType || Types.BIGINT == sqlType || Types.DECIMAL == sqlType ||
  467 | 				Types.DOUBLE == sqlType || Types.FLOAT == sqlType || Types.INTEGER == sqlType ||
  468 | 				Types.NUMERIC == sqlType || Types.REAL == sqlType || Types.SMALLINT == sqlType ||
  469 | 				Types.TINYINT == sqlType);
  470 | 	}
  471 | 
  472 | 	/**
  473 | 	 * Resolve the standard type name for the given SQL type, if possible.
  474 | 	 * @param sqlType the SQL type to resolve
  475 | 	 * @return the corresponding constant name in {@link java.sql.Types}
  476 | 	 * (for example, "VARCHAR"/"NUMERIC"), or {@code null} if not resolvable
  477 | 	 * @since 5.2
  478 | 	 */
  479 | 	public static @Nullable String resolveTypeName(int sqlType) {
  480 | 		return typeNames.get(sqlType);
  481 | 	}
  482 | 
  483 | 	/**
  484 | 	 * Determine the column name to use. The column name is determined based on a
  485 | 	 * lookup using ResultSetMetaData.
  486 | 	 * <p>This method's implementation takes into account clarifications expressed
  487 | 	 * in the JDBC 4.0 specification:
  488 | 	 * <p><i>columnLabel - the label for the column specified with the SQL AS clause.
  489 | 	 * If the SQL AS clause was not specified, then the label is the name of the column</i>.
  490 | 	 * @param resultSetMetaData the current meta-data to use
  491 | 	 * @param columnIndex the index of the column for the lookup
  492 | 	 * @return the column name to use
  493 | 	 * @throws SQLException in case of lookup failure
  494 | 	 */
  495 | 	public static String lookupColumnName(ResultSetMetaData resultSetMetaData, int columnIndex) throws SQLException {
  496 | 		String name = resultSetMetaData.getColumnLabel(columnIndex);
  497 | 		if (!StringUtils.hasLength(name)) {
  498 | 			name = resultSetMetaData.getColumnName(columnIndex);
  499 | 		}
  500 | 		return name;
  501 | 	}
  502 | 
  503 | 	/**
  504 | 	 * Convert a property name using "camelCase" to a corresponding column name with underscores.
  505 | 	 * A name like "customerNumber" would match a "customer_number" column name.
  506 | 	 * @param name the property name to be converted
  507 | 	 * @return the column name using underscores
  508 | 	 * @since 6.1
  509 | 	 * @see #convertUnderscoreNameToPropertyName
  510 | 	 */
  511 | 	public static String convertPropertyNameToUnderscoreName(@Nullable String name) {
  512 | 		if (!StringUtils.hasLength(name)) {
  513 | 			return "";
  514 | 		}
  515 | 
  516 | 		StringBuilder result = new StringBuilder();
  517 | 		result.append(Character.toLowerCase(name.charAt(0)));
  518 | 		for (int i = 1; i < name.length(); i++) {
  519 | 			char c = name.charAt(i);
  520 | 			if (Character.isUpperCase(c)) {
  521 | 				result.append('_').append(Character.toLowerCase(c));
  522 | 			}
  523 | 			else {
  524 | 				result.append(c);
  525 | 			}
  526 | 		}
  527 | 		return result.toString();
  528 | 	}
  529 | 
  530 | 	/**
  531 | 	 * Convert a column name with underscores to the corresponding property name using "camelCase".
  532 | 	 * A name like "customer_number" would match a "customerNumber" property name.
  533 | 	 * @param name the potentially underscores-based column name to be converted
  534 | 	 * @return the name using "camelCase"
  535 | 	 * @see #convertPropertyNameToUnderscoreName
  536 | 	 */
  537 | 	public static String convertUnderscoreNameToPropertyName(@Nullable String name) {
  538 | 		if (!StringUtils.hasLength(name)) {
  539 | 			return "";
  540 | 		}
  541 | 
  542 | 		StringBuilder result = new StringBuilder();
  543 | 		boolean nextIsUpper = false;
  544 | 		if (name.length() > 1 && name.charAt(1) == '_') {
  545 | 			result.append(Character.toUpperCase(name.charAt(0)));
  546 | 		}
  547 | 		else {
  548 | 			result.append(Character.toLowerCase(name.charAt(0)));
  549 | 		}
  550 | 		for (int i = 1; i < name.length(); i++) {
  551 | 			char c = name.charAt(i);
  552 | 			if (c == '_') {
  553 | 				nextIsUpper = true;
  554 | 			}
  555 | 			else {
  556 | 				if (nextIsUpper) {
  557 | 					result.append(Character.toUpperCase(c));
  558 | 					nextIsUpper = false;
  559 | 				}
  560 | 				else {
  561 | 					result.append(Character.toLowerCase(c));
  562 | 				}
  563 | 			}
  564 | 		}
  565 | 		return result.toString();
  566 | 	}
  567 | 
  568 | }
```
