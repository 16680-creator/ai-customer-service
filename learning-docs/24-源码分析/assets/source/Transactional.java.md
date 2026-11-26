<!-- learning-docs source archive（机械抓取原文，禁止手改代码内容） -->
<!-- origin: org.springframework:spring-tx:7.1.0 | path: org/springframework/transaction/annotation/Transactional.java -->
<!-- 正文引用：[[源码:Transactional.java#L起-L止]] ；代码块头部：(src: Transactional.java#L起-L止) -->

# Transactional.java

| 项 | 值 |
| --- | --- |
| 来源 | `org.springframework:spring-tx:7.1.0` |
| 路径 | `org/springframework/transaction/annotation/Transactional.java` |
| 行数 | 289 |
| 在线地址 | https://repo1.maven.org/maven2/org/springframework/spring-tx/7.1.0/spring-tx-7.1.0-sources.jar |
| 抓取时间 | 2026-11-25T14:00:14.199Z |

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
   19 | import java.lang.annotation.Documented;
   20 | import java.lang.annotation.ElementType;
   21 | import java.lang.annotation.Inherited;
   22 | import java.lang.annotation.Retention;
   23 | import java.lang.annotation.RetentionPolicy;
   24 | import java.lang.annotation.Target;
   25 | 
   26 | import org.springframework.aot.hint.annotation.Reflective;
   27 | import org.springframework.core.annotation.AliasFor;
   28 | import org.springframework.transaction.TransactionDefinition;
   29 | 
   30 | /**
   31 |  * Describes a transaction attribute on an individual method or on a class.
   32 |  *
   33 |  * <p>When this annotation is declared at the class level, it applies as a default
   34 |  * to all methods of the declaring class and its subclasses. Note that it does not
   35 |  * apply to ancestor classes up the class hierarchy; inherited methods need to be
   36 |  * locally redeclared in order to participate in a subclass-level annotation. For
   37 |  * details on method visibility constraints, consult the
   38 |  * <a href="https://docs.spring.io/spring-framework/reference/data-access/transaction/declarative/annotations.html#transaction-declarative-annotations-method-visibility">Transaction Management</a>
   39 |  * section of the reference manual.
   40 |  *
   41 |  * <p>This annotation is generally directly comparable to Spring's
   42 |  * {@link org.springframework.transaction.interceptor.RuleBasedTransactionAttribute}
   43 |  * class, and in fact {@link AnnotationTransactionAttributeSource} will directly
   44 |  * convert this annotation's attributes to properties in {@code RuleBasedTransactionAttribute},
   45 |  * so that Spring's transaction support code does not have to know about annotations.
   46 |  *
   47 |  * <h3>Attribute Semantics</h3>
   48 |  *
   49 |  * <p>If no custom rollback rules are configured in this annotation, the transaction
   50 |  * will roll back on {@link RuntimeException} and {@link Error} but not on checked
   51 |  * exceptions.
   52 |  *
   53 |  * <p>Rollback rules determine if a transaction should be rolled back when a given
   54 |  * exception is thrown, and the rules are based on types or patterns. Custom
   55 |  * rules may be configured via {@link #rollbackFor}/{@link #noRollbackFor} and
   56 |  * {@link #rollbackForClassName}/{@link #noRollbackForClassName}, which allow
   57 |  * rules to be specified as types or patterns, respectively.
   58 |  *
   59 |  * <p>When a rollback rule is defined with an exception type &mdash; for example,
   60 |  * via {@link #rollbackFor} &mdash; that type will be used to match against the
   61 |  * type of a thrown exception. Specifically, given a configured exception type
   62 |  * {@code C}, a thrown exception of type {@code T} will be considered a match
   63 |  * against {@code C} if {@code T} is equal to {@code C} or a subclass of {@code C}.
   64 |  * This provides type safety and avoids any unintentional matches that may occur
   65 |  * when using a pattern. For example, a value of
   66 |  * {@code jakarta.servlet.ServletException.class} will only match thrown exceptions
   67 |  * of type {@code jakarta.servlet.ServletException} and its subclasses.
   68 |  *
   69 |  * <p>When a rollback rule is defined with an exception pattern, the pattern can
   70 |  * be a fully qualified class name or a substring of a fully qualified class name
   71 |  * for an exception type (which must be a subclass of {@code Throwable}), with no
   72 |  * wildcard support at present. For example, a value of
   73 |  * {@code "jakarta.servlet.ServletException"} or {@code "ServletException"} will
   74 |  * match {@code jakarta.servlet.ServletException} and its subclasses.
   75 |  *
   76 |  * <p><strong>WARNING:</strong> You must carefully consider how specific a pattern
   77 |  * is and whether to include package information (which isn't mandatory). For example,
   78 |  * {@code "Exception"} will match nearly anything and will probably hide other
   79 |  * rules. {@code "java.lang.Exception"} would be correct if {@code "Exception"}
   80 |  * were meant to define a rule for all checked exceptions. With more unique
   81 |  * exception names such as {@code "BaseBusinessException"} there is likely no
   82 |  * need to use the fully qualified class name for the exception pattern. Furthermore,
   83 |  * rollback rules defined via patterns may result in unintentional matches for
   84 |  * similarly named exceptions and nested classes. This is due to the fact that a
   85 |  * thrown exception is considered to be a match for a given pattern-based rollback
   86 |  * rule if the name of thrown exception contains the exception pattern configured
   87 |  * for the rollback rule. For example, given a rule configured to match against
   88 |  * {@code "com.example.CustomException"}, that rule will match against an exception
   89 |  * named {@code com.example.CustomExceptionV2} (an exception in the same package as
   90 |  * {@code CustomException} but with an additional suffix) or an exception named
   91 |  * {@code com.example.CustomException$AnotherException} (an exception declared as
   92 |  * a nested class in {@code CustomException}).
   93 |  *
   94 |  * <p>For specific information about the semantics of other attributes in this
   95 |  * annotation, consult the {@link org.springframework.transaction.TransactionDefinition}
   96 |  * and {@link org.springframework.transaction.interceptor.TransactionAttribute} javadocs.
   97 |  *
   98 |  * <h3>Transaction Management</h3>
   99 |  *
  100 |  * <p>This annotation commonly works with thread-bound transactions managed by a
  101 |  * {@link org.springframework.transaction.PlatformTransactionManager}, exposing a
  102 |  * transaction to all data access operations within the current execution thread.
  103 |  * <b>Note: This does NOT propagate to newly started threads within the method.</b>
  104 |  *
  105 |  * <p>Alternatively, this annotation may demarcate a reactive transaction managed
  106 |  * by a {@link org.springframework.transaction.ReactiveTransactionManager} which
  107 |  * uses the Reactor context instead of thread-local variables. As a consequence,
  108 |  * all participating data access operations need to execute within the same
  109 |  * Reactor context in the same reactive pipeline.
  110 |  *
  111 |  * <p><b>Note: When configured with a {@code ReactiveTransactionManager}, all
  112 |  * transaction-demarcated methods are expected to return a reactive pipeline.</b>
  113 |  * Void methods or regular return types need to be associated with a regular
  114 |  * {@code PlatformTransactionManager}, for example, through {@link #transactionManager()}.
  115 |  *
  116 |  * @author Colin Sampaleanu
  117 |  * @author Juergen Hoeller
  118 |  * @author Sam Brannen
  119 |  * @author Mark Paluch
  120 |  * @since 1.2
  121 |  * @see org.springframework.transaction.interceptor.TransactionAttribute
  122 |  * @see org.springframework.transaction.interceptor.DefaultTransactionAttribute
  123 |  * @see org.springframework.transaction.interceptor.RuleBasedTransactionAttribute
  124 |  */
  125 | @Target({ElementType.TYPE, ElementType.METHOD})
  126 | @Retention(RetentionPolicy.RUNTIME)
  127 | @Inherited
  128 | @Documented
  129 | @Reflective
  130 | public @interface Transactional {
  131 | 
  132 | 	/**
  133 | 	 * Alias for {@link #transactionManager}.
  134 | 	 * @see #transactionManager
  135 | 	 */
  136 | 	@AliasFor("transactionManager")
  137 | 	String value() default "";
  138 | 
  139 | 	/**
  140 | 	 * A <em>qualifier</em> value for the specified transaction.
  141 | 	 * <p>May be used to determine the target transaction manager, matching the
  142 | 	 * qualifier value (or the bean name) of a specific
  143 | 	 * {@link org.springframework.transaction.TransactionManager TransactionManager}
  144 | 	 * bean definition.
  145 | 	 * <p>Alternatively, as of 6.2, a type-level bean qualifier annotation with a
  146 | 	 * {@link org.springframework.beans.factory.annotation.Qualifier#value() qualifier value}
  147 | 	 * is also taken into account. If it matches the qualifier value (or bean name)
  148 | 	 * of a specific transaction manager, that transaction manager is going to be used
  149 | 	 * for transaction definitions without a specific qualifier on this attribute here.
  150 | 	 * Such a type-level qualifier can be declared on the concrete class, applying
  151 | 	 * to transaction definitions from a base class as well, effectively overriding
  152 | 	 * the default transaction manager choice for any unqualified base class methods.
  153 | 	 * @since 4.2
  154 | 	 * @see #value
  155 | 	 * @see org.springframework.transaction.PlatformTransactionManager
  156 | 	 * @see org.springframework.transaction.ReactiveTransactionManager
  157 | 	 */
  158 | 	@AliasFor("value")
  159 | 	String transactionManager() default "";
  160 | 
  161 | 	/**
  162 | 	 * Defines zero (0) or more transaction labels.
  163 | 	 * <p>Labels may be used to describe a transaction, and they can be evaluated
  164 | 	 * by individual transaction managers. Labels may serve a solely descriptive
  165 | 	 * purpose or map to pre-defined transaction manager-specific options.
  166 | 	 * <p>See the documentation of the actual transaction manager implementation
  167 | 	 * for details on how it evaluates transaction labels.
  168 | 	 * @since 5.3
  169 | 	 * @see org.springframework.transaction.interceptor.DefaultTransactionAttribute#getLabels()
  170 | 	 */
  171 | 	String[] label() default {};
  172 | 
  173 | 	/**
  174 | 	 * The transaction propagation type.
  175 | 	 * <p>Defaults to {@link Propagation#REQUIRED}.
  176 | 	 * @see org.springframework.transaction.interceptor.TransactionAttribute#getPropagationBehavior()
  177 | 	 */
  178 | 	Propagation propagation() default Propagation.REQUIRED;
  179 | 
  180 | 	/**
  181 | 	 * The transaction isolation level.
  182 | 	 * <p>Defaults to {@link Isolation#DEFAULT}.
  183 | 	 * <p>Exclusively designed for use with {@link Propagation#REQUIRED} or
  184 | 	 * {@link Propagation#REQUIRES_NEW} since it only applies to newly started
  185 | 	 * transactions. Consider switching the "validateExistingTransaction" flag to
  186 | 	 * "true" on your transaction manager if you'd like isolation level declarations
  187 | 	 * to get rejected when participating in an existing transaction with a different
  188 | 	 * isolation level.
  189 | 	 * @see org.springframework.transaction.interceptor.TransactionAttribute#getIsolationLevel()
  190 | 	 * @see org.springframework.transaction.support.AbstractPlatformTransactionManager#setValidateExistingTransaction
  191 | 	 */
  192 | 	Isolation isolation() default Isolation.DEFAULT;
  193 | 
  194 | 	/**
  195 | 	 * The timeout for this transaction (in seconds).
  196 | 	 * <p>Defaults to the default timeout of the underlying transaction system.
  197 | 	 * <p>Exclusively designed for use with {@link Propagation#REQUIRED} or
  198 | 	 * {@link Propagation#REQUIRES_NEW} since it only applies to newly started
  199 | 	 * transactions.
  200 | 	 * @return the timeout in seconds
  201 | 	 * @see org.springframework.transaction.interceptor.TransactionAttribute#getTimeout()
  202 | 	 */
  203 | 	int timeout() default TransactionDefinition.TIMEOUT_DEFAULT;
  204 | 
  205 | 	/**
  206 | 	 * The timeout for this transaction (in seconds).
  207 | 	 * <p>Defaults to the default timeout of the underlying transaction system.
  208 | 	 * <p>Exclusively designed for use with {@link Propagation#REQUIRED} or
  209 | 	 * {@link Propagation#REQUIRES_NEW} since it only applies to newly started
  210 | 	 * transactions.
  211 | 	 * @return the timeout in seconds as a String value, for example, a placeholder
  212 | 	 * @since 5.3
  213 | 	 * @see org.springframework.transaction.interceptor.TransactionAttribute#getTimeout()
  214 | 	 */
  215 | 	String timeoutString() default "";
  216 | 
  217 | 	/**
  218 | 	 * A boolean flag that can be set to {@code true} if the transaction is
  219 | 	 * effectively read-only, allowing for corresponding optimizations at runtime.
  220 | 	 * <p>Defaults to {@code false}.
  221 | 	 * <p>This just serves as a hint for the actual transaction subsystem;
  222 | 	 * it will <i>not necessarily</i> cause failure of write access attempts.
  223 | 	 * A transaction manager which cannot interpret the read-only hint will
  224 | 	 * <i>not</i> throw an exception when asked for a read-only transaction
  225 | 	 * but rather silently ignore the hint.
  226 | 	 * @see org.springframework.transaction.interceptor.TransactionAttribute#isReadOnly()
  227 | 	 * @see org.springframework.transaction.support.TransactionSynchronizationManager#isCurrentTransactionReadOnly()
  228 | 	 */
  229 | 	boolean readOnly() default false;
  230 | 
  231 | 	/**
  232 | 	 * Defines zero (0) or more exception {@linkplain Class types}, which must be
  233 | 	 * subclasses of {@link Throwable}, indicating which exception types must cause
  234 | 	 * a transaction rollback.
  235 | 	 * <p>By default, a transaction will be rolled back on {@link RuntimeException}
  236 | 	 * and {@link Error} but not on checked exceptions (business exceptions). See
  237 | 	 * {@link org.springframework.transaction.interceptor.DefaultTransactionAttribute#rollbackOn(Throwable)}
  238 | 	 * for a detailed explanation.
  239 | 	 * <p>This is the preferred way to construct a rollback rule (in contrast to
  240 | 	 * {@link #rollbackForClassName}), matching the exception type and its subclasses
  241 | 	 * in a type-safe manner. See the {@linkplain Transactional class-level javadocs}
  242 | 	 * for further details on rollback rule semantics.
  243 | 	 * @see #rollbackForClassName
  244 | 	 * @see org.springframework.transaction.interceptor.RollbackRuleAttribute#RollbackRuleAttribute(Class)
  245 | 	 * @see org.springframework.transaction.interceptor.DefaultTransactionAttribute#rollbackOn(Throwable)
  246 | 	 */
  247 | 	Class<? extends Throwable>[] rollbackFor() default {};
  248 | 
  249 | 	/**
  250 | 	 * Defines zero (0) or more exception name patterns (for exceptions which must be a
  251 | 	 * subclass of {@link Throwable}), indicating which exception types must cause
  252 | 	 * a transaction rollback.
  253 | 	 * <p>See the {@linkplain Transactional class-level javadocs} for further details
  254 | 	 * on rollback rule semantics, patterns, and warnings regarding possible
  255 | 	 * unintentional matches.
  256 | 	 * @see #rollbackFor
  257 | 	 * @see org.springframework.transaction.interceptor.RollbackRuleAttribute#RollbackRuleAttribute(String)
  258 | 	 * @see org.springframework.transaction.interceptor.DefaultTransactionAttribute#rollbackOn(Throwable)
  259 | 	 */
  260 | 	String[] rollbackForClassName() default {};
  261 | 
  262 | 	/**
  263 | 	 * Defines zero (0) or more exception {@link Class types}, which must be
  264 | 	 * subclasses of {@link Throwable}, indicating which exception types must
  265 | 	 * <b>not</b> cause a transaction rollback.
  266 | 	 * <p>This is the preferred way to construct a rollback rule (in contrast to
  267 | 	 * {@link #noRollbackForClassName}), matching the exception type and its subclasses
  268 | 	 * in a type-safe manner. See the {@linkplain Transactional class-level javadocs}
  269 | 	 * for further details on rollback rule semantics.
  270 | 	 * @see #noRollbackForClassName
  271 | 	 * @see org.springframework.transaction.interceptor.NoRollbackRuleAttribute#NoRollbackRuleAttribute(Class)
  272 | 	 * @see org.springframework.transaction.interceptor.DefaultTransactionAttribute#rollbackOn(Throwable)
  273 | 	 */
  274 | 	Class<? extends Throwable>[] noRollbackFor() default {};
  275 | 
  276 | 	/**
  277 | 	 * Defines zero (0) or more exception name patterns (for exceptions which must be a
  278 | 	 * subclass of {@link Throwable}) indicating which exception types must <b>not</b>
  279 | 	 * cause a transaction rollback.
  280 | 	 * <p>See the {@linkplain Transactional class-level javadocs} for further details
  281 | 	 * on rollback rule semantics, patterns, and warnings regarding possible
  282 | 	 * unintentional matches.
  283 | 	 * @see #noRollbackFor
  284 | 	 * @see org.springframework.transaction.interceptor.NoRollbackRuleAttribute#NoRollbackRuleAttribute(String)
  285 | 	 * @see org.springframework.transaction.interceptor.DefaultTransactionAttribute#rollbackOn(Throwable)
  286 | 	 */
  287 | 	String[] noRollbackForClassName() default {};
  288 | 
  289 | }
```
