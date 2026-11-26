<!-- learning-docs source archive（机械抓取原文，禁止手改代码内容） -->
<!-- origin: org.springframework:spring-web:7.1.0 | path: org/springframework/http/converter/json/AbstractJackson2HttpMessageConverter.java -->
<!-- 正文引用：[[源码:AbstractJackson2HttpMessageConverter.java#L起-L止]] ；代码块头部：(src: AbstractJackson2HttpMessageConverter.java#L起-L止) -->

# AbstractJackson2HttpMessageConverter.java

| 项 | 值 |
| --- | --- |
| 来源 | `org.springframework:spring-web:7.1.0` |
| 路径 | `org/springframework/http/converter/json/AbstractJackson2HttpMessageConverter.java` |
| 行数 | 583 |
| 在线地址 | https://repo1.maven.org/maven2/org/springframework/spring-web/7.1.0/spring-web-7.1.0-sources.jar |
| 抓取时间 | 2026-11-25T14:01:25.325Z |

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
   19 | import java.io.IOException;
   20 | import java.io.InputStream;
   21 | import java.io.InputStreamReader;
   22 | import java.io.OutputStream;
   23 | import java.io.Reader;
   24 | import java.lang.reflect.Type;
   25 | import java.nio.charset.Charset;
   26 | import java.nio.charset.StandardCharsets;
   27 | import java.util.ArrayList;
   28 | import java.util.Arrays;
   29 | import java.util.Collections;
   30 | import java.util.LinkedHashMap;
   31 | import java.util.List;
   32 | import java.util.Map;
   33 | import java.util.Optional;
   34 | import java.util.concurrent.atomic.AtomicReference;
   35 | import java.util.function.Consumer;
   36 | 
   37 | import com.fasterxml.jackson.core.JsonEncoding;
   38 | import com.fasterxml.jackson.core.JsonGenerator;
   39 | import com.fasterxml.jackson.core.JsonProcessingException;
   40 | import com.fasterxml.jackson.core.PrettyPrinter;
   41 | import com.fasterxml.jackson.core.util.DefaultIndenter;
   42 | import com.fasterxml.jackson.core.util.DefaultPrettyPrinter;
   43 | import com.fasterxml.jackson.databind.JavaType;
   44 | import com.fasterxml.jackson.databind.JsonMappingException;
   45 | import com.fasterxml.jackson.databind.ObjectMapper;
   46 | import com.fasterxml.jackson.databind.ObjectReader;
   47 | import com.fasterxml.jackson.databind.ObjectWriter;
   48 | import com.fasterxml.jackson.databind.SerializationConfig;
   49 | import com.fasterxml.jackson.databind.SerializationFeature;
   50 | import com.fasterxml.jackson.databind.exc.InvalidDefinitionException;
   51 | import com.fasterxml.jackson.databind.ser.FilterProvider;
   52 | import org.jspecify.annotations.Nullable;
   53 | 
   54 | import org.springframework.core.GenericTypeResolver;
   55 | import org.springframework.http.HttpInputMessage;
   56 | import org.springframework.http.HttpOutputMessage;
   57 | import org.springframework.http.MediaType;
   58 | import org.springframework.http.ProblemDetail;
   59 | import org.springframework.http.converter.AbstractGenericHttpMessageConverter;
   60 | import org.springframework.http.converter.AbstractJacksonHttpMessageConverter;
   61 | import org.springframework.http.converter.HttpMessageConversionException;
   62 | import org.springframework.http.converter.HttpMessageConverter;
   63 | import org.springframework.http.converter.HttpMessageNotReadableException;
   64 | import org.springframework.http.converter.HttpMessageNotWritableException;
   65 | import org.springframework.util.Assert;
   66 | import org.springframework.util.CollectionUtils;
   67 | import org.springframework.util.StreamUtils;
   68 | import org.springframework.util.TypeUtils;
   69 | 
   70 | /**
   71 |  * Abstract base class for Jackson based and content type independent
   72 |  * {@link HttpMessageConverter} implementations.
   73 |  *
   74 |  * @author Arjen Poutsma
   75 |  * @author Keith Donald
   76 |  * @author Rossen Stoyanchev
   77 |  * @author Juergen Hoeller
   78 |  * @author Sebastien Deleuze
   79 |  * @author Sam Brannen
   80 |  * @since 4.1
   81 |  * @see MappingJackson2HttpMessageConverter
   82 |  * @deprecated since 7.0 in favor of {@link AbstractJacksonHttpMessageConverter}
   83 |  */
   84 | @Deprecated(since = "7.0", forRemoval = true)
   85 | @SuppressWarnings("removal")
   86 | public abstract class AbstractJackson2HttpMessageConverter extends AbstractGenericHttpMessageConverter<Object> {
   87 | 
   88 | 	private static final Map<String, JsonEncoding> ENCODINGS;
   89 | 
   90 | 	static {
   91 | 		ENCODINGS = CollectionUtils.newHashMap(JsonEncoding.values().length);
   92 | 		for (JsonEncoding encoding : JsonEncoding.values()) {
   93 | 			ENCODINGS.put(encoding.getJavaName(), encoding);
   94 | 		}
   95 | 		ENCODINGS.put("US-ASCII", JsonEncoding.UTF8);
   96 | 	}
   97 | 
   98 | 
   99 | 	protected ObjectMapper defaultObjectMapper;
  100 | 
  101 | 	private @Nullable Map<Class<?>, Map<MediaType, ObjectMapper>> objectMapperRegistrations;
  102 | 
  103 | 	private @Nullable Boolean prettyPrint;
  104 | 
  105 | 	private final @Nullable PrettyPrinter ssePrettyPrinter;
  106 | 
  107 | 
  108 | 	protected AbstractJackson2HttpMessageConverter(ObjectMapper objectMapper) {
  109 | 		this.defaultObjectMapper = objectMapper;
  110 | 		DefaultPrettyPrinter prettyPrinter = new DefaultPrettyPrinter();
  111 | 		prettyPrinter.indentObjectsWith(new DefaultIndenter("  ", "\ndata: "));
  112 | 		this.ssePrettyPrinter = prettyPrinter;
  113 | 	}
  114 | 
  115 | 	protected AbstractJackson2HttpMessageConverter(ObjectMapper objectMapper, MediaType supportedMediaType) {
  116 | 		this(objectMapper);
  117 | 		setSupportedMediaTypes(Collections.singletonList(supportedMediaType));
  118 | 	}
  119 | 
  120 | 	protected AbstractJackson2HttpMessageConverter(ObjectMapper objectMapper, MediaType... supportedMediaTypes) {
  121 | 		this(objectMapper);
  122 | 		setSupportedMediaTypes(Arrays.asList(supportedMediaTypes));
  123 | 	}
  124 | 
  125 | 
  126 | 	@Override
  127 | 	public void setSupportedMediaTypes(List<MediaType> supportedMediaTypes) {
  128 | 		super.setSupportedMediaTypes(supportedMediaTypes);
  129 | 	}
  130 | 
  131 | 	/**
  132 | 	 * Configure the main {@code ObjectMapper} to use for Object conversion.
  133 | 	 * If not set, a default {@link ObjectMapper} instance is created.
  134 | 	 * <p>Setting a custom-configured {@code ObjectMapper} is one way to take
  135 | 	 * further control of the JSON serialization process. For example, an extended
  136 | 	 * {@link com.fasterxml.jackson.databind.ser.SerializerFactory}
  137 | 	 * can be configured that provides custom serializers for specific types.
  138 | 	 * Another option for refining the serialization process is to use Jackson's
  139 | 	 * provided annotations on the types to be serialized, in which case a
  140 | 	 * custom-configured ObjectMapper is unnecessary.
  141 | 	 * @see #registerObjectMappersForType(Class, Consumer)
  142 | 	 */
  143 | 	public void setObjectMapper(ObjectMapper objectMapper) {
  144 | 		Assert.notNull(objectMapper, "ObjectMapper must not be null");
  145 | 		this.defaultObjectMapper = objectMapper;
  146 | 		configurePrettyPrint();
  147 | 	}
  148 | 
  149 | 	/**
  150 | 	 * Return the main {@code ObjectMapper} in use.
  151 | 	 */
  152 | 	public ObjectMapper getObjectMapper() {
  153 | 		return this.defaultObjectMapper;
  154 | 	}
  155 | 
  156 | 	/**
  157 | 	 * Configure the {@link ObjectMapper} instances to use for the given
  158 | 	 * {@link Class}. This is useful when you want to deviate from the
  159 | 	 * {@link #getObjectMapper() default} ObjectMapper or have the
  160 | 	 * {@code ObjectMapper} vary by {@code MediaType}.
  161 | 	 * <p><strong>Note:</strong> Use of this method effectively turns off use of
  162 | 	 * the default {@link #getObjectMapper() ObjectMapper} and
  163 | 	 * {@link #setSupportedMediaTypes(List) supportedMediaTypes} for the given
  164 | 	 * class. Therefore it is important for the mappings configured here to
  165 | 	 * {@link MediaType#includes(MediaType) include} every MediaType that must
  166 | 	 * be supported for the given class.
  167 | 	 * @param clazz the type of Object to register ObjectMapper instances for
  168 | 	 * @param registrar a consumer to populate or otherwise update the
  169 | 	 * MediaType-to-ObjectMapper associations for the given Class
  170 | 	 * @since 5.3.4
  171 | 	 */
  172 | 	public void registerObjectMappersForType(Class<?> clazz, Consumer<Map<MediaType, ObjectMapper>> registrar) {
  173 | 		if (this.objectMapperRegistrations == null) {
  174 | 			this.objectMapperRegistrations = new LinkedHashMap<>();
  175 | 		}
  176 | 		Map<MediaType, ObjectMapper> registrations =
  177 | 				this.objectMapperRegistrations.computeIfAbsent(clazz, key -> new LinkedHashMap<>());
  178 | 		registrar.accept(registrations);
  179 | 	}
  180 | 
  181 | 	/**
  182 | 	 * Return ObjectMapper registrations for the given class, if any.
  183 | 	 * @param clazz the class to look up for registrations for
  184 | 	 * @return a map with registered MediaType-to-ObjectMapper registrations,
  185 | 	 * or empty if in case of no registrations for the given class.
  186 | 	 * @since 5.3.4
  187 | 	 */
  188 | 	public Map<MediaType, ObjectMapper> getObjectMappersForType(Class<?> clazz) {
  189 | 		for (Map.Entry<Class<?>, Map<MediaType, ObjectMapper>> entry : getObjectMapperRegistrations().entrySet()) {
  190 | 			if (entry.getKey().isAssignableFrom(clazz)) {
  191 | 				return entry.getValue();
  192 | 			}
  193 | 		}
  194 | 		return Collections.emptyMap();
  195 | 	}
  196 | 
  197 | 	@Override
  198 | 	public List<MediaType> getSupportedMediaTypes(Class<?> clazz) {
  199 | 		List<MediaType> result = null;
  200 | 		for (Map.Entry<Class<?>, Map<MediaType, ObjectMapper>> entry : getObjectMapperRegistrations().entrySet()) {
  201 | 			if (entry.getKey().isAssignableFrom(clazz)) {
  202 | 				result = (result != null ? result : new ArrayList<>(entry.getValue().size()));
  203 | 				result.addAll(entry.getValue().keySet());
  204 | 			}
  205 | 		}
  206 | 		if (!CollectionUtils.isEmpty(result)) {
  207 | 			return result;
  208 | 		}
  209 | 		return (ProblemDetail.class.isAssignableFrom(clazz) ?
  210 | 				getMediaTypesForProblemDetail() : getSupportedMediaTypes());
  211 | 	}
  212 | 
  213 | 	private Map<Class<?>, Map<MediaType, ObjectMapper>> getObjectMapperRegistrations() {
  214 | 		return (this.objectMapperRegistrations != null ? this.objectMapperRegistrations : Collections.emptyMap());
  215 | 	}
  216 | 
  217 | 	/**
  218 | 	 * Return the supported media type(s) for {@link ProblemDetail}.
  219 | 	 * By default, an empty list, unless overridden in subclasses.
  220 | 	 * @since 6.0.5
  221 | 	 */
  222 | 	protected List<MediaType> getMediaTypesForProblemDetail() {
  223 | 		return Collections.emptyList();
  224 | 	}
  225 | 
  226 | 	/**
  227 | 	 * Whether to use the {@link DefaultPrettyPrinter} when writing JSON.
  228 | 	 * This is a shortcut for setting up an {@code ObjectMapper} as follows:
  229 | 	 * <pre class="code">
  230 | 	 * ObjectMapper mapper = new ObjectMapper();
  231 | 	 * mapper.configure(SerializationFeature.INDENT_OUTPUT, true);
  232 | 	 * converter.setObjectMapper(mapper);
  233 | 	 * </pre>
  234 | 	 */
  235 | 	public void setPrettyPrint(boolean prettyPrint) {
  236 | 		this.prettyPrint = prettyPrint;
  237 | 		configurePrettyPrint();
  238 | 	}
  239 | 
  240 | 	private void configurePrettyPrint() {
  241 | 		if (this.prettyPrint != null) {
  242 | 			this.defaultObjectMapper.configure(SerializationFeature.INDENT_OUTPUT, this.prettyPrint);
  243 | 		}
  244 | 	}
  245 | 
  246 | 
  247 | 	@Override
  248 | 	public boolean canRead(Class<?> clazz, @Nullable MediaType mediaType) {
  249 | 		return canRead(clazz, null, mediaType);
  250 | 	}
  251 | 
  252 | 	@SuppressWarnings("deprecation")  // as of Jackson 2.18: can(De)Serialize
  253 | 	@Override
  254 | 	public boolean canRead(Type type, @Nullable Class<?> contextClass, @Nullable MediaType mediaType) {
  255 | 		if (!canRead(mediaType)) {
  256 | 			return false;
  257 | 		}
  258 | 		JavaType javaType = getJavaType(type, contextClass);
  259 | 		ObjectMapper objectMapper = selectObjectMapper(javaType.getRawClass(), mediaType);
  260 | 		if (objectMapper == null) {
  261 | 			return false;
  262 | 		}
  263 | 		AtomicReference<Throwable> causeRef = new AtomicReference<>();
  264 | 		if (objectMapper.canDeserialize(javaType, causeRef)) {
  265 | 			return true;
  266 | 		}
  267 | 		logWarningIfNecessary(javaType, causeRef.get());
  268 | 		return false;
  269 | 	}
  270 | 
  271 | 	@SuppressWarnings("deprecation")  // as of Jackson 2.18: can(De)Serialize
  272 | 	@Override
  273 | 	public boolean canWrite(Class<?> clazz, @Nullable MediaType mediaType) {
  274 | 		if (!canWrite(mediaType)) {
  275 | 			return false;
  276 | 		}
  277 | 		if (mediaType != null && mediaType.getCharset() != null) {
  278 | 			Charset charset = mediaType.getCharset();
  279 | 			if (!ENCODINGS.containsKey(charset.name())) {
  280 | 				return false;
  281 | 			}
  282 | 		}
  283 | 		ObjectMapper objectMapper = selectObjectMapper(clazz, mediaType);
  284 | 		if (objectMapper == null) {
  285 | 			return false;
  286 | 		}
  287 | 		AtomicReference<Throwable> causeRef = new AtomicReference<>();
  288 | 		if (objectMapper.canSerialize(clazz, causeRef)) {
  289 | 			return true;
  290 | 		}
  291 | 		logWarningIfNecessary(clazz, causeRef.get());
  292 | 		return false;
  293 | 	}
  294 | 
  295 | 	@Override
  296 | 	public boolean canWriteRepeatedly(Object o, @Nullable MediaType contentType) {
  297 | 		return true;
  298 | 	}
  299 | 
  300 | 	/**
  301 | 	 * Select an ObjectMapper to use, either the main ObjectMapper or another
  302 | 	 * if the handling for the given Class has been customized through
  303 | 	 * {@link #registerObjectMappersForType(Class, Consumer)}.
  304 | 	 */
  305 | 	private @Nullable ObjectMapper selectObjectMapper(Class<?> targetType, @Nullable MediaType targetMediaType) {
  306 | 		if (targetMediaType == null || CollectionUtils.isEmpty(this.objectMapperRegistrations)) {
  307 | 			return this.defaultObjectMapper;
  308 | 		}
  309 | 		for (Map.Entry<Class<?>, Map<MediaType, ObjectMapper>> typeEntry : getObjectMapperRegistrations().entrySet()) {
  310 | 			if (typeEntry.getKey().isAssignableFrom(targetType)) {
  311 | 				for (Map.Entry<MediaType, ObjectMapper> objectMapperEntry : typeEntry.getValue().entrySet()) {
  312 | 					if (objectMapperEntry.getKey().includes(targetMediaType)) {
  313 | 						return objectMapperEntry.getValue();
  314 | 					}
  315 | 				}
  316 | 				// No matching registrations
  317 | 				return null;
  318 | 			}
  319 | 		}
  320 | 		// No registrations
  321 | 		return this.defaultObjectMapper;
  322 | 	}
  323 | 
  324 | 	/**
  325 | 	 * Determine whether to log the given exception coming from a
  326 | 	 * {@link ObjectMapper#canDeserialize} / {@link ObjectMapper#canSerialize} check.
  327 | 	 * @param type the class that Jackson tested for (de-)serializability
  328 | 	 * @param cause the Jackson-thrown exception to evaluate
  329 | 	 * (typically a {@link JsonMappingException})
  330 | 	 * @since 4.3
  331 | 	 */
  332 | 	protected void logWarningIfNecessary(Type type, @Nullable Throwable cause) {
  333 | 		if (cause == null) {
  334 | 			return;
  335 | 		}
  336 | 
  337 | 		// Do not log warning for serializer not found (note: different message wording on Jackson 2.9)
  338 | 		boolean debugLevel = (cause instanceof JsonMappingException && cause.getMessage() != null &&
  339 | 				cause.getMessage().startsWith("Cannot find"));
  340 | 
  341 | 		if (debugLevel ? logger.isDebugEnabled() : logger.isWarnEnabled()) {
  342 | 			String msg = "Failed to evaluate Jackson " + (type instanceof JavaType ? "de" : "") +
  343 | 					"serialization for type [" + type + "]";
  344 | 			if (debugLevel) {
  345 | 				logger.debug(msg, cause);
  346 | 			}
  347 | 			else if (logger.isDebugEnabled()) {
  348 | 				logger.warn(msg, cause);
  349 | 			}
  350 | 			else {
  351 | 				logger.warn(msg + ": " + cause);
  352 | 			}
  353 | 		}
  354 | 	}
  355 | 
  356 | 	@Override
  357 | 	public Object read(Type type, @Nullable Class<?> contextClass, HttpInputMessage inputMessage)
  358 | 			throws IOException, HttpMessageNotReadableException {
  359 | 
  360 | 		JavaType javaType = getJavaType(type, contextClass);
  361 | 		return readJavaType(javaType, inputMessage);
  362 | 	}
  363 | 
  364 | 	@Override
  365 | 	protected Object readInternal(Class<?> clazz, HttpInputMessage inputMessage)
  366 | 			throws IOException, HttpMessageNotReadableException {
  367 | 
  368 | 		JavaType javaType = getJavaType(clazz, null);
  369 | 		return readJavaType(javaType, inputMessage);
  370 | 	}
  371 | 
  372 | 	private Object readJavaType(JavaType javaType, HttpInputMessage inputMessage) throws IOException {
  373 | 		MediaType contentType = inputMessage.getHeaders().getContentType();
  374 | 		Charset charset = getCharset(contentType);
  375 | 
  376 | 		ObjectMapper objectMapper = selectObjectMapper(javaType.getRawClass(), contentType);
  377 | 		Assert.state(objectMapper != null, () -> "No ObjectMapper for " + javaType);
  378 | 
  379 | 		boolean isUnicode = ENCODINGS.containsKey(charset.name()) ||
  380 | 				"UTF-16".equals(charset.name()) ||
  381 | 				"UTF-32".equals(charset.name());
  382 | 		try {
  383 | 			InputStream inputStream = StreamUtils.nonClosing(inputMessage.getBody());
  384 | 			if (inputMessage instanceof MappingJacksonInputMessage mappingJacksonInputMessage) {
  385 | 				Class<?> deserializationView = mappingJacksonInputMessage.getDeserializationView();
  386 | 				if (deserializationView != null) {
  387 | 					ObjectReader objectReader = objectMapper.readerWithView(deserializationView).forType(javaType);
  388 | 					objectReader = customizeReader(objectReader, javaType);
  389 | 					if (isUnicode) {
  390 | 						return objectReader.readValue(inputStream);
  391 | 					}
  392 | 					else {
  393 | 						Reader reader = new InputStreamReader(inputStream, charset);
  394 | 						return objectReader.readValue(reader);
  395 | 					}
  396 | 				}
  397 | 			}
  398 | 
  399 | 			ObjectReader objectReader = objectMapper.reader().forType(javaType);
  400 | 			objectReader = customizeReader(objectReader, javaType);
  401 | 			if (isUnicode) {
  402 | 				return objectReader.readValue(inputStream);
  403 | 			}
  404 | 			else {
  405 | 				Reader reader = new InputStreamReader(inputStream, charset);
  406 | 				return objectReader.readValue(reader);
  407 | 			}
  408 | 		}
  409 | 		catch (InvalidDefinitionException ex) {
  410 | 			throw new HttpMessageConversionException("Type definition error: " + ex.getType(), ex);
  411 | 		}
  412 | 		catch (JsonProcessingException ex) {
  413 | 			throw new HttpMessageNotReadableException("JSON parse error: " + ex.getOriginalMessage(), ex, inputMessage);
  414 | 		}
  415 | 	}
  416 | 
  417 | 	/**
  418 | 	 * Subclasses can use this method to customize {@link ObjectReader} used
  419 | 	 * for reading values.
  420 | 	 * @param reader the reader instance to customize
  421 | 	 * @param javaType the target type of element values to read to
  422 | 	 * @return the customized {@link ObjectReader}
  423 | 	 * @since 6.0
  424 | 	 */
  425 | 	protected ObjectReader customizeReader(ObjectReader reader, JavaType javaType) {
  426 | 		return reader;
  427 | 	}
  428 | 
  429 | 	/**
  430 | 	 * Determine the charset to use for JSON input.
  431 | 	 * <p>By default this is either the charset from the input {@code MediaType}
  432 | 	 * or otherwise falling back on {@code UTF-8}. Can be overridden in subclasses.
  433 | 	 * @param contentType the content type of the HTTP input message
  434 | 	 * @return the charset to use
  435 | 	 * @since 5.1.18
  436 | 	 */
  437 | 	protected Charset getCharset(@Nullable MediaType contentType) {
  438 | 		if (contentType != null && contentType.getCharset() != null) {
  439 | 			return contentType.getCharset();
  440 | 		}
  441 | 		else {
  442 | 			return StandardCharsets.UTF_8;
  443 | 		}
  444 | 	}
  445 | 
  446 | 	@Override
  447 | 	protected void writeInternal(Object object, @Nullable Type type, HttpOutputMessage outputMessage)
  448 | 			throws IOException, HttpMessageNotWritableException {
  449 | 
  450 | 		MediaType contentType = outputMessage.getHeaders().getContentType();
  451 | 		JsonEncoding encoding = getJsonEncoding(contentType);
  452 | 
  453 | 		Class<?> clazz = (object instanceof MappingJacksonValue mappingJacksonValue ?
  454 | 				mappingJacksonValue.getValue().getClass() : object.getClass());
  455 | 		ObjectMapper objectMapper = selectObjectMapper(clazz, contentType);
  456 | 		Assert.state(objectMapper != null, () -> "No ObjectMapper for " + clazz.getName());
  457 | 
  458 | 		OutputStream outputStream = StreamUtils.nonClosing(outputMessage.getBody());
  459 | 		try (JsonGenerator generator = objectMapper.getFactory().createGenerator(outputStream, encoding)) {
  460 | 			writePrefix(generator, object);
  461 | 
  462 | 			Object value = object;
  463 | 			Class<?> serializationView = null;
  464 | 			FilterProvider filters = null;
  465 | 			JavaType javaType = null;
  466 | 
  467 | 			if (object instanceof MappingJacksonValue mappingJacksonValue) {
  468 | 				value = mappingJacksonValue.getValue();
  469 | 				serializationView = mappingJacksonValue.getSerializationView();
  470 | 				filters = mappingJacksonValue.getFilters();
  471 | 			}
  472 | 			if (type != null && TypeUtils.isAssignable(type, value.getClass())) {
  473 | 				javaType = getJavaType(type, null);
  474 | 			}
  475 | 
  476 | 			ObjectWriter objectWriter = (serializationView != null ?
  477 | 					objectMapper.writerWithView(serializationView) : objectMapper.writer());
  478 | 			if (filters != null) {
  479 | 				objectWriter = objectWriter.with(filters);
  480 | 			}
  481 | 			if (javaType != null && (javaType.isContainerType() || javaType.isTypeOrSubTypeOf(Optional.class))) {
  482 | 				objectWriter = objectWriter.forType(javaType);
  483 | 			}
  484 | 			SerializationConfig config = objectWriter.getConfig();
  485 | 			if (contentType != null && contentType.isCompatibleWith(MediaType.TEXT_EVENT_STREAM) &&
  486 | 					config.isEnabled(SerializationFeature.INDENT_OUTPUT)) {
  487 | 				objectWriter = objectWriter.with(this.ssePrettyPrinter);
  488 | 			}
  489 | 			objectWriter = customizeWriter(objectWriter, javaType, contentType);
  490 | 			objectWriter.writeValue(generator, value);
  491 | 
  492 | 			writeSuffix(generator, object);
  493 | 			generator.flush();
  494 | 		}
  495 | 		catch (InvalidDefinitionException ex) {
  496 | 			throw new HttpMessageConversionException("Type definition error: " + ex.getType(), ex);
  497 | 		}
  498 | 		catch (JsonProcessingException ex) {
  499 | 			throw new HttpMessageNotWritableException("Could not write JSON: " + ex.getOriginalMessage(), ex);
  500 | 		}
  501 | 	}
  502 | 
  503 | 	/**
  504 | 	 * Subclasses can use this method to customize {@link ObjectWriter} used
  505 | 	 * for writing values.
  506 | 	 * @param writer the writer instance to customize
  507 | 	 * @param javaType the type of element values to write
  508 | 	 * @param contentType the selected media type
  509 | 	 * @return the customized {@link ObjectWriter}
  510 | 	 * @since 6.0
  511 | 	 */
  512 | 	protected ObjectWriter customizeWriter(
  513 | 			ObjectWriter writer, @Nullable JavaType javaType, @Nullable MediaType contentType) {
  514 | 
  515 | 		return writer;
  516 | 	}
  517 | 
  518 | 	/**
  519 | 	 * Write a prefix before the main content.
  520 | 	 * @param generator the generator to use for writing content.
  521 | 	 * @param object the object to write to the output message.
  522 | 	 */
  523 | 	protected void writePrefix(JsonGenerator generator, Object object) throws IOException {
  524 | 	}
  525 | 
  526 | 	/**
  527 | 	 * Write a suffix after the main content.
  528 | 	 * @param generator the generator to use for writing content.
  529 | 	 * @param object the object to write to the output message.
  530 | 	 */
  531 | 	protected void writeSuffix(JsonGenerator generator, Object object) throws IOException {
  532 | 	}
  533 | 
  534 | 	/**
  535 | 	 * Return the Jackson {@link JavaType} for the specified type and context class.
  536 | 	 * @param type the generic type to return the Jackson JavaType for
  537 | 	 * @param contextClass a context class for the target type, for example a class
  538 | 	 * in which the target type appears in a method signature (can be {@code null})
  539 | 	 * @return the Jackson JavaType
  540 | 	 */
  541 | 	protected JavaType getJavaType(Type type, @Nullable Class<?> contextClass) {
  542 | 		return this.defaultObjectMapper.constructType(GenericTypeResolver.resolveType(type, contextClass));
  543 | 	}
  544 | 
  545 | 	/**
  546 | 	 * Determine the JSON encoding to use for the given content type.
  547 | 	 * @param contentType the media type as requested by the caller
  548 | 	 * @return the JSON encoding to use (never {@code null})
  549 | 	 */
  550 | 	protected JsonEncoding getJsonEncoding(@Nullable MediaType contentType) {
  551 | 		if (contentType != null && contentType.getCharset() != null) {
  552 | 			Charset charset = contentType.getCharset();
  553 | 			JsonEncoding encoding = ENCODINGS.get(charset.name());
  554 | 			if (encoding != null) {
  555 | 				return encoding;
  556 | 			}
  557 | 		}
  558 | 		return JsonEncoding.UTF8;
  559 | 	}
  560 | 
  561 | 	@Override
  562 | 	protected @Nullable MediaType getDefaultContentType(Object object) throws IOException {
  563 | 		if (object instanceof MappingJacksonValue mappingJacksonValue) {
  564 | 			object = mappingJacksonValue.getValue();
  565 | 		}
  566 | 		return super.getDefaultContentType(object);
  567 | 	}
  568 | 
  569 | 	@Override
  570 | 	protected @Nullable Long getContentLength(Object object, @Nullable MediaType contentType) throws IOException {
  571 | 		if (object instanceof MappingJacksonValue mappingJacksonValue) {
  572 | 			object = mappingJacksonValue.getValue();
  573 | 		}
  574 | 		return super.getContentLength(object, contentType);
  575 | 	}
  576 | 
  577 | 	@Override
  578 | 	@SuppressWarnings("removal")
  579 | 	protected boolean supportsRepeatableWrites(Object o) {
  580 | 		return true;
  581 | 	}
  582 | 
  583 | }
```
