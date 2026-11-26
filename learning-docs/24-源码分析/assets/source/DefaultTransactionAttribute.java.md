<!-- learning-docs source archive（机械抓取原文，禁止手改代码内容） -->
<!-- origin: org.springframework:spring-tx:7.1.0 | path: org/springframework/transaction/interceptor/DefaultTransactionAttribute.java -->
<!-- 正文引用：[[源码:DefaultTransactionAttribute.java#L起-L止]] ；代码块头部：(src: DefaultTransactionAttribute.java#L起-L止) -->

# DefaultTransactionAttribute.java

| 项 | 值 |
| --- | --- |
| 来源 | `org.springframework:spring-tx:7.1.0` |
| 路径 | `org/springframework/transaction/interceptor/DefaultTransactionAttribute.java` |
| 行数 | 236 |
| 在线地址 | https://repo1.maven.org/maven2/org/springframework/spring-tx/7.1.0/spring-tx-7.1.0-sources.jar |
| 抓取时间 | 2026-11-25T14:00:57.514Z |

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
   19 | import java.util.Collection;
   20 | import java.util.Collections;
   21 | import java.util.Set;
   22 | 
   23 | import org.jspecify.annotations.Nullable;
   24 | 
   25 | import org.springframework.transaction.support.DefaultTransactionDefinition;
   26 | import org.springframework.util.CollectionUtils;
   27 | import org.springframework.util.StringUtils;
   28 | import org.springframework.util.StringValueResolver;
   29 | 
   30 | /**
   31 |  * Spring's common transaction attribute implementation.
   32 |  * Rolls back on runtime, but not checked, exceptions by default.
   33 |  *
   34 |  * @author Rod Johnson
   35 |  * @author Juergen Hoeller
   36 |  * @author Mark Paluch
   37 |  * @since 16.03.2003
   38 |  */
   39 | @SuppressWarnings("serial")
   40 | public class DefaultTransactionAttribute extends DefaultTransactionDefinition implements TransactionAttribute {
   41 | 
   42 | 	private @Nullable String descriptor;
   43 | 
   44 | 	private @Nullable String timeoutString;
   45 | 
   46 | 	private @Nullable String qualifier;
   47 | 
   48 | 	private Collection<String> labels = Collections.emptyList();
   49 | 
   50 | 
   51 | 	/**
   52 | 	 * Create a new {@code DefaultTransactionAttribute} with default settings.
   53 | 	 * Can be modified through bean property setters.
   54 | 	 * @see #setPropagationBehavior
   55 | 	 * @see #setIsolationLevel
   56 | 	 * @see #setTimeout
   57 | 	 * @see #setReadOnly
   58 | 	 * @see #setName
   59 | 	 */
   60 | 	public DefaultTransactionAttribute() {
   61 | 	}
   62 | 
   63 | 	/**
   64 | 	 * Copy constructor. Definition can be modified through bean property setters.
   65 | 	 * @see #setPropagationBehavior
   66 | 	 * @see #setIsolationLevel
   67 | 	 * @see #setTimeout
   68 | 	 * @see #setReadOnly
   69 | 	 * @see #setName
   70 | 	 */
   71 | 	public DefaultTransactionAttribute(TransactionAttribute other) {
   72 | 		super(other);
   73 | 	}
   74 | 
   75 | 	/**
   76 | 	 * Create a new {@code DefaultTransactionAttribute} with the given
   77 | 	 * propagation behavior. Can be modified through bean property setters.
   78 | 	 * @param propagationBehavior one of the propagation constants in the
   79 | 	 * TransactionDefinition interface
   80 | 	 * @see #setIsolationLevel
   81 | 	 * @see #setTimeout
   82 | 	 * @see #setReadOnly
   83 | 	 */
   84 | 	public DefaultTransactionAttribute(int propagationBehavior) {
   85 | 		super(propagationBehavior);
   86 | 	}
   87 | 
   88 | 
   89 | 	/**
   90 | 	 * Set a descriptor for this transaction attribute,
   91 | 	 * for example, indicating where the attribute is applying.
   92 | 	 * @since 4.3.4
   93 | 	 */
   94 | 	public void setDescriptor(@Nullable String descriptor) {
   95 | 		this.descriptor = descriptor;
   96 | 	}
   97 | 
   98 | 	/**
   99 | 	 * Return a descriptor for this transaction attribute,
  100 | 	 * or {@code null} if none.
  101 | 	 * @since 4.3.4
  102 | 	 */
  103 | 	public @Nullable String getDescriptor() {
  104 | 		return this.descriptor;
  105 | 	}
  106 | 
  107 | 	/**
  108 | 	 * Set the timeout to apply, if any,
  109 | 	 * as a String value that resolves to a number of seconds.
  110 | 	 * @since 5.3
  111 | 	 * @see #setTimeout
  112 | 	 * @see #resolveAttributeStrings
  113 | 	 */
  114 | 	public void setTimeoutString(@Nullable String timeoutString) {
  115 | 		this.timeoutString = timeoutString;
  116 | 	}
  117 | 
  118 | 	/**
  119 | 	 * Return the timeout to apply, if any,
  120 | 	 * as a String value that resolves to a number of seconds.
  121 | 	 * @since 5.3
  122 | 	 * @see #getTimeout
  123 | 	 * @see #resolveAttributeStrings
  124 | 	 */
  125 | 	public @Nullable String getTimeoutString() {
  126 | 		return this.timeoutString;
  127 | 	}
  128 | 
  129 | 	/**
  130 | 	 * Associate a qualifier value with this transaction attribute.
  131 | 	 * <p>This may be used for choosing a corresponding transaction manager
  132 | 	 * to process this specific transaction.
  133 | 	 * @since 3.0
  134 | 	 * @see #resolveAttributeStrings
  135 | 	 */
  136 | 	public void setQualifier(@Nullable String qualifier) {
  137 | 		this.qualifier = qualifier;
  138 | 	}
  139 | 
  140 | 	/**
  141 | 	 * Return a qualifier value associated with this transaction attribute.
  142 | 	 * @since 3.0
  143 | 	 */
  144 | 	@Override
  145 | 	public @Nullable String getQualifier() {
  146 | 		return this.qualifier;
  147 | 	}
  148 | 
  149 | 	/**
  150 | 	 * Associate one or more labels with this transaction attribute.
  151 | 	 * <p>This may be used for applying specific transactional behavior
  152 | 	 * or follow a purely descriptive nature.
  153 | 	 * @since 5.3
  154 | 	 * @see #resolveAttributeStrings
  155 | 	 */
  156 | 	public void setLabels(Collection<String> labels) {
  157 | 		this.labels = labels;
  158 | 	}
  159 | 
  160 | 	@Override
  161 | 	public Collection<String> getLabels() {
  162 | 		return this.labels;
  163 | 	}
  164 | 
  165 | 	/**
  166 | 	 * The default behavior is as with EJB: rollback on unchecked exception
  167 | 	 * ({@link RuntimeException}), assuming an unexpected outcome outside any
  168 | 	 * business rules. Additionally, we also attempt to rollback on {@link Error} which
  169 | 	 * is clearly an unexpected outcome as well. By contrast, a checked exception is
  170 | 	 * considered a business exception and therefore a regular expected outcome of the
  171 | 	 * transactional business method, i.e. a kind of alternative return value which
  172 | 	 * still allows for regular completion of resource operations.
  173 | 	 * <p>This is largely consistent with TransactionTemplate's default behavior,
  174 | 	 * except that TransactionTemplate also rolls back on undeclared checked exceptions
  175 | 	 * (a corner case). For declarative transactions, we expect checked exceptions to be
  176 | 	 * intentionally declared as business exceptions, leading to a commit by default.
  177 | 	 * @see org.springframework.transaction.support.TransactionTemplate#execute
  178 | 	 */
  179 | 	@Override
  180 | 	public boolean rollbackOn(Throwable ex) {
  181 | 		return (ex instanceof RuntimeException || ex instanceof Error);
  182 | 	}
  183 | 
  184 | 
  185 | 	/**
  186 | 	 * Resolve attribute values that are defined as resolvable Strings:
  187 | 	 * {@link #setTimeoutString}, {@link #setQualifier}, {@link #setLabels}.
  188 | 	 * This is typically used for resolving "${...}" placeholders.
  189 | 	 * @param resolver the embedded value resolver to apply, if any
  190 | 	 * @since 5.3
  191 | 	 */
  192 | 	public void resolveAttributeStrings(@Nullable StringValueResolver resolver) {
  193 | 		String timeoutString = this.timeoutString;
  194 | 		if (StringUtils.hasText(timeoutString)) {
  195 | 			if (resolver != null) {
  196 | 				timeoutString = resolver.resolveStringValue(timeoutString);
  197 | 			}
  198 | 			if (StringUtils.hasLength(timeoutString)) {
  199 | 				try {
  200 | 					setTimeout(Integer.parseInt(timeoutString));
  201 | 				}
  202 | 				catch (RuntimeException ex) {
  203 | 					throw new IllegalArgumentException(
  204 | 							"Invalid timeoutString value \"" + timeoutString + "\"; " + ex);
  205 | 				}
  206 | 			}
  207 | 		}
  208 | 
  209 | 		if (resolver != null) {
  210 | 			if (this.qualifier != null) {
  211 | 				this.qualifier = resolver.resolveStringValue(this.qualifier);
  212 | 			}
  213 | 			Set<String> resolvedLabels = CollectionUtils.newLinkedHashSet(this.labels.size());
  214 | 			for (String label : this.labels) {
  215 | 				resolvedLabels.add(resolver.resolveStringValue(label));
  216 | 			}
  217 | 			this.labels = resolvedLabels;
  218 | 		}
  219 | 	}
  220 | 
  221 | 	/**
  222 | 	 * Return an identifying description for this transaction attribute.
  223 | 	 * <p>Available to subclasses, for inclusion in their {@code toString()} result.
  224 | 	 */
  225 | 	protected final StringBuilder getAttributeDescription() {
  226 | 		StringBuilder result = getDefinitionDescription();
  227 | 		if (StringUtils.hasText(this.qualifier)) {
  228 | 			result.append("; '").append(this.qualifier).append('\'');
  229 | 		}
  230 | 		if (!this.labels.isEmpty()) {
  231 | 			result.append("; ").append(this.labels);
  232 | 		}
  233 | 		return result;
  234 | 	}
  235 | 
  236 | }
```
