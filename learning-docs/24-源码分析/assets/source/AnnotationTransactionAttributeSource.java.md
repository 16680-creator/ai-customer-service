<!-- learning-docs source archive（机械抓取原文，禁止手改代码内容） -->
<!-- origin: org.springframework:spring-tx:7.1.0 | path: org/springframework/transaction/annotation/AnnotationTransactionAttributeSource.java -->
<!-- 正文引用：[[源码:AnnotationTransactionAttributeSource.java#L起-L止]] ；代码块头部：(src: AnnotationTransactionAttributeSource.java#L起-L止) -->

# AnnotationTransactionAttributeSource.java

| 项 | 值 |
| --- | --- |
| 来源 | `org.springframework:spring-tx:7.1.0` |
| 路径 | `org/springframework/transaction/annotation/AnnotationTransactionAttributeSource.java` |
| 行数 | 232 |
| 在线地址 | https://repo1.maven.org/maven2/org/springframework/spring-tx/7.1.0/spring-tx-7.1.0-sources.jar |
| 抓取时间 | 2026-11-25T14:00:14.001Z |

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
   17 | package org.springframework.transaction.annotation;
   18 | 
   19 | import java.io.Serializable;
   20 | import java.lang.reflect.AnnotatedElement;
   21 | import java.lang.reflect.Method;
   22 | import java.util.Collections;
   23 | import java.util.LinkedHashSet;
   24 | import java.util.Set;
   25 | 
   26 | import org.jspecify.annotations.Nullable;
   27 | 
   28 | import org.springframework.transaction.interceptor.AbstractFallbackTransactionAttributeSource;
   29 | import org.springframework.transaction.interceptor.RollbackRuleAttribute;
   30 | import org.springframework.transaction.interceptor.RuleBasedTransactionAttribute;
   31 | import org.springframework.transaction.interceptor.TransactionAttribute;
   32 | import org.springframework.util.Assert;
   33 | import org.springframework.util.ClassUtils;
   34 | import org.springframework.util.CollectionUtils;
   35 | 
   36 | /**
   37 |  * Implementation of the
   38 |  * {@link org.springframework.transaction.interceptor.TransactionAttributeSource}
   39 |  * interface for working with transaction metadata from annotations.
   40 |  *
   41 |  * <p>This class reads Spring's {@link Transactional @Transactional} annotation and
   42 |  * exposes corresponding transaction attributes to Spring's transaction infrastructure.
   43 |  * Also supports JTA's {@link jakarta.transaction.Transactional} and EJB's
   44 |  * {@link jakarta.ejb.TransactionAttribute} annotation (if present).
   45 |  *
   46 |  * <p>This class may also serve as base class for a custom TransactionAttributeSource,
   47 |  * or get customized through {@link TransactionAnnotationParser} strategies.
   48 |  *
   49 |  * @author Colin Sampaleanu
   50 |  * @author Juergen Hoeller
   51 |  * @since 1.2
   52 |  * @see Transactional
   53 |  * @see TransactionAnnotationParser
   54 |  * @see SpringTransactionAnnotationParser
   55 |  * @see Ejb3TransactionAnnotationParser
   56 |  * @see org.springframework.transaction.interceptor.TransactionInterceptor#setTransactionAttributeSource
   57 |  * @see org.springframework.transaction.interceptor.TransactionProxyFactoryBean#setTransactionAttributeSource
   58 |  */
   59 | @SuppressWarnings("serial")
   60 | public class AnnotationTransactionAttributeSource extends AbstractFallbackTransactionAttributeSource
   61 | 		implements Serializable {
   62 | 
   63 | 	private static final boolean JTA_PRESENT;
   64 | 
   65 | 	private static final boolean EJB_3_PRESENT;
   66 | 
   67 | 	static {
   68 | 		ClassLoader classLoader = AnnotationTransactionAttributeSource.class.getClassLoader();
   69 | 		JTA_PRESENT = ClassUtils.isPresent("jakarta.transaction.Transactional", classLoader);
   70 | 		EJB_3_PRESENT = ClassUtils.isPresent("jakarta.ejb.TransactionAttribute", classLoader);
   71 | 	}
   72 | 
   73 | 	private final Set<TransactionAnnotationParser> annotationParsers;
   74 | 
   75 | 	private boolean publicMethodsOnly = true;
   76 | 
   77 | 	private @Nullable Set<RollbackRuleAttribute> defaultRollbackRules;
   78 | 
   79 | 
   80 | 	/**
   81 | 	 * Create a default AnnotationTransactionAttributeSource, supporting
   82 | 	 * public methods that carry the {@code Transactional} annotation
   83 | 	 * or the EJB3 {@link jakarta.ejb.TransactionAttribute} annotation.
   84 | 	 */
   85 | 	public AnnotationTransactionAttributeSource() {
   86 | 		if (JTA_PRESENT || EJB_3_PRESENT) {
   87 | 			this.annotationParsers = CollectionUtils.newLinkedHashSet(3);
   88 | 			this.annotationParsers.add(new SpringTransactionAnnotationParser());
   89 | 			if (JTA_PRESENT) {
   90 | 				this.annotationParsers.add(new JtaTransactionAnnotationParser());
   91 | 			}
   92 | 			if (EJB_3_PRESENT) {
   93 | 				this.annotationParsers.add(new Ejb3TransactionAnnotationParser());
   94 | 			}
   95 | 		}
   96 | 		else {
   97 | 			this.annotationParsers = Collections.singleton(new SpringTransactionAnnotationParser());
   98 | 		}
   99 | 	}
  100 | 
  101 | 	/**
  102 | 	 * Create a custom AnnotationTransactionAttributeSource, supporting
  103 | 	 * public methods that carry the {@code Transactional} annotation
  104 | 	 * or the EJB3 {@link jakarta.ejb.TransactionAttribute} annotation.
  105 | 	 * @param publicMethodsOnly whether to support public methods that carry
  106 | 	 * the {@code Transactional} annotation only (typically for use
  107 | 	 * with proxy-based AOP), or protected/private methods as well
  108 | 	 * (typically used with AspectJ class weaving)
  109 | 	 * @see #setPublicMethodsOnly
  110 | 	 */
  111 | 	public AnnotationTransactionAttributeSource(boolean publicMethodsOnly) {
  112 | 		this();
  113 | 		this.publicMethodsOnly = publicMethodsOnly;
  114 | 	}
  115 | 
  116 | 	/**
  117 | 	 * Create a custom AnnotationTransactionAttributeSource.
  118 | 	 * @param annotationParser the TransactionAnnotationParser to use
  119 | 	 */
  120 | 	public AnnotationTransactionAttributeSource(TransactionAnnotationParser annotationParser) {
  121 | 		Assert.notNull(annotationParser, "TransactionAnnotationParser must not be null");
  122 | 		this.annotationParsers = Collections.singleton(annotationParser);
  123 | 	}
  124 | 
  125 | 	/**
  126 | 	 * Create a custom AnnotationTransactionAttributeSource.
  127 | 	 * @param annotationParsers the TransactionAnnotationParsers to use
  128 | 	 */
  129 | 	public AnnotationTransactionAttributeSource(TransactionAnnotationParser... annotationParsers) {
  130 | 		Assert.notEmpty(annotationParsers, "At least one TransactionAnnotationParser needs to be specified");
  131 | 		this.annotationParsers = Set.of(annotationParsers);
  132 | 	}
  133 | 
  134 | 
  135 | 	/**
  136 | 	 * Set whether transactional methods are expected to be public.
  137 | 	 * <p>The default is {@code true}.
  138 | 	 * @since 6.2
  139 | 	 * @see #AnnotationTransactionAttributeSource(boolean)
  140 | 	 */
  141 | 	public void setPublicMethodsOnly(boolean publicMethodsOnly) {
  142 | 		this.publicMethodsOnly = publicMethodsOnly;
  143 | 	}
  144 | 
  145 | 	/**
  146 | 	 * Add a default rollback rule, to be applied to all rule-based
  147 | 	 * transaction attributes returned by this source.
  148 | 	 * <p>By default, a rollback will be triggered on unchecked exceptions
  149 | 	 * but not on checked exceptions. A default rule may override this
  150 | 	 * while still respecting any custom rules in the transaction attribute.
  151 | 	 * @param rollbackRule a rollback rule overriding the default behavior,
  152 | 	 * for example, {@link RollbackRuleAttribute#ROLLBACK_ON_ALL_EXCEPTIONS}
  153 | 	 * @since 6.2
  154 | 	 * @see RuleBasedTransactionAttribute#getRollbackRules()
  155 | 	 * @see EnableTransactionManagement#rollbackOn()
  156 | 	 * @see Transactional#rollbackFor()
  157 | 	 * @see Transactional#noRollbackFor()
  158 | 	 */
  159 | 	public void addDefaultRollbackRule(RollbackRuleAttribute rollbackRule) {
  160 | 		if (this.defaultRollbackRules == null) {
  161 | 			this.defaultRollbackRules = new LinkedHashSet<>();
  162 | 		}
  163 | 		this.defaultRollbackRules.add(rollbackRule);
  164 | 	}
  165 | 
  166 | 
  167 | 	@Override
  168 | 	public boolean isCandidateClass(Class<?> targetClass) {
  169 | 		for (TransactionAnnotationParser parser : this.annotationParsers) {
  170 | 			if (parser.isCandidateClass(targetClass)) {
  171 | 				return true;
  172 | 			}
  173 | 		}
  174 | 		return false;
  175 | 	}
  176 | 
  177 | 	@Override
  178 | 	protected @Nullable TransactionAttribute findTransactionAttribute(Class<?> clazz) {
  179 | 		return determineTransactionAttribute(clazz);
  180 | 	}
  181 | 
  182 | 	@Override
  183 | 	protected @Nullable TransactionAttribute findTransactionAttribute(Method method) {
  184 | 		return determineTransactionAttribute(method);
  185 | 	}
  186 | 
  187 | 	/**
  188 | 	 * Determine the transaction attribute for the given method or class.
  189 | 	 * <p>This implementation delegates to configured
  190 | 	 * {@link TransactionAnnotationParser TransactionAnnotationParsers}
  191 | 	 * for parsing known annotations into Spring's metadata attribute class.
  192 | 	 * Returns {@code null} if it's not transactional.
  193 | 	 * <p>Can be overridden to support custom annotations that carry transaction metadata.
  194 | 	 * @param element the annotated method or class
  195 | 	 * @return the configured transaction attribute, or {@code null} if none was found
  196 | 	 */
  197 | 	protected @Nullable TransactionAttribute determineTransactionAttribute(AnnotatedElement element) {
  198 | 		for (TransactionAnnotationParser parser : this.annotationParsers) {
  199 | 			TransactionAttribute attr = parser.parseTransactionAnnotation(element);
  200 | 			if (attr != null) {
  201 | 				if (this.defaultRollbackRules != null && attr instanceof RuleBasedTransactionAttribute ruleAttr) {
  202 | 					ruleAttr.getRollbackRules().addAll(this.defaultRollbackRules);
  203 | 				}
  204 | 				return attr;
  205 | 			}
  206 | 		}
  207 | 		return null;
  208 | 	}
  209 | 
  210 | 	/**
  211 | 	 * By default, only public methods can be made transactional.
  212 | 	 * @see #setPublicMethodsOnly
  213 | 	 */
  214 | 	@Override
  215 | 	protected boolean allowPublicMethodsOnly() {
  216 | 		return this.publicMethodsOnly;
  217 | 	}
  218 | 
  219 | 
  220 | 	@Override
  221 | 	public boolean equals(@Nullable Object other) {
  222 | 		return (this == other || (other instanceof AnnotationTransactionAttributeSource otherTas &&
  223 | 				this.annotationParsers.equals(otherTas.annotationParsers) &&
  224 | 				this.publicMethodsOnly == otherTas.publicMethodsOnly));
  225 | 	}
  226 | 
  227 | 	@Override
  228 | 	public int hashCode() {
  229 | 		return this.annotationParsers.hashCode();
  230 | 	}
  231 | 
  232 | }
```
