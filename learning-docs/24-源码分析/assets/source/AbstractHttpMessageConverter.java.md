<!-- learning-docs source archive（机械抓取原文，禁止手改代码内容） -->
<!-- origin: org.springframework:spring-web:7.1.0 | path: org/springframework/http/converter/AbstractHttpMessageConverter.java -->
<!-- 正文引用：[[源码:AbstractHttpMessageConverter.java#L起-L止]] ；代码块头部：(src: AbstractHttpMessageConverter.java#L起-L止) -->

# AbstractHttpMessageConverter.java

| 项 | 值 |
| --- | --- |
| 来源 | `org.springframework:spring-web:7.1.0` |
| 路径 | `org/springframework/http/converter/AbstractHttpMessageConverter.java` |
| 行数 | 343 |
| 在线地址 | https://repo1.maven.org/maven2/org/springframework/spring-web/7.1.0/spring-web-7.1.0-sources.jar |
| 抓取时间 | 2026-11-25T14:01:24.759Z |

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
   21 | import java.nio.charset.Charset;
   22 | import java.util.Arrays;
   23 | import java.util.Collections;
   24 | import java.util.List;
   25 | 
   26 | import org.apache.commons.logging.Log;
   27 | import org.jspecify.annotations.Nullable;
   28 | 
   29 | import org.springframework.http.HttpHeaders;
   30 | import org.springframework.http.HttpInputMessage;
   31 | import org.springframework.http.HttpLogging;
   32 | import org.springframework.http.HttpOutputMessage;
   33 | import org.springframework.http.MediaType;
   34 | import org.springframework.http.StreamingHttpOutputMessage;
   35 | import org.springframework.util.Assert;
   36 | 
   37 | /**
   38 |  * Abstract base class for most {@link HttpMessageConverter} implementations.
   39 |  *
   40 |  * <p>This base class adds support for setting supported {@code MediaTypes}, through the
   41 |  * {@link #setSupportedMediaTypes(List) supportedMediaTypes} bean property. It also adds
   42 |  * support for {@code Content-Type} and {@code Content-Length} when writing to output messages.
   43 |  *
   44 |  * @author Arjen Poutsma
   45 |  * @author Juergen Hoeller
   46 |  * @author Sebastien Deleuze
   47 |  * @since 3.0
   48 |  * @param <T> the converted object type
   49 |  */
   50 | public abstract class AbstractHttpMessageConverter<T> implements HttpMessageConverter<T> {
   51 | 
   52 | 	/** Logger available to subclasses. */
   53 | 	protected final Log logger = HttpLogging.forLogName(getClass());
   54 | 
   55 | 	private List<MediaType> supportedMediaTypes = Collections.emptyList();
   56 | 
   57 | 	private @Nullable Charset defaultCharset;
   58 | 
   59 | 
   60 | 	/**
   61 | 	 * Construct an {@code AbstractHttpMessageConverter} with no supported media types.
   62 | 	 * @see #setSupportedMediaTypes
   63 | 	 */
   64 | 	protected AbstractHttpMessageConverter() {
   65 | 	}
   66 | 
   67 | 	/**
   68 | 	 * Construct an {@code AbstractHttpMessageConverter} with one supported media type.
   69 | 	 * @param supportedMediaType the supported media type
   70 | 	 */
   71 | 	protected AbstractHttpMessageConverter(MediaType supportedMediaType) {
   72 | 		setSupportedMediaTypes(Collections.singletonList(supportedMediaType));
   73 | 	}
   74 | 
   75 | 	/**
   76 | 	 * Construct an {@code AbstractHttpMessageConverter} with multiple supported media types.
   77 | 	 * @param supportedMediaTypes the supported media types
   78 | 	 */
   79 | 	protected AbstractHttpMessageConverter(MediaType... supportedMediaTypes) {
   80 | 		setSupportedMediaTypes(Arrays.asList(supportedMediaTypes));
   81 | 	}
   82 | 
   83 | 	/**
   84 | 	 * Construct an {@code AbstractHttpMessageConverter} with a default charset and
   85 | 	 * multiple supported media types.
   86 | 	 * @param defaultCharset the default character set
   87 | 	 * @param supportedMediaTypes the supported media types
   88 | 	 * @since 4.3
   89 | 	 */
   90 | 	protected AbstractHttpMessageConverter(Charset defaultCharset, MediaType... supportedMediaTypes) {
   91 | 		this.defaultCharset = defaultCharset;
   92 | 		setSupportedMediaTypes(Arrays.asList(supportedMediaTypes));
   93 | 	}
   94 | 
   95 | 
   96 | 	/**
   97 | 	 * Set the list of {@link MediaType} objects supported by this converter.
   98 | 	 */
   99 | 	public void setSupportedMediaTypes(List<MediaType> supportedMediaTypes) {
  100 | 		Assert.notEmpty(supportedMediaTypes, "MediaType List must not be empty");
  101 | 		this.supportedMediaTypes = List.copyOf(supportedMediaTypes);
  102 | 	}
  103 | 
  104 | 	@Override
  105 | 	public List<MediaType> getSupportedMediaTypes() {
  106 | 		return this.supportedMediaTypes;
  107 | 	}
  108 | 
  109 | 	/**
  110 | 	 * Set the default character set, if any.
  111 | 	 * @since 4.3
  112 | 	 */
  113 | 	public void setDefaultCharset(@Nullable Charset defaultCharset) {
  114 | 		this.defaultCharset = defaultCharset;
  115 | 	}
  116 | 
  117 | 	/**
  118 | 	 * Return the default character set, if any.
  119 | 	 * @since 4.3
  120 | 	 */
  121 | 	public @Nullable Charset getDefaultCharset() {
  122 | 		return this.defaultCharset;
  123 | 	}
  124 | 
  125 | 
  126 | 	/**
  127 | 	 * This implementation checks if the given class is {@linkplain #supports(Class) supported},
  128 | 	 * and if the {@linkplain #getSupportedMediaTypes() supported media types}
  129 | 	 * {@linkplain MediaType#includes(MediaType) include} the given media type.
  130 | 	 */
  131 | 	@Override
  132 | 	public boolean canRead(Class<?> clazz, @Nullable MediaType mediaType) {
  133 | 		return supports(clazz) && canRead(mediaType);
  134 | 	}
  135 | 
  136 | 	/**
  137 | 	 * Returns {@code true} if any of the {@linkplain #setSupportedMediaTypes(List)
  138 | 	 * supported} media types {@link MediaType#includes(MediaType) include} the
  139 | 	 * given media type.
  140 | 	 * @param mediaType the media type to read, can be {@code null} if not specified.
  141 | 	 * Typically the value of a {@code Content-Type} header.
  142 | 	 * @return {@code true} if the supported media types include the media type,
  143 | 	 * or if the media type is {@code null}
  144 | 	 */
  145 | 	protected boolean canRead(@Nullable MediaType mediaType) {
  146 | 		if (mediaType == null) {
  147 | 			return true;
  148 | 		}
  149 | 		for (MediaType supportedMediaType : getSupportedMediaTypes()) {
  150 | 			if (supportedMediaType.includes(mediaType)) {
  151 | 				return true;
  152 | 			}
  153 | 		}
  154 | 		return false;
  155 | 	}
  156 | 
  157 | 	/**
  158 | 	 * This implementation checks if the given class is
  159 | 	 * {@linkplain #supports(Class) supported}, and if the
  160 | 	 * {@linkplain #getSupportedMediaTypes() supported} media types
  161 | 	 * {@linkplain MediaType#includes(MediaType) include} the given media type.
  162 | 	 */
  163 | 	@Override
  164 | 	public boolean canWrite(Class<?> clazz, @Nullable MediaType mediaType) {
  165 | 		return supports(clazz) && canWrite(mediaType);
  166 | 	}
  167 | 
  168 | 	/**
  169 | 	 * Returns {@code true} if the given media type includes any of the
  170 | 	 * {@linkplain #setSupportedMediaTypes(List) supported media types}.
  171 | 	 * @param mediaType the media type to write, can be {@code null} if not specified.
  172 | 	 * Typically the value of an {@code Accept} header.
  173 | 	 * @return {@code true} if the supported media types are compatible with the media type,
  174 | 	 * or if the media type is {@code null}
  175 | 	 */
  176 | 	protected boolean canWrite(@Nullable MediaType mediaType) {
  177 | 		if (mediaType == null || MediaType.ALL.equalsTypeAndSubtype(mediaType)) {
  178 | 			return true;
  179 | 		}
  180 | 		for (MediaType supportedMediaType : getSupportedMediaTypes()) {
  181 | 			if (supportedMediaType.isCompatibleWith(mediaType)) {
  182 | 				return true;
  183 | 			}
  184 | 		}
  185 | 		return false;
  186 | 	}
  187 | 
  188 | 	/**
  189 | 	 * This implementation simple delegates to {@link #readInternal(Class, HttpInputMessage)}.
  190 | 	 * Future implementations might add some default behavior, however.
  191 | 	 */
  192 | 	@Override
  193 | 	public final T read(Class<? extends T> clazz, HttpInputMessage inputMessage)
  194 | 			throws IOException, HttpMessageNotReadableException {
  195 | 
  196 | 		return readInternal(clazz, inputMessage);
  197 | 	}
  198 | 
  199 | 	/**
  200 | 	 * This implementation sets the default headers by calling {@link #addDefaultHeaders},
  201 | 	 * and then calls {@link #writeInternal}.
  202 | 	 */
  203 | 	@Override
  204 | 	public final void write(final T t, @Nullable MediaType contentType, HttpOutputMessage outputMessage)
  205 | 			throws IOException, HttpMessageNotWritableException {
  206 | 
  207 | 		final HttpHeaders headers = outputMessage.getHeaders();
  208 | 		addDefaultHeaders(headers, t, contentType);
  209 | 
  210 | 		if (outputMessage instanceof StreamingHttpOutputMessage streamingOutputMessage) {
  211 | 			streamingOutputMessage.setBody(new StreamingHttpOutputMessage.Body() {
  212 | 				@Override
  213 | 				public void writeTo(OutputStream outputStream) throws IOException {
  214 | 					writeInternal(t, new HttpOutputMessage() {
  215 | 						@Override
  216 | 						public OutputStream getBody() {
  217 | 							return outputStream;
  218 | 						}
  219 | 						@Override
  220 | 						public HttpHeaders getHeaders() {
  221 | 							return headers;
  222 | 						}
  223 | 					});
  224 | 				}
  225 | 				@Override
  226 | 				public boolean repeatable() {
  227 | 					return canWriteRepeatedly(t, null);
  228 | 				}
  229 | 			});
  230 | 		}
  231 | 		else {
  232 | 			writeInternal(t, outputMessage);
  233 | 			outputMessage.getBody().flush();
  234 | 		}
  235 | 	}
  236 | 
  237 | 	/**
  238 | 	 * Add default headers to the output message.
  239 | 	 * <p>This implementation delegates to {@link #getDefaultContentType(Object)} if a
  240 | 	 * content type was not provided, set if necessary the default character set, calls
  241 | 	 * {@link #getContentLength}, and sets the corresponding headers.
  242 | 	 * @since 4.2
  243 | 	 */
  244 | 	protected void addDefaultHeaders(HttpHeaders headers, T t, @Nullable MediaType contentType) throws IOException {
  245 | 		if (headers.getContentType() == null) {
  246 | 			MediaType contentTypeToUse = contentType;
  247 | 			if (contentType == null || !contentType.isConcrete()) {
  248 | 				contentTypeToUse = getDefaultContentType(t);
  249 | 			}
  250 | 			else if (MediaType.APPLICATION_OCTET_STREAM.equals(contentType)) {
  251 | 				MediaType mediaType = getDefaultContentType(t);
  252 | 				contentTypeToUse = (mediaType != null ? mediaType : contentTypeToUse);
  253 | 			}
  254 | 			if (contentTypeToUse != null) {
  255 | 				String value = contentTypeToUse.toString();
  256 | 				if (contentTypeToUse.getCharset() == null) {
  257 | 					Charset defaultCharset = getDefaultCharset();
  258 | 					if (defaultCharset != null) {
  259 | 						value += ";charset=" + defaultCharset.name();
  260 | 					}
  261 | 				}
  262 | 				headers.set(HttpHeaders.CONTENT_TYPE, value);
  263 | 			}
  264 | 		}
  265 | 		if (headers.getContentLength() < 0 && !headers.containsHeader(HttpHeaders.TRANSFER_ENCODING)) {
  266 | 			Long contentLength = getContentLength(t, headers.getContentType());
  267 | 			if (contentLength != null) {
  268 | 				headers.setContentLength(contentLength);
  269 | 			}
  270 | 		}
  271 | 	}
  272 | 
  273 | 	/**
  274 | 	 * Returns the default content type for the given type. Called when {@link #write}
  275 | 	 * is invoked without a specified content type parameter.
  276 | 	 * <p>By default, this returns the first element of the
  277 | 	 * {@link #setSupportedMediaTypes(List) supportedMediaTypes} property, if any.
  278 | 	 * Can be overridden in subclasses.
  279 | 	 * @param t the type to return the content type for
  280 | 	 * @return the content type, or {@code null} if not known
  281 | 	 */
  282 | 	protected @Nullable MediaType getDefaultContentType(T t) throws IOException {
  283 | 		List<MediaType> mediaTypes = getSupportedMediaTypes();
  284 | 		return (!mediaTypes.isEmpty() ? mediaTypes.get(0) : null);
  285 | 	}
  286 | 
  287 | 	/**
  288 | 	 * Returns the content length for the given type.
  289 | 	 * <p>By default, this returns {@code null}, meaning that the content length is unknown.
  290 | 	 * Can be overridden in subclasses.
  291 | 	 * @param t the type to return the content length for
  292 | 	 * @return the content length, or {@code null} if not known
  293 | 	 */
  294 | 	protected @Nullable Long getContentLength(T t, @Nullable MediaType contentType) throws IOException {
  295 | 		return null;
  296 | 	}
  297 | 
  298 | 	/**
  299 | 	 * Indicates whether this message converter can
  300 | 	 * {@linkplain #write(Object, MediaType, HttpOutputMessage) write} the
  301 | 	 * given object multiple times.
  302 | 	 * <p>The default implementation returns {@code false}.
  303 | 	 * @param t the object t
  304 | 	 * @return {@code true} if {@code t} can be written repeatedly;
  305 | 	 * {@code false} otherwise
  306 | 	 * @since 6.1
  307 | 	 * @deprecated since 7.1 in favor of {@link #canWriteRepeatedly(Object, MediaType)}.
  308 | 	 */
  309 | 	@Deprecated(since = "7.1", forRemoval = true)
  310 | 	protected boolean supportsRepeatableWrites(T t) {
  311 | 		return canWriteRepeatedly(t, null);
  312 | 	}
  313 | 
  314 | 
  315 | 	/**
  316 | 	 * Indicates whether the given class is supported by this converter.
  317 | 	 * @param clazz the class to test for support
  318 | 	 * @return {@code true} if supported; {@code false} otherwise
  319 | 	 */
  320 | 	protected abstract boolean supports(Class<?> clazz);
  321 | 
  322 | 	/**
  323 | 	 * Abstract template method that reads the actual object. Invoked from {@link #read}.
  324 | 	 * @param clazz the type of object to return
  325 | 	 * @param inputMessage the HTTP input message to read from
  326 | 	 * @return the converted object
  327 | 	 * @throws IOException in case of I/O errors
  328 | 	 * @throws HttpMessageNotReadableException in case of conversion errors
  329 | 	 */
  330 | 	protected abstract T readInternal(Class<? extends T> clazz, HttpInputMessage inputMessage)
  331 | 			throws IOException, HttpMessageNotReadableException;
  332 | 
  333 | 	/**
  334 | 	 * Abstract template method that writes the actual body. Invoked from {@link #write}.
  335 | 	 * @param t the object to write to the output message
  336 | 	 * @param outputMessage the HTTP output message to write to
  337 | 	 * @throws IOException in case of I/O errors
  338 | 	 * @throws HttpMessageNotWritableException in case of conversion errors
  339 | 	 */
  340 | 	protected abstract void writeInternal(T t, HttpOutputMessage outputMessage)
  341 | 			throws IOException, HttpMessageNotWritableException;
  342 | 
  343 | }
```
