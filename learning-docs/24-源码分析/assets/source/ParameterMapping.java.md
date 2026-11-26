<!-- learning-docs source archive（机械抓取原文，禁止手改代码内容） -->
<!-- origin: org.mybatis:mybatis:3.6.0 | path: org/apache/ibatis/mapping/ParameterMapping.java -->
<!-- 正文引用：[[源码:ParameterMapping.java#L起-L止]] ；代码块头部：(src: ParameterMapping.java#L起-L止) -->

# ParameterMapping.java

| 项 | 值 |
| --- | --- |
| 来源 | `org.mybatis:mybatis:3.6.0` |
| 路径 | `org/apache/ibatis/mapping/ParameterMapping.java` |
| 行数 | 222 |
| 在线地址 | https://repo1.maven.org/maven2/org/mybatis/mybatis/3.6.0/mybatis-3.6.0-sources.jar |
| 抓取时间 | 2026-11-25T14:59:19.630Z |

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
   16 | package org.apache.ibatis.mapping;
   17 | 
   18 | import java.sql.ResultSet;
   19 | 
   20 | import org.apache.ibatis.session.Configuration;
   21 | import org.apache.ibatis.type.JdbcType;
   22 | import org.apache.ibatis.type.TypeHandler;
   23 | 
   24 | /**
   25 |  * @author Clinton Begin
   26 |  */
   27 | public class ParameterMapping {
   28 | 
   29 |   private static final Object UNSET = new Object();
   30 |   private Configuration configuration;
   31 | 
   32 |   private String property;
   33 |   private ParameterMode mode;
   34 |   private Class<?> javaType = Object.class;
   35 |   private JdbcType jdbcType;
   36 |   private Integer numericScale;
   37 |   private TypeHandler<?> typeHandler;
   38 |   private String resultMapId;
   39 |   private String jdbcTypeName;
   40 |   private String expression;
   41 |   private Object value = UNSET;
   42 | 
   43 |   private ParameterMapping() {
   44 |   }
   45 | 
   46 |   public static class Builder {
   47 |     private final ParameterMapping parameterMapping = new ParameterMapping();
   48 | 
   49 |     public Builder(Configuration configuration, String property, TypeHandler<?> typeHandler) {
   50 |       parameterMapping.configuration = configuration;
   51 |       parameterMapping.property = property;
   52 |       parameterMapping.typeHandler = typeHandler;
   53 |       parameterMapping.mode = ParameterMode.IN;
   54 |     }
   55 | 
   56 |     public Builder(Configuration configuration, String property, Class<?> javaType) {
   57 |       parameterMapping.configuration = configuration;
   58 |       parameterMapping.property = property;
   59 |       parameterMapping.javaType = javaType;
   60 |       parameterMapping.mode = ParameterMode.IN;
   61 |     }
   62 | 
   63 |     public Builder mode(ParameterMode mode) {
   64 |       parameterMapping.mode = mode;
   65 |       return this;
   66 |     }
   67 | 
   68 |     public Builder javaType(Class<?> javaType) {
   69 |       parameterMapping.javaType = javaType;
   70 |       return this;
   71 |     }
   72 | 
   73 |     public Builder jdbcType(JdbcType jdbcType) {
   74 |       parameterMapping.jdbcType = jdbcType;
   75 |       return this;
   76 |     }
   77 | 
   78 |     public Builder numericScale(Integer numericScale) {
   79 |       parameterMapping.numericScale = numericScale;
   80 |       return this;
   81 |     }
   82 | 
   83 |     public Builder resultMapId(String resultMapId) {
   84 |       parameterMapping.resultMapId = resultMapId;
   85 |       return this;
   86 |     }
   87 | 
   88 |     public Builder typeHandler(TypeHandler<?> typeHandler) {
   89 |       parameterMapping.typeHandler = typeHandler;
   90 |       return this;
   91 |     }
   92 | 
   93 |     public Builder jdbcTypeName(String jdbcTypeName) {
   94 |       parameterMapping.jdbcTypeName = jdbcTypeName;
   95 |       return this;
   96 |     }
   97 | 
   98 |     public Builder expression(String expression) {
   99 |       parameterMapping.expression = expression;
  100 |       return this;
  101 |     }
  102 | 
  103 |     public Builder value(Object value) {
  104 |       parameterMapping.value = value;
  105 |       return this;
  106 |     }
  107 | 
  108 |     public ParameterMapping build() {
  109 |       validate();
  110 |       return parameterMapping;
  111 |     }
  112 | 
  113 |     private void validate() {
  114 |       if (ResultSet.class.equals(parameterMapping.javaType) && parameterMapping.resultMapId == null) {
  115 |         throw new IllegalStateException("Missing resultMap in property '" + parameterMapping.property + "'.  "
  116 |             + "Parameters of type java.sql.ResultSet require a resultMap.");
  117 |       }
  118 |     }
  119 |   }
  120 | 
  121 |   public String getProperty() {
  122 |     return property;
  123 |   }
  124 | 
  125 |   /**
  126 |    * Used for handling output of callable statements.
  127 |    *
  128 |    * @return the mode
  129 |    */
  130 |   public ParameterMode getMode() {
  131 |     return mode;
  132 |   }
  133 | 
  134 |   /**
  135 |    * Used for handling output of callable statements.
  136 |    *
  137 |    * @return the java type
  138 |    */
  139 |   public Class<?> getJavaType() {
  140 |     return javaType;
  141 |   }
  142 | 
  143 |   /**
  144 |    * Used in the UnknownTypeHandler in case there is no handler for the property type.
  145 |    *
  146 |    * @return the jdbc type
  147 |    */
  148 |   public JdbcType getJdbcType() {
  149 |     return jdbcType;
  150 |   }
  151 | 
  152 |   /**
  153 |    * Used for handling output of callable statements.
  154 |    *
  155 |    * @return the numeric scale
  156 |    */
  157 |   public Integer getNumericScale() {
  158 |     return numericScale;
  159 |   }
  160 | 
  161 |   /**
  162 |    * Used when setting parameters to the PreparedStatement.
  163 |    *
  164 |    * @return the type handler
  165 |    */
  166 |   public TypeHandler<?> getTypeHandler() {
  167 |     return typeHandler;
  168 |   }
  169 | 
  170 |   /**
  171 |    * Used for handling output of callable statements.
  172 |    *
  173 |    * @return the result map id
  174 |    */
  175 |   public String getResultMapId() {
  176 |     return resultMapId;
  177 |   }
  178 | 
  179 |   /**
  180 |    * Used for handling output of callable statements.
  181 |    *
  182 |    * @return the jdbc type name
  183 |    */
  184 |   public String getJdbcTypeName() {
  185 |     return jdbcTypeName;
  186 |   }
  187 | 
  188 |   /**
  189 |    * Expression 'Not used'.
  190 |    *
  191 |    * @return the expression
  192 |    */
  193 |   public String getExpression() {
  194 |     return expression;
  195 |   }
  196 | 
  197 |   public Object getValue() {
  198 |     return value;
  199 |   }
  200 | 
  201 |   public boolean hasValue() {
  202 |     return value != UNSET;
  203 |   }
  204 | 
  205 |   @Override
  206 |   public String toString() {
  207 |     final StringBuilder sb = new StringBuilder("ParameterMapping{");
  208 |     // sb.append("configuration=").append(configuration); // configuration doesn't have a useful .toString()
  209 |     sb.append("property='").append(property).append('\'');
  210 |     sb.append(", mode=").append(mode);
  211 |     sb.append(", javaType=").append(javaType);
  212 |     sb.append(", jdbcType=").append(jdbcType);
  213 |     sb.append(", numericScale=").append(numericScale);
  214 |     // sb.append(", typeHandler=").append(typeHandler); // typeHandler also doesn't have a useful .toString()
  215 |     sb.append(", resultMapId='").append(resultMapId).append('\'');
  216 |     sb.append(", jdbcTypeName='").append(jdbcTypeName).append('\'');
  217 |     sb.append(", expression='").append(expression).append('\'');
  218 |     sb.append(", value='").append(value).append('\'');
  219 |     sb.append('}');
  220 |     return sb.toString();
  221 |   }
  222 | }
```
