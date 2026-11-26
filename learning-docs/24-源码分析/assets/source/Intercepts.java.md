<!-- learning-docs source archive（机械抓取原文，禁止手改代码内容） -->
<!-- origin: org.mybatis:mybatis:3.6.0 | path: org/apache/ibatis/plugin/Intercepts.java -->
<!-- 正文引用：[[源码:Intercepts.java#L起-L止]] ；代码块头部：(src: Intercepts.java#L起-L止) -->

# Intercepts.java

| 项 | 值 |
| --- | --- |
| 来源 | `org.mybatis:mybatis:3.6.0` |
| 路径 | `org/apache/ibatis/plugin/Intercepts.java` |
| 行数 | 54 |
| 在线地址 | https://repo1.maven.org/maven2/org/mybatis/mybatis/3.6.0/mybatis-3.6.0-sources.jar |
| 抓取时间 | 2026-11-25T15:00:25.485Z |

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
   18 | import java.lang.annotation.Documented;
   19 | import java.lang.annotation.ElementType;
   20 | import java.lang.annotation.Retention;
   21 | import java.lang.annotation.RetentionPolicy;
   22 | import java.lang.annotation.Target;
   23 | 
   24 | /**
   25 |  * The annotation that specify target methods to intercept.
   26 |  * <p>
   27 |  * <b>How to use:</b>
   28 |  *
   29 |  * <pre>{@code
   30 |  * @Intercepts({ &#064;Signature(type = Executor.class, method = "update", args = { MappedStatement.class, Object.class }) })
   31 |  * public class ExamplePlugin implements Interceptor {
   32 |  *   &#064;Override
   33 |  *   public Object intercept(Invocation invocation) throws Throwable {
   34 |  *     // implement pre-processing if needed
   35 |  *     Object returnObject = invocation.proceed();
   36 |  *     // implement post-processing if needed
   37 |  *     return returnObject;
   38 |  *   }
   39 |  * }
   40 |  * }</pre>
   41 |  *
   42 |  * @author Clinton Begin
   43 |  */
   44 | @Documented
   45 | @Retention(RetentionPolicy.RUNTIME)
   46 | @Target(ElementType.TYPE)
   47 | public @interface Intercepts {
   48 |   /**
   49 |    * Returns method signatures to intercept.
   50 |    *
   51 |    * @return method signatures
   52 |    */
   53 |   Signature[] value();
   54 | }
```
