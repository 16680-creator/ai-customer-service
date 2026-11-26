<!-- learning-docs source archive（机械抓取原文，禁止手改代码内容） -->
<!-- origin: org.mybatis:mybatis:3.6.0 | path: org/apache/ibatis/binding/MapperRegistry.java -->
<!-- 正文引用：[[源码:MapperRegistry.java#L起-L止]] ；代码块头部：(src: MapperRegistry.java#L起-L止) -->

# MapperRegistry.java

| 项 | 值 |
| --- | --- |
| 来源 | `org.mybatis:mybatis:3.6.0` |
| 路径 | `org/apache/ibatis/binding/MapperRegistry.java` |
| 行数 | 124 |
| 在线地址 | https://repo1.maven.org/maven2/org/mybatis/mybatis/3.6.0/mybatis-3.6.0-sources.jar |
| 抓取时间 | 2026-11-25T14:00:19.817Z |

```java
    1 | /*
    2 |  *    Copyright 2009-2023 the original author or authors.
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
   18 | import java.util.Collection;
   19 | import java.util.Collections;
   20 | import java.util.Map;
   21 | import java.util.Set;
   22 | import java.util.concurrent.ConcurrentHashMap;
   23 | 
   24 | import org.apache.ibatis.builder.annotation.MapperAnnotationBuilder;
   25 | import org.apache.ibatis.io.ResolverUtil;
   26 | import org.apache.ibatis.session.Configuration;
   27 | import org.apache.ibatis.session.SqlSession;
   28 | 
   29 | /**
   30 |  * @author Clinton Begin
   31 |  * @author Eduardo Macarron
   32 |  * @author Lasse Voss
   33 |  */
   34 | public class MapperRegistry {
   35 | 
   36 |   private final Configuration config;
   37 |   private final Map<Class<?>, MapperProxyFactory<?>> knownMappers = new ConcurrentHashMap<>();
   38 | 
   39 |   public MapperRegistry(Configuration config) {
   40 |     this.config = config;
   41 |   }
   42 | 
   43 |   @SuppressWarnings("unchecked")
   44 |   public <T> T getMapper(Class<T> type, SqlSession sqlSession) {
   45 |     final MapperProxyFactory<T> mapperProxyFactory = (MapperProxyFactory<T>) knownMappers.get(type);
   46 |     if (mapperProxyFactory == null) {
   47 |       throw new BindingException("Type " + type + " is not known to the MapperRegistry.");
   48 |     }
   49 |     try {
   50 |       return mapperProxyFactory.newInstance(sqlSession);
   51 |     } catch (Exception e) {
   52 |       throw new BindingException("Error getting mapper instance. Cause: " + e, e);
   53 |     }
   54 |   }
   55 | 
   56 |   public <T> boolean hasMapper(Class<T> type) {
   57 |     return knownMappers.containsKey(type);
   58 |   }
   59 | 
   60 |   public <T> void addMapper(Class<T> type) {
   61 |     if (type.isInterface()) {
   62 |       if (hasMapper(type)) {
   63 |         throw new BindingException("Type " + type + " is already known to the MapperRegistry.");
   64 |       }
   65 |       boolean loadCompleted = false;
   66 |       try {
   67 |         knownMappers.put(type, new MapperProxyFactory<>(type));
   68 |         // It's important that the type is added before the parser is run
   69 |         // otherwise the binding may automatically be attempted by the
   70 |         // mapper parser. If the type is already known, it won't try.
   71 |         MapperAnnotationBuilder parser = new MapperAnnotationBuilder(config, type);
   72 |         parser.parse();
   73 |         loadCompleted = true;
   74 |       } finally {
   75 |         if (!loadCompleted) {
   76 |           knownMappers.remove(type);
   77 |         }
   78 |       }
   79 |     }
   80 |   }
   81 | 
   82 |   /**
   83 |    * Gets the mappers.
   84 |    *
   85 |    * @return the mappers
   86 |    *
   87 |    * @since 3.2.2
   88 |    */
   89 |   public Collection<Class<?>> getMappers() {
   90 |     return Collections.unmodifiableCollection(knownMappers.keySet());
   91 |   }
   92 | 
   93 |   /**
   94 |    * Adds the mappers.
   95 |    *
   96 |    * @param packageName
   97 |    *          the package name
   98 |    * @param superType
   99 |    *          the super type
  100 |    *
  101 |    * @since 3.2.2
  102 |    */
  103 |   public void addMappers(String packageName, Class<?> superType) {
  104 |     ResolverUtil<Class<?>> resolverUtil = new ResolverUtil<>();
  105 |     resolverUtil.find(new ResolverUtil.IsA(superType), packageName);
  106 |     Set<Class<? extends Class<?>>> mapperSet = resolverUtil.getClasses();
  107 |     for (Class<?> mapperClass : mapperSet) {
  108 |       addMapper(mapperClass);
  109 |     }
  110 |   }
  111 | 
  112 |   /**
  113 |    * Adds the mappers.
  114 |    *
  115 |    * @param packageName
  116 |    *          the package name
  117 |    *
  118 |    * @since 3.2.2
  119 |    */
  120 |   public void addMappers(String packageName) {
  121 |     addMappers(packageName, Object.class);
  122 |   }
  123 | 
  124 | }
```
