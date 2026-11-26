<!-- learning-docs source archive（机械抓取原文，禁止手改代码内容） -->
<!-- origin: org.mybatis:mybatis:3.6.0 | path: org/apache/ibatis/binding/MapperProxyFactory.java -->
<!-- 正文引用：[[源码:MapperProxyFactory.java#L起-L止]] ；代码块头部：(src: MapperProxyFactory.java#L起-L止) -->

# MapperProxyFactory.java

| 项 | 值 |
| --- | --- |
| 来源 | `org.mybatis:mybatis:3.6.0` |
| 路径 | `org/apache/ibatis/binding/MapperProxyFactory.java` |
| 行数 | 55 |
| 在线地址 | https://repo1.maven.org/maven2/org/mybatis/mybatis/3.6.0/mybatis-3.6.0-sources.jar |
| 抓取时间 | 2026-11-25T14:00:20.221Z |

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
   18 | import java.lang.reflect.Method;
   19 | import java.lang.reflect.Proxy;
   20 | import java.util.Map;
   21 | import java.util.concurrent.ConcurrentHashMap;
   22 | 
   23 | import org.apache.ibatis.session.SqlSession;
   24 | 
   25 | /**
   26 |  * @author Lasse Voss
   27 |  */
   28 | public class MapperProxyFactory<T> {
   29 | 
   30 |   private final Class<T> mapperInterface;
   31 |   private final Map<Method, MapperMethodInvoker> methodCache = new ConcurrentHashMap<>();
   32 | 
   33 |   public MapperProxyFactory(Class<T> mapperInterface) {
   34 |     this.mapperInterface = mapperInterface;
   35 |   }
   36 | 
   37 |   public Class<T> getMapperInterface() {
   38 |     return mapperInterface;
   39 |   }
   40 | 
   41 |   public Map<Method, MapperMethodInvoker> getMethodCache() {
   42 |     return methodCache;
   43 |   }
   44 | 
   45 |   @SuppressWarnings("unchecked")
   46 |   protected T newInstance(MapperProxy<T> mapperProxy) {
   47 |     return (T) Proxy.newProxyInstance(mapperInterface.getClassLoader(), new Class[] { mapperInterface }, mapperProxy);
   48 |   }
   49 | 
   50 |   public T newInstance(SqlSession sqlSession) {
   51 |     final MapperProxy<T> mapperProxy = new MapperProxy<>(sqlSession, mapperInterface, methodCache);
   52 |     return newInstance(mapperProxy);
   53 |   }
   54 | 
   55 | }
```
