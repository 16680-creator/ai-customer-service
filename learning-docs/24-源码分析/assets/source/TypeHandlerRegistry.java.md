<!-- learning-docs source archive（机械抓取原文，禁止手改代码内容） -->
<!-- origin: org.mybatis:mybatis:3.6.0 | path: org/apache/ibatis/type/TypeHandlerRegistry.java -->
<!-- 正文引用：[[源码:TypeHandlerRegistry.java#L起-L止]] ；代码块头部：(src: TypeHandlerRegistry.java#L起-L止) -->

# TypeHandlerRegistry.java

| 项 | 值 |
| --- | --- |
| 来源 | `org.mybatis:mybatis:3.6.0` |
| 路径 | `org/apache/ibatis/type/TypeHandlerRegistry.java` |
| 行数 | 565 |
| 在线地址 | https://repo1.maven.org/maven2/org/mybatis/mybatis/3.6.0/mybatis-3.6.0-sources.jar |
| 抓取时间 | 2026-11-25T14:59:19.084Z |

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
   16 | package org.apache.ibatis.type;
   17 | 
   18 | import java.io.InputStream;
   19 | import java.io.Reader;
   20 | import java.lang.reflect.Constructor;
   21 | import java.lang.reflect.Modifier;
   22 | import java.lang.reflect.ParameterizedType;
   23 | import java.lang.reflect.Type;
   24 | import java.math.BigDecimal;
   25 | import java.math.BigInteger;
   26 | import java.sql.Time;
   27 | import java.sql.Timestamp;
   28 | import java.time.Instant;
   29 | import java.time.LocalDate;
   30 | import java.time.LocalDateTime;
   31 | import java.time.LocalTime;
   32 | import java.time.Month;
   33 | import java.time.OffsetDateTime;
   34 | import java.time.OffsetTime;
   35 | import java.time.Year;
   36 | import java.time.YearMonth;
   37 | import java.time.ZonedDateTime;
   38 | import java.time.chrono.JapaneseDate;
   39 | import java.util.Arrays;
   40 | import java.util.Collection;
   41 | import java.util.Collections;
   42 | import java.util.Date;
   43 | import java.util.EnumMap;
   44 | import java.util.HashMap;
   45 | import java.util.Map;
   46 | import java.util.Map.Entry;
   47 | import java.util.Set;
   48 | import java.util.concurrent.ConcurrentHashMap;
   49 | 
   50 | import org.apache.ibatis.binding.MapperMethod.ParamMap;
   51 | import org.apache.ibatis.io.ResolverUtil;
   52 | import org.apache.ibatis.io.Resources;
   53 | import org.apache.ibatis.reflection.TypeParameterResolver;
   54 | import org.apache.ibatis.session.Configuration;
   55 | import org.gaul.modernizer_maven_annotations.SuppressModernizer;
   56 | 
   57 | /**
   58 |  * @author Clinton Begin
   59 |  * @author Kazuki Shimizu
   60 |  */
   61 | public final class TypeHandlerRegistry {
   62 | 
   63 |   private final Map<JdbcType, TypeHandler<?>> jdbcTypeHandlerMap = new EnumMap<>(JdbcType.class);
   64 |   private final Map<Type, Map<JdbcType, TypeHandler<?>>> typeHandlerMap = new ConcurrentHashMap<>();
   65 |   private final ConcurrentHashMap<Type, Constructor<?>> smartHandlers = new ConcurrentHashMap<>();
   66 |   private final Map<Class<?>, TypeHandler<?>> allTypeHandlersMap = new HashMap<>();
   67 | 
   68 |   private static final Map<JdbcType, TypeHandler<?>> NULL_TYPE_HANDLER_MAP = createNullTypeHandlerMap();
   69 | 
   70 |   // Map.of() does not permit null keys; this map is intentionally queried with null JdbcType
   71 |   @SuppressModernizer
   72 |   private static Map<JdbcType, TypeHandler<?>> createNullTypeHandlerMap() {
   73 |     return Collections.emptyMap();
   74 |   }
   75 | 
   76 |   @SuppressWarnings("rawtypes")
   77 |   private Class<? extends TypeHandler> defaultEnumTypeHandler = EnumTypeHandler.class;
   78 | 
   79 |   /**
   80 |    * The default constructor.
   81 |    */
   82 |   public TypeHandlerRegistry() {
   83 |     this(new Configuration());
   84 |   }
   85 | 
   86 |   /**
   87 |    * The constructor that pass the MyBatis configuration.
   88 |    *
   89 |    * @param configuration
   90 |    *          a MyBatis configuration
   91 |    *
   92 |    * @since 3.5.4
   93 |    */
   94 |   public TypeHandlerRegistry(Configuration configuration) {
   95 |     // If a handler is registered against null JDBC type, it is the default handler for the Java type. Users can
   96 |     // override the default handler (e.g. `register(boolean.class, null, new YNBooleanTypeHandler())` or register a
   97 |     // custom handler for a specific Java-JDBC type combination (e.g. `register(boolean.class, JdbcType.CHAR, new
   98 |     // YNBooleanTypeHandler())`).
   99 |     register(new Type[] { Boolean.class, boolean.class }, new JdbcType[] { null }, BooleanTypeHandler.INSTANCE);
  100 |     register(new Type[] { Byte.class, byte.class }, new JdbcType[] { null }, ByteTypeHandler.INSTANCE);
  101 |     register(new Type[] { Short.class, short.class }, new JdbcType[] { null }, ShortTypeHandler.INSTANCE);
  102 |     register(new Type[] { Integer.class, int.class }, new JdbcType[] { null }, IntegerTypeHandler.INSTANCE);
  103 |     register(new Type[] { Long.class, long.class }, new JdbcType[] { null }, LongTypeHandler.INSTANCE);
  104 |     register(new Type[] { Float.class, float.class }, new JdbcType[] { null }, FloatTypeHandler.INSTANCE);
  105 |     register(new Type[] { Double.class, double.class }, new JdbcType[] { null }, DoubleTypeHandler.INSTANCE);
  106 |     register(new Type[] { Character.class, char.class }, new JdbcType[] { null }, new CharacterTypeHandler());
  107 |     register(String.class, null, StringTypeHandler.INSTANCE);
  108 |     register(Reader.class, null, new ClobReaderTypeHandler());
  109 |     register(BigInteger.class, null, new BigIntegerTypeHandler());
  110 |     register(BigDecimal.class, null, BigDecimalTypeHandler.INSTANCE);
  111 |     register(InputStream.class, null, new BlobInputStreamTypeHandler());
  112 |     register(Byte[].class, null, new ByteObjectArrayTypeHandler());
  113 |     register(byte[].class, null, ByteArrayTypeHandler.INSTANCE);
  114 |     register(Date.class, null, DateTypeHandler.INSTANCE);
  115 |     register(java.sql.Date.class, null, new SqlDateTypeHandler());
  116 |     register(Time.class, null, new SqlTimeTypeHandler());
  117 |     register(Timestamp.class, null, new SqlTimestampTypeHandler());
  118 |     register(Instant.class, null, new InstantTypeHandler());
  119 |     register(LocalDateTime.class, null, new LocalDateTimeTypeHandler());
  120 |     register(LocalDate.class, null, new LocalDateTypeHandler());
  121 |     register(LocalTime.class, null, new LocalTimeTypeHandler());
  122 |     register(OffsetDateTime.class, null, new OffsetDateTimeTypeHandler());
  123 |     register(OffsetTime.class, null, new OffsetTimeTypeHandler());
  124 |     register(ZonedDateTime.class, null, new ZonedDateTimeTypeHandler());
  125 |     register(Month.class, null, new MonthTypeHandler());
  126 |     register(Year.class, null, new YearTypeHandler());
  127 |     register(YearMonth.class, null, new YearMonthTypeHandler());
  128 |     register(JapaneseDate.class, null, new JapaneseDateTypeHandler());
  129 | 
  130 |     // These type handlers are used only for specific combinations of Java type and JDBC type.
  131 |     register(String.class, JdbcType.CLOB, ClobTypeHandler.INSTANCE);
  132 |     register(String.class, JdbcType.NCLOB, NClobTypeHandler.INSTANCE);
  133 |     register(new Type[] { String.class }, new JdbcType[] { JdbcType.NCHAR, JdbcType.NVARCHAR, JdbcType.LONGNVARCHAR },
  134 |         NStringTypeHandler.INSTANCE);
  135 |     register(new Type[] { Byte[].class }, new JdbcType[] { JdbcType.BLOB, JdbcType.LONGVARBINARY },
  136 |         new BlobByteObjectArrayTypeHandler());
  137 |     register(new Type[] { byte[].class }, new JdbcType[] { JdbcType.BLOB, JdbcType.LONGVARBINARY },
  138 |         BlobTypeHandler.INSTANCE);
  139 |     register(Date.class, JdbcType.DATE, DateOnlyTypeHandler.INSTANCE);
  140 |     register(Date.class, JdbcType.TIME, TimeOnlyTypeHandler.INSTANCE);
  141 |     register(String.class, JdbcType.SQLXML, new SqlxmlTypeHandler());
  142 | 
  143 |     // Type handlers in the `jdbcTypeHandlerMap` are used when Java type is unknown or
  144 |     // as a last resort when no matching handler is found for the target Java type.
  145 |     // It is also used in some internal purposes like creating cache keys.
  146 |     // Although it is possible for users to override these mappings via register(JdbcType, TypeHandler),
  147 |     // it might have unexpected side-effect.
  148 |     // To configure type handlers for mapping to Map, for example, it is recommended to call the 3-args
  149 |     // version of register method. e.g. register(Object.class, JdbcType.DATE, new DateTypeHandler())
  150 |     jdbcTypeHandlerMap.put(JdbcType.BOOLEAN, BooleanTypeHandler.INSTANCE);
  151 |     jdbcTypeHandlerMap.put(JdbcType.BIT, BooleanTypeHandler.INSTANCE);
  152 |     jdbcTypeHandlerMap.put(JdbcType.TINYINT, ByteTypeHandler.INSTANCE);
  153 |     jdbcTypeHandlerMap.put(JdbcType.SMALLINT, ShortTypeHandler.INSTANCE);
  154 |     jdbcTypeHandlerMap.put(JdbcType.INTEGER, IntegerTypeHandler.INSTANCE);
  155 |     jdbcTypeHandlerMap.put(JdbcType.BIGINT, LongTypeHandler.INSTANCE);
  156 |     jdbcTypeHandlerMap.put(JdbcType.REAL, FloatTypeHandler.INSTANCE); // As per JDBC spec
  157 |     jdbcTypeHandlerMap.put(JdbcType.FLOAT, DoubleTypeHandler.INSTANCE); // As per JDBC spec
  158 |     jdbcTypeHandlerMap.put(JdbcType.DOUBLE, DoubleTypeHandler.INSTANCE);
  159 |     jdbcTypeHandlerMap.put(JdbcType.DECIMAL, BigDecimalTypeHandler.INSTANCE);
  160 |     jdbcTypeHandlerMap.put(JdbcType.NUMERIC, BigDecimalTypeHandler.INSTANCE);
  161 |     jdbcTypeHandlerMap.put(JdbcType.CHAR, StringTypeHandler.INSTANCE);
  162 |     jdbcTypeHandlerMap.put(JdbcType.VARCHAR, StringTypeHandler.INSTANCE);
  163 |     jdbcTypeHandlerMap.put(JdbcType.LONGVARCHAR, StringTypeHandler.INSTANCE);
  164 |     jdbcTypeHandlerMap.put(JdbcType.CLOB, ClobTypeHandler.INSTANCE);
  165 |     jdbcTypeHandlerMap.put(JdbcType.NVARCHAR, NStringTypeHandler.INSTANCE);
  166 |     jdbcTypeHandlerMap.put(JdbcType.NCHAR, NStringTypeHandler.INSTANCE);
  167 |     jdbcTypeHandlerMap.put(JdbcType.LONGNVARCHAR, NStringTypeHandler.INSTANCE);
  168 |     jdbcTypeHandlerMap.put(JdbcType.NCLOB, NClobTypeHandler.INSTANCE);
  169 |     jdbcTypeHandlerMap.put(JdbcType.ARRAY, new ArrayTypeHandler());
  170 |     jdbcTypeHandlerMap.put(JdbcType.BINARY, ByteArrayTypeHandler.INSTANCE);
  171 |     jdbcTypeHandlerMap.put(JdbcType.VARBINARY, ByteArrayTypeHandler.INSTANCE);
  172 |     jdbcTypeHandlerMap.put(JdbcType.LONGVARBINARY, ByteArrayTypeHandler.INSTANCE);
  173 |     jdbcTypeHandlerMap.put(JdbcType.BLOB, BlobTypeHandler.INSTANCE);
  174 |     jdbcTypeHandlerMap.put(JdbcType.TIMESTAMP, DateTypeHandler.INSTANCE);
  175 |     jdbcTypeHandlerMap.put(JdbcType.DATE, DateOnlyTypeHandler.INSTANCE);
  176 |     jdbcTypeHandlerMap.put(JdbcType.TIME, TimeOnlyTypeHandler.INSTANCE);
  177 |   }
  178 | 
  179 |   /**
  180 |    * Set a default {@link TypeHandler} class for {@link Enum}. A default {@link TypeHandler} is
  181 |    * {@link org.apache.ibatis.type.EnumTypeHandler}.
  182 |    *
  183 |    * @param typeHandler
  184 |    *          a type handler class for {@link Enum}
  185 |    *
  186 |    * @since 3.4.5
  187 |    */
  188 |   public void setDefaultEnumTypeHandler(@SuppressWarnings("rawtypes") Class<? extends TypeHandler> typeHandler) {
  189 |     this.defaultEnumTypeHandler = typeHandler;
  190 |   }
  191 | 
  192 |   public boolean hasTypeHandler(Type javaType) {
  193 |     return hasTypeHandler(javaType, null);
  194 |   }
  195 | 
  196 |   @Deprecated(since = "3.6.0", forRemoval = true)
  197 |   public boolean hasTypeHandler(TypeReference<?> javaTypeReference) {
  198 |     return hasTypeHandler(javaTypeReference, null);
  199 |   }
  200 | 
  201 |   public boolean hasTypeHandler(Type javaType, JdbcType jdbcType) {
  202 |     return javaType != null && getTypeHandler(javaType, jdbcType) != null;
  203 |   }
  204 | 
  205 |   @Deprecated(since = "3.6.0", forRemoval = true)
  206 |   public boolean hasTypeHandler(TypeReference<?> javaTypeReference, JdbcType jdbcType) {
  207 |     return javaTypeReference != null && getTypeHandler(javaTypeReference, jdbcType) != null;
  208 |   }
  209 | 
  210 |   @Deprecated(since = "3.6.0", forRemoval = true)
  211 |   public TypeHandler<?> getMappingTypeHandler(Class<? extends TypeHandler<?>> handlerType) {
  212 |     return allTypeHandlersMap.get(handlerType);
  213 |   }
  214 | 
  215 |   public TypeHandler<?> getTypeHandler(Type type) {
  216 |     return getTypeHandler(type, null);
  217 |   }
  218 | 
  219 |   @Deprecated(since = "3.6.0", forRemoval = true)
  220 |   public <T> TypeHandler<T> getTypeHandler(TypeReference<T> javaTypeReference) {
  221 |     return getTypeHandler(javaTypeReference, null);
  222 |   }
  223 | 
  224 |   public TypeHandler<?> getTypeHandler(JdbcType jdbcType) {
  225 |     return jdbcTypeHandlerMap.get(jdbcType);
  226 |   }
  227 | 
  228 |   @SuppressWarnings("unchecked")
  229 |   @Deprecated(since = "3.6.0", forRemoval = true)
  230 |   public <T> TypeHandler<T> getTypeHandler(TypeReference<T> javaTypeReference, JdbcType jdbcType) {
  231 |     return (TypeHandler<T>) getTypeHandler(javaTypeReference.getRawType(), jdbcType);
  232 |   }
  233 | 
  234 |   public TypeHandler<?> getTypeHandler(Type type, JdbcType jdbcType, Class<? extends TypeHandler<?>> typeHandlerClass) {
  235 |     TypeHandler<?> typeHandler = getTypeHandler(type, jdbcType);
  236 |     if (typeHandler != null && (typeHandlerClass == null || typeHandler.getClass().equals(typeHandlerClass))) {
  237 |       return typeHandler;
  238 |     }
  239 |     if (typeHandlerClass == null) {
  240 |       typeHandler = getSmartHandler(type, jdbcType);
  241 |     } else {
  242 |       typeHandler = getMappingTypeHandler(typeHandlerClass);
  243 |       if (typeHandler == null) {
  244 |         typeHandler = getInstance(type, typeHandlerClass);
  245 |       }
  246 |     }
  247 |     return typeHandler;
  248 |   }
  249 | 
  250 |   public TypeHandler<?> getTypeHandler(Type type, JdbcType jdbcType) {
  251 |     if (ParamMap.class.equals(type)) {
  252 |       return null;
  253 |     } else if (type == null) {
  254 |       return getTypeHandler(jdbcType);
  255 |     }
  256 | 
  257 |     TypeHandler<?> handler = null;
  258 |     Map<JdbcType, TypeHandler<?>> jdbcHandlerMap = getJdbcHandlerMap(type);
  259 | 
  260 |     if (Object.class.equals(type)) {
  261 |       if (jdbcHandlerMap != null) {
  262 |         handler = jdbcHandlerMap.get(jdbcType);
  263 |       }
  264 |       return handler;
  265 |     }
  266 | 
  267 |     if (jdbcHandlerMap != null) {
  268 |       handler = jdbcHandlerMap.get(jdbcType);
  269 |       if (handler == null) {
  270 |         handler = jdbcHandlerMap.get(null);
  271 |       }
  272 |       if (handler == null) {
  273 |         // #591
  274 |         handler = pickSoleHandler(jdbcHandlerMap);
  275 |       }
  276 |     }
  277 |     if (handler == null) {
  278 |       handler = getSmartHandler(type, jdbcType);
  279 |     }
  280 |     if (handler == null && type instanceof ParameterizedType) {
  281 |       handler = getTypeHandler((Class<?>) ((ParameterizedType) type).getRawType(), jdbcType);
  282 |     }
  283 |     return handler;
  284 |   }
  285 | 
  286 |   private TypeHandler<?> getSmartHandler(Type type, JdbcType jdbcType) {
  287 |     Constructor<?> candidate = null;
  288 | 
  289 |     for (Entry<Type, Constructor<?>> entry : smartHandlers.entrySet()) {
  290 |       Type registeredType = entry.getKey();
  291 |       if (registeredType.equals(type)) {
  292 |         candidate = entry.getValue();
  293 |         break;
  294 |       }
  295 |       if (registeredType instanceof Class) {
  296 |         if (type instanceof Class && ((Class<?>) registeredType).isAssignableFrom((Class<?>) type)) {
  297 |           candidate = entry.getValue();
  298 |         }
  299 |       } else if (registeredType instanceof ParameterizedType) {
  300 |         Class<?> registeredClass = (Class<?>) ((ParameterizedType) registeredType).getRawType();
  301 |         if (type instanceof ParameterizedType) {
  302 |           Class<?> clazz = (Class<?>) ((ParameterizedType) type).getRawType();
  303 |           if (registeredClass.isAssignableFrom(clazz)) {
  304 |             candidate = entry.getValue();
  305 |           }
  306 |         }
  307 |       }
  308 |     }
  309 | 
  310 |     if (candidate == null) {
  311 |       if (type instanceof Class) {
  312 |         Class<?> clazz = (Class<?>) type;
  313 |         if (Enum.class.isAssignableFrom(clazz)) {
  314 |           Class<?> enumClass = (clazz.isAnonymousClass() || !clazz.isEnum()) ? clazz.getSuperclass() : clazz;
  315 |           TypeHandler<?> enumHandler = getInstance(enumClass, defaultEnumTypeHandler);
  316 |           register(new Type[] { enumClass }, new JdbcType[] { jdbcType }, enumHandler);
  317 |           return enumHandler;
  318 |         }
  319 |       }
  320 |       return null;
  321 |     }
  322 | 
  323 |     try {
  324 |       TypeHandler<?> typeHandler = (TypeHandler<?>) candidate.newInstance(type);
  325 |       register(type, jdbcType, typeHandler);
  326 |       return typeHandler;
  327 |     } catch (ReflectiveOperationException e) {
  328 |       throw new TypeException("Failed to invoke constructor " + candidate.toString(), e);
  329 |     }
  330 |   }
  331 | 
  332 |   private Map<JdbcType, TypeHandler<?>> getJdbcHandlerMap(Type type) {
  333 |     Map<JdbcType, TypeHandler<?>> jdbcHandlerMap = typeHandlerMap.get(type);
  334 |     if (jdbcHandlerMap != null) {
  335 |       return NULL_TYPE_HANDLER_MAP.equals(jdbcHandlerMap) ? null : jdbcHandlerMap;
  336 |     }
  337 |     if (type instanceof Class) {
  338 |       Class<?> clazz = (Class<?>) type;
  339 |       if (!Enum.class.isAssignableFrom(clazz)) {
  340 |         jdbcHandlerMap = getJdbcHandlerMapForSuperclass(clazz);
  341 |       }
  342 |     }
  343 |     typeHandlerMap.put(type, jdbcHandlerMap == null ? NULL_TYPE_HANDLER_MAP : jdbcHandlerMap);
  344 |     return jdbcHandlerMap;
  345 |   }
  346 | 
  347 |   private Map<JdbcType, TypeHandler<?>> getJdbcHandlerMapForSuperclass(Class<?> clazz) {
  348 |     Class<?> superclass = clazz.getSuperclass();
  349 |     if (superclass == null || Object.class.equals(superclass)) {
  350 |       return null;
  351 |     }
  352 |     Map<JdbcType, TypeHandler<?>> jdbcHandlerMap = typeHandlerMap.get(superclass);
  353 |     if (jdbcHandlerMap != null) {
  354 |       return jdbcHandlerMap;
  355 |     }
  356 |     return getJdbcHandlerMapForSuperclass(superclass);
  357 |   }
  358 | 
  359 |   private TypeHandler<?> pickSoleHandler(Map<JdbcType, TypeHandler<?>> jdbcHandlerMap) {
  360 |     TypeHandler<?> soleHandler = null;
  361 |     for (TypeHandler<?> handler : jdbcHandlerMap.values()) {
  362 |       if (soleHandler == null) {
  363 |         soleHandler = handler;
  364 |       } else if (!handler.getClass().equals(soleHandler.getClass())) {
  365 |         // More than one type handlers registered.
  366 |         return null;
  367 |       }
  368 |     }
  369 |     return soleHandler;
  370 |   }
  371 | 
  372 |   public void register(JdbcType mappedJdbcType, TypeHandler<?> handler) {
  373 |     jdbcTypeHandlerMap.put(mappedJdbcType, handler);
  374 |   }
  375 | 
  376 |   //
  377 |   // REGISTER INSTANCE
  378 |   //
  379 | 
  380 |   // Only handler
  381 | 
  382 |   public <T> void register(TypeHandler<T> handler) {
  383 |     register(mappedJavaTypes(handler.getClass()), mappedJdbcTypes(handler.getClass()), handler);
  384 |   }
  385 | 
  386 |   // java type + handler
  387 | 
  388 |   public void register(Class<?> mappedJavaType, TypeHandler<?> handler) {
  389 |     register((Type) mappedJavaType, handler);
  390 |   }
  391 | 
  392 |   private void register(Type mappedJavaType, TypeHandler<?> handler) {
  393 |     register(new Type[] { mappedJavaType }, mappedJdbcTypes(handler.getClass()), handler);
  394 |   }
  395 | 
  396 |   @Deprecated(since = "3.6.0", forRemoval = true)
  397 |   public <T> void register(TypeReference<T> javaTypeReference, TypeHandler<? extends T> handler) {
  398 |     register(javaTypeReference.getRawType(), handler);
  399 |   }
  400 | 
  401 |   // java type + jdbc type + handler
  402 | 
  403 |   public void register(Type mappedJavaType, JdbcType mappedJdbcType, TypeHandler<?> handler) {
  404 |     register(new Type[] { mappedJavaType }, new JdbcType[] { mappedJdbcType }, handler);
  405 |   }
  406 | 
  407 |   private void register(Type[] mappedJavaTypes, JdbcType[] mappedJdbcTypes, TypeHandler<?> handler) {
  408 |     for (Type javaType : mappedJavaTypes) {
  409 |       if (javaType == null) {
  410 |         continue;
  411 |       }
  412 | 
  413 |       typeHandlerMap.compute(javaType, (k, v) -> {
  414 |         Map<JdbcType, TypeHandler<?>> map = (v == null || v == NULL_TYPE_HANDLER_MAP ? new HashMap<>() : v);
  415 |         for (JdbcType jdbcType : mappedJdbcTypes) {
  416 |           map.put(jdbcType, handler);
  417 |         }
  418 |         return map;
  419 |       });
  420 | 
  421 |       if (javaType instanceof ParameterizedType) {
  422 |         // MEMO: add annotation to skip this?
  423 |         Type rawType = ((ParameterizedType) javaType).getRawType();
  424 |         typeHandlerMap.compute(rawType, (k, v) -> {
  425 |           Map<JdbcType, TypeHandler<?>> map = (v == null || v == NULL_TYPE_HANDLER_MAP ? new HashMap<>() : v);
  426 |           for (JdbcType jdbcType : mappedJdbcTypes) {
  427 |             map.merge(jdbcType, handler, (handler1, handler2) -> handler1.equals(handler2) ? handler1
  428 |                 : new ConflictedTypeHandler((Class<?>) rawType, jdbcType, handler1, handler2));
  429 |           }
  430 |           return map;
  431 |         });
  432 |       }
  433 |     }
  434 | 
  435 |     allTypeHandlersMap.put(handler.getClass(), handler);
  436 |   }
  437 | 
  438 |   //
  439 |   // REGISTER CLASS
  440 |   //
  441 | 
  442 |   // Only handler type
  443 | 
  444 |   public void register(Class<?> handlerClass) {
  445 |     register(mappedJavaTypes(handlerClass), mappedJdbcTypes(handlerClass), handlerClass);
  446 |   }
  447 | 
  448 |   // java type + handler type
  449 | 
  450 |   @Deprecated(since = "3.6.0", forRemoval = true)
  451 |   public void register(String javaTypeClassName, String typeHandlerClassName) throws ClassNotFoundException {
  452 |     register(Resources.classForName(javaTypeClassName), Resources.classForName(typeHandlerClassName));
  453 |   }
  454 | 
  455 |   public void register(Type mappedJavaType, Class<?> handlerClass) {
  456 |     register(new Type[] { mappedJavaType }, mappedJdbcTypes(handlerClass), handlerClass);
  457 |   }
  458 | 
  459 |   // java type + jdbc type + handler type
  460 | 
  461 |   public void register(Type mappedJavaType, JdbcType mappedJdbcType, Class<?> handlerClass) {
  462 |     register(new Type[] { mappedJavaType }, new JdbcType[] { mappedJdbcType }, handlerClass);
  463 |   }
  464 | 
  465 |   private void register(Type[] mappedJavaTypes, JdbcType[] mappedJdbcTypes, Class<?> handlerClass) {
  466 |     if (!TypeHandler.class.isAssignableFrom(handlerClass)) {
  467 |       throw new IllegalArgumentException(String.format("'%s' does not implement TypeHandler.", handlerClass.getName()));
  468 |     }
  469 |     for (Constructor<?> constructor : handlerClass.getConstructors()) {
  470 |       if (constructor.getParameterCount() != 1) {
  471 |         continue;
  472 |       }
  473 |       Class<?> argType = constructor.getParameterTypes()[0];
  474 |       if (Type.class.equals(argType) || Class.class.equals(argType)) {
  475 |         for (Type javaType : mappedJavaTypes) {
  476 |           smartHandlers.computeIfAbsent(javaType, k -> constructor);
  477 |         }
  478 |         return;
  479 |       }
  480 |     }
  481 |     // It is not a smart handler
  482 |     register(mappedJavaTypes, mappedJdbcTypes, getInstance(null, handlerClass));
  483 |   }
  484 | 
  485 |   private Type[] mappedJavaTypes(Class<?> clazz) {
  486 |     MappedTypes mappedTypesAnno = clazz.getAnnotation(MappedTypes.class);
  487 |     if (mappedTypesAnno != null) {
  488 |       return mappedTypesAnno.value();
  489 |     }
  490 |     return TypeParameterResolver.resolveClassTypeParams(TypeHandler.class, clazz);
  491 |   }
  492 | 
  493 |   private JdbcType[] mappedJdbcTypes(Class<?> clazz) {
  494 |     MappedJdbcTypes mappedJdbcTypesAnno = clazz.getAnnotation(MappedJdbcTypes.class);
  495 |     if (mappedJdbcTypesAnno != null) {
  496 |       JdbcType[] jdbcTypes = mappedJdbcTypesAnno.value();
  497 |       if (mappedJdbcTypesAnno.includeNullJdbcType()) {
  498 |         int newLength = jdbcTypes.length + 1;
  499 |         jdbcTypes = Arrays.copyOf(jdbcTypes, newLength);
  500 |         jdbcTypes[newLength - 1] = null;
  501 |       }
  502 |       return jdbcTypes;
  503 |     }
  504 |     return new JdbcType[] { null };
  505 |   }
  506 | 
  507 |   // Construct a handler (used also from Builders)
  508 | 
  509 |   @SuppressWarnings("unchecked")
  510 |   public <T> TypeHandler<T> getInstance(Type javaType, Class<?> handlerClass) {
  511 |     Constructor<?> c;
  512 |     try {
  513 |       if (javaType != null) {
  514 |         try {
  515 |           c = handlerClass.getConstructor(Type.class);
  516 |           return (TypeHandler<T>) c.newInstance(javaType);
  517 |         } catch (NoSuchMethodException ignored) {
  518 |         }
  519 |         if (javaType instanceof Class) {
  520 |           try {
  521 |             c = handlerClass.getConstructor(Class.class);
  522 |             return (TypeHandler<T>) c.newInstance(javaType);
  523 |           } catch (NoSuchMethodException ignored) {
  524 |           }
  525 |         }
  526 |       }
  527 |       try {
  528 |         c = handlerClass.getConstructor();
  529 |         return (TypeHandler<T>) c.newInstance();
  530 |       } catch (NoSuchMethodException e) {
  531 |         throw new TypeException("Unable to find a usable constructor for " + handlerClass, e);
  532 |       }
  533 |     } catch (ReflectiveOperationException e) {
  534 |       throw new TypeException("Failed to invoke constructor for handler " + handlerClass, e);
  535 |     }
  536 |   }
  537 | 
  538 |   // scan
  539 | 
  540 |   public void register(String packageName) {
  541 |     ResolverUtil<Class<?>> resolverUtil = new ResolverUtil<>();
  542 |     resolverUtil.find(new ResolverUtil.IsA(TypeHandler.class), packageName);
  543 |     Set<Class<? extends Class<?>>> handlerSet = resolverUtil.getClasses();
  544 |     for (Class<?> type : handlerSet) {
  545 |       // Ignore inner classes and interfaces (including package-info.java) and abstract classes
  546 |       if (!type.isAnonymousClass() && !type.isInterface() && !Modifier.isAbstract(type.getModifiers())) {
  547 |         register(type);
  548 |       }
  549 |     }
  550 |   }
  551 | 
  552 |   // get information
  553 | 
  554 |   /**
  555 |    * Gets the type handlers. Used by mybatis-guice.
  556 |    *
  557 |    * @return the type handlers
  558 |    *
  559 |    * @since 3.2.2
  560 |    */
  561 |   public Collection<TypeHandler<?>> getTypeHandlers() {
  562 |     return Collections.unmodifiableCollection(allTypeHandlersMap.values());
  563 |   }
  564 | 
  565 | }
```
