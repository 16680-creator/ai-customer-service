<!-- learning-docs source archive（机械抓取原文，禁止手改代码内容） -->
<!-- origin: org.springframework.boot:spring-boot:4.2.0 | path: org/springframework/boot/context/properties/bind/Binder.java -->
<!-- 正文引用：[[源码:Binder.java#L起-L止]] ；代码块头部：(src: Binder.java#L起-L止) -->

# Binder.java

| 项 | 值 |
| --- | --- |
| 来源 | `org.springframework.boot:spring-boot:4.2.0` |
| 路径 | `org/springframework/boot/context/properties/bind/Binder.java` |
| 行数 | 686 |
| 在线地址 | https://repo1.maven.org/maven2/org/springframework/boot/spring-boot/4.2.0/spring-boot-4.2.0-sources.jar |
| 抓取时间 | 2026-11-25T14:04:52.913Z |

```java
    1 | /*
    2 |  * Copyright 2012-present the original author or authors.
    3 |  */
    4 | 
    5 | package org.springframework.boot.context.properties.bind;
    6 | 
    7 | import java.util.ArrayDeque;
    8 | import java.util.Arrays;
    9 | import java.util.Collection;
   10 | import java.util.Collections;
   11 | import java.util.Deque;
   12 | import java.util.HashMap;
   13 | import java.util.HashSet;
   14 | import java.util.List;
   15 | import java.util.Map;
   16 | import java.util.Set;
   17 | import java.util.function.Consumer;
   18 | import java.util.function.Function;
   19 | import java.util.function.Supplier;
   20 | 
   21 | import org.jspecify.annotations.Nullable;
   22 | 
   23 | import org.springframework.beans.PropertyEditorRegistry;
   24 | import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;
   25 | import org.springframework.boot.context.properties.bind.Bindable.BindRestriction;
   26 | import org.springframework.boot.context.properties.source.ConfigurationProperty;
   27 | import org.springframework.boot.context.properties.source.ConfigurationPropertyCaching;
   28 | import org.springframework.boot.context.properties.source.ConfigurationPropertyName;
   29 | import org.springframework.boot.context.properties.source.ConfigurationPropertySource;
   30 | import org.springframework.boot.context.properties.source.ConfigurationPropertySources;
   31 | import org.springframework.boot.context.properties.source.ConfigurationPropertyState;
   32 | import org.springframework.boot.convert.ApplicationConversionService;
   33 | import org.springframework.core.convert.ConversionService;
   34 | import org.springframework.core.convert.ConverterNotFoundException;
   35 | import org.springframework.core.env.Environment;
   36 | import org.springframework.format.support.DefaultFormattingConversionService;
   37 | import org.springframework.lang.Contract;
   38 | import org.springframework.util.Assert;
   39 | import org.springframework.util.ConcurrentReferenceHashMap;
   40 | import org.springframework.util.ObjectUtils;
   41 | 
   42 | /**
   43 |  * A container object which Binds objects from one or more
   44 |  * {@link ConfigurationPropertySource ConfigurationPropertySources}.
   45 |  *
   46 |  * @author Phillip Webb
   47 |  * @author Madhura Bhave
   48 |  * @since 2.0.0
   49 |  */
   50 | public class Binder {
   51 | 
   52 | 	private static final Set<Class<?>> NON_BEAN_CLASSES = Collections
   53 | 		.unmodifiableSet(new HashSet<>(Arrays.asList(Object.class, Class.class)));
   54 | 
   55 | 	private final Iterable<ConfigurationPropertySource> sources;
   56 | 
   57 | 	private final PlaceholdersResolver placeholdersResolver;
   58 | 
   59 | 	private final BindConverter bindConverter;
   60 | 
   61 | 	private final BindHandler defaultBindHandler;
   62 | 
   63 | 	private final Map<@Nullable BindMethod, List<DataObjectBinder>> dataObjectBinders;
   64 | 
   65 | 	private final Map<Object, Object> cache = new ConcurrentReferenceHashMap<>();
   66 | 
   67 | 	private final ConfigurationPropertyCaching configurationPropertyCaching;
   68 | 
   69 | 	/**
   70 | 	 * Create a new {@link Binder} instance for the specified sources. A
   71 | 	 * {@link DefaultFormattingConversionService} will be used for all conversion.
   72 | 	 * @param sources the sources used for binding
   73 | 	 */
   74 | 	public Binder(ConfigurationPropertySource... sources) {
   75 | 		this(sourcesAsList(sources), null, null, null);
   76 | 	}
   77 | 
   78 | 	private static List<ConfigurationPropertySource> sourcesAsList(ConfigurationPropertySource[] sources) {
   79 | 		Assert.notNull(sources, "'sources' must not be null");
   80 | 		return Arrays.asList(sources);
   81 | 	}
   82 | 
   83 | 	/**
   84 | 	 * Create a new {@link Binder} instance for the specified sources. A
   85 | 	 * {@link DefaultFormattingConversionService} will be used for all conversion.
   86 | 	 * @param sources the sources used for binding
   87 | 	 */
   88 | 	public Binder(Iterable<ConfigurationPropertySource> sources) {
   89 | 		this(sources, null, null, null);
   90 | 	}
   91 | 
   92 | 	/**
   93 | 	 * Create a new {@link Binder} instance for the specified sources.
   94 | 	 * @param sources the sources used for binding
   95 | 	 * @param placeholdersResolver strategy to resolve any property placeholders
   96 | 	 */
   97 | 	public Binder(Iterable<ConfigurationPropertySource> sources, @Nullable PlaceholdersResolver placeholdersResolver) {
   98 | 		this(sources, placeholdersResolver, null, null);
   99 | 	}
  100 | 
  101 | 	/**
  102 | 	 * Create a new {@link Binder} instance for the specified sources.
  103 | 	 * @param sources the sources used for binding
  104 | 	 * @param placeholdersResolver strategy to resolve any property placeholders
  105 | 	 * @param conversionService the conversion service to convert values (or {@code null}
  106 | 	 * to use {@link ApplicationConversionService})
  107 | 	 */
  108 | 	public Binder(Iterable<ConfigurationPropertySource> sources, @Nullable PlaceholdersResolver placeholdersResolver,
  109 | 			@Nullable ConversionService conversionService) {
  110 | 		this(sources, placeholdersResolver, conversionService, null);
  111 | 	}
  112 | 
  113 | 	/**
  114 | 	 * Create a new {@link Binder} instance for the specified sources.
  115 | 	 * @param sources the sources used for binding
  116 | 	 * @param placeholdersResolver strategy to resolve any property placeholders
  117 | 	 * @param conversionService the conversion service to convert values (or {@code null}
  118 | 	 * to use {@link ApplicationConversionService})
  119 | 	 * @param propertyEditorInitializer initializer used to configure the property editors
  120 | 	 * that can convert values (or {@code null} if no initialization is required). Often
  121 | 	 * used to call {@link ConfigurableListableBeanFactory#copyRegisteredEditorsTo}.
  122 | 	 */
  123 | 	public Binder(Iterable<ConfigurationPropertySource> sources, @Nullable PlaceholdersResolver placeholdersResolver,
  124 | 			@Nullable ConversionService conversionService,
  125 | 			@Nullable Consumer<PropertyEditorRegistry> propertyEditorInitializer) {
  126 | 		this(sources, placeholdersResolver, conversionService, propertyEditorInitializer, null);
  127 | 	}
  128 | 
  129 | 	/**
  130 | 	 * Create a new {@link Binder} instance for the specified sources.
  131 | 	 * @param sources the sources used for binding
  132 | 	 * @param placeholdersResolver strategy to resolve any property placeholders
  133 | 	 * @param conversionService the conversion service to convert values (or {@code null}
  134 | 	 * to use {@link ApplicationConversionService})
  135 | 	 * @param propertyEditorInitializer initializer used to configure the property editors
  136 | 	 * that can convert values (or {@code null} if no initialization is required). Often
  137 | 	 * used to call {@link ConfigurableListableBeanFactory#copyRegisteredEditorsTo}.
  138 | 	 * @param defaultBindHandler the default bind handler to use if none is specified when
  139 | 	 * binding
  140 | 	 * @since 2.2.0
  141 | 	 */
  142 | 	public Binder(Iterable<ConfigurationPropertySource> sources, @Nullable PlaceholdersResolver placeholdersResolver,
  143 | 			@Nullable ConversionService conversionService,
  144 | 			@Nullable Consumer<PropertyEditorRegistry> propertyEditorInitializer,
  145 | 			@Nullable BindHandler defaultBindHandler) {
  146 | 		this(sources, placeholdersResolver, conversionService, propertyEditorInitializer, defaultBindHandler, null);
  147 | 	}
  148 | 
  149 | 	/**
  150 | 	 * Create a new {@link Binder} instance for the specified sources.
  151 | 	 * @param sources the sources used for binding
  152 | 	 * @param placeholdersResolver strategy to resolve any property placeholders
  153 | 	 * @param conversionService the conversion service to convert values (or {@code null}
  154 | 	 * to use {@link ApplicationConversionService})
  155 | 	 * @param propertyEditorInitializer initializer used to configure the property editors
  156 | 	 * that can convert values (or {@code null} if no initialization is required). Often
  157 | 	 * used to call {@link ConfigurableListableBeanFactory#copyRegisteredEditorsTo}.
  158 | 	 * @param defaultBindHandler the default bind handler to use if none is specified when
  159 | 	 * binding
  160 | 	 * @param constructorProvider the constructor provider which provides the bind
  161 | 	 * constructor to use when binding
  162 | 	 * @since 2.2.1
  163 | 	 */
  164 | 	public Binder(Iterable<ConfigurationPropertySource> sources, @Nullable PlaceholdersResolver placeholdersResolver,
  165 | 			@Nullable ConversionService conversionService,
  166 | 			@Nullable Consumer<PropertyEditorRegistry> propertyEditorInitializer,
  167 | 			@Nullable BindHandler defaultBindHandler, @Nullable BindConstructorProvider constructorProvider) {
  168 | 		this(sources, placeholdersResolver,
  169 | 				(conversionService != null) ? Collections.singletonList(conversionService) : null,
  170 | 				propertyEditorInitializer, defaultBindHandler, constructorProvider);
  171 | 	}
  172 | 
  173 | 	/**
  174 | 	 * Create a new {@link Binder} instance for the specified sources.
  175 | 	 * @param sources the sources used for binding
  176 | 	 * @param placeholdersResolver strategy to resolve any property placeholders
  177 | 	 * @param conversionServices the conversion services to convert values (or
  178 | 	 * {@code null} to use {@link ApplicationConversionService})
  179 | 	 * @param propertyEditorInitializer initializer used to configure the property editors
  180 | 	 * that can convert values (or {@code null} if no initialization is required). Often
  181 | 	 * used to call {@link ConfigurableListableBeanFactory#copyRegisteredEditorsTo}.
  182 | 	 * @param defaultBindHandler the default bind handler to use if none is specified when
  183 | 	 * binding
  184 | 	 * @param constructorProvider the constructor provider which provides the bind
  185 | 	 * constructor to use when binding
  186 | 	 * @since 2.5.0
  187 | 	 */
  188 | 	public Binder(Iterable<ConfigurationPropertySource> sources, @Nullable PlaceholdersResolver placeholdersResolver,
  189 | 			@Nullable List<ConversionService> conversionServices,
  190 | 			@Nullable Consumer<PropertyEditorRegistry> propertyEditorInitializer,
  191 | 			@Nullable BindHandler defaultBindHandler, @Nullable BindConstructorProvider constructorProvider) {
  192 | 		Assert.notNull(sources, "'sources' must not be null");
  193 | 		for (ConfigurationPropertySource source : sources) {
  194 | 			Assert.notNull(source, "'sources' must not contain null elements");
  195 | 		}
  196 | 		this.sources = sources;
  197 | 		this.configurationPropertyCaching = ConfigurationPropertyCaching.get(sources);
  198 | 		this.placeholdersResolver = (placeholdersResolver != null) ? placeholdersResolver : PlaceholdersResolver.NONE;
  199 | 		this.bindConverter = BindConverter.get(conversionServices, propertyEditorInitializer);
  200 | 		this.defaultBindHandler = (defaultBindHandler != null) ? defaultBindHandler : BindHandler.DEFAULT;
  201 | 		if (constructorProvider == null) {
  202 | 			constructorProvider = BindConstructorProvider.DEFAULT;
  203 | 		}
  204 | 		ValueObjectBinder valueObjectBinder = new ValueObjectBinder(constructorProvider);
  205 | 		JavaBeanBinder javaBeanBinder = JavaBeanBinder.INSTANCE;
  206 | 		Map<@Nullable BindMethod, List<DataObjectBinder>> dataObjectBinders = new HashMap<>();
  207 | 		dataObjectBinders.put(BindMethod.VALUE_OBJECT, List.of(valueObjectBinder));
  208 | 		dataObjectBinders.put(BindMethod.JAVA_BEAN, List.of(javaBeanBinder));
  209 | 		dataObjectBinders.put(null, List.of(valueObjectBinder, javaBeanBinder));
  210 | 		this.dataObjectBinders = Collections.unmodifiableMap(dataObjectBinders);
  211 | 	}
  212 | 
  213 | 	/**
  214 | 	 * Bind the specified target {@link Class} using this binder's
  215 | 	 * {@link ConfigurationPropertySource property sources}.
  216 | 	 * @param name the configuration property name to bind
  217 | 	 * @param target the target class
  218 | 	 * @param <T> the bound type
  219 | 	 * @return the binding result (never {@code null})
  220 | 	 * @see #bind(ConfigurationPropertyName, Bindable, BindHandler)
  221 | 	 */
  222 | 	public <T> BindResult<T> bind(String name, Class<T> target) {
  223 | 		return bind(name, Bindable.of(target));
  224 | 	}
  225 | 
  226 | 	/**
  227 | 	 * Bind the specified target {@link Bindable} using this binder's
  228 | 	 * {@link ConfigurationPropertySource property sources}.
  229 | 	 * @param name the configuration property name to bind
  230 | 	 * @param target the target bindable
  231 | 	 * @param <T> the bound type
  232 | 	 * @return the binding result (never {@code null})
  233 | 	 * @see #bind(ConfigurationPropertyName, Bindable, BindHandler)
  234 | 	 */
  235 | 	public <T> BindResult<T> bind(String name, Bindable<T> target) {
  236 | 		return bind(ConfigurationPropertyName.of(name), target, null);
  237 | 	}
  238 | 
  239 | 	/**
  240 | 	 * Bind the specified target {@link Bindable} using this binder's
  241 | 	 * {@link ConfigurationPropertySource property sources}.
  242 | 	 * @param name the configuration property name to bind
  243 | 	 * @param target the target bindable
  244 | 	 * @param <T> the bound type
  245 | 	 * @return the binding result (never {@code null})
  246 | 	 * @see #bind(ConfigurationPropertyName, Bindable, BindHandler)
  247 | 	 */
  248 | 	public <T> BindResult<T> bind(ConfigurationPropertyName name, Bindable<T> target) {
  249 | 		return bind(name, target, null);
  250 | 	}
  251 | 
  252 | 	/**
  253 | 	 * Bind the specified target {@link Bindable} using this binder's
  254 | 	 * {@link ConfigurationPropertySource property sources}.
  255 | 	 * @param name the configuration property name to bind
  256 | 	 * @param target the target bindable
  257 | 	 * @param handler the bind handler (may be {@code null})
  258 | 	 * @param <T> the bound type
  259 | 	 * @return the binding result (never {@code null})
  260 | 	 */
  261 | 	public <T> BindResult<T> bind(String name, Bindable<T> target, @Nullable BindHandler handler) {
  262 | 		return bind(ConfigurationPropertyName.of(name), target, handler);
  263 | 	}
  264 | 
  265 | 	/**
  266 | 	 * Bind the specified target {@link Bindable} using this binder's
  267 | 	 * {@link ConfigurationPropertySource property sources}.
  268 | 	 * @param name the configuration property name to bind
  269 | 	 * @param target the target bindable
  270 | 	 * @param handler the bind handler (may be {@code null})
  271 | 	 * @param <T> the bound type
  272 | 	 * @return the binding result (never {@code null})
  273 | 	 */
  274 | 	public <T> BindResult<T> bind(ConfigurationPropertyName name, Bindable<T> target, @Nullable BindHandler handler) {
  275 | 		T bound = bind(name, target, handler, false);
  276 | 		return BindResult.of(bound);
  277 | 	}
  278 | 
  279 | 	/**
  280 | 	 * Bind the specified target {@link Class} using this binder's
  281 | 	 * {@link ConfigurationPropertySource property sources} or create a new instance of
  282 | 	 * the specified target {@link Class} if the result of the binding is {@code null}.
  283 | 	 * @param name the configuration property name to bind
  284 | 	 * @param target the target class
  285 | 	 * @param <T> the bound type
  286 | 	 * @return the bound or created object
  287 | 	 * @since 2.2.0
  288 | 	 * @see #bind(ConfigurationPropertyName, Bindable, BindHandler)
  289 | 	 */
  290 | 	public <T> T bindOrCreate(String name, Class<T> target) {
  291 | 		return bindOrCreate(name, Bindable.of(target));
  292 | 	}
  293 | 
  294 | 	/**
  295 | 	 * Bind the specified target {@link Bindable} using this binder's
  296 | 	 * {@link ConfigurationPropertySource property sources} or create a new instance using
  297 | 	 * the type of the {@link Bindable} if the result of the binding is {@code null}.
  298 | 	 * @param name the configuration property name to bind
  299 | 	 * @param target the target bindable
  300 | 	 * @param <T> the bound type
  301 | 	 * @return the bound or created object
  302 | 	 * @since 2.2.0
  303 | 	 * @see #bindOrCreate(ConfigurationPropertyName, Bindable, BindHandler)
  304 | 	 */
  305 | 	public <T> T bindOrCreate(String name, Bindable<T> target) {
  306 | 		return bindOrCreate(ConfigurationPropertyName.of(name), target, null);
  307 | 	}
  308 | 
  309 | 	/**
  310 | 	 * Bind the specified target {@link Bindable} using this binder's
  311 | 	 * {@link ConfigurationPropertySource property sources} or create a new instance using
  312 | 	 * the type of the {@link Bindable} if the result of the binding is {@code null}.
  313 | 	 * @param name the configuration property name to bind
  314 | 	 * @param target the target bindable
  315 | 	 * @param handler the bind handler
  316 | 	 * @param <T> the bound type
  317 | 	 * @return the bound or created object
  318 | 	 * @since 2.2.0
  319 | 	 * @see #bindOrCreate(ConfigurationPropertyName, Bindable, BindHandler)
  320 | 	 */
  321 | 	public <T> T bindOrCreate(String name, Bindable<T> target, BindHandler handler) {
  322 | 		return bindOrCreate(ConfigurationPropertyName.of(name), target, handler);
  323 | 	}
  324 | 
  325 | 	/**
  326 | 	 * Bind the specified target {@link Bindable} using this binder's
  327 | 	 * {@link ConfigurationPropertySource property sources} or create a new instance using
  328 | 	 * the type of the {@link Bindable} if the result of the binding is {@code null}.
  329 | 	 * @param name the configuration property name to bind
  330 | 	 * @param target the target bindable
  331 | 	 * @param handler the bind handler (may be {@code null})
  332 | 	 * @param <T> the bound or created type
  333 | 	 * @return the bound or created object
  334 | 	 * @since 2.2.0
  335 | 	 */
  336 | 	public <T> T bindOrCreate(ConfigurationPropertyName name, Bindable<T> target, @Nullable BindHandler handler) {
  337 | 		return bind(name, target, handler, true);
  338 | 	}
  339 | 
  340 | 	@Contract("_, _, _, true -> !null")
  341 | 	private <T> @Nullable T bind(ConfigurationPropertyName name, Bindable<T> target, @Nullable BindHandler handler,
  342 | 			boolean create) {
  343 | 		Assert.notNull(name, "'name' must not be null");
  344 | 		Assert.notNull(target, "'target' must not be null");
  345 | 		handler = (handler != null) ? handler : this.defaultBindHandler;
  346 | 		Context context = new Context();
  347 | 		return bind(name, target, handler, context, false, create);
  348 | 	}
  349 | 
  350 | 	@Contract("_, _, _, _, _, true -> !null")
  351 | 	private <T> @Nullable T bind(ConfigurationPropertyName name, Bindable<T> target, BindHandler handler,
  352 | 			Context context, boolean allowRecursiveBinding, boolean create) {
  353 | 		try (ConfigurationPropertyCaching.CacheOverride cacheOverride = this.configurationPropertyCaching.override()) {
  354 | 			try {
  355 | 				Bindable<T> replacementTarget = handler.onStart(name, target, context);
  356 | 				if (replacementTarget == null) {
  357 | 					return handleBindResult(name, target, handler, context, null, create);
  358 | 				}
  359 | 				target = replacementTarget;
  360 | 				Object bound = bindObject(name, target, handler, context, allowRecursiveBinding);
  361 | 				return handleBindResult(name, target, handler, context, bound, create);
  362 | 			}
  363 | 			catch (Exception ex) {
  364 | 				return handleBindError(name, target, handler, context, ex);
  365 | 			}
  366 | 		}
  367 | 	}
  368 | 
  369 | 	@Contract("_, _, _, _, _, true -> null")
  370 | 	private <T> @Nullable T handleBindResult(ConfigurationPropertyName name, Bindable<T> target, BindHandler handler,
  371 | 			Context context, @Nullable Object result, boolean create) throws Exception {
  372 | 		if (result != null) {
  373 | 			result = handler.onSuccess(name, target, context, result);
  374 | 			result = context.getConverter().convert(result, target);
  375 | 		}
  376 | 		if (result == null && create) {
  377 | 			result = fromDataObjectBinders(target.getBindMethod(),
  378 | 					(dataObjectBinder) -> dataObjectBinder.create(target, context));
  379 | 			if (result == null) {
  380 | 				IllegalStateException ex = new IllegalStateException(
  381 | 						"Unable to create instance for " + target.getType());
  382 | 				List<DataObjectBinder> dataObjectBinders = this.dataObjectBinders.get(target.getBindMethod());
  383 | 				Assert.state(dataObjectBinders != null, "'dataObjectBinders' must not be null");
  384 | 				dataObjectBinders
  385 | 					.forEach((dataObjectBinder) -> dataObjectBinder.onUnableToCreateInstance(target, context, ex));
  386 | 				throw ex;
  387 | 			}
  388 | 			result = handler.onCreate(name, target, context, result);
  389 | 			result = context.getConverter().convert(result, target);
  390 | 		}
  391 | 		handler.onFinish(name, target, context, result);
  392 | 		return context.getConverter().convert(result, target);
  393 | 	}
  394 | 
  395 | 	private <T> @Nullable T handleBindError(ConfigurationPropertyName name, Bindable<T> target, BindHandler handler,
  396 | 			Context context, Exception error) {
  397 | 		try {
  398 | 			Object result = handler.onFailure(name, target, context, error);
  399 | 			return context.getConverter().convert(result, target);
  400 | 		}
  401 | 		catch (Exception ex) {
  402 | 			if (ex instanceof BindException bindException) {
  403 | 				throw bindException;
  404 | 			}
  405 | 			throw new BindException(name, target, context.getConfigurationProperty(), ex);
  406 | 		}
  407 | 	}
  408 | 
  409 | 	private <T> @Nullable Object bindObject(ConfigurationPropertyName name, Bindable<T> target, BindHandler handler,
  410 | 			Context context, boolean allowRecursiveBinding) {
  411 | 		ConfigurationProperty property = findProperty(name, target, context);
  412 | 		if (property == null && context.depth != 0 && containsNoDescendantOf(context.getSources(), name)) {
  413 | 			return null;
  414 | 		}
  415 | 		AggregateBinder<?> aggregateBinder = getAggregateBinder(target, context);
  416 | 		if (aggregateBinder != null) {
  417 | 			return bindAggregate(name, target, handler, context, aggregateBinder);
  418 | 		}
  419 | 		if (property != null) {
  420 | 			try {
  421 | 				return bindProperty(target, context, property);
  422 | 			}
  423 | 			catch (ConverterNotFoundException ex) {
  424 | 				// We might still be able to bind it using the recursive binders
  425 | 				boolean fallbackToDefaultValue = ObjectUtils.isEmpty(property.getValue());
  426 | 				Object instance = bindDataObject(name, target, handler, context, allowRecursiveBinding,
  427 | 						fallbackToDefaultValue);
  428 | 				if (instance != null) {
  429 | 					return instance;
  430 | 				}
  431 | 				throw ex;
  432 | 			}
  433 | 		}
  434 | 		return bindDataObject(name, target, handler, context, allowRecursiveBinding, false);
  435 | 	}
  436 | 
  437 | 	private @Nullable AggregateBinder<?> getAggregateBinder(Bindable<?> target, Context context) {
  438 | 		Class<?> resolvedType = target.getType().resolve(Object.class);
  439 | 		if (Map.class.isAssignableFrom(resolvedType)) {
  440 | 			return new MapBinder(context);
  441 | 		}
  442 | 		if (Collection.class.isAssignableFrom(resolvedType)) {
  443 | 			return new CollectionBinder(context);
  444 | 		}
  445 | 		if (target.getType().isArray()) {
  446 | 			return new ArrayBinder(context);
  447 | 		}
  448 | 		return null;
  449 | 	}
  450 | 
  451 | 	private <T> @Nullable Object bindAggregate(ConfigurationPropertyName name, Bindable<T> target, BindHandler handler,
  452 | 			Context context, AggregateBinder<?> aggregateBinder) {
  453 | 		AggregateElementBinder elementBinder = (itemName, itemTarget, source) -> {
  454 | 			boolean allowRecursiveBinding = aggregateBinder.isAllowRecursiveBinding(source);
  455 | 			Supplier<?> supplier = () -> bind(itemName, itemTarget, handler, context, allowRecursiveBinding, false);
  456 | 			return context.withSource(source, supplier);
  457 | 		};
  458 | 		Supplier<@Nullable Object> supplier = () -> aggregateBinder.bind(name, target, elementBinder);
  459 | 		return context.withIncreasedDepth(supplier);
  460 | 	}
  461 | 
  462 | 	private <T> @Nullable ConfigurationProperty findProperty(ConfigurationPropertyName name, Bindable<T> target,
  463 | 			Context context) {
  464 | 		if (name.isEmpty() || target.hasBindRestriction(BindRestriction.NO_DIRECT_PROPERTY)) {
  465 | 			return null;
  466 | 		}
  467 | 		for (ConfigurationPropertySource source : context.getSources()) {
  468 | 			ConfigurationProperty property = source.getConfigurationProperty(name);
  469 | 			if (property != null) {
  470 | 				return property;
  471 | 			}
  472 | 		}
  473 | 		return null;
  474 | 	}
  475 | 
  476 | 	private <T> @Nullable Object bindProperty(Bindable<T> target, Context context, ConfigurationProperty property) {
  477 | 		context.setConfigurationProperty(property);
  478 | 		Object result = property.getValue();
  479 | 		result = this.placeholdersResolver.resolvePlaceholders(result);
  480 | 		result = context.getConverter().convert(result, target);
  481 | 		return result;
  482 | 	}
  483 | 
  484 | 	private @Nullable Object bindDataObject(ConfigurationPropertyName name, Bindable<?> target, BindHandler handler,
  485 | 			Context context, boolean allowRecursiveBinding, boolean fallbackToDefaultValue) {
  486 | 		if (isUnbindableBean(name, target, context)) {
  487 | 			return null;
  488 | 		}
  489 | 		Class<?> type = target.getType().resolve(Object.class);
  490 | 		BindMethod bindMethod = target.getBindMethod();
  491 | 		if (!allowRecursiveBinding && context.isBindingDataObject(type)) {
  492 | 			return null;
  493 | 		}
  494 | 		DataObjectPropertyBinder propertyBinder = (propertyName, propertyTarget) -> bind(name.append(propertyName),
  495 | 				propertyTarget, handler, context, false, false);
  496 | 		Supplier<@Nullable Object> supplier = () -> fromDataObjectBinders(bindMethod,
  497 | 				(dataObjectBinder) -> dataObjectBinder.bind(name, target, context, propertyBinder,
  498 | 						fallbackToDefaultValue));
  499 | 		return context.withDataObject(type, supplier);
  500 | 	}
  501 | 
  502 | 	private @Nullable Object fromDataObjectBinders(@Nullable BindMethod bindMethod,
  503 | 			Function<DataObjectBinder, @Nullable Object> operation) {
  504 | 		List<DataObjectBinder> dataObjectBinders = this.dataObjectBinders.get(bindMethod);
  505 | 		Assert.state(dataObjectBinders != null, "'dataObjectBinders' must not be null");
  506 | 		for (DataObjectBinder dataObjectBinder : dataObjectBinders) {
  507 | 			Object bound = operation.apply(dataObjectBinder);
  508 | 			if (bound != null) {
  509 | 				return bound;
  510 | 			}
  511 | 		}
  512 | 		return null;
  513 | 	}
  514 | 
  515 | 	private boolean isUnbindableBean(ConfigurationPropertyName name, Bindable<?> target, Context context) {
  516 | 		for (ConfigurationPropertySource source : context.getSources()) {
  517 | 			if (source.containsDescendantOf(name) == ConfigurationPropertyState.PRESENT) {
  518 | 				// We know there are properties to bind so we can't bypass anything
  519 | 				return false;
  520 | 			}
  521 | 		}
  522 | 		Class<?> resolved = target.getType().resolve(Object.class);
  523 | 		if (resolved.isPrimitive() || NON_BEAN_CLASSES.contains(resolved)) {
  524 | 			return true;
  525 | 		}
  526 | 		return resolved.getName().startsWith("java.");
  527 | 	}
  528 | 
  529 | 	private boolean containsNoDescendantOf(Iterable<ConfigurationPropertySource> sources,
  530 | 			ConfigurationPropertyName name) {
  531 | 		for (ConfigurationPropertySource source : sources) {
  532 | 			if (source.containsDescendantOf(name) != ConfigurationPropertyState.ABSENT) {
  533 | 				return false;
  534 | 			}
  535 | 		}
  536 | 		return true;
  537 | 	}
  538 | 
  539 | 	/**
  540 | 	 * Create a new {@link Binder} instance from the specified environment.
  541 | 	 * @param environment the environment source (must have attached
  542 | 	 * {@link ConfigurationPropertySources})
  543 | 	 * @return a {@link Binder} instance
  544 | 	 */
  545 | 	public static Binder get(Environment environment) {
  546 | 		return get(environment, null);
  547 | 	}
  548 | 
  549 | 	/**
  550 | 	 * Create a new {@link Binder} instance from the specified environment.
  551 | 	 * @param environment the environment source (must have attached
  552 | 	 * {@link ConfigurationPropertySources})
  553 | 	 * @param defaultBindHandler the default bind handler to use if none is specified when
  554 | 	 * binding
  555 | 	 * @return a {@link Binder} instance
  556 | 	 * @since 2.2.0
  557 | 	 */
  558 | 	public static Binder get(Environment environment, @Nullable BindHandler defaultBindHandler) {
  559 | 		Iterable<ConfigurationPropertySource> sources = ConfigurationPropertySources.get(environment);
  560 | 		PropertySourcesPlaceholdersResolver placeholdersResolver = new PropertySourcesPlaceholdersResolver(environment);
  561 | 		return new Binder(sources, placeholdersResolver, null, null, defaultBindHandler);
  562 | 	}
  563 | 
  564 | 	/**
  565 | 	 * Context used when binding and the {@link BindContext} implementation.
  566 | 	 */
  567 | 	final class Context implements BindContext {
  568 | 
  569 | 		private int depth;
  570 | 
  571 | 		private final List<ConfigurationPropertySource> source = Arrays.asList((ConfigurationPropertySource) null);
  572 | 
  573 | 		private int sourcePushCount;
  574 | 
  575 | 		private final Deque<Class<?>> dataObjectBindings = new ArrayDeque<>();
  576 | 
  577 | 		private final Deque<Class<?>> constructorBindings = new ArrayDeque<>();
  578 | 
  579 | 		private @Nullable ConfigurationProperty configurationProperty;
  580 | 
  581 | 		private void increaseDepth() {
  582 | 			this.depth++;
  583 | 		}
  584 | 
  585 | 		private void decreaseDepth() {
  586 | 			this.depth--;
  587 | 		}
  588 | 
  589 | 		private <T> T withSource(@Nullable ConfigurationPropertySource source, Supplier<T> supplier) {
  590 | 			if (source == null) {
  591 | 				return supplier.get();
  592 | 			}
  593 | 			ConfigurationPropertySource previous = this.source.get(0);
  594 | 			this.source.set(0, source);
  595 | 			this.sourcePushCount++;
  596 | 			try {
  597 | 				return supplier.get();
  598 | 			}
  599 | 			finally {
  600 | 				this.sourcePushCount--;
  601 | 				this.source.set(0, previous);
  602 | 			}
  603 | 		}
  604 | 
  605 | 		private <T> @Nullable T withDataObject(Class<?> type, Supplier<@Nullable T> supplier) {
  606 | 			this.dataObjectBindings.push(type);
  607 | 			try {
  608 | 				return withIncreasedDepth(supplier);
  609 | 			}
  610 | 			finally {
  611 | 				this.dataObjectBindings.pop();
  612 | 			}
  613 | 		}
  614 | 
  615 | 		private boolean isBindingDataObject(Class<?> type) {
  616 | 			return this.dataObjectBindings.contains(type);
  617 | 		}
  618 | 
  619 | 		private <T> @Nullable T withIncreasedDepth(Supplier<@Nullable T> supplier) {
  620 | 			increaseDepth();
  621 | 			try {
  622 | 				return supplier.get();
  623 | 			}
  624 | 			finally {
  625 | 				decreaseDepth();
  626 | 			}
  627 | 		}
  628 | 
  629 | 		void setConfigurationProperty(ConfigurationProperty configurationProperty) {
  630 | 			this.configurationProperty = configurationProperty;
  631 | 		}
  632 | 
  633 | 		void clearConfigurationProperty() {
  634 | 			this.configurationProperty = null;
  635 | 		}
  636 | 
  637 | 		void pushConstructorBoundTypes(Class<?> value) {
  638 | 			this.constructorBindings.push(value);
  639 | 		}
  640 | 
  641 | 		boolean isNestedConstructorBinding() {
  642 | 			return !this.constructorBindings.isEmpty();
  643 | 		}
  644 | 
  645 | 		void popConstructorBoundTypes() {
  646 | 			this.constructorBindings.pop();
  647 | 		}
  648 | 
  649 | 		PlaceholdersResolver getPlaceholdersResolver() {
  650 | 			return Binder.this.placeholdersResolver;
  651 | 		}
  652 | 
  653 | 		BindConverter getConverter() {
  654 | 			return Binder.this.bindConverter;
  655 | 		}
  656 | 
  657 | 		Map<Object, Object> getCache() {
  658 | 			return Binder.this.cache;
  659 | 		}
  660 | 
  661 | 		@Override
  662 | 		public Binder getBinder() {
  663 | 			return Binder.this;
  664 | 		}
  665 | 
  666 | 		@Override
  667 | 		public int getDepth() {
  668 | 			return this.depth;
  669 | 		}
  670 | 
  671 | 		@Override
  672 | 		public Iterable<ConfigurationPropertySource> getSources() {
  673 | 			if (this.sourcePushCount > 0) {
  674 | 				return this.source;
  675 | 			}
  676 | 			return Binder.this.sources;
  677 | 		}
  678 | 
  679 | 		@Override
  680 | 		public @Nullable ConfigurationProperty getConfigurationProperty() {
  681 | 			return this.configurationProperty;
  682 | 		}
  683 | 
  684 | 	}
  685 | 
  686 | }
```
