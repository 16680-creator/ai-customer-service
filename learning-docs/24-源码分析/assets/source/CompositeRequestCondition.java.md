<!-- learning-docs source archive（机械抓取原文，禁止手改代码内容） -->
<!-- origin: org.springframework:spring-webmvc:7.1.0 | path: org/springframework/web/servlet/mvc/condition/CompositeRequestCondition.java -->
<!-- 正文引用：[[源码:CompositeRequestCondition.java#L起-L止]] ；代码块头部：(src: CompositeRequestCondition.java#L起-L止) -->

# CompositeRequestCondition.java

| 项 | 值 |
| --- | --- |
| 来源 | `org.springframework:spring-webmvc:7.1.0` |
| 路径 | `org/springframework/web/servlet/mvc/condition/CompositeRequestCondition.java` |
| 行数 | 189 |
| 在线地址 | https://repo1.maven.org/maven2/org/springframework/spring-webmvc/7.1.0/spring-webmvc-7.1.0-sources.jar |
| 抓取时间 | 2026-11-25T14:01:14.200Z |

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
   17 | package org.springframework.web.servlet.mvc.condition;
   18 | 
   19 | import java.util.ArrayList;
   20 | import java.util.Collection;
   21 | import java.util.Collections;
   22 | import java.util.List;
   23 | 
   24 | import jakarta.servlet.http.HttpServletRequest;
   25 | import org.jspecify.annotations.Nullable;
   26 | 
   27 | import org.springframework.util.Assert;
   28 | import org.springframework.util.ObjectUtils;
   29 | 
   30 | /**
   31 |  * Implements the {@link RequestCondition} contract by delegating to multiple
   32 |  * {@code RequestCondition} types and using a logical conjunction ({@code ' && '}) to
   33 |  * ensure all conditions match a given request.
   34 |  *
   35 |  * <p>When {@code CompositeRequestCondition} instances are combined or compared
   36 |  * they are expected to (a) contain the same number of conditions and (b) that
   37 |  * conditions in the respective index are of the same type. It is acceptable to
   38 |  * provide {@code null} conditions or no conditions at all to the constructor.
   39 |  *
   40 |  * @author Rossen Stoyanchev
   41 |  * @since 3.2
   42 |  */
   43 | public class CompositeRequestCondition extends AbstractRequestCondition<CompositeRequestCondition> {
   44 | 
   45 | 	private final RequestConditionHolder[] requestConditions;
   46 | 
   47 | 
   48 | 	/**
   49 | 	 * Create an instance with 0 or more {@code RequestCondition} types. It is
   50 | 	 * important to create {@code CompositeRequestCondition} instances with the
   51 | 	 * same number of conditions so they may be compared and combined.
   52 | 	 * It is acceptable to provide {@code null} conditions.
   53 | 	 */
   54 | 	public CompositeRequestCondition(RequestCondition<?>... requestConditions) {
   55 | 		this.requestConditions = wrap(requestConditions);
   56 | 	}
   57 | 
   58 | 	private CompositeRequestCondition(RequestConditionHolder[] requestConditions) {
   59 | 		this.requestConditions = requestConditions;
   60 | 	}
   61 | 
   62 | 
   63 | 	private RequestConditionHolder[] wrap(RequestCondition<?>... rawConditions) {
   64 | 		RequestConditionHolder[] wrappedConditions = new RequestConditionHolder[rawConditions.length];
   65 | 		for (int i = 0; i < rawConditions.length; i++) {
   66 | 			wrappedConditions[i] = new RequestConditionHolder(rawConditions[i]);
   67 | 		}
   68 | 		return wrappedConditions;
   69 | 	}
   70 | 
   71 | 	/**
   72 | 	 * Whether this instance contains 0 conditions or not.
   73 | 	 */
   74 | 	@Override
   75 | 	public boolean isEmpty() {
   76 | 		return ObjectUtils.isEmpty(this.requestConditions);
   77 | 	}
   78 | 
   79 | 	/**
   80 | 	 * Return the underlying conditions (possibly empty but never {@code null}).
   81 | 	 */
   82 | 	public List<RequestCondition<?>> getConditions() {
   83 | 		return unwrap();
   84 | 	}
   85 | 
   86 | 	private List<RequestCondition<?>> unwrap() {
   87 | 		List<RequestCondition<?>> result = new ArrayList<>();
   88 | 		for (RequestConditionHolder holder : this.requestConditions) {
   89 | 			result.add(holder.getCondition());
   90 | 		}
   91 | 		return result;
   92 | 	}
   93 | 
   94 | 	@Override
   95 | 	protected Collection<?> getContent() {
   96 | 		return (!isEmpty() ? getConditions() : Collections.emptyList());
   97 | 	}
   98 | 
   99 | 	@Override
  100 | 	protected String getToStringInfix() {
  101 | 		return " && ";
  102 | 	}
  103 | 
  104 | 	private int getLength() {
  105 | 		return this.requestConditions.length;
  106 | 	}
  107 | 
  108 | 	/**
  109 | 	 * If one instance is empty, return the other.
  110 | 	 * If both instances have conditions, combine the individual conditions
  111 | 	 * after ensuring they are of the same type and number.
  112 | 	 */
  113 | 	@Override
  114 | 	public CompositeRequestCondition combine(CompositeRequestCondition other) {
  115 | 		if (isEmpty() && other.isEmpty()) {
  116 | 			return this;
  117 | 		}
  118 | 		else if (other.isEmpty()) {
  119 | 			return this;
  120 | 		}
  121 | 		else if (isEmpty()) {
  122 | 			return other;
  123 | 		}
  124 | 		else {
  125 | 			assertNumberOfConditions(other);
  126 | 			RequestConditionHolder[] combinedConditions = new RequestConditionHolder[getLength()];
  127 | 			for (int i = 0; i < getLength(); i++) {
  128 | 				combinedConditions[i] = this.requestConditions[i].combine(other.requestConditions[i]);
  129 | 			}
  130 | 			return new CompositeRequestCondition(combinedConditions);
  131 | 		}
  132 | 	}
  133 | 
  134 | 	private void assertNumberOfConditions(CompositeRequestCondition other) {
  135 | 		Assert.isTrue(getLength() == other.getLength(),
  136 | 				() -> "Cannot combine CompositeRequestConditions with a different number of conditions. " +
  137 | 				ObjectUtils.nullSafeToString(this.requestConditions) + " and " +
  138 | 				ObjectUtils.nullSafeToString(other.requestConditions));
  139 | 	}
  140 | 
  141 | 	/**
  142 | 	 * Delegate to <em>all</em> contained conditions to match the request and return the
  143 | 	 * resulting "matching" condition instances.
  144 | 	 * <p>An empty {@code CompositeRequestCondition} matches to all requests.
  145 | 	 */
  146 | 	@Override
  147 | 	public @Nullable CompositeRequestCondition getMatchingCondition(HttpServletRequest request) {
  148 | 		if (isEmpty()) {
  149 | 			return this;
  150 | 		}
  151 | 		RequestConditionHolder[] matchingConditions = new RequestConditionHolder[getLength()];
  152 | 		for (int i = 0; i < getLength(); i++) {
  153 | 			RequestConditionHolder matchingCondition = this.requestConditions[i].getMatchingCondition(request);
  154 | 			if (matchingCondition == null) {
  155 | 				return null;
  156 | 			}
  157 | 			matchingConditions[i] = matchingCondition;
  158 | 		}
  159 | 		return new CompositeRequestCondition(matchingConditions);
  160 | 	}
  161 | 
  162 | 	/**
  163 | 	 * If one instance is empty, the other "wins". If both instances have
  164 | 	 * conditions, compare them in the order in which they were provided.
  165 | 	 */
  166 | 	@Override
  167 | 	public int compareTo(CompositeRequestCondition other, HttpServletRequest request) {
  168 | 		if (isEmpty() && other.isEmpty()) {
  169 | 			return 0;
  170 | 		}
  171 | 		else if (isEmpty()) {
  172 | 			return 1;
  173 | 		}
  174 | 		else if (other.isEmpty()) {
  175 | 			return -1;
  176 | 		}
  177 | 		else {
  178 | 			assertNumberOfConditions(other);
  179 | 			for (int i = 0; i < getLength(); i++) {
  180 | 				int result = this.requestConditions[i].compareTo(other.requestConditions[i], request);
  181 | 				if (result != 0) {
  182 | 					return result;
  183 | 				}
  184 | 			}
  185 | 			return 0;
  186 | 		}
  187 | 	}
  188 | 
  189 | }
```
