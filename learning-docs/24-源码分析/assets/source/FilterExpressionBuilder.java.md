<!-- learning-docs source archive（机械抓取原文，禁止手改代码内容） -->
<!-- origin: org.springframework.ai:spring-ai-vector-store:2.1.0 | path: org/springframework/ai/vectorstore/filter/FilterExpressionBuilder.java -->
<!-- 正文引用：[[源码:FilterExpressionBuilder.java#L起-L止]] ；代码块头部：(src: FilterExpressionBuilder.java#L起-L止) -->

# FilterExpressionBuilder.java

| 项 | 值 |
| --- | --- |
| 来源 | `org.springframework.ai:spring-ai-vector-store:2.1.0` |
| 路径 | `org/springframework/ai/vectorstore/filter/FilterExpressionBuilder.java` |
| 行数 | 139 |
| 在线地址 | https://repo1.maven.org/maven2/org/springframework/ai/spring-ai-vector-store/2.1.0/spring-ai-vector-store-2.1.0-sources.jar |
| 抓取时间 | 2026-11-25T16:28:46.963Z |

```java
    1 | /*
    2 |  * Copyright 2023-present the original author or authors.
    3 |  *
    4 |  * Licensed under the Apache License, Version 2.0 (the "License");
    5 |  * you may not use this file except in compliance with the License.
    6 |  * You may obtain a copy of the License at
    7 |  *
    8 |  *      https://www.apache.org/licenses/LICENSE-2.0
    9 |  *
   10 |  * Unless required by applicable law or agreed to in writing, software
   11 |  * distributed under the License is distributed on an "AS IS" BASIS,
   12 |  * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
   13 |  * See the License for the specific language governing permissions and
   14 |  * limitations under the License.
   15 |  */
   16 | 
   17 | package org.springframework.ai.vectorstore.filter;
   18 | 
   19 | import java.util.List;
   20 | 
   21 | import org.springframework.ai.vectorstore.filter.Filter.ExpressionType;
   22 | import org.springframework.ai.vectorstore.filter.Filter.Key;
   23 | import org.springframework.ai.vectorstore.filter.Filter.Value;
   24 | 
   25 | /**
   26 |  * DSL builder for {@link Filter.Expression} instances. Here are some common examples:
   27 |  *
   28 |  * <pre>{@code
   29 |  * var b = new FilterExpressionBuilder();
   30 |  *
   31 |  * // 1: country == "BG"
   32 |  * var exp1 = b.eq("country", "BG");
   33 |  *
   34 |  * // 2: genre == "drama" AND year >= 2020
   35 |  * var exp2 = b.and(b.eq("genre", "drama"), b.gte("year", 2020));
   36 |  *
   37 |  * // 3: genre in ["comedy", "documentary", "drama"]
   38 |  * var exp3 = b.in("genre", "comedy", "documentary", "drama");
   39 |  *
   40 |  * // 4: year >= 2020 OR country == "BG" AND city != "Sofia"
   41 |  * var exp4 = b.and(b.or(b.gte("year", 2020), b.eq("country", "BG")), b.ne("city", "Sofia"));
   42 |  *
   43 |  * // 5: (year >= 2020 OR country == "BG") AND city NIN ["Sofia", "Plovdiv"]
   44 |  * var exp5 = b.and(b.group(b.or(b.gte("year", 2020), b.eq("country", "BG"))), b.nin("city", "Sofia", "Plovdiv"));
   45 |  *
   46 |  * // 6: isOpen == true AND year >= 2020 AND country IN ["BG", "NL", "US"]
   47 |  * var exp6 = b.and(b.and(b.eq("isOpen", true), b.gte("year", 2020)), b.in("country", "BG", "NL", "US"));
   48 |  *
   49 |  * }</pre>
   50 |  *
   51 |  *
   52 |  * This builder DSL mimics the common
   53 |  * <a href="https://www.baeldung.com/hibernate-criteria-queries">Criteria Queries</a>
   54 |  * syntax.
   55 |  *
   56 |  * @author Christian Tzolov
   57 |  */
   58 | public class FilterExpressionBuilder {
   59 | 
   60 | 	public Op eq(String key, Object value) {
   61 | 		return new Op(new Filter.Expression(ExpressionType.EQ, new Key(key), new Value(value)));
   62 | 	}
   63 | 
   64 | 	public Op ne(String key, Object value) {
   65 | 		return new Op(new Filter.Expression(ExpressionType.NE, new Key(key), new Value(value)));
   66 | 	}
   67 | 
   68 | 	public Op gt(String key, Object value) {
   69 | 		return new Op(new Filter.Expression(ExpressionType.GT, new Key(key), new Value(value)));
   70 | 	}
   71 | 
   72 | 	public Op gte(String key, Object value) {
   73 | 		return new Op(new Filter.Expression(ExpressionType.GTE, new Key(key), new Value(value)));
   74 | 	}
   75 | 
   76 | 	public Op lt(String key, Object value) {
   77 | 		return new Op(new Filter.Expression(ExpressionType.LT, new Key(key), new Value(value)));
   78 | 	}
   79 | 
   80 | 	public Op lte(String key, Object value) {
   81 | 		return new Op(new Filter.Expression(ExpressionType.LTE, new Key(key), new Value(value)));
   82 | 	}
   83 | 
   84 | 	public Op and(Op left, Op right) {
   85 | 		return new Op(new Filter.Expression(ExpressionType.AND, left.expression, right.expression));
   86 | 	}
   87 | 
   88 | 	public Op or(Op left, Op right) {
   89 | 		return new Op(new Filter.Expression(ExpressionType.OR, left.expression, right.expression));
   90 | 	}
   91 | 
   92 | 	public Op in(String key, Object... values) {
   93 | 		return this.in(key, List.of(values));
   94 | 	}
   95 | 
   96 | 	public Op in(String key, List<Object> values) {
   97 | 		return new Op(new Filter.Expression(ExpressionType.IN, new Key(key), new Value(values)));
   98 | 	}
   99 | 
  100 | 	public Op nin(String key, Object... values) {
  101 | 		return this.nin(key, List.of(values));
  102 | 	}
  103 | 
  104 | 	public Op nin(String key, List<Object> values) {
  105 | 		return new Op(new Filter.Expression(ExpressionType.NIN, new Key(key), new Value(values)));
  106 | 	}
  107 | 
  108 | 	public Op isNull(String key) {
  109 | 		return new Op(new Filter.Expression(ExpressionType.ISNULL, new Key(key)));
  110 | 	}
  111 | 
  112 | 	public Op isNotNull(String key) {
  113 | 		return new Op(new Filter.Expression(ExpressionType.ISNOTNULL, new Key(key)));
  114 | 	}
  115 | 
  116 | 	public Op group(Op content) {
  117 | 		return new Op(new Filter.Group(content.build()));
  118 | 	}
  119 | 
  120 | 	public Op not(Op content) {
  121 | 		return new Op(new Filter.Expression(ExpressionType.NOT, content.expression, null));
  122 | 	}
  123 | 
  124 | 	public record Op(Filter.Operand expression) {
  125 | 
  126 | 		public Filter.Expression build() {
  127 | 			if (this.expression instanceof Filter.Group group) {
  128 | 				// Remove the top-level grouping.
  129 | 				return group.content();
  130 | 			}
  131 | 			else if (this.expression instanceof Filter.Expression exp) {
  132 | 				return exp;
  133 | 			}
  134 | 			throw new RuntimeException("Invalid expression: " + this.expression);
  135 | 		}
  136 | 
  137 | 	}
  138 | 
  139 | }
```
