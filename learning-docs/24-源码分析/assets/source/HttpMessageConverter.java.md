<!-- learning-docs source archive（机械抓取原文，禁止手改代码内容） -->
<!-- origin: org.springframework:spring-web:7.1.0 | path: org/springframework/http/converter/HttpMessageConverter.java -->
<!-- 正文引用：[[源码:HttpMessageConverter.java#L起-L止]] ；代码块头部：(src: HttpMessageConverter.java#L起-L止) -->

# HttpMessageConverter.java

| 项 | 值 |
| --- | --- |
| 来源 | `org.springframework:spring-web:7.1.0` |
| 路径 | `org/springframework/http/converter/HttpMessageConverter.java` |
| 行数 | 129 |
| 在线地址 | https://repo1.maven.org/maven2/org/springframework/spring-web/7.1.0/spring-web-7.1.0-sources.jar |
| 抓取时间 | 2026-11-25T13:56:28.909Z |

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
   20 | import java.util.Collections;
   21 | import java.util.List;
   22 | 
   23 | import org.jspecify.annotations.Nullable;
   24 | 
   25 | import org.springframework.http.HttpInputMessage;
   26 | import org.springframework.http.HttpOutputMessage;
   27 | import org.springframework.http.MediaType;
   28 | 
   29 | /**
   30 |  * Strategy interface for converting from and to HTTP requests and responses.
   31 |  *
   32 |  * @author Arjen Poutsma
   33 |  * @author Juergen Hoeller
   34 |  * @author Rossen Stoyanchev
   35 |  * @since 3.0
   36 |  * @param <T> the converted object type
   37 |  * @see SmartHttpMessageConverter
   38 |  */
   39 | public interface HttpMessageConverter<T> {
   40 | 
   41 | 	/**
   42 | 	 * Indicates whether the given class can be read by this converter.
   43 | 	 * @param clazz the class to test for readability
   44 | 	 * @param mediaType the media type to read (can be {@code null} if not specified);
   45 | 	 * typically the value of a {@code Content-Type} header.
   46 | 	 * @return {@code true} if readable; {@code false} otherwise
   47 | 	 */
   48 | 	boolean canRead(Class<?> clazz, @Nullable MediaType mediaType);
   49 | 
   50 | 	/**
   51 | 	 * Indicates whether the given class can be written by this converter.
   52 | 	 * @param clazz the class to test for writability
   53 | 	 * @param mediaType the media type to write (can be {@code null} if not specified);
   54 | 	 * typically the value of an {@code Accept} header.
   55 | 	 * @return {@code true} if writable; {@code false} otherwise
   56 | 	 */
   57 | 	boolean canWrite(Class<?> clazz, @Nullable MediaType mediaType);
   58 | 
   59 | 	/**
   60 | 	 * Indicates whether this message converter can
   61 | 	 * {@linkplain #write(Object, MediaType, HttpOutputMessage) write} the
   62 | 	 * given payload multiple times.
   63 | 	 * <p>This can be used by HTTP client libraries to know whether a message can be
   64 | 	 * sent again, for example after an HTTP redirect. The default implementation
   65 | 	 * returns {@code false}. This typically returns false if the payload can be read
   66 | 	 * only once.
   67 | 	 * @param t the object t
   68 | 	 * @param contentType the content type to use when writing.
   69 | 	 * @return {@code true} if {@code t} can be written repeatedly;
   70 | 	 * {@code false} otherwise
   71 | 	 * @since 7.1
   72 | 	 */
   73 | 	default boolean canWriteRepeatedly(T t, @Nullable MediaType contentType) {
   74 | 		return false;
   75 | 	}
   76 | 
   77 | 	/**
   78 | 	 * Return the list of media types supported by this converter. The list may
   79 | 	 * not apply to every possible target element type and calls to this method
   80 | 	 * should typically be guarded via {@link #canWrite(Class, MediaType)
   81 | 	 * canWrite(clazz, null}. The list may also exclude MIME types supported
   82 | 	 * only for a specific class. Alternatively, use
   83 | 	 * {@link #getSupportedMediaTypes(Class)} for a more precise list.
   84 | 	 * @return the list of supported media types
   85 | 	 */
   86 | 	List<MediaType> getSupportedMediaTypes();
   87 | 
   88 | 	/**
   89 | 	 * Return the list of media types supported by this converter for the given
   90 | 	 * class. The list may differ from {@link #getSupportedMediaTypes()} if the
   91 | 	 * converter does not support the given Class or if it supports it only for
   92 | 	 * a subset of media types.
   93 | 	 * @param clazz the type of class to check
   94 | 	 * @return the list of media types supported for the given class
   95 | 	 * @since 5.3.4
   96 | 	 */
   97 | 	default List<MediaType> getSupportedMediaTypes(Class<?> clazz) {
   98 | 		return (canRead(clazz, null) || canWrite(clazz, null) ?
   99 | 				getSupportedMediaTypes() : Collections.emptyList());
  100 | 	}
  101 | 
  102 | 	/**
  103 | 	 * Read an object of the given type from the given input message, and returns it.
  104 | 	 * @param clazz the type of object to return. This type must have previously been passed to the
  105 | 	 * {@link #canRead canRead} method of this interface, which must have returned {@code true}.
  106 | 	 * @param inputMessage the HTTP input message to read from
  107 | 	 * @return the converted object
  108 | 	 * @throws IOException in case of I/O errors
  109 | 	 * @throws HttpMessageNotReadableException in case of conversion errors
  110 | 	 */
  111 | 	T read(Class<? extends T> clazz, HttpInputMessage inputMessage)
  112 | 			throws IOException, HttpMessageNotReadableException;
  113 | 
  114 | 	/**
  115 | 	 * Write a given object to the given output message.
  116 | 	 * @param t the object to write to the output message. The type of this object must have previously been
  117 | 	 * passed to the {@link #canWrite canWrite} method of this interface, which must have returned {@code true}.
  118 | 	 * @param contentType the content type to use when writing. May be {@code null} to indicate that the
  119 | 	 * default content type of the converter must be used. If not {@code null}, this media type must have
  120 | 	 * previously been passed to the {@link #canWrite canWrite} method of this interface, which must have
  121 | 	 * returned {@code true}.
  122 | 	 * @param outputMessage the message to write to
  123 | 	 * @throws IOException in case of I/O errors
  124 | 	 * @throws HttpMessageNotWritableException in case of conversion errors
  125 | 	 */
  126 | 	void write(T t, @Nullable MediaType contentType, HttpOutputMessage outputMessage)
  127 | 			throws IOException, HttpMessageNotWritableException;
  128 | 
  129 | }
```
