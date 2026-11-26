<!-- learning-docs source archive（机械抓取原文，禁止手改代码内容） -->
<!-- origin: org.springframework.boot:spring-boot-http-converter:4.2.0 | path: org/springframework/boot/http/converter/autoconfigure/JacksonHttpMessageConvertersConfiguration.java -->
<!-- 正文引用：[[源码:JacksonHttpMessageConvertersConfiguration.java#L起-L止]] ；代码块头部：(src: JacksonHttpMessageConvertersConfiguration.java#L起-L止) -->

# JacksonHttpMessageConvertersConfiguration.java

| 项 | 值 |
| --- | --- |
| 来源 | `org.springframework.boot:spring-boot-http-converter:4.2.0` |
| 路径 | `org/springframework/boot/http/converter/autoconfigure/JacksonHttpMessageConvertersConfiguration.java` |
| 行数 | 105 |
| 在线地址 | https://repo1.maven.org/maven2/org/springframework/boot/spring-boot-http-converter/4.2.0/spring-boot-http-converter-4.2.0-sources.jar |
| 抓取时间 | 2026-11-25T14:02:25.199Z |

```java
    1 | /*
    2 |  * Copyright 2012-present the original author or authors.
    3 |  */
    4 | 
    5 | package org.springframework.boot.http.converter.autoconfigure;
    6 | 
    7 | import tools.jackson.databind.json.JsonMapper;
    8 | import tools.jackson.dataformat.xml.XmlMapper;
    9 | 
   10 | import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
   11 | import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
   12 | import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
   13 | import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
   14 | import org.springframework.context.annotation.Bean;
   15 | import org.springframework.context.annotation.Configuration;
   16 | import org.springframework.core.annotation.Order;
   17 | import org.springframework.http.converter.HttpMessageConverters.ClientBuilder;
   18 | import org.springframework.http.converter.HttpMessageConverters.ServerBuilder;
   19 | import org.springframework.http.converter.json.JacksonJsonHttpMessageConverter;
   20 | import org.springframework.http.converter.xml.JacksonXmlHttpMessageConverter;
   21 | 
   22 | /**
   23 |  * Configuration for HTTP message converters that use Jackson.
   24 |  *
   25 |  * @author Andy Wilkinson
   26 |  * @author Brian Clozel
   27 |  */
   28 | @Configuration(proxyBeanMethods = false)
   29 | class JacksonHttpMessageConvertersConfiguration {
   30 | 
   31 | 	@Configuration(proxyBeanMethods = false)
   32 | 	@ConditionalOnClass(JsonMapper.class)
   33 | 	@ConditionalOnBean(JsonMapper.class)
   34 | 	@ConditionalOnProperty(name = HttpMessageConvertersAutoConfiguration.PREFERRED_MAPPER_PROPERTY,
   35 | 			havingValue = "jackson", matchIfMissing = true)
   36 | 	static class JacksonJsonHttpMessageConverterConfiguration {
   37 | 
   38 | 		@Bean
   39 | 		@Order(0)
   40 | 		@ConditionalOnMissingBean(value = JacksonJsonHttpMessageConverter.class,
   41 | 				ignoredType = { "org.springframework.hateoas.server.mvc.TypeConstrainedJacksonJsonHttpMessageConverter",
   42 | 						"org.springframework.data.rest.webmvc.alps.AlpsJacksonJsonHttpMessageConverter" })
   43 | 		JacksonJsonHttpMessageConvertersCustomizer jacksonJsonHttpMessageConvertersCustomizer(JsonMapper jsonMapper) {
   44 | 			return new JacksonJsonHttpMessageConvertersCustomizer(jsonMapper);
   45 | 		}
   46 | 
   47 | 	}
   48 | 
   49 | 	@Configuration(proxyBeanMethods = false)
   50 | 	@ConditionalOnClass(XmlMapper.class)
   51 | 	@ConditionalOnBean(XmlMapper.class)
   52 | 	protected static class JacksonXmlHttpMessageConverterConfiguration {
   53 | 
   54 | 		@Bean
   55 | 		@Order(0)
   56 | 		@ConditionalOnMissingBean(JacksonXmlHttpMessageConverter.class)
   57 | 		JacksonXmlHttpMessageConvertersCustomizer jacksonXmlHttpMessageConvertersCustomizer(XmlMapper xmlMapper) {
   58 | 			return new JacksonXmlHttpMessageConvertersCustomizer(xmlMapper);
   59 | 		}
   60 | 
   61 | 	}
   62 | 
   63 | 	static class JacksonJsonHttpMessageConvertersCustomizer
   64 | 			implements ClientHttpMessageConvertersCustomizer, ServerHttpMessageConvertersCustomizer {
   65 | 
   66 | 		private final JsonMapper jsonMapper;
   67 | 
   68 | 		JacksonJsonHttpMessageConvertersCustomizer(JsonMapper jsonMapper) {
   69 | 			this.jsonMapper = jsonMapper;
   70 | 		}
   71 | 
   72 | 		@Override
   73 | 		public void customize(ClientBuilder builder) {
   74 | 			builder.withJsonConverter(new JacksonJsonHttpMessageConverter(this.jsonMapper));
   75 | 		}
   76 | 
   77 | 		@Override
   78 | 		public void customize(ServerBuilder builder) {
   79 | 			builder.withJsonConverter(new JacksonJsonHttpMessageConverter(this.jsonMapper));
   80 | 		}
   81 | 
   82 | 	}
   83 | 
   84 | 	static class JacksonXmlHttpMessageConvertersCustomizer
   85 | 			implements ClientHttpMessageConvertersCustomizer, ServerHttpMessageConvertersCustomizer {
   86 | 
   87 | 		private final XmlMapper xmlMapper;
   88 | 
   89 | 		JacksonXmlHttpMessageConvertersCustomizer(XmlMapper xmlMapper) {
   90 | 			this.xmlMapper = xmlMapper;
   91 | 		}
   92 | 
   93 | 		@Override
   94 | 		public void customize(ClientBuilder builder) {
   95 | 			builder.withXmlConverter(new JacksonXmlHttpMessageConverter(this.xmlMapper));
   96 | 		}
   97 | 
   98 | 		@Override
   99 | 		public void customize(ServerBuilder builder) {
  100 | 			builder.withXmlConverter(new JacksonXmlHttpMessageConverter(this.xmlMapper));
  101 | 		}
  102 | 
  103 | 	}
  104 | 
  105 | }
```
