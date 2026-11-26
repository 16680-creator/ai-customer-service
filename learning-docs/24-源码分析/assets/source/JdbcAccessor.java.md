<!-- learning-docs source archive（机械抓取原文，禁止手改代码内容） -->
<!-- origin: org.springframework:spring-jdbc:7.1.0 | path: org/springframework/jdbc/support/JdbcAccessor.java -->
<!-- 正文引用：[[源码:JdbcAccessor.java#L起-L止]] ；代码块头部：(src: JdbcAccessor.java#L起-L止) -->

# JdbcAccessor.java

| 项 | 值 |
| --- | --- |
| 来源 | `org.springframework:spring-jdbc:7.1.0` |
| 路径 | `org/springframework/jdbc/support/JdbcAccessor.java` |
| 行数 | 168 |
| 在线地址 | https://repo1.maven.org/maven2/org/springframework/spring-jdbc/7.1.0/spring-jdbc-7.1.0-sources.jar |
| 抓取时间 | 2026-11-25T14:00:10.337Z |

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
   19 | import javax.sql.DataSource;
   20 | 
   21 | import org.apache.commons.logging.Log;
   22 | import org.apache.commons.logging.LogFactory;
   23 | import org.jspecify.annotations.Nullable;
   24 | 
   25 | import org.springframework.beans.factory.InitializingBean;
   26 | import org.springframework.util.Assert;
   27 | 
   28 | /**
   29 |  * Base class for {@link org.springframework.jdbc.core.JdbcTemplate} and
   30 |  * other JDBC-accessing DAO helpers, defining common properties such as
   31 |  * DataSource and exception translator.
   32 |  *
   33 |  * <p>Not intended to be used directly.
   34 |  * See {@link org.springframework.jdbc.core.JdbcTemplate}.
   35 |  *
   36 |  * @author Juergen Hoeller
   37 |  * @author Sebastien Deleuze
   38 |  * @since 28.11.2003
   39 |  * @see org.springframework.jdbc.core.JdbcTemplate
   40 |  */
   41 | public abstract class JdbcAccessor implements InitializingBean {
   42 | 
   43 | 	/** Logger available to subclasses. */
   44 | 	protected final Log logger = LogFactory.getLog(getClass());
   45 | 
   46 | 	private @Nullable DataSource dataSource;
   47 | 
   48 | 	private volatile @Nullable SQLExceptionTranslator exceptionTranslator;
   49 | 
   50 | 	private boolean lazyInit = true;
   51 | 
   52 | 
   53 | 	/**
   54 | 	 * Set the JDBC DataSource to obtain connections from.
   55 | 	 */
   56 | 	public void setDataSource(@Nullable DataSource dataSource) {
   57 | 		this.dataSource = dataSource;
   58 | 	}
   59 | 
   60 | 	/**
   61 | 	 * Return the DataSource used by this template.
   62 | 	 */
   63 | 	public @Nullable DataSource getDataSource() {
   64 | 		return this.dataSource;
   65 | 	}
   66 | 
   67 | 	/**
   68 | 	 * Obtain the DataSource for actual use.
   69 | 	 * @return the DataSource (never {@code null})
   70 | 	 * @throws IllegalStateException in case of no DataSource set
   71 | 	 * @since 5.0
   72 | 	 */
   73 | 	protected DataSource obtainDataSource() {
   74 | 		DataSource dataSource = getDataSource();
   75 | 		Assert.state(dataSource != null, "No DataSource set");
   76 | 		return dataSource;
   77 | 	}
   78 | 
   79 | 	/**
   80 | 	 * Specify the database product name for the {@code DataSource} that this accessor uses.
   81 | 	 * This allows for initializing a {@link SQLErrorCodeSQLExceptionTranslator} without
   82 | 	 * obtaining a {@code Connection} from the {@code DataSource} to get the meta-data.
   83 | 	 * @param dbName the database product name that identifies the error codes entry
   84 | 	 * @see #setExceptionTranslator
   85 | 	 * @see SQLErrorCodeSQLExceptionTranslator#setDatabaseProductName
   86 | 	 * @see java.sql.DatabaseMetaData#getDatabaseProductName()
   87 | 	 */
   88 | 	public void setDatabaseProductName(String dbName) {
   89 | 		if (SQLErrorCodeSQLExceptionTranslator.hasUserProvidedErrorCodesFile()) {
   90 | 			this.exceptionTranslator = new SQLErrorCodeSQLExceptionTranslator(dbName);
   91 | 		}
   92 | 		else {
   93 | 			this.exceptionTranslator = new SQLExceptionSubclassTranslator();
   94 | 		}
   95 | 	}
   96 | 
   97 | 	/**
   98 | 	 * Set the exception translator for this instance.
   99 | 	 * <p>A {@link SQLErrorCodeSQLExceptionTranslator} used by default if a user-provided
  100 | 	 * `sql-error-codes.xml` file has been found in the root of the classpath. Otherwise,
  101 | 	 * {@link SQLExceptionSubclassTranslator} serves as the default translator as of 6.0.
  102 | 	 * @see org.springframework.jdbc.support.SQLErrorCodeSQLExceptionTranslator
  103 | 	 * @see org.springframework.jdbc.support.SQLExceptionSubclassTranslator
  104 | 	 */
  105 | 	public void setExceptionTranslator(SQLExceptionTranslator exceptionTranslator) {
  106 | 		this.exceptionTranslator = exceptionTranslator;
  107 | 	}
  108 | 
  109 | 	/**
  110 | 	 * Return the exception translator to use for this instance,
  111 | 	 * creating a default if necessary.
  112 | 	 * @see #setExceptionTranslator
  113 | 	 */
  114 | 	public SQLExceptionTranslator getExceptionTranslator() {
  115 | 		SQLExceptionTranslator exceptionTranslator = this.exceptionTranslator;
  116 | 		if (exceptionTranslator != null) {
  117 | 			return exceptionTranslator;
  118 | 		}
  119 | 		synchronized (this) {
  120 | 			exceptionTranslator = this.exceptionTranslator;
  121 | 			if (exceptionTranslator == null) {
  122 | 				if (SQLErrorCodeSQLExceptionTranslator.hasUserProvidedErrorCodesFile()) {
  123 | 					exceptionTranslator = new SQLErrorCodeSQLExceptionTranslator(obtainDataSource());
  124 | 				}
  125 | 				else {
  126 | 					exceptionTranslator = new SQLExceptionSubclassTranslator();
  127 | 				}
  128 | 				this.exceptionTranslator = exceptionTranslator;
  129 | 			}
  130 | 			return exceptionTranslator;
  131 | 		}
  132 | 	}
  133 | 
  134 | 	/**
  135 | 	 * Set whether to lazily initialize the SQLExceptionTranslator for this accessor,
  136 | 	 * on first encounter of an SQLException. Default is "true"; can be switched to
  137 | 	 * "false" for initialization on startup.
  138 | 	 * <p>Early initialization just applies if {@code afterPropertiesSet()} is called.
  139 | 	 * @see #getExceptionTranslator()
  140 | 	 * @see #afterPropertiesSet()
  141 | 	 */
  142 | 	public void setLazyInit(boolean lazyInit) {
  143 | 		this.lazyInit = lazyInit;
  144 | 	}
  145 | 
  146 | 	/**
  147 | 	 * Return whether to lazily initialize the SQLExceptionTranslator for this accessor.
  148 | 	 * @see #getExceptionTranslator()
  149 | 	 */
  150 | 	public boolean isLazyInit() {
  151 | 		return this.lazyInit;
  152 | 	}
  153 | 
  154 | 	/**
  155 | 	 * Eagerly initialize the exception translator, if demanded,
  156 | 	 * creating a default one for the specified DataSource if none set.
  157 | 	 */
  158 | 	@Override
  159 | 	public void afterPropertiesSet() {
  160 | 		if (getDataSource() == null) {
  161 | 			throw new IllegalArgumentException("Property 'dataSource' is required");
  162 | 		}
  163 | 		if (!isLazyInit()) {
  164 | 			getExceptionTranslator();
  165 | 		}
  166 | 	}
  167 | 
  168 | }
```
