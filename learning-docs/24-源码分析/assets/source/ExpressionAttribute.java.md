<!-- learning-docs source archive（机械抓取原文，禁止手改代码内容） -->
<!-- origin: org.springframework.security:spring-security-core:7.2.0 | path: org/springframework/security/authorization/method/ExpressionAttribute.java -->
<!-- 正文引用：[[源码:ExpressionAttribute.java#L起-L止]] ；代码块头部：(src: ExpressionAttribute.java#L起-L止) -->

# ExpressionAttribute.java

| 项 | 值 |
| --- | --- |
| 来源 | `org.springframework.security:spring-security-core:7.2.0` |
| 路径 | `org/springframework/security/authorization/method/ExpressionAttribute.java` |
| 行数 | 53 |
| 在线地址 | https://repo1.maven.org/maven2/org/springframework/security/spring-security-core/7.2.0/spring-security-core-7.2.0-sources.jar |
| 抓取时间 | 2026-11-26T06:38:07.052Z |

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
   20 | 
   21 | /**
   22 |  * An {@link Expression} attribute.
   23 |  *
   24 |  * @author Evgeniy Cheban
   25 |  * @since 5.5
   26 |  */
   27 | class ExpressionAttribute {
   28 | 
   29 | 	private final Expression expression;
   30 | 
   31 | 	/**
   32 | 	 * Creates an instance.
   33 | 	 * @param expression the {@link Expression} to use
   34 | 	 */
   35 | 	ExpressionAttribute(Expression expression) {
   36 | 		this.expression = expression;
   37 | 	}
   38 | 
   39 | 	/**
   40 | 	 * Returns the {@link Expression}.
   41 | 	 * @return the {@link Expression} to use
   42 | 	 */
   43 | 	Expression getExpression() {
   44 | 		return this.expression;
   45 | 	}
   46 | 
   47 | 	@Override
   48 | 	public String toString() {
   49 | 		return getClass().getSimpleName() + " [Expression="
   50 | 				+ ((this.expression != null) ? this.expression.getExpressionString() : null) + "]";
   51 | 	}
   52 | 
   53 | }
```
