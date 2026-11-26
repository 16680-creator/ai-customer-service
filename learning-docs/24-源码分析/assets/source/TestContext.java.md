<!-- learning-docs source archive（机械抓取原文，禁止手改代码内容） -->
<!-- origin: org.springframework:spring-test:7.1.0 | path: org/springframework/test/context/TestContext.java -->
<!-- 正文引用：[[源码:TestContext.java#L起-L止]] ；代码块头部：(src: TestContext.java#L起-L止) -->

# TestContext.java

| 项 | 值 |
| --- | --- |
| 来源 | `org.springframework:spring-test:7.1.0` |
| 路径 | `org/springframework/test/context/TestContext.java` |
| 行数 | 216 |
| 在线地址 | https://repo1.maven.org/maven2/org/springframework/spring-test/7.1.0/spring-test-7.1.0-sources.jar |
| 抓取时间 | 2026-11-25T14:10:51.658Z |

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
   17 | package org.springframework.test.context;
   18 | 
   19 | import java.io.Serializable;
   20 | import java.lang.reflect.Method;
   21 | import java.util.function.Function;
   22 | 
   23 | import org.jspecify.annotations.Nullable;
   24 | 
   25 | import org.springframework.context.ApplicationContext;
   26 | import org.springframework.context.ApplicationEvent;
   27 | import org.springframework.core.AttributeAccessor;
   28 | import org.springframework.test.annotation.DirtiesContext.HierarchyMode;
   29 | 
   30 | /**
   31 |  * {@code TestContext} encapsulates the context in which a test is executed,
   32 |  * agnostic of the actual testing framework in use.
   33 |  *
   34 |  * <p>Concrete implementations are highly encouraged to implement a <em>copy
   35 |  * constructor</em> in order to allow the immutable state and attributes of a
   36 |  * {@code TestContext} to be used as a template for additional contexts created
   37 |  * for parallel test execution. The copy constructor must accept a single argument
   38 |  * of the type of the concrete implementation. Any implementation that does not
   39 |  * provide a copy constructor will likely fail in an environment that executes
   40 |  * tests concurrently.
   41 |  *
   42 |  * <p>As of Spring Framework 6.1, concrete implementations are highly encouraged to
   43 |  * override {@link #setMethodInvoker(MethodInvoker)} and {@link #getMethodInvoker()}.
   44 |  *
   45 |  * @author Sam Brannen
   46 |  * @since 2.5
   47 |  * @see TestContextManager
   48 |  * @see TestExecutionListener
   49 |  */
   50 | public interface TestContext extends AttributeAccessor, Serializable {
   51 | 
   52 | 	/**
   53 | 	 * Determine if the {@linkplain ApplicationContext application context} for
   54 | 	 * this test context is known to be available.
   55 | 	 * <p>If this method returns {@code true}, a subsequent invocation of
   56 | 	 * {@link #getApplicationContext()} or {@link #markApplicationContextUnused()}
   57 | 	 * should succeed.
   58 | 	 * <p>The default implementation of this method always returns {@code false}.
   59 | 	 * Custom {@code TestContext} implementations are therefore highly encouraged
   60 | 	 * to override this method with a more meaningful implementation. Note that
   61 | 	 * the standard {@code TestContext} implementation in Spring overrides this
   62 | 	 * method appropriately.
   63 | 	 * @return {@code true} if the application context has already been loaded
   64 | 	 * @since 5.2
   65 | 	 * @see #getApplicationContext()
   66 | 	 * @see #markApplicationContextUnused()
   67 | 	 */
   68 | 	default boolean hasApplicationContext() {
   69 | 		return false;
   70 | 	}
   71 | 
   72 | 	/**
   73 | 	 * Get the {@linkplain ApplicationContext application context} for this
   74 | 	 * test context, possibly cached.
   75 | 	 * <p>Implementations of this method are responsible for loading the
   76 | 	 * application context if the corresponding context has not already been
   77 | 	 * loaded, potentially caching the context as well.
   78 | 	 * @return the application context (never {@code null})
   79 | 	 * @throws IllegalStateException if an error occurs while retrieving the
   80 | 	 * application context
   81 | 	 * @see #hasApplicationContext()
   82 | 	 * @see #markApplicationContextUnused()
   83 | 	 */
   84 | 	ApplicationContext getApplicationContext();
   85 | 
   86 | 	/**
   87 | 	 * Publish the {@link ApplicationEvent} created by the given {@code eventFactory}
   88 | 	 * to the {@linkplain ApplicationContext application context} for this
   89 | 	 * test context.
   90 | 	 * <p>The {@code ApplicationEvent} will only be published if the application
   91 | 	 * context for this test context {@linkplain #hasApplicationContext() is available}.
   92 | 	 * @param eventFactory factory for lazy creation of the {@code ApplicationEvent}
   93 | 	 * @since 5.2
   94 | 	 * @see #hasApplicationContext()
   95 | 	 * @see #getApplicationContext()
   96 | 	 */
   97 | 	default void publishEvent(Function<TestContext, ? extends ApplicationEvent> eventFactory) {
   98 | 		if (hasApplicationContext()) {
   99 | 			getApplicationContext().publishEvent(eventFactory.apply(this));
  100 | 		}
  101 | 	}
  102 | 
  103 | 	/**
  104 | 	 * Get the {@linkplain Class test class} for this test context.
  105 | 	 * <p>Since JUnit Jupiter 5.12, if the
  106 | 	 * {@link org.springframework.test.context.junit.jupiter.SpringExtension
  107 | 	 * SpringExtension} is used with a {@linkplain
  108 | 	 * org.junit.jupiter.api.extension.TestInstantiationAwareExtension.ExtensionContextScope#TEST_METHOD
  109 | 	 * test-method scoped} {@link org.junit.jupiter.api.extension.ExtensionContext
  110 | 	 * ExtensionContext}, the {@code Class} returned from this method may refer
  111 | 	 * to the test class for the current {@linkplain #getTestMethod() test method},
  112 | 	 * which may be a {@link org.junit.jupiter.api.Nested @Nested} test class
  113 | 	 * within the class for the {@linkplain #getTestInstance() test instance}.
  114 | 	 * Thus, if you need consistent access to the class for the current test
  115 | 	 * instance within an implementation of
  116 | 	 * {@link TestExecutionListener#prepareTestInstance(TestContext)}, you should
  117 | 	 * invoke {@code testContext.getTestInstance().getClass()} instead of
  118 | 	 * {@code testContext.getTestClass()}.
  119 | 	 * @return the test class (never {@code null})
  120 | 	 * @see org.springframework.test.context.junit.jupiter.SpringExtensionConfig @SpringExtensionConfig
  121 | 	 */
  122 | 	Class<?> getTestClass();
  123 | 
  124 | 	/**
  125 | 	 * Get the current {@linkplain Object test instance} for this test context.
  126 | 	 * <p>Note: this is a mutable property.
  127 | 	 * @return the current test instance (never {@code null})
  128 | 	 * @see #updateState(Object, Method, Throwable)
  129 | 	 */
  130 | 	Object getTestInstance();
  131 | 
  132 | 	/**
  133 | 	 * Get the current {@linkplain Method test method} for this test context.
  134 | 	 * <p>Note: this is a mutable property.
  135 | 	 * @return the current test method (never {@code null})
  136 | 	 * @see #updateState(Object, Method, Throwable)
  137 | 	 */
  138 | 	Method getTestMethod();
  139 | 
  140 | 	/**
  141 | 	 * Get the {@linkplain Throwable exception} that was thrown during execution
  142 | 	 * of the {@linkplain #getTestMethod() test method}.
  143 | 	 * <p>Note: this is a mutable property.
  144 | 	 * @return the exception that was thrown, or {@code null} if no exception was thrown
  145 | 	 * @see #updateState(Object, Method, Throwable)
  146 | 	 */
  147 | 	@Nullable Throwable getTestException();
  148 | 
  149 | 	/**
  150 | 	 * Call this method to signal that the {@linkplain #getTestClass() test class}
  151 | 	 * is no longer using the {@linkplain ApplicationContext application context}
  152 | 	 * associated with this test context.
  153 | 	 * <p>This informs the context cache that the application context can be safely
  154 | 	 * {@linkplain org.springframework.context.ConfigurableApplicationContext#pause() paused}
  155 | 	 * if no other test classes are actively using the same application context.
  156 | 	 * <p>This method is intended to be invoked after execution of the test class
  157 | 	 * has ended and should not be invoked unless the application context for this
  158 | 	 * test context is known to be {@linkplain #hasApplicationContext() available}.
  159 | 	 * <p>This feature is primarily intended for use within the framework.
  160 | 	 * @since 7.0
  161 | 	 * @see TestContextManager#afterTestClass()
  162 | 	 */
  163 | 	default void markApplicationContextUnused() {
  164 | 		/* no-op */
  165 | 	}
  166 | 
  167 | 	/**
  168 | 	 * Call this method to signal that the {@linkplain ApplicationContext application
  169 | 	 * context} associated with this test context is <em>dirty</em> and should be
  170 | 	 * removed from the context cache.
  171 | 	 * <p>Do this if a test has modified the context &mdash; for example, by
  172 | 	 * modifying the state of a singleton bean, modifying the state of an embedded
  173 | 	 * database, etc.
  174 | 	 * @param hierarchyMode the context cache clearing mode to be applied if the
  175 | 	 * context is part of a hierarchy (may be {@code null})
  176 | 	 */
  177 | 	void markApplicationContextDirty(@Nullable HierarchyMode hierarchyMode);
  178 | 
  179 | 	/**
  180 | 	 * Update this test context to reflect the state of the currently executing test.
  181 | 	 * <p><strong>WARNING</strong>: This method should only be invoked by the
  182 | 	 * {@link TestContextManager}.
  183 | 	 * <p>Caution: concurrent invocations of this method might not be thread-safe,
  184 | 	 * depending on the underlying implementation.
  185 | 	 * @param testInstance the current test instance (may be {@code null})
  186 | 	 * @param testMethod the current test method (may be {@code null})
  187 | 	 * @param testException the exception that was thrown in the test method,
  188 | 	 * or {@code null} if no exception was thrown
  189 | 	 */
  190 | 	void updateState(@Nullable Object testInstance, @Nullable Method testMethod, @Nullable Throwable testException);
  191 | 
  192 | 	/**
  193 | 	 * Set the {@link MethodInvoker} to use.
  194 | 	 * <p>By default, this method does nothing.
  195 | 	 * <p>Concrete implementations should track the supplied {@code MethodInvoker}
  196 | 	 * and return it from {@link #getMethodInvoker()}. Note that the standard
  197 | 	 * {@code TestContext} implementation in Spring overrides this method appropriately.
  198 | 	 * @since 6.1
  199 | 	 */
  200 | 	default void setMethodInvoker(MethodInvoker methodInvoker) {
  201 | 		/* no-op */
  202 | 	}
  203 | 
  204 | 	/**
  205 | 	 * Get the {@link MethodInvoker} to use.
  206 | 	 * <p>By default, this method returns {@link MethodInvoker#DEFAULT_INVOKER}.
  207 | 	 * <p>Concrete implementations should return the {@code MethodInvoker} supplied
  208 | 	 * to {@link #setMethodInvoker(MethodInvoker)}. Note that the standard
  209 | 	 * {@code TestContext} implementation in Spring overrides this method appropriately.
  210 | 	 * @since 6.1
  211 | 	 */
  212 | 	default MethodInvoker getMethodInvoker() {
  213 | 		return MethodInvoker.DEFAULT_INVOKER;
  214 | 	}
  215 | 
  216 | }
```
