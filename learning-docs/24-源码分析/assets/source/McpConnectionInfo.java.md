<!-- learning-docs source archive（机械抓取原文，禁止手改代码内容） -->
<!-- origin: org.springframework.ai:spring-ai-mcp:2.1.0 | path: org/springframework/ai/mcp/McpConnectionInfo.java -->
<!-- 正文引用：[[源码:McpConnectionInfo.java#L起-L止]] ；代码块头部：(src: McpConnectionInfo.java#L起-L止) -->

# McpConnectionInfo.java

| 项 | 值 |
| --- | --- |
| 来源 | `org.springframework.ai:spring-ai-mcp:2.1.0` |
| 路径 | `org/springframework/ai/mcp/McpConnectionInfo.java` |
| 行数 | 105 |
| 在线地址 | https://repo1.maven.org/maven2/org/springframework/ai/spring-ai-mcp/2.1.0/spring-ai-mcp-2.1.0-sources.jar |
| 抓取时间 | 2026-11-26T06:39:22.118Z |

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
   17 | package org.springframework.ai.mcp;
   18 | 
   19 | import io.modelcontextprotocol.spec.McpSchema;
   20 | import org.jspecify.annotations.Nullable;
   21 | 
   22 | import org.springframework.util.Assert;
   23 | 
   24 | /**
   25 |  * MCP connection info record containing the client and server related metadata.
   26 |  *
   27 |  * @param clientCapabilities the MCP client capabilities
   28 |  * @param clientInfo the MCP client information
   29 |  * @param initializeResult the MCP server initialization result
   30 |  * @author Ilayaperumal Gopinathan
   31 |  * @author Christian Tzolov
   32 |  */
   33 | public record McpConnectionInfo(// @formatter:off
   34 | 	McpSchema.ClientCapabilities clientCapabilities,
   35 | 	McpSchema.Implementation clientInfo,
   36 | 	McpSchema.@Nullable InitializeResult initializeResult) { // @formatter:on
   37 | 
   38 | 	/**
   39 | 	 * Creates a new Builder instance for constructing McpConnectionInfo.
   40 | 	 * @return a new Builder instance
   41 | 	 */
   42 | 	public static Builder builder() {
   43 | 		return new Builder();
   44 | 	}
   45 | 
   46 | 	/**
   47 | 	 * Builder class for constructing McpConnectionInfo instances.
   48 | 	 */
   49 | 	public static final class Builder {
   50 | 
   51 | 		private McpSchema.@Nullable ClientCapabilities clientCapabilities;
   52 | 
   53 | 		private McpSchema.@Nullable Implementation clientInfo;
   54 | 
   55 | 		private McpSchema.@Nullable InitializeResult initializeResult;
   56 | 
   57 | 		/**
   58 | 		 * Private constructor to enforce builder pattern.
   59 | 		 */
   60 | 		private Builder() {
   61 | 		}
   62 | 
   63 | 		/**
   64 | 		 * Sets the client capabilities.
   65 | 		 * @param clientCapabilities the MCP client capabilities
   66 | 		 * @return this builder instance for method chaining
   67 | 		 */
   68 | 		public Builder clientCapabilities(McpSchema.ClientCapabilities clientCapabilities) {
   69 | 			this.clientCapabilities = clientCapabilities;
   70 | 			return this;
   71 | 		}
   72 | 
   73 | 		/**
   74 | 		 * Sets the client information.
   75 | 		 * @param clientInfo the MCP client information
   76 | 		 * @return this builder instance for method chaining
   77 | 		 */
   78 | 		public Builder clientInfo(McpSchema.Implementation clientInfo) {
   79 | 			this.clientInfo = clientInfo;
   80 | 			return this;
   81 | 		}
   82 | 
   83 | 		/**
   84 | 		 * Sets the initialize result.
   85 | 		 * @param initializeResult the MCP server initialization result
   86 | 		 * @return this builder instance for method chaining
   87 | 		 */
   88 | 		public Builder initializeResult(McpSchema.InitializeResult initializeResult) {
   89 | 			this.initializeResult = initializeResult;
   90 | 			return this;
   91 | 		}
   92 | 
   93 | 		/**
   94 | 		 * Builds and returns a new McpConnectionInfo instance with the configured values.
   95 | 		 * @return a new McpConnectionInfo instance
   96 | 		 */
   97 | 		public McpConnectionInfo build() {
   98 | 			Assert.state(this.clientCapabilities != null, "clientCapabilities should not be null");
   99 | 			Assert.state(this.clientInfo != null, "clientInfo should not be null");
  100 | 			return new McpConnectionInfo(this.clientCapabilities, this.clientInfo, this.initializeResult);
  101 | 		}
  102 | 
  103 | 	}
  104 | 
  105 | }
```
