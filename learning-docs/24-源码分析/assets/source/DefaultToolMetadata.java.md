<!-- learning-docs source archive（机械抓取原文，禁止手改代码内容） -->
<!-- origin: org.springframework.ai:spring-ai-model:2.1.0 | path: org/springframework/ai/tool/metadata/DefaultToolMetadata.java -->
<!-- 正文引用：[[源码:DefaultToolMetadata.java#L起-L止]] ；代码块头部：(src: DefaultToolMetadata.java#L起-L止) -->

# DefaultToolMetadata.java

| 项 | 值 |
| --- | --- |
| 来源 | `org.springframework.ai:spring-ai-model:2.1.0` |
| 路径 | `org/springframework/ai/tool/metadata/DefaultToolMetadata.java` |
| 行数 | 76 |
| 在线地址 | https://repo1.maven.org/maven2/org/springframework/ai/spring-ai-model/2.1.0/spring-ai-model-2.1.0-sources.jar |
| 抓取时间 | 2026-11-26T06:39:21.082Z |

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
   17 | package org.springframework.ai.tool.metadata;
   18 | 
   19 | /**
   20 |  * Default implementation of {@link ToolMetadata}.
   21 |  *
   22 |  * @param returnDirect whether the tool result should be returned directly or passed back
   23 |  * to the model
   24 |  * @param requiresConfirmation whether a call of the tool needs a person's confirmation
   25 |  * before it runs (since 2.1.0)
   26 |  * @author Thomas Vitale
   27 |  * @since 1.0.0
   28 |  */
   29 | public record DefaultToolMetadata(boolean returnDirect, boolean requiresConfirmation) implements ToolMetadata {
   30 | 
   31 | 	/**
   32 | 	 * Create metadata for a tool that needs no confirmation.
   33 | 	 * @param returnDirect whether the tool result should be returned directly or passed
   34 | 	 * back to the model
   35 | 	 */
   36 | 	public DefaultToolMetadata(boolean returnDirect) {
   37 | 		this(returnDirect, false);
   38 | 	}
   39 | 
   40 | 	public static Builder builder() {
   41 | 		return new Builder();
   42 | 	}
   43 | 
   44 | 	public static final class Builder {
   45 | 
   46 | 		private boolean returnDirect = false;
   47 | 
   48 | 		private boolean requiresConfirmation = false;
   49 | 
   50 | 		private Builder() {
   51 | 		}
   52 | 
   53 | 		public Builder returnDirect(boolean returnDirect) {
   54 | 			this.returnDirect = returnDirect;
   55 | 			return this;
   56 | 		}
   57 | 
   58 | 		/**
   59 | 		 * Whether a call of the tool needs a person's confirmation before it runs.
   60 | 		 * @param requiresConfirmation {@code true} to pause the tool-calling loop on a
   61 | 		 * call of the tool
   62 | 		 * @return this builder
   63 | 		 * @since 2.1.0
   64 | 		 */
   65 | 		public Builder requiresConfirmation(boolean requiresConfirmation) {
   66 | 			this.requiresConfirmation = requiresConfirmation;
   67 | 			return this;
   68 | 		}
   69 | 
   70 | 		public ToolMetadata build() {
   71 | 			return new DefaultToolMetadata(this.returnDirect, this.requiresConfirmation);
   72 | 		}
   73 | 
   74 | 	}
   75 | 
   76 | }
```
