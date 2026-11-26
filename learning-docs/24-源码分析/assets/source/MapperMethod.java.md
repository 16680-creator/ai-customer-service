<!-- learning-docs source archive（机械抓取原文，禁止手改代码内容） -->
<!-- origin: org.mybatis:mybatis:3.6.0 | path: org/apache/ibatis/binding/MapperMethod.java -->
<!-- 正文引用：[[源码:MapperMethod.java#L起-L止]] ；代码块头部：(src: MapperMethod.java#L起-L止) -->

# MapperMethod.java

| 项 | 值 |
| --- | --- |
| 来源 | `org.mybatis:mybatis:3.6.0` |
| 路径 | `org/apache/ibatis/binding/MapperMethod.java` |
| 行数 | 386 |
| 在线地址 | https://repo1.maven.org/maven2/org/mybatis/mybatis/3.6.0/mybatis-3.6.0-sources.jar |
| 抓取时间 | 2026-11-25T14:00:20.416Z |

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
   16 | package org.apache.ibatis.binding;
   17 | 
   18 | import java.lang.reflect.Array;
   19 | import java.lang.reflect.Method;
   20 | import java.lang.reflect.ParameterizedType;
   21 | import java.lang.reflect.Type;
   22 | import java.util.HashMap;
   23 | import java.util.List;
   24 | import java.util.Map;
   25 | import java.util.Optional;
   26 | 
   27 | import org.apache.ibatis.annotations.Flush;
   28 | import org.apache.ibatis.annotations.MapKey;
   29 | import org.apache.ibatis.cursor.Cursor;
   30 | import org.apache.ibatis.mapping.MappedStatement;
   31 | import org.apache.ibatis.mapping.SqlCommandType;
   32 | import org.apache.ibatis.mapping.StatementType;
   33 | import org.apache.ibatis.reflection.MetaObject;
   34 | import org.apache.ibatis.reflection.ParamNameResolver;
   35 | import org.apache.ibatis.reflection.TypeParameterResolver;
   36 | import org.apache.ibatis.session.Configuration;
   37 | import org.apache.ibatis.session.ResultHandler;
   38 | import org.apache.ibatis.session.RowBounds;
   39 | import org.apache.ibatis.session.SqlSession;
   40 | 
   41 | /**
   42 |  * @author Clinton Begin
   43 |  * @author Eduardo Macarron
   44 |  * @author Lasse Voss
   45 |  * @author Kazuki Shimizu
   46 |  */
   47 | public class MapperMethod {
   48 | 
   49 |   private final SqlCommand command;
   50 |   private final MethodSignature method;
   51 | 
   52 |   public MapperMethod(Class<?> mapperInterface, Method method, Configuration config) {
   53 |     this.command = new SqlCommand(config, mapperInterface, method);
   54 |     this.method = new MethodSignature(config, mapperInterface, method);
   55 |   }
   56 | 
   57 |   public Object execute(SqlSession sqlSession, Object[] args) {
   58 |     Object result;
   59 |     switch (command.getType()) {
   60 |       case INSERT: {
   61 |         Object param = method.convertArgsToSqlCommandParam(args);
   62 |         result = rowCountResult(sqlSession.insert(command.getName(), param));
   63 |         break;
   64 |       }
   65 |       case UPDATE: {
   66 |         Object param = method.convertArgsToSqlCommandParam(args);
   67 |         result = rowCountResult(sqlSession.update(command.getName(), param));
   68 |         break;
   69 |       }
   70 |       case DELETE: {
   71 |         Object param = method.convertArgsToSqlCommandParam(args);
   72 |         result = rowCountResult(sqlSession.delete(command.getName(), param));
   73 |         break;
   74 |       }
   75 |       case SELECT:
   76 |         if (method.returnsVoid() && method.hasResultHandler()) {
   77 |           executeWithResultHandler(sqlSession, args);
   78 |           result = null;
   79 |         } else if (method.returnsMany()) {
   80 |           result = executeForMany(sqlSession, args);
   81 |         } else if (method.returnsMap()) {
   82 |           result = executeForMap(sqlSession, args);
   83 |         } else if (method.returnsCursor()) {
   84 |           result = executeForCursor(sqlSession, args);
   85 |         } else {
   86 |           Object param = method.convertArgsToSqlCommandParam(args);
   87 |           result = sqlSession.selectOne(command.getName(), param);
   88 |           if (method.returnsOptional() && (result == null || !method.getReturnType().equals(result.getClass()))) {
   89 |             result = Optional.ofNullable(result);
   90 |           }
   91 |         }
   92 |         break;
   93 |       case FLUSH:
   94 |         result = sqlSession.flushStatements();
   95 |         break;
   96 |       default:
   97 |         throw new BindingException("Unknown execution method for: " + command.getName());
   98 |     }
   99 |     if (result == null && method.getReturnType().isPrimitive() && !method.returnsVoid()) {
  100 |       throw new BindingException("Mapper method '" + command.getName()
  101 |           + "' attempted to return null from a method with a primitive return type (" + method.getReturnType() + ").");
  102 |     }
  103 |     return result;
  104 |   }
  105 | 
  106 |   private Object rowCountResult(int rowCount) {
  107 |     final Object result;
  108 |     if (method.returnsVoid()) {
  109 |       result = null;
  110 |     } else if (Integer.class.equals(method.getReturnType()) || Integer.TYPE.equals(method.getReturnType())) {
  111 |       result = rowCount;
  112 |     } else if (Long.class.equals(method.getReturnType()) || Long.TYPE.equals(method.getReturnType())) {
  113 |       result = (long) rowCount;
  114 |     } else if (Boolean.class.equals(method.getReturnType()) || Boolean.TYPE.equals(method.getReturnType())) {
  115 |       result = rowCount > 0;
  116 |     } else {
  117 |       throw new BindingException(
  118 |           "Mapper method '" + command.getName() + "' has an unsupported return type: " + method.getReturnType());
  119 |     }
  120 |     return result;
  121 |   }
  122 | 
  123 |   private void executeWithResultHandler(SqlSession sqlSession, Object[] args) {
  124 |     MappedStatement ms = sqlSession.getConfiguration().getMappedStatement(command.getName());
  125 |     if (!StatementType.CALLABLE.equals(ms.getStatementType())
  126 |         && void.class.equals(ms.getResultMaps().get(0).getType())) {
  127 |       throw new BindingException(
  128 |           "method " + command.getName() + " needs either a @ResultMap annotation, a @ResultType annotation,"
  129 |               + " or a resultType attribute in XML so a ResultHandler can be used as a parameter.");
  130 |     }
  131 |     Object param = method.convertArgsToSqlCommandParam(args);
  132 |     if (method.hasRowBounds()) {
  133 |       RowBounds rowBounds = method.extractRowBounds(args);
  134 |       sqlSession.select(command.getName(), param, rowBounds, method.extractResultHandler(args));
  135 |     } else {
  136 |       sqlSession.select(command.getName(), param, method.extractResultHandler(args));
  137 |     }
  138 |   }
  139 | 
  140 |   private <E> Object executeForMany(SqlSession sqlSession, Object[] args) {
  141 |     List<E> result;
  142 |     Object param = method.convertArgsToSqlCommandParam(args);
  143 |     if (method.hasRowBounds()) {
  144 |       RowBounds rowBounds = method.extractRowBounds(args);
  145 |       result = sqlSession.selectList(command.getName(), param, rowBounds);
  146 |     } else {
  147 |       result = sqlSession.selectList(command.getName(), param);
  148 |     }
  149 |     // issue #510 Collections & arrays support
  150 |     if (!method.getReturnType().isAssignableFrom(result.getClass())) {
  151 |       if (method.getReturnType().isArray()) {
  152 |         return convertToArray(result);
  153 |       }
  154 |       return convertToDeclaredCollection(sqlSession.getConfiguration(), result);
  155 |     }
  156 |     return result;
  157 |   }
  158 | 
  159 |   private <T> Cursor<T> executeForCursor(SqlSession sqlSession, Object[] args) {
  160 |     Cursor<T> result;
  161 |     Object param = method.convertArgsToSqlCommandParam(args);
  162 |     if (method.hasRowBounds()) {
  163 |       RowBounds rowBounds = method.extractRowBounds(args);
  164 |       result = sqlSession.selectCursor(command.getName(), param, rowBounds);
  165 |     } else {
  166 |       result = sqlSession.selectCursor(command.getName(), param);
  167 |     }
  168 |     return result;
  169 |   }
  170 | 
  171 |   private <E> Object convertToDeclaredCollection(Configuration config, List<E> list) {
  172 |     Object collection = config.getObjectFactory().create(method.getReturnType());
  173 |     MetaObject metaObject = config.newMetaObject(collection);
  174 |     metaObject.addAll(list);
  175 |     return collection;
  176 |   }
  177 | 
  178 |   @SuppressWarnings("unchecked")
  179 |   private <E> Object convertToArray(List<E> list) {
  180 |     Class<?> arrayComponentType = method.getReturnType().getComponentType();
  181 |     Object array = Array.newInstance(arrayComponentType, list.size());
  182 |     if (!arrayComponentType.isPrimitive()) {
  183 |       return list.toArray((E[]) array);
  184 |     }
  185 |     for (int i = 0; i < list.size(); i++) {
  186 |       Array.set(array, i, list.get(i));
  187 |     }
  188 |     return array;
  189 |   }
  190 | 
  191 |   private <K, V> Map<K, V> executeForMap(SqlSession sqlSession, Object[] args) {
  192 |     Map<K, V> result;
  193 |     Object param = method.convertArgsToSqlCommandParam(args);
  194 |     if (method.hasRowBounds()) {
  195 |       RowBounds rowBounds = method.extractRowBounds(args);
  196 |       result = sqlSession.selectMap(command.getName(), param, method.getMapKey(), rowBounds);
  197 |     } else {
  198 |       result = sqlSession.selectMap(command.getName(), param, method.getMapKey());
  199 |     }
  200 |     return result;
  201 |   }
  202 | 
  203 |   public static class ParamMap<V> extends HashMap<String, V> {
  204 | 
  205 |     private static final long serialVersionUID = -2212268410512043556L;
  206 | 
  207 |     @Override
  208 |     public V get(Object key) {
  209 |       if (!super.containsKey(key)) {
  210 |         throw new BindingException("Parameter '" + key + "' not found. Available parameters are " + keySet());
  211 |       }
  212 |       return super.get(key);
  213 |     }
  214 | 
  215 |   }
  216 | 
  217 |   public static class SqlCommand {
  218 | 
  219 |     private final String name;
  220 |     private final SqlCommandType type;
  221 | 
  222 |     public SqlCommand(Configuration configuration, Class<?> mapperInterface, Method method) {
  223 |       final String methodName = method.getName();
  224 |       final Class<?> declaringClass = method.getDeclaringClass();
  225 |       MappedStatement ms = resolveMappedStatement(mapperInterface, methodName, declaringClass, configuration);
  226 |       if (ms == null) {
  227 |         if (method.getAnnotation(Flush.class) == null) {
  228 |           throw new BindingException(
  229 |               "Invalid bound statement (not found): " + mapperInterface.getName() + "." + methodName);
  230 |         }
  231 |         name = null;
  232 |         type = SqlCommandType.FLUSH;
  233 |       } else {
  234 |         name = ms.getId();
  235 |         type = ms.getSqlCommandType();
  236 |         if (type == SqlCommandType.UNKNOWN) {
  237 |           throw new BindingException("Unknown execution method for: " + name);
  238 |         }
  239 |       }
  240 |     }
  241 | 
  242 |     public String getName() {
  243 |       return name;
  244 |     }
  245 | 
  246 |     public SqlCommandType getType() {
  247 |       return type;
  248 |     }
  249 | 
  250 |     private MappedStatement resolveMappedStatement(Class<?> mapperInterface, String methodName, Class<?> declaringClass,
  251 |         Configuration configuration) {
  252 |       String statementId = mapperInterface.getName() + "." + methodName;
  253 |       if (configuration.hasStatement(statementId)) {
  254 |         return configuration.getMappedStatement(statementId);
  255 |       }
  256 |       if (mapperInterface.equals(declaringClass)) {
  257 |         return null;
  258 |       }
  259 |       for (Class<?> superInterface : mapperInterface.getInterfaces()) {
  260 |         if (declaringClass.isAssignableFrom(superInterface)) {
  261 |           MappedStatement ms = resolveMappedStatement(superInterface, methodName, declaringClass, configuration);
  262 |           if (ms != null) {
  263 |             return ms;
  264 |           }
  265 |         }
  266 |       }
  267 |       return null;
  268 |     }
  269 |   }
  270 | 
  271 |   public static class MethodSignature {
  272 | 
  273 |     private final boolean returnsMany;
  274 |     private final boolean returnsMap;
  275 |     private final boolean returnsVoid;
  276 |     private final boolean returnsCursor;
  277 |     private final boolean returnsOptional;
  278 |     private final Class<?> returnType;
  279 |     private final String mapKey;
  280 |     private final Integer resultHandlerIndex;
  281 |     private final Integer rowBoundsIndex;
  282 |     private final ParamNameResolver paramNameResolver;
  283 | 
  284 |     public MethodSignature(Configuration configuration, Class<?> mapperInterface, Method method) {
  285 |       Type resolvedReturnType = TypeParameterResolver.resolveReturnType(method, mapperInterface);
  286 |       if (resolvedReturnType instanceof Class<?>) {
  287 |         this.returnType = (Class<?>) resolvedReturnType;
  288 |       } else if (resolvedReturnType instanceof ParameterizedType) {
  289 |         this.returnType = (Class<?>) ((ParameterizedType) resolvedReturnType).getRawType();
  290 |       } else {
  291 |         this.returnType = method.getReturnType();
  292 |       }
  293 |       this.returnsVoid = void.class.equals(this.returnType);
  294 |       this.returnsMany = configuration.getObjectFactory().isCollection(this.returnType) || this.returnType.isArray();
  295 |       this.returnsCursor = Cursor.class.equals(this.returnType);
  296 |       this.returnsOptional = Optional.class.equals(this.returnType);
  297 |       this.mapKey = getMapKey(method);
  298 |       this.returnsMap = this.mapKey != null;
  299 |       this.rowBoundsIndex = getUniqueParamIndex(method, RowBounds.class);
  300 |       this.resultHandlerIndex = getUniqueParamIndex(method, ResultHandler.class);
  301 |       this.paramNameResolver = new ParamNameResolver(configuration, method, mapperInterface);
  302 |     }
  303 | 
  304 |     public Object convertArgsToSqlCommandParam(Object[] args) {
  305 |       return paramNameResolver.getNamedParams(args);
  306 |     }
  307 | 
  308 |     public boolean hasRowBounds() {
  309 |       return rowBoundsIndex != null;
  310 |     }
  311 | 
  312 |     public RowBounds extractRowBounds(Object[] args) {
  313 |       return hasRowBounds() ? (RowBounds) args[rowBoundsIndex] : null;
  314 |     }
  315 | 
  316 |     public boolean hasResultHandler() {
  317 |       return resultHandlerIndex != null;
  318 |     }
  319 | 
  320 |     public ResultHandler extractResultHandler(Object[] args) {
  321 |       return hasResultHandler() ? (ResultHandler) args[resultHandlerIndex] : null;
  322 |     }
  323 | 
  324 |     public Class<?> getReturnType() {
  325 |       return returnType;
  326 |     }
  327 | 
  328 |     public boolean returnsMany() {
  329 |       return returnsMany;
  330 |     }
  331 | 
  332 |     public boolean returnsMap() {
  333 |       return returnsMap;
  334 |     }
  335 | 
  336 |     public boolean returnsVoid() {
  337 |       return returnsVoid;
  338 |     }
  339 | 
  340 |     public boolean returnsCursor() {
  341 |       return returnsCursor;
  342 |     }
  343 | 
  344 |     /**
  345 |      * return whether return type is {@code java.util.Optional}.
  346 |      *
  347 |      * @return return {@code true}, if return type is {@code java.util.Optional}
  348 |      *
  349 |      * @since 3.5.0
  350 |      */
  351 |     public boolean returnsOptional() {
  352 |       return returnsOptional;
  353 |     }
  354 | 
  355 |     private Integer getUniqueParamIndex(Method method, Class<?> paramType) {
  356 |       Integer index = null;
  357 |       final Class<?>[] argTypes = method.getParameterTypes();
  358 |       for (int i = 0; i < argTypes.length; i++) {
  359 |         if (paramType.isAssignableFrom(argTypes[i])) {
  360 |           if (index != null) {
  361 |             throw new BindingException(
  362 |                 method.getName() + " cannot have multiple " + paramType.getSimpleName() + " parameters");
  363 |           }
  364 |           index = i;
  365 |         }
  366 |       }
  367 |       return index;
  368 |     }
  369 | 
  370 |     public String getMapKey() {
  371 |       return mapKey;
  372 |     }
  373 | 
  374 |     private String getMapKey(Method method) {
  375 |       String mapKey = null;
  376 |       if (Map.class.isAssignableFrom(method.getReturnType())) {
  377 |         final MapKey mapKeyAnnotation = method.getAnnotation(MapKey.class);
  378 |         if (mapKeyAnnotation != null) {
  379 |           mapKey = mapKeyAnnotation.value();
  380 |         }
  381 |       }
  382 |       return mapKey;
  383 |     }
  384 |   }
  385 | 
  386 | }
```
