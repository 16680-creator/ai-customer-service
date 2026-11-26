<!-- learning-docs source archive（机械抓取原文，禁止手改代码内容） -->
<!-- origin: org.mybatis:mybatis:3.6.0 | path: org/apache/ibatis/type/BaseTypeHandler.java -->
<!-- 正文引用：[[源码:BaseTypeHandler.java#L起-L止]] ；代码块头部：(src: BaseTypeHandler.java#L起-L止) -->

# BaseTypeHandler.java

| 项 | 值 |
| --- | --- |
| 来源 | `org.mybatis:mybatis:3.6.0` |
| 路径 | `org/apache/ibatis/type/BaseTypeHandler.java` |
| 行数 | 134 |
| 在线地址 | https://repo1.maven.org/maven2/org/mybatis/mybatis/3.6.0/mybatis-3.6.0-sources.jar |
| 抓取时间 | 2026-11-25T14:59:23.074Z |

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
   23 | import org.apache.ibatis.executor.result.ResultMapException;
   24 | import org.apache.ibatis.session.Configuration;
   25 | 
   26 | /**
   27 |  * The base {@link TypeHandler} for references a generic type.
   28 |  * <p>
   29 |  * Important: Since 3.5.0, This class never call the {@link ResultSet#wasNull()} and {@link CallableStatement#wasNull()}
   30 |  * method for handling the SQL {@code NULL} value. In other words, {@code null} value handling should be performed on
   31 |  * subclass.
   32 |  *
   33 |  * @author Clinton Begin
   34 |  * @author Simone Tripodi
   35 |  * @author Kzuki Shimizu
   36 |  */
   37 | public abstract class BaseTypeHandler<T> implements TypeHandler<T> {
   38 | 
   39 |   /**
   40 |    * @deprecated Since 3.5.0 - See https://github.com/mybatis/mybatis-3/issues/1203. This field will remove future.
   41 |    */
   42 |   @Deprecated
   43 |   protected Configuration configuration;
   44 | 
   45 |   /**
   46 |    * Sets the configuration.
   47 |    *
   48 |    * @param c
   49 |    *          the new configuration
   50 |    *
   51 |    * @deprecated Since 3.5.0 - See https://github.com/mybatis/mybatis-3/issues/1203. This property will remove future.
   52 |    */
   53 |   @Deprecated
   54 |   public void setConfiguration(Configuration c) {
   55 |     this.configuration = c;
   56 |   }
   57 | 
   58 |   @Override
   59 |   public void setParameter(PreparedStatement ps, int i, T parameter, JdbcType jdbcType) throws SQLException {
   60 |     if (parameter == null) {
   61 |       if (jdbcType == null) {
   62 |         throw new TypeException("JDBC requires that the JdbcType must be specified for all nullable parameters.");
   63 |       }
   64 |       try {
   65 |         ps.setNull(i, jdbcType.TYPE_CODE);
   66 |       } catch (SQLException e) {
   67 |         throw new TypeException("Error setting null for parameter #" + i + " with JdbcType " + jdbcType + " . "
   68 |             + "Try setting a different JdbcType for this parameter or a different jdbcTypeForNull configuration property. "
   69 |             + "Cause: " + e, e);
   70 |       }
   71 |     } else {
   72 |       try {
   73 |         setNonNullParameter(ps, i, parameter, jdbcType);
   74 |       } catch (Exception e) {
   75 |         throw new TypeException("Error setting non null for parameter #" + i + " with JdbcType " + jdbcType + " . "
   76 |             + "Try setting a different JdbcType for this parameter or a different configuration property. " + "Cause: "
   77 |             + e, e);
   78 |       }
   79 |     }
   80 |   }
   81 | 
   82 |   @Override
   83 |   public T getResult(ResultSet rs, String columnName) throws SQLException {
   84 |     try {
   85 |       return getNullableResult(rs, columnName);
   86 |     } catch (Exception e) {
   87 |       throw new ResultMapException("Error attempting to get column '" + columnName + "' from result set.  Cause: " + e,
   88 |           e);
   89 |     }
   90 |   }
   91 | 
   92 |   @Override
   93 |   public T getResult(ResultSet rs, int columnIndex) throws SQLException {
   94 |     try {
   95 |       return getNullableResult(rs, columnIndex);
   96 |     } catch (Exception e) {
   97 |       throw new ResultMapException("Error attempting to get column #" + columnIndex + " from result set.  Cause: " + e,
   98 |           e);
   99 |     }
  100 |   }
  101 | 
  102 |   @Override
  103 |   public T getResult(CallableStatement cs, int columnIndex) throws SQLException {
  104 |     try {
  105 |       return getNullableResult(cs, columnIndex);
  106 |     } catch (Exception e) {
  107 |       throw new ResultMapException(
  108 |           "Error attempting to get column #" + columnIndex + " from callable statement.  Cause: " + e, e);
  109 |     }
  110 |   }
  111 | 
  112 |   public abstract void setNonNullParameter(PreparedStatement ps, int i, T parameter, JdbcType jdbcType)
  113 |       throws SQLException;
  114 | 
  115 |   /**
  116 |    * Gets the nullable result.
  117 |    *
  118 |    * @param rs
  119 |    *          the rs
  120 |    * @param columnName
  121 |    *          Column name, when configuration <code>useColumnLabel</code> is <code>false</code>
  122 |    *
  123 |    * @return the nullable result
  124 |    *
  125 |    * @throws SQLException
  126 |    *           the SQL exception
  127 |    */
  128 |   public abstract T getNullableResult(ResultSet rs, String columnName) throws SQLException;
  129 | 
  130 |   public abstract T getNullableResult(ResultSet rs, int columnIndex) throws SQLException;
  131 | 
  132 |   public abstract T getNullableResult(CallableStatement cs, int columnIndex) throws SQLException;
  133 | 
  134 | }
```
