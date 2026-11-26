<!-- learning-docs source archive（机械抓取原文，禁止手改代码内容） -->
<!-- origin: org.springframework.ai:spring-ai-mcp:2.1.0 | path: org/springframework/ai/mcp/McpToolsChangedEvent.java -->
<!-- 正文引用：[[源码:McpToolsChangedEvent.java#L起-L止]] ；代码块头部：(src: McpToolsChangedEvent.java#L起-L止) -->

# McpToolsChangedEvent.java

| 项 | 值 |
| --- | --- |
| 来源 | `org.springframework.ai:spring-ai-mcp:2.1.0` |
| 路径 | `org/springframework/ai/mcp/McpToolsChangedEvent.java` |
| 行数 | 50 |
| 在线地址 | https://repo1.maven.org/maven2/org/springframework/ai/spring-ai-mcp/2.1.0/spring-ai-mcp-2.1.0-sources.jar |
| 抓取时间 | 2026-11-26T06:39:22.273Z |

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
   19 | import java.util.List;
   20 | 
   21 | import io.modelcontextprotocol.spec.McpSchema.Tool;
   22 | 
   23 | import org.springframework.context.ApplicationEvent;
   24 | 
   25 | /**
   26 |  * Event published when the MCP Tools have changed for a given MCP connection.
   27 |  *
   28 |  * @author Christian Tzolov
   29 |  */
   30 | public class McpToolsChangedEvent extends ApplicationEvent {
   31 | 
   32 | 	private final String connectionName;
   33 | 
   34 | 	private final List<Tool> tools;
   35 | 
   36 | 	public McpToolsChangedEvent(String connectionName, List<Tool> tools) {
   37 | 		super(connectionName);
   38 | 		this.connectionName = connectionName;
   39 | 		this.tools = tools;
   40 | 	}
   41 | 
   42 | 	public String getConnectionName() {
   43 | 		return this.connectionName;
   44 | 	}
   45 | 
   46 | 	public List<Tool> getTools() {
   47 | 		return this.tools;
   48 | 	}
   49 | 
   50 | }
```
