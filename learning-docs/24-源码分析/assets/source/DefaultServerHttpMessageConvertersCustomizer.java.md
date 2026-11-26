<!-- learning-docs source archive（机械抓取原文，禁止手改代码内容） -->
<!-- origin: org.springframework.boot:spring-boot-http-converter:4.2.0 | path: org/springframework/boot/http/converter/autoconfigure/DefaultServerHttpMessageConvertersCustomizer.java -->
<!-- 正文引用：[[源码:DefaultServerHttpMessageConvertersCustomizer.java#L起-L止]] ；代码块头部：(src: DefaultServerHttpMessageConvertersCustomizer.java#L起-L止) -->

# DefaultServerHttpMessageConvertersCustomizer.java

| 项 | 值 |
| --- | --- |
| 来源 | `org.springframework.boot:spring-boot-http-converter:4.2.0` |
| 路径 | `org/springframework/boot/http/converter/autoconfigure/DefaultServerHttpMessageConvertersCustomizer.java` |
| 行数 | 34 |
| 在线地址 | https://repo1.maven.org/maven2/org/springframework/boot/spring-boot-http-converter/4.2.0/spring-boot-http-converter-4.2.0-sources.jar |
| 抓取时间 | 2026-11-25T14:02:25.469Z |

```java
    1 | /*
    2 |  * Copyright 2012-present the original author or authors.
    3 |  */
    4 | 
    5 | package org.springframework.boot.http.converter.autoconfigure;
    6 | 
    7 | import java.util.Collection;
    8 | 
    9 | import org.springframework.http.converter.HttpMessageConverter;
   10 | import org.springframework.http.converter.HttpMessageConverters.ServerBuilder;
   11 | import org.springframework.http.converter.json.KotlinSerializationJsonHttpMessageConverter;
   12 | 
   13 | class DefaultServerHttpMessageConvertersCustomizer implements ServerHttpMessageConvertersCustomizer {
   14 | 
   15 | 	private final Collection<HttpMessageConverter<?>> converters;
   16 | 
   17 | 	DefaultServerHttpMessageConvertersCustomizer(Collection<HttpMessageConverter<?>> converters) {
   18 | 		this.converters = converters;
   19 | 	}
   20 | 
   21 | 	@Override
   22 | 	public void customize(ServerBuilder builder) {
   23 | 		builder.registerDefaults();
   24 | 		this.converters.forEach((converter) -> {
   25 | 			if (converter instanceof KotlinSerializationJsonHttpMessageConverter) {
   26 | 				builder.withKotlinSerializationJsonConverter(converter);
   27 | 			}
   28 | 			else {
   29 | 				builder.addCustomConverter(converter);
   30 | 			}
   31 | 		});
   32 | 	}
   33 | 
   34 | }
```
