<!-- learning-docs source archive（机械抓取原文，禁止手改代码内容） -->
<!-- origin: org.springframework.ai:spring-ai-mcp:2.1.0 | path: org/springframework/ai/mcp/ToolContextToMcpMetaConverter.java -->
<!-- 正文引用：[[源码:ToolContextToMcpMetaConverter.java#L起-L止]] ；代码块头部：(src: ToolContextToMcpMetaConverter.java#L起-L止) -->

# ToolContextToMcpMetaConverter.java

| 项 | 值 |
| --- | --- |
| 来源 | `org.springframework.ai:spring-ai-mcp:2.1.0` |
| 路径 | `org/springframework/ai/mcp/ToolContextToMcpMetaConverter.java` |
| 行数 | 70 |
| 在线地址 | https://repo1.maven.org/maven2/org/springframework/ai/spring-ai-mcp/2.1.0/spring-ai-mcp-2.1.0-sources.jar |
| 抓取时间 | 2026-11-26T06:39:22.196Z |

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
   19 | import java.util.Map;
   20 | import java.util.stream.Collectors;
   21 | 
   22 | import org.springframework.ai.chat.model.ToolContext;
   23 | import org.springframework.util.CollectionUtils;
   24 | 
   25 | /**
   26 |  * Strategy interface for converting a {@link ToolContext} to a map of metadata to be sent
   27 |  * as part of an MCP tool call.
   28 |  *
   29 |  * @author Christian Tzolov
   30 |  * @author YunKui Lu
   31 |  */
   32 | public interface ToolContextToMcpMetaConverter {
   33 | 
   34 | 	/**
   35 | 	 * Convert the given {@link ToolContext} to a Map<String, Object> as MCP tool call
   36 | 	 * metadata.
   37 | 	 * <p>
   38 | 	 * The default implementation ignores the
   39 | 	 * {@link McpToolUtils#TOOL_CONTEXT_MCP_EXCHANGE_KEY} entry and any entries with null
   40 | 	 * values.
   41 | 	 * @param toolContext the tool context to convert
   42 | 	 * @return a map of metadata to be sent as part of the MCP tool call
   43 | 	 */
   44 | 	Map<String, Object> convert(ToolContext toolContext);
   45 | 
   46 | 	static ToolContextToMcpMetaConverter defaultConverter() {
   47 | 
   48 | 		return toolContext -> {
   49 | 			if (toolContext == null || CollectionUtils.isEmpty(toolContext.getContext())) {
   50 | 				return Map.of();
   51 | 			}
   52 | 
   53 | 			return toolContext.getContext()
   54 | 				.entrySet()
   55 | 				.stream()
   56 | 				.filter(entry -> !McpToolUtils.TOOL_CONTEXT_MCP_EXCHANGE_KEY.equals(entry.getKey())
   57 | 						&& entry.getValue() != null)
   58 | 				.collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));
   59 | 		};
   60 | 	}
   61 | 
   62 | 	/**
   63 | 	 * Static factory method to create a no-op converter that returns an empty map.
   64 | 	 * @return a no-op converter
   65 | 	 */
   66 | 	static ToolContextToMcpMetaConverter noOp() {
   67 | 		return toolContext -> Map.of();
   68 | 	}
   69 | 
   70 | }
```
