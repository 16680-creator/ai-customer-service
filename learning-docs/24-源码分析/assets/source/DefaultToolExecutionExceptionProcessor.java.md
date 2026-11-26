<!-- learning-docs source archive（机械抓取原文，禁止手改代码内容） -->
<!-- origin: org.springframework.ai:spring-ai-model:2.1.0 | path: org/springframework/ai/tool/execution/DefaultToolExecutionExceptionProcessor.java -->
<!-- 正文引用：[[源码:DefaultToolExecutionExceptionProcessor.java#L起-L止]] ；代码块头部：(src: DefaultToolExecutionExceptionProcessor.java#L起-L止) -->

# DefaultToolExecutionExceptionProcessor.java

| 项 | 值 |
| --- | --- |
| 来源 | `org.springframework.ai:spring-ai-model:2.1.0` |
| 路径 | `org/springframework/ai/tool/execution/DefaultToolExecutionExceptionProcessor.java` |
| 行数 | 124 |
| 在线地址 | https://repo1.maven.org/maven2/org/springframework/ai/spring-ai-model/2.1.0/spring-ai-model-2.1.0-sources.jar |
| 抓取时间 | 2026-11-25T16:28:26.886Z |

```java
    1 | /*
    2 |  * Copyright 2023-present the original author or authors.
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
   17 | package org.springframework.ai.tool.execution;
   18 | 
   19 | import java.util.Collections;
   20 | import java.util.List;
   21 | 
   22 | import org.apache.commons.logging.Log;
   23 | import org.apache.commons.logging.LogFactory;
   24 | 
   25 | import org.springframework.util.Assert;
   26 | 
   27 | /**
   28 |  * Default implementation of {@link ToolExecutionExceptionProcessor}. Can be configured
   29 |  * with an allowlist of exceptions that will be unwrapped from the
   30 |  * {@link ToolExecutionException} and rethrown as is.
   31 |  *
   32 |  * @author Thomas Vitale
   33 |  * @author Daniel Garnier-Moiroux
   34 |  * @author YunKui Lu
   35 |  * @since 1.0.0
   36 |  */
   37 | public class DefaultToolExecutionExceptionProcessor implements ToolExecutionExceptionProcessor {
   38 | 
   39 | 	private static final Log logger = LogFactory.getLog(DefaultToolExecutionExceptionProcessor.class);
   40 | 
   41 | 	private static final boolean DEFAULT_ALWAYS_THROW = false;
   42 | 
   43 | 	private final boolean alwaysThrow;
   44 | 
   45 | 	private final List<Class<? extends RuntimeException>> rethrownExceptions;
   46 | 
   47 | 	public DefaultToolExecutionExceptionProcessor(boolean alwaysThrow) {
   48 | 		this(alwaysThrow, Collections.emptyList());
   49 | 	}
   50 | 
   51 | 	public DefaultToolExecutionExceptionProcessor(boolean alwaysThrow,
   52 | 			List<Class<? extends RuntimeException>> rethrownExceptions) {
   53 | 		this.alwaysThrow = alwaysThrow;
   54 | 		this.rethrownExceptions = Collections.unmodifiableList(rethrownExceptions);
   55 | 	}
   56 | 
   57 | 	@Override
   58 | 	public String process(ToolExecutionException exception) {
   59 | 		Assert.notNull(exception, "exception cannot be null");
   60 | 		Throwable cause = exception.getCause();
   61 | 		if (cause instanceof RuntimeException runtimeException) {
   62 | 			if (this.rethrownExceptions.stream().anyMatch(rethrown -> rethrown.isAssignableFrom(cause.getClass()))) {
   63 | 				throw runtimeException;
   64 | 			}
   65 | 		}
   66 | 		else {
   67 | 			// If the cause is not a RuntimeException (e.g., IOException,
   68 | 			// OutOfMemoryError), rethrow the tool exception.
   69 | 			throw exception;
   70 | 		}
   71 | 
   72 | 		if (this.alwaysThrow) {
   73 | 			throw exception;
   74 | 		}
   75 | 		String message = exception.getMessage();
   76 | 		if (message == null || message.isBlank()) {
   77 | 			message = "Exception occurred in tool: " + exception.getToolDefinition().name() + " ("
   78 | 					+ cause.getClass().getSimpleName() + ")";
   79 | 		}
   80 | 		if (logger.isDebugEnabled()) {
   81 | 			logger.debug("Exception thrown by tool: " + exception.getToolDefinition().name() + ". Message: " + message,
   82 | 					exception);
   83 | 		}
   84 | 		return message;
   85 | 	}
   86 | 
   87 | 	public static Builder builder() {
   88 | 		return new Builder();
   89 | 	}
   90 | 
   91 | 	public static final class Builder {
   92 | 
   93 | 		private boolean alwaysThrow = DEFAULT_ALWAYS_THROW;
   94 | 
   95 | 		private List<Class<? extends RuntimeException>> exceptions = Collections.emptyList();
   96 | 
   97 | 		/**
   98 | 		 * Rethrow the {@link ToolExecutionException}
   99 | 		 * @param alwaysThrow when true, throws; when false, returns the exception message
  100 | 		 * @return the builder instance
  101 | 		 */
  102 | 		public Builder alwaysThrow(boolean alwaysThrow) {
  103 | 			this.alwaysThrow = alwaysThrow;
  104 | 			return this;
  105 | 		}
  106 | 
  107 | 		/**
  108 | 		 * An allowlist of exceptions thrown by tools, which will be unwrapped and
  109 | 		 * re-thrown without further processing.
  110 | 		 * @param exceptions the list of exceptions
  111 | 		 * @return the builder instance
  112 | 		 */
  113 | 		public Builder rethrowExceptions(List<Class<? extends RuntimeException>> exceptions) {
  114 | 			this.exceptions = exceptions;
  115 | 			return this;
  116 | 		}
  117 | 
  118 | 		public DefaultToolExecutionExceptionProcessor build() {
  119 | 			return new DefaultToolExecutionExceptionProcessor(this.alwaysThrow, this.exceptions);
  120 | 		}
  121 | 
  122 | 	}
  123 | 
  124 | }
```
