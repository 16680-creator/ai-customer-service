<!-- learning-docs source archive（机械抓取原文，禁止手改代码内容） -->
<!-- origin: org.mybatis:mybatis:3.6.0 | path: org/apache/ibatis/scripting/xmltags/TextSqlNode.java -->
<!-- 正文引用：[[源码:TextSqlNode.java#L起-L止]] ；代码块头部：(src: TextSqlNode.java#L起-L止) -->

# TextSqlNode.java

| 项 | 值 |
| --- | --- |
| 来源 | `org.mybatis:mybatis:3.6.0` |
| 路径 | `org/apache/ibatis/scripting/xmltags/TextSqlNode.java` |
| 行数 | 91 |
| 在线地址 | https://repo1.maven.org/maven2/org/mybatis/mybatis/3.6.0/mybatis-3.6.0-sources.jar |
| 抓取时间 | 2026-11-25T14:59:18.364Z |

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
   18 | import org.apache.ibatis.parsing.GenericTokenParser;
   19 | import org.apache.ibatis.parsing.TokenHandler;
   20 | import org.apache.ibatis.type.SimpleTypeRegistry;
   21 | 
   22 | /**
   23 |  * @author Clinton Begin
   24 |  */
   25 | public class TextSqlNode implements SqlNode {
   26 |   private final String text;
   27 | 
   28 |   public TextSqlNode(String text) {
   29 |     this.text = text;
   30 |   }
   31 | 
   32 |   public boolean isDynamic() {
   33 |     DynamicCheckerTokenParser checker = new DynamicCheckerTokenParser();
   34 |     GenericTokenParser parser = createParser(checker);
   35 |     parser.parse(text);
   36 |     return checker.isDynamic();
   37 |   }
   38 | 
   39 |   @Override
   40 |   public boolean apply(DynamicContext context) {
   41 |     GenericTokenParser parser = createParser(new BindingTokenParser(context));
   42 |     context.appendSql(context.parseParam(parser.parse(text)));
   43 |     return true;
   44 |   }
   45 | 
   46 |   private GenericTokenParser createParser(TokenHandler handler) {
   47 |     return new GenericTokenParser("${", "}", handler);
   48 |   }
   49 | 
   50 |   private static class BindingTokenParser implements TokenHandler {
   51 | 
   52 |     private final DynamicContext context;
   53 | 
   54 |     public BindingTokenParser(DynamicContext context) {
   55 |       this.context = context;
   56 |     }
   57 | 
   58 |     @Override
   59 |     public String handleToken(String content) {
   60 |       Object parameter = context.getBindings().get("_parameter");
   61 |       if (parameter == null) {
   62 |         context.getBindings().put("value", null);
   63 |       } else if (SimpleTypeRegistry.isSimpleType(parameter.getClass())) {
   64 |         context.getBindings().put("value", parameter);
   65 |       }
   66 |       Object value = OgnlCache.getValue(content, context.getBindings());
   67 |       // issue #274 return "" instead of "null"
   68 |       return value == null ? "" : String.valueOf(value);
   69 |     }
   70 |   }
   71 | 
   72 |   private static class DynamicCheckerTokenParser implements TokenHandler {
   73 | 
   74 |     private boolean isDynamic;
   75 | 
   76 |     public DynamicCheckerTokenParser() {
   77 |       // Prevent Synthetic Access
   78 |     }
   79 | 
   80 |     public boolean isDynamic() {
   81 |       return isDynamic;
   82 |     }
   83 | 
   84 |     @Override
   85 |     public String handleToken(String content) {
   86 |       this.isDynamic = true;
   87 |       return null;
   88 |     }
   89 |   }
   90 | 
   91 | }
```
