<!-- learning-docs source archive（机械抓取原文，禁止手改代码内容） -->
<!-- origin: org.springframework.cloud:spring-cloud-openfeign-core:5.1.0 | path: org/springframework/cloud/openfeign/support/ResponseEntityDecoder.java -->
<!-- 正文引用：[[源码:ResponseEntityDecoder.java#L起-L止]] ；代码块头部：(src: ResponseEntityDecoder.java#L起-L止) -->

# ResponseEntityDecoder.java

| 项 | 值 |
| --- | --- |
| 来源 | `org.springframework.cloud:spring-cloud-openfeign-core:5.1.0` |
| 路径 | `org/springframework/cloud/openfeign/support/ResponseEntityDecoder.java` |
| 行数 | 94 |
| 在线地址 | https://repo1.maven.org/maven2/org/springframework/cloud/spring-cloud-openfeign-core/5.1.0/spring-cloud-openfeign-core-5.1.0-sources.jar |
| 抓取时间 | 2026-11-25T15:33:25.897Z |

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
   19 | import java.io.IOException;
   20 | import java.lang.reflect.ParameterizedType;
   21 | import java.lang.reflect.Type;
   22 | import java.util.LinkedList;
   23 | 
   24 | import feign.FeignException;
   25 | import feign.Response;
   26 | import feign.codec.Decoder;
   27 | 
   28 | import org.springframework.http.HttpEntity;
   29 | import org.springframework.http.HttpHeaders;
   30 | import org.springframework.http.HttpStatusCode;
   31 | import org.springframework.http.ResponseEntity;
   32 | 
   33 | /**
   34 |  * Decoder adds compatibility for Spring MVC's ResponseEntity to any other decoder via
   35 |  * composition.
   36 |  *
   37 |  * @author chad jaros
   38 |  * @author Olga Maciaszek-Sharma
   39 |  * @author Maksym Pasichenko
   40 |  */
   41 | public class ResponseEntityDecoder implements Decoder {
   42 | 
   43 | 	private final Decoder decoder;
   44 | 
   45 | 	public ResponseEntityDecoder(Decoder decoder) {
   46 | 		this.decoder = decoder;
   47 | 	}
   48 | 
   49 | 	@Override
   50 | 	public Object decode(final Response response, Type type) throws IOException, FeignException {
   51 | 
   52 | 		if (isParameterizeHttpEntity(type)) {
   53 | 			type = ((ParameterizedType) type).getActualTypeArguments()[0];
   54 | 			if (type == Void.class) {
   55 | 				return createResponse(null, response);
   56 | 			}
   57 | 			Object decodedObject = this.decoder.decode(response, type);
   58 | 
   59 | 			return createResponse(decodedObject, response);
   60 | 		}
   61 | 		else if (isHttpEntity(type)) {
   62 | 			return createResponse(null, response);
   63 | 		}
   64 | 		else {
   65 | 			return this.decoder.decode(response, type);
   66 | 		}
   67 | 	}
   68 | 
   69 | 	private boolean isParameterizeHttpEntity(Type type) {
   70 | 		if (type instanceof ParameterizedType) {
   71 | 			return isHttpEntity(((ParameterizedType) type).getRawType());
   72 | 		}
   73 | 		return false;
   74 | 	}
   75 | 
   76 | 	private boolean isHttpEntity(Type type) {
   77 | 		if (type instanceof Class c) {
   78 | 			return HttpEntity.class.isAssignableFrom(c);
   79 | 		}
   80 | 		return false;
   81 | 	}
   82 | 
   83 | 	@SuppressWarnings("unchecked")
   84 | 	private <T> ResponseEntity<T> createResponse(Object instance, Response response) {
   85 | 
   86 | 		HttpHeaders headers = new HttpHeaders();
   87 | 		for (String key : response.headers().keySet()) {
   88 | 			headers.put(key, new LinkedList<>(response.headers().get(key)));
   89 | 		}
   90 | 
   91 | 		return new ResponseEntity<>((T) instance, headers, HttpStatusCode.valueOf(response.status()));
   92 | 	}
   93 | 
   94 | }
```
