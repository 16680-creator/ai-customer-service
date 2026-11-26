<!-- learning-docs source archive（机械抓取原文，禁止手改代码内容） -->
<!-- origin: org.springframework.cloud:spring-cloud-openfeign-core:5.1.0 | path: org/springframework/cloud/openfeign/support/SpringEncoder.java -->
<!-- 正文引用：[[源码:SpringEncoder.java#L起-L止]] ；代码块头部：(src: SpringEncoder.java#L起-L止) -->

# SpringEncoder.java

| 项 | 值 |
| --- | --- |
| 来源 | `org.springframework.cloud:spring-cloud-openfeign-core:5.1.0` |
| 路径 | `org/springframework/cloud/openfeign/support/SpringEncoder.java` |
| 行数 | 264 |
| 在线地址 | https://repo1.maven.org/maven2/org/springframework/cloud/spring-cloud-openfeign-core/5.1.0/spring-cloud-openfeign-core-5.1.0-sources.jar |
| 抓取时间 | 2026-11-25T15:33:26.738Z |

```java
    1 | /*
    2 |  * Copyright 2013-present the original author or authors.
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
   17 | package org.springframework.cloud.openfeign.support;
   18 | 
   19 | import java.io.ByteArrayOutputStream;
   20 | import java.io.IOException;
   21 | import java.io.OutputStream;
   22 | import java.lang.reflect.Type;
   23 | import java.nio.charset.Charset;
   24 | import java.nio.charset.StandardCharsets;
   25 | import java.util.Arrays;
   26 | import java.util.Collection;
   27 | import java.util.LinkedHashMap;
   28 | import java.util.Objects;
   29 | import java.util.stream.Stream;
   30 | 
   31 | import feign.RequestTemplate;
   32 | import feign.codec.EncodeException;
   33 | import feign.codec.Encoder;
   34 | import feign.form.spring.SpringFormEncoder;
   35 | import org.apache.commons.logging.Log;
   36 | import org.apache.commons.logging.LogFactory;
   37 | 
   38 | import org.springframework.beans.factory.ObjectProvider;
   39 | import org.springframework.cloud.openfeign.encoding.HttpEncoding;
   40 | import org.springframework.http.HttpHeaders;
   41 | import org.springframework.http.HttpOutputMessage;
   42 | import org.springframework.http.MediaType;
   43 | import org.springframework.http.converter.ByteArrayHttpMessageConverter;
   44 | import org.springframework.http.converter.GenericHttpMessageConverter;
   45 | import org.springframework.http.converter.HttpMessageConversionException;
   46 | import org.springframework.http.converter.HttpMessageConverter;
   47 | import org.springframework.http.converter.protobuf.ProtobufHttpMessageConverter;
   48 | import org.springframework.util.LinkedMultiValueMap;
   49 | import org.springframework.util.MultiValueMap;
   50 | import org.springframework.web.multipart.MultipartFile;
   51 | 
   52 | import static org.springframework.cloud.openfeign.support.FeignUtils.getHttpHeaders;
   53 | import static org.springframework.http.MediaType.APPLICATION_FORM_URLENCODED;
   54 | import static org.springframework.http.MediaType.MULTIPART_FORM_DATA;
   55 | import static org.springframework.http.MediaType.MULTIPART_MIXED;
   56 | import static org.springframework.http.MediaType.MULTIPART_RELATED;
   57 | 
   58 | /**
   59 |  * @author Spencer Gibb
   60 |  * @author Scien Jus
   61 |  * @author Ahmad Mozafarnia
   62 |  * @author Aaron Whiteside
   63 |  * @author Darren Foong
   64 |  * @author Olga Maciaszek-Sharma
   65 |  * @author Can Bezmen
   66 |  */
   67 | @SuppressWarnings("rawtypes")
   68 | public class SpringEncoder implements Encoder {
   69 | 
   70 | 	private static final Log log = LogFactory.getLog(SpringEncoder.class);
   71 | 
   72 | 	private final SpringFormEncoder springFormEncoder;
   73 | 
   74 | 	private final ObjectProvider<FeignHttpMessageConverters> converters;
   75 | 
   76 | 	private final FeignEncoderProperties encoderProperties;
   77 | 
   78 | 	public SpringEncoder(ObjectProvider<FeignHttpMessageConverters> converters) {
   79 | 		this(new SpringFormEncoder(), new FeignEncoderProperties(), converters);
   80 | 	}
   81 | 
   82 | 	public SpringEncoder(SpringFormEncoder springFormEncoder, FeignEncoderProperties encoderProperties,
   83 | 			ObjectProvider<FeignHttpMessageConverters> converters) {
   84 | 		this.springFormEncoder = springFormEncoder;
   85 | 		this.encoderProperties = encoderProperties;
   86 | 		this.converters = converters;
   87 | 	}
   88 | 
   89 | 	@Override
   90 | 	public void encode(Object requestBody, Type bodyType, RequestTemplate request) throws EncodeException {
   91 | 		if (requestBody != null) {
   92 | 			Collection<String> contentTypes = request.headers().get(HttpEncoding.CONTENT_TYPE);
   93 | 
   94 | 			MediaType requestContentType = null;
   95 | 			if (contentTypes != null && !contentTypes.isEmpty()) {
   96 | 				String type = contentTypes.iterator().next();
   97 | 				requestContentType = MediaType.valueOf(type);
   98 | 			}
   99 | 
  100 | 			if (isFormRelatedContentType(requestContentType)) {
  101 | 				springFormEncoder.encode(requestBody, bodyType, request);
  102 | 				return;
  103 | 			}
  104 | 			else {
  105 | 				if (bodyType == MultipartFile.class) {
  106 | 					log.warn("For MultipartFile to be handled correctly, the 'consumes' parameter of @RequestMapping "
  107 | 							+ "should be specified as MediaType.MULTIPART_FORM_DATA_VALUE");
  108 | 				}
  109 | 			}
  110 | 			encodeWithMessageConverter(requestBody, bodyType, request, requestContentType);
  111 | 		}
  112 | 	}
  113 | 
  114 | 	private void encodeWithMessageConverter(Object requestBody, Type bodyType, RequestTemplate request,
  115 | 			MediaType requestContentType) {
  116 | 		for (HttpMessageConverter messageConverter : converters.getObject().getConverters()) {
  117 | 			FeignOutputMessage outputMessage;
  118 | 			try {
  119 | 				if (messageConverter instanceof GenericHttpMessageConverter) {
  120 | 					outputMessage = checkAndWrite(requestBody, bodyType, requestContentType,
  121 | 							(GenericHttpMessageConverter) messageConverter, request);
  122 | 				}
  123 | 				else {
  124 | 					outputMessage = checkAndWrite(requestBody, requestContentType, messageConverter, request);
  125 | 				}
  126 | 			}
  127 | 			catch (IOException | HttpMessageConversionException ex) {
  128 | 				throw new EncodeException("Error converting request body", ex);
  129 | 			}
  130 | 			if (outputMessage != null) {
  131 | 				// clear headers
  132 | 				request.headers(null);
  133 | 				// converters can modify headers, so update the request
  134 | 				// with the modified headers
  135 | 
  136 | 				MultiValueMap<String, String> newHeaders = new LinkedMultiValueMap<>();
  137 | 				if (outputMessage.getHeaders() != null) {
  138 | 					outputMessage.getHeaders().forEach(newHeaders::put);
  139 | 				}
  140 | 
  141 | 				request.headers(new LinkedHashMap<>(newHeaders));
  142 | 
  143 | 				// do not use charset for binary data and protobuf
  144 | 				Charset charset;
  145 | 
  146 | 				MediaType contentType = outputMessage.getHeaders().getContentType();
  147 | 				Charset charsetFromContentType = contentType != null ? contentType.getCharset() : null;
  148 | 
  149 | 				if (encoderProperties != null && encoderProperties.isCharsetFromContentType()
  150 | 						&& charsetFromContentType != null) {
  151 | 					charset = charsetFromContentType;
  152 | 				}
  153 | 				else if (shouldHaveNullCharset(messageConverter, outputMessage)) {
  154 | 					charset = null;
  155 | 				}
  156 | 				else {
  157 | 					charset = StandardCharsets.UTF_8;
  158 | 				}
  159 | 				request.body(outputMessage.getOutputStream().toByteArray(), charset);
  160 | 				return;
  161 | 			}
  162 | 		}
  163 | 		String message = "Could not write request: no suitable HttpMessageConverter " + "found for request type ["
  164 | 				+ requestBody.getClass().getName() + "]";
  165 | 		if (requestContentType != null) {
  166 | 			message += " and content type [" + requestContentType + "]";
  167 | 		}
  168 | 		throw new EncodeException(message);
  169 | 	}
  170 | 
  171 | 	private boolean shouldHaveNullCharset(HttpMessageConverter messageConverter, FeignOutputMessage outputMessage) {
  172 | 		return binaryContentType(outputMessage) || messageConverter instanceof ByteArrayHttpMessageConverter
  173 | 				|| messageConverter instanceof ProtobufHttpMessageConverter && ProtobufHttpMessageConverter.PROTOBUF
  174 | 					.isCompatibleWith(outputMessage.getHeaders().getContentType());
  175 | 	}
  176 | 
  177 | 	@SuppressWarnings("unchecked")
  178 | 	private FeignOutputMessage checkAndWrite(Object body, MediaType contentType, HttpMessageConverter converter,
  179 | 			RequestTemplate request) throws IOException {
  180 | 		if (converter.canWrite(body.getClass(), contentType)) {
  181 | 			logBeforeWrite(body, contentType, converter);
  182 | 			FeignOutputMessage outputMessage = new FeignOutputMessage(request);
  183 | 			converter.write(body, contentType, outputMessage);
  184 | 			return outputMessage;
  185 | 		}
  186 | 		else {
  187 | 			return null;
  188 | 		}
  189 | 	}
  190 | 
  191 | 	@SuppressWarnings("unchecked")
  192 | 	private FeignOutputMessage checkAndWrite(Object body, Type genericType, MediaType contentType,
  193 | 			GenericHttpMessageConverter converter, RequestTemplate request) throws IOException {
  194 | 		if (converter.canWrite(genericType, body.getClass(), contentType)) {
  195 | 			logBeforeWrite(body, contentType, converter);
  196 | 			FeignOutputMessage outputMessage = new FeignOutputMessage(request);
  197 | 			converter.write(body, genericType, contentType, outputMessage);
  198 | 			return outputMessage;
  199 | 		}
  200 | 		else {
  201 | 			return null;
  202 | 		}
  203 | 	}
  204 | 
  205 | 	private void logBeforeWrite(Object requestBody, MediaType requestContentType,
  206 | 			HttpMessageConverter messageConverter) {
  207 | 		if (log.isDebugEnabled()) {
  208 | 			if (requestContentType != null) {
  209 | 				log.debug("Writing [" + requestBody + "] as \"" + requestContentType + "\" using [" + messageConverter
  210 | 						+ "]");
  211 | 			}
  212 | 			else {
  213 | 				log.debug("Writing [" + requestBody + "] using [" + messageConverter + "]");
  214 | 			}
  215 | 		}
  216 | 	}
  217 | 
  218 | 	private boolean isFormRelatedContentType(MediaType requestContentType) {
  219 | 		return isMultipartType(requestContentType) || isFormUrlEncoded(requestContentType);
  220 | 	}
  221 | 
  222 | 	private boolean isMultipartType(MediaType requestContentType) {
  223 | 		return Arrays.asList(MULTIPART_FORM_DATA, MULTIPART_MIXED, MULTIPART_RELATED).contains(requestContentType);
  224 | 	}
  225 | 
  226 | 	private boolean isFormUrlEncoded(MediaType requestContentType) {
  227 | 		return Objects.equals(APPLICATION_FORM_URLENCODED, requestContentType);
  228 | 	}
  229 | 
  230 | 	protected boolean binaryContentType(FeignOutputMessage outputMessage) {
  231 | 		MediaType contentType = outputMessage.getHeaders().getContentType();
  232 | 		return contentType == null || Stream
  233 | 			.of(MediaType.APPLICATION_CBOR, MediaType.APPLICATION_OCTET_STREAM, MediaType.APPLICATION_PDF,
  234 | 					MediaType.IMAGE_GIF, MediaType.IMAGE_JPEG, MediaType.IMAGE_PNG)
  235 | 			.anyMatch(mediaType -> mediaType.includes(contentType));
  236 | 	}
  237 | 
  238 | 	protected final class FeignOutputMessage implements HttpOutputMessage {
  239 | 
  240 | 		private final ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
  241 | 
  242 | 		private final HttpHeaders httpHeaders;
  243 | 
  244 | 		private FeignOutputMessage(RequestTemplate request) {
  245 | 			httpHeaders = getHttpHeaders(request.headers());
  246 | 		}
  247 | 
  248 | 		@Override
  249 | 		public OutputStream getBody() {
  250 | 			return outputStream;
  251 | 		}
  252 | 
  253 | 		@Override
  254 | 		public HttpHeaders getHeaders() {
  255 | 			return httpHeaders;
  256 | 		}
  257 | 
  258 | 		public ByteArrayOutputStream getOutputStream() {
  259 | 			return outputStream;
  260 | 		}
  261 | 
  262 | 	}
  263 | 
  264 | }
```
