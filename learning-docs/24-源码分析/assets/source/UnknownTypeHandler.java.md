<!-- learning-docs source archive（机械抓取原文，禁止手改代码内容） -->
<!-- origin: org.mybatis:mybatis:3.6.0 | path: org/apache/ibatis/type/UnknownTypeHandler.java -->
<!-- 正文引用：[[源码:UnknownTypeHandler.java#L起-L止]] ；代码块头部：(src: UnknownTypeHandler.java#L起-L止) -->

# UnknownTypeHandler.java

| 项 | 值 |
| --- | --- |
| 来源 | `org.mybatis:mybatis:3.6.0` |
| 路径 | `org/apache/ibatis/type/UnknownTypeHandler.java` |
| 行数 | 162 |
| 在线地址 | https://repo1.maven.org/maven2/org/mybatis/mybatis/3.6.0/mybatis-3.6.0-sources.jar |
| 抓取时间 | 2026-11-25T15:02:23.407Z |

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
   21 | import java.sql.ResultSetMetaData;
   22 | import java.sql.SQLException;
   23 | import java.util.HashMap;
   24 | import java.util.Map;
   25 | import java.util.function.Supplier;
   26 | 
   27 | import org.apache.ibatis.io.Resources;
   28 | import org.apache.ibatis.session.Configuration;
   29 | 
   30 | /**
   31 |  * @author Clinton Begin
   32 |  */
   33 | @Deprecated(since = "3.6.0", forRemoval = true)
   34 | public class UnknownTypeHandler extends BaseTypeHandler<Object> {
   35 | 
   36 |   // TODO Rename to 'configuration' after removing the 'configuration' property(deprecated property) on parent class
   37 |   private final Configuration config;
   38 |   private final Supplier<TypeHandlerRegistry> typeHandlerRegistrySupplier;
   39 | 
   40 |   /**
   41 |    * The constructor that pass a MyBatis configuration.
   42 |    *
   43 |    * @param configuration
   44 |    *          a MyBatis configuration
   45 |    *
   46 |    * @since 3.5.4
   47 |    */
   48 |   public UnknownTypeHandler(Configuration configuration) {
   49 |     this.config = configuration;
   50 |     this.typeHandlerRegistrySupplier = configuration::getTypeHandlerRegistry;
   51 |   }
   52 | 
   53 |   /**
   54 |    * The constructor that pass the type handler registry.
   55 |    *
   56 |    * @param typeHandlerRegistry
   57 |    *          a type handler registry
   58 |    *
   59 |    * @deprecated Since 3.5.4, please use the {@link #UnknownTypeHandler(Configuration)}.
   60 |    */
   61 |   @Deprecated
   62 |   public UnknownTypeHandler(TypeHandlerRegistry typeHandlerRegistry) {
   63 |     this.config = new Configuration();
   64 |     this.typeHandlerRegistrySupplier = () -> typeHandlerRegistry;
   65 |   }
   66 | 
   67 |   @Override
   68 |   public void setNonNullParameter(PreparedStatement ps, int i, Object parameter, JdbcType jdbcType)
   69 |       throws SQLException {
   70 |     TypeHandler handler = resolveTypeHandler(parameter, jdbcType);
   71 |     handler.setParameter(ps, i, parameter, jdbcType);
   72 |   }
   73 | 
   74 |   @Override
   75 |   public Object getNullableResult(ResultSet rs, String columnName) throws SQLException {
   76 |     TypeHandler<?> handler = resolveTypeHandler(rs, columnName);
   77 |     return handler.getResult(rs, columnName);
   78 |   }
   79 | 
   80 |   @Override
   81 |   public Object getNullableResult(ResultSet rs, int columnIndex) throws SQLException {
   82 |     TypeHandler<?> handler = resolveTypeHandler(rs.getMetaData(), columnIndex);
   83 |     if (handler == null || handler instanceof UnknownTypeHandler) {
   84 |       handler = ObjectTypeHandler.INSTANCE;
   85 |     }
   86 |     return handler.getResult(rs, columnIndex);
   87 |   }
   88 | 
   89 |   @Override
   90 |   public Object getNullableResult(CallableStatement cs, int columnIndex) throws SQLException {
   91 |     return cs.getObject(columnIndex);
   92 |   }
   93 | 
   94 |   private TypeHandler<?> resolveTypeHandler(Object parameter, JdbcType jdbcType) {
   95 |     TypeHandler<?> handler;
   96 |     if (parameter == null) {
   97 |       handler = ObjectTypeHandler.INSTANCE;
   98 |     } else {
   99 |       handler = typeHandlerRegistrySupplier.get().getTypeHandler(parameter.getClass(), jdbcType);
  100 |       // check if handler is null (issue #270)
  101 |       if (handler == null || handler instanceof UnknownTypeHandler) {
  102 |         handler = ObjectTypeHandler.INSTANCE;
  103 |       }
  104 |     }
  105 |     return handler;
  106 |   }
  107 | 
  108 |   private TypeHandler<?> resolveTypeHandler(ResultSet rs, String column) {
  109 |     try {
  110 |       Map<String, Integer> columnIndexLookup;
  111 |       columnIndexLookup = new HashMap<>();
  112 |       ResultSetMetaData rsmd = rs.getMetaData();
  113 |       int count = rsmd.getColumnCount();
  114 |       boolean useColumnLabel = config.isUseColumnLabel();
  115 |       for (int i = 1; i <= count; i++) {
  116 |         String name = useColumnLabel ? rsmd.getColumnLabel(i) : rsmd.getColumnName(i);
  117 |         columnIndexLookup.put(name, i);
  118 |       }
  119 |       Integer columnIndex = columnIndexLookup.get(column);
  120 |       TypeHandler<?> handler = null;
  121 |       if (columnIndex != null) {
  122 |         handler = resolveTypeHandler(rsmd, columnIndex);
  123 |       }
  124 |       if (handler == null || handler instanceof UnknownTypeHandler) {
  125 |         handler = ObjectTypeHandler.INSTANCE;
  126 |       }
  127 |       return handler;
  128 |     } catch (SQLException e) {
  129 |       throw new TypeException("Error determining JDBC type for column " + column + ".  Cause: " + e, e);
  130 |     }
  131 |   }
  132 | 
  133 |   private TypeHandler<?> resolveTypeHandler(ResultSetMetaData rsmd, Integer columnIndex) {
  134 |     TypeHandler<?> handler = null;
  135 |     JdbcType jdbcType = safeGetJdbcTypeForColumn(rsmd, columnIndex);
  136 |     Class<?> javaType = safeGetClassForColumn(rsmd, columnIndex);
  137 |     if (javaType != null && jdbcType != null) {
  138 |       handler = typeHandlerRegistrySupplier.get().getTypeHandler(javaType, jdbcType);
  139 |     } else if (javaType != null) {
  140 |       handler = typeHandlerRegistrySupplier.get().getTypeHandler(javaType);
  141 |     } else if (jdbcType != null) {
  142 |       handler = typeHandlerRegistrySupplier.get().getTypeHandler(jdbcType);
  143 |     }
  144 |     return handler;
  145 |   }
  146 | 
  147 |   private JdbcType safeGetJdbcTypeForColumn(ResultSetMetaData rsmd, Integer columnIndex) {
  148 |     try {
  149 |       return JdbcType.forCode(rsmd.getColumnType(columnIndex));
  150 |     } catch (Exception e) {
  151 |       return null;
  152 |     }
  153 |   }
  154 | 
  155 |   private Class<?> safeGetClassForColumn(ResultSetMetaData rsmd, Integer columnIndex) {
  156 |     try {
  157 |       return Resources.classForName(rsmd.getColumnClassName(columnIndex));
  158 |     } catch (Exception e) {
  159 |       return null;
  160 |     }
  161 |   }
  162 | }
```
