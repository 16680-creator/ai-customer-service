<!-- learning-docs source archive（机械抓取原文，禁止手改代码内容） -->
<!-- origin: org.springframework:spring-webmvc:7.1.0 | path: org/springframework/web/servlet/mvc/method/annotation/ResponseEntityReturnValueHandler.java -->
<!-- 正文引用：[[源码:ResponseEntityReturnValueHandler.java#L起-L止]] ；代码块头部：(src: ResponseEntityReturnValueHandler.java#L起-L止) -->

# ResponseEntityReturnValueHandler.java

| 项 | 值 |
| --- | --- |
| 来源 | `org.springframework:spring-webmvc:7.1.0` |
| 路径 | `org/springframework/web/servlet/mvc/method/annotation/ResponseEntityReturnValueHandler.java` |
| 行数 | 83 |
| 在线地址 | https://repo1.maven.org/maven2/org/springframework/spring-webmvc/7.1.0/spring-webmvc-7.1.0-sources.jar |
| 抓取时间 | 2026-11-25T14:01:13.033Z |

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
   22 | import org.springframework.http.HttpEntity;
   23 | import org.springframework.web.context.request.NativeWebRequest;
   24 | import org.springframework.web.method.support.HandlerMethodReturnValueHandler;
   25 | import org.springframework.web.method.support.ModelAndViewContainer;
   26 | 
   27 | /**
   28 |  * Handler for return values of type {@link org.springframework.http.ResponseEntity}
   29 |  * that delegates to one of the following:
   30 |  *
   31 |  * <ul>
   32 |  * <li>{@link HttpEntityMethodProcessor} for responses with a concrete body value
   33 |  * <li>{@link ResponseBodyEmitterReturnValueHandler} for responses with a body
   34 |  * that is a {@link ResponseBodyEmitter} or an async/reactive type.
   35 |  * </ul>
   36 |  *
   37 |  * <p>Use of this wrapper allows for late check in {@link #handleReturnValue} of
   38 |  * the type of the actual body value in case the method signature does not
   39 |  * provide enough information to decide via {@link #supportsReturnType}.
   40 |  *
   41 |  * @author Rossen Stoyanchev
   42 |  * @since 6.2.9
   43 |  */
   44 | public class ResponseEntityReturnValueHandler implements HandlerMethodReturnValueHandler {
   45 | 
   46 | 	private final HttpEntityMethodProcessor httpEntityMethodProcessor;
   47 | 
   48 | 	private final ResponseBodyEmitterReturnValueHandler responseBodyEmitterHandler;
   49 | 
   50 | 
   51 | 	public ResponseEntityReturnValueHandler(
   52 | 			HttpEntityMethodProcessor httpEntityMethodProcessor,
   53 | 			ResponseBodyEmitterReturnValueHandler responseBodyEmitterHandler) {
   54 | 
   55 | 		this.httpEntityMethodProcessor = httpEntityMethodProcessor;
   56 | 		this.responseBodyEmitterHandler = responseBodyEmitterHandler;
   57 | 	}
   58 | 
   59 | 
   60 | 	@Override
   61 | 	public boolean supportsReturnType(MethodParameter returnType) {
   62 | 		return this.httpEntityMethodProcessor.supportsReturnType(returnType);
   63 | 	}
   64 | 
   65 | 	@Override
   66 | 	public void handleReturnValue(
   67 | 			@Nullable Object returnValue, MethodParameter returnType, ModelAndViewContainer mavContainer,
   68 | 			NativeWebRequest request) throws Exception {
   69 | 
   70 | 		if (returnValue instanceof HttpEntity<?> httpEntity) {
   71 | 			Object body = httpEntity.getBody();
   72 | 			if (body != null) {
   73 | 				if (this.responseBodyEmitterHandler.supportsBodyType(body.getClass())) {
   74 | 					this.responseBodyEmitterHandler.handleReturnValue(returnValue, returnType, mavContainer, request);
   75 | 					return;
   76 | 				}
   77 | 			}
   78 | 		}
   79 | 
   80 | 		this.httpEntityMethodProcessor.handleReturnValue(returnValue, returnType, mavContainer, request);
   81 | 	}
   82 | 
   83 | }
```
