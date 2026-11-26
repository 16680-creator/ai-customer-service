<!-- learning-docs source archive（机械抓取原文，禁止手改代码内容） -->
<!-- origin: org.springframework:spring-web:7.1.0 | path: org/springframework/http/converter/GenericHttpMessageConverter.java -->
<!-- 正文引用：[[源码:GenericHttpMessageConverter.java#L起-L止]] ；代码块头部：(src: GenericHttpMessageConverter.java#L起-L止) -->

# GenericHttpMessageConverter.java

| 项 | 值 |
| --- | --- |
| 来源 | `org.springframework:spring-web:7.1.0` |
| 路径 | `org/springframework/http/converter/GenericHttpMessageConverter.java` |
| 行数 | 108 |
| 在线地址 | https://repo1.maven.org/maven2/org/springframework/spring-web/7.1.0/spring-web-7.1.0-sources.jar |
| 抓取时间 | 2026-11-25T14:01:24.542Z |

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
   20 | import java.lang.reflect.Type;
   21 | 
   22 | import org.jspecify.annotations.Nullable;
   23 | 
   24 | import org.springframework.http.HttpInputMessage;
   25 | import org.springframework.http.HttpOutputMessage;
   26 | import org.springframework.http.MediaType;
   27 | 
   28 | /**
   29 |  * A specialization of {@link HttpMessageConverter} that can convert an HTTP request
   30 |  * into a target object of a specified generic type and a source object of a specified
   31 |  * generic type into an HTTP response.
   32 |  *
   33 |  * @author Arjen Poutsma
   34 |  * @author Rossen Stoyanchev
   35 |  * @author Sebastien Deleuze
   36 |  * @since 3.2
   37 |  * @param <T> the converted object type
   38 |  * @see org.springframework.core.ParameterizedTypeReference
   39 |  * @see SmartHttpMessageConverter
   40 |  */
   41 | public interface GenericHttpMessageConverter<T> extends HttpMessageConverter<T> {
   42 | 
   43 | 	/**
   44 | 	 * Indicates whether the given type can be read by this converter.
   45 | 	 * This method should perform the same checks as
   46 | 	 * {@link HttpMessageConverter#canRead(Class, MediaType)} with additional ones
   47 | 	 * related to the generic type.
   48 | 	 * @param type the (potentially generic) type to test for readability
   49 | 	 * @param contextClass a context class for the target type, for example a class
   50 | 	 * in which the target type appears in a method signature (can be {@code null})
   51 | 	 * @param mediaType the media type to read, can be {@code null} if not specified.
   52 | 	 * Typically, the value of a {@code Content-Type} header.
   53 | 	 * @return {@code true} if readable; {@code false} otherwise
   54 | 	 */
   55 | 	boolean canRead(Type type, @Nullable Class<?> contextClass, @Nullable MediaType mediaType);
   56 | 
   57 | 	/**
   58 | 	 * Read an object of the given type from the given input message, and returns it.
   59 | 	 * @param type the (potentially generic) type of object to return. This type must have
   60 | 	 * previously been passed to the {@link #canRead canRead} method of this interface,
   61 | 	 * which must have returned {@code true}.
   62 | 	 * @param contextClass a context class for the target type, for example a class
   63 | 	 * in which the target type appears in a method signature (can be {@code null})
   64 | 	 * @param inputMessage the HTTP input message to read from
   65 | 	 * @return the converted object
   66 | 	 * @throws IOException in case of I/O errors
   67 | 	 * @throws HttpMessageNotReadableException in case of conversion errors
   68 | 	 */
   69 | 	T read(Type type, @Nullable Class<?> contextClass, HttpInputMessage inputMessage)
   70 | 			throws IOException, HttpMessageNotReadableException;
   71 | 
   72 | 	/**
   73 | 	 * Indicates whether the given class can be written by this converter.
   74 | 	 * <p>This method should perform the same checks as
   75 | 	 * {@link HttpMessageConverter#canWrite(Class, MediaType)} with additional ones
   76 | 	 * related to the generic type.
   77 | 	 * @param type the (potentially generic) type to test for writability
   78 | 	 * (can be {@code null} if not specified)
   79 | 	 * @param clazz the source object class to test for writability
   80 | 	 * @param mediaType the media type to write (can be {@code null} if not specified);
   81 | 	 * typically the value of an {@code Accept} header.
   82 | 	 * @return {@code true} if writable; {@code false} otherwise
   83 | 	 * @since 4.2
   84 | 	 */
   85 | 	boolean canWrite(@Nullable Type type, Class<?> clazz, @Nullable MediaType mediaType);
   86 | 
   87 | 	/**
   88 | 	 * Write a given object to the given output message.
   89 | 	 * @param t the object to write to the output message. The type of this object must
   90 | 	 * have previously been passed to the {@link #canWrite canWrite} method of this
   91 | 	 * interface, which must have returned {@code true}.
   92 | 	 * @param type the (potentially generic) type of object to write. This type must have
   93 | 	 * previously been passed to the {@link #canWrite canWrite} method of this interface,
   94 | 	 * which must have returned {@code true}. Can be {@code null} if not specified.
   95 | 	 * @param contentType the content type to use when writing. May be {@code null} to
   96 | 	 * indicate that the default content type of the converter must be used. If not
   97 | 	 * {@code null}, this media type must have previously been passed to the
   98 | 	 * {@link #canWrite canWrite} method of this interface, which must have returned
   99 | 	 * {@code true}.
  100 | 	 * @param outputMessage the message to write to
  101 | 	 * @throws IOException in case of I/O errors
  102 | 	 * @throws HttpMessageNotWritableException in case of conversion errors
  103 | 	 * @since 4.2
  104 | 	 */
  105 | 	void write(T t, @Nullable Type type, @Nullable MediaType contentType, HttpOutputMessage outputMessage)
  106 | 			throws IOException, HttpMessageNotWritableException;
  107 | 
  108 | }
```
