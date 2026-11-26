<!-- learning-docs source archive（机械抓取原文，禁止手改代码内容） -->
<!-- origin: org.mybatis:mybatis:3.6.0 | path: org/apache/ibatis/session/SqlSessionFactory.java -->
<!-- 正文引用：[[源码:SqlSessionFactory.java#L起-L止]] ；代码块头部：(src: SqlSessionFactory.java#L起-L止) -->

# SqlSessionFactory.java

| 项 | 值 |
| --- | --- |
| 来源 | `org.mybatis:mybatis:3.6.0` |
| 路径 | `org/apache/ibatis/session/SqlSessionFactory.java` |
| 行数 | 45 |
| 在线地址 | https://repo1.maven.org/maven2/org/mybatis/mybatis/3.6.0/mybatis-3.6.0-sources.jar |
| 抓取时间 | 2026-11-25T14:00:18.217Z |

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
   16 | package org.apache.ibatis.session;
   17 | 
   18 | import java.sql.Connection;
   19 | 
   20 | /**
   21 |  * Creates an {@link SqlSession} out of a connection or a DataSource
   22 |  *
   23 |  * @author Clinton Begin
   24 |  */
   25 | public interface SqlSessionFactory {
   26 | 
   27 |   SqlSession openSession();
   28 | 
   29 |   SqlSession openSession(boolean autoCommit);
   30 | 
   31 |   SqlSession openSession(Connection connection);
   32 | 
   33 |   SqlSession openSession(TransactionIsolationLevel level);
   34 | 
   35 |   SqlSession openSession(ExecutorType execType);
   36 | 
   37 |   SqlSession openSession(ExecutorType execType, boolean autoCommit);
   38 | 
   39 |   SqlSession openSession(ExecutorType execType, TransactionIsolationLevel level);
   40 | 
   41 |   SqlSession openSession(ExecutorType execType, Connection connection);
   42 | 
   43 |   Configuration getConfiguration();
   44 | 
   45 | }
```
