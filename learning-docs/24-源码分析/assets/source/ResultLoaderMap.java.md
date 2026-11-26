<!-- learning-docs source archive（机械抓取原文，禁止手改代码内容） -->
<!-- origin: org.mybatis:mybatis:3.6.0 | path: org/apache/ibatis/executor/loader/ResultLoaderMap.java -->
<!-- 正文引用：[[源码:ResultLoaderMap.java#L起-L止]] ；代码块头部：(src: ResultLoaderMap.java#L起-L止) -->

# ResultLoaderMap.java

| 项 | 值 |
| --- | --- |
| 来源 | `org.mybatis:mybatis:3.6.0` |
| 路径 | `org/apache/ibatis/executor/loader/ResultLoaderMap.java` |
| 行数 | 313 |
| 在线地址 | https://repo1.maven.org/maven2/org/mybatis/mybatis/3.6.0/mybatis-3.6.0-sources.jar |
| 抓取时间 | 2026-11-25T15:09:17.558Z |

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
   16 | package org.apache.ibatis.executor.loader;
   17 | 
   18 | import java.io.Serializable;
   19 | import java.lang.reflect.Method;
   20 | import java.lang.reflect.Modifier;
   21 | import java.security.AccessController;
   22 | import java.security.PrivilegedActionException;
   23 | import java.security.PrivilegedExceptionAction;
   24 | import java.sql.SQLException;
   25 | import java.util.HashMap;
   26 | import java.util.List;
   27 | import java.util.Locale;
   28 | import java.util.Map;
   29 | import java.util.Set;
   30 | 
   31 | import org.apache.ibatis.cursor.Cursor;
   32 | import org.apache.ibatis.executor.BaseExecutor;
   33 | import org.apache.ibatis.executor.BatchResult;
   34 | import org.apache.ibatis.executor.ExecutorException;
   35 | import org.apache.ibatis.logging.Log;
   36 | import org.apache.ibatis.logging.LogFactory;
   37 | import org.apache.ibatis.mapping.BoundSql;
   38 | import org.apache.ibatis.mapping.MappedStatement;
   39 | import org.apache.ibatis.reflection.MetaObject;
   40 | import org.apache.ibatis.session.Configuration;
   41 | import org.apache.ibatis.session.ResultHandler;
   42 | import org.apache.ibatis.session.RowBounds;
   43 | 
   44 | /**
   45 |  * @author Clinton Begin
   46 |  * @author Franta Mejta
   47 |  */
   48 | public class ResultLoaderMap {
   49 | 
   50 |   private final Map<String, LoadPair> loaderMap = new HashMap<>();
   51 | 
   52 |   public void addLoader(String property, MetaObject metaResultObject, ResultLoader resultLoader) {
   53 |     String upperFirst = getUppercaseFirstProperty(property);
   54 |     if (!upperFirst.equalsIgnoreCase(property) && loaderMap.containsKey(upperFirst)) {
   55 |       throw new ExecutorException("Nested lazy loaded result property '" + property + "' for query id '"
   56 |           + resultLoader.mappedStatement.getId()
   57 |           + " already exists in the result map. The leftmost property of all lazy loaded properties must be unique within a result map.");
   58 |     }
   59 |     loaderMap.put(upperFirst, new LoadPair(property, metaResultObject, resultLoader));
   60 |   }
   61 | 
   62 |   public final Map<String, LoadPair> getProperties() {
   63 |     return new HashMap<>(this.loaderMap);
   64 |   }
   65 | 
   66 |   public Set<String> getPropertyNames() {
   67 |     return loaderMap.keySet();
   68 |   }
   69 | 
   70 |   public int size() {
   71 |     return loaderMap.size();
   72 |   }
   73 | 
   74 |   public boolean isEmpty() {
   75 |     return loaderMap.isEmpty();
   76 |   }
   77 | 
   78 |   public boolean hasLoader(String property) {
   79 |     return loaderMap.containsKey(property.toUpperCase(Locale.ENGLISH));
   80 |   }
   81 | 
   82 |   public boolean load(String property) throws SQLException {
   83 |     LoadPair pair = loaderMap.remove(property.toUpperCase(Locale.ENGLISH));
   84 |     if (pair != null) {
   85 |       pair.load();
   86 |       return true;
   87 |     }
   88 |     return false;
   89 |   }
   90 | 
   91 |   public void remove(String property) {
   92 |     loaderMap.remove(property.toUpperCase(Locale.ENGLISH));
   93 |   }
   94 | 
   95 |   public void loadAll() throws SQLException {
   96 |     final Set<String> methodNameSet = loaderMap.keySet();
   97 |     String[] methodNames = methodNameSet.toArray(new String[methodNameSet.size()]);
   98 |     for (String methodName : methodNames) {
   99 |       load(methodName);
  100 |     }
  101 |   }
  102 | 
  103 |   private static String getUppercaseFirstProperty(String property) {
  104 |     String[] parts = property.split("\\.");
  105 |     return parts[0].toUpperCase(Locale.ENGLISH);
  106 |   }
  107 | 
  108 |   /**
  109 |    * Property which was not loaded yet.
  110 |    */
  111 |   public static class LoadPair implements Serializable {
  112 | 
  113 |     private static final long serialVersionUID = 20130412;
  114 |     /**
  115 |      * Name of factory method which returns database connection.
  116 |      */
  117 |     private static final String FACTORY_METHOD = "getConfiguration";
  118 |     /**
  119 |      * Object to check whether we went through serialization..
  120 |      */
  121 |     private final transient Object serializationCheck = new Object();
  122 |     /**
  123 |      * Meta object which sets loaded properties.
  124 |      */
  125 |     private transient MetaObject metaResultObject;
  126 |     /**
  127 |      * Result loader which loads unread properties.
  128 |      */
  129 |     private transient ResultLoader resultLoader;
  130 |     /**
  131 |      * Wow, logger.
  132 |      */
  133 |     private transient Log log;
  134 |     /**
  135 |      * Factory class through which we get database connection.
  136 |      */
  137 |     private Class<?> configurationFactory;
  138 |     /**
  139 |      * Name of the unread property.
  140 |      */
  141 |     private final String property;
  142 |     /**
  143 |      * ID of SQL statement which loads the property.
  144 |      */
  145 |     private String mappedStatement;
  146 |     /**
  147 |      * Parameter of the sql statement.
  148 |      */
  149 |     private Serializable mappedParameter;
  150 | 
  151 |     private LoadPair(final String property, MetaObject metaResultObject, ResultLoader resultLoader) {
  152 |       this.property = property;
  153 |       this.metaResultObject = metaResultObject;
  154 |       this.resultLoader = resultLoader;
  155 | 
  156 |       /* Save required information only if original object can be serialized. */
  157 |       if (metaResultObject != null && metaResultObject.getOriginalObject() instanceof Serializable) {
  158 |         final Object mappedStatementParameter = resultLoader.parameterObject;
  159 | 
  160 |         /* @todo May the parameter be null? */
  161 |         if (mappedStatementParameter instanceof Serializable) {
  162 |           this.mappedStatement = resultLoader.mappedStatement.getId();
  163 |           this.mappedParameter = (Serializable) mappedStatementParameter;
  164 | 
  165 |           this.configurationFactory = resultLoader.configuration.getConfigurationFactory();
  166 |         } else {
  167 |           Log log = this.getLogger();
  168 |           if (log.isDebugEnabled()) {
  169 |             log.debug("Property [" + this.property + "] of [" + metaResultObject.getOriginalObject().getClass()
  170 |                 + "] cannot be loaded " + "after deserialization. Make sure it's loaded before serializing "
  171 |                 + "forenamed object.");
  172 |           }
  173 |         }
  174 |       }
  175 |     }
  176 | 
  177 |     public void load() throws SQLException {
  178 |       /*
  179 |        * These field should not be null unless the loadpair was serialized. Yet in that case this method should not be
  180 |        * called.
  181 |        */
  182 |       if (this.metaResultObject == null) {
  183 |         throw new IllegalArgumentException("metaResultObject is null");
  184 |       }
  185 |       if (this.resultLoader == null) {
  186 |         throw new IllegalArgumentException("resultLoader is null");
  187 |       }
  188 | 
  189 |       this.load(null);
  190 |     }
  191 | 
  192 |     public void load(final Object userObject) throws SQLException {
  193 |       if (this.metaResultObject == null || this.resultLoader == null) {
  194 |         if (this.mappedParameter == null) {
  195 |           throw new ExecutorException("Property [" + this.property + "] cannot be loaded because "
  196 |               + "required parameter of mapped statement [" + this.mappedStatement + "] is not serializable.");
  197 |         }
  198 | 
  199 |         final Configuration config = this.getConfiguration();
  200 |         final MappedStatement ms = config.getMappedStatement(this.mappedStatement);
  201 |         if (ms == null) {
  202 |           throw new ExecutorException(
  203 |               "Cannot lazy load property [" + this.property + "] of deserialized object [" + userObject.getClass()
  204 |                   + "] because configuration does not contain statement [" + this.mappedStatement + "]");
  205 |         }
  206 | 
  207 |         this.metaResultObject = config.newMetaObject(userObject);
  208 |         this.resultLoader = new ResultLoader(config, new ClosedExecutor(), ms, this.mappedParameter,
  209 |             metaResultObject.getSetterType(this.property), null, null);
  210 |       }
  211 | 
  212 |       /*
  213 |        * We are using a new executor because we may be (and likely are) on a new thread and executors aren't thread
  214 |        * safe. (Is this sufficient?) A better approach would be making executors thread safe.
  215 |        */
  216 |       if (this.serializationCheck == null) {
  217 |         final ResultLoader old = this.resultLoader;
  218 |         this.resultLoader = new ResultLoader(old.configuration, new ClosedExecutor(), old.mappedStatement,
  219 |             old.parameterObject, old.targetType, old.cacheKey, old.boundSql);
  220 |       }
  221 | 
  222 |       this.metaResultObject.setValue(property, this.resultLoader.loadResult());
  223 |     }
  224 | 
  225 |     private Configuration getConfiguration() {
  226 |       if (this.configurationFactory == null) {
  227 |         throw new ExecutorException("Cannot get Configuration as configuration factory was not set.");
  228 |       }
  229 | 
  230 |       Object configurationObject;
  231 |       try {
  232 |         final Method factoryMethod = this.configurationFactory.getDeclaredMethod(FACTORY_METHOD);
  233 |         if (!Modifier.isStatic(factoryMethod.getModifiers())) {
  234 |           throw new ExecutorException("Cannot get Configuration as factory method [" + this.configurationFactory + "]#["
  235 |               + FACTORY_METHOD + "] is not static.");
  236 |         }
  237 | 
  238 |         if (!factoryMethod.canAccess(null)) {
  239 |           configurationObject = AccessController.doPrivileged((PrivilegedExceptionAction<Object>) () -> {
  240 |             try {
  241 |               factoryMethod.setAccessible(true);
  242 |               return factoryMethod.invoke(null);
  243 |             } finally {
  244 |               factoryMethod.setAccessible(false);
  245 |             }
  246 |           });
  247 |         } else {
  248 |           configurationObject = factoryMethod.invoke(null);
  249 |         }
  250 |       } catch (final ExecutorException ex) {
  251 |         throw ex;
  252 |       } catch (final NoSuchMethodException ex) {
  253 |         throw new ExecutorException("Cannot get Configuration as factory class [" + this.configurationFactory
  254 |             + "] is missing factory method of name [" + FACTORY_METHOD + "].", ex);
  255 |       } catch (final PrivilegedActionException ex) {
  256 |         throw new ExecutorException("Cannot get Configuration as factory method [" + this.configurationFactory + "]#["
  257 |             + FACTORY_METHOD + "] threw an exception.", ex.getCause());
  258 |       } catch (final Exception ex) {
  259 |         throw new ExecutorException("Cannot get Configuration as factory method [" + this.configurationFactory + "]#["
  260 |             + FACTORY_METHOD + "] threw an exception.", ex);
  261 |       }
  262 | 
  263 |       if (!(configurationObject instanceof Configuration)) {
  264 |         throw new ExecutorException("Cannot get Configuration as factory method [" + this.configurationFactory + "]#["
  265 |             + FACTORY_METHOD + "] didn't return [" + Configuration.class + "] but ["
  266 |             + (configurationObject == null ? "null" : configurationObject.getClass()) + "].");
  267 |       }
  268 | 
  269 |       return Configuration.class.cast(configurationObject);
  270 |     }
  271 | 
  272 |     private Log getLogger() {
  273 |       if (this.log == null) {
  274 |         this.log = LogFactory.getLog(this.getClass());
  275 |       }
  276 |       return this.log;
  277 |     }
  278 |   }
  279 | 
  280 |   private static final class ClosedExecutor extends BaseExecutor {
  281 | 
  282 |     public ClosedExecutor() {
  283 |       super(null, null);
  284 |     }
  285 | 
  286 |     @Override
  287 |     public boolean isClosed() {
  288 |       return true;
  289 |     }
  290 | 
  291 |     @Override
  292 |     protected int doUpdate(MappedStatement ms, Object parameter) throws SQLException {
  293 |       throw new UnsupportedOperationException("Not supported.");
  294 |     }
  295 | 
  296 |     @Override
  297 |     protected List<BatchResult> doFlushStatements(boolean isRollback) throws SQLException {
  298 |       throw new UnsupportedOperationException("Not supported.");
  299 |     }
  300 | 
  301 |     @Override
  302 |     protected <E> List<E> doQuery(MappedStatement ms, Object parameter, RowBounds rowBounds,
  303 |         ResultHandler resultHandler, BoundSql boundSql) throws SQLException {
  304 |       throw new UnsupportedOperationException("Not supported.");
  305 |     }
  306 | 
  307 |     @Override
  308 |     protected <E> Cursor<E> doQueryCursor(MappedStatement ms, Object parameter, RowBounds rowBounds, BoundSql boundSql)
  309 |         throws SQLException {
  310 |       throw new UnsupportedOperationException("Not supported.");
  311 |     }
  312 |   }
  313 | }
```
