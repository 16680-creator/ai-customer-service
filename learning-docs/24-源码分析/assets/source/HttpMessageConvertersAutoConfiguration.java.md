<!-- learning-docs source archive（机械抓取原文，禁止手改代码内容） -->
<!-- origin: org.springframework.boot:spring-boot-http-converter:4.2.0 | path: org/springframework/boot/http/converter/autoconfigure/HttpMessageConvertersAutoConfiguration.java -->
<!-- 正文引用：[[源码:HttpMessageConvertersAutoConfiguration.java#L起-L止]] ；代码块头部：(src: HttpMessageConvertersAutoConfiguration.java#L起-L止) -->

# HttpMessageConvertersAutoConfiguration.java

| 项 | 值 |
| --- | --- |
| 来源 | `org.springframework.boot:spring-boot-http-converter:4.2.0` |
| 路径 | `org/springframework/boot/http/converter/autoconfigure/HttpMessageConvertersAutoConfiguration.java` |
| 行数 | 121 |
| 在线地址 | https://repo1.maven.org/maven2/org/springframework/boot/spring-boot-http-converter/4.2.0/spring-boot-http-converter-4.2.0-sources.jar |
| 抓取时间 | 2026-11-25T14:02:25.334Z |

```java
    1 | /*
    2 |  * Copyright 2012-present the original author or authors.
    3 |  */
    4 | 
    5 | package org.springframework.boot.http.converter.autoconfigure;
    6 | 
    7 | import org.springframework.beans.factory.ObjectProvider;
    8 | import org.springframework.boot.autoconfigure.AutoConfiguration;
    9 | import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
   10 | import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
   11 | import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
   12 | import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication;
   13 | import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication.Type;
   14 | import org.springframework.boot.autoconfigure.condition.NoneNestedConditions;
   15 | import org.springframework.boot.context.properties.EnableConfigurationProperties;
   16 | import org.springframework.boot.http.converter.autoconfigure.HttpMessageConvertersAutoConfiguration.NotReactiveWebApplicationCondition;
   17 | import org.springframework.context.annotation.Bean;
   18 | import org.springframework.context.annotation.Conditional;
   19 | import org.springframework.context.annotation.Configuration;
   20 | import org.springframework.context.annotation.Import;
   21 | import org.springframework.core.annotation.Order;
   22 | import org.springframework.http.converter.HttpMessageConverter;
   23 | import org.springframework.http.converter.HttpMessageConverters.ClientBuilder;
   24 | import org.springframework.http.converter.HttpMessageConverters.ServerBuilder;
   25 | import org.springframework.http.converter.StringHttpMessageConverter;
   26 | 
   27 | /**
   28 |  * {@link EnableAutoConfiguration Auto-configuration} for {@link HttpMessageConverter}s.
   29 |  *
   30 |  * @author Dave Syer
   31 |  * @author Christian Dupuis
   32 |  * @author Piotr Maj
   33 |  * @author Oliver Gierke
   34 |  * @author David Liu
   35 |  * @author Andy Wilkinson
   36 |  * @author Sebastien Deleuze
   37 |  * @author Stephane Nicoll
   38 |  * @author Eddú Meléndez
   39 |  * @author Dmitry Sulman
   40 |  * @author Brian Clozel
   41 |  * @since 4.0.0
   42 |  */
   43 | @SuppressWarnings("removal")
   44 | @AutoConfiguration(afterName = { "org.springframework.boot.jackson.autoconfigure.JacksonAutoConfiguration",
   45 | 		"org.springframework.boot.jackson2.autoconfigure.Jackson2AutoConfiguration",
   46 | 		"org.springframework.boot.jsonb.autoconfigure.JsonbAutoConfiguration",
   47 | 		"org.springframework.boot.gson.autoconfigure.GsonAutoConfiguration",
   48 | 		"org.springframework.boot.kotlinx.serialization.json.autoconfigure.KotlinxSerializationJsonAutoConfiguration" })
   49 | @ConditionalOnClass(HttpMessageConverter.class)
   50 | @Conditional(NotReactiveWebApplicationCondition.class)
   51 | @Import({ JacksonHttpMessageConvertersConfiguration.class, Jackson2HttpMessageConvertersConfiguration.class,
   52 | 		GsonHttpMessageConvertersConfiguration.class, JsonbHttpMessageConvertersConfiguration.class,
   53 | 		KotlinSerializationHttpMessageConvertersConfiguration.class })
   54 | public final class HttpMessageConvertersAutoConfiguration {
   55 | 
   56 | 	static final String PREFERRED_MAPPER_PROPERTY = "spring.http.converters.preferred-json-mapper";
   57 | 
   58 | 	@Bean
   59 | 	@Order(0)
   60 | 	ClientHttpMessageConvertersCustomizer clientConvertersCustomizer(
   61 | 			ObjectProvider<HttpMessageConverter<?>> converters) {
   62 | 		return new DefaultClientHttpMessageConvertersCustomizer(converters.orderedStream().toList());
   63 | 	}
   64 | 
   65 | 	@Bean
   66 | 	@Order(0)
   67 | 	ServerHttpMessageConvertersCustomizer serverConvertersCustomizer(
   68 | 			ObjectProvider<HttpMessageConverter<?>> converters) {
   69 | 		return new DefaultServerHttpMessageConvertersCustomizer(converters.orderedStream().toList());
   70 | 	}
   71 | 
   72 | 	@Configuration(proxyBeanMethods = false)
   73 | 	@EnableConfigurationProperties(HttpMessageConvertersProperties.class)
   74 | 	protected static class StringHttpMessageConverterConfiguration {
   75 | 
   76 | 		@Bean
   77 | 		@Order(0)
   78 | 		@ConditionalOnMissingBean(StringHttpMessageConverter.class)
   79 | 		StringHttpMessageConvertersCustomizer stringHttpMessageConvertersCustomizer(
   80 | 				HttpMessageConvertersProperties properties) {
   81 | 			return new StringHttpMessageConvertersCustomizer(properties);
   82 | 		}
   83 | 
   84 | 	}
   85 | 
   86 | 	static class StringHttpMessageConvertersCustomizer
   87 | 			implements ClientHttpMessageConvertersCustomizer, ServerHttpMessageConvertersCustomizer {
   88 | 
   89 | 		StringHttpMessageConverter converter;
   90 | 
   91 | 		StringHttpMessageConvertersCustomizer(HttpMessageConvertersProperties properties) {
   92 | 			this.converter = new StringHttpMessageConverter(properties.getStringEncodingCharset());
   93 | 			this.converter.setWriteAcceptCharset(false);
   94 | 		}
   95 | 
   96 | 		@Override
   97 | 		public void customize(ClientBuilder builder) {
   98 | 			builder.withStringConverter(this.converter);
   99 | 		}
  100 | 
  101 | 		@Override
  102 | 		public void customize(ServerBuilder builder) {
  103 | 			builder.withStringConverter(this.converter);
  104 | 		}
  105 | 
  106 | 	}
  107 | 
  108 | 	static class NotReactiveWebApplicationCondition extends NoneNestedConditions {
  109 | 
  110 | 		NotReactiveWebApplicationCondition() {
  111 | 			super(ConfigurationPhase.PARSE_CONFIGURATION);
  112 | 		}
  113 | 
  114 | 		@ConditionalOnWebApplication(type = Type.REACTIVE)
  115 | 		private static final class ReactiveWebApplication {
  116 | 
  117 | 		}
  118 | 
  119 | 	}
  120 | 
  121 | }
```
