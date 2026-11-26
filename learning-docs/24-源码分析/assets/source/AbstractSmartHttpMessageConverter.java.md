<!-- learning-docs source archive（机械抓取原文，禁止手改代码内容） -->
<!-- origin: org.springframework:spring-web:7.1.0 | path: org/springframework/http/converter/AbstractSmartHttpMessageConverter.java -->
<!-- 正文引用：[[源码:AbstractSmartHttpMessageConverter.java#L起-L止]] ；代码块头部：(src: AbstractSmartHttpMessageConverter.java#L起-L止) -->

# AbstractSmartHttpMessageConverter.java

| 项 | 值 |
| --- | --- |
| 来源 | `org.springframework:spring-web:7.1.0` |
| 路径 | `org/springframework/http/converter/AbstractSmartHttpMessageConverter.java` |
| 行数 | 147 |
| 在线地址 | https://repo1.maven.org/maven2/org/springframework/spring-web/7.1.0/spring-web-7.1.0-sources.jar |
| 抓取时间 | 2026-11-25T14:01:26.345Z |

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
   19 | import java.io.IOException;
   20 | import java.io.OutputStream;
   21 | import java.util.Map;
   22 | 
   23 | import org.jspecify.annotations.Nullable;
   24 | 
   25 | import org.springframework.core.ResolvableType;
   26 | import org.springframework.http.HttpHeaders;
   27 | import org.springframework.http.HttpInputMessage;
   28 | import org.springframework.http.HttpOutputMessage;
   29 | import org.springframework.http.MediaType;
   30 | import org.springframework.http.StreamingHttpOutputMessage;
   31 | 
   32 | /**
   33 |  * Abstract base class for most {@link SmartHttpMessageConverter} implementations.
   34 |  *
   35 |  * @author Sebastien Deleuze
   36 |  * @since 6.2
   37 |  * @param <T> the converted object type
   38 |  */
   39 | public abstract class AbstractSmartHttpMessageConverter<T> extends AbstractHttpMessageConverter<T>
   40 | 		implements SmartHttpMessageConverter<T> {
   41 | 
   42 | 	/**
   43 | 	 * Construct an {@code AbstractSmartHttpMessageConverter} with no supported media types.
   44 | 	 * @see #setSupportedMediaTypes
   45 | 	 */
   46 | 	protected AbstractSmartHttpMessageConverter() {
   47 | 	}
   48 | 
   49 | 	/**
   50 | 	 * Construct an {@code AbstractSmartHttpMessageConverter} with one supported media type.
   51 | 	 * @param supportedMediaType the supported media type
   52 | 	 */
   53 | 	protected AbstractSmartHttpMessageConverter(MediaType supportedMediaType) {
   54 | 		super(supportedMediaType);
   55 | 	}
   56 | 
   57 | 	/**
   58 | 	 * Construct an {@code AbstractSmartHttpMessageConverter} with multiple supported media type.
   59 | 	 * @param supportedMediaTypes the supported media types
   60 | 	 */
   61 | 	protected AbstractSmartHttpMessageConverter(MediaType... supportedMediaTypes) {
   62 | 		super(supportedMediaTypes);
   63 | 	}
   64 | 
   65 | 
   66 | 	@Override
   67 | 	protected boolean supports(Class<?> clazz) {
   68 | 		return true;
   69 | 	}
   70 | 
   71 | 	@Override
   72 | 	public boolean canRead(Class<?> clazz, @Nullable MediaType mediaType) {
   73 | 		return canRead(ResolvableType.forClass(clazz), mediaType);
   74 | 	}
   75 | 
   76 | 	@Override
   77 | 	public boolean canWrite(Class<?> clazz, @Nullable MediaType mediaType) {
   78 | 		return canWrite(ResolvableType.forClass(clazz), clazz, mediaType);
   79 | 	}
   80 | 
   81 | 	/**
   82 | 	 * This implementation sets the default headers by calling {@link #addDefaultHeaders},
   83 | 	 * and then calls {@link #writeInternal}.
   84 | 	 */
   85 | 	@Override
   86 | 	public final void write(T t, ResolvableType type, @Nullable MediaType contentType,
   87 | 			HttpOutputMessage outputMessage, @Nullable Map<String, Object> hints)
   88 | 			throws IOException, HttpMessageNotWritableException {
   89 | 
   90 | 		HttpHeaders headers = outputMessage.getHeaders();
   91 | 		addDefaultHeaders(headers, t, contentType);
   92 | 
   93 | 		if (outputMessage instanceof StreamingHttpOutputMessage streamingOutputMessage) {
   94 | 			streamingOutputMessage.setBody(new StreamingHttpOutputMessage.Body() {
   95 | 				@Override
   96 | 				public void writeTo(OutputStream outputStream) throws IOException {
   97 | 					writeInternal(t, type, new HttpOutputMessage() {
   98 | 						@Override
   99 | 						public OutputStream getBody() {
  100 | 							return outputStream;
  101 | 						}
  102 | 
  103 | 						@Override
  104 | 						public HttpHeaders getHeaders() {
  105 | 							return headers;
  106 | 						}
  107 | 					}, hints);
  108 | 				}
  109 | 				@Override
  110 | 				public boolean repeatable() {
  111 | 					return canWriteRepeatedly(t, contentType);
  112 | 				}
  113 | 			});
  114 | 		}
  115 | 		else {
  116 | 			writeInternal(t, type, outputMessage, hints);
  117 | 			outputMessage.getBody().flush();
  118 | 		}
  119 | 	}
  120 | 
  121 | 	@Override
  122 | 	protected void writeInternal(T t, HttpOutputMessage outputMessage)
  123 | 			throws IOException, HttpMessageNotWritableException {
  124 | 
  125 | 		writeInternal(t, ResolvableType.NONE, outputMessage, null);
  126 | 	}
  127 | 
  128 | 	/**
  129 | 	 * Abstract template method that writes the actual body. Invoked from
  130 | 	 * {@link #write(Object, ResolvableType, MediaType, HttpOutputMessage, Map)}.
  131 | 	 * @param t the object to write to the output message
  132 | 	 * @param type the type of object to write
  133 | 	 * @param outputMessage the HTTP output message to write to
  134 | 	 * @param hints additional information about how to encode
  135 | 	 * @throws IOException in case of I/O errors
  136 | 	 * @throws HttpMessageNotWritableException in case of conversion errors
  137 | 	 */
  138 | 	protected abstract void writeInternal(T t, ResolvableType type, HttpOutputMessage outputMessage,
  139 | 			@Nullable Map<String, Object> hints) throws IOException, HttpMessageNotWritableException;
  140 | 
  141 | 	@Override
  142 | 	protected T readInternal(Class<? extends T> clazz, HttpInputMessage inputMessage)
  143 | 			throws IOException, HttpMessageNotReadableException {
  144 | 
  145 | 		return read(ResolvableType.forClass(clazz), inputMessage, null);
  146 | 	}
  147 | }
```
