<!-- learning-docs source archive（机械抓取原文，禁止手改代码内容） -->
<!-- origin: org.mybatis:mybatis:3.6.0 | path: org/apache/ibatis/type/EnumTypeHandler.java -->
<!-- 正文引用：[[源码:EnumTypeHandler.java#L起-L止]] ；代码块头部：(src: EnumTypeHandler.java#L起-L止) -->

# EnumTypeHandler.java

| 项 | 值 |
| --- | --- |
| 来源 | `org.mybatis:mybatis:3.6.0` |
| 路径 | `org/apache/ibatis/type/EnumTypeHandler.java` |
| 行数 | 63 |
| 在线地址 | https://repo1.maven.org/maven2/org/mybatis/mybatis/3.6.0/mybatis-3.6.0-sources.jar |
| 抓取时间 | 2026-11-25T14:59:22.924Z |

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
   26 | public class EnumTypeHandler<E extends Enum<E>> extends BaseTypeHandler<E> {
   27 | 
   28 |   private final Class<E> type;
   29 | 
   30 |   public EnumTypeHandler(Class<E> type) {
   31 |     if (type == null) {
   32 |       throw new IllegalArgumentException("Type argument cannot be null");
   33 |     }
   34 |     this.type = type;
   35 |   }
   36 | 
   37 |   @Override
   38 |   public void setNonNullParameter(PreparedStatement ps, int i, E parameter, JdbcType jdbcType) throws SQLException {
   39 |     if (jdbcType == null) {
   40 |       ps.setString(i, parameter.name());
   41 |     } else {
   42 |       ps.setObject(i, parameter.name(), jdbcType.TYPE_CODE); // see r3589
   43 |     }
   44 |   }
   45 | 
   46 |   @Override
   47 |   public E getNullableResult(ResultSet rs, String columnName) throws SQLException {
   48 |     String s = rs.getString(columnName);
   49 |     return s == null ? null : Enum.valueOf(type, s);
   50 |   }
   51 | 
   52 |   @Override
   53 |   public E getNullableResult(ResultSet rs, int columnIndex) throws SQLException {
   54 |     String s = rs.getString(columnIndex);
   55 |     return s == null ? null : Enum.valueOf(type, s);
   56 |   }
   57 | 
   58 |   @Override
   59 |   public E getNullableResult(CallableStatement cs, int columnIndex) throws SQLException {
   60 |     String s = cs.getString(columnIndex);
   61 |     return s == null ? null : Enum.valueOf(type, s);
   62 |   }
   63 | }
```
