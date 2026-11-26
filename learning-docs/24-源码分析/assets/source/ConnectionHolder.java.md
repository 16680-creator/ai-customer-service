<!-- learning-docs source archive（机械抓取原文，禁止手改代码内容） -->
<!-- origin: org.springframework:spring-jdbc:7.1.0 | path: org/springframework/jdbc/datasource/ConnectionHolder.java -->
<!-- 正文引用：[[源码:ConnectionHolder.java#L起-L止]] ；代码块头部：(src: ConnectionHolder.java#L起-L止) -->

# ConnectionHolder.java

| 项 | 值 |
| --- | --- |
| 来源 | `org.springframework:spring-jdbc:7.1.0` |
| 路径 | `org/springframework/jdbc/datasource/ConnectionHolder.java` |
| 行数 | 214 |
| 在线地址 | https://repo1.maven.org/maven2/org/springframework/spring-jdbc/7.1.0/spring-jdbc-7.1.0-sources.jar |
| 抓取时间 | 2026-11-25T14:01:04.275Z |

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
   21 | import java.sql.Savepoint;
   22 | 
   23 | import org.jspecify.annotations.Nullable;
   24 | 
   25 | import org.springframework.transaction.support.ResourceHolderSupport;
   26 | import org.springframework.util.Assert;
   27 | 
   28 | /**
   29 |  * Resource holder wrapping a JDBC {@link Connection}.
   30 |  * {@link DataSourceTransactionManager} binds instances of this class
   31 |  * to the thread, for a specific {@link javax.sql.DataSource}.
   32 |  *
   33 |  * <p>Inherits rollback-only support for nested JDBC transactions
   34 |  * and reference count functionality from the base class.
   35 |  *
   36 |  * <p>Note: This is an SPI class, not intended to be used by applications.
   37 |  *
   38 |  * @author Juergen Hoeller
   39 |  * @since 06.05.2003
   40 |  * @see DataSourceTransactionManager
   41 |  * @see DataSourceUtils
   42 |  */
   43 | public class ConnectionHolder extends ResourceHolderSupport {
   44 | 
   45 | 	/**
   46 | 	 * Prefix for savepoint names.
   47 | 	 */
   48 | 	public static final String SAVEPOINT_NAME_PREFIX = "SAVEPOINT_";
   49 | 
   50 | 
   51 | 	private @Nullable ConnectionHandle connectionHandle;
   52 | 
   53 | 	private @Nullable Connection currentConnection;
   54 | 
   55 | 	private boolean transactionActive = false;
   56 | 
   57 | 	private @Nullable Boolean savepointsSupported;
   58 | 
   59 | 	private int savepointCounter = 0;
   60 | 
   61 | 
   62 | 	/**
   63 | 	 * Create a new ConnectionHolder for the given ConnectionHandle.
   64 | 	 * @param connectionHandle the ConnectionHandle to hold
   65 | 	 */
   66 | 	public ConnectionHolder(ConnectionHandle connectionHandle) {
   67 | 		Assert.notNull(connectionHandle, "ConnectionHandle must not be null");
   68 | 		this.connectionHandle = connectionHandle;
   69 | 	}
   70 | 
   71 | 	/**
   72 | 	 * Create a new ConnectionHolder for the given JDBC Connection,
   73 | 	 * wrapping it with a {@link SimpleConnectionHandle},
   74 | 	 * assuming that there is no ongoing transaction.
   75 | 	 * @param connection the JDBC Connection to hold
   76 | 	 * @see SimpleConnectionHandle
   77 | 	 * @see #ConnectionHolder(java.sql.Connection, boolean)
   78 | 	 */
   79 | 	public ConnectionHolder(Connection connection) {
   80 | 		this.connectionHandle = new SimpleConnectionHandle(connection);
   81 | 	}
   82 | 
   83 | 	/**
   84 | 	 * Create a new ConnectionHolder for the given JDBC Connection,
   85 | 	 * wrapping it with a {@link SimpleConnectionHandle}.
   86 | 	 * @param connection the JDBC Connection to hold
   87 | 	 * @param transactionActive whether the given Connection is involved
   88 | 	 * in an ongoing transaction
   89 | 	 * @see SimpleConnectionHandle
   90 | 	 */
   91 | 	public ConnectionHolder(Connection connection, boolean transactionActive) {
   92 | 		this(connection);
   93 | 		this.transactionActive = transactionActive;
   94 | 	}
   95 | 
   96 | 
   97 | 	/**
   98 | 	 * Return the ConnectionHandle held by this ConnectionHolder.
   99 | 	 */
  100 | 	public @Nullable ConnectionHandle getConnectionHandle() {
  101 | 		return this.connectionHandle;
  102 | 	}
  103 | 
  104 | 	/**
  105 | 	 * Return whether this holder currently has a Connection.
  106 | 	 */
  107 | 	protected boolean hasConnection() {
  108 | 		return (this.connectionHandle != null);
  109 | 	}
  110 | 
  111 | 	/**
  112 | 	 * Set whether this holder represents an active, JDBC-managed transaction.
  113 | 	 * @see DataSourceTransactionManager
  114 | 	 */
  115 | 	protected void setTransactionActive(boolean transactionActive) {
  116 | 		this.transactionActive = transactionActive;
  117 | 	}
  118 | 
  119 | 	/**
  120 | 	 * Return whether this holder represents an active, JDBC-managed transaction.
  121 | 	 */
  122 | 	protected boolean isTransactionActive() {
  123 | 		return this.transactionActive;
  124 | 	}
  125 | 
  126 | 
  127 | 	/**
  128 | 	 * Override the existing Connection handle with the given Connection.
  129 | 	 * Reset the handle if given {@code null}.
  130 | 	 * <p>Used for releasing the Connection on suspend (with a {@code null}
  131 | 	 * argument) and setting a fresh Connection on resume.
  132 | 	 */
  133 | 	protected void setConnection(@Nullable Connection connection) {
  134 | 		if (this.currentConnection != null) {
  135 | 			if (this.connectionHandle != null) {
  136 | 				this.connectionHandle.releaseConnection(this.currentConnection);
  137 | 			}
  138 | 			this.currentConnection = null;
  139 | 		}
  140 | 		if (connection != null) {
  141 | 			this.connectionHandle = new SimpleConnectionHandle(connection);
  142 | 		}
  143 | 		else {
  144 | 			this.connectionHandle = null;
  145 | 		}
  146 | 	}
  147 | 
  148 | 	/**
  149 | 	 * Return the current Connection held by this ConnectionHolder.
  150 | 	 * <p>This will be the same Connection until {@code released}
  151 | 	 * gets called on the ConnectionHolder, which will reset the
  152 | 	 * held Connection, fetching a new Connection on demand.
  153 | 	 * @see ConnectionHandle#getConnection()
  154 | 	 * @see #released()
  155 | 	 */
  156 | 	public Connection getConnection() {
  157 | 		Assert.state(this.connectionHandle != null, "Active Connection is required");
  158 | 		if (this.currentConnection == null) {
  159 | 			this.currentConnection = this.connectionHandle.getConnection();
  160 | 		}
  161 | 		return this.currentConnection;
  162 | 	}
  163 | 
  164 | 	/**
  165 | 	 * Return whether JDBC Savepoints are supported.
  166 | 	 * Caches the flag for the lifetime of this ConnectionHolder.
  167 | 	 * @throws SQLException if thrown by the JDBC driver
  168 | 	 */
  169 | 	public boolean supportsSavepoints() throws SQLException {
  170 | 		if (this.savepointsSupported == null) {
  171 | 			this.savepointsSupported = getConnection().getMetaData().supportsSavepoints();
  172 | 		}
  173 | 		return this.savepointsSupported;
  174 | 	}
  175 | 
  176 | 	/**
  177 | 	 * Create a new JDBC Savepoint for the current Connection,
  178 | 	 * using generated savepoint names that are unique for the Connection.
  179 | 	 * @return the new Savepoint
  180 | 	 * @throws SQLException if thrown by the JDBC driver
  181 | 	 */
  182 | 	public Savepoint createSavepoint() throws SQLException {
  183 | 		this.savepointCounter++;
  184 | 		return getConnection().setSavepoint(SAVEPOINT_NAME_PREFIX + this.savepointCounter);
  185 | 	}
  186 | 
  187 | 	/**
  188 | 	 * Releases the current Connection held by this ConnectionHolder.
  189 | 	 * <p>This is necessary for ConnectionHandles that expect "Connection borrowing",
  190 | 	 * where each returned Connection is only temporarily leased and needs to be
  191 | 	 * returned once the data operation is done, to make the Connection available
  192 | 	 * for other operations within the same transaction.
  193 | 	 */
  194 | 	@Override
  195 | 	public void released() {
  196 | 		super.released();
  197 | 		if (!isOpen() && this.currentConnection != null) {
  198 | 			if (this.connectionHandle != null) {
  199 | 				this.connectionHandle.releaseConnection(this.currentConnection);
  200 | 			}
  201 | 			this.currentConnection = null;
  202 | 		}
  203 | 	}
  204 | 
  205 | 
  206 | 	@Override
  207 | 	public void clear() {
  208 | 		super.clear();
  209 | 		this.transactionActive = false;
  210 | 		this.savepointsSupported = null;
  211 | 		this.savepointCounter = 0;
  212 | 	}
  213 | 
  214 | }
```
