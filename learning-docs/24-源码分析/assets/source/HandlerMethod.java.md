<!-- learning-docs source archive（机械抓取原文，禁止手改代码内容） -->
<!-- origin: org.springframework:spring-web:7.1.0 | path: org/springframework/web/method/HandlerMethod.java -->
<!-- 正文引用：[[源码:HandlerMethod.java#L起-L止]] ；代码块头部：(src: HandlerMethod.java#L起-L止) -->

# HandlerMethod.java

| 项 | 值 |
| --- | --- |
| 来源 | `org.springframework:spring-web:7.1.0` |
| 路径 | `org/springframework/web/method/HandlerMethod.java` |
| 行数 | 518 |
| 在线地址 | https://repo1.maven.org/maven2/org/springframework/spring-web/7.1.0/spring-web-7.1.0-sources.jar |
| 抓取时间 | 2026-11-25T14:05:28.465Z |

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
   17 | package org.springframework.web.method;
   18 | 
   19 | import java.lang.annotation.Annotation;
   20 | import java.lang.reflect.AnnotatedParameterizedType;
   21 | import java.lang.reflect.AnnotatedType;
   22 | import java.lang.reflect.Method;
   23 | import java.util.ArrayList;
   24 | import java.util.Collections;
   25 | import java.util.List;
   26 | import java.util.Map;
   27 | import java.util.StringJoiner;
   28 | import java.util.function.Predicate;
   29 | import java.util.stream.Collectors;
   30 | import java.util.stream.IntStream;
   31 | 
   32 | import org.apache.commons.logging.Log;
   33 | import org.apache.commons.logging.LogFactory;
   34 | import org.jspecify.annotations.Nullable;
   35 | 
   36 | import org.springframework.beans.factory.BeanFactory;
   37 | import org.springframework.context.MessageSource;
   38 | import org.springframework.context.i18n.LocaleContextHolder;
   39 | import org.springframework.core.MethodParameter;
   40 | import org.springframework.core.annotation.AnnotatedElementUtils;
   41 | import org.springframework.core.annotation.AnnotatedMethod;
   42 | import org.springframework.core.annotation.AnnotationUtils;
   43 | import org.springframework.core.annotation.MergedAnnotation;
   44 | import org.springframework.core.annotation.MergedAnnotationPredicates;
   45 | import org.springframework.core.annotation.MergedAnnotations;
   46 | import org.springframework.http.HttpStatusCode;
   47 | import org.springframework.util.Assert;
   48 | import org.springframework.util.ClassUtils;
   49 | import org.springframework.util.StringUtils;
   50 | import org.springframework.validation.annotation.Validated;
   51 | import org.springframework.validation.annotation.ValidationAnnotationUtils;
   52 | import org.springframework.web.bind.annotation.ResponseStatus;
   53 | 
   54 | /**
   55 |  * Encapsulates information about a handler method consisting of a
   56 |  * {@linkplain #getMethod() method} and a {@linkplain #getBean() bean}.
   57 |  * Provides convenient access to method parameters, the method return value,
   58 |  * method annotations, etc.
   59 |  *
   60 |  * <p>The class may be created with a bean instance or with a bean name
   61 |  * (for example, lazy-init bean, prototype bean). Use {@link #createWithResolvedBean()}
   62 |  * to obtain a {@code HandlerMethod} instance with a bean instance resolved
   63 |  * through the associated {@link BeanFactory}.
   64 |  *
   65 |  * @author Arjen Poutsma
   66 |  * @author Rossen Stoyanchev
   67 |  * @author Juergen Hoeller
   68 |  * @author Sam Brannen
   69 |  * @since 3.1
   70 |  */
   71 | public class HandlerMethod extends AnnotatedMethod {
   72 | 
   73 | 	/** Logger that is available to subclasses. */
   74 | 	protected static final Log logger = LogFactory.getLog(HandlerMethod.class);
   75 | 
   76 | 	private static final Class<?>[] EMPTY_CLASS_ARRAY = new Class<?>[0];
   77 | 
   78 | 
   79 | 	private final Object bean;
   80 | 
   81 | 	private final @Nullable BeanFactory beanFactory;
   82 | 
   83 | 	private final @Nullable MessageSource messageSource;
   84 | 
   85 | 	private final Class<?> beanType;
   86 | 
   87 | 	private final boolean validateArguments;
   88 | 
   89 | 	private final boolean validateReturnValue;
   90 | 
   91 | 	private Class<?> @Nullable [] validationGroups;
   92 | 
   93 | 	private @Nullable HttpStatusCode responseStatus;
   94 | 
   95 | 	private @Nullable String responseStatusReason;
   96 | 
   97 | 	private @Nullable HandlerMethod resolvedFromHandlerMethod;
   98 | 
   99 | 	private volatile @Nullable HandlerMethod resolvedBeanHandlerMethod;
  100 | 
  101 | 	private final String description;
  102 | 
  103 | 
  104 | 	/**
  105 | 	 * Create an instance from a bean instance and a method.
  106 | 	 */
  107 | 	public HandlerMethod(Object bean, Method method) {
  108 | 		this(bean, method, null);
  109 | 	}
  110 | 
  111 | 	/**
  112 | 	 * Variant of {@link #HandlerMethod(Object, Method)} that
  113 | 	 * also accepts a {@link MessageSource} for use from subclasses.
  114 | 	 * @since 5.3.10
  115 | 	 */
  116 | 	protected HandlerMethod(Object bean, Method method, @Nullable MessageSource messageSource) {
  117 | 		super(method);
  118 | 		this.bean = bean;
  119 | 		this.beanFactory = null;
  120 | 		this.messageSource = messageSource;
  121 | 		this.beanType = ClassUtils.getUserClass(bean);
  122 | 		this.validateArguments = false;
  123 | 		this.validateReturnValue = false;
  124 | 		evaluateResponseStatus();
  125 | 		this.description = initDescription(this.beanType, method);
  126 | 	}
  127 | 
  128 | 	/**
  129 | 	 * Create an instance from a bean instance, method name, and parameter types.
  130 | 	 * @throws NoSuchMethodException when the method cannot be found
  131 | 	 */
  132 | 	public HandlerMethod(Object bean, String methodName, Class<?>... parameterTypes) throws NoSuchMethodException {
  133 | 		super(bean.getClass().getMethod(methodName, parameterTypes));
  134 | 		this.bean = bean;
  135 | 		this.beanFactory = null;
  136 | 		this.messageSource = null;
  137 | 		this.beanType = ClassUtils.getUserClass(bean);
  138 | 		this.validateArguments = false;
  139 | 		this.validateReturnValue = false;
  140 | 		evaluateResponseStatus();
  141 | 		this.description = initDescription(this.beanType, getMethod());
  142 | 	}
  143 | 
  144 | 	/**
  145 | 	 * Create an instance from a bean name, a method, and a {@code BeanFactory}.
  146 | 	 * The method {@link #createWithResolvedBean()} may be used later to
  147 | 	 * re-create the {@code HandlerMethod} with an initialized bean.
  148 | 	 */
  149 | 	public HandlerMethod(String beanName, BeanFactory beanFactory, Method method) {
  150 | 		this(beanName, beanFactory, null, method);
  151 | 	}
  152 | 
  153 | 	/**
  154 | 	 * Variant of {@link #HandlerMethod(String, BeanFactory, Method)} that
  155 | 	 * also accepts a {@link MessageSource}.
  156 | 	 */
  157 | 	public HandlerMethod(
  158 | 			String beanName, BeanFactory beanFactory,
  159 | 			@Nullable MessageSource messageSource, Method method) {
  160 | 
  161 | 		super(method);
  162 | 		Assert.hasText(beanName, "Bean name is required");
  163 | 		Assert.notNull(beanFactory, "BeanFactory is required");
  164 | 		this.bean = beanName;
  165 | 		this.beanFactory = beanFactory;
  166 | 		this.messageSource = messageSource;
  167 | 		Class<?> beanType = beanFactory.getType(beanName);
  168 | 		if (beanType == null) {
  169 | 			throw new IllegalStateException("Cannot resolve bean type for bean with name '" + beanName + "'");
  170 | 		}
  171 | 		this.beanType = ClassUtils.getUserClass(beanType);
  172 | 		this.validateArguments = false;
  173 | 		this.validateReturnValue = false;
  174 | 		evaluateResponseStatus();
  175 | 		this.description = initDescription(this.beanType, method);
  176 | 	}
  177 | 
  178 | 	/**
  179 | 	 * Copy constructor for use in subclasses.
  180 | 	 */
  181 | 	protected HandlerMethod(HandlerMethod handlerMethod) {
  182 | 		this(handlerMethod, null, false);
  183 | 	}
  184 | 
  185 | 	/**
  186 | 	 * Re-create new HandlerMethod instance that copies the given HandlerMethod
  187 | 	 * but replaces the handler, and optionally checks for the presence of
  188 | 	 * validation annotations.
  189 | 	 * <p>Subclasses can override this to ensure that a HandlerMethod is of the
  190 | 	 * same type if re-created.
  191 | 	 * @since 6.2.3
  192 | 	 */
  193 | 	protected HandlerMethod(HandlerMethod handlerMethod, @Nullable Object handler, boolean initValidateFlags) {
  194 | 		super(handlerMethod);
  195 | 		this.bean = (handler != null ? handler : handlerMethod.bean);
  196 | 		this.beanFactory = handlerMethod.beanFactory;
  197 | 		this.messageSource = handlerMethod.messageSource;
  198 | 		this.beanType = handlerMethod.beanType;
  199 | 
  200 | 		this.validateArguments = (initValidateFlags ?
  201 | 				MethodValidationInitializer.checkArguments(this.beanType, getMethodParameters()) :
  202 | 				handlerMethod.validateArguments);
  203 | 
  204 | 		this.validateReturnValue = (initValidateFlags ?
  205 | 				MethodValidationInitializer.checkReturnValue(this.beanType, getBridgedMethod()) :
  206 | 				handlerMethod.validateReturnValue);
  207 | 
  208 | 		this.validationGroups = (handler != null && (shouldValidateArguments() || shouldValidateReturnValue()) ?
  209 | 				ValidationAnnotationUtils.determineValidationGroups(handler, getBridgedMethod()) :
  210 | 				handlerMethod.validationGroups);
  211 | 
  212 | 		this.responseStatus = handlerMethod.responseStatus;
  213 | 		this.responseStatusReason = handlerMethod.responseStatusReason;
  214 | 
  215 | 		this.resolvedFromHandlerMethod = (handlerMethod.resolvedFromHandlerMethod != null ?
  216 | 				handlerMethod.resolvedFromHandlerMethod : handlerMethod);
  217 | 
  218 | 		this.description = handlerMethod.toString();
  219 | 	}
  220 | 
  221 | 
  222 | 	private void evaluateResponseStatus() {
  223 | 		ResponseStatus annotation = getMethodAnnotation(ResponseStatus.class);
  224 | 		if (annotation == null) {
  225 | 			annotation = AnnotatedElementUtils.findMergedAnnotation(getBeanType(), ResponseStatus.class);
  226 | 		}
  227 | 		if (annotation != null) {
  228 | 			String reason = annotation.reason();
  229 | 			String resolvedReason = (StringUtils.hasText(reason) && this.messageSource != null ?
  230 | 					this.messageSource.getMessage(reason, null, reason, LocaleContextHolder.getLocale()) :
  231 | 					reason);
  232 | 
  233 | 			this.responseStatus = annotation.code();
  234 | 			this.responseStatusReason = resolvedReason;
  235 | 			if (StringUtils.hasText(this.responseStatusReason) && getMethod().getReturnType() != void.class) {
  236 | 				if (logger.isWarnEnabled()) {
  237 | 					logger.warn("Return value of [" + getMethod() +
  238 | 							"] will be ignored since @ResponseStatus 'reason' attribute is set.");
  239 | 				}
  240 | 			}
  241 | 		}
  242 | 	}
  243 | 
  244 | 	private static String initDescription(Class<?> beanType, Method method) {
  245 | 		StringJoiner joiner = new StringJoiner(", ", "(", ")");
  246 | 		for (Class<?> paramType : method.getParameterTypes()) {
  247 | 			joiner.add(paramType.getSimpleName());
  248 | 		}
  249 | 		return beanType.getName() + "#" + method.getName() + joiner;
  250 | 	}
  251 | 
  252 | 
  253 | 	/**
  254 | 	 * Return the bean for this handler method.
  255 | 	 */
  256 | 	public Object getBean() {
  257 | 		return this.bean;
  258 | 	}
  259 | 
  260 | 	/**
  261 | 	 * This method returns the type of the handler for this handler method.
  262 | 	 * <p>Note that if the bean type is a CGLIB-generated class, the original
  263 | 	 * user-defined class is returned.
  264 | 	 */
  265 | 	public Class<?> getBeanType() {
  266 | 		return this.beanType;
  267 | 	}
  268 | 
  269 | 	@Override
  270 | 	protected Class<?> getContainingClass() {
  271 | 		return this.beanType;
  272 | 	}
  273 | 
  274 | 	/**
  275 | 	 * Whether the method arguments are a candidate for method validation, which
  276 | 	 * is the case when there are parameter {@code jakarta.validation.Constraint}
  277 | 	 * annotations.
  278 | 	 * <p>The presence of {@code jakarta.validation.Valid} by itself does not
  279 | 	 * trigger method validation since such parameters are already validated at
  280 | 	 * the level of argument resolvers.
  281 | 	 * <p><strong>Note:</strong> if the class is annotated with {@link Validated},
  282 | 	 * this method returns false, deferring to method validation via AOP proxy.
  283 | 	 * @since 6.1
  284 | 	 */
  285 | 	public boolean shouldValidateArguments() {
  286 | 		return this.validateArguments;
  287 | 	}
  288 | 
  289 | 	/**
  290 | 	 * Whether the method return value is a candidate for method validation, which
  291 | 	 * is the case when there are method {@code jakarta.validation.Constraint}
  292 | 	 * or {@code jakarta.validation.Valid} annotations.
  293 | 	 * <p><strong>Note:</strong> if the class is annotated with {@link Validated},
  294 | 	 * this method returns false, deferring to method validation via AOP proxy.
  295 | 	 * @since 6.1
  296 | 	 */
  297 | 	public boolean shouldValidateReturnValue() {
  298 | 		return this.validateReturnValue;
  299 | 	}
  300 | 
  301 | 	/**
  302 | 	 * Return validation groups declared in
  303 | 	 * {@link org.springframework.validation.annotation.Validated @Validated}
  304 | 	 * either on the method, or on the containing target class of the method, or
  305 | 	 * for an AOP proxy without a target (with all behavior in advisors), also
  306 | 	 * check on proxied interfaces.
  307 | 	 * @since 7.0.4
  308 | 	 */
  309 | 	public Class<?>[] getValidationGroups() {
  310 | 		return (this.validationGroups != null ? this.validationGroups : EMPTY_CLASS_ARRAY);
  311 | 	}
  312 | 
  313 | 	/**
  314 | 	 * Return the specified response status, if any.
  315 | 	 * @since 4.3.8
  316 | 	 * @see ResponseStatus#code()
  317 | 	 */
  318 | 	protected @Nullable HttpStatusCode getResponseStatus() {
  319 | 		return this.responseStatus;
  320 | 	}
  321 | 
  322 | 	/**
  323 | 	 * Return the associated response status reason, if any.
  324 | 	 * @since 4.3.8
  325 | 	 * @see ResponseStatus#reason()
  326 | 	 */
  327 | 	protected @Nullable String getResponseStatusReason() {
  328 | 		return this.responseStatusReason;
  329 | 	}
  330 | 
  331 | 	/**
  332 | 	 * Return the original HandlerMethod instance that from which the current
  333 | 	 * HandlerMethod instance was created via either
  334 | 	 * {@link #createWithValidateFlags()} or {@link #createWithResolvedBean()}.
  335 | 	 * The original instance is needed for cached CORS config lookups.
  336 | 	 */
  337 | 	public @Nullable HandlerMethod getResolvedFromHandlerMethod() {
  338 | 		return this.resolvedFromHandlerMethod;
  339 | 	}
  340 | 
  341 | 	/**
  342 | 	 * Re-create the HandlerMethod and initialize
  343 | 	 * {@link #shouldValidateArguments()} and {@link #shouldValidateReturnValue()}.
  344 | 	 * @since 6.1.3
  345 | 	 */
  346 | 	public HandlerMethod createWithValidateFlags() {
  347 | 		return new HandlerMethod(this, null, true);
  348 | 	}
  349 | 
  350 | 	/**
  351 | 	 * If the {@link #getBean() handler} is a bean name rather than the actual
  352 | 	 * handler instance, resolve the bean name through Spring configuration
  353 | 	 * (e.g. for prototype beans), and return a new {@link HandlerMethod}
  354 | 	 * instance with the resolved handler.
  355 | 	 * <p>If the {@link #getBean() handler} is not String, return the same instance.
  356 | 	 */
  357 | 	public HandlerMethod createWithResolvedBean() {
  358 | 		HandlerMethod resolvedBeanHandlerMethod = this.resolvedBeanHandlerMethod;
  359 | 		if (resolvedBeanHandlerMethod != null) {
  360 | 			return resolvedBeanHandlerMethod;
  361 | 		}
  362 | 
  363 | 		// We need to resolve a bean name reference.
  364 | 		if (!(this.bean instanceof String beanName)) {
  365 | 			return this;
  366 | 		}
  367 | 		Assert.state(this.beanFactory != null, "Cannot resolve bean name without BeanFactory");
  368 | 		Object handler = this.beanFactory.getBean(beanName);
  369 | 
  370 | 		HandlerMethod handlerMethod = new HandlerMethod(this, handler, false);
  371 | 		if (this.beanFactory.isSingleton(beanName)) {
  372 | 			this.resolvedBeanHandlerMethod = handlerMethod;
  373 | 		}
  374 | 		return handlerMethod;
  375 | 	}
  376 | 
  377 | 	/**
  378 | 	 * Return a short representation of this handler method for log message purposes.
  379 | 	 * @since 4.3
  380 | 	 */
  381 | 	public String getShortLogMessage() {
  382 | 		return getBeanType().getName() + "#" + getMethod().getName() +
  383 | 				"[" + getMethod().getParameterCount() + " args]";
  384 | 	}
  385 | 
  386 | 
  387 | 	@Override
  388 | 	public boolean equals(@Nullable Object other) {
  389 | 		return (this == other || (super.equals(other) && other instanceof HandlerMethod otherMethod && this.bean.equals(otherMethod.bean)));
  390 | 	}
  391 | 
  392 | 	@Override
  393 | 	public int hashCode() {
  394 | 		return (this.bean.hashCode() * 31 + super.hashCode());
  395 | 	}
  396 | 
  397 | 	/**
  398 | 	 * Returns a concise description of this {@code HandlerMethod}, which is used
  399 | 	 * in log and error messages.
  400 | 	 * <p>The description should typically include the method signature of the
  401 | 	 * underlying handler method for clarity and debugging purposes.
  402 | 	 */
  403 | 	@Override
  404 | 	public String toString() {
  405 | 		return this.description;
  406 | 	}
  407 | 
  408 | 
  409 | 	// Support methods for use in subclass variants
  410 | 
  411 | 	/**
  412 | 	 * Assert that the target bean class is an instance of the class where the given method
  413 | 	 * is declared. In some cases the actual controller instance at request-processing time
  414 | 	 * may be a JDK dynamic proxy (lazy initialization, prototype beans, and others).
  415 | 	 * {@code @Controller}'s that require proxying should prefer class-based proxy mechanisms.
  416 | 	 */
  417 | 	protected void assertTargetBean(Method method, Object targetBean, @Nullable Object[] args) {
  418 | 		Class<?> methodDeclaringClass = method.getDeclaringClass();
  419 | 		Class<?> targetBeanClass = targetBean.getClass();
  420 | 		if (!methodDeclaringClass.isAssignableFrom(targetBeanClass)) {
  421 | 			String text = "The mapped handler method class '" + methodDeclaringClass.getName() +
  422 | 					"' is not an instance of the actual target bean class '" +
  423 | 					targetBeanClass.getName() + "'. If the target bean requires proxying " +
  424 | 					"(for example, due to @Transactional), please use class-based proxying.";
  425 | 			throw new IllegalStateException(formatInvokeError(text, args));
  426 | 		}
  427 | 	}
  428 | 
  429 | 	protected String formatInvokeError(String text, @Nullable Object[] args) {
  430 | 		String formattedArgs = IntStream.range(0, args.length)
  431 | 				.mapToObj(i -> {
  432 | 					Object arg = args[i];
  433 | 					return (arg != null ?
  434 | 							"[" + i + "] [type=" + arg.getClass().getName() + "] [value=" + arg + "]" :
  435 | 							"[" + i + "] [null]");
  436 | 				})
  437 | 				.collect(Collectors.joining(",\n"));
  438 | 		return text + "\n" +
  439 | 				"Handler [" + getBeanType().getName() + "]\n" +
  440 | 				"Method [" + getBridgedMethod().toGenericString() + "] " +
  441 | 				"with argument values:\n" + formattedArgs;
  442 | 	}
  443 | 
  444 | 
  445 | 	/**
  446 | 	 * Checks for the presence of {@code @Constraint} and {@code @Valid}
  447 | 	 * annotations on the method and method parameters.
  448 | 	 */
  449 | 	private static class MethodValidationInitializer {
  450 | 
  451 | 		private static final boolean BEAN_VALIDATION_PRESENT =
  452 | 				ClassUtils.isPresent("jakarta.validation.Validator", HandlerMethod.class.getClassLoader());
  453 | 
  454 | 		private static final Predicate<MergedAnnotation<? extends Annotation>> CONSTRAINT_PREDICATE =
  455 | 				MergedAnnotationPredicates.typeIn("jakarta.validation.Constraint");
  456 | 
  457 | 		private static final Predicate<MergedAnnotation<? extends Annotation>> VALID_PREDICATE =
  458 | 				MergedAnnotationPredicates.typeIn("jakarta.validation.Valid");
  459 | 
  460 | 		public static boolean checkArguments(Class<?> beanType, MethodParameter[] parameters) {
  461 | 			if (BEAN_VALIDATION_PRESENT && AnnotationUtils.findAnnotation(beanType, Validated.class) == null) {
  462 | 				for (MethodParameter param : parameters) {
  463 | 					MergedAnnotations merged = MergedAnnotations.from(param.getParameterAnnotations());
  464 | 					if (merged.stream().anyMatch(CONSTRAINT_PREDICATE)) {
  465 | 						return true;
  466 | 					}
  467 | 					Class<?> type = param.getParameterType();
  468 | 					if (merged.stream().anyMatch(VALID_PREDICATE) && isIndexOrKeyBasedContainer(type)) {
  469 | 						return true;
  470 | 					}
  471 | 					merged = MergedAnnotations.from(getContainerElementAnnotations(param));
  472 | 					if (merged.stream().anyMatch(CONSTRAINT_PREDICATE.or(VALID_PREDICATE))) {
  473 | 						return true;
  474 | 					}
  475 | 				}
  476 | 			}
  477 | 			return false;
  478 | 		}
  479 | 
  480 | 		public static boolean checkReturnValue(Class<?> beanType, Method method) {
  481 | 			if (BEAN_VALIDATION_PRESENT && AnnotationUtils.findAnnotation(beanType, Validated.class) == null) {
  482 | 				MergedAnnotations merged = MergedAnnotations.from(method, MergedAnnotations.SearchStrategy.TYPE_HIERARCHY);
  483 | 				return merged.stream().anyMatch(CONSTRAINT_PREDICATE.or(VALID_PREDICATE));
  484 | 			}
  485 | 			return false;
  486 | 		}
  487 | 
  488 | 		private static boolean isIndexOrKeyBasedContainer(Class<?> type) {
  489 | 
  490 | 			// Index or key-based containers only, or MethodValidationAdapter cannot access
  491 | 			// the element given what is exposed in ConstraintViolation.
  492 | 
  493 | 			return (List.class.isAssignableFrom(type) || Object[].class.isAssignableFrom(type) ||
  494 | 					Map.class.isAssignableFrom(type));
  495 | 		}
  496 | 
  497 | 		/**
  498 | 		 * There may be constraints on elements of a container (list, map).
  499 | 		 */
  500 | 		private static Annotation[] getContainerElementAnnotations(MethodParameter param) {
  501 | 			List<Annotation> result = null;
  502 | 			int i = param.getParameterIndex();
  503 | 			Method method = param.getMethod();
  504 | 			if (method != null && method.getAnnotatedParameterTypes()[i] instanceof AnnotatedParameterizedType apt) {
  505 | 				for (AnnotatedType type : apt.getAnnotatedActualTypeArguments()) {
  506 | 					for (Annotation annot : type.getAnnotations()) {
  507 | 						result = (result != null ? result : new ArrayList<>());
  508 | 						result.add(annot);
  509 | 					}
  510 | 				}
  511 | 			}
  512 | 			result = (result != null ? result : Collections.emptyList());
  513 | 			return result.toArray(new Annotation[0]);
  514 | 		}
  515 | 
  516 | 	}
  517 | 
  518 | }
```
