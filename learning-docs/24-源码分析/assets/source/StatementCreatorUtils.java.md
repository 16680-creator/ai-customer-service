<!-- learning-docs source archive（机械抓取原文，禁止手改代码内容） -->
<!-- origin: org.springframework:spring-jdbc:7.1.0 | path: org/springframework/jdbc/core/StatementCreatorUtils.java -->
<!-- 正文引用：[[源码:StatementCreatorUtils.java#L起-L止]] ；代码块头部：(src: StatementCreatorUtils.java#L起-L止) -->

# StatementCreatorUtils.java

| 项 | 值 |
| --- | --- |
| 来源 | `org.springframework:spring-jdbc:7.1.0` |
| 路径 | `org/springframework/jdbc/core/StatementCreatorUtils.java` |
| 行数 | 522 |
| 在线地址 | https://repo1.maven.org/maven2/org/springframework/spring-jdbc/7.1.0/spring-jdbc-7.1.0-sources.jar |
| 抓取时间 | 2026-11-25T14:03:07.545Z |

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
   19 | import java.io.StringReader;
   20 | import java.io.StringWriter;
   21 | import java.math.BigDecimal;
   22 | import java.math.BigInteger;
   23 | import java.sql.Blob;
   24 | import java.sql.Clob;
   25 | import java.sql.DatabaseMetaData;
   26 | import java.sql.PreparedStatement;
   27 | import java.sql.SQLException;
   28 | import java.sql.SQLFeatureNotSupportedException;
   29 | import java.sql.Types;
   30 | import java.time.LocalDate;
   31 | import java.time.LocalDateTime;
   32 | import java.time.LocalTime;
   33 | import java.time.OffsetDateTime;
   34 | import java.time.OffsetTime;
   35 | import java.util.Arrays;
   36 | import java.util.Calendar;
   37 | import java.util.Collection;
   38 | import java.util.HashMap;
   39 | import java.util.Map;
   40 | 
   41 | import org.apache.commons.logging.Log;
   42 | import org.apache.commons.logging.LogFactory;
   43 | import org.jspecify.annotations.Nullable;
   44 | 
   45 | import org.springframework.core.SpringProperties;
   46 | import org.springframework.jdbc.support.SqlValue;
   47 | 
   48 | /**
   49 |  * Utility methods for PreparedStatementSetter/Creator and CallableStatementCreator
   50 |  * implementations, providing sophisticated parameter management (including support
   51 |  * for LOB values).
   52 |  *
   53 |  * <p>Used by PreparedStatementCreatorFactory and CallableStatementCreatorFactory,
   54 |  * but also available for direct use in custom setter/creator implementations.
   55 |  *
   56 |  * @author Thomas Risberg
   57 |  * @author Juergen Hoeller
   58 |  * @since 1.1
   59 |  * @see PreparedStatementSetter
   60 |  * @see PreparedStatementCreator
   61 |  * @see CallableStatementCreator
   62 |  * @see PreparedStatementCreatorFactory
   63 |  * @see CallableStatementCreatorFactory
   64 |  * @see SqlParameter
   65 |  * @see SqlTypeValue
   66 |  * @see org.springframework.jdbc.core.support.SqlLobValue
   67 |  */
   68 | public abstract class StatementCreatorUtils {
   69 | 
   70 | 	/**
   71 | 	 * System property that instructs Spring to ignore {@link java.sql.ParameterMetaData#getParameterType}
   72 | 	 * completely, i.e. to never even attempt to retrieve {@link PreparedStatement#getParameterMetaData()}
   73 | 	 * for {@link StatementCreatorUtils#setNull} calls.
   74 | 	 * <p>The default is "false", trying {@code getParameterType} calls first and falling back to
   75 | 	 * {@link PreparedStatement#setNull} / {@link PreparedStatement#setObject} calls based on
   76 | 	 * well-known behavior of common databases.
   77 | 	 * <p>Consider switching this flag to "true" if you experience misbehavior at runtime,
   78 | 	 * for example, with connection pool issues in case of an exception thrown from {@code getParameterType}
   79 | 	 * (as reported on JBoss AS 7) or in case of performance problems (as reported on PostgreSQL).
   80 | 	 */
   81 | 	public static final String IGNORE_GETPARAMETERTYPE_PROPERTY_NAME = "spring.jdbc.getParameterType.ignore";
   82 | 
   83 | 
   84 | 	private static final Log logger = LogFactory.getLog(StatementCreatorUtils.class);
   85 | 
   86 | 	private static final Map<Class<?>, Integer> javaTypeToSqlTypeMap = new HashMap<>(64);
   87 | 
   88 | 	static @Nullable Boolean shouldIgnoreGetParameterType = SpringProperties.checkFlag(IGNORE_GETPARAMETERTYPE_PROPERTY_NAME);
   89 | 
   90 | 	static {
   91 | 		javaTypeToSqlTypeMap.put(boolean.class, Types.BOOLEAN);
   92 | 		javaTypeToSqlTypeMap.put(Boolean.class, Types.BOOLEAN);
   93 | 		javaTypeToSqlTypeMap.put(byte.class, Types.TINYINT);
   94 | 		javaTypeToSqlTypeMap.put(Byte.class, Types.TINYINT);
   95 | 		javaTypeToSqlTypeMap.put(short.class, Types.SMALLINT);
   96 | 		javaTypeToSqlTypeMap.put(Short.class, Types.SMALLINT);
   97 | 		javaTypeToSqlTypeMap.put(int.class, Types.INTEGER);
   98 | 		javaTypeToSqlTypeMap.put(Integer.class, Types.INTEGER);
   99 | 		javaTypeToSqlTypeMap.put(long.class, Types.BIGINT);
  100 | 		javaTypeToSqlTypeMap.put(Long.class, Types.BIGINT);
  101 | 		javaTypeToSqlTypeMap.put(BigInteger.class, Types.BIGINT);
  102 | 		javaTypeToSqlTypeMap.put(float.class, Types.FLOAT);
  103 | 		javaTypeToSqlTypeMap.put(Float.class, Types.FLOAT);
  104 | 		javaTypeToSqlTypeMap.put(double.class, Types.DOUBLE);
  105 | 		javaTypeToSqlTypeMap.put(Double.class, Types.DOUBLE);
  106 | 		javaTypeToSqlTypeMap.put(BigDecimal.class, Types.DECIMAL);
  107 | 		javaTypeToSqlTypeMap.put(LocalDate.class, Types.DATE);
  108 | 		javaTypeToSqlTypeMap.put(LocalTime.class, Types.TIME);
  109 | 		javaTypeToSqlTypeMap.put(LocalDateTime.class, Types.TIMESTAMP);
  110 | 		javaTypeToSqlTypeMap.put(OffsetTime.class, Types.TIME_WITH_TIMEZONE);
  111 | 		javaTypeToSqlTypeMap.put(OffsetDateTime.class, Types.TIMESTAMP_WITH_TIMEZONE);
  112 | 		javaTypeToSqlTypeMap.put(java.sql.Date.class, Types.DATE);
  113 | 		javaTypeToSqlTypeMap.put(java.sql.Time.class, Types.TIME);
  114 | 		javaTypeToSqlTypeMap.put(java.sql.Timestamp.class, Types.TIMESTAMP);
  115 | 		javaTypeToSqlTypeMap.put(Blob.class, Types.BLOB);
  116 | 		javaTypeToSqlTypeMap.put(Clob.class, Types.CLOB);
  117 | 	}
  118 | 
  119 | 
  120 | 	/**
  121 | 	 * Derive a default SQL type from the given Java type.
  122 | 	 * @param javaType the Java type to translate
  123 | 	 * @return the corresponding SQL type, or {@link SqlTypeValue#TYPE_UNKNOWN} if none found
  124 | 	 */
  125 | 	public static int javaTypeToSqlParameterType(@Nullable Class<?> javaType) {
  126 | 		if (javaType == null) {
  127 | 			return SqlTypeValue.TYPE_UNKNOWN;
  128 | 		}
  129 | 		Integer sqlType = javaTypeToSqlTypeMap.get(javaType);
  130 | 		if (sqlType != null) {
  131 | 			return sqlType;
  132 | 		}
  133 | 		if (Number.class.isAssignableFrom(javaType)) {
  134 | 			return Types.NUMERIC;
  135 | 		}
  136 | 		if (isStringValue(javaType)) {
  137 | 			return Types.VARCHAR;
  138 | 		}
  139 | 		if (isDateValue(javaType) || Calendar.class.isAssignableFrom(javaType)) {
  140 | 			return Types.TIMESTAMP;
  141 | 		}
  142 | 		return SqlTypeValue.TYPE_UNKNOWN;
  143 | 	}
  144 | 
  145 | 	/**
  146 | 	 * Set the value for a parameter. The method used is based on the SQL type
  147 | 	 * of the parameter and we can handle complex types like arrays and LOBs.
  148 | 	 * @param ps the prepared statement or callable statement
  149 | 	 * @param paramIndex index of the parameter we are setting
  150 | 	 * @param param the parameter as it is declared including type
  151 | 	 * @param inValue the value to set
  152 | 	 * @throws SQLException if thrown by PreparedStatement methods
  153 | 	 */
  154 | 	public static void setParameterValue(PreparedStatement ps, int paramIndex, SqlParameter param,
  155 | 			@Nullable Object inValue) throws SQLException {
  156 | 
  157 | 		setParameterValueInternal(ps, paramIndex, param.getSqlType(), param.getTypeName(), param.getScale(), inValue);
  158 | 	}
  159 | 
  160 | 	/**
  161 | 	 * Set the value for a parameter. The method used is based on the SQL type
  162 | 	 * of the parameter and we can handle complex types like arrays and LOBs.
  163 | 	 * @param ps the prepared statement or callable statement
  164 | 	 * @param paramIndex index of the parameter we are setting
  165 | 	 * @param sqlType the SQL type of the parameter
  166 | 	 * @param inValue the value to set (plain value or an SqlTypeValue)
  167 | 	 * @throws SQLException if thrown by PreparedStatement methods
  168 | 	 * @see SqlTypeValue
  169 | 	 */
  170 | 	public static void setParameterValue(PreparedStatement ps, int paramIndex, int sqlType,
  171 | 			@Nullable Object inValue) throws SQLException {
  172 | 
  173 | 		setParameterValueInternal(ps, paramIndex, sqlType, null, null, inValue);
  174 | 	}
  175 | 
  176 | 	/**
  177 | 	 * Set the value for a parameter. The method used is based on the SQL type
  178 | 	 * of the parameter and we can handle complex types like arrays and LOBs.
  179 | 	 * @param ps the prepared statement or callable statement
  180 | 	 * @param paramIndex index of the parameter we are setting
  181 | 	 * @param sqlType the SQL type of the parameter
  182 | 	 * @param typeName the type name of the parameter
  183 | 	 * (optional, only used for SQL NULL and SqlTypeValue)
  184 | 	 * @param inValue the value to set (plain value or an SqlTypeValue)
  185 | 	 * @throws SQLException if thrown by PreparedStatement methods
  186 | 	 * @see SqlTypeValue
  187 | 	 */
  188 | 	public static void setParameterValue(PreparedStatement ps, int paramIndex, int sqlType, String typeName,
  189 | 			@Nullable Object inValue) throws SQLException {
  190 | 
  191 | 		setParameterValueInternal(ps, paramIndex, sqlType, typeName, null, inValue);
  192 | 	}
  193 | 
  194 | 	/**
  195 | 	 * Set the value for a parameter. The method used is based on the SQL type
  196 | 	 * of the parameter and we can handle complex types like arrays and LOBs.
  197 | 	 * @param ps the prepared statement or callable statement
  198 | 	 * @param paramIndex index of the parameter we are setting
  199 | 	 * @param sqlType the SQL type of the parameter
  200 | 	 * @param typeName the type name of the parameter
  201 | 	 * (optional, only used for SQL NULL and SqlTypeValue)
  202 | 	 * @param scale the number of digits after the decimal point
  203 | 	 * (for DECIMAL and NUMERIC types)
  204 | 	 * @param inValue the value to set (plain value or an SqlTypeValue)
  205 | 	 * @throws SQLException if thrown by PreparedStatement methods
  206 | 	 * @see SqlTypeValue
  207 | 	 */
  208 | 	private static void setParameterValueInternal(PreparedStatement ps, int paramIndex, int sqlType,
  209 | 			@Nullable String typeName, @Nullable Integer scale, @Nullable Object inValue) throws SQLException {
  210 | 
  211 | 		String typeNameToUse = typeName;
  212 | 		int sqlTypeToUse = sqlType;
  213 | 		Object inValueToUse = inValue;
  214 | 
  215 | 		// override type info?
  216 | 		if (inValue instanceof SqlParameterValue parameterValue) {
  217 | 			if (logger.isDebugEnabled()) {
  218 | 				logger.debug("Overriding type info with runtime info from SqlParameterValue: column index " + paramIndex +
  219 | 						", SQL type " + parameterValue.getSqlType() + ", type name " + parameterValue.getTypeName());
  220 | 			}
  221 | 			if (parameterValue.getSqlType() != SqlTypeValue.TYPE_UNKNOWN) {
  222 | 				sqlTypeToUse = parameterValue.getSqlType();
  223 | 			}
  224 | 			if (parameterValue.getTypeName() != null) {
  225 | 				typeNameToUse = parameterValue.getTypeName();
  226 | 			}
  227 | 			inValueToUse = parameterValue.getValue();
  228 | 		}
  229 | 
  230 | 		if (logger.isTraceEnabled()) {
  231 | 			logger.trace("Setting SQL statement parameter value: column index " + paramIndex +
  232 | 					", parameter value [" + inValueToUse +
  233 | 					"], value class [" + (inValueToUse != null ? inValueToUse.getClass().getName() : "null") +
  234 | 					"], SQL type " + (sqlTypeToUse == SqlTypeValue.TYPE_UNKNOWN ? "unknown" : Integer.toString(sqlTypeToUse)));
  235 | 		}
  236 | 
  237 | 		if (inValueToUse == null) {
  238 | 			setNull(ps, paramIndex, sqlTypeToUse, typeNameToUse);
  239 | 		}
  240 | 		else {
  241 | 			setValue(ps, paramIndex, sqlTypeToUse, typeNameToUse, scale, inValueToUse);
  242 | 		}
  243 | 	}
  244 | 
  245 | 	/**
  246 | 	 * Set the specified PreparedStatement parameter to null,
  247 | 	 * respecting database-specific peculiarities.
  248 | 	 */
  249 | 	private static void setNull(PreparedStatement ps, int paramIndex, int sqlType, @Nullable String typeName)
  250 | 			throws SQLException {
  251 | 
  252 | 		if (sqlType == SqlTypeValue.TYPE_UNKNOWN || (sqlType == Types.OTHER && typeName == null)) {
  253 | 			boolean callGetParameterType = false;
  254 | 			boolean useSetObject = false;
  255 | 			Integer sqlTypeToUse = null;
  256 | 			if (shouldIgnoreGetParameterType != null) {
  257 | 				callGetParameterType = !shouldIgnoreGetParameterType;
  258 | 			}
  259 | 			else {
  260 | 				String jdbcDriverName = ps.getConnection().getMetaData().getDriverName();
  261 | 				if (jdbcDriverName.startsWith("PostgreSQL")) {
  262 | 					sqlTypeToUse = Types.NULL;
  263 | 				}
  264 | 				else if (jdbcDriverName.startsWith("Microsoft") && jdbcDriverName.contains("SQL Server")) {
  265 | 					sqlTypeToUse = Types.NULL;
  266 | 					useSetObject = true;
  267 | 				}
  268 | 				else {
  269 | 					callGetParameterType = true;
  270 | 				}
  271 | 			}
  272 | 			if (callGetParameterType) {
  273 | 				try {
  274 | 					sqlTypeToUse = ps.getParameterMetaData().getParameterType(paramIndex);
  275 | 				}
  276 | 				catch (SQLException ex) {
  277 | 					if (logger.isDebugEnabled()) {
  278 | 						logger.debug("JDBC getParameterType call failed - using fallback method instead: " + ex);
  279 | 					}
  280 | 				}
  281 | 			}
  282 | 			if (sqlTypeToUse == null) {
  283 | 				// Proceed with database-specific checks
  284 | 				sqlTypeToUse = Types.NULL;
  285 | 				DatabaseMetaData dbmd = ps.getConnection().getMetaData();
  286 | 				String jdbcDriverName = dbmd.getDriverName();
  287 | 				String databaseProductName = dbmd.getDatabaseProductName();
  288 | 				if (databaseProductName.startsWith("Informix") ||
  289 | 						(jdbcDriverName.startsWith("Microsoft") && jdbcDriverName.contains("SQL Server"))) {
  290 | 						// "Microsoft SQL Server JDBC Driver 3.0" versus "Microsoft JDBC Driver 4.0 for SQL Server"
  291 | 					useSetObject = true;
  292 | 				}
  293 | 				else if (databaseProductName.startsWith("DB2") ||
  294 | 						jdbcDriverName.startsWith("jConnect") ||
  295 | 						jdbcDriverName.startsWith("SQLServer") ||
  296 | 						jdbcDriverName.startsWith("Apache Derby")) {
  297 | 					sqlTypeToUse = Types.VARCHAR;
  298 | 				}
  299 | 			}
  300 | 			if (useSetObject) {
  301 | 				ps.setObject(paramIndex, null);
  302 | 			}
  303 | 			else {
  304 | 				ps.setNull(paramIndex, sqlTypeToUse);
  305 | 			}
  306 | 		}
  307 | 		else if (typeName != null) {
  308 | 			ps.setNull(paramIndex, sqlType, typeName);
  309 | 		}
  310 | 		else {
  311 | 			// Fall back to generic setNull call.
  312 | 			try {
  313 | 				// Try generic setNull call with SQL type specified.
  314 | 				ps.setNull(paramIndex, sqlType);
  315 | 			}
  316 | 			catch (SQLFeatureNotSupportedException ex) {
  317 | 				if (sqlType == Types.NULL) {
  318 | 					throw ex;
  319 | 				}
  320 | 				// Fall back to generic setNull call without SQL type specified
  321 | 				// (for example, for MySQL TIME_WITH_TIMEZONE / TIMESTAMP_WITH_TIMEZONE).
  322 | 				ps.setNull(paramIndex, Types.NULL);
  323 | 			}
  324 | 		}
  325 | 	}
  326 | 
  327 | 	private static void setValue(PreparedStatement ps, int paramIndex, int sqlType,
  328 | 			@Nullable String typeName, @Nullable Integer scale, Object inValue) throws SQLException {
  329 | 
  330 | 		if (inValue instanceof SqlTypeValue sqlTypeValue) {
  331 | 			sqlTypeValue.setTypeValue(ps, paramIndex, sqlType, typeName);
  332 | 		}
  333 | 		else if (inValue instanceof SqlValue sqlValue) {
  334 | 			sqlValue.setValue(ps, paramIndex);
  335 | 		}
  336 | 		else if (sqlType == Types.VARCHAR || sqlType == Types.LONGVARCHAR ) {
  337 | 			ps.setString(paramIndex, inValue.toString());
  338 | 		}
  339 | 		else if (sqlType == Types.NVARCHAR || sqlType == Types.LONGNVARCHAR) {
  340 | 			ps.setNString(paramIndex, inValue.toString());
  341 | 		}
  342 | 		else if ((sqlType == Types.CLOB || sqlType == Types.NCLOB) && isStringValue(inValue.getClass())) {
  343 | 			String strVal = inValue.toString();
  344 | 			if (strVal.length() > 4000) {
  345 | 				// Necessary for older Oracle drivers, in particular when running against an Oracle 10 database.
  346 | 				// Should also work fine against other drivers/databases since it uses standard JDBC 4.0 API.
  347 | 				if (sqlType == Types.NCLOB) {
  348 | 					ps.setNClob(paramIndex, new StringReader(strVal), strVal.length());
  349 | 				}
  350 | 				else {
  351 | 					ps.setClob(paramIndex, new StringReader(strVal), strVal.length());
  352 | 				}
  353 | 			}
  354 | 			else {
  355 | 				// Fallback: setString or setNString binding
  356 | 				if (sqlType == Types.NCLOB) {
  357 | 					ps.setNString(paramIndex, strVal);
  358 | 				}
  359 | 				else {
  360 | 					ps.setString(paramIndex, strVal);
  361 | 				}
  362 | 			}
  363 | 		}
  364 | 		else if (sqlType == Types.DECIMAL || sqlType == Types.NUMERIC) {
  365 | 			if (inValue instanceof BigDecimal bigDecimal) {
  366 | 				ps.setBigDecimal(paramIndex, bigDecimal);
  367 | 			}
  368 | 			else if (scale != null) {
  369 | 				ps.setObject(paramIndex, inValue, sqlType, scale);
  370 | 			}
  371 | 			else {
  372 | 				ps.setObject(paramIndex, inValue, sqlType);
  373 | 			}
  374 | 		}
  375 | 		else if (sqlType == Types.BOOLEAN) {
  376 | 			if (inValue instanceof Boolean flag) {
  377 | 				ps.setBoolean(paramIndex, flag);
  378 | 			}
  379 | 			else {
  380 | 				ps.setObject(paramIndex, inValue, Types.BOOLEAN);
  381 | 			}
  382 | 		}
  383 | 		else if (sqlType == Types.DATE) {
  384 | 			if (inValue instanceof java.util.Date date) {
  385 | 				if (inValue instanceof java.sql.Date sqlDate) {
  386 | 					ps.setDate(paramIndex, sqlDate);
  387 | 				}
  388 | 				else {
  389 | 					ps.setDate(paramIndex, new java.sql.Date(date.getTime()));
  390 | 				}
  391 | 			}
  392 | 			else if (inValue instanceof Calendar cal) {
  393 | 				ps.setDate(paramIndex, new java.sql.Date(cal.getTime().getTime()), cal);
  394 | 			}
  395 | 			else {
  396 | 				ps.setObject(paramIndex, inValue, Types.DATE);
  397 | 			}
  398 | 		}
  399 | 		else if (sqlType == Types.TIME) {
  400 | 			if (inValue instanceof java.util.Date date) {
  401 | 				if (inValue instanceof java.sql.Time time) {
  402 | 					ps.setTime(paramIndex, time);
  403 | 				}
  404 | 				else {
  405 | 					ps.setTime(paramIndex, new java.sql.Time(date.getTime()));
  406 | 				}
  407 | 			}
  408 | 			else if (inValue instanceof Calendar cal) {
  409 | 				ps.setTime(paramIndex, new java.sql.Time(cal.getTime().getTime()), cal);
  410 | 			}
  411 | 			else {
  412 | 				ps.setObject(paramIndex, inValue, Types.TIME);
  413 | 			}
  414 | 		}
  415 | 		else if (sqlType == Types.TIMESTAMP) {
  416 | 			if (inValue instanceof java.util.Date date) {
  417 | 				if (inValue instanceof java.sql.Timestamp timestamp) {
  418 | 					ps.setTimestamp(paramIndex, timestamp);
  419 | 				}
  420 | 				else {
  421 | 					ps.setTimestamp(paramIndex, new java.sql.Timestamp(date.getTime()));
  422 | 				}
  423 | 			}
  424 | 			else if (inValue instanceof Calendar cal) {
  425 | 				ps.setTimestamp(paramIndex, new java.sql.Timestamp(cal.getTime().getTime()), cal);
  426 | 			}
  427 | 			else {
  428 | 				ps.setObject(paramIndex, inValue, Types.TIMESTAMP);
  429 | 			}
  430 | 		}
  431 | 		else if (sqlType == SqlTypeValue.TYPE_UNKNOWN || (sqlType == Types.OTHER &&
  432 | 				"Oracle".equals(ps.getConnection().getMetaData().getDatabaseProductName()))) {
  433 | 			if (inValue instanceof byte[] bytes) {
  434 | 				ps.setBytes(paramIndex, bytes);
  435 | 			}
  436 | 			else if (isStringValue(inValue.getClass())) {
  437 | 				ps.setString(paramIndex, inValue.toString());
  438 | 			}
  439 | 			else if (isDateValue(inValue.getClass())) {
  440 | 				ps.setTimestamp(paramIndex, new java.sql.Timestamp(((java.util.Date) inValue).getTime()));
  441 | 			}
  442 | 			else if (inValue instanceof Calendar cal) {
  443 | 				ps.setTimestamp(paramIndex, new java.sql.Timestamp(cal.getTime().getTime()), cal);
  444 | 			}
  445 | 			else {
  446 | 				// Fall back to generic setObject call without SQL type specified.
  447 | 				ps.setObject(paramIndex, inValue);
  448 | 			}
  449 | 		}
  450 | 		else {
  451 | 			// Fall back to generic setObject call.
  452 | 			try {
  453 | 				// Try generic setObject call with SQL type specified.
  454 | 				ps.setObject(paramIndex, inValue, sqlType);
  455 | 			}
  456 | 			catch (SQLFeatureNotSupportedException ex) {
  457 | 				// Fall back to generic setObject call without SQL type specified
  458 | 				// (for example, for MySQL TIME_WITH_TIMEZONE / TIMESTAMP_WITH_TIMEZONE).
  459 | 				ps.setObject(paramIndex, inValue);
  460 | 			}
  461 | 		}
  462 | 	}
  463 | 
  464 | 	/**
  465 | 	 * Check whether the given value can be treated as a String value.
  466 | 	 */
  467 | 	private static boolean isStringValue(Class<?> inValueType) {
  468 | 		// Consider any CharSequence (including StringBuffer and StringBuilder) as a String.
  469 | 		return (CharSequence.class.isAssignableFrom(inValueType) ||
  470 | 				StringWriter.class.isAssignableFrom(inValueType));
  471 | 	}
  472 | 
  473 | 	/**
  474 | 	 * Check whether the given value is a {@code java.util.Date}
  475 | 	 * (but not one of the JDBC-specific subclasses).
  476 | 	 */
  477 | 	private static boolean isDateValue(Class<?> inValueType) {
  478 | 		return (java.util.Date.class.isAssignableFrom(inValueType) &&
  479 | 				!(java.sql.Date.class.isAssignableFrom(inValueType) ||
  480 | 						java.sql.Time.class.isAssignableFrom(inValueType) ||
  481 | 						java.sql.Timestamp.class.isAssignableFrom(inValueType)));
  482 | 	}
  483 | 
  484 | 	/**
  485 | 	 * Clean up all resources held by parameter values which were passed to an
  486 | 	 * execute method. This is for example important for closing LOB values.
  487 | 	 * @param paramValues parameter values supplied. May be {@code null}.
  488 | 	 * @see DisposableSqlTypeValue#cleanup()
  489 | 	 * @see org.springframework.jdbc.core.support.SqlLobValue#cleanup()
  490 | 	 */
  491 | 	public static void cleanupParameters(@Nullable Object @Nullable ... paramValues) {
  492 | 		if (paramValues != null) {
  493 | 			cleanupParameters(Arrays.asList(paramValues));
  494 | 		}
  495 | 	}
  496 | 
  497 | 	/**
  498 | 	 * Clean up all resources held by parameter values which were passed to an
  499 | 	 * execute method. This is for example important for closing LOB values.
  500 | 	 * @param paramValues parameter values supplied. May be {@code null}.
  501 | 	 * @see DisposableSqlTypeValue#cleanup()
  502 | 	 * @see org.springframework.jdbc.core.support.SqlLobValue#cleanup()
  503 | 	 */
  504 | 	public static void cleanupParameters(@Nullable Collection<?> paramValues) {
  505 | 		if (paramValues != null) {
  506 | 			for (Object inValue : paramValues) {
  507 | 				// Unwrap SqlParameterValue first...
  508 | 				if (inValue instanceof SqlParameterValue sqlParameterValue) {
  509 | 					inValue = sqlParameterValue.getValue();
  510 | 				}
  511 | 				// Check for disposable value types
  512 | 				if (inValue instanceof SqlValue sqlValue) {
  513 | 					sqlValue.cleanup();
  514 | 				}
  515 | 				else if (inValue instanceof DisposableSqlTypeValue disposableSqlTypeValue) {
  516 | 					disposableSqlTypeValue.cleanup();
  517 | 				}
  518 | 			}
  519 | 		}
  520 | 	}
  521 | 
  522 | }
```
