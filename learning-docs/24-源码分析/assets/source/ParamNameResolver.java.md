<!-- learning-docs source archive（机械抓取原文，禁止手改代码内容） -->
<!-- origin: org.mybatis:mybatis:3.6.0 | path: org/apache/ibatis/reflection/ParamNameResolver.java -->
<!-- 正文引用：[[源码:ParamNameResolver.java#L起-L止]] ；代码块头部：(src: ParamNameResolver.java#L起-L止) -->

# ParamNameResolver.java

| 项 | 值 |
| --- | --- |
| 来源 | `org.mybatis:mybatis:3.6.0` |
| 路径 | `org/apache/ibatis/reflection/ParamNameResolver.java` |
| 行数 | 244 |
| 在线地址 | https://repo1.maven.org/maven2/org/mybatis/mybatis/3.6.0/mybatis-3.6.0-sources.jar |
| 抓取时间 | 2026-11-25T14:59:18.787Z |

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
   16 | package org.apache.ibatis.reflection;
   17 | 
   18 | import java.lang.annotation.Annotation;
   19 | import java.lang.reflect.GenericArrayType;
   20 | import java.lang.reflect.Method;
   21 | import java.lang.reflect.ParameterizedType;
   22 | import java.lang.reflect.Type;
   23 | import java.util.Collection;
   24 | import java.util.Collections;
   25 | import java.util.HashMap;
   26 | import java.util.List;
   27 | import java.util.Map;
   28 | import java.util.Optional;
   29 | import java.util.SortedMap;
   30 | import java.util.TreeMap;
   31 | 
   32 | import org.apache.ibatis.annotations.Param;
   33 | import org.apache.ibatis.binding.MapperMethod.ParamMap;
   34 | import org.apache.ibatis.reflection.property.PropertyTokenizer;
   35 | import org.apache.ibatis.session.Configuration;
   36 | import org.apache.ibatis.session.ResultHandler;
   37 | import org.apache.ibatis.session.RowBounds;
   38 | 
   39 | public class ParamNameResolver {
   40 | 
   41 |   public static final String GENERIC_NAME_PREFIX = "param";
   42 | 
   43 |   public static final String[] GENERIC_NAME_CACHE = new String[10];
   44 | 
   45 |   static {
   46 |     for (int i = 0; i < 10; i++) {
   47 |       GENERIC_NAME_CACHE[i] = GENERIC_NAME_PREFIX + (i + 1);
   48 |     }
   49 |   }
   50 | 
   51 |   private final boolean useActualParamName;
   52 | 
   53 |   /**
   54 |    * The key is the index and the value is the name of the parameter.<br />
   55 |    * The name is obtained from {@link Param} if specified. When {@link Param} is not specified, the parameter index is
   56 |    * used. Note that this index could be different from the actual index when the method has special parameters (i.e.
   57 |    * {@link RowBounds} or {@link ResultHandler}).
   58 |    * <ul>
   59 |    * <li>aMethod(@Param("M") int a, @Param("N") int b) -&gt; {{0, "M"}, {1, "N"}}</li>
   60 |    * <li>aMethod(int a, int b) -&gt; {{0, "0"}, {1, "1"}}</li>
   61 |    * <li>aMethod(int a, RowBounds rb, int b) -&gt; {{0, "0"}, {2, "1"}}</li>
   62 |    * </ul>
   63 |    */
   64 |   private final SortedMap<Integer, String> names;
   65 |   private final Map<String, Type> typeMap = new HashMap<>();
   66 | 
   67 |   private boolean hasParamAnnotation;
   68 |   private boolean useParamMap;
   69 | 
   70 |   public ParamNameResolver(Configuration config, Method method, Class<?> mapperClass) {
   71 |     this.useActualParamName = config.isUseActualParamName();
   72 |     final Class<?>[] paramTypes = method.getParameterTypes();
   73 |     final Annotation[][] paramAnnotations = method.getParameterAnnotations();
   74 |     final SortedMap<Integer, String> map = new TreeMap<>();
   75 |     Type[] actualParamTypes = TypeParameterResolver.resolveParamTypes(method, mapperClass);
   76 |     int paramCount = paramAnnotations.length;
   77 |     // get names from @Param annotations
   78 |     for (int paramIndex = 0; paramIndex < paramCount; paramIndex++) {
   79 |       if (isSpecialParameter(paramTypes[paramIndex])) {
   80 |         // skip special parameters
   81 |         continue;
   82 |       }
   83 |       String name = null;
   84 |       for (Annotation annotation : paramAnnotations[paramIndex]) {
   85 |         if (annotation instanceof Param) {
   86 |           hasParamAnnotation = true;
   87 |           useParamMap = true;
   88 |           name = ((Param) annotation).value();
   89 |           break;
   90 |         }
   91 |       }
   92 |       if (name == null) {
   93 |         // @Param was not specified.
   94 |         if (useActualParamName) {
   95 |           name = getActualParamName(method, paramIndex);
   96 |         }
   97 |         if (name == null) {
   98 |           // use the parameter index as the name ("0", "1", ...)
   99 |           // gcode issue #71
  100 |           name = String.valueOf(map.size());
  101 |         }
  102 |       }
  103 |       map.put(paramIndex, name);
  104 |       typeMap.put(name, actualParamTypes[paramIndex]);
  105 |     }
  106 |     names = Collections.unmodifiableSortedMap(map);
  107 |     if (names.size() > 1) {
  108 |       useParamMap = true;
  109 |     }
  110 |     if (names.size() == 1) {
  111 |       Type soleParamType = actualParamTypes[0];
  112 |       if (soleParamType instanceof GenericArrayType) {
  113 |         typeMap.put("array", soleParamType);
  114 |       } else {
  115 |         Class<?> soleParamClass = null;
  116 |         if (soleParamType instanceof ParameterizedType) {
  117 |           soleParamClass = (Class<?>) ((ParameterizedType) soleParamType).getRawType();
  118 |         } else if (soleParamType instanceof Class) {
  119 |           soleParamClass = (Class<?>) soleParamType;
  120 |         }
  121 |         if (Collection.class.isAssignableFrom(soleParamClass)) {
  122 |           typeMap.put("collection", soleParamType);
  123 |           if (List.class.isAssignableFrom(soleParamClass)) {
  124 |             typeMap.put("list", soleParamType);
  125 |           }
  126 |         }
  127 |       }
  128 |     }
  129 |   }
  130 | 
  131 |   private String getActualParamName(Method method, int paramIndex) {
  132 |     return ParamNameUtil.getParamNames(method).get(paramIndex);
  133 |   }
  134 | 
  135 |   private static boolean isSpecialParameter(Class<?> clazz) {
  136 |     return RowBounds.class.isAssignableFrom(clazz) || ResultHandler.class.isAssignableFrom(clazz);
  137 |   }
  138 | 
  139 |   /**
  140 |    * Returns parameter names referenced by SQL providers.
  141 |    *
  142 |    * @return the names
  143 |    */
  144 |   public String[] getNames() {
  145 |     return names.values().toArray(new String[0]);
  146 |   }
  147 | 
  148 |   /**
  149 |    * A single non-special parameter is returned without a name. Multiple parameters are named using the naming rule. In
  150 |    * addition to the default names, this method also adds the generic names (param1, param2, ...).
  151 |    *
  152 |    * @param args
  153 |    *          the args
  154 |    *
  155 |    * @return the named params
  156 |    */
  157 |   public Object getNamedParams(Object[] args) {
  158 |     final int paramCount = names.size();
  159 |     if (args == null || paramCount == 0) {
  160 |       return null;
  161 |     }
  162 |     if (!hasParamAnnotation && paramCount == 1) {
  163 |       Object value = args[names.firstKey()];
  164 |       return wrapToMapIfCollection(value, useActualParamName ? names.get(names.firstKey()) : null);
  165 |     } else {
  166 |       final Map<String, Object> param = new ParamMap<>();
  167 |       int i = 0;
  168 |       for (Map.Entry<Integer, String> entry : names.entrySet()) {
  169 |         param.put(entry.getValue(), args[entry.getKey()]);
  170 |         // add generic param names (param1, param2, ...)
  171 |         final String genericParamName = i < 10 ? GENERIC_NAME_CACHE[i] : GENERIC_NAME_PREFIX + (i + 1);
  172 |         // ensure not to overwrite parameter named with @Param
  173 |         if (!names.containsValue(genericParamName)) {
  174 |           param.put(genericParamName, args[entry.getKey()]);
  175 |         }
  176 |         i++;
  177 |       }
  178 |       return param;
  179 |     }
  180 |   }
  181 | 
  182 |   public Type getType(String name) {
  183 |     PropertyTokenizer propertyTokenizer = new PropertyTokenizer(name);
  184 |     String unindexed = propertyTokenizer.getName();
  185 |     Type type = typeMap.get(unindexed);
  186 | 
  187 |     if (type == null && unindexed.startsWith(GENERIC_NAME_PREFIX)) {
  188 |       try {
  189 |         Integer paramIndex = Integer.valueOf(unindexed.substring(GENERIC_NAME_PREFIX.length())) - 1;
  190 |         unindexed = names.get(paramIndex);
  191 |         if (unindexed != null) {
  192 |           type = typeMap.get(unindexed);
  193 |         }
  194 |       } catch (NumberFormatException e) {
  195 |         // user mistake
  196 |       }
  197 |     }
  198 | 
  199 |     if (propertyTokenizer.getIndex() != null) {
  200 |       if (type instanceof ParameterizedType) {
  201 |         Type[] typeArgs = ((ParameterizedType) type).getActualTypeArguments();
  202 |         return typeArgs[0];
  203 |       } else if (type instanceof Class && ((Class<?>) type).isArray()) {
  204 |         return ((Class<?>) type).getComponentType();
  205 |       }
  206 |     }
  207 |     return type;
  208 |   }
  209 | 
  210 |   /**
  211 |    * Wrap to a {@link ParamMap} if object is {@link Collection} or array.
  212 |    *
  213 |    * @param object
  214 |    *          a parameter object
  215 |    * @param actualParamName
  216 |    *          an actual parameter name (If specify a name, set an object to {@link ParamMap} with specified name)
  217 |    *
  218 |    * @return a {@link ParamMap}
  219 |    *
  220 |    * @since 3.5.5
  221 |    */
  222 |   public static Object wrapToMapIfCollection(Object object, String actualParamName) {
  223 |     if (object instanceof Collection) {
  224 |       ParamMap<Object> map = new ParamMap<>();
  225 |       map.put("collection", object);
  226 |       if (object instanceof List) {
  227 |         map.put("list", object);
  228 |       }
  229 |       Optional.ofNullable(actualParamName).ifPresent(name -> map.put(name, object));
  230 |       return map;
  231 |     }
  232 |     if (object != null && object.getClass().isArray()) {
  233 |       ParamMap<Object> map = new ParamMap<>();
  234 |       map.put("array", object);
  235 |       Optional.ofNullable(actualParamName).ifPresent(name -> map.put(name, object));
  236 |       return map;
  237 |     }
  238 |     return object;
  239 |   }
  240 | 
  241 |   public boolean isUseParamMap() {
  242 |     return useParamMap;
  243 |   }
  244 | }
```
