<!-- learning-docs source archive（机械抓取原文，禁止手改代码内容） -->
<!-- origin: org.springframework:spring-aop:7.1.0 | path: org/springframework/aop/aspectj/MethodInvocationProceedingJoinPoint.java -->
<!-- 正文引用：[[源码:MethodInvocationProceedingJoinPoint.java#L起-L止]] ；代码块头部：(src: MethodInvocationProceedingJoinPoint.java#L起-L止) -->

# MethodInvocationProceedingJoinPoint.java

| 项 | 值 |
| --- | --- |
| 来源 | `org.springframework:spring-aop:7.1.0` |
| 路径 | `org/springframework/aop/aspectj/MethodInvocationProceedingJoinPoint.java` |
| 行数 | 325 |
| 在线地址 | https://repo1.maven.org/maven2/org/springframework/spring-aop/7.1.0/spring-aop-7.1.0-sources.jar |
| 抓取时间 | 2026-11-25T14:00:00.703Z |

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
   17 | package org.springframework.aop.aspectj;
   18 | 
   19 | import java.lang.reflect.Method;
   20 | import java.lang.reflect.Modifier;
   21 | 
   22 | import org.aspectj.lang.JoinPoint;
   23 | import org.aspectj.lang.ProceedingJoinPoint;
   24 | import org.aspectj.lang.Signature;
   25 | import org.aspectj.lang.reflect.MethodSignature;
   26 | import org.aspectj.lang.reflect.SourceLocation;
   27 | import org.aspectj.runtime.internal.AroundClosure;
   28 | import org.jspecify.annotations.Nullable;
   29 | 
   30 | import org.springframework.aop.ProxyMethodInvocation;
   31 | import org.springframework.core.DefaultParameterNameDiscoverer;
   32 | import org.springframework.util.Assert;
   33 | 
   34 | /**
   35 |  * An implementation of the AspectJ {@link ProceedingJoinPoint} interface
   36 |  * wrapping an AOP Alliance {@link org.aopalliance.intercept.MethodInvocation}.
   37 |  *
   38 |  * <p><b>Note</b>: The {@code getThis()} method returns the current Spring AOP proxy.
   39 |  * The {@code getTarget()} method returns the current Spring AOP target (which may be
   40 |  * {@code null} if there is no target instance) as a plain POJO without any advice.
   41 |  * <b>If you want to call the object and have the advice take effect, use {@code getThis()}.</b>
   42 |  * A common example is casting the object to an introduced interface in the implementation of
   43 |  * an introduction. There is no such distinction between target and proxy in AspectJ itself.
   44 |  *
   45 |  * @author Rod Johnson
   46 |  * @author Juergen Hoeller
   47 |  * @author Adrian Colyer
   48 |  * @author Ramnivas Laddad
   49 |  * @since 2.0
   50 |  */
   51 | public class MethodInvocationProceedingJoinPoint implements ProceedingJoinPoint, JoinPoint.StaticPart {
   52 | 
   53 | 	private final ProxyMethodInvocation methodInvocation;
   54 | 
   55 | 	private @Nullable Object @Nullable [] args;
   56 | 
   57 | 	/** Lazily initialized signature object. */
   58 | 	private @Nullable Signature signature;
   59 | 
   60 | 	/** Lazily initialized source location object. */
   61 | 	private @Nullable SourceLocation sourceLocation;
   62 | 
   63 | 
   64 | 	/**
   65 | 	 * Create a new MethodInvocationProceedingJoinPoint, wrapping the given
   66 | 	 * Spring ProxyMethodInvocation object.
   67 | 	 * @param methodInvocation the Spring ProxyMethodInvocation object
   68 | 	 */
   69 | 	public MethodInvocationProceedingJoinPoint(ProxyMethodInvocation methodInvocation) {
   70 | 		Assert.notNull(methodInvocation, "MethodInvocation must not be null");
   71 | 		this.methodInvocation = methodInvocation;
   72 | 	}
   73 | 
   74 | 
   75 | 	@Override
   76 | 	public void set$AroundClosure(AroundClosure aroundClosure) {
   77 | 		throw new UnsupportedOperationException();
   78 | 	}
   79 | 
   80 | 	@Override
   81 | 	public @Nullable Object proceed() throws Throwable {
   82 | 		return this.methodInvocation.invocableClone().proceed();
   83 | 	}
   84 | 
   85 | 	@Override
   86 | 	public @Nullable Object proceed(Object[] arguments) throws Throwable {
   87 | 		Assert.notNull(arguments, "Argument array passed to proceed cannot be null");
   88 | 		if (arguments.length != this.methodInvocation.getArguments().length) {
   89 | 			throw new IllegalArgumentException("Expecting " +
   90 | 					this.methodInvocation.getArguments().length + " arguments to proceed, " +
   91 | 					"but was passed " + arguments.length + " arguments");
   92 | 		}
   93 | 		this.methodInvocation.setArguments(arguments);
   94 | 		return this.methodInvocation.invocableClone(arguments).proceed();
   95 | 	}
   96 | 
   97 | 	/**
   98 | 	 * Returns the Spring AOP proxy. Cannot be {@code null}.
   99 | 	 */
  100 | 	@Override
  101 | 	public Object getThis() {
  102 | 		return this.methodInvocation.getProxy();
  103 | 	}
  104 | 
  105 | 	/**
  106 | 	 * Returns the Spring AOP target. May be {@code null} if there is no target.
  107 | 	 */
  108 | 	@Override
  109 | 	public @Nullable Object getTarget() {
  110 | 		return this.methodInvocation.getThis();
  111 | 	}
  112 | 
  113 | 	@Override
  114 | 	@SuppressWarnings("NullAway") // Overridden method does not define nullness
  115 | 	public @Nullable Object[] getArgs() {
  116 | 		if (this.args == null) {
  117 | 			this.args = this.methodInvocation.getArguments().clone();
  118 | 		}
  119 | 		return this.args;
  120 | 	}
  121 | 
  122 | 	@Override
  123 | 	public Signature getSignature() {
  124 | 		if (this.signature == null) {
  125 | 			this.signature = new MethodSignatureImpl();
  126 | 		}
  127 | 		return this.signature;
  128 | 	}
  129 | 
  130 | 	@Override
  131 | 	public SourceLocation getSourceLocation() {
  132 | 		if (this.sourceLocation == null) {
  133 | 			this.sourceLocation = new SourceLocationImpl();
  134 | 		}
  135 | 		return this.sourceLocation;
  136 | 	}
  137 | 
  138 | 	@Override
  139 | 	public String getKind() {
  140 | 		return ProceedingJoinPoint.METHOD_EXECUTION;
  141 | 	}
  142 | 
  143 | 	@Override
  144 | 	public int getId() {
  145 | 		// TODO: It's just an adapter but returning 0 might still have side effects...
  146 | 		return 0;
  147 | 	}
  148 | 
  149 | 	@Override
  150 | 	public JoinPoint.StaticPart getStaticPart() {
  151 | 		return this;
  152 | 	}
  153 | 
  154 | 	@Override
  155 | 	public String toShortString() {
  156 | 		return "execution(" + getSignature().toShortString() + ")";
  157 | 	}
  158 | 
  159 | 	@Override
  160 | 	public String toLongString() {
  161 | 		return "execution(" + getSignature().toLongString() + ")";
  162 | 	}
  163 | 
  164 | 	@Override
  165 | 	public String toString() {
  166 | 		return "execution(" + getSignature() + ")";
  167 | 	}
  168 | 
  169 | 
  170 | 	/**
  171 | 	 * Lazily initialized MethodSignature.
  172 | 	 */
  173 | 	private class MethodSignatureImpl implements MethodSignature {
  174 | 
  175 | 		private volatile @Nullable String @Nullable [] parameterNames;
  176 | 
  177 | 		@Override
  178 | 		public String getName() {
  179 | 			return methodInvocation.getMethod().getName();
  180 | 		}
  181 | 
  182 | 		@Override
  183 | 		public int getModifiers() {
  184 | 			return methodInvocation.getMethod().getModifiers();
  185 | 		}
  186 | 
  187 | 		@Override
  188 | 		public Class<?> getDeclaringType() {
  189 | 			return methodInvocation.getMethod().getDeclaringClass();
  190 | 		}
  191 | 
  192 | 		@Override
  193 | 		public String getDeclaringTypeName() {
  194 | 			return methodInvocation.getMethod().getDeclaringClass().getName();
  195 | 		}
  196 | 
  197 | 		@Override
  198 | 		public Class<?> getReturnType() {
  199 | 			return methodInvocation.getMethod().getReturnType();
  200 | 		}
  201 | 
  202 | 		@Override
  203 | 		public Method getMethod() {
  204 | 			return methodInvocation.getMethod();
  205 | 		}
  206 | 
  207 | 		@Override
  208 | 		public Class<?>[] getParameterTypes() {
  209 | 			return methodInvocation.getMethod().getParameterTypes();
  210 | 		}
  211 | 
  212 | 		@Override
  213 | 		@SuppressWarnings("NullAway") // Overridden method does not define nullness
  214 | 		public @Nullable String @Nullable [] getParameterNames() {
  215 | 			@Nullable String[] parameterNames = this.parameterNames;
  216 | 			if (parameterNames == null) {
  217 | 				parameterNames = DefaultParameterNameDiscoverer.getSharedInstance().getParameterNames(getMethod());
  218 | 				this.parameterNames = parameterNames;
  219 | 			}
  220 | 			return parameterNames;
  221 | 		}
  222 | 
  223 | 		@Override
  224 | 		public Class<?>[] getExceptionTypes() {
  225 | 			return methodInvocation.getMethod().getExceptionTypes();
  226 | 		}
  227 | 
  228 | 		@Override
  229 | 		public String toShortString() {
  230 | 			return toString(false, false, false, false);
  231 | 		}
  232 | 
  233 | 		@Override
  234 | 		public String toLongString() {
  235 | 			return toString(true, true, true, true);
  236 | 		}
  237 | 
  238 | 		@Override
  239 | 		public String toString() {
  240 | 			return toString(false, true, false, true);
  241 | 		}
  242 | 
  243 | 		private String toString(boolean includeModifier, boolean includeReturnTypeAndArgs,
  244 | 				boolean useLongReturnAndArgumentTypeName, boolean useLongTypeName) {
  245 | 
  246 | 			StringBuilder sb = new StringBuilder();
  247 | 			if (includeModifier) {
  248 | 				sb.append(Modifier.toString(getModifiers()));
  249 | 				sb.append(' ');
  250 | 			}
  251 | 			if (includeReturnTypeAndArgs) {
  252 | 				appendType(sb, getReturnType(), useLongReturnAndArgumentTypeName);
  253 | 				sb.append(' ');
  254 | 			}
  255 | 			appendType(sb, getDeclaringType(), useLongTypeName);
  256 | 			sb.append('.');
  257 | 			sb.append(getMethod().getName());
  258 | 			sb.append('(');
  259 | 			Class<?>[] parametersTypes = getParameterTypes();
  260 | 			appendTypes(sb, parametersTypes, includeReturnTypeAndArgs, useLongReturnAndArgumentTypeName);
  261 | 			sb.append(')');
  262 | 			return sb.toString();
  263 | 		}
  264 | 
  265 | 		private void appendTypes(StringBuilder sb, Class<?>[] types, boolean includeArgs,
  266 | 				boolean useLongReturnAndArgumentTypeName) {
  267 | 
  268 | 			if (includeArgs) {
  269 | 				for (int size = types.length, i = 0; i < size; i++) {
  270 | 					appendType(sb, types[i], useLongReturnAndArgumentTypeName);
  271 | 					if (i < size - 1) {
  272 | 						sb.append(',');
  273 | 					}
  274 | 				}
  275 | 			}
  276 | 			else {
  277 | 				if (types.length != 0) {
  278 | 					sb.append("..");
  279 | 				}
  280 | 			}
  281 | 		}
  282 | 
  283 | 		private void appendType(StringBuilder sb, Class<?> type, boolean useLongTypeName) {
  284 | 			if (type.isArray()) {
  285 | 				appendType(sb, type.componentType(), useLongTypeName);
  286 | 				sb.append("[]");
  287 | 			}
  288 | 			else {
  289 | 				sb.append(useLongTypeName ? type.getName() : type.getSimpleName());
  290 | 			}
  291 | 		}
  292 | 	}
  293 | 
  294 | 
  295 | 	/**
  296 | 	 * Lazily initialized SourceLocation.
  297 | 	 */
  298 | 	private class SourceLocationImpl implements SourceLocation {
  299 | 
  300 | 		@Override
  301 | 		public Class<?> getWithinType() {
  302 | 			if (methodInvocation.getThis() == null) {
  303 | 				throw new UnsupportedOperationException("No source location joinpoint available: target is null");
  304 | 			}
  305 | 			return methodInvocation.getThis().getClass();
  306 | 		}
  307 | 
  308 | 		@Override
  309 | 		public String getFileName() {
  310 | 			throw new UnsupportedOperationException();
  311 | 		}
  312 | 
  313 | 		@Override
  314 | 		public int getLine() {
  315 | 			throw new UnsupportedOperationException();
  316 | 		}
  317 | 
  318 | 		@Override
  319 | 		@Deprecated(since = "4.0") // deprecated by AspectJ
  320 | 		public int getColumn() {
  321 | 			throw new UnsupportedOperationException();
  322 | 		}
  323 | 	}
  324 | 
  325 | }
```
