<!-- learning-docs source archive（机械抓取原文，禁止手改代码内容） -->
<!-- origin: org.springframework.ai:spring-ai-model:2.1.0 | path: org/springframework/ai/tool/definition/DefaultToolDefinition.java -->
<!-- 正文引用：[[源码:DefaultToolDefinition.java#L起-L止]] ；代码块头部：(src: DefaultToolDefinition.java#L起-L止) -->

# DefaultToolDefinition.java

| 项 | 值 |
| --- | --- |
| 来源 | `org.springframework.ai:spring-ai-model:2.1.0` |
| 路径 | `org/springframework/ai/tool/definition/DefaultToolDefinition.java` |
| 行数 | 81 |
| 在线地址 | https://repo1.maven.org/maven2/org/springframework/ai/spring-ai-model/2.1.0/spring-ai-model-2.1.0-sources.jar |
| 抓取时间 | 2026-11-25T16:28:24.560Z |

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
   17 | package org.springframework.ai.tool.definition;
   18 | 
   19 | import org.jspecify.annotations.Nullable;
   20 | 
   21 | import org.springframework.ai.util.ParsingUtils;
   22 | import org.springframework.util.Assert;
   23 | import org.springframework.util.StringUtils;
   24 | 
   25 | /**
   26 |  * Default implementation of {@link ToolDefinition}.
   27 |  *
   28 |  * @author Thomas Vitale
   29 |  * @since 1.0.0
   30 |  */
   31 | public record DefaultToolDefinition(String name, String description, String inputSchema) implements ToolDefinition {
   32 | 
   33 | 	public DefaultToolDefinition {
   34 | 		Assert.hasText(name, "name cannot be null or empty");
   35 | 		Assert.hasText(description, "description cannot be null or empty");
   36 | 		Assert.hasText(inputSchema, "inputSchema cannot be null or empty");
   37 | 	}
   38 | 
   39 | 	public static Builder builder() {
   40 | 		return new Builder();
   41 | 	}
   42 | 
   43 | 	public static final class Builder {
   44 | 
   45 | 		private @Nullable String name;
   46 | 
   47 | 		private @Nullable String description;
   48 | 
   49 | 		private @Nullable String inputSchema;
   50 | 
   51 | 		private Builder() {
   52 | 		}
   53 | 
   54 | 		public Builder name(String name) {
   55 | 			this.name = name;
   56 | 			return this;
   57 | 		}
   58 | 
   59 | 		public Builder description(String description) {
   60 | 			this.description = description;
   61 | 			return this;
   62 | 		}
   63 | 
   64 | 		public Builder inputSchema(String inputSchema) {
   65 | 			this.inputSchema = inputSchema;
   66 | 			return this;
   67 | 		}
   68 | 
   69 | 		public ToolDefinition build() {
   70 | 			Assert.state(this.name != null, "toolName cannot be null or empty");
   71 | 			if (!StringUtils.hasText(this.description)) {
   72 | 				this.description = ParsingUtils.reConcatenateCamelCase(this.name, " ");
   73 | 			}
   74 | 			Assert.state(this.description != null, "toolDescription cannot be null or empty");
   75 | 			Assert.state(this.inputSchema != null, "inputSchema cannot be null or empty");
   76 | 			return new DefaultToolDefinition(this.name, this.description, this.inputSchema);
   77 | 		}
   78 | 
   79 | 	}
   80 | 
   81 | }
```
