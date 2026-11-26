<!-- learning-docs source archive（机械抓取原文，禁止手改代码内容） -->
<!-- origin: org.springframework.boot:spring-boot:4.2.0 | path: org/springframework/boot/context/properties/bind/JavaBeanBinder.java -->
<!-- 正文引用：[[源码:JavaBeanBinder.java#L起-L止]] ；代码块头部：(src: JavaBeanBinder.java#L起-L止) -->

# JavaBeanBinder.java

| 项 | 值 |
| --- | --- |
| 来源 | `org.springframework.boot:spring-boot:4.2.0` |
| 路径 | `org/springframework/boot/context/properties/bind/JavaBeanBinder.java` |
| 行数 | 462 |
| 在线地址 | https://repo1.maven.org/maven2/org/springframework/boot/spring-boot/4.2.0/spring-boot-4.2.0-sources.jar |
| 抓取时间 | 2026-11-25T14:04:53.217Z |

```java
    1 | /*
    2 |  * Copyright 2012-present the original author or authors.
    3 |  */
    4 | 
    5 | package org.springframework.boot.context.properties.bind;
    6 | 
    7 | import java.beans.Introspector;
    8 | import java.lang.annotation.Annotation;
    9 | import java.lang.reflect.Field;
   10 | import java.lang.reflect.InvocationTargetException;
   11 | import java.lang.reflect.Method;
   12 | import java.lang.reflect.Modifier;
   13 | import java.util.Arrays;
   14 | import java.util.Comparator;
   15 | import java.util.LinkedHashMap;
   16 | import java.util.LinkedHashSet;
   17 | import java.util.Map;
   18 | import java.util.Set;
   19 | import java.util.concurrent.ConcurrentHashMap;
   20 | import java.util.function.BiConsumer;
   21 | import java.util.function.Function;
   22 | import java.util.function.Supplier;
   23 | 
   24 | import org.jspecify.annotations.Nullable;
   25 | 
   26 | import org.springframework.beans.BeanUtils;
   27 | import org.springframework.boot.context.properties.bind.Binder.Context;
   28 | import org.springframework.boot.context.properties.source.ConfigurationPropertyName;
   29 | import org.springframework.boot.context.properties.source.ConfigurationPropertySource;
   30 | import org.springframework.boot.context.properties.source.ConfigurationPropertyState;
   31 | import org.springframework.core.BridgeMethodResolver;
   32 | import org.springframework.core.MethodParameter;
   33 | import org.springframework.core.ResolvableType;
   34 | import org.springframework.util.Assert;
   35 | 
   36 | /**
   37 |  * {@link DataObjectBinder} for mutable Java Beans.
   38 |  *
   39 |  * @author Phillip Webb
   40 |  * @author Madhura Bhave
   41 |  * @author Lasse Wulff
   42 |  */
   43 | class JavaBeanBinder implements DataObjectBinder {
   44 | 
   45 | 	private static final String HAS_KNOWN_BINDABLE_PROPERTIES_CACHE = JavaBeanBinder.class.getName()
   46 | 			+ ".HAS_KNOWN_BINDABLE_PROPERTIES_CACHE";
   47 | 
   48 | 	static final JavaBeanBinder INSTANCE = new JavaBeanBinder();
   49 | 
   50 | 	@Override
   51 | 	public <T> @Nullable T bind(ConfigurationPropertyName name, Bindable<T> target, Context context,
   52 | 			DataObjectPropertyBinder propertyBinder, boolean fallbackToDefaultValue) {
   53 | 		boolean hasKnownBindableProperties = target.getValue() != null && hasKnownBindableProperties(name, context);
   54 | 		Bean<T> bean = Bean.get(target, context, hasKnownBindableProperties);
   55 | 		if (bean == null) {
   56 | 			return null;
   57 | 		}
   58 | 		BeanSupplier<T> beanSupplier = bean.getSupplier(target);
   59 | 		boolean bound = bind(propertyBinder, bean, beanSupplier, context);
   60 | 		return (bound ? beanSupplier.get() : null);
   61 | 	}
   62 | 
   63 | 	@Override
   64 | 	@SuppressWarnings("unchecked")
   65 | 	public <T> @Nullable T create(Bindable<T> target, Context context) {
   66 | 		Class<T> type = (Class<T>) target.getType().resolve();
   67 | 		return (type != null) ? BeanUtils.instantiateClass(type) : null;
   68 | 	}
   69 | 
   70 | 	private boolean hasKnownBindableProperties(ConfigurationPropertyName name, Context context) {
   71 | 		Map<ConfigurationPropertyName, Boolean> cache = getHasKnownBindablePropertiesCache(context);
   72 | 		Boolean hasKnownBindableProperties = cache.get(name);
   73 | 		if (hasKnownBindableProperties == null) {
   74 | 			hasKnownBindableProperties = computeHasKnownBindableProperties(name, context);
   75 | 			cache.put(name, hasKnownBindableProperties);
   76 | 		}
   77 | 		return hasKnownBindableProperties;
   78 | 	}
   79 | 
   80 | 	private boolean computeHasKnownBindableProperties(ConfigurationPropertyName name, Context context) {
   81 | 		for (ConfigurationPropertySource source : context.getSources()) {
   82 | 			if (source.containsDescendantOf(name) == ConfigurationPropertyState.PRESENT) {
   83 | 				return true;
   84 | 			}
   85 | 		}
   86 | 		return false;
   87 | 	}
   88 | 
   89 | 	@SuppressWarnings("unchecked")
   90 | 	private Map<ConfigurationPropertyName, Boolean> getHasKnownBindablePropertiesCache(Context context) {
   91 | 		Object cache = context.getCache().get(HAS_KNOWN_BINDABLE_PROPERTIES_CACHE);
   92 | 		if (cache == null) {
   93 | 			cache = new ConcurrentHashMap<ConfigurationPropertyName, Boolean>();
   94 | 			context.getCache().put(HAS_KNOWN_BINDABLE_PROPERTIES_CACHE, cache);
   95 | 		}
   96 | 		return (Map<ConfigurationPropertyName, Boolean>) cache;
   97 | 	}
   98 | 
   99 | 	private <T> boolean bind(DataObjectPropertyBinder propertyBinder, Bean<T> bean, BeanSupplier<T> beanSupplier,
  100 | 			Context context) {
  101 | 		boolean bound = false;
  102 | 		for (BeanProperty beanProperty : bean.getProperties().values()) {
  103 | 			bound |= bind(beanSupplier, propertyBinder, beanProperty);
  104 | 			context.clearConfigurationProperty();
  105 | 		}
  106 | 		return bound;
  107 | 	}
  108 | 
  109 | 	private <T> boolean bind(BeanSupplier<T> beanSupplier, DataObjectPropertyBinder propertyBinder,
  110 | 			BeanProperty property) {
  111 | 		String propertyName = determinePropertyName(property);
  112 | 		ResolvableType type = property.getType();
  113 | 		Supplier<Object> value = property.getValue(beanSupplier);
  114 | 		Annotation[] annotations = property.getAnnotations();
  115 | 		Object bound = propertyBinder.bindProperty(propertyName,
  116 | 				Bindable.of(type).withSuppliedValue(value).withAnnotations(annotations));
  117 | 		if (bound == null) {
  118 | 			return false;
  119 | 		}
  120 | 		if (property.isSettable()) {
  121 | 			property.setValue(beanSupplier, bound);
  122 | 		}
  123 | 		else if (value == null || !bound.equals(value.get())) {
  124 | 			throw new IllegalStateException("No setter found for property: " + property.getName());
  125 | 		}
  126 | 		return true;
  127 | 	}
  128 | 
  129 | 	private String determinePropertyName(BeanProperty property) {
  130 | 		return Arrays.stream((property.getAnnotations() != null) ? property.getAnnotations() : new Annotation[0])
  131 | 			.filter((annotation) -> annotation.annotationType() == Name.class)
  132 | 			.findFirst()
  133 | 			.map(Name.class::cast)
  134 | 			.map(Name::value)
  135 | 			.orElse(property.getName());
  136 | 	}
  137 | 
  138 | 	/**
  139 | 	 * The properties of a bean that may be bound.
  140 | 	 */
  141 | 	static class BeanProperties {
  142 | 
  143 | 		private final Map<String, BeanProperty> properties = new LinkedHashMap<>();
  144 | 
  145 | 		private final ResolvableType type;
  146 | 
  147 | 		private final Class<?> resolvedType;
  148 | 
  149 | 		BeanProperties(ResolvableType type, Class<?> resolvedType) {
  150 | 			this.type = type;
  151 | 			this.resolvedType = resolvedType;
  152 | 			addProperties(resolvedType);
  153 | 		}
  154 | 
  155 | 		private void addProperties(Class<?> type) {
  156 | 			while (type != null && !Object.class.equals(type)) {
  157 | 				Method[] declaredMethods = getSorted(type, this::getDeclaredMethods, Method::getName);
  158 | 				Field[] declaredFields = getSorted(type, Class::getDeclaredFields, Field::getName);
  159 | 				addProperties(declaredMethods, declaredFields);
  160 | 				type = type.getSuperclass();
  161 | 			}
  162 | 		}
  163 | 
  164 | 		private Method[] getDeclaredMethods(Class<?> type) {
  165 | 			Method[] methods = type.getDeclaredMethods();
  166 | 			Set<Method> result = new LinkedHashSet<>(methods.length);
  167 | 			for (Method method : methods) {
  168 | 				result.add(BridgeMethodResolver.findBridgedMethod(method));
  169 | 			}
  170 | 			return result.toArray(new Method[0]);
  171 | 		}
  172 | 
  173 | 		private <S, E> E[] getSorted(S source, Function<S, E[]> elements, Function<E, String> name) {
  174 | 			E[] result = elements.apply(source);
  175 | 			Arrays.sort(result, Comparator.comparing(name));
  176 | 			return result;
  177 | 		}
  178 | 
  179 | 		protected void addProperties(Method[] declaredMethods, Field[] declaredFields) {
  180 | 			@Nullable Method[] methods = new Method[declaredMethods.length];
  181 | 			for (int i = 0; i < declaredMethods.length; i++) {
  182 | 				methods[i] = isCandidate(declaredMethods[i]) ? declaredMethods[i] : null;
  183 | 			}
  184 | 			for (Method method : methods) {
  185 | 				addMethodIfPossible(method, "is", 0, BeanProperty::addGetter);
  186 | 			}
  187 | 			for (Method method : methods) {
  188 | 				addMethodIfPossible(method, "get", 0, BeanProperty::addGetter);
  189 | 			}
  190 | 			for (Method method : methods) {
  191 | 				addMethodIfPossible(method, "set", 1, BeanProperty::addSetter);
  192 | 			}
  193 | 			for (Field field : declaredFields) {
  194 | 				addField(field);
  195 | 			}
  196 | 		}
  197 | 
  198 | 		private boolean isCandidate(Method method) {
  199 | 			int modifiers = method.getModifiers();
  200 | 			return !Modifier.isPrivate(modifiers) && !Modifier.isProtected(modifiers) && !Modifier.isAbstract(modifiers)
  201 | 					&& !Modifier.isStatic(modifiers) && !method.isBridge()
  202 | 					&& !Object.class.equals(method.getDeclaringClass())
  203 | 					&& !Class.class.equals(method.getDeclaringClass()) && method.getName().indexOf('$') == -1;
  204 | 		}
  205 | 
  206 | 		private void addMethodIfPossible(@Nullable Method method, String prefix, int parameterCount,
  207 | 				BiConsumer<BeanProperty, Method> consumer) {
  208 | 			if (method != null && method.getParameterCount() == parameterCount && method.getName().startsWith(prefix)
  209 | 					&& method.getName().length() > prefix.length()) {
  210 | 				String propertyName = Introspector.decapitalize(method.getName().substring(prefix.length()));
  211 | 				consumer.accept(this.properties.computeIfAbsent(propertyName, this::getBeanProperty), method);
  212 | 			}
  213 | 		}
  214 | 
  215 | 		private BeanProperty getBeanProperty(String name) {
  216 | 			return new BeanProperty(name, this.type);
  217 | 		}
  218 | 
  219 | 		private void addField(Field field) {
  220 | 			BeanProperty property = this.properties.get(field.getName());
  221 | 			if (property != null) {
  222 | 				property.addField(field);
  223 | 			}
  224 | 		}
  225 | 
  226 | 		protected final ResolvableType getType() {
  227 | 			return this.type;
  228 | 		}
  229 | 
  230 | 		protected final Class<?> getResolvedType() {
  231 | 			return this.resolvedType;
  232 | 		}
  233 | 
  234 | 		final Map<String, BeanProperty> getProperties() {
  235 | 			return this.properties;
  236 | 		}
  237 | 
  238 | 		static BeanProperties of(Bindable<?> bindable) {
  239 | 			ResolvableType type = bindable.getType();
  240 | 			Class<?> resolvedType = type.resolve(Object.class);
  241 | 			return new BeanProperties(type, resolvedType);
  242 | 		}
  243 | 
  244 | 	}
  245 | 
  246 | 	/**
  247 | 	 * The bean being bound.
  248 | 	 *
  249 | 	 * @param <T> the bean type
  250 | 	 */
  251 | 	static class Bean<T> extends BeanProperties {
  252 | 
  253 | 		Bean(ResolvableType type, Class<?> resolvedType) {
  254 | 			super(type, resolvedType);
  255 | 		}
  256 | 
  257 | 		@SuppressWarnings("unchecked")
  258 | 		BeanSupplier<T> getSupplier(Bindable<T> target) {
  259 | 			return new BeanSupplier<>(() -> {
  260 | 				T instance = null;
  261 | 				if (target.getValue() != null) {
  262 | 					instance = target.getValue().get();
  263 | 				}
  264 | 				if (instance == null) {
  265 | 					instance = (T) BeanUtils.instantiateClass(getResolvedType());
  266 | 				}
  267 | 				return instance;
  268 | 			});
  269 | 		}
  270 | 
  271 | 		@SuppressWarnings("unchecked")
  272 | 		static <T> @Nullable Bean<T> get(Bindable<T> bindable, Context context, boolean canCallGetValue) {
  273 | 			ResolvableType type = bindable.getType();
  274 | 			Class<?> resolvedType = type.resolve(Object.class);
  275 | 			Supplier<T> value = bindable.getValue();
  276 | 			T instance = null;
  277 | 			if (canCallGetValue && value != null) {
  278 | 				instance = value.get();
  279 | 				resolvedType = (instance != null) ? instance.getClass() : resolvedType;
  280 | 			}
  281 | 			if (instance == null && !isInstantiable(resolvedType)) {
  282 | 				return null;
  283 | 			}
  284 | 			Map<CacheKey, Bean<?>> cache = getCache(context);
  285 | 			CacheKey cacheKey = new CacheKey(type, resolvedType);
  286 | 			Bean<?> bean = cache.get(cacheKey);
  287 | 			if (bean == null) {
  288 | 				bean = new Bean<>(type, resolvedType);
  289 | 				cache.put(cacheKey, bean);
  290 | 			}
  291 | 			return (Bean<T>) bean;
  292 | 		}
  293 | 
  294 | 		@SuppressWarnings("unchecked")
  295 | 		private static Map<CacheKey, Bean<?>> getCache(Context context) {
  296 | 			Map<CacheKey, Bean<?>> cache = (Map<CacheKey, Bean<?>>) context.getCache().get(Bean.class);
  297 | 			if (cache == null) {
  298 | 				cache = new ConcurrentHashMap<>();
  299 | 				context.getCache().put(Bean.class, cache);
  300 | 			}
  301 | 			return cache;
  302 | 		}
  303 | 
  304 | 		private static boolean isInstantiable(Class<?> type) {
  305 | 			if (type.isInterface()) {
  306 | 				return false;
  307 | 			}
  308 | 			try {
  309 | 				type.getDeclaredConstructor();
  310 | 				return true;
  311 | 			}
  312 | 			catch (Exception ex) {
  313 | 				return false;
  314 | 			}
  315 | 		}
  316 | 
  317 | 		private record CacheKey(ResolvableType type, Class<?> resolvedType) {
  318 | 
  319 | 		}
  320 | 
  321 | 	}
  322 | 
  323 | 	private static class BeanSupplier<T> implements Supplier<T> {
  324 | 
  325 | 		private final Supplier<T> factory;
  326 | 
  327 | 		private @Nullable T instance;
  328 | 
  329 | 		BeanSupplier(Supplier<T> factory) {
  330 | 			this.factory = factory;
  331 | 		}
  332 | 
  333 | 		@Override
  334 | 		public T get() {
  335 | 			if (this.instance == null) {
  336 | 				this.instance = this.factory.get();
  337 | 			}
  338 | 			return this.instance;
  339 | 		}
  340 | 
  341 | 	}
  342 | 
  343 | 	/**
  344 | 	 * A bean property being bound.
  345 | 	 */
  346 | 	static class BeanProperty {
  347 | 
  348 | 		private final String name;
  349 | 
  350 | 		private final ResolvableType declaringClassType;
  351 | 
  352 | 		private @Nullable Method getter;
  353 | 
  354 | 		private @Nullable Method setter;
  355 | 
  356 | 		private @Nullable Field field;
  357 | 
  358 | 		BeanProperty(String name, ResolvableType declaringClassType) {
  359 | 			this.name = DataObjectPropertyName.toDashedForm(name);
  360 | 			this.declaringClassType = declaringClassType;
  361 | 		}
  362 | 
  363 | 		void addGetter(Method getter) {
  364 | 			if (this.getter == null || this.getter.getName().startsWith("is")) {
  365 | 				this.getter = getter;
  366 | 			}
  367 | 		}
  368 | 
  369 | 		void addSetter(Method setter) {
  370 | 			if (this.setter == null || isBetterSetter(setter)) {
  371 | 				this.setter = setter;
  372 | 			}
  373 | 		}
  374 | 
  375 | 		private boolean isBetterSetter(Method setter) {
  376 | 			return this.getter != null && this.getter.getReturnType().equals(setter.getParameterTypes()[0]);
  377 | 		}
  378 | 
  379 | 		void addField(Field field) {
  380 | 			if (this.field == null) {
  381 | 				this.field = field;
  382 | 			}
  383 | 		}
  384 | 
  385 | 		String getName() {
  386 | 			return this.name;
  387 | 		}
  388 | 
  389 | 		ResolvableType getType() {
  390 | 			if (this.setter != null) {
  391 | 				MethodParameter methodParameter = new MethodParameter(this.setter, 0);
  392 | 				return ResolvableType.forMethodParameter(methodParameter, this.declaringClassType);
  393 | 			}
  394 | 			Assert.state(this.getter != null, "'getter' must not be null");
  395 | 			MethodParameter methodParameter = new MethodParameter(this.getter, -1);
  396 | 			return ResolvableType.forMethodParameter(methodParameter, this.declaringClassType);
  397 | 		}
  398 | 
  399 | 		Annotation @Nullable [] getAnnotations() {
  400 | 			try {
  401 | 				return (this.field != null) ? this.field.getDeclaredAnnotations() : null;
  402 | 			}
  403 | 			catch (Exception ex) {
  404 | 				return null;
  405 | 			}
  406 | 		}
  407 | 
  408 | 		@Nullable Supplier<Object> getValue(Supplier<?> instance) {
  409 | 			if (this.getter == null) {
  410 | 				return null;
  411 | 			}
  412 | 			return () -> {
  413 | 				Assert.state(this.getter != null, "'getter' must not be null");
  414 | 				try {
  415 | 					this.getter.setAccessible(true);
  416 | 					return this.getter.invoke(instance.get());
  417 | 				}
  418 | 				catch (Exception ex) {
  419 | 					if (isUninitializedKotlinProperty(ex)) {
  420 | 						return null;
  421 | 					}
  422 | 					throw new IllegalStateException("Unable to get value for property " + this.name, ex);
  423 | 				}
  424 | 			};
  425 | 		}
  426 | 
  427 | 		private boolean isUninitializedKotlinProperty(Exception ex) {
  428 | 			return (ex instanceof InvocationTargetException invocationTargetException)
  429 | 					&& "kotlin.UninitializedPropertyAccessException"
  430 | 						.equals(invocationTargetException.getTargetException().getClass().getName());
  431 | 		}
  432 | 
  433 | 		boolean isSettable() {
  434 | 			return this.setter != null;
  435 | 		}
  436 | 
  437 | 		void setValue(Supplier<?> instance, Object value) {
  438 | 			Assert.state(this.setter != null, "'setter' must not be null");
  439 | 			try {
  440 | 				this.setter.setAccessible(true);
  441 | 				this.setter.invoke(instance.get(), value);
  442 | 			}
  443 | 			catch (Exception ex) {
  444 | 				throw new IllegalStateException("Unable to set value for property " + this.name, ex);
  445 | 			}
  446 | 		}
  447 | 
  448 | 		@Nullable Method getGetter() {
  449 | 			return this.getter;
  450 | 		}
  451 | 
  452 | 		@Nullable Method getSetter() {
  453 | 			return this.setter;
  454 | 		}
  455 | 
  456 | 		@Nullable Field getField() {
  457 | 			return this.field;
  458 | 		}
  459 | 
  460 | 	}
  461 | 
  462 | }
```
