<!-- learning-docs source archive（机械抓取原文，禁止手改代码内容） -->
<!-- origin: org.mybatis:mybatis-spring:3.0.5 | path: org/mybatis/spring/SqlSessionUtils.java -->
<!-- 正文引用：[[源码:SqlSessionUtils.java#L起-L止]] ；代码块头部：(src: SqlSessionUtils.java#L起-L止) -->

# SqlSessionUtils.java

| 项 | 值 |
| --- | --- |
| 来源 | `org.mybatis:mybatis-spring:3.0.5` |
| 路径 | `org/mybatis/spring/SqlSessionUtils.java` |
| 行数 | 316 |
| 在线地址 | https://repo1.maven.org/maven2/org/mybatis/mybatis-spring/3.0.5/mybatis-spring-3.0.5-sources.jar |
| 抓取时间 | 2026-11-25T15:01:24.549Z |

```java
    1 | /*
    2 |  * Copyright 2010-2024 the original author or authors.
    3 |  *
    4 |  * Licensed under the Apache License, Version 2.0 (the "License");
    5 |  * you may not use this file except in compliance with the License.
    6 |  * You may obtain a copy of the License at
    7 |  *
    8 |  *    https://www.apache.org/licenses/LICENSE-2.0
    9 |  *
   10 |  * Unless required by applicable law or agreed to in writing, software
   11 |  * distributed under the License is distributed on an "AS IS" BASIS,
   12 |  * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
   13 |  * See the License for the specific language governing permissions and
   14 |  * limitations under the License.
   15 |  */
   16 | package org.mybatis.spring;
   17 | 
   18 | import static org.springframework.util.Assert.notNull;
   19 | 
   20 | import org.apache.ibatis.exceptions.PersistenceException;
   21 | import org.apache.ibatis.session.ExecutorType;
   22 | import org.apache.ibatis.session.SqlSession;
   23 | import org.apache.ibatis.session.SqlSessionFactory;
   24 | import org.mybatis.logging.Logger;
   25 | import org.mybatis.logging.LoggerFactory;
   26 | import org.mybatis.spring.transaction.SpringManagedTransactionFactory;
   27 | import org.springframework.dao.TransientDataAccessResourceException;
   28 | import org.springframework.dao.support.PersistenceExceptionTranslator;
   29 | import org.springframework.jdbc.datasource.DataSourceUtils;
   30 | import org.springframework.transaction.support.TransactionSynchronization;
   31 | import org.springframework.transaction.support.TransactionSynchronizationManager;
   32 | 
   33 | /**
   34 |  * Handles MyBatis SqlSession life cycle. It can register and get SqlSessions from Spring
   35 |  * {@code TransactionSynchronizationManager}. Also works if no transaction is active.
   36 |  *
   37 |  * @author Hunter Presnall
   38 |  * @author Eduardo Macarron
   39 |  */
   40 | public final class SqlSessionUtils {
   41 | 
   42 |   private static final Logger LOGGER = LoggerFactory.getLogger(SqlSessionUtils.class);
   43 | 
   44 |   private static final String NO_EXECUTOR_TYPE_SPECIFIED = "No ExecutorType specified";
   45 |   private static final String NO_SQL_SESSION_FACTORY_SPECIFIED = "No SqlSessionFactory specified";
   46 |   private static final String NO_SQL_SESSION_SPECIFIED = "No SqlSession specified";
   47 | 
   48 |   /**
   49 |    * This class can't be instantiated, exposes static utility methods only.
   50 |    */
   51 |   private SqlSessionUtils() {
   52 |     // do nothing
   53 |   }
   54 | 
   55 |   /**
   56 |    * Creates a new MyBatis {@code SqlSession} from the {@code SqlSessionFactory} provided as a parameter and using its
   57 |    * {@code DataSource} and {@code ExecutorType}
   58 |    *
   59 |    * @param sessionFactory
   60 |    *          a MyBatis {@code SqlSessionFactory} to create new sessions
   61 |    *
   62 |    * @return a MyBatis {@code SqlSession}
   63 |    *
   64 |    * @throws TransientDataAccessResourceException
   65 |    *           if a transaction is active and the {@code SqlSessionFactory} is not using a
   66 |    *           {@code SpringManagedTransactionFactory}
   67 |    */
   68 |   public static SqlSession getSqlSession(SqlSessionFactory sessionFactory) {
   69 |     var executorType = sessionFactory.getConfiguration().getDefaultExecutorType();
   70 |     return getSqlSession(sessionFactory, executorType, null);
   71 |   }
   72 | 
   73 |   /**
   74 |    * Gets an SqlSession from Spring Transaction Manager or creates a new one if needed. Tries to get a SqlSession out of
   75 |    * current transaction. If there is not any, it creates a new one. Then, it synchronizes the SqlSession with the
   76 |    * transaction if Spring TX is active and <code>SpringManagedTransactionFactory</code> is configured as a transaction
   77 |    * manager.
   78 |    *
   79 |    * @param sessionFactory
   80 |    *          a MyBatis {@code SqlSessionFactory} to create new sessions
   81 |    * @param executorType
   82 |    *          The executor type of the SqlSession to create
   83 |    * @param exceptionTranslator
   84 |    *          Optional. Translates SqlSession.commit() exceptions to Spring exceptions.
   85 |    *
   86 |    * @return an SqlSession managed by Spring Transaction Manager
   87 |    *
   88 |    * @throws TransientDataAccessResourceException
   89 |    *           if a transaction is active and the {@code SqlSessionFactory} is not using a
   90 |    *           {@code SpringManagedTransactionFactory}
   91 |    *
   92 |    * @see SpringManagedTransactionFactory
   93 |    */
   94 |   public static SqlSession getSqlSession(SqlSessionFactory sessionFactory, ExecutorType executorType,
   95 |       PersistenceExceptionTranslator exceptionTranslator) {
   96 | 
   97 |     notNull(sessionFactory, NO_SQL_SESSION_FACTORY_SPECIFIED);
   98 |     notNull(executorType, NO_EXECUTOR_TYPE_SPECIFIED);
   99 | 
  100 |     var holder = (SqlSessionHolder) TransactionSynchronizationManager.getResource(sessionFactory);
  101 | 
  102 |     var session = sessionHolder(executorType, holder);
  103 |     if (session != null) {
  104 |       return session;
  105 |     }
  106 | 
  107 |     LOGGER.debug(() -> "Creating a new SqlSession");
  108 |     session = sessionFactory.openSession(executorType);
  109 | 
  110 |     registerSessionHolder(sessionFactory, executorType, exceptionTranslator, session);
  111 | 
  112 |     return session;
  113 |   }
  114 | 
  115 |   /**
  116 |    * Register session holder if synchronization is active (i.e. a Spring TX is active).
  117 |    * <p>
  118 |    * Note: The DataSource used by the Environment should be synchronized with the transaction either through
  119 |    * DataSourceTxMgr or another tx synchronization. Further assume that if an exception is thrown, whatever started the
  120 |    * transaction will handle closing / rolling back the Connection associated with the SqlSession.
  121 |    *
  122 |    * @param sessionFactory
  123 |    *          sqlSessionFactory used for registration.
  124 |    * @param executorType
  125 |    *          executorType used for registration.
  126 |    * @param exceptionTranslator
  127 |    *          persistenceExceptionTranslator used for registration.
  128 |    * @param session
  129 |    *          sqlSession used for registration.
  130 |    */
  131 |   private static void registerSessionHolder(SqlSessionFactory sessionFactory, ExecutorType executorType,
  132 |       PersistenceExceptionTranslator exceptionTranslator, SqlSession session) {
  133 |     SqlSessionHolder holder;
  134 |     if (TransactionSynchronizationManager.isSynchronizationActive()) {
  135 |       var environment = sessionFactory.getConfiguration().getEnvironment();
  136 | 
  137 |       if (environment.getTransactionFactory() instanceof SpringManagedTransactionFactory) {
  138 |         LOGGER.debug(() -> "Registering transaction synchronization for SqlSession [" + session + "]");
  139 | 
  140 |         holder = new SqlSessionHolder(session, executorType, exceptionTranslator);
  141 |         TransactionSynchronizationManager.bindResource(sessionFactory, holder);
  142 |         TransactionSynchronizationManager
  143 |             .registerSynchronization(new SqlSessionSynchronization(holder, sessionFactory));
  144 |         holder.setSynchronizedWithTransaction(true);
  145 |         holder.requested();
  146 |       } else if (TransactionSynchronizationManager.getResource(environment.getDataSource()) == null) {
  147 |         LOGGER.debug(() -> "SqlSession [" + session
  148 |             + "] was not registered for synchronization because DataSource is not transactional");
  149 |       } else {
  150 |         throw new TransientDataAccessResourceException(
  151 |             "SqlSessionFactory must be using a SpringManagedTransactionFactory in order to use Spring transaction synchronization");
  152 |       }
  153 |     } else {
  154 |       LOGGER.debug(() -> "SqlSession [" + session
  155 |           + "] was not registered for synchronization because synchronization is not active");
  156 |     }
  157 | 
  158 |   }
  159 | 
  160 |   private static SqlSession sessionHolder(ExecutorType executorType, SqlSessionHolder holder) {
  161 |     SqlSession session = null;
  162 |     if (holder != null && holder.isSynchronizedWithTransaction()) {
  163 |       if (holder.getExecutorType() != executorType) {
  164 |         throw new TransientDataAccessResourceException(
  165 |             "Cannot change the ExecutorType when there is an existing transaction");
  166 |       }
  167 | 
  168 |       holder.requested();
  169 | 
  170 |       LOGGER.debug(() -> "Fetched SqlSession [" + holder.getSqlSession() + "] from current transaction");
  171 |       session = holder.getSqlSession();
  172 |     }
  173 |     return session;
  174 |   }
  175 | 
  176 |   /**
  177 |    * Checks if {@code SqlSession} passed as an argument is managed by Spring {@code TransactionSynchronizationManager}
  178 |    * If it is not, it closes it, otherwise it just updates the reference counter and lets Spring call the close callback
  179 |    * when the managed transaction ends
  180 |    *
  181 |    * @param session
  182 |    *          a target SqlSession
  183 |    * @param sessionFactory
  184 |    *          a factory of SqlSession
  185 |    */
  186 |   public static void closeSqlSession(SqlSession session, SqlSessionFactory sessionFactory) {
  187 |     notNull(session, NO_SQL_SESSION_SPECIFIED);
  188 |     notNull(sessionFactory, NO_SQL_SESSION_FACTORY_SPECIFIED);
  189 | 
  190 |     var holder = (SqlSessionHolder) TransactionSynchronizationManager.getResource(sessionFactory);
  191 |     if (holder != null && holder.getSqlSession() == session) {
  192 |       LOGGER.debug(() -> "Releasing transactional SqlSession [" + session + "]");
  193 |       holder.released();
  194 |     } else {
  195 |       LOGGER.debug(() -> "Closing non transactional SqlSession [" + session + "]");
  196 |       session.close();
  197 |     }
  198 |   }
  199 | 
  200 |   /**
  201 |    * Returns if the {@code SqlSession} passed as an argument is being managed by Spring
  202 |    *
  203 |    * @param session
  204 |    *          a MyBatis SqlSession to check
  205 |    * @param sessionFactory
  206 |    *          the SqlSessionFactory which the SqlSession was built with
  207 |    *
  208 |    * @return true if session is transactional, otherwise false
  209 |    */
  210 |   public static boolean isSqlSessionTransactional(SqlSession session, SqlSessionFactory sessionFactory) {
  211 |     notNull(session, NO_SQL_SESSION_SPECIFIED);
  212 |     notNull(sessionFactory, NO_SQL_SESSION_FACTORY_SPECIFIED);
  213 | 
  214 |     var holder = (SqlSessionHolder) TransactionSynchronizationManager.getResource(sessionFactory);
  215 | 
  216 |     return holder != null && holder.getSqlSession() == session;
  217 |   }
  218 | 
  219 |   /**
  220 |    * Callback for cleaning up resources. It cleans TransactionSynchronizationManager and also commits and closes the
  221 |    * {@code SqlSession}. It assumes that {@code Connection} life cycle will be managed by
  222 |    * {@code DataSourceTransactionManager} or {@code JtaTransactionManager}
  223 |    */
  224 |   private static final class SqlSessionSynchronization implements TransactionSynchronization {
  225 | 
  226 |     private final SqlSessionHolder holder;
  227 | 
  228 |     private final SqlSessionFactory sessionFactory;
  229 | 
  230 |     private boolean holderActive = true;
  231 | 
  232 |     public SqlSessionSynchronization(SqlSessionHolder holder, SqlSessionFactory sessionFactory) {
  233 |       notNull(holder, "Parameter 'holder' must be not null");
  234 |       notNull(sessionFactory, "Parameter 'sessionFactory' must be not null");
  235 | 
  236 |       this.holder = holder;
  237 |       this.sessionFactory = sessionFactory;
  238 |     }
  239 | 
  240 |     @Override
  241 |     public int getOrder() {
  242 |       // order right before any Connection synchronization
  243 |       return DataSourceUtils.CONNECTION_SYNCHRONIZATION_ORDER - 1;
  244 |     }
  245 | 
  246 |     @Override
  247 |     public void suspend() {
  248 |       if (this.holderActive) {
  249 |         LOGGER.debug(() -> "Transaction synchronization suspending SqlSession [" + this.holder.getSqlSession() + "]");
  250 |         TransactionSynchronizationManager.unbindResource(this.sessionFactory);
  251 |       }
  252 |     }
  253 | 
  254 |     @Override
  255 |     public void resume() {
  256 |       if (this.holderActive) {
  257 |         LOGGER.debug(() -> "Transaction synchronization resuming SqlSession [" + this.holder.getSqlSession() + "]");
  258 |         TransactionSynchronizationManager.bindResource(this.sessionFactory, this.holder);
  259 |       }
  260 |     }
  261 | 
  262 |     @Override
  263 |     public void beforeCommit(boolean readOnly) {
  264 |       // Connection commit or rollback will be handled by ConnectionSynchronization or
  265 |       // DataSourceTransactionManager.
  266 |       // But, do cleanup the SqlSession / Executor, including flushing BATCH statements so
  267 |       // they are actually executed.
  268 |       // SpringManagedTransaction will no-op the commit over the jdbc connection
  269 |       // TODO This updates 2nd level caches but the tx may be rolledback later on!
  270 |       if (TransactionSynchronizationManager.isActualTransactionActive()) {
  271 |         try {
  272 |           LOGGER.debug(() -> "Transaction synchronization committing SqlSession [" + this.holder.getSqlSession() + "]");
  273 |           this.holder.getSqlSession().commit();
  274 |         } catch (PersistenceException p) {
  275 |           if (this.holder.getPersistenceExceptionTranslator() != null) {
  276 |             var translated = this.holder.getPersistenceExceptionTranslator().translateExceptionIfPossible(p);
  277 |             if (translated != null) {
  278 |               throw translated;
  279 |             }
  280 |           }
  281 |           throw p;
  282 |         }
  283 |       }
  284 |     }
  285 | 
  286 |     @Override
  287 |     public void beforeCompletion() {
  288 |       // Issue #18 Close SqlSession and deregister it now
  289 |       // because afterCompletion may be called from a different thread
  290 |       if (!this.holder.isOpen()) {
  291 |         LOGGER
  292 |             .debug(() -> "Transaction synchronization deregistering SqlSession [" + this.holder.getSqlSession() + "]");
  293 |         TransactionSynchronizationManager.unbindResource(sessionFactory);
  294 |         this.holderActive = false;
  295 |         LOGGER.debug(() -> "Transaction synchronization closing SqlSession [" + this.holder.getSqlSession() + "]");
  296 |         this.holder.getSqlSession().close();
  297 |       }
  298 |     }
  299 | 
  300 |     @Override
  301 |     public void afterCompletion(int status) {
  302 |       if (this.holderActive) {
  303 |         // afterCompletion may have been called from a different thread
  304 |         // so avoid failing if there is nothing in this one
  305 |         LOGGER
  306 |             .debug(() -> "Transaction synchronization deregistering SqlSession [" + this.holder.getSqlSession() + "]");
  307 |         TransactionSynchronizationManager.unbindResourceIfPossible(sessionFactory);
  308 |         this.holderActive = false;
  309 |         LOGGER.debug(() -> "Transaction synchronization closing SqlSession [" + this.holder.getSqlSession() + "]");
  310 |         this.holder.getSqlSession().close();
  311 |       }
  312 |       this.holder.reset();
  313 |     }
  314 |   }
  315 | 
  316 | }
```
