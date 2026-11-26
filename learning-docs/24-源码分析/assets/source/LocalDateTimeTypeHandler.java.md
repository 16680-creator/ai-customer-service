<!-- learning-docs source archive（机械抓取原文，禁止手改代码内容） -->
<!-- origin: org.mybatis:mybatis:3.6.0 | path: org/apache/ibatis/type/LocalDateTimeTypeHandler.java -->
<!-- 正文引用：[[源码:LocalDateTimeTypeHandler.java#L起-L止]] ；代码块头部：(src: LocalDateTimeTypeHandler.java#L起-L止) -->

# LocalDateTimeTypeHandler.java

| 项 | 值 |
| --- | --- |
| 来源 | `org.mybatis:mybatis:3.6.0` |
| 路径 | `org/apache/ibatis/type/LocalDateTimeTypeHandler.java` |
| 行数 | 51 |
| 在线地址 | https://repo1.maven.org/maven2/org/mybatis/mybatis/3.6.0/mybatis-3.6.0-sources.jar |
| 抓取时间 | 2026-11-25T15:00:25.910Z |

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
   22 | import java.time.LocalDateTime;
   23 | 
   24 | /**
   25 |  * @since 3.4.5
   26 |  *
   27 |  * @author Tomas Rohovsky
   28 |  */
   29 | public class LocalDateTimeTypeHandler extends BaseTypeHandler<LocalDateTime> {
   30 | 
   31 |   @Override
   32 |   public void setNonNullParameter(PreparedStatement ps, int i, LocalDateTime parameter, JdbcType jdbcType)
   33 |       throws SQLException {
   34 |     ps.setObject(i, parameter);
   35 |   }
   36 | 
   37 |   @Override
   38 |   public LocalDateTime getNullableResult(ResultSet rs, String columnName) throws SQLException {
   39 |     return rs.getObject(columnName, LocalDateTime.class);
   40 |   }
   41 | 
   42 |   @Override
   43 |   public LocalDateTime getNullableResult(ResultSet rs, int columnIndex) throws SQLException {
   44 |     return rs.getObject(columnIndex, LocalDateTime.class);
   45 |   }
   46 | 
   47 |   @Override
   48 |   public LocalDateTime getNullableResult(CallableStatement cs, int columnIndex) throws SQLException {
   49 |     return cs.getObject(columnIndex, LocalDateTime.class);
   50 |   }
   51 | }
```
