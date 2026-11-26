<!-- learning-docs source archive（机械抓取原文，禁止手改代码内容） -->
<!-- origin: org.mybatis:mybatis:3.6.0 | path: org/apache/ibatis/builder/ParameterExpression.java -->
<!-- 正文引用：[[源码:ParameterExpression.java#L起-L止]] ；代码块头部：(src: ParameterExpression.java#L起-L止) -->

# ParameterExpression.java

| 项 | 值 |
| --- | --- |
| 来源 | `org.mybatis:mybatis:3.6.0` |
| 路径 | `org/apache/ibatis/builder/ParameterExpression.java` |
| 行数 | 139 |
| 在线地址 | https://repo1.maven.org/maven2/org/mybatis/mybatis/3.6.0/mybatis-3.6.0-sources.jar |
| 抓取时间 | 2026-11-25T15:03:14.937Z |

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
   16 | package org.apache.ibatis.builder;
   17 | 
   18 | import java.util.HashMap;
   19 | 
   20 | /**
   21 |  * Inline parameter expression parser. Supported grammar (simplified):
   22 |  *
   23 |  * <pre>
   24 |  * inline-parameter = (propertyName | expression) oldJdbcType attributes
   25 |  * propertyName = /expression language's property navigation path/
   26 |  * expression = '(' /expression language's expression/ ')'
   27 |  * oldJdbcType = ':' /any valid jdbc type/
   28 |  * attributes = (',' attribute)*
   29 |  * attribute = name '=' value
   30 |  * </pre>
   31 |  *
   32 |  * @author Frank D. Martinez [mnesarco]
   33 |  */
   34 | public class ParameterExpression extends HashMap<String, String> {
   35 | 
   36 |   private static final long serialVersionUID = -2417552199605158680L;
   37 | 
   38 |   public ParameterExpression(String expression) {
   39 |     parse(expression);
   40 |   }
   41 | 
   42 |   private void parse(String expression) {
   43 |     int p = skipWS(expression, 0);
   44 |     if (expression.charAt(p) == '(') {
   45 |       expression(expression, p + 1);
   46 |     } else {
   47 |       property(expression, p);
   48 |     }
   49 |   }
   50 | 
   51 |   private void expression(String expression, int left) {
   52 |     int match = 1;
   53 |     int right = left + 1;
   54 |     while (match > 0) {
   55 |       if (expression.charAt(right) == ')') {
   56 |         match--;
   57 |       } else if (expression.charAt(right) == '(') {
   58 |         match++;
   59 |       }
   60 |       right++;
   61 |     }
   62 |     put("expression", expression.substring(left, right - 1));
   63 |     jdbcTypeOpt(expression, right);
   64 |   }
   65 | 
   66 |   private void property(String expression, int left) {
   67 |     if (left < expression.length()) {
   68 |       int right = skipUntil(expression, left, ",:");
   69 |       put("property", trimmedStr(expression, left, right));
   70 |       jdbcTypeOpt(expression, right);
   71 |     }
   72 |   }
   73 | 
   74 |   private int skipWS(String expression, int p) {
   75 |     for (int i = p; i < expression.length(); i++) {
   76 |       if (expression.charAt(i) > 0x20) {
   77 |         return i;
   78 |       }
   79 |     }
   80 |     return expression.length();
   81 |   }
   82 | 
   83 |   private int skipUntil(String expression, int p, final String endChars) {
   84 |     for (int i = p; i < expression.length(); i++) {
   85 |       char c = expression.charAt(i);
   86 |       if (endChars.indexOf(c) > -1) {
   87 |         return i;
   88 |       }
   89 |     }
   90 |     return expression.length();
   91 |   }
   92 | 
   93 |   private void jdbcTypeOpt(String expression, int p) {
   94 |     p = skipWS(expression, p);
   95 |     if (p < expression.length()) {
   96 |       if (expression.charAt(p) == ':') {
   97 |         jdbcType(expression, p + 1);
   98 |       } else if (expression.charAt(p) == ',') {
   99 |         option(expression, p + 1);
  100 |       } else {
  101 |         throw new BuilderException("Parsing error in {" + expression + "} in position " + p);
  102 |       }
  103 |     }
  104 |   }
  105 | 
  106 |   private void jdbcType(String expression, int p) {
  107 |     int left = skipWS(expression, p);
  108 |     int right = skipUntil(expression, left, ",");
  109 |     if (right <= left) {
  110 |       throw new BuilderException("Parsing error in {" + expression + "} in position " + p);
  111 |     }
  112 |     put("jdbcType", trimmedStr(expression, left, right));
  113 |     option(expression, right + 1);
  114 |   }
  115 | 
  116 |   private void option(String expression, int p) {
  117 |     int left = skipWS(expression, p);
  118 |     if (left < expression.length()) {
  119 |       int right = skipUntil(expression, left, "=");
  120 |       String name = trimmedStr(expression, left, right);
  121 |       left = right + 1;
  122 |       right = skipUntil(expression, left, ",");
  123 |       String value = trimmedStr(expression, left, right);
  124 |       put(name, value);
  125 |       option(expression, right + 1);
  126 |     }
  127 |   }
  128 | 
  129 |   private String trimmedStr(String str, int start, int end) {
  130 |     while (str.charAt(start) <= 0x20) {
  131 |       start++;
  132 |     }
  133 |     while (str.charAt(end - 1) <= 0x20) {
  134 |       end--;
  135 |     }
  136 |     return start >= end ? "" : str.substring(start, end);
  137 |   }
  138 | 
  139 | }
```
