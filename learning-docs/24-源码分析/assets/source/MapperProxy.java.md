<!-- learning-docs source archive（机械抓取原文，禁止手改代码内容） -->
<!-- origin: org.mybatis:mybatis:3.6.0 | path: org/apache/ibatis/binding/MapperProxy.java -->
<!-- 正文引用：[[源码:MapperProxy.java#L起-L止]] ；代码块头部：(src: MapperProxy.java#L起-L止) -->

# MapperProxy.java

| 项 | 值 |
| --- | --- |
| 来源 | `org.mybatis:mybatis:3.6.0` |
| 路径 | `org/apache/ibatis/binding/MapperProxy.java` |
| 行数 | 121 |
| 在线地址 | https://repo1.maven.org/maven2/org/mybatis/mybatis/3.6.0/mybatis-3.6.0-sources.jar |
| 抓取时间 | 2026-11-25T14:00:20.030Z |

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
   18 | import java.io.Serializable;
   19 | import java.lang.invoke.MethodHandle;
   20 | import java.lang.invoke.MethodHandles;
   21 | import java.lang.invoke.MethodHandles.Lookup;
   22 | import java.lang.invoke.MethodType;
   23 | import java.lang.reflect.InvocationHandler;
   24 | import java.lang.reflect.InvocationTargetException;
   25 | import java.lang.reflect.Method;
   26 | import java.util.Map;
   27 | 
   28 | import org.apache.ibatis.reflection.ExceptionUtil;
   29 | import org.apache.ibatis.session.SqlSession;
   30 | 
   31 | /**
   32 |  * @author Clinton Begin
   33 |  * @author Eduardo Macarron
   34 |  */
   35 | public class MapperProxy<T> implements InvocationHandler, Serializable {
   36 | 
   37 |   private static final long serialVersionUID = -4724728412955527868L;
   38 |   private static final Method privateLookupInMethod;
   39 |   private final SqlSession sqlSession;
   40 |   private final Class<T> mapperInterface;
   41 |   private final Map<Method, MapperMethodInvoker> methodCache;
   42 | 
   43 |   public MapperProxy(SqlSession sqlSession, Class<T> mapperInterface, Map<Method, MapperMethodInvoker> methodCache) {
   44 |     this.sqlSession = sqlSession;
   45 |     this.mapperInterface = mapperInterface;
   46 |     this.methodCache = methodCache;
   47 |   }
   48 | 
   49 |   static {
   50 |     try {
   51 |       privateLookupInMethod = MethodHandles.class.getMethod("privateLookupIn", Class.class, MethodHandles.Lookup.class);
   52 |     } catch (NoSuchMethodException e) {
   53 |       throw new IllegalStateException(
   54 |           "There is no 'privateLookupIn(Class, Lookup)' method in java.lang.invoke.MethodHandles.", e);
   55 |     }
   56 |   }
   57 | 
   58 |   @Override
   59 |   public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
   60 |     try {
   61 |       if (Object.class.equals(method.getDeclaringClass())) {
   62 |         return method.invoke(this, args);
   63 |       }
   64 |       return cachedInvoker(method).invoke(proxy, method, args, sqlSession);
   65 |     } catch (Throwable t) {
   66 |       throw ExceptionUtil.unwrapThrowable(t);
   67 |     }
   68 |   }
   69 | 
   70 |   private MapperMethodInvoker cachedInvoker(Method method) throws Throwable {
   71 |     try {
   72 |       return methodCache.computeIfAbsent(method, m -> {
   73 |         if (!m.isDefault()) {
   74 |           return new PlainMethodInvoker(new MapperMethod(mapperInterface, method, sqlSession.getConfiguration()));
   75 |         }
   76 |         try {
   77 |           return new DefaultMethodInvoker(getMethodHandleJava9(method));
   78 |         } catch (NoSuchMethodException | IllegalAccessException | InvocationTargetException e) {
   79 |           throw new RuntimeException(e);
   80 |         }
   81 |       });
   82 |     } catch (RuntimeException re) {
   83 |       Throwable cause = re.getCause();
   84 |       throw cause == null ? re : cause;
   85 |     }
   86 |   }
   87 | 
   88 |   private MethodHandle getMethodHandleJava9(Method method)
   89 |       throws NoSuchMethodException, IllegalAccessException, InvocationTargetException {
   90 |     final Class<?> declaringClass = method.getDeclaringClass();
   91 |     return ((Lookup) privateLookupInMethod.invoke(null, declaringClass, MethodHandles.lookup())).findSpecial(
   92 |         declaringClass, method.getName(), MethodType.methodType(method.getReturnType(), method.getParameterTypes()),
   93 |         declaringClass);
   94 |   }
   95 | 
   96 |   private static class PlainMethodInvoker implements MapperMethodInvoker {
   97 |     private final MapperMethod mapperMethod;
   98 | 
   99 |     public PlainMethodInvoker(MapperMethod mapperMethod) {
  100 |       this.mapperMethod = mapperMethod;
  101 |     }
  102 | 
  103 |     @Override
  104 |     public Object invoke(Object proxy, Method method, Object[] args, SqlSession sqlSession) throws Throwable {
  105 |       return mapperMethod.execute(sqlSession, args);
  106 |     }
  107 |   }
  108 | 
  109 |   private static class DefaultMethodInvoker implements MapperMethodInvoker {
  110 |     private final MethodHandle methodHandle;
  111 | 
  112 |     public DefaultMethodInvoker(MethodHandle methodHandle) {
  113 |       this.methodHandle = methodHandle;
  114 |     }
  115 | 
  116 |     @Override
  117 |     public Object invoke(Object proxy, Method method, Object[] args, SqlSession sqlSession) throws Throwable {
  118 |       return methodHandle.bindTo(proxy).invokeWithArguments(args);
  119 |     }
  120 |   }
  121 | }
```
