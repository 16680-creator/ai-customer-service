<!-- learning-docs source archive（机械抓取原文，禁止手改代码内容） -->
<!-- origin: org.springframework.boot:spring-boot:4.2.0 | path: org/springframework/boot/context/properties/bind/ValueObjectBinder.java -->
<!-- 正文引用：[[源码:ValueObjectBinder.java#L起-L止]] ；代码块头部：(src: ValueObjectBinder.java#L起-L止) -->

# ValueObjectBinder.java

| 项 | 值 |
| --- | --- |
| 来源 | `org.springframework.boot:spring-boot:4.2.0` |
| 路径 | `org/springframework/boot/context/properties/bind/ValueObjectBinder.java` |
| 行数 | 445 |
| 在线地址 | https://repo1.maven.org/maven2/org/springframework/boot/spring-boot/4.2.0/spring-boot-4.2.0-sources.jar |
| 抓取时间 | 2026-11-25T14:04:53.359Z |

```java
    1 | /*
    2 |  * Copyright 2012-present the original author or authors.
    3 |  */
    4 | 
    5 | package org.springframework.boot.context.properties.bind;
    6 | 
    7 | import java.lang.annotation.Annotation;
    8 | import java.lang.reflect.Array;
    9 | import java.lang.reflect.Constructor;
   10 | import java.lang.reflect.Method;
   11 | import java.lang.reflect.Modifier;
   12 | import java.lang.reflect.Parameter;
   13 | import java.util.ArrayList;
   14 | import java.util.Collection;
   15 | import java.util.Collections;
   16 | import java.util.EnumMap;
   17 | import java.util.List;
   18 | import java.util.Map;
   19 | import java.util.Optional;
   20 | import java.util.concurrent.ConcurrentHashMap;
   21 | import java.util.function.Consumer;
   22 | 
   23 | import kotlin.reflect.KFunction;
   24 | import kotlin.reflect.KParameter;
   25 | import kotlin.reflect.jvm.ReflectJvmMapping;
   26 | import org.apache.commons.logging.Log;
   27 | import org.apache.commons.logging.LogFactory;
   28 | import org.jspecify.annotations.Nullable;
   29 | 
   30 | import org.springframework.beans.BeanUtils;
   31 | import org.springframework.boot.context.properties.bind.Binder.Context;
   32 | import org.springframework.boot.context.properties.source.ConfigurationPropertyName;
   33 | import org.springframework.core.CollectionFactory;
   34 | import org.springframework.core.DefaultParameterNameDiscoverer;
   35 | import org.springframework.core.KotlinDetector;
   36 | import org.springframework.core.MethodParameter;
   37 | import org.springframework.core.ParameterNameDiscoverer;
   38 | import org.springframework.core.ResolvableType;
   39 | import org.springframework.core.annotation.MergedAnnotation;
   40 | import org.springframework.core.annotation.MergedAnnotations;
   41 | import org.springframework.core.convert.ConversionException;
   42 | import org.springframework.core.log.LogMessage;
   43 | import org.springframework.util.Assert;
   44 | 
   45 | /**
   46 |  * {@link DataObjectBinder} for immutable value objects.
   47 |  *
   48 |  * @author Madhura Bhave
   49 |  * @author Stephane Nicoll
   50 |  * @author Phillip Webb
   51 |  * @author Scott Frederick
   52 |  * @author Ondřej Světlík
   53 |  * @author Wan bin yu
   54 |  */
   55 | class ValueObjectBinder implements DataObjectBinder {
   56 | 
   57 | 	private static final Log logger = LogFactory.getLog(ValueObjectBinder.class);
   58 | 
   59 | 	private final BindConstructorProvider constructorProvider;
   60 | 
   61 | 	ValueObjectBinder(BindConstructorProvider constructorProvider) {
   62 | 		this.constructorProvider = constructorProvider;
   63 | 	}
   64 | 
   65 | 	@Override
   66 | 	public <T> @Nullable T bind(ConfigurationPropertyName name, Bindable<T> target, Binder.Context context,
   67 | 			DataObjectPropertyBinder propertyBinder, boolean fallbackToDefaultValue) {
   68 | 		ValueObject<T> valueObject = ValueObject.get(target, context, this.constructorProvider, Discoverer.LENIENT);
   69 | 		if (valueObject != null) {
   70 | 			Class<?> targetType = target.getType().resolve();
   71 | 			Assert.state(targetType != null, "'targetType' must not be null");
   72 | 			context.pushConstructorBoundTypes(targetType);
   73 | 			List<ConstructorParameter> parameters = valueObject.getConstructorParameters();
   74 | 			List<@Nullable Object> args = new ArrayList<>(parameters.size());
   75 | 			boolean bound = false;
   76 | 			for (ConstructorParameter parameter : parameters) {
   77 | 				Object arg = parameter.bind(propertyBinder);
   78 | 				bound = bound || arg != null;
   79 | 				arg = (arg != null) ? arg : getDefaultValue(context, parameter);
   80 | 				args.add(arg);
   81 | 			}
   82 | 			context.clearConfigurationProperty();
   83 | 			context.popConstructorBoundTypes();
   84 | 			if (bound) {
   85 | 				return valueObject.instantiate(args);
   86 | 			}
   87 | 		}
   88 | 		if (fallbackToDefaultValue) {
   89 | 			return getNewDefaultValueInstanceIfPossible(context, target.getType());
   90 | 		}
   91 | 		return null;
   92 | 	}
   93 | 
   94 | 	@Override
   95 | 	public <T> @Nullable T create(Bindable<T> target, Binder.Context context) {
   96 | 		ValueObject<T> valueObject = ValueObject.get(target, context, this.constructorProvider, Discoverer.LENIENT);
   97 | 		if (valueObject == null) {
   98 | 			return null;
   99 | 		}
  100 | 		List<ConstructorParameter> parameters = valueObject.getConstructorParameters();
  101 | 		List<@Nullable Object> args = new ArrayList<>(parameters.size());
  102 | 		for (ConstructorParameter parameter : parameters) {
  103 | 			args.add(getDefaultValue(context, parameter));
  104 | 		}
  105 | 		return valueObject.instantiate(args);
  106 | 	}
  107 | 
  108 | 	@Override
  109 | 	public <T> void onUnableToCreateInstance(Bindable<T> target, Context context, RuntimeException exception) {
  110 | 		try {
  111 | 			ValueObject.get(target, context, this.constructorProvider, Discoverer.STRICT);
  112 | 		}
  113 | 		catch (Exception ex) {
  114 | 			exception.addSuppressed(ex);
  115 | 		}
  116 | 	}
  117 | 
  118 | 	private <T> @Nullable T getDefaultValue(Binder.Context context, ConstructorParameter parameter) {
  119 | 		ResolvableType type = parameter.getType();
  120 | 		Annotation[] annotations = parameter.getAnnotations();
  121 | 		for (Annotation annotation : annotations) {
  122 | 			if (annotation instanceof DefaultValue defaultValueAnnotation) {
  123 | 				String[] defaultValue = defaultValueAnnotation.value();
  124 | 				if (defaultValue.length == 0) {
  125 | 					return getNewDefaultValueInstanceIfPossible(context, type);
  126 | 				}
  127 | 				return convertDefaultValue(context.getConverter(),
  128 | 						resolveDefaultValue(context.getPlaceholdersResolver(), defaultValue), type, annotations);
  129 | 			}
  130 | 		}
  131 | 		return context.getConverter().convert(null, type);
  132 | 	}
  133 | 
  134 | 	private String[] resolveDefaultValue(PlaceholdersResolver resolver, String[] defaultValue) {
  135 | 		String[] resolved = new String[defaultValue.length];
  136 | 		for (int i = 0; i < defaultValue.length; i++) {
  137 | 			Object value = resolver.resolvePlaceholders(defaultValue[i]);
  138 | 			resolved[i] = String.valueOf(value);
  139 | 		}
  140 | 		return resolved;
  141 | 	}
  142 | 
  143 | 	private <T> @Nullable T convertDefaultValue(BindConverter converter, String[] defaultValue, ResolvableType type,
  144 | 			Annotation[] annotations) {
  145 | 		try {
  146 | 			return converter.convert(defaultValue, type, annotations);
  147 | 		}
  148 | 		catch (ConversionException ex) {
  149 | 			// Try again in case ArrayToObjectConverter is not in play
  150 | 			if (defaultValue.length == 1) {
  151 | 				return converter.convert(defaultValue[0], type, annotations);
  152 | 			}
  153 | 			throw ex;
  154 | 		}
  155 | 	}
  156 | 
  157 | 	@SuppressWarnings("unchecked")
  158 | 	private <T> @Nullable T getNewDefaultValueInstanceIfPossible(Binder.Context context, ResolvableType type) {
  159 | 		Class<T> resolved = (Class<T>) type.resolve();
  160 | 		Assert.state(resolved == null || isEmptyDefaultValueAllowed(resolved),
  161 | 				() -> "Parameter of type " + type + " must have a non-empty default value.");
  162 | 		if (resolved != null) {
  163 | 			if (Optional.class == resolved) {
  164 | 				return (T) Optional.empty();
  165 | 			}
  166 | 			if (Collection.class.isAssignableFrom(resolved)) {
  167 | 				return (T) CollectionFactory.createCollection(resolved, 0);
  168 | 			}
  169 | 			if (EnumMap.class.isAssignableFrom(resolved)) {
  170 | 				Class<?> keyType = type.asMap().resolveGeneric(0);
  171 | 				return (T) CollectionFactory.createMap(resolved, keyType, 0);
  172 | 			}
  173 | 			if (Map.class.isAssignableFrom(resolved)) {
  174 | 				return (T) CollectionFactory.createMap(resolved, 0);
  175 | 			}
  176 | 			if (resolved.isArray()) {
  177 | 				return (T) Array.newInstance(resolved.getComponentType(), 0);
  178 | 			}
  179 | 		}
  180 | 		T instance = create(Bindable.of(type), context);
  181 | 		if (instance != null) {
  182 | 			return instance;
  183 | 		}
  184 | 		return (resolved != null) ? BeanUtils.instantiateClass(resolved) : null;
  185 | 	}
  186 | 
  187 | 	private boolean isEmptyDefaultValueAllowed(Class<?> type) {
  188 | 		return (Optional.class == type || isAggregate(type))
  189 | 				|| !(type.isPrimitive() || type.isEnum() || type.getName().startsWith("java.lang"));
  190 | 	}
  191 | 
  192 | 	private boolean isAggregate(Class<?> type) {
  193 | 		return type.isArray() || Map.class.isAssignableFrom(type) || Collection.class.isAssignableFrom(type);
  194 | 	}
  195 | 
  196 | 	/**
  197 | 	 * The value object being bound.
  198 | 	 *
  199 | 	 * @param <T> the value object type
  200 | 	 */
  201 | 	private abstract static class ValueObject<T> {
  202 | 
  203 | 		private static final Object NONE = new Object();
  204 | 
  205 | 		private final Constructor<T> constructor;
  206 | 
  207 | 		protected ValueObject(Constructor<T> constructor) {
  208 | 			this.constructor = constructor;
  209 | 		}
  210 | 
  211 | 		T instantiate(List<@Nullable Object> args) {
  212 | 			return BeanUtils.instantiateClass(this.constructor, args.toArray());
  213 | 		}
  214 | 
  215 | 		abstract List<ConstructorParameter> getConstructorParameters();
  216 | 
  217 | 		@SuppressWarnings("unchecked")
  218 | 		static <T> @Nullable ValueObject<T> get(Bindable<T> bindable, Binder.Context context,
  219 | 				BindConstructorProvider constructorProvider, ParameterNameDiscoverer parameterNameDiscoverer) {
  220 | 			Class<T> resolvedType = (Class<T>) bindable.getType().resolve();
  221 | 			if (resolvedType == null || resolvedType.isEnum() || Modifier.isAbstract(resolvedType.getModifiers())) {
  222 | 				return null;
  223 | 			}
  224 | 			Map<CacheKey, Object> cache = getCache(context);
  225 | 			CacheKey cacheKey = new CacheKey(bindable, constructorProvider, parameterNameDiscoverer);
  226 | 			Object valueObject = cache.get(cacheKey);
  227 | 			if (valueObject == null) {
  228 | 				valueObject = get(bindable, context, constructorProvider, parameterNameDiscoverer, resolvedType);
  229 | 				cache.put(cacheKey, (valueObject != null) ? valueObject : NONE);
  230 | 			}
  231 | 			return (valueObject != NONE) ? (ValueObject<T>) valueObject : null;
  232 | 		}
  233 | 
  234 | 		@SuppressWarnings("unchecked")
  235 | 		private static <T> @Nullable ValueObject<T> get(Bindable<T> bindable, Binder.Context context,
  236 | 				BindConstructorProvider constructorProvider, ParameterNameDiscoverer parameterNameDiscoverer,
  237 | 				Class<T> resolvedType) {
  238 | 			Constructor<?> bindConstructor = constructorProvider.getBindConstructor(bindable,
  239 | 					context.isNestedConstructorBinding());
  240 | 			if (bindConstructor == null) {
  241 | 				return null;
  242 | 			}
  243 | 			if (KotlinDetector.isKotlinType(resolvedType)) {
  244 | 				return KotlinValueObject.get((Constructor<T>) bindConstructor, bindable.getType(),
  245 | 						parameterNameDiscoverer);
  246 | 			}
  247 | 			return DefaultValueObject.get(bindConstructor, bindable.getType(), parameterNameDiscoverer);
  248 | 		}
  249 | 
  250 | 		@SuppressWarnings("unchecked")
  251 | 		private static Map<CacheKey, Object> getCache(Context context) {
  252 | 			Map<CacheKey, Object> cache = (Map<CacheKey, Object>) context.getCache().get(ValueObject.class);
  253 | 			if (cache == null) {
  254 | 				cache = new ConcurrentHashMap<>();
  255 | 				context.getCache().put(ValueObject.class, cache);
  256 | 			}
  257 | 			return cache;
  258 | 		}
  259 | 
  260 | 		private record CacheKey(Bindable<?> bindable, BindConstructorProvider constructorProvider,
  261 | 				ParameterNameDiscoverer parameterNameDiscoverer) {
  262 | 
  263 | 		}
  264 | 
  265 | 	}
  266 | 
  267 | 	/**
  268 | 	 * A {@link ValueObject} implementation that is aware of Kotlin specific constructs.
  269 | 	 */
  270 | 	private static final class KotlinValueObject<T> extends ValueObject<T> {
  271 | 
  272 | 		private static final Annotation[] ANNOTATION_ARRAY = new Annotation[0];
  273 | 
  274 | 		private final List<ConstructorParameter> constructorParameters;
  275 | 
  276 | 		private KotlinValueObject(Constructor<T> primaryConstructor, KFunction<T> kotlinConstructor,
  277 | 				ResolvableType type) {
  278 | 			super(primaryConstructor);
  279 | 			this.constructorParameters = parseConstructorParameters(kotlinConstructor, type);
  280 | 		}
  281 | 
  282 | 		private List<ConstructorParameter> parseConstructorParameters(KFunction<T> kotlinConstructor,
  283 | 				ResolvableType type) {
  284 | 			List<KParameter> parameters = kotlinConstructor.getParameters();
  285 | 			List<ConstructorParameter> result = new ArrayList<>(parameters.size());
  286 | 			for (KParameter parameter : parameters) {
  287 | 				String name = getParameterName(parameter);
  288 | 				ResolvableType parameterType = ResolvableType
  289 | 					.forType(ReflectJvmMapping.getJavaType(parameter.getType()), type);
  290 | 				Annotation[] annotations = parameter.getAnnotations().toArray(ANNOTATION_ARRAY);
  291 | 				Assert.state(name != null, "'name' must not be null");
  292 | 				result.add(new ConstructorParameter(name, parameterType, annotations));
  293 | 			}
  294 | 			return Collections.unmodifiableList(result);
  295 | 		}
  296 | 
  297 | 		private @Nullable String getParameterName(KParameter parameter) {
  298 | 			return MergedAnnotations.from(parameter, parameter.getAnnotations().toArray(ANNOTATION_ARRAY))
  299 | 				.get(Name.class)
  300 | 				.getValue(MergedAnnotation.VALUE, String.class)
  301 | 				.orElseGet(parameter::getName);
  302 | 		}
  303 | 
  304 | 		@Override
  305 | 		List<ConstructorParameter> getConstructorParameters() {
  306 | 			return this.constructorParameters;
  307 | 		}
  308 | 
  309 | 		static <T> @Nullable ValueObject<T> get(Constructor<T> bindConstructor, ResolvableType type,
  310 | 				ParameterNameDiscoverer parameterNameDiscoverer) {
  311 | 			KFunction<T> kotlinConstructor = ReflectJvmMapping.getKotlinFunction(bindConstructor);
  312 | 			if (kotlinConstructor != null) {
  313 | 				return new KotlinValueObject<>(bindConstructor, kotlinConstructor, type);
  314 | 			}
  315 | 			return DefaultValueObject.get(bindConstructor, type, parameterNameDiscoverer);
  316 | 		}
  317 | 
  318 | 	}
  319 | 
  320 | 	/**
  321 | 	 * A default {@link ValueObject} implementation that uses only standard Java
  322 | 	 * reflection calls.
  323 | 	 */
  324 | 	private static final class DefaultValueObject<T> extends ValueObject<T> {
  325 | 
  326 | 		private final List<ConstructorParameter> constructorParameters;
  327 | 
  328 | 		private DefaultValueObject(Constructor<T> constructor, List<ConstructorParameter> constructorParameters) {
  329 | 			super(constructor);
  330 | 			this.constructorParameters = constructorParameters;
  331 | 		}
  332 | 
  333 | 		@Override
  334 | 		List<ConstructorParameter> getConstructorParameters() {
  335 | 			return this.constructorParameters;
  336 | 		}
  337 | 
  338 | 		@SuppressWarnings("unchecked")
  339 | 		static <T> @Nullable ValueObject<T> get(Constructor<?> bindConstructor, ResolvableType type,
  340 | 				ParameterNameDiscoverer parameterNameDiscoverer) {
  341 | 			@Nullable String[] names = parameterNameDiscoverer.getParameterNames(bindConstructor);
  342 | 			if (names == null) {
  343 | 				return null;
  344 | 			}
  345 | 			List<ConstructorParameter> constructorParameters = parseConstructorParameters(bindConstructor, type, names);
  346 | 			return new DefaultValueObject<>((Constructor<T>) bindConstructor, constructorParameters);
  347 | 		}
  348 | 
  349 | 		private static List<ConstructorParameter> parseConstructorParameters(Constructor<?> constructor,
  350 | 				ResolvableType type, @Nullable String[] names) {
  351 | 			Parameter[] parameters = constructor.getParameters();
  352 | 			List<ConstructorParameter> result = new ArrayList<>(parameters.length);
  353 | 			for (int i = 0; i < parameters.length; i++) {
  354 | 				String name = MergedAnnotations.from(parameters[i])
  355 | 					.get(Name.class)
  356 | 					.getValue(MergedAnnotation.VALUE, String.class)
  357 | 					.orElse(names[i]);
  358 | 				ResolvableType parameterType = ResolvableType.forMethodParameter(new MethodParameter(constructor, i),
  359 | 						type);
  360 | 				Annotation[] annotations = parameters[i].getDeclaredAnnotations();
  361 | 				Assert.state(name != null, "'name' must not be null");
  362 | 				result.add(new ConstructorParameter(name, parameterType, annotations));
  363 | 			}
  364 | 			return Collections.unmodifiableList(result);
  365 | 		}
  366 | 
  367 | 	}
  368 | 
  369 | 	/**
  370 | 	 * A constructor parameter being bound.
  371 | 	 */
  372 | 	private static class ConstructorParameter {
  373 | 
  374 | 		private final String name;
  375 | 
  376 | 		private final ResolvableType type;
  377 | 
  378 | 		private final Annotation[] annotations;
  379 | 
  380 | 		ConstructorParameter(String name, ResolvableType type, Annotation[] annotations) {
  381 | 			this.name = DataObjectPropertyName.toDashedForm(name);
  382 | 			this.type = type;
  383 | 			this.annotations = annotations;
  384 | 		}
  385 | 
  386 | 		@Nullable Object bind(DataObjectPropertyBinder propertyBinder) {
  387 | 			return propertyBinder.bindProperty(this.name, Bindable.of(this.type).withAnnotations(this.annotations));
  388 | 		}
  389 | 
  390 | 		Annotation[] getAnnotations() {
  391 | 			return this.annotations;
  392 | 		}
  393 | 
  394 | 		ResolvableType getType() {
  395 | 			return this.type;
  396 | 		}
  397 | 
  398 | 	}
  399 | 
  400 | 	/**
  401 | 	 * {@link ParameterNameDiscoverer} used for value data object binding.
  402 | 	 */
  403 | 	static final class Discoverer implements ParameterNameDiscoverer {
  404 | 
  405 | 		private static final ParameterNameDiscoverer DEFAULT_DELEGATE = new DefaultParameterNameDiscoverer();
  406 | 
  407 | 		private static final ParameterNameDiscoverer LENIENT = new Discoverer(DEFAULT_DELEGATE, (message) -> {
  408 | 		});
  409 | 
  410 | 		private static final ParameterNameDiscoverer STRICT = new Discoverer(DEFAULT_DELEGATE, (message) -> {
  411 | 			throw new IllegalStateException(message.toString());
  412 | 		});
  413 | 
  414 | 		private final ParameterNameDiscoverer delegate;
  415 | 
  416 | 		private final Consumer<LogMessage> noParameterNamesHandler;
  417 | 
  418 | 		private Discoverer(ParameterNameDiscoverer delegate, Consumer<LogMessage> noParameterNamesHandler) {
  419 | 			this.delegate = delegate;
  420 | 			this.noParameterNamesHandler = noParameterNamesHandler;
  421 | 		}
  422 | 
  423 | 		@Override
  424 | 		public String[] getParameterNames(Method method) {
  425 | 			throw new UnsupportedOperationException();
  426 | 		}
  427 | 
  428 | 		@Override
  429 | 		public @Nullable String @Nullable [] getParameterNames(Constructor<?> constructor) {
  430 | 			@Nullable String[] names = this.delegate.getParameterNames(constructor);
  431 | 			if (names != null) {
  432 | 				return names;
  433 | 			}
  434 | 			LogMessage message = LogMessage.format(
  435 | 					"Unable to use value object binding with constructor [%s] as parameter names cannot be discovered. "
  436 | 							+ "Ensure that the compiler uses the '-parameters' flag",
  437 | 					constructor);
  438 | 			this.noParameterNamesHandler.accept(message);
  439 | 			logger.debug(message);
  440 | 			return null;
  441 | 		}
  442 | 
  443 | 	}
  444 | 
  445 | }
```
