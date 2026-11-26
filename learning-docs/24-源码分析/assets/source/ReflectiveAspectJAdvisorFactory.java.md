<!-- learning-docs source archive（机械抓取原文，禁止手改代码内容） -->
<!-- origin: org.springframework:spring-aop:7.1.0 | path: org/springframework/aop/aspectj/annotation/ReflectiveAspectJAdvisorFactory.java -->
<!-- 正文引用：[[源码:ReflectiveAspectJAdvisorFactory.java#L起-L止]] ；代码块头部：(src: ReflectiveAspectJAdvisorFactory.java#L起-L止) -->

# ReflectiveAspectJAdvisorFactory.java

| 项 | 值 |
| --- | --- |
| 来源 | `org.springframework:spring-aop:7.1.0` |
| 路径 | `org/springframework/aop/aspectj/annotation/ReflectiveAspectJAdvisorFactory.java` |
| 行数 | 328 |
| 在线地址 | https://repo1.maven.org/maven2/org/springframework/spring-aop/7.1.0/spring-aop-7.1.0-sources.jar |
| 抓取时间 | 2026-11-25T14:00:01.511Z |

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
   17 | package org.springframework.aop.aspectj.annotation;
   18 | 
   19 | import java.io.Serializable;
   20 | import java.lang.annotation.Annotation;
   21 | import java.lang.reflect.Field;
   22 | import java.lang.reflect.Method;
   23 | import java.util.ArrayList;
   24 | import java.util.Comparator;
   25 | import java.util.List;
   26 | 
   27 | import org.aopalliance.aop.Advice;
   28 | import org.aspectj.lang.annotation.After;
   29 | import org.aspectj.lang.annotation.AfterReturning;
   30 | import org.aspectj.lang.annotation.AfterThrowing;
   31 | import org.aspectj.lang.annotation.Around;
   32 | import org.aspectj.lang.annotation.Before;
   33 | import org.aspectj.lang.annotation.DeclareParents;
   34 | import org.aspectj.lang.annotation.Pointcut;
   35 | import org.jspecify.annotations.Nullable;
   36 | 
   37 | import org.springframework.aop.Advisor;
   38 | import org.springframework.aop.MethodBeforeAdvice;
   39 | import org.springframework.aop.aspectj.AbstractAspectJAdvice;
   40 | import org.springframework.aop.aspectj.AspectJAfterAdvice;
   41 | import org.springframework.aop.aspectj.AspectJAfterReturningAdvice;
   42 | import org.springframework.aop.aspectj.AspectJAfterThrowingAdvice;
   43 | import org.springframework.aop.aspectj.AspectJAroundAdvice;
   44 | import org.springframework.aop.aspectj.AspectJExpressionPointcut;
   45 | import org.springframework.aop.aspectj.AspectJMethodBeforeAdvice;
   46 | import org.springframework.aop.aspectj.DeclareParentsAdvisor;
   47 | import org.springframework.aop.framework.AopConfigException;
   48 | import org.springframework.aop.support.DefaultPointcutAdvisor;
   49 | import org.springframework.beans.factory.BeanFactory;
   50 | import org.springframework.core.annotation.AnnotationUtils;
   51 | import org.springframework.core.convert.converter.ConvertingComparator;
   52 | import org.springframework.util.ClassUtils;
   53 | import org.springframework.util.ReflectionUtils;
   54 | import org.springframework.util.ReflectionUtils.MethodFilter;
   55 | import org.springframework.util.StringUtils;
   56 | import org.springframework.util.comparator.InstanceComparator;
   57 | 
   58 | /**
   59 |  * Factory that can create Spring AOP Advisors given AspectJ classes from
   60 |  * classes honoring AspectJ's annotation syntax, using reflection to invoke the
   61 |  * corresponding advice methods.
   62 |  *
   63 |  * @author Rod Johnson
   64 |  * @author Adrian Colyer
   65 |  * @author Juergen Hoeller
   66 |  * @author Ramnivas Laddad
   67 |  * @author Phillip Webb
   68 |  * @author Sam Brannen
   69 |  * @since 2.0
   70 |  */
   71 | @SuppressWarnings("serial")
   72 | public class ReflectiveAspectJAdvisorFactory extends AbstractAspectJAdvisorFactory implements Serializable {
   73 | 
   74 | 	// Exclude @Pointcut methods
   75 | 	private static final MethodFilter adviceMethodFilter = ReflectionUtils.USER_DECLARED_METHODS
   76 | 			.and(method -> (AnnotationUtils.getAnnotation(method, Pointcut.class) == null));
   77 | 
   78 | 	private static final Comparator<Method> adviceMethodComparator;
   79 | 
   80 | 	static {
   81 | 		// Note: although @After is ordered before @AfterReturning and @AfterThrowing,
   82 | 		// an @After advice method will actually be invoked after @AfterReturning and
   83 | 		// @AfterThrowing methods due to the fact that AspectJAfterAdvice.invoke(MethodInvocation)
   84 | 		// invokes proceed() in a `try` block and only invokes the @After advice method
   85 | 		// in a corresponding `finally` block.
   86 | 		Comparator<Method> adviceKindComparator = new ConvertingComparator<Method, @Nullable Annotation>(
   87 | 				new InstanceComparator<>(
   88 | 						Around.class, Before.class, After.class, AfterReturning.class, AfterThrowing.class),
   89 | 				method -> {
   90 | 					AspectJAnnotation ann = AbstractAspectJAdvisorFactory.findAspectJAnnotationOnMethod(method);
   91 | 					return (ann != null ? ann.getAnnotation() : null);
   92 | 				});
   93 | 		Comparator<Method> methodNameComparator = new ConvertingComparator<>(Method::getName);
   94 | 		adviceMethodComparator = adviceKindComparator.thenComparing(methodNameComparator);
   95 | 	}
   96 | 
   97 | 
   98 | 	private final @Nullable BeanFactory beanFactory;
   99 | 
  100 | 
  101 | 	/**
  102 | 	 * Create a new {@code ReflectiveAspectJAdvisorFactory}.
  103 | 	 */
  104 | 	public ReflectiveAspectJAdvisorFactory() {
  105 | 		this(null);
  106 | 	}
  107 | 
  108 | 	/**
  109 | 	 * Create a new {@code ReflectiveAspectJAdvisorFactory}, propagating the given
  110 | 	 * {@link BeanFactory} to the created {@link AspectJExpressionPointcut} instances,
  111 | 	 * for bean pointcut handling as well as consistent {@link ClassLoader} resolution.
  112 | 	 * @param beanFactory the BeanFactory to propagate (may be {@code null})
  113 | 	 * @since 4.3.6
  114 | 	 * @see AspectJExpressionPointcut#setBeanFactory
  115 | 	 * @see org.springframework.beans.factory.config.ConfigurableBeanFactory#getBeanClassLoader()
  116 | 	 */
  117 | 	public ReflectiveAspectJAdvisorFactory(@Nullable BeanFactory beanFactory) {
  118 | 		this.beanFactory = beanFactory;
  119 | 	}
  120 | 
  121 | 
  122 | 	@Override
  123 | 	public List<Advisor> getAdvisors(MetadataAwareAspectInstanceFactory aspectInstanceFactory) {
  124 | 		Class<?> aspectClass = aspectInstanceFactory.getAspectMetadata().getAspectClass();
  125 | 		String aspectName = aspectInstanceFactory.getAspectMetadata().getAspectName();
  126 | 		validate(aspectClass);
  127 | 
  128 | 		// We need to wrap the MetadataAwareAspectInstanceFactory with a decorator
  129 | 		// so that it will only instantiate once.
  130 | 		MetadataAwareAspectInstanceFactory lazySingletonAspectInstanceFactory =
  131 | 				new LazySingletonAspectInstanceFactoryDecorator(aspectInstanceFactory);
  132 | 
  133 | 		List<Advisor> advisors = new ArrayList<>();
  134 | 		for (Method method : getAdvisorMethods(aspectClass)) {
  135 | 			if (method.equals(ClassUtils.getMostSpecificMethod(method, aspectClass))) {
  136 | 				// Prior to Spring Framework 5.2.7, advisors.size() was supplied as the declarationOrderInAspect
  137 | 				// to getAdvisor(...) to represent the "current position" in the declared methods list.
  138 | 				// However, since Java 7 the "current position" is not valid since the JDK no longer
  139 | 				// returns declared methods in the order in which they are declared in the source code.
  140 | 				// Thus, we now hard code the declarationOrderInAspect to 0 for all advice methods
  141 | 				// discovered via reflection in order to support reliable advice ordering across JVM launches.
  142 | 				// Specifically, a value of 0 aligns with the default value used in
  143 | 				// AspectJPrecedenceComparator.getAspectDeclarationOrder(Advisor).
  144 | 				Advisor advisor = getAdvisor(method, lazySingletonAspectInstanceFactory, 0, aspectName);
  145 | 				if (advisor != null) {
  146 | 					advisors.add(advisor);
  147 | 				}
  148 | 			}
  149 | 		}
  150 | 
  151 | 		// If it's a per target aspect, emit the dummy instantiating aspect.
  152 | 		if (!advisors.isEmpty() && lazySingletonAspectInstanceFactory.getAspectMetadata().isLazilyInstantiated()) {
  153 | 			Advisor instantiationAdvisor = new SyntheticInstantiationAdvisor(lazySingletonAspectInstanceFactory);
  154 | 			advisors.add(0, instantiationAdvisor);
  155 | 		}
  156 | 
  157 | 		// Find introduction fields.
  158 | 		for (Field field : aspectClass.getDeclaredFields()) {
  159 | 			Advisor advisor = getDeclareParentsAdvisor(field);
  160 | 			if (advisor != null) {
  161 | 				advisors.add(advisor);
  162 | 			}
  163 | 		}
  164 | 
  165 | 		return advisors;
  166 | 	}
  167 | 
  168 | 	private List<Method> getAdvisorMethods(Class<?> aspectClass) {
  169 | 		List<Method> methods = new ArrayList<>();
  170 | 		ReflectionUtils.doWithMethods(aspectClass, methods::add, adviceMethodFilter);
  171 | 		if (methods.size() > 1) {
  172 | 			methods.sort(adviceMethodComparator);
  173 | 		}
  174 | 		return methods;
  175 | 	}
  176 | 
  177 | 	/**
  178 | 	 * Build a {@link org.springframework.aop.aspectj.DeclareParentsAdvisor}
  179 | 	 * for the given introduction field.
  180 | 	 * <p>Resulting Advisors will need to be evaluated for targets.
  181 | 	 * @param introductionField the field to introspect
  182 | 	 * @return the Advisor instance, or {@code null} if not an Advisor
  183 | 	 */
  184 | 	private @Nullable Advisor getDeclareParentsAdvisor(Field introductionField) {
  185 | 		DeclareParents declareParents = introductionField.getAnnotation(DeclareParents.class);
  186 | 		if (declareParents == null) {
  187 | 			// Not an introduction field
  188 | 			return null;
  189 | 		}
  190 | 
  191 | 		if (DeclareParents.class == declareParents.defaultImpl()) {
  192 | 			throw new IllegalStateException("'defaultImpl' attribute must be set on DeclareParents");
  193 | 		}
  194 | 
  195 | 		return new DeclareParentsAdvisor(
  196 | 				introductionField.getType(), declareParents.value(), declareParents.defaultImpl());
  197 | 	}
  198 | 
  199 | 
  200 | 	@Override
  201 | 	public @Nullable Advisor getAdvisor(Method candidateAdviceMethod, MetadataAwareAspectInstanceFactory aspectInstanceFactory,
  202 | 			int declarationOrderInAspect, String aspectName) {
  203 | 
  204 | 		validate(aspectInstanceFactory.getAspectMetadata().getAspectClass());
  205 | 
  206 | 		AspectJExpressionPointcut expressionPointcut = getPointcut(
  207 | 				candidateAdviceMethod, aspectInstanceFactory.getAspectMetadata().getAspectClass());
  208 | 		if (expressionPointcut == null) {
  209 | 			return null;
  210 | 		}
  211 | 
  212 | 		try {
  213 | 			return new InstantiationModelAwarePointcutAdvisorImpl(expressionPointcut, candidateAdviceMethod,
  214 | 					this, aspectInstanceFactory, declarationOrderInAspect, aspectName);
  215 | 		}
  216 | 		catch (IllegalArgumentException | IllegalStateException ex) {
  217 | 			if (logger.isDebugEnabled()) {
  218 | 				logger.debug("Ignoring incompatible advice method: " + candidateAdviceMethod, ex);
  219 | 			}
  220 | 			return null;
  221 | 		}
  222 | 	}
  223 | 
  224 | 	private @Nullable AspectJExpressionPointcut getPointcut(Method candidateAdviceMethod, Class<?> candidateAspectClass) {
  225 | 		AspectJAnnotation aspectJAnnotation =
  226 | 				AbstractAspectJAdvisorFactory.findAspectJAnnotationOnMethod(candidateAdviceMethod);
  227 | 		if (aspectJAnnotation == null) {
  228 | 			return null;
  229 | 		}
  230 | 
  231 | 		AspectJExpressionPointcut ajexp =
  232 | 				new AspectJExpressionPointcut(candidateAspectClass, new String[0], new Class<?>[0]);
  233 | 		ajexp.setExpression(aspectJAnnotation.getPointcutExpression());
  234 | 		if (this.beanFactory != null) {
  235 | 			ajexp.setBeanFactory(this.beanFactory);
  236 | 		}
  237 | 		return ajexp;
  238 | 	}
  239 | 
  240 | 
  241 | 	@Override
  242 | 	public @Nullable Advice getAdvice(Method candidateAdviceMethod, AspectJExpressionPointcut expressionPointcut,
  243 | 			MetadataAwareAspectInstanceFactory aspectInstanceFactory, int declarationOrder, String aspectName) {
  244 | 
  245 | 		Class<?> candidateAspectClass = aspectInstanceFactory.getAspectMetadata().getAspectClass();
  246 | 		validate(candidateAspectClass);
  247 | 
  248 | 		AspectJAnnotation aspectJAnnotation =
  249 | 				AbstractAspectJAdvisorFactory.findAspectJAnnotationOnMethod(candidateAdviceMethod);
  250 | 		if (aspectJAnnotation == null) {
  251 | 			return null;
  252 | 		}
  253 | 
  254 | 		// If we get here, we know we have an AspectJ method.
  255 | 		// Check that it's an AspectJ-annotated class
  256 | 		if (!isAspect(candidateAspectClass)) {
  257 | 			throw new AopConfigException("Advice must be declared inside an aspect type: " +
  258 | 					"Offending method '" + candidateAdviceMethod + "' in class [" +
  259 | 					candidateAspectClass.getName() + "]");
  260 | 		}
  261 | 
  262 | 		if (logger.isDebugEnabled()) {
  263 | 			logger.debug("Found AspectJ method: " + candidateAdviceMethod);
  264 | 		}
  265 | 
  266 | 		AbstractAspectJAdvice springAdvice;
  267 | 
  268 | 		switch (aspectJAnnotation.getAnnotationType()) {
  269 | 			case AtPointcut -> {
  270 | 				if (logger.isDebugEnabled()) {
  271 | 					logger.debug("Processing pointcut '" + candidateAdviceMethod.getName() + "'");
  272 | 				}
  273 | 				return null;
  274 | 			}
  275 | 			case AtAround -> springAdvice = new AspectJAroundAdvice(
  276 | 					candidateAdviceMethod, expressionPointcut, aspectInstanceFactory);
  277 | 			case AtBefore -> springAdvice = new AspectJMethodBeforeAdvice(
  278 | 					candidateAdviceMethod, expressionPointcut, aspectInstanceFactory);
  279 | 			case AtAfter -> springAdvice = new AspectJAfterAdvice(
  280 | 					candidateAdviceMethod, expressionPointcut, aspectInstanceFactory);
  281 | 			case AtAfterReturning -> {
  282 | 				springAdvice = new AspectJAfterReturningAdvice(
  283 | 						candidateAdviceMethod, expressionPointcut, aspectInstanceFactory);
  284 | 				AfterReturning afterReturningAnnotation = (AfterReturning) aspectJAnnotation.getAnnotation();
  285 | 				if (StringUtils.hasText(afterReturningAnnotation.returning())) {
  286 | 					springAdvice.setReturningName(afterReturningAnnotation.returning());
  287 | 				}
  288 | 			}
  289 | 			case AtAfterThrowing -> {
  290 | 				springAdvice = new AspectJAfterThrowingAdvice(
  291 | 						candidateAdviceMethod, expressionPointcut, aspectInstanceFactory);
  292 | 				AfterThrowing afterThrowingAnnotation = (AfterThrowing) aspectJAnnotation.getAnnotation();
  293 | 				if (StringUtils.hasText(afterThrowingAnnotation.throwing())) {
  294 | 					springAdvice.setThrowingName(afterThrowingAnnotation.throwing());
  295 | 				}
  296 | 			}
  297 | 			default -> throw new UnsupportedOperationException(
  298 | 					"Unsupported advice type on method: " + candidateAdviceMethod);
  299 | 		}
  300 | 
  301 | 		// Now to configure the advice...
  302 | 		springAdvice.setAspectName(aspectName);
  303 | 		springAdvice.setDeclarationOrder(declarationOrder);
  304 | 		@Nullable String[] argNames = this.parameterNameDiscoverer.getParameterNames(candidateAdviceMethod);
  305 | 		if (argNames != null) {
  306 | 			springAdvice.setArgumentNamesFromStringArray(argNames);
  307 | 		}
  308 | 		springAdvice.calculateArgumentBindings();
  309 | 
  310 | 		return springAdvice;
  311 | 	}
  312 | 
  313 | 
  314 | 	/**
  315 | 	 * Synthetic advisor that instantiates the aspect.
  316 | 	 * Triggered by per-clause pointcut on non-singleton aspect.
  317 | 	 * The advice has no effect.
  318 | 	 */
  319 | 	@SuppressWarnings("serial")
  320 | 	protected static class SyntheticInstantiationAdvisor extends DefaultPointcutAdvisor {
  321 | 
  322 | 		public SyntheticInstantiationAdvisor(final MetadataAwareAspectInstanceFactory aif) {
  323 | 			super(aif.getAspectMetadata().getPerClausePointcut(), (MethodBeforeAdvice)
  324 | 					(method, args, target) -> aif.getAspectInstance());
  325 | 		}
  326 | 	}
  327 | 
  328 | }
```
