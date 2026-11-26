<!-- learning-docs source archive（机械抓取原文，禁止手改代码内容） -->
<!-- origin: org.springframework:spring-tx:7.1.0 | path: org/springframework/transaction/interceptor/RollbackRuleAttribute.java -->
<!-- 正文引用：[[源码:RollbackRuleAttribute.java#L起-L止]] ；代码块头部：(src: RollbackRuleAttribute.java#L起-L止) -->

# RollbackRuleAttribute.java

| 项 | 值 |
| --- | --- |
| 来源 | `org.springframework:spring-tx:7.1.0` |
| 路径 | `org/springframework/transaction/interceptor/RollbackRuleAttribute.java` |
| 行数 | 205 |
| 在线地址 | https://repo1.maven.org/maven2/org/springframework/spring-tx/7.1.0/spring-tx-7.1.0-sources.jar |
| 抓取时间 | 2026-11-25T14:00:57.148Z |

```java
    1 | /*
    2 |  * Copyright 2002-present the original author or authors.
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
   17 | package org.springframework.transaction.interceptor;
   18 | 
   19 | import java.io.Serializable;
   20 | 
   21 | import org.jspecify.annotations.Nullable;
   22 | 
   23 | import org.springframework.util.Assert;
   24 | 
   25 | /**
   26 |  * Rule determining whether a given exception should cause a rollback.
   27 |  *
   28 |  * <p>Multiple such rules can be applied to determine whether a transaction
   29 |  * should commit or rollback after an exception has been thrown.
   30 |  *
   31 |  * <p>Each rule is based on an exception type or exception pattern, supplied via
   32 |  * {@link #RollbackRuleAttribute(Class)} or {@link #RollbackRuleAttribute(String)},
   33 |  * respectively.
   34 |  *
   35 |  * <p>When a rollback rule is defined with an exception type, that type will be
   36 |  * used to match against the type of a thrown exception and its super types,
   37 |  * providing type safety and avoiding any unintentional matches that may occur
   38 |  * when using a pattern. For example, a value of
   39 |  * {@code jakarta.servlet.ServletException.class} will only match thrown exceptions
   40 |  * of type {@code jakarta.servlet.ServletException} and its subclasses.
   41 |  *
   42 |  * <p>When a rollback rule is defined with an exception pattern, the pattern can
   43 |  * be a fully qualified class name or a substring of a fully qualified class name
   44 |  * for an exception type (which must be a subclass of {@code Throwable}), with no
   45 |  * wildcard support at present. For example, a value of
   46 |  * {@code "jakarta.servlet.ServletException"} or {@code "ServletException"} will
   47 |  * match {@code jakarta.servlet.ServletException} and its subclasses.
   48 |  *
   49 |  * <p>See the javadocs for
   50 |  * {@link org.springframework.transaction.annotation.Transactional @Transactional}
   51 |  * for further details on rollback rule semantics, patterns, and warnings regarding
   52 |  * possible unintentional matches with pattern-based rules.
   53 |  *
   54 |  * @author Rod Johnson
   55 |  * @author Sam Brannen
   56 |  * @since 09.04.2003
   57 |  * @see NoRollbackRuleAttribute
   58 |  */
   59 | @SuppressWarnings("serial")
   60 | public class RollbackRuleAttribute implements Serializable{
   61 | 
   62 | 	/**
   63 | 	 * The {@linkplain RollbackRuleAttribute rollback rule} for
   64 | 	 * {@link RuntimeException RuntimeExceptions}.
   65 | 	 */
   66 | 	public static final RollbackRuleAttribute ROLLBACK_ON_RUNTIME_EXCEPTIONS =
   67 | 			new RollbackRuleAttribute(RuntimeException.class);
   68 | 
   69 | 	/**
   70 | 	 * The {@linkplain RollbackRuleAttribute rollback rule} for all
   71 | 	 * {@link Exception Exceptions}, including checked exceptions.
   72 | 	 * @since 6.2
   73 | 	 */
   74 | 	public static final RollbackRuleAttribute ROLLBACK_ON_ALL_EXCEPTIONS =
   75 | 			new RollbackRuleAttribute(Exception.class);
   76 | 
   77 | 
   78 | 	/**
   79 | 	 * Exception pattern: used when searching for matches in a thrown exception's
   80 | 	 * class hierarchy based on names of exceptions, with zero type safety and
   81 | 	 * potentially resulting in unintentional matches for similarly named exception
   82 | 	 * types and nested exception types.
   83 | 	 */
   84 | 	private final String exceptionPattern;
   85 | 
   86 | 	/**
   87 | 	 * Exception type: used to ensure type safety when searching for matches in
   88 | 	 * a thrown exception's class hierarchy.
   89 | 	 * @since 6.0
   90 | 	 */
   91 | 	private final @Nullable Class<? extends Throwable> exceptionType;
   92 | 
   93 | 
   94 | 	/**
   95 | 	 * Create a new instance of the {@code RollbackRuleAttribute} class
   96 | 	 * for the given {@code exceptionType}.
   97 | 	 * <p>This is the preferred way to construct a rollback rule that matches
   98 | 	 * the supplied exception type and its subclasses with type safety.
   99 | 	 * <p>See the javadocs for
  100 | 	 * {@link org.springframework.transaction.annotation.Transactional @Transactional}
  101 | 	 * for further details on rollback rule semantics.
  102 | 	 * @param exceptionType exception type; must be {@link Throwable} or a subclass
  103 | 	 * of {@code Throwable}
  104 | 	 * @throws IllegalArgumentException if the supplied {@code exceptionType} is
  105 | 	 * not a {@code Throwable} type or is {@code null}
  106 | 	 */
  107 | 	@SuppressWarnings("unchecked")
  108 | 	public RollbackRuleAttribute(Class<?> exceptionType) {
  109 | 		Assert.notNull(exceptionType, "'exceptionType' cannot be null");
  110 | 		if (!Throwable.class.isAssignableFrom(exceptionType)) {
  111 | 			throw new IllegalArgumentException(
  112 | 					"Cannot construct rollback rule from [" + exceptionType.getName() + "]: it's not a Throwable");
  113 | 		}
  114 | 		this.exceptionPattern = exceptionType.getName();
  115 | 		this.exceptionType = (Class<? extends Throwable>) exceptionType;
  116 | 	}
  117 | 
  118 | 	/**
  119 | 	 * Create a new instance of the {@code RollbackRuleAttribute} class
  120 | 	 * for the given {@code exceptionPattern}.
  121 | 	 * <p>See the javadocs for
  122 | 	 * {@link org.springframework.transaction.annotation.Transactional @Transactional}
  123 | 	 * for further details on rollback rule semantics, patterns, and warnings regarding
  124 | 	 * possible unintentional matches.
  125 | 	 * <p>For improved type safety and to avoid unintentional matches, use
  126 | 	 * {@link #RollbackRuleAttribute(Class)} instead.
  127 | 	 * @param exceptionPattern the exception name pattern; can also be a fully
  128 | 	 * package-qualified class name
  129 | 	 * @throws IllegalArgumentException if the supplied {@code exceptionPattern}
  130 | 	 * is {@code null} or empty
  131 | 	 */
  132 | 	public RollbackRuleAttribute(String exceptionPattern) {
  133 | 		Assert.hasText(exceptionPattern, "'exceptionPattern' cannot be null or empty");
  134 | 		this.exceptionPattern = exceptionPattern;
  135 | 		this.exceptionType = null;
  136 | 	}
  137 | 
  138 | 
  139 | 	/**
  140 | 	 * Get the configured exception name pattern that this rule uses for matching.
  141 | 	 * @see #getDepth(Throwable)
  142 | 	 */
  143 | 	public String getExceptionName() {
  144 | 		return this.exceptionPattern;
  145 | 	}
  146 | 
  147 | 	/**
  148 | 	 * Return the depth of the superclass matching, with the following semantics.
  149 | 	 * <ul>
  150 | 	 * <li>{@code -1} means this rule does not match the supplied {@code exception}.</li>
  151 | 	 * <li>{@code 0} means this rule matches the supplied {@code exception} directly.</li>
  152 | 	 * <li>Any other positive value means this rule matches the supplied {@code exception}
  153 | 	 * within the superclass hierarchy, where the value is the number of levels in the
  154 | 	 * class hierarchy between the supplied {@code exception} and the exception against
  155 | 	 * which this rule matches directly.</li>
  156 | 	 * </ul>
  157 | 	 * <p>When comparing roll back rules that match against a given exception, a rule
  158 | 	 * with a lower matching depth wins. For example, a direct match ({@code depth == 0})
  159 | 	 * wins over a match in the superclass hierarchy ({@code depth > 0}).
  160 | 	 * <p>When constructed with an exception pattern via {@link #RollbackRuleAttribute(String)},
  161 | 	 * a match against a nested exception type or similarly named exception type
  162 | 	 * will return a depth signifying a match at the corresponding level in the
  163 | 	 * class hierarchy as if there had been a direct match.
  164 | 	 */
  165 | 	public int getDepth(Throwable exception) {
  166 | 		return getDepth(exception.getClass(), 0);
  167 | 	}
  168 | 
  169 | 
  170 | 	private int getDepth(Class<?> exceptionType, int depth) {
  171 | 		if (this.exceptionType != null) {
  172 | 			if (this.exceptionType.equals(exceptionType)) {
  173 | 				// Found it!
  174 | 				return depth;
  175 | 			}
  176 | 		}
  177 | 		else if (exceptionType.getName().contains(this.exceptionPattern)) {
  178 | 			// Found it!
  179 | 			return depth;
  180 | 		}
  181 | 		// If we've gone as far as we can go and haven't found it...
  182 | 		if (exceptionType == Throwable.class) {
  183 | 			return -1;
  184 | 		}
  185 | 		return getDepth(exceptionType.getSuperclass(), depth + 1);
  186 | 	}
  187 | 
  188 | 
  189 | 	@Override
  190 | 	public boolean equals(@Nullable Object other) {
  191 | 		return (this == other || (other instanceof RollbackRuleAttribute that &&
  192 | 				this.exceptionPattern.equals(that.exceptionPattern)));
  193 | 	}
  194 | 
  195 | 	@Override
  196 | 	public int hashCode() {
  197 | 		return this.exceptionPattern.hashCode();
  198 | 	}
  199 | 
  200 | 	@Override
  201 | 	public String toString() {
  202 | 		return "RollbackRuleAttribute with pattern [" + this.exceptionPattern + "]";
  203 | 	}
  204 | 
  205 | }
```
