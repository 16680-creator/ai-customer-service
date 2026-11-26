<!-- learning-docs source archive（机械抓取原文，禁止手改代码内容） -->
<!-- origin: org.springframework.ai:spring-ai-client-chat:2.1.0 | path: org/springframework/ai/chat/client/advisor/ToolCallAdvisor.java -->
<!-- 正文引用：[[源码:ToolCallAdvisor.java#L起-L止]] ；代码块头部：(src: ToolCallAdvisor.java#L起-L止) -->

# ToolCallAdvisor.java

| 项 | 值 |
| --- | --- |
| 来源 | `org.springframework.ai:spring-ai-client-chat:2.1.0` |
| 路径 | `org/springframework/ai/chat/client/advisor/ToolCallAdvisor.java` |
| 行数 | 63 |
| 在线地址 | https://repo1.maven.org/maven2/org/springframework/ai/spring-ai-client-chat/2.1.0/spring-ai-client-chat-2.1.0-sources.jar |
| 抓取时间 | 2026-11-25T16:27:55.341Z |

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
   17 | package org.springframework.ai.chat.client.advisor;
   18 | 
   19 | import org.springframework.ai.model.tool.ToolCallingManager;
   20 | import org.springframework.ai.model.tool.ToolExecutionEligibilityChecker;
   21 | 
   22 | /**
   23 |  * @author Christian Tzolov
   24 |  * @deprecated since 2.0.0 in favor of {@link ToolCallingAdvisor}. This is kept for
   25 |  * backward compatibility and will be removed in a future release.
   26 |  */
   27 | @Deprecated(since = "2.0.0", forRemoval = true)
   28 | public class ToolCallAdvisor extends ToolCallingAdvisor {
   29 | 
   30 | 	protected ToolCallAdvisor(ToolCallingManager toolCallingManager,
   31 | 			ToolExecutionEligibilityChecker toolExecutionEligibilityChecker, int advisorOrder,
   32 | 			boolean conversationHistoryEnabled) {
   33 | 		super(toolCallingManager, toolExecutionEligibilityChecker, advisorOrder, conversationHistoryEnabled);
   34 | 	}
   35 | 
   36 | 	/**
   37 | 	 * @deprecated since 2.0.0 in favor of {@link ToolCallingAdvisor#builder()}.
   38 | 	 */
   39 | 	@Deprecated(since = "2.0.0", forRemoval = true)
   40 | 	public static Builder<?> builder() {
   41 | 		return new Builder<>();
   42 | 	}
   43 | 
   44 | 	/**
   45 | 	 * @deprecated since 2.0.0 in favor of {@link ToolCallingAdvisor.Builder}.
   46 | 	 */
   47 | 	@Deprecated(since = "2.0.0", forRemoval = true)
   48 | 	public static class Builder<T extends Builder<T>> extends ToolCallingAdvisor.Builder<T> {
   49 | 
   50 | 		@Deprecated(since = "2.0.0", forRemoval = true)
   51 | 		protected Builder() {
   52 | 		}
   53 | 
   54 | 		@Deprecated(since = "2.0.0", forRemoval = true)
   55 | 		@Override
   56 | 		public ToolCallAdvisor build() {
   57 | 			return new ToolCallAdvisor(getToolCallingManager(), getToolExecutionEligibilityChecker(), getAdvisorOrder(),
   58 | 					isConversationHistoryEnabled());
   59 | 		}
   60 | 
   61 | 	}
   62 | 
   63 | }
```
