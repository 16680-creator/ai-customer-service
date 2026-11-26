<!-- learning-docs source archive（机械抓取原文，禁止手改代码内容） -->
<!-- origin: org.mybatis:mybatis:3.6.0 | path: org/apache/ibatis/scripting/xmltags/TrimSqlNode.java -->
<!-- 正文引用：[[源码:TrimSqlNode.java#L起-L止]] ；代码块头部：(src: TrimSqlNode.java#L起-L止) -->

# TrimSqlNode.java

| 项 | 值 |
| --- | --- |
| 来源 | `org.mybatis:mybatis:3.6.0` |
| 路径 | `org/apache/ibatis/scripting/xmltags/TrimSqlNode.java` |
| 行数 | 153 |
| 在线地址 | https://repo1.maven.org/maven2/org/mybatis/mybatis/3.6.0/mybatis-3.6.0-sources.jar |
| 抓取时间 | 2026-11-25T14:59:17.577Z |

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
   16 | package org.apache.ibatis.scripting.xmltags;
   17 | 
   18 | import java.util.ArrayList;
   19 | import java.util.List;
   20 | import java.util.Locale;
   21 | 
   22 | import org.apache.ibatis.mapping.ParameterMapping;
   23 | import org.apache.ibatis.session.Configuration;
   24 | 
   25 | /**
   26 |  * @author Clinton Begin
   27 |  */
   28 | public class TrimSqlNode implements SqlNode {
   29 | 
   30 |   private final SqlNode contents;
   31 |   private final String prefix;
   32 |   private final String suffix;
   33 |   private final List<String> prefixesToOverride;
   34 |   private final List<String> suffixesToOverride;
   35 |   private final Configuration configuration;
   36 | 
   37 |   public TrimSqlNode(Configuration configuration, SqlNode contents, String prefix, String prefixesToOverride,
   38 |       String suffix, String suffixesToOverride) {
   39 |     this(configuration, contents, prefix, parseOverrides(prefixesToOverride), suffix,
   40 |         parseOverrides(suffixesToOverride));
   41 |   }
   42 | 
   43 |   protected TrimSqlNode(Configuration configuration, SqlNode contents, String prefix, List<String> prefixesToOverride,
   44 |       String suffix, List<String> suffixesToOverride) {
   45 |     this.contents = contents;
   46 |     this.prefix = prefix;
   47 |     this.prefixesToOverride = prefixesToOverride;
   48 |     this.suffix = suffix;
   49 |     this.suffixesToOverride = suffixesToOverride;
   50 |     this.configuration = configuration;
   51 |   }
   52 | 
   53 |   @Override
   54 |   public boolean apply(DynamicContext context) {
   55 |     FilteredDynamicContext filteredDynamicContext = new FilteredDynamicContext(context);
   56 |     boolean result = contents.apply(filteredDynamicContext);
   57 |     filteredDynamicContext.applyAll();
   58 |     return result;
   59 |   }
   60 | 
   61 |   private static List<String> parseOverrides(final String overrides) {
   62 |     if (overrides != null) {
   63 |       final List<String> list = new ArrayList<>();
   64 |       for (final String override : overrides.split("\\|")) {
   65 |         if (!override.isEmpty()) {
   66 |           list.add(override.toUpperCase(Locale.ENGLISH));
   67 |         }
   68 |       }
   69 |       return list;
   70 |     }
   71 |     return List.of();
   72 |   }
   73 | 
   74 |   private class FilteredDynamicContext extends DynamicContext {
   75 |     private final DynamicContext delegate;
   76 |     private boolean prefixApplied;
   77 |     private boolean suffixApplied;
   78 |     private StringBuilder sqlBuffer;
   79 | 
   80 |     public FilteredDynamicContext(DynamicContext delegate) {
   81 |       super(configuration, delegate.getParameterObject(), delegate.getParameterType(), delegate.getParamNameResolver(),
   82 |           delegate.isParamExists());
   83 |       this.delegate = delegate;
   84 |       this.prefixApplied = false;
   85 |       this.suffixApplied = false;
   86 |       this.sqlBuffer = new StringBuilder();
   87 |       this.bindings.putAll(delegate.getBindings());
   88 |     }
   89 | 
   90 |     public void applyAll() {
   91 |       sqlBuffer = new StringBuilder(sqlBuffer.toString().trim());
   92 |       String trimmedUppercaseSql = sqlBuffer.toString().toUpperCase(Locale.ENGLISH);
   93 |       if (!trimmedUppercaseSql.isEmpty()) {
   94 |         applyPrefix(sqlBuffer, trimmedUppercaseSql);
   95 |         applySuffix(sqlBuffer, trimmedUppercaseSql);
   96 |       }
   97 |       delegate.appendSql(sqlBuffer.toString());
   98 |     }
   99 | 
  100 |     @Override
  101 |     public void appendSql(String sql) {
  102 |       if (sqlBuffer.length() > 0) {
  103 |         sqlBuffer.append(" ");
  104 |       }
  105 |       sqlBuffer.append(sql);
  106 |     }
  107 | 
  108 |     @Override
  109 |     public String getSql() {
  110 |       return delegate.getSql();
  111 |     }
  112 | 
  113 |     @Override
  114 |     public List<ParameterMapping> getParameterMappings() {
  115 |       return delegate.getParameterMappings();
  116 |     }
  117 | 
  118 |     private void applyPrefix(StringBuilder sql, String trimmedUppercaseSql) {
  119 |       if (prefixApplied) {
  120 |         return;
  121 |       }
  122 |       prefixApplied = true;
  123 |       if (prefixesToOverride != null) {
  124 |         prefixesToOverride.stream().filter(trimmedUppercaseSql::startsWith).findFirst()
  125 |             .ifPresent(toRemove -> sql.delete(0, toRemove.trim().length()));
  126 |       }
  127 |       if (prefix != null) {
  128 |         sql.insert(0, " ").insert(0, prefix);
  129 |       }
  130 |     }
  131 | 
  132 |     private void applySuffix(StringBuilder sql, String trimmedUppercaseSql) {
  133 |       if (suffixApplied) {
  134 |         return;
  135 |       }
  136 |       suffixApplied = true;
  137 |       if (suffixesToOverride != null) {
  138 |         suffixesToOverride.stream()
  139 |             .filter(toRemove -> trimmedUppercaseSql.endsWith(toRemove) || trimmedUppercaseSql.endsWith(toRemove.trim()))
  140 |             .findFirst().ifPresent(toRemove -> {
  141 |               int start = sql.length() - toRemove.trim().length();
  142 |               int end = sql.length();
  143 |               sql.delete(start, end);
  144 |             });
  145 |       }
  146 |       if (suffix != null) {
  147 |         sql.append(" ").append(suffix);
  148 |       }
  149 |     }
  150 | 
  151 |   }
  152 | 
  153 | }
```
