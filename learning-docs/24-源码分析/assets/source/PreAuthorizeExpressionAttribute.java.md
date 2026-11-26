<!-- learning-docs source archive（机械抓取原文，禁止手改代码内容） -->
<!-- origin: org.springframework.security:spring-security-core:7.2.0 | path: org/springframework/security/authorization/method/PreAuthorizeExpressionAttribute.java -->
<!-- 正文引用：[[源码:PreAuthorizeExpressionAttribute.java#L起-L止]] ；代码块头部：(src: PreAuthorizeExpressionAttribute.java#L起-L止) -->

# PreAuthorizeExpressionAttribute.java

| 项 | 值 |
| --- | --- |
| 来源 | `org.springframework.security:spring-security-core:7.2.0` |
| 路径 | `org/springframework/security/authorization/method/PreAuthorizeExpressionAttribute.java` |
| 行数 | 42 |
| 在线地址 | https://repo1.maven.org/maven2/org/springframework/security/spring-security-core/7.2.0/spring-security-core-7.2.0-sources.jar |
| 抓取时间 | 2026-11-26T06:38:06.977Z |

```java
    1 | /*
    2 |  * Copyright 2004-present the original author or authors.
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
   17 | package org.springframework.security.authorization.method;
   18 | 
   19 | import org.springframework.expression.Expression;
   20 | import org.springframework.util.Assert;
   21 | 
   22 | /**
   23 |  * An {@link ExpressionAttribute} that carries additional properties for
   24 |  * {@code @PreAuthorize}.
   25 |  *
   26 |  * @author Marcus da Coregio
   27 |  */
   28 | class PreAuthorizeExpressionAttribute extends ExpressionAttribute {
   29 | 
   30 | 	private final MethodAuthorizationDeniedHandler handler;
   31 | 
   32 | 	PreAuthorizeExpressionAttribute(Expression expression, MethodAuthorizationDeniedHandler handler) {
   33 | 		super(expression);
   34 | 		Assert.notNull(handler, "handler cannot be null");
   35 | 		this.handler = handler;
   36 | 	}
   37 | 
   38 | 	MethodAuthorizationDeniedHandler getHandler() {
   39 | 		return this.handler;
   40 | 	}
   41 | 
   42 | }
```
