<!-- learning-docs source archive（机械抓取原文，禁止手改代码内容） -->
<!-- origin: org.springframework:spring-web:7.1.0 | path: org/springframework/http/converter/json/Jackson2ObjectMapperBuilder.java -->
<!-- 正文引用：[[源码:Jackson2ObjectMapperBuilder.java#L起-L止]] ；代码块头部：(src: Jackson2ObjectMapperBuilder.java#L起-L止) -->

# Jackson2ObjectMapperBuilder.java

| 项 | 值 |
| --- | --- |
| 来源 | `org.springframework:spring-web:7.1.0` |
| 路径 | `org/springframework/http/converter/json/Jackson2ObjectMapperBuilder.java` |
| 行数 | 979 |
| 在线地址 | https://repo1.maven.org/maven2/org/springframework/spring-web/7.1.0/spring-web-7.1.0-sources.jar |
| 抓取时间 | 2026-11-25T14:01:25.758Z |

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
   17 | package org.springframework.http.converter.json;
   18 | 
   19 | import java.text.DateFormat;
   20 | import java.text.SimpleDateFormat;
   21 | import java.util.ArrayList;
   22 | import java.util.Arrays;
   23 | import java.util.LinkedHashMap;
   24 | import java.util.List;
   25 | import java.util.Locale;
   26 | import java.util.Map;
   27 | import java.util.TimeZone;
   28 | import java.util.function.Consumer;
   29 | import java.util.function.Function;
   30 | 
   31 | import com.fasterxml.jackson.annotation.JsonAutoDetect;
   32 | import com.fasterxml.jackson.annotation.JsonFilter;
   33 | import com.fasterxml.jackson.annotation.JsonInclude;
   34 | import com.fasterxml.jackson.annotation.PropertyAccessor;
   35 | import com.fasterxml.jackson.core.JsonFactory;
   36 | import com.fasterxml.jackson.core.JsonGenerator;
   37 | import com.fasterxml.jackson.core.JsonParser;
   38 | import com.fasterxml.jackson.databind.AnnotationIntrospector;
   39 | import com.fasterxml.jackson.databind.DeserializationFeature;
   40 | import com.fasterxml.jackson.databind.JsonDeserializer;
   41 | import com.fasterxml.jackson.databind.JsonSerializer;
   42 | import com.fasterxml.jackson.databind.KeyDeserializer;
   43 | import com.fasterxml.jackson.databind.MapperFeature;
   44 | import com.fasterxml.jackson.databind.Module;
   45 | import com.fasterxml.jackson.databind.ObjectMapper;
   46 | import com.fasterxml.jackson.databind.PropertyNamingStrategy;
   47 | import com.fasterxml.jackson.databind.SerializationFeature;
   48 | import com.fasterxml.jackson.databind.cfg.DatatypeFeature;
   49 | import com.fasterxml.jackson.databind.cfg.HandlerInstantiator;
   50 | import com.fasterxml.jackson.databind.introspect.AnnotationIntrospectorPair;
   51 | import com.fasterxml.jackson.databind.jsontype.TypeResolverBuilder;
   52 | import com.fasterxml.jackson.databind.module.SimpleModule;
   53 | import com.fasterxml.jackson.databind.ser.FilterProvider;
   54 | import com.fasterxml.jackson.dataformat.cbor.CBORFactory;
   55 | import com.fasterxml.jackson.dataformat.smile.SmileFactory;
   56 | import com.fasterxml.jackson.dataformat.xml.JacksonXmlModule;
   57 | import com.fasterxml.jackson.dataformat.xml.XmlFactory;
   58 | import com.fasterxml.jackson.dataformat.xml.XmlMapper;
   59 | import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
   60 | import org.jspecify.annotations.Nullable;
   61 | 
   62 | import org.springframework.beans.BeanUtils;
   63 | import org.springframework.context.ApplicationContext;
   64 | import org.springframework.core.KotlinDetector;
   65 | import org.springframework.http.ProblemDetail;
   66 | import org.springframework.util.Assert;
   67 | import org.springframework.util.ClassUtils;
   68 | import org.springframework.util.LinkedMultiValueMap;
   69 | import org.springframework.util.MultiValueMap;
   70 | import org.springframework.util.StringUtils;
   71 | import org.springframework.util.xml.StaxUtils;
   72 | 
   73 | /**
   74 |  * A builder used to create {@link ObjectMapper} instances with a fluent API.
   75 |  *
   76 |  * <p>It customizes Jackson's default properties with the following ones:
   77 |  * <ul>
   78 |  * <li>{@link MapperFeature#DEFAULT_VIEW_INCLUSION} is disabled</li>
   79 |  * <li>{@link DeserializationFeature#FAIL_ON_UNKNOWN_PROPERTIES} is disabled</li>
   80 |  * </ul>
   81 |  *
   82 |  * <p>It also automatically registers the following well-known modules if they are
   83 |  * detected on the classpath:
   84 |  * <ul>
   85 |  * <li><a href="https://github.com/FasterXML/jackson-datatype-jdk8">jackson-datatype-jdk8</a>:
   86 |  * support for Java 8 types like {@link java.util.Optional}</li>
   87 |  * <li><a href="https://github.com/FasterXML/jackson-datatype-jsr310">jackson-datatype-jsr310</a>:
   88 |  * support for Java Date &amp; Time API types</li>
   89 |  * <li><a href="https://github.com/FasterXML/jackson-module-kotlin">jackson-module-kotlin</a>:
   90 |  * support for Kotlin classes and data classes</li>
   91 |  * <li><a href="https://github.com/FasterXML/jackson-modules-java8/tree/2.18/parameter-names">jackson-modules-java8/parameter-names</a>:
   92 |  * support for accessing parameter names</li>
   93 |  * </ul>
   94 |  *
   95 |  * @author Sebastien Deleuze
   96 |  * @author Juergen Hoeller
   97 |  * @author Tadaya Tsuyukubo
   98 |  * @author Eddú Meléndez
   99 |  * @author Hyoungjune Kim
  100 |  * @since 4.1.1
  101 |  * @see #build()
  102 |  * @see #configure(ObjectMapper)
  103 |  * @deprecated since 7.0 in favor using directly Jackson 3 builders like {@link tools.jackson.databind.json.JsonMapper#builder}
  104 |  */
  105 | @Deprecated(since = "7.0", forRemoval = true)
  106 | @SuppressWarnings("removal")
  107 | public class Jackson2ObjectMapperBuilder {
  108 | 
  109 | 	private static final boolean JACKSON_2_XML_PRESENT = ClassUtils.isPresent(
  110 | 			"com.fasterxml.jackson.dataformat.xml.XmlMapper", Jackson2ObjectMapperBuilder.class.getClassLoader());
  111 | 
  112 | 
  113 | 	private final Map<Class<?>, Class<?>> mixIns = new LinkedHashMap<>();
  114 | 
  115 | 	private final Map<Class<?>, JsonSerializer<?>> serializers = new LinkedHashMap<>();
  116 | 
  117 | 	private final Map<Class<?>, JsonDeserializer<?>> deserializers = new LinkedHashMap<>();
  118 | 
  119 | 	private final Map<PropertyAccessor, JsonAutoDetect.Visibility> visibilities = new LinkedHashMap<>();
  120 | 
  121 | 	private final Map<Object, Boolean> features = new LinkedHashMap<>();
  122 | 
  123 | 	private boolean createXmlMapper = false;
  124 | 
  125 | 	private @Nullable JsonFactory factory;
  126 | 
  127 | 	private @Nullable DateFormat dateFormat;
  128 | 
  129 | 	private @Nullable Locale locale;
  130 | 
  131 | 	private @Nullable TimeZone timeZone;
  132 | 
  133 | 	private @Nullable AnnotationIntrospector annotationIntrospector;
  134 | 
  135 | 	private @Nullable PropertyNamingStrategy propertyNamingStrategy;
  136 | 
  137 | 	private @Nullable TypeResolverBuilder<?> defaultTyping;
  138 | 
  139 | 	private JsonInclude.@Nullable Value serializationInclusion;
  140 | 
  141 | 	private @Nullable FilterProvider filters;
  142 | 
  143 | 	private @Nullable List<Module> modules;
  144 | 
  145 | 	private Class<? extends Module> @Nullable [] moduleClasses;
  146 | 
  147 | 	private boolean findModulesViaServiceLoader = false;
  148 | 
  149 | 	private boolean findWellKnownModules = true;
  150 | 
  151 | 	private ClassLoader moduleClassLoader = getClass().getClassLoader();
  152 | 
  153 | 	private @Nullable HandlerInstantiator handlerInstantiator;
  154 | 
  155 | 	private @Nullable ApplicationContext applicationContext;
  156 | 
  157 | 	private @Nullable Boolean defaultUseWrapper;
  158 | 
  159 | 	private @Nullable Consumer<ObjectMapper> configurer;
  160 | 
  161 | 
  162 | 	/**
  163 | 	 * If set to {@code true}, an {@link XmlMapper} will be created using its
  164 | 	 * default constructor. This is only applicable to {@link #build()} calls,
  165 | 	 * not to {@link #configure} calls.
  166 | 	 */
  167 | 	public Jackson2ObjectMapperBuilder createXmlMapper(boolean createXmlMapper) {
  168 | 		this.createXmlMapper = createXmlMapper;
  169 | 		return this;
  170 | 	}
  171 | 
  172 | 	/**
  173 | 	 * Define the {@link JsonFactory} to be used to create the {@link ObjectMapper}
  174 | 	 * instance.
  175 | 	 * @since 5.0
  176 | 	 */
  177 | 	public Jackson2ObjectMapperBuilder factory(JsonFactory factory) {
  178 | 		this.factory = factory;
  179 | 		return this;
  180 | 	}
  181 | 
  182 | 	/**
  183 | 	 * Define the format for date/time with the given {@link DateFormat}.
  184 | 	 * <p>Note: Setting this property makes the exposed {@link ObjectMapper}
  185 | 	 * non-thread-safe, according to Jackson's thread safety rules.
  186 | 	 * @see #simpleDateFormat(String)
  187 | 	 */
  188 | 	public Jackson2ObjectMapperBuilder dateFormat(DateFormat dateFormat) {
  189 | 		this.dateFormat = dateFormat;
  190 | 		return this;
  191 | 	}
  192 | 
  193 | 	/**
  194 | 	 * Define the date/time format with a {@link SimpleDateFormat}.
  195 | 	 * <p>Note: Setting this property makes the exposed {@link ObjectMapper}
  196 | 	 * non-thread-safe, according to Jackson's thread safety rules.
  197 | 	 * @see #dateFormat(DateFormat)
  198 | 	 */
  199 | 	public Jackson2ObjectMapperBuilder simpleDateFormat(String format) {
  200 | 		this.dateFormat = new SimpleDateFormat(format);
  201 | 		return this;
  202 | 	}
  203 | 
  204 | 	/**
  205 | 	 * Override the default {@link Locale} to use for formatting.
  206 | 	 * <p>Default value used is {@link Locale#getDefault()}.
  207 | 	 * @since 4.1.5
  208 | 	 */
  209 | 	public Jackson2ObjectMapperBuilder locale(Locale locale) {
  210 | 		this.locale = locale;
  211 | 		return this;
  212 | 	}
  213 | 
  214 | 	/**
  215 | 	 * Override the default {@link Locale} to use for formatting.
  216 | 	 * <p>Default value used is {@link Locale#getDefault()}.
  217 | 	 * @param localeString the locale ID as a String representation
  218 | 	 * @since 4.1.5
  219 | 	 */
  220 | 	public Jackson2ObjectMapperBuilder locale(String localeString) {
  221 | 		this.locale = StringUtils.parseLocale(localeString);
  222 | 		return this;
  223 | 	}
  224 | 
  225 | 	/**
  226 | 	 * Override the default {@link TimeZone} to use for formatting.
  227 | 	 * <p>Default value used is UTC (NOT local timezone).
  228 | 	 * @since 4.1.5
  229 | 	 */
  230 | 	public Jackson2ObjectMapperBuilder timeZone(TimeZone timeZone) {
  231 | 		this.timeZone = timeZone;
  232 | 		return this;
  233 | 	}
  234 | 
  235 | 	/**
  236 | 	 * Override the default {@link TimeZone} to use for formatting.
  237 | 	 * <p>Default value used is UTC (NOT local timezone).
  238 | 	 * @param timeZoneString the zone ID as a String representation
  239 | 	 * @since 4.1.5
  240 | 	 */
  241 | 	public Jackson2ObjectMapperBuilder timeZone(String timeZoneString) {
  242 | 		this.timeZone = StringUtils.parseTimeZoneString(timeZoneString);
  243 | 		return this;
  244 | 	}
  245 | 
  246 | 	/**
  247 | 	 * Set an {@link AnnotationIntrospector} for both serialization and deserialization.
  248 | 	 */
  249 | 	public Jackson2ObjectMapperBuilder annotationIntrospector(AnnotationIntrospector annotationIntrospector) {
  250 | 		this.annotationIntrospector = annotationIntrospector;
  251 | 		return this;
  252 | 	}
  253 | 
  254 | 	/**
  255 | 	 * Alternative to {@link #annotationIntrospector(AnnotationIntrospector)}
  256 | 	 * that allows combining with rather than replacing the currently set
  257 | 	 * introspector, for example, via
  258 | 	 * {@link AnnotationIntrospectorPair#pair(AnnotationIntrospector, AnnotationIntrospector)}.
  259 | 	 * @param pairingFunction a function to apply to the currently set
  260 | 	 * introspector (possibly {@code null}); the result of the function becomes
  261 | 	 * the new introspector.
  262 | 	 * @since 5.2.4
  263 | 	 */
  264 | 	public Jackson2ObjectMapperBuilder annotationIntrospector(
  265 | 			Function<@Nullable AnnotationIntrospector, @Nullable AnnotationIntrospector> pairingFunction) {
  266 | 
  267 | 		this.annotationIntrospector = pairingFunction.apply(this.annotationIntrospector);
  268 | 		return this;
  269 | 	}
  270 | 
  271 | 	/**
  272 | 	 * Specify a {@link com.fasterxml.jackson.databind.PropertyNamingStrategy} to
  273 | 	 * configure the {@link ObjectMapper} with.
  274 | 	 */
  275 | 	public Jackson2ObjectMapperBuilder propertyNamingStrategy(PropertyNamingStrategy propertyNamingStrategy) {
  276 | 		this.propertyNamingStrategy = propertyNamingStrategy;
  277 | 		return this;
  278 | 	}
  279 | 
  280 | 	/**
  281 | 	 * Specify a {@link TypeResolverBuilder} to use for Jackson's default typing.
  282 | 	 * @since 4.2.2
  283 | 	 */
  284 | 	public Jackson2ObjectMapperBuilder defaultTyping(TypeResolverBuilder<?> typeResolverBuilder) {
  285 | 		this.defaultTyping = typeResolverBuilder;
  286 | 		return this;
  287 | 	}
  288 | 
  289 | 	/**
  290 | 	 * Set a custom inclusion strategy for serialization.
  291 | 	 * @see com.fasterxml.jackson.annotation.JsonInclude.Include
  292 | 	 */
  293 | 	public Jackson2ObjectMapperBuilder serializationInclusion(JsonInclude.Include inclusion) {
  294 | 		return serializationInclusion(JsonInclude.Value.construct(inclusion, inclusion));
  295 | 	}
  296 | 
  297 | 	/**
  298 | 	 * Set a custom inclusion strategy for serialization.
  299 | 	 * @since 5.3
  300 | 	 * @see com.fasterxml.jackson.annotation.JsonInclude.Value
  301 | 	 */
  302 | 	public Jackson2ObjectMapperBuilder serializationInclusion(JsonInclude.Value serializationInclusion) {
  303 | 		this.serializationInclusion = serializationInclusion;
  304 | 		return this;
  305 | 	}
  306 | 
  307 | 	/**
  308 | 	 * Set the global filters to use in order to support {@link JsonFilter @JsonFilter} annotated POJO.
  309 | 	 * @since 4.2
  310 | 	 * @see MappingJacksonValue#setFilters(FilterProvider)
  311 | 	 */
  312 | 	public Jackson2ObjectMapperBuilder filters(FilterProvider filters) {
  313 | 		this.filters = filters;
  314 | 		return this;
  315 | 	}
  316 | 
  317 | 	/**
  318 | 	 * Add mix-in annotations to use for augmenting specified class or interface.
  319 | 	 * @param target class (or interface) whose annotations to effectively override
  320 | 	 * @param mixinSource class (or interface) whose annotations are to be "added"
  321 | 	 * to target's annotations as value
  322 | 	 * @since 4.1.2
  323 | 	 * @see com.fasterxml.jackson.databind.ObjectMapper#addMixIn(Class, Class)
  324 | 	 */
  325 | 	public Jackson2ObjectMapperBuilder mixIn(Class<?> target, Class<?> mixinSource) {
  326 | 		this.mixIns.put(target, mixinSource);
  327 | 		return this;
  328 | 	}
  329 | 
  330 | 	/**
  331 | 	 * Add mix-in annotations to use for augmenting specified class or interface.
  332 | 	 * @param mixIns a Map of entries with target classes (or interface) whose annotations
  333 | 	 * to effectively override as key and mix-in classes (or interface) whose
  334 | 	 * annotations are to be "added" to target's annotations as value.
  335 | 	 * @since 4.1.2
  336 | 	 * @see com.fasterxml.jackson.databind.ObjectMapper#addMixIn(Class, Class)
  337 | 	 */
  338 | 	public Jackson2ObjectMapperBuilder mixIns(Map<Class<?>, Class<?>> mixIns) {
  339 | 		this.mixIns.putAll(mixIns);
  340 | 		return this;
  341 | 	}
  342 | 
  343 | 	/**
  344 | 	 * Configure custom serializers. Each serializer is registered for the type
  345 | 	 * returned by {@link JsonSerializer#handledType()}, which must not be {@code null}.
  346 | 	 * @see #serializersByType(Map)
  347 | 	 */
  348 | 	public Jackson2ObjectMapperBuilder serializers(JsonSerializer<?>... serializers) {
  349 | 		for (JsonSerializer<?> serializer : serializers) {
  350 | 			Class<?> handledType = serializer.handledType();
  351 | 			if (handledType == null || handledType == Object.class) {
  352 | 				throw new IllegalArgumentException("Unknown handled type in " + serializer.getClass().getName());
  353 | 			}
  354 | 			this.serializers.put(serializer.handledType(), serializer);
  355 | 		}
  356 | 		return this;
  357 | 	}
  358 | 
  359 | 	/**
  360 | 	 * Configure a custom serializer for the given type.
  361 | 	 * @since 4.1.2
  362 | 	 * @see #serializers(JsonSerializer...)
  363 | 	 */
  364 | 	public Jackson2ObjectMapperBuilder serializerByType(Class<?> type, JsonSerializer<?> serializer) {
  365 | 		this.serializers.put(type, serializer);
  366 | 		return this;
  367 | 	}
  368 | 
  369 | 	/**
  370 | 	 * Configure custom serializers for the given types.
  371 | 	 * @see #serializers(JsonSerializer...)
  372 | 	 */
  373 | 	public Jackson2ObjectMapperBuilder serializersByType(Map<Class<?>, JsonSerializer<?>> serializers) {
  374 | 		this.serializers.putAll(serializers);
  375 | 		return this;
  376 | 	}
  377 | 
  378 | 	/**
  379 | 	 * Configure custom deserializers. Each deserializer is registered for the type
  380 | 	 * returned by {@link JsonDeserializer#handledType()}, which must not be {@code null}.
  381 | 	 * @since 4.3
  382 | 	 * @see #deserializersByType(Map)
  383 | 	 */
  384 | 	public Jackson2ObjectMapperBuilder deserializers(JsonDeserializer<?>... deserializers) {
  385 | 		for (JsonDeserializer<?> deserializer : deserializers) {
  386 | 			Class<?> handledType = deserializer.handledType();
  387 | 			if (handledType == null || handledType == Object.class) {
  388 | 				throw new IllegalArgumentException("Unknown handled type in " + deserializer.getClass().getName());
  389 | 			}
  390 | 			this.deserializers.put(deserializer.handledType(), deserializer);
  391 | 		}
  392 | 		return this;
  393 | 	}
  394 | 
  395 | 	/**
  396 | 	 * Configure a custom deserializer for the given type.
  397 | 	 * @since 4.1.2
  398 | 	 */
  399 | 	public Jackson2ObjectMapperBuilder deserializerByType(Class<?> type, JsonDeserializer<?> deserializer) {
  400 | 		this.deserializers.put(type, deserializer);
  401 | 		return this;
  402 | 	}
  403 | 
  404 | 	/**
  405 | 	 * Configure custom deserializers for the given types.
  406 | 	 */
  407 | 	public Jackson2ObjectMapperBuilder deserializersByType(Map<Class<?>, JsonDeserializer<?>> deserializers) {
  408 | 		this.deserializers.putAll(deserializers);
  409 | 		return this;
  410 | 	}
  411 | 
  412 | 	/**
  413 | 	 * Shortcut for {@link MapperFeature#AUTO_DETECT_FIELDS} option.
  414 | 	 */
  415 | 	public Jackson2ObjectMapperBuilder autoDetectFields(boolean autoDetectFields) {
  416 | 		this.features.put(MapperFeature.AUTO_DETECT_FIELDS, autoDetectFields);
  417 | 		return this;
  418 | 	}
  419 | 
  420 | 	/**
  421 | 	 * Shortcut for {@link MapperFeature#AUTO_DETECT_SETTERS}/
  422 | 	 * {@link MapperFeature#AUTO_DETECT_GETTERS}/{@link MapperFeature#AUTO_DETECT_IS_GETTERS}
  423 | 	 * options.
  424 | 	 */
  425 | 	public Jackson2ObjectMapperBuilder autoDetectGettersSetters(boolean autoDetectGettersSetters) {
  426 | 		this.features.put(MapperFeature.AUTO_DETECT_GETTERS, autoDetectGettersSetters);
  427 | 		this.features.put(MapperFeature.AUTO_DETECT_SETTERS, autoDetectGettersSetters);
  428 | 		this.features.put(MapperFeature.AUTO_DETECT_IS_GETTERS, autoDetectGettersSetters);
  429 | 		return this;
  430 | 	}
  431 | 
  432 | 	/**
  433 | 	 * Shortcut for {@link MapperFeature#DEFAULT_VIEW_INCLUSION} option.
  434 | 	 */
  435 | 	public Jackson2ObjectMapperBuilder defaultViewInclusion(boolean defaultViewInclusion) {
  436 | 		this.features.put(MapperFeature.DEFAULT_VIEW_INCLUSION, defaultViewInclusion);
  437 | 		return this;
  438 | 	}
  439 | 
  440 | 	/**
  441 | 	 * Shortcut for {@link DeserializationFeature#FAIL_ON_UNKNOWN_PROPERTIES} option.
  442 | 	 */
  443 | 	public Jackson2ObjectMapperBuilder failOnUnknownProperties(boolean failOnUnknownProperties) {
  444 | 		this.features.put(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, failOnUnknownProperties);
  445 | 		return this;
  446 | 	}
  447 | 
  448 | 	/**
  449 | 	 * Shortcut for {@link SerializationFeature#FAIL_ON_EMPTY_BEANS} option.
  450 | 	 */
  451 | 	public Jackson2ObjectMapperBuilder failOnEmptyBeans(boolean failOnEmptyBeans) {
  452 | 		this.features.put(SerializationFeature.FAIL_ON_EMPTY_BEANS, failOnEmptyBeans);
  453 | 		return this;
  454 | 	}
  455 | 
  456 | 	/**
  457 | 	 * Shortcut for {@link SerializationFeature#INDENT_OUTPUT} option.
  458 | 	 */
  459 | 	public Jackson2ObjectMapperBuilder indentOutput(boolean indentOutput) {
  460 | 		this.features.put(SerializationFeature.INDENT_OUTPUT, indentOutput);
  461 | 		return this;
  462 | 	}
  463 | 
  464 | 	/**
  465 | 	 * Define if a wrapper will be used for indexed (List, array) properties or not by
  466 | 	 * default (only applies to {@link XmlMapper}).
  467 | 	 * @since 4.3
  468 | 	 */
  469 | 	public Jackson2ObjectMapperBuilder defaultUseWrapper(boolean defaultUseWrapper) {
  470 | 		this.defaultUseWrapper = defaultUseWrapper;
  471 | 		return this;
  472 | 	}
  473 | 
  474 | 	/**
  475 | 	 * Specify visibility to limit what kind of properties are auto-detected.
  476 | 	 * @since 5.1
  477 | 	 * @see com.fasterxml.jackson.annotation.PropertyAccessor
  478 | 	 * @see com.fasterxml.jackson.annotation.JsonAutoDetect.Visibility
  479 | 	 */
  480 | 	public Jackson2ObjectMapperBuilder visibility(PropertyAccessor accessor, JsonAutoDetect.Visibility visibility) {
  481 | 		this.visibilities.put(accessor, visibility);
  482 | 		return this;
  483 | 	}
  484 | 
  485 | 	/**
  486 | 	 * Specify features to enable.
  487 | 	 * @see com.fasterxml.jackson.core.JsonParser.Feature
  488 | 	 * @see com.fasterxml.jackson.core.JsonGenerator.Feature
  489 | 	 * @see com.fasterxml.jackson.databind.SerializationFeature
  490 | 	 * @see com.fasterxml.jackson.databind.DeserializationFeature
  491 | 	 * @see com.fasterxml.jackson.databind.MapperFeature
  492 | 	 */
  493 | 	public Jackson2ObjectMapperBuilder featuresToEnable(Object... featuresToEnable) {
  494 | 		for (Object feature : featuresToEnable) {
  495 | 			this.features.put(feature, Boolean.TRUE);
  496 | 		}
  497 | 		return this;
  498 | 	}
  499 | 
  500 | 	/**
  501 | 	 * Specify features to disable.
  502 | 	 * @see com.fasterxml.jackson.core.JsonParser.Feature
  503 | 	 * @see com.fasterxml.jackson.core.JsonGenerator.Feature
  504 | 	 * @see com.fasterxml.jackson.databind.SerializationFeature
  505 | 	 * @see com.fasterxml.jackson.databind.DeserializationFeature
  506 | 	 * @see com.fasterxml.jackson.databind.MapperFeature
  507 | 	 */
  508 | 	public Jackson2ObjectMapperBuilder featuresToDisable(Object... featuresToDisable) {
  509 | 		for (Object feature : featuresToDisable) {
  510 | 			this.features.put(feature, Boolean.FALSE);
  511 | 		}
  512 | 		return this;
  513 | 	}
  514 | 
  515 | 	/**
  516 | 	 * Specify the modules to be registered with the {@link ObjectMapper}.
  517 | 	 * <p>Multiple invocations are not additive; the last one defines the modules to
  518 | 	 * register.
  519 | 	 * <p>Note: If this is set, autodetection of modules will not occur &mdash; not
  520 | 	 * by Jackson, and not by Spring either (see {@link #findModulesViaServiceLoader}).
  521 | 	 * As a consequence, specifying an empty list here will suppress any kind of
  522 | 	 * module detection.
  523 | 	 * <p>Specify either this or {@link #modulesToInstall}, not both.
  524 | 	 * @since 4.1.5
  525 | 	 * @see #modules(List)
  526 | 	 * @see com.fasterxml.jackson.databind.Module
  527 | 	 */
  528 | 	public Jackson2ObjectMapperBuilder modules(Module... modules) {
  529 | 		return modules(Arrays.asList(modules));
  530 | 	}
  531 | 
  532 | 	/**
  533 | 	 * Variant of {@link #modules(Module...)} with a {@link List}.
  534 | 	 * @see #modules(Module...)
  535 | 	 * @see #modules(Consumer)
  536 | 	 * @see com.fasterxml.jackson.databind.Module
  537 | 	 */
  538 | 	public Jackson2ObjectMapperBuilder modules(List<Module> modules) {
  539 | 		this.modules = new ArrayList<>(modules);
  540 | 		this.findModulesViaServiceLoader = false;
  541 | 		this.findWellKnownModules = false;
  542 | 		return this;
  543 | 	}
  544 | 
  545 | 	/**
  546 | 	 * Variant of {@link #modules(Module...)} with a {@link Consumer} for full
  547 | 	 * control over the underlying list of modules.
  548 | 	 * @since 5.3.22
  549 | 	 * @see #modules(Module...)
  550 | 	 * @see #modules(List)
  551 | 	 * @see com.fasterxml.jackson.databind.Module
  552 | 	 */
  553 | 	public Jackson2ObjectMapperBuilder modules(Consumer<List<Module>> consumer) {
  554 | 		this.modules = (this.modules != null ? this.modules : new ArrayList<>());
  555 | 		this.findModulesViaServiceLoader = false;
  556 | 		this.findWellKnownModules = false;
  557 | 		consumer.accept(this.modules);
  558 | 		return this;
  559 | 	}
  560 | 
  561 | 	/**
  562 | 	 * Specify one or more modules to be registered with the {@link ObjectMapper}.
  563 | 	 * <p>Multiple invocations are not additive; the last one defines the modules
  564 | 	 * to register.
  565 | 	 * <p>Modules specified here will be registered after Spring's autodetection of
  566 | 	 * JSR-310, or Jackson's finding of modules (see {@link #findModulesViaServiceLoader}),
  567 | 	 * allowing to eventually override their configuration.
  568 | 	 * <p>Specify either this or {@link #modules(Module...)}, not both.
  569 | 	 * @since 4.1.5
  570 | 	 * @see #modulesToInstall(Consumer)
  571 | 	 * @see #modulesToInstall(Class...)
  572 | 	 * @see com.fasterxml.jackson.databind.Module
  573 | 	 */
  574 | 	public Jackson2ObjectMapperBuilder modulesToInstall(Module... modules) {
  575 | 		this.modules = new ArrayList<>(Arrays.asList(modules));
  576 | 		this.findWellKnownModules = true;
  577 | 		return this;
  578 | 	}
  579 | 
  580 | 	/**
  581 | 	 * Variant of {@link #modulesToInstall(Module...)} with a {@link Consumer}
  582 | 	 * for full control over the underlying list of modules.
  583 | 	 * @since 5.3.22
  584 | 	 * @see #modulesToInstall(Module...)
  585 | 	 * @see #modulesToInstall(Class...)
  586 | 	 * @see com.fasterxml.jackson.databind.Module
  587 | 	 */
  588 | 	public Jackson2ObjectMapperBuilder modulesToInstall(Consumer<List<Module>> consumer) {
  589 | 		this.modules = (this.modules != null ? this.modules : new ArrayList<>());
  590 | 		this.findWellKnownModules = true;
  591 | 		consumer.accept(this.modules);
  592 | 		return this;
  593 | 	}
  594 | 
  595 | 	/**
  596 | 	 * Specify one or more modules by class to be registered with the {@link ObjectMapper}.
  597 | 	 * <p>Multiple invocations are not additive; the last one defines the modules
  598 | 	 * to register.
  599 | 	 * <p>Modules specified here will be registered after Spring's autodetection of
  600 | 	 * JSR-310, or Jackson's finding of modules (see {@link #findModulesViaServiceLoader}),
  601 | 	 * allowing to eventually override their configuration.
  602 | 	 * <p>Specify either this or {@link #modules(Module...)}, not both.
  603 | 	 * @see #modulesToInstall(Module...)
  604 | 	 * @see #modulesToInstall(Consumer)
  605 | 	 * @see com.fasterxml.jackson.databind.Module
  606 | 	 */
  607 | 	@SafeVarargs
  608 | 	@SuppressWarnings("varargs")
  609 | 	public final Jackson2ObjectMapperBuilder modulesToInstall(Class<? extends Module>... modules) {
  610 | 		this.moduleClasses = modules;
  611 | 		this.findWellKnownModules = true;
  612 | 		return this;
  613 | 	}
  614 | 
  615 | 	/**
  616 | 	 * Set whether to let Jackson find available modules via the JDK ServiceLoader,
  617 | 	 * based on META-INF metadata in the classpath.
  618 | 	 * <p>If this mode is not set, Spring's Jackson2ObjectMapperBuilder itself
  619 | 	 * will try to find the JSR-310 support module on the classpath.
  620 | 	 * @see com.fasterxml.jackson.databind.ObjectMapper#findModules()
  621 | 	 */
  622 | 	public Jackson2ObjectMapperBuilder findModulesViaServiceLoader(boolean findModules) {
  623 | 		this.findModulesViaServiceLoader = findModules;
  624 | 		return this;
  625 | 	}
  626 | 
  627 | 	/**
  628 | 	 * Set the ClassLoader to use for loading Jackson extension modules.
  629 | 	 */
  630 | 	public Jackson2ObjectMapperBuilder moduleClassLoader(ClassLoader moduleClassLoader) {
  631 | 		this.moduleClassLoader = moduleClassLoader;
  632 | 		return this;
  633 | 	}
  634 | 
  635 | 	/**
  636 | 	 * Customize the construction of Jackson handlers ({@link JsonSerializer}, {@link JsonDeserializer},
  637 | 	 * {@link KeyDeserializer}, {@code TypeResolverBuilder} and {@code TypeIdResolver}).
  638 | 	 * @since 4.1.3
  639 | 	 * @see Jackson2ObjectMapperBuilder#applicationContext(ApplicationContext)
  640 | 	 */
  641 | 	public Jackson2ObjectMapperBuilder handlerInstantiator(HandlerInstantiator handlerInstantiator) {
  642 | 		this.handlerInstantiator = handlerInstantiator;
  643 | 		return this;
  644 | 	}
  645 | 
  646 | 	/**
  647 | 	 * Set the Spring {@link ApplicationContext} in order to autowire Jackson handlers ({@link JsonSerializer},
  648 | 	 * {@link JsonDeserializer}, {@link KeyDeserializer}, {@code TypeResolverBuilder} and {@code TypeIdResolver}).
  649 | 	 * @since 4.1.3
  650 | 	 * @see SpringHandlerInstantiator
  651 | 	 */
  652 | 	public Jackson2ObjectMapperBuilder applicationContext(ApplicationContext applicationContext) {
  653 | 		this.applicationContext = applicationContext;
  654 | 		return this;
  655 | 	}
  656 | 
  657 | 	/**
  658 | 	 * An option to apply additional customizations directly to the
  659 | 	 * {@code ObjectMapper} instances at the end, after all other config
  660 | 	 * properties of the builder have been applied.
  661 | 	 * @param configurer a configurer to apply. If several configurers are
  662 | 	 * registered, they will get applied in their registration order.
  663 | 	 * @since 5.3
  664 | 	 */
  665 | 	public Jackson2ObjectMapperBuilder postConfigurer(Consumer<ObjectMapper> configurer) {
  666 | 		this.configurer = (this.configurer != null ? this.configurer.andThen(configurer) : configurer);
  667 | 		return this;
  668 | 	}
  669 | 
  670 | 
  671 | 	/**
  672 | 	 * Build a new {@link ObjectMapper} instance.
  673 | 	 * <p>Each build operation produces an independent {@link ObjectMapper} instance.
  674 | 	 * The builder's settings can get modified, with a subsequent build operation
  675 | 	 * then producing a new {@link ObjectMapper} based on the most recent settings.
  676 | 	 * @return the newly built ObjectMapper
  677 | 	 */
  678 | 	@SuppressWarnings("unchecked")
  679 | 	public <T extends ObjectMapper> T build() {
  680 | 		ObjectMapper mapper;
  681 | 		if (this.createXmlMapper) {
  682 | 			mapper = (this.defaultUseWrapper != null ?
  683 | 					new XmlObjectMapperInitializer().create(this.defaultUseWrapper, this.factory) :
  684 | 					new XmlObjectMapperInitializer().create(this.factory));
  685 | 		}
  686 | 		else {
  687 | 			mapper = (this.factory != null ? new ObjectMapper(this.factory) : new ObjectMapper());
  688 | 		}
  689 | 		configure(mapper);
  690 | 		return (T) mapper;
  691 | 	}
  692 | 
  693 | 	/**
  694 | 	 * Configure an existing {@link ObjectMapper} instance with this builder's
  695 | 	 * settings. This can be applied to any number of {@code ObjectMappers}.
  696 | 	 * @param objectMapper the ObjectMapper to configure
  697 | 	 */
  698 | 	public void configure(ObjectMapper objectMapper) {
  699 | 		Assert.notNull(objectMapper, "ObjectMapper must not be null");
  700 | 
  701 | 		MultiValueMap<Object, Module> modulesToRegister = new LinkedMultiValueMap<>();
  702 | 		if (this.findModulesViaServiceLoader) {
  703 | 			ObjectMapper.findModules(this.moduleClassLoader).forEach(module -> registerModule(module, modulesToRegister));
  704 | 		}
  705 | 		else if (this.findWellKnownModules) {
  706 | 			registerWellKnownModulesIfAvailable(modulesToRegister);
  707 | 		}
  708 | 
  709 | 		if (this.modules != null) {
  710 | 			this.modules.forEach(module -> registerModule(module, modulesToRegister));
  711 | 		}
  712 | 		if (this.moduleClasses != null) {
  713 | 			for (Class<? extends Module> moduleClass : this.moduleClasses) {
  714 | 				registerModule(BeanUtils.instantiateClass(moduleClass), modulesToRegister);
  715 | 			}
  716 | 		}
  717 | 		List<Module> modules = new ArrayList<>();
  718 | 		for (List<Module> nestedModules : modulesToRegister.values()) {
  719 | 			modules.addAll(nestedModules);
  720 | 		}
  721 | 		objectMapper.registerModules(modules);
  722 | 
  723 | 		if (this.dateFormat != null) {
  724 | 			objectMapper.setDateFormat(this.dateFormat);
  725 | 		}
  726 | 		if (this.locale != null) {
  727 | 			objectMapper.setLocale(this.locale);
  728 | 		}
  729 | 		if (this.timeZone != null) {
  730 | 			objectMapper.setTimeZone(this.timeZone);
  731 | 		}
  732 | 
  733 | 		if (this.annotationIntrospector != null) {
  734 | 			objectMapper.setAnnotationIntrospector(this.annotationIntrospector);
  735 | 		}
  736 | 		if (this.propertyNamingStrategy != null) {
  737 | 			objectMapper.setPropertyNamingStrategy(this.propertyNamingStrategy);
  738 | 		}
  739 | 		if (this.defaultTyping != null) {
  740 | 			objectMapper.setDefaultTyping(this.defaultTyping);
  741 | 		}
  742 | 		if (this.serializationInclusion != null) {
  743 | 			objectMapper.setDefaultPropertyInclusion(this.serializationInclusion);
  744 | 		}
  745 | 
  746 | 		if (this.filters != null) {
  747 | 			objectMapper.setFilterProvider(this.filters);
  748 | 		}
  749 | 
  750 | 		if (JACKSON_2_XML_PRESENT) {
  751 | 			objectMapper.addMixIn(ProblemDetail.class, ProblemDetailJacksonXmlMixin.class);
  752 | 		}
  753 | 		else {
  754 | 			objectMapper.addMixIn(ProblemDetail.class, ProblemDetailJacksonMixin.class);
  755 | 		}
  756 | 		this.mixIns.forEach(objectMapper::addMixIn);
  757 | 
  758 | 		if (!this.serializers.isEmpty() || !this.deserializers.isEmpty()) {
  759 | 			SimpleModule module = new SimpleModule();
  760 | 			addSerializers(module);
  761 | 			addDeserializers(module);
  762 | 			objectMapper.registerModule(module);
  763 | 		}
  764 | 
  765 | 		this.visibilities.forEach(objectMapper::setVisibility);
  766 | 
  767 | 		customizeDefaultFeatures(objectMapper);
  768 | 		this.features.forEach((feature, enabled) -> configureFeature(objectMapper, feature, enabled));
  769 | 
  770 | 		if (this.handlerInstantiator != null) {
  771 | 			objectMapper.setHandlerInstantiator(this.handlerInstantiator);
  772 | 		}
  773 | 		else if (this.applicationContext != null) {
  774 | 			objectMapper.setHandlerInstantiator(
  775 | 					new SpringHandlerInstantiator(this.applicationContext.getAutowireCapableBeanFactory()));
  776 | 		}
  777 | 
  778 | 		if (this.configurer != null) {
  779 | 			this.configurer.accept(objectMapper);
  780 | 		}
  781 | 	}
  782 | 
  783 | 	private void registerModule(Module module, MultiValueMap<Object, Module> modulesToRegister) {
  784 | 		if (module.getTypeId() == null) {
  785 | 			modulesToRegister.add(SimpleModule.class.getName(), module);
  786 | 		}
  787 | 		else {
  788 | 			modulesToRegister.set(module.getTypeId(), module);
  789 | 		}
  790 | 	}
  791 | 
  792 | 
  793 | 	// Any change to this method should be also applied to spring-jms and spring-messaging
  794 | 	// MappingJackson2MessageConverter default constructors
  795 | 	private void customizeDefaultFeatures(ObjectMapper objectMapper) {
  796 | 		if (!this.features.containsKey(MapperFeature.DEFAULT_VIEW_INCLUSION)) {
  797 | 			configureFeature(objectMapper, MapperFeature.DEFAULT_VIEW_INCLUSION, false);
  798 | 		}
  799 | 		if (!this.features.containsKey(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES)) {
  800 | 			configureFeature(objectMapper, DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
  801 | 		}
  802 | 	}
  803 | 
  804 | 	@SuppressWarnings("unchecked")
  805 | 	private <T> void addSerializers(SimpleModule module) {
  806 | 		this.serializers.forEach((type, serializer) ->
  807 | 				module.addSerializer((Class<? extends T>) type, (JsonSerializer<T>) serializer));
  808 | 	}
  809 | 
  810 | 	@SuppressWarnings("unchecked")
  811 | 	private <T> void addDeserializers(SimpleModule module) {
  812 | 		this.deserializers.forEach((type, deserializer) ->
  813 | 				module.addDeserializer((Class<T>) type, (JsonDeserializer<? extends T>) deserializer));
  814 | 	}
  815 | 
  816 | 	@SuppressWarnings("deprecation")  // on Jackson 2.13: configure(MapperFeature, boolean)
  817 | 	private void configureFeature(ObjectMapper objectMapper, Object feature, boolean enabled) {
  818 | 		if (feature instanceof JsonParser.Feature jsonParserFeature) {
  819 | 			objectMapper.configure(jsonParserFeature, enabled);
  820 | 		}
  821 | 		else if (feature instanceof JsonGenerator.Feature jsonGeneratorFeature) {
  822 | 			objectMapper.configure(jsonGeneratorFeature, enabled);
  823 | 		}
  824 | 		else if (feature instanceof SerializationFeature serializationFeature) {
  825 | 			objectMapper.configure(serializationFeature, enabled);
  826 | 		}
  827 | 		else if (feature instanceof DeserializationFeature deserializationFeature) {
  828 | 			objectMapper.configure(deserializationFeature, enabled);
  829 | 		}
  830 | 		else if (feature instanceof DatatypeFeature datatypeFeature) {
  831 | 			objectMapper.configure(datatypeFeature, enabled);
  832 | 		}
  833 | 		else if (feature instanceof MapperFeature mapperFeature) {
  834 | 			objectMapper.configure(mapperFeature, enabled);
  835 | 		}
  836 | 		else {
  837 | 			throw new IllegalArgumentException("Unknown feature class: " + feature.getClass().getName());
  838 | 		}
  839 | 	}
  840 | 
  841 | 	@SuppressWarnings("unchecked")
  842 | 	private void registerWellKnownModulesIfAvailable(MultiValueMap<Object, Module> modulesToRegister) {
  843 | 		try {
  844 | 			Class<? extends Module> jdk8ModuleClass = (Class<? extends Module>)
  845 | 					ClassUtils.forName("com.fasterxml.jackson.datatype.jdk8.Jdk8Module", this.moduleClassLoader);
  846 | 			Module jdk8Module = BeanUtils.instantiateClass(jdk8ModuleClass);
  847 | 			modulesToRegister.set(jdk8Module.getTypeId(), jdk8Module);
  848 | 		}
  849 | 		catch (ClassNotFoundException ex) {
  850 | 			// jackson-datatype-jdk8 not available
  851 | 		}
  852 | 
  853 | 		try {
  854 | 			Class<? extends Module> parameterNamesModuleClass = (Class<? extends Module>)
  855 | 					ClassUtils.forName("com.fasterxml.jackson.module.paramnames.ParameterNamesModule", this.moduleClassLoader);
  856 | 			Module parameterNamesModule = BeanUtils.instantiateClass(parameterNamesModuleClass);
  857 | 			modulesToRegister.set(parameterNamesModule.getTypeId(), parameterNamesModule);
  858 | 		}
  859 | 		catch (ClassNotFoundException ex) {
  860 | 			// jackson-module-parameter-names not available
  861 | 		}
  862 | 
  863 | 		try {
  864 | 			Class<? extends Module> javaTimeModuleClass = (Class<? extends Module>)
  865 | 					ClassUtils.forName("com.fasterxml.jackson.datatype.jsr310.JavaTimeModule", this.moduleClassLoader);
  866 | 			Module javaTimeModule = BeanUtils.instantiateClass(javaTimeModuleClass);
  867 | 			modulesToRegister.set(javaTimeModule.getTypeId(), javaTimeModule);
  868 | 		}
  869 | 		catch (ClassNotFoundException ex) {
  870 | 			// jackson-datatype-jsr310 not available
  871 | 		}
  872 | 
  873 | 		if (KotlinDetector.isKotlinReflectPresent()) {
  874 | 			try {
  875 | 				Class<? extends Module> kotlinModuleClass = (Class<? extends Module>)
  876 | 						ClassUtils.forName("com.fasterxml.jackson.module.kotlin.KotlinModule", this.moduleClassLoader);
  877 | 				Module kotlinModule = BeanUtils.instantiateClass(kotlinModuleClass);
  878 | 				modulesToRegister.set(kotlinModule.getTypeId(), kotlinModule);
  879 | 			}
  880 | 			catch (ClassNotFoundException ex) {
  881 | 				// jackson-module-kotlin not available
  882 | 			}
  883 | 		}
  884 | 	}
  885 | 
  886 | 
  887 | 	// Convenience factory methods
  888 | 
  889 | 	/**
  890 | 	 * Obtain a {@link Jackson2ObjectMapperBuilder} instance in order to
  891 | 	 * build a regular JSON {@link ObjectMapper} instance.
  892 | 	 */
  893 | 	public static Jackson2ObjectMapperBuilder json() {
  894 | 		return new Jackson2ObjectMapperBuilder();
  895 | 	}
  896 | 
  897 | 	/**
  898 | 	 * Obtain a {@link Jackson2ObjectMapperBuilder} instance in order to
  899 | 	 * build an {@link XmlMapper} instance.
  900 | 	 */
  901 | 	public static Jackson2ObjectMapperBuilder xml() {
  902 | 		return new Jackson2ObjectMapperBuilder().createXmlMapper(true);
  903 | 	}
  904 | 
  905 | 	/**
  906 | 	 * Obtain a {@link Jackson2ObjectMapperBuilder} instance in order to
  907 | 	 * build a Smile data format {@link ObjectMapper} instance.
  908 | 	 * @since 5.0
  909 | 	 */
  910 | 	public static Jackson2ObjectMapperBuilder smile() {
  911 | 		return new Jackson2ObjectMapperBuilder().factory(new SmileFactoryInitializer().create());
  912 | 	}
  913 | 
  914 | 	/**
  915 | 	 * Obtain a {@link Jackson2ObjectMapperBuilder} instance in order to
  916 | 	 * build a CBOR data format {@link ObjectMapper} instance.
  917 | 	 * @since 5.0
  918 | 	 */
  919 | 	public static Jackson2ObjectMapperBuilder cbor() {
  920 | 		return new Jackson2ObjectMapperBuilder().factory(new CborFactoryInitializer().create());
  921 | 	}
  922 | 
  923 | 	/**
  924 | 	 * Obtain a {@link Jackson2ObjectMapperBuilder} instance in order to
  925 | 	 * build a YAML data format {@link ObjectMapper} instance.
  926 | 	 * @since 6.2
  927 | 	 */
  928 | 	public static Jackson2ObjectMapperBuilder yaml() {
  929 | 		return new Jackson2ObjectMapperBuilder().factory(new YamlFactoryInitializer().create());
  930 | 	}
  931 | 
  932 | 
  933 | 	private static class XmlObjectMapperInitializer {
  934 | 
  935 | 		public ObjectMapper create(@Nullable JsonFactory factory) {
  936 | 			if (factory != null) {
  937 | 				return new XmlMapper((XmlFactory) factory);
  938 | 			}
  939 | 			else {
  940 | 				return new XmlMapper(StaxUtils.createDefensiveInputFactory());
  941 | 			}
  942 | 		}
  943 | 
  944 | 		public ObjectMapper create(boolean defaultUseWrapper, @Nullable JsonFactory factory) {
  945 | 			JacksonXmlModule module = new JacksonXmlModule();
  946 | 			module.setDefaultUseWrapper(defaultUseWrapper);
  947 | 			if (factory != null) {
  948 | 				return new XmlMapper((XmlFactory) factory, module);
  949 | 			}
  950 | 			else {
  951 | 				return new XmlMapper(new XmlFactory(StaxUtils.createDefensiveInputFactory()), module);
  952 | 			}
  953 | 		}
  954 | 	}
  955 | 
  956 | 
  957 | 	private static class SmileFactoryInitializer {
  958 | 
  959 | 		public JsonFactory create() {
  960 | 			return new SmileFactory();
  961 | 		}
  962 | 	}
  963 | 
  964 | 
  965 | 	private static class CborFactoryInitializer {
  966 | 
  967 | 		public JsonFactory create() {
  968 | 			return new CBORFactory();
  969 | 		}
  970 | 	}
  971 | 
  972 | 	private static class YamlFactoryInitializer {
  973 | 
  974 | 		public JsonFactory create() {
  975 | 			return new YAMLFactory();
  976 | 		}
  977 | 	}
  978 | 
  979 | }
```
