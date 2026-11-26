<!-- learning-docs source archive（机械抓取原文，禁止手改代码内容） -->
<!-- origin: org.springframework.security:spring-security-core:7.2.0 | path: org/springframework/security/core/context/SecurityContextHolder.java -->
<!-- 正文引用：[[源码:SecurityContextHolder.java#L起-L止]] ；代码块头部：(src: SecurityContextHolder.java#L起-L止) -->

# SecurityContextHolder.java

| 项 | 值 |
| --- | --- |
| 来源 | `org.springframework.security:spring-security-core:7.2.0` |
| 路径 | `org/springframework/security/core/context/SecurityContextHolder.java` |
| 行数 | 236 |
| 在线地址 | https://repo1.maven.org/maven2/org/springframework/security/spring-security-core/7.2.0/spring-security-core-7.2.0-sources.jar |
| 抓取时间 | 2026-11-25T16:38:37.653Z |

```java
    1 | /*
    2 |  * Copyright 2004, 2005, 2006 Acegi Technology Pty Limited
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
   17 | package org.springframework.security.core.context;
   18 | 
   19 | import java.lang.reflect.Constructor;
   20 | import java.util.function.Supplier;
   21 | 
   22 | import org.springframework.util.Assert;
   23 | import org.springframework.util.ReflectionUtils;
   24 | import org.springframework.util.StringUtils;
   25 | 
   26 | /**
   27 |  * Associates a given {@link SecurityContext} with the current execution thread.
   28 |  * <p>
   29 |  * This class provides a series of static methods that delegate to an instance of
   30 |  * {@link org.springframework.security.core.context.SecurityContextHolderStrategy}. The
   31 |  * purpose of the class is to provide a convenient way to specify the strategy that should
   32 |  * be used for a given JVM. This is a JVM-wide setting, since everything in this class is
   33 |  * <code>static</code> to facilitate ease of use in calling code.
   34 |  * <p>
   35 |  * To specify which strategy should be used, you must provide a mode setting. A mode
   36 |  * setting is one of the three valid <code>MODE_</code> settings defined as
   37 |  * <code>static final</code> fields, or a fully qualified classname to a concrete
   38 |  * implementation of
   39 |  * {@link org.springframework.security.core.context.SecurityContextHolderStrategy} that
   40 |  * provides a public no-argument constructor.
   41 |  * <p>
   42 |  * There are two ways to specify the desired strategy mode <code>String</code>. The first
   43 |  * is to specify it via the system property keyed on {@link #SYSTEM_PROPERTY}. The second
   44 |  * is to call {@link #setStrategyName(String)} before using the class. If neither approach
   45 |  * is used, the class will default to using {@link #MODE_THREADLOCAL}, which is backwards
   46 |  * compatible, has fewer JVM incompatibilities and is appropriate on servers (whereas
   47 |  * {@link #MODE_GLOBAL} is definitely inappropriate for server use).
   48 |  *
   49 |  * @author Ben Alex
   50 |  * @author Rob Winch
   51 |  *
   52 |  */
   53 | public class SecurityContextHolder {
   54 | 
   55 | 	public static final String MODE_THREADLOCAL = "MODE_THREADLOCAL";
   56 | 
   57 | 	public static final String MODE_INHERITABLETHREADLOCAL = "MODE_INHERITABLETHREADLOCAL";
   58 | 
   59 | 	public static final String MODE_GLOBAL = "MODE_GLOBAL";
   60 | 
   61 | 	private static final String MODE_PRE_INITIALIZED = "MODE_PRE_INITIALIZED";
   62 | 
   63 | 	public static final String SYSTEM_PROPERTY = "spring.security.strategy";
   64 | 
   65 | 	private static String strategyName = System.getProperty(SYSTEM_PROPERTY);
   66 | 
   67 | 	private static SecurityContextHolderStrategy strategy = new ThreadLocalSecurityContextHolderStrategy();
   68 | 
   69 | 	private static int initializeCount = 0;
   70 | 
   71 | 	static {
   72 | 		initialize();
   73 | 	}
   74 | 
   75 | 	private static void initialize() {
   76 | 		initializeStrategy();
   77 | 		initializeCount++;
   78 | 	}
   79 | 
   80 | 	private static void initializeStrategy() {
   81 | 		if (MODE_PRE_INITIALIZED.equals(strategyName)) {
   82 | 			Assert.state(strategy != null, "When using " + MODE_PRE_INITIALIZED
   83 | 					+ ", setContextHolderStrategy must be called with the fully constructed strategy");
   84 | 			return;
   85 | 		}
   86 | 		if (!StringUtils.hasText(strategyName)) {
   87 | 			// Set default
   88 | 			strategyName = MODE_THREADLOCAL;
   89 | 		}
   90 | 		if (strategyName.equals(MODE_THREADLOCAL)) {
   91 | 			strategy = new ThreadLocalSecurityContextHolderStrategy();
   92 | 			return;
   93 | 		}
   94 | 		if (strategyName.equals(MODE_INHERITABLETHREADLOCAL)) {
   95 | 			strategy = new InheritableThreadLocalSecurityContextHolderStrategy();
   96 | 			return;
   97 | 		}
   98 | 		if (strategyName.equals(MODE_GLOBAL)) {
   99 | 			strategy = new GlobalSecurityContextHolderStrategy();
  100 | 			return;
  101 | 		}
  102 | 		// Try to load a custom strategy
  103 | 		try {
  104 | 			Class<?> clazz = Class.forName(strategyName);
  105 | 			Constructor<?> customStrategy = clazz.getConstructor();
  106 | 			strategy = (SecurityContextHolderStrategy) customStrategy.newInstance();
  107 | 		}
  108 | 		catch (Exception ex) {
  109 | 			ReflectionUtils.handleReflectionException(ex);
  110 | 		}
  111 | 	}
  112 | 
  113 | 	/**
  114 | 	 * Explicitly clears the context value from the current thread.
  115 | 	 */
  116 | 	public static void clearContext() {
  117 | 		strategy.clearContext();
  118 | 	}
  119 | 
  120 | 	/**
  121 | 	 * Obtain the current <code>SecurityContext</code>.
  122 | 	 * @return the security context (never <code>null</code>)
  123 | 	 */
  124 | 	public static SecurityContext getContext() {
  125 | 		return strategy.getContext();
  126 | 	}
  127 | 
  128 | 	/**
  129 | 	 * Obtains a {@link Supplier} that returns the current context.
  130 | 	 * @return a {@link Supplier} that returns the current context (never
  131 | 	 * <code>null</code> - create a default implementation if necessary)
  132 | 	 * @since 5.8
  133 | 	 */
  134 | 	public static Supplier<SecurityContext> getDeferredContext() {
  135 | 		return strategy.getDeferredContext();
  136 | 	}
  137 | 
  138 | 	/**
  139 | 	 * Primarily for troubleshooting purposes, this method shows how many times the class
  140 | 	 * has re-initialized its <code>SecurityContextHolderStrategy</code>.
  141 | 	 * @return the count (should be one unless you've called
  142 | 	 * {@link #setStrategyName(String)} or
  143 | 	 * {@link #setContextHolderStrategy(SecurityContextHolderStrategy)} to switch to an
  144 | 	 * alternate strategy).
  145 | 	 */
  146 | 	public static int getInitializeCount() {
  147 | 		return initializeCount;
  148 | 	}
  149 | 
  150 | 	/**
  151 | 	 * Associates a new <code>SecurityContext</code> with the current thread of execution.
  152 | 	 * @param context the new <code>SecurityContext</code> (may not be <code>null</code>)
  153 | 	 */
  154 | 	public static void setContext(SecurityContext context) {
  155 | 		strategy.setContext(context);
  156 | 	}
  157 | 
  158 | 	/**
  159 | 	 * Sets a {@link Supplier} that will return the current context. Implementations can
  160 | 	 * override the default to avoid invoking {@link Supplier#get()}.
  161 | 	 * @param deferredContext a {@link Supplier} that returns the {@link SecurityContext}
  162 | 	 * @since 5.8
  163 | 	 */
  164 | 	public static void setDeferredContext(Supplier<SecurityContext> deferredContext) {
  165 | 		strategy.setDeferredContext(deferredContext);
  166 | 	}
  167 | 
  168 | 	/**
  169 | 	 * Changes the preferred strategy. Do <em>NOT</em> call this method more than once for
  170 | 	 * a given JVM, as it will re-initialize the strategy and adversely affect any
  171 | 	 * existing threads using the old strategy.
  172 | 	 * @param strategyName the fully qualified class name of the strategy that should be
  173 | 	 * used.
  174 | 	 */
  175 | 	public static void setStrategyName(String strategyName) {
  176 | 		SecurityContextHolder.strategyName = strategyName;
  177 | 		initialize();
  178 | 	}
  179 | 
  180 | 	/**
  181 | 	 * Use this {@link SecurityContextHolderStrategy}.
  182 | 	 *
  183 | 	 * Call either {@link #setStrategyName(String)} or this method, but not both.
  184 | 	 *
  185 | 	 * This method is not thread safe. Changing the strategy while requests are in-flight
  186 | 	 * may cause race conditions.
  187 | 	 *
  188 | 	 * {@link SecurityContextHolder} maintains a static reference to the provided
  189 | 	 * {@link SecurityContextHolderStrategy}. This means that the strategy and its members
  190 | 	 * will not be garbage collected until you remove your strategy.
  191 | 	 *
  192 | 	 * To ensure garbage collection, remember the original strategy like so:
  193 | 	 *
  194 | 	 * <pre>
  195 | 	 *     SecurityContextHolderStrategy original = SecurityContextHolder.getContextHolderStrategy();
  196 | 	 *     SecurityContextHolder.setContextHolderStrategy(myStrategy);
  197 | 	 * </pre>
  198 | 	 *
  199 | 	 * And then when you are ready for {@code myStrategy} to be garbage collected you can
  200 | 	 * do:
  201 | 	 *
  202 | 	 * <pre>
  203 | 	 *     SecurityContextHolder.setContextHolderStrategy(original);
  204 | 	 * </pre>
  205 | 	 * @param strategy the {@link SecurityContextHolderStrategy} to use
  206 | 	 * @since 5.6
  207 | 	 */
  208 | 	public static void setContextHolderStrategy(SecurityContextHolderStrategy strategy) {
  209 | 		Assert.notNull(strategy, "securityContextHolderStrategy cannot be null");
  210 | 		SecurityContextHolder.strategyName = MODE_PRE_INITIALIZED;
  211 | 		SecurityContextHolder.strategy = strategy;
  212 | 		initialize();
  213 | 	}
  214 | 
  215 | 	/**
  216 | 	 * Allows retrieval of the context strategy. See SEC-1188.
  217 | 	 * @return the configured strategy for storing the security context.
  218 | 	 */
  219 | 	public static SecurityContextHolderStrategy getContextHolderStrategy() {
  220 | 		return strategy;
  221 | 	}
  222 | 
  223 | 	/**
  224 | 	 * Delegates the creation of a new, empty context to the configured strategy.
  225 | 	 */
  226 | 	public static SecurityContext createEmptyContext() {
  227 | 		return strategy.createEmptyContext();
  228 | 	}
  229 | 
  230 | 	@Override
  231 | 	public String toString() {
  232 | 		return "SecurityContextHolder[strategy='" + strategy.getClass().getSimpleName() + "'; initializeCount="
  233 | 				+ initializeCount + "]";
  234 | 	}
  235 | 
  236 | }
```
