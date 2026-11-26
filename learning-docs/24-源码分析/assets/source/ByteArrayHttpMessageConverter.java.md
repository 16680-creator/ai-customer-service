<!-- learning-docs source archive（机械抓取原文，禁止手改代码内容） -->
<!-- origin: org.springframework:spring-web:7.1.0 | path: org/springframework/http/converter/ByteArrayHttpMessageConverter.java -->
<!-- 正文引用：[[源码:ByteArrayHttpMessageConverter.java#L起-L止]] ；代码块头部：(src: ByteArrayHttpMessageConverter.java#L起-L止) -->

# ByteArrayHttpMessageConverter.java

| 项 | 值 |
| --- | --- |
| 来源 | `org.springframework:spring-web:7.1.0` |
| 路径 | `org/springframework/http/converter/ByteArrayHttpMessageConverter.java` |
| 行数 | 81 |
| 在线地址 | https://repo1.maven.org/maven2/org/springframework/spring-web/7.1.0/spring-web-7.1.0-sources.jar |
| 抓取时间 | 2026-11-25T14:01:25.126Z |

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
   20 | 
   21 | import org.jspecify.annotations.Nullable;
   22 | 
   23 | import org.springframework.http.HttpInputMessage;
   24 | import org.springframework.http.HttpOutputMessage;
   25 | import org.springframework.http.MediaType;
   26 | 
   27 | /**
   28 |  * Implementation of {@link HttpMessageConverter} that can read and write byte arrays.
   29 |  *
   30 |  * <p>By default, this converter supports all media types (<code>&#42;/&#42;</code>), and
   31 |  * writes with a {@code Content-Type} of {@code application/octet-stream}. This can be
   32 |  * overridden by setting the {@link #setSupportedMediaTypes supportedMediaTypes} property.
   33 |  *
   34 |  * @author Arjen Poutsma
   35 |  * @author Juergen Hoeller
   36 |  * @since 3.0
   37 |  */
   38 | public class ByteArrayHttpMessageConverter extends AbstractHttpMessageConverter<byte[]> {
   39 | 
   40 | 	/**
   41 | 	 * Create a new instance of the {@code ByteArrayHttpMessageConverter}.
   42 | 	 */
   43 | 	public ByteArrayHttpMessageConverter() {
   44 | 		super(MediaType.APPLICATION_OCTET_STREAM, MediaType.ALL);
   45 | 	}
   46 | 
   47 | 
   48 | 	@Override
   49 | 	public boolean supports(Class<?> clazz) {
   50 | 		return byte[].class == clazz;
   51 | 	}
   52 | 
   53 | 	@Override
   54 | 	public byte[] readInternal(Class<? extends byte[]> clazz, HttpInputMessage message) throws IOException {
   55 | 		long length = message.getHeaders().getContentLength();
   56 | 		return (length >= 0 && length < Integer.MAX_VALUE ?
   57 | 				message.getBody().readNBytes((int) length) : message.getBody().readAllBytes());
   58 | 	}
   59 | 
   60 | 	@Override
   61 | 	protected Long getContentLength(byte[] bytes, @Nullable MediaType contentType) {
   62 | 		return (long) bytes.length;
   63 | 	}
   64 | 
   65 | 	@Override
   66 | 	protected void writeInternal(byte[] bytes, HttpOutputMessage outputMessage) throws IOException {
   67 | 		outputMessage.getBody().write(bytes);
   68 | 	}
   69 | 
   70 | 	@Override
   71 | 	public boolean canWriteRepeatedly(byte[] bytes, @Nullable MediaType contentType) {
   72 | 		return true;
   73 | 	}
   74 | 
   75 | 	@Override
   76 | 	@SuppressWarnings("removal")
   77 | 	protected boolean supportsRepeatableWrites(byte[] bytes) {
   78 | 		return true;
   79 | 	}
   80 | 
   81 | }
```
