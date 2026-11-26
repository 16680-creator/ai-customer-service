<!-- learning-docs source archive（机械抓取原文，禁止手改代码内容） -->
<!-- origin: org.springframework:spring-test:7.1.0 | path: org/springframework/test/context/bean/override/mockito/MockitoBean.java -->
<!-- 正文引用：[[源码:MockitoBean.java#L起-L止]] ；代码块头部：(src: MockitoBean.java#L起-L止) -->

# MockitoBean.java

| 项 | 值 |
| --- | --- |
| 来源 | `org.springframework:spring-test:7.1.0` |
| 路径 | `org/springframework/test/context/bean/override/mockito/MockitoBean.java` |
| 行数 | 216 |
| 在线地址 | https://repo1.maven.org/maven2/org/springframework/spring-test/7.1.0/spring-test-7.1.0-sources.jar |
| 抓取时间 | 2026-11-25T14:18:34.708Z |

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
   17 | package org.springframework.test.context.bean.override.mockito;
   18 | 
   19 | import java.lang.annotation.Documented;
   20 | import java.lang.annotation.ElementType;
   21 | import java.lang.annotation.Repeatable;
   22 | import java.lang.annotation.Retention;
   23 | import java.lang.annotation.RetentionPolicy;
   24 | import java.lang.annotation.Target;
   25 | 
   26 | import org.mockito.Answers;
   27 | import org.mockito.MockSettings;
   28 | 
   29 | import org.springframework.core.annotation.AliasFor;
   30 | import org.springframework.test.context.bean.override.BeanOverride;
   31 | 
   32 | /**
   33 |  * {@code @MockitoBean} is an annotation that can be used in test classes to
   34 |  * override a bean in the test's
   35 |  * {@link org.springframework.context.ApplicationContext ApplicationContext}
   36 |  * with a Mockito mock.
   37 |  *
   38 |  * <p>{@code @MockitoBean} can be applied in the following ways.
   39 |  * <ul>
   40 |  * <li>On a non-static field in a test class or any of its superclasses.</li>
   41 |  * <li>On a non-static field in an enclosing class for a {@code @Nested} test class
   42 |  * or in any class in the type hierarchy or enclosing class hierarchy above the
   43 |  * {@code @Nested} test class.</li>
   44 |  * <li>On a parameter in the constructor for the test class.</li>
   45 |  * <li>At the type level on a test class or any superclass or implemented interface
   46 |  * in the type hierarchy above the test class.</li>
   47 |  * <li>At the type level on an enclosing class for a {@code @Nested} test class
   48 |  * or on any class or interface in the type hierarchy or enclosing class hierarchy
   49 |  * above the {@code @Nested} test class.</li>
   50 |  * </ul>
   51 |  *
   52 |  * <p>When {@code @MockitoBean} is declared on a field or parameter, the bean to
   53 |  * mock is inferred from the type of the annotated field or parameter. If multiple
   54 |  * candidates exist in the {@code ApplicationContext}, a {@code @Qualifier} annotation
   55 |  * can be declared on the field or parameter to help disambiguate. In the absence
   56 |  * of a {@code @Qualifier} annotation, the name of the annotated field or parameter
   57 |  * will be used as a <em>fallback qualifier</em>. Alternatively, you can explicitly
   58 |  * specify a bean name to mock by setting the {@link #value() value} or
   59 |  * {@link #name() name} attribute.
   60 |  *
   61 |  * <p>When {@code @MockitoBean} is declared at the type level, the type of bean
   62 |  * (or beans) to mock must be supplied via the {@link #types() types} attribute.
   63 |  * If multiple candidates exist in the {@code ApplicationContext}, you can
   64 |  * explicitly specify a bean name to mock by setting the {@link #name() name}
   65 |  * attribute. Note, however, that the {@code types} attribute must contain a
   66 |  * single type if an explicit bean {@code name} is configured.
   67 |  *
   68 |  * <p>A bean will be created if a corresponding bean does not exist. However, if
   69 |  * you would like for the test to fail when a corresponding bean does not exist,
   70 |  * you can set the {@link #enforceOverride() enforceOverride} attribute to {@code true}
   71 |  * &mdash; for example,  {@code @MockitoBean(enforceOverride = true)}.
   72 |  *
   73 |  * <p>Dependencies that are known to the application context but are not beans
   74 |  * (such as those
   75 |  * {@linkplain org.springframework.beans.factory.config.ConfigurableListableBeanFactory#registerResolvableDependency(Class, Object)
   76 |  * registered directly}) will not be found, and a mocked bean will be added to
   77 |  * the context alongside the existing dependency.
   78 |  *
   79 |  * <p><strong>WARNING</strong>: Using {@code @MockitoBean} in conjunction with
   80 |  * {@code @ContextHierarchy} can lead to undesirable results since each
   81 |  * {@code @MockitoBean} will be applied to all context hierarchy levels by default.
   82 |  * To ensure that a particular {@code @MockitoBean} is applied to a single context
   83 |  * hierarchy level, set the {@link #contextName() contextName} to match a
   84 |  * configured {@code @ContextConfiguration}
   85 |  * {@link org.springframework.test.context.ContextConfiguration#name() name}.
   86 |  * See the Javadoc for {@link org.springframework.test.context.ContextHierarchy @ContextHierarchy}
   87 |  * for further details and examples.
   88 |  *
   89 |  * <p><strong>NOTE</strong>: When mocking a non-singleton bean, the non-singleton
   90 |  * bean will be replaced with a singleton mock, and the corresponding bean definition
   91 |  * will be converted to a singleton. Consequently, if you mock a prototype or scoped
   92 |  * bean, the mock will be treated as a singleton. Similarly, when mocking a bean
   93 |  * created by a {@link org.springframework.beans.factory.FactoryBean FactoryBean},
   94 |  * the {@code FactoryBean} will be replaced with a singleton mock of the type of
   95 |  * object created by the {@code FactoryBean}.
   96 |  *
   97 |  * <p>There are no restrictions on the visibility of a {@code @MockitoBean} field.
   98 |  * Such fields can therefore be {@code public}, {@code protected}, package-private
   99 |  * (default visibility), or {@code private} depending on the needs or coding
  100 |  * practices of the project.
  101 |  *
  102 |  * <p>{@code @MockitoBean} fields and type-level {@code @MockitoBean} declarations
  103 |  * will be inherited from an enclosing test class by default. See
  104 |  * {@link org.springframework.test.context.NestedTestConfiguration @NestedTestConfiguration}
  105 |  * for details.
  106 |  *
  107 |  * <p>{@code @MockitoBean} may be used as a <em>meta-annotation</em> to create custom
  108 |  * <em>composed annotations</em> &mdash; for example, to define common mock
  109 |  * configuration in a single annotation that can be reused across a test suite.
  110 |  * {@code @MockitoBean} can also be used as a <em>{@linkplain Repeatable repeatable}</em>
  111 |  * annotation at the type level &mdash; for example, to mock several beans by
  112 |  * {@link #name() name}.
  113 |  *
  114 |  * @author Simon Baslé
  115 |  * @author Sam Brannen
  116 |  * @since 6.2
  117 |  * @see org.springframework.test.context.bean.override.mockito.MockitoBeans @MockitoBeans
  118 |  * @see org.springframework.test.context.bean.override.mockito.MockitoSpyBean @MockitoSpyBean
  119 |  * @see org.springframework.test.context.bean.override.convention.TestBean @TestBean
  120 |  */
  121 | @Target({ElementType.FIELD, ElementType.PARAMETER, ElementType.TYPE})
  122 | @Retention(RetentionPolicy.RUNTIME)
  123 | @Documented
  124 | @Repeatable(MockitoBeans.class)
  125 | @BeanOverride(MockitoBeanOverrideProcessor.class)
  126 | public @interface MockitoBean {
  127 | 
  128 | 	/**
  129 | 	 * Alias for {@link #name() name}.
  130 | 	 * <p>Intended to be used when no other attributes are needed &mdash; for
  131 | 	 * example, {@code @MockitoBean("customBeanName")}.
  132 | 	 * @see #name()
  133 | 	 */
  134 | 	@AliasFor("name")
  135 | 	String value() default "";
  136 | 
  137 | 	/**
  138 | 	 * Name of the bean to mock.
  139 | 	 * <p>If left unspecified, the bean to mock is selected according to the
  140 | 	 * configured {@link #types() types} or the type of the annotated field or
  141 | 	 * parameter, taking qualifiers into account if necessary. See the
  142 | 	 * {@linkplain MockitoBean class-level documentation} for details.
  143 | 	 * @see #value()
  144 | 	 */
  145 | 	@AliasFor("value")
  146 | 	String name() default "";
  147 | 
  148 | 	/**
  149 | 	 * One or more types to mock.
  150 | 	 * <p>Defaults to none.
  151 | 	 * <p>Each type specified will result in a mock being created and registered
  152 | 	 * with the {@code ApplicationContext}.
  153 | 	 * <p>Types must be omitted when the annotation is used on a field or parameter.
  154 | 	 * <p>When {@code @MockitoBean} also defines a {@link #name name}, this attribute
  155 | 	 * can only contain a single value.
  156 | 	 * @return the types to mock
  157 | 	 * @since 6.2.2
  158 | 	 */
  159 | 	Class<?>[] types() default {};
  160 | 
  161 | 	/**
  162 | 	 * The name of the context hierarchy level in which this {@code @MockitoBean}
  163 | 	 * should be applied.
  164 | 	 * <p>Defaults to an empty string which indicates that this {@code @MockitoBean}
  165 | 	 * should be applied to all application contexts.
  166 | 	 * <p>If a context name is configured, it must match a name configured via
  167 | 	 * {@code @ContextConfiguration(name=...)}.
  168 | 	 * @since 6.2.6
  169 | 	 * @see org.springframework.test.context.ContextHierarchy @ContextHierarchy
  170 | 	 * @see org.springframework.test.context.ContextConfiguration#name() @ContextConfiguration(name=...)
  171 | 	 */
  172 | 	String contextName() default "";
  173 | 
  174 | 	/**
  175 | 	 * Extra interfaces that should also be declared by the mock.
  176 | 	 * <p>Defaults to none.
  177 | 	 * @return any extra interfaces
  178 | 	 * @see MockSettings#extraInterfaces(Class...)
  179 | 	 */
  180 | 	Class<?>[] extraInterfaces() default {};
  181 | 
  182 | 	/**
  183 | 	 * The {@link Answers} type to use in the mock.
  184 | 	 * <p>Defaults to {@link Answers#RETURNS_DEFAULTS}.
  185 | 	 * @return the answer type
  186 | 	 */
  187 | 	Answers answers() default Answers.RETURNS_DEFAULTS;
  188 | 
  189 | 	/**
  190 | 	 * Whether the generated mock is serializable.
  191 | 	 * <p>Defaults to {@code false}.
  192 | 	 * @return {@code true} if the mock is serializable
  193 | 	 * @see MockSettings#serializable()
  194 | 	 */
  195 | 	boolean serializable() default false;
  196 | 
  197 | 	/**
  198 | 	 * The reset mode to apply to the mock.
  199 | 	 * <p>The default is {@link MockReset#AFTER} meaning that mocks are
  200 | 	 * automatically reset after each test method is invoked.
  201 | 	 * @return the reset mode
  202 | 	 */
  203 | 	MockReset reset() default MockReset.AFTER;
  204 | 
  205 | 	/**
  206 | 	 * Whether to require the existence of the bean being mocked.
  207 | 	 * <p>Defaults to {@code false} which means that a mock will be created if a
  208 | 	 * corresponding bean does not exist.
  209 | 	 * <p>Set to {@code true} to cause an exception to be thrown if a corresponding
  210 | 	 * bean does not exist.
  211 | 	 * @see org.springframework.test.context.bean.override.BeanOverrideStrategy#REPLACE_OR_CREATE
  212 | 	 * @see org.springframework.test.context.bean.override.BeanOverrideStrategy#REPLACE
  213 | 	 */
  214 | 	boolean enforceOverride() default false;
  215 | 
  216 | }
```
