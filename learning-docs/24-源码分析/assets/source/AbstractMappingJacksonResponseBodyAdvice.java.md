<!-- learning-docs source archive（机械抓取原文，禁止手改代码内容） -->
<!-- origin: org.springframework:spring-webmvc:7.1.0 | path: org/springframework/web/servlet/mvc/method/annotation/AbstractMappingJacksonResponseBodyAdvice.java -->
<!-- 正文引用：[[源码:AbstractMappingJacksonResponseBodyAdvice.java#L起-L止]] ；代码块头部：(src: AbstractMappingJacksonResponseBodyAdvice.java#L起-L止) -->

# AbstractMappingJacksonResponseBodyAdvice.java

| 项 | 值 |
| --- | --- |
| 来源 | `org.springframework:spring-webmvc:7.1.0` |
| 路径 | `org/springframework/web/servlet/mvc/method/annotation/AbstractMappingJacksonResponseBodyAdvice.java` |
| 行数 | 79 |
| 在线地址 | https://repo1.maven.org/maven2/org/springframework/spring-webmvc/7.1.0/spring-webmvc-7.1.0-sources.jar |
| 抓取时间 | 2026-11-25T14:01:13.250Z |

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
   17 | package org.springframework.web.servlet.mvc.method.annotation;
   18 | 
   19 | import org.jspecify.annotations.Nullable;
   20 | 
   21 | import org.springframework.core.MethodParameter;
   22 | import org.springframework.http.MediaType;
   23 | import org.springframework.http.converter.AbstractJacksonHttpMessageConverter;
   24 | import org.springframework.http.converter.HttpMessageConverter;
   25 | import org.springframework.http.converter.json.AbstractJackson2HttpMessageConverter;
   26 | import org.springframework.http.converter.json.MappingJacksonValue;
   27 | import org.springframework.http.server.ServerHttpRequest;
   28 | import org.springframework.http.server.ServerHttpResponse;
   29 | 
   30 | /**
   31 |  * A convenient base class for {@code ResponseBodyAdvice} implementations
   32 |  * that customize the response before JSON serialization with
   33 |  * {@link AbstractJacksonHttpMessageConverter}'s and
   34 |  * {@link AbstractJackson2HttpMessageConverter}'s concrete subclasses.
   35 |  *
   36 |  * @author Rossen Stoyanchev
   37 |  * @author Sebastien Deleuze
   38 |  * @since 4.1
   39 |  */
   40 | @SuppressWarnings("removal")
   41 | public abstract class AbstractMappingJacksonResponseBodyAdvice implements ResponseBodyAdvice<Object> {
   42 | 
   43 | 	@Override
   44 | 	public boolean supports(MethodParameter returnType, Class<? extends HttpMessageConverter<?>> converterType) {
   45 | 		return AbstractJacksonHttpMessageConverter.class.isAssignableFrom(converterType) ||
   46 | 				AbstractJackson2HttpMessageConverter.class.isAssignableFrom(converterType);
   47 | 	}
   48 | 
   49 | 	@Override
   50 | 	public final @Nullable Object beforeBodyWrite(@Nullable Object body, MethodParameter returnType,
   51 | 			MediaType contentType, Class<? extends HttpMessageConverter<?>> converterType,
   52 | 			ServerHttpRequest request, ServerHttpResponse response) {
   53 | 
   54 | 		if (body == null) {
   55 | 			return null;
   56 | 		}
   57 | 		if (AbstractJacksonHttpMessageConverter.class.isAssignableFrom(converterType)) {
   58 | 			return body;
   59 | 		}
   60 | 		MappingJacksonValue container = getOrCreateContainer(body);
   61 | 		beforeBodyWriteInternal(container, contentType, returnType, request, response);
   62 | 		return container;
   63 | 	}
   64 | 
   65 | 	/**
   66 | 	 * Wrap the body in a {@link MappingJacksonValue} value container (for providing
   67 | 	 * additional serialization instructions) or simply cast it if already wrapped.
   68 | 	 */
   69 | 	protected MappingJacksonValue getOrCreateContainer(Object body) {
   70 | 		return (body instanceof MappingJacksonValue mjv ? mjv : new MappingJacksonValue(body));
   71 | 	}
   72 | 
   73 | 	/**
   74 | 	 * Invoked only if the converter type is {@code MappingJackson2HttpMessageConverter}.
   75 | 	 */
   76 | 	protected abstract void beforeBodyWriteInternal(MappingJacksonValue bodyContainer, MediaType contentType,
   77 | 			MethodParameter returnType, ServerHttpRequest request, ServerHttpResponse response);
   78 | 
   79 | }
```
