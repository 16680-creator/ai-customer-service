<!-- learning-docs source archive（机械抓取原文，禁止手改代码内容） -->
<!-- origin: org.springframework:spring-web:7.1.0 | path: org/springframework/http/converter/json/MappingJackson2HttpMessageConverter.java -->
<!-- 正文引用：[[源码:MappingJackson2HttpMessageConverter.java#L起-L止]] ；代码块头部：(src: MappingJackson2HttpMessageConverter.java#L起-L止) -->

# MappingJackson2HttpMessageConverter.java

| 项 | 值 |
| --- | --- |
| 来源 | `org.springframework:spring-web:7.1.0` |
| 路径 | `org/springframework/http/converter/json/MappingJackson2HttpMessageConverter.java` |
| 行数 | 111 |
| 在线地址 | https://repo1.maven.org/maven2/org/springframework/spring-web/7.1.0/spring-web-7.1.0-sources.jar |
| 抓取时间 | 2026-11-25T14:01:25.984Z |

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
   17 | package org.springframework.http.converter.json;
   18 | 
   19 | import java.io.IOException;
   20 | import java.util.Collections;
   21 | import java.util.List;
   22 | 
   23 | import com.fasterxml.jackson.core.JsonGenerator;
   24 | import com.fasterxml.jackson.databind.ObjectMapper;
   25 | import org.jspecify.annotations.Nullable;
   26 | 
   27 | import org.springframework.http.MediaType;
   28 | 
   29 | /**
   30 |  * Implementation of {@link org.springframework.http.converter.HttpMessageConverter} that can read and
   31 |  * write JSON using <a href="https://github.com/FasterXML/jackson">Jackson 2.x's</a> {@link ObjectMapper}.
   32 |  *
   33 |  * <p>This converter can be used to bind to typed beans, or untyped {@code HashMap} instances.
   34 |  *
   35 |  * <p>By default, this converter supports {@code application/json} and {@code application/*+json}
   36 |  * with {@code UTF-8} character set. This can be overridden by setting the
   37 |  * {@link #setSupportedMediaTypes supportedMediaTypes} property.
   38 |  *
   39 |  * <p>The default constructor uses the default configuration provided by {@link Jackson2ObjectMapperBuilder}.
   40 |  *
   41 |  * @author Arjen Poutsma
   42 |  * @author Keith Donald
   43 |  * @author Rossen Stoyanchev
   44 |  * @author Juergen Hoeller
   45 |  * @author Sebastien Deleuze
   46 |  * @since 3.1.2
   47 |  * @deprecated since 7.0 in favor of {@link JacksonJsonHttpMessageConverter}
   48 |  */
   49 | @Deprecated(since = "7.0", forRemoval = true)
   50 | @SuppressWarnings("removal")
   51 | public class MappingJackson2HttpMessageConverter extends AbstractJackson2HttpMessageConverter {
   52 | 
   53 | 	private static final List<MediaType> problemDetailMediaTypes =
   54 | 			Collections.singletonList(MediaType.APPLICATION_PROBLEM_JSON);
   55 | 
   56 | 
   57 | 	private @Nullable String jsonPrefix;
   58 | 
   59 | 
   60 | 	/**
   61 | 	 * Construct a new {@link MappingJackson2HttpMessageConverter} using default configuration
   62 | 	 * provided by {@link Jackson2ObjectMapperBuilder}.
   63 | 	 */
   64 | 	public MappingJackson2HttpMessageConverter() {
   65 | 		this(Jackson2ObjectMapperBuilder.json().build());
   66 | 	}
   67 | 
   68 | 	/**
   69 | 	 * Construct a new {@link MappingJackson2HttpMessageConverter} with a custom {@link ObjectMapper}.
   70 | 	 * You can use {@link Jackson2ObjectMapperBuilder} to build it easily.
   71 | 	 * @see Jackson2ObjectMapperBuilder#json()
   72 | 	 */
   73 | 	public MappingJackson2HttpMessageConverter(ObjectMapper objectMapper) {
   74 | 		super(objectMapper, MediaType.APPLICATION_JSON, new MediaType("application", "*+json"));
   75 | 	}
   76 | 
   77 | 
   78 | 	/**
   79 | 	 * Specify a custom prefix to use for this view's JSON output.
   80 | 	 * Default is none.
   81 | 	 * @see #setPrefixJson
   82 | 	 */
   83 | 	public void setJsonPrefix(String jsonPrefix) {
   84 | 		this.jsonPrefix = jsonPrefix;
   85 | 	}
   86 | 
   87 | 	/**
   88 | 	 * Indicate whether the JSON output by this view should be prefixed with ")]}', ". Default is {@code false}.
   89 | 	 * <p>Prefixing the JSON string in this manner is used to help prevent JSON Hijacking.
   90 | 	 * The prefix renders the string syntactically invalid as a script so that it cannot be hijacked.
   91 | 	 * This prefix should be stripped before parsing the string as JSON.
   92 | 	 * @see #setJsonPrefix
   93 | 	 */
   94 | 	public void setPrefixJson(boolean prefixJson) {
   95 | 		this.jsonPrefix = (prefixJson ? ")]}', " : null);
   96 | 	}
   97 | 
   98 | 
   99 | 	@Override
  100 | 	protected List<MediaType> getMediaTypesForProblemDetail() {
  101 | 		return problemDetailMediaTypes;
  102 | 	}
  103 | 
  104 | 	@Override
  105 | 	protected void writePrefix(JsonGenerator generator, Object object) throws IOException {
  106 | 		if (this.jsonPrefix != null) {
  107 | 			generator.writeRaw(this.jsonPrefix);
  108 | 		}
  109 | 	}
  110 | 
  111 | }
```
