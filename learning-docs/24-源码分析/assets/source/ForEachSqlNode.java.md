<!-- learning-docs source archive（机械抓取原文，禁止手改代码内容） -->
<!-- origin: org.mybatis:mybatis:3.6.0 | path: org/apache/ibatis/scripting/xmltags/ForEachSqlNode.java -->
<!-- 正文引用：[[源码:ForEachSqlNode.java#L起-L止]] ；代码块头部：(src: ForEachSqlNode.java#L起-L止) -->

# ForEachSqlNode.java

| 项 | 值 |
| --- | --- |
| 来源 | `org.mybatis:mybatis:3.6.0` |
| 路径 | `org/apache/ibatis/scripting/xmltags/ForEachSqlNode.java` |
| 行数 | 167 |
| 在线地址 | https://repo1.maven.org/maven2/org/mybatis/mybatis/3.6.0/mybatis-3.6.0-sources.jar |
| 抓取时间 | 2026-11-25T14:59:17.846Z |

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
   16 | package org.apache.ibatis.scripting.xmltags;
   17 | 
   18 | import java.util.List;
   19 | import java.util.Map;
   20 | import java.util.Optional;
   21 | 
   22 | import org.apache.ibatis.mapping.ParameterMapping;
   23 | import org.apache.ibatis.session.Configuration;
   24 | 
   25 | /**
   26 |  * @author Clinton Begin
   27 |  */
   28 | public class ForEachSqlNode implements SqlNode {
   29 | 
   30 |   private final ExpressionEvaluator evaluator = ExpressionEvaluator.INSTANCE;
   31 |   private final String collectionExpression;
   32 |   private final Boolean nullable;
   33 |   private final SqlNode contents;
   34 |   private final String open;
   35 |   private final String close;
   36 |   private final String separator;
   37 |   private final String item;
   38 |   private final String index;
   39 |   private final Configuration configuration;
   40 | 
   41 |   /**
   42 |    * @deprecated Since 3.5.9, use the
   43 |    *             {@link #ForEachSqlNode(Configuration, SqlNode, String, Boolean, String, String, String, String, String)}.
   44 |    */
   45 |   @Deprecated
   46 |   public ForEachSqlNode(Configuration configuration, SqlNode contents, String collectionExpression, String index,
   47 |       String item, String open, String close, String separator) {
   48 |     this(configuration, contents, collectionExpression, null, index, item, open, close, separator);
   49 |   }
   50 | 
   51 |   /**
   52 |    * @since 3.5.9
   53 |    */
   54 |   public ForEachSqlNode(Configuration configuration, SqlNode contents, String collectionExpression, Boolean nullable,
   55 |       String index, String item, String open, String close, String separator) {
   56 |     this.collectionExpression = collectionExpression;
   57 |     this.nullable = nullable;
   58 |     this.contents = contents;
   59 |     this.open = open;
   60 |     this.close = close;
   61 |     this.separator = separator;
   62 |     this.index = index;
   63 |     this.item = item;
   64 |     this.configuration = configuration;
   65 |   }
   66 | 
   67 |   @Override
   68 |   public boolean apply(DynamicContext context) {
   69 |     Map<String, Object> bindings = context.getBindings();
   70 |     final Iterable<?> iterable = evaluator.evaluateIterable(collectionExpression, bindings,
   71 |         Optional.ofNullable(nullable).orElseGet(configuration::isNullableOnForEach));
   72 |     if (iterable == null || !iterable.iterator().hasNext()) {
   73 |       return true;
   74 |     }
   75 |     boolean first = true;
   76 |     applyOpen(context);
   77 |     int i = 0;
   78 |     for (Object o : iterable) {
   79 |       DynamicContext scopedContext;
   80 |       if (first || separator == null) {
   81 |         scopedContext = new PrefixedContext(context, "");
   82 |       } else {
   83 |         scopedContext = new PrefixedContext(context, separator);
   84 |       }
   85 |       // Issue #709
   86 |       if (o instanceof Map.Entry) {
   87 |         @SuppressWarnings("unchecked")
   88 |         Map.Entry<Object, Object> mapEntry = (Map.Entry<Object, Object>) o;
   89 |         applyIndex(scopedContext, mapEntry.getKey());
   90 |         applyItem(scopedContext, mapEntry.getValue());
   91 |       } else {
   92 |         applyIndex(scopedContext, i);
   93 |         applyItem(scopedContext, o);
   94 |       }
   95 |       contents.apply(scopedContext);
   96 |       if (first) {
   97 |         first = !((PrefixedContext) scopedContext).isPrefixApplied();
   98 |       }
   99 |       i++;
  100 |     }
  101 |     applyClose(context);
  102 |     return true;
  103 |   }
  104 | 
  105 |   private void applyIndex(DynamicContext context, Object o) {
  106 |     if (index != null) {
  107 |       context.bind(index, o);
  108 |     }
  109 |   }
  110 | 
  111 |   private void applyItem(DynamicContext context, Object o) {
  112 |     if (item != null) {
  113 |       context.bind(item, o);
  114 |     }
  115 |   }
  116 | 
  117 |   private void applyOpen(DynamicContext context) {
  118 |     if (open != null) {
  119 |       context.appendSql(open);
  120 |     }
  121 |   }
  122 | 
  123 |   private void applyClose(DynamicContext context) {
  124 |     if (close != null) {
  125 |       context.appendSql(close);
  126 |     }
  127 |   }
  128 | 
  129 |   private class PrefixedContext extends DynamicContext {
  130 |     private final DynamicContext delegate;
  131 |     private final String prefix;
  132 |     private boolean prefixApplied;
  133 | 
  134 |     public PrefixedContext(DynamicContext delegate, String prefix) {
  135 |       super(configuration, delegate.getParameterObject(), delegate.getParameterType(), delegate.getParamNameResolver(),
  136 |           delegate.isParamExists());
  137 |       this.delegate = delegate;
  138 |       this.prefix = prefix;
  139 |       this.prefixApplied = false;
  140 |       this.bindings.putAll(delegate.getBindings());
  141 |     }
  142 | 
  143 |     public boolean isPrefixApplied() {
  144 |       return prefixApplied;
  145 |     }
  146 | 
  147 |     @Override
  148 |     public void appendSql(String sql) {
  149 |       if (!prefixApplied && sql != null && sql.trim().length() > 0) {
  150 |         delegate.appendSql(prefix);
  151 |         prefixApplied = true;
  152 |       }
  153 |       delegate.appendSql(sql);
  154 |     }
  155 | 
  156 |     @Override
  157 |     public String getSql() {
  158 |       return delegate.getSql();
  159 |     }
  160 | 
  161 |     @Override
  162 |     public List<ParameterMapping> getParameterMappings() {
  163 |       return delegate.getParameterMappings();
  164 |     }
  165 |   }
  166 | 
  167 | }
```
