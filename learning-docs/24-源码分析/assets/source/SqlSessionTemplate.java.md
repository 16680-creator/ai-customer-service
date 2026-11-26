<!-- learning-docs source archive（机械抓取原文，禁止手改代码内容） -->
<!-- origin: org.mybatis:mybatis-spring:3.0.5 | path: org/mybatis/spring/SqlSessionTemplate.java -->
<!-- 正文引用：[[源码:SqlSessionTemplate.java#L起-L止]] ；代码块头部：(src: SqlSessionTemplate.java#L起-L止) -->

# SqlSessionTemplate.java

| 项 | 值 |
| --- | --- |
| 来源 | `org.mybatis:mybatis-spring:3.0.5` |
| 路径 | `org/mybatis/spring/SqlSessionTemplate.java` |
| 行数 | 361 |
| 在线地址 | https://repo1.maven.org/maven2/org/mybatis/mybatis-spring/3.0.5/mybatis-spring-3.0.5-sources.jar |
| 抓取时间 | 2026-11-25T15:01:24.244Z |

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
   18 | import static java.lang.reflect.Proxy.newProxyInstance;
   19 | 
   20 | import static org.apache.ibatis.reflection.ExceptionUtil.unwrapThrowable;
   21 | import static org.mybatis.spring.SqlSessionUtils.closeSqlSession;
   22 | import static org.mybatis.spring.SqlSessionUtils.getSqlSession;
   23 | import static org.mybatis.spring.SqlSessionUtils.isSqlSessionTransactional;
   24 | import static org.springframework.util.Assert.notNull;
   25 | 
   26 | import java.lang.reflect.InvocationHandler;
   27 | import java.lang.reflect.Method;
   28 | import java.sql.Connection;
   29 | import java.util.List;
   30 | import java.util.Map;
   31 | 
   32 | import org.apache.ibatis.cursor.Cursor;
   33 | import org.apache.ibatis.exceptions.PersistenceException;
   34 | import org.apache.ibatis.executor.BatchResult;
   35 | import org.apache.ibatis.session.Configuration;
   36 | import org.apache.ibatis.session.ExecutorType;
   37 | import org.apache.ibatis.session.ResultHandler;
   38 | import org.apache.ibatis.session.RowBounds;
   39 | import org.apache.ibatis.session.SqlSession;
   40 | import org.apache.ibatis.session.SqlSessionFactory;
   41 | import org.springframework.beans.factory.DisposableBean;
   42 | import org.springframework.dao.support.PersistenceExceptionTranslator;
   43 | 
   44 | /**
   45 |  * Thread safe, Spring managed, {@code SqlSession} that works with Spring transaction management to ensure that the
   46 |  * actual SqlSession used is the one associated with the current Spring transaction. In addition, it manages the session
   47 |  * life-cycle, including closing, committing or rolling back the session as necessary based on the Spring transaction
   48 |  * configuration.
   49 |  * <p>
   50 |  * The template needs a SqlSessionFactory to create SqlSessions, passed as a constructor argument. It also can be
   51 |  * constructed indicating the executor type to be used, if not, the default executor type, defined in the session
   52 |  * factory will be used.
   53 |  * <p>
   54 |  * This template converts MyBatis PersistenceExceptions into unchecked DataAccessExceptions, using, by default, a
   55 |  * {@code MyBatisExceptionTranslator}.
   56 |  * <p>
   57 |  * Because SqlSessionTemplate is thread safe, a single instance can be shared by all DAOs; there should also be a small
   58 |  * memory savings by doing this. This pattern can be used in Spring configuration files as follows:
   59 |  *
   60 |  * <pre class="code">
   61 |  * {@code
   62 |  * <bean id="sqlSessionTemplate" class="org.mybatis.spring.SqlSessionTemplate">
   63 |  *   <constructor-arg ref="sqlSessionFactory" />
   64 |  * </bean>
   65 |  * }
   66 |  * </pre>
   67 |  *
   68 |  * @author Putthiphong Boonphong
   69 |  * @author Hunter Presnall
   70 |  * @author Eduardo Macarron
   71 |  *
   72 |  * @see SqlSessionFactory
   73 |  * @see MyBatisExceptionTranslator
   74 |  */
   75 | public class SqlSessionTemplate implements SqlSession, DisposableBean {
   76 | 
   77 |   private final SqlSessionFactory sqlSessionFactory;
   78 | 
   79 |   private final ExecutorType executorType;
   80 | 
   81 |   private final SqlSession sqlSessionProxy;
   82 | 
   83 |   private final PersistenceExceptionTranslator exceptionTranslator;
   84 | 
   85 |   /**
   86 |    * Constructs a Spring managed SqlSession with the {@code SqlSessionFactory} provided as an argument.
   87 |    *
   88 |    * @param sqlSessionFactory
   89 |    *          a factory of SqlSession
   90 |    */
   91 |   public SqlSessionTemplate(SqlSessionFactory sqlSessionFactory) {
   92 |     this(sqlSessionFactory, sqlSessionFactory.getConfiguration().getDefaultExecutorType());
   93 |   }
   94 | 
   95 |   /**
   96 |    * Constructs a Spring managed SqlSession with the {@code SqlSessionFactory} provided as an argument and the given
   97 |    * {@code ExecutorType} {@code ExecutorType} cannot be changed once the {@code SqlSessionTemplate} is constructed.
   98 |    *
   99 |    * @param sqlSessionFactory
  100 |    *          a factory of SqlSession
  101 |    * @param executorType
  102 |    *          an executor type on session
  103 |    */
  104 |   public SqlSessionTemplate(SqlSessionFactory sqlSessionFactory, ExecutorType executorType) {
  105 |     this(sqlSessionFactory, executorType,
  106 |         new MyBatisExceptionTranslator(sqlSessionFactory.getConfiguration().getEnvironment().getDataSource(), true));
  107 |   }
  108 | 
  109 |   /**
  110 |    * Constructs a Spring managed {@code SqlSession} with the given {@code SqlSessionFactory} and {@code ExecutorType}. A
  111 |    * custom {@code SQLExceptionTranslator} can be provided as an argument so any {@code PersistenceException} thrown by
  112 |    * MyBatis can be custom translated to a {@code RuntimeException} The {@code SQLExceptionTranslator} can also be null
  113 |    * and thus no exception translation will be done and MyBatis exceptions will be thrown
  114 |    *
  115 |    * @param sqlSessionFactory
  116 |    *          a factory of SqlSession
  117 |    * @param executorType
  118 |    *          an executor type on session
  119 |    * @param exceptionTranslator
  120 |    *          a translator of exception
  121 |    */
  122 |   public SqlSessionTemplate(SqlSessionFactory sqlSessionFactory, ExecutorType executorType,
  123 |       PersistenceExceptionTranslator exceptionTranslator) {
  124 | 
  125 |     notNull(sqlSessionFactory, "Property 'sqlSessionFactory' is required");
  126 |     notNull(executorType, "Property 'executorType' is required");
  127 | 
  128 |     this.sqlSessionFactory = sqlSessionFactory;
  129 |     this.executorType = executorType;
  130 |     this.exceptionTranslator = exceptionTranslator;
  131 |     this.sqlSessionProxy = (SqlSession) newProxyInstance(SqlSessionFactory.class.getClassLoader(),
  132 |         new Class[] { SqlSession.class }, new SqlSessionInterceptor());
  133 |   }
  134 | 
  135 |   public SqlSessionFactory getSqlSessionFactory() {
  136 |     return this.sqlSessionFactory;
  137 |   }
  138 | 
  139 |   public ExecutorType getExecutorType() {
  140 |     return this.executorType;
  141 |   }
  142 | 
  143 |   public PersistenceExceptionTranslator getPersistenceExceptionTranslator() {
  144 |     return this.exceptionTranslator;
  145 |   }
  146 | 
  147 |   @Override
  148 |   public <T> T selectOne(String statement) {
  149 |     return this.sqlSessionProxy.selectOne(statement);
  150 |   }
  151 | 
  152 |   @Override
  153 |   public <T> T selectOne(String statement, Object parameter) {
  154 |     return this.sqlSessionProxy.selectOne(statement, parameter);
  155 |   }
  156 | 
  157 |   @Override
  158 |   public <K, V> Map<K, V> selectMap(String statement, String mapKey) {
  159 |     return this.sqlSessionProxy.selectMap(statement, mapKey);
  160 |   }
  161 | 
  162 |   @Override
  163 |   public <K, V> Map<K, V> selectMap(String statement, Object parameter, String mapKey) {
  164 |     return this.sqlSessionProxy.selectMap(statement, parameter, mapKey);
  165 |   }
  166 | 
  167 |   @Override
  168 |   public <K, V> Map<K, V> selectMap(String statement, Object parameter, String mapKey, RowBounds rowBounds) {
  169 |     return this.sqlSessionProxy.selectMap(statement, parameter, mapKey, rowBounds);
  170 |   }
  171 | 
  172 |   @Override
  173 |   public <T> Cursor<T> selectCursor(String statement) {
  174 |     return this.sqlSessionProxy.selectCursor(statement);
  175 |   }
  176 | 
  177 |   @Override
  178 |   public <T> Cursor<T> selectCursor(String statement, Object parameter) {
  179 |     return this.sqlSessionProxy.selectCursor(statement, parameter);
  180 |   }
  181 | 
  182 |   @Override
  183 |   public <T> Cursor<T> selectCursor(String statement, Object parameter, RowBounds rowBounds) {
  184 |     return this.sqlSessionProxy.selectCursor(statement, parameter, rowBounds);
  185 |   }
  186 | 
  187 |   @Override
  188 |   public <E> List<E> selectList(String statement) {
  189 |     return this.sqlSessionProxy.selectList(statement);
  190 |   }
  191 | 
  192 |   @Override
  193 |   public <E> List<E> selectList(String statement, Object parameter) {
  194 |     return this.sqlSessionProxy.selectList(statement, parameter);
  195 |   }
  196 | 
  197 |   @Override
  198 |   public <E> List<E> selectList(String statement, Object parameter, RowBounds rowBounds) {
  199 |     return this.sqlSessionProxy.selectList(statement, parameter, rowBounds);
  200 |   }
  201 | 
  202 |   @Override
  203 |   public void select(String statement, ResultHandler handler) {
  204 |     this.sqlSessionProxy.select(statement, handler);
  205 |   }
  206 | 
  207 |   @Override
  208 |   public void select(String statement, Object parameter, ResultHandler handler) {
  209 |     this.sqlSessionProxy.select(statement, parameter, handler);
  210 |   }
  211 | 
  212 |   @Override
  213 |   public void select(String statement, Object parameter, RowBounds rowBounds, ResultHandler handler) {
  214 |     this.sqlSessionProxy.select(statement, parameter, rowBounds, handler);
  215 |   }
  216 | 
  217 |   @Override
  218 |   public int insert(String statement) {
  219 |     return this.sqlSessionProxy.insert(statement);
  220 |   }
  221 | 
  222 |   @Override
  223 |   public int insert(String statement, Object parameter) {
  224 |     return this.sqlSessionProxy.insert(statement, parameter);
  225 |   }
  226 | 
  227 |   @Override
  228 |   public int update(String statement) {
  229 |     return this.sqlSessionProxy.update(statement);
  230 |   }
  231 | 
  232 |   @Override
  233 |   public int update(String statement, Object parameter) {
  234 |     return this.sqlSessionProxy.update(statement, parameter);
  235 |   }
  236 | 
  237 |   @Override
  238 |   public int delete(String statement) {
  239 |     return this.sqlSessionProxy.delete(statement);
  240 |   }
  241 | 
  242 |   @Override
  243 |   public int delete(String statement, Object parameter) {
  244 |     return this.sqlSessionProxy.delete(statement, parameter);
  245 |   }
  246 | 
  247 |   @Override
  248 |   public <T> T getMapper(Class<T> type) {
  249 |     return getConfiguration().getMapper(type, this);
  250 |   }
  251 | 
  252 |   @Override
  253 |   public void commit() {
  254 |     throw new UnsupportedOperationException("Manual commit is not allowed over a Spring managed SqlSession");
  255 |   }
  256 | 
  257 |   @Override
  258 |   public void commit(boolean force) {
  259 |     throw new UnsupportedOperationException("Manual commit is not allowed over a Spring managed SqlSession");
  260 |   }
  261 | 
  262 |   @Override
  263 |   public void rollback() {
  264 |     throw new UnsupportedOperationException("Manual rollback is not allowed over a Spring managed SqlSession");
  265 |   }
  266 | 
  267 |   @Override
  268 |   public void rollback(boolean force) {
  269 |     throw new UnsupportedOperationException("Manual rollback is not allowed over a Spring managed SqlSession");
  270 |   }
  271 | 
  272 |   @Override
  273 |   public void close() {
  274 |     throw new UnsupportedOperationException("Manual close is not allowed over a Spring managed SqlSession");
  275 |   }
  276 | 
  277 |   @Override
  278 |   public void clearCache() {
  279 |     this.sqlSessionProxy.clearCache();
  280 |   }
  281 | 
  282 |   @Override
  283 |   public Configuration getConfiguration() {
  284 |     return this.sqlSessionFactory.getConfiguration();
  285 |   }
  286 | 
  287 |   @Override
  288 |   public Connection getConnection() {
  289 |     return this.sqlSessionProxy.getConnection();
  290 |   }
  291 | 
  292 |   @Override
  293 |   public List<BatchResult> flushStatements() {
  294 |     return this.sqlSessionProxy.flushStatements();
  295 |   }
  296 | 
  297 |   /**
  298 |    * Allow gently dispose bean:
  299 |    *
  300 |    * <pre>
  301 |    * {@code
  302 |    *
  303 |    * <bean id="sqlSession" class="org.mybatis.spring.SqlSessionTemplate">
  304 |    *  <constructor-arg index="0" ref="sqlSessionFactory" />
  305 |    * </bean>
  306 |    * }
  307 |    * </pre>
  308 |    *
  309 |    * The implementation of {@link DisposableBean} forces spring context to use {@link DisposableBean#destroy()} method
  310 |    * instead of {@link SqlSessionTemplate#close()} to shutdown gently.
  311 |    *
  312 |    * @see SqlSessionTemplate#close()
  313 |    * @see "org.springframework.beans.factory.support.DisposableBeanAdapter#inferDestroyMethodIfNecessary(Object, RootBeanDefinition)"
  314 |    * @see "org.springframework.beans.factory.support.DisposableBeanAdapter#CLOSE_METHOD_NAME"
  315 |    */
  316 |   @Override
  317 |   public void destroy() throws Exception {
  318 |     // This method forces spring disposer to avoid call of SqlSessionTemplate.close() which gives
  319 |     // UnsupportedOperationException
  320 |   }
  321 | 
  322 |   /**
  323 |    * Proxy needed to route MyBatis method calls to the proper SqlSession got from Spring's Transaction Manager It also
  324 |    * unwraps exceptions thrown by {@code Method#invoke(Object, Object...)} to pass a {@code PersistenceException} to the
  325 |    * {@code PersistenceExceptionTranslator}.
  326 |    */
  327 |   private class SqlSessionInterceptor implements InvocationHandler {
  328 |     @Override
  329 |     public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
  330 |       var sqlSession = getSqlSession(SqlSessionTemplate.this.sqlSessionFactory, SqlSessionTemplate.this.executorType,
  331 |           SqlSessionTemplate.this.exceptionTranslator);
  332 |       try {
  333 |         var result = method.invoke(sqlSession, args);
  334 |         if (!isSqlSessionTransactional(sqlSession, SqlSessionTemplate.this.sqlSessionFactory)) {
  335 |           // force commit even on non-dirty sessions because some databases require
  336 |           // a commit/rollback before calling close()
  337 |           sqlSession.commit(true);
  338 |         }
  339 |         return result;
  340 |       } catch (Throwable t) {
  341 |         var unwrapped = unwrapThrowable(t);
  342 |         if (SqlSessionTemplate.this.exceptionTranslator != null && unwrapped instanceof PersistenceException) {
  343 |           // release the connection to avoid a deadlock if the translator is no loaded. See issue #22
  344 |           closeSqlSession(sqlSession, SqlSessionTemplate.this.sqlSessionFactory);
  345 |           sqlSession = null;
  346 |           Throwable translated = SqlSessionTemplate.this.exceptionTranslator
  347 |               .translateExceptionIfPossible((PersistenceException) unwrapped);
  348 |           if (translated != null) {
  349 |             unwrapped = translated;
  350 |           }
  351 |         }
  352 |         throw unwrapped;
  353 |       } finally {
  354 |         if (sqlSession != null) {
  355 |           closeSqlSession(sqlSession, SqlSessionTemplate.this.sqlSessionFactory);
  356 |         }
  357 |       }
  358 |     }
  359 |   }
  360 | 
  361 | }
```
