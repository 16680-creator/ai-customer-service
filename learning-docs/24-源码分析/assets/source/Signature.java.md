<!-- learning-docs source archive（机械抓取原文，禁止手改代码内容） -->
<!-- origin: org.mybatis:mybatis:3.6.0 | path: org/apache/ibatis/plugin/Signature.java -->
<!-- 正文引用：[[源码:Signature.java#L起-L止]] ；代码块头部：(src: Signature.java#L起-L止) -->

# Signature.java

| 项 | 值 |
| --- | --- |
| 来源 | `org.mybatis:mybatis:3.6.0` |
| 路径 | `org/apache/ibatis/plugin/Signature.java` |
| 行数 | 54 |
| 在线地址 | https://repo1.maven.org/maven2/org/mybatis/mybatis/3.6.0/mybatis-3.6.0-sources.jar |
| 抓取时间 | 2026-11-25T15:00:25.558Z |

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
   16 | package org.apache.ibatis.plugin;
   17 | 
   18 | import java.lang.annotation.Documented;
   19 | import java.lang.annotation.Retention;
   20 | import java.lang.annotation.RetentionPolicy;
   21 | import java.lang.annotation.Target;
   22 | 
   23 | /**
   24 |  * The annotation that indicate the method signature.
   25 |  *
   26 |  * @see Intercepts
   27 |  *
   28 |  * @author Clinton Begin
   29 |  */
   30 | @Documented
   31 | @Retention(RetentionPolicy.RUNTIME)
   32 | @Target({})
   33 | public @interface Signature {
   34 |   /**
   35 |    * Returns the java type.
   36 |    *
   37 |    * @return the java type
   38 |    */
   39 |   Class<?> type();
   40 | 
   41 |   /**
   42 |    * Returns the method name.
   43 |    *
   44 |    * @return the method name
   45 |    */
   46 |   String method();
   47 | 
   48 |   /**
   49 |    * Returns java types for method argument.
   50 |    *
   51 |    * @return java types for method argument
   52 |    */
   53 |   Class<?>[] args();
   54 | }
```
