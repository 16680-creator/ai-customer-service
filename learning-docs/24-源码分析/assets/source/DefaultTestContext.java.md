<!-- learning-docs source archive（机械抓取原文，禁止手改代码内容） -->
<!-- origin: org.springframework:spring-test:7.1.0 | path: org/springframework/test/context/support/DefaultTestContext.java -->
<!-- 正文引用：[[源码:DefaultTestContext.java#L起-L止]] ；代码块头部：(src: DefaultTestContext.java#L起-L止) -->

# DefaultTestContext.java

| 项 | 值 |
| --- | --- |
| 来源 | `org.springframework:spring-test:7.1.0` |
| 路径 | `org/springframework/test/context/support/DefaultTestContext.java` |
| 行数 | 278 |
| 在线地址 | https://repo1.maven.org/maven2/org/springframework/spring-test/7.1.0/spring-test-7.1.0-sources.jar |
| 抓取时间 | 2026-11-25T14:10:52.110Z |

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
   17 | package org.springframework.test.context.support;
   18 | 
   19 | import java.lang.reflect.Method;
   20 | import java.util.Map;
   21 | import java.util.concurrent.ConcurrentHashMap;
   22 | import java.util.function.Function;
   23 | 
   24 | import org.jspecify.annotations.Nullable;
   25 | 
   26 | import org.springframework.context.ApplicationContext;
   27 | import org.springframework.context.ConfigurableApplicationContext;
   28 | import org.springframework.core.style.DefaultToStringStyler;
   29 | import org.springframework.core.style.SimpleValueStyler;
   30 | import org.springframework.core.style.ToStringCreator;
   31 | import org.springframework.test.annotation.DirtiesContext.HierarchyMode;
   32 | import org.springframework.test.context.CacheAwareContextLoaderDelegate;
   33 | import org.springframework.test.context.MergedContextConfiguration;
   34 | import org.springframework.test.context.MethodInvoker;
   35 | import org.springframework.test.context.TestContext;
   36 | import org.springframework.util.Assert;
   37 | import org.springframework.util.StringUtils;
   38 | 
   39 | /**
   40 |  * Default implementation of the {@link TestContext} interface.
   41 |  *
   42 |  * @author Sam Brannen
   43 |  * @author Juergen Hoeller
   44 |  * @author Rob Harrop
   45 |  * @since 4.0
   46 |  */
   47 | @SuppressWarnings("serial")
   48 | public class DefaultTestContext implements TestContext {
   49 | 
   50 | 	private static final long serialVersionUID = -5827157174866681233L;
   51 | 
   52 | 	private final Map<String, Object> attributes = new ConcurrentHashMap<>(4);
   53 | 
   54 | 	private final CacheAwareContextLoaderDelegate cacheAwareContextLoaderDelegate;
   55 | 
   56 | 	private final MergedContextConfiguration mergedConfig;
   57 | 
   58 | 	private final Class<?> testClass;
   59 | 
   60 | 	private volatile @Nullable Object testInstance;
   61 | 
   62 | 	private volatile @Nullable Method testMethod;
   63 | 
   64 | 	private volatile @Nullable Throwable testException;
   65 | 
   66 | 	private volatile MethodInvoker methodInvoker = MethodInvoker.DEFAULT_INVOKER;
   67 | 
   68 | 
   69 | 	/**
   70 | 	 * <em>Copy constructor</em> for creating a new {@code DefaultTestContext}
   71 | 	 * based on the <em>attributes</em> and immutable state of the supplied context.
   72 | 	 * <p><em>Immutable state</em> includes all arguments supplied to the
   73 | 	 * {@linkplain #DefaultTestContext(Class, MergedContextConfiguration,
   74 | 	 * CacheAwareContextLoaderDelegate) standard constructor}.
   75 | 	 * @throws NullPointerException if the supplied {@code DefaultTestContext}
   76 | 	 * is {@code null}
   77 | 	 */
   78 | 	public DefaultTestContext(DefaultTestContext testContext) {
   79 | 		this(testContext.testClass, testContext.mergedConfig,
   80 | 			testContext.cacheAwareContextLoaderDelegate);
   81 | 		this.attributes.putAll(testContext.attributes);
   82 | 	}
   83 | 
   84 | 	/**
   85 | 	 * Construct a new {@code DefaultTestContext} from the supplied arguments.
   86 | 	 * @param testClass the test class for this test context
   87 | 	 * @param mergedConfig the merged application context
   88 | 	 * configuration for this test context
   89 | 	 * @param cacheAwareContextLoaderDelegate the delegate to use for loading
   90 | 	 * and closing the application context for this test context
   91 | 	 */
   92 | 	public DefaultTestContext(Class<?> testClass, MergedContextConfiguration mergedConfig,
   93 | 			CacheAwareContextLoaderDelegate cacheAwareContextLoaderDelegate) {
   94 | 
   95 | 		Assert.notNull(testClass, "Test Class must not be null");
   96 | 		Assert.notNull(mergedConfig, "MergedContextConfiguration must not be null");
   97 | 		Assert.notNull(cacheAwareContextLoaderDelegate, "CacheAwareContextLoaderDelegate must not be null");
   98 | 		this.testClass = testClass;
   99 | 		this.mergedConfig = mergedConfig;
  100 | 		this.cacheAwareContextLoaderDelegate = cacheAwareContextLoaderDelegate;
  101 | 	}
  102 | 
  103 | 	/**
  104 | 	 * Determine if the {@linkplain ApplicationContext application context} for
  105 | 	 * this test context is present in the context cache.
  106 | 	 * @return {@code true} if the application context has already been loaded
  107 | 	 * and stored in the context cache
  108 | 	 * @since 5.2
  109 | 	 * @see #getApplicationContext()
  110 | 	 * @see CacheAwareContextLoaderDelegate#isContextLoaded
  111 | 	 */
  112 | 	@Override
  113 | 	public boolean hasApplicationContext() {
  114 | 		return this.cacheAwareContextLoaderDelegate.isContextLoaded(this.mergedConfig);
  115 | 	}
  116 | 
  117 | 	/**
  118 | 	 * Get the {@linkplain ApplicationContext application context} for this
  119 | 	 * test context.
  120 | 	 * <p>The default implementation delegates to the {@link CacheAwareContextLoaderDelegate}
  121 | 	 * that was supplied when this {@code TestContext} was constructed.
  122 | 	 * @throws IllegalStateException if the context returned by the context
  123 | 	 * loader delegate is not <em>active</em> (i.e., has been closed)
  124 | 	 * @see CacheAwareContextLoaderDelegate#loadContext
  125 | 	 */
  126 | 	@Override
  127 | 	public ApplicationContext getApplicationContext() {
  128 | 		ApplicationContext context = this.cacheAwareContextLoaderDelegate.loadContext(this.mergedConfig);
  129 | 		if (context instanceof ConfigurableApplicationContext cac) {
  130 | 			Assert.state(cac.isActive(), () -> """
  131 | 					The ApplicationContext loaded for %s is not active. \
  132 | 					This may be due to one of the following reasons: \
  133 | 					1) the context was closed programmatically by user code; \
  134 | 					2) the context was closed during parallel test execution either \
  135 | 					according to @DirtiesContext semantics or due to automatic eviction \
  136 | 					from the ContextCache due to a maximum cache size policy."""
  137 | 						.formatted(this.mergedConfig));
  138 | 		}
  139 | 		this.cacheAwareContextLoaderDelegate.registerContextUsage(this.mergedConfig, this.testClass);
  140 | 		return context;
  141 | 	}
  142 | 
  143 | 	/**
  144 | 	 * Mark the {@linkplain ApplicationContext application context} associated
  145 | 	 * with this test context as <em>unused</em> so that it can be safely
  146 | 	 * {@linkplain org.springframework.context.ConfigurableApplicationContext#pause() paused}
  147 | 	 * if no other test classes are actively using the same application context.
  148 | 	 * <p>The default implementation delegates to the {@link CacheAwareContextLoaderDelegate}
  149 | 	 * that was supplied when this {@code TestContext} was constructed.
  150 | 	 * @since 7.0
  151 | 	 * @see CacheAwareContextLoaderDelegate#unregisterContextUsage(MergedContextConfiguration, Class)
  152 | 	 */
  153 | 	@Override
  154 | 	public void markApplicationContextUnused() {
  155 | 		this.cacheAwareContextLoaderDelegate.unregisterContextUsage(this.mergedConfig, this.testClass);
  156 | 	}
  157 | 
  158 | 	/**
  159 | 	 * Mark the {@linkplain ApplicationContext application context} associated
  160 | 	 * with this test context as <em>dirty</em> (i.e., by removing it from the
  161 | 	 * context cache and closing it).
  162 | 	 * <p>The default implementation delegates to the {@link CacheAwareContextLoaderDelegate}
  163 | 	 * that was supplied when this {@code TestContext} was constructed.
  164 | 	 * @see CacheAwareContextLoaderDelegate#closeContext
  165 | 	 */
  166 | 	@Override
  167 | 	public void markApplicationContextDirty(@Nullable HierarchyMode hierarchyMode) {
  168 | 		this.cacheAwareContextLoaderDelegate.closeContext(this.mergedConfig, hierarchyMode);
  169 | 	}
  170 | 
  171 | 	@Override
  172 | 	public final Class<?> getTestClass() {
  173 | 		return this.testClass;
  174 | 	}
  175 | 
  176 | 	@Override
  177 | 	public final Object getTestInstance() {
  178 | 		Object testInstance = this.testInstance;
  179 | 		Assert.state(testInstance != null, "No test instance");
  180 | 		return testInstance;
  181 | 	}
  182 | 
  183 | 	@Override
  184 | 	public final Method getTestMethod() {
  185 | 		Method testMethod = this.testMethod;
  186 | 		Assert.state(testMethod != null, "No test method");
  187 | 		return testMethod;
  188 | 	}
  189 | 
  190 | 	@Override
  191 | 	public final @Nullable Throwable getTestException() {
  192 | 		return this.testException;
  193 | 	}
  194 | 
  195 | 	@Override
  196 | 	public void updateState(@Nullable Object testInstance, @Nullable Method testMethod, @Nullable Throwable testException) {
  197 | 		this.testInstance = testInstance;
  198 | 		this.testMethod = testMethod;
  199 | 		this.testException = testException;
  200 | 	}
  201 | 
  202 | 	@Override
  203 | 	public final void setMethodInvoker(MethodInvoker methodInvoker) {
  204 | 		Assert.notNull(methodInvoker, "MethodInvoker must not be null");
  205 | 		this.methodInvoker = methodInvoker;
  206 | 	}
  207 | 
  208 | 	@Override
  209 | 	public final MethodInvoker getMethodInvoker() {
  210 | 		return this.methodInvoker;
  211 | 	}
  212 | 
  213 | 	@Override
  214 | 	public void setAttribute(String name, @Nullable Object value) {
  215 | 		Assert.notNull(name, "Name must not be null");
  216 | 		synchronized (this.attributes) {
  217 | 			if (value != null) {
  218 | 				this.attributes.put(name, value);
  219 | 			}
  220 | 			else {
  221 | 				this.attributes.remove(name);
  222 | 			}
  223 | 		}
  224 | 	}
  225 | 
  226 | 	@Override
  227 | 	public @Nullable Object getAttribute(String name) {
  228 | 		Assert.notNull(name, "Name must not be null");
  229 | 		return this.attributes.get(name);
  230 | 	}
  231 | 
  232 | 	@Override
  233 | 	@SuppressWarnings("unchecked")
  234 | 	public <T> T computeAttribute(String name, Function<String, T> computeFunction) {
  235 | 		Assert.notNull(name, "Name must not be null");
  236 | 		Assert.notNull(computeFunction, "Compute function must not be null");
  237 | 		Object value = this.attributes.computeIfAbsent(name, computeFunction);
  238 | 		Assert.state(value != null,
  239 | 				() -> "Compute function must not return null for attribute named '%s'".formatted(name));
  240 | 		return (T) value;
  241 | 	}
  242 | 
  243 | 	@Override
  244 | 	public @Nullable Object removeAttribute(String name) {
  245 | 		Assert.notNull(name, "Name must not be null");
  246 | 		return this.attributes.remove(name);
  247 | 	}
  248 | 
  249 | 	@Override
  250 | 	public boolean hasAttribute(String name) {
  251 | 		Assert.notNull(name, "Name must not be null");
  252 | 		return this.attributes.containsKey(name);
  253 | 	}
  254 | 
  255 | 	@Override
  256 | 	public String[] attributeNames() {
  257 | 		synchronized (this.attributes) {
  258 | 			return StringUtils.toStringArray(this.attributes.keySet());
  259 | 		}
  260 | 	}
  261 | 
  262 | 
  263 | 	/**
  264 | 	 * Provide a String representation of this test context's state.
  265 | 	 */
  266 | 	@Override
  267 | 	public String toString() {
  268 | 		return new ToStringCreator(this, new DefaultToStringStyler(new SimpleValueStyler()))
  269 | 				.append("testClass", this.testClass)
  270 | 				.append("testInstance", this.testInstance)
  271 | 				.append("testMethod", this.testMethod)
  272 | 				.append("testException", this.testException)
  273 | 				.append("mergedContextConfiguration", this.mergedConfig)
  274 | 				.append("attributes", this.attributes)
  275 | 				.toString();
  276 | 	}
  277 | 
  278 | }
```
