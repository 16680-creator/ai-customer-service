<!-- learning-docs source archive（机械抓取原文，禁止手改代码内容） -->
<!-- origin: org.mybatis:mybatis:3.6.0 | path: org/apache/ibatis/parsing/GenericTokenParser.java -->
<!-- 正文引用：[[源码:GenericTokenParser.java#L起-L止]] ；代码块头部：(src: GenericTokenParser.java#L起-L止) -->

# GenericTokenParser.java

| 项 | 值 |
| --- | --- |
| 来源 | `org.mybatis:mybatis:3.6.0` |
| 路径 | `org/apache/ibatis/parsing/GenericTokenParser.java` |
| 行数 | 87 |
| 在线地址 | https://repo1.maven.org/maven2/org/mybatis/mybatis/3.6.0/mybatis-3.6.0-sources.jar |
| 抓取时间 | 2026-11-25T14:00:23.627Z |

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
   16 | package org.apache.ibatis.parsing;
   17 | 
   18 | /**
   19 |  * @author Clinton Begin
   20 |  */
   21 | public class GenericTokenParser {
   22 | 
   23 |   private final String openToken;
   24 |   private final String closeToken;
   25 |   private final TokenHandler handler;
   26 | 
   27 |   public GenericTokenParser(String openToken, String closeToken, TokenHandler handler) {
   28 |     this.openToken = openToken;
   29 |     this.closeToken = closeToken;
   30 |     this.handler = handler;
   31 |   }
   32 | 
   33 |   public String parse(String text) {
   34 |     if (text == null || text.isEmpty()) {
   35 |       return "";
   36 |     }
   37 |     // search open token
   38 |     int start = text.indexOf(openToken);
   39 |     if (start == -1) {
   40 |       return text;
   41 |     }
   42 |     char[] src = text.toCharArray();
   43 |     int offset = 0;
   44 |     final StringBuilder builder = new StringBuilder();
   45 |     StringBuilder expression = null;
   46 |     do {
   47 |       if (start > 0 && src[start - 1] == '\\') {
   48 |         // this open token is escaped. remove the backslash and continue.
   49 |         builder.append(src, offset, start - offset - 1).append(openToken);
   50 |         offset = start + openToken.length();
   51 |       } else {
   52 |         // found open token. let's search close token.
   53 |         if (expression == null) {
   54 |           expression = new StringBuilder();
   55 |         } else {
   56 |           expression.setLength(0);
   57 |         }
   58 |         builder.append(src, offset, start - offset);
   59 |         offset = start + openToken.length();
   60 |         int end = text.indexOf(closeToken, offset);
   61 |         while (end > -1) {
   62 |           if ((end <= offset) || (src[end - 1] != '\\')) {
   63 |             expression.append(src, offset, end - offset);
   64 |             break;
   65 |           }
   66 |           // this close token is escaped. remove the backslash and continue.
   67 |           expression.append(src, offset, end - offset - 1).append(closeToken);
   68 |           offset = end + closeToken.length();
   69 |           end = text.indexOf(closeToken, offset);
   70 |         }
   71 |         if (end == -1) {
   72 |           // close token was not found.
   73 |           builder.append(src, start, src.length - start);
   74 |           offset = src.length;
   75 |         } else {
   76 |           builder.append(handler.handleToken(expression.toString()));
   77 |           offset = end + closeToken.length();
   78 |         }
   79 |       }
   80 |       start = text.indexOf(openToken, offset);
   81 |     } while (start > -1);
   82 |     if (offset < src.length) {
   83 |       builder.append(src, offset, src.length - offset);
   84 |     }
   85 |     return builder.toString();
   86 |   }
   87 | }
```
