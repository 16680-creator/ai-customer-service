<!-- learning-docs source archive（机械抓取原文，禁止手改代码内容） -->
<!-- origin: org.mybatis:mybatis:3.6.0 | path: org/apache/ibatis/mapping/MappedStatement.java -->
<!-- 正文引用：[[源码:MappedStatement.java#L起-L止]] ；代码块头部：(src: MappedStatement.java#L起-L止) -->

# MappedStatement.java

| 项 | 值 |
| --- | --- |
| 来源 | `org.mybatis:mybatis:3.6.0` |
| 路径 | `org/apache/ibatis/mapping/MappedStatement.java` |
| 行数 | 356 |
| 在线地址 | https://repo1.maven.org/maven2/org/mybatis/mybatis/3.6.0/mybatis-3.6.0-sources.jar |
| 抓取时间 | 2026-11-25T14:00:19.129Z |

```java
    1 | /*
    2 |  *    Copyright 2009-2026 the original author or authors.
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
   16 | package org.apache.ibatis.mapping;
   17 | 
   18 | import java.util.ArrayList;
   19 | import java.util.List;
   20 | 
   21 | import org.apache.ibatis.cache.Cache;
   22 | import org.apache.ibatis.executor.keygen.Jdbc3KeyGenerator;
   23 | import org.apache.ibatis.executor.keygen.KeyGenerator;
   24 | import org.apache.ibatis.executor.keygen.NoKeyGenerator;
   25 | import org.apache.ibatis.logging.Log;
   26 | import org.apache.ibatis.logging.LogFactory;
   27 | import org.apache.ibatis.reflection.ParamNameResolver;
   28 | import org.apache.ibatis.scripting.LanguageDriver;
   29 | import org.apache.ibatis.session.Configuration;
   30 | 
   31 | /**
   32 |  * @author Clinton Begin
   33 |  */
   34 | public final class MappedStatement {
   35 | 
   36 |   private String resource;
   37 |   private Configuration configuration;
   38 |   private String id;
   39 |   private Integer fetchSize;
   40 |   private Integer timeout;
   41 |   private StatementType statementType;
   42 |   private ResultSetType resultSetType;
   43 |   private SqlSource sqlSource;
   44 |   private Cache cache;
   45 |   private ParameterMap parameterMap;
   46 |   private List<ResultMap> resultMaps;
   47 |   private boolean flushCacheRequired;
   48 |   private boolean useCache;
   49 |   private boolean resultOrdered;
   50 |   private SqlCommandType sqlCommandType;
   51 |   private KeyGenerator keyGenerator;
   52 |   private String[] keyProperties;
   53 |   private String[] keyColumns;
   54 |   private boolean hasNestedResultMaps;
   55 |   private String databaseId;
   56 |   private Log statementLog;
   57 |   private LanguageDriver lang;
   58 |   private String[] resultSets;
   59 |   private ParamNameResolver paramNameResolver;
   60 |   private boolean dirtySelect;
   61 | 
   62 |   MappedStatement() {
   63 |     // constructor disabled
   64 |   }
   65 | 
   66 |   public static class Builder {
   67 |     private final MappedStatement mappedStatement = new MappedStatement();
   68 | 
   69 |     public Builder(Configuration configuration, String id, SqlSource sqlSource, SqlCommandType sqlCommandType) {
   70 |       mappedStatement.configuration = configuration;
   71 |       mappedStatement.id = id;
   72 |       mappedStatement.sqlSource = sqlSource;
   73 |       mappedStatement.statementType = StatementType.PREPARED;
   74 |       mappedStatement.resultSetType = ResultSetType.DEFAULT;
   75 |       mappedStatement.parameterMap = ParameterMap.buildEmpty("defaultParameterMap", null);
   76 |       mappedStatement.resultMaps = new ArrayList<>();
   77 |       mappedStatement.sqlCommandType = sqlCommandType;
   78 |       mappedStatement.keyGenerator = configuration.isUseGeneratedKeys() && SqlCommandType.INSERT.equals(sqlCommandType)
   79 |           ? Jdbc3KeyGenerator.INSTANCE : NoKeyGenerator.INSTANCE;
   80 |       String logId = id;
   81 |       if (configuration.getLogPrefix() != null) {
   82 |         logId = configuration.getLogPrefix() + id;
   83 |       }
   84 |       mappedStatement.statementLog = LogFactory.getLog(logId);
   85 |       mappedStatement.lang = configuration.getDefaultScriptingLanguageInstance();
   86 |     }
   87 | 
   88 |     public Builder resource(String resource) {
   89 |       mappedStatement.resource = resource;
   90 |       return this;
   91 |     }
   92 | 
   93 |     public String id() {
   94 |       return mappedStatement.id;
   95 |     }
   96 | 
   97 |     public Builder parameterMap(ParameterMap parameterMap) {
   98 |       mappedStatement.parameterMap = parameterMap;
   99 |       return this;
  100 |     }
  101 | 
  102 |     public Builder resultMaps(List<ResultMap> resultMaps) {
  103 |       mappedStatement.resultMaps = resultMaps;
  104 |       for (ResultMap resultMap : resultMaps) {
  105 |         mappedStatement.hasNestedResultMaps = mappedStatement.hasNestedResultMaps || resultMap.hasNestedResultMaps();
  106 |       }
  107 |       return this;
  108 |     }
  109 | 
  110 |     public Builder fetchSize(Integer fetchSize) {
  111 |       mappedStatement.fetchSize = fetchSize;
  112 |       return this;
  113 |     }
  114 | 
  115 |     public Builder timeout(Integer timeout) {
  116 |       mappedStatement.timeout = timeout;
  117 |       return this;
  118 |     }
  119 | 
  120 |     public Builder statementType(StatementType statementType) {
  121 |       mappedStatement.statementType = statementType;
  122 |       return this;
  123 |     }
  124 | 
  125 |     public Builder resultSetType(ResultSetType resultSetType) {
  126 |       mappedStatement.resultSetType = resultSetType == null ? ResultSetType.DEFAULT : resultSetType;
  127 |       return this;
  128 |     }
  129 | 
  130 |     public Builder cache(Cache cache) {
  131 |       mappedStatement.cache = cache;
  132 |       return this;
  133 |     }
  134 | 
  135 |     public Builder flushCacheRequired(boolean flushCacheRequired) {
  136 |       mappedStatement.flushCacheRequired = flushCacheRequired;
  137 |       return this;
  138 |     }
  139 | 
  140 |     public Builder useCache(boolean useCache) {
  141 |       mappedStatement.useCache = useCache;
  142 |       return this;
  143 |     }
  144 | 
  145 |     public Builder resultOrdered(boolean resultOrdered) {
  146 |       mappedStatement.resultOrdered = resultOrdered;
  147 |       return this;
  148 |     }
  149 | 
  150 |     public Builder keyGenerator(KeyGenerator keyGenerator) {
  151 |       mappedStatement.keyGenerator = keyGenerator;
  152 |       return this;
  153 |     }
  154 | 
  155 |     public Builder keyProperty(String keyProperty) {
  156 |       mappedStatement.keyProperties = delimitedStringToArray(keyProperty);
  157 |       return this;
  158 |     }
  159 | 
  160 |     public Builder keyColumn(String keyColumn) {
  161 |       mappedStatement.keyColumns = delimitedStringToArray(keyColumn);
  162 |       return this;
  163 |     }
  164 | 
  165 |     public Builder databaseId(String databaseId) {
  166 |       mappedStatement.databaseId = databaseId;
  167 |       return this;
  168 |     }
  169 | 
  170 |     public Builder lang(LanguageDriver driver) {
  171 |       mappedStatement.lang = driver;
  172 |       return this;
  173 |     }
  174 | 
  175 |     public Builder resultSets(String resultSet) {
  176 |       mappedStatement.resultSets = delimitedStringToArray(resultSet);
  177 |       return this;
  178 |     }
  179 | 
  180 |     public Builder dirtySelect(boolean dirtySelect) {
  181 |       mappedStatement.dirtySelect = dirtySelect;
  182 |       return this;
  183 |     }
  184 | 
  185 |     public Builder paramNameResolver(ParamNameResolver paramNameResolver) {
  186 |       mappedStatement.paramNameResolver = paramNameResolver;
  187 |       return this;
  188 |     }
  189 | 
  190 |     /**
  191 |      * Resul sets.
  192 |      *
  193 |      * @param resultSet
  194 |      *          the result set
  195 |      *
  196 |      * @return the builder
  197 |      *
  198 |      * @deprecated Use {@link #resultSets}
  199 |      */
  200 |     @Deprecated
  201 |     public Builder resulSets(String resultSet) {
  202 |       mappedStatement.resultSets = delimitedStringToArray(resultSet);
  203 |       return this;
  204 |     }
  205 | 
  206 |     public MappedStatement build() {
  207 |       assert mappedStatement.configuration != null;
  208 |       assert mappedStatement.id != null;
  209 |       assert mappedStatement.sqlSource != null;
  210 |       assert mappedStatement.lang != null;
  211 |       mappedStatement.resultMaps = List.copyOf(mappedStatement.resultMaps);
  212 |       return mappedStatement;
  213 |     }
  214 |   }
  215 | 
  216 |   public KeyGenerator getKeyGenerator() {
  217 |     return keyGenerator;
  218 |   }
  219 | 
  220 |   public SqlCommandType getSqlCommandType() {
  221 |     return sqlCommandType;
  222 |   }
  223 | 
  224 |   public String getResource() {
  225 |     return resource;
  226 |   }
  227 | 
  228 |   public Configuration getConfiguration() {
  229 |     return configuration;
  230 |   }
  231 | 
  232 |   public String getId() {
  233 |     return id;
  234 |   }
  235 | 
  236 |   public boolean hasNestedResultMaps() {
  237 |     return hasNestedResultMaps;
  238 |   }
  239 | 
  240 |   public Integer getFetchSize() {
  241 |     return fetchSize;
  242 |   }
  243 | 
  244 |   public Integer getTimeout() {
  245 |     return timeout;
  246 |   }
  247 | 
  248 |   public StatementType getStatementType() {
  249 |     return statementType;
  250 |   }
  251 | 
  252 |   public ResultSetType getResultSetType() {
  253 |     return resultSetType;
  254 |   }
  255 | 
  256 |   public SqlSource getSqlSource() {
  257 |     return sqlSource;
  258 |   }
  259 | 
  260 |   public ParameterMap getParameterMap() {
  261 |     return parameterMap;
  262 |   }
  263 | 
  264 |   public List<ResultMap> getResultMaps() {
  265 |     return resultMaps;
  266 |   }
  267 | 
  268 |   public Cache getCache() {
  269 |     return cache;
  270 |   }
  271 | 
  272 |   public boolean isFlushCacheRequired() {
  273 |     return flushCacheRequired;
  274 |   }
  275 | 
  276 |   public boolean isUseCache() {
  277 |     return useCache;
  278 |   }
  279 | 
  280 |   public boolean isResultOrdered() {
  281 |     return resultOrdered;
  282 |   }
  283 | 
  284 |   public String getDatabaseId() {
  285 |     return databaseId;
  286 |   }
  287 | 
  288 |   public String[] getKeyProperties() {
  289 |     return keyProperties;
  290 |   }
  291 | 
  292 |   public String[] getKeyColumns() {
  293 |     return keyColumns;
  294 |   }
  295 | 
  296 |   public Log getStatementLog() {
  297 |     return statementLog;
  298 |   }
  299 | 
  300 |   public LanguageDriver getLang() {
  301 |     return lang;
  302 |   }
  303 | 
  304 |   public String[] getResultSets() {
  305 |     return resultSets;
  306 |   }
  307 | 
  308 |   public boolean isDirtySelect() {
  309 |     return dirtySelect;
  310 |   }
  311 | 
  312 |   public ParamNameResolver getParamNameResolver() {
  313 |     return paramNameResolver;
  314 |   }
  315 | 
  316 |   /**
  317 |    * Gets the resul sets.
  318 |    *
  319 |    * @return the resul sets
  320 |    *
  321 |    * @deprecated Use {@link #getResultSets()}
  322 |    */
  323 |   @Deprecated
  324 |   public String[] getResulSets() {
  325 |     return resultSets;
  326 |   }
  327 | 
  328 |   public BoundSql getBoundSql(Object parameterObject) {
  329 |     BoundSql boundSql = sqlSource.getBoundSql(parameterObject);
  330 |     List<ParameterMapping> parameterMappings = boundSql.getParameterMappings();
  331 |     if (parameterMappings == null || parameterMappings.isEmpty()) {
  332 |       boundSql = new BoundSql(configuration, boundSql.getSql(), parameterMap.getParameterMappings(), parameterObject);
  333 |     }
  334 | 
  335 |     // check for nested result maps in parameter mappings (issue #30)
  336 |     for (ParameterMapping pm : boundSql.getParameterMappings()) {
  337 |       String rmId = pm.getResultMapId();
  338 |       if (rmId != null) {
  339 |         ResultMap rm = configuration.getResultMap(rmId);
  340 |         if (rm != null) {
  341 |           hasNestedResultMaps |= rm.hasNestedResultMaps();
  342 |         }
  343 |       }
  344 |     }
  345 | 
  346 |     return boundSql;
  347 |   }
  348 | 
  349 |   private static String[] delimitedStringToArray(String in) {
  350 |     if (in == null || in.trim().length() == 0) {
  351 |       return null;
  352 |     }
  353 |     return in.split(",");
  354 |   }
  355 | 
  356 | }
```
