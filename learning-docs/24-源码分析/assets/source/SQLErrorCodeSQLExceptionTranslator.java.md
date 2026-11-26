<!-- learning-docs source archive（机械抓取原文，禁止手改代码内容） -->
<!-- origin: org.springframework:spring-jdbc:7.1.0 | path: org/springframework/jdbc/support/SQLErrorCodeSQLExceptionTranslator.java -->
<!-- 正文引用：[[源码:SQLErrorCodeSQLExceptionTranslator.java#L起-L止]] ；代码块头部：(src: SQLErrorCodeSQLExceptionTranslator.java#L起-L止) -->

# SQLErrorCodeSQLExceptionTranslator.java

| 项 | 值 |
| --- | --- |
| 来源 | `org.springframework:spring-jdbc:7.1.0` |
| 路径 | `org/springframework/jdbc/support/SQLErrorCodeSQLExceptionTranslator.java` |
| 行数 | 433 |
| 在线地址 | https://repo1.maven.org/maven2/org/springframework/spring-jdbc/7.1.0/spring-jdbc-7.1.0-sources.jar |
| 抓取时间 | 2026-11-25T14:00:09.927Z |

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
   19 | import java.lang.reflect.Constructor;
   20 | import java.sql.BatchUpdateException;
   21 | import java.sql.SQLException;
   22 | import java.util.Arrays;
   23 | 
   24 | import javax.sql.DataSource;
   25 | 
   26 | import org.jspecify.annotations.Nullable;
   27 | 
   28 | import org.springframework.core.io.ClassPathResource;
   29 | import org.springframework.dao.CannotAcquireLockException;
   30 | import org.springframework.dao.DataAccessException;
   31 | import org.springframework.dao.DataAccessResourceFailureException;
   32 | import org.springframework.dao.DataIntegrityViolationException;
   33 | import org.springframework.dao.DuplicateKeyException;
   34 | import org.springframework.dao.PermissionDeniedDataAccessException;
   35 | import org.springframework.dao.TransientDataAccessResourceException;
   36 | import org.springframework.jdbc.BadSqlGrammarException;
   37 | import org.springframework.jdbc.InvalidResultSetAccessException;
   38 | import org.springframework.util.function.SingletonSupplier;
   39 | import org.springframework.util.function.SupplierUtils;
   40 | 
   41 | /**
   42 |  * Implementation of {@link SQLExceptionTranslator} that analyzes vendor-specific error codes.
   43 |  * More precise than an implementation based on SQL state, but heavily vendor-specific.
   44 |  *
   45 |  * <p>This class applies the following matching rules:
   46 |  * <ul>
   47 |  * <li>Try custom translation implemented by any subclass. Note that this class is
   48 |  * concrete and is typically used itself, in which case this rule doesn't apply.
   49 |  * <li>Apply error code matching. Error codes are obtained from the SQLErrorCodesFactory
   50 |  * by default. This factory loads a "sql-error-codes.xml" file from the class path,
   51 |  * defining error code mappings for database names from database meta-data.
   52 |  * <li>Fallback to a fallback translator. {@link SQLStateSQLExceptionTranslator} is the
   53 |  * default fallback translator, analyzing the exception's SQL state only. Since Java 6
   54 |  * which introduces its own {@code SQLException} subclass hierarchy, we use
   55 |  * {@link SQLExceptionSubclassTranslator} by default, which in turns falls back
   56 |  * to Spring's own SQL state translation when not encountering specific subclasses.
   57 |  * </ul>
   58 |  *
   59 |  * <p>The configuration file named "sql-error-codes.xml" is by default read from
   60 |  * this package. It can be overridden through a file of the same name in the root
   61 |  * of the class path (for example, in the "/WEB-INF/classes" directory), as long as the
   62 |  * Spring JDBC package is loaded from the same ClassLoader.
   63 |  *
   64 |  * <p>This translator is commonly used by default if a user-provided `sql-error-codes.xml`
   65 |  * file has been found in the root of the classpath, as a signal to use this strategy.
   66 |  * Otherwise, {@link SQLExceptionSubclassTranslator} serves as the default translator
   67 |  * as of 6.0.
   68 |  *
   69 |  * @author Rod Johnson
   70 |  * @author Thomas Risberg
   71 |  * @author Juergen Hoeller
   72 |  * @author Sam Brannen
   73 |  * @see SQLErrorCodesFactory
   74 |  * @see SQLStateSQLExceptionTranslator
   75 |  */
   76 | public class SQLErrorCodeSQLExceptionTranslator extends AbstractFallbackSQLExceptionTranslator {
   77 | 
   78 | 	private static final int MESSAGE_ONLY_CONSTRUCTOR = 1;
   79 | 	private static final int MESSAGE_THROWABLE_CONSTRUCTOR = 2;
   80 | 	private static final int MESSAGE_SQLEX_CONSTRUCTOR = 3;
   81 | 	private static final int MESSAGE_SQL_THROWABLE_CONSTRUCTOR = 4;
   82 | 	private static final int MESSAGE_SQL_SQLEX_CONSTRUCTOR = 5;
   83 | 
   84 | 	private static final boolean userProvidedErrorCodesFilePresent =
   85 | 			new ClassPathResource(SQLErrorCodesFactory.SQL_ERROR_CODE_OVERRIDE_PATH,
   86 | 					SQLErrorCodesFactory.class.getClassLoader()).exists();
   87 | 
   88 | 	private @Nullable SingletonSupplier<SQLErrorCodes> sqlErrorCodes;
   89 | 
   90 | 
   91 | 	/**
   92 | 	 * Constructor for use as a JavaBean.
   93 | 	 * The SqlErrorCodes or DataSource property must be set.
   94 | 	 */
   95 | 	public SQLErrorCodeSQLExceptionTranslator() {
   96 | 		setFallbackTranslator(new SQLExceptionSubclassTranslator());
   97 | 	}
   98 | 
   99 | 	/**
  100 | 	 * Create an SQL error code translator for the given DataSource.
  101 | 	 * Invoking this constructor will cause a Connection to be obtained
  102 | 	 * from the DataSource to get the meta-data.
  103 | 	 * @param dataSource the DataSource to use to find meta-data and establish
  104 | 	 * which error codes are usable
  105 | 	 * @see SQLErrorCodesFactory
  106 | 	 */
  107 | 	public SQLErrorCodeSQLExceptionTranslator(DataSource dataSource) {
  108 | 		this();
  109 | 		setDataSource(dataSource);
  110 | 	}
  111 | 
  112 | 	/**
  113 | 	 * Create an SQL error code translator for the given database product name.
  114 | 	 * Invoking this constructor will avoid obtaining a Connection from the
  115 | 	 * DataSource to get the meta-data.
  116 | 	 * @param dbName the database product name that identifies the error codes entry
  117 | 	 * @see SQLErrorCodesFactory
  118 | 	 * @see java.sql.DatabaseMetaData#getDatabaseProductName()
  119 | 	 */
  120 | 	public SQLErrorCodeSQLExceptionTranslator(String dbName) {
  121 | 		this();
  122 | 		setDatabaseProductName(dbName);
  123 | 	}
  124 | 
  125 | 	/**
  126 | 	 * Create an SQLErrorCode translator given these error codes.
  127 | 	 * Does not require a database meta-data lookup to be performed using a connection.
  128 | 	 * @param sec error codes
  129 | 	 */
  130 | 	public SQLErrorCodeSQLExceptionTranslator(SQLErrorCodes sec) {
  131 | 		this();
  132 | 		this.sqlErrorCodes = SingletonSupplier.of(sec);
  133 | 	}
  134 | 
  135 | 
  136 | 	/**
  137 | 	 * Set the DataSource for this translator.
  138 | 	 * <p>Setting this property will cause a Connection to be obtained from
  139 | 	 * the DataSource to get the meta-data.
  140 | 	 * @param dataSource the DataSource to use to find meta-data and establish
  141 | 	 * which error codes are usable
  142 | 	 * @see SQLErrorCodesFactory#getErrorCodes(javax.sql.DataSource)
  143 | 	 * @see java.sql.DatabaseMetaData#getDatabaseProductName()
  144 | 	 */
  145 | 	public void setDataSource(DataSource dataSource) {
  146 | 		this.sqlErrorCodes =
  147 | 				SingletonSupplier.of(() -> SQLErrorCodesFactory.getInstance().resolveErrorCodes(dataSource));
  148 | 		this.sqlErrorCodes.get();  // try early initialization - otherwise the supplier will retry later
  149 | 	}
  150 | 
  151 | 	/**
  152 | 	 * Set the database product name for this translator.
  153 | 	 * <p>Setting this property will avoid obtaining a Connection from the DataSource
  154 | 	 * to get the meta-data.
  155 | 	 * @param dbName the database product name that identifies the error codes entry
  156 | 	 * @see SQLErrorCodesFactory#getErrorCodes(String)
  157 | 	 * @see java.sql.DatabaseMetaData#getDatabaseProductName()
  158 | 	 */
  159 | 	public void setDatabaseProductName(String dbName) {
  160 | 		this.sqlErrorCodes = SingletonSupplier.of(SQLErrorCodesFactory.getInstance().getErrorCodes(dbName));
  161 | 	}
  162 | 
  163 | 	/**
  164 | 	 * Set custom error codes to be used for translation.
  165 | 	 * @param sec custom error codes to use
  166 | 	 */
  167 | 	public void setSqlErrorCodes(@Nullable SQLErrorCodes sec) {
  168 | 		this.sqlErrorCodes = SingletonSupplier.ofNullable(sec);
  169 | 	}
  170 | 
  171 | 	/**
  172 | 	 * Return the error codes used by this translator.
  173 | 	 * Usually determined via a DataSource.
  174 | 	 * @see #setDataSource
  175 | 	 */
  176 | 	public @Nullable SQLErrorCodes getSqlErrorCodes() {
  177 | 		return SupplierUtils.resolve(this.sqlErrorCodes);
  178 | 	}
  179 | 
  180 | 
  181 | 	@SuppressWarnings("deprecation")
  182 | 	@Override
  183 | 	protected @Nullable DataAccessException doTranslate(String task, @Nullable String sql, SQLException ex) {
  184 | 		SQLException sqlEx = ex;
  185 | 		if (sqlEx instanceof BatchUpdateException && sqlEx.getNextException() != null) {
  186 | 			SQLException nestedSqlEx = sqlEx.getNextException();
  187 | 			if (nestedSqlEx.getErrorCode() > 0 || nestedSqlEx.getSQLState() != null) {
  188 | 				sqlEx = nestedSqlEx;
  189 | 			}
  190 | 		}
  191 | 
  192 | 		// First, try custom translation from overridden method.
  193 | 		DataAccessException dae = customTranslate(task, sql, sqlEx);
  194 | 		if (dae != null) {
  195 | 			return dae;
  196 | 		}
  197 | 
  198 | 		// Next, try the custom SQLException translator, if available.
  199 | 		SQLErrorCodes sqlErrorCodes = getSqlErrorCodes();
  200 | 		if (sqlErrorCodes != null) {
  201 | 			SQLExceptionTranslator customTranslator = sqlErrorCodes.getCustomSqlExceptionTranslator();
  202 | 			if (customTranslator != null) {
  203 | 				dae = customTranslator.translate(task, sql, sqlEx);
  204 | 				if (dae != null) {
  205 | 					return dae;
  206 | 				}
  207 | 			}
  208 | 		}
  209 | 
  210 | 		// Check SQLErrorCodes with corresponding error code, if available.
  211 | 		if (sqlErrorCodes != null) {
  212 | 			String errorCode;
  213 | 			if (sqlErrorCodes.isUseSqlStateForTranslation()) {
  214 | 				errorCode = sqlEx.getSQLState();
  215 | 			}
  216 | 			else {
  217 | 				// Try to find SQLException with actual error code, looping through the causes.
  218 | 				// For example, applicable to java.sql.DataTruncation as of JDK 1.6.
  219 | 				SQLException current = sqlEx;
  220 | 				while (current.getErrorCode() == 0 && current.getCause() instanceof SQLException sqlException) {
  221 | 					current = sqlException;
  222 | 				}
  223 | 				errorCode = Integer.toString(current.getErrorCode());
  224 | 			}
  225 | 
  226 | 			if (errorCode != null) {
  227 | 				// Look for defined custom translations first.
  228 | 				CustomSQLErrorCodesTranslation[] customTranslations = sqlErrorCodes.getCustomTranslations();
  229 | 				if (customTranslations != null) {
  230 | 					for (CustomSQLErrorCodesTranslation customTranslation : customTranslations) {
  231 | 						if (Arrays.binarySearch(customTranslation.getErrorCodes(), errorCode) >= 0 &&
  232 | 								customTranslation.getExceptionClass() != null) {
  233 | 							dae = createCustomException(task, sql, sqlEx, customTranslation.getExceptionClass());
  234 | 							if (dae != null) {
  235 | 								logTranslation(task, sql, sqlEx, true);
  236 | 								return dae;
  237 | 							}
  238 | 						}
  239 | 					}
  240 | 				}
  241 | 				// Next, look for grouped error codes.
  242 | 				if (Arrays.binarySearch(sqlErrorCodes.getBadSqlGrammarCodes(), errorCode) >= 0) {
  243 | 					logTranslation(task, sql, sqlEx, false);
  244 | 					return new BadSqlGrammarException(task, (sql != null ? sql : ""), sqlEx);
  245 | 				}
  246 | 				else if (Arrays.binarySearch(sqlErrorCodes.getInvalidResultSetAccessCodes(), errorCode) >= 0) {
  247 | 					logTranslation(task, sql, sqlEx, false);
  248 | 					return new InvalidResultSetAccessException(task, (sql != null ? sql : ""), sqlEx);
  249 | 				}
  250 | 				else if (Arrays.binarySearch(sqlErrorCodes.getDuplicateKeyCodes(), errorCode) >= 0) {
  251 | 					logTranslation(task, sql, sqlEx, false);
  252 | 					return new DuplicateKeyException(buildMessage(task, sql, sqlEx), sqlEx);
  253 | 				}
  254 | 				else if (Arrays.binarySearch(sqlErrorCodes.getDataIntegrityViolationCodes(), errorCode) >= 0) {
  255 | 					logTranslation(task, sql, sqlEx, false);
  256 | 					return new DataIntegrityViolationException(buildMessage(task, sql, sqlEx), sqlEx);
  257 | 				}
  258 | 				else if (Arrays.binarySearch(sqlErrorCodes.getPermissionDeniedCodes(), errorCode) >= 0) {
  259 | 					logTranslation(task, sql, sqlEx, false);
  260 | 					return new PermissionDeniedDataAccessException(buildMessage(task, sql, sqlEx), sqlEx);
  261 | 				}
  262 | 				else if (Arrays.binarySearch(sqlErrorCodes.getDataAccessResourceFailureCodes(), errorCode) >= 0) {
  263 | 					logTranslation(task, sql, sqlEx, false);
  264 | 					return new DataAccessResourceFailureException(buildMessage(task, sql, sqlEx), sqlEx);
  265 | 				}
  266 | 				else if (Arrays.binarySearch(sqlErrorCodes.getTransientDataAccessResourceCodes(), errorCode) >= 0) {
  267 | 					logTranslation(task, sql, sqlEx, false);
  268 | 					return new TransientDataAccessResourceException(buildMessage(task, sql, sqlEx), sqlEx);
  269 | 				}
  270 | 				else if (Arrays.binarySearch(sqlErrorCodes.getCannotAcquireLockCodes(), errorCode) >= 0) {
  271 | 					logTranslation(task, sql, sqlEx, false);
  272 | 					return new CannotAcquireLockException(buildMessage(task, sql, sqlEx), sqlEx);
  273 | 				}
  274 | 				else if (Arrays.binarySearch(sqlErrorCodes.getDeadlockLoserCodes(), errorCode) >= 0) {
  275 | 					logTranslation(task, sql, sqlEx, false);
  276 | 					return new org.springframework.dao.DeadlockLoserDataAccessException(buildMessage(task, sql, sqlEx), sqlEx);
  277 | 				}
  278 | 				else if (Arrays.binarySearch(sqlErrorCodes.getCannotSerializeTransactionCodes(), errorCode) >= 0) {
  279 | 					logTranslation(task, sql, sqlEx, false);
  280 | 					return new org.springframework.dao.CannotSerializeTransactionException(buildMessage(task, sql, sqlEx), sqlEx);
  281 | 				}
  282 | 			}
  283 | 		}
  284 | 
  285 | 		// We couldn't identify it more precisely - let's hand it over to the SQLState fallback translator.
  286 | 		if (logger.isDebugEnabled()) {
  287 | 			String codes;
  288 | 			if (sqlErrorCodes != null && sqlErrorCodes.isUseSqlStateForTranslation()) {
  289 | 				codes = "SQL state '" + sqlEx.getSQLState() + "', error code '" + sqlEx.getErrorCode();
  290 | 			}
  291 | 			else {
  292 | 				codes = "Error code '" + sqlEx.getErrorCode() + "'";
  293 | 			}
  294 | 			logger.debug("Unable to translate SQLException with " + codes + ", will now try the fallback translator");
  295 | 		}
  296 | 
  297 | 		return null;
  298 | 	}
  299 | 
  300 | 	/**
  301 | 	 * Subclasses can override this method to attempt a custom mapping from
  302 | 	 * {@link SQLException} to {@link DataAccessException}.
  303 | 	 * @param task readable text describing the task being attempted
  304 | 	 * @param sql the SQL query or update that caused the problem (may be {@code null})
  305 | 	 * @param sqlEx the offending SQLException
  306 | 	 * @return {@code null} if no custom translation applies, otherwise a {@link DataAccessException}
  307 | 	 * resulting from custom translation. This exception should include the {@code sqlEx} parameter
  308 | 	 * as a nested root cause. This implementation always returns {@code null}, meaning that the
  309 | 	 * translator always falls back to the default error codes.
  310 | 	 * @deprecated as of 6.1, in favor of {@link #setCustomTranslator}
  311 | 	 */
  312 | 	@Deprecated(since = "6.1")
  313 | 	protected @Nullable DataAccessException customTranslate(String task, @Nullable String sql, SQLException sqlEx) {
  314 | 		return null;
  315 | 	}
  316 | 
  317 | 	/**
  318 | 	 * Create a custom {@link DataAccessException}, based on a given exception
  319 | 	 * class from a {@link CustomSQLErrorCodesTranslation} definition.
  320 | 	 * @param task readable text describing the task being attempted
  321 | 	 * @param sql the SQL query or update that caused the problem (may be {@code null})
  322 | 	 * @param sqlEx the offending SQLException
  323 | 	 * @param exceptionClass the exception class to use, as defined in the
  324 | 	 * {@link CustomSQLErrorCodesTranslation} definition
  325 | 	 * @return {@code null} if the custom exception could not be created, otherwise
  326 | 	 * the resulting {@link DataAccessException}. This exception should include the
  327 | 	 * {@code sqlEx} parameter as a nested root cause.
  328 | 	 * @see CustomSQLErrorCodesTranslation#setExceptionClass
  329 | 	 */
  330 | 	protected @Nullable DataAccessException createCustomException(
  331 | 			String task, @Nullable String sql, SQLException sqlEx, Class<?> exceptionClass) {
  332 | 
  333 | 		// Find appropriate constructor for the given exception class
  334 | 		try {
  335 | 			int constructorType = 0;
  336 | 			Constructor<?>[] constructors = exceptionClass.getConstructors();
  337 | 			for (Constructor<?> constructor : constructors) {
  338 | 				Class<?>[] parameterTypes = constructor.getParameterTypes();
  339 | 				if (parameterTypes.length == 1 && String.class == parameterTypes[0] &&
  340 | 						constructorType < MESSAGE_ONLY_CONSTRUCTOR) {
  341 | 					constructorType = MESSAGE_ONLY_CONSTRUCTOR;
  342 | 				}
  343 | 				if (parameterTypes.length == 2 && String.class == parameterTypes[0] &&
  344 | 						Throwable.class == parameterTypes[1] &&
  345 | 						constructorType < MESSAGE_THROWABLE_CONSTRUCTOR) {
  346 | 					constructorType = MESSAGE_THROWABLE_CONSTRUCTOR;
  347 | 				}
  348 | 				if (parameterTypes.length == 2 && String.class == parameterTypes[0] &&
  349 | 						SQLException.class == parameterTypes[1] &&
  350 | 						constructorType < MESSAGE_SQLEX_CONSTRUCTOR) {
  351 | 					constructorType = MESSAGE_SQLEX_CONSTRUCTOR;
  352 | 				}
  353 | 				if (parameterTypes.length == 3 && String.class == parameterTypes[0] &&
  354 | 						String.class == parameterTypes[1] && Throwable.class == parameterTypes[2] &&
  355 | 						constructorType < MESSAGE_SQL_THROWABLE_CONSTRUCTOR) {
  356 | 					constructorType = MESSAGE_SQL_THROWABLE_CONSTRUCTOR;
  357 | 				}
  358 | 				if (parameterTypes.length == 3 && String.class == parameterTypes[0] &&
  359 | 						String.class == parameterTypes[1] && SQLException.class == parameterTypes[2] &&
  360 | 						constructorType < MESSAGE_SQL_SQLEX_CONSTRUCTOR) {
  361 | 					constructorType = MESSAGE_SQL_SQLEX_CONSTRUCTOR;
  362 | 				}
  363 | 			}
  364 | 
  365 | 			// invoke constructor
  366 | 			Constructor<?> exceptionConstructor;
  367 | 			return switch (constructorType) {
  368 | 				case MESSAGE_SQL_SQLEX_CONSTRUCTOR -> {
  369 | 					Class<?>[] messageAndSqlAndSqlExArgsClass = new Class<?>[] {String.class, String.class, SQLException.class};
  370 | 					Object[] messageAndSqlAndSqlExArgs = new Object[] {task, sql, sqlEx};
  371 | 					exceptionConstructor = exceptionClass.getConstructor(messageAndSqlAndSqlExArgsClass);
  372 | 					yield (DataAccessException) exceptionConstructor.newInstance(messageAndSqlAndSqlExArgs);
  373 | 				}
  374 | 				case MESSAGE_SQL_THROWABLE_CONSTRUCTOR -> {
  375 | 					Class<?>[] messageAndSqlAndThrowableArgsClass = new Class<?>[] {String.class, String.class, Throwable.class};
  376 | 					Object[] messageAndSqlAndThrowableArgs = new Object[] {task, sql, sqlEx};
  377 | 					exceptionConstructor = exceptionClass.getConstructor(messageAndSqlAndThrowableArgsClass);
  378 | 					yield (DataAccessException) exceptionConstructor.newInstance(messageAndSqlAndThrowableArgs);
  379 | 				}
  380 | 				case MESSAGE_SQLEX_CONSTRUCTOR -> {
  381 | 					Class<?>[] messageAndSqlExArgsClass = new Class<?>[] {String.class, SQLException.class};
  382 | 					Object[] messageAndSqlExArgs = new Object[] {task + ": " + sqlEx.getMessage(), sqlEx};
  383 | 					exceptionConstructor = exceptionClass.getConstructor(messageAndSqlExArgsClass);
  384 | 					yield (DataAccessException) exceptionConstructor.newInstance(messageAndSqlExArgs);
  385 | 				}
  386 | 				case MESSAGE_THROWABLE_CONSTRUCTOR -> {
  387 | 					Class<?>[] messageAndThrowableArgsClass = new Class<?>[] {String.class, Throwable.class};
  388 | 					Object[] messageAndThrowableArgs = new Object[] {task + ": " + sqlEx.getMessage(), sqlEx};
  389 | 					exceptionConstructor = exceptionClass.getConstructor(messageAndThrowableArgsClass);
  390 | 					yield (DataAccessException)exceptionConstructor.newInstance(messageAndThrowableArgs);
  391 | 				}
  392 | 				case MESSAGE_ONLY_CONSTRUCTOR -> {
  393 | 					Class<?>[] messageOnlyArgsClass = new Class<?>[] {String.class};
  394 | 					Object[] messageOnlyArgs = new Object[] {task + ": " + sqlEx.getMessage()};
  395 | 					exceptionConstructor = exceptionClass.getConstructor(messageOnlyArgsClass);
  396 | 					yield (DataAccessException) exceptionConstructor.newInstance(messageOnlyArgs);
  397 | 				}
  398 | 				default -> {
  399 | 					if (logger.isWarnEnabled()) {
  400 | 						logger.warn("Unable to find appropriate constructor of custom exception class [" +
  401 | 								exceptionClass.getName() + "]");
  402 | 					}
  403 | 					yield null;
  404 | 				}
  405 | 			};
  406 | 		}
  407 | 		catch (Throwable ex) {
  408 | 			if (logger.isWarnEnabled()) {
  409 | 				logger.warn("Unable to instantiate custom exception class [" + exceptionClass.getName() + "]", ex);
  410 | 			}
  411 | 			return null;
  412 | 		}
  413 | 	}
  414 | 
  415 | 	private void logTranslation(String task, @Nullable String sql, SQLException sqlEx, boolean custom) {
  416 | 		if (logger.isDebugEnabled()) {
  417 | 			String intro = custom ? "Custom translation of" : "Translating";
  418 | 			logger.debug(intro + " SQLException with SQL state '" + sqlEx.getSQLState() +
  419 | 					"', error code '" + sqlEx.getErrorCode() + "', message [" + sqlEx.getMessage() + "]" +
  420 | 					(sql != null ? "; SQL was [" + sql + "]": "") + " for task [" + task + "]");
  421 | 		}
  422 | 	}
  423 | 
  424 | 
  425 | 	/**
  426 | 	 * Check whether there is a user-provided `sql-error-codes.xml` file
  427 | 	 * in the root of the classpath.
  428 | 	 */
  429 | 	static boolean hasUserProvidedErrorCodesFile() {
  430 | 		return userProvidedErrorCodesFilePresent;
  431 | 	}
  432 | 
  433 | }
```
