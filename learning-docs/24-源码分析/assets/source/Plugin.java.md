<!-- learning-docs source archive（机械抓取原文，禁止手改代码内容） -->
<!-- origin: org.mybatis:mybatis:3.6.0 | path: org/apache/ibatis/plugin/Plugin.java -->
<!-- 正文引用：[[源码:Plugin.java#L起-L止]] ；代码块头部：(src: Plugin.java#L起-L止) -->

# Plugin.java

| 项 | 值 |
| --- | --- |
| 来源 | `org.mybatis:mybatis:3.6.0` |
| 路径 | `org/apache/ibatis/plugin/Plugin.java` |
| 行数 | 101 |
| 在线地址 | https://repo1.maven.org/maven2/org/mybatis/mybatis/3.6.0/mybatis-3.6.0-sources.jar |
| 抓取时间 | 2026-11-25T14:59:22.160Z |

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
   16 | package org.apache.ibatis.plugin;
   17 | 
   18 | import java.lang.reflect.InvocationHandler;
   19 | import java.lang.reflect.Method;
   20 | import java.lang.reflect.Proxy;
   21 | import java.util.HashMap;
   22 | import java.util.HashSet;
   23 | import java.util.Map;
   24 | import java.util.Set;
   25 | 
   26 | import org.apache.ibatis.reflection.ExceptionUtil;
   27 | 
   28 | /**
   29 |  * @author Clinton Begin
   30 |  */
   31 | public class Plugin implements InvocationHandler {
   32 | 
   33 |   private final Object target;
   34 |   private final Interceptor interceptor;
   35 |   private final Map<Class<?>, Set<Method>> signatureMap;
   36 | 
   37 |   private Plugin(Object target, Interceptor interceptor, Map<Class<?>, Set<Method>> signatureMap) {
   38 |     this.target = target;
   39 |     this.interceptor = interceptor;
   40 |     this.signatureMap = signatureMap;
   41 |   }
   42 | 
   43 |   public static Object wrap(Object target, Interceptor interceptor) {
   44 |     Map<Class<?>, Set<Method>> signatureMap = getSignatureMap(interceptor);
   45 |     Class<?> type = target.getClass();
   46 |     Class<?>[] interfaces = getAllInterfaces(type, signatureMap);
   47 |     if (interfaces.length > 0) {
   48 |       return Proxy.newProxyInstance(type.getClassLoader(), interfaces, new Plugin(target, interceptor, signatureMap));
   49 |     }
   50 |     return target;
   51 |   }
   52 | 
   53 |   @Override
   54 |   public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
   55 |     try {
   56 |       Set<Method> methods = signatureMap.get(method.getDeclaringClass());
   57 |       if (methods != null && methods.contains(method)) {
   58 |         return interceptor.intercept(new Invocation(target, method, args));
   59 |       }
   60 |       return method.invoke(target, args);
   61 |     } catch (Exception e) {
   62 |       throw ExceptionUtil.unwrapThrowable(e);
   63 |     }
   64 |   }
   65 | 
   66 |   private static Map<Class<?>, Set<Method>> getSignatureMap(Interceptor interceptor) {
   67 |     Intercepts interceptsAnnotation = interceptor.getClass().getAnnotation(Intercepts.class);
   68 |     // issue #251
   69 |     if (interceptsAnnotation == null) {
   70 |       throw new PluginException(
   71 |           "No @Intercepts annotation was found in interceptor " + interceptor.getClass().getName());
   72 |     }
   73 |     Signature[] sigs = interceptsAnnotation.value();
   74 |     Map<Class<?>, Set<Method>> signatureMap = new HashMap<>();
   75 |     for (Signature sig : sigs) {
   76 |       Set<Method> methods = signatureMap.computeIfAbsent(sig.type(), k -> new HashSet<>());
   77 |       try {
   78 |         Method method = sig.type().getMethod(sig.method(), sig.args());
   79 |         methods.add(method);
   80 |       } catch (NoSuchMethodException e) {
   81 |         throw new PluginException("Could not find method on " + sig.type() + " named " + sig.method() + ". Cause: " + e,
   82 |             e);
   83 |       }
   84 |     }
   85 |     return signatureMap;
   86 |   }
   87 | 
   88 |   private static Class<?>[] getAllInterfaces(Class<?> type, Map<Class<?>, Set<Method>> signatureMap) {
   89 |     Set<Class<?>> interfaces = new HashSet<>();
   90 |     while (type != null) {
   91 |       for (Class<?> c : type.getInterfaces()) {
   92 |         if (signatureMap.containsKey(c)) {
   93 |           interfaces.add(c);
   94 |         }
   95 |       }
   96 |       type = type.getSuperclass();
   97 |     }
   98 |     return interfaces.toArray(new Class<?>[0]);
   99 |   }
  100 | 
  101 | }
```
