<!-- learning-docs source archive（机械抓取原文，禁止手改代码内容） -->
<!-- origin: org.springframework:spring-jdbc:7.1.0 | path: org/springframework/jdbc/support/JdbcTransactionManager.java -->
<!-- 正文引用：[[源码:JdbcTransactionManager.java#L起-L止]] ；代码块头部：(src: JdbcTransactionManager.java#L起-L止) -->

# JdbcTransactionManager.java

| 项 | 值 |
| --- | --- |
| 来源 | `org.springframework:spring-jdbc:7.1.0` |
| 路径 | `org/springframework/jdbc/support/JdbcTransactionManager.java` |
| 行数 | 186 |
| 在线地址 | https://repo1.maven.org/maven2/org/springframework/spring-jdbc/7.1.0/spring-jdbc-7.1.0-sources.jar |
| 抓取时间 | 2026-11-25T14:01:04.579Z |

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
   19 | import java.sql.SQLException;
   20 | 
   21 | import javax.sql.DataSource;
   22 | 
   23 | import org.jspecify.annotations.Nullable;
   24 | 
   25 | import org.springframework.dao.DataAccessException;
   26 | import org.springframework.jdbc.datasource.DataSourceTransactionManager;
   27 | 
   28 | /**
   29 |  * {@link JdbcAccessor}-aligned subclass of the plain {@link DataSourceTransactionManager},
   30 |  * adding common JDBC exception translation for the commit and rollback step.
   31 |  * Typically used in combination with {@link org.springframework.jdbc.core.JdbcTemplate}
   32 |  * which applies the same {@link SQLExceptionTranslator} infrastructure by default.
   33 |  *
   34 |  * <p>Exception translation is specifically relevant for commit steps in serializable
   35 |  * transactions (for example, on Postgres) where concurrency failures may occur late on commit.
   36 |  * This allows for throwing {@link org.springframework.dao.ConcurrencyFailureException} to
   37 |  * callers instead of {@link org.springframework.transaction.TransactionSystemException}.
   38 |  *
   39 |  * <p>Analogous to {@code HibernateTransactionManager} and {@code JpaTransactionManager},
   40 |  * this transaction manager may throw {@link DataAccessException} from {@link #commit}
   41 |  * and possibly also from {@link #rollback}. Calling code should be prepared for handling
   42 |  * such exceptions next to {@link org.springframework.transaction.TransactionException},
   43 |  * which is generally sensible since {@code TransactionSynchronization} implementations
   44 |  * may also throw such exceptions in their {@code flush} and {@code beforeCommit} phases.
   45 |  *
   46 |  * @author Juergen Hoeller
   47 |  * @author Sebastien Deleuze
   48 |  * @since 5.3
   49 |  * @see DataSourceTransactionManager
   50 |  * @see #setDataSource
   51 |  * @see #setExceptionTranslator
   52 |  */
   53 | @SuppressWarnings("serial")
   54 | public class JdbcTransactionManager extends DataSourceTransactionManager {
   55 | 
   56 | 	private volatile @Nullable SQLExceptionTranslator exceptionTranslator;
   57 | 
   58 | 	private boolean lazyInit = true;
   59 | 
   60 | 
   61 | 	/**
   62 | 	 * Create a new {@code JdbcTransactionManager} instance.
   63 | 	 * A {@code DataSource} has to be set to be able to use it.
   64 | 	 * @see #setDataSource
   65 | 	 */
   66 | 	public JdbcTransactionManager() {
   67 | 	}
   68 | 
   69 | 	/**
   70 | 	 * Create a new {@code JdbcTransactionManager} instance.
   71 | 	 * @param dataSource the JDBC DataSource to manage transactions for
   72 | 	 */
   73 | 	public JdbcTransactionManager(DataSource dataSource) {
   74 | 		this();
   75 | 		setDataSource(dataSource);
   76 | 		afterPropertiesSet();
   77 | 	}
   78 | 
   79 | 
   80 | 	/**
   81 | 	 * Specify the database product name for the {@code DataSource} that this
   82 | 	 * transaction manager operates on.
   83 | 	 * This allows for initializing a {@link SQLErrorCodeSQLExceptionTranslator} without
   84 | 	 * obtaining a {@code Connection} from the {@code DataSource} to get the meta-data.
   85 | 	 * @param dbName the database product name that identifies the error codes entry
   86 | 	 * @see #setExceptionTranslator
   87 | 	 * @see SQLErrorCodeSQLExceptionTranslator#setDatabaseProductName
   88 | 	 * @see java.sql.DatabaseMetaData#getDatabaseProductName()
   89 | 	 * @see JdbcAccessor#setDatabaseProductName
   90 | 	 */
   91 | 	public void setDatabaseProductName(String dbName) {
   92 | 		if (SQLErrorCodeSQLExceptionTranslator.hasUserProvidedErrorCodesFile()) {
   93 | 			this.exceptionTranslator = new SQLErrorCodeSQLExceptionTranslator(dbName);
   94 | 		}
   95 | 		else {
   96 | 			this.exceptionTranslator = new SQLExceptionSubclassTranslator();
   97 | 		}
   98 | 	}
   99 | 
  100 | 	/**
  101 | 	 * Set the exception translator for this transaction manager.
  102 | 	 * <p>A {@link SQLErrorCodeSQLExceptionTranslator} used by default if a user-provided
  103 | 	 * `sql-error-codes.xml` file has been found in the root of the classpath. Otherwise,
  104 | 	 * {@link SQLExceptionSubclassTranslator} serves as the default translator as of 6.0.
  105 | 	 * @see org.springframework.jdbc.support.SQLErrorCodeSQLExceptionTranslator
  106 | 	 * @see org.springframework.jdbc.support.SQLExceptionSubclassTranslator
  107 | 	 * @see JdbcAccessor#setExceptionTranslator
  108 | 	 */
  109 | 	public void setExceptionTranslator(SQLExceptionTranslator exceptionTranslator) {
  110 | 		this.exceptionTranslator = exceptionTranslator;
  111 | 	}
  112 | 
  113 | 	/**
  114 | 	 * Return the exception translator to use for this instance,
  115 | 	 * creating a default if necessary.
  116 | 	 * @see #setExceptionTranslator
  117 | 	 */
  118 | 	public SQLExceptionTranslator getExceptionTranslator() {
  119 | 		SQLExceptionTranslator exceptionTranslator = this.exceptionTranslator;
  120 | 		if (exceptionTranslator != null) {
  121 | 			return exceptionTranslator;
  122 | 		}
  123 | 		synchronized (this) {
  124 | 			exceptionTranslator = this.exceptionTranslator;
  125 | 			if (exceptionTranslator == null) {
  126 | 				if (SQLErrorCodeSQLExceptionTranslator.hasUserProvidedErrorCodesFile()) {
  127 | 					exceptionTranslator = new SQLErrorCodeSQLExceptionTranslator(obtainDataSource());
  128 | 				}
  129 | 				else {
  130 | 					exceptionTranslator = new SQLExceptionSubclassTranslator();
  131 | 				}
  132 | 				this.exceptionTranslator = exceptionTranslator;
  133 | 			}
  134 | 			return exceptionTranslator;
  135 | 		}
  136 | 	}
  137 | 
  138 | 	/**
  139 | 	 * Set whether to lazily initialize the SQLExceptionTranslator for this transaction manager,
  140 | 	 * on first encounter of an SQLException. Default is "true"; can be switched to
  141 | 	 * "false" for initialization on startup.
  142 | 	 * <p>Early initialization just applies if {@code afterPropertiesSet()} is called.
  143 | 	 * @see #getExceptionTranslator()
  144 | 	 * @see #afterPropertiesSet()
  145 | 	 */
  146 | 	public void setLazyInit(boolean lazyInit) {
  147 | 		this.lazyInit = lazyInit;
  148 | 	}
  149 | 
  150 | 	/**
  151 | 	 * Return whether to lazily initialize the SQLExceptionTranslator for this transaction manager.
  152 | 	 * @see #getExceptionTranslator()
  153 | 	 */
  154 | 	public boolean isLazyInit() {
  155 | 		return this.lazyInit;
  156 | 	}
  157 | 
  158 | 	/**
  159 | 	 * Eagerly initialize the exception translator, if demanded,
  160 | 	 * creating a default one for the specified DataSource if none set.
  161 | 	 */
  162 | 	@Override
  163 | 	public void afterPropertiesSet() {
  164 | 		super.afterPropertiesSet();
  165 | 		if (!isLazyInit()) {
  166 | 			getExceptionTranslator();
  167 | 		}
  168 | 	}
  169 | 
  170 | 
  171 | 	/**
  172 | 	 * This implementation attempts to use the {@link SQLExceptionTranslator},
  173 | 	 * falling back to a {@link org.springframework.transaction.TransactionSystemException}.
  174 | 	 * @see #getExceptionTranslator()
  175 | 	 * @see DataSourceTransactionManager#translateException
  176 | 	 */
  177 | 	@Override
  178 | 	protected RuntimeException translateException(String task, SQLException ex) {
  179 | 		DataAccessException dae = getExceptionTranslator().translate(task, null, ex);
  180 | 		if (dae != null) {
  181 | 			return dae;
  182 | 		}
  183 | 		return super.translateException(task, ex);
  184 | 	}
  185 | 
  186 | }
```
