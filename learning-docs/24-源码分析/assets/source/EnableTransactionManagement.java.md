<!-- learning-docs source archive（机械抓取原文，禁止手改代码内容） -->
<!-- origin: org.springframework:spring-tx:7.1.0 | path: org/springframework/transaction/annotation/EnableTransactionManagement.java -->
<!-- 正文引用：[[源码:EnableTransactionManagement.java#L起-L止]] ；代码块头部：(src: EnableTransactionManagement.java#L起-L止) -->

# EnableTransactionManagement.java

| 项 | 值 |
| --- | --- |
| 来源 | `org.springframework:spring-tx:7.1.0` |
| 路径 | `org/springframework/transaction/annotation/EnableTransactionManagement.java` |
| 行数 | 224 |
| 在线地址 | https://repo1.maven.org/maven2/org/springframework/spring-tx/7.1.0/spring-tx-7.1.0-sources.jar |
| 抓取时间 | 2026-11-25T14:00:56.559Z |

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
   21 | import java.lang.annotation.Retention;
   22 | import java.lang.annotation.RetentionPolicy;
   23 | import java.lang.annotation.Target;
   24 | 
   25 | import org.springframework.context.annotation.AdviceMode;
   26 | import org.springframework.context.annotation.Import;
   27 | import org.springframework.core.Ordered;
   28 | 
   29 | /**
   30 |  * Enables Spring's annotation-driven transaction management capability, similar to
   31 |  * the support found in Spring's {@code <tx:*>} XML namespace. To be used on
   32 |  * {@link org.springframework.context.annotation.Configuration @Configuration}
   33 |  * classes to configure traditional, imperative transaction management or
   34 |  * reactive transaction management.
   35 |  *
   36 |  * <p>The following example demonstrates imperative transaction management
   37 |  * using a {@link org.springframework.transaction.PlatformTransactionManager
   38 |  * PlatformTransactionManager}. For reactive transaction management, configure a
   39 |  * {@link org.springframework.transaction.ReactiveTransactionManager
   40 |  * ReactiveTransactionManager} instead.
   41 |  *
   42 |  * <pre class="code">
   43 |  * &#064;Configuration
   44 |  * &#064;EnableTransactionManagement
   45 |  * public class AppConfig {
   46 |  *
   47 |  *     &#064;Bean
   48 |  *     public FooRepository fooRepository() {
   49 |  *         // configure and return a class having &#064;Transactional methods
   50 |  *         return new JdbcFooRepository(dataSource());
   51 |  *     }
   52 |  *
   53 |  *     &#064;Bean
   54 |  *     public DataSource dataSource() {
   55 |  *         // configure and return the necessary JDBC DataSource
   56 |  *     }
   57 |  *
   58 |  *     &#064;Bean
   59 |  *     public PlatformTransactionManager txManager() {
   60 |  *         return new DataSourceTransactionManager(dataSource());
   61 |  *     }
   62 |  * }</pre>
   63 |  *
   64 |  * <p>For reference, the example above can be compared to the following Spring XML
   65 |  * configuration:
   66 |  *
   67 |  * <pre class="code">
   68 |  * &lt;beans&gt;
   69 |  *
   70 |  *     &lt;tx:annotation-driven/&gt;
   71 |  *
   72 |  *     &lt;bean id="fooRepository" class="com.foo.JdbcFooRepository"&gt;
   73 |  *         &lt;constructor-arg ref="dataSource"/&gt;
   74 |  *     &lt;/bean&gt;
   75 |  *
   76 |  *     &lt;bean id="dataSource" class="com.vendor.VendorDataSource"/&gt;
   77 |  *
   78 |  *     &lt;bean id="transactionManager" class="org.sfwk...DataSourceTransactionManager"&gt;
   79 |  *         &lt;constructor-arg ref="dataSource"/&gt;
   80 |  *     &lt;/bean&gt;
   81 |  *
   82 |  * &lt;/beans&gt;
   83 |  * </pre>
   84 |  *
   85 |  * In both of the scenarios above, {@code @EnableTransactionManagement} and {@code
   86 |  * <tx:annotation-driven/>} are responsible for registering the necessary Spring
   87 |  * components that power annotation-driven transaction management, such as the
   88 |  * TransactionInterceptor and the proxy- or AspectJ-based advice that weaves the
   89 |  * interceptor into the call stack when {@code JdbcFooRepository}'s {@code @Transactional}
   90 |  * methods are invoked.
   91 |  *
   92 |  * <p>A minor difference between the two examples lies in the naming of the {@code
   93 |  * TransactionManager} bean: In the {@code @Bean} case, the name is
   94 |  * <em>"txManager"</em> (per the name of the method); in the XML case, the name is
   95 |  * <em>"transactionManager"</em>. {@code <tx:annotation-driven/>} is hard-wired to
   96 |  * look for a bean named "transactionManager" by default, however
   97 |  * {@code @EnableTransactionManagement} is more flexible; it will fall back to a by-type
   98 |  * lookup for any {@code TransactionManager} bean in the container. Thus the name
   99 |  * can be "txManager", "transactionManager", or "tm": it simply does not matter.
  100 |  *
  101 |  * <p>For those that wish to establish a more direct relationship between
  102 |  * {@code @EnableTransactionManagement} and the exact transaction manager bean to be used,
  103 |  * the {@link TransactionManagementConfigurer} callback interface may be implemented -
  104 |  * notice the {@code implements} clause and the {@code @Override}-annotated method below:
  105 |  *
  106 |  * <pre class="code">
  107 |  * &#064;Configuration
  108 |  * &#064;EnableTransactionManagement
  109 |  * public class AppConfig implements TransactionManagementConfigurer {
  110 |  *
  111 |  *     &#064;Bean
  112 |  *     public FooRepository fooRepository() {
  113 |  *         // configure and return a class having &#064;Transactional methods
  114 |  *         return new JdbcFooRepository(dataSource());
  115 |  *     }
  116 |  *
  117 |  *     &#064;Bean
  118 |  *     public DataSource dataSource() {
  119 |  *         // configure and return the necessary JDBC DataSource
  120 |  *     }
  121 |  *
  122 |  *     &#064;Bean
  123 |  *     public PlatformTransactionManager txManager() {
  124 |  *         return new DataSourceTransactionManager(dataSource());
  125 |  *     }
  126 |  *
  127 |  *     &#064;Override
  128 |  *     public PlatformTransactionManager annotationDrivenTransactionManager() {
  129 |  *         return txManager();
  130 |  *     }
  131 |  * }</pre>
  132 |  *
  133 |  * <p>This approach may be desirable simply because it is more explicit, or it may be
  134 |  * necessary in order to distinguish between two {@code TransactionManager} beans
  135 |  * present in the same container.  As the name suggests, the
  136 |  * {@code annotationDrivenTransactionManager()} will be the one used for processing
  137 |  * {@code @Transactional} methods. See {@link TransactionManagementConfigurer} Javadoc
  138 |  * for further details.
  139 |  *
  140 |  * <p>The {@link #mode} attribute controls how advice is applied: If the mode is
  141 |  * {@link AdviceMode#PROXY} (the default), then the other attributes control the behavior
  142 |  * of the proxying. Please note that proxy mode allows for interception of calls through
  143 |  * the proxy only; local calls within the same class cannot get intercepted that way.
  144 |  *
  145 |  * <p>Note that if the {@linkplain #mode} is set to {@link AdviceMode#ASPECTJ}, then the
  146 |  * value of the {@link #proxyTargetClass} attribute will be ignored. Note also that in
  147 |  * this case the {@code spring-aspects} module JAR must be present on the classpath, with
  148 |  * compile-time weaving or load-time weaving applying the aspect to the affected classes.
  149 |  * There is no proxy involved in such a scenario; local calls will be intercepted as well.
  150 |  *
  151 |  * @author Chris Beams
  152 |  * @author Juergen Hoeller
  153 |  * @since 3.1
  154 |  * @see TransactionManagementConfigurer
  155 |  * @see TransactionManagementConfigurationSelector
  156 |  * @see ProxyTransactionManagementConfiguration
  157 |  * @see org.springframework.transaction.aspectj.AspectJTransactionManagementConfiguration
  158 |  */
  159 | @Target(ElementType.TYPE)
  160 | @Retention(RetentionPolicy.RUNTIME)
  161 | @Documented
  162 | @Import(TransactionManagementConfigurationSelector.class)
  163 | public @interface EnableTransactionManagement {
  164 | 
  165 | 	/**
  166 | 	 * Indicate whether subclass-based (CGLIB) proxies are to be created ({@code true})
  167 | 	 * as opposed to standard Java interface-based proxies ({@code false}).
  168 | 	 * The default is {@code false}. <strong>Applicable only if {@link #mode()}
  169 | 	 * is set to {@link AdviceMode#PROXY}</strong>.
  170 | 	 * <p>Note that setting this attribute to {@code true} will affect <em>all</em>
  171 | 	 * Spring-managed beans requiring proxying, not just those marked with
  172 | 	 * {@code @Transactional}. For example, other beans marked with Spring's
  173 | 	 * {@code @Async} annotation will be upgraded to subclass proxying at the same
  174 | 	 * time. This approach has no negative impact in practice unless one is explicitly
  175 | 	 * expecting one type of proxy vs another, for example, in tests.
  176 | 	 * <p>It is usually recommendable to rely on a global default proxy configuration
  177 | 	 * instead, with specific proxy requirements for certain beans expressed through
  178 | 	 * a {@link org.springframework.context.annotation.Proxyable} annotation on
  179 | 	 * the affected bean classes.
  180 | 	 * @see org.springframework.aop.config.AopConfigUtils#forceAutoProxyCreatorToUseClassProxying
  181 | 	 */
  182 | 	boolean proxyTargetClass() default false;
  183 | 
  184 | 	/**
  185 | 	 * Indicate how transactional advice should be applied.
  186 | 	 * <p><b>The default is {@link AdviceMode#PROXY}.</b>
  187 | 	 * Please note that proxy mode allows for interception of calls through the proxy
  188 | 	 * only. Local calls within the same class cannot get intercepted that way; an
  189 | 	 * {@link Transactional} annotation on such a method within a local call will be
  190 | 	 * ignored since Spring's interceptor does not even kick in for such a runtime
  191 | 	 * scenario. For a more advanced mode of interception, consider switching this to
  192 | 	 * {@link AdviceMode#ASPECTJ}.
  193 | 	 */
  194 | 	AdviceMode mode() default AdviceMode.PROXY;
  195 | 
  196 | 	/**
  197 | 	 * Indicate the ordering of the execution of the transaction advisor
  198 | 	 * when multiple advices are applied at a specific joinpoint.
  199 | 	 * <p>The default is {@link Ordered#LOWEST_PRECEDENCE}.
  200 | 	 */
  201 | 	int order() default Ordered.LOWEST_PRECEDENCE;
  202 | 
  203 | 	/**
  204 | 	 * Indicate the rollback behavior for rule-based transactions without
  205 | 	 * custom rollback rules: default is rollback on unchecked exception,
  206 | 	 * this can be switched to rollback on any exception (including checked).
  207 | 	 * <p>Note that transaction-specific rollback rules override the default
  208 | 	 * behavior but retain the chosen default for unspecified exceptions.
  209 | 	 * This is the case for Spring's {@link Transactional} as well as JTA's
  210 | 	 * {@link jakarta.transaction.Transactional} when used with Spring here.
  211 | 	 * <p>Unless you rely on EJB-style business exceptions with commit behavior,
  212 | 	 * it is advisable to switch to {@link RollbackOn#ALL_EXCEPTIONS} for a
  213 | 	 * consistent rollback even in case of a (potentially accidental) checked
  214 | 	 * exception. Also, it is advisable to make that switch for Kotlin-based
  215 | 	 * applications where there is no enforcement of checked exceptions at all.
  216 | 	 * @since 6.2
  217 | 	 * @see Transactional#rollbackFor()
  218 | 	 * @see Transactional#noRollbackFor()
  219 | 	 * @see jakarta.transaction.Transactional#rollbackOn()
  220 | 	 * @see jakarta.transaction.Transactional#dontRollbackOn()
  221 | 	 */
  222 | 	RollbackOn rollbackOn() default RollbackOn.RUNTIME_EXCEPTIONS;
  223 | 
  224 | }
```
