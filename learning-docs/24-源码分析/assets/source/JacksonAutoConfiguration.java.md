<!-- learning-docs source archive（机械抓取原文，禁止手改代码内容） -->
<!-- origin: org.springframework.boot:spring-boot-jackson:4.2.0 | path: org/springframework/boot/jackson/autoconfigure/JacksonAutoConfiguration.java -->
<!-- 正文引用：[[源码:JacksonAutoConfiguration.java#L起-L止]] ；代码块头部：(src: JacksonAutoConfiguration.java#L起-L止) -->

# JacksonAutoConfiguration.java

| 项 | 值 |
| --- | --- |
| 来源 | `org.springframework.boot:spring-boot-jackson:4.2.0` |
| 路径 | `org/springframework/boot/jackson/autoconfigure/JacksonAutoConfiguration.java` |
| 行数 | 651 |
| 在线地址 | https://repo1.maven.org/maven2/org/springframework/boot/spring-boot-jackson/4.2.0/spring-boot-jackson-4.2.0-sources.jar |
| 抓取时间 | 2026-11-25T14:02:16.597Z |

```java
    1 | /*
    2 |  * Copyright 2012-present the original author or authors.
    3 |  */
    4 | 
    5 | package org.springframework.boot.jackson.autoconfigure;
    6 | 
    7 | import java.lang.reflect.Field;
    8 | import java.text.DateFormat;
    9 | import java.text.SimpleDateFormat;
   10 | import java.util.Collection;
   11 | import java.util.Collections;
   12 | import java.util.List;
   13 | import java.util.Locale;
   14 | import java.util.Map;
   15 | import java.util.TimeZone;
   16 | import java.util.function.BiConsumer;
   17 | import java.util.stream.Stream;
   18 | 
   19 | import com.fasterxml.jackson.annotation.JsonAutoDetect;
   20 | import com.fasterxml.jackson.annotation.JsonInclude.Include;
   21 | import com.fasterxml.jackson.annotation.PropertyAccessor;
   22 | import org.jspecify.annotations.Nullable;
   23 | import tools.jackson.core.StreamReadConstraints;
   24 | import tools.jackson.core.StreamWriteConstraints;
   25 | import tools.jackson.core.base.DecorableTSFactory.DecorableTSFBuilder;
   26 | import tools.jackson.core.json.JsonFactory;
   27 | import tools.jackson.core.json.JsonFactoryBuilder;
   28 | import tools.jackson.databind.DeserializationFeature;
   29 | import tools.jackson.databind.JacksonModule;
   30 | import tools.jackson.databind.MapperFeature;
   31 | import tools.jackson.databind.ObjectMapper;
   32 | import tools.jackson.databind.PropertyNamingStrategies;
   33 | import tools.jackson.databind.PropertyNamingStrategy;
   34 | import tools.jackson.databind.cfg.ConstructorDetector;
   35 | import tools.jackson.databind.cfg.DateTimeFeature;
   36 | import tools.jackson.databind.cfg.HandlerInstantiator;
   37 | import tools.jackson.databind.cfg.MapperBuilder;
   38 | import tools.jackson.databind.json.JsonMapper;
   39 | import tools.jackson.dataformat.cbor.CBORFactory;
   40 | import tools.jackson.dataformat.cbor.CBORFactoryBuilder;
   41 | import tools.jackson.dataformat.cbor.CBORMapper;
   42 | import tools.jackson.dataformat.xml.XmlFactory;
   43 | import tools.jackson.dataformat.xml.XmlFactoryBuilder;
   44 | import tools.jackson.dataformat.xml.XmlMapper;
   45 | 
   46 | import org.springframework.aot.hint.ReflectionHints;
   47 | import org.springframework.aot.hint.RuntimeHints;
   48 | import org.springframework.aot.hint.RuntimeHintsRegistrar;
   49 | import org.springframework.beans.BeanUtils;
   50 | import org.springframework.beans.factory.ObjectProvider;
   51 | import org.springframework.beans.factory.config.AutowireCapableBeanFactory;
   52 | import org.springframework.beans.factory.config.ConfigurableBeanFactory;
   53 | import org.springframework.boot.autoconfigure.AutoConfiguration;
   54 | import org.springframework.boot.autoconfigure.AutoConfigurationPackages;
   55 | import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
   56 | import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
   57 | import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
   58 | import org.springframework.boot.context.properties.EnableConfigurationProperties;
   59 | import org.springframework.boot.context.properties.PropertyMapper;
   60 | import org.springframework.boot.jackson.JacksonComponentModule;
   61 | import org.springframework.boot.jackson.JacksonMixinModule;
   62 | import org.springframework.boot.jackson.JacksonMixinModuleEntries;
   63 | import org.springframework.boot.jackson.autoconfigure.JacksonProperties.ConstructorDetectorStrategy;
   64 | import org.springframework.boot.jackson.autoconfigure.JacksonProperties.Factory.Constraints;
   65 | import org.springframework.boot.jackson.autoconfigure.JacksonProperties.Factory.Constraints.Read;
   66 | import org.springframework.boot.jackson.autoconfigure.JacksonProperties.Factory.Constraints.Write;
   67 | import org.springframework.context.ApplicationContext;
   68 | import org.springframework.context.annotation.Bean;
   69 | import org.springframework.context.annotation.Configuration;
   70 | import org.springframework.context.annotation.Primary;
   71 | import org.springframework.context.annotation.Scope;
   72 | import org.springframework.core.Ordered;
   73 | import org.springframework.http.ProblemDetail;
   74 | import org.springframework.http.converter.json.ProblemDetailJacksonMixin;
   75 | import org.springframework.http.converter.json.ProblemDetailJacksonXmlMixin;
   76 | import org.springframework.util.Assert;
   77 | import org.springframework.util.ClassUtils;
   78 | import org.springframework.util.ReflectionUtils;
   79 | 
   80 | /**
   81 |  * {@link EnableAutoConfiguration Auto-configuration} for Jackson.
   82 |  *
   83 |  * @author Oliver Gierke
   84 |  * @author Andy Wilkinson
   85 |  * @author Marcel Overdijk
   86 |  * @author Sebastien Deleuze
   87 |  * @author Johannes Edmeier
   88 |  * @author Phillip Webb
   89 |  * @author Eddú Meléndez
   90 |  * @author Ralf Ueberfuhr
   91 |  * @since 4.0.0
   92 |  */
   93 | @AutoConfiguration
   94 | @ConditionalOnClass(JsonMapper.class)
   95 | public final class JacksonAutoConfiguration {
   96 | 
   97 | 	@Bean
   98 | 	JacksonComponentModule jsonComponentModule() {
   99 | 		return new JacksonComponentModule();
  100 | 	}
  101 | 
  102 | 	@Bean
  103 | 	@ConditionalOnMissingBean
  104 | 	JsonFactory jsonFactory(ObjectProvider<JsonFactoryBuilderCustomizer> customizers) {
  105 | 		JsonFactoryBuilder builder = JsonFactory.builder();
  106 | 		customizers.orderedStream().forEach((customizer) -> customizer.customize(builder));
  107 | 		return builder.build();
  108 | 	}
  109 | 
  110 | 	@Bean
  111 | 	@Scope(ConfigurableBeanFactory.SCOPE_PROTOTYPE)
  112 | 	@ConditionalOnMissingBean
  113 | 	JsonMapper.Builder jsonMapperBuilder(ObjectProvider<JsonMapperBuilderCustomizer> customizers,
  114 | 			JsonFactory jsonFactory) {
  115 | 		JsonMapper.Builder builder = JsonMapper.builder(jsonFactory);
  116 | 		customizers.orderedStream().forEach((customizer) -> customizer.customize(builder));
  117 | 		return builder;
  118 | 	}
  119 | 
  120 | 	@Bean
  121 | 	@Primary
  122 | 	@ConditionalOnMissingBean
  123 | 	JsonMapper jacksonJsonMapper(JsonMapper.Builder builder) {
  124 | 		return builder.build();
  125 | 	}
  126 | 
  127 | 	@Configuration(proxyBeanMethods = false)
  128 | 	static class JacksonMixinConfiguration {
  129 | 
  130 | 		@Bean
  131 | 		static JacksonMixinModuleEntries jacksonMixinModuleEntries(ApplicationContext context) {
  132 | 			List<String> packages = AutoConfigurationPackages.has(context) ? AutoConfigurationPackages.get(context)
  133 | 					: Collections.emptyList();
  134 | 			return JacksonMixinModuleEntries.scan(context, packages);
  135 | 		}
  136 | 
  137 | 		@Bean
  138 | 		JacksonMixinModule jacksonMixinModule(ApplicationContext context, JacksonMixinModuleEntries entries) {
  139 | 			JacksonMixinModule jacksonMixinModule = new JacksonMixinModule();
  140 | 			jacksonMixinModule.registerEntries(entries, context.getClassLoader());
  141 | 			return jacksonMixinModule;
  142 | 		}
  143 | 
  144 | 	}
  145 | 
  146 | 	@Configuration(proxyBeanMethods = false)
  147 | 	@EnableConfigurationProperties(JacksonProperties.class)
  148 | 	static class JacksonJsonCustomizerConfiguration {
  149 | 
  150 | 		private final JacksonProperties jacksonProperties;
  151 | 
  152 | 		JacksonJsonCustomizerConfiguration(JacksonProperties jacksonProperties) {
  153 | 			this.jacksonProperties = jacksonProperties;
  154 | 		}
  155 | 
  156 | 		@Bean
  157 | 		StandardJsonFactoryBuilderCustomizer standardJsonFactoryBuilderCustomizer() {
  158 | 			return new StandardJsonFactoryBuilderCustomizer(this.jacksonProperties);
  159 | 		}
  160 | 
  161 | 		@Bean
  162 | 		StandardJsonMapperBuilderCustomizer standardJsonMapperBuilderCustomizer(ObjectProvider<JacksonModule> modules,
  163 | 				AutowireCapableBeanFactory beanFactory) {
  164 | 			return new StandardJsonMapperBuilderCustomizer(this.jacksonProperties, modules.stream().toList(),
  165 | 					beanFactory);
  166 | 		}
  167 | 
  168 | 		static final class StandardJsonFactoryBuilderCustomizer
  169 | 				extends AbstractFactoryBuilderCustomizer<JsonFactoryBuilder> implements JsonFactoryBuilderCustomizer {
  170 | 
  171 | 			StandardJsonFactoryBuilderCustomizer(JacksonProperties jacksonProperties) {
  172 | 				super(jacksonProperties);
  173 | 			}
  174 | 
  175 | 			@Override
  176 | 			public void customize(JsonFactoryBuilder jsonFactoryBuilder) {
  177 | 				super.customize(jsonFactoryBuilder);
  178 | 			}
  179 | 
  180 | 		}
  181 | 
  182 | 		static final class StandardJsonMapperBuilderCustomizer
  183 | 				extends AbstractMapperBuilderCustomizer<JsonMapper.Builder> implements JsonMapperBuilderCustomizer {
  184 | 
  185 | 			StandardJsonMapperBuilderCustomizer(JacksonProperties jacksonProperties, Collection<JacksonModule> modules,
  186 | 					AutowireCapableBeanFactory beanFactory) {
  187 | 				super(jacksonProperties, modules, beanFactory);
  188 | 			}
  189 | 
  190 | 			@Override
  191 | 			public void customize(JsonMapper.Builder builder) {
  192 | 				super.customize(builder);
  193 | 				configureFeatures(builder, properties().getJson().getRead(), builder::configure);
  194 | 				configureFeatures(builder, properties().getJson().getWrite(), builder::configure);
  195 | 			}
  196 | 
  197 | 		}
  198 | 
  199 | 	}
  200 | 
  201 | 	@Configuration(proxyBeanMethods = false)
  202 | 	@ConditionalOnClass(ProblemDetail.class)
  203 | 	static class JsonProblemDetailsConfiguration {
  204 | 
  205 | 		@Bean
  206 | 		ProblemDetailJsonMapperBuilderCustomizer problemDetailJsonMapperBuilderCustomizer() {
  207 | 			return new ProblemDetailJsonMapperBuilderCustomizer();
  208 | 		}
  209 | 
  210 | 		static final class ProblemDetailJsonMapperBuilderCustomizer implements JsonMapperBuilderCustomizer {
  211 | 
  212 | 			@Override
  213 | 			public void customize(JsonMapper.Builder builder) {
  214 | 				builder.addMixIn(ProblemDetail.class, ProblemDetailJacksonMixin.class);
  215 | 			}
  216 | 
  217 | 		}
  218 | 
  219 | 	}
  220 | 
  221 | 	@Configuration(proxyBeanMethods = false)
  222 | 	@ConditionalOnClass(CBORMapper.class)
  223 | 	@EnableConfigurationProperties(JacksonCborProperties.class)
  224 | 	static class CborConfiguration {
  225 | 
  226 | 		private final JacksonProperties jacksonProperties;
  227 | 
  228 | 		CborConfiguration(JacksonProperties jacksonProperties) {
  229 | 			this.jacksonProperties = jacksonProperties;
  230 | 		}
  231 | 
  232 | 		@Bean
  233 | 		@ConditionalOnMissingBean
  234 | 		CBORFactory cborFactory(ObjectProvider<CborFactoryBuilderCustomizer> customizers) {
  235 | 			CBORFactoryBuilder builder = CBORFactory.builder();
  236 | 			customizers.orderedStream().forEach((customizer) -> customizer.customize(builder));
  237 | 			return builder.build();
  238 | 		}
  239 | 
  240 | 		@Bean
  241 | 		@Scope(ConfigurableBeanFactory.SCOPE_PROTOTYPE)
  242 | 		@ConditionalOnMissingBean
  243 | 		CBORMapper.Builder cborMapperBuilder(CBORFactory factory,
  244 | 				ObjectProvider<CborMapperBuilderCustomizer> customizers) {
  245 | 			CBORMapper.Builder builder = CBORMapper.builder(factory);
  246 | 			customizers.orderedStream().forEach((customizer) -> customizer.customize(builder));
  247 | 			return builder;
  248 | 		}
  249 | 
  250 | 		@Bean
  251 | 		@ConditionalOnMissingBean
  252 | 		CBORMapper cborMapper(CBORMapper.Builder builder) {
  253 | 			return builder.build();
  254 | 		}
  255 | 
  256 | 		@Bean
  257 | 		StandardCborFactoryBuilderCustomizer standardCborFactoryBuilderCustomizer() {
  258 | 			return new StandardCborFactoryBuilderCustomizer(this.jacksonProperties);
  259 | 		}
  260 | 
  261 | 		@Bean
  262 | 		StandardCborMapperBuilderCustomizer standardCborMapperBuilderCustomizer(ObjectProvider<JacksonModule> modules,
  263 | 				JacksonCborProperties cborProperties, AutowireCapableBeanFactory beanFactory) {
  264 | 			return new StandardCborMapperBuilderCustomizer(this.jacksonProperties, modules.stream().toList(),
  265 | 					cborProperties, beanFactory);
  266 | 		}
  267 | 
  268 | 		static final class StandardCborFactoryBuilderCustomizer
  269 | 				extends AbstractFactoryBuilderCustomizer<CBORFactoryBuilder> implements CborFactoryBuilderCustomizer {
  270 | 
  271 | 			StandardCborFactoryBuilderCustomizer(JacksonProperties jacksonProperties) {
  272 | 				super(jacksonProperties);
  273 | 			}
  274 | 
  275 | 			@Override
  276 | 			public void customize(CBORFactoryBuilder cborFactoryBuilder) {
  277 | 				super.customize(cborFactoryBuilder);
  278 | 			}
  279 | 
  280 | 		}
  281 | 
  282 | 		static class StandardCborMapperBuilderCustomizer extends AbstractMapperBuilderCustomizer<CBORMapper.Builder>
  283 | 				implements CborMapperBuilderCustomizer {
  284 | 
  285 | 			private final JacksonCborProperties cborProperties;
  286 | 
  287 | 			StandardCborMapperBuilderCustomizer(JacksonProperties jacksonProperties, Collection<JacksonModule> modules,
  288 | 					JacksonCborProperties cborProperties, AutowireCapableBeanFactory beanFactory) {
  289 | 				super(jacksonProperties, modules, beanFactory);
  290 | 				this.cborProperties = cborProperties;
  291 | 			}
  292 | 
  293 | 			@Override
  294 | 			public void customize(CBORMapper.Builder builder) {
  295 | 				super.customize(builder);
  296 | 				configureFeatures(builder, this.cborProperties.getRead(), builder::configure);
  297 | 				configureFeatures(builder, this.cborProperties.getWrite(), builder::configure);
  298 | 			}
  299 | 
  300 | 		}
  301 | 
  302 | 	}
  303 | 
  304 | 	@Configuration(proxyBeanMethods = false)
  305 | 	@ConditionalOnClass(XmlMapper.class)
  306 | 	@EnableConfigurationProperties(JacksonXmlProperties.class)
  307 | 	static class XmlConfiguration {
  308 | 
  309 | 		private final JacksonProperties jacksonProperties;
  310 | 
  311 | 		XmlConfiguration(JacksonProperties jacksonProperties) {
  312 | 			this.jacksonProperties = jacksonProperties;
  313 | 		}
  314 | 
  315 | 		@Bean
  316 | 		@ConditionalOnMissingBean
  317 | 		XmlFactory xmlFactory(ObjectProvider<XmlFactoryBuilderCustomizer> customizers) {
  318 | 			XmlFactoryBuilder builder = XmlFactory.builder();
  319 | 			customizers.orderedStream().forEach((customizer) -> customizer.customize(builder));
  320 | 			return builder.build();
  321 | 		}
  322 | 
  323 | 		@Bean
  324 | 		@Scope(ConfigurableBeanFactory.SCOPE_PROTOTYPE)
  325 | 		@ConditionalOnMissingBean
  326 | 		XmlMapper.Builder xmlMapperBuilder(XmlFactory xmlFactory,
  327 | 				ObjectProvider<XmlMapperBuilderCustomizer> customizers) {
  328 | 			XmlMapper.Builder builder = XmlMapper.builder(xmlFactory);
  329 | 			customizers.orderedStream().forEach((customizer) -> customizer.customize(builder));
  330 | 			return builder;
  331 | 		}
  332 | 
  333 | 		@Bean
  334 | 		@ConditionalOnMissingBean
  335 | 		XmlMapper xmlMapper(XmlMapper.Builder builder) {
  336 | 			return builder.build();
  337 | 		}
  338 | 
  339 | 		@Bean
  340 | 		StandardXmlFactoryBuilderCustomizer standardXmlFactoryBuilderCustomizer() {
  341 | 			return new StandardXmlFactoryBuilderCustomizer(this.jacksonProperties);
  342 | 		}
  343 | 
  344 | 		@Bean
  345 | 		StandardXmlMapperBuilderCustomizer standardXmlMapperBuilderCustomizer(ObjectProvider<JacksonModule> modules,
  346 | 				JacksonXmlProperties xmlProperties, AutowireCapableBeanFactory beanFactory) {
  347 | 			return new StandardXmlMapperBuilderCustomizer(this.jacksonProperties, modules.stream().toList(),
  348 | 					xmlProperties, beanFactory);
  349 | 		}
  350 | 
  351 | 		@Configuration(proxyBeanMethods = false)
  352 | 		@ConditionalOnClass(ProblemDetail.class)
  353 | 		static class XmlProblemDetailsConfiguration {
  354 | 
  355 | 			@Bean
  356 | 			ProblemDetailXmlMapperBuilderCustomizer problemDetailXmlMapperBuilderCustomizer() {
  357 | 				return new ProblemDetailXmlMapperBuilderCustomizer();
  358 | 			}
  359 | 
  360 | 			static final class ProblemDetailXmlMapperBuilderCustomizer implements XmlMapperBuilderCustomizer {
  361 | 
  362 | 				@Override
  363 | 				public void customize(XmlMapper.Builder builder) {
  364 | 					builder.addMixIn(ProblemDetail.class, ProblemDetailJacksonXmlMixin.class);
  365 | 				}
  366 | 
  367 | 			}
  368 | 
  369 | 		}
  370 | 
  371 | 		static final class StandardXmlFactoryBuilderCustomizer
  372 | 				extends AbstractFactoryBuilderCustomizer<XmlFactoryBuilder> implements XmlFactoryBuilderCustomizer {
  373 | 
  374 | 			StandardXmlFactoryBuilderCustomizer(JacksonProperties jacksonProperties) {
  375 | 				super(jacksonProperties);
  376 | 			}
  377 | 
  378 | 			@Override
  379 | 			public void customize(XmlFactoryBuilder xmlFactoryBuilder) {
  380 | 				super.customize(xmlFactoryBuilder);
  381 | 			}
  382 | 
  383 | 		}
  384 | 
  385 | 		static class StandardXmlMapperBuilderCustomizer extends AbstractMapperBuilderCustomizer<XmlMapper.Builder>
  386 | 				implements XmlMapperBuilderCustomizer {
  387 | 
  388 | 			private final JacksonXmlProperties xmlProperties;
  389 | 
  390 | 			StandardXmlMapperBuilderCustomizer(JacksonProperties jacksonProperties, Collection<JacksonModule> modules,
  391 | 					JacksonXmlProperties xmlProperties, AutowireCapableBeanFactory beanFactory) {
  392 | 				super(jacksonProperties, modules, beanFactory);
  393 | 				this.xmlProperties = xmlProperties;
  394 | 			}
  395 | 
  396 | 			@Override
  397 | 			public void customize(XmlMapper.Builder builder) {
  398 | 				super.customize(builder);
  399 | 				configureFeatures(builder, this.xmlProperties.getRead(), builder::configure);
  400 | 				configureFeatures(builder, this.xmlProperties.getWrite(), builder::configure);
  401 | 			}
  402 | 
  403 | 		}
  404 | 
  405 | 	}
  406 | 
  407 | 	static class JacksonAutoConfigurationRuntimeHints implements RuntimeHintsRegistrar {
  408 | 
  409 | 		@Override
  410 | 		public void registerHints(RuntimeHints hints, @Nullable ClassLoader classLoader) {
  411 | 			if (ClassUtils.isPresent("tools.jackson.databind.PropertyNamingStrategy", classLoader)) {
  412 | 				registerPropertyNamingStrategyHints(hints.reflection());
  413 | 			}
  414 | 		}
  415 | 
  416 | 		/**
  417 | 		 * Register hints for the {@code configurePropertyNamingStrategyField} method to
  418 | 		 * use.
  419 | 		 * @param hints reflection hints
  420 | 		 */
  421 | 		private void registerPropertyNamingStrategyHints(ReflectionHints hints) {
  422 | 			registerPropertyNamingStrategyHints(hints, PropertyNamingStrategies.class);
  423 | 		}
  424 | 
  425 | 		private void registerPropertyNamingStrategyHints(ReflectionHints hints, Class<?> type) {
  426 | 			Stream.of(type.getDeclaredFields())
  427 | 				.filter(this::isPropertyNamingStrategyField)
  428 | 				.forEach(hints::registerField);
  429 | 		}
  430 | 
  431 | 		private boolean isPropertyNamingStrategyField(Field candidate) {
  432 | 			return ReflectionUtils.isPublicStaticFinal(candidate)
  433 | 					&& candidate.getType().isAssignableFrom(PropertyNamingStrategy.class);
  434 | 		}
  435 | 
  436 | 	}
  437 | 
  438 | 	abstract static class AbstractFactoryBuilderCustomizer<B extends DecorableTSFBuilder<?, ?>> implements Ordered {
  439 | 
  440 | 		private final JacksonProperties jacksonProperties;
  441 | 
  442 | 		AbstractFactoryBuilderCustomizer(JacksonProperties jacksonProperties) {
  443 | 			this.jacksonProperties = jacksonProperties;
  444 | 		}
  445 | 
  446 | 		@Override
  447 | 		public int getOrder() {
  448 | 			return 0;
  449 | 		}
  450 | 
  451 | 		protected void customize(B builder) {
  452 | 			Constraints constraints = this.jacksonProperties.getFactory().getConstraints();
  453 | 			builder.streamReadConstraints(readConstraintsFrom(constraints.getRead()));
  454 | 			builder.streamWriteConstraints(writeConstraintsFrom(constraints.getWrite()));
  455 | 		}
  456 | 
  457 | 		private StreamReadConstraints readConstraintsFrom(Read read) {
  458 | 			PropertyMapper map = PropertyMapper.get();
  459 | 			StreamReadConstraints.Builder constraintsBuilder = StreamReadConstraints.builder();
  460 | 			map.from(read::getMaxDocumentLength).to(constraintsBuilder::maxDocumentLength);
  461 | 			map.from(read::getMaxNameLength).to(constraintsBuilder::maxNameLength);
  462 | 			map.from(read::getMaxNestingDepth).to(constraintsBuilder::maxNestingDepth);
  463 | 			map.from(read::getMaxNumberLength).to(constraintsBuilder::maxNumberLength);
  464 | 			map.from(read::getMaxStringLength).to(constraintsBuilder::maxStringLength);
  465 | 			map.from(read::getMaxTokenCount).to(constraintsBuilder::maxTokenCount);
  466 | 			return constraintsBuilder.build();
  467 | 		}
  468 | 
  469 | 		private StreamWriteConstraints writeConstraintsFrom(Write write) {
  470 | 			PropertyMapper map = PropertyMapper.get();
  471 | 			StreamWriteConstraints.Builder constraintsBuilder = StreamWriteConstraints.builder();
  472 | 			map.from(write::getMaxNestingDepth).to(constraintsBuilder::maxNestingDepth);
  473 | 			return constraintsBuilder.build();
  474 | 		}
  475 | 
  476 | 	}
  477 | 
  478 | 	abstract static class AbstractMapperBuilderCustomizer<B extends MapperBuilder<?, ?>> implements Ordered {
  479 | 
  480 | 		private final JacksonProperties jacksonProperties;
  481 | 
  482 | 		private final Collection<JacksonModule> modules;
  483 | 
  484 | 		private final HandlerInstantiator handlerInstantiator;
  485 | 
  486 | 		AbstractMapperBuilderCustomizer(JacksonProperties jacksonProperties, Collection<JacksonModule> modules,
  487 | 				AutowireCapableBeanFactory beanFactory) {
  488 | 			this.jacksonProperties = jacksonProperties;
  489 | 			this.modules = modules;
  490 | 			this.handlerInstantiator = new SpringBeanHandlerInstantiator(beanFactory);
  491 | 		}
  492 | 
  493 | 		@Override
  494 | 		public int getOrder() {
  495 | 			return 0;
  496 | 		}
  497 | 
  498 | 		protected JacksonProperties properties() {
  499 | 			return this.jacksonProperties;
  500 | 		}
  501 | 
  502 | 		protected void customize(B builder) {
  503 | 			if (this.jacksonProperties.isUseJackson2Defaults()) {
  504 | 				builder.configureForJackson2()
  505 | 					.disable(DateTimeFeature.WRITE_DATES_AS_TIMESTAMPS, DateTimeFeature.WRITE_DURATIONS_AS_TIMESTAMPS)
  506 | 					.disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES)
  507 | 					.disable(MapperFeature.DEFAULT_VIEW_INCLUSION);
  508 | 			}
  509 | 			if (this.jacksonProperties.isFindAndAddModules()) {
  510 | 				builder.findAndAddModules(getClass().getClassLoader());
  511 | 			}
  512 | 			Include propertyInclusion = this.jacksonProperties.getDefaultPropertyInclusion();
  513 | 			if (propertyInclusion != null) {
  514 | 				builder.changeDefaultPropertyInclusion((handler) -> handler.withValueInclusion(propertyInclusion)
  515 | 					.withContentInclusion(propertyInclusion));
  516 | 			}
  517 | 			if (this.jacksonProperties.getTimeZone() != null) {
  518 | 				builder.defaultTimeZone(this.jacksonProperties.getTimeZone());
  519 | 			}
  520 | 			builder.handlerInstantiator(this.handlerInstantiator);
  521 | 			configureVisibility(builder, this.jacksonProperties.getVisibility());
  522 | 			configureFeatures(builder, this.jacksonProperties.getDeserialization(), builder::configure);
  523 | 			configureFeatures(builder, this.jacksonProperties.getSerialization(), builder::configure);
  524 | 			configureFeatures(builder, this.jacksonProperties.getMapper(), builder::configure);
  525 | 			configureFeatures(builder, this.jacksonProperties.getDatatype().getDatetime(), builder::configure);
  526 | 			configureFeatures(builder, this.jacksonProperties.getDatatype().getEnum(), builder::configure);
  527 | 			configureFeatures(builder, this.jacksonProperties.getDatatype().getJsonNode(), builder::configure);
  528 | 			configureFeatures(builder, this.jacksonProperties.getRead(), builder::configure);
  529 | 			configureFeatures(builder, this.jacksonProperties.getWrite(), builder::configure);
  530 | 			configureDateFormat(builder);
  531 | 			configurePropertyNamingStrategy(builder);
  532 | 			configureModules(builder);
  533 | 			configureLocale(builder);
  534 | 			configureDefaultLeniency(builder);
  535 | 			configureConstructorDetector(builder);
  536 | 		}
  537 | 
  538 | 		protected <T> void configureFeatures(B builder, Map<T, Boolean> features, BiConsumer<T, Boolean> configure) {
  539 | 			features.forEach((feature, value) -> {
  540 | 				if (value != null) {
  541 | 					configure.accept(feature, value);
  542 | 				}
  543 | 			});
  544 | 		}
  545 | 
  546 | 		private void configureVisibility(MapperBuilder<?, ?> builder,
  547 | 				Map<PropertyAccessor, JsonAutoDetect.Visibility> visibilities) {
  548 | 			builder.changeDefaultVisibility((visibilityChecker) -> {
  549 | 				for (Map.Entry<PropertyAccessor, JsonAutoDetect.Visibility> entry : visibilities.entrySet()) {
  550 | 					visibilityChecker = visibilityChecker.withVisibility(entry.getKey(), entry.getValue());
  551 | 				}
  552 | 				return visibilityChecker;
  553 | 			});
  554 | 		}
  555 | 
  556 | 		private void configureDateFormat(MapperBuilder<?, ?> builder) {
  557 | 			// We support a fully qualified class name extending DateFormat or a date
  558 | 			// pattern string value
  559 | 			String dateFormat = this.jacksonProperties.getDateFormat();
  560 | 			if (dateFormat != null) {
  561 | 				try {
  562 | 					Class<?> dateFormatClass = ClassUtils.forName(dateFormat, null);
  563 | 					builder.defaultDateFormat((DateFormat) BeanUtils.instantiateClass(dateFormatClass));
  564 | 				}
  565 | 				catch (ClassNotFoundException ex) {
  566 | 					SimpleDateFormat simpleDateFormat = new SimpleDateFormat(dateFormat);
  567 | 					// Since Jackson 2.6.3 we always need to set a TimeZone (see
  568 | 					// gh-4170). If none in our properties fallback to Jackson's
  569 | 					// default
  570 | 					TimeZone timeZone = this.jacksonProperties.getTimeZone();
  571 | 					if (timeZone == null) {
  572 | 						timeZone = new ObjectMapper().serializationConfig().getTimeZone();
  573 | 					}
  574 | 					simpleDateFormat.setTimeZone(timeZone);
  575 | 					builder.defaultDateFormat(simpleDateFormat);
  576 | 				}
  577 | 			}
  578 | 		}
  579 | 
  580 | 		private void configurePropertyNamingStrategy(MapperBuilder<?, ?> builder) {
  581 | 			// We support a fully qualified class name extending Jackson's
  582 | 			// PropertyNamingStrategy or a string value corresponding to the constant
  583 | 			// names in PropertyNamingStrategy which hold default provided
  584 | 			// implementations
  585 | 			String strategy = this.jacksonProperties.getPropertyNamingStrategy();
  586 | 			if (strategy != null) {
  587 | 				try {
  588 | 					configurePropertyNamingStrategyClass(builder, ClassUtils.forName(strategy, null));
  589 | 				}
  590 | 				catch (ClassNotFoundException ex) {
  591 | 					configurePropertyNamingStrategyField(builder, strategy);
  592 | 				}
  593 | 			}
  594 | 		}
  595 | 
  596 | 		private void configurePropertyNamingStrategyClass(MapperBuilder<?, ?> builder,
  597 | 				Class<?> propertyNamingStrategyClass) {
  598 | 			builder.propertyNamingStrategy(
  599 | 					(PropertyNamingStrategy) BeanUtils.instantiateClass(propertyNamingStrategyClass));
  600 | 		}
  601 | 
  602 | 		private void configurePropertyNamingStrategyField(MapperBuilder<?, ?> builder, String fieldName) {
  603 | 			// Find the field (this way we automatically support new constants
  604 | 			// that may be added by Jackson in the future)
  605 | 			Field field = findPropertyNamingStrategyField(fieldName);
  606 | 			try {
  607 | 				builder.propertyNamingStrategy((PropertyNamingStrategy) field.get(null));
  608 | 			}
  609 | 			catch (Exception ex) {
  610 | 				throw new IllegalStateException(ex);
  611 | 			}
  612 | 		}
  613 | 
  614 | 		private Field findPropertyNamingStrategyField(String fieldName) {
  615 | 			Field field = ReflectionUtils.findField(PropertyNamingStrategies.class, fieldName,
  616 | 					PropertyNamingStrategy.class);
  617 | 			Assert.state(field != null, () -> "Constant named '" + fieldName + "' not found");
  618 | 			return field;
  619 | 		}
  620 | 
  621 | 		private void configureModules(MapperBuilder<?, ?> builder) {
  622 | 			builder.addModules(this.modules);
  623 | 		}
  624 | 
  625 | 		private void configureLocale(MapperBuilder<?, ?> builder) {
  626 | 			Locale locale = this.jacksonProperties.getLocale();
  627 | 			if (locale != null) {
  628 | 				builder.defaultLocale(locale);
  629 | 			}
  630 | 		}
  631 | 
  632 | 		private void configureDefaultLeniency(MapperBuilder<?, ?> builder) {
  633 | 			Boolean defaultLeniency = this.jacksonProperties.getDefaultLeniency();
  634 | 			if (defaultLeniency != null) {
  635 | 				builder.defaultLeniency(defaultLeniency);
  636 | 			}
  637 | 		}
  638 | 
  639 | 		private void configureConstructorDetector(MapperBuilder<?, ?> builder) {
  640 | 			ConstructorDetectorStrategy strategy = this.jacksonProperties.getConstructorDetector();
  641 | 			switch (strategy) {
  642 | 				case USE_PROPERTIES_BASED -> builder.constructorDetector(ConstructorDetector.USE_PROPERTIES_BASED);
  643 | 				case USE_DELEGATING -> builder.constructorDetector(ConstructorDetector.USE_DELEGATING);
  644 | 				case EXPLICIT_ONLY -> builder.constructorDetector(ConstructorDetector.EXPLICIT_ONLY);
  645 | 				default -> builder.constructorDetector(ConstructorDetector.DEFAULT);
  646 | 			}
  647 | 		}
  648 | 
  649 | 	}
  650 | 
  651 | }
```
