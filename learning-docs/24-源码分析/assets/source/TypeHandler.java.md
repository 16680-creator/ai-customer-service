<!-- learning-docs source archive（机械抓取原文，禁止手改代码内容） -->
<!-- origin: org.mybatis:mybatis:3.6.0 | path: org/apache/ibatis/type/TypeHandler.java -->
<!-- 正文引用：[[源码:TypeHandler.java#L起-L止]] ；代码块头部：(src: TypeHandler.java#L起-L止) -->

# TypeHandler.java

| 项 | 值 |
| --- | --- |
| 来源 | `org.mybatis:mybatis:3.6.0` |
| 路径 | `org/apache/ibatis/type/TypeHandler.java` |
| 行数 | 49 |
| 在线地址 | https://repo1.maven.org/maven2/org/mybatis/mybatis/3.6.0/mybatis-3.6.0-sources.jar |
| 抓取时间 | 2026-11-25T15:00:25.836Z |

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
   16 | package org.apache.ibatis.type;
   17 | 
   18 | import java.sql.CallableStatement;
   19 | import java.sql.PreparedStatement;
   20 | import java.sql.ResultSet;
   21 | import java.sql.SQLException;
   22 | 
   23 | /**
   24 |  * @author Clinton Begin
   25 |  */
   26 | public interface TypeHandler<T> {
   27 | 
   28 |   void setParameter(PreparedStatement ps, int i, T parameter, JdbcType jdbcType) throws SQLException;
   29 | 
   30 |   /**
   31 |    * Gets the result.
   32 |    *
   33 |    * @param rs
   34 |    *          the rs
   35 |    * @param columnName
   36 |    *          Column name, when configuration <code>useColumnLabel</code> is <code>false</code>
   37 |    *
   38 |    * @return the result
   39 |    *
   40 |    * @throws SQLException
   41 |    *           the SQL exception
   42 |    */
   43 |   T getResult(ResultSet rs, String columnName) throws SQLException;
   44 | 
   45 |   T getResult(ResultSet rs, int columnIndex) throws SQLException;
   46 | 
   47 |   T getResult(CallableStatement cs, int columnIndex) throws SQLException;
   48 | 
   49 | }
```
