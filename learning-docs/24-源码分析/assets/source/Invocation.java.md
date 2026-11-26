<!-- learning-docs source archive（机械抓取原文，禁止手改代码内容） -->
<!-- origin: org.mybatis:mybatis:3.6.0 | path: org/apache/ibatis/plugin/Invocation.java -->
<!-- 正文引用：[[源码:Invocation.java#L起-L止]] ；代码块头部：(src: Invocation.java#L起-L止) -->

# Invocation.java

| 项 | 值 |
| --- | --- |
| 来源 | `org.mybatis:mybatis:3.6.0` |
| 路径 | `org/apache/ibatis/plugin/Invocation.java` |
| 行数 | 64 |
| 在线地址 | https://repo1.maven.org/maven2/org/mybatis/mybatis/3.6.0/mybatis-3.6.0-sources.jar |
| 抓取时间 | 2026-11-25T14:59:22.328Z |

```java
    1 | /*
    2 |  *    Copyright 2009-2024 the original author or authors.
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
   18 | import java.lang.reflect.InvocationTargetException;
   19 | import java.lang.reflect.Method;
   20 | import java.util.Arrays;
   21 | import java.util.List;
   22 | 
   23 | import org.apache.ibatis.executor.Executor;
   24 | import org.apache.ibatis.executor.parameter.ParameterHandler;
   25 | import org.apache.ibatis.executor.resultset.ResultSetHandler;
   26 | import org.apache.ibatis.executor.statement.StatementHandler;
   27 | 
   28 | /**
   29 |  * @author Clinton Begin
   30 |  */
   31 | public class Invocation {
   32 | 
   33 |   private static final List<Class<?>> targetClasses = Arrays.asList(Executor.class, ParameterHandler.class,
   34 |       ResultSetHandler.class, StatementHandler.class);
   35 |   private final Object target;
   36 |   private final Method method;
   37 |   private final Object[] args;
   38 | 
   39 |   public Invocation(Object target, Method method, Object[] args) {
   40 |     if (!targetClasses.contains(method.getDeclaringClass())) {
   41 |       throw new IllegalArgumentException("Method '" + method + "' is not supported as a plugin target.");
   42 |     }
   43 |     this.target = target;
   44 |     this.method = method;
   45 |     this.args = args;
   46 |   }
   47 | 
   48 |   public Object getTarget() {
   49 |     return target;
   50 |   }
   51 | 
   52 |   public Method getMethod() {
   53 |     return method;
   54 |   }
   55 | 
   56 |   public Object[] getArgs() {
   57 |     return args;
   58 |   }
   59 | 
   60 |   public Object proceed() throws InvocationTargetException, IllegalAccessException {
   61 |     return method.invoke(target, args);
   62 |   }
   63 | 
   64 | }
```
