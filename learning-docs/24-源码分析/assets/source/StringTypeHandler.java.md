<!-- learning-docs source archive（机械抓取原文，禁止手改代码内容） -->
<!-- origin: org.mybatis:mybatis:3.6.0 | path: org/apache/ibatis/type/StringTypeHandler.java -->
<!-- 正文引用：[[源码:StringTypeHandler.java#L起-L止]] ；代码块头部：(src: StringTypeHandler.java#L起-L止) -->

# StringTypeHandler.java

| 项 | 值 |
| --- | --- |
| 来源 | `org.mybatis:mybatis:3.6.0` |
| 路径 | `org/apache/ibatis/type/StringTypeHandler.java` |
| 行数 | 49 |
| 在线地址 | https://repo1.maven.org/maven2/org/mybatis/mybatis/3.6.0/mybatis-3.6.0-sources.jar |
| 抓取时间 | 2026-11-25T15:02:23.657Z |

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
   26 | public class StringTypeHandler extends BaseTypeHandler<String> {
   27 |   public static final StringTypeHandler INSTANCE = new StringTypeHandler();
   28 | 
   29 |   @Override
   30 |   public void setNonNullParameter(PreparedStatement ps, int i, String parameter, JdbcType jdbcType)
   31 |       throws SQLException {
   32 |     ps.setString(i, parameter);
   33 |   }
   34 | 
   35 |   @Override
   36 |   public String getNullableResult(ResultSet rs, String columnName) throws SQLException {
   37 |     return rs.getString(columnName);
   38 |   }
   39 | 
   40 |   @Override
   41 |   public String getNullableResult(ResultSet rs, int columnIndex) throws SQLException {
   42 |     return rs.getString(columnIndex);
   43 |   }
   44 | 
   45 |   @Override
   46 |   public String getNullableResult(CallableStatement cs, int columnIndex) throws SQLException {
   47 |     return cs.getString(columnIndex);
   48 |   }
   49 | }
```
