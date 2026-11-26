<!-- learning-docs source archive（机械抓取原文，禁止手改代码内容） -->
<!-- origin: org.mybatis:mybatis:3.6.0 | path: org/apache/ibatis/mapping/ResultMapping.java -->
<!-- 正文引用：[[源码:ResultMapping.java#L起-L止]] ；代码块头部：(src: ResultMapping.java#L起-L止) -->

# ResultMapping.java

| 项 | 值 |
| --- | --- |
| 来源 | `org.mybatis:mybatis:3.6.0` |
| 路径 | `org/apache/ibatis/mapping/ResultMapping.java` |
| 行数 | 315 |
| 在线地址 | https://repo1.maven.org/maven2/org/mybatis/mybatis/3.6.0/mybatis-3.6.0-sources.jar |
| 抓取时间 | 2026-11-25T14:59:20.431Z |

```java
    1 | /*
    2 |  *    Copyright 2009-2026 the original author or authors.
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
   18 | import java.util.ArrayList;
   19 | import java.util.List;
   20 | import java.util.Set;
   21 | 
   22 | import org.apache.ibatis.session.Configuration;
   23 | import org.apache.ibatis.type.JdbcType;
   24 | import org.apache.ibatis.type.TypeHandler;
   25 | 
   26 | /**
   27 |  * @author Clinton Begin
   28 |  */
   29 | public class ResultMapping {
   30 | 
   31 |   private Configuration configuration;
   32 |   private String property;
   33 |   private String column;
   34 |   private Class<?> javaType;
   35 |   private JdbcType jdbcType;
   36 |   private TypeHandler<?> typeHandler;
   37 |   private String nestedResultMapId;
   38 |   private String nestedQueryId;
   39 |   private Set<String> notNullColumns;
   40 |   private String columnPrefix;
   41 |   private List<ResultFlag> flags;
   42 |   private List<ResultMapping> composites;
   43 |   private String resultSet;
   44 |   private String foreignColumn;
   45 |   private boolean lazy;
   46 | 
   47 |   ResultMapping() {
   48 |   }
   49 | 
   50 |   public static class Builder {
   51 |     private final ResultMapping resultMapping = new ResultMapping();
   52 | 
   53 |     public Builder(Configuration configuration, String property, String column, TypeHandler<?> typeHandler) {
   54 |       this(configuration, property);
   55 |       resultMapping.column = column;
   56 |       resultMapping.typeHandler = typeHandler;
   57 |     }
   58 | 
   59 |     public Builder(Configuration configuration, String property, String column, Class<?> javaType) {
   60 |       this(configuration, property);
   61 |       resultMapping.column = column;
   62 |       resultMapping.javaType = javaType;
   63 |     }
   64 | 
   65 |     public Builder(Configuration configuration, String property) {
   66 |       resultMapping.configuration = configuration;
   67 |       resultMapping.property = property;
   68 |       resultMapping.flags = new ArrayList<>();
   69 |       resultMapping.composites = new ArrayList<>();
   70 |       resultMapping.lazy = configuration.isLazyLoadingEnabled();
   71 |     }
   72 | 
   73 |     public Builder(ResultMapping otherMapping) {
   74 |       this(otherMapping.configuration, otherMapping.property);
   75 | 
   76 |       resultMapping.flags.addAll(otherMapping.flags);
   77 |       resultMapping.composites.addAll(otherMapping.composites);
   78 | 
   79 |       resultMapping.column = otherMapping.column;
   80 |       resultMapping.javaType = otherMapping.javaType;
   81 |       resultMapping.jdbcType = otherMapping.jdbcType;
   82 |       resultMapping.typeHandler = otherMapping.typeHandler;
   83 |       resultMapping.nestedResultMapId = otherMapping.nestedResultMapId;
   84 |       resultMapping.nestedQueryId = otherMapping.nestedQueryId;
   85 |       resultMapping.notNullColumns = otherMapping.notNullColumns;
   86 |       resultMapping.columnPrefix = otherMapping.columnPrefix;
   87 |       resultMapping.resultSet = otherMapping.resultSet;
   88 |       resultMapping.foreignColumn = otherMapping.foreignColumn;
   89 |       resultMapping.lazy = otherMapping.lazy;
   90 |     }
   91 | 
   92 |     public Builder javaType(Class<?> javaType) {
   93 |       resultMapping.javaType = javaType;
   94 |       return this;
   95 |     }
   96 | 
   97 |     public Builder jdbcType(JdbcType jdbcType) {
   98 |       resultMapping.jdbcType = jdbcType;
   99 |       return this;
  100 |     }
  101 | 
  102 |     public Builder nestedResultMapId(String nestedResultMapId) {
  103 |       resultMapping.nestedResultMapId = nestedResultMapId;
  104 |       return this;
  105 |     }
  106 | 
  107 |     public Builder nestedQueryId(String nestedQueryId) {
  108 |       resultMapping.nestedQueryId = nestedQueryId;
  109 |       return this;
  110 |     }
  111 | 
  112 |     public Builder resultSet(String resultSet) {
  113 |       resultMapping.resultSet = resultSet;
  114 |       return this;
  115 |     }
  116 | 
  117 |     public Builder foreignColumn(String foreignColumn) {
  118 |       resultMapping.foreignColumn = foreignColumn;
  119 |       return this;
  120 |     }
  121 | 
  122 |     public Builder notNullColumns(Set<String> notNullColumns) {
  123 |       resultMapping.notNullColumns = notNullColumns;
  124 |       return this;
  125 |     }
  126 | 
  127 |     public Builder columnPrefix(String columnPrefix) {
  128 |       resultMapping.columnPrefix = columnPrefix;
  129 |       return this;
  130 |     }
  131 | 
  132 |     public Builder flags(List<ResultFlag> flags) {
  133 |       resultMapping.flags = flags;
  134 |       return this;
  135 |     }
  136 | 
  137 |     public Builder typeHandler(TypeHandler<?> typeHandler) {
  138 |       resultMapping.typeHandler = typeHandler;
  139 |       return this;
  140 |     }
  141 | 
  142 |     public Builder composites(List<ResultMapping> composites) {
  143 |       resultMapping.composites = composites;
  144 |       return this;
  145 |     }
  146 | 
  147 |     public Builder lazy(boolean lazy) {
  148 |       resultMapping.lazy = lazy;
  149 |       return this;
  150 |     }
  151 | 
  152 |     public ResultMapping build() {
  153 |       // lock down collections
  154 |       resultMapping.flags = List.copyOf(resultMapping.flags);
  155 |       resultMapping.composites = List.copyOf(resultMapping.composites);
  156 |       validate();
  157 |       return resultMapping;
  158 |     }
  159 | 
  160 |     private void validate() {
  161 |       // Issue #697: cannot define both nestedQueryId and nestedResultMapId
  162 |       if (resultMapping.nestedQueryId != null && resultMapping.nestedResultMapId != null) {
  163 |         throw new IllegalStateException(
  164 |             "Cannot define both nestedQueryId and nestedResultMapId in property " + resultMapping.property);
  165 |       }
  166 |       // Issue #4 and GH #39: column is optional only in nested resultMaps but not in the rest
  167 |       if (resultMapping.nestedResultMapId == null && resultMapping.column == null
  168 |           && resultMapping.composites.isEmpty()) {
  169 |         throw new IllegalStateException("Mapping is missing column attribute for property " + resultMapping.property);
  170 |       }
  171 |       if (resultMapping.getResultSet() != null) {
  172 |         int numColumns = 0;
  173 |         if (resultMapping.column != null) {
  174 |           numColumns = resultMapping.column.split(",").length;
  175 |         }
  176 |         int numForeignColumns = 0;
  177 |         if (resultMapping.foreignColumn != null) {
  178 |           numForeignColumns = resultMapping.foreignColumn.split(",").length;
  179 |         }
  180 |         if (numColumns != numForeignColumns) {
  181 |           throw new IllegalStateException(
  182 |               "There should be the same number of columns and foreignColumns in property " + resultMapping.property);
  183 |         }
  184 |       }
  185 |     }
  186 | 
  187 |     public Builder column(String column) {
  188 |       resultMapping.column = column;
  189 |       return this;
  190 |     }
  191 |   }
  192 | 
  193 |   public String getProperty() {
  194 |     return property;
  195 |   }
  196 | 
  197 |   public String getColumn() {
  198 |     return column;
  199 |   }
  200 | 
  201 |   public Class<?> getJavaType() {
  202 |     return javaType;
  203 |   }
  204 | 
  205 |   public JdbcType getJdbcType() {
  206 |     return jdbcType;
  207 |   }
  208 | 
  209 |   public TypeHandler<?> getTypeHandler() {
  210 |     return typeHandler;
  211 |   }
  212 | 
  213 |   public String getNestedResultMapId() {
  214 |     return nestedResultMapId;
  215 |   }
  216 | 
  217 |   public String getNestedQueryId() {
  218 |     return nestedQueryId;
  219 |   }
  220 | 
  221 |   public Set<String> getNotNullColumns() {
  222 |     return notNullColumns;
  223 |   }
  224 | 
  225 |   public String getColumnPrefix() {
  226 |     return columnPrefix;
  227 |   }
  228 | 
  229 |   public List<ResultFlag> getFlags() {
  230 |     return flags;
  231 |   }
  232 | 
  233 |   public List<ResultMapping> getComposites() {
  234 |     return composites;
  235 |   }
  236 | 
  237 |   public boolean isCompositeResult() {
  238 |     return this.composites != null && !this.composites.isEmpty();
  239 |   }
  240 | 
  241 |   public String getResultSet() {
  242 |     return this.resultSet;
  243 |   }
  244 | 
  245 |   public String getForeignColumn() {
  246 |     return foreignColumn;
  247 |   }
  248 | 
  249 |   @Deprecated
  250 |   public void setForeignColumn(String foreignColumn) {
  251 |     this.foreignColumn = foreignColumn;
  252 |   }
  253 | 
  254 |   public boolean isLazy() {
  255 |     return lazy;
  256 |   }
  257 | 
  258 |   @Deprecated
  259 |   public void setLazy(boolean lazy) {
  260 |     this.lazy = lazy;
  261 |   }
  262 | 
  263 |   public boolean isSimple() {
  264 |     return this.nestedResultMapId == null && this.nestedQueryId == null && this.resultSet == null;
  265 |   }
  266 | 
  267 |   @Override
  268 |   public boolean equals(Object o) {
  269 |     if (this == o) {
  270 |       return true;
  271 |     }
  272 |     if (o == null || getClass() != o.getClass()) {
  273 |       return false;
  274 |     }
  275 | 
  276 |     ResultMapping that = (ResultMapping) o;
  277 | 
  278 |     return property != null && property.equals(that.property);
  279 |   }
  280 | 
  281 |   @Override
  282 |   public int hashCode() {
  283 |     if (property != null) {
  284 |       return property.hashCode();
  285 |     }
  286 |     if (column != null) {
  287 |       return column.hashCode();
  288 |     } else {
  289 |       return 0;
  290 |     }
  291 |   }
  292 | 
  293 |   @Override
  294 |   public String toString() {
  295 |     final StringBuilder sb = new StringBuilder("ResultMapping{");
  296 |     // sb.append("configuration=").append(configuration); // configuration doesn't have a useful .toString()
  297 |     sb.append("property='").append(property).append('\'');
  298 |     sb.append(", column='").append(column).append('\'');
  299 |     sb.append(", javaType=").append(javaType);
  300 |     sb.append(", jdbcType=").append(jdbcType);
  301 |     // sb.append(", typeHandler=").append(typeHandler); // typeHandler also doesn't have a useful .toString()
  302 |     sb.append(", nestedResultMapId='").append(nestedResultMapId).append('\'');
  303 |     sb.append(", nestedQueryId='").append(nestedQueryId).append('\'');
  304 |     sb.append(", notNullColumns=").append(notNullColumns);
  305 |     sb.append(", columnPrefix='").append(columnPrefix).append('\'');
  306 |     sb.append(", flags=").append(flags);
  307 |     sb.append(", composites=").append(composites);
  308 |     sb.append(", resultSet='").append(resultSet).append('\'');
  309 |     sb.append(", foreignColumn='").append(foreignColumn).append('\'');
  310 |     sb.append(", lazy=").append(lazy);
  311 |     sb.append('}');
  312 |     return sb.toString();
  313 |   }
  314 | 
  315 | }
```
