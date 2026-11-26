<!-- learning-docs source archive（机械抓取原文，禁止手改代码内容） -->
<!-- origin: org.mybatis:mybatis:3.6.0 | path: org/apache/ibatis/plugin/Interceptor.java -->
<!-- 正文引用：[[源码:Interceptor.java#L起-L止]] ；代码块头部：(src: Interceptor.java#L起-L止) -->

# Interceptor.java

| 项 | 值 |
| --- | --- |
| 来源 | `org.mybatis:mybatis:3.6.0` |
| 路径 | `org/apache/ibatis/plugin/Interceptor.java` |
| 行数 | 35 |
| 在线地址 | https://repo1.maven.org/maven2/org/mybatis/mybatis/3.6.0/mybatis-3.6.0-sources.jar |
| 抓取时间 | 2026-11-25T14:59:21.619Z |

```java
    1 | /*
    2 |  *    Copyright 2009-2022 the original author or authors.
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
   18 | import java.util.Properties;
   19 | 
   20 | /**
   21 |  * @author Clinton Begin
   22 |  */
   23 | public interface Interceptor {
   24 | 
   25 |   Object intercept(Invocation invocation) throws Throwable;
   26 | 
   27 |   default Object plugin(Object target) {
   28 |     return Plugin.wrap(target, this);
   29 |   }
   30 | 
   31 |   default void setProperties(Properties properties) {
   32 |     // NOP
   33 |   }
   34 | 
   35 | }
```
