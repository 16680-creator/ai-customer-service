<!-- learning-docs source archive（机械抓取原文，禁止手改代码内容） -->
<!-- origin: org.springframework.ai:spring-ai-vector-store:2.1.0 | path: org/springframework/ai/vectorstore/filter/Filter.java -->
<!-- 正文引用：[[源码:Filter.java#L起-L止]] ；代码块头部：(src: Filter.java#L起-L止) -->

# Filter.java

| 项 | 值 |
| --- | --- |
| 来源 | `org.springframework.ai:spring-ai-vector-store:2.1.0` |
| 路径 | `org/springframework/ai/vectorstore/filter/Filter.java` |
| 行数 | 145 |
| 在线地址 | https://repo1.maven.org/maven2/org/springframework/ai/spring-ai-vector-store/2.1.0/spring-ai-vector-store-2.1.0-sources.jar |
| 抓取时间 | 2026-11-25T16:28:47.185Z |

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
   19 | import org.jspecify.annotations.Nullable;
   20 | 
   21 | /**
   22 |  * Portable runtime generative for metadata filter expressions. This generic generative is
   23 |  * used to define store agnostic filter expressions than later can be converted into
   24 |  * vector-store specific, native, expressions.
   25 |  *
   26 |  * The expression generative supports constant comparison
   27 |  * {@code (e.g. ==, !=, <, <=, >, >=) }, IN/NON-IN checks and AND and OR to compose
   28 |  * multiple expressions.
   29 |  *
   30 |  * For example:
   31 |  *
   32 |  * <pre>{@code
   33 |  * // 1: country == "BG"
   34 |  * new Expression(EQ, new Key("country"), new Value("BG"));
   35 |  *
   36 |  * // 2: genre == "drama" AND year >= 2020
   37 |  * new Expression(AND, new Expression(EQ, new Key("genre"), new Value("drama")),
   38 |  * 		new Expression(GTE, new Key("year"), new Value(2020)));
   39 |  *
   40 |  * // 3: genre in ["comedy", "documentary", "drama"]
   41 |  * new Expression(IN, new Key("genre"), new Value(List.of("comedy", "documentary", "drama")));
   42 |  *
   43 |  * // 4: year >= 2020 OR country == "BG" AND city != "Sofia"
   44 |  * new Expression(OR, new Expression(GTE, new Key("year"), new Value(2020)),
   45 |  * 		new Expression(AND, new Expression(EQ, new Key("country"), new Value("BG")),
   46 |  * 				new Expression(NE, new Key("city"), new Value("Sofia"))));
   47 |  *
   48 |  * // 5: (year >= 2020 OR country == "BG") AND city NIN ["Sofia", "Plovdiv"]
   49 |  * new Expression(AND,
   50 |  * 		new Group(new Expression(OR, new Expression(EQ, new Key("country"), new Value("BG")),
   51 |  * 				new Expression(GTE, new Key("year"), new Value(2020)))),
   52 |  * 		new Expression(NIN, new Key("city"), new Value(List.of("Sofia", "Varna"))));
   53 |  *
   54 |  * // 6: isOpen == true AND year >= 2020 AND country IN ["BG", "NL", "US"]
   55 |  * new Expression(AND, new Expression(EQ, new Key("isOpen"), new Value(true)),
   56 |  * 		new Expression(AND, new Expression(GTE, new Key("year"), new Value(2020)),
   57 |  * 				new Expression(IN, new Key("country"), new Value(List.of("BG", "NL", "US")))));
   58 |  *
   59 |  * }</pre>
   60 |  *
   61 |  *
   62 |  * Usually you will not create expression manually but use either the
   63 |  * {@link FilterExpressionBuilder} DSL or the {@link FilterExpressionTextParser} for
   64 |  * parsing generic text expressions.
   65 |  *
   66 |  * @author Christian Tzolov
   67 |  */
   68 | public class Filter {
   69 | 
   70 | 	/**
   71 | 	 * Filter expression operations. <br/>
   72 | 	 *
   73 | 	 * - EQ, NE, GT, GTE, LT, LTE operations supports "Key ExprType Value"
   74 | 	 * expressions.<br/>
   75 | 	 *
   76 | 	 * - AND, OR are binary operations that support "(Expression|Group) ExprType
   77 | 	 * (Expression|Group)" expressions. <br/>
   78 | 	 *
   79 | 	 * - IN, NIN support "Key (IN|NIN) ArrayValue" expression. <br/>
   80 | 	 */
   81 | 	public enum ExpressionType {
   82 | 
   83 | 		AND, OR, EQ, NE, GT, GTE, LT, LTE, IN, NIN, NOT, ISNULL, ISNOTNULL
   84 | 
   85 | 	}
   86 | 
   87 | 	/**
   88 | 	 * Mark interface representing the supported expression types: {@link Key},
   89 | 	 * {@link Value}, {@link Expression} and {@link Group}.
   90 | 	 */
   91 | 	public interface Operand {
   92 | 
   93 | 	}
   94 | 
   95 | 	/**
   96 | 	 * String identifier representing an expression key. (e.g. the country in the country
   97 | 	 * == "NL" expression).
   98 | 	 *
   99 | 	 * @param key expression key
  100 | 	 */
  101 | 	public record Key(String key) implements Operand {
  102 | 
  103 | 	}
  104 | 
  105 | 	/**
  106 | 	 * Represents expression value constant or constant array. Support Numeric, Boolean
  107 | 	 * and String data types.
  108 | 	 *
  109 | 	 * @param value value constant or constant array
  110 | 	 */
  111 | 	public record Value(Object value) implements Operand {
  112 | 
  113 | 	}
  114 | 
  115 | 	/**
  116 | 	 * Triple that represents and filter boolean expression as
  117 | 	 * <code>left type right</code>.
  118 | 	 *
  119 | 	 * @param type Specify the expression type.
  120 | 	 * @param left For comparison and inclusion expression types, the operand must be of
  121 | 	 * type {@link Key} and for the AND|OR expression types the left operand must be
  122 | 	 * another {@link Expression}.
  123 | 	 * @param right For comparison and inclusion expression types, the operand must be of
  124 | 	 * type {@link Value} or array of values. For the AND|OR type the right operand must
  125 | 	 * be another {@link Expression}.
  126 | 	 */
  127 | 	public record Expression(ExpressionType type, Operand left, @Nullable Operand right) implements Operand {
  128 | 
  129 | 		public Expression(ExpressionType type, Operand operand) {
  130 | 			this(type, operand, null);
  131 | 		}
  132 | 
  133 | 	}
  134 | 
  135 | 	/**
  136 | 	 * Represents expression grouping (e.g. (...) ) that indicates that the group needs to
  137 | 	 * be evaluated with a precedence.
  138 | 	 *
  139 | 	 * @param content Inner expression to be evaluated as a part of the group.
  140 | 	 */
  141 | 	public record Group(Expression content) implements Operand {
  142 | 
  143 | 	}
  144 | 
  145 | }
```
