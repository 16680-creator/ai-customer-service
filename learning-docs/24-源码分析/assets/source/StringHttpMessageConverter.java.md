<!-- learning-docs source archive（机械抓取原文，禁止手改代码内容） -->
<!-- origin: org.springframework:spring-web:7.1.0 | path: org/springframework/http/converter/StringHttpMessageConverter.java -->
<!-- 正文引用：[[源码:StringHttpMessageConverter.java#L起-L止]] ；代码块头部：(src: StringHttpMessageConverter.java#L起-L止) -->

# StringHttpMessageConverter.java

| 项 | 值 |
| --- | --- |
| 来源 | `org.springframework:spring-web:7.1.0` |
| 路径 | `org/springframework/http/converter/StringHttpMessageConverter.java` |
| 行数 | 172 |
| 在线地址 | https://repo1.maven.org/maven2/org/springframework/spring-web/7.1.0/spring-web-7.1.0-sources.jar |
| 抓取时间 | 2026-11-25T14:01:24.938Z |

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
   20 | import java.nio.charset.Charset;
   21 | import java.nio.charset.StandardCharsets;
   22 | import java.util.ArrayList;
   23 | import java.util.List;
   24 | 
   25 | import org.jspecify.annotations.Nullable;
   26 | 
   27 | import org.springframework.http.HttpHeaders;
   28 | import org.springframework.http.HttpInputMessage;
   29 | import org.springframework.http.HttpOutputMessage;
   30 | import org.springframework.http.MediaType;
   31 | import org.springframework.util.Assert;
   32 | 
   33 | /**
   34 |  * Implementation of {@link HttpMessageConverter} that can read and write strings.
   35 |  *
   36 |  * <p>By default, this converter supports all media types (<code>&#42;/&#42;</code>),
   37 |  * and writes with a {@code Content-Type} of {@code text/plain}. This can be overridden
   38 |  * by setting the {@link #setSupportedMediaTypes supportedMediaTypes} property.
   39 |  *
   40 |  * @author Arjen Poutsma
   41 |  * @author Juergen Hoeller
   42 |  * @since 3.0
   43 |  */
   44 | public class StringHttpMessageConverter extends AbstractHttpMessageConverter<String> {
   45 | 
   46 | 	private static final MediaType APPLICATION_PLUS_JSON = new MediaType("application", "*+json");
   47 | 
   48 | 	/**
   49 | 	 * The default charset used by the converter.
   50 | 	 */
   51 | 	public static final Charset DEFAULT_CHARSET = StandardCharsets.ISO_8859_1;
   52 | 
   53 | 
   54 | 	private volatile @Nullable List<Charset> availableCharsets;
   55 | 
   56 | 	private boolean writeAcceptCharset = false;
   57 | 
   58 | 
   59 | 	/**
   60 | 	 * A default constructor that uses {@code "ISO-8859-1"} as the default charset.
   61 | 	 * @see #StringHttpMessageConverter(Charset)
   62 | 	 */
   63 | 	public StringHttpMessageConverter() {
   64 | 		this(DEFAULT_CHARSET);
   65 | 	}
   66 | 
   67 | 	/**
   68 | 	 * A constructor accepting a default charset to use if the requested content
   69 | 	 * type does not specify one.
   70 | 	 */
   71 | 	public StringHttpMessageConverter(Charset defaultCharset) {
   72 | 		super(defaultCharset, MediaType.TEXT_PLAIN, MediaType.ALL);
   73 | 	}
   74 | 
   75 | 
   76 | 	/**
   77 | 	 * Whether the {@code Accept-Charset} header should be written to any outgoing
   78 | 	 * request sourced from the value of {@link Charset#availableCharsets()}.
   79 | 	 * The behavior is suppressed if the header has already been set.
   80 | 	 * <p>As of 5.2, by default is set to {@code false}.
   81 | 	 */
   82 | 	public void setWriteAcceptCharset(boolean writeAcceptCharset) {
   83 | 		this.writeAcceptCharset = writeAcceptCharset;
   84 | 	}
   85 | 
   86 | 
   87 | 	@Override
   88 | 	public boolean supports(Class<?> clazz) {
   89 | 		return String.class == clazz;
   90 | 	}
   91 | 
   92 | 	@Override
   93 | 	protected String readInternal(Class<? extends String> clazz, HttpInputMessage inputMessage) throws IOException {
   94 | 		Charset charset = getContentTypeCharset(inputMessage.getHeaders().getContentType());
   95 | 		long length = inputMessage.getHeaders().getContentLength();
   96 | 		byte[] bytes = (length >= 0 && length <= Integer.MAX_VALUE ?
   97 | 				inputMessage.getBody().readNBytes((int) length) : inputMessage.getBody().readAllBytes());
   98 | 		return new String(bytes, charset);
   99 | 	}
  100 | 
  101 | 	@Override
  102 | 	protected void writeInternal(String str, HttpOutputMessage outputMessage) throws IOException {
  103 | 		HttpHeaders headers = outputMessage.getHeaders();
  104 | 		if (this.writeAcceptCharset && headers.get(HttpHeaders.ACCEPT_CHARSET) == null) {
  105 | 			headers.setAcceptCharset(getAcceptedCharsets());
  106 | 		}
  107 | 		Charset charset = getContentTypeCharset(headers.getContentType());
  108 | 		outputMessage.getBody().write(str.getBytes(charset));
  109 | 	}
  110 | 
  111 | 	@Override
  112 | 	protected void addDefaultHeaders(HttpHeaders headers, String s, @Nullable MediaType type) throws IOException {
  113 | 		if (headers.getContentType() == null ) {
  114 | 			if (type != null && type.isConcrete() && (type.isCompatibleWith(MediaType.APPLICATION_JSON) ||
  115 | 					type.isCompatibleWith(APPLICATION_PLUS_JSON))) {
  116 | 				// Prevent charset parameter for JSON.
  117 | 				headers.setContentType(type);
  118 | 			}
  119 | 		}
  120 | 		super.addDefaultHeaders(headers, s, type);
  121 | 	}
  122 | 
  123 | 	@Override
  124 | 	protected Long getContentLength(String str, @Nullable MediaType contentType) {
  125 | 		Charset charset = getContentTypeCharset(contentType);
  126 | 		return (long) str.getBytes(charset).length;
  127 | 	}
  128 | 
  129 | 	/**
  130 | 	 * Return the list of supported {@link Charset Charsets}.
  131 | 	 * <p>By default, returns {@link Charset#availableCharsets()}.
  132 | 	 * Can be overridden in subclasses.
  133 | 	 * @return the list of accepted charsets
  134 | 	 */
  135 | 	protected List<Charset> getAcceptedCharsets() {
  136 | 		List<Charset> charsets = this.availableCharsets;
  137 | 		if (charsets == null) {
  138 | 			charsets = new ArrayList<>(Charset.availableCharsets().values());
  139 | 			this.availableCharsets = charsets;
  140 | 		}
  141 | 		return charsets;
  142 | 	}
  143 | 
  144 | 	private Charset getContentTypeCharset(@Nullable MediaType contentType) {
  145 | 		if (contentType != null) {
  146 | 			Charset charset = contentType.getCharset();
  147 | 			if (charset != null) {
  148 | 				return charset;
  149 | 			}
  150 | 			else if (contentType.isCompatibleWith(MediaType.APPLICATION_JSON) ||
  151 | 					contentType.isCompatibleWith(APPLICATION_PLUS_JSON)) {
  152 | 				// Matching to AbstractJackson2HttpMessageConverter#DEFAULT_CHARSET
  153 | 				return StandardCharsets.UTF_8;
  154 | 			}
  155 | 		}
  156 | 		Charset charset = getDefaultCharset();
  157 | 		Assert.state(charset != null, "No default charset");
  158 | 		return charset;
  159 | 	}
  160 | 
  161 | 	@Override
  162 | 	public boolean canWriteRepeatedly(String s, @Nullable MediaType contentType) {
  163 | 		return true;
  164 | 	}
  165 | 
  166 | 	@Override
  167 | 	@SuppressWarnings("removal")
  168 | 	protected boolean supportsRepeatableWrites(String s) {
  169 | 		return true;
  170 | 	}
  171 | 
  172 | }
```
