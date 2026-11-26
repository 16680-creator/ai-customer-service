<!-- learning-docs source archive（机械抓取原文，禁止手改代码内容） -->
<!-- origin: org.springframework:spring-web:7.1.0 | path: org/springframework/http/converter/DefaultHttpMessageConverters.java -->
<!-- 正文引用：[[源码:DefaultHttpMessageConverters.java#L起-L止]] ；代码块头部：(src: DefaultHttpMessageConverters.java#L起-L止) -->

# DefaultHttpMessageConverters.java

| 项 | 值 |
| --- | --- |
| 来源 | `org.springframework:spring-web:7.1.0` |
| 路径 | `org/springframework/http/converter/DefaultHttpMessageConverters.java` |
| 行数 | 669 |
| 在线地址 | https://repo1.maven.org/maven2/org/springframework/spring-web/7.1.0/spring-web-7.1.0-sources.jar |
| 抓取时间 | 2026-11-25T14:01:26.181Z |

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
   17 | package org.springframework.http.converter;
   18 | 
   19 | import java.util.ArrayList;
   20 | import java.util.Iterator;
   21 | import java.util.List;
   22 | import java.util.function.Consumer;
   23 | 
   24 | import org.jspecify.annotations.Nullable;
   25 | 
   26 | import org.springframework.http.MediaType;
   27 | import org.springframework.http.converter.cbor.JacksonCborHttpMessageConverter;
   28 | import org.springframework.http.converter.cbor.KotlinSerializationCborHttpMessageConverter;
   29 | import org.springframework.http.converter.cbor.MappingJackson2CborHttpMessageConverter;
   30 | import org.springframework.http.converter.feed.AtomFeedHttpMessageConverter;
   31 | import org.springframework.http.converter.feed.RssChannelHttpMessageConverter;
   32 | import org.springframework.http.converter.json.GsonHttpMessageConverter;
   33 | import org.springframework.http.converter.json.JacksonJsonHttpMessageConverter;
   34 | import org.springframework.http.converter.json.JsonbHttpMessageConverter;
   35 | import org.springframework.http.converter.json.KotlinSerializationJsonHttpMessageConverter;
   36 | import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
   37 | import org.springframework.http.converter.multipart.MultipartHttpMessageConverter;
   38 | import org.springframework.http.converter.protobuf.KotlinSerializationProtobufHttpMessageConverter;
   39 | import org.springframework.http.converter.smile.JacksonSmileHttpMessageConverter;
   40 | import org.springframework.http.converter.smile.MappingJackson2SmileHttpMessageConverter;
   41 | import org.springframework.http.converter.xml.JacksonXmlHttpMessageConverter;
   42 | import org.springframework.http.converter.xml.Jaxb2RootElementHttpMessageConverter;
   43 | import org.springframework.http.converter.xml.MappingJackson2XmlHttpMessageConverter;
   44 | import org.springframework.http.converter.yaml.JacksonYamlHttpMessageConverter;
   45 | import org.springframework.http.converter.yaml.MappingJackson2YamlHttpMessageConverter;
   46 | import org.springframework.util.Assert;
   47 | import org.springframework.util.ClassUtils;
   48 | 
   49 | /**
   50 |  * Default implementation for {@link HttpMessageConverters}.
   51 |  *
   52 |  * @author Brian Clozel
   53 |  * @author Sebastien Deleuze
   54 |  */
   55 | @SuppressWarnings("removal")
   56 | class DefaultHttpMessageConverters implements HttpMessageConverters {
   57 | 
   58 | 	private final List<HttpMessageConverter<?>> messageConverters;
   59 | 
   60 | 
   61 | 	DefaultHttpMessageConverters(List<HttpMessageConverter<?>> messageConverters) {
   62 | 		this.messageConverters = messageConverters;
   63 | 	}
   64 | 
   65 | 
   66 | 	@Override
   67 | 	public boolean isEmpty() {
   68 | 		return this.messageConverters.isEmpty();
   69 | 	}
   70 | 
   71 | 	@Override
   72 | 	public Iterator<HttpMessageConverter<?>> iterator() {
   73 | 		return this.messageConverters.iterator();
   74 | 	}
   75 | 
   76 | 
   77 | 	abstract static class DefaultBuilder {
   78 | 
   79 | 		static final boolean JACKSON_PRESENT;
   80 | 
   81 | 		static final boolean JACKSON_2_PRESENT;
   82 | 
   83 | 		static final boolean GSON_PRESENT;
   84 | 
   85 | 		static final boolean JSONB_PRESENT;
   86 | 
   87 | 		static final boolean KOTLIN_SERIALIZATION_JSON_PRESENT;
   88 | 
   89 | 		static final boolean JACKSON_XML_PRESENT;
   90 | 
   91 | 		static final boolean JACKSON_2_XML_PRESENT;
   92 | 
   93 | 		static final boolean JAXB_2_PRESENT;
   94 | 
   95 | 		static final boolean JACKSON_SMILE_PRESENT;
   96 | 
   97 | 		static final boolean JACKSON_2_SMILE_PRESENT;
   98 | 
   99 | 		static final boolean JACKSON_CBOR_PRESENT;
  100 | 
  101 | 		static final boolean JACKSON_2_CBOR_PRESENT;
  102 | 
  103 | 		static final boolean KOTLIN_SERIALIZATION_CBOR_PRESENT;
  104 | 
  105 | 		static final boolean JACKSON_YAML_PRESENT;
  106 | 
  107 | 		static final boolean JACKSON_2_YAML_PRESENT;
  108 | 
  109 | 		static final boolean KOTLIN_SERIALIZATION_PROTOBUF_PRESENT;
  110 | 
  111 | 		static final boolean ROME_PRESENT;
  112 | 
  113 | 		boolean registerDefaults;
  114 | 
  115 | 		@Nullable ByteArrayHttpMessageConverter byteArrayConverter;
  116 | 
  117 | 		@Nullable HttpMessageConverter<?> stringConverter;
  118 | 
  119 | 		@Nullable HttpMessageConverter<?> resourceConverter;
  120 | 
  121 | 		@Nullable HttpMessageConverter<?> resourceRegionConverter;
  122 | 
  123 | 		@Nullable HttpMessageConverter<?> formConverter;
  124 | 
  125 | 		@Nullable Consumer<HttpMessageConverter<?>> configurer;
  126 | 
  127 | 		@Nullable Consumer<List<HttpMessageConverter<?>>> convertersListConfigurer;
  128 | 
  129 | 		@Nullable HttpMessageConverter<?> kotlinJsonConverter;
  130 | 
  131 | 		@Nullable HttpMessageConverter<?> jsonConverter;
  132 | 
  133 | 		@Nullable HttpMessageConverter<?> xmlConverter;
  134 | 
  135 | 		@Nullable HttpMessageConverter<?> smileConverter;
  136 | 
  137 | 		@Nullable HttpMessageConverter<?> kotlinCborConverter;
  138 | 
  139 | 		@Nullable HttpMessageConverter<?> cborConverter;
  140 | 
  141 | 		@Nullable HttpMessageConverter<?> yamlConverter;
  142 | 
  143 | 		@Nullable HttpMessageConverter<?> protobufConverter;
  144 | 
  145 | 		@Nullable HttpMessageConverter<?> atomConverter;
  146 | 
  147 | 		@Nullable HttpMessageConverter<?> rssConverter;
  148 | 
  149 | 		final List<HttpMessageConverter<?>> customConverters = new ArrayList<>();
  150 | 
  151 | 
  152 | 		static {
  153 | 			ClassLoader classLoader = DefaultBuilder.class.getClassLoader();
  154 | 			JACKSON_PRESENT = ClassUtils.isPresent("tools.jackson.databind.ObjectMapper", classLoader);
  155 | 			JACKSON_2_PRESENT = ClassUtils.isPresent("com.fasterxml.jackson.databind.ObjectMapper", classLoader) &&
  156 | 						ClassUtils.isPresent("com.fasterxml.jackson.core.JsonGenerator", classLoader);
  157 | 			GSON_PRESENT = ClassUtils.isPresent("com.google.gson.Gson", classLoader);
  158 | 			JSONB_PRESENT = ClassUtils.isPresent("jakarta.json.bind.Jsonb", classLoader);
  159 | 			KOTLIN_SERIALIZATION_JSON_PRESENT = ClassUtils.isPresent("kotlinx.serialization.json.Json", classLoader);
  160 | 			JACKSON_SMILE_PRESENT = JACKSON_PRESENT && ClassUtils.isPresent("tools.jackson.dataformat.smile.SmileMapper", classLoader);
  161 | 			JACKSON_2_SMILE_PRESENT = ClassUtils.isPresent("com.fasterxml.jackson.dataformat.smile.SmileFactory", classLoader);
  162 | 			JAXB_2_PRESENT = ClassUtils.isPresent("jakarta.xml.bind.Binder", classLoader);
  163 | 			JACKSON_XML_PRESENT = JACKSON_PRESENT && ClassUtils.isPresent("tools.jackson.dataformat.xml.XmlMapper", classLoader);
  164 | 			JACKSON_2_XML_PRESENT = ClassUtils.isPresent("com.fasterxml.jackson.dataformat.xml.XmlMapper", classLoader);
  165 | 			JACKSON_CBOR_PRESENT = JACKSON_PRESENT && ClassUtils.isPresent("tools.jackson.dataformat.cbor.CBORMapper", classLoader);
  166 | 			JACKSON_2_CBOR_PRESENT = ClassUtils.isPresent("com.fasterxml.jackson.dataformat.cbor.CBORFactory", classLoader);
  167 | 			JACKSON_YAML_PRESENT = JACKSON_PRESENT && ClassUtils.isPresent("tools.jackson.dataformat.yaml.YAMLMapper", classLoader);
  168 | 			JACKSON_2_YAML_PRESENT = ClassUtils.isPresent("com.fasterxml.jackson.dataformat.yaml.YAMLFactory", classLoader);
  169 | 			KOTLIN_SERIALIZATION_CBOR_PRESENT = ClassUtils.isPresent("kotlinx.serialization.cbor.Cbor", classLoader);
  170 | 			KOTLIN_SERIALIZATION_PROTOBUF_PRESENT = ClassUtils.isPresent("kotlinx.serialization.protobuf.ProtoBuf", classLoader);
  171 | 			ROME_PRESENT = ClassUtils.isPresent("com.rometools.rome.feed.WireFeed", classLoader);
  172 | 		}
  173 | 
  174 | 
  175 | 		void setStringConverter(HttpMessageConverter<?> stringConverter) {
  176 | 			checkConverterSupports(stringConverter, MediaType.TEXT_PLAIN);
  177 | 			this.stringConverter = stringConverter;
  178 | 		}
  179 | 
  180 | 		public void setFormConverter(HttpMessageConverter<?> formConverter) {
  181 | 			checkConverterSupports(formConverter, MediaType.APPLICATION_FORM_URLENCODED);
  182 | 			this.formConverter = formConverter;
  183 | 		}
  184 | 
  185 | 		void setKotlinSerializationJsonConverter(HttpMessageConverter<?> kotlinJsonConverter) {
  186 | 			Assert.notNull(kotlinJsonConverter, "kotlinJsonConverter must not be null");
  187 | 			this.kotlinJsonConverter = kotlinJsonConverter;
  188 | 		}
  189 | 
  190 | 		void setJsonConverter(HttpMessageConverter<?> jsonConverter) {
  191 | 			checkConverterSupports(jsonConverter, MediaType.APPLICATION_JSON);
  192 | 			this.jsonConverter = jsonConverter;
  193 | 		}
  194 | 
  195 | 		void setXmlConverter(HttpMessageConverter<?> xmlConverter) {
  196 | 			checkConverterSupports(xmlConverter, MediaType.TEXT_XML);
  197 | 			this.xmlConverter = xmlConverter;
  198 | 		}
  199 | 
  200 | 		void setSmileConverter(HttpMessageConverter<?> smileConverter) {
  201 | 			checkConverterSupports(smileConverter, new MediaType("application", "x-jackson-smile"));
  202 | 			this.smileConverter = smileConverter;
  203 | 		}
  204 | 
  205 | 		void setKotlinSerializationCborConverter(HttpMessageConverter<?> kotlinCborConverter) {
  206 | 			Assert.notNull(kotlinCborConverter, "kotlinCborConverter must not be null");
  207 | 			this.kotlinCborConverter = kotlinCborConverter;
  208 | 		}
  209 | 
  210 | 		void setCborConverter(HttpMessageConverter<?> cborConverter) {
  211 | 			checkConverterSupports(cborConverter, MediaType.APPLICATION_CBOR);
  212 | 			this.cborConverter = cborConverter;
  213 | 		}
  214 | 
  215 | 		void setYamlConverter(HttpMessageConverter<?> yamlConverter) {
  216 | 			checkConverterSupports(yamlConverter, MediaType.APPLICATION_YAML);
  217 | 			this.yamlConverter = yamlConverter;
  218 | 		}
  219 | 
  220 | 		private void checkConverterSupports(HttpMessageConverter<?> converter, MediaType mediaType) {
  221 | 			for (MediaType supportedMediaType : converter.getSupportedMediaTypes()) {
  222 | 				if (mediaType.equalsTypeAndSubtype(supportedMediaType)) {
  223 | 					return;
  224 | 				}
  225 | 			}
  226 | 			throw new IllegalArgumentException("converter should support '" + mediaType + "'");
  227 | 		}
  228 | 
  229 | 		void addCustomMessageConverter(HttpMessageConverter<?> customConverter) {
  230 | 			Assert.notNull(customConverter, "'customConverter' must not be null");
  231 | 			this.customConverters.add(customConverter);
  232 | 		}
  233 | 
  234 | 		void addMessageConverterConfigurer(Consumer<HttpMessageConverter<?>> configurer) {
  235 | 			this.configurer = (this.configurer != null) ? this.configurer.andThen(configurer) : configurer;
  236 | 		}
  237 | 
  238 | 		void addMessageConvertersListConfigurer(Consumer<List<HttpMessageConverter<?>>> configurer) {
  239 | 			this.convertersListConfigurer = (this.convertersListConfigurer != null) ?
  240 | 					this.convertersListConfigurer.andThen(configurer) : configurer;
  241 | 		}
  242 | 
  243 | 		List<HttpMessageConverter<?>> getBaseConverters() {
  244 | 			List<HttpMessageConverter<?>> converters = new ArrayList<>();
  245 | 			if (this.byteArrayConverter != null) {
  246 | 				converters.add(this.byteArrayConverter);
  247 | 			}
  248 | 			if (this.stringConverter != null) {
  249 | 				converters.add(this.stringConverter);
  250 | 			}
  251 | 			if (this.formConverter != null) {
  252 | 				converters.add(this.formConverter);
  253 | 			}
  254 | 			return converters;
  255 | 		}
  256 | 
  257 | 		List<HttpMessageConverter<?>> getCoreConverters() {
  258 | 			List<HttpMessageConverter<?>> converters = new ArrayList<>();
  259 | 			if (this.kotlinJsonConverter != null) {
  260 | 				converters.add(this.kotlinJsonConverter);
  261 | 			}
  262 | 			if (this.jsonConverter != null) {
  263 | 				converters.add(this.jsonConverter);
  264 | 			}
  265 | 			if (this.smileConverter != null) {
  266 | 				converters.add(this.smileConverter);
  267 | 			}
  268 | 			if (this.kotlinCborConverter != null) {
  269 | 				converters.add(this.kotlinCborConverter);
  270 | 			}
  271 | 			if (this.cborConverter != null) {
  272 | 				converters.add(this.cborConverter);
  273 | 			}
  274 | 			if (this.yamlConverter != null) {
  275 | 				converters.add(this.yamlConverter);
  276 | 			}
  277 | 			if (this.xmlConverter != null) {
  278 | 				converters.add(this.xmlConverter);
  279 | 			}
  280 | 			if (this.protobufConverter != null) {
  281 | 				converters.add(this.protobufConverter);
  282 | 			}
  283 | 			if (this.atomConverter != null) {
  284 | 				converters.add(this.atomConverter);
  285 | 			}
  286 | 			if (this.rssConverter != null) {
  287 | 				converters.add(this.rssConverter);
  288 | 			}
  289 | 			return converters;
  290 | 		}
  291 | 
  292 | 		List<HttpMessageConverter<?>> getCustomConverters() {
  293 | 			return this.customConverters;
  294 | 		}
  295 | 
  296 | 		void detectMessageConverters() {
  297 | 			this.byteArrayConverter = new ByteArrayHttpMessageConverter();
  298 | 
  299 | 			if (this.stringConverter == null) {
  300 | 				this.stringConverter = new StringHttpMessageConverter();
  301 | 			}
  302 | 			if (this.formConverter == null) {
  303 | 				this.formConverter = new FormHttpMessageConverter();
  304 | 			}
  305 | 			if (this.kotlinJsonConverter == null) {
  306 | 				if (KOTLIN_SERIALIZATION_JSON_PRESENT) {
  307 | 					if (this.jsonConverter != null || JACKSON_PRESENT || JACKSON_2_PRESENT || GSON_PRESENT || JSONB_PRESENT) {
  308 | 						this.kotlinJsonConverter = new KotlinSerializationJsonHttpMessageConverter();
  309 | 					}
  310 | 					else {
  311 | 						this.kotlinJsonConverter = new KotlinSerializationJsonHttpMessageConverter(type -> true);
  312 | 					}
  313 | 				}
  314 | 			}
  315 | 			if (this.jsonConverter == null) {
  316 | 				if (JACKSON_PRESENT) {
  317 | 					this.jsonConverter = new JacksonJsonHttpMessageConverter();
  318 | 				}
  319 | 				else if (JACKSON_2_PRESENT) {
  320 | 					this.jsonConverter = new MappingJackson2HttpMessageConverter();
  321 | 				}
  322 | 				else if (GSON_PRESENT) {
  323 | 					this.jsonConverter = new GsonHttpMessageConverter();
  324 | 				}
  325 | 				else if (JSONB_PRESENT) {
  326 | 					this.jsonConverter = new JsonbHttpMessageConverter();
  327 | 				}
  328 | 			}
  329 | 
  330 | 			if (this.xmlConverter == null) {
  331 | 				if (JACKSON_XML_PRESENT) {
  332 | 					this.xmlConverter = new JacksonXmlHttpMessageConverter();
  333 | 				}
  334 | 				else if (JACKSON_2_XML_PRESENT) {
  335 | 					this.xmlConverter = new MappingJackson2XmlHttpMessageConverter();
  336 | 				}
  337 | 				else if (JAXB_2_PRESENT) {
  338 | 					this.xmlConverter = new Jaxb2RootElementHttpMessageConverter();
  339 | 				}
  340 | 			}
  341 | 
  342 | 			if (this.smileConverter == null) {
  343 | 				if (JACKSON_SMILE_PRESENT) {
  344 | 					this.smileConverter = new JacksonSmileHttpMessageConverter();
  345 | 				}
  346 | 				else if (JACKSON_2_SMILE_PRESENT) {
  347 | 					this.smileConverter = new MappingJackson2SmileHttpMessageConverter();
  348 | 				}
  349 | 			}
  350 | 
  351 | 			if (this.kotlinCborConverter == null) {
  352 | 				if (KOTLIN_SERIALIZATION_CBOR_PRESENT) {
  353 | 					if (this.cborConverter != null || JACKSON_CBOR_PRESENT || JACKSON_2_CBOR_PRESENT) {
  354 | 						this.kotlinCborConverter = new KotlinSerializationCborHttpMessageConverter();
  355 | 					}
  356 | 					else {
  357 | 						this.kotlinCborConverter = new KotlinSerializationCborHttpMessageConverter(type -> true);
  358 | 					}
  359 | 				}
  360 | 			}
  361 | 			if (this.cborConverter == null) {
  362 | 				if (JACKSON_CBOR_PRESENT) {
  363 | 					this.cborConverter = new JacksonCborHttpMessageConverter();
  364 | 				}
  365 | 				else if (JACKSON_2_CBOR_PRESENT) {
  366 | 					this.cborConverter = new MappingJackson2CborHttpMessageConverter();
  367 | 				}
  368 | 			}
  369 | 
  370 | 			if (this.yamlConverter == null) {
  371 | 				if (JACKSON_YAML_PRESENT) {
  372 | 					this.yamlConverter = new JacksonYamlHttpMessageConverter();
  373 | 				}
  374 | 				else if (JACKSON_2_YAML_PRESENT) {
  375 | 					this.yamlConverter = new MappingJackson2YamlHttpMessageConverter();
  376 | 				}
  377 | 			}
  378 | 
  379 | 			if (this.protobufConverter == null) {
  380 | 				if (KOTLIN_SERIALIZATION_PROTOBUF_PRESENT) {
  381 | 					this.protobufConverter = new KotlinSerializationProtobufHttpMessageConverter(type -> true);
  382 | 				}
  383 | 			}
  384 | 
  385 | 			if (ROME_PRESENT) {
  386 | 				if (this.atomConverter == null) {
  387 | 					this.atomConverter = new AtomFeedHttpMessageConverter();
  388 | 				}
  389 | 				if (this.rssConverter == null) {
  390 | 					this.rssConverter = new RssChannelHttpMessageConverter();
  391 | 				}
  392 | 			}
  393 | 		}
  394 | 
  395 | 	}
  396 | 
  397 | 	static class DefaultClientBuilder extends DefaultBuilder implements ClientBuilder {
  398 | 
  399 | 		@Override
  400 | 		public DefaultClientBuilder registerDefaults() {
  401 | 			this.registerDefaults = true;
  402 | 			return this;
  403 | 		}
  404 | 
  405 | 		@Override
  406 | 		public ClientBuilder disableDefaults() {
  407 | 			this.registerDefaults = false;
  408 | 			return this;
  409 | 		}
  410 | 
  411 | 		@Override
  412 | 		public ClientBuilder withStringConverter(HttpMessageConverter<?> stringConverter) {
  413 | 			setStringConverter(stringConverter);
  414 | 			return this;
  415 | 		}
  416 | 
  417 | 		@Override
  418 | 		public ClientBuilder withFormConverter(HttpMessageConverter<?> formMessageConverter) {
  419 | 			setFormConverter(formMessageConverter);
  420 | 			return this;
  421 | 		}
  422 | 
  423 | 		@Override
  424 | 		public ClientBuilder withKotlinSerializationJsonConverter(HttpMessageConverter<?> kotlinSerializationJsonConverter) {
  425 | 			setKotlinSerializationJsonConverter(kotlinSerializationJsonConverter);
  426 | 			return this;
  427 | 		}
  428 | 
  429 | 		@Override
  430 | 		public ClientBuilder withJsonConverter(HttpMessageConverter<?> jsonConverter) {
  431 | 			setJsonConverter(jsonConverter);
  432 | 			return this;
  433 | 		}
  434 | 
  435 | 		@Override
  436 | 		public ClientBuilder withXmlConverter(HttpMessageConverter<?> xmlConverter) {
  437 | 			setXmlConverter(xmlConverter);
  438 | 			return this;
  439 | 		}
  440 | 
  441 | 		@Override
  442 | 		public ClientBuilder withSmileConverter(HttpMessageConverter<?> smileConverter) {
  443 | 			setSmileConverter(smileConverter);
  444 | 			return this;
  445 | 		}
  446 | 
  447 | 		@Override
  448 | 		public ClientBuilder withKotlinSerializationCborConverter(HttpMessageConverter<?> kotlinSerializationCborConverter) {
  449 | 			setKotlinSerializationCborConverter(kotlinSerializationCborConverter);
  450 | 			return this;
  451 | 		}
  452 | 
  453 | 		@Override
  454 | 		public ClientBuilder withCborConverter(HttpMessageConverter<?> cborConverter) {
  455 | 			setCborConverter(cborConverter);
  456 | 			return this;
  457 | 		}
  458 | 
  459 | 		@Override
  460 | 		public ClientBuilder withYamlConverter(HttpMessageConverter<?> yamlConverter) {
  461 | 			setYamlConverter(yamlConverter);
  462 | 			return this;
  463 | 		}
  464 | 
  465 | 		@Override
  466 | 		public ClientBuilder addCustomConverter(HttpMessageConverter<?> customConverter) {
  467 | 			addCustomMessageConverter(customConverter);
  468 | 			return this;
  469 | 		}
  470 | 
  471 | 		@Override
  472 | 		public ClientBuilder configureMessageConverters(Consumer<HttpMessageConverter<?>> configurer) {
  473 | 			addMessageConverterConfigurer(configurer);
  474 | 			return this;
  475 | 		}
  476 | 
  477 | 		@Override
  478 | 		public ClientBuilder configureMessageConvertersList(Consumer<List<HttpMessageConverter<?>>> configurer) {
  479 | 			addMessageConvertersListConfigurer(configurer);
  480 | 			return this;
  481 | 		}
  482 | 
  483 | 		@Override
  484 | 		void detectMessageConverters() {
  485 | 			super.detectMessageConverters();
  486 | 			// client-specific detection behavior
  487 | 			if (this.xmlConverter == null) {
  488 | 				if (JACKSON_XML_PRESENT) {
  489 | 					this.xmlConverter = new JacksonXmlHttpMessageConverter();
  490 | 				}
  491 | 				else if (JACKSON_2_XML_PRESENT) {
  492 | 					this.xmlConverter = new MappingJackson2XmlHttpMessageConverter();
  493 | 				}
  494 | 				else if (JAXB_2_PRESENT) {
  495 | 					this.xmlConverter = new Jaxb2RootElementHttpMessageConverter();
  496 | 				}
  497 | 			}
  498 | 		}
  499 | 
  500 | 		@Override
  501 | 		public HttpMessageConverters build() {
  502 | 			if (this.registerDefaults) {
  503 | 				this.resourceConverter = new ResourceHttpMessageConverter(false);
  504 | 				detectMessageConverters();
  505 | 			}
  506 | 			List<HttpMessageConverter<?>> partConverters = new ArrayList<>(this.getCustomConverters());
  507 | 			List<HttpMessageConverter<?>> allConverters = new ArrayList<>(this.getCustomConverters());
  508 | 			allConverters.addAll(this.getBaseConverters());
  509 | 			if (this.resourceConverter != null) {
  510 | 				allConverters.add(this.resourceConverter);
  511 | 			}
  512 | 			if (!this.getCoreConverters().isEmpty()) {
  513 | 				// use separate instances of base converters for multipart
  514 | 				partConverters.addAll(List.of(new ByteArrayHttpMessageConverter(),
  515 | 						new StringHttpMessageConverter(), new ResourceHttpMessageConverter()));
  516 | 				partConverters.addAll(this.getCoreConverters());
  517 | 			}
  518 | 			if (!partConverters.isEmpty() || !allConverters.isEmpty()) {
  519 | 				allConverters.add(new MultipartHttpMessageConverter(partConverters));
  520 | 			}
  521 | 			allConverters.addAll(this.getCoreConverters());
  522 | 			if (this.convertersListConfigurer != null) {
  523 | 				this.convertersListConfigurer.accept(allConverters);
  524 | 			}
  525 | 			if (this.configurer != null) {
  526 | 				allConverters.forEach(this.configurer);
  527 | 			}
  528 | 			return new DefaultHttpMessageConverters(allConverters);
  529 | 		}
  530 | 	}
  531 | 
  532 | 	static class DefaultServerBuilder extends DefaultBuilder implements ServerBuilder {
  533 | 
  534 | 
  535 | 		@Override
  536 | 		public ServerBuilder registerDefaults() {
  537 | 			this.registerDefaults = true;
  538 | 			return this;
  539 | 		}
  540 | 
  541 | 		@Override
  542 | 		public ServerBuilder disableDefaults() {
  543 | 			this.registerDefaults = false;
  544 | 			return this;
  545 | 		}
  546 | 
  547 | 		@Override
  548 | 		public ServerBuilder withStringConverter(HttpMessageConverter<?> stringConverter) {
  549 | 			setStringConverter(stringConverter);
  550 | 			return this;
  551 | 		}
  552 | 
  553 | 		@Override
  554 | 		public ServerBuilder withFormConverter(HttpMessageConverter<?> formMessageConverter) {
  555 | 			setFormConverter(formMessageConverter);
  556 | 			return this;
  557 | 		}
  558 | 
  559 | 		@Override
  560 | 		public ServerBuilder withKotlinSerializationJsonConverter(HttpMessageConverter<?> kotlinSerializationJsonConverter) {
  561 | 			setKotlinSerializationJsonConverter(kotlinSerializationJsonConverter);
  562 | 			return this;
  563 | 		}
  564 | 
  565 | 		@Override
  566 | 		public ServerBuilder withJsonConverter(HttpMessageConverter<?> jsonConverter) {
  567 | 			setJsonConverter(jsonConverter);
  568 | 			return this;
  569 | 		}
  570 | 
  571 | 		@Override
  572 | 		public ServerBuilder withXmlConverter(HttpMessageConverter<?> xmlConverter) {
  573 | 			setXmlConverter(xmlConverter);
  574 | 			return this;
  575 | 		}
  576 | 
  577 | 		@Override
  578 | 		public ServerBuilder withSmileConverter(HttpMessageConverter<?> smileConverter) {
  579 | 			setSmileConverter(smileConverter);
  580 | 			return this;
  581 | 		}
  582 | 
  583 | 		@Override
  584 | 		public ServerBuilder withKotlinSerializationCborConverter(HttpMessageConverter<?> kotlinSerializationCborConverter) {
  585 | 			setKotlinSerializationCborConverter(kotlinSerializationCborConverter);
  586 | 			return this;
  587 | 		}
  588 | 
  589 | 		@Override
  590 | 		public ServerBuilder withCborConverter(HttpMessageConverter<?> cborConverter) {
  591 | 			setCborConverter(cborConverter);
  592 | 			return this;
  593 | 		}
  594 | 
  595 | 		@Override
  596 | 		public ServerBuilder withYamlConverter(HttpMessageConverter<?> yamlConverter) {
  597 | 			setYamlConverter(yamlConverter);
  598 | 			return this;
  599 | 		}
  600 | 
  601 | 		@Override
  602 | 		public ServerBuilder addCustomConverter(HttpMessageConverter<?> customConverter) {
  603 | 			addCustomMessageConverter(customConverter);
  604 | 			return this;
  605 | 		}
  606 | 
  607 | 		@Override
  608 | 		public ServerBuilder configureMessageConverters(Consumer<HttpMessageConverter<?>> configurer) {
  609 | 			addMessageConverterConfigurer(configurer);
  610 | 			return this;
  611 | 		}
  612 | 
  613 | 		@Override
  614 | 		public ServerBuilder configureMessageConvertersList(Consumer<List<HttpMessageConverter<?>>> configurer) {
  615 | 			addMessageConvertersListConfigurer(configurer);
  616 | 			return this;
  617 | 		}
  618 | 
  619 | 		@Override
  620 | 		void detectMessageConverters() {
  621 | 			super.detectMessageConverters();
  622 | 			// server-specific detection behavior
  623 | 			if (this.xmlConverter == null) {
  624 | 				if (JACKSON_XML_PRESENT) {
  625 | 					this.xmlConverter = new JacksonXmlHttpMessageConverter();
  626 | 				}
  627 | 				else if (JACKSON_2_XML_PRESENT) {
  628 | 					this.xmlConverter = new MappingJackson2XmlHttpMessageConverter();
  629 | 				}
  630 | 			}
  631 | 		}
  632 | 
  633 | 		@Override
  634 | 		public HttpMessageConverters build() {
  635 | 			if (this.registerDefaults) {
  636 | 				this.resourceConverter = new ResourceHttpMessageConverter();
  637 | 				this.resourceRegionConverter = new ResourceRegionHttpMessageConverter();
  638 | 				detectMessageConverters();
  639 | 			}
  640 | 			List<HttpMessageConverter<?>> partConverters = new ArrayList<>(this.getCustomConverters());
  641 | 			List<HttpMessageConverter<?>> allConverters = new ArrayList<>(this.getCustomConverters());
  642 | 			allConverters.addAll(this.getBaseConverters());
  643 | 			if (this.resourceConverter != null) {
  644 | 				allConverters.add(this.resourceConverter);
  645 | 			}
  646 | 			if (this.resourceRegionConverter != null) {
  647 | 				allConverters.add(this.resourceRegionConverter);
  648 | 			}
  649 | 			if (!this.getCoreConverters().isEmpty()) {
  650 | 				// use separate instances of base converters for multipart
  651 | 				partConverters.addAll(List.of(new ByteArrayHttpMessageConverter(),
  652 | 						new StringHttpMessageConverter(), new ResourceHttpMessageConverter()));
  653 | 				partConverters.addAll(this.getCoreConverters());
  654 | 			}
  655 | 			if (!partConverters.isEmpty() || !allConverters.isEmpty()) {
  656 | 				allConverters.add(new MultipartHttpMessageConverter(partConverters));
  657 | 			}
  658 | 			allConverters.addAll(this.getCoreConverters());
  659 | 			if (this.convertersListConfigurer != null) {
  660 | 				this.convertersListConfigurer.accept(allConverters);
  661 | 			}
  662 | 			if (this.configurer != null) {
  663 | 				allConverters.forEach(this.configurer);
  664 | 			}
  665 | 			return new DefaultHttpMessageConverters(allConverters);
  666 | 		}
  667 | 	}
  668 | 
  669 | }
```
