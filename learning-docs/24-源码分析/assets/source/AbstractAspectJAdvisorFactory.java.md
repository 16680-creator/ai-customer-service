<!-- learning-docs source archive（机械抓取原文，禁止手改代码内容） -->
<!-- origin: org.springframework:spring-aop:7.1.0 | path: org/springframework/aop/aspectj/annotation/AbstractAspectJAdvisorFactory.java -->
<!-- 正文引用：[[源码:AbstractAspectJAdvisorFactory.java#L起-L止]] ；代码块头部：(src: AbstractAspectJAdvisorFactory.java#L起-L止) -->

# AbstractAspectJAdvisorFactory.java

| 项 | 值 |
| --- | --- |
| 来源 | `org.springframework:spring-aop:7.1.0` |
| 路径 | `org/springframework/aop/aspectj/annotation/AbstractAspectJAdvisorFactory.java` |
| 行数 | 271 |
| 在线地址 | https://repo1.maven.org/maven2/org/springframework/spring-aop/7.1.0/spring-aop-7.1.0-sources.jar |
| 抓取时间 | 2026-11-25T14:00:00.979Z |

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
   19 | import java.lang.annotation.Annotation;
   20 | import java.lang.reflect.Constructor;
   21 | import java.lang.reflect.Field;
   22 | import java.lang.reflect.Method;
   23 | import java.util.Map;
   24 | import java.util.StringTokenizer;
   25 | 
   26 | import org.apache.commons.logging.Log;
   27 | import org.apache.commons.logging.LogFactory;
   28 | import org.aspectj.lang.annotation.After;
   29 | import org.aspectj.lang.annotation.AfterReturning;
   30 | import org.aspectj.lang.annotation.AfterThrowing;
   31 | import org.aspectj.lang.annotation.Around;
   32 | import org.aspectj.lang.annotation.Aspect;
   33 | import org.aspectj.lang.annotation.Before;
   34 | import org.aspectj.lang.annotation.Pointcut;
   35 | import org.aspectj.lang.reflect.AjType;
   36 | import org.aspectj.lang.reflect.AjTypeSystem;
   37 | import org.aspectj.lang.reflect.PerClauseKind;
   38 | import org.jspecify.annotations.Nullable;
   39 | 
   40 | import org.springframework.aop.framework.AopConfigException;
   41 | import org.springframework.core.ParameterNameDiscoverer;
   42 | import org.springframework.core.SpringProperties;
   43 | import org.springframework.core.annotation.AnnotationUtils;
   44 | 
   45 | /**
   46 |  * Abstract base class for factories that can create Spring AOP Advisors
   47 |  * given AspectJ classes from classes honoring the AspectJ 5 annotation syntax.
   48 |  *
   49 |  * <p>This class handles annotation parsing and validation functionality.
   50 |  * It does not actually generate Spring AOP Advisors, which is deferred to subclasses.
   51 |  *
   52 |  * @author Rod Johnson
   53 |  * @author Adrian Colyer
   54 |  * @author Juergen Hoeller
   55 |  * @author Sam Brannen
   56 |  * @since 2.0
   57 |  */
   58 | public abstract class AbstractAspectJAdvisorFactory implements AspectJAdvisorFactory {
   59 | 
   60 | 	private static final Class<?>[] ASPECTJ_ANNOTATION_CLASSES = new Class<?>[] {
   61 | 			Pointcut.class, Around.class, Before.class, After.class, AfterReturning.class, AfterThrowing.class};
   62 | 
   63 | 	private static final String AJC_MAGIC = "ajc$";
   64 | 
   65 | 	/**
   66 | 	 * System property that instructs Spring to ignore ajc-compiled aspects
   67 | 	 * for Spring AOP proxying, restoring traditional Spring behavior for
   68 | 	 * scenarios where both weaving and AspectJ auto-proxying are enabled.
   69 | 	 * <p>The default is "false". Consider switching this to "true" if you
   70 | 	 * encounter double execution of your aspects in a given build setup.
   71 | 	 * Note that we recommend restructuring your AspectJ configuration to
   72 | 	 * avoid such double exposure of an AspectJ aspect to begin with.
   73 | 	 * @since 6.1.15
   74 | 	 */
   75 | 	public static final String IGNORE_AJC_PROPERTY_NAME = "spring.aop.ajc.ignore";
   76 | 
   77 | 	private static final boolean shouldIgnoreAjcCompiledAspects =
   78 | 			SpringProperties.getFlag(IGNORE_AJC_PROPERTY_NAME);
   79 | 
   80 | 
   81 | 	/** Logger available to subclasses. */
   82 | 	protected final Log logger = LogFactory.getLog(getClass());
   83 | 
   84 | 	protected final ParameterNameDiscoverer parameterNameDiscoverer = new AspectJAnnotationParameterNameDiscoverer();
   85 | 
   86 | 
   87 | 	@Override
   88 | 	public boolean isAspect(Class<?> clazz) {
   89 | 		return (AnnotationUtils.findAnnotation(clazz, Aspect.class) != null &&
   90 | 				(!shouldIgnoreAjcCompiledAspects || !compiledByAjc(clazz)));
   91 | 	}
   92 | 
   93 | 	@Override
   94 | 	public void validate(Class<?> aspectClass) throws AopConfigException {
   95 | 		AjType<?> ajType = AjTypeSystem.getAjType(aspectClass);
   96 | 		if (!ajType.isAspect()) {
   97 | 			throw new NotAnAtAspectException(aspectClass);
   98 | 		}
   99 | 		if (ajType.getPerClause().getKind() == PerClauseKind.PERCFLOW) {
  100 | 			throw new AopConfigException(aspectClass.getName() + " uses percflow instantiation model: " +
  101 | 					"This is not supported in Spring AOP.");
  102 | 		}
  103 | 		if (ajType.getPerClause().getKind() == PerClauseKind.PERCFLOWBELOW) {
  104 | 			throw new AopConfigException(aspectClass.getName() + " uses percflowbelow instantiation model: " +
  105 | 					"This is not supported in Spring AOP.");
  106 | 		}
  107 | 	}
  108 | 
  109 | 
  110 | 	/**
  111 | 	 * Find and return the first AspectJ annotation on the given method
  112 | 	 * (there <i>should</i> only be one anyway...).
  113 | 	 */
  114 | 	@SuppressWarnings("unchecked")
  115 | 	protected static @Nullable AspectJAnnotation findAspectJAnnotationOnMethod(Method method) {
  116 | 		for (Class<?> annotationType : ASPECTJ_ANNOTATION_CLASSES) {
  117 | 			AspectJAnnotation annotation = findAnnotation(method, (Class<Annotation>) annotationType);
  118 | 			if (annotation != null) {
  119 | 				return annotation;
  120 | 			}
  121 | 		}
  122 | 		return null;
  123 | 	}
  124 | 
  125 | 	private static @Nullable AspectJAnnotation findAnnotation(Method method, Class<? extends Annotation> annotationType) {
  126 | 		Annotation annotation = AnnotationUtils.findAnnotation(method, annotationType);
  127 | 		if (annotation != null) {
  128 | 			return new AspectJAnnotation(annotation);
  129 | 		}
  130 | 		else {
  131 | 			return null;
  132 | 		}
  133 | 	}
  134 | 
  135 | 	private static boolean compiledByAjc(Class<?> clazz) {
  136 | 		for (Field field : clazz.getDeclaredFields()) {
  137 | 			if (field.getName().startsWith(AJC_MAGIC)) {
  138 | 				return true;
  139 | 			}
  140 | 		}
  141 | 		return false;
  142 | 	}
  143 | 
  144 | 
  145 | 	/**
  146 | 	 * Enum for AspectJ annotation types.
  147 | 	 * @see AspectJAnnotation#getAnnotationType()
  148 | 	 */
  149 | 	protected enum AspectJAnnotationType {
  150 | 
  151 | 		AtPointcut, AtAround, AtBefore, AtAfter, AtAfterReturning, AtAfterThrowing
  152 | 	}
  153 | 
  154 | 
  155 | 	/**
  156 | 	 * Class modeling an AspectJ annotation, exposing its type enumeration and
  157 | 	 * pointcut String.
  158 | 	 */
  159 | 	protected static class AspectJAnnotation {
  160 | 
  161 | 		private static final String[] EXPRESSION_ATTRIBUTES = {"pointcut", "value"};
  162 | 
  163 | 		private static final Map<Class<?>, AspectJAnnotationType> annotationTypeMap = Map.of(
  164 | 				Pointcut.class, AspectJAnnotationType.AtPointcut, //
  165 | 				Around.class, AspectJAnnotationType.AtAround, //
  166 | 				Before.class, AspectJAnnotationType.AtBefore, //
  167 | 				After.class, AspectJAnnotationType.AtAfter, //
  168 | 				AfterReturning.class, AspectJAnnotationType.AtAfterReturning, //
  169 | 				AfterThrowing.class, AspectJAnnotationType.AtAfterThrowing //
  170 | 			);
  171 | 
  172 | 		private final Annotation annotation;
  173 | 
  174 | 		private final AspectJAnnotationType annotationType;
  175 | 
  176 | 		private final String pointcutExpression;
  177 | 
  178 | 		private final String argumentNames;
  179 | 
  180 | 		public AspectJAnnotation(Annotation annotation) {
  181 | 			this.annotation = annotation;
  182 | 			this.annotationType = determineAnnotationType(annotation);
  183 | 			try {
  184 | 				this.pointcutExpression = resolvePointcutExpression(annotation);
  185 | 				Object argNames = AnnotationUtils.getValue(annotation, "argNames");
  186 | 				this.argumentNames = (argNames instanceof String names ? names : "");
  187 | 			}
  188 | 			catch (Exception ex) {
  189 | 				throw new IllegalArgumentException(annotation + " is not a valid AspectJ annotation", ex);
  190 | 			}
  191 | 		}
  192 | 
  193 | 		private AspectJAnnotationType determineAnnotationType(Annotation annotation) {
  194 | 			AspectJAnnotationType type = annotationTypeMap.get(annotation.annotationType());
  195 | 			if (type != null) {
  196 | 				return type;
  197 | 			}
  198 | 			throw new IllegalStateException("Unknown annotation type: " + annotation);
  199 | 		}
  200 | 
  201 | 		private String resolvePointcutExpression(Annotation annotation) {
  202 | 			for (String attributeName : EXPRESSION_ATTRIBUTES) {
  203 | 				Object val = AnnotationUtils.getValue(annotation, attributeName);
  204 | 				if (val instanceof String str && !str.isEmpty()) {
  205 | 					return str;
  206 | 				}
  207 | 			}
  208 | 			throw new IllegalStateException("Failed to resolve pointcut expression in: " + annotation);
  209 | 		}
  210 | 
  211 | 		public AspectJAnnotationType getAnnotationType() {
  212 | 			return this.annotationType;
  213 | 		}
  214 | 
  215 | 		public Annotation getAnnotation() {
  216 | 			return this.annotation;
  217 | 		}
  218 | 
  219 | 		public String getPointcutExpression() {
  220 | 			return this.pointcutExpression;
  221 | 		}
  222 | 
  223 | 		public String getArgumentNames() {
  224 | 			return this.argumentNames;
  225 | 		}
  226 | 
  227 | 		@Override
  228 | 		public String toString() {
  229 | 			return this.annotation.toString();
  230 | 		}
  231 | 	}
  232 | 
  233 | 
  234 | 	/**
  235 | 	 * ParameterNameDiscoverer implementation that analyzes the arg names
  236 | 	 * specified at the AspectJ annotation level.
  237 | 	 */
  238 | 	private static class AspectJAnnotationParameterNameDiscoverer implements ParameterNameDiscoverer {
  239 | 
  240 | 		private static final String[] EMPTY_ARRAY = new String[0];
  241 | 
  242 | 		@Override
  243 | 		public String @Nullable [] getParameterNames(Method method) {
  244 | 			if (method.getParameterCount() == 0) {
  245 | 				return EMPTY_ARRAY;
  246 | 			}
  247 | 			AspectJAnnotation annotation = findAspectJAnnotationOnMethod(method);
  248 | 			if (annotation == null) {
  249 | 				return null;
  250 | 			}
  251 | 			StringTokenizer nameTokens = new StringTokenizer(annotation.getArgumentNames(), ",");
  252 | 			int numTokens = nameTokens.countTokens();
  253 | 			if (numTokens > 0) {
  254 | 				String[] names = new String[numTokens];
  255 | 				for (int i = 0; i < names.length; i++) {
  256 | 					names[i] = nameTokens.nextToken();
  257 | 				}
  258 | 				return names;
  259 | 			}
  260 | 			else {
  261 | 				return null;
  262 | 			}
  263 | 		}
  264 | 
  265 | 		@Override
  266 | 		public @Nullable String @Nullable [] getParameterNames(Constructor<?> ctor) {
  267 | 			throw new UnsupportedOperationException("Spring AOP cannot handle constructor advice");
  268 | 		}
  269 | 	}
  270 | 
  271 | }
```
