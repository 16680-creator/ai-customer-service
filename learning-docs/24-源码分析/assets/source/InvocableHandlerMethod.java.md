<!-- learning-docs source archive（机械抓取原文，禁止手改代码内容） -->
<!-- origin: org.springframework:spring-web:7.1.0 | path: org/springframework/web/method/support/InvocableHandlerMethod.java -->
<!-- 正文引用：[[源码:InvocableHandlerMethod.java#L起-L止]] ；代码块头部：(src: InvocableHandlerMethod.java#L起-L止) -->

# InvocableHandlerMethod.java

| 项 | 值 |
| --- | --- |
| 来源 | `org.springframework:spring-web:7.1.0` |
| 路径 | `org/springframework/web/method/support/InvocableHandlerMethod.java` |
| 行数 | 381 |
| 在线地址 | https://repo1.maven.org/maven2/org/springframework/spring-web/7.1.0/spring-web-7.1.0-sources.jar |
| 抓取时间 | 2026-11-25T14:18:38.426Z |

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
   17 | package org.springframework.web.method.support;
   18 | 
   19 | import java.lang.reflect.InvocationTargetException;
   20 | import java.lang.reflect.Method;
   21 | import java.util.Arrays;
   22 | import java.util.Map;
   23 | import java.util.Objects;
   24 | 
   25 | import kotlin.Unit;
   26 | import kotlin.jvm.JvmClassMappingKt;
   27 | import kotlin.reflect.KClass;
   28 | import kotlin.reflect.KFunction;
   29 | import kotlin.reflect.KParameter;
   30 | import kotlin.reflect.KType;
   31 | import kotlin.reflect.full.KClasses;
   32 | import kotlin.reflect.jvm.KCallablesJvm;
   33 | import kotlin.reflect.jvm.ReflectJvmMapping;
   34 | import org.jspecify.annotations.Nullable;
   35 | import reactor.core.publisher.Mono;
   36 | import reactor.core.publisher.SynchronousSink;
   37 | 
   38 | import org.springframework.context.MessageSource;
   39 | import org.springframework.core.CoroutinesUtils;
   40 | import org.springframework.core.DefaultParameterNameDiscoverer;
   41 | import org.springframework.core.KotlinDetector;
   42 | import org.springframework.core.MethodParameter;
   43 | import org.springframework.core.ParameterNameDiscoverer;
   44 | import org.springframework.util.CollectionUtils;
   45 | import org.springframework.util.ObjectUtils;
   46 | import org.springframework.validation.method.MethodValidator;
   47 | import org.springframework.web.bind.WebDataBinder;
   48 | import org.springframework.web.bind.support.SessionStatus;
   49 | import org.springframework.web.bind.support.WebDataBinderFactory;
   50 | import org.springframework.web.context.request.NativeWebRequest;
   51 | import org.springframework.web.method.HandlerMethod;
   52 | 
   53 | /**
   54 |  * Extension of {@link HandlerMethod} that invokes the underlying method with
   55 |  * argument values resolved from the current HTTP request through a list of
   56 |  * {@link HandlerMethodArgumentResolver}.
   57 |  *
   58 |  * @author Rossen Stoyanchev
   59 |  * @author Juergen Hoeller
   60 |  * @author Sebastien Deleuze
   61 |  * @since 3.1
   62 |  */
   63 | public class InvocableHandlerMethod extends HandlerMethod {
   64 | 
   65 | 	private static final Object[] EMPTY_ARGS = new Object[0];
   66 | 
   67 | 	private static final boolean KOTLIN_REFLECT_PRESENT = KotlinDetector.isKotlinReflectPresent();
   68 | 
   69 | 
   70 | 	private HandlerMethodArgumentResolverComposite resolvers = new HandlerMethodArgumentResolverComposite();
   71 | 
   72 | 	private ParameterNameDiscoverer parameterNameDiscoverer = DefaultParameterNameDiscoverer.getSharedInstance();
   73 | 
   74 | 	private @Nullable WebDataBinderFactory dataBinderFactory;
   75 | 
   76 | 	private @Nullable MethodValidator methodValidator;
   77 | 
   78 | 
   79 | 	/**
   80 | 	 * Create an instance from a {@code HandlerMethod}.
   81 | 	 */
   82 | 	public InvocableHandlerMethod(HandlerMethod handlerMethod) {
   83 | 		super(handlerMethod);
   84 | 	}
   85 | 
   86 | 	/**
   87 | 	 * Create an instance from a bean instance and a method.
   88 | 	 */
   89 | 	public InvocableHandlerMethod(Object bean, Method method) {
   90 | 		super(bean, method);
   91 | 	}
   92 | 
   93 | 	/**
   94 | 	 * Variant of {@link #InvocableHandlerMethod(Object, Method)} that
   95 | 	 * also accepts a {@link MessageSource}, for use in subclasses.
   96 | 	 * @since 5.3.10
   97 | 	 */
   98 | 	protected InvocableHandlerMethod(Object bean, Method method, @Nullable MessageSource messageSource) {
   99 | 		super(bean, method, messageSource);
  100 | 	}
  101 | 
  102 | 	/**
  103 | 	 * Construct a new handler method with the given bean instance, method name and parameters.
  104 | 	 * @param bean the object bean
  105 | 	 * @param methodName the method name
  106 | 	 * @param parameterTypes the method parameter types
  107 | 	 * @throws NoSuchMethodException when the method cannot be found
  108 | 	 */
  109 | 	public InvocableHandlerMethod(Object bean, String methodName, Class<?>... parameterTypes)
  110 | 			throws NoSuchMethodException {
  111 | 
  112 | 		super(bean, methodName, parameterTypes);
  113 | 	}
  114 | 
  115 | 
  116 | 	/**
  117 | 	 * Set {@link HandlerMethodArgumentResolver HandlerMethodArgumentResolvers}
  118 | 	 * to use for resolving method argument values.
  119 | 	 */
  120 | 	public void setHandlerMethodArgumentResolvers(HandlerMethodArgumentResolverComposite argumentResolvers) {
  121 | 		this.resolvers = argumentResolvers;
  122 | 	}
  123 | 
  124 | 	/**
  125 | 	 * Set the ParameterNameDiscoverer for resolving parameter names when needed
  126 | 	 * (for example, default request attribute name).
  127 | 	 * <p>Default is a {@link org.springframework.core.DefaultParameterNameDiscoverer}.
  128 | 	 */
  129 | 	public void setParameterNameDiscoverer(ParameterNameDiscoverer parameterNameDiscoverer) {
  130 | 		this.parameterNameDiscoverer = parameterNameDiscoverer;
  131 | 	}
  132 | 
  133 | 	/**
  134 | 	 * Set the {@link WebDataBinderFactory} to be passed to argument resolvers allowing them
  135 | 	 * to create a {@link WebDataBinder} for data binding and type conversion purposes.
  136 | 	 */
  137 | 	public void setDataBinderFactory(WebDataBinderFactory dataBinderFactory) {
  138 | 		this.dataBinderFactory = dataBinderFactory;
  139 | 	}
  140 | 
  141 | 	/**
  142 | 	 * Set the {@link MethodValidator} to perform method validation with if the
  143 | 	 * controller method {@link #shouldValidateArguments()} or
  144 | 	 * {@link #shouldValidateReturnValue()}.
  145 | 	 * @since 6.1
  146 | 	 */
  147 | 	public void setMethodValidator(@Nullable MethodValidator methodValidator) {
  148 | 		this.methodValidator = methodValidator;
  149 | 	}
  150 | 
  151 | 
  152 | 	/**
  153 | 	 * Invoke the method after resolving its argument values in the context of the given request.
  154 | 	 * <p>Argument values are commonly resolved through
  155 | 	 * {@link HandlerMethodArgumentResolver HandlerMethodArgumentResolvers}.
  156 | 	 * The {@code providedArgs} parameter however may supply argument values to be used directly,
  157 | 	 * i.e. without argument resolution. Examples of provided argument values include a
  158 | 	 * {@link WebDataBinder}, a {@link SessionStatus}, or a thrown exception instance.
  159 | 	 * Provided argument values are checked before argument resolvers.
  160 | 	 * <p>Delegates to {@link #getMethodArgumentValues} and calls {@link #doInvoke} with the
  161 | 	 * resolved arguments.
  162 | 	 * @param request the current request
  163 | 	 * @param mavContainer the ModelAndViewContainer for this request
  164 | 	 * @param providedArgs "given" arguments matched by type, not resolved
  165 | 	 * @return the raw value returned by the invoked method
  166 | 	 * @throws Exception raised if no suitable argument resolver can be found,
  167 | 	 * or if the method raised an exception
  168 | 	 * @see #getMethodArgumentValues
  169 | 	 * @see #doInvoke
  170 | 	 */
  171 | 	public @Nullable Object invokeForRequest(NativeWebRequest request, @Nullable ModelAndViewContainer mavContainer,
  172 | 			@Nullable Object... providedArgs) throws Exception {
  173 | 
  174 | 		@Nullable Object[] args = getMethodArgumentValues(request, mavContainer, providedArgs);
  175 | 		if (logger.isTraceEnabled()) {
  176 | 			logger.trace("Arguments: " + Arrays.toString(args));
  177 | 		}
  178 | 
  179 | 		if (shouldValidateArguments() && this.methodValidator != null) {
  180 | 			this.methodValidator.applyArgumentValidation(
  181 | 					getBean(), getBridgedMethod(), getMethodParameters(), args, getValidationGroups());
  182 | 		}
  183 | 
  184 | 		Object returnValue = doInvoke(args);
  185 | 
  186 | 		if (shouldValidateReturnValue() && this.methodValidator != null) {
  187 | 			this.methodValidator.applyReturnValueValidation(
  188 | 					getBean(), getBridgedMethod(), getReturnType(), returnValue, getValidationGroups());
  189 | 		}
  190 | 
  191 | 		return returnValue;
  192 | 	}
  193 | 
  194 | 	/**
  195 | 	 * Get the method argument values for the current request, checking the provided
  196 | 	 * argument values and falling back to the configured argument resolvers.
  197 | 	 * <p>The resulting array will be passed into {@link #doInvoke}.
  198 | 	 * @since 5.1.2
  199 | 	 */
  200 | 	protected @Nullable Object[] getMethodArgumentValues(NativeWebRequest request, @Nullable ModelAndViewContainer mavContainer,
  201 | 			@Nullable Object... providedArgs) throws Exception {
  202 | 
  203 | 		MethodParameter[] parameters = getMethodParameters();
  204 | 		if (ObjectUtils.isEmpty(parameters)) {
  205 | 			return EMPTY_ARGS;
  206 | 		}
  207 | 
  208 | 		@Nullable Object[] args = new Object[parameters.length];
  209 | 		for (int i = 0; i < parameters.length; i++) {
  210 | 			MethodParameter parameter = parameters[i];
  211 | 			parameter.initParameterNameDiscovery(this.parameterNameDiscoverer);
  212 | 			args[i] = findProvidedArgument(parameter, providedArgs);
  213 | 			if (args[i] != null) {
  214 | 				continue;
  215 | 			}
  216 | 			if (parameter.getParameterType().equals(HandlerMethod.class) && parameter.isOptional()) {
  217 | 				args[i] = null;
  218 | 				continue;
  219 | 			}
  220 | 			if (!this.resolvers.supportsParameter(parameter)) {
  221 | 				throw new IllegalStateException(formatArgumentError(parameter, "No suitable resolver"));
  222 | 			}
  223 | 			try {
  224 | 				args[i] = this.resolvers.resolveArgument(parameter, mavContainer, request, this.dataBinderFactory);
  225 | 			}
  226 | 			catch (Exception ex) {
  227 | 				// Leave stack trace for later, exception may actually be resolved and handled...
  228 | 				if (logger.isDebugEnabled()) {
  229 | 					String exMsg = ex.getMessage();
  230 | 					if (exMsg != null && !exMsg.contains(parameter.getExecutable().toGenericString())) {
  231 | 						logger.debug(formatArgumentError(parameter, exMsg));
  232 | 					}
  233 | 				}
  234 | 				throw ex;
  235 | 			}
  236 | 		}
  237 | 		return args;
  238 | 	}
  239 | 
  240 | 	/**
  241 | 	 * Invoke the handler method with the given argument values.
  242 | 	 */
  243 | 	protected @Nullable Object doInvoke(@Nullable Object... args) throws Exception {
  244 | 		Method method = getBridgedMethod();
  245 | 		try {
  246 | 			if (KOTLIN_REFLECT_PRESENT && KotlinDetector.isKotlinType(method.getDeclaringClass())) {
  247 | 				if (KotlinDetector.isSuspendingFunction(method)) {
  248 | 					return invokeSuspendingFunction(method, getBean(), args);
  249 | 				}
  250 | 				return KotlinDelegate.invokeFunction(method, getBean(), args);
  251 | 			}
  252 | 			return method.invoke(getBean(), args);
  253 | 		}
  254 | 		catch (IllegalArgumentException ex) {
  255 | 			assertTargetBean(method, getBean(), args);
  256 | 			String text = (ex.getMessage() == null || ex.getCause() instanceof NullPointerException) ?
  257 | 					"Illegal argument" : ex.getMessage();
  258 | 			throw new IllegalStateException(formatInvokeError(text, args), ex);
  259 | 		}
  260 | 		catch (InvocationTargetException ex) {
  261 | 			// Unwrap for HandlerExceptionResolvers ...
  262 | 			Throwable targetException = ex.getCause();
  263 | 			if (targetException instanceof RuntimeException runtimeException) {
  264 | 				throw runtimeException;
  265 | 			}
  266 | 			else if (targetException instanceof Error error) {
  267 | 				throw error;
  268 | 			}
  269 | 			else if (targetException instanceof Exception exception) {
  270 | 				throw exception;
  271 | 			}
  272 | 			else {
  273 | 				throw new IllegalStateException(formatInvokeError("Invocation failure", args), targetException);
  274 | 			}
  275 | 		}
  276 | 	}
  277 | 
  278 | 	/**
  279 | 	 * Invoke the given Kotlin coroutine suspended function.
  280 | 	 * <p>The default implementation invokes
  281 | 	 * {@link CoroutinesUtils#invokeSuspendingFunction(Method, Object, Object...)},
  282 | 	 * but subclasses can override this method to use
  283 | 	 * {@link CoroutinesUtils#invokeSuspendingFunction(kotlin.coroutines.CoroutineContext, Method, Object, Object...)}
  284 | 	 * instead.
  285 | 	 * @since 6.0
  286 | 	 */
  287 | 	protected Object invokeSuspendingFunction(Method method, Object target, @Nullable Object[] args) {
  288 | 		Object result = CoroutinesUtils.invokeSuspendingFunction(method, target, args);
  289 | 		return (result instanceof Mono<?> mono ? mono.handle(KotlinDelegate::handleResult) : result);
  290 | 	}
  291 | 
  292 | 
  293 | 	/**
  294 | 	 * Inner class to avoid a hard dependency on Kotlin at runtime.
  295 | 	 */
  296 | 	private static class KotlinDelegate {
  297 | 
  298 | 		@SuppressWarnings("DataFlowIssue")
  299 | 		public static @Nullable Object invokeFunction(Method method, Object target, @Nullable Object[] args) throws
  300 | 				InvocationTargetException, IllegalAccessException, NoSuchMethodException {
  301 | 
  302 | 			KFunction<?> function = ReflectJvmMapping.getKotlinFunction(method);
  303 | 			// For property accessors
  304 | 			if (function == null) {
  305 | 				return method.invoke(target, args);
  306 | 			}
  307 | 			if (!KCallablesJvm.isAccessible(function)) {
  308 | 				KCallablesJvm.setAccessible(function, true);
  309 | 			}
  310 | 			Map<KParameter, Object> argMap = CollectionUtils.newHashMap(args.length + 1);
  311 | 			int index = 0;
  312 | 			for (KParameter parameter : function.getParameters()) {
  313 | 				switch (parameter.getKind()) {
  314 | 					case INSTANCE -> argMap.put(parameter, target);
  315 | 					case VALUE, EXTENSION_RECEIVER -> {
  316 | 						Object arg = args[index];
  317 | 						if (!(parameter.isOptional() && arg == null)) {
  318 | 							KType type = parameter.getType();
  319 | 							if (!(type.isMarkedNullable() && arg == null) &&
  320 | 									type.getClassifier() instanceof KClass<?> kClass &&
  321 | 									KotlinDetector.isInlineClass(JvmClassMappingKt.getJavaClass(kClass)) &&
  322 | 									!JvmClassMappingKt.getJavaClass(kClass).isInstance(arg)) {
  323 | 								arg = box(kClass, arg);
  324 | 							}
  325 | 							argMap.put(parameter, arg);
  326 | 						}
  327 | 						index++;
  328 | 					}
  329 | 				}
  330 | 			}
  331 | 			Object result = function.callBy(argMap);
  332 | 			if (result != null && KotlinDetector.isInlineClass(result.getClass())) {
  333 | 				result = unbox(result);
  334 | 			}
  335 | 			return (result == Unit.INSTANCE ? null : result);
  336 | 		}
  337 | 
  338 | 		private static Object box(KClass<?> kClass, @Nullable Object arg) {
  339 | 			KFunction<?> constructor = Objects.requireNonNull(KClasses.getPrimaryConstructor(kClass));
  340 | 			KType type = constructor.getParameters().get(0).getType();
  341 | 			if (!(type.isMarkedNullable() && arg == null) &&
  342 | 					type.getClassifier() instanceof KClass<?> parameterClass &&
  343 | 					KotlinDetector.isInlineClass(JvmClassMappingKt.getJavaClass(parameterClass)) &&
  344 | 					!JvmClassMappingKt.getJavaClass(parameterClass).isInstance(arg)) {
  345 | 				arg = box(parameterClass, arg);
  346 | 			}
  347 | 			if (!KCallablesJvm.isAccessible(constructor)) {
  348 | 				KCallablesJvm.setAccessible(constructor, true);
  349 | 			}
  350 | 			return constructor.call(arg);
  351 | 		}
  352 | 
  353 | 		private static void handleResult(Object result, SynchronousSink<Object> sink) {
  354 | 			if (KotlinDetector.isInlineClass(result.getClass())) {
  355 | 				try {
  356 | 					Object unboxed = unbox(result);
  357 | 					if (unboxed != Unit.INSTANCE) {
  358 | 						sink.next(unboxed);
  359 | 					}
  360 | 					sink.complete();
  361 | 				}
  362 | 				catch (NoSuchMethodException | InvocationTargetException | IllegalAccessException ex) {
  363 | 					sink.error(ex);
  364 | 				}
  365 | 			}
  366 | 			else {
  367 | 				sink.next(result);
  368 | 				sink.complete();
  369 | 			}
  370 | 		}
  371 | 
  372 | 		private static Object unbox(Object result) throws InvocationTargetException, IllegalAccessException, NoSuchMethodException {
  373 | 			Object unboxed = result.getClass().getDeclaredMethod("unbox-impl").invoke(result);
  374 | 			if (KotlinDetector.isInlineClass(unboxed.getClass())) {
  375 | 				return unbox(unboxed);
  376 | 			}
  377 | 			return unboxed;
  378 | 		}
  379 | 	}
  380 | 
  381 | }
```
