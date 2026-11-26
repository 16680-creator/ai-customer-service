<!-- learning-docs source archive（机械抓取原文，禁止手改代码内容） -->
<!-- origin: org.mybatis:mybatis:3.6.0 | path: org/apache/ibatis/session/defaults/DefaultSqlSession.java -->
<!-- 正文引用：[[源码:DefaultSqlSession.java#L起-L止]] ；代码块头部：(src: DefaultSqlSession.java#L起-L止) -->

# DefaultSqlSession.java

| 项 | 值 |
| --- | --- |
| 来源 | `org.mybatis:mybatis:3.6.0` |
| 路径 | `org/apache/ibatis/session/defaults/DefaultSqlSession.java` |
| 行数 | 336 |
| 在线地址 | https://repo1.maven.org/maven2/org/mybatis/mybatis/3.6.0/mybatis-3.6.0-sources.jar |
| 抓取时间 | 2026-11-25T13:48:34.192Z |

```java
    1 | /*
    2 |  *    Copyright 2009-2025 the original author or authors.
    3 |  *
    4 |  *    Licensed under the Apache License, Version 2.0 (the "License");
    5 |  *    you may not use this file except in compliance with the License.
    6 |  *    You may obtain a copy of the License at
    7 |  *
    8 |  *       https://www.apache.org/licenses/LICENSE-2.0
    9 |  *
   10 |  *    Unless required by applicable law or agreed to in writing, software
   11 |  *    distributed under the License is distributed on an "AS IS" BASIS,
   12 |  *    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
   13 |  *    See the License for the specific language governing permissions and
   14 |  *    limitations under the License.
   15 |  */
   16 | package org.apache.ibatis.session.defaults;
   17 | 
   18 | import java.sql.Connection;
   19 | import java.sql.SQLException;
   20 | import java.util.ArrayList;
   21 | import java.util.HashMap;
   22 | import java.util.List;
   23 | import java.util.Map;
   24 | 
   25 | import org.apache.ibatis.binding.BindingException;
   26 | import org.apache.ibatis.cursor.Cursor;
   27 | import org.apache.ibatis.exceptions.ExceptionFactory;
   28 | import org.apache.ibatis.exceptions.TooManyResultsException;
   29 | import org.apache.ibatis.executor.BatchResult;
   30 | import org.apache.ibatis.executor.ErrorContext;
   31 | import org.apache.ibatis.executor.Executor;
   32 | import org.apache.ibatis.executor.result.DefaultMapResultHandler;
   33 | import org.apache.ibatis.executor.result.DefaultResultContext;
   34 | import org.apache.ibatis.mapping.MappedStatement;
   35 | import org.apache.ibatis.reflection.ParamNameResolver;
   36 | import org.apache.ibatis.session.Configuration;
   37 | import org.apache.ibatis.session.ResultHandler;
   38 | import org.apache.ibatis.session.RowBounds;
   39 | import org.apache.ibatis.session.SqlSession;
   40 | 
   41 | /**
   42 |  * The default implementation for {@link SqlSession}. Note that this class is not Thread-Safe.
   43 |  *
   44 |  * @author Clinton Begin
   45 |  */
   46 | public class DefaultSqlSession implements SqlSession {
   47 | 
   48 |   private final Configuration configuration;
   49 |   private final Executor executor;
   50 | 
   51 |   private final boolean autoCommit;
   52 |   private boolean dirty;
   53 |   private List<Cursor<?>> cursorList;
   54 | 
   55 |   public DefaultSqlSession(Configuration configuration, Executor executor, boolean autoCommit) {
   56 |     this.configuration = configuration;
   57 |     this.executor = executor;
   58 |     this.dirty = false;
   59 |     this.autoCommit = autoCommit;
   60 |   }
   61 | 
   62 |   public DefaultSqlSession(Configuration configuration, Executor executor) {
   63 |     this(configuration, executor, false);
   64 |   }
   65 | 
   66 |   @Override
   67 |   public <T> T selectOne(String statement) {
   68 |     return this.selectOne(statement, null);
   69 |   }
   70 | 
   71 |   @Override
   72 |   public <T> T selectOne(String statement, Object parameter) {
   73 |     // Popular vote was to return null on 0 results and throw exception on too many.
   74 |     List<T> list = this.selectList(statement, parameter);
   75 |     if (list.size() == 1) {
   76 |       return list.get(0);
   77 |     }
   78 |     if (list.size() > 1) {
   79 |       throw new TooManyResultsException(
   80 |           "Expected one result (or null) to be returned by selectOne(), but found: " + list.size());
   81 |     } else {
   82 |       return null;
   83 |     }
   84 |   }
   85 | 
   86 |   @Override
   87 |   public <K, V> Map<K, V> selectMap(String statement, String mapKey) {
   88 |     return this.selectMap(statement, null, mapKey, RowBounds.DEFAULT);
   89 |   }
   90 | 
   91 |   @Override
   92 |   public <K, V> Map<K, V> selectMap(String statement, Object parameter, String mapKey) {
   93 |     return this.selectMap(statement, parameter, mapKey, RowBounds.DEFAULT);
   94 |   }
   95 | 
   96 |   @Override
   97 |   public <K, V> Map<K, V> selectMap(String statement, Object parameter, String mapKey, RowBounds rowBounds) {
   98 |     final List<? extends V> list = selectList(statement, parameter, rowBounds);
   99 |     final DefaultMapResultHandler<K, V> mapResultHandler = new DefaultMapResultHandler<>(mapKey,
  100 |         configuration.getObjectFactory(), configuration.getObjectWrapperFactory(), configuration.getReflectorFactory());
  101 |     final DefaultResultContext<V> context = new DefaultResultContext<>();
  102 |     for (V o : list) {
  103 |       context.nextResultObject(o);
  104 |       mapResultHandler.handleResult(context);
  105 |     }
  106 |     return mapResultHandler.getMappedResults();
  107 |   }
  108 | 
  109 |   @Override
  110 |   public <T> Cursor<T> selectCursor(String statement) {
  111 |     return selectCursor(statement, null);
  112 |   }
  113 | 
  114 |   @Override
  115 |   public <T> Cursor<T> selectCursor(String statement, Object parameter) {
  116 |     return selectCursor(statement, parameter, RowBounds.DEFAULT);
  117 |   }
  118 | 
  119 |   @Override
  120 |   public <T> Cursor<T> selectCursor(String statement, Object parameter, RowBounds rowBounds) {
  121 |     try {
  122 |       MappedStatement ms = configuration.getMappedStatement(statement);
  123 |       dirty |= ms.isDirtySelect();
  124 |       Cursor<T> cursor = executor.queryCursor(ms, wrapCollection(parameter), rowBounds);
  125 |       registerCursor(cursor);
  126 |       return cursor;
  127 |     } catch (Exception e) {
  128 |       throw ExceptionFactory.wrapException("Error querying database.  Cause: " + e, e);
  129 |     } finally {
  130 |       ErrorContext.instance().reset();
  131 |     }
  132 |   }
  133 | 
  134 |   @Override
  135 |   public <E> List<E> selectList(String statement) {
  136 |     return this.selectList(statement, null);
  137 |   }
  138 | 
  139 |   @Override
  140 |   public <E> List<E> selectList(String statement, Object parameter) {
  141 |     return this.selectList(statement, parameter, RowBounds.DEFAULT);
  142 |   }
  143 | 
  144 |   @Override
  145 |   public <E> List<E> selectList(String statement, Object parameter, RowBounds rowBounds) {
  146 |     return selectList(statement, parameter, rowBounds, Executor.NO_RESULT_HANDLER);
  147 |   }
  148 | 
  149 |   private <E> List<E> selectList(String statement, Object parameter, RowBounds rowBounds, ResultHandler handler) {
  150 |     try {
  151 |       MappedStatement ms = configuration.getMappedStatement(statement);
  152 |       dirty |= ms.isDirtySelect();
  153 |       return executor.query(ms, wrapCollection(parameter), rowBounds, handler);
  154 |     } catch (Exception e) {
  155 |       throw ExceptionFactory.wrapException("Error querying database.  Cause: " + e, e);
  156 |     } finally {
  157 |       ErrorContext.instance().reset();
  158 |     }
  159 |   }
  160 | 
  161 |   @Override
  162 |   public void select(String statement, Object parameter, ResultHandler handler) {
  163 |     select(statement, parameter, RowBounds.DEFAULT, handler);
  164 |   }
  165 | 
  166 |   @Override
  167 |   public void select(String statement, ResultHandler handler) {
  168 |     select(statement, null, RowBounds.DEFAULT, handler);
  169 |   }
  170 | 
  171 |   @Override
  172 |   public void select(String statement, Object parameter, RowBounds rowBounds, ResultHandler handler) {
  173 |     selectList(statement, parameter, rowBounds, handler);
  174 |   }
  175 | 
  176 |   @Override
  177 |   public int insert(String statement) {
  178 |     return insert(statement, null);
  179 |   }
  180 | 
  181 |   @Override
  182 |   public int insert(String statement, Object parameter) {
  183 |     return update(statement, parameter);
  184 |   }
  185 | 
  186 |   @Override
  187 |   public int update(String statement) {
  188 |     return update(statement, null);
  189 |   }
  190 | 
  191 |   @Override
  192 |   public int update(String statement, Object parameter) {
  193 |     try {
  194 |       dirty = true;
  195 |       MappedStatement ms = configuration.getMappedStatement(statement);
  196 |       return executor.update(ms, wrapCollection(parameter));
  197 |     } catch (Exception e) {
  198 |       throw ExceptionFactory.wrapException("Error updating database.  Cause: " + e, e);
  199 |     } finally {
  200 |       ErrorContext.instance().reset();
  201 |     }
  202 |   }
  203 | 
  204 |   @Override
  205 |   public int delete(String statement) {
  206 |     return update(statement, null);
  207 |   }
  208 | 
  209 |   @Override
  210 |   public int delete(String statement, Object parameter) {
  211 |     return update(statement, parameter);
  212 |   }
  213 | 
  214 |   @Override
  215 |   public void commit() {
  216 |     commit(false);
  217 |   }
  218 | 
  219 |   @Override
  220 |   public void commit(boolean force) {
  221 |     try {
  222 |       executor.commit(isCommitOrRollbackRequired(force));
  223 |       dirty = false;
  224 |     } catch (Exception e) {
  225 |       throw ExceptionFactory.wrapException("Error committing transaction.  Cause: " + e, e);
  226 |     } finally {
  227 |       ErrorContext.instance().reset();
  228 |     }
  229 |   }
  230 | 
  231 |   @Override
  232 |   public void rollback() {
  233 |     rollback(false);
  234 |   }
  235 | 
  236 |   @Override
  237 |   public void rollback(boolean force) {
  238 |     try {
  239 |       executor.rollback(isCommitOrRollbackRequired(force));
  240 |       dirty = false;
  241 |     } catch (Exception e) {
  242 |       throw ExceptionFactory.wrapException("Error rolling back transaction.  Cause: " + e, e);
  243 |     } finally {
  244 |       ErrorContext.instance().reset();
  245 |     }
  246 |   }
  247 | 
  248 |   @Override
  249 |   public List<BatchResult> flushStatements() {
  250 |     try {
  251 |       return executor.flushStatements();
  252 |     } catch (Exception e) {
  253 |       throw ExceptionFactory.wrapException("Error flushing statements.  Cause: " + e, e);
  254 |     } finally {
  255 |       ErrorContext.instance().reset();
  256 |     }
  257 |   }
  258 | 
  259 |   @Override
  260 |   public void close() {
  261 |     try {
  262 |       executor.close(isCommitOrRollbackRequired(false));
  263 |       closeCursors();
  264 |       dirty = false;
  265 |     } finally {
  266 |       ErrorContext.instance().reset();
  267 |     }
  268 |   }
  269 | 
  270 |   private void closeCursors() {
  271 |     if (cursorList != null && !cursorList.isEmpty()) {
  272 |       for (Cursor<?> cursor : cursorList) {
  273 |         cursor.close();
  274 |       }
  275 |       cursorList.clear();
  276 |     }
  277 |   }
  278 | 
  279 |   @Override
  280 |   public Configuration getConfiguration() {
  281 |     return configuration;
  282 |   }
  283 | 
  284 |   @Override
  285 |   public <T> T getMapper(Class<T> type) {
  286 |     return configuration.getMapper(type, this);
  287 |   }
  288 | 
  289 |   @Override
  290 |   public Connection getConnection() {
  291 |     try {
  292 |       return executor.getTransaction().getConnection();
  293 |     } catch (SQLException e) {
  294 |       throw ExceptionFactory.wrapException("Error getting a new connection.  Cause: " + e, e);
  295 |     }
  296 |   }
  297 | 
  298 |   @Override
  299 |   public void clearCache() {
  300 |     executor.clearLocalCache();
  301 |   }
  302 | 
  303 |   private <T> void registerCursor(Cursor<T> cursor) {
  304 |     if (cursorList == null) {
  305 |       cursorList = new ArrayList<>();
  306 |     }
  307 |     cursorList.add(cursor);
  308 |   }
  309 | 
  310 |   private boolean isCommitOrRollbackRequired(boolean force) {
  311 |     return !autoCommit && dirty || force;
  312 |   }
  313 | 
  314 |   private Object wrapCollection(final Object object) {
  315 |     return ParamNameResolver.wrapToMapIfCollection(object, null);
  316 |   }
  317 | 
  318 |   /**
  319 |    * @deprecated Since 3.5.5
  320 |    */
  321 |   @Deprecated
  322 |   public static class StrictMap<V> extends HashMap<String, V> {
  323 | 
  324 |     private static final long serialVersionUID = -5741767162221585340L;
  325 | 
  326 |     @Override
  327 |     public V get(Object key) {
  328 |       if (!super.containsKey(key)) {
  329 |         throw new BindingException("Parameter '" + key + "' not found. Available parameters are " + this.keySet());
  330 |       }
  331 |       return super.get(key);
  332 |     }
  333 | 
  334 |   }
  335 | 
  336 | }
```
