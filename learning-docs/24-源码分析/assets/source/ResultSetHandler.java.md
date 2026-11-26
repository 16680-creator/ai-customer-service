<!-- learning-docs source archive（机械抓取原文，禁止手改代码内容） -->
<!-- origin: org.mybatis:mybatis:3.6.0 | path: org/apache/ibatis/executor/resultset/ResultSetHandler.java -->
<!-- 正文引用：[[源码:ResultSetHandler.java#L起-L止]] ；代码块头部：(src: ResultSetHandler.java#L起-L止) -->

# ResultSetHandler.java

| 项 | 值 |
| --- | --- |
| 来源 | `org.mybatis:mybatis:3.6.0` |
| 路径 | `org/apache/ibatis/executor/resultset/ResultSetHandler.java` |
| 行数 | 36 |
| 在线地址 | https://repo1.maven.org/maven2/org/mybatis/mybatis/3.6.0/mybatis-3.6.0-sources.jar |
| 抓取时间 | 2026-11-25T14:00:30.916Z |

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
   16 | package org.apache.ibatis.executor.resultset;
   17 | 
   18 | import java.sql.CallableStatement;
   19 | import java.sql.SQLException;
   20 | import java.sql.Statement;
   21 | import java.util.List;
   22 | 
   23 | import org.apache.ibatis.cursor.Cursor;
   24 | 
   25 | /**
   26 |  * @author Clinton Begin
   27 |  */
   28 | public interface ResultSetHandler {
   29 | 
   30 |   <E> List<E> handleResultSets(Statement stmt) throws SQLException;
   31 | 
   32 |   <E> Cursor<E> handleCursorResultSets(Statement stmt) throws SQLException;
   33 | 
   34 |   void handleOutputParameters(CallableStatement cs) throws SQLException;
   35 | 
   36 | }
```
