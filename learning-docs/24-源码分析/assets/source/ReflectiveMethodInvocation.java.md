<!-- learning-docs source archive（机械抓取原文，禁止手改代码内容） -->
<!-- origin: org.springframework:spring-aop:7.1.0 | path: org/springframework/aop/framework/ReflectiveMethodInvocation.java -->
<!-- 正文引用：[[源码:ReflectiveMethodInvocation.java#L起-L止]] ；代码块头部：(src: ReflectiveMethodInvocation.java#L起-L止) -->

# ReflectiveMethodInvocation.java

| 项 | 值 |
| --- | --- |
| 来源 | `org.springframework:spring-aop:7.1.0` |
| 路径 | `org/springframework/aop/framework/ReflectiveMethodInvocation.java` |
| 行数 | 290 |
| 在线地址 | https://repo1.maven.org/maven2/org/springframework/spring-aop/7.1.0/spring-aop-7.1.0-sources.jar |
| 抓取时间 | 2026-11-25T13:59:57.834Z |

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
   17 | package org.springframework.aop.framework;
   18 | 
   19 | import java.lang.reflect.AccessibleObject;
   20 | import java.lang.reflect.Method;
   21 | import java.util.HashMap;
   22 | import java.util.List;
   23 | import java.util.Map;
   24 | 
   25 | import org.aopalliance.intercept.MethodInterceptor;
   26 | import org.aopalliance.intercept.MethodInvocation;
   27 | import org.jspecify.annotations.Nullable;
   28 | 
   29 | import org.springframework.aop.ProxyMethodInvocation;
   30 | import org.springframework.aop.support.AopUtils;
   31 | import org.springframework.core.BridgeMethodResolver;
   32 | 
   33 | /**
   34 |  * Spring's implementation of the AOP Alliance
   35 |  * {@link org.aopalliance.intercept.MethodInvocation} interface,
   36 |  * implementing the extended
   37 |  * {@link org.springframework.aop.ProxyMethodInvocation} interface.
   38 |  *
   39 |  * <p>Invokes the target object using reflection. Subclasses can override the
   40 |  * {@link #invokeJoinpoint()} method to change this behavior, so this is also
   41 |  * a useful base class for more specialized MethodInvocation implementations.
   42 |  *
   43 |  * <p>It is possible to clone an invocation, to invoke {@link #proceed()}
   44 |  * repeatedly (once per clone), using the {@link #invocableClone()} method.
   45 |  * It is also possible to attach custom attributes to the invocation,
   46 |  * using the {@link #setUserAttribute} / {@link #getUserAttribute} methods.
   47 |  *
   48 |  * <p><b>NOTE:</b> This class is considered internal and should not be
   49 |  * directly accessed. The sole reason for it being public is compatibility
   50 |  * with existing framework integrations (for example, Pitchfork). For any other
   51 |  * purposes, use the {@link ProxyMethodInvocation} interface instead.
   52 |  *
   53 |  * @author Rod Johnson
   54 |  * @author Juergen Hoeller
   55 |  * @author Adrian Colyer
   56 |  * @see #invokeJoinpoint
   57 |  * @see #proceed
   58 |  * @see #invocableClone
   59 |  * @see #setUserAttribute
   60 |  * @see #getUserAttribute
   61 |  */
   62 | public class ReflectiveMethodInvocation implements ProxyMethodInvocation, Cloneable {
   63 | 
   64 | 	protected final Object proxy;
   65 | 
   66 | 	protected final @Nullable Object target;
   67 | 
   68 | 	protected final Method method;
   69 | 
   70 | 	protected @Nullable Object[] arguments;
   71 | 
   72 | 	private final @Nullable Class<?> targetClass;
   73 | 
   74 | 	/**
   75 | 	 * Lazily initialized map of user-specific attributes for this invocation.
   76 | 	 */
   77 | 	private @Nullable Map<String, Object> userAttributes;
   78 | 
   79 | 	/**
   80 | 	 * List of MethodInterceptor and InterceptorAndDynamicMethodMatcher
   81 | 	 * that need dynamic checks.
   82 | 	 */
   83 | 	protected final List<?> interceptorsAndDynamicMethodMatchers;
   84 | 
   85 | 	/**
   86 | 	 * Index from 0 of the current interceptor we're invoking.
   87 | 	 * -1 until we invoke: then the current interceptor.
   88 | 	 */
   89 | 	private int currentInterceptorIndex = -1;
   90 | 
   91 | 
   92 | 	/**
   93 | 	 * Construct a new ReflectiveMethodInvocation with the given arguments.
   94 | 	 * @param proxy the proxy object that the invocation was made on
   95 | 	 * @param target the target object to invoke
   96 | 	 * @param method the method to invoke
   97 | 	 * @param arguments the arguments to invoke the method with
   98 | 	 * @param targetClass the target class, for MethodMatcher invocations
   99 | 	 * @param interceptorsAndDynamicMethodMatchers interceptors that should be applied,
  100 | 	 * along with any InterceptorAndDynamicMethodMatchers that need evaluation at runtime.
  101 | 	 * MethodMatchers included in this struct must already have been found to have matched
  102 | 	 * as far as was possibly statically. Passing an array might be about 10% faster,
  103 | 	 * but would complicate the code. And it would work only for static pointcuts.
  104 | 	 */
  105 | 	protected ReflectiveMethodInvocation(
  106 | 			Object proxy, @Nullable Object target, Method method, @Nullable Object[] arguments,
  107 | 			@Nullable Class<?> targetClass, List<Object> interceptorsAndDynamicMethodMatchers) {
  108 | 
  109 | 		this.proxy = proxy;
  110 | 		this.target = target;
  111 | 		this.targetClass = targetClass;
  112 | 		this.method = BridgeMethodResolver.findBridgedMethod(method);
  113 | 		this.arguments = AopProxyUtils.adaptArgumentsIfNecessary(method, arguments);
  114 | 		this.interceptorsAndDynamicMethodMatchers = interceptorsAndDynamicMethodMatchers;
  115 | 	}
  116 | 
  117 | 
  118 | 	@Override
  119 | 	public final Object getProxy() {
  120 | 		return this.proxy;
  121 | 	}
  122 | 
  123 | 	@Override
  124 | 	public final @Nullable Object getThis() {
  125 | 		return this.target;
  126 | 	}
  127 | 
  128 | 	@Override
  129 | 	public final AccessibleObject getStaticPart() {
  130 | 		return this.method;
  131 | 	}
  132 | 
  133 | 	/**
  134 | 	 * Return the method invoked on the proxied interface.
  135 | 	 * May or may not correspond with a method invoked on an underlying
  136 | 	 * implementation of that interface.
  137 | 	 */
  138 | 	@Override
  139 | 	public final Method getMethod() {
  140 | 		return this.method;
  141 | 	}
  142 | 
  143 | 	@Override
  144 | 	public final @Nullable Object[] getArguments() {
  145 | 		return this.arguments;
  146 | 	}
  147 | 
  148 | 	@Override
  149 | 	public void setArguments(@Nullable Object... arguments) {
  150 | 		this.arguments = arguments;
  151 | 	}
  152 | 
  153 | 
  154 | 	@Override
  155 | 	public @Nullable Object proceed() throws Throwable {
  156 | 		// We start with an index of -1 and increment early.
  157 | 		if (this.currentInterceptorIndex == this.interceptorsAndDynamicMethodMatchers.size() - 1) {
  158 | 			return invokeJoinpoint();
  159 | 		}
  160 | 
  161 | 		Object interceptorOrInterceptionAdvice =
  162 | 				this.interceptorsAndDynamicMethodMatchers.get(++this.currentInterceptorIndex);
  163 | 		if (interceptorOrInterceptionAdvice instanceof InterceptorAndDynamicMethodMatcher dm) {
  164 | 			// Evaluate dynamic method matcher here: static part will already have
  165 | 			// been evaluated and found to match.
  166 | 			Class<?> targetClass = (this.targetClass != null ? this.targetClass : this.method.getDeclaringClass());
  167 | 			if (dm.matcher().matches(this.method, targetClass, this.arguments)) {
  168 | 				return dm.interceptor().invoke(this);
  169 | 			}
  170 | 			else {
  171 | 				// Dynamic matching failed.
  172 | 				// Skip this interceptor and invoke the next in the chain.
  173 | 				return proceed();
  174 | 			}
  175 | 		}
  176 | 		else {
  177 | 			// It's an interceptor, so we just invoke it: The pointcut will have
  178 | 			// been evaluated statically before this object was constructed.
  179 | 			return ((MethodInterceptor) interceptorOrInterceptionAdvice).invoke(this);
  180 | 		}
  181 | 	}
  182 | 
  183 | 	/**
  184 | 	 * Invoke the joinpoint using reflection.
  185 | 	 * Subclasses can override this to use custom invocation.
  186 | 	 * @return the return value of the joinpoint
  187 | 	 * @throws Throwable if invoking the joinpoint resulted in an exception
  188 | 	 */
  189 | 	protected @Nullable Object invokeJoinpoint() throws Throwable {
  190 | 		return AopUtils.invokeJoinpointUsingReflection(this.target, this.method, this.arguments);
  191 | 	}
  192 | 
  193 | 
  194 | 	/**
  195 | 	 * This implementation returns a shallow copy of this invocation object,
  196 | 	 * including an independent copy of the original arguments array.
  197 | 	 * <p>We want a shallow copy in this case: We want to use the same interceptor
  198 | 	 * chain and other object references, but we want an independent value for the
  199 | 	 * current interceptor index.
  200 | 	 * @see java.lang.Object#clone()
  201 | 	 */
  202 | 	@Override
  203 | 	public MethodInvocation invocableClone() {
  204 | 		@Nullable Object[] cloneArguments = this.arguments;
  205 | 		if (this.arguments.length > 0) {
  206 | 			// Build an independent copy of the arguments array.
  207 | 			cloneArguments = this.arguments.clone();
  208 | 		}
  209 | 		return invocableClone(cloneArguments);
  210 | 	}
  211 | 
  212 | 	/**
  213 | 	 * This implementation returns a shallow copy of this invocation object,
  214 | 	 * using the given arguments array for the clone.
  215 | 	 * <p>We want a shallow copy in this case: We want to use the same interceptor
  216 | 	 * chain and other object references, but we want an independent value for the
  217 | 	 * current interceptor index.
  218 | 	 * @see java.lang.Object#clone()
  219 | 	 */
  220 | 	@Override
  221 | 	public MethodInvocation invocableClone(@Nullable Object... arguments) {
  222 | 		// Force initialization of the user attributes Map,
  223 | 		// for having a shared Map reference in the clone.
  224 | 		if (this.userAttributes == null) {
  225 | 			this.userAttributes = new HashMap<>();
  226 | 		}
  227 | 
  228 | 		// Create the MethodInvocation clone.
  229 | 		try {
  230 | 			ReflectiveMethodInvocation clone = (ReflectiveMethodInvocation) clone();
  231 | 			clone.arguments = arguments;
  232 | 			return clone;
  233 | 		}
  234 | 		catch (CloneNotSupportedException ex) {
  235 | 			throw new IllegalStateException(
  236 | 					"Should be able to clone object of type [" + getClass() + "]: " + ex);
  237 | 		}
  238 | 	}
  239 | 
  240 | 
  241 | 	@Override
  242 | 	public void setUserAttribute(String key, @Nullable Object value) {
  243 | 		if (value != null) {
  244 | 			if (this.userAttributes == null) {
  245 | 				this.userAttributes = new HashMap<>();
  246 | 			}
  247 | 			this.userAttributes.put(key, value);
  248 | 		}
  249 | 		else {
  250 | 			if (this.userAttributes != null) {
  251 | 				this.userAttributes.remove(key);
  252 | 			}
  253 | 		}
  254 | 	}
  255 | 
  256 | 	@Override
  257 | 	public @Nullable Object getUserAttribute(String key) {
  258 | 		return (this.userAttributes != null ? this.userAttributes.get(key) : null);
  259 | 	}
  260 | 
  261 | 	/**
  262 | 	 * Return user attributes associated with this invocation.
  263 | 	 * This method provides an invocation-bound alternative to a ThreadLocal.
  264 | 	 * <p>This map is initialized lazily and is not used in the AOP framework itself.
  265 | 	 * @return any user attributes associated with this invocation
  266 | 	 * (never {@code null})
  267 | 	 */
  268 | 	public Map<String, Object> getUserAttributes() {
  269 | 		if (this.userAttributes == null) {
  270 | 			this.userAttributes = new HashMap<>();
  271 | 		}
  272 | 		return this.userAttributes;
  273 | 	}
  274 | 
  275 | 
  276 | 	@Override
  277 | 	public String toString() {
  278 | 		// Don't do toString on target, it may be proxied.
  279 | 		StringBuilder sb = new StringBuilder("ReflectiveMethodInvocation: ");
  280 | 		sb.append(this.method).append("; ");
  281 | 		if (this.target == null) {
  282 | 			sb.append("target is null");
  283 | 		}
  284 | 		else {
  285 | 			sb.append("target is of class [").append(this.target.getClass().getName()).append(']');
  286 | 		}
  287 | 		return sb.toString();
  288 | 	}
  289 | 
  290 | }
```
