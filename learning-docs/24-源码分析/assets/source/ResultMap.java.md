<!-- learning-docs source archive（机械抓取原文，禁止手改代码内容） -->
<!-- origin: org.mybatis:mybatis:3.6.0 | path: org/apache/ibatis/mapping/ResultMap.java -->
<!-- 正文引用：[[源码:ResultMap.java#L起-L止]] ；代码块头部：(src: ResultMap.java#L起-L止) -->

# ResultMap.java

| 项 | 值 |
| --- | --- |
| 来源 | `org.mybatis:mybatis:3.6.0` |
| 路径 | `org/apache/ibatis/mapping/ResultMap.java` |
| 行数 | 210 |
| 在线地址 | https://repo1.maven.org/maven2/org/mybatis/mybatis/3.6.0/mybatis-3.6.0-sources.jar |
| 抓取时间 | 2026-11-25T14:59:19.995Z |

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
   19 | import java.util.HashSet;
   20 | import java.util.List;
   21 | import java.util.Locale;
   22 | import java.util.Set;
   23 | 
   24 | import org.apache.ibatis.session.Configuration;
   25 | import org.apache.ibatis.type.JdbcType;
   26 | 
   27 | /**
   28 |  * @author Clinton Begin
   29 |  */
   30 | public class ResultMap {
   31 |   private Configuration configuration;
   32 | 
   33 |   private String id;
   34 |   private Class<?> type;
   35 |   private List<ResultMapping> resultMappings;
   36 |   private List<ResultMapping> idResultMappings;
   37 |   private List<ResultMapping> constructorResultMappings;
   38 |   private List<ResultMapping> propertyResultMappings;
   39 |   private Set<String> mappedColumns;
   40 |   private Set<String> mappedProperties;
   41 |   private Discriminator discriminator;
   42 |   private boolean hasResultMapsUsingConstructorCollection;
   43 |   private boolean hasNestedResultMaps;
   44 |   private boolean hasNestedQueries;
   45 |   private Boolean autoMapping;
   46 | 
   47 |   private ResultMap() {
   48 |   }
   49 | 
   50 |   public static ResultMap buildEmpty(Configuration configuration, String statementId, Class<?> resultType) {
   51 |     ResultMap emptyResultMap = new ResultMap();
   52 |     emptyResultMap.configuration = configuration;
   53 |     emptyResultMap.id = statementId;
   54 |     emptyResultMap.type = resultType;
   55 |     emptyResultMap.resultMappings = List.of();
   56 |     emptyResultMap.idResultMappings = List.of();
   57 |     emptyResultMap.constructorResultMappings = List.of();
   58 |     emptyResultMap.propertyResultMappings = List.of();
   59 |     emptyResultMap.mappedColumns = Set.of();
   60 |     emptyResultMap.mappedProperties = Set.of();
   61 |     return emptyResultMap;
   62 |   }
   63 | 
   64 |   public static class Builder {
   65 |     private final ResultMap resultMap = new ResultMap();
   66 | 
   67 |     public Builder(Configuration configuration, String id, Class<?> type, List<ResultMapping> resultMappings) {
   68 |       this(configuration, id, type, resultMappings, null);
   69 |     }
   70 | 
   71 |     public Builder(Configuration configuration, String id, Class<?> type, List<ResultMapping> resultMappings,
   72 |         Boolean autoMapping) {
   73 |       resultMap.configuration = configuration;
   74 |       resultMap.id = id;
   75 |       resultMap.type = type;
   76 |       resultMap.resultMappings = resultMappings;
   77 |       resultMap.autoMapping = autoMapping;
   78 |     }
   79 | 
   80 |     public Builder discriminator(Discriminator discriminator) {
   81 |       resultMap.discriminator = discriminator;
   82 |       return this;
   83 |     }
   84 | 
   85 |     public Class<?> type() {
   86 |       return resultMap.type;
   87 |     }
   88 | 
   89 |     public ResultMap build() {
   90 |       if (resultMap.id == null) {
   91 |         throw new IllegalArgumentException("ResultMaps must have an id");
   92 |       }
   93 | 
   94 |       resultMap.mappedColumns = new HashSet<>();
   95 |       resultMap.mappedProperties = new HashSet<>();
   96 |       resultMap.idResultMappings = new ArrayList<>();
   97 |       resultMap.constructorResultMappings = new ArrayList<>();
   98 |       resultMap.propertyResultMappings = new ArrayList<>();
   99 | 
  100 |       for (ResultMapping resultMapping : resultMap.resultMappings) {
  101 |         resultMap.hasNestedQueries = resultMap.hasNestedQueries || resultMapping.getNestedQueryId() != null;
  102 |         resultMap.hasNestedResultMaps = resultMap.hasNestedResultMaps || resultMapping.getNestedResultMapId() != null
  103 |             && resultMapping.getResultSet() == null && !JdbcType.CURSOR.equals(resultMapping.getJdbcType());
  104 |         final String column = resultMapping.getColumn();
  105 |         if (column != null) {
  106 |           resultMap.mappedColumns.add(column.toUpperCase(Locale.ENGLISH));
  107 |         } else if (resultMapping.isCompositeResult()) {
  108 |           for (ResultMapping compositeResultMapping : resultMapping.getComposites()) {
  109 |             final String compositeColumn = compositeResultMapping.getColumn();
  110 |             if (compositeColumn != null) {
  111 |               resultMap.mappedColumns.add(compositeColumn.toUpperCase(Locale.ENGLISH));
  112 |             }
  113 |           }
  114 |         }
  115 | 
  116 |         final String property = resultMapping.getProperty();
  117 |         if (property != null) {
  118 |           resultMap.mappedProperties.add(property);
  119 |         }
  120 | 
  121 |         if (resultMapping.getFlags().contains(ResultFlag.CONSTRUCTOR)) {
  122 |           resultMap.constructorResultMappings.add(resultMapping);
  123 | 
  124 |           // #101
  125 |           Class<?> javaType = resultMapping.getJavaType();
  126 |           resultMap.hasResultMapsUsingConstructorCollection = resultMap.hasResultMapsUsingConstructorCollection
  127 |               || (resultMapping.getNestedQueryId() == null && resultMapping.getTypeHandler() == null && javaType != null
  128 |                   && resultMap.configuration.getObjectFactory().isCollection(javaType));
  129 |         } else {
  130 |           resultMap.propertyResultMappings.add(resultMapping);
  131 |         }
  132 | 
  133 |         if (resultMapping.getFlags().contains(ResultFlag.ID)) {
  134 |           resultMap.idResultMappings.add(resultMapping);
  135 |         }
  136 |       }
  137 | 
  138 |       if (resultMap.idResultMappings.isEmpty()) {
  139 |         resultMap.idResultMappings.addAll(resultMap.resultMappings);
  140 |       }
  141 | 
  142 |       // lock down collections
  143 |       resultMap.resultMappings = List.copyOf(resultMap.resultMappings);
  144 |       resultMap.idResultMappings = List.copyOf(resultMap.idResultMappings);
  145 |       resultMap.constructorResultMappings = List.copyOf(resultMap.constructorResultMappings);
  146 |       resultMap.propertyResultMappings = List.copyOf(resultMap.propertyResultMappings);
  147 |       resultMap.mappedColumns = Set.copyOf(resultMap.mappedColumns);
  148 |       resultMap.mappedProperties = Set.copyOf(resultMap.mappedProperties);
  149 | 
  150 |       return resultMap;
  151 |     }
  152 |   }
  153 | 
  154 |   public String getId() {
  155 |     return id;
  156 |   }
  157 | 
  158 |   public boolean hasResultMapsUsingConstructorCollection() {
  159 |     return hasResultMapsUsingConstructorCollection;
  160 |   }
  161 | 
  162 |   public boolean hasNestedResultMaps() {
  163 |     return hasNestedResultMaps;
  164 |   }
  165 | 
  166 |   public boolean hasNestedQueries() {
  167 |     return hasNestedQueries;
  168 |   }
  169 | 
  170 |   public Class<?> getType() {
  171 |     return type;
  172 |   }
  173 | 
  174 |   public List<ResultMapping> getResultMappings() {
  175 |     return resultMappings;
  176 |   }
  177 | 
  178 |   public List<ResultMapping> getConstructorResultMappings() {
  179 |     return constructorResultMappings;
  180 |   }
  181 | 
  182 |   public List<ResultMapping> getPropertyResultMappings() {
  183 |     return propertyResultMappings;
  184 |   }
  185 | 
  186 |   public List<ResultMapping> getIdResultMappings() {
  187 |     return idResultMappings;
  188 |   }
  189 | 
  190 |   public Set<String> getMappedColumns() {
  191 |     return mappedColumns;
  192 |   }
  193 | 
  194 |   public Set<String> getMappedProperties() {
  195 |     return mappedProperties;
  196 |   }
  197 | 
  198 |   public Discriminator getDiscriminator() {
  199 |     return discriminator;
  200 |   }
  201 | 
  202 |   public void forceNestedResultMaps() {
  203 |     hasNestedResultMaps = true;
  204 |   }
  205 | 
  206 |   public Boolean getAutoMapping() {
  207 |     return autoMapping;
  208 |   }
  209 | 
  210 | }
```
