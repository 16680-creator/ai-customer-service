<!-- learning-docs source archive（机械抓取原文，禁止手改代码内容） -->
<!-- origin: org.mybatis:mybatis:3.6.0 | path: org/apache/ibatis/scripting/xmltags/ExpressionEvaluator.java -->
<!-- 正文引用：[[源码:ExpressionEvaluator.java#L起-L止]] ；代码块头部：(src: ExpressionEvaluator.java#L起-L止) -->

# ExpressionEvaluator.java

| 项 | 值 |
| --- | --- |
| 来源 | `org.mybatis:mybatis:3.6.0` |
| 路径 | `org/apache/ibatis/scripting/xmltags/ExpressionEvaluator.java` |
| 行数 | 85 |
| 在线地址 | https://repo1.maven.org/maven2/org/mybatis/mybatis/3.6.0/mybatis-3.6.0-sources.jar |
| 抓取时间 | 2026-11-25T14:59:17.447Z |

```java
    1 | /*
    2 |  *    Copyright 2009-2024 the original author or authors.
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
   18 | import java.lang.reflect.Array;
   19 | import java.math.BigDecimal;
   20 | import java.util.ArrayList;
   21 | import java.util.List;
   22 | import java.util.Map;
   23 | 
   24 | import org.apache.ibatis.builder.BuilderException;
   25 | 
   26 | /**
   27 |  * @author Clinton Begin
   28 |  */
   29 | public class ExpressionEvaluator {
   30 | 
   31 |   public static final ExpressionEvaluator INSTANCE = new ExpressionEvaluator();
   32 | 
   33 |   public boolean evaluateBoolean(String expression, Object parameterObject) {
   34 |     Object value = OgnlCache.getValue(expression, parameterObject);
   35 |     if (value instanceof Boolean) {
   36 |       return (Boolean) value;
   37 |     }
   38 |     if (value instanceof Number) {
   39 |       return new BigDecimal(String.valueOf(value)).compareTo(BigDecimal.ZERO) != 0;
   40 |     }
   41 |     return value != null;
   42 |   }
   43 | 
   44 |   /**
   45 |    * @deprecated Since 3.5.9, use the {@link #evaluateIterable(String, Object, boolean)}.
   46 |    */
   47 |   @Deprecated
   48 |   public Iterable<?> evaluateIterable(String expression, Object parameterObject) {
   49 |     return evaluateIterable(expression, parameterObject, false);
   50 |   }
   51 | 
   52 |   /**
   53 |    * @since 3.5.9
   54 |    */
   55 |   public Iterable<?> evaluateIterable(String expression, Object parameterObject, boolean nullable) {
   56 |     Object value = OgnlCache.getValue(expression, parameterObject);
   57 |     if (value == null) {
   58 |       if (nullable) {
   59 |         return null;
   60 |       }
   61 |       throw new BuilderException("The expression '" + expression + "' evaluated to a null value.");
   62 |     }
   63 |     if (value instanceof Iterable) {
   64 |       return (Iterable<?>) value;
   65 |     }
   66 |     if (value.getClass().isArray()) {
   67 |       // the array may be primitive, so Arrays.asList() may throw
   68 |       // a ClassCastException (issue 209). Do the work manually
   69 |       // Curse primitives! :) (JGB)
   70 |       int size = Array.getLength(value);
   71 |       List<Object> answer = new ArrayList<>();
   72 |       for (int i = 0; i < size; i++) {
   73 |         Object o = Array.get(value, i);
   74 |         answer.add(o);
   75 |       }
   76 |       return answer;
   77 |     }
   78 |     if (value instanceof Map) {
   79 |       return ((Map) value).entrySet();
   80 |     }
   81 |     throw new BuilderException(
   82 |         "Error evaluating expression '" + expression + "'.  Return value (" + value + ") was not iterable.");
   83 |   }
   84 | 
   85 | }
```
