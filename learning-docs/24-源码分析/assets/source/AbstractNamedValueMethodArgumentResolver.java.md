<!-- learning-docs source archive（机械抓取原文，禁止手改代码内容） -->
<!-- origin: org.springframework:spring-web:7.1.0 | path: org/springframework/web/method/annotation/AbstractNamedValueMethodArgumentResolver.java -->
<!-- 正文引用：[[源码:AbstractNamedValueMethodArgumentResolver.java#L起-L止]] ；代码块头部：(src: AbstractNamedValueMethodArgumentResolver.java#L起-L止) -->

# AbstractNamedValueMethodArgumentResolver.java

| 项 | 值 |
| --- | --- |
| 来源 | `org.springframework:spring-web:7.1.0` |
| 路径 | `org/springframework/web/method/annotation/AbstractNamedValueMethodArgumentResolver.java` |
| 行数 | 359 |
| 在线地址 | https://repo1.maven.org/maven2/org/springframework/spring-web/7.1.0/spring-web-7.1.0-sources.jar |
| 抓取时间 | 2026-11-25T14:01:21.301Z |

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
   17 | package org.springframework.web.method.annotation;
   18 | 
   19 | import java.lang.reflect.Constructor;
   20 | import java.lang.reflect.Method;
   21 | import java.util.Map;
   22 | import java.util.concurrent.ConcurrentHashMap;
   23 | 
   24 | import jakarta.servlet.ServletException;
   25 | import kotlin.reflect.KFunction;
   26 | import kotlin.reflect.KParameter;
   27 | import kotlin.reflect.jvm.ReflectJvmMapping;
   28 | import org.jspecify.annotations.Nullable;
   29 | 
   30 | import org.springframework.beans.BeanUtils;
   31 | import org.springframework.beans.ConversionNotSupportedException;
   32 | import org.springframework.beans.TypeMismatchException;
   33 | import org.springframework.beans.factory.config.BeanExpressionContext;
   34 | import org.springframework.beans.factory.config.BeanExpressionResolver;
   35 | import org.springframework.beans.factory.config.ConfigurableBeanFactory;
   36 | import org.springframework.core.KotlinDetector;
   37 | import org.springframework.core.MethodParameter;
   38 | import org.springframework.web.bind.ServletRequestBindingException;
   39 | import org.springframework.web.bind.WebDataBinder;
   40 | import org.springframework.web.bind.annotation.ValueConstants;
   41 | import org.springframework.web.bind.support.WebDataBinderFactory;
   42 | import org.springframework.web.context.request.NativeWebRequest;
   43 | import org.springframework.web.context.request.RequestScope;
   44 | import org.springframework.web.method.support.HandlerMethodArgumentResolver;
   45 | import org.springframework.web.method.support.ModelAndViewContainer;
   46 | 
   47 | /**
   48 |  * Abstract base class for resolving method arguments from a named value.
   49 |  * Request parameters, request headers, and path variables are examples of named
   50 |  * values. Each may have a name, a required flag, and a default value.
   51 |  *
   52 |  * <p>Subclasses define how to do the following:
   53 |  * <ul>
   54 |  * <li>Obtain named value information for a method parameter
   55 |  * <li>Resolve names into argument values
   56 |  * <li>Handle missing argument values when argument values are required
   57 |  * <li>Optionally handle a resolved value
   58 |  * </ul>
   59 |  *
   60 |  * <p>A default value string can contain ${...} placeholders and Spring Expression
   61 |  * Language #{...} expressions. For this to work a
   62 |  * {@link ConfigurableBeanFactory} must be supplied to the class constructor.
   63 |  *
   64 |  * <p>A {@link WebDataBinder} is created to apply type conversion to the resolved
   65 |  * argument value if it doesn't match the method parameter type.
   66 |  *
   67 |  * @author Arjen Poutsma
   68 |  * @author Rossen Stoyanchev
   69 |  * @author Juergen Hoeller
   70 |  * @author Sebastien Deleuze
   71 |  * @since 3.1
   72 |  */
   73 | public abstract class AbstractNamedValueMethodArgumentResolver implements HandlerMethodArgumentResolver {
   74 | 
   75 | 	private static final boolean KOTLIN_REFLECT_PRESENT = KotlinDetector.isKotlinReflectPresent();
   76 | 
   77 | 	private final @Nullable ConfigurableBeanFactory configurableBeanFactory;
   78 | 
   79 | 	private final @Nullable BeanExpressionContext expressionContext;
   80 | 
   81 | 	private final Map<MethodParameter, NamedValueInfo> namedValueInfoCache = new ConcurrentHashMap<>(256);
   82 | 
   83 | 
   84 | 	public AbstractNamedValueMethodArgumentResolver() {
   85 | 		this.configurableBeanFactory = null;
   86 | 		this.expressionContext = null;
   87 | 	}
   88 | 
   89 | 	/**
   90 | 	 * Create a new {@link AbstractNamedValueMethodArgumentResolver} instance.
   91 | 	 * @param beanFactory a bean factory to use for resolving ${...} placeholder
   92 | 	 * and #{...} SpEL expressions in default values, or {@code null} if default
   93 | 	 * values are not expected to contain expressions
   94 | 	 */
   95 | 	public AbstractNamedValueMethodArgumentResolver(@Nullable ConfigurableBeanFactory beanFactory) {
   96 | 		this.configurableBeanFactory = beanFactory;
   97 | 		this.expressionContext =
   98 | 				(beanFactory != null ? new BeanExpressionContext(beanFactory, new RequestScope()) : null);
   99 | 	}
  100 | 
  101 | 
  102 | 	@Override
  103 | 	public final @Nullable Object resolveArgument(MethodParameter parameter, @Nullable ModelAndViewContainer mavContainer,
  104 | 			NativeWebRequest webRequest, @Nullable WebDataBinderFactory binderFactory) throws Exception {
  105 | 
  106 | 		NamedValueInfo namedValueInfo = getNamedValueInfo(parameter);
  107 | 		MethodParameter nestedParameter = parameter.nestedIfOptional();
  108 | 		boolean hasDefaultValue = KOTLIN_REFLECT_PRESENT && KotlinDetector.isKotlinType(parameter.getDeclaringClass()) &&
  109 | 				KotlinDelegate.hasDefaultValue(nestedParameter);
  110 | 
  111 | 		Object resolvedName = resolveEmbeddedValuesAndExpressions(namedValueInfo.name);
  112 | 		if (resolvedName == null) {
  113 | 			throw new IllegalArgumentException(
  114 | 					"Specified name must not resolve to null: [" + namedValueInfo.name + "]");
  115 | 		}
  116 | 
  117 | 		Object arg = resolveName(resolvedName.toString(), nestedParameter, webRequest);
  118 | 		if (arg == null) {
  119 | 			if (namedValueInfo.defaultValue != null) {
  120 | 				arg = resolveEmbeddedValuesAndExpressions(namedValueInfo.defaultValue);
  121 | 			}
  122 | 			else if (namedValueInfo.required && !nestedParameter.isOptional()) {
  123 | 				handleMissingValue(resolvedName.toString(), nestedParameter, webRequest);
  124 | 			}
  125 | 			if (!hasDefaultValue) {
  126 | 				arg = handleNullValue(resolvedName.toString(), arg, nestedParameter.getNestedParameterType());
  127 | 			}
  128 | 		}
  129 | 		else if ("".equals(arg) && namedValueInfo.defaultValue != null) {
  130 | 			arg = resolveEmbeddedValuesAndExpressions(namedValueInfo.defaultValue);
  131 | 		}
  132 | 
  133 | 		if (binderFactory != null && (arg != null || !hasDefaultValue)) {
  134 | 			arg = convertIfNecessary(parameter, webRequest, binderFactory, namedValueInfo, arg);
  135 | 			// Check for null value after conversion of incoming argument value
  136 | 			if (arg == null) {
  137 | 				if (namedValueInfo.defaultValue != null) {
  138 | 					arg = resolveEmbeddedValuesAndExpressions(namedValueInfo.defaultValue);
  139 | 					arg = convertIfNecessary(parameter, webRequest, binderFactory, namedValueInfo, arg);
  140 | 				}
  141 | 				else if (namedValueInfo.required && !nestedParameter.isOptional()) {
  142 | 					handleMissingValueAfterConversion(resolvedName.toString(), nestedParameter, webRequest);
  143 | 				}
  144 | 			}
  145 | 		}
  146 | 
  147 | 		handleResolvedValue(arg, namedValueInfo.name, parameter, mavContainer, webRequest);
  148 | 
  149 | 		return arg;
  150 | 	}
  151 | 
  152 | 	/**
  153 | 	 * Obtain the named value for the given method parameter.
  154 | 	 */
  155 | 	private NamedValueInfo getNamedValueInfo(MethodParameter parameter) {
  156 | 		NamedValueInfo namedValueInfo = this.namedValueInfoCache.get(parameter);
  157 | 		if (namedValueInfo == null) {
  158 | 			namedValueInfo = createNamedValueInfo(parameter);
  159 | 			namedValueInfo = updateNamedValueInfo(parameter, namedValueInfo);
  160 | 			this.namedValueInfoCache.put(parameter, namedValueInfo);
  161 | 		}
  162 | 		return namedValueInfo;
  163 | 	}
  164 | 
  165 | 	/**
  166 | 	 * Create the {@link NamedValueInfo} object for the given method parameter. Implementations typically
  167 | 	 * retrieve the method annotation by means of {@link MethodParameter#getParameterAnnotation(Class)}.
  168 | 	 * @param parameter the method parameter
  169 | 	 * @return the named value information
  170 | 	 */
  171 | 	protected abstract NamedValueInfo createNamedValueInfo(MethodParameter parameter);
  172 | 
  173 | 	/**
  174 | 	 * Create a new NamedValueInfo based on the given NamedValueInfo with sanitized values.
  175 | 	 */
  176 | 	private NamedValueInfo updateNamedValueInfo(MethodParameter parameter, NamedValueInfo info) {
  177 | 		String name = info.name;
  178 | 		if (info.name.isEmpty()) {
  179 | 			name = parameter.getParameterName();
  180 | 			if (name == null) {
  181 | 				throw new IllegalArgumentException("""
  182 | 						Name for argument of type [%s] not specified, and parameter name information not \
  183 | 						available via reflection. Ensure that the compiler uses the '-parameters' flag."""
  184 | 							.formatted(parameter.getNestedParameterType().getName()));
  185 | 			}
  186 | 		}
  187 | 		String defaultValue = (ValueConstants.DEFAULT_NONE.equals(info.defaultValue) ? null : info.defaultValue);
  188 | 		return new NamedValueInfo(name, info.required, defaultValue);
  189 | 	}
  190 | 
  191 | 	/**
  192 | 	 * Resolve the given annotation-specified value,
  193 | 	 * potentially containing placeholders and expressions.
  194 | 	 */
  195 | 	private @Nullable Object resolveEmbeddedValuesAndExpressions(String value) {
  196 | 		if (this.configurableBeanFactory == null || this.expressionContext == null) {
  197 | 			return value;
  198 | 		}
  199 | 		String placeholdersResolved = this.configurableBeanFactory.resolveEmbeddedValue(value);
  200 | 		BeanExpressionResolver exprResolver = this.configurableBeanFactory.getBeanExpressionResolver();
  201 | 		if (exprResolver == null) {
  202 | 			return value;
  203 | 		}
  204 | 		return exprResolver.evaluate(placeholdersResolved, this.expressionContext);
  205 | 	}
  206 | 
  207 | 	/**
  208 | 	 * Resolve the given parameter type and value name into an argument value.
  209 | 	 * @param name the name of the value being resolved
  210 | 	 * @param parameter the method parameter to resolve to an argument value
  211 | 	 * (pre-nested in case of a {@link java.util.Optional} declaration)
  212 | 	 * @param request the current request
  213 | 	 * @return the resolved argument (may be {@code null})
  214 | 	 * @throws Exception in case of errors
  215 | 	 */
  216 | 	protected abstract @Nullable Object resolveName(String name, MethodParameter parameter, NativeWebRequest request)
  217 | 			throws Exception;
  218 | 
  219 | 	/**
  220 | 	 * Invoked when a named value is required, but {@link #resolveName(String, MethodParameter, NativeWebRequest)}
  221 | 	 * returned {@code null} and there is no default value. Subclasses typically throw an exception in this case.
  222 | 	 * @param name the name for the value
  223 | 	 * @param parameter the method parameter
  224 | 	 * @param request the current request
  225 | 	 * @since 4.3
  226 | 	 */
  227 | 	protected void handleMissingValue(String name, MethodParameter parameter, NativeWebRequest request)
  228 | 			throws Exception {
  229 | 
  230 | 		handleMissingValue(name, parameter);
  231 | 	}
  232 | 
  233 | 	/**
  234 | 	 * Invoked when a named value is required, but {@link #resolveName(String, MethodParameter, NativeWebRequest)}
  235 | 	 * returned {@code null} and there is no default value. Subclasses typically throw an exception in this case.
  236 | 	 * @param name the name for the value
  237 | 	 * @param parameter the method parameter
  238 | 	 */
  239 | 	protected void handleMissingValue(String name, MethodParameter parameter) throws ServletException {
  240 | 		throw new ServletRequestBindingException("Missing argument '" + name +
  241 | 				"' for method parameter of type " + parameter.getNestedParameterType().getSimpleName());
  242 | 	}
  243 | 
  244 | 	/**
  245 | 	 * Invoked when a named value is present but becomes {@code null} after conversion.
  246 | 	 * @param name the name for the value
  247 | 	 * @param parameter the method parameter
  248 | 	 * @param request the current request
  249 | 	 * @since 5.3.6
  250 | 	 */
  251 | 	protected void handleMissingValueAfterConversion(String name, MethodParameter parameter, NativeWebRequest request)
  252 | 			throws Exception {
  253 | 
  254 | 		handleMissingValue(name, parameter, request);
  255 | 	}
  256 | 
  257 | 	/**
  258 | 	 * A {@code null} results in a {@code false} value for {@code boolean}s or an exception for other primitives.
  259 | 	 */
  260 | 	private @Nullable Object handleNullValue(String name, @Nullable Object value, Class<?> paramType) {
  261 | 		if (value == null) {
  262 | 			if (paramType == boolean.class) {
  263 | 				return Boolean.FALSE;
  264 | 			}
  265 | 			else if (paramType.isPrimitive()) {
  266 | 				throw new IllegalStateException("Optional " + paramType.getSimpleName() + " parameter '" + name +
  267 | 						"' is present but cannot be translated into a null value due to being declared as a " +
  268 | 						"primitive type. Consider declaring it as object wrapper for the corresponding primitive type.");
  269 | 			}
  270 | 		}
  271 | 		return value;
  272 | 	}
  273 | 
  274 | 	private static @Nullable Object convertIfNecessary(
  275 | 			MethodParameter parameter, NativeWebRequest webRequest, WebDataBinderFactory binderFactory,
  276 | 			NamedValueInfo namedValueInfo, @Nullable Object arg) throws Exception {
  277 | 
  278 | 		WebDataBinder binder = binderFactory.createBinder(webRequest, null, namedValueInfo.name);
  279 | 		Class<?> parameterType = parameter.getParameterType();
  280 | 		if (KotlinDetector.isInlineClass(parameterType)) {
  281 | 			Constructor<?> ctor = BeanUtils.findPrimaryConstructor(parameterType);
  282 | 			if (ctor != null) {
  283 | 				parameterType = ctor.getParameterTypes()[0];
  284 | 			}
  285 | 		}
  286 | 		try {
  287 | 			arg = binder.convertIfNecessary(arg, parameterType, parameter);
  288 | 		}
  289 | 		catch (ConversionNotSupportedException ex) {
  290 | 			throw new MethodArgumentConversionNotSupportedException(arg, ex.getRequiredType(),
  291 | 					namedValueInfo.name, parameter, ex.getCause());
  292 | 		}
  293 | 		catch (TypeMismatchException ex) {
  294 | 			throw new MethodArgumentTypeMismatchException(arg, ex.getRequiredType(),
  295 | 					namedValueInfo.name, parameter, ex.getCause());
  296 | 		}
  297 | 		return arg;
  298 | 	}
  299 | 
  300 | 	/**
  301 | 	 * Invoked after a value is resolved.
  302 | 	 * @param arg the resolved argument value
  303 | 	 * @param name the argument name
  304 | 	 * @param parameter the argument parameter type
  305 | 	 * @param mavContainer the {@link ModelAndViewContainer} (may be {@code null})
  306 | 	 * @param webRequest the current request
  307 | 	 */
  308 | 	protected void handleResolvedValue(@Nullable Object arg, String name, MethodParameter parameter,
  309 | 			@Nullable ModelAndViewContainer mavContainer, NativeWebRequest webRequest) {
  310 | 	}
  311 | 
  312 | 
  313 | 	/**
  314 | 	 * Represents the information about a named value, including name, whether it's required and a default value.
  315 | 	 */
  316 | 	protected static class NamedValueInfo {
  317 | 
  318 | 		private final String name;
  319 | 
  320 | 		private final boolean required;
  321 | 
  322 | 		private final @Nullable String defaultValue;
  323 | 
  324 | 		public NamedValueInfo(String name, boolean required, @Nullable String defaultValue) {
  325 | 			this.name = name;
  326 | 			this.required = required;
  327 | 			this.defaultValue = defaultValue;
  328 | 		}
  329 | 	}
  330 | 
  331 | 
  332 | 	/**
  333 | 	 * Inner class to avoid a hard dependency on Kotlin at runtime.
  334 | 	 */
  335 | 	private static class KotlinDelegate {
  336 | 
  337 | 		/**
  338 | 		 * Check whether the specified {@link MethodParameter} represents a nullable Kotlin type
  339 | 		 * or an optional parameter (with a default value in the Kotlin declaration).
  340 | 		 */
  341 | 		public static boolean hasDefaultValue(MethodParameter parameter) {
  342 | 			Method method = parameter.getMethod();
  343 | 			if (method == null) {
  344 | 				return false;
  345 | 			}
  346 | 			KFunction<?> function = ReflectJvmMapping.getKotlinFunction(method);
  347 | 			if (function != null) {
  348 | 				int index = 0;
  349 | 				for (KParameter kParameter : function.getParameters()) {
  350 | 					if (KParameter.Kind.VALUE.equals(kParameter.getKind()) && parameter.getParameterIndex() == index++) {
  351 | 						return kParameter.isOptional();
  352 | 					}
  353 | 				}
  354 | 			}
  355 | 			return false;
  356 | 		}
  357 | 	}
  358 | 
  359 | }
```
